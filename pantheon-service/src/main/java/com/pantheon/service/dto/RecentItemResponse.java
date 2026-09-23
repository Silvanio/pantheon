package com.pantheon.service.dto;

import java.time.Instant;
import java.util.UUID;

/** A recently created item surfaced on the obra summary — shared shape for Pedido de Compra,
 * Orçamento, Task, and Projeto recent lists. */
public record RecentItemResponse(UUID id, String title, Instant createdAt) {
}
