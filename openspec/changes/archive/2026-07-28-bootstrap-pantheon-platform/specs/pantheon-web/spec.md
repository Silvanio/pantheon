## ADDED Requirements

### Requirement: Vue 3 application shell with routing
`pantheon-web` SHALL be a Vue 3 + TypeScript single-page application, built with Vite and using Vue Router for navigation between views.

#### Scenario: Application loads and navigates
- **WHEN** a user opens `pantheon-web` in a browser and navigates to a defined route
- **THEN** the corresponding view renders without a full page reload

### Requirement: Dark and light theme support
`pantheon-web` SHALL support both a dark mode and a light mode, styled with Tailwind CSS, following an interface design intended to be intuitive and to evoke technology and civil construction. The user's theme choice SHALL persist across sessions.

#### Scenario: User toggles theme
- **WHEN** a user switches the theme toggle from light to dark (or vice versa)
- **THEN** the interface immediately re-renders in the selected theme

#### Scenario: Theme preference persists
- **WHEN** a user who previously selected a theme reloads or revisits `pantheon-web`
- **THEN** the previously selected theme is applied automatically, without requiring the user to reselect it

### Requirement: Server-Sent Events client
`pantheon-web` SHALL connect to `pantheon-service`'s SSE endpoint and reflect received events in the UI in real time, reconnecting automatically if the connection drops.

#### Scenario: Real-time event received
- **WHEN** `pantheon-service` pushes an event over the SSE stream while a user has `pantheon-web` open
- **THEN** `pantheon-web` receives the event and updates the relevant UI state without a manual page refresh

#### Scenario: Connection drop triggers reconnect
- **WHEN** the SSE connection between `pantheon-web` and `pantheon-service` is interrupted
- **THEN** `pantheon-web` automatically attempts to reconnect rather than leaving the stream permanently closed
