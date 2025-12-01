import 'package:dio/dio.dart';
import '../models/delivery_person.dart';

abstract class StaffRepository {
  Future<List<DeliveryPerson>> getAllStaff();
  Future<DeliveryPerson> createStaff(DeliveryPerson staff);
  Future<DeliveryPerson> updateStaff(int id, DeliveryPerson staff);
  Future<void> deleteStaff(int id);
}

class StaffRepositoryImpl implements StaffRepository {
  final Dio dio;

  StaffRepositoryImpl(this.dio);

  @override
  Future<List<DeliveryPerson>> getAllStaff() async {
    try {
      final response = await dio.get('/delivery');
      final List<dynamic> data = response.data as List<dynamic>;
      return data.map((json) => DeliveryPerson.fromJson(json)).toList();
    } on DioException catch (e) {
      throw Exception('Failed to fetch delivery staff: ${e.message}');
    }
  }

  @override
  Future<DeliveryPerson> createStaff(DeliveryPerson staff) async {
    try {
      final response = await dio.post('/delivery', data: staff.toJson());
      return DeliveryPerson.fromJson(response.data);
    } on DioException catch (e) {
      throw Exception('Failed to create delivery staff: ${e.message}');
    }
  }

  @override
  Future<DeliveryPerson> updateStaff(int id, DeliveryPerson staff) async {
    try {
      final response = await dio.put('/delivery/$id', data: staff.toJson());
      return DeliveryPerson.fromJson(response.data);
    } on DioException catch (e) {
      throw Exception('Failed to update delivery staff: ${e.message}');
    }
  }

  @override
  Future<void> deleteStaff(int id) async {
    try {
      await dio.delete('/delivery/$id');
    } on DioException catch (e) {
      throw Exception('Failed to delete delivery staff: ${e.message}');
    }
  }
}
