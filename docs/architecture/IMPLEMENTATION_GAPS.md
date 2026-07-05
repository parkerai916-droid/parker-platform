# Parker Platform — Implementation Gaps

Recorded while implementing Phase 1 (Volume 1 Core Contracts) on
`feature/phase-1-core-contracts`. Updated during the Phase 1 follow-up
specification cleanup pass — each original gap is now marked **Resolved**,
**Deferred**, or **Requires human decision**. Three new findings surfaced
while doing that cleanup are appended at the end.

## 1. `IdentityService` has no interface

**Status: Resolved by the Identity Service Implementation phase.**

`IdentityService` was deferred from Phase 1 implementation, then given a
full architecture proposal in `docs/architecture/IdentityService.md`
during the v0.7 Architecture Completion Phase. This item's own text said
promoting that proposal to `src/interfaces`/Volume 3 "remains a decision
for an explicitly-declared implementation phase" -- that phase has now
happened. `src/interfaces/IdentityService.kt` and
`src/runtime/InMemoryIdentityService.kt` implement the proposed
`register`/`resolve`/`updateStatus`/`touch`/`listByOwner` shape exactly,
and `src/contracts/PrincipalLifecycle.kt` adds `PrincipalLifecycleTransitions`
(closing the Principal half of item #5 below). See the "Identity Service
Implementation" section near the end of this document for what was
deliberately still left out (cascading revocation, event publishing, real
authentication, PermissionEngine integration) and the interpretive
choices made where the architecture document left more than one option
open.

## 2. `ExecutionRequest`: prose spec and JSON Schema disagreed

**Status: Resolved.**

`docs/schemas/ExecutionRequest.schema.json` now includes `expiresAt`
(optional, `string`/`null`, `format: date-time`) and `correlationId`
(required, `string`), matching the prose spec and this implementation's
Kotlin. The worked example (`docs/schemas/examples/ExecutionRequest.example.json`)
was updated to match.

## 3. Several referenced types had no defined shape anywhere

**Status: Resolved** (as provisional, reviewable specifications — not as
finished designs; each new document says so explicitly).

Added `docs/specifications/volume-03-core-interfaces/{PermissionExplanation,ToolResult,ToolDescriptor,CancellationResult,ExecutionStatus}.md`,
each with Purpose/Required Fields/Normative Requirements and an "Open
Questions" section listing what the entry deliberately leaves unresolved.
`VOLUME_3_INDEX.md` now lists these under a new "Supporting Types"
section. `Resource.sensitivity` is addressed separately — see new finding
below; it turned out to already have a defined enum I'd missed.

## 4. `ADR-005` was cited but does not exist

**Status: Resolved** (by removal, not invention).

`EventBus.md`'s dangling "ADR-005" citation has been replaced with an
explicit note that no such ADR exists and none was invented to fill the
gap, per your explicit instruction not to invent an ADR unless required.
**Requires human decision:** whether a real ADR should be authored for
event authentication requirements, and by whom.

## 5. `Principal` and `Resource` lifecycles had no transition rules

**Status: Partially resolved -- Principal half closed by the Identity
Service Implementation phase; Resource half still deferred.**

Added `docs/diagrams/principal-lifecycle-state-machine.mmd` and
`docs/diagrams/resource-lifecycle-state-machine.mmd`, each a literal,
un-embellished transcription of the linear chain already stated in prose
(no invented branching — e.g. whether a Suspended principal can return to
Active is still not stated, so no such edge is diagrammed). Both contract
`.md` files now reference their diagram.

**Deferred:** writing `PrincipalLifecycleTransitions`/`ResourceLifecycleTransitions`
Kotlin validators (parallel to `ExecutionLifecycleTransitions`) was treated
as runtime/code work, out of scope for this documentation-only pass. Now
that diagrams exist, this is a well-specified, low-risk follow-up.

**Update (Identity Service Implementation phase):** the Principal half is
now done -- `PrincipalLifecycleTransitions` (`src/contracts/PrincipalLifecycle.kt`)
enforces the exact linear chain in the diagram, no branching added.
`ResourceLifecycleTransitions` remains undone; it is a Resource Registry
concern, not this phase's scope.

## 6. Volume 2 "core schemas" markdown files were templated placeholders

**Status: Resolved.**

All 10 Volume 2 files plus `VOLUME_2_INDEX.md` now state their `.schema.json`
file is the normative source and give a real required/optional field list
and key enumerations, rather than generic boilerplate. Where writing these
summaries surfaced a real prose/schema/Kotlin disagreement, the summary
says so explicitly instead of papering over it — see the three new
findings below, all surfaced this way.

## 7. Build-scope exclusion of eight later-phase interface stubs

**Status: Unchanged / not a gap requiring resolution.**

Recorded for traceability only, as before; this is a `build.gradle.kts`
configuration choice, not a specification gap, and remains in effect.

---

## New findings from this cleanup pass

### 8. `Permission.schema.json` and `PermissionDecision.schema.json` disagree

**Status: Requires human decision.**

Two separate schema files both describe the same concept ("PermissionDecision")
with different field sets: `Permission.schema.json`'s `action` enum omits
`SEND_EXTERNAL` and adds `reason`/`requiresConfirmation` properties that
`PermissionDecision.schema.json` doesn't have. `PermissionDecision-Schema.md`
now documents both and treats `PermissionDecision.schema.json` as
authoritative (it matches Volume 3's naming and this implementation), but
this is a judgment call, not a resolution — **someone needs to decide
whether `Permission.schema.json` should be deleted, merged, or repurposed
for a distinct concept** (e.g. a tool-declared permission *requirement*,
distinct from a *decision* record).

### 9. `Principal.schema.json` disagreed with the prose `Principal` contract and this implementation's Kotlin

**Status: Resolved.**

Decision made: the prose contract and Kotlin implementation agreed with
each other, so `docs/schemas/Principal.schema.json` was the inconsistent
artifact and has been reconciled to match them, not the other way around.

`docs/schemas/Principal.schema.json` now uses `USER, SYSTEM, INTERNAL_AGENT,
PLUGIN, TOOL, SCHEDULED_TASK, DEVELOPER_SESSION, FUTURE_REMOTE_DEVICE` for
`principalType`, and requires `owner` (nullable -- a root User/System
principal may have none) and `lastSeenAt` (non-null, `format: date-time`).
`docs/schemas/examples/Principal.example.json` and
`Principal-Schema.md` were updated to match; the disagreement language in
the latter has been removed. `src/contracts/Principal.kt` required no
change, since it was already correct.

### 10. `Resource.sensitivity` does have a defined enum — earlier finding was wrong

**Status: Resolved (correction), and now fully closed by the targeted
refinement pass.**

Gap #3's original entry said no value set was defined for `Resource.sensitivity`
anywhere. That was based on reading only the prose `Resource.md`, not
`Resource.schema.json` — which does define one (`PUBLIC`, `PERSONAL`,
`HOUSEHOLD`, `FINANCIAL`, `MEDICAL`, `LEGAL`, `SECURITY_SENSITIVE`,
`CREDENTIALS_SECRETS`, `THIRD_PARTY_PERSONAL_DATA`). `Resource-Schema.md`
now documents this enum. **The targeted refinement pass added
`ResourceSensitivity` (`src/contracts/Resource.kt`) with exactly these nine
values and retyped `Resource.sensitivity` from `String` to it** — see item
#29, the follow-up code fix this item recommended, now done.


---

## v0.7 Architecture Completion Phase — additional resolutions and findings

### 11. No `ToolRegistry` interface existed (consistency review §2.1)

**Status: Resolved at the architecture level; Kotlin not implemented.**

`docs/architecture/tool-registry.md` now specifies purpose, responsibilities,
registration model, discovery model, lookup process, version handling,
plugin integration, runtime lifecycle, failure behaviour, thread safety
expectations, and relationships to the Resource Registry, Permission
Engine, and Execution Pipeline, with sequence diagrams. It also proposes
extending `ToolDescriptor` with `supportedActions`/`supportedResourceTypes`
fields to make dispatch deterministic, answering that document's own
previously-open question. This is a specification, not an implementation
— no Kotlin was written or changed.

### 12. No mapping from `ExecutionRequest.proposedActions` to `PermissionDecision.action` (consistency review §2.3)

**Status: Resolved at the architecture level.**

`docs/architecture/action-mapping.md` defines the complete
Intent -> Planner -> Proposed Actions -> Permission Actions -> Permission
Engine -> Decision process, including transformation rules (a
Planner-owned action vocabulary table), validation, unknown-action
handling (invalid vs. denied), multiple/composite actions, and plugin-
supplied actions. This document is now part of Parker's Trust
Architecture. No Kotlin was written.

### 13. `EventBus` supporting types were unspecified (consistency review §2.2)

**Status: Resolved.**

`EventType`, `EventHandler`, `Subscription`, and `PublishResult` are now
each specified as Volume 3 supporting types
(`docs/specifications/volume-03-core-interfaces/{EventType,EventHandler,Subscription,PublishResult}.md`).
`EventBus.md` was rewritten to add lifecycle, authentication,
authorisation, ordering, delivery guarantee, failure handling,
cancellation, and security sections. `VOLUME_3_INDEX.md`'s Supporting
Types section lists all four. No Kotlin `ParkerEvent` type or `EventBus`
implementation was added — still deferred to whichever phase implements
EventBus, per `Event-Schema.md`.

### 14. `ADR-004` dangling citation in `Agent.md`

**Status: Resolved** (by removal, same treatment as `ADR-005` in
`EventBus.md`).

**Requires human decision** (carried over, same as item 4): whether a
real ADR should be authored for Agent-specific accountability rules.

### 15. `ExecutionResult.schema.json` and `Resource.schema.json` were missing prose-required fields

**Status: Resolved** for the unambiguous cases.

`ExecutionResult.schema.json` now defines `toolResults` and
`reflectionCandidate`, matching `src/contracts/ExecutionResult.kt`.
`Resource.schema.json` now defines `createdAt`, `updatedAt`, and `source`
(now required), matching `Resource.md` and `src/contracts/Resource.kt`.
Both worked examples were updated to match.

**New finding, requires human decision:** while fixing `Resource.schema.json`,
found its `resourceType` enum has 18 values, but `Resource.md`'s "Resource
Categories" list and `src/contracts/Resource.kt`'s `ResourceType` enum
both independently agree on only 14 — the schema additionally has
`SESSION`, `EVENT`, `TASK`, `WORKFLOW`. Unlike the Principal mismatch
(where prose+Kotlin agreed and the schema was simply wrong), this one
cuts the other way: the schema may be *ahead* of prose/Kotlin, since
Session/Event/Task/Workflow each already have their own canonical schemas
and plausibly belong in the Resource Registry's catalogue (Chapter 8).
Not resolved here — recorded in `Resource-Schema.md` — because deciding
which 14 or 18 (or a different set) is correct is a content decision
about what counts as a Resource, not a mechanical fix.

### 16. `Permission.schema.json` / `PermissionDecision.schema.json` duplication (item 8, restated)

**Status: Still requires human decision; now explicitly labelled.**

`Permission.schema.json` now carries a `$comment` marking it deprecated in
favour of `PermissionDecision.schema.json` and pointing back to this item,
so a future reader of the schema file itself (not just this document)
sees the warning. `VOLUME_1_INDEX.md`'s "Included Schemas" list was also
corrected — it previously listed `Permission.schema.json` as Volume 1's
canonical schema for this concept, which conflicted with the judgment call
already made in `PermissionDecision-Schema.md`. The file itself was
**not deleted** — deletion, merging, or repurposing still requires a human
decision, as before.

### 17. Twelve original Volume 3 interface docs had no version/status header (consistency review §3.6)

**Status: Resolved.**

All twelve (`ExecutionPipeline.md`, `PermissionEngine.md`,
`ResourceRegistry.md`, `Tool.md`, `Agent.md`, `Plugin.md`,
`MemoryStore.md`, `WorldModel.md`, `ModelManager.md`,
`NotificationService.md`, `AuditService.md`, and `EventBus.md`, the
latter rewritten more substantially per item 13) now carry a `## Status`
header. Content of the eleven not otherwise touched this phase is
unchanged — only the header was added, honestly noting the content's
actual last-substantive-revision point.

### 18. `README.md` / `CHANGELOG.md` still said "v0.4" (consistency review §3.7)

**Status: Resolved.**

Both now describe the actual current state (Chapters 1-50, 20 ADRs,
Volumes 1-3, Phase 1 Kotlin contracts, the v0.6 consistency review, and
this v0.7 Architecture Completion Phase), with a CHANGELOG entry per
version.

### 19. `RequestOrigin.AGENT` vs `PrincipalType.INTERNAL_AGENT` (consistency review §3.8)

**Status: Resolved** (clarifying note added, no rename).

`ExecutionRequest.md` now explains why these are two different fields
(request channel vs. actor type) that happen to share a word, rather than
the same field disagreeing with itself — confirmed this is not the same
class of bug as the `Principal.schema.json` enum-value mismatch that was
genuinely fixed earlier.

### 20. `AgentHealth` is referenced but never defined (new finding)

**Status: Requires human decision for the actual type definition; the gap
is now explicitly flagged in `Agent.md` itself (targeted refinement pass),
so it is no longer silently discoverable only by reading Kotlin.**

While fixing `Agent.md`'s `ADR-004` citation (item 14), found that both
`Agent.md` and `src/interfaces/Agent.kt` reference a return type
`AgentHealth` for `Agent.health()` that is defined nowhere — the same
category of gap the `EventBus` supporting types were before this phase.
**Not resolved here**, because Agent Framework implementation is outside
this phase's five priorities (Tool Registry, Action Mapping, EventBus,
Identity Service, remaining lifecycle models) and outside
`IMPLEMENTATION_ORDER.md`'s near-term phases — fixing it now would mean
reaching into architecture this phase wasn't scoped to complete. Recorded
here so it isn't lost, and so a future Agent-Framework-focused phase
starts with this already flagged rather than rediscovering it.


---

## Phase 2 Runtime Implementation (feature/phase-2-runtime) — findings

Recorded while implementing Tool Registry, Action Mapping, and EventBus
per the v0.7 architecture documents. No AI logic, agents, memory, world
model, voice, Android UI, Home Assistant, email, calendar, or autonomous
planning was implemented, per that phase's explicit scope.

### 21. No formal Volume 3 `ToolRegistry.md` interface document exists

**Status: Resolved by the targeted refinement pass.**
`docs/specifications/volume-03-core-interfaces/ToolRegistry.md` now exists,
summarizing the already-implemented `src/interfaces/ToolRegistry.kt`
exactly as built, and `VOLUME_3_INDEX.md`'s Included Interfaces list now
names it. No new Tool Registry behaviour was introduced.

`docs/architecture/tool-registry.md` (an architecture document) was
implemented directly as `src/interfaces/ToolRegistry.kt`. Every other
Volume 3 interface (`ExecutionPipeline`, `PermissionEngine`,
`ResourceRegistry`, `Tool`, `EventBus`, ...) has a matching
`docs/specifications/volume-03-core-interfaces/*.md` document; `ToolRegistry`
does not yet. Recommended follow-up: backfill a `ToolRegistry.md` in that
directory now that the interface shape has been implemented and proven
against real usage (registration, resolution, lifecycle), rather than
before -- the Kotlin surfaced two decisions (see items 22-23) worth
folding back into that doc when it's written.

### 22. `InMemoryResourceRegistry` added as a supporting dependency, not one of the three requested systems

**Status: Deliberate, documented addition.**

`docs/architecture/tool-registry.md`'s registration invariant ("every
registered Tool MUST also have a corresponding Resource Registry entry")
cannot be enforced against `ResourceRegistry` with no working
implementation. `IMPLEMENTATION_ORDER.md` places Resource Registry in an
earlier phase than Tool Registry, so `src/runtime/InMemoryResourceRegistry.kt`
is a small, boring implementation of the *already-specified*
`ResourceRegistry` interface -- no new architecture invented, just a
missing prerequisite filled in because Tool Registry's own architecture
document requires it to exist.

### 23. Tool Registry's discovery surface has no Principal-scoped visibility filtering

**Status: Known scope reduction, documented in `ToolRegistry.kt`'s own KDoc.
Partially unblocked, not yet wired.** `IdentityService`/`InMemoryIdentityService`
now exist as a concrete implementation (Identity Service Implementation
phase), so the "resolving a caller's Principal" half of this blocker is
no longer missing. `ToolRegistry`'s discovery methods are not wired to it
-- doing so, and deciding what "some plausible Permission path" means,
is still blocked on a policy-bearing `PermissionEngine`, which remains
unimplemented.

`tool-registry.md`'s Discovery Model specifies that a Tool descriptor
should only be visible to a caller with "some plausible Permission path"
to it. Implementing that requires resolving a caller's Principal
(IdentityService, still unimplemented per item 1) and evaluating
plausible permission paths (a policy-bearing PermissionEngine, not yet
built). `listAll()`/`findCandidates()` currently return the full
catalogue unfiltered by caller. Closing this is blocked on IdentityService
and real PermissionEngine policy, not on anything Tool-Registry-specific.

### 24. Registration and lifecycle changes are not gated by a live Permission Engine evaluation

**Status: Known scope reduction, by design.**

`tool-registry.md` specifies that registering a Tool, or changing its
lifecycle state, should itself be evaluated as a `PermissionAction.CONTROL`
decision. No concrete `PermissionEngine.evaluate` with real authorisation
policy exists (this phase only implements the *action-mapping* layer that
sits before evaluation, per `action-mapping.md` -- it does not implement
policy/authorisation itself, which is unspecified). `InMemoryToolRegistry.register`
and `.setLifecycleState` therefore perform no live permission check.
Wiring this in is a follow-up once a policy-bearing PermissionEngine
exists.

### 25. Action Mapping implements the vocabulary/mapping layer only, not `PermissionEngine.evaluate`

**Status: Closed by Sprint 2, Unit A2 (commit pending).** `DefaultPermissionPolicy`
(`src/runtime/DefaultPermissionPolicy.kt`) now implements the policy model
`docs/specifications/volume-03-core-interfaces/PermissionPolicy.md`
describes, and `DefaultPermissionEngine`
(`src/runtime/DefaultPermissionEngine.kt`) delegates every Active-principal
request to it, after Unit A1's identity-status gating has already run.
`DefaultPermissionPolicy` re-derives the same `PermissionAction`/
`ResourceType` pairs `ActionMapper` and `ResourceRegistry` already produce
for a request, rather than inventing a second interpretation, so
`PermissionDecision.action` stays consistent with what
`DefaultExecutionPipeline` resolved moments earlier. Unknown action,
unknown resource, unknown permission, and "no matching policy rule" all
resolve to `DENIED`, per PermissionPolicy.md's own conservative default --
confirmed by `tests/runtime/DefaultPermissionPolicyTest.kt` and
`tests/runtime/DefaultPermissionEngineTest.kt` (Android Studio: 253/253
tests passing). `APPROVED_WITH_CONFIRMATION` is returned as a policy
outcome only; no confirmation UI or workflow was implemented, and none of
RBAC, ABAC, capability security, delegated authority, temporary
permissions, policy persistence, policy editing, organisation policy, or
plugin policy was introduced -- all remain out of scope, per
PermissionPolicy.md's own Non-Goals and Future Considerations. Original
finding retained below for historical context (describes the
pre-Unit-A2 state):

`src/runtime/ActionMapper.kt` implements exactly the process
action-mapping.md specifies as sitting *before* the Permission Engine:
resolving `ExecutionRequest.proposedActions` strings to
`PermissionAction`/`ResourceType` pairs via a vocabulary table. It does
not implement `PermissionEngine.evaluate` itself, because no
authorisation policy (who may do what, under what circumstances) is
specified anywhere in the architecture yet -- inventing one was out of
scope. Wiring `ActionMapper`'s output into a concrete
`PermissionEngine.evaluate` is the natural next step once policy exists.

### 26. EventBus authentication and signature verification are placeholders, not real trust decisions

**Status: Documented gap, deliberate stand-ins. `IdentityService` now
exists as a real candidate for `InMemoryEventBus`'s injected
`PrincipalAuthenticator` seam** (resolve a Principal, check its status is
ACTIVE) -- not wired in this phase, since that would be new integration
work beyond this round's explicit scope. Recorded as a low-risk,
well-specified follow-up.

`InMemoryEventBus` accepts an injected `PrincipalAuthenticator`
(`suspend fun isInGoodStanding(principalId): Boolean`) as the seam where
real IdentityService integration will plug in; the default
`AllowAllPrincipalAuthenticator` treats every syntactically valid
`PrincipalId` as active, which is not a real trust decision. Separately,
the "trust-sensitive event types require a signature" rule
(`permission.*`/`execution.*`) is implemented as a **presence/non-blank
check only** -- no cryptographic signature verification is implemented,
since no signing scheme is specified anywhere in the architecture and
inventing one was out of scope for this phase.

### 27. EventBus subscriber Principal identity is not asserted

**Status: Resolved by the targeted refinement pass.**
`EventBus.subscribe` now takes an explicit `subscriberPrincipalId:
PrincipalId` parameter; `InMemoryEventBus` uses the caller-supplied value
directly instead of a placeholder. This makes subscriber identity
explicit only -- it does not implement IdentityService and does not
implement the cascading-cancellation-on-Revoke behaviour described below
(original text retained for that reason):

`EventBus.subscribe` is a synchronous (non-suspend) call per its existing
Volume 3 interface stub, with no caller-context parameter to identify the
subscribing Principal. `InMemoryEventBus` currently stamps every
`Subscription.subscriberPrincipalId` with a placeholder value
(`"system.event-bus.unresolved-subscriber"`). This means the
cascading-cancellation-on-Principal-Revoke rule from `EventBus.md`
("Security Considerations"/Subscription.md) is **not implemented** --
there's no real subscriber identity to cascade from yet. Closing this
requires either changing the interface (a Volume 3 change, out of scope
here) or resolving subscriber identity from ambient coroutine context
(not yet designed anywhere).

### 28. `docs/diagrams/tool-lifecycle-state-machine.mmd` does not exist as a standalone file

**Status: Resolved by the targeted refinement pass.** The standalone file
now exists, transcribed verbatim from `tool-registry.md`'s inline
diagram -- no new states or transitions. Original finding retained below
for context:

`src/contracts/ToolLifecycle.kt`'s `ToolLifecycleTransitions` was written
directly from `docs/architecture/tool-registry.md`'s inline
`stateDiagram-v2` block, the same one that document's own "Runtime
Lifecycle" section describes. A standalone `.mmd` file (matching the
pattern used for Principal/Resource/Session/Task/Workflow) was not
produced this round; recorded as a documentation-completeness follow-up,
not a behavioural gap -- the Kotlin and the architecture doc's diagram
agree.

### 29. `Resource.sensitivity: String` (item 5/10, restated) still not changed to the enum

**Status: Resolved by the targeted refinement pass.** See item #10 --
`ResourceSensitivity` now exists and `Resource.sensitivity` uses it.
`tests/contracts/ResourceTest.kt` and all other `Resource(...)`
construction sites were updated to the enum; `Resource-Schema.md` no
longer recommends this as a pending follow-up.


---

## Runtime Integration (Priority 4, feature/phase-2-runtime) — findings

Recorded while wiring `DefaultExecutionPipeline` (Execution Pipeline ->
Permission Engine -> Tool Registry -> Event Bus). Per the "Authority"
process (stop, record, recommend smallest correction, continue only if
safe), each item below was hit *during* implementation and handled by
implementing the smallest safe workaround while recording the real fix as
an open decision -- none were silently invented around.

### 30. `PermissionEngine.evaluate`'s signature doesn't match action-mapping.md's "once per action" description

**Status: Documentation half resolved by the targeted refinement pass;
interface question still requires human decision.**
`action-mapping.md`'s "Multiple Actions" section was rewritten to
correctly describe the interface as it actually exists (`evaluate` called
exactly once per `ExecutionRequest`; `PermissionDecision.action` records
a "primary mapped action"; multi-action handling lives entirely in the
action mapping layer) -- the prose no longer disagrees with the Kotlin.
`PermissionEngine.evaluate`'s signature was deliberately NOT changed, per
this pass's explicit scope. Original finding retained below for the
still-open interface question:

`action-mapping.md` ("Multiple Actions") states "each resolved Permission
Action is evaluated as its own `PermissionEngine.evaluate` call, producing
its own `PermissionDecision`." But the existing Volume 3 interface
(`suspend fun evaluate(request: ExecutionRequest): PermissionDecision`,
predating that document, from Phase 1) has no parameter identifying
*which* action is being evaluated when a request maps to more than one
`PermissionAction`. `DefaultExecutionPipeline` calls `evaluate(request)`
exactly once per request, matching the interface as it actually exists
today, rather than inventing a different signature. **Recommended
smallest correction:** either add an `action: PermissionAction` parameter
to `evaluate`, or add a batch-evaluation variant, and reconcile
`action-mapping.md`'s prose with whichever is chosen.

### 31. The ExecutionLifecycleState transition table has no `CREATED -> FAILED` edge for validation failures

**Status: Resolved by the targeted refinement pass.** `CREATED -> FAILED`
is now a legal edge in `ExecutionLifecycleTransitions` and
`execution-state-machine.mmd`; `DefaultExecutionPipeline` now calls
`transition(..., FAILED)` on both validation-failure paths instead of
leaving the tracked state at `CREATED`. No new lifecycle states were
added. Original finding retained below for context:

`action-mapping.md` says an unresolvable proposed action (or, by the same
reasoning, an unresolvable target Resource) is "Invalid, not Denied" and
should fail before ever reaching `PermissionPending`. But
`ExecutionLifecycleTransitions` (locked in and tested since Phase 1) only
permits `CREATED -> {VALIDATED, EXPIRED}` -- there is no legal edge
representing "this request failed validation." `DefaultExecutionPipeline`
does not force an illegal transition or add an edge unilaterally to a
contract with existing passing tests; instead, on a validation failure it
returns an `ExecutionResult` with `status = FAILED` (a value
`ExecutionResultStatus` already supports, independent of
`ExecutionLifecycleState`) while leaving the tracked lifecycle state at
`CREATED` -- an honest "never legally left Created," not a fabricated
edge. **Recommended smallest correction:** add `CREATED -> FAILED` (or a
new dedicated pre-permission-evaluation terminal state, e.g. `INVALID`)
to the state machine and to `ExecutionLifecycleTransitions`.

### 32. No concrete `Tool` implementation exists to actually invoke

**Status: Closed by Sprint 1, Unit 11A (commit `13c9322`).**
`DefaultExecutionPipeline` now obtains the bound `Tool` via
`ToolInvocationBinding.invocableFor` once `ToolRegistry.resolve` returns
a `ToolDescriptor`, and calls `Tool.validate()` then `Tool.execute()`
(`src/runtime/DefaultExecutionPipeline.kt`, `executeResolvedTool`). A
`SUCCESS` `ExecutionResult` now means a Tool actually ran, not merely
that the right one was found -- confirmed by
`tests/runtime/DefaultExecutionPipelineTest.kt`'s Unit 11A tests,
including that `PermissionEngine` denial, an unbound descriptor, a
failed `Tool.validate()`, and a failed `Tool.execute()` are all handled
without a fabricated success path. Original finding retained below for
historical context (describes the pre-Unit-11A state):

`ToolRegistry.resolve` deliberately returns a `ToolDescriptor`, never a
live `Tool` (per `docs/architecture/tool-registry.md`'s own design: models
and planners never hold executable references, only the Execution
Pipeline does). But no concrete `Tool` implementation exists anywhere in
this codebase to resolve *to* -- Tool implementations are explicitly out
of this phase's scope (and every other phase's scope so far). A `SUCCESS`
result from `DefaultExecutionPipeline` therefore means "every orchestration
stage up to and including finding the right Tool succeeded," not "a Tool
actually ran." This is disclosed prominently in `DefaultExecutionPipeline`'s
own KDoc and in the Phase 2 completion report -- recorded here so it
isn't lost as a footnote.

**Sprint 1 contract-closure addendum.** The missing *contract* between a
resolved `ToolDescriptor` and something invocable is now named:
`src/contracts/ToolInvocationBinding.kt` adds `ToolInvocationBinding`
(`bind(descriptor, tool)` / `invocableFor(descriptor): Tool?`) as a
minimal, purely additive, Execution-Pipeline-only lookup a future
implementation MAY wire up so `DefaultExecutionPipeline` can go from an
already-`Resolved` descriptor to an actual `Tool` instance, without
changing `ToolRegistry.resolve`'s or `ToolResolution.Resolved`'s existing,
already-tested shape. At the time this addendum was written, this closed
only the *contract-shape* half of this gap -- an implementation of
`ToolInvocationBinding`, a concrete `Tool`, and the
`DefaultExecutionPipeline` wiring to call it were all still outstanding
(`docs/implementation/SPRINT_1_VERTICAL_SLICE_PLAN.md` Units 3-4;
`docs/implementation/SPRINT_1_BLOCKER_CLOSURE.md` records the full
investigation, including why this was chosen over folding an invocable
handle directly into `ToolResolution.Resolved`). **All three were
subsequently completed**: `InMemoryToolInvocationBinding` (Unit 4,
`src/runtime/InMemoryToolInvocationBinding.kt`), `MockTool` (Unit 4,
`tests/runtime/MockTool.kt`), and the `DefaultExecutionPipeline` wiring
itself (Unit 11A, commit `13c9322`) -- see this item's own Status line
above.

### 33. `execution.timed_out` (an event name from the original runtime task's lifecycle list) has no corresponding `ExecutionLifecycleState`

**Status: Minor terminology gap, not blocking.**

The original Parker Runtime v0.1 task listed lifecycle events including
"execution timed out." `ExecutionLifecycleState` has `EXPIRED` (checked
before execution begins, for `ExecutionRequest.expiresAt`) and `FAILED`
(after execution), but no distinct "timed out mid-execution" state or
event. `DefaultExecutionPipeline` does not implement execution timeouts at
all (no concrete Tool runs long enough for this to matter yet) and does
not publish an `execution.timed_out` event. Recorded for whichever future
phase adds real Tool execution with duration limits.

### 34. `DefaultExecutionPipeline`'s simplifications, restated for visibility

**Status: Documented design choices, not gaps requiring a decision.**

Processing is synchronous within `submit()` (no background execution
queue -- `QUEUED`/`EXECUTING` are transitioned through within the same
call); `APPROVED_WITH_CONFIRMATION` is treated identically to `APPROVED`
(a real confirmation workflow is Chapter 42 territory); lifecycle events
published to the Event Bus use a placeholder signature
(`"internal-execution-pipeline-authority"`), consistent with the
already-recorded placeholder pattern in item 26 (no real signing scheme
is specified anywhere).

---

## Targeted Refinement Pass (feature/phase-2-runtime) — closed items

Executed the seven specific small refinements identified in
`IMPLEMENTATION_REFINEMENTS.md`, and only those -- not a redesign, not
Phase 3. Items closed: #10, #20 (doc clarification only, decision still
open), #21, #27, #28, #29, #30 (doc clarification only, interface
decision still open), #31. Items #1, #8, #16, #22-26, #32-34 remain open
exactly as previously recorded -- none were touched by this pass.

---

## Identity Service Implementation (feature/phase-2-runtime) — findings

Recorded while implementing `IdentityService`/`InMemoryIdentityService`
per `docs/architecture/IdentityService.md`. No authentication providers,
OAuth, biometrics, Android account integration, remote identity
federation, agent runtime, memory, world model, AI/LLM logic, plugins, or
Home Assistant/email/calendar integration was implemented, per that
phase's explicit scope.

### 35. Cascading revocation was not implemented -- left fully conservative

**Status: Deliberate scope boundary, matches an explicit instruction.**

`IdentityService.md` ("Trust Relationships") says the Identity Service
"MUST evaluate whether any Principal it owns... should also transition"
on Revoke, but in the same breath leaves "the exact cascading rule
(immediate revoke vs. suspend-pending-review)" as an open question rather
than settling it. `InMemoryIdentityService.updateStatus` does not cascade
to owned Principals at all -- revoking a Principal has no effect on
anything it owns. **Requires human decision:** what the exact cascading
rule should be, before it can be implemented without inventing policy.

### 36. `PrincipalLifecycleTransitions` only allows the literal linear chain -- no direct Active -> Revoked, no Suspended -> Active

**Status: Matches the diagram exactly; practical consequence flagged for visibility.**

Per `docs/diagrams/principal-lifecycle-state-machine.mmd` (no branching
specified anywhere), `PrincipalLifecycleTransitions` only permits
`Created -> Active -> Suspended -> Revoked -> Archived`, strictly in
order. A practical consequence: a Principal cannot be revoked without
first passing through Suspended, and a Suspended Principal can never
return to Active. **Requires human decision:** whether real-world
operation needs a direct `Active -> Revoked` edge (e.g. immediate
revocation without a suspend step) or a `Suspended -> Active`
reactivation edge -- neither is invented here.

### 37. `resolve()` does not suppress Revoked or Archived Principals

**Status: Conservative choice between two options the architecture leaves open.**

`IdentityService.md` ("Principal Resolution") says an unresolvable
Principal is "not found, or found but Revoked/Archived" -- but also says
elsewhere the Identity Service should "refuse to resolve, or resolve as
inert" such a Principal, without picking one. `InMemoryIdentityService.resolve`
takes the smaller, easier-to-extend-later option: it always returns the
stored `Principal` record regardless of status, never hiding data.
Treating a Revoked/Archived Principal as "cannot act" is left to callers
(the future Permission Engine integration, item #39 below) rather than
baked into the read path. **Requires human decision:** whether `resolve`
should instead suppress non-Active Principals once a real caller
(Permission Engine) exists to depend on that behaviour.

### 38. Owner validation for delegated Principal types: interpreted as "non-null AND already registered"

**Status: Concrete interpretive decision, recorded for review.**

`IdentityService.md` says `register` "requires an already-established
owning context" for any `PrincipalType` other than `USER`/`SYSTEM`, but
does not spell out an algorithm. `InMemoryIdentityService.register`
interprets this as: `owner` must be non-null, and that `owner` must
already resolve to a registered Principal at registration time. `USER`
and `SYSTEM` Principals may register with a null owner, but are not
*forbidden* from having one (the architecture only states the typical
pattern, not a prohibition). **Requires human decision (low urgency):**
whether this interpretation is correct, and whether `USER`/`SYSTEM`
should in fact be forbidden from having a non-null owner.

### 39. `identity.*` event publishing (Audit integration) not implemented

**Status: Deferred, explicitly out of this phase's allowed-behaviour list.**

`IdentityService.md` ("Interaction with Audit") specifies that every
`register`, `updateStatus`, and resolution failure should emit an
`identity.*` event via the Event Bus for Audit to consume.
`InMemoryIdentityService` has no `EventBus` dependency and publishes
nothing. This phase's task description did not list event publishing
among the allowed/required behaviours, so it was not added rather than
guessed at. Recommended follow-up: wire an `EventBus` dependency through
`InMemoryIdentityService`'s constructor (optional, defaulted, mirroring
`InMemoryEventBus`'s own `PrincipalAuthenticator` injection pattern) once
this is explicitly scoped.

### 40. `PermissionEngine.evaluate` is not yet wired to resolve identity first

**Status: Closed by Sprint 2, Unit A1 (commit pending).** `DefaultPermissionEngine`
(`src/runtime/DefaultPermissionEngine.kt`) now resolves
`request.principalId` via `IdentityService` as the first step of
`evaluate`, before any delegated permission decision runs, and
short-circuits to `DENIED` for a Suspended, Revoked, Archived, Created,
or unresolvable Principal. Only an Active Principal's request reaches
the caller-supplied decision function, and that function's decision is
returned unchanged -- confirmed by
`tests/runtime/DefaultPermissionEngineTest.kt` (Android Studio: 244/244
tests passing). No permission policy content was introduced by this
unit; that remains `IMPLEMENTATION_GAPS.md` #25, still open. Original
finding retained below for historical context (describes the
pre-Unit-A1 state):

`IdentityService.md` ("Integration with Permission Engine") specifies
that `PermissionEngine.evaluate(request)` MUST resolve
`request.principalId` via the Identity Service as its first step, and
short-circuit to `DENIED` for a Suspended/Revoked Principal. This phase's
task explicitly said not to proceed to Permission Engine policy
integration, so `PermissionEngine`/`FakePermissionEngine`/`DefaultExecutionPipeline`
are all untouched by this work. The Identity Service foundation exists
and is ready for that wiring; doing it is the natural next milestone, not
done here.

### 41. `ToolInvocationBinding.invocableFor` and `ToolRegistry.resolve` restrict callers to the Execution Pipeline by convention only, not by construction

**Status: Deliberate scope boundary, matching an existing, older
instance of the identical gap. Distinct from item #32 (which was about
no invocation existing at all -- now closed by Sprint 1, Unit 11A).**

Both `InMemoryToolInvocationBinding.invocableFor` and
`InMemoryToolRegistry.resolve` are documented as "Execution-Pipeline-only"
in their own KDoc, but neither performs a caller-identity check or
restricts visibility in any way -- any caller in the same process can
call either method directly. `InMemoryToolInvocationBinding`'s own KDoc
states this plainly: enforcing the restriction "would mean introducing a
caller-identity or visibility mechanism that does not exist anywhere
else in this repository today (including on `ToolRegistry.resolve`
itself, which this type is deliberately built to match)."
`docs/implementation/SPRINT_1_VERTICAL_SLICE_PLAN.md` §7's own
acceptance criterion for this mechanism -- "true by construction... not
merely true by convention" -- is not fully met by either method.
**Requires human decision:** whether closing this requires a
caller-identity/visibility mechanism (which would be a first for this
repository, per `ToolRegistry.resolve`'s identical, pre-existing
limitation), or whether the convention-based restriction remains
acceptable for the platform's current trust model.

### 42. `InMemoryTaskManagerRuntime` does not subscribe to Agent lifecycle events

**Status: Closed by Sprint 2, Track B, Unit B1 (commit `7bbf909`).**
`InMemoryTaskManagerRuntime` (`src/runtime/InMemoryTaskManagerRuntime.kt`)
now subscribes, once each at construction, to `agent.completed` and
`agent.failed` on its injected `EventBus`, and records each received event
against the correct Task by reading the `taskId` entry
`InMemoryAgentRuntime.publish` already carries in every `agent.*` event's
payload -- confirmed by `tests/runtime/InMemoryTaskManagerRuntimeTest.kt`
(Android Studio: 261/261 tests passing). This unit closes only the
subscription/recording half of this gap: no `TaskLifecycleTransitions`
call and no `Task.status` mutation results from any Agent Event -- Task
status transition behaviour in response to an Agent Event remains
intentionally deferred to Sprint 2 Track B, Unit B2, per
`SPRINT_2_IMPLEMENTATION_PLAN.md`'s own two-unit split. `agent.cancelled`,
`agent.action_denied`, and `agent.action_deferred` remain unsubscribed,
since no production code emits any of the three today (see this gap's
original finding below, which still describes their status accurately).
Original finding retained below for historical context (describes the
pre-Unit-B1 state):

`docs/architecture/TaskManagerRuntimeSpecification.md` §6 and §11 already
specify that the Task Manager Runtime subscribes to Agent Run lifecycle
events (`agent.completed`, `agent.failed`, `agent.cancelled`,
`agent.action_denied`, `agent.action_deferred`) so that Agent Run outcomes
can be recorded against the relevant Task. No numbered gap has previously
recorded that `src/runtime/InMemoryTaskManagerRuntime.kt` does not yet do
this: it holds an `EventBus` dependency (added by Sprint 1 Unit 9) only to
*publish* `task.*` events, and calls no `EventBus.subscribe` of its own.

Of the five event types §6 names, only `agent.completed` and
`agent.failed` are currently emitted by any Sprint 2 runtime code --
`src/runtime/InMemoryAgentRuntime.kt`'s own class KDoc states it "only
ever drives `CREATED -> INITIALISED -> READY -> RUNNING -> {COMPLETED,
FAILED}`," so these are the only two of the five events a subscriber
could observe from production code today. `agent.cancelled` remains a
specified concept -- `AgentRunStatus.CANCELLED` exists in the lifecycle
enum -- with no production emitter today. `agent.action_denied` and
`agent.action_deferred` correspond to per-action permission outcomes
within an Agent Step, but no production path currently emits either event
because `InMemoryAgentRuntime` does not implement `WAITING_FOR_PERMISSION`
or per-action permission handling.

Closing this gap is scoped in two parts, matching the two-unit split
already recorded in `SPRINT_2_IMPLEMENTATION_PLAN.md`: Unit B1 closes only
the subscription and recording of Agent Events against the correct Task
(observation, not a Task status change); Unit B2 is responsible for what,
if anything, a received Agent Event does to `TaskStatus`. This gap should
not be marked resolved until Unit B1's own subscription/recording scope is
implemented and verified.

---

## Phase 2 Runtime — Gap Closure Summary (all 42 items, current status)

Compiled at the close of Phase 2 Runtime (Tool Registry, Action Mapping,
EventBus, Runtime Integration, Targeted Refinement Pass, Identity Service
Implementation -- all on `feature/phase-2-runtime`), so every item's
current disposition is visible in one place rather than requiring a
scroll through the full history above.

**Resolved:** #1 (IdentityService), #2, #3, #4, #6, #9, #10/#29
(Resource.sensitivity enum), #11 (architecture level), #12 (architecture
level), #13, #14, #15, #17, #18, #19, #21 (ToolRegistry.md backfill), #27
(EventBus subscriber identity), #28 (tool lifecycle diagram), #31
(Created -> Failed edge), #32 (Tool invocation -- closed by Sprint 1,
Unit 11A, commit `13c9322`), #40 (PermissionEngine identity resolution --
closed by Sprint 2, Unit A1, `DefaultPermissionEngine`, commit pending),
#25 (Action Mapping wired into `PermissionEngine.evaluate` via policy --
closed by Sprint 2, Unit A2, `DefaultPermissionPolicy`, commit pending),
#42 (`InMemoryTaskManagerRuntime` Agent-Event subscription/recording --
closed by Sprint 2, Track B, Unit B1, commit pending; Task status
transition behaviour in response to an Agent Event remains deferred to
Unit B2).

**Partially resolved:** #5 (Principal half done via
`PrincipalLifecycleTransitions`; Resource half still deferred), #30
(action-mapping.md's prose now matches the interface; the interface
itself is unchanged, per explicit instruction).

**Deliberate scope boundaries / known, documented limitations (not
defects, not pending):** #7, #22, #23, #24, #26, #33, #34, #35,
#36, #37, #38, #39, #41 (ToolInvocationBinding/ToolRegistry access
enforcement).

**Still requires a human decision:** #8/#16 (`Permission.schema.json` vs
`PermissionDecision.schema.json` duplication), #20 (`AgentHealth`'s
shape), #35 (exact cascading-revocation rule), #36 (whether the Principal
lifecycle needs an Active -> Revoked or Suspended -> Active edge), #37
(whether `resolve()` should suppress non-Active Principals), #38 (whether
the owner-validation interpretation is correct), #41 (whether closing
`ToolInvocationBinding`/`ToolRegistry.resolve`'s convention-based access
restriction requires a caller-identity/visibility mechanism, or whether
convention-based restriction remains acceptable).

No item in this file was closed by inventing behaviour beyond what its
governing architecture document already specified.
