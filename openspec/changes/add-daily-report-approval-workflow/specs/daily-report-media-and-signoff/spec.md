## MODIFIED Requirements

### Requirement: Report sign-off
`pantheon-service` SHALL allow a project member to record their sign-off (`DailyReportSignature`) on a daily report only once that report has been approved (`APPROVED`).

#### Scenario: Member signs an approved report
- **WHEN** a project member submits a sign-off request for a daily report in `APPROVED` status
- **THEN** `pantheon-service` records a `DailyReportSignature` for that member on the report, including their construction function at the time of signing

#### Scenario: Cannot sign a draft or pending-approval report
- **WHEN** a project member attempts to sign a daily report that is still in `DRAFT` or `PENDING_APPROVAL` status
- **THEN** `pantheon-service` rejects the request

#### Scenario: Signatures listed
- **WHEN** a project member requests a daily report's signatures
- **THEN** `pantheon-service` returns every `DailyReportSignature` recorded for that report
