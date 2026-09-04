package com.pantheon.service.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

/** A PDF file attached to a {@link SiteDocumentProject}, stored in object storage. */
@Entity
@Table(name = "site_document_project_attachment")
public class SiteDocumentProjectAttachment {

    @Id
    private UUID id;

    @Column(name = "site_document_project_id", nullable = false)
    private UUID siteDocumentProjectId;

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

    protected SiteDocumentProjectAttachment() {
        // JPA
    }

    public SiteDocumentProjectAttachment(
            UUID id,
            UUID siteDocumentProjectId,
            String storageKey,
            String contentType,
            String originalName,
            UUID uploadedBy,
            Instant createdAt) {
        this.id = id;
        this.siteDocumentProjectId = siteDocumentProjectId;
        this.storageKey = storageKey;
        this.contentType = contentType;
        this.originalName = originalName;
        this.uploadedBy = uploadedBy;
        this.createdAt = createdAt;
    }

    public UUID getId() {
        return id;
    }

    public UUID getSiteDocumentProjectId() {
        return siteDocumentProjectId;
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
