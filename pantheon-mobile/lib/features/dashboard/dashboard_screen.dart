import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';

import '../../core/auth/auth_provider.dart';
import '../../core/widgets/app_bottom_nav.dart';
import '../../core/widgets/async_value_view.dart';
import '../../core/widgets/site_photo.dart';
import '../../core/widgets/status_badge.dart';
import '../../theme/app_colors.dart';
import 'dashboard_providers.dart';
import 'site_models.dart';

String _greeting() {
  final hour = DateTime.now().hour;
  if (hour < 12) return 'Bom dia';
  if (hour < 18) return 'Boa tarde';
  return 'Boa noite';
}

class DashboardScreen extends ConsumerWidget {
  const DashboardScreen({super.key});

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final dashboard = ref.watch(dashboardDataProvider);
    final auth = ref.watch(authControllerProvider);
    final name = auth.email?.split('@').first;

    return Scaffold(
      backgroundColor: AppColors.steel50,
      bottomNavigationBar: const AppBottomNav(currentIndex: 0),
      body: SafeArea(
        bottom: false,
        child: RefreshIndicator(
          onRefresh: () => ref.refresh(dashboardDataProvider.future),
          child: AsyncValueView(
            value: dashboard,
            data: (data) {
              return CustomScrollView(
                slivers: [
                  SliverPadding(
                    padding: const EdgeInsets.fromLTRB(20, 20, 20, 4),
                    sliver: SliverToBoxAdapter(
                      child: Column(
                        crossAxisAlignment: CrossAxisAlignment.start,
                        children: [
                          Text(
                            '${_greeting()}${name != null ? ', $name' : ''}',
                            style: const TextStyle(fontSize: 21, fontWeight: FontWeight.w800, color: AppColors.steel800),
                          ),
                          const SizedBox(height: 4),
                          Text(
                            data.activeCompany?.companyName ?? 'Minhas obras',
                            style: const TextStyle(fontSize: 13.5, color: AppColors.steel500),
                          ),
                          const SizedBox(height: 16),
                          Row(
                            children: [
                              Expanded(
                                child: _SummaryPill(
                                  icon: Icons.apartment_outlined,
                                  value: '${data.activeSitesCount}',
                                  label: 'obras ativas',
                                  color: AppColors.blueprint600,
                                ),
                              ),
                              const SizedBox(width: 10),
                              Expanded(
                                child: _SummaryPill(
                                  icon: Icons.shopping_cart_outlined,
                                  value: '${data.pendingPurchaseRequests}',
                                  label: 'pedidos pendentes',
                                  color: AppColors.amber700,
                                ),
                              ),
                              const SizedBox(width: 10),
                              Expanded(
                                child: _SummaryPill(
                                  icon: Icons.attach_money,
                                  value: '${data.pendingOrcamentos}',
                                  label: 'orçamentos em aberto',
                                  color: AppColors.emerald600,
                                ),
                              ),
                            ],
                          ),
                        ],
                      ),
                    ),
                  ),
                  if (data.sites.isEmpty)
                    const SliverFillRemaining(
                      hasScrollBody: false,
                      child: Padding(
                        padding: EdgeInsets.all(24),
                        child: EmptyState(message: 'Nenhuma obra cadastrada ainda.', icon: Icons.apartment_outlined),
                      ),
                    )
                  else
                    SliverPadding(
                      padding: const EdgeInsets.fromLTRB(16, 12, 16, 24),
                      sliver: SliverList(
                        delegate: SliverChildBuilderDelegate(
                          (context, index) => Padding(
                            padding: const EdgeInsets.only(bottom: 14),
                            child: _SiteCard(site: data.sites[index], stats: data.statsFor(data.sites[index].id)),
                          ),
                          childCount: data.sites.length,
                        ),
                      ),
                    ),
                ],
              );
            },
          ),
        ),
      ),
    );
  }
}

class _SummaryPill extends StatelessWidget {
  const _SummaryPill({required this.icon, required this.value, required this.label, required this.color});
  final IconData icon;
  final String value;
  final String label;
  final Color color;

  @override
  Widget build(BuildContext context) {
    return Container(
      padding: const EdgeInsets.symmetric(horizontal: 10, vertical: 10),
      decoration: BoxDecoration(color: Colors.white, borderRadius: BorderRadius.circular(14), border: Border.all(color: AppColors.steel200)),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Icon(icon, size: 16, color: color),
          const SizedBox(height: 6),
          Text(value, style: TextStyle(fontSize: 17, fontWeight: FontWeight.w800, color: color)),
          Text(label, maxLines: 2, style: const TextStyle(fontSize: 10, color: AppColors.steel500)),
        ],
      ),
    );
  }
}

class _SiteCard extends StatelessWidget {
  const _SiteCard({required this.site, required this.stats});
  final ConstructionSite site;
  final SitePendingStats stats;

  @override
  Widget build(BuildContext context) {
    return Material(
      color: Colors.white,
      borderRadius: BorderRadius.circular(20),
      clipBehavior: Clip.antiAlias,
      elevation: 0,
      child: InkWell(
        onTap: () => context.push('/sites/${site.id}'),
        child: Container(
          decoration: BoxDecoration(borderRadius: BorderRadius.circular(20), border: Border.all(color: AppColors.steel200)),
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              SizedBox(
                height: 120,
                width: double.infinity,
                child: Stack(
                  fit: StackFit.expand,
                  children: [
                    SitePhoto(siteId: site.id, hasPhoto: site.photoObjectKey != null),
                    DecoratedBox(
                      decoration: BoxDecoration(
                        gradient: LinearGradient(
                          begin: Alignment.topCenter,
                          end: Alignment.bottomCenter,
                          colors: [Colors.transparent, Colors.black.withValues(alpha: 0.55)],
                        ),
                      ),
                    ),
                    Positioned(
                      left: 14,
                      right: 14,
                      bottom: 10,
                      child: Row(
                        children: [
                          Expanded(
                            child: Text(
                              site.name,
                              maxLines: 1,
                              overflow: TextOverflow.ellipsis,
                              style: const TextStyle(color: Colors.white, fontWeight: FontWeight.w800, fontSize: 15.5),
                            ),
                          ),
                        ],
                      ),
                    ),
                    Positioned(
                      top: 10,
                      right: 10,
                      child: StatusBadge(kind: StatusBadgeKind.constructionSite, status: site.status),
                    ),
                  ],
                ),
              ),
              Padding(
                padding: const EdgeInsets.all(14),
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    Row(
                      children: [
                        const Icon(Icons.place_outlined, size: 13, color: AppColors.steel400),
                        const SizedBox(width: 4),
                        Expanded(
                          child: Text(
                            site.address,
                            maxLines: 1,
                            overflow: TextOverflow.ellipsis,
                            style: const TextStyle(color: AppColors.steel500, fontSize: 12),
                          ),
                        ),
                      ],
                    ),
                    const SizedBox(height: 12),
                    Row(
                      children: [
                        const Text('Cronograma', style: TextStyle(fontSize: 11, fontWeight: FontWeight.w700, color: AppColors.steel500)),
                        const Spacer(),
                        Text('${site.progressPercent}%', style: const TextStyle(fontSize: 12, fontWeight: FontWeight.w800, color: AppColors.emerald600)),
                      ],
                    ),
                    const SizedBox(height: 5),
                    ClipRRect(
                      borderRadius: BorderRadius.circular(4),
                      child: LinearProgressIndicator(
                        value: site.progressPercent / 100,
                        minHeight: 6,
                        backgroundColor: AppColors.steel100,
                        valueColor: const AlwaysStoppedAnimation(AppColors.emerald600),
                      ),
                    ),
                    if (stats.pendingPurchaseRequests > 0 || stats.pendingOrcamentos > 0) ...[
                      const SizedBox(height: 12),
                      Wrap(
                        spacing: 8,
                        runSpacing: 8,
                        children: [
                          if (stats.pendingPurchaseRequests > 0)
                            _Pill(
                              icon: Icons.shopping_cart_outlined,
                              label: '${stats.pendingPurchaseRequests} pedido(s) pendente(s)',
                              color: AppColors.amber700,
                              background: AppColors.amber50,
                            ),
                          if (stats.pendingOrcamentos > 0)
                            _Pill(
                              icon: Icons.attach_money,
                              label: '${stats.pendingOrcamentos} orçamento(s) em rascunho',
                              color: AppColors.blueprint700,
                              background: AppColors.blueprint50,
                            ),
                        ],
                      ),
                    ],
                  ],
                ),
              ),
            ],
          ),
        ),
      ),
    );
  }
}

class _Pill extends StatelessWidget {
  const _Pill({required this.icon, required this.label, required this.color, required this.background});
  final IconData icon;
  final String label;
  final Color color;
  final Color background;

  @override
  Widget build(BuildContext context) {
    return Container(
      padding: const EdgeInsets.symmetric(horizontal: 9, vertical: 5),
      decoration: BoxDecoration(color: background, borderRadius: BorderRadius.circular(999)),
      child: Row(
        mainAxisSize: MainAxisSize.min,
        children: [
          Icon(icon, size: 12, color: color),
          const SizedBox(width: 4),
          Text(label, style: TextStyle(fontSize: 10.5, fontWeight: FontWeight.w700, color: color)),
        ],
      ),
    );
  }
}
