package com.pantheon.service.security;

import com.pantheon.service.entity.AppUser;
import com.pantheon.service.service.AppUserService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

/**
 * On successful Google login, finds-or-creates the local user and redirects back to
 * pantheon-web carrying the same JWT format issued by the email/password login path.
 */
@Component
public class OAuth2LoginSuccessHandler implements AuthenticationSuccessHandler {

    private final AppUserService userService;
    private final JwtService jwtService;
    private final String webOrigin;

    public OAuth2LoginSuccessHandler(
            AppUserService userService,
            JwtService jwtService,
            @Value("${pantheon.web.cors-allowed-origin}") String webOrigin) {
        this.userService = userService;
        this.jwtService = jwtService;
        this.webOrigin = webOrigin;
    }

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response, Authentication authentication)
            throws IOException {
        OAuth2User oAuth2User = (OAuth2User) authentication.getPrincipal();
        String googleSubject = oAuth2User.getAttribute("sub");
        String email = oAuth2User.getAttribute("email");
        String name = oAuth2User.getAttribute("name");

        AppUser user = userService.findOrCreateFromGoogle(googleSubject, email, name != null ? name : email);
        String token = jwtService.issueToken(user.getId(), user.getEmail());

        String redirectUrl = UriComponentsBuilder.fromUriString(webOrigin + "/oauth2/callback")
                .queryParam("token", token)
                .build()
                .toUriString();
        response.sendRedirect(redirectUrl);
    }
}
