## ADDED Requirements

### Requirement: Durable task engine prerequisite
Camera image collection SHALL use the `durable-business-task` capability as its only task, execution, event, schedule, progress, retry, lease and control authority. The image domain MUST NOT create a parallel task state machine, execution dispatcher or lease-recovery subsystem.

#### Scenario: Image module starts without task engine
- **WHEN** image collection is enabled but the durable business task engine or `IMAGE_COLLECTION` Handler registration is unavailable
- **THEN** startup fails with a clear dependency diagnostic before accepting collection requests

### Requirement: Existing camera directory as source of truth
Collection creation SHALL select one camera position using explicit existing `deviceId` and `channelId`. The image domain SHALL verify both records exist, the channel belongs to the device, and the caller can collect from the resource; it MUST NOT duplicate the camera catalog or infer ownership from identifier prefixes.

#### Scenario: Channel does not belong to device
- **WHEN** a create request combines an existing device with a channel owned by another device
- **THEN** the request is rejected before creating image configuration, task or execution facts

### Requirement: Image collection configuration
Each image collection task SHALL have exactly one `tb_image_collection_config` row keyed by the generic `taskId`. The row SHALL contain device/channel identity, immutable device/channel name snapshots, retention policy and image-specific options, while task name, mode, schedule, state, progress and counters remain in the generic task tables.

#### Scenario: Read collection task detail
- **WHEN** an authorized caller requests image collection detail
- **THEN** the service combines the generic task facts with exactly one image configuration without duplicating state fields

### Requirement: Atomic domain task creation
The image collection application service SHALL atomically persist image configuration and create the generic task through the trusted internal creation API. An `ONCE` request SHALL result in one generic ONCE task and first execution; a scheduled request SHALL result in one generic FIXED_RATE task with its schedule cursor.

#### Scenario: Configuration insert fails
- **WHEN** image configuration persistence fails during task creation
- **THEN** neither generic task nor execution facts remain committed

### Requirement: Idempotent collection creation
Image collection creation SHALL require an Idempotency-Key scoped by actor and `IMAGE_COLLECTION` task type. Repeating the same accepted request SHALL return the original generic task and image configuration without creating another capture.

#### Scenario: Repeated one-time request
- **WHEN** the same owner repeats an ONCE request with the same idempotency key
- **THEN** the original task/execution identity is returned and the camera Handler is not queued twice

### Requirement: Scheduled collection constraints
A scheduled collection SHALL bind one camera, inclusive start time, inclusive end time and fixed interval. The image service SHALL apply image-specific minimum interval and maximum duration/count constraints before calling the generic FIXED_RATE creation API.

#### Scenario: Excessive image schedule
- **WHEN** the inclusive plan count exceeds the configured image collection maximum
- **THEN** the request is rejected before any image or generic task facts are persisted

### Requirement: IMAGE_COLLECTION Handler binding
The image module SHALL register exactly one stable `IMAGE_COLLECTION` Handler with a versioned payload containing only the image configuration identity and required immutable execution inputs. The Handler SHALL declare progress, cancel, pause and manual-retry capabilities supported by image collection.

#### Scenario: Unknown payload version
- **WHEN** the Handler receives an unsupported image collection payload version
- **THEN** the execution fails with a stable non-retryable payload-version code without opening a media stream

### Requirement: Capture stream lease
Each image execution SHALL acquire a live-stream reference through the existing `MediaPlayService.startLive`, use the returned stream and owning media node for snapshot, and release exactly the acquired reference through `stopLive` in a `finally` path. Existing viewers and other executions MUST retain their references.

#### Scenario: Reuse active live stream
- **WHEN** an active stream already exists for the device and channel
- **THEN** the Handler increments one reference, captures one image and decrements one reference without terminating other viewers

#### Scenario: Snapshot fails after acquisition
- **WHEN** ZLM snapshot fails after a stream reference was acquired
- **THEN** the acquired reference is still released and the exception is returned to the generic retry classifier

### Requirement: Pre-execution camera validation
Before acquiring a stream, every execution SHALL revalidate device/channel existence, ownership and required online state. Missing, mismatched, unauthorized or offline resources SHALL produce stable image-domain failures and MUST NOT invoke ZLM.

#### Scenario: Channel removed after scheduling
- **WHEN** a future execution starts after its configured channel has been removed
- **THEN** the Handler returns a non-retryable missing-channel failure and creates no asset

### Requirement: Unified snapshot ingest
Successful camera snapshot bytes SHALL pass through the same stage, signature, format, dimension, checksum, promotion and storage validation used by uploads. ZLM temporary files or URLs MUST NOT become permanent asset references.

#### Scenario: ZLM returns corrupt JPEG
- **WHEN** ZLM reports success but the produced file cannot be decoded as an allowed image
- **THEN** validation rejects it, temporary content is cleaned, and no available asset is registered

### Requirement: Transactional image completion participant
The image Handler SHALL provide an idempotent completion participant invoked by the generic task engine in the execution-completion transaction. It SHALL insert the asset and source, bind the stable generic execution ID, and allow the engine to mark execution success and update task counters atomically.

#### Scenario: Completion transaction rolls back
- **WHEN** asset/source insertion or generic execution completion fails
- **THEN** all database facts roll back, promoted storage is compensated, and no visible asset claims a failed execution

### Requirement: One asset per successful execution
An image source SHALL use the generic execution ID as a unique nullable source key. One execution SHALL register at most one asset, and an idempotent completion retry SHALL return the already registered asset rather than creating another.

#### Scenario: Completion callback repeats
- **WHEN** the same claimed execution reaches image completion twice because of retry or callback duplication
- **THEN** the unique execution-source relation leaves one asset and one generic success count

### Requirement: Image-specific retry classification
The Handler SHALL classify media timeout, temporary node unavailability and retryable storage errors as transient only when the generic execution window permits. Invalid configuration, missing resources, permission errors, image validation failures and MISSED plan points MUST NOT be retried automatically.

#### Scenario: First snapshot attempt times out
- **WHEN** a snapshot timeout occurs and the generic attempt/deadline policy permits retry
- **THEN** the Handler returns a retryable classification and the task engine schedules bounded backoff

### Requirement: Missed and cancelled executions avoid media work
When the generic task engine records an image execution as `MISSED` or `CANCELLED` before claim, the image Handler SHALL not be invoked. A running Handler SHALL honor the generic cooperative cancellation token between stream acquisition, snapshot, validation and promotion stages.

#### Scenario: Cancel before dispatch
- **WHEN** an image execution is cancelled while still pending
- **THEN** no stream reference, snapshot file or asset is created

### Requirement: Per-camera concurrency guard
The image Handler SHALL use a bounded per-camera guard around stream and snapshot work so simultaneous generic executions cannot overload one camera. The guard MUST NOT replace the generic execution claim or cause work loss when unavailable.

#### Scenario: Two tasks target the same camera
- **WHEN** two claimed image executions target the same device/channel concurrently
- **THEN** only the configured number enter snapshot work and the other remains safely retryable within its deadline

### Requirement: Domain and generic queries
Image collection APIs SHALL provide creation and image-enriched task queries by device/channel. Generic state, progress, controls, execution history and event timelines SHALL come from the business-task APIs and use generic IDs without image-specific shadow copies.

#### Scenario: Open image task from task center
- **WHEN** a user follows an IMAGE_COLLECTION task from the generic task center
- **THEN** the image type registry routes to an enriched view that combines generic facts, camera configuration and resulting assets

### Requirement: Manual retry preserves history
Manual retry of a retryable failed image execution SHALL use the generic engine to create a new ONCE `IMAGE_COLLECTION` task linked to the original task/execution and reuse validated camera configuration as a new immutable snapshot.

#### Scenario: Retry failed scheduled capture
- **WHEN** an authorized user retries one failed scheduled execution
- **THEN** the original schedule and execution remain unchanged while a new current-time one-shot task is created

### Requirement: Database-authoritative notifications
Image asset events and generic task/execution events SHALL be published only after their respective database commits. The image UI SHALL treat SSE as a refresh hint and reconstruct state from image plus business-task queries after reconnect.

#### Scenario: Browser reconnects after missed events
- **WHEN** a browser reconnects after missing task, execution and asset SSE events
- **THEN** fresh queries reconstruct the complete task, camera, execution and asset relationships
