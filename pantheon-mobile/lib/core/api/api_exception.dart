/// Thrown by [ApiClient] for any non-2xx response — mirrors `pantheon-web`'s `HttpError`.
class ApiException implements Exception {
  ApiException(this.statusCode, this.message);

  final int statusCode;
  final String message;

  @override
  String toString() => 'ApiException($statusCode): $message';
}
