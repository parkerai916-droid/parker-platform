# Reasoning-to-Planning Handoff — Governance Review

## Status

**Governance review only. No Kotlin, no interfaces, no contracts, no
tests, no dependency or build changes exist anywhere in this document or
arise from it.** Nothing under `src/` or `tests/` is touched. This
document does not begin Contract Design — it determines whether Contract
Design may begin, and on what terms.

**Scope, restated exactly as chartered.** This review is narrowed to one
boundary only: `ReasoningProviderResponse.Goal` → construction of
`PlanningRequest` → invocation responsibility for the existing
`PlannerRuntime`. It explicitly excludes, and settles nothing about:
`PlanCandidate` generation or deliberation/ranking policy; production
wiring of `InMemoryPlannerRuntime` or `InMemoryTaskManagerRuntime` into
`ParkerRuntime.kt`; Task creation; execution; Tool invocation; persistent
Goal Manager implementation; any change to planning algorithms; any
change to reasoning behaviour. Those are named in Section 9 as
out-of-scope, not silently assumed away.

---

## 1. Repository Review (fresh reads, this Unit)

Read directly, at the level cited, immediately before this document was
written — this review does not rely on the prior read-only architectural
survey from memory:

- `src/interfaces/ReasoningProvider.kt` — read in full. `ReasoningProviderResponse.Goal(text: String)`'s own KDoc: "Maps, at the caller's discretion and outside this unit's scope, toward a future `PlanningRequest.goal`."
- `docs/architecture/REASONING_PROVIDER_CONTRACT_DESIGN.md` Sections 3–7 — read directly. Confirms `Goal.text` is "deliberately shaped to be directly usable as a future `PlanningRequest.goal: String`... once the calling component (never this contract) decides to submit one," restating `REASONING_PROVIDER_ARCHITECTURE.md` Section 9: "A Reasoning Provider never constructs a `PlanningRequest` itself." Section 7's closing invariant: "a `Goal`... is not, and never becomes, a substitute for a Task Proposal, a Plan Decision, or a Permission Decision."
- `src/runtime/ResponseComposer.kt` — read in full. The live, current, tested handling of a `Goal`: `is ReasoningProviderResponse.Goal -> GatedOutcome.NotAccepted("not a Reply; reasoningResponse was Goal")`. Its own KDoc states this class's constructor accepts only `IdentityService`, and that absence "is itself the structural guarantee that this class cannot reach... `PlannerRuntime`."
- `src/runtime/ConversationTurnReasoningCoordinator.kt` — read in full. Its own KDoc: "This coordinator stops after obtaining a `ReasoningProviderResponse` and returns it unchanged. It does not invoke `PlannerRuntime`, construct a `PlanningRequest`..." and states its two-dependency constructor is "the structural guarantee that this coordinator cannot reach `PlannerRuntime`."
- `src/runtime/ConversationReplyCoordinator.kt` — read in full. Confirms this is the one existing component that already holds the raw `ReasoningProviderResponse` (as `reasoned.value`, inside `GatedOutcome.Produced`) before unconditionally forwarding it to `ReplyDeliveryCoordinator.composeAndDeliver`. Its own KDoc likewise disclaims reaching `PlannerRuntime` "directly."
- `src/composition/ParkerRuntime.kt` — `submitOwnerMessage` read directly (lines 391–520-ish). Confirms the full production call chain: `submitOwnerMessage → ConversationReplyCoordinator.submitAndDeliver → (Produced) → ReplyDeliveryCoordinator.composeAndDeliver → ResponseComposer.compose`. Confirmed by grep: no reference to `PlannerRuntime` or `TaskManagerRuntime` anywhere in this file.
- `src/runtime/InMemoryPlannerRuntime.kt` — read in full. `plan(request: PlanningRequest, candidates: List<PlanCandidate>)` resolves its own publisher identity and `request.initiatingPrincipalId` via `IdentityService` (existence check only — no status check, no `PermissionEngine` call anywhere in this method) before doing anything else; an empty or non-viable `candidates` list deterministically returns `PlanningSessionResult.Failed` via `PlanDecisionResult.NoViableCandidate`.
- `src/contracts/PlanDecision.kt` — `PlanningRequest` (lines 214–226) and `PlanCandidate` (lines 76–88) read directly. `PlanningRequest.goal: String`; `PlanCandidate` is a distinct, richer type with no existing production constructor anywhere in `src/`.
- `docs/architecture/PLANNER_RUNTIME_CONTRACT_DESIGN.md` Section 6 (`PlanningRequest`) — read directly. "**Ownership.** Caller-supplied (whatever originates a Planning Request)" — left open, not assigned, by design.
- `src/interfaces/CommunicationIntake.kt` — `InboundOwnerMessage` and `CorrelationId` read directly. `channelId`, `senderPrincipalId: PrincipalId`, `correlationId: CorrelationId`, all already field-shaped.
- `src/interfaces/ConversationEngine.kt` — `Turn` read directly: `turnId`, `conversationId`, `message: InboundOwnerMessage`, `receivedAt`.
- `src/contracts/ExecutionRequest.kt` — `RequestOrigin` enum read directly (`VOICE, TEXT, SCHEDULED_TASK, AGENT, PLUGIN, HOME_ASSISTANT_EVENT, ANDROID_EVENT, REMOTE_INTERFACE`), reused unchanged by `PlanningRequest.source`.
- `docs/architecture/AUTHENTICATION_AND_TRUST_GOVERNANCE.md` — read in full. Core Principle: "Reasoning cannot grant permissions... reasoning cannot grant trust any more than it can grant authorisation." Architectural Position (Section 8): Authorisation is "the existing Permission Engine stage the constitution already defines," placed conceptually after Identity/Authentication and before Conversation/Reasoning — this document does not require any change to `ConversationEngine` or the Reasoning Provider, and authentication itself remains entirely unimplemented (governance only, no running code).
- `docs/architecture/INTER_SPECIFICATION_CONTRACTS.md` Section 3 — read directly. Row: `User / Front End | Goal / Planning Request | Planner Runtime | Proposed contract... No front end is specified yet... Goal formation itself remains explicitly out of scope everywhere it is used.` (Note: this document's own note that the Planner Runtime "remains unpromoted to an implementation phase" is stale — `InMemoryPlannerRuntime` was subsequently implemented in `src/runtime/`, Sprint 3 Track D — but its statement about Goal formation being out of scope is still accurate today, independently confirmed by this review's own reading of `ResponseComposer.kt`.)
- `docs/architecture/23-goal-manager.md` — read in full (three lines of substantive content): "The Goal Manager maintains long-term objectives," lifecycle `Created → Active → Progressing → Completed → Archived`. No field shape, no interface, no relationship to `PlanningRequest` or `ReasoningProviderResponse.Goal` stated anywhere in this or any other document found.
- `docs/implementation/REPLY_TO_OUTBOUND_RESPONSE_IMPLEMENTATION_PLAN.md` — grepped for `Goal` handling. Confirms the `Goal -> GatedOutcome.NotAccepted("not a Reply; Goal routing is out of scope")` behaviour was a deliberate design choice at the time `ResponseComposer` was built (Sprint 10, Unit 1), not an oversight.
- `docs/architecture/parker-constitution.md`'s two governing invariants, as restated verbatim by `AUTHENTICATION_AND_TRUST_GOVERNANCE.md`: "Parker owns authority. Modules provide capability." and "Cognition proposes. Trust authorises. Runtime executes."

### Central findings, not previously recorded this precisely

**Finding 1 — the dead end is real, live, and already covered by a passing test.** `ResponseComposer.compose` receiving a `Goal` does not throw, log a defect, or leave a TODO — it returns a well-formed `GatedOutcome.NotAccepted`, exactly as its Sprint 10 Scope Lock specified. Production Parker, today, discards any Reasoning Provider Goal determination silently at this exact line. This is disclosed, intentional behaviour, not a bug this review is reporting for the first time — but it is the precise, single point where the two spines (conversational, executive) fail to connect, confirmed by direct reading rather than inferred from documentation.

**Finding 2 — every existing coordinator between Reasoning and Response explicitly disclaims reaching `PlannerRuntime`.** `ConversationTurnReasoningCoordinator`, `ConversationReplyCoordinator`, and `ResponseComposer` each state, in their own KDoc, that their limited constructor is "the structural guarantee" they cannot reach `PlannerRuntime`. Closing this boundary therefore necessarily revises at least one existing component's own documented guarantee — there is no seam in the current call chain that is silent on this point. Section 6 below identifies which one revision is smallest.

**Finding 3 — the field-construction problem is smaller than it first appears.** `Turn.message` (`InboundOwnerMessage`) already carries `senderPrincipalId: PrincipalId` and `correlationId: CorrelationId`, and `REASONING_PROVIDER_CONTRACT_DESIGN.md` Section 4 already lists both as "already field-shaped" types reused, transitively, by `ReasoningProviderRequest.turn`. Three of `PlanningRequest`'s four caller-supplied fields (`initiatingPrincipalId`, `correlationId`, `goal`) already exist at the exact point `ConversationTurnReasoningCoordinator.submitTurnAndReason` returns a `Goal`. Only `planningSessionId` has no existing upstream source and must be freshly minted.

**Finding 4 — candidate generation is a hard, separate blocker, not a scoping nicety.** `PlannerRuntime.plan` requires `candidates: List<PlanCandidate>` as a mandatory second parameter. No production code anywhere constructs a `PlanCandidate` — confirmed by a repository-wide grep of `src/` (zero matches outside the type's own definition). Any coordinator this review recommends, if built and wired today, could only be exercised with an empty candidate list, which `InMemoryPlannerRuntime.plan` already handles correctly and deterministically: `PlanningSessionResult.Failed` via `PlanDecisionResult.NoViableCandidate`, reason `"no Plan Candidates were supplied for this Planning Session"`. This is disclosed in Section 8 as a dependency, not solved here.

---

## 2. The Problem, Stated Precisely

`ReasoningProvider.reason()` can already return `ReasoningProviderResponse.Goal(text: String)` — a confident determination that the owner's message expresses an intent that should be pursued, not merely replied to. `PlannerRuntime.plan()` already exists, is implemented, and is tested, and already accepts a `PlanningRequest` shaped almost exactly like what a `Goal` provides. Nothing between the two calls exists. The contract that would carry a `Goal` into a `PlanningRequest`, the component that would own constructing that `PlanningRequest`, and the decision of where in the existing call chain that construction should happen, are all currently undecided — not partially designed, not deferred to an existing seam, genuinely absent.

This is not a defect in either `ReasoningProvider` or `PlannerRuntime` individually. Both were built, and disclosed, as deliberately not reaching the other (Finding 2). The gap is the connective tissue itself, which no prior Unit was chartered to build.

---

## 3. The Current Dead End in Production

Traced by direct reading, not inferred: `ParkerRuntime.submitOwnerMessage` → `ConversationReplyCoordinator.submitAndDeliver` → (on `GatedOutcome.Produced`) → `ReplyDeliveryCoordinator.composeAndDeliver` → `ResponseComposer.compose`. When the wrapped `ReasoningProviderResponse` is `Goal`, `ResponseComposer.compose` returns:

```kotlin
is ReasoningProviderResponse.Goal -> GatedOutcome.NotAccepted(
    "not a Reply; reasoningResponse was Goal",
)
```

`ParkerRuntime.submitOwnerMessage` receives this as `GatedOutcome.NotAccepted`, logs `"Conversation not accepted for delivery"`, and returns `ParkerRuntimeOutcome.NotAccepted(...)` to its own caller. No `PlanningRequest` is ever constructed. No error occurs. The owner receives no reply and no acknowledgement that anything was understood as actionable — the Goal is reasoned about successfully and then discarded. This is the exact, current, tested behaviour of the production system as of `main` at `9ed1570`.

---

## 4. Reconciling the Naming Collision Around "Goal"

Two documents use the word "Goal" for what this review confirms are not proven to be the same thing:

- **The transient, per-Turn Goal** (`ReasoningProviderResponse.Goal.text`, `PlanningRequest.goal`): exists only for the lifetime of one Planning Session, carries no identity of its own beyond the session it starts, has no persistence, and is discarded once a `TaskProposal` is built or the session fails.
- **The persistent Goal** (`docs/architecture/23-goal-manager.md`): "maintains long-term objectives," with its own five-state lifecycle (`Created → Active → Progressing → Completed → Archived`) independent of any single Planning Session or Turn. This chapter is a three-line stub — no field shape, no interface, no relationship to `PlanningRequest` stated anywhere.

**Determination: related but distinct, not the same concept, and not entirely separate either.** They are related in that a real system would plausibly want a per-Turn Goal to sometimes originate from, or contribute toward, a longer-lived objective — but nothing in this repository specifies that relationship today, and inventing one here would exceed this review's charter. They are not the same concept: one has a lifecycle spanning a single Planning Session; the other explicitly claims to survive across many. They are not entirely separate either, because a future unit reconciling them will need to decide how a per-Turn Goal that recurs, or that a Goal Manager should track, gets promoted — a decision this review does not make.

**Consequence for this boundary.** `PlanningRequest.goal` and `ReasoningProviderResponse.Goal.text` both already denote the transient concept only. This review's handoff addresses that concept exclusively. Chapter 23's Goal Manager remains untouched, unimplemented, and explicitly out of scope (Section 9) — consistent with the Persistent Memory precedent already established for this project (a durable, lifecycle-bearing concept requires its own governance cycle, not incidental construction inside a narrower integration unit).

---

## 5. Lawful Ownership Boundary

### 5.1 Does `Goal.text` map directly to `PlanningRequest.goal`, or is a first-class Goal type required first?

**Direct string mapping.** `REASONING_PROVIDER_CONTRACT_DESIGN.md` Section 3 states `Goal.text` is "deliberately shaped to be directly usable as a future `PlanningRequest.goal: String`... once the calling component... decides to submit one" — this was the explicit design intent when `ReasoningProviderResponse` was built, not an accident this review is now correcting. Every downstream consumer of a goal string today (`DefaultPlanDecision.decide(goal: String, candidates)`, `TaskProposal.goal: String`, `PlanCandidate.goal: String`) already treats it as an unstructured string. Introducing a first-class `Goal` type now would require revising `PlanningRequest`, `PlanDecision`, `TaskProposal`, and `PlanCandidate` — four already-accepted, already-tested Sprint 3 contracts — for no consumer that currently needs more than a string. That is a materially larger, unjustified contract-revision cycle for this Unit's own narrow charter.

### 5.2 Same concept / related / separate

Answered in Section 4: related but distinct. `PlanningRequest.goal` addresses the transient concept only.

### 5.3 Which component owns constructing `planningSessionId`, `initiatingPrincipalId`, `correlationId`, and the goal payload?

- `initiatingPrincipalId`: already available as `turn.message.senderPrincipalId` at the point a `Goal` is produced. No construction needed, only extraction.
- `correlationId`: already available as `turn.message.correlationId.value` (unwrapping the existing `CorrelationId` value class into the plain `String` `PlanningRequest.correlationId` expects). No construction needed, only extraction and one type unwrap.
- `goal`: `ReasoningProviderResponse.Goal.text`, already available, unchanged (Section 5.1).
- `planningSessionId`: has no existing upstream source anywhere in this repository. It must be freshly minted — a new `PlanningSessionId`, by whichever component performs this handoff, the same way `ConversationId`/`TurnId` are already minted by their own owning components today.

**Ownership determination.** `PLANNER_RUNTIME_CONTRACT_DESIGN.md` Section 6 already states `PlanningRequest`'s ownership is "Caller-supplied (whatever originates a Planning Request)" — deliberately left open for exactly this future decision. `REASONING_PROVIDER_ARCHITECTURE.md` Section 9 (restated in Contract Design Section 3) states plainly: "A Reasoning Provider never constructs a `PlanningRequest` itself." Combined with Finding 2 (every existing coordinator between Reasoning and Response disclaims `PlannerRuntime`), the lawful owner of this construction is a **new** component — not `ReasoningProvider`, not `ConversationTurnReasoningCoordinator`, not `ResponseComposer`, and not `ConversationEngine`.

### 5.4 Is a new coordinator required?

**Yes.** Every existing boundary crossing of this kind in this repository (`ConversationTurnReasoningCoordinator`, `CommunicationConversationCoordinator`, `ReplyDeliveryCoordinator`) is a small, concrete, non-interface-backed class, constructor-injected with the minimum dependency set, whose own limited constructor is documented as the structural proof of what it cannot reach. This handoff is architecturally identical in kind: sequence an already-produced value into a call against an already-existing dependency, constructing one intermediate contract object along the way — exactly what `ResponseComposer` already does for `Reply → OutboundParkerResponse`. A new, equivalently-scoped coordinator for `Goal → PlanningRequest → PlannerRuntime.plan` follows this established pattern; reusing or extending an existing coordinator for this purpose would require revising a documented "cannot reach `PlannerRuntime`" guarantee on a component not otherwise being changed for this reason (see 5.5/Section 6 for which one revision is smallest).

### 5.5 Intercept before `ResponseComposer`, or should `ResponseComposer` gain a routed outcome?

**Intercept before `ResponseComposer` — do not modify it.** `ResponseComposer`'s own KDoc states its two-argument-free constructor "is itself the structural guarantee that this class cannot reach... `PlannerRuntime`." Adding a `PlannerRuntime` dependency here breaks an already-accepted, already-tested Sprint 10 Unit 1 invariant for no benefit, since a suitable interception point already exists upstream: `ConversationReplyCoordinator.submitAndDeliver` already holds the raw `ReasoningProviderResponse` (as `reasoned.value`, inside `GatedOutcome.Produced`) before it ever calls `ReplyDeliveryCoordinator.composeAndDeliver`. A future Contract Design can extend `ConversationReplyCoordinator`'s existing `when` to a third branch — `Goal` routes to the new coordinator (5.4); `Reply`/`NoAction` proceed exactly as today — touching exactly one existing class's dependency list and one `when` expression, leaving `ReplyDeliveryCoordinator` and `ResponseComposer`, and every one of their existing tests, untouched. `ResponseComposer`'s existing `Goal -> NotAccepted` branch is not removed by this review's recommendation; it simply stops being reachable from the production `Goal` path once `ConversationReplyCoordinator` is revised, and remains correct, defensive, exhaustive-`when` handling for any other caller.

### 5.6 Authentication, identity, trust, and permission assumptions at this boundary

- **No new authorisation stage belongs here.** `AUTHENTICATION_AND_TRUST_GOVERNANCE.md`'s Core Principle: "Reasoning cannot grant permissions." `REASONING_PROVIDER_CONTRACT_DESIGN.md` Section 7's closing invariant: "a `Goal`... is not, and never becomes, a substitute for a Task Proposal, a Plan Decision, or a Permission Decision." The constitution's "Cognition proposes. Trust authorises. Runtime executes." places Planning within Cognition — a `TaskProposal` remains a proposal, not an authorised action. Permission Engine evaluation correctly remains downstream, at `ExecutionRequest` submission (`DefaultExecutionPipeline` → `DefaultPermissionEngine`), exactly where it already runs today. This boundary must not add a Permission Engine check of its own.
- **Identity resolution is inherited, not reimplemented.** `InMemoryPlannerRuntime.plan` already requires `request.initiatingPrincipalId` to resolve via `IdentityService` before creating any session record. The new coordinator does not need to duplicate this — it is enforced inside `PlannerRuntime` itself.
- **Authentication does not exist as running code anywhere in this system.** Per `AUTHENTICATION_AND_TRUST_GOVERNANCE.md`'s own Status, it is a reserved architectural boundary, not an implementation. This handoff inherits the same "identity-resolvability only, no real authentication" position every other existing boundary (Reply delivery, Memory Source, World Model Source) already has — a project-wide characteristic, not a defect specific to this Unit.
- **Risk worth naming, not fixing here.** `IdentityService.resolve` does not suppress or flag Suspended/Revoked Principals (`IMPLEMENTATION_GAPS.md` #37, open). `PlannerRuntime.plan` therefore cannot today distinguish an Active initiating Principal from a Suspended or Revoked one that still happens to resolve. This is a pre-existing `PlannerRuntime` characteristic, not introduced by this review or its recommended Unit — but this Unit is what would first give it a real, reachable path from live conversational input, which raises its practical stakes for the first time. Recorded here per this project's own disclosure convention (mirroring how `IMPLEMENTATION_GAPS.md` #40 treated an analogous stakes-raising event for `PermissionEngine`), not resolved.

---

## 6. Architectural Options Considered

**Option A — Modify `ResponseComposer` to add a routed `Goal` outcome.** Rejected. Requires adding a `PlannerRuntime` dependency to a component whose own KDoc and tests assert it cannot reach one; the largest blast radius of the options considered, touching an already-accepted Sprint 10 contract for a responsibility (composing/delivering conversational replies) that was deliberately scoped away from planning.

**Option B — Have `ConversationTurnReasoningCoordinator` branch and call `PlannerRuntime` directly.** Rejected. Its own KDoc states in the most structurally explicit terms in the whole codebase that its two-dependency constructor exists specifically to make this unreachable ("The absence of any other constructor parameter is itself the structural guarantee..."). Violating this is a direct contradiction of a documented invariant, not merely an extension of one.

**Option C — Extend `ConversationReplyCoordinator` with a third branch, delegating `Goal` to a new, narrowly-scoped coordinator.** The dependency it must add (a new coordinator, or `PlannerRuntime` transitively through one) is additive to exactly one class; `ReplyDeliveryCoordinator` and `ResponseComposer` are untouched. Matches the established pattern of small, single-purpose, disclosed coordinators this codebase already uses throughout the conversational spine.

**Option D — Insert a new top-level branch inside `ParkerRuntime.submitOwnerMessage` itself, upstream of `ConversationReplyCoordinator`.** Considered and rejected: `submitOwnerMessage` does not currently receive the raw `ReasoningProviderResponse` at all — only the final `GatedOutcome<ExecutionResult>` `ConversationReplyCoordinator.submitAndDeliver` returns. Reaching the `Goal` value here would require either restructuring `ConversationReplyCoordinator`'s own return type (a larger, unjustified contract change) or parsing `GatedOutcome.NotAccepted`'s free-text `reason` string to infer that the underlying cause was `Goal` — explicitly rejected as an unauthorised inference of hidden state from a string never contracted to be machine-parseable for this purpose (the same category of shortcut this project's own conventions have refused elsewhere, e.g. `IMPLEMENTATION_GAPS.md` #43's refusal to reconstruct `AgentRunId` by inference).

---

## 7. Recommended Option, and Reasons

**Option C.** It is the smallest revision that does not contradict any existing component's documented guarantee: exactly one existing class (`ConversationReplyCoordinator`) gains one new dependency and one new `when` branch; every other component in the current chain — `ConversationTurnReasoningCoordinator`, `CommunicationConversationCoordinator`, `ReplyDeliveryCoordinator`, `ResponseComposer`, `ResponseDelivery` — and every one of their existing tests, remains untouched. It reuses the exact coordinator pattern this codebase already applies consistently (small, concrete, disclosed, minimum-dependency classes), rather than introducing a new architectural shape. It correctly keeps identity resolution and Plan Decision evaluation inside `PlannerRuntime`, where they already live, rather than duplicating them in a new coordinator.

---

## 8. Risks and Dependencies

- **Hard dependency: `PlanCandidate` generation does not exist (Finding 4).** A coordinator built per this recommendation is real, honest, and testable on its own terms, but cannot produce a successful `PlanningSessionResult` in production until a separate, future Unit supplies real candidate generation. Until then, every live `Goal` reaching this new coordinator would deterministically fail with `"no Plan Candidates were supplied for this Planning Session"` — a disclosed, correct dead end one step further down the chain than today's, not a solved path to real goal pursuit. This mirrors exactly how Memory Source and World Model Source were wired to stores that start, and remain, empty in production (Sprint 11 Units 7–8) — an honest partial wiring, not a defect.
- **Soft dependency: `InMemoryPlannerRuntime`/`InMemoryTaskManagerRuntime` are not constructed anywhere in `ParkerRuntime.kt`.** This review's recommended coordinator needs a live `PlannerRuntime` instance injected into it; today none exists in the production composition root. Wiring one in is additive and low-risk (the same kind of change Sprint 11 already made twice for Memory/World Model), but it is a separate unit, not performed here.
- **Identity risk, restated from 5.6.** A Suspended/Revoked Principal that still resolves via `IdentityService.resolve` can reach `PlannerRuntime.plan` once this boundary is closed, for the first time from real conversational input rather than only from test harnesses. Not a blocker; recorded so a future Unit does not discover it by surprise.
- **`PlannerSessionId` minting has no established convention yet.** Unlike `ConversationId`/`TurnId`, no existing component mints a `PlanningSessionId` today. The future Contract Design must decide the minting scheme (e.g., UUID-based, or derived from `correlationId`) — not decided by this review.
- **`correlationId` type mismatch.** `InboundOwnerMessage.correlationId: CorrelationId` (value class) versus `PlanningRequest.correlationId: String`. A one-line `.value` unwrap, not a design problem, but worth naming so the future Contract Design does not treat it as a new discovery mid-implementation.

---

## 9. Explicitly Out of Scope

Unchanged from this Unit's charter, restated for completeness: `PlanCandidate` generation; deliberation or ranking policy; production wiring of `InMemoryPlannerRuntime`; production wiring of `InMemoryTaskManagerRuntime`; Task creation; execution; Tool invocation; persistent Goal Manager implementation (Chapter 23); any change to planning algorithms; any change to reasoning behaviour. This review also does not decide `PlanningSessionId`'s minting scheme, or write the new coordinator's exact interface shape — both belong to the Contract Design this review clears the way for, not to this document.

---

## 10. Constraining Contracts and Governance Documents

- `docs/architecture/REASONING_PROVIDER_ARCHITECTURE.md` and `docs/architecture/REASONING_PROVIDER_CONTRACT_DESIGN.md` — `Goal`'s own shape and deferred-mapping language; "a Reasoning Provider never constructs a `PlanningRequest` itself."
- `docs/architecture/PLANNER_RUNTIME_CONTRACT_DESIGN.md` and `PlannerRuntimeSpecification.md` (cited via the former) — `PlanningRequest`'s field shape and its own "caller-supplied, whatever originates a Planning Request" ownership note.
- `docs/architecture/PLANNER_RUNTIME_PROGRESSION_DESIGN.md` — Planner Runtime's own progression and scope boundaries (candidate generation excluded, restated by `InMemoryPlannerRuntime`'s own KDoc).
- `docs/implementation/RESPONSE_COMPOSER_SCOPE_LOCK.md` and `REPLY_TO_OUTBOUND_RESPONSE_IMPLEMENTATION_PLAN.md` — `ResponseComposer`'s existing, deliberate `Goal -> NotAccepted` behaviour and its structural "cannot reach `PlannerRuntime`" guarantee.
- `docs/implementation/CONVERSATION_REPLY_COORDINATOR_SCOPE_LOCK.md` / `_IMPLEMENTATION_PLAN.md` — `ConversationReplyCoordinator`'s existing sequencing rule and dependency list, the component this review recommends revising.
- `docs/architecture/AUTHENTICATION_AND_TRUST_GOVERNANCE.md` — "Reasoning cannot grant permissions"; Authorisation remains the Permission Engine's role, downstream of Planning.
- `docs/architecture/parker-constitution.md` — "Cognition proposes. Trust authorises. Runtime executes."; "Parker owns authority. Modules provide capability."
- `docs/architecture/INTER_SPECIFICATION_CONTRACTS.md` Section 3 — records Goal/Planning Request as a "Proposed contract," Goal formation as explicitly out of scope everywhere used, prior to this review.
- `docs/architecture/23-goal-manager.md` — the source of the naming collision addressed in Section 4.
- `docs/architecture/IMPLEMENTATION_GAPS.md` #37 (identity resolution does not suppress non-Active Principals), #48 (deterministic ID cardinality caps, relevant precedent for any `PlanningSessionId` minting decision), #49 (Planner Runtime publisher identity resolution, closed — the existing precedent for how `PlannerRuntime` already handles identity).
- `docs/architecture/MEMORY_SOURCE_GOVERNANCE_REVIEW.md` and `WORLD_MODEL_SOURCE_GOVERNANCE_REVIEW.md` — the direct structural precedent this review follows for scoping a single disconnected boundary and disclosing, rather than solving, adjacent gaps.
- `docs/architecture/PRE_MODULE_ID_MULTIPLICITY_DECISION.md` — relevant precedent: this recommendation produces at most one `PlanningRequest` per `Goal`, consistent with the existing one-Task-Proposal-per-Planning-Session constraint this document already settled.

---

## 11. Readiness Determination — May Contract Design Proceed?

**Yes, with the dependencies in Section 8 disclosed and carried forward, not resolved here.**

- **Governance:** clear. No reviewed document conflicts with defining this handoff. `PLANNER_RUNTIME_CONTRACT_DESIGN.md` Section 6 already reserves "caller-supplied, whatever originates a Planning Request" for exactly this decision; `AUTHENTICATION_AND_TRUST_GOVERNANCE.md` confirms no authorisation stage belongs at this boundary.
- **Naming collision:** resolved for this Unit's purposes (Section 4) — the transient and persistent Goal concepts are related but distinct; this Unit addresses the transient concept only, and does not touch Chapter 23.
- **Ownership boundary:** determined (Section 5) — a new, narrowly-scoped coordinator, extending `ConversationReplyCoordinator`'s existing branch (Option C), constructing `PlanningRequest` from fields already available on `Turn.message` plus a freshly-minted `PlanningSessionId`.
- **Blockers:** none that prevent writing the Contract Design itself. `PlanCandidate` generation's absence (Finding 4) blocks this handoff from producing a *successful* Planning Session in production, but does not block designing and implementing the handoff honestly, disclosed, exactly as Memory Source and World Model Source were wired ahead of their own write paths existing.

Contract Design may proceed, scoped to: the new coordinator's interface and dependency shape; the exact construction of `PlanningRequest` from a `Turn` and a `Goal`; the `PlanningSessionId` minting scheme; and the precise revision to `ConversationReplyCoordinator`'s existing `when` branch. It must not expand into `PlanCandidate` generation, production composition wiring, or Chapter 23's Goal Manager — those remain separately chartered, per Section 9.
