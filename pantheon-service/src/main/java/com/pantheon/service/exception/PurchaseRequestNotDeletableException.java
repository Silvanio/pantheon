package com.pantheon.service.exception;

import java.util.UUID;

/** Thrown when deletion is attempted on a Pedido de Compra that has already reached {@code CONCLUIDO}. */
public class PurchaseRequestNotDeletableException extends RuntimeException {

    public PurchaseRequestNotDeletableException(UUID purchaseRequestId) {
        super("Purchase request can no longer be deleted once Concluido: " + purchaseRequestId);
    }
}
