package com.pantheon.service.dto;

import java.util.UUID;

/** {@code orcamentoLineItemId == null} clears the item's current selection. */
public record SetItemSelectionRequest(UUID orcamentoLineItemId) {
}
