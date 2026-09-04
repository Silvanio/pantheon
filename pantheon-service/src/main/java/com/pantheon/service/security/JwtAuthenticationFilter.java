package com.pantheon.service.security;

import com.pantheon.service.entity.AppUser;
import com.pantheon.service.repository.AppUserRepository;
import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;
import java.util.UUID;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Authenticates REST and SSE requests using the JWT issued at login, regardless of
 * whether the user logged in via Google OAuth2 or email/password. Sets the plain
 * {@link AppUser} as the request principal.
 */
@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtService jwtService;
    private final AppUserRepository userRepository;

    public JwtAuthenticationFilter(JwtService jwtService, AppUserRepository userRepository) {
        this.jwtService = jwtService;
        this.userRepository = userRepository;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        String token = resolveToken(request);
        if (token != null) {
            jwtService.parse(token)
                    .map(Claims::getSubject)
                    .map(UUID::fromString)
                    .flatMap(userRepository::findById)
                    .ifPresent(this::authenticate);
        }
        filterChain.doFilter(request, response);
    }

    /**
     * Prefers the Authorization header (used by regular REST calls). Falls back to an
     * "access_token" query parameter because the browser's native EventSource API
     * (used by pantheon-web's SSE client) cannot set custom request headers.
     */
    private String resolveToken(HttpServletRequest request) {
        String header = request.getHeader("Authorization");
        if (header != null && header.startsWith("Bearer ")) {
            return header.substring("Bearer ".length());
        }
        return request.getParameter("access_token");
    }

    private void authenticate(AppUser user) {
        var authentication = new UsernamePasswordAuthenticationToken(
                user, null, List.of(new SimpleGrantedAuthority("ROLE_USER")));
        SecurityContextHolder.getContext().setAuthentication(authentication);
    }
}
