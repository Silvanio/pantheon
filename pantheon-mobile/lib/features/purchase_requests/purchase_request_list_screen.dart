import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';

import '../../core/widgets/async_value_view.dart';
import '../../core/widgets/status_badge.dart';
import '../../theme/app_colors.dart';
import 'purchase_request_models.dart';
import 'purchase_request_repository.dart';

final _purchaseRequestListProvider =
    FutureProvider.family((ref, String siteId) => ref.watch(purchaseRequestRepositoryProvider).list(siteId, size: 50));

String _stageLabel(PurchaseRequest pr) {
  switch (pr.status) {
    case 'INICIADO':
      return 'Não orçado';
    case 'ORCADO':
      return pr.submittedAt != null ? 'Em aprovação' : 'Em orçamento';
    case 'CONFERIDO':
      return 'Aprovado';
    default:
      return 'Concluído';
  }
}

class PurchaseRequestListScreen extends ConsumerWidget {
  const PurchaseRequestListScreen({super.key, required this.siteId});
  final String siteId;

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final page = ref.watch(_purchaseRequestListProvider(siteId));
    return Scaffold(
      appBar: AppBar(title: const Text('Pedido de Compra')),
      body: RefreshIndicator(
        onRefresh: () => ref.refresh(_purchaseRequestListProvider(siteId).future),
        child: AsyncValueView(
          value: page,
          data: (p) {
            if (p.content.isEmpty) {
              return ListView(children: const [EmptyState(message: 'Nenhum pedido de compra criado ainda.')]);
            }
            return ListView.separated(
              padding: const EdgeInsets.all(16),
              itemCount: p.content.length,
              separatorBuilder: (_, _) => const SizedBox(height: 8),
              itemBuilder: (context, index) {
                final pr = p.content[index];
                final suppliers = pr.linkedOrcamentos.map((o) => o.fornecedorNome).join(', ');
                return Card(
                  child: ListTile(
                    onTap: () => context.push('/purchase-requests/${pr.id}'),
                    title: Text(pr.name, style: const TextStyle(fontWeight: FontWeight.w700)),
                    subtitle: Column(
                      crossAxisAlignment: CrossAxisAlignment.start,
                      children: [
                        const SizedBox(height: 4),
                        Text(_stageLabel(pr), style: const TextStyle(fontSize: 12.5)),
                        if (suppliers.isNotEmpty)
                          Text(suppliers, style: const TextStyle(fontSize: 12, color: AppColors.steel500)),
                      ],
                    ),
                    trailing: StatusBadge(kind: StatusBadgeKind.purchaseRequest, status: pr.status),
                    isThreeLine: suppliers.isNotEmpty,
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
