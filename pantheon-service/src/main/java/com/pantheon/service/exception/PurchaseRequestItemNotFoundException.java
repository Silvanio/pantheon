package com.pantheon.service.exception;

import java.util.UUID;

public class PurchaseRequestItemNotFoundException extends RuntimeException {

    public PurchaseRequestItemNotFoundException(UUID itemId) {
        super("Purchase request item not found: " + itemId);
    }
}
