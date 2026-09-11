package com.pantheon.service.dto;

import com.pantheon.service.entity.MaterialDeliveryPhoto;
import java.time.Instant;
import java.util.UUID;

public record MaterialDeliveryPhotoResponse(UUID id, UUID materialId, UUID uploadedBy, Instant createdAt) {

    public static MaterialDeliveryPhotoResponse from(MaterialDeliveryPhoto photo) {
        return new MaterialDeliveryPhotoResponse(
                photo.getId(), photo.getMaterialId(), photo.getUploadedBy(), photo.getCreatedAt());
    }
}
