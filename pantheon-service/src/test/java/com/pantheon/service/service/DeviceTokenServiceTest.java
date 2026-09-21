package com.pantheon.service.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.pantheon.service.entity.DevicePlatform;
import com.pantheon.service.entity.DeviceToken;
import com.pantheon.service.repository.DeviceTokenRepository;
import com.pantheon.service.service.DeviceTokenService.DeviceTokenRegistrationData;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class DeviceTokenServiceTest {

    @Mock
    private DeviceTokenRepository deviceTokenRepository;

    private DeviceTokenService service;

    @BeforeEach
    void setUp() {
        service = new DeviceTokenService(deviceTokenRepository);
    }

    @Test
    void registersANewTokenWhenNotSeenBefore() {
        UUID userId = UUID.randomUUID();
        when(deviceTokenRepository.findByToken("token-1")).thenReturn(Optional.empty());

        service.register(userId, new DeviceTokenRegistrationData("token-1", DevicePlatform.ANDROID));

        verify(deviceTokenRepository).save(any(DeviceToken.class));
    }

    @Test
    void reRegisteringTheSameTokenForTheSameUserDoesNotDuplicateIt() {
        UUID userId = UUID.randomUUID();
        DeviceToken existing = new DeviceToken(UUID.randomUUID(), userId, DevicePlatform.ANDROID, "token-1", Instant.now());
        when(deviceTokenRepository.findByToken("token-1")).thenReturn(Optional.of(existing));

        service.register(userId, new DeviceTokenRegistrationData("token-1", DevicePlatform.ANDROID));

        verify(deviceTokenRepository, never()).save(any());
        verify(deviceTokenRepository, never()).delete(any());
    }

    @Test
    void reAssignsATokenPreviouslyRegisteredToADifferentUser() {
        UUID previousUserId = UUID.randomUUID();
        UUID newUserId = UUID.randomUUID();
        DeviceToken existing =
                new DeviceToken(UUID.randomUUID(), previousUserId, DevicePlatform.IOS, "token-1", Instant.now());
        when(deviceTokenRepository.findByToken("token-1")).thenReturn(Optional.of(existing));

        service.register(newUserId, new DeviceTokenRegistrationData("token-1", DevicePlatform.IOS));

        verify(deviceTokenRepository).delete(existing);
        verify(deviceTokenRepository, times(1)).save(any(DeviceToken.class));
    }

    @Test
    void unregisterDelegatesToRepository() {
        UUID userId = UUID.randomUUID();

        service.unregister(userId, "token-1");

        verify(deviceTokenRepository).deleteByUserIdAndToken(userId, "token-1");
    }
}
