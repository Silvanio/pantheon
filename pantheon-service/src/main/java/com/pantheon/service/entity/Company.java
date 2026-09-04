package com.pantheon.service.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

/**
 * The paying tenant. Replaces the former {@code Project} entity: a construction site ("obra")
 * now belongs directly to a {@code Company}, which is created name-only and then must select a
 * {@link Plan} and complete its commercial profile before it is usable (see
 * {@link #getOnboardingStatus()}).
 */
@Entity
@Table(name = "company")
public class Company {

    @Id
    private UUID id;

    @Column(nullable = false)
    private String name;

    @Column(name = "plan_id")
    private UUID planId;

    @Column(name = "legal_name")
    private String legalName;

    @Column(name = "trade_name")
    private String tradeName;

    @Column
    private String cnpj;

    @Column
    private String address;

    @Column(name = "logo_object_key")
    private String logoObjectKey;

    @Column(name = "created_by", nullable = false)
    private UUID createdBy;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected Company() {
        // JPA
    }

    public Company(UUID id, String name, UUID createdBy, Instant createdAt) {
        this.id = id;
        this.name = name;
        this.createdBy = createdBy;
        this.createdAt = createdAt;
        this.updatedAt = createdAt;
    }

    public void selectPlan(UUID planId, Instant now) {
        this.planId = planId;
        this.updatedAt = now;
    }

    public void completeProfile(
            String legalName, String tradeName, String cnpj, String address, String logoObjectKey, Instant now) {
        this.legalName = legalName;
        this.tradeName = tradeName;
        this.cnpj = cnpj;
        this.address = address;
        this.logoObjectKey = logoObjectKey;
        this.updatedAt = now;
    }

    private boolean hasProfile() {
        return legalName != null && tradeName != null && cnpj != null && address != null && logoObjectKey != null;
    }

    /** Derived, not stored, so it can never drift from the underlying plan/profile data. */
    public CompanyOnboardingStatus getOnboardingStatus() {
        if (planId == null) {
            return CompanyOnboardingStatus.PLAN_PENDING;
        }
        if (!hasProfile()) {
            return CompanyOnboardingStatus.PROFILE_PENDING;
        }
        return CompanyOnboardingStatus.COMPLETE;
    }

    public UUID getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public UUID getPlanId() {
        return planId;
    }

    public String getLegalName() {
        return legalName;
    }

    public String getTradeName() {
        return tradeName;
    }

    public String getCnpj() {
        return cnpj;
    }

    public String getAddress() {
        return address;
    }

    public String getLogoObjectKey() {
        return logoObjectKey;
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
