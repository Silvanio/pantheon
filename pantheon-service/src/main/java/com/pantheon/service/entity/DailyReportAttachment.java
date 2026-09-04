package com.pantheon.service.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "daily_report_attachment")
public class DailyReportAttachment {

    @Id
    private UUID id;

    @Column(name = "daily_report_id", nullable = false)
    private UUID dailyReportId;

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

    protected DailyReportAttachment() {
        // JPA
    }

    public DailyReportAttachment(
            UUID id, UUID dailyReportId, String storageKey, String contentType, String originalName, UUID uploadedBy,
            Instant createdAt) {
        this.id = id;
        this.dailyReportId = dailyReportId;
        this.storageKey = storageKey;
        this.contentType = contentType;
        this.originalName = originalName;
        this.uploadedBy = uploadedBy;
        this.createdAt = createdAt;
    }

    public UUID getId() {
        return id;
    }

    public UUID getDailyReportId() {
        return dailyReportId;
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
