import 'package:flutter/material.dart';
import '../models/shopping_item.dart';

class ShoppingItemWidget extends StatelessWidget {
  final ShoppingItem item;
  final Function(bool) onToggle;
  final VoidCallback onEdit;
  final VoidCallback onDelete;

  const ShoppingItemWidget({
    Key? key,
    required this.item,
    required this.onToggle,
    required this.onEdit,
    required this.onDelete,
  }) : super(key: key);

  @override
  Widget build(BuildContext context) {
    // Преобразование строки цвета в объект Color
    Color itemColor;
    try {
      itemColor = Color(int.parse(item.color.substring(1), radix: 16) | 0xFF000000);
    } catch (e) {
      itemColor = Colors.grey;
    }

    return Dismissible(
      key: ValueKey(item.id ?? UniqueKey()),
      background: Container(
        color: Colors.red,
        alignment: Alignment.centerRight,
        padding: const EdgeInsets.only(right: 20),
        child: const Icon(
          Icons.delete,
          color: Colors.white,
        ),
      ),
      direction: DismissDirection.endToStart,
      onDismissed: (direction) {
        onDelete();
      },
      child: Card(
        margin: const EdgeInsets.symmetric(horizontal: 8, vertical: 4),
        child: ListTile(
          leading: Container(
            width: 24,
            height: 24,
            decoration: BoxDecoration(
              color: itemColor,
              shape: BoxShape.circle,
            ),
          ),
          title: Text(
            item.name,
            style: TextStyle(
              decoration: item.checked ? TextDecoration.lineThrough : null,
            ),
          ),
          subtitle: Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              if (item.category.isNotEmpty)
                Text(
                  item.category,
                  style: TextStyle(
                    color: itemColor.withOpacity(0.8),
                    fontSize: 12,
                  ),
                ),
              Text(
                '${item.quantity} × ${item.price.toStringAsFixed(2)} = ${item.total.toStringAsFixed(2)}',
              ),
            ],
          ),
          trailing: Row(
            mainAxisSize: MainAxisSize.min,
            children: [
              Checkbox(
                value: item.checked,
                onChanged: (value) => onToggle(value ?? false),
              ),
              IconButton(
                icon: const Icon(Icons.edit),
                onPressed: onEdit,
              ),
            ],
          ),
        ),
      ),
    );
  }
} 