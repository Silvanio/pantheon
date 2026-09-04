# pantheon-web Specification

## Purpose

Vue 3 single-page application for the Pantheon platform. Provides the app shell and routing, dark/light theming with an interface evoking technology and civil construction, and an SSE client that subscribes to `pantheon-service`'s real-time event stream.

## Requirements

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

### Requirement: Post-login onboarding popup
`pantheon-web` SHALL, immediately after a successful login when the user has no active project, present a popup asking whether the user wants to create a new project or join an existing one, before allowing navigation to the dashboard.

#### Scenario: First access shows onboarding popup
- **WHEN** a user with no project membership completes login
- **THEN** `pantheon-web` displays a popup offering the choices "Create a new project" and "Join a project" instead of navigating to the dashboard

#### Scenario: Existing active project skips onboarding popup
- **WHEN** a user with at least one active (non-expired) project membership completes login
- **THEN** `pantheon-web` navigates directly toward the dashboard without displaying the onboarding popup

### Requirement: No-project message on join
`pantheon-web` SHALL show a message informing the user they have no project yet when they choose to join a project while having no project membership.

#### Scenario: User chooses to join with no projects
- **WHEN** a user with no project membership selects "Join a project" from the onboarding popup
- **THEN** `pantheon-web` displays a message stating the user does not yet have a project and must be added by an administrator

### Requirement: Project registration view
`pantheon-web` SHALL provide a form for creating a project, collecting the project name along with the user's CNPJ/CPF, Nome/Razão Social, Endereço, and CEP, and submitting them to `pantheon-service` to create the project and record the user's profile data.

#### Scenario: User creates a project
- **WHEN** a user selects "Create a new project" from the onboarding popup and submits the registration form with valid data
- **THEN** `pantheon-web` submits the data to `pantheon-service`, and upon success navigates the user to the dashboard

### Requirement: Plan selection view
`pantheon-web` SHALL, when the current user's projects require plan selection (trial or confirmed plan expired), present a screen offering the Basic, Pro, and Ilimitado plans before allowing navigation to the dashboard. Selecting a plan and confirming SHALL submit the choice to `pantheon-service` and then proceed to the dashboard.

#### Scenario: Expired trial prompts plan selection
- **WHEN** a user whose project's trial period has expired with no plan confirmed logs in
- **THEN** `pantheon-web` displays the plan selection screen instead of navigating to the dashboard

#### Scenario: User confirms a plan
- **WHEN** a user on the plan selection screen picks a plan and clicks "Concluído"
- **THEN** `pantheon-web` submits the chosen plan to `pantheon-service` and, on success, navigates to the dashboard

### Requirement: Post-login routing guard
`pantheon-web` SHALL gate access to the dashboard and other protected routes behind a check of the authenticated user's onboarding status, redirecting to the onboarding popup, the no-project message, the project registration view, or the plan selection view as appropriate.

#### Scenario: Protected route blocked pending onboarding
- **WHEN** an authenticated user whose onboarding status is not clean (no active project) attempts to navigate to a protected route
- **THEN** `pantheon-web` redirects them to the appropriate onboarding step instead of rendering the protected route

### Requirement: Internationalized UI text
`pantheon-web` SHALL source all user-facing text (labels, buttons, messages, validation errors) from i18n locale resource files rather than hardcoding it in components, with `pt-BR` as the default and, for now, only shipped locale.

#### Scenario: New text added through the locale file
- **WHEN** a new user-facing string is introduced by any view or component
- **THEN** it is added as a key in the `pt-BR` locale resource file and referenced through the i18n mechanism, not inlined as a literal in the template or script

#### Scenario: Application renders in Portuguese by default
- **WHEN** a user opens `pantheon-web` without an explicit locale override
- **THEN** every user-facing string renders from the `pt-BR` locale resource file
