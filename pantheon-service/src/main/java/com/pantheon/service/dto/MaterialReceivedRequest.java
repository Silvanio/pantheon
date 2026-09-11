package com.pantheon.service.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.math.BigDecimal;

public record MaterialReceivedRequest(@NotBlank String materialName, String unit, @NotNull @Positive BigDecimal quantity) {
}
