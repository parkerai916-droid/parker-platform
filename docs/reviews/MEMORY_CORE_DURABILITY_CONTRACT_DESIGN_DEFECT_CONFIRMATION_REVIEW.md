# Memory Core Durability Contract Design — Defect Confirmation Review

## Status

**Defect confirmation review only — not a new Independent Constitutional Review.** This review does not re-examine the governance-vehicle determination, the ADR-024 authority boundary, the durable-scope list, the atomicity rule, the recovery/corruption/versioning/immutability/concurrency requirements, the runtime boundary, or the exclusion table — `docs/reviews/MEMORY_CORE_DURABILITY_CONTRACT_DESIGN_INDEPENDENT_CONSTITUTIONAL_REVIEW.md` ("the Independent Review") already examined each of those and found them sound. It confirms only that the Independent Review's two Required Corrections (§14) are genuinely present in the current committed text of `docs/architecture/MEMORY_CORE_DURABILITY_CONTRACT_DESIGN.md` ("the Contract Design"), and it resolves one procedural gap the Independent Review's own §16 left open: the Contract Design's own bottom status line. The Contract Design is **not edited by this review** — this task's own governing instruction ("do not modify existing governance") forbids it, and this document works within that constraint rather than around it. Nothing is staged, committed, or pushed.

---

## Repository Baseline

- **HEAD:** `15e97791e4081ff8bc4e1b571a888d6e8f322c08`
- **Branch:** `main`
- **Working tree:** clean at the start of this review.

---

## What Is Being Confirmed, and Why This Confirmation Looks Different From Prior Ones

Every prior Defect Confirmation Review in this session's own history (Units 9.4, 9.5, 9.6) confirmed a correction applied as a distinguishable, separately-committed diff against a defect an Independent Review had found in already-committed text. This case differs, and the difference is disclosed rather than smoothed over: `git log --oneline --all -- docs/architecture/MEMORY_CORE_DURABILITY_CONTRACT_DESIGN.md` returns exactly one commit, `fabb212`, and `git show --stat fabb212` confirms the Contract Design (234 lines) and the Independent Review (191 lines) were added together, in that single commit. There is no separate "before correction" commit of the Contract Design to diff against — the text now in the repository is the only text that has ever existed in it, and it already carries both required corrections.

This means verification here cannot be a `git diff` review of a correction patch. It is instead a direct, word-for-word check of the current committed text against each of the Independent Review's two Required Corrections (§14), performed by reading both documents in full — not by re-trusting a prior grep or a prior session's own summary of one.

---

## Required Correction 1 — Explicit Version 1 Relationship Statement

**What the Independent Review required (§14.1):** "Add an explicit, standalone statement of this document's relationship to Memory Core Version 1 — its own subsection, not folded into the vehicle-selection reasoning or the Final Recommendation. It must state plainly that Version 1 remains, unmodified, in-memory and non-persistent exactly as currently accepted; that this document does not discharge, satisfy, or amend Scope Lock §14's own `SHALL`; and that durability becomes binding only once a future, separate governance stage (naming which of "a later Memory Core version," "a layered capability beneath the existing contract," or "a separately governed durability programme" applies, rather than leaving the choice implicit) is itself accepted."

**Verified present.** The Contract Design carries a standalone section, `## Relationship to Memory Core Version 1` (lines 30–37), positioned between "Context and Constitutional Basis" and "1. Purpose" — genuinely its own subsection, not folded into the vehicle-selection reasoning (Status section) or the Final Recommendation, exactly as required. Checked clause by clause:

- **"Version 1 remains, unmodified, in-memory and non-persistent exactly as currently accepted"** — present: "Memory Core Version 1 is unchanged by this document, in every respect. Memory Core Version 1 remains an in-memory, non-persistent implementation, exactly as currently accepted and exactly as `InMemoryMemoryCore.kt` currently implements it."
- **"Does not discharge, satisfy, or amend Scope Lock §14's own `SHALL`"** — present, using the identical three verbs the correction specified: "This document does not supersede, discharge, or begin to satisfy any Version 1 `SHALL`" and, in the paragraph above it, "Scope Lock §14's own `SHALL`... remains fully authoritative, unamended, and undischarged by anything in this document."
- **Naming which framing applies, rather than leaving the choice implicit** — present: the section names the future work as "a future durability capability layered beneath Memory Core's own already-frozen `MemoryCore`/`MemoryRetrieval` contracts," which selects the second of the Independent Review's three named framings ("a layered capability beneath the existing contract") explicitly, not by default.
- **"Durability becomes binding only once a future, separate governance stage is itself accepted"** — present: "a capability that does not yet exist, is not authorised to be built by this document, and remains subject to a later, separate governance stage (a Memory Core Durability Scope Lock, named in the Final Recommendation, below) and a later, separate Implementation Plan before any Kotlin implementing it may be written."

**Correction 1 is confirmed correctly and completely applied.**

---

## Required Correction 2 — Mechanism-Neutral Vocabulary

**What the Independent Review required (§14.2):** replace "duplicate-entry handling"/"interrupted-tail handling" (§6) with idempotent-retry and never-partially-applied language; replace "compaction" (§9) with "any future storage-efficiency mechanism"; replace the `Mutex`-naming sentence (§10) with a purely behavioural statement.

**Verified present, checked against each of the four cited instances individually, by reading the current text of §6, §9, and §10 directly (not by grep alone):**

1. **§6, repeated-record handling.** Current text: "A creation fact that was durably committed more than once — the expected consequence of an at-least-once durable-write discipline, however the durable mechanism achieves it — must be recognised and idempotently skipped during replay, never treated as a second, conflicting record." No occurrence of "duplicate entry" remains; the replacement closely tracks the Independent Review's own suggested formulation ("a write durably committed more than once due to a retry must be idempotently recognised, never treated as two conflicting records").
2. **§6, partial-write handling.** Current text: "A write that did not complete before an interruption is treated as a write that never happened — discarded during replay, never partially applied." No occurrence of "interrupted tail" or "tail" remains; the replacement closely tracks the Independent Review's own suggested formulation ("a write that did not complete before an interruption must never be partially applied").
3. **§9, storage-efficiency mechanism.** Current text: "No storage-efficiency mechanism may discard constitutionally relevant history. Should a future mechanism ever introduce any representation optimisation for storage efficiency, it may only ever preserve every creation fact and every transition in some recoverable form..." No occurrence of "compaction" remains; "any future storage-efficiency mechanism" / "any representation optimisation for storage efficiency" is used exactly as directed.
4. **§10, serialisation guarantee.** Current text: "`InMemoryMemoryCore`'s own existing single-writer-at-a-time discipline, guarding every store and counter under one serialisation point, remains the correct behavioural model — whatever mechanism provides it." No occurrence of `Mutex` remains in this sentence; the requirement is stated purely behaviourally, exactly as directed.

Confirmed independently by direct search: `grep -n "duplicate entry\|interrupted.tail\|\btail\b\|compaction\|Mutex" docs/architecture/MEMORY_CORE_DURABILITY_CONTRACT_DESIGN.md` returns no match (exit code 1). The only remaining occurrence of `Mutex` anywhere in the Contract Design's text is unrelated — none exists; the word appears only in the Independent Review's own document, which correctly documents the defect historically and is not itself subject to this correction.

**Correction 2 is confirmed correctly and completely applied, across all four cited instances.**

---

## The Three Minor Findings (§13, rows 3–5) — Correctly Left Unaddressed

The Independent Review's own §14 states only two corrections are *required*; the three minor findings (process-crash-vs-cancellation distinction in §11; "constitutionally relevant history" left undefined in §9; the identifier-counter requirement framed around "counters" rather than the more abstract "however minted") are named explicitly as "recommended refinements, not required corrections — the document remains internally sound and unambiguous without them." Checked directly: none of the three was applied. This is correct, not an omission requiring further correction — applying an unrequired refinement would not itself be a defect, but *failing* to apply an optional one is equally not a defect. No action is required or taken on these three findings by this review.

---

## Consistency Check — Nothing Else Was Alt­ered Inconsistently

Since no pre-correction version of the Contract Design exists to diff against, this check instead confirms the current text is internally consistent, not merely correct at the two required points:

- The embedded self-review section near the bottom of the Contract Design, `## Independent Constitutional Review (Performed Before Completion)`, is the *drafting session's own* self-check, textually and logically distinct from, and predating, the genuine standalone Independent Review document. It is not contradicted by either required correction — its own checklist ("Does it preserve the existing `MemoryCore`/`MemoryRetrieval` contracts unchanged?", etc.) remains accurate against the corrected text, and it makes no claim about Version 1's relationship or mechanism neutrality that the two corrections now falsify.
- Every other section of the Contract Design (§1–§5, §7, §8, §11–§15, the Final Recommendation) was re-read in full as part of this review and found unchanged from what the Independent Review itself already examined and found sound (§4, §7, §10, §12 of the Independent Review) — no correction touched any of them, and none needed to.
- The Contract Design's own §5 and §2 both promise mechanism neutrality; Correction 2 brings §6, §9, and §10 into conformance with that promise rather than introducing a new one — an internal consistency the corrected text now satisfies that the original text did not.

**No inconsistency found.**

---

## The One Remaining Procedural Gap — The Stale Status Line, Resolved Narratively

The Contract Design's own closing line (immediately above the "Independent Constitutional Review (Performed Before Completion)" section) still reads: `MEMORY CORE DURABILITY CONTRACT DESIGN — DRAFT — AWAITING INDEPENDENT CONSTITUTIONAL REVIEW`. This is stale: the independent constitutional review it names has already happened, found two required corrections, and both are now confirmed applied. The Independent Review's own §16 states the correct sequence directly: "Only after that confirmation should the document's own Status be changed from Draft to Accepted, and only then is a Memory Core Durability Scope Lock authorised to begin."

This review is that confirmation. It does not edit the Contract Design's own status line — this task's own instruction not to modify existing governance forbids it, and the file remains, byte-for-byte, whatever it already was. What this review establishes instead is the determination the status line itself would record if a future, separately-authorised housekeeping edit updated it: **the Contract Design's substantive content is confirmed accepted, as of this review, for the purpose of authorising the next governance stage.** A future editor authorised to touch `docs/architecture/` governance documents should update the closing line to `MEMORY CORE DURABILITY CONTRACT DESIGN — ACCEPTED` and cite this document as the confirming Defect Confirmation Review, mirroring how every other accepted Contract Design in this repository records its own acceptance. Until that housekeeping edit happens, the stale label is a known, disclosed inconsistency between the document's own closing line and its actual governance status — not a live ambiguity, since this review, the Independent Review, and the user's own explicit direction (authorising the full sequence: Defect Confirmation Review, then Scope Lock, then Implementation Plan) together establish the same determination through three independent, mutually consistent channels.

---

## Confirmations

- Both of the Independent Review's Required Corrections (§14.1, §14.2) are present, verified word-for-word against the current committed text of the Contract Design, not merely re-asserted from a prior grep or a prior summary.
- The three minor findings (§13, rows 3–5) were correctly left unaddressed, as the Independent Review itself authorised.
- No section of the Contract Design beyond the two required corrections was altered, and none needed to be — confirmed by full re-read against the Independent Review's own already-sound findings for every other section.
- The Contract Design itself was not modified by this review. Its own closing status line remains stale, disclosed above, pending a future, separately-authorised housekeeping edit.
- No new architectural, mechanism, or scope decision is made by this review. It confirms only that a previously-identified gap between two documents no longer exists.

---

## Recommended Next Step

The Contract Design is confirmed accepted in substance. Per the user's own explicit direction this task cycle ("Full sequence: Defect Confirmation Review, then Scope Lock, then Implementation Plan"), the Memory Core Durability Scope Lock — the next governance stage both the Contract Design's own Final Recommendation and the Independent Review's own §16 name as required before any Implementation Plan may begin — is now authorised to proceed.

---

## Final Git Status

```
$ git status --short
?? docs/reviews/MEMORY_CORE_DURABILITY_CONTRACT_DESIGN_DEFECT_CONFIRMATION_REVIEW.md
```

Nothing staged, committed, or pushed. The Contract Design and its Independent Review remain byte-for-byte unmodified.
