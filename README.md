# Personal To-Do Manager

Status: P1 done, P2 (authentication) in progress. Full README (OAuth setup, WebSocket protocol) arrives in P6.

Docker is not used in this project (ADR-007).

## Requirements
- Java 21, Maven (wrapper included), Node 22+, MySQL 8.0

## Database
1. Create the app schema and user with access to it, e.g. `todo_manager` for local runs.
2. Create the test schema used by `./mvnw verify`:
   `CREATE DATABASE todo_manager_test; GRANT ALL ON todo_manager_test.* TO 'todo_user'@'%';`

## Configuration
Copy `.env.example` to `.env` (git-ignored) and export the values in your shell. Required for sign-in:
`GOOGLE_CLIENT_ID`, `GOOGLE_CLIENT_SECRET`, `GITHUB_CLIENT_ID`, `GITHUB_CLIENT_SECRET`.

## Run locally
- Backend: `cd backend && ./mvnw spring-boot:run` (port 8080; health: `GET /actuator/health`).
- Frontend: `cd frontend && npm ci && npm run dev` (port 5173; proxies `/api`, `/oauth2`, `/login/oauth2`, `/ws`).

## Tests
- Backend: `cd backend && ./mvnw verify`. Uses the `todo_manager_test` schema and a WireMock OAuth stub; no real provider credentials needed.
- Frontend: `cd frontend && npm run lint && npm run format:check && npm run build`.
