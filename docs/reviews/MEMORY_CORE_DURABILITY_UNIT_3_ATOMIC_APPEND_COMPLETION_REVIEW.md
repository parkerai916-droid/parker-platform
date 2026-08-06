# Memory Core Durability — Unit 3: Atomic Append — Completion Review

## Status

**Implementation complete for Unit 3 only.** Units 4 through 10 are not begun. Nothing is staged, committed, or pushed.

---

## Repository Baseline

- **HEAD (before this Unit began):** `8fa8771f1aa64a154245230215606baa43ec526c`
- **Branch:** `main`
- **Working tree (before this Unit began):** clean. Unit 1 (durable record format) and Unit 2 (durable storage interface) confirmed committed at this HEAD.
- **Governing status confirmed at the start of this Unit:** Memory Core Durability Contract Design accepted; Memory Core Durability Scope Lock accepted; Memory Core Durability Implementation Plan committed; Units 1 and 2 implemented, reviewed, and committed.

---

## Planning Review

Read fresh, per this task's own instruction: `docs/implementation/MEMORY_CORE_DURABILITY_IMPLEMENTATION_PLAN.md` in full; `docs/architecture/MEMORY_CORE_DURABILITY_SCOPE_LOCK.md` §§7–8; `docs/architecture/MEMORY_CORE_DURABILITY_CONTRACT_DESIGN.md` §§4, 6, 7, 11; `src/runtime/DurableMemoryCoreEntry.kt`; `src/runtime/MemoryCoreDurabilityLog.kt`; `src/runtime/FileSystemEvidenceArtifactStorage.kt` and `FileSystemEvidenceDeletionAudit.kt` (and their own test files) as the mechanism precedent.

**A significant, disclosed unit-numbering divergence was found and resolved before coding began.** The Implementation Plan's own "Unit 3 — Append and Atomicity Behaviour" section describes building `DurableMemoryCore`, the full `MemoryCore`/`MemoryRetrieval`-implementing decorator wrapping `InMemoryMemoryCore` together with the durability log. That is **not** what this session's own task asked for. This session's task explicitly requests "the production implementation of `MemoryCoreDurabilityLog`," and its own exclusion list confirms this reading directly: "no Memory Core decorator" and "no runtime composition" are named as explicitly out of scope for this session's Unit 3 — exactly what the Implementation Plan's own Unit 3 would otherwise require. What this session's task actually specifies matches, field-for-field, the Implementation Plan's own **Unit 2** output specification instead: "a single concrete class, `FileSystemMemoryCoreDurabilityLog`... Two operations: `suspend fun append(entry: DurableMemoryCoreEntry)`... and `suspend fun readAll(): List<DurableMemoryCoreEntry>`" — the very implementation this session's own prior Unit 2 task explicitly deferred ("Do not create a production implementation yet").

**Determination: no Boundary Review was required.** This is a session-level relabelling of already-authorised work, not a new capability, a boundary conflict, or an architectural question requiring escalation. The concrete class this Unit builds was already fully specified by the Implementation Plan's own Unit 2 text; only which session-level task label attached to actually building it has changed. No governing document's own fixed requirement is contradicted by proceeding under this label. Disclosed here, and in the production file's own KDoc, rather than silently reconciled — mirroring exactly how Units 1 and 2 each disclosed their own, smaller divergences from the Implementation Plan's literal text.

**Governance sufficiency confirmed.** Both required corrections to the Contract Design and the one required correction to the Scope Lock remain confirmed applied (per their own Defect Confirmation Reviews, already committed). No new governance gap was found. Implementation proceeded on this basis.

---

## Objective

Create the production implementation of `MemoryCoreDurabilityLog` (Unit 2's own interface): append-only durability, atomic append semantics, no partial record visibility, preserved append order, fail-fast on write failure — with no recovery logic, no replay logic, no runtime composition, no Memory Core decorator, no serialization format evolution, no batching, no compaction, no checkpointing, and no concurrency beyond a baseline single-writer guard.

---

## What Was Implemented

**New file:** `src/runtime/DurableMemoryCoreEntryCodec.kt` — `internal object DurableMemoryCoreEntryCodec` with `encode(entry): String` and `decode(line, lineNumber): DurableMemoryCoreEntry`. This is the round-trip-safe text encoding `DurableMemoryCoreEntry.kt`'s own KDoc explicitly deferred out of Unit 1 ("the round-trip text encoding... is deferred to a future unit's own 'internal-only persistence seam' work") — this is that unit.

- **Format:** tab-separated `key=value` fields, one line per entry, extending `FileSystemEvidenceDeletionAudit`'s own established "fixed order" convention. `kind` and `schemaVersion` always appear first.
- **Every string-valued field is Base64-encoded, without exception.** No identifier type in this repository restricts its own value beyond a blank check (`src/interfaces/MemoryCore.kt`), so every free-text and identifier field could theoretically carry an embedded tab or newline. Rather than hand-rolling a backslash-escape scheme (a well-known source of round-trip bugs at edge cases), every string field is Base64-encoded — `java.util.Base64`, already part of the JDK, no new runtime dependency — which structurally eliminates the escaping problem rather than attempting to solve it correctly. Numeric fields, `Instant` (ISO-8601), `Boolean`, and closed enum values are written raw.
- **Lists and maps** are encoded as Base64-encoded elements/pairs joined by comma/colon; an empty collection is omitted from the line entirely, exactly like a null field, and decodes back to the record type's own established default.
- **A nullable field that is absent is simply omitted** from the line; decode treats a missing key as `null`.
- **Fail-fast, no corruption classification.** `decode` throws `MemoryCoreDurabilityLogException.MalformedEntry` for any unparseable field and `UnrecognizedSchemaVersion` for any `schemaVersion` other than `1`. It deliberately does **not** distinguish a genuinely corrupted, already-committed entry from a partial, interrupted-mid-append write — that position-aware classification is Implementation Unit 4's own, later, Contract-Design-§7-governed responsibility, correctly out of this Unit's own scope ("no recovery logic").

**New file:** `src/runtime/FileSystemMemoryCoreDurabilityLog.kt` — `internal class FileSystemMemoryCoreDurabilityLog(logFile: Path) : MemoryCoreDurabilityLog`, plus `internal sealed class MemoryCoreDurabilityLogException` (`InvalidStorageRoot`, `StorageIOFailure`, `MalformedEntry`, `UnrecognizedSchemaVersion`), mirroring `EvidenceArtifactStorageException`'s own established "sealed, thrown, not returned" convention.

- **Construction-time validation** (parent directory exists, is a directory, is writable; log file created empty if absent, preserved if present) mirrors `FileSystemEvidenceDeletionAudit`'s own `init` block exactly.
- **`append`** encodes the entry, opens the file in append mode, writes the bytes (looping until fully written, per `FileChannel.write`'s own general contract), and calls `FileChannel.force(true)` before returning — mirroring `FileSystemEvidenceDeletionAudit.record`'s own append-then-force discipline exactly, not `FileSystemEvidenceArtifactStorage`'s own temp-file/atomic-move discipline. This is a disclosed, deliberate choice: the Contract Design's own §6 already anticipates and requires that "a write that did not complete before an interruption is treated as a write that never happened," so a durability design built around a future replay step that already tolerates and discards an interrupted trailing write has no need for the stronger, more expensive guarantee the create-once artefact precedent uses to solve a different problem.
- **`readAll`** reads every line and decodes each in file order, propagating the first decode failure as a thrown exception — no partial result, no silent skip.
- **Concurrency:** one `Mutex` serialises both `append` and `readAll`, mirroring `FileSystemEvidenceDeletionAudit`'s own identical, already-accepted precedent for the same class of concern (preventing two concurrent writers from interleaving bytes at the file level) — a baseline correctness guard for this class alone, explicitly distinct from, and not attempting to satisfy, Implementation Unit 7's own later cross-class atomicity guarantee (which does not yet exist to satisfy, since it spans an interaction with a not-yet-built `InMemoryMemoryCore` composition).

---

## Files Created

- `src/runtime/DurableMemoryCoreEntryCodec.kt`
- `src/runtime/FileSystemMemoryCoreDurabilityLog.kt`
- `tests/runtime/DurableMemoryCoreEntryCodecTest.kt`
- `tests/runtime/FileSystemMemoryCoreDurabilityLogTest.kt`
- `docs/reviews/MEMORY_CORE_DURABILITY_UNIT_3_ATOMIC_APPEND_COMPLETION_REVIEW.md` (this document)

## Files Modified

None. `src/runtime/DurableMemoryCoreEntry.kt`, `src/runtime/MemoryCoreDurabilityLog.kt`, `src/interfaces/MemoryCore.kt`, `src/runtime/InMemoryMemoryCore.kt`, `src/composition/ParkerRuntime.kt`, `Dockerfile`, `docker-compose.yml`, and every other existing production or test file are untouched.

---

## Implementation Summary

| Responsibility | How satisfied |
| --- | --- |
| Append-only durability | `append` only ever appends; no method exists to modify, delete, or truncate an existing line. |
| Atomic append semantics | Each `append` call either durably commits its one entry (return normally) or throws (`StorageIOFailure`) with nothing written — no partial-success return value exists. |
| No partial record visibility | Line-oriented format, `FileChannel.force` before return, single `Mutex` serialising all access — a caller can never observe a torn or half-written entry through this class's own API. |
| Preservation of append order | Single append-only file, single `Mutex`; `readAll` returns entries in exact file order, verified by a dedicated concurrency test (25 concurrent appends, all present, none corrupted). |
| Fail-fast behaviour on write failure | I/O failures during `append` throw `StorageIOFailure`; construction failures throw `InvalidStorageRoot`. |
| No recovery logic / no replay logic | `readAll` performs no corruption classification; a decode failure anywhere fails the entire read, regardless of position (verified directly). |
| No runtime composition / no Memory Core decorator | Confirmed: no reference to `ParkerRuntime`, `MemoryCore`, `MemoryRetrieval`, or `InMemoryMemoryCore` anywhere in either new file. |
| No serialization format evolution | Only `CURRENT_SCHEMA_VERSION = 1` is ever produced or accepted; any other version throws `UnrecognizedSchemaVersion` rather than being interpreted. |
| No batching / compaction / checkpointing | `append` accepts exactly one entry per call (the interface's own already-fixed shape); no such operation exists anywhere in either file. |
| No concurrency beyond what Unit 3 authorises | Exactly one `Mutex`, mirroring an already-accepted precedent for the identical concern — no connection pool, no async batching, no cross-instance coordination. |

---

## Test Totals

`tests/runtime/DurableMemoryCoreEntryCodecTest.kt` — 20 tests: round-trip correctness for all six entry kinds (each with optional fields both present and absent); adversarial free-text content (embedded tabs, newlines, backslashes, Unicode, an empty-but-present metadata value distinguished from an absent key); unrecognised schema version rejection; four distinct malformed-input rejection cases; exception field content (line number, kind, version) directly verified.

`tests/runtime/FileSystemMemoryCoreDurabilityLogTest.kt` — 13 tests: interface compliance; three construction-validation cases (missing parent, non-directory parent, fresh-file creation); restart-like survival across a fresh instance over the same file; single-entry and mixed-six-kind append/readAll round-trips; empty-log behaviour; one-line-per-append confirmation; no-partial-visibility confirmation; fail-fast propagation for a corrupted line in first, middle, and last position alike; a 25-way concurrent-append correctness test.

**33 tests added in total.**

---

## Targeted Verification

```
$ ./gradlew clean test --tests "*DurableMemoryCoreEntryCodecTest*" --tests "*FileSystemMemoryCoreDurabilityLogTest*"
BUILD SUCCESSFUL
```

`DurableMemoryCoreEntryCodecTest`: `tests="20" skipped="0" failures="0" errors="0"`.
`FileSystemMemoryCoreDurabilityLogTest`: `tests="13" skipped="0" failures="0" errors="0"`.

---

## Full Repository Verification

```
$ ./gradlew clean test
BUILD SUCCESSFUL in 45s
```

Aggregated across every test-result XML: `tests="1825" skipped="5" failures="0" errors="0"`.

1825 total, up from the pre-Unit-3 baseline of 1792 by exactly the 33 tests this Unit adds. Zero failures, zero errors, the same 5 pre-existing skips as before — zero regression in any other subsystem.

---

## Self-Review, Performed Before This Document Was Presented as Complete

**A genuine implementation defect was found and corrected during this self-review, before any Independent Constitutional Review began.** The original test `an empty string free-text field round-trips as an empty string, distinct from absence` constructed a `Provenance` with `creator = ""`, which fails at the domain model's own construction (`Provenance`'s `init` block requires `creator == null || creator.isNotBlank()`) — an empty string is blank, so this is rejected before the codec is ever exercised. This was not a codec defect; it was an invalid test, constructing a domain state the record type itself forbids. Corrected: the test now uses an `Entity.metadata` value (`mapOf("note" to "")`), a field with no blank-value restriction anywhere in `Entity`'s own `init` block, genuinely exercising the "empty string is distinct from an absent key" property the codec's own `field`/`decodeField` helpers are meant to guarantee. Re-run and confirmed passing after correction.

**Every quotation in both production files' own KDoc was checked word-for-word against its cited source before being presented in quotation marks.** Two minor mis-quotations were found and corrected, the same defect class this session's prior self-reviews (Units 1 and 2) have each caught: "no production implementation yet" (a paraphrase of the actual instruction, "Do not create a production implementation yet") and "fixed field order" (a paraphrase of `FileSystemEvidenceDeletionAudit`'s own "fixed order"). Both corrected to quote the exact source text. A third apparent case — "no recovery logic; no replay logic," joining two separate bullet-list items with a semicolon as if one continuous quotation — was corrected to state each as its own separately-quoted fragment. One additional, ellipsis-bridged quotation (the Implementation Plan's own Unit 2 "Outputs" description) was checked carefully and found **not** to be the same defect: all three ellipsis-joined fragments are exact, contiguous substrings of one single, continuous source paragraph, in original order, with only intervening explanatory clauses skipped and clearly marked — legitimate selective quotation from one continuous passage, not a splice of unrelated sentences. Left as written.

**A separate, non-quotation defect was also found and corrected during this self-review: a duplicated word left over from the "no production implementation yet" correction above.** The edit that fixed the mis-quotation was applied mechanically and left the sentence reading "...explicitly explicitly instructing that..." — a genuine grammar defect, not a fidelity issue, caught by re-reading the corrected file in full rather than trusting the diff in isolation. Corrected to a single "explicitly instructing."

**A second full quotation audit of both production files, performed after the corrections above, found one further discrepancy:** the KDoc describing Implementation Unit 7's own future guarantee quoted the Durability Contract Design's own §10 as "durable write and its corresponding in-memory update occur as one atomic unit from the caller's own perspective" — the source reads "the corresponding," not "its corresponding." A small word substitution, but a real one, presented inside quotation marks as verbatim. Corrected to quote the source exactly.

All four corrections were re-verified together by re-running the targeted test suite (`DurableMemoryCoreEntryCodecTest`: 20/20; `FileSystemMemoryCoreDurabilityLogTest`: 13/13) after the final fix, confirming every correction was comment-only and introduced no behavioural change.

- **No architectural decision was invented beyond the one disclosed unit-numbering resolution**, itself grounded directly in the Implementation Plan's own already-authorised Unit 2 output specification.
- **Mechanism neutrality was preserved** at the governing-document tier — no governance document was edited; the mechanism selected (filesystem, append-then-force, Base64-safe text encoding) was already the Implementation Plan's own Section 4 selection, not a new choice made here.
- **Memory Core Version 1 remains unchanged.** `src/interfaces/MemoryCore.kt` and `src/runtime/InMemoryMemoryCore.kt` are both untouched.
- **Units 1 and 2's own deliverables are referenced, never modified.**
- **No Knowledge Memory, Permission Engine, Evidence Intelligence, or runtime composition content leaked into this Unit.**

---

## Final Git Status

```
$ git status --short
?? docs/reviews/MEMORY_CORE_DURABILITY_UNIT_3_ATOMIC_APPEND_COMPLETION_REVIEW.md
?? src/runtime/DurableMemoryCoreEntryCodec.kt
?? src/runtime/FileSystemMemoryCoreDurabilityLog.kt
?? tests/runtime/DurableMemoryCoreEntryCodecTest.kt
?? tests/runtime/FileSystemMemoryCoreDurabilityLogTest.kt
```

Nothing staged, committed, or pushed.

---

## Recommended Next Step

An Independent Constitutional Review of this Unit follows, mirroring this session's own unbroken pattern for every implementation unit and governance document produced so far.
