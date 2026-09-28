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
 * One instance of an approval step for one submission cycle of a {@link DailyReport},
 * snapshotted from that site's {@link SiteDailyReportApprovalLevel} configuration (or the
 * default) at submit time. Rows from earlier, rejected cycles are kept for history — see
 * {@code daily-report-approval-workflow}'s "Per-site Diário de Obra approval levels" and "Acting
 * on a Diário de Obra approval step". Mirrors {@link PurchaseRequestApproval} field-for-field —
 * a new, parallel entity rather than a shared one (see the change's design.md Decision 1).
 */
@Entity
@Table(name = "daily_report_approval")
public class DailyReportApproval {

    @Id
    private UUID id;

    @Column(name = "daily_report_id", nullable = false)
    private UUID dailyReportId;

    @Column(name = "cycle_number", nullable = false)
    private int cycleNumber;

    @Column(name = "step_order", nullable = false)
    private int stepOrder;

    @Enumerated(EnumType.STRING)
    @Column(name = "approver_function", nullable = false)
    private ConstructionFunction approverFunction;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private DailyReportApprovalStatus status;

    @Column(name = "decided_by_site_membership_id")
    private UUID decidedBySiteMembershipId;

    @Column(name = "decided_at")
    private Instant decidedAt;

    @Column
    private String comment;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected DailyReportApproval() {
        // JPA
    }

    public DailyReportApproval(
            UUID id, UUID dailyReportId, int cycleNumber, int stepOrder, ConstructionFunction approverFunction,
            Instant createdAt) {
        this.id = id;
        this.dailyReportId = dailyReportId;
        this.cycleNumber = cycleNumber;
        this.stepOrder = stepOrder;
        this.approverFunction = approverFunction;
        this.status = DailyReportApprovalStatus.PENDING;
        this.createdAt = createdAt;
    }

    public void approve(UUID decidedBySiteMembershipId, String comment, Instant now) {
        this.status = DailyReportApprovalStatus.APPROVED;
        this.decidedBySiteMembershipId = decidedBySiteMembershipId;
        this.comment = comment;
        this.decidedAt = now;
    }

    public void reject(UUID decidedBySiteMembershipId, String comment, Instant now) {
        this.status = DailyReportApprovalStatus.REJECTED;
        this.decidedBySiteMembershipId = decidedBySiteMembershipId;
        this.comment = comment;
        this.decidedAt = now;
    }

    public UUID getId() {
        return id;
    }

    public UUID getDailyReportId() {
        return dailyReportId;
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

    public DailyReportApprovalStatus getStatus() {
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
