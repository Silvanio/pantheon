package com.pantheon.service.exception;

import java.util.UUID;

public class MaterialRequestNotApprovedException extends RuntimeException {

    public MaterialRequestNotApprovedException(UUID requestId) {
        super("Material request is not approved (or receipt-tracking): " + requestId);
    }
}
