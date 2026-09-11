package com.pantheon.service.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

/**
 * A "Pedido de Compra" document: a named, dated header grouping one or more
 * {@link PurchaseRequestItem}s entered together. The name is generated at creation time (see
 * {@code purchase-requests}' header-creation requirement) and never changes. A single header may
 * spawn more than one {@link Orcamento} over time, as its items are converted in separate
 * batches.
 */
@Entity
@Table(name = "purchase_request")
public class PurchaseRequest {

    @Id
    private UUID id;

    @Column(name = "construction_site_id", nullable = false)
    private UUID constructionSiteId;

    @Column(nullable = false)
    private String name;

    @Column(name = "created_by", nullable = false)
    private UUID createdBy;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected PurchaseRequest() {
        // JPA
    }

    public PurchaseRequest(UUID id, UUID constructionSiteId, String name, UUID createdBy, Instant createdAt) {
        this.id = id;
        this.constructionSiteId = constructionSiteId;
        this.name = name;
        this.createdBy = createdBy;
        this.createdAt = createdAt;
    }

    public UUID getId() {
        return id;
    }

    public UUID getConstructionSiteId() {
        return constructionSiteId;
    }

    public String getName() {
        return name;
    }

    public UUID getCreatedBy() {
        return createdBy;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
