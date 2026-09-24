package com.pantheon.service.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.Optional;
import java.util.UUID;
import javax.crypto.SecretKey;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Issues and validates the single, self-contained session token used by pantheon-service
 * regardless of login method (Google OAuth2 or email/password) — see design.md
 * "Unified session token issuance".
 */
@Component
public class JwtService {

    private final SecretKey key;
    private final Duration expiration;

    public JwtService(
            @Value("${pantheon.jwt.secret}") String secret,
            @Value("${pantheon.jwt.expiration-minutes}") long expirationMinutes) {
        this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.expiration = Duration.ofMinutes(expirationMinutes);
    }

    public String issueToken(UUID userId, String email) {
        return issueToken(userId, email, false);
    }

    /**
     * {@code superAdmin} is embedded purely as a client-side routing hint (like {@code email},
     * decoded but never trusted for authorization) — every server-side check re-reads the flag
     * fresh from {@code AppUser} via {@link com.pantheon.service.service.PlatformAdminService}.
     */
    public String issueToken(UUID userId, String email, boolean superAdmin) {
        Instant now = Instant.now();
        return Jwts.builder()
                .subject(userId.toString())
                .claim("email", email)
                .claim("superAdmin", superAdmin)
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plus(expiration)))
                .signWith(key)
                .compact();
    }

    public Optional<Claims> parse(String token) {
        try {
            return Optional.of(Jwts.parser().verifyWith(key).build().parseSignedClaims(token).getPayload());
        } catch (JwtException | IllegalArgumentException e) {
            return Optional.empty();
        }
    }
}
