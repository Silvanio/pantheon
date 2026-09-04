package com.pantheon.service.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

/** A delivery-proof photo attached to a {@link ReceiptVerification}. */
@Entity
@Table(name = "receipt_verification_photo")
public class ReceiptVerificationPhoto {

    @Id
    private UUID id;

    @Column(name = "receipt_verification_id", nullable = false)
    private UUID receiptVerificationId;

    @Column(name = "storage_key", nullable = false)
    private String storageKey;

    @Column(name = "content_type", nullable = false)
    private String contentType;

    @Column(name = "uploaded_by", nullable = false)
    private UUID uploadedBy;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected ReceiptVerificationPhoto() {
        // JPA
    }

    public ReceiptVerificationPhoto(
            UUID id, UUID receiptVerificationId, String storageKey, String contentType, UUID uploadedBy,
            Instant createdAt) {
        this.id = id;
        this.receiptVerificationId = receiptVerificationId;
        this.storageKey = storageKey;
        this.contentType = contentType;
        this.uploadedBy = uploadedBy;
        this.createdAt = createdAt;
    }

    public UUID getId() {
        return id;
    }

    public UUID getReceiptVerificationId() {
        return receiptVerificationId;
    }

    public String getStorageKey() {
        return storageKey;
    }

    public String getContentType() {
        return contentType;
    }

    public UUID getUploadedBy() {
        return uploadedBy;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
