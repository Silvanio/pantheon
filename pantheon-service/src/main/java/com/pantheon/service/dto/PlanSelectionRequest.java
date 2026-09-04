package com.pantheon.service.dto;

import com.pantheon.service.entity.PlanCode;
import jakarta.validation.constraints.NotNull;

public record PlanSelectionRequest(@NotNull PlanCode planCode) {
}
