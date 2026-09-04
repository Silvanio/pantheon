package com.pantheon.service.dto;

import com.pantheon.service.entity.Plan;
import com.pantheon.service.entity.PlanCode;
import java.util.UUID;

public record PlanResponse(UUID id, PlanCode code, String name, Integer activeSiteLimit) {

    public static PlanResponse from(Plan plan) {
        return new PlanResponse(plan.getId(), plan.getCode(), plan.getName(), plan.getActiveSiteLimit());
    }
}
