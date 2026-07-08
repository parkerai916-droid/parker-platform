# Conversation Engine Contract Design

## Status

Design proposal, not yet reviewed or accepted. **This document is
contract design only.** No Kotlin is implemented, proposed as a diff, or
changed by it — every shape below is described in prose, not as a
`kotlin`-fenced signature block. Neither `src/` nor `tests/` is touched.
No implementation plan, no test, no reasoning-provider design, no
Response Delivery mechanism, no persistence decision, and no Android,
UI, speech, or LLM-prompt concept is introduced.

### Why this unit exists

`docs/architecture/19-conversation-engine.md` ("Conversation Engine
Architecture") received Stage 1 Architecture Review and was accepted —
purpose, responsibilities, non-responsibilities, ownership boundary,
trust boundary, a conceptual lifecycle, and ten explicitly named open
items, but deliberately no field-level Kotlin shape (its own Section 13,
"Open Questions and Deferred Items," names the exact mechanism questions
this document now begins to answer). This document performs the
field-level design pass that document deferred — exactly the same
relationship `COMMUNICATION_CONTRACT_DESIGN.md` bears to
`COMMUNICATION_CHANNEL_ARCHITECTURE.md`, and `MODULE_CONTRACT_DESIGN.md`
bears to `MODULE_FRAMEWORK_ARCHITECTURE.md`.

**Scope, following Architecture Section 14's own recommendation.**
Architecture Section 14 (Implementation Sequencing) predicted that "the
most immediately achievable first Contract Design pass is likely the
Conversation Engine's inbound half only — consuming an accepted
`InboundOwnerMessage` and maintaining Conversation/Turn continuity state
— deferring the reasoning-provider engagement and Response Delivery
routing halves," because Architecture Section 13 Item 6 had already
found that no reasoning-provider Kotlin contract exists anywhere in this
repository (`ModelManager.kt` remains excluded from the build; no
interface for a reasoning provider exists in `src/interfaces`). This
document follows that recommendation exactly: it defines the inbound
consumption and continuity-binding boundary in full, and defers
everything downstream of it that depends on a reasoning provider's own,
not-yet-scoped contract.

## Review

Reviewed, in authority order:

1. `docs/architecture/parker-constitution.md`.
2. `docs/architecture/PARKER_ENGINEERING_STANDARD.md` (PES-001) — Stage
   2A's own definition (line 223 onward): a Contract Design document
   "reviews the existing stub or prior art without assuming it is
   correct," "determines the minimum required set of public contracts,
   explicitly stating what is required, what is excluded, and what is
   deferred, and why," "resolves named outstanding design questions
   against approved architecture, never inventing new architecture to
   answer them," "states whether a separate 'Runtime' wrapper interface
   is needed, or whether one interface already suffices," and "ends with
   a Self-Traceability Review."
3. `docs/architecture/19-conversation-engine.md` — the Stage 1
   Architecture this document implements as contracts and does not
   redefine, redesign, or revise.
4. `docs/architecture/ARCHITECTURE_V2_FROZEN_BASELINE.md` — Section 2's
   six frozen runtime subsystems (Identity Runtime, EventBus, Planner
   Runtime, Agent Runtime, Memory Runtime, World Model Runtime) this
   document builds on top of, not into; confirms the Conversation Engine
   is not a seventh peer among them (Architecture Section 4).
5. `docs/architecture/COMMUNICATION_CONTRACT_DESIGN.md` — the direct
   upstream contract set this document consumes without modification:
   `InboundOwnerMessage`, `OutboundParkerResponse`, `CorrelationId`,
   `CommunicationIntake`, `CommunicationIntakeDisposition`. This
   document's own structure (Status, Review, Constitutional Boundaries,
   Contract Minimalism Review, numbered sections, Self-Traceability
   Review) mirrors that document's, and `LOCAL_TEXT_CHANNEL_CONTRACT_DESIGN.md`'s,
   established house style.
6. `docs/architecture/MODULE_CONTRACT_DESIGN.md` — Section 1's `ModuleId`
   shape, reused unchanged for `channelId` wherever it appears below; no
   new identifier is introduced for a concept `ModuleId` already covers.
7. `docs/architecture/IMPLEMENTATION_GAPS.md` — Gap #53 (the gap this
   document is a prerequisite step toward closing, closure path (b) only
   — see Deferred Items); Gap #49 (the resolved-Principal-identity
   discipline this document's Trust Boundary and Dependencies sections
   restate); Gap #51 (the open persistence/durability boundary question
   this document's own persistence deferral ties back to, rather than
   re-litigating).
8. `src/interfaces/CommunicationIntake.kt` — the direct, already-
   implemented Kotlin precedent for this document's own interface
   minimalism (`CommunicationIntake`'s "one method, one payload type, one
   outcome type" shape) and identifier style (`@JvmInline value class`,
   blank-rejecting `init` block, matching `CorrelationId`/`PrincipalId`/
   `ModuleId`).
9. `src/contracts/PlanDecision.kt` (`PlanningRequest`) — confirms
   `PlanningRequest`'s existing shape (`planningSessionId`,
   `initiatingPrincipalId`, `goal: String`, `correlationId: String`,
   `source`, `priority`) requires no change to support a
   Conversation-Engine-originated planning request, exactly as
   Architecture Section 8 and `COMMUNICATION_CONTRACT_DESIGN.md` Section
   9 already established.
10. `src/contracts/TaskProposal.kt` (`TaskProposalIntake`) — the original
    structural precedent both `CommunicationIntake` and this document's
    own `ConversationEngine` interface mirror: one method, one payload
    type, one outcome type.

---

## Constitutional Boundaries

Restated up front, identical in substance to
`docs/architecture/19-conversation-engine.md` Sections 3, 5, and 12, not
re-derived differently here:

- **The Conversation Engine coordinates messages. It does not decide
  whether Parker acts, and it does not interpret what a message means.**
  Interpretation is a reasoning provider's job, entirely deferred by this
  document (Section 8, below).
- **The Conversation Engine is an ordinary component operating under a
  resolvable Principal identity, never anonymously, and never
  substituting the message sender's identity for its own.** No contract
  below grants it implicit trust, self-approval, or a bypass of
  Identity/Permission evaluation.
- **No Task Proposal may originate from a conversational exchange except
  through the Conversation Engine's own routing** (Architecture Section
  5) — a strictly one-way dependency this document's own minimalism
  preserves by introducing no second entry point into `PlannerRuntime.plan`.
- **Cognition proposes. Trust authorises. Runtime executes — with no
  shortcut for messages that arrived via a Conversation.** Nothing this
  document defines calls `ExecutionPipeline`, `PermissionEngine.evaluate`,
  or a reasoning provider directly; see Section 6 (Trust Boundary) and
  Section 7 (Dependencies).

---

## Contract Minimalism Review — Summary

| Candidate | Determination |
| --- | --- |
| `ConversationId` | **Include.** The one genuinely new identifier a Conversation requires — see Section 2. |
| `TurnId` | **Include.** The one genuinely new identifier a Turn requires — see Section 2. |
| `Conversation` | **Include.** The continuity record Architecture Section 4 names as the Conversation Engine's sole owned state. |
| `Turn` | **Include.** The bounded per-message record Architecture Section 6 names. |
| `ConversationDisposition` | **Include, as a plain (non-sealed) result type** — see Section 2 and Section 9 for why a sealed, multi-variant shape (mirroring `CommunicationIntakeDisposition`) is unnecessary here. |
| `ConversationResult` | **Exclude.** Would represent the outcome of a Turn's full processing (reasoning plus routing plus completion) — a shape that cannot be honestly specified until the reasoning-provider contract this document defers actually exists. See Section 9. |
| A `Rejected` variant on `ConversationDisposition` | **Exclude.** See Section 9. |
| A `TurnStatus` / `ConversationStatus` enum | **Exclude from this pass.** See Section 9. |
| A `completeTurn`-shaped second operation | **Exclude from this pass.** See Section 9. |
| A separate `ConversationEngineRuntime` wrapper interface | **Exclude — one interface suffices.** See Section 1. |
| A new `ChannelId` type | **Exclude — reuse `ModuleId`,** mirroring `COMMUNICATION_CONTRACT_DESIGN.md` Section 1's identical precedent. |
| A reasoning-provider invocation contract | **Exclude — deferred in full.** See Section 8. |

Net result: **five new contracts** (`ConversationId`, `TurnId`,
`Conversation`, `Turn`, `ConversationDisposition`), **one new interface**
(`ConversationEngine`, one operation), **five existing contracts reused
unchanged** (`InboundOwnerMessage`, `CorrelationId`, `ModuleId`,
`PrincipalId`, `PlanningRequest` — the last named only to confirm it
requires no change, not because this pass calls it), **zero modified**,
and **one deferred class of contract in full** (a reasoning-provider
contract, Section 8).

---

## 1. Public Conversation Engine Contract

**`ConversationEngine`** — the single public interface for Turn
consumption and Conversation continuity binding, mirroring
`CommunicationIntake`'s exact shape and minimalism (one method, one
payload type, one outcome type):

- **One operation:** given an `InboundOwnerMessage` already accepted by
  `CommunicationIntake`, determine whether it continues an existing
  Conversation or begins a new one, create a Turn bound to that
  Conversation, and return a `ConversationDisposition` describing the
  result.
- **Its responsibility ends there.** This operation does not engage a
  reasoning provider, does not construct a `PlanningRequest`, does not
  construct an `OutboundParkerResponse`, and does not call
  `PlannerRuntime`, `ExecutionPipeline`, `PermissionEngine`,
  `MemoryStore`, or `WorldModel`. Restating Architecture Section 2 and
  Section 13 Item 6: engaging a reasoning provider and routing its
  resulting proposal remain the Conversation Engine's architectural
  responsibilities in principle, but this Contract Design pass cannot
  give either a Kotlin shape yet, because no reasoning-provider contract
  exists anywhere in this repository to engage. That gap is named in
  full in Section 8, not silently narrowed here.
- **No separate "Runtime" wrapper interface is needed.** Per PES-001
  Stage 2A's own required determination: exactly one interface,
  `ConversationEngine`, suffices for everything this pass defines —
  mirroring `CommunicationIntake`'s identical one-interface precedent,
  and unlike `PlannerRuntime`/`AgentRuntime`, which expose multiple
  operations across a session or run's full lifecycle. If a future
  Contract Design pass (once the reasoning-provider contract exists)
  finds a second operation is genuinely needed, it is added additively to
  this same interface or a clearly-justified second one — not
  speculatively reserved here.

**What this interface's precondition is.** Its input must already be an
`InboundOwnerMessage` for which `CommunicationIntake.submitInboundMessage`
returned `CommunicationIntakeDisposition.Accepted` (`COMMUNICATION_CONTRACT_DESIGN.md`
Section 6). This interface does not repeat, second-guess, or bypass
`CommunicationIntake`'s own two structural checks — restating
Architecture Section 7 at the contract level.

## 2. Core Contract Types

Only the minimum set of new public types this pass requires, all reusing
existing identifier and record shapes wherever this repository already
has one.

**`ConversationId`** — a single, non-blank string value, matching
`PrincipalId`/`ModuleId`/`CorrelationId`'s identical established shape
(`@JvmInline value class`, blank-rejecting `init` block). The one
genuinely new identifier a Conversation requires — restating Architecture
Section 13 Item 3: **`ConversationId` is distinct from `CorrelationId`.**
`CorrelationId` ties one inbound message to whatever eventually answers
it, scoped to a single Turn; `ConversationId` groups many Turns — each
still carrying its own `CorrelationId` — under one continuing exchange.
This document does not mandate how a `ConversationId` is generated, only
that it exists and is stable for the life of a Conversation, mirroring
`CorrelationId`'s own identical "no mandated generation algorithm"
precedent.

**`TurnId`** — a single, non-blank string value, same shape as
`ConversationId`. The one genuinely new identifier a Turn requires.

**`Conversation`** — the continuity record Architecture Section 4 names
as the Conversation Engine's sole owned state:

- **`conversationId: ConversationId`**
- **`ownerPrincipalId: PrincipalId`** — reused, unchanged — the owner
  whose `InboundOwnerMessage.senderPrincipalId` began this Conversation.
  Restating Architecture Section 5: never the Conversation Engine's own
  operating identity.
- **`channelId: ModuleId`** — reused, unchanged, mirroring
  `COMMUNICATION_CONTRACT_DESIGN.md` Section 1's identical "no separate
  `ChannelId` type" precedent — which Communication Channel this
  Conversation is, so far, associated with. Architecture Section 13 Item
  5 (whether a Conversation may span more than one channel) is not
  decided here; this field records the channel of the Conversation's
  first Turn and this document does not specify whether or how it may
  change.
- **`turnIds: List<TurnId>`** — which Turns belong to this Conversation,
  restating Architecture Section 4's own ownership definition verbatim.
  This is a field shape, not a storage decision — how (or whether) this
  list is persisted beyond process memory is Section 8's own deferred
  concern, tied to Gap #51.

**`Turn`** — the bounded per-message record Architecture Section 6
names:

- **`turnId: TurnId`**
- **`conversationId: ConversationId`** — which Conversation this Turn
  belongs to.
- **`message: InboundOwnerMessage`** — reused, unchanged — the inbound
  message this Turn was created for. One Turn, one `InboundOwnerMessage`,
  restating Architecture Section 2's "each Turn being one inbound message"
  definition.
- **`receivedAt: Instant`** — when this Turn was created, mirroring
  `InboundOwnerMessage.timestamp`'s own established shape rather than
  inventing a different time representation.

**No `status` field is included on either type in this pass.** See
Section 9 for why a `TurnStatus`/`ConversationStatus` enum is excluded
here rather than included with an undefined transition mechanism.

**`ConversationDisposition`** — the outcome of `ConversationEngine`'s one
operation, kept as a **plain data class, not a sealed type**, unlike
`CommunicationIntakeDisposition`'s two-variant sealed shape — see Section
9 for why no second (rejection) variant is needed:

- **`conversation: Conversation`** — the resulting Conversation, whether
  newly created or continued, in its state immediately after this Turn
  was bound.
- **`turn: Turn`** — the newly created Turn.
- **`isNewConversation: Boolean`** — whether this call began a new
  Conversation or continued an existing one, satisfying Section 3's own
  requirement to define "Conversation creation" and "Conversation
  continuation" as observably distinct lifecycle outcomes, without
  specifying the recognition algorithm that decided between them
  (Architecture Section 13 Item 3, still open).

  **Why this field is justified rather than redundant.** The question is
  whether it describes something intrinsic to the contract or something
  transient about one invocation. It is intrinsic: `conversation` and
  `turn` alone do not let a caller determine which branch occurred,
  because the caller supplies no `ConversationId` on the way in (Section
  4) — recognition is performed entirely inside this interface (Section
  1), owned state Architecture Section 4 assigns to the Conversation
  Engine alone. A caller could only recover this fact by independently
  tracking every `ConversationId` it has previously seen and diffing
  against it — duplicating, outside this interface, exactly the lookup
  this interface exists to own. `isNewConversation` reports an
  architecturally-named lifecycle distinction (Section 3, Stages 1–2)
  that would otherwise be unobservable, not incidental detail about how
  one call happened to resolve.

**Not redefined, reused exactly as they already exist:** `CorrelationId`,
`ModuleId`, `PrincipalId`, `PlanningRequest`, `InboundOwnerMessage` — per
this document's own explicit scope instruction, and because nothing in
this pass's minimal surface requires a change to any of them.

## 3. Lifecycle

At the contract level only — no algorithm, no storage decision, mirroring
Architecture Section 6's own conceptual (non-Kotlin) framing, now named
against the types in Section 2. **These five stages describe conceptual
progression only and do not imply a single implementation method, a
single state machine, or any particular internal structure — a future
implementation is free to realise them however it chooses, so long as
the observable outcomes below hold.**

1. **Conversation creation.** `ConversationEngine`'s one operation is
   called with an already-accepted `InboundOwnerMessage` for which no
   continuing Conversation is found (the recognition rule itself remains
   Architecture Section 13 Item 3, not decided here). A new
   `ConversationId` is assigned; the resulting `Conversation` has exactly
   one `TurnId` in `turnIds`. `ConversationDisposition.isNewConversation`
   is `true`.
2. **Conversation continuation.** The same operation is called with an
   `InboundOwnerMessage` for which a continuing Conversation *is* found.
   The existing `Conversation`'s `turnIds` gains one more entry.
   `ConversationDisposition.isNewConversation` is `false`.
3. **Turn creation.** In both cases above, exactly one new `Turn` is
   created, bound to the resulting `Conversation`'s `conversationId`, and
   returned as part of the same `ConversationDisposition`. A Turn is
   never created except as part of this one operation; there is no
   separate "create a Turn" operation.
4. **Turn completion.** Restating Architecture Section 6 Step 4
   conceptually: a Turn concludes once a response has been delivered, a
   Task Proposal has been submitted, or a reasoning provider determines no
   action is warranted. **This document does not define a Kotlin
   mechanism, field, or operation that marks a Turn as completed.** Doing
   so honestly requires knowing how and when a reasoning provider
   concludes its engagement with a Turn — which depends entirely on the
   reasoning-provider contract this document defers in full (Section 8).
   A `Turn`, as defined in Section 2, has no field recording completion;
   a future Contract Design pass, once that dependency is resolved, is
   where this is added.
5. **Conversation completion.** Restating Architecture Section 6 Step 5
   and Section 13 Item 4 conceptually: a Conversation continues, goes
   idle, or ends. **This document does not define the idle/termination
   rule, or any Kotlin mechanism that marks a Conversation as ended** —
   Architecture Section 13 Item 4 already named this as undecided, and
   this document does not narrow it. A `Conversation`, as defined in
   Section 2, has no field recording this either.

No Conversation, and no Turn, produced by this document's one operation
ever carries a status implying authorisation — restating Architecture
Section 6's own closing invariant: neither is, or ever becomes, a
substitute for a Task Proposal, a Plan Decision, or a Permission
Decision.

## 4. Inputs

Exactly one input enters the Conversation Engine through the surface this
document defines, aligning directly with Chapter 19 Section 7
("Relationship to `CommunicationIntake`") and Section 2 ("Receive
accepted `InboundOwnerMessage` instances downstream of
`CommunicationIntake`"):

- **`InboundOwnerMessage`** (reused, unchanged, `COMMUNICATION_CONTRACT_DESIGN.md`
  Section 2) — already accepted by `CommunicationIntake`. No other input
  enters through this document's one operation.

**What does not enter here.** A reasoning provider's interpretation, a
goal string, or any content derived from interpreting `message.text` is
not an input this document's surface accepts — restating Architecture
Section 1: interpretation is a reasoning provider's job, and this
document defines no path for its output to re-enter the Conversation
Engine, because no reasoning-provider contract exists to produce one
(Section 8).

## 5. Outputs

**The only output this document's one operation produces is its return
value: a `ConversationDisposition` (Section 2).** This is data, not an
execution path — `ConversationEngine.submitTurn` (or equivalently named
operation) calls no other subsystem and triggers no side effect outside
its own Conversation state.

**No new execution path is introduced.** Restating this document's own
scope instruction: Conversation Engine may only produce outputs already
authorised by the architecture. Two further outputs are authorised by
`docs/architecture/19-conversation-engine.md` Section 2 and Section 8 —

- an ordinary `PlanningRequest` (reused, unchanged) submitted to
  `PlannerRuntime.plan`, exactly as `COMMUNICATION_CONTRACT_DESIGN.md`
  Section 9 already authorises for any proposer, and
- an ordinary `OutboundParkerResponse` (reused, unchanged) delivered
  through the originating channel's exposed "deliver" Tool, exactly as
  `COMMUNICATION_CONTRACT_DESIGN.md` Section 7 already authorises —

but **neither is produced by any operation this document defines.**
Both depend on a reasoning provider having first determined a Turn's
outcome (a goal worth planning, or a reply worth sending), and that
determination is exactly the reasoning-provider contract this document
defers in full (Section 8). Naming these two outputs here is not
introducing a new execution path — both already exist and are already
authorised — it is confirming that this document invents no third path
alongside them, and that this pass's own operation does not yet reach
either.

## 6. Trust Boundary

Restated, unchanged in substance from `docs/architecture/19-conversation-engine.md`
Section 5, and made concrete against the one operation this document
defines:

- **`ConversationEngine`'s one operation never authorises anything.**
  Binding a Turn to a Conversation is a continuity record, never a
  permission decision — restating Architecture Section 6's own closing
  invariant.
- **`ConversationEngine` never executes anything.** Its one operation
  calls no `Tool`, no `ExecutionPipeline`, and produces no `ExecutionRequest`.
- **`ConversationEngine` never bypasses Planner.** This pass's own
  operation does not call `PlannerRuntime.plan` at all (Section 1,
  Section 5); when a future pass does add that call, Section 8's own
  deferred reasoning-provider resolution governs how, and it will be an
  ordinary `PlanningRequest` call, never a parallel path.
- **`ConversationEngine` never bypasses the Permission Engine.** No
  operation this document defines calls, or has any path to,
  `PermissionEngine.evaluate`.
- **`ConversationEngine` never owns Memory.** `MemoryStore` is not a
  dependency of this document's one operation (Section 7); nothing this
  document defines decides what is worth retaining long-term.
- **`ConversationEngine` never owns the World Model.** `WorldModel` is
  not a dependency of this document's one operation (Section 7); nothing
  this document defines represents current belief about the world's
  state.
- **Its own operating identity is a resolvable Principal, never
  anonymous, and never the message sender's own identity.** Restating
  Architecture Section 5: an implementation of `ConversationEngine` must
  resolve its own operating identity through `IdentityService` before
  acting (Section 7), mirroring `PLANNER_RUNTIME_PRINCIPAL_ID`/
  `TASK_MANAGER_RUNTIME_PRINCIPAL_ID`'s established precedent (Gap #49's
  closure). The exact identity value is not decided here — Stage 3/6
  territory, restating Architecture Section 13 Item 10.

## 7. Dependencies

**Approved, and exercised by this document's one operation:**

- **`IdentityService`** — to resolve `ConversationEngine`'s own operating
  Principal identity (Section 6). This document does not require
  re-resolving the message's own `senderPrincipalId`; it is already
  resolved by `CommunicationIntake` before this document's operation is
  ever called (Section 1's own precondition).

**Approved by the architecture in principle, but not exercised by any
operation this document defines** (Section 5, Section 8):

- **`PlannerRuntime`**, via an ordinary `PlanningRequest` — Architecture
  Section 8; not called by this pass's own operation.
- **The originating channel's exposed "deliver" `Tool`, via
  `ExecutionPipeline`** — Architecture Section 2, Section 8;
  `COMMUNICATION_CONTRACT_DESIGN.md` Section 7; not called by this
  pass's own operation.
- **`EventBus`**, for optional, purely observational publication —
  Architecture Section 10, itself framed as authorised-but-not-required;
  not exercised by this pass.

**Explicitly prohibited, restating Architecture Section 3 and Section 5:**

- `ExecutionPipeline`, called directly (not via the deliver-Tool path
  above) — never.
- `PermissionEngine.evaluate`, called directly — never.
- `ModuleRegistry` — this document's one operation does not repeat or
  second-guess `CommunicationIntake`'s own channel-status check
  (Section 1).
- `MemoryStore` — never a dependency of this document's one operation
  (Section 6).
- `WorldModel` — never a dependency of this document's one operation
  (Section 6).
- `AgentRuntime` — reached, if at all, only downstream of a Task
  Proposal's own disposition, exactly as `COMMUNICATION_CONTRACT_DESIGN.md`
  Section 9 already establishes; never a direct dependency here.
- A reasoning provider / Cognition — no such contract exists to depend on
  (Section 8).

## 8. Deferred Items

Explicitly deferred, not partially designed:

- **Reasoning-provider contract.** No Kotlin interface for a reasoning
  provider exists anywhere in this repository (`ModelManager.kt` remains
  excluded from the build; confirmed absent from `src/interfaces`).
  Everything downstream of accepting a Turn — engaging a reasoning
  provider, obtaining an interpretation, deciding whether a goal or a
  reply results, and marking a Turn or Conversation complete (Section 3)
  — depends on this contract and is deferred in full until it exists.
- **Response Delivery.** `IMPLEMENTATION_GAPS.md` Gap #53's own closure
  path (b) — this document is one step toward it, not the step that
  closes it. The concrete Tool-based delivery mechanism
  `COMMUNICATION_CONTRACT_DESIGN.md` Section 7 already describes at the
  contract level remains unimplemented and is not extended, narrowed, or
  designed further here.
- **Persistence.** How, or whether, `Conversation`/`Turn` state
  (Section 2) survives beyond process memory is not decided here. Ties
  directly to `IMPLEMENTATION_GAPS.md` Gap #51's own open persistence/
  durability boundary question, which spans Memory, Identity, and Audit
  as well — this document does not carve out a special case for
  Conversation state and does not anticipate Gap #51's own eventual ADR.
- **Android.** No Android service, activity, or platform integration is
  named or implied by any contract above.
- **UI.** No visual, textual-display, or interaction-surface concept is
  defined here; `ConversationEngine` is a backend coordination interface
  only.
- **Speech.** No speech-to-text, text-to-speech, or wake-word concept is
  introduced; `InboundOwnerMessage.text` (reused, Section 4) already
  assumes any such conversion happened upstream, exactly as
  `COMMUNICATION_CONTRACT_DESIGN.md` Section 2 already established.
- **LLM prompts.** No prompt template, model selection, or reasoning
  strategy is named or implied — entirely a reasoning-provider concern,
  deferred above.
- **Execution pipeline changes.** `ExecutionRequest`, `ExecutionPipeline`,
  and `Tool` are not modified, extended, or redesigned by this document —
  restating Architecture Section 3's own "not a redefinition of
  `ExecutionRequest`" non-responsibility, unchanged here.

## 9. Contract Minimalism Review

Every candidate type considered, including every example this document's
own task brief named, with a reason for exclusion where excluded:

| Candidate | Included? | Reason |
| --- | --- | --- |
| `ConversationId` | **Yes** | New identifier a Conversation genuinely requires; no existing identifier covers "which continuing exchange." |
| `TurnId` | **Yes** | New identifier a Turn genuinely requires; distinct from `CorrelationId` (Architecture Section 13 Item 3) and from `ConversationId`. |
| `Conversation` | **Yes** | The continuity record Architecture Section 4 names as this component's sole owned state. |
| `Turn` | **Yes** | The bounded per-message record Architecture Section 6 names. |
| `ConversationDisposition` | **Yes, as a plain data class** | A caller needs to learn the resulting `Conversation`/`Turn` and whether a new Conversation began; a sealed, multi-variant shape is unnecessary because no legitimate rejection case exists at this layer (below). |
| `ConversationResult` | **No** | Would represent a Turn's fully-processed outcome (reasoning plus routing plus completion). Its fields cannot be honestly specified without the reasoning-provider contract this document defers in full (Section 8); defining it now would mean guessing at a shape for a mechanism that does not exist, which PES-001 Stage 2A's own "resolves named outstanding design questions against approved architecture, never inventing new architecture to answer them" instruction forecloses. |
| A `Rejected` variant on `ConversationDisposition` | **No** | This document's one operation's only precondition is an already-`CommunicationIntake`-accepted message (Section 1); Architecture Section 11 explicitly treats correlation failure or ambiguity as a non-blocking condition resolved by opening a new Conversation, not a rejectable error. There is no legitimate failure outcome at this layer to give a variant to. |
| A `TurnStatus` / `ConversationStatus` enum | **No, from this pass** | Naming a status field without also defining what transitions it, and when, would be inventing a partial mechanism rather than deferring it cleanly (Section 3). Mirrors `COMMUNICATION_CONTRACT_DESIGN.md`'s own Minimalism Review precedent for a channel-kind enumeration: excluded until a concrete consumer exists, added additively later rather than guessed at now. |
| A `completeTurn`-shaped second operation | **No, from this pass** | Would require deciding what triggers Turn completion and what data describes its outcome — both depend on the deferred reasoning-provider contract (Section 8). Adding it now would mean designing around a mechanism this document explicitly declines to design around. |
| A separate `ConversationEngineRuntime` wrapper interface | **No** | PES-001 Stage 2A requires this document to state whether one is needed; it is not. One interface, one operation, mirrors `CommunicationIntake`'s identical precedent (Section 1). |
| A new `ChannelId` type | **No — reuse `ModuleId`** | Mirrors `COMMUNICATION_CONTRACT_DESIGN.md` Section 1's identical reasoning: a Communication Channel's identity is already its `ModuleId`; no concrete need for a second, parallel identifier has been identified. |
| A reasoning-provider invocation contract (any shape) | **No — deferred in full** | Section 8. Not partially designed, not stubbed, not named beyond acknowledging it does not yet exist. |
| A `conversation.*` `EventBus` payload schema | **No** | Architecture Section 10 authorises, but does not mandate, a future observability namespace; no operation this document defines publishes anything, so no payload shape is needed yet. |

Net result, restated from the Summary above: **five new contracts, one
new single-operation interface, five existing contracts reused
unchanged, zero modified, one entire deferred class of contract.**

## 10. Self-Traceability Review

| Contract | Traced to Conversation Engine Architecture | Traced to Communication Contract Design | Traced to Module Contract Design | Traced to Parker Constitution | Traced to PES-001 |
| --- | --- | --- | --- | --- | --- |
| `ConversationEngine` (interface) | Section 2 ("Receive accepted `InboundOwnerMessage` instances"), Section 7 | Section 6 (`CommunicationIntake`'s identical one-operation precedent) | — | "Cognition proposes. Trust authorises. Runtime executes" (no self-authorisation) | Stage 2A: "states whether a separate 'Runtime' wrapper interface is needed" |
| `ConversationId` | Section 13 Item 3 (distinct from `CorrelationId`) | Section 4 (`CorrelationId`'s identical "no mandated generation algorithm" precedent) | — | — | Stage 2A: "minimum required set of public contracts" |
| `TurnId` | Section 6 (Turn as bounded unit) | — | — | — | Stage 2A: minimum required set |
| `Conversation` | Section 4 (ownership: "which Turns belong to which Conversation") | Section 1 (`ModuleId` reuse for `channelId`) | Section 1 (`ModuleId` definition) | — | Stage 2A: field-level contract for an approved architecture's owned state |
| `Turn` | Section 6 (Turn diagram: "Owner message") | Section 2 (`InboundOwnerMessage` reuse) | — | — | Stage 2A: field-level contract |
| `ConversationDisposition` | Section 6 (Conversation creation vs. continuation as distinct outcomes) | Section 6 (`CommunicationIntakeDisposition`'s precedent, deliberately not mirrored variant-for-variant — Section 9) | — | — | Stage 2A: "explicitly stating what is required, what is excluded... and why" |
| Trust Boundary (Section 6, above) | Section 5 in full | Section 8 ("no shortcut for messages that arrived via a Communication Channel") | Section 6 ("modules never self-authorise") | "Cognition proposes. Trust authorises. Runtime executes"; "If trust cannot be verified, Parker cannot act" | Chapter 2, Responsibility Model |
| Dependency prohibitions (Section 7, above) | Section 4 (ownership exclusions), Section 8 | Section 9 (`CommunicationIntake`'s identical "no dependency on Planner/Agent/Memory/World Model" precedent) | Section 6, Section 7 (permission/tool bypass prohibitions) | "No capability may bypass trust" | Stage 1's "runtime boundaries" requirement, now made concrete |
| Deferred Items (Section 8, above) | Section 13 (all ten items), Section 14 (sequencing) | Section 11 ("Out of Scope") | — | "Replaceable reasoning providers" (no reasoning provider is load-bearing) | Stage 2A: "explicitly stating... what is deferred, and why" |

Every contract above traces to an authoritative source actually read
while drafting this document (Review, above). No architecture is invented
beyond what `docs/architecture/19-conversation-engine.md` already
authorises at the concept level; no field-level shape contradicts it.

## Conclusion

**This document gives the Conversation Engine's inbound half a settled
Stage 2A Contract Design: one public interface (`ConversationEngine`,
one operation), five new field-level types (`ConversationId`, `TurnId`,
`Conversation`, `Turn`, `ConversationDisposition`), a contract-level
lifecycle naming all five stages Chapter 19 anticipated (two of which —
Turn completion and Conversation completion — are named as concepts but
not yet given a Kotlin mechanism, honestly deferred rather than guessed
at), a trust boundary confirming no authorisation, execution, or bypass
occurs anywhere in this surface, a dependency list separating what this
pass's own operation exercises from what the architecture authorises but
this pass does not yet reach, eight explicitly deferred items, a
Minimalism Review accounting for every candidate type this document's own
brief named, and a Self-Traceability Review connecting every contract
back to its authorising source.**

Consistent with this Sprint's own scope, this document does not implement
anything, does not modify `docs/architecture/19-conversation-engine.md`
or any other existing document, and does not close
`IMPLEMENTATION_GAPS.md` #53 — Gap #53 remains open, and this document
narrows, but does not complete, its closure path (b). Once reviewed and
accepted, this document is the basis for a Stage 3 Implementation Plan
scoped to exactly the surface defined above (`ConversationEngine`,
`ConversationId`, `TurnId`, `Conversation`, `Turn`,
`ConversationDisposition`) — no more, and not the reasoning-provider
engagement or Response Delivery halves this document deliberately leaves
for a future, separately-scoped Contract Design pass.

## Related

- `docs/architecture/19-conversation-engine.md`
- `docs/architecture/parker-constitution.md`
- `docs/architecture/PARKER_ENGINEERING_STANDARD.md`
- `docs/architecture/COMMUNICATION_CONTRACT_DESIGN.md`
- `docs/architecture/LOCAL_TEXT_CHANNEL_CONTRACT_DESIGN.md`
- `docs/architecture/MODULE_CONTRACT_DESIGN.md`
- `docs/architecture/ARCHITECTURE_V2_FROZEN_BASELINE.md`
- `docs/architecture/IMPLEMENTATION_GAPS.md` (#53, #49, #51)
- `src/interfaces/CommunicationIntake.kt`
- `src/contracts/PlanDecision.kt`
- `src/contracts/TaskProposal.kt`
