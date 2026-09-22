package com.pantheon.service.dto;

import java.time.LocalDate;

/** Every field is optional — only non-null fields are applied (PATCH semantics), mirroring {@code DailyReportCoreUpdateRequest}. */
public record ScheduleStageUpdateRequest(
        String name, String color, LocalDate startDate, LocalDate endDate, Integer sortOrder) {
}
