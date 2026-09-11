package com.pantheon.service.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

/**
 * A Tasks board card, scoped to one {@link ConstructionSite}. Placed into a {@link TaskColumn}
 * shared by that site's company — see {@code obra-tasks-board}.
 */
@Entity
@Table(name = "task_card")
public class TaskCard {

    @Id
    private UUID id;

    @Column(name = "construction_site_id", nullable = false)
    private UUID constructionSiteId;

    @Column(name = "column_id", nullable = false)
    private UUID columnId;

    @Column(nullable = false)
    private String title;

    @Column
    private String description;

    @Column(name = "sort_order", nullable = false)
    private int sortOrder;

    @Column(name = "created_by", nullable = false)
    private UUID createdBy;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected TaskCard() {
        // JPA
    }

    public TaskCard(
            UUID id, UUID constructionSiteId, UUID columnId, String title, String description, int sortOrder,
            UUID createdBy, Instant createdAt, Instant updatedAt) {
        this.id = id;
        this.constructionSiteId = constructionSiteId;
        this.columnId = columnId;
        this.title = title;
        this.description = description;
        this.sortOrder = sortOrder;
        this.createdBy = createdBy;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public void moveTo(UUID columnId, int sortOrder, Instant now) {
        this.columnId = columnId;
        this.sortOrder = sortOrder;
        this.updatedAt = now;
    }

    public UUID getId() {
        return id;
    }

    public UUID getConstructionSiteId() {
        return constructionSiteId;
    }

    public UUID getColumnId() {
        return columnId;
    }

    public String getTitle() {
        return title;
    }

    public String getDescription() {
        return description;
    }

    public int getSortOrder() {
        return sortOrder;
    }

    public UUID getCreatedBy() {
        return createdBy;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
