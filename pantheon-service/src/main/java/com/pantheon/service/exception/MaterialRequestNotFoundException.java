package com.pantheon.service.exception;

import java.util.UUID;

public class MaterialRequestNotFoundException extends RuntimeException {

    public MaterialRequestNotFoundException(UUID requestId) {
        super("Material request not found: " + requestId);
    }
}
