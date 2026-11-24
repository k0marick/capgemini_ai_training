# ADR-001: Modular monolith with package-by-feature

- Status: Proposed

## Context
MVP with one team, one database, and modest load. Requirements cover auth, projects, tasks, search, and WebSocket notifications.

## Decision
Single Spring Boot application, packages by feature (`auth`, `user`, `project`, `task`, `notification`, `common`), layered Controller → Service → Repository. Single React SPA. Single MySQL database.

## Alternatives
- **Microservices**: operational cost, distributed auth and data consistency; no benefit at this scale.
- **Layered by technical type** (`controllers/`, `services/`): weaker module boundaries.
- **Hexagonal with ports/adapters**: extra abstraction without a current need.

## Consequences
- Simple local run, one deploy unit, transactional consistency for project delete and task changes.
- Boundaries enforced by convention and code review; cross-feature calls go through services.
- Splitting later remains possible if load demands it.
