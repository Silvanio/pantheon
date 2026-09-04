package com.pantheon.service.messaging;

import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.support.converter.JacksonJsonMessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import tools.jackson.databind.json.JsonMapper;

/**
 * Declares the shared "pantheon.events" topic exchange this service publishes to.
 * pantheon-message declares the same exchange plus the queue/bindings that consume from it.
 */
@Configuration
public class RabbitMqConfig {

    public static final String EXCHANGE = "pantheon.events";

    @Bean
    public TopicExchange eventsExchange() {
        return new TopicExchange(EXCHANGE, true, false);
    }

    @Bean
    public JacksonJsonMessageConverter jsonMessageConverter(JsonMapper jsonMapper) {
        return new JacksonJsonMessageConverter(jsonMapper);
    }
}
