package com.pantheon.service.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * A free-text "Pedido de Compra" entry: a material the site needs to buy, with no catalog
 * reference. Selecting one or more {@code PENDING} items converts them into a new
 * {@link Orcamento}'s line items (see {@code purchase-requests}' conversion requirement).
 */
@Entity
@Table(name = "purchase_request_item")
public class PurchaseRequestItem {

    @Id
    private UUID id;

    @Column(name = "construction_site_id", nullable = false)
    private UUID constructionSiteId;

    @Column(name = "purchase_request_id", nullable = false)
    private UUID purchaseRequestId;

    @Column(nullable = false)
    private String name;

    @Column
    private String type;

    @Column(nullable = false, precision = 19, scale = 3)
    private BigDecimal quantity;

    @Column
    private String unit;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PurchaseRequestItemStatus status;

    @Column(name = "created_by", nullable = false)
    private UUID createdBy;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "converted_to_orcamento_id")
    private UUID convertedToOrcamentoId;

    @Column(name = "converted_at")
    private Instant convertedAt;

    @Column(name = "selected_orcamento_line_item_id")
    private UUID selectedOrcamentoLineItemId;

    protected PurchaseRequestItem() {
        // JPA
    }

    public PurchaseRequestItem(
            UUID id, UUID constructionSiteId, UUID purchaseRequestId, String name, String type, BigDecimal quantity,
            String unit, UUID createdBy, Instant createdAt) {
        this.id = id;
        this.constructionSiteId = constructionSiteId;
        this.purchaseRequestId = purchaseRequestId;
        this.name = name;
        this.type = type;
        this.quantity = quantity;
        this.unit = unit;
        this.status = PurchaseRequestItemStatus.PENDING;
        this.createdBy = createdBy;
        this.createdAt = createdAt;
    }

    public void convertTo(UUID orcamentoId, Instant now) {
        this.status = PurchaseRequestItemStatus.CONVERTED;
        this.convertedToOrcamentoId = orcamentoId;
        this.convertedAt = now;
    }

    /** Undoes {@link #convertTo}, called when the Orcamento it was converted into is deleted and no other Orcamento still quotes it. */
    public void revertConversion() {
        this.status = PurchaseRequestItemStatus.PENDING;
        this.convertedToOrcamentoId = null;
        this.convertedAt = null;
    }

    /**
     * Re-points the recorded Orcamento without touching {@code status}/{@code convertedAt} —
     * called when the currently-recorded Orcamento is deleted but another still quotes this item.
     */
    public void repointConversion(UUID orcamentoId) {
        this.convertedToOrcamentoId = orcamentoId;
    }

    /**
     * Records which {@code OrcamentoLineItem} (and therefore which supplier's quote) fulfills
     * this item — see {@code purchase-requests}' "Per-item supplier selection". A plain field
     * write with no side effects on this item's own status.
     */
    public void select(UUID orcamentoLineItemId) {
        this.selectedOrcamentoLineItemId = orcamentoLineItemId;
    }

    public void clearSelection() {
        this.selectedOrcamentoLineItemId = null;
    }

    public UUID getId() {
        return id;
    }

    public UUID getConstructionSiteId() {
        return constructionSiteId;
    }

    public UUID getPurchaseRequestId() {
        return purchaseRequestId;
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

    public String getUnit() {
        return unit;
    }

    public PurchaseRequestItemStatus getStatus() {
        return status;
    }

    public UUID getCreatedBy() {
        return createdBy;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public UUID getConvertedToOrcamentoId() {
        return convertedToOrcamentoId;
    }

    public Instant getConvertedAt() {
        return convertedAt;
    }

    public UUID getSelectedOrcamentoLineItemId() {
        return selectedOrcamentoLineItemId;
    }
}
