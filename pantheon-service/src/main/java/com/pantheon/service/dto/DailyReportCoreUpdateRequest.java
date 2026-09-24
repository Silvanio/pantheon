package com.pantheon.service.dto;

import java.time.LocalTime;

/**
 * Only non-null fields are applied, so a caller can save one section (e.g. just weather) without
 * resending the others — but the update is rejected (see {@code DailyReportCoreFieldsRequiredException})
 * if it would leave {@code weatherCondition}, {@code workHoursStart}, or {@code workHoursEnd} null
 * on the saved report; only {@code weatherBlockedTasks} and {@code comments} stay optional.
 */
public record DailyReportCoreUpdateRequest(
        String weatherCondition,
        Boolean weatherBlockedTasks,
        LocalTime workHoursStart,
        LocalTime workHoursEnd,
        String comments) {
}
