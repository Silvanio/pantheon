## Why

Pantheon is currently web-only. Site staff, engineers, and clients need to check obra status, approve purchase requests, and fill daily reports from the field, on a phone — a desktop-oriented web UI accessed through a mobile browser doesn't fit that workflow. A native Flutter app (Android + iOS) reusing the existing REST API and the same visual design system closes this gap, and push notifications for purchase-request approvals make the approval workflow actually usable on the go instead of requiring the approver to remember to check the web app.

## What Changes

- New Flutter mobile app (`pantheon-mobile/`), Android + iOS, consuming the existing `pantheon-service` REST API — no breaking changes to that API.
- Mobile-native navigation (bottom nav + drawer, not the web's sidebar), matching mobile platform UX conventions, while reusing the same brand colors (`blueprint-*`, `steel-*`, `ink-*`, `safety-*`), Manrope typeface, and status semantics as `pantheon-web`.
- Screens: login/register, dashboard (obras list + summary), obra detail with tabs (Equipe, Diário de Obra, Pedido de Compra, Orçamentos, Tasks board, Equipamentos, Projetos, Permissões), purchase-request detail with approve/reject actions and comparison view, orçamento detail, daily-report detail with native camera photo capture, and a Kanban-style tasks board adapted for touch (per-column pages instead of desktop drag-across-columns).
- **New**: push notifications for purchase-request approval-step events (a step becomes pending for your function, your step is approved/rejected, the request is concluded) — requires `pantheon-service` to send messages via Firebase Cloud Messaging when these transitions happen, and a new endpoint for the mobile app to register/unregister its device token.
- No changes to existing web-facing behavior; `pantheon-web` is untouched by this change.

## Capabilities

### New Capabilities
- `mobile-app`: the Flutter mobile client — auth, navigation shell, and the obra-management screens (dashboard, obra detail, purchase requests, orçamentos, daily reports, tasks board, equipamentos, projetos, permissões), consuming the existing backend API and following the same visual design tokens as `pantheon-web`.
- `push-notifications`: device-token registration and push delivery, cross-cutting the backend (`pantheon-service`, sending) and the mobile app (registering, receiving, deep-linking on tap).

### Modified Capabilities
- `purchase-request-approval-workflow`: adds a requirement that submitting for approval, approving a step, rejecting a step, and concluding a purchase request each trigger a push notification (via the new `push-notifications` capability) to the user(s) who can act on or are affected by the resulting state, in addition to the existing in-app/API behavior.

## Impact

- **New**: `pantheon-mobile/` — a full Flutter project (Android + iOS targets), with its own README for local run instructions.
- **`pantheon-service`**: new `DeviceToken` entity/repository/controller (register/unregister endpoint under the caller's own user), a `PushNotificationService` wrapping the Firebase Admin SDK (new `firebase-admin` Maven dependency), and calls into it from `PurchaseRequestService` at the existing submit/approve/reject/conclude transition points. Firebase credentials are supplied via environment/config and the service no-ops (logs only) when unconfigured, so existing dev/test environments without Firebase credentials are unaffected.
- **No changes** to `pantheon-web` or to any existing REST endpoint's request/response shape.
