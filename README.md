# Task Manager

A full-stack example app: a **TypeScript + React** frontend talking to a **Java Spring Boot** REST API backed by a **SQL database**.

```
frontend/   React 19 + TypeScript + Vite
backend/    Spring Boot 4 (Java 21), Spring Data JPA, Flyway, Bean Validation
```

## Features

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

Open http://localhost:5173.

### Using PostgreSQL instead of H2

```bash
docker compose up -d                       # starts PostgreSQL on localhost:5432
cd backend
./mvnw spring-boot:run -Dspring-boot.run.profiles=postgres
```

Connection settings can be overridden with `DB_URL`, `DB_USERNAME` and `DB_PASSWORD`.

While running on H2, the database console is at http://localhost:8080/h2-console
(JDBC URL `jdbc:h2:mem:taskmanager`, user `sa`, empty password).

## REST API

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

Example:

```bash
curl -X POST localhost:8080/api/tasks \
  -H 'Content-Type: application/json' \
  -d '{"title": "Try the API", "projectId": 1, "priority": "HIGH"}'
```

Validation errors return `400` with per-field messages:

```json
{ "status": 400, "detail": "Validation failed", "errors": { "title": "must not be blank" } }
```

## Tests and builds

```bash
cd backend && ./mvnw test          # API integration tests (MockMvc + H2)
cd frontend && npm run build       # type-check and production build
```

## Configuration

| Setting                | Where                         | Default                  |
|------------------------|-------------------------------|--------------------------|
| `CORS_ALLOWED_ORIGINS` | backend env var               | `http://localhost:5173`  |
| `VITE_API_BASE_URL`    | frontend env (`.env.local`)   | empty (uses Vite proxy)  |
