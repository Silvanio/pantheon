## 1. RabbitMQ fan-out backplane

- [x] 1.1 Add `SseRabbitConfig` (`pantheon-service/.../sse/`) declaring the `pantheon.sse.events` `FanoutExchange` (durable, non-auto-delete exchange; the per-pod queues bound to it are the ephemeral part).
- [x] 1.2 Add `SseEventEnvelope` record (`eventName`, nullable `companyId`, `payload`) for messages published to that exchange.
- [x] 1.3 Add `SseEventPublisher` (mirrors `EventPublisher`, targets `pantheon.sse.events` via `RabbitTemplate`) with `publishToCompany(UUID companyId, String eventName, Object payload)` and `publishGlobal(String eventName, Object payload)`.
- [x] 1.4 Add `SseEventListener` with an anonymous exclusive/auto-delete queue bound to `pantheon.sse.events` (`@RabbitListener(bindings = @QueueBinding(...))`), calling `SseBroadcaster.broadcastToCompany(...)` when `companyId` is present or `SseBroadcaster.broadcast(...)` when it's null.

## 2. `SseBroadcaster` company-scoping and heartbeat

- [x] 2.1 Change `SseBroadcaster` to hold `Set<SseEmitter> allEmitters` plus `Map<UUID, Set<SseEmitter>> emittersByCompany`; change `subscribe()` to `subscribe(Set<UUID> companyIds)`, registering the emitter in `allEmitters` and every relevant company bucket, with cleanup on completion/timeout/error removing it from all of them.
- [x] 2.2 Add `broadcastToCompany(UUID companyId, String eventName, Object data)`, iterating only that company's bucket; keep `broadcast(eventName, data)` iterating `allEmitters` for unscoped events.
- [x] 2.3 Add a `@Scheduled(fixedRate = 20_000)` heartbeat sweep sending `SseEmitter.event().comment("keep-alive")` to every emitter in `allEmitters`; add `@EnableScheduling` to `PantheonServiceApplication`.
- [x] 2.4 Update `SseController.subscribe()` to resolve the caller's active company ids (`CompanyMembershipRepository.findByUserId`, filtered to `isActive()`, mapped to `getCompanyId()`) and pass them to `SseBroadcaster.subscribe(...)`.

## 3. Card-move event

- [x] 3.1 In `TaskCardService.moveCard()`, after `cardRepository.save(card)`, build the `task-card-moved` payload (`cardId`, `constructionSiteId`, `companyId` from `site.getCompanyId()`, `columnId`, `sortOrder`, `movedBy` = `actingUserId`, `movedAt` = now) and call `SseEventPublisher.publishToCompany(...)`.
- [x] 3.2 Migrate `AppUserService.publishUserRegistered` from calling `SseBroadcaster.broadcast(...)` directly to calling `SseEventPublisher.publishGlobal(...)`, so it also fans out across pods; remove the now-unused direct `SseBroadcaster` dependency from `AppUserService` if nothing else in that class uses it.

## 4. `pantheon-web`

- [x] 4.1 Extend `useSse.ts` to accept an optional `onEvent(eventName, data)` callback (parsing each event's JSON payload) alongside the existing log-only `entries`, without changing `EventLog.vue`'s current behavior.
- [x] 4.2 In `TasksBoardPanel.vue`, subscribe via `useSse(['task-card-moved'], { onEvent })`; on receipt, if `payload.constructionSiteId === props.siteId`, find the card by `cardId` in `board.value.cards` and update its `columnId`/`sortOrder` in place; otherwise ignore the event. Connect on mount, disconnect on unmount (matching `EventLog.vue`'s pattern).

## 5. Infra

- [x] 5.1 Raise `infra/k8s/pantheon-service.yaml` `replicas` from `1` to `2` and replace the comment explaining the old single-replica constraint with one describing the new RabbitMQ-backed fan-out.
- [x] 5.2 Add a short note (README or a comment in the k8s manifest) that any reverse proxy placed in front of `pantheon-service`'s SSE endpoint must disable response buffering (e.g. `proxy_buffering off` for nginx) — no proxy/ingress manifest exists in this repo today, so this is documentation only.

## 6. Verification

- [x] 6.1 `cd pantheon-service && mvn test` (or the project's usual command) covering `SseBroadcasterTest`/`TaskCardServiceTest` additions for company-scoped delivery. Ran the full suite (`./mvnw -o test`): 100/100 tests pass, including the full-context `PantheonServiceApplicationTests`, which confirmed the new `pantheon.sse.events` exchange and per-pod anonymous queue bind successfully against a real RabbitMQ broker. `pantheon-web`'s `npm run build` (vue-tsc + vite build) also passes.
- [ ] 6.2 Manual check via `infra/docker-compose.yml`: open the same obra's Tasks board in two browser sessions as users from the same company, move a card in one, confirm it moves live in the other without reload; confirm a session logged into a different company does not receive the event. **Not run in this session** — needs two distinct authenticated browser sessions (two different users/companies), which isn't practical to simulate reliably from a single automated browser context; left for manual verification before merging.
