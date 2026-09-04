import { createI18n } from 'vue-i18n'
import ptBR from './locales/pt-BR.json'

/**
 * `pt-BR` is the only shipped locale for now — see add-construction-site-and-team's design.md
 * "pantheon-web internationalization". Every user-facing string added from here on is a key in
 * a locale resource file, never a literal in a `.vue` template/script.
 */
export const i18n = createI18n({
  legacy: false,
  locale: 'pt-BR',
  fallbackLocale: 'pt-BR',
  messages: {
    'pt-BR': ptBR,
  },
})
