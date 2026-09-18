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
 * A "Pedido de Compra" document: a named, dated header grouping one or more
 * {@link PurchaseRequestItem}s entered together. The name is generated at creation time (see
 * {@code purchase-requests}' header-creation requirement) and never changes. A single header may
 * spawn more than one {@link Orcamento} over time, as its items are converted in separate
 * batches, and is itself the unit of approval/conclusion — see
 * {@code purchase-request-approval-workflow}.
 */
@Entity
@Table(name = "purchase_request")
public class PurchaseRequest {

    @Id
    private UUID id;

    @Column(name = "construction_site_id", nullable = false)
    private UUID constructionSiteId;

    @Column(nullable = false)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PurchaseRequestStatus status;

    @Column(name = "created_by", nullable = false)
    private UUID createdBy;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "submitted_at")
    private Instant submittedAt;

    @Column(name = "approved_at")
    private Instant approvedAt;

    @Column(name = "completed_at")
    private Instant completedAt;

    @Column(name = "current_approval_cycle", nullable = false)
    private int currentApprovalCycle;

    @Column(name = "last_rejection_reason")
    private String lastRejectionReason;

    protected PurchaseRequest() {
        // JPA
    }

    public PurchaseRequest(UUID id, UUID constructionSiteId, String name, UUID createdBy, Instant createdAt) {
        this.id = id;
        this.constructionSiteId = constructionSiteId;
        this.name = name;
        this.status = PurchaseRequestStatus.INICIADO;
        this.createdBy = createdBy;
        this.createdAt = createdAt;
        this.currentApprovalCycle = 0;
    }

    /** The first Orcamento created for this header moves it INICIADO -> ORCADO; later ones don't re-trigger this. */
    public void markOrcado() {
        this.status = PurchaseRequestStatus.ORCADO;
    }

    /** Undoes {@link #markOrcado}, called when the header's last remaining linked Orcamento is deleted. */
    public void revertToIniciado() {
        this.status = PurchaseRequestStatus.INICIADO;
    }

    /** Starts a new approval cycle. Stays ORCADO until the cycle's final step is approved. */
    public void submitForApproval(Instant now) {
        this.submittedAt = now;
        this.currentApprovalCycle += 1;
    }

    public void approve(Instant now) {
        this.status = PurchaseRequestStatus.CONFERIDO;
        this.approvedAt = now;
    }

    /** A rejection at any approval step bounces the header back to ORCADO for revision. */
    public void returnToOrcadoAfterRejection(String reason) {
        this.status = PurchaseRequestStatus.ORCADO;
        this.lastRejectionReason = reason;
    }

    public void complete(Instant now) {
        this.status = PurchaseRequestStatus.CONCLUIDO;
        this.completedAt = now;
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

    public PurchaseRequestStatus getStatus() {
        return status;
    }

    public UUID getCreatedBy() {
        return createdBy;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getSubmittedAt() {
        return submittedAt;
    }

    public Instant getApprovedAt() {
        return approvedAt;
    }

    public Instant getCompletedAt() {
        return completedAt;
    }

    public int getCurrentApprovalCycle() {
        return currentApprovalCycle;
    }

    public String getLastRejectionReason() {
        return lastRejectionReason;
    }
}
