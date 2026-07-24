# Open SSE Topic Subscription Design

## Problem

The pending SSE authorization refactor introduced a hard-coded root-topic allowlist and per-user delivery checks. Existing protocol-lab subscriptions include `session` and `clientcmd`, which are absent from that allowlist. Because subscription validation rejects the entire topic set when one topic is invalid, the protocol-lab SSE connection cannot register and loses otherwise valid `device` and `alarm` events as well.

Maintaining a complete topic allowlist in code also makes every new root topic require a backend change. A database-backed dynamic allowlist would require configuration APIs and an administration page, which are intentionally out of scope for the current release.

## Goals

- Require a valid authenticated user for every SSE connection.
- Allow an authenticated user to subscribe to any non-blank topic supplied by the client.
- Remove both hard-coded root-topic validation and user-permission filtering from SSE.
- Preserve exact-topic and dotted-prefix matching for event delivery.
- Keep Local and Redis-backed SSE behavior identical.
- Preserve stable user IDs and generated emitter IDs so raw JWT values are never used as connection identities.

## Non-Goals

- Building a topic configuration API or administration page.
- Adding database tables or menu records for SSE topics.
- Changing REST, menu, task, image, or other application permissions.
- Making the SSE endpoint anonymous.
- Changing event names or payload contracts.

## Subscription Contract

`GET /api/v1/stream/events` continues to require a valid token. After authentication, the server splits the comma-separated `topics` value, trims entries, removes duplicates, and rejects the request only when the resulting set is empty.

There is no supported-root allowlist. Existing roots such as `device`, `session`, `clientcmd`, `cascade`, `live`, `alarm`, `business.task`, and `image.asset` work without special registration, and future roots work under the same contract.

A subscription candidate matches an event when it equals the event topic or is a dotted prefix of it. For example, `clientcmd` matches `clientcmd.ptz`, while `client` does not.

## Authorization Boundary

Authentication remains the only SSE access boundary. The authenticated user's stable database ID is retained in the connection context for identity and diagnostics, but permissions are not copied into the context and are not evaluated during registration or delivery.

This decision intentionally permits any authenticated user who knows a topic to receive its event payload. REST and domain-service authorization remain unchanged and continue to protect commands, queries, and resource access.

## Component Changes

- `SseController` resolves the authenticated `UserDTO`, normalizes requested topics through the SSE subscription context, and registers the context without calling business-task permission checks.
- `SseSubscriptionContext` contains only the generated emitter ID, stable user ID, and normalized topic set. It validates stable identity and a non-empty normalized topic set, but has no allowlist or permission flags.
- The shared delivery decision performs only exact or dotted-prefix topic matching. It does not inspect `taskType` or query permissions.
- `LocalSseEventBus` and `RedisBackedSseEventBus` keep using the same shared delivery decision, preserving cross-mode behavior.
- `BusinessTaskAuthorizationService` no longer owns SSE context construction; its REST and task-domain authorization responsibilities remain intact.

## Error Handling

- Missing, invalid, or unresolved token: retain the existing authentication error and HTTP status behavior.
- Missing stable user ID: reject registration as an authentication failure.
- Null, empty, or whitespace-only topic set: reject registration with the existing SSE invalid-subscription business error.
- Any non-blank topic value: accept it; an unmatched topic simply receives no events.

## Verification

- A valid user with no task or image permissions can subscribe to and receive `business.task.*` and `image.asset.*` events.
- `device,session,clientcmd,alarm` registers as one subscription and receives matching events.
- A previously unknown root such as `future-domain` registers successfully and receives `future-domain.created`.
- Empty and whitespace-only topic requests are rejected.
- Exact matching and dotted-prefix matching remain correct.
- Local and Redis-backed event buses make the same delivery decisions.
- Emitter IDs and Redis messages do not contain the raw token.

## Compatibility and Risk

The change restores the pre-refactor open-topic behavior while retaining the newer stable identity and connection-context design. The accepted security trade-off is broader visibility of SSE payloads to authenticated users. If topic-level access control is required later, it should be introduced together with a complete dynamic configuration and administration workflow rather than another hard-coded list.
