import 'package:flutter/material.dart';
import 'package:provider/provider.dart';
import '../services/shopping_list_service.dart';
import '../widgets/shopping_item_widget.dart';
import '../widgets/add_item_dialog.dart';
import 'my_lists_screen.dart';

class HomeScreen extends StatefulWidget {
  const HomeScreen({Key? key}) : super(key: key);

  @override
  State<HomeScreen> createState() => _HomeScreenState();
}

class _HomeScreenState extends State<HomeScreen> {
  final _formKey = GlobalKey<FormState>();
  final _nameController = TextEditingController();
  final _quantityController = TextEditingController();
  final _priceController = TextEditingController();
  final _categoryController = TextEditingController();
  String _selectedColor = '#4CAF50'; // Зеленый цвет по умолчанию

  @override
  void dispose() {
    _nameController.dispose();
    _quantityController.dispose();
    _priceController.dispose();
    _categoryController.dispose();
    super.dispose();
  }

  // Диалог для изменения названия списка
  void _showRenameListDialog(BuildContext context, ShoppingListService service) {
    final nameController = TextEditingController(text: service.currentListName);

    showDialog(
      context: context,
      builder: (context) => AlertDialog(
        title: const Text('Переименовать список'),
        content: TextField(
          controller: nameController,
          decoration: const InputDecoration(
            labelText: 'Название списка',
          ),
          autofocus: true,
        ),
        actions: [
          TextButton(
            onPressed: () => Navigator.of(context).pop(),
            child: const Text('Отмена'),
          ),
          TextButton(
            onPressed: () {
              if (nameController.text.isNotEmpty) {
                service.setCurrentListName(nameController.text);
                Navigator.of(context).pop();
              }
            },
            child: const Text('Сохранить'),
          ),
        ],
      ),
    );
  }

  // Сохранение списка в базу данных
  Future<void> _saveList(BuildContext context, ShoppingListService service) async {
    if (service.currentList.isEmpty) {
      ScaffoldMessenger.of(context).showSnackBar(
        const SnackBar(content: Text('Список пуст')),
      );
      return;
    }

    try {
      final id = await service.saveCurrentListToDatabase();
      if (!mounted) return;
      
      ScaffoldMessenger.of(context).showSnackBar(
        SnackBar(content: Text('Список "${service.currentListName}" сохранен')),
      );
    } catch (e) {
      if (!mounted) return;
      
      ScaffoldMessenger.of(context).showSnackBar(
        SnackBar(content: Text('Ошибка при сохранении: $e')),
      );
    }
  }

  // Очистка текущего списка
  void _clearList(BuildContext context, ShoppingListService service) {
    if (service.currentList.isEmpty) return;

    showDialog(
      context: context,
      builder: (context) => AlertDialog(
        title: const Text('Очистить список?'),
        content: const Text('Это действие нельзя отменить'),
        actions: [
          TextButton(
            onPressed: () => Navigator.of(context).pop(),
            child: const Text('Отмена'),
          ),
          TextButton(
            onPressed: () {
              service.clearCurrentList();
              Navigator.of(context).pop();
            },
            child: const Text('Очистить'),
          ),
        ],
      ),
    );
  }

  // Показать сохраненные списки
  void _showSavedLists(BuildContext context) {
    Navigator.of(context).push(
      MaterialPageRoute(
        builder: (context) => const MyListsScreen(),
      ),
    );
  }

  // Диалог для добавления нового элемента
  void _showAddItemDialog(BuildContext context, ShoppingListService service) {
    showDialog(
      context: context,
      builder: (context) => AddItemDialog(
        onAddItem: (item) {
          service.addItem(item);
        },
      ),
    );
  }

  @override
  Widget build(BuildContext context) {
    return Consumer<ShoppingListService>(
      builder: (context, shoppingService, child) {
        final currentList = shoppingService.currentList;
        final total = shoppingService.total;
        final currency = shoppingService.currency;

        return Scaffold(
          appBar: AppBar(
            title: GestureDetector(
              onTap: () => _showRenameListDialog(context, shoppingService),
              child: Text(shoppingService.currentListName),
            ),
            actions: [
              // Загрузить сохраненные списки
              IconButton(
                icon: const Icon(Icons.folder_open),
                onPressed: () => _showSavedLists(context),
                tooltip: 'Мои списки',
              ),
              // Сохранить текущий список
              IconButton(
                icon: const Icon(Icons.save),
                onPressed: () => _saveList(context, shoppingService),
                tooltip: 'Сохранить список',
              ),
              // Очистить список
              IconButton(
                icon: const Icon(Icons.delete_outline),
                onPressed: () => _clearList(context, shoppingService),
                tooltip: 'Очистить список',
              ),
              // Дополнительное меню
              PopupMenuButton(
                itemBuilder: (context) => [
                  const PopupMenuItem(
                    value: 'theme',
                    child: Text('Сменить тему'),
                  ),
                  const PopupMenuItem(
                    value: 'currency',
                    child: Text('Валюта'),
                  ),
                  const PopupMenuItem(
                    value: 'share',
                    child: Text('Поделиться'),
                  ),
                  const PopupMenuItem(
                    value: 'export',
                    child: Text('Экспорт'),
                  ),
                  const PopupMenuItem(
                    value: 'about',
                    child: Text('О приложении'),
                  ),
                ],
                onSelected: (value) {
                  // Обработка выбора пункта меню
                  switch (value) {
                    case 'theme':
                      // TODO: Реализовать смену темы
                      break;
                    case 'currency':
                      // TODO: Реализовать смену валюты
                      break;
                    case 'share':
                      // TODO: Реализовать функцию "Поделиться"
                      break;
                    case 'export':
                      // TODO: Реализовать экспорт списка
                      break;
                    case 'about':
                      // TODO: Показать информацию о приложении
                      break;
                  }
                },
              ),
            ],
          ),
          body: currentList.isEmpty
              ? Center(
                  child: Text(
                    'Список пуст',
                    style: Theme.of(context).textTheme.titleLarge,
                  ),
                )
              : Column(
                  children: [
                    // Список элементов
                    Expanded(
                      child: ListView.builder(
                        itemCount: currentList.length,
                        itemBuilder: (context, index) {
                          final item = currentList[index];
                          return ShoppingItemWidget(
                            item: item,
                            onToggle: (checked) {
                              shoppingService.toggleItemChecked(index, checked);
                            },
                            onEdit: () {
                              // TODO: Реализовать редактирование элемента
                            },
                            onDelete: () {
                              shoppingService.removeItem(index);
                            },
                          );
                        },
                      ),
                    ),
                    // Итоговая сумма
                    Container(
                      padding: const EdgeInsets.all(16),
                      color: Theme.of(context).colorScheme.primaryContainer,
                      child: Row(
                        mainAxisAlignment: MainAxisAlignment.spaceBetween,
                        children: [
                          Text(
                            'Итого:',
                            style: Theme.of(context).textTheme.titleMedium,
                          ),
                          Text(
                            '${total.toStringAsFixed(2)} $currency',
                            style: Theme.of(context).textTheme.titleLarge!.copyWith(
                                  fontWeight: FontWeight.bold,
                                ),
                          ),
                        ],
                      ),
                    ),
                  ],
                ),
          floatingActionButton: FloatingActionButton(
            onPressed: () => _showAddItemDialog(context, shoppingService),
            child: const Icon(Icons.add),
          ),
        );
      },
    );
  }
} 