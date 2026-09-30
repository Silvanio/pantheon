## Why

Two bugs reported in production use: (1) right after logging in, screens sometimes show no data instead of fetching it; (2) after editing something and navigating between screens, the edited data isn't reflected — stale data persists. Both trace to the same root cause: every data-loading Riverpod provider in the app lives forever once built (no provider anywhere uses `.autoDispose`), and nothing invalidates them on login. A screen that resolved to an error (e.g. a request racing the auth token becoming available) or to now-stale data just keeps showing that forever, across navigations and even across logging out and back in as a different user on the same device. This was already flagged as a known risk when offline support first shipped (`fix-mobile-permission-parity-and-offline-support`'s design.md: "a screen that failed while offline won't automatically retry just because connectivity returned... this is a pre-existing characteristic of the app's provider usage") but never fixed — pull-to-refresh was the accepted stopgap, and it isn't enough.

While investigating, a second thing surfaced, but it turned out **not** to need a code change: the accepted `mobile-app` spec's wording says a "qualifying write" for offline queueing includes "create/submit a Diário de Obra report," but the actual `DailyReportRepository`/`PurchaseRequestRepository` code only ever queues `create` (plus photo/video upload) — every other mutation, including `submit`, is deliberately excluded, with explicit reasoning in code comments: `submit`/`approveStep`/`rejectStep`/`conclude` are the approval workflow itself; edits to an already-existing record's structure (workforce/equipment/activities, core fields, PR items) are excluded because queueing them risks the UI looking out of sync with a server state other screens may already be showing. That reasoning is sound and was already deliberately decided — this change corrects the spec's imprecise wording to match it, rather than changing the code to match the spec.

## What Changes

- **Provider lifecycle**: convert every screen-data `FutureProvider`/`FutureProvider.family` to `.autoDispose`/`.autoDispose.family`, so leaving a screen tears down its cached result and returning re-fetches (hitting the network-first `ApiClient.get`, matching the "online → always fetch fresh" policy already implemented at the HTTP layer). App-wide singletons (`outboxControllerProvider`, `authControllerProvider`, `offlineDbProvider`) are explicitly NOT converted — they must stay alive for the whole session.
- **Session-epoch invalidation**: a new `sessionEpochProvider` that `AuthController` bumps on successful login and on logout/forced logout; screen-data providers depend on it so a provider built (and possibly errored) before/during a login can never leak into the freshly-authenticated session, and one user's cached data can never leak into a different user's session on a shared device.
- **Refresh-on-return for the list → detail → back flow**: `.autoDispose` alone doesn't cover this, since `context.push` keeps the list screen mounted underneath the pushed detail screen the whole time (confirmed live, see design.md). A new `RefreshOnReturn` mixin refreshes a screen's own data whenever a route pushed on top of it is popped, applied to the Diário de Obra list, Pedido de Compra list, and the site home screen.
- **Spec correction, no code change**: fix the `mobile-app` spec's "qualifying write" wording, which currently overstates what's queued (says "create/submit"), to accurately describe the existing, deliberate, already-correct behavior (only `create` and photo/video upload are queued; everything else on an existing record requires connectivity).
- **BREAKING (internal only)**: any code that currently assumes a data provider's `AsyncValue` survives navigation (there shouldn't be any deliberate instance of this, per the investigation, but this is called out since it's a behavior change) will now see the provider re-fetch instead.

Deliberately unchanged: `ApiClient`'s cache layer, `OutboxController`'s sync-drain triggers (connectivity transition + manual), the `requireOnline`/`confirmProceedOffline` dialogs, which mutations are queued vs. require connectivity (this was already correctly scoped), and the existing exclusion of Projetos, Permissões, the Tasks board, and both features' approval workflows from all offline support.

## Capabilities

### New Capabilities
- None.

### Modified Capabilities
- `mobile-app`: corrects the "Offline support for Diário de Obra, Pedido de Compra, and Orçamentos" requirement's "qualifying write" wording to match the app's actual, already-correct behavior (documentation fix, no behavior change); adds a new requirement that screen data providers reset on navigation-away and on login/logout rather than persisting indefinitely (this is the actual bug fix).

## Impact

- **`pantheon-mobile/lib/`**: every `*_providers.dart`/inline `FutureProvider` declaration backing screen content (dashboard, daily report list/detail, purchase request list/detail, equipment, site home, materials, orçamentos, site photo auth headers — full audit in tasks.md); `lib/core/auth/auth_provider.dart` (new session-epoch bump); new `lib/core/widgets/refresh_on_return.dart`; `lib/core/router/app_router.dart` (registers its `routeObserver`); `daily_report_list_screen.dart`, `purchase_request_list_screen.dart`, `site_home_screen.dart` converted from `ConsumerWidget` to `ConsumerStatefulWidget` to use it.
- No repository (`*_repository.dart`) changes — the outbox-queueing scope was already correct.
- No backend or web changes. No new dependencies — reuses the existing Riverpod infrastructure as-is.
