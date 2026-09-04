package com.pantheon.service.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

/**
 * Aggregated registration data for a user (CNPJ/CPF, legal name, address) — collected once
 * when the user first creates a project, and reused/updated on subsequent project creations
 * rather than re-attached to each project.
 */
@Entity
@Table(name = "user_profile")
public class UserProfile {

    @Id
    private UUID id;

    @Column(name = "user_id", nullable = false, unique = true)
    private UUID userId;

    @Column(name = "cnpj_cpf", nullable = false)
    private String cnpjCpf;

    @Column(name = "legal_name", nullable = false)
    private String legalName;

    @Column(nullable = false)
    private String address;

    @Column(name = "postal_code", nullable = false)
    private String postalCode;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected UserProfile() {
        // JPA
    }

    public UserProfile(
            UUID id, UUID userId, String cnpjCpf, String legalName, String address, String postalCode, Instant now) {
        this.id = id;
        this.userId = userId;
        this.cnpjCpf = cnpjCpf;
        this.legalName = legalName;
        this.address = address;
        this.postalCode = postalCode;
        this.createdAt = now;
        this.updatedAt = now;
    }

    public void update(String cnpjCpf, String legalName, String address, String postalCode, Instant now) {
        this.cnpjCpf = cnpjCpf;
        this.legalName = legalName;
        this.address = address;
        this.postalCode = postalCode;
        this.updatedAt = now;
    }

    public UUID getId() {
        return id;
    }

    public UUID getUserId() {
        return userId;
    }

    public String getCnpjCpf() {
        return cnpjCpf;
    }

    public String getLegalName() {
        return legalName;
    }

    public String getAddress() {
        return address;
    }

    public String getPostalCode() {
        return postalCode;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
