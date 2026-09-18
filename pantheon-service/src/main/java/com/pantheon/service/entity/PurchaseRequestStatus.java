package com.pantheon.service.entity;

/**
 * Iniciado (created) → Orçado (at least one {@link Orcamento} has been created for it) →
 * Conferido (its approval cycle is fully approved) → Concluído (delivery-tracking materials
 * created). A rejection at any approval step returns Conferido back to Orçado rather than a
 * dead-end "rejected" state — see {@code purchase-request-approval-workflow}'s "Acting on an
 * approval step".
 */
public enum PurchaseRequestStatus {
    INICIADO,
    ORCADO,
    CONFERIDO,
    CONCLUIDO
}
