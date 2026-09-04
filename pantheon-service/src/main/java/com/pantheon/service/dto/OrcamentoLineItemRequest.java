package com.pantheon.service.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.util.UUID;

public record OrcamentoLineItemRequest(
        @NotNull UUID materialRequestItemId, @NotNull @DecimalMin("0.0") BigDecimal unitPrice) {
}
