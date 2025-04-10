package tm.davletov.myshopping;

import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.pdf.PdfDocument;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.ContextMenu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.appbar.MaterialToolbar;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class MyListsActivity extends AppCompatActivity implements MyListsAdapter.OnListClickListener, MyListsAdapter.OnCategoryClickListener {
    private RecyclerView recyclerView;
    private MyListsAdapter adapter;
    private DatabaseHelper dbHelper;
    private SharedPreferences prefs;

    // Переменная для хранения позиции элемента, выбранного в контекстном меню
    private int contextMenuPosition = -1;

    // Константы для SAF
    private static final int REQUEST_CODE_SAVE_JPG = 101;
    private static final int REQUEST_CODE_SAVE_PDF = 102;
    private static final int REQUEST_CODE_SAVE_MYSHOPPING = 103;

    // Переменная для хранения текущего списка, который экспортируется
    private ShoppingList currentExportList;

    private static final int THEME_BASE = 0;  // Зеленая (базовая)
    private static final int THEME_ORANGE = 1;
    private static final int THEME_DARK = 2;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // Применяем тему до setContentView
        prefs = getSharedPreferences("app_prefs", MODE_PRIVATE);
        int theme = prefs.getInt("theme", THEME_BASE);
        
        switch (theme) {
            case THEME_BASE:
                setTheme(R.style.AppTheme_Green);
                break;
            case THEME_ORANGE:
                setTheme(R.style.AppTheme_Orange);
                break;
            case THEME_DARK:
                setTheme(R.style.AppTheme_Dark);
                break;
            default:
                setTheme(R.style.AppTheme_Green);
                break;
        }

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_my_lists);

        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setTitle(R.string.my_lists);

        recyclerView = findViewById(R.id.recyclerView);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        
        dbHelper = new DatabaseHelper(this);
        adapter = new MyListsAdapter(this, this, this);
        recyclerView.setAdapter(adapter);
        
        // Регистрируем RecyclerView для контекстного меню
        registerForContextMenu(recyclerView);

        loadLists();
    }

    private void loadLists() {
        List<ShoppingList> lists = dbHelper.getAllLists();
        if (lists.isEmpty()) {
            Toast.makeText(this, R.string.no_lists_found, Toast.LENGTH_SHORT).show();
        }
        adapter.setData(lists);
    }

    @Override
    public void onListClick(ShoppingList list) {
        showListDetails(list);
    }

    @Override
    public void onCategoryClick(String category, List<ShoppingItem> items) {
        showCategoryItems(category, items);
    }

    private void showListDetails(@NonNull ShoppingList list) {
        StringBuilder details = new StringBuilder();
        details.append(getString(R.string.list_label, list.getName())).append("\n");
        details.append(getString(R.string.date_label, list.getDateTime())).append("\n\n");
        
        double total = 0;
        for (ShoppingItem item : list.getItems()) {
            details.append(String.format("%s - %.2f x %.2f = %.2f\n",
                    item.getName(), item.getQuantity(), item.getPrice(), item.getTotal()));
            total += item.getTotal();
        }
        details.append("\n").append(getString(R.string.total_label, total));

        new AlertDialog.Builder(this)
                .setTitle(list.getName())
                .setMessage(details.toString())
                .setPositiveButton(android.R.string.ok, null)
                .setNeutralButton(R.string.edit_list, (dialog, which) -> {
                    Intent intent = new Intent(this, MainActivity.class);
                    intent.putExtra("list", list);
                    startActivity(intent);
                })
                .show();
    }

    private void showCategoryItems(String category, List<ShoppingItem> items) {
        StringBuilder details = new StringBuilder();
        details.append(getString(R.string.category_items_title, category)).append("\n\n");
        
        double total = 0;
        for (ShoppingItem item : items) {
            details.append(String.format("%s - %.2f x %.2f = %.2f\n",
                    item.getName(), item.getQuantity(), item.getPrice(), item.getTotal()));
            total += item.getTotal();
        }
        details.append("\n").append(getString(R.string.total_label, total));

        new AlertDialog.Builder(this)
                .setTitle(category)
                .setMessage(details.toString())
                .setPositiveButton(android.R.string.ok, null)
                .show();
    }

    @Override
    public void onCreateContextMenu(@NonNull ContextMenu menu, @NonNull View v, ContextMenu.ContextMenuInfo menuInfo) {
        super.onCreateContextMenu(menu, v, menuInfo);
        // Инфлейтим меню здесь, чтобы оно не дублировалось
        getMenuInflater().inflate(R.menu.context_menu, menu);
        
        // Заголовок меню устанавливается в адаптере
    }

    // Метод для установки позиции выбранного элемента
    public void setContextMenuPosition(int position) {
        this.contextMenuPosition = position;
    }

    @Override
    public boolean onContextItemSelected(@NonNull MenuItem item) {
        // Используем сохраненную позицию
        if (contextMenuPosition == -1) {
            return super.onContextItemSelected(item);
        }
        
        ShoppingList list = adapter.getItem(contextMenuPosition);
        if (list == null) {
            return super.onContextItemSelected(item);
        }

        int id = item.getItemId();
        if (id == R.id.action_view) {
            showListDetails(list);
            return true;
        } else if (id == R.id.action_show) {
            showList(list);
            return true;
        } else if (id == R.id.action_copy) {
            copyList(list);
            return true;
        } else if (id == R.id.action_edit) {
            editList(list);
            return true;
        } else if (id == R.id.action_delete) {
            deleteList(list);
            return true;
        } else if (id == R.id.action_share) {
            shareList(list);
            return true;
        } else if (id == R.id.action_export) {
            showExportDialog(list);
            return true;
        }
        return super.onContextItemSelected(item);
    }

    private void showExportDialog(@NonNull ShoppingList list) {
        currentExportList = list;
        String[] options = {
                getString(R.string.export_to_jpg),
                getString(R.string.export_to_pdf),
                getString(R.string.export_to_myshopping)
        };
        AlertDialog.Builder builder = createThemedDialogBuilder();
        builder.setTitle(R.string.export_as_title)
                .setItems(options, (dialog, which) -> {
                    switch (which) {
                        case 0:
                            exportWithSAF("image/jpeg", list.getName() + ".jpg", REQUEST_CODE_SAVE_JPG);
                            break;
                        case 1:
                            exportWithSAF("application/pdf", list.getName() + ".pdf", REQUEST_CODE_SAVE_PDF);
                            break;
                        case 2:
                            exportWithSAF("application/octet-stream", list.getName() + ".myshopping", REQUEST_CODE_SAVE_MYSHOPPING);
                            break;
                    }
                })
                .setNegativeButton(android.R.string.cancel, (dialog, which) -> currentExportList = null)
                .show();
    }

    private void showList(ShoppingList list) {
        Intent intent = new Intent(this, MainActivity.class);
        intent.putExtra("list", list);
        startActivity(intent);
    }

    private void copyList(@NonNull ShoppingList list) {
        String newName = list.getName() + " " + getString(R.string.copy_suffix);
        dbHelper.saveList(newName, list.getItems(), list.getCurrency());
        loadLists();
    }

    private void editList(@NonNull ShoppingList list) {
        AlertDialog.Builder builder = createThemedDialogBuilder();
        builder.setTitle(getString(R.string.edit_list_title, list.getName()));

        View dialogView = getLayoutInflater().inflate(R.layout.dialog_edit_list, null);
        EditText etListName = dialogView.findViewById(R.id.et_list_name);
        RecyclerView rvItems = dialogView.findViewById(R.id.rv_edit_items);
        Button btnAddItem = dialogView.findViewById(R.id.btn_add_item);

        etListName.setText(list.getName());
        ShoppingListAdapter adapter = new ShoppingListAdapter(list.getItems(), () -> {}, pos -> editItemInDialog(list.getItems(), pos, rvItems));
        rvItems.setLayoutManager(new LinearLayoutManager(this));
        rvItems.setAdapter(adapter);

        new ItemTouchHelper(new ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT | ItemTouchHelper.RIGHT) {
            @Override
            public boolean onMove(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder, @NonNull RecyclerView.ViewHolder target) {
                return false;
            }

            @Override
            public void onSwiped(@NonNull RecyclerView.ViewHolder viewHolder, int direction) {
                int position = viewHolder.getAdapterPosition();
                ShoppingItem item = list.getItems().get(position);
                AlertDialog.Builder deleteBuilder = createThemedDialogBuilder();
                deleteBuilder.setTitle(R.string.delete_item_title)
                        .setMessage(getString(R.string.delete_item_message, item.getName()))
                        .setPositiveButton(android.R.string.yes, (dialog, which) -> {
                            list.getItems().remove(position);
                            adapter.notifyItemRemoved(position);
                        })
                        .setNegativeButton(android.R.string.no, (dialog, which) -> adapter.notifyItemChanged(position))
                        .setOnCancelListener(dialog -> adapter.notifyItemChanged(position))
                        .show();
            }
        }).attachToRecyclerView(rvItems);

        btnAddItem.setOnClickListener(v -> addItemInDialog(list.getItems(), rvItems));

        builder.setView(dialogView)
                .setPositiveButton(R.string.save, (dialog, which) -> {
                    String newName = etListName.getText().toString().trim();
                    if (!newName.isEmpty()) {
                        dbHelper.deleteList(list.getId());
                        dbHelper.saveList(newName, list.getItems(), list.getCurrency());
                        loadLists();
                        Toast.makeText(this, R.string.list_updated, Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(this, R.string.list_name_empty, Toast.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton(android.R.string.cancel, null)
                .show();
    }

    private void editItemInDialog(@NonNull List<ShoppingItem> items, int position, @NonNull RecyclerView rvItems) {
        ShoppingItem item = items.get(position);
        EditText etEditName = new EditText(this);
        EditText etEditQuantity = new EditText(this);
        EditText etEditPrice = new EditText(this);
        EditText etEditCategory = new EditText(this);
        Button btnEditColor = new Button(this);
        etEditName.setText(item.getName());
        etEditQuantity.setText(String.valueOf(item.getQuantity()));
        etEditPrice.setText(String.valueOf(item.getPrice()));
        etEditCategory.setText(item.getCategory() != null ? item.getCategory() : "");
        btnEditColor.setText(R.string.select_color);
        if (item.getColor() != null) {
            btnEditColor.setBackgroundColor(Color.parseColor(item.getColor()));
        }

        btnEditColor.setOnClickListener(v -> showColorPickerDialog(item, btnEditColor, rvItems, position));

        AlertDialog.Builder builder = createThemedDialogBuilder();
        builder.setTitle(R.string.edit_item_title)
                .setView(createEditItemDialogView(etEditName, etEditQuantity, etEditPrice, etEditCategory, btnEditColor))
                .setPositiveButton(R.string.save, (dialog, which) -> {
                    try {
                        String name = etEditName.getText().toString().trim();
                        double quantity = Double.parseDouble(etEditQuantity.getText().toString().trim());
                        double price = Double.parseDouble(etEditPrice.getText().toString().trim());
                        String category = etEditCategory.getText().toString().trim();
                        if (!name.isEmpty()) {
                            item.setName(name);
                            item.setQuantity(quantity);
                            item.setPrice(price);
                            item.setCategory(category.isEmpty() ? null : category);
                            rvItems.getAdapter().notifyItemChanged(position);
                        }
                    } catch (NumberFormatException e) {
                        Toast.makeText(this, R.string.invalid_input, Toast.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton(android.R.string.cancel, null)
                .show();
    }

    private void addItemInDialog(@NonNull List<ShoppingItem> items, @NonNull RecyclerView rvItems) {
        EditText etEditName = new EditText(this);
        EditText etEditQuantity = new EditText(this);
        EditText etEditPrice = new EditText(this);
        EditText etEditCategory = new EditText(this);
        Button btnEditColor = new Button(this);
        etEditName.setHint(R.string.name_hint);
        etEditQuantity.setHint(R.string.quantity_hint);
        etEditPrice.setHint(R.string.price_hint);
        etEditCategory.setHint(R.string.category_hint);
        btnEditColor.setText(R.string.select_color);

        String[] selectedColor = {null};
        btnEditColor.setOnClickListener(v -> showColorPickerDialog(null, btnEditColor, rvItems, -1, color -> selectedColor[0] = color));

        AlertDialog.Builder builder = createThemedDialogBuilder();
        builder.setTitle(R.string.add_item_title)
                .setView(createEditItemDialogView(etEditName, etEditQuantity, etEditPrice, etEditCategory, btnEditColor))
                .setPositiveButton(R.string.add, (dialog, which) -> {
                    try {
                        String name = etEditName.getText().toString().trim();
                        double quantity = Double.parseDouble(etEditQuantity.getText().toString().trim());
                        double price = Double.parseDouble(etEditPrice.getText().toString().trim());
                        String category = etEditCategory.getText().toString().trim();
                        if (!name.isEmpty()) {
                            ShoppingItem newItem = new ShoppingItem(name, quantity, price, category.isEmpty() ? null : category, false);
                            newItem.setColor(selectedColor[0]);
                            items.add(newItem);
                            rvItems.getAdapter().notifyItemInserted(items.size() - 1);
                        }
                    } catch (NumberFormatException e) {
                        Toast.makeText(this, R.string.invalid_input, Toast.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton(android.R.string.cancel, null)
                .show();
    }

    private View createEditItemDialogView(@NonNull EditText name, @NonNull EditText quantity, @NonNull EditText price, @NonNull EditText category, @NonNull Button colorButton) {
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(16, 0, 16, 0);
        layout.addView(name);
        layout.addView(quantity);
        layout.addView(price);
        layout.addView(category);
        layout.addView(colorButton);
        return layout;
    }

    private void showColorPickerDialog(@NonNull ShoppingItem item, @NonNull Button colorButton, @NonNull RecyclerView rvItems, int position) {
        showColorPickerDialog(item, colorButton, rvItems, position, color -> {
            item.setColor(color);
            rvItems.getAdapter().notifyItemChanged(position);
            colorButton.setBackgroundColor(Color.parseColor(color));
        });
    }

    private void showColorPickerDialog(ShoppingItem item, @NonNull Button colorButton, RecyclerView rvItems, int position, @NonNull ColorSelectedListener listener) {
        String[] colors = {"#FF0000", "#00FF00", "#0000FF", "#FFFF00", "#FF00FF", "#00FFFF", "#FFA500", "#800080", "#008080", "#FFD700"};
        AlertDialog.Builder builder = createThemedDialogBuilder();
        builder.setTitle(R.string.select_color)
                .setItems(colors, (dialog, which) -> {
                    String selectedColor = colors[which];
                    listener.onColorSelected(selectedColor);
                })
                .show();
    }

    interface ColorSelectedListener {
        void onColorSelected(String color);
    }

    private void deleteList(@NonNull ShoppingList list) {
        dbHelper.deleteList(list.getId());
        loadLists();
    }

    private void shareList(@NonNull ShoppingList list) {
        StringBuilder text = new StringBuilder();
        text.append(getString(R.string.list_label, list.getName())).append("\n");
        for (ShoppingItem item : list.getItems()) {
            text.append(String.format(Locale.getDefault(), "%s - %.2f x %.2f\n", item.getName(), item.getQuantity(), item.getPrice()));
        }
        Intent shareIntent = new Intent(Intent.ACTION_SEND);
        shareIntent.setType("text/plain");
        shareIntent.putExtra(Intent.EXTRA_TEXT, text.toString());
        startActivity(Intent.createChooser(shareIntent, getString(R.string.share_list_title)));
    }

    private void exportWithSAF(String mimeType, String fileName, int requestCode) {
        String timestamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        String finalFileName = fileName.replace(".jpg", "_" + timestamp + ".jpg")
                .replace(".pdf", "_" + timestamp + ".pdf")
                .replace(".myshopping", "_" + timestamp + ".myshopping");

        Intent intent = new Intent(Intent.ACTION_CREATE_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType(mimeType);
        intent.putExtra(Intent.EXTRA_TITLE, finalFileName);
        startActivityForResult(intent, requestCode);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == RESULT_OK && data != null && currentExportList != null) {
            Uri uri = data.getData();
            if (uri != null) {
                try (OutputStream out = getContentResolver().openOutputStream(uri)) {
                    byte[] fileData = getFileDataForExport(requestCode);
                    if (fileData != null) {
                        out.write(fileData);
                        out.flush();
                        Toast.makeText(this, "File saved successfully", Toast.LENGTH_LONG).show();
                    } else {
                        Toast.makeText(this, "Failed to generate file data", Toast.LENGTH_LONG).show();
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                    Toast.makeText(this, getString(R.string.export_failed, e.getMessage()), Toast.LENGTH_LONG).show();
                }
            }
            currentExportList = null; // Сбрасываем после экспорта
        }
    }

    private byte[] getFileDataForExport(int requestCode) {
        if (currentExportList == null) return null;

        try {
            if (requestCode == REQUEST_CODE_SAVE_JPG) {
                return exportToJPG(currentExportList);
            } else if (requestCode == REQUEST_CODE_SAVE_PDF) {
                return exportToPDF(currentExportList);
            } else if (requestCode == REQUEST_CODE_SAVE_MYSHOPPING) {
                return exportToMyShopping(currentExportList);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    private byte[] exportToJPG(@NonNull ShoppingList list) {
        int height = 150 + list.getItems().size() * 50;
        Bitmap bitmap = Bitmap.createBitmap(500, height, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        Paint paint = new Paint();
        paint.setAntiAlias(true);
        paint.setTextSize(20);
        paint.setColor(Color.BLACK);

        canvas.drawColor(Color.WHITE);
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(2);
        canvas.drawRect(10, 10, 490, height - 10, paint);
        paint.setStyle(Paint.Style.FILL);

        paint.setTextSize(24);
        paint.setFakeBoldText(true);
        canvas.drawText(getString(R.string.list_label, list.getName()), 20, 40, paint);
        paint.setTextSize(16);
        paint.setFakeBoldText(false);
        canvas.drawText(getString(R.string.date_label, list.getDateTime()), 20, 70, paint);

        int y = 110;
        paint.setTextSize(18);
        for (ShoppingItem item : list.getItems()) {
            String text = String.format(Locale.getDefault(), "%s - %.2f x %.2f = %.2f %s",
                    item.getName(), item.getQuantity(), item.getPrice(), item.getTotal(), list.getCurrency());
            if (item.getColor() != null) {
                paint.setColor(Color.parseColor(item.getColor()));
            } else {
                paint.setColor(Color.BLACK);
            }
            canvas.drawText(text, 20, y, paint);
            y += 30;
        }

        java.io.ByteArrayOutputStream stream = new java.io.ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.JPEG, 100, stream);
        return stream.toByteArray();
    }

    private byte[] exportToPDF(@NonNull ShoppingList list) throws IOException {
        PdfDocument document = new PdfDocument();
        int pageHeight = 150 + list.getItems().size() * 30;
        PdfDocument.PageInfo pageInfo = new PdfDocument.PageInfo.Builder(300, pageHeight, 1).create();
        PdfDocument.Page page = document.startPage(pageInfo);
        Canvas canvas = page.getCanvas();
        Paint paint = new Paint();
        paint.setAntiAlias(true);
        paint.setColor(Color.BLACK);

        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(1);
        canvas.drawRect(10, 10, 290, pageHeight - 10, paint);
        paint.setStyle(Paint.Style.FILL);

        paint.setTextSize(16);
        paint.setFakeBoldText(true);
        canvas.drawText(getString(R.string.list_label, list.getName()), 15, 30, paint);
        paint.setTextSize(12);
        paint.setFakeBoldText(false);
        canvas.drawText(getString(R.string.date_label, list.getDateTime()), 15, 50, paint);

        int y = 80;
        paint.setTextSize(12);
        for (ShoppingItem item : list.getItems()) {
            String text = String.format(Locale.getDefault(), "%s - %.2f x %.2f = %.2f %s",
                    item.getName(), item.getQuantity(), item.getPrice(), item.getTotal(), list.getCurrency());
            if (item.getColor() != null) {
                paint.setColor(Color.parseColor(item.getColor()));
            } else {
                paint.setColor(Color.BLACK);
            }
            canvas.drawText(text, 15, y, paint);
            y += 20;
        }

        document.finishPage(page);
        java.io.ByteArrayOutputStream stream = new java.io.ByteArrayOutputStream();
        document.writeTo(stream);
        document.close();
        return stream.toByteArray();
    }

    private byte[] exportToMyShopping(@NonNull ShoppingList list) {
        if (list.getItems().size() > 100) {
            Toast.makeText(this, R.string.list_exceeds_100_items, Toast.LENGTH_LONG).show();
            return null;
        }
        JSONObject json = new JSONObject();
        try {
            json.put("list_name", list.getName());
            json.put("date_time", list.getDateTime());
            json.put("currency", list.getCurrency());
            JSONArray items = new JSONArray();
            for (int i = 0; i < Math.min(list.getItems().size(), 100); i++) {
                ShoppingItem item = list.getItems().get(i);
                JSONObject itemJson = new JSONObject();
                itemJson.put("name", item.getName());
                itemJson.put("quantity", item.getQuantity());
                itemJson.put("price", item.getPrice());
                itemJson.put("total", item.getTotal());
                itemJson.put("category", item.getCategory());
                itemJson.put("color", item.getColor());
                itemJson.put("is_purchased", item.isPurchased());
                items.put(itemJson);
            }
            json.put("items", items);
            return json.toString().getBytes();
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    private AlertDialog.Builder createThemedDialogBuilder() {
        int theme = prefs.getInt("theme", THEME_BASE);
        Log.d("MyListsActivity", "Creating themed dialog with theme: " + theme);

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

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (dbHelper != null) {
            dbHelper.close();
        }
    }
}