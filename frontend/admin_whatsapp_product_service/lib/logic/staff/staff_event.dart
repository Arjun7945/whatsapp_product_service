import 'package:equatable/equatable.dart';
import '../../data/models/delivery_person.dart';

abstract class StaffEvent extends Equatable {
  const StaffEvent();

  @override
  List<Object?> get props => [];
}

class LoadStaff extends StaffEvent {}

class CreateStaff extends StaffEvent {
  final DeliveryPerson staff;

  const CreateStaff(this.staff);

  @override
  List<Object?> get props => [staff];
}

class UpdateStaff extends StaffEvent {
  final int id;
  final DeliveryPerson staff;

  const UpdateStaff({required this.id, required this.staff});

  @override
  List<Object?> get props => [id, staff];
}

class DeleteStaff extends StaffEvent {
  final int id;

  const DeleteStaff(this.id);

  @override
  List<Object?> get props => [id];
}
