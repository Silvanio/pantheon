package com.pantheon.service.dto;

import com.pantheon.service.entity.OrcamentoLineItem;
import java.math.BigDecimal;
import java.util.UUID;

public record OrcamentoLineItemResponse(
        UUID id,
        UUID orcamentoId,
        String name,
        String type,
        BigDecimal quantity,
        BigDecimal unitPrice,
        UUID sourcePurchaseRequestItemId) {

    public static OrcamentoLineItemResponse from(OrcamentoLineItem item) {
        return new OrcamentoLineItemResponse(
                item.getId(), item.getOrcamentoId(), item.getName(), item.getType(), item.getQuantity(),
                item.getUnitPrice(), item.getSourcePurchaseRequestItemId());
    }
}
