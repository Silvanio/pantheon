package com.pantheon.service.exception;

import com.pantheon.service.entity.PermissionCapability;
import java.util.UUID;

/** Thrown when a site member's resolved access level for a capability is insufficient for the attempted action. */
public class ForbiddenCapabilityException extends RuntimeException {

    public ForbiddenCapabilityException(UUID constructionSiteId, PermissionCapability capability) {
        super("Insufficient " + capability + " access on construction site: " + constructionSiteId);
    }
}
