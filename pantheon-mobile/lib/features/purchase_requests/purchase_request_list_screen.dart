import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';

import '../../core/widgets/async_value_view.dart';
import '../../core/widgets/offline_dialogs.dart';
import '../../core/widgets/status_badge.dart';
import '../../theme/app_colors.dart';
import '../site/site_repository.dart';
import 'purchase_request_models.dart';
import 'purchase_request_repository.dart';

final _purchaseRequestListProvider =
    FutureProvider.family((ref, String siteId) => ref.watch(purchaseRequestRepositoryProvider).list(siteId, size: 50));

/// Mirrors `pantheon-web`'s `PurchaseRequestPanel.vue` `canManage` prop
/// (`myPermissions?.PURCHASE_REQUEST === 'MANAGE'`), which gates the "Novo pedido" button.
final _canManageProvider = FutureProvider.family(
  (ref, String siteId) => ref.watch(siteRepositoryProvider).getMyPermissions(siteId).then((p) => p['PURCHASE_REQUEST'] == 'MANAGE'),
);

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

  Future<void> _openCreateSheet(BuildContext context, WidgetRef ref) async {
    // null = the sheet was dismissed without creating anything; true/false (created, either sent
    // live or queued offline — see ApiClient.mutateQueueable) both mean the list should refresh.
    final sentLive = await showModalBottomSheet<bool>(
      context: context,
      isScrollControlled: true,
      builder: (context) => _CreatePurchaseRequestSheet(siteId: siteId),
    );
    if (sentLive == null) return;
    ref.invalidate(_purchaseRequestListProvider(siteId));
    if (!sentLive && context.mounted) {
      await showOfflineSavedDialog(context);
    }
  }

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final page = ref.watch(_purchaseRequestListProvider(siteId));
    final canManage = ref.watch(_canManageProvider(siteId));
    return Scaffold(
      appBar: AppBar(
        title: const Text('Pedido de Compra'),
        actions: [
          canManage.when(
            data: (allowed) => allowed
                ? IconButton(icon: const Icon(Icons.add), onPressed: () => _openCreateSheet(context, ref))
                : const SizedBox.shrink(),
            loading: () => const SizedBox.shrink(),
            error: (_, _) => const SizedBox.shrink(),
          ),
        ],
      ),
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

class _ItemRow {
  final nameController = TextEditingController();
  final typeController = TextEditingController();
  final quantityController = TextEditingController();
  final unitController = TextEditingController();
}

class _CreatePurchaseRequestSheet extends ConsumerStatefulWidget {
  const _CreatePurchaseRequestSheet({required this.siteId});
  final String siteId;

  @override
  ConsumerState<_CreatePurchaseRequestSheet> createState() => _CreatePurchaseRequestSheetState();
}

class _CreatePurchaseRequestSheetState extends ConsumerState<_CreatePurchaseRequestSheet> {
  final List<_ItemRow> _rows = [_ItemRow()];
  bool _submitting = false;
  String? _error;

  void _addRow() => setState(() => _rows.add(_ItemRow()));

  void _removeRow(int index) => setState(() => _rows.removeAt(index));

  Future<void> _submit() async {
    final items = _rows
        .where((r) => r.nameController.text.trim().isNotEmpty && r.quantityController.text.trim().isNotEmpty)
        .map((r) => {
              'name': r.nameController.text.trim(),
              'type': r.typeController.text.trim().isEmpty ? null : r.typeController.text.trim(),
              'quantity': r.quantityController.text.trim(),
              'unit': r.unitController.text.trim().isEmpty ? null : r.unitController.text.trim(),
            })
        .toList();
    if (items.isEmpty) {
      setState(() => _error = 'Informe ao menos um item com nome e quantidade.');
      return;
    }
    if (!await confirmProceedOffline(context, ref)) return;
    setState(() {
      _submitting = true;
      _error = null;
    });
    try {
      final sentLive = await ref.read(purchaseRequestRepositoryProvider).create(widget.siteId, items);
      if (mounted) Navigator.pop(context, sentLive);
    } catch (_) {
      setState(() => _error = 'Não foi possível criar o pedido. Verifique os dados.');
    } finally {
      if (mounted) setState(() => _submitting = false);
    }
  }

  @override
  Widget build(BuildContext context) {
    return Padding(
      padding: EdgeInsets.only(
        left: 16,
        right: 16,
        top: 16,
        bottom: MediaQuery.of(context).viewInsets.bottom + 16,
      ),
      child: SingleChildScrollView(
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.stretch,
          mainAxisSize: MainAxisSize.min,
          children: [
            const Text('Novo pedido de compra', style: TextStyle(fontSize: 17, fontWeight: FontWeight.w800)),
            const SizedBox(height: 16),
            for (var i = 0; i < _rows.length; i++) ...[
              Row(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  Expanded(
                    child: Column(
                      children: [
                        TextField(
                          controller: _rows[i].nameController,
                          decoration: const InputDecoration(labelText: 'Nome do item'),
                        ),
                        const SizedBox(height: 8),
                        Row(
                          children: [
                            Expanded(
                              child: TextField(
                                controller: _rows[i].quantityController,
                                keyboardType: const TextInputType.numberWithOptions(decimal: true),
                                decoration: const InputDecoration(labelText: 'Quantidade'),
                              ),
                            ),
                            const SizedBox(width: 8),
                            Expanded(
                              child: TextField(
                                controller: _rows[i].unitController,
                                decoration: const InputDecoration(labelText: 'Unidade'),
                              ),
                            ),
                          ],
                        ),
                      ],
                    ),
                  ),
                  if (_rows.length > 1)
                    IconButton(icon: const Icon(Icons.close), onPressed: () => _removeRow(i)),
                ],
              ),
              const SizedBox(height: 12),
            ],
            TextButton.icon(onPressed: _addRow, icon: const Icon(Icons.add), label: const Text('Adicionar item')),
            if (_error != null) ...[
              const SizedBox(height: 8),
              Text(_error!, style: const TextStyle(color: AppColors.safety600, fontSize: 13)),
            ],
            const SizedBox(height: 16),
            ElevatedButton(
              onPressed: _submitting ? null : _submit,
              child: _submitting
                  ? const SizedBox(height: 18, width: 18, child: CircularProgressIndicator(strokeWidth: 2, color: Colors.white))
                  : const Text('Criar pedido'),
            ),
          ],
        ),
      ),
    );
  }
}
