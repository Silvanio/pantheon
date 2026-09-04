package com.pantheon.service.exception;

import java.util.UUID;

/** Thrown when an action requiring a {@code SENT} orcamento (approve/reject) is attempted on one in another status. */
public class OrcamentoNotSentException extends RuntimeException {

    public OrcamentoNotSentException(UUID orcamentoId) {
        super("Orcamento is not awaiting a client decision: " + orcamentoId);
    }
}
