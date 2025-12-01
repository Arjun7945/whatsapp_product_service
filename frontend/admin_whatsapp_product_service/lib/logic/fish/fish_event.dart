import 'package:equatable/equatable.dart';
import '../../data/models/fish_product.dart';

abstract class FishEvent extends Equatable {
  const FishEvent();

  @override
  List<Object?> get props => [];
}

class LoadFish extends FishEvent {}

class CreateFish extends FishEvent {
  final FishProduct fish;

  const CreateFish({required this.fish});

  @override
  List<Object?> get props => [fish];
}

class UpdateFish extends FishEvent {
  final int id;
  final FishProduct fish;

  const UpdateFish({
    required this.id,
    required this.fish,
  });

  @override
  List<Object?> get props => [id, fish];
}

class DeleteFish extends FishEvent {
  final int id;

  const DeleteFish(this.id);

  @override
  List<Object?> get props => [id];
}
