## Context

`pantheon-web` already persists one global UI preference this way: `useTheme.ts` reads/writes a `localStorage` key on a module-level `ref`, shared by every component that calls `useTheme()`. The obra hero-collapse preference follows the same shape.

## Goals / Non-Goals

**Goals:**
- Let the user reclaim vertical space on the obra detail page without losing the obra's name from view.
- Make the theme toggle reachable from the obra detail page, not just the dashboard.
- Show the user's own company identity in the dashboard header instead of generic app branding.

**Non-Goals:**
- Per-obra collapse state (explicitly requested as one global preference, not remembered separately per obra).
- Any change to the site cover photo upload/storage mechanism itself — only its visibility toggle.

## Decisions

### Hero-collapse preference: global, localStorage-backed, same pattern as `useTheme`
`useSiteHeroCollapse.ts` mirrors `useTheme.ts` exactly: a module-level `ref<boolean>` seeded from `localStorage.getItem('pantheon-site-hero-collapsed')`, written back on every change via `watchEffect`. Being module-level (not per-component state) means every `SiteDetailView` instance across navigations shares the same reactive value automatically — no explicit cross-component sync needed.

Alternative considered: per-obra storage (key including `siteId`). Rejected — the user explicitly asked for one preference that follows them between obras.

### Icon-only header/hero buttons: two different treatments for two different backgrounds
The hero's collapse button sits on top of a photo of unpredictable color, so it uses a dark, translucent circular badge (`bg-black/35` + blur) that stays legible over anything. The header's expand button and the dashboard's theme toggle sit on the app's own steel/white surface, so they use the bordered square icon-button style already established by `ThemeToggle.vue` (`h-9 w-9 rounded-md border`), rather than inventing a third button style.

### Dashboard branding falls back to the generic brand only when no company is active yet
`companyName` is read the same way `companyId` already was (`activeCompany(status)?.companyName`). The existing `BrandMark` (generic "Pantheon" logo+text) fallback path is left untouched — it only matters during onboarding, before a company exists to name.

## Risks / Trade-offs

- **[Trade-off] The hero-collapse preference is invisible until the user first collapses something.** Default is expanded (matches current/prior behavior), so nothing changes for a user who never touches the new toggle.
