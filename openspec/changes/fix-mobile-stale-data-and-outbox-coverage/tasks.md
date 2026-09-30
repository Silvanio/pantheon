## 1. Session-epoch foundation

- [x] 1.1 Add `sessionEpochProvider` (a `StateProvider<int>`, initial value `0`) in `lib/core/auth/auth_provider.dart`, next to `authControllerProvider`.
- [x] 1.2 In `AuthController`, bump `sessionEpochProvider` (`ref.read(sessionEpochProvider.notifier).state++`) on successful login, and on every logout/forced-logout path. Also bumped on `_restore()`'s authenticated branch (a cold-start session restore is the same "become authenticated" transition as an explicit login, and is subject to the same race).

## 2. Convert screen-data providers to `.autoDispose`

For each provider below: change `FutureProvider`/`FutureProvider.family` to `FutureProvider.autoDispose`/`FutureProvider.autoDispose.family`, and add `ref.watch(sessionEpochProvider);` as the first line of its build function (value unused — it's a dependency trigger so the provider also resets on login/logout, not just on navigation-away). Do NOT touch `authControllerProvider`, `outboxControllerProvider`, or `offlineDbProvider` — those must stay app-wide singletons.

- [x] 2.1 `lib/core/widgets/site_photo.dart` — `_authHeaderProvider` (not screen-scoped; still needs the session-epoch dependency so a stale auth header can't survive a re-login with a different token).
- [x] 2.2 `lib/features/dashboard/dashboard_providers.dart` — `dashboardDataProvider`.
- [x] 2.3 `lib/features/site/site_home_screen.dart` — every `FutureProvider.family` in the file (site detail, my-permissions, my-company-memberships).
- [x] 2.4 `lib/features/site/team_screen.dart` — `_membersProvider`.
- [x] 2.5 `lib/features/site/site_summary_section.dart` — its `FutureProvider.family`.
- [x] 2.6 `lib/features/site/permissions_screen.dart` — its `FutureProvider.family` (this screen's own GET already opts out of caching via `offlineCapable: false`; autoDispose is still needed so a re-visit re-fetches instead of reusing a stale result).
- [x] 2.7 `lib/features/daily_reports/daily_report_list_screen.dart` — its `FutureProvider.family`.
- [x] 2.8 `lib/features/daily_reports/daily_report_detail_screen.dart` — every `FutureProvider.family` in the file (detail, media, attachments, imported invoices, delivered materials, site members, equipment catalog, can-manage-materials, authority).
- [x] 2.9 `lib/features/purchase_requests/purchase_request_list_screen.dart` — both `FutureProvider.family`s (list, can-manage).
- [x] 2.10 `lib/features/purchase_requests/purchase_request_detail_screen.dart` — every `FutureProvider.family` in the file (detail, comparison, authority).
- [x] 2.11 `lib/features/orcamentos/orcamento_list_screen.dart` — `_listProvider`.
- [x] 2.12 `lib/features/orcamentos/orcamento_detail_screen.dart` — `_detailProvider`.
- [x] 2.13 `lib/features/equipment/equipment_list_screen.dart` — `_listProvider`.
- [x] 2.14 `lib/features/materials/material_list_screen.dart` — both `FutureProvider.family`s (materials, can-manage).
- [x] 2.15 `lib/features/projects/project_browser_screen.dart` — `_contentsProvider`.
- [x] 2.16 `lib/features/tasks/task_board_screen.dart` — `_boardProvider` (still gets the lifecycle fix even though its GET is `offlineCapable: false` — this is about provider staleness, not caching).
- [x] 2.17 Re-ran `grep -rn "FutureProvider" lib/` — zero non-`.autoDispose` matches remain; 30 providers converted, all 30 also watch `sessionEpochProvider` (verified by count).

## 3. Documentation-only spec correction

- [x] 3.1 Confirmed — the delta spec is the only change needed, no repository code touched.

## 4. Verification (round 1 — iOS Simulator access recovered mid-task)

- [x] 4.1 `flutter analyze` — clean ("No issues found!"); `flutter test` — all 11 existing tests pass, including the widget test that boots the app to the login screen (exercises the touched `AuthController`/router path).
- [x] 4.2 Manual: logged in on a booted iOS Simulator (real login screen, real backend) — dashboard loaded obra/stats data immediately, no empty state.
- [x] 4.3 Manual (round 1, revealed a gap): created a draft report, pushed into its detail screen, submitted it via a direct API call (simulating the app's own submit action), pressed back — **the list still showed "Rascunho"**. Root-caused to `context.push` keeping the list screen mounted underneath the whole time (see design.md's new Decision) — `.autoDispose` alone doesn't cover this. Led to task group 5 below.
- [x] 4.4 Manual: logged out via Perfil → Sair, logged back in — dashboard fetched and showed fresh data immediately post-login, confirming the `sessionEpochProvider` bump-on-auth-change path works end to end. Tested as the same user logging back in (no second test account was set up), so this confirms the reset-on-auth-transition mechanism fires correctly but doesn't separately exercise cross-user data isolation with two distinct accounts.
- [ ] 4.5 Manual: spot-check that opening a bottom sheet/dialog on top of a converted screen doesn't cause a visible extra reload. **Not yet run** (design.md's reasoning for why this is low-risk still stands, but not spot-checked live).
- [ ] 4.6 Manual: confirm existing offline behavior is unchanged (queue vs. block). **Not yet run.**

## 5. Refresh-on-return for the push/pop navigation gap (found during verification, see design.md)

- [x] 5.1 Add `lib/core/widgets/refresh_on_return.dart`: a `routeObserver` (`RouteObserver<ModalRoute<dynamic>>`) and a `RefreshOnReturn` mixin that calls `onReturnVisible()` on `didPopNext`.
- [x] 5.2 Register `routeObserver` on `GoRouter`'s `observers` in `lib/core/router/app_router.dart`, alongside the existing `SentryNavigatorObserver`.
- [x] 5.3 Convert `DailyReportListScreen` to `ConsumerStatefulWidget`, mix in `RefreshOnReturn`, invalidate `dailyReportListProvider(siteId)` in `onReturnVisible`.
- [x] 5.4 Convert `PurchaseRequestListScreen` to `ConsumerStatefulWidget`, mix in `RefreshOnReturn`, invalidate `purchaseRequestListProvider(siteId)` in `onReturnVisible`.
- [x] 5.5 Convert `SiteHomeScreen` to `ConsumerStatefulWidget`, mix in `RefreshOnReturn`, invalidate `_siteProvider`, `_myPermissionsProvider`, and `siteSummaryProvider` in `onReturnVisible` — it has the identical staleness risk for its own stat cards after pushing into any obra entry and coming back. Confirmed live in round 2: after creating 3 reports total via the Diário de Obra flow and returning to the site home, its "Diário de Obra" stat card correctly updated from a stale "1" to "3" with the latest date, without a manual refresh.
- [x] 5.6 `flutter analyze` (whole project) and `flutter test` — both clean/passing after the conversions.

## 6. Verification (round 2 — re-ran the exact scenario that failed in round 1)

- [x] 6.1 Rebuilt (`flutter build ios --simulator`) and relaunched on the same booted Simulator.
- [x] 6.2 Repeated 4.3 exactly: created a new draft report, pushed into its detail, submitted via a direct API call, pressed back — **the list immediately showed "Em aprovação"**. Confirms `RefreshOnReturn` closes the gap round 1 found.
- [ ] 6.3 Re-run 4.4, 4.5, 4.6 (still outstanding from round 1 — not blocking, but not yet done).
