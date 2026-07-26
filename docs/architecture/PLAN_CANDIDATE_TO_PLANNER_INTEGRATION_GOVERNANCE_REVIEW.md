# Plan Candidate to PlannerRuntime Integration — Governance Review

## Status

**Governance review only. No Kotlin, no interfaces, no contracts, no
tests, no dependency or build changes exist anywhere in this document or
arise from it.** Nothing under `src/` or `tests/` is touched. This
document does not begin Contract Design — it determines whether Contract
Design may begin, and on what terms.

**Repository position.** Branch `main`. Latest verified commit includes
`PlanCandidateGenerator`, `DefaultPlanCandidateGenerator`, and the three
governance/design/scope-lock documents for Candidate Generation. Native
Gradle verification passed; the repository is clean. Every prior
Governance Review, Contract Design, and Scope Lock is treated as settled
architecture and is not reopened here.

**Scope, restated exactly as chartered.** The correct architectural
method for introducing the newly implemented `PlanCandidateGenerator`
into the existing planning pipeline: `PlanningRequest → PlanCandidateGenerator
→ List<PlanCandidate> → PlannerRuntime.plan(...)`. This is not execution,
not task management — purely the transition from candidate generation to
plan selection.

---

## 1. Repository Review (fresh reads, this Unit)

Read directly, at the level cited, immediately before this document was
written:

- `src/runtime/GoalPlanningHandoffCoordinator.kt` — re-read in full.
  Exactly one constructor dependency, `planningSessionIdFactory: () ->
  String`. One method, `initiatePlanning(originalMessage, goal):
  GoalPlanningHandoffOutcome`, which constructs a `PlanningRequest` and
  always returns `GoalPlanningHandoffOutcome.Deferred`. Its own KDoc
  states plainly: "a future, separately-governed Unit -- which must also
  supply legitimate `PlanCandidate` generation and production composition
  wiring -- is the one that adds a `PlannerRuntime` dependency here and
  replaces `[initiatePlanning]`'s body with a real `plan()` call." That
  Unit is this one, now that `PlanCandidateGenerator` exists.
- `src/contracts/PlanCandidateGenerator.kt`, `src/runtime/DefaultPlanCandidateGenerator.kt`
  — re-read in full. `generate(request: PlanningRequest): List<PlanCandidate>`,
  `suspend`, sole input `PlanningRequest`. `DefaultPlanCandidateGenerator`
  always returns exactly one candidate, echoing `request.goal` verbatim,
  and cannot throw for any well-formed `PlanningRequest`.
- `src/contracts/PlanDecision.kt` — `PlannerRuntime.plan(request:
  PlanningRequest, candidates: List<PlanCandidate>): PlanningSessionResult`
  re-confirmed unchanged. `PlanningRequest`'s own field shape unchanged.
- `src/runtime/InMemoryPlannerRuntime.kt` — re-read in full, specifically
  its constructor: `class InMemoryPlannerRuntime(private val
  identityService: IdentityService, private val eventBus: EventBus,
  private val taskProposalIntake: TaskProposalIntake, private val
  planDecision: PlanDecision = DefaultPlanDecision())`. **`taskProposalIntake:
  TaskProposalIntake` has no default -- it is mandatory.** `plan()` calls
  `taskProposalIntake.submitProposal(proposal)` whenever `PlanDecision`
  selects a winner (already-implemented, already-approved behaviour,
  unchanged).
- `src/runtime/InMemoryTaskManagerRuntime.kt` — confirmed, by grep, the
  **sole** existing implementation of `TaskProposalIntake` anywhere in
  `src/`.
- `src/runtime/ConversationOutcome.kt`, `src/composition/ParkerRuntimeOutcome.kt`,
  `src/composition/ParkerRuntime.kt` — re-confirmed unchanged since the
  Reasoning-to-Planning Handoff: `ConversationOutcome.PlanningDeferred(outcome:
  GoalPlanningHandoffOutcome)`, `ParkerRuntimeOutcome.PlanningDeferred(outcome:
  GoalPlanningHandoffOutcome)`, both single-purpose around the `Deferred`
  concept. `ParkerRuntime.kt`'s composition root still constructs no
  `PlannerRuntime`, `InMemoryPlannerRuntime`, `TaskManagerRuntime`, or
  `InMemoryTaskManagerRuntime` anywhere (confirmed by grep, unchanged).
- `docs/specifications/volume-06-planner-runtime/PlannerRuntimeSpecification.md`
  Section 6 — re-read: "The Task Manager Runtime Specification did not
  define a Task Proposal intake operation... now closed... implemented...
  `InMemoryTaskManagerRuntime.kt`... is the first implementation of
  `TaskProposalIntake`." Confirms the coupling above is original,
  approved architecture, not an accident of this Review's own reading.

### Findings not previously recorded this precisely

**Finding 1 — this is exactly the Unit `GoalPlanningHandoffCoordinator`'s
own KDoc already named.** No architectural surprise exists at the level
of "should something call `plan()` now" — that was always the plan. What
remained genuinely undecided (Candidate Generation Contract Design
Section 9, "Placement of invocation") is *which* component does it, and
that is this Review's primary job.

**Finding 2 — `PlannerRuntime` cannot be constructed, in production or in
a real integration test, without also supplying a working
`TaskProposalIntake`, and `InMemoryTaskManagerRuntime` is the only one
that exists.** This is a sharper, more precise restatement of a
previously-disclosed "soft dependency" (Reasoning-to-Planning Handoff
Governance Review, Section 8): that document characterised
`PlannerRuntime`/`TaskManagerRuntime` wiring as two *independently*
additive changes. They are not independent — `InMemoryPlannerRuntime`'s
constructor makes `TaskProposalIntake` a mandatory, non-default parameter.
**Any future Unit that wires a real `PlannerRuntime` into production
necessarily wires a real `TaskManagerRuntime`-shaped `TaskProposalIntake`
at the same time — there is no smaller increment.** This Review's own
"do not introduce `TaskManagerRuntime`" exclusion therefore also,
transitively, forecloses constructing a working production `PlannerRuntime`
in this Unit, even though that exclusion was not separately named.
Disclosed here in full precision; not resolved, since resolving it is
production-wiring work, out of this Review's scope and the scope of the
Contract Design it recommends.

**Finding 3 — the "Deferred" concept this repository already shipped
becomes unreachable, not merely narrowed, once this integration is
implemented.** `GoalPlanningHandoffOutcome.Deferred`'s sole reason,
`PlanningDeferralReason.CANDIDATE_GENERATION_UNAVAILABLE`, is true today
because no `PlanCandidate` generator exists. It is no longer true: one
exists. Once the orchestrating coordinator actually calls
`PlanCandidateGenerator.generate()` and `PlannerRuntime.plan()`
unconditionally (Section 4 below), no code path remains that would
legitimately produce `Deferred` for this reason — `PlannerRuntime.plan()`
already, correctly, turns an empty candidate list into
`PlanningSessionResult.Failed`, which is a different, more honest outcome
than "deferred." This is a real, disclosure-worthy consequence of
incremental architecture, not a defect in any prior Unit: each Unit's own
documentation already predicted this ("a future `Submitted`-shaped
variant... is a distinct, separately-governed addition" —
`GoalPlanningHandoffCoordinator`'s own KDoc). Section 5 and the Unresolved
Questions section address what to do about it; this Review does not
decide it.

---

## 2. Question 1 — Ownership

### 2.1 Candidates evaluated

**`PlannerRuntime` itself.** Rejected. This would require removing the
`candidates` parameter from `plan()` and having `InMemoryPlannerRuntime`
call a `PlanCandidateGenerator` internally — directly reversing the
already-settled, multiply-reaffirmed architectural decision
(`PLANNER_RUNTIME_CONTRACT_DESIGN.md` Type 6's own correction; AD-010
Model Independence; the Candidate Generation Governance Review's own
Section 3.1) that candidate generation must remain external to
`PlannerRuntime`. Not "absolutely necessary" (this Review's own governing
constraint) — the current signature already accepts an externally-supplied
list, which is precisely what this integration needs to finally provide.

**A new, separate coordinator inserted between `GoalPlanningHandoffCoordinator`
and `PlannerRuntime`.** Considered. Would leave `GoalPlanningHandoffCoordinator`
unchanged (still only constructing a `PlanningRequest` and returning
`Deferred`) and introduce a second, new component to do generation +
invocation. Rejected as the recommended option: it would require
`GoalPlanningHandoffCoordinator` to still return something
(`Deferred`, unconditionally) that a second coordinator would then have
to override or ignore to proceed — misrepresenting a real attempt as
"deferred" first, then un-deferring it, which is strictly less honest
than not producing `Deferred` at all when a real attempt is about to
happen. It would also duplicate `PlanningRequest`-construction-adjacent
responsibility across two coordinators for no benefit, and add a fourth
link to an already four-deep coordinator chain (`ConversationReplyCoordinator
→ GoalPlanningHandoffCoordinator → [new] → PlannerRuntime`) where three
links already exist and precedent supports extending the existing one
instead (below).

**`ConversationReplyCoordinator` absorbing this responsibility directly**
(eliminating `GoalPlanningHandoffCoordinator` as a distinct concept).
Considered. Rejected: `ConversationReplyCoordinator` already holds three
dependencies precisely to keep its own responsibility narrow (routing
`Reply`/`Goal`/`NoAction`), delegating `Goal`-handling to a dedicated
coordinator so its own dependency list does not grow with every future
Planning-side capability. Absorbing candidate generation and planner
invocation into it would balloon its own dependency list (five, including
the ID factory) and retire a component whose narrower scope is exactly
what let this Review reason about it in isolation. No existing precedent
in this codebase collapses two purpose-built coordinators together once
both already exist.

**`GoalPlanningHandoffCoordinator`, revised in place. Recommended.**

- Its own KDoc already names this exact evolution as its own future, not
  a hypothetical: "replaces `[initiatePlanning]`'s body with a real
  `plan()` call."
- The dependency-count objection does not hold: `ConversationReplyCoordinator`
  already has three dependencies; a revised `GoalPlanningHandoffCoordinator`
  holding `planningSessionIdFactory`, `planCandidateGenerator`, and
  `plannerRuntime` (three) is squarely within this codebase's own
  established range, not an outlier.
- Precedent already exists in this codebase for a coordinator-shaped class
  holding a top-level executive component directly: `ResponseDelivery`
  already holds `ExecutionPipeline` directly
  (`ResponseDelivery(resourceRegistry, executionPipeline)`).
  `PlannerRuntime` is the same *kind* of dependency.
- The "structural guarantee" this class's own KDoc currently asserts
  ("the absence of any other constructor parameter is itself the
  structural guarantee that this class cannot reach `PlannerRuntime`")
  is not violated by revision -- it is *retired and replaced*, exactly the
  same pattern `ConversationReplyCoordinator` itself already underwent
  once (its own prior "cannot reach Planning" guarantee was retired when
  it gained `goalPlanningHandoffCoordinator`, replaced by "reaches
  Planning only through a coordinator that itself cannot invoke `plan()`
  directly"). Each layer's guarantee moving one level outward as the
  pipeline is built out is this codebase's own established, healthy
  pattern, not a regression.
- The method itself already performs one "sequence" (construct request,
  return outcome); extending that same method to sequence three steps
  (construct → generate → plan) is an in-kind extension of the same
  responsibility, not a bolt-on of an unrelated second one.

**Recommendation: revise `GoalPlanningHandoffCoordinator` in place.** Its
exact name and outcome-type shape are Contract Design questions (Section
9, Unresolved Questions) — this Review settles only that the same class
is the correct owner, not a new one.

---

## 3. Question 2 — PlannerRuntime Boundary

**`PlannerRuntime` should continue receiving `List<PlanCandidate>`
exactly as it does today. The existing contract remains fully
sufficient.** No revision to `plan(request: PlanningRequest, candidates:
List<PlanCandidate>): PlanningSessionResult` is required or motivated by
this integration.

This directly answers the report's own explicit question: **yes,
`PlannerRuntime` can be integrated without modifying its public
contract.** The signature was already, deliberately, designed for exactly
this moment (`PLANNER_RUNTIME_CONTRACT_DESIGN.md` Type 6: "candidates are
generated by planning, not supplied by callers alongside the request that
starts it" — meaning by an external mechanism, passed in). This
integration is the first Unit to actually supply a real value for that
parameter; it is not the first to need the parameter shaped this way.

No alternative boundary is preferable. Introducing a new intermediate type
(e.g. a wrapper combining `PlanningRequest` and its candidates) would
duplicate `PlanningRequest`'s own already-approved shape for no benefit,
and was already considered and rejected once, in the opposite direction,
when `candidates` was deliberately removed from `PlanningRequest` itself.

---

## 4. Question 3 — Orchestration

- **Who calls `PlanCandidateGenerator`:** the orchestrating coordinator
  (Section 2's recommended `GoalPlanningHandoffCoordinator`), immediately
  after constructing `PlanningRequest`, exactly once per `initiatePlanning`-
  shaped call.
- **Who calls `PlannerRuntime`:** the same coordinator, immediately after
  receiving the generator's result, passing the same `PlanningRequest`
  instance and the candidates list **unconditionally** — including when
  the list is empty. No pre-check, no early return, no special-casing of
  an empty list inside the coordinator: `PlannerRuntime.plan()` already,
  correctly, turns an empty list into `PlanningSessionResult.Failed`
  (Finding 3). Reintroducing a coordinator-level empty-list check would
  duplicate logic `plan()` already owns and would resurrect exactly the
  "deferred" framing this integration retires.
- **Who owns error propagation:** the orchestrating coordinator owns
  none of it, by design — no `try`/`catch` is added, mirroring every
  existing coordinator in this codebase's own "no coordinator catches"
  discipline. A `PlanCandidateGenerator.generate()` fault and a
  `PlannerRuntime.plan()` fault both propagate uncaught, through the
  coordinator, through `ConversationReplyCoordinator` (also no
  `try`/`catch`), to `ParkerRuntime.submitOwnerMessage`'s existing outer
  boundary, surfacing as `ParkerRuntimeOutcome.Failed(PipelineStage.UNKNOWN,
  e)` — the same, already-governed machinery reused unchanged, exactly as
  a blank/throwing `planningSessionIdFactory` already does today.
- **Who owns logging:** unchanged from today — `ParkerRuntime.submitOwnerMessage`,
  not the coordinator. No coordinator in this codebase's existing chain
  holds a logger dependency; `GoalPlanningHandoffCoordinator` does not
  today and should not gain one for this integration. `ParkerRuntime.kt`'s
  existing `when` block, which already logs "Planning initiation
  deferred," is the correct place for whatever the equivalent log line
  becomes once a real result exists.
- **Who owns `PlanningDeferred` outcomes:** the orchestrating coordinator
  remains the sole producer of whatever `GoalPlanningHandoffOutcome`
  value results — this does not change. What changes is *which* value it
  can produce: the `Deferred` variant becomes unreachable in practice
  (Finding 3); a new variant carrying a real `PlanningSessionResult`
  becomes the ordinary case. Exact naming and disposition of `Deferred`
  is a Contract Design question (Unresolved Questions, below) — ownership
  of producing whichever variant exists does not move.

---

## 5. Question 4 — Failure Behaviour (Ownership Only)

- **Empty candidate list.** Not the orchestrating coordinator's concern
  at all, by design (Section 4). `PlannerRuntime`/`PlanDecision` already
  own this outcome: `PlanDecisionResult.NoViableCandidate` →
  `PlanningSessionResult.Failed`, reason "no Plan Candidates were supplied
  for this Planning Session" — unchanged, already implemented, already
  correct.
- **Generator exception.** `PlanCandidateGenerator`'s own contract
  (Candidate Generation Contract Design Section 4) already reserves
  exceptions for genuine faults only. Ownership of propagation is the
  orchestrating coordinator's (uncaught), ultimately surfacing via
  `ParkerRuntime.submitOwnerMessage`'s existing boundary, unchanged
  machinery.
- **Planner exception.** Identical ownership and propagation path.
  `PlannerRuntime.plan()` may fault for its own genuine reasons (an
  `EventBus` publish failure, for example) — not introduced or altered by
  this integration.
- **Planner selecting no candidate** (every supplied candidate rejected by
  `PlanDecision`, distinct from an empty input list). Exclusively
  `PlanDecision`'s/`PlannerRuntime`'s own, already-implemented decision —
  unchanged. The orchestrating coordinator's only job regarding this
  outcome is to carry the resulting `PlanningSessionResult.Failed` value
  forward, unreinterpreted, into whatever `GoalPlanningHandoffOutcome`
  variant represents "planning was attempted" — never silently converting
  it into an exception, a rejection, or a fabricated success.

**No implementation is designed for any of the four cases above** — this
section states ownership only, per the charter.

---

## 6. Question 5 — Existing Contracts

| Contract | Revision required? |
| --- | --- |
| `PlanningRequest` | **No.** Already the correct, sufficient, shared input to both `PlanCandidateGenerator.generate` and `PlannerRuntime.plan`. |
| `PlanCandidate` | **No.** Unchanged; already the correct output/input type at this boundary. |
| `PlanDecision` | **No.** Consumed internally by `PlannerRuntime.plan()`, unchanged; this integration introduces no new caller of it. |
| `PlannerRuntime` | **No.** `plan(request, candidates)`'s signature is already sufficient (Section 3). This integration is the first real caller, not a reason to change the interface. |
| `PlanCandidateGenerator` | **No.** Excluded from modification by this Review's own charter, and none is architecturally motivated. |
| `GoalPlanningHandoffCoordinator` | **Yes.** The one component whose shape must change: two new constructor dependencies (`PlanCandidateGenerator`, `PlannerRuntime`), and its `initiatePlanning` method's body and outcome type must evolve from "always defer" to "generate, then plan, then report the real result." Exact shape is Contract Design's job, not decided here. |

**Two further contracts, not named in the charter's list but transitively
affected, disclosed for completeness:**

- **`GoalPlanningHandoffOutcome`/`PlanningDeferralReason`** (defined
  alongside `GoalPlanningHandoffCoordinator` in the same file) — require
  an additive revision at minimum (a new variant carrying a real
  `PlanningSessionResult`); whether `Deferred`/`CANDIDATE_GENERATION_UNAVAILABLE`
  is retained as unreachable-but-defined, or retired outright, is
  Unresolved (below).
- **`ConversationOutcome`/`ParkerRuntimeOutcome`** — both currently define
  `PlanningDeferred(outcome: GoalPlanningHandoffOutcome)` as their sole
  Planning-related variant. Once `GoalPlanningHandoffOutcome` itself
  changes shape, these two must be revisited for naming/mapping
  consistency (e.g. does `PlanningDeferred` get renamed, or does it
  remain the wrapper name while its payload's own meaning evolves?) — not
  decided here.

**If none were required, this section would state so explicitly, per the
charter — one is: `GoalPlanningHandoffCoordinator`, plus two transitively
affected composition-layer types.**

---

## 7. Question 6 — Trust Boundary

**Confirmed, with one precision worth stating exactly rather than
asserting blanket.**

- **No execution.** Neither the orchestrating coordinator nor
  `PlanCandidateGenerator` holds any `Tool`/`ExecutionRequest` reference.
  Unchanged.
- **No tool invocation.** Identical, unchanged.
- **No scheduling.** No Task Manager Task field is written by anything
  this integration introduces.
- **No authority granted.** No `PermissionDecision`-producing type exists
  anywhere in this chain.
- **Task creation — precise statement, not a blanket denial.**
  `PlannerRuntime.plan()` **already**, as pre-existing, already-approved
  behaviour, calls `TaskProposalIntake.submitProposal` when `PlanDecision`
  selects a winner (`InMemoryPlannerRuntime.plan`, unchanged). This
  integration's own new code (the orchestrating coordinator) creates no
  task, invokes no tool, and grants no authority directly — it only
  sequences calls to already-governed components. But once this
  integration is implemented *and* a future, separate Unit wires a real
  `PlannerRuntime` into production (Finding 2 — which necessarily also
  wires a real `TaskManagerRuntime`), a real Task Proposal submission
  chain would, for the first time, become reachable from a live
  conversational Goal. That reachability is not created or newly
  authorised by this integration or by this Review — it is `PlannerRuntime`'s
  own, already-approved contract, exercised for the first time only once
  a separate, still-excluded production-wiring Unit occurs. Stated
  precisely so the trust-boundary confirmation is honest, not merely
  reassuring.

---

## 8. Question 7 — Production Wiring Sequence

The correct architectural order, once a future Unit authorises real
production wiring (not this one):

1. `ConversationReplyCoordinator.submitAndDeliver` receives
   `GatedOutcome.Produced(ReasoningProviderResponse.Goal)` from
   `communicationConversationCoordinator.submitAndReason` — unchanged.
2. It calls the orchestrating coordinator's single method with
   `(originalMessage, goal)` — unchanged call site, unchanged arguments.
3. **`PlanningRequest` is constructed first**, inside that coordinator —
   exactly as today: `planningSessionId` from the existing injected
   factory; `initiatingPrincipalId`, `correlationId`, `goal` extracted
   unchanged; `source`/`priority` at their existing defaults.
4. **`PlanCandidateGenerator.generate(request)` runs second**, given the
   same `PlanningRequest` just constructed, producing `List<PlanCandidate>`
   (zero, one, or many).
5. **`PlannerRuntime.plan(request, candidates)` runs third**, given the
   same `PlanningRequest` instance (not a copy or reconstruction) and the
   candidates list exactly as returned — unconditionally, per Section 4.
6. **Internally, `plan()` returns a `PlanDecisionResult`** as part of its
   own already-implemented lifecycle (`ANALYSING`'s own call to
   `PlanDecision.decide(request.goal, candidates)`) — this is `plan()`'s
   own internal step, not separately orchestrated by the coordinator.
7. `plan()` completes its own lifecycle and returns one
   `PlanningSessionResult` (`Completed`, `Rejected`, or `Failed`) — the
   coordinator receives this as `generate`'s and `plan`'s combined result.
8. **The orchestrating coordinator produces its own outcome fourth**,
   wrapping the `PlanningSessionResult` into whatever `GoalPlanningHandoffOutcome`
   shape Contract Design defines.
9. `ConversationReplyCoordinator` wraps that into `ConversationOutcome`
   — unchanged sequencing, only the payload's shape changes.
10. `ParkerRuntime.submitOwnerMessage` maps `ConversationOutcome` to
    `ParkerRuntimeOutcome`, logs, and returns — unchanged sequencing,
    same one-to-one mapping pattern already established.

**This sequence assumes a working `PlannerRuntime` instance already
exists at step 5 — this Review does not construct one, per Finding 2 and
this Unit's own exclusions.** The sequence above describes call order
once such an instance is available; it does not authorise, and this
Review does not recommend, actually wiring one in this Unit or the
Contract Design it clears the way for.

---

## 9. Question 8 — Test Strategy (Architectural Description Only)

- **Structural dependency proof.** The orchestrating coordinator's
  constructor accepts exactly its declared dependencies
  (`planningSessionIdFactory`, `planCandidateGenerator`, `plannerRuntime`)
  — no more, mirroring `GoalPlanningHandoffCoordinatorTest`'s own existing
  structural-test convention.
- **Sequencing proof.** `PlanCandidateGenerator.generate` is called
  before `PlannerRuntime.plan` — provable via a fake generator and a fake
  `PlannerRuntime` that record invocation order.
- **Argument-fidelity proof.** The exact `PlanningRequest` instance
  constructed by the coordinator is the one passed to both `generate` and
  `plan` — no reconstruction, no field drift between the two calls.
- **Candidate-list fidelity and ordering proof.** The list `generate`
  returns is passed to `plan` unchanged — same elements, same order, not
  filtered, not reordered, not mutated — since `DefaultPlanDecision` relies
  on generation order as its own tie-break rule.
- **Empty-list pass-through proof.** A fake generator returning an empty
  list still results in `plan` being called (not short-circuited) —
  proving the "no coordinator-level special-casing" rule from Section 4
  structurally, not just by inspection.
- **Failure propagation proof (generator).** A throwing fake
  `PlanCandidateGenerator` causes its exception to propagate uncaught
  through the coordinator — mirroring the existing throwing-
  `planningSessionIdFactory` test precedent exactly.
- **Failure propagation proof (planner).** A throwing fake `PlannerRuntime`
  causes its exception to propagate uncaught through the coordinator,
  symmetric to the above.
- **Result-fidelity proof.** Whatever `PlanningSessionResult` a fake
  `PlannerRuntime.plan()` returns is carried, unreinterpreted, into the
  coordinator's own returned outcome — value-equal, not merely
  type-equal.
- **Zero-call proofs for `Reply`/`NoAction`.** Neither
  `PlanCandidateGenerator` nor `PlannerRuntime` is ever invoked when the
  Reasoning response is `Reply` or `NoAction` — mirroring the existing
  "poisoned dependency, zero-call" pattern already used for
  `planningSessionIdFactory`/`replyDeliveryCoordinator` in
  `ConversationReplyCoordinatorTest.kt`.
- **Composition-level (`ParkerRuntime`) test — explicitly not achievable
  within this Unit's own eventual implementation.** A true end-to-end test
  through a real `PlannerRuntime` requires a real `TaskProposalIntake`
  (Finding 2), which requires production `TaskManagerRuntime` wiring —
  out of scope here and for the Contract Design this Review recommends.
  Composition-level tests for this integration should continue to use a
  fake or in-memory `PlannerRuntime`/`PlanCandidateGenerator`, exactly as
  `ParkerRuntimeConversationPipelineTest.kt` already uses a real stack for
  Reply but not (yet) for Planning.

---

## 10. Risks and Dependencies

- **Finding 2, restated as the primary risk.** No future Unit can wire a
  real `PlannerRuntime` into production without also wiring a real
  `TaskManagerRuntime`-shaped `TaskProposalIntake` — there is no smaller
  increment. This Review does not resolve it; it sharpens a previously
  understated dependency into a precise, disclosed fact for whichever
  future Unit performs that wiring.
- **Finding 3's consequence for already-shipped contracts.** `GoalPlanningHandoffOutcome.Deferred`
  becomes production-unreachable once this integration is implemented.
  Contract Design must explicitly decide its disposition (Unresolved
  Questions) rather than silently stop producing it.
- **Naming drift.** `GoalPlanningHandoffCoordinator`'s own name, and
  `ConversationOutcome.PlanningDeferred`'s own name, both describe a
  "handoff"/"deferral" concept this integration renders less accurate.
  Not a blocker — Contract Design may rename, or may knowingly retain the
  name for continuity, either being a legitimate, disclosed choice.

---

## 11. Explicitly Out of Scope

Restated for completeness, unchanged from this Unit's charter: modifying
`PlanCandidateGenerator`; modifying `PlannerRuntime`'s public contract
(confirmed unnecessary, Section 3); redesigning Candidate Generation;
revisiting `ReasoningContext`; introducing Planning Context; redesigning
Goal Management or Chapter 23; introducing execution, `TaskManagerRuntime`,
Tool execution, permissions, or authentication changes; writing Kotlin.
This Review also does not name the orchestrating coordinator's final
class/outcome-type names, does not decide `Deferred`'s disposition, and
does not design production wiring for `PlannerRuntime`/`TaskManagerRuntime`
— all three belong to a future Contract Design or a separately-chartered
production-wiring Unit, not to this document.

---

## 12. Constraining Contracts and Governance Documents

- `src/runtime/GoalPlanningHandoffCoordinator.kt`, `src/runtime/ConversationOutcome.kt`,
  `src/composition/ParkerRuntimeOutcome.kt`, `src/composition/ParkerRuntime.kt`
  — the components whose shape or content this Review's recommendation
  affects.
- `src/contracts/PlanCandidateGenerator.kt`, `src/runtime/DefaultPlanCandidateGenerator.kt`
  — the newly-implemented, unmodified inputs to this integration.
- `src/contracts/PlanDecision.kt` — `PlanningRequest`, `PlanCandidate`,
  `PlanDecision`, `PlannerRuntime`, all confirmed sufficient, unchanged.
- `src/runtime/InMemoryPlannerRuntime.kt`, `src/runtime/InMemoryTaskManagerRuntime.kt`
  — the source of Finding 2's coupling.
- `docs/architecture/CANDIDATE_GENERATION_CONTRACT_DESIGN.md`,
  `docs/implementation/CANDIDATE_GENERATION_SCOPE_LOCK.md` — Section 9's
  "Placement of invocation" open question, resolved by this Review.
- `docs/architecture/REASONING_TO_PLANNING_HANDOFF_GOVERNANCE_REVIEW.md`,
  `_CONTRACT_DESIGN.md`, `docs/implementation/REASONING_TO_PLANNING_HANDOFF_SCOPE_LOCK.md`
  — the origin of `GoalPlanningHandoffCoordinator`/`GoalPlanningHandoffOutcome`,
  and Section 8's original, now-sharpened "soft dependency" note (Finding
  2).
- `docs/architecture/PLANNER_RUNTIME_CONTRACT_DESIGN.md` — the already-settled
  decision keeping `candidates` external to `PlanningRequest` and to
  `PlannerRuntime`'s own internals, reconfirmed sufficient by Section 3.
- `docs/specifications/volume-06-planner-runtime/PlannerRuntimeSpecification.md`
  Section 6 — the Task Proposal intake closure this Review's Finding 2
  cites.
- `docs/architecture/ARCHITECTURE_DECISIONS.md` — AD-010 (Model
  Independence), restated as the reason `PlannerRuntime` itself is
  rejected as the orchestration owner (Section 2.1).

---

## 13. Unresolved Questions (for Contract Design, not this Review)

- **`GoalPlanningHandoffOutcome.Deferred`'s disposition.** Retained,
  unreachable but defined (for a hypothetical future generator that
  legitimately returns empty and a hypothetical reason to still special-
  case it); or retired outright, a breaking change to an already-shipped
  type. Not decided here.
- **Naming.** Whether `GoalPlanningHandoffCoordinator`/`GoalPlanningHandoffOutcome`/
  `ConversationOutcome.PlanningDeferred` are renamed to reflect that a
  real planning attempt, not merely a deferral, now occurs. Not decided
  here.
- **Exact new outcome variant shape.** What the coordinator returns when
  `plan()` genuinely completes, is rejected, or fails — a single new
  variant carrying `PlanningSessionResult` unchanged, or a richer mapping.
  Not decided here.
- **`PipelineStage` specificity for generator/planner faults.** Whether
  these continue to surface as `PipelineStage.UNKNOWN` (today's convention
  for ID-generation faults) or warrant a more specific stage. Not decided
  here.

None of these blocks Contract Design from proceeding — each is a
well-bounded, narrow decision Contract Design is positioned to make, not
a sign of unresolved architectural uncertainty about the integration
itself.

---

## 14. Readiness Determination

**Ready for Contract Design.**

- **Ownership:** determined (Section 2) — revise `GoalPlanningHandoffCoordinator`
  in place; adding `PlanCandidateGenerator` and `PlannerRuntime` as two
  new constructor dependencies, extending its existing single method.
- **`PlannerRuntime` boundary:** confirmed sufficient, unchanged (Section
  3) — this integration can be designed and implemented without
  modifying `PlannerRuntime`'s public contract.
- **Orchestration and failure-ownership:** fully assigned (Sections 4–5)
  — no ambiguity about who calls what, who propagates what, or who logs
  what.
- **Existing contracts:** exactly one (`GoalPlanningHandoffCoordinator`,
  plus its own co-located outcome type and two transitively-affected
  composition types) requires revision; five require none (Section 6).
- **Trust boundary:** reconfirmed, stated precisely rather than blanket
  (Section 7).
- **Production wiring sequence:** fully specified at the architecture
  level (Section 8), explicitly bounded to not require solving Finding
  2's `TaskManagerRuntime` coupling in this Unit or the Contract Design it
  authorises.
- **Test strategy:** described at the architectural level (Section 9),
  consistent with this codebase's own existing structural-and-behavioural
  test conventions.

The items in Section 13 are genuinely separable, narrow naming and
shape decisions, not gaps in this Review's own charter — none blocks a
Contract Design scoped to: the orchestrating coordinator's revised
dependency list and method body; the exact shape of its new outcome
variant(s); and the disposition of `Deferred`. Contract Design must not
expand into production wiring of `PlannerRuntime`/`TaskManagerRuntime`,
Planning Context, or any topic named in Section 11 — those remain
separately chartered.
