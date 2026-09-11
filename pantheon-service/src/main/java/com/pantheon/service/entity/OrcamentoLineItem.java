package com.pantheon.service.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.util.UUID;

/**
 * A free-text line item of an {@link Orcamento} — name/type/quantity are always typed in by the
 * creator (copied from a {@link PurchaseRequestItem} when converting one, or entered from
 * scratch), never a live catalog reference. {@code sourcePurchaseRequestItemId} is kept purely
 * for traceability. See {@code orcamento-approval-workflow}'s "Orçamento line item management".
 */
@Entity
@Table(name = "orcamento_line_item")
public class OrcamentoLineItem {

    @Id
    private UUID id;

    @Column(name = "orcamento_id", nullable = false)
    private UUID orcamentoId;

    @Column(nullable = false)
    private String name;

    @Column
    private String type;

    @Column(nullable = false, precision = 19, scale = 3)
    private BigDecimal quantity;

    @Column(name = "unit_price", precision = 19, scale = 2)
    private BigDecimal unitPrice;

    @Column(name = "source_purchase_request_item_id")
    private UUID sourcePurchaseRequestItemId;

    protected OrcamentoLineItem() {
        // JPA
    }

    public OrcamentoLineItem(
            UUID id, UUID orcamentoId, String name, String type, BigDecimal quantity, BigDecimal unitPrice,
            UUID sourcePurchaseRequestItemId) {
        this.id = id;
        this.orcamentoId = orcamentoId;
        this.name = name;
        this.type = type;
        this.quantity = quantity;
        this.unitPrice = unitPrice;
        this.sourcePurchaseRequestItemId = sourcePurchaseRequestItemId;
    }

    public void update(String name, String type, BigDecimal quantity, BigDecimal unitPrice) {
        this.name = name;
        this.type = type;
        this.quantity = quantity;
        this.unitPrice = unitPrice;
    }

    public UUID getId() {
        return id;
    }

    public UUID getOrcamentoId() {
        return orcamentoId;
    }

    public String getName() {
        return name;
    }

    public String getType() {
        return type;
    }

    public BigDecimal getQuantity() {
        return quantity;
    }

    public BigDecimal getUnitPrice() {
        return unitPrice;
    }

    public UUID getSourcePurchaseRequestItemId() {
        return sourcePurchaseRequestItemId;
    }
}
