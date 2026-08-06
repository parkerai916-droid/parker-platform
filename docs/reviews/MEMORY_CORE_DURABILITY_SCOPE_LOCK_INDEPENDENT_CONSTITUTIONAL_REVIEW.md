# Memory Core Durability Scope Lock — Independent Constitutional Review

## Status

**Genuine Independent Constitutional Review**, performed as if by another reviewer, against the four frozen governing documents re-read fresh, and against the actual, current file contents — not against the drafting session's own summary of them. This document does not amend `docs/architecture/MEMORY_CORE_DURABILITY_SCOPE_LOCK.md` (the Scope Lock), the Durability Contract Design, the Version 1 Contract Design, the Version 1 Scope Lock, ADR-024, or any other governance document. It identifies conflict, or its absence, and states a determination. Nothing is staged, committed, or pushed.

---

## 1. Baseline Confirmation

`HEAD` is `15e97791e4081ff8bc4e1b571a888d6e8f322c08`, unchanged since this task cycle began. The working tree carries exactly the expected set: two new, untracked governance documents (the Defect Confirmation Review for the Contract Design, and the Scope Lock itself). No `src/`, no `tests/`, no Docker file, and no `ParkerRuntime.kt` is touched.

---

## 2. Scope and Method

This review re-reads `docs/architecture/MEMORY_CORE_DURABILITY_CONTRACT_DESIGN.md` (the Contract Design), `docs/architecture/MEMORY_CORE_SCOPE_LOCK.md` (the Version 1 Scope Lock), and `docs/adr/ADR-024-module-event-audit-durability-boundary.md` fresh, and checks every section of the new Scope Lock against each, section by section. Every sentence in the new Scope Lock presented as a restatement of an already-frozen rule ("Frozen from Durability Contract Design §N") is checked for substantive fidelity against the cited section's actual current text — not merely for the presence of a citation. Every concrete factual claim about the current repository (in particular, claims about `src/composition/ParkerRuntime.kt`'s own composition graph) is independently re-verified by reading the cited file directly, not accepted from the drafting session's own prior grep results. This is the same discipline that caught a genuine defect in each of the four immediately preceding Independent Constitutional Reviews this session has performed.

---

## 3. Governance-Vehicle and Sequencing Review

**Test:** was a Scope Lock the correct next document, and was it correctly triggered?

Independently re-derived: the Contract Design's own Final Recommendation states, verbatim, "The next governance stage — a Memory Core Durability Scope Lock — is authorised to begin only after independent constitutional review of this document." The Contract Design's own Independent Constitutional Review states, verbatim, in its §16: "Only after that confirmation should the document's own Status be changed from Draft to Accepted, and only then is a Memory Core Durability Scope Lock authorised to begin." Both conditions are satisfied: the Independent Review happened, found two Required Corrections, both are verified present in the current text, and the Defect Confirmation Review (`docs/reviews/MEMORY_CORE_DURABILITY_CONTRACT_DESIGN_DEFECT_CONFIRMATION_REVIEW.md`) documents that verification directly, word-for-word, against the current committed text — not merely by re-trusting an earlier grep. **Sound** — the Scope Lock was correctly triggered, on genuine confirmation, not on an assumption that "accepted and committed" already meant procedurally closed.

---

## 4. Structural Fidelity to the Version 1 Scope Lock Precedent

**Test:** does the new document's own structure genuinely mirror the established Scope Lock convention, or does it merely borrow section titles without the same substantive discipline?

Checked directly against `docs/architecture/MEMORY_CORE_SCOPE_LOCK.md`'s own Status block, Scope Lock Principle, and IN SCOPE/OUT OF SCOPE binary: the new document's own Status section states the same "binding," "frozen, normative inputs," and burden-of-proof-favours-exclusion framing, adapted to name the correct four inputs for this narrower subject (ADR-024, the Version 1 Contract Design as amended, the Version 1 Scope Lock, and the Durability Contract Design) rather than merely copying the Version 1 Scope Lock's own three. **Sound.**

---

## 5. Traceability Review — Sections 2, 5, 7 Through 14

**Test:** does every "Frozen from Durability Contract Design §N" restatement accurately reflect the cited section's substance, or does any restatement drift, strengthen, or weaken what was actually fixed?

Checked line-by-line against the Contract Design's current text (re-read in full for this review): Section 5 (Durable Record Scope) accurately restates §3; Section 7 (Atomicity) accurately restates §4, including the correct, non-strengthened treatment of `EvidenceRegistrationOutcome`'s existing intermediate-outcome vehicle; Section 8 (Recovery) accurately restates §6, including the corrected, mechanism-neutral wording for repeated-write and partial-write handling (matching the Contract Design's own post-correction text, not the pre-correction wording the Independent Constitutional Review had originally flagged); Section 9 (Corruption and Lifecycle) accurately restates §7; Section 10 (Versioning) accurately restates §8; Section 11 (Immutability and Transitions) accurately restates §9, including the corrected "any future storage-efficiency mechanism" wording rather than the original "compaction" language; Section 12 (Concurrency and Ordering) accurately restates §10, including the corrected purely-behavioural serialisation wording rather than the original `Mutex`-naming sentence; Section 13 (Failure Semantics) accurately restates §11; the first three paragraphs of Section 14 (Runtime Boundary) accurately restate §12. **Sound in all nine instances** — no restatement invents, strengthens, or silently narrows anything the Contract Design did not already fix, and each correctly reflects the *corrected* text, not an earlier draft.

---

## 6. Mechanism Neutrality Review

**Test:** does the new Scope Lock itself introduce any mechanism-specific commitment the Contract Design's own corrected text was careful to avoid?

Checked directly: Section 6 (Durability Mechanism Boundary) explicitly declines to select between the append-only-replay direction and SQLite, restates the existing-dependency-justification bar, and states the boundary purely in terms of properties a mechanism must satisfy. No occurrence of "duplicate entry," "tail," "compaction," or `Mutex` appears anywhere in the new document (confirmed by direct search). Section 4's Explicit Exclusions table correctly defers "Selection of a specific storage technology, file format, serializer, or Kotlin class" and "Any SQLite adoption... in advance of Implementation Plan-level evidence." **Sound** — the new document does not reopen or narrow the mechanism neutrality the Contract Design's own corrected text established.

---

## 7. Finding — Section 14 and Section 18 Overreach Into Implementation-Plan-Tier Composition Wiring

**Test:** does any part of the new Scope Lock fix a requirement at a tier the Contract Design itself, or the Version 1 Scope Lock's own precedent, reserves for the Implementation Plan?

This is the question worth pressing hardest, since it is exactly the kind of tier violation this repository's own governance discipline treats as a genuine defect rather than a stylistic concern.

**The defect.** Section 14's third paragraph, headed "Composition discipline, confirmed against the actual current composition graph, not assumed," states a set of *concrete, current* facts about `src/composition/ParkerRuntime.kt` — that `InMemoryMemoryCore()` is constructed at exactly one site, bound to local variables named `inMemoryMemoryCore` and `memoryCore`, exposed raw to `EvidenceRegistrationCoordinator` and a named Programme 4 coordinator, and wrapped by exactly one `PermissionFilteredMemoryRetrieval` — and then fixes a binding **SHALL** ("A Memory Core durability implementation **shall** preserve this exact pattern... Constructing the durable implementation more than once, anywhere in the composition graph, is a defect regardless of mechanism"). Section 18 (Acceptance Criteria) restates the same binding requirement as a formal SHALL: "A Memory Core durability implementation **SHALL** be constructed exactly once within `ParkerRuntime.kt`'s own composition graph, and **SHALL** preserve the existing decorator pattern..."

Checked directly against the Contract Design's own §13 Explicit Exclusions table: "Runtime composition, `ParkerRuntime.kt` wiring | Implementation-Plan-tier work, not Contract-Design-tier; this document fixes requirements, not wiring." Checked directly against the Version 1 Scope Lock's own precedent, Section 15 (Out-of-Scope Register): "`EventBus`/`PermissionEngine`/`IdentityService` composition-ordering in `ParkerRuntime.kt` | The Implementation Plan (Contract Design §19, Open Question 3)." Both of the two frozen documents this new Scope Lock names as its own governing inputs place concrete composition-graph wiring facts at Implementation-Plan tier, one level below Scope Lock, not at Scope Lock tier itself — and the Version 1 Scope Lock, which this new document explicitly holds up as its own structural precedent, has already, itself, deferred exactly this category of fact (which local variable holds which dependency, which decorator wraps which instance) rather than freezing it as binding scope text.

**Why this is a genuine defect, not pedantry.** The Durability Contract Design §12 already fixes the correct Scope-Lock-tier requirement in the abstract, property-level form the tier calls for: no new dependency on `PermissionEngine`/Knowledge Memory/Evidence Custodian is introduced into the storage layer; durability is invisible to every caller through the public interface. That abstract property is exactly what belongs, restated as binding, at Scope Lock tier — and the new document's own first three paragraphs of Section 14 already state it correctly. The *fourth* paragraph goes further: it freezes a snapshot of *today's specific composition wiring* (which variable names, which single site, which decorator) as though it were itself a frozen requirement, when it is in fact a factual finding about the current state of a file this task was explicitly instructed not to modify or treat as fixed. A future, unrelated refactor of `ParkerRuntime.kt`'s own composition graph — for a reason having nothing to do with durability — could change variable names or introduce a second, legitimate `PermissionFilteredMemoryRetrieval` instance for an unrelated new consumer, and the letter of this Scope Lock's own Section 14/18 text would then read as though it forbade a change no durability concern actually forbids. That is the concrete failure mode a Scope Lock's own tier discipline exists to prevent: freezing an implementation-tier fact under Scope-Lock-tier authority, requiring a Scope Lock revision to unfreeze something that was never a durability requirement in the first place.

**What is not wrong.** The underlying *substance* — durability must not introduce double permission gating, must be constructed exactly once, and must not bypass whichever decorator boundary the Implementation Plan's own composition work establishes — is correct and Contract-Design-traceable (§12's own "no `PermissionEngine`... dependency... in the storage layer" already implies "not gated twice"). The defect is narrower than "this content is wrong"; it is "this content is pitched at the wrong tier, using today's specific composition facts as though they were themselves frozen scope," exactly mirroring the class of defect the Contract Design's own Independent Constitutional Review found in its §6/§9/§11 (mechanism-specific vocabulary presented as a general rule) — here, implementation-specific composition facts presented as general, binding scope.

**Required correction:** restate Section 14's fourth paragraph and Section 18's corresponding bullet in purely behavioural, property-level terms — "constructed exactly once; no double permission gating; no new dependency introduced beyond what §12 already excludes" — without naming today's specific variable names, construction line, or which named coordinators currently hold a raw reference. The concrete, current-state finding about `ParkerRuntime.kt`'s own actual composition graph is genuinely useful evidence for a future Implementation Plan to consult, but belongs in a disclosed, explicitly-informational note (or the Risks section, where a residual-risk framing already exists for exactly this kind of "this document can fix requirements but not verify an implementation that does not yet exist" situation) — never inside the Acceptance Criteria section itself, and never phrased as something "this exact pattern... shall be preserved."

No other required correction was found.

---

## 8. Explicit Exclusions and Out-of-Scope Register Review

**Test:** does Section 4's exclusion table and Section 16's deferral register correctly, completely mirror the Contract Design's own §13 table, without silently dropping or adding an item?

Checked item-by-item against the Contract Design §13: Knowledge Memory durability, legacy `KnowledgeRecord` reconciliation, Identity Service durability, World Model durability (correctly marked permanent, not deferred), Conversation History durability, and the constitutional Audit log are each present in both the new document's Section 4 and Section 16, with the same reasoning the Contract Design itself gives. Three items in Section 4 (backup/replication policy, storage optimisation, cross-process access) do not appear verbatim in the Contract Design's own §13 table, but are traced instead to the Contract Design's own §14 ("explicitly rejected as unnecessary at this stage") and §10 (cross-process disclaimer) — checked directly, both citations are accurate to their sections. **Sound** — no exclusion is invented without a traceable source, and none of the Contract Design's own excluded items is silently dropped.

---

## 9. Verification Scope Review

**Test:** does Section 15's required minimum test list match the Contract Design's own §15 Verification Requirements, or does it add a requirement the Contract Design never fixed?

Checked directly: every property named in the Contract Design's own §15 (single-record atomicity, deterministic ordered replay, referential-integrity enforcement, counter correctness, partial-write tolerance/corruption intolerance, no silent empty-store fallback, version-tag presence, immutability/history preservation, no public-contract change, no dependency reachability) has a corresponding item in the new Section 15. Two items in the new Section 15 — "A failed recovery leaves every write... unreachable" and the Docker-volume-backed restart requirement — do not appear in the Contract Design's own §15 list by that exact wording, but are traceable respectively to the Contract Design's own §7 (no partial-repair, no silent-empty-store) and to the user's own original task instruction (required verification unit 10, named directly in the governing task, not invented by the drafting session). Since the user's own task instruction is itself a governing input to this entire work cycle, this is not an invention beyond authorised scope. **Sound**, with the observation that the Docker item is phrased behaviourally (no volume name, mount path, or concrete Docker configuration is fixed), correctly avoiding the same tier violation Finding 1 (Section 7, above) identifies elsewhere.

---

## 10. Acceptance Criteria Review

**Test:** does every SHALL/SHALL NOT statement in Section 18 trace to an already-fixed requirement, with the one exception already found in Section 7, above?

Checked one-by-one against Sections 2 through 14's own already-verified traceability: fourteen of the fifteen Acceptance Criteria bullets restate an already-traced requirement without addition. The fifteenth (composition-graph-specific) is the same defect already found and consolidated in Finding 1, above — not a second, independent defect.

---

## Findings

### Finding 1 (Required Correction) — Concrete composition-graph facts frozen as Scope-Lock-tier binding text, in two locations

See Section 7, above, for the full analysis. **Locations:** Section 14 ("Composition discipline, confirmed against the actual current composition graph, not assumed" paragraph) and Section 18 (the "SHALL be constructed exactly once within `ParkerRuntime.kt`'s own composition graph, and SHALL preserve the existing decorator pattern..." bullet). One defect, two textual occurrences of it, exactly mirroring how the Contract Design's own Independent Constitutional Review consolidated four occurrences of one mechanism-neutrality defect into a single required correction.

**Required correction:** restate both locations in purely behavioural, property-level terms (constructed exactly once; no double permission gating; no dependency reachability beyond what §12/Section 14's own first three paragraphs already exclude), and move today's specific, disclosed factual finding about `ParkerRuntime.kt`'s own current composition graph into a clearly-labelled informational note or the Risks section, where it can genuinely help a future Implementation Plan without being frozen as this Scope Lock's own binding requirement.

No other required correction was found. The governance-vehicle and sequencing determination, the structural fidelity to the Version 1 Scope Lock precedent, the traceability of Sections 2/5/7–13 and the first three paragraphs of Section 14, mechanism neutrality, the Explicit Exclusions/Out-of-Scope Register content, and the Verification Scope are all confirmed sound.

---

## Constitutional Verdict

```
REQUIRES REVISION
```

One required correction — a tier-boundary defect confined to one paragraph and one Acceptance Criteria bullet, both addressing the same underlying issue (concrete, current-state composition-wiring facts frozen as Scope-Lock-tier binding text, when both the Contract Design's own §13 and the Version 1 Scope Lock's own precedent reserve that category of fact for the Implementation Plan). Every other section's traceability, mechanism neutrality, exclusion completeness, and acceptance-criteria substance is confirmed sound and requires no change.

---

## Recommended Next Step

Correct Section 14's fourth paragraph and Section 18's corresponding bullet in `docs/architecture/MEMORY_CORE_DURABILITY_SCOPE_LOCK.md`; touch nothing else. A narrow Defect Confirmation Review follows, confirming the correction was applied precisely and that no other section was altered, without repeating this full review.

---

## Final Git Status at Time of This Review

```
$ git status --short
?? docs/architecture/MEMORY_CORE_DURABILITY_SCOPE_LOCK.md
?? docs/reviews/MEMORY_CORE_DURABILITY_CONTRACT_DESIGN_DEFECT_CONFIRMATION_REVIEW.md
?? docs/reviews/MEMORY_CORE_DURABILITY_SCOPE_LOCK_INDEPENDENT_CONSTITUTIONAL_REVIEW.md
```

Nothing staged, committed, or pushed.
