## Why

`add-site-member-people-search` (archived 2026-09-14) let an admin find and reuse a known person's data when adding a team member, but didn't stop two problems: re-submitting someone who is already an active member of the exact obra being managed (a pointless duplicate that only fails after submit, with a generic error), and — new — silently reusing an account that already belongs to a completely different company, which should not be allowed at all. Email is the identity key for both checks (CPF stays optional and unreliable for this purpose, per the user).

## What Changes

- Adding a member is blocked, both in the UI (disabled submit, before the round trip) and authoritatively in the backend, when the submitted email is already an **active** member of *this exact* construction site. Re-submitting an email whose invitation is still pending (`INVITED`, not yet accepted) is unaffected — that already resends the invite, and stays allowed.
- Adding a member is blocked when the submitted email already belongs to an `AppUser` with any membership (company staff or any obra's team) in a **different** company than the one being managed. Reusing a known person **within the same company** (found on a different obra, e.g. via the people-search autocomplete) is explicitly unaffected and keeps working exactly as shipped.
- A new lightweight endpoint lets the frontend check these two conditions for the typed email before submit, so the button can disable proactively instead of only failing after a round trip.

## Capabilities

### New Capabilities
(none — extends `obra-team-management`)

### Modified Capabilities
- `obra-team-management`: adds the same-site-active-duplicate and cross-company block rules, and the UI's proactive disable behavior.

## Impact

- Affected code: `pantheon-service` — `SiteMembershipService.inviteMember` (new cross-company check), new `PersonBelongsToAnotherCompanyException`, new `PersonSearchService` method + `PersonSearchController` endpoint for the pre-submit check.
- Affected code: `pantheon-web` — `SiteTeamPanel.vue` (debounced email-conflict check, disabled submit + inline message), `useSiteMembers.ts` (new type/call).
- No schema changes. No change to the already-shipped people-search/autocomplete behavior for reusing a person within the same company.
