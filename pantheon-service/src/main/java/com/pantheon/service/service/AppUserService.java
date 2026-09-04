package com.pantheon.service.service;

import com.pantheon.service.messaging.EventPublisher;
import com.pantheon.service.sse.SseBroadcaster;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.pantheon.service.entity.AppUser;
import com.pantheon.service.exception.EmailAlreadyRegisteredException;
import com.pantheon.service.repository.AppUserRepository;
@Service
public class AppUserService {

    private final AppUserRepository repository;
    private final PasswordEncoder passwordEncoder;
    private final EventPublisher eventPublisher;
    private final SseBroadcaster sseBroadcaster;

    public AppUserService(
            AppUserRepository repository,
            PasswordEncoder passwordEncoder,
            EventPublisher eventPublisher,
            SseBroadcaster sseBroadcaster) {
        this.repository = repository;
        this.passwordEncoder = passwordEncoder;
        this.eventPublisher = eventPublisher;
        this.sseBroadcaster = sseBroadcaster;
    }

    public AppUser registerWithPassword(String email, String rawPassword, String displayName) {
        AppUser existing = repository.findByEmail(email).orElse(null);
        if (existing != null) {
            // A pre-registration account (created when this email was invited to a project
            // before it had an account) is completed here rather than being rejected.
            if (existing.isPendingRegistration()) {
                existing.completeRegistration(passwordEncoder.encode(rawPassword), displayName);
                repository.save(existing);
                publishUserRegistered(existing);
                return existing;
            }
            throw new EmailAlreadyRegisteredException(email);
        }
        Instant now = Instant.now();
        AppUser user = new AppUser(UUID.randomUUID(), email, displayName, passwordEncoder.encode(rawPassword), null, now, now);
        repository.save(user);
        publishUserRegistered(user);
        return user;
    }

    /**
     * Finds the local user matching a Google login, linking the Google subject to an
     * existing email/password account if one matches, or creating a new user otherwise.
     */
    public AppUser findOrCreateFromGoogle(String googleSubject, String email, String displayName) {
        return repository.findByGoogleSubject(googleSubject)
                .or(() -> repository.findByEmail(email).map(existing -> {
                    existing.linkGoogleSubject(googleSubject);
                    return repository.save(existing);
                }))
                .orElseGet(() -> {
                    AppUser user = repository.save(
                            new AppUser(UUID.randomUUID(), email, displayName, null, googleSubject, Instant.now(), Instant.now()));
                    publishUserRegistered(user);
                    return user;
                });
    }

    private void publishUserRegistered(AppUser user) {
        Map<String, String> payload = Map.of(
                "userId", user.getId().toString(),
                "email", user.getEmail(),
                "displayName", user.getDisplayName());
        eventPublisher.publish("user-registered", payload);
        sseBroadcaster.broadcast("user-registered", payload);
    }
}
