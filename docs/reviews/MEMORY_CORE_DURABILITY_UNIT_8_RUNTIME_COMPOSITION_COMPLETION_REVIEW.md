# Memory Core Durability — Unit 8: Runtime Composition — Completion Review

## Status

Completion Review for Memory Core Durability Implementation Unit 8 ("Runtime Composition"). Composes the already-implemented, already-independently-verified durability stack (Units 1–7) into the live Parker runtime, replacing the single, raw `InMemoryMemoryCore` construction site in `ParkerRuntime.start()` with `FileSystemMemoryCoreDurabilityLog` → `DurableMemoryCore.create` (recovery), and re-pointing every existing `MemoryCore`/`MemoryRetrieval` consumer at the durable instance. No governance document was modified. Unit 9 was not begun.

---

## 1. Baseline

Branch `main`, `HEAD` clean at task start, matching the task's own claimed `3215473 feat: add durable Memory Core decorator and write ordering` exactly (`git rev-parse HEAD` = `3215473f9cf274d34eca9ef9bd16306ba8f2a3db`). Units 1–7 confirmed complete, reviewed, committed. This baseline claim was accurate, unlike a prior Unit's inaccurate claim earlier in this session.

---

## 2. Planning Review Findings

Performed before any Kotlin was written, against the current repository state, not against memory of it:

- **Every `InMemoryMemoryCore` construction site**: exactly one, `ParkerRuntime.kt:634` (`val inMemoryMemoryCore = InMemoryMemoryCore()`), now replaced.
- **Every `MemoryCore` injection site**: exactly two raw consumers — `EvidenceRegistrationCoordinator` (`ParkerRuntime.kt:638`, originally) and `EvidenceIntelligenceAcceptanceCoordinator` (originally `ParkerRuntime.kt:803`) — plus one `MemoryRetrieval` consumer, the single shared `PermissionFilteredMemoryRetrieval` (originally `ParkerRuntime.kt:765`).
- **No runtime path bypasses the decorator**: confirmed by re-reading every reference to `memoryCore`/`inMemoryMemoryCore` in the file; all three consumer sites are accounted for above.
- **No runtime path needs direct access to the recovered in-memory delegate**: `DurableMemoryCore`'s own recovered `InMemoryMemoryCore` is a private field, unreachable from composition — by design, and by the task's own instruction ("No runtime component shall write directly to the recovered delegate except the decorator itself").
- **Startup ordering**: `permissionEngine` is constructed at line 598, strictly before Memory Core's own construction (originally line 634) — Permission Engine before Memory Core, exactly as the task's own diagram requires.
- **Shutdown**: `ParkerRuntime.shutdown()` (lines 1265–1288) references only `runtimeEventLogger`; no Memory Core reference exists there today, and none is needed — every durable write already commits synchronously inside `DurableMemoryCore`'s own write methods (append-then-restore), so there is no buffered state to flush at shutdown. **No shutdown change was required or made.**
- **Runtime composition was confirmed the lawful next step**: Units 1–7 are complete, reviewed, and committed; the Implementation Plan's own Dependency Graph names Unit 8 next.

### 2a. Governance/sequencing finding — reported per the task's own "STOP. Report it." instruction

The task's own required architecture diagram routes `DurableMemoryCore → PermissionGatedMemoryCore → existing runtime consumers`. Direct inspection of the current, actual `ParkerRuntime.kt` (its own pre-existing inline comments, unchanged by this Unit until reviewed) showed both real `MemoryCore` consumers already self-gate their own writes internally — `EvidenceRegistrationCoordinator` and `EvidenceIntelligenceAcceptanceCoordinator` each hold their own `PermissionEngine` reference and perform their own resource/action checks before writing. Wrapping either in `PermissionGatedMemoryCore` would double-gate an already-gated write. This is independently confirmed by three sources: the Implementation Plan's own Unit 8 text (explicit non-responsibility "does not add a `PermissionGatedMemoryCore` wrapper," and an explicit Stop Condition forbidding any change to either coordinator's own gating logic), the Scope Lock's own §18 binding `SHALL NOT` ("introduce double permission gating on any write path already gated internally by its own caller"), and the code's own pre-existing comments. A repo-wide grep additionally confirmed `PermissionGatedMemoryCore` has zero construction sites anywhere in `src/` — it is fully implemented and independently verified but genuinely unwired.

This finding was presented to the user directly (not silently resolved) before any Kotlin was written. The user selected **"Preserve no-double-gating"**: no `PermissionGatedMemoryCore` construction; both existing consumers continue receiving a raw `MemoryCore` reference, now pointing at the durable instance rather than the volatile one, identical shape to today.

---

## 3. Files Created

- `tests/composition/ParkerRuntimeMemoryCoreDurabilityCompositionTest.kt` — new dedicated composition test suite for this Unit (6 tests): shared-instance identity across both self-gating coordinators; no double gating; the shared `PermissionFilteredMemoryRetrieval` wraps the same `DurableMemoryCore` instance, never a second reference to the private recovered delegate; a genuine cross-instance durability proof (an Entity created through one `ParkerRuntime` is recovered by a second, independent `ParkerRuntime` pointed at the same durability log file); and two runtime-failure-verification tests (malformed durability log fixture aborts `start()` to `FAILED`, naming `"Memory Core recovery"`, never reaching `RUNNING`; a missing parent directory aborts naming `"Memory Core durability log construction"`).

## 4. Files Modified

- `src/composition/ParkerRuntime.kt` — replaced the raw `InMemoryMemoryCore()` construction with `stage()`-wrapped `FileSystemMemoryCoreDurabilityLog` construction followed by `stage()`-wrapped `DurableMemoryCore.create(...)` recovery; re-pointed the shared `memoryCore` reference and `permissionFilteredMemoryRetrieval` at the durable instance; updated inline comments to reflect the new composition and to record this Unit's own reaffirmation of the no-double-gating finding; removed the now-unused `InMemoryMemoryCore` import.
- `src/composition/ParkerRuntimeConfig.kt` — added the required `memoryCoreDurabilityLogPath: String` field and `KEY_MEMORY_CORE_DURABILITY_LOG_PATH` constant, mirroring `evidenceDeletionAuditLogPath`'s own exact shape (a single required file path, `requireKey`-sourced, no invented default).
- Seven existing composition test files (`ParkerRuntimeKnowledgeRetrievalCompositionTest.kt`, `ParkerRuntimeEvidenceCustodianIntegrationTest.kt`, `ParkerRuntimeStartupAndShutdownTest.kt`, `ParkerRuntimeConversationPipelineTest.kt`, `ParkerRuntimeFailureHandlingTest.kt`, `ParkerRuntimeEvidenceIntelligenceCompositionTest.kt`, `ParkerRuntimeReasoningContextIntegrationTest.kt`) — each supplies `memoryCoreDurabilityLogPath` in its own `config()` helper (a new required field, additive).
- `tests/composition/ParkerRuntimeConfigLoaderTest.kt` — extended `fullEnvironment()` with the new key; added one "every key present" assertion and one "missing PARKER_MEMORY_CORE_DURABILITY_LOG_PATH throws MissingConfiguration" test, mirroring the existing evidence-audit-path tests exactly.
- `tests/composition/ParkerRuntimeEvidenceIntelligenceCompositionTest.kt` — updated the one test asserting the raw `memoryCore`'s concrete type: it was `InMemoryMemoryCore` before this Unit and is now, correctly, `DurableMemoryCore` — an expected, intended consequence of this Unit's own objective, not a defect being patched over. Updated the corresponding fixture-setup reflection type and one KDoc reference identically.

No governance document was touched. No file outside this Unit's own scope was modified.

---

## 5. Runtime Composition Summary

```
FileSystemMemoryCoreDurabilityLog(Path.of(config.memoryCoreDurabilityLogPath))
        │  stage("Memory Core durability log construction")
        ▼
DurableMemoryCore.create(memoryCoreDurabilityLog)
        │  stage("Memory Core recovery") -- recovers InMemoryMemoryCore's
        │  complete starting state exactly once (MemoryCoreRecovery.recover,
        │  Units 4-5); identifier counters derived as part of that same pass
        ▼
val memoryCore: MemoryCore = durableMemoryCore
        │
        ├──> EvidenceRegistrationCoordinator(defaultEvidenceCustodian, memoryCore, permissionEngine)
        │       (raw reference -- already self-gates internally; no PermissionGatedMemoryCore)
        │
        └──> EvidenceIntelligenceAcceptanceCoordinator(defaultEvidenceCustodian, memoryCore, knowledgeSubmission, permissionEngine)
                (raw reference -- already self-gates internally; no PermissionGatedMemoryCore)

durableMemoryCore (as MemoryRetrieval)
        ▼
PermissionFilteredMemoryRetrieval(durableMemoryCore, permissionEngine)
        │  the one shared retrieval decorator -- unchanged consumers:
        ├──> EvidenceIntelligenceInputResolver
        └──> DefaultKnowledgeCandidateEvaluator
```

Both `stage()` calls follow the existing `DependencyConstructionFailed`-wrapping discipline this composition root already uses everywhere else; a thrown `MemoryCoreRecoveryException` (not a `ParkerRuntimeException`) is therefore automatically wrapped and surfaces as `state = FAILED`, never `RUNNING` — no separate fallback code path exists that could construct a fresh, empty `InMemoryMemoryCore` instead. No `PermissionGatedMemoryCore` is constructed anywhere in this graph, per the disclosed, user-confirmed resolution in Section 2a above.

---

## 6. Behavioural Verification Results

### 6a. Repository-level durability proof (genuine, not mocked)

`ParkerRuntimeMemoryCoreDurabilityCompositionTest`'s own `an Entity created through the running graph is recovered by a second, independent ParkerRuntime pointed at the same durability log` test: a real `ParkerRuntime` is started, a real `Entity` is durably created through the real, composed `DurableMemoryCore`, that runtime is fully shut down, and a **second, independently constructed** `ParkerRuntime` — sharing nothing but the durability log's file path — is started and recovers the Entity, observable through its own, independently-recovered `DurableMemoryCore`. This is genuine file-backed persistence across two independently-constructed runtime graphs, not two references to shared in-memory state (`DurableMemoryCore.create` is a pure factory; no singleton or static state exists anywhere in this design).

### 6b. Live interactive verification — a significant, pre-existing gap discovered, reported, and resolved with the user before proceeding

Running Parker interactively (`./gradlew installDist`, then the produced launcher, with a live local Ollama model) and teaching it "Remember that my favourite coffee mug is black" produced the reply *"I've noted that your favourite coffee mug is black..."* — but direct inspection of the durability log file showed **zero bytes written**. Tracing this confirmed that **no path from conversation to a `MemoryCore` write exists anywhere in this runtime**: `ConversationTurnReasoningCoordinator`, `CommunicationConversationCoordinator`, `ConversationReplyCoordinator`, and `ResponseComposer` hold no `MemoryCore` reference, and no `Tool` implementation in the codebase calls `MemoryCore`. The model's reply was an unbacked, hallucinated acknowledgment. Confirming this further: a completely fresh process, asked the same question with no conversation history, correctly replied *"I don't have enough information..."* — proving the first session's apparent "recall" came from the model's own in-context conversation window, not from Memory Core.

This is not a defect in this Unit's own composition (which is independently proven correct in 6a) — it is a pre-existing gap: the capability to turn a natural-language "remember" instruction into an actual Memory Core write was never built in any prior unit, and building it is well outside "compose the existing durability stack into the runtime." This was reported to the user directly, mid-verification, exactly as the task's own "If a governance or sequencing issue exists: STOP. Report it" instruction requires in spirit, even though the issue surfaced during verification rather than planning. The user selected: verify durability through the write paths that genuinely exist today (Evidence Custodian registration, Evidence Intelligence acceptance — exercised by 6a above), and document the conversational gap plainly rather than attempt to close it inside this Unit.

### 6c. Runtime failure verification (isolated test fixture only)

`ParkerRuntimeMemoryCoreDurabilityCompositionTest`'s own `start() aborts to FAILED, naming Memory Core recovery, when the durability log fixture is intentionally malformed` test: one line of deliberately invalid data (never produced by any real `append()` call) is written directly to a fresh log file before `ParkerRuntime` is even constructed. `start()` throws `ParkerRuntimeException.DependencyConstructionFailed` naming `"Memory Core recovery"`, leaves `state == FAILED`, and — confirmed directly — no empty, writable `InMemoryMemoryCore` is ever produced or reachable (there is no code path that could construct one; recovery failure propagates as a thrown exception with no `catch`-and-fallback anywhere in this Unit's own code).

---

## 7. Targeted Test Results

`ParkerRuntimeMemoryCoreDurabilityCompositionTest`: 6/6 passed. `ParkerRuntimeConfigLoaderTest`: 17/17 passed. `ParkerRuntimeEvidenceIntelligenceCompositionTest`: 13/13 passed. `ParkerRuntimeStartupAndShutdownTest`, `ParkerRuntimeEvidenceCustodianIntegrationTest`, `ParkerRuntimeKnowledgeRetrievalCompositionTest`, `ParkerRuntimeConversationPipelineTest`, `ParkerRuntimeFailureHandlingTest`, `ParkerRuntimeReasoningContextIntegrationTest`: all passed, zero failures.

## 8. Full Repository Verification

`./gradlew clean test`: **BUILD SUCCESSFUL**, 1905 tests, 0 skipped beyond the pre-existing 5, **0 failures, 0 errors**.

---

## 9. Explicit Non-Responsibilities Honoured

No backup, replication, log compaction, pruning, migration, SQLite, multiple stores, Knowledge Memory persistence, Evidence persistence (already independently complete), or conversation persistence was implemented. `Dockerfile` and `docker-compose.yml` were not touched — the Implementation Plan's own Unit 8 section states this explicitly as its own Explicit non-responsibility ("Does not touch Docker (Unit 9)"), and Docker volume mapping is Unit 9's own, separately-governed scope. An earlier draft of this Unit's own work did add a Docker volume, incorrectly reasoning from a different section of the Implementation Plan's own summary tables; this was caught and reverted before this review was finalised — see the accompanying Defect Confirmation Review for the full account. No governance document was modified. Unit 9 was not begun. A direct, practical consequence of this correction, disclosed here rather than left implicit: `docker compose up` will not start correctly against this Unit's own change alone, since `PARKER_MEMORY_CORE_DURABILITY_LOG_PATH` is now a required key with no value supplied anywhere in `docker-compose.yml` — this is the expected, intended handoff to Unit 9, not a regression this Unit is responsible for closing.

---

## Recommended Next Step

Proceed to an Independent Constitutional Review, performed fresh against the current repository state.
