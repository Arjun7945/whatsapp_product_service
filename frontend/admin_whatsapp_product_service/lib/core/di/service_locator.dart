import 'package:get_it/get_it.dart';
import 'package:dio/dio.dart';
import '../constants/app_constants.dart';
import '../../data/repositories/auth_repository.dart';
import '../../data/repositories/fish_repository.dart';
import '../../data/repositories/staff_repository.dart';
import '../../data/repositories/order_repository.dart';
import '../../logic/auth/auth_bloc.dart';
import '../../logic/fish/fish_bloc.dart';
import '../../logic/staff/staff_bloc.dart';
import '../../logic/order/order_bloc.dart';

final getIt = GetIt.instance;

Future<void> setupServiceLocator() async {
  // Core
  getIt.registerLazySingleton<Dio>(() => Dio(BaseOptions(baseUrl: AppConstants.apiBaseUrl)));

  // Repositories
  getIt.registerLazySingleton<AuthRepository>(() => AuthRepositoryImpl(getIt()));
  getIt.registerLazySingleton<FishRepository>(() => FishRepositoryImpl(getIt()));
  getIt.registerLazySingleton<StaffRepository>(() => StaffRepositoryImpl(getIt()));
  getIt.registerLazySingleton<OrderRepository>(() => OrderRepositoryImpl(getIt()));

  // BLoCs
  getIt.registerFactory(() => AuthBloc(getIt()));
  getIt.registerFactory(() => FishBloc(getIt()));
  getIt.registerFactory(() => StaffBloc(getIt()));
  getIt.registerFactory(() => OrderBloc(getIt()));
}
