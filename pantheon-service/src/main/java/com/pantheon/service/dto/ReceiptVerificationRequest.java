package com.pantheon.service.dto;

import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.util.UUID;

public record ReceiptVerificationRequest(
        @NotNull UUID materialRequestItemId, @NotNull BigDecimal receivedQuantity, String note) {
}
