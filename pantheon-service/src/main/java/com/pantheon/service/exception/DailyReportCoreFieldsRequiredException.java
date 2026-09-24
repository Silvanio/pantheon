package com.pantheon.service.exception;

import java.util.UUID;

/**
 * Thrown when saving a daily report's core section would leave weather condition, work hours
 * start, or work hours end null — all three are mandatory together (only {@code comments} is
 * optional). See {@code daily-construction-report}.
 */
public class DailyReportCoreFieldsRequiredException extends RuntimeException {

    public DailyReportCoreFieldsRequiredException(UUID reportId) {
        super("Daily report's weather condition, work hours start, and work hours end are all required: " + reportId);
    }
}
