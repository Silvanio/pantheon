package com.pantheon.service.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

/**
 * A Tasks board column, scoped to the owning {@link Company} (not to a single construction
 * site) so every obra of that company renders the same board structure. Created, renamed,
 * reordered, and deleted only by a company admin — see {@code company-task-columns}.
 */
@Entity
@Table(name = "task_column")
public class TaskColumn {

    @Id
    private UUID id;

    @Column(name = "company_id", nullable = false)
    private UUID companyId;

    @Column(nullable = false)
    private String name;

    @Column(name = "sort_order", nullable = false)
    private int sortOrder;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected TaskColumn() {
        // JPA
    }

    public TaskColumn(UUID id, UUID companyId, String name, int sortOrder, Instant createdAt) {
        this.id = id;
        this.companyId = companyId;
        this.name = name;
        this.sortOrder = sortOrder;
        this.createdAt = createdAt;
    }

    public void rename(String name) {
        this.name = name;
    }

    public void reorder(int sortOrder) {
        this.sortOrder = sortOrder;
    }

    public UUID getId() {
        return id;
    }

    public UUID getCompanyId() {
        return companyId;
    }

    public String getName() {
        return name;
    }

    public int getSortOrder() {
        return sortOrder;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
