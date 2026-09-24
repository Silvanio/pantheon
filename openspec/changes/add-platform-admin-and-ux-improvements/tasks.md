## 1. Superadmin backend

- [x] 1.1 `AppUser`: add nullable `superAdmin` column/field, `isSuperAdmin()` reading null as `false`.
- [x] 1.2 Migration `V60`: add `super_admin` column; seed the superadmin account (`inextitsolutions@gmail.com`, BCrypt-hashed password, `ACTIVE`, `super_admin = true`), guarded by `WHERE NOT EXISTS`.
- [x] 1.3 New `PlatformAdminService` (`isSuperAdmin`/`requireSuperAdmin`) and `NotSuperAdminException` (mapped to HTTP 403 in `CompanyExceptionHandler`).
- [x] 1.4 Bypass superadmin in `SiteAccessService.resolve` (returns the same `SiteAccessContext(true, null)` shape as company staff), `CompanyService.requireMembership`/`requireAdmin`, `ConstructionSiteService.requireMembership`/`requireAdmin`.
- [x] 1.5 `CompanyRepository.findByNameContainingIgnoreCase`; `CompanyService.listAllCompanies(actingUserId, search, pageable)` gated by `requireSuperAdmin`; new `PlatformAdminController` (`GET /api/admin/companies`).
- [x] 1.6 `JwtService.issueToken` carries a `superAdmin` claim (display/routing hint only); wired through `AuthController.register`/`login` and `OAuth2LoginSuccessHandler`; `UserResponse` also carries the flag.
- [x] 1.7 Backend tests: `PlatformAdminServiceTest`, superadmin-bypass tests in `SiteAccessServiceTest`/`CompanyServiceTest` (including `listAllCompanies`), full `./mvnw test` green.

## 2. Superadmin frontend

- [x] 2.1 `useAuth.ts`: `isSuperAdmin` computed, decoded from the JWT (display/routing only).
- [x] 2.2 `useCompanies.ts`: `listAllCompanies(options)`.
- [x] 2.3 New `AdminCompaniesView.vue` (`/admin` route): a dropdown-with-search filter button/popover for companies (mirroring `GlobalTasksBoardView.vue`'s obra filter — up to 10 results, debounced server-side search from 3 characters via `listAllCompanies`); selecting one shows its obras as dashboard-style cards (reusing `SitePhoto`/`StatusBadge`, the same progress-bar/placeholder-progress logic as `DashboardView.vue`) linking to the normal obra detail view, plus a link to its settings screen.
- [x] 2.4 Router guard (`router/index.ts`): a superadmin skips the company-onboarding logic entirely and is sent to `/admin` in place of the dashboard.

## 3. Page-size selector (Diário de Obra, Equipamentos, Pedido de Compra, Orçamentos)

- [x] 3.1 `DailyReportsPanel.vue`, `EquipmentPanel.vue`, `PurchaseRequestPanel.vue`, `OrcamentoListPanel.vue`: replace the hardcoded `PAGE_SIZE` constant with a `pageSize` ref and a `[1, 5, 10]` `<select>` next to the existing prev/next controls, visible whenever the list is non-empty; changing it resets to page 0 and reloads.
- [x] 3.2 New shared `common.pagination.pageSizeLabel` i18n string, reused across all four panels.

## 4. Breadcrumb navigation (Diário de Obra, Pedido de Compra, Orçamentos detail views)

- [x] 4.1 `SiteDetailView.vue`: seed `activeTab` from `route.query.tab` on load (falling back to the first visible tab as before) — fixes the root cause (the tab was never part of the URL).
- [x] 4.2 New shared `SiteBreadcrumb.vue` (obra-name link + section link carrying `?tab=`, plus a leading back-arrow icon that navigates to the same section target), rendered in `AppHeader`'s `#left` slot rather than inline in the page body.
- [x] 4.3 `DailyReportDetailView.vue`, `PurchaseRequestDetailView.vue`, `OrcamentoDetailView.vue`: replace the back button with `SiteBreadcrumb`; replace their post-delete `router.back()` with an explicit push to `/sites/:id?tab=:tab`.
- [x] 4.4 Bug fix (follow-up report): `SiteBreadcrumb.vue` split its single ambiguous "section" crumb into two — a "Lista de {label}" link (new i18n key `common.breadcrumb.list`) back to the list, and a final non-clickable `{label}` leaf marking the current detail page — so the breadcrumb no longer looks like "you are here" while actually being a link elsewhere.

## 5. Checkbox styling

- [x] 5.1 New `.field-checkbox` class in `style.css`'s `@layer components` (border, rounded, `accent-color`, focus ring, both themes).
- [x] 5.2 Apply it at all 4 existing checkbox call sites: `PurchaseRequestDetailView.vue` (select-all and per-row), `DailyReportDetailView.vue` (weather-blocked-tasks), `ScheduleGantt.vue` (task-done toggle, with its existing compact size preserved).

## 6. Global Tasks Board obra filter

- [x] 6.1 `GlobalTaskBoardService.requireAdmin`: add the same `PlatformAdminService` bypass used elsewhere (found missing during live verification — this service had its own independent membership check, not routed through `CompanyService`).
- [x] 6.2 `GlobalTasksBoardView.vue`: fetch the company's full obra list (`useConstructionSites().listSites`) alongside the board; add a filter dropdown (button + popover with a search input and a capped, up-to-10-result list), filtering client-side by name once 3+ characters are typed; selecting an obra filters `cardsForColumn` to that obra's cards; a "Limpar filtro" action resets it.
- [x] 6.3 Backend test: `GlobalTaskBoardServiceTest` — superadmin can build the board without a company membership.

## 7. Global Tasks Board card enrichment (assignees, attachments, comments)

- [x] 7.1 `GlobalTaskBoardService`: inject `TaskCardAssigneeRepository`, `TaskCommentRepository`, `SiteDocumentProjectAttachmentRepository`, `SiteMembershipRepository`, `AppUserRepository`; in `build()`, compute `assigneeIdsByCard`/`commentCountByCard`/`attachmentCountByCard` across the full cross-site card-id list (same repository calls `TaskCardService.getBoard` uses for one site); resolve the distinct assignee site-membership ids into `SiteMemberResponse`s, mirroring `SiteMembershipService.listMembers`'s AppUser-or-accountless-name fallback exactly.
- [x] 7.2 `GlobalTaskBoard` record, `GlobalTaskCardResponse`, `GlobalTaskBoardResponse`: carry the new per-card fields (`assigneeIds`, `commentCount`, `attachmentCount`) and the board-level `assignees` list.
- [x] 7.3 `GlobalTaskBoardController`: thread the new fields through to the response.
- [x] 7.4 `useGlobalTasksBoard.ts`: add the new fields to `GlobalTaskCard`/`GlobalTaskBoard` (reusing the existing `SiteMember` type for `assignees`).
- [x] 7.5 `GlobalTasksBoardView.vue`: add `memberById`/`memberLabel`/`memberInitials` (identical to `TasksBoardPanel.vue`'s); add the assignee-avatars row and the attachment/comment-icon row to each card, using the exact same icon SVGs and visibility rules (attachment icon is tooltip-only, comment icon shows its count) as the per-obra board.
- [x] 7.6 Backend test: `GlobalTaskBoardServiceTest` — a card's assignees/comments/attachments resolve correctly across sites, including the accountless-membership name fallback.

## 8. Verification

- [x] 8.1 Backend: `./mvnw test` green (full suite, re-run after every change including the `GlobalTaskBoardService` fix and card-enrichment additions).
- [x] 8.2 Frontend: `npx vue-tsc --noEmit` and `npm run build` green (re-run after every change).
- [x] 8.3 Live end-to-end check against the real dev database and browser: superadmin login redirects to `/admin`; company list/search works; selecting a company shows its real obras and a working settings link; opening an obra shows full content (every tab, real summary data) despite zero membership rows; page-size selector on Pedido de Compra re-fetches correctly (confirmed via the "Página 1 de 7" summary appearing after switching to size 1); breadcrumb (now in the page header, with a back-arrow icon) on a Pedido de Compra detail page correctly returns to the Pedido de Compra tab (not Resumo); checkbox appearance confirmed via screenshot in dark mode; Global Tasks Board obra filter confirmed live (selecting "Obra Matheus" narrowed the board to its one card, "Limpar filtro" restored the full board) — this run is what surfaced and confirmed the `GlobalTaskBoardService` superadmin gap and its fix.
- [x] 8.4 Live re-check of `AdminCompaniesView.vue` after redesign: the "Selecionar empresa" dropdown popover opens, lists companies, selecting one renders its obras as dashboard-style cards (photo, date, progress bar, status badge — confirmed via screenshot matching `DashboardView.vue`'s own card look), and clicking a card navigates into the normal obra detail view (confirmed by the obra's own tab bar, including "Resumo", appearing).
- [x] 8.5 Live re-check of the card-enrichment work: fetched the real board API response directly and confirmed `assigneeIds`/`commentCount`/`attachmentCount` and the resolved `assignees` list are populated correctly for real cards with data (one card had an assignee "Silva Engenheiro", 2 comments, 1 attachment); confirmed in the rendered page text that the assignee initials ("SI"), comment count ("2"), due date, labels, and obra badge all appear on the corresponding cards.
- [x] 8.6 Live re-check of the breadcrumb fix on all three detail views: Pedido de Compra, Orçamento, and Diário de Obra detail pages each now show `< [icon] Obra Silvânio e Angela / Lista de {section} / {section}` (verified via the accessibility tree showing both the "Lista de X" link and a separate non-clickable "X" leaf for all three); clicking "Lista de Pedido de Compra" confirmed to land back on the correct tab.
