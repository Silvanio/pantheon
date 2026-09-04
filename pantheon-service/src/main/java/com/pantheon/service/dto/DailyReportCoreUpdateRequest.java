package com.pantheon.service.dto;

import java.time.LocalTime;

/** Every field is optional — only non-null fields are applied, so a caller can save one section (e.g. just weather) without resending the others. */
public record DailyReportCoreUpdateRequest(
        String weatherCondition,
        Boolean weatherBlockedTasks,
        LocalTime workHoursStart,
        LocalTime workHoursEnd,
        String comments) {
}
