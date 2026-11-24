# ADR-007: No Docker; integration tests use a local MySQL 8.0 schema

- Status: Accepted

## Context
Docker Desktop is not available for this project. CLAUDE.md and the architecture doc originally planned Testcontainers MySQL for integration tests and Docker Compose for local runs.

## Decision
- Do not use Docker locally. No Testcontainers dependency.
- Backend tests run against MySQL 8.0 on localhost, schema `todo_manager_test` (configured in `application-test.yml`, overridable with `TEST_MYSQL_*` env vars). Tables are cleared before each test that needs them.
- OAuth providers remain stubbed with an embedded WireMock server; no real provider credentials are needed for tests.
- CI (GitHub Actions, Linux runner) uses a `mysql:8.0.46` service container, not Testcontainers.
- `docker-compose.yml` stays in the repo as an optional artifact; it is not used or verified here.

## Alternatives
- **Testcontainers**: isolated and reproducible, but requires Docker, which is unavailable.
- **H2**: no Docker, but diverges from MySQL behavior (CHECK constraints, collation, LIKE); rejected earlier (ADR-005).

## Consequences
- Developers must create `todo_manager_test` and grant `todo_user` access before running `./mvnw verify`.
- Test isolation is weaker than per-run containers; tests must clean their own rows.
- Behaviour matches production MySQL 8.0.46.
