## Context

The user reviewed a prototype (an Artifact-hosted canvas with 6 mock screens: cover/style-guide, login, dashboard, obra workspace, Pedido de Compra approval, and Kanban board) built from a competitor's visual language, and approved it wholesale, asking for full implementation with no further check-ins. The prototype used illustrative/fabricated data (progress percentages, avatar stacks, gradient banner photos) that doesn't exist in Pantheon's real domain model — the implementation below adapts the prototype's structure and palette to only the data that's actually available (e.g. `ConstructionSite.status`, which already existed in the backend but was never surfaced in the UI, stands in for the prototype's fabricated progress bar).

## Goals / Non-Goals

**Goals:**
- Ship a systemic color/typography change that cascades to (almost) every screen via shared Tailwind `@theme` tokens and shared components, rather than a per-screen rewrite.
- Add persistent navigation (global sidebar + obra sidebar) so wayfinding doesn't depend solely on browser back or a cramped tab bar.
- Keep every existing feature, route, and API contract identical — this is a reskin, not a rebuild.
- Recolor PDFs to match, without risking PDF generation reliability.

**Non-Goals:**
- No fabricated data on real screens (no invented progress percentages, no avatar stacks not backed by a real assignee list on that screen).
- No drag/drop, permission, or workflow logic changes to the Kanban board, approval flow, or any other feature — visual only.
- No PDF font embedding in this pass (see decision 5).
- No login/registration flow *behavior* change — `LoginView.vue` inherits the new tokens/font automatically and was not otherwise touched.

## Decisions

### 1. Reuse token names, change values

`blueprint-*`, `steel-*`, and `safety-*` keep their existing names in `style.css`'s `@theme` block; only their hex values change. ~37 `.vue` files already reference these tokens via Tailwind classes (`bg-blueprint-600`, `text-steel-700`, etc.) — changing values in one place cascades everywhere without touching those files. Two new scales are added: `ink-*` (the sidebar's own scale, always dark regardless of light/dark theme — distinct from `steel-*`, which flips) and nothing else; status semantics deliberately do NOT get a custom scale (decision 2).

### 2. Status colors reuse Tailwind's built-in `emerald`/`amber`, not new custom tokens

An earlier draft of this change added custom `--color-mint-*` and `--color-amber-*` tokens to `@theme`. Caught immediately: `StatusBadge.vue` already uses Tailwind's *built-in* `amber-100`/`amber-800`/`emerald-*` classes for exactly this purpose — defining a custom scale under the `amber` name would collide with Tailwind's own default palette (undefined/inconsistent per-shade override behavior in Tailwind v4). Reverted; status colors stay on Tailwind's stock `emerald-*` (success) and `amber-*` (pending/warning) scales, with `safety-*` (now coral) for rejected/danger — matching what was already centralized in `StatusBadge.vue` before this change, just with `safety-*`'s hex values updated.

### 3. Global sidebar as a new component, wrapped per-view

`App.vue` is a bare `<RouterView>` with no shared layout — each authenticated view builds its own chrome by using `<AppHeader>` directly. Rather than introduce a router-level layout wrapper (higher risk of regressing every route's rendering in one change), `AppSidebar.vue` is a new, self-contained, presentational component (logo mark + a single "my obras" nav icon; no data fetching beyond the current route) added as a sibling of `<AppHeader>` inside each of the 8 views that already render one: `DashboardView`, `SiteDetailView`, `UserProfileView`, `GlobalTasksBoardView`, `OrcamentoDetailView`, `PurchaseRequestDetailView`, `DailyReportDetailView`, `CompanySettingsView`. Onboarding-flow views (`CompanyCreationView`, `PlanSelectionView`, `CompanyProfileView`) don't use `AppHeader` today and are left as focused, chrome-free wizards — consistent with their existing design, not part of this change.

### 4. Obra sidebar replaces the tab bar; ProfileMenu/ThemeToggle stay put

`SiteDetailView.vue`'s horizontal `<nav>` of 9 tab buttons becomes a second, labeled `<aside>` (obra photo + name + status badge, then a vertical nav list with icons) to the right of the global sidebar — matching the prototype and giving each obra section its own icon and permanent visual slot instead of competing for horizontal space. The tab-switching logic (`activeTab`, `isTabVisible`, `TAB_ORDER`) is untouched; only the markup rendering it changed, plus a `md:hidden` copy of the old horizontal-tab-button markup is kept as a fallback for viewports narrower than the sidebar can support. `ProfileMenu`/`ThemeToggle` stay in `AppHeader`'s right slot rather than moving into the sidebar footer (as sketched in the prototype) — moving them would require reworking `ThemeToggle.vue`'s hardcoded light-chrome styling to work on a dark background, which is unnecessary risk for a cosmetic relocation with no functional benefit.

### 5. PDFs get the new palette, not the new font

`PdfBrandingService.STYLE` (one CSS string shared by every PDF service) is a straightforward hex-value swap to the new `blueprint-*`/`steel-*` values — same structure, same class names, so no template/markup changes needed in `PurchaseRequestPdfService`/`DailyReportPdfService`. Embedding Manrope for PDF body text was considered and dropped: Manrope is only published as a variable font on Google Fonts (no static-weight TTFs in the repo), and openhtmltopdf's variable-font handling is known to be inconsistent; the risk of breaking PDF generation for every document type outweighs exact typeface parity with the screen. PDFs keep Helvetica/Arial (PDF-safe standard fonts), matching the product on color but not typeface.

### 6. Real data only — no fabricated metrics on live screens

The prototype's dashboard obra cards showed a fabricated "progress %" bar and multi-avatar team stack; neither is backed by real, cheaply-available data (no progress-percentage field exists on `ConstructionSite`, and per-card team-avatar fetching would mean N additional API calls per dashboard load). The shipped `DashboardView.vue` card instead surfaces `ConstructionSite.status` (`PLANNING`/`IN_PROGRESS`/`PAUSED`/`COMPLETED`) via a new `StatusBadge` `constructionSite` kind — a real backend field (`ConstructionSiteService.updateStatus` already existed) that was simply never rendered anywhere in the UI before this change.

## Risks / Trade-offs

- [PDF/screen font mismatch (Manrope on screen, Helvetica/Arial in PDFs)] → Accepted; see decision 5. Revisit if a reliable static Manrope TTF becomes available.
- [`AppSidebar` is duplicated per-view rather than router-level] → A handful of near-identical wrapper edits across 8 files instead of one router layout change; accepted for lower blast radius. A future cleanup could introduce a shared authenticated-layout route wrapper.
- [`ConstructionSite.status` was never user-settable from the UI before this change and still isn't] → Out of scope; this change only *displays* the existing field, it doesn't add a way to change it. A separate change would be needed to let a user transition an obra's status from the UI.

## Migration Plan

No data migration — purely presentational plus one previously-dead field (`ConstructionSite.status`) now being displayed. Ship frontend and backend (PDF colors) together so a freshly generated PDF and the screen that triggered it look consistent immediately.
