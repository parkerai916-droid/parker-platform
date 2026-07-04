# Parker Architecture v1.1

## Architecture Decision Reconciliation

## Executive Summary

Sprint 1 (commit `795544d`, tag `sprint1-complete`, 234/234 Android
Studio tests) is the first executable evidence base `ARCHITECTURE_DECISIONS.md`'s
16 Accepted decisions (AD-001–AD-016) have ever been checked against.
This document reconciles each decision with that evidence.

**Headline result:** no decision requires replacement. Thirteen remain
**unchanged** (proven or unexercised, in both cases correct as written).
Three require **clarification** (AD-003, AD-005, AD-006, AD-009,
AD-013 — see below; AD-005/AD-006 clarifications are narrow,
Future-Considerations-only). Two are **strengthened** by direct
executable proof of behaviour the architecture already claimed but had
never been tested (AD-015, AD-016). None of the 16 decisions were
contradicted by Sprint 1 — every deviation found was an implementation
gap already recorded in `IMPLEMENTATION_GAPS.md`, not a defect in the
decision itself.

One implementation gap (`IMPLEMENTATION_GAPS.md` #32) is fully closed.
One new gap is justified and recommended (`ToolInvocationBinding`/`ToolRegistry.resolve`
access enforcement — distinct from #32, not a reuse of it). One candidate
new Architecture Decision (data propagation explicitness) is evaluated
and **not** recommended for adoption yet, on the architecture document's
own stated evidentiary bar.

This document makes no edits to `ARCHITECTURE_DECISIONS.md`,
`IMPLEMENTATION_GAPS.md`, or any other architecture document. All
findings below are recommendations.

---

## AD Review

Throughout, **Sprint 1 tests** cites specific files under `tests/`;
**implementation** cites `src/`; **architecture** cites the pre-existing
`docs/architecture/`/`docs/specifications/` text the AD itself already
references.

### AD-001 — Identity First

**Status:** Unchanged.

**Evidence:**
- Architecture: `IdentityService.md` ("who is requesting this" vs.
  "are they allowed"); `AgentRuntimeSpecification.md` §7;
  `TaskManagerRuntimeSpecification.md` §8.
- Implementation: `InMemoryTaskManagerRuntime.submitProposal` resolves
  `proposedOwnerPrincipalId` via `IdentityService.resolve` before
  creating a `Task`; `InMemoryAgentRuntime.start` resolves the derived
  Agent Identity before advancing past `CREATED`.
- Sprint 1 tests: `tests/runtime/InMemoryTaskManagerRuntimeTest.kt`
  ("ownerPrincipalId is resolved through the Identity Service, not
  trusted as-is"); `tests/runtime/InMemoryAgentRuntimeTest.kt` (identity
  resolution required before `INITIALISED`).
- Proven: the decision holds for every resolvable-identity path Sprint 1
  exercises. **Not exercised:** the non-Active-Principal path — Sprint 1
  never submitted a request under a Suspended/Revoked/Archived Principal.
  AD-001's own Future Considerations already list #35/#37/#39 as open;
  Sprint 1 changes none of them.

**Recommendation:** No change. Future Considerations already accurately
describe the open gaps; no new one was discovered.

---

### AD-002 — Proposal Before Authority

**Status:** Unchanged.

**Evidence:**
- Architecture: `PlannerRuntimeSpecification.md` §2;
  `TaskManagerRuntimeSpecification.md` §1.
- Implementation: across Units 5–7, no subsystem collapses two roles —
  `DeterministicPlannerHarness` never writes a `taskId` or resolves
  permission; `InMemoryTaskManagerRuntime` never calls
  `ToolRegistry.resolve` or `PermissionEngine.evaluate`;
  `InMemoryAgentRuntime` proposes (constructs an `ExecutionRequest`) but
  never authorises or executes it directly.
- Sprint 1 tests: the full four-role chain is exercised end-to-end by
  `tests/runtime/VerticalSliceEndToEndTest.kt` with the four roles never
  merging.
- Proven, not merely unexercised: this is the first time all four roles
  ran together in one call chain.

**Recommendation:** No change. Consider citing
`VerticalSliceEndToEndTest.kt` as a new evidentiary example alongside
the existing specification citations, since it is now the single test
that exercises all four roles together — an editorial addition, not a
decision change.

---

### AD-003 — Execution Pipeline Is the Sole Execution Authority

**Status:** Clarified.

**Specific questions asked:** should the invariant become stronger now
that `ToolInvocationBinding` is wired; should `ToolInvocationBinding`
ownership move into this AD.

**Evidence:**
- Architecture: `ExecutionPipeline.md` ("MUST NOT bypass PermissionEngine");
  `ToolRegistry.md` ("`resolve` is... **intended to be called only by**
  the Execution Pipeline" — already convention-based language, not
  construction-enforced language, predating Sprint 1).
- Implementation: `DefaultExecutionPipeline.executeResolvedTool` (Unit
  11A) is the only call site anywhere in the repository that invokes
  `ToolInvocationBinding.invocableFor`, `Tool.validate`, or
  `Tool.execute` — confirmed by direct reading of every production file
  under `src/runtime/`.
- Sprint 1 tests: `tests/runtime/DefaultExecutionPipelineTest.kt`'s Unit
  11A tests prove a DENIED decision never reaches
  `ToolInvocationBinding` at all (`validateCallCount`/`executeCallCount`
  both 0).
- `InMemoryToolInvocationBinding`'s own KDoc states its
  "Execution-Pipeline-only" restriction is enforced "by documentation
  only... no caller-identity check, no reduced visibility," explicitly
  mirroring `ToolRegistry.resolve`'s identical, pre-existing pattern.

**Analysis:** The Decision text itself ("No subsystem holds an
invocable Tool reference or has a second path to an external effect")
does not need to become stronger — it is already the strongest possible
statement of the rule, and Sprint 1 proved it is *true today* (no second
call site exists). What Sprint 1 changed is that this rule now governs
a real invocation, not merely a resolution — previously "sole execution
authority" was checked only up to "the only place that resolves a
`ToolDescriptor`," since nothing downstream of that ever ran (`IMPLEMENTATION_GAPS.md`
#32). The AD's Evidence and Affected specifications sections predate
`ToolInvocationBinding`'s existence and do not cite it.

**Recommendation:**
- Add `ToolInvocationBinding` (currently `src/contracts/ToolInvocationBinding.kt`
  only — no Volume 3 specification document exists for it, confirmed by
  direct search) to AD-003's Evidence and Affected specifications once a
  specification document exists to cite (see Documentation Updates
  Required).
- Do **not** move "ownership" of `ToolInvocationBinding` into AD-003.
  Per the document's own "How to Use This Document" section, an
  Architecture Decision "does not define a lifecycle, an event schema,
  or a field list" — that is a specification's job. AD-003 should
  **govern** `ToolInvocationBinding` (as it already does, by the same
  rule that governs `ToolRegistry.resolve`), not **own** it.
- Do not claim construction-enforcement exists. It does not, for either
  `ToolRegistry.resolve` or `ToolInvocationBinding.invocableFor` — this
  is the subject of the new gap recommended below, not an AD-003 change.

---

### AD-004 — Task Manager Owns Canonical Tasks

**Status:** Unchanged.

**Evidence:**
- Architecture: `Task-Schema.md`; ADR-012.
- Implementation: `src/contracts/Task.kt` is the only Task-shaped type
  produced by Sprint 1 code; `TaskProposal` and `AgentRun` are
  deliberately distinct types, never conflated with `Task`.
- Sprint 1 tests: `tests/runtime/InMemoryTaskManagerRuntimeTest.kt`
  confirms exactly one `Task` per accepted `TaskProposal`.

**Recommendation:** No change.

---

### AD-005 — Planner Never Creates Tasks

**Status:** Clarified (Future Considerations only; Decision text
unchanged).

**Evidence:**
- Architecture: `PlannerRuntimeSpecification.md` §6, §13.
- Implementation: `DeterministicPlannerHarness` has no operation that
  writes a `TaskId`; only `InMemoryTaskManagerRuntime.submitProposal`
  creates one.
- Sprint 1 tests: `tests/runtime/DeterministicPlannerHarnessTest.kt` (no
  Task-creating operation exists on the harness at all — structurally,
  not just behaviourally, impossible).

**Analysis:** AD-005's own Future Considerations state: "The Task
Manager Runtime Specification does not yet define a Task Proposal
intake operation or a disposition mechanism... reported back to the
Planner." This is now stale — `TaskProposalIntake.submitProposal`
(named in the specification-preparation pass) is implemented by
`InMemoryTaskManagerRuntime` (Unit 6), and the disposition mechanism
(`TaskProposalDisposition`) is real and tested.

**Recommendation:** Update AD-005's Future Considerations to note the
intake operation and disposition mechanism now have a Sprint 1
implementation (accept-only; `Deferred`/`Split`/`Merged`/business-reason
`Rejected` remain unimplemented, per `IMPLEMENTATION_GAPS.md`-adjacent
Sprint 1 scope notes). Decision text itself requires no change.

---

### AD-006 — Agent Runtime Never Owns Tasks

**Status:** Clarified (Future Considerations only; Decision text
unchanged).

**Evidence:**
- Architecture: `AgentRuntimeSpecification.md` §5, §11;
  `TaskManagerRuntimeSpecification.md` §6.
- Implementation: `InMemoryAgentRuntime` reads `command.taskId` as a
  reference only; it never constructs, mutates, or reads a `Task`
  record.
- Sprint 1 tests: no test in `tests/runtime/InMemoryAgentRuntimeTest.kt`
  gives `InMemoryAgentRuntime` any dependency capable of writing Task
  state — structurally enforced by the constructor signature, not just
  by convention.

**Analysis:** AD-006's Future Considerations state: "No named contract
yet exists for the Task Manager Runtime to request an Agent Run's
creation or cancellation." This is now stale — `AgentRunCommand`
(Blocker 3, pre-Sprint-1 contract closure) is exactly that named
contract, and Units 6–7 implement its construction
(`InMemoryTaskManagerRuntime`) and consumption
(`InMemoryAgentRuntime`).

**Recommendation:** Update AD-006's Future Considerations to note
`AgentRunCommand` now has a real construction and consumption path
(`START` only; `SUSPEND`/`RESUME`/`CANCEL` remain unimplemented,
returning explicit `Rejected`, never a silent no-op). Decision text
itself requires no change.

---

### AD-007 — Permission Decisions Belong to the Permission Engine

**Status:** Unchanged.

**Specific question asked:** now that the runtime executes real Tools,
does this AD still fully describe the authority boundary?

**Evidence:**
- Architecture: `PermissionEngine.md` ("Every ExecutionRequest MUST be
  evaluated before execution").
- Implementation: `DefaultExecutionPipeline.executeResolvedTool` (Unit
  11A) is reached only from the `APPROVED`/`APPROVED_WITH_CONFIRMATION`
  branch of `submit()`, strictly after `permissionEngine.evaluate`
  returns.
- Sprint 1 tests: `tests/runtime/DefaultExecutionPipelineTest.kt`
  ("denied request never reaches Tool resolution, and the bound Tool is
  never validated or executed" — `validateCallCount`/`executeCallCount`
  both 0 on `DENIED`).

**Analysis:** Yes — AD-007 still fully describes the authority
boundary. The boundary is about *where a decision is made* (exactly
once, by `PermissionEngine.evaluate`), not about *what happens after* a
decision is made. Sprint 1 changed the second thing (a `SUCCESS` now
means a Tool really ran) without touching the first. However, this
raises the real-world stakes of an already-recorded gap: AD-007's
Future Considerations already note `PermissionEngine.evaluate` is "not
yet wired to resolve identity first" (`IMPLEMENTATION_GAPS.md` #40).
Before Unit 11A, an incorrect `APPROVED` decision had no real
consequence (no Tool ever ran). After Unit 11A, an incorrect `APPROVED`
decision now causes a real `Tool.execute()` call. The gap itself is
unchanged; its consequence is not.

**Recommendation:** No change to the Decision text. Recommend the
Future Considerations paragraph be amended to note that gap #40's
priority has increased now that Tool execution is real, without
changing what the gap *is*.

---

### AD-008 — Identity Decisions Belong to Identity Service

**Status:** Unchanged.

**Evidence:**
- Architecture: `IdentityService.md`.
- Implementation: no Sprint 1 component calls `updateStatus` or
  constructs a `Principal` record outside `InMemoryIdentityService`
  itself; every consumer (`InMemoryTaskManagerRuntime`,
  `InMemoryAgentRuntime`) only calls `resolve`.
- Sprint 1 tests: none exercise `updateStatus` from a non-Identity-Service
  caller (none exist to test against, by construction).

**Recommendation:** No change. Sprint 1 exercised only the read
(`resolve`) half of this decision; the write (`updateStatus`) half
remains exactly as previously specified and untouched.

---

### AD-009 — Everything Important Is Auditable

**Status:** Clarified.

**Specific question asked:** implementation now distinguishes lifecycle
publication, event observation, and audit reconstruction — should the
AD explicitly distinguish those concepts?

**Evidence:**
- Architecture: `EventBus.md`; `EventType.md`'s `<domain>.<event>`
  convention.
- Implementation: Unit 9 (`DeterministicPlannerHarness`,
  `InMemoryTaskManagerRuntime`, `InMemoryAgentRuntime`,
  pre-existing `DefaultExecutionPipeline`) is publication —
  producers call `EventBus.publish`. Unit 10 (`EventCollector`) is
  observation — a subscriber that collects and later queries by
  `correlationId`. Neither is Chapter 43's Audit and Observability
  component, which does not exist in this repository.
- Sprint 1 tests: `EventCollector`'s own KDoc states plainly: "Test-only
  fixture... nothing in the specifications requires a production
  auditing/collection component; this exists solely so a test can
  reconstruct what a run actually published" — the implementation
  itself already draws this distinction in prose, ahead of the
  architecture document doing so.

**Analysis:** AD-009's Decision text says events are emitted "so that
Chapter 43 (Audit and Observability) has the same visibility into any
subsystem's activity" — treating "an event was published" as
synonymous with "this is auditable." Sprint 1 exposed a real
three-way distinction the single sentence does not make: (1)
publication is a producer obligation, now proven across three domains;
(2) observation is what any subscriber (a real future Chapter 43
component, or `EventCollector`) can do with published events; (3) audit
reconstruction is Chapter 43's own, still-unbuilt responsibility.
"Auditable" today accurately means "structurally reconstructable by
some future or test-time subscriber," not "currently being audited."

**Recommendation:** Clarify AD-009 to explicitly separate these three
concepts, citing `EventCollector` as the existing implementation
evidence that observation and audit-reconstruction are not the same
component, and that Sprint 1 satisfies (1) and demonstrates (2) is
possible, without implementing (3) at all.

---

### AD-010 — Model Independence

**Status:** Unchanged.

**Evidence:**
- Architecture: `AgentRuntimeSpecification.md` §2;
  `PlannerRuntimeSpecification.md` §2.
- Implementation: `DeterministicPlannerHarness`'s own KDoc: "Deterministic
  by construction, per AD-010: given the same arguments to `run`, the
  produced `PlanCandidate` and `TaskProposal` are identical every time —
  no randomness, no clock reads beyond what the caller supplies, no
  external state."
- Sprint 1 tests: `tests/runtime/DeterministicPlannerHarnessTest.kt`
  ("two harnesses given identical arguments produce identical
  PlanCandidate and TaskProposal").

**Recommendation:** No change. This is the one AD Sprint 1's own
implementation explicitly cites by ID in its KDoc — direct,
self-declared proof.

---

### AD-011 — Context Is Reference-Based

**Status:** Unchanged (not meaningfully exercised).

**Evidence:**
- Architecture: `TaskManagerRuntimeSpecification.md` §9;
  `PlannerRuntimeSpecification.md` §9.
- Implementation: Sprint 1 does not implement Planning Context, Task
  Context, or Agent Context in any real sense —
  `DeterministicPlannerHarness`'s own KDoc states it "does not... resolve
  real Planning Context," consistent with `SPRINT_1_VERTICAL_SLICE_PLAN.md`
  §2's "Planning Context gathering is trivial."

**Analysis:** `TaskProposal.resourceReferences` (Unit 11B) carries a
concrete `ResourceId`, not a copied `Resource` — consistent *in style*
with reference-based Context, but it is a Task Proposal field, not a
Context model implementation, so it does not constitute a test of
AD-011 itself.

**Recommendation:** No change. Flag for a future sprint that actually
implements one of the three Context models — Sprint 1 provides no
evidence either for or against this decision beyond stylistic
consistency.

---

### AD-012 — Memory and World Model Are Context Providers

**Status:** Unchanged (not exercised).

**Evidence:** No Memory or World Model implementation exists anywhere
in Sprint 1's scope, per `SPRINT_1_VERTICAL_SLICE_PLAN.md` §3's own
Non-Scope list.

**Recommendation:** No change. Not applicable to Sprint 1.

---

### AD-013 — Specifications Define Contracts

**Status:** Clarified.

**Specific question asked:** did Sprint 1 validate this decision; does
it need implementation notes?

**Evidence:**
- Architecture: every specification's Status header ("specification
  only... Nothing described here is authorised for implementation until
  an explicitly-declared implementation phase promotes it").
- Implementation: every Sprint 1 unit cites a specific specification
  section as its authorising basis (e.g. Unit 6's `InMemoryTaskManagerRuntime`
  KDoc cites `TaskManagerRuntimeSpecification.md` §15; Unit 7's
  `InMemoryAgentRuntime` cites §5/§6 of the Agent Runtime Specification).
- Sprint 1 tests: none directly test this decision (it is a process
  rule, not a runtime behaviour), but the pattern held across all 9
  implementation units without exception.

**Analysis — validated, with one exposed edge case:** Sprint 1
validated the general rule (no Kotlin was written without an upstream
specification citation). It also exposed a case AD-013's existing text
does not name: `TaskProposal.resourceReferences` (Unit 11B) is a
contract addition grounded in `PlannerRuntimeSpecification.md` §9's
prose ("Resource references... a Plan Candidate or Task Proposal may
target"), even though §10's own normative field list does not name the
field. This is not "writing a specification alongside the Kotlin it
describes" (AD-013's named exception) — the specification predates the
Kotlin — but it is also not full alignment: the specification's field
list has not yet been updated to match. AD-013 does not currently name
this third case (contract change grounded in existing prose, ahead of a
field-list update).

**Recommendation:** Add an implementation note to AD-013 naming this
third case explicitly, citing both `AgentRunCommand` (Blocker 3) and
`TaskProposal.resourceReferences` (Unit 11B) as its two instances, and
recommend the corresponding specification field lists be updated to
match (see Documentation Updates Required). Decision text itself
remains correct.

---

### AD-014 — Architecture Before Implementation

**Status:** Unchanged — validated.

**Specific question asked:** did Sprint 1 validate this; does it
require clarification?

**Evidence:**
- Architecture: `AgentRuntimeSpecificationReview.md`,
  `TaskManagerRuntimeSpecificationReview.md` (completed review-and-
  correction passes); AD-014's own Future Considerations already state
  the Planner Runtime Specification has **not** received the same pass.
- Implementation: `DeterministicPlannerHarness` lives in `tests/runtime/`,
  not `src/runtime/`, and its own KDoc states this is deliberate: it
  models 5 of 10 lifecycle states, generates exactly one Plan Candidate,
  and performs no Plan Decision — explicitly because "the Planner Runtime
  itself remains specification-only... not yet promoted to an
  implementation phase."

**Analysis:** Sprint 1 validated this decision by *respecting* the
already-documented gap rather than working around it: rather than
building a production `src/runtime/` Planner implementation against a
specification AD-014 itself flags as not-yet-reviewed, the team built
the smallest possible deterministic test fixture. This is direct,
practical evidence for AD-014's own Reasoning ("cheaper and less risky
to do on paper than to discover the same issue after Kotlin has been
written against a flawed design") — no Kotlin was written against the
unreviewed parts of the Planner Runtime Specification's design (Plan
Decision, multi-candidate generation, the 5 unmodelled lifecycle
states).

**Recommendation:** No change. Optionally add
`DeterministicPlannerHarness`'s own scope-limiting choice as a
concrete example of this decision being followed in practice.

---

### AD-015 — Invalid Is Not Denied

**Status:** Strengthened.

**Specific question asked:** should the AD explicitly record that
failed validation, missing Tool binding, and Tool execution failure all
become `FAILED` rather than `DENIED`?

**Evidence:**
- Architecture: `action-mapping.md`, `IdentityService.md`,
  `AgentRuntimeSpecification.md` §10, `TaskManagerRuntimeSpecification.md`
  §8 — all pre-Sprint-1 sources, all about upstream validation failures
  (unresolvable action, unresolvable Principal).
- Implementation: `DefaultExecutionPipeline.executeResolvedTool` (Unit
  11A) — a resolved-but-unbound `ToolDescriptor`, a failed
  `Tool.validate()`, and a failed `Tool.execute()` each independently
  produce `ExecutionResultStatus.FAILED`, never `DENIED`, per the
  method's own KDoc citing AD-015 by name.
- Sprint 1 tests: `tests/runtime/DefaultExecutionPipelineTest.kt` proves
  all three cases directly and independently ("a resolved Tool with no
  invocable binding produces a FAILED result, never DENIED"; "a Tool
  that fails validation produces a FAILED result"; "a Tool whose execute
  reports failure produces a FAILED result").

**Analysis:** These three cases are new points in the pipeline where
AD-015 applies — later than any case the AD's existing Evidence
section cites (all of which are pre-Permission-Engine validation
failures). AD-015's Evidence section has never previously cited a
post-approval, execution-time application of the same rule.

**Recommendation:** Add these three cases to AD-015's Evidence section,
citing `tests/runtime/DefaultExecutionPipelineTest.kt` and
`DefaultExecutionPipeline.executeResolvedTool` directly, as a second,
later-pipeline-stage instance of the same already-Accepted rule.
Decision text itself is already correct and needs no wording change —
only additional evidence.

---

### AD-016 — Terminal Lifecycle States Are Final

**Status:** Strengthened.

**Evidence:**
- Architecture: `Task-Schema.md`; `AgentRuntimeSpecification.md` §5;
  `PlannerRuntimeSpecification.md` §5.
- Implementation: three new lifecycle machines were added by Sprint 1
  (`PlanningSessionLifecycleTransitions`, `TaskLifecycleTransitions`,
  `AgentRunLifecycleTransitions`), each independently enforcing this
  rule.
- Sprint 1 tests: `SPRINT_1_VERTICAL_SLICE_PLAN.md` §7 explicitly
  required negative tests at "each new lifecycle introduced this
  sprint," and they exist:
  `tests/runtime/DeterministicPlannerHarnessTest.kt` ("SUBMITTED is
  terminal for this fixed harness"); `tests/runtime/DefaultExecutionPipelineTest.kt`
  ("cancel after a terminal result reports not cancelled");
  contract-level tests for `TaskLifecycleTransitions` and
  `AgentRunLifecycleTransitions`.

**Recommendation:** Add these three new lifecycle machines to AD-016's
Evidence section as newly-implemented, explicitly-tested instances of
an already-Accepted rule.

---

## Proposed New Architecture Decisions

**Candidate: AD-017, "Runtime Data Propagation Must Be Explicit"
(evaluated, not created).**

**Evidence for:**
- Two independent, real instances exist where data needed at a
  downstream layer had no explicit carrying field until implementation
  reached the point of requiring it: `AgentRunCommand`'s
  Suspend/Resume/Cancel shape (Blocker 3, pre-Sprint-1) and
  `TaskProposal.resourceReferences` (Unit 11B, discovered specifically
  because `SPRINT_1_VERTICAL_SLICE_PLAN.md` §7's own acceptance
  criteria could not be honestly met without it).
- Both instances span multiple specifications (Planner Runtime
  Specification, Task Manager Runtime Specification, Agent Runtime
  Specification all touch the propagation chain in question), meeting
  `ARCHITECTURE_DECISIONS.md` §7's "multiple specifications depend on
  it" criterion in principle.

**Evidence against / reasons not to adopt yet:**
- `ARCHITECTURE_DECISIONS.md` §7 itself states an explicit two-data-point
  standard for exactly this kind of pattern-recognition case ("This is
  evidenced twice, independently, but... if a third or fourth
  specification independently repeats it, it would meet Section 7's
  criteria and should be added at that point, rather than being adopted
  now on two data points alone" — stated there for a narrower
  data-modelling convention, but it is the document's own explicit
  bar for this category of candidate). The propagation-explicitness
  pattern currently has exactly two data points, matching that same
  threshold.
- The proposed title ("Runtime Data Propagation Must Be Explicit") is
  broader than the two instances actually evidence — both instances are
  specifically about a specification's own *prose* anticipating a
  capability its formal field list omitted, not about data propagation
  in general. A future AD-017, if adopted, should be scoped to what is
  actually evidenced rather than the broader framing, unless a third
  instance independently broadens it.
- AD-011 (Context Is Reference-Based) already governs a related but
  distinct concern (how each runtime's own transient Context is built);
  the proposed AD-017 would govern a different concern (how a concrete
  value needed by a *later* contract object gets there at all). These
  are not redundant, but the boundary between them would need to be
  drawn carefully if AD-017 is ever adopted.

**Recommendation:** Do not adopt AD-017 now. Track it. If a third
independent instance of the same pattern (a specification's prose
anticipating a capability its own field list omits) occurs in Sprint 2
— plausible candidates include any Task Manager disposition beyond
Accept, or any Agent Runtime command beyond START — adopt it at that
point, scoped narrowly to the actually-evidenced pattern rather than
the broader "propagation" framing.

---

## Implementation Gap Review

**Closed gaps:**

- **#32 — "No concrete `Tool` implementation exists to actually
  invoke."** Closed by Unit 11A. `DefaultExecutionPipeline` now obtains
  the bound `Tool` via `ToolInvocationBinding.invocableFor` and calls
  `validate()`/`execute()`; a `SUCCESS` result means a Tool actually
  ran. Confirmed by direct reading of
  `src/runtime/DefaultExecutionPipeline.kt` and
  `tests/runtime/DefaultExecutionPipelineTest.kt`. **Stale:** #32's own
  `**Status:**` line and its listing under "Deliberate scope boundaries"
  in the Phase 2 Gap Closure Summary table both still describe the
  pre-11A state.

**Remaining gaps (confirmed unchanged by Sprint 1 — none of these were
in Sprint 1's scope, and none were touched):**

- **#25** — `PermissionEngine` policy remains fake/test-only
  (`FakePermissionEngine`); no authorisation policy is specified
  anywhere.
- **#26** — `EventBus` authentication/signature verification remain
  placeholders (`AllowAllPrincipalAuthenticator`; presence-only
  signature check).
- **#35** — Cascading revocation on Principal Revoke is not implemented;
  requires a human decision on the exact rule.
- **#36** — `PrincipalLifecycleTransitions` permits only the literal
  linear chain; no direct `Active → Revoked` or `Suspended → Active`
  edge; requires a human decision.
- **#37** — `resolve()` does not suppress Revoked/Archived Principals;
  requires a human decision.
- **#39** — `identity.*` event publishing is not implemented.
- **#40** — `PermissionEngine.evaluate` is not wired to resolve identity
  first (see AD-007 above — same gap, higher real-world stakes now that
  Tool execution is real).

**New proposed gap:**

- **New — `ToolInvocationBinding.invocableFor` (and
  `ToolRegistry.resolve`, which has the identical, older instance of the
  same gap) restrict callers to "Execution-Pipeline-only" by
  documentation convention only, not by construction.** Confirmed: no
  caller-identity check, no reduced visibility modifier, no access
  control of any kind exists on either method. Confirmed distinct from
  #32 (#32 was "no Tool exists to invoke at all"; this is "the one path
  that does invoke a Tool is not structurally restricted to the one
  caller architecturally allowed to use it"). Confirmed distinct from
  #23 (Principal-scoped *discovery* visibility filtering — a different
  concern from *invocation* access) and #24 (registration/lifecycle-change
  permission gating — also different). No existing gap number covers
  this specific claim; confirmed by direct search of
  `IMPLEMENTATION_GAPS.md` for the phrase this restriction is
  consistently described with ("Execution-Pipeline-only"). **This gap
  should be opened as a new, separately-numbered entry, not folded into
  #32.** `SPRINT_1_VERTICAL_SLICE_PLAN.md` §7's own acceptance criterion
  ("true by construction... not merely true by convention") is the
  specification-level source already naming this as an open bar not yet
  met.

---

## Documentation Updates Required

Listed only — none edited by this document:

1. `docs/architecture/IMPLEMENTATION_GAPS.md` — update #32's Status line
   to reflect closure (Unit 11A / commit `13c9322`); move its listing in
   the Phase 2 Gap Closure Summary table from "Deliberate scope
   boundaries" to "Resolved."
2. `docs/architecture/IMPLEMENTATION_GAPS.md` — add the new gap entry
   recommended above (`ToolInvocationBinding`/`ToolRegistry.resolve`
   access enforcement), with its own number, distinct from #32.
3. `docs/architecture/ARCHITECTURE_DECISIONS.md` — AD-003: add
   `ToolInvocationBinding` to Evidence/Affected specifications (pending
   item 6 below).
4. `docs/architecture/ARCHITECTURE_DECISIONS.md` — AD-005: update Future
   Considerations to reflect that Task Proposal intake and the
   disposition mechanism are now implemented (accept-only).
5. `docs/architecture/ARCHITECTURE_DECISIONS.md` — AD-006: update Future
   Considerations to reflect that `AgentRunCommand` now has a real
   construction/consumption path (START only).
6. `docs/architecture/ARCHITECTURE_DECISIONS.md` — AD-007: amend Future
   Considerations to note gap #40's increased real-world priority
   post-Unit-11A, without changing the gap itself.
7. `docs/architecture/ARCHITECTURE_DECISIONS.md` — AD-009: clarify the
   Decision text (or add an explanatory paragraph) distinguishing
   lifecycle publication, event observation, and audit reconstruction,
   citing `EventCollector` as evidence the distinction already exists in
   implementation.
8. `docs/architecture/ARCHITECTURE_DECISIONS.md` — AD-013: add an
   implementation note naming the "prose anticipates a capability its
   own field list omits" case, citing `AgentRunCommand` and
   `TaskProposal.resourceReferences` as its two instances.
9. `docs/architecture/ARCHITECTURE_DECISIONS.md` — AD-015: add the three
   Unit 11A `FAILED`-not-`DENIED` cases to Evidence.
10. `docs/architecture/ARCHITECTURE_DECISIONS.md` — AD-016: add the
    three new Sprint 1 lifecycle machines to Evidence.
11. `docs/specifications/volume-06-planner-runtime/PlannerRuntimeSpecification.md`
    §10 ("Proposal Model") — add a Resource references field entry,
    citing §9's own existing language, so the specification matches
    `TaskProposal.kt`.
12. **New Volume 3 specification document for `ToolInvocationBinding`**
    — none currently exists (confirmed by direct search); it exists
    today only as a Kotlin contract with its own KDoc. `AD-013`'s own
    "backfill an already-built, already-tested component" pattern
    (`ToolRegistry.md`'s precedent) applies directly here.
13. `docs/implementation/IMPLEMENTATION_HISTORY.md` — already current
    through Unit 11B as of this reconciliation pass; no further action.

---

## Readiness for Architecture v1.1

**Architecture v1.1 can be produced once, and only once, the
documentation updates above are applied** — this reconciliation
identifies exactly what v1.1 needs to say differently from v1.0; it does
not itself constitute v1.1.

No blocking disagreement exists between Sprint 1's implementation and
any of the 16 existing Architecture Decisions. Every decision remains
either directly validated or requires only additive
evidence/clarification, never replacement. The two implementation gap
actions (closing #32, opening the new `ToolInvocationBinding` access
gap) and the thirteen documentation updates listed above are
editorial and additive — none require new Kotlin, and none contradict
Sprint 1's own test results.

**Recommendation:** proceed to produce `architecture-v1.1` once items 1
through 13 above are applied. The AD-017 candidate should remain
tracked, not adopted, pending a third independent instance of its
underlying pattern.

---

## Files Created

- `docs/architecture/PARKER_ARCHITECTURE_V1_1_AD_RECONCILIATION.md`

## Files Modified

- None.
