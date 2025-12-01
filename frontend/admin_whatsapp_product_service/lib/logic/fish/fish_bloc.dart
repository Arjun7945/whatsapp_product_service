import 'package:flutter_bloc/flutter_bloc.dart';
import '../../data/repositories/fish_repository.dart';
import 'fish_event.dart';
import 'fish_state.dart';

class FishBloc extends Bloc<FishEvent, FishState> {
  final FishRepository fishRepository;

  FishBloc(this.fishRepository) : super(FishInitial()) {
    on<LoadFish>(_onLoadFish);
    on<CreateFish>(_onCreateFish);
    on<UpdateFish>(_onUpdateFish);
    on<DeleteFish>(_onDeleteFish);
  }

  Future<void> _onLoadFish(
    LoadFish event,
    Emitter<FishState> emit,
  ) async {
    emit(FishLoading());
    try {
      final fishProducts = await fishRepository.getAllFish();
      emit(FishLoaded(fishProducts));
    } catch (e) {
      emit(FishError(e.toString()));
    }
  }

  Future<void> _onCreateFish(
    CreateFish event,
    Emitter<FishState> emit,
  ) async {
    emit(FishLoading());
    try {
      await fishRepository.createFish(event.fish);
      final fishProducts = await fishRepository.getAllFish();
      emit(FishOperationSuccess(
        message: 'Fish product created successfully',
        fishProducts: fishProducts,
      ));
    } catch (e) {
      emit(FishError(e.toString()));
    }
  }

  Future<void> _onUpdateFish(
    UpdateFish event,
    Emitter<FishState> emit,
  ) async {
    emit(FishLoading());
    try {
      await fishRepository.updateFish(event.id, event.fish);
      final fishProducts = await fishRepository.getAllFish();
      emit(FishOperationSuccess(
        message: 'Fish product updated successfully',
        fishProducts: fishProducts,
      ));
    } catch (e) {
      emit(FishError(e.toString()));
    }
  }

  Future<void> _onDeleteFish(
    DeleteFish event,
    Emitter<FishState> emit,
  ) async {
    emit(FishLoading());
    try {
      await fishRepository.deleteFish(event.id);
      final fishProducts = await fishRepository.getAllFish();
      emit(FishOperationSuccess(
        message: 'Fish product deleted successfully',
        fishProducts: fishProducts,
      ));
    } catch (e) {
      emit(FishError(e.toString()));
    }
  }
}
