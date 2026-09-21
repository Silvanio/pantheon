import 'dart:convert';

import 'package:dio/dio.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../api/api_client.dart';
import '../api/api_config.dart';
import '../storage/token_storage.dart';

final tokenStorageProvider = Provider<TokenStorage>((ref) => TokenStorage());

enum AuthStatus { unknown, authenticated, unauthenticated }

class AuthState {
  const AuthState({required this.status, this.email});

  final AuthStatus status;
  final String? email;

  static const initial = AuthState(status: AuthStatus.unknown);

  AuthState copyWith({AuthStatus? status, String? email}) =>
      AuthState(status: status ?? this.status, email: email ?? this.email);
}

/// Decodes the JWT payload's `email` claim for display purposes only — never trust this for
/// authorization (mirrors `pantheon-web`'s `useAuth.ts` `decodeEmail`).
String? _decodeEmail(String jwt) {
  try {
    final parts = jwt.split('.');
    if (parts.length != 3) return null;
    final normalized = base64.normalize(parts[1]);
    final payload = json.decode(utf8.decode(base64.decode(normalized))) as Map<String, dynamic>;
    return payload['email'] as String?;
  } catch (_) {
    return null;
  }
}

class AuthController extends StateNotifier<AuthState> {
  AuthController(this._ref, this._tokenStorage) : super(AuthState.initial) {
    _restore();
  }

  final Ref _ref;
  final TokenStorage _tokenStorage;

  Future<void> _restore() async {
    try {
      final token = await _tokenStorage.read();
      if (token == null) {
        state = state.copyWith(status: AuthStatus.unauthenticated);
      } else {
        state = AuthState(status: AuthStatus.authenticated, email: _decodeEmail(token));
      }
    } catch (_) {
      // Secure storage being unreadable (first launch on some platforms, a widget test host
      // with no platform channel, ...) should fall back to logged-out, not hang forever.
      state = state.copyWith(status: AuthStatus.unauthenticated);
    }
  }

  Future<void> login(String email, String password) async {
    final dio = Dio(BaseOptions(baseUrl: ApiConfig.baseUrl));
    final response = await dio.post<Map<String, dynamic>>(
      '/api/auth/login',
      data: {'email': email, 'password': password},
    );
    final token = response.data!['token'] as String;
    await _tokenStorage.write(token);
    state = AuthState(status: AuthStatus.authenticated, email: _decodeEmail(token));
  }

  Future<void> register(String email, String password, String displayName) async {
    final dio = Dio(BaseOptions(baseUrl: ApiConfig.baseUrl));
    final response = await dio.post<Map<String, dynamic>>(
      '/api/auth/register',
      data: {'email': email, 'password': password, 'displayName': displayName},
    );
    final token = response.data!['token'] as String;
    await _tokenStorage.write(token);
    state = AuthState(status: AuthStatus.authenticated, email: _decodeEmail(token));
  }

  Future<void> logout() async {
    try {
      await _ref.read(deviceTokenUnregisterProvider)();
    } catch (_) {
      // Best-effort — logging out locally must succeed even if the unregister call fails.
    }
    await _tokenStorage.clear();
    state = state.copyWith(status: AuthStatus.unauthenticated, email: null);
  }

  /// Called by [ApiClient] when any request comes back 401 — the stored token is no longer
  /// valid (expired/revoked), so end the session without an extra round trip.
  Future<void> forceLogout() async {
    await _tokenStorage.clear();
    state = AuthState(status: AuthStatus.unauthenticated);
  }
}

final authControllerProvider = StateNotifierProvider<AuthController, AuthState>((ref) {
  return AuthController(ref, ref.watch(tokenStorageProvider));
});

/// Overridden by the notifications feature once it's wired up; a no-op until then so
/// `AuthController` doesn't need a direct dependency on the notifications feature module.
final deviceTokenUnregisterProvider = Provider<Future<void> Function()>((ref) => () async {});
