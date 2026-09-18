package com.pantheon.service.exception;

import java.util.UUID;

/** Thrown when submitting for approval is attempted on a Pedido de Compra that is not (yet) {@code ORCADO}. */
public class PurchaseRequestNotOrcadoException extends RuntimeException {

    public PurchaseRequestNotOrcadoException(UUID purchaseRequestId) {
        super("Purchase request is not Orçado: " + purchaseRequestId);
    }
}
