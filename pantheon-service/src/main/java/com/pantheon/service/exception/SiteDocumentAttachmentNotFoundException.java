package com.pantheon.service.exception;

import java.util.UUID;

public class SiteDocumentAttachmentNotFoundException extends RuntimeException {

    public SiteDocumentAttachmentNotFoundException(UUID id) {
        super("Site document attachment not found: " + id);
    }
}
