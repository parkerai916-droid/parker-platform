# Conversation History Source — Scope Lock

## Status

**Sprint 11, Unit 4. This is the governing document for Conversation
History Source.** Once frozen, a future Contract Design must implement
what this document defines, not redesign it. Companion to
`CONVERSATION_HISTORY_SOURCE_IMPLEMENTATION_PLAN.md`, which supplies the
architectural reasoning this document's decisions rest on. Governance
only — no Kotlin is created or modified by this document.

---

## 1. Responsibilities — What Conversation History Source Owns

Conversation History Source owns exactly one responsibility:
**retrieving an already-existing, read-only excerpt of prior conversation
history.**

That single responsibility decomposes into what it must be able to
answer, in prose, without presupposing a Kotlin shape:

- **Prior Turns.** Given some means of identifying a conversation
  (Section on Ownership/Lifetime below; not resolved by this document —
  see `CONVERSATION_HISTORY_SOURCE_ENGINEERING_CHECKPOINT`-equivalent,
  i.e. `SPRINT_11_UNIT_4_ENGINEERING_CHECKPOINT.md`), Conversation
  History Source can return the prior Turns already belonging to it, as
  `ConversationEngine`'s own owned state (`Conversation.turnIds`,
  Architecture Section 4) already records them.
- **Nothing beyond retrieval.** It does not decide how many Turns are
  relevant, does not rank them, does not summarise them, and does not
  reshape them into prose — any such transformation belongs, at most, to
  whichever future component consumes what Conversation History Source
  returns (a future Reasoning Context Assembler revision), never to
  Conversation History Source itself.

Conversation History Source retrieves conversation history. Nothing
more.

---

## 2. Explicit Exclusions — What It Must Never Own

Restated in the same closing form
`PRODUCTION_REASONING_CONTEXT_SCOPE_LOCK.md` Section 2 already
established: Conversation History Source references information. It
does not own systems.

- **Memory.** A distinct, future architectural concept
  (`docs/architecture/reasoning-context.md`'s own "what Parker has
  learned" layer). Conversation History Source must never evolve into
  Memory, must never read from a future `MemoryStore`, and must never be
  mistaken for a durable-knowledge boundary. Conversation history and
  long-term memory are distinct architectural concepts; this document
  exists specifically to preserve that separation, not blur it.
- **World Model.** A distinct, future architectural concept ("what
  Parker currently believes," live and frequently changing).
  Conversation History Source must never expose World Model state,
  and must never be extended to read it.
- **Planner.** Conversation History Source performs no planning. It
  never invokes `PlannerRuntime`, never constructs a `PlanningRequest`,
  and never reasons about what a Conversation's history implies should
  happen next.
- **Tool execution.** No dependency on `ToolRegistry.resolve`, any
  `Tool` handle, `ToolInvocationBinding`, or the Execution Pipeline
  exists or is ever introduced.
- **Turn creation.** Conversation History Source never constructs a
  `Turn`. That remains `ConversationEngine.submitTurn`'s own, sole,
  unchanged responsibility.
- **Conversation mutation.** Conversation History Source never creates,
  amends, merges, or closes a `Conversation`. It never calls anything
  equivalent to `submitTurn`, and no future revision may grant it that
  capability without first revising this Scope Lock.
- **Persistence policy.** Conversation History Source does not decide
  how long a Conversation's history is retained, does not implement
  storage, and does not own a durability guarantee. Persistence, if any,
  remains whichever component already owns `ConversationEngine`'s own
  state today or in the future.
- **Summarisation.** Conversation History Source does not condense,
  compress, or paraphrase history. Whatever it returns is retrieved, not
  authored.
- **Embeddings.** Conversation History Source does not vectorise,
  embed, or index conversation content in any form.
- **Semantic retrieval.** Conversation History Source does not rank,
  filter, or select history by relevance, similarity, or any other
  semantic criterion. Any such capability, if ever needed, belongs to a
  future, separately-scoped and separately-justified component — not
  folded into this boundary "since it's already there," mirroring
  `PRODUCTION_REASONING_CONTEXT_CONTRACT_DESIGN.md` Section 5's identical
  warning against scope-creep for the Reasoning Context Assembler's own
  Memory/World Model dependencies.

---

## 3. Governing Principle

**Conversation History Source is a read boundary, not a conversation
owner. It exposes history. It never mutates history.**

Every item Conversation History Source can ever return has exactly one
other home: `ConversationEngine`'s own owned `Conversation`/`Turn` state
(Architecture Section 4). Conversation History Source originates
nothing and never becomes canonical. If a Conversation History Source
excerpt and `ConversationEngine`'s own current state ever disagree,
`ConversationEngine`'s own state is correct and the excerpt is simply
stale — the identical projection discipline
`PRODUCTION_REASONING_CONTEXT_SCOPE_LOCK.md` Section 3 already
established for `ReasoningContext` itself, restated here for this
boundary.

---

## 4. Ownership

- **Exactly one production owner.** Whichever production composition
  root exists at the time a future implementation Unit is authorised
  (today, `parker.composition.ParkerRuntime`) constructs Conversation
  History Source, exactly once, at startup — mirroring
  `PRODUCTION_REASONING_CONTEXT_SCOPE_LOCK.md` Section 4's identical
  ownership shape for the Reasoning Context Assembler.
- **Exactly one production caller.** A future Reasoning Context
  Assembler revision, or the composition root on its behalf — not
  decided further here (an open question this Scope Lock records rather
  than resolves; see `SPRINT_11_UNIT_4_ENGINEERING_CHECKPOINT.md`). No
  coordinator, no Reasoning Provider, and no other runtime component may
  become a second caller without a future Scope Lock revision.

---

## 5. Lifetime

- **Construction.** Once, at startup, alongside every other stateless
  collaborator a future composition root builds. Construction failure
  is reported the same way every other collaborator's construction
  failure already is (`ParkerRuntime`'s own existing `stage()` pattern) —
  not decided further here, since this document does not describe
  implementation.
- **Use.** Read-only, per request. Each read is independent; nothing
  observed or returned by one read is retained for, or influences, the
  next.
- **Disposal.** A returned history excerpt is discarded once its caller
  is finished with it. Conversation History Source itself is never
  torn down and reconstructed per request — a natural consequence of
  Statelessness, below.

---

## 6. Threading

- **Sharing.** A single Conversation History Source instance is shared
  across every concurrent request the runtime handles, exactly as every
  other stateless, constructed-once collaborator in this runtime already
  is.
- **Immutability.** Whatever Conversation History Source returns is
  immutable from the moment it is returned — mirroring
  `PRODUCTION_REASONING_CONTEXT_SCOPE_LOCK.md` Section 6's identical
  expectation for `ReasoningContext` itself. A given returned excerpt is
  never shared across more than one caller's own handling of one
  request.
- **Coroutine expectations.** Any operation Conversation History Source
  exposes is expected to be `suspend`-declared, consistent with every
  other read-only dependency this runtime already relies on
  (`IdentityService.resolve`, `ToolRegistry.listAll`, both `suspend`
  today) — not decided as a final interface shape here, only as an
  expectation a future Contract Design should meet or explicitly
  revise.

---

## 7. Relationship to the Constitution

`docs/architecture/parker-constitution.md`: "Parker owns authority.
Modules provide capability" (line 18/40) and "Cognition proposes. Trust
authorises. Runtime executes" (line 48/52). Conversation History Source
is capability, not authority — it proposes nothing, authorises nothing,
and executes nothing. It is a read boundary a future Cognition-adjacent
component may draw on, exactly as `IdentityService` and `ToolRegistry`
already are for the Reasoning Context Assembler
(`PRODUCTION_REASONING_CONTEXT_CONTRACT_DESIGN.md` Section 4.1) — never
itself a source of proposal, authority, or execution.
