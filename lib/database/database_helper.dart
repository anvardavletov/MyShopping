import 'dart:async';
import 'package:path/path.dart';
import 'package:sqflite/sqflite.dart';
import '../models/shopping_list.dart';

class DatabaseHelper {
  static final DatabaseHelper _instance = DatabaseHelper._internal();
  static DatabaseHelper get instance => _instance;

  // Имена таблиц
  static const String tableShoppingLists = 'shopping_lists';

  // Имена столбцов общие
  static const String columnId = 'id';

  // Столбцы для таблицы списков
  static const String columnName = 'name';
  static const String columnDateTime = 'date_time';
  static const String columnItems = 'items';
  static const String columnCurrency = 'currency';

  // Создаем синглтон
  DatabaseHelper._internal();

  // База данных
  static Database? _database;
  Future<Database> get database async {
    if (_database != null) return _database!;
    _database = await _initDatabase();
    return _database!;
  }

  // Инициализация базы данных
  Future<Database> _initDatabase() async {
    final path = await getDatabasesPath();
    final dbPath = join(path, 'myshopping.db');

    return await openDatabase(dbPath, version: 1, onCreate: _onCreate);
  }

  // Создание таблиц
  Future<void> _onCreate(Database db, int version) async {
    await db.execute('''
      CREATE TABLE $tableShoppingLists (
        $columnId INTEGER PRIMARY KEY AUTOINCREMENT,
        $columnName TEXT NOT NULL,
        $columnDateTime TEXT NOT NULL,
        $columnItems TEXT,
        $columnCurrency TEXT DEFAULT 'RUB'
      )
    ''');
  }

  // Сохранение списка покупок
  Future<int> saveList(ShoppingList list) async {
    final db = await database;
    return await db.insert(
      tableShoppingLists,
      list.toMap(),
      conflictAlgorithm: ConflictAlgorithm.replace,
    );
  }

  // Получение всех списков
  Future<List<ShoppingList>> getAllLists() async {
    final db = await database;
    final List<Map<String, dynamic>> maps = await db.query(
      tableShoppingLists,
      orderBy: '$columnDateTime DESC',
    );

    return List.generate(maps.length, (i) {
      return ShoppingList.fromMap(maps[i]);
    });
  }

  // Получение списка по ID
  Future<ShoppingList?> getList(int id) async {
    final db = await database;
    final List<Map<String, dynamic>> maps = await db.query(
      tableShoppingLists,
      where: '$columnId = ?',
      whereArgs: [id],
    );

    if (maps.isNotEmpty) {
      return ShoppingList.fromMap(maps.first);
    }
    return null;
  }

  // Обновление списка
  Future<int> updateList(ShoppingList list) async {
    final db = await database;
    return await db.update(
      tableShoppingLists,
      list.toMap(),
      where: '$columnId = ?',
      whereArgs: [list.id],
    );
  }

  // Удаление списка
  Future<int> deleteList(int id) async {
    final db = await database;
    return await db.delete(
      tableShoppingLists,
      where: '$columnId = ?',
      whereArgs: [id],
    );
  }

  // Закрытие базы данных
  Future<void> close() async {
    final db = await database;
    db.close();
  }
}
