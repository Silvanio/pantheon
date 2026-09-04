package com.pantheon.service.dto;

import java.util.UUID;

import com.pantheon.service.entity.Equipment;
import com.pantheon.service.entity.EquipmentStatus;
public record EquipmentResponse(UUID id, UUID constructionSiteId, String name, String type, EquipmentStatus status) {

    public static EquipmentResponse from(Equipment equipment) {
        return new EquipmentResponse(
                equipment.getId(),
                equipment.getConstructionSiteId(),
                equipment.getName(),
                equipment.getType(),
                equipment.getStatus());
    }
}
