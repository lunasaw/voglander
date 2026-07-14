## 1. Baseline, Contracts and Acceptance Gates

- [ ] 1.1 Record current SQLite/MySQL/PostgreSQL `tb_export_task` row counts, API consumers and rollback backups before implementation
- [ ] 1.2 Add architecture tests that classify existing `@Scheduled` and protocol timers as explicitly outside business-task scope
- [ ] 1.3 Lock stable task type, mode, task state, execution state, event type, capability and failure code enums with parameterized transition tests
- [ ] 1.4 Define the 720000 business-task error-code range and test HTTP mappings without changing existing error behavior
- [ ] 1.5 Document module dependency placement and add a dependency test proving `voglander-client` task contracts do not depend on Repository, Web or domain handlers

## 2. Core SPI and Domain Contracts

- [ ] 2.1 Add `TaskConstant`, ID prefixes, Redis lock prefixes, SSE topics, permissions and default limits in `voglander-common`
- [ ] 2.2 Add task create, payload, capability, attempt, result, retry decision and result-reference models in `voglander-client`
- [ ] 2.3 Define `LongTaskHandler`, `LongTaskContext`, `TaskCompletionParticipant` and cancellation/heartbeat/progress/compensation contracts with JavaDoc invariants
- [ ] 2.4 Add abstract Handler contract tests for stable type, payload version, capability declaration, validation, retry classification and sanitized results
- [ ] 2.5 Implement Handler registry startup validation for duplicate type, blank type and unsupported payload version
- [ ] 2.6 Add registry tests proving an unknown task type is rejected before task or execution persistence

## 3. Three-Database Schema and Migration

- [ ] 3.1 Add failing schema tests for `tb_biz_task`, `tb_biz_task_execution`, `tb_biz_task_event`, all required fields, unique keys and indexes
- [ ] 3.2 Add equivalent business-task tables and indexes to MySQL full schema using native types and P3C naming
- [ ] 3.3 Add equivalent tables and indexes to SQLite full schema with valid nullable unique semantics
- [ ] 3.4 Add equivalent tables and indexes to PostgreSQL full schema using `BIGSERIAL`, `TIMESTAMP` and native index syntax
- [ ] 3.5 Add non-destructive core-table incremental migrations for MySQL, SQLite and PostgreSQL
- [ ] 3.6 Add migration history key and SQLite migrator tests for old databases, repeated startup, partial failure rollback and existing business-data preservation
- [ ] 3.7 Implement SQLite business-task migrator after full-schema initialization and fail fast when core schema verification fails
- [ ] 3.8 Add validation SQL for all three dialects and run fresh-schema plus incremental-schema tests

## 4. Repository Entities and Conditional SQL

- [ ] 4.1 Add `BizTaskDO`, `BizTaskExecutionDO` and `BizTaskEventDO` with `LocalDateTime`, `IdType.AUTO` and complete field parity
- [ ] 4.2 Add three Mapper interfaces and XML result maps without leaking domain payload parsing into Repository
- [ ] 4.3 Implement task due-scan, execution dispatch-scan, expired-lease scan and fixed allowlisted query SQL
- [ ] 4.4 Implement execution insert-if-absent and task cursor/version conditional updates
- [ ] 4.5 Implement task/execution conditional state transitions, claim, heartbeat, retry-wait and terminal updates
- [ ] 4.6 Implement append-only event insert and fixed task/execution timeline queries
- [ ] 4.7 Add Repository integration tests for all unique constraints, nullable idempotency keys and competing insert/update behavior

## 5. Manager DTO, Assemblers and Transactions

- [ ] 5.1 Add task, execution, event, query, progress, statistics and command DTOs under the existing Manager package convention
- [ ] 5.2 Add FastJSON2 Assemblers and tests for complete DO/DTO mapping, payload version and sanitized result/event summaries
- [ ] 5.3 Implement `BizTaskManager.create` transaction for idempotent task plus first execution creation
- [ ] 5.4 Implement task page/detail/statistics queries with access scope and deterministic sorting
- [ ] 5.5 Implement `BizTaskExecutionManager.insertIfAbsent`, page/detail, claim, heartbeat, retry-wait, missed and recovery operations
- [ ] 5.6 Implement `BizTaskEventManager` append/timeline APIs and prohibit update/delete methods
- [ ] 5.7 Implement task control Manager transitions for pause, resume, cancel and reschedule with full state-matrix tests
- [ ] 5.8 Implement completion participant plus execution terminal, task counter/progress/last-state and event single transaction
- [ ] 5.9 Add duplicate completion, duplicate event, optimistic conflict and rollback tests proving counters never double increment

## 6. Task Creation and Handler Registry Service

- [ ] 6.1 Add `BizTaskCreateService` internal API and reject creation from unregistered handlers
- [ ] 6.2 Implement owner/type/idempotency lookup before payload serialization and return the original accepted task
- [ ] 6.3 Validate payload size, version, prohibited sensitive fields and immutable serialization boundaries
- [ ] 6.4 Calculate ONCE, AT_TIME and inclusive FIXED_RATE counts with overflow, max-duration and max-count tests
- [ ] 6.5 Persist ONCE current-time execution, AT_TIME cursor and FIXED_RATE cursor according to schedule mode
- [ ] 6.6 Add domain-service integration test showing a trusted fake Handler can create a task while generic Web creation is absent

## 7. Scheduler and Missed-Point Reconciliation

- [ ] 7.1 Add injectable-clock tests for fixed cursor, no drift, inclusive end, allowed delay and schedule-version boundaries
- [ ] 7.2 Implement due-task batch scanner and per-task Redis schedule lock with transaction re-read
- [ ] 7.3 Implement per-point execution insert-if-absent and cursor CAS advance
- [ ] 7.4 Implement catch-up batches that persist expired points as MISSED without invoking Handlers
- [ ] 7.5 Add two-node concurrency test proving one execution fact per task/version/planned time
- [ ] 7.6 Add pause/resume tests proving cursor preservation and per-point MISSED reconciliation
- [ ] 7.7 Add natural terminal evaluation for completed, partial, failed and cancelled scheduled tasks

## 8. Dispatcher, Worker and Backpressure

- [ ] 8.1 Define a dedicated bounded business-task executor with validated core/max/queue settings and Abort rejection policy
- [ ] 8.2 Implement durable runnable execution scan and ID-only submission
- [ ] 8.3 Keep PENDING/RETRY_WAIT state unchanged when the executor queue is full and add saturation tests
- [ ] 8.4 Implement execution short lock and CAS claim with claim token, worker node, lease and attempt increment
- [ ] 8.5 Invoke the resolved Handler only after claim, run its completion participant inside CompletionManager transaction and reject task-type/handler mismatches as stable failures
- [ ] 8.6 Isolate Handler exceptions and prove scheduler/dispatcher/other task types remain alive
- [ ] 8.7 Add graceful shutdown tests for queued and running work without discarding durable facts

## 9. Progress, Heartbeat and Cancellation

- [ ] 9.1 Implement `LongTaskContext` progress validation for non-negative values, monotonic current/total and revision ordering
- [ ] 9.2 Implement configurable 500ms/1% progress persistence throttling with immediate terminal/phase writes
- [ ] 9.3 Mirror ONCE/AT_TIME progress and aggregate FIXED_RATE terminal-count plus active-execution progress
- [ ] 9.4 Implement lease heartbeat tied to the active claim token and reject stale-worker heartbeats
- [ ] 9.5 Implement cooperative cancellation token polling and Handler cancellation capability checks
- [ ] 9.6 Add concurrent progress, stale revision, total shrink, cancellation and database rollback tests

## 10. Retry and Lease Recovery

- [ ] 10.1 Implement retry decision normalization so unknown exceptions become sanitized non-sensitive failures
- [ ] 10.2 Implement bounded exponential backoff and deadline/max-attempt checks for `RUNNING → RETRY_WAIT`
- [ ] 10.3 Implement expired lease scanner with CAS recovery to RETRY_WAIT or FAILED
- [ ] 10.4 Add injectable-clock recovery tests for active lease, expired lease, exhausted attempts, expired deadline and terminal execution
- [ ] 10.5 Implement manual retry as a new ONCE task/execution with origin identifiers and immutable original history
- [ ] 10.6 Add repeated manual retry idempotency and permission/state conflict tests

## 11. Unified Web API and Security

- [ ] 11.1 Add task constraints/statistics/page/detail Req/VO/Resp and Web Assembler with Unix-millisecond times
- [ ] 11.2 Add execution page/detail/event timeline Req/VO/Resp without payload, claim token, path, secret or stack fields
- [ ] 11.3 Implement `/business-tasks` query/detail and `/business-task-executions` query/detail endpoints
- [ ] 11.4 Implement pause/resume/cancel/retry command endpoints with Handler capability, state and click-time permission validation
- [ ] 11.5 Add `Task:Query` and `Task:Control` menu/button permissions and administrator role grants in all database scripts
- [ ] 11.6 Add Controller tests for 403 missing permission, 404 invisible resource, 409 state conflict and idempotent controls
- [ ] 11.7 Update backend OpenAPI and verify stable code, time, pagination and AjaxResult contracts

## 12. SSE, Events, Metrics and Audit

- [ ] 12.1 Publish state/progress/execution SSE only after transaction commit with minimal identifiers and codes
- [ ] 12.2 Add rollback, duplicate terminal, burst progress and browser reconnect SSE tests
- [ ] 12.3 Register bounded-cardinality task, execution, duration, lag, queue, retry, lease and progress metrics
- [ ] 12.4 Add metric tests rejecting taskId, userId, subjectId, message, path or secret as tags
- [ ] 12.5 Add structured audit records for accepted/rejected controls and verify payload/secret/stack redaction
- [ ] 12.6 Implement configured event retention cleanup as a maintenance scheduler, not as a business task

## 13. Vue Vben Admin Task API and Routing

- [ ] 13.1 Sync authoritative OpenAPI to `vue-vben-admin/apps/web-antd/api` before frontend implementation
- [ ] 13.2 Add task/execution/event/capability TypeScript types that exactly mirror backend contracts
- [ ] 13.3 Add task query/detail/control and execution query/detail API modules with URL/method/body tests
- [ ] 13.4 Add task-center route and backend menu component mapping
- [ ] 13.5 Add Chinese and English task i18n files with key-symmetry and no-hardcoded-text tests
- [ ] 13.6 Implement task type registry for label, icon, domain route, result renderer and unknown-type fallback

## 14. Vue Vben Admin Task Center

- [ ] 14.1 Add pure-function tests for query conversion, status/action matrix, capabilities, permissions and progress presentation
- [ ] 14.2 Implement statistics cards, Schema filters and deterministic VxeGrid columns with fixed operation column
- [ ] 14.3 Implement quantified and indeterminate progress components with text, accessible labels and dark-theme states
- [ ] 14.4 Implement task detail Drawer with summary, schedule, owner, counters, business summary and result navigation
- [ ] 14.5 Implement execution history and append-only event timeline with sanitized failure guidance
- [ ] 14.6 Implement pause/resume/cancel/retry confirmations, loading, state conflict refresh and click-time permission checks
- [ ] 14.7 Implement 300ms SSE refresh coalescing and reconnect full-query behavior
- [ ] 14.8 Add responsive 375/768/1024/1440, keyboard, focus-return, 44px target and non-color-only status tests

## 15. Legacy Export Task Removal

- [ ] 15.1 Add a release preflight test/script that backs up and counts `tb_export_task` before destructive migration
- [ ] 15.2 Add explicit failure for non-empty legacy tables unless the authorized destructive flag is present
- [ ] 15.3 Remove `tb_export_task` creation and indexes from all three full schemas and add guarded DROP to incremental migrations
- [ ] 15.4 Delete ExportTask entity, Mapper/XML, Service/Impl, Manager, Assembler, DTO and enums
- [ ] 15.5 Delete ExportTask Controller, Req, VO, Web Assembler and every `/api/v1/exportTask/*` route
- [ ] 15.6 Remove ExportTask fixtures, test cleaner entries, API documentation and frontend mirrored SQL/docs
- [ ] 15.7 Add negative tests proving the old table is absent on fresh schema and old API routes return 404
- [ ] 15.8 Document that future export uses `DATA_EXPORT` Handler and a domain detail table, never the legacy table

## 16. Cross-Database, Fault and End-to-End Verification

- [ ] 16.1 Run fresh and incremental schema tests on SQLite, MySQL and PostgreSQL and save table/index/drop evidence
- [ ] 16.2 Test immediate, future and fixed-rate tasks through a deterministic fake Handler
- [ ] 16.3 Test application restart with durable PENDING, RETRY_WAIT, expired RUNNING and MISSED points
- [ ] 16.4 Test two application instances competing for schedule and execution claims
- [ ] 16.5 Inject queue-full, Handler exception, DB rollback, heartbeat loss, worker crash and SSE disconnect failures
- [ ] 16.6 Verify technical schedulers and SIP/session behavior continue when the business-task engine is disabled
- [ ] 16.7 Run affected Java tests, `mvn clean compile`, schema validation and dependency/purity scans
- [ ] 16.8 Run frontend task tests, `pnpm check`, `pnpm lint` and `pnpm build:antd`

## 17. Documentation, Rollout and First Handler Gate

- [ ] 17.1 Add operator guide for configuration, queue sizing, metrics, event retention, destructive legacy removal and rollback
- [ ] 17.2 Update architecture overview and development workflow with business-task versus technical-scheduler boundary
- [ ] 17.3 Publish task Handler onboarding guide, contract-test template and idempotent-side-effect checklist
- [ ] 17.4 Deploy core tables and query API with scheduler/dispatcher disabled and validate zero-work health
- [ ] 17.5 Enable scheduler/dispatcher with a deterministic internal Handler and observe lag, queue, retry and lease metrics
- [ ] 17.6 Allow `add-image-asset-collection` to register the first production `IMAGE_COLLECTION` Handler only after core acceptance gates pass
- [ ] 17.7 Complete release report with commits, test evidence, database preflight, old API removal, rollback proof and known limitations
