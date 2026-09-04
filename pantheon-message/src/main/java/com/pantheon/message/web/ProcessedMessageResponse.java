package com.pantheon.message.web;

import com.pantheon.message.domain.ProcessedMessage;
import java.time.Instant;
import java.util.UUID;

public record ProcessedMessageResponse(
        UUID id,
        UUID correlationId,
        String type,
        String payload,
        Instant occurredAt,
        Instant processedAt) {

    public static ProcessedMessageResponse from(ProcessedMessage message) {
        return new ProcessedMessageResponse(
                message.getId(),
                message.getCorrelationId(),
                message.getType(),
                message.getPayload(),
                message.getOccurredAt(),
                message.getProcessedAt());
    }
}
