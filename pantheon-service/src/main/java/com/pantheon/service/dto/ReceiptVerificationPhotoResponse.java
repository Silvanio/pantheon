package com.pantheon.service.dto;

import com.pantheon.service.entity.ReceiptVerificationPhoto;
import java.time.Instant;
import java.util.UUID;

public record ReceiptVerificationPhotoResponse(
        UUID id, UUID receiptVerificationId, String contentType, Instant createdAt) {

    public static ReceiptVerificationPhotoResponse from(ReceiptVerificationPhoto photo) {
        return new ReceiptVerificationPhotoResponse(
                photo.getId(), photo.getReceiptVerificationId(), photo.getContentType(), photo.getCreatedAt());
    }
}
