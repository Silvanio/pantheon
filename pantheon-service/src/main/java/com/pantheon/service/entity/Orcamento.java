package com.pantheon.service.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

/**
 * A client-facing budget/quote drafted against a {@link MaterialRequest}. A request may have
 * more than one over time (e.g., a revised quote after a rejection).
 */
@Entity
@Table(name = "orcamento")
public class Orcamento {

    @Id
    private UUID id;

    @Column(name = "material_request_id", nullable = false)
    private UUID materialRequestId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private OrcamentoStatus status;

    @Column(name = "created_by", nullable = false)
    private UUID createdBy;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "sent_at")
    private Instant sentAt;

    @Column(name = "decided_at")
    private Instant decidedAt;

    @Column(name = "rejection_reason")
    private String rejectionReason;

    protected Orcamento() {
        // JPA
    }

    public Orcamento(UUID id, UUID materialRequestId, UUID createdBy, Instant createdAt) {
        this.id = id;
        this.materialRequestId = materialRequestId;
        this.status = OrcamentoStatus.DRAFT;
        this.createdBy = createdBy;
        this.createdAt = createdAt;
    }

    public void send(Instant now) {
        this.status = OrcamentoStatus.SENT;
        this.sentAt = now;
    }

    public void approve(Instant now) {
        this.status = OrcamentoStatus.APPROVED;
        this.decidedAt = now;
    }

    public void reject(String reason, Instant now) {
        this.status = OrcamentoStatus.REJECTED;
        this.rejectionReason = reason;
        this.decidedAt = now;
    }

    public UUID getId() {
        return id;
    }

    public UUID getMaterialRequestId() {
        return materialRequestId;
    }

    public OrcamentoStatus getStatus() {
        return status;
    }

    public UUID getCreatedBy() {
        return createdBy;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getSentAt() {
        return sentAt;
    }

    public Instant getDecidedAt() {
        return decidedAt;
    }

    public String getRejectionReason() {
        return rejectionReason;
    }
}
