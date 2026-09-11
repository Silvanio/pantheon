package com.pantheon.service.exception;

import java.util.UUID;

/** Thrown when submitting an Orcamento with no line items for approval. */
public class OrcamentoEmptyException extends RuntimeException {

    public OrcamentoEmptyException(UUID orcamentoId) {
        super("Orcamento has no line items: " + orcamentoId);
    }
}
