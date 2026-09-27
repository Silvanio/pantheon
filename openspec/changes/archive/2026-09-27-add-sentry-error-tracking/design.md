## Context

Pantheon has three independently deployable services (`pantheon-service`, `pantheon-message`,
`pantheon-web`) plus a Flutter app (`pantheon-mobile`), none of which have any centralized error
visibility today — see `AGENTS.md`'s repository layout. This change adds Sentry, constrained to
its free "Developer" plan: 5,000 error events/month, a separate and smaller performance-unit
quota, 1 team member, 30-day retention, no paid features (no SSO, no advanced alerting rules
beyond basics). The user explicitly wants to stay on this tier, so quota discipline is a design
constraint, not an afterthought.

The repo's established convention (`AGENTS.md`, reinforced by this session's Firebase and
dart-define work) is: all config via environment variables, sensible disabled-by-default dev
defaults, never hardcoded, documented per deployment target (local IDE, Docker, Swarm, K8s).

## Goals / Non-Goals

**Goals:**
- Unhandled exceptions/crashes in all three services (`pantheon-service`, `pantheon-web`,
  `pantheon-mobile`) reach Sentry with enough context (environment, release, stack trace) to
  debug them.
- Mobile screen navigation is visible as breadcrumbs leading up to an error/crash.
- Zero data sent, zero quota consumed, when no DSN is configured (local dev default).
- Stay within the free plan's limits by design (sampling, log-level filtering), not by luck.
- No sensitive data (JWTs, passwords, document contents, full request/response bodies) ever
  reaches Sentry.

**Non-Goals:**
- Performance/APM tracing as a first-class feature — `tracesSampleRate` is set near zero
  everywhere; this change is about errors and navigation, not latency profiling (the free plan's
  performance-unit quota is tighter than the error quota and easy to exhaust by accident).
- Session Replay — not requested, and it consumes yet another separate free-plan quota
  (500 replays/month) better reserved in case it's wanted later.
- `pantheon-message` — see Decisions below; excluded from this change's scope.
- Alerting/notification rules inside Sentry — out of scope; the free plan's basic email alerts
  are Sentry's own default and need no work here.

## Decisions

**1. One Sentry organization, one project per service (3 projects: `pantheon-service`,
`pantheon-web`, `pantheon-mobile`), not a single shared project.**
Alternative considered: one project for everything, tagged by `service`. Rejected — Sentry's
free plan allows multiple projects under one org at no extra cost, and separate projects give
separate issue streams, separate DSNs (so a leaked frontend DSN can't be used to distinguish
backend behavior), and cleaner default alerting per project. The per-service event streams also
make it much easier to notice which service is consuming the shared 5,000/month quota.

**2. `pantheon-message` is excluded from this change.**
The user said "backend" (singular) and only `pantheon-service` was discussed throughout this
session's other work (Firebase, k8s manifests, etc.); `pantheon-message` is a background worker
with a much lower expected error surface (RabbitMQ consumer + email sending). Rather than guess,
this is called out explicitly: `pantheon-message` gets no Sentry integration in this change's
tasks. Adding it later is a small, mechanical follow-up (identical pattern to `pantheon-service`)
if the user wants it — not worth blocking or bloating this change over.

**3. Log-level floor: only `ERROR` (Java: `ERROR`; JS: `window.onerror`/unhandled
rejections; Flutter: `FlutterError.onError`/`PlatformDispatcher.instance.onError`), never
`WARN`/`INFO` forwarded to Sentry as events.**
The existing codebases log fairly liberally at `WARN`/`INFO` (e.g. `PushNotificationService`'s
per-token delivery failures, logged as `warn`, from this session's earlier work). Forwarding
those to Sentry as events would burn the 5,000/month budget on routine, already-handled
conditions. `WARN`-level logs remain in the existing local logging (SLF4J/console) as today — this
change does not touch existing logging statements or levels, it only adds a new, separate,
narrower drain (unhandled exceptions only) into Sentry.

**4. Disabled-by-default, enforced by environment/profile — not merely by env var presence.**
The user explicitly wants Local to *never* run Sentry, not just "off by default until someone
sets a var." So Local is a hard, structural exclusion, not a soft default:

- `pantheon-service` gains real Spring profiles: `application-local.yml`, `application-dev.yml`,
  `application-prd.yml`, alongside the existing `application.yml` (kept as shared base config).
  `application.yml` adds `spring.profiles.active: ${SPRING_PROFILES_ACTIVE:local}` — so a bare
  `mvn spring-boot:run` with zero env vars (today's whole local workflow) activates `local`
  automatically. `application-local.yml` hardcodes `sentry.dsn: ""` (empty, not
  `${SENTRY_DSN:}`) — this **ignores** `SENTRY_DSN` entirely while the local profile is active,
  so even a stray exported `SENTRY_DSN` in a developer's shell can't leak local events into the
  shared quota. `application-dev.yml`/`application-prd.yml` set `sentry.dsn: ${SENTRY_DSN:}` and
  a hardcoded `sentry.environment: dev`/`prd` tag (not secret, fine to commit).
- `pantheon-web` gains Vite mode files mapped onto Vite's own conventions rather than fighting
  them: **Local → `.env.development`** (Vite's own default mode for `npm run dev` with zero
  flags — no `.env.local` file is created for this, see the naming gotcha below), **Dev →
  `.env.dev`** (a genuinely custom mode, run via `--mode dev`), **PRD → `.env.production`**
  (Vite's own default mode for `npm run build`). `.env.development` omits `VITE_SENTRY_DSN`
  (blank → disabled); `.env.dev` and `.env.production` set it to the real DSN.
  **Naming gotcha, deliberately avoided**: Vite treats a file literally named `.env.local` as an
  always-loaded, gitignored personal-override file layered on top of *every* mode — it is not a
  mode-specific file despite the name. Naming our "Local" tier's file `.env.local` would silently
  break this (it would apply in Dev/PRD builds too). Using `.env.development` for the Local tier
  sidesteps this entirely.
- `pantheon-mobile` already has the equivalent mechanism (`env/local-simulator.json`,
  `env/local-device.json`, `env/prod.json`, added earlier this session) — no new mechanism
  needed, just add `SENTRY_DSN` to each (blank in the two local files, real value in `prod.json`).
  A `env/dev.json` mirroring the backend/frontend's "Dev" tier is not added in this change since
  the user's request named only Spring Boot and the frontend — see Open Questions.

**5. Mobile navigation tracking via `SentryNavigatorObserver` in `go_router`'s `observers:`
list**, not a custom breadcrumb-per-route call.
`go_router`'s `GoRouter` constructor accepts a `NavigatorObserver` list, and `sentry_flutter`
ships `SentryNavigatorObserver` specifically for this (standard integration path, documented by
Sentry for `go_router`). This is one line at router construction, versus hand-adding a
`Sentry.addBreadcrumb(...)` call at every route — far less error-prone and keeps route names
consistent with whatever go_router already calls them.

**6. PII/data scrubbing**: enable Sentry's default PII scrubbing (`sendDefaultPii: false` — the
conservative default, not the opt-in "send full PII" mode) on all three SDKs, and audit that no
call site attaches raw request/response bodies, auth headers, or document content as `extra`/
`context` data. Breadcrumbs added for navigation only carry route names (e.g.
`/purchase-requests/:id`), never entity payloads. This is a review checklist item in tasks.md,
not new code by itself.

**7. DSN storage**: a Sentry DSN is a semi-public identifier (like a Firebase Web API key — it
identifies where to send events, but sending fake events with a known DSN is a minor abuse
vector at worst, not a credential that grants read access to existing data). It follows the
existing env-var convention, but — unlike the Firebase Admin SDK service-account JSON — it does
**not** need file-based Secret+volume treatment in Kubernetes; a plain `ConfigMap` entry
(`SENTRY_DSN`) alongside the other non-secret config in `infra/k8s/pantheon-service.yaml` is
enough, documented the same way as the other per-environment entries in the root `README.md`.

## Risks / Trade-offs

- **[Risk] A bug causing a tight retry/exception loop could burn the whole month's 5,000-event
  quota in minutes.** → Mitigation: Sentry's own per-project rate limiting / spike protection
  (built into the free plan) caps ingestion during a sudden spike; additionally, keeping the
  event floor at `ERROR`-only (Decision 3) means routine retries logged at `WARN` never reach
  Sentry in the first place.
- **[Risk] Forgetting to set a real DSN in a real deployment silently means "no error
  visibility," not a loud failure.** → Mitigation: document a one-line startup log
  ("Sentry disabled: SENTRY_DSN not set" / "Sentry enabled") for `pantheon-service`, matching the
  `PushNotificationService` pattern already established this session, so it's visible in
  deployment logs whether it's active — same idea applied for web/mobile console logs.
- **[Trade-off] Excluding `pantheon-message` means backend errors there stay invisible.**
  Accepted for now (Decision 2) — small, mechanical to add later.
- **[Trade-off] No performance tracing means Sentry can't help diagnose slow requests.** Accepted
  — out of scope non-goal; the free plan's performance quota is too tight to spend here.

## Migration Plan

1. User creates a free Sentry account + organization (manual, cannot be automated/guessed).
2. User creates 3 projects (`pantheon-service`, `pantheon-web`, `pantheon-mobile`) under that org
   and retrieves each project's DSN.
3. Implement the three SDK integrations (this change's tasks), all defaulting to disabled.
4. User provides the 3 DSNs; wire them into local run configs the same way
   `PANTHEON_FIREBASE_CREDENTIALS_PATH` was wired (IntelliJ run config for the backend,
   `.env`/`VITE_SENTRY_DSN` for web, `env/prod.json` — or a new `env/*.json` — for mobile).
5. Trigger one deliberate test error per service, confirm it appears in the corresponding Sentry
   project, confirm no PII is visible in the captured event.
6. Rollback is trivial and requires no code change: unset the DSN env var in any environment to
   fully disable Sentry there again.

## Open Questions

- Should `pantheon-message` get the same treatment in a follow-up change? (Leaning: yes, later,
  same pattern — not blocking this change.)
- Does the user want Sentry release tracking tied to git commit SHA or to each `pubspec.yaml`/
  Maven/`package.json` version string? Defaulted to version string for now (simpler, no CI
  changes needed); revisit if commit-level correlation is wanted later.
- Should `pantheon-mobile` also get a `env/dev.json` (mirroring backend/frontend's 3-tier
  Local/Dev/PRD split), or does its existing 2-local-files-plus-prod split already cover what's
  needed? Not added in this change since it wasn't explicitly requested for mobile — flagged
  here so it's not forgotten if a mobile "Dev" build target is wanted later.
- Beyond Sentry, should other existing config (DB URL, CORS origin, JWT secret, etc.) eventually
  migrate into the new `application-{profile}.yml` / `.env.{mode}` files instead of living purely
  in env vars with inline defaults? Out of scope for this change — the new profile/mode
  mechanism is introduced generally, but this change only uses it to gate Sentry.
