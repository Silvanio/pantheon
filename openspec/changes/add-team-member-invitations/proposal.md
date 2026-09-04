## Why

Today an administrator can only add someone to a project if that person already has a Pantheon account — `POST /api/projects/{id}/members` rejects unknown emails with 404, and any known user is added silently with no say in the matter. Real construction teams are assembled by inviting people (clients, engineers, service providers) who often have never used the platform. We need an invitation flow: unknown emails get a pre-registration and an email to finish signing up, and *every* prospective member — new or existing — must explicitly accept before they actually join the team.

## What Changes

- Adding a member by email now **always creates an invitation** instead of an immediate active membership. The membership is created right away but in an `INVITED` state and does not grant access to the project until accepted.
- **BREAKING** (API behavior): `POST /api/projects/{id}/members` no longer returns 404 for an unknown email and no longer produces an immediately-active membership. It returns the created invitation.
- If the invited email has **no account**, `pantheon-service` creates a pre-registration user (email only, no credentials, status `PENDING_REGISTRATION`) and sends an email with a tokenized link to complete registration.
- If the invited email **already has an account**, `pantheon-service` still creates an `INVITED` membership and sends a notification email with a tokenized link to review and accept the invitation.
- New endpoints to view an invitation by token, complete pre-registration from an invitation, and accept an invitation. Accepting flips the membership from `INVITED` to `ACTIVE`.
- The project member list returns each membership's invitation state; `pantheon-web` shows an invited person in the list with a "Convite" badge until they accept, after which the badge disappears and they are a normal member.
- Onboarding/status, member counts, and project-access checks treat `INVITED` memberships as not-yet-joined (an invited-only user still has "no active project").
- `pantheon-message` gains the ability to actually send transactional email (SMTP), driven by invitation events published by `pantheon-service`.
- Local orchestration adds an SMTP sink (Mailpit) so invitation emails are visible in development.

## Capabilities

### New Capabilities
- `team-invitations`: The invitation lifecycle for project membership — creating invitations (with pre-registration for unknown emails), the tokenized invitation link, completing pre-registration, and accepting/declining, including how invited memberships are excluded from project access until accepted.

### Modified Capabilities
- `pantheon-service`: "Project membership management" changes so adding a member creates an `INVITED` membership (and, for unknown emails, a `PENDING_REGISTRATION` user) rather than an immediately-active one for a pre-existing user; "Onboarding status" and project-access checks exclude `INVITED` memberships.
- `construction-site-management`: "List project members" and "Construction site management views" gain an invitation-state field / "Convite" badge; the add-member flow description covers inviting a not-yet-registered email.
- `pantheon-message`: Adds a requirement to send transactional email via SMTP for invitation events consumed from RabbitMQ.
- `platform-foundation`: Local orchestration setup adds an SMTP service for outbound email in development.

## Impact

- **Affected code**:
  - `pantheon-service`: `ProjectMembership` (new `status`), `AppUser` (new `registrationStatus`), new `MembershipInvitation` entity/repository, `ProjectService.addMember` rewrite, new `InvitationService` + `InvitationController`, new Flyway migrations, event publishing for `team-invitation-created`, adjustments to `getOnboardingStatus` / `requireMembership`.
  - `pantheon-message`: `spring-boot-starter-mail`, an `EmailSender`, invitation-email template/handler in `EventListener`, new config (`SPRING_MAIL_*`, `PANTHEON_WEB_BASE_URL`).
  - `pantheon-web`: `TeamPanel.vue` (badge + invited handling), new `InvitationView.vue` + route `/invitations/:token` (public), `useProjects` / new `useInvitations` composable, i18n strings.
  - `infra/docker-compose.yml` (+ `infra/k8s` note): Mailpit service, `SPRING_MAIL_*` and `PANTHEON_WEB_BASE_URL` env wiring for `pantheon-message`.
- **New REST surface**: `GET /api/invitations/{token}`, `POST /api/invitations/{token}/complete-registration`, `POST /api/invitations/{token}/accept` (and the changed response of `POST /api/projects/{id}/members`).
- **Non-goals**: declining/revoking invitations UI beyond a basic endpoint, invitation expiry reminders/resend scheduling, inviting by link without a target email, bulk invitations, changing how OAuth (Google) accounts are provisioned, and real email deliverability/DKIM setup for production.
