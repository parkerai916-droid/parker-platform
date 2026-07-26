# Conversation History Source — Implementation Plan

## Status

**Sprint 11, Unit 4. PES-001 Stage 2 (Implementation Plan). Governance
only.** Defines the architecture of a new boundary, Conversation History
Source, that a future Unit will contract-design and implement. No Kotlin
is created or modified by this document; no production or test file is
touched. This document describes architecture only — it does not
describe implementation.

---

## 1. Governing Documents

In order of authority:

1. `docs/architecture/parker-constitution.md` — "Parker owns authority.
   Modules provide capability" (line 18/40) and "Cognition proposes.
   Trust authorises. Runtime executes" (line 48/52). Conversation
   History Source is a capability a future Cognition-adjacent component
   may draw on; it proposes nothing and authorises nothing itself.
2. `docs/architecture/reasoning-context.md` — the three-layer knowledge
   architecture (Memory / World Model / Reasoning Context) this
   document does not redefine. Conversation History Source is not a
   fourth layer; it is a read boundary that a future Reasoning Context
   Assembler input may draw the "Conversation Context" category from
   (`PRODUCTION_REASONING_CONTEXT_SCOPE_LOCK.md` Section 1).
3. `docs/implementation/PRODUCTION_REASONING_CONTEXT_SCOPE_LOCK.md`
   (Sprint 11, Unit 1) — names "Current conversation" as a Scope Lock
   Section 1 item, tagged *(Conversation Context)*, not yet resolvable.
4. `docs/architecture/PRODUCTION_REASONING_CONTEXT_CONTRACT_DESIGN.md`
   (Sprint 11, Unit 2) Section 4.2 — names "Conversation History
   Source" explicitly as a deferred dependency boundary: "no existing
   interface in this codebase can supply it today," and states plainly
   that resolving it is "a future Unit's own decision," not designed
   there. This Unit is that future Unit, for the boundary only — a
   Contract Design and implementation remain separately scoped, later
   Units.
5. `src/interfaces/ConversationEngine.kt` — the frozen, unmodified
   `ConversationEngine` interface and `Conversation`/`Turn`/`ConversationDisposition`
   shapes, read but not changed by this document.
6. `src/runtime/InMemoryConversationEngine.kt` — the frozen, unmodified
   current implementation, read for its own documented behaviour
   (Section 2, below).

---

## 2. Why This Question Is Still Open

Two independent findings, both already on record, converge on the same
conclusion: this boundary cannot be filled by reusing `ConversationEngine`
as it exists today, and cannot yet be designed in full, because the
thing it would read does not yet exist in a retrievable form.

**Finding 1 (Sprint 11, Unit 2): `ConversationEngine` exposes no read
operation.** `ConversationEngine`'s only method is `submitTurn`, which
*constructs* a `Turn` as a side effect (Architecture-Section-6-described
mutation). Giving any reader — the Reasoning Context Assembler
specifically named, but any future reader in general — a live
`ConversationEngine` reference would hand it the ability to call
`submitTurn` itself, a mutating operation incompatible with a read-only
boundary. No other type in this codebase exposes prior Turns by
`ConversationId` for reading.

**Finding 2 (this Unit, newly confirmed by direct reading): conversation
identity itself is not known at the point a future reader would need
it.** `ConversationDisposition`'s own KDoc states plainly: "the caller
supplies no `ConversationId` on the way in" — `submitTurn` alone decides
whether an inbound message continues an existing Conversation or begins
a new one, and only *returns* that decision after the fact, via
`ConversationDisposition.isNewConversation`. Consequently, before
`ConversationEngine.submitTurn` runs, nothing in this runtime knows which
Conversation (if any continuing one) an inbound message belongs to.
`ReasoningContextAssembler.assemble` runs *before* `submitTurn`
(`PRODUCTION_REASONING_CONTEXT_CONTRACT_DESIGN.md` Section 2) — meaning
even if a read-only history interface existed today, the Assembler would
have no `ConversationId` to query it with yet.

**Compounding both findings: `InMemoryConversationEngine`'s own,
current, frozen implementation does not yet recognise conversation
continuity at all.** Its own KDoc states its "Required Implementation
Decision 1": "every inbound Turn begins a new Conversation... this first
unit does not attempt conversation-continuation recognition — there is
no stored state to consult." Concretely, `submitTurn` mints a fresh,
random `ConversationId` (`UUID.randomUUID()`) for every call, with no
lookup of any kind. There is, today, no continuing Conversation for a
Conversation History Source to retrieve history *from* — the concept
this boundary exists to serve is itself not yet implemented downstream
of it.

This Unit does not resolve either finding. It defines the boundary a
future Contract Design will need to fill, and records both findings so
that future Contract Design does not have to rediscover them
(`SPRINT_11_UNIT_4_ENGINEERING_CHECKPOINT.md` records both as
unresolved).

---

## 3. Responsibilities

Conversation History Source's one responsibility: **given a means of
identifying a conversation, retrieve a read-only, already-existing
excerpt of that conversation's prior history.** Nothing about assembling
that excerpt into a `ReasoningContext` entry, nothing about deciding how
much history is relevant, and nothing about summarising, embedding, or
semantically ranking that history belongs to it — those remain, at most,
a future Reasoning Context Assembler's own concern when it consumes
whatever Conversation History Source returns.

This mirrors exactly the discipline `PRODUCTION_REASONING_CONTEXT_SCOPE_LOCK.md`
Section 3 already established for `ReasoningContext` itself: Conversation
History Source is a projection of `ConversationEngine`'s own owned state
(`Conversation`/`Turn`, Architecture Section 4), never a second source of
truth for it. If a Conversation History Source excerpt and
`ConversationEngine`'s own state ever disagree, `ConversationEngine`'s
own state is correct.

---

## 4. Lifecycle

Two distinct lifecycles, not conflated (mirroring
`PRODUCTION_REASONING_CONTEXT_CONTRACT_DESIGN.md` Section 7's identical
distinction for the Reasoning Context Assembler):

- **The Conversation History Source component's own lifecycle.**
  Constructed once, reused for every read — a natural consequence of
  Statelessness (Section 5, `CONVERSATION_HISTORY_SOURCE_SCOPE_LOCK.md`).
- **A returned history excerpt's own lifecycle.** Per-request, immutable
  from the moment it is returned, discarded once its caller is done with
  it — not persisted, not cached, not accumulated across requests.

---

## 5. Ownership

Conversation History Source is owned and constructed by whichever
production composition root exists at the time a future implementation
Unit is authorised (today, `parker.composition.ParkerRuntime`), exactly
mirroring `PRODUCTION_REASONING_CONTEXT_SCOPE_LOCK.md` Section 4's
identical ownership shape for the Reasoning Context Assembler. Exactly
one production owner; exactly one production caller
(`CONVERSATION_HISTORY_SOURCE_SCOPE_LOCK.md` Section 4).

---

## 6. Construction

Not designed by this document. A future Contract Design must decide
whether Conversation History Source is backed by `ConversationEngine`
itself (through some new, narrower, read-only surface `ConversationEngine`'s
own future Contract Design revision would need to add — out of this
Unit's authority to propose), by an independent store `ConversationEngine`
writes to in addition to its own owned state, or by some other mechanism
entirely. Naming this boundary does not presuppose its construction.

---

## 7. Interaction With Existing Runtime

Conversation History Source integrates with, and does not redesign, the
frozen components this Unit's own task instructions name: `ParkerRuntime`,
`ConversationEngine`, `CommunicationConversationCoordinator`,
`ConversationTurnReasoningCoordinator`, `ConversationReplyCoordinator`,
`ReasoningContextAssembler`, `ModelBackedReasoningProvider`,
`ResponseComposer`, `ReplyDeliveryCoordinator`, `IdentityService`,
`ToolRegistry`, `ResourceRegistry`, the Permission Engine, and the
Execution Pipeline. None of these components' existing signatures
change as a result of this document. In particular:

- `ConversationEngine.submitTurn`'s signature is unchanged — this
  document does not add a read method to it, and does not propose one.
- `ReasoningContextAssembler.assemble`'s signature
  (`suspend fun assemble(message: InboundOwnerMessage): ReasoningContext`)
  is unchanged. A future Contract Design may propose threading a
  Conversation History Source read through it, or may not — not decided
  here (Section 8).
- No coordinator between `ParkerRuntime.submitOwnerMessage` and the
  Reasoning Provider acquires a new responsibility, a new dependency, or
  a new call.

---

## 8. Excluded From This Unit

- Any Kotlin interface, class, or test.
- A Contract Design for Conversation History Source's own interface
  shape (a separately-scoped, future Unit, per this Unit's own
  instructions).
- Any resolution of Section 2's two findings (how a conversation is
  identified before `submitTurn` runs; how continuity recognition itself
  would work) — recorded as unresolved
  (`SPRINT_11_UNIT_4_ENGINEERING_CHECKPOINT.md`), not answered.
- Any change to `ConversationEngine`, `InMemoryConversationEngine`, or
  any other frozen component named in Section 7.
- Memory, World Model, Planner, summarisation, embeddings, or semantic
  retrieval — each named as a permanent exclusion, not a future
  responsibility of Conversation History Source
  (`CONVERSATION_HISTORY_SOURCE_SCOPE_LOCK.md` Section 2).

---

## Conclusion

Conversation History Source is named here as a real, needed, distinct
architectural boundary — a read-only source of prior conversation
history, owned by a future implementation, invoked by a future
Reasoning Context Assembler revision or its composition root, never
mutating `ConversationEngine`'s own owned state. This Unit defines the
boundary's shape in prose only. Two genuine, load-bearing open
questions — how a conversation is identified before a Turn exists, and
how continuity recognition itself would need to work — are recorded,
not answered, in this Unit's own Engineering Checkpoint. A future
Contract Design must resolve both before implementation can begin.
