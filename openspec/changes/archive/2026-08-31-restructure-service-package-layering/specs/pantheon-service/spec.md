## ADDED Requirements

### Requirement: Java package and build coordinate convention
`pantheon-service` SHALL use the `com.pantheon` Maven groupId and `com.pantheon.service` Java package prefix, and SHALL organize its Java source into layered packages (`controller`, `service`, `repository`, `entity`, `dto`, `exception`) rather than per-feature packages. Cross-cutting infrastructure (`security`, `messaging`, `sse`) is exempt from this layering and may remain organized by concern.

#### Scenario: Package prefix
- **WHEN** inspecting the package declaration of any Java source file under `pantheon-service`'s application code
- **THEN** the declaration starts with `com.pantheon.service`

#### Scenario: Layered organization
- **WHEN** inspecting the top-level packages under `com.pantheon.service`
- **THEN** Spring MVC controllers live in `controller`, business logic in `service`, Spring Data repositories in `repository`, JPA-mapped types (entities and their enums) in `entity`, request/response payload types in `dto`, and exception types together with their `@RestControllerAdvice` handlers in `exception`
