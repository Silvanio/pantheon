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
 * A company's internal staff — role {@code ADMIN}/{@code MEMBER}, no construction function.
 * Construction-site-scoped team members (client, architect, engineer, site foreman, service
 * provider) are a separate concept: see {@link SiteMembership}.
 */
@Entity
@Table(name = "company_membership")
public class CompanyMembership {

    @Id
    private UUID id;

    @Column(name = "company_id", nullable = false)
    private UUID companyId;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private CompanyRole role;

    /**
     * Membership lifecycle. Nullable so rows created before invitations existed remain
     * valid without a backfill; {@link #getStatus()} reads a null column as
     * {@link MembershipStatus#ACTIVE}.
     */
    @Enumerated(EnumType.STRING)
    @Column
    private MembershipStatus status;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected CompanyMembership() {
        // JPA
    }

    public CompanyMembership(UUID id, UUID companyId, UUID userId, CompanyRole role, Instant createdAt) {
        this(id, companyId, userId, role, MembershipStatus.ACTIVE, createdAt);
    }

    private CompanyMembership(
            UUID id, UUID companyId, UUID userId, CompanyRole role, MembershipStatus status, Instant createdAt) {
        this.id = id;
        this.companyId = companyId;
        this.userId = userId;
        this.role = role;
        this.status = status;
        this.createdAt = createdAt;
    }

    /** A pending staff member: role {@code MEMBER}, status {@code INVITED}. */
    public static CompanyMembership invited(UUID id, UUID companyId, UUID userId, Instant createdAt) {
        return new CompanyMembership(id, companyId, userId, CompanyRole.MEMBER, MembershipStatus.INVITED, createdAt);
    }

    /** Transitions an {@code INVITED} membership to {@code ACTIVE}. Idempotent. */
    public void accept() {
        this.status = MembershipStatus.ACTIVE;
    }

    public UUID getId() {
        return id;
    }

    public UUID getCompanyId() {
        return companyId;
    }

    public UUID getUserId() {
        return userId;
    }

    public CompanyRole getRole() {
        return role;
    }

    /** Reads a null column (legacy rows) as {@link MembershipStatus#ACTIVE}. */
    public MembershipStatus getStatus() {
        return status != null ? status : MembershipStatus.ACTIVE;
    }

    public boolean isActive() {
        return getStatus() == MembershipStatus.ACTIVE;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
