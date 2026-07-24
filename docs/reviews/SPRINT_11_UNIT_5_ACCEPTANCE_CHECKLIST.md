# Sprint 11, Unit 5 — Conversation Identity Architecture — Acceptance Checklist

## Status

Defines completion criteria for this Unit **before any Contract Design
begins.** This Unit is governance-only; every criterion below is a
documentation or architectural-boundary criterion, not a test-passing or
build-passing one. Acceptance is Steven's, per PES-001 — this checklist
states what acceptance should verify, not that it has been granted.

---

## Criteria

- [ ] **Mandatory review performed and recorded before drafting.**
  `CONVERSATION_IDENTITY_ARCHITECTURE_IMPLEMENTATION_PLAN.md` Section 2
  records four distinct findings — Fact 1 (identity and ownership already
  exist, at the Conversation Engine), Fact 2 (the recognition rule is
  genuinely open across two governance stages), Fact 3 (the current
  always-new-Conversation behaviour is a disclosed Sprint-7 default, not
  an architectural ceiling), and Fact 4 (pre-Turn identity availability
  is a newly-surfaced, genuinely open question) — each traced to a
  specific, already-existing, authoritative document rather than
  asserted.
- [ ] **Conversation identity responsibilities defined.** Scope Lock
  Section 1 states exactly three responsibilities (recognising
  continuity conceptually, exposing identity to authorised readers,
  remaining independent from content) — no more.
- [ ] **Scope boundaries frozen.** Scope Lock Section 2 excludes
  conversation storage, history retrieval, Memory, World Model, Planner,
  Turn creation, message routing, tool execution, persistence
  implementation, embeddings, semantic search, and summarisation — each
  restated, not weakened, from this Unit's own task instructions.
- [ ] **Architectural principles stated, each traced to an existing
  source, not invented.** Scope Lock Section 3's five principles —
  identity not content, independence from storage, stability for the
  life of a conversation, no implied persistence, no dependency on
  future capabilities — each cite an authoritative document (Chapter 19,
  Contract Design, or this Unit's own review) rather than asserting a
  new rule.
- [ ] **Existing runtime unchanged.** `ParkerRuntime`, `ConversationEngine`,
  `CommunicationConversationCoordinator`, `ConversationTurnReasoningCoordinator`,
  `ConversationReplyCoordinator`, `ReasoningContextAssembler`,
  `ModelBackedReasoningProvider`, `ResponseComposer`,
  `ReplyDeliveryCoordinator`, `IdentityService`, `ToolRegistry`,
  `ResourceRegistry`, the Permission Engine, and the Execution Pipeline
  each retain their exact existing signatures — confirmed by `git diff`
  touching no file under `src/` or `tests/`.
- [ ] **`ConversationEngine` not redesigned, and its existing ownership
  not reassigned.** Scope Lock Section 4 restates, rather than
  transfers, `19-conversation-engine.md` Section 4's own ownership of
  conversation continuity recognition. No new method is proposed on
  `ConversationEngine`'s interface; `submitTurn`'s signature and
  sole-operation status are unchanged.
- [ ] **No Kotlin written.** No `.kt` file exists for Conversation
  Identity, any interface, or any test — confirmed by `git status`
  showing only the four new `.md` files this Unit adds.
- [ ] **No production or test file modified.** Confirmed by `git status`:
  only new files under `docs/implementation/` and `docs/reviews/` are
  added; no file under `src/` or `tests/` is touched.
- [ ] **Risks, assumptions, and unresolved questions recorded, not
  resolved.** `SPRINT_11_UNIT_5_ENGINEERING_CHECKPOINT.md` covers
  architectural, dependency, and sequencing risk categories, a
  dedicated assumptions-requiring-validation section, and a dedicated
  unresolved-questions section, each recorded rather than answered by
  implementation detail smuggled into the Implementation Plan or Scope
  Lock.
- [ ] **Both load-bearing open questions are recorded, not silently
  assumed answered.** (1) The exact conversation-continuation
  recognition rule (`19-conversation-engine.md` Section 13 Item 3,
  still open). (2) How, or whether, conversation identity can be made
  available before `ConversationEngine.submitTurn` runs, without
  duplicating or redesigning its recognition authority. Both appear in
  `SPRINT_11_UNIT_5_ENGINEERING_CHECKPOINT.md` Section 5, explicitly
  unresolved.
- [ ] **Conversation History Source implementation unblocked, not
  designed further.** This Unit gives a future Conversation History
  Source Contract Design a named prerequisite concept and a named set of
  architectural properties to depend on (Scope Lock Section 9), without
  itself designing Conversation History Source's own interface — that
  remains Sprint 11 Unit 4's own, separately-scoped future work.
- [ ] **Future Memory architecture preserved.** Scope Lock Section 11
  restates that conversation identity and long-term memory are distinct
  architectural concepts, and that Memory remains a future Unit this
  document does not anticipate or design toward.
- [ ] **Future World Model and Planner architecture preserved.** Scope
  Lock Sections 12 and 13 confirm conversation identity exposes no
  World Model state and performs no planning.
- [ ] **Every responsibility has exactly one owner; every exclusion
  remains excluded.** Cross-checked directly: Section 1's three
  responsibilities each name a single owner or a single conceptual
  contribution (this document's own, for the properties; the
  Conversation Engine's, for recognition); Section 2's eleven exclusions
  each remain excluded with no conditional carve-out.

---

## Explicitly Not Required for This Unit's Acceptance

- Any Kotlin interface, class, or test actually created in `src/` or
  `tests/`.
- A resolved answer to either open question
  `SPRINT_11_UNIT_5_ENGINEERING_CHECKPOINT.md` Section 5 records as
  unresolved — the recognition rule itself, and pre-Turn identity
  availability.
- A decision on the sequencing question between resolving the
  recognition rule and resolving pre-Turn availability
  (`SPRINT_11_UNIT_5_ENGINEERING_CHECKPOINT.md` Section 3).
- Any change to `ConversationEngine`, `InMemoryConversationEngine`, or
  any other frozen component named in this Unit's own task instructions.
- A Contract Design for either Conversation Identity or Conversation
  History Source — both remain future, separately-scoped work.
