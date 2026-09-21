import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';

import '../../core/widgets/app_bottom_nav.dart';
import '../../core/widgets/async_value_view.dart';
import '../../core/widgets/status_badge.dart';
import '../../theme/app_colors.dart';
import 'dashboard_providers.dart';
import 'site_models.dart';

class DashboardScreen extends ConsumerWidget {
  const DashboardScreen({super.key});

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final dashboard = ref.watch(dashboardDataProvider);
    return Scaffold(
      appBar: AppBar(title: const Text('Minhas obras')),
      bottomNavigationBar: const AppBottomNav(currentIndex: 0),
      body: RefreshIndicator(
        onRefresh: () => ref.refresh(dashboardDataProvider.future),
        child: AsyncValueView(
          value: dashboard,
          data: (data) {
            final summary = [
              '${data.activeSitesCount} obras ativas',
              if (data.pendingPurchaseRequests > 0) '${data.pendingPurchaseRequests} aguardando aprovação de pedidos',
              if (data.pendingOrcamentos > 0) '${data.pendingOrcamentos} orçamentos pendentes',
            ].join(' · ');

            if (data.sites.isEmpty) {
              return ListView(
                padding: const EdgeInsets.all(24),
                children: const [EmptyState(message: 'Nenhuma obra cadastrada ainda.', icon: Icons.apartment_outlined)],
              );
            }

            return ListView.separated(
              padding: const EdgeInsets.fromLTRB(16, 8, 16, 24),
              itemCount: data.sites.length + 1,
              separatorBuilder: (_, _) => const SizedBox(height: 12),
              itemBuilder: (context, index) {
                if (index == 0) {
                  return Padding(
                    padding: const EdgeInsets.symmetric(horizontal: 4, vertical: 8),
                    child: Text(summary, style: const TextStyle(color: AppColors.steel500, fontSize: 13)),
                  );
                }
                final site = data.sites[index - 1];
                return _SiteCard(site: site);
              },
            );
          },
        ),
      ),
    );
  }
}

class _SiteCard extends StatelessWidget {
  const _SiteCard({required this.site});
  final ConstructionSite site;

  @override
  Widget build(BuildContext context) {
    return Card(
      clipBehavior: Clip.antiAlias,
      child: InkWell(
        onTap: () => context.push('/sites/${site.id}'),
        child: Padding(
          padding: const EdgeInsets.all(16),
          child: Row(
            children: [
              Container(
                height: 48,
                width: 48,
                decoration: BoxDecoration(
                  gradient: const LinearGradient(colors: [AppColors.blueprint600, AppColors.ink900]),
                  borderRadius: BorderRadius.circular(12),
                ),
                alignment: Alignment.center,
                child: const Icon(Icons.apartment, color: Colors.white, size: 22),
              ),
              const SizedBox(width: 14),
              Expanded(
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    Text(site.name, style: const TextStyle(fontWeight: FontWeight.w800, fontSize: 15)),
                    const SizedBox(height: 3),
                    Text(
                      site.address,
                      maxLines: 1,
                      overflow: TextOverflow.ellipsis,
                      style: const TextStyle(color: AppColors.steel500, fontSize: 12.5),
                    ),
                  ],
                ),
              ),
              const SizedBox(width: 8),
              StatusBadge(kind: StatusBadgeKind.constructionSite, status: site.status),
            ],
          ),
        ),
      ),
    );
  }
}
