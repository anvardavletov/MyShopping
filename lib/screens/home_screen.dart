import 'package:flutter/material.dart';
import 'package:provider/provider.dart';
import 'package:showcaseview/showcaseview.dart';
import '../services/shopping_list_service.dart';
import '../widgets/shopping_item_widget.dart';
import '../widgets/add_item_dialog.dart';
import 'my_lists_screen.dart';

class HomeScreen extends StatelessWidget {
  const HomeScreen({Key? key}) : super(key: key);

  @override
  Widget build(BuildContext context) {
    return ShowcaseWidget(
      builder: Builder(
        builder: (context) => _HomeScreenContent(),
      ),
    );
  }
}

class _HomeScreenContent extends StatefulWidget {
  const _HomeScreenContent({Key? key}) : super(key: key);

  @override
  State<_HomeScreenContent> createState() => _HomeScreenContentState();
}

class _HomeScreenContentState extends State<_HomeScreenContent> {
  // Showcase keys
  final GlobalKey _titleKey = GlobalKey();
  final GlobalKey _myListsKey = GlobalKey();
  final GlobalKey _saveKey = GlobalKey();
  final GlobalKey _clearKey = GlobalKey();
  final GlobalKey _fabKey = GlobalKey();
  final GlobalKey _listKey = GlobalKey();

  // Showcase keys for AddItemDialog fields
  final GlobalKey _nameFieldKey = GlobalKey();
  final GlobalKey _quantityFieldKey = GlobalKey();
  final GlobalKey _priceFieldKey = GlobalKey();
  final GlobalKey _categoryFieldKey = GlobalKey();

  bool _showcaseStarted = false;
  bool _addDialogShowcaseStarted = false;

  @override
  void initState() {
    super.initState();
    WidgetsBinding.instance.addPostFrameCallback((_) {
      if (!_showcaseStarted) {
        setState(() {
          _showcaseStarted = true;
        });
        ShowCaseWidget.of(context).startShowCase([
          _titleKey,
          _myListsKey,
          _saveKey,
          _clearKey,
          _listKey,
          _fabKey,
        ]).then((_) {
          // После завершения showcase основных элементов — показать showcase для AddItemDialog
          _showAddItemDialog(context, Provider.of<ShoppingListService>(context, listen: false), showcase: true);
        });
      }
    });
  }
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
  void _showAddItemDialog(BuildContext context, ShoppingListService service, {bool showcase = false}) {
    showDialog(
      context: context,
      builder: (context) => AddItemDialog(
        onAddItem: (item) {
          service.addItem(item);
        },
        nameKey: _nameFieldKey,
        quantityKey: _quantityFieldKey,
        priceKey: _priceFieldKey,
        categoryKey: _categoryFieldKey,
        startShowcase: showcase,
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
            title: Showcase(
              key: _titleKey,
              description: 'Название списка. Нажмите, чтобы переименовать.',
              child: GestureDetector(
                onTap: () => _showRenameListDialog(context, shoppingService),
                child: Text(shoppingService.currentListName),
              ),
            ),
            actions: [
              // Загрузить сохраненные списки
              Showcase(
                key: _myListsKey,
                description: 'Здесь можно загрузить сохранённые списки.',
                child: IconButton(
                  icon: const Icon(Icons.folder_open),
                  onPressed: () => _showSavedLists(context),
                  tooltip: 'Мои списки',
                ),
              ),
              // Сохранить текущий список
              Showcase(
                key: _saveKey,
                description: 'Сохранить текущий список покупок.',
                child: IconButton(
                  icon: const Icon(Icons.save),
                  onPressed: () => _saveList(context, shoppingService),
                  tooltip: 'Сохранить список',
                ),
              ),
              // Очистить список
              Showcase(
                key: _clearKey,
                description: 'Очистить текущий список.',
                child: IconButton(
                  icon: const Icon(Icons.delete_outline),
                  onPressed: () => _clearList(context, shoppingService),
                  tooltip: 'Очистить список',
                ),
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
                      child: Showcase(
                        key: _listKey,
                        description: 'Список ваших покупок. Здесь отображаются все добавленные позиции.',
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
          floatingActionButton: Showcase(
            key: _fabKey,
            description: 'Добавить новый товар в список.',
            child: FloatingActionButton(
              onPressed: () => _showAddItemDialog(context, shoppingService),
              child: const Icon(Icons.add),
            ),
          ),
        );
      },
    );
  }
}