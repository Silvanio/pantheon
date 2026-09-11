package com.pantheon.service.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

/**
 * Assigns one {@link SiteMembership} to one {@link TaskCard}. Keyed by membership rather than
 * {@code userId} since a membership can belong to a service-provider member with no app user
 * account. See {@code obra-tasks-board}.
 */
@Entity
@Table(name = "task_card_assignee")
public class TaskCardAssignee {

    @Id
    private UUID id;

    @Column(name = "card_id", nullable = false)
    private UUID cardId;

    @Column(name = "site_membership_id", nullable = false)
    private UUID siteMembershipId;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected TaskCardAssignee() {
        // JPA
    }

    public TaskCardAssignee(UUID id, UUID cardId, UUID siteMembershipId, Instant createdAt) {
        this.id = id;
        this.cardId = cardId;
        this.siteMembershipId = siteMembershipId;
        this.createdAt = createdAt;
    }

    public UUID getId() {
        return id;
    }

    public UUID getCardId() {
        return cardId;
    }

    public UUID getSiteMembershipId() {
        return siteMembershipId;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
