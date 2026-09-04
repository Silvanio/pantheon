## Context

`Material` (catalog of types) exists from `add-equipment-and-material-registry`; this change adds the request/approve/verify workflow around it, independent of the daily report's own lightweight materials-received log.

## Goals / Non-Goals

**Goals:**
- A simple, single-level approve/reject gate on material requests.
- A verification step that records what was actually received per line item and surfaces divergences without blocking anything (informational, not a hard gate).
- Keep this workflow decoupled from the daily report, per explicit product direction ("fora do diário").

**Non-Goals:**
- Multi-approver/sequential approval chains.
- Supplier/purchase-order integration.
- Automatic inventory/stock-level tracking beyond this request's own line items.
- Partial approval of individual line items within one request.
- Automatically reflecting a verification in the daily report's materials-received section (kept independent, as directed).

## Decisions

### Entity model
```
MaterialRequest                          -- table: material_request
  id                    UUID PK
  construction_site_id  UUID FK construction_site.id
  status                VARCHAR   -- PENDING | APPROVED | REJECTED | PARTIALLY_RECEIVED | RECEIVED
  requested_by          UUID FK app_user.id
  decided_by            UUID NULL FK app_user.id
  decision_note         VARCHAR NULL   -- required when status = REJECTED
  decided_at            TIMESTAMPTZ NULL
  created_at / updated_at

MaterialRequestItem                      -- table: material_request_item
  id                    UUID PK
  material_request_id   UUID FK material_request.id
  material_id           UUID FK material.id
  requested_quantity    NUMERIC

ReceiptVerification                      -- table: receipt_verification
  id                        UUID PK
  material_request_item_id  UUID FK material_request_item.id
  received_quantity         NUMERIC
  verified_by               UUID FK app_user.id
  verified_at                TIMESTAMPTZ
  note                       VARCHAR NULL   -- e.g. reason for divergence
```
One `ReceiptVerification` per `MaterialRequestItem` (a line item can only be verified once in this phase — see Open Questions for partial/multiple-delivery handling). Divergence is derived, not stored: `received_quantity != requested_quantity` is computed at read time.

### Approval permission
Approve/reject is restricted to `ADMIN` or a member with construction function `ENGINEER`, mirroring the real-world responsibility split (the engineer or the admin signs off on spend/delivery), distinct from `add-equipment-and-material-registry`'s `SITE_FOREMAN`-centric catalog management permission.

*Alternative considered*: also allowing `SITE_FOREMAN` to approve — rejected because the site foreman is more naturally the *requester* (they know what's needed on site day-to-day) while the engineer/admin is the natural approver; keeping them distinct avoids a request being both created and approved by the same operational role.

### Status derivation
`MaterialRequest.status` transitions: `PENDING` → (`APPROVED` | `REJECTED`) via the approval decision; once `APPROVED`, it becomes `PARTIALLY_RECEIVED` as soon as at least one (but not all) line items have a verification record, and `RECEIVED` once every line item does. This is stored (denormalized) on the `MaterialRequest` row for simple listing/filtering, recomputed transactionally whenever a `ReceiptVerification` is inserted.

*Alternative considered*: computing status on read from the line items every time (no stored status transition for receipt) — rejected because listing/filtering requests by status (a core UI need — "list pending", "list received") would otherwise require joining and aggregating on every list call; a denormalized, transactionally-updated status column is simpler for that access pattern.

## Risks / Trade-offs

- **[Risk]** A line item can only be verified once, so a delivery split across multiple shipments (e.g., half today, half next week) isn't naturally represented → **Mitigation**: acceptable for this phase; flagged in Open Questions as a likely near-term follow-up.
- **[Risk]** Denormalized `status` on `MaterialRequest` can drift from its line items' verification state if updated inconsistently → **Mitigation**: recompute-and-write happens in the same transaction as each verification insert, not as a separate step.
- **[Risk]** No line-item-level approval means one unwanted item forces rejecting (and presumably resubmitting) the whole request → **Mitigation**: accepted as an explicit Non-Goal for this phase; the requester can split future requests into smaller ones if this becomes friction.

## Migration Plan

Additive only: new `material_request`, `material_request_item`, `receipt_verification` tables via new Flyway migrations. No existing table/endpoint changes. Rollback: drop the three new tables.

## Open Questions

- Should a line item support multiple partial verifications (split deliveries) rather than exactly one? This design assumes exactly one verification per line item for now; revisit if split deliveries turn out to be common in practice.
- Does a rejected request support resubmission (edit + resubmit) or must a new request be created from scratch? This design assumes a new request must be created; `REJECTED` is terminal.
