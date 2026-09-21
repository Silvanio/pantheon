# Pantheon Mobile

Flutter app (Android + iOS) for Pantheon, consuming the same `pantheon-service` REST API as
`pantheon-web`. See `openspec/changes/archive/*-add-flutter-mobile-app/` for the proposal and
design behind this app once archived (or `openspec/changes/add-flutter-mobile-app/` while
in-flight), and `openspec/specs/mobile-app/spec.md` / `openspec/specs/push-notifications/spec.md`
for the accepted behavior.

## Stack

- Flutter (latest stable) + Dart, Material 3
- State management: [flutter_riverpod](https://pub.dev/packages/flutter_riverpod)
- Routing: [go_router](https://pub.dev/packages/go_router)
- Networking: [dio](https://pub.dev/packages/dio)
- Secure token storage: [flutter_secure_storage](https://pub.dev/packages/flutter_secure_storage) (Keychain/EncryptedSharedPreferences — never plain `SharedPreferences`)
- Camera/gallery: [image_picker](https://pub.dev/packages/image_picker)
- Push notifications: [firebase_messaging](https://pub.dev/packages/firebase_messaging) (optional — see below)

Design tokens (`lib/theme/`) are ported 1:1 from `pantheon-web/src/style.css`'s `@theme` block
(`blueprint-*`, `steel-*`, `ink-*`, `safety-*` hex values, Manrope typeface) — keep both files in
sync if the web palette changes.

## Running locally

```bash
cd pantheon-mobile
flutter pub get
flutter run
```

The backend base URL defaults per-platform to reach a local `pantheon-service`
(`http://10.0.2.2:8081` for the Android emulator, `http://localhost:8081` for iOS
Simulator/web), matching `pantheon-web`'s dev default. Override it for a real server:

```bash
flutter run --dart-define=PANTHEON_SERVICE_URL=https://your-host:8081
```

## What's implemented

Auth (email/password only — see "Known gaps" below), Dashboard, obra navigation gated by the
caller's resolved permissions (`/api/sites/{id}/permissions/mine`), Pedido de Compra
(list/detail/approve/reject/comparison), Orçamentos (list/detail), Diário de Obra
(list/detail/photo capture via camera or gallery), Tasks board (per-column pages, touch-friendly
"mover para coluna" instead of the web's drag-and-drop), and simplified read-only screens for
Equipamentos, Projetos and Permissões (creating/uploading/editing those stays web-only for now).

## Known gaps (by design, this pass)

- **No Google OAuth login on mobile.** The web's `/oauth2/authorization/google` redirect flow
  doesn't translate directly to a mobile custom-tab/deep-link flow without extra backend
  redirect-URI work — email/password only for now.
- **No offline support.** Data is fetched fresh per screen (Riverpod `FutureProvider`s); there's
  no local cache/sync.
- **Not published to any store.** This app builds and runs locally (Android emulator/device, iOS
  Simulator) but publishing to the Play Store or App Store requires your own Play Console /
  Apple Developer accounts and is out of scope here.

## Activating push notifications

Push notifications ship **code-complete but inert** until a real Firebase project exists —
`PushNotificationService` (mobile) and `PushNotificationService` (backend, in
`pantheon-service`) both detect the missing configuration and no-op (log only), so the rest of
the app works today without any of this. To light it up end to end:

1. **Create a Firebase project** at [console.firebase.google.com](https://console.firebase.google.com)
   (requires a Google account — this is a one-time manual step only you can do).
2. **Add an Android app** to it with application id `com.pantheon.pantheon_mobile` (see
   `android/app/build.gradle.kts`), download the generated `google-services.json`, and place it
   at `pantheon-mobile/android/app/google-services.json`. Then apply the Google Services Gradle
   plugin: add `id("com.google.gms.google-services")` to `android/app/build.gradle.kts`'s
   `plugins {}` block and `classpath("com.google.gms:google-services:4.4.2")` (or current) to the
   project-level `android/build.gradle.kts`.
3. **Add an iOS app** to the same project, download `GoogleService-Info.plist`, and add it to
   `pantheon-mobile/ios/Runner/` via Xcode (drag into the `Runner` target so it's bundled).
4. **Upload an APNs authentication key** (from your Apple Developer account) to the Firebase
   project's Cloud Messaging settings — required for iOS push delivery.
5. **Generate a service-account key** for the Firebase project (Project Settings → Service
   Accounts → "Generate new private key"), save the JSON file somewhere on the backend host, and
   point `pantheon-service` at it via the `PANTHEON_FIREBASE_CREDENTIALS_PATH` environment
   variable (see `pantheon-service/src/main/resources/application.yml`).

Once all five steps are done, restart both the backend and the mobile app — no code changes are
needed, since every piece was already wired to activate on the presence of that configuration.
