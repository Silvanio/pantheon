package com.pantheon.service.dto;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

/**
 * A read-only, on-demand comparison grid for a Pedido de Compra: one row per item, one column
 * per linked Orcamento (supplier). See {@code purchase-requests}' "Pedido de Compra comparison
 * table".
 */
public record PurchaseRequestComparisonResponse(List<ColumnResponse> columns, List<RowResponse> rows) {

    public record ColumnResponse(UUID orcamentoId, String supplierName) {
    }

    /** {@code cells} contains at most one entry per column's {@code orcamentoId}; an absent column has no quote for this item. */
    public record RowResponse(UUID itemId, String itemName, BigDecimal quantity, String unit, List<CellResponse> cells) {
    }

    public record CellResponse(UUID orcamentoId, UUID lineItemId, BigDecimal unitPrice, BigDecimal quantity, boolean selected) {
    }
}
