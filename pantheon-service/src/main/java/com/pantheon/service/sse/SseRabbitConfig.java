package com.pantheon.service.sse;

import org.springframework.amqp.core.FanoutExchange;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Declares the "pantheon.sse.events" fanout exchange used to replicate SSE-worthy events to
 * every pantheon-service instance, so a client's local {@link SseBroadcaster} stays correct
 * regardless of which instance produced the event. Separate from the "pantheon.events" topic
 * exchange (see {@code RabbitMqConfig}), which carries durable work items for pantheon-message
 * and should not be mixed with ephemeral broadcast traffic. Each instance binds its own
 * exclusive, auto-delete queue to this exchange (see {@link SseEventListener}); the exchange
 * itself stays durable so it survives a broker restart independent of any pod's lifecycle.
 */
@Configuration
public class SseRabbitConfig {

    public static final String EXCHANGE = "pantheon.sse.events";

    @Bean
    public FanoutExchange sseEventsExchange() {
        return new FanoutExchange(EXCHANGE, true, false);
    }
}
