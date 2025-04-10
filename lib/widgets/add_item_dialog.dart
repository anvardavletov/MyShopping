import 'package:flutter/material.dart';
import 'package:flutter_colorpicker/flutter_colorpicker.dart';
import '../models/shopping_item.dart';

class AddItemDialog extends StatefulWidget {
  final Function(ShoppingItem) onAddItem;
  final ShoppingItem? initialItem;

  const AddItemDialog({
    Key? key,
    required this.onAddItem,
    this.initialItem,
  }) : super(key: key);

  @override
  State<AddItemDialog> createState() => _AddItemDialogState();
}

class _AddItemDialogState extends State<AddItemDialog> {
  final _formKey = GlobalKey<FormState>();
  final _nameController = TextEditingController();
  final _quantityController = TextEditingController();
  final _priceController = TextEditingController();
  final _categoryController = TextEditingController();
  late Color _pickerColor;
  late String _selectedColor;

  @override
  void initState() {
    super.initState();
    
    if (widget.initialItem != null) {
      _nameController.text = widget.initialItem!.name;
      _quantityController.text = widget.initialItem!.quantity.toString();
      _priceController.text = widget.initialItem!.price.toString();
      _categoryController.text = widget.initialItem!.category;
      _selectedColor = widget.initialItem!.color;
      try {
        _pickerColor = Color(int.parse(_selectedColor.substring(1), radix: 16) | 0xFF000000);
      } catch (e) {
        _pickerColor = Colors.green;
      }
    } else {
      _selectedColor = '#4CAF50'; // Зеленый цвет по умолчанию
      _pickerColor = Colors.green;
    }
  }

  @override
  void dispose() {
    _nameController.dispose();
    _quantityController.dispose();
    _priceController.dispose();
    _categoryController.dispose();
    super.dispose();
  }

  // Диалог выбора цвета
  void _showColorPicker() {
    showDialog(
      context: context,
      builder: (context) {
        return AlertDialog(
          title: const Text('Выберите цвет'),
          content: SingleChildScrollView(
            child: ColorPicker(
              pickerColor: _pickerColor,
              onColorChanged: (color) {
                setState(() {
                  _pickerColor = color;
                  _selectedColor = '#${color.value.toRadixString(16).substring(2).toUpperCase()}';
                });
              },
              labelTypes: const [ColorLabelType.hex],
              pickerAreaHeightPercent: 0.8,
            ),
          ),
          actions: [
            TextButton(
              onPressed: () {
                Navigator.of(context).pop();
              },
              child: const Text('Готово'),
            ),
          ],
        );
      },
    );
  }

  // Сохранение нового элемента
  void _saveItem() {
    if (_formKey.currentState!.validate()) {
      final name = _nameController.text;
      double quantity;
      double price;
      
      try {
        quantity = double.parse(_quantityController.text.replaceAll(',', '.'));
      } catch (e) {
        quantity = 1;
      }
      
      try {
        price = double.parse(_priceController.text.replaceAll(',', '.'));
      } catch (e) {
        price = 0;
      }
      
      final category = _categoryController.text;
      
      final item = ShoppingItem(
        id: widget.initialItem?.id,
        name: name,
        quantity: quantity,
        price: price,
        category: category,
        color: _selectedColor,
        checked: widget.initialItem?.checked ?? false,
      );
      
      widget.onAddItem(item);
      Navigator.of(context).pop();
    }
  }

  @override
  Widget build(BuildContext context) {
    return AlertDialog(
      title: Text(widget.initialItem != null ? 'Редактировать товар' : 'Добавить товар'),
      content: Form(
        key: _formKey,
        child: SingleChildScrollView(
          child: Column(
            mainAxisSize: MainAxisSize.min,
            children: [
              // Название товара
              TextFormField(
                controller: _nameController,
                decoration: const InputDecoration(
                  labelText: 'Название',
                  icon: Icon(Icons.shopping_bag),
                ),
                validator: (value) {
                  if (value == null || value.isEmpty) {
                    return 'Введите название товара';
                  }
                  return null;
                },
                autofocus: true,
              ),
              
              // Количество
              TextFormField(
                controller: _quantityController,
                decoration: const InputDecoration(
                  labelText: 'Количество',
                  icon: Icon(Icons.numbers),
                ),
                keyboardType: const TextInputType.numberWithOptions(decimal: true),
                validator: (value) {
                  if (value == null || value.isEmpty) {
                    return 'Введите количество';
                  }
                  if (double.tryParse(value.replaceAll(',', '.')) == null) {
                    return 'Введите корректное число';
                  }
                  return null;
                },
              ),
              
              // Цена
              TextFormField(
                controller: _priceController,
                decoration: const InputDecoration(
                  labelText: 'Цена',
                  icon: Icon(Icons.currency_ruble),
                ),
                keyboardType: const TextInputType.numberWithOptions(decimal: true),
                validator: (value) {
                  if (value == null || value.isEmpty) {
                    return 'Введите цену';
                  }
                  if (double.tryParse(value.replaceAll(',', '.')) == null) {
                    return 'Введите корректное число';
                  }
                  return null;
                },
              ),
              
              // Категория
              TextFormField(
                controller: _categoryController,
                decoration: const InputDecoration(
                  labelText: 'Категория',
                  icon: Icon(Icons.category),
                ),
              ),
              
              // Выбор цвета
              const SizedBox(height: 16),
              ListTile(
                leading: Icon(Icons.color_lens, color: _pickerColor),
                title: const Text('Цвет категории'),
                trailing: Container(
                  width: 24,
                  height: 24,
                  decoration: BoxDecoration(
                    color: _pickerColor,
                    shape: BoxShape.circle,
                  ),
                ),
                onTap: _showColorPicker,
              ),
            ],
          ),
        ),
      ),
      actions: [
        TextButton(
          onPressed: () => Navigator.of(context).pop(),
          child: const Text('Отмена'),
        ),
        ElevatedButton(
          onPressed: _saveItem,
          child: Text(widget.initialItem != null ? 'Сохранить' : 'Добавить'),
        ),
      ],
    );
  }
} 