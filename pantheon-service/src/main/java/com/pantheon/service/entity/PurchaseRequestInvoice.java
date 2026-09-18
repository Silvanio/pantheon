package com.pantheon.service.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

/** A fiscal invoice ("nota fiscal") attached to a {@link PurchaseRequest}, stored in object storage. */
@Entity
@Table(name = "purchase_request_invoice")
public class PurchaseRequestInvoice {

    @Id
    private UUID id;

    @Column(name = "purchase_request_id", nullable = false)
    private UUID purchaseRequestId;

    @Column(name = "storage_key", nullable = false)
    private String storageKey;

    @Column(name = "content_type", nullable = false)
    private String contentType;

    @Column(name = "original_name", nullable = false)
    private String originalName;

    @Column(name = "uploaded_by", nullable = false)
    private UUID uploadedBy;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected PurchaseRequestInvoice() {
        // JPA
    }

    public PurchaseRequestInvoice(
            UUID id, UUID purchaseRequestId, String storageKey, String contentType, String originalName,
            UUID uploadedBy, Instant createdAt) {
        this.id = id;
        this.purchaseRequestId = purchaseRequestId;
        this.storageKey = storageKey;
        this.contentType = contentType;
        this.originalName = originalName;
        this.uploadedBy = uploadedBy;
        this.createdAt = createdAt;
    }

    public UUID getId() {
        return id;
    }

    public UUID getPurchaseRequestId() {
        return purchaseRequestId;
    }

    public String getStorageKey() {
        return storageKey;
    }

    public String getContentType() {
        return contentType;
    }

    public String getOriginalName() {
        return originalName;
    }

    public UUID getUploadedBy() {
        return uploadedBy;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
