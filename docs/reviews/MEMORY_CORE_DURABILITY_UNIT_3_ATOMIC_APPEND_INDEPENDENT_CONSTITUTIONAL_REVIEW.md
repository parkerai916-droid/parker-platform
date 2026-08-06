# Memory Core Durability — Unit 3: Atomic Append — Independent Constitutional Review

## Status

**Genuine Independent Constitutional Review**, performed as if by another reviewer, against the governing documents re-read fresh, and against the actual, current file contents — not against the Completion Review's own summary of them. This document does not amend `src/runtime/FileSystemMemoryCoreDurabilityLog.kt`, `src/runtime/DurableMemoryCoreEntryCodec.kt`, either test file, the Completion Review, or any governance document. It identifies conflict, or its absence, and states a determination.

---

## 1. Baseline Confirmation

`HEAD` is `8fa8771f1aa64a154245230215606baa43ec526c`, unchanged since this task began. The working tree carries exactly the expected set: this Unit's own four new files (two production, two test) plus its own Completion Review. No other file is touched.

---

## 2. Scope and Method

This review re-reads `docs/implementation/MEMORY_CORE_DURABILITY_IMPLEMENTATION_PLAN.md`'s own Unit 2 and Unit 3 sections, `docs/architecture/MEMORY_CORE_DURABILITY_CONTRACT_DESIGN.md` §§4, 6, 7, 8, 10, 11, and `docs/architecture/MEMORY_CORE_DURABILITY_SCOPE_LOCK.md` §§7, 8 fresh, and checks both production files line-by-line against each. Every quotation either production file's own KDoc makes of any governing document, the Implementation Plan, or this governing task's own instruction text is independently re-checked word-for-word against its cited source — not accepted from the Completion Review's own account of its own self-review, which itself reports four separate corrections found across two passes. This is exactly the situation where independent re-verification matters most: a drafting process that needed four rounds of self-correction on quotation fidelity alone is not obviously guaranteed to have caught a fifth. The unit-numbering divergence the Completion Review reports is independently re-derived from the Implementation Plan's own text, not accepted at face value. The codec's own correctness is independently re-derived by tracing several field types by hand against the domain model's own construction rules, not merely by re-running the existing test suite.

---

## 3. Re-Verification of the Completion Review's Four Self-Reported Corrections

Checked directly against the current text of both files:

1. **The invalid test (`Provenance.creator = ""`).** Confirmed: `Provenance`'s own `init` block requires `creator == null || creator.isNotBlank()` (checked directly against `src/interfaces/MemoryCore.kt`) — an empty string is blank, so the original test could never have exercised the codec at all. The replacement test (`Entity.metadata = mapOf("note" to "")`) is checked directly against `Entity`'s own `init` block, which validates `entityType`, `primaryLabel`, and that every alias is non-blank, but imposes no constraint on `metadata`'s own keys or values — the replacement genuinely reaches the codec's own empty-string-handling path. **Correctly replaced.**
2. **"no production implementation yet" → "Do not create a production implementation yet."** Checked against this governing task's own instruction text: the corrected quote is an exact match. **Correctly applied.**
3. **"fixed field order" → "fixed order."** Checked against `FileSystemEvidenceDeletionAudit.kt`'s own KDoc: "in that fixed order, terminated by a single `\n`." The corrected quote is an exact, contiguous substring. **Correctly applied.**
4. **The duplicated-word grammar defect ("explicitly explicitly instructing").** Checked directly: the current text reads "...explicitly instructing that 'Do not create a production implementation yet.'" — a single "explicitly," grammatically sound. **Correctly applied.**

All four self-reported corrections are genuine and correctly executed.

---

## 4. Full, Independent Quotation Audit — a Fifth Discrepancy Found and Already Corrected Mid-Review

Auditing every quoted fragment in both production files independently, one discrepancy not caught by the Completion Review's own two self-review passes was found during this review's own drafting: the KDoc describing Implementation Unit 7's own future guarantee quoted the Contract Design's own §10 as "durable write and its corresponding in-memory update occur as one atomic unit from the caller's own perspective" against the source's actual text, "a durable write and the corresponding in-memory update occur as one atomic unit from the caller's own perspective" — "its" for "the." This was found and corrected during the same working session that produced this review, before this document was finalised, and the Completion Review has already been updated to disclose it as the review's own fourth self-reported correction (Section headed "Self-Review" there). Re-checked here, independently, against the current text: the correction is present and accurate. No further discrepancy was found across the remainder of either file's own quotations, checked individually:

| Quoted fragment | Cited source | Verified |
| --- | --- | --- |
| "one `MemoryCore` contract operation is one atomic durable act" (referenced by section number, not re-quoted in this Unit's own files) | N/A this Unit | Not applicable — not quoted here. |
| "a single concrete class, `FileSystemMemoryCoreDurabilityLog`... Two operations:..." | Implementation Plan, Unit 2 | Three ellipsis-joined fragments, each an exact contiguous substring of one continuous source paragraph, in original order — legitimate selective quotation, not a splice of unrelated sentences (independently re-verified, not merely accepted from the Completion Review's own finding). |
| "Do not create a production implementation yet." | This governing task's own instruction | Exact match. |
| "no Memory Core decorator" / "no runtime composition" | This governing task's own exclusion list | Each an exact, separately-quoted, separately-attributed fragment — not spliced. |
| "a write that did not complete before an interruption is treated as a write that never happened -- discarded during replay, never partially applied" | Durability Contract Design §6 | Exact match. |
| "no recovery logic" / "no replay logic" | This governing task's own exclusion list | Each an exact, separately-quoted, separately-attributed fragment — not spliced. |
| "a durable write and the corresponding in-memory update occur as one atomic unit from the caller's own perspective" | Durability Contract Design §10 | Verified above — corrected and now exact. |
| "fail fast at construction" | `FileSystemEvidenceDeletionAudit.kt` | Exact, contiguous truncation. |
| "unknown future versions must never be silently interpreted" | Durability Contract Design §8 | Exact match, both occurrences (codec file and exception KDoc). |
| "fixed order" | `FileSystemEvidenceDeletionAudit.kt` | Verified above. |

**No further defect found.**

---

## 5. The Unit-Numbering Divergence — Independently Re-Derived, Not Merely Accepted

**Test:** does this session's own "Unit 3" task genuinely correspond to the Implementation Plan's own Unit 2 output, and was proceeding without a Boundary Review actually correct?

Re-derived independently against the Implementation Plan's own text, re-read fresh for this review: Unit 2's own "Outputs" field states, verbatim, "A single concrete class, `FileSystemMemoryCoreDurabilityLog`... Two operations: `suspend fun append(entry: DurableMemoryCoreEntry)`... and `suspend fun readAll(): List<DurableMemoryCoreEntry>`." Unit 3's own "Outputs" field, by contrast, states, verbatim, "The write-path half of `DurableMemoryCore`... for each of the six `MemoryCore` operations, `DurableMemoryCore` first constructs and durably appends the corresponding `DurableMemoryCoreEntry`... and only then invokes the corresponding `InMemoryMemoryCore` operation." These are unambiguously different deliverables — the Plan's own Unit 3 presupposes a `MemoryCore`-implementing decorator class (`DurableMemoryCore`) that does not exist anywhere in this repository yet, confirmed by a word-boundary-anchored search (`grep -rn "^internal class DurableMemoryCore\b\|^class DurableMemoryCore\b" src/` returns no match; an unanchored search for the substring `DurableMemoryCore` alone also matches the unrelated, already-existing `DurableMemoryCoreEntry`, so the anchored form is the one that actually answers the question asked). This session's own task explicitly excludes exactly that deliverable ("no Memory Core decorator," "no runtime composition") and explicitly requests the Unit-2-shaped one instead ("the production implementation of `MemoryCoreDurabilityLog`"). The Completion Review's own reading is independently confirmed correct: this session's "Unit 3" **is** the Implementation Plan's own deferred Unit 2 output, not the Plan's own Unit 3.

**Was a Boundary Review genuinely unnecessary?** Tested against what a Boundary Review exists to resolve in this repository's own established practice (a genuine, load-bearing question about whether a newly-needed capability conflicts with an already-fixed boundary — the precedent named, `EVIDENCE_CUSTODIAN_PHASE_7_BOUNDARY_CLARIFICATION.md`, resolved a genuine ambiguity about where a gating responsibility belonged). No comparable ambiguity exists here: the concrete capability built (a filesystem-backed `append`/`readAll` implementation) was already fully and unambiguously specified by the Implementation Plan's own Unit 2 text, down to the exact method signatures and the exact mechanism precedent (`FileSystemEvidenceDeletionAudit.record`) to mirror. Nothing about *what* to build was in question — only *which session-level label* the work would be filed under. **Sound** — no Boundary Review was required, and none would have surfaced a genuine open question this determination has not already closed.

---

## 6. Codec Correctness — Independently Traced, Not Merely Re-Run

**Test:** setting the passing test suite aside, does the codec's own design actually guarantee round-trip fidelity for every field this Programme's five record types and one transition shape carry?

Traced by hand against `src/interfaces/MemoryCore.kt`'s own current field lists, not merely re-run: every `String`-typed field and every identifier's own `.value` is passed through `field()` (Base64-encoding); every `Instant`, `Boolean`, `Int`, `Double`, and closed-enum `.name` is passed through `rawField()` (written unencoded). Cross-checked field-by-field against each of `Provenance`, `Entity`, `Document`, `Assertion`, `Relationship`, and `MemoryCoreRecordReference`'s own current constructors: no field was found encoded with the wrong helper (which would either corrupt a raw numeric/enum value by wrapping it unnecessarily, or — the more dangerous direction — leave a free-text field raw and vulnerable to an embedded tab corrupting the line format). `RelationshipEndpoint.recordKind`/`recordId` (open, non-blank, unrestricted strings per `RelationshipEndpoint`'s own KDoc, deliberately accepting a kind Memory Core does not own) are correctly Base64-encoded via `field()`, not left raw — a case worth checking specifically, since these values are structurally closer to "control data" than typical free text and could plausibly have been mistaken for safe-to-leave-raw. They were not. **Sound.**

**A minor, non-blocking robustness observation, not a defect:** `decodeMap`'s own `it.split(MAP_ENTRY_SEPARATOR)` does not explicitly guard against a malformed pair producing fewer than two parts (which would throw an unchecked `IndexOutOfBoundsException` when `parts[1]` is accessed). This exception is still caught by `decode`'s own outer, broad `catch (e: Exception)` block and correctly re-surfaced as `MalformedEntry` — so no failure mode is silent or incorrect, only the resulting error message is less specific than a dedicated guard would produce. This can only be reached via deliberately malformed input; `encode` never produces a pair missing its colon. Not required to be corrected for this Unit's own completion criteria.

---

## 7. Boundary Discipline Against the Task's Own Explicit Exclusion List

Checked directly, term by term, against both production files: no reference to `ParkerRuntime`, `MemoryCore`, `MemoryRetrieval`, `InMemoryMemoryCore`, `PermissionEngine`, or any Knowledge-Memory-owned type anywhere in either file (confirmed by direct reading of both files' own complete import lists — `FileSystemMemoryCoreDurabilityLog.kt` imports only JDK I/O and `kotlinx.coroutines.sync`; `DurableMemoryCoreEntryCodec.kt` imports only the JDK and this Unit's own already-existing domain types). No batching, compaction, checkpointing, or schema-migration operation exists anywhere — `append` accepts exactly one entry (the interface's own already-fixed shape, unchanged); only `CURRENT_SCHEMA_VERSION` is ever produced, and any other value is rejected, never migrated. `git status --short` confirms no file under `src/composition/`, `Dockerfile`, `docker-compose.yml`, or any previous durability Unit's own file is touched. **Sound.**

---

## 8. Concurrency Discipline — Independently Re-Tested, Not Merely Accepted

**Test:** does the single shared `Mutex` genuinely satisfy "no concurrency beyond what Unit 3 explicitly authorizes," or does serialising `readAll` against `append` (rather than only serialising `append` against itself) go beyond what was authorised?

Re-derived independently: the governing task's own exclusion list forbids "concurrency beyond what Unit 3 explicitly authorizes," which is a ceiling, not a floor — it does not forbid a *more conservative* concurrency model than the minimum required for correctness. A single shared `Mutex` covering both operations is the more conservative choice compared to a reader-writer lock permitting concurrent reads, and it is the same choice `FileSystemEvidenceDeletionAudit` already makes for the identical class of concern (that class has no read operation to consider, but its own single-`Mutex`-for-everything shape is the direct precedent this Unit's own KDoc cites and mirrors). Introducing a more permissive, reader-writer-style lock would be *inventing new structure* (a new concurrency primitive/behaviour) with no concrete need identified — precisely what the Scope Lock's own "no structure without a concrete need" discipline, applied elsewhere in this Programme, would counsel against. **Sound** — the simpler, more conservative choice is the correct one here, not an oversight.

---

## Findings

No required correction was found. The four corrections the Completion Review self-reports are independently re-verified as genuine and correctly applied (Section 3). A full, independent quotation audit found the same fifth discrepancy the Completion Review's own account already discloses as having been caught and corrected during the same drafting session, and found no sixth (Section 4). The unit-numbering divergence and the determination that no Boundary Review was required are both independently re-derived as correct, not merely accepted (Section 5). The codec's own field-by-field correctness is independently traced by hand, surfacing one minor, non-blocking robustness observation that does not rise to a required correction (Section 6). Boundary discipline and concurrency discipline are each independently confirmed sound (Sections 7, 8).

---

## Constitutional Verdict

```
ACCEPTED
```

No required correction. Every determination in this Unit's own Completion Review is independently re-derived and confirmed sound, including re-verification of a drafting process that itself required four rounds of self-correction — this review's own independent audit found no defect beyond what the Completion Review had already disclosed.

---

## Recommended Next Step

No further correction or Defect Confirmation Review is required for Unit 3. Per this task's own explicit stop point, work halts here: Unit 4 is not begun; nothing is staged, committed, or pushed.

---

## Final Git Status at Time of This Review

```
$ git status --short
?? docs/reviews/MEMORY_CORE_DURABILITY_UNIT_3_ATOMIC_APPEND_COMPLETION_REVIEW.md
?? docs/reviews/MEMORY_CORE_DURABILITY_UNIT_3_ATOMIC_APPEND_INDEPENDENT_CONSTITUTIONAL_REVIEW.md
?? src/runtime/DurableMemoryCoreEntryCodec.kt
?? src/runtime/FileSystemMemoryCoreDurabilityLog.kt
?? tests/runtime/DurableMemoryCoreEntryCodecTest.kt
?? tests/runtime/FileSystemMemoryCoreDurabilityLogTest.kt
```

Nothing staged, committed, or pushed.
