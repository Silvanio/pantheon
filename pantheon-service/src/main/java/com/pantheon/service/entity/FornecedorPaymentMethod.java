package com.pantheon.service.entity;

/** How a {@link Fornecedor} is to be paid. Only {@code PIX} carries a {@code pixKey}. */
public enum FornecedorPaymentMethod {
    CARTAO,
    BOLETO,
    PIX,
    DINHEIRO
}
