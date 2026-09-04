package com.pantheon.service.dto;

import java.math.BigDecimal;
import java.util.UUID;

import com.pantheon.service.entity.MaterialRequestItem;
public record MaterialRequestItemResponse(UUID id, UUID materialId, BigDecimal requestedQuantity) {

    public static MaterialRequestItemResponse from(MaterialRequestItem item) {
        return new MaterialRequestItemResponse(item.getId(), item.getMaterialId(), item.getRequestedQuantity());
    }
}
