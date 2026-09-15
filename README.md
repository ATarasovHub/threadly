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
└── frontend/   React + TypeScript client (Vite, TanStack Query, Tailwind)
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

The API is served on http://localhost:8080, its description on
http://localhost:8080/swagger-ui.html, and the health probe on
http://localhost:8080/actuator/health.

Then the client:

```bash
cd frontend
npm install
npm run dev
```

It runs on http://localhost:5173, which is the origin the backend allows through CORS by
default.

Configuration is read from environment variables with local defaults; copy
`.env.example` to `.env` to override them.

## API so far

| Method | Path                     | Auth   | Purpose                                   |
|--------|--------------------------|--------|-------------------------------------------|
| POST   | `/api/v1/auth/register`  | —      | Create an account                         |
| POST   | `/api/v1/auth/login`     | —      | Exchange credentials for an access token  |
| POST   | `/api/v1/auth/google`    | —      | Exchange a Google ID token for a session   |
| POST   | `/api/v1/auth/refresh`   | cookie | Rotate the refresh token, get a new access token |
| POST   | `/api/v1/auth/logout`    | cookie | Revoke the session                        |
| GET    | `/api/v1/me`             | bearer | The authenticated account                 |
| GET    | `/api/v1/users/{handle}` | bearer | A public profile, matched case-insensitively |
| PATCH  | `/api/v1/me/profile`     | bearer | Edit your own profile                     |
| PATCH  | `/api/v1/me/username`    | bearer | Change your handle                        |
| POST   | `/api/v1/posts`          | bearer | Publish a post                            |
| GET    | `/api/v1/posts/{id}`     | bearer | Read a post                               |
| PATCH  | `/api/v1/posts/{id}`     | bearer | Edit your own post                        |
| DELETE | `/api/v1/posts/{id}`     | bearer | Soft-delete your own post                 |
| GET    | `/api/v1/users/{handle}/posts` | bearer | An author's timeline, cursor-paginated |
| POST   | `/api/v1/users/{handle}/follow` | bearer | Follow (idempotent)                |
| DELETE | `/api/v1/users/{handle}/follow` | bearer | Unfollow (idempotent)              |
| GET    | `/api/v1/users/{handle}/followers` | bearer | Followers, cursor-paginated     |
| GET    | `/api/v1/users/{handle}/following` | bearer | Following, cursor-paginated     |

Collections are paginated by cursor, never by offset: `?cursor=<token>&limit=20`. The token
encodes the `(createdAt, id)` of the last row served, so rows written mid-pagination cannot
shift the window and cause duplicates or gaps. A response carries `nextCursor`, or `null` on
the last page.

Access tokens are short-lived JWTs sent as `Authorization: Bearer`. The refresh token lives
only in an HttpOnly, SameSite=Strict cookie, is stored server-side as a SHA-256 hash, and is
rotated on every use; replaying a rotated token revokes the whole session family.

## Google sign-in

Optional and off unless configured. Create an OAuth 2.0 Client ID of type *Web application* in
the [Google Cloud Console](https://console.cloud.google.com/apis/credentials), add
`http://localhost:5173` as an authorised JavaScript origin, then set the same id in two places:

```bash
# backend
GOOGLE_CLIENT_ID=<client-id>.apps.googleusercontent.com
# frontend/.env
VITE_GOOGLE_CLIENT_ID=<client-id>.apps.googleusercontent.com
```

The browser receives a Google ID token and forwards it to the API, which verifies its signature
against Google's published keys and checks the issuer and audience before trusting anything in
it. Google only establishes identity: the session, the access token and the rotating refresh
cookie are Threadly's own either way.

An existing account is linked when the Google address matches it **and** Google reports the
address as verified. Without that second condition, anyone able to register an address at Google
could claim the Threadly account using it.

## Roadmap

- [x] Project skeleton
- [x] Docker Compose infrastructure
- [x] Authentication (registration, JWT, refresh tokens, roles)
- [x] Profiles
- [x] Posts (replies and reposts still to come)
- [x] Follow graph
- [x] Feeds with cursor pagination
- [x] Likes, bookmarks, replies, reposts
- [x] Notifications
- [x] React frontend
- [x] Production hardening (OpenAPI, rate limiting, Docker)
