package com.pantheon.service.exception;

import java.util.UUID;

public class ConstructionSiteNotFoundException extends RuntimeException {

    public ConstructionSiteNotFoundException(UUID siteId) {
        super("Construction site not found: " + siteId);
    }
}
