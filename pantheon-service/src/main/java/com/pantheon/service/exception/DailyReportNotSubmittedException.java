package com.pantheon.service.exception;

import java.util.UUID;

public class DailyReportNotSubmittedException extends RuntimeException {

    public DailyReportNotSubmittedException(UUID reportId) {
        super("Daily report must be submitted before it can be signed: " + reportId);
    }
}
