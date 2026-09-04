package com.pantheon.service.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.util.UUID;

@Entity
@Table(name = "material_request_item")
public class MaterialRequestItem {

    @Id
    private UUID id;

    @Column(name = "material_request_id", nullable = false)
    private UUID materialRequestId;

    @Column(name = "material_id", nullable = false)
    private UUID materialId;

    @Column(name = "requested_quantity", nullable = false, precision = 19, scale = 3)
    private BigDecimal requestedQuantity;

    protected MaterialRequestItem() {
        // JPA
    }

    public MaterialRequestItem(UUID id, UUID materialRequestId, UUID materialId, BigDecimal requestedQuantity) {
        this.id = id;
        this.materialRequestId = materialRequestId;
        this.materialId = materialId;
        this.requestedQuantity = requestedQuantity;
    }

    public UUID getId() {
        return id;
    }

    public UUID getMaterialRequestId() {
        return materialRequestId;
    }

    public UUID getMaterialId() {
        return materialId;
    }

    public BigDecimal getRequestedQuantity() {
        return requestedQuantity;
    }
}
