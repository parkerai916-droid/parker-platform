# Parker Platform — Implementation Refinements

## Purpose

A final architecture refinement pass over every gap recorded in
`docs/architecture/IMPLEMENTATION_GAPS.md` (items 1–34, spanning Phase 1
core contracts, the v0.7 Architecture Completion Phase, and Phase 2
runtime implementation). This is **not** a redesign. For each gap, this
document classifies it as one of four kinds and recommends the smallest
correction that closes it — or states plainly that no correction is
needed. No new features, interfaces, or behaviour are introduced here;
where a real fix requires a design decision, that decision is left open
and flagged as such, not made unilaterally.

## Classification Key

- **Doc** — Documentation inconsistency: two documents (or a document and
  the Kotlin/schema it describes) disagree, or a document is stale/
  incomplete.
- **Interface** — Missing interface contract: a type, parameter, or
  document that a referenced interface needs doesn't yet exist.
- **Transition** — Missing state transition: a lifecycle/state model is
  missing an edge or state needed to represent a real outcome.
- **Defect** — Actual architectural defect: a genuine design tension
  between two already-approved pieces of architecture, not fixable by
  documentation alone.
- **N/A** — Not a gap requiring correction (a deliberate choice, an
  already-fully-resolved item, or an out-of-scope boundary correctly
  left alone).

## Summary Table

| # | Gap | Class | Smallest Correction | Status |
|---|---|---|---|---|
| 1 | `IdentityService` has no formal interface | Interface | Promote the already-drafted proposed interface in `IdentityService.md` into Volume 3's "Included Interfaces" list (documentation move only); do not write Kotlin until an implementation phase is declared | Open (doc-only step available now) |
| 2 | `ExecutionRequest` prose/schema disagreed | Doc | Already applied: added `expiresAt`/`correlationId` to the schema | Closed |
| 3 | Several referenced types had no shape | Interface | Already applied: 5 supporting-type docs added | Closed |
| 4 | Dangling `ADR-005` citation | Doc | Already applied: citation removed. Whether to author a real ADR for event authentication is a separate, still-open human decision | Open (ADR-authoring decision only) |
| 5 | No `Principal`/`Resource` lifecycle transition validators | Transition | Implement `PrincipalLifecycleTransitions`/`ResourceLifecycleTransitions` Kotlin objects, copied directly from the existing `.mmd` diagrams — same shape as `ExecutionLifecycleTransitions`, no new edges invented | Open (small, well-specified code task) |
| 6 | Volume 2 docs were templated placeholders | Doc | Already applied: all 10 filled in | Closed |
| 7 | Build excludes 8 later-phase stubs | N/A | No correction — a build-scope choice, not a defect | Closed |
| 8 | `Permission.schema.json` vs `PermissionDecision.schema.json` duplicate | Doc | Delete `Permission.schema.json` now that `PermissionDecision.schema.json` is documented as authoritative everywhere else — the smallest remaining step is removing the duplicate, not further labelling it | Open (deletion decision) |
| 9 | `Principal.schema.json` disagreed with prose/Kotlin | Doc | Already applied: schema reconciled to prose+Kotlin | Closed |
| 10 | `Resource.kt`'s `sensitivity: String` doesn't use the already-specified enum | Defect (narrow: implementation trails an already-agreed spec) | Change `sensitivity: String` to the existing 9-value enum in `Resource.schema.json` — one field type change, enum already fully specified, no new design | Open (small code fix) |
| 11 | No Volume 3 `ToolRegistry.md` document | Interface | Backfill `docs/specifications/volume-03-core-interfaces/ToolRegistry.md`, summarizing the interface already implemented in `src/interfaces/ToolRegistry.kt` | Open |
| 12 | No `proposedActions` → `PermissionDecision.action` mapping | Interface | Already applied: `action-mapping.md` written | Closed (see #30 for the one leftover interface tension) |
| 13 | EventBus supporting types unspecified | Interface | Already applied: 4 supporting-type docs added | Closed |
| 14 | Dangling `ADR-004` citation in `Agent.md` | Doc | Already applied: citation removed. Same open ADR-authoring decision as #4 | Open (ADR-authoring decision only) |
| 15 | `ExecutionResult.schema.json`/`Resource.schema.json` missing fields | Doc | Already applied for `toolResults`/`reflectionCandidate`/`createdAt`/`updatedAt`/`source`. Remaining: `Resource.schema.json`'s 18-value `resourceType` enum vs. prose/Kotlin's 14 — reconcile by adding `SESSION`/`EVENT`/`TASK`/`WORKFLOW` to `Resource.md` and `ResourceType` in Kotlin (the schema is the more complete artifact here) | Open (content decision, smallest fix identified) |
| 16 | Permission/PermissionDecision duplication, restated | Doc | Same as #8 | Open (same deletion decision) |
| 17 | 12 Volume 3 docs missing version headers | Doc | Already applied | Closed |
| 18 | README/CHANGELOG said "v0.4" | Doc | Already applied | Closed |
| 19 | `RequestOrigin.AGENT` vs `PrincipalType.INTERNAL_AGENT` | Doc | Already applied: clarifying note added; confirmed not a real inconsistency, no rename needed | Closed |
| 20 | `AgentHealth` referenced, never defined | Interface | Add one line to `Agent.md`'s `## Status` section noting `AgentHealth` is undefined and `Agent.kt` is excluded from compilation pending it (documentation only — Agent Framework itself remains out of scope) | Open (one-line doc fix) |
| 21 | No Volume 3 `ToolRegistry.md` (restated) | Interface | Same as #11 | Open |
| 22 | `InMemoryResourceRegistry` added as a supporting dependency | N/A | No correction — deliberate, already-specified interface, correctly implemented as a prerequisite | Closed |
| 23 | Tool Registry discovery has no Principal-scoped visibility | Interface | No interface change recommended yet — adding an unused `caller` parameter now would be a feature with no consumer. Smallest correction: leave `tool-registry.md`'s existing note that this is blocked on IdentityService/PermissionEngine policy; revisit only once those exist | Deferred by design |
| 24 | Tool registration/lifecycle changes not gated by live Permission Engine | Interface (dependency-blocked) | No correction now — no policy-bearing `PermissionEngine.evaluate` exists to gate against. Revisit once one does | Deferred by design |
| 25 | `ActionMapper` doesn't implement `PermissionEngine.evaluate` | Interface (dependency-blocked) | No correction now — implementing real authorisation policy is a distinct, larger unit of work, not a small fix. Recommend `IMPLEMENTATION_ORDER.md` name it explicitly as its own future step | Deferred by design |
| 26 | EventBus auth/signature checks are placeholders | Interface (dependency-blocked) | No correction now — the seam (`PrincipalAuthenticator`) is already in place for real IdentityService integration; nothing to fix until that exists | Deferred by design |
| 27 | EventBus subscriber Principal identity unresolved | Interface | Add a `subscriberPrincipalId: PrincipalId` parameter to `EventBus.subscribe` — a minimal, additive signature change (not a redesign) that lets real identity be asserted by the caller instead of guessed | Open (small interface change, genuinely actionable now) |
| 28 | No standalone `tool-lifecycle-state-machine.mmd` | Doc | Add the file — literally the same `stateDiagram-v2` block already in `tool-registry.md`, copied to its own file for consistency with the Principal/Resource/Session/Task/Workflow pattern | Open (zero-design copy) |
| 29 | `Resource.sensitivity` restated | Defect (same as #10) | Same as #10 | Open |
| 30 | `PermissionEngine.evaluate` signature vs. action-mapping.md's "once per action" model | Defect | Smallest fix is correcting the **document**, not the interface: reword `action-mapping.md`'s "Multiple Actions" section to say the Permission Engine is evaluated once per request (matching the real interface), with `decision.action` reflecting the primary mapped action. Changing the Volume 3 interface itself is the larger alternative and is not recommended unless multi-decision-per-request turns out to be a hard requirement | Open (recommend doc correction over interface change) |
| 31 | No `CREATED -> FAILED` edge for validation failures | Transition | Add exactly one edge, `CREATED -> FAILED`, to `ExecutionLifecycleTransitions`'s allowed map and to `docs/diagrams/execution-state-machine.mmd`. Purely additive — no existing edge changes. Re-run `ExecutionLifecycleTransitionsTest` after, since it may assert the current edge set exhaustively | Open (smallest correction identified, one line) |
| 32 | No concrete `Tool` implementation exists | N/A | No correction — correctly out of scope for every phase so far | Closed |
| 33 | `execution.timed_out` has no matching lifecycle state | Transition (dormant) | No correction now. When real timeouts are eventually implemented, recommend reusing `ExecutionResultStatus.FAILED` with an error-code convention (matching the pattern already used for Tool Registry failures) rather than adding a new state | Deferred, recommendation pre-registered |
| 34 | `DefaultExecutionPipeline` simplifications | N/A | No correction — documented design choices, not gaps | Closed |

## By Classification, at a Glance

- **Documentation inconsistency (Doc):** 2, 3, 4, 6, 8, 9, 14, 15, 16, 17,
  18, 19, 20, 28 — the large majority. Most are already closed; the
  remaining open ones (8/16's deletion, 15's resourceType reconciliation,
  20's one-liner, 28's file copy) are all small, mechanical documentation
  edits with no design content.
- **Missing interface contract (Interface):** 1, 11, 12, 13, 21, 23, 24,
  25, 26, 27 — the largest live category. Most of these (23–26) are not
  actually actionable yet — they're correctly blocked on IdentityService
  and a policy-bearing Permission Engine, which don't exist. Only #27
  (subscriber identity parameter) and #1/#11/#21 (documentation backfill)
  are genuinely actionable now without redesigning anything.
- **Missing state transition (Transition):** 5, 31, 33 — the smallest
  category, and the most concrete: #31 in particular is a single missing
  edge in an already-tested state machine, the clearest "smallest
  possible correction" in this whole review.
- **Actual architectural defect (Defect):** 10, 29, 30 — narrow in scope.
  10/29 is a one-field Kotlin type lagging an already-agreed schema
  enum. 30 is the one case where the *document* (not the code) is the
  thing to correct, because the code already matches a pre-existing,
  tested interface.
- **Not a gap (N/A):** 7, 22, 32, 34 — deliberate choices already working
  as intended.

## Recommended Immediate Actions (ranked, smallest first)

These are the only items in the table above with a concrete, small,
non-design correction ready to apply without waiting on anything else:

1. **#31** — add the single `CREATED -> FAILED` transition edge.
2. **#28** — copy the existing tool-lifecycle diagram into its own `.mmd`
   file.
3. **#20** — one clarifying line in `Agent.md` about `AgentHealth`.
4. **#30** — reword `action-mapping.md`'s "Multiple Actions" section to
   match the existing `PermissionEngine.evaluate` signature.
5. **#27** — add `subscriberPrincipalId` as a parameter to
   `EventBus.subscribe`.
6. **#11/#21** — backfill `ToolRegistry.md`.
7. **#8/#16** — delete `Permission.schema.json`.
8. **#10/#29** — change `Resource.kt`'s `sensitivity` to the existing enum.
9. **#15** (resourceType) — reconcile the 14-vs-18 `ResourceType` list.
10. **#5** — implement the two lifecycle transition validators.

Everything else in the table is either already closed or correctly
blocked on a dependency (IdentityService, a policy-bearing Permission
Engine) that no small correction can substitute for.

## Explicitly Not Done Here

No code was written or changed to produce this document. No new
architecture was introduced. Where a "smallest correction" above is a
code change (items 5, 10/29, 27, 31), it is a recommendation for a future
pass, not applied in this one — consistent with this task's instruction
that this is a refinement review, not an implementation round.
