package com.pantheon.service.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.util.UUID;

/**
 * A per-construction-site permission override, targeting either one specific
 * {@link SiteMembership} ({@code siteMembershipId} set) or every member of a
 * {@link ConstructionFunction} on the site ({@code function} set) — never both. Resolution
 * order (see {@code SitePermissionService}): member override wins, then function override,
 * then a hardcoded default.
 */
@Entity
@Table(name = "site_permission_override")
public class SitePermissionOverride {

    @Id
    private UUID id;

    @Column(name = "construction_site_id", nullable = false)
    private UUID constructionSiteId;

    @Column(name = "site_membership_id")
    private UUID siteMembershipId;

    @Enumerated(EnumType.STRING)
    @Column
    private ConstructionFunction function;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PermissionCapability capability;

    @Enumerated(EnumType.STRING)
    @Column(name = "access_level", nullable = false)
    private AccessLevel accessLevel;

    protected SitePermissionOverride() {
        // JPA
    }

    private SitePermissionOverride(
            UUID id,
            UUID constructionSiteId,
            UUID siteMembershipId,
            ConstructionFunction function,
            PermissionCapability capability,
            AccessLevel accessLevel) {
        this.id = id;
        this.constructionSiteId = constructionSiteId;
        this.siteMembershipId = siteMembershipId;
        this.function = function;
        this.capability = capability;
        this.accessLevel = accessLevel;
    }

    public static SitePermissionOverride forMember(
            UUID id, UUID constructionSiteId, UUID siteMembershipId, PermissionCapability capability,
            AccessLevel accessLevel) {
        return new SitePermissionOverride(id, constructionSiteId, siteMembershipId, null, capability, accessLevel);
    }

    public static SitePermissionOverride forFunction(
            UUID id, UUID constructionSiteId, ConstructionFunction function, PermissionCapability capability,
            AccessLevel accessLevel) {
        return new SitePermissionOverride(id, constructionSiteId, null, function, capability, accessLevel);
    }

    public UUID getId() {
        return id;
    }

    public UUID getConstructionSiteId() {
        return constructionSiteId;
    }

    public UUID getSiteMembershipId() {
        return siteMembershipId;
    }

    public ConstructionFunction getFunction() {
        return function;
    }

    public PermissionCapability getCapability() {
        return capability;
    }

    public AccessLevel getAccessLevel() {
        return accessLevel;
    }
}
