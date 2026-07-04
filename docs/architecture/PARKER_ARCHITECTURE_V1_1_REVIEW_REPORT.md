# Parker Architecture v1.1 — Review Report

## Status

Documentation-only. No Kotlin, tests, or Gradle files were modified in
producing this report or the changes it summarizes. No Sprint 2 work has
begun. Nothing has been committed, pushed, or tagged.

This report summarizes the Architecture v1.1 documentation updates
applied per `docs/architecture/PARKER_ARCHITECTURE_V1_1_CONSOLIDATION_PLAN.md`,
itself built on `docs/architecture/PARKER_ARCHITECTURE_V1_1_REVIEW_BRIEF.md`
and `docs/architecture/PARKER_ARCHITECTURE_V1_1_AD_RECONCILIATION.md`, all
three previously accepted as the authoritative basis for this pass.

---

## 1. Documents Modified

- **`docs/architecture/IMPLEMENTATION_GAPS.md`** — closed gap #32, opened
  new gap #41, updated the Phase 2 Gap Closure Summary table and header.
- **`docs/specifications/volume-03-core-interfaces/ToolInvocationBinding.md`**
  — new file (backfill specification for already-built behaviour).
- **`docs/architecture/ARCHITECTURE_DECISIONS.md`** — eight edits across
  AD-003, AD-005, AD-006, AD-007, AD-009, AD-013, AD-015, AD-016.
- **`docs/specifications/volume-06-planner-runtime/PlannerRuntimeSpecification.md`**
  — one new field added to the §10 Proposal Model field list.
- **`docs/specifications/volume-05-task-manager-runtime/TaskManagerRuntimeSpecification.md`**
  — one clarifying sentence added to the §9 Task Context Model.

Each is detailed in Sections 3–5 below.

## 2. Documents Intentionally Unchanged

- **`docs/implementation/IMPLEMENTATION_HISTORY.md`** — verification
  pass only, per the Consolidation Plan's explicit "No action required —
  confirm only." Repository Status (commit `4a44abe`, 234/234), the
  Current Vertical Slice diagram, the Current Runtime Chain, and the
  Current Known Architecture Gaps list were re-read against the five
  edits above and found to still accurately describe Sprint 1 as built.
  No drift was introduced. One pre-existing, pre-Sprint-1 bullet in
  Current Known Architecture Gaps ("Execution Pipeline access is still
  enforced by convention rather than construction") was checked against
  `IMPLEMENTATION_GAPS.md` and found to correspond to no existing gap
  number — it appears to predate the numbered-gap convention and concern
  a different question (unrestricted callers of `ExecutionPipeline.submit`
  itself) than the newly-closed #32 or newly-opened #41 (both about
  `ToolInvocationBinding`/`ToolRegistry.resolve` specifically). It is left
  untouched, as the Consolidation Plan did not identify it for edit, but
  is flagged here for a future human decision on whether it should be
  reconciled into a numbered gap.
- **`docs/specifications/volume-03-core-interfaces/ExecutionPipeline.md`**,
  **`ToolRegistry.md`**, **`AgentRuntimeSpecification.md`**,
  **`action-mapping.md`**, **`IdentityService.md`**, **`Task-Schema.md`**,
  and all other specification/architecture documents not listed in
  Section 1 — the Consolidation Plan identified no required or
  recommended change to any of these; each was reviewed in the AD
  Reconciliation and found consistent with Sprint 1 as built.
- **AD-001, AD-002, AD-004, AD-008, AD-010, AD-011, AD-012, AD-014** — no
  change. Each was reviewed in the AD Reconciliation and found either
  fully validated by Sprint 1 (AD-010, AD-014) or not exercised by
  Sprint 1's scope (AD-011, AD-012) or otherwise unaffected.
- **AD-017 ("Runtime Data Propagation Must Be Explicit")** — evaluated
  and explicitly **not adopted**. Two independent instances exist
  (`AgentRunCommand`'s Blocker-3 shape; `TaskProposal.resourceReferences`),
  short of `ARCHITECTURE_DECISIONS.md` §7's own three-instance threshold
  for this category of candidate. Remains tracked, not created.

## 3. Clarifications Introduced

Clarifications add explanation or distinguish existing concepts without
changing what any Decision or specification requires.

- **AD-003** — Evidence and Affected Specifications now cite
  `ToolInvocationBinding.md` and note that Sprint 1 Unit 11A confirmed
  the sole-execution-authority rule *by construction* (one call site)
  where previously it was confirmed only up to the point of Tool
  resolution. The Decision text itself is unchanged; no claim of
  construction-level *access* enforcement was introduced (that remains
  gap #41, tracked separately).
- **AD-009** — a new "Clarification (Architecture v1.1)" paragraph
  distinguishes **publication** (producers calling `EventBus.publish`),
  **observation** (a subscriber like `EventCollector` consuming
  published events), and **audit reconstruction** (Chapter 43's own,
  still-unbuilt responsibility), citing `EventCollector`'s own KDoc. The
  existing rule — every meaningful lifecycle transition MUST emit an
  event — is explicitly preserved, not narrowed.
- **AD-013** — a new "Implementation note (Architecture v1.1)" names the
  "prose-anticipated capability, field-list omitted" pattern explicitly,
  citing `AgentRunCommand` (Blocker 3) and `TaskProposal.resourceReferences`
  (Unit 11B) as its two instances, and recommends the affected
  specification field lists be updated to match (addressed directly by
  Section 4/5 changes below).
- **`TaskManagerRuntimeSpecification.md` §9** — one sentence distinguishes
  Task Context's own "Resource references" concept from
  `TaskProposal.resourceReferences`, noting `Task.kt` carries no such
  field today.

## 4. Corrections Introduced

Corrections remove or replace text that had become factually stale
relative to Sprint 1 as actually built.

- **`IMPLEMENTATION_GAPS.md` #32** — Status line corrected from "known,
  expected limitation" to a closure statement citing Unit 11A, commit
  `13c9322`, and `DefaultExecutionPipeline.executeResolvedTool`. The
  original pre-11A finding text is retained below it for historical
  context, per this file's own established convention (never delete,
  always retain and annotate). The "Sprint 1 contract-closure addendum"
  paragraph's closing sentence, which had claimed the gap remained open,
  is corrected to state the three previously-outstanding pieces were
  subsequently completed. The Phase 2 Gap Closure Summary table moved
  #32 from "Deliberate scope boundaries" to "Resolved."
- **AD-005 Future Considerations** — the stale claim that "the Task
  Manager Runtime Specification does not yet define a Task Proposal
  intake operation or a disposition mechanism" is corrected: Sprint 1
  Unit 6 implements both (`InMemoryTaskManagerRuntime.submitProposal`;
  `TaskProposalDisposition`, Accept-only). Decision text unchanged.
- **AD-006 Future Considerations** — the stale claim that "no named
  contract yet exists for the Task Manager Runtime to request an Agent
  Run's creation or cancellation" is corrected: `AgentRunCommand`
  (pre-Sprint-1) is that contract, and Sprint 1 Units 6–7 implement its
  construction and consumption (`START` only). Decision text unchanged.

## 5. Strengthened Statements

Strengthening adds new evidence to an already-correct Decision without
changing its wording.

- **AD-007 Future Considerations** — a new sentence notes that gap #40's
  real-world stakes increased after Unit 11A: an incorrect `APPROVED`
  decision previously had no downstream consequence (no Tool ever ran);
  now it causes a real `Tool.execute()` call. The gap itself, and the
  Decision text, are unchanged.
- **AD-015 Evidence** — three new cases added: a resolved-but-unbound
  Tool, a Tool failing `validate()`, and a Tool failing `execute()` each
  independently produce `FAILED`, never `DENIED` (Unit 11A,
  `tests/runtime/DefaultExecutionPipelineTest.kt`) — the first Evidence
  citing a post-Permission-Engine-approval application of this rule.
- **AD-016 Evidence** — three new, independently-tested lifecycle
  machines added (`PlanningSessionLifecycleTransitions`,
  `TaskLifecycleTransitions`, `AgentRunLifecycleTransitions`), each
  enforcing the already-Accepted terminal-state rule.

## 6. Remaining Implementation Gaps

Confirmed unchanged, out of Sprint 1's scope, and not touched by this
documentation pass:

- **#25** — `PermissionEngine` policy remains fake/test-only.
- **#26** — `EventBus` authentication/signature verification remain
  placeholders.
- **#35** — Cascading revocation on Principal Revoke undecided.
- **#36** — `PrincipalLifecycleTransitions` permits only the literal
  linear chain; undecided.
- **#37** — `resolve()` does not suppress Revoked/Archived Principals;
  undecided.
- **#39** — `identity.*` event publishing not implemented.
- **#40** — `PermissionEngine.evaluate` not wired to resolve identity
  first (higher real-world stakes since Unit 11A; see AD-007 above).
- **#41 (new)** — `ToolInvocationBinding.invocableFor` and
  `ToolRegistry.resolve` restrict callers to the Execution Pipeline by
  documentation convention only, not by construction. Distinct from the
  now-closed #32. Requires a human decision on whether a caller-identity
  or visibility mechanism should be introduced, or whether convention-based
  restriction remains acceptable.

## 7. Remaining Architecture Gaps

- The Planner Runtime Specification has still not received its own
  dedicated review-and-correction pass (AD-014's Future Considerations,
  unchanged by this pass).
- Task Manager disposition beyond `Accept` (`Deferred`/`Split`/`Merged`/
  business-reason `Rejected`) and Agent Run commands beyond `START`
  (`SUSPEND`/`RESUME`/`CANCEL`) remain unimplemented — noted as
  implementation breadth gaps under the now-corrected AD-005/AD-006, not
  contract gaps.
- AD-017 ("Runtime Data Propagation Must Be Explicit") remains tracked,
  not adopted, pending a third independent instance of the
  prose-anticipated-capability pattern.
- Chapter 43 (Audit and Observability) remains unbuilt; `EventCollector`
  is explicitly a test-only stand-in, not a production audit component
  (per the new AD-009 clarification).
- `IMPLEMENTATION_HISTORY.md`'s "Execution Pipeline access is still
  enforced by convention rather than construction" bullet (Section 2
  above) does not map to a numbered gap and may warrant reconciliation
  in a future pass.

## 8. Internal Consistency Confirmation

Architecture-v1.1 remains internally consistent:

- No Architecture Decision was superseded, replaced, or contradicted.
  All eight AD edits are clarifications, corrections of stale Future
  Considerations text, or additional Evidence for already-Accepted
  decisions.
- AD-009's new clarifying paragraph does not narrow or weaken "every
  meaningful lifecycle transition emits an event"; it only distinguishes
  publication, observation, and audit reconstruction as concepts.
- AD-003's new Evidence does not assert construction-level *access*
  enforcement that does not exist; that gap is tracked separately as #41.
- The recorded runtime chain (`Goal → Planner → TaskProposal → Task
  Manager → Task → AgentRunCommand → Agent Runtime → ExecutionRequest →
  Permission Engine → Action Mapping → Tool Registry →
  ToolInvocationBinding → Tool.validate() → Tool.execute() →
  ExecutionResult → Lifecycle Events → EventCollector`) in
  `IMPLEMENTATION_HISTORY.md` is not contradicted by `ExecutionPipeline.md`,
  `ToolRegistry.md`, or the new `ToolInvocationBinding.md`.
- Terminology is consistent across all edited documents:
  `TaskProposal.resourceReferences` (Planner-side, carried to
  `AgentRunCommand.resourceReferences`) is distinguished by name from
  Task Context's own, unrelated "Resource references" concept everywhere
  both appear.
- No new Architecture Decision was created; AD-017 remains evaluated,
  not adopted.

## 9. Confirmation: No Sprint 2 Work Has Begun

No Kotlin file under `src/` or `tests/` was read for the purpose of
modification, and none was modified. No Gradle file was modified. No new
Architecture Decision was created. AD-017 was not adopted. All changes in
this pass are confined to `docs/architecture/` and
`docs/specifications/`, applying only the changes already approved in
the Review Brief, AD Reconciliation, and Consolidation Plan. Nothing has
been committed, pushed, or tagged.

---

## Recommended Version

Per the Consolidation Plan's own recommendation, unchanged by this pass:
the resulting documentation set should become **`architecture-v1.1`** — a
minor version, since no Architecture Decision is superseded and the
existing decision set and specification structure remain intact and
binding. Formally stamping this (updating
`IMPLEMENTATION_HISTORY.md`'s "Architecture Version" line, tagging, etc.)
is a separate action left for explicit instruction, not performed as
part of this documentation pass.

## Related

- `docs/architecture/PARKER_ARCHITECTURE_V1_1_REVIEW_BRIEF.md`
- `docs/architecture/PARKER_ARCHITECTURE_V1_1_AD_RECONCILIATION.md`
- `docs/architecture/PARKER_ARCHITECTURE_V1_1_CONSOLIDATION_PLAN.md`
- `docs/architecture/ARCHITECTURE_DECISIONS.md`
- `docs/architecture/IMPLEMENTATION_GAPS.md`
- `docs/specifications/volume-03-core-interfaces/ToolInvocationBinding.md`
- `docs/specifications/volume-06-planner-runtime/PlannerRuntimeSpecification.md`
- `docs/specifications/volume-05-task-manager-runtime/TaskManagerRuntimeSpecification.md`
- `docs/implementation/IMPLEMENTATION_HISTORY.md`
