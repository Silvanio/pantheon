package com.pantheon.service.dto;

import java.util.UUID;

import com.pantheon.service.entity.ConstructionFunction;
import com.pantheon.service.entity.ProjectRole;
public record ProjectMembershipResponse(
        UUID projectId,
        String projectName,
        ProjectRole role,
        boolean active,
        ConstructionFunction function,
        String specialty) {
}
