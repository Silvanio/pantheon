package com.pantheon.service.entity;

/**
 * Rascunho (DRAFT) — freely editable — or Bloqueado (LOCKED), which it becomes automatically
 * while its originating Pedido de Compra's approval is {@code CONFERIDO} or {@code CONCLUIDO},
 * and leaves if that approval is rejected. An Orcamento with no originating Pedido de Compra
 * stays DRAFT indefinitely. See {@code orcamento-management}.
 */
public enum OrcamentoStatus {
    DRAFT,
    LOCKED
}
