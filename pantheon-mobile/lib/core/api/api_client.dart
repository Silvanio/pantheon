import 'dart:async';

import 'package:dio/dio.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../auth/auth_provider.dart';
import '../offline/cache_store.dart';
import '../offline/outbox_controller.dart';
import '../storage/token_storage.dart';
import 'api_config.dart';
import 'api_exception.dart';

/// Thin wrapper around a single `dio` instance, mirroring `pantheon-web`'s pattern of one
/// `authFetch` helper reused by every feature's composable: every authenticated request goes
/// through here, gets the bearer token attached, and a 401 anywhere logs the session out.
///
/// Also the offline-support integration point (see design.md): every GET write-through-caches
/// its response and falls back to that cache when a live request fails for connectivity
/// reasons, and [mutateQueueable]/[uploadQueueable] enqueue into [OutboxController] instead of
/// surfacing an error when a mutation can't reach the server right now.
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

  CacheStore get _cache => _ref.read(cacheStoreProvider);

  static bool isConnectivityError(DioException e) =>
      e.type == DioExceptionType.connectionError ||
      e.type == DioExceptionType.connectionTimeout ||
      e.type == DioExceptionType.sendTimeout ||
      e.type == DioExceptionType.receiveTimeout ||
      (e.type == DioExceptionType.unknown && e.response == null);

  static String _cacheKey(String path, Map<String, dynamic>? query) =>
      query == null || query.isEmpty ? path : '$path?${Uri(queryParameters: query.map((k, v) => MapEntry(k, '$v'))).query}';

  /// Reads through the local cache: on success, stores the response for later offline use; on a
  /// connectivity failure, falls back to the last cached value for this exact path+query if one
  /// exists, so a screen the user already visited keeps showing its last-known data offline.
  ///
  /// Pass `offlineCapable: false` for data that must never be shown stale or persisted locally
  /// (Projetos, Permissões, Tasks — see design.md's offline-support scoping): it's neither
  /// written to nor read from the cache, so a connectivity failure surfaces as a normal error.
  Future<T> get<T>(String path, {Map<String, dynamic>? query, bool offlineCapable = true}) async {
    final key = _cacheKey(path, query);
    try {
      final response = await _dio.get(path, queryParameters: query);
      if (offlineCapable) unawaited(_cache.put(key, response.data));
      return response.data as T;
    } on DioException catch (e) {
      if (offlineCapable && isConnectivityError(e)) {
        final cached = await _cache.get(key);
        if (cached != null) return cached as T;
      }
      throw _toApiException(e);
    }
  }

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

  /// A mutation that's safe to defer: tries it live; if it fails purely for connectivity
  /// reasons, queues it in the outbox and returns `false` instead of throwing (the caller shows
  /// a "saved, will send once you're back online" message rather than an error). A real
  /// server/business error (4xx/5xx) still throws normally — only offline defers.
  Future<bool> mutateQueueable({
    required String method,
    required String path,
    Object? body,
    required String entityLabel,
  }) async {
    try {
      await _dio.request(path, data: body, options: Options(method: method));
      return true;
    } on DioException catch (e) {
      if (isConnectivityError(e)) {
        await _ref.read(outboxControllerProvider.notifier).enqueue(method: method, path: path, body: body, entityLabel: entityLabel);
        return false;
      }
      throw _toApiException(e);
    }
  }

  /// Same as [mutateQueueable] but for a single-file multipart upload (e.g. a daily-report
  /// photo) — queues the local file path and replays the upload later.
  Future<bool> uploadQueueable({
    required String path,
    required String fileFieldName,
    required String localFilePath,
    Map<String, String> fields = const {},
    required String entityLabel,
  }) async {
    try {
      final formData = FormData.fromMap({fileFieldName: await MultipartFile.fromFile(localFilePath), ...fields});
      await _dio.request(path, data: formData, options: Options(method: 'POST'));
      return true;
    } on DioException catch (e) {
      if (isConnectivityError(e)) {
        await _ref.read(outboxControllerProvider.notifier).enqueueUpload(
              path: path,
              fileFieldName: fileFieldName,
              localFilePath: localFilePath,
              fields: fields,
              entityLabel: entityLabel,
            );
        return false;
      }
      throw _toApiException(e);
    }
  }

  /// Raw replay used only by [OutboxController] to drain the queue — deliberately doesn't
  /// itself re-enqueue on failure (the caller decides what to do with a still-offline error).
  Future<void> replayRaw(String method, String path, {Object? body}) =>
      _dio.request(path, data: body, options: Options(method: method));

  Future<void> replayUpload(String path, String fileFieldName, String localFilePath, Map<String, String> fields) async {
    final formData = FormData.fromMap({fileFieldName: await MultipartFile.fromFile(localFilePath), ...fields});
    await _dio.request(path, data: formData, options: Options(method: 'POST'));
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
