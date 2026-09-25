import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';
import 'package:intl/intl.dart';

import '../../core/widgets/async_value_view.dart';
import '../../core/widgets/offline_dialogs.dart';
import '../../core/widgets/status_badge.dart';
import '../../theme/app_colors.dart';
import '../site/site_repository.dart';
import 'purchase_request_models.dart';
import 'purchase_request_repository.dart';

/// Public (not `daily_report_list_screen.dart`-private) because the detail screen invalidates it
/// after a delete, so the list reflects the removal on the way back.
final purchaseRequestListProvider =
    FutureProvider.family((ref, String siteId) => ref.watch(purchaseRequestRepositoryProvider).list(siteId, size: 50));

/// Mirrors `pantheon-web`'s `PurchaseRequestPanel.vue` `canManage` prop
/// (`myPermissions?.PURCHASE_REQUEST === 'MANAGE'`), which gates the "Novo pedido" button.
final _canManageProvider = FutureProvider.family(
  (ref, String siteId) => ref.watch(siteRepositoryProvider).getMyPermissions(siteId).then((p) => p['PURCHASE_REQUEST'] == 'MANAGE'),
);

final _dateFormat = DateFormat('dd/MM/yyyy');

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

IconData _stageIcon(PurchaseRequest pr) {
  switch (pr.status) {
    case 'INICIADO':
      return Icons.edit_note_outlined;
    case 'ORCADO':
      return Icons.hourglass_top_outlined;
    case 'CONFERIDO':
      return Icons.verified_outlined;
    default:
      return Icons.task_alt;
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
    ref.invalidate(purchaseRequestListProvider(siteId));
    if (!sentLive && context.mounted) {
      await showOfflineSavedDialog(context);
    }
  }

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final page = ref.watch(purchaseRequestListProvider(siteId));
    final canManage = ref.watch(_canManageProvider(siteId));
    return Scaffold(
      backgroundColor: AppColors.steel50,
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
      floatingActionButton: canManage.maybeWhen(
        data: (allowed) => allowed
            ? FloatingActionButton.extended(
                onPressed: () => _openCreateSheet(context, ref),
                icon: const Icon(Icons.add),
                label: const Text('Novo pedido'),
              )
            : null,
        orElse: () => null,
      ),
      body: RefreshIndicator(
        onRefresh: () => ref.refresh(purchaseRequestListProvider(siteId).future),
        child: AsyncValueView(
          value: page,
          data: (p) {
            if (p.content.isEmpty) {
              return ListView(
                children: const [
                  EmptyState(
                    icon: Icons.shopping_cart_outlined,
                    message: 'Nenhum pedido de compra criado ainda.\nToque em "Novo pedido" para começar.',
                  ),
                ],
              );
            }
            return ListView.separated(
              padding: const EdgeInsets.fromLTRB(16, 16, 16, 96),
              itemCount: p.content.length,
              separatorBuilder: (_, _) => const SizedBox(height: 10),
              itemBuilder: (context, index) {
                final pr = p.content[index];
                final suppliers = pr.linkedOrcamentos.map((o) => o.fornecedorNome).join(', ');
                return _PurchaseRequestCard(purchaseRequest: pr, suppliers: suppliers);
              },
            );
          },
        ),
      ),
    );
  }
}

class _PurchaseRequestCard extends StatelessWidget {
  const _PurchaseRequestCard({required this.purchaseRequest, required this.suppliers});
  final PurchaseRequest purchaseRequest;
  final String suppliers;

  @override
  Widget build(BuildContext context) {
    final pr = purchaseRequest;
    return Material(
      color: Colors.white,
      borderRadius: BorderRadius.circular(16),
      elevation: 0,
      child: InkWell(
        borderRadius: BorderRadius.circular(16),
        onTap: () => context.push('/purchase-requests/${pr.id}'),
        child: Container(
          padding: const EdgeInsets.all(14),
          decoration: BoxDecoration(
            borderRadius: BorderRadius.circular(16),
            border: Border.all(color: AppColors.steel200),
          ),
          child: Row(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              Container(
                height: 42,
                width: 42,
                decoration: BoxDecoration(color: AppColors.blueprint50, borderRadius: BorderRadius.circular(12)),
                alignment: Alignment.center,
                child: Icon(_stageIcon(pr), color: AppColors.blueprint600, size: 20),
              ),
              const SizedBox(width: 12),
              Expanded(
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    Text(pr.name, style: const TextStyle(fontWeight: FontWeight.w700, fontSize: 14.5)),
                    const SizedBox(height: 3),
                    Text(_stageLabel(pr), style: const TextStyle(fontSize: 12.5, color: AppColors.steel500)),
                    if (suppliers.isNotEmpty) ...[
                      const SizedBox(height: 3),
                      Text(
                        suppliers,
                        maxLines: 1,
                        overflow: TextOverflow.ellipsis,
                        style: const TextStyle(fontSize: 11.5, color: AppColors.steel400),
                      ),
                    ],
                  ],
                ),
              ),
              const SizedBox(width: 8),
              Column(
                crossAxisAlignment: CrossAxisAlignment.end,
                children: [
                  StatusBadge(kind: StatusBadgeKind.purchaseRequest, status: pr.status),
                  const SizedBox(height: 6),
                  Text(_dateFormat.format(DateTime.parse(pr.createdAt).toLocal()), style: const TextStyle(fontSize: 10.5, color: AppColors.steel400)),
                ],
              ),
            ],
          ),
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
    // Mirrors the backend's PurchaseRequestItemCreationRequest validation: name, quantity, and
    // unit are all mandatory now (a 400 otherwise) — every filled-in row must have all three.
    final incomplete = _rows.any(
      (r) =>
          r.nameController.text.trim().isEmpty ||
          r.quantityController.text.trim().isEmpty ||
          r.unitController.text.trim().isEmpty,
    );
    if (incomplete) {
      setState(() => _error = 'Preencha nome, quantidade e unidade de todos os itens.');
      return;
    }
    final items = _rows
        .map((r) => {
              'name': r.nameController.text.trim(),
              'type': r.typeController.text.trim().isEmpty ? null : r.typeController.text.trim(),
              'quantity': r.quantityController.text.trim(),
              'unit': r.unitController.text.trim(),
            })
        .toList();
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
              Container(
                padding: const EdgeInsets.all(12),
                decoration: BoxDecoration(
                  color: AppColors.steel50,
                  borderRadius: BorderRadius.circular(12),
                  border: Border.all(color: AppColors.steel200),
                ),
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.stretch,
                  children: [
                    Row(
                      crossAxisAlignment: CrossAxisAlignment.start,
                      children: [
                        Expanded(
                          child: Text('Produto ${i + 1}', style: const TextStyle(fontWeight: FontWeight.w700, fontSize: 12, color: AppColors.steel500)),
                        ),
                        if (_rows.length > 1)
                          IconButton(
                            padding: EdgeInsets.zero,
                            constraints: const BoxConstraints(),
                            icon: const Icon(Icons.close, size: 18),
                            onPressed: () => _removeRow(i),
                          ),
                      ],
                    ),
                    const SizedBox(height: 6),
                    TextField(
                      controller: _rows[i].nameController,
                      decoration: const InputDecoration(labelText: 'Nome do item *'),
                    ),
                    const SizedBox(height: 8),
                    Row(
                      children: [
                        Expanded(
                          child: TextField(
                            controller: _rows[i].quantityController,
                            keyboardType: const TextInputType.numberWithOptions(decimal: true),
                            decoration: const InputDecoration(labelText: 'Quantidade *'),
                          ),
                        ),
                        const SizedBox(width: 8),
                        Expanded(
                          child: TextField(
                            controller: _rows[i].unitController,
                            decoration: const InputDecoration(labelText: 'Unidade *'),
                          ),
                        ),
                      ],
                    ),
                  ],
                ),
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
