package com.pantheon.service.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "daily_report_activity")
public class DailyReportActivity {

    @Id
    private UUID id;

    @Column(name = "daily_report_id", nullable = false)
    private UUID dailyReportId;

    @Column(nullable = false)
    private String description;

    @Column(name = "progress_note", nullable = false)
    private String progressNote;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ActivityStatus status;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected DailyReportActivity() {
        // JPA
    }

    public DailyReportActivity(
            UUID id, UUID dailyReportId, String description, String progressNote, ActivityStatus status,
            Instant createdAt) {
        this.id = id;
        this.dailyReportId = dailyReportId;
        this.description = description;
        this.progressNote = progressNote;
        this.status = status;
        this.createdAt = createdAt;
    }

    public UUID getId() {
        return id;
    }

    public UUID getDailyReportId() {
        return dailyReportId;
    }

    public String getDescription() {
        return description;
    }

    public String getProgressNote() {
        return progressNote;
    }

    public ActivityStatus getStatus() {
        return status;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
