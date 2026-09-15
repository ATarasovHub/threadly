# One image serving both the API and the client, built from the repository root.
#
# The client is compiled first and dropped into the backend's static resources, so the whole
# application ships as a single artifact on a single origin. That is what lets the refresh cookie
# stay SameSite=Strict in production and keeps CORS out of the picture entirely.

FROM node:22-alpine AS client
WORKDIR /client

COPY frontend/package.json frontend/package-lock.json ./
# npm ci fails on any drift from the lockfile, which npm install would quietly paper over.
RUN npm ci

COPY frontend/ ./
# Empty base URL means "same origin": in production the API is served from this very host.
ENV VITE_API_BASE_URL=""
# Supplied at build time because Vite inlines it; an empty value simply hides the Google button.
ARG VITE_GOOGLE_CLIENT_ID=""
ENV VITE_GOOGLE_CLIENT_ID=$VITE_GOOGLE_CLIENT_ID
RUN npm run build

FROM eclipse-temurin:21-jdk-alpine AS server
WORKDIR /workspace

# Build files first so the dependency layer survives source-only rebuilds.
COPY backend/gradlew ./
COPY backend/gradle ./gradle
COPY backend/settings.gradle.kts backend/build.gradle.kts ./
RUN chmod +x gradlew && ./gradlew --no-daemon dependencies > /dev/null 2>&1 || true

COPY backend/src ./src
COPY --from=client /client/dist/ ./src/main/resources/static/
# Tests need a Docker daemon for Testcontainers and run in CI instead.
RUN ./gradlew --no-daemon bootJar -x test

# Split dependencies and application classes into separate image layers, so a redeploy ships only
# what changed.
RUN mkdir -p build/extracted \
    && java -Djarmode=tools -jar build/libs/threadly-backend.jar extract --destination build/extracted

FROM eclipse-temurin:21-jre-alpine AS runtime
WORKDIR /app

RUN addgroup -S threadly && adduser -S threadly -G threadly
USER threadly

COPY --from=server --chown=threadly:threadly /workspace/build/extracted/ ./

EXPOSE 8080

ENV SPRING_PROFILES_ACTIVE=prod
# Container-aware sizing: the JVM reads the cgroup limit rather than the host's memory.
ENV JAVA_OPTS="-XX:MaxRAMPercentage=75"

ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS -jar threadly-backend.jar"]
