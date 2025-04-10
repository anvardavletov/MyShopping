import 'package:flutter/foundation.dart';
import 'package:shared_preferences/shared_preferences.dart';
import '../database/database_helper.dart';
import '../models/shopping_list.dart';
import '../models/shopping_item.dart';
import 'dart:convert';

class ShoppingListService with ChangeNotifier {
  final DatabaseHelper _dbHelper = DatabaseHelper.instance;
  
  // Текущий активный список покупок
  List<ShoppingItem> _currentList = [];
  String _currentListName = 'Новый список';
  String _currency = '₽';
  
  // Геттеры для доступа к данным
  List<ShoppingItem> get currentList => _currentList;
  String get currentListName => _currentListName;
  String get currency => _currency;
  double get total => _currentList.fold(0, (sum, item) => sum + item.total);
  
  // Конструктор
  ShoppingListService() {
    _loadSavedList();
  }
  
  // Добавление нового элемента в текущий список
  void addItem(ShoppingItem item) {
    _currentList.add(item);
    _saveCurrentList();
    notifyListeners();
  }
  
  // Обновление элемента в текущем списке
  void updateItem(ShoppingItem updatedItem) {
    final index = _currentList.indexWhere((item) => item.id == updatedItem.id);
    if (index != -1) {
      _currentList[index] = updatedItem;
      _saveCurrentList();
      notifyListeners();
    }
  }
  
  // Удаление элемента из текущего списка
  void removeItem(int index) {
    _currentList.removeAt(index);
    _saveCurrentList();
    notifyListeners();
  }
  
  // Установка статуса checked для элемента
  void toggleItemChecked(int index, bool checked) {
    if (index >= 0 && index < _currentList.length) {
      _currentList[index] = _currentList[index].copyWith(checked: checked);
      _saveCurrentList();
      notifyListeners();
    }
  }
  
  // Установка имени текущего списка
  void setCurrentListName(String name) {
    _currentListName = name;
    _saveCurrentList();
    notifyListeners();
  }
  
  // Установка валюты
  void setCurrency(String currency) {
    _currency = currency;
    _saveCurrentList();
    notifyListeners();
  }
  
  // Очистка текущего списка
  void clearCurrentList() {
    _currentList.clear();
    _currentListName = 'Новый список';
    _saveCurrentList();
    notifyListeners();
  }
  
  // Загрузка сохраненного списка из SharedPreferences
  Future<void> _loadSavedList() async {
    try {
      final prefs = await SharedPreferences.getInstance();
      final listJson = prefs.getString('current_list');
      if (listJson != null) {
        final listData = jsonDecode(listJson);
        _currentListName = listData['name'] ?? 'Новый список';
        _currency = listData['currency'] ?? '₽';
        
        final itemsData = listData['items'] as List?;
        if (itemsData != null) {
          _currentList = itemsData.map((itemData) => ShoppingItem(
            id: itemData['id'],
            name: itemData['name'] ?? '',
            quantity: (itemData['quantity'] ?? 0).toDouble(),
            price: (itemData['price'] ?? 0).toDouble(),
            category: itemData['category'] ?? '',
            color: itemData['color'] ?? '#FFFFFF',
            checked: itemData['checked'] == 1,
          )).toList();
        }
      }
    } catch (e) {
      debugPrint('Ошибка при загрузке списка: $e');
    }
    notifyListeners();
  }
  
  // Сохранение текущего списка в SharedPreferences
  Future<void> _saveCurrentList() async {
    try {
      final prefs = await SharedPreferences.getInstance();
      final listData = {
        'name': _currentListName,
        'currency': _currency,
        'items': _currentList.map((item) => item.toMap()).toList(),
      };
      await prefs.setString('current_list', jsonEncode(listData));
    } catch (e) {
      debugPrint('Ошибка при сохранении списка: $e');
    }
  }
  
  // Сохранение текущего списка в базу данных
  Future<int> saveCurrentListToDatabase() async {
    final shoppingList = ShoppingList(
      name: _currentListName,
      items: List.from(_currentList),
      currency: _currency,
    );
    final id = await _dbHelper.saveList(shoppingList);
    return id;
  }
  
  // Загрузка списка из базы данных
  Future<void> loadListFromDatabase(int id) async {
    final list = await _dbHelper.getList(id);
    if (list != null) {
      _currentListName = list.name;
      _currentList = List.from(list.items);
      _currency = list.currency;
      _saveCurrentList();
      notifyListeners();
    }
  }
  
  // Получение всех сохраненных списков
  Future<List<ShoppingList>> getAllLists() async {
    return await _dbHelper.getAllLists();
  }
  
  // Удаление списка из базы данных
  Future<void> deleteList(int id) async {
    await _dbHelper.deleteList(id);
    notifyListeners();
  }
} 