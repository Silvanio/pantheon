import 'package:dio/dio.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../auth/auth_provider.dart';
import '../storage/token_storage.dart';
import 'api_config.dart';
import 'api_exception.dart';

/// Thin wrapper around a single `dio` instance, mirroring `pantheon-web`'s pattern of one
/// `authFetch` helper reused by every feature's composable: every authenticated request goes
/// through here, gets the bearer token attached, and a 401 anywhere logs the session out.
class ApiClient {
  ApiClient(this._ref, this._tokenStorage) {
    _dio = Dio(BaseOptions(baseUrl: ApiConfig.baseUrl, connectTimeout: const Duration(seconds: 15)));
    _dio.interceptors.add(
      InterceptorsWrapper(
        onRequest: (options, handler) async {
          final token = await _tokenStorage.read();
          if (token != null) options.headers['Authorization'] = 'Bearer $token';
          handler.next(options);
        },
        onError: (error, handler) {
          if (error.response?.statusCode == 401) {
            // Deferred read (not during this provider's own construction) — safe with Riverpod.
            _ref.read(authControllerProvider.notifier).forceLogout();
          }
          handler.next(error);
        },
      ),
    );
  }

  final Ref _ref;
  final TokenStorage _tokenStorage;
  late final Dio _dio;

  Future<T> get<T>(String path, {Map<String, dynamic>? query}) => _unwrap(_dio.get(path, queryParameters: query));

  Future<T> post<T>(String path, {Object? body}) => _unwrap(_dio.post(path, data: body));

  Future<T> put<T>(String path, {Object? body}) => _unwrap(_dio.put(path, data: body));

  Future<T> patch<T>(String path, {Object? body}) => _unwrap(_dio.patch(path, data: body));

  Future<T> delete<T>(String path) => _unwrap(_dio.delete(path));

  Future<T> uploadMultipart<T>(String path, FormData formData, {String method = 'POST'}) =>
      _unwrap(_dio.request(path, data: formData, options: Options(method: method)));

  /// For binary responses (PDFs, photos) — returns raw bytes instead of decoded JSON.
  Future<List<int>> getBytes(String path) async {
    try {
      final response = await _dio.get<List<int>>(path, options: Options(responseType: ResponseType.bytes));
      return response.data ?? const [];
    } on DioException catch (e) {
      throw _toApiException(e);
    }
  }

  Future<T> _unwrap<T>(Future<Response<dynamic>> request) async {
    try {
      final response = await request;
      return response.data as T;
    } on DioException catch (e) {
      throw _toApiException(e);
    }
  }

  ApiException _toApiException(DioException e) {
    final status = e.response?.statusCode ?? -1;
    return ApiException(status, 'Request to ${e.requestOptions.path} failed with status $status');
  }
}

final apiClientProvider = Provider<ApiClient>((ref) {
  return ApiClient(ref, ref.watch(tokenStorageProvider));
});
