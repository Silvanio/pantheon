## Context

`pantheon-service` already has a minimal SSE stack: `SseBroadcaster` (an in-memory `CopyOnWriteArrayList<SseEmitter>`), `SseController` (`GET /api/sse/subscribe`, JWT via `access_token` query param since `EventSource` can't set headers), and `pantheon-web`'s `useSse.ts` composable. Its only current emitter is `AppUserService.publishUserRegistered`, broadcast unscoped to every connected client. `infra/k8s/pantheon-service.yaml` is pinned to `replicas: 1` with a comment explaining why: emitter state is per-pod, so a client connected to pod B never sees an event triggered by a request that landed on pod A.

The `TaskCardService.moveCard()` use case needs a real-time push that (a) only reaches users belonging to the moved card's company, and (b) reaches its intended recipients regardless of which `pantheon-service` pod they're connected to or which pod handled the move. The project already runs RabbitMQ (`spring-boot-starter-amqp`, topic exchange `pantheon.events` consumed by `pantheon-message`) and has no Redis; the design reuses RabbitMQ rather than introducing a new dependency.

## Goals / Non-Goals

**Goals:**
- Deliver a `task-card-moved` SSE event to every connected client belonging to the moved card's company, in real time, regardless of how many `pantheon-service` replicas are running or which one handled the move.
- Make `SseBroadcaster` company-scoped in general (not just for this one event), fixing the current unscoped-broadcast behavior for any future event too.
- Keep the existing unscoped `user-registered` event working unchanged (it has no company context — a user registering has no `CompanyMembership` yet).
- Make it safe to raise `pantheon-service`'s replica count above 1.

**Non-Goals:**
- No change to the global (admin-only, cross-obra) Tasks board (`global-tasks-board`) in this change — only the per-obra board (`obra-tasks-board`) reacts to the new event. The event payload carries enough (`constructionSiteId`) that wiring the global board later is a small follow-up, not a redesign.
- No fine-grained delivery by site/permission (e.g. excluding a company member who lacks `VIEW` on the site). Company-level scoping is judged sufficient because site access within a company is already the norm for this feature set, and per-connection site-permission filtering would need to be re-evaluated on every membership change; out of scope here.
- No Redis, no WebSockets, no third-party push service (Pusher/Ably). RabbitMQ is reused as the only new moving part.
- No reverse-proxy/ingress manifest is added — the repo currently has no nginx/ingress config in front of `pantheon-service`'s K8s `Service`. `proxy_buffering off` is documented as a requirement for whoever adds one, not implemented here.

## Decisions

### 1. `SseBroadcaster` keeps both a global path and a per-company index
`SseBroadcaster` moves from one flat `List<SseEmitter>` to:
- `Set<SseEmitter> allEmitters` — every connected emitter, used by `broadcast(eventName, data)` (unscoped; keeps `user-registered` working) and by the new heartbeat sweep.
- `Map<UUID companyId, Set<SseEmitter>> emittersByCompany` — used by the new `broadcastToCompany(companyId, eventName, data)`.

`subscribe(Set<UUID> companyIds)` adds the emitter to `allEmitters` and to each bucket in `emittersByCompany` for the caller's company ids (a user belonging to more than one company is registered under all of them). Cleanup (`onCompletion`/`onTimeout`/`onError`) removes the emitter from `allEmitters` and every bucket it was added to, using the same `companyIds` set captured at subscribe time.

Alternative considered: drop the unscoped path entirely and force every event to declare a company. Rejected — `user-registered` fires before any `CompanyMembership` exists, so it has no company to scope to; keeping both paths is less churn than inventing a fake scope for that event.

### 2. Company-scoping is resolved once, at SSE subscribe time
`SseController.subscribe()` loads the caller's active `CompanyMembership`s (`CompanyMembershipRepository.findByUserId`, filtered to `isActive()`) and passes that set of company ids to `SseBroadcaster.subscribe(...)`. Membership changes that happen while a connection is already open are not retroactively applied to that connection.

Alternative considered: re-check membership on every broadcast. Rejected as unnecessary DB load for an edge case (a user rarely gets added to a company mid-session); the existing reconnect-with-backoff behavior in `useSse.ts` naturally re-resolves membership on the next reconnect (network blip, pod restart/deploy, token refresh). Documented as an accepted limitation below.

### 3. Cross-pod fan-out: one fanout exchange, one exclusive queue per pod, local filtering
A new RabbitMQ `FanoutExchange` named `pantheon.sse.events` (declared in a new `SseRabbitConfig`, separate from the existing `pantheon.events` topic exchange used for `pantheon-message` work items — mixing SSE fan-out traffic into that queue would pollute a durable, business-critical work queue with ephemeral broadcast messages). Each `pantheon-service` instance declares its own **exclusive, auto-delete, server-named queue** bound to that exchange on startup (via `@RabbitListener(bindings = @QueueBinding(...))` on an anonymous `@Queue`) — a fanout exchange ignores routing keys and delivers every message to every bound queue, so every pod receives every SSE-worthy event. The listener (`SseEventListener`) deserializes the envelope and calls `sseBroadcaster.broadcast(...)` or `.broadcastToCompany(...)` depending on whether `companyId` is present, delivering only to that pod's **local** emitters.

Publishing goes through a new `SseEventPublisher` (mirrors the existing `EventPublisher`, but targets `pantheon.sse.events` instead of `pantheon.events`), called from `TaskCardService.moveCard()` (new) and `AppUserService.publishUserRegistered` (migrated from calling `SseBroadcaster` directly, so `user-registered` also fans out to other pods instead of only reaching the originating pod's local clients).

Alternatives considered:
- **Per-company queue.** Rejected: would require N companies × M pods queues (or shared per-company queues that every pod still has to consume in full to know if it has a local subscriber — no actual reduction in fan-out), for no benefit at this scale. A single fanout queue per pod with in-memory filtering is the standard pattern and is simpler.
- **Sticky sessions on the load balancer instead of a backplane.** Rejected: sticky sessions only pin one client's *connection* to a pod; they do nothing for the *origin* of an event, which can be triggered by a different client connected to a different pod. The backplane is required regardless of LB stickiness.
- **Redis pub/sub.** Rejected: introduces a new infra dependency the project doesn't otherwise need; RabbitMQ already does the job and is already deployed everywhere `pantheon-service` runs.

### 4. Event contract
Event name: `task-card-moved`. Payload (JSON):
```json
{
  "cardId": "uuid",
  "constructionSiteId": "uuid",
  "companyId": "uuid",
  "columnId": "uuid",
  "sortOrder": 0,
  "movedBy": "uuid",
  "movedAt": "2026-09-14T12:34:56Z"
}
```
`companyId` is the routing key for `broadcastToCompany`; `constructionSiteId` lets `pantheon-web` ignore events for a different obra's board without a second round trip. The envelope wrapping this on the RabbitMQ side (`SseEventEnvelope{eventName, companyId, payload}`) is internal plumbing, never seen by the browser — the browser only ever receives a plain SSE frame (`event: task-card-moved`, `data: <payload JSON>`), same shape as `user-registered` today.

### 5. `moveCard()` publishes inline, after `save`, inside the existing `@Transactional` method
Matches the existing precedent in this codebase (`AppUserService`, `MembershipInvitationIssuer`, `OrcamentoService` all call `EventPublisher.publish(...)` inline, with no `TransactionSynchronization.afterCommit` wrapping). Accepts the same small, already-accepted risk as every other domain event in this codebase: a publish could in theory fire moments before a surrounding commit fails. Introducing transactional-outbox semantics here would be a bigger, cross-cutting change out of scope for this proposal.

### 6. SSE heartbeat
`SseBroadcaster` gains a `@Scheduled(fixedRate = 20_000)` sweep that sends an SSE comment frame (`SseEmitter.event().comment("keep-alive")`) to every emitter in `allEmitters`. `@EnableScheduling` is added to `PantheonServiceApplication`. 20s is comfortably under typical LB/proxy idle-connection timeouts (e.g. AWS ALB's default 60s).

### 7. `pantheon-web`: `useSse.ts` gains an event-handler callback; board composable patches state in place
`useSse(eventNames, { onEvent })` gains an optional `onEvent(eventName, parsedData)` callback (in addition to the existing log-only `entries`) so callers other than the debug `EventLog.vue` panel can react to events, not just display them. `TasksBoardPanel.vue` calls `useSse(['task-card-moved'], { onEvent })` and, on receipt, if `payload.constructionSiteId === props.siteId`, finds the card by id in `board.value.cards` and updates its `columnId`/`sortOrder` in place (no refetch). If the card id isn't found (board hasn't loaded it yet, or it's a race with the initial `load()`), the event is ignored — the next `load()` will have it.

Alternative considered: trigger a full `load()` on every event instead of patching in place. Rejected as unnecessarily chatty (a full board + site-members + labels refetch per move, for every connected client, every time anyone moves a card) and visually flickery compared to an in-place patch.

`TasksBoardPanel.vue` opens its own SSE connection (same pattern `EventLog.vue` already uses — `useSse` is called per-consumer, not a shared singleton). Two simultaneous SSE connections from the same tab (if a user somehow has both the debug event log and a task board open) is an accepted, pre-existing characteristic of `useSse`'s design, not something this change needs to fix.

## Risks / Trade-offs

- **[Risk] A connection open before a membership change won't receive events for a newly-joined company until it reconnects.** → Mitigation: `useSse.ts` already reconnects with exponential backoff on any drop, and pod restarts/deploys naturally recycle connections; acceptable for a rare edge case, documented rather than engineered around.
- **[Risk] Publish-then-rollback dual-write (see Decision 5).** → Mitigation: none added beyond existing codebase precedent; a spurious extra `task-card-moved` event on a rolled-back move is a rare, low-consequence UI glitch (the next `load()` on any client corrects it), not a data-integrity issue.
- **[Risk] Every pod receives every SSE-worthy event even if it has zero locally-connected clients for that company.** → Mitigation: acceptable at current and near-term scale (human-driven move actions, not a high-frequency stream); the design note in "Non-Goals" flags dynamic per-company queue bindings as the next optimization step if this ever becomes measurable overhead.
- **[Trade-off] Two SSE connections per browser tab if a user has both `EventLog.vue` and a task board mounted simultaneously.** → Accepted; `EventLog.vue` is a debug panel, not a normal end-user surface.

## Migration Plan

1. Ship the `SseBroadcaster`/RabbitMQ/`TaskCardService` changes with `replicas` still at `1` — functionally identical to today for a single pod, but now exercised end-to-end (each pod's own fanout queue receives its own publishes).
2. Verify locally via `infra/docker-compose.yml` (already runs RabbitMQ) that a card move triggers `task-card-moved` on a second browser session.
3. Raise `infra/k8s/pantheon-service.yaml` `replicas` to `2` and update its comment to reflect the new fan-out backplane, once the above is verified.
4. Rollback: revert the replica count to `1` if problems surface; the RabbitMQ/broadcaster changes themselves are backward-compatible at `replicas: 1` and don't need to be reverted to roll back the scale-out.

## Open Questions

None outstanding — scope, event contract, and fan-out mechanism are settled by the decisions above.
