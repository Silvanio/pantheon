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
 * One step of a construction site's configured Diário de Obra approval chain: at
 * {@code stepOrder}, a member with function {@code approverFunction} (or any company staff) must
 * approve. A site with no rows here uses the single-{@code ENGINEER} default instead — see
 * {@code daily-report-approval-workflow}'s "Per-site Diário de Obra approval levels". Mirrors
 * {@link SitePurchaseRequestApprovalLevel} field-for-field — a new, parallel entity rather than a
 * shared one (see the change's design.md Decision 1).
 */
@Entity
@Table(name = "site_daily_report_approval_level")
public class SiteDailyReportApprovalLevel {

    @Id
    private UUID id;

    @Column(name = "construction_site_id", nullable = false)
    private UUID constructionSiteId;

    @Column(name = "step_order", nullable = false)
    private int stepOrder;

    @Enumerated(EnumType.STRING)
    @Column(name = "approver_function", nullable = false)
    private ConstructionFunction approverFunction;

    @Column(nullable = false)
    private boolean active;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected SiteDailyReportApprovalLevel() {
        // JPA
    }

    public SiteDailyReportApprovalLevel(
            UUID id, UUID constructionSiteId, int stepOrder, ConstructionFunction approverFunction, Instant now) {
        this.id = id;
        this.constructionSiteId = constructionSiteId;
        this.stepOrder = stepOrder;
        this.approverFunction = approverFunction;
        this.active = true;
        this.createdAt = now;
        this.updatedAt = now;
    }

    public void deactivate(Instant now) {
        this.active = false;
        this.updatedAt = now;
    }

    public UUID getId() {
        return id;
    }

    public UUID getConstructionSiteId() {
        return constructionSiteId;
    }

    public int getStepOrder() {
        return stepOrder;
    }

    public ConstructionFunction getApproverFunction() {
        return approverFunction;
    }

    public boolean isActive() {
        return active;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
