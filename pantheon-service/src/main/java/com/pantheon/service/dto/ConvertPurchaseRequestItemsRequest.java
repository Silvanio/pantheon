package com.pantheon.service.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.util.List;
import java.util.UUID;

public record ConvertPurchaseRequestItemsRequest(
        @NotEmpty List<UUID> itemIds, @NotNull @Valid FornecedorRequest fornecedor) {
}
