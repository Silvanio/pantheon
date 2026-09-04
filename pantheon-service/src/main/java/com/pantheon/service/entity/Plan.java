package com.pantheon.service.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.util.UUID;

/**
 * A registered plan (Basic, Profissional, Ilimitado), seeded via Flyway. Read-only from the
 * application's perspective: the catalog is data so its active-site limit can change without a
 * code deploy, not because the application ever creates/edits plans at runtime.
 */
@Entity
@Table(name = "plan")
public class Plan {

    @Id
    private UUID id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PlanCode code;

    @Column(nullable = false)
    private String name;

    @Column(name = "active_site_limit")
    private Integer activeSiteLimit;

    @Column(name = "sort_order", nullable = false)
    private int sortOrder;

    protected Plan() {
        // JPA
    }

    public UUID getId() {
        return id;
    }

    public PlanCode getCode() {
        return code;
    }

    public String getName() {
        return name;
    }

    /** Maximum number of active construction sites a company on this plan may have, or {@code null} if unbounded. */
    public Integer getActiveSiteLimit() {
        return activeSiteLimit;
    }

    public int getSortOrder() {
        return sortOrder;
    }
}
