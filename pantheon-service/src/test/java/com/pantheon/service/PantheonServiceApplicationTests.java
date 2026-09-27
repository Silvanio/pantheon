package com.pantheon.service;

import static org.assertj.core.api.Assertions.assertThat;

import io.sentry.Sentry;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

/**
 * Explicitly pins the {@code local} profile so this suite verifies the error-tracking
 * capability's Local/Dev/PRD gate: the app must start cleanly and Sentry must stay disabled
 * here even if a developer's shell happens to export SENTRY_DSN, since application-local.yml
 * hardcodes {@code sentry.dsn: ""} rather than reading the env var.
 */
@SpringBootTest
@ActiveProfiles("local")
class PantheonServiceApplicationTests {

	@Test
	void contextLoads() {
	}

	@Test
	void sentryStaysDisabledOnLocalProfile() {
		assertThat(Sentry.isEnabled()).isFalse();
	}

}
