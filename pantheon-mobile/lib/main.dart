import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import 'core/auth/auth_provider.dart';
import 'core/router/app_router.dart';
import 'features/notifications/push_notification_service.dart';
import 'theme/app_theme.dart';

void main() {
  runApp(
    ProviderScope(
      overrides: [
        deviceTokenUnregisterProvider.overrideWith(
          (ref) => () => ref.read(pushNotificationServiceProvider).unregisterCurrent(),
        ),
      ],
      child: const PantheonApp(),
    ),
  );
}

class PantheonApp extends ConsumerStatefulWidget {
  const PantheonApp({super.key});

  @override
  ConsumerState<PantheonApp> createState() => _PantheonAppState();
}

class _PantheonAppState extends ConsumerState<PantheonApp> {
  @override
  void initState() {
    super.initState();
    // Fire-and-forget: no-ops gracefully if Firebase isn't configured yet (see
    // PushNotificationService's doc comment and README.md).
    WidgetsBinding.instance.addPostFrameCallback((_) {
      ref.read(pushNotificationServiceProvider).initialize();
    });
  }

  @override
  Widget build(BuildContext context) {
    final router = ref.watch(appRouterProvider);
    return MaterialApp.router(
      title: 'Pantheon',
      debugShowCheckedModeBanner: false,
      theme: AppTheme.light(),
      darkTheme: AppTheme.dark(),
      themeMode: ThemeMode.system,
      routerConfig: router,
    );
  }
}
