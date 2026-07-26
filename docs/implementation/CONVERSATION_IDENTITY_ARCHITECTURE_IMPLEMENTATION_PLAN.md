# Conversation Identity Architecture — Implementation Plan

## Status

**Sprint 11, Unit 5. PES-001 Stage 2 (Implementation Plan). Governance
and architecture only.** Defines the architectural concept of
conversation identity — how Parker recognises that multiple inbound
interactions belong to the same logical conversation — as a prerequisite
`CONVERSATION_HISTORY_SOURCE_IMPLEMENTATION_PLAN.md` (Sprint 11, Unit 4)
already named but did not resolve. No Kotlin is created or modified by
this document; no production or test file is touched. This document
describes architecture only.

**Mandatory read-only review performed before drafting.** Per this
Unit's own "Important Review Requirement," Section 2 below records what
that review found — the architectural facts — before any responsibility,
exclusion, or principle is defined. Nothing in Sections 3 onward
contradicts what Section 2 establishes.

---

## 1. Governing Documents

In order of authority:

1. `docs/architecture/parker-constitution.md` — "Parker owns authority.
   Modules provide capability" (line 18/40); "Cognition proposes. Trust
   authorises. Runtime executes" (line 48/52).
2. `docs/architecture/19-conversation-engine.md` ("Chapter 19",
   Stage 1 Architecture, accepted, frozen) — Section 4 assigns
   Conversation state ownership, including "which Turns belong to which
   Conversation," to the Conversation Engine. Section 13 Item 3 names
   "the exact rule for recognising that a new inbound message continues
   an existing Conversation" as explicitly undecided, and states plainly
   that a Conversation identifier, "distinct from `CorrelationId`," is
   already a named (if unresolved) concept.
3. `docs/architecture/CONVERSATION_ENGINE_CONTRACT_DESIGN.md` (Stage 2A,
   accepted, frozen) — Section 2 gives `ConversationId` its field-level
   shape and states its one binding property: it "exists and is stable
   for the life of a Conversation." Section 3 restates that the
   recognition rule itself "remains Architecture Section 13 Item 3, not
   decided here."
4. `docs/implementation/CONVERSATION_ENGINE_IMPLEMENTATION_PLAN.md`
   (Stage 3, accepted, frozen) Section 5, Decision 1 — records the
   current implementation's "every Turn begins a new Conversation"
   behaviour explicitly as "a Stage 3/4-level conservative default, not
   an architectural claim," and states that the existing, frozen
   contract shapes require no change once a real recognition rule
   replaces it.
5. `docs/implementation/CONVERSATION_HISTORY_SOURCE_IMPLEMENTATION_PLAN.md`
   and `docs/implementation/CONVERSATION_HISTORY_SOURCE_SCOPE_LOCK.md`
   (Sprint 11, Unit 4) — named this Unit's own prerequisite: Conversation
   History Source cannot retrieve history until conversation identity
   itself is resolvable, including before a `Turn` exists.
6. `docs/architecture/PRODUCTION_REASONING_CONTEXT_CONTRACT_DESIGN.md`
   (Sprint 11, Unit 2) Section 2 — establishes that
   `ReasoningContextAssembler.assemble` runs before
   `ConversationEngine.submitTurn`, the origin of this Unit's own second
   finding (Section 2, below).
7. `src/interfaces/ConversationEngine.kt` — the frozen, unmodified
   `ConversationId`/`Conversation`/`Turn`/`ConversationDisposition`
   shapes, read but not changed by this document.
8. `src/runtime/InMemoryConversationEngine.kt` — the frozen, unmodified
   current implementation, read for its own documented behaviour.

---

## 2. Architectural Review Findings

This Unit's own task instructions require a read-only review,
performed before any responsibility, exclusion, or principle was
drafted, to determine whether the conversation-continuity problem Sprint
11 Units 2 and 4 surfaced is a genuine architectural gap, an artefact of
`InMemoryConversationEngine` specifically, or temporary scaffolding. The
review traced `ConversationId`'s complete lifecycle across all three
governing documents named in Section 1 above. Three distinct facts
emerged, and they must not be conflated:

**Fact 1 — Conversation identity already exists conceptually, and its
ownership is already assigned. This is constitutional architecture, not
a blank slate.** Chapter 19 Section 4 assigns "which Turns belong to
which Conversation" to the Conversation Engine as its sole owned state,
in the same document that first named Conversation/Turn at all. Contract
Design Section 2 gives `ConversationId` a field-level shape and a
binding property — stable for the life of a Conversation — and states
explicitly that it is architecturally distinct from `CorrelationId`
(Chapter 19 Section 13 Item 3, restated). This Unit does not invent
conversation identity as a concept, and does not reassign its ownership
away from the Conversation Engine — both already exist, frozen, and
accepted.

**Fact 2 — The recognition rule itself is genuinely, still unresolved
architecture, named as open at two separate governance stages, not
silently omitted.** Chapter 19 Section 13 Item 3 (Stage 1 Architecture)
and Contract Design Section 3 (Stage 2A Contract Design) both explicitly
defer "the exact rule for recognising that a new inbound message
continues an existing Conversation." This is not an oversight this Unit
is discovering for the first time — it is a deliberately, repeatedly
named open question, carried forward, unresolved, across two accepted
governance stages. This part of the problem is genuinely architectural:
no document anywhere in this repository has yet decided it.

**Fact 3 — The current always-new-Conversation behaviour is a disclosed,
temporary, Sprint-7-scoped implementation default, not an architectural
ceiling.** `CONVERSATION_ENGINE_IMPLEMENTATION_PLAN.md` Section 5,
Decision 1, proposes "every Turn begins a new Conversation" as a
conservative default specifically because Fact 2's own open question
could not be silently invented at Stage 3 — and states plainly, in its
own Self-Traceability-style table, that this default is "a Stage 3/4-level
conservative default, not an architectural claim." Critically, the same
Decision records that **the existing, frozen contract shapes need no
change** when a real recognition rule eventually replaces this default:
"`isNewConversation` and `Conversation.turnIds` already exist to carry a
richer answer." The random, per-Turn `ConversationId` generation inside
`InMemoryConversationEngine.submitTurn` is scaffolding in exactly this
sense — a placeholder value standing in for a real recognition outcome,
already anticipated to be replaced without touching `ConversationEngine`'s
own interface.

**Fact 4 — Sprint 11 surfaced a fourth, genuinely new problem Chapter 19
and its Contract Design did not anticipate: identity may be needed
*before* a Turn exists at all.** Chapter 19 and its Contract Design both
frame conversation continuity recognition as something `ConversationEngine.submitTurn`
performs internally, for its own purposes, when a Turn is created.
Neither document anticipates a caller needing to know conversation
identity *before* calling `submitTurn` — because no such caller existed
when either was written. `PRODUCTION_REASONING_CONTEXT_CONTRACT_DESIGN.md`
Section 2 establishes that `ReasoningContextAssembler.assemble` runs
before `ConversationEngine.submitTurn`, and `ConversationDisposition`'s
own KDoc confirms "the caller supplies no `ConversationId` on the way
in" — meaning today, nothing upstream of `submitTurn` has one to supply.
This is not the same question as Fact 2 (what rule recognises
continuity) — it is a distinct question (when, and to whom, can a
recognised or provisional identity be made available) that no prior
document could have named, since the component that needs it
(`ReasoningContextAssembler`) did not exist until Sprint 11.

**Conclusion of this review.** The problem is neither purely a genuine
unresolved architectural gap nor purely an `InMemoryConversationEngine`
artefact — it is both, in different parts, and this Unit's own
documents (Sections 3 onward, and the accompanying Scope Lock) scope
themselves to exactly the two parts that are genuinely unresolved (Facts
2 and 4), without touching or reassigning what is already settled (Fact
1), and without treating Fact 3's implementation default as anything
more than what its own governing document already says it is.

---

## 3. Purpose

Conversation Identity Architecture exists to state, as a single,
consolidated, constitutional-grade reference, the properties any
conversation identifier must have — properties that today are correct
but scattered across three separate documents (Section 1) — and to name,
without resolving, the two genuinely open questions Section 2 identifies
(the recognition rule, and pre-Turn availability), so that a future
Contract Design pass for either `ConversationEngine`'s own recognition
rule or Conversation History Source's own interface has one place to
read both from.

## 4. Responsibilities

- **State the architectural properties a conversation identifier must
  satisfy**, consolidating what Chapter 19 and its Contract Design
  already established (Fact 1) rather than inventing new ones.
- **Name, precisely, the two remaining open questions** (Facts 2 and 4)
  as the scope a future Contract Design must resolve.
- **Preserve, not reassign, existing ownership.** The Conversation
  Engine's own Chapter 19 Section 4 ownership of conversation continuity
  recognition is restated, not duplicated or competed with.

## 5. Architectural Role

Conversation Identity Architecture is not a new runtime component and
not a new peer alongside the Conversation Engine. It is the
architectural specification of a concept — what a conversation
identifier is, and what must remain true of it — that the Conversation
Engine's own future recognition-rule implementation (Fact 2) must
satisfy, and that a future Conversation History Source (Sprint 11, Unit
4) may rely on as a value contract once it can be supplied (Fact 4).
Restated as this Unit's own governing principle
(`CONVERSATION_IDENTITY_ARCHITECTURE_SCOPE_LOCK.md` Section 3):
conversation identity is identity, not content, and not a competing
owner of Conversation state.

## 6. Ownership

Per Fact 1, the production owner of a resolved conversation identity's
*value* — the component that actually recognises continuity and
assigns or continues a `ConversationId` — remains the Conversation
Engine, exactly as Chapter 19 Section 4 already establishes. This
document does not reassign that ownership, does not introduce a second
component authorised to decide continuity, and does not assume any
particular future implementation of it. Its own contribution is the
conceptual contract (Section 4, above) that ownership must produce,
regardless of which future recognition rule (Fact 2) eventually fills
it in.

## 7. Lifecycle

Described conceptually only, per this Unit's own instruction:

- **Creation.** A conversation identifier comes into existence at the
  same conceptual moment Chapter 19 Section 6 already describes for
  Conversation creation — when an inbound message is determined not to
  continue any existing Conversation. This document does not add a
  second creation moment.
- **Stability.** Once created, a conversation identifier does not
  change for the life of the Conversation it identifies (Contract
  Design Section 2, restated as this document's own binding principle,
  `CONVERSATION_IDENTITY_ARCHITECTURE_SCOPE_LOCK.md` Section 3).
- **Continuation.** A further inbound message determined to continue an
  existing Conversation is associated with that Conversation's own,
  already-stable identifier — never issued a new one.
- **Expiry/replacement.** Not decided by this document. Chapter 19
  Section 13 Item 4 (idle/termination rules) remains exactly as open as
  it already was; this document does not narrow it.

## 8. Interaction With Existing Runtime

This document changes no existing component's signature. In particular:

- `ConversationEngine.submitTurn`'s signature is unchanged. This
  document does not add a read method, does not add a parameter, and
  does not propose one — restating this Unit's own "do not redesign
  existing runtime components" instruction.
- `ReasoningContextAssembler.assemble(message: InboundOwnerMessage): ReasoningContext`
  is unchanged. Fact 4's own open question (whether and how a
  conversation identity could ever be made available at this call site)
  is named, not answered, here.
- No coordinator among `ParkerRuntime`, `CommunicationConversationCoordinator`,
  `ConversationTurnReasoningCoordinator`, or `ConversationReplyCoordinator`
  acquires a new responsibility, dependency, or call as a result of this
  document.
- `IdentityService`, `ToolRegistry`, `ResourceRegistry`, the Permission
  Engine, and the Execution Pipeline are unaffected — none is a
  dependency of anything this document defines.

## 9. Future Interaction With Conversation History Source

Restating `CONVERSATION_HISTORY_SOURCE_SCOPE_LOCK.md`'s own relationship
statement, now from this document's side: Conversation Identity
Architecture is a prerequisite Conversation History Source depends upon.
Conversation Identity does not retrieve history: it is, at most, the
value a future Conversation History Source would be given, or would
resolve, in order to know *which* conversation's history to retrieve.
Conversation History Source does not determine identity — it consumes
whatever identity concept this document and a future `ConversationEngine`
recognition-rule Contract Design eventually produce. These
responsibilities remain permanently separate, restated identically in
both Units' own Scope Locks.

## 10. Excluded From This Unit

- Any Kotlin interface, class, or test.
- The recognition rule itself (Fact 2) — remains a future
  `ConversationEngine`-focused Contract Design's own decision, not
  this Unit's.
- A resolution to Fact 4 (how identity becomes available before a Turn
  exists) — named as an unresolved question
  (`SPRINT_11_UNIT_5_ENGINEERING_CHECKPOINT.md`), not answered.
- Any change to `ConversationEngine`, `InMemoryConversationEngine`, or
  any other frozen component named in Section 8.
- Conversation storage, history retrieval, persistence implementation,
  Memory, World Model, Planner, embeddings, semantic search, and
  summarisation — each a permanent exclusion, restated in
  `CONVERSATION_IDENTITY_ARCHITECTURE_SCOPE_LOCK.md` Section 2.

---

## Conclusion

Conversation Identity Architecture consolidates, as one constitutional
reference, what Chapter 19 and its Contract Design already decided about
conversation identity (Fact 1) — ownership at the Conversation Engine,
stability for the life of a Conversation, distinctness from
`CorrelationId` — and names, precisely and without resolving, the two
genuinely open questions this Sprint has found: the recognition rule
itself (Fact 2, open since Chapter 19), and the newly-discovered
pre-Turn availability problem (Fact 4, first surfaced by Sprint 11 Unit
2). It reassigns no ownership, redesigns no frozen component, and
answers neither open question. A future Contract Design — for the
Conversation Engine's own recognition rule, for Conversation History
Source, or both — must resolve them before implementation can begin.
