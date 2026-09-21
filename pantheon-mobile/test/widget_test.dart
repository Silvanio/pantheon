import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:flutter_test/flutter_test.dart';

import 'package:pantheon_mobile/core/auth/auth_provider.dart';
import 'package:pantheon_mobile/core/storage/token_storage.dart';
import 'package:pantheon_mobile/main.dart';

/// Avoids the real `flutter_secure_storage` platform channel, which has no native
/// implementation available under `flutter test` (a plain unit/widget test runs on the Dart
/// VM with no plugin bindings) and would otherwise leave the read `Future` unresolved.
class _FakeTokenStorage implements TokenStorage {
  @override
  Future<String?> read() async => null;

  @override
  Future<void> write(String token) async {}

  @override
  Future<void> clear() async {}
}

void main() {
  testWidgets('App boots to the login screen when unauthenticated', (WidgetTester tester) async {
    await tester.pumpWidget(
      ProviderScope(
        overrides: [tokenStorageProvider.overrideWithValue(_FakeTokenStorage())],
        child: const PantheonApp(),
      ),
    );
    // A few pumps let the auth controller's session-restore Future (now resolved immediately
    // by the fake storage) settle, and go_router's `refreshListenable`-triggered redirect
    // re-evaluate, before asserting on the resulting screen.
    for (var i = 0; i < 5; i++) {
      await tester.pump();
    }

    expect(find.text('Bem-vindo de volta'), findsOneWidget);
  });
}
