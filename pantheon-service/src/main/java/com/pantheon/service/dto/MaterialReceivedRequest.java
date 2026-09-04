package com.pantheon.service.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.math.BigDecimal;
import java.util.UUID;

public record MaterialReceivedRequest(@NotNull UUID materialId, @NotNull @Positive BigDecimal quantity) {
}
