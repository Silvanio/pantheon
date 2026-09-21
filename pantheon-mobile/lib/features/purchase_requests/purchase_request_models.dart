import '../../core/models/json_utils.dart';

class LinkedOrcamentoSummary {
  LinkedOrcamentoSummary({required this.id, required this.fornecedorNome});

  factory LinkedOrcamentoSummary.fromJson(Map<String, dynamic> json) =>
      LinkedOrcamentoSummary(id: json['id'] as String, fornecedorNome: json['fornecedorNome'] as String);

  final String id;
  final String fornecedorNome;
}

class PurchaseRequest {
  PurchaseRequest({
    required this.id,
    required this.constructionSiteId,
    required this.name,
    required this.status,
    required this.createdBy,
    required this.createdAt,
    required this.submittedAt,
    required this.linkedOrcamentos,
  });

  factory PurchaseRequest.fromJson(Map<String, dynamic> json) => PurchaseRequest(
        id: json['id'] as String,
        constructionSiteId: json['constructionSiteId'] as String,
        name: json['name'] as String,
        status: json['status'] as String,
        createdBy: json['createdBy'] as String,
        createdAt: json['createdAt'] as String,
        submittedAt: json['submittedAt'] as String?,
        linkedOrcamentos: (json['linkedOrcamentos'] as List)
            .map((e) => LinkedOrcamentoSummary.fromJson(e as Map<String, dynamic>))
            .toList(),
      );

  final String id;
  final String constructionSiteId;
  final String name;
  final String status;
  final String createdBy;
  final String createdAt;
  final String? submittedAt;
  final List<LinkedOrcamentoSummary> linkedOrcamentos;
}

class PurchaseRequestItem {
  PurchaseRequestItem({
    required this.id,
    required this.name,
    required this.type,
    required this.quantity,
    required this.unit,
    required this.status,
    required this.selectedOrcamentoLineItemId,
  });

  factory PurchaseRequestItem.fromJson(Map<String, dynamic> json) => PurchaseRequestItem(
        id: json['id'] as String,
        name: json['name'] as String,
        type: json['type'] as String?,
        quantity: asDecimalString(json['quantity']),
        unit: json['unit'] as String?,
        status: json['status'] as String,
        selectedOrcamentoLineItemId: json['selectedOrcamentoLineItemId'] as String?,
      );

  final String id;
  final String name;
  final String? type;
  final String quantity;
  final String? unit;
  final String status;
  final String? selectedOrcamentoLineItemId;
}

class PurchaseRequestApproval {
  PurchaseRequestApproval({
    required this.id,
    required this.cycleNumber,
    required this.stepOrder,
    required this.approverFunction,
    required this.status,
    required this.comment,
  });

  factory PurchaseRequestApproval.fromJson(Map<String, dynamic> json) => PurchaseRequestApproval(
        id: json['id'] as String,
        cycleNumber: json['cycleNumber'] as int,
        stepOrder: json['stepOrder'] as int,
        approverFunction: json['approverFunction'] as String,
        status: json['status'] as String,
        comment: json['comment'] as String?,
      );

  final String id;
  final int cycleNumber;
  final int stepOrder;
  final String approverFunction;
  final String status;
  final String? comment;
}

class ComparisonColumn {
  ComparisonColumn({required this.orcamentoId, required this.supplierName});

  factory ComparisonColumn.fromJson(Map<String, dynamic> json) =>
      ComparisonColumn(orcamentoId: json['orcamentoId'] as String, supplierName: json['supplierName'] as String);

  final String orcamentoId;
  final String supplierName;
}

class ComparisonCell {
  ComparisonCell({required this.orcamentoId, required this.unitPrice, required this.selected});

  factory ComparisonCell.fromJson(Map<String, dynamic> json) => ComparisonCell(
        orcamentoId: json['orcamentoId'] as String,
        unitPrice: asDecimalStringOrNull(json['unitPrice']),
        selected: json['selected'] as bool,
      );

  final String orcamentoId;
  final String? unitPrice;
  final bool selected;
}

class ComparisonRow {
  ComparisonRow({
    required this.itemId,
    required this.itemName,
    required this.quantity,
    required this.unit,
    required this.cells,
  });

  factory ComparisonRow.fromJson(Map<String, dynamic> json) => ComparisonRow(
        itemId: json['itemId'] as String,
        itemName: json['itemName'] as String,
        quantity: asDecimalString(json['quantity']),
        unit: json['unit'] as String?,
        cells: (json['cells'] as List).map((e) => ComparisonCell.fromJson(e as Map<String, dynamic>)).toList(),
      );

  final String itemId;
  final String itemName;
  final String quantity;
  final String? unit;
  final List<ComparisonCell> cells;

  ComparisonCell? cellFor(String orcamentoId) {
    for (final cell in cells) {
      if (cell.orcamentoId == orcamentoId) return cell;
    }
    return null;
  }
}

class PurchaseRequestComparison {
  PurchaseRequestComparison({required this.columns, required this.rows});

  factory PurchaseRequestComparison.fromJson(Map<String, dynamic> json) => PurchaseRequestComparison(
        columns: (json['columns'] as List).map((e) => ComparisonColumn.fromJson(e as Map<String, dynamic>)).toList(),
        rows: (json['rows'] as List).map((e) => ComparisonRow.fromJson(e as Map<String, dynamic>)).toList(),
      );

  final List<ComparisonColumn> columns;
  final List<ComparisonRow> rows;
}

class PurchaseRequestDetail {
  PurchaseRequestDetail({required this.purchaseRequest, required this.items, required this.approvals});

  factory PurchaseRequestDetail.fromJson(Map<String, dynamic> json) => PurchaseRequestDetail(
        purchaseRequest: PurchaseRequest.fromJson(json['purchaseRequest'] as Map<String, dynamic>),
        items: (json['items'] as List).map((e) => PurchaseRequestItem.fromJson(e as Map<String, dynamic>)).toList(),
        approvals:
            (json['approvals'] as List).map((e) => PurchaseRequestApproval.fromJson(e as Map<String, dynamic>)).toList(),
      );

  final PurchaseRequest purchaseRequest;
  final List<PurchaseRequestItem> items;
  final List<PurchaseRequestApproval> approvals;
}
