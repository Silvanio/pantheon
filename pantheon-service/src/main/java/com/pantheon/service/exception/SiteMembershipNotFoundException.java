package com.pantheon.service.exception;

import java.util.UUID;

/** Thrown when a referenced {@code SiteMembership} does not exist, or does not belong to the expected site. */
public class SiteMembershipNotFoundException extends RuntimeException {

    public SiteMembershipNotFoundException(UUID siteMembershipId) {
        super("Site membership not found: " + siteMembershipId);
    }
}
