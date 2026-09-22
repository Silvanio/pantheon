package com.pantheon.service.exception;

import java.util.UUID;

/** Thrown when deletion is attempted on a Diário de Obra that is not (still) DRAFT. */
public class DailyReportNotDeletableException extends RuntimeException {

    public DailyReportNotDeletableException(UUID reportId) {
        super("Daily report can only be deleted while still a draft: " + reportId);
    }
}
