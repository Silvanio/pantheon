package com.pantheon.service.exception;

import java.util.UUID;

/** Thrown when a user has no access to a construction site — neither company staff nor an active {@code SiteMembership}. */
public class NotSiteMemberException extends RuntimeException {

    public NotSiteMemberException(UUID constructionSiteId) {
        super("User has no access to construction site: " + constructionSiteId);
    }
}
