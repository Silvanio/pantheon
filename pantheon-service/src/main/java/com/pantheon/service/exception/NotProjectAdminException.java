package com.pantheon.service.exception;

import java.util.UUID;

public class NotProjectAdminException extends RuntimeException {

    public NotProjectAdminException(UUID projectId) {
        super("User is not an administrator of project: " + projectId);
    }
}
