import 'package:equatable/equatable.dart';

class DeliveryPerson extends Equatable {
  final int? id;
  final String name;
  final String? waPhoneNumber;
  final String phoneNumber;
  final bool isActive;

  const DeliveryPerson({
    this.id,
    required this.name,
    this.waPhoneNumber,
    required this.phoneNumber,
    required this.isActive,
  });

  factory DeliveryPerson.fromJson(Map<String, dynamic> json) {
    return DeliveryPerson(
      id: json['id'] as int?,
      name: json['name'] as String,
      waPhoneNumber: json['waPhoneNumber'] as String?,
      phoneNumber: json['phoneNumber'] as String,
      isActive: json['isActive'] as bool? ?? true,
    );
  }

  Map<String, dynamic> toJson() {
    return {
      if (id != null) 'id': id,
      'name': name,
      if (waPhoneNumber != null) 'waPhoneNumber': waPhoneNumber,
      'phoneNumber': phoneNumber,
      'isActive': isActive,
    };
  }

  DeliveryPerson copyWith({
    int? id,
    String? name,
    String? waPhoneNumber,
    String? phoneNumber,
    bool? isActive,
  }) {
    return DeliveryPerson(
      id: id ?? this.id,
      name: name ?? this.name,
      waPhoneNumber: waPhoneNumber ?? this.waPhoneNumber,
      phoneNumber: phoneNumber ?? this.phoneNumber,
      isActive: isActive ?? this.isActive,
    );
  }

  @override
  List<Object?> get props => [id, name, waPhoneNumber, phoneNumber, isActive];
}
