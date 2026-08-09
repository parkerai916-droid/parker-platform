**Status:** Unit 2 Implementation/Execution Plan — **ACCEPTED**, subject to its accepted Independent Constitutional Review. Governance only; implementation and live calls require a later PASS Boundary Review and explicit approval.

# Reasoning Protocol Live-Model Conformance Unit 2 — Baseline Characterisation Implementation/Execution Plan

## 1. Objective and exact surface

Implement one test-only campaign driver and one detached explicit task that execute the Scope Lock's immutable schedule through committed Unit 1 APIs.

Future implementation surface is exactly:

1. `build.gradle.kts` — add `reasoningProtocolBaselineCharacterisation`, using existing `liveModelEvaluation` output/runtime classpath, JUnit Platform filtering to `parker.integration.ReasoningProtocolBaselineCharacterisationTest`, and JVM property `parker.reasoning.baseline.enabled=true`. It must not attach to any lifecycle task.
2. `tests/integration/ReasoningProtocolBaselineCharacterisationTest.kt` — all Unit 2 fixture/schedule definitions, driver, durable ledger/manifests, summary generator, offline tests, and live entry.

No new dependency, plugin, source set, production file, Unit 1 file, existing test, or third helper file is planned. Any such need stops for Boundary Review amendment.

## 2. Explicit activation and task isolation

The live campaign test requires both:

- JVM property `parker.reasoning.baseline.enabled=true`, supplied only by the dedicated task; and
- the complete `PARKER_REASONING_EVAL_*` configuration accepted in Unit 1 plus Unit 2 campaign ID, artifact root, Ubuntu/runtime identity, and immutable model identity inputs frozen by the later Boundary Review.

Absent campaign property means skip before driver/client construction. Partial configuration fails redacted. The existing `reasoningProtocolLiveModelEvaluation` task does not set the property and therefore cannot accidentally launch Unit 2. The Unit 2 task filters out the Unit 1 smoke class. `test`, `check`, `build`, and `assemble` remain detached and offline.

## 3. Immutable definitions and hashes

Encode the 23 Scope Lock fixtures and twelve sentinels as immutable Kotlin constants using Unit 1 `ConformanceFixture`. Reuse Unit 1 `ContextProfileId` and `SyntheticContextProfiles`; do not copy production prompt content.

At driver startup, serialize a canonical campaign definition containing fixture fields/rubrics, profile IDs/order, sentinel IDs, stages, attempts, timeout, and request-format version. SHA-256 becomes `campaignDefinitionHash`. The driver fails if its computed hash does not match the frozen expected hash established by deterministic tests/accepted implementation review.

Generate the complete ordered schedule before endpoint construction. Assert exactly 11 unscored calls, 1,380 Stage 1 calls, 2,520 Stage 2 calls, 3,900 unique scored IDs, 3,911 unique total IDs, 30 IDs per scored cell, and no fixture/profile outside authority.

## 4. Pre-campaign operator commands/checks

The execution runbook must require read-only checks equivalent to:

```text
git branch --show-current
git rev-parse HEAD
git status --short
java -version
./gradlew --version
ollama list
ollama show qwen2.5-coder:7b
docker inspect <authorised Parker/Ollama container names, where used>
df -h <artifact filesystem>
```

The operator records outputs or cryptographic hashes without recording secrets. Confirm Ubuntu, authorised commit/working snapshot, clean or explicitly authorised governance/driver state, model digest/manifest, endpoint reachability, runtime/container IDs, writable access-controlled artifact root, sufficient free space, 90-second timeout, and no competing model workload where practical.

If any identity cannot be pinned, stop before warm-up. The runbook never changes model, prompt, sampling, production timeout, or server configuration to make preflight pass.

## 5. Stage 0 execution

Create a dedicated `preflight/` artifact set. Execute `W01`–`W03` serially, then `PF01`–`PF08` in frozen order. None is passed to scored aggregation.

For every call, prove a unique ID, production-generated prompt and hash, raw envelope, extracted response, parser evidence, latency/metadata, append/flush success, and configuration fingerprint. After PF08 verify exactly eleven records, seal hashes/manifest, and re-check the harness has no downstream dependency. Failure blocks scored execution.

## 6. Batch plan and per-trial transaction

Materialize fourteen scored batches at cell boundaries, maximum 300 calls:

- Stage 1: `S1-B01`–`S1-B05`, containing 10/10/10/10/6 cells;
- Stage 2: `S2-B01`–`S2-B09`, containing 10/10/10/10/10/10/10/10/4 cells.

Within a batch, before each endpoint call:

1. verify configuration fingerprint and prior manifest chain;
2. compute the next scheduled deterministic ID;
3. scan the verified raw ledger and require the ID absent; and
4. present operator-visible batch/cell/trial progress.

After the one Unit 1 `execute` call:

1. serialize the complete `TrialObservation` through Unit 1 `EvaluationJsonLines`;
2. append one UTF-8 line;
3. flush and force the file channel to durable storage where supported;
4. update an append-only completion ledger and atomic checkpoint manifest; and
5. evaluate only the false-positive pause predicate before selecting another ID.

No endpoint call is automatically repeated. Operator interruption is honored after the current record is durable.

## 7. Resume algorithm

On resume, load the immutable campaign definition and configuration fingerprint, verify the complete prior-manifest hash chain, verify every artifact's SHA-256/size/line count, parse every raw record's trial identity, and reject unknown or duplicate IDs. Reconstruct completed IDs from valid raw records, cross-check the checkpoint ledger, and select the first missing scheduled ID.

A raw record without a later checkpoint entry is authoritative after it validates; the checkpoint is repaired without another model call. A checkpoint ID without a raw record is artifact corruption and blocks resume. Any commit, model/digest, endpoint, timeout, request shape, runtime/container, corpus/profile, campaign hash, or manifest mismatch blocks continuation.

## 8. Consequential pause implementation

After durable append, compare expected and actual typed actions. A non-Remember parsed as Remember or non-Goal parsed as Goal writes a consequential-event record, seals the batch as `PAUSED_CONSEQUENTIAL_FALSE_POSITIVE`, and returns before the next call.

Independent verification must record whether the event is measurement defect or genuine model behavior. A harness defect returns to Unit 1 governance. A genuine event may resume only with explicit approval and remains in all summaries. The driver cannot modify configuration, delete evidence, reset schedule, or rerun the event.

## 9. Deterministic analysis

The same file implements pure aggregation over recorded observations:

- counts/proportions and 95% Wilson intervals;
- four expected-action confusion matrices with the ten frozen actual columns;
- per-action/profile/fixture metrics;
- latency quantiles and available token/timing distributions;
- H/I operational report;
- separate Remember/Goal false-positive report;
- action/representation/byte/fidelity stability;
- context category calculations using the Planning Review's descriptive thresholds; and
- a content-fidelity worksheet keyed by opaque output ID.

Use stable row/key order and deterministic UTF-8 JSON/CSV/Markdown encoding implemented with JDK/Kotlin standard library. Manual reviewer decisions are entered into a separate append-only worksheet with reviewer IDs, independent decisions, resolution, and timestamps; summary regeneration consumes the preserved worksheet. Arithmetic is not manually transcribed.

## 10. Artifact tree

Under an explicitly configured durable root outside the worktree:

```text
<campaign-id>/
  campaign-definition.json
  preflight/raw.jsonl
  preflight/manifest.json
  stage-1/<batch-id>/raw.jsonl
  stage-1/<batch-id>/checkpoint.json
  stage-1/<batch-id>/manifest.json
  stage-2/<batch-id>/raw.jsonl
  stage-2/<batch-id>/checkpoint.json
  stage-2/<batch-id>/manifest.json
  reviews/content-fidelity.csv
  reports/summary.json
  reports/summary.md
  reports/confusion-matrices.csv
  reports/repeatability.csv
  reports/context-drift.csv
  reports/operational.csv
  reports/consequential-events.jsonl
  artifact-manifest.json
```

Every file has SHA-256, size, line count where applicable, producer version, and campaign/config identity. No ordinary Parker log receives trial evidence.

## 11. Offline verification before live execution

The new test class must prove without Ollama:

1. exact fixture texts/actions/rubrics/counts and twelve sentinel IDs;
2. exact profile construction/order;
3. exact 3,911-call schedule and 30 unique attempts per scored cell;
4. batch sizes/order and unique deterministic IDs;
5. baseline property/config absence skips before client construction;
6. partial/identity-mismatched configuration fails redacted;
7. Stage 0 failure prevents Stage 1;
8. append/flush/checkpoint/manifest ordering;
9. crash-after-raw-before-checkpoint resumes without duplicate call;
10. duplicate/unknown/corrupt record blocks resume;
11. every configuration/identity mismatch blocks resume;
12. H/I/wrong/malformed completed trials are not retried;
13. false-positive event pauses before the next stub request and survives resume;
14. summary matrices, Wilson intervals, drift and repeatability fixtures are deterministic;
15. JSON/CSV/Markdown and hash manifests are byte-stable;
16. content worksheet preserves independent reviewer outcomes;
17. Unit 1 files remain byte-identical to `b34f8d0`;
18. dedicated task selects only Unit 2 class; and
19. normal lifecycle graphs remain detached/offline.

Use pure data and a JDK loopback stub only. No live call occurs during verification.

## 12. Live execution sequence

After targeted tests, ordinary full-suite verification, accepted Completion readiness checkpoint, and explicit execution approval:

1. run operator checks and capture identity manifest;
2. run the dedicated task for Stage 0 only;
3. independently verify and seal Stage 0;
4. explicitly authorize scored execution;
5. execute batches serially with visible progress and operator stop control;
6. handle any constitutional pause before resuming;
7. verify all 3,900 unique scored IDs and artifact chain;
8. complete blinded fidelity review;
9. regenerate deterministic reports from immutable artifacts;
10. perform Completion Review and Independent Constitutional Review.

No background autonomous retry or unsupervised remedy exists. Batch continuation can be operator-driven; the driver never changes experiment configuration.

## 13. Stop conditions

Stop Unit 2 for real/private data, model/config identity drift, artifact corruption/loss, schedule duplication/omission, inability to enforce resume, downstream reachability, or any Unit 1 traffic/parser/evidence defect. Do not correct Unit 1 within Unit 2.

Bad model output is evidence, not an implementation defect and not authority to tune. Timeout, malformed output, wrong action, and transport failure remain completed observations.

## 14. Reviews and completion

Implementation requires a PASS Boundary Review and explicit approval. After implementation but before scored execution, targeted and ordinary verification plus a readiness review are required. After campaign completion: hash verification, deterministic report regeneration, content review, Completion Review, Independent Constitutional Review, and Defect Confirmation only for a genuine corrected defect.

Unit 3 receives evidence after Unit 2 acceptance. No Unit 3 remedy recommendation appears in the driver or reports beyond neutral failure-class descriptions.

## 15. Disposition

```text
ACCEPTED
```

Corrective action: **NONE**. No implementation or endpoint call begins through this plan alone.
