package com.pantheon.service.exception;

import java.util.UUID;

/** Thrown when adding or removing items is attempted on a Pedido de Compra that is no longer {@code INICIADO}. */
public class PurchaseRequestNotIniciadoException extends RuntimeException {

    public PurchaseRequestNotIniciadoException(UUID purchaseRequestId) {
        super("Purchase request is not Iniciado: " + purchaseRequestId);
    }
}
