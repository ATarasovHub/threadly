# Threadly

A Twitter/X-style social network built as a full-stack portfolio project.

## Stack

| Layer      | Technology                                                        |
|------------|-------------------------------------------------------------------|
| Backend    | Java 21, Spring Boot 4.1, Spring Web MVC, Spring Security, Spring Data JPA |
| Database   | PostgreSQL 17, Flyway migrations                                   |
| Frontend   | React, TypeScript, Vite, TanStack Query, Tailwind CSS              |
| Testing    | JUnit 5, Testcontainers                                            |
| Infra      | Docker Compose, GitHub Actions                                     |

## Layout

```
threadly/
├── backend/    Spring Boot application (Gradle, Kotlin DSL)
└── frontend/   React + TypeScript client (added in a later step)
```

## Running locally

Start the infrastructure (PostgreSQL 17):

```bash
docker compose up -d
```

Then run the backend — the Gradle wrapper provisions the Java 21 toolchain automatically:

```bash
cd backend
./gradlew bootRun
```

The API is served on http://localhost:8080 and the health probe on
http://localhost:8080/actuator/health.

Configuration is read from environment variables with local defaults; copy
`.env.example` to `.env` to override them.

## API so far

| Method | Path                     | Auth   | Purpose                                   |
|--------|--------------------------|--------|-------------------------------------------|
| POST   | `/api/v1/auth/register`  | —      | Create an account                         |
| POST   | `/api/v1/auth/login`     | —      | Exchange credentials for an access token  |
| POST   | `/api/v1/auth/refresh`   | cookie | Rotate the refresh token, get a new access token |
| POST   | `/api/v1/auth/logout`    | cookie | Revoke the session                        |
| GET    | `/api/v1/me`             | bearer | The authenticated account                 |

Access tokens are short-lived JWTs sent as `Authorization: Bearer`. The refresh token lives
only in an HttpOnly, SameSite=Strict cookie, is stored server-side as a SHA-256 hash, and is
rotated on every use; replaying a rotated token revokes the whole session family.

## Roadmap

- [x] Project skeleton
- [x] Docker Compose infrastructure
- [x] Authentication (registration, JWT, refresh tokens, roles)
- [ ] Profiles
- [ ] Posts, replies, reposts
- [ ] Follow graph
- [ ] Feeds with cursor pagination
- [ ] Likes, bookmarks
- [ ] Notifications
- [ ] React frontend
- [ ] Production hardening (OpenAPI, rate limiting, metrics)
