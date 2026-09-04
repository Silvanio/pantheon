package com.pantheon.service.dto;

import java.time.Instant;
import java.util.UUID;

import com.pantheon.service.entity.Plan;
import com.pantheon.service.entity.Project;
public record ProjectResponse(UUID id, String name, Plan plan, Instant trialExpiresAt, Instant planValidUntil) {

    public static ProjectResponse from(Project project) {
        return new ProjectResponse(
                project.getId(), project.getName(), project.getPlan(), project.getTrialExpiresAt(), project.getPlanValidUntil());
    }
}
