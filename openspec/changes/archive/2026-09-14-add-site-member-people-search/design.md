## Context

Today, `SiteTeamPanel.vue`'s add-member form has two shapes depending on `function`: SERVICE_PROVIDER-without-email creates an accountless `SiteMembership` (`displayName`, `trade`, `contactEmail`); every other case invites by email only (`email`, `cpf` — CPF shown only for CLIENT). `SiteMembershipService.inviteMember` already resolves "does this email belong to an existing `AppUser`" via the shared `MembershipInvitationIssuer.resolveOrCreateUser`, transparently either reusing the account or pre-registering one (`AppUser.preRegistration` already derives a display name from the email's local part when none is given). There is no existing "search people" endpoint anywhere in the codebase, and no CPF checksum validation anywhere (the `Fornecedor` entity's Javadoc explicitly documents CPF/CNPJ as "free text, no checksum validation" as the established convention prior to this change).

## Goals / Non-Goals

**Goals:**
- Let an admin find and reuse a person's already-known data (name, email, CPF, phone) instead of retyping it, searching by CPF or email as they type.
- Capture a name for every new team member, not just accountless service providers.
- Capture an optional phone number for every function.
- Make CPF an optional, cross-function field with real format/checksum validation, instead of a CLIENT-only free-text field.
- Validate email format server-side for every function that requires it.

**Non-Goals:**
- Removing or changing the accountless service-provider flow's defining trait (no email required). It stays the one path where a person can be added without an account.
- A platform-wide or cross-company people directory. Search results never include another company's people.
- Editing an existing member's data (name/CPF/phone) after creation — out of scope; this change only touches the add-member path.
- CNPJ or any non-person validation — CPF only, matching that this is about individual team members.

## Decisions

### 1. People search is scoped to the acting company only, and only to the company's *own known* people
"Known to the company" means: (a) any `AppUser` with a `CompanyMembership` in this company, (b) any `AppUser` with a `SiteMembership` on one of this company's construction sites, and (c) any accountless `SiteMembership` (service provider, no `userId`) on one of this company's sites. The search endpoint takes `siteId` in its path (`GET /api/sites/{siteId}/people/search`) — mirroring `POST /api/sites/{siteId}/members` — and resolves the company from that site server-side, gated by the same `requireManage(..., PermissionCapability.TEAM_MANAGE)` check the add-member endpoint already uses. A minimum query length (3 characters) avoids over-broad scans.

Alternative considered: a platform-wide search by CPF/email (find *any* person in the system, not just this company's). Rejected outright — CPF is sensitive PII; returning another company's team member's CPF to an unrelated company's admin would be a real cross-tenant data leak. Scoping to the company is the only responsible default here, not just a design preference.

### 2. Matching is done in application code over the company's already-small people set, not a bespoke SQL query
`PersonSearchService` gathers the company's `CompanyMembership`s, the `SiteMembership`s across its sites (resolving linked `AppUser`s in bulk via `findAllById`), and filters by whether the query is a prefix of the candidate's CPF, email, or name (case-insensitive). A construction company's team is realistically dozens of people, not thousands, so an in-memory scan is simple and fast enough; existing services in this codebase already favor simple JPA finds + Java-side filtering over hand-written SQL for comparable read paths (e.g. `TaskCardService.getBoard`). Results are capped (e.g. 8) and de-duplicated by `userId` (or by contact email for accountless rows).

Alternative considered: a dedicated SQL query with `ILIKE` prefix matching across a UNION of `company_membership`/`site_membership` joined to `app_user`. Rejected for now as premature — no measured performance problem exists at this data scale, and the simpler approach is far less code to review and maintain. Worth revisiting if a company's team ever grows large enough to matter.

### 3. Picking a search result is a pure frontend prefill — no new backend "invite an existing person" action
Selecting a suggestion in `SiteTeamPanel.vue` just populates the form's `name`/`email`/`cpf`/`phone` fields (and relabels the submit button "Enviar convite") and submits through the exact same `POST /api/sites/{siteId}/members` call as a freshly-typed entry. The backend's existing `resolveOrCreateUser` already does the right thing whether the email matches an existing account or not — there's no meaningful backend difference between "inviting someone we found" and "inviting someone freshly typed who happens to already exist." This keeps the new capability's backend surface to one new read endpoint rather than a second write path to keep in sync with the first.

### 4. `SiteMembership.clientCpf` → `cpf` (generalized), new `SiteMembership.phone`, both optional for every function
Migration `V52__generalize_site_membership_cpf_and_add_phone.sql`: `ALTER TABLE site_membership RENAME COLUMN client_cpf TO cpf;` then `ALTER TABLE site_membership ADD COLUMN phone VARCHAR(20);`. Entity field renamed `clientCpf` → `cpf` (Javadoc updated: no longer CLIENT-only); `phone` added alongside it, threaded through `SiteMembership.invited(...)` and `SiteMembership.accountless(...)`, `AddSiteMemberRequest`, and `SiteMemberResponse`.

Alternative considered: keep `client_cpf` CLIENT-only and add a *separate* general CPF concept. Rejected — there is no product reason CPF should mean something different for a CLIENT than for an ARCHITECT or SERVICE_PROVIDER; the CLIENT-only restriction was never load-bearing, just how the field happened to be introduced.

### 5. A submitted name seeds a *new* pre-registration account's display name; an existing account's name is never touched
`MembershipInvitationIssuer.resolveOrCreateUser` gets a new overload, `resolveOrCreateUser(String email, String displayNameOverride, Instant now)`, that passes `displayNameOverride` into `AppUser.preRegistration(id, email, displayNameOverride, now)` (that factory gets a new overload too, falling back to its existing email-derived default when the override is null/blank) — but only on the *create* branch; the *found-existing-account* branch is unchanged and never mutates `existing.displayName`. The two-arg `resolveOrCreateUser(email, now)` overload stays as-is for `CompanyService`'s unrelated call site, so nothing outside this change's scope is touched.

For the accountless service-provider path, the submitted name continues to go straight onto `SiteMembership.displayName`, as today — unaffected by this decision.

### 6. CPF validation: a small standalone utility, not a Bean Validation annotation
No custom Bean Validation constraint exists anywhere in this codebase yet, and introducing the `ConstraintValidator` scaffolding for a single field used by one endpoint is more machinery than the problem needs. `CpfValidator.isValid(String cpf)` implements the standard two-check-digit algorithm (strips non-digits, rejects non-11-digit and all-repeated-digit sequences, verifies both check digits via the standard mod-11 formula) as a plain static method, called explicitly wherever a CPF is accepted (`SiteMembershipService`). An invalid CPF throws a new `InvalidCpfException`, following the codebase's existing one-exception-per-concern convention. Email's existing `@Email` Jakarta annotation is reused directly (no new machinery needed there); the "required unless accountless service provider" rule is enforced imperatively in the service, mirroring how that branch is already decided today.

## Risks / Trade-offs

- **[Trade-off] In-memory candidate filtering (Decision 2) doesn't scale to very large companies.** → Acceptable at current product scale; revisit only if it becomes a real problem.
- **[Risk] Renaming `client_cpf` → `cpf` (Decision 4) is a breaking column rename.** → Mitigation: single `ALTER TABLE ... RENAME COLUMN`, no data loss, and the field was already optional/nullable so no backfill is needed.
- **[Trade-off] CPF validation checks structure/check-digits only, not registry status (e.g. whether the CPF is actually issued/active).** → Matches what's realistically achievable without a paid external validation service; out of scope.

## Migration Plan

1. Ship the migration + backend changes; `cpf`/`phone` remain optional columns, so existing rows and existing callers (frontend not yet updated) keep working unchanged during rollout.
2. Ship the frontend changes.
3. No rollback complexity beyond the normal revert — the column rename has no forward-only data dependency (nothing else reads `client_cpf` by that name after the rename completes in the same migration).
