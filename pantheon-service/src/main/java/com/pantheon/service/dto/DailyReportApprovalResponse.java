package com.pantheon.service.dto;

import com.pantheon.service.entity.ConstructionFunction;
import com.pantheon.service.entity.DailyReportApproval;
import com.pantheon.service.entity.DailyReportApprovalStatus;
import java.time.Instant;
import java.util.UUID;

public record DailyReportApprovalResponse(
        UUID id,
        UUID dailyReportId,
        int cycleNumber,
        int stepOrder,
        ConstructionFunction approverFunction,
        DailyReportApprovalStatus status,
        UUID decidedBySiteMembershipId,
        Instant decidedAt,
        String comment,
        Instant createdAt) {

    public static DailyReportApprovalResponse from(DailyReportApproval approval) {
        return new DailyReportApprovalResponse(
                approval.getId(), approval.getDailyReportId(), approval.getCycleNumber(), approval.getStepOrder(),
                approval.getApproverFunction(), approval.getStatus(), approval.getDecidedBySiteMembershipId(),
                approval.getDecidedAt(), approval.getComment(), approval.getCreatedAt());
    }
}
