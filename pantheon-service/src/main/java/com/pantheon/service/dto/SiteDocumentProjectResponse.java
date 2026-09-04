package com.pantheon.service.dto;

import com.pantheon.service.entity.SiteDocumentProject;
import java.time.Instant;
import java.util.UUID;

public record SiteDocumentProjectResponse(
        UUID id, UUID constructionSiteId, String name, UUID createdBy, Instant createdAt) {

    public static SiteDocumentProjectResponse from(SiteDocumentProject project) {
        return new SiteDocumentProjectResponse(
                project.getId(), project.getConstructionSiteId(), project.getName(), project.getCreatedBy(),
                project.getCreatedAt());
    }
}
