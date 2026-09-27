import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:sentry_flutter/sentry_flutter.dart';

import 'core/auth/auth_provider.dart';
import 'core/router/app_router.dart';
import 'core/widgets/sync_status_badge.dart';
import 'features/notifications/push_notification_service.dart';
import 'theme/app_theme.dart';

/// Blank (the local-dev default via `env/local-simulator.json`/`env/local-device.json`) means
/// Sentry stays fully disabled — no init, no network calls. Only `env/prod.json` sets a real
/// value. See openspec/changes/add-sentry-error-tracking/.
const _sentryDsn = String.fromEnvironment('SENTRY_DSN');

/// Tags events with which tier sent them (`local`/`dev`/`prod`). No existing environment-name
/// concept exists in this app yet (unlike `pantheon-service`'s Spring profiles), so this is a
/// standalone dart-define; defaults to `local` since that's the safe assumption when unset.
const _sentryEnvironment = String.fromEnvironment('SENTRY_ENVIRONMENT', defaultValue: 'local');

void main() {
  if (_sentryDsn.isEmpty) {
    debugPrint('Sentry disabled: SENTRY_DSN not set');
    _runPantheonApp();
    return;
  }

  SentryFlutter.init(
    (options) {
      options.dsn = _sentryDsn;
      options.environment = _sentryEnvironment;
      // Errors/crashes are the goal here, not latency profiling — keep tracing near-zero so the
      // free plan's (separate, tighter) performance-unit quota isn't spent by accident. Release
      // is left unset deliberately: sentry_flutter's bundled LoadReleaseIntegration already
      // derives `<packageName>@<version>+<buildNumber>` from the native app bundle's version
      // info at runtime (itself populated from pubspec.yaml's `version:` at build time), so
      // there's no need for an extra package_info_plus dependency just to read it ourselves.
      options.tracesSampleRate = 0.01;
      // Keep default PII scrubbing on (i.e. do NOT set sendDefaultPii = true).
    },
    appRunner: _runPantheonApp,
  );
  debugPrint('Sentry enabled (environment: $_sentryEnvironment)');
}

void _runPantheonApp() {
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
    final isAuthenticated = ref.watch(authControllerProvider.select((s) => s.status == AuthStatus.authenticated));
    return MaterialApp.router(
      title: 'Pantheon',
      debugShowCheckedModeBanner: false,
      theme: AppTheme.light(),
      darkTheme: AppTheme.dark(),
      themeMode: ThemeMode.system,
      routerConfig: router,
      // The offline/pending-sync indicator floats above every authenticated screen — see
      // SyncStatusBadge's doc comment.
      builder: (context, child) => Stack(
        children: [
          ?child,
          if (isAuthenticated) const SyncStatusBadge(),
        ],
      ),
    );
  }
}
