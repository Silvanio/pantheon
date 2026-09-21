import 'package:flutter_riverpod/flutter_riverpod.dart';
import '../../core/api/api_client.dart';
import '../../core/models/page_response.dart';
import 'purchase_request_models.dart';

class PurchaseRequestRepository {
  PurchaseRequestRepository(this._client);
  final ApiClient _client;

  Future<PageResponse<PurchaseRequest>> list(String siteId, {String? status, int page = 0, int size = 20}) async {
    final json = await _client.get<Map<String, dynamic>>(
      '/api/construction-sites/$siteId/purchase-requests',
      query: {'page': page, 'size': size, 'status': ?status},
    );
    return PageResponse.fromJson(json, PurchaseRequest.fromJson);
  }

  Future<PurchaseRequestDetail> getDetail(String id) async {
    final json = await _client.get<Map<String, dynamic>>('/api/purchase-requests/$id');
    return PurchaseRequestDetail.fromJson(json);
  }

  Future<PurchaseRequestComparison> getComparison(String id) async {
    final json = await _client.get<Map<String, dynamic>>('/api/purchase-requests/$id/comparison');
    return PurchaseRequestComparison.fromJson(json);
  }

  Future<void> submitForApproval(String id) async {
    await _client.post<dynamic>('/api/purchase-requests/$id/submit');
  }

  Future<void> approveStep(String id, {String? comment}) async {
    final query = (comment != null && comment.isNotEmpty) ? '?comment=${Uri.encodeComponent(comment)}' : '';
    await _client.post<dynamic>('/api/purchase-requests/$id/approve-step$query');
  }

  Future<void> rejectStep(String id, String reason) async {
    await _client.post<dynamic>('/api/purchase-requests/$id/reject-step', body: {'reason': reason});
  }

  Future<void> conclude(String id) async {
    await _client.post<dynamic>('/api/purchase-requests/$id/conclude');
  }
}

final purchaseRequestRepositoryProvider = Provider((ref) => PurchaseRequestRepository(ref.watch(apiClientProvider)));
