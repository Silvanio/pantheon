package com.pantheon.service.dto;

import com.pantheon.service.entity.SiteDocumentProjectAttachment;
import java.time.Instant;
import java.util.UUID;

public record SiteDocumentProjectAttachmentResponse(
        UUID id,
        UUID constructionSiteId,
        UUID siteDocumentProjectId,
        String originalName,
        String contentType,
        UUID taskCardId,
        UUID uploadedBy,
        String uploadedByName,
        Instant createdAt,
        String folderPath) {

    public static SiteDocumentProjectAttachmentResponse from(
            SiteDocumentProjectAttachment attachment, String uploadedByName, String folderPath) {
        return new SiteDocumentProjectAttachmentResponse(
                attachment.getId(), attachment.getConstructionSiteId(), attachment.getSiteDocumentProjectId(),
                attachment.getOriginalName(), attachment.getContentType(), attachment.getTaskCardId(),
                attachment.getUploadedBy(), uploadedByName, attachment.getCreatedAt(), folderPath);
    }

    public static SiteDocumentProjectAttachmentResponse from(SiteDocumentProjectAttachment attachment, String uploadedByName) {
        return from(attachment, uploadedByName, null);
    }
}
