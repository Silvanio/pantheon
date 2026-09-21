package com.pantheon.service.service;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.Mockito.verifyNoInteractions;

import com.pantheon.service.repository.DeviceTokenRepository;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class PushNotificationServiceTest {

    @Mock
    private DeviceTokenRepository deviceTokenRepository;

    @Test
    void sendToUserNoOpsWhenFirebaseIsNotConfigured() {
        DeviceTokenService deviceTokenService = new DeviceTokenService(deviceTokenRepository);
        PushNotificationService service = new PushNotificationService(deviceTokenService, "");
        service.initialize();

        assertThatCode(() -> service.sendToUser(UUID.randomUUID(), "title", "body", Map.of()))
                .doesNotThrowAnyException();
        verifyNoInteractions(deviceTokenRepository);
    }
}
