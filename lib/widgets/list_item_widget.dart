import 'package:flutter/material.dart';
import '../models/shopping_list.dart';

class ListItemWidget extends StatelessWidget {
  final ShoppingList list;
  final VoidCallback onTap;
  final VoidCallback onEdit;
  final VoidCallback onDelete;

  const ListItemWidget({
    Key? key,
    required this.list,
    required this.onTap,
    required this.onEdit,
    required this.onDelete,
  }) : super(key: key);

  @override
  Widget build(BuildContext context) {
    // Получаем категории и их цвета
    final categories = <String, Color>{};
    for (var item in list.items) {
      if (item.category.isNotEmpty) {
        try {
          final color = Color(int.parse(item.color.substring(1), radix: 16) | 0xFF000000);
          categories[item.category] = color;
        } catch (e) {
          categories[item.category] = Colors.grey;
        }
      }
    }

    return Dismissible(
      key: ValueKey(list.id),
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
      confirmDismiss: (direction) async {
        bool? result;
        await showDialog(
          context: context,
          builder: (context) => AlertDialog(
            title: const Text('Подтверждение'),
            content: Text('Вы уверены, что хотите удалить список "${list.name}"?'),
            actions: [
              TextButton(
                onPressed: () {
                  Navigator.of(context).pop(false);
                },
                child: const Text('Отмена'),
              ),
              TextButton(
                onPressed: () {
                  Navigator.of(context).pop(true);
                },
                child: const Text('Удалить'),
              ),
            ],
          ),
        ).then((value) => result = value ?? false);
        return result;
      },
      child: Card(
        margin: const EdgeInsets.symmetric(horizontal: 8, vertical: 4),
        child: InkWell(
          onTap: onTap,
          child: Padding(
            padding: const EdgeInsets.all(16.0),
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Row(
                  mainAxisAlignment: MainAxisAlignment.spaceBetween,
                  children: [
                    Flexible(
                      child: Text(
                        list.name,
                        style: Theme.of(context).textTheme.titleLarge,
                        overflow: TextOverflow.ellipsis,
                      ),
                    ),
                    Text(
                      '${list.total.toStringAsFixed(2)} ${list.currency}',
                      style: Theme.of(context).textTheme.titleMedium,
                    ),
                  ],
                ),
                const SizedBox(height: 4),
                Text(
                  list.dateTime,
                  style: Theme.of(context).textTheme.bodySmall,
                ),
                if (categories.isNotEmpty) ...[
                  const SizedBox(height: 8),
                  Wrap(
                    spacing: 4,
                    runSpacing: 4,
                    children: categories.entries.map((entry) {
                      return Chip(
                        label: Text(
                          entry.key,
                          style: const TextStyle(fontSize: 10),
                        ),
                        backgroundColor: entry.value.withOpacity(0.3),
                        visualDensity: VisualDensity.compact,
                        materialTapTargetSize: MaterialTapTargetSize.shrinkWrap,
                      );
                    }).toList(),
                  ),
                ],
                Row(
                  mainAxisAlignment: MainAxisAlignment.end,
                  children: [
                    IconButton(
                      icon: const Icon(Icons.edit),
                      onPressed: onEdit,
                      tooltip: 'Редактировать',
                    ),
                    IconButton(
                      icon: const Icon(Icons.delete),
                      onPressed: onDelete,
                      tooltip: 'Удалить',
                    ),
                  ],
                ),
              ],
            ),
          ),
        ),
      ),
    );
  }
} 