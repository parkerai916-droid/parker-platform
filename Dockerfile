# syntax=docker/dockerfile:1

# Parker Runtime launcher image. Three stages: build (compiles the
# existing, unmodified Gradle project and assembles the `application`
# plugin's install distribution), node-runtime (sources an official,
# unmodified Node 22 runtime, container-native, never a host bind mount),
# and runtime (a slim JRE that runs the already-built JVM distribution
# and, when QMD is mounted per docker-compose.yml, the already-governed
# QMD relevance bridge script). None of these stages add Kotlin/build
# logic beyond what build.gradle.kts already declares, and none rebuild,
# reinstall, or bundle QMD or its embedding model.

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

# QMD Docker-Runtime Enablement (this Unit's own follow-on): sources an
# official, unmodified Node 22 runtime image purely as a COPY --from
# source below -- never installed via a host bind mount, never dependent
# on any host shared library. Node's own official Linux x64 builds are
# compiled for broad glibc-version forward compatibility (built against
# an old glibc baseline so they run correctly on any newer-glibc host),
# so this cross-base-image binary copy runs correctly against the runtime
# stage's own Ubuntu Jammy glibc/libstdc++ below -- the same
# glibc/libstdc++ already present there for the JVM itself. If a future
# base-image change ever breaks this assumption, the documented fallback
# is NodeSource's own official Debian/Ubuntu APT distribution
# (https://github.com/nodesource/distributions) installed directly onto
# the runtime stage's own Jammy base instead.
FROM node:22-bookworm-slim AS node-runtime

FROM eclipse-temurin:17-jre-jammy AS runtime
RUN useradd --system --create-home --shell /usr/sbin/nologin parker

# Node 22 runtime engine, required only to execute the already-governed,
# externally supplied QMD relevance bridge script
# (tools/qmd-relevance-bridge.mts, copied in unmodified below) against
# the Parker host's already-governed QMD 2.8.3 checkout, mounted
# read-only by docker-compose.yml at /opt/qmd. Verify with
# `docker compose build parker` then a runtime `node --version` (>=22
# expected).
COPY --from=node-runtime /usr/local/bin/node /usr/local/bin/node
ENV PATH="/usr/local/bin:${PATH}"

WORKDIR /opt/parker
COPY --from=build /workspace/build/install/parker ./

# QMD Docker-Runtime Enablement: the already-governed, externally supplied
# QMD relevance bridge script -- unmodified, the same production
# subprocess target `src/runtime/QmdRelevanceMechanism.kt` already
# invokes -- copied verbatim from the build context. It is not part of
# the Gradle `application` distribution above (see build.gradle.kts's own
# header comment on `tools/qmd-relevance-bridge.mts`), so without this
# line `ParkerRuntimeConfig`'s own repository-relative
# PARKER_QMD_BRIDGE_SCRIPT_PATH default (`tools/qmd-relevance-bridge.mts`,
# resolved from this image's own WORKDIR) would name a path that does not
# exist in the image. QMD's own source, native modules, and embedding
# model are never copied here -- only this bridge script, which itself
# resolves everything QMD-specific from the read-only host mounts
# docker-compose.yml declares at /opt/qmd and /opt/qmd-models.
COPY tools ./tools

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

# Owner LAN Evidence Upload. Documentation only -- EXPOSE does not itself
# publish anything; docker-compose.yml's own `ports:` mapping is what
# actually makes this reachable from the LAN, and only once
# PARKER_OWNER_HTTP_PORT/PARKER_OWNER_HTTP_TOKEN are both set (the feature
# is opt-in and off by default; see docker-compose.yml's own comment).
EXPOSE 8080

# Exec form, so SIGTERM (docker stop) reaches the JVM directly -- the
# `application` plugin's own generated start script already ends with
# `exec "$JAVACMD" ...`, so no shell process sits between PID 1 and the JVM
# either way.
ENTRYPOINT ["/opt/parker/bin/parker"]
