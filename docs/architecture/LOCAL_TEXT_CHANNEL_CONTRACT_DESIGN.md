# Local Text Channel Contract Design

## Status

Design proposal, not yet reviewed or accepted. **This document is
contract design only.** No Kotlin is implemented, proposed as a diff, or
changed by it — every shape below is described in prose, not as a
`kotlin`-fenced signature block, precisely so nothing here can be
mistaken for an implementation. Neither `src/` nor `tests/` is touched.
`IMPLEMENTATION_HISTORY.md` is untouched. `IMPLEMENTATION_GAPS.md` is
untouched — this document closes no gap. No module, module loading,
terminal I/O, CLI, Android, or networking implementation is added. No
other document is modified.

### Why this unit exists

`docs/architecture/COMMUNICATION_CHANNEL_ARCHITECTURE.md` named the local
text channel as the first concrete Communication Channel
(`COMMUNICATION_CHANNEL_ARCHITECTURE.md` Section 6) but explicitly
excluded "any actual channel implementation, including the local text
channel" from its own scope (Section 8), deferring the channel's own
field-level shape to a later, separately-scoped Contract Design pass
(Section 10, Section 11's own instruction to a "future Module/Communication
Contract Design"). `docs/architecture/COMMUNICATION_CONTRACT_DESIGN.md`
gave field-level shape to `CommunicationIntake` and its supporting
message contracts, and was itself implemented (Sprint 7, Unit C1,
`src/interfaces/CommunicationIntake.kt`, `src/runtime/InMemoryCommunicationIntake.kt`)
— but that document also explicitly excluded "any actual channel
implementation, including the local text channel named as the first
implementation target" from its own scope (its own Section 11), naming
this exact gap in its Conclusion as a bounded, deferred follow-up.

This document performs the field-level design pass both parent documents
deferred: it gives the local text channel's own inbound half a Kotlin
shape, so a future implementation unit builds it against an
already-approved contract, never by inventing one mid-Kotlin — the same
relationship `MODULE_CONTRACT_DESIGN.md` bears to
`MODULE_FRAMEWORK_ARCHITECTURE.md`, and `COMMUNICATION_CONTRACT_DESIGN.md`
bears to `COMMUNICATION_CHANNEL_ARCHITECTURE.md`.

This document performs the contract minimalism review PES-001 and this
platform's own precedent require throughout: every candidate contract is
checked against a concrete, cited need in the authoritative sources below
before it is accepted, and every named requirement this document cannot
derive from those sources is marked **Deferred**, not designed.

## Review

Reviewed, in authority order:

1. `docs/architecture/parker-constitution.md` — the fixed point every
   contract below is checked against; in particular "Parker owns
   authority. Modules provide capability," and "No capability may bypass
   trust."
2. `docs/architecture/PARKER_ENGINEERING_STANDARD.md` (PES-001) — Stage
   2A's own purpose and required content (Chapter 1); the in-memory
   concurrency and policy-seam discipline (Chapter 7) a future
   implementation of this document's contracts must follow.
3. `docs/architecture/COMMUNICATION_CHANNEL_ARCHITECTURE.md` — the
   architecture this document gives field-level shape to for the channel
   itself, and does not redefine. Section 6 (First Implementation
   Target), Section 3 (Minimum Message Shape), Section 7 (Module
   Relationship), and Section 8/9's own deferral of the channel's Kotlin
   shape are this document's primary source.
4. `docs/architecture/COMMUNICATION_CONTRACT_DESIGN.md` — the
   already-approved, already-implemented `CorrelationId`,
   `InboundOwnerMessage`, `CommunicationIntakeDisposition`, and
   `CommunicationIntake` contracts this channel is built to submit into,
   reused here unchanged, never re-specified.
5. `docs/architecture/MODULE_FRAMEWORK_ARCHITECTURE.md` — the module
   concept a Communication Channel already is (`COMMUNICATION_CHANNEL_ARCHITECTURE.md`
   Section 1, Section 7); this document does not reopen what a module is
   or may do.
6. `docs/architecture/MODULE_CONTRACT_DESIGN.md` — the already-approved,
   already-implemented `ModuleId`, `ModuleDescriptor`,
   `ModulePermissionRequirement`, `ModuleConnectivityDeclaration`,
   `ModuleStatus`, `ModuleLifecycleTransitions`, and `ModuleRegistry`
   contracts this channel's own registration reuses unchanged.

Also consulted for accurate context, though not itself an authoritative
architecture source this document is bound by: `docs/architecture/IMPLEMENTATION_GAPS.md`
#53, which already records that outbound Response Delivery and
Cognition's consumption of an accepted message remain open, unresolved
blockers this document does not attempt to close.

---

## Constitutional Boundaries

Restated up front, identical in substance to the Constitution and to
`COMMUNICATION_CHANNEL_ARCHITECTURE.md` Section 1/Section 5 and
`MODULE_FRAMEWORK_ARCHITECTURE.md` Section 1/Section 3, not re-derived
differently here:

- **The Local Text Channel is an ordinary module, and therefore an
  ordinary Principal.** No contract below grants it implicit trust,
  self-approval, or a bypass of Identity/Permission evaluation.
- **The Local Text Channel carries a message. It never decides what the
  message means.** It performs no interpretation of `text` beyond what is
  required to construct an `InboundOwnerMessage` (Section 2, below); it
  never decides whether the message warrants an action.
- **The Local Text Channel never executes, and never reaches
  `ExecutionPipeline`, `PermissionEngine`, `PlannerRuntime`,
  `AgentRuntime`, `MemoryStore`, or `WorldModel`.** Its only downstream
  collaborator is `CommunicationIntake` (Section 6 of
  `COMMUNICATION_CONTRACT_DESIGN.md`); submitting an inbound message
  through `CommunicationIntake` is this channel's entire path into the
  rest of Parker.
- **`EventBus` is never an acceptable delivery path for the message this
  channel carries**, restated unchanged from `COMMUNICATION_CHANNEL_ARCHITECTURE.md`
  Section 3 and `COMMUNICATION_CONTRACT_DESIGN.md`'s own Constitutional
  Boundaries.
- **No contract below grants the Local Text Channel a permission by
  existing, registering, or being Enabled.** Every actual invocation of
  anything it later exposes remains independently evaluated by
  `PermissionEngine.evaluate`, exactly as `MODULE_FRAMEWORK_ARCHITECTURE.md`
  Section 3 and Section 7 already require of every module.

---

## Contract Minimalism Review — Summary

| Candidate | Determination |
| --- | --- |
| `LocalTextChannel` (new interface) | **Include.** The one genuinely new contract this document introduces — the field-level shape both `COMMUNICATION_CHANNEL_ARCHITECTURE.md` (Section 8/11) and `COMMUNICATION_CONTRACT_DESIGN.md` (Section 11) explicitly deferred to a future, separately-scoped Contract Design pass. |
| A new `LocalTextChannelMessage`/wrapper data type | **Exclude.** `InboundOwnerMessage` and `CommunicationIntakeDisposition` already fully express everything this channel needs to carry or return; a wrapper would duplicate structure, not add it. |
| A new, channel-specific `ChannelId` type | **Exclude.** Already excluded by `COMMUNICATION_CONTRACT_DESIGN.md` Section 1; this document does not reopen it. `ModuleId` remains this channel's identity. |
| A new exception/error type | **Exclude.** Construction-time validation (`InboundOwnerMessage`'s existing blank-text rejection) and `CommunicationIntakeDisposition.Rejected` already cover every failure mode this channel can produce (Section 7, below). |
| A `LocalTextChannel`-local `ModuleRegistry.getModuleStatus` check | **Exclude.** `CommunicationIntake` already performs the channel-status check on every submission (`COMMUNICATION_CONTRACT_DESIGN.md` Section 6); a second, local copy would be redundant, unauthorised logic, and a second place for that check to drift out of sync with the first. |
| A raw-input seam (e.g. a `TextInputSource`-shaped policy interface, mirroring `AgentStepSource`/`PlanDecision`) | **Exclude.** No approved architecture document names or shapes a raw-input seam for this channel; inventing one now would exceed this Unit's own "do not implement CLI terminal behaviour" boundary (see Section 10, Deferred Items). |
| A concrete `CorrelationId` minting algorithm | **Exclude from this document.** `COMMUNICATION_CONTRACT_DESIGN.md` Section 4 already declines to mandate one; this document settles *who* mints it and *when* (Section 5, below) but leaves the concrete algorithm to Stage 4 Implementation Decisions, mirroring the parent document's own explicit non-mandate. |
| A concrete Principal-resolution / login mechanism | **Exclude.** Not named or shaped anywhere in the six authoritative sources. **Deferred** (Section 6, Section 10). |
| A "deliver" `ToolDescriptor` registered now, for future outbound use | **Exclude.** `COMMUNICATION_CONTRACT_DESIGN.md` Section 7's own disclosed tension (no `ExecutionRequest` payload field) blocks a real deliver Tool from being meaningfully implemented today; registering a non-functional placeholder would misrepresent this module's actual, current capability. Deferred entirely to whichever future unit resolves gap #53. |
| A `communication.*` `EventBus` observability event for this channel | **Exclude.** `COMMUNICATION_CHANNEL_ARCHITECTURE.md` Section 3's own parenthetical already frames this as optional and additive; no concrete need is identified here, and this document does not decide it. |
| A self-registration helper method on `LocalTextChannel` | **Exclude.** Registration already has a single, existing surface (`ModuleRegistry.register`, `MODULE_CONTRACT_DESIGN.md` Section 5); adding a second, channel-specific registration path would duplicate it, not simplify it (Section 4, below). |

Net result: **one new contract** (`LocalTextChannel`), **zero new data
types**, and every other candidate this review considered is either
reused unchanged or explicitly excluded/deferred for a stated, concrete
reason.

---

## 1. Public Kotlin Interfaces

**`LocalTextChannel`** — the single new public interface this document
introduces, mirroring `CommunicationIntake`'s own minimalism (one
interface, one operation, described in prose, no signature block).

- **Purpose.** The field-level shape of "the local text channel," per
  `COMMUNICATION_CHANNEL_ARCHITECTURE.md` Section 6 — the boundary
  between raw owner text (already obtained, by whatever mechanism a
  future, separately-scoped unit supplies — Section 10, Deferred Items)
  and the already-approved `CommunicationIntake` boundary (Sprint 7, Unit
  C1).
- **One operation, in prose:** given the owner's already-obtained input
  text, the `PrincipalId` responsible for it, and optionally a timestamp
  and channel-specific metadata, an implementation:
  1. determines the timestamp to record — the caller-supplied value if
     one is given, otherwise the current time at the moment of the call
     (Section 3, below);
  2. mints a fresh `CorrelationId` (Section 5, below);
  3. constructs an `InboundOwnerMessage` (`COMMUNICATION_CONTRACT_DESIGN.md`
     Section 2) using this channel's own `ModuleId` — fixed at
     construction, per Section 1 of that document's "one channel, one
     `ModuleId`" rule — as `channelId`, the supplied `senderPrincipalId`,
     the supplied text, the resolved timestamp, the minted `CorrelationId`,
     and the supplied metadata (defaulted to empty);
  4. submits that message, unchanged, to the injected `CommunicationIntake`'s
     `submitInboundMessage` operation; and
  5. returns the resulting `CommunicationIntakeDisposition` unchanged —
     no translation, wrapping, or reinterpretation of it.
- **Suspend-capable.** This operation must be declared `suspend`,
  matching `CommunicationIntake.submitInboundMessage`'s own shape and
  PES-001 Chapter 7.2's "suspend-capable" guidance for any interface a
  future, possibly-slower implementation might need to await.
- **Dependencies.** An implementation's only collaborator is
  `CommunicationIntake`. No dependency on `ModuleRegistry`, `IdentityService`,
  `ExecutionPipeline`, `ToolRegistry`, `PermissionEngine`, `PlannerRuntime`,
  `AgentRuntime`, `MemoryStore`, `WorldModel`, or `EventBus` is introduced
  or authorised by this document.
- **What this interface must not do**, restated from the Constitutional
  Boundaries above and `COMMUNICATION_CHANNEL_ARCHITECTURE.md` Section 1:
  interpret `text` beyond what constructing an `InboundOwnerMessage`
  requires; decide whether the message implies an action; construct or
  reference an `ExecutionRequest`; perform outbound delivery or construct
  an `OutboundParkerResponse`; or call anything other than
  `CommunicationIntake`.

## 2. Data Contracts

**No new data contract is introduced.** Every value this document's one
interface handles is an existing, already-approved type:

- `ModuleId` (`MODULE_CONTRACT_DESIGN.md` Section 1) — this channel's own
  fixed identity.
- `PrincipalId` (Volume 1 core contract, reused unchanged) — the supplied
  `senderPrincipalId`.
- `String` — the owner's raw input text, and the values inside any
  supplied metadata map.
- `java.time.Instant` — the optional caller-supplied timestamp, or the
  current time if none is supplied.
- `CorrelationId`, `InboundOwnerMessage`, `CommunicationIntakeDisposition`
  (`COMMUNICATION_CONTRACT_DESIGN.md` Sections 2, 4, 6) — reused
  unchanged, exactly as Unit C1 already implemented them.

This is a deliberate finding, not an oversight: see the Minimalism
Review above for each candidate new data type this document considered
and excluded.

## 3. Validation Rules

No new validation rule is introduced. Every constraint this channel's
operation is subject to already exists, unchanged, on the types it
constructs or calls:

- **Text must be non-blank.** Enforced by `InboundOwnerMessage`'s own
  existing constructor validation (`COMMUNICATION_CONTRACT_DESIGN.md`
  Section 2) — this document does not duplicate that check in
  `LocalTextChannel` itself; a blank-text call fails exactly where it
  already fails today, via that constructor.
- **`senderPrincipalId` must be a well-formed, non-blank `PrincipalId`.**
  Enforced by `PrincipalId`'s own existing constructor validation. Whether
  it *resolves* to a registered `Principal` is a separate question,
  checked downstream by `CommunicationIntake` itself (Section 6, below;
  `COMMUNICATION_CONTRACT_DESIGN.md` Section 6) — not by this channel.
- **`CorrelationId` must be non-blank.** Enforced by `CorrelationId`'s own
  existing constructor validation, regardless of which minting scheme a
  future implementation chooses (Section 5, below).
- **Metadata carries no validation of its own** beyond what
  `InboundOwnerMessage.metadata`'s existing, unconstrained `Map<String, String>`
  shape already allows — mirroring that field's own "non-authoritative
  extension point" treatment.
- **No validation rule in this document may be read as a trust or
  permission decision.** Every check named above is structural
  (well-formedness), never a decision about whether the resulting message
  should be acted on — that distinction is `CommunicationIntake`'s own
  (Section 8, `COMMUNICATION_CONTRACT_DESIGN.md`), restated, not
  re-derived, here.

## 4. Module Registration Requirements

The Local Text Channel registers through the existing, unmodified
`ModuleRegistry.register(descriptor)` — no new registration path,
method, or type is introduced (mirroring `MODULE_CONTRACT_DESIGN.md`
Section 5's own "no second registration path" rule).

This document specifies the following field values for this channel's
own `ModuleDescriptor`, each traceable to a named authoritative source:

- **`moduleId`** — a stable, caller-declared `ModuleId`. Its exact string
  value is an implementation-time choice (`MODULE_CONTRACT_DESIGN.md`
  Section 1: a `ModuleId` is declared, never minted by Parker), not an
  architectural constraint this document fixes.
- **`toolsExposed`** — **empty**, for this Unit. No "deliver" Tool is
  registered yet (see the Minimalism Review above and Section 10,
  Deferred Items) — registering a Tool that cannot yet be meaningfully
  invoked, given `COMMUNICATION_CONTRACT_DESIGN.md` Section 7's own
  disclosed `ExecutionRequest` content-carrying gap, would misrepresent
  this module's actual capability.
- **`requiredPermissions`** — **empty**, for this Unit. With no exposed
  Tool, there is nothing for this channel to declare a
  `ModulePermissionRequirement` against yet.
- **`connectivityDeclaration`** — **`LOCAL_ONLY`**, per
  `COMMUNICATION_CHANNEL_ARCHITECTURE.md` Section 6's own "almost
  certainly `LOCAL_ONLY` for the first implementation target."
- **`eventSubscriptions`** — **empty**. No subscription need is
  identified for this channel, and any future one remains additionally
  gated on ADR-024's own precondition (`MODULE_CONTRACT_DESIGN.md`
  Section 8), unaffected by this document.
- **`minimumPlatformVersion`** — not specified by this document; remains
  the optional field `MODULE_CONTRACT_DESIGN.md` Section 2 already
  defines.

**Enable/Disable/Remove.** This channel's `ModuleStatus` moves through
the same, unmodified `ModuleLifecycleTransitions`
(`REGISTERED -> {ENABLED, REMOVED}`, `ENABLED -> {DISABLED}`,
`DISABLED -> {ENABLED, REMOVED}`, `REMOVED -> {}`) as every other module.
Consistent with `MODULE_FRAMEWORK_ARCHITECTURE.md` Section 4/Section 7,
this channel never self-enables: Enable, Disable, and Remove each remain
an explicit, attributable decision by some other Principal, exactly as
`ModuleRegistry.enable`/`disable`/`remove` already require. Whether that
decision is itself gated by a live `PermissionEngine` evaluation is not
resolved by this document — it inherits, unchanged, the same disclosed
scope reduction already recorded for Module Registry generally
(`IMPLEMENTATION_GAPS.md` #24, #52), neither newly introduced nor newly
resolved here.

## 5. CorrelationId Ownership

**The Local Text Channel mints the `CorrelationId`, once, at the moment
it constructs an `InboundOwnerMessage` — never `CommunicationIntake`,**
per `COMMUNICATION_CONTRACT_DESIGN.md` Section 4's own settled ownership
rule ("the Communication Channel mints a `CorrelationId`... before ever
calling `CommunicationIntake`. `CommunicationIntake` trusts the
`CorrelationId` already present on the message; it does not mint or
overwrite one").

This document settles *who* mints it and *when*. It does **not** settle
*how*:

- **Required:** non-blank (enforced by `CorrelationId`'s own existing
  constructor), minted exactly once per submitted message, never reused
  across two distinct owner messages, and threaded unchanged into the
  `InboundOwnerMessage` this channel constructs.
- **Deferred:** the concrete minting algorithm (a random/UUID scheme, a
  monotonic counter, or otherwise) is a Stage 4 Implementation Decision,
  not a Stage 2A Contract Design matter — mirroring
  `COMMUNICATION_CONTRACT_DESIGN.md` Section 4's own explicit refusal to
  mandate one. If a future implementation chooses a scheme that retains
  shared, mutable state (e.g. a counter), Section 8 (Thread-Safety
  Expectations, below) governs how that state must be guarded.

## 6. Principal Resolution Requirements

- **This channel does not resolve, authenticate, or verify
  `senderPrincipalId`.** It is supplied to `LocalTextChannel`'s one
  operation by its caller (Section 1). Whether the supplied value
  actually resolves to a registered `Principal` is checked downstream,
  exactly once, by `CommunicationIntake.submitInboundMessage`'s own
  existing sender-resolution check (`COMMUNICATION_CONTRACT_DESIGN.md`
  Section 6; already implemented, Sprint 7 Unit C1) — this document does
  not duplicate, pre-check, or short-circuit that resolution.
- **Per `COMMUNICATION_CONTRACT_DESIGN.md` Section 5**, this document
  does not require the resolved Principal's `principalType` to be `USER`
  specifically, or its `status` to be `ACTIVE` specifically — the same
  rule Unit C1 already implements, restated, not narrowed, here.
- **Deferred:** the mechanism by which a future, separately-scoped raw-input
  layer determines *which* `PrincipalId` corresponds to "the owner" for a
  given piece of typed input — a fixed, configured value; a single-owner
  assumption; a login or session mechanism; or something else — is named
  nowhere in the six authoritative sources this document is bound by, and
  is not designed here. `LocalTextChannel`'s own interface requires only
  that *some* `PrincipalId` be supplied; it does not shape how a caller
  obtains one.

## 7. Error Model

**No new exception or error type is introduced.** This document reuses
the two failure shapes the authoritative sources already establish, at
the two points where a failure can occur:

- **Construction-time failures** (blank `text`, a malformed `senderPrincipalId`,
  or a blank `CorrelationId`) surface as `IllegalArgumentException`,
  thrown by the existing constructors of `InboundOwnerMessage`,
  `PrincipalId`, and `CorrelationId` respectively — exactly the
  established pattern every other blank-rejecting value type in this
  codebase already uses. `LocalTextChannel` does not catch, wrap, or
  reinterpret these.
- **Intake-time failures** (the channel's own backing module is not
  `ENABLED`; the supplied `senderPrincipalId` does not resolve) are
  **not** exceptions. They surface as `CommunicationIntakeDisposition.Rejected`,
  exactly as `CommunicationIntake` already defines and Unit C1 already
  implements, per that document's own reasoning that such a rejection is
  "an expected, routine outcome of ordinary channel lifecycle timing... not
  a caller-misuse condition" (`COMMUNICATION_CONTRACT_DESIGN.md` Section
  6). `LocalTextChannel`'s one operation must return this disposition
  unchanged, never translate a `Rejected` outcome into a thrown
  exception — doing so would contradict the precedent Unit C1 already
  established for exactly this boundary.
- **No error path in this document ever reaches, or is confused with, a
  `PermissionEngine` denial.** Nothing in this channel's own scope
  evaluates or represents a permission decision; `CommunicationIntakeDisposition`'s
  two structural checks (channel status, sender resolution) are
  preconditions, not permission decisions, restated unchanged from
  `COMMUNICATION_CONTRACT_DESIGN.md` Section 8.

## 8. Thread-Safety Expectations

- **`LocalTextChannel`'s one operation is `suspend`-declared** (Section
  1), so a future implementation may be called concurrently by whatever
  caller ultimately drives it.
- **If a concrete implementation retains no shared, mutable state**
  (for example, a `CorrelationId` minting scheme with no shared counter),
  **no additional synchronisation is required.** This is the simplest
  compliant shape and is not disallowed or discouraged by anything in
  this document.
- **If a concrete implementation does retain shared, mutable state**
  (for example, a monotonic counter-based minting scheme), it must follow
  PES-001 Chapter 7.1's In-Memory Concurrency Rule exactly: any exclusion
  mechanism guarding that state must not be held across the `suspend`
  call into the injected `CommunicationIntake` collaborator. A lock, if
  one exists, is acquired only for the short, synchronous read/write of
  this channel's own state, and released before calling
  `CommunicationIntake.submitInboundMessage` — mirroring
  `InMemoryAgentRuntime`'s and `InMemoryPlannerRuntime`'s established
  "read state, unlock, call collaborator" shape, not the discouraged
  alternative PES-001 Chapter 7.1 itself names.
- **`LocalTextChannel` does not need to reason about `CommunicationIntake`'s
  own concurrency.** `InMemoryCommunicationIntake` already guards its own
  state and safely accepts concurrent submissions (proved by Sprint 7
  Unit C1's own concurrency tests); this document imposes no additional
  serialisation requirement on top of that guarantee.

## 9. Lifecycle

- **The Local Text Channel's backing module moves through the same,
  unmodified `ModuleLifecycleTransitions`** every other module does
  (Section 4, above): `REGISTERED -> {ENABLED, REMOVED}`,
  `ENABLED -> {DISABLED}`, `DISABLED -> {ENABLED, REMOVED}`,
  `REMOVED -> {}` (terminal).
- **`LocalTextChannel` itself carries no lifecycle of its own**, distinct
  from its backing `ModuleStatus` — mirroring `CommunicationIntake`'s own
  "no lifecycle of its own" treatment and `ModuleRegistry`'s identical
  pattern (`MODULE_CONTRACT_DESIGN.md` Section 5). There is no
  "`LocalTextChannel`-specific" enabled/disabled state to track beyond
  what `ModuleRegistry` already tracks for its backing `ModuleId`.
- **A submission while the backing module is not `ENABLED` is not a
  lifecycle violation `LocalTextChannel` itself must detect or prevent.**
  It is simply rejected, downstream, by `CommunicationIntake`'s own
  existing channel-status check (Section 6 of
  `COMMUNICATION_CONTRACT_DESIGN.md`; Section 7 of this document,
  above) — `LocalTextChannel` does not duplicate that check (Minimalism
  Review, above).
- **No self-enable.** Consistent with `MODULE_FRAMEWORK_ARCHITECTURE.md`
  Section 4/Section 7, Enable/Disable/Remove for this channel's backing
  module remain an explicit, attributable decision by some Principal
  other than the channel itself; `LocalTextChannel`'s own interface
  exposes no operation that could be mistaken for one.

## 10. Explicit Deferred Items

The following are named, not designed, per this document's own
instruction to mark undecidable items Deferred rather than invent them:

1. **The concrete mechanism for obtaining raw owner text** — a terminal
   read loop, a minimal local UI, a test harness, or otherwise. Excluded
   from this Unit's own scope ("must not implement CLI terminal
   behaviour"); named nowhere at the field level in any of the six
   authoritative sources.
2. **The mechanism for determining which `PrincipalId` is "the owner"**
   for a given piece of input text (Section 6, above) — fixed
   configuration, a single-owner assumption, a login/session mechanism,
   or something else.
3. **The concrete `CorrelationId` minting algorithm** (Section 5, above)
   — the requirement (non-blank, minted once, at receipt) is settled; the
   algorithm is not, mirroring `COMMUNICATION_CONTRACT_DESIGN.md` Section
   4's own explicit non-mandate.
4. **Outbound response delivery** — this channel's own "deliver" Tool,
   and any construction or delivery of an `OutboundParkerResponse`.
   Remains blocked on `ExecutionRequest`'s content-carrying gap
   (`IMPLEMENTATION_GAPS.md` #53) and is not addressed by this document.
5. **Any Cognition or interpretation behaviour** for an accepted
   `InboundOwnerMessage` — remains Cognition's own, not-yet-scoped
   responsibility, unchanged from `COMMUNICATION_CONTRACT_DESIGN.md`
   Section 9 and gap #53.
6. **Live `PermissionEngine` gating of this channel's own Enable/Disable/Remove**
   — inherited, unresolved scope reduction already recorded for Module
   Registry generally (`IMPLEMENTATION_GAPS.md` #24, #52); neither newly
   introduced nor newly resolved by this document.
7. **Android, networking, wake word, speech, and notifications** — out
   of scope per `COMMUNICATION_CHANNEL_ARCHITECTURE.md`'s own ordered
   future phases (Section 9) and this Unit's explicit instructions.
8. **Any concrete package name, class name, or actual implementation** —
   Stage 3 (Implementation Plan) and Stage 6 (Implementation) territory,
   not this document's.
9. **Whether this channel needs its own `communication.*` observability
   `EventBus` event** — `COMMUNICATION_CHANNEL_ARCHITECTURE.md` Section
   3's own parenthetical already frames this as optional and additive;
   not decided here.
10. **This channel's actual registered `ModuleId` value, and wiring it
    into any running Parker instance** — an implementation-time choice,
    not an architecture-time one (Section 4, above).

---

## Stage 2A Self-Traceability Review

| Contract Element | Authorised by |
| --- | --- |
| `LocalTextChannel` (new interface, Section 1) | `COMMUNICATION_CHANNEL_ARCHITECTURE.md` Section 6 (First Implementation Target), Section 8/11 (deferring the channel's own Kotlin shape to a future Contract Design pass); `COMMUNICATION_CONTRACT_DESIGN.md` Section 11 (identical deferral) |
| One operation only, prose-described, no wrapper return type (Section 1, Section 2) | `COMMUNICATION_CONTRACT_DESIGN.md` Section 6 (`CommunicationIntake`'s own one-operation minimalism, mirrored here) |
| Reuse of `ModuleId` as this channel's sole identity, no `ChannelId` (Section 1, Section 4) | `COMMUNICATION_CONTRACT_DESIGN.md` Section 1 |
| Reuse of `CorrelationId`, `InboundOwnerMessage`, `CommunicationIntakeDisposition` unchanged (Section 2) | `COMMUNICATION_CONTRACT_DESIGN.md` Sections 2, 4, 6 |
| `CommunicationIntake` as the sole downstream collaborator (Section 1) | `COMMUNICATION_CONTRACT_DESIGN.md` Section 6, Section 9 |
| No new validation rule; reuse of existing constructor validation (Section 3) | `COMMUNICATION_CONTRACT_DESIGN.md` Section 2, Section 4 |
| Registration via unmodified `ModuleRegistry.register`/`ModuleDescriptor` (Section 4) | `MODULE_CONTRACT_DESIGN.md` Sections 2, 5 |
| `toolsExposed = emptyList()`, `requiredPermissions = emptyList()` for this Unit (Section 4) | `COMMUNICATION_CONTRACT_DESIGN.md` Section 7 (disclosed tension); `IMPLEMENTATION_GAPS.md` #53 |
| `connectivityDeclaration = LOCAL_ONLY` (Section 4) | `COMMUNICATION_CHANNEL_ARCHITECTURE.md` Section 6 |
| Lifecycle via unmodified `ModuleLifecycleTransitions`; no self-enable (Section 4, Section 9) | `MODULE_CONTRACT_DESIGN.md` Section 4; `MODULE_FRAMEWORK_ARCHITECTURE.md` Section 4, Section 7 |
| `CorrelationId` minted by the channel, once, at receipt (Section 5) | `COMMUNICATION_CONTRACT_DESIGN.md` Section 4 |
| No Principal resolution/authentication performed by this channel (Section 6) | `COMMUNICATION_CONTRACT_DESIGN.md` Section 5, Section 6 |
| Construction-time exceptions vs. structural `Rejected` disposition, no new error type (Section 7) | `COMMUNICATION_CONTRACT_DESIGN.md` Section 6, Section 8 |
| Suspend-capable operation; lock discipline for any retained shared state (Section 8) | PES-001 Chapter 7.1, Chapter 7.2 |
| No permission granted by registration, Enable, or existence (Constitutional Boundaries) | Parker Constitution ("Parker owns authority. Modules provide capability."; "No capability may bypass trust."); `MODULE_FRAMEWORK_ARCHITECTURE.md` Sections 3, 7 |
| Every Deferred item (Section 10) | Named explicitly in this document; none invented or silently assumed |

No contract element in this document introduces a concept none of the
six authoritative sources already anticipated at the architectural
level. Every exclusion in the Minimalism Review is traceable to a named
section above, or to a concrete absence of need identified while
performing this design pass.

---

## Engineering Review

**Architectural consistency.** Every included contract traces to a named
section of `COMMUNICATION_CHANNEL_ARCHITECTURE.md`, `COMMUNICATION_CONTRACT_DESIGN.md`,
`MODULE_FRAMEWORK_ARCHITECTURE.md`, or `MODULE_CONTRACT_DESIGN.md`
(Stage 2A Self-Traceability Review, above). Nothing here reopens any of
the four frozen runtime subsystems, the Module Framework, or
`CommunicationIntake`'s own already-approved and already-implemented
shape.

**Model independence.** No contract in this document assumes a specific
reasoning or model implementation sits behind how owner text is obtained
or interpreted (both remain out of this document's scope entirely,
Section 10), consistent with AD-010.

**Minimalism.** One new contract (`LocalTextChannel`), zero new data
types, eleven candidates considered and excluded for a stated reason
(Contract Minimalism Review, above).

**Traceability.** Every required contract element's authorising section
is named in the Stage 2A Self-Traceability Review.

**Consistency with the Constitution.** No contract here grants the Local
Text Channel authority, self-approval, or a bypass of trust; its entire
reachable surface, per this document, is: register through
`ModuleRegistry` (capability declaration only), and submit an inbound
message through `CommunicationIntake` (a precondition check, not a
permission decision) — both already-governed surfaces, neither
introduced nor altered here.

**Open contract questions.** None remain unresolved at the level this
document is responsible for. Ten items are explicitly deferred (Section
10), each already disclosed as such rather than silently assumed. None of
them blocks this document's own one contract (`LocalTextChannel`) from
being reviewed and, if accepted, carried into a Stage 3 Implementation
Plan scoped to exactly what this document defines.

---

## Conclusion

**This document defines the complete Stage 2A contract for the Local
Text Channel's inbound half: one new interface (`LocalTextChannel`), zero
new data types, explicit module registration field values, explicit
`CorrelationId` ownership, explicit Principal-resolution requirements,
an explicit error model, explicit thread-safety expectations, an
unmodified lifecycle, and ten explicitly named Deferred items.**

Consistent with this Unit's own scope, this document does not implement
anything, does not modify any existing architecture document, and does
not close `IMPLEMENTATION_GAPS.md` #53 or any other gap — outbound
delivery and Cognition's consumption of an accepted message remain
exactly as open as they were before this document was written. Once
reviewed and accepted, this document is the basis for a Stage 3
Implementation Plan scoped to exactly the contract it defines — no
broader, and not before that review has happened.
