package com.pantheon.message.email;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;

/** Renders and sends the "orçamento aguardando aprovação" notification for an {@code orcamento-approval-step-pending} event. */
@Component
public class OrcamentoApprovalStepPendingEmailHandler {

    public static final String EVENT_TYPE = "orcamento-approval-step-pending";

    private final EmailSender emailSender;
    private final ObjectMapper objectMapper;

    public OrcamentoApprovalStepPendingEmailHandler(EmailSender emailSender, ObjectMapper objectMapper) {
        this.emailSender = emailSender;
        this.objectMapper = objectMapper;
    }

    public void handle(JsonNode payloadNode) {
        Payload payload = objectMapper.convertValue(payloadNode, Payload.class);

        String subject = "Orçamento aguardando aprovação na obra " + payload.constructionSiteName();
        String body = "Um orçamento na obra \"" + payload.constructionSiteName()
                + "\" está aguardando a sua aprovação. Acesse o Pantheon para aprovar ou rejeitar.\n";

        emailSender.send(payload.recipientEmail(), subject, body);
    }

    record Payload(
            String orcamentoId, String constructionSiteId, String constructionSiteName, String approverFunction,
            String recipientEmail) {
    }
}
