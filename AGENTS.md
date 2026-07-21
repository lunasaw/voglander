# Repository Guidelines

## Project Structure & Module Organization

Voglander is a Java 17/Spring Boot multi-module platform. `voglander-web` owns HTTP/SSE entry points; `voglander-service` domain and live-stream orchestration; `voglander-manager` templates, event sharding, coordination, and node routing; `voglander-repository` persistence/cache. External adapters belong in `voglander-integration`; shared contracts/utilities in `voglander-client` and `voglander-common`.

Use `src/main/java` for code and `src/main/resources` for configuration. Place all tests under `voglander-web/src/test/java/io/github/lunasaw/voglander`. Database scripts live in `sql/`; documentation in `doc/` and `openspec/`.

## Build, Test, and Development Commands

- `mvn clean compile` — compile every module.
- `mvn spring-boot:run -pl voglander-web` — run `ApplicationWeb` locally.
- `mvn test` — run the complete JUnit 5 test suite.
- `mvn test -Dtest=DeviceManagerTest` — run one test class; append `#methodName` for one method.
- `./generate-coverage-report.sh` — generate aggregate JaCoCo coverage.

SQLite is the local default. Redis tests skip when Redis is unavailable.

## Architecture & Development Workflow

Read `doc/architecture/current/ARCHITECTURE-OVERVIEW.md` before structural changes. Keep Web limited to validation/model conversion, Integration to external adaptation, and persistence in Repository. Extend protocols through `ProtocolEventHandler` (inbound), `DeviceCommandService` (outbound), or `MediaProtocolHandler` (media). Preserve sharded event ordering, `callId`-centred SIP state, Manager cache invalidation, and event-driven SSE.

Follow `doc/DEVELOPMENT_WORKFLOW.md` for features: research code and impact; present **Check 关键点** and await confirmation; design and self-review; implement bottom-up with red-green-refactor TDD; run acceptance gates; report changes, test evidence, risks, and remaining work. After three failed attempts at one issue, reassess the approach.

## Coding Style & Naming Conventions

Use four-space indentation, Java 17, `jakarta.*`, Lombok, and FastJSON2—not Jackson/Gson. Packages begin with `io.github.lunasaw.voglander`. Name models `*DO`, `*DTO`, `*Req`, and `*VO`. Controllers convert through assemblers; Manager APIs expose DTOs, never DOs. Build MyBatis-Plus queries with conditional lambda chains. Use `LocalDateTime` internally and Unix-millisecond web values.

## Testing Guidelines

Name tests `*Test`. Use JUnit 5 and Mockito for controller/service unit tests. Manager/repository tests use `@SpringBootTest` with `BaseTest`; asynchronous and HTTP tests require explicit cleanup, not `@Transactional`. Cover behavior changes and isolate fixtures with unique keys.

## Commit & Pull Request Guidelines

Use Conventional Commits: `feat(database): add PostgreSQL support`, `fix(scope): ...`, or `docs: ...`. PRs should explain the problem and solution, list verification, link issues/specifications, and include UI screenshots. Highlight schema, configuration, protocol, or compatibility impacts.

## Security & Protocol Compliance

Never commit credentials, generated databases, or local overrides. Keep secrets in environment-specific configuration. GB28181, SIP, and ONVIF changes must follow their standards; do not infer identifiers through string prefixes or reuse lab-only conventions in production paths.
