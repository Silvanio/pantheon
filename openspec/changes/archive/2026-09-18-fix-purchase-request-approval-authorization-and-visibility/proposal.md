## Why

A construction-site member who is both a company-staff `CompanyMembership` (e.g. the company owner, registered as the site's `CLIENT`) and a `SiteMembership` with a specific function on a site can currently approve **any** Pedido de Compra approval step, regardless of which function it requires — because `SiteAccessService` resolves company-staff status before ever looking at that member's real `SiteMembership` function, and the approval code treats company staff as an unconditional bypass. A client was able to approve an engineer's approval step. Separately, every member with any non-`HIDDEN` `PURCHASE_REQUEST` access sees every Pedido de Compra on the site immediately, even ones where they have no current or past role to play in the approval chain — cluttering their view and leaking purchase decisions they have no business seeing yet.

## What Changes

- **BREAKING**: The company-staff bypass for acting on an approval step now applies only when the acting user holds no active `SiteMembership` at all on that specific site. A user who holds an active `SiteMembership` there — company staff or not — must have a function matching the pending step's function to approve or reject it, with no exception.
- Acting on an approval step now also requires the member's resolved `PURCHASE_REQUEST` access level to be `MANAGE` or the new `VIEW_AND_APPROVE` (never `VIEW` or `HIDDEN`), even when their function matches the step.
- Adds a new `AccessLevel` value, `VIEW_AND_APPROVE`, usable only for the `PURCHASE_REQUEST` capability: a member at this level cannot create a Pedido de Compra or submit one for approval, but can approve/reject a step matching their function, and only sees a given Pedido de Compra when it is `CONCLUIDO` or when the current approval cycle has (or had) a step matching their function.
- `VIEW` on `PURCHASE_REQUEST` becomes a real, unrestricted read-only mode: a `VIEW` member always sees every Pedido de Compra on the site, and can never act on any approval step even if their function would otherwise match.
- Updates the `CLIENT` function's default `PURCHASE_REQUEST` access level from `VIEW` to `VIEW_AND_APPROVE`, matching the client's usual role as an approver rather than a full manager.
- `pantheon-web`'s obra permission-configuration view gains `VIEW_AND_APPROVE` as a selectable access level for `PURCHASE_REQUEST`, and the Pedido de Compra screens stop offering actions (create, submit, approve, reject, conclude) the viewer's resolved access can never actually perform — including hiding the approve/reject actions from a viewer whose own function doesn't match the *currently* pending step (a `pantheon-web`-only concern needing a new `GET /api/sites/{siteId}/members/mine` endpoint, since no existing frontend call exposed "my own function on this site").
- Concluding a `CONFERIDO` Pedido de Compra now requires the same access levels as acting on an approval step (`MANAGE` or `VIEW_AND_APPROVE`), not a strict `MANAGE`-only check — so a `VIEW_AND_APPROVE` client who approved the final step can also mark it concluded.

## Capabilities

### New Capabilities
(none)

### Modified Capabilities
- `obra-permission-management`: the `PURCHASE_REQUEST` capability gains a third real access level (`VIEW_AND_APPROVE`) alongside `VIEW` and `MANAGE`; the `CLIENT` function's default changes; `VIEW` is clarified as always-visible/never-authoring/never-approving.
- `purchase-request-approval-workflow`: acting on an approval step now requires an access-level check in addition to the function match, and the company-staff bypass is narrowed to members with no `SiteMembership` on the site; adds dynamic, per-member visibility filtering of Pedidos de Compra for `VIEW_AND_APPROVE` members.

## Impact

- `pantheon-service`: `AccessLevel`, `SitePermissionService` (defaults + docs), `PurchaseRequestService` (`requireStepAuthority`, `approveStep`, `rejectStep`, `list`, `get`, `getComparison`, `listInvoices`, `getInvoiceContent`), `SiteAccessService` usage patterns, `PurchaseRequestApprovalRepository` queries, new `SiteMembershipService.findMyFunction` + `GET /api/sites/{siteId}/members/mine` endpoint (`SiteMembershipController`), associated unit tests.
- `pantheon-web`: obra permission-configuration view (access-level selector), `PurchaseRequestPanel.vue`/`PurchaseRequestDetailView.vue` action visibility (including function-matching via the new `useSiteMembers().getMyFunction`), `pt-BR` locale strings.
