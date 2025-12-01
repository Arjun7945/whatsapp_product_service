import 'package:flutter/material.dart';
import 'package:flutter_bloc/flutter_bloc.dart';
import '../../core/di/service_locator.dart';
import '../../logic/staff/staff_bloc.dart';
import '../../logic/staff/staff_event.dart';
import '../../logic/staff/staff_state.dart';
import '../../data/models/delivery_person.dart';

class DeliveryStaffScreen extends StatelessWidget {
  const DeliveryStaffScreen({super.key});

  @override
  Widget build(BuildContext context) {
    return BlocProvider(
      create: (_) => getIt<StaffBloc>()..add(LoadStaff()),
      child: const _DeliveryStaffView(),
    );
  }
}

class _DeliveryStaffView extends StatelessWidget {
  const _DeliveryStaffView();

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(
        title: const Text('Delivery Staff'),
      ),
      body: BlocConsumer<StaffBloc, StaffState>(
        listener: (context, state) {
          if (state is StaffOperationSuccess) {
            ScaffoldMessenger.of(context).showSnackBar(
              SnackBar(
                content: Text(state.message),
                backgroundColor: Colors.green,
              ),
            );
          } else if (state is StaffError) {
            ScaffoldMessenger.of(context).showSnackBar(
              SnackBar(
                content: Text(state.message),
                backgroundColor: Colors.red,
              ),
            );
          }
        },
        builder: (context, state) {
          if (state is StaffLoading) {
            return const Center(child: CircularProgressIndicator());
          } else if (state is StaffLoaded || state is StaffOperationSuccess) {
            final staff = state is StaffLoaded
                ? state.staff
                : (state as StaffOperationSuccess).staff;

            if (staff.isEmpty) {
              return const Center(
                child: Text('No delivery staff found'),
              );
            }

            return ListView.builder(
              padding: const EdgeInsets.all(16),
              itemCount: staff.length,
              itemBuilder: (context, index) {
                final person = staff[index];
                return _StaffCard(staff: person);
              },
            );
          } else if (state is StaffError) {
            return Center(
              child: Column(
                mainAxisAlignment: MainAxisAlignment.center,
                children: [
                  const Icon(Icons.error, size: 64, color: Colors.red),
                  const SizedBox(height: 16),
                  Text(state.message),
                  const SizedBox(height: 16),
                  FilledButton(
                    onPressed: () {
                      context.read<StaffBloc>().add(LoadStaff());
                    },
                    child: const Text('Retry'),
                  ),
                ],
              ),
            );
          }

          return const SizedBox.shrink();
        },
      ),
      floatingActionButton: FloatingActionButton.extended(
        onPressed: () {
          _showStaffDialog(context);
        },
        icon: const Icon(Icons.add),
        label: const Text('Add Staff'),
      ),
    );
  }

  void _showStaffDialog(BuildContext context, [DeliveryPerson? staff]) {
    showDialog(
      context: context,
      builder: (dialogContext) => BlocProvider.value(
        value: context.read<StaffBloc>(),
        child: _StaffFormDialog(staff: staff),
      ),
    );
  }
}

class _StaffCard extends StatelessWidget {
  final DeliveryPerson staff;

  const _StaffCard({required this.staff});

  @override
  Widget build(BuildContext context) {
    return Card(
      margin: const EdgeInsets.only(bottom: 16),
      child: ListTile(
        leading: CircleAvatar(
          backgroundColor: staff.isActive ? Colors.green : Colors.grey,
          child: const Icon(Icons.person, color: Colors.white),
        ),
        title: Text(
          staff.name,
          style: const TextStyle(fontWeight: FontWeight.bold),
        ),
        subtitle: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            const SizedBox(height: 4),
            Text(staff.phoneNumber),
            const SizedBox(height: 4),
            Chip(
              label: Text(
                staff.isActive ? 'Active' : 'Inactive',
                style: const TextStyle(fontSize: 12),
              ),
              backgroundColor: staff.isActive
                  ? Colors.green.withOpacity(0.2)
                  : Colors.grey.withOpacity(0.2),
              side: BorderSide.none,
              padding: EdgeInsets.zero,
              visualDensity: VisualDensity.compact,
            ),
          ],
        ),
        trailing: Row(
          mainAxisSize: MainAxisSize.min,
          children: [
            IconButton(
              icon: const Icon(Icons.edit),
              onPressed: () {
                _showStaffDialog(context, staff);
              },
            ),
            IconButton(
              icon: const Icon(Icons.delete),
              color: Colors.red,
              onPressed: () {
                _showDeleteDialog(context, staff);
              },
            ),
          ],
        ),
      ),
    );
  }

  void _showStaffDialog(BuildContext context, [DeliveryPerson? staff]) {
    showDialog(
      context: context,
      builder: (dialogContext) => BlocProvider.value(
        value: context.read<StaffBloc>(),
        child: _StaffFormDialog(staff: staff),
      ),
    );
  }

  void _showDeleteDialog(BuildContext context, DeliveryPerson staff) {
    showDialog(
      context: context,
      builder: (dialogContext) => AlertDialog(
        title: const Text('Delete Staff'),
        content: Text('Are you sure you want to delete ${staff.name}?'),
        actions: [
          TextButton(
            onPressed: () => Navigator.pop(dialogContext),
            child: const Text('Cancel'),
          ),
          FilledButton(
            onPressed: () {
              context.read<StaffBloc>().add(DeleteStaff(staff.id!));
              Navigator.pop(dialogContext);
            },
            style: FilledButton.styleFrom(
              backgroundColor: Colors.red,
            ),
            child: const Text('Delete'),
          ),
        ],
      ),
    );
  }
}

class _StaffFormDialog extends StatefulWidget {
  final DeliveryPerson? staff;

  const _StaffFormDialog({this.staff});

  @override
  State<_StaffFormDialog> createState() => _StaffFormDialogState();
}

class _StaffFormDialogState extends State<_StaffFormDialog> {
  final _formKey = GlobalKey<FormState>();
  final _nameController = TextEditingController();
  final _phoneController = TextEditingController();
  bool _isActive = true;

  bool get isEditing => widget.staff != null;

  @override
  void initState() {
    super.initState();
    if (widget.staff != null) {
      _nameController.text = widget.staff!.name;
      _phoneController.text = widget.staff!.phoneNumber;
      _isActive = widget.staff!.isActive;
    }
  }

  @override
  void dispose() {
    _nameController.dispose();
    _phoneController.dispose();
    super.dispose();
  }

  void _handleSubmit() {
    if (_formKey.currentState!.validate()) {
      final staff = DeliveryPerson(
        id: widget.staff?.id,
        name: _nameController.text.trim(),
        phoneNumber: _phoneController.text.trim(),
        isActive: _isActive,
      );

      if (isEditing) {
        context.read<StaffBloc>().add(
              UpdateStaff(id: widget.staff!.id!, staff: staff),
            );
      } else {
        context.read<StaffBloc>().add(CreateStaff(staff));
      }

      Navigator.pop(context);
    }
  }

  @override
  Widget build(BuildContext context) {
    return AlertDialog(
      title: Text(isEditing ? 'Edit Staff' : 'Add Staff'),
      content: Form(
        key: _formKey,
        child: Column(
          mainAxisSize: MainAxisSize.min,
          children: [
            TextFormField(
              controller: _nameController,
              decoration: const InputDecoration(
                labelText: 'Name',
                prefixIcon: Icon(Icons.person),
              ),
              validator: (value) {
                if (value == null || value.isEmpty) {
                  return 'Please enter name';
                }
                return null;
              },
            ),
            const SizedBox(height: 16),
            TextFormField(
              controller: _phoneController,
              decoration: const InputDecoration(
                labelText: 'Phone Number',
                prefixIcon: Icon(Icons.phone),
              ),
              keyboardType: TextInputType.phone,
              validator: (value) {
                if (value == null || value.isEmpty) {
                  return 'Please enter phone number';
                }
                if (value.length < 10) {
                  return 'Phone number must be at least 10 digits';
                }
                return null;
              },
            ),
            const SizedBox(height: 16),
            SwitchListTile(
              title: const Text('Active'),
              value: _isActive,
              onChanged: (value) {
                setState(() {
                  _isActive = value;
                });
              },
            ),
          ],
        ),
      ),
      actions: [
        TextButton(
          onPressed: () => Navigator.pop(context),
          child: const Text('Cancel'),
        ),
        FilledButton(
          onPressed: _handleSubmit,
          child: Text(isEditing ? 'Update' : 'Create'),
        ),
      ],
    );
  }
}
