## 1. Backend

- [x] 1.1 Migration `V66__split_daily_report_weather_by_period.sql`: added the two columns, backfilled both from `weather_condition`, dropped it. Also dropped+recreated `daily_report_core_fields_together` (V61's DB-level check constraint referenced the old column directly — not called out in design.md, found while implementing).
- [x] 1.2 `DailyReport` entity: `weatherConditionMorning`/`weatherConditionAfternoon` fields/getters; `updateCore(...)` and its core-fields-required check updated.
- [x] 1.3 `DailyReportCoreUpdateRequest`, `DailyReportResponse` updated (`DailyReportDetailResponse` wraps `DailyReportResponse`, no separate field to change).
- [x] 1.4 `DailyReportService`/`DailyReportPdfService` call sites updated (PDF now shows "Clima (manhã)"/"Clima (tarde)" as two rows); `DailyReportServiceTest` updated (split the old single "weather missing" test into separate morning/afternoon-missing tests).

## 2. Frontend (`pantheon-web`)

- [x] 2.1 `DailyReportDetailView.vue`: two weather-chip pickers (Manhã/Tarde), both required in the save guard.
- [x] 2.2 Read-only detail view shows both periods; `DailyReportsPanel.vue`'s history table shows a combined "Manhã / Tarde" summary via a new `weatherSummary()` helper.
- [x] 2.3 `pt-BR.json`: `weatherCondition` → `weatherConditionMorning`/`weatherConditionAfternoon`.

## 3. Mobile (`pantheon-mobile`)

- [x] 3.1 `daily_report_detail_screen.dart`'s edit form: two weather chip-pickers (Manhã/Tarde), both required.
- [x] 3.2 Read-only display shows both periods ("Manhã: ...", "Tarde: ...").
- [x] 3.3 `daily_report_core_fields_test.dart` rewritten for the two-field requirement (7 tests, was 5).

## 4. Verification

- [x] 4.1 `./mvnw test` — 367/367 green. (Also fixed an unrelated pre-existing bug found along the way: `application-local.yml` had the real Sentry DSN hardcoded instead of `""`, breaking the `sentryStaysDisabledOnLocalProfile` test and the Local-never-sends-to-Sentry guarantee — corrected.)
- [x] 4.2 `vue-tsc -b` — clean aside from the pre-existing, unrelated `OrcamentoDetailView.vue` error.
- [x] 4.3 `flutter analyze` — no issues. `flutter test` — 7/7 green.
