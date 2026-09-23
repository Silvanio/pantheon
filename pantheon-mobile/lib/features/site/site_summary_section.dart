import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../core/widgets/async_value_view.dart';
import '../../theme/app_colors.dart';
import 'site_repository.dart';
import 'site_summary_models.dart';

final siteSummaryProvider =
    FutureProvider.family((ref, String siteId) => ref.watch(siteRepositoryProvider).getSummary(siteId));

/// The obra summary — shown first on entering an obra, above the existing entry-card grid.
/// Horizontally-scrollable stat cards rather than a dense grid, sized for a phone screen.
class SiteSummarySection extends ConsumerWidget {
  const SiteSummarySection({super.key, required this.siteId});
  final String siteId;

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final summary = ref.watch(siteSummaryProvider(siteId));
    return AsyncValueView(
      value: summary,
      errorMessage: 'Não foi possível carregar o resumo da obra.',
      data: (s) {
        final pending =
            (s.purchaseRequests?.awaitingApproval ?? 0) + (s.orcamentos?.draft ?? 0);
        final cards = <Widget>[
          if (pending > 0) _PendingCard(count: pending),
          if (s.schedulePercentComplete != null) _ScheduleCard(percent: s.schedulePercentComplete!),
          if (s.dailyReports != null) _StatCard(
                title: 'Diário de Obra',
                icon: Icons.article_outlined,
                total: s.dailyReports!.total,
                subtitle: s.dailyReports!.lastReportDate != null ? 'Último em ${s.dailyReports!.lastReportDate}' : null,
              ),
          if (s.purchaseRequests != null) _RecentCard(
                title: 'Pedido de Compra',
                icon: Icons.shopping_cart_outlined,
                total: s.purchaseRequests!.total,
                recent: s.purchaseRequests!.recent,
              ),
          if (s.orcamentos != null) _RecentCard(
                title: 'Orçamentos',
                icon: Icons.attach_money,
                total: s.orcamentos!.total,
                recent: s.orcamentos!.recent,
              ),
          if (s.equipment != null) _StatCard(
                title: 'Equipamentos',
                icon: Icons.construction_outlined,
                total: s.equipment!.total,
                subtitle: s.equipment!.unavailable > 0 ? '${s.equipment!.unavailable} indisponível(is)' : null,
                subtitleColor: s.equipment!.unavailable > 0 ? AppColors.safety600 : null,
              ),
          if (s.tasks != null) _RecentCard(title: 'Tasks', icon: Icons.view_kanban_outlined, total: s.tasks!.total, recent: s.tasks!.recent),
          if (s.projects != null) _RecentCard(title: 'Projetos', icon: Icons.folder_outlined, total: s.projects!.total, recent: s.projects!.recent),
          if (s.teamMembersCount != null) _StatCard(title: 'Equipe', icon: Icons.groups_outlined, total: s.teamMembersCount!),
        ];
        if (cards.isEmpty) return const SizedBox.shrink();
        return SizedBox(
          height: 150,
          child: ListView.separated(
            scrollDirection: Axis.horizontal,
            itemCount: cards.length,
            separatorBuilder: (_, _) => const SizedBox(width: 10),
            itemBuilder: (context, index) => cards[index],
          ),
        );
      },
    );
  }
}

class _CardShell extends StatelessWidget {
  const _CardShell({required this.child, this.color, this.borderColor});
  final Widget child;
  final Color? color;
  final Color? borderColor;

  @override
  Widget build(BuildContext context) {
    return Container(
      width: 160,
      padding: const EdgeInsets.all(14),
      decoration: BoxDecoration(
        color: color ?? Colors.white,
        borderRadius: BorderRadius.circular(16),
        border: Border.all(color: borderColor ?? AppColors.steel200),
      ),
      child: child,
    );
  }
}

class _ScheduleCard extends StatelessWidget {
  const _ScheduleCard({required this.percent});
  final int percent;

  @override
  Widget build(BuildContext context) {
    return _CardShell(
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          const Text('Cronograma', style: TextStyle(fontSize: 11, fontWeight: FontWeight.w700, color: AppColors.steel500)),
          const Spacer(),
          Text('$percent%', style: const TextStyle(fontSize: 22, fontWeight: FontWeight.w800, color: AppColors.emerald600)),
          const SizedBox(height: 6),
          ClipRRect(
            borderRadius: BorderRadius.circular(4),
            child: LinearProgressIndicator(
              value: percent / 100,
              minHeight: 6,
              backgroundColor: AppColors.steel100,
              valueColor: const AlwaysStoppedAnimation(AppColors.emerald600),
            ),
          ),
        ],
      ),
    );
  }
}

class _PendingCard extends StatelessWidget {
  const _PendingCard({required this.count});
  final int count;

  @override
  Widget build(BuildContext context) {
    return _CardShell(
      color: AppColors.amber50,
      borderColor: AppColors.amber600.withValues(alpha: 0.3),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          const Text('Pendências', style: TextStyle(fontSize: 11, fontWeight: FontWeight.w700, color: AppColors.amber700)),
          const Spacer(),
          Text('$count', style: const TextStyle(fontSize: 26, fontWeight: FontWeight.w800, color: AppColors.amber700)),
          const SizedBox(height: 4),
          const Text('Aguardando ação', style: TextStyle(fontSize: 11, color: AppColors.amber700)),
        ],
      ),
    );
  }
}

class _StatCard extends StatelessWidget {
  const _StatCard({required this.title, required this.icon, required this.total, this.subtitle, this.subtitleColor});
  final String title;
  final IconData icon;
  final int total;
  final String? subtitle;
  final Color? subtitleColor;

  @override
  Widget build(BuildContext context) {
    return _CardShell(
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Icon(icon, size: 18, color: AppColors.blueprint600),
          const SizedBox(height: 6),
          Text(title, style: const TextStyle(fontSize: 11, fontWeight: FontWeight.w700, color: AppColors.steel500)),
          const Spacer(),
          Text('$total', style: const TextStyle(fontSize: 22, fontWeight: FontWeight.w800, color: AppColors.blueprint600)),
          if (subtitle != null)
            Text(
              subtitle!,
              maxLines: 1,
              overflow: TextOverflow.ellipsis,
              style: TextStyle(fontSize: 10.5, color: subtitleColor ?? AppColors.steel500),
            ),
        ],
      ),
    );
  }
}

class _RecentCard extends StatelessWidget {
  const _RecentCard({required this.title, required this.icon, required this.total, required this.recent});
  final String title;
  final IconData icon;
  final int total;
  final List<RecentItem> recent;

  @override
  Widget build(BuildContext context) {
    return _CardShell(
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Row(
            children: [
              Icon(icon, size: 16, color: AppColors.blueprint600),
              const SizedBox(width: 6),
              Expanded(child: Text(title, style: const TextStyle(fontSize: 11, fontWeight: FontWeight.w700, color: AppColors.steel500))),
            ],
          ),
          const SizedBox(height: 4),
          Text('$total', style: const TextStyle(fontSize: 18, fontWeight: FontWeight.w800, color: AppColors.blueprint600)),
          const SizedBox(height: 4),
          ...recent.take(2).map(
                (item) => Padding(
                  padding: const EdgeInsets.only(top: 2),
                  child: Row(
                    children: [
                      Expanded(
                        child: Text(item.title, maxLines: 1, overflow: TextOverflow.ellipsis, style: const TextStyle(fontSize: 10.5)),
                      ),
                      if (item.isNew) ...[
                        const SizedBox(width: 4),
                        Container(
                          padding: const EdgeInsets.symmetric(horizontal: 4, vertical: 1),
                          decoration: BoxDecoration(color: AppColors.emerald50, borderRadius: BorderRadius.circular(999)),
                          child: const Text('Novo', style: TextStyle(fontSize: 8, fontWeight: FontWeight.w800, color: AppColors.emerald700)),
                        ),
                      ],
                    ],
                  ),
                ),
              ),
        ],
      ),
    );
  }
}
