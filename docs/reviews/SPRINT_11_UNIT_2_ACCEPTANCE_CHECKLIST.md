# Sprint 11, Unit 2 — Production Reasoning Context Contract Design — Acceptance Checklist

## Status

Defines completion criteria for this Unit **before any implementation
begins.** This Unit is contract-only; every criterion below is a
documentation or contract-boundary criterion, not a test-passing or
build-passing one. Acceptance is Steven's, per PES-001 — this checklist
states what acceptance should verify, not that it has been granted.

---

## Criteria

- [ ] **Contract complete.** `PRODUCTION_REASONING_CONTEXT_CONTRACT_DESIGN.md`
  defines the Assembler's interface, responsibilities, dependencies,
  ownership boundaries, production lifecycle, failure behaviour, and
  immutability expectations (Sections 3–8), each traceable to a specific
  Scope Lock section it implements rather than reinterprets.
- [ ] **Ownership preserved.** The Reasoning Context Assembler still
  constructs; the Production Composition Root still alone invokes
  (Contract Design Section 9) — identical to Scope Lock Section 4, not
  revised.
- [ ] **Scope Lock unchanged.** `PRODUCTION_REASONING_CONTEXT_SCOPE_LOCK.md`
  is not modified by this Unit. Every reference to it above cites,
  rather than amends, its existing section numbers.
- [ ] **Projection principle reinforced, not diluted.** Contract Design
  Section 10 confirms every dependency, every input, and every failure
  path traces back to Scope Lock Section 3; no section grants the
  Assembler a new source of truth.
- [ ] **Every dependency justified individually.** Contract Design
  Section 4.1 answers "why does this component need this dependency?"
  for `IdentityService` and `ToolRegistry` separately; no dependency is
  included on a "might be useful" basis. Section 4.2 confirms Memory,
  World Model, and Conversation History are named boundaries only, absent
  from the illustrative constructor.
- [ ] **Conversation history abstraction defined, not solved.** The
  Assembler's need for prior-turn content is named (Contract Design
  Section 4.2, "Conversation History Source"); how many Turns, retrieval
  rules, and summarisation rules all remain unresolved, matching
  `SPRINT_11_UNIT_1_ENGINEERING_CHECKPOINT.md` Section 3's own unresolved
  item, not quietly answered here.
- [ ] **Memory and World Model boundaries defined, not designed.**
  Contract Design Section 4.2 names both; neither retrieval, storage,
  nor indexing is specified for either.
- [ ] **Sequence shows assembly, immutability, and hand-off points
  explicitly.** `PRODUCTION_REASONING_CONTEXT_SEQUENCE.md` Section 3
  (assembly), Section 4 (immutability), and Section 5 (structural
  hand-off) each name an exact step in the real, frozen production call
  chain — not a generic or illustrative flow.
- [ ] **No Kotlin written.** Every interface and class shown in the
  Contract Design (`ReasoningContextAssembler`, `DefaultReasoningContextAssembler`)
  is explicitly labelled illustrative; no `.kt` file exists for either.
- [ ] **Existing runtime contracts unchanged.** `ParkerRuntime`,
  `ConversationReplyCoordinator`, `CommunicationConversationCoordinator`,
  `ConversationTurnReasoningCoordinator`, `ReplyDeliveryCoordinator`,
  `ModelBackedReasoningProvider`, `ResponseComposer`, the Permission
  Engine, the Execution Pipeline, the Tool Registry, the Identity
  Service, and the Resource Registry each retain their exact existing
  signatures — confirmed by `git diff` touching no file under `src/` or
  `tests/`.
- [ ] **Contract principles preserved.** Deterministic, stateless,
  side-effect-free, and "produces one immutable `ReasoningContext` from
  one inbound request" are each stated as binding contract terms
  (Contract Design Section 5), not aspirational description.
- [ ] **Exclusions restated, not weakened.** The Assembler is confirmed,
  in contract terms, not to cache, persist, mutate Memory, mutate the
  World Model, invoke Tools, invoke Planner, invoke the Permission
  Engine, or invoke the Execution Pipeline (Contract Design Section 5) —
  matching Scope Lock Section 2 exactly.
- [ ] **Risks recorded, not resolved.** `SPRINT_11_UNIT_2_ENGINEERING_CHECKPOINT.md`
  covers contract, dependency, lifecycle, ownership, future Memory, and
  future World Model risk categories, each recorded rather than answered
  by implementation detail smuggled into the Contract Design or Sequence
  documents.
- [ ] **Future implementation unblocked.** A future Implementation Plan
  for the Reasoning Context Assembler has a concrete interface, a
  justified dependency list, and a named (not designed) set of future
  integration boundaries to build from — sufficient to begin
  implementation Contract-Design-conformant, without needing to
  re-derive ownership, lifecycle, or scope from Unit 1 and Unit 2 from
  scratch.
- [ ] **`IMPLEMENTATION_GAPS.md` #53 updated only after acceptance.**
  Consistent with Unit 1's own standing rule: the gap's `ReasoningContext`
  assembly item is not marked "contract designed" until Steven confirms
  this checklist is satisfied.

---

## Explicitly Not Required for This Unit's Acceptance

- Any Kotlin interface, class, or test actually created in `src/` or
  `tests/`.
- A resolved answer to any item
  `SPRINT_11_UNIT_1_ENGINEERING_CHECKPOINT.md` Section 3 or
  `SPRINT_11_UNIT_2_ENGINEERING_CHECKPOINT.md` records as unresolved —
  conversation-history selection, prose-rendering strategy, Memory
  integration shape, World Model integration shape, and caching
  decisions all remain future Contract Design or Implementation Plan
  work.
- A dedicated `PipelineStage` value for context-assembly failures, or
  any other change to `src/composition/ParkerRuntimeOutcome.kt` or any
  other frozen production file.
