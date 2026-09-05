package com.pantheon.message.email;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Renders and sends the team-invitation email for a {@code team-invitation-created} event.
 * The link points at pantheon-web's invitation page, built from the configured web base
 * URL and the raw token carried in the event. The invitation may target a company (staff)
 * or a construction site (obra team) — {@code membershipType} picks the wording.
 */
@Component
public class InvitationEmailHandler {

    public static final String EVENT_TYPE = "team-invitation-created";

    private final EmailSender emailSender;
    private final ObjectMapper objectMapper;
    private final String webBaseUrl;

    public InvitationEmailHandler(
            EmailSender emailSender,
            ObjectMapper objectMapper,
            @Value("${pantheon.web.base-url}") String webBaseUrl) {
        this.emailSender = emailSender;
        this.objectMapper = objectMapper;
        this.webBaseUrl = webBaseUrl.endsWith("/") ? webBaseUrl.substring(0, webBaseUrl.length() - 1) : webBaseUrl;
    }

    public void handle(JsonNode payloadNode) {
        Payload payload = objectMapper.convertValue(payloadNode, Payload.class);

        boolean isSite = "SITE".equals(payload.membershipType());
        String targetLabel = isSite ? "a obra" : "a empresa";
        String targetLabelContracted = isSite ? "da obra" : "da empresa";

        String link = webBaseUrl + "/invitations/" + payload.token();
        String subject = "Convite para " + targetLabel + " " + payload.targetName() + " no Pantheon";
        String inviter = payload.inviterName() == null || payload.inviterName().isBlank()
                ? "Alguém"
                : payload.inviterName();

        StringBuilder body = new StringBuilder();
        body.append(inviter)
                .append(" convidou você para participar ")
                .append(targetLabelContracted)
                .append(" \"")
                .append(payload.targetName())
                .append("\" no Pantheon.\n\n");
        if (payload.requiresRegistration()) {
            body.append("Você ainda não tem cadastro. Acesse o link abaixo para concluir seu cadastro ")
                    .append("e aceitar o convite:\n");
        } else {
            body.append("Acesse o link abaixo para revisar e aceitar o convite:\n");
        }
        body.append(link).append("\n\n");
        body.append("Se você não esperava este convite, ignore este e-mail.\n");

        emailSender.send(payload.email(), subject, body.toString());
    }

    record Payload(
            String email,
            String token,
            String targetName,
            String membershipType,
            String inviterName,
            boolean requiresRegistration) {
    }
}
