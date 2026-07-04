# Sprint 2 Unit A1 Checkpoint

## Status

Unit:
Sprint 2 Unit A1 – Identity-Aware Permission Engine

Commit:
4ceeb0e

Android Studio:
244/244 tests passing

Date:
2026-07-04

---

## Outcome

Unit A1 completed successfully. The architecture was validated without requiring any contract or architectural changes. Sprint 2 continues as planned.

---

## 1. What Unit A1 Proved

- `DefaultPermissionEngine` (`src/runtime/DefaultPermissionEngine.kt`) is the platform's first real, non-test-fixture `PermissionEngine` implementation, and it performs identity-status enforcement: `evaluate()` resolves `request.principalId` via `IdentityService` before any other decision logic runs, closing `IMPLEMENTATION_GAPS.md` #40.
- `IdentityService` remains the single source of identity truth — `DefaultPermissionEngine` introduces no local identity store and consults `IdentityService.resolve` only.
- Suspended, Revoked, Archived, Created, and unresolved Principals are all denied before any delegated permission evaluation occurs — proven directly by `tests/runtime/DefaultPermissionEngineTest.kt`'s dedicated cases for each status, plus the unresolvable-`principalId` case.
- An Active Principal's request reaches the caller-supplied decision function unchanged, and that function's returned decision is returned unchanged (proven by reference-equality assertion, not merely structural equality).
- `DefaultExecutionPipeline` required no changes — `PermissionEngine` was already constructor-injected there, so a new implementation could be introduced without touching the pipeline itself.
- Existing runtime boundaries held unchanged: `PermissionEngine` remains the sole authority deciding APPROVED/DENIED/DEFERRED, `FakePermissionEngine` remains untouched as `DefaultExecutionPipelineTest`'s fixture, and no other component gained identity-status decision authority.
- All 234 pre-existing tests plus the new `DefaultPermissionEngineTest` cases pass (244/244), with no existing test file modified to accommodate this unit.

---

## 2. What Changed During Implementation

- **Created Principals required an explicit deny-by-default interpretation.** Neither `IdentityService.md` nor Unit A1's own instructions addressed a Principal at status `Created` (registered but never activated). This implementation chose to deny it — only `Active` reaches the supplied decision function — and recorded this as a deliberate, narrow interpretation in `DefaultPermissionEngine`'s own KDoc, mirroring the precedent already set by `IMPLEMENTATION_GAPS.md` #38's recorded interpretive decision.
- **Archived Principals also deny before policy evaluation.** `IdentityService.md`'s own normative text names only `Suspended`/`Revoked` for the short-circuit. This implementation extends the denial to `Archived` as well — a safe superset, since `PrincipalLifecycleTransitions`'s strict linear chain means a Principal cannot reach `Archived` without already having passed through `Revoked`.
- **Neither interpretation required a contract change.** Both decisions were made entirely within `DefaultPermissionEngine`'s own implementation, using `PrincipalStatus`'s existing five values exactly as already defined in `src/contracts/Principal.kt`.
- **No architecture boundary changed.** No specification, interface, or Architecture Decision was modified to accommodate either interpretation.

---

## 3. Was the Architecture Validated?

Unit A1 validated the existing architecture; it did not contradict it.

- **Assumptions that proved correct:** `PermissionEngine`'s existing interface shape (`evaluate(request): PermissionDecision`, `explain(decisionId): PermissionExplanation`) needed no change to support identity-first evaluation. `IdentityService.resolve`'s never-throws-on-not-found contract composed cleanly with a deny-on-unresolved rule. `DefaultExecutionPipeline`'s existing constructor-injection point for `PermissionEngine` was sufficient to introduce a new implementation with zero pipeline changes, confirming AD-007's boundary (Permission Engine is the sole permission authority) was already structurally sound.
- **Assumptions that proved false:** None. No existing contract, interface, or Architecture Decision was found to be incompatible with Unit A1's scope.
- **New implementation gaps discovered:** None. The two interpretive decisions in Section 2 above are documented choices within already-existing `PrincipalStatus` values, not newly discovered gaps in the architecture itself. No new numbered `IMPLEMENTATION_GAPS.md` entry was warranted by this unit.

---

## 4. Is Sprint 2 Still Correct?

- **Unit A2 remains the next implementation unit**, per `docs/implementation/SPRINT_2_IMPLEMENTATION_PLAN.md`'s own sequencing (Unit A2: Permission Policy Model and Enforcement), gated on the Permission Policy specification note that plan already names as a prerequisite.
- **Sprint sequencing remains correct.** Nothing learned during Unit A1 changes the priority order the Sprint 2 Architecture Readiness Review established (Identity/Permission first, Task Manager second, Agent Runtime third, Planner fourth, Resource Discovery fifth, Workflow Runtime excluded).
- **No reprioritisation is recommended.**

---

## 5. Risks Before Unit A2

- Permission policy is still not implemented — `DefaultPermissionEngine` delegates Active-Principal decisions to a caller-supplied placeholder function only.
- Gap #25 remains intentionally open, unaffected by this unit.
- Identity gating now exists, but policy behaviour does not: an Active Principal's request today reaches only whatever decision function a caller supplies, which carries no real authorisation rules of its own.

---

## Metrics

| Metric | Value |
|--------|-------|
| New production classes | 1 |
| New test classes | 1 |
| Existing Kotlin files modified | 0 |
| Documentation files updated | 2 |
| Android Studio tests | 244 / 244 passing |
| New implementation gaps | 0 |
| Architecture decisions changed | 0 |

---

## 6. Decision

Proceed with the Permission Policy specification required for Unit A2. Do not begin Kotlin implementation until that specification has been reviewed.

This checkpoint establishes the baseline for Sprint 2 Unit A2 and records the engineering state immediately following completion of Unit A1.
