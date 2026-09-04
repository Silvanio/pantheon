package com.pantheon.service.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import com.pantheon.service.entity.ReceiptVerification;
public record ReceiptVerificationResponse(
        UUID id,
        UUID materialRequestItemId,
        BigDecimal receivedQuantity,
        BigDecimal requestedQuantity,
        boolean divergent,
        String note,
        Instant verifiedAt) {

    public static ReceiptVerificationResponse from(ReceiptVerification verification, BigDecimal requestedQuantity) {
        boolean divergent = requestedQuantity != null
                && verification.getReceivedQuantity().compareTo(requestedQuantity) != 0;
        return new ReceiptVerificationResponse(
                verification.getId(),
                verification.getMaterialRequestItemId(),
                verification.getReceivedQuantity(),
                requestedQuantity,
                divergent,
                verification.getNote(),
                verification.getVerifiedAt());
    }
}
