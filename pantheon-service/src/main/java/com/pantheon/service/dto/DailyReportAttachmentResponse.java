package com.pantheon.service.dto;

import java.time.Instant;
import java.util.UUID;

import com.pantheon.service.entity.DailyReportAttachment;
public record DailyReportAttachmentResponse(UUID id, String originalName, String contentType, Instant uploadedAt) {

    public static DailyReportAttachmentResponse from(DailyReportAttachment attachment) {
        return new DailyReportAttachmentResponse(
                attachment.getId(), attachment.getOriginalName(), attachment.getContentType(), attachment.getCreatedAt());
    }
}
