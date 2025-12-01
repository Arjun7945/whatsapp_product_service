import 'package:equatable/equatable.dart';
import '../../data/models/fish_product.dart';

abstract class FishState extends Equatable {
  const FishState();

  @override
  List<Object?> get props => [];
}

class FishInitial extends FishState {}

class FishLoading extends FishState {}

class FishLoaded extends FishState {
  final List<FishProduct> fishProducts;

  const FishLoaded(this.fishProducts);

  @override
  List<Object?> get props => [fishProducts];
}

class FishOperationSuccess extends FishState {
  final String message;
  final List<FishProduct> fishProducts;

  const FishOperationSuccess({
    required this.message,
    required this.fishProducts,
  });

  @override
  List<Object?> get props => [message, fishProducts];
}

class FishError extends FishState {
  final String message;

  const FishError(this.message);

  @override
  List<Object?> get props => [message];
}
