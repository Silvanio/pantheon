## Why

Today, unhandled exceptions and errors in `pantheon-service`, `pantheon-web`, and
`pantheon-mobile` are only visible in local logs (or not at all once deployed) — there is no
centralized place to see what's failing in production, correlate an error across a request, or
see what screen/page a user was on when a mobile crash happened. Sentry gives all three
services a shared, free error-tracking backend (crash reports, stack traces, breadcrumbs,
release/environment tagging) without needing to build or host anything.

## What Changes

- Add Sentry SDKs to all three services, each disabled by default (no-op) unless a DSN is
  configured, so local development never sends data and never touches the free-tier quota:
  - `pantheon-service`: `sentry-spring-boot-starter-jakarta`, capturing unhandled exceptions and
    `ERROR`-level logs only (not `WARN`/`INFO`, to control event volume against the free plan's
    5,000-events/month cap), tagged with `environment` and `release`.
  - `pantheon-web`: `@sentry/vue`, capturing unhandled JS errors, with performance tracing
    (`tracesSampleRate`) set near zero — the free plan's performance-unit quota is separate and
    more restrictive than the error quota, and this feature is about errors, not tracing.
  - `pantheon-mobile`: `sentry_flutter`, capturing crashes/errors **and** screen navigation as
    breadcrumbs via a `SentryNavigatorObserver` wired into the app's existing `go_router` setup —
    this is the "navegação de página" (page navigation) tracking requirement.
- All three read their DSN from this repo's existing env-var-based config pattern (`SENTRY_DSN`
  for the Java service via `application.yml`'s `${VAR:default}`, `VITE_SENTRY_DSN` for the
  frontend via Vite's env handling, a `SENTRY_DSN` `--dart-define` for mobile using the
  `env/*.json` files just established) — never hardcoded, consistent with `AGENTS.md`.
- Introduce a formal **Local / Dev / PRD** environment mechanism in the two services that don't
  have one yet, since Sentry must be unconditionally off in Local regardless of any stray env
  var: `pantheon-service` gets real Spring profiles (`application-local.yml`,
  `application-dev.yml`, `application-prd.yml`, defaulting to `local` when
  `SPRING_PROFILES_ACTIVE` is unset), and `pantheon-web` gets equivalent Vite mode files
  (`.env.development` for Local — Vite's own default dev-server mode — `.env.dev` for Dev, and
  `.env.production` for PRD). `pantheon-mobile` already has this via `env/*.json`
  (`local-simulator`/`local-device`/`prod`) from earlier in this session — no change needed
  there beyond adding `SENTRY_DSN` to the existing files.
- Document the free-tier operating constraints (event volume discipline, PII scrubbing, what NOT
  to send as event context) so the setup doesn't silently exceed the free plan or leak sensitive
  data (JWTs, passwords, uploaded document contents) into Sentry.
- **BREAKING**: none — this is purely additive instrumentation; nothing existing changes
  behavior when `SENTRY_DSN`/`VITE_SENTRY_DSN` is unset.

## Capabilities

### New Capabilities
- `error-tracking`: cross-cutting observability capability covering error/crash reporting and
  page-navigation breadcrumbs across `pantheon-service`, `pantheon-web`, and `pantheon-mobile`,
  all backed by a single free-tier Sentry organization.

### Modified Capabilities
(none — no existing capability's requirements change; this only adds new, independent behavior)

## Impact

- **Affected code**: `pantheon-service` (new dependency + config class/properties +
  `application.yml` entry), `pantheon-web` (new dependency + `main.ts` init + Vite env var),
  `pantheon-mobile` (new dependency + `main.dart` init + router observer wiring + `env/*.json`
  files), root `README.md` / `infra/k8s/*.yaml` (document the new env var per deployment target,
  same pattern as the Firebase credential documentation added earlier).
- **Affected systems**: a new external dependency on Sentry (SaaS, free tier) for all three
  services; no database, API contract, or existing UI behavior changes.
- **Manual step required**: the user must create the Sentry account/organization and
  project(s) and provide the real DSN value(s) — this cannot be fabricated or guessed, the same
  way the Firebase project had to be created manually earlier in this project's setup.
