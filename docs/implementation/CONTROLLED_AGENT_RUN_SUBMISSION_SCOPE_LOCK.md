# Controlled Agent Run Submission — Scope Lock

**Status:** Approved; amended twice (Section 15, "Implementation Discipline"; and the "Amendment — Two-Phase Agent Run Operation" at the end of this document, following the Contract Design's own Amendment 1). No source files modified by this document. Nothing staged, committed, or pushed. **Implementation of the two-phase amendment may begin only after that amendment is explicitly approved, in addition to this document's own original approval.**

**Authoritative inputs:**
- `docs/architecture/CONTROLLED_AGENT_RUN_SUBMISSION_GOVERNANCE_REVIEW.md`
- `docs/architecture/CONTROLLED_AGENT_RUN_SUBMISSION_CONTRACT_DESIGN.md`

This document resolves the three items the Contract Design deliberately left open, corrects one internal inconsistency the Contract Design's own wording created, and freezes every remaining implementation boundary so that Implementation is a mechanical act, not a design act.

**Repository baseline:** Branch `main`, commit `992538e`, working tree clean, synchronized with `origin/main`.

---

## 1. Resolution: Permission-Denied or Deferred `START`

The Contract Design's Section 5 left open whether a permission-denied `START` results in an `AgentRun` record at `CREATED` (mirroring the identity-rejection precedent) or at `FAILED`. Both options assumed a record is created. That assumption is now corrected and the question is closed on stricter terms:

**Frozen:**

- The run-initiation permission evaluation occurs before any `AgentRun` record is created — not merely before the record leaves `CREATED`.
- `PermissionDecisionOutcome.DENIED` and `PermissionDecisionOutcome.DEFERRED` both produce `AgentRunCommandResult.Rejected` immediately.
- **No entry is written to `InMemoryAgentRuntime`'s internal `agentRuns` map.** `getAgentRun(agentRunId)` for this `taskId`'s deterministic ID returns `null` after a rejection, exactly as it would for an ID that was never submitted at all.
- **No `AgentRunStatus` exists or is assigned.** There is no `CREATED` record to be "stuck" anywhere — the Contract Design's CREATED-vs-FAILED question is void, not merely resolved in favour of one option.
- The originating `Task` remains at `QUEUED`.
- `task.agent_run_rejected` is published (Section 10) carrying the exact `AgentRunCommandResult.Rejected.reason` string and `commandType.name`.
- Agent Identity resolution (`identityService.resolve("agent-for-${taskId.value}")`) is **not attempted**.
- `agentStepSource.nextStep(...)` is **not invoked**.
- **No `agent.*` lifecycle event is published** — not `agent.created`, not any other. The only event this rejection produces anywhere in the system is `task.agent_run_rejected`, published by `InMemoryTaskManagerRuntime`, not `InMemoryAgentRuntime`.

**Exact revised sequence inside `InMemoryAgentRuntime.start()`, under the existing single `mutex.withLock` block:**

1. Compute the deterministic `agentRunId` (`"run-for-${command.taskId.value}"`) as a local value — **not yet inserted into `agentRuns`**.
2. `check(agentRunId !in agentRuns)` — existing duplicate-`START` protection, unchanged (Section 8, item 8).
3. **New:** construct the synthetic `ExecutionRequest` (Section 4) and call `permissionEngine.evaluate(request)`. On `DENIED`/`DEFERRED`: return `AgentRunCommandResult.Rejected` now. **Nothing has been written to `agentRuns`; nothing else has happened.**
4. Only on `APPROVED`/`APPROVED_WITH_CONFIRMATION`: insert the new `AgentRun` record into `agentRuns` at `CREATED` — this is the first point at which any record exists, unchanged in shape from today's implementation.
5. Agent Identity resolution proceeds exactly as it does today, against the now-existing `CREATED` record — including its own existing, unchanged "stuck at `CREATED`" outcome for an unresolvable or invalid Agent Identity. This design does not touch that behaviour; it only moves the point at which a record starts existing at all to be strictly after step 3.
6. Lifecycle drive and multi-step loop proceed exactly as today.

**Consequence, named explicitly rather than left implicit:** because no record is ever created for a permission-denied `START`, the existing duplicate-`START` check (`agentRunId !in agentRuns`) does not block a hypothetical later resubmission attempt for the same `taskId` after a rejection — only an `Accepted` `START` ever occupies the map and triggers duplicate protection. This is a correct, intended consequence of "no record is created," not a gap: this milestone builds no retry mechanism (Contract Design Section 14, restated in Section 7 below), so nothing in production actually exercises this path today, but Scope Lock records it so it is never mistaken for an oversight during Native Verification or a later review.

---

## 2. Resolution: Integration Test Name

**Frozen, no further discussion:**

- File: `tests/runtime/TaskManagerAgentRunSubmissionIntegrationTest.kt`
- Class: `TaskManagerAgentRunSubmissionIntegrationTest`

---

## 3. Resolution: Production Permission Configuration

**Frozen additions, both in `src/composition/ParkerRuntime.kt`'s existing `buildAndRegisterRuntimeGraph()`, extending — not replacing — the one existing precedent of the same shape (the `NOTIFY`/`TOOL` rule already composed for the Local Text Channel's `deliver` Tool, `ParkerRuntime.kt:299-310`):**

**3.1 `ActionVocabulary` entry.** The existing verb-phrase convention in this file is natural free text (`NOTIFY_OWNER_VERB_PHRASE = "notify owner"`, not a dotted machine-style string) — the Contract Design's proposed `"agent_run.start"` is corrected here to match the one real precedent that exists:

```
const val AGENT_RUN_START_VERB_PHRASE = "start agent run"
```

registered in the existing `stage("action vocabulary registration")` block, alongside (not replacing) the existing `NOTIFY_OWNER_VERB_PHRASE` registration:

```
vocabulary.register(
    ActionVocabularyEntry(
        verbPhrase = AGENT_RUN_START_VERB_PHRASE,
        mappings = setOf(ActionResourceMapping(PermissionAction.EXECUTE, ResourceType.AGENT)),
    ),
)
```

**3.2 `PermissionPolicyRule`.** One new entry appended to the existing `rules = listOf(...)` passed to `DefaultPermissionPolicy` (`ParkerRuntime.kt:302-309`) — the list gains a second element, its first (`NOTIFY`/`TOOL`) unchanged:

```
PermissionPolicyRule(
    action = PermissionAction.EXECUTE,
    resourceType = ResourceType.AGENT,
    outcome = PermissionDecisionOutcome.APPROVED,
    level = PermissionLevel.AUTOMATIC,
),
```

**3.3 Owner-scoping — resolved honestly, not overstated.** `PermissionPolicyRule` is matched purely by `(action, resourceType)` (`DefaultPermissionPolicy.ruleOutcomeFor`, `DefaultPermissionPolicy.kt:145-148`) — it carries no `principalId` field and the engine has no mechanism to scope a rule to a specific Principal or to "the resolved owner of this Task" specifically. This document does not pretend otherwise. The rule above is, by itself, a blanket `(EXECUTE, AGENT) -> APPROVED` grant for whichever Principal is evaluated. **Owner-scoping is enforced by call-site structure, not by rule content:**

- Decision 2 (frozen, unchanged) makes `InMemoryTaskManagerRuntime` the sole production caller of `AgentRunCommandChannel.submit()`.
- `InMemoryTaskManagerRuntime.submitProposal()` sets `AgentRunCommand.requestingPrincipalId = owner.principalId` — always and only the Task's own resolved owner (`identityService.resolve(proposal.proposedOwnerPrincipalId)`), never an arbitrary, unauthenticated, or externally-supplied value.
- Therefore the only Principal this rule can ever be evaluated against, through the only production path that exists, is a Task's own legitimately-resolved owner. The rule's *narrowness* is in what it grants (Section 3.4) and in what resource it targets (Section 4) — its scoping to "the owner" is a property of the system's current single call site, not of the policy rule's own matching logic. If a second production caller of `AgentRunCommandChannel.submit()` is ever introduced, this scoping argument must be re-examined; it is not a property Scope Lock can freeze into the rule itself, only into the call graph as it exists today.

**3.4 What the rule grants and does not grant — restated as the load-bearing distinction this whole design rests on:**

**`START` authorisation permits creation and commencement of the governed Agent Run. It does not authorise any proposed action inside that run.** The rule above answers exactly one question — "may this Principal cause an Agent Run to be created and begin at all" — and nothing else. Every action that Agent Run subsequently proposes still passes through `ExecutionPipeline.submit()` → `PermissionEngine.evaluate()` independently, exactly as today (Section 7, item 8), evaluated against whatever `(action, resourceType)` pair that specific proposed action and its target Resource resolve to — never against the `(EXECUTE, AGENT)` rule above, which has no bearing on any Resource other than the one fixed boundary Resource named in Section 4. This is not a new argument invented for this document; it is `AgentRunCommand`'s own existing KDoc restated exactly: "Neither outcome grants execution authority by itself... accepting a `START` command means an Agent Run now exists and will begin its own lifecycle, not that any action it later proposes is pre-approved" (`AgentRunCommand.kt:166-169`, unchanged by this milestone).

---

## 4. Resolution: Permission Request Resource Shape

The Contract Design's Section 3 proposed populating the synthetic `ExecutionRequest.targetResources` from `command.resourceReferences` — the Governance Review and this Scope Lock's own instructions correctly reject this: `resourceReferences` is caller-supplied, frequently empty today, and has no necessary relationship to "the Agent Run subsystem as a whole." This is resolved as follows.

**Frozen: a single, deterministic, pre-registered, singleton Resource represents the Agent Runtime's own execution boundary — never a specific Agent Run, never persisted as though any specific run already exists.**

```
const val AGENT_RUNTIME_BOUNDARY_RESOURCE_ID = ResourceId("resource-agent-runtime-boundary")
```

Registered exactly once, at composition-root startup, in a new `stage("agent runtime boundary resource registration")` block in `ParkerRuntime.kt`'s `buildAndRegisterRuntimeGraph()`, placed after `registerSystemIdentities(identityService)` (so `SYSTEM_PARKER_PRINCIPAL_ID` already exists) and before `permissionPolicy`/`executionPipeline` construction:

```
resourceRegistry.register(
    Resource(
        resourceId = AGENT_RUNTIME_BOUNDARY_RESOURCE_ID,
        resourceType = ResourceType.AGENT,
        displayName = "Agent Runtime Execution Boundary",
        ownerPrincipalId = SYSTEM_PARKER_PRINCIPAL_ID,
        sensitivity = ResourceSensitivity.PUBLIC,
        lifecycleState = ResourceLifecycleState.REGISTERED,
        createdAt = now,
        updatedAt = now,
        source = "composition-root:agent-runtime-boundary",
    ),
)
```

mirroring `InMemoryModuleRegistry.moduleToolResource`'s own established shape for a composition-registered Resource exactly (`InMemoryModuleRegistry.kt:203-213`) — same `ResourceSensitivity.PUBLIC` default reasoning (a system capability surface, not personal/household/financial/medical data), same `ResourceLifecycleState.REGISTERED` value at registration, same descriptive `source` tagging convention. **Zero new system identity registrations**: `SYSTEM_PARKER_PRINCIPAL_ID` is already registered by the existing `registerSystemIdentities` call; this Resource simply names it as owner, consistent with "the platform itself, not any specific runtime component" holding the boundary.

**Why this satisfies "must not be persisted as though the Agent Run already exists":** this Resource is registered once, at startup, before any `TaskProposal` is ever submitted, and is never created, updated, or referenced per-Task or per-run. It represents the fixed capability surface "Parker's Agent Runtime exists and can be asked to start Agent Runs" — not any specific Agent Run, which still does not exist until `InMemoryAgentRuntime.start()` itself creates one, strictly after this permission check succeeds (Section 1).

**Revised synthetic `ExecutionRequest` (supersedes Contract Design Section 3's `targetResources` line only — every other field is unchanged):**

```
ExecutionRequest(
    requestId = RequestId("run-init-${command.taskId.value}"),
    principalId = command.requestingPrincipalId,
    origin = RequestOrigin.AGENT,
    intent = "Start Agent Run for Task '${command.taskId.value}': ${command.goalDescription}",
    targetResources = listOf(runInitiationResourceId),   // fixed constant, NOT command.resourceReferences
    proposedActions = listOf(AGENT_RUN_START_VERB_PHRASE),
    priority = RequestPriority.NORMAL,
    createdAt = Instant.now(),
    correlationId = command.correlationId,
)
```

**Constructor threading, corrected from the Contract Design's Section 2.** `AGENT_RUNTIME_BOUNDARY_RESOURCE_ID` is a `private companion object` value in `ParkerRuntime.kt`; `InMemoryAgentRuntime` lives in a different file and cannot reference a private composition-root constant, and per this project's own already-established precedent (Plan Candidate to Planner Integration Scope Lock's system-principal-ID resolution: "must NOT use reflection or expose private constants merely to avoid writing an explicit composition value"), the correct resolution is an explicit constructor parameter, not a shared literal or a loosened visibility modifier. `InMemoryAgentRuntime`'s constructor, as frozen by the Contract Design plus this correction, is therefore:

```
class InMemoryAgentRuntime(
    private val identityService: IdentityService,
    private val executionPipeline: ExecutionPipeline,
    private val eventBus: EventBus,
    private val agentStepSource: AgentStepSource,
    private val agentPolicy: AgentPolicy,
    private val permissionEngine: PermissionEngine,          // Contract Design Section 2
    private val runInitiationResourceId: ResourceId,         // NEW — this Scope Lock's own addition
) : AgentRunCommandChannel
```

`ParkerRuntime.kt` passes `AGENT_RUNTIME_BOUNDARY_RESOURCE_ID` (Section 4, above) as this argument. This is a seven-parameter constructor, not the six-parameter one the Contract Design named — this correction is recorded here explicitly so Implementation does not silently reconcile a discrepancy between the two documents without it being visible.

---

## 5. Frozen Accepted Flow Ordering

```
Task created
  -> Task QUEUED
  -> AgentRunCommand.START constructed
  -> run-initiation permission evaluated                         [Section 1, step 3]
  -> Agent Identity resolved                                     [Section 1, step 5]
  -> Agent Run created and accepted                               (AgentRunCommandResult.Accepted)
  -> task.agent_run_started published                             [Section 10]
  -> Task QUEUED to RUNNING
  -> existing Agent Runtime lifecycle continues                   (unchanged: INITIALISED -> READY -> RUNNING -> runLoop)
```

No step may be reordered, skipped, or parallelised. "Agent Run created and accepted" (the fourth step) is the same event as "insert into `agentRuns` at `CREATED`" from Section 1, step 4 — restated here at the flow level, not a distinct additional step.

---

## 6. Frozen Rejected Flow Ordering

```
Task created
  -> Task QUEUED
  -> AgentRunCommand.START constructed
  -> run-initiation permission denied or deferred                 [Section 1, step 3]
  -> no Agent Run created                                          [Section 1 — no map entry, no AgentRunStatus]
  -> task.agent_run_rejected published                             [Section 10]
  -> Task remains QUEUED
  -> TaskProposalDisposition.Accepted returned
```

`TaskProposalDisposition.Accepted` is returned in **both** the accepted and rejected flows (Contract Design Section 6.1's own resolution, unchanged): proposal intake and run authorisation are independently reported outcomes. This is restated here so the rejected flow's final step is never mistaken for an inconsistency.

---

## 7. Preserved Unchanged — Restated as an Explicit Checklist

Each item below is unchanged by this milestone. Where a file in Section 8 is nonetheless touched for an unrelated reason (a new constructor parameter, for instance), the specific behaviour named here is not among the changes made to it.

- **Per-action Trust-gating inside `InMemoryAgentRuntime.runLoop()`** — every `AgentStepDecision.Propose` still becomes a real `ExecutionRequest` submitted through `executionPipeline.submit()` → `permissionEngine.evaluate()`, completely independent of the new run-initiation check (Section 3.4).
- **Existing duplicate-`START` rejection** — `check(agentRunId !in agentRuns)` remains the mechanism (Section 1's revised sequence keeps it as step 2, unmoved in relative order relative to identity resolution and lifecycle progression — only the point at which a record is written moves, not the duplicate-check itself).
- **Existing `agent.completed` handling** — `InMemoryTaskManagerRuntime`'s `init` block subscriptions to `agent.completed`/`agent.failed` are untouched.
- **Existing `RUNNING -> COMPLETED` transition** — `applyCompletedTransition`'s existing `TaskStatus.RUNNING` branch is untouched and requires no change, per the compatibility argument already verified in Contract Design Section 6.1.
- **`SUSPEND`, `RESUME`, and `CANCEL` behaviour** — entirely untouched; this milestone concerns `START` only.
- **All Contract Design Section 14 non-goals** — no new `AgentRunCommandResult` variant, no new `PermissionAction`/`ResourceType` enum value beyond reusing `EXECUTE`/`AGENT`, no `ActionVocabulary`/`PermissionPolicyRule` content beyond the one narrow rule this document itself authorises (Section 3, an explicit, bounded exception the Contract Design left for Scope Lock to decide — decided here as "yes, author the minimum viable rule now," not deferred), no real Planner-backed `AgentStepSource`, no retry mechanism, no changes to the other four `TaskProposalDisposition` outcomes, no scheduling/Workflow Engine/multi-agent/distributed-execution/retry-policy/plugin/Home-Assistant/Android/unrelated-refactoring work.

---

## 8. Permitted File Boundary

**Production files — modify:**
- `src/runtime/InMemoryTaskManagerRuntime.kt` — new constructor parameter, `submit()` call, two new event-publish branches, one new `TaskLifecycleTransitions` call site.
- `src/runtime/InMemoryAgentRuntime.kt` — two new constructor parameters (`permissionEngine`, `runInitiationResourceId`), the revised `start()` sequence (Section 1).
- `src/composition/ParkerRuntime.kt` — new companion constants (`AGENT_RUN_START_VERB_PHRASE`, `AGENT_RUNTIME_BOUNDARY_RESOURCE_ID`, `DEFAULT_AGENT_POLICY`), new resource-registration stage, extended vocabulary-registration stage, extended `rules` list, construction of `DeterministicAgentStepSource` and `InMemoryAgentRuntime`, reordered construction (agent runtime before Task Manager runtime), new constructor arguments threaded through.

**Production files — KDoc-only correction (no behavioural change):**
- `src/contracts/AgentRunCommand.kt` — correct `AgentRunCommandChannel`'s stale "no implementation exists" claim.
- `src/contracts/AgentStep.kt` — correct the `FixedSequenceAgentStepSource` naming citation to name `DeterministicAgentStepSource`.

**Production files — new:**
- `src/runtime/DeterministicAgentStepSource.kt`

**Documentation files — modify:**
- `docs/specifications/volume-05-task-manager-runtime/TaskManagerRuntimeSpecification.md` — correct §16's stale claim; add `task.agent_run_rejected` to the §10 event table.
- `docs/architecture/IMPLEMENTATION_GAPS.md` — update the Gap #53 entry (`IMPLEMENTATION_GAPS.md:2513-2517`).
- `docs/implementation/IMPLEMENTATION_HISTORY.md` — new entry.

**Test files — modify:**
- `tests/runtime/InMemoryTaskManagerRuntimeTest.kt`
- `tests/runtime/InMemoryAgentRuntimeTest.kt`
- `tests/composition/ParkerRuntimeConversationPipelineTest.kt` (or a new, narrower composition-level test — see Section 9)

**Test files — new:**
- `tests/runtime/DeterministicAgentStepSourceTest.kt`
- `tests/runtime/TaskManagerAgentRunSubmissionIntegrationTest.kt` (Section 2)

**Explicitly not touched, listed for the avoidance of doubt:** `src/contracts/Resource.kt`, `src/contracts/Permission.kt`, `src/contracts/ActionMapping.kt`, `src/interfaces/ResourceRegistry.kt`, `src/runtime/ActionMapper.kt`, `src/interfaces/PermissionEngine.kt`, `src/runtime/DefaultPermissionPolicy.kt`, `src/runtime/DefaultPermissionEngine.kt`, `src/runtime/DefaultExecutionPipeline.kt`, `src/contracts/TaskProposal.kt`, `src/contracts/Task.kt`, `src/contracts/TaskLifecycle.kt`, `src/contracts/AgentRun.kt`, `src/contracts/AgentRunLifecycle.kt`. Every mechanism this milestone uses in these files (Resource registration, ActionVocabulary registration, PermissionPolicyRule matching, ExecutionRequest evaluation) is exercised exactly as these files already define it — none of them gain a new method, field, or behaviour.

---

## 9. Prohibited

**Everything not explicitly named in Section 8 is prohibited for this milestone**, including but not limited to: any change to `SUSPEND`/`RESUME`/`CANCEL` handling; any change to `TaskProposalDisposition`'s other four outcomes; any change to `TaskLifecycleTransitions`'s edge set; any new `PermissionAction` or `ResourceType` enum value; any new `AgentRunCommandResult` variant; any change to `ExecutionRequest`, `PermissionDecision`, or `AgentRunCommand`'s own field shape; any retry or auto-resubmission logic; any Planner-backed `AgentStepSource`; any scheduling, Workflow Engine, multi-agent orchestration, distributed execution, advanced retry policy, plugin ecosystem, Home Assistant integration, Android integration, or refactoring unrelated to this milestone's own named files. If Implementation discovers a need to touch a file not listed in Section 8, or to change a contract listed as untouched, it must stop and return to Contract Design — this Scope Lock does not pre-authorise that judgment call to be made silently during Implementation.

---

## 10. Event Payloads — Final

| Event | Publisher | Payload | Trigger |
|---|---|---|---|
| `task.agent_run_started` | `TASK_MANAGER_RUNTIME_PRINCIPAL_ID` | `{"agentRunId": <AgentRunCommandResult.Accepted.agentRunId.value>}` | `AgentRunCommandResult.Accepted` for `START` |
| `task.agent_run_rejected` | `TASK_MANAGER_RUNTIME_PRINCIPAL_ID` | `{"reason": <AgentRunCommandResult.Rejected.reason>, "commandType": "START"}` | `AgentRunCommandResult.Rejected` for `START` |

No other event's shape, publisher, or trigger changes.

---

## 11. Test Obligations — Final

1. **`tests/runtime/InMemoryTaskManagerRuntimeTest.kt`** — thread a fake `AgentRunCommandChannel` through every existing test; add tests for both branches of Section 5/6, including the exact payload content of Section 10 and `TaskProposalDisposition.Accepted` in both branches.
2. **`tests/runtime/InMemoryAgentRuntimeTest.kt`** — thread a fake `PermissionEngine` (default-approve) and a fixed `runInitiationResourceId` through every existing test; add tests asserting, for `DENIED` and `DEFERRED` separately: `AgentRunCommandResult.Rejected` is returned, `getAgentRun` returns `null` for the deterministic ID, no `agent.*` event is published, and Agent Identity resolution / `AgentStepSource.nextStep` are never invoked (a spy or call-counting fake is sufficient to prove the latter).
3. **`tests/runtime/DeterministicAgentStepSourceTest.kt`** (new) — `Propose` on step 1, `Complete` thereafter, mirroring `SingleStepAgentStepSource`'s own tested shape.
4. **`tests/runtime/TaskManagerAgentRunSubmissionIntegrationTest.kt`** (new, Section 2) — real `InMemoryTaskManagerRuntime` + real `InMemoryAgentRuntime` + real `IdentityService`/`EventBus`/`ExecutionPipeline`/`PermissionEngine` (the latter carrying the actual production rule from Section 3, not a fake), `DeterministicAgentStepSource`, and the real `AGENT_RUNTIME_BOUNDARY_RESOURCE_ID` Resource registered exactly as Section 4 specifies. Covers, end to end, the full accepted-flow ordering (Section 5) and, in a second scenario, the full rejected-flow ordering (Section 6) using a Principal the production rule does not (or a policy configured to) approve.
5. **`tests/composition/ParkerRuntimeConversationPipelineTest.kt`** or a new, narrower composition test — confirms `InMemoryAgentRuntime` is constructed in the real composition root with the frozen `AgentPolicy` default and the real `AGENT_RUNTIME_BOUNDARY_RESOURCE_ID` resolvable through the real `ResourceRegistry`.

---

## 12. Documentation Reconciliation — Final

- `src/contracts/AgentRunCommand.kt` — correct `AgentRunCommandChannel`'s stale claim.
- `docs/specifications/volume-05-task-manager-runtime/TaskManagerRuntimeSpecification.md` §16 — same correction; §10 event table gains `task.agent_run_rejected`.
- `src/contracts/AgentStep.kt` — correct the `FixedSequenceAgentStepSource` citation to `DeterministicAgentStepSource`, noting `SingleStepAgentStepSource`'s continued, legitimate, test-only existence.
- `docs/architecture/IMPLEMENTATION_GAPS.md` — update the Gap #53 entry; this milestone closes the "execution" half of that entry (`InMemoryTaskManagerRuntime`'s constructed `AgentRunCommand` is now submitted and consumed) — Gap #53 as a whole may remain open if any of its other named items are untouched by this milestone; Documentation Reconciliation must check, not assume closure.
- `docs/implementation/IMPLEMENTATION_HISTORY.md` — new entry per the established per-milestone convention.

---

## 13. Definition of Complete

1. Every file in Section 8 is modified or created exactly as scoped; no file outside Section 8 is touched (Section 9).
2. `InMemoryAgentRuntime.start()` implements the exact revised sequence in Section 1 — verified by the tests in Section 11, item 2, specifically the "no record, no event, no identity resolution, no step-source invocation" assertions for `DENIED`/`DEFERRED`.
3. `InMemoryTaskManagerRuntime.submitProposal()` implements the exact accepted/rejected flows in Sections 5-6, including `TaskProposalDisposition.Accepted` in both branches.
4. The production `ActionVocabulary` entry, `PermissionPolicyRule`, and `AGENT_RUNTIME_BOUNDARY_RESOURCE_ID` Resource from Sections 3-4 exist in `ParkerRuntime.kt` exactly as specified, and a real `START` submitted on behalf of a Task's real, resolved owner is genuinely `Accepted` end-to-end (proven by the integration test, Section 11 item 4) — not merely wired but inert.
5. `task.agent_run_started` and `task.agent_run_rejected` are published with the exact payloads in Section 10.
6. All five test obligations in Section 11 exist and pass.
7. Documentation Reconciliation (Section 12) is complete.
8. `.\gradlew.bat test` passes in full, confirmed by the user, before any commit.
9. No file outside Section 8's permitted list appears in `git status` as modified as a result of this work (pre-existing, unrelated line-ending noise excepted, per this session's own established, previously-disclosed finding).

---

## 14. Rollback Conditions

Implementation must stop, report, and await further instruction — not improvise a workaround — if any of the following occurs:

- Native Verification shows the production `PermissionPolicyRule` from Section 3 does not actually resolve to `APPROVED` for a legitimate owner-requested `START` (i.e., the `ActionMapper`/`ResourceRegistry` wiring does not behave as this document predicts). This is a design assumption, not yet compiler- or test-verified; if wrong, return to Contract Design, do not patch around it with a second rule or a widened match.
- Any test in Section 11 requires touching a file outside Section 8 to pass.
- The seven-parameter `InMemoryAgentRuntime` constructor (Section 4) breaks an existing test in a way not attributable to the new parameters themselves (i.e., an unrelated regression) — stop and report; do not silently alter unrelated behaviour to make the test pass.
- Any discovery that `InMemoryTaskManagerRuntime` or `InMemoryAgentRuntime` has a production caller or dependent this review and design did not identify (none are currently known — Governance Review Section 2.2 and 2.11 found none) — stop and report before proceeding, since this would mean the "sole production caller" and "constructed nowhere" premises (decisions 1-2, 12) this entire Scope Lock rests on are false.
- Any compile error or test failure whose correct fix would require modifying `AgentRunCommand`, `AgentRunCommandResult`, `TaskProposal`, `TaskProposalDisposition`, `TaskLifecycleTransitions`, `AgentRunLifecycleTransitions`, `PermissionEngine`, `PermissionDecision`, `ExecutionRequest`, `ResourceRegistry`, or `ActionMapper` — all of these are frozen, untouched contracts (Section 8); a fix requiring their modification means this Scope Lock's own assumptions were wrong and must be revisited, not implemented around.

---

## 15. Implementation Discipline

Implementation is authorised only to realise the design frozen by this Scope Lock.

During implementation, no additional behaviour, architectural changes, convenience features, optimisations, refactoring, or design improvements are to be introduced, even if they appear beneficial or technically superior.

If implementation identifies:

- an apparent architectural improvement;
- a cleaner design;
- a broader refactoring opportunity;
- an additional capability;
- a missing feature;
- a newly discovered optimisation; or
- any other change beyond the boundaries explicitly frozen in this Scope Lock,

implementation must stop and record the observation rather than implementing it. Any such change requires a separate governance decision through the normal project process (Governance Review, Contract Design, or a subsequent Scope Lock, as appropriate).

The purpose of this restriction is to ensure that implementation remains a mechanical execution of approved design decisions rather than becoming a second design phase.

---

## 16. Approval Gate

**No implementation work may begin until this Scope Lock is explicitly approved.** This document has not modified any source file, has not staged anything, and has not committed or pushed anything. Upon approval, Implementation proceeds strictly within Section 8's permitted file boundary, strictly per the sequences frozen in Sections 1, 5, and 6, and strictly subject to the rollback conditions in Section 14, and subject to the implementation discipline in Section 15.

---

## Amendment — Two-Phase Agent Run Operation (Acceptance/Execution Separation)

**Status:** Approved Scope Lock Amendment, freezing implementation of
`docs/architecture/CONTROLLED_AGENT_RUN_SUBMISSION_CONTRACT_DESIGN.md`'s
own "Amendment 1," itself corrected once (Lifecycle State Correction,
Contract Design Amendment 1 §A1.8) before implementation began — the phase
boundary below already reflects that correction: **phase 1 ends at
`READY`, not `RUNNING`**. Triggered by Native Verification finding a genuine
self-deadlock in `InMemoryTaskManagerRuntime.submitProposal()`, and a
subsequent rejected first correction (documented, superseded, in
`docs/architecture/CONTROLLED_AGENT_RUN_SUBMISSION_DEADLOCK_DESIGN_RECONCILIATION.md`)
that resolved the deadlock but let `task.agent_run_started` observably
follow `task.completed`. The approved correction is
`docs/architecture/CONTROLLED_AGENT_RUN_SUBMISSION_ACCEPTANCE_EXECUTION_SEPARATION_RECONCILIATION.md`,
Option 1. This Scope Lock Amendment does not reopen or relitigate Sections
1-16 above except where explicitly named below.

### A.1 New execution-trigger interface

```kotlin
interface AgentRunExecutionTrigger {
    suspend fun execute(agentRunId: AgentRunId)
}
```

Implemented by `InMemoryAgentRuntime` — the same instance that implements
`AgentRunCommandChannel` (Contract Design Amendment 1, A1.2). No second
implementation exists or is planned. `execute()` owns the `READY -> RUNNING`
transition and the `agent.started` publish (Contract Design Amendment 1,
§A1.8) — neither happens inside `submit()`/phase 1 any longer.

### A.2 Revised constructor dependencies

Supersedes Section 4's `InMemoryTaskManagerRuntime` shape (which Section 4
itself did not state explicitly, only `InMemoryAgentRuntime`'s — recorded
here for the avoidance of doubt, this is the shape actually implemented,
including the `runInitiationVerbPhrase` Implementation Clarification
approved during Implementation and never separately written back into
Section 4 above):

```kotlin
class InMemoryTaskManagerRuntime(
    private val identityService: IdentityService,
    private val eventBus: EventBus,
    private val agentRunCommandChannel: AgentRunCommandChannel,
    private val agentRunExecutionTrigger: AgentRunExecutionTrigger,   // NEW
) : TaskProposalIntake
```

`InMemoryAgentRuntime`'s own constructor is unchanged by this amendment (it
already exists, as implemented: `identityService`, `executionPipeline`,
`eventBus`, `agentStepSource`, `agentPolicy`, `permissionEngine`,
`runInitiationResourceId`, `runInitiationVerbPhrase` — eight parameters).
It gains a second implemented interface, not a ninth constructor parameter.

`ParkerRuntime.kt` passes the same `agentRuntime` instance for both the
`agentRunCommandChannel` and `agentRunExecutionTrigger` arguments of
`InMemoryTaskManagerRuntime`'s constructor.

### A.3 Exact two-phase sequence — supersedes Section 5 in full

```
Task created
  -> Task QUEUED
  -> AgentRunCommand.START constructed
  -> run-initiation permission evaluated
  -> duplicate-START protection enforced (InMemoryAgentRuntime, unchanged)
  -> Agent Run created (CREATED)
  -> Agent Identity resolved
  -> Agent Run advanced: INITIALISED -> READY                         [phase 1 stops here -- NOT RUNNING]
  -> AgentRunCommandChannel.submit() returns Accepted(agentRunId)     [phase 1 ends]
  -> task.agent_run_started published
  -> Task QUEUED to RUNNING                                          [TaskStatus, not AgentRunStatus -- see Contract Design Amendment 1, A1.4]
  -> Task Manager mutex released
  -> AgentRunExecutionTrigger.execute(agentRunId) invoked             [phase 2 begins]
  -> Agent Run advanced: READY -> RUNNING
  -> agent.started published                                         [now truthful -- execution is actually beginning]
  -> runLoop() -- agent step/execution/permission events, unchanged
  -> agent.completed OR agent.failed published
  -> Task Manager's existing agent.completed/agent.failed subscription fires
     (mutex NOT held by any outer call -- safe to reacquire)
  -> task.completed OR (no transition; agent.failed still performs none, unchanged)
```

**Frozen observable event order, accepted flow:**

```
task.created
task.ready
agent.created
agent.initialised
agent.ready
task.agent_run_started
Task QUEUED -> RUNNING
execute(agentRunId)
agent.started
agent step/execution/permission events
agent.completed OR agent.failed
task.completed
```

**`task.completed` is published only on the `agent.completed` path.** No
`task.failed` event exists in this codebase, and none is introduced by this
amendment — `agent.failed` performs no Task Status transition (Unit B2,
unchanged; restated in Section 7 of this document). On the `agent.failed`
path, the frozen sequence above ends at `agent.failed`; the Task remains at
whatever status it already held (`TaskStatus.RUNNING`, unable to progress
further without a mechanism this milestone does not build), exactly as
every prior version of this Scope Lock has already recorded as a known,
accepted, pre-existing gap. This is named explicitly so the single
`task.completed` line in the frozen sequence is never misread as
unconditional.

**Frozen observable event order, rejected flow (unchanged in substance from
Section 6, restated for completeness):**

```
task.created
task.ready
task.agent_run_rejected
Task remains QUEUED
```

**No `task.agent_run_started` may be published for a rejected START.**
Structurally guaranteed: phase 2 (and the `task.agent_run_started` publish
that precedes it) is reached only inside the `is Accepted ->` branch
(Contract Design Amendment 1, A1.4); the `is Rejected ->` branch never
executes that code path.

### A.4 The mutex rule — frozen explicitly

**`InMemoryTaskManagerRuntime`'s own mutex must not be held while
`AgentRunExecutionTrigger.execute()` is in flight.** `submitProposal()`'s
`mutex.withLock` block ends after the `Accepted`/`Rejected` branch's own
bookkeeping (publish, transition) completes; `execute()` is called strictly
outside it. This is the specific, named implementation rule that resolves
the original deadlock (root cause: `docs/architecture/CONTROLLED_AGENT_RUN_SUBMISSION_DEADLOCK_DESIGN_RECONCILIATION.md`
§1) without reintroducing it.

### A.5 Expanded permitted file boundary — supersedes Section 8 by addition only

**Production files — modify (in addition to Section 8's list):**
- `src/runtime/InMemoryAgentRuntime.kt` — split `start()` into phase 1
  (ending at `READY`, returning `Accepted`/`Rejected`, before the
  `READY -> RUNNING` transition, before `agent.started`, before `runLoop()`)
  and a new `execute()` method implementing `AgentRunExecutionTrigger`
  (performs `READY -> RUNNING`, publishes `agent.started`, then calls
  `runLoop()`); no other behavioural change.
- `src/runtime/InMemoryTaskManagerRuntime.kt` — second constructor parameter;
  `submitProposal()` restructured per Contract Design Amendment 1, A1.4.
- `src/composition/ParkerRuntime.kt` — thread the same `agentRuntime`
  instance into `InMemoryTaskManagerRuntime`'s new second parameter.

**Production files — new:**
- None. `AgentRunExecutionTrigger` is declared alongside `AgentRunCommandChannel`
  in `src/contracts/AgentRunCommand.kt` (the existing frozen-contracts file
  for this command family), not a new file.

**Test files — modify (supersedes and expands Section 8's test list):**
- `tests/runtime/InMemoryTaskManagerRuntimeTest.kt` — second constructor
  argument (a fake `AgentRunExecutionTrigger`) threaded through every
  existing test; existing `Accepted`-branch tests updated to assert
  `execute()` was invoked with the correct `agentRunId`.
- `tests/runtime/InMemoryAgentRuntimeTest.kt` — every existing test that
  calls `submit(startCommand())` and asserts a terminal `AgentRun` status
  immediately afterward must add an explicit `.execute(agentRunId)` call
  between `submit()` and that assertion. This is a behavioural update, not
  a signature update — the largest single piece of test work this amendment
  requires.
- `tests/runtime/EventCollectorTest.kt`,
  `tests/runtime/RuntimeLifecycleEventPublicationTest.kt`,
  `tests/runtime/VerticalSliceEndToEndTest.kt` — **newly added to the
  permitted-file boundary by this amendment** (none were named in Section 8
  or in Contract Design's original Section 15). Each calls
  `InMemoryAgentRuntime.submit()` directly and asserts terminal state
  immediately afterward; each needs the same `.execute()` call added, in
  addition to the already-separately-agreed `SPRINT_1_EVENT_TYPES` fixture
  fix in `tests/runtime/EventCollector.kt` (unaffected by this amendment,
  still required, still test-fixture-only).
- `tests/runtime/TaskManagerAgentRunSubmissionIntegrationTest.kt` — accepted
  flow's event-order assertion updated to the frozen sequence in A.3, which
  is now genuinely achievable and must be asserted exactly, not disclosed as
  an anomaly.

**Test files — new:** none required beyond what Section 8 already named;
`AgentRunExecutionTrigger` needs no dedicated test file of its own — its
behaviour is exercised by the same call sites already covered above.

### A.6 Required test coverage for both phases

1. Phase 1 alone: `submit(startCommand())` returns `Accepted(agentRunId)`
   with the `AgentRun` at `READY` (not `RUNNING` — Lifecycle State
   Correction, Contract Design Amendment 1 §A1.8) and `runStates[agentRunId]`
   populated, **without** `runLoop()` having been entered and **without**
   `agent.started` having been published — provable via a spy
   `AgentStepSource` whose `nextStepCallCount == 0` immediately after
   `submit()` returns, mirroring the existing DENIED/DEFERRED proof
   technique already used elsewhere in `InMemoryAgentRuntimeTest.kt`.
2. Phase 2 alone: calling `execute(agentRunId)` for an `AgentRun` phase 1
   already accepted drives it to `COMPLETED`/`FAILED`/`SUSPENDED` exactly as
   the pre-amendment single-call `submit()` used to, with the same event
   sequence from `agent.step_started` onward.
3. `TaskManagerAgentRunSubmissionIntegrationTest.kt`'s accepted flow: the
   complete frozen event ordering (A.3) asserted through execution, ending
   in whichever terminal Agent lifecycle event the configured fixture
   actually produces (`agent.completed` **or** `agent.failed` — A.3 already
   freezes both as legitimate outcomes of the accepted execution path; a
   fixture whose proposed action has no matching `ActionVocabulary` entry
   is exercising a genuine execution-failure path, not a submission
   failure, and is not required to be extended to reach `agent.completed`).
   Always asserted, regardless of which terminal event actually occurs:
   `task.agent_run_started` strictly precedes `agent.started`;
   `task.agent_run_started` strictly precedes the terminal Agent lifecycle
   event; the accepted flow completes without deadlock; and the observed
   terminal event matches the fixture's configured execution behaviour.
   (Documentation correction, recorded here for provenance: this item
   originally named `agent.completed` as the only terminal event to assert
   against, which was in tension with A.3's own explicit `agent.completed`-
   OR-`agent.failed` framing for the accepted execution path. Implementation
   surfaced the tension against the actual, unmodified
   `TaskManagerAgentRunSubmissionIntegrationTest.kt` fixture — whose Task
   goal has no corresponding `ActionVocabulary` entry, so its accepted flow
   has always terminated at `agent.failed`, before and unaffected by this
   amendment — and stopped rather than either silently narrow the test's
   assertions to fit the original wording or broaden the fixture beyond
   this Scope Lock's own approved boundary to force `agent.completed`. This
   is a correction to this item's own wording, not a reopening of A.3's
   already-frozen sequence or of A.5's file boundary.)
4. A negative test proving `execute()` is never called by
   `InMemoryTaskManagerRuntime` on the `Rejected` branch (a spy/fake
   `AgentRunExecutionTrigger` with a call-count assertion of zero).
5. A negative test proving `InMemoryTaskManagerRuntime`'s own mutex is not
   held during `execute()` — the most direct available proof is the accepted
   flow itself completing at all (Native Verification failed with
   `UncompletedCoroutinesError` before this amendment; passing is itself the
   evidence), not a new introspective lock-state assertion (`Mutex` exposes
   no such public API).

### A.7 Rollback conditions — addition to Section 14

Implementation must stop, report, and await further instruction if:

- Splitting `start()` at the `READY`/`runLoop()` boundary (with the
  `READY -> RUNNING` transition and `agent.started` now owned by phase 2)
  requires changing any behaviour `runLoop()` itself performs (it must not —
  phase 2's `execute()` should call the existing `runLoop()` unmodified).
- Any test in `InMemoryAgentRuntimeTest.kt` cannot be made to pass by adding
  an `.execute()` call alone, and instead requires a change to what phase 1
  or phase 2 does — that would mean this amendment's own A.1/A.2 boundary is
  wrong and must return to Contract Design Amendment, not be patched around.
- The frozen event order in A.3 is not what Native Verification actually
  observes once implemented — return to this Scope Lock Amendment, do not
  silently adjust the order and call it equivalent.

### A.8 Approval Gate

**No implementation work on this amendment may begin until it is explicitly
approved**, in addition to this Scope Lock's own original Section 16
approval. Upon approval, Implementation proceeds strictly within Section
A.5's expanded file boundary, strictly per the sequence frozen in A.3,
strictly subject to A.7's rollback conditions, and strictly subject to
Section 15's implementation discipline (unchanged, still in force).
