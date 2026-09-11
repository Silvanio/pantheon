package com.pantheon.service.exception;

/** Thrown when a conversion selection includes items from more than one Pedido de Compra. */
public class ItemsSpanMultiplePurchaseRequestsException extends RuntimeException {

    public ItemsSpanMultiplePurchaseRequestsException() {
        super("Selected items must all belong to the same Pedido de Compra");
    }
}
