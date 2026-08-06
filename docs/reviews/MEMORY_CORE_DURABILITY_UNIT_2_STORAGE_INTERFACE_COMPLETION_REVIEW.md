# Memory Core Durability — Unit 2: Durable Storage Interface — Completion Review

## Status

**Implementation complete for Unit 2 only.** Unit 3 and every later durability unit are not begun. Nothing is staged, committed, or pushed.

---

## Repository Baseline

- **HEAD (before this Unit began):** `5862b4792cb10745edfa367b22d718f9f23e0d1e`
- **Branch:** `main`
- **Working tree (before this Unit began):** clean. Unit 1 (durable record format) confirmed committed and pushed at this HEAD.
- **Governing status confirmed at the start of this Unit:** Memory Core Durability Contract Design accepted; Memory Core Durability Scope Lock accepted; Memory Core Durability Implementation Plan committed; Unit 1 implemented, reviewed, committed, and pushed.

---

## Governing Documents Read Fresh for This Unit

Per this task's own instruction, not relied on from summary: `docs/architecture/MEMORY_CORE_DURABILITY_CONTRACT_DESIGN.md`; `docs/architecture/MEMORY_CORE_DURABILITY_SCOPE_LOCK.md`; `docs/implementation/MEMORY_CORE_DURABILITY_IMPLEMENTATION_PLAN.md` (Unit 2 section specifically, re-read fresh); `src/runtime/DurableMemoryCoreEntry.kt` (re-read in full, confirmed unchanged since Unit 1's own commit); `src/interfaces/EvidenceArtifactStorage.kt` (the interface/implementation split precedent, read fresh, not previously read in full this session); `tests/contracts/EvidenceArtifactStorageScopeTest.kt` (the `declaredFunctions`-based exact-operation-set test pattern this Unit's own test file directly mirrors); `tests/runtime/FakeEvidenceDeletionAudit.kt` (the lambda-configurable test-fake pattern this Unit's own fake directly mirrors).

---

## Objective

Introduce the internal persistence seam through which durable Memory Core records will later be appended and read — the storage contract only. No filesystem persistence, no serialization, no replay.

---

## A Disclosed, Deliberate Redirection From the Implementation Plan's Own Unit 2 Text — Not a Mere Narrowing

The Implementation Plan's own Unit 2 description (drafted before this implementation session) anticipated "a single concrete class, `FileSystemMemoryCoreDurabilityLog` — deliberately not split into an interface plus one implementation the way `EvidenceArtifactStorage`/`FileSystemEvidenceArtifactStorage` are." This governing task's own explicit instruction supersedes that specific reasoning for this session: it asks to "Create the internal durability storage interface required by the Implementation Plan," explicitly states "Do not create a production implementation yet," and asks for a test-only fake in its place.

This is a more significant redirection than Unit 1's own disclosed scope narrowing (which only moved *where* an already-planned capability would be implemented). Here, the task introduces exactly the interface-plus-implementation split the Implementation Plan's own text reasoned against. It is honoured as this session's own explicit, current, and more specific instruction. It does not conflict with either governing document's own fixed authority: the Contract Design's own Section 5 already establishes that it "fixes required properties; it does not select a mechanism," and an interface-first sequencing is one lawful way to build toward a mechanism without selecting one prematurely — a sequencing choice, not a new architectural decision requiring a Scope Lock revision. Disclosed here and in the production file's own KDoc, not silently reconciled. A future housekeeping pass to the Implementation Plan's own Unit 2 text (separately authorised, mirroring Unit 1's own analogous disclosed gap) would bring the Plan's own wording in line with what this session actually built.

---

## What Was Implemented

**New file:** `src/runtime/MemoryCoreDurabilityLog.kt` — `internal interface MemoryCoreDurabilityLog` with exactly two operations, the exact shape the Implementation Plan's own Unit 2 text already authorised:

```kotlin
suspend fun append(entry: DurableMemoryCoreEntry)
suspend fun readAll(): List<DurableMemoryCoreEntry>
```

**Design decisions, and the reasoning behind each:**

- **`append` returns `Unit`, never a `Boolean` or nullable success indicator.** A failure is a thrown exception, never a returned value a caller could mistake for success.
- **`readAll` returns a non-nullable `List<DurableMemoryCoreEntry>`, never a sealed-result wrapper or nullable type.** An empty list means, and only ever means, "nothing has been durably appended yet"; an implementation that cannot determine this must throw, never return an empty list in its place — the interface's own KDoc states this explicitly as a binding contract on any future implementation.
- **No dedicated exception type is defined for this interface.** No real implementation exists yet to have a real failure mode to name; inventing one in advance risked fixing the wrong shape before Unit 3's own genuine failure semantics are known. An implementation remains free to let an underlying exception propagate or to wrap it in a purpose-named one — either satisfies this interface's own contract.
- **`internal`, not public** — mirroring `DurableMemoryCoreEntry`'s and `MemoryCoreLifecycleTransitions`'s own identical precedent.
- **No import beyond `DurableMemoryCoreEntry`** (same package, no import statement needed) and standard Kotlin collection types — confirmed by the file's own empty import list.

**New file:** `tests/runtime/FakeMemoryCoreDurabilityLog.kt` — a test-only fake, mirroring `FakeEvidenceDeletionAudit`'s own lambda-configurable shape: records every genuinely-appended entry; either `append` or `readAll` can be configured with a throwing behaviour to simulate a durability fault without a real, failing filesystem. Never referenced by `src/composition/`.

---

## Files Created

- `src/runtime/MemoryCoreDurabilityLog.kt`
- `tests/runtime/FakeMemoryCoreDurabilityLog.kt`
- `tests/runtime/MemoryCoreDurabilityLogTest.kt`
- `docs/reviews/MEMORY_CORE_DURABILITY_UNIT_2_STORAGE_INTERFACE_COMPLETION_REVIEW.md` (this document)

## Files Modified

None. `src/interfaces/MemoryCore.kt`, `src/runtime/DurableMemoryCoreEntry.kt`, `src/runtime/InMemoryMemoryCore.kt`, and every other existing production or test file are untouched.

---

## Interface Operations Introduced

| Operation | Signature | Cardinality |
| --- | --- | --- |
| `append` | `suspend fun append(entry: DurableMemoryCoreEntry)` | Exactly one entry per call — no batch, no vararg. |
| `readAll` | `suspend fun readAll(): List<DurableMemoryCoreEntry>` | Returns every durably-appended entry, in exact append order. |

No delete, update, replace, truncate, clear, or compact operation exists — confirmed by a dedicated `declaredFunctions`-based structural test mirroring `EvidenceArtifactStorageScopeTest`'s own established pattern.

---

## Failure Behaviour

Fixed exactly as authorised, no more: both operations propagate a failure as a thrown exception, never a returned value. `readAll` must never substitute an empty list for a failure to read — an empty list means only genuine emptiness. No new public result taxonomy is invented; no dedicated exception type is defined yet (deferred to a future unit, once a real implementation has a real failure mode to name). Corruption classification (partial write versus genuine corruption) is explicitly out of this Unit's own scope. Verified behaviourally against the test-only fake: a configured `append` fault throws and leaves nothing recorded; a configured `readAll` fault throws rather than returning `emptyList()`; a genuinely empty log and an unavailable one are proven distinguishable (one returns normally with an empty list, the other throws).

---

## Tests Added

`tests/runtime/MemoryCoreDurabilityLogTest.kt` — 11 tests, covering every category this task required:

- **Interface visibility.** `MemoryCoreDurabilityLog::class.visibility == KVisibility.INTERNAL`.
- **Exact operation set.** `declaredFunctions` yields exactly `{append, readAll}`, mirroring `EvidenceArtifactStorageScopeTest`'s own pattern.
- **Append accepts one durable entry at a time.** Reflection confirms `append` has exactly one value parameter, typed `DurableMemoryCoreEntry` — no batch form.
- **Read preserves sequence semantics.** A behavioural test appends three entries via the fake and confirms `readAll()` returns them in exact append order.
- **No batch-write operation; no delete, update, replace, truncate, clear, or compact operation.** Covered by the exact-operation-set test above — no such name exists to declare a separate test against.
- **No filesystem or serialization type leakage.** Reflection over every parameter and return type of both operations, rejecting any qualified name matching a filesystem, stream, channel, serializer, or database pattern.
- **No prohibited Parker-domain dependency.** The same reflection sweep, rejecting any type whose qualified name references `PermissionEngine`, Knowledge Memory, `EvidenceCustodian`/`EvidenceIntelligence`, `EventBus`, or `ParkerRuntime`.
- **No public API widening.** The internal-visibility test above, plus direct confirmation (Files Modified, above) that `src/interfaces/MemoryCore.kt` is untouched.
- **Failure behaviour remains explicit and cannot silently become an empty store.** Three dedicated tests: a configured `append` fault propagates and leaves the fake's own backing store empty; a configured `readAll` fault propagates rather than returning an empty list; a genuinely empty log and an unavailable one are proven distinguishable side-by-side in one test.

No production implementation exists to test — every behavioural test exercises `FakeMemoryCoreDurabilityLog` only, per this task's own explicit instruction.

---

## Targeted Verification

```
$ ./gradlew test --tests "*MemoryCoreDurabilityLogTest*"
BUILD SUCCESSFUL in 4s
```

Test report: `tests="11" skipped="0" failures="0" errors="0"`. All 11 tests pass.

---

## Full Repository Verification

```
$ ./gradlew clean test
BUILD SUCCESSFUL in 47s
```

Aggregated across every test-result XML: `tests="1792" skipped="5" failures="0" errors="0"`.

1792 total, up from the pre-Unit-2 baseline of 1781 by exactly the 11 tests this Unit adds. Zero failures, zero errors, the same 5 pre-existing skips as before — zero regression in any other subsystem.

---

## Boundaries Confirmed

Per this task's own explicit instruction, none of the following was implemented, and none appears anywhere in the diff: file I/O; append-only log encoding; atomic flush; replay; startup recovery; restore functions; identifier restoration; lifecycle replay; runtime composition; Docker volume wiring; backup or replication; SQLite; caching; indexing. Confirmed directly — `git status --short` (below) shows exactly three new files under `src/runtime/` and `tests/runtime/`, plus this review document; no file under `src/composition/`, `Dockerfile`, `docker-compose.yml`, or any Permission Engine– or Knowledge-Memory–owned path is touched.

---

## Self-Review, Performed Before This Document Was Presented as Complete

**Every quotation in the production file's own KDoc was checked word-for-word against its cited source before being presented in quotation marks.** Two genuine defects were found and corrected during this self-review, before any Independent Constitutional Review began — the same fabricated/spliced-quotation defect class this session's prior Independent Constitutional Reviews (Units 9.4, 9.5, 9.6, and Unit 1's own self-review) have each independently found:

1. The KDoc's original text spliced two non-adjacent sentences from the Durability Contract Design's own §11 — "No new caller-facing public result type is invented." (the section's own opening sentence) and "a thrown exception" (appearing several sentences later, inside a different clause) — into one continuous quotation joined by an ellipsis. Corrected: the opening sentence is now quoted alone, in full, with its own clean attribution; the reference to `EvidenceArtifactStorage`'s own exception-handling convention is now a separate, accurately-quoted fragment ("sealed and thrown -- not returned as a sealed result type") rather than a paraphrase ("thrown, not returned") presented inside quotation marks as if verbatim.
2. The KDoc's original text presented "no production implementation yet" inside quotation marks, attributed to this governing task's own instruction — but the task's actual text reads "Do not create a production implementation yet," a materially different sentence structure, not merely a capitalisation difference. Corrected to quote the actual sentence.

Both corrections were verified by direct re-read of the corrected text and by a full `./gradlew clean test` re-run confirming no regression (1792 tests, 0 failures, 0 errors — the same count reported above, since these were KDoc-only corrections made before the first test run, not after).

- **No architectural decision was invented beyond the one disclosed redirection (interface-first sequencing), itself explicitly authorised by this session's own governing task.**
- **Mechanism neutrality was preserved.** No serialization technology, storage mechanism, or file format appears anywhere in the new interface.
- **Memory Core Version 1 remains unchanged.** `src/interfaces/MemoryCore.kt` is untouched.
- **Unit 1's own deliverable (`DurableMemoryCoreEntry.kt`) is referenced, never modified** — confirmed both by direct review and by the file remaining absent from `git status --short`'s own modified-file list.
- **No Knowledge Memory, Permission Engine, Evidence Intelligence, or runtime composition content leaked into this Unit.**
- **No production code beyond the one new interface file was written; the fake is clearly test-only, in `tests/runtime/`, never referenced by `src/composition/`.**

---

## Final Git Status

```
$ git status --short
?? src/runtime/MemoryCoreDurabilityLog.kt
?? tests/runtime/FakeMemoryCoreDurabilityLog.kt
?? tests/runtime/MemoryCoreDurabilityLogTest.kt
?? docs/reviews/MEMORY_CORE_DURABILITY_UNIT_2_STORAGE_INTERFACE_COMPLETION_REVIEW.md
```

Nothing staged, committed, or pushed.

---

## Recommended Next Step

An Independent Constitutional Review of this Unit follows, mirroring this session's own unbroken pattern for every implementation unit and governance document produced so far.
