package com.pantheon.service.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

import com.pantheon.service.entity.AppUser;
import com.pantheon.service.exception.EmailAlreadyRegisteredException;
import com.pantheon.service.messaging.EventPublisher;
import com.pantheon.service.repository.AppUserRepository;
import com.pantheon.service.sse.SseBroadcaster;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

@ExtendWith(MockitoExtension.class)
class AppUserServiceTest {

    @Mock
    private AppUserRepository repository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private EventPublisher eventPublisher;

    @Mock
    private SseBroadcaster sseBroadcaster;

    private AppUserService service;

    @BeforeEach
    void setUp() {
        service = new AppUserService(repository, passwordEncoder, eventPublisher, sseBroadcaster);
        lenient().when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        lenient().when(passwordEncoder.encode(any())).thenReturn("encoded");
    }

    @Test
    void registerCompletesExistingPendingRegistrationAccount() {
        AppUser pending = AppUser.preRegistration(UUID.randomUUID(), "invitee@example.com", Instant.now());
        when(repository.findByEmail("invitee@example.com")).thenReturn(Optional.of(pending));

        AppUser result = service.registerWithPassword("invitee@example.com", "password123", "Novo Membro");

        assertThat(result).isSameAs(pending);
        assertThat(result.isPendingRegistration()).isFalse();
        assertThat(result.getPasswordHash()).isEqualTo("encoded");
        assertThat(result.getDisplayName()).isEqualTo("Novo Membro");
    }

    @Test
    void registerStillRejectsAnAlreadyActiveEmail() {
        AppUser active = new AppUser(
                UUID.randomUUID(), "taken@example.com", "Taken", "hash", null, Instant.now(), Instant.now());
        when(repository.findByEmail("taken@example.com")).thenReturn(Optional.of(active));

        assertThatThrownBy(() -> service.registerWithPassword("taken@example.com", "password123", "X"))
                .isInstanceOf(EmailAlreadyRegisteredException.class);
    }
}
