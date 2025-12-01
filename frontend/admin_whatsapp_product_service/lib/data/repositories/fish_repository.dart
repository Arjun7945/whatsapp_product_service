import 'package:dio/dio.dart';
import '../models/fish_product.dart';

abstract class FishRepository {
  Future<List<FishProduct>> getAllFish();
  Future<FishProduct> createFish(FishProduct fish);
  Future<FishProduct> updateFish(int id, FishProduct fish);
  Future<void> deleteFish(int id);
}

class FishRepositoryImpl implements FishRepository {
  final Dio dio;

  FishRepositoryImpl(this.dio);

  @override
  Future<List<FishProduct>> getAllFish() async {
    try {
      final response = await dio.get('/fish');
      final List<dynamic> data = response.data as List<dynamic>;
      return data.map((json) => FishProduct.fromJson(json)).toList();
    } on DioException catch (e) {
      throw Exception('Failed to fetch fish products: ${e.message}');
    }
  }

  @override
  Future<FishProduct> createFish(FishProduct fish) async {
    try {
      final jsonData = fish.toJson();
      print('DEBUG Repository: Sending JSON to backend: $jsonData'); // Debug log
      final response = await dio.post('/fish', data: jsonData);
      print('DEBUG Repository: Response from backend: ${response.data}'); // Debug log
      return FishProduct.fromJson(response.data);
    } on DioException catch (e) {
      print('DEBUG Repository: Error creating fish: ${e.message}'); // Debug log
      throw Exception('Failed to create fish product: ${e.message}');
    }
  }

  @override
  Future<FishProduct> updateFish(int id, FishProduct fish) async {
    try {
      final jsonData = fish.toJson();
      print('DEBUG Repository: Updating fish $id with JSON: $jsonData'); // Debug log
      final response = await dio.put('/fish/$id', data: jsonData);
      print('DEBUG Repository: Update response: ${response.data}'); // Debug log
      return FishProduct.fromJson(response.data);
    } on DioException catch (e) {
      print('DEBUG Repository: Error updating fish: ${e.message}'); // Debug log
      throw Exception('Failed to update fish product: ${e.message}');
    }
  }

  @override
  Future<void> deleteFish(int id) async {
    try {
      await dio.delete('/fish/$id');
    } on DioException catch (e) {
      throw Exception('Failed to delete fish product: ${e.message}');
    }
  }
}
