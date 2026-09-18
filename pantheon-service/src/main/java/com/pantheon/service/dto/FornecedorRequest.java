package com.pantheon.service.dto;

import com.pantheon.service.entity.FornecedorPaymentMethod;
import jakarta.validation.constraints.NotBlank;

public record FornecedorRequest(
        @NotBlank String cnpj, @NotBlank String name, String address, String contactName, String contactPhone,
        FornecedorPaymentMethod paymentMethod, String pixKey) {
}
