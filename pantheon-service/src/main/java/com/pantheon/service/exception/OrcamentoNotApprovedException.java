package com.pantheon.service.exception;

import java.util.UUID;

/** Thrown when "Concluir" is attempted on an Orcamento that is not (yet) {@code APPROVED}. */
public class OrcamentoNotApprovedException extends RuntimeException {

    public OrcamentoNotApprovedException(UUID orcamentoId) {
        super("Orcamento is not Aprovado: " + orcamentoId);
    }
}
