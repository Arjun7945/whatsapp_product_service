import 'package:dio/dio.dart';
import '../models/customer_order.dart';

abstract class OrderRepository {
  Future<List<CustomerOrder>> getAllOrders();
  Future<CustomerOrder> getOrderById(int id);
}

class OrderRepositoryImpl implements OrderRepository {
  final Dio dio;

  OrderRepositoryImpl(this.dio);

  @override
  Future<List<CustomerOrder>> getAllOrders() async {
    try {
      final response = await dio.get('/orders');
      final List<dynamic> data = response.data as List<dynamic>;
      return data.map((json) => CustomerOrder.fromJson(json)).toList();
    } on DioException catch (e) {
      throw Exception('Failed to fetch orders: ${e.message}');
    }
  }

  @override
  Future<CustomerOrder> getOrderById(int id) async {
    try {
      final response = await dio.get('/orders/$id');
      return CustomerOrder.fromJson(response.data);
    } on DioException catch (e) {
      throw Exception('Failed to fetch order details: ${e.message}');
    }
  }
}
