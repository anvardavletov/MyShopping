package tm.davletov.myshopping;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class DatabaseHelper extends SQLiteOpenHelper {
    private static final String DATABASE_NAME = "MyShopping.db";
    private static final int DATABASE_VERSION = 2;

    // Таблицы
    private static final String TABLE_LISTS = "shopping_lists";
    private static final String TABLE_ITEMS = "shopping_items";
    private static final String TABLE_CATEGORIES = "categories";

    // Общие столбцы
    private static final String COLUMN_ID = "id";
    private static final String COLUMN_NAME = "name";
    
    // Столбцы для таблицы списков
    private static final String COLUMN_DATE = "date";
    private static final String COLUMN_CURRENCY = "currency";

    // Столбцы для таблицы элементов
    private static final String COLUMN_LIST_ID = "list_id";
    private static final String COLUMN_QUANTITY = "quantity";
    private static final String COLUMN_PRICE = "price";
    private static final String COLUMN_CHECKED = "checked";
    private static final String COLUMN_COLOR = "color";
    private static final String COLUMN_CATEGORY_ID = "category_id";
    
    // Столбцы для таблицы категорий
    private static final String COLUMN_ICON = "icon";

    public DatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        // Создание таблицы категорий
        String createCategoriesTable = "CREATE TABLE " + TABLE_CATEGORIES + "("
                + COLUMN_ID + " INTEGER PRIMARY KEY AUTOINCREMENT,"
                + COLUMN_NAME + " TEXT NOT NULL,"
                + COLUMN_COLOR + " TEXT,"
                + COLUMN_ICON + " INTEGER"
                + ")";
        db.execSQL(createCategoriesTable);
        
        // Создание таблицы списков
        String createListsTable = "CREATE TABLE " + TABLE_LISTS + "("
                + COLUMN_ID + " INTEGER PRIMARY KEY AUTOINCREMENT,"
                + COLUMN_NAME + " TEXT NOT NULL,"
                + COLUMN_DATE + " TEXT,"
                + COLUMN_CURRENCY + " TEXT"
                + ")";
        db.execSQL(createListsTable);

        // Создание таблицы элементов с внешним ключом на категорию
        String createItemsTable = "CREATE TABLE " + TABLE_ITEMS + "("
                + COLUMN_ID + " INTEGER PRIMARY KEY AUTOINCREMENT,"
                + COLUMN_LIST_ID + " INTEGER,"
                + COLUMN_NAME + " TEXT NOT NULL,"
                + COLUMN_QUANTITY + " REAL DEFAULT 1,"
                + COLUMN_PRICE + " REAL DEFAULT 0,"
                + COLUMN_CHECKED + " INTEGER DEFAULT 0,"
                + COLUMN_COLOR + " TEXT,"
                + COLUMN_CATEGORY_ID + " INTEGER,"
                + "FOREIGN KEY(" + COLUMN_LIST_ID + ") REFERENCES " + TABLE_LISTS + "(" + COLUMN_ID + "),"
                + "FOREIGN KEY(" + COLUMN_CATEGORY_ID + ") REFERENCES " + TABLE_CATEGORIES + "(" + COLUMN_ID + ")"
                + ")";
        db.execSQL(createItemsTable);
        
        // Добавление предустановленных категорий
        addDefaultCategories(db);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        if (oldVersion < 2) {
            // Создание таблицы категорий
            String createCategoriesTable = "CREATE TABLE " + TABLE_CATEGORIES + "("
                    + COLUMN_ID + " INTEGER PRIMARY KEY AUTOINCREMENT,"
                    + COLUMN_NAME + " TEXT NOT NULL,"
                    + COLUMN_COLOR + " TEXT,"
                    + COLUMN_ICON + " INTEGER"
                    + ")";
            db.execSQL(createCategoriesTable);
            
            // Добавление столбца category_id в таблицу элементов
            db.execSQL("ALTER TABLE " + TABLE_ITEMS + " ADD COLUMN " + COLUMN_CATEGORY_ID + " INTEGER");
            
            // Добавление предустановленных категорий
            addDefaultCategories(db);
        }
    }
    
    // Добавление предустановленных категорий
    private void addDefaultCategories(SQLiteDatabase db) {
        String[] defaultCategoryNames = {"Продукты", "Напитки", "Бытовая химия", "Хлебобулочные", "Молочные", "Мясные"};
        String[] defaultCategoryColors = {"#FF5733", "#33FF57", "#3357FF", "#FFFF33", "#FF33FF", "#33FFFF"};
        int[] defaultCategoryIcons = {0, 0, 0, 0, 0, 0}; // Здесь можно добавить идентификаторы иконок
        
        for (int i = 0; i < defaultCategoryNames.length; i++) {
            ContentValues values = new ContentValues();
            values.put(COLUMN_NAME, defaultCategoryNames[i]);
            values.put(COLUMN_COLOR, defaultCategoryColors[i]);
            values.put(COLUMN_ICON, defaultCategoryIcons[i]);
            db.insert(TABLE_CATEGORIES, null, values);
        }
    }

    public void saveList(String name, List<ShoppingItem> items, String currency) {
        SQLiteDatabase db = this.getWritableDatabase();
        String currentDateTime = new SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault()).format(new Date());

        ContentValues listValues = new ContentValues();
        listValues.put(COLUMN_NAME, name);
        listValues.put(COLUMN_DATE, currentDateTime);
        listValues.put(COLUMN_CURRENCY, currency);
        long listId = db.insert(TABLE_LISTS, null, listValues);

        for (ShoppingItem item : items) {
            ContentValues itemValues = new ContentValues();
            itemValues.put(COLUMN_LIST_ID, listId);
            itemValues.put(COLUMN_NAME, item.getName());
            itemValues.put(COLUMN_QUANTITY, item.getQuantity());
            itemValues.put(COLUMN_PRICE, item.getPrice());
            itemValues.put(COLUMN_CATEGORY_ID, item.getCategoryId());
            itemValues.put(COLUMN_COLOR, item.getColor());
            itemValues.put(COLUMN_CHECKED, item.isPurchased() ? 1 : 0);
            db.insert(TABLE_ITEMS, null, itemValues);
        }
        db.close();
    }

    public List<ShoppingList> getAllLists() {
        List<ShoppingList> lists = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();
        
        Cursor cursor = db.rawQuery("SELECT * FROM " + TABLE_LISTS + " ORDER BY " + COLUMN_DATE + " DESC", null);
        if (cursor.moveToFirst()) {
            do {
                int id = cursor.getInt(0);
                String name = cursor.getString(1);
                String dateTime = cursor.getString(2);
                String currency = cursor.getString(3);
                List<ShoppingItem> items = getItemsForList(id);
                lists.add(new ShoppingList(id, name, dateTime, currency, items));
            } while (cursor.moveToNext());
        }
        cursor.close();
        db.close();
        return lists;
    }

    private List<ShoppingItem> getItemsForList(int listId) {
        List<ShoppingItem> items = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();
        
        Cursor cursor = db.rawQuery("SELECT * FROM " + TABLE_ITEMS + " WHERE " + COLUMN_LIST_ID + " = ?", new String[]{String.valueOf(listId)});
        if (cursor.moveToFirst()) {
            do {
                String name = cursor.getString(2);
                double quantity = cursor.getDouble(3);
                double price = cursor.getDouble(4);
                String category = cursor.getString(5);
                String color = cursor.getString(6);
                boolean isPurchased = cursor.getInt(7) == 1;
                
                ShoppingItem item = new ShoppingItem(name, quantity, price, category, isPurchased);
                item.setColor(color);
                items.add(item);
            } while (cursor.moveToNext());
        }
        cursor.close();
        return items;
    }

    public void deleteList(int listId) {
        SQLiteDatabase db = this.getWritableDatabase();
        db.delete(TABLE_ITEMS, COLUMN_LIST_ID + " = ?", new String[]{String.valueOf(listId)});
        db.delete(TABLE_LISTS, COLUMN_ID + " = ?", new String[]{String.valueOf(listId)});
        db.close();
    }

    // Методы для работы с категориями
    
    public long addCategory(Category category) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        
        values.put(COLUMN_NAME, category.getName());
        values.put(COLUMN_COLOR, category.getColor());
        values.put(COLUMN_ICON, category.getIcon());
        
        long id = db.insert(TABLE_CATEGORIES, null, values);
        db.close();
        return id;
    }
    
    public List<Category> getAllCategories() {
        List<Category> categories = new ArrayList<>();
        
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.query(
                TABLE_CATEGORIES,
                new String[] {COLUMN_ID, COLUMN_NAME, COLUMN_COLOR, COLUMN_ICON},
                null, null, null, null, COLUMN_NAME + " ASC");
        
        if (cursor.moveToFirst()) {
            do {
                Category category = new Category();
                category.setId(cursor.getLong(0));
                category.setName(cursor.getString(1));
                category.setColor(cursor.getString(2));
                category.setIcon(cursor.getInt(3));
                
                categories.add(category);
            } while (cursor.moveToNext());
        }
        
        cursor.close();
        db.close();
        return categories;
    }
    
    public Category getCategory(long id) {
        SQLiteDatabase db = this.getReadableDatabase();
        
        Cursor cursor = db.query(
                TABLE_CATEGORIES,
                new String[] {COLUMN_ID, COLUMN_NAME, COLUMN_COLOR, COLUMN_ICON},
                COLUMN_ID + "=?",
                new String[] {String.valueOf(id)},
                null, null, null);
        
        Category category = null;
        if (cursor.moveToFirst()) {
            category = new Category();
            category.setId(cursor.getLong(0));
            category.setName(cursor.getString(1));
            category.setColor(cursor.getString(2));
            category.setIcon(cursor.getInt(3));
        }
        
        cursor.close();
        db.close();
        return category;
    }
    
    public int updateCategory(Category category) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        
        values.put(COLUMN_NAME, category.getName());
        values.put(COLUMN_COLOR, category.getColor());
        values.put(COLUMN_ICON, category.getIcon());
        
        int rowsAffected = db.update(
                TABLE_CATEGORIES,
                values,
                COLUMN_ID + "=?",
                new String[] {String.valueOf(category.getId())});
        
        db.close();
        return rowsAffected;
    }
    
    public int deleteCategory(long id) {
        SQLiteDatabase db = this.getWritableDatabase();
        int rowsAffected = db.delete(
                TABLE_CATEGORIES,
                COLUMN_ID + "=?",
                new String[] {String.valueOf(id)});
        
        db.close();
        return rowsAffected;
    }
    
    // Обновления методов для работы с элементами списка
    
    public long addItem(long listId, ShoppingItem item) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        
        values.put(COLUMN_LIST_ID, listId);
        values.put(COLUMN_NAME, item.getName());
        values.put(COLUMN_QUANTITY, item.getQuantity());
        values.put(COLUMN_PRICE, item.getPrice());
        values.put(COLUMN_CHECKED, item.isChecked() ? 1 : 0);
        values.put(COLUMN_COLOR, item.getColor());
        values.put(COLUMN_CATEGORY_ID, item.getCategoryId());
        
        long id = db.insert(TABLE_ITEMS, null, values);
        db.close();
        return id;
    }
    
    public List<ShoppingItem> getItemsForList(long listId) {
        List<ShoppingItem> items = new ArrayList<>();
        
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.query(
                TABLE_ITEMS,
                new String[] {COLUMN_ID, COLUMN_NAME, COLUMN_QUANTITY, COLUMN_PRICE, COLUMN_CHECKED, COLUMN_COLOR, COLUMN_CATEGORY_ID},
                COLUMN_LIST_ID + "=?",
                new String[] {String.valueOf(listId)},
                null, null, null);
        
        if (cursor.moveToFirst()) {
            do {
                ShoppingItem item = new ShoppingItem(
                        cursor.getLong(0),
                        cursor.getString(1),
                        cursor.getDouble(2),
                        cursor.getDouble(3),
                        cursor.getInt(4) == 1,
                        cursor.getString(5),
                        cursor.getLong(6)
                );
                
                items.add(item);
            } while (cursor.moveToNext());
        }
        
        cursor.close();
        db.close();
        return items;
    }
    
    public int updateItem(ShoppingItem item) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        
        values.put(COLUMN_NAME, item.getName());
        values.put(COLUMN_QUANTITY, item.getQuantity());
        values.put(COLUMN_PRICE, item.getPrice());
        values.put(COLUMN_CHECKED, item.isChecked() ? 1 : 0);
        values.put(COLUMN_COLOR, item.getColor());
        values.put(COLUMN_CATEGORY_ID, item.getCategoryId());
        
        int rowsAffected = db.update(
                TABLE_ITEMS,
                values,
                COLUMN_ID + "=?",
                new String[] {String.valueOf(item.getId())});
        
        db.close();
        return rowsAffected;
    }
    
    // Дополнительные методы для работы с категориями товаров
    
    public List<ShoppingItem> getItemsByCategory(long listId, long categoryId) {
        List<ShoppingItem> items = new ArrayList<>();
        
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.query(
                TABLE_ITEMS,
                new String[] {COLUMN_ID, COLUMN_NAME, COLUMN_QUANTITY, COLUMN_PRICE, COLUMN_CHECKED, COLUMN_COLOR, COLUMN_CATEGORY_ID},
                COLUMN_LIST_ID + "=? AND " + COLUMN_CATEGORY_ID + "=?",
                new String[] {String.valueOf(listId), String.valueOf(categoryId)},
                null, null, null);
        
        if (cursor.moveToFirst()) {
            do {
                ShoppingItem item = new ShoppingItem(
                        cursor.getLong(0),
                        cursor.getString(1),
                        cursor.getDouble(2),
                        cursor.getDouble(3),
                        cursor.getInt(4) == 1,
                        cursor.getString(5),
                        cursor.getLong(6)
                );
                
                items.add(item);
            } while (cursor.moveToNext());
        }
        
        cursor.close();
        db.close();
        return items;
    }
}