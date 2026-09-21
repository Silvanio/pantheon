import 'package:flutter_riverpod/flutter_riverpod.dart';
import '../../core/api/api_client.dart';

class DeviceTokenRepository {
  DeviceTokenRepository(this._client);
  final ApiClient _client;

  Future<void> register(String token, String platform) async {
    await _client.post<dynamic>('/api/me/device-tokens', body: {'token': token, 'platform': platform});
  }

  Future<void> unregister(String token) async {
    await _client.delete<dynamic>('/api/me/device-tokens/${Uri.encodeComponent(token)}');
  }
}

final deviceTokenRepositoryProvider = Provider((ref) => DeviceTokenRepository(ref.watch(apiClientProvider)));
