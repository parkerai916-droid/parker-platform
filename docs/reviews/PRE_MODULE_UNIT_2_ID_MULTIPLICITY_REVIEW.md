# Pre-Module Readiness Unit 2 — Post-Implementation Review

## ID Multiplicity Decision (gap #48)

## Correction (post-review, inherited from Unit 1)

Unit 1's `identityServiceWithPlannerRegistered()` helper (which every test
added or touched in this unit also uses) was found, after both this
review and Unit 1's own review were first written, to register the
planner's system Principal with `status = ACTIVE` rather than the
`CREATED` status `InMemoryIdentityService.register()` requires -- see
`PRE_MODULE_UNIT_1_PLANNER_PUBLISHER_IDENTITY_REVIEW.md`'s own
Correction section for the full detail. Fixed at the source (the shared
helper), which corrects every test in this unit's own file that depends
on it without any further change here. This unit's Sections 3-6 are
otherwise unaffected: the test count, the message-content assertions, and
the gap #48 disposition were never wrong, only the shared setup helper
was.

## 1. Summary

`InMemoryAgentRuntime` derives `AgentRunId` deterministically from
`TaskId`, and `InMemoryPlannerRuntime` derives `TaskProposalId`
deterministically from `PlanningSessionId` plus a fixed `-proposal-1`
suffix — both capping their respective multiplicity at exactly one, while
their governing specifications leave the wider multiplicity open (gap
#48, independent architecture audit Finding 1). This unit performed a
design pass first (`docs/architecture/PRE_MODULE_ID_MULTIPLICITY_DECISION.md`),
decided **Option B** (formally constrain the current phase to one-per-parent,
deferred not prohibited), and implemented only what that document
authorised: message and KDoc clarification, and test strengthening — no
ID-generation change, no contract change, no retry/forking logic.

## 2. Scope

**In scope, and done:**

- A design decision document, written and reviewed before any Kotlin
  change, per this unit's own "do not guess" instruction.
- Exception-message and KDoc updates in both `InMemoryAgentRuntime.kt` and
  `InMemoryPlannerRuntime.kt` citing the decision explicitly.
- Test updates: the two pre-existing "resubmitting..." tests tightened to
  check message content; one new test per subsystem confirming the
  message cites this decision and gap #48 by name.
- `IMPLEMENTATION_GAPS.md` #48 updated to "formally constrained, deferred."
- `IMPLEMENTATION_HISTORY.md` updated with this unit's entry.

**Explicitly out of scope, and not done, per the decision document's own
Section 6 ("Not authorised") and this unit's instructions:**

- No new `AgentRunId`/`TaskProposalId` generation mechanism (no counters,
  no UUIDs, no caller-supplied identifiers) — none is needed while
  multiplicity remains deferred, not implemented.
- No change to `AgentRunCommand`, `PlanningRequest`, or any other public
  contract.
- No retry, forking, or multi-instance orchestration logic in either
  runtime.
- No module access of any kind.
- No change to `EventBus`, Memory Runtime, or World Model Runtime.
- No redesign of Agent Runtime or Planner Runtime: the multi-step loop,
  Plan Decision algorithm, lifecycle transitions, and event sequences in
  both classes are byte-for-byte unchanged apart from the two message/KDoc
  edits named above.

## 3. What Changed

`src/runtime/InMemoryAgentRuntime.kt`:

- The `check(agentRunId !in agentRuns) { ... }` exception message in
  `start()` now states the cap is "a deliberate, documented constraint"
  and cites `IMPLEMENTATION_GAPS.md #48` and the decision document by
  path, instead of only stating that a second Agent Run is unsupported.
- A comment above the `AgentRunId` minting line explains the decision and
  notes that `TaskManagerRuntimeSpecification.md`'s own "zero, one, or
  many" language is not being narrowed by this implementation decision.

`src/runtime/InMemoryPlannerRuntime.kt`:

- The `check(request.planningSessionId !in sessions) { ... }` exception
  message in `plan()` gained the same kind of explicit citation, appended
  to its existing "reconsideration requires a new PlanningRequest" text.
- A comment above the `TaskProposalId` minting line mirrors the Agent
  Runtime comment, citing `PlannerRuntimeSpecification.md`'s own "one or
  more Task Proposals" language as deliberately not narrowed.

`tests/runtime/InMemoryAgentRuntimeTest.kt`:

- "resubmitting START for the same taskId is rejected as caller misuse"
  now captures the thrown exception and asserts its message contains "has
  already been processed" (the pre-existing, unchanged prefix).
- New test: "the one-Agent-Run-per-Task cap is a deliberate, documented
  decision, not an accidental limitation" — asserts the message contains
  "deliberate, documented constraint" and "IMPLEMENTATION_GAPS.md #48".

`tests/runtime/InMemoryPlannerRuntimeTest.kt`:

- "resubmitting the same planningSessionId is rejected as caller misuse"
  now captures the thrown exception and asserts its message contains "has
  already been submitted" (the pre-existing, unchanged prefix).
- New test: "the one-Task-Proposal-per-Planning-Session cap is a
  deliberate, documented decision, not an accidental limitation" —
  asserts the same two substrings as the Agent Runtime equivalent.

`docs/architecture/IMPLEMENTATION_GAPS.md` #48 and
`docs/implementation/IMPLEMENTATION_HISTORY.md`: updated as described in
Section 1 above.

## 4. Testing

Static count only — no working Kotlin/Gradle toolchain was available in
this session's sandbox, and Android Studio verification is Human
authority per PES-001. Two tests were added (one per subsystem); no test
was removed. Against the prior static projection of 416/416 (Pre-Module
Readiness Unit 1), the expected total is 418/418. This number is **not
considered authoritative** until confirmed in Android Studio, per
`IMPLEMENTATION_HISTORY.md`'s own entry for this unit.

## 5. Self-Traceability Review

No new public type, field, or interface was introduced. The only Kotlin
changes are: two exception-message string literals (longer, not
differently triggered), four comment blocks, and four test methods (two
tightened, two new) — all mechanical, all traceable directly to
`PRE_MODULE_ID_MULTIPLICITY_DECISION.md` Section 6's own "Authorised"
list, and nothing beyond it. `AgentRunId("run-for-${taskId}")` and
`TaskProposalId("${planningSessionId}-proposal-1")`'s actual values are
unchanged — the same IDs are minted today as before this unit.

## 6. Confirmation

- **#48 is closed or formally constrained.** Formally constrained, as
  "deferred" — see `IMPLEMENTATION_GAPS.md` #48's updated status. Not
  closed as a defect fix, because the design document's own Section 1
  concluded no defect existed, only an undisclosed decision.
- **Architecture and implementation agree.** Both governing specifications'
  "zero, one, or many"/"one or more" language remains untouched and open;
  the implementation's current one-per-parent behaviour is now explicitly
  documented as this phase's deliberate choice within that open range, not
  a silent contradiction of it.
- **No retry/forking/module access was accidentally introduced.** Verified
  directly: neither `InMemoryAgentRuntime.kt` nor `InMemoryPlannerRuntime.kt`
  gained any new method, field, or dependency; the only changes are the
  message/comment edits described in Section 3.
- **All tests pass:** not yet confirmed by Android Studio (see Section 4).
  The static, arithmetic projection is 418/418, carried forward
  unauthoritative into `IMPLEMENTATION_HISTORY.md`'s own entry pending
  human verification.

Not committed, per this unit's own instruction.
