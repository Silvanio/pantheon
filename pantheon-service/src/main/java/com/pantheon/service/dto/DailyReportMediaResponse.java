package com.pantheon.service.dto;

import java.time.Instant;
import java.util.UUID;

import com.pantheon.service.entity.DailyReportMedia;
import com.pantheon.service.entity.MediaType;
public record DailyReportMediaResponse(UUID id, MediaType type, String caption, String contentType, Instant uploadedAt) {

    public static DailyReportMediaResponse from(DailyReportMedia media) {
        return new DailyReportMediaResponse(
                media.getId(), media.getType(), media.getCaption(), media.getContentType(), media.getCreatedAt());
    }
}
