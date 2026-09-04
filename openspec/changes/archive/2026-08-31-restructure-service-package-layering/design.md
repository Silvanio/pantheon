## Context

`pantheon-service` and `pantheon-message` both currently live under Maven groupId `dev.pantheon` and Java package prefix `dev.pantheon.*`, inherited from a shared parent POM (`infra/build/pantheon-parent/pom.xml`). `pantheon-service` further organizes its own code by feature (`auth/`, `project/`, `security/`, `messaging/`, `sse/`, `user/`), so each feature package mixes controllers, services, repositories, JPA entities, DTOs, and exception handlers together (e.g. `project/` alone has 20 files spanning all of those roles).

This was scoped interactively (via `/opsx:explore`) with the repo owner, who confirmed:
1. The package layering should be **pure horizontal** (single top-level `controller/`, `service/`, `repository/`, `entity/`, `dto/`, `exception/` packages) rather than a per-feature hybrid where each domain keeps its own `controller/service/repository` subpackages.
2. The `dev.pantheon` → `com.pantheon` rename applies to **all** Java modules (`pantheon-service`, `pantheon-message`, and the shared parent POM), not just `pantheon-service`, to avoid inconsistent coordinates across the monorepo. `pantheon-web` has no Java package and is unaffected.
3. `SseBroadcaster` stays in `sse/` as cross-cutting infrastructure rather than moving into `service/`.

## Goals / Non-Goals

**Goals:**
- Standardize Maven groupId and Java package prefix on `com.pantheon` across `pantheon-service`, `pantheon-message`, and their shared parent POM.
- Reorganize `pantheon-service` into conventional layered packages (`controller/`, `service/`, `repository/`, `entity/`, `dto/`, `exception/`), dissolving the `auth/`, `project/`, and `user/` feature packages.
- Preserve `security/` and `messaging/` as unchanged cross-cutting infrastructure packages in both modules, and keep `sse/` scoped to `SseBroadcaster` only (with `SseController` moving to `controller/`).
- Keep the change purely mechanical: zero changes to REST contracts, DTO shapes, persisted schema, RabbitMQ message contracts, or business logic.

**Non-Goals:**
- Not introducing a per-domain hybrid structure (e.g. `project/controller`, `project/service`) — explicitly rejected in favor of flat layering.
- Not restructuring `pantheon-message`'s internal packages (`domain/`, `messaging/`, `security/`, `web/`) — only its coordinate prefix changes.
- Not introducing a new `AuthService` class — `AuthController` continues to wire `AppUserService` and `JwtService` directly, matching current behavior.
- Not changing any `openspec/specs/pantheon-service/spec.md` or `openspec/specs/pantheon-message/spec.md` requirement — no delta specs are produced by this change.

## Decisions

**Flat layering over per-feature hybrid.** A flat `controller/service/repository/entity/dto/exception` scheme was chosen over keeping `project/` as a cohesive feature folder with its own sublayers. Trade-off: the `project` domain's ~20 files scatter across 6 packages instead of staying together, but this is what was explicitly requested and keeps the convention uniform and simple to reason about (import path tells you the class's role, not its feature).

**`security/`, `messaging/`, `sse/` remain outside the layered scheme.** These are cross-cutting/infrastructure concerns (Spring Security config, RabbitMQ publishing, SSE connection management), not request-handling layers tied to a specific domain flow. Folding them into `controller/service/repository` would misrepresent their role (e.g. `JwtAuthenticationFilter` is not a "controller", `RabbitMqConfig` is not a "repository").

**Enums (`Plan`, `ProjectRole`) go to `entity/`.** They are part of the persisted domain model (used as JPA enum-typed fields), not request/response shapes, so they belong with `AppUser`/`Project`/`ProjectMembership` rather than `dto/`.

**`@RestControllerAdvice` classes go to `exception/`.** `AuthExceptionHandler` and `ProjectExceptionHandler` map exceptions to HTTP responses; grouping them with the exceptions they handle keeps exception-to-response mapping discoverable in one place rather than splitting it into a separate `advice/` package.

**Rename is repo-wide across Java modules, not `pantheon-service`-only.** Since `pantheon-service` and `pantheon-message` share a parent POM, changing only one module's groupId would leave the monorepo with mixed `dev.pantheon`/`com.pantheon` coordinates. Renaming both together (plus the parent POM) keeps Maven coordinates consistent.

**Directory moves, not `mvn` package-refactor tooling.** Since this environment doesn't have IDE-level automated refactoring, the plan is a manual/scripted `git mv` + package-declaration + import-statement update per file, verified by a full build (`mvn -pl pantheon-service,pantheon-message compile test`) at the end of each module's rename.

## Risks / Trade-offs

- **[Risk] Large, mechanical diff touching nearly every file in `pantheon-service` and `pantheon-message`** → Mitigation: sequence the work as (1) groupId/package rename first, verified green, then (2) layering reorganization second, verified green — two independently buildable checkpoints instead of one giant unverified diff.
- **[Risk] Missed import after a class moves package** → Mitigation: rely on the compiler (`mvn compile`) to surface unresolved imports; do not consider a step done until both modules compile and existing tests (`ProjectServiceTest`, `UserProfileServiceTest`, `PantheonServiceApplicationTests`, `PantheonMessageApplicationTests`) pass.
- **[Risk] Hardcoded `dev.pantheon` string references outside `.java`/`pom.xml`** (e.g. `logging.level.dev.pantheon.service` in `application.yml`, Docker/K8s manifests referencing the old main-class name, CI workflow steps) → Mitigation: repo-wide grep for `dev.pantheon` after the Java/POM changes to catch stragglers before considering the change done.
- **[Risk] `project/` domain cohesion is lost, making future project-related changes touch more packages** → Accepted trade-off per explicit decision; not something this change re-litigates.
- **[Trade-off] `pantheon-message` gets a coordinate rename but no structural reorg**, while `pantheon-service` gets both → intentional asymmetry: the user only asked for `pantheon-service`'s internal layering to change.

## Migration Plan

1. Rename groupId in `infra/build/pantheon-parent/pom.xml`, then in `pantheon-service/pom.xml` and `pantheon-message/pom.xml` (including `<parent>` references).
2. Move `pantheon-message` Java sources from `dev/pantheon/message` to `com/pantheon/message` (main + test), updating package declarations and imports; grep for stray `dev.pantheon` in `application.yml`/`application.properties`; build and test.
3. Move `pantheon-service` Java sources from `dev/pantheon/service` to `com/pantheon/service` (main + test) as a straight prefix rename first (still feature-organized under the new prefix), updating package declarations and imports; grep for stray `dev.pantheon`; build and test.
4. Reorganize `com.pantheon.service` into `controller/`, `service/`, `repository/`, `entity/`, `dto/`, `exception/` per the mapping in the proposal, dissolving `auth/`, `project/`, `user/`; leave `security/`, `messaging/`, `sse/` in place; move test files accordingly; build and test.
5. Final repo-wide grep for `dev.pantheon` and `groupId>dev.pantheon` to confirm nothing was missed (excluding build output directories).

No runtime/deployment migration is needed — this only affects source layout and build coordinates, not running artifacts' external behavior. Rollback is a plain `git revert` if a step fails to compile or breaks tests.

## Open Questions

None outstanding — layering scheme, rename scope, and `SseBroadcaster` placement were all confirmed during exploration.
