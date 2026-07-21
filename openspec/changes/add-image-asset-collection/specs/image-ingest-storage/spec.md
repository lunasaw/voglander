## ADDED Requirements

### Requirement: Validated image upload
The system SHALL accept multipart uploads only for configured JPEG, PNG, and WebP formats and SHALL validate the declared MIME type, file signature, decoder-recognized format, dimensions, byte size, and pixel count before promoting content to a permanent asset. SVG and formats outside the allowlist MUST be rejected.

#### Scenario: Valid JPEG upload
- **WHEN** an authenticated user with upload permission submits a JPEG whose signature, decoder format, size, and dimensions satisfy all limits
- **THEN** the system stores the content, creates an available asset and upload source, and returns the stable `assetId`

#### Scenario: Extension and content disagree
- **WHEN** an uploaded file is named with an allowed extension but its signature or decoded format is not allowed
- **THEN** the system rejects it as an unsupported or invalid image and removes every staged object

#### Scenario: Decompression bomb dimensions
- **WHEN** an image is within the byte limit but its verified width multiplied by height exceeds the configured pixel limit
- **THEN** the system rejects the image before permanent registration

### Requirement: Streaming resource limits
Upload and provider ingest SHALL be streamed through a hard byte-counting limit and checksum digest rather than buffered entirely in memory. Validation SHALL fail as soon as the configured byte ceiling is exceeded and SHALL close all streams and temporary resources.

#### Scenario: Oversized stream without content length
- **WHEN** a client streams an upload without a trustworthy `Content-Length` and the bytes cross the configured maximum
- **THEN** the server terminates staging, returns the stable file-too-large error, and leaves no permanent object

### Requirement: Stable checksum and image inspection
The ingest flow SHALL calculate SHA-256 over the exact persisted bytes and SHALL obtain image width, height, and format from a decoder/metadata inspector with explicit resource limits. File extensions and user-provided metadata MUST NOT determine persisted image facts.

#### Scenario: Metadata spoofing
- **WHEN** a client claims dimensions, content type, or checksum values that differ from the file
- **THEN** the system ignores the claimed values and persists only server-verified metadata

### Requirement: Provider-independent storage port
Business services SHALL access image bytes only through an `ImageStorageService` contract supporting staging, staged reads, promotion to a generated final key, content reads, idempotent deletion, existence checks, and staged-object cleanup. Database records SHALL contain provider identifiers and relative storage keys only, never image BLOBs, Base64 content, or absolute filesystem paths.

#### Scenario: Storage implementation is replaced
- **WHEN** a future shared-filesystem or object-storage adapter implements the storage contract
- **THEN** asset, source, collection, and Web API behavior remains unchanged

### Requirement: Local storage safety
The first-phase local provider SHALL generate keys under `images/YYYY/MM/DD/{assetId}.{ext}`, stage writes under an isolated temporary directory, create directories safely, use atomic move when the filesystem supports it, and reject absolute, parent-traversal, symlink-escape, or provider-root-escape paths. Original filenames MUST NOT participate in storage keys.

#### Scenario: Malicious original filename
- **WHEN** an upload filename contains path separators, `..`, control characters, or an absolute path
- **THEN** the sanitized display name is bounded and safe while the generated storage location remains under the configured root

#### Scenario: Resolved key escapes provider root
- **WHEN** a malformed or tampered storage key resolves outside the configured canonical root
- **THEN** the provider rejects the operation and performs no file read, write, or delete outside the root

### Requirement: File and database compensation
Ingest SHALL stage and validate content before promotion, promote it before the asset/source database transaction, and delete the promoted object if that transaction fails. Compensation failure SHALL be logged and discoverable by orphan reconciliation; an incomplete transaction MUST NOT be returned as a successful asset.

#### Scenario: Database registration fails after promotion
- **WHEN** the permanent object exists but the asset/source transaction rolls back
- **THEN** the service attempts idempotent provider deletion, returns an ingest failure, and records an orphan-cleanup signal if deletion also fails

#### Scenario: Process stops with a staged file
- **WHEN** the process terminates after staging but before promotion
- **THEN** a later temporary-object sweep can identify and remove the expired staged object without consulting an asset record

### Requirement: Camera snapshot adaptation
Camera capture SHALL call a `MediaSnapshotAdapter` that wraps the existing ZLM `getSnap` capability with the stream's actual media node, a supported play URL, configured timeout, zero cache expiry, and a unique local temporary save path. The adapter SHALL verify that the result exists and contains bytes and SHALL remove its temporary file in all outcomes.

#### Scenario: ZLM returns no snapshot
- **WHEN** ZLM returns null, an empty path, a missing file, or a zero-length file
- **THEN** the adapter returns the stable snapshot-failed error and the collection execution fails without an asset

#### Scenario: Concurrent snapshots use temporary files
- **WHEN** multiple executions capture the same camera concurrently
- **THEN** each adapter invocation uses a unique temporary path and one execution cannot overwrite or delete another execution's snapshot

### Requirement: Storage content streaming
The storage service SHALL open content as a bounded stream/resource and the Web layer SHALL stream it to the response without reading the whole asset into a byte array. The stream SHALL be closed on success, client disconnect, and provider error.

#### Scenario: Client aborts a preview
- **WHEN** a browser closes the connection during image streaming
- **THEN** the server closes the storage stream and records the abort without treating it as content corruption

### Requirement: Storage health and orphan reconciliation
The system SHALL expose storage health information and a scheduled reconciliation process that can find expired staging objects, permanent objects with no asset row, and available asset rows whose object is missing. Reconciliation SHALL default to report-only for permanent objects and MUST NOT silently delete registered assets.

#### Scenario: Missing registered object
- **WHEN** reconciliation finds an `AVAILABLE` asset whose storage object does not exist
- **THEN** it emits an error metric and diagnostic record for operator action without fabricating content or deleting the asset row

### Requirement: Multi-node storage safety
Local storage SHALL be supported for single-node or shared-filesystem deployments only. When collection or content endpoints run in a multi-node deployment, configuration validation SHALL require a shared provider or an explicitly supported node-routing strategy; the system MUST NOT claim arbitrary-node readability for node-local files.

#### Scenario: Unsafe multi-node local configuration
- **WHEN** startup detects multi-node mode with non-shared local storage and no supported storage-node routing
- **THEN** startup fails fast or disables image writes with a clear configuration error
