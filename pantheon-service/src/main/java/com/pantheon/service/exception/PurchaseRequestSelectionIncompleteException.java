package com.pantheon.service.exception;

import java.util.UUID;

/** Thrown when submitting for approval is attempted while at least one item has no selected Orcamento line item. */
public class PurchaseRequestSelectionIncompleteException extends RuntimeException {

    public PurchaseRequestSelectionIncompleteException(UUID purchaseRequestId) {
        super("Purchase request has at least one item with no selected supplier: " + purchaseRequestId);
    }
}
