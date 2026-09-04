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
@Table(name = "project")
public class Project {

    @Id
    private UUID id;

    @Column(nullable = false)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column
    private Plan plan;

    @Column(name = "trial_started_at", nullable = false)
    private Instant trialStartedAt;

    @Column(name = "trial_expires_at", nullable = false)
    private Instant trialExpiresAt;

    @Column(name = "plan_confirmed_at")
    private Instant planConfirmedAt;

    @Column(name = "plan_valid_until")
    private Instant planValidUntil;

    @Column(name = "created_by", nullable = false)
    private UUID createdBy;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected Project() {
        // JPA
    }

    public Project(UUID id, String name, UUID createdBy, Instant trialStartedAt, Instant trialExpiresAt) {
        this.id = id;
        this.name = name;
        this.createdBy = createdBy;
        this.trialStartedAt = trialStartedAt;
        this.trialExpiresAt = trialExpiresAt;
        this.createdAt = trialStartedAt;
        this.updatedAt = trialStartedAt;
    }

    public void confirmPlan(Plan plan, Instant confirmedAt, Instant validUntil) {
        this.plan = plan;
        this.planConfirmedAt = confirmedAt;
        this.planValidUntil = validUntil;
        this.updatedAt = confirmedAt;
    }

    /**
     * A project is active while still within its trial window, or — once a plan has been
     * confirmed — while within that plan's 1-year validity window. Derived at read time so
     * no scheduled job is needed to transition state on the clock.
     */
    public boolean isActive(Instant now) {
        if (plan == null) {
            return now.isBefore(trialExpiresAt);
        }
        return planValidUntil != null && now.isBefore(planValidUntil);
    }

    public UUID getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public Plan getPlan() {
        return plan;
    }

    public Instant getTrialStartedAt() {
        return trialStartedAt;
    }

    public Instant getTrialExpiresAt() {
        return trialExpiresAt;
    }

    public Instant getPlanConfirmedAt() {
        return planConfirmedAt;
    }

    public Instant getPlanValidUntil() {
        return planValidUntil;
    }

    public UUID getCreatedBy() {
        return createdBy;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
