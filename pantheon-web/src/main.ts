import { createApp } from 'vue'
import * as Sentry from '@sentry/vue'
import './style.css'
import App from './App.vue'
import router from './router'
import { i18n } from './i18n'

const app = createApp(App)

const sentryDsn = import.meta.env.VITE_SENTRY_DSN
// The `development` mode (Local tier, Vite's default `npm run dev`) must never initialize
// Sentry, even if a stray VITE_SENTRY_DSN happens to be set in the shell environment (Vite's
// process.env can override .env.development's own value) — this is a hard, structural
// exclusion, matching pantheon-service's `local` Spring profile ignoring SENTRY_DSN outright.
const sentryAllowedInThisMode = import.meta.env.MODE !== 'development'
if (sentryDsn && sentryAllowedInThisMode) {
  Sentry.init({
    app,
    dsn: sentryDsn,
    environment: import.meta.env.MODE,
    release: __APP_VERSION__,
    // Near-zero rather than 0: some SDK versions treat an explicit 0 as "disable tracing
    // instrumentation entirely" vs. "sample essentially none of it" — 0.01 keeps tracing
    // technically active (matching the free plan's separate, tighter performance-unit quota)
    // while being effectively negligible. This is about errors, not performance tracing.
    tracesSampleRate: 0.01,
    // Default PII scrubbing stays on — sendDefaultPii is intentionally NOT set to true.
  })
  console.info(`[Sentry] initialized (environment: ${import.meta.env.MODE})`)
} else if (!sentryAllowedInThisMode) {
  console.info('[Sentry] disabled: development mode (Local tier) never initializes Sentry')
} else {
  console.info('[Sentry] disabled: VITE_SENTRY_DSN not set')
}

app.use(router).use(i18n).mount('#app')
