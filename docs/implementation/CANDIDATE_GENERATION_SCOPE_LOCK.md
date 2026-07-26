# Plan Candidate Generation — Scope Lock

## Status

**Binding, frozen Scope Lock for a future implementation Unit.** Companion
to `docs/architecture/PLAN_CANDIDATE_GENERATION_GOVERNANCE_REVIEW.md`
(repository review and readiness determination) and
`docs/architecture/CANDIDATE_GENERATION_CONTRACT_DESIGN.md` (approved —
the type-level design this Scope Lock freezes). The frozen contract,
information boundary, output semantics, and file boundary below are
binding terms a future implementation must satisfy exactly, not redesign.

**This document authorises no implementation.** Producing this Scope Lock
is the final deliverable of this governance/design pass. No Kotlin is
written, staged, committed, or pushed as part of this Unit's own work.

---

## 1. Responsibilities — What This Unit Owns

This Unit owns exactly two things:

- **The `PlanCandidateGenerator` interface** — one method, one input, one
  output type, freezing the information boundary
  `CANDIDATE_GENERATION_CONTRACT_DESIGN.md` Sections 1–4 already settled.
- **Exactly one concrete implementation of it** —
  `DefaultPlanCandidateGenerator`, a deliberately non-deliberative,
  verbatim reference implementation, authorised in Section 3 below because
  the repository already provides enough settled policy to define it
  honestly, without fabricating decomposition intelligence this project
  has never authorised anywhere.

**Nothing beyond this.** This Unit does not call `PlannerRuntime.plan()`,
does not wire anything into production, does not revise
`GoalPlanningHandoffCoordinator` or any other existing contract, and does
not design Planning Context. It adds one new interface and one new,
honestly-limited implementation of it — nothing else exists to own.

---

## 2. Frozen Contract (Included)

### 2.1 The Interface

```kotlin
interface PlanCandidateGenerator {
    suspend fun generate(
        request: PlanningRequest,
    ): List<PlanCandidate>
}
```

- **Name:** `PlanCandidateGenerator`.
- **Location:** `src/contracts/PlanCandidateGenerator.kt` — a new file,
  not an addition to `src/contracts/PlanDecision.kt`.
- **Exactly one method.** No overload, no second operation.
- **Exactly one input: the complete `PlanningRequest`.** No decomposition
  into separate `goal`/`correlationId`/`initiatingPrincipalId` parameters.
- **No default arguments, anywhere on this signature.**
- **No additional context parameter of any kind** — Section 2.2 below is
  the binding, exhaustive statement of what this method may and may not
  receive.
- **An empty `List<PlanCandidate>` is a valid, non-exceptional result.**
  It is never treated by this interface, or by any implementation of it,
  as an error.
- **Exceptions represent genuine generator faults only** — a real
  implementation failure (timeout, crash, malformed upstream output),
  never "I could not decompose this Goal," which is the empty-list case
  above, not a fault.
- **`suspend`, for forward-compatibility only** — identical reasoning to
  `PlanDecision.decide` and `ReasoningProvider.reason`: a future
  model-backed or human-in-the-loop implementation would need to suspend;
  declaring this now avoids a breaking signature change later.

### 2.2 Frozen Information Boundary

**Candidate Generation may receive only `PlanningRequest`.** Nothing else,
under any circumstance, in this Unit or any future one without a Scope
Lock revision of its own.

**It must not directly receive:**

| Prohibited | Why |
| --- | --- |
| `Turn` | `ConversationEngine`-owned; direct access would violate AD-011 (Context Is Reference-Based). |
| `InboundOwnerMessage` | Superseded entirely by `PlanningRequest`, which already carries everything from it this boundary needs (`initiatingPrincipalId`, `correlationId`). |
| `ReasoningContext` | Reasoning's own, already-consumed, differently-owned artifact (Contract Design Section 3) — not Planning Context, and never a substitute for it. |
| Conversation History | `ConversationEngine`/Reasoning-owned; not needed to decompose an already-identified Goal. |
| Memory Source | No Memory read path exists anywhere in the planning boundary; not introduced here (`PlannerRuntimeSpecification.md` Section 3 Non-Goals). |
| World Model Source | Identical reasoning to Memory Source. |
| Runtime metadata | No such contract concept exists in this codebase; not invented here. |
| `IdentityService` | Identity resolution remains exclusively `PlannerRuntime.plan()`'s job; Candidate Generation performs no resolution or status check. |
| Authenticated Principal objects | Only the already-present `PlanningRequest.initiatingPrincipalId` identifier is available — a reference, never a resolved or authenticated object. |
| Permission Engine state | Candidate Generation labels advisory fields only; it never reads or queries permission state. |
| Tool Registry state | No invocable or discoverable Tool reference of any kind. |

**`initiatingPrincipalId`, `correlationId`, Goal text, `source`, and
`priority` are available only through the existing `PlanningRequest`** —
never as separate parameters, never independently sourced.

**`GoalPlanningHandoffCoordinator` remains ignorant of `ReasoningContext`.**
Restated as settled, binding fact from Contract Design Section 3 — not
re-decided, not revisited, and not modified by this Unit in any way.

**No Planning Context is designed or implemented by this Unit.** A future,
independently-owned Planning Context assembler remains entirely deferred
(Contract Design Section 9).

### 2.3 Frozen Output Semantics

**The generator may return zero, one, or multiple candidates.** Each
returned item must be a valid, already-existing `PlanCandidate`
(`src/contracts/PlanDecision.kt`) — no new candidate type, no relaxed or
tightened field validation.

**Candidate Generation owns:**

- Candidate construction (populating `PlanCandidate`'s fields).
- Candidate-specific rationale (`PlanCandidate.rationale`) — explaining
  why *this* decomposition was proposed.
- Assumptions attached through existing fields only (`rationale`,
  `constraints`) — no new Assumption-shaped field or type.
- Deterministic output ordering (Section 2.4).

**Candidate Generation does not own:**

- Ranking or comparison among candidates — exclusively `PlanDecision`'s
  job, unchanged.
- Final selection — exclusively `PlanDecision`/`PlannerRuntime`'s job,
  unchanged.
- Fallback selection rationale — `InMemoryPlannerRuntime.buildProposal`'s
  own existing default-rationale behaviour when a winning candidate's own
  `rationale` is blank; untouched, unduplicated here.
- Authorisation, execution, or task creation — restated, not revisited,
  from the Governance Review's and Contract Design's own Trust Boundary
  sections.

### 2.4 Deterministic Ordering (Binding)

**Output list order must be stable and deterministic for identical inputs
and identical generator configuration.** This is required because
`DefaultPlanDecision` may consume list order as a tie-break rule ("first
valid, non-duplicate candidate in generation order").

**Implementations must not derive output order from an unordered
collection, or from incidental `Map`/`Set` iteration order.** Any ordering
must be an explicit, intentional property of the implementation's own
logic — never an accident of a collection type's iteration behaviour.

### 2.5 Empty-List Semantics (Binding)

**An empty list means only: no candidate was produced for this
`PlanningRequest`.**

**It must not be treated as a structured signal for:**

- Ambiguous Goal.
- Insufficient information.
- Impossible request.
- Unsafe request.
- Conflicting Goals.

**These five distinctions are not represented by this contract and remain
separate, future governance concerns** (Contract Design Section 9). **No
new rejection, refusal, waiting, safety, or failure type may be
introduced by this Unit** to represent any of them.

---

## 3. Concrete Implementation Boundary (Binding)

**A concrete implementation is authorised.** The repository provides
enough settled policy to define one honestly, without fabricating
decomposition intelligence: `PLANNER_RUNTIME_CONTRACT_DESIGN.md`'s own
Type 2 disposition already states candidate generation is "out of scope
for Track D entirely — 'Planner reasoning'/'LLM integration' are
explicitly excluded," and `DeterministicPlannerHarness.kt`'s own
already-accepted, already-precedented behaviour is exactly "a fixed,
non-LLM function producing exactly one Plan Candidate... for one fixed
Goal." This Unit's authorised implementation generalises that same,
already-precedented shape from one fixed test Goal to any well-formed
`PlanningRequest.goal` — it does not invent new behaviour, it extends an
already-approved pattern to production.

### 3.1 Concrete Class Name

**`DefaultPlanCandidateGenerator`** — matching this codebase's own
established naming convention for the single, non-configurable,
production-quality reference implementation of a contract
(`DefaultPlanDecision`, `DefaultExecutionPipeline`,
`DefaultReasoningContextAssembler`, `DefaultPermissionEngine`).

### 3.2 Constructor Dependencies

**None. Zero-argument constructor.** No injected ID factory, unlike
`GoalPlanningHandoffCoordinator`'s `planningSessionIdFactory: () -> String`
— a deliberately different, equally precedented pattern, not an
inconsistency. `GoalPlanningHandoffCoordinator` needed an injected factory
because `PlanningSessionId` has **no existing parent identifier** to
derive from and must be freshly, unpredictably minted. `PlanCandidateId`
is the opposite case: `PlanningRequest.planningSessionId` already exists
by the time `generate()` is called, and this codebase already has an
established, accepted convention for deterministically deriving a child
identifier from an already-existing parent one —
`TaskProposalId("${request.planningSessionId.value}-proposal-1")`
(`InMemoryPlannerRuntime.buildProposal`) and
`PlanCandidateId("$planningSessionId-candidate-1")`
(`DeterministicPlannerHarness.run`), both already in production or
test-precedent use, both justified by
`docs/architecture/PRE_MODULE_ID_MULTIPLICITY_DECISION.md` and
`IMPLEMENTATION_GAPS.md` #48. No randomness, and therefore no injected
factory, is needed here.

### 3.3 Exact Candidate-Production Policy

**Always produces exactly one candidate. Never zero. Never more than
one.** For any well-formed `PlanningRequest` (already guaranteed non-blank
on `goal` and `correlationId` by that type's own `init`):

- `planCandidateId`: deterministically derived (Section 3.4).
- `goal`: `request.goal`, copied verbatim, unmodified.
- `rationale`: a fixed, non-fabricated literal disclosing exactly what
  this implementation does and does not do — see Section 3.7. Never
  derived from, or claiming insight into, the Goal's own content.
- Every other optional field (`riskEstimate`, `requiredCapabilities`,
  `anticipatedPermissionActions`, `constraints`, `dependencies`,
  `contextReferences`, `resourceReferences`, `expectedOutputs`) is left at
  its own existing type-level default. **No value is invented for any of
  them** — `PlanningRequest` carries no information that would legitimately
  populate any one, and inventing content here is exactly the fabrication
  this Scope Lock exists to prevent.

**This is a deliberately narrow, honestly-disclosed reference
implementation, not broad deliberative intelligence.** It performs no
decomposition, no interpretation, and no judgment — it echoes the
Planning Request's own Goal text back as its only candidate. This is a
disclosed limitation of `DefaultPlanCandidateGenerator` specifically, not
a limitation of the `PlanCandidateGenerator` contract itself, which
remains open to a future, richer implementation.

### 3.4 Candidate ID Minting

```kotlin
PlanCandidateId("${request.planningSessionId.value}-candidate-1")
```

Deterministic, parent-derived from `request.planningSessionId.value`
(itself already guaranteed non-blank by `PlanningSessionId`'s own `init`
check) — mirroring `TaskProposalId`'s and `DeterministicPlannerHarness`'s
own identical minting shape exactly. Because this implementation always
produces exactly one candidate, no uniqueness concern arises within a
single `generate()` call; `PlanCandidateId`'s own contract does not
require cross-call uniqueness (`PlanDecision` enforces uniqueness only
within one evaluation, per that type's own KDoc).

### 3.5 Deterministic Ordering Rule

Trivial, by construction: the returned list is `listOf(candidate)`,
constructed directly from a single, freshly-built value — never derived
from a `Map`, a `Set`, or any other unordered collection. Two calls with
identical `request` values produce list-equal results (content-equal,
not necessarily reference-equal).

### 3.6 When It Returns Empty

**Never, for this implementation, by design.** `DefaultPlanCandidateGenerator`
has no ambiguity-detection, no insufficient-information detection, and no
safety-refusal logic — building any of those would require exactly the
decomposition intelligence this Unit is not authorised to invent
(Section 3.3). This is an explicit, disclosed limitation of this specific
implementation: the interface's own empty-list path (Section 2.5) exists
for a future, more capable implementation to exercise, not for this one.

### 3.7 Assumptions and Rationale

`DefaultPlanCandidateGenerator` produces exactly one fixed, non-caller-
supplied rationale string per candidate — analogous in spirit to
`GoalPlanningHandoffCoordinator`'s own `DEFERRAL_DETAIL` constant — stating
plainly that this candidate is a verbatim, undecomposed treatment of the
Planning Request's own Goal text, and that no decomposition policy exists
in this repository yet. It records no per-request Assumption beyond this
fixed disclosure, since it has no interpretive basis to form one —
inventing a request-specific Assumption would itself be fabrication.

### 3.8 No Blocker

**No unresolved architectural blocker exists for this concrete
implementation.** The candidate-production policy, ID minting scheme, and
empty-list behaviour above are each derived directly from already-settled
repository precedent (Type 2's ownership disposition,
`DeterministicPlannerHarness`'s own shape,
`TaskProposalId`'s minting convention) — none is invented for this Unit.

---

## 4. Integration Exclusions (Binding)

The Scope Lock prohibits, without exception, absent a stop-and-report on a
genuine blocker:

- **Revising `PlanningRequest`.** Used exactly as already approved.
- **Revising `PlanCandidate`.** Used exactly as already approved,
  including its already-disclosed lack of a `Confidence` field — not
  added here.
- **Revising `PlanDecision`.** Unchanged; already consumes exactly the
  `List<PlanCandidate>` shape this Unit's interface produces.
- **Revising `PlannerRuntime`.** Unchanged; `plan(request, candidates)`'s
  signature already accepts an externally-produced list.
- **Revising `GoalPlanningHandoffCoordinator`.** Not touched, in any form.
- **Calling `PlannerRuntime.plan()`.** No code this Unit introduces calls
  `plan()`, under any input.
- **Wiring candidate generation into production.** No production file
  (`ParkerRuntime.kt` or any other) constructs, references, or imports
  `PlanCandidateGenerator` or `DefaultPlanCandidateGenerator`.
- **Wiring `PlannerRuntime` into production.** No `PlannerRuntime` or
  `InMemoryPlannerRuntime` instance is constructed anywhere by this Unit.
- **Wiring `TaskManagerRuntime` into production.** Identical prohibition.
- **Creating tasks.** No `Task`, `TaskProposal`, or `TaskProposalIntake`
  interaction of any kind.
- **Invoking tools.** No `Tool`, `ToolRegistry`, or `ToolInvocationBinding`
  interaction of any kind.
- **Adding permissions or authentication.** No `PermissionEngine`,
  `PermissionPolicy`, or `IdentityService` dependency introduced anywhere.
- **Adding `ReasoningContext` access.** `PlanCandidateGenerator`'s sole
  input remains `PlanningRequest`; no method, overload, or future default
  parameter introduces `ReasoningContext` access in this Unit.
- **Designing Planning Context.** Deferred (Contract Design Section 9);
  not designed, not stubbed, not partially implemented here.
- **Altering Chapter 23 Goal Manager.** Not read as authority, not cited,
  not implemented against, not modified.
- **Modifying `ResponseComposer`.** Zero changes, in any form.
- **Adding persistent memory.** No relation to, dependency on, or
  interaction with `MemoryStore`, `MemorySource`, or any persistence
  mechanism.
- **Touching voice or Home Assistant integration.** No relation to,
  dependency on, or interaction with either.

---

## 5. Test Obligations (Binding)

**New: `tests/runtime/DefaultPlanCandidateGeneratorTest.kt`**

- **Contract signature.** `DefaultPlanCandidateGenerator` implements
  `PlanCandidateGenerator`; `generate` accepts exactly one
  `PlanningRequest` parameter.
- **One-candidate result.** For a representative, well-formed
  `PlanningRequest`, `generate` returns exactly one `PlanCandidate`.
  **Zero- and multiple-candidate results are not exercised by this test
  file** — explicitly disclosed as not supported by this concrete policy
  (Section 3.6), not a coverage gap.
- **Deterministic output for identical input.** Two `generate` calls with
  value-equal `PlanningRequest` arguments produce content-equal
  `PlanCandidate` lists.
- **Stable ordering.** The returned list is not constructed from, or
  dependent on, any unordered collection — verified by inspection of the
  implementation alongside a repeated-call equality assertion.
- **Valid field construction.** `planCandidateId` is non-blank and
  correctly derived from the specific `planningSessionId` supplied;
  `goal` equals the specific `PlanningRequest.goal` supplied, verbatim,
  for at least two distinct input Goals (proving the value is read from
  the input, not hardcoded).
- **Preservation of `PlanningRequest` provenance.** Two requests with
  distinct `planningSessionId` values produce two candidates with
  distinct, correctly-derived `planCandidateId` values.
- **Exception propagation for genuine generator faults: not applicable to
  this implementation, explicitly disclosed, not silently skipped.**
  `DefaultPlanCandidateGenerator.generate` cannot throw for any
  well-formed `PlanningRequest` — provable by construction, since
  `PlanningRequest.planningSessionId.value` is already guaranteed
  non-blank before `generate` is ever called, making the derived
  `PlanCandidateId` construction above incapable of failing its own
  non-blank check. No fault-injection test is fabricated for a method
  that has no real fault surface.
- **Structural proof of no prohibited dependencies.** `DefaultPlanCandidateGenerator`'s
  constructor accepts zero parameters — the strongest possible structural
  guarantee, stronger than a minimum-dependency list, that it holds no
  reference to `IdentityService`, `EventBus`, `MemorySource`,
  `WorldModelSource`, `ReasoningContext`, `PermissionEngine`,
  `ToolRegistry`, `PlannerRuntime`, or `TaskManagerRuntime`.
- **Structural proof it does not call `PlannerRuntime`, `TaskManagerRuntime`,
  tools, permissions, or execution services.** Reinforces the
  zero-dependency proof above: with no such reference reachable from the
  class's own constructor or fields, no call to any of them is possible.

No test file beyond this one is created or modified by this Unit.

---

## 6. File Boundary (Binding)

**Exactly these files may be created by the implementation this Scope
Lock authorises. No existing production file may be modified.**

**New:**

- `src/contracts/PlanCandidateGenerator.kt` — `PlanCandidateGenerator`.
- `src/runtime/DefaultPlanCandidateGenerator.kt` — `DefaultPlanCandidateGenerator`.

**Test files (only this one; Section 5):**

- `tests/runtime/DefaultPlanCandidateGeneratorTest.kt` (new).

**Documentation (directly necessary only):**

- `docs/implementation/IMPLEMENTATION_HISTORY.md` — one new entry,
  mirroring every prior Unit's own entry format.
- `docs/architecture/IMPLEMENTATION_GAPS.md` — reconciliation only if this
  Unit genuinely narrows a recorded gap. Candidate: Gap #53's own
  Reasoning-to-Planning Handoff update already records "No production
  `PlanCandidate` generation exists" as open; a production-quality
  (though deliberately non-deliberative), unwired `PlanCandidateGenerator`
  implementation narrows that specific clause honestly (an implementation
  now exists; it remains unwired) without closing the gap as a whole. No
  other gap entry is touched.

**No existing production file requires modification, and none is
identified as needing one.** `PlanDecision.kt`, `InMemoryPlannerRuntime.kt`,
`GoalPlanningHandoffCoordinator.kt`, `ConversationOutcome.kt`,
`ParkerRuntimeOutcome.kt`, and `ParkerRuntime.kt` are all used exactly as
they already exist, unmodified. This Unit is purely additive.

**No other file — production, test, or documentation — may be created,
modified, or deleted by this Unit.**

---

## 7. Governing Principle

**This Unit produces candidates. It decides nothing among them,
authorises nothing, and executes nothing.**

`DefaultPlanCandidateGenerator` can only ever produce one shape of value:
a single-element `List<PlanCandidate>` whose sole entry echoes its own
input Goal back, unchanged, labelled honestly as undecomposed. It
originates no plan, no task, no execution, and no permission decision. If
a future, richer generator's judgment and this implementation's own
verbatim output ever appear to disagree about what a Goal "really" means,
neither is authoritative on its own — `PlanDecision`'s own, unchanged
selection logic, and `PlannerRuntime`'s own, unchanged lifecycle, remain
the only components that ever decide anything among whatever candidates
either generator supplies.

---

## 8. Ownership

- **No production owner exists after this Unit, by design.** This Scope
  Lock explicitly excludes wiring `PlanCandidateGenerator` or
  `DefaultPlanCandidateGenerator` into `ParkerRuntime.kt` or any other
  production composition root (Section 4). Both new types exist,
  compile, and are tested, but are constructed only by
  `tests/runtime/DefaultPlanCandidateGeneratorTest.kt` until a future,
  separately-chartered Unit wires them in.
- **No production caller exists after this Unit.** Nothing calls
  `PlanCandidateGenerator.generate()` from any production code path.

---

## 9. Lifetime and Threading

- **Construction.** `DefaultPlanCandidateGenerator()` — no arguments, may
  be constructed freely, anywhere, at any time; carries no state that
  requires singleton discipline.
- **Use.** Fully stateless, per call. Each `generate` call is independent
  and pure with respect to its own input.
- **Sharing.** Safe to share a single instance across arbitrarily many
  concurrent callers, since it holds no mutable state of its own to
  guard — though this Unit does not construct any such shared instance in
  production (Section 8).
- **Coroutine expectations.** `generate` is `suspend`-declared, for
  forward-compatibility only (Section 2.1); `DefaultPlanCandidateGenerator`'s
  own body never suspends.

---

## 10. Relationship to the Constitution

`docs/architecture/parker-constitution.md`: "Parker owns authority.
Modules provide capability" and "Cognition proposes. Trust authorises.
Runtime executes." `PlanCandidateGenerator` and
`DefaultPlanCandidateGenerator` sit entirely within Cognition — they
propose candidates, never authorise, never execute. Every advisory field
either might populate (`riskEstimate`, `anticipatedPermissionActions`)
remains labelling only, exactly as `PlannerRuntimeSpecification.md`
Section 8 already requires; neither type checks, grants, or bypasses
permission at any point.

---

## 11. Acceptance of This Scope Lock

This Scope Lock is binding once accepted. A future implementation Unit
authorised against it must satisfy the frozen contract (Section 2), the
concrete implementation boundary (Section 3), and the file boundary
(Section 6) exactly, must not implement anything in the Integration
Exclusions (Section 4), and must treat any discovered need to exceed
either as grounds to pause and request a Scope Lock revision — not as
licence to proceed under this one.
