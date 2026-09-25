package com.pantheon.service.dto;

import com.pantheon.service.entity.Orcamento;
import com.pantheon.service.entity.PurchaseRequest;
import com.pantheon.service.entity.PurchaseRequestStatus;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record PurchaseRequestResponse(
        UUID id,
        UUID constructionSiteId,
        String name,
        PurchaseRequestStatus status,
        UUID createdBy,
        String createdByName,
        Instant createdAt,
        Instant submittedAt,
        Instant approvedAt,
        Instant completedAt,
        String lastRejectionReason,
        List<LinkedOrcamentoSummary> linkedOrcamentos) {

    public record LinkedOrcamentoSummary(UUID id, String fornecedorNome) {

        public static LinkedOrcamentoSummary from(Orcamento orcamento) {
            return new LinkedOrcamentoSummary(orcamento.getId(), orcamento.getFornecedorNome());
        }
    }

    public static PurchaseRequestResponse from(PurchaseRequest purchaseRequest, String createdByName) {
        return from(purchaseRequest, List.of(), createdByName);
    }

    public static PurchaseRequestResponse from(
            PurchaseRequest purchaseRequest, List<Orcamento> linkedOrcamentos, String createdByName) {
        return new PurchaseRequestResponse(
                purchaseRequest.getId(), purchaseRequest.getConstructionSiteId(), purchaseRequest.getName(),
                purchaseRequest.getStatus(), purchaseRequest.getCreatedBy(), createdByName, purchaseRequest.getCreatedAt(),
                purchaseRequest.getSubmittedAt(), purchaseRequest.getApprovedAt(), purchaseRequest.getCompletedAt(),
                purchaseRequest.getLastRejectionReason(),
                linkedOrcamentos.stream().map(LinkedOrcamentoSummary::from).toList());
    }
}
