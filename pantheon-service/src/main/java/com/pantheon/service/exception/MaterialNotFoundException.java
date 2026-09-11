package com.pantheon.service.exception;

import java.util.UUID;

public class MaterialNotFoundException extends RuntimeException {

    public MaterialNotFoundException(UUID materialId) {
        super("Material not found: " + materialId);
    }
}
