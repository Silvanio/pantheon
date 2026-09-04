## ADDED Requirements

### Requirement: Internationalized UI text
`pantheon-web` SHALL source all user-facing text (labels, buttons, messages, validation errors) from i18n locale resource files rather than hardcoding it in components, with `pt-BR` as the default and, for now, only shipped locale.

#### Scenario: New text added through the locale file
- **WHEN** a new user-facing string is introduced by any view or component
- **THEN** it is added as a key in the `pt-BR` locale resource file and referenced through the i18n mechanism, not inlined as a literal in the template or script

#### Scenario: Application renders in Portuguese by default
- **WHEN** a user opens `pantheon-web` without an explicit locale override
- **THEN** every user-facing string renders from the `pt-BR` locale resource file
