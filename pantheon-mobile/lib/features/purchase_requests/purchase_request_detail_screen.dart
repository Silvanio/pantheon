import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../core/widgets/async_value_view.dart';
import '../../core/widgets/buttons.dart';
import '../../core/widgets/status_badge.dart';
import '../../theme/app_colors.dart';
import '../site/site_repository.dart';
import 'purchase_request_models.dart';
import 'purchase_request_repository.dart';

final _detailProvider =
    FutureProvider.family((ref, String id) => ref.watch(purchaseRequestRepositoryProvider).getDetail(id));
final _comparisonProvider =
    FutureProvider.family((ref, String id) => ref.watch(purchaseRequestRepositoryProvider).getComparison(id));

class _Authority {
  const _Authority(this.myFunction, this.accessLevel);
  final String? myFunction;
  final String? accessLevel;

  bool get canApprove => accessLevel == 'MANAGE' || accessLevel == 'VIEW_AND_APPROVE';
}

final _authorityProvider = FutureProvider.family((ref, String siteId) async {
  final repo = ref.watch(siteRepositoryProvider);
  final results = await Future.wait([repo.getMyFunction(siteId), repo.getMyPermissions(siteId)]);
  final myFunction = results[0] as String?;
  final perms = results[1] as Map<String, String>;
  return _Authority(myFunction, perms['PURCHASE_REQUEST']);
});

const _approverFunctionLabels = {
  'ADMIN': 'Administrador',
  'CLIENT': 'Cliente',
  'ARCHITECT': 'Arquiteto',
  'ENGINEER': 'Engenheiro',
  'SITE_FOREMAN': 'Mestre de obra',
  'SERVICE_PROVIDER': 'Prestador de serviço',
};

class PurchaseRequestDetailScreen extends ConsumerStatefulWidget {
  const PurchaseRequestDetailScreen({super.key, required this.id});
  final String id;

  @override
  ConsumerState<PurchaseRequestDetailScreen> createState() => _PurchaseRequestDetailScreenState();
}

class _PurchaseRequestDetailScreenState extends ConsumerState<PurchaseRequestDetailScreen> {
  bool _acting = false;

  Future<void> _act(Future<void> Function() action) async {
    setState(() => _acting = true);
    try {
      await action();
      ref.invalidate(_detailProvider(widget.id));
    } catch (e) {
      if (mounted) {
        ScaffoldMessenger.of(context).showSnackBar(const SnackBar(content: Text('Não foi possível concluir a ação.')));
      }
    } finally {
      if (mounted) setState(() => _acting = false);
    }
  }

  Future<void> _showRejectDialog() async {
    final controller = TextEditingController();
    final reason = await showDialog<String>(
      context: context,
      builder: (context) => AlertDialog(
        title: const Text('Rejeitar etapa'),
        content: TextField(
          controller: controller,
          decoration: const InputDecoration(labelText: 'Motivo da rejeição'),
          autofocus: true,
        ),
        actions: [
          TextButton(onPressed: () => Navigator.pop(context), child: const Text('Cancelar')),
          TextButton(onPressed: () => Navigator.pop(context, controller.text), child: const Text('Rejeitar')),
        ],
      ),
    );
    if (reason != null && reason.trim().isNotEmpty) {
      final repo = ref.read(purchaseRequestRepositoryProvider);
      await _act(() => repo.rejectStep(widget.id, reason.trim()));
    }
  }

  @override
  Widget build(BuildContext context) {
    final detail = ref.watch(_detailProvider(widget.id));
    return Scaffold(
      appBar: AppBar(title: const Text('Pedido de Compra')),
      body: AsyncValueView(
        value: detail,
        data: (d) {
          final authority = ref.watch(_authorityProvider(d.purchaseRequest.constructionSiteId));
          final pendingApproval = d.approvals.where((a) => a.status == 'PENDING').toList();
          final currentPending = pendingApproval.isEmpty ? null : pendingApproval.last;

          return ListView(
            padding: const EdgeInsets.all(16),
            children: [
              Row(
                children: [
                  Expanded(
                    child: Text(d.purchaseRequest.name, style: const TextStyle(fontSize: 19, fontWeight: FontWeight.w800)),
                  ),
                  StatusBadge(kind: StatusBadgeKind.purchaseRequest, status: d.purchaseRequest.status),
                ],
              ),
              const SizedBox(height: 20),
              const Text('Etapas de aprovação', style: TextStyle(fontWeight: FontWeight.w700, fontSize: 14)),
              const SizedBox(height: 8),
              ...d.approvals.map(
                (a) => Card(
                  margin: const EdgeInsets.only(bottom: 8),
                  child: ListTile(
                    dense: true,
                    title: Text(_approverFunctionLabels[a.approverFunction] ?? a.approverFunction),
                    subtitle: a.comment != null ? Text(a.comment!) : null,
                    trailing: StatusBadge(kind: StatusBadgeKind.approval, status: a.status),
                  ),
                ),
              ),
              if (currentPending != null)
                authority.when(
                  data: (auth) {
                    final canAct = auth.canApprove && (auth.myFunction == null || auth.myFunction == currentPending.approverFunction);
                    if (!canAct) return const SizedBox.shrink();
                    return Padding(
                      padding: const EdgeInsets.only(top: 8, bottom: 8),
                      child: Row(
                        children: [
                          Expanded(
                            child: SuccessButton(
                              label: 'Aprovar',
                              loading: _acting,
                              onPressed: () => _act(() => ref.read(purchaseRequestRepositoryProvider).approveStep(widget.id)),
                            ),
                          ),
                          const SizedBox(width: 10),
                          Expanded(
                            child: DangerButton(label: 'Rejeitar', loading: _acting, onPressed: _showRejectDialog),
                          ),
                        ],
                      ),
                    );
                  },
                  loading: () => const SizedBox.shrink(),
                  error: (_, _) => const SizedBox.shrink(),
                ),
              const SizedBox(height: 12),
              const Text('Itens', style: TextStyle(fontWeight: FontWeight.w700, fontSize: 14)),
              const SizedBox(height: 8),
              ...d.items.map(
                (item) => Card(
                  margin: const EdgeInsets.only(bottom: 8),
                  child: ListTile(
                    dense: true,
                    title: Text(item.name),
                    subtitle: Text('${item.quantity} ${item.unit ?? ''}'.trim()),
                  ),
                ),
              ),
              if (d.purchaseRequest.linkedOrcamentos.isNotEmpty) ...[
                const SizedBox(height: 12),
                const Text('Fornecedores', style: TextStyle(fontWeight: FontWeight.w700, fontSize: 14)),
                const SizedBox(height: 8),
                ...d.purchaseRequest.linkedOrcamentos.map(
                  (o) => Card(
                    margin: const EdgeInsets.only(bottom: 8),
                    child: ListTile(dense: true, leading: const Icon(Icons.storefront_outlined, color: AppColors.steel500), title: Text(o.fornecedorNome)),
                  ),
                ),
                const SizedBox(height: 12),
                const Text('Comparativo de fornecedores', style: TextStyle(fontWeight: FontWeight.w700, fontSize: 14)),
                const SizedBox(height: 8),
                Consumer(
                  builder: (context, ref, _) => AsyncValueView(
                    value: ref.watch(_comparisonProvider(widget.id)),
                    data: (comparison) => _ComparisonTable(comparison: comparison),
                  ),
                ),
              ],
            ],
          );
        },
      ),
    );
  }
}

class _ComparisonTable extends StatelessWidget {
  const _ComparisonTable({required this.comparison});
  final PurchaseRequestComparison comparison;

  @override
  Widget build(BuildContext context) {
    if (comparison.rows.isEmpty || comparison.columns.isEmpty) {
      return const Text('Nenhuma cotação para comparar ainda.', style: TextStyle(color: AppColors.steel500));
    }
    return SingleChildScrollView(
      scrollDirection: Axis.horizontal,
      child: DataTable(
        headingRowHeight: 40,
        dataRowMinHeight: 44,
        dataRowMaxHeight: 56,
        columns: [
          const DataColumn(label: Text('Item', style: TextStyle(fontWeight: FontWeight.w700))),
          for (final column in comparison.columns)
            DataColumn(label: Text(column.supplierName, style: const TextStyle(fontWeight: FontWeight.w700))),
        ],
        rows: [
          for (final row in comparison.rows)
            DataRow(
              cells: [
                DataCell(Text('${row.itemName}\n${row.quantity} ${row.unit ?? ''}'.trim())),
                for (final column in comparison.columns)
                  DataCell(_buildCell(row.cellFor(column.orcamentoId))),
              ],
            ),
        ],
      ),
    );
  }

  Widget _buildCell(ComparisonCell? cell) {
    if (cell == null || cell.unitPrice == null) return const Text('—', style: TextStyle(color: AppColors.steel400));
    return Text(
      'R\$ ${cell.unitPrice}',
      style: TextStyle(
        fontWeight: cell.selected ? FontWeight.w800 : FontWeight.w400,
        color: cell.selected ? AppColors.emerald700 : null,
      ),
    );
  }
}
