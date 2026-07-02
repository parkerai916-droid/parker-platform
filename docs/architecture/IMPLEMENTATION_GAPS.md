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
