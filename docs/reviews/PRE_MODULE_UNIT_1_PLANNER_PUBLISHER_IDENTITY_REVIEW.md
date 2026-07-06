# Pre-Module Readiness Unit 1 — Post-Implementation Review

## Planner Runtime Publisher Identity (gap #49)

## Correction (post-review)

`identityServiceWithPlannerRegistered()` (Section 3) originally registered
the planner's system Principal with `status = PrincipalStatus.ACTIVE`.
`InMemoryIdentityService.register()` requires `status == CREATED` for any
newly registered Principal (`InMemoryIdentityService.kt:58-60`, itself
proven by an existing test in `InMemoryIdentityServiceTest.kt`) and throws
`IllegalArgumentException` otherwise -- which would have made every test
in `InMemoryPlannerRuntimeTest.kt` that uses this helper fail at setup,
before any Planner logic ran, including all pre-existing tests this unit
updated to use it. This was found after this review was first written and
corrected by changing the registered status to `CREATED` -- the only
change; `resolve()` does not filter by status (gap #37), so nothing else
in this unit's implementation or test assertions depended on which status
was used. The test count and file list in Sections 3-4 are unaffected;
only this one status value was wrong.

## 1. Summary

`InMemoryPlannerRuntime` previously published every Planner event under a
hardcoded `PrincipalId("system.planner-runtime")` that was never
registered with, or resolved through, `IdentityService` --
`docs/architecture/IMPLEMENTATION_GAPS.md` #49, surfaced by the
independent architecture audit
(`docs/reviews/ARCHITECTURE_V2_INDEPENDENT_AUDIT_TRIAGE.md`, Finding 2)
and classified there as "should fix soon." This unit closes it by making
`InMemoryPlannerRuntime` resolve its own publisher identity through
`IdentityService` before publishing anything, following
`InMemoryAgentRuntime`'s own, pre-existing `agentIdentityPrincipalId`
precedent exactly.

## 2. Scope

**In scope, and done:**

- `InMemoryPlannerRuntime.plan` resolves `PLANNER_RUNTIME_PRINCIPAL_ID`
  via `IdentityService` before publishing any event.
- A safe, non-crashing failure path when that identity does not resolve:
  `plan` returns `PlanningSessionResult.Failed` with an explicit reason,
  rolls back its tentative session reservation, and publishes nothing.
- Every `publish`/`publishRejections` call for the remainder of a given
  `plan` invocation uses the resolved `Principal.principalId`, not the
  raw constant.
- Tests proving resolution, safe failure, and that published events
  carry the resolved identity.

**Explicitly out of scope, and not done, per this unit's own instructions:**

- No redesign of Planner Runtime's Plan Decision algorithm, Task Proposal
  construction, lifecycle transitions, or event sequence.
- No change to `EventBus` or `InMemoryEventBus` authentication
  (`AllowAllPrincipalAuthenticator` is untouched).
- No module access of any kind.
- No change to Memory Runtime or World Model Runtime.
- No public contract change: `PlanningSessionResult.Failed` already
  existed with a `reason` field; no new type, variant, or field was
  introduced.

## 3. What Changed

`src/runtime/InMemoryPlannerRuntime.kt`:

- `plan` now resolves `PLANNER_RUNTIME_PRINCIPAL_ID` via
  `identityService.resolve(...)` immediately after the tentative
  `CREATED` reservation, and before the pre-existing
  `request.initiatingPrincipalId` resolution check. Publisher-identity
  resolution is checked first because publishing the very first event
  (`planner.session_created`) depends on this runtime having a valid
  identity to publish under, not on who initiated the request.
- If unresolved, `plan` removes the tentative session reservation and
  returns `PlanningSessionResult.Failed` with a reason naming the
  unresolved identity explicitly -- no session record survives, no event
  is published, and `getSessionStatus` returns `null` afterward, exactly
  mirroring the existing unresolvable-initiating-Principal behaviour.
- `publish` and `publishRejections` both gained an explicit
  `publisherPrincipalId: PrincipalId` parameter; every call site within
  `plan` passes the identity resolved at the top of that call. Neither
  method resolves anything itself -- both trust their caller, consistent
  with `InMemoryAgentRuntime.publish`'s identical "already resolved by the
  time this is called" discipline.
- KDoc on the class, the companion object's `PLANNER_RUNTIME_PRINCIPAL_ID`,
  `plan`, and `publish` updated to describe the new precondition and cite
  gap #49 and this unit by name.

`tests/runtime/InMemoryPlannerRuntimeTest.kt`:

- Added `identityServiceWithPlannerRegistered()`, a suspend helper
  returning an `InMemoryIdentityService` with a `SYSTEM`-type
  `system.planner-runtime` Principal (owner `null`, status `ACTIVE`)
  already registered.
- Every pre-existing test that expects `plan` to proceed past `CREATED`
  now uses this helper in place of a bare `InMemoryIdentityService()` --
  a mechanical update; no pre-existing assertion changed in meaning.
- The two pre-existing "unresolvable initiating Principal" tests were
  updated to use the same helper (so the planner's own identity resolves
  fine) and tightened to assert the failure reason specifically names
  `initiatingPrincipalId`, keeping them isolated to that failure path now
  that a second, distinct identity-resolution failure exists.
- Three new tests added: publisher identity resolution succeeds and
  `plan` proceeds normally; an unregistered publisher identity produces a
  `Failed` result with no session record, no rejections, and no events
  published; and every published event's `publisherPrincipalId` equals
  the identity resolved through `IdentityService`.

## 4. Testing

Static count only -- no working Kotlin/Gradle toolchain was available in
this session's sandbox, and Android Studio verification is Human
authority per PES-001. `tests/runtime/InMemoryPlannerRuntimeTest.kt` goes
from 18 tests to 21 (+3; no test removed). Against the prior confirmed
total of 413/413 (Sprint 4, Track B, Unit B3), the expected total is
416/416. This number is **not considered authoritative** until confirmed
in Android Studio, per `docs/implementation/IMPLEMENTATION_HISTORY.md`'s
own entry for this unit.

## 5. Self-Traceability Review

Every type touched in this unit already existed and is unchanged in
shape: `PrincipalId`, `Principal`, `PlanningSessionResult.Failed`,
`PlannerSessionStatus`. No new public type, field, or interface was
introduced. `PLANNER_RUNTIME_PRINCIPAL_ID`'s value
(`"system.planner-runtime"`) is unchanged -- only how it is used changed,
from a raw constant passed directly to `EventBus.publish`, to a resolution
key passed to `IdentityService.resolve` first. The `publisherPrincipalId`
parameter added to `publish`/`publishRejections` is private, internal
implementation detail, not part of `PlannerRuntime`. Nothing in this unit
was invented beyond what gap #49's own description and
`InMemoryAgentRuntime`'s existing precedent authorised.

## 6. Confirmation

- **#49 is closed.** See `docs/architecture/IMPLEMENTATION_GAPS.md` #49's
  updated status and `docs/implementation/IMPLEMENTATION_HISTORY.md`'s new
  entry for this unit.
- **No EventBus redesign occurred.** `src/interfaces/EventBus.kt`,
  `src/runtime/InMemoryEventBus.kt`, and its authenticator seam are
  byte-for-byte unmodified.
- **No new module access was introduced.** No module-facing interface,
  loader, registry, or capability surface was added or changed.
- **Planner Runtime now matches Agent Runtime's identity-resolution
  discipline.** Both `InMemoryAgentRuntime.start` and
  `InMemoryPlannerRuntime.plan` now resolve their own publishing identity
  through `IdentityService` before publishing anything, and both fail
  safely (a rejection/failure result, not an exception or a silent
  no-op) when that identity does not resolve.
- **All tests pass:** not yet confirmed by Android Studio (see Section 4).
  The static, arithmetic projection is 416/416, carried forward
  unauthoritative into `IMPLEMENTATION_HISTORY.md`'s own entry pending
  human verification.

Not committed, per this unit's own instruction.
