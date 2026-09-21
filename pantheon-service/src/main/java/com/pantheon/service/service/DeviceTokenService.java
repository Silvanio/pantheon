package com.pantheon.service.service;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.pantheon.service.entity.DevicePlatform;
import com.pantheon.service.entity.DeviceToken;
import com.pantheon.service.repository.DeviceTokenRepository;

/** Registration for the push-notifications capability — see {@link PushNotificationService} for the sending side. */
@Service
public class DeviceTokenService {

    private final DeviceTokenRepository deviceTokenRepository;

    public DeviceTokenService(DeviceTokenRepository deviceTokenRepository) {
        this.deviceTokenRepository = deviceTokenRepository;
    }

    /**
     * Registers a token under the given user, re-assigning it if it was previously registered
     * to a different user (the same device logging in as someone else) rather than erroring on
     * the token's unique constraint.
     */
    @Transactional
    public void register(UUID userId, DeviceTokenRegistrationData data) {
        deviceTokenRepository
                .findByToken(data.token())
                .ifPresentOrElse(
                        existing -> {
                            if (!existing.getUserId().equals(userId)) {
                                deviceTokenRepository.delete(existing);
                                deviceTokenRepository.save(
                                        new DeviceToken(UUID.randomUUID(), userId, data.platform(), data.token(), Instant.now()));
                            }
                        },
                        () -> deviceTokenRepository.save(
                                new DeviceToken(UUID.randomUUID(), userId, data.platform(), data.token(), Instant.now())));
    }

    @Transactional
    public void unregister(UUID userId, String token) {
        deviceTokenRepository.deleteByUserIdAndToken(userId, token);
    }

    public List<DeviceToken> findByUserId(UUID userId) {
        return deviceTokenRepository.findByUserId(userId);
    }

    public record DeviceTokenRegistrationData(String token, DevicePlatform platform) {
    }
}
