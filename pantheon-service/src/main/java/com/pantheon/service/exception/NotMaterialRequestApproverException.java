package com.pantheon.service.exception;

import java.util.UUID;

public class NotMaterialRequestApproverException extends RuntimeException {

    public NotMaterialRequestApproverException(UUID projectId) {
        super("User is neither an administrator nor an engineer of project: " + projectId);
    }
}
