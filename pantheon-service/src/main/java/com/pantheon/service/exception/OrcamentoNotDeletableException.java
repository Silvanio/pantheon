package com.pantheon.service.exception;

import java.util.UUID;

/** Thrown when deletion is attempted on an Orcamento that is {@code LOCKED} rather than {@code DRAFT}. */
public class OrcamentoNotDeletableException extends RuntimeException {

    public OrcamentoNotDeletableException(UUID orcamentoId) {
        super("Orcamento can only be deleted while Rascunho: " + orcamentoId);
    }
}
