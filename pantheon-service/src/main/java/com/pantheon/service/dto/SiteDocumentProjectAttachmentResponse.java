package com.pantheon.service.dto;

import com.pantheon.service.entity.SiteDocumentProjectAttachment;
import java.time.Instant;
import java.util.UUID;

public record SiteDocumentProjectAttachmentResponse(
        UUID id, UUID siteDocumentProjectId, String originalName, String contentType, Instant createdAt) {

    public static SiteDocumentProjectAttachmentResponse from(SiteDocumentProjectAttachment attachment) {
        return new SiteDocumentProjectAttachmentResponse(
                attachment.getId(), attachment.getSiteDocumentProjectId(), attachment.getOriginalName(),
                attachment.getContentType(), attachment.getCreatedAt());
    }
}
