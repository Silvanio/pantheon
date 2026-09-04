package com.pantheon.service.exception;

import java.util.UUID;

public class NotProjectMemberException extends RuntimeException {

    public NotProjectMemberException(UUID projectId) {
        super("User is not a member of project: " + projectId);
    }
}
