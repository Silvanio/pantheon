package com.pantheon.message.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "processed_message")
public class ProcessedMessage {

    @Id
    private UUID id;

    @Column(name = "correlation_id", nullable = false)
    private UUID correlationId;

    @Column(nullable = false)
    private String type;

    @Column(nullable = false, columnDefinition = "text")
    private String payload;

    @Column(name = "occurred_at", nullable = false)
    private Instant occurredAt;

    @Column(name = "processed_at", nullable = false)
    private Instant processedAt;

    protected ProcessedMessage() {
        // JPA
    }

    public ProcessedMessage(UUID id, UUID correlationId, String type, String payload, Instant occurredAt, Instant processedAt) {
        this.id = id;
        this.correlationId = correlationId;
        this.type = type;
        this.payload = payload;
        this.occurredAt = occurredAt;
        this.processedAt = processedAt;
    }

    public UUID getId() {
        return id;
    }

    public UUID getCorrelationId() {
        return correlationId;
    }

    public String getType() {
        return type;
    }

    public String getPayload() {
        return payload;
    }

    public Instant getOccurredAt() {
        return occurredAt;
    }

    public Instant getProcessedAt() {
        return processedAt;
    }
}
