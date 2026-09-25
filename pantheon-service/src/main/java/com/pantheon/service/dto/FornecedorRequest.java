package com.pantheon.service.dto;

import com.pantheon.service.entity.FornecedorPaymentMethod;

/** {@code cnpj} (really CPF or CNPJ) and {@code name} are both optional — the frontend validates
 * the document's format when one is entered. */
public record FornecedorRequest(
        String cnpj, String name, String address, String contactName, String contactPhone,
        FornecedorPaymentMethod paymentMethod, String pixKey) {
}
