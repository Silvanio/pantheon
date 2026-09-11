package com.pantheon.service.exception;

import java.util.UUID;

/** Thrown when an action requiring a {@code DRAFT} Orcamento (line-item edits, submit) is attempted on one in another status. */
public class OrcamentoNotDraftException extends RuntimeException {

    public OrcamentoNotDraftException(UUID orcamentoId) {
        super("Orcamento is not in Rascunho status: " + orcamentoId);
    }
}
