import 'package:dio/dio.dart';
import '../models/auth_model.dart';

abstract class AuthRepository {
  Future<LoginResponse> login(LoginRequest request);
}

class AuthRepositoryImpl implements AuthRepository {
  final Dio dio;

  AuthRepositoryImpl(this.dio);

  @override
  Future<LoginResponse> login(LoginRequest request) async {
    try {
      final response = await dio.post(
        '/auth/login',
        data: request.toJson(),
      );
      return LoginResponse.fromJson(response.data);
    } on DioException catch (e) {
      if (e.response != null) {
        throw Exception('Login failed: ${e.response?.data['message'] ?? 'Unknown error'}');
      } else {
        throw Exception('Network error: ${e.message}');
      }
    }
  }
}
