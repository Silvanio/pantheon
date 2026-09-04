package com.pantheon.service.dto;

import java.util.UUID;

import com.pantheon.service.entity.Material;
public record MaterialResponse(UUID id, UUID constructionSiteId, String name, String unit) {

    public static MaterialResponse from(Material material) {
        return new MaterialResponse(
                material.getId(), material.getConstructionSiteId(), material.getName(), material.getUnit());
    }
}
