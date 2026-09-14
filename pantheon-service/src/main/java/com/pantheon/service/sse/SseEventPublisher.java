package com.pantheon.service.sse;

import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.AmqpException;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

/**
 * Publishes SSE-worthy events to the "pantheon.sse.events" fanout exchange so every
 * pantheon-service instance's {@link SseBroadcaster} learns about them, not just the instance
 * that produced the event. A publish failure is logged and re-thrown rather than swallowed,
 * matching {@code EventPublisher}'s behavior for the "pantheon.events" exchange.
 */
@Component
public class SseEventPublisher {

    private static final Logger log = LoggerFactory.getLogger(SseEventPublisher.class);

    private final RabbitTemplate rabbitTemplate;
    private final ObjectMapper objectMapper;

    public SseEventPublisher(RabbitTemplate rabbitTemplate, ObjectMapper objectMapper) {
        this.rabbitTemplate = rabbitTemplate;
        this.objectMapper = objectMapper;
    }

    /** Publishes an event scoped to one company; only that company's connected clients receive it. */
    public void publishToCompany(UUID companyId, String eventName, Object payload) {
        publish(new SseEventEnvelope(eventName, companyId, objectMapper.valueToTree(payload)));
    }

    /** Publishes an event with no company scope; every connected client receives it. */
    public void publishGlobal(String eventName, Object payload) {
        publish(new SseEventEnvelope(eventName, null, objectMapper.valueToTree(payload)));
    }

    private void publish(SseEventEnvelope envelope) {
        try {
            rabbitTemplate.convertAndSend(SseRabbitConfig.EXCHANGE, "", envelope);
        } catch (AmqpException e) {
            log.error("Failed to publish SSE event eventName={} companyId={}", envelope.eventName(), envelope.companyId(), e);
            throw new SseEventPublishException("Failed to publish SSE event: " + envelope.eventName(), e);
        }
    }
}
