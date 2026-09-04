package com.pantheon.service.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "receipt_verification")
public class ReceiptVerification {

    @Id
    private UUID id;

    @Column(name = "material_request_item_id", nullable = false)
    private UUID materialRequestItemId;

    @Column(name = "received_quantity", nullable = false, precision = 19, scale = 3)
    private BigDecimal receivedQuantity;

    @Column(name = "verified_by", nullable = false)
    private UUID verifiedBy;

    @Column(name = "verified_at", nullable = false)
    private Instant verifiedAt;

    @Column
    private String note;

    protected ReceiptVerification() {
        // JPA
    }

    public ReceiptVerification(
            UUID id, UUID materialRequestItemId, BigDecimal receivedQuantity, UUID verifiedBy, String note,
            Instant verifiedAt) {
        this.id = id;
        this.materialRequestItemId = materialRequestItemId;
        this.receivedQuantity = receivedQuantity;
        this.verifiedBy = verifiedBy;
        this.note = note;
        this.verifiedAt = verifiedAt;
    }

    public UUID getId() {
        return id;
    }

    public UUID getMaterialRequestItemId() {
        return materialRequestItemId;
    }

    public BigDecimal getReceivedQuantity() {
        return receivedQuantity;
    }

    public UUID getVerifiedBy() {
        return verifiedBy;
    }

    public Instant getVerifiedAt() {
        return verifiedAt;
    }

    public String getNote() {
        return note;
    }
}
