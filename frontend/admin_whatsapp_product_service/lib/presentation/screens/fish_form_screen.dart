import 'package:flutter/material.dart';
import 'package:flutter_bloc/flutter_bloc.dart';
import '../../logic/fish/fish_bloc.dart';
import '../../logic/fish/fish_event.dart';
import '../../data/models/fish_product.dart';

class FishFormScreen extends StatefulWidget {
  final FishProduct? fish;

  const FishFormScreen({super.key, this.fish});

  @override
  State<FishFormScreen> createState() => _FishFormScreenState();
}

class _FishFormScreenState extends State<FishFormScreen> {
  final _formKey = GlobalKey<FormState>();
  final _nameController = TextEditingController();
  final _priceController = TextEditingController();
  bool _isAvailable = true;

  bool get isEditing => widget.fish != null;

  @override
  void initState() {
    super.initState();
    if (widget.fish != null) {
      _nameController.text = widget.fish!.name;
      _priceController.text = widget.fish!.pricePerKg.toString();
      _isAvailable = widget.fish!.isAvailable;
    }
  }

  @override
  void dispose() {
    _nameController.dispose();
    _priceController.dispose();
    super.dispose();
  }





  void _handleSubmit() {
    if (_formKey.currentState!.validate()) {
      print('DEBUG: Creating fish with isAvailable = $_isAvailable'); // Debug log
      
      final fish = FishProduct(
        id: widget.fish?.id,
        name: _nameController.text.trim(),
        pricePerKg: double.parse(_priceController.text),
        isAvailable: _isAvailable,
        imageURL: widget.fish?.imageURL,
      );

      print('DEBUG: Fish object isAvailable = ${fish.isAvailable}'); // Debug log
      print('DEBUG: Fish toJson = ${fish.toJson()}'); // Debug log

      if (isEditing) {
        context.read<FishBloc>().add(
              UpdateFish(
                id: widget.fish!.id!,
                fish: fish,
              ),
            );
      } else {
        context.read<FishBloc>().add(
              CreateFish(
                fish: fish,
              ),
            );
      }

      Navigator.pop(context);
    }
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(
        title: Text(isEditing ? 'Edit Fish Product' : 'Add Fish Product'),
      ),
      body: SingleChildScrollView(
        padding: const EdgeInsets.all(16),
        child: Form(
          key: _formKey,
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.stretch,
            children: [
              // Name Field
              TextFormField(
                controller: _nameController,
                decoration: const InputDecoration(
                  labelText: 'Fish Name',
                  hintText: 'e.g., Salmon',
                ),
                validator: (value) {
                  if (value == null || value.isEmpty) {
                    return 'Please enter fish name';
                  }
                  return null;
                },
              ),
              const SizedBox(height: 16),
              // Price Field
              TextFormField(
                controller: _priceController,
                decoration: const InputDecoration(
                  labelText: 'Price per KG',
                  hintText: 'e.g., 250.00',
                  prefixText: '₹ ',
                ),
                keyboardType: const TextInputType.numberWithOptions(decimal: true),
                validator: (value) {
                  if (value == null || value.isEmpty) {
                    return 'Please enter price';
                  }
                  if (double.tryParse(value) == null) {
                    return 'Please enter a valid number';
                  }
                  if (double.parse(value) <= 0) {
                    return 'Price must be greater than 0';
                  }
                  return null;
                },
              ),
              const SizedBox(height: 16),
              // Availability Switch
              SwitchListTile(
                title: const Text('Is Available'),
                subtitle: Text(_isAvailable ? 'In Stock' : 'Out of Stock'),
                value: _isAvailable,
                onChanged: (value) {
                  setState(() {
                    _isAvailable = value;
                  });
                },
              ),
              const SizedBox(height: 32),
              // Submit Button
              FilledButton(
                onPressed: _handleSubmit,
                child: Padding(
                  padding: const EdgeInsets.all(16.0),
                  child: Text(isEditing ? 'Update' : 'Create'),
                ),
              ),
            ],
          ),
        ),
      ),
    );
  }
}
