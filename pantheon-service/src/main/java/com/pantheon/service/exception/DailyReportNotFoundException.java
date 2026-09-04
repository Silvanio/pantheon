package com.pantheon.service.exception;

import java.util.UUID;

public class DailyReportNotFoundException extends RuntimeException {

    public DailyReportNotFoundException(UUID reportId) {
        super("Daily report not found: " + reportId);
    }
}
