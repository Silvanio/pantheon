import 'package:flutter_secure_storage/flutter_secure_storage.dart';

/// Persists the JWT in platform-secure storage (Keychain on iOS, EncryptedSharedPreferences
/// on Android) — the mobile equivalent of the web app's `localStorage` token, but not
/// readable by other apps or plain-text on disk.
class TokenStorage {
  TokenStorage() : _storage = const FlutterSecureStorage();

  final FlutterSecureStorage _storage;
  static const _tokenKey = 'pantheon_token';

  Future<String?> read() => _storage.read(key: _tokenKey);

  Future<void> write(String token) => _storage.write(key: _tokenKey, value: token);

  Future<void> clear() => _storage.delete(key: _tokenKey);
}
