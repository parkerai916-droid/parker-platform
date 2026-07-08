# Response Delivery Contract Design

## Status

Design proposal, not yet reviewed or accepted. **This document is contract
design only.** No Kotlin is implemented, proposed as a diff, or changed by
it — every shape below is described in prose, not as a `kotlin`-fenced
signature block. Neither `src/` nor `tests/` is touched.
`IMPLEMENTATION_HISTORY.md` is untouched. `IMPLEMENTATION_GAPS.md` is
untouched — this document closes no gap. No Planner integration, Goal
routing, `PlanCandidate` generation, Workflow Runtime, Android, UI,
Speech, Notifications, persistence, Memory, World Model, retry policy,
queueing, streaming, multi-recipient, multi-channel fan-out, batch
delivery, or model-backed Reasoning Provider behaviour is added. No other
document is modified.

### Why this unit exists

`docs/architecture/COMMUNICATION_CONTRACT_DESIGN.md` Section 7 (Response
Delivery) already specified the outbound mechanism at the concept level —
a Communication Channel's "deliver" `Tool`, reached through an ordinary
`ExecutionRequest` submitted via `ExecutionPipeline` — but explicitly
disclosed one unresolved tension (`ExecutionRequest` has no dedicated
content field) and left the field-level shape of "Response Delivery"
itself undefined, out of that document's own scope. `docs/architecture/ADR-025_RESPONSE_DELIVERY_CONTENT_CARRIER.md`
has since resolved that tension: `ExecutionRequest` is not extended;
`ExecutionRequest.metadata` carries a response's text for a first
implementation. The Sprint 7 Governance Review (accepted) identified
Response Delivery as the highest-value remaining implementation path,
since every other component it depends on — Communication Runtime, Local
Text Channel, Conversation Engine, Reasoning Provider, Execution
Pipeline, Permission Engine, Tool Registry — already exists and is
already tested.

This document performs the field-level Stage 2A design pass Response
Delivery still lacks: it gives "Response Delivery" a concrete Kotlin
shape, built on ADR-025's settled decision, so a future Implementation
Plan builds it against an already-approved contract, never by inventing
one mid-Kotlin — the same relationship every prior Contract Design in
this Sprint bears to its own parent architecture document.

## Review

Reviewed, in authority order:

1. `docs/architecture/parker-constitution.md` — "Cognition proposes.
   Trust authorises. Runtime executes," restated below (Constitutional
   Boundaries) as the fixed point every contract here is checked against.
2. `docs/architecture/PARKER_ENGINEERING_STANDARD.md` (PES-001) — Stage
   2A's own purpose and required content (Chapter 1); Chapter 7's
   in-memory concurrency and policy-seam discipline, for any future
   implementation of this document's contracts.
3. `docs/architecture/ADR-025_RESPONSE_DELIVERY_CONTENT_CARRIER.md` — the
   settled architectural decision this document builds on and does not
   reopen: `ExecutionRequest` is unmodified; `metadata` carries response
   text; the exact metadata key is left to Stage 4.
4. `docs/architecture/COMMUNICATION_CONTRACT_DESIGN.md` — Section 3
   (`OutboundParkerResponse`, already implemented), Section 7 (Response
   Delivery's own mechanism, restated and made concrete here), Section 9
   (Relationship to Planner/Agent/Memory/World Model — restated,
   unchanged).
5. `docs/architecture/LOCAL_TEXT_CHANNEL_CONTRACT_DESIGN.md` — Section 4's
   own deliberate exclusion of a "deliver" `ToolDescriptor` "given
   `COMMUNICATION_CONTRACT_DESIGN.md` Section 7's own disclosed tension";
   Section 10 Deferred Item 4, naming outbound delivery as blocked until
   this exact document exists.
6. `docs/architecture/19-conversation-engine.md` and
   `docs/architecture/CONVERSATION_ENGINE_CONTRACT_DESIGN.md` — confirm
   `Turn`/`Conversation`/`ConversationDisposition` carry no outbound
   concept at all (Conversation Engine's own scope stops at
   `submitTurn`); Response Delivery introduces no dependency on either.
7. `docs/architecture/REASONING_PROVIDER_ARCHITECTURE.md` and
   `docs/architecture/REASONING_PROVIDER_CONTRACT_DESIGN.md` — confirm
   `ReasoningProviderResponse.Reply(text: String)` is "deliberately
   shaped to be directly usable as a future `OutboundParkerResponse.text`...
   once the calling component decides to route it there" — the calling
   component that builds an `OutboundParkerResponse` from a `Reply` is
   explicitly out of this document's own scope (see Out of Scope, below);
   this document begins only once an `OutboundParkerResponse` already
   exists.
8. `docs/adr/ADR-016-core-contracts.md`, `docs/adr/ADR-017-execution-request-is-canonical.md`,
   `docs/adr/ADR-018-immutable-execution-requests.md` — the three ADRs
   ADR-025 already checked this decision against; restated here only
   where Response Delivery's own contract shape touches them directly.
9. `src/interfaces/CommunicationIntake.kt` — `OutboundParkerResponse`,
   `CorrelationId`, already implemented; reused unchanged, not
   re-specified.
10. `src/interfaces/ExecutionPipeline.kt`, `src/contracts/ExecutionRequest.kt`,
    `src/contracts/ExecutionResult.kt`, `src/interfaces/ToolRegistry.kt`,
    `src/runtime/DefaultExecutionPipeline.kt`, `src/interfaces/ResourceRegistry.kt`,
    `src/runtime/InMemoryToolRegistry.kt`, `src/runtime/ActionMapper.kt` —
    the exact, already-implemented mechanics Response Delivery's own
    dispatch depends on, read to confirm every dependency this document
    relies on already exists, not assumed.
11. `docs/architecture/IMPLEMENTATION_GAPS.md` #52 — records, in its own
    words, that a module-exposed Tool's backing `Resource.ownerPrincipalId`
    being set to `PrincipalId(moduleId.value)` is "an interpretive choice,
    not a specified one," by one implementation (`InMemoryModuleRegistry`),
    not an approved architectural decision. This document's own Decision 2
    (below) depends on that convention remaining true; per the Stage 2A
    Contract Review that produced this revision, that dependency is not
    treated as settled architecture here — see "Stage 3 Blocking
    Prerequisites," below.
12. `src/runtime/GatedOutcome.kt` — the already-implemented, already-generic
    "upstream admission gate" type (Sprint 7, Unit C2), reused here as
    `ResponseDelivery`'s own return type (Section 1, below) rather than a
    new `ResponseDeliveryResult`.

---

## Constitutional Boundaries

Restated up front, identical in substance to the Constitution and to
`COMMUNICATION_CONTRACT_DESIGN.md` Sections 5 and 8, not re-derived
differently here:

- **Response Delivery delivers an already-authorised response. It never
  authorises anything itself.** By the time Response Delivery's own
  contract is ever invoked, "Trust authorises" has already happened
  upstream — this contract does not decide whether a response *should*
  be sent, only how an already-decided one reaches its channel.
- **Response Delivery is not a new authority.** It grants no permission,
  self-approves nothing, and holds no private or parallel path to
  action. Every delivery attempt is still an ordinary `ExecutionRequest`,
  evaluated by `PermissionEngine.evaluate`, executed only through
  `ExecutionPipeline` — exactly as any other request, with no exception
  carved out for having originated as a Reasoning Provider's `Reply`.
- **Response Delivery never constructs the content of a response.**
  Deciding *what* to say is Cognition/Reasoning Provider's job, entirely
  upstream and out of this document's scope (Review, above, Item 7).
  Response Delivery's own contract begins only once an
  `OutboundParkerResponse` already exists, fully formed.
- **No Tool may bypass Trust.** A Communication Channel's own "deliver"
  Tool is reached only through `ExecutionPipeline`/`ToolRegistry`,
  exactly like any other Tool (`COMMUNICATION_CONTRACT_DESIGN.md` Section
  7) — this document introduces no second invocation path.

---

## Required Design Decisions

### Decision 1 — Component shape: not interface-backed

**`ResponseDelivery` is a concrete, non-interface-backed class, mirroring
`CommunicationConversationCoordinator`'s and
`ConversationTurnReasoningCoordinator`'s own precedent, not
`CommunicationIntake`'s/`ConversationEngine`'s/`ReasoningProvider`'s.**

Those three interfaces exist because each represents a genuine seam where
more than one plausible implementation exists today or is anticipated (a
different intake policy, a different conversation-continuity strategy, a
different reasoning backend). Response Delivery has no such seam: the
mechanism is already fixed, in full, by `COMMUNICATION_CONTRACT_DESIGN.md`
Section 7 itself — "no new mechanism is required for this half: it is
already fully covered by the Module Framework's existing tool-exposure
model." There is exactly one way to deliver a response under this
platform's existing architecture: build one specific `ExecutionRequest`
shape and submit it through the one existing `ExecutionPipeline`. This is
sequencing of already-shaped components, not a new domain concept
requiring swappability — the same reasoning
`CommunicationConversationCoordinator`'s own Decision 2 already applied.
If a future need for an alternative delivery strategy is ever
demonstrated, that need should be met by a later Contract Design pass
introducing an interface at that point, not by speculatively
interface-backing this one now with no second implementation to justify
it.

**Does Decision 2's Resource-location logic (below) change this?** No.
Unlike a genuine policy seam (`PlanDecision`, `MemoryPromotionPolicy`),
Decision 2's logic is a deterministic, structural infrastructure lookup —
given a `channelId`, find the one Resource it owns of a known type — with
no plausible alternative *algorithm*, only an open question about which
*data* it reads (Stage 3 Blocking Prerequisites, below). A policy seam is
interface-backed because different implementations could reasonably
decide differently given the same input; nothing about "which Resource
does this `ModuleId` own" admits more than one reasonable answer once the
ownership convention itself is settled. `ResponseDelivery` is therefore
better understood as a boundary component in the same sense
`CommunicationIntake` is (Communication-shaped input in, canonical
Execution-shaped output out) but without `CommunicationIntake`'s reason
for being interface-backed (a policy that could plausibly vary) — the
boundary crossing does not, by itself, require interface treatment.

### Decision 2 — Locating the channel's own backing Resource

`COMMUNICATION_CONTRACT_DESIGN.md` Section 7 specifies
`targetResources = the deliver Tool's own backing ResourceId`, but no
existing interface — `ToolRegistry`, `ModuleRegistry`, or
`ResourceRegistry` — exposes a direct lookup from a `ModuleId` (a
response's `channelId`) to the specific `ResourceId` backing that
channel's own exposed Tool. `ToolDescriptor` itself carries no
`resourceId` field (`src/contracts/ToolDescriptor.kt`), and
`ToolRegistry.listAll()`/`findCandidates()` return descriptors only,
never the backing `ResourceId` that registered them
(`src/runtime/InMemoryToolRegistry.kt`'s own `Entry.resourceId` is
private). Deriving a `ResourceId` from `moduleId`/`toolId` by
reconstructing `InMemoryModuleRegistry`'s own internal naming convention
(`"module-tool-<moduleId>-<toolId>"`) was considered and rejected — this
would be exactly the "unauthorised inference of a hidden identifier by
duplicating another subsystem's internal ID-minting scheme" gap #43
already named and rejected for `AgentRunId`.

**Resolution: `ResponseDelivery` locates the channel's backing Resource
via `ResourceRegistry.listByOwner(PrincipalId(response.channelId.value))`,
filtered to `resourceType == ResourceType.TOOL`.** This is a legitimate
reuse of an already-existing, general-purpose `ResourceRegistry` method —
not a new lookup, not a redesign of `ResourceRegistry`, and not a private
ID-minting scheme.

**This resolution is not fully settled, and this document does not treat
it as if it were.** It depends on `InMemoryModuleRegistry` continuing to
set a module-exposed Tool's backing `Resource.ownerPrincipalId` to
`PrincipalId(moduleId.value)` — a convention `IMPLEMENTATION_GAPS.md` #52
itself calls "an interpretive choice, not a specified one," not an
approved architectural decision, and one that gap's own text names as a
candidate for reconciliation once Discovery is designed. Because this is
the single mechanism the rest of this contract's own defined surface
depends on (Section 1, Step 1), this document does not silently rely on
it as approved architecture — see "Stage 3 Blocking Prerequisites,"
below, which this decision is explicitly conditioned on.

Mirroring `ToolRegistry.resolve`'s own zero/one/many discipline (Section
5, below), exactly one matching Resource is required for delivery to
proceed; zero or more than one is a disclosed failure outcome, returned
as `GatedOutcome.NotAccepted` (Section 1, below), not an exception and
not a fabricated `ExecutionResult`.

---

## Contract Minimalism Review — Summary

| Candidate | Determination |
| --- | --- |
| `ResponseDelivery` (new component) | **Include, as a concrete class, not an interface** — Decision 1. The one genuinely new contract this document introduces. |
| `ResponseDeliveryRequest` (new data type) | **Exclude.** `OutboundParkerResponse` (already implemented, `src/interfaces/CommunicationIntake.kt`) already carries every field `ResponseDelivery` needs — `channelId`, `senderPrincipalId`, `text`, `timestamp`, `correlationId`, `metadata`. A second wrapper type would duplicate this structure, not add to it, failing this document's own "could this be replaced by an existing type" test. |
| `ResponseDeliveryResult` (new data type) | **Exclude.** Not `ExecutionResult` alone, and not a new wrapper type either — `GatedOutcome<ExecutionResult>` (already implemented, Sprint 7 Unit C2) is reused instead. `ExecutionResult` alone cannot represent the case where Resource-location fails before any `ExecutionRequest` exists (`ExecutionResult.requestId` is required and non-nullable); `GatedOutcome<T>`'s existing `Produced`/`NotAccepted` shape already models exactly this two-stage outcome, so no new type is required. |
| `GatedOutcome<ExecutionResult>` reuse, in place of a bespoke return type | **Include (reuse, not new).** Already implemented, already generic (Sprint 7 Unit C2); reused here for the identical reason `CommunicationConversationCoordinator` reused it — an upstream gate (Resource-location) that either admits the work, producing an `ExecutionResult`, or rejects it with a reason before any `ExecutionRequest` exists. |
| A new exception/error type | **Exclude.** Construction-time validation (`OutboundParkerResponse`'s own existing constructor validation) and `ExecutionResult.status`/`errors` already cover every failure mode this component can produce (Section 6, below). |
| A dedicated content field on `ExecutionRequest` | **Excluded already, by ADR-025.** Not reopened here. |
| A `ChannelId`-to-`ResourceId` lookup method added to `ToolRegistry` or `ModuleRegistry` | **Exclude — not needed.** `ResourceRegistry.listByOwner`, already implemented, already sufficient (Decision 2). Adding a new interface method would be a redesign of an existing, unmodified contract; this document's own instruction forbids that, and it is not required. |
| A `ResponseDeliveryChannel`/wrapper concept distinct from `ModuleId` | **Exclude.** `COMMUNICATION_CONTRACT_DESIGN.md` Section 1 already settled "no separate `ChannelId` type"; this document does not reopen it. |
| A metadata-key constant/enumeration | **Deferred to Stage 4**, per ADR-025's own explicit deferral of the exact metadata key. Not designed here. |
| A `ResponseDeliveryPolicy`/seam for choosing among delivery strategies | **Exclude.** No second delivery strategy exists or is anticipated (Decision 1); inventing a policy seam with one implementation to plug into it would be the same unvalidated speculative generality this platform's "100,000-line test" discipline already rejects elsewhere. |

Net result: **one new contract** (`ResponseDelivery`, a concrete class),
**zero new data types**, and every other candidate this review considered
is either reused unchanged or explicitly excluded/deferred for a stated,
concrete reason.

---

## 1. Public Contract — `ResponseDelivery`

**One operation, in prose**, mirroring every prior Sprint 7 contract's
minimalism (one component, one operation, described in prose, no
signature block):

Given an already-constructed `OutboundParkerResponse`, an implementation:

1. Locates the response's own channel's backing Resource via
   `ResourceRegistry.listByOwner(PrincipalId(response.channelId.value))`,
   filtered to Resources of type `TOOL` (Decision 2). If the result is not
   exactly one match, delivery stops here: no `ExecutionRequest` is
   constructed, and the operation returns
   `GatedOutcome.NotAccepted(reason)`, with `reason` naming which case
   occurred — no matching Resource, or more than one (Section 5).
2. Constructs one `ExecutionRequest`, per `COMMUNICATION_CONTRACT_DESIGN.md`
   Section 7 and ADR-025:
   - `principalId` = `response.senderPrincipalId`, unchanged — never the
     channel's own identity, never a hardcoded constant.
   - `origin` = `RequestOrigin.TEXT`. Restricted to `TEXT` for this first
     implementation — the only channel this document is aware of (the
     Local Text Channel) is text-only; a future channel of a different
     kind is Deferred (Section 11, Item 5), not designed speculatively
     here.
   - `intent` — a short, non-blank description of the request (e.g. "deliver
     response"), never the response's actual content (ADR-025's own
     distinction between `intent` and payload).
   - `targetResources` = a single-element list containing the `ResourceId`
     of the Resource located in Step 1.
   - `proposedActions` — a single proposed-action string this document
     does not fix (Section 3, below; Stage 3 Blocking Prerequisites,
     below).
   - `correlationId` = `response.correlationId.value`, unchanged.
   - `metadata` — carries `response.text` under a well-known key (Section
     3, below), per ADR-025. `response.metadata` (the
     `OutboundParkerResponse`'s own, separate metadata field) is **not**
     copied into `ExecutionRequest.metadata` by this first implementation
     (Section 3, below).
   - `priority`, `sessionId`, `riskEstimate`, `expiresAt` — left at their
     existing defaults; this document identifies no requirement to set
     any of them.
3. Submits that `ExecutionRequest`, unchanged, to the injected
   `ExecutionPipeline.submit`, and wraps the resulting `ExecutionResult`
   in `GatedOutcome.Produced` — no translation, wrapping, or
   reinterpretation of the `ExecutionResult` itself (Minimalism Review,
   above).

**Return type: `GatedOutcome<ExecutionResult>`, not bare `ExecutionResult`.**
`ExecutionResult.requestId` (`src/contracts/ExecutionResult.kt`) is a
required, non-nullable field. When Step 1 does not find exactly one
matching Resource, no `ExecutionRequest` — and therefore no `RequestId` —
ever exists, so a bare `ExecutionResult` cannot honestly represent that
outcome. `GatedOutcome<T>` (`src/runtime/GatedOutcome.kt`, already
implemented, Sprint 7 Unit C2) already models exactly this shape: admit,
producing one value, or reject with a reason, before the downstream step
ever runs. `GatedOutcome.Produced(executionResult)` is used only when an
`ExecutionRequest` was actually constructed and submitted;
`GatedOutcome.NotAccepted(reason)` covers every case where delivery could
not proceed that far — mirroring `CommunicationConversationCoordinator`'s
own identical two-stage-gate shape (a structural precondition check, then
a downstream call only if it passes).

**Suspend-capable.** This operation must be declared `suspend`, matching
`ExecutionPipeline.submit`'s own shape and PES-001 Chapter 7.2's
"suspend-capable" guidance.

**Dependencies.** An implementation's only two collaborators are
`ResourceRegistry` (Step 1) and `ExecutionPipeline` (Step 3). No
dependency on `ToolRegistry`, `PermissionEngine`, `ModuleRegistry`,
`IdentityService`, `CommunicationIntake`, `ConversationEngine`,
`ReasoningProvider`, `PlannerRuntime`, `AgentRuntime`, `MemoryStore`, or
`WorldModel` is introduced or authorised by this document. `ToolRegistry`
and `PermissionEngine` are reached only indirectly, inside
`ExecutionPipeline.submit`, exactly as they already are for any other
caller — Response Delivery does not call either directly, mirroring
`CommunicationIntake`'s own established pattern of reaching only its
immediate collaborators, never reaching past them into what they
themselves call.

**What this component must not do**, restated from the Constitutional
Boundaries above: construct or reinterpret an `OutboundParkerResponse`'s
own `text`; decide whether a response should be sent; call
`PermissionEngine.evaluate` or `Tool.execute` directly, bypassing
`ExecutionPipeline`; retry, batch, or queue a delivery; or deliver to more
than one channel per call.

## 2. Data Contracts

**No new data contract is introduced.** Every value this document's one
operation handles is an existing, already-approved type:

- `OutboundParkerResponse` (`COMMUNICATION_CONTRACT_DESIGN.md` Section 3,
  already implemented) — the sole input.
- `ExecutionRequest` (Volume 1 core contract, unmodified per ADR-025) —
  constructed once per call, exactly as specified in Section 1, only when
  Step 1 locates exactly one matching Resource.
- `ExecutionResult` (Volume 1 core contract, unmodified) — carried,
  unchanged, inside `GatedOutcome.Produced` when an `ExecutionRequest` was
  actually submitted.
- `GatedOutcome<T>` (`src/runtime/GatedOutcome.kt`, already implemented,
  Sprint 7 Unit C2) — reused, unmodified, as `ResponseDelivery`'s own
  return type, specialised to `GatedOutcome<ExecutionResult>` (Section 1).
- `PrincipalId`, `ModuleId`, `ResourceId`, `RequestId`, `CorrelationId` —
  reused unchanged, exactly as every prior Sprint 7 contract has reused
  them.

This is a deliberate finding, not an oversight — see the Minimalism
Review above for each candidate new data type this document considered
and excluded.

## 3. Interaction with `ExecutionRequest.metadata`

Per ADR-025, `response.text` travels inside `ExecutionRequest.metadata`
under a single, well-known key. **This document names the requirement,
not the exact key string** — consistent with ADR-025's own explicit
statement that "the specific metadata key a 'deliver' Tool implementation
reads... is not fixed here — it is a Stage 4 Implementation Decision."
What this document does settle: exactly one metadata entry carries the
response's full, unmodified `text`; no other field on `ExecutionRequest`
is a substitute location for it; and a future "deliver" Tool
implementation is the sole reader of that entry — `ResponseDelivery`
itself never reads it back, since it only ever writes it once per call
and never inspects the resulting `ExecutionResult.metadata` for content
of its own (Section 6, Ownership Boundaries, below).

**This is not a violation of `metadata`'s established "non-authoritative"
meaning.** Restated from ADR-025: that convention governs Parker's own
trust and control-flow decisions (`PermissionEngine.evaluate` never
inspects `metadata`), not what an already-authorised Tool's own business
logic may read from the one request already addressed to it —
`src/interfaces/Tool.kt` confirms no Tool implementation, present or
future, has any field to read its own operating data from other than
`ExecutionRequest` itself.

**`OutboundParkerResponse.metadata` is not forwarded to
`ExecutionRequest.metadata` in this first implementation.**
`OutboundParkerResponse` carries its own, separate `metadata` field — a
channel-specific, non-authoritative extension point
(`COMMUNICATION_CONTRACT_DESIGN.md` Section 3), distinct from the one
well-known key this section reserves for `response.text`. Two options
were considered: copying `response.metadata`'s entries into
`ExecutionRequest.metadata` under a namespaced prefix (e.g.
`"response.<key>"`, to keep them distinguishable from the response-text
key), or not forwarding them at all in a first implementation. **This
document adopts the second: `response.metadata` is deliberately not
forwarded.** No concrete consumer of a forwarded entry exists anywhere in
this repository today — no "deliver" Tool implementation exists yet
(Section 4) to read one, and this document's own Review found no channel
that currently populates `OutboundParkerResponse.metadata` with anything
at all. Forwarding it now, under an invented namespacing convention, with
no consumer to validate the convention against, is exactly the kind of
unvalidated speculative generality this platform's "100,000-line test"
discipline already rejects elsewhere (Decision 1). If a future channel
demonstrates a real need to carry response-level metadata through to its
own Tool, that need — and the namespacing convention to satisfy it —
should be designed against a real consumer, in a future Contract Design
revision, not invented here.

## 4. Interaction with the Local Text Channel Deliver Tool

**No "deliver" Tool exists yet for the Local Text Channel.**
`LOCAL_TEXT_CHANNEL_CONTRACT_DESIGN.md` Section 4 deliberately registers
`toolsExposed = emptyList()` "given `COMMUNICATION_CONTRACT_DESIGN.md`
Section 7's own disclosed tension" — that tension is now resolved
(ADR-025), but registering the Tool itself is not this document's own
scope; it is a small, additive follow-up to `LOCAL_TEXT_CHANNEL_CONTRACT_DESIGN.md`
(Deferred Item 2, Section 11, below).

This document specifies what that future Tool must be able to do, so a
future unit can register it against an already-settled shape:

- Its `ToolDescriptor.supportedActions` must include whichever
  `PermissionAction` the eventual `proposedActions` string maps to —
  `PermissionAction.NOTIFY` (already defined,
  `src/contracts/Permission.kt`) is the natural, though not mandated,
  candidate, restating `COMMUNICATION_CONTRACT_DESIGN.md` Section 7's own
  language.
- Its `ToolDescriptor.supportedResourceTypes` must include
  `ResourceType.TOOL`, matching its own backing Resource's type
  (Decision 2) — the same convention every other module-exposed Tool
  already follows (`IMPLEMENTATION_GAPS.md` #52).
- Its `Tool.execute(request: ExecutionRequest)` implementation is
  responsible for reading the response text out of `request.metadata`
  (Section 3, above) and performing the channel-specific act of showing
  it to the owner — a Local Text Channel implementation detail entirely
  outside this document's own scope (this document defines the contract
  Response Delivery submits against, not what any specific channel's
  Tool does with it once invoked).

## 5. Locating the Target Resource

Restated in full from Decision 2, with the explicit failure handling a
Contract Design must specify:

- **Zero matching Resources** (no `TOOL`-type Resource owned by
  `PrincipalId(response.channelId.value)`) — the channel has no
  registered deliverable capability. `ResponseDelivery` must not
  construct or submit an `ExecutionRequest` in this case; it returns
  `GatedOutcome.NotAccepted(reason)`, not a thrown exception and not a
  fabricated `ExecutionResult` (Section 1; Section 10, Contract
  Invariants, below), mirroring `ToolResolutionFailureReason.TOOL_NOT_FOUND`'s
  equivalent case one layer up.
- **Exactly one matching Resource** — proceeds as specified in Section 1,
  returning `GatedOutcome.Produced(executionResult)`.
- **More than one matching Resource** — an ambiguous state this document
  does not resolve (mirroring `ToolResolutionFailureReason.TOOL_AMBIGUOUS`'s
  identical shape). `ResponseDelivery` must not guess which Resource is
  the "real" one; it returns `GatedOutcome.NotAccepted(reason)`, the same
  outcome type as the zero-match case, with a `reason` distinguishing the
  two. This is a genuine, disclosed limitation for a future multi-channel
  scenario (Deferred Items, below), not a defect in this document — no
  second channel with a registered deliver Tool exists anywhere in this
  repository today, so this branch is unreachable in the platform's
  current, single-channel state.

## 6. Ownership Boundaries

**Nothing this document defines is owned by `ResponseDelivery`.** It
holds no state between calls (Section 9, below); it does not own a
Conversation, a Turn, a Resource, or an `ExecutionRequest`'s eventual
outcome beyond returning it unchanged. `ExecutionPipeline` continues to
own the full lifecycle of the `ExecutionRequest` it submits, exactly as
it already does for any other caller — `ResponseDelivery` does not
duplicate, track, or second-guess `ExecutionLifecycleState` itself.

## 7. Trust Boundary

Restated, unchanged in substance from `COMMUNICATION_CONTRACT_DESIGN.md`
Section 8, and made concrete against this document's own contract:

- **Locating a Resource (Section 5) is a structural lookup, not a
  permission decision.** Finding zero, one, or many candidate Resources
  never itself grants or denies anything; it only determines whether
  `ResponseDelivery` has enough information to construct a well-formed
  request.
- **Every constructed `ExecutionRequest` is evaluated by
  `PermissionEngine.evaluate`, with no exception.** `ResponseDelivery`
  holds no permission of its own to bypass that evaluation, and this
  document introduces no mechanism by which it could.
- **No Tool is ever invoked directly by `ResponseDelivery`.** The
  addressed channel's own "deliver" Tool is reached only through
  `ExecutionPipeline`/`ToolRegistry`'s existing path — `Tool.execute` is
  never called by this component itself.
- **`ResourceRegistry.listByOwner` has no caller-scoping of its own.**
  This is an existing, already-disclosed platform limitation
  (`IMPLEMENTATION_GAPS.md` #23/#24's identical class of gap for
  `ToolRegistry`'s own discovery surface) that this document inherits,
  not one it introduces — `ResponseDelivery` does not add a new
  unscoped-read capability to the platform, it reuses one that already
  exists in exactly this shape.

## 8. Runtime Boundaries / Non-Responsibilities

Restated explicitly, since this is the section most future maintainers
will need:

- **`ResponseDelivery` does not construct an `OutboundParkerResponse`.**
  Converting a `ReasoningProviderResponse.Reply` into an
  `OutboundParkerResponse` is a future, separately-scoped coordinator's
  job — structurally the same "who builds this" question
  `CommunicationConversationCoordinator`'s own Contract Design and
  Implementation Plan already resolved for the inbound side, and left
  unresolved here for the outbound side, deliberately, per this Unit's
  Out of Scope (below).
- **`ResponseDelivery` does not decide whether to deliver.** That
  decision has already been made by whatever upstream component produced
  a ready `OutboundParkerResponse` in the first place.
- **`ResponseDelivery` does not retry, queue, batch, or fan out.** Exactly
  one `ExecutionRequest` is submitted per call, to exactly one channel,
  regardless of the resulting `ExecutionResult.status`.
- **`ResponseDelivery` is never itself a `Tool`, a `Module`, or a
  `Principal`.** It is an ordinary orchestration component, invoked by
  whatever future caller holds a ready `OutboundParkerResponse` — not a
  registered capability reachable through `ToolRegistry` itself.

## 9. Lifecycle

`ResponseDelivery` has no lifecycle of its own, distinct from the
`ExecutionRequest`s it constructs. Each call to its one operation is
independent; nothing is retained across calls (Contract Invariants,
below). This mirrors `CommunicationConversationCoordinator`'s own
identical "no lifecycle of its own" treatment.

## 10. Contract Invariants

Mirroring the structural-guarantee discipline every Sprint 7 coordinator
has already established:

- **Statelessness.** An implementation holds no field beyond its two
  constructor-injected dependencies (`ResourceRegistry`,
  `ExecutionPipeline`). No `var`, no mutable collection, no cache of any
  prior `OutboundParkerResponse`/`ExecutionRequest`/`ExecutionResult`.
  Each call is fully independent of every other call.
- **Response pass-through.** `response.text`, `response.senderPrincipalId`,
  `response.correlationId`, and `response.channelId` are read, never
  mutated, copied, or reinterpreted. `ResponseDelivery` never constructs
  a new `OutboundParkerResponse`, and never substitutes any of these
  fields' values with anything other than what was supplied.
- **Exactly-once invocation.** Exactly one `ResourceRegistry.listByOwner`
  call and, if a single matching Resource is found, exactly one
  `ExecutionPipeline.submit` call occur per invocation of this
  component's one operation. No retry, loop, or batching behaviour exists
  anywhere in this contract.
- **No exception recovery.** This component must not recover from,
  translate, retry, or suppress an exception thrown by either dependency.
  Such failures propagate unchanged to the caller — never converted into
  a fabricated `ExecutionResult` and never converted into a
  `GatedOutcome.NotAccepted`. `GatedOutcome.NotAccepted` is reserved
  exclusively for the structural Resource-location outcomes Section 5
  names; it is never used to represent a dependency's own thrown
  exception.
- **No authority is ever granted by this component's existence, by a
  successful delivery, or by a `GatedOutcome.NotAccepted` outcome.** A
  `SUCCESS` `ExecutionResult` records that an already-authorised action
  completed; it authorises nothing further. A `GatedOutcome.NotAccepted`
  records only that delivery could not be attempted; it is not, and must
  never be read as, a permission decision.

## 11. Deferred Items

Named, not designed, per this document's own instruction to mark
undecidable items Deferred rather than invent them. Two items formerly
listed here — the `ActionMapper` vocabulary gap and the gap #52
dependency — have been elevated out of this list; see "Stage 3 Blocking
Prerequisites," below, for why they no longer belong among ordinary
Deferred items.

1. **The exact `ExecutionRequest.metadata` key** carrying response text
   (Section 3) — a Stage 4 Implementation Decision, per ADR-025's own
   explicit deferral.
2. **Registering the Local Text Channel's own "deliver" `ToolDescriptor`**
   (Section 4) — a small, additive revision to
   `LOCAL_TEXT_CHANNEL_CONTRACT_DESIGN.md`'s own Section 4 (`toolsExposed`),
   not performed by this document.
3. **Constructing an `OutboundParkerResponse` from a
   `ReasoningProviderResponse.Reply`** (Section 8) — a future,
   separately-scoped coordinator's job, symmetric to the still-undesigned
   Goal → `PlanCandidate` generation problem on the other branch of gap
   #53; not designed here.
4. **Multi-channel dispatch disambiguation** (Section 5) — `ToolRegistry.resolve`'s
   own capability-based dispatch (action + resource type only) cannot
   today distinguish between two `ENABLED` Tools declaring the same
   action on `ResourceType.TOOL`, and `ResourceRegistry.listByOwner`'s
   own "more than one match" case is left as a non-proceeding, disclosed
   limitation (Section 5). Safe for today's single-channel state; a
   future Contract Design revision, once a second channel with a deliver
   Tool actually exists, must resolve it — not designed speculatively
   here, per this platform's own "100,000-line test" discipline.
5. **A channel of a kind other than the Local Text Channel** (Section 1)
   — this first implementation restricts `origin` to `RequestOrigin.TEXT`
   only, since the Local Text Channel is the only channel that exists
   today. Whether a future speech or other non-text channel should use
   `.VOICE` or a different value is not decided here, and is not named
   speculatively — a future Contract Design revision, made once such a
   channel actually exists, is the right place to decide it.
6. **Whether a `response_delivered`/`response_delivery_failed` observability
   `EventBus` event should exist** — `COMMUNICATION_CHANNEL_ARCHITECTURE.md`
   Section 3's own parenthetical already frames this class of event as
   optional and additive; not decided here. `DefaultExecutionPipeline`
   already publishes its own `execution.*` lifecycle events for every
   `ExecutionRequest`, including one constructed by `ResponseDelivery`, so
   the underlying action is already observable without any addition.

## Stage 3 Blocking Prerequisites

Unlike the Deferred Items above — each safely postponable without
affecting whether this contract's own defined surface is soundly
grounded — the two items below are **not** ordinary deferrals. Both are
load-bearing for the single mechanism this entire contract depends on
(Section 1, Step 1), and this document does not treat either as settled.
**A Stage 3 Implementation Plan for `ResponseDelivery` must not be
authorised until both are resolved.**

1. **The `Resource.ownerPrincipalId = PrincipalId(moduleId.value)`
   convention (Decision 2) is not approved architecture.**
   `IMPLEMENTATION_GAPS.md` #52 itself states this is "an interpretive
   choice, not a specified one," made by one implementation
   (`InMemoryModuleRegistry`), and separately names it as a candidate for
   reconciliation once Discovery is designed. This document's Resource-
   location mechanism (Decision 2, Section 5) has no other way to find a
   channel's backing Resource today (Decision 2's own review of, and
   rejection of, the alternatives), which makes this convention the
   single most load-bearing dependency in the entire document — not a
   peripheral concern. **Before a Stage 3 Implementation Plan may be
   authorised, this convention must be formally settled** — either
   accepted as-is by a short ADR or an explicit gap #52 closure, or
   replaced by whichever mechanism that resolution produces instead. This
   document's own Decision 2 is conditioned on that resolution matching
   what is written here; if it does not, Decision 2 itself must be
   revisited before implementation, not patched around inside Kotlin.
2. **No `ActionVocabularyEntry` mapping to `PermissionAction.NOTIFY` (or
   any other action) exists anywhere in this repository today**
   (`src/runtime/ActionMapper.kt`, `InMemoryActionVocabulary`, confirmed
   by direct inspection). Without one, `ActionMapper.map` returns
   `ActionMappingResult.Failed(UNKNOWN_ACTION)` for any `proposedActions`
   string `ResponseDelivery` supplies, and `DefaultExecutionPipeline.submit`
   fails the request before it ever reaches `PermissionEngine.evaluate`
   or `ToolRegistry.resolve` — the entire mechanism this contract defines
   would be inert on arrival. **A Stage 3 Implementation Plan for
   `ResponseDelivery` must include, as Included Work, registering one
   `ActionVocabularyEntry`** mapping a chosen `proposedActions` string to
   `PermissionAction.NOTIFY` (an additive `ActionVocabulary.register`
   call, using that interface exactly as it already exists — not a
   redesign of `ActionMapper`). This does not block accepting this
   Contract Design itself, but must be satisfied before Stage 3
   Implementation begins, not discovered mid-implementation.

Neither item requires reopening this document's own scope or any
existing contract. Both are prerequisites to be satisfied — by a short,
separately-scoped decision in the first case, and by inclusion in a
future Implementation Plan's own Included Work in the second — before
Stage 3 begins, not defects in this Contract Design itself.

## 12. Out of Scope

Restated explicitly, matching this Unit's own instructions in full:

Planner integration, Goal routing, `PlanCandidate` generation, Workflow
Runtime, Android, UI, Speech, Notifications, persistence, Memory writes,
World Model writes, retry policy, queueing, streaming responses, multiple
recipients, multi-channel fan-out, batch delivery, and any model-backed
Reasoning Provider behaviour. Also out of scope, restated from Section 8:
constructing an `OutboundParkerResponse` from a `Reply`; deciding whether
a response should be sent; and any redesign of `ExecutionRequest`,
`ExecutionPipeline`, `ToolRegistry`, `PermissionEngine`, `ConversationEngine`,
`ReasoningProvider`, or `PlannerRuntime` — every one of these is reused
exactly as it already exists.

---

## Public Contract Inventory

| Contract | New / Reused | Purpose |
| --- | --- | --- |
| `ResponseDelivery` | **New** (concrete class, not interface) | Delivers an already-authorised `OutboundParkerResponse` via `ExecutionPipeline`, returning `GatedOutcome<ExecutionResult>` (Section 1). |
| `OutboundParkerResponse` | Reused, unmodified | The sole input; already implemented (`COMMUNICATION_CONTRACT_DESIGN.md` Section 3). |
| `ExecutionRequest` | Reused, unmodified (ADR-025) | Constructed once per call, only if Section 5's Resource-location succeeds; carries response text in `metadata` (Section 3). |
| `ExecutionResult` | Reused, unmodified | Carried unchanged inside `GatedOutcome.Produced` (Section 1, Minimalism Review). |
| `GatedOutcome<T>` | Reused, unmodified | `ResponseDelivery`'s own return type, specialised to `ExecutionResult` (Section 1, Minimalism Review). |
| `ResourceRegistry` / `listByOwner` | Reused, unmodified | Locates the channel's own backing Resource (Decision 2, Section 5; Stage 3 Blocking Prerequisite 1). |
| `ExecutionPipeline` / `submit` | Reused, unmodified | The sole invocation path (Section 1, Section 7). |
| `ToolRegistry` / `PermissionEngine` | Reused, unmodified, indirect only | Reached only inside `ExecutionPipeline.submit`, never called directly by `ResponseDelivery` (Section 1). |
| `PermissionAction.NOTIFY` | Reused, unmodified | The natural candidate action for a deliver Tool to declare (Section 4; Stage 3 Blocking Prerequisite 2). |
| `ModuleId` / `PrincipalId` / `ResourceId` / `CorrelationId` | Reused, unmodified | Identity types, unchanged (Section 2). |

**One new contract, nine existing contracts reused unchanged, zero
modified.**

## Self-Traceability Review

| Contract Element | Authorised by |
| --- | --- |
| `ResponseDelivery` exists at all (Section 1) | `COMMUNICATION_CONTRACT_DESIGN.md` Section 7; `LOCAL_TEXT_CHANNEL_CONTRACT_DESIGN.md` Section 10 Deferred Item 4 |
| Not interface-backed (Decision 1) | Precedent: `CommunicationConversationCoordinator`/`ConversationTurnReasoningCoordinator` Decision 2; boundary-vs-policy-seam distinction (Decision 1, revised) |
| `OutboundParkerResponse` reused as the sole input, no `ResponseDeliveryRequest` (Minimalism Review, Section 2) | `COMMUNICATION_CONTRACT_DESIGN.md` Section 3 |
| `GatedOutcome<ExecutionResult>` reused as the return type, no `ResponseDeliveryResult` (Minimalism Review, Section 1, Section 2) | `src/runtime/GatedOutcome.kt` (already implemented, Sprint 7 Unit C2) |
| `ExecutionRequest.metadata` carries response text; `response.metadata` not forwarded (Section 3) | `ADR-025_RESPONSE_DELIVERY_CONTENT_CARRIER.md` |
| `targetResources` located via `ResourceRegistry.listByOwner` (Decision 2, Section 5) — **conditioned on Stage 3 Blocking Prerequisite 1** | `src/interfaces/ResourceRegistry.kt` (already implemented); `IMPLEMENTATION_GAPS.md` #52 (explicitly *not* approved architecture — see Stage 3 Blocking Prerequisites) |
| Delivery reached only through `ExecutionPipeline`/`ToolRegistry`, never directly (Section 1, Section 7) | `COMMUNICATION_CONTRACT_DESIGN.md` Section 7, Section 8 |
| `PermissionAction.NOTIFY` as the candidate action (Section 4) — **conditioned on Stage 3 Blocking Prerequisite 2** | `COMMUNICATION_CONTRACT_DESIGN.md` Section 7; `src/runtime/ActionMapper.kt` (confirmed no matching vocabulary entry exists) |
| No `OutboundParkerResponse` construction (Section 8) | `COMMUNICATION_CONTRACT_DESIGN.md` Section 9 ("Cognition, or a future Conversation Engine") |
| Every Deferred item (Section 11) and every Stage 3 Blocking Prerequisite | Named explicitly in this document; none invented or silently assumed |

No contract element in this document introduces a concept none of the
authoritative sources above already anticipated at the architectural
level.

---

## Engineering Review

Answering this Unit's own required review questions directly:

- **Does any responsibility overlap another component?** No.
  `ResponseDelivery` does not duplicate `ExecutionPipeline`'s lifecycle
  tracking, `PermissionEngine`'s evaluation, `ToolRegistry`'s resolution,
  or `ResourceRegistry`'s own storage — it calls each exactly once, for
  exactly the purpose each already exists to serve.
- **Does Response Delivery gain authority it should not have?** No. It
  holds no permission of its own; every constructed request is evaluated
  independently, with no shortcut (Section 7).
- **Can any Tool bypass Trust?** No. The only invocation path remains
  `ExecutionPipeline.submit` → `PermissionEngine.evaluate` →
  `ToolRegistry.resolve` → `Tool.execute`, unchanged.
- **Is every new type genuinely required?** Only one new type exists —
  `ResponseDelivery` itself, and it is a concrete class, not even a new
  interface (Decision 1). Every other candidate type was excluded
  (Minimalism Review).
- **Could any new type be replaced by an existing one?** Applied and
  answered twice: `ResponseDeliveryRequest` replaced by
  `OutboundParkerResponse`; `ResponseDeliveryResult` replaced not by bare
  `ExecutionResult` (which cannot represent every outcome this contract
  needs) but by `GatedOutcome<ExecutionResult>` — an existing, already-
  generic type, not a new one.
- **Does every dependency already exist, in the sense of being usable
  without further authorisation?** Partially. `ResourceRegistry` and
  `ExecutionPipeline` themselves, yes — both already implemented and
  tested; no new interface is introduced. But two concrete prerequisites
  are not yet satisfied and are not treated as though they were: the
  `Resource.ownerPrincipalId` convention Decision 2 depends on is
  explicitly disclosed, by its own governing gap, as unapproved (Stage 3
  Blocking Prerequisite 1); and no `PermissionAction.NOTIFY` vocabulary
  entry exists yet for `ActionMapper` to resolve (Stage 3 Blocking
  Prerequisite 2). Both are named as blocking, not folded into the
  ordinary Deferred Items list.
- **Is any future capability being designed before a real consumer
  exists?** No. Multi-channel dispatch (Deferred Item 4) is explicitly
  named and explicitly *not* designed, precisely because no second
  channel exists yet to validate a design against.

No answer above required simplifying the design further than the
Minimalism Review had already reduced it.

## Conclusion

**This document defines the complete Stage 2A contract for Response
Delivery's first constitutional capability: one new component
(`ResponseDelivery`, deliberately not interface-backed, returning
`GatedOutcome<ExecutionResult>` rather than a new result type), zero new
data types, an explicit mechanism for locating a channel's own backing
Resource through an already-existing `ResourceRegistry` method, an
explicit decision not to forward `OutboundParkerResponse.metadata` in
this first implementation, explicit interaction with
`ExecutionRequest.metadata` per ADR-025, explicit interaction with the
(not-yet-registered) Local Text Channel deliver Tool, explicit ownership
and trust boundaries, five structural invariants, six explicitly named
Deferred items, and two explicitly named Stage 3 Blocking Prerequisites.**

Consistent with this Unit's own scope, this document does not implement
anything, does not modify any existing architecture, contract design, or
ADR document, and does not close `IMPLEMENTATION_GAPS.md` #53 — Goal
routing, `OutboundParkerResponse` construction from a `Reply`, and every
other item named Out of Scope remain exactly as open as they were before
this document was written. **Unlike a document with only Deferred Items,
this one is not yet ready for a Stage 3 Implementation Plan to be
authorised against it** — the two Stage 3 Blocking Prerequisites above
must be resolved first (a short, separately-scoped decision for the
first; inclusion in a future Plan's own Included Work for the second).
Once those are satisfied and this document is reviewed and accepted, it
is the basis for a Stage 3 Implementation Plan scoped to exactly the
contract it defines — no broader, and not before both that review and
those prerequisites have happened.
