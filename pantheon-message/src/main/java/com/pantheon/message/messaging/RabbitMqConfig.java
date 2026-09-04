package com.pantheon.message.messaging;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.support.converter.JacksonJsonMessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import tools.jackson.databind.json.JsonMapper;

/**
 * Declares the shared "pantheon.events" topic exchange contract (see design.md) and the
 * durable queue/bindings pantheon-message consumes from, including a dead-letter path for
 * messages that fail processing.
 */
@Configuration
public class RabbitMqConfig {

    public static final String EXCHANGE = "pantheon.events";
    public static final String QUEUE = "pantheon-message.inbox";
    public static final String ROUTING_KEY_PATTERN = "message.#";

    public static final String DEAD_LETTER_EXCHANGE = "pantheon.events.dlx";
    public static final String DEAD_LETTER_QUEUE = "pantheon-message.inbox.dlq";

    @Bean
    public TopicExchange eventsExchange() {
        return new TopicExchange(EXCHANGE, true, false);
    }

    @Bean
    public DirectExchange deadLetterExchange() {
        return new DirectExchange(DEAD_LETTER_EXCHANGE, true, false);
    }

    @Bean
    public Queue inboxQueue() {
        return QueueBuilder.durable(QUEUE)
                .withArgument("x-dead-letter-exchange", DEAD_LETTER_EXCHANGE)
                .withArgument("x-dead-letter-routing-key", QUEUE)
                .build();
    }

    @Bean
    public Queue inboxDeadLetterQueue() {
        return QueueBuilder.durable(DEAD_LETTER_QUEUE).build();
    }

    @Bean
    public Binding inboxBinding(Queue inboxQueue, TopicExchange eventsExchange) {
        return BindingBuilder.bind(inboxQueue).to(eventsExchange).with(ROUTING_KEY_PATTERN);
    }

    @Bean
    public Binding deadLetterBinding(Queue inboxDeadLetterQueue, DirectExchange deadLetterExchange) {
        return BindingBuilder.bind(inboxDeadLetterQueue).to(deadLetterExchange).with(QUEUE);
    }

    @Bean
    public JacksonJsonMessageConverter jsonMessageConverter(JsonMapper jsonMapper) {
        return new JacksonJsonMessageConverter(jsonMapper);
    }
}
