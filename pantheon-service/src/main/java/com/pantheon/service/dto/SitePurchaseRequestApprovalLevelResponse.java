package com.pantheon.service.dto;

import com.pantheon.service.entity.ConstructionFunction;
import com.pantheon.service.entity.SitePurchaseRequestApprovalLevel;
import java.util.UUID;

public record SitePurchaseRequestApprovalLevelResponse(
        UUID id, UUID constructionSiteId, int stepOrder, ConstructionFunction approverFunction) {

    public static SitePurchaseRequestApprovalLevelResponse from(SitePurchaseRequestApprovalLevel level) {
        return new SitePurchaseRequestApprovalLevelResponse(
                level.getId(), level.getConstructionSiteId(), level.getStepOrder(), level.getApproverFunction());
    }
}
