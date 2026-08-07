# Memory Core Durability — Unit 9: Container Durability — Completion Review

## Status

Completion Review for Memory Core Durability Implementation Unit 9 ("Container Durability"). Maps the durable storage location Unit 8 already wired into `ParkerRuntimeConfig`/`ParkerRuntime` to a Docker named volume, following the existing `evidence-storage`/`evidence-audit` two-volume precedent exactly. No governance document was modified. No Kotlin file was touched. Unit 10 was not begun.

---

## 1. Baseline

`HEAD` at task start: `02b5e31 feat: compose durable Memory Core into runtime` (Unit 8's own commit), `git status --short` clean. All eight prior Units' own Independent Constitutional Reviews were re-read for their verdict line directly (not recalled): all eight read `ACCEPTED`. No remaining work from Units 1–8 was found.

---

## 2. Planning Review Findings

- **Governing authority re-read fresh**: Implementation Plan's own "Unit 9 — Container Durability" section; Scope Lock (composition discipline, and its own explicit deferral of "Docker volume name, mount path, and `ParkerRuntimeConfig` field names" to the Implementation Plan); Contract Design §13 (Docker volume declarations deferred to the Implementation Plan, not fixed by Contract Design).
- **Unit 9 confirmed the lawful next step**: the Dependency Graph names Unit 9's own sole prerequisite as Unit 8, complete and `ACCEPTED`.
- **Docker/runtime changes authorised by Unit 9**, read directly from the Plan's own "Outputs" text: in `docker-compose.yml`, a new named volume literally named `memory-core-durability`, mounted at a fixed in-container path (the Plan's own example: `/data/memory-core`), with a new environment variable `PARKER_MEMORY_CORE_DURABILITY_LOG_PATH` pointing at a file inside that mount (the Plan's own example: `/data/memory-core/durability.log`), mirroring `PARKER_EVIDENCE_DELETION_AUDIT_LOG_PATH`'s own identical shape; in `Dockerfile`, the new mount-point directory added to the existing single `mkdir -p ... && chown -R parker:parker /opt/parker /data` line — explicitly, "no new `RUN` step." Unit 9's own "Test files expected to change" is explicitly **none** ("Docker configuration has no Kotlin test file; verified instead by Unit 10's own Docker-restart verification step").
- **No governance conflict found.** No stop condition applies (the Plan's own text: "None identified — this Unit's own scope is fully determined by the existing two-volume precedent and Unit 8's own configuration field").
- **Correction to my own prior work, applied before writing any Docker line**: an earlier, already-reverted draft (removed during Unit 8's own Defect Confirmation Review, since it was out of Unit 8's scope) had used the names `memory-core-storage` and `memory-core.log` — neither matches the Plan's own text. This Unit's own implementation was written fresh against the Plan's own literal wording (`memory-core-durability`, `durability.log`), not restored from that earlier draft, per this Unit's own governing task instruction to restore prior Docker work "if, and only if, [it] belong[s] to Unit 9" — the prior draft's naming did not, so nothing was restored verbatim.

---

## 3. Files Created

None.

## 4. Files Modified

- `docker-compose.yml` — added the `memory-core-durability` named volume (top-level `volumes:` block), its mount (`memory-core-durability:/data/memory-core`, alongside the existing two), and the `PARKER_MEMORY_CORE_DURABILITY_LOG_PATH: /data/memory-core/durability.log` environment entry on the `parker` service, in the same fixed-in-container-path style as the two existing Evidence Custodian entries immediately above it.
- `Dockerfile` — added `/data/memory-core` to the existing `mkdir -p` argument list in the single `RUN mkdir -p ... && chown -R parker:parker /opt/parker /data` line; no new `RUN` step, no other line changed.

No Kotlin file (production or test) was touched. No governance document was touched.

---

## 5. Behavioural Changes

None at the Memory Core level — no method signature, durability semantics, recovery logic, identifier restoration, or runtime composition code was modified. The only behavioural change is infrastructural: a `docker compose up` deployment now has a durable, named-volume-backed location for the Memory Core durability log, where previously (immediately after Unit 8 alone) `docker compose up` would have failed outright at configuration load with `MissingConfiguration`, since `PARKER_MEMORY_CORE_DURABILITY_LOG_PATH` had no value supplied anywhere in `docker-compose.yml`. That gap — explicitly disclosed as the intended Unit 8→Unit 9 handoff state in Unit 8's own Completion Review — is now closed.

---

## 6. Verification

### 6a. Static validation (Plan's own completion criteria)

`docker compose config` (with a placeholder `PARKER_OWNER_PRINCIPAL_ID`, since compose itself requires it) accepts the modified `docker-compose.yml` without error; the rendered config shows `PARKER_MEMORY_CORE_DURABILITY_LOG_PATH: /data/memory-core/durability.log` and a `memory-core-durability` volume mounted at `/data/memory-core`, structurally identical in shape to the two existing volumes.

### 6b. Image build and mount-point ownership (genuine, not assumed)

`docker build` completed successfully end-to-end (multi-stage: Gradle build, then JRE runtime image). Running the built image directly (`docker run --entrypoint sh`) and inspecting `/data` confirmed `/data/memory-core` exists, owned by `parker:parker` (uid/gid 999) — the single existing `chown -R parker:parker /opt/parker /data` step genuinely covers the new mount point; no second `chown` or `RUN` step was needed or added.

### 6c. Live container verification (genuine, not merely static)

`docker compose up -d --build` (real Ollama already reachable from the host via the existing `host.docker.internal` mapping): the `memory-core-durability` volume was created, the container started, and its logs showed `Runtime starting` → `Runtime started` — `ParkerRuntime.start()` reached `RUNNING` inside the real container, meaning `FileSystemMemoryCoreDurabilityLog` construction and `DurableMemoryCore.create` recovery (Unit 8's own code, unmodified) both succeeded against the new mounted path. `docker compose exec parker sh -c "ls -la /data/memory-core"` confirmed a real, empty `durability.log` file was created at the exact configured path. `docker compose restart parker` produced a clean `Shutdown signal received` → `Runtime shutting down` → `Runtime stopped` → `Runtime starting` → `Runtime started` sequence — graceful shutdown and restart through the real container lifecycle, not merely `ParkerRuntime.shutdown()` called directly in a test. `docker volume ls` confirmed the named volume survived the container restart, as Docker's own volume model guarantees. All test containers, volumes, and images were removed afterward (`docker compose down -v`, `docker rmi`) — no residue left behind.

This exceeds Unit 9's own stated completion criteria (which name only `docker compose config` and structural inspection) but stops short of Unit 10's own full "Docker-volume-backed restart test," which additionally proves a real Memory Core record survives the restart — that remains Unit 10's own explicitly separate responsibility, not duplicated here.

---

## 7. Targeted Test Results

Unit 9 has no Kotlin test surface (the Plan's own explicit statement, confirmed correct by inspection — Docker/compose configuration has no corresponding test file in this repository's own test source sets). The verification performed instead is Sections 6a–6c above.

## 8. Full Repository Verification

`./gradlew clean test`: **BUILD SUCCESSFUL**, 1905 tests, 0 skipped beyond the pre-existing 5, **0 failures, 0 errors** — identical count to Unit 8's own final run, confirming zero Kotlin-visible change from this Unit.

---

## 9. Explicit Non-Responsibilities Honoured

No backup or replication policy was selected or configured for the new volume. No multi-host or multi-container deployment was addressed. No Memory Core behaviour, durability semantics, recovery logic, or identifier restoration was modified. No runtime composition file (`ParkerRuntime.kt`, `ParkerRuntimeConfig.kt`) was touched. No governance document was modified. Unit 10 was not begun.

---

## Recommended Next Step

Proceed to an Independent Constitutional Review, performed fresh against the current repository state.
