package com.pantheon.message.security;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Interim token validation: compares against a single pre-shared secret read from
 * config (env var {@code PANTHEON_API_TOKEN}). Kept behind {@link ApiTokenValidator}
 * so it can be swapped for a rotatable, DB-backed scheme later without touching callers.
 */
@Component
public class EnvApiTokenValidator implements ApiTokenValidator {

    private final byte[] expectedTokenBytes;

    public EnvApiTokenValidator(@Value("${pantheon.api-token}") String expectedToken) {
        this.expectedTokenBytes = expectedToken.getBytes(StandardCharsets.UTF_8);
    }

    @Override
    public boolean isValid(String token) {
        if (token == null || token.isBlank()) {
            return false;
        }
        return MessageDigest.isEqual(token.getBytes(StandardCharsets.UTF_8), expectedTokenBytes);
    }
}
