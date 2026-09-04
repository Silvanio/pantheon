package com.pantheon.service.dto;

import jakarta.validation.constraints.NotNull;

import com.pantheon.service.entity.Plan;
public record PlanSelectionRequest(@NotNull Plan plan) {
}
