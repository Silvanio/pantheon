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
 * One instance of an approval step for one submission cycle of an {@link Orcamento}, snapshotted
 * from that site's {@link SiteOrcamentoApprovalLevel} configuration (or the default) at submit
 * time. Rows from earlier, rejected cycles are kept for history — see
 * {@code orcamento-approval-workflow}'s "Submitting an Orçamento for approval" and "Acting on an
 * approval step".
 */
@Entity
@Table(name = "orcamento_approval")
public class OrcamentoApproval {

    @Id
    private UUID id;

    @Column(name = "orcamento_id", nullable = false)
    private UUID orcamentoId;

    @Column(name = "cycle_number", nullable = false)
    private int cycleNumber;

    @Column(name = "step_order", nullable = false)
    private int stepOrder;

    @Enumerated(EnumType.STRING)
    @Column(name = "approver_function", nullable = false)
    private ConstructionFunction approverFunction;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private OrcamentoApprovalStatus status;

    @Column(name = "decided_by_site_membership_id")
    private UUID decidedBySiteMembershipId;

    @Column(name = "decided_at")
    private Instant decidedAt;

    @Column
    private String comment;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected OrcamentoApproval() {
        // JPA
    }

    public OrcamentoApproval(
            UUID id, UUID orcamentoId, int cycleNumber, int stepOrder, ConstructionFunction approverFunction,
            Instant createdAt) {
        this.id = id;
        this.orcamentoId = orcamentoId;
        this.cycleNumber = cycleNumber;
        this.stepOrder = stepOrder;
        this.approverFunction = approverFunction;
        this.status = OrcamentoApprovalStatus.PENDING;
        this.createdAt = createdAt;
    }

    public void approve(UUID decidedBySiteMembershipId, String comment, Instant now) {
        this.status = OrcamentoApprovalStatus.APPROVED;
        this.decidedBySiteMembershipId = decidedBySiteMembershipId;
        this.comment = comment;
        this.decidedAt = now;
    }

    public void reject(UUID decidedBySiteMembershipId, String comment, Instant now) {
        this.status = OrcamentoApprovalStatus.REJECTED;
        this.decidedBySiteMembershipId = decidedBySiteMembershipId;
        this.comment = comment;
        this.decidedAt = now;
    }

    public UUID getId() {
        return id;
    }

    public UUID getOrcamentoId() {
        return orcamentoId;
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

    public OrcamentoApprovalStatus getStatus() {
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
