## 1. Design tokens and typography

- [x] 1.1 Update `blueprint-*`/`steel-*` hex values in `pantheon-web/src/style.css`'s `@theme` block; add `ink-*` (sidebar-only, always-dark) scale; repurpose `safety-*` as a coral danger scale.
- [x] 1.2 Confirm status colors stay on Tailwind's built-in `emerald-*`/`amber-*` (used by `StatusBadge.vue`) rather than a custom scale — an earlier draft added custom `mint`/`amber` tokens that collided with Tailwind's built-ins; reverted.
- [x] 1.3 Switch `--font-sans` to Manrope; add the Google Fonts `<link>` to `pantheon-web/index.html`.
- [x] 1.4 Add `.btn-success` to `style.css`'s `@layer components` for positive/confirming actions (approve, mark done) distinct from the primary brand blue.

## 2. Persistent navigation shell

- [x] 2.1 Create `AppSidebar.vue` (global, icon-only, `bg-ink-950`, logo + dashboard link).
- [x] 2.2 Add `appSidebar.home`/`appSidebar.myObras` pt-BR locale keys.
- [x] 2.3 Wrap `DashboardView.vue`, `UserProfileView.vue`, `GlobalTasksBoardView.vue`, `OrcamentoDetailView.vue`, `DailyReportDetailView.vue`, `CompanySettingsView.vue`, and `PurchaseRequestDetailView.vue` with `AppSidebar` alongside their existing `AppHeader`.
- [x] 2.4 `SiteDetailView.vue`: replace the horizontal 9-tab `<nav>` with a labeled obra sidebar (photo, name, `StatusBadge`, vertical icon+label nav list); keep the original tab markup as a `md:hidden` narrow-viewport fallback with identical visibility rules.
- [x] 2.5 Add `StatusBadge`'s `constructionSite` kind (`PLANNING`/`IN_PROGRESS`/`PAUSED`/`COMPLETED`) and `constructionSites.status.*` pt-BR labels.

## 3. Screen-level redesigns

- [x] 3.1 `DashboardView.vue`: gradient card banner, `StatusBadge kind="constructionSite"` on each obra card.
- [x] 3.2 `PurchaseRequestDetailView.vue`: circular checkmark/amber status stepper; amber pending-approval banner with avatar-style step number, distinct `.btn-success`/`.btn-danger` approve/reject actions; added `purchaseRequests.pendingStepSubtitle` locale key.
- [x] 3.3 `TasksBoardPanel.vue`: colored dot + card count per column header, card border/shadow polish. No drag/drop or data-model changes.

## 4. PDFs

- [x] 4.1 Recolor `PdfBrandingService.STYLE` to the new `blueprint-*`/`steel-*` hex values (shared by `PurchaseRequestPdfService` and `DailyReportPdfService`). Keep Helvetica/Arial — no webfont embedding (see design.md decision 5).
- [x] 4.2 Confirm no test asserts on the old hex values; run `PdfBrandingServiceTest`, `PurchaseRequestPdfServiceTest`, `DailyReportPdfServiceTest`.

## 5. Verification

- [x] 5.1 `npm run build` (type-check + bundle) after each meaningful batch of frontend changes.
- [x] 5.2 `./mvnw test` (full backend suite) after the PDF color change.
- [x] 5.3 Live-verify in the browser: registered a throwaway account, created a company/obra/tasks/purchase-request via direct API calls (UI form file-input couldn't be driven by the available browser-automation tools), and visually confirmed the dashboard card, obra sidebar nav, Pedido de Compra stepper, and Kanban board all render the new design correctly against the real running app (not just the static prototype).

## 6. Documentation

- [x] 6.1 Add a "Visual design system" entry to `AGENTS.md`'s Conventions section (tokens, sidebar pattern, typeface, PDF color/font trade-off).
- [x] 6.2 Write this change's proposal/design/spec and archive it once applied.
