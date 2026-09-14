package com.pantheon.service.sse;

import org.springframework.amqp.core.ExchangeTypes;
import org.springframework.amqp.rabbit.annotation.Exchange;
import org.springframework.amqp.rabbit.annotation.Queue;
import org.springframework.amqp.rabbit.annotation.QueueBinding;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

/**
 * Rehydrates SSE-worthy events published by any pantheon-service instance into this
 * instance's local {@link SseBroadcaster}. Each instance binds its own exclusive, auto-delete
 * queue to the fanout exchange (rather than a shared named queue), so every instance receives
 * every event instead of the broker round-robining messages across instances the way it would
 * for a work queue. See design.md "Cross-pod fan-out".
 */
@Component
public class SseEventListener {

    private final SseBroadcaster broadcaster;

    public SseEventListener(SseBroadcaster broadcaster) {
        this.broadcaster = broadcaster;
    }

    @RabbitListener(bindings = @QueueBinding(
            value = @Queue(exclusive = "true", autoDelete = "true", durable = "false"),
            exchange = @Exchange(value = SseRabbitConfig.EXCHANGE, type = ExchangeTypes.FANOUT, durable = "true")))
    public void onMessage(SseEventEnvelope envelope) {
        if (envelope.companyId() != null) {
            broadcaster.broadcastToCompany(envelope.companyId(), envelope.eventName(), envelope.payload());
        } else {
            broadcaster.broadcast(envelope.eventName(), envelope.payload());
        }
    }
}
