package com.pantheon.service.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

/**
 * A label attachable to a {@link TaskCard} via {@link TaskCardLabel}, in one of two scopes:
 * <b>predefined</b> ({@code companyId} set, {@code cardId} null) — company-admin-managed, reused
 * across every obra of that company — or <b>custom</b> ({@code cardId} set, {@code companyId}
 * null) — created directly on one card, auto-attached, and never offered to any other card.
 * Exactly one of the two is set. Distinct from the global Tasks board's per-obra color, which is
 * computed rather than stored. See {@code company-task-labels} and {@code obra-tasks-board}.
 */
@Entity
@Table(name = "task_label")
public class TaskLabel {

    @Id
    private UUID id;

    @Column(name = "company_id")
    private UUID companyId;

    @Column(name = "card_id")
    private UUID cardId;

    @Column(nullable = false)
    private String name;

    @Column(name = "color_hex", nullable = false)
    private String colorHex;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected TaskLabel() {
        // JPA
    }

    private TaskLabel(UUID id, UUID companyId, UUID cardId, String name, String colorHex, Instant createdAt) {
        this.id = id;
        this.companyId = companyId;
        this.cardId = cardId;
        this.name = name;
        this.colorHex = colorHex;
        this.createdAt = createdAt;
    }

    public static TaskLabel predefined(UUID id, UUID companyId, String name, String colorHex, Instant createdAt) {
        return new TaskLabel(id, companyId, null, name, colorHex, createdAt);
    }

    public static TaskLabel custom(UUID id, UUID cardId, String name, String colorHex, Instant createdAt) {
        return new TaskLabel(id, null, cardId, name, colorHex, createdAt);
    }

    public UUID getId() {
        return id;
    }

    public UUID getCompanyId() {
        return companyId;
    }

    public UUID getCardId() {
        return cardId;
    }

    public boolean isPredefined() {
        return companyId != null;
    }

    public boolean isCustom() {
        return cardId != null;
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
