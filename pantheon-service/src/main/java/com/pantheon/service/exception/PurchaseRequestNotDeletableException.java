package com.pantheon.service.exception;

import java.util.UUID;

/** Thrown when deletion is attempted on a Pedido de Compra that is not (still) {@code INICIADO}. */
public class PurchaseRequestNotDeletableException extends RuntimeException {

    public PurchaseRequestNotDeletableException(UUID purchaseRequestId) {
        super("Purchase request can only be deleted while Iniciado: " + purchaseRequestId);
    }
}
