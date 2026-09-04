package com.pantheon.service.dto;

import com.pantheon.service.entity.OrcamentoLineItem;
import java.math.BigDecimal;
import java.util.UUID;

public record OrcamentoLineItemResponse(UUID id, UUID materialRequestItemId, BigDecimal unitPrice) {

    public static OrcamentoLineItemResponse from(OrcamentoLineItem item) {
        return new OrcamentoLineItemResponse(item.getId(), item.getMaterialRequestItemId(), item.getUnitPrice());
    }
}
