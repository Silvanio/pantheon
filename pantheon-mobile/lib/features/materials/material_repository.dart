import 'package:dio/dio.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../core/api/api_client.dart';
import 'material_models.dart';

class MaterialRepository {
  MaterialRepository(this._client);
  final ApiClient _client;

  Future<List<MaterialDelivery>> list(String siteId) async {
    final json = await _client.get<List<dynamic>>('/api/construction-sites/$siteId/materials');
    return json.map((e) => MaterialDelivery.fromJson(e as Map<String, dynamic>)).toList();
  }

  /// Delivery-status transitions are workflow actions on a record other screens may already be
  /// showing (like purchase-request approvals) — never queued offline, so a failure surfaces
  /// immediately rather than looking like it worked until sync catches up. The screen calls
  /// `requireOnline` before invoking either of these.
  Future<MaterialDelivery> markDelivered(String id) async {
    final json = await _client.post<Map<String, dynamic>>('/api/materials/$id/mark-delivered');
    return MaterialDelivery.fromJson(json);
  }

  /// `photoPaths` is optional (mirrors the backend's `@RequestParam(required = false)`), so a
  /// material can be marked checked without attaching any photo.
  Future<MaterialDelivery> markChecked(String id, List<String> photoPaths) async {
    final formData = FormData();
    for (final path in photoPaths) {
      formData.files.add(MapEntry('photos', await MultipartFile.fromFile(path)));
    }
    final json = await _client.uploadMultipart<Map<String, dynamic>>('/api/materials/$id/mark-checked', formData);
    return MaterialDelivery.fromJson(json);
  }
}

final materialRepositoryProvider = Provider((ref) => MaterialRepository(ref.watch(apiClientProvider)));
