# Conversation Identity Architecture — Scope Lock

## Status

**Sprint 11, Unit 5. This is the constitutional governing document for
Conversation Identity.** Once frozen, any future Contract Design —
whether for the Conversation Engine's own recognition rule or for
Conversation History Source — must implement what this document defines,
not redesign it. Companion to
`CONVERSATION_IDENTITY_ARCHITECTURE_IMPLEMENTATION_PLAN.md`, whose
Section 2 (Architectural Review Findings) this document's every decision
rests on. Governance only — no Kotlin is created or modified by this
document.

---

## 1. Responsibilities

Conversation identity owns exactly three things:

- **Recognising logical conversation continuity — conceptually, not
  algorithmically.** This document states that continuity recognition
  is a real, necessary capability and restates who is already
  responsible for it (Section 4, Ownership); it does not itself supply
  a recognition rule. The rule remains genuinely open
  (`CONVERSATION_IDENTITY_ARCHITECTURE_IMPLEMENTATION_PLAN.md` Section
  2, Fact 2) and is not decided here.
- **Exposing conversation identity to authorised runtime components.**
  Once recognised, a conversation identifier is a value other
  components may legitimately be given — today, `ConversationDisposition`
  already exposes it (`conversation.conversationId`) to whatever calls
  `ConversationEngine.submitTurn`. A future component (Conversation
  History Source, Sprint 11 Unit 4) may need the same value made
  available earlier or elsewhere; this document names that need without
  designing the mechanism (`SPRINT_11_UNIT_5_ENGINEERING_CHECKPOINT.md`).
- **Remaining independent from conversation content.** A conversation
  identifier is a label, never the substance of what was said. It carries
  no message text, no reasoning output, and no Turn content of any kind.

## 2. Explicit Exclusions

Conversation identity must never own:

- **Conversation storage.** How, or whether, `Conversation`/`Turn` state
  survives beyond process memory is `IMPLEMENTATION_GAPS.md` Gap #51's
  own open question, untouched here.
- **History retrieval.** Retrieving prior Turns belongs to Conversation
  History Source (Sprint 11, Unit 4), never to identity itself — Section
  9, below, restates this boundary as permanent.
- **Memory.** A distinct, future architectural concept. Conversation
  identity must never evolve into Memory, must never read from a future
  `MemoryStore`, and identity and long-term knowledge remain distinct
  architectural concepts (Section 11).
- **World Model.** A distinct, future architectural concept. Conversation
  identity must never expose World Model state (Section 12).
- **Planner.** Conversation identity performs no planning and never
  invokes `PlannerRuntime` (Section 13).
- **Turn creation.** Conversation identity never constructs a `Turn`.
  That remains `ConversationEngine.submitTurn`'s own, sole, unchanged
  responsibility.
- **Message routing.** Conversation identity never decides where a
  message goes next, never calls `PlannerRuntime.plan`, and never
  triggers Response Delivery.
- **Tool execution.** No dependency on `ToolRegistry.resolve`, any `Tool`
  handle, `ToolInvocationBinding`, or the Execution Pipeline exists or is
  ever introduced.
- **Persistence implementation.** Identity does not imply persistence
  (Section 3) — this document takes no position on whether, or how, a
  conversation identifier is ever stored beyond one Conversation's own
  in-memory lifetime.
- **Embeddings.** No vectorisation, embedding, or indexing of any kind.
- **Semantic search.** No ranking, filtering, or similarity-based
  selection of any kind.
- **Summarisation.** No condensation, compression, or paraphrase of
  anything.

## 3. Architectural Principles

- **Conversation identity is identity, not content.** A conversation
  identifier answers only "which continuing exchange is this," never
  "what was said" or "what does it mean." `ConversationId`'s own
  existing shape (`src/interfaces/ConversationEngine.kt`) already
  reflects this — a bare, non-blank string value, structurally identical
  to `PrincipalId`/`ModuleId`/`CorrelationId`, carrying no content field
  of any kind.
- **Conversation identity exists independently of conversation
  storage.** Whether, or how, `Conversation`/`Turn` state is ever
  persisted (Gap #51, untouched here) has no bearing on whether a
  conversation identifier exists and remains stable. The two questions
  are architecturally independent; this document decides only the
  second.
- **A conversation identifier must remain stable throughout the
  lifetime of a logical conversation.** Restated, not invented, from
  `CONVERSATION_ENGINE_CONTRACT_DESIGN.md` Section 2: a `ConversationId`
  "exists and is stable for the life of a Conversation." This document
  elevates that already-accepted contract property to a binding
  architectural principle for conversation identity generally, not only
  for `ConversationId`'s own Kotlin shape.
- **Identity does not imply persistence.** A stable identifier for the
  duration of one in-memory Conversation carries no claim about whether
  that Conversation, or its identifier, survives a process restart, a
  crash, or any durability boundary `IMPLEMENTATION_GAPS.md` Gap #51 will
  eventually settle. Stability and durability are distinct properties;
  this document requires only the former.
- **Identity must not require knowledge of future runtime
  capabilities.** A conversation identifier's own architectural
  properties (Section 1, above) must hold regardless of whether Memory,
  World Model, or Conversation History Source ever exist, ever are
  implemented, or ever change. Nothing about recognising or exposing
  conversation identity may be designed to depend on a future
  capability that does not exist today.

## 4. Ownership

**The production owner of conversation identity's actual recognition
and assignment is the Conversation Engine — restated, not reassigned,
from `19-conversation-engine.md` Section 4** ("The Conversation Engine
owns exactly one thing: Conversation state — which Turns belong to which
Conversation"). This document does not introduce a second owner, does
not compete with that ownership, and does not assume any particular
future implementation of it (`CONVERSATION_IDENTITY_ARCHITECTURE_IMPLEMENTATION_PLAN.md`
Section 6). What this document owns instead is the conceptual contract
(Section 3, above) that whichever recognition rule the Conversation
Engine's own future Contract Design eventually adopts must satisfy.

Whether a future Conversation History Source ever becomes a second
legitimate reader of a resolved conversation identity (not a second
owner of recognising it) is named, not decided, here — restated as an
open question in `SPRINT_11_UNIT_5_ENGINEERING_CHECKPOINT.md`.

## 5. Lifetime

Described conceptually only — no Kotlin state machine, no storage
decision:

- **Creation.** A conversation identifier is created at the same
  conceptual moment `19-conversation-engine.md` Section 6 already
  describes for Conversation creation — when an inbound message is
  determined not to continue any existing Conversation. This document
  does not add a second creation moment or a second creation authority.
- **Stability.** From creation until the Conversation it identifies
  ends (`19-conversation-engine.md` Section 13 Item 4, still open), the
  identifier does not change (Section 3, above).
- **Expiry.** Not decided by this document. When, or whether, a
  Conversation — and therefore its identifier — is considered ended is
  exactly as open as `19-conversation-engine.md` Section 13 Item 4
  already left it; this document does not narrow that question.
- **Replacement.** A conversation identifier is never replaced while its
  Conversation continues. If a Conversation is ever considered to have
  ended and a subsequent message begins a genuinely new one, that new
  Conversation receives a new identifier — this is Conversation
  creation recurring, not identifier replacement within one Conversation's
  own lifetime.

## 6. Threading

- **Immutability.** A conversation identifier, once created, is an
  immutable value — mirroring `ConversationId`'s own existing
  `@JvmInline value class` shape. Nothing about exposing it to an
  authorised reader (Section 1) permits mutation.
- **Sharing.** A conversation identifier may be read by more than one
  authorised component (today, whatever calls `ConversationEngine.submitTurn`;
  in future, potentially a Conversation History Source reader) without
  risk, precisely because it is immutable and carries no content
  (Section 3). This document does not restrict how many components may
  hold a reference to the same identifier value.
- **Coroutine expectations.** Any future operation that recognises or
  exposes conversation identity is expected to be `suspend`-declared,
  consistent with `ConversationEngine.submitTurn` and every other
  read-oriented dependency this runtime already relies on — not decided
  as a final interface shape here, only as an expectation a future
  Contract Design should meet or explicitly revise.

## 7. Governing Principle

**Conversation identity is identity, not content. It exists
independently of storage. It must remain stable for the life of the
conversation it identifies. It does not imply persistence, and it must
not require knowledge of any future runtime capability.**

Every property above traces to an already-accepted, frozen document
(Section 3 citations) or to this Unit's own required review
(`CONVERSATION_IDENTITY_ARCHITECTURE_IMPLEMENTATION_PLAN.md` Section 2).
Nothing here is invented beyond consolidating and naming what those
sources already establish, or leaving open what they already left open.

## 8. What Conversation Identity Is, and Is Not

**Is:**

- Read-only, in the sense that consuming an already-recognised identity
  never mutates it.
- Deterministic — mirroring `ConversationId`'s own existing shape, and
  restating `CONVERSATION_ENGINE_CONTRACT_DESIGN.md` Section 2's "no
  mandated generation algorithm" precedent: whatever process eventually
  recognises continuity, the resulting identifier's own stability and
  content-independence properties (Section 3) hold regardless of that
  process's own internal mechanism.
- Side-effect free. Recognising and exposing identity does not itself
  cause any observable change outside the Conversation Engine's own
  already-owned state.
- Stateless, as a concept. This document defines properties an
  identifier must have; it holds no state of its own.
- An architecture boundary only — a concept and a set of properties,
  not a runtime component this document constructs.

**Is not:**

- Memory (Section 2, Section 11).
- World Model (Section 2, Section 12).
- Planner (Section 2, Section 13).
- `ConversationEngine` itself — this document does not redesign,
  replace, or duplicate it; `ConversationEngine` remains the sole owner
  of recognition (Section 4).
- A conversation store or a conversation repository — this document
  defines no storage mechanism (Section 2).

Conversation identity does not create Turns, does not modify Turns, and
does not delete Turns — all three remain `ConversationEngine.submitTurn`'s
own, sole, unchanged responsibility. It only provides a governed
mechanism — a set of properties and a named (not designed) exposure
point — through which future components may obtain conversation
identity.

## 9. Relationship to Conversation History Source

Conversation Identity is a prerequisite. Conversation History Source
(Sprint 11, Unit 4) depends upon Conversation Identity — it needs a
conversation identifier to know which conversation's history to
retrieve. Conversation Identity must not retrieve history; Conversation
History Source must not determine identity. These responsibilities
remain permanently separate, restated identically in both Units' own
Scope Locks (`CONVERSATION_HISTORY_SOURCE_SCOPE_LOCK.md` Section 1).

## 10. Relationship to the Constitution

`docs/architecture/parker-constitution.md`: "Parker owns authority.
Modules provide capability" (line 18/40); "Cognition proposes. Trust
authorises. Runtime executes" (line 48/52). Conversation identity is
capability, not authority. It proposes nothing, authorises nothing, and
executes nothing — it is a continuity fact about which messages belong
together, never itself a grant of trust, permission, or elevated
confidence in any Turn's interpretation, restating
`19-conversation-engine.md` Section 6's identical closing invariant
("Conversation" and "Turn" are continuity and correlation concepts
only).

## 11. Relationship to Memory

Memory remains a future Unit. Conversation identity must not evolve into
Memory. Conversation identity and long-term memory are distinct
architectural concepts: identity answers "which conversation," never
"what is worth remembering." Preserving that separation is this
document's own explicit responsibility, restated from
`CONVERSATION_HISTORY_SOURCE_SCOPE_LOCK.md` Section 2's identical
concern for Conversation History Source.

## 12. Relationship to World Model

World Model remains a future Unit. Conversation identity must not expose
World Model state. Identity concerns which conversation a Turn belongs
to; it carries no belief about the world's current state.

## 13. Relationship to Planner

Planner remains a future Unit. Conversation identity performs no
planning, and never invokes `PlannerRuntime` — recognising continuity is
never itself a proposal, a plan, or a decision to act.
