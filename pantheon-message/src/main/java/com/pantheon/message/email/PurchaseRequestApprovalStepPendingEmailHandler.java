package com.pantheon.message.email;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;

/** Renders and sends the "pedido de compra aguardando aprovação" notification for a {@code purchase-request-approval-step-pending} event. */
@Component
public class PurchaseRequestApprovalStepPendingEmailHandler {

    public static final String EVENT_TYPE = "purchase-request-approval-step-pending";

    private final EmailSender emailSender;
    private final ObjectMapper objectMapper;

    public PurchaseRequestApprovalStepPendingEmailHandler(EmailSender emailSender, ObjectMapper objectMapper) {
        this.emailSender = emailSender;
        this.objectMapper = objectMapper;
    }

    public void handle(JsonNode payloadNode) {
        Payload payload = objectMapper.convertValue(payloadNode, Payload.class);

        String subject = "Pedido de compra aguardando aprovação na obra " + payload.constructionSiteName();
        String body = "Um pedido de compra na obra \"" + payload.constructionSiteName()
                + "\" está aguardando a sua aprovação. Acesse o Pantheon para aprovar ou rejeitar.\n";

        emailSender.send(payload.recipientEmail(), subject, body);
    }

    record Payload(
            String purchaseRequestId, String constructionSiteId, String constructionSiteName, String approverFunction,
            String recipientEmail) {
    }
}
