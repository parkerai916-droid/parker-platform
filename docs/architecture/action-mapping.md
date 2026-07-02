# Action Mapping Architecture

## Status

Version: 0.7-alpha
Status: New architecture specification (Priority 2, v0.7 Architecture
Completion Phase). Closes the blocker recorded in
`docs/reviews/PARKER_V0_6_CONSISTENCY_REVIEW.md` §2.3 and the
reconciliation report §8 item 5: nothing defined how
`ExecutionRequest.proposedActions` (free text) becomes
`PermissionDecision.action` (a closed enum) for the Permission Engine to
evaluate. This document is part of Parker's Trust Architecture (Chapter 9)
and is specification only — no Kotlin is implemented here.

## Purpose

`ExecutionRequest.proposedActions` is `List<String>` — deliberately open
text, because intent can be expressed by voice, text, a Plugin, a
Scheduled Task, or an Agent, and none of those origins can be expected to
speak in `PermissionAction`'s closed vocabulary directly. `PermissionEngine.evaluate`
however must produce a `PermissionDecision` whose `action` field is one of
exactly ten enum values. Something has to sit between the two. This
document specifies what that something is, so that `PermissionEngine`
implementations do not each invent their own translation.

## The Complete Process

```text
ExecutionRequest
        ↓
Intent
        ↓
Planner
        ↓
Proposed Actions
        ↓
Permission Actions
        ↓
Permission Engine
        ↓
Decision
```

Mapped onto the contracts and interfaces that already exist:

1. **ExecutionRequest** — the canonical, immutable-after-validation
   request object (ADR-017, ADR-018). Carries `intent: String` and
   `proposedActions: List<String>` as already specified in Volume 1.
2. **Intent** — not a separate type; `ExecutionRequest.intent` *is* the
   intent, expressed as free text by whichever origin created the
   request (voice transcript, typed command, Agent-internal goal, Plugin
   trigger). No change proposed here.
3. **Planner** — Chapter 20 (Planning and Deliberation Framework) is the
   existing architectural home for turning an intent into concrete,
   orderable steps. This document specifies that **the Planner is the
   sole owner of the translation described below** — the Permission
   Engine never interprets free text itself (see "Why the Permission
   Engine must never parse intent," below).
4. **Proposed Actions** — `ExecutionRequest.proposedActions`, the
   free-text list the Planner produced. This is the boundary object
   already defined in Volume 1; unchanged by this document.
5. **Permission Actions** — the new concept this document defines: the
   deterministic mapping from each proposed-action string to one or more
   `PermissionAction` enum values (see "Transformation Rules").
6. **Permission Engine** — `PermissionEngine.evaluate(request):
   PermissionDecision`, already specified in Volume 3. It receives
   Permission Actions (not proposed-action strings) as its actual input
   to the decision logic.
7. **Decision** — `PermissionDecision`, already specified in Volume 1/3.

## Transformation Rules

The mapping from a proposed-action string to `PermissionAction` value(s)
is performed by a **Planner-owned lookup table**, not by pattern-matching
or free-form NLP inside the Permission Engine. This is a deliberate
architectural boundary:

- Every proposed-action string that a Planner emits **MUST** be drawn
  from (or immediately translatable to) a registered **action vocabulary
  entry**. An action vocabulary entry is a `(verbPhrase, PermissionAction,
  applicableResourceTypes)` triple — e.g. `("send email", SEND_EXTERNAL,
  {EMAIL})`, `("read calendar", READ, {CALENDAR})`, `("delete document",
  DELETE, {DOCUMENT})`.
- The action vocabulary is versioned and extensible (see "Plugin Supplied
  Actions" and "Future Extensibility"), but **closed at evaluation time**
  — the Permission Engine only ever receives `PermissionAction` values
  that came from a vocabulary lookup, never a raw string it must
  interpret itself.
- The Planner performs the lookup **before** the `ExecutionRequest` is
  considered `VALIDATED` in the execution lifecycle
  (`docs/diagrams/execution-state-machine.mmd`: `Created → Validated →
  PermissionPending`). By the time a request reaches `PermissionPending`,
  every proposed action has already been resolved to Permission Action(s)
  — resolution is not deferred into the Permission Engine itself.
- The resolved Permission Action(s) are **not** added as a new field to
  `ExecutionRequest` (which would touch Volume 1's schema and interact
  with ADR-018's immutability guarantee mid-flow). Instead, the mapping is
  a pure, stateless function of `proposedActions` +
  `targetResources`'/`ResourceType`(s), re-derivable at any point from the
  same immutable inputs. `PermissionEngine.evaluate` performs this lookup
  as its first internal step, using the same vocabulary table the Planner
  used — guaranteeing the Planner and the Permission Engine always agree,
  since they consult the same source of truth rather than passing a
  precomputed value hand-to-hand.

### Why the Permission Engine must never parse intent

If the Permission Engine were to interpret `proposedActions` strings
itself (e.g. via keyword matching or an embedded model), two Trust
Framework invariants would break: Chapter 9's "trust is woven through, not
wrapped around" (interpretation is a cognitive act, and ADR-001 already
forbids cognitive components — models — from being anywhere near
execution authority), and Chapter 10's requirement that Permission
evaluation be deterministic and auditable (`PermissionDecision`s must be
explainable via `PermissionEngine.explain`, which is impossible to do
reliably over a non-deterministic interpretation step). The vocabulary
table keeps the entire mapping deterministic, inspectable, and equally
available to both the Planner and the Permission Engine.

## Validation

- A `proposedActions` entry that does not resolve to any vocabulary entry
  is a **validation failure**, not a Permission Engine concern. The
  `ExecutionRequest` fails at the `Created → Validated` transition (see
  "Unknown Actions" below) — it never reaches `PermissionPending` in an
  unresolvable state.
- A vocabulary entry's `applicableResourceTypes` **MUST** be checked
  against the actual `ResourceType`(s) of `ExecutionRequest.targetResources`
  at validation time. A mapped action whose declared resource types don't
  match any target Resource is also a validation failure (e.g. a
  `("read calendar", READ, {CALENDAR})` entry proposed against a
  `targetResources` list containing only `DOCUMENT` resources).

## Unknown Actions

An unresolvable proposed action is treated as **invalid, not denied** —
these are different outcomes with different meanings in this
architecture:

- **Invalid** (this case): the request itself is malformed or refers to a
  capability Parker has no concept of. The `ExecutionRequest` never
  completes validation; no `PermissionDecision` is ever produced, because
  there is nothing well-formed to evaluate. Surfaces as
  `ExecutionResultStatus.FAILED` (or a validation-stage rejection prior to
  a result existing at all, mirroring how `ValidationResult.Invalid`
  already works for `Tool.validate`).
- **Denied** (a different case, already specified): the action resolved
  correctly to a `PermissionAction`, but the Permission Engine determined
  the Principal may not perform it. This is `PermissionDecisionOutcome.DENIED`,
  already specified in Volume 1/3 — unaffected by this document.

Conflating the two would let a well-formed but forbidden action look
identical to a nonsensical one in audit logs, which would weaken
explainability (Chapter 43). This document keeps them distinct.

## Multiple Actions

An `ExecutionRequest` may legitimately propose more than one action (e.g.
"read the calendar and send a summary email" → `READ` + `SEND_EXTERNAL`).
v0.7 specifies:

- Each resolved Permission Action is evaluated as its own
  `PermissionEngine.evaluate` call, producing its own `PermissionDecision`
  — there is no merged "batch decision" type. `ExecutionRequest` remains
  singular per Volume 1, but may yield multiple `PermissionDecision`
  records, each independently auditable.
- The overall request may only proceed to `Approved` in the execution
  lifecycle if **every** resolved Permission Action's decision is
  `APPROVED` or `APPROVED_WITH_CONFIRMATION`. A single `DENIED` among
  several proposed actions denies the whole request — v0.7 does not
  specify partial execution of only the approved subset, since that would
  let a Planner-composed multi-action request silently execute less than
  what its `intent` described, without the user ever having proposed the
  narrower version. Partial execution of a deliberately-scoped subset is
  left as a future extensibility item (see below), not invented here.
- `PermissionDecisionOutcome.DEFERRED` on any one action defers the whole
  request, same reasoning.

## Composite Actions

Some proposed actions are inherently compound (e.g. "move document to
archive" implies both `READ` the source and `DELETE` or `WRITE` the
original location, plus `WRITE` the destination). v0.7 specifies:

- A vocabulary entry **MAY** map one proposed-action string to **more
  than one** `PermissionAction`, each potentially against a different
  target Resource. This is expressed as a set of
  `(PermissionAction, ResourceType)` pairs per vocabulary entry, rather
  than requiring the Planner to decompose composite actions into multiple
  separate proposed-action strings itself.
- Composite actions still follow the "Multiple Actions" rule above: every
  constituent `PermissionAction` must independently resolve to
  `APPROVED`/`APPROVED_WITH_CONFIRMATION` for the composite action to
  proceed.
- This document does not attempt to enumerate the composite vocabulary —
  that is implementation-phase content (a data table, not an
  architectural decision) once this mapping model is approved.

## Plugin Supplied Actions

Plugins (Chapter 15) may introduce new capabilities that don't correspond
to any core vocabulary entry (e.g. a smart-home Plugin's "run irrigation
cycle"). v0.7 specifies:

- A Plugin **MAY** register new action vocabulary entries as part of its
  Tool registration (see `docs/architecture/tool-registry.md`, "Plugin
  Integration"), scoped to its own declared `supportedActions`/
  `supportedResourceTypes` (the same `ToolDescriptor` extension proposed
  in that document).
- A Plugin **MUST NOT** register a vocabulary entry that maps to a
  `PermissionAction` outside what its own Principal could ever be granted
  — the vocabulary table does not grant permission, it only makes an
  action nameable and evaluable. This preserves "installation does not
  imply permission" (Chapter 15) at the vocabulary layer too.
- Plugin-supplied vocabulary entries are namespaced by the Plugin's
  identity (e.g. `plugin:<pluginId>:run irrigation cycle`) to prevent
  collision with core or other plugins' vocabulary, and to make audit
  trails immediately show which Plugin introduced a given mapping.

## Future Extensibility

Recorded as open, not resolved by this document:

- Partial execution of a multi-action request when only some actions are
  approved (currently: any denial or deferral denies/defers the whole
  request).
- A tie-breaking or priority model when a proposed action could map to
  more than one vocabulary entry (ambiguous mapping) — mirrors the
  `TOOL_AMBIGUOUS` open question in `tool-registry.md`, and is likely the
  same underlying decision.
- Whether the action vocabulary itself should become a first-class,
  versioned schema artifact under `docs/schemas/` (analogous to
  `ExecutionRequest.schema.json`) rather than living purely as
  Planner/Permission-Engine-internal data. Recommended for a future ADR
  once an initial vocabulary table is actually drafted during the
  implementation phase.
- Natural-language-to-vocabulary matching itself (how the Planner decides
  "send the Joneses an email" maps to the `("send email", SEND_EXTERNAL,
  {EMAIL})` entry) is Planning/Deliberation Framework (Chapter 20)
  territory, not Trust Architecture — this document only specifies what
  happens once that matching has produced a proposed-action string, not
  how the match itself is made.

## Related

- Chapter 9 – Trust Framework
- Chapter 10 – Permission Engine
- Chapter 20 – Planning and Deliberation Framework
- ADR-001 – Models Never Execute Tools
- ADR-017 – ExecutionRequest Is Canonical
- ADR-018 – ExecutionRequests Are Immutable After Validation
- `docs/specifications/volume-01-core-contracts/ExecutionRequest.md`
- `docs/specifications/volume-01-core-contracts/Permission.md`
- `docs/specifications/volume-03-core-interfaces/PermissionEngine.md`
- `docs/architecture/tool-registry.md`
- `docs/reviews/PARKER_V0_6_CONSISTENCY_REVIEW.md` §2.3
