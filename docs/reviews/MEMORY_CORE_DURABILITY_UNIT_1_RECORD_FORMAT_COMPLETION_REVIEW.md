# Memory Core Durability — Unit 1: Durable Record Format Implementation — Completion Review

## Status

**Implementation complete for Unit 1 only.** Units 2 through 10 are not begun. Nothing is staged, committed, or pushed.

---

## Repository Baseline

- **HEAD (before this Unit began):** `15e97791e4081ff8bc4e1b571a888d6e8f322c08`
- **Branch:** `main`
- **Working tree (before this Unit began):** clean apart from the five governance documents produced earlier in this task cycle (the Contract Design and Scope Lock Defect Confirmation Reviews, the Scope Lock itself, its own Independent Constitutional Review, and the Implementation Plan) — no `src/`, no `tests/`.
- **Governing status confirmed at the start of this Unit:** Memory Core Durability Contract Design accepted (per its own Defect Confirmation Review); Memory Core Durability Scope Lock accepted (per its own Independent Constitutional Review and Defect Confirmation Review); Memory Core Durability Implementation Plan complete. No durability implementation existed anywhere in the repository before this Unit.

---

## Governing Documents Read Fresh for This Unit

Per this task's own instruction, not relied on from summary: `docs/architecture/MEMORY_CORE_DURABILITY_CONTRACT_DESIGN.md`; `docs/architecture/MEMORY_CORE_DURABILITY_SCOPE_LOCK.md`; `docs/implementation/MEMORY_CORE_DURABILITY_IMPLEMENTATION_PLAN.md`; `docs/architecture/MEMORY_CORE_CONTRACT_DESIGN.md` (the Version 1 Contract Design, full text); `src/interfaces/MemoryCore.kt`; `src/runtime/InMemoryMemoryCore.kt`; `tests/contracts/MemoryCoreContractsTest.kt`; `tests/contracts/MemoryCoreInterfacesTest.kt`; `tests/runtime/InMemoryMemoryCoreTest.kt` (conventions).

---

## Objective

Implement the internal durable-record representation that will become the append-only persistence boundary — the durable model only. No filesystem I/O, no replay, no change to Memory Core behaviour.

---

## A Disclosed, Deliberate Narrowing of Unit 1's Own Boundary Relative to the Implementation Plan's Own Text

The Implementation Plan's own Unit 1 description (drafted before this implementation session) anticipated "a deterministic, round-trip-safe line-oriented text encoding for each case" living in this same file. This governing task's own explicit instruction narrows that, requiring this Unit to "expose no serialization technology," and separately stating "No JSON library, SQLite library, serializer, parser, or storage implementation is authorised in this unit." This implementation honours the narrower, more specific instruction actually given for this session: `DurableMemoryCoreEntry.kt` contains no `encode`/`decode` function, no text format, and no file I/O of any kind. This is disclosed here explicitly, and in the production file's own KDoc, as a deliberate scope refinement, not a deviation requiring a Plan revision — it moves *where* an already-planned capability (round-trip text encoding) will be implemented, deferred to a future unit's own "internal-only persistence seam" work where the concrete storage format actually belongs, not *whether* it is planned at all. The Implementation Plan's own Deferred Work Register and general mechanism-neutrality discipline already support this deferral.

---

## What Was Implemented

**New file:** `src/runtime/DurableMemoryCoreEntry.kt` — one `internal sealed class DurableMemoryCoreEntry` with exactly six cases, one per `InMemoryMemoryCore` write operation:

| Case | Wraps | Corresponds to |
| --- | --- | --- |
| `ProvenanceCreated` | `Provenance` | `createProvenance` |
| `EntityCreated` | `Entity` | `createEntity` |
| `DocumentRegistered` | `Document` | `registerDocument` |
| `AssertionCreated` | `Assertion` | `createAssertion` |
| `RelationshipCreated` | `Relationship` | `createRelationship` |
| `StatusTransitioned` | (no existing record type — see below) | `transitionStatus` |

**Design decisions, and the reasoning behind each:**

- **Five of six cases wrap the existing, already-frozen domain record type unchanged**, rather than duplicating its own field list a second time. No field is added, removed, or renamed on any of `Provenance`, `Entity`, `Document`, `Assertion`, or `Relationship` — confirmed directly: `src/interfaces/MemoryCore.kt` is untouched by this Unit (see Files Modified, below). This is what makes "preserve immutable identifiers," "preserve timestamps," and "preserve provenance links" true by construction, not by separate, error-prone re-implementation: whatever those five record types already carry, an entry wrapping one carries too.
- **`StatusTransitioned` fixes its own minimal shape**, since no existing domain type represents a bare transition fact: `reference: MemoryCoreRecordReference` (the same sealed type `InMemoryMemoryCore.transitionStatus` itself already accepts), `priorStatus` and `targetStatus` (both, not merely the target, since a transition is a `(prior, target)` pair and recording only the target would lose exactly the "what actually changed" information "preserve lifecycle information" requires), and `transitionedAt: Instant` (an entry-owned timestamp, since none of the four lifecycle-bearing record types carries a "last transitioned at" field of its own today).
- **Every case carries an explicit `schemaVersion: Int`**, defaulted to a single `CURRENT_SCHEMA_VERSION = 1` constant — one source of truth, never hardcoded separately per case.
- **No `requestingPrincipalId` field on any case.** Considered and explicitly declined: this Unit's own requirement list names "preserve immutable identifiers," "preserve timestamps," "preserve provenance links," and "preserve lifecycle information," and does not name "preserve requesting principal." Adding a field the task did not ask for would itself be the kind of unjustified scope expansion the Scope Lock's own Scope Lock Principle prohibits. Disclosed in the production file's own KDoc, not silently decided.
- **`internal`, not public**, on the sealed class and all six cases — mirroring `MemoryCoreLifecycleTransitions`'s own existing precedent in `InMemoryMemoryCore.kt` exactly: reachable by code elsewhere in this module (a future durability implementation; this Unit's own test file), never part of any public, caller-facing contract.
- **`DocumentRegistered`, not `DocumentCreated`** — deliberately mirrors `Document`'s own already-established verb distinction (`registeredAt`, never `createdAt`).

**No file under `src/interfaces/` is touched.** No filesystem, JSON, SQL, or serialization import exists anywhere in the new file — confirmed both by direct review of its own import list (`java.time.Instant` and six types from `parker.core.interfaces`, nothing else) and by a dedicated structural test (below).

---

## Files Created

- `src/runtime/DurableMemoryCoreEntry.kt`
- `tests/runtime/DurableMemoryCoreEntryTest.kt`
- `docs/reviews/MEMORY_CORE_DURABILITY_UNIT_1_RECORD_FORMAT_COMPLETION_REVIEW.md` (this document)

## Files Modified

None. `src/interfaces/MemoryCore.kt`, `src/runtime/InMemoryMemoryCore.kt`, and every other existing production or test file are untouched.

---

## Tests Added

`tests/runtime/DurableMemoryCoreEntryTest.kt` — 18 tests, covering every category this task required:

- **Every durable record kind.** One construction test per case (six total), each confirming the wrapped record's own identifier, timestamps, and provenance link are preserved unchanged, plus a structural "exactly six cases" test against `DurableMemoryCoreEntry::class.sealedSubclasses`.
- **Schema version presence.** Every case defaults its `schemaVersion` to the single `CURRENT_SCHEMA_VERSION` constant (`= 1`); an explicit override is honoured.
- **Identifier preservation.** Each per-kind construction test asserts the wrapped record's own identifier is unchanged after wrapping.
- **Immutable field preservation.** Each per-kind construction test asserts timestamps and provenance links are unchanged; a dedicated reflection-based test confirms no case exposes a mutable (`var`) property anywhere.
- **Lifecycle transition representation.** Two dedicated `StatusTransitioned` tests: one confirming `reference`/`priorStatus`/`targetStatus`/`transitionedAt` are all independently preserved; one confirming `priorStatus` and `targetStatus` are never collapsed to the same value.
- **Version evolution constraints where governed.** `CURRENT_SCHEMA_VERSION == 1` is a fixed, single source of truth; two otherwise-identical entries differing only in `schemaVersion` are proven **not** equal — confirming version genuinely participates in an entry's own identity, the property a future decode step's "reject an unrecognised version" behaviour will depend on.
- **Equality and immutability expectations.** Structural equality for identically-constructed entries; inequality across different cases; a `copy()` test confirming a copy never mutates its own original.
- **Absence of runtime or filesystem dependencies.** A reflection-based test inspects every case's own primary-constructor parameter types directly, failing if any qualified type name starts with `java.nio.file`, `java.io`, `java.sql`, or `javax.sql`, or contains `Json`, `Sqlite`, `Serializer`, `Serializable`, `Parser`, or `Codec` (case-sensitive substring, catching both class and package-name variants); a second test confirms no case declares a `suspend` function anywhere, confirming no I/O of any kind is reachable through this Unit's own types.
- **Internal visibility.** A dedicated test confirms `DurableMemoryCoreEntry` and all six cases report `KVisibility.INTERNAL`, not `PUBLIC`.

No persistence test exists anywhere in this file, per this task's own explicit instruction.

---

## Targeted Verification

```
$ ./gradlew clean test --tests "*DurableMemoryCoreEntryTest*"
BUILD SUCCESSFUL in 36s
```

Test report: `tests="18" skipped="0" failures="0" errors="0"`. All 18 tests pass.

---

## Full Repository Verification

```
$ ./gradlew clean test
BUILD SUCCESSFUL in 42s
```

Aggregated across every test-result XML: `tests="1781" skipped="5" failures="0" errors="0"`.

1781 total, up from the pre-Unit-1 baseline of 1763 by exactly the 18 tests this Unit adds. Zero failures, zero errors, the same 5 pre-existing skips as before — zero regression in any other subsystem.

---

## Boundaries Confirmed

Per this task's own explicit instruction, none of the following was implemented, and none appears anywhere in the diff: persistence; append log; replay; startup recovery; restore methods; runtime composition; Docker integration; Permission Engine changes; Knowledge Memory changes. Confirmed directly — `git status --short` (below) shows exactly two new files under `src/runtime/` and `tests/runtime/`, plus this review document; no file under `src/composition/`, `Dockerfile`, `docker-compose.yml`, or any Permission Engine– or Knowledge-Memory–owned path is touched.

---

## Self-Review, Performed Before This Document Was Presented as Complete

- **Every quotation in the production file's own KDoc was checked word-for-word against its cited source before being presented in quotation marks.** Two genuine defects were found and corrected during this self-review, before any Independent Constitutional Review began:
  1. The KDoc's original text spliced two non-adjacent sentences from this task's own instruction message — "expose no serialization technology" (a Requirements-section bullet) and "No JSON library, SQLite library, serializer, parser, or storage implementation is authorised in this unit." (a separate sentence under "The model must remain mechanism-neutral") — into one continuous quotation joined by an ellipsis, the same fabricated/spliced-quotation defect class this session's prior Independent Constitutional Reviews (Units 9.4, 9.5, 9.6) each independently found and corrected. Corrected: the two fragments are now each quoted separately, with their own attribution, never presented as one continuous sentence.
  2. The KDoc's original text attributed the "only for auditability" language governing `requestingPrincipalId` to "Contract Design Section 15" — checked directly against the Version 1 Contract Design's own full text, that language ("only for auditability (recording who asked) — never as a filter this contract applies on its own") actually appears in **Section 9** (the Retrieval Contract), not Section 15 (Runtime Responsibilities). Corrected to cite Section 9 directly, quoting the exact source sentence rather than paraphrasing it under the wrong citation, and to attribute the parameter's own addition to every operation's signature to Errata 004 separately, rather than conflating the two.

  Both corrections were verified by direct re-read of the corrected text and by a full `./gradlew clean test` re-run (below) confirming no regression. `git diff` was not available to re-verify either correction against a prior commit, since the file was never committed in its uncorrected form.
- **No architectural decision was invented.** Every requirement traces to this task's own explicit instruction list, the Implementation Plan's own Unit 1 section, or the Contract Design's already-fixed durable record scope (§3).
- **Mechanism neutrality was preserved.** No serialization technology, storage mechanism, or file format appears anywhere in the new file.
- **Memory Core Version 1 remains unchanged.** `src/interfaces/MemoryCore.kt` and `src/runtime/InMemoryMemoryCore.kt` are both untouched.
- **No Knowledge Memory, Permission Engine, or runtime composition content leaked into this Unit.**
- **No production code beyond the one new file was written; no test beyond the one new test file was written.**

---

## Final Git Status

```
$ git status --short
?? docs/architecture/MEMORY_CORE_DURABILITY_SCOPE_LOCK.md
?? docs/implementation/MEMORY_CORE_DURABILITY_IMPLEMENTATION_PLAN.md
?? docs/reviews/MEMORY_CORE_DURABILITY_CONTRACT_DESIGN_DEFECT_CONFIRMATION_REVIEW.md
?? docs/reviews/MEMORY_CORE_DURABILITY_SCOPE_LOCK_DEFECT_CONFIRMATION_REVIEW.md
?? docs/reviews/MEMORY_CORE_DURABILITY_SCOPE_LOCK_INDEPENDENT_CONSTITUTIONAL_REVIEW.md
?? docs/reviews/MEMORY_CORE_DURABILITY_UNIT_1_RECORD_FORMAT_COMPLETION_REVIEW.md
?? src/runtime/DurableMemoryCoreEntry.kt
?? tests/runtime/DurableMemoryCoreEntryTest.kt
```

Nothing staged, committed, or pushed.

---

## Recommended Next Step

An Independent Constitutional Review of this Unit follows, mirroring this session's own unbroken pattern for every implementation unit and governance document produced so far.
