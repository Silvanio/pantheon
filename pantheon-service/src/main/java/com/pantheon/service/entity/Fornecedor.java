package com.pantheon.service.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

/**
 * A supplier that has quoted an Orçamento, scoped to the owning {@link Company} (not to a single
 * construction site) so it can be reused across every obra of that company. Identified within a
 * company by CNPJ (free text, no checksum validation — same convention as
 * {@link Company#getCnpj()}). Never edited once created; see {@code supplier-registry}'s
 * find-or-create requirement.
 */
@Entity
@Table(name = "fornecedor")
public class Fornecedor {

    @Id
    private UUID id;

    @Column(name = "company_id", nullable = false)
    private UUID companyId;

    @Column(nullable = false)
    private String cnpj;

    @Column(nullable = false)
    private String name;

    @Column
    private String address;

    @Column(name = "contact_name")
    private String contactName;

    @Column(name = "contact_phone")
    private String contactPhone;

    @Column(name = "created_by", nullable = false)
    private UUID createdBy;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected Fornecedor() {
        // JPA
    }

    public Fornecedor(
            UUID id, UUID companyId, String cnpj, String name, String address, String contactName,
            String contactPhone, UUID createdBy, Instant createdAt) {
        this.id = id;
        this.companyId = companyId;
        this.cnpj = cnpj;
        this.name = name;
        this.address = address;
        this.contactName = contactName;
        this.contactPhone = contactPhone;
        this.createdBy = createdBy;
        this.createdAt = createdAt;
    }

    public UUID getId() {
        return id;
    }

    public UUID getCompanyId() {
        return companyId;
    }

    public String getCnpj() {
        return cnpj;
    }

    public String getName() {
        return name;
    }

    public String getAddress() {
        return address;
    }

    public String getContactName() {
        return contactName;
    }

    public String getContactPhone() {
        return contactPhone;
    }

    public UUID getCreatedBy() {
        return createdBy;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
