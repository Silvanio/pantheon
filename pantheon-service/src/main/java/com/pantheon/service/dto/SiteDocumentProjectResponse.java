package com.pantheon.service.dto;

import com.pantheon.service.entity.SiteDocumentProject;
import java.time.Instant;
import java.util.UUID;

public record SiteDocumentProjectResponse(
        UUID id,
        UUID constructionSiteId,
        UUID parentId,
        String name,
        UUID taskCardId,
        String linkedTaskTitle,
        UUID createdBy,
        String createdByName,
        Instant createdAt,
        UUID updatedBy,
        String updatedByName,
        Instant updatedAt) {

    public static SiteDocumentProjectResponse from(
            SiteDocumentProject project, String createdByName, String updatedByName, String linkedTaskTitle) {
        return new SiteDocumentProjectResponse(
                project.getId(), project.getConstructionSiteId(), project.getParentId(), project.getName(),
                project.getTaskCardId(), linkedTaskTitle, project.getCreatedBy(), createdByName,
                project.getCreatedAt(), project.getUpdatedBy(), updatedByName, project.getUpdatedAt());
    }
}
