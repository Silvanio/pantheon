package com.pantheon.service.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "schedule_task")
public class ScheduleTask {

    @Id
    private UUID id;

    @Column(name = "stage_id", nullable = false)
    private UUID stageId;

    @Column(nullable = false)
    private String title;

    @Column(name = "start_date", nullable = false)
    private LocalDate startDate;

    @Column(name = "end_date", nullable = false)
    private LocalDate endDate;

    @Column(name = "responsible_site_membership_id")
    private UUID responsibleSiteMembershipId;

    @Column(name = "percent_complete", nullable = false)
    private int percentComplete;

    @Column(name = "sort_order", nullable = false)
    private int sortOrder;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected ScheduleTask() {
        // JPA
    }

    public ScheduleTask(
            UUID id, UUID stageId, String title, LocalDate startDate, LocalDate endDate,
            UUID responsibleSiteMembershipId, int percentComplete, int sortOrder, Instant createdAt) {
        this.id = id;
        this.stageId = stageId;
        this.title = title;
        this.startDate = startDate;
        this.endDate = endDate;
        this.responsibleSiteMembershipId = responsibleSiteMembershipId;
        this.percentComplete = percentComplete;
        this.sortOrder = sortOrder;
        this.createdAt = createdAt;
        this.updatedAt = createdAt;
    }

    public void update(
            String title, LocalDate startDate, LocalDate endDate, UUID responsibleSiteMembershipId,
            Integer percentComplete, Integer sortOrder, boolean clearResponsible, Instant now) {
        if (title != null) {
            this.title = title;
        }
        if (startDate != null) {
            this.startDate = startDate;
        }
        if (endDate != null) {
            this.endDate = endDate;
        }
        if (clearResponsible) {
            this.responsibleSiteMembershipId = null;
        } else if (responsibleSiteMembershipId != null) {
            this.responsibleSiteMembershipId = responsibleSiteMembershipId;
        }
        if (percentComplete != null) {
            this.percentComplete = percentComplete;
        }
        if (sortOrder != null) {
            this.sortOrder = sortOrder;
        }
        this.updatedAt = now;
    }

    public UUID getId() {
        return id;
    }

    public UUID getStageId() {
        return stageId;
    }

    public String getTitle() {
        return title;
    }

    public LocalDate getStartDate() {
        return startDate;
    }

    public LocalDate getEndDate() {
        return endDate;
    }

    public UUID getResponsibleSiteMembershipId() {
        return responsibleSiteMembershipId;
    }

    public int getPercentComplete() {
        return percentComplete;
    }

    public int getSortOrder() {
        return sortOrder;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
