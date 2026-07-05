# Sprint 2 Unit A2 Checkpoint

## Status

Unit:
Sprint 2 Unit A2 – Permission Policy

Commit:
e7e1bbf

Android Studio:
253/253 tests passing

Date:
2026-07-05

---

## 1. What Unit A2 Proved

- `docs/specifications/volume-03-core-interfaces/PermissionPolicy.md` could be implemented in full without changing the specification itself — `src/runtime/DefaultPermissionPolicy.kt` implements every section of it (rule shape, conservative DENIED defaults, `APPROVED_WITH_CONFIRMATION` as an outcome only, Policy Invariants, Policy Evaluation Principles) with no amendment to the document.
- `DefaultPermissionEngine` (`src/runtime/DefaultPermissionEngine.kt`) now delegates every Active-principal request to `DefaultPermissionPolicy.evaluate(request)`, unchanged from Unit A1's identity-status gating — the ACTIVE branch of `evaluate()`'s `when` is the only line that changed to make this true.
- Identity and Permission remain cleanly separated: `DefaultPermissionPolicy` never calls `IdentityService`, never resolves a Principal, and never sees `PrincipalStatus`; `DefaultPermissionEngineTest.kt`'s tests confirm the policy is not consulted at all for Suspended, Revoked, Archived, Created, or unresolvable Principals.
- Policy evaluation is deterministic — `DefaultPermissionPolicyTest.kt`'s repeated-evaluation test confirms two calls with the same `ExecutionRequest` produce the same `decisionId`, `action`, `decision`, and `level`.
- Policy evaluation has no side effects — confirmed by a dedicated test wrapping `ResourceRegistry` and asserting `register`/`update`/`listByOwner` are never called during `evaluate`, only `resolve`.
- Conservative DENIED defaults work as intended — unknown action, unknown resource, and "no rule addresses this action/resource pair" (Unknown Permission) all independently produce `DENIED`, each proven by its own test case.
- `APPROVED_WITH_CONFIRMATION` exists as a policy outcome only — a rule can produce it, and `DefaultPermissionPolicy` returns it unchanged, but no confirmation UI, workflow, or follow-up mechanism was implemented anywhere in this unit.
- Android Studio passed 253/253 tests: the 244 tests present at the close of Unit A1, plus 9 new cases in `DefaultPermissionPolicyTest.kt`, with the 10 pre-existing `DefaultPermissionEngineTest.kt` cases retained (adapted to the new constructor, not removed or reduced in number).

---

## 2. What Changed During Implementation

- **`ActionMapper` and `ResourceRegistry` were reused, not duplicated.** `DefaultPermissionPolicy` takes both as constructor dependencies and re-derives the same `PermissionAction`/`ResourceType` pairs `DefaultExecutionPipeline` already computes moments earlier for the same request, rather than inventing a second, independently-maintained mapping. This was a genuine pre-code ambiguity (the existing `PermissionEngine.evaluate(request)` signature does not carry an already-resolved action/resource pair) and was resolved by an explicit stop-and-report before coding began, not decided unilaterally.
- **Unknown action, unknown resource, and unknown permission naturally collapsed into one "no policy match → DENIED" path.** Once action/resource resolution is delegated to `ActionMapper`/`ResourceRegistry`, there is no separate field anywhere in the existing contracts (`ExecutionRequest`, `PermissionDecision`) representing a "requested permission" distinct from a resolved (action, resourceType) pair. This was also a genuine pre-code ambiguity, resolved the same way (stop-and-report, then confirmed) rather than assumed.
- **Gap #30's accepted "most restrictive wins" simplification proved sufficient for this unit.** `ActionMapper.map` can return more than one resolved `(action, resourceType)` pair for a single request (composite actions), but `PermissionEngine.evaluate` still returns exactly one `PermissionDecision`. Collapsing multiple resolved pairs by restrictiveness (`DENIED` < `DEFERRED` < `APPROVED_WITH_CONFIRMATION` < `APPROVED`) required no change to `PermissionDecision`'s shape or to gap #30's own already-documented scope boundary.
- **A test-harness-only coroutine defect was discovered and repaired after the first Android Studio run.** A default parameter value in `DefaultPermissionEngineTest.kt` called a suspend function (`ResourceRegistry.register`) directly inside a parameter-default expression, which the Kotlin coroutines compiler does not support ("Unsupported suspend function calls in a coroutine context"). The suspend call was moved into the function body as an ordinary statement. This was a test-harness defect only — it required no change to `DefaultPermissionEngine.kt` or `DefaultPermissionPolicy.kt`, and no test assertion changed as a result.

---

## 3. Was the Architecture Validated?

Unit A2 confirmed the existing architecture; it did not require changing it.

- **No Architecture Decision required modification.** AD-007 (Permission Decisions Belong to the Permission Engine) and AD-015 (Invalid Is Not Denied) both held: `DefaultPermissionPolicy` is the sole source of policy-driven decisions for an Active Principal, and the "Invalid, not Denied" boundary for action-mapping failures remains intact upstream in `DefaultExecutionPipeline` — `DefaultPermissionPolicy` only ever sees requests that already passed that boundary.
- **No specification proved incorrect.** `PermissionPolicy.md` was implemented section-by-section with no amendment required.
- **No redesign was required.** The two ambiguities in Section 2 were resolved by interpretation within existing contracts, not by changing any interface.
- **No runtime contract changed.** `PermissionEngine`'s interface (`evaluate`, `explain`) is exactly as it was after Unit A1; `ActionMapper`, `ResourceRegistry`, `IdentityService`, `ExecutionRequest`, and `PermissionDecision` are all unmodified by this unit.

---

## 4. Did Sprint Sequencing Change?

`docs/implementation/SPRINT_2_IMPLEMENTATION_PLAN.md`'s Track A (Identity and Permission Hardening) contains exactly two units, A1 and A2. There is no Unit A3 in the plan — Track A is now complete. This checkpoint does not invent one.

- Nothing discovered during Unit A2 exposes a new dependency requiring reprioritisation of Tracks B–E.
- Per the plan's own sequence (Section 5), the next scheduled work is Track B (Task Manager Event Handling), which is gated on logging a new numbered `IMPLEMENTATION_GAPS.md` entry for "Task Manager Agent-Event subscription" (Section 4 of the plan) before Unit B1 may begin. That gap entry has not been logged in this session.
- **Recommendation: Sprint 2 continues unchanged.** Track A closes as scoped; the immediate next action per the existing plan is the Track B gap-log prerequisite, not a new Track A unit.

---

## 5. Implementation Gaps

- **Gap #25** — closed by this unit (`IMPLEMENTATION_GAPS.md`, prior update).
- **Gap #30** — unchanged; its "most restrictive wins" composite-action simplification is cited, not altered, by `DefaultPermissionPolicy`.
- **Gap #35** — unchanged, still requires human decision.
- **Gap #36** — unchanged, still requires human decision.
- **Gap #37** — unchanged, still requires human decision.
- **Gap #41** — unchanged, still requires human decision.

No new numbered gap was opened by this unit. The gap file itself is not modified by this checkpoint.

---

## 6. Technical Debt Deliberately Carried Forward

- No RBAC (Role-Based Access Control).
- No ABAC (Attribute-Based Access Control).
- No delegated authority.
- No temporary permissions.
- No policy persistence.
- No policy editing.
- No organisation policy.
- No plugin policy.

Each of these was consciously excluded, not overlooked: `PermissionPolicy.md`'s own Non-Goals and Future Considerations sections name all eight as out of scope for the policy model this unit implements. `DefaultPermissionPolicy`'s fixed, caller-supplied `List<PermissionPolicyRule>` is the policy *mechanism* the specification describes, not a shipped, real-world policy — populating it with actual organisational rules remains future work, deliberately deferred.

---

## 7. Sprint Health

- **Architecture stability:** Stable. Two consecutive units (A1, A2) completed with zero Architecture Decision changes and zero interface changes to `PermissionEngine`, `IdentityService`, `ActionMapper`, or `ResourceRegistry`.
- **Implementation quality:** `DefaultPermissionPolicy` and `DefaultPermissionEngine` are small, single-purpose classes with every interpretive decision recorded in KDoc, mirroring the project's established convention.
- **Documentation quality:** Specification-first discipline held — `PermissionPolicy.md` was reviewed and approved before any Kotlin was written, and gap/history documentation was updated only after Android Studio confirmed the build.
- **Test coverage:** 253/253 passing, with 19 tests specifically dedicated to the identity-gate/policy boundary (10 in `DefaultPermissionEngineTest.kt`, 9 in `DefaultPermissionPolicyTest.kt`).
- **Risk level entering next work:** Low. Track A is closed as scoped. The one open item is administrative (the Track B gap-log entry), not a technical or architectural risk.

---

## 8. Decision

**Proceed unchanged.**

Both Track A units closed exactly as scoped in `SPRINT_2_IMPLEMENTATION_PLAN.md`, with no architecture changes, no specification changes, and no regressions (253/253). Sprint 2's priority order established by the Architecture Readiness Review is unaffected. The next action is the Track B prerequisite already named in the plan — logging a new gap entry for Task Manager Agent-Event subscription — not a reprioritisation and not a new Track A unit.
