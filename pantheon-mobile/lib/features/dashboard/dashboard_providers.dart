import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../orcamentos/orcamento_repository.dart';
import '../purchase_requests/purchase_request_repository.dart';
import 'company_models.dart';
import 'dashboard_repository.dart';
import 'site_models.dart';

class DashboardData {
  DashboardData({required this.activeCompany, required this.sites, required this.pendingPurchaseRequests, required this.pendingOrcamentos});

  final CompanyMembership? activeCompany;
  final List<ConstructionSite> sites;
  final int pendingPurchaseRequests;
  final int pendingOrcamentos;

  int get activeSitesCount => sites.where((s) => s.status == 'PLANNING' || s.status == 'IN_PROGRESS').length;
}

/// Mirrors `pantheon-web`'s `DashboardView.vue` `loadSites`/`loadStats`: a site-only member
/// (no company) sees obras gathered across all their memberships instead of one company's list.
final dashboardDataProvider = FutureProvider<DashboardData>((ref) async {
  final dashboardRepo = ref.watch(dashboardRepositoryProvider);
  final prRepo = ref.watch(purchaseRequestRepositoryProvider);
  final orcRepo = ref.watch(orcamentoRepositoryProvider);

  final onboarding = await dashboardRepo.getOnboardingStatus();
  final activeCompany = onboarding.active;

  final sites = activeCompany != null ? await dashboardRepo.listSites(activeCompany.companyId) : await dashboardRepo.listMine();

  var pendingPr = 0;
  var pendingOrc = 0;
  await Future.wait(
    sites.map((site) async {
      try {
        final prPage = await prRepo.list(site.id, size: 100);
        pendingPr += prPage.content.where((pr) => pr.status != 'CONCLUIDO').length;
      } catch (_) {
        // Best-effort per site, mirroring the web dashboard's try/catch-per-site fallback.
      }
      try {
        final orcPage = await orcRepo.list(site.id, size: 100);
        pendingOrc += orcPage.content.where((o) => o.status == 'DRAFT').length;
      } catch (_) {}
    }),
  );

  return DashboardData(activeCompany: activeCompany, sites: sites, pendingPurchaseRequests: pendingPr, pendingOrcamentos: pendingOrc);
});
