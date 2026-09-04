package com.pantheon.service.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.util.UUID;

/** Prices one line item of the {@link MaterialRequest} an {@link Orcamento} was drafted against. */
@Entity
@Table(name = "orcamento_line_item")
public class OrcamentoLineItem {

    @Id
    private UUID id;

    @Column(name = "orcamento_id", nullable = false)
    private UUID orcamentoId;

    @Column(name = "material_request_item_id", nullable = false)
    private UUID materialRequestItemId;

    @Column(name = "unit_price", nullable = false)
    private BigDecimal unitPrice;

    protected OrcamentoLineItem() {
        // JPA
    }

    public OrcamentoLineItem(UUID id, UUID orcamentoId, UUID materialRequestItemId, BigDecimal unitPrice) {
        this.id = id;
        this.orcamentoId = orcamentoId;
        this.materialRequestItemId = materialRequestItemId;
        this.unitPrice = unitPrice;
    }

    public UUID getId() {
        return id;
    }

    public UUID getOrcamentoId() {
        return orcamentoId;
    }

    public UUID getMaterialRequestItemId() {
        return materialRequestItemId;
    }

    public BigDecimal getUnitPrice() {
        return unitPrice;
    }
}
