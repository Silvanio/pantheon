package com.pantheon.service.dto;

import com.pantheon.service.entity.ConstructionFunction;
import com.pantheon.service.entity.SiteDailyReportApprovalLevel;
import java.util.UUID;

public record SiteDailyReportApprovalLevelResponse(
        UUID id, UUID constructionSiteId, int stepOrder, ConstructionFunction approverFunction) {

    public static SiteDailyReportApprovalLevelResponse from(SiteDailyReportApprovalLevel level) {
        return new SiteDailyReportApprovalLevelResponse(
                level.getId(), level.getConstructionSiteId(), level.getStepOrder(), level.getApproverFunction());
    }
}
