package com.pantheon.service.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

/**
 * A file in a construction site's "Projetos" explorer, stored in object storage.
 * {@code siteDocumentProjectId == null} means it sits at the site's root; otherwise it is nested
 * under that folder. May optionally link to a {@code TaskCard} — the only way that link is ever
 * created is by attaching the file from the task's own detail view (never editable from
 * Projetos itself); it is cleared (never cascaded) when the task is deleted.
 */
@Entity
@Table(name = "site_document_project_attachment")
public class SiteDocumentProjectAttachment {

    @Id
    private UUID id;

    @Column(name = "construction_site_id", nullable = false)
    private UUID constructionSiteId;

    @Column(name = "site_document_project_id")
    private UUID siteDocumentProjectId;

    @Column(name = "storage_key", nullable = false)
    private String storageKey;

    @Column(name = "content_type", nullable = false)
    private String contentType;

    @Column(name = "original_name", nullable = false)
    private String originalName;

    @Column(name = "task_card_id")
    private UUID taskCardId;

    @Column(name = "uploaded_by", nullable = false)
    private UUID uploadedBy;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected SiteDocumentProjectAttachment() {
        // JPA
    }

    public SiteDocumentProjectAttachment(
            UUID id,
            UUID constructionSiteId,
            UUID siteDocumentProjectId,
            String storageKey,
            String contentType,
            String originalName,
            UUID taskCardId,
            UUID uploadedBy,
            Instant createdAt) {
        this.id = id;
        this.constructionSiteId = constructionSiteId;
        this.siteDocumentProjectId = siteDocumentProjectId;
        this.storageKey = storageKey;
        this.contentType = contentType;
        this.originalName = originalName;
        this.taskCardId = taskCardId;
        this.uploadedBy = uploadedBy;
        this.createdAt = createdAt;
    }

    public UUID getId() {
        return id;
    }

    public UUID getConstructionSiteId() {
        return constructionSiteId;
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

    public UUID getTaskCardId() {
        return taskCardId;
    }

    public UUID getUploadedBy() {
        return uploadedBy;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
