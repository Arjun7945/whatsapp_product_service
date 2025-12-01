import 'package:flutter/material.dart';
import 'package:flutter_bloc/flutter_bloc.dart';
import 'package:cached_network_image/cached_network_image.dart';
import '../../core/di/service_locator.dart';
import '../../logic/fish/fish_bloc.dart';
import '../../logic/fish/fish_event.dart';
import '../../logic/fish/fish_state.dart';
import '../../data/models/fish_product.dart';
import 'fish_form_screen.dart';

class FishManagementScreen extends StatelessWidget {
  const FishManagementScreen({super.key});

  @override
  Widget build(BuildContext context) {
    return BlocProvider(
      create: (_) => getIt<FishBloc>()..add(LoadFish()),
      child: const _FishManagementView(),
    );
  }
}

class _FishManagementView extends StatelessWidget {
  const _FishManagementView();

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(
        title: const Text('Fish Products'),
      ),
      body: BlocConsumer<FishBloc, FishState>(
        listener: (context, state) {
          if (state is FishOperationSuccess) {
            ScaffoldMessenger.of(context).showSnackBar(
              SnackBar(
                content: Text(state.message),
                backgroundColor: Colors.green,
              ),
            );
          } else if (state is FishError) {
            ScaffoldMessenger.of(context).showSnackBar(
              SnackBar(
                content: Text(state.message),
                backgroundColor: Colors.red,
              ),
            );
          }
        },
        builder: (context, state) {
          if (state is FishLoading) {
            return const Center(child: CircularProgressIndicator());
          } else if (state is FishLoaded || state is FishOperationSuccess) {
            final fishProducts = state is FishLoaded
                ? state.fishProducts
                : (state as FishOperationSuccess).fishProducts;

            if (fishProducts.isEmpty) {
              return const Center(
                child: Text('No fish products found'),
              );
            }

            return ListView.builder(
              padding: const EdgeInsets.all(16),
              itemCount: fishProducts.length,
              itemBuilder: (context, index) {
                final fish = fishProducts[index];
                return _FishProductCard(fish: fish);
              },
            );
          } else if (state is FishError) {
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
                      context.read<FishBloc>().add(LoadFish());
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
          Navigator.push(
            context,
            MaterialPageRoute(
              builder: (_) => BlocProvider.value(
                value: context.read<FishBloc>(),
                child: const FishFormScreen(),
              ),
            ),
          );
        },
        icon: const Icon(Icons.add),
        label: const Text('Add Fish'),
      ),
    );
  }
}

class _FishProductCard extends StatelessWidget {
  final FishProduct fish;

  const _FishProductCard({required this.fish});

  @override
  Widget build(BuildContext context) {
    return Card(
      margin: const EdgeInsets.only(bottom: 16),
      child: Padding(
        padding: const EdgeInsets.all(12),
        child: Row(
          children: [
            // Image
            ClipRRect(
              borderRadius: BorderRadius.circular(8),
              child: fish.imageURL != null
                  ? CachedNetworkImage(
                      imageUrl: fish.imageURL!,
                      width: 80,
                      height: 80,
                      fit: BoxFit.cover,
                      placeholder: (context, url) => const SizedBox(
                        width: 80,
                        height: 80,
                        child: Center(child: CircularProgressIndicator()),
                      ),
                      errorWidget: (context, url, error) => Container(
                        width: 80,
                        height: 80,
                        color: Colors.grey[300],
                        child: const Icon(Icons.image_not_supported),
                      ),
                    )
                  : Container(
                      width: 80,
                      height: 80,
                      color: Colors.grey[300],
                      child: const Icon(Icons.image),
                    ),
            ),
            const SizedBox(width: 16),
            // Details
            Expanded(
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  Text(
                    fish.name,
                    style: Theme.of(context).textTheme.titleMedium?.copyWith(
                          fontWeight: FontWeight.bold,
                        ),
                  ),
                  const SizedBox(height: 4),
                  Text(
                    '₹${fish.pricePerKg.toStringAsFixed(2)}/kg',
                    style: Theme.of(context).textTheme.bodyLarge?.copyWith(
                          color: Theme.of(context).colorScheme.primary,
                        ),
                  ),
                  const SizedBox(height: 4),
                  Chip(
                    label: Text(
                      fish.isAvailable ? 'Available' : 'Unavailable',
                      style: const TextStyle(fontSize: 12),
                    ),
                    backgroundColor: fish.isAvailable
                        ? Colors.green.withOpacity(0.2)
                        : Colors.red.withOpacity(0.2),
                    side: BorderSide.none,
                    padding: EdgeInsets.zero,
                    visualDensity: VisualDensity.compact,
                  ),
                ],
              ),
            ),
            // Actions
            Column(
              children: [
                IconButton(
                  icon: const Icon(Icons.edit),
                  onPressed: () {
                    Navigator.push(
                      context,
                      MaterialPageRoute(
                        builder: (_) => BlocProvider.value(
                          value: context.read<FishBloc>(),
                          child: FishFormScreen(fish: fish),
                        ),
                      ),
                    );
                  },
                ),
                IconButton(
                  icon: const Icon(Icons.delete),
                  color: Colors.red,
                  onPressed: () {
                    _showDeleteDialog(context, fish);
                  },
                ),
              ],
            ),
          ],
        ),
      ),
    );
  }

  void _showDeleteDialog(BuildContext context, FishProduct fish) {
    showDialog(
      context: context,
      builder: (dialogContext) => AlertDialog(
        title: const Text('Delete Fish Product'),
        content: Text('Are you sure you want to delete ${fish.name}?'),
        actions: [
          TextButton(
            onPressed: () => Navigator.pop(dialogContext),
            child: const Text('Cancel'),
          ),
          FilledButton(
            onPressed: () {
              context.read<FishBloc>().add(DeleteFish(fish.id!));
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
