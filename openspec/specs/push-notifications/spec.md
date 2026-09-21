# push-notifications Specification

## Purpose
TBD - created by archiving change add-flutter-mobile-app. Update Purpose after archive.
## Requirements
### Requirement: Device token registration
The backend SHALL let an authenticated user register their mobile device's push token (platform + token) under their own user id, and unregister a specific token, so pushes can later be addressed to that user across their devices.

#### Scenario: Registering a token after login
- **WHEN** the mobile app logs a user in and obtains an FCM token
- **THEN** it calls the register-device-token endpoint with that token and platform, associated with the authenticated user

#### Scenario: Unregistering on logout
- **WHEN** a user logs out of the mobile app
- **THEN** the app calls the unregister endpoint for its current device token

### Requirement: Push delivery is best-effort and configuration-gated
The backend's push-sending service SHALL no-op (log only, no exception) when push credentials are not configured, so environments without a Firebase project keep working unaffected. When configured, a delivery failure for one device token SHALL NOT prevent delivery to the user's other registered tokens.

#### Scenario: Unconfigured environment
- **WHEN** a code path that would send a push notification runs in an environment with no Firebase credentials configured
- **THEN** the backend logs that push is unconfigured and completes the triggering operation normally

#### Scenario: One stale token among several
- **WHEN** a user has two registered device tokens and one is no longer valid
- **THEN** the backend still attempts delivery to the other valid token

### Requirement: Notification tap navigates to the relevant screen
The mobile app SHALL, when a user taps a purchase-request-related push notification, navigate directly to that purchase request's detail screen (foreground, background, or app-closed launch).

#### Scenario: Tapping a notification while the app is backgrounded
- **WHEN** a user taps a push notification referencing a purchase request while the app is in the background
- **THEN** the app foregrounds and navigates to that purchase request's detail screen

