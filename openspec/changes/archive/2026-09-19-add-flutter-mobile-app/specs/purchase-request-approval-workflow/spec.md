## ADDED Requirements

### Requirement: Approval-step transitions trigger a push notification
In addition to their existing effects, submitting a Pedido de Compra for approval, approving a step, rejecting a step, and concluding a Pedido de Compra SHALL each trigger a best-effort push notification (via the `push-notifications` capability) to the affected user(s): the site members whose function matches the newly-pending step's approver function on submit/approve, and the request's creator on reject/conclude. A failure or absence of push configuration SHALL NOT affect the underlying state transition.

#### Scenario: Submitting for approval notifies the first approver
- **WHEN** a Pedido de Compra is submitted for approval and its first step's approver function is ENGINEER
- **THEN** the site's members with an active ENGINEER `SiteMembership` and a registered device token receive a push notification, and the submission itself succeeds regardless of push delivery outcome

#### Scenario: Rejecting a step notifies the creator
- **WHEN** an approver rejects the pending step of a Pedido de Compra
- **THEN** the request's creator receives a push notification if they have a registered device token, and the rejection itself succeeds regardless of push delivery outcome
