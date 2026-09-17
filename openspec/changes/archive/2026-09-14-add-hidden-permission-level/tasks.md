## 1. `AccessLevel` and resolution

- [x] 1.1 Add `HIDDEN` to `AccessLevel` enum.
- [x] 1.2 `SitePermissionService`: add `requireVisible(siteId, access, capability)` (throws `ForbiddenCapabilityException` when resolved level is `HIDDEN`) and `resolveAll(siteId, access)` (returns `Map<PermissionCapability, AccessLevel>` for all 7 capabilities).

## 2. Read-path gating (same capability as each service's own `requireManage`)

- [x] 2.1 `SiteDocumentProjectService`: `list`, `get`, `listAttachments`, `getAttachmentContent` → `requireVisible(..., DOCUMENT_PROJECTS)`.
- [x] 2.2 `DailyReportService`: `list`, `getDetail`, `listWorkforceEntries`, `listEquipmentUsage`, `listActivities`, `listOccurrences`, `listMaterialsReceived` → `requireVisible(..., DAILY_REPORT)`.
- [x] 2.3 `EquipmentService.list` → `requireVisible(..., EQUIPMENT)`.
- [x] 2.4 `MaterialService`: `list`, `getPhotoContent` → `requireVisible(..., ORCAMENTO_MANAGE)` (matches its own `requireManage`).
- [x] 2.5 `PurchaseRequestService`: `list`, `get` → `requireVisible(..., PURCHASE_REQUEST)`.
- [x] 2.6 `OrcamentoService`: `list`, `get` → `requireVisible(..., ORCAMENTO_MANAGE)` (covers `listLineItems`/`listApprovals` transitively — controller only calls them after `get` succeeds).
- [x] 2.7 `TaskCardService.getBoard` → `requireVisible(..., TASKS)`.
- [x] 2.8 `SiteMembershipService.listMembers` → `requireVisible(..., TEAM_MANAGE)`.

## 3. "My resolved permissions" endpoint

- [x] 3.1 Add `GET /api/sites/{siteId}/permissions/mine` to `SitePermissionController`, open to any active site member (not staff-only), returning `resolveAll(...)`.

## 4. Frontend: permission config UI

- [x] 4.1 `useSitePermissions.ts`: add `'HIDDEN'` to the `AccessLevel` type, add `'TEAM_MANAGE'` to `PermissionCapability`, add `getMyPermissions(siteId)`.
- [x] 4.2 `SitePermissionsPanel.vue`: add `HIDDEN` as a third `<option>`; add `TEAM_MANAGE` to the `capabilities` array.

## 5. Frontend: tab visibility

- [x] 5.1 `SiteDetailView.vue`: fetch `getMyPermissions(siteId)` alongside site load; map each tab (`team, dailyReport, projects, equipment, purchaseRequests, orcamentos, tasks`) to its capability; compute `visibleTabs`; `v-if` each nav button and panel on visibility (default to visible until permissions have loaded, so nothing flashes hidden); set the initial `activeTab` to the first visible tab instead of the hardcoded `'team'`.
- [x] 5.2 `pt-BR.json`: add the `HIDDEN` access-level label and the `TEAM_MANAGE` capability label for the permissions panel.

## 6. Verification

- [x] 6.1 Backend: unit tests for `SitePermissionService.requireVisible`/`resolveAll`, and for at least one representative read-gating addition per service touched in step 2 (hidden rejected, view/manage still allowed). Run the full suite.
- [x] 6.2 Frontend: `npm run build` green.
- [ ] 6.3 Manual check left for the user: setting a function to `HIDDEN` removes that tab for members of that function and blocks the API directly; company staff still see everything; `VIEW`/`MANAGE` behavior is unchanged. **Not run in this session** — the assistant could not log in to verify live.
