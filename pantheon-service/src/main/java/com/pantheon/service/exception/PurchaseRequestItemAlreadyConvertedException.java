package com.pantheon.service.exception;

import java.util.UUID;

public class PurchaseRequestItemAlreadyConvertedException extends RuntimeException {

    public PurchaseRequestItemAlreadyConvertedException(UUID itemId) {
        super("Purchase request item is already converted into an Orcamento: " + itemId);
    }
}
