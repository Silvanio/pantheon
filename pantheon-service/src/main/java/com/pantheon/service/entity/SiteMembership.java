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
 * A construction site's own team, independent of the company's staff ({@link CompanyMembership}).
 * {@code CLIENT}, {@code ARCHITECT}, {@code ENGINEER}, and {@code SITE_FOREMAN} always require an
 * account and go through the invite/accept lifecycle ({@code status} starts {@code INVITED}).
 * {@code SERVICE_PROVIDER} may have no account at all ({@code userId} null, {@code status} =
 * {@code NONE}, identified by {@code displayName}/{@code contactEmail}) — registration is
 * optional for that function only.
 */
@Entity
@Table(name = "site_membership")
public class SiteMembership {

    @Id
    private UUID id;

    @Column(name = "construction_site_id", nullable = false)
    private UUID constructionSiteId;

    @Column(name = "user_id")
    private UUID userId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ConstructionFunction function;

    /** Free-text trade, only meaningful when {@link #function} is SERVICE_PROVIDER. */
    @Column(name = "service_provider_trade")
    private String serviceProviderTrade;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private MembershipStatus status;

    /** Only meaningful when {@link #function} is CLIENT. */
    @Column(name = "client_cpf")
    private String clientCpf;

    /** Used in place of a linked {@link AppUser} when {@link #userId} is null. */
    @Column(name = "display_name")
    private String displayName;

    @Column(name = "contact_email")
    private String contactEmail;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected SiteMembership() {
        // JPA
    }

    private SiteMembership(
            UUID id,
            UUID constructionSiteId,
            UUID userId,
            ConstructionFunction function,
            String serviceProviderTrade,
            MembershipStatus status,
            String clientCpf,
            String displayName,
            String contactEmail,
            Instant createdAt) {
        this.id = id;
        this.constructionSiteId = constructionSiteId;
        this.userId = userId;
        this.function = function;
        this.serviceProviderTrade = serviceProviderTrade;
        this.status = status;
        this.clientCpf = clientCpf;
        this.displayName = displayName;
        this.contactEmail = contactEmail;
        this.createdAt = createdAt;
    }

    /** A client, architect, engineer, or site foreman invited by email — always {@code INVITED} until accepted. */
    public static SiteMembership invited(
            UUID id,
            UUID constructionSiteId,
            UUID userId,
            ConstructionFunction function,
            String clientCpf,
            Instant createdAt) {
        return new SiteMembership(
                id, constructionSiteId, userId, function, null, MembershipStatus.INVITED, clientCpf, null, null,
                createdAt);
    }

    /** A service-provider member with no account: immediately usable, no invitation involved. */
    public static SiteMembership accountless(
            UUID id,
            UUID constructionSiteId,
            String trade,
            String displayName,
            String contactEmail,
            Instant createdAt) {
        return new SiteMembership(
                id, constructionSiteId, null, ConstructionFunction.SERVICE_PROVIDER, trade, MembershipStatus.NONE,
                null, displayName, contactEmail, createdAt);
    }

    /** Transitions an {@code INVITED} membership to {@code ACTIVE}. Idempotent. */
    public void accept() {
        this.status = MembershipStatus.ACTIVE;
    }

    /** Attaches a real account to a previously accountless service-provider membership, starting its invite. */
    public void attachAccount(UUID userId) {
        this.userId = userId;
        this.status = MembershipStatus.INVITED;
    }

    public UUID getId() {
        return id;
    }

    public UUID getConstructionSiteId() {
        return constructionSiteId;
    }

    public UUID getUserId() {
        return userId;
    }

    public ConstructionFunction getFunction() {
        return function;
    }

    public String getServiceProviderTrade() {
        return serviceProviderTrade;
    }

    public MembershipStatus getStatus() {
        return status;
    }

    public boolean isActive() {
        return status == MembershipStatus.ACTIVE || status == MembershipStatus.NONE;
    }

    public String getClientCpf() {
        return clientCpf;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getContactEmail() {
        return contactEmail;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
