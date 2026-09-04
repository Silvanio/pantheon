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
@Table(name = "material_request")
public class MaterialRequest {

    @Id
    private UUID id;

    @Column(name = "construction_site_id", nullable = false)
    private UUID constructionSiteId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private MaterialRequestStatus status;

    @Column(name = "requested_by", nullable = false)
    private UUID requestedBy;

    @Column(name = "decided_by")
    private UUID decidedBy;

    @Column(name = "decision_note")
    private String decisionNote;

    @Column(name = "decided_at")
    private Instant decidedAt;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected MaterialRequest() {
        // JPA
    }

    public MaterialRequest(UUID id, UUID constructionSiteId, UUID requestedBy, Instant createdAt) {
        this.id = id;
        this.constructionSiteId = constructionSiteId;
        this.status = MaterialRequestStatus.PENDING;
        this.requestedBy = requestedBy;
        this.createdAt = createdAt;
        this.updatedAt = createdAt;
    }

    public void approve(UUID decidedBy, Instant now) {
        this.status = MaterialRequestStatus.APPROVED;
        this.decidedBy = decidedBy;
        this.decidedAt = now;
        this.updatedAt = now;
    }

    public void reject(UUID decidedBy, String reason, Instant now) {
        this.status = MaterialRequestStatus.REJECTED;
        this.decidedBy = decidedBy;
        this.decisionNote = reason;
        this.decidedAt = now;
        this.updatedAt = now;
    }

    public void updateReceiptStatus(boolean fullyReceived, Instant now) {
        this.status = fullyReceived ? MaterialRequestStatus.RECEIVED : MaterialRequestStatus.PARTIALLY_RECEIVED;
        this.updatedAt = now;
    }

    public UUID getId() {
        return id;
    }

    public UUID getConstructionSiteId() {
        return constructionSiteId;
    }

    public MaterialRequestStatus getStatus() {
        return status;
    }

    public UUID getRequestedBy() {
        return requestedBy;
    }

    public UUID getDecidedBy() {
        return decidedBy;
    }

    public String getDecisionNote() {
        return decisionNote;
    }

    public Instant getDecidedAt() {
        return decidedAt;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
