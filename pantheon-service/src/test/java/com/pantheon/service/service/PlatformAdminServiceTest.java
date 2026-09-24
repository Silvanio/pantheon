package com.pantheon.service.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import com.pantheon.service.entity.AppUser;
import com.pantheon.service.exception.NotSuperAdminException;
import com.pantheon.service.repository.AppUserRepository;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class PlatformAdminServiceTest {

    @Mock
    private AppUserRepository userRepository;

    private PlatformAdminService service;

    @BeforeEach
    void setUp() {
        service = new PlatformAdminService(userRepository);
    }

    // superAdmin has no domain setter (it's seeded exclusively via migration) — set directly for this test.
    private AppUser user(boolean superAdmin) {
        AppUser user = new AppUser(UUID.randomUUID(), "user@example.com", "User", null, null, Instant.now(), Instant.now());
        ReflectionTestUtils.setField(user, "superAdmin", superAdmin);
        return user;
    }

    @Test
    void isSuperAdminTrueForFlaggedUser() {
        AppUser user = user(true);
        when(userRepository.findById(user.getId())).thenReturn(Optional.of(user));

        assertThat(service.isSuperAdmin(user.getId())).isTrue();
    }

    @Test
    void isSuperAdminFalseForRegularUser() {
        AppUser user = user(false);
        when(userRepository.findById(user.getId())).thenReturn(Optional.of(user));

        assertThat(service.isSuperAdmin(user.getId())).isFalse();
    }

    @Test
    void isSuperAdminFalseWhenUserNotFound() {
        UUID missingId = UUID.randomUUID();
        when(userRepository.findById(missingId)).thenReturn(Optional.empty());

        assertThat(service.isSuperAdmin(missingId)).isFalse();
    }

    @Test
    void requireSuperAdminThrowsForNonSuperAdmin() {
        AppUser user = user(false);
        when(userRepository.findById(user.getId())).thenReturn(Optional.of(user));

        assertThatThrownBy(() -> service.requireSuperAdmin(user.getId())).isInstanceOf(NotSuperAdminException.class);
    }
}
