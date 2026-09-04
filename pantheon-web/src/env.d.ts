/// <reference types="vite/client" />

interface ImportMetaEnv {
  readonly VITE_PANTHEON_SERVICE_URL: string
}

interface ImportMeta {
  readonly env: ImportMetaEnv
}
