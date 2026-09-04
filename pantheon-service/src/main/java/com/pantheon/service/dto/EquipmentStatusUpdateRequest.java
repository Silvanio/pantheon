package com.pantheon.service.dto;

import jakarta.validation.constraints.NotNull;

import com.pantheon.service.entity.EquipmentStatus;
public record EquipmentStatusUpdateRequest(@NotNull EquipmentStatus status) {
}
