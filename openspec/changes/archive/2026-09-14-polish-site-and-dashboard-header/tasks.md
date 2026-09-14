## 1. Obra detail page: collapsible cover photo + theme toggle

- [x] 1.1 Add `useSiteHeroCollapse.ts` composable (global, `localStorage`-persisted, same pattern as `useTheme.ts`).
- [x] 1.2 `SiteDetailView.vue`: render the cover photo only when expanded, with a collapse button on it (dark translucent circular badge, legible over any photo).
- [x] 1.3 `SiteDetailView.vue`: when collapsed, show the obra's name in the header (`#left`, after the back button) and an expand button in the header (`#right`), styled like `ThemeToggle.vue`'s bordered square icon button.
- [x] 1.4 `SiteDetailView.vue`: add `<ThemeToggle />` to the header's `#right` slot.
- [x] 1.5 Add `siteDetail.collapsePhoto` / `siteDetail.expandPhoto` keys to `pt-BR.json`.

## 2. Dashboard header branding and layout

- [x] 2.1 `DashboardView.vue`: replace the hardcoded "Pantheon" header text with the active company's `companyName`; keep the `BrandMark` fallback for when no company is active yet.
- [x] 2.2 `DashboardView.vue`: nudge the logo+name flush to the header's left edge (`-ml-1`).
- [x] 2.3 `DashboardView.vue`: reorder the header's right-side controls so `<ThemeToggle />` is the last (rightmost) element, after the profile menu.

## 3. Verification

- [x] 3.1 `cd pantheon-web && npm run build` (vue-tsc + vite build) green after each round of changes.
- [ ] 3.2 Live visual check in a logged-in session (collapse/expand on the obra page, header spacing on the dashboard, dark mode toggle in both places). **Not run in this session** — the assistant could not log in to verify; left for the user.
