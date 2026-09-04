package com.pantheon.service.exception;

import java.util.UUID;

public class EquipmentNotFoundException extends RuntimeException {

    public EquipmentNotFoundException(UUID equipmentId) {
        super("Equipment not found: " + equipmentId);
    }
}
