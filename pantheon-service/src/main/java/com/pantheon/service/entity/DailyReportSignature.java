package com.pantheon.service.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "daily_report_signature")
public class DailyReportSignature {

    @Id
    private UUID id;

    @Column(name = "daily_report_id", nullable = false)
    private UUID dailyReportId;

    @Column(name = "membership_id", nullable = false)
    private UUID membershipId;

    /** Snapshot of the signer's construction function at signing time, for the PDF's audit trail. */
    @Enumerated(EnumType.STRING)
    @Column
    private ConstructionFunction function;

    @Column(name = "signed_at", nullable = false)
    private Instant signedAt;

    protected DailyReportSignature() {
        // JPA
    }

    public DailyReportSignature(
            UUID id, UUID dailyReportId, UUID membershipId, ConstructionFunction function, Instant signedAt) {
        this.id = id;
        this.dailyReportId = dailyReportId;
        this.membershipId = membershipId;
        this.function = function;
        this.signedAt = signedAt;
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

    public ConstructionFunction getFunction() {
        return function;
    }

    public Instant getSignedAt() {
        return signedAt;
    }
}
