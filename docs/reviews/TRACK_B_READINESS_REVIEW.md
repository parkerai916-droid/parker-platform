# Sprint 2 Track B Readiness Review

## Status

Repository baseline:

Architecture:
v1.1

Sprint:
Sprint 2

Track A:
Complete

Android Studio:
253/253 tests passing

Latest implementation:
Sprint 2 Unit A2

---

## 1. Purpose

Track A established Parker's trust foundation: identity-status enforcement (`DefaultPermissionEngine`, Unit A1) and policy-driven authorisation (`DefaultPermissionPolicy`, Unit A2), both delegated to from `PermissionEngine.evaluate` in that order.

Track B begins coordination between runtime components: `InMemoryTaskManagerRuntime` subscribing to Agent Run lifecycle events (`docs/implementation/SPRINT_2_IMPLEMENTATION_PLAN.md`, Unit B1) and applying a fixed rule to Task status in response (Unit B2).

The purpose of this review is to verify, using only what is implemented in the repository today, that Track A provides every dependency Track B's own plan says it needs, and to surface — without inventing or resolving them — any gaps a Track B implementer would otherwise discover mid-unit.

---

## 2. Track A Deliverables

| Component | Implemented | Verified | Architecturally stable |
|---|---|---|---|
| Execution Pipeline (`src/runtime/DefaultExecutionPipeline.kt`) | Yes | Yes — `tests/runtime/DefaultExecutionPipelineTest.kt`, `tests/runtime/VerticalSliceEndToEndTest.kt` | Yes — unchanged by Units A1/A2; `PermissionEngine` remains constructor-injected |
| Tool Registry (`src/runtime/InMemoryToolRegistry.kt`) | Yes | Yes — `tests/runtime/InMemoryToolRegistryTest.kt` | Yes — unchanged by Sprint 2 to date |
| ToolInvocationBinding (`src/runtime/InMemoryToolInvocationBinding.kt`) | Yes | Yes — `tests/runtime/InMemoryToolInvocationBindingTest.kt` | Yes — unchanged; gap #41 (convention-only access restriction) remains open but does not affect Track B |
| Resource Registry (`src/runtime/InMemoryResourceRegistry.kt`) | Yes | Yes — `tests/runtime/InMemoryResourceRegistryTest.kt`, plus new read-only consumption by `DefaultPermissionPolicy` in Unit A2 | Yes — Unit A2 added a new caller (`DefaultPermissionPolicy`) but changed no method on the interface or implementation |
| Identity Service (`src/runtime/InMemoryIdentityService.kt`) | Yes | Yes — `tests/runtime/InMemoryIdentityServiceTest.kt` | Yes — unchanged by Units A1/A2; consulted, not modified, by `DefaultPermissionEngine` |
| Default Permission Engine (`src/runtime/DefaultPermissionEngine.kt`) | Yes | Yes — `tests/runtime/DefaultPermissionEngineTest.kt` (10 cases) | Yes — Unit A1's identity gate, Unit A2's delegation to policy; `PermissionEngine` interface itself unchanged |
| Permission Policy (`src/runtime/DefaultPermissionPolicy.kt`) | Yes | Yes — `tests/runtime/DefaultPermissionPolicyTest.kt` (9 cases) | Yes — implements `docs/specifications/volume-03-core-interfaces/PermissionPolicy.md` without amending it |
| EventBus (`src/runtime/InMemoryEventBus.kt`) | Yes | Yes — `tests/runtime/InMemoryEventBusTest.kt` | Yes — unchanged since Sprint 1 Unit 9 / the targeted refinement pass (gap #27); gap #26 (authentication/signature placeholders) remains open but is a pre-existing, documented limitation, not a Track A regression |

All eight components have a production implementation and a dedicated test file. None was modified by this review.

---

## 3. Runtime Dependencies

Reviewed against Track B's own stated dependencies (`SPRINT_2_IMPLEMENTATION_PLAN.md`, Unit B1 "Architecture dependencies" and Unit B2 "Architecture dependencies"):

| Dependency | Status |
|---|---|
| `EventBus` / `InMemoryEventBus` | Exists |
| `InMemoryTaskManagerRuntime` already holding an `EventBus` constructor dependency (from Sprint 1 Unit 9, for publishing `task.*` events) | Exists |
| `TaskManagerRuntimeSpecification.md` §6/§11 (approved, unmodified) | Exists |
| `TaskLifecycleTransitions` (`src/contracts/TaskLifecycle.kt`) | Exists |
| `agent.completed` event, actually published by `InMemoryAgentRuntime` | Exists |
| `agent.failed` event, actually published by `InMemoryAgentRuntime` | Exists |
| `agent.cancelled` event | **Missing.** `InMemoryAgentRuntime`'s own class KDoc states it "only ever drives `CREATED -> INITIALISED -> READY -> RUNNING -> {COMPLETED, FAILED}`" — no code path transitions an `AgentRun` to `CANCELLED`, so nothing publishes this event today, even though `AgentRunStatus.CANCELLED` exists in the lifecycle enum. |
| `agent.action_denied` event | **Missing.** No corresponding `AgentRunStatus` value or code path exists for this event at all. |
| `agent.action_deferred` event | **Missing.** Same as above. |
| The new numbered gap entry for "Task Manager Agent-Event subscription" (named in the plan's Section 4, to be logged before Unit B1 begins) | **Missing** — not yet logged. Per the plan, logging it is Unit B1's own first documentation step, not a pre-existing item this review can find already done. |

`EventType` (`src/contracts/EventContracts.kt`) is an open, namespaced string, not a closed enum, so subscribing to `agent.cancelled`/`agent.action_denied`/`agent.action_deferred` is not blocked by the type system. What is missing is a production emitter for three of the five event types Unit B1 is scoped to subscribe to.

---

## 4. Implementation Gaps

Only gaps relevant to Track B are assessed. No gap is modified by this review.

- **Task Manager Agent-Event subscription bookkeeping item** (plan Section 4, not yet a numbered gap): **Deferred by design** — the plan itself assigns logging this entry to Unit B1, not to a pre-condition of starting it.
- **Gap #25** (Action Mapping wired into `PermissionEngine.evaluate` via policy): **Closed** (Sprint 2 Unit A2). Relevant only as trust-foundation context; not a Track B input.
- **Gap #26** (EventBus authentication and signature verification are placeholders): **Open / deliberate stand-in.** Track B's Unit B1 depends on `EventBus.subscribe`/`publish`, which this gap concerns — the dependency exists and functions, but carries no real trust decision on publishers or subscribers. This does not block subscription/receipt mechanics, which is all Unit B1 scopes.
- **Gap #30** (`PermissionEngine.evaluate`'s signature vs. "once per action" description): **Partially resolved**, per the Phase 2 Gap Closure Summary. Not relevant to Task Manager event handling; Track B does not call `PermissionEngine.evaluate`.
- **Gaps #35, #36, #37** (cascading revocation, Principal lifecycle edge set, `resolve()` suppression): **Require human decision.** The plan explicitly states these are independent of Track A and do not block Track A or Track B.
- **Gap #41** (`ToolInvocationBinding`/`ToolRegistry.resolve` convention-only access restriction): **Requires human decision.** Concerns the Execution Pipeline/Tool Registry boundary, not Task Manager event handling; does not block Track B.

No relevant gap blocks Track B's stated scope (subscription and receipt in Unit B1; a fixed, minimal completion rule in Unit B2).

---

## 5. Architecture Validation

Track A's implementation did not change any approved architecture.

- **Did any Architecture Decision require revision?** No. AD-007 (Permission Decisions Belong to the Permission Engine) and AD-015 (Invalid Is Not Denied) both held across Units A1 and A2, confirmed in both units' own checkpoints.
- **Did any specification prove incorrect?** No. `PermissionPolicy.md` was implemented section-by-section with no amendment. `TaskManagerRuntimeSpecification.md` §6/§11, which Track B depends on, is untouched by Track A and remains as approved.
- **Did implementation invalidate any runtime contract?** No. `PermissionEngine`, `IdentityService`, `EventBus`, `ActionMapper`, and `ResourceRegistry` all retain the interfaces they had entering Sprint 2.

This matches the conclusion already recorded in `docs/reviews/SPRINT_2_A1_CHECKPOINT.md` and `docs/reviews/SPRINT_2_A2_CHECKPOINT.md`; this review finds no evidence to revise it.

---

## 6. Risks Entering Track B

- **Architectural risk:** None identified. Track A introduced no interface or contract Track B would need to build against incorrectly.
- **Implementation risk:** Three of the five event types Unit B1 is scoped to subscribe to (`agent.cancelled`, `agent.action_denied`, `agent.action_deferred`) are not produced by any current code path (Section 3). Unit B1's own acceptance criterion only exercises `agent.completed`, and Unit B2's negative test only names `agent.failed`, so this does not block either unit as scoped — but genuine end-to-end coverage of cancellation/denial/deferral will remain limited to directly-published synthetic test events until `InMemoryAgentRuntime` itself grows those transitions, which is outside Track B's scope.
- **Documentation risk:** Low. The Task Manager Agent-Event subscription gap entry has not been logged, but the plan assigns that step to Unit B1 itself, not to a readiness precondition.
- **Testing risk:** Low. The existing 253/253 suite is a stable regression baseline, and none of Track A's tests touch `InMemoryTaskManagerRuntime` or `InMemoryAgentRuntime`'s event-publishing paths, so Track B work is unlikely to interact with Track A's test surface.

---

## 7. Definition of Track B Ready

- Trust layer complete (Identity + Permission): **Met** — Units A1 and A2 both complete.
- Permission Policy implemented: **Met** — `DefaultPermissionPolicy`.
- Execution path verified: **Met** — `DefaultExecutionPipeline` plus `VerticalSliceEndToEndTest.kt`.
- Required runtime contracts implemented: **Met** — `EventBus`, `InMemoryTaskManagerRuntime`, `InMemoryAgentRuntime`, `TaskLifecycleTransitions` all exist and are tested.
- Android Studio passing: **Met** — 253/253.
- No unresolved blocker preventing Track B: **Met**, with the Section 3/6 finding noted as a scoping fact already anticipated by Unit B1's own text, not an unresolved blocker.

---

## 8. Recommendation

**Proceed with Track B unchanged.**

Every dependency Track B's own plan names as required (`EventBus`, `TaskManagerRuntimeSpecification.md` §6/§11, `TaskLifecycleTransitions`, the `agent.completed`/`agent.failed` events Unit B1's acceptance criteria actually exercise) exists, is implemented, and is verified by 253/253 passing tests. Track A closed both its units exactly as scoped, with no Architecture Decision, specification, or runtime contract requiring change (Section 5). The one repository-evidenced gap — three of five named agent event types having no production emitter yet — is not a blocker: it falls within Unit B1's own "subscription and receipt only" scope and its acceptance criteria already limit verification to `agent.completed`. No sequencing change is warranted.
