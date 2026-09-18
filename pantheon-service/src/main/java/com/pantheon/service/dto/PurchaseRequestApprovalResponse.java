package com.pantheon.service.dto;

import com.pantheon.service.entity.ConstructionFunction;
import com.pantheon.service.entity.PurchaseRequestApproval;
import com.pantheon.service.entity.PurchaseRequestApprovalStatus;
import java.time.Instant;
import java.util.UUID;

public record PurchaseRequestApprovalResponse(
        UUID id,
        UUID purchaseRequestId,
        int cycleNumber,
        int stepOrder,
        ConstructionFunction approverFunction,
        PurchaseRequestApprovalStatus status,
        UUID decidedBySiteMembershipId,
        Instant decidedAt,
        String comment,
        Instant createdAt) {

    public static PurchaseRequestApprovalResponse from(PurchaseRequestApproval approval) {
        return new PurchaseRequestApprovalResponse(
                approval.getId(), approval.getPurchaseRequestId(), approval.getCycleNumber(), approval.getStepOrder(),
                approval.getApproverFunction(), approval.getStatus(), approval.getDecidedBySiteMembershipId(),
                approval.getDecidedAt(), approval.getComment(), approval.getCreatedAt());
    }
}
