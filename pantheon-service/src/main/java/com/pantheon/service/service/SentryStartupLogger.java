package com.pantheon.service.service;

import io.sentry.Sentry;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Logs whether Sentry error tracking is active at startup, mirroring
 * {@link PushNotificationService}'s enabled/disabled startup log pattern for the
 * error-tracking capability. The Sentry Spring Boot starter auto-configures and initializes the
 * SDK (or leaves it disabled, when {@code sentry.dsn} resolves to blank for the active profile —
 * see {@code application-local/dev/prd.yml}) before application beans are constructed, so
 * {@link Sentry#isEnabled()} already reflects the final state by the time this runs.
 */
@Component
public class SentryStartupLogger {

    private static final Logger log = LoggerFactory.getLogger(SentryStartupLogger.class);

    @PostConstruct
    void logStatus() {
        if (Sentry.isEnabled()) {
            log.info("Sentry error tracking enabled.");
        } else {
            log.info("Sentry error tracking disabled: sentry.dsn is not set for the active profile.");
        }
    }
}
