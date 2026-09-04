package com.pantheon.service.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import java.util.List;

public record OrcamentoCreationRequest(@NotEmpty @Valid List<OrcamentoLineItemRequest> items) {
}
