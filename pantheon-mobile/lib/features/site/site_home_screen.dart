import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';

import '../../core/widgets/async_value_view.dart';
import '../../core/widgets/status_badge.dart';
import '../../theme/app_colors.dart';
import '../dashboard/dashboard_repository.dart';
import 'site_repository.dart';

final _siteProvider = FutureProvider.family((ref, String siteId) => ref.watch(dashboardRepositoryProvider).getSite(siteId));
final _myPermissionsProvider =
    FutureProvider.family((ref, String siteId) => ref.watch(siteRepositoryProvider).getMyPermissions(siteId));

class _Entry {
  const _Entry(this.label, this.icon, this.route, this.capability);
  final String label;
  final IconData icon;
  final String Function(String siteId) route;
  final String? capability;
}

const _entries = [
  _Entry('Equipe', Icons.groups_outlined, _teamRoute, 'TEAM_MANAGE'),
  _Entry('Diário de Obra', Icons.article_outlined, _dailyReportsRoute, 'DAILY_REPORT'),
  _Entry('Pedido de Compra', Icons.shopping_cart_outlined, _purchaseRequestsRoute, 'PURCHASE_REQUEST'),
  _Entry('Orçamentos', Icons.attach_money, _orcamentosRoute, 'ORCAMENTO_MANAGE'),
  _Entry('Tasks', Icons.view_kanban_outlined, _tasksRoute, 'TASKS'),
  _Entry('Equipamentos', Icons.construction_outlined, _equipmentRoute, 'EQUIPMENT'),
  _Entry('Projetos', Icons.folder_outlined, _projectsRoute, 'DOCUMENT_PROJECTS'),
  _Entry('Permissões', Icons.lock_outline, _permissionsRoute, null),
];

String _teamRoute(String id) => '/sites/$id/team';
String _dailyReportsRoute(String id) => '/sites/$id/daily-reports';
String _purchaseRequestsRoute(String id) => '/sites/$id/purchase-requests';
String _orcamentosRoute(String id) => '/sites/$id/orcamentos';
String _tasksRoute(String id) => '/sites/$id/tasks';
String _equipmentRoute(String id) => '/sites/$id/equipment';
String _projectsRoute(String id) => '/sites/$id/projects';
String _permissionsRoute(String id) => '/sites/$id/permissions';

class SiteHomeScreen extends ConsumerWidget {
  const SiteHomeScreen({super.key, required this.siteId});
  final String siteId;

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final site = ref.watch(_siteProvider(siteId));
    final permissions = ref.watch(_myPermissionsProvider(siteId));

    return Scaffold(
      appBar: AppBar(
        title: site.when(
          data: (s) => Text(s.name),
          loading: () => const Text('Obra'),
          error: (_, _) => const Text('Obra'),
        ),
      ),
      body: AsyncValueView(
        value: site,
        data: (s) => Padding(
          padding: const EdgeInsets.all(16),
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.stretch,
            children: [
              Row(
                children: [
                  Expanded(
                    child: Text(s.address, style: const TextStyle(color: AppColors.steel500, fontSize: 13)),
                  ),
                  StatusBadge(kind: StatusBadgeKind.constructionSite, status: s.status),
                ],
              ),
              const SizedBox(height: 20),
              Expanded(
                child: permissions.when(
                  data: (perms) {
                    final visible = _entries.where((e) => e.capability == null || perms[e.capability] != 'HIDDEN').toList();
                    return GridView.builder(
                      gridDelegate: const SliverGridDelegateWithFixedCrossAxisCount(
                        crossAxisCount: 2,
                        mainAxisSpacing: 12,
                        crossAxisSpacing: 12,
                        childAspectRatio: 1.3,
                      ),
                      itemCount: visible.length,
                      itemBuilder: (context, index) {
                        final entry = visible[index];
                        return _EntryCard(entry: entry, siteId: siteId);
                      },
                    );
                  },
                  loading: () => const Center(child: CircularProgressIndicator()),
                  error: (_, _) => const Center(child: Text('Não foi possível carregar as permissões.')),
                ),
              ),
            ],
          ),
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
    return Card(
      clipBehavior: Clip.antiAlias,
      child: InkWell(
        onTap: () => context.push(entry.route(siteId)),
        child: Padding(
          padding: const EdgeInsets.all(16),
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            mainAxisAlignment: MainAxisAlignment.center,
            children: [
              Icon(entry.icon, color: AppColors.blueprint600, size: 26),
              const SizedBox(height: 10),
              Text(entry.label, style: const TextStyle(fontWeight: FontWeight.w700, fontSize: 13.5)),
            ],
          ),
        ),
      ),
    );
  }
}
