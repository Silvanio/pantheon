# mobile-app Specification

## Purpose
TBD - created by archiving change add-flutter-mobile-app. Update Purpose after archive.
## Requirements
### Requirement: Mobile authentication
The mobile app SHALL allow a user to log in with email and password against the existing `pantheon-service` auth API, store the resulting JWT in secure platform storage (not plain preferences), and attach it as a bearer token to every authenticated request. The app SHALL redirect an unauthenticated user (no stored token, or a token rejected with 401) to the login screen.

#### Scenario: Successful login
- **WHEN** a user submits valid email and password on the login screen
- **THEN** the app stores the returned JWT in secure storage and navigates to the dashboard

#### Scenario: Session expiry
- **WHEN** any authenticated API call returns 401
- **THEN** the app clears the stored token and navigates to the login screen

### Requirement: Mobile navigation shell matches platform conventions
The mobile app SHALL use a bottom navigation bar as its top-level navigation (not the web's persistent sidebar), and SHALL present an obra's sections (Equipe, Diário de Obra, Pedido de Compra, Orçamentos, Tasks, Equipamentos, Projetos, Permissões) as an entry-card list on an obra home screen rather than an always-visible side list.

#### Scenario: Switching top-level sections
- **WHEN** a logged-in user taps a bottom navigation item
- **THEN** the app navigates to that section without leaving the authenticated shell

### Requirement: Visual consistency with the web app
The mobile app SHALL use the same brand color tokens (`blueprint-*`, `steel-*`, `ink-*`, `safety-*` hex values from `pantheon-web`'s `style.css`), the Manrope typeface, and the same status-color semantics (emerald=success/approved, amber=pending, coral/safety=danger/rejected) as `pantheon-web`, adapted to Flutter's `ThemeData`, including a light and dark mode matching the web app's theme toggle.

#### Scenario: Status badge color parity
- **WHEN** the mobile app renders a purchase-request or orçamento status badge
- **THEN** it uses the same color mapping as `pantheon-web`'s `StatusBadge.vue` for that status

### Requirement: Dashboard shows the user's obras and summary
The mobile app SHALL show, on its dashboard, the list of obras the user has access to (scoped by active company, mirroring `pantheon-web`'s dashboard) and a summary line of active obras count and pending purchase-request/orçamento counts.

#### Scenario: Dashboard loads obras
- **WHEN** a logged-in user opens the dashboard
- **THEN** the app lists their obras with name, status, and address

### Requirement: Purchase-request list, detail, and approval actions
The mobile app SHALL list an obra's purchase requests with status and stage, show a request's detail (items, approval steps, linked orçamentos), and allow a user with sufficient approval authority to approve or reject the current pending step, mirroring `pantheon-web`'s existing authorization rules (the backend enforces authority; the app SHALL only hide actions the current user's resolved permissions do not allow, not attempt its own authorization logic).

#### Scenario: Approving a pending step
- **WHEN** a user with matching approval authority taps "Aprovar" on a purchase request's pending step
- **THEN** the app calls the existing approve-step endpoint and refreshes the request's status

### Requirement: Orçamento list and detail
The mobile app SHALL list an obra's orçamentos and show an orçamento's detail (fornecedor info, line items, totals), read-only, matching the data the web app's `OrcamentoDetailView` shows.

#### Scenario: Viewing an orçamento
- **WHEN** a user opens an orçamento from the list
- **THEN** the app shows its fornecedor, line items, and total

### Requirement: Diário de Obra list, detail, and photo capture
The mobile app SHALL list an obra's daily reports, show a report's detail (weather, hours, comments, workforce, activities), and allow attaching a photo taken with the device camera or chosen from the gallery to a draft report, using the existing media-upload endpoint.

#### Scenario: Attaching a camera photo to a draft report
- **WHEN** a user on a draft daily report taps "Adicionar foto" and takes a photo with the device camera
- **THEN** the app uploads it via the existing daily-report media endpoint and shows it in the report's media list

### Requirement: Tasks board adapted for touch
The mobile app SHALL show an obra's task board as horizontally-paged columns (one column per page) instead of the web's side-by-side drag-and-drop layout, and SHALL allow moving a card to a different column via an explicit "mover para coluna" action using the existing move-card endpoint.

#### Scenario: Moving a card between columns on mobile
- **WHEN** a user picks "Mover para coluna" on a task card and selects a target column
- **THEN** the app calls the existing move-card endpoint and the card appears in the target column's page

