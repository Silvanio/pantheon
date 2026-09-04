package com.pantheon.service.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.pantheon.service.entity.UserProfile;
import com.pantheon.service.repository.UserProfileRepository;
@ExtendWith(MockitoExtension.class)
class UserProfileServiceTest {

    @Mock
    private UserProfileRepository repository;

    @Test
    void createsProfileWhenNoneExists() {
        UUID userId = UUID.randomUUID();
        when(repository.findByUserId(userId)).thenReturn(Optional.empty());
        when(repository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        UserProfileService service = new UserProfileService(repository);
        UserProfile profile = service.upsert(userId, "12345678900", "Acme", "Rua Um", "01000-000");

        assertThat(profile.getUserId()).isEqualTo(userId);
        assertThat(profile.getCnpjCpf()).isEqualTo("12345678900");
    }

    @Test
    void updatesExistingProfileInPlace() {
        UUID userId = UUID.randomUUID();
        UserProfile existing = new UserProfile(
                UUID.randomUUID(), userId, "old-doc", "Old Name", "Old Address", "00000-000", Instant.now());
        when(repository.findByUserId(userId)).thenReturn(Optional.of(existing));

        UserProfileService service = new UserProfileService(repository);
        UserProfile updated = service.upsert(userId, "new-doc", "New Name", "New Address", "11111-111");

        assertThat(updated.getId()).isEqualTo(existing.getId());
        assertThat(updated.getCnpjCpf()).isEqualTo("new-doc");
        assertThat(updated.getLegalName()).isEqualTo("New Name");
    }
}
