package com.pantheon.service.exception;

import java.util.UUID;

public class SiteDocumentProjectNotFoundException extends RuntimeException {

    public SiteDocumentProjectNotFoundException(UUID id) {
        super("Site document project not found: " + id);
    }
}
