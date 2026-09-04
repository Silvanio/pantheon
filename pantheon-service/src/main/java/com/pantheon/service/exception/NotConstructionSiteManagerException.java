package com.pantheon.service.exception;

import java.util.UUID;

/**
 * Thrown when a user who is neither the project's ADMIN nor a SITE_FOREMAN attempts an
 * action reserved for those two (e.g. managing an Obra's equipment/material catalog).
 */
public class NotConstructionSiteManagerException extends RuntimeException {

    public NotConstructionSiteManagerException(UUID projectId) {
        super("User is neither an administrator nor a site foreman of project: " + projectId);
    }
}
