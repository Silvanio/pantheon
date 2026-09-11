package com.pantheon.service.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import java.util.List;

/** {@code items} may be empty (a blank Rascunho to be filled in via line-item management). */
public record OrcamentoCreationRequest(@NotNull @Valid List<OrcamentoLineItemRequest> items, @NotNull @Valid FornecedorRequest fornecedor) {
}
