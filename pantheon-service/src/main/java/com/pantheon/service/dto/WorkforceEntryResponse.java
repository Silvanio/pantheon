package com.pantheon.service.dto;

import java.util.UUID;

import com.pantheon.service.entity.DailyReportWorkforceEntry;
public record WorkforceEntryResponse(UUID id, UUID membershipId, String roleDescription, int headcount) {

    public static WorkforceEntryResponse from(DailyReportWorkforceEntry entry) {
        return new WorkforceEntryResponse(
                entry.getId(), entry.getMembershipId(), entry.getRoleDescription(), entry.getHeadcount());
    }
}
