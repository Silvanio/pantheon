## Why

Two header/layout requests came up while using the app: the obra detail page's cover photo takes up a lot of vertical space with no way to collapse it, and it had no dark-mode toggle at all; separately, the dashboard header showed the generic "Pantheon" brand instead of the user's own company name, and its branding/controls felt misaligned. Both are UI-only polish, no backend or data-model changes.

## What Changes

- Obra detail page (`SiteDetailView.vue`): the cover photo can be collapsed (hidden) and expanded again via a toggle button; when collapsed, the obra's name relocates into the page header next to the back button, with an expand button on the header's right. The collapsed/expanded choice is a single global preference (not per-obra), persisted in `localStorage` (mirrors how the light/dark theme choice already persists), so it carries over to every obra visited afterward.
- Obra detail page also gains the dark/light theme toggle in its header (previously only present on the dashboard).
- Dashboard header (`DashboardView.vue`): shows the active company's name instead of the hardcoded "Pantheon" brand text (the generic brand mark remains as a fallback before a company is active, e.g. during onboarding); the logo+name sit flush against the header's left edge; the theme toggle moves to be the rightmost header control, after the profile menu.

## Capabilities

### New Capabilities
(none)

### Modified Capabilities
- `construction-site-management`: adds a requirement for the obra detail page's collapsible cover photo/header and the theme toggle's presence there.
- `company-onboarding`: adds a requirement for the dashboard header showing the active company's own branding (name) instead of the generic app brand, and the placement of its controls.

## Impact

- Affected code: `pantheon-web` only — `views/SiteDetailView.vue`, `views/DashboardView.vue`, new `composables/useSiteHeroCollapse.ts`, `locales/pt-BR.json`.
- No backend changes, no API changes, no database changes.
- Already implemented and verified via `npm run build` (typecheck + build); live visual verification in a logged-in session is left to the user (the assistant could not log in to check itself).
