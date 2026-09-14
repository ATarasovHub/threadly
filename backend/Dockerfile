# Build and runtime are separate stages so the shipped image carries no JDK, Gradle cache or
# source — only a JRE and the layered application.
FROM eclipse-temurin:21-jdk-alpine AS build
WORKDIR /workspace

# Copy the wrapper and build files first: these change rarely, so the dependency download below
# stays cached across source-only rebuilds.
COPY gradlew ./
COPY gradle ./gradle
COPY settings.gradle.kts build.gradle.kts ./
RUN chmod +x gradlew && ./gradlew --no-daemon dependencies > /dev/null 2>&1 || true

COPY src ./src
# Tests need a Docker daemon (Testcontainers) and run in CI instead.
RUN ./gradlew --no-daemon bootJar -x test

# Unpack the fat jar so dependencies and application classes land in separate image layers;
# redeploying then ships only the small layer that actually changed.
RUN mkdir -p build/extracted && java -Djarmode=tools -jar build/libs/*.jar extract --destination build/extracted

FROM eclipse-temurin:21-jre-alpine AS runtime
WORKDIR /app

# Never run as root.
RUN addgroup -S threadly && adduser -S threadly -G threadly
USER threadly

COPY --from=build --chown=threadly:threadly /workspace/build/extracted/ ./

EXPOSE 8080

# Container-aware heap sizing; the JVM reads the cgroup limit rather than the host's memory.
ENV JAVA_OPTS="-XX:MaxRAMPercentage=75"

ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS -jar threadly-backend.jar"]
