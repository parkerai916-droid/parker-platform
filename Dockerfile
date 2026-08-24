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

# Docling Production Runtime Enablement. A self-contained Python 3.12 +
# Docling (CPU-only) virtual environment, built on `ubuntu:22.04` --
# byte-identical to the runtime stage's own `eclipse-temurin:17-jre-jammy`
# base OS (both are Ubuntu 22.04 "Jammy") -- so the compiled native
# extensions below (torch, onnxruntime, opencv, pypdfium2) are guaranteed
# glibc/ABI-compatible with the stage they are COPY'd into. This is the
# reason the host's own already-provisioned `~/docling-venv` is never
# bind-mounted as a shortcut: fresh inspection of that venv found its
# `python3` symlink resolves outside the venv entirely (to a `uv`-managed
# interpreter under `~/.local/share/uv/`, itself absent from any
# container), and the host OS is Ubuntu 26.04 -- two Jammy major versions
# newer than this image's own base, with no forward glibc-compatibility
# guarantee. Building fresh, on the same base OS the runtime stage itself
# uses, avoids both problems entirely rather than working around them.
#
# `python3.12` is not in Ubuntu 22.04's own default repository (Jammy
# ships 3.10); the deadsnakes PPA (`ppa:deadsnakes/ppa`, the long-established,
# widely-used community source for Ubuntu Python builds) is added for this
# one build stage only -- it never reaches the final runtime stage's own
# image or apt sources. `--copies` (rather than the default symlink-based
# venv) makes the venv self-contained: without it, `bin/python3.12` would
# symlink to this build stage's own system interpreter, which does not
# exist in the runtime stage COPY targets it.
FROM ubuntu:22.04 AS docling-build
ENV DEBIAN_FRONTEND=noninteractive
RUN apt-get update \
    && apt-get install -y --no-install-recommends software-properties-common ca-certificates gnupg \
    && add-apt-repository -y ppa:deadsnakes/ppa \
    && apt-get update \
    && apt-get install -y --no-install-recommends python3.12 python3.12-venv \
    && rm -rf /var/lib/apt/lists/*
RUN python3.12 -m venv --copies /opt/docling-venv
WORKDIR /docling-build
COPY tools/docling-requirements.txt ./
# torch/torchvision installed first, from PyTorch's own dedicated CPU wheel
# index, pinned to the exact versions already proven -- `pip install torch`
# alone resolves the default CUDA-enabled wheel (~5 GB of nvidia-*
# packages) even on a GPU-less build machine; installing the CPU build
# first means the requirements-file install below finds it already
# satisfied rather than silently upgrading to CUDA. See
# tools/docling-requirements.txt's own header for the opencv-python
# (not headless) rationale.
RUN /opt/docling-venv/bin/pip install --no-cache-dir --upgrade pip \
    && /opt/docling-venv/bin/pip install --no-cache-dir \
        --index-url https://download.pytorch.org/whl/cpu \
        torch==2.13.0+cpu torchvision==0.28.0+cpu \
    && /opt/docling-venv/bin/pip install --no-cache-dir -r docling-requirements.txt

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

# Docling Production Runtime Enablement. Native runtime libraries
# opencv-python's compiled extension actually dlopens at import time,
# verified via `ldd` against the real, proven venv -- no X server,
# display, window manager, or desktop environment package among them; the
# rest of what `ldd` reports (Qt5, ffmpeg codecs, libpng/libavif, OpenBLAS)
# is bundled inside the wheel's own `.libs` directory already and needs no
# system package. `--no-install-recommends` keeps this to exactly the
# shared libraries below, nothing pulled in as a "recommended" extra.
RUN apt-get update \
    && apt-get install -y --no-install-recommends \
        libgl1 \
        libglib2.0-0 \
        libx11-6 \
        libxcb1 \
        libxau6 \
        libxdmcp6 \
    && rm -rf /var/lib/apt/lists/*

# The self-contained Python 3.12 + Docling (CPU-only) venv built in the
# docling-build stage above, on the same Ubuntu 22.04 Jammy base as this
# stage -- COPY'd, never bind-mounted from the host, for the glibc/ABI
# reasons documented on that stage's own header comment. `python -m venv`,
# even with `--copies`, does not duplicate the interpreter's own standard
# library (`sys.base_prefix`) -- only site-packages lives fully inside the
# venv directory itself; the stdlib remains the system Python installation
# it was created from (confirmed empirically: without this second COPY,
# the venv's own python3.12 fails at startup with "No module named
# 'encodings'"). Both COPYs together are what makes this venv actually
# self-contained across the stage boundary.
# --chown at COPY time, not a later `chown -R`: overlay2 (this daemon's
# storage driver) records a `chown -R` over a large tree as a fresh
# copy-on-write of every file it touches, which was observed (via `docker
# history`) to nearly double this layer's own reported size (~1.9 GB
# duplicated). Setting ownership during the COPY itself avoids that
# entirely -- confirmed via a rebuild that the resulting image is smaller.
COPY --chown=parker:parker --from=docling-build /opt/docling-venv /opt/docling-venv
COPY --from=docling-build /usr/lib/python3.12 /usr/lib/python3.12

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
RUN mkdir -p /data/evidence \
    /data/evidence-audit \
    /data/evidence-source-manifests \
    /data/derivative-generations \
    /data/document-ingestion-audit \
    /data/memory-core \
    /data/knowledge-items \
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
