import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../orcamentos/orcamento_repository.dart';
import '../purchase_requests/purchase_request_repository.dart';
import 'company_models.dart';
import 'dashboard_repository.dart';
import 'site_models.dart';

/// Per-site pending counts backing each dashboard card's stat pills — mirrors
/// `pantheon-web`'s `DashboardView.vue` `statsBySite`.
class SitePendingStats {
  const SitePendingStats({required this.pendingPurchaseRequests, required this.pendingOrcamentos});
  final int pendingPurchaseRequests;
  final int pendingOrcamentos;
}

class DashboardData {
  DashboardData({
    required this.activeCompany,
    required this.sites,
    required this.pendingPurchaseRequests,
    required this.pendingOrcamentos,
    required this.statsBySite,
  });

  final CompanyMembership? activeCompany;
  final List<ConstructionSite> sites;
  final int pendingPurchaseRequests;
  final int pendingOrcamentos;
  final Map<String, SitePendingStats> statsBySite;

  int get activeSitesCount => sites.where((s) => s.status == 'PLANNING' || s.status == 'IN_PROGRESS').length;

  SitePendingStats statsFor(String siteId) => statsBySite[siteId] ?? const SitePendingStats(pendingPurchaseRequests: 0, pendingOrcamentos: 0);
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
  final statsBySite = <String, SitePendingStats>{};
  await Future.wait(
    sites.map((site) async {
      var sitePendingPr = 0;
      var sitePendingOrc = 0;
      try {
        final prPage = await prRepo.list(site.id, size: 100);
        sitePendingPr = prPage.content.where((pr) => pr.status != 'CONCLUIDO').length;
      } catch (_) {
        // Best-effort per site, mirroring the web dashboard's try/catch-per-site fallback.
      }
      try {
        final orcPage = await orcRepo.list(site.id, size: 100);
        sitePendingOrc = orcPage.content.where((o) => o.status == 'DRAFT').length;
      } catch (_) {}
      pendingPr += sitePendingPr;
      pendingOrc += sitePendingOrc;
      statsBySite[site.id] = SitePendingStats(pendingPurchaseRequests: sitePendingPr, pendingOrcamentos: sitePendingOrc);
    }),
  );

  return DashboardData(
    activeCompany: activeCompany,
    sites: sites,
    pendingPurchaseRequests: pendingPr,
    pendingOrcamentos: pendingOrc,
    statsBySite: statsBySite,
  );
});
