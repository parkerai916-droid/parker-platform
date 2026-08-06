# Memory Core Durability — Unit 1: Durable Record Format Implementation — Independent Constitutional Review

## Status

**Genuine Independent Constitutional Review**, performed as if by another reviewer, against the governing documents re-read fresh, and against the actual, current file contents — not against the Completion Review's own summary of them. This document does not amend `src/runtime/DurableMemoryCoreEntry.kt`, `tests/runtime/DurableMemoryCoreEntryTest.kt`, the Completion Review, or any governance document. It identifies conflict, or its absence, and states a determination.

---

## 1. Baseline Confirmation

`HEAD` is `15e97791e4081ff8bc4e1b571a888d6e8f322c08`, unchanged since this task began. The working tree carries exactly the expected set: five governance documents from the earlier stages of this task cycle, plus this Unit's own three new files (production, test, Completion Review). No other file is touched.

---

## 2. Scope and Method

This review re-reads `docs/implementation/MEMORY_CORE_DURABILITY_IMPLEMENTATION_PLAN.md`'s own Unit 1 section, `docs/architecture/MEMORY_CORE_DURABILITY_CONTRACT_DESIGN.md`, and `docs/architecture/MEMORY_CORE_CONTRACT_DESIGN.md` (the Version 1 Contract Design) fresh, and checks `src/runtime/DurableMemoryCoreEntry.kt` line-by-line against each. Every quotation the production file's own KDoc makes of any governing document is checked word-for-word against its cited source and section number — the same discipline that has caught a genuine defect in the Independent Constitutional Review of nearly every prior governance-tier document and implementation unit this session has produced. The Completion Review's own account of a two-defect self-correction, performed before this review began, is independently re-verified against the file's own current text, not accepted on the Completion Review's own say-so.

---

## 3. Re-Verification of the Two Self-Corrections the Completion Review Reports

**Test:** did the two corrections the Completion Review describes actually land in the current text, correctly?

Checked directly:

1. **The spliced-quotation correction.** The current text (lines 27–31) reads: `requiring this Unit to "expose no serialization technology," and separately stating that "no JSON library, SQLite library, serializer, parser, or storage implementation is authorised in this unit."` — two independently quoted, independently attributed fragments, joined by ordinary prose ("and separately stating that"), never by an ellipsis presenting them as one continuous sentence. Checked against the governing task's own instruction text: `"expose no serialization technology."` is a verbatim Requirements-section bullet; `"No JSON library, SQLite library, serializer, parser, or storage implementation is authorised in this unit."` is a verbatim, separate sentence under "The model must remain mechanism-neutral." Both fragments are quoted exactly, including punctuation, modulo the leading-capital-to-lowercase adjustment standard practice permits at a quotation's own grammatical seam. **Correctly applied.**
2. **The mis-citation correction.** The current text (lines 109–112) reads: `but "only for auditability (recording who asked) -- never as a filter" (`docs/architecture/MEMORY_CORE_CONTRACT_DESIGN.md` Section 9)`. Checked directly against the Version 1 Contract Design's own Section 9 (Retrieval Contract), re-read in full for this review: "Every operation below still accepts a `requestingPrincipalId`, but only for auditability (recording who asked) — never as a filter this contract applies on its own." The quoted fragment is a clean, contiguous, accurately truncated substring of that sentence (truncated at "filter," dropping "this contract applies on its own," which changes nothing about what the retained fragment claims). Section 15 (Runtime Responsibilities), the original, incorrect citation, was independently checked and confirmed to contain no comparable sentence — the correction is not merely plausible but demonstrably necessary. **Correctly applied.**

Both self-reported corrections are genuine and correctly executed. No third, undisclosed defect of the same class (a citation or quotation not checked) was found by re-scanning every remaining quotation mark in the file (Section 4, below, covers this systematically).

---

## 4. Full Citation and Quotation Audit, Independent of the Completion Review's Own Account

Every quoted fragment in the current file, checked individually against its cited source:

| Quoted fragment | Cited source | Verified |
| --- | --- | --- |
| "one `MemoryCore` contract operation is one atomic durable act" | Durability Contract Design §4 | Exact match (source: "**One `MemoryCore` contract operation is one atomic durable act.**"), case/punctuation adjusted only at the quotation's own grammatical seam. |
| "a deterministic, round-trip-safe line-oriented text encoding for each case" | Implementation Plan, Unit 1 | Exact, contiguous substring match. |
| "no field added, removed, or renamed by this document" | Durability Contract Design §3 | Exact match. |
| "Lifecycle transitions are appended, not rewritten" | Durability Contract Design §9 | Exact match. |
| "every durably stored record carries an explicit schema-version tag" | Durability Contract Design §8 | Exact match. |
| "internal-only persistence seam" | Implementation Plan, Unit 2 | Exact match (Unit 2's own heading text: "The internal-only persistence seam"). |
| "preserve immutable identifiers," "preserve timestamps," "preserve provenance links," "preserve lifecycle information" | This governing task's own Unit 1 requirement list | Each an individually accurate fragment of the literal list ("preserve immutable identifiers; preserve timestamps; preserve provenance links; preserve lifecycle information where applicable"); none spliced with another. |
| "preserve requesting principal" | (Negatively cited — confirmed absent from the task's own list) | Confirmed: no such phrase appears anywhere in the governing task's own requirement list. |
| "the burden of proof favours exclusion, not inclusion" | Durability Scope Lock, Status section | Exact match. |
| "only for auditability (recording who asked) -- never as a filter" | Version 1 Contract Design §9 | Verified in Section 3, above. |

**No further defect found.** Every citation and quotation in the current file is accurate, correctly attributed, and correctly sectioned.

---

## 5. Boundary Discipline — Checked Against the Task's Own Explicit "Do Not Implement" List

**Test:** does anything in the diff cross into persistence, append log, replay, startup recovery, restore methods, runtime composition, Docker integration, Permission Engine changes, or Knowledge Memory changes?

Checked directly: `src/runtime/DurableMemoryCoreEntry.kt` contains no `suspend` function (confirmed both by direct reading and by the test file's own dedicated structural test), no file-path or `Path`/`File` type anywhere in any constructor signature, no reference to `PermissionEngine`, `KnowledgeItem`, `KnowledgeStore`, `EventBus`, or `ParkerRuntime`, and no import beyond `java.time.Instant` and six `parker.core.interfaces` types. `git status --short` confirms no file under `src/composition/`, `Dockerfile`, or `docker-compose.yml` is touched. **Sound** — this Unit's own boundary is honoured in full.

---

## 6. Structural Correctness of the Data Model

**Test:** does the six-case design genuinely satisfy "one entry per write operation," and does `StatusTransitioned`'s own shape genuinely satisfy "preserve lifecycle information where applicable" without inventing a field the governing task did not authorise?

Checked directly against `src/interfaces/MemoryCore.kt`'s own six write operations (`createProvenance`, `createEntity`, `registerDocument`, `createAssertion`, `createRelationship`, `transitionStatus`): each has exactly one corresponding `DurableMemoryCoreEntry` case, confirmed by the test suite's own `sealedSubclasses` structural test. `StatusTransitioned`'s own four fields (`reference`, `priorStatus`, `targetStatus`, `transitionedAt`) are each independently justified: `reference` reuses the existing `MemoryCoreRecordReference` sealed type rather than inventing a new one; `priorStatus`/`targetStatus` together, not `targetStatus` alone, are the minimum shape that actually represents "what changed," not merely "what it is now"; `transitionedAt` is a genuinely new field with no existing home on any of the four lifecycle-bearing record types, and is disclosed as such rather than silently invented. **Sound.**

---

## 7. The `requestingPrincipalId` Omission — Independently Re-Tested, Not Merely Accepted

**Test:** is declining to carry `requestingPrincipalId` on each entry actually correct, or does it silently discard information a future recovery/audit unit will need and have no way to recover?

This is worth testing independently rather than accepting the Completion Review's own "the task didn't ask for it" reasoning at face value, since a future Implementation Plan unit could conceivably need this information and find it irrecoverably absent. Checked: the Durability Contract Design's own §3 (Durable Record Scope) fixes durability at exactly "the five existing record kinds... with no field added" plus lifecycle transitions "as its own discrete, appended fact" — it does not name `requestingPrincipalId` as part of what must be durable, and the Contract Design's own §13 Explicit Exclusions table separately excludes "the constitutional Audit log ADR-024 §D, Rule 17, separately requires" from this Programme's own scope entirely, which is the more natural home for "who requested every act," if that is ever required. The omission is therefore not merely permitted by the absence of an instruction — it is consistent with the Contract Design's own already-fixed scope, which this Unit correctly declined to exceed. **Sound**, and correctly reasoned, not merely permitted by silence.

---

## 8. Finding — A Disclosed, Non-Blocking Gap Between the Implementation Plan's Own Unit 1 Completion Criteria and What This Session Actually Authorised

**Test:** does "Unit 1 is complete" hold, given the Implementation Plan's own Unit 1 completion criteria (drafted before this implementation session) require round-trip encode/decode tests that do not exist anywhere in this Unit's own deliverables?

Checked directly against the Implementation Plan's own text: Unit 1's stated completion criteria include "Every one of the six entry cases round-trips exactly (encode then decode reproduces an identical value...) under property-style and adversarial-input tests." No `encode`/`decode` function, and no round-trip test, exists anywhere in this Unit's own deliverables — confirmed directly, and disclosed openly by the Completion Review itself, not discovered here for the first time.

**This is not a defect in what was implemented.** The governing task for this specific implementation session is more specific and more current than the Implementation Plan's own general Unit 1 description, and states its own boundary explicitly and repeatedly: "expose no serialization technology," "No JSON library, SQLite library, serializer, parser, or storage implementation is authorised in this unit," and "No persistence tests yet." A more specific, later, directly-given instruction lawfully narrows a more general, earlier planning document's own description of the same unit's boundary — this is not a conflict requiring escalation, since nothing in the Implementation Plan's own governing authority (the Durability Scope Lock, the Durability Contract Design) is itself violated by deferring the text-encoding capability to a later unit; only the Implementation Plan's own *description* of Unit 1's own completion criteria is now inaccurate relative to what was actually built under this name.

**What this does leave outstanding:** the Implementation Plan document itself, `docs/implementation/MEMORY_CORE_DURABILITY_IMPLEMENTATION_PLAN.md`, was not, and could not lawfully be, updated by this Unit's own task (which authorised implementing Unit 1 only, not editing planning or governance documents). A future reader consulting the Implementation Plan alone, without also reading this Unit's own Completion Review, could reasonably expect to find round-trip encode/decode tests in `tests/runtime/DurableMemoryCoreEntryTest.kt` and be surprised not to. This is a **minor, disclosed, non-blocking finding** — not a required correction within this Unit's own authorised scope, since correcting it would require editing a document this task did not authorise touching. It is recommended, not required, that a future housekeeping pass (itself separately authorised) update the Implementation Plan's own Unit 1 section to match the boundary this session's own governing task actually fixed, mirroring exactly how this session has already handled an analogous stale-text situation (the Durability Contract Design's own un-updated status line, resolved narratively rather than by unauthorised edit).

No other finding was identified.

---

## Findings

### Finding 1 (Minor, Non-Blocking) — Implementation Plan's own Unit 1 completion criteria are now stale relative to this session's own more specific, authoritative narrowing

See Section 8, above. Not a defect in the Kotlin implementation itself, which correctly and faithfully follows the actual, current, most-specific governing instruction for this session. No correction is required within this Unit's own authorised scope. Recommended for a future, separately-authorised housekeeping pass to the Implementation Plan document.

No other required correction was found. The two self-corrections the Completion Review reports are independently re-verified as genuine and correctly applied (Section 3). The full citation and quotation audit found no further defect (Section 4). Boundary discipline, structural correctness of the data model, and the `requestingPrincipalId` omission are all confirmed sound (Sections 5, 6, 7).

---

## Constitutional Verdict

```
ACCEPTED
```

No required correction. One minor, disclosed, non-blocking finding, already fully accounted for by this session's own explicit, authoritative task instructions and outside this Unit's own authorised scope to resolve. The implementation, its tests, and its own Completion Review are each independently confirmed sound.

---

## Recommended Next Step

No further correction or Defect Confirmation Review is required for Unit 1. Per this task's own explicit stop point, work halts here: Unit 2 is not begun; nothing is staged, committed, or pushed.

---

## Final Git Status at Time of This Review

```
$ git status --short
?? docs/architecture/MEMORY_CORE_DURABILITY_SCOPE_LOCK.md
?? docs/implementation/MEMORY_CORE_DURABILITY_IMPLEMENTATION_PLAN.md
?? docs/reviews/MEMORY_CORE_DURABILITY_CONTRACT_DESIGN_DEFECT_CONFIRMATION_REVIEW.md
?? docs/reviews/MEMORY_CORE_DURABILITY_SCOPE_LOCK_DEFECT_CONFIRMATION_REVIEW.md
?? docs/reviews/MEMORY_CORE_DURABILITY_SCOPE_LOCK_INDEPENDENT_CONSTITUTIONAL_REVIEW.md
?? docs/reviews/MEMORY_CORE_DURABILITY_UNIT_1_RECORD_FORMAT_COMPLETION_REVIEW.md
?? docs/reviews/MEMORY_CORE_DURABILITY_UNIT_1_RECORD_FORMAT_INDEPENDENT_CONSTITUTIONAL_REVIEW.md
?? src/runtime/DurableMemoryCoreEntry.kt
?? tests/runtime/DurableMemoryCoreEntryTest.kt
```

Nothing staged, committed, or pushed.
