## Why

The daily report's "materials received" (from `add-daily-construction-report`) is a simple same-day log entry, but real procurement needs a request-then-approve-then-verify flow: someone asks for materials, someone with authority approves the purchase/delivery, and someone checks what actually arrived against what was requested. The competitor's feature list doesn't cover this, but the product owner explicitly asked for it as a companion capability outside the daily log.

## What Changes

- Add `MaterialRequest`: created against a `ConstructionSite`, with one or more line items (referencing the site's `Material` catalog, requested quantity), starting in `PENDING` status.
- Add approval: the project administrator or a member with construction function `ENGINEER` can approve or reject a pending request (rejection requires a reason).
- Add `ReceiptVerification`: when a delivery arrives against an approved request, a project member records the quantity actually received per line item, flagging divergences from the requested quantity.
- An approved request's verification progress determines its final status (`APPROVED` → `PARTIALLY_RECEIVED` or `RECEIVED` once all line items have a verification record).
- `pantheon-web`: views to create a request, list/track requests by status, approve/reject, and record a receipt verification — all copy sourced from the `pt-BR` locale resource file.

## Capabilities

### New Capabilities
- `material-request-workflow`: material request creation, approval/rejection, and received-materials verification, scoped to a construction site.

### Modified Capabilities
(none)

## Impact

- **Affected code**: `pantheon-service` (new `MaterialRequest`, `MaterialRequestItem`, `ReceiptVerification` entities/repositories/services/controllers/DTOs; new Flyway migrations), `pantheon-web` (new material request creation/list/approval/verification views).
- **Depends on** `add-construction-site-and-team` (ConstructionSite, member function) and `add-equipment-and-material-registry` (Material catalog references).
- **New REST surface**: `POST /api/construction-sites/{siteId}/material-requests`, `GET /api/construction-sites/{siteId}/material-requests`, `GET /api/material-requests/{id}`, `POST /api/material-requests/{id}/approve`, `POST /api/material-requests/{id}/reject`, `POST /api/material-requests/{id}/receipt-verifications`.
- **Not affected**: `add-daily-construction-report`'s "materials received" log entry — the two remain independent by design; a delivery may be logged in both places without one driving the other in this phase.
- **Non-goals**: multi-level/sequential approval chains, purchase order integration with suppliers, automatic stock/inventory levels, partial-approval of individual line items (a request is approved or rejected as a whole).
