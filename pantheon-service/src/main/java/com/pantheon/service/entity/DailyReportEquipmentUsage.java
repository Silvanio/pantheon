package com.pantheon.service.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "daily_report_equipment_usage")
public class DailyReportEquipmentUsage {

    @Id
    private UUID id;

    @Column(name = "daily_report_id", nullable = false)
    private UUID dailyReportId;

    @Column(name = "equipment_id", nullable = false)
    private UUID equipmentId;

    @Column(name = "status_note")
    private String statusNote;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected DailyReportEquipmentUsage() {
        // JPA
    }

    public DailyReportEquipmentUsage(
            UUID id, UUID dailyReportId, UUID equipmentId, String statusNote, Instant createdAt) {
        this.id = id;
        this.dailyReportId = dailyReportId;
        this.equipmentId = equipmentId;
        this.statusNote = statusNote;
        this.createdAt = createdAt;
    }

    public UUID getId() {
        return id;
    }

    public UUID getDailyReportId() {
        return dailyReportId;
    }

    public UUID getEquipmentId() {
        return equipmentId;
    }

    public String getStatusNote() {
        return statusNote;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
