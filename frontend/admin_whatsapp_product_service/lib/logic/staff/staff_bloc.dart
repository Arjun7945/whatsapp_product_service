import 'package:flutter_bloc/flutter_bloc.dart';
import '../../data/repositories/staff_repository.dart';
import 'staff_event.dart';
import 'staff_state.dart';

class StaffBloc extends Bloc<StaffEvent, StaffState> {
  final StaffRepository staffRepository;

  StaffBloc(this.staffRepository) : super(StaffInitial()) {
    on<LoadStaff>(_onLoadStaff);
    on<CreateStaff>(_onCreateStaff);
    on<UpdateStaff>(_onUpdateStaff);
    on<DeleteStaff>(_onDeleteStaff);
  }

  Future<void> _onLoadStaff(
    LoadStaff event,
    Emitter<StaffState> emit,
  ) async {
    emit(StaffLoading());
    try {
      final staff = await staffRepository.getAllStaff();
      emit(StaffLoaded(staff));
    } catch (e) {
      emit(StaffError(e.toString()));
    }
  }

  Future<void> _onCreateStaff(
    CreateStaff event,
    Emitter<StaffState> emit,
  ) async {
    emit(StaffLoading());
    try {
      await staffRepository.createStaff(event.staff);
      final staff = await staffRepository.getAllStaff();
      emit(StaffOperationSuccess(
        message: 'Delivery staff created successfully',
        staff: staff,
      ));
    } catch (e) {
      emit(StaffError(e.toString()));
    }
  }

  Future<void> _onUpdateStaff(
    UpdateStaff event,
    Emitter<StaffState> emit,
  ) async {
    emit(StaffLoading());
    try {
      await staffRepository.updateStaff(event.id, event.staff);
      final staff = await staffRepository.getAllStaff();
      emit(StaffOperationSuccess(
        message: 'Delivery staff updated successfully',
        staff: staff,
      ));
    } catch (e) {
      emit(StaffError(e.toString()));
    }
  }

  Future<void> _onDeleteStaff(
    DeleteStaff event,
    Emitter<StaffState> emit,
  ) async {
    emit(StaffLoading());
    try {
      await staffRepository.deleteStaff(event.id);
      final staff = await staffRepository.getAllStaff();
      emit(StaffOperationSuccess(
        message: 'Delivery staff deleted successfully',
        staff: staff,
      ));
    } catch (e) {
      emit(StaffError(e.toString()));
    }
  }
}
