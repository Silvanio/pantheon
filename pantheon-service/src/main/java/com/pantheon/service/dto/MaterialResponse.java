package com.pantheon.service.dto;

import com.pantheon.service.entity.Material;
import com.pantheon.service.entity.MaterialDeliveryStatus;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record MaterialResponse(
        UUID id,
        UUID constructionSiteId,
        UUID orcamentoLineItemId,
        String name,
        String type,
        BigDecimal quantity,
        MaterialDeliveryStatus status,
        Instant deliveredAt,
        UUID deliveredBy,
        Instant checkedAt,
        UUID checkedBy,
        Instant createdAt,
        UUID sourcePurchaseRequestId,
        String sourcePurchaseRequestName) {

    public static MaterialResponse from(Material material) {
        return from(material, null, null);
    }

    public static MaterialResponse from(Material material, UUID sourcePurchaseRequestId, String sourcePurchaseRequestName) {
        return new MaterialResponse(
                material.getId(), material.getConstructionSiteId(), material.getOrcamentoLineItemId(),
                material.getName(), material.getType(), material.getQuantity(), material.getStatus(),
                material.getDeliveredAt(), material.getDeliveredBy(), material.getCheckedAt(),
                material.getCheckedBy(), material.getCreatedAt(), sourcePurchaseRequestId, sourcePurchaseRequestName);
    }
}
