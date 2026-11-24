# ADR-005: MySQL, Flyway, and optimistic locking

- Status: Proposed

## Context
Persistent storage for a multi-user app with search and per-user scoping. Two browser tabs may edit the same task. The schema must be reproducible and tests must run against the real database engine.

## Decision
- **MySQL 8.0** as the database; Testcontainers MySQL in integration tests (no H2).
- **Flyway** migrations; Hibernate `ddl-auto=validate`.
- **UUID** primary keys (`BINARY(16)`); timestamps `DATETIME(6)` in UTC.
- Enums as strings with `CHECK` constraints.
- **Optimistic locking**: `tasks.version` (`@Version`). Edit and status PATCH require `version`; mismatch → 409.

## Alternatives
- **PostgreSQL**: strong option, but the requirements and CLAUDE.md name MySQL; source doc mixed both.
- **H2 for tests**: fast, but diverges from MySQL behavior (CHECK, collation, LIKE).
- **Pessimistic locking**: unnecessary for single-user-owned data; holds locks across requests.
- **Last-write-wins**: silently loses edits from stale tabs.

## Consequences
- Requires Docker for integration tests (also needed for CI).
- Clients must send `version` and handle 409 (refetch and message).
- Search uses `LIKE` on `title`/`description`; full-text index deferred.
