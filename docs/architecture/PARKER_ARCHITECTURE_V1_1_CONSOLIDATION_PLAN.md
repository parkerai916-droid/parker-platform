# Parker Architecture v1.1 Consolidation Plan

## Purpose

This document plans, but does not perform, the documentation work needed
to produce Architecture v1.1. It consolidates the findings of two prior
review documents into a single ordered work plan: which documents
change, why, how urgently, how large the change is, and in what order
they should be edited to avoid one edit invalidating another.

No architecture document, Architecture Decision, or Kotlin file is
modified by this plan. No new Architecture Decision is created here
(AD-017 remains a tracked, not-adopted candidate per the AD
Reconciliation). No implementation gap is opened or closed here. This
document only identifies what must change, categorised by necessity, and
in what order.

## Inputs

- **Architecture v1.0** — `docs/architecture/ARCHITECTURE_DECISIONS.md`
  (16 Accepted decisions, AD-001–AD-016), the Volume 3–6 specifications,
  and `docs/architecture/IMPLEMENTATION_GAPS.md` (40 recorded items).
- **Sprint 1 implementation** — commit `795544d`, tag `sprint1-complete`,
  234/234 Android Studio tests, `docs/implementation/IMPLEMENTATION_HISTORY.md`
  Units 4–11B.
- **Review Brief** — `docs/architecture/PARKER_ARCHITECTURE_V1_1_REVIEW_BRIEF.md`.
- **AD Reconciliation** — `docs/architecture/PARKER_ARCHITECTURE_V1_1_AD_RECONCILIATION.md`.

Every item below traces to a specific finding in one or both review
documents, cited by section name rather than restated in full.

## Documents Requiring Update

| Document | Reason | Priority | Estimated scope |
|---|---|---|---|
| `docs/architecture/IMPLEMENTATION_GAPS.md` | #32's Status line and its Phase 2 Gap Closure Summary table listing both still describe the pre-Unit-11A state (AD Reconciliation, "Implementation Gap Review — Closed gaps"; Review Brief, "Architecture Gaps Closed by Sprint 1") | **Required** | Small — one status line rewrite, one table-entry move (`#32` from "Deliberate scope boundaries" to "Resolved") |
| `docs/architecture/IMPLEMENTATION_GAPS.md` | No existing gap number covers `ToolInvocationBinding`/`ToolRegistry.resolve`'s convention-based (not construction-enforced) access restriction — confirmed distinct from #23, #24, and #32 by direct search (AD Reconciliation, "New proposed gap") | **Recommended** | Small — one new numbered entry, same format as existing items |
| `docs/architecture/ARCHITECTURE_DECISIONS.md` — AD-005 | Future Considerations still says "The Task Manager Runtime Specification does not yet define a Task Proposal intake operation or a disposition mechanism" — false as of Unit 6 (AD Reconciliation, AD-005) | **Required** | Small — one paragraph |
| `docs/architecture/ARCHITECTURE_DECISIONS.md` — AD-006 | Future Considerations still says "No named contract yet exists for the Task Manager Runtime to request an Agent Run's creation" — false since Blocker 3 / implemented by Units 6–7 (AD Reconciliation, AD-006) | **Required** | Small — one paragraph |
| `docs/architecture/ARCHITECTURE_DECISIONS.md` — AD-003 | Evidence/Affected specifications predate `ToolInvocationBinding`'s existence and do not cite it, even though it now implements part of the same rule (AD Reconciliation, AD-003) | **Recommended** | Small — one Evidence bullet, one Affected-specifications entry |
| `docs/architecture/ARCHITECTURE_DECISIONS.md` — AD-007 | Future Considerations correctly identifies gap #40 but does not reflect that its real-world consequence changed once Tool execution became real (AD Reconciliation, AD-007) | **Recommended** | Small — one clause added to an existing paragraph |
| `docs/architecture/ARCHITECTURE_DECISIONS.md` — AD-009 | Decision text conflates "an event was published" with "this is auditable"; Sprint 1's own `EventCollector` KDoc already draws a three-way publication/observation/audit-reconstruction distinction the AD text does not (AD Reconciliation, AD-009) | **Recommended** | Medium — clarifying paragraph, no change to the core Decision sentence |
| `docs/architecture/ARCHITECTURE_DECISIONS.md` — AD-013 | Does not name the case actually exercised twice (Blocker 3, Unit 11B): a specification's own prose anticipates a capability its formal field list omits (AD Reconciliation, AD-013) | **Recommended** | Small — one implementation-note paragraph |
| `docs/architecture/ARCHITECTURE_DECISIONS.md` — AD-015 | Evidence section has no citation for the three new Unit 11A `FAILED`-not-`DENIED` cases (unbound Tool, failed validation, failed execution) (AD Reconciliation, AD-015) | **Recommended** | Small — three Evidence bullets |
| `docs/architecture/ARCHITECTURE_DECISIONS.md` — AD-016 | Evidence section has no citation for the three new Sprint 1 lifecycle machines (`PlanningSessionLifecycleTransitions`, `TaskLifecycleTransitions`, `AgentRunLifecycleTransitions`) (AD Reconciliation, AD-016) | **Recommended** | Small — three Evidence bullets |
| `docs/specifications/volume-06-planner-runtime/PlannerRuntimeSpecification.md` §10 | "Proposal Model" field list has no Resource references field, even though §9 of the same document already anticipates one ("a Plan Candidate or Task Proposal may target") and `TaskProposal.kt` now implements it (Review Brief, "Clarifications Required for v1.1" #1; AD Reconciliation, AD-013) | **Recommended** | Small — one field-entry paragraph, same format as existing §10 entries |
| `docs/specifications/volume-05-task-manager-runtime/TaskManagerRuntimeSpecification.md` §9 | Task Context's own "Resource references" concept could be misread as already continuous with `TaskProposal.resourceReferences`; `Task.kt` carries no such field, so a clarifying note prevents that misreading (Review Brief, "Proposed v1.1 Documentation Updates" #5) | **Optional** | Small — one cross-reference sentence |
| New: `docs/specifications/volume-03-core-interfaces/ToolInvocationBinding.md` | No Volume 3 specification document exists for `ToolInvocationBinding` at all — confirmed by direct search; it exists only as a Kotlin contract with its own KDoc. AD-013's own "backfill an already-built, already-tested component" pattern (`ToolRegistry.md`'s precedent) applies directly (AD Reconciliation, AD-003 and "Documentation Updates Required" #12) | **Optional** | Medium — one new document, ~40–60 lines, mirroring `ToolRegistry.md`'s existing backfill structure |
| `docs/implementation/IMPLEMENTATION_HISTORY.md` | Already updated through Unit 11B (Repository Status, Unit 11A/11B sections, Current Vertical Slice diagram, Current Runtime Chain, Current Known Architecture Gaps) as of commit `795544d` | **No action required** | None — confirm only |
| `docs/specifications/volume-03-core-interfaces/EventType.md` | Already correctly frames wildcard/prefix subscription as an unresolved Open Question, not a stale "missing" claim (Review Brief research) | **Reviewed — no change required** | None |
| `docs/specifications/volume-03-core-interfaces/ExecutionPipeline.md` | Already lists "Dispatch tools" as a Responsibility; Unit 11A makes this literally true rather than contradicting it — no stale text found | **Reviewed — no change required** | None |
| `docs/architecture/tool-registry.md` / `docs/specifications/volume-03-core-interfaces/ToolRegistry.md` | Already uses honest, convention-based language ("intended to be called only by the Execution Pipeline") predating and consistent with the new gap recommendation above — no correction needed, only the new gap entry (above) documents the enforcement shortfall | **Reviewed — no change required** | None |

## Documentation Changes

For each document with a required or recommended change, specified as
clarification / strengthened statement / evidence added / stale text
removed. No prose is rewritten here — this is a change-type
classification only.

**`IMPLEMENTATION_GAPS.md`**
- Stale text removed: #32's `**Status:**` line (currently describes a
  pre-11A state) and its Gap Closure Summary table listing.
- Evidence added: cite Unit 11A / commit `13c9322` as the closing
  change.
- New guidance: one new gap entry for `ToolInvocationBinding`/`ToolRegistry.resolve`
  access enforcement, written in the file's existing per-item format
  (Status line, description, "Requires human decision" if applicable).

**`ARCHITECTURE_DECISIONS.md` — AD-003**
- Evidence added: `ToolInvocationBinding` citation (pending the new
  Volume 3 document, or citing the existing Kotlin contract directly if
  sequencing requires).
- No change to Decision, Reasoning, or Consequences text.

**`ARCHITECTURE_DECISIONS.md` — AD-005, AD-006**
- Stale text removed: the "no contract/mechanism yet exists" sentences
  in each Future Considerations section.
- Clarification added: both now note the relevant mechanism exists and
  is implemented, with its current scope boundary named explicitly
  (accept-only; START-only, respectively) so the update does not
  overstate completeness.

**`ARCHITECTURE_DECISIONS.md` — AD-007**
- Clarification added: one sentence noting gap #40's real-world
  consequence changed post-Unit-11A.
- No change to Decision, Reasoning, Evidence, or Consequences text.

**`ARCHITECTURE_DECISIONS.md` — AD-009**
- Clarification added: an explanatory paragraph distinguishing
  publication, observation, and audit reconstruction, citing
  `EventCollector`.
- Decision sentence itself is a candidate for a strengthened restatement
  (naming the three concepts explicitly) — sequencing and exact
  placement (new paragraph vs. amended sentence) is a decision for
  whoever performs the edit, not fixed by this plan.

**`ARCHITECTURE_DECISIONS.md` — AD-013**
- Evidence added: an implementation-note paragraph naming the
  "prose-anticipated, field-list-omitted" case, citing `AgentRunCommand`
  and `TaskProposal.resourceReferences`.
- No change to Decision, Reasoning, or Consequences text.

**`ARCHITECTURE_DECISIONS.md` — AD-015, AD-016**
- Evidence added only, in both cases. No change to Decision, Reasoning,
  Affected specifications, or Consequences text — both decisions are
  already correctly worded; Sprint 1 only supplies new proof points.

**`PlannerRuntimeSpecification.md` §10**
- Stale text removed: none (this is an omission, not an incorrect
  statement).
- New guidance added: one Resource references field entry, in the same
  `**(...)** ` provenance-tagging style already used by every other §10
  field.

**`TaskManagerRuntimeSpecification.md` §9**
- Clarification added: one sentence distinguishing Task Context's own
  Resource references concept from `TaskProposal.resourceReferences`.

**New `ToolInvocationBinding.md`**
- New guidance only (a new document). No existing text to classify as
  clarified/strengthened/stale.

## Architecture Changes

Each change below is categorised, with reason, evidence, affected
documents, implementation impact, and expected risk.

---

**Change 1 — Close `IMPLEMENTATION_GAPS.md` #32**
- Category: **Documentation only**.
- Reason: #32 describes a state (no concrete Tool implementation exists
  to invoke) Sprint 1 directly contradicts.
- Evidence: `src/runtime/DefaultExecutionPipeline.kt`
  (`executeResolvedTool`); `tests/runtime/DefaultExecutionPipelineTest.kt`
  Unit 11A tests; AD Reconciliation "Implementation Gap Review."
- Affected documents: `IMPLEMENTATION_GAPS.md` only.
- Implementation impact: none — documentation reflects already-shipped
  code.
- Expected risk: **Low**. Purely descriptive; no behaviour is implied to
  change.

---

**Change 2 — Open a new gap for `ToolInvocationBinding`/`ToolRegistry.resolve` access enforcement**
- Category: **Documentation only** (records a real, currently
  undocumented condition; does not itself change behaviour).
- Reason: the "Execution-Pipeline-only" restriction is real but
  unenforced by construction, and no existing gap number names this
  specific claim.
- Evidence: `InMemoryToolInvocationBinding`'s own KDoc ("enforced...by
  documentation only... no caller-identity check, no reduced
  visibility"); `SPRINT_1_VERTICAL_SLICE_PLAN.md` §7's own "true by
  construction... not merely by convention" acceptance bar; AD
  Reconciliation "New proposed gap."
- Affected documents: `IMPLEMENTATION_GAPS.md` only.
- Implementation impact: none directly — but this gap, once opened,
  becomes a legitimate candidate for Sprint 2 scoping.
- Expected risk: **Low**. Recording a known limitation does not change
  behaviour; the risk is entirely in *not* recording it (a future
  contributor assuming enforcement exists where it does not).

---

**Change 3 — Update AD-005 and AD-006 Future Considerations**
- Category: **Correction** (removing a now-false statement) +
  **Documentation only**.
- Reason: both currently assert a contract/mechanism does not exist that
  Sprint 1 implemented.
- Evidence: `src/runtime/InMemoryTaskManagerRuntime.kt`
  (`TaskProposalIntake` implementation); `src/contracts/AgentRunCommand.kt`
  and `src/runtime/InMemoryAgentRuntime.kt` (construction/consumption);
  AD Reconciliation AD-005/AD-006.
- Affected documents: `ARCHITECTURE_DECISIONS.md` only.
- Implementation impact: none.
- Expected risk: **Low**. These are factual corrections with direct code
  evidence; no interpretive judgment required.

---

**Change 4 — Add `ToolInvocationBinding` to AD-003's Evidence**
- Category: **Strengthening** (additional proof of an already-Accepted
  decision) + **Documentation only**.
- Reason: AD-003's Evidence predates `ToolInvocationBinding` and does
  not yet cite the component that now implements part of the same rule
  at the invocation step.
- Evidence: AD Reconciliation AD-003; direct confirmation that
  `DefaultExecutionPipeline.executeResolvedTool` is the only call site
  for `ToolInvocationBinding.invocableFor`/`Tool.validate`/`Tool.execute`
  anywhere in `src/`.
- Affected documents: `ARCHITECTURE_DECISIONS.md`; ideally sequenced
  after the new `ToolInvocationBinding.md` specification document
  exists, so the citation points to a specification rather than a
  Kotlin file directly (see Documentation Ordering).
- Implementation impact: none.
- Expected risk: **Low**.

---

**Change 5 — Amend AD-007 Future Considerations for gap #40's increased stakes**
- Category: **Clarification**.
- Reason: the gap itself (`PermissionEngine.evaluate` not resolving
  identity first) is unchanged, but its real-world consequence changed
  once Unit 11A made Tool execution real.
- Evidence: AD Reconciliation AD-007; `IMPLEMENTATION_GAPS.md` #40.
- Affected documents: `ARCHITECTURE_DECISIONS.md`.
- Implementation impact: none directly, but may influence Sprint 2
  prioritisation (a planning signal, not a behaviour change).
- Expected risk: **Low**.

---

**Change 6 — Clarify AD-009 (publication / observation / audit reconstruction)**
- Category: **Clarification** (the most interpretively significant
  change in this plan — closest to touching the Decision's own wording,
  though the underlying rule does not change).
- Reason: Sprint 1's own implementation (`EventCollector`) already draws
  a distinction AD-009's text does not.
- Evidence: `tests/runtime/EventCollector.kt`'s own KDoc; AD
  Reconciliation AD-009.
- Affected documents: `ARCHITECTURE_DECISIONS.md`.
- Implementation impact: none — this is a documentation-only
  clarification of an already-true state of the world.
- Expected risk: **Medium**. This is the one change in this plan closest
  to touching the Decision sentence itself rather than only its
  Evidence/Future-Considerations sections; whoever performs this edit
  should take care not to narrow or reinterpret the existing "every
  meaningful lifecycle transition emits an event" rule while adding the
  three-way distinction — per `ARCHITECTURE_DECISIONS.md` §5's own
  governance rule, a disagreement is resolved by revising the
  specification/decision text carefully, not by "weakening, narrowing,
  or quietly reinterpreting an already-accepted decision."

---

**Change 7 — Add an implementation note to AD-013**
- Category: **New guidance** (names a case the existing decision did not
  previously enumerate) + **Documentation only**.
- Reason: Sprint 1 validated AD-013's general rule but exercised a case
  (prose anticipates a capability its field list omits) the decision
  text does not currently name.
- Evidence: `src/contracts/AgentRunCommand.kt` (Blocker 3);
  `src/contracts/TaskProposal.kt` (Unit 11B); AD Reconciliation AD-013.
- Affected documents: `ARCHITECTURE_DECISIONS.md`.
- Implementation impact: none.
- Expected risk: **Low**.

---

**Change 8 — Add Evidence to AD-015 and AD-016**
- Category: **Strengthening** + **Documentation only**.
- Reason: both decisions are already correctly worded; Sprint 1 supplies
  new, later-pipeline-stage (AD-015) and new-lifecycle-machine (AD-016)
  proof points not yet cited.
- Evidence: `tests/runtime/DefaultExecutionPipelineTest.kt` (AD-015);
  `PlanningSessionLifecycleTransitions`, `TaskLifecycleTransitions`,
  `AgentRunLifecycleTransitions` and their respective tests (AD-016); AD
  Reconciliation AD-015/AD-016.
- Affected documents: `ARCHITECTURE_DECISIONS.md`.
- Implementation impact: none.
- Expected risk: **Low**.

---

**Change 9 — Add a Resource references field to `PlannerRuntimeSpecification.md` §10**
- Category: **Correction** (closing a specification/implementation
  mismatch) + **Documentation only**.
- Reason: §9 of the same document already anticipates this field in
  prose; §10's normative field list omits it; `TaskProposal.kt` already
  implements it.
- Evidence: `PlannerRuntimeSpecification.md` §9's "a Plan Candidate or
  Task Proposal may target" language; `src/contracts/TaskProposal.kt`;
  Review Brief "Clarifications Required for v1.1" #1; AD Reconciliation
  AD-013.
- Affected documents: `PlannerRuntimeSpecification.md`. Also touches the
  same underlying fact as AD-013's new implementation note (Change 7) —
  sequence AD-013 first so the specification edit can cite the AD's own
  naming of the pattern (see Documentation Ordering).
- Implementation impact: none — the Kotlin field already exists and is
  tested.
- Expected risk: **Low**.

---

**Change 10 — Clarifying cross-reference in `TaskManagerRuntimeSpecification.md` §9**
- Category: **Clarification** + **Documentation only**.
- Reason: prevent a future reader from assuming Task Context's existing
  "Resource references" concept is already continuous with
  `TaskProposal.resourceReferences`, when `Task.kt` carries no such
  field.
- Evidence: `src/contracts/Task.kt`'s own class KDoc (deliberately
  minimal, explicitly excluding Section 4 concepts including Resource
  references); Review Brief "Proposed v1.1 Documentation Updates" #5.
- Affected documents: `TaskManagerRuntimeSpecification.md`.
- Implementation impact: none.
- Expected risk: **Low**.

---

**Change 11 — New `ToolInvocationBinding.md` Volume 3 specification document**
- Category: **New guidance** + **Documentation only**.
- Reason: no specification document exists for a contract that is now
  load-bearing (Unit 11A wires it into the one execution path AD-003
  governs); `ToolRegistry.md`'s own backfill precedent
  (`docs/architecture/ARCHITECTURE_DECISIONS.md` AD-013 Evidence) is
  directly applicable.
- Evidence: direct search confirming no such document exists; AD
  Reconciliation "Documentation Updates Required" #12.
- Affected documents: new file under
  `docs/specifications/volume-03-core-interfaces/`; indirectly enables
  Change 4 (AD-003 citing a specification rather than a Kotlin file).
- Implementation impact: none — backfills an already-built,
  already-tested component per AD-013's own established pattern.
- Expected risk: **Low**, provided the document is written as a backfill
  (describing what `ToolInvocationBinding.kt` already does and is
  already tested to do) rather than as a forward-looking design
  document that could imply unbuilt behaviour.

---

**Change 12 — No new Architecture Decision (AD-017) at this time**
- Category: **Documentation only** — a decision *not* to add a decision,
  recorded so the choice is traceable.
- Reason: `ARCHITECTURE_DECISIONS.md` §7's own stated bar (three
  independent instances before promoting a recognised pattern to a
  Decision) is not yet met — two instances exist (Blocker 3, Unit 11B).
- Evidence: AD Reconciliation "Proposed New Architecture Decisions."
- Affected documents: none (no edit made).
- Implementation impact: none.
- Expected risk: **None** — status quo.

## Documentation Ordering

Recommended editing order, sequenced to minimise one edit invalidating
or duplicating another:

1. **`IMPLEMENTATION_GAPS.md`** (Changes 1–2). No dependency on any other
   document; finalises gap numbering and status before anything else
   cites it.
2. **New `ToolInvocationBinding.md`** (Change 11). Can cite the newly
   finalised gap number from step 1 if it chooses to reference the
   enforcement limitation; has no dependency on
   `ARCHITECTURE_DECISIONS.md`.
3. **`ARCHITECTURE_DECISIONS.md`** (Changes 3–8, i.e. AD-003, AD-005,
   AD-006, AD-007, AD-009, AD-013, AD-015, AD-016). Sequenced after
   steps 1–2 so AD-003 can cite the new specification document from step
   2 rather than a Kotlin file directly, and so any AD text referencing
   gap numbers (AD-007, AD-003) is accurate against step 1's finalised
   state.
4. **`PlannerRuntimeSpecification.md` §10** (Change 9). Sequenced after
   AD-013 (step 3) so the specification edit can align with, and
   optionally cite, the AD's own naming of the "prose-anticipated,
   field-list-omitted" pattern.
5. **`TaskManagerRuntimeSpecification.md` §9** (Change 10). Low-priority,
   self-contained; no dependency on any other step, placed last among
   substantive edits.
6. **`IMPLEMENTATION_HISTORY.md`** — verification pass only, no new
   content anticipated. Re-confirm after steps 1–5 that no cross-reference
   (Repository Status, Current Known Architecture Gaps, Current Runtime
   Chain) has drifted out of sync with the newly-updated gap/AD text.

## Validation Checklist

To be run after v1.1 documentation edits are applied (not run by this
plan):

- [ ] **No stale statements remain** — specifically: `IMPLEMENTATION_GAPS.md`
  #32's status/table entry, AD-005's and AD-006's Future Considerations
  sentences, all corrected.
- [ ] **No Sprint 1 contradiction remains** — no Architecture Decision or
  specification asserts something any test under `tests/` (234/234
  passing) disproves.
- [ ] **All closed gaps reflect closure accurately** — #32 moved to
  "Resolved" in the Phase 2 Gap Closure Summary table. Note: per
  `IMPLEMENTATION_GAPS.md`'s own stated practice ("No item in this file
  was closed by inventing behaviour beyond what its governing
  architecture document already specified"), closed items are
  re-categorised with updated status, not deleted — "removed" in this
  checklist means removed from the "open/deliberate scope boundary"
  bucket, not deleted from the file's history.
- [ ] **All remaining gaps still accurate** — #25, #26, #35, #36, #37,
  #39, #40 re-confirmed unchanged and untouched by Sprint 1 (per AD
  Reconciliation "Implementation Gap Review — Remaining gaps"); the new
  `ToolInvocationBinding` gap entry accurately distinct from #23, #24,
  and #32.
- [ ] **All Architecture Decisions internally consistent** — the AD-009
  clarification (Change 6, the one edit closest to touching Decision
  wording) does not narrow, weaken, or reinterpret the existing "every
  meaningful lifecycle transition emits an event" rule; AD-003's new
  Evidence entry does not imply construction-level enforcement that does
  not exist.
- [ ] **Runtime chain consistent everywhere** — the full chain (`Goal →
  Planner → TaskProposal → Task Manager → Task → AgentRunCommand → Agent
  Runtime → ExecutionRequest → Permission Engine → Action Mapping → Tool
  Registry → ToolInvocationBinding → Tool.validate() → Tool.execute() →
  ExecutionResult → Lifecycle Events → EventCollector`) as already
  recorded in `IMPLEMENTATION_HISTORY.md`'s Current Vertical Slice and
  Current Runtime Chain sections is not contradicted by a shorter or
  older chain anywhere in `ExecutionPipeline.md`, `ToolRegistry.md`, or
  the new `ToolInvocationBinding.md`.
- [ ] **Terminology consistent** — "`ToolInvocationBinding`" and
  "`resourceReferences`" (and `TaskProposal.resourceReferences` vs.
  `AgentRunCommand.resourceReferences` vs. Task Context's own unrelated
  "Resource references" concept, per Change 10) are named identically
  and distinguished identically everywhere they appear across the edited
  documents.
- [ ] **Implementation history aligned** — `IMPLEMENTATION_HISTORY.md`
  still accurately reflects commit `795544d`, 234/234, and Units 4–11B
  after steps 1–5 are applied, with no new drift introduced.

## Recommended Architecture Version

**Recommendation: the resulting documentation should become
`architecture-v1.1`.**

Reasoning: per the AD Reconciliation's own Executive Summary, none of
the 16 existing Architecture Decisions are superseded, replaced, or
contradicted by Sprint 1 — every change in this plan is a clarification,
a correction of now-stale text, or additional evidence for an
already-Accepted decision. No new Architecture Decision is being added
(AD-017 remains tracked, not adopted). This is consistent with a minor
version increment: `architecture-v1.0`'s decision set and specification
structure remain intact and binding; `v1.1` is v1.0 with its
documentation brought current against the first executable proof of
that architecture, not a new architectural direction. A major version
change would only be warranted if a decision were superseded or a new,
adopted Architecture Decision changed platform-wide behaviour —
neither occurred.

## Rules Confirmation

This document is planning only. No architecture chapter, specification,
`ARCHITECTURE_DECISIONS.md`, or `IMPLEMENTATION_GAPS.md` was edited. No
new Architecture Decision was created. No implementation gap was opened
or closed. No prose was rewritten.

## Files Created

- `docs/architecture/PARKER_ARCHITECTURE_V1_1_CONSOLIDATION_PLAN.md`

## Files Modified

- None.
