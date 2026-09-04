package com.pantheon.service.dto;

import java.util.List;

public record MaterialRequestDetailResponse(
        MaterialRequestResponse request,
        List<MaterialRequestItemResponse> items,
        List<ReceiptVerificationResponse> verifications) {
}
