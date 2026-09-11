package com.pantheon.service.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "daily_report_material_received")
public class DailyReportMaterialReceived {

    @Id
    private UUID id;

    @Column(name = "daily_report_id", nullable = false)
    private UUID dailyReportId;

    @Column(name = "material_name", nullable = false)
    private String materialName;

    @Column
    private String unit;

    @Column(nullable = false, precision = 19, scale = 3)
    private BigDecimal quantity;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected DailyReportMaterialReceived() {
        // JPA
    }

    public DailyReportMaterialReceived(
            UUID id, UUID dailyReportId, String materialName, String unit, BigDecimal quantity, Instant createdAt) {
        this.id = id;
        this.dailyReportId = dailyReportId;
        this.materialName = materialName;
        this.unit = unit;
        this.quantity = quantity;
        this.createdAt = createdAt;
    }

    public UUID getId() {
        return id;
    }

    public UUID getDailyReportId() {
        return dailyReportId;
    }

    public String getMaterialName() {
        return materialName;
    }

    public String getUnit() {
        return unit;
    }

    public BigDecimal getQuantity() {
        return quantity;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
