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

## Running the backend

The Gradle wrapper provisions the Java 21 toolchain automatically.

```bash
cd backend
./gradlew bootRun
```

The application expects a PostgreSQL instance; see `docker-compose.yml` (added in the next step).

## Roadmap

- [x] Project skeleton
- [ ] Docker Compose infrastructure
- [ ] Authentication (registration, JWT, roles)
- [ ] Profiles
- [ ] Posts, replies, reposts
- [ ] Follow graph
- [ ] Feeds with cursor pagination
- [ ] Likes, bookmarks
- [ ] Notifications
- [ ] React frontend
- [ ] Production hardening (OpenAPI, rate limiting, metrics)
