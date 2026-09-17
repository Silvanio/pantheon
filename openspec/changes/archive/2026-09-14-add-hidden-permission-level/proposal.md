## Why

Today a site member's access to each capability (documentos, diário de obra, equipamentos, pedidos de compra, orçamentos, tasks, equipe) is either `VIEW` or `MANAGE` — there's no way to keep a member off a capability entirely. The user wants a third choice: hide the menu/tab altogether, which must also mean the member genuinely cannot read that data (not just that the tab is missing from the UI) — confirmed nothing today gates *reads* by the resolved access level, only writes, so this is a real enforcement gap to close, not just a UI toggle.

## What Changes

- `AccessLevel` gains a third value, `HIDDEN`, settable the same way `VIEW`/`MANAGE` already are (function-level or member-level override), with company staff exempt (always `MANAGE`, same as today).
- Every capability's read path (list/get and their sub-resource reads) now rejects a member whose resolved level for that capability is `HIDDEN`, matching whatever capability its own write path already checks — previously reads had **no** permission check at all, only basic site membership.
- A new "my resolved permissions" endpoint lets `pantheon-web` know, per capability, what the current user can do on this site — nothing like this existed before (the frontend had zero visibility into resolved access levels).
- `SiteDetailView.vue`'s tab bar hides a tab entirely when its capability resolves to `HIDDEN` for the current user, and the default active tab becomes the first visible one instead of a hardcoded `'team'`.
- `SitePermissionsPanel.vue` gains the third `HIDDEN` choice in its dropdown, and also gains `TEAM_MANAGE` — present in the backend enum and enforced in code, but missing from this configuration UI today (a pre-existing gap; closing it here since "Equipe" is one of the tabs this feature is meant to be able to hide).

## Capabilities

### New Capabilities
(none — extends `obra-permission-management`)

### Modified Capabilities
- `obra-permission-management`: `AccessLevel` gains `HIDDEN`; every read-path requirement gains a "hidden member cannot even view" scenario alongside its existing "view-only member can still view" scenario; the permission-configuration view requirement gains the `HIDDEN` choice and the missing `TEAM_MANAGE` row; a new requirement covers the tab bar hiding a capability's menu and the "my resolved permissions" endpoint it relies on.

## Impact

- Affected code: `pantheon-service` — `AccessLevel` enum, `SitePermissionService` (`requireVisible`, `resolveAll`), read methods across `SiteDocumentProjectService`, `DailyReportService`, `EquipmentService`, `MaterialService`, `PurchaseRequestService`, `OrcamentoService`, `TaskCardService`, `SiteMembershipService`, new `GET /api/sites/{siteId}/permissions/mine` on `SitePermissionController`.
- Affected code: `pantheon-web` — `useSitePermissions.ts` (new `AccessLevel` value, new `TEAM_MANAGE`, new `getMyPermissions` call), `SitePermissionsPanel.vue` (third option, `TEAM_MANAGE` row), `SiteDetailView.vue` (tab visibility + default-tab selection).
- No schema/migration changes — `AccessLevel` is stored as a string enum column, and `HIDDEN` is just a new valid value.
- Company staff (admins) are never affected — `HIDDEN` only ever applies to non-staff site members, exactly like `VIEW`/`MANAGE` overrides already do.
