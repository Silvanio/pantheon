package com.pantheon.service.exception;

import java.util.UUID;

public class OrcamentoNotFoundException extends RuntimeException {

    public OrcamentoNotFoundException(UUID orcamentoId) {
        super("Orcamento not found: " + orcamentoId);
    }
}
