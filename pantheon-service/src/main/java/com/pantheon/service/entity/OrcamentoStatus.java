package com.pantheon.service.entity;

/**
 * Rascunho (DRAFT) → Em aprovação (IN_APPROVAL) → Aprovado (APPROVED) → Concluído (COMPLETED).
 * A rejection at any approval step returns the Orcamento to DRAFT rather than a dead-end
 * "rejected" state — see {@code orcamento-approval-workflow}'s "Acting on an approval step".
 */
public enum OrcamentoStatus {
    DRAFT,
    IN_APPROVAL,
    APPROVED,
    COMPLETED
}
