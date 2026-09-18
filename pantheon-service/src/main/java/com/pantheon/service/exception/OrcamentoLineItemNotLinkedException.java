package com.pantheon.service.exception;

import java.util.UUID;

/**
 * Thrown when a selection targets an {@code OrcamentoLineItem} whose Orcamento is not linked to
 * the same Pedido de Compra header as the item being selected, or whose own
 * {@code sourcePurchaseRequestItemId} points at a different item.
 */
public class OrcamentoLineItemNotLinkedException extends RuntimeException {

    public OrcamentoLineItemNotLinkedException(UUID orcamentoLineItemId) {
        super("Orcamento line item is not a valid selection for this purchase request item: " + orcamentoLineItemId);
    }
}
