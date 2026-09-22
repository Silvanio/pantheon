import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';

import '../../core/widgets/async_value_view.dart';
import '../../core/widgets/offline_dialogs.dart';
import '../../theme/app_colors.dart';
import 'daily_report_repository.dart';

final dailyReportListProvider =
    FutureProvider.family((ref, String siteId) => ref.watch(dailyReportRepositoryProvider).list(siteId));

const _statusLabels = {'DRAFT': 'Rascunho', 'SUBMITTED': 'Enviado'};

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
      appBar: AppBar(
        title: const Text('Diário de Obra'),
        actions: [IconButton(icon: const Icon(Icons.add), onPressed: () => _createReport(context, ref))],
      ),
      body: RefreshIndicator(
        onRefresh: () => ref.refresh(dailyReportListProvider(siteId).future),
        child: AsyncValueView(
          value: reports,
          data: (list) {
            if (list.isEmpty) return ListView(children: const [EmptyState(message: 'Nenhum relatório registrado ainda.')]);
            return ListView.separated(
              padding: const EdgeInsets.all(16),
              itemCount: list.length,
              separatorBuilder: (_, _) => const SizedBox(height: 8),
              itemBuilder: (context, index) {
                final r = list[index];
                return Card(
                  child: ListTile(
                    onTap: () => context.push('/daily-reports/${r.id}'),
                    title: Text('Relatório #${r.sequenceNo}', style: const TextStyle(fontWeight: FontWeight.w700)),
                    subtitle: Text(r.reportDate),
                    trailing: Text(_statusLabels[r.status] ?? r.status, style: const TextStyle(color: AppColors.steel500)),
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
