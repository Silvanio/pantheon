## Why

The user asked for a full visual redesign of `pantheon-web` (colors, navigation layout, overall look) while keeping every existing feature intact — the old design used a single flat blue accent, a single orange accent for every alert/destructive action, no persistent navigation (only a per-view top header, with an obra's 9 sections crammed into a horizontal tab bar), and no color-coded status system. The user reviewed a prototype (a redesign proposal artifact) inspired by a competitor's construction-management product and approved it, asking for it to be implemented across the whole app — dashboard, obra workspace, purchase-request approval, Kanban board, PDFs, daily report, orçamentos — with the direction recorded in specs and `AGENTS.md` so future work stays consistent with it.

## What Changes

- New color system: `blueprint-*` (brand blue) and `steel-*` (neutrals) tokens get new hex values; adds a new `ink-*` scale (always-dark, for the sidebar) and repurposes `safety-*` as a coral danger scale. Status semantics (pending/approved/rejected/etc.) continue to use Tailwind's built-in `emerald-*`/`amber-*` scales via `StatusBadge.vue`, now including a `constructionSite` status kind.
- New typeface: Manrope replaces Inter app-wide (screens only — PDFs keep standard fonts).
- New persistent app shell: a dark, icon-only global sidebar (`AppSidebar.vue`) on every authenticated screen, linking back to the dashboard.
- Inside an obra, the horizontal tab bar is replaced by a second, labeled sidebar listing the obra's sections (a compact horizontal fallback is kept for narrow viewports).
- Dashboard obra cards gain a gradient banner and a real status badge (`ConstructionSite.status`, already existed but wasn't shown).
- The Pedido de Compra approval stepper and pending-approval banner are visually redesigned (checkmarked circular steps, amber pending banner, distinct approve/reject button colors via a new `.btn-success` utility).
- The Kanban board (`TasksBoardPanel.vue`) gains a colored dot per column, a card count badge, and card-level visual polish (no drag/drop or data-model changes).
- `PdfBrandingService.STYLE` (shared by every generated PDF) is recolored to match the new `blueprint-*`/`steel-*` palette.
- **BREAKING** (visual only, not behavioral): every screen's look changes; no API, route, or data-model behavior changes.

## Capabilities

### New Capabilities
- `obra-visual-design`: the app's shared design system (color tokens, typography, the persistent sidebar navigation shell, and how PDFs stay visually consistent with it) — the reference for all future screens.

### Modified Capabilities
(none — no existing capability's functional requirements change; this is purely the shared visual language those capabilities' screens already render through)

## Impact

- `pantheon-web`: `style.css` (tokens, font, `.btn-success`), `index.html` (font link), new `AppSidebar.vue`, `SiteDetailView.vue` (sidebar nav replacing tab bar), `DashboardView.vue` (card redesign), `StatusBadge.vue` (`constructionSite` kind), `PurchaseRequestDetailView.vue` (stepper/approval banner), `TasksBoardPanel.vue` (column/card styling), and every other authenticated view (sidebar wrap only: `UserProfileView.vue`, `GlobalTasksBoardView.vue`, `OrcamentoDetailView.vue`, `DailyReportDetailView.vue`, `CompanySettingsView.vue`).
- `pantheon-service`: `PdfBrandingService.STYLE` (shared CSS for every PDF).
- `AGENTS.md`: new design-system convention entry.
