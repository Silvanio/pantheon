import 'dart:io' show Platform;
import 'package:flutter/foundation.dart' show kIsWeb;

/// Backend base URL. Override at build/run time with
/// `--dart-define=PANTHEON_SERVICE_URL=https://your-host:8081` (e.g. pointing at a real
/// server instead of a local dev backend). Defaults mirror `pantheon-web`'s dev setup
/// (`http://localhost:8081`), adjusted per platform since "localhost" from an Android
/// emulator or a physical device does not reach the host machine the same way a desktop
/// browser does.
class ApiConfig {
  ApiConfig._();

  static const _override = String.fromEnvironment('PANTHEON_SERVICE_URL');

  static String get baseUrl {
    if (_override.isNotEmpty) return _override;
    if (kIsWeb) return 'http://localhost:8081';
    if (Platform.isAndroid) {
      // 10.0.2.2 is the Android emulator's alias for the host machine's localhost.
      return 'http://10.0.2.2:8081';
    }
    // iOS Simulator shares the host's network namespace, so localhost works directly.
    return 'http://localhost:8081';
  }
}
