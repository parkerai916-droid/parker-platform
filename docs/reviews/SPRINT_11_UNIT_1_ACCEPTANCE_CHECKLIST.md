# Sprint 11, Unit 1 — Production Reasoning Context — Acceptance Checklist

## Status

Defines completion criteria for this Unit **before any Kotlin exists.**
This Unit is architecture-only; every criterion below is a documentation
or architectural-boundary criterion, not a test-passing or
build-passing one. Acceptance is Steven's, per PES-001 — this checklist
states what acceptance should verify, not that it has been granted.

---

## Criteria

- [ ] **Scope Lock approved.** `PRODUCTION_REASONING_CONTEXT_SCOPE_LOCK.md`
  reviewed and accepted by Steven as the governing document for all
  future Reasoning Context Assembler work.
- [ ] **Ownership defined.** Exactly one component (the Reasoning
  Context Assembler) is named as the constructor of `ReasoningContext`;
  exactly one component (the Production Composition Root) is named as
  its sole invoker. No ambiguity or shared ownership remains (Scope Lock
  Section 4).
- [ ] **Responsibilities frozen.** What belongs inside Reasoning Context
  (Scope Lock Section 1, organised into Conversation, Operational, and
  future Retrieved Context categories) and what explicitly does not
  (Section 2) are both stated with individual justification, not
  asserted by category alone.
- [ ] **Projection principle stated.** Reasoning Context is documented as
  a projection, never a source of truth — every item it carries
  originates elsewhere, and Reasoning Context owns none of it (Scope
  Lock Section 3). Confirmed as a standing principle future Units must
  satisfy, not merely a description of today's item list.
- [ ] **Construction path defined.** The Implementation Plan (Section 5)
  states which component invokes assembly and when, without specifying
  Kotlin — sufficient for a future Contract Design to begin from, not a
  substitute for one.
- [ ] **Lifecycle defined.** Creation, mutation (none — immutable after
  construction), and disposal are each stated (Scope Lock Section 5),
  consistent with `reasoning-context.md`'s own ephemerality principle.
- [ ] **Threading expectations documented.** Sharing, mutability, and
  coroutine expectations are each stated (Scope Lock Section 6),
  sufficient to prevent a future implementation from introducing
  concurrent-mutation hazards this document already rules out by design.
- [ ] **No existing frozen component redesigned.** Every coordinator,
  the Reasoning Provider, and the Production Composition Root's own
  existing signatures are confirmed unchanged (Implementation Plan
  Section 2 and Section 7). No Scope Lock for any existing component is
  reopened or amended.
- [ ] **Existing runtime unaffected.** The frozen pipeline
  (`Communication Intake -> Conversation Engine ->
  CommunicationConversationCoordinator ->
  ConversationTurnReasoningCoordinator -> ModelBackedReasoningProvider ->
  ResponseComposer -> ReplyDeliveryCoordinator ->
  ConversationReplyCoordinator`) is confirmed structurally untouched;
  the Reasoning Context Assembler is confirmed to sit outside it,
  upstream, owned by the Production Composition Root alone.
- [ ] **Memory and World Model boundaries stated, not designed.** Both
  are named as future input sources only (Implementation Plan Section
  8); neither's internals, storage, or retrieval mechanism is specified
  by this Unit.
- [ ] **Planner/Goal boundary preserved.** Reasoning Context may carry
  Goal-relevant prose exactly as it does today; no planning, routing, or
  Planner state is introduced or implied (Scope Lock Section 2).
- [ ] **Constitutional invariants preserved.** "Cognition proposes.
  Trust authorises. Runtime executes." remains unaffected; available
  tool descriptions are confirmed to carry no authorisation
  implication (Scope Lock Section 7).
- [ ] **Unresolved questions recorded, not answered.** Every open
  question in `SPRINT_11_UNIT_1_ENGINEERING_CHECKPOINT.md` Section 3 is
  confirmed genuinely open, not quietly resolved by implementation
  detail smuggled into the Plan or Scope Lock.
- [ ] **No implementation begun.** No Kotlin file created or modified;
  no `build.gradle.kts` change; no test written; `git status --short`
  shows only the four new documentation files this Unit produces.
- [ ] **`IMPLEMENTATION_GAPS.md` #53 updated only after acceptance.**
  The gap's `ReasoningContext` assembly item is not marked
  "assigned"/updated until Steven confirms this checklist is satisfied —
  consistent with this repository's own standing rule not to update
  acceptance-adjacent documentation ahead of confirmed acceptance.

---

## Explicitly Not Required for This Unit's Acceptance

- A Contract Design for the Reasoning Context Assembler.
- Any Kotlin interface, class, or test.
- A decision on Memory's or the World Model's own integration shape.
- A decision on prose-rendering wording, conversation-history bounding,
  or any other item `SPRINT_11_UNIT_1_ENGINEERING_CHECKPOINT.md` Section
  3 records as unresolved.
