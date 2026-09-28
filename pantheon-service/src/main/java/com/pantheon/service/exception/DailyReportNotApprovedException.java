package com.pantheon.service.exception;

import java.util.UUID;

/** Thrown when a sign-off is attempted on a daily report that is not yet {@code APPROVED} (still {@code DRAFT} or {@code PENDING_APPROVAL}). */
public class DailyReportNotApprovedException extends RuntimeException {

    public DailyReportNotApprovedException(UUID reportId) {
        super("Daily report must be approved before it can be signed: " + reportId);
    }
}
