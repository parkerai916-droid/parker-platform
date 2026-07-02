# Parker Platform — Implementation Gaps

Recorded while implementing Phase 1 (Volume 1 Core Contracts) on
`feature/phase-1-core-contracts`. Updated during the Phase 1 follow-up
specification cleanup pass — each original gap is now marked **Resolved**,
**Deferred**, or **Requires human decision**. Three new findings surfaced
while doing that cleanup are appended at the end.

## 1. `IdentityService` has no interface

**Status: Architecture proposed (v0.7); implementation still deferred.**

`IdentityService` was deferred from Phase 1 implementation. The v0.7
Architecture Completion Phase delivered the "reviewable architecture
proposal" promised above: `docs/architecture/IdentityService.md` now
specifies responsibilities, a proposed interface shape, lifecycle
ownership, principal resolution, trust/delegation relationships (via the
existing `Principal.owner` field), an authentication-flow integration
diagram, and integration points with the Permission Engine, World Model,
and Audit. **No Kotlin was added** — the proposed interface is
documentation only, per ADR-022 and this phase's explicit "do not
implement Kotlin" rule. Promoting it to `src/interfaces`/Volume 3 remains
a decision for an explicitly-declared implementation phase.

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

**Status: Partially resolved; validator implementation deferred.**

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

**Status: Resolved (correction).**

Gap #3's original entry said no value set was defined for `Resource.sensitivity`
anywhere. That was based on reading only the prose `Resource.md`, not
`Resource.schema.json` — which does define one (`PUBLIC`, `PERSONAL`,
`HOUSEHOLD`, `FINANCIAL`, `MEDICAL`, `LEGAL`, `SECURITY_SENSITIVE`,
`CREDENTIALS_SECRETS`, `THIRD_PARTY_PERSONAL_DATA`). `Resource-Schema.md`
now documents this enum. **`src/contracts/Resource.kt` still types
`sensitivity` as a plain `String`, not this enum** — not changed in this
pass (documentation-only scope); recommended as a follow-up code fix.


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

**Status: Requires human decision — not addressed by this phase.**

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

**Status: Requires follow-up, not blocking.**

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

**Status: Known scope reduction, documented in `ToolRegistry.kt`'s own KDoc.**

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

**Status: Deliberate scope boundary, not an oversight.**

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

**Status: Documented gap, deliberate stand-ins.**

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

**Status: Documented gap.**

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

**Status: Not a blocker; Kotlin already matches the architecture doc.**

`src/contracts/ToolLifecycle.kt`'s `ToolLifecycleTransitions` was written
directly from `docs/architecture/tool-registry.md`'s inline
`stateDiagram-v2` block, the same one that document's own "Runtime
Lifecycle" section describes. A standalone `.mmd` file (matching the
pattern used for Principal/Resource/Session/Task/Workflow) was not
produced this round; recorded as a documentation-completeness follow-up,
not a behavioural gap -- the Kotlin and the architecture doc's diagram
agree.

### 29. `Resource.sensitivity: String` (item 5/10, restated) still not changed to the enum

**Status: Still open; unrelated to this phase's changes.**

Carried over unchanged. Not touched by Phase 2 work, since it's an
independent, previously-recorded item.
