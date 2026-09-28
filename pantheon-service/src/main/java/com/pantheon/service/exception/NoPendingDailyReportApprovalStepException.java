package com.pantheon.service.exception;

import java.util.UUID;

/** Thrown when approving/rejecting is attempted on a Diário de Obra with no pending approval step. */
public class NoPendingDailyReportApprovalStepException extends RuntimeException {

    public NoPendingDailyReportApprovalStepException(UUID dailyReportId) {
        super("Daily report has no pending approval step: " + dailyReportId);
    }
}
