package com.pantheon.service.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "construction_site")
public class ConstructionSite {

    @Id
    private UUID id;

    @Column(name = "company_id", nullable = false)
    private UUID companyId;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private String address;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private SiteStatus status;

    @Column(name = "start_date", nullable = false)
    private LocalDate startDate;

    @Column(name = "expected_end_date")
    private LocalDate expectedEndDate;

    @Column(name = "photo_object_key")
    private String photoObjectKey;

    @Column(name = "created_by", nullable = false)
    private UUID createdBy;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected ConstructionSite() {
        // JPA
    }

    public ConstructionSite(
            UUID id,
            UUID companyId,
            String name,
            String address,
            LocalDate startDate,
            LocalDate expectedEndDate,
            UUID createdBy,
            Instant createdAt) {
        this.id = id;
        this.companyId = companyId;
        this.name = name;
        this.address = address;
        this.status = SiteStatus.PLANNING;
        this.startDate = startDate;
        this.expectedEndDate = expectedEndDate;
        this.createdBy = createdBy;
        this.createdAt = createdAt;
        this.updatedAt = createdAt;
    }

    public void updateStatus(SiteStatus newStatus, Instant now) {
        this.status = newStatus;
        this.updatedAt = now;
    }

    public void updatePhoto(String photoObjectKey, Instant now) {
        this.photoObjectKey = photoObjectKey;
        this.updatedAt = now;
    }

    public UUID getId() {
        return id;
    }

    public UUID getCompanyId() {
        return companyId;
    }

    public String getPhotoObjectKey() {
        return photoObjectKey;
    }

    public String getName() {
        return name;
    }

    public String getAddress() {
        return address;
    }

    public SiteStatus getStatus() {
        return status;
    }

    public LocalDate getStartDate() {
        return startDate;
    }

    public LocalDate getExpectedEndDate() {
        return expectedEndDate;
    }

    public UUID getCreatedBy() {
        return createdBy;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
