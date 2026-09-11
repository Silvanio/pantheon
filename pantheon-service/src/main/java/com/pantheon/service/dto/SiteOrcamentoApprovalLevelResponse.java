package com.pantheon.service.dto;

import com.pantheon.service.entity.ConstructionFunction;
import com.pantheon.service.entity.SiteOrcamentoApprovalLevel;
import java.util.UUID;

public record SiteOrcamentoApprovalLevelResponse(
        UUID id, UUID constructionSiteId, int stepOrder, ConstructionFunction approverFunction) {

    public static SiteOrcamentoApprovalLevelResponse from(SiteOrcamentoApprovalLevel level) {
        return new SiteOrcamentoApprovalLevelResponse(
                level.getId(), level.getConstructionSiteId(), level.getStepOrder(), level.getApproverFunction());
    }
}
