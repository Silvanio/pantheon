package com.pantheon.service.dto;

import com.pantheon.service.entity.Orcamento;
import com.pantheon.service.entity.OrcamentoStatus;
import java.time.Instant;
import java.util.UUID;

public record OrcamentoResponse(
        UUID id,
        UUID constructionSiteId,
        OrcamentoStatus status,
        UUID createdBy,
        Instant createdAt,
        Instant submittedAt,
        Instant approvedAt,
        Instant completedAt,
        int currentApprovalCycle,
        String lastRejectionReason,
        String fornecedorCnpj,
        String fornecedorNome,
        String fornecedorEndereco,
        String fornecedorContatoNome,
        String fornecedorContatoTelefone,
        UUID sourcePurchaseRequestId,
        String sourcePurchaseRequestName) {

    public static OrcamentoResponse from(Orcamento orcamento) {
        return from(orcamento, null);
    }

    public static OrcamentoResponse from(Orcamento orcamento, String sourcePurchaseRequestName) {
        return new OrcamentoResponse(
                orcamento.getId(), orcamento.getConstructionSiteId(), orcamento.getStatus(), orcamento.getCreatedBy(),
                orcamento.getCreatedAt(), orcamento.getSubmittedAt(), orcamento.getApprovedAt(),
                orcamento.getCompletedAt(), orcamento.getCurrentApprovalCycle(), orcamento.getLastRejectionReason(),
                orcamento.getFornecedorCnpj(), orcamento.getFornecedorNome(), orcamento.getFornecedorEndereco(),
                orcamento.getFornecedorContatoNome(), orcamento.getFornecedorContatoTelefone(),
                orcamento.getSourcePurchaseRequestId(), sourcePurchaseRequestName);
    }
}
