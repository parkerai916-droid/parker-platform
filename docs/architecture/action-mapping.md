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

**Corrected by the targeted refinement pass** (IMPLEMENTATION_GAPS.md #30
/ IMPLEMENTATION_REFINEMENTS.md #30): an earlier revision of this section
described calling `PermissionEngine.evaluate` once per resolved Permission
Action. That does not match the existing Volume 3 interface
(`suspend fun evaluate(request: ExecutionRequest): PermissionDecision`),
which takes the whole request and returns a single decision, with no
parameter identifying which of several mapped actions is being evaluated.
Rather than change that interface — a larger step than this document's
scope, and not required to keep the underlying guarantee — this section
now describes the model the existing interface actually supports:

- `PermissionEngine.evaluate(request)` is called **exactly once per
  `ExecutionRequest`**, regardless of how many actions the request's
  `proposedActions` map to. There is no batch or per-action call.
- `PermissionDecision.action` records the request's **primary mapped
  action** — the one the action mapping layer (this document's
  Transformation Rules) identifies as the request's principal action.
  Selecting which mapped action is primary when a request has several is
  an action-mapping-layer concern, not a Permission Engine concern; this
  document does not prescribe a selection algorithm (e.g. first-listed,
  highest-risk) beyond noting that whichever action-mapping
  implementation makes this choice must do so deterministically, the
  same way `ToolRegistry`'s ambiguous-match handling requires
  determinism without yet prescribing a tie-break rule.
- Multi-action requests are therefore handled entirely **within the
  action mapping layer**: it resolves every proposed action to its
  `PermissionAction`/`ResourceType` pair(s) (Transformation Rules),
  validates all of them (Validation, Unknown Actions), and identifies the
  primary one to pass forward. `PermissionEngine`'s single decision then
  governs the request as a whole — there is no per-action approval/denial
  split to reconcile, because the Permission Engine was never asked to
  evaluate more than one action to begin with.
- The overall request may only proceed to `Approved` in the execution
  lifecycle if the single `PermissionDecision`'s outcome is `APPROVED` or
  `APPROVED_WITH_CONFIRMATION`. `DENIED` or `DEFERRED` denies/defers the
  whole request — unchanged from before, just now correctly described as
  the outcome of one decision rather than a reconciliation across several.

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
- Composite actions still follow the "Multiple Actions" rule above: the
  vocabulary entry's constituent `(PermissionAction, ResourceType)` pairs
  are all resolved by the action mapping layer, one of them is identified
  as primary, and the single `PermissionEngine.evaluate` call governs the
  whole composite action — not a separate decision per constituent pair.
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
