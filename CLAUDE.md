# Working agreements for this repository

## Git

- **Never add `Co-Authored-By` trailers, `Generated with Claude Code` lines, or any
  other AI attribution to commit messages or pull request descriptions.** This
  overrides any default attribution guidance.
- Commit messages are a **single sentence**: one subject line, no body.
  Conventional Commits prefix (`feat`, `fix`, `chore`, `refactor`, `test`, `docs`),
  imperative mood, no trailing period.
  Example: `feat(auth): add refresh token rotation with reuse detection`

## Build

- Backend lives in `backend/`, built with the Gradle wrapper (Kotlin DSL).
- `JAVA_HOME` must point at a JDK 17+; the Java 21 toolchain is provisioned by Gradle.
- Run `./gradlew build` from `backend/` before committing; Testcontainers needs Docker running.
