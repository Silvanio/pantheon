package com.pantheon.message.messaging;

import tools.jackson.databind.ObjectMapper;
import com.pantheon.message.domain.ProcessedMessage;
import com.pantheon.message.domain.ProcessedMessageRepository;
import com.pantheon.message.email.InvitationEmailHandler;
import com.pantheon.message.email.OrcamentoSentEmailHandler;
import java.time.Instant;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.AmqpRejectAndDontRequeueException;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Component
public class EventListener {

    private static final Logger log = LoggerFactory.getLogger(EventListener.class);

    private final ProcessedMessageRepository repository;
    private final ObjectMapper objectMapper;
    private final InvitationEmailHandler invitationEmailHandler;
    private final OrcamentoSentEmailHandler orcamentoSentEmailHandler;

    public EventListener(
            ProcessedMessageRepository repository,
            ObjectMapper objectMapper,
            InvitationEmailHandler invitationEmailHandler,
            OrcamentoSentEmailHandler orcamentoSentEmailHandler) {
        this.repository = repository;
        this.objectMapper = objectMapper;
        this.invitationEmailHandler = invitationEmailHandler;
        this.orcamentoSentEmailHandler = orcamentoSentEmailHandler;
    }

    @RabbitListener(queues = RabbitMqConfig.QUEUE)
    public void onMessage(MessageEnvelope envelope) {
        try {
            String payloadJson = objectMapper.writeValueAsString(envelope.payload());

            // Side effects that must succeed before the message is acknowledged. A send
            // failure propagates so the message is retried / dead-lettered rather than lost.
            if (InvitationEmailHandler.EVENT_TYPE.equals(envelope.type())) {
                invitationEmailHandler.handle(envelope.payload());
            } else if (OrcamentoSentEmailHandler.EVENT_TYPE.equals(envelope.type())) {
                orcamentoSentEmailHandler.handle(envelope.payload());
            }

            ProcessedMessage processed = new ProcessedMessage(
                    UUID.randomUUID(),
                    envelope.correlationId(),
                    envelope.type(),
                    payloadJson,
                    envelope.occurredAt(),
                    Instant.now());
            repository.save(processed);
        } catch (Exception e) {
            log.error("Failed to process message type={} correlationId={}", envelope.type(), envelope.correlationId(), e);
            // Not acknowledged as successful: rejected without requeue, routed to the DLQ
            // configured on the queue rather than being lost or retried forever.
            throw new AmqpRejectAndDontRequeueException("Failed to process message", e);
        }
    }
}
