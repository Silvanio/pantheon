package com.pantheon.service.dto;

import com.pantheon.service.entity.ConstructionFunction;
import com.pantheon.service.entity.OrcamentoApproval;
import com.pantheon.service.entity.OrcamentoApprovalStatus;
import java.time.Instant;
import java.util.UUID;

public record OrcamentoApprovalResponse(
        UUID id,
        UUID orcamentoId,
        int cycleNumber,
        int stepOrder,
        ConstructionFunction approverFunction,
        OrcamentoApprovalStatus status,
        UUID decidedBySiteMembershipId,
        Instant decidedAt,
        String comment,
        Instant createdAt) {

    public static OrcamentoApprovalResponse from(OrcamentoApproval approval) {
        return new OrcamentoApprovalResponse(
                approval.getId(), approval.getOrcamentoId(), approval.getCycleNumber(), approval.getStepOrder(),
                approval.getApproverFunction(), approval.getStatus(), approval.getDecidedBySiteMembershipId(),
                approval.getDecidedAt(), approval.getComment(), approval.getCreatedAt());
    }
}
