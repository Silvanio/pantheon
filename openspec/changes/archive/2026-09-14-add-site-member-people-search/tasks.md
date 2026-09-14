## 1. Schema

- [x] 1.1 Migration `V52__generalize_site_membership_cpf_and_add_phone.sql`: rename `site_membership.client_cpf` to `cpf`, add nullable `phone VARCHAR(20)`.

## 2. `SiteMembership` entity and DTOs

- [x] 2.1 `SiteMembership.java`: rename `clientCpf` field/getter to `cpf` (update Javadoc — no longer CLIENT-only); add `phone` field/getter; thread `phone` through the `invited(...)` and `accountless(...)` factories.
- [x] 2.2 `AddSiteMemberRequest`: add `displayName` (the general name field) and `phone` fields; add `@Email` on `email`; update Javadoc.
- [x] 2.3 `SiteMemberResponse`: add `phone`.

## 3. CPF validation

- [x] 3.1 Add `CpfValidator.isValid(String cpf)` (strip non-digits, reject non-11-digit and all-repeated-digit values, verify both check digits via the standard mod-11 algorithm).
- [x] 3.2 Add `InvalidCpfException` and wire it to the existing exception-handling convention (HTTP 400). Also added `EmailRequiredException` for the "email required unless accountless service provider" rule.

## 4. Name-seeding for new pre-registered accounts

- [x] 4.1 `AppUser.preRegistration(...)`: add an overload accepting an optional display-name override, falling back to the existing email-derived default when null/blank.
- [x] 4.2 `MembershipInvitationIssuer.resolveOrCreateUser(...)`: add a 3-arg overload (`email, displayNameOverride, now`) that only applies the override on the *create* branch, never touching an existing account's name; keep the existing 2-arg overload delegating with `null` for `CompanyService`'s unrelated call site.

## 5. `SiteMembershipService`

- [x] 5.1 `inviteMember(...)`: accept `displayName`/`phone`; validate CPF via `CpfValidator` when present; pass `displayName` into the new `resolveOrCreateUser` overload; persist `phone` on the new `SiteMembership`.
- [x] 5.2 `addAccountlessServiceProvider(...)`: accept `phone`; validate CPF via `CpfValidator` when present.
- [x] 5.3 `attachAccount(...)`: pass the existing membership's `displayName` into `resolveOrCreateUser`'s new overload, so a freshly-created account for a previously-accountless service provider inherits their already-known name.
- [x] 5.4 `listMembers(...)`: include `phone` in `SiteMemberResponse`.
- [x] 5.5 `SiteMembershipController.add(...)`: enforce "email required unless accountless service provider" explicitly (throws `EmailRequiredException` otherwise) before branching; pass `request.displayName()`/`request.phone()` through to both service methods.

## 6. People search

- [x] 6.1 Add `PersonSearchResult` DTO (`name`, `email`, `cpf`, `phone`).
- [x] 6.2 Add `PersonSearchService`: given a `siteId` and query, resolve the site's company, gather `CompanyMembership`s + this company's sites' `SiteMembership`s (resolving linked `AppUser`s in bulk), filter by CPF/email/name prefix match (min 3 chars), de-duplicate, cap results (8).
- [x] 6.3 Add `PersonSearchController`: `GET /api/sites/{siteId}/people/search?q=...`, gated by `requireManage(..., PermissionCapability.TEAM_MANAGE)` (same gate as adding a member).

## 7. Frontend

- [x] 7.1 `useSiteMembers.ts`: add `PersonSearchResult` type and `searchPeople(siteId, query)` call; extend `AddSiteMemberData`/`SiteMember` types with `displayName`(required)/`phone`.
- [x] 7.2 `SiteTeamPanel.vue`: unified `displayName` (required) and `phone` (optional) inputs for every function; CPF field now visible for every function, not just CLIENT.
- [x] 7.3 `SiteTeamPanel.vue`: debounced (300ms) autocomplete on the CPF and email inputs, calling `searchPeople`; suggestion dropdown under the email field; selecting one fills `displayName`/`email`/`cpf`/`phone` and flips the submit button's label to "Enviar convite" (`siteTeam.form.sendInvite`), reset on further typing or form reset.
- [x] 7.4 `pt-BR.json`: added `siteTeam.form.phone`, `siteTeam.form.sendInvite`, `siteTeam.form.emailOptionalHint`, `siteTeam.form.invalidCpf`, `siteTeam.form.knownPerson` (reused existing `displayName`/`cpf` keys for labels).

## 8. Verification

- [x] 8.1 Backend: added/updated unit tests in `SiteMembershipServiceTest` (phone persisted, CPF validation accepted/rejected, existing-account name never overwritten, new-account name seeded, accountless-provider CPF validation), new `CpfValidatorTest` (5 cases), new `PersonSearchServiceTest` (5 cases: cross-obra same-company match, CPF-prefix match, cross-company isolation, short-query no-op, company-staff-with-no-site-membership match). Full suite: 123/123 passing, including the full-context test applying migration V52 against the real dev database.
- [x] 8.2 Frontend: `npm run build` (typecheck + build) green.
- [ ] 8.3 Manual check left for the user: search-as-you-type finds an existing person and pre-fills correctly; a genuinely new person still adds exactly as before; CPF/email validation errors surface sensibly in the UI. **Not run in this session** — the assistant could not log in to verify live.
