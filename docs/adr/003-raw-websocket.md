# ADR-003: Raw WebSocket with JSON messages

- Status: Proposed

## Context
Requirements need a server→client notification stream and at least one client→server message, for one authenticated user channel. No message broker.

## Decision
Spring WebSocket raw handler at `/ws/notifications` with a JSON `{type, payload}` envelope. Native browser `WebSocket` on the client. Server-side scheduler (60 s) computes per-user batches.

## Alternatives
- **STOMP over WebSocket**: built-in destinations and subscriptions, but a larger protocol surface and no need for topic routing here.
- **Server-Sent Events**: server-only; does not satisfy the two-way requirement.
- **Polling REST**: simpler, but not a WebSocket and does not meet the requirement.

## Consequences
- Small, documented protocol (SUBSCRIBE, SUBSCRIPTION_UPDATED, NOTIFICATION_BATCH, ERROR).
- Developer owns validation, rate limits, and per-connection serialization (`ConcurrentWebSocketSessionDecorator`).
- Protocol documented in `docs/architecture.md` §10 and copied to README.
