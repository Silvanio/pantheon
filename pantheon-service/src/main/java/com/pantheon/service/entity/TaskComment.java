package com.pantheon.service.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

/** A comment on a {@link TaskCard}. See {@code obra-tasks-board}. */
@Entity
@Table(name = "task_comment")
public class TaskComment {

    @Id
    private UUID id;

    @Column(name = "card_id", nullable = false)
    private UUID cardId;

    @Column(name = "author_id", nullable = false)
    private UUID authorId;

    @Column(nullable = false)
    private String body;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected TaskComment() {
        // JPA
    }

    public TaskComment(UUID id, UUID cardId, UUID authorId, String body, Instant createdAt) {
        this.id = id;
        this.cardId = cardId;
        this.authorId = authorId;
        this.body = body;
        this.createdAt = createdAt;
    }

    public UUID getId() {
        return id;
    }

    public UUID getCardId() {
        return cardId;
    }

    public UUID getAuthorId() {
        return authorId;
    }

    public String getBody() {
        return body;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
