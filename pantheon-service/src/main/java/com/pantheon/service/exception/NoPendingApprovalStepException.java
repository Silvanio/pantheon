package com.pantheon.service.exception;

import java.util.UUID;

/** Thrown when approving/rejecting is attempted on an Orcamento with no pending approval step (e.g. not Em aprovação). */
public class NoPendingApprovalStepException extends RuntimeException {

    public NoPendingApprovalStepException(UUID orcamentoId) {
        super("Orcamento has no pending approval step: " + orcamentoId);
    }
}
