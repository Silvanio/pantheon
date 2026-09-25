import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';

import '../../core/widgets/async_value_view.dart';
import '../../core/widgets/offline_dialogs.dart';
import '../../theme/app_colors.dart';
import 'daily_report_repository.dart';

final dailyReportListProvider =
    FutureProvider.family((ref, String siteId) => ref.watch(dailyReportRepositoryProvider).list(siteId));

class DailyReportListScreen extends ConsumerWidget {
  const DailyReportListScreen({super.key, required this.siteId});
  final String siteId;

  Future<void> _createReport(BuildContext context, WidgetRef ref) async {
    final date = await showDatePicker(
      context: context,
      initialDate: DateTime.now(),
      firstDate: DateTime(2020),
      lastDate: DateTime(2100),
    );
    if (date == null) return;
    if (!context.mounted || !await confirmProceedOffline(context, ref)) return;
    final iso = date.toIso8601String().split('T').first;
    try {
      final sentLive = await ref.read(dailyReportRepositoryProvider).create(siteId, iso);
      ref.invalidate(dailyReportListProvider(siteId));
      if (!sentLive && context.mounted) {
        await showOfflineSavedDialog(context);
      }
    } catch (_) {
      if (context.mounted) {
        ScaffoldMessenger.of(context).showSnackBar(
          const SnackBar(content: Text('Não foi possível criar o relatório. Já existe um para esta data?')),
        );
      }
    }
  }

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final reports = ref.watch(dailyReportListProvider(siteId));
    return Scaffold(
      backgroundColor: AppColors.steel50,
      appBar: AppBar(
        title: const Text('Diário de Obra'),
        actions: [IconButton(icon: const Icon(Icons.add), onPressed: () => _createReport(context, ref))],
      ),
      floatingActionButton: FloatingActionButton.extended(
        onPressed: () => _createReport(context, ref),
        icon: const Icon(Icons.add),
        label: const Text('Novo relatório'),
      ),
      body: RefreshIndicator(
        onRefresh: () => ref.refresh(dailyReportListProvider(siteId).future),
        child: AsyncValueView(
          value: reports,
          data: (list) {
            if (list.isEmpty) {
              return ListView(
                children: const [
                  EmptyState(icon: Icons.article_outlined, message: 'Nenhum relatório registrado ainda.'),
                ],
              );
            }
            return ListView.separated(
              padding: const EdgeInsets.fromLTRB(16, 16, 16, 96),
              itemCount: list.length,
              separatorBuilder: (_, _) => const SizedBox(height: 10),
              itemBuilder: (context, index) {
                final r = list[index];
                final isDraft = r.status == 'DRAFT';
                return Material(
                  color: Colors.white,
                  borderRadius: BorderRadius.circular(16),
                  child: InkWell(
                    borderRadius: BorderRadius.circular(16),
                    onTap: () => context.push('/daily-reports/${r.id}'),
                    child: Container(
                      padding: const EdgeInsets.all(14),
                      decoration: BoxDecoration(
                        borderRadius: BorderRadius.circular(16),
                        border: Border.all(color: AppColors.steel200),
                      ),
                      child: Row(
                        children: [
                          Container(
                            height: 42,
                            width: 42,
                            decoration: BoxDecoration(color: AppColors.blueprint50, borderRadius: BorderRadius.circular(12)),
                            alignment: Alignment.center,
                            child: const Icon(Icons.article_outlined, color: AppColors.blueprint600, size: 20),
                          ),
                          const SizedBox(width: 12),
                          Expanded(
                            child: Column(
                              crossAxisAlignment: CrossAxisAlignment.start,
                              children: [
                                Text('Relatório #${r.sequenceNo}', style: const TextStyle(fontWeight: FontWeight.w700, fontSize: 14.5)),
                                const SizedBox(height: 3),
                                Text(r.reportDate, style: const TextStyle(fontSize: 12.5, color: AppColors.steel500)),
                              ],
                            ),
                          ),
                          Container(
                            padding: const EdgeInsets.symmetric(horizontal: 10, vertical: 4),
                            decoration: BoxDecoration(
                              color: isDraft ? AppColors.amber50 : AppColors.emerald50,
                              borderRadius: BorderRadius.circular(999),
                            ),
                            child: Text(
                              isDraft ? 'Rascunho' : 'Enviado',
                              style: TextStyle(
                                color: isDraft ? AppColors.amber700 : AppColors.emerald700,
                                fontWeight: FontWeight.w700,
                                fontSize: 11.5,
                              ),
                            ),
                          ),
                        ],
                      ),
                    ),
                  ),
                );
              },
            );
          },
        ),
      ),
    );
  }
}
