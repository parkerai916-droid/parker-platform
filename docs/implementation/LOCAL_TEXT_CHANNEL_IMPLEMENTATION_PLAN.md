# Local Text Channel Implementation Plan

## Status

**Version: v1.0 (proposed).**

This is an **implementation planning document**, not an architecture
document, not a Contract Design, and not code. It proposes no new
architectural principle, alters no existing specification or contract,
and adds no file under `src/` or `tests/`. Its job is to sequence and
scope the implementation of exactly what
`docs/architecture/LOCAL_TEXT_CHANNEL_CONTRACT_DESIGN.md` ("the Contract
Design") already authorises — no broader, and nothing it defers.

### Readiness determination

Reviewed to reach this determination:
`docs/architecture/LOCAL_TEXT_CHANNEL_CONTRACT_DESIGN.md`,
`docs/architecture/COMMUNICATION_CHANNEL_ARCHITECTURE.md`,
`docs/architecture/COMMUNICATION_CONTRACT_DESIGN.md`,
`docs/architecture/PARKER_ENGINEERING_STANDARD.md` (PES-001), and
`docs/architecture/IMPLEMENTATION_GAPS.md`.

PES-001 Chapter 1 gates Implementation Plan (Stage 3) behind Architecture
(Stage 1), Architecture Review (Stage 2), and Contract Design (Stage 2A,
"required for new public runtime contracts"). All three are satisfied for
the scope this plan covers:

- **Stage 1 — Architecture.** `COMMUNICATION_CHANNEL_ARCHITECTURE.md`
  Section 6 already names the local text channel as the first concrete
  Communication Channel, with responsibilities, trust boundary, module
  relationship, and lifecycle already settled by that document and by
  `MODULE_FRAMEWORK_ARCHITECTURE.md`.
- **Stage 2 — Architecture Review.** Performed in place, by precedent:
  neither this repository nor PES-001 produces a separate,
  distinctly-named "Architecture Review" document for any subsystem —
  every prior track (Module Framework, Communication) satisfied this
  stage through the parent architecture document's own Engineering
  Review/Conclusion section. `COMMUNICATION_CHANNEL_ARCHITECTURE.md`'s
  own Conclusion states it is ready for Contract Design; that Contract
  Design (`COMMUNICATION_CONTRACT_DESIGN.md`) was subsequently reviewed,
  implemented (Sprint 7, Unit C1), and is not reopened here.
- **Stage 2A — Contract Design.** `LOCAL_TEXT_CHANNEL_CONTRACT_DESIGN.md`
  exists, and meets the same completeness bar
  `MODULE_CONTRACT_DESIGN.md` and `COMMUNICATION_CONTRACT_DESIGN.md` each
  met before the units built on them (Sprint 6 Unit M1; Sprint 7 Unit C1)
  proceeded: a Contract Minimalism Review, field-level shapes for every
  included contract, and its own Stage 2A Self-Traceability Review. Its
  own Engineering Review states plainly that none of its ten Deferred
  Items blocks its one included contract (`LocalTextChannel`) from being
  carried into an Implementation Plan.
- **`IMPLEMENTATION_GAPS.md` check.** Gap #53 (Response Delivery and
  Cognition's consumption of accepted messages) remains open, but covers
  exactly the two things the Contract Design already excludes from this
  channel's scope (outbound delivery, Cognition consumption) — it does
  not block the inbound-only scope this plan covers, and this plan does
  not attempt to close it.

**Determination: PES-001 authorises a Stage 3 Implementation Plan**,
scoped exactly to the Contract Design's one included contract
(`LocalTextChannel`) and its module registration requirements — not to
any of its ten Deferred Items. One Stage 4 Implementation Decision (the
`CorrelationId` minting algorithm, Contract Design Section 5) remains
genuinely open and is named, not resolved, by this plan (Section 3,
below) — a small, named prerequisite to Scope Lock, not a blocker to
writing this plan, mirroring how Sprint 2's own plan named a
gap-log-entry prerequisite for its Unit B1 without that prerequisite
blocking the plan itself.

This plan does not modify `LOCAL_TEXT_CHANNEL_CONTRACT_DESIGN.md`,
`COMMUNICATION_CHANNEL_ARCHITECTURE.md`, `COMMUNICATION_CONTRACT_DESIGN.md`,
or any other architecture or contract document. It does not update
`IMPLEMENTATION_HISTORY.md` — that update belongs to Stage 10, after the
unit below is actually implemented and verified, not to this planning
document.

## Unit Dashboard

| Unit | Scope | Status |
|---|---|---|
| C3 | `LocalTextChannel` interface + `DefaultLocalTextChannel` implementation | Not Started |

Only one implementation unit is scoped by this plan, matching the
Contract Design's own single-contract minimalism finding (one new
interface, zero new data types). This continues Sprint 7's own Track C
numbering: Unit C1 implemented `CommunicationIntake`; Unit C2 was this
plan's own Contract Design (design-only); Unit C3 is the implementation
this plan schedules.

## 1. Plan Purpose

The Contract Design answered *what* the Local Text Channel's inbound
half is, field by field. This plan answers how that shape becomes one
small, reviewable, independently testable implementation unit — with a
stated objective, stated scope, named files, named dependencies,
acceptance criteria, a review checkpoint, an Android Studio verification
gate, and a documentation update, before the unit is considered done.

## 2. Scope

This plan covers exactly the Contract Design's one included contract:

- The `LocalTextChannel` interface (Contract Design Section 1).
- One concrete implementation of it, `DefaultLocalTextChannel`, whose
  only collaborator is `CommunicationIntake` and whose only state is its
  own fixed `ModuleId` (Contract Design Section 1, Section 8).
- The module registration field values the Contract Design specifies for
  this channel's `ModuleDescriptor` (Contract Design Section 4) —
  registration itself uses the existing, unmodified
  `ModuleRegistry.register`, `enable`, `disable`, `remove`.

## 3. Non-Scope

Carried forward from the Contract Design's own Section 10 (Explicit
Deferred Items) without modification. This plan does not schedule:

- Any raw-input mechanism (terminal loop, minimal UI, CLI) for obtaining
  owner text.
- Any mechanism for determining which `PrincipalId` is "the owner."
- Outbound response delivery, a "deliver" `ToolDescriptor`, or any
  `OutboundParkerResponse` construction (blocked on
  `IMPLEMENTATION_GAPS.md` #53).
- Any Cognition or interpretation behaviour for an accepted message.
- Live `PermissionEngine` gating of this channel's own Enable/Disable/Remove
  (inherited, unresolved scope reduction, gaps #24/#52 — unchanged by
  this plan).
- Android, networking, wake word, speech, or notifications.
- A `communication.*` observability event for this channel.

**No Scope Expansion.** If implementing Unit C3 surfaces additional
desirable behaviour beyond what Section 2 names, that behaviour becomes
a candidate for a future, separately-scoped unit — not a silent addition
to Unit C3 — unless it blocks Unit C3's own Acceptance Criteria, in which
case it is handled under Unit Stop Conditions (Section 7), not absorbed
into scope.

## 4. Implementation Decision (Resolved by IDR-001)

One Stage 4 Implementation Decision was required before Scope Lock,
named by the Contract Design (Section 5) and not resolved by it. It has
now been approved as IDR-001, recorded in full immediately below:

| Item | Status | Blocks | Resolution |
|---|---|---|---|
| **`CorrelationId` minting algorithm** — Contract Design Section 5 settles *who* mints it (this channel) and *when* (once, at receipt), but explicitly defers the concrete algorithm to Stage 4. | Approved (IDR-001). | None — resolved; no prerequisite remains before Unit C3 implementation. | Decided (IDR-001): generate a randomly generated UUID for every inbound message, because no stable parent identifier exists to derive one from deterministically (unlike `AgentRunId`/`TaskProposalId`'s parent-derived scheme, `PRE_MODULE_ID_MULTIPLICITY_DECISION.md`'s own precedent for when a deterministic derivation is and is not the correct shape) and a random value requires no shared, mutable state (satisfying Contract Design Section 8's stateless-preferred guidance trivially). |

IDR-001 authorises this algorithm for Unit C3 only. This is an
implementation decision only — it does not amend architecture or
Contract Design, and it introduces no further prerequisite before Unit
C3 implementation begins.

### Implementation Decision Record

```
Decision ID: IDR-001

Subject:
CorrelationId minting algorithm.

Status:
Approved before implementation begins.

Decision:
Generate a random UUID for every inbound message.

Rationale:
- Channel has no stable parent identifier.
- No deterministic derivation exists.
- Stateless implementation.
- Thread-safe.
- Matches the Contract Design's ownership rules.

Applies to:
Unit C3 only.

This decision does not amend architecture or Contract Design.
```

## 5. Implementation Unit

### Unit C3 — Local Text Channel Interface and Default Implementation

- **Objective.** Implement `LocalTextChannel`
  (`docs/architecture/LOCAL_TEXT_CHANNEL_CONTRACT_DESIGN.md` Section 1)
  and its one concrete implementation, `DefaultLocalTextChannel`, exactly
  as the Contract Design specifies — one operation, one collaborator
  (`CommunicationIntake`), zero new data types.
- **Scope.** The interface, its implementation, and Kotlin-level
  registration wiring (constructing this channel's own `ModuleDescriptor`
  with the field values Contract Design Section 4 specifies, and calling
  the existing `ModuleRegistry.register`). Does not include anything
  named in Section 3 (Non-Scope), above.
- **Files expected to change (all new; nothing existing is modified).**
  - `src/interfaces/LocalTextChannel.kt` — the interface (Contract Design
    Section 1). No new data type is added to this file, per the Contract
    Design's own "zero new data types" finding (Section 2).
  - `src/runtime/DefaultLocalTextChannel.kt` — the implementation.
    Constructor takes exactly this channel's own `ModuleId` and an
    injected `CommunicationIntake`; no other dependency.
  - `tests/runtime/FakeCommunicationIntake.kt` — a lambda-based test
    fixture mirroring `FakePermissionEngine`/`FakeMemoryPromotionPolicy`'s
    established precedent, isolating `DefaultLocalTextChannel`'s own
    logic from `InMemoryCommunicationIntake`'s.
  - `tests/runtime/DefaultLocalTextChannelTest.kt` — the unit's own test
    suite (Section 6, Testing Strategy, below).
- **Architecture dependencies.**
  `LOCAL_TEXT_CHANNEL_CONTRACT_DESIGN.md` (this plan's own basis);
  `CommunicationIntake`/`InboundOwnerMessage`/`CorrelationId`/
  `CommunicationIntakeDisposition` (`COMMUNICATION_CONTRACT_DESIGN.md`,
  already implemented, Sprint 7 Unit C1 — reused unchanged);
  `ModuleDescriptor`/`ModuleRegistry`/`ModuleLifecycleTransitions`/
  `ModuleConnectivityDeclaration` (`MODULE_CONTRACT_DESIGN.md`, already
  implemented, Sprint 6 Unit M1 — reused unchanged); the Required
  Implementation Decision (Section 4, above), which must be resolved
  before this unit's own Scope Lock.
- **Acceptance criteria.** See Section 6 (Testing Strategy) for the full
  list; summarised:
  1. A valid submission (non-blank text, a `senderPrincipalId`) through
     an enabled, registered channel produces
     `CommunicationIntakeDisposition.Accepted`, whose `message.channelId`
     equals this channel's own fixed `ModuleId`.
  2. `CommunicationIntake`'s own rejection outcomes (channel not
     `ENABLED`; sender does not resolve) are returned unchanged, never
     translated into an exception.
  3. Blank text fails at construction (`InboundOwnerMessage`'s existing
     validation), before `CommunicationIntake` is ever called.
  4. `CommunicationIntake.submitInboundMessage` is called exactly once
     per `DefaultLocalTextChannel` call that reaches it.
  5. Each call mints a distinct `CorrelationId`; no two calls in a
     concurrent batch collide.
  6. `DefaultLocalTextChannel`'s constructor takes only a `ModuleId` and a
     `CommunicationIntake` — no `ExecutionPipeline`, `ToolRegistry`,
     `PermissionEngine`, `PlannerRuntime`, `AgentRuntime`, `MemoryStore`,
     `WorldModel`, `ModuleRegistry`, or `IdentityService` dependency.
- **Review checkpoint.** Confirm: (a) no new public data type was added
  beyond the one interface (matching the Contract Design's own
  minimalism finding); (b) `DefaultLocalTextChannel` performs no channel-status
  or sender-resolution check of its own — both remain
  `CommunicationIntake`'s alone, per Contract Design Section 6/9; (c) no
  outbound delivery, `ExecutionRequest`, or Cognition-adjacent code was
  introduced.
- **Android Studio verification.** Full existing suite passes unmodified
  (Sprint 7 Unit C1's 26 tests and every prior test, none of which this
  unit touches) plus this unit's new tests.
- **Documentation updates required before this unit is considered
  complete.** Add an `IMPLEMENTATION_HISTORY.md` entry for Unit C3,
  following the established format. `IMPLEMENTATION_GAPS.md` #53 is
  **not** closed by this unit (its outbound-delivery and
  Cognition-consumption items remain exactly as open as before) — if
  anything, a brief note may be added confirming the inbound half this
  gap's own clarification recommended is now implemented, without
  claiming the gap itself is resolved.
- **Definition of Done.** Unit C3 is complete when:
  - `LocalTextChannel` and `DefaultLocalTextChannel` exist, matching the
    Contract Design's field-level shape exactly.
  - Zero new public data types were introduced.
  - All Section 6 tests pass, including the concurrency and delegation
    tests.
  - The full existing suite passes unmodified.
  - `IMPLEMENTATION_HISTORY.md` is updated; `IMPLEMENTATION_GAPS.md` #53
    is left open, not closed, and not misrepresented as resolved.

## 6. Testing Strategy

No new file is required under `tests/contracts/` — the Contract Design
introduces zero new data types, so every construction-time validation
this unit relies on (`InboundOwnerMessage.text`, `PrincipalId`,
`CorrelationId`) is already covered by Sprint 7 Unit C1's own
`tests/contracts/CommunicationContractsTest.kt` and earlier identifier
tests, unmodified.

`tests/runtime/DefaultLocalTextChannelTest.kt` covers, mirroring the
depth and style of `tests/runtime/InMemoryCommunicationIntakeTest.kt`:

- **Successful path.** A valid call produces `Accepted`; the accepted
  message's `channelId` equals this channel's own fixed `ModuleId`; the
  message is inspectable via `CommunicationIntake`'s own existing
  `acceptedMessages()`/`acceptedMessageFor` when a real
  `InMemoryCommunicationIntake` is used (one integration-style test using
  real `InMemoryCommunicationIntake` + `InMemoryModuleRegistry` +
  `InMemoryIdentityService` together, proving genuine end-to-end wiring,
  not just fake-isolated behaviour).
- **Rejection passthrough.** Using `FakeCommunicationIntake`, a
  `Rejected` disposition returned by the fake is returned by
  `DefaultLocalTextChannel` unchanged — no exception, no
  reinterpretation.
- **Invalid request handling.** Blank text throws `IllegalArgumentException`
  before `FakeCommunicationIntake.submitInboundMessage` is ever called
  (asserted via the fake's own call count, mirroring
  `InMemoryMemoryStoreTest`'s "policy seam is consulted internally,
  exactly once" pattern, applied here in reverse — zero calls on a
  construction failure).
- **Delegation correctness.** `FakeCommunicationIntake.submitInboundMessage`
  is called exactly once per `DefaultLocalTextChannel` call that reaches
  it, with an `InboundOwnerMessage` whose fields match what was supplied
  (text, senderPrincipalId, timestamp, metadata) and whose `channelId`
  matches this channel's own fixed `ModuleId`.
- **`CorrelationId` behaviour.** Two calls, otherwise identical, mint two
  different `CorrelationId`s (a determinism-of-uniqueness check, not a
  determinism-of-value check — the Contract Design never requires
  reproducible values, only non-reuse).
- **Timestamp handling.** A caller-supplied timestamp is threaded through
  unchanged; an omitted one falls back to a current-time default.
- **Metadata passthrough.** Supplied metadata reaches the constructed
  `InboundOwnerMessage` unchanged.
- **Thread safety.** A concurrent batch (mirroring Unit C1's own 50-way
  concurrency test) of valid calls through a real
  `InMemoryCommunicationIntake` all succeed, each with a distinct
  `CorrelationId`, with no lost or corrupted submissions.
- **Scope discipline (structural).** `DefaultLocalTextChannel`'s
  constructor takes only a `ModuleId` and a `CommunicationIntake` — the
  same "structural proof via constructor signature" pattern
  `InMemoryCommunicationIntakeTest`/`InMemoryMemoryStoreTest` already
  establish, not a runtime assertion.

## 7. Unit Stop Conditions

If implementing Unit C3 reveals a contract gap, a Contract Design
contradiction, or a new architectural boundary, implementation of Unit
C3 pauses. The issue is classified before coding continues: a contract
gap or contradiction returns to Contract Design for a revision (a new
version of `LOCAL_TEXT_CHANNEL_CONTRACT_DESIGN.md`, not a silent
in-Kotlin correction); a new architectural boundary returns to
Architecture. Implementation resumes only once the correction is
reviewed and accepted — the same discipline every prior Sprint's plan in
this repository has followed.

## 8. Dependencies

- **Already satisfied, reused unchanged:** `CommunicationIntake` and its
  supporting contracts (Sprint 7, Unit C1); `ModuleRegistry` and its
  supporting contracts (Sprint 6, Unit M1).
- **Already satisfied:** `LOCAL_TEXT_CHANNEL_CONTRACT_DESIGN.md` (Stage
  2A), reviewed for completeness in this plan's own Readiness
  Determination, above.
- **Resolved implementation dependency:** IDR-001 approves the
  `CorrelationId` minting algorithm (random UUID). No remaining
  implementation-decision dependency blocks Unit C3.
- **Not a dependency of this unit:** `IMPLEMENTATION_GAPS.md` #53's two
  open items (outbound delivery, Cognition consumption). Unit C3 does
  not touch, narrow, or depend on either being resolved.

## 9. Architectural Traceability

| Planned Kotlin element | Authorised by (Contract Design section) | Ultimately traces to |
|---|---|---|
| `LocalTextChannel` interface | Section 1 | `COMMUNICATION_CHANNEL_ARCHITECTURE.md` Section 6, Section 8/11 |
| `DefaultLocalTextChannel` implementation | Section 1, Section 8 (thread-safety), Section 9 (lifecycle) | Same, plus PES-001 Chapter 7.1/7.2 |
| Zero new data types | Section 2 | `COMMUNICATION_CONTRACT_DESIGN.md` Sections 2, 4, 6 (all reused) |
| Construction-time validation reused, no new rule | Section 3 | `COMMUNICATION_CONTRACT_DESIGN.md` Section 2 |
| `ModuleDescriptor` field values (`LOCAL_ONLY`, empty tools/permissions) | Section 4 | `COMMUNICATION_CHANNEL_ARCHITECTURE.md` Section 6; `MODULE_CONTRACT_DESIGN.md` Sections 2, 5 |
| `CorrelationId` minted once, by this channel, at receipt | Section 5 | `COMMUNICATION_CONTRACT_DESIGN.md` Section 4 |
| No Principal resolution performed by this channel | Section 6 | `COMMUNICATION_CONTRACT_DESIGN.md` Section 5, Section 6 |
| Construction exceptions vs. structural `Rejected`, no new error type | Section 7 | `COMMUNICATION_CONTRACT_DESIGN.md` Section 6, Section 8 |
| Suspend-capable operation; lock discipline if state is retained | Section 8 | PES-001 Chapter 7.1, Chapter 7.2 |
| Lifecycle via unmodified `ModuleLifecycleTransitions`; no self-enable | Section 9 | `MODULE_CONTRACT_DESIGN.md` Section 4; `MODULE_FRAMEWORK_ARCHITECTURE.md` Section 4, Section 7 |
| Everything in Section 3 (Non-Scope) | Section 10 (Deferred Items) | Named explicitly; none invented by this plan |

No file this plan schedules introduces a concept the Contract Design did
not already authorise. No item in Section 3 (Non-Scope) is scheduled,
scoped, or partially implemented by Unit C3.

## 10. Completion Criteria

This plan is complete when:

- Unit C3 reaches its own Definition of Done (Section 5).
- The full Android Studio test suite passes, with no existing test
  modified.
- `IMPLEMENTATION_HISTORY.md` records Unit C3.
- `IMPLEMENTATION_GAPS.md` #53 remains open and accurately described —
  not closed, not silently narrowed beyond what Unit C3 actually
  changes.
- A Post-Implementation Review, including a Self-Traceability Review
  (PES-001 Stage 9), confirms every public type Unit C3 introduces is
  authorised by `LOCAL_TEXT_CHANNEL_CONTRACT_DESIGN.md`, per PES-001's
  Level 2 requirement (this unit is a Level 2 — Behavioural
  Implementation — per PES-001 Chapter 4: new runtime behaviour and new
  tests, introducing no new runtime subsystem, no lifecycle redesign,
  and no trust-model change).

## Governance Statement

This plan schedules exactly one unit. No unit begins coding until the
Required Implementation Decision (Section 4) is resolved and Scope Lock
(PES-001 Stage 5) is confirmed: the Contract Design is accepted, the
Included/Excluded scope above is frozen, and this unit's Acceptance
Criteria are agreed. Once accepted, this plan is frozen as **v1.0**; a
change of scope or sequence is handled through Unit Stop Conditions
(Section 7), not by editing this document in place during coding.

## Related

- `docs/architecture/LOCAL_TEXT_CHANNEL_CONTRACT_DESIGN.md`
- `docs/architecture/COMMUNICATION_CHANNEL_ARCHITECTURE.md`
- `docs/architecture/COMMUNICATION_CONTRACT_DESIGN.md`
- `docs/architecture/MODULE_CONTRACT_DESIGN.md`
- `docs/architecture/MODULE_FRAMEWORK_ARCHITECTURE.md`
- `docs/architecture/PARKER_ENGINEERING_STANDARD.md`
- `docs/architecture/IMPLEMENTATION_GAPS.md` (#53)
- `docs/implementation/IMPLEMENTATION_HISTORY.md`
- `src/interfaces/CommunicationIntake.kt`, `src/runtime/InMemoryCommunicationIntake.kt` (Sprint 7, Unit C1)
- `src/interfaces/ModuleRegistry.kt`, `src/runtime/InMemoryModuleRegistry.kt` (Sprint 6, Unit M1)
