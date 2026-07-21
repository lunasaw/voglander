## ADDED Requirements

### Requirement: Image management navigation
The frontend SHALL provide an Image Management menu with Image Assets at `/image/assets` and Image Collection at `/image/collection`, backed by database menu records and static route references whose component paths resolve under `apps/web-antd/src/views/image`. Device and channel pages SHALL be able to deep-link into collection or filtered assets with explicit route parameters/query values.

#### Scenario: Open collection from channel
- **WHEN** a user chooses image collection from a device channel row
- **THEN** the application navigates to `/image/collection` with that device and channel preselected after validating permission

### Requirement: Backend-defined API contract
Frontend request types, response types, fields, enum values, and endpoints SHALL match the committed Voglander OpenAPI specification. Frontend code MUST NOT invent derived backend fields; purely visual derived values SHALL be calculated locally and clearly typed.

#### Scenario: API specification changes
- **WHEN** a backend image field or endpoint changes
- **THEN** the OpenAPI snapshot and typed frontend API are updated together before the page consumes the change

### Requirement: Asset workbench
The Image Assets page SHALL use `Page` and provide access-scoped summary cards, schema-driven filters, upload and refresh controls, and a persistent gallery/table view switch. Both views SHALL use the same server query, pagination, selection, and sort state.

#### Scenario: Switch gallery to table
- **WHEN** a user switches from gallery to table after filtering and moving to another page
- **THEN** the filter values, current page, page size, selected items, and server result remain consistent

### Requirement: Lazy authorized thumbnails
Gallery cards SHALL load authorized asset content lazily as Blob URLs, show skeleton/error/empty states, limit concurrent preview requests, and revoke every Blob URL when a card is replaced, leaves the viewport cache, the query changes, or the component unmounts. The frontend MUST NOT construct a filesystem or permanent storage URL.

#### Scenario: Filter replaces visible cards
- **WHEN** a query refresh replaces the gallery result set
- **THEN** Blob URLs belonging to removed cards are revoked and new previews are fetched only for current visible assets

### Requirement: Asset upload drawer
The upload interaction SHALL use a connected Vben Drawer and Ant Design Upload, validate extension and configured client size before submission, generate one `Idempotency-Key` per user submission, show upload progress, and display server validation errors without treating client checks as authoritative.

#### Scenario: User retries a timed-out upload
- **WHEN** the browser retries the same submission after an unknown network outcome
- **THEN** it reuses the same idempotency key and the UI resolves to the original asset instead of displaying a duplicate

### Requirement: Asset detail and actions
The asset detail drawer SHALL show a large authorized preview, public metadata, source trace, lifecycle state, and context-aware links to task, execution, device, and channel. Preview, download, delete, and delete-retry controls SHALL be shown only when both the permission and asset state allow the action.

#### Scenario: Delete failed asset detail
- **WHEN** an asset is in `DELETE_FAILED` and the user has delete permission
- **THEN** the drawer hides content actions, shows the sanitized failure summary, and offers delete retry

### Requirement: Collection camera selector
The Image Collection page SHALL reuse existing device and channel APIs to provide a searchable, lazy-loaded device/channel selector with online state. It SHALL select exactly one channel and SHALL not maintain a second camera directory in browser state or backend data.

#### Scenario: Expand a device
- **WHEN** a user expands a device node in the selector
- **THEN** the page loads that device's channels from the existing channel API and marks offline options without fabricating identifiers

### Requirement: One-time and scheduled task forms
The collection task drawer SHALL provide `ONCE` and `SCHEDULED` modes. Scheduled mode SHALL require start, end, and interval, display minimum interval and maximum range, calculate the inclusive planned count, show first and last planned time, and block locally invalid submissions while accepting the server as final validator.

#### Scenario: Interval changes planned count
- **WHEN** a user changes a valid scheduled interval
- **THEN** the preview updates planned count and final planned time using the same inclusive formula as the backend

### Requirement: Task operations table
The collection page SHALL list mode, camera, state, range, interval, planned/success/failed/missed counts, next and last execution times, and actions allowed by the current state. Pause, resume, cancel, reschedule, view executions, and view assets SHALL each perform click-time permission validation and show a confirmation where the action changes state.

#### Scenario: Task state changes through SSE
- **WHEN** an SSE task-state event references a task visible on the current page
- **THEN** the page debounces a server refresh rather than treating the event payload as the authoritative full row

### Requirement: Execution history drawer
The execution drawer SHALL provide paginated execution history with planned, started, captured, and finished times, state, attempt count, worker node, asset link, and sanitized failure details. Retry SHALL appear only for server-designated retryable failures and SHALL explain that it creates a new current capture.

#### Scenario: Missed execution detail
- **WHEN** a user views a `MISSED` execution
- **THEN** the drawer identifies the missed planned time, shows no asset preview, and does not offer historical retry

### Requirement: Permission defense in depth
All image page actions SHALL use `useAccess().hasAccessByCodes` for visibility and SHALL repeat permission validation at click entry before opening a privileged drawer or issuing a request. Permission failure SHALL use an internationalized error message; the backend remains authoritative.

#### Scenario: Stale page permissions
- **WHEN** a user's permission is revoked after the page rendered but before an action click
- **THEN** click-time validation prevents the request when the frontend permission store is updated, and the backend still rejects any direct or stale request

### Requirement: Internationalization and icon consistency
Every user-visible image-management string SHALL exist in both `zh-CN/image.json` and `en-US/image.json`, using the `image.entity.action` key hierarchy. Icons SHALL come only from `@vben/icons` or backend menu icon identifiers already supported by the application.

#### Scenario: English locale
- **WHEN** the application locale is `en-US`
- **THEN** asset, collection, validation, empty, error, confirmation, and status text render without missing-key warnings or hard-coded Chinese strings

### Requirement: Responsive and accessible image views
Gallery, task forms, drawers, and tables SHALL remain usable at supported desktop/tablet widths, provide keyboard-operable controls, visible focus, meaningful alt text, and non-color-only state cues. VxeGrid tables SHALL enable synchronized horizontal and vertical scrolling with explicit column widths and a fixed action column.

#### Scenario: Narrow viewport asset gallery
- **WHEN** the asset page is shown at the minimum supported width
- **THEN** gallery columns collapse without horizontal page overflow and all asset operations remain keyboard reachable

### Requirement: Frontend verification
The frontend SHALL include unit tests for API mappings, time-range conversion, schedule-count calculation, permission-dependent actions, Blob URL lifecycle, state/action matrices, route deep links, and i18n completeness, plus an end-to-end path for upload, preview, collection, execution tracking, and delete.

#### Scenario: Blob lifecycle unit test
- **WHEN** a tested gallery component replaces and unmounts preview cards
- **THEN** the test proves that each created object URL is revoked exactly once
