package com.pantheon.service.dto;

import com.pantheon.service.entity.Fornecedor;
import com.pantheon.service.entity.FornecedorPaymentMethod;
import java.util.UUID;

public record FornecedorResponse(
        UUID id, String cnpj, String name, String address, String contactName, String contactPhone,
        FornecedorPaymentMethod paymentMethod, String pixKey) {

    public static FornecedorResponse from(Fornecedor fornecedor) {
        return new FornecedorResponse(
                fornecedor.getId(), fornecedor.getCnpj(), fornecedor.getName(), fornecedor.getAddress(),
                fornecedor.getContactName(), fornecedor.getContactPhone(), fornecedor.getPaymentMethod(),
                fornecedor.getPixKey());
    }
}
