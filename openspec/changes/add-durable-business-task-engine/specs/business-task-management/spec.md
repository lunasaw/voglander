## ADDED Requirements

### Requirement: Unified task query
The system SHALL provide authorized, stable, paginated queries across task types using task ID, type, state, name, owner, subject, business key, and time ranges. Sorting MUST use a server allowlist and produce deterministic ordering.

#### Scenario: Query running image tasks
- **WHEN** an authorized user filters by `taskType=IMAGE_COLLECTION` and `state=RUNNING`
- **THEN** the API returns only visible matching tasks in deterministic page order

### Requirement: Task and execution detail
The system SHALL provide task detail, paginated execution history, execution detail, and sanitized event timelines. Generic responses MUST NOT expose raw payloads, claim tokens, storage keys, absolute paths, credentials, or stack traces.

#### Scenario: Inspect failed execution
- **WHEN** a user opens an authorized failed execution
- **THEN** the response includes stable failure code, sanitized message, attempts, times, result reference and events without internal secrets

### Requirement: Capability-aware controls
Pause, resume, cancel, manual retry, progress, and reschedule actions SHALL be returned as explicit capabilities derived from Handler metadata, current state, and caller permissions. The backend MUST revalidate all three at command time.

#### Scenario: Stale retry button
- **WHEN** the UI submits retry after the execution has become non-retryable
- **THEN** the backend rejects the command with a state conflict and creates no new task

### Requirement: Consistent time and state contract
All Web timestamps SHALL be Unix millisecond values and all task types, modes, states, event types, capabilities, and failure codes SHALL be stable machine codes. User-visible labels SHALL be resolved through frontend i18n.

#### Scenario: Display task in English locale
- **WHEN** the task center renders a task under the English locale
- **THEN** it maps stable response codes to English text without depending on a Chinese backend description

### Requirement: Module permission and visibility
Unified task APIs SHALL require `Task:Query` or `Task:Control`. Missing module permission SHALL return 403; a task outside the caller visibility scope SHALL be indistinguishable from a missing resource and return 404.

#### Scenario: Direct access without visibility
- **WHEN** a caller with query permission requests a task outside its access scope
- **THEN** the API returns 404 and discloses no task metadata

### Requirement: Database-authoritative SSE
Task state, progress, and execution state SSE events SHALL be published only after database commit and SHALL contain only identifiers, stable codes, timestamp, and minimal relation keys. SSE MUST remain an optional refresh hint rather than a queue or fact store.

#### Scenario: Browser misses progress events
- **WHEN** a browser reconnects after missing several SSE notifications
- **THEN** querying task detail reconstructs the latest authoritative state and progress

### Requirement: Unified task center
Vue Vben Admin SHALL provide a task center with statistics, filters, deterministic VxeGrid pagination, status and progress display, task detail, execution history, event timeline, authorized controls, failure guidance, and business result navigation.

#### Scenario: Unknown task type appears
- **WHEN** the backend returns a newly registered task type unknown to the current frontend
- **THEN** the task center renders a generic label and detail fallback without page failure

### Requirement: Accessible progress and state
Task state and progress SHALL not rely on color alone. Quantified progress SHALL show numbers and text; unquantified progress SHALL show an indeterminate indicator with text; controls and drawers SHALL remain keyboard accessible and responsive.

#### Scenario: Task has no measurable total
- **WHEN** a running task reports `progressTotal=0`
- **THEN** the UI displays an indeterminate progress state and current phase message rather than a misleading percentage

### Requirement: Task type frontend registry
The frontend SHALL maintain a task type registry for localized name, icon, business detail route and optional result renderer, with a generic fallback. Business-specific rendering MUST NOT be implemented through conditionals scattered across the task center.

#### Scenario: Navigate to image collection result
- **WHEN** an image collection task exposes an authorized result or subject reference
- **THEN** the registered adapter builds the image-domain route while the generic task center remains domain-independent

### Requirement: Auditable control commands
Each accepted or rejected state-changing command SHALL emit a structured audit record with trace ID, actor, task, execution when applicable, command, previous/current state and stable result code. Audit output MUST omit raw payload and secrets.

#### Scenario: Unauthorized cancel attempt
- **WHEN** a caller without control permission attempts cancellation
- **THEN** the task is unchanged and an access-denied audit fact is recorded without exposing task payload

### Requirement: Bounded observability cardinality
The engine SHALL publish task counts, execution counts, duration, scheduler lag, queue depth, retry and lease recovery metrics using bounded tags such as type, state, result and failure class. Task IDs, user IDs, subject IDs and exception messages MUST NOT be metric tags.

#### Scenario: Large number of tasks
- **WHEN** millions of distinct task IDs are processed
- **THEN** metric series cardinality remains bounded by configured task types, states and result classes

### Requirement: Legacy export task retirement
The release SHALL remove the `tb_export_task` table, all ExportTask persistence/manager/Web classes, every `/api/v1/exportTask/*` endpoint, and their published API schemas. No compatibility endpoint, dual write, old-table read, or history migration SHALL remain.

#### Scenario: Call removed export endpoint
- **WHEN** a client calls an old `/api/v1/exportTask/*` route after the release
- **THEN** the server returns 404 and does not translate the request into a generic task

### Requirement: Destructive migration gate
Before dropping `tb_export_task`, deployment SHALL back up the database and verify the table row count. An empty table SHALL be dropped directly; a non-empty table MUST stop automated migration unless an explicitly authorized destructive flag is present.

#### Scenario: Production legacy table contains rows
- **WHEN** migration detects one or more rows and no authorized destructive flag
- **THEN** deployment fails before dropping the table and reports the required operator action

### Requirement: Cross-database schema parity
SQLite, MySQL and PostgreSQL full and incremental scripts SHALL provide equivalent core tables, unique constraints, indexes, legacy removal behavior and validation queries using native dialects.

#### Scenario: Fresh database initialization
- **WHEN** each supported database initializes from its full schema
- **THEN** all three business task tables and required indexes exist and `tb_export_task` does not exist

### Requirement: No technical scheduler migration
The business task engine SHALL not register node heartbeats, SSE heartbeats, session GC, SIP keepalive, subscription refresh, protocol dialog timers or other system maintenance jobs as business tasks.

#### Scenario: SIP keepalive continues after task engine disabled
- **WHEN** the business task feature is disabled
- **THEN** protocol keepalive and unrelated maintenance schedulers continue according to their existing configuration
