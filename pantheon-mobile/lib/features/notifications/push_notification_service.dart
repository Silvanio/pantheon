import 'dart:io' show Platform;

import 'package:firebase_core/firebase_core.dart';
import 'package:firebase_messaging/firebase_messaging.dart';
import 'package:flutter/foundation.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../core/router/app_router.dart';
import 'device_token_repository.dart';

/// Push notifications for purchase-request approval events (see push-notifications capability).
///
/// Firebase is only usable once a real project's config files
/// (`android/app/google-services.json`, `ios/Runner/GoogleService-Info.plist`) are dropped in —
/// see `pantheon-mobile/README.md`. Until then, every method here no-ops after logging, so the
/// rest of the app works normally without a Firebase project configured.
class PushNotificationService {
  PushNotificationService(this._ref);

  final Ref _ref;
  bool _available = false;
  String? _currentToken;

  Future<void> initialize() async {
    try {
      if (Firebase.apps.isEmpty) await Firebase.initializeApp();
      _available = true;
    } catch (e) {
      debugPrint('[push] Firebase not configured — push notifications disabled ($e).');
      return;
    }

    try {
      await FirebaseMessaging.instance.requestPermission();
      final token = await FirebaseMessaging.instance.getToken();
      if (token != null) await _registerToken(token);
      FirebaseMessaging.instance.onTokenRefresh.listen(_registerToken);
      FirebaseMessaging.onMessageOpenedApp.listen(_handleTap);
      final initialMessage = await FirebaseMessaging.instance.getInitialMessage();
      if (initialMessage != null) _handleTap(initialMessage);
    } catch (e) {
      debugPrint('[push] Failed to set up push notifications: $e');
    }
  }

  Future<void> _registerToken(String token) async {
    _currentToken = token;
    final platform = Platform.isIOS ? 'IOS' : 'ANDROID';
    try {
      await _ref.read(deviceTokenRepositoryProvider).register(token, platform);
    } catch (e) {
      debugPrint('[push] Failed to register device token: $e');
    }
  }

  /// Re-fetches the current FCM token and registers it — call after a successful login, since
  /// the initial [initialize] call may have run before the user was authenticated.
  Future<void> refreshRegistration() async {
    if (!_available) return;
    try {
      final token = await FirebaseMessaging.instance.getToken();
      if (token != null) await _registerToken(token);
    } catch (e) {
      debugPrint('[push] Failed to refresh device token registration: $e');
    }
  }

  Future<void> unregisterCurrent() async {
    if (!_available || _currentToken == null) return;
    try {
      await _ref.read(deviceTokenRepositoryProvider).unregister(_currentToken!);
    } catch (_) {
      // Best-effort — logging out locally must succeed regardless.
    }
    _currentToken = null;
  }

  void _handleTap(RemoteMessage message) {
    final purchaseRequestId = message.data['purchaseRequestId'] as String?;
    if (purchaseRequestId != null) {
      _ref.read(appRouterProvider).push('/purchase-requests/$purchaseRequestId');
    }
  }
}

final pushNotificationServiceProvider = Provider((ref) => PushNotificationService(ref));
