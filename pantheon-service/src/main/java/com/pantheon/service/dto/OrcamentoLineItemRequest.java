package com.pantheon.service.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.math.BigDecimal;

public record OrcamentoLineItemRequest(
        @NotBlank String name, String type, @NotNull @Positive BigDecimal quantity, BigDecimal unitPrice) {
}
