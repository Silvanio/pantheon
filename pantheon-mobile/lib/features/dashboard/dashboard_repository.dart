import 'package:flutter_riverpod/flutter_riverpod.dart';
import '../../core/api/api_client.dart';
import 'company_models.dart';
import 'site_models.dart';

class DashboardRepository {
  DashboardRepository(this._client);
  final ApiClient _client;

  Future<OnboardingStatus> getOnboardingStatus() async {
    final json = await _client.get<Map<String, dynamic>>('/api/onboarding/status');
    return OnboardingStatus.fromJson(json);
  }

  Future<List<ConstructionSite>> listSites(String companyId) async {
    final json = await _client.get<List<dynamic>>('/api/companies/$companyId/construction-sites');
    return json.map((e) => ConstructionSite.fromJson(e as Map<String, dynamic>)).toList();
  }

  Future<List<ConstructionSite>> listMine() async {
    final json = await _client.get<List<dynamic>>('/api/construction-sites/mine');
    return json.map((e) => ConstructionSite.fromJson(e as Map<String, dynamic>)).toList();
  }

  Future<ConstructionSite> getSite(String siteId) async {
    final json = await _client.get<Map<String, dynamic>>('/api/construction-sites/$siteId');
    return ConstructionSite.fromJson(json);
  }

  /// Gated server-side by the `SITE_STATUS` permission capability (defaults to `MANAGE` for
  /// ADMIN/ENGINEER site functions only) — the screen checks `getMyPermissions` before showing
  /// any control that calls this. Never queued offline: like the purchase-request approval
  /// workflow, a status change is a small, high-signal action best surfaced immediately if it
  /// fails rather than silently deferred.
  Future<ConstructionSite> updateStatus(String siteId, String status) async {
    final json = await _client.patch<Map<String, dynamic>>(
      '/api/construction-sites/$siteId/status',
      body: {'status': status},
    );
    return ConstructionSite.fromJson(json);
  }
}

final dashboardRepositoryProvider = Provider((ref) => DashboardRepository(ref.watch(apiClientProvider)));
