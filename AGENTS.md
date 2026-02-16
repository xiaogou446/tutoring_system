# AGENTS.md

Guidance for coding agents working in this repository.

## 0) Language policy

- All assistant-user conversations must be in Chinese.
- All newly written project documentation must be in Chinese (keep code symbols, API names, and third-party proper nouns in original form where needed).
- If existing files are in English, append or update content in Chinese unless external constraints require English.

## 1) Project shape

- Build tool: Maven multi-module project.
- Java version: 17.
- Parent POM: `pom.xml`.
- Modules (reactor order): `facade`, `infrastructure`, `service`, `bootstrap`, `test`.
- Runtime app module: `bootstrap` (Spring Boot).
- Main entrypoint: `bootstrap/src/main/java/com/lin/webtemplate/WebTemplateApplication.java`.
- Web/controller code currently lives in `service` and is picked up by component scanning.
- Tests currently live in `bootstrap/src/test/java`.
- Local Maven repo is redirected to workspace by `.mvn/maven.config`.

## 2) Canonical commands

Run commands from repository root unless noted.

### Build

- Full compile/package (skip tests):
  - `mvn clean package -DskipTests`
- Full verification (includes tests):
  - `mvn clean verify`
- Build only one module plus required upstream modules:
  - `mvn -pl bootstrap -am package -DskipTests`

### Test

- Run all tests in all modules:
  - `mvn test`
- Run tests only in one module:
  - `mvn -pl bootstrap -am test -Dsurefire.failIfNoSpecifiedTests=false`
- Run a single test class:
  - `mvn -pl bootstrap -am -Dtest=HeartbeatControllerTest test -Dsurefire.failIfNoSpecifiedTests=false`
- Run a single test method (preferred pattern):
  - `mvn -pl bootstrap -am -Dtest=HeartbeatControllerTest#actuatorHealth_shouldBeAvailable test -Dsurefire.failIfNoSpecifiedTests=false`
- If Maven says no matching tests in other modules, keep `-Dsurefire.failIfNoSpecifiedTests=false`.

### Lint / formatting / static analysis

- There is no dedicated linter/formatter plugin configured (no Checkstyle/Spotless/PMD in POMs).
- Use compilation + tests as quality gates:
  - `mvn clean verify`
- If you add a formatter or lint tool, document commands here and in the parent `pom.xml`.

### Run app

- Run Spring Boot app from root:
  - `mvn -pl bootstrap -am spring-boot:run`
- Default port is `8081` (see `bootstrap/src/main/resources/application.properties`).

## 3) Known current behavior

- `HeartbeatController` is mapped at `/health/heartbeat` via class + method mappings.
- Existing test `heartbeat_shouldReturnResultWrapper` currently calls `/heartbeat` and fails with 404.
- Do not silently “fix around” this mismatch; align test and API intentionally in the same change.

## 4) Code style and conventions

Follow existing Spring Boot + Java 17 conventions in this repo.

### General formatting

- Use 4-space indentation, no tabs.
- Keep one top-level public class per file.
- Keep files UTF-8.
- Use trailing newline at end of file.
- Keep methods focused and short; extract helper methods before adding complex branching.

### Required comments and spacing

- Generated code must include comments; do not leave key logic completely uncommented.
- Every new class must have a class-level Javadoc block (about 5-6 lines), using this template:
  - `/**`
  - ` * 功能：<一句话描述该类职责>`
  - ` *`
  - ` * @author linyi`
  - ` * @since <YYYY-MM-DD，当天日期>`
  - ` */`
- Add concise comments for non-obvious or critical logic inside methods (validation branches, business rules, boundary handling, external calls).
- Keep comments accurate and maintainable; update comments together with logic changes.
- Leave one blank line between field/property declarations in classes for readability.

### Imports

- Prefer explicit imports; avoid wildcard imports.
- Group order:
  1. `java.*` / `javax.*`
  2. third-party (`org.*`, `lombok.*`, etc.)
  3. project (`com.lin.*`)
- Separate groups with a blank line.
- Remove unused imports.
- Use static imports only when they materially improve readability (common in tests).

### Types and models

- Prefer concrete, domain-relevant types over `Object` or raw types.
- Keep generics explicit at boundaries (e.g., `Result<HeartbeatData>`).
- This project currently prefers regular classes with Lombok over Java records for response wrappers.
- Use `final` for fields that should not change after construction.
- Favor constructor injection for Spring components.

### Naming

- Packages: lowercase dot-separated (`com.lin.webtemplate...`).
- Classes: PascalCase (`HeartbeatController`).
- Methods/fields: camelCase (`heartbeat_shouldReturnResultWrapper` only for test names).
- Constants: UPPER_SNAKE_CASE.
- Test method names: behavior-focused; `should` style is acceptable and already used.

### Spring and API patterns

- Use `@RestController` for JSON endpoints.
- Keep request mappings explicit and stable; avoid ambiguous overlapping paths.
- Prefer returning typed wrappers used by the project (`Result<T>`) when touching existing endpoints.
- If HTTP status remains 200 for domain-level failure, encode machine-readable error code/message in body consistently.
- Keep actuator and business endpoints separated by clear base paths.

### Error handling

- Do not swallow exceptions.
- Prefer explicit, meaningful error messages with stable codes.
- For recoverable/domain failures, return structured `Result.fail(code, message, data)`.
- For unexpected failures, use centralized exception handling (`@ControllerAdvice`) if introducing cross-cutting behavior.
- Avoid leaking secrets/internal details in error messages.

### Logging

- Use `@Slf4j` and structured, concise log messages.
- Log lifecycle milestones at `info` level.
- Log actionable diagnostics at `warn`/`error` with context.
- Do not log sensitive configuration or credentials.

### Testing expectations

- Use JUnit 5 + Spring Boot Test (`spring-boot-starter-test`).
- For web endpoints, prefer `MockMvc` tests as in existing tests.
- Assert both HTTP layer and JSON payload contract.
- Disable environment-fragile health indicators in tests when needed (example: disk space health check).
- When fixing a bug, add/adjust a test that fails before and passes after.

## 5) Dependency and module rules

- Keep module boundaries clean:
  - `bootstrap` depends on `service`.
  - `service` depends on `infrastructure`.
  - `infrastructure` depends on `facade`.
- Add dependencies in the narrowest module possible.
- Prefer managing versions in parent POM `dependencyManagement`.
- Keep Lombok usage consistent with existing code; do not mix many competing boilerplate patterns in same package.

## 6) Agent workflow guidance

- Before edits, inspect affected module POM and nearby classes/tests.
- After edits, run targeted tests first, then broader verification if feasible.
- Keep diffs small and intention-revealing.
- If behavior and tests disagree, update both coherently and explain rationale.
- Do not modify `.mvn/maven.config` unless intentionally changing local repository strategy.

## 7) Cursor/Copilot rules check

- Checked for Cursor rules:
  - `.cursorrules`
  - `.cursor/rules/`
- Checked for Copilot instructions:
  - `.github/copilot-instructions.md`
- Result: none of these files/directories exist in this repository right now.
- If added later, mirror their key constraints into this document.
