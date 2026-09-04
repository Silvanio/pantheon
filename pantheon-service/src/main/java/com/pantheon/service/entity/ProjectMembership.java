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
@Table(name = "project_membership")
public class ProjectMembership {

    @Id
    private UUID id;

    @Column(name = "project_id", nullable = false)
    private UUID projectId;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ProjectRole role;

    /**
     * Construction-domain function on site (Client, Engineer, Site Foreman, ...), independent
     * of {@link #role}. Nullable so memberships created before this column existed remain
     * valid without a backfill; new memberships always set it, defaulting to OTHER.
     */
    @Enumerated(EnumType.STRING)
    @Column
    private ConstructionFunction function;

    /** Free-text trade, only meaningful when {@link #function} is SERVICE_PROVIDER. */
    @Column
    private String specialty;

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

    protected ProjectMembership() {
        // JPA
    }

    public ProjectMembership(UUID id, UUID projectId, UUID userId, ProjectRole role, Instant createdAt) {
        this(id, projectId, userId, role, null, null, createdAt);
    }

    public ProjectMembership(
            UUID id,
            UUID projectId,
            UUID userId,
            ProjectRole role,
            ConstructionFunction function,
            String specialty,
            Instant createdAt) {
        this(id, projectId, userId, role, function, specialty, MembershipStatus.ACTIVE, createdAt);
    }

    private ProjectMembership(
            UUID id,
            UUID projectId,
            UUID userId,
            ProjectRole role,
            ConstructionFunction function,
            String specialty,
            MembershipStatus status,
            Instant createdAt) {
        this.id = id;
        this.projectId = projectId;
        this.userId = userId;
        this.role = role;
        this.function = function;
        this.specialty = specialty;
        this.status = status;
        this.createdAt = createdAt;
    }

    /**
     * A pending team member: role {@code MEMBER}, status {@code INVITED}. Confers no
     * project access until {@link #accept()} is called on invitation acceptance.
     */
    public static ProjectMembership invited(
            UUID id,
            UUID projectId,
            UUID userId,
            ConstructionFunction function,
            String specialty,
            Instant createdAt) {
        return new ProjectMembership(
                id, projectId, userId, ProjectRole.MEMBER, function, specialty, MembershipStatus.INVITED, createdAt);
    }

    /** Transitions an {@code INVITED} membership to {@code ACTIVE}. Idempotent. */
    public void accept() {
        this.status = MembershipStatus.ACTIVE;
    }

    public UUID getId() {
        return id;
    }

    public UUID getProjectId() {
        return projectId;
    }

    public UUID getUserId() {
        return userId;
    }

    public ProjectRole getRole() {
        return role;
    }

    public ConstructionFunction getFunction() {
        return function;
    }

    public String getSpecialty() {
        return specialty;
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
