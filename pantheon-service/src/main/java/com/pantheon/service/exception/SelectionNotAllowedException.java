package com.pantheon.service.exception;

import java.util.UUID;

/** Thrown when setting/clearing a {@code PurchaseRequestItem}'s selection is attempted while its header is not Orçado. */
public class SelectionNotAllowedException extends RuntimeException {

    public SelectionNotAllowedException(UUID purchaseRequestItemId) {
        super("Purchase request item's header is not Orçado, selection is not allowed: " + purchaseRequestItemId);
    }
}
