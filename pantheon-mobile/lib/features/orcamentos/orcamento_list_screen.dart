import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';

import '../../core/widgets/async_value_view.dart';
import '../../core/widgets/status_badge.dart';
import '../../theme/app_colors.dart';
import 'orcamento_repository.dart';

final _listProvider = FutureProvider.family((ref, String siteId) => ref.watch(orcamentoRepositoryProvider).list(siteId, size: 50));

class OrcamentoListScreen extends ConsumerWidget {
  const OrcamentoListScreen({super.key, required this.siteId});
  final String siteId;

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final page = ref.watch(_listProvider(siteId));
    return Scaffold(
      backgroundColor: AppColors.steel50,
      appBar: AppBar(title: const Text('Orçamentos')),
      body: RefreshIndicator(
        onRefresh: () => ref.refresh(_listProvider(siteId).future),
        child: AsyncValueView(
          value: page,
          data: (p) {
            if (p.content.isEmpty) {
              return ListView(
                children: const [EmptyState(icon: Icons.attach_money, message: 'Nenhum orçamento criado ainda.')],
              );
            }
            return ListView.separated(
              padding: const EdgeInsets.all(16),
              itemCount: p.content.length,
              separatorBuilder: (_, _) => const SizedBox(height: 10),
              itemBuilder: (context, index) {
                final o = p.content[index];
                return Material(
                  color: Colors.white,
                  borderRadius: BorderRadius.circular(16),
                  child: InkWell(
                    borderRadius: BorderRadius.circular(16),
                    onTap: () => context.push('/orcamentos/${o.id}'),
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
                            child: const Icon(Icons.storefront_outlined, color: AppColors.blueprint600, size: 20),
                          ),
                          const SizedBox(width: 12),
                          Expanded(
                            child: Column(
                              crossAxisAlignment: CrossAxisAlignment.start,
                              children: [
                                Text(o.fornecedorNome, style: const TextStyle(fontWeight: FontWeight.w700, fontSize: 14.5)),
                                const SizedBox(height: 3),
                                Text(
                                  o.sourcePurchaseRequestName ?? 'Orçamento #${o.id.substring(0, 8)}',
                                  maxLines: 1,
                                  overflow: TextOverflow.ellipsis,
                                  style: const TextStyle(fontSize: 12.5, color: AppColors.steel500),
                                ),
                                if (o.createdByName != null) ...[
                                  const SizedBox(height: 2),
                                  Text(
                                    'Criado por ${o.createdByName}',
                                    style: const TextStyle(fontSize: 11, color: AppColors.steel400),
                                  ),
                                ],
                              ],
                            ),
                          ),
                          const SizedBox(width: 8),
                          StatusBadge(kind: StatusBadgeKind.orcamento, status: o.status),
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
