## 1. Backend check + endpoint

- [x] 1.1 Add `EmailConflictCheck` DTO (`activeOnThisSite`, `existsInAnotherCompany`).
- [x] 1.2 Add `PersonSearchService.checkEmailConflicts(siteId, actingUserId, email)` and `belongsToAnotherCompany(userId, companyId)` (reused by both the check and `inviteMember`'s enforcement).
- [x] 1.3 Add `PersonSearchController` endpoint: `GET /api/sites/{siteId}/people/email-check?email=...`.

## 2. Backend enforcement

- [x] 2.1 Add `PersonBelongsToAnotherCompanyException` (409), wired into `ConstructionExceptionHandler`.
- [x] 2.2 `SiteMembershipService.inviteMember(...)`: after the existing same-site-active check (unchanged) and only when creating a genuinely new membership on this site, check cross-company (skipped for a freshly pre-registered account) and throw the new exception when it fails.

## 3. Frontend

- [x] 3.1 `useSiteMembers.ts`: add `EmailConflictCheck` type and `checkEmailConflicts(siteId, email)` call.
- [x] 3.2 `SiteTeamPanel.vue`: debounced call on the email field (`onEmailInput`, alongside the existing autocomplete debounce and re-triggered after selecting a suggestion); `submitBlocked` computed disables the submit button and shows the matching inline message; guarded in `onSubmit` too as a second line of defense.

## 4. Verification

- [x] 4.1 Backend: unit tests for `checkEmailConflicts` (unknown email, same-site active, pending-invite not flagged) and `belongsToAnotherCompany` (via company staff, via another company's site, false when same-company-only), plus `inviteMember` tests for the new rejection and for allowing same-company reuse. Full suite: 131/131 passing.
- [x] 4.2 Frontend: `npm run build` green.
- [ ] 4.3 Manual check left for the user: same-obra duplicate disables submit; pending invite does not; cross-company email disables submit with a clear message; same-company different-obra reuse still works. **Not run in this session** — the assistant could not log in to verify live.
