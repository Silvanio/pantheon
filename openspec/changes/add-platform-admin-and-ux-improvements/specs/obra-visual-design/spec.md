## ADDED Requirements

### Requirement: Standard checkbox appearance
`pantheon-web` SHALL style every native `<input type="checkbox">` with the shared `.field-checkbox` component class (bordered, rounded, `accent-color` set to the brand blueprint color in both light and dark mode, with a focus ring matching `.field-input`), rather than the browser's unstyled default appearance.

#### Scenario: Checkbox uses the shared style in both themes
- **WHEN** a member views any checkbox in the app (e.g. Pedido de Compra item selection, Diário de Obra's weather-blocked-tasks toggle, a Cronograma task's done toggle) in either light or dark mode
- **THEN** `pantheon-web` renders it with the `.field-checkbox` class's bordered, rounded, brand-colored appearance instead of the browser's default checkbox
