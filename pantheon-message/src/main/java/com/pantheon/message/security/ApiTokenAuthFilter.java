package com.pantheon.message.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Requires a valid pre-shared API token on every request except Actuator health checks.
 * pantheon-service sends this token on every call it makes into pantheon-message.
 */
@Component
public class ApiTokenAuthFilter extends OncePerRequestFilter {

    public static final String HEADER_NAME = "X-API-Token";

    private final ApiTokenValidator validator;

    public ApiTokenAuthFilter(ApiTokenValidator validator) {
        this.validator = validator;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return request.getRequestURI().startsWith("/actuator");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        String token = request.getHeader(HEADER_NAME);
        if (!validator.isValid(token)) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setContentType("application/json");
            response.getWriter().write("{\"error\":\"missing or invalid API token\"}");
            return;
        }
        filterChain.doFilter(request, response);
    }
}
