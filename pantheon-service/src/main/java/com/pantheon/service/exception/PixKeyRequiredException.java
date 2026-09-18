package com.pantheon.service.exception;

/** Thrown when registering a new Fornecedor with payment method {@code PIX} and no Pix key. */
public class PixKeyRequiredException extends RuntimeException {

    public PixKeyRequiredException() {
        super("A Pix key is required when the payment method is Pix");
    }
}
