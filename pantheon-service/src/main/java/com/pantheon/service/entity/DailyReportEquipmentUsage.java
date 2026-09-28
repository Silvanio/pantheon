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

    /** Nullable — {@link #customName} is used instead when the equipment isn't registered. Exactly one of the two must be non-null, enforced in {@code DailyReportService#addEquipmentUsage}. */
    @Column(name = "equipment_id")
    private UUID equipmentId;

    /** Free-text equipment name for "outro equipamento" (not in the site's registry). Mirrors {@link DailyReportWorkforceEntry}'s membershipId/roleDescription fallback pattern. */
    @Column(name = "custom_name")
    private String customName;

    @Column(name = "status_note")
    private String statusNote;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected DailyReportEquipmentUsage() {
        // JPA
    }

    public DailyReportEquipmentUsage(
            UUID id, UUID dailyReportId, UUID equipmentId, String customName, String statusNote, Instant createdAt) {
        this.id = id;
        this.dailyReportId = dailyReportId;
        this.equipmentId = equipmentId;
        this.customName = customName;
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

    public String getCustomName() {
        return customName;
    }

    public String getStatusNote() {
        return statusNote;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
