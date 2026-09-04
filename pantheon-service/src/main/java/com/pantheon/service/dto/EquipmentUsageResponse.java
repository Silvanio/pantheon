package com.pantheon.service.dto;

import java.util.UUID;

import com.pantheon.service.entity.DailyReportEquipmentUsage;
public record EquipmentUsageResponse(UUID id, UUID equipmentId, String statusNote) {

    public static EquipmentUsageResponse from(DailyReportEquipmentUsage usage) {
        return new EquipmentUsageResponse(usage.getId(), usage.getEquipmentId(), usage.getStatusNote());
    }
}
