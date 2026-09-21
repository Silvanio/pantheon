package com.pantheon.service.service;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.FirebaseMessagingException;
import com.google.firebase.messaging.Message;
import jakarta.annotation.PostConstruct;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.pantheon.service.entity.DeviceToken;

/**
 * Best-effort push delivery for the push-notifications capability. No-ops (logs only) when
 * {@code pantheon.push.firebase-credentials-path} isn't configured, so environments without a
 * Firebase project (dev, CI, tests) keep working unaffected — see design.md's
 * "Push delivery is best-effort and configuration-gated" requirement.
 */
@Service
public class PushNotificationService {

    private static final Logger log = LoggerFactory.getLogger(PushNotificationService.class);

    private final DeviceTokenService deviceTokenService;
    private final String credentialsPath;
    private boolean available = false;

    public PushNotificationService(
            DeviceTokenService deviceTokenService,
            @Value("${pantheon.push.firebase-credentials-path:}") String credentialsPath) {
        this.deviceTokenService = deviceTokenService;
        this.credentialsPath = credentialsPath;
    }

    @PostConstruct
    void initialize() {
        if (credentialsPath == null || credentialsPath.isBlank()) {
            log.info("Push notifications disabled: pantheon.push.firebase-credentials-path is not set.");
            return;
        }
        try (FileInputStream serviceAccount = new FileInputStream(credentialsPath)) {
            FirebaseOptions options = FirebaseOptions.builder()
                    .setCredentials(GoogleCredentials.fromStream(serviceAccount))
                    .build();
            if (FirebaseApp.getApps().isEmpty()) {
                FirebaseApp.initializeApp(options);
            }
            available = true;
            log.info("Push notifications enabled (Firebase initialized).");
        } catch (IOException | RuntimeException e) {
            log.warn("Failed to initialize Firebase for push notifications — push disabled.", e);
        }
    }

    /**
     * Sends to every device token registered for {@code userId}. A failure for one token
     * (stale/unregistered) does not prevent delivery to the user's other tokens, and never
     * throws — callers invoke this alongside a state transition that must succeed regardless
     * of push delivery outcome.
     */
    public void sendToUser(UUID userId, String title, String body, Map<String, String> data) {
        if (!available) {
            log.debug("Push not configured — skipping notification '{}' for user {}.", title, userId);
            return;
        }
        List<DeviceToken> tokens = deviceTokenService.findByUserId(userId);
        for (DeviceToken deviceToken : tokens) {
            try {
                Message message = Message.builder()
                        .setToken(deviceToken.getToken())
                        .setNotification(com.google.firebase.messaging.Notification.builder()
                                .setTitle(title)
                                .setBody(body)
                                .build())
                        .putAllData(data)
                        .build();
                FirebaseMessaging.getInstance().send(message);
            } catch (FirebaseMessagingException e) {
                log.warn("Failed to deliver push notification to a device token for user {}: {}", userId, e.getMessage());
            }
        }
    }
}
