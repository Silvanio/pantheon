package com.pantheon.service.exception;

import java.util.UUID;

/** Thrown when a Material's delivery status is advanced out of order (e.g. straight to checked without being delivered first). */
public class MaterialDeliveryStatusOrderException extends RuntimeException {

    public MaterialDeliveryStatusOrderException(UUID materialId) {
        super("Material delivery status cannot be advanced out of order: " + materialId);
    }
}
