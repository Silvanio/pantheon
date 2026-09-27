/// <reference types="vite/client" />

interface ImportMetaEnv {
  readonly VITE_PANTHEON_SERVICE_URL: string
  readonly VITE_SENTRY_DSN?: string
}

interface ImportMeta {
  readonly env: ImportMetaEnv
}

// Injected at build time by vite.config.ts's `define`, from package.json's `version` field.
declare const __APP_VERSION__: string
