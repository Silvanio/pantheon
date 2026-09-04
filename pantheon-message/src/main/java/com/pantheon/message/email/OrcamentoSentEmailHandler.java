package com.pantheon.message.email;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;

/** Renders and sends the "orçamento enviado" notification for an {@code orcamento-sent} event. */
@Component
public class OrcamentoSentEmailHandler {

    public static final String EVENT_TYPE = "orcamento-sent";

    private final EmailSender emailSender;
    private final ObjectMapper objectMapper;

    public OrcamentoSentEmailHandler(EmailSender emailSender, ObjectMapper objectMapper) {
        this.emailSender = emailSender;
        this.objectMapper = objectMapper;
    }

    public void handle(JsonNode payloadNode) {
        Payload payload = objectMapper.convertValue(payloadNode, Payload.class);

        String subject = "Novo orçamento para a obra " + payload.constructionSiteName();
        String body = "Um novo orçamento foi enviado para sua análise na obra \"" + payload.constructionSiteName()
                + "\". Acesse o Pantheon para aprovar ou rejeitar.\n";

        emailSender.send(payload.clientEmail(), subject, body);
    }

    record Payload(
            String orcamentoId, String materialRequestId, String constructionSiteId, String constructionSiteName,
            String clientEmail) {
    }
}
