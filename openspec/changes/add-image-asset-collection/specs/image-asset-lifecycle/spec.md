## ADDED Requirements

### Requirement: Unified image asset identity
The system SHALL assign every successfully ingested image a globally unique, immutable `assetId` and SHALL represent uploaded, camera-captured, and future externally imported images with the same asset model. Failed uploads and failed or missed captures MUST NOT create asset records.

#### Scenario: Successful camera capture creates one asset
- **WHEN** a camera collection execution completes with a valid stored image
- **THEN** the system creates exactly one available image asset and links the execution to that asset

#### Scenario: Failed ingest creates no asset
- **WHEN** image validation, snapshot acquisition, storage, or database registration fails before ingest completes
- **THEN** the system records the operation failure without creating a failed or placeholder image asset

### Requirement: Traceable primary source
Each image asset SHALL have exactly one primary source record containing a source type, source system, source entity identity, and an immutable source snapshot. Camera sources SHALL retain task, execution, device, channel, media-node, and stream facts available at capture time; upload sources SHALL retain uploader and sanitized original filename facts.

#### Scenario: Device metadata changes after capture
- **WHEN** a device or channel is renamed after a camera image was captured
- **THEN** the asset source continues to expose the device and channel names recorded at capture time

#### Scenario: Internal source metadata is returned
- **WHEN** an authorized user requests asset details
- **THEN** the system returns only the source metadata fields on the public allowlist and does not expose secrets or internal paths

### Requirement: Asset metadata completeness
An available asset SHALL persist its content type, format, byte size, pixel width, pixel height, checksum algorithm, SHA-256 checksum, storage provider reference, captured time, ingested time, owner, and retention policy. Web responses SHALL express time values as Unix milliseconds while persistence and DTO layers use `LocalDateTime`.

#### Scenario: Uploaded image metadata is queried
- **WHEN** an authorized user queries an uploaded asset
- **THEN** the returned metadata matches the verified file content and contains distinct captured, ingested, created, and updated time semantics

### Requirement: Asset page query
The system SHALL provide paginated asset querying by asset identity or name, source type, device, channel, task, execution, owner, status, captured-time range, and create-time range. The default order SHALL be `capturedAt DESC, assetId DESC`, and deleted assets SHALL be excluded unless an authorized administrative query explicitly requests them.

#### Scenario: Query camera assets for a channel
- **WHEN** a user with query permission filters by `deviceId`, `channelId`, and a captured-time range
- **THEN** the system returns only matching, visible assets in stable descending order with `total` and `items`

#### Scenario: Next page has equal captured times
- **WHEN** multiple matching assets share the same captured time and the user changes pages
- **THEN** `assetId` tie-breaking produces deterministic, non-duplicated page ordering

### Requirement: Asset summary statistics
The system SHALL provide summary counts required by the asset page for total visible assets, available assets, assets ingested today, and delete-failed assets, using the same access scope as asset queries.

#### Scenario: User opens the asset page
- **WHEN** the page requests asset statistics
- **THEN** each count reflects only assets the authenticated user is permitted to query

### Requirement: Authorized inline content
The system SHALL serve image content through an authenticated asset endpoint rather than a public filesystem mapping or permanent provider URL. Available content responses SHALL use the verified `Content-Type`, `Content-Length`, checksum-based `ETag`, `Cache-Control: private`, and `Content-Disposition: inline`; storage keys, absolute paths, and provider secrets MUST NOT appear in metadata or response headers.

#### Scenario: Authorized preview
- **WHEN** a user with view permission requests content for an available asset in scope
- **THEN** the system streams the image with inline disposition and private caching headers

#### Scenario: Conditional preview
- **WHEN** a client repeats a content request with an `If-None-Match` value equal to the asset checksum ETag
- **THEN** the system returns HTTP 304 without reopening or streaming the object body

#### Scenario: Deleted asset preview
- **WHEN** a user requests content for an asset in `DELETED`, `DELETING`, or `DELETE_FAILED`
- **THEN** the system refuses content access and does not reveal whether the storage object still exists

### Requirement: Authorized download
The system SHALL expose a separate authenticated download operation that uses `Content-Disposition: attachment` and a sanitized filename while preserving the same access, ETag, MIME, and path-secrecy guarantees as inline content.

#### Scenario: Asset download
- **WHEN** a user with download permission downloads an available asset
- **THEN** the browser receives the original verified bytes using a safe display filename that cannot inject path or header content

### Requirement: Idempotent deletion lifecycle
Asset deletion SHALL use the states `AVAILABLE`, `DELETING`, `DELETED`, and `DELETE_FAILED`. The system SHALL mark an asset `DELETING` before removing storage, SHALL mark it `DELETED` only after idempotent storage deletion succeeds or confirms absence, and SHALL preserve a sanitized failure code and message in `DELETE_FAILED` for retry.

#### Scenario: Successful delete
- **WHEN** an authorized user deletes an available asset and storage deletion succeeds
- **THEN** the asset transitions through `DELETING` to `DELETED`, receives `deletedAt` and `deleteReason`, and can no longer be previewed or downloaded

#### Scenario: Repeated delete
- **WHEN** an authorized user deletes an already deleted asset
- **THEN** the operation succeeds without recreating work or changing the original deletion facts

#### Scenario: Storage delete failure
- **WHEN** provider deletion fails after the asset becomes `DELETING`
- **THEN** the asset becomes `DELETE_FAILED`, remains auditable, and can be retried without losing its storage reference

### Requirement: Permanent first-phase retention
Every first-phase asset SHALL use `retentionPolicy=PERMANENT` and `expiresAt=null`. The API MUST NOT allow ordinary users to set an unsupported retention policy or trigger automatic expiration.

#### Scenario: Client supplies an unsupported retention policy
- **WHEN** an upload or collection request specifies a policy other than `PERMANENT`
- **THEN** the system rejects the request without saving content or creating task or asset records

### Requirement: Server-side access enforcement
The backend SHALL enforce separate query, view, upload, download, delete, collection-query, collection-create, and collection-control permissions. Asset and source visibility SHALL be evaluated by an access-policy component on every metadata and content operation; frontend button hiding MUST NOT be treated as authorization.

#### Scenario: Direct API call without permission
- **WHEN** an authenticated user calls an image endpoint directly without its required permission code
- **THEN** the backend returns HTTP 403 and performs no storage or database mutation

### Requirement: Image audit trail
Upload, content download, delete, delete retry, collection creation, and collection control operations SHALL emit structured audit logs containing trace ID, actor ID, operation, result, and relevant asset/task/execution/device/channel identifiers, without image bytes, Base64 data, secrets, or absolute paths.

#### Scenario: Delete attempt is audited
- **WHEN** an asset delete succeeds or fails
- **THEN** one structured audit record captures the actor, target, trace, result, and stable failure code without sensitive storage details
