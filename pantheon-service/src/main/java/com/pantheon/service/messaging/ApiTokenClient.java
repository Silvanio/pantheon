package com.pantheon.service.messaging;

import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

/**
 * REST client pantheon-service uses to call pantheon-message directly, attaching the
 * shared pre-shared API token header expected by pantheon-message's ApiTokenAuthFilter.
 */
@Component
public class ApiTokenClient {

    private final RestClient restClient;

    public ApiTokenClient(
            @Value("${pantheon.message-service.base-url}") String baseUrl,
            @Value("${pantheon.api-token}") String apiToken) {
        this.restClient = RestClient.builder()
                .baseUrl(baseUrl)
                .defaultHeader("X-API-Token", apiToken)
                .build();
    }

    public List<Map<String, Object>> listProcessedMessages() {
        return restClient.get()
                .uri("/api/messages")
                .retrieve()
                .body(new ParameterizedTypeReference<>() {});
    }
}
