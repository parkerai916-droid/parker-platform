# Sprint 11, Unit 5 (Contract Design Stage) — Acceptance Checklist

## Status

**Third revision, following conditional approval.** Architectural review
approved the corrected authority/propagation design (prior revision)
subject to confirming four concurrency and failure guarantees are explicit
in the documents: resolution atomicity, idempotence while active, no
re-resolution inside `submitTurn`, and resolution failure stopping the
entire pipeline. These now appear as Contract Design Section 5.1's four
Binding Guarantees, referenced throughout the Sequence document. This
checklist adds criteria verifying each is present as a stated contract
term, not merely implied. Contract-Design-only; every criterion is a
documentation/architectural criterion, not a test-passing or
build-passing one. Acceptance is Steven's, per PES-001.

---

## Criteria

- [ ] **Exactly one authority is named for both continuity recognition and
  `ConversationId` selection.** Contract Design Section 4 names
  `ConversationEngine` alone — not divided with `ParkerRuntime`, which only
  invokes the authority, never decides.
- [ ] **Exactly one authoritative decision occurs per inbound message.**
  Contract Design Section 5 and Sequence Sections 2-3 show
  `resolveConversationId` called exactly once, before `ReasoningContext`
  assembly; `submitTurn` consumes its result rather than re-deciding.
- [ ] **Exactly one identifier is used throughout the pipeline.** Sequence
  document Section 5 (Cross-Sequence Summary) confirms the same
  `ConversationId` value flows unchanged from resolution through envelope
  construction, Assembler invocation, coordinator forwarding, and Turn
  creation, for both the continuing and new paths.
- [ ] **Propagation from resolution through Turn creation is unchanged —
  the value is carried, never recomputed.** Contract Design Section 5
  states explicitly that `submitTurn` "no longer decides identity at all"
  and receives the value as a required parameter.
- [ ] **No component independently re-implements or re-evaluates the
  recognition rule.** Confirmed: `ParkerRuntime` never mints or guesses an
  identifier (Contract Design Section 4); `ConversationEngine.submitTurn`
  no longer contains any recognition logic of its own (Section 5);
  `ReasoningContextAssembler` never touches `ConversationEngine` or the
  recognition rule (Section 12).
- [ ] **No unresolved mismatch is possible between `ReasoningContext` and
  the created Turn's own `ConversationId`.** Contract Design Section 5,
  step 7, states this is "structurally incapable of differing," not merely
  expected to match — because `submitTurn` has no independent means of
  producing a different value.
- [ ] **The Derived and Resolved identity models are not mixed.** Contract
  Design Section 3 selects Resolved identity exclusively and states
  explicitly why Derived identity is rejected outright, not merely
  disfavoured.
- [ ] **The `ConversationEngine` interface question is answered honestly,
  not preserved for its own sake.** Contract Design Section 6 states
  plainly that the interface changes — one new method, one additive
  parameter — and explains why this is additive extension, not redesign,
  under Frozen Fact 7.
- [ ] **State requirements are distinguished by purpose, not asserted
  wholesale.** Contract Design Section 7 separately addresses identity
  selection, Turn retention, active-Conversation tracking, termination,
  reopening, and replacement-after-expiry, and states explicitly what
  state is *not* needed for (computing an identifier from nothing).
- [ ] **`ConversationEngine` ownership of Conversation membership is
  preserved.** Contract Design Section 12: `ConversationEngine` remains
  the only component that constructs a `Conversation`/`Turn`, mutates
  `turnIds`, mints a `ConversationId`, or determines continuity.
- [ ] **`ConversationId` remains stable for a logical Conversation's own
  lifetime, and distinct from `CorrelationId`.** Contract Design Section 1,
  re-confirmed unchanged from the prior revision.
- [ ] **`ReasoningContextAssembler` remains side-effect free and has no
  direct dependency on `ConversationEngine`.** Contract Design Section 12:
  the Assembler receives an already-resolved value from `ParkerRuntime`; it
  never calls `ConversationEngine` itself.
- [ ] **No Memory, Planner, or World Model dependency is introduced.**
  Unchanged from the prior revision; the continuity key and its resolution
  touch none of the three.
- [ ] **No semantic or content-based recognition is used.** Unchanged: the
  continuity key is `channelId` and `senderPrincipalId` only, never
  `InboundOwnerMessage.text`.
- [ ] **No caller may mint an arbitrary `ConversationId`.** Contract Design
  Section 5: minting occurs only inside `ConversationEngine.resolveConversationId`,
  on the no-open-Conversation branch.
- [ ] **The revised sequence document shows, for new, continuing, and
  failure paths, all eight required elements** (inbound message received;
  continuity key established; one authoritative identity decision;
  `ConversationId` propagated; `ReasoningContext` assembled; Turn created
  with that exact identifier; `ConversationEngine` accepts and records
  membership; reasoning and delivery continue) **with no second derivation
  or second identity decision in any path.** Confirmed against
  `CONVERSATION_CONTINUITY_SEQUENCE.md` Sections 2-4.
- [ ] **Resolution atomicity is a stated contract guarantee, not an
  implementation preference.** Contract Design Section 5.1 Guarantee 1:
  two concurrent resolutions for the same continuity key must never each
  mint a distinct `ConversationId`; at most one is ever active per key.
- [ ] **Idempotence while active is a stated contract guarantee.** Section
  5.1 Guarantee 2: repeated resolution for the same, still-active
  continuity key returns the identical `ConversationId` every time,
  making "stable for the life of a Conversation" operational, not
  decorative.
- [ ] **`submitTurn` is contractually forbidden from re-resolving, and its
  rejection behaviour for invalid/unknown/stale identifiers is stated.**
  Section 5.1 Guarantee 3: `submitTurn` either accepts a supplied
  identifier it recognises or rejects it observably; it never
  independently computes or substitutes another.
- [ ] **Resolution failure is contractually required to stop the entire
  pipeline.** Section 5.1 Guarantee 4, and Sequence document Sections 4a-4b:
  no `ReasoningContext` is assembled (or, if already assembled, is not
  used further), no `Turn` is created, no reasoning occurs, and no
  response is delivered, on any resolution or submission failure.
- [ ] **The wider migration footprint is disclosed, not minimised.**
  `SPRINT_11_UNIT_5_CONTRACT_ENGINEERING_CHECKPOINT.md` Section 4 names
  four call sites (not one) whose signatures change, and the tests each
  will require.
- [ ] **No Kotlin written.** No `.kt` file exists for this Unit — confirmed
  by `git status` showing only the same four `.md` files this stage
  revises, no new files added.
- [ ] **No production or test file modified.** Confirmed by `git status`:
  only the four documents named in this Unit's own instructions are
  changed; no file under `src/` or `tests/` is touched.
- [ ] **No frozen source document is edited.** `19-conversation-engine.md`
  and `CONVERSATION_ENGINE_CONTRACT_DESIGN.md` remain cited, not modified.
- [ ] **Implementation unblocked, and only now, on this revision.** The
  prior revision's authority/propagation contradiction — the specific
  reason acceptance was withheld — is resolved, not merely re-described,
  by Sections 4-6 of the revised Contract Design.

---

## Explicitly Not Required for This Stage's Acceptance

- Any Kotlin interface, class, or test actually created or modified in
  `src/` or `tests/`.
- The exact internal representation of `ConversationEngine`'s new
  continuity-key state, or its concurrency mechanism.
- A termination, reopening, or replacement-after-expiry rule
  (`19-conversation-engine.md` Section 13 Item 4) — remains open.
- A Contract Design for Conversation History Source itself — remains
  Sprint 11 Unit 4's own, separately-scoped future work.
