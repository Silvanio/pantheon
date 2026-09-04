## 1. Groupwide Maven coordinate rename

- [x] 1.1 Update `groupId` from `dev.pantheon` to `com.pantheon` in `infra/build/pantheon-parent/pom.xml`
- [x] 1.2 Update `groupId` and `<parent>` groupId reference from `dev.pantheon` to `com.pantheon` in `pantheon-service/pom.xml`
- [x] 1.3 Update `groupId` and `<parent>` groupId reference from `dev.pantheon` to `com.pantheon` in `pantheon-message/pom.xml`
- [x] 1.4 Grep the repo (excluding `target/`, `dist/`, `node_modules/`) for remaining `dev.pantheon` references in non-Java config (Docker/K8s manifests, CI workflows) and update any that reference the Maven coordinates or main-class names

## 2. Rename pantheon-message package prefix

- [x] 2.1 Move `pantheon-message/src/main/java/dev/pantheon/message/**` to `pantheon-message/src/main/java/com/pantheon/message/**`, updating every file's `package` declaration and internal imports (`PantheonMessageApplication`, `domain/`, `messaging/`, `security/`, `web/` keep their existing subpackages)
- [x] 2.2 Move `pantheon-message/src/test/java/dev/pantheon/message/**` to `pantheon-message/src/test/java/com/pantheon/message/**`, updating `package` declarations and imports
- [x] 2.3 Check `pantheon-message`'s `application.yml`/`application.properties` for hardcoded `dev.pantheon` references (e.g. `logging.level`) and update to `com.pantheon`
- [x] 2.4 Build and test `pantheon-message` (`mvn -pl pantheon-message compile test`) and confirm it passes

## 3. Rename pantheon-service package prefix

- [x] 3.1 Move `pantheon-service/src/main/java/dev/pantheon/service/**` to `pantheon-service/src/main/java/com/pantheon/service/**`, updating every file's `package` declaration and internal imports, keeping the existing `auth/`, `messaging/`, `project/`, `security/`, `sse/`, `user/` feature subpackages unchanged for this step
- [x] 3.2 Move `pantheon-service/src/test/java/dev/pantheon/service/**` to `pantheon-service/src/test/java/com/pantheon/service/**`, updating `package` declarations and imports
- [x] 3.3 Check `pantheon-service`'s `application.yml`/`application.properties` for hardcoded `dev.pantheon` references and update to `com.pantheon`
- [x] 3.4 Build and test `pantheon-service` (`mvn -pl pantheon-service compile test`) and confirm it passes (also fixed a pre-existing Mockito `UnnecessaryStubbing` issue in `ProjectServiceTest` unrelated to the rename, using `lenient()` for shared `setUp()` stubs)

## 4. Reorganize pantheon-service into layered packages

- [x] 4.1 Create `com.pantheon.service.controller`, `.service`, `.repository`, `.entity`, `.dto`, `.exception` packages
- [x] 4.2 Move `auth/AuthController` → `controller/`; `auth/AuthResponse`, `LoginRequest`, `RegisterRequest`, `UserResponse` → `dto/`; `auth/AuthExceptionHandler` → `exception/`; update package declarations and all referencing imports
- [x] 4.3 Move `project/ProjectController`, `project/OnboardingController` → `controller/`; `project/ProjectService` → `service/`; `project/ProjectRepository`, `project/ProjectMembershipRepository` → `repository/`; `project/Project`, `ProjectMembership`, `Plan`, `ProjectRole` → `entity/`; `project/ActiveProjectResponse`, `AddMemberRequest`, `OnboardingStatusResponse`, `PlanSelectionRequest`, `ProjectMembershipResponse`, `ProjectRegistrationRequest`, `ProjectResponse` → `dto/`; `project/ProjectExceptionHandler`, `MemberNotFoundException`, `NotProjectAdminException`, `ProjectLimitExceededException`, `ProjectNotFoundException` → `exception/`; update package declarations and all referencing imports
- [x] 4.4 Move `user/AppUserService`, `user/UserProfileService` → `service/`; `user/AppUserRepository`, `user/UserProfileRepository` → `repository/`; `user/AppUser`, `user/UserProfile` → `entity/`; `user/EmailAlreadyRegisteredException` → `exception/`; update package declarations and all referencing imports (including `security/` classes that reference `AppUser`)
- [x] 4.5 Move `sse/SseController` → `controller/`; leave `sse/SseBroadcaster` in place; update package declarations and all referencing imports
- [x] 4.6 Confirm `security/` and `messaging/` packages are untouched (no file moves), and update any of their imports that reference moved `entity`/`service` classes (e.g. `AppUser`, `AppUserService`)
- [x] 4.7 Move `test/.../project/ProjectServiceTest.java` and `test/.../user/UserProfileServiceTest.java` to `test/.../service/`, updating package declarations and imports; leave `PantheonServiceApplicationTests.java` at the root test package
- [x] 4.8 Delete the now-empty `auth/`, `project/`, `user/` directories under both `src/main/java` and `src/test/java`
- [x] 4.9 Build and test `pantheon-service` (`mvn -pl pantheon-service compile test`) and confirm it passes (also had to add explicit imports for references that were previously implicit same-package usages before the split, e.g. `ProjectService` needing new imports for `Project`, `ProjectMembership`, `ProjectRole`, etc.)

## 5. Final verification

- [x] 5.1 Repo-wide grep for `dev.pantheon` and `dev/pantheon` (excluding `target/`, `dist/`, `node_modules/`, `.git/`) and confirm zero remaining matches (only a historical, inert `.claude/settings.local.json` Bash permission allowlist entry remains, which is not project code)
- [x] 5.2 Full build of both modules (`mvn -pl pantheon-service,pantheon-message compile test`) and confirm both pass
- [x] 5.3 Manually skim `pantheon-service`'s new `controller/`, `service/`, `repository/`, `entity/`, `dto/`, `exception/` packages against the mapping in `proposal.md` to confirm every class landed where intended
