# Task Manager

A full-stack example app: a **TypeScript + React** frontend talking to a **Java Spring Boot** REST API backed by a **SQL database**.

```
frontend/   React 19 + TypeScript + Vite
backend/    Spring Boot 4 (Java 21), Spring Data JPA, Flyway, Bean Validation
```

## Features

- Sign up / sign in / sign out with Spring Security; every user sees only their own projects and tasks
- Projects and tasks (one-to-many relationship in SQL)
- Full CRUD REST API with validation and RFC 9457 `problem+json` errors
- Task filtering by project and status, quick status changes, inline editing
- Schema managed with Flyway migrations (`backend/src/main/resources/db/migration`)
- Runs on in-memory **H2** out of the box, or **PostgreSQL** with one flag

## Prerequisites

- Java 21+
- Node.js 20+
- (Optional) Docker, for PostgreSQL

## Running locally

**1. Start the backend** (port 8080, in-memory H2 with sample data):

```bash
cd backend
./mvnw spring-boot:run
```

**2. Start the frontend** (port 5173, proxies `/api` to the backend):

```bash
cd frontend
npm install
npm run dev
```

Open http://localhost:5173 and sign in with the demo account, **`demo@example.com` / `password123`**,
which owns the sample data, or create your own account.

### Using PostgreSQL instead of H2

```bash
docker compose up -d                       # starts PostgreSQL on localhost:5432
cd backend
./mvnw spring-boot:run -Dspring-boot.run.profiles=postgres
```

Connection settings can be overridden with `DB_URL`, `DB_USERNAME` and `DB_PASSWORD`.

While running on H2, the database console is at http://localhost:8080/h2-console
(JDBC URL `jdbc:h2:mem:taskmanager`, user `sa`, empty password).

## Authentication

Sign-in uses a **server-side session** (Spring Security) rather than tokens in browser storage:

- `POST /api/auth/login` checks the password and stores the user in the HTTP session. The browser
  receives a `JSESSIONID` cookie that is `HttpOnly` (unreadable by JavaScript) and `SameSite=Lax`.
- Passwords are hashed with **BCrypt** (stored as `{bcrypt}…`, so the algorithm can be upgraded later).
- **CSRF protection**: the server sends a readable `XSRF-TOKEN` cookie, and every `POST`/`PUT`/`PATCH`/`DELETE`
  must echo it in an `X-XSRF-TOKEN` header. `frontend/src/api/client.ts` does this automatically.
- A new session id is issued on sign-in (prevents session fixation), and sessions expire after 8 hours idle.
- Wrong password and unknown email return the same `401 Invalid email or password`.
- **Failed sign-ins are rate limited** (see below).
- Projects have an `owner_id`; every query is filtered by the signed-in user, and other users' records
  return `404` rather than `403` so their ids don't leak.

| Method | Path                  | Auth | Description                                         |
|--------|-----------------------|------|-----------------------------------------------------|
| POST   | `/api/auth/register`  | no   | `{email, password, displayName}` → creates the account and signs in |
| POST   | `/api/auth/login`     | no   | `{email, password}` → signs in (`429` when rate limited) |
| POST   | `/api/auth/logout`    | yes  | Ends the session (`204`)                            |
| GET    | `/api/auth/me`        | yes  | The signed-in user, or `401`                        |

Calling the API with `curl` (keeps cookies in a jar and sends the CSRF token):

```bash
curl -s -c jar -b jar localhost:8080/api/auth/me > /dev/null          # get the XSRF-TOKEN cookie
TOKEN=$(awk '/XSRF-TOKEN/ {print $7}' jar)
curl -s -c jar -b jar -H "X-XSRF-TOKEN: $TOKEN" -H 'Content-Type: application/json' \
  -d '{"email": "demo@example.com", "password": "password123"}' localhost:8080/api/auth/login
curl -s -b jar localhost:8080/api/projects
```

### Sign-in attempt limits

`LoginAttemptLimiter` counts **failed** sign-ins over a 15-minute window:

| Key            | Limit | Stops                                             |
|----------------|-------|---------------------------------------------------|
| IP + email     | 5     | Guessing one account's password                   |
| IP             | 20    | One client trying many accounts (password spraying) |

When a limit is hit, `POST /api/auth/login` returns **`429 Too Many Requests`** with a `Retry-After`
header (seconds) and `retryAfterSeconds` in the body, even if the password is right, and the
password is not checked. The sign-in page shows how many minutes to wait.

- Limits are keyed by IP as well as email, so an attacker can't lock the real owner out of
  their account from another network.
- A successful sign-in resets the IP + email counter but not the IP counter, so an attacker can't
  refill their budget by signing in to their own account between guesses.
- Change the limits with `app.security.login-attempts.*` in `application.yml`.
- Counters are kept in memory: they reset on restart and aren't shared between instances. If you
  run more than one backend instance, move them to a shared store such as Redis.
- **Behind a reverse proxy or load balancer**, every request appears to come from the proxy's IP,
  so all users would share one IP limit. Set `server.forward-headers-strategy=native` so the real
  client IP is read from `X-Forwarded-For`. Only do this behind a proxy you control, because clients
  can forge that header otherwise.

**Before deploying publicly:** delete or change the demo account (it is created by migration
`V3__add_users.sql`), serve over HTTPS, and set `SESSION_COOKIE_SECURE=true`.

## REST API

All project and task endpoints require a signed-in user and only operate on that user's data.

| Method | Path                       | Description                                      |
|--------|----------------------------|--------------------------------------------------|
| GET    | `/api/projects`            | List projects (with task / open-task counts)     |
| GET    | `/api/projects/{id}`       | Get a project                                    |
| POST   | `/api/projects`            | Create a project `{name, description?}`          |
| PUT    | `/api/projects/{id}`       | Update a project                                 |
| DELETE | `/api/projects/{id}`       | Delete a project and its tasks                   |
| GET    | `/api/tasks?projectId=&status=` | List tasks, optionally filtered             |
| GET    | `/api/tasks/{id}`          | Get a task                                       |
| POST   | `/api/tasks`               | Create a task                                    |
| PUT    | `/api/tasks/{id}`          | Replace a task                                   |
| PATCH  | `/api/tasks/{id}/status`   | Change only the status `{status}`                |
| DELETE | `/api/tasks/{id}`          | Delete a task                                    |

Task body:

```json
{
  "title": "Write docs",
  "description": "optional",
  "status": "TODO | IN_PROGRESS | DONE",
  "priority": "LOW | MEDIUM | HIGH",
  "dueDate": "2030-01-15",
  "projectId": 1
}
```

Example (after signing in as shown above):

```bash
curl -b jar -X POST localhost:8080/api/tasks \
  -H "X-XSRF-TOKEN: $TOKEN" -H 'Content-Type: application/json' \
  -d '{"title": "Try the API", "projectId": 1, "priority": "HIGH"}'
```

Validation errors return `400` with per-field messages:

```json
{ "status": 400, "detail": "Validation failed", "errors": { "title": "must not be blank" } }
```

## Tests and builds

```bash
cd backend && ./mvnw test          # API, auth, CSRF, rate-limit and data-isolation tests (H2)
cd frontend && npm run build       # type-check and production build
```

## Configuration

| Setting                | Where                         | Default                  |
|------------------------|-------------------------------|--------------------------|
| `CORS_ALLOWED_ORIGINS` | backend env var               | `http://localhost:5173`  |
| `SESSION_COOKIE_SECURE`| backend env var               | `false` (set `true` behind HTTPS) |
| `app.security.login-attempts.*` | `application.yml`    | 5 per IP + email, 20 per IP, 15 min window |
| `VITE_API_BASE_URL`    | frontend env (`.env.local`)   | empty (uses Vite proxy)  |
