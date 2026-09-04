package com.pantheon.message.messaging;

import tools.jackson.databind.JsonNode;
import java.time.Instant;
import java.util.UUID;

/**
 * Shared message envelope published by pantheon-service to the "pantheon.events"
 * exchange and consumed here. See design.md "RabbitMQ contract".
 */
public record MessageEnvelope(String type, JsonNode payload, Instant occurredAt, UUID correlationId) {
}
