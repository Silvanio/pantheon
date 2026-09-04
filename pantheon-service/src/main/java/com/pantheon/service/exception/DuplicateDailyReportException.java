package com.pantheon.service.exception;

import java.time.LocalDate;
import java.util.UUID;

public class DuplicateDailyReportException extends RuntimeException {

    public DuplicateDailyReportException(UUID constructionSiteId, LocalDate reportDate) {
        super("A daily report already exists for construction site " + constructionSiteId + " on " + reportDate);
    }
}
