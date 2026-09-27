## 1. Manual prerequisites (user)

- [x] 1.1 Create a free Sentry account/organization. (org `pantheon-yc`)
- [x] 1.2 Create 3 projects under it: `pantheon-service`, `pantheon-web`, `pantheon-mobile`; note
      each project's DSN.

## 2. pantheon-service: Spring profiles (Local / Dev / PRD)

- [x] 2.1 Add `spring.profiles.active: ${SPRING_PROFILES_ACTIVE:local}` to `application.yml`.
- [x] 2.2 Create `application-local.yml` with `sentry.dsn: ""` (hardcoded empty — ignores
      `SENTRY_DSN`) and `sentry.environment: local`.
- [x] 2.3 Create `application-dev.yml` with `sentry.dsn: ${SENTRY_DSN:}` and
      `sentry.environment: dev`.
- [x] 2.4 Create `application-prd.yml` with `sentry.dsn: ${SENTRY_DSN:}` and
      `sentry.environment: prd`.

## 3. pantheon-service: Sentry SDK

- [x] 3.1 Add the Sentry dependency to `pantheon-service/pom.xml`. Used `io.sentry:
      sentry-spring-boot-4:8.58.0` instead of the `-jakarta` starter named in this task — Spring
      Boot 4 has its own dedicated module per Sentry's own install docs; the `-jakarta` starter
      targets Spring Boot 3. Verified via `docs.sentry.io/platforms/java/guides/spring-boot/`.
- [x] 3.2 `sentry.logging.minimum-event-level: error` set explicitly in `application.yml`.
- [x] 3.3 `sentry.send-default-pii: false` set explicitly.
- [x] 3.4 Added `SentryStartupLogger.java`, mirroring `PushNotificationService`'s pattern.
- [x] 3.5 `sentry.release: '@project.version@'`, resolved via the parent POM's existing `@...@`
      resource filtering (confirmed resolved to `0.0.1-SNAPSHOT` in `target/classes`).
- [x] 3.6 Added `sentryStaysDisabledOnLocalProfile()` to `PantheonServiceApplicationTests`,
      asserting `Sentry.isEnabled()` is false. Full suite independently re-run: 340/340 passing.

## 4. pantheon-web: Vite modes (Local / Dev / PRD)

- [x] 4.1 Created `.env.development` (Local) — no `VITE_SENTRY_DSN` key.
- [x] 4.2 Created `.env.dev` — real DSN (not a placeholder; DSN isn't secret, see design.md
      Decision 7, and Vite embeds `VITE_*` vars into the client bundle at build time regardless).
- [x] 4.3 Created `.env.production` — real DSN, same reasoning.
- [x] 4.4 Added `"build:dev": "vue-tsc -b && vite build --mode dev"` to `package.json`.
- [x] 4.5 Updated `.env.example` documenting all three files and the `.env.local` footgun.

## 5. pantheon-web: Sentry SDK

- [x] 5.1 Added `@sentry/vue@10.75.3` (not v11 — v11 requires Node ≥20.19, this machine has
      20.14).
- [x] 5.2 Initialized in `main.ts`, gated on **two** conditions: `VITE_SENTRY_DSN` non-empty AND
      `import.meta.env.MODE !== 'development'` — the second is a hard structural exclusion added
      after the implementing agent adversarially confirmed a stray `VITE_SENTRY_DSN` in a dev's
      shell could otherwise leak through in Local mode with a DSN-presence-only check.
- [x] 5.3 `tracesSampleRate: 0.01`; `environment: import.meta.env.MODE`; `release` wired via a
      new `__APP_VERSION__` build-time constant (`vite.config.ts` reads `package.json`).
- [x] 5.4 No PII opt-in flags passed; default scrubbing stays on.
- [x] 5.5 Three distinct `console.info` branches added (initialized / disabled-dev-mode /
      disabled-no-DSN).

## 6. pantheon-mobile: Sentry SDK + navigation tracking

- [x] 6.1 Added `sentry_flutter: ^9.30.1`.
- [x] 6.2 `SENTRY_DSN: ""` added to `env/local-simulator.json` and `env/local-device.json`; real
      DSN added to `env/prod.json`.
- [x] 6.3 `main.dart` now branches on `String.fromEnvironment('SENTRY_DSN')`: blank → logs
      disabled + calls the (unchanged, extracted) `_runPantheonApp()` directly; non-blank →
      `SentryFlutter.init(options, appRunner: _runPantheonApp)`.
- [x] 6.4 `tracesSampleRate: 0.01`; `environment` via a new `SENTRY_ENVIRONMENT` dart-define
      (default `local`); `release` left to `sentry_flutter`'s own automatic
      `LoadReleaseIntegration` (reads `pubspec.yaml`'s version via `package_info_plus`,
      already a transitive dependency — no new direct dependency needed).
- [x] 6.5 `SentryNavigatorObserver()` added to `appRouterProvider`'s `GoRouter(...)` in
      `lib/core/router/app_router.dart`.
- [x] 6.6 Confirmed via reading `sentry_flutter`/go_router source: go_router never forwards
      `GoRouterState.extra` into `RouteSettings`, so the default observer can only ever see
      path/query values already visible in the URL — no entity data reaches breadcrumbs.
      `flutter analyze`: clean. `flutter test`: 6/6 passing.

## 7. Documentation

- [x] 7.1 Added a new "Environment profiles (Local / Dev / PRD) and Sentry" section to the root
      `README.md`, covering all three services and the `.env.local` Vite footgun.
- [x] 7.2 Added `SPRING_PROFILES_ACTIVE: "prd"` and `SENTRY_DSN` (real value — non-secret, see
      Decision 7) to `infra/k8s/pantheon-service.yaml`'s `ConfigMap`.
- [x] 7.3 Done by the implementing agent as part of task group 6 —
      `pantheon-mobile/README.md`'s "Running locally" section now documents `SENTRY_DSN`
      alongside `PANTHEON_SERVICE_URL`.

## 8. Verification

- [x] 8.1 Confirmed two ways: the automated `sentryStaysDisabledOnLocalProfile` test asserts
      `Sentry.isEnabled()` is false on the `local` profile, and a live local run's startup log
      showed no "Sentry error tracking enabled" line.
- [ ] 8.2 Live-ran `pantheon-service` with `dev` profile — startup log confirmed
      `Sentry error tracking enabled.`, but no test exception was actually triggered/checked
      against the real Sentry dashboard yet. **Remaining**: trigger a real error and confirm it
      shows up in the `pantheon-service` Sentry project tagged `environment: dev`.
- [x] 8.3 Confirmed by the implementing agent: `vite build --mode development` produces a bundle
      with no `Sentry.init` call and no DSN string present at all.
- [ ] 8.4 Confirmed the DSN is embedded correctly in a `--mode dev` build, but no test JS error
      was actually triggered/checked against the real Sentry dashboard yet. **Remaining**:
      trigger a real error and confirm it shows up in the `pantheon-web` Sentry project.
- [ ] 8.5 Code-reviewed (the disabled-when-blank guard mirrors the already-verified
      backend/frontend pattern) but not run on an actual simulator/device yet. **Remaining**: run
      `pantheon-mobile` via `env/local-simulator.json` and confirm no Sentry network activity.
- [ ] 8.6 **Remaining**: run with a real DSN, navigate a few screens, trigger a test error,
      confirm the Sentry event shows navigation breadcrumbs and no entity data.
- [ ] 8.7 **Remaining**: once 8.2/8.4/8.6 produce real events, spot-check all three for PII
      leakage (tokens, passwords, document content) — none should be present.
