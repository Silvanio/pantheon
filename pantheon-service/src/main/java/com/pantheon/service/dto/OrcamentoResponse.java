package com.pantheon.service.dto;

import com.pantheon.service.entity.FornecedorPaymentMethod;
import com.pantheon.service.entity.Orcamento;
import com.pantheon.service.entity.OrcamentoStatus;
import java.time.Instant;
import java.util.UUID;

public record OrcamentoResponse(
        UUID id,
        UUID constructionSiteId,
        OrcamentoStatus status,
        UUID createdBy,
        String createdByName,
        Instant createdAt,
        String fornecedorCnpj,
        String fornecedorNome,
        String fornecedorEndereco,
        String fornecedorContatoNome,
        String fornecedorContatoTelefone,
        FornecedorPaymentMethod fornecedorFormaPagamento,
        String fornecedorPixKey,
        UUID sourcePurchaseRequestId,
        String sourcePurchaseRequestName) {

    public static OrcamentoResponse from(Orcamento orcamento, String sourcePurchaseRequestName, String createdByName) {
        return new OrcamentoResponse(
                orcamento.getId(), orcamento.getConstructionSiteId(), orcamento.getStatus(), orcamento.getCreatedBy(),
                createdByName, orcamento.getCreatedAt(), orcamento.getFornecedorCnpj(), orcamento.getFornecedorNome(),
                orcamento.getFornecedorEndereco(), orcamento.getFornecedorContatoNome(),
                orcamento.getFornecedorContatoTelefone(), orcamento.getFornecedorFormaPagamento(),
                orcamento.getFornecedorPixKey(), orcamento.getSourcePurchaseRequestId(), sourcePurchaseRequestName);
    }
}
