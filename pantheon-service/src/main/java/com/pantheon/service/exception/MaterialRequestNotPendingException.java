package com.pantheon.service.exception;

import java.util.UUID;

public class MaterialRequestNotPendingException extends RuntimeException {

    public MaterialRequestNotPendingException(UUID requestId) {
        super("Material request is not pending: " + requestId);
    }
}
