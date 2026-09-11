package com.pantheon.service.dto;

import com.pantheon.service.entity.PurchaseRequestItem;
import com.pantheon.service.entity.PurchaseRequestItemStatus;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record PurchaseRequestItemResponse(
        UUID id,
        UUID constructionSiteId,
        UUID purchaseRequestId,
        String name,
        String type,
        BigDecimal quantity,
        String unit,
        PurchaseRequestItemStatus status,
        UUID createdBy,
        Instant createdAt,
        UUID convertedToOrcamentoId,
        Instant convertedAt) {

    public static PurchaseRequestItemResponse from(PurchaseRequestItem item) {
        return new PurchaseRequestItemResponse(
                item.getId(), item.getConstructionSiteId(), item.getPurchaseRequestId(), item.getName(),
                item.getType(), item.getQuantity(), item.getUnit(), item.getStatus(), item.getCreatedBy(),
                item.getCreatedAt(), item.getConvertedToOrcamentoId(), item.getConvertedAt());
    }
}
