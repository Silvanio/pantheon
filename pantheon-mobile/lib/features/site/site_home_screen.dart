import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';

import '../../core/widgets/async_value_view.dart';
import '../../core/widgets/site_photo.dart';
import '../../core/widgets/status_badge.dart';
import '../../theme/app_colors.dart';
import '../dashboard/dashboard_repository.dart';
import 'site_repository.dart';
import 'site_summary_section.dart';

final _siteProvider = FutureProvider.family((ref, String siteId) => ref.watch(dashboardRepositoryProvider).getSite(siteId));
final _myPermissionsProvider =
    FutureProvider.family((ref, String siteId) => ref.watch(siteRepositoryProvider).getMyPermissions(siteId));

/// Mirrors `pantheon-web`'s `SiteDetailView.vue` `listMyCompanies()` call, used the same way
/// there: to compute `isCompanyAdmin` and gate the Permissões tab, which — unlike every other
/// tab — is gated by company-admin role, not a `PermissionCapability`.
final _myCompanyMembershipsProvider =
    FutureProvider((ref) => ref.watch(dashboardRepositoryProvider).getOnboardingStatus().then((s) => s.companies));

class _Entry {
  const _Entry(this.label, this.icon, this.color, this.route, {this.capability, this.adminOnly = false});
  final String label;
  final IconData icon;
  final Color color;
  final String Function(String siteId) route;
  final String? capability;
  final bool adminOnly;
}

const _entries = [
  _Entry('Equipe', Icons.groups_outlined, AppColors.blueprint600, _teamRoute, capability: 'TEAM_MANAGE'),
  _Entry('Diário de Obra', Icons.article_outlined, AppColors.blueprint600, _dailyReportsRoute, capability: 'DAILY_REPORT'),
  _Entry('Pedido de Compra', Icons.shopping_cart_outlined, AppColors.amber700, _purchaseRequestsRoute, capability: 'PURCHASE_REQUEST'),
  _Entry('Orçamentos', Icons.attach_money, AppColors.emerald600, _orcamentosRoute, capability: 'ORCAMENTO_MANAGE'),
  _Entry('Materiais', Icons.local_shipping_outlined, AppColors.blueprint600, _materialsRoute, capability: 'ORCAMENTO_MANAGE'),
  _Entry('Tasks', Icons.view_kanban_outlined, AppColors.blueprint600, _tasksRoute, capability: 'TASKS'),
  _Entry('Equipamentos', Icons.construction_outlined, AppColors.steel600, _equipmentRoute, capability: 'EQUIPMENT'),
  _Entry('Projetos', Icons.folder_outlined, AppColors.steel600, _projectsRoute, capability: 'DOCUMENT_PROJECTS'),
  _Entry('Permissões', Icons.lock_outline, AppColors.steel600, _permissionsRoute, adminOnly: true),
];

String _teamRoute(String id) => '/sites/$id/team';
String _dailyReportsRoute(String id) => '/sites/$id/daily-reports';
String _purchaseRequestsRoute(String id) => '/sites/$id/purchase-requests';
String _orcamentosRoute(String id) => '/sites/$id/orcamentos';
String _materialsRoute(String id) => '/sites/$id/materials';
String _tasksRoute(String id) => '/sites/$id/tasks';
String _equipmentRoute(String id) => '/sites/$id/equipment';
String _projectsRoute(String id) => '/sites/$id/projects';
String _permissionsRoute(String id) => '/sites/$id/permissions';

const _statusOptions = ['PLANNING', 'IN_PROGRESS', 'PAUSED', 'COMPLETED'];

class SiteHomeScreen extends ConsumerWidget {
  const SiteHomeScreen({super.key, required this.siteId});
  final String siteId;

  Future<void> _openStatusSheet(BuildContext context, WidgetRef ref, String currentStatus) async {
    final selected = await showModalBottomSheet<String>(
      context: context,
      builder: (context) => SafeArea(
        child: Column(
          mainAxisSize: MainAxisSize.min,
          children: [
            const Padding(
              padding: EdgeInsets.fromLTRB(20, 16, 20, 4),
              child: Align(
                alignment: Alignment.centerLeft,
                child: Text('Status da obra', style: TextStyle(fontWeight: FontWeight.w800, fontSize: 16)),
              ),
            ),
            for (final status in _statusOptions)
              ListTile(
                leading: StatusBadge(kind: StatusBadgeKind.constructionSite, status: status),
                trailing: status == currentStatus ? const Icon(Icons.check, color: AppColors.blueprint600) : null,
                onTap: () => Navigator.pop(context, status),
              ),
          ],
        ),
      ),
    );
    if (selected == null || selected == currentStatus) return;
    try {
      await ref.read(dashboardRepositoryProvider).updateStatus(siteId, selected);
      ref.invalidate(_siteProvider(siteId));
    } catch (_) {
      if (context.mounted) {
        ScaffoldMessenger.of(context).showSnackBar(const SnackBar(content: Text('Não foi possível atualizar o status.')));
      }
    }
  }

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final site = ref.watch(_siteProvider(siteId));
    final permissions = ref.watch(_myPermissionsProvider(siteId));

    return Scaffold(
      backgroundColor: AppColors.steel50,
      body: AsyncValueView(
        value: site,
        data: (s) => CustomScrollView(
          slivers: [
            SliverAppBar(
              expandedHeight: 190,
              pinned: true,
              backgroundColor: AppColors.ink900,
              iconTheme: const IconThemeData(color: Colors.white),
              flexibleSpace: FlexibleSpaceBar(
                title: Text(s.name, style: const TextStyle(color: Colors.white, fontWeight: FontWeight.w700, fontSize: 15)),
                background: Stack(
                  fit: StackFit.expand,
                  children: [
                    SitePhoto(siteId: siteId, hasPhoto: s.photoObjectKey != null),
                    DecoratedBox(
                      decoration: BoxDecoration(
                        gradient: LinearGradient(
                          begin: Alignment.topCenter,
                          end: Alignment.bottomCenter,
                          colors: [Colors.black.withValues(alpha: 0.05), Colors.black.withValues(alpha: 0.65)],
                        ),
                      ),
                    ),
                    Positioned(
                      left: 16,
                      right: 16,
                      bottom: 44,
                      child: Column(
                        crossAxisAlignment: CrossAxisAlignment.start,
                        children: [
                          Row(
                            children: [
                              const Icon(Icons.place_outlined, size: 13, color: Colors.white70),
                              const SizedBox(width: 4),
                              Expanded(
                                child: Text(
                                  s.address,
                                  maxLines: 1,
                                  overflow: TextOverflow.ellipsis,
                                  style: const TextStyle(color: Colors.white70, fontSize: 12),
                                ),
                              ),
                            ],
                          ),
                          const SizedBox(height: 8),
                          permissions.maybeWhen(
                            data: (perms) => perms['SITE_STATUS'] == 'MANAGE'
                                ? InkWell(
                                    borderRadius: BorderRadius.circular(999),
                                    onTap: () => _openStatusSheet(context, ref, s.status),
                                    child: Row(
                                      mainAxisSize: MainAxisSize.min,
                                      children: [
                                        StatusBadge(kind: StatusBadgeKind.constructionSite, status: s.status),
                                        const SizedBox(width: 4),
                                        const Icon(Icons.edit, size: 13, color: Colors.white70),
                                      ],
                                    ),
                                  )
                                : StatusBadge(kind: StatusBadgeKind.constructionSite, status: s.status),
                            orElse: () => StatusBadge(kind: StatusBadgeKind.constructionSite, status: s.status),
                          ),
                        ],
                      ),
                    ),
                  ],
                ),
              ),
            ),
            SliverPadding(
              padding: const EdgeInsets.fromLTRB(16, 16, 16, 32),
              sliver: SliverList(
                delegate: SliverChildListDelegate([
                  SiteSummarySection(siteId: siteId),
                  const SizedBox(height: 20),
                  const Text('Acessar', style: TextStyle(fontWeight: FontWeight.w800, fontSize: 15)),
                  const SizedBox(height: 12),
                  permissions.when(
                    data: (perms) {
                      final memberships = ref.watch(_myCompanyMembershipsProvider);
                      final isCompanyAdmin = memberships.maybeWhen(
                        data: (list) => list.any((m) => m.companyId == s.companyId && m.role == 'ADMIN'),
                        // Don't hide anything before this loads — mirrors the web app's
                        // isTabVisible comment ("avoids a flash of a tab disappearing").
                        orElse: () => true,
                      );
                      final visible = _entries
                          .where((e) => e.adminOnly ? isCompanyAdmin : perms[e.capability] != 'HIDDEN')
                          .toList();
                      return GridView.builder(
                        shrinkWrap: true,
                        physics: const NeverScrollableScrollPhysics(),
                        gridDelegate: const SliverGridDelegateWithFixedCrossAxisCount(
                          crossAxisCount: 2,
                          mainAxisSpacing: 12,
                          crossAxisSpacing: 12,
                          childAspectRatio: 1.35,
                        ),
                        itemCount: visible.length,
                        itemBuilder: (context, index) => _EntryCard(entry: visible[index], siteId: siteId),
                      );
                    },
                    loading: () => const Padding(
                      padding: EdgeInsets.symmetric(vertical: 40),
                      child: Center(child: CircularProgressIndicator()),
                    ),
                    error: (_, _) => const Padding(
                      padding: EdgeInsets.symmetric(vertical: 24),
                      child: Text('Não foi possível carregar as permissões.', style: TextStyle(color: AppColors.steel500)),
                    ),
                  ),
                ]),
              ),
            ),
          ],
        ),
      ),
    );
  }
}

class _EntryCard extends StatelessWidget {
  const _EntryCard({required this.entry, required this.siteId});
  final _Entry entry;
  final String siteId;

  @override
  Widget build(BuildContext context) {
    return Material(
      color: Colors.white,
      borderRadius: BorderRadius.circular(16),
      child: InkWell(
        borderRadius: BorderRadius.circular(16),
        onTap: () => context.push(entry.route(siteId)),
        child: Container(
          padding: const EdgeInsets.all(14),
          decoration: BoxDecoration(
            borderRadius: BorderRadius.circular(16),
            border: Border.all(color: AppColors.steel200),
          ),
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            mainAxisAlignment: MainAxisAlignment.center,
            children: [
              Container(
                height: 38,
                width: 38,
                decoration: BoxDecoration(color: entry.color.withValues(alpha: 0.12), borderRadius: BorderRadius.circular(11)),
                alignment: Alignment.center,
                child: Icon(entry.icon, color: entry.color, size: 20),
              ),
              const SizedBox(height: 10),
              Text(entry.label, style: const TextStyle(fontWeight: FontWeight.w700, fontSize: 13.5)),
            ],
          ),
        ),
      ),
    );
  }
}
