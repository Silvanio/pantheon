package com.pantheon.service.sse;

import java.util.UUID;
import tools.jackson.databind.JsonNode;

/**
 * Message published to the "pantheon.sse.events" fanout exchange. A null {@code companyId}
 * marks an unscoped (global) event, delivered to every connected client rather than filtered
 * to one company's emitters — see {@link SseBroadcaster}.
 */
public record SseEventEnvelope(String eventName, UUID companyId, JsonNode payload) {
}
