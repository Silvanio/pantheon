package com.pantheon.service.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

/**
 * A folder in a construction site's "Projetos" explorer. {@code parentId == null} means it sits
 * at the site's root; otherwise it is a child of another {@code SiteDocumentProject} — there is
 * no distinct "sub-folder" entity, nesting is just this same type pointing at its parent. May
 * optionally link to a {@code TaskCard}; that link is cleared (never cascaded) when the task is
 * deleted. See redesign-site-projects-as-folder-explorer's design.md.
 */
@Entity
@Table(name = "site_document_project")
public class SiteDocumentProject {

    @Id
    private UUID id;

    @Column(name = "construction_site_id", nullable = false)
    private UUID constructionSiteId;

    @Column(name = "parent_id")
    private UUID parentId;

    @Column(nullable = false)
    private String name;

    @Column(name = "task_card_id")
    private UUID taskCardId;

    @Column(name = "created_by", nullable = false)
    private UUID createdBy;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_by", nullable = false)
    private UUID updatedBy;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected SiteDocumentProject() {
        // JPA
    }

    public SiteDocumentProject(
            UUID id, UUID constructionSiteId, UUID parentId, String name, UUID taskCardId, UUID createdBy,
            Instant createdAt, UUID updatedBy, Instant updatedAt) {
        this.id = id;
        this.constructionSiteId = constructionSiteId;
        this.parentId = parentId;
        this.name = name;
        this.taskCardId = taskCardId;
        this.createdBy = createdBy;
        this.createdAt = createdAt;
        this.updatedBy = updatedBy;
        this.updatedAt = updatedAt;
    }

    public void rename(String name, UUID actingUserId, Instant now) {
        this.name = name;
        this.updatedBy = actingUserId;
        this.updatedAt = now;
    }

    public void linkTask(UUID taskCardId, UUID actingUserId, Instant now) {
        this.taskCardId = taskCardId;
        this.updatedBy = actingUserId;
        this.updatedAt = now;
    }

    public UUID getId() {
        return id;
    }

    public UUID getConstructionSiteId() {
        return constructionSiteId;
    }

    public UUID getParentId() {
        return parentId;
    }

    public String getName() {
        return name;
    }

    public UUID getTaskCardId() {
        return taskCardId;
    }

    public UUID getCreatedBy() {
        return createdBy;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public UUID getUpdatedBy() {
        return updatedBy;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
