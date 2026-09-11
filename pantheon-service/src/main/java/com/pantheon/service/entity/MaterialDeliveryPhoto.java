package com.pantheon.service.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

/** A conference/proof photo attached when a {@link Material} is marked {@code DELIVERED_AND_CHECKED}. */
@Entity
@Table(name = "material_delivery_photo")
public class MaterialDeliveryPhoto {

    @Id
    private UUID id;

    @Column(name = "material_id", nullable = false)
    private UUID materialId;

    @Column(name = "storage_key", nullable = false)
    private String storageKey;

    @Column(name = "content_type", nullable = false)
    private String contentType;

    @Column(name = "uploaded_by", nullable = false)
    private UUID uploadedBy;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected MaterialDeliveryPhoto() {
        // JPA
    }

    public MaterialDeliveryPhoto(
            UUID id, UUID materialId, String storageKey, String contentType, UUID uploadedBy, Instant createdAt) {
        this.id = id;
        this.materialId = materialId;
        this.storageKey = storageKey;
        this.contentType = contentType;
        this.uploadedBy = uploadedBy;
        this.createdAt = createdAt;
    }

    public UUID getId() {
        return id;
    }

    public UUID getMaterialId() {
        return materialId;
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
