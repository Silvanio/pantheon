package com.pantheon.service.messaging;

import tools.jackson.databind.JsonNode;
import java.time.Instant;
import java.util.UUID;

/**
 * Shared message envelope published to the "pantheon.events" exchange and consumed
 * by pantheon-message. See design.md "RabbitMQ contract".
 */
public record MessageEnvelope(String type, JsonNode payload, Instant occurredAt, UUID correlationId) {
}
