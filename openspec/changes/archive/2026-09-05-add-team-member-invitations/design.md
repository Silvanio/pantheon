## Context

`ProjectService.addMember` (in `pantheon-service`) currently resolves the target email against `app_user` and throws `MemberNotFoundException` (HTTP 404) if there is no match; otherwise it immediately persists an active `ProjectMembership` with role `MEMBER`. There is no notion of an invitation, a pending account, or member consent. `AppUser` has `passwordHash` and `googleSubject` (either may be null) and no lifecycle status. `pantheon-message` consumes RabbitMQ events (`MessageEnvelope`) and only persists a `ProcessedMessage` row — it has no email capability. Local orchestration (`infra/docker-compose.yml`) runs RabbitMQ, two Postgres DBs, and MinIO, but no SMTP.

`pantheon-web`'s `TeamPanel.vue` lists members and has an add-member form (email + construction function + specialty). `useAuth` stores a JWT issued by `pantheon-service` (`/api/auth/register`, `/api/auth/login`, OAuth2 success handler). Onboarding gating (`useProjectOnboarding`, router guard) keys off `GET /api/onboarding/status`, which is derived from the user's `ProjectMembership` rows.

This change introduces an invitation lifecycle for project membership. The controlling requirement from the request: **every prospective member must explicitly accept before joining — even one who already has an account.**

## Goals / Non-Goals

**Goals:**
- Adding a member by email always yields an `INVITED` membership that confers no project access until accepted.
- Unknown emails get a pre-registration account and an email to finish signing up; known emails get a notification email to review and accept.
- The member list distinguishes invited-but-not-accepted people (a "Convite" badge) from full members.
- An invited-only user is treated as having no active project by onboarding/access checks.
- `pantheon-message` actually sends the invitation email over SMTP; development has a visible SMTP sink.

**Non-Goals:**
- Decline/revoke UX beyond a minimal endpoint; invitation reminders, resend scheduling, or auto-expiry cleanup jobs.
- Invite-by-link without a specific target email; bulk invite.
- Changing Google/OAuth provisioning.
- Production email deliverability (SPF/DKIM/bounce handling).

## Decisions

### 1. Membership state via a `status` column, invitation details in a separate table

`ProjectMembership` gains `status VARCHAR(20)` with values `INVITED` and `ACTIVE` (nullable in the DB; a null legacy row is read as `ACTIVE`). Invitation-specific data lives in a new `membership_invitation` table:

```
membership_invitation(
  id UUID PK,
  membership_id UUID NOT NULL UNIQUE REFERENCES project_membership(id),
  email VARCHAR(255) NOT NULL,
  token_hash VARCHAR(255) NOT NULL UNIQUE,
  invited_by UUID NOT NULL REFERENCES app_user(id),
  requires_registration BOOLEAN NOT NULL,
  created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
  expires_at TIMESTAMPTZ NOT NULL,
  accepted_at TIMESTAMPTZ
)
```

- **Why a separate table:** the membership row is the long-lived record and is referenced by daily-report signatures/workforce entries; invitation metadata (token, inviter, expiry) is transient and one-to-one. Keeping it separate avoids widening `project_membership` with columns that are meaningless after acceptance.
- **Alternative considered:** put `invitation_token`, `invited_by`, `invited_at` directly on `project_membership`. Rejected — clutters the core entity and complicates the "list members" query with columns that are null for the common case.

### 2. Pre-registration reuses `AppUser` with a `registration_status`

`AppUser` gains `registration_status VARCHAR(20)` — `PENDING_REGISTRATION` or `ACTIVE` (null legacy = `ACTIVE`). A pre-registration user is created with the invited email, a placeholder `display_name` (derived from the email local-part), `password_hash = null`, `google_subject = null`, `registration_status = PENDING_REGISTRATION`.

- **Why reuse `AppUser`:** memberships, `ProjectMemberResponse`, and daily-report references all point at `app_user.id`. A parallel "pending user" table would force every consumer to union two sources. The unique index on `project_membership(project_id, user_id)` and existing `findByEmail` keep working unchanged.
- Completing registration sets `password_hash`, a real `display_name`, and flips `registration_status = ACTIVE`, then issues a JWT (same shape as `/api/auth/register`).
- A `PENDING_REGISTRATION` user cannot log in via `/api/auth/login` (no password hash — `DaoAuthenticationProvider` already fails) and is excluded from being offered as an existing account. If such an email later goes through normal `/api/auth/register`, that path completes the same record instead of throwing `EmailAlreadyRegisteredException`.
- **Alternative considered:** separate `pending_invitation_email` table with no user row until acceptance. Rejected for the fan-out reason above and because the member list must show the invited person immediately.

### 3. `addMember` always creates `INVITED`; three token endpoints drive the rest

`POST /api/projects/{id}/members` (admin-only, unchanged auth):
1. Resolve email. If no `AppUser`, create a `PENDING_REGISTRATION` user. If an `ACTIVE` user exists and already has an `ACTIVE` membership on the project → 409. If they have an existing `INVITED` membership → return that invitation (idempotent-ish resend).
2. Create `ProjectMembership(status=INVITED, role=MEMBER, function, specialty)`.
3. Create `MembershipInvitation` with a random 256-bit token (store only a SHA-256 hash; the raw token goes into the email link only), `requires_registration = (user was just pre-registered)`, `expires_at = now + 14 days`.
4. Publish `team-invitation-created` event; return `201` with the invitation summary (no raw token in the body).

New endpoints (public — no JWT; the token is the credential), all under `/api/invitations`:
- `GET /{token}` → `{ projectName, inviterName, email, requiresRegistration, status }`. 404 for unknown/expired token.
- `POST /{token}/complete-registration` `{ password, displayName }` → only valid when `requiresRegistration` and the user is `PENDING_REGISTRATION`; sets credentials, flips user to `ACTIVE`, returns `{ token: <JWT> }`. Does **not** accept the invitation (see decision 4).
- `POST /{token}/accept` → requires the caller to be authenticated **as the invited user** (JWT whose subject matches the invitation's user). Flips membership `INVITED → ACTIVE`, sets `accepted_at`. Idempotent if already `ACTIVE`. Returns the project summary so the SPA can route into it.

### 4. Acceptance is always a distinct, authenticated step

Per the request, completing registration does not by itself add the person to the team. The flow for a new user is: open link → `complete-registration` (now logged in) → `accept`. For an existing user: open link → log in if needed → `accept`. Requiring the JWT-as-invited-user on `accept` means the raw token alone cannot add someone without them authenticating, and a forwarded link can't be accepted by the wrong person.

- **Alternative considered:** accept purely by token possession. Rejected — a leaked/forwarded link would let anyone join, and "the client must accept" implies an authenticated act by that client.

### 5. `INVITED` memberships are invisible to access & onboarding

- `requireMembership` / `requireAdmin` and `listMembers`' membership check only accept `ACTIVE` memberships (admins are always `ACTIVE` — created directly).
- `getOnboardingStatus` and `listMyProjects` filter to `ACTIVE` memberships when deciding "has an active project"; an invited-only user gets the "no project" onboarding path. The invitation itself is surfaced to the SPA via the email link, not the dashboard, in this change.
- `listMembers` returns **all** memberships (including `INVITED`) with a `status` / `invited` field so the admin sees who is still pending. Plan/project-count limits count only `ACTIVE`.

### 6. Email via `pantheon-message` + Spring Mail + Mailpit locally

`pantheon-message` adds `spring-boot-starter-mail` and an `EmailSender` wrapper over `JavaMailSender`. `EventListener` gains a branch on `envelope.type() == "team-invitation-created"` that renders a plain-text (+ simple HTML) email and sends it. Config: `SPRING_MAIL_HOST/PORT/USERNAME/PASSWORD`, `PANTHEON_MAIL_FROM`, and `PANTHEON_WEB_BASE_URL` (to build `${base}/invitations/${token}`). It still persists a `ProcessedMessage` for the event.

`infra/docker-compose.yml` adds `mailpit` (`axllent/mailpit`, SMTP `1025`, UI `8025`) and points `pantheon-message`'s `SPRING_MAIL_*` at it plus `PANTHEON_WEB_BASE_URL: http://localhost:8080`. A k8s ConfigMap note documents the same env for `pantheon-message`.

- **Why send from `pantheon-message`:** it already owns async event consumption and DLQ semantics; a transient SMTP failure should retry via RabbitMQ, not fail the admin's HTTP request.
- **Why the raw token only travels in the event/email:** `pantheon-service` stores just the hash, so a DB read never exposes a usable link.

### 7. RabbitMQ contract addition

New event type `team-invitation-created`, routing key `message.team-invitation-created` (matches existing `message.*` binding). Payload:

```json
{
  "invitationId": "uuid",
  "email": "person@example.com",
  "token": "<raw-token>",
  "projectId": "uuid",
  "projectName": "Obra X",
  "inviterName": "Maria",
  "requiresRegistration": true
}
```

## Risks / Trade-offs

- **[Risk] Pre-registration users pollute `app_user` if invitations are never accepted.** → Rows are inert (can't log in, excluded from onboarding). Cleanup of expired unaccepted invitations + their orphan pending users is a follow-up job, noted as out of scope.
- **[Risk] Email enumeration — `GET /api/invitations/{token}` and the add-member 409 could leak whether an email is registered.** → The token endpoint reveals nothing without a valid token; `addMember` is admin-only so the admin already knows the team. Acceptable.
- **[Risk] Changing `POST /api/projects/{id}/members` response/behavior breaks existing `pantheon-web` expectations.** → `pantheon-web` is updated in the same change; the endpoint is internal to this product. Documented as BREAKING in the proposal.
- **[Risk] `registration_status` / `status` nullable columns mean code must treat null as `ACTIVE` everywhere.** → Centralize in entity getters (`getStatus()` returns `ACTIVE` when null) and a single Flyway backfill (`UPDATE ... SET status='ACTIVE' WHERE status IS NULL`) so only genuinely new rows use the new states.
- **[Risk] SMTP down in dev makes invitations look broken.** → Mailpit is part of the compose stack with a healthcheck; `pantheon-message` retries via the queue and DLQ.
- **[Trade-off] Requiring JWT-as-invited-user on `accept` adds a login step for existing users.** → This is the explicit product requirement (consent), not incidental friction.

## Migration Plan

1. Flyway `V21__add_status_to_project_membership.sql` (add `status`, backfill `ACTIVE`), `V22__add_registration_status_to_app_user.sql` (add `registration_status`, backfill `ACTIVE`), `V23__create_membership_invitation_table.sql`.
2. Deploy `pantheon-service` (new endpoints; `addMember` behavior change) and `pantheon-message` (mail) together.
3. Deploy `infra` compose/k8s changes (Mailpit + mail env) before/with `pantheon-message`.
4. Deploy `pantheon-web` with the `/invitations/:token` route and badge.
5. **Rollback:** revert services; the added columns/table are additive and harmless if unused (legacy `addMember` reading them as `ACTIVE` still works). Pending users created in the window remain inert.

## Open Questions

- Invitation expiry window — 14 days assumed; confirm with product.
- Should an admin be able to see/resend/cancel invitations from `TeamPanel` in this change, or is the badge-only view enough for v1? (Currently scoped as badge-only + a minimal decline endpoint.)
- Placeholder `display_name` for pre-registration users in the member list — show the email instead until registration completes?
