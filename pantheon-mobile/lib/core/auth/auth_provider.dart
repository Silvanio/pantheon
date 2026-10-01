import 'dart:convert';

import 'package:dio/dio.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../api/api_client.dart';
import '../api/api_config.dart';
import '../storage/token_storage.dart';

final tokenStorageProvider = Provider<TokenStorage>((ref) => TokenStorage());

/// Bumped on every transition to/from an authenticated session (login, restoring a session on
/// cold start, logout, forced logout). Screen-data providers `ref.watch` this so they're forced
/// to recompute at that moment, regardless of whatever navigation/widget-tree timing happens to
/// keep a watcher alive across the transition — `.autoDispose` alone only guarantees freshness
/// when a provider's watcher count actually reaches zero, which isn't guaranteed at the exact
/// instant auth state changes. See fix-mobile-stale-data-and-outbox-coverage's design.md.
final sessionEpochProvider = StateProvider<int>((ref) => 0);

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

  // The secure-storage read below is a local, near-instant operation — without this floor the
  // splash screen (see features/auth/splash_screen.dart) would get redirected away mid-animation
  // on most launches, so its entrance (icon pop, wordmark, tagline) is never actually seen.
  // `Future.delayed` starts counting the moment it's created, so creating it before the read and
  // awaiting it after waits only for whichever of the two takes longer, never both in sequence.
  static const _minSplashDuration = Duration(milliseconds: 4800);

  Future<void> _restore() async {
    final minSplash = Future<void>.delayed(_minSplashDuration);
    try {
      final token = await _tokenStorage.read();
      await minSplash;
      if (token == null) {
        state = state.copyWith(status: AuthStatus.unauthenticated);
      } else {
        state = AuthState(status: AuthStatus.authenticated, email: _decodeEmail(token));
        _bumpSessionEpoch();
      }
    } catch (_) {
      // Secure storage being unreadable (first launch on some platforms, a widget test host
      // with no platform channel, ...) should fall back to logged-out, not hang forever.
      await minSplash;
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
    _bumpSessionEpoch();
  }

  Future<void> logout() async {
    try {
      await _ref.read(deviceTokenUnregisterProvider)();
    } catch (_) {
      // Best-effort — logging out locally must succeed even if the unregister call fails.
    }
    await _tokenStorage.clear();
    state = state.copyWith(status: AuthStatus.unauthenticated, email: null);
    _bumpSessionEpoch();
  }

  /// Called by [ApiClient] when any request comes back 401 — the stored token is no longer
  /// valid (expired/revoked), so end the session without an extra round trip.
  Future<void> forceLogout() async {
    await _tokenStorage.clear();
    state = AuthState(status: AuthStatus.unauthenticated);
    _bumpSessionEpoch();
  }

  void _bumpSessionEpoch() {
    _ref.read(sessionEpochProvider.notifier).state++;
  }
}

final authControllerProvider = StateNotifierProvider<AuthController, AuthState>((ref) {
  return AuthController(ref, ref.watch(tokenStorageProvider));
});

/// Overridden by the notifications feature once it's wired up; a no-op until then so
/// `AuthController` doesn't need a direct dependency on the notifications feature module.
final deviceTokenUnregisterProvider = Provider<Future<void> Function()>((ref) => () async {});
