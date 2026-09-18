package com.pantheon.service.exception;

import java.util.UUID;

public class PurchaseRequestInvoiceNotFoundException extends RuntimeException {

    public PurchaseRequestInvoiceNotFoundException(UUID id) {
        super("Purchase request invoice not found: " + id);
    }
}
