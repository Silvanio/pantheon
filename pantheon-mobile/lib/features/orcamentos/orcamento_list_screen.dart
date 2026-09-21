import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';

import '../../core/widgets/async_value_view.dart';
import '../../core/widgets/status_badge.dart';
import 'orcamento_repository.dart';

final _listProvider = FutureProvider.family((ref, String siteId) => ref.watch(orcamentoRepositoryProvider).list(siteId, size: 50));

class OrcamentoListScreen extends ConsumerWidget {
  const OrcamentoListScreen({super.key, required this.siteId});
  final String siteId;

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final page = ref.watch(_listProvider(siteId));
    return Scaffold(
      appBar: AppBar(title: const Text('Orçamentos')),
      body: RefreshIndicator(
        onRefresh: () => ref.refresh(_listProvider(siteId).future),
        child: AsyncValueView(
          value: page,
          data: (p) {
            if (p.content.isEmpty) return ListView(children: const [EmptyState(message: 'Nenhum orçamento criado ainda.')]);
            return ListView.separated(
              padding: const EdgeInsets.all(16),
              itemCount: p.content.length,
              separatorBuilder: (_, _) => const SizedBox(height: 8),
              itemBuilder: (context, index) {
                final o = p.content[index];
                return Card(
                  child: ListTile(
                    onTap: () => context.push('/orcamentos/${o.id}'),
                    title: Text(o.fornecedorNome, style: const TextStyle(fontWeight: FontWeight.w700)),
                    subtitle: Text(o.sourcePurchaseRequestName ?? 'Orçamento #${o.id.substring(0, 8)}'),
                    trailing: StatusBadge(kind: StatusBadgeKind.orcamento, status: o.status),
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
