package com.pantheon.service.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.util.UUID;

/** Join row attaching one {@link TaskLabel} to one {@link TaskCard}. See {@code obra-tasks-board}. */
@Entity
@Table(name = "task_card_label")
public class TaskCardLabel {

    @Id
    private UUID id;

    @Column(name = "card_id", nullable = false)
    private UUID cardId;

    @Column(name = "label_id", nullable = false)
    private UUID labelId;

    protected TaskCardLabel() {
        // JPA
    }

    public TaskCardLabel(UUID id, UUID cardId, UUID labelId) {
        this.id = id;
        this.cardId = cardId;
        this.labelId = labelId;
    }

    public UUID getId() {
        return id;
    }

    public UUID getCardId() {
        return cardId;
    }

    public UUID getLabelId() {
        return labelId;
    }
}
