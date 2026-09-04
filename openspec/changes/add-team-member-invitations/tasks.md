## 1. Data model & migrations (pantheon-service)

- [x] 1.1 Add `V21__add_status_to_project_membership.sql`: `ALTER TABLE project_membership ADD COLUMN status VARCHAR(20)`, then `UPDATE project_membership SET status = 'ACTIVE' WHERE status IS NULL`
- [x] 1.2 Add `V22__add_registration_status_to_app_user.sql`: `ALTER TABLE app_user ADD COLUMN registration_status VARCHAR(20)`, then `UPDATE app_user SET registration_status = 'ACTIVE' WHERE registration_status IS NULL`
- [x] 1.3 Add `V23__create_membership_invitation_table.sql` per design (columns, `membership_id` unique FK, `token_hash` unique, `expires_at`, `accepted_at` nullable)
- [x] 1.4 Add `MembershipStatus` enum (`INVITED`, `ACTIVE`) and `RegistrationStatus` enum (`ACTIVE`, `PENDING_REGISTRATION`)
- [x] 1.5 Add `status` to `ProjectMembership` entity with `getStatus()` returning `ACTIVE` when the column is null; add constructor/factory paths for `INVITED` and an `accept()` mutator
- [x] 1.6 Add `registrationStatus` to `AppUser` with null-safe getter, a `completeRegistration(passwordHash, displayName)` mutator, and a static factory for a pre-registration user (email + placeholder display name, no credentials)
- [x] 1.7 Create `MembershipInvitation` entity + `MembershipInvitationRepository` (`findByTokenHash`, `findByMembershipId`)

## 2. Invitation domain logic (pantheon-service)

- [x] 2.1 Add a token utility: generate a 256-bit URL-safe random token, hash with SHA-256 for storage/lookup
- [x] 2.2 Rewrite `ProjectService.addMember`: resolve email → reuse existing active account / reuse existing pending account / create pending account; reject with 409 if an `ACTIVE` membership already exists; return existing invitation if an `INVITED` membership already exists; otherwise create `INVITED` membership + `MembershipInvitation` (14-day expiry) and publish `team-invitation-created`
- [x] 2.3 Extend `EventPublisher` usage: publish `team-invitation-created` with `{ invitationId, email, token (raw), projectId, projectName, inviterName, requiresRegistration }`
- [x] 2.4 Create `InvitationService`: `getByToken` (404 on unknown/expired), `completeRegistration` (only when `requiresRegistration` + user `PENDING_REGISTRATION`, else 409; sets credentials, flips user `ACTIVE`), `accept` (requires caller == invited user; flips membership `INVITED`→`ACTIVE`, sets `accepted_at`; idempotent when already `ACTIVE`)
- [x] 2.5 Update `getOnboardingStatus` and `listMyProjects` to consider only `ACTIVE` memberships
- [x] 2.6 Update `requireMembership` / `requireAdmin` / `listMembers` access check to accept only `ACTIVE` memberships
- [x] 2.7 Update `listMembers` to return all memberships including `INVITED`, adding an `invited`/`status` field to `ProjectMemberResponse`
- [x] 2.8 Update plan/project-count limit logic to count only `ACTIVE` memberships/projects where relevant
- [x] 2.9 Update `AppUserService.registerWithPassword` to complete an existing `PENDING_REGISTRATION` account instead of throwing `EmailAlreadyRegisteredException`; ensure login path already rejects credential-less accounts

## 3. REST surface (pantheon-service)

- [x] 3.1 Change `POST /api/projects/{id}/members` to return `201` with an invitation summary and the new behavior; drop the 404-on-unknown-email path
- [x] 3.2 Add `InvitationController`: `GET /api/invitations/{token}` (public), `POST /api/invitations/{token}/complete-registration` (public, returns `{ token: JWT }`), `POST /api/invitations/{token}/accept` (authenticated as invited user)
- [x] 3.3 Permit `/api/invitations/**` (except `accept`) in the security config as unauthenticated; keep `accept` authenticated
- [x] 3.4 Add/adjust exception handling: 409 for conflicting membership/registration, 404 for bad token, 403 for wrong-account accept
- [x] 3.5 Add DTOs: `InvitationResponse`, `CompleteRegistrationRequest`, `AcceptInvitationResponse`

## 4. Email delivery (pantheon-message)

- [x] 4.1 Add `spring-boot-starter-mail` dependency
- [x] 4.2 Add `EmailSender` component wrapping `JavaMailSender`; config keys `spring.mail.*`, `pantheon.mail.from`, `pantheon.web.base-url` with env overrides
- [x] 4.3 In `EventListener`, branch on `type == "team-invitation-created"`: render invitation email (plain text + minimal HTML) with link `${web.base-url}/invitations/${token}`, send it, then persist `ProcessedMessage`; on send failure throw so the message is not acked
- [x] 4.4 Update `pantheon-message` `application.yml` with the new config blocks and defaults for local dev
- [x] 4.5 Add a unit/slice test for the invitation-email branch (e.g., GreenMail or a mocked `JavaMailSender`)

## 5. Local orchestration & infra

- [x] 5.1 Add `mailpit` service to `infra/docker-compose.yml` (SMTP `1025`, UI `8025`, healthcheck)
- [x] 5.2 Wire `pantheon-message` env in compose: `SPRING_MAIL_HOST=mailpit`, `SPRING_MAIL_PORT=1025`, `PANTHEON_MAIL_FROM`, `PANTHEON_WEB_BASE_URL=http://localhost:8080`; add `depends_on` mailpit
- [x] 5.3 Update `infra/k8s/pantheon-message.yaml` ConfigMap with the SMTP / web-base-url env keys (documented placeholders)

## 6. Web (pantheon-web)

- [x] 6.1 Add `invited` / `status` to the `ProjectMember` type and `listMembers` mapping in `useProjects`
- [x] 6.2 Update `TeamPanel.vue`: render a "Convite" badge for invited members; keep the add-member form working for unregistered emails; adjust success copy
- [x] 6.3 Add `useInvitations` composable: `getInvitation(token)`, `completeRegistration(token, payload)`, `acceptInvitation(token)`
- [x] 6.4 Add `InvitationView.vue` + public route `/invitations/:token`: fetch invitation; if `requiresRegistration` and not logged in → registration form → on success store JWT; if existing account → prompt login; then show "Aceitar convite" → on accept route into the project
- [x] 6.5 Ensure the router guard allows `/invitations/:token` without an active project and without forcing onboarding
- [x] 6.6 Add i18n strings (`members.badge.invited`, invitation view labels, errors) for existing locales

## 7. Tests & verification

- [x] 7.1 `pantheon-service` tests: `addMember` creates `INVITED` for known + unknown email; 409 on existing active member; onboarding ignores `INVITED`; `listMembers` includes invitations and blocks invited-only users
- [x] 7.2 `pantheon-service` tests: `GET /api/invitations/{token}` (valid/expired), `complete-registration` (happy + 409), `accept` (happy, wrong account 403, idempotent), normal register completing a pending account
- [x] 7.3 `pantheon-web` tests/component check: `vue-tsc` typecheck + `vite build` pass with the new `InvitationView`, route, composable, and `TeamPanel` badge (no unit-test harness exists in `pantheon-web`)
- [ ] 7.4 Run full stack via `infra/docker-compose.yml`, invite an unregistered email, confirm the email appears in Mailpit, complete registration + accept, confirm the badge disappears and the member gains access — MANUAL QA, not run in this session
- [ ] 7.5 Update `openspec` specs via `/opsx:sync` (or archive) once implemented and verified
