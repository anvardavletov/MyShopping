import 'package:flutter/material.dart';
import 'package:provider/provider.dart';
import '../models/shopping_list.dart';
import '../services/shopping_list_service.dart';
import '../widgets/list_item_widget.dart';

class MyListsScreen extends StatefulWidget {
  const MyListsScreen({Key? key}) : super(key: key);

  @override
  State<MyListsScreen> createState() => _MyListsScreenState();
}

class _MyListsScreenState extends State<MyListsScreen> {
  late Future<List<ShoppingList>> _listsFuture;

  @override
  void initState() {
    super.initState();
    _loadLists();
  }

  void _loadLists() {
    final shoppingService = Provider.of<ShoppingListService>(context, listen: false);
    setState(() {
      _listsFuture = shoppingService.getAllLists();
    });
  }

  // Загрузка списка
  void _loadList(ShoppingList list) {
    final shoppingService = Provider.of<ShoppingListService>(context, listen: false);
    shoppingService.loadListFromDatabase(list.id!);
    Navigator.of(context).pop();
  }

  // Удаление списка
  void _deleteList(ShoppingList list) {
    showDialog(
      context: context,
      builder: (context) => AlertDialog(
        title: const Text('Удалить список?'),
        content: Text('Вы уверены, что хотите удалить список "${list.name}"?'),
        actions: [
          TextButton(
            onPressed: () => Navigator.of(context).pop(),
            child: const Text('Отмена'),
          ),
          TextButton(
            onPressed: () async {
              final shoppingService = Provider.of<ShoppingListService>(context, listen: false);
              await shoppingService.deleteList(list.id!);
              if (!mounted) return;
              Navigator.of(context).pop();
              // Обновляем список
              _loadLists();
            },
            child: const Text('Удалить'),
          ),
        ],
      ),
    );
  }

  // Показать детали списка
  void _showListDetails(ShoppingList list) {
    showDialog(
      context: context,
      builder: (context) => AlertDialog(
        title: Text(list.name),
        content: SizedBox(
          width: double.maxFinite,
          child: ListView(
            shrinkWrap: true,
            children: [
              Text('Дата: ${list.dateTime}'),
              const Divider(),
              ...list.items.map(
                (item) => ListTile(
                  title: Text(item.name),
                  subtitle: Text(
                    '${item.quantity} × ${item.price.toStringAsFixed(2)} = ${item.total.toStringAsFixed(2)} ${list.currency}',
                  ),
                  leading: Container(
                    width: 16,
                    height: 16,
                    decoration: BoxDecoration(
                      color: _getColorFromHex(item.color),
                      shape: BoxShape.circle,
                    ),
                  ),
                ),
              ),
              const Divider(),
              Padding(
                padding: const EdgeInsets.all(8.0),
                child: Text(
                  'Итого: ${list.total.toStringAsFixed(2)} ${list.currency}',
                  style: const TextStyle(fontWeight: FontWeight.bold),
                ),
              ),
            ],
          ),
        ),
        actions: [
          TextButton(
            onPressed: () => Navigator.of(context).pop(),
            child: const Text('Закрыть'),
          ),
          TextButton(
            onPressed: () {
              Navigator.of(context).pop();
              _loadList(list);
            },
            child: const Text('Загрузить'),
          ),
        ],
      ),
    );
  }

  // Вспомогательный метод для преобразования hex-цвета в Color
  Color _getColorFromHex(String hexColor) {
    try {
      hexColor = hexColor.replaceAll('#', '');
      if (hexColor.length == 6) {
        hexColor = 'FF$hexColor';
      }
      return Color(int.parse(hexColor, radix: 16));
    } catch (e) {
      return Colors.grey;
    }
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(
        title: const Text('Мои списки'),
      ),
      body: FutureBuilder<List<ShoppingList>>(
        future: _listsFuture,
        builder: (context, snapshot) {
          if (snapshot.connectionState == ConnectionState.waiting) {
            return const Center(child: CircularProgressIndicator());
          }
          
          if (snapshot.hasError) {
            return Center(
              child: Text('Ошибка: ${snapshot.error}'),
            );
          }
          
          final lists = snapshot.data ?? [];
          
          if (lists.isEmpty) {
            return const Center(
              child: Text('Нет сохраненных списков'),
            );
          }
          
          return ListView.builder(
            itemCount: lists.length,
            itemBuilder: (context, index) {
              final list = lists[index];
              return ListItemWidget(
                list: list,
                onTap: () => _showListDetails(list),
                onEdit: () => _loadList(list),
                onDelete: () => _deleteList(list),
              );
            },
          );
        },
      ),
    );
  }
} 