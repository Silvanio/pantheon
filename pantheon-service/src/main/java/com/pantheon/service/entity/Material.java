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
 * A delivery-tracking record, created only when an {@link Orcamento} is marked {@code COMPLETED}
 * — one per {@link OrcamentoLineItem}, copying its name/type/quantity at that moment. Never
 * created any other way, and never a catalog. See {@code material-delivery-tracking}.
 */
@Entity
@Table(name = "material")
public class Material {

    @Id
    private UUID id;

    @Column(name = "construction_site_id", nullable = false)
    private UUID constructionSiteId;

    @Column(name = "orcamento_line_item_id", nullable = false)
    private UUID orcamentoLineItemId;

    @Column(nullable = false)
    private String name;

    @Column
    private String type;

    @Column(nullable = false, precision = 19, scale = 3)
    private BigDecimal quantity;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private MaterialDeliveryStatus status;

    @Column(name = "delivered_at")
    private Instant deliveredAt;

    @Column(name = "delivered_by")
    private UUID deliveredBy;

    @Column(name = "checked_at")
    private Instant checkedAt;

    @Column(name = "checked_by")
    private UUID checkedBy;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected Material() {
        // JPA
    }

    public Material(
            UUID id, UUID constructionSiteId, UUID orcamentoLineItemId, String name, String type, BigDecimal quantity,
            Instant createdAt) {
        this.id = id;
        this.constructionSiteId = constructionSiteId;
        this.orcamentoLineItemId = orcamentoLineItemId;
        this.name = name;
        this.type = type;
        this.quantity = quantity;
        this.status = MaterialDeliveryStatus.AWAITING_DELIVERY;
        this.createdAt = createdAt;
    }

    public void markDelivered(UUID deliveredBy, Instant now) {
        this.status = MaterialDeliveryStatus.DELIVERED;
        this.deliveredBy = deliveredBy;
        this.deliveredAt = now;
    }

    public void markChecked(UUID checkedBy, Instant now) {
        this.status = MaterialDeliveryStatus.DELIVERED_AND_CHECKED;
        this.checkedBy = checkedBy;
        this.checkedAt = now;
    }

    public UUID getId() {
        return id;
    }

    public UUID getConstructionSiteId() {
        return constructionSiteId;
    }

    public UUID getOrcamentoLineItemId() {
        return orcamentoLineItemId;
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

    public MaterialDeliveryStatus getStatus() {
        return status;
    }

    public Instant getDeliveredAt() {
        return deliveredAt;
    }

    public UUID getDeliveredBy() {
        return deliveredBy;
    }

    public Instant getCheckedAt() {
        return checkedAt;
    }

    public UUID getCheckedBy() {
        return checkedBy;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
