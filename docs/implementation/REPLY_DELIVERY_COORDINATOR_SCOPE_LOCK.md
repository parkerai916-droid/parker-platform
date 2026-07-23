# Reply Delivery Coordinator — Scope Lock (Sprint 10, Unit 2)

## Status

**Stage 5 Scope Lock, PES-001. Locked for Unit 2 only.** This document
freezes `docs/implementation/REPLY_DELIVERY_COORDINATOR_IMPLEMENTATION_PLAN.md`
("the Plan") exactly as reviewed (Sprint 10 – Unit 2 Governance Review)
and migrated, byte-for-byte unchanged, into the canonical repository. It
does not redesign, add to, or narrow anything the Plan already decided —
it restates the Plan's own decisions as frozen scope, per PES-001's own
Stage 3/Stage 5 relationship ("Implementation Decisions clarify approved
architecture. They do not replace or redefine it," applied here one level
up: a Scope Lock clarifies which of a Plan's own already-approved
decisions are now frozen; it does not relitigate them).

**Precondition satisfied.** The Plan's own Section 14, Implementation
Sequence, Step 1 requires "the companion Unit (`ResponseComposer`) is
Scope Locked, implemented, and its own tests pass first." This is now
fully satisfied: `ResponseComposer` is Scope Locked
(`docs/implementation/RESPONSE_COMPOSER_SCOPE_LOCK.md`), implemented
(`src/runtime/ResponseComposer.kt`), and verified — Android Studio
589/589 passing, BUILD SUCCESSFUL, per
`docs/implementation/IMPLEMENTATION_HISTORY.md`'s Sprint 10, Unit 1
entry. At the time the Plan and its companion Scope Lock were drafted,
this precondition was only partially satisfied (Scope Locked but not yet
implemented); it is fully satisfied now.

No Kotlin is written by this document. No test is created by this
document. No architecture document, ADR, Contract Design, or
Implementation Plan is modified by this document. `IMPLEMENTATION_GAPS.md`
and `IMPLEMENTATION_HISTORY.md` are not modified by this document. No
commit, tag, or push is performed by this document.

---

## 1. Governing Documents

In order of authority, per this program's own established hierarchy
(Constitution > PES-001 > Accepted ADRs > Contract Design > Implementation
Plans > Sprint Handovers):

1. `docs/architecture/PARKER_ENGINEERING_STANDARD.md` (PES-001) — Stage 3
   (Implementation Plan) and Stage 5 (Scope Lock) definitions, and the
   Human-primary-authority model governing both.
2. `docs/architecture/RESPONSE_DELIVERY_CONTRACT_DESIGN.md` — Section 8
   ("`ResponseDelivery` does not construct an `OutboundParkerResponse`...
   a future, separately-scoped coordinator's job"), the architectural
   basis for this Unit existing at all.
3. `docs/architecture/COMMUNICATION_CONTRACT_DESIGN.md` — Section 8,
   "Cognition proposes. Trust authorises. Runtime executes," the
   boundary this Unit's own Sequencing-Only Invariant (Plan Section 4d)
   preserves without exception.
4. `docs/architecture/REASONING_PROVIDER_CONTRACT_DESIGN.md` —
   Constitutional Boundaries ("A Reasoning Provider proposes. It
   authorises nothing, executes nothing"), restated at this Unit's own
   distance from any Reasoning Provider dependency (this Unit holds
   none, direct or indirect).
5. `docs/architecture/ADR-025_RESPONSE_DELIVERY_CONTENT_CARRIER.md` and
   `docs/architecture/ADR-026_MODULE_RESOURCE_OWNERSHIP_CONVENTION.md` —
   both Accepted; both load-bearing for `ResponseDelivery`'s own already-
   frozen internals, reused unchanged by this Unit, not reopened.
6. `docs/implementation/RESPONSE_COMPOSER_SCOPE_LOCK.md` — the companion
   Unit's own frozen scope; this Unit's own precondition (Status, above).
7. `docs/architecture/IMPLEMENTATION_GAPS.md` #53 (the remaining
   Reply→`ResponseDelivery` wiring portion this Unit narrows — Section
   13, below) and #49 (the identity-constant defect class this Unit's
   design must not, and does not, reintroduce).
8. `RECONCILIATION_ADDENDUM_RESPONSE_COMPOSER_VS_REPLY_DELIVERY_COORDINATOR.md`
   — Option C, the governing decision confirming this Unit's redefined,
   two-dependency, identity-free shape.
9. **`docs/implementation/REPLY_DELIVERY_COORDINATOR_IMPLEMENTATION_PLAN.md`
   itself, as governance-reviewed and migrated unchanged — the direct
   source this Scope Lock freezes**, Sections 1 through 16.
10. **Sprint 10 – Unit 2 Governance Review** (this session) — confirmed
    zero drift between every dependency, signature, and test-fixture
    assumption the Plan makes and the actual, current, as-built code; no
    architectural conflict found; the sole finding was a process step
    (migration), now resolved.

---

## 2. Exact Included Work

Restated from the Plan, Section 2, unchanged:

- One new, small, standalone, non-interface-backed coordinator,
  `ReplyDeliveryCoordinator`, with exactly one public method,
  `composeAndDeliver`, performing exactly the five-step sequence in the
  Plan's Section 1.
- Unit tests for the coordinator, per Section 11, below.

## 3. Exact Excluded Work

Restated from the Plan, Section 9 (Implementation Boundaries — Out of
Scope), unchanged:

- Constructing `OutboundParkerResponse` — `ResponseComposer`'s job
  alone.
- Resolving identity of any kind — no `IdentityService` dependency
  exists.
- Direct dependency on `ExecutionPipeline` — reachable only indirectly,
  inside `ResponseDelivery`'s own already-approved internals.
- Direct dependency on `PermissionEngine` — same reasoning.
- Dependency on `ReasoningProvider` — this Unit consumes whatever
  `ReasoningProviderResponse` it is given as an input parameter; it does
  not produce one.
- Routing `Goal`/`NoAction` beyond propagating `ResponseComposer`'s own
  `NotAccepted` — this Unit does not itself distinguish which variant
  caused a rejection.
- Modifying response content or adding metadata.
- Retry policy of any kind — each dependency is called exactly once,
  unconditionally.
- Exception handling of any kind around either dependency — no
  `try`/`catch`, no fallback value, no logging-and-continuing.
- Owning mutable state of any kind.
- Introducing a new public contract or result type — the return type is
  the existing `GatedOutcome<ExecutionResult>`, already
  `ResponseDelivery.deliver`'s own return type.
- Modifying `ResponseComposer`, `ResponseDelivery`, or `GatedOutcome` —
  all three are reused exactly as they exist.
- A production composition root — no production startup path is added
  by this Unit to construct a real `ReplyDeliveryCoordinator` for
  general use.

## 4. Exact Production Files to Add

**Frozen. No other production file may be added or modified under this
Scope Lock.**

| File | Status |
| --- | --- |
| `src/runtime/ReplyDeliveryCoordinator.kt` | New |

No existing `src/` file is modified. No new file is added under
`src/interfaces/` — this Unit introduces no public contract type (Plan,
Required Analysis Question 2).

## 5. Exact Test Files to Add

**Frozen. No other test file may be added or modified under this Scope
Lock.**

| File | Status |
| --- | --- |
| `tests/runtime/ReplyDeliveryCoordinatorTest.kt` | New — built on `FakeIdentityService` (companion Plan/Scope Lock, already existing), and the pre-existing `FakeResourceRegistry`/`FakeExecutionPipeline` (`tests/runtime/`, already committed, already used by `ResponseDeliveryTest.kt`). |

No existing `tests/` file is modified. **This Unit adds no new test
fixture of its own** — it reuses `FakeIdentityService`,
`FakeResourceRegistry`, and `FakeExecutionPipeline` exactly as they
already exist (Plan, Section 3).

## 6. Approved Class and Method Signature

Frozen exactly as the Plan's Section 10 proposes:

```kotlin
package parker.core.runtime

import parker.core.interfaces.InboundOwnerMessage
import parker.core.interfaces.ReasoningProviderResponse
import parker.core.contracts.ExecutionResult

/**
 * Sequences [ResponseComposer] and [ResponseDelivery]. Introduces no
 * business logic of its own -- see
 * `docs/implementation/REPLY_DELIVERY_COORDINATOR_IMPLEMENTATION_PLAN.md`.
 */
class ReplyDeliveryCoordinator(
    private val responseComposer: ResponseComposer,
    private val responseDelivery: ResponseDelivery,
) {

    suspend fun composeAndDeliver(
        originalMessage: InboundOwnerMessage,
        reasoningResponse: ReasoningProviderResponse,
    ): GatedOutcome<ExecutionResult> {
        val composed = responseComposer.compose(originalMessage, reasoningResponse)
        return when (composed) {
            is GatedOutcome.NotAccepted -> composed
            is GatedOutcome.Produced -> responseDelivery.deliver(composed.value)
        }
    }
}
```

**Frozen elements of this signature specifically:** the class name
(`ReplyDeliveryCoordinator`); its exactly two constructor parameters
(`responseComposer: ResponseComposer`, `responseDelivery: ResponseDelivery`);
the `composeAndDeliver` method's name, parameter list
(`originalMessage: InboundOwnerMessage`, `reasoningResponse:
ReasoningProviderResponse`), `suspend` modifier, and return type
(`GatedOutcome<ExecutionResult>`); and the exact five-line body — call
`responseComposer.compose` once, branch on the result, return
`composed` unchanged on `NotAccepted`, return
`responseDelivery.deliver(composed.value)` unchanged on `Produced`. Any
deviation from this signature discovered during implementation is a
Scope Lock violation requiring a new planning pass, not a silent
adjustment (Plan, Section 16).

## 7. Approved Dependency and Sequencing Behaviour

Frozen exactly as the Plan's Section 1, Section 4, and Section 4d
establish:

- `ResponseComposer.compose(originalMessage, reasoningResponse)` is
  called **exactly once, first**, on every invocation.
- If the result is `GatedOutcome.NotAccepted`: it is returned
  **unchanged** — `ResponseDelivery.deliver` is **never** called on this
  branch.
- If the result is `GatedOutcome.Produced(response)`:
  `ResponseDelivery.deliver(response)` is called **exactly once**, and
  its own `GatedOutcome<ExecutionResult>` is returned **unchanged**,
  whatever it is (`Produced` or `NotAccepted`) — no additional wrapping
  (Plan, Section 5, Decision 1; `GatedOutcome<T>`'s declared `out T`
  covariance is what makes the `NotAccepted` branch type-check without
  conversion).
- **This coordinator never resolves identity of its own.** No
  `IdentityService` dependency exists; sender identity was already
  resolved inside `ResponseComposer`, and this coordinator never
  re-touches it (Plan, Section 4d).
- **This coordinator never retries.** Each dependency is called exactly
  once per invocation, unconditionally.
- **This coordinator never inspects `reasoningResponse`'s own variant.**
  It does not distinguish "was a `Goal`" from "was `NoAction`" from "was
  a `Reply` that failed delivery" — it only branches on `GatedOutcome`'s
  own two cases, whichever dependency produced them.

## 8. Approved Branch Behaviour

Frozen exactly as the Plan's Section 1 and Section 10 establish:

| `responseComposer.compose(...)` result | `responseDelivery.deliver` called? | Result |
| --- | --- | --- |
| `GatedOutcome.NotAccepted(reason)` | No | Returned unchanged — same `reason`, no new instance constructed |
| `GatedOutcome.Produced(response)` | Yes, exactly once | `responseDelivery.deliver(response)`'s own result returned unchanged — `Produced(executionResult)` or `NotAccepted(reason)`, whichever `ResponseDelivery` itself produces |

No other branch, variant, or fallback exists. Nothing routes a `Goal`
or `NoAction` anywhere beyond the `NotAccepted` `ResponseComposer`
already returned for them.

## 9. Approved Unit Boundaries — Architectural Separation

Frozen exactly as the Plan's Section 4, Section 4d, and Section 9
establish, restated explicitly per this Scope Lock's own governing
instruction to preserve this separation without exception:

- **`ResponseComposer` performs composition only.** Already Scope
  Locked and implemented (`RESPONSE_COMPOSER_SCOPE_LOCK.md`); reused by
  this Unit unchanged, called exactly once, first.
- **`ReplyDeliveryCoordinator` performs orchestration only.** It
  sequences two already-shaped components and introduces no new field,
  no new domain concept, and no state of its own (Plan, Section 5,
  Decision 3). It never constructs an `OutboundParkerResponse`, never
  resolves identity, never authorises, and never touches
  `ExecutionPipeline`, `PermissionEngine`, `ToolRegistry`, or
  `ReasoningProvider` at any depth.
- **`ResponseDelivery` performs delivery only.** Already implemented,
  Contract Designed, and verified (Sprint 7 Unit C4 / Sprint 8); reused
  by this Unit unchanged, called exactly once, only on the `Produced`
  branch.
- **`ExecutionPipeline` performs execution only.** Reached only
  indirectly, inside `ResponseDelivery`'s own already-approved
  internals — never directly by `ReplyDeliveryCoordinator`.

**The coordinator remains a thin orchestrator.** This is enforced
structurally, not merely stated: the constructor accepts exactly two
parameters (Section 6), neither of which is `IdentityService`,
`ExecutionPipeline`, `PermissionEngine`, `ReasoningProvider`,
`ResourceRegistry`, `ToolRegistry`, `PlannerRuntime`, `MemoryStore`, or
`WorldModel` — there is nothing reachable, at any depth, through which
this coordinator could reason, plan, authorise, resolve identity, or
execute. A dedicated structural test (Section 11, below) makes this a
compile-time-adjacent, test-verified property.

## 10. Approved Exception Behaviour

Frozen exactly as the Plan's Section 4c establishes:

- **This coordinator must not recover from, translate, retry, or
  suppress an exception thrown by either dependency.** Such failures
  propagate unchanged to the caller.
- `GatedOutcome.NotAccepted` is reserved exclusively for the one
  structural, expected outcome each dependency already defines for
  itself (`ResponseComposer`'s "not a Reply"; `ResponseDelivery`'s "no
  channel resource found" / "ambiguous resource") — never for catching
  and repackaging a thrown exception.
- No `try`/`catch` exists anywhere in `ReplyDeliveryCoordinator`.

## 11. Approved Test Strategy

Frozen exactly as the Plan's Section 7 establishes, for
`tests/runtime/ReplyDeliveryCoordinatorTest.kt`, using a real
`ResponseComposer` built from `FakeIdentityService` and a real
`ResponseDelivery` built from `FakeResourceRegistry`/
`FakeExecutionPipeline` as the primary fixture (Plan, Section 5,
Decision 2 — value-level verification through each dependency's own
existing interface-backed seams, not object-reference identity, since
neither `ResponseComposer` nor `ResponseDelivery` is itself
interface-backed):

1. Produced path (`Reply`, successful delivery) — `Produced` carrying
   `ResponseDelivery`'s own `ExecutionResult`;
   `pipeline.lastSubmittedRequest` carries `principalId ==
   PrincipalId("system.response-composer")`, `correlationId ==
   originalMessage.correlationId.value`, and
   `metadata[RESPONSE_TEXT_METADATA_KEY]` equal to the `Reply`'s own
   text — the value-level proof that the composed response's fields
   reached `ExecutionPipeline` unchanged.
2. `NotAccepted` path (`Goal`) — identical reason string
   `ResponseComposer.compose` itself produced;
   `fakeIdentityService.resolveCallCount == 0`;
   `resources.listByOwnerCallCount == 0`; `pipeline.submitCallCount ==
   0` — `ResponseDelivery` never entered.
3. `NotAccepted` path (`NoAction`) — same shape of assertion.
4. `NotAccepted` path (delivery-level rejection) — a `Reply` that
   composes successfully (`resolveCallCount == 1`) but
   `fakeResourceRegistry` returns no matching Resource:
   `ResponseDelivery`'s own rejection reason returned unchanged;
   `resources.listByOwnerCallCount == 1` but `pipeline.submitCallCount
   == 0` — the call-counted distinction between "never entered" and
   "entered and rejected."
5. No construction, no mutation — verified by direct code review of
   the five-line method body (Section 6, above); no runtime
   reference-identity assertion is made (Plan, Section 4b's own
   Verification note).
6. Exactly-once invocation, no retries, per-branch counts — no count
   exceeds `1` on any branch, for any dependency, in any test.
7. Exception propagation, not recovery — with `fakeIdentityService`
   configured to throw from `resolve`: the coordinator's own method call
   propagates that exact exception via `assertFailsWith`;
   `resources.listByOwnerCallCount`/`pipeline.submitCallCount` both
   remain `0`.
8. Structural / negative test — the constructor accepts only a
   `ResponseComposer` and a `ResponseDelivery`; no dependency slot
   exists for any prohibited type (Section 9, above).
9. Statelessness test — `ReplyDeliveryCoordinator::class.java.declaredFields`
   contains exactly its two constructor-injected dependencies and
   nothing else.
10. Real end-to-end test, narrower scope — the full real
    `InMemoryResourceRegistry`/`InMemoryToolRegistry`/
    `InMemoryModuleRegistry`/`InMemoryToolInvocationBinding`/
    `InMemoryEventBus`/`FakePermissionEngine`/`DefaultExecutionPipeline`/
    real `LocalTextChannelDeliverTool` stack, plus a real
    `InMemoryIdentityService` with `system.response-composer`
    registered: for a `Reply`, the owner-notification callback receives
    exactly the reply text, unchanged. **Scoped to the successful path
    only** — it does not assert anything about `deliver` invocation
    counts on rejection paths (those are covered by tests 2–4, above).

Full Gradle test suite run once implementation is complete; a static,
honestly-disclosed projected count if the sandbox cannot resolve the
Kotlin Gradle plugin, pending Steven's Android Studio verification
(Section 14, below).

## 12. Acceptance Criteria

Frozen exactly as the Plan's Section 8 establishes:

- `ReplyDeliveryCoordinator` exists, is independently constructible with
  only a `ResponseComposer` and a `ResponseDelivery`, and its own tests
  (Section 11, above) pass.
- A `Reply` that composes and delivers successfully results in
  `GatedOutcome.Produced` carrying `ResponseDelivery`'s own
  `ExecutionResult`, unchanged.
- A `Goal`, `NoAction`, or any `ResponseComposer`-level rejection never
  reaches `ResponseDelivery` — verified structurally, not merely by
  inspection.
- A `ResponseDelivery`-level rejection (e.g. no matching channel
  Resource) is returned unchanged, exactly as `ResponseComposer`-level
  rejections are — no asymmetry between the two sources of
  `NotAccepted`.
- The coordinator constructs no `OutboundParkerResponse` and mutates
  neither dependency's input or output — verified structurally.
- The coordinator is stateless, holds no `IdentityService` dependency,
  and resolves no identity of its own — verified structurally.
- Exactly one call to each dependency per invocation on the applicable
  branch — no retries, unconditionally — verified by the call-counting
  technique in Section 11.
- The coordinator never recovers from, translates, retries, or
  suppresses a thrown exception — verified by test.
- No production (`src/`) code added by this Unit references
  `IdentityService`, `ExecutionPipeline`, `PermissionEngine`,
  `ReasoningProvider`, `ResourceRegistry`, `ToolRegistry`,
  `PlannerRuntime`, `MemoryStore`, or `WorldModel`, anywhere — verified
  by the dependency list in Section 6 and the structural test in
  Section 11.
- No existing `src/` or `tests/` file, and no companion-Unit file, is
  modified.
- All tests listed in Section 11 pass, and the full Gradle suite passes
  (or a projected count is honestly reported).

## 13. Explicit Confirmation: Scope of Gap #53 Closure

**This Scope Lock authorises work that narrows, but does not close,
`IMPLEMENTATION_GAPS.md` #53.** Consistent with the Sprint 10 – Unit 2
Governance Review's own Finding 4:

**What this Unit, once implemented and verified, closes:** the specific
open item currently recorded in gap #53 — *"Nothing in this repository
calls `ResponseComposer.compose` from a real conversation turn — the
wiring that would connect `CommunicationConversationCoordinator`'s
reasoning output to `ResponseComposer`, and `ResponseComposer`'s output
onward to `ResponseDelivery.deliver`, remains unimplemented. This is
`ReplyDeliveryCoordinator`, Unit 2."* Once `ReplyDeliveryCoordinator`
exists and its tests (Section 11, above) pass, one real, tested,
production-shaped code path exists that takes a `Reply` through to a
`ResponseDelivery.deliver` call. That, precisely, and nothing broader,
is what this Unit closes.

**What this Unit does not close, and this Scope Lock does not authorise
implying is closed:**

- **A production composition root.** No production startup path is
  added by this Unit (Section 3, Section 9). Nothing will call
  `ReplyDeliveryCoordinator.composeAndDeliver` from a real, running
  `CommunicationConversationCoordinator` output after this Unit lands,
  any more than `ResponseComposer.compose` is called from one today.
- **`Goal` / Planner Runtime routing.** This Unit only propagates
  `ResponseComposer`'s own `NotAccepted` for `Goal`/`NoAction`
  unchanged (Section 8, above); it does not route either variant
  anywhere.
- **`ReasoningContext` assembly ownership.** Entirely untouched by this
  Unit; remains unassigned exactly as `REASONING_PROVIDER_CONTRACT_DESIGN.md`
  Section 9 already discloses.
- **`LocalHttpModelInferenceClient`'s live HTTP path.** Entirely
  unrelated to this Unit; remains untested by the automated suite.

Any future documentation update recording this Unit's completion must
state plainly which two files (`ResponseComposer.kt`,
`ReplyDeliveryCoordinator.kt`) closed the specific wiring item quoted
above, and must not overstate closure of any other #53 item — restated
from the Plan's own Section 13 and the Governance Review's own Risk
finding on this exact point.

## 14. Verification Requirements

Restated from the Plan's Section 14, Step 5, and mirroring the
companion Scope Lock's own Section 14, unchanged in kind:

1. Steven opens the `parker-platform` project in Android Studio.
2. Runs the full test suite (`./gradlew test` or equivalent) — not only
   `ReplyDeliveryCoordinatorTest.kt` — confirming no existing test
   regresses, including the companion Unit's own 589 already-verified
   tests.
3. Confirms each of the ten tests in Section 11, above, passes
   individually.
4. Records the real pass/fail count and compares it against the static
   projection reported at implementation time, and against the
   pre-existing baseline (589 passing prior to this Unit); any
   discrepancy is itself a finding to report.
5. Only after a real, verified pass does Section 15 (Documentation
   Follow-up), below, become eligible to proceed.

## 15. Documentation Follow-up

Restated from the Plan's Section 13, unchanged, and explicitly
deferred — not performed by this document:

- `IMPLEMENTATION_HISTORY.md` **may** be updated after verified
  implementation, recording this Unit exactly as delivered (files
  added, tests added, a real Gradle result) — not before.
- `IMPLEMENTATION_GAPS.md` #53 **may be narrowed further, not closed in
  full**, after verified implementation — restated precisely from
  Section 13, above: only the specific "wiring that would actually call
  `ResponseDelivery`" item closes; the production composition root,
  `Goal`/Planner routing, `ReasoningContext` assembly ownership, and the
  untested live HTTP path all remain open, and any documentation update
  must say so explicitly, not merely by omission.
- No architecture or Contract Design document is modified at any point
  during this Unit's implementation.
- Neither `IMPLEMENTATION_GAPS.md` nor `IMPLEMENTATION_HISTORY.md` is
  touched by this Scope Lock document itself.

## 16. Stop Conditions

This Scope Lock does not authorise:

- Writing any Kotlin file not listed in Sections 4 and 5, above.
- Modifying any existing `src/` or `tests/` file.
- Modifying `RESPONSE_COMPOSER_SCOPE_LOCK.md`, any architecture
  document, any ADR, any Contract Design, or any Implementation Plan.
- Updating `IMPLEMENTATION_GAPS.md` or `IMPLEMENTATION_HISTORY.md`.
- Any commit, tag, or push.
- Any deviation from the signature (Section 6), dependency and
  sequencing behaviour (Section 7), branch behaviour (Section 8), unit
  boundaries (Section 9), or exception behaviour (Section 10) frozen
  above, without a new planning pass.
- Any implication that `IMPLEMENTATION_GAPS.md` #53 closes in full upon
  this Unit's completion (Section 13, above).

If implementation reveals that any frozen element above cannot be
satisfied as written, work stops and returns to Steven for a new
decision — it is not silently resolved during implementation.

---

## Conclusion

This Scope Lock freezes `ReplyDeliveryCoordinator` (Sprint 10, Unit 2)
exactly as reviewed and migrated in
`docs/implementation/REPLY_DELIVERY_COORDINATOR_IMPLEMENTATION_PLAN.md`.
One production file (`src/runtime/ReplyDeliveryCoordinator.kt`) and one
test file (`tests/runtime/ReplyDeliveryCoordinatorTest.kt`) are
authorised, both additions, no existing file modified, no new test
fixture introduced. The coordinator holds exactly two dependencies
(`ResponseComposer`, `ResponseDelivery`), performs sequencing only, and
preserves the architectural separation named in Section 9: composition
(`ResponseComposer`), orchestration (`ReplyDeliveryCoordinator`),
delivery (`ResponseDelivery`), and execution (`ExecutionPipeline`) each
remain exactly one component's job. This document freezes only what the
Plan already authorises — it introduces no new implementation decision,
resolves no new design question, and neither broadens nor narrows the
Plan's own scope. It closes no part of `IMPLEMENTATION_GAPS.md` #53
itself; it authorises the work that, once implemented and verified,
would narrow #53 by exactly the wiring item named in Section 13, and no
more. This document does not implement anything, does not create any
test, does not modify `IMPLEMENTATION_GAPS.md` or
`IMPLEMENTATION_HISTORY.md`, and does not commit, tag, or push. It
awaits Steven's review.

## Related

- `docs/implementation/REPLY_DELIVERY_COORDINATOR_IMPLEMENTATION_PLAN.md` (the Plan this document locks)
- `docs/implementation/RESPONSE_COMPOSER_SCOPE_LOCK.md` (companion Unit, precondition)
- `docs/implementation/IMPLEMENTATION_HISTORY.md` (Sprint 10, Unit 1 entry — the 589/589 verified baseline this Unit's own tests are added against)
- Sprint 10 – Unit 2 Governance Review (this session)
- `RECONCILIATION_ADDENDUM_RESPONSE_COMPOSER_VS_REPLY_DELIVERY_COORDINATOR.md`
- `docs/architecture/PARKER_ENGINEERING_STANDARD.md`
- `docs/architecture/RESPONSE_DELIVERY_CONTRACT_DESIGN.md`
- `docs/architecture/COMMUNICATION_CONTRACT_DESIGN.md`
- `docs/architecture/REASONING_PROVIDER_CONTRACT_DESIGN.md`
- `docs/architecture/ADR-025_RESPONSE_DELIVERY_CONTENT_CARRIER.md`
- `docs/architecture/ADR-026_MODULE_RESOURCE_OWNERSHIP_CONVENTION.md`
- `docs/architecture/IMPLEMENTATION_GAPS.md` (#49, #53)
- `src/runtime/ResponseComposer.kt`, `src/runtime/ResponseDelivery.kt`, `src/runtime/GatedOutcome.kt`
- `tests/runtime/FakeIdentityService.kt`, `tests/runtime/FakeResourceRegistry.kt`, `tests/runtime/FakeExecutionPipeline.kt`
