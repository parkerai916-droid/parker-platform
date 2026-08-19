# syntax=docker/dockerfile:1

# Parker Runtime launcher image. Two stages: build (compiles the existing,
# unmodified Gradle project and assembles the `application` plugin's
# install distribution) and runtime (a slim JRE that only ever runs that
# already-built distribution). Neither stage adds Kotlin/build logic
# beyond what build.gradle.kts already declares.

FROM eclipse-temurin:17-jdk-jammy AS build
WORKDIR /workspace

# Gradle wrapper and build config first, so dependency resolution is
# cached across rebuilds that only change src/.
COPY gradlew ./
COPY gradle ./gradle
COPY settings.gradle.kts build.gradle.kts ./
RUN ./gradlew --version --no-daemon

COPY src ./src
RUN ./gradlew installDist --no-daemon

FROM eclipse-temurin:17-jre-jammy AS runtime
RUN useradd --system --create-home --shell /usr/sbin/nologin parker
WORKDIR /opt/parker
COPY --from=build /workspace/build/install/parker ./

# Evidence Custodian storage/audit mount points, plus Memory Core
# Durability Unit 9's own memory-core mount point (docker-compose.yml's
# evidence-storage/evidence-audit/memory-core-durability volumes). Created
# and owned by `parker` here, before USER switches below, so that on first
# `docker compose up` -- when Docker initialises each named volume from the
# image's own directory content -- the volume comes up already owned by the
# non-root user the process actually runs as, not root.
RUN mkdir -p /data/evidence /data/evidence-audit /data/memory-core /data/knowledge-items \
    && chown -R parker:parker /opt/parker /data
USER parker

# Exec form, so SIGTERM (docker stop) reaches the JVM directly -- the
# `application` plugin's own generated start script already ends with
# `exec "$JAVACMD" ...`, so no shell process sits between PID 1 and the JVM
# either way.
ENTRYPOINT ["/opt/parker/bin/parker"]
