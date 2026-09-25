import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:intl/intl.dart';

import '../../core/widgets/async_value_view.dart';
import '../../core/widgets/buttons.dart';
import '../../core/widgets/offline_dialogs.dart';
import '../../core/widgets/status_badge.dart';
import '../../theme/app_colors.dart';
import '../site/site_repository.dart';
import 'purchase_request_list_screen.dart' show purchaseRequestListProvider;
import 'purchase_request_models.dart';
import 'purchase_request_repository.dart';

final _detailProvider =
    FutureProvider.family((ref, String id) => ref.watch(purchaseRequestRepositoryProvider).getDetail(id));
final _comparisonProvider =
    FutureProvider.family((ref, String id) => ref.watch(purchaseRequestRepositoryProvider).getComparison(id));

final _dateFormat = DateFormat('dd/MM/yyyy');

class _Authority {
  const _Authority(this.myFunction, this.accessLevel);
  final String? myFunction;
  final String? accessLevel;

  /// Mirrors `pantheon-web`'s `canApprove` computed / the backend's `SitePermissionService.canApprove`.
  bool get canApprove => accessLevel == 'MANAGE' || accessLevel == 'VIEW_AND_APPROVE';

  bool get canManage => accessLevel == 'MANAGE';

  /// Mirrors `pantheon-web`'s `canActOnApproval` / the backend's `requireStepAuthority`: a member
  /// holding a `SiteMembership` on this site (even one who is also company staff) may act only
  /// when their function matches the pending step; a company-staff user with NO `SiteMembership`
  /// here keeps the admin bypass, but that bypass requires `MANAGE` specifically — `VIEW_AND_APPROVE`
  /// alone is not enough without a matching function, since there's no function to match.
  bool canActOn(String approverFunction) {
    if (myFunction != null) return myFunction == approverFunction && canApprove;
    return canManage;
  }
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
  String? _removingItemId;
  bool _deleting = false;

  /// The approval workflow (submit/approve/reject/conclude) requires being online — see
  /// `PurchaseRequestRepository`'s doc comment — so this blocks with an explanatory dialog
  /// instead of attempting anything while offline.
  Future<void> _act(Future<void> Function() action) async {
    if (!await requireOnline(context, ref)) return;
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

  Future<void> _submit() async {
    await _act(() => ref.read(purchaseRequestRepositoryProvider).submitForApproval(widget.id));
  }

  Future<void> _conclude() async {
    await _act(() => ref.read(purchaseRequestRepositoryProvider).conclude(widget.id));
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

  Future<void> _openAddItemsSheet(String siteId) async {
    final added = await showModalBottomSheet<bool>(
      context: context,
      isScrollControlled: true,
      builder: (context) => _AddItemsSheet(purchaseRequestId: widget.id),
    );
    if (added == true) ref.invalidate(_detailProvider(widget.id));
  }

  Future<void> _removeItem(PurchaseRequestItem item) async {
    final confirmed = await showDialog<bool>(
      context: context,
      builder: (context) => AlertDialog(
        title: const Text('Remover item'),
        content: Text('Remover "${item.name}" deste pedido de compra?'),
        actions: [
          TextButton(onPressed: () => Navigator.pop(context, false), child: const Text('Cancelar')),
          TextButton(onPressed: () => Navigator.pop(context, true), child: const Text('Remover')),
        ],
      ),
    );
    if (confirmed != true) return;
    if (!mounted || !await requireOnline(context, ref)) return;
    setState(() => _removingItemId = item.id);
    try {
      await ref.read(purchaseRequestRepositoryProvider).removeItem(widget.id, item.id);
      ref.invalidate(_detailProvider(widget.id));
    } catch (_) {
      if (mounted) {
        ScaffoldMessenger.of(context).showSnackBar(const SnackBar(content: Text('Não foi possível remover o item.')));
      }
    } finally {
      if (mounted) setState(() => _removingItemId = null);
    }
  }

  Future<void> _deletePurchaseRequest(String siteId) async {
    final confirmed = await showDialog<bool>(
      context: context,
      builder: (context) => AlertDialog(
        title: const Text('Excluir pedido de compra'),
        content: const Text(
          'Esta ação também exclui os orçamentos vinculados a este pedido. Não pode ser desfeita.',
        ),
        actions: [
          TextButton(onPressed: () => Navigator.pop(context, false), child: const Text('Cancelar')),
          TextButton(onPressed: () => Navigator.pop(context, true), child: const Text('Excluir')),
        ],
      ),
    );
    if (confirmed != true) return;
    if (!mounted || !await requireOnline(context, ref)) return;
    setState(() => _deleting = true);
    try {
      await ref.read(purchaseRequestRepositoryProvider).delete(widget.id);
      ref.invalidate(purchaseRequestListProvider(siteId));
      if (mounted) Navigator.of(context).pop();
    } catch (_) {
      if (mounted) {
        ScaffoldMessenger.of(context).showSnackBar(const SnackBar(content: Text('Não foi possível excluir o pedido.')));
        setState(() => _deleting = false);
      }
    }
  }

  @override
  Widget build(BuildContext context) {
    final detail = ref.watch(_detailProvider(widget.id));
    return Scaffold(
      appBar: AppBar(
        title: const Text('Pedido de Compra'),
        actions: [
          detail.maybeWhen(
            data: (d) {
              if (d.purchaseRequest.status == 'CONCLUIDO') return const SizedBox.shrink();
              return IconButton(
                icon: _deleting
                    ? const SizedBox(height: 18, width: 18, child: CircularProgressIndicator(strokeWidth: 2))
                    : const Icon(Icons.delete_outline),
                tooltip: 'Excluir pedido',
                onPressed: _deleting ? null : () => _deletePurchaseRequest(d.purchaseRequest.constructionSiteId),
              );
            },
            orElse: () => const SizedBox.shrink(),
          ),
        ],
      ),
      body: AsyncValueView(
        value: detail,
        data: (d) {
          final authority = ref.watch(_authorityProvider(d.purchaseRequest.constructionSiteId));

          // Mirrors pantheon-web's `currentCycle`/`currentPendingApproval` computeds: all of a
          // cycle's steps are created PENDING up front on submit, so more than one can be PENDING
          // at once (e.g. steps 2 and 3 while step 1 hasn't been decided yet) — the actionable one
          // is the *lowest step order* within the *current* (highest) cycle, not just "any pending".
          final currentCycle = d.approvals.isEmpty ? 0 : d.approvals.map((a) => a.cycleNumber).reduce((a, b) => a > b ? a : b);
          final currentCyclePending = d.approvals.where((a) => a.cycleNumber == currentCycle && a.status == 'PENDING').toList()
            ..sort((a, b) => a.stepOrder.compareTo(b.stepOrder));
          final currentPending = currentCyclePending.isEmpty ? null : currentCyclePending.first;

          return RefreshIndicator(
            onRefresh: () => ref.refresh(_detailProvider(widget.id).future),
            child: ListView(
              padding: const EdgeInsets.fromLTRB(16, 16, 16, 32),
              children: [
                // Header card
                Container(
                  padding: const EdgeInsets.all(18),
                  decoration: BoxDecoration(
                    color: Colors.white,
                    borderRadius: BorderRadius.circular(18),
                    boxShadow: [BoxShadow(color: Colors.black.withValues(alpha: 0.04), blurRadius: 10, offset: const Offset(0, 3))],
                  ),
                  child: Column(
                    crossAxisAlignment: CrossAxisAlignment.start,
                    children: [
                      Row(
                        crossAxisAlignment: CrossAxisAlignment.start,
                        children: [
                          Expanded(
                            child: Text(d.purchaseRequest.name, style: const TextStyle(fontSize: 19, fontWeight: FontWeight.w800)),
                          ),
                          const SizedBox(width: 8),
                          StatusBadge(kind: StatusBadgeKind.purchaseRequest, status: d.purchaseRequest.status),
                        ],
                      ),
                      const SizedBox(height: 10),
                      Row(
                        children: [
                          const Icon(Icons.person_outline, size: 14, color: AppColors.steel400),
                          const SizedBox(width: 4),
                          Text(
                            d.purchaseRequest.createdByName ?? 'Criado por alguém da equipe',
                            style: const TextStyle(fontSize: 12.5, color: AppColors.steel500),
                          ),
                          const SizedBox(width: 12),
                          const Icon(Icons.calendar_today_outlined, size: 12, color: AppColors.steel400),
                          const SizedBox(width: 4),
                          Text(
                            _dateFormat.format(DateTime.parse(d.purchaseRequest.createdAt).toLocal()),
                            style: const TextStyle(fontSize: 12.5, color: AppColors.steel500),
                          ),
                        ],
                      ),
                      if (d.purchaseRequest.lastRejectionReason != null && d.purchaseRequest.status == 'ORCADO') ...[
                        const SizedBox(height: 10),
                        Container(
                          padding: const EdgeInsets.all(10),
                          decoration: BoxDecoration(color: AppColors.safety50, borderRadius: BorderRadius.circular(10)),
                          child: Row(
                            crossAxisAlignment: CrossAxisAlignment.start,
                            children: [
                              const Icon(Icons.info_outline, size: 16, color: AppColors.safety600),
                              const SizedBox(width: 8),
                              Expanded(
                                child: Text(
                                  d.purchaseRequest.lastRejectionReason!,
                                  style: const TextStyle(fontSize: 12.5, color: AppColors.safety600),
                                ),
                              ),
                            ],
                          ),
                        ),
                      ],
                      authority.when(
                        data: (auth) {
                          // Mirrors pantheon-web's `canSubmit`: MANAGE only, status ORCADO, at
                          // least one item, and every item has a selected Orçamento line item.
                          final canSubmit = auth.canManage &&
                              d.purchaseRequest.status == 'ORCADO' &&
                              d.items.isNotEmpty &&
                              d.items.every((item) => item.selectedOrcamentoLineItemId != null);
                          // Mirrors pantheon-web's `canConclude`: any approve-capable member,
                          // regardless of function — status CONFERIDO only.
                          final canConclude = d.purchaseRequest.status == 'CONFERIDO' && auth.canApprove;
                          if (!canSubmit && !canConclude) return const SizedBox.shrink();
                          return Padding(
                            padding: const EdgeInsets.only(top: 14),
                            child: SizedBox(
                              width: double.infinity,
                              child: canSubmit
                                  ? ElevatedButton.icon(
                                      onPressed: _acting ? null : _submit,
                                      icon: const Icon(Icons.send_outlined, size: 18),
                                      label: const Text('Enviar para aprovação'),
                                    )
                                  : SuccessButton(label: 'Concluir', loading: _acting, onPressed: _conclude, icon: Icons.task_alt),
                            ),
                          );
                        },
                        loading: () => const SizedBox.shrink(),
                        error: (_, _) => const SizedBox.shrink(),
                      ),
                    ],
                  ),
                ),

                if (d.approvals.isNotEmpty) ...[
                  const SizedBox(height: 16),
                  _SectionCard(
                    icon: Icons.rule_folder_outlined,
                    title: 'Etapas de aprovação',
                    child: Column(
                      children: [
                        ...d.approvals.map(
                          (a) => Padding(
                            padding: const EdgeInsets.only(bottom: 8),
                            child: Row(
                              children: [
                                Expanded(
                                  child: Text(
                                    _approverFunctionLabels[a.approverFunction] ?? a.approverFunction,
                                    style: const TextStyle(fontWeight: FontWeight.w600, fontSize: 13.5),
                                  ),
                                ),
                                StatusBadge(kind: StatusBadgeKind.approval, status: a.status),
                              ],
                            ),
                          ),
                        ),
                        if (currentPending != null)
                          authority.when(
                            data: (auth) {
                              if (!auth.canActOn(currentPending.approverFunction)) return const SizedBox.shrink();
                              return Padding(
                                padding: const EdgeInsets.only(top: 4),
                                child: Row(
                                  children: [
                                    Expanded(
                                      child: SuccessButton(
                                        label: 'Aprovar',
                                        loading: _acting,
                                        onPressed: () =>
                                            _act(() => ref.read(purchaseRequestRepositoryProvider).approveStep(widget.id)),
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
                      ],
                    ),
                  ),
                ],

                const SizedBox(height: 16),
                authority.when(
                  data: (auth) {
                    final canAddItems = auth.canManage && d.purchaseRequest.status == 'INICIADO';
                    return _SectionCard(
                      icon: Icons.inventory_2_outlined,
                      title: 'Itens',
                      trailing: canAddItems
                          ? TextButton.icon(
                              onPressed: () => _openAddItemsSheet(d.purchaseRequest.constructionSiteId),
                              icon: const Icon(Icons.add, size: 18),
                              label: const Text('Adicionar'),
                            )
                          : null,
                      child: d.items.isEmpty
                          ? const Padding(
                              padding: EdgeInsets.symmetric(vertical: 12),
                              child: Text('Nenhum item adicionado ainda.', style: TextStyle(color: AppColors.steel500)),
                            )
                          : Column(
                              children: d.items
                                  .map(
                                    (item) => Padding(
                                      padding: const EdgeInsets.only(bottom: 10),
                                      child: Row(
                                        crossAxisAlignment: CrossAxisAlignment.start,
                                        children: [
                                          Expanded(
                                            child: Column(
                                              crossAxisAlignment: CrossAxisAlignment.start,
                                              children: [
                                                Text(item.name, style: const TextStyle(fontWeight: FontWeight.w700, fontSize: 13.5)),
                                                const SizedBox(height: 2),
                                                Text(
                                                  '${item.quantity} ${item.unit ?? ''}'.trim() +
                                                      (item.type != null ? ' · ${item.type}' : ''),
                                                  style: const TextStyle(fontSize: 12, color: AppColors.steel500),
                                                ),
                                              ],
                                            ),
                                          ),
                                          const SizedBox(width: 8),
                                          StatusBadge(kind: StatusBadgeKind.purchaseRequestItem, status: item.status),
                                          if (canAddItems) ...[
                                            const SizedBox(width: 4),
                                            SizedBox(
                                              height: 32,
                                              width: 32,
                                              child: _removingItemId == item.id
                                                  ? const Padding(
                                                      padding: EdgeInsets.all(6),
                                                      child: CircularProgressIndicator(strokeWidth: 2),
                                                    )
                                                  : IconButton(
                                                      padding: EdgeInsets.zero,
                                                      icon: const Icon(Icons.delete_outline, size: 18, color: AppColors.safety600),
                                                      onPressed: () => _removeItem(item),
                                                    ),
                                            ),
                                          ],
                                        ],
                                      ),
                                    ),
                                  )
                                  .toList(),
                            ),
                    );
                  },
                  loading: () => _SectionCard(
                    icon: Icons.inventory_2_outlined,
                    title: 'Itens',
                    child: Column(children: d.items.map((item) => ListTile(dense: true, title: Text(item.name))).toList()),
                  ),
                  error: (_, _) => _SectionCard(
                    icon: Icons.inventory_2_outlined,
                    title: 'Itens',
                    child: Column(children: d.items.map((item) => ListTile(dense: true, title: Text(item.name))).toList()),
                  ),
                ),

                if (d.purchaseRequest.linkedOrcamentos.isNotEmpty) ...[
                  const SizedBox(height: 16),
                  _SectionCard(
                    icon: Icons.storefront_outlined,
                    title: 'Fornecedores',
                    child: Column(
                      children: d.purchaseRequest.linkedOrcamentos
                          .map(
                            (o) => Padding(
                              padding: const EdgeInsets.only(bottom: 6),
                              child: Text(o.fornecedorNome, style: const TextStyle(fontWeight: FontWeight.w600, fontSize: 13.5)),
                            ),
                          )
                          .toList(),
                    ),
                  ),
                  const SizedBox(height: 16),
                  _SectionCard(
                    icon: Icons.compare_arrows_outlined,
                    title: 'Comparativo de fornecedores',
                    child: Consumer(
                      builder: (context, ref, _) => AsyncValueView(
                        value: ref.watch(_comparisonProvider(widget.id)),
                        data: (comparison) => _ComparisonTable(comparison: comparison),
                      ),
                    ),
                  ),
                ],
              ],
            ),
          );
        },
      ),
    );
  }
}

class _SectionCard extends StatelessWidget {
  const _SectionCard({required this.icon, required this.title, required this.child, this.trailing});
  final IconData icon;
  final String title;
  final Widget child;
  final Widget? trailing;

  @override
  Widget build(BuildContext context) {
    return Container(
      padding: const EdgeInsets.all(16),
      decoration: BoxDecoration(
        color: Colors.white,
        borderRadius: BorderRadius.circular(16),
        boxShadow: [BoxShadow(color: Colors.black.withValues(alpha: 0.03), blurRadius: 8, offset: const Offset(0, 2))],
      ),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Row(
            children: [
              Icon(icon, size: 17, color: AppColors.blueprint600),
              const SizedBox(width: 8),
              Expanded(
                child: Text(
                  title,
                  style: const TextStyle(fontWeight: FontWeight.w700, fontSize: 13, color: AppColors.steel700),
                ),
              ),
              ?trailing,
            ],
          ),
          const SizedBox(height: 12),
          child,
        ],
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

class _AddItemRow {
  final nameController = TextEditingController();
  final typeController = TextEditingController();
  final quantityController = TextEditingController();
  final unitController = TextEditingController();
}

/// Mirrors `pantheon-web`'s add-items modal on `PurchaseRequestDetailView.vue`: name, quantity
/// and unit are all mandatory now (backend rejects with 400 otherwise — see
/// `PurchaseRequestItemCreationRequest`'s `@NotBlank`/`@NotNull` annotations).
class _AddItemsSheet extends ConsumerStatefulWidget {
  const _AddItemsSheet({required this.purchaseRequestId});
  final String purchaseRequestId;

  @override
  ConsumerState<_AddItemsSheet> createState() => _AddItemsSheetState();
}

class _AddItemsSheetState extends ConsumerState<_AddItemsSheet> {
  final List<_AddItemRow> _rows = [_AddItemRow()];
  bool _submitting = false;
  String? _error;

  void _addRow() => setState(() => _rows.add(_AddItemRow()));

  void _removeRow(int index) => setState(() => _rows.removeAt(index));

  Future<void> _submit() async {
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
    setState(() {
      _submitting = true;
      _error = null;
    });
    try {
      await ref.read(purchaseRequestRepositoryProvider).addItems(widget.purchaseRequestId, items);
      if (mounted) Navigator.pop(context, true);
    } catch (_) {
      setState(() => _error = 'Não foi possível adicionar os itens. Verifique os dados.');
    } finally {
      if (mounted) setState(() => _submitting = false);
    }
  }

  @override
  Widget build(BuildContext context) {
    return Padding(
      padding: EdgeInsets.only(left: 16, right: 16, top: 16, bottom: MediaQuery.of(context).viewInsets.bottom + 16),
      child: SingleChildScrollView(
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.stretch,
          mainAxisSize: MainAxisSize.min,
          children: [
            const Text('Adicionar produtos', style: TextStyle(fontSize: 17, fontWeight: FontWeight.w800)),
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
                    TextField(
                      controller: _rows[i].typeController,
                      decoration: const InputDecoration(labelText: 'Tipo'),
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
            TextButton.icon(onPressed: _addRow, icon: const Icon(Icons.add), label: const Text('Adicionar outro produto')),
            if (_error != null) ...[
              const SizedBox(height: 8),
              Text(_error!, style: const TextStyle(color: AppColors.safety600, fontSize: 13)),
            ],
            const SizedBox(height: 16),
            ElevatedButton(
              onPressed: _submitting ? null : _submit,
              child: _submitting
                  ? const SizedBox(height: 18, width: 18, child: CircularProgressIndicator(strokeWidth: 2, color: Colors.white))
                  : const Text('Adicionar'),
            ),
          ],
        ),
      ),
    );
  }
}
