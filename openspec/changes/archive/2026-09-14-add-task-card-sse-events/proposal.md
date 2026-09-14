## Why

When a `TaskCard` is moved between columns, other users with the same obra's (or global) Tasks board open have no way to see the change until they manually reload. The existing SSE infrastructure (`SseBroadcaster`, `/api/sse/subscribe`, `useSse.ts`) broadcasts to every connected client with no tenant filtering, and is pinned to a single `pantheon-service` replica (`infra/k8s/pantheon-service.yaml` is fixed at `replicas: 1`) because emitter state lives only in that pod's memory — a second replica would silently drop events for clients connected to a different pod. This change makes card-move events real-time and company-scoped, and makes the SSE stream safe to run behind a load balancer with multiple `pantheon-service` replicas.

## What Changes

- Publish a `task-card-moved` domain event (with `companyId`, `constructionSiteId`, `cardId`, target column and sort order) whenever `TaskCardService.moveCard()` persists a move.
- **BREAKING (internal)**: Replace `SseBroadcaster`'s single global emitter list with a per-company index (`Map<companyId, Collection<SseEmitter>>`), so an emitter only ever receives events for the companies its owner belongs to. `broadcast(eventName, data)` is replaced by a company-scoped `broadcastToCompany(companyId, eventName, data)`; the one existing caller (`user-registered` in `AppUserService`) is updated to pass the acting user's company id(s).
- Introduce a dedicated RabbitMQ fanout exchange (`pantheon.sse.events`, distinct from the existing `pantheon.events` exchange) used purely to replicate SSE-worthy events to every `pantheon-service` instance. Each instance declares its own exclusive, auto-delete queue bound to that exchange at startup and rehydrates matching events into its local `SseBroadcaster`.
- Add an SSE heartbeat (periodic comment frame) on the `/api/sse/subscribe` connection so idle connections aren't closed by intermediary proxies/load balancers.
- `pantheon-web`'s task board composables subscribe to `task-card-moved` via the existing `useSse.ts` and apply the move to local board state without a full refetch.
- Raise `pantheon-service`'s `infra/k8s/pantheon-service.yaml` `replicas` from `1` to `2`, now that SSE state is safe to run across multiple pods, and document the `proxy_buffering off` requirement for any reverse proxy sitting in front of the SSE endpoint.

## Capabilities

### New Capabilities
(none — this extends existing SSE and task-board capabilities rather than introducing a new domain capability)

### Modified Capabilities
- `pantheon-service`: the "Server-Sent Events stream" requirement gains company-scoped delivery and multi-instance (RabbitMQ-backed) fan-out so the stream behaves identically regardless of which replica a client or a triggering action lands on.
- `obra-tasks-board`: the "Card movement between columns" requirement gains a real-time broadcast to other company members watching the board.
- `pantheon-web`: the SSE client requirement gains a specific reaction to `task-card-moved` events, updating board state in place.

## Impact

- Affected code: `pantheon-service` (`sse/SseBroadcaster.java`, `sse/SseController.java`, `service/TaskCardService.java`, new RabbitMQ config/listener classes under `messaging`/`sse`), `pantheon-web` (`composables/useSse.ts`, task board composables that hold board state).
- Infra: new RabbitMQ exchange declaration (via Spring AMQP `@Bean`s, no manual broker setup needed beyond what docker-compose/K8s already provision), `infra/k8s/pantheon-service.yaml` replica count.
- No new external dependencies (no Redis); reuses the RabbitMQ broker already present in `infra/docker-compose.yml` and `infra/k8s`.
- No database schema changes.
