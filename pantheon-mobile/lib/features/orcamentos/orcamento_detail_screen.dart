import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../core/widgets/async_value_view.dart';
import '../../theme/app_colors.dart';
import 'orcamento_repository.dart';

final _detailProvider = FutureProvider.family((ref, String id) => ref.watch(orcamentoRepositoryProvider).getDetail(id));

const _paymentLabels = {'CARTAO': 'Cartão', 'BOLETO': 'Boleto', 'PIX': 'Pix', 'DINHEIRO': 'Dinheiro'};

class OrcamentoDetailScreen extends ConsumerWidget {
  const OrcamentoDetailScreen({super.key, required this.id});
  final String id;

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final detail = ref.watch(_detailProvider(id));
    return Scaffold(
      appBar: AppBar(title: const Text('Orçamento')),
      body: AsyncValueView(
        value: detail,
        data: (d) {
          final total = d.lineItems
              .where((item) => item.selected && item.unitPrice != null)
              .fold<double>(0, (sum, item) => sum + (double.tryParse(item.unitPrice!) ?? 0) * (double.tryParse(item.quantity) ?? 0));

          return ListView(
            padding: const EdgeInsets.all(16),
            children: [
              Card(
                child: Padding(
                  padding: const EdgeInsets.all(16),
                  child: Column(
                    crossAxisAlignment: CrossAxisAlignment.start,
                    children: [
                      const Text('Fornecedor', style: TextStyle(fontWeight: FontWeight.w700, fontSize: 13, color: AppColors.steel500)),
                      const SizedBox(height: 6),
                      Text(d.orcamento.fornecedorNome, style: const TextStyle(fontWeight: FontWeight.w800, fontSize: 17)),
                      const SizedBox(height: 4),
                      Text('CNPJ: ${d.orcamento.fornecedorCnpj}', style: const TextStyle(color: AppColors.steel500, fontSize: 12.5)),
                      if (d.orcamento.fornecedorFormaPagamento != null)
                        Text(
                          'Pagamento: ${_paymentLabels[d.orcamento.fornecedorFormaPagamento] ?? d.orcamento.fornecedorFormaPagamento}',
                          style: const TextStyle(color: AppColors.steel500, fontSize: 12.5),
                        ),
                      if (d.orcamento.fornecedorPixKey != null)
                        Text('Chave Pix: ${d.orcamento.fornecedorPixKey}', style: const TextStyle(color: AppColors.steel500, fontSize: 12.5)),
                    ],
                  ),
                ),
              ),
              const SizedBox(height: 16),
              const Text('Itens do orçamento', style: TextStyle(fontWeight: FontWeight.w700, fontSize: 14)),
              const SizedBox(height: 8),
              ...d.lineItems.map(
                (item) => Card(
                  margin: const EdgeInsets.only(bottom: 8),
                  child: ListTile(
                    dense: true,
                    title: Text(item.name),
                    subtitle: Text('${item.quantity} ${item.type ?? ''}'.trim()),
                    trailing: Text(
                      item.unitPrice != null ? 'R\$ ${item.unitPrice}' : '—',
                      style: const TextStyle(fontWeight: FontWeight.w700),
                    ),
                  ),
                ),
              ),
              const SizedBox(height: 8),
              Card(
                child: Padding(
                  padding: const EdgeInsets.all(16),
                  child: Row(
                    mainAxisAlignment: MainAxisAlignment.spaceBetween,
                    children: [
                      const Text('Total', style: TextStyle(fontWeight: FontWeight.w700)),
                      Text('R\$ ${total.toStringAsFixed(2)}', style: const TextStyle(fontWeight: FontWeight.w800)),
                    ],
                  ),
                ),
              ),
            ],
          );
        },
      ),
    );
  }
}
