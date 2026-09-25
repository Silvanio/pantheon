import '../../core/models/json_utils.dart';

/// Delivery-tracking record created only from a concluded Pedido de Compra (see
/// `pantheon-service`'s `MaterialService` doc comment) — mirrors `pantheon-web`'s
/// `useMaterialDeliveries.ts` `Material` interface.
class MaterialDelivery {
  MaterialDelivery({
    required this.id,
    required this.constructionSiteId,
    required this.name,
    this.type,
    required this.quantity,
    required this.status,
    this.sourcePurchaseRequestId,
    this.sourcePurchaseRequestName,
  });

  factory MaterialDelivery.fromJson(Map<String, dynamic> json) => MaterialDelivery(
        id: json['id'] as String,
        constructionSiteId: json['constructionSiteId'] as String,
        name: json['name'] as String,
        type: json['type'] as String?,
        quantity: asDecimalString(json['quantity']),
        status: json['status'] as String,
        sourcePurchaseRequestId: json['sourcePurchaseRequestId'] as String?,
        sourcePurchaseRequestName: json['sourcePurchaseRequestName'] as String?,
      );

  final String id;
  final String constructionSiteId;
  final String name;
  final String? type;
  final String quantity;

  /// `AWAITING_DELIVERY` -> `DELIVERED` -> `DELIVERED_AND_CHECKED`, strictly sequential.
  final String status;
  final String? sourcePurchaseRequestId;
  final String? sourcePurchaseRequestName;
}
