package com.pantheon.service.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import com.pantheon.service.entity.EquipmentStatus;
public record EquipmentRegistrationRequest(@NotBlank String name, String type, @NotNull EquipmentStatus status) {
}
