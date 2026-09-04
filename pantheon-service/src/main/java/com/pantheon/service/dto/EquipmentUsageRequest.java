package com.pantheon.service.dto;

import jakarta.validation.constraints.NotNull;
import java.util.UUID;

public record EquipmentUsageRequest(@NotNull UUID equipmentId, String statusNote) {
}
