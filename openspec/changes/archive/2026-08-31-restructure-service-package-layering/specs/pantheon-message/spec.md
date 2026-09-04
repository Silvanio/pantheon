## ADDED Requirements

### Requirement: Java package and build coordinate convention
`pantheon-message` SHALL use the `com.pantheon` Maven groupId and `com.pantheon.message` Java package prefix, consistent with `pantheon-service` and the shared parent POM.

#### Scenario: Package prefix
- **WHEN** inspecting the package declaration of any Java source file under `pantheon-message`
- **THEN** the declaration starts with `com.pantheon.message`
