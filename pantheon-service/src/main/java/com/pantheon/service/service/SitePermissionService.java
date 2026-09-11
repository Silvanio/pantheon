package com.pantheon.service.service;

import com.pantheon.service.entity.AccessLevel;
import com.pantheon.service.entity.ConstructionFunction;
import com.pantheon.service.entity.PermissionCapability;
import com.pantheon.service.entity.SitePermissionOverride;
import com.pantheon.service.exception.ForbiddenCapabilityException;
import com.pantheon.service.repository.SitePermissionOverrideRepository;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.stereotype.Service;

/**
 * Resolves a site member's effective {@link AccessLevel} for a {@link PermissionCapability}:
 * a member-specific override wins, then a function-level override, then the hardcoded default
 * below (matching the pre-permission-configuration role rules, so a freshly created obra needs
 * no configuration to behave correctly). {@code PURCHASE_REQUEST}/{@code ORCAMENTO_MANAGE}
 * have no real "view" mode - {@link AccessLevel#MANAGE} means granted, {@link AccessLevel#VIEW}
 * means denied. Orcamento approval-step authority is resolved separately, from that site's
 * {@code SiteOrcamentoApprovalLevel} configuration, not from a capability here.
 */
@Service
public class SitePermissionService {

    private static final Map<ConstructionFunction, Map<PermissionCapability, AccessLevel>> DEFAULTS =
            new EnumMap<>(ConstructionFunction.class);

    static {
        DEFAULTS.put(ConstructionFunction.ENGINEER, Map.of(
                PermissionCapability.DOCUMENT_PROJECTS, AccessLevel.MANAGE,
                PermissionCapability.DAILY_REPORT, AccessLevel.MANAGE,
                PermissionCapability.EQUIPMENT, AccessLevel.VIEW,
                PermissionCapability.PURCHASE_REQUEST, AccessLevel.MANAGE,
                PermissionCapability.ORCAMENTO_MANAGE, AccessLevel.MANAGE));
        DEFAULTS.put(ConstructionFunction.ARCHITECT, Map.of(
                PermissionCapability.DOCUMENT_PROJECTS, AccessLevel.MANAGE,
                PermissionCapability.DAILY_REPORT, AccessLevel.MANAGE,
                PermissionCapability.EQUIPMENT, AccessLevel.VIEW,
                PermissionCapability.PURCHASE_REQUEST, AccessLevel.MANAGE,
                PermissionCapability.ORCAMENTO_MANAGE, AccessLevel.MANAGE));
        DEFAULTS.put(ConstructionFunction.SITE_FOREMAN, Map.of(
                PermissionCapability.DOCUMENT_PROJECTS, AccessLevel.VIEW,
                PermissionCapability.DAILY_REPORT, AccessLevel.MANAGE,
                PermissionCapability.EQUIPMENT, AccessLevel.MANAGE,
                PermissionCapability.PURCHASE_REQUEST, AccessLevel.VIEW,
                PermissionCapability.ORCAMENTO_MANAGE, AccessLevel.VIEW));
        DEFAULTS.put(ConstructionFunction.CLIENT, Map.of(
                PermissionCapability.DOCUMENT_PROJECTS, AccessLevel.VIEW,
                PermissionCapability.DAILY_REPORT, AccessLevel.VIEW,
                PermissionCapability.EQUIPMENT, AccessLevel.VIEW,
                PermissionCapability.PURCHASE_REQUEST, AccessLevel.VIEW,
                PermissionCapability.ORCAMENTO_MANAGE, AccessLevel.VIEW));
        DEFAULTS.put(ConstructionFunction.SERVICE_PROVIDER, Map.of(
                PermissionCapability.DOCUMENT_PROJECTS, AccessLevel.VIEW,
                PermissionCapability.DAILY_REPORT, AccessLevel.VIEW,
                PermissionCapability.EQUIPMENT, AccessLevel.VIEW,
                PermissionCapability.PURCHASE_REQUEST, AccessLevel.VIEW,
                PermissionCapability.ORCAMENTO_MANAGE, AccessLevel.VIEW));
    }

    private final SitePermissionOverrideRepository overrideRepository;

    public SitePermissionService(SitePermissionOverrideRepository overrideRepository) {
        this.overrideRepository = overrideRepository;
    }

    public AccessLevel resolve(UUID constructionSiteId, SiteAccessContext access, PermissionCapability capability) {
        if (access.companyStaff()) {
            return AccessLevel.MANAGE;
        }

        UUID membershipId = access.siteMembership().getId();
        var memberOverride = overrideRepository.findBySiteMembershipIdAndCapability(membershipId, capability);
        if (memberOverride.isPresent()) {
            return memberOverride.get().getAccessLevel();
        }

        ConstructionFunction function = access.function();
        var functionOverride =
                overrideRepository.findByConstructionSiteIdAndFunctionAndCapability(constructionSiteId, function, capability);
        if (functionOverride.isPresent()) {
            return functionOverride.get().getAccessLevel();
        }

        return DEFAULTS.getOrDefault(function, Map.of()).getOrDefault(capability, AccessLevel.VIEW);
    }

    public boolean canManage(UUID constructionSiteId, SiteAccessContext access, PermissionCapability capability) {
        return resolve(constructionSiteId, access, capability) == AccessLevel.MANAGE;
    }

    public void requireManage(UUID constructionSiteId, SiteAccessContext access, PermissionCapability capability) {
        if (!canManage(constructionSiteId, access, capability)) {
            throw new ForbiddenCapabilityException(constructionSiteId, capability);
        }
    }

    public List<SitePermissionOverride> listOverrides(UUID constructionSiteId) {
        return overrideRepository.findByConstructionSiteId(constructionSiteId);
    }

    /** Sets (creating or replacing) the function-level default for a capability on a site. */
    public SitePermissionOverride setFunctionOverride(
            UUID constructionSiteId, ConstructionFunction function, PermissionCapability capability, AccessLevel accessLevel) {
        SitePermissionOverride existing = overrideRepository
                .findByConstructionSiteIdAndFunctionAndCapability(constructionSiteId, function, capability)
                .orElse(null);
        UUID id = existing != null ? existing.getId() : UUID.randomUUID();
        return overrideRepository.save(
                SitePermissionOverride.forFunction(id, constructionSiteId, function, capability, accessLevel));
    }

    /** Sets (creating or replacing) a member-specific override for a capability. */
    public SitePermissionOverride setMemberOverride(
            UUID constructionSiteId, UUID siteMembershipId, PermissionCapability capability, AccessLevel accessLevel) {
        SitePermissionOverride existing =
                overrideRepository.findBySiteMembershipIdAndCapability(siteMembershipId, capability).orElse(null);
        UUID id = existing != null ? existing.getId() : UUID.randomUUID();
        return overrideRepository.save(
                SitePermissionOverride.forMember(id, constructionSiteId, siteMembershipId, capability, accessLevel));
    }
}
