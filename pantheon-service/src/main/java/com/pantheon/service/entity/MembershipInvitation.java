package com.pantheon.service.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

/**
 * One-to-one with an {@code INVITED} {@link ProjectMembership}. Holds the transient
 * invitation metadata — only a hash of the secret token is stored; the raw token travels
 * solely in the invitation email. {@code requiresRegistration} is true when the invited
 * email had no account and a pre-registration one was created for it.
 */
@Entity
@Table(name = "membership_invitation")
public class MembershipInvitation {

    @Id
    private UUID id;

    @Column(name = "membership_id", nullable = false)
    private UUID membershipId;

    @Column(nullable = false)
    private String email;

    @Column(name = "token_hash", nullable = false)
    private String tokenHash;

    @Column(name = "invited_by", nullable = false)
    private UUID invitedBy;

    @Column(name = "requires_registration", nullable = false)
    private boolean requiresRegistration;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    @Column(name = "accepted_at")
    private Instant acceptedAt;

    protected MembershipInvitation() {
        // JPA
    }

    public MembershipInvitation(
            UUID id,
            UUID membershipId,
            String email,
            String tokenHash,
            UUID invitedBy,
            boolean requiresRegistration,
            Instant createdAt,
            Instant expiresAt) {
        this.id = id;
        this.membershipId = membershipId;
        this.email = email;
        this.tokenHash = tokenHash;
        this.invitedBy = invitedBy;
        this.requiresRegistration = requiresRegistration;
        this.createdAt = createdAt;
        this.expiresAt = expiresAt;
    }

    public boolean isExpired(Instant now) {
        return now.isAfter(expiresAt);
    }

    public boolean isAccepted() {
        return acceptedAt != null;
    }

    public void markAccepted(Instant when) {
        if (this.acceptedAt == null) {
            this.acceptedAt = when;
        }
    }

    /** Re-issues the invitation with a fresh token and expiry (admin re-adds an invited email). */
    public void reissue(String tokenHash, Instant now, Instant expiresAt) {
        this.tokenHash = tokenHash;
        this.createdAt = now;
        this.expiresAt = expiresAt;
        this.acceptedAt = null;
    }

    public UUID getId() {
        return id;
    }

    public UUID getMembershipId() {
        return membershipId;
    }

    public String getEmail() {
        return email;
    }

    public String getTokenHash() {
        return tokenHash;
    }

    public UUID getInvitedBy() {
        return invitedBy;
    }

    public boolean isRequiresRegistration() {
        return requiresRegistration;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getExpiresAt() {
        return expiresAt;
    }

    public Instant getAcceptedAt() {
        return acceptedAt;
    }
}
