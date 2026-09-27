## ADDED Requirements

### Requirement: Disabled by default without a configured DSN
Each service (`pantheon-service`, `pantheon-web`, `pantheon-mobile`) SHALL initialize its Sentry
SDK only when a DSN is configured via that service's environment-variable/dart-define convention
(`SENTRY_DSN` for `pantheon-service` and `pantheon-mobile`, `VITE_SENTRY_DSN` for
`pantheon-web`). When the DSN is unset or blank, the service SHALL start and run normally with no
Sentry initialization and no network calls to Sentry.

#### Scenario: Local development with no DSN configured
- **WHEN** `pantheon-service` starts with `SENTRY_DSN` unset (the local dev default)
- **THEN** the Sentry SDK is not initialized and no events are sent, and a startup log line
  states Sentry is disabled

#### Scenario: Mobile app built with no DSN configured
- **WHEN** `pantheon-mobile` is run via `env/local-simulator.json` or `env/local-device.json`
  (neither sets `SENTRY_DSN`)
- **THEN** `SentryFlutter.init` is skipped and the app's `main()` runs directly with no Sentry
  activity

### Requirement: Backend Sentry activation is gated by Spring profile, not env var alone
`pantheon-service` SHALL support `local`, `dev`, and `prd` Spring profiles. The `local` profile
SHALL force Sentry off unconditionally, ignoring any `SENTRY_DSN` value. The `dev` and `prd`
profiles SHALL read `SENTRY_DSN` from the environment and enable Sentry when it is set, tagging
events with `sentry.environment` set to `dev`/`prd` respectively. When `SPRING_PROFILES_ACTIVE`
is not set, the `local` profile SHALL be active by default.

#### Scenario: Local profile ignores a stray SENTRY_DSN
- **WHEN** the `local` profile is active and `SENTRY_DSN` happens to be set in the environment
- **THEN** Sentry remains disabled — the `local` profile's own DSN setting overrides it

#### Scenario: No profile specified defaults to local
- **WHEN** `pantheon-service` starts with `SPRING_PROFILES_ACTIVE` unset
- **THEN** the `local` profile is active and Sentry is disabled

#### Scenario: Dev profile with a DSN enables Sentry
- **WHEN** the `dev` profile is active and `SENTRY_DSN` is set
- **THEN** Sentry is enabled and events are tagged `environment: dev`

### Requirement: Frontend Sentry activation is gated by Vite mode, not env var alone
`pantheon-web` SHALL support three Vite modes mapped to Local/Dev/PRD: the `development` mode
(Vite's default `npm run dev` mode) SHALL never enable Sentry regardless of `VITE_SENTRY_DSN`,
while a `dev` mode and the `production` mode (Vite's default `npm run build` mode) SHALL enable
Sentry when `VITE_SENTRY_DSN` is set in that mode's env file.

#### Scenario: Local (development mode) never sends to Sentry
- **WHEN** the app runs in Vite's `development` mode (`npm run dev` with no `--mode` flag)
- **THEN** Sentry is not initialized, regardless of any `VITE_SENTRY_DSN` value present in the
  environment

#### Scenario: Production build with a DSN enables Sentry
- **WHEN** the app is built in the `production` mode and `.env.production` sets
  `VITE_SENTRY_DSN`
- **THEN** Sentry is initialized and events are tagged `environment: production`

### Requirement: Backend captures unhandled exceptions
`pantheon-service` SHALL report unhandled exceptions to Sentry when `SENTRY_DSN` is configured,
tagged with the current environment and release version, without forwarding routine `WARN`/
`INFO`-level log statements as Sentry events.

#### Scenario: Unhandled exception with Sentry enabled
- **WHEN** `SENTRY_DSN` is configured and an unhandled exception occurs while processing a
  request
- **THEN** the exception is sent to Sentry as an event tagged with `environment` and `release`

#### Scenario: Routine warning is not forwarded
- **WHEN** `SENTRY_DSN` is configured and code logs at `WARN` or `INFO` level (e.g. a per-token
  push delivery failure)
- **THEN** no Sentry event is created for that log statement

### Requirement: Frontend captures unhandled errors
`pantheon-web` SHALL report unhandled JavaScript errors and unhandled promise rejections to
Sentry when `VITE_SENTRY_DSN` is configured, with performance tracing disabled or sampled near
zero.

#### Scenario: Unhandled error with Sentry enabled
- **WHEN** `VITE_SENTRY_DSN` is configured and an unhandled JS error occurs in the running app
- **THEN** the error is sent to Sentry as an event tagged with `environment` and `release`

### Requirement: Mobile captures crashes and screen navigation
`pantheon-mobile` SHALL report unhandled Dart/Flutter errors to Sentry when `SENTRY_DSN` is
configured, and SHALL record screen navigation events as breadcrumbs leading up to any captured
error or crash.

#### Scenario: Unhandled error with Sentry enabled
- **WHEN** `SENTRY_DSN` is configured and an unhandled error occurs
- **THEN** the error is sent to Sentry as an event tagged with `environment` and `release`,
  including recent navigation breadcrumbs

#### Scenario: Screen navigation is recorded
- **WHEN** `SENTRY_DSN` is configured and the user navigates between screens via the app's router
- **THEN** each navigation is recorded as a breadcrumb (route name only, no entity data)

### Requirement: No sensitive data is sent to Sentry
None of the three integrations SHALL attach authentication tokens, passwords, or uploaded
document/report content to any Sentry event, context, or breadcrumb. Default PII scrubbing SHALL
remain enabled (not opted out) on all three SDKs.

#### Scenario: Navigation breadcrumb contains no entity data
- **WHEN** a mobile screen navigation breadcrumb is recorded for a detail screen (e.g. a Pedido
  de Compra detail)
- **THEN** the breadcrumb contains only the route name/path, not the request body, response
  payload, or any document/report content
