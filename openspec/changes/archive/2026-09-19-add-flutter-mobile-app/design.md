## Context

`pantheon-service` already exposes a complete, stable REST API (JWT bearer auth) that `pantheon-web` consumes. The mobile app is a second, independent client of that same API — it does not need a new backend architecture, only a mobile-appropriate presentation layer and one new cross-cutting capability (push notifications) that the web app has no equivalent of. `AGENTS.md`'s `obra-visual-design` tokens (colors, type, status semantics) are the source of truth for brand consistency between web and mobile.

## Goals / Non-Goals

**Goals:**
- Ship a working Flutter app (Android + iOS) covering: auth, dashboard, obra detail navigation, purchase requests (list/detail/approve/reject), orçamentos (list/detail), diário de obra (list/detail/photo capture), tasks board, equipamentos, projetos, permissões — against the real backend, using the same color/type tokens as the web app.
- Add push notifications for purchase-request approval events, end to end (backend send + mobile receive/deep-link).
- Keep `pantheon-service`'s existing endpoints and response shapes untouched; only add new endpoints/entities.

**Non-Goals:**
- Google OAuth login on mobile (the web's `/oauth2/authorization/google` redirect flow doesn't translate directly to a mobile custom-tab/deep-link flow without extra backend redirect-URI work) — email/password login only for this change.
- Offline-first data sync / local caching beyond in-memory (Riverpod) state for the current session.
- Publishing to the App Store / Play Store — that requires the user's own Apple Developer and Play Console accounts and is out of scope for this change; the deliverable is a working, locally-buildable app (debug builds verified in Android emulator and iOS Simulator).
- SINAPI integration (discussed separately, explicitly deferred).
- A remote-config/feature-flag panel in `pantheon-web`'s company settings for the app — explicitly declined for this change.

## Decisions

- **State management: Riverpod.** Chosen over Bloc/Provider for less boilerplate per screen given the number of screens needed, while still keeping business logic out of widgets (notifiers hold state, widgets only read/watch). Alternative considered: Bloc — more ceremony per feature, not justified at this breadth.
- **Routing: go_router.** Declarative, deep-link friendly (needed for push-notification tap-to-navigate), and the de facto standard for Flutter apps needing named routes + guards (redirect unauthenticated users to `/login`).
- **HTTP: dio + a single `ApiClient`.** Mirrors `pantheon-web`'s pattern of one `authFetch` helper reused by every composable — one dio instance with an auth interceptor (attaches the bearer token, and on a 401 clears the session and redirects to login) reused by every feature's repository class.
- **Token storage: `flutter_secure_storage`.** Equivalent of the web's `localStorage` token, but using the platform keychain/keystore instead, since a mobile app shouldn't keep a bearer token in plain SharedPreferences.
- **Navigation shell: bottom navigation bar (Dashboard / Obra atual / Perfil) instead of the web's persistent sidebar.** A left icon rail doesn't fit a phone width; a bottom bar is the standard mobile pattern. Inside an obra, the section list (Equipe, Diário de Obra, Pedido de Compra, Orçamentos, Tasks, Equipamentos, Projetos, Permissões) becomes a scrollable grid/list of entry cards on an "obra home" screen rather than a persistent side list, since there's no screen real estate for a always-visible secondary sidebar on mobile.
- **Tasks board on mobile: per-column pages (`PageView`), not free drag-and-drop across columns.** Cross-column drag-and-drop is fussy on touch at phone width; a horizontally-paged column view with a "mover para coluna" action sheet on each card is the same underlying capability (`moveCard`) with a UX suited to touch, matching common mobile Kanban clients (e.g., Trello's mobile app).
- **Design tokens ported as Dart constants (`AppColors`, `AppTypography`), not re-derived at runtime.** The web's tokens live in `style.css` as CSS custom properties; the mobile equivalent is a small `lib/theme/` module with the same hex values and a `ThemeData` built from them (light + dark, mirroring the web's light/dark toggle), keeping both platforms visually in lockstep without sharing a build toolchain.
- **Push notifications: Firebase Cloud Messaging (FCM) for both platforms, via `firebase_messaging` + `firebase-admin` (Java) on the backend**, rather than a custom notification server. FCM is the standard cross-platform choice (single backend integration reaches both Android natively and iOS via APNs-through-FCM), avoiding a separate APNs-only code path.
  - New backend entity `DeviceToken` (`userId`, `platform`, `token`, `createdAt`) with a register endpoint (`POST /api/me/device-tokens`) and unregister (`DELETE /api/me/device-tokens/{token}`), scoped to the authenticated caller.
  - New `PushNotificationService`, called from `PurchaseRequestService` at the same points that already mutate approval state (`submitForApproval`, `approveStep`, `rejectStep`, `conclude`) — it resolves the target user(s) (the site members whose `ConstructionFunction` matches the newly-pending step's `approverFunction`, or the request's creator on reject/conclude) and sends via the Firebase Admin SDK. If Firebase isn't configured (no credentials path set), the service logs and no-ops rather than throwing, so existing dev/CI environments are unaffected.
  - The mobile app registers its FCM token after login and on token refresh; tapping a notification deep-links to `/purchase-requests/:id` via go_router.

## Risks / Trade-offs

- [Firebase project doesn't exist yet — pushes silently no-op until it's created] → `PushNotificationService` is written defensively (config-gated, no-op + log when unconfigured) specifically so the rest of the app ships and works today; the proposal's Impact section documents the one-time manual setup (Firebase project, `google-services.json`/`GoogleService-Info.plist`, APNs key) the user must do afterward to light up push end-to-end.
- [No Google OAuth on mobile in this pass] → users who only ever signed up via Google on web can't log into the mobile app yet with that flow; documented as a known gap, not silently dropped.
- [iOS push notifications need an Apple Developer Program membership + APNs key uploaded to Firebase] → same as above, called out explicitly rather than assumed done.
- [Mobile Kanban UX differs from web's drag-across-columns] → intentional trade-off for touch usability, not a bug; both drive the same `moveCard` API.
- [App Store/Play Store signing requires the user's own accounts] → this change's deliverable stops at a locally-buildable, simulator/emulator-verified app; store submission is explicitly out of scope.

## Migration Plan

- Additive only — no existing table, endpoint, or web behavior changes. `DeviceToken` is a new table (new migration), and the new push-trigger calls in `PurchaseRequestService` are no-ops when Firebase is unconfigured, so this can ship without any coordinated rollout step. Rollback is deleting the new mobile project directory and reverting the backend's new file/migration if ever needed — no data migration to undo.
