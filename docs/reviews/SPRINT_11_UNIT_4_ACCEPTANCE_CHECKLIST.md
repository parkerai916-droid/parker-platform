# Sprint 11, Unit 4 — Conversation History Source — Acceptance Checklist

## Status

Defines completion criteria for this Unit **before any Contract Design
begins.** This Unit is governance-only; every criterion below is a
documentation or architectural-boundary criterion, not a test-passing or
build-passing one. Acceptance is Steven's, per PES-001 — this checklist
states what acceptance should verify, not that it has been granted.

---

## Criteria

- [ ] **Scope Lock approved.** `CONVERSATION_HISTORY_SOURCE_SCOPE_LOCK.md`
  defines responsibilities (Section 1), explicit exclusions (Section 2),
  the governing principle (Section 3), ownership (Section 4), lifetime
  (Section 5), and threading expectations (Section 6), each traceable to
  this Unit's own task instructions rather than invented.
- [ ] **Governing principle stated verbatim.** "Conversation History
  Source is a read boundary, not a conversation owner. It exposes
  history. It never mutates history" appears in Scope Lock Section 3, as
  a binding statement, not a paraphrase.
- [ ] **Ownership defined.** Exactly one production owner and exactly
  one production caller are named as binding requirements (Scope Lock
  Section 4) — no second owner or caller is authorised without a future
  Scope Lock revision.
- [ ] **Every excluded responsibility restated, not weakened.** Memory,
  World Model, Planner, Tool execution, Turn creation, Conversation
  mutation, Persistence policy, Summarisation, Embeddings, and Semantic
  retrieval are each confirmed excluded in Scope Lock Section 2, in the
  same closing terms Sprint 11 Unit 1's own Scope Lock used ("references
  information; does not own systems").
- [ ] **Relationship to Reasoning Context correctly scoped.** Conversation
  History Source is named as one *future* input to
  `ReasoningContextAssembler`, never itself an assembler of
  `ReasoningContext` (Implementation Plan Section 7; Scope Lock Section
  1's "nothing beyond retrieval").
- [ ] **Relationship to Memory correctly scoped.** Conversation history
  and long-term memory are stated as distinct architectural concepts
  (Scope Lock Section 2); Conversation History Source is confirmed not
  to read from, depend on, or evolve into a future `MemoryStore`.
- [ ] **Relationship to World Model correctly scoped.** Conversation
  History Source is confirmed never to expose World Model state (Scope
  Lock Section 2).
- [ ] **Relationship to Planner correctly scoped.** Conversation History
  Source is confirmed to perform no planning and never invoke
  `PlannerRuntime` (Scope Lock Section 2).
- [ ] **Existing runtime unchanged.** `ParkerRuntime`, `ConversationEngine`,
  `CommunicationConversationCoordinator`, `ConversationTurnReasoningCoordinator`,
  `ConversationReplyCoordinator`, `ReasoningContextAssembler`,
  `ModelBackedReasoningProvider`, `ResponseComposer`,
  `ReplyDeliveryCoordinator`, `IdentityService`, `ToolRegistry`,
  `ResourceRegistry`, the Permission Engine, and the Execution Pipeline
  each retain their exact existing signatures — confirmed by `git diff`
  touching no file under `src/` or `tests/`.
- [ ] **`ConversationEngine` not redesigned.** No new method is proposed
  on `ConversationEngine`'s own interface; `submitTurn`'s signature and
  sole-operation status are unchanged. Where a future read surface might
  eventually be needed, this Unit names the possibility
  (Implementation Plan Section 6) without designing or authorising it.
- [ ] **No Kotlin written.** No `.kt` file exists for Conversation
  History Source, any interface, or any test — confirmed by `git status`
  showing only the four new `.md` files this Unit adds.
- [ ] **Architectural risks recorded, not resolved.**
  `SPRINT_11_UNIT_4_ENGINEERING_CHECKPOINT.md` covers architectural,
  dependency, and sequencing risk categories, each recorded rather than
  answered by implementation detail smuggled into the Implementation Plan
  or Scope Lock.
- [ ] **The two load-bearing open questions are both recorded, not
  silently assumed answered.** (1) How a conversation is identified
  before `ConversationEngine.submitTurn` runs and a `ConversationId`
  exists. (2) That `InMemoryConversationEngine` does not yet implement
  conversation-continuity recognition at all, so there is, today, no
  continuing Conversation for this boundary to retrieve history from.
  Both appear in `SPRINT_11_UNIT_4_ENGINEERING_CHECKPOINT.md` Sections 1
  and 4, explicitly unresolved.
- [ ] **Future implementation unblocked.** A future Contract Design for
  Conversation History Source has a named responsibility, a named set of
  exclusions, a stated governing principle, and a named (not designed)
  construction question to resolve — sufficient to begin Contract Design
  work without needing to re-derive scope from this Unit's own
  Implementation Plan and Scope Lock from scratch.
- [ ] **No production or test file modified.** Confirmed by `git status`:
  only new files under `docs/implementation/` and `docs/reviews/` are
  added; no file under `src/` or `tests/` is touched.

---

## Explicitly Not Required for This Unit's Acceptance

- Any Kotlin interface, class, or test actually created in `src/` or
  `tests/`.
- A resolved answer to either open question
  `SPRINT_11_UNIT_4_ENGINEERING_CHECKPOINT.md` Section 4 records as
  unresolved — how conversation identity is established before a Turn
  exists, and whether `ConversationEngine` itself must first gain
  continuity recognition.
- A decision on Conversation History Source's own construction mechanism
  (new `ConversationEngine` read surface, independent store, or other) —
  remains future Contract Design work.
- A decision on how many prior Turns one retrieval returns, or whether
  retrieval ever spans more than one Conversation.
- Any change to `ConversationEngine`, `InMemoryConversationEngine`, or
  any other frozen component named in this Unit's own task instructions.
