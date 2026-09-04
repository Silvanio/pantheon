package com.pantheon.service.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.math.BigDecimal;
import java.util.UUID;

public record MaterialRequestItemCreationRequest(@NotNull UUID materialId, @NotNull @Positive BigDecimal requestedQuantity) {
}
