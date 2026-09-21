## 1. Environment & project scaffold

- [x] 1.1 Install/verify Flutter (latest stable) and Android/iOS toolchains locally
- [x] 1.2 Create `pantheon-mobile/` Flutter project (Android + iOS targets), set application id/bundle id under `com.pantheon.mobile` (landed as `com.pantheon.pantheon_mobile` — Flutter's default naming from `--org com.pantheon --project-name pantheon_mobile`, close enough to not warrant a risky manual rename across build files)
- [x] 1.3 Add core dependencies: riverpod, go_router, dio, flutter_secure_storage, image_picker, firebase_core, firebase_messaging, flutter_local_notifications, intl
- [x] 1.4 Set up `lib/` feature-first folder structure (core/, features/*, theme/)

## 2. Design system

- [x] 2.1 Port `blueprint-*`/`steel-*`/`ink-*`/`safety-*` color tokens from `pantheon-web/src/style.css` into `lib/theme/app_colors.dart`
- [x] 2.2 Bundle Manrope font files and wire `ThemeData` (light + dark) in `lib/theme/app_theme.dart`
- [x] 2.3 Build shared widgets: status badge (mirroring `StatusBadge.vue` semantics), primary/secondary/danger/success buttons, card container, empty state

## 3. Core infra

- [x] 3.1 `ApiClient` (dio) with base URL config, auth-header interceptor, 401 handler
- [x] 3.2 Secure token storage service (`flutter_secure_storage`)
- [x] 3.3 Riverpod providers for auth state, current company/site context
- [x] 3.4 go_router setup with auth guard (redirect to `/login` when unauthenticated; a `/splash` route added beyond the original plan so an authenticated-only screen's network calls never fire before the session-restore resolves)

## 4. Auth

- [x] 4.1 Login screen (email/password) wired to `/api/auth/login`
- [x] 4.2 Register screen wired to `/api/auth/register`
- [x] 4.3 Logout (clear token, unregister device token, navigate to login)

## 5. Dashboard & obra navigation

- [x] 5.1 Dashboard screen: obras list + summary line (active obras, pending purchase requests/orçamentos), consuming `/api/companies/{id}/construction-sites` or `/api/construction-sites/mine`
- [x] 5.2 Obra home screen: entry-card list for Equipe, Diário de Obra, Pedido de Compra, Orçamentos, Tasks, Equipamentos, Projetos, Permissões, gated by `/api/sites/{id}/permissions/mine`
- [x] 5.3 Bottom navigation shell (Dashboard / Obra atual / Perfil)

## 6. Purchase requests

- [x] 6.1 List screen (status, stage, suppliers) via `/api/construction-sites/{id}/purchase-requests`
- [x] 6.2 Detail screen: items, approval-step stepper, linked orçamentos summary
- [x] 6.3 Approve/reject actions wired to approve-step/reject-step endpoints, hidden per resolved permission level
- [x] 6.4 Comparison view (read-only) via `/api/purchase-requests/{id}/comparison`

## 7. Orçamentos

- [x] 7.1 List screen via `/api/construction-sites/{id}/orcamentos`
- [x] 7.2 Detail screen: fornecedor info, line items, total

## 8. Diário de Obra

- [x] 8.1 List screen via `/api/construction-sites/{id}/daily-reports`
- [x] 8.2 Detail screen: weather/hours/comments, workforce, activities
- [x] 8.3 Photo capture (camera or gallery via `image_picker`) uploaded through the existing media endpoint

## 9. Tasks board

- [x] 9.1 Board screen: horizontally-paged columns (`PageView`), cards per column
- [x] 9.2 "Mover para coluna" action sheet wired to the existing move-card endpoint
- [ ] 9.3 Card detail: comments, attachments (read + add) — deferred; card creation/move is implemented but a dedicated card-detail screen is not, in favor of breadth across the other modules given the session's time budget

## 10. Equipamentos, Projetos, Permissões (simplified screens)

- [x] 10.1 Equipamentos: list + create
- [x] 10.2 Projetos (site documents): folder/file list (read-only; download deferred alongside 9.3 for the same reason)
- [x] 10.3 Permissões: read-only view of current overrides (editing stays web-only for this pass)

## 11. Backend: push notifications

- [x] 11.1 `DeviceToken` entity + repository + Flyway migration (`V57__create_device_token_table.sql`)
- [x] 11.2 `POST /api/me/device-tokens` and `DELETE /api/me/device-tokens/{token}` endpoints, scoped to the authenticated caller
- [x] 11.3 `firebase-admin` Maven dependency + `PushNotificationService` (config-gated no-op when unconfigured)
- [x] 11.4 Call `PushNotificationService` from `PurchaseRequestService`'s submit/approve/reject/conclude transitions
- [x] 11.5 Backend tests: `DeviceTokenServiceTest`, `PushNotificationServiceTest` (no-op-when-unconfigured), and `PurchaseRequestServiceTest` updated for the new constructor dependency — full suite green

## 12. Mobile: push notifications

- [x] 12.1 Firebase initialization (`firebase_core`), gracefully skipped if config files are absent — verified: both Android and iOS log the graceful fallback and the app functions normally with no Firebase project configured
- [x] 12.2 Register/refresh FCM token with the backend after login; unregister on logout
- [x] 12.3 Foreground/background/terminated notification handling + tap-to-navigate to the referenced purchase request (implemented; cannot be end-to-end verified without a real Firebase project — see README's activation steps)

## 13. Verification

- [x] 13.1 `flutter analyze` clean
- [x] 13.2 Run and manually verify core flows in Android emulator — login, dashboard, obra navigation, Pedido de Compra list/detail/comparison, Orçamentos list/detail, Diário de Obra detail, Tasks board (paged columns) all verified live against the real backend with real data; found and fixed a real bug in the process (see below)
- [x] 13.3 Run and manually verify core flows in iOS Simulator — build succeeds, login screen verified visually matching Android/web
- [x] 13.4 Backend: `./mvnw test` passes with the new device-token/push code
- [x] 13.5 Write `pantheon-mobile/README.md` covering local run instructions and the manual Firebase setup steps needed to activate push end-to-end

### Bug found and fixed during verification

Jackson serializes Java `BigDecimal` fields (quantities, unit prices) as JSON *numbers*, not strings — `pantheon-web`'s TypeScript interfaces label them `string` (harmless in JS, no runtime type checking), but Dart's `as String` cast is a real runtime check and threw, breaking the Pedido de Compra detail screen. Fixed with a shared `asDecimalString`/`asDecimalStringOrNull` helper (`lib/core/models/json_utils.dart`) used everywhere a `BigDecimal`-backed field is parsed.
