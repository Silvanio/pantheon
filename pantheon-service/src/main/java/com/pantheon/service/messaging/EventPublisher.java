package com.pantheon.service.messaging;

import tools.jackson.databind.ObjectMapper;
import java.time.Instant;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.AmqpException;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

@Component
public class EventPublisher {

    private static final Logger log = LoggerFactory.getLogger(EventPublisher.class);

    private final RabbitTemplate rabbitTemplate;
    private final ObjectMapper objectMapper;

    public EventPublisher(RabbitTemplate rabbitTemplate, ObjectMapper objectMapper) {
        this.rabbitTemplate = rabbitTemplate;
        this.objectMapper = objectMapper;
    }

    /**
     * Publishes a domain event to the "pantheon.events" exchange. A broker/publish
     * failure is logged and re-thrown rather than swallowed, so the triggering request
     * surfaces the failure instead of silently reporting success.
     */
    public void publish(String type, Object payload) {
        UUID correlationId = UUID.randomUUID();
        MessageEnvelope envelope = new MessageEnvelope(type, objectMapper.valueToTree(payload), Instant.now(), correlationId);
        String routingKey = "message." + type;
        try {
            rabbitTemplate.convertAndSend(RabbitMqConfig.EXCHANGE, routingKey, envelope);
        } catch (AmqpException e) {
            log.error("Failed to publish event type={} correlationId={}", type, correlationId, e);
            throw new EventPublishException("Failed to publish event: " + type, e);
        }
    }
}
