# Personal To-Do Manager

Status: P1 foundation. Full README (OAuth setup, WebSocket protocol, test commands) arrives in P6.

## Requirements
- Java 21, Maven (wrapper included), Node 22+, MySQL 8.0 (or Docker for `docker-compose.yml`)

## Local run (non-container)
1. Create `.env` from `.env.example` (git-ignored) and export its values in your shell.
2. Ensure MySQL 8.0 is running and the `MYSQL_DB` database and `MYSQL_USER` exist with privileges.
3. Backend: `cd backend && ./mvnw spring-boot:run` (port 8080; health: `GET /actuator/health`).
4. Frontend: `cd frontend && npm ci && npm run dev` (port 5173; proxies `/api`, `/oauth2`, `/login/oauth2`, `/ws`).

## Docker (MySQL only)
`docker compose up -d mysql` needs `MYSQL_PASSWORD` and `MYSQL_ROOT_PASSWORD` in `.env`. Stop a local MySQL on 3306 first, or set `MYSQL_PORT`.

## Tests
- Backend: `cd backend && ./mvnw verify` (requires Docker for Testcontainers).
- Frontend: `cd frontend && npm run lint && npm run format:check && npm run build`.
