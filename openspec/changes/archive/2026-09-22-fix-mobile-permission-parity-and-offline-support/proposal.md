## Why

Two gaps surfaced after `add-flutter-mobile-app` shipped: (1) the mobile app's permission/visibility rules for several screens and actions didn't match `pantheon-web`'s — some showed things a user's resolved access level shouldn't (or hid actions it should show), a real correctness bug; and (2) the app had no offline behavior at all, which doesn't fit how it's actually used on a construction site (spotty or no connectivity), specifically for Diário de Obra, Pedido de Compra, and Orçamentos.

## What Changes

- **Permission parity fixes** (mobile now matches `pantheon-web` exactly):
  - "Permissões" obra-entry card is now gated by company-admin role (`isCompanyAdmin`), like web — previously shown to every user regardless of role.
  - The purchase-request approval step shown as actionable now uses the correct current-cycle, lowest-step-order selection (mirroring `pantheon-web`'s `currentPendingApproval`) instead of picking the wrong step when more than one is `PENDING` at once.
  - The company-staff-with-no-`SiteMembership` approval bypass now requires `MANAGE` specifically (not `VIEW_AND_APPROVE`), matching the backend's `requireStepAuthority`.
  - Added the previously-missing "Enviar para aprovação", "Concluir", and "Novo pedido" actions to the mobile Pedido de Compra screens, gated by the same permission checks as web (`MANAGE`, `VIEW_AND_APPROVE`, item-selection/status preconditions).
- **Offline support**, scoped deliberately (not applied uniformly):
  - Diário de Obra (create/submit report, upload photo) and Pedido de Compra (view, create) and Orçamentos (view) now work offline: reads fall back to the last-known cached value, and writes are queued locally and sent automatically once connectivity returns.
  - A confirmation modal appears before an action would be queued offline (explains what will happen, lets the user cancel), and an acknowledgement modal appears if connectivity drops mid-action instead.
  - A floating status badge (visible on every authenticated screen) shows offline state and pending-sync count; tapping it opens a sync screen listing what's pending, with a manual "Sincronizar agora" action and the ability to discard a stuck item.
  - **Deliberately excluded** from all of the above (no caching, no queueing — these require a live connection): Projetos, the Permissões configuration screen, the Tasks board (viewing and mutating), and the Pedido de Compra approval workflow itself (submit/approve/reject/conclude) — these fail with a clear "you need to be online" message instead.

## Capabilities

### New Capabilities
- None — this extends the existing `mobile-app` capability.

### Modified Capabilities
- `mobile-app`: adds offline-support requirements (cache-fallback reads, a queued-writes outbox, and the explicit online-required exclusions) and corrects the permission/visibility requirements to match `pantheon-web` exactly.

## Impact

- **`pantheon-mobile/`**: new `lib/core/offline/` module (`OfflineDb`, `CacheStore`, `OutboxStore`, `OutboxController`), new `lib/core/widgets/offline_dialogs.dart` and `sync_status_badge.dart`, new `lib/features/sync/sync_screen.dart`, and a new `offlineCapable` flag on `ApiClient.get`. New dependencies: `sqflite`, `path`, `connectivity_plus`, `uuid`.
- Permission-parity fixes touch `SiteHomeScreen`, `PurchaseRequestDetailScreen`/`PurchaseRequestListScreen`/`PurchaseRequestRepository`.
- No backend or web changes.
