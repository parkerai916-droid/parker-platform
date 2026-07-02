# Parker Platform — Implementation Gaps

Recorded while implementing Phase 1 (Volume 1 Core Contracts) on
`feature/phase-1-core-contracts`. Updated during the Phase 1 follow-up
specification cleanup pass — each original gap is now marked **Resolved**,
**Deferred**, or **Requires human decision**. Three new findings surfaced
while doing that cleanup are appended at the end.

## 1. `IdentityService` has no interface

**Status: Deferred (explicit decision, recorded per your instruction).**

`IdentityService` is deferred from this Phase 1 implementation pass.
Volume 3 does not include it despite Chapter 41 and `IMPLEMENTATION_ORDER.md`
naming it, and per Phase 7's "do not invent missing architecture," no
interface was written. **A proposed interface may be drafted later as a
reviewable architecture proposal** — explicitly not as a silent addition
to Volume 3, since Volume 3 currently states its included interfaces are
"normative for v0.6 engineering work" and IdentityService isn't among them.

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

### 9. `Principal.schema.json` disagrees with the prose `Principal` contract and this implementation's Kotlin

**Status: Requires human decision.**

The schema's `principalType` enum (`AGENT`, `REMOTE_DEVICE`) differs from
the prose's (`Internal Agent`, `Future Remote Device`) and this
implementation's Kotlin (`INTERNAL_AGENT`, `FUTURE_REMOTE_DEVICE`). The
schema also omits `owner` and `lastSeenAt` entirely, though the prose
requires both. `Principal-Schema.md` now documents this discrepancy.
**`src/contracts/Principal.kt` was not changed in this pass** (this round
was documentation/specification only, per its stated scope) — updating it
to match whichever naming is decided as correct is a recommended, ready-to-do
follow-up once a decision is made.

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
