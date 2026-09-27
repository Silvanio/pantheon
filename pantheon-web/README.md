# Vue 3 + TypeScript + Vite

This template should help get you started developing with Vue 3 and TypeScript in Vite. The template uses Vue 3 `<script setup>` SFCs, check out the [script setup docs](https://v3.vuejs.org/api/sfc-script-setup.html#sfc-script-setup) to learn more.

Learn more about the recommended Project Setup and IDE Support in the [Vue Docs TypeScript Guide](https://vuejs.org/guide/typescript/overview.html#project-setup).

## Environment modes (Local / Dev / PRD)

See `.env.example` for the full explanation and variable list. In short:

- **Local** — `.env.development`, Vite's default mode (`npm run dev`). Never initializes Sentry.
- **Dev** — `.env.dev`, a custom mode built via `npm run build:dev`.
- **PRD** — `.env.production`, Vite's default build mode (`npm run build`).

Do not create a `.env.local` file to represent "Local" — Vite reserves that filename as an
always-loaded override layered on every mode, not a mode-specific file.
