package com.pantheon.service.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.client.registration.InMemoryClientRegistrationRepository;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.ClientAuthenticationMethod;
import org.springframework.security.oauth2.core.oidc.IdTokenClaimNames;
import org.springframework.util.StringUtils;

/**
 * Registers the Google OAuth2 client registration, with fixed well-known endpoints
 * (CommonOAuth2Provider.GOOGLE was removed in Spring Security 7) instead of OIDC
 * discovery, so the app never needs network access to accounts.google.com at startup.
 *
 * <p>A registration is always created, falling back to placeholder credentials when
 * GOOGLE_CLIENT_ID/SECRET are unset. Google login is one of two optional login methods
 * (see proposal.md) - email/password always works - and Spring Security's own
 * OAuth2ClientWebMvcSecurityConfiguration eagerly resolves the ClientRegistrationRepository
 * bean at startup once .oauth2Login() is configured, regardless of whether it's ever used,
 * so skipping bean creation entirely (e.g. returning null) breaks startup. The placeholder
 * registration boots cleanly; Google login itself simply won't work until real credentials
 * are supplied.
 */
@Configuration
public class GoogleOAuth2Config {

    private static final String PLACEHOLDER = "unconfigured";

    @Bean
    public ClientRegistrationRepository clientRegistrationRepository(
            @Value("${pantheon.oauth2.google.client-id}") String clientId,
            @Value("${pantheon.oauth2.google.client-secret}") String clientSecret) {
        ClientRegistration google = ClientRegistration.withRegistrationId("google")
                .clientId(StringUtils.hasText(clientId) ? clientId : PLACEHOLDER)
                .clientSecret(StringUtils.hasText(clientSecret) ? clientSecret : PLACEHOLDER)
                .clientAuthenticationMethod(ClientAuthenticationMethod.CLIENT_SECRET_BASIC)
                .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
                .redirectUri("{baseUrl}/login/oauth2/code/{registrationId}")
                .scope("openid", "profile", "email")
                .authorizationUri("https://accounts.google.com/o/oauth2/v2/auth")
                .tokenUri("https://www.googleapis.com/oauth2/v4/token")
                .userInfoUri("https://www.googleapis.com/oauth2/v3/userinfo")
                .userNameAttributeName(IdTokenClaimNames.SUB)
                .jwkSetUri("https://www.googleapis.com/oauth2/v3/certs")
                .clientName("Google")
                .build();
        return new InMemoryClientRegistrationRepository(google);
    }
}
