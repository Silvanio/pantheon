package com.pantheon.service.exception;

import java.util.UUID;

/** Thrown when "Concluir" is attempted on a Pedido de Compra that is not (yet) {@code CONFERIDO}. */
public class PurchaseRequestNotConferidoException extends RuntimeException {

    public PurchaseRequestNotConferidoException(UUID purchaseRequestId) {
        super("Purchase request is not Conferido: " + purchaseRequestId);
    }
}
