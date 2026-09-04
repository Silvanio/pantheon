package com.pantheon.service.exception;

import java.util.UUID;

/** Thrown when a user who is not the construction site's client attempts a client-only decision (e.g. deciding an orcamento). */
public class NotSiteClientException extends RuntimeException {

    public NotSiteClientException(UUID constructionSiteId) {
        super("User is not the client of construction site: " + constructionSiteId);
    }
}
