package com.pantheon.service.dto;

import java.util.UUID;

import com.pantheon.service.entity.ArchitecturalProject;
public record ArchitecturalProjectResponse(
        UUID id, UUID projectId, UUID constructionSiteId, String name, String description) {

    public static ArchitecturalProjectResponse from(ArchitecturalProject project) {
        return new ArchitecturalProjectResponse(
                project.getId(),
                project.getProjectId(),
                project.getConstructionSiteId(),
                project.getName(),
                project.getDescription());
    }
}
