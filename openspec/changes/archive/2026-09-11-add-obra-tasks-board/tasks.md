## 1. Database migrations

- [x] 1.1 `V44__create_task_column_table.sql` — `task_column` (id, company_id FK, name, sort_order, created_at)
- [x] 1.2 `V45__create_task_card_table.sql` — `task_card` (id, construction_site_id FK, column_id FK → task_column, title, description nullable, sort_order, created_by, created_at, updated_at)
- [x] 1.3 `V46__create_task_label_tables.sql` — `task_label` (id, construction_site_id FK, name, color_hex, created_at) and join table `task_card_label` (id, card_id FK, label_id FK, unique index)
- [x] 1.4 `V47__create_task_comment_table.sql` — `task_comment` (id, card_id FK → task_card, author_id, body, created_at)

## 2. Permission capability

- [x] 2.1 Add `TASKS` to `PermissionCapability` enum (`pantheon-service/.../entity/PermissionCapability.java`)
- [x] 2.2 Add `TASKS` defaults to `SitePermissionService.DEFAULTS` for every `ConstructionFunction`, mirroring `DAILY_REPORT` (`ENGINEER`/`ARCHITECT`/`SITE_FOREMAN` → `MANAGE`, `CLIENT`/`SERVICE_PROVIDER` → `VIEW`)
- [x] 2.3 Update `obra-permission-management` doc comment on `SitePermissionService` if needed to reflect the new capability (no change needed — comment already describes the general cascade, not an exhaustive capability list)

## 3. Backend: entities, repositories, exceptions

- [x] 3.1 `TaskColumn` entity (company-scoped, service-generated UUID, `name`, `sortOrder`)
- [x] 3.2 `TaskCard` entity (obra-scoped, `columnId`, `title`, `description`, `sortOrder`, `createdBy`, timestamps)
- [x] 3.3 `TaskLabel` entity (obra-scoped, `name`, `colorHex`) and `TaskCardLabel` join entity
- [x] 3.4 `TaskComment` entity (`cardId`, `authorId`, `body`, `createdAt`)
- [x] 3.5 `TaskColumnRepository`, `TaskCardRepository` (include `existsByColumnId`), `TaskLabelRepository`, `TaskCardLabelRepository`, `TaskCommentRepository`
- [x] 3.6 Exceptions: `TaskColumnNotFoundException`, `TaskColumnInUseException`, `TaskCardNotFoundException`, `TaskLabelNotFoundException`, reuse `NotCompanyAdminException` for admin checks

## 4. Backend: company-task-columns (admin-only)

- [x] 4.1 `TaskColumnService` with private `requireAdmin(companyId, userId)` (mirroring `CompanyService`/`PlanService`/`ConstructionSiteService`), `list`, `create`, `rename`, `reorder`, `delete` (blocked via `TaskColumnInUseException` when `TaskCardRepository.existsByColumnId` is true)
- [x] 4.2 `TaskColumnController`: `GET/POST /api/companies/{companyId}/task-columns`, `PUT /api/companies/{companyId}/task-columns/{columnId}`, `PUT .../{columnId}/sort-order`, `DELETE /api/companies/{companyId}/task-columns/{columnId}`
- [x] 4.3 DTOs: `TaskColumnRequest`, `TaskColumnReorderRequest`, `TaskColumnResponse` (static `from(entity)` factory)
- [x] 4.4 Unit tests: `TaskColumnServiceTest` covering admin-only enforcement and in-use delete blocking

## 5. Backend: obra-tasks-board (cards, labels, comments)

- [x] 5.1 `TaskCardService`: `requireSite`, read only requires `SiteAccessService.requireAccess` (real `VIEW`, like `DAILY_REPORT`), `requireManage` (write, `TASKS` `MANAGE`) following `PurchaseRequestService` pattern; `getBoard(siteId)` returning columns + that site's cards + label ids per card, `createCard`, `moveCard` (no separate title/description edit — out of scope for the simpler version)
- [x] 5.2 `TaskLabelService`: `create`, `attach`, `detach`, `list`, scoped to `constructionSiteId`
- [x] 5.3 `TaskCommentService`: `list` (view), `add` (manage)
- [x] 5.4 `TaskCardController`: `GET /api/construction-sites/{siteId}/task-board`, `POST /api/construction-sites/{siteId}/task-cards`, `PATCH /api/task-cards/{cardId}/move` (routes use the existing `/api/construction-sites/{siteId}/...` convention, not `/api/sites/...` as originally sketched in design.md)
- [x] 5.5 `TaskLabelController`: `GET/POST /api/construction-sites/{siteId}/task-labels`, `POST/DELETE /api/task-cards/{cardId}/labels/{labelId}`
- [x] 5.6 `TaskCommentController`: `GET/POST /api/task-cards/{cardId}/comments`
- [x] 5.7 DTOs: `TaskCardCreationRequest`, `TaskCardResponse`, `TaskBoardResponse`, `MoveTaskCardRequest`, `TaskLabelRequest`, `TaskLabelResponse`, `TaskCommentRequest`, `TaskCommentResponse`
- [x] 5.8 Unit tests: `TaskCardServiceTest` covering obra isolation, cross-company column rejection, and `TASKS` `MANAGE` enforcement; `TaskLabelServiceTest`; `TaskCommentServiceTest` (view vs. manage)

## 6. Backend: global-tasks-board (admin-only aggregated view)

- [x] 6.1 `GlobalTaskBoardService`: `requireAdmin(companyId, userId)`, `build(companyId, userId)` fetching all sites for the company, all their cards, joining with shared columns, computing each card's obra color via `Math.floorMod(constructionSiteId.hashCode(), palette.size())` against a fixed 14-color palette constant
- [x] 6.2 `GlobalTaskBoardController`: `GET /api/companies/{companyId}/tasks-board`
- [x] 6.3 DTOs: `GlobalTaskBoardResponse`, `GlobalTaskCardResponse` (includes `constructionSiteId`, `siteName`, `siteColorHex`)
- [x] 6.4 Unit test: `GlobalTaskBoardServiceTest` covering admin-only access and color stability across two calls for the same site

## 7. Frontend: permission sync

- [x] 7.1 Fix `useSitePermissions.ts`'s `PermissionCapability` union to match the backend enum (`DOCUMENT_PROJECTS | DAILY_REPORT | EQUIPMENT | PURCHASE_REQUEST | ORCAMENTO_MANAGE | TASKS`), removing the stale retired values. Also fixed the same staleness in `SitePermissionsPanel.vue`'s hardcoded capability list and the `sitePermissions.capability.*` i18n keys in `pt-BR.json`, which were still on the pre-restructure names and would have rendered raw i18n keys.

## 8. Frontend: company task-column configuration

- [x] 8.1 `useTaskColumns.ts` composable: `listColumns`, `createColumn`, `renameColumn`, `reorderColumn`, `deleteColumn`
- [x] 8.2 `CompanyTaskColumnsPanel.vue`: list + create/rename/reorder/delete controls, rendered only when `listMyCompanies()` shows the current user's role as `ADMIN` for this company
- [x] 8.3 Mount `CompanyTaskColumnsPanel` in `CompanySettingsView.vue`
- [x] 8.4 i18n strings under `company.taskColumns.*` in `pt-BR.json`

## 9. Frontend: obra Tasks tab

- [x] 9.1 `useTaskCards.ts` composable: `getBoard`, `createCard`, `moveCard`, `listLabels`, `createLabel`, `attachLabel`, `detachLabel`, `listComments`, `addComment`
- [x] 9.2 `TasksBoardPanel.vue`: Trello-like columns/cards using native HTML5 drag-and-drop for `moveCard`, card detail modal with labels and comments. Deviated from the design.md sketch of client-side "resolved TASKS access level" gating: no existing panel in the codebase does that (`PurchaseRequestPanel`/`CompanyStaffPanel` always show controls and let the backend's 403 surface as a generic error message), so this panel follows that same simpler, already-established convention instead of introducing a new one.
- [x] 9.3 Add `'tasks'` to `SiteDetailView.vue`'s `Tab` union, add nav button, add `<TasksBoardPanel v-if="activeTab === 'tasks'" :site-id="siteId" />`
- [x] 9.4 i18n strings under `siteDetail.tabs.tasks` and `tasks.*` in `pt-BR.json`

## 10. Frontend: dashboard shortcut and global board

- [x] 10.1 `useGlobalTasksBoard.ts` composable: `getGlobalBoard(companyId)`
- [x] 10.2 `GlobalTasksBoardView.vue`: read-only aggregated board rendering each card with its obra's colored label
- [x] 10.3 Router entry `/companies/:companyId/tasks-board` → `GlobalTasksBoardView`
- [x] 10.4 Dashboard shortcut in `DashboardView.vue`, shown only when `activeCompany(status)?.role === 'ADMIN'`
- [x] 10.5 i18n strings under `dashboard.globalTasksBoardButton` and `globalTasksBoard.*` in `pt-BR.json`

## 11. Verification

- [x] 11.1 Run backend test suite (`mvn test` in `pantheon-service`) — 83/83 tests pass, no regressions
- [x] 11.2 Manually verify in the running app: admin creates columns in company settings, obra members (varying functions) see correct create/move/label/comment access on the Tasks tab, admin-only dashboard shortcut opens the global board with distinct per-obra colors. Found and fixed a real bug during this pass: `SecurityConfig`'s CORS `allowedMethods` was missing `PATCH`, so the drag-and-drop card move (`PATCH /api/task-cards/{id}/move`) failed its preflight with 403. Role-based VIEW/MANAGE gating in the UI was not manually re-tested (already covered by backend unit tests; frontend intentionally relies on the backend's 403, matching existing panel conventions).
