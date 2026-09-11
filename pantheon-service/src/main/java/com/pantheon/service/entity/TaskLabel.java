package com.pantheon.service.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

/**
 * A reusable label scoped to one {@link ConstructionSite}, attachable to that obra's
 * {@link TaskCard}s via {@link TaskCardLabel}. Distinct from the global Tasks board's per-obra
 * color, which is computed rather than stored. See {@code obra-tasks-board}.
 */
@Entity
@Table(name = "task_label")
public class TaskLabel {

    @Id
    private UUID id;

    @Column(name = "construction_site_id", nullable = false)
    private UUID constructionSiteId;

    @Column(nullable = false)
    private String name;

    @Column(name = "color_hex", nullable = false)
    private String colorHex;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected TaskLabel() {
        // JPA
    }

    public TaskLabel(UUID id, UUID constructionSiteId, String name, String colorHex, Instant createdAt) {
        this.id = id;
        this.constructionSiteId = constructionSiteId;
        this.name = name;
        this.colorHex = colorHex;
        this.createdAt = createdAt;
    }

    public UUID getId() {
        return id;
    }

    public UUID getConstructionSiteId() {
        return constructionSiteId;
    }

    public String getName() {
        return name;
    }

    public String getColorHex() {
        return colorHex;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
