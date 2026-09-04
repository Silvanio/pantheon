package com.pantheon.service.dto;

import com.pantheon.service.entity.AccessLevel;
import com.pantheon.service.entity.ConstructionFunction;
import com.pantheon.service.entity.PermissionCapability;
import com.pantheon.service.entity.SitePermissionOverride;
import java.util.UUID;

public record SitePermissionOverrideResponse(
        UUID id, UUID siteMembershipId, ConstructionFunction function, PermissionCapability capability,
        AccessLevel accessLevel) {

    public static SitePermissionOverrideResponse from(SitePermissionOverride override) {
        return new SitePermissionOverrideResponse(
                override.getId(), override.getSiteMembershipId(), override.getFunction(), override.getCapability(),
                override.getAccessLevel());
    }
}
