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
 * One instance of an approval step for one submission cycle of a {@link PurchaseRequest},
 * snapshotted from that site's {@link SitePurchaseRequestApprovalLevel} configuration (or the
 * default) at submit time. Rows from earlier, rejected cycles are kept for history — see
 * {@code purchase-request-approval-workflow}'s "Submitting a Pedido de Compra for approval" and
 * "Acting on an approval step".
 */
@Entity
@Table(name = "purchase_request_approval")
public class PurchaseRequestApproval {

    @Id
    private UUID id;

    @Column(name = "purchase_request_id", nullable = false)
    private UUID purchaseRequestId;

    @Column(name = "cycle_number", nullable = false)
    private int cycleNumber;

    @Column(name = "step_order", nullable = false)
    private int stepOrder;

    @Enumerated(EnumType.STRING)
    @Column(name = "approver_function", nullable = false)
    private ConstructionFunction approverFunction;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PurchaseRequestApprovalStatus status;

    @Column(name = "decided_by_site_membership_id")
    private UUID decidedBySiteMembershipId;

    @Column(name = "decided_at")
    private Instant decidedAt;

    @Column
    private String comment;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected PurchaseRequestApproval() {
        // JPA
    }

    public PurchaseRequestApproval(
            UUID id, UUID purchaseRequestId, int cycleNumber, int stepOrder, ConstructionFunction approverFunction,
            Instant createdAt) {
        this.id = id;
        this.purchaseRequestId = purchaseRequestId;
        this.cycleNumber = cycleNumber;
        this.stepOrder = stepOrder;
        this.approverFunction = approverFunction;
        this.status = PurchaseRequestApprovalStatus.PENDING;
        this.createdAt = createdAt;
    }

    public void approve(UUID decidedBySiteMembershipId, String comment, Instant now) {
        this.status = PurchaseRequestApprovalStatus.APPROVED;
        this.decidedBySiteMembershipId = decidedBySiteMembershipId;
        this.comment = comment;
        this.decidedAt = now;
    }

    public void reject(UUID decidedBySiteMembershipId, String comment, Instant now) {
        this.status = PurchaseRequestApprovalStatus.REJECTED;
        this.decidedBySiteMembershipId = decidedBySiteMembershipId;
        this.comment = comment;
        this.decidedAt = now;
    }

    public UUID getId() {
        return id;
    }

    public UUID getPurchaseRequestId() {
        return purchaseRequestId;
    }

    public int getCycleNumber() {
        return cycleNumber;
    }

    public int getStepOrder() {
        return stepOrder;
    }

    public ConstructionFunction getApproverFunction() {
        return approverFunction;
    }

    public PurchaseRequestApprovalStatus getStatus() {
        return status;
    }

    public UUID getDecidedBySiteMembershipId() {
        return decidedBySiteMembershipId;
    }

    public Instant getDecidedAt() {
        return decidedAt;
    }

    public String getComment() {
        return comment;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
