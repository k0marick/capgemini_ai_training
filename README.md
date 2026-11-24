# Personal To-Do Manager (TaskFlow)

Projects (lists), tasks with status, priority and due dates, search and due-date filters, and real-time
notifications for overdue and due-soon tasks. Sign-in with Google or GitHub. Each user sees only their own data.

Stack: Java 21, Spring Boot 4 (MVC, Security OAuth2 client, WebSocket, JPA, Flyway), MySQL 8.0,
React 19, TypeScript, Vite, TanStack Query, React Router, React Hook Form + Zod.

Docker is not used for local development (ADR-007). An optional backend image is in `backend/Dockerfile`.

Design docs: `docs/requirements.md`, `docs/architecture.md`, `docs/adr/`.

## Prerequisites

- Java 21 (the Maven wrapper is included; no global Maven needed)
- Node.js 22+
- MySQL 8.0 running locally
- On Windows, run the `./mvnw` and `npm` commands from Git Bash (the Maven wrapper is a shell script)

## 1. Database

Run as a MySQL administrator:

```sql
CREATE DATABASE todo_manager;
CREATE DATABASE todo_manager_test;
CREATE DATABASE todo_manager_e2e;
CREATE USER 'todo_user'@'%' IDENTIFIED BY 'choose-a-password';
GRANT ALL ON todo_manager.* TO 'todo_user'@'%';
GRANT ALL ON todo_manager_test.* TO 'todo_user'@'%';
GRANT ALL ON todo_manager_e2e.* TO 'todo_user'@'%';
```

Flyway creates and migrates the tables at backend startup. Only `todo_manager` is needed to run the app;
the other two are for tests.

## 2. Configuration

1. Copy `.env.example` to `.env` (git-ignored) and fill in `MYSQL_PASSWORD` and the OAuth values.
2. Load it into your shell before starting the backend:
   `set -a; source .env; set +a`
3. `SPRING_PROFILES_ACTIVE=dev` enables Swagger UI and the sample-data endpoint.

### OAuth providers (required: the backend does not start with empty client IDs)

Google (OpenID Connect):

1. Google Cloud Console > APIs & Services > Credentials > Create OAuth client ID > Web application.
2. Authorized JavaScript origin: `http://localhost:5173`
3. Authorized redirect URI: `http://localhost:5173/login/oauth2/code/google`
4. Put the client ID and secret into `GOOGLE_CLIENT_ID` and `GOOGLE_CLIENT_SECRET`.

GitHub:

1. GitHub > Settings > Developer settings > OAuth Apps > New OAuth App.
2. Homepage URL: `http://localhost:5173`
3. Authorization callback URL: `http://localhost:5173/login/oauth2/code/github`
4. Put the client ID and secret into `GITHUB_CLIENT_ID` and `GITHUB_CLIENT_SECRET`.

Requests go through the Vite dev server (port 5173), which proxies `/oauth2`, `/login/oauth2`, `/api` and
`/ws` to the backend. Always open the app at `http://localhost:5173`, not 8080.

Never commit real client secrets.

## 3. Run locally

```bash
# Terminal 1 - backend (port 8080)
cd backend
./mvnw spring-boot:run

# Terminal 2 - frontend (port 5173)
cd frontend
npm ci
npm run dev
```

Then open `http://localhost:5173` and sign in.

Useful URLs (dev profile, backend port 8080):

- Health: `http://localhost:8080/actuator/health`
- Swagger UI: `http://localhost:8080/swagger-ui/index.html`
- OpenAPI JSON: `http://localhost:8080/v3/api-docs`

In dev, the sidebar has a **Load sample data (dev)** button that creates two lists and six tasks.

Production profile (`SPRING_PROFILES_ACTIVE=prod`): Swagger is off, session cookies are `Secure`, and
stack traces are never returned. Serve it over HTTPS.

## 4. Tests

```bash
# Backend: unit and integration tests (needs todo_manager_test; uses a WireMock OAuth stub, no provider credentials)
cd backend && ./mvnw verify

# Frontend: static checks, unit tests, production build
cd frontend
npm run lint
npm run typecheck
npm run format:check
npm test -- --run
npm run build

# End-to-end: Playwright, stub OAuth providers (needs todo_manager_e2e)
cd frontend
npx playwright install chromium   # first time only
npm run e2e
```

To use a different schema locally, set `E2E_MYSQL_DB` (for example `E2E_MYSQL_DB=todo_manager_test npm run e2e`).
`npm run e2e` starts three processes and waits for each: the OAuth stub on port 9999 (`frontend/e2e/stub-providers.mjs`),
the backend with the `e2e` profile on port 8081, and the Vite dev server on port 5173. None of them uses real
provider credentials. The E2E backend deliberately uses a separate port so it cannot reuse a backend started with
other settings.

CI (`.github/workflows/ci.yml`) runs validate, backend tests, frontend tests, build, and E2E on each push and pull request.

## Business rules

Dates are evaluated in the user's timezone (default UTC). Categories for a task with due date `D` and today `T`:

| Category    | Rule                                                       |
| ----------- | ---------------------------------------------------------- |
| Overdue     | `D < T` and status is not `DONE`                           |
| Today       | `D = T`                                                    |
| Due soon    | `T <= D <= T+3` and status is not `DONE`                   |
| Next 7 days | `T <= D <= T+6`                                            |
| All         | no restriction (tasks without a due date match only "All") |

Notifications use the "Due soon" window above.

- A project can be deleted only when it has no tasks (`409` otherwise).
- Edits use optimistic locking: send the current `version`; a stale value returns `409`.
- Search is case-insensitive over title and description; `%` and `_` are matched literally.

## REST API

All endpoints are under `/api/v1`, require a session cookie except `GET /csrf`, and require the `X-XSRF-TOKEN`
header (value from the `XSRF-TOKEN` cookie) on `POST`, `PATCH` and `DELETE`. Errors use `application/problem+json`.

| Method               | Path                   | Notes                                                        |
| -------------------- | ---------------------- | ------------------------------------------------------------ |
| GET                  | `/me`                  | Current user                                                 |
| GET                  | `/csrf`                | Issues the CSRF token                                        |
| PATCH                | `/me/preferences`      | `{ timezone }`                                               |
| POST                 | `/logout`              | 204; closes notification sockets                             |
| GET / POST           | `/projects`            | List with `taskCount`; create `{ name }` (201)               |
| GET / PATCH / DELETE | `/projects/{id}`       | Delete returns 409 if tasks exist                            |
| GET                  | `/tasks`               | `projectId, search, status, priority, due, page, size, sort` |
| POST                 | `/projects/{id}/tasks` | 201                                                          |
| GET / PATCH / DELETE | `/tasks/{id}`          | PATCH requires `version`                                     |
| PATCH                | `/tasks/{id}/status`   | `{ status, version }`                                        |
| POST                 | `/dev/sample-data`     | dev profile only                                             |

Lists return `{ content, page, size, totalElements, totalPages }`. `size` defaults to 20 and is capped at 100.
Sort fields: `dueDate, priority, createdAt, title, status`. `due` is `OVERDUE | TODAY | NEXT_7_DAYS | ALL`.

## Notifications WebSocket

Endpoint: `ws://localhost:5173/ws/notifications` (or `wss://` in production). Text frames, JSON envelope
`{ "type": "...", "payload": { ... } }`. The browser must have a valid session cookie and an allowed `Origin`;
otherwise the handshake is rejected with 403 (bad origin) or 401 (no session). Frames are capped at 4 KB.

Client to server:

```json
{ "type": "SUBSCRIBE", "payload": { "enabledTypes": ["OVERDUE", "DUE_SOON"] } }
```

`enabledTypes` is a subset of `OVERDUE` and `DUE_SOON` with no duplicates. An empty array turns notifications off.
Both types are on until the first `SUBSCRIBE`. No identity fields are accepted.

Server to client:

```json
{ "type": "SUBSCRIPTION_UPDATED", "payload": { "enabledTypes": ["OVERDUE"] } }
```

```json
{
  "type": "NOTIFICATION_BATCH",
  "payload": {
    "notifications": [
      {
        "type": "OVERDUE",
        "taskId": "...",
        "projectId": "...",
        "title": "Review PR",
        "dueDate": "2026-11-22",
        "priority": "HIGH"
      }
    ]
  }
}
```

```json
{
  "type": "ERROR",
  "payload": {
    "code": "INVALID_PAYLOAD",
    "message": "enabledTypes contains unknown value"
  }
}
```

Error codes: `INVALID_JSON`, `UNKNOWN_TYPE`, `INVALID_PAYLOAD`, `RATE_LIMITED` (the socket then closes with 1008).
Oversized frames close with 1009. Logout or session expiry closes the socket with 4401 (`session-ended`);
the client does not reconnect after 4401.

The server scans for due and overdue tasks every `NOTIFICATION_INTERVAL_MS` (default 60000; the E2E profile uses 2000).
A batch is sent only when its contents change.

## Project layout

```
backend/   Spring Boot API, grouped by feature (auth, user, project, task, notification, dev)
           src/main/resources/db/migration  Flyway migrations
frontend/  React SPA (features/*), Vitest unit tests, frontend/e2e Playwright tests and OAuth stub
docs/      Requirements, architecture, ADRs
```

## Troubleshooting

- **`redirect_uri_mismatch` on sign-in**: the redirect URI in the provider console must match
  `http://localhost:5173/login/oauth2/code/<provider>` exactly.
- **Sign-in fails and returns to `/login` with an error**: check the backend log; usually a wrong client secret or a missing scope.
- **Backend cannot connect to MySQL**: check `MYSQL_*` values and that the schema exists and `todo_user` has access to it.
- **Port already in use**: stop the other process, or change the port in `application.yml` and `vite.config.ts`.
