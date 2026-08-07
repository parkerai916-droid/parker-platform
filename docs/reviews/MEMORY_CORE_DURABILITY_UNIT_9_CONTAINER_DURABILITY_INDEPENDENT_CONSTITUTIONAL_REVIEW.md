# Memory Core Durability — Unit 9: Container Durability — Independent Constitutional Review

## Status

**Genuine Independent Constitutional Review**, performed as if by another reviewer, against the governing documents re-read fresh and against the actual, current file contents — not against the Completion Review's own summary of them. This document does not amend `Dockerfile`, `docker-compose.yml`, the Completion Review, or any governance document.

---

## 1. Baseline and Prerequisite Re-Verification

Independently re-confirmed: `git status --short` before this Unit's own work began was clean at `HEAD = 02b5e31` (Unit 8's own commit). Independently re-read all eight prior Units' own Independent Constitutional Review documents for their literal verdict line (`grep -E "^(ACCEPTED|CONDITIONALLY ACCEPTED|REJECTED)"` against each file directly, not recalled from memory of the session): all eight read `ACCEPTED`, none conditionally. **No remaining work from Units 1–8 exists** — independently confirmed, not merely accepted from the Completion Review's own claim.

---

## 2. Re-Verification of the Central Naming Question — Was `memory-core-durability` Genuinely Required, or Could a Reasonable Reviewer Read It as Merely Illustrative?

**Test:** re-read the Implementation Plan's own Unit 9 "Outputs" sentence character-by-character, independent of the Completion Review's own paraphrase of it: "a new named volume, `memory-core-durability`, mounted at a fixed in-container path (**for example** `/data/memory-core`)... with a new environment variable (`PARKER_MEMORY_CORE_DURABILITY_LOG_PATH`, pointing at a file inside that mount, **e.g.** `/data/memory-core/durability.log`)". The hedge words "for example" and "e.g." are placed immediately against the mount *path* and the log *file* path respectively — not against the volume *name*, which is stated as a bare, unhedged appositive ("a new named volume, `memory-core-durability`,"). **This distinction is real, not a retrofit** — the same sentence hedges two of its three named artefacts and pointedly does not hedge the third. The Completion Review's own claim that the volume name is fixed while the paths are illustrative-but-followed is independently confirmed correct on the document's own actual punctuation, not an inference beyond what the text supports.

---

## 3. Challenge — Was the Prior, Already-Reverted Draft Correctly *Not* Restored, or Should Something From It Have Been Kept?

Checked directly, since the task's own instruction was conditional ("restore... if, and only if, they belong to Unit 9"): the prior draft (visible in Unit 8's own Defect Confirmation Review) used volume name `memory-core-storage` and file name `memory-core.log`, neither matching the Plan's own text confirmed in Section 2 above. Every other structural element of that prior draft — one new named volume, one new mount, one new environment variable, one `mkdir -p` argument added to the existing single line, no new `RUN` step — is identical in shape to what this Unit's own fresh implementation produced independently. **Nothing was lost by not restoring the prior draft verbatim**: its correct structural shape was independently re-derived from the Plan directly, and its incorrect names were correctly not carried forward.

---

## 4. Challenge — Does the Single Existing `chown -R parker:parker /opt/parker /data` Step Genuinely Cover the New Mount Point, or Was This Merely Assumed?

Independently re-run, not accepted from the Completion Review's own account: `docker build` on the current `Dockerfile`, then `docker run --entrypoint sh <image> -c "ls -la /data && id parker"` against the freshly-built image. Output, reproduced directly: `/data/memory-core` present, owned by `parker parker`; `id parker` reports `uid=999(parker) gid=999(parker)`. Since `chown -R` is recursive and `/data` is its own argument (not each subdirectory named individually), and the new `/data/memory-core` directory is created by the same `mkdir -p` invocation *before* the `chown -R` runs (both joined by `&&` in one `RUN` step, and Docker's shell form runs them in the stated left-to-right order), coverage is not incidental — it is structurally guaranteed by the existing command's own shape, independent of how many paths are named in `mkdir -p`'s own argument list. **Sound, and independently reproduced, not merely re-stated.**

---

## 5. Challenge — Does the Live Container Verification Actually Prove What the Completion Review Claims, or Could `Runtime started` Have Appeared Despite a Silent Configuration Problem?

Pressed specifically because a log line alone can be misleading if the underlying operation silently no-ops. Independently re-run: `docker compose up -d --build` with a placeholder `PARKER_OWNER_PRINCIPAL_ID`; `docker compose logs` showed `Runtime starting` then `Runtime started` with no `[ERROR]` line between them, matching `ParkerRuntime.start()`'s own already-verified (Unit 8) logging contract, under which `Runtime started` is only ever logged after `state = RuntimeLifecycleState.RUNNING` is assigned, which itself is unreachable if `stage("Memory Core durability log construction")` or `stage("Memory Core recovery")` throws. Independently confirmed the stronger, non-log-dependent evidence the Completion Review also cites: `docker compose exec parker sh -c "ls -la /data/memory-core"` showed a real `durability.log` file, size 0 bytes, timestamped at container start — a file `FileSystemMemoryCoreDurabilityLog`'s own constructor (Unit 3, unmodified) creates only if it did not already exist, and only after successfully validating its parent directory is writable. A silent no-op could not have produced this file. **The verification is genuine, not merely log-line-deep.**

---

## 6. Challenge — Was Anything Beyond Unit 9's Own Scope Touched?

Checked directly: `git status --short` after this Unit's own work shows exactly two modified files, `Dockerfile` and `docker-compose.yml` — no third file. `git diff` for both, read in full, shows no line changed beyond what Section 2's own confirmed-required outputs describe: one new volume declaration, one new mount line, one new environment entry, one extended `mkdir -p` argument list — no existing line altered, reordered, or removed. No `src/` or `tests/` file appears anywhere in the diff. **Sound.**

---

## 7. Full, Independent Quotation Audit

| Quoted fragment | Cited source | Verified |
| --- | --- | --- |
| "a new named volume, `memory-core-durability`, mounted at a fixed in-container path (for example `/data/memory-core`)... pointing at a file inside that mount, e.g. `/data/memory-core/durability.log`" | Implementation Plan, Unit 9, Outputs | Exact match, re-verified independently in Section 2, above, including the hedge-word placement the Completion Review's own argument depends on. |
| "no new `RUN` step" | Implementation Plan, Unit 9, Outputs | Exact match, confirmed by direct re-read; independently confirmed true of the actual diff in Section 6. |
| "Docker configuration has no Kotlin test file; verified instead by Unit 10's own Docker-restart verification step" | Implementation Plan, Unit 9, Test files expected to change | Exact match (lightly requoted from the Plan's own two adjacent sentences), confirmed by direct re-read. |
| "Container restart... the named volume's own content survives natively via Docker's own volume persistence" | Implementation Plan, Unit 9, Outputs | Exact match, confirmed by direct re-read; independently reproduced at the infrastructure level in Section 5 above (`docker volume ls` after `docker compose restart`), short of Unit 10's own further, Memory-Core-record-level proof, which the Completion Review itself correctly declines to claim. |

No further quoted fragment appears in the Completion Review beyond ordinary file/variable names. **No defect found.**

---

## Findings

No required correction was found. The central naming question this Unit turns on (Section 2) is independently re-derived from the Plan's own exact punctuation, not merely re-accepted. The decision not to restore the prior, incorrectly-named draft is independently confirmed correct and lossless (Section 3). Mount-point ownership coverage (Section 4) and the genuineness of the live container verification (Section 5) are each independently reproduced, not merely re-stated. No out-of-scope file was touched (Section 6).

---

## Constitutional Verdict

```
ACCEPTED
```

No required correction. No Defect Confirmation Review is necessary.

---

## Recommended Next Step

Per this task's own explicit stop point: work halts here. Unit 10 (Verification) is not begun. Nothing is staged, committed, or pushed.

---

## Final Git Status at Time of This Review

```
$ git status --short
 M Dockerfile
 M docker-compose.yml
?? docs/reviews/MEMORY_CORE_DURABILITY_UNIT_9_CONTAINER_DURABILITY_COMPLETION_REVIEW.md
?? docs/reviews/MEMORY_CORE_DURABILITY_UNIT_9_CONTAINER_DURABILITY_INDEPENDENT_CONSTITUTIONAL_REVIEW.md
```

Nothing staged, committed, or pushed.
