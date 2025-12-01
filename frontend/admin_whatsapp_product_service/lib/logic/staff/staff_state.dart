import 'package:equatable/equatable.dart';
import '../../data/models/delivery_person.dart';

abstract class StaffState extends Equatable {
  const StaffState();

  @override
  List<Object?> get props => [];
}

class StaffInitial extends StaffState {}

class StaffLoading extends StaffState {}

class StaffLoaded extends StaffState {
  final List<DeliveryPerson> staff;

  const StaffLoaded(this.staff);

  @override
  List<Object?> get props => [staff];
}

class StaffOperationSuccess extends StaffState {
  final String message;
  final List<DeliveryPerson> staff;

  const StaffOperationSuccess({
    required this.message,
    required this.staff,
  });

  @override
  List<Object?> get props => [message, staff];
}

class StaffError extends StaffState {
  final String message;

  const StaffError(this.message);

  @override
  List<Object?> get props => [message];
}
