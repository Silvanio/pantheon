package com.pantheon.service.exception;

import java.util.UUID;

public class DailyReportNotEditableException extends RuntimeException {

    public DailyReportNotEditableException(UUID reportId) {
        super("Daily report is no longer editable (already submitted): " + reportId);
    }
}
