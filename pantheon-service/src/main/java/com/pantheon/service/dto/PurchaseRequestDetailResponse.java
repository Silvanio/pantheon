package com.pantheon.service.dto;

import java.util.List;

public record PurchaseRequestDetailResponse(PurchaseRequestResponse purchaseRequest, List<PurchaseRequestItemResponse> items) {
}
