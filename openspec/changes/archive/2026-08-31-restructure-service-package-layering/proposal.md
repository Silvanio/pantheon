## Why

`pantheon-service`, `pantheon-message`, and their shared parent POM currently use the `dev.pantheon` groupId/package prefix, while the intended long-term convention is `com.pantheon`. Separately, `pantheon-service` is organized by feature (`auth/`, `project/`, `user/`, ...), which mixes controllers, services, repositories, entities, DTOs, and exception handlers in the same package. Standardizing on `com.pantheon` and splitting `pantheon-service` into conventional layers (`controller/`, `service/`, `repository/`, `entity/`, `dto/`, `exception/`) makes the module's structure predictable and consistent with common Spring Boot conventions before the codebase grows further.

## What Changes

- Rename the Maven groupId from `dev.pantheon` to `com.pantheon` in `infra/build/pantheon-parent/pom.xml`, `pantheon-service/pom.xml`, and `pantheon-message/pom.xml` (including the `<parent>` groupId references). **BREAKING** (Maven coordinates change for both artifacts).
- Move all Java sources in `pantheon-service` from package `dev.pantheon.service.*` to `com.pantheon.service.*`, updating package declarations, imports, and directory layout (`src/main/java/dev/pantheon/service` → `src/main/java/com/pantheon/service`, mirrored under `src/test/java`).
- Move all Java sources in `pantheon-message` from package `dev.pantheon.message.*` to `com.pantheon.message.*`, updating package declarations, imports, and directory layout, mirrored under `src/test/java`. `pantheon-message`'s existing internal structure (`domain/`, `messaging/`, `security/`, `web/`) is otherwise unchanged.
- Update any hardcoded `dev.pantheon` references in `application.yml`/`application.properties` (e.g. logging levels) in both modules.
- Reorganize `pantheon-service`'s `com.pantheon.service` package from feature-based packages (`auth/`, `project/`, `user/`) into layered packages:
  - `controller/`: `AuthController`, `ProjectController`, `OnboardingController`, `SseController`
  - `service/`: `AppUserService`, `UserProfileService`, `ProjectService`
  - `repository/`: `AppUserRepository`, `UserProfileRepository`, `ProjectRepository`, `ProjectMembershipRepository`
  - `entity/`: `AppUser`, `UserProfile`, `Project`, `ProjectMembership`, `Plan`, `ProjectRole`
  - `dto/`: `AuthResponse`, `LoginRequest`, `RegisterRequest`, `UserResponse`, `ActiveProjectResponse`, `AddMemberRequest`, `OnboardingStatusResponse`, `PlanSelectionRequest`, `ProjectMembershipResponse`, `ProjectRegistrationRequest`, `ProjectResponse`
  - `exception/`: `AuthExceptionHandler`, `EmailAlreadyRegisteredException`, `ProjectExceptionHandler`, `MemberNotFoundException`, `NotProjectAdminException`, `ProjectLimitExceededException`, `ProjectNotFoundException`
  - `security/`, `messaging/`, and `sse/` (containing only `SseBroadcaster`) remain unchanged as cross-cutting infrastructure packages, not folded into the layered scheme.
- Update corresponding test packages: `ProjectServiceTest` and `UserProfileServiceTest` move under `.../service/` alongside the other tests.
- No REST endpoints, request/response payload shapes, database schema, or business logic change as part of this refactor — it is a purely mechanical rename and reorganization.

## Capabilities

### New Capabilities
(none)

### Modified Capabilities
- `pantheon-service`: adds a requirement documenting the `com.pantheon` package prefix and the layered package convention (`controller`/`service`/`repository`/`entity`/`dto`/`exception`) as the module's code organization standard. No existing behavioral requirement changes.
- `pantheon-message`: adds a requirement documenting the `com.pantheon` package prefix as the module's Java package convention. No existing behavioral requirement changes.

## Impact

- **Affected code**: All Java source and test files in `pantheon-service` and `pantheon-message` (package declarations, imports, directory paths); `infra/build/pantheon-parent/pom.xml`, `pantheon-service/pom.xml`, `pantheon-message/pom.xml` (groupId); `application.yml`/`application.properties` in both modules if they reference the old package prefix.
- **Not affected**: `pantheon-web` (no Java package concept), REST API contracts, database schema/migrations, RabbitMQ message contracts, business logic and behavior.
- **Build/tooling**: Any IDE run configurations, Docker build contexts, or CI steps that reference `dev.pantheon` artifact coordinates or main class names (e.g. `dev.pantheon.service.PantheonServiceApplication`) need to be checked for the new `com.pantheon` coordinates.
