import '../../core/models/json_utils.dart';

class OrcamentoLineItem {
  OrcamentoLineItem({
    required this.id,
    required this.name,
    required this.type,
    required this.quantity,
    required this.unitPrice,
    required this.selected,
  });

  factory OrcamentoLineItem.fromJson(Map<String, dynamic> json) => OrcamentoLineItem(
        id: json['id'] as String,
        name: json['name'] as String,
        type: json['type'] as String?,
        quantity: asDecimalString(json['quantity']),
        unitPrice: asDecimalStringOrNull(json['unitPrice']),
        selected: json['selected'] as bool,
      );

  final String id;
  final String name;
  final String? type;
  final String quantity;
  final String? unitPrice;
  final bool selected;
}

class Orcamento {
  Orcamento({
    required this.id,
    required this.constructionSiteId,
    required this.status,
    required this.createdBy,
    required this.createdAt,
    required this.fornecedorCnpj,
    required this.fornecedorNome,
    this.fornecedorEndereco,
    this.fornecedorContatoNome,
    this.fornecedorContatoTelefone,
    this.fornecedorFormaPagamento,
    this.fornecedorPixKey,
    this.sourcePurchaseRequestId,
    this.sourcePurchaseRequestName,
  });

  factory Orcamento.fromJson(Map<String, dynamic> json) => Orcamento(
        id: json['id'] as String,
        constructionSiteId: json['constructionSiteId'] as String,
        status: json['status'] as String,
        createdBy: json['createdBy'] as String,
        createdAt: json['createdAt'] as String,
        fornecedorCnpj: json['fornecedorCnpj'] as String,
        fornecedorNome: json['fornecedorNome'] as String,
        fornecedorEndereco: json['fornecedorEndereco'] as String?,
        fornecedorContatoNome: json['fornecedorContatoNome'] as String?,
        fornecedorContatoTelefone: json['fornecedorContatoTelefone'] as String?,
        fornecedorFormaPagamento: json['fornecedorFormaPagamento'] as String?,
        fornecedorPixKey: json['fornecedorPixKey'] as String?,
        sourcePurchaseRequestId: json['sourcePurchaseRequestId'] as String?,
        sourcePurchaseRequestName: json['sourcePurchaseRequestName'] as String?,
      );

  final String id;
  final String constructionSiteId;
  final String status;
  final String createdBy;
  final String createdAt;
  final String fornecedorCnpj;
  final String fornecedorNome;
  final String? fornecedorEndereco;
  final String? fornecedorContatoNome;
  final String? fornecedorContatoTelefone;
  final String? fornecedorFormaPagamento;
  final String? fornecedorPixKey;
  final String? sourcePurchaseRequestId;
  final String? sourcePurchaseRequestName;
}

class OrcamentoDetail {
  OrcamentoDetail({required this.orcamento, required this.lineItems});

  factory OrcamentoDetail.fromJson(Map<String, dynamic> json) => OrcamentoDetail(
        orcamento: Orcamento.fromJson(json['orcamento'] as Map<String, dynamic>),
        lineItems:
            (json['lineItems'] as List).map((e) => OrcamentoLineItem.fromJson(e as Map<String, dynamic>)).toList(),
      );

  final Orcamento orcamento;
  final List<OrcamentoLineItem> lineItems;
}
