package com.pantheon.service.exception;

import java.util.UUID;

/** Thrown when a non-superadmin user attempts a platform-admin-only action. */
public class NotSuperAdminException extends RuntimeException {

    public NotSuperAdminException(UUID userId) {
        super("User is not a superadmin: " + userId);
    }
}
