package com.pantheon.message.email;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

@ExtendWith(MockitoExtension.class)
class InvitationEmailHandlerTest {

    private final JsonMapper jsonMapper = JsonMapper.builder().build();

    @Mock
    private EmailSender emailSender;

    private InvitationEmailHandler handler(String webBaseUrl) {
        return new InvitationEmailHandler(emailSender, jsonMapper, webBaseUrl);
    }

    private JsonNode payload(boolean requiresRegistration) {
        return jsonMapper.createObjectNode()
                .put("invitationId", "11111111-1111-1111-1111-111111111111")
                .put("email", "novo@example.com")
                .put("token", "raw-token-abc")
                .put("projectId", "22222222-2222-2222-2222-222222222222")
                .put("projectName", "Obra Centro")
                .put("inviterName", "Maria")
                .put("requiresRegistration", requiresRegistration);
    }

    @Test
    void sendsEmailWithInvitationLink() {
        handler("http://localhost:8080/").handle(payload(true));

        ArgumentCaptor<String> to = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> subject = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> body = ArgumentCaptor.forClass(String.class);
        verify(emailSender).send(to.capture(), subject.capture(), body.capture());

        assertThat(to.getValue()).isEqualTo("novo@example.com");
        assertThat(subject.getValue()).contains("Obra Centro");
        assertThat(body.getValue()).contains("http://localhost:8080/invitations/raw-token-abc");
        assertThat(body.getValue()).contains("Maria");
        assertThat(body.getValue()).contains("concluir seu cadastro");
    }

    @Test
    void existingAccountEmailOmitsRegistrationWording() {
        handler("http://localhost:8080").handle(payload(false));

        ArgumentCaptor<String> body = ArgumentCaptor.forClass(String.class);
        verify(emailSender).send(org.mockito.ArgumentMatchers.eq("novo@example.com"),
                org.mockito.ArgumentMatchers.anyString(), body.capture());
        assertThat(body.getValue()).contains("revisar e aceitar o convite");
        assertThat(body.getValue()).doesNotContain("concluir seu cadastro");
    }
}
