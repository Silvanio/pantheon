package com.pantheon.service.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

/** A payment-proof or invoice ("nota fiscal") file attached to an {@link Orcamento}. */
@Entity
@Table(name = "orcamento_attachment")
public class OrcamentoAttachment {

    @Id
    private UUID id;

    @Column(name = "orcamento_id", nullable = false)
    private UUID orcamentoId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AttachmentKind kind;

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

    protected OrcamentoAttachment() {
        // JPA
    }

    public OrcamentoAttachment(
            UUID id,
            UUID orcamentoId,
            AttachmentKind kind,
            String storageKey,
            String contentType,
            String originalName,
            UUID uploadedBy,
            Instant createdAt) {
        this.id = id;
        this.orcamentoId = orcamentoId;
        this.kind = kind;
        this.storageKey = storageKey;
        this.contentType = contentType;
        this.originalName = originalName;
        this.uploadedBy = uploadedBy;
        this.createdAt = createdAt;
    }

    public UUID getId() {
        return id;
    }

    public UUID getOrcamentoId() {
        return orcamentoId;
    }

    public AttachmentKind getKind() {
        return kind;
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
