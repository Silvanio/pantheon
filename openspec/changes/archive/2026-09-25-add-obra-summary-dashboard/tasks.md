## 1. Backend: new repository support for Tasks/Projetos/Equipe

- [x] 1.1 `TaskCardRepository.java`: add `long countByConstructionSiteId(UUID constructionSiteId)` and `List<TaskCard> findTop5ByConstructionSiteIdOrderByCreatedAtDesc(UUID constructionSiteId)`.
- [x] 1.2 `SiteDocumentProjectRepository.java`: add `long countByConstructionSiteId(UUID constructionSiteId)` and `List<SiteDocumentProject> findTop5ByConstructionSiteIdOrderByCreatedAtDesc(UUID constructionSiteId)`.
- [x] 1.3 `SiteMembershipRepository.java`: add `long countByConstructionSiteId(UUID constructionSiteId)`.

## 2. Backend: DTOs

- [x] 2.1 `RecentItemResponse(UUID id, String title, Instant createdAt)` — shared shape for a recent Pedido de Compra/Orçamento/Task/Projeto item (title = the resource's own name/title field).
- [x] 2.2 `CountStatResponse(long total, List<RecentItemResponse> recent)` — shared shape for Pedido de Compra/Orçamentos/Tasks/Projetos sections.
- [x] 2.3 `SiteSummaryResponse` with nullable fields: `Integer schedulePercentComplete`, `DailyReportSummary` (`long total`, `LocalDate lastReportDate` nullable), `CountStatResponse purchaseRequests`, `CountStatResponse orcamentos`, `EquipmentSummary` (`long total`, `long unavailable`), `CountStatResponse projects`, `CountStatResponse tasks`, `Long teamMembersCount` — each field `null` when that capability resolves `HIDDEN` for the caller.

## 3. Backend: SiteSummaryService

- [x] 3.1 Add `SiteSummaryService.build(UUID siteId, UUID actingUserId)`: for schedule, call `scheduleService.computeProgress(siteId)` directly (no permission gate needed — progress isn't a separate capability). For daily reports/purchase requests/orçamentos/equipment, call the existing paginated `list(siteId, actingUserId, PageRequest.of(0, 3))` (or a size appropriate to the section's stat, e.g. equipment needs a full count so use `PageRequest.of(0, 1)` plus a new `countByConstructionSiteIdAndStatus` for unavailable — see 3.2), wrapped in a try/catch on `ForbiddenCapabilityException` → section omitted (`null`).
- [x] 3.2 `EquipmentRepository.java`: add `long countByConstructionSiteIdAndStatus(UUID constructionSiteId, EquipmentStatus status)`, used for the equipment section's `unavailable` count (`UNAVAILABLE` status).
- [x] 3.3 For Tasks and Projetos (no existing service `list`): `SiteSummaryService` does its own `siteAccessService.requireAccess` + `permissionService.requireVisible(TASKS / DOCUMENT_PROJECTS)`, then calls the new repository methods from task group 1, mapping the top 3 of the 5 fetched into `RecentItemResponse` (title = `TaskCard.title` / `SiteDocumentProject.name`).
- [x] 3.4 For Team: `SiteSummaryService` calls `permissionService.requireVisible(TEAM_MANAGE)` then `siteMembershipRepository.countByConstructionSiteId(siteId)`.
- [x] 3.5 Add `SiteSummaryController.java`: `GET /api/construction-sites/{siteId}/summary` → `SiteSummaryResponse`.

## 4. Backend tests

- [x] 4.1 `SiteSummaryServiceTest.java`: full-access member sees every section; a member with one `HIDDEN` capability gets that section omitted (`null`) while others are present; purchase-request/orçamento/task/project recent lists cap at 3 and are newest-first; equipment `unavailable` count is correct; daily-report `lastReportDate` reflects the most recent report.
- [x] 4.2 Run `./mvnw test` from `pantheon-service/` and confirm the full suite passes.

## 5. Frontend: composable

- [x] 5.1 `pantheon-web/src/composables/useSiteSummary.ts`: types mirroring the backend DTOs (all section fields optional/nullable), `getSummary(siteId): Promise<SiteSummary>`.

## 6. Frontend: SiteSummaryPanel.vue

- [x] 6.1 New `pantheon-web/src/components/SiteSummaryPanel.vue` (props: `siteId`): fetch on mount, render a card grid (existing design tokens — rounded-2xl white cards, `blueprint-*`/`emerald-*`/`amber-*` accents matching other panels) with one card per present section: Cronograma (progress bar + %), Diário de Obra (total + last report date), Pedido de Compra (total + recent list), Orçamentos (total + recent list), Equipamentos (total + unavailable highlighted), Projetos (total + recent list), Tasks (total + recent list), Equipe (member count). A recent item created within 7 days gets a small "Novo" badge. Clicking a recent item or a card navigates to that section/item's existing route.
- [x] 6.2 A "Pendências" highlight banner above the grid (amber-toned) showing the combined count of purchase-requests-awaiting-approval + draft-orçamentos, computed client-side from the two sections' own data (not a new backend field) — hidden entirely when both are 0 or both sections are absent.
- [x] 6.3 Add `siteSummary.*` i18n strings to `pt-BR.json`.

## 7. Frontend: tab wiring

- [x] 7.1 In `SiteDetailView.vue`: add `'summary'` to the `Tab` union and to the front of `TAB_ORDER`; mount `<SiteSummaryPanel v-if="activeTab === 'summary'" :site-id="siteId" />` (ungated — always visible, like `permissions`'s admin-only carve-out but visible to everyone); change the default-tab logic so `'summary'` is always the landing tab regardless of which other tabs are visible (not run through `isTabVisible`/`TAB_CAPABILITY`, since it aggregates all sections rather than belonging to one capability).
- [x] 7.2 Add a "Resumo" nav button (desktop sidebar + mobile fallback nav) as the first item, following the existing nav-button markup pattern.

## 8. Mobile: summary section

- [x] 8.1 Add `pantheon-mobile/lib/features/site/site_summary.dart` (or extend `site_home_screen.dart` directly): repository call to `GET /api/construction-sites/{siteId}/summary`, models mirroring the backend DTOs.
- [x] 8.2 In `SiteHomeScreen`, add a summary/stats section (cards, matching the app's existing card visual language) above the existing entry-card `GridView`, showing the same sections as web (progress bar, counts, recent items with "Novo" badges) sized for a phone screen (vertical stack or horizontally-scrollable row of stat cards, not a dense multi-column grid).

## 9. Verification

- [x] 9.1 Backend: `./mvnw test` green.
- [x] 9.2 Frontend: `npx vue-tsc --noEmit` and `npm run build` green.
- [x] 9.3 Mobile: `flutter analyze` and `flutter test` green.
- [x] 9.4 Live/static check: confirm the summary is what appears first on entering an obra (web), confirm a hidden capability's card doesn't appear for a restricted member, confirm the Pendências highlight math, confirm "Novo" badges only appear on genuinely recent items.
