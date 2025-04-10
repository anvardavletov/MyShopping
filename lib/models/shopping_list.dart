import 'dart:convert';
import 'package:intl/intl.dart';
import 'shopping_item.dart';

class ShoppingList {
  final int? id;
  final String name;
  final String dateTime;
  final List<ShoppingItem> items;
  final String currency;

  ShoppingList({
    this.id,
    required this.name,
    String? dateTime,
    required this.items,
    this.currency = '₽',
  }) : dateTime = dateTime ?? DateFormat('dd.MM.yyyy HH:mm').format(DateTime.now());

  // Получение общей суммы списка
  double get total => items.fold(0, (sum, item) => sum + item.total);

  // Группировка элементов по категориям
  Map<String, List<ShoppingItem>> get itemsByCategory {
    final result = <String, List<ShoppingItem>>{};
    for (var item in items) {
      if (item.category.isNotEmpty) {
        if (!result.containsKey(item.category)) {
          result[item.category] = [];
        }
        result[item.category]!.add(item);
      }
    }
    return result;
  }

  // Создание копии списка с новыми значениями
  ShoppingList copyWith({
    int? id,
    String? name,
    String? dateTime,
    List<ShoppingItem>? items,
    String? currency,
  }) {
    return ShoppingList(
      id: id ?? this.id,
      name: name ?? this.name,
      dateTime: dateTime ?? this.dateTime,
      items: items ?? List.from(this.items),
      currency: currency ?? this.currency,
    );
  }

  // Преобразование объекта в Map для сохранения в базе данных
  Map<String, dynamic> toMap() {
    return {
      'id': id,
      'name': name,
      'dateTime': dateTime,
      'items': jsonEncode(items.map((item) => item.toMap()).toList()),
      'currency': currency,
    };
  }

  // Создание объекта из Map из базы данных
  factory ShoppingList.fromMap(Map<String, dynamic> map) {
    final itemsJson = jsonDecode(map['items']) as List;
    return ShoppingList(
      id: map['id'],
      name: map['name'],
      dateTime: map['dateTime'],
      items: itemsJson.map((item) => ShoppingItem.fromMap(item)).toList(),
      currency: map['currency'],
    );
  }

  // Преобразование в строку
  @override
  String toString() {
    return 'ShoppingList{id: $id, name: $name, dateTime: $dateTime, items: $items, currency: $currency}';
  }
} 