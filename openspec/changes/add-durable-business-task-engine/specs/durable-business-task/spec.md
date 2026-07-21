## ADDED Requirements

### Requirement: Registered task handlers
Every accepted business task SHALL resolve to exactly one startup-validated `LongTaskHandler` identified by a stable `taskType`. Duplicate handlers, blank types, or unsupported payload versions MUST fail application startup or task acceptance before any execution is created.

#### Scenario: Duplicate task type
- **WHEN** two handlers register the same stable task type
- **THEN** application startup fails with a diagnostic naming the duplicated type

### Requirement: Trusted domain task creation
Only trusted domain application services SHALL create tasks through the internal task creation API after domain validation and authorization. The public Web API MUST NOT accept an arbitrary client-provided `taskType` and payload combination.

#### Scenario: Client attempts generic creation
- **WHEN** a client calls the unified task API with an arbitrary type and JSON payload
- **THEN** no generic creation endpoint accepts the request and no task is persisted

### Requirement: Stable identity and idempotent creation
Tasks, executions, and events SHALL use stable opaque business identifiers independent of database primary keys. Repeating a create request with the same owner, task type, and idempotency key SHALL return the original task without creating another task or first execution.

#### Scenario: Repeated domain request
- **WHEN** the same owner repeats a valid request with the same task type and idempotency key
- **THEN** the original task identity and current state are returned and no duplicate execution is materialized

### Requirement: Immutable versioned payload
The system SHALL persist a FastJSON2 JSON payload snapshot and payload version at task creation. The payload MUST remain immutable, MUST be interpreted only by its registered Handler, and MUST NOT contain credentials, binary content, Base64 data, absolute paths, or internal media secrets.

#### Scenario: Handler upgraded after task creation
- **WHEN** a queued task created with an older supported payload version is executed after a Handler deployment
- **THEN** the Handler receives the original payload and version and executes or rejects it according to its declared compatibility

### Requirement: Supported schedule modes
The task engine SHALL support `ONCE`, `AT_TIME`, and `FIXED_RATE` modes. Fixed-rate plan points SHALL be derived from the prior theoretical point plus the interval and MUST NOT drift based on actual completion time.

#### Scenario: Slow fixed-rate execution
- **WHEN** one execution completes after the next theoretical plan time
- **THEN** later plan times remain anchored to the original schedule cursor

### Requirement: Durable execution facts
The system SHALL persist one execution per planned point with unique `(taskId, scheduleVersion, plannedAt)` identity. An execution SHALL remain queryable after completion, retry, application restart, pause, cancellation, or task terminalization.

#### Scenario: Competing scheduler nodes
- **WHEN** two nodes materialize the same task plan point concurrently
- **THEN** conditional updates and the database unique constraint leave exactly one execution fact

### Requirement: Explicit task state machine
Task state transitions SHALL be validated against the current state and optimistic version. Supported active/control states SHALL be `SCHEDULED`, `RUNNING`, `PAUSED`, and `CANCELLING`; supported terminal states SHALL be `COMPLETED`, `PARTIAL_COMPLETED`, `FAILED`, and `CANCELLED`. Terminal tasks MUST NOT be reopened.

#### Scenario: Resume completed task
- **WHEN** a caller requests resume for a completed task
- **THEN** the command is rejected as a state conflict and the task remains completed

### Requirement: Explicit execution state machine
Execution state transitions SHALL be limited to `PENDING`, `RUNNING`, `RETRY_WAIT`, `SUCCEEDED`, `FAILED`, `MISSED`, and `CANCELLED` according to the designed transition graph. A terminal execution MUST NOT be overwritten by a duplicate completion or recovery callback.

#### Scenario: Duplicate success completion
- **WHEN** two callbacks attempt to mark the same running execution successful
- **THEN** exactly one conditional update succeeds and task success counters increase once

### Requirement: Missed plan semantics
A plan point older than its allowed-delay or deadline window SHALL become `MISSED` without invoking the Handler. Restart and resume reconciliation SHALL persist each missed point instead of executing historical work at the current time.

#### Scenario: Restart after scheduled time
- **WHEN** the application restarts after a fixed-rate point is outside its allowed window
- **THEN** the point becomes MISSED and the Handler is not invoked for that historical time

### Requirement: Bounded durable dispatch
The dispatcher SHALL scan durable runnable executions and submit only execution identifiers to a bounded worker pool. Queue saturation MUST leave work durably pending or retry-waiting and MUST NOT discard it or run it on the scheduler thread.

#### Scenario: Worker queue saturated
- **WHEN** the bounded worker queue rejects a submission
- **THEN** the execution remains eligible for a later dispatch and no terminal counter advances

### Requirement: Multi-node claim and lease
Before invoking a Handler, a worker SHALL claim the execution using current state, optimistic version, a unique claim token, worker node, and lease deadline. Only the successful claimant may complete, retry, heartbeat, or cancel that attempt.

#### Scenario: Two workers claim one execution
- **WHEN** two workers race to claim the same pending execution
- **THEN** one CAS update succeeds and only that worker invokes the Handler

### Requirement: Lease heartbeat and crash recovery
Running executions SHALL renew their lease while making progress. A recovery scanner SHALL move an expired lease to `RETRY_WAIT` when attempts and deadline permit, otherwise to `FAILED`, without recovering terminal executions.

#### Scenario: Worker crashes during execution
- **WHEN** a worker stops heartbeating and its lease expires before the retry deadline
- **THEN** the execution becomes retry-waiting with a lease-expired failure event and can be claimed again

### Requirement: Classified bounded automatic retry
The Handler SHALL classify failures as retryable or permanent. Retryable failures SHALL use bounded exponential backoff only while max attempts and the execution deadline permit; permanent failures, exhausted attempts, and expired windows SHALL become `FAILED`.

#### Scenario: Retryable timeout exhausts attempts
- **WHEN** a retryable timeout occurs on the final allowed attempt
- **THEN** the execution becomes FAILED with the stable timeout code and no new automatic attempt is scheduled

### Requirement: Monotonic rate-limited progress
Handlers SHALL report progress through `LongTaskContext`, not by updating persistence directly. Progress revisions and current values MUST be monotonic, invalid totals MUST be rejected, writes SHALL be rate limited, and terminal progress SHALL always be persisted.

#### Scenario: Handler reports lower progress
- **WHEN** a Handler reports a current value below the persisted current value
- **THEN** the update is rejected or ignored and clients never observe progress moving backwards

### Requirement: Cooperative cancellation
Cancellation SHALL stop new plan materialization, cancel unstarted executions, expose a cancellation token to running Handlers, and allow a bounded completion period before lease/timeout recovery. The engine MUST NOT use unsafe thread termination.

#### Scenario: Cancel task with running Handler
- **WHEN** a user cancels a task while one execution is running
- **THEN** no later point is created, pending work is cancelled, the Handler observes the token, and the task reaches CANCELLED after active work terminates

### Requirement: Pause and resume fixed-rate tasks
Handlers declaring pause support SHALL allow idempotent pause and resume. Pausing SHALL preserve the schedule cursor; resuming SHALL reconcile expired points as MISSED and continue from the first eligible point.

#### Scenario: Resume after several elapsed points
- **WHEN** a paused fixed-rate task resumes after three points exceeded their deadlines
- **THEN** three MISSED executions are persisted and only a still-eligible or future point can run

### Requirement: Transactional counters and terminal result
Execution terminalization, task counters, task progress summary, last execution fields, task terminal evaluation, and the corresponding event SHALL commit in one transaction. Counters MUST equal unique terminal execution facts.

#### Scenario: Mixed final outcomes
- **WHEN** the final fixed-rate point ends and the task has both succeeded and failed or missed executions
- **THEN** the task becomes PARTIAL_COMPLETED with accurate success, failed, missed, cancelled, and planned counts

### Requirement: Manual retry creates new facts
An authorized manual retry SHALL create a new `ONCE` task and execution linked through origin identifiers. It MUST NOT alter the failed execution or reopen the original terminal task.

#### Scenario: Retry failed task from task center
- **WHEN** a retryable failed execution is manually retried
- **THEN** a new task is returned, the original history remains immutable, and the new task records its origin

### Requirement: Append-only audit events
Every accepted control command, state transition, attempt start, retry classification, terminal result, and significant progress phase SHALL append a sanitized event after or within the authoritative database transaction. Events MUST NOT contain payload secrets or exception stacks.

#### Scenario: Transaction rolls back
- **WHEN** an execution completion transaction rolls back
- **THEN** neither its terminal state, task counter, nor success event becomes visible

### Requirement: Handler failure isolation
A Handler exception SHALL be converted to a stable classified execution result without terminating scheduler, dispatcher, recovery, or unrelated Handler work. Unknown exceptions SHALL default to a non-sensitive system failure code.

#### Scenario: Handler throws unexpected runtime exception
- **WHEN** one Handler throws an unclassified runtime exception
- **THEN** its execution records a sanitized failure while other task types continue dispatching

### Requirement: Transactional domain completion participant
A Handler MAY provide an idempotent completion participant for same-datasource domain writes. The engine SHALL invoke it in the same transaction as execution success, task counters/progress/terminal state, result reference and success event; external I/O MUST occur before this transaction and use idempotent compensation if the transaction fails.

#### Scenario: Domain completion insert fails
- **WHEN** a domain completion participant fails while registering its result
- **THEN** domain writes, execution success, task counters and success event all roll back and the Worker invokes configured external-resource compensation
