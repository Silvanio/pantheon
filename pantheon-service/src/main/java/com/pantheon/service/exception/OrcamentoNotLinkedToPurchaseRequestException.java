package com.pantheon.service.exception;

import java.util.UUID;

/** Thrown when generating a supplier-scoped PDF for an Orcamento that is not linked to the given Pedido de Compra. */
public class OrcamentoNotLinkedToPurchaseRequestException extends RuntimeException {

    public OrcamentoNotLinkedToPurchaseRequestException(UUID orcamentoId, UUID purchaseRequestId) {
        super("Orcamento " + orcamentoId + " is not linked to purchase request " + purchaseRequestId);
    }
}
