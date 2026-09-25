import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:intl/intl.dart';

import '../../core/widgets/async_value_view.dart';
import '../../core/widgets/status_badge.dart';
import '../../theme/app_colors.dart';
import 'orcamento_repository.dart';

final _detailProvider = FutureProvider.family((ref, String id) => ref.watch(orcamentoRepositoryProvider).getDetail(id));

const _paymentLabels = {'CARTAO': 'Cartão', 'BOLETO': 'Boleto', 'PIX': 'Pix', 'DINHEIRO': 'Dinheiro'};

final _dateFormat = DateFormat('dd/MM/yyyy');

class OrcamentoDetailScreen extends ConsumerWidget {
  const OrcamentoDetailScreen({super.key, required this.id});
  final String id;

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final detail = ref.watch(_detailProvider(id));
    return Scaffold(
      backgroundColor: AppColors.steel50,
      appBar: AppBar(title: const Text('Orçamento')),
      body: AsyncValueView(
        value: detail,
        data: (d) {
          final total = d.lineItems
              .where((item) => item.selected && item.unitPrice != null)
              .fold<double>(0, (sum, item) => sum + (double.tryParse(item.unitPrice!) ?? 0) * (double.tryParse(item.quantity) ?? 0));

          return RefreshIndicator(
            onRefresh: () => ref.refresh(_detailProvider(id).future),
            child: ListView(
              padding: const EdgeInsets.all(16),
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
                          Container(
                            height: 44,
                            width: 44,
                            decoration: BoxDecoration(color: AppColors.blueprint50, borderRadius: BorderRadius.circular(12)),
                            alignment: Alignment.center,
                            child: const Icon(Icons.storefront_outlined, color: AppColors.blueprint600, size: 22),
                          ),
                          const SizedBox(width: 12),
                          Expanded(
                            child: Column(
                              crossAxisAlignment: CrossAxisAlignment.start,
                              children: [
                                Text(d.orcamento.fornecedorNome, style: const TextStyle(fontWeight: FontWeight.w800, fontSize: 17)),
                                const SizedBox(height: 2),
                                Text('CNPJ: ${d.orcamento.fornecedorCnpj}', style: const TextStyle(color: AppColors.steel500, fontSize: 12.5)),
                              ],
                            ),
                          ),
                          StatusBadge(kind: StatusBadgeKind.orcamento, status: d.orcamento.status),
                        ],
                      ),
                      const Divider(height: 24),
                      Wrap(
                        spacing: 18,
                        runSpacing: 10,
                        children: [
                          _InfoItem(
                            icon: Icons.person_outline,
                            label: 'Criado por',
                            value: d.orcamento.createdByName ?? '—',
                          ),
                          _InfoItem(
                            icon: Icons.calendar_today_outlined,
                            label: 'Criado em',
                            value: _dateFormat.format(DateTime.parse(d.orcamento.createdAt).toLocal()),
                          ),
                          if (d.orcamento.fornecedorFormaPagamento != null)
                            _InfoItem(
                              icon: Icons.payments_outlined,
                              label: 'Pagamento',
                              value: _paymentLabels[d.orcamento.fornecedorFormaPagamento] ?? d.orcamento.fornecedorFormaPagamento!,
                            ),
                          if (d.orcamento.fornecedorPixKey != null)
                            _InfoItem(icon: Icons.qr_code, label: 'Chave Pix', value: d.orcamento.fornecedorPixKey!),
                          if (d.orcamento.sourcePurchaseRequestName != null)
                            _InfoItem(
                              icon: Icons.shopping_cart_outlined,
                              label: 'Pedido de compra',
                              value: d.orcamento.sourcePurchaseRequestName!,
                            ),
                        ],
                      ),
                    ],
                  ),
                ),

                const SizedBox(height: 16),
                Container(
                  padding: const EdgeInsets.all(16),
                  decoration: BoxDecoration(
                    color: Colors.white,
                    borderRadius: BorderRadius.circular(16),
                    boxShadow: [BoxShadow(color: Colors.black.withValues(alpha: 0.03), blurRadius: 8, offset: const Offset(0, 2))],
                  ),
                  child: Column(
                    crossAxisAlignment: CrossAxisAlignment.start,
                    children: [
                      const Row(
                        children: [
                          Icon(Icons.receipt_long_outlined, size: 17, color: AppColors.blueprint600),
                          SizedBox(width: 8),
                          Text('Itens do orçamento', style: TextStyle(fontWeight: FontWeight.w700, fontSize: 13, color: AppColors.steel700)),
                        ],
                      ),
                      const SizedBox(height: 12),
                      ...d.lineItems.map(
                        (item) => Padding(
                          padding: const EdgeInsets.only(bottom: 10),
                          child: Row(
                            children: [
                              Expanded(
                                child: Column(
                                  crossAxisAlignment: CrossAxisAlignment.start,
                                  children: [
                                    Row(
                                      children: [
                                        Expanded(
                                          child: Text(item.name, style: const TextStyle(fontWeight: FontWeight.w700, fontSize: 13.5)),
                                        ),
                                        if (item.selected)
                                          const Padding(
                                            padding: EdgeInsets.only(left: 6),
                                            child: Icon(Icons.check_circle, size: 15, color: AppColors.emerald600),
                                          ),
                                      ],
                                    ),
                                    const SizedBox(height: 2),
                                    Text('${item.quantity} ${item.type ?? ''}'.trim(), style: const TextStyle(fontSize: 12, color: AppColors.steel500)),
                                  ],
                                ),
                              ),
                              Text(
                                item.unitPrice != null ? 'R\$ ${item.unitPrice}' : '—',
                                style: const TextStyle(fontWeight: FontWeight.w800, fontSize: 13.5),
                              ),
                            ],
                          ),
                        ),
                      ),
                      const Divider(height: 20),
                      Row(
                        mainAxisAlignment: MainAxisAlignment.spaceBetween,
                        children: [
                          const Text('Total selecionado', style: TextStyle(fontWeight: FontWeight.w700, fontSize: 13.5)),
                          Text(
                            'R\$ ${total.toStringAsFixed(2)}',
                            style: const TextStyle(fontWeight: FontWeight.w800, fontSize: 16, color: AppColors.blueprint600),
                          ),
                        ],
                      ),
                    ],
                  ),
                ),
              ],
            ),
          );
        },
      ),
    );
  }
}

class _InfoItem extends StatelessWidget {
  const _InfoItem({required this.icon, required this.label, required this.value});
  final IconData icon;
  final String label;
  final String value;

  @override
  Widget build(BuildContext context) {
    return SizedBox(
      width: 150,
      child: Row(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Icon(icon, size: 14, color: AppColors.steel400),
          const SizedBox(width: 5),
          Expanded(
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Text(label, style: const TextStyle(fontSize: 10.5, color: AppColors.steel400)),
                Text(
                  value,
                  maxLines: 1,
                  overflow: TextOverflow.ellipsis,
                  style: const TextStyle(fontSize: 12.5, fontWeight: FontWeight.w600, color: AppColors.steel700),
                ),
              ],
            ),
          ),
        ],
      ),
    );
  }
}
