package com.pantheon.service.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "daily_report_workforce_entry")
public class DailyReportWorkforceEntry {

    @Id
    private UUID id;

    @Column(name = "daily_report_id", nullable = false)
    private UUID dailyReportId;

    @Column(name = "membership_id")
    private UUID membershipId;

    @Column(name = "role_description", nullable = false)
    private String roleDescription;

    @Column(nullable = false)
    private int headcount;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected DailyReportWorkforceEntry() {
        // JPA
    }

    public DailyReportWorkforceEntry(
            UUID id, UUID dailyReportId, UUID membershipId, String roleDescription, int headcount, Instant createdAt) {
        this.id = id;
        this.dailyReportId = dailyReportId;
        this.membershipId = membershipId;
        this.roleDescription = roleDescription;
        this.headcount = headcount;
        this.createdAt = createdAt;
    }

    public UUID getId() {
        return id;
    }

    public UUID getDailyReportId() {
        return dailyReportId;
    }

    public UUID getMembershipId() {
        return membershipId;
    }

    public String getRoleDescription() {
        return roleDescription;
    }

    public int getHeadcount() {
        return headcount;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
