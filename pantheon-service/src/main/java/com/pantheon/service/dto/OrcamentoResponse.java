package com.pantheon.service.dto;

import com.pantheon.service.entity.Orcamento;
import com.pantheon.service.entity.OrcamentoStatus;
import java.time.Instant;
import java.util.UUID;

public record OrcamentoResponse(
        UUID id,
        UUID materialRequestId,
        OrcamentoStatus status,
        UUID createdBy,
        Instant createdAt,
        Instant sentAt,
        Instant decidedAt,
        String rejectionReason) {

    public static OrcamentoResponse from(Orcamento orcamento) {
        return new OrcamentoResponse(
                orcamento.getId(), orcamento.getMaterialRequestId(), orcamento.getStatus(), orcamento.getCreatedBy(),
                orcamento.getCreatedAt(), orcamento.getSentAt(), orcamento.getDecidedAt(),
                orcamento.getRejectionReason());
    }
}
