package tm.davletov.myshopping;

import java.text.NumberFormat;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.text.InputType;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.android.billingclient.api.AcknowledgePurchaseParams;
import com.android.billingclient.api.BillingClient;
import com.android.billingclient.api.BillingClientStateListener;
import com.android.billingclient.api.BillingFlowParams;
import com.android.billingclient.api.BillingResult;
import com.android.billingclient.api.PendingPurchasesParams;
import com.android.billingclient.api.ProductDetails;
import com.android.billingclient.api.Purchase;
import com.android.billingclient.api.PurchasesUpdatedListener;
import com.android.billingclient.api.QueryProductDetailsParams;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdView;
import com.google.android.gms.ads.MobileAds;
import com.google.android.gms.ads.RequestConfiguration;
import com.google.android.material.appbar.MaterialToolbar;
import android.util.TypedValue;

public class MainActivity extends AppCompatActivity implements PurchasesUpdatedListener {
    private List<ShoppingItem> currentList = new ArrayList<>();
    private ShoppingListAdapter adapter;
    private DatabaseHelper db;

    private CurrencyManager currencyManager;
    private String currencySymbol = "$";
    private String selectedColor = null;
    private String selectedCategory = null;
    private boolean adsRemoved = false;
    private BillingClient billingClient;
    private ProductDetails removeAdsProductDetails;

    private static final int THEME_BASE = 0;  // Зеленая (базовая)
    private static final int THEME_ORANGE = 1;
    private static final int THEME_DARK = 2;

    private SharedPreferences prefs;

    // Добавляем константу для имени файла списка
    private static final String CURRENT_LIST_PREFS = "current_shopping_list";

    private final ActivityResultLauncher<String[]> permissionLauncher = registerForActivityResult(
            new ActivityResultContracts.RequestMultiplePermissions(), result -> {
                if (result.get(android.Manifest.permission.READ_EXTERNAL_STORAGE) == Boolean.TRUE &&
                        result.get(android.Manifest.permission.WRITE_EXTERNAL_STORAGE) == Boolean.TRUE) {
                    importList();
                } else {
                    Toast.makeText(this, "Storage permissions denied", Toast.LENGTH_SHORT).show();
                }
            });

    private final ActivityResultLauncher<Intent> importListLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                    Uri uri = result.getData().getData();
                    try {
                        String jsonString = FileUtils.readFileFromUri(this, uri);
                        ShoppingList list = FileUtils.parseMyShoppingFile(jsonString);
                        new AlertDialog.Builder(this)
                                .setTitle("Import List")
                                .setMessage("Loading this list will clear the current list. Continue?")
                                .setPositiveButton("Yes", (dialog, which) -> {
                                    int oldSize = currentList.size();
                                    currentList.clear();
                                    adapter.notifyItemRangeRemoved(0, oldSize);
                                    currentList.addAll(list.getItems());
                                    adapter.notifyItemRangeInserted(0, currentList.size());
                                    currencySymbol = list.getCurrency();
                                    updateTotal();
                                })
                                .setNegativeButton("No", null)
                                .show();
                    } catch (Exception e) {
                        Toast.makeText(this, "Error importing file: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // Инициализируем SharedPreferences
        prefs = getSharedPreferences("app_prefs", MODE_PRIVATE); // Используем "app_prefs"

        // Применяем тему до setContentView
        int theme = prefs.getInt("theme", THEME_BASE);
        // Отладка: выводим текущую тему в лог
        Log.d("MainActivity", "Current theme onCreate: " + theme);

        switch (theme) {
            case THEME_BASE:
                setTheme(R.style.AppTheme_Green);
                Log.d("MainActivity", "Applied theme: AppTheme_Green");
                break;
            case THEME_ORANGE:
                setTheme(R.style.AppTheme_Orange);
                Log.d("MainActivity", "Applied theme: AppTheme_Orange");
                break;
            case THEME_DARK:
                setTheme(R.style.AppTheme_Dark);
                Log.d("MainActivity", "Applied theme: AppTheme_Dark");
                break;
            default:
                setTheme(R.style.AppTheme_Green);
                Log.d("MainActivity", "Applied default theme: AppTheme_Green");
                break;
        }
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        EditText etName = findViewById(R.id.et_name);
        EditText etQuantity = findViewById(R.id.et_quantity);
        EditText etPrice = findViewById(R.id.et_price);
        Button btnAdd = findViewById(R.id.btn_add);
        Button btnRed = findViewById(R.id.btn_red);
        Button btnGreen = findViewById(R.id.btn_green);
        Button btnBlue = findViewById(R.id.btn_blue);
        Button btnPalette = findViewById(R.id.btn_palette);
        Button btnSave = findViewById(R.id.btn_save);
        Button btnClear = findViewById(R.id.btn_clear);
        RecyclerView rvItems = findViewById(R.id.rv_items);
        TextView tvTotal = findViewById(R.id.all_total);
        AdView adView = findViewById(R.id.ad_view);

        db = new DatabaseHelper(this);
        currencyManager = new CurrencyManager(this);
        adsRemoved = prefs.getBoolean("ads_removed", false);

        currencySymbol = currencyManager.getCurrency();

        etQuantity.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);
        etPrice.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);

        // Сначала проверяем savedInstanceState (на случай поворота экрана)
        if (savedInstanceState != null) {
            currentList = savedInstanceState.getParcelableArrayList("current_list");
        } else {
            // Если savedInstanceState нет, пытаемся загрузить из SharedPreferences
            loadListFromPrefs();
        }

        adapter = new ShoppingListAdapter(currentList, this::updateTotal, this::editItem);
        rvItems.setLayoutManager(new LinearLayoutManager(this));
        rvItems.setAdapter(adapter);

        new ItemTouchHelper(new ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT | ItemTouchHelper.RIGHT) {
            @Override
            public boolean onMove(RecyclerView recyclerView, RecyclerView.ViewHolder viewHolder, RecyclerView.ViewHolder target) {
                return false;
            }

            @Override
            public void onSwiped(RecyclerView.ViewHolder viewHolder, int direction) {
                int position = viewHolder.getBindingAdapterPosition();
                currentList.remove(position);
                adapter.notifyItemRemoved(position);
                updateTotal();
            }
        }).attachToRecyclerView(rvItems);

        if (!adsRemoved && adView != null) {
            MobileAds.initialize(this, initializationStatus -> {});
            RequestConfiguration configuration = new RequestConfiguration.Builder()
                    .setTestDeviceIds(Collections.singletonList("0CADD9D9C802D9C0C89C79A6436A454D"))
                    .build();
            MobileAds.setRequestConfiguration(configuration);
            AdRequest adRequest = new AdRequest.Builder().build();
            adView.loadAd(adRequest);
        } else if (adView != null) {
            adView.setVisibility(View.GONE);
        }

        btnRed.setOnClickListener(v -> selectCategory("#FF0000"));
        btnGreen.setOnClickListener(v -> selectCategory("#00FF00"));
        btnBlue.setOnClickListener(v -> selectCategory("#2196F3"));
        btnPalette.setOnClickListener(v -> showPaletteDialog());
        btnAdd.setOnClickListener(v -> addItem(etName, etQuantity, etPrice, rvItems));
        btnSave.setOnClickListener(v -> saveList());
        btnClear.setOnClickListener(v -> clearList());

        ShoppingList importedList = (ShoppingList) getIntent().getSerializableExtra("list");
        if (importedList != null) {
            new AlertDialog.Builder(this)
                    .setTitle("Load List")
                    .setMessage("Loading this list will clear the current list. Continue?")
                    .setPositiveButton("Yes", (dialog, which) -> {
                        int oldSize = currentList.size();
                        currentList.clear();
                        adapter.notifyItemRangeRemoved(0, oldSize);
                        currentList.addAll(importedList.getItems());
                        adapter.notifyItemRangeInserted(0, currentList.size());
                        currencySymbol = importedList.getCurrency();
                        updateTotal();
                    })
                    .setNegativeButton("No", (dialog, which) -> finish())
                    .setCancelable(false)
                    .show();
        } else {
            updateTotal();
        }

        setupBillingClient();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.action_menu) {
            showMenuDialog();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void showMenuDialog() {
        String[] menuItems = {
                getString(R.string.action_themes),
                getString(R.string.action_my_lists),
                getString(R.string.action_share_receipt),
                getString(R.string.action_currency),
                getString(R.string.action_rate),
                getString(R.string.action_share_app),
                getString(R.string.action_remove_ads),
                getString(R.string.action_about),
                getString(R.string.action_import)
        };

        AlertDialog.Builder builder = createThemedDialogBuilder();
        AlertDialog dialog = builder.setTitle("Menu")
                .setItems(menuItems, (dlg, which) -> {
                    switch (which) {
                        case 0: showThemeDialog(); break;
                        case 1: startActivity(new Intent(this, MyListsActivity.class)); break;
                        case 2: shareReceipt(); break;
                        case 3: currencyManager.showCurrencyDialog(this::updateTotal); break;
                        case 4: rateApp(); break;
                        case 5: shareApp(); break;
                        case 6: removeAds(); break;
                        case 7: showAboutDialog(); break;
                        case 8: checkStoragePermissions(); break;
                    }
                })
                .create();
        
        // Получаем цвет текста из атрибутов темы
        TypedValue typedValue = new TypedValue();
        getTheme().resolveAttribute(R.attr.menu_item_textColor, typedValue, true);
        int textColor = typedValue.data;
        
        // Настраиваем цвет текста для списка перед показом диалога
        ListView listView = dialog.getListView();
        if (listView != null) {
            listView.setOnHierarchyChangeListener(new ViewGroup.OnHierarchyChangeListener() {
                @Override
                public void onChildViewAdded(View parent, View child) {
                    if (child instanceof TextView) {
                        // Используем цвет из атрибутов темы
                        ((TextView) child).setTextColor(textColor);
                    }
                }

                @Override
                public void onChildViewRemoved(View parent, View child) {
                    // Ничего не делаем
                }
            });
        }
        
        dialog.show();
    }

    private void setupBillingClient() {
        billingClient = BillingClient.newBuilder(this)
                .setListener(this)
                .enablePendingPurchases(
                        PendingPurchasesParams.newBuilder()
                                .enableOneTimeProducts()
                                .build()
                )
                .build();

        billingClient.startConnection(new BillingClientStateListener() {
            @Override
            public void onBillingSetupFinished(BillingResult billingResult) {
                if (billingResult.getResponseCode() == BillingClient.BillingResponseCode.OK) {
                    queryProductDetails();
                }
            }

            @Override
            public void onBillingServiceDisconnected() {
                billingClient.startConnection(new BillingClientStateListener() {
                    @Override
                    public void onBillingSetupFinished(BillingResult billingResult) {
                        if (billingResult.getResponseCode() == BillingClient.BillingResponseCode.OK) {
                            queryProductDetails();
                        } else {
                            Toast.makeText(MainActivity.this, "Failed to reconnect to billing service", Toast.LENGTH_SHORT).show();
                        }
                    }

                    @Override
                    public void onBillingServiceDisconnected() {
                        // Повторные попытки можно ограничить
                    }
                });
            }
        });
    }

    private void queryProductDetails() {
        List<QueryProductDetailsParams.Product> productList = new ArrayList<>();
        productList.add(QueryProductDetailsParams.Product.newBuilder()
                .setProductId("remove_ads")
                .setProductType(BillingClient.ProductType.INAPP)
                .build());

        QueryProductDetailsParams params = QueryProductDetailsParams.newBuilder()
                .setProductList(productList)
                .build();

        billingClient.queryProductDetailsAsync(params, (billingResult, productDetailsList) -> {
            if (billingResult.getResponseCode() == BillingClient.BillingResponseCode.OK && productDetailsList != null) {
                for (ProductDetails productDetails : productDetailsList) {
                    if ("remove_ads".equals(productDetails.getProductId())) {
                        removeAdsProductDetails = productDetails;
                    }
                }
            } else {
                Toast.makeText(this, "Failed to load product details: " + billingResult.getDebugMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    public void onPurchasesUpdated(BillingResult billingResult, List<Purchase> purchases) {
        if (billingResult.getResponseCode() == BillingClient.BillingResponseCode.OK && purchases != null) {
            for (Purchase purchase : purchases) {
                if (purchase.getProducts().contains("remove_ads") && purchase.getPurchaseState() == Purchase.PurchaseState.PURCHASED) {
                    if (!purchase.isAcknowledged()) {
                        acknowledgePurchase(purchase);
                    } else {
                        handlePurchaseConfirmed();
                    }
                }
            }
        } else if (billingResult.getResponseCode() == BillingClient.BillingResponseCode.USER_CANCELED) {
            Toast.makeText(this, "Purchase canceled", Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(this, "Purchase error: " + billingResult.getDebugMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private void acknowledgePurchase(Purchase purchase) {
        AcknowledgePurchaseParams acknowledgeParams = AcknowledgePurchaseParams.newBuilder()
                .setPurchaseToken(purchase.getPurchaseToken())
                .build();

        billingClient.acknowledgePurchase(acknowledgeParams, billingResult -> {
            if (billingResult.getResponseCode() == BillingClient.BillingResponseCode.OK) {
                handlePurchaseConfirmed();
            } else {
                Toast.makeText(this, "Failed to acknowledge purchase", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void handlePurchaseConfirmed() {
        prefs.edit().putBoolean("ads_removed", true).apply();
        adsRemoved = true;
        AdView adView = findViewById(R.id.ad_view);
        if (adView != null) {
            adView.setVisibility(View.GONE);
        }
        Toast.makeText(this, "Ads removed!", Toast.LENGTH_SHORT).show();
    }

    private void addItem(EditText etName, EditText etQuantity, EditText etPrice, RecyclerView rvItems) {
        String name = etName.getText().toString().trim();
        String quantityStr = etQuantity.getText().toString().trim();
        String priceStr = etPrice.getText().toString().trim();

        if (name.isEmpty()) {
            etName.setError("Название товара не может быть пустым");
            return;
        }

        NumberFormat numberFormat = NumberFormat.getNumberInstance(Locale.getDefault());
        // Устанавливаем формат с двумя знаками после запятой
        numberFormat.setMinimumFractionDigits(2);
        numberFormat.setMaximumFractionDigits(2);
        
        double quantity;
        if (quantityStr.isEmpty()) {
            quantity = 0;
        } else {
            try {
                // Заменяем запятую на точку для правильного парсинга
                String normalizedStr = quantityStr.replace(',', '.');
                quantity = Double.parseDouble(normalizedStr);
                if (quantity < 0) {
                    etQuantity.setError("Количество не может быть отрицательным");
                    return;
                }
            } catch (NumberFormatException e) {
                etQuantity.setError("Некорректное количество (например, 1.5)");
                return;
            }
        }

        double price;
        if (priceStr.isEmpty()) {
            price = 0;
        } else {
            try {
                // Заменяем запятую на точку для правильного парсинга
                String normalizedStr = priceStr.replace(',', '.');
                price = Double.parseDouble(normalizedStr);
                if (price < 0) {
                    etPrice.setError("Цена не может быть отрицательной");
                    return;
                }
            } catch (NumberFormatException e) {
                etPrice.setError("Некорректная цена (например, 10.99)");
                return;
            }
        }

        ShoppingItem item = new ShoppingItem(name, quantity, price, selectedCategory, false);
        item.setColor(selectedColor);
        currentList.add(item);
        adapter.notifyItemInserted(currentList.size() - 1);
        updateTotal();
        clearInputs(etName, etQuantity, etPrice);
        selectedCategory = null;
        selectedColor = null;

        rvItems.scrollToPosition(currentList.size() - 1);
        rvItems.requestFocus();
        hideKeyboard();
    }

    private void editItem(@NonNull Integer position) {
        ShoppingItem item = currentList.get(position);
        EditText etEditName = new EditText(this);
        EditText etEditQuantity = new EditText(this);
        EditText etEditPrice = new EditText(this);
        etEditName.setText(item.getName());
        // Форматируем количество и цену с учётом локали
        NumberFormat numberFormat = NumberFormat.getNumberInstance(Locale.getDefault());
        numberFormat.setMinimumFractionDigits(2);
        numberFormat.setMaximumFractionDigits(2);
        etEditQuantity.setText(numberFormat.format(item.getQuantity()));
        etEditPrice.setText(numberFormat.format(item.getPrice()));

        etEditQuantity.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);
        etEditPrice.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);

        AlertDialog.Builder builder = createThemedDialogBuilder();
        builder.setTitle("Редактировать товар")
                .setView(createEditDialogView(etEditName, etEditQuantity, etEditPrice))
                .setPositiveButton("OK", (dialog, which) -> {
                    String name = etEditName.getText().toString().trim();
                    String quantityStr = etEditQuantity.getText().toString().trim();
                    String priceStr = etEditPrice.getText().toString().trim();

                    if (name.isEmpty()) {
                        Toast.makeText(this, "Название товара не может быть пустым", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    double quantity;
                    if (quantityStr.isEmpty()) {
                        quantity = 0;
                    } else {
                        try {
                            // Заменяем запятую на точку для правильного парсинга
                            String normalizedStr = quantityStr.replace(',', '.');
                            quantity = Double.parseDouble(normalizedStr);
                            if (quantity < 0) {
                                Toast.makeText(this, "Количество не может быть отрицательным", Toast.LENGTH_SHORT).show();
                                return;
                            }
                        } catch (NumberFormatException e) {
                            Toast.makeText(this, "Некорректное количество: введите число (например, 1.5)", Toast.LENGTH_SHORT).show();
                            return;
                        }
                    }

                    double price;
                    if (priceStr.isEmpty()) {
                        price = 0;
                    } else {
                        try {
                            // Заменяем запятую на точку для правильного парсинга
                            String normalizedStr = priceStr.replace(',', '.');
                            price = Double.parseDouble(normalizedStr);
                            if (price < 0) {
                                Toast.makeText(this, "Цена не может быть отрицательной", Toast.LENGTH_SHORT).show();
                                return;
                            }
                        } catch (NumberFormatException e) {
                            Toast.makeText(this, "Некорректная цена: введите число (например, 10.99)", Toast.LENGTH_SHORT).show();
                            return;
                        }
                    }

                    item.setName(name);
                    item.setQuantity(quantity);
                    item.setPrice(price);
                    adapter.notifyItemChanged(position);
                    updateTotal();
                    RecyclerView rvItems = findViewById(R.id.rv_items);
                    rvItems.scrollToPosition(position);
                })
                .setNegativeButton("Отмена", null)
                .show();
    }

    private View createEditDialogView(EditText name, EditText quantity, EditText price) {
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(32, 16, 32, 16);
        name.setHint("Название товара");
        quantity.setHint("Количество");
        price.setHint("Цена");
        layout.addView(name);
        layout.addView(quantity);
        layout.addView(price);
        return layout;
    }

    private void saveList() {
        if (currentList.isEmpty()) {
            Toast.makeText(this, "List is empty", Toast.LENGTH_SHORT).show();
            return;
        }
        EditText input = new EditText(this);
        AlertDialog.Builder builder = createThemedDialogBuilder();
        builder.setTitle("Enter list name")
                .setView(input)
                .setPositiveButton("Save", (dialog, which) -> {
                    String listName = input.getText().toString().trim();
                    if (!listName.isEmpty()) {
                        Cursor cursor = db.getReadableDatabase().rawQuery(
                                "SELECT id FROM shopping_lists WHERE name = ?",
                                new String[]{listName}
                        );
                        if (cursor.moveToFirst()) {
                            AlertDialog.Builder overwriteBuilder = createThemedDialogBuilder();
                            overwriteBuilder.setTitle("List Exists")
                                    .setMessage("A list with this name already exists. Overwrite it?")
                                    .setPositiveButton("Yes", (d, w) -> {
                                        db.saveList(listName, currentList, currencySymbol);
                                        Toast.makeText(this, "Your list has been saved successfully", Toast.LENGTH_SHORT).show();
                                    })
                                    .setNegativeButton("No", null)
                                    .show();
                        } else {
                            db.saveList(listName, currentList, currencySymbol);
                            Toast.makeText(this, "Your list has been saved successfully", Toast.LENGTH_SHORT).show();
                        }
                        cursor.close();
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void clearList() {
        if (!currentList.isEmpty()) {
            AlertDialog.Builder builder = createThemedDialogBuilder();
            builder.setTitle("Clear List")
                    .setMessage("Are you sure you want to clear the list?")
                    .setPositiveButton("Yes", (dialog, which) -> {
                        int size = currentList.size();
                        currentList.clear();
                        adapter.notifyItemRangeRemoved(0, size);
                        updateTotal();
                    })
                    .setNegativeButton("No", null)
                    .show();
        }
    }

    private void selectCategory(String defaultColor) {
        // Проверяем, есть ли уже категория с таким цветом
        for (ShoppingItem item : currentList) {
            if (defaultColor != null && defaultColor.equals(item.getColor()) && item.getCategory() != null 
                    && !item.getCategory().equals("-1") && !item.getCategory().isEmpty()) {
                selectedCategory = item.getCategory();
                selectedColor = defaultColor;
                Toast.makeText(this, "Товар добавлен в категорию \"" + selectedCategory + "\"", Toast.LENGTH_SHORT).show();
                return;
            }
        }
        
        // Если нет категории с таким цветом, запрашиваем имя
        EditText input = new EditText(this);
        AlertDialog.Builder builder = createThemedDialogBuilder();
        builder.setTitle("Назовите категорию")
                .setView(input)
                .setPositiveButton("OK", (dialog, which) -> {
                    String categoryName = input.getText().toString().trim();
                    if (!categoryName.isEmpty()) {
                        selectedCategory = categoryName;
                        selectedColor = defaultColor;
                        Toast.makeText(this, "Создана новая категория \"" + selectedCategory + "\"", Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(this, "Название категории не может быть пустым", Toast.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton("Отмена", (dialog, which) -> {
                    selectedCategory = null;
                    selectedColor = null;
                })
                .show();
    }

    private void showPaletteDialog() {
        String[] colors = {"#FFFF00", "#FF00FF", "#00FFFF", "#FFA500", "#800080", "#008080", "#FFD700", "#FF4500", "#8A2BE2", "#20B2AA"};
        String[] colorNames = {"Yellow", "Magenta", "Cyan", "Orange", "Purple", "Teal", "Gold", "OrangeRed", "Violet", "LightSeaGreen"};
        AlertDialog.Builder builder = createThemedDialogBuilder();
        builder.setTitle("Выбери цвет")
                .setItems(colorNames, (dialog, which) -> selectCategory(colors[which]))
                .show();
    }

    private void updateTotal() {
        double total = 0;
        for (ShoppingItem item : currentList) {
            total += item.getTotal();
        }
        TextView tvTotal = findViewById(R.id.all_total);
        tvTotal.setText(getString(R.string.total_format, total, currencySymbol));
    }

    private void clearInputs(EditText etName, EditText etQuantity, EditText etPrice) {
        etName.setText("");
        etQuantity.setText("");
        etPrice.setText("");
    }

    private void hideKeyboard() {
        InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        View view = getCurrentFocus();
        if (view != null) {
            imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
        }
    }

    private void checkStoragePermissions() {
        if (android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.Q &&
                (ContextCompat.checkSelfPermission(this, android.Manifest.permission.READ_EXTERNAL_STORAGE) != android.content.pm.PackageManager.PERMISSION_GRANTED ||
                        ContextCompat.checkSelfPermission(this, android.Manifest.permission.WRITE_EXTERNAL_STORAGE) != android.content.pm.PackageManager.PERMISSION_GRANTED)) {
            permissionLauncher.launch(new String[]{android.Manifest.permission.READ_EXTERNAL_STORAGE, android.Manifest.permission.WRITE_EXTERNAL_STORAGE});
        } else {
            importList();
        }
    }

    private void showThemeDialog() {
        String[] themes = {"Base (Green)", "Orange", "Dark"};
        AlertDialog.Builder builder = createThemedDialogBuilder();
        AlertDialog dialog = builder.setTitle("Select Theme")
                .setItems(themes, (dlg, which) -> {
                    // Сохраняем новую тему как int
                    prefs.edit().putInt("theme", which).apply();
                    // Отладка: выводим новую тему в лог
                    Log.d("MainActivity", "New theme saved: " + which);

                    // Перезапускаем приложение, чтобы применить тему
                    Intent intent = new Intent(this, MainActivity.class);
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(intent);
                    finish();
                })
                .setNegativeButton("Cancel", null)
                .create();
                
        // Получаем цвет текста из атрибутов темы
        TypedValue typedValue = new TypedValue();
        getTheme().resolveAttribute(R.attr.menu_item_textColor, typedValue, true);
        int textColor = typedValue.data;
        
        // Настраиваем цвет текста для списка перед показом диалога
        ListView listView = dialog.getListView();
        if (listView != null) {
            listView.setOnHierarchyChangeListener(new ViewGroup.OnHierarchyChangeListener() {
                @Override
                public void onChildViewAdded(View parent, View child) {
                    if (child instanceof TextView) {
                        // Используем цвет из атрибутов темы
                        ((TextView) child).setTextColor(textColor);
                    }
                }

                @Override
                public void onChildViewRemoved(View parent, View child) {
                    // Ничего не делаем
                }
            });
        }
        
        dialog.show();
    }

    private void shareReceipt() {
        StringBuilder receipt = new StringBuilder("Shopping List:\n");
        for (ShoppingItem item : currentList) {
            receipt.append(String.format(Locale.getDefault(), "%s | %.2f | %.2f | %.2f %s\n",
                    item.getName(), item.getQuantity(), item.getPrice(), item.getTotal(), currencySymbol));
        }
        receipt.append("Total: ").append(String.format(Locale.getDefault(), "%.2f", getTotal()));
        Intent shareIntent = new Intent(Intent.ACTION_SEND);
        shareIntent.setType("text/plain");
        shareIntent.putExtra(Intent.EXTRA_TEXT, receipt.toString());
        startActivity(Intent.createChooser(shareIntent, "Share Receipt"));
    }

    private double getTotal() {
        double total = 0;
        for (ShoppingItem item : currentList) {
            total += item.getTotal();
        }
        return total;
    }

    private void shareApp() {
        Intent shareIntent = new Intent(Intent.ACTION_SEND);
        shareIntent.setType("text/plain");
        shareIntent.putExtra(Intent.EXTRA_TEXT, "Check out MyShopping app! Download it here: [your app link]");
        startActivity(Intent.createChooser(shareIntent, "Share App"));
    }

    private void rateApp() {
        Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=" + getPackageName()));
        try {
            startActivity(intent);
        } catch (Exception e) {
            startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/details?id=" + getPackageName())));
        }
    }

    private void removeAds() {
        if (adsRemoved) {
            Toast.makeText(this, "Ads already removed", Toast.LENGTH_SHORT).show();
            return;
        }
        if (billingClient.isReady()) {
            if (removeAdsProductDetails != null) {
                BillingFlowParams.ProductDetailsParams productDetailsParams = BillingFlowParams.ProductDetailsParams.newBuilder()
                        .setProductDetails(removeAdsProductDetails)
                        .build();
                BillingFlowParams billingFlowParams = BillingFlowParams.newBuilder()
                        .setProductDetailsParamsList(Collections.singletonList(productDetailsParams))
                        .build();
                billingClient.launchBillingFlow(this, billingFlowParams);
            } else {
                Toast.makeText(this, "Product details not loaded, try again later", Toast.LENGTH_SHORT).show();
            }
        } else {
            Toast.makeText(this, "Billing client not ready, try again later", Toast.LENGTH_SHORT).show();
        }
    }

    private void showAboutDialog() {
        AlertDialog.Builder builder = createThemedDialogBuilder();
        builder.setTitle("About MyShopping")
                .setMessage("Version 1.0\nCreated by [Your Name]")
                .setPositiveButton("OK", null)
                .show();
    }

    private AlertDialog.Builder createThemedDialogBuilder() {
        int theme = prefs.getInt("theme", THEME_BASE);
        Log.d("MainActivity", "Creating themed dialog with theme: " + theme);

        switch (theme) {
            case THEME_BASE:
                return new AlertDialog.Builder(this, R.style.AlertDialogTheme_Base);
            case THEME_ORANGE:
                return new AlertDialog.Builder(this, R.style.AlertDialogTheme_Orange);
            case THEME_DARK:
                return new AlertDialog.Builder(this, R.style.AlertDialogTheme_Dark);
            default:
                return new AlertDialog.Builder(this, R.style.AlertDialogTheme_Base);
        }
    }

    private void importList() {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("*/*");
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        importListLauncher.launch(Intent.createChooser(intent, "Select .myshopping file"));
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putParcelableArrayList("current_list", new ArrayList<>(currentList));
    }

    @Override
    protected void onPause() {
        super.onPause();
        // Сохраняем текущий список при приостановке активности
        saveListToPrefs();
    }

    @Override
    protected void onResume() {
        super.onResume();
        // При необходимости можно обновить UI здесь
        updateTotal();
    }

    @Override
    protected void onDestroy() {
        if (billingClient != null) {
            billingClient.endConnection();
        }
        super.onDestroy();
    }

    // Метод для сохранения списка в SharedPreferences
    private void saveListToPrefs() {
        try {
            SharedPreferences.Editor editor = prefs.edit();
            
            // Сохраняем количество элементов
            editor.putInt(CURRENT_LIST_PREFS + "_size", currentList.size());
            
            // Сохраняем каждый элемент
            for (int i = 0; i < currentList.size(); i++) {
                ShoppingItem item = currentList.get(i);
                String prefix = CURRENT_LIST_PREFS + "_item_" + i;
                
                editor.putString(prefix + "_name", item.getName());
                editor.putFloat(prefix + "_quantity", (float) item.getQuantity());
                editor.putFloat(prefix + "_price", (float) item.getPrice());
                editor.putString(prefix + "_category", item.getCategory());
                editor.putBoolean(prefix + "_checked", item.isChecked());
                editor.putString(prefix + "_color", item.getColor() != null ? item.getColor() : "");
            }
            
            // Сохраняем валюту
            editor.putString(CURRENT_LIST_PREFS + "_currency", currencySymbol);
            
            editor.apply();
            Log.d("MainActivity", "Список сохранен, элементов: " + currentList.size());
        } catch (Exception e) {
            Log.e("MainActivity", "Ошибка при сохранении списка: " + e.getMessage());
        }
    }
    
    // Метод для загрузки списка из SharedPreferences
    private void loadListFromPrefs() {
        try {
            // Получаем количество элементов
            int size = prefs.getInt(CURRENT_LIST_PREFS + "_size", 0);
            
            // Если список не пустой, загружаем элементы
            if (size > 0) {
                currentList.clear();
                
                for (int i = 0; i < size; i++) {
                    String prefix = CURRENT_LIST_PREFS + "_item_" + i;
                    
                    String name = prefs.getString(prefix + "_name", "");
                    double quantity = prefs.getFloat(prefix + "_quantity", 0);
                    double price = prefs.getFloat(prefix + "_price", 0);
                    String category = prefs.getString(prefix + "_category", null);
                    boolean checked = prefs.getBoolean(prefix + "_checked", false);
                    String color = prefs.getString(prefix + "_color", "");
                    
                    ShoppingItem item = new ShoppingItem(name, quantity, price, category, checked);
                    if (!color.isEmpty()) {
                        item.setColor(color);
                    }
                    
                    currentList.add(item);
                }
                
                // Загружаем валюту
                currencySymbol = prefs.getString(CURRENT_LIST_PREFS + "_currency", "$");
                
                Log.d("MainActivity", "Список загружен, элементов: " + currentList.size());
            }
        } catch (Exception e) {
            Log.e("MainActivity", "Ошибка при загрузке списка: " + e.getMessage());
        }
    }
}