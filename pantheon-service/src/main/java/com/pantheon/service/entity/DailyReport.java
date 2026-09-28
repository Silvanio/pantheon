package com.pantheon.service.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;

@Entity
@Table(name = "daily_report")
public class DailyReport {

    @Id
    private UUID id;

    @Column(name = "construction_site_id", nullable = false)
    private UUID constructionSiteId;

    @Column(name = "report_date", nullable = false)
    private LocalDate reportDate;

    @Column(name = "sequence_no", nullable = false)
    private int sequenceNo;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private DailyReportStatus status;

    @Column(name = "weather_condition_morning")
    private String weatherConditionMorning;

    @Column(name = "weather_condition_afternoon")
    private String weatherConditionAfternoon;

    @Column(name = "weather_blocked_tasks")
    private Boolean weatherBlockedTasks;

    @Column(name = "work_hours_start")
    private LocalTime workHoursStart;

    @Column(name = "work_hours_end")
    private LocalTime workHoursEnd;

    @Column(columnDefinition = "TEXT")
    private String comments;

    @Column(name = "created_by", nullable = false)
    private UUID createdBy;

    @Column(name = "submitted_at")
    private Instant submittedAt;

    @Column(name = "current_approval_cycle", nullable = false)
    private int currentApprovalCycle;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected DailyReport() {
        // JPA
    }

    public DailyReport(
            UUID id, UUID constructionSiteId, LocalDate reportDate, int sequenceNo, UUID createdBy, Instant createdAt) {
        this.id = id;
        this.constructionSiteId = constructionSiteId;
        this.reportDate = reportDate;
        this.sequenceNo = sequenceNo;
        this.status = DailyReportStatus.DRAFT;
        this.createdBy = createdBy;
        this.currentApprovalCycle = 0;
        this.createdAt = createdAt;
        this.updatedAt = createdAt;
    }

    public void updateCore(
            String weatherConditionMorning,
            String weatherConditionAfternoon,
            Boolean weatherBlockedTasks,
            LocalTime workHoursStart,
            LocalTime workHoursEnd,
            String comments,
            Instant now) {
        if (weatherConditionMorning != null) {
            this.weatherConditionMorning = weatherConditionMorning;
        }
        if (weatherConditionAfternoon != null) {
            this.weatherConditionAfternoon = weatherConditionAfternoon;
        }
        if (weatherBlockedTasks != null) {
            this.weatherBlockedTasks = weatherBlockedTasks;
        }
        if (workHoursStart != null) {
            this.workHoursStart = workHoursStart;
        }
        if (workHoursEnd != null) {
            this.workHoursEnd = workHoursEnd;
        }
        if (comments != null) {
            this.comments = comments;
        }
        this.updatedAt = now;
    }

    /** Starts a new approval cycle: DRAFT -> PENDING_APPROVAL. Its approval steps are created by the service. */
    public void submit(Instant now) {
        this.status = DailyReportStatus.PENDING_APPROVAL;
        this.submittedAt = now;
        this.currentApprovalCycle += 1;
        this.updatedAt = now;
    }

    /** The cycle's final step was approved: PENDING_APPROVAL -> APPROVED. */
    public void approve(Instant now) {
        this.status = DailyReportStatus.APPROVED;
        this.updatedAt = now;
    }

    /** A rejection at any approval step bounces the report back to DRAFT for revision: PENDING_APPROVAL -> DRAFT. */
    public void reject(Instant now) {
        this.status = DailyReportStatus.DRAFT;
        this.updatedAt = now;
    }

    public boolean isEditable() {
        return status == DailyReportStatus.DRAFT;
    }

    public void touch(Instant now) {
        this.updatedAt = now;
    }

    public UUID getId() {
        return id;
    }

    public UUID getConstructionSiteId() {
        return constructionSiteId;
    }

    public LocalDate getReportDate() {
        return reportDate;
    }

    public int getSequenceNo() {
        return sequenceNo;
    }

    public DailyReportStatus getStatus() {
        return status;
    }

    public String getWeatherConditionMorning() {
        return weatherConditionMorning;
    }

    public String getWeatherConditionAfternoon() {
        return weatherConditionAfternoon;
    }

    public Boolean getWeatherBlockedTasks() {
        return weatherBlockedTasks;
    }

    public LocalTime getWorkHoursStart() {
        return workHoursStart;
    }

    public LocalTime getWorkHoursEnd() {
        return workHoursEnd;
    }

    public String getComments() {
        return comments;
    }

    public UUID getCreatedBy() {
        return createdBy;
    }

    public Instant getSubmittedAt() {
        return submittedAt;
    }

    public int getCurrentApprovalCycle() {
        return currentApprovalCycle;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
