package com.pantheon.service.exception;

import java.util.UUID;

/** Thrown when a member whose function does not match the current pending approval step (and who is not company staff) attempts to act on it. */
public class NotCurrentApprovalStepException extends RuntimeException {

    public NotCurrentApprovalStepException(UUID orcamentoId) {
        super("User is not authorized to act on the current approval step of Orcamento: " + orcamentoId);
    }
}
