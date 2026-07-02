# Parker Platform — v0.7 Architecture Completion Report

Produced on `feature/v0.7-architecture-completion` (branched from
`feature/phase-1-core-contracts` at commit `c131594`, the v0.6
consistency review). **No Kotlin was written or changed to produce this
phase's work** — every change in this branch is a specification, schema,
or documentation change, per this phase's explicit "do not write Kotlin
implementation" rule.

## 1. Executive Summary

The v0.6 consistency review (`PARKER_V0_6_CONSISTENCY_REVIEW.md`)
concluded Phase 1 was solid but found three blockers that made Phase 2
unsafe to start: no `ToolRegistry` specification anywhere, `EventBus`'s
supporting types entirely unspecified, and no defined mapping from
`ExecutionRequest.proposedActions` to `PermissionDecision.action`. This
phase treats those findings as validation of the architecture-first
approach, not implementation failures, and closes all three at the
specification level:

- **Tool Registry** now has a complete architecture
  (`docs/architecture/tool-registry.md`), including a proposed
  `ToolDescriptor` extension (`supportedActions`/`supportedResourceTypes`)
  that makes tool dispatch deterministic without touching
  `ExecutionRequest`'s schema.
- **Action Mapping** now has a complete architecture
  (`docs/architecture/action-mapping.md`), introducing a Planner-owned
  action vocabulary table as the deterministic bridge between free-text
  proposed actions and the closed `PermissionAction` enum, and folding
  this into Parker's Trust Architecture.
- **EventBus** is now fully specified: `EventType`, `EventHandler`,
  `Subscription`, and `PublishResult` each have their own Volume 3
  supporting-type document, and `EventBus.md` itself was rewritten with
  lifecycle, authentication, authorisation, ordering, delivery-guarantee,
  failure-handling, cancellation, and security sections.

Beyond the three headline blockers, this phase also produced the
`IdentityService.md` architecture proposal (Priority 4 — the interface
remains unimplemented, per ADR-022, but is no longer un-specified),
lifecycle diagrams for Session, Task, and Workflow (Priority 5), and a
specification consistency pass (Priority 6) that resolved eight of the
ten non-blocking items the v0.6 review found, labelled (without deleting)
the still-disputed `Permission.schema.json`/`PermissionDecision.schema.json`
duplication, and surfaced two genuinely new findings that this phase
deliberately did not resolve (see §5).

**Bottom line: the architecture is now stable enough to resume sustained
Kotlin implementation of the Phase 2 systems** (Execution Pipeline, Tool
Registry, Permission Engine action evaluation, Event Bus), **provided the
remaining human decisions in §5 are either resolved first or explicitly
accepted as open while implementation proceeds around them.** None of
those remaining items block the shape of the Kotlin work itself — they
are judgment calls about file disposition and cascading behaviour, not
missing design.

## 2. Architectural Gaps Closed

| Gap | Source | Resolution |
|---|---|---|
| No `ToolRegistry` interface anywhere | Consistency review §2.1 | `docs/architecture/tool-registry.md` — full spec + proposed `ToolDescriptor` extension |
| No `ExecutionRequest.proposedActions` -> `PermissionDecision.action` mapping | Consistency review §2.3 | `docs/architecture/action-mapping.md` — Planner-owned vocabulary table, part of Trust Architecture |
| `EventBus` supporting types unspecified | Consistency review §2.2 | `EventType.md`, `EventHandler.md`, `Subscription.md`, `PublishResult.md`; `EventBus.md` rewritten |
| `IdentityService` has no interface | IMPLEMENTATION_GAPS.md #1 | `docs/architecture/IdentityService.md` — proposed interface, not implemented |
| Session/Task/Workflow lifecycles undiagrammed | Consistency review §3.10 | Three new `.mmd` diagrams + Volume 2 doc sections |
| `ADR-004` dangling citation in `Agent.md` | Consistency review §3.1 | Removed, same treatment as the already-fixed `ADR-005` |
| `ExecutionResult.schema.json` missing `toolResults`/`reflectionCandidate` | Consistency review §3.3 | Added to schema + example |
| `Resource.schema.json` missing `createdAt`/`updatedAt`/`source` | Consistency review §3.4 | Added to schema + example (now required) |
| 12 Volume 3 interface docs had no version header | Consistency review §3.6 | All 12 stamped |
| README/CHANGELOG said "v0.4" | Consistency review §3.7 | Both rewritten to current state |
| `RequestOrigin.AGENT` / `PrincipalType.INTERNAL_AGENT` near-miss | Consistency review §3.8 | Clarifying note added to `ExecutionRequest.md`; confirmed not a bug |
| `VOLUME_1_INDEX.md` listed the deprecated `Permission.schema.json` as canonical | New finding, this phase | Corrected to reference `PermissionDecision.schema.json` |

`src/contracts/Resource.kt`'s `sensitivity: String` (consistency review
§3.5) is **not** closed — changing it is a Kotlin edit, out of scope for
an architecture-only phase. It remains a recommended, ready-to-do
follow-up once Kotlin work resumes.

## 3. New Architecture Documents

- `docs/architecture/tool-registry.md`
- `docs/architecture/action-mapping.md`
- `docs/architecture/IdentityService.md`
- `docs/specifications/volume-03-core-interfaces/EventType.md`
- `docs/specifications/volume-03-core-interfaces/EventHandler.md`
- `docs/specifications/volume-03-core-interfaces/Subscription.md`
- `docs/specifications/volume-03-core-interfaces/PublishResult.md`
- `docs/diagrams/session-lifecycle-state-machine.mmd`
- `docs/diagrams/task-lifecycle-state-machine.mmd`
- `docs/diagrams/workflow-lifecycle-state-machine.mmd`
- `docs/reviews/PARKER_V0_7_ARCHITECTURE_COMPLETION_REPORT.md` (this document)

## 4. Updated Specifications

- `docs/specifications/volume-03-core-interfaces/EventBus.md` — rewritten
  (lifecycle, auth, ordering, delivery, failure, cancellation, security).
- `docs/specifications/volume-03-core-interfaces/Agent.md` — `ADR-004`
  citation removed; version header added.
- `docs/specifications/volume-03-core-interfaces/{ExecutionPipeline,PermissionEngine,ResourceRegistry,Tool,Plugin,MemoryStore,WorldModel,ModelManager,NotificationService,AuditService}.md` —
  version headers added (content otherwise unchanged).
- `docs/specifications/volume-03-core-interfaces/VOLUME_3_INDEX.md` —
  version bumped; Supporting Types section extended with the four new
  EventBus types.
- `docs/specifications/volume-01-core-contracts/ExecutionRequest.md` —
  `RequestOrigin`/`PrincipalType` terminology note added.
- `docs/specifications/volume-01-core-contracts/VOLUME_1_INDEX.md` —
  "Included Schemas" corrected; revision history entry added.
- `docs/specifications/volume-02-core-schemas/{Resource-Schema,ExecutionResult-Schema}.md` —
  updated to reflect the schema fixes below, plus the new `resourceType`
  finding.
- `docs/specifications/volume-02-core-schemas/{Session-Schema,Task-Schema,Workflow-Schema}.md` —
  Lifecycle sections appended (states, valid/invalid transitions, failure
  states, archived states).
- `docs/schemas/ExecutionResult.schema.json` (+ example) — added
  `toolResults`, `reflectionCandidate`.
- `docs/schemas/Resource.schema.json` (+ example) — added `createdAt`,
  `updatedAt`, `source` (now required).
- `docs/schemas/Permission.schema.json` — added a `$comment` marking it
  deprecated in favour of `PermissionDecision.schema.json`; **not
  deleted**.
- `README.md`, `CHANGELOG.md` — brought current (v0.6/v0.7 state,
  chapter/ADR/volume counts, repository layout).
- `docs/architecture/IMPLEMENTATION_GAPS.md` — items 1, 3, 5 updated;
  items 11–20 added.

## 5. Remaining Human Decisions

Carried over, unresolved by design:

1. **`Permission.schema.json` vs `PermissionDecision.schema.json`.**
   Now clearly labelled (both in the file itself and in the
   specification), but whether to delete, merge, or repurpose
   `Permission.schema.json` is still an open call — deletion of a schema
   file is a decision this phase declined to make unilaterally.
2. **`Resource.schema.json`'s `resourceType` enum has 18 values; prose and
   Kotlin agree on 14.** New finding from this phase. Unlike the
   Principal precedent, it's plausible the schema is ahead of prose/Kotlin
   (Session/Event/Task/Workflow plausibly belong in the Resource
   Registry's catalogue) rather than simply wrong — needs a decision on
   which list is correct, not a mechanical fix.
3. **Cascading revocation rule for owned Principals**
   (`IdentityService.md`) — immediate revoke vs. suspend-pending-review
   for Principals owned by a just-Revoked Principal.
4. **Tie-breaking rule for ambiguous Tool Registry lookups**
   (`tool-registry.md`) and the parallel ambiguous-vocabulary-mapping case
   (`action-mapping.md`) — likely the same underlying decision, not yet
   made.
5. **Whether a dedicated `ExecutionResultStatus` value is warranted** for
   Tool Registry resolution failures, instead of overloading `FAILED`
   with error codes.
6. **Whether an ADR should be authored** for event authentication/
   authorisation rules (long-standing, restated) and for Agent-specific
   accountability rules (the `ADR-004` gap, restated).
7. **`src/contracts/Resource.kt`'s `sensitivity: String`** should become
   the 9-value enum already in the schema — a contained Kotlin change,
   deliberately not made in this Kotlin-free phase.
8. **`AgentHealth`** (`Agent.md` / `src/interfaces/Agent.kt`) is
   referenced but nowhere defined — newly found this phase, explicitly
   left unresolved since Agent Framework work is outside this phase's
   five priorities.

## 6. Phase 2 Readiness Assessment

**Ready, with the caveats in §5 noted as open rather than blocking.**

The three items that made the v0.6 review say "no-go" — missing
`ToolRegistry`, unspecified `EventBus` types, and no action-mapping rule —
are now each backed by a specification detailed enough to implement
against without inventing behaviour mid-code. The remaining open items in
§5 are judgment calls (which file survives, which enum list is correct,
what the cascading/tie-break policy is) that can each be resolved
independently of, and in parallel with, Kotlin work starting — none of
them changes the shape of `ExecutionPipeline`, `ToolRegistry`,
`PermissionEngine`, or `EventBus` as now specified.

## 7. Recommendation to Resume Kotlin

**Resume Kotlin implementation**, scoped to the Phase 2 systems this
architecture work now supports: `ToolRegistry` (per
`tool-registry.md`), the action-mapping vocabulary mechanism inside
`PermissionEngine.evaluate` (per `action-mapping.md`), and `EventBus`
with its four newly-specified supporting types. Recommended order,
matching dependency direction:

1. `ToolRegistry` (depends only on already-implemented `Resource`/
   `Principal`/`ToolDescriptor` — extend `ToolDescriptor` per
   `tool-registry.md` first, a small, well-specified Kotlin change).
2. Action-mapping vocabulary table + wiring into `PermissionEngine.evaluate`.
3. `EventBus` + its four supporting types + a Kotlin `ParkerEvent` mapping
   from `Event.schema.json` (not yet implemented at all).

`IdentityService` remains explicitly deferred (per ADR-022 and this
phase's own scope) — its architecture is now ready, but implementing it
is a separate, explicitly-declared decision, not an automatic
consequence of this report.

Do not begin any of the above until this report has been reviewed and
Kotlin resumption is explicitly approved, per this phase's Final
Instruction.
