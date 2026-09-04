package com.pantheon.service.exception;

import java.util.UUID;

public class MaterialRequestItemNotFoundException extends RuntimeException {

    public MaterialRequestItemNotFoundException(UUID itemId) {
        super("Material request item not found: " + itemId);
    }
}
