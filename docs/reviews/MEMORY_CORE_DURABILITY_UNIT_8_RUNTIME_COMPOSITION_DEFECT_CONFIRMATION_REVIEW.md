# Memory Core Durability — Unit 8: Runtime Composition — Defect Confirmation Review

## Status

One genuine defect was found while gathering evidence for the Independent Constitutional Review, corrected, and re-verified. This document records the defect, the correction, and the re-run verification, per this Unit's own governing task instruction ("If genuine defects are found: correct only those defects, produce a Defect Confirmation Review, rerun targeted tests, rerun full verification").

---

## The Defect

While cross-checking the Completion Review's own citation of the Implementation Plan against the Plan's actual current text (in preparation for the Independent Constitutional Review), a direct quotation search surfaced the Plan's own Unit 8 section, verbatim:

> **Explicit non-responsibilities.** Does not add a `PermissionGatedMemoryCore` wrapper. Does not change any method signature on `MemoryCore`/`MemoryRetrieval`. **Does not touch Docker (Unit 9).**

This directly contradicts a change already made: `docker-compose.yml` had been given a new `memory-core-storage` named volume, a mount, and a `PARKER_MEMORY_CORE_DURABILITY_LOG_PATH` environment variable; `Dockerfile` had been given a matching new mount-point directory in its existing `mkdir`/`chown` step. The Plan's own separate Unit 9 section ("Container Durability") shows this is deliberately not this Unit's own work — Unit 9 owns exactly this Docker volume mapping, with its own governing authority, its own inputs (naming `ParkerRuntimeConfig.memoryCoreDurabilityLogPath`, this Unit's own output, as an *input* to Unit 9), and its own completion criteria. The task's own current instruction is explicit: "Do not begin Unit 9."

**Root cause of the error:** a different passage in the same Implementation Plan document — a document-wide "Repository Impact" summary table, not the Unit 8 section itself — lists `Dockerfile, docker-compose.yml (one new volume/mount/env-var, following the existing two-volume pattern exactly)` under a general "Modified files" heading. This summary line was read and relied upon without first confirming which Unit's own section it was summarising; it in fact summarises Unit 9's own output, not Unit 8's. Citing a document's own summary table without tracing it back to the specific section it summarises was the proximate cause.

This was a genuine scope violation, not a stylistic issue: it began work explicitly reserved for a unit the task instructs must not be begun.

---

## The Correction

1. `git checkout -- Dockerfile docker-compose.yml` — both files restored to their exact pre-Unit-8 content. Confirmed via `git status --short`: neither file appears as modified afterward.
2. `.env.example` was also reverted (`git checkout -- .env.example`). Its own edit had documented the (now-reverted) `docker-compose.yml` default, and was not itself named in Unit 8's own "Production files expected to change" (`src/composition/ParkerRuntimeConfig.kt`; `src/composition/ParkerRuntime.kt` only) — once its stated rationale (documenting a Docker default) no longer held, no independent justification remained for touching it inside this Unit.
3. The already-drafted Completion Review was corrected: the "Files Modified" list no longer names `docker-compose.yml`, `Dockerfile`, or `.env.example`; "Section 9 (Explicit Non-Responsibilities Honoured)" now states plainly that Docker was not touched, cites the Plan's own "Does not touch Docker (Unit 9)" text directly, and discloses the practical consequence (a container start today would fail on this key alone, which is the correct, expected Unit 8→Unit 9 handoff state, not a regression).

No other file was touched by this correction. `src/composition/ParkerRuntime.kt`, `src/composition/ParkerRuntimeConfig.kt`, the new and modified test files, and the required config field's own required-with-no-default shape are all unaffected — the defect was confined entirely to the two Docker files and the one documentation file.

---

## Re-Run Verification

- **Targeted tests:** re-run after the revert (`ParkerRuntimeMemoryCoreDurabilityCompositionTest`, `ParkerRuntimeConfigLoaderTest`, and the seven other affected composition test files) — unaffected by a Docker/documentation-only revert, all still passing, as expected: the revert touched no Kotlin file.
- **Full repository verification:** `./gradlew clean test` re-run after the revert. **BUILD SUCCESSFUL**, 1905 tests, 0 skipped beyond the pre-existing 5, **0 failures, 0 errors** — identical count to the pre-correction run, confirming the revert changed nothing Kotlin-visible.
- **`git status --short` after correction:** confirmed clean of `Dockerfile`, `docker-compose.yml`, and `.env.example` — only the two Unit-8-authorised production files, the affected test files, and the two new review documents remain.

---

## Finding

One genuine defect (an unauthorised, out-of-scope Docker/compose change, reserved for Unit 9) found and corrected. No other defect was found in the same pass. The corrected state was carried forward into the Independent Constitutional Review.
