# obra-visual-design Specification

## Purpose
Defines Pantheon's shared visual design system for `pantheon-web` — color tokens, typography, and the persistent sidebar navigation shell — and how generated PDFs stay visually consistent with it. Introduced by the 2026 redesign (see the archived `redesign-app-visual-language` change) as the reference every screen and PDF should follow going forward.
## Requirements
### Requirement: Shared design tokens
`pantheon-web` SHALL define its color palette as named Tailwind `@theme` tokens in `style.css` — `blueprint-*` (brand blue, primary actions and links), `steel-*` (neutrals, backgrounds and text, flips with light/dark theme), `ink-*` (the global sidebar's own scale, which SHALL stay dark regardless of the active theme), and `safety-*` (coral, danger/destructive actions) — rather than one-off hex values in component markup, so a future palette change is a token edit, not a per-component search-and-replace. `pantheon-web` SHALL NOT define a custom color scale under a name that collides with one of Tailwind's built-in palette names (e.g. `amber`, `emerald`); status semantics SHALL reuse Tailwind's built-in scales instead.

#### Scenario: Token value change cascades without touching components
- **WHEN** a `blueprint-*` token's hex value is changed in `style.css`
- **THEN** every component using a `blueprint-*` Tailwind class (e.g. `bg-blueprint-600`, `text-blueprint-700`) reflects the new color without any edit to that component's file

#### Scenario: No custom scale collides with a Tailwind built-in name
- **WHEN** a developer wants to add a new status color
- **THEN** they extend `StatusBadge.vue`'s class map using one of Tailwind's existing built-in scales (e.g. `emerald-*`, `amber-*`), not a new custom `@theme` token sharing that scale's name

### Requirement: Persistent global sidebar
`pantheon-web` SHALL show a persistent, icon-only, always-dark (`ink-*`) sidebar (`AppSidebar.vue`) on every authenticated screen that renders `AppHeader`, providing at minimum a link back to the dashboard. Onboarding-wizard screens (company creation, plan selection, company profile) are exempt and SHALL remain chrome-free.

#### Scenario: Sidebar present on an authenticated screen
- **WHEN** an authenticated user views any screen that renders `AppHeader` (dashboard, an obra, a Pedido de Compra, an Orçamento, a daily report, company settings, the global tasks board, or the user's profile)
- **THEN** `pantheon-web` shows `AppSidebar` to the left of that screen's content, and clicking its home/logo icon navigates to the dashboard

### Requirement: Obra section navigation lives in a sidebar, not a tab bar
`pantheon-web` SHALL show an obra's sections (Equipe, Diário de Obra, Projetos, Equipamentos, Pedido de Compra, Orçamentos, Tasks, Cronograma, and — for company admins — Permissões) as a vertical, labeled, icon-prefixed navigation list in a dedicated sidebar on `SiteDetailView`, positioned between the global sidebar and the obra's content, showing the obra's photo, name, and status. On viewports too narrow to show that sidebar, `pantheon-web` SHALL fall back to a compact horizontal tab bar offering the same sections. A section's visibility rule (hidden when the viewer's resolved capability access is `HIDDEN`; Permissões shown only to company admins) SHALL be identical between the sidebar and its narrow-viewport fallback.

#### Scenario: Obra sections listed in the sidebar
- **WHEN** a construction site member with full access opens an obra
- **THEN** `pantheon-web` shows all of Equipe, Diário de Obra, Projetos, Equipamentos, Pedido de Compra, Orçamentos, and Tasks as items in the obra sidebar, each switching that section's content on click, with the currently active section visually highlighted

#### Scenario: Hidden capability excluded from both the sidebar and its fallback
- **WHEN** a construction site member's resolved access to a capability (e.g. `EQUIPMENT`) is `HIDDEN`
- **THEN** `pantheon-web` excludes that section from both the obra sidebar and the narrow-viewport fallback tab bar

### Requirement: Construction site status is visible on the dashboard
`pantheon-web` SHALL show each obra's `status` (`PLANNING`, `IN_PROGRESS`, `PAUSED`, or `COMPLETED`) as a color-coded status badge on its dashboard card, using the same `StatusBadge` component and color convention used for Pedido de Compra, Orçamento, and approval statuses elsewhere in the app.

#### Scenario: Dashboard card shows the obra's real status
- **WHEN** a user views the dashboard with at least one obra whose `status` is `IN_PROGRESS`
- **THEN** `pantheon-web` shows a status badge reading that obra's actual status on its card, not a fabricated or placeholder value

### Requirement: Generated PDFs share the screen's color system
`pantheon-service` SHALL render every generated PDF (Pedido de Compra per-supplier, Pedido de Compra summary, daily report) using `PdfBrandingService.STYLE`'s color values kept in sync with `pantheon-web`'s `blueprint-*`/`steel-*` token values, so a PDF and the screen that generated it are visually consistent. PDF body text SHALL continue to use PDF-safe standard fonts (Helvetica/Arial family) rather than the screen's webfont, prioritizing rendering reliability over exact typeface parity.

#### Scenario: PDF header accent matches the app's brand blue
- **WHEN** any PDF is generated
- **THEN** its header rule, section labels, and table header fill use the same `blueprint-*` hex values currently defined in `pantheon-web`'s `style.css`, not the pre-redesign palette

#### Scenario: PDF generation does not depend on webfont embedding
- **WHEN** a PDF is generated for a company whose obra names or content include characters outside a narrow font's coverage
- **THEN** `pantheon-service` still renders successfully, because PDF text uses a standard, always-available font family rather than an embedded webfont

### Requirement: Standard checkbox appearance
`pantheon-web` SHALL style every native `<input type="checkbox">` with the shared `.field-checkbox` component class (bordered, rounded, `accent-color` set to the brand blueprint color in both light and dark mode, with a focus ring matching `.field-input`), rather than the browser's unstyled default appearance.

#### Scenario: Checkbox uses the shared style in both themes
- **WHEN** a member views any checkbox in the app (e.g. Pedido de Compra item selection, Diário de Obra's weather-blocked-tasks toggle, a Cronograma task's done toggle) in either light or dark mode
- **THEN** `pantheon-web` renders it with the `.field-checkbox` class's bordered, rounded, brand-colored appearance instead of the browser's default checkbox

