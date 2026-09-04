package com.pantheon.service.exception;

import java.util.UUID;

public class DuplicateReceiptVerificationException extends RuntimeException {

    public DuplicateReceiptVerificationException(UUID itemId) {
        super("Material request item already has a receipt verification: " + itemId);
    }
}
