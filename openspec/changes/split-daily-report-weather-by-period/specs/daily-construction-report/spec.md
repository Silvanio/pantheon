## MODIFIED Requirements

### Requirement: Weather condition logging
`pantheon-service` SHALL allow a weather condition to be recorded separately for the morning and afternoon periods of a draft daily report, plus whether weather blocked planned tasks that day.

#### Scenario: Weather recorded for both periods
- **WHEN** a project member submits a morning weather condition, an afternoon weather condition, and whether weather blocked planned tasks for a draft daily report
- **THEN** `pantheon-service` saves both weather conditions and the blocked-tasks flag on that report

#### Scenario: Incomplete weather rejected
- **WHEN** a project member submits only a morning weather condition, leaving the afternoon one unset, in a way that would leave the report's core fields incomplete
- **THEN** `pantheon-service` rejects the request
