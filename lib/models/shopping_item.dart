class ShoppingItem {
  final int? id;
  final String name;
  final double quantity;
  final double price;
  final String category;
  final String color;
  final bool checked;

  ShoppingItem({
    this.id,
    required this.name,
    required this.quantity,
    required this.price,
    required this.category,
    required this.color,
    this.checked = false,
  });

  // Вычисляемое свойство для общей суммы
  double get total => quantity * price;

  // Создание копии объекта с новыми значениями
  ShoppingItem copyWith({
    int? id,
    String? name,
    double? quantity,
    double? price,
    String? category,
    String? color,
    bool? checked,
  }) {
    return ShoppingItem(
      id: id ?? this.id,
      name: name ?? this.name,
      quantity: quantity ?? this.quantity,
      price: price ?? this.price,
      category: category ?? this.category,
      color: color ?? this.color,
      checked: checked ?? this.checked,
    );
  }

  // Преобразование объекта в Map для сохранения в базе данных
  Map<String, dynamic> toMap() {
    return {
      'id': id,
      'name': name,
      'quantity': quantity,
      'price': price,
      'category': category,
      'color': color,
      'checked': checked ? 1 : 0,
    };
  }

  // Создание объекта из Map из базы данных
  factory ShoppingItem.fromMap(Map<String, dynamic> map) {
    return ShoppingItem(
      id: map['id'],
      name: map['name'],
      quantity: map['quantity'],
      price: map['price'],
      category: map['category'],
      color: map['color'],
      checked: map['checked'] == 1,
    );
  }

  // Преобразование в JSON строку
  @override
  String toString() {
    return 'ShoppingItem{id: $id, name: $name, quantity: $quantity, price: $price, category: $category, color: $color, checked: $checked}';
  }
} 