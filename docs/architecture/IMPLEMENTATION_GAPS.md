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

**Status: Closed by Sprint 2, Unit A2 (commit `e7e1bbf`).** `DefaultPermissionPolicy`
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

**Status: Closed by Sprint 2, Unit A1 (commit `4ceeb0e`).** `DefaultPermissionEngine`
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

**Status: Closed by Sprint 2, Track B, Unit B1 (commit `7bbf909`) and
Unit B2 (commit `115fb42`, documentation finalized in `aa5c507`).**
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

**Sprint 2, Track B, Unit B2 update:** `InMemoryTaskManagerRuntime` now
also drives a `TaskStatus` transition in response to `agent.completed`,
closing the second half of this gap. For a Task with exactly one Agent
Run Reference, `agent.completed` moves the Task through both already-valid
`TaskLifecycleTransitions` edges in sequence -- `QUEUED -> RUNNING`, then
`RUNNING -> COMPLETED` -- publishing `task.started` then `task.completed`;
a Task already `RUNNING` takes only the second edge; a Task already
`COMPLETED` is left unmutated. No new lifecycle edge was introduced.
`agent.failed` still performs no transition -- the event is recorded
exactly as Unit B1 already did, and nothing else happens, per this unit's

**Sprint 3, Track C, Unit C2 update (factual correction, not a status
change):** the paragraph above states `agent.cancelled`, `agent.action_denied`,
and `agent.action_deferred` remain unsubscribed "since no production code
emits any of the three today." That reason is now stale.
`InMemoryAgentRuntime` (`src/runtime/InMemoryAgentRuntime.kt`), rewritten by
Unit C2 to implement the multi-step Agent Run model
(`docs/architecture/MULTI_STEP_AGENT_RUN_DESIGN.md`), now genuinely reaches
`WAITING_FOR_PERMISSION` and publishes all three events in production code
-- `agent.action_denied` when a proposed action is `DENIED`,
`agent.action_deferred` when one is `DEFERRED`, and `agent.cancelled` when a
`CANCEL` command is accepted. The gap itself is unchanged and still open
for these three event types: `InMemoryTaskManagerRuntime` still subscribes
to only `agent.completed` and `agent.failed`, and Unit C2's own scope
(Sprint 3, Track C, Unit C2) explicitly does not modify Task Manager
Runtime behaviour. Closing this remaining part of the gap -- deciding what,
if anything, a Task should do in response to an Agent Run being cancelled,
denied, or deferred -- remains future work, not a Unit C2 decision.
own scope (`docs/implementation/SPRINT_2_B2_IMPLEMENTATION_DECISIONS.md`).
Confirmed by `tests/runtime/InMemoryTaskManagerRuntimeTest.kt` (Android
Studio: 269/269 tests passing). A Post-Implementation Review for Unit B2
(`docs/reviews/SPRINT_2_B2_POST_IMPLEMENTATION_REVIEW.md`) was performed
retroactively, after this commit, per PES-001's Level 2 requirement --
recorded there as a process finding, not a defect. The general
Task-completion policy for a Task with more than one Agent Run Reference
remains explicitly out of scope, per `SPRINT_2_IMPLEMENTATION_PLAN.md`'s
own Unit B2 scope text -- not resolved here, not silently assumed.
`agent.cancelled`,
`agent.action_denied`, and `agent.action_deferred` remain unsubscribed and
cause no transition, unchanged from Unit B1's own scope boundary.
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

### 43. `task.started` and `task.completed` publish without their §10-specified payload fields

**Status: Resolved.**

Found during the Sprint 2 Health Review (`docs/reviews/SPRINT_2_B2_POST_IMPLEMENTATION_REVIEW.md`)
performed after Track A and Track B were both implemented.
`TaskManagerRuntimeSpecification.md` §10's event table specifies required
payload beyond `taskId`/`correlationId` for two of the events Unit B2's
`applyCompletedTransition` publishes: "Agent Run Reference, if any" for
`TaskStarted` (`task.started`), and "Task Result summary" for
`TaskCompleted` (`task.completed`). `InMemoryTaskManagerRuntime.applyCompletedTransition`
(`src/runtime/InMemoryTaskManagerRuntime.kt`) publishes both events with
an empty payload map -- neither field is populated. This does not affect
the correctness of any Task Status transition (the transition itself is
unaffected by event payload content) and is not a violation of any
Architecture Decision; it is a specification-completeness gap in the
event's observable detail, consistent with AD-009's "Everything Important
Is Auditable" being about publication occurring at all, not yet about
every specified field being present on every published event. No test
currently asserts either event's payload contents, which is why this went
unnoticed through Unit B2's own review checkpoint. **Recommended closure:**
thread an Agent Run Reference (already recorded in `agentEvents`, per Unit
B1) into the `task.started` payload, and a minimal Task Result summary
into the `task.completed` payload, as a small, additive follow-up --
no interface or lifecycle change required.

**Update (Task Event Payload Completion, `docs/implementation/TASK_EVENT_PAYLOAD_COMPLETION_IMPLEMENTATION_PLAN.md`):
the `task.completed` half is closed; the `task.started` half is deliberately
left open, not silently dropped.** `task.completed`'s payload is now
implemented -- `InMemoryTaskManagerRuntime.applyCompletedTransition`'s two
`task.completed` publish call sites carry `{"taskId": <the Task's own
id>, "status": "COMPLETED"}`, verified passing in Android Studio
(482/482). The "Recommended closure" text above turned out to be only
half achievable as written: `agentEvents` (Unit B1's own recorded
`agent.completed`/`agent.failed` events) does not actually carry an
`agentRunId` field to thread -- confirmed by direct inspection,
`InMemoryAgentRuntime.publish`'s own `agent.completed` payload carries
only `taskId`, never `agentRunId`. `task.started`'s Agent Run Reference
therefore **remains intentionally unimplemented**: closing it would
require either reconstructing `AgentRunId` locally (rejected, as an
unauthorised inference of a hidden identifier by duplicating another
subsystem's internal ID-minting scheme) or extending Agent Runtime
(`InMemoryAgentRuntime.kt`) to expose `agentRunId` on `agent.completed`'s
own payload -- both explicitly out of this Unit's scope. **The remaining
work depends on future Agent Runtime support and remains an open gap**,
not resolved, not closed, and not silently assumed away by this Unit.

**Update (Agent Run Reference Exposure, `docs/implementation/AGENT_RUN_REFERENCE_EXPOSURE_IMPLEMENTATION_PLAN.md`):
closed in full.** The `task.completed` half was implemented by the
Task Event Payload Completion unit above (verified 482/482). This update
closes the remaining `task.started` half: `InMemoryAgentRuntime`'s single
shared `publish(run, eventType)` helper now exposes `"agentRunId"` on
every event it publishes -- not `agent.completed` alone, exposed
uniformly per that plan's own consistency finding.
`InMemoryTaskManagerRuntime.applyCompletedTransition` reads that value
off the triggering `agent.completed` event, via the same `agent.completed`
subscription already established since Unit B1 (`IMPLEMENTATION_GAPS.md`
#42) -- no new `EventBus.subscribe` call was added. `task.started` now
carries `"agentRunId"` when the triggering event has one, and
`emptyMap()` when it does not, matching §10's own "if any" language. No
`AgentRunId` is reconstructed locally anywhere in this repository --
the value is always read directly from Agent Runtime's own published
event, never derived or guessed. `correlationId` behaviour was
intentionally left unchanged throughout: neither its value nor how it is
set on any event was touched by this closure. Verified passing in
Android Studio, 484/484. **The pre-existing `correlationId`-vs-`AgentRunId`
wording tension** between `AgentRuntimeSpecification.md` Section 9's
prose ("`correlationId` set to the Agent Run ID") and `AgentRun.kt`'s own
documented convention (`correlationId` as the shared, cross-subsystem
value, not literally `AgentRunId.value`) **remains a separate
documentation/specification matter, outside this gap's own scope** --
it is not resolved by this closure and is not part of what #43 ever
required.

---

### 44. `ExecutionPipeline.cancel` cannot interrupt a `Tool.execute()` call already in flight

**Status: Open, not yet closed. Surfaced (not created) by Sprint 3,
Track C, Unit C2** -- the first unit to actually call
`ExecutionPipeline.cancel` against a request that may genuinely be
`EXECUTING`. `ExecutionLifecycleTransitions`
(`src/contracts/ExecutionLifecycle.kt`) has never had an
`EXECUTING -> CANCELLED` edge -- `EXECUTING` may only reach `COMPLETED` or
`FAILED`. This was always true of `DefaultExecutionPipeline`, but had no
observable consequence before Unit C2, since no earlier caller ever
invoked `ExecutionPipeline.cancel` against a request that might be
mid-`Tool.execute()`: Sprint 1 Unit 7's `InMemoryAgentRuntime` rejected
`CANCEL` outright, and no other production caller of
`ExecutionPipeline.cancel` exists yet.

`InMemoryAgentRuntime`'s new `cancelRun` (design document
`MULTI_STEP_AGENT_RUN_DESIGN.md` §6.5) calls
`executionPipeline.cancel(requestId)` on a best-effort basis the moment a
`CANCEL` command is accepted, regardless of whether that request is
currently `EXECUTING`. Per the existing transition table, that call
returns `CancellationResult(cancelled = false, reason = "cannot cancel
from state EXECUTING")` whenever a Tool's `execute()` call is genuinely in
progress -- the Agent Run itself still transitions to `CANCELLED`
immediately and correctly (that transition belongs to
`AgentRunLifecycleTransitions`, a separate state machine, and does not
depend on this call's outcome), but the underlying Tool invocation runs to
completion regardless, with its eventual `ToolResult` simply discarded by
`InMemoryAgentRuntime` (confirmed by
`tests/runtime/InMemoryAgentRuntimeTest.kt`'s
"CANCEL received while a step is executing..." test).

This is not a defect in Unit C2's own implementation -- `ExecutionPipeline`
and `Tool` are unchanged, unmodified interfaces, and Unit C2's own scope
explicitly excludes changing them. It is a pre-existing architectural gap
this unit's own honest handling of `CANCEL` makes newly visible: this
repository specifies no mechanism (cooperative cancellation signal,
`kotlinx.coroutines` `Job` cancellation propagated into `Tool.execute`,
etc.) by which a `Tool` can actually be interrupted once it has started
running. **Recommended closure (a future unit's decision, not this one's):**
either add a genuine `EXECUTING -> CANCELLED` edge together with a
cooperative-cancellation contract `Tool.execute` implementations would
need to honour, or explicitly document (e.g. in `Tool.md`) that Tools are
expected to run to completion once started and that `cancel` is
best-effort/advisory only for a request already `EXECUTING`.

---

### 45. No dedicated `planner.session_rejected` event exists for a real, reachable `SUBMITTED -> REJECTED` transition

**Status: Open, not yet closed. Surfaced (not created) by Sprint 3, Track
D, Unit D2** -- the first unit whose production code actually calls
`TaskProposalIntake.submitProposal` from the Planner Runtime side and
reaches `SUBMITTED -> REJECTED` for a real disposition.
`PlannerRuntimeSpecification.md` Section 11 already recorded this as an
Open Question ("Whether a dedicated Planning Event ... should be added for
the `SUBMITTED -> REJECTED` transition ... still open"), but until this
Unit, no code -- production or test -- had ever exercised that transition
at all: `DeterministicPlannerHarness.kt` never calls `submitProposal`, and
no other Planner Runtime implementation existed. `InMemoryPlannerRuntime`
(`src/runtime/`) now reaches this transition for real (confirmed by
`tests/runtime/InMemoryPlannerRuntimeTest.kt`'s "a Task Manager rejection
results in a Rejected Planning Session" test), and, per the specification's
own instruction, does not invent a `planner.session_rejected` event to
cover it -- `PlanningSessionResult.Rejected` is returned to the caller
with no corresponding Planning Event published.

This is not a defect in Unit D2's own implementation -- the specification
explicitly reserves this decision, and inventing an event for it would be
exceeding this Unit's own "no architecture beyond what Unit D1 settles"
scope. It is the same already-recorded Open Question, now attached to a
real, reachable code path instead of a purely theoretical one.
**Recommended closure (a future unit's decision, not this one's):** either
add a `planner.session_rejected` event (Section 11's own first-listed
option) or explicitly close the Open Question the other way, documenting
that a `TaskProposalDisposition.Rejected` return value is itself sufficient
signal and no dedicated event is needed.

---

### 46. `DefaultMemoryPromotionPolicy` implements only two of `33-memory-consolidation.md`'s six named promotion factors

**Status: Open, not yet closed. Surfaced (not created) by Sprint 4, Track
A, Unit A3** -- the first unit to actually implement a
`MemoryPromotionPolicy`. `docs/architecture/33-memory-consolidation.md`
names six promotion factors: repetition, user importance, goal relevance,
frequency, confidence, and explicit request.
`DefaultMemoryPromotionPolicy` (`src/runtime/DefaultMemoryPromotionPolicy.kt`)
implements exactly two of them -- explicit request (unconditional
promotion) and confidence (promotion at or above a fixed threshold) --
deterministically and without randomness, per this Unit's own
instructions.

The remaining four factors -- repetition, user importance beyond an
explicit request, goal relevance, and frequency -- are not implemented,
and this is a genuine gap, not an oversight quietly worked around: each
of those four requires comparing a submitted `CandidateMemory` against a
population of Memory's own existing `MemoryRecord`s (has this, or
something like it, been said before? how often? how does it relate to
what else Memory already holds?). Neither
`docs/architecture/MEMORY_CONTRACT_DESIGN.md` (Unit A2) nor this
Unit's own instructions shape a way to supply that population to
`MemoryPromotionPolicy.evaluate(candidate, memoryId)` -- its signature
takes only the one `CandidateMemory` under evaluation and its assigned
`MemoryId`. Extending it to accept a queryable view of existing records
would be a genuine interface change, and inventing one now, mid-Kotlin,
without an approved design for what that view looks like (a full
`MemoryStore` reference? a bounded recent-records window? something
else?) would be exactly the kind of unauthorised architecture invention
this Unit's instructions forbid.

This is not a defect in Unit A3's own implementation -- Unit A2 approved
`MemoryPromotionPolicy` as a seam without specifying which or how many of
the six factors any one implementation must weigh, and this Unit's own
instructions asked for "explicit-request promotion factor" and
"confidence-based promotion factor if implemented" specifically, naming
no requirement to implement the other four. It is a real, disclosed
limitation of `DefaultMemoryPromotionPolicy` as a *first* implementation,
not of the `MemoryPromotionPolicy` interface itself, which remains free
of this limitation (a future, more capable implementation could weigh all
six without any interface change, provided it is given a way to see
Memory's existing records). **Recommended closure (a future unit's
decision, not this one's):** either extend `MemoryPromotionPolicy` (or
its concrete implementation's constructor) with an explicit way to
consult existing `MemoryRecord`s, or explicitly document that a
population-comparison-based promotion policy remains future,
model-or-heuristic-backed work, and that `DefaultMemoryPromotionPolicy`'s
two-factor rule is intentionally the minimal deterministic baseline, not
a placeholder awaiting completion.

### 47. `InMemoryWorldModel` does not publish state change events

**Status: Open, not yet closed. Surfaced (not created) by Sprint 4, Track
B, Unit B3** -- the first unit to actually implement `WorldModel`.
`docs/specifications/volume-03-core-interfaces/WorldModel.md`'s
Responsibilities list names five items, one of which is "Publish state
change events." `InMemoryWorldModel` (`src/runtime/InMemoryWorldModel.kt`)
implements the other four (store transient state, track confidence,
expire stale observations, resolve current state) but does not publish
anything to the `EventBus` when a belief is accepted, invalidated, or
excluded at expiry.

This is a genuine, disclosed gap, not a silent omission, and this Unit's
own instructions asked explicitly that it be reported rather than
resolved unilaterally: "If EventBus publication appears necessary because
`WorldModel.md` requires state change events, stop and report how it can
be implemented without giving World Model autonomous orchestration
authority."

**How it could be implemented without granting orchestration authority
(reported, not yet done):** `InMemoryWorldModel` could accept an optional,
injected `EventBus` reference and publish a one-way, read-only event
(for example, `worldmodel.belief_accepted` /
`worldmodel.belief_invalidated`) immediately after `observe` accepts or
invalidates a belief -- strictly as an observability/audit broadcast, the
same role every other Runtime component's own event publication already
plays (`agent.*`, `task.*`, `planner.*`). This would not, by itself, grant
the World Model orchestration authority, provided the same rule already
governing every other event in this codebase continues to hold: no
subscriber may treat receipt of a `worldmodel.*` event as authorization to
act, and the World Model itself never subscribes to anything. Publishing
is a broadcast outward, never a channel back in.

**Why it was not implemented now:** Unit B3's own Implementation Scope and
Testing sections name no `EventBus` requirement, its own "Do NOT
Implement" list says "EventBus publication unless already required by the
existing WorldModel specification and safely scoped," and adding a new
`EventBus` constructor dependency to `InMemoryWorldModel` was judged safer
to report and defer than to add speculatively, mid-implementation, without
an approved event-name/payload contract (no `worldmodel.*` event is named
anywhere in `docs/architecture/16-world-model.md`,
`WORLD_MODEL_RUNTIME_ARCHITECTURE.md`, or `WORLD_MODEL_CONTRACT_DESIGN.md`
today). **Recommended closure (a future unit's decision, not this one's):**
either a small, additive follow-up unit that adds the injected `EventBus`
dependency and the two events described above, or an explicit architecture-level
decision that `WorldModel.md`'s "Publish state change events" responsibility
is satisfied by callers observing `current`/`query` themselves rather than
by the World Model pushing events, closing the Open Question the other
way -- mirroring gap #45's identical two-path resolution for
`planner.session_rejected`.

**Update (Pre-Module Readiness Unit 3):** `docs/adr/ADR-024-module-event-audit-durability-boundary.md`
now gives this gap an architectural decision -- it reaffirms ADR-023's
existing publish-only shape for World Model, adds the module-access
framing this gap's own resolution must respect once modules exist, and
explicitly declines to authorise Memory Runtime publication (Section B).
**This is a design decision, not an implementation.** `InMemoryWorldModel`
is unmodified; this gap's status remains **open, pending implementation**,
now with a settled shape rather than an open design question standing in
front of it.

---

## Phase 2 Runtime — Gap Closure Summary (all 47 items, current status)

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
closed by Sprint 2, Unit A1, `DefaultPermissionEngine`, commit `4ceeb0e`),
#25 (Action Mapping wired into `PermissionEngine.evaluate` via policy --
closed by Sprint 2, Unit A2, `DefaultPermissionPolicy`, commit `e7e1bbf`),
#42 (`InMemoryTaskManagerRuntime` Agent-Event subscription/recording,
closed by Sprint 2, Track B, Unit B1, commit `7bbf909`; Task status
transition on `agent.completed`, closed by Unit B2, commit `115fb42`
(documentation finalized in `aa5c507`); the general Task-completion
policy for a Task with more than one Agent Run Reference remains
explicitly out of scope, not a pending item).

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

**Open, pending implementation (not a human decision, not a deliberate
boundary):** #43 (`task.started`/`task.completed` publish without their
§10-specified Agent Run Reference / Task Result summary payload fields --
found by the Sprint 2 Health Review; recommended closure is a small,
additive follow-up, not a redesign); #44 (`ExecutionPipeline.cancel` cannot
interrupt a `Tool.execute()` call already `EXECUTING` -- a pre-existing
gap surfaced, not created, by Sprint 3 Track C Unit C2's honest handling of
`CANCEL`; recommended closure is either a cooperative-cancellation
contract or explicit documentation that `cancel` is best-effort/advisory
once a Tool is running); #45 (no dedicated `planner.session_rejected`
event for a real, reachable `SUBMITTED -> REJECTED` transition -- an
already-recorded `PlannerRuntimeSpecification.md` Section 11 Open Question,
now attached to a real code path by Sprint 3 Track D Unit D2's
`InMemoryPlannerRuntime`; recommended closure is either adding the event or
explicitly closing the Open Question the other way); #46
(`DefaultMemoryPromotionPolicy` implements only 2 of
`33-memory-consolidation.md`'s 6 named promotion factors --
repetition/user-importance/goal-relevance/frequency require comparing a
submission against Memory's own existing records, which
`MemoryPromotionPolicy.evaluate`'s current signature has no way to
supply; found by Sprint 4 Track A Unit A3; recommended closure is either
extending the seam with a way to consult existing records or explicitly
documenting the two-factor rule as an intentional, non-placeholder
minimal baseline); #47 (`InMemoryWorldModel` does not publish state
change events, per `WorldModel.md`'s own "Publish state change events"
responsibility -- found and explicitly reported by Sprint 4 Track B Unit
B3, per that Unit's own instruction to stop and report rather than
implement unilaterally; recommended closure is either an additive
follow-up adding an injected `EventBus` dependency and two new,
purely-observational events, or an explicit architecture-level decision
that this responsibility is satisfied by callers polling `current`/`query`
instead).

No item in this file was closed by inventing behaviour beyond what its
governing architecture document already specified.

---

## Independent Architecture Audit Findings (Sprint 5)

Recorded following a skeptical, feature-agnostic architectural review of
the repository as it stands today (not scoped to any single unit), and
triaged in `docs/reviews/ARCHITECTURE_V2_INDEPENDENT_AUDIT_TRIAGE.md`.
These four items are logged here for tracking, per that triage document's
"Should fix soon" and "Needs ADR/design unit" categories. None are fixed
by this entry — this is a tracking addition only.

### 48. Deterministic parent-derived IDs cap multiplicity for AgentRunId and TaskProposalId

**Status: Formally constrained by Pre-Module Readiness Unit 2 -- deferred,
not closed as a defect, because none existed.**

`InMemoryAgentRuntime.start()` mints
`AgentRunId("run-for-${command.taskId.value}")`
(`src/runtime/InMemoryAgentRuntime.kt`) and rejects a second `START`
command for the same Task. `InMemoryPlannerRuntime` mints
`TaskProposalId("${request.planningSessionId.value}-proposal-1")`
(`src/runtime/InMemoryPlannerRuntime.kt`) and rejects a second `plan()`
call for the same Planning Session. Both governing specifications leave
the corresponding multiplicity open: Task Manager is documented as
associating "zero, one, or many Agent Runs" with a Task, and the Planner
Runtime specification allows "one or more Task Proposals" per Planning
Session. Neither implementation could previously produce more than one, by
construction of its ID-minting scheme -- not by an explicit architectural
decision to cap it at one, which is what this item originally recorded as
a disclosure gap.

**Decision (`docs/architecture/PRE_MODULE_ID_MULTIPLICITY_DECISION.md`):**
one Agent Run per Task and one Task Proposal per Planning Session is now a
deliberate, documented constraint for the current platform phase --
**deferred**, not prohibited, since no consumer in this repository today
(no Workflow Engine, no retry logic, no Multi-agent planning/Resource
optimisation) needs the wider multiplicity either specification reserves,
and inventing multiplicity-handling with no real consumer to validate it
against was rejected under the same "100,000-line test" already applied
throughout `docs/reviews/SPRINT_4_ARCHITECTURE_ACTIONS.md`. No
ID-generation change, no public contract change, and no retry/forking
logic was introduced. Both implementations' existing exception messages
(thrown on a duplicate `START`/`plan()` call, unchanged in when or how
they fire) were updated to state explicitly that the cap is deliberate
and documented, not accidental, and both existing tests covering this
behaviour (`tests/runtime/InMemoryAgentRuntimeTest.kt` "resubmitting
START for the same taskId...", `tests/runtime/InMemoryPlannerRuntimeTest.kt`
"resubmitting the same planningSessionId...") were tightened to check the
updated message content; one new test per subsystem was added asserting
the message explicitly cites this decision. **Recommended follow-up:**
when a real consumer exists (Workflow Engine re-attempting a Task, or
Multi-agent planning/Resource optimisation splitting one Planning Session
into several Task Proposals), that unit's own Contract Design pass should
adopt a per-parent monotonic counter/sequence for the affected ID (per
the decision document's Section 3), not a caller-supplied or randomly
generated identifier.

### 49. Planner Runtime publisher identity is hardcoded and unresolved

**Status: Closed by Pre-Module Readiness Unit 1.**

`InMemoryPlannerRuntime` previously published every Planner event under a
hardcoded `PrincipalId("system.planner-runtime")` that was never
registered with or resolved through `IdentityService`, while
`InMemoryAgentRuntime` resolved its Agent identity via
`identityService.resolve(agentIdentityPrincipalId)` before a run starts,
and republished under that verified identity. This asymmetry was
invisible only because `InMemoryEventBus`'s wired-in
`AllowAllPrincipalAuthenticator` accepts every syntactically valid
`PrincipalId` (see item #26) -- it would have stopped being invisible the
moment a real, identity-backed `EventBus` authenticator was introduced.

**Fix (Pre-Module Readiness Unit 1):** `InMemoryPlannerRuntime.plan`
(`src/runtime/InMemoryPlannerRuntime.kt`) now calls
`identityService.resolve(PLANNER_RUNTIME_PRINCIPAL_ID)` before publishing
anything -- checked before the initiating Principal, since publishing the
first event (`planner.session_created`) does not depend on who initiated
the request. If unresolved, `plan` rolls back its tentative session
reservation and returns `PlanningSessionResult.Failed` with an explicit
reason, exactly mirroring the existing unresolvable-initiating-Principal
precondition and `InMemoryAgentRuntime`'s own `agentIdentityPrincipalId`
precedent -- no session record is created and no event is published. The
resolved `Principal.principalId` (not the raw constant) is threaded
through every `publish`/`publishRejections` call as an explicit parameter
for the remainder of that `plan` call. No public contract changed:
`PlanningSessionResult.Failed` already existed with a `reason` field.
`tests/runtime/InMemoryPlannerRuntimeTest.kt` adds
`identityServiceWithPlannerRegistered()` (an `InMemoryIdentityService`
with `system.planner-runtime` pre-registered, used by every test that
expects `plan` to proceed) and three new tests: resolution succeeds and
`plan` proceeds when the identity is registered; an unregistered publisher
identity produces a `Failed` result with no session record and no events
published; and every published event's `publisherPrincipalId` equals the
resolved identity. No EventBus change, no Planner Runtime redesign, and no
module access were introduced by this fix.

### 50. EventBus publish is synchronous; a slow subscriber can block all publishers

**Status: Open, logged for tracking. Deferred scale/design issue --
correct and low-risk at current scale, not an implementation bug.**

`InMemoryEventBus.publish()` iterates subscribers and calls
`subscription.deliver(event)` sequentially, inside the publisher's own
coroutine (`src/runtime/InMemoryEventBus.kt:88-112`). A throwing subscriber
is caught and isolated; a slow or blocking one is not, and every runtime
subsystem shares one `EventBus` instance. No current subscriber performs
blocking work, so this has no observable effect today.

**Requires an ADR before it requires a fix; no action needed now.**
**Recommended follow-up:** an ADR ("EventBus Delivery Isolation") deciding
concurrent delivery, per-subscriber timeouts, or an explicit
fire-and-forget dispatch boundary, authored alongside -- not before -- the
first future unit that adds a subscriber performing real I/O or
non-trivial work (the most likely candidate being a future Audit
subsystem).

**Update (Pre-Module Readiness Unit 3):** `docs/adr/ADR-024-module-event-audit-durability-boundary.md`
Section C now settles this ADR: EventBus remains synchronous for today's
fast, in-process subscriber set; the target future semantic is
per-subscriber isolated dispatch (concurrent and/or timeout-bounded), not
a durable queue; and delivery isolation is a precondition for any future
Audit subscriber or module subscriber, not a concurrent task alongside
adding one. **This is a design decision, not an implementation.**
`InMemoryEventBus` is unmodified; this gap's status remains **open,
pending implementation**, now with a settled target shape.

### 51. Persistence / durability / audit boundary is not structurally defined

**Status: Open, logged for tracking. Strategic architecture gap -- not a
defect in the current in-memory reference implementation.**

`MemoryStore.md`'s Purpose statement calls Memory "Parker's durable
long-term knowledge," but `InMemoryMemoryStore` -- like every other
runtime store, including `InMemoryIdentityService`'s Principals -- loses
all state on process restart. Separately, no `AuditService` implementation
exists, and `InMemoryEventBus` is explicitly at-most-once with no replay
(item #26), so the Constitution's Auditability principle currently has no
durable mechanism behind it anywhere in the platform.

**No action required to close now; requires an ADR before persistence or
durable audit becomes load-bearing.** **Recommended follow-up:** an ADR
defining the persistence/durability boundary across Memory, Identity, and
Audit, including whether `MemoryStore.md`'s current "durable" language
should be scoped ("logically durable within process lifetime; physical
durability is a reserved seam") until that ADR lands. See
`docs/reviews/ARCHITECTURE_V2_INDEPENDENT_AUDIT_TRIAGE.md` Finding 4 for
the full reasoning.

**Update (Pre-Module Readiness Unit 3):** `docs/adr/ADR-024-module-event-audit-durability-boundary.md`
Section D now settles this ADR: Memory Records, Principal records, and an
Audit log must eventually be durable; World Model beliefs and ordinary
per-request working state may remain in-memory; Memory may not be treated
as durable across a restart until a real persistence layer exists and is
verified (`MemoryStore.md`'s own language is not amended by this ADR, only
interpreted, pending a future documentation-only pass); and no document
may claim AD-009's Auditability guarantee is satisfied in a durable,
reconstructable sense until a real Audit mechanism exists and receives
events from every subsystem the claim covers. **This is a design
decision, not an implementation.** No persistence or audit storage was
implemented; this gap's status remains **open, pending implementation**,
now with a settled boundary.

**Note on Agent/Agent Runtime naming.** The independent audit's sixth
finding (ambiguity between the Background Agent interface and Agent
Runtime) is **not** added as a gap here. Per
`ARCHITECTURE_V2_INDEPENDENT_AUDIT_TRIAGE.md` Finding 6, this is resolved
at the documentation layer (Sprint 5 Cleanup) and the remaining exposure is
a low-risk, structurally-unenforced residual, not an open gap, while
`Agent.kt` stays excluded from build scope.

No item in this section was closed by inventing behaviour beyond what its
governing architecture document already specifies; all four are logged as
open tracking entries only.

---

## Module Registry Runtime (Sprint 6, Track A, Unit M1) -- findings

### 52. Module Registry's Tool/Resource wiring rests on interpretive choices Contract Design left unspecified, and defers live Permission Engine gating of its own lifecycle operations

**Status: Open, disclosed implementation-level limitations. Surfaced (not
created) by Sprint 6, Track A, Unit M1** -- the first unit to implement
`ModuleRegistry`. None of these is a defect in Unit M1's own
implementation: each is a genuine interpretive gap `docs/architecture/MODULE_CONTRACT_DESIGN.md`
left open, or a scope reduction that document explicitly authorised an
implementation unit to make, mirroring this file's own #24 for Tool
Registry.

**Permission Engine gating of `enable`/`disable`/`remove` is deferred, not
wired in -- mirrors gap #24 exactly.** Contract Design's own Section 5
states each of these operations is "architecturally, a
`PermissionAction.CONTROL`-equivalent decision requiring evaluation," but
explicitly leaves whether a first implementation wires that evaluation in
or defers it as "an implementation-unit decision this document does not
make." `InMemoryModuleRegistry.enable`/`disable`/`remove` accept a
`requestingPrincipalId: PrincipalId` parameter but do not evaluate it
against `IdentityService` or `PermissionEngine` -- any syntactically valid
`PrincipalId`, registered or not, is accepted as-is, identical to
`InMemoryToolRegistry.register`/`.setLifecycleState`'s own, already-open
gap #24. **Recommended closure (a future unit's decision, not this one's):**
wire a live `PermissionEngine.evaluate`-equivalent check once a concrete
policy for administrative/`CONTROL`-class actions exists, for both Tool
Registry and Module Registry together, rather than inventing two separate
mechanisms.

**The module itself is not registered as an `IdentityService` Principal.**
`docs/adr/ADR-024-module-event-audit-durability-boundary.md` Section A,
Rule 3 states "every module is an ordinary Principal," but neither
`MODULE_FRAMEWORK_ARCHITECTURE.md` nor `MODULE_CONTRACT_DESIGN.md` assigns
`ModuleRegistry.register` (or any other operation this Unit implements)
the responsibility of calling `IdentityService.register` for the module
being registered -- that step belongs to whatever future Discovery
mechanism produces a validated `ModuleDescriptor` in the first place
(explicitly out of this Unit's scope: "Do not implement discovery").
`InMemoryModuleRegistry` interprets `ModuleId(moduleId.value)` as a
`PrincipalId` only for the narrow purpose of naming a `Resource`'s nominal
owner (see below) -- this is not, and is not intended to be, a claim that
the module is a verified, resolvable Principal anywhere else in the
platform. **Recommended closure (a future unit's decision, not this
one's):** decide, when Discovery is designed, whether Discovery,
Description, or Registration is the step that calls
`IdentityService.register` for a module, and reconcile the `PrincipalId`
derivation below with whatever identity that call actually produces.

**Interpretive choices in `ModuleRegistry`'s Tool Registry/Resource
Registry wiring, none authorised or forbidden explicitly by Contract
Design:**

- Each Tool a module exposes needs a backing `Resource`
  (`tool-registry.md`'s own registration invariant, gap #22's precedent
  for exactly this kind of prerequisite-filling). `InMemoryModuleRegistry`
  mints a deterministic `ResourceId("module-tool-<moduleId>-<toolId>")`
  and sets `ownerPrincipalId = PrincipalId(moduleId.value)` -- treating the
  module as the nominal owner of its own exposed Tools' Resources, without
  verifying that identity against `IdentityService` (`InMemoryResourceRegistry.register`
  performs no such check today, so this is safe to construct but is an
  interpretive choice, not a specified one).
- Every such Resource defaults to `ResourceSensitivity.PUBLIC`, since
  neither `ModuleDescriptor` nor `ToolDescriptor` carries a sensitivity
  classification field. A module exposing a genuinely sensitive
  capability would have its backing Resource under-classified until this
  is revisited.
- Module registration is **not atomic** across multiple declared Tools: if
  the Nth Tool fails to register with `ToolRegistry` (a genuine
  `toolId`+`version` conflict with something already registered), the
  first `N-1` Tools remain registered there -- and in `ResourceRegistry`
  -- with no corresponding Module Registry entry, since `ToolLifecycleTransitions`
  has no legal `REGISTERED -> REMOVED` edge to undo a freshly-registered,
  never-enabled Tool. `tests/runtime/InMemoryModuleRegistryTest.kt`'s "a
  failed multi-tool registration is not atomic..." test demonstrates and
  asserts this honestly rather than hiding it.
- `ToolRegistry` exposes no operation to read a specific `toolId`+`version`'s
  current `ToolLifecycleState` from outside. `InMemoryModuleRegistry`
  tracks each of its own exposed Tools' state locally, mirroring every
  transition it itself drives via `setLifecycleState`. If a different
  module later registers a new version of the same `toolId` (a
  cross-module version collision, causing Tool Registry's own supersession
  behaviour to move the earlier version to `DEPRECATED`), this module's
  locally-tracked state becomes stale relative to `ToolRegistry`'s real
  state, and a subsequent `enable`/`disable`/`remove` call on this module
  could throw an unexpected `IllegalArgumentException` from
  `ToolLifecycleTransitions.requireValidTransition`. Not solved by this
  Unit -- doing so would require either a new read operation on
  `ToolRegistry` (a Volume 3 interface change, out of this Unit's scope)
  or a cross-module reconciliation mechanism neither Contract Design nor
  this Unit's own instructions authorise.

**Recommended closure for the wiring choices above (a future unit's
decision, not this one's):** if and when module-exposed Tool sensitivity,
cross-module Tool version collisions, or atomic multi-Tool registration
become real, observed problems (not before, per this repository's
established "100,000-line test" discipline), address them together in a
small, additive Contract Design revision rather than three separate,
uncoordinated fixes.

No item in this section was closed by inventing behaviour beyond what its
governing architecture document already specifies. This is a tracking
addition only -- no Kotlin behaviour was changed as a result of writing
this entry.

---

## Communication Runtime (Sprint 7, Unit C1) -- findings

### 53. Response Delivery and Cognition's consumption of accepted inbound messages remain unimplemented

**Status: Open, not yet closed. Deliberate scope boundary, matching
`docs/architecture/COMMUNICATION_CONTRACT_DESIGN.md`'s own Conclusion --
not a defect, not a silent omission.**

Unit C1 implemented `CommunicationIntake`/`InMemoryCommunicationIntake`
exactly as Contract Design authorises for a first Communication Runtime
unit: the two structural checks (channel `ENABLED`, sender resolves) and
nothing more. This Unit's own task brief separately asked for a broader
Communication Runtime that constructs an `ExecutionRequest`, submits it
through `ExecutionPipeline`, awaits completion, and returns a response --
exactly the two things Contract Design's own Section 14 names as genuine,
disclosed open items a first unit must **not** resolve: (1) the mechanism
by which Cognition consumes an accepted `InboundOwnerMessage` (no
Cognition/Conversation Engine contract exists anywhere in this repository
yet), and (2) `ExecutionRequest`'s lack of a dedicated payload/content
field, which leaves a response's actual text with nowhere to travel except
the non-authoritative `metadata` map.

This conflict was surfaced before any code was written and resolved by an
explicit human decision (recorded in this Unit's own Implementation
History entry) to build only what Contract Design currently authorises,
rather than silently picking an interpretation either way. Consequently:

- No `ExecutionRequest` is constructed anywhere in `CommunicationIntake.kt`
  or `InMemoryCommunicationIntake.kt`, and no `ExecutionPipeline`,
  `PermissionEngine`, or `ToolRegistry` call is made by either.
- `OutboundParkerResponse` (the contract Response Delivery would need) is
  defined, field-shaped, per Contract Design Section 3 -- but nothing in
  this Unit constructs one, delivers one, or wires it through a channel's
  exposed "deliver" Tool (Contract Design Section 7).
- No Cognition, Conversation Engine, `PlannerRuntime`, or `AgentRuntime`
  call is made from an accepted `InboundOwnerMessage`. `acceptedMessages()`/
  `acceptedMessageFor` exist only as inspection methods outside the formal
  interface (mirroring `InMemoryMemoryStore.wasForgotten`'s precedent), not
  a queue-consumption API for a real consumer.

**Recommended closure (a future unit's decision, not this one's):** a
separately-scoped Contract Design/implementation pass, once Cognition or a
Conversation-Engine-shaped intermediary is itself scoped, that either (a)
resolves `ExecutionRequest`'s content-carrying gap (a Volume 1 core
contract revision, outside this Unit's authority) and implements Response
Delivery against it, or (b) defines the concrete mechanism by which
Cognition consumes an accepted `InboundOwnerMessage` from
`CommunicationIntake` -- per Contract Design Section 14, neither is
required before `CommunicationIntake` itself can be used as built here.

**Clarification (planning pass, no Kotlin or architecture changed): of
the two closure paths above, (a) and (b) are not equally ready.** (a)
requires a Volume 1 core contract revision to `ExecutionRequest` --
likely a Level 3 Architectural Change under PES-001 (Architecture
Decision required, wide blast radius across `ExecutionPipeline`,
`AgentRunCommand`, `PlanDecision`, and every existing caller of
`ExecutionRequest`). (b) requires Cognition/Conversation Engine to
first receive real Stage 1 Architecture -- `docs/architecture/19-conversation-engine.md`
is currently a three-line stub with no responsibilities, ownership,
trust boundary, lifecycle, invariants, or security model, i.e. none of
Stage 1's required content exists yet. Neither (a) nor (b) is close to
implementable. A third, smaller, more immediately ready path -- a
Contract Design pass for the Local Text Channel itself
(`docs/architecture/COMMUNICATION_CHANNEL_ARCHITECTURE.md` Section 6),
scoped to its already-unblocked inbound half only (registering the
channel as a Module and feeding `CommunicationIntake.submitInboundMessage`,
both already implemented) -- is recommended as the next Stage 2A
document to write, ahead of either (a) or (b). This is a recommendation
only, not a decision: it does not close this gap, and neither
`COMMUNICATION_CHANNEL_ARCHITECTURE.md` nor `COMMUNICATION_CONTRACT_DESIGN.md`
was modified to record it.
