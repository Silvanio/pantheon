package com.pantheon.service.dto;

import com.pantheon.service.entity.PurchaseRequest;
import java.time.Instant;
import java.util.UUID;

public record PurchaseRequestResponse(UUID id, UUID constructionSiteId, String name, UUID createdBy, Instant createdAt) {

    public static PurchaseRequestResponse from(PurchaseRequest purchaseRequest) {
        return new PurchaseRequestResponse(
                purchaseRequest.getId(), purchaseRequest.getConstructionSiteId(), purchaseRequest.getName(),
                purchaseRequest.getCreatedBy(), purchaseRequest.getCreatedAt());
    }
}
