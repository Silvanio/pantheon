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
        String decidedByName,
        Instant decidedAt,
        String comment,
        Instant createdAt) {

    /** {@code decidedByName} left {@code null} — use {@link #from(DailyReportApproval, String)} when a resolved name is available (see {@code DailyReportService#listApprovalResponses}). */
    public static DailyReportApprovalResponse from(DailyReportApproval approval) {
        return from(approval, null);
    }

    public static DailyReportApprovalResponse from(DailyReportApproval approval, String decidedByName) {
        return new DailyReportApprovalResponse(
                approval.getId(), approval.getDailyReportId(), approval.getCycleNumber(), approval.getStepOrder(),
                approval.getApproverFunction(), approval.getStatus(), approval.getDecidedBySiteMembershipId(),
                decidedByName, approval.getDecidedAt(), approval.getComment(), approval.getCreatedAt());
    }
}
