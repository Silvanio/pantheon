## Context

Two rules were requested for the add-team-member form, and both were clarified directly with the user before design (their answers are load-bearing, not assumptions):
1. Reusing a known person **within the same company** (the exact scenario `add-site-member-people-search`'s autocomplete exists for) must keep working — only a duplicate on the *exact same obra* should block.
2. The "already exists" check is keyed on **email**, not CPF — CPF is optional and often absent, so it can't reliably detect a duplicate person; email is the field every path already requires (except the accountless service-provider case, which is exempt from both rules below since it has no account/email to key on).

## Goals / Non-Goals

**Goals:**
- Block re-submitting someone already an *active* member of the obra currently being managed, proactively (disabled button) where the data is already available client-side, and authoritatively server-side either way.
- Block adding someone whose email already belongs to an account with a membership in a *different* company, both proactively (via a new lightweight check) and authoritatively server-side.

**Non-Goals:**
- No change to same-company reuse across obras — that keeps working exactly as shipped.
- No change for a still-pending (`INVITED`, not yet accepted) invitation on this site — resubmitting that continues to resend the invite (existing `reissue` behavior), not treated as a blocked duplicate.
- No CPF-keyed uniqueness rule of any kind — explicitly out per the user's clarification.
- The accountless service-provider path (no email) is unaffected by either rule — there's no account to check membership against.

## Decisions

### 1. Same-obra duplicate check runs client-side off already-loaded data; cross-company check needs a new endpoint
`SiteTeamPanel.vue` already loads the current obra's full member list (`listMembers`) to render the roster. Checking "is this email already an ACTIVE member here" is a pure client-side lookup against that list — no new endpoint needed for the proactive UI disable. The cross-company check has no equivalent local data (the frontend never has visibility into other companies' membership, by design — see the people-search change's Decision 1), so it needs a dedicated server round trip.

### 2. One combined endpoint, not two
`GET /api/sites/{siteId}/people/email-check?email=...` returns `{ activeOnThisSite, existsInAnotherCompany }` in one call, debounced on the email field (same pattern as the existing CPF/email autocomplete). Returning both flags from one endpoint — even though `activeOnThisSite` is also computable client-side — keeps the server as the single source of truth for the *authoritative* answer the submit button's disabled state relies on, avoiding client/server drift if the two ever disagree (e.g., a stale local member list).

### 3. Cross-company check reveals existence only, not identity
The endpoint returns booleans only — never the other company's name, the person's other roles, or any other detail. This matches the same privacy posture as the people-search feature (company-scoped data never crosses tenant boundaries) while still allowing a yes/no "already taken elsewhere" signal, the same category of information an ordinary "email already registered" signup check reveals.

### 4. Server-side enforcement order in `inviteMember`
After resolving the account (`resolveOrCreateUser`): first the existing same-site-active check (`MemberAlreadyActiveException`, unchanged), then the new cross-company check (new `PersonBelongsToAnotherCompanyException`) — only reached if the account is *not* already active on this exact site, and only meaningful for an account that already existed (a freshly pre-registered account has no prior memberships, so the check is a no-op for genuinely new people).

## Risks / Trade-offs

- **[Trade-off] The cross-company check is a live existence oracle for any email an admin cares to type.** → Same shape of leak as any "email already registered" check (ubiquitous on signup forms), gated behind the same `MANAGE`-on-team permission every other people-facing endpoint here already requires — not exposed to unauthenticated or unprivileged callers.
