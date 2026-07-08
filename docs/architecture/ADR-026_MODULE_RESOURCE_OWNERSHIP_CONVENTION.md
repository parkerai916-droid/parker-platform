# ADR-026 – Module-Exposed Tool Resource Ownership Convention

## Status

Accepted.

## Context

`docs/architecture/RESPONSE_DELIVERY_CONTRACT_DESIGN.md` Decision 2 locates
a Communication Channel's own backing Resource via
`ResourceRegistry.listByOwner(PrincipalId(response.channelId.value))`,
filtered to `ResourceType.TOOL`. This is the only mechanism that document
identifies for that lookup (its own review of, and rejection of,
alternatives — reconstructing `InMemoryModuleRegistry`'s private
`ResourceId` naming scheme — is unchanged here). That mechanism depends on
one fact remaining true: that a module-exposed Tool's backing `Resource`
has `ownerPrincipalId = PrincipalId(moduleId.value)`.

`docs/architecture/IMPLEMENTATION_GAPS.md` #52 already discloses that this
fact, as implemented by `InMemoryModuleRegistry` (`src/runtime/InMemoryModuleRegistry.kt`,
`moduleToolResource`), is **"an interpretive choice, not a specified
one"** — made by one implementation, not required or forbidden by
`MODULE_CONTRACT_DESIGN.md`, and named by that gap's own text as a
candidate for reconciliation once module Discovery is designed. The
Stage 2A Contract Review of `RESPONSE_DELIVERY_CONTRACT_DESIGN.md`
identified this as a genuine compliance concern — PES-001 requires
resolving open design questions against *approved* architecture, not
building a contract's central mechanism on a disclosed-as-unsettled
implementation choice — and the revised Contract Design elevated it to a
named **Stage 3 Blocking Prerequisite**, to be "formally settled — either
accepted as-is by a short ADR or an explicit gap #52 closure, or replaced
by whichever mechanism that resolution produces instead" before a Stage 3
Implementation Plan may be authorised.

This ADR is that short ADR. It resolves the narrow, load-bearing slice of
gap #52 that Response Delivery actually depends on. It does not resolve
gap #52 in full — three other interpretive choices that gap discloses
(the `ResourceSensitivity.PUBLIC` default; non-atomic multi-Tool
registration; stale locally-tracked `ToolLifecycleState`) are untouched,
unaffected, and not addressed here, matching this program's own
established precedent for scoping an ADR to exactly the slice of a larger
open question a real, present consumer needs (`ADR-023` resolving only
World Model's own publication shape; `ADR-024` resolving only the
module-access-boundary slice of three larger gaps without closing any of
them).

Also relevant: `docs/adr/ADR-024-module-event-audit-durability-boundary.md`
Section A, Rule 3 — **"Every module is an ordinary `Principal`, subject to
the same Identity and Permission evaluation as any other actor"** — already
settles, at the constitutional level, that a module *is* a `Principal`.
What ADR-024 does not settle, and what gap #52 leaves open, is the
narrower, mechanical question this ADR answers: which concrete
`PrincipalId` value represents that module, for the specific, limited
purpose of tagging Resource ownership.

## Decision

**`Resource.ownerPrincipalId = PrincipalId(moduleId.value)` is accepted as
the settled, approved convention for how a module-exposed Tool's backing
`Resource` records its nominal owner.** This is now approved architecture
this platform's implementations may rely on, not merely one
implementation's disclosed interpretive choice.

Stated precisely, to avoid claiming more than is decided:

**Decided:**

1. For any `Resource` created to back a Tool a Module exposes (per
   `MODULE_CONTRACT_DESIGN.md` Section 7 / `InMemoryModuleRegistry`'s
   existing registration flow), `ownerPrincipalId` **must** be
   `PrincipalId(moduleId.value)`. This is now the specified derivation,
   not an implementation-local choice — any future `ModuleRegistry`
   implementation must derive it the same way.
2. `ResourceRegistry.listByOwner(PrincipalId(moduleId.value))` **may** be
   relied upon by any caller — including `ResponseDelivery`
   (`RESPONSE_DELIVERY_CONTRACT_DESIGN.md` Decision 2) — as a stable,
   architecturally-sound way to find the set of Resources a given module
   owns, specifically for its own exposed Tools.
3. `InMemoryModuleRegistry`'s existing behaviour already implements this
   convention exactly (`moduleToolResource`, `src/runtime/InMemoryModuleRegistry.kt`
   lines 201-214). This ADR ratifies that existing, already-tested
   behaviour; it requires no Kotlin change.

**Not decided (remains exactly as open as gap #52 already leaves it):**

4. **Whether, or when, a module itself is registered as a verified
   `IdentityService` `Principal`.** Gap #52's own text is explicit that
   deriving `PrincipalId(moduleId.value)` for Resource-ownership tagging
   "is not, and is not intended to be, a claim that the module is a
   verified, resolvable Principal anywhere else in the platform." This ADR
   preserves that separation rather than collapsing it. `IdentityService.register`
   for a module remains unassigned to any specific step (Discovery,
   Description, or Registration) — exactly as gap #52 already leaves it,
   and exactly as ADR-024 Section E already frames as a precondition for
   *other* kinds of module access, not this one.
5. The three other interpretive choices gap #52 discloses (sensitivity
   default, registration atomicity, stale lifecycle tracking) — untouched.

## Reasoning

**Why this is safe to accept as architecture despite a module not being a
verified `Principal` elsewhere.** `PrincipalId` is used here purely as a
structural key for Resource-ownership bookkeeping.
`InMemoryResourceRegistry.register` performs no verification against
`IdentityService` (confirmed by direct reading — `ResourceRegistry`'s own
four-method interface, `src/interfaces/ResourceRegistry.kt`, has no such
check, and nothing in `Resource`'s own constructor validation,
`src/contracts/Resource.kt`, requires one). Tagging ownership this way
carries no implicit trust grant and authorises nothing — it is inert
bookkeeping data, read only by `ResourceRegistry.listByOwner`, until some
other mechanism (`PermissionEngine.evaluate`) makes an actual trust
decision. `ADR-024` Section A, Rule 3 already establishes, at the
constitutional level, that a module *is* an ordinary `Principal` — this
ADR does not invent that status; it only fixes the concrete value used to
express it for this one, narrow purpose, consistent with a status already
settled.

**Why this does not need to wait for Discovery, or for gap #52's full
closure.** The one real, present consumer of this convention today —
`ResponseDelivery`, Decision 2 — needs a stable, deterministic way to find
a Resource by its owning module, not a verified, authenticated identity
chain. Requiring full Discovery design (which gap #52 itself defers to "a
future unit's decision") before Response Delivery could proceed would
block a small, ready, real unit of work on an unrelated, much larger,
not-yet-scheduled one — the same reasoning this program has already
applied against unvalidated speculative generality elsewhere (the
"100,000-line test," `IMPLEMENTATION_GAPS.md` #22; `PRE_MODULE_ID_MULTIPLICITY_DECISION.md`).

**Why a short ADR, not a full gap #52 closure.** Gap #52 bundles four
separate, independent interpretive choices. Only one — Resource-ownership
derivation — is load-bearing for any real, present consumer. Resolving
only that one, and leaving the other three exactly as open as they already
are, is the smallest architectural change that unblocks Response Delivery,
matching this program's own repeated precedent of scoping an ADR to
exactly the slice of a larger question a real consumer needs, rather than
resolving an entire multi-part gap in one pass.

## Relationship to Gap #52

This ADR does **not** close gap #52. It formally settles the first of its
four disclosed interpretive choices — the `ownerPrincipalId` derivation —
as approved architecture. The remaining three items (`ResourceSensitivity.PUBLIC`
default; non-atomic multi-Tool registration; stale locally-tracked
`ToolLifecycleState`) remain open, unaddressed, and unaffected.
`IMPLEMENTATION_GAPS.md` itself is **not modified by this ADR** — that
file is out of this task's authorised scope. A future documentation pass
may update gap #52's own text to record that this one item is now
settled, distinct from, and not performed by, writing this ADR.

## Consequences

No Kotlin, test, or existing specification change results from this ADR.
`InMemoryModuleRegistry`'s existing behaviour already implements exactly
what this ADR now formally accepts — nothing about it changes.
`RESPONSE_DELIVERY_CONTRACT_DESIGN.md` Decision 2 may now be treated as
depending on approved architecture rather than an unsettled interpretive
choice. **`RESPONSE_DELIVERY_CONTRACT_DESIGN.md`'s Stage 3 Blocking
Prerequisite 1 is resolved by this ADR's acceptance.**

## Future Considerations

- Whether, or when, `IdentityService.register` is ever called for a
  module remains an open Discovery-design question, entirely unaffected
  by this ADR — exactly as gap #52 already leaves it.
- If a future Discovery design reconciles `PrincipalId` derivation
  differently than this ADR's Decision (per gap #52's own "Recommended
  closure... decide, when Discovery is designed... and reconcile the
  `PrincipalId` derivation... with whatever identity that call actually
  produces"), that reconciliation would supersede this ADR's Decision.
  This ADR does not pre-empt that future revisiting — it settles only
  what is needed for Response Delivery's own, present, narrow dependency,
  mirroring `ADR-025`'s own explicitly provisional, revisitable framing
  for a decision scoped the same way.
- The other three items in gap #52 remain available for a future,
  separate ADR or Contract Design revision, if and when a real consumer
  for any of them appears — not before, per this platform's own
  "100,000-line test" discipline.

## Related

- `docs/architecture/IMPLEMENTATION_GAPS.md` (#52)
- `docs/architecture/RESPONSE_DELIVERY_CONTRACT_DESIGN.md` (Decision 2,
  Stage 3 Blocking Prerequisite 1)
- `docs/adr/ADR-024-module-event-audit-durability-boundary.md` (Section A,
  Rule 3)
- `docs/architecture/MODULE_CONTRACT_DESIGN.md`
- `src/runtime/InMemoryModuleRegistry.kt` (`moduleToolResource`)
- `src/interfaces/ResourceRegistry.kt`
