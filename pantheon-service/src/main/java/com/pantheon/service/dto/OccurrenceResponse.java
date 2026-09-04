package com.pantheon.service.dto;

import java.util.UUID;

import com.pantheon.service.entity.DailyReportOccurrence;
public record OccurrenceResponse(UUID id, String description) {

    public static OccurrenceResponse from(DailyReportOccurrence occurrence) {
        return new OccurrenceResponse(occurrence.getId(), occurrence.getDescription());
    }
}
