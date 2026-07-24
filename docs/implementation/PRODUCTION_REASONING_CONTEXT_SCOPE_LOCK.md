# Production Reasoning Context — Scope Lock

## Status

**Sprint 11, Unit 1. Governing document.** This Scope Lock is the
authoritative boundary for Production Reasoning Context. Any future
Contract Design, Implementation Plan, or implementation for the
Reasoning Context Assembler this document names must conform to it or
formally amend it first — the same discipline every prior Scope Lock in
this repository has held itself to. Architecture only. No Kotlin exists
yet; none is authorised by this document.

Governing documents, and this Unit's relationship to each, are listed in
full in `PRODUCTION_REASONING_CONTEXT_IMPLEMENTATION_PLAN.md` Section 1;
not repeated here.

---

## 1. What Belongs Inside Reasoning Context

Each item justified against `reasoning-context.md`'s own definition
("what matters for the current task," assembled from Memory, the World
Model, and the current request) and against what the frozen pipeline
already has available at the point the Reasoning Context Assembler would
run.

Conceptually, every item below falls into one of three categories — a
categorisation for architectural clarity only; it introduces no new
Kotlin type and does not change `ReasoningContext`'s own flat
`entries: List<String>` shape (this section's closing paragraph, below).
**Conversation Context** is what the current message and Conversation
themselves supply. **Operational Context** is what the runtime already
knows about how and where the request arrived, independent of any
Conversation content. **Retrieved Context** is whatever a future
Reasoning Context Assembler implementation draws from Memory or the
World Model — named here so those future integrations have a clear
category to populate rather than an undifferentiated list to append to;
it is empty today, since neither integration exists yet (Implementation
Plan Section 8). Each item below is labelled with the category it
belongs to.

- **Current conversation** *(Conversation Context).* The Turn under
  reasoning, and whatever
  bounded slice of prior Turns in the same Conversation
  `reasoning-context.md`'s own task-scoping principle justifies
  including. This is the single clearest instance of "what matters for
  the current task" the architecture names.
- **Participant identities** *(Conversation Context).* Who is present in
  the Conversation being reasoned about, at the level `IdentityService`
  already exposes (`PrincipalId`, `displayName`) — not a Memory-sourced
  profile, only who is structurally part of this task.
- **Requesting principal identity** *(Conversation Context).* The
  identity of the Principal the current request is attributed to —
  today always the configured owner Principal, already resolved by the
  Production Composition Root at startup
  (`ParkerRuntimeConfig.ownerPrincipalId`) — included because Trust
  authorisation and response delivery both already depend on knowing
  this identity, and a Reasoning Provider reasoning about the request
  benefits from the same fact, not a new one. Named "requesting
  principal," not "owner," deliberately: today's runtime has exactly one
  requesting Principal type in practice, but nothing in this wording
  couples Reasoning Context's own architecture to that remaining true
  forever. This does not redesign `IdentityService` or its
  `PrincipalType`s — it only avoids assuming, in Reasoning Context's own
  documentation, a permanence `IdentityService` itself never claimed.
- **Current request** *(Conversation Context).* The content of the
  inbound message itself (`InboundOwnerMessage.text` and its structural
  fields) — the task's own immediate subject matter, already the first
  input `Turn` carries.
- **Active communication channel** *(Operational Context).* Which
  Module/channel the message arrived on
  (`InboundOwnerMessage.channelId`) — relevant because a reasoning
  provider's proposal may reasonably depend on what channel a reply will
  need to travel back through, without this component choosing or
  constructing that reply.
- **Available tool descriptions** *(Operational Context).* What a Tool
  is named and described as doing (`ToolDescriptor`'s own public shape)
  — not how it is implemented, not a live handle to invoke it, and never
  framed as already authorised. Named "available," not "authorised,"
  deliberately: reasoning must know what capability *exists*, never what
  Trust *will* approve for this particular request — approval is decided
  later, per-request, by the Permission Engine alone (Section 7, below).
  Justified because a reasoning provider proposing a `Goal` or `Reply`
  benefits from knowing what capability exists in principle, exactly as
  the Parker Constitution's "Cognition proposes" already assumes
  cognition is aware of what it might eventually propose using — while
  "Trust authorises" remains entirely untouched by this awareness
  (Section 2, below, is explicit that no authorisation shortcut is
  created here).
- **Current time** *(Operational Context).* The message's own
  `timestamp`, already present on `InboundOwnerMessage` — included
  because a Reasoning Provider cannot correctly interpret relative or
  time-sensitive language in the current request ("in an hour,"
  "tomorrow," "right now") without knowing what time "now" is; this is a
  genuine reasoning input, not a diagnostic one. `CorrelationId` is
  deliberately **not** included: it identifies a request for tracing and
  logging purposes only, and reasoning about the current request is not
  improved by knowing its own correlation identifier. Operational
  diagnostics of this kind belong to `RuntimeEventLogger` and
  `ParkerLogger`, never to Reasoning Context (excluded explicitly,
  Section 2).

No item above is a new Kotlin field or a new contract. Every one is
already-available data at the point in the pipeline where assembly would
occur; this Scope Lock only says that each is in-scope to be *rendered*
into a `ReasoningContext` entry, not that any new type carries it
structurally. `ReasoningContext` itself remains exactly
`data class ReasoningContext(val entries: List<String>)` — a flat,
opaque list of prose, per `REASONING_PROVIDER_CONTRACT_DESIGN.md`
Section 2, unchanged.

---

## 2. What Explicitly Does NOT Belong

- **The Memory database itself.** The Reasoning Context Assembler may
  draw *from* Memory in a future implementation Unit, but Reasoning
  Context does not become, hold a reference to, or expose Memory as a
  system. `reasoning-context.md`'s own separation principle: "If Memory
  and the World Model were not filtered down before reaching a reasoning
  provider, every task would be reasoned over with irrelevant, excess
  context."
- **The World Model.** Same reasoning as Memory, above — a future input
  source, never owned or exposed wholesale.
- **Planner state.** Goal routing and planning are explicitly a future
  Unit's responsibility (`IMPLEMENTATION_GAPS.md` #53's own remaining
  open items). Reasoning Context may carry a prose entry describing the
  current request in terms that could inform a future `Goal`, but it
  does not track, hold, or expose Planner state of any kind.
- **Tool implementations.** Section 1 permits Tool *descriptions*
  (name, purpose); it explicitly excludes any live handle to a Tool, its
  `ToolInvocationBinding`, or anything capable of invoking one. Cognition
  proposes; it must never be handed a means to act.
- **Execution results.** Nothing about a past or in-flight
  `ExecutionResult` belongs here. Reasoning Context is assembled before
  reasoning happens, for the purpose of reasoning about the current
  request; execution outcomes belong to the runtime's own operational
  history, not to this task-scoped working set.
- **Caches.** Reasoning Context is not a cache and must not become one.
  It is constructed fresh, per message, per Section 5 (Lifetime), below.
- **Persistence.** Reasoning Context is never written to disk, a
  database, or any durable store. Only Parker's deliberate memory policy
  — a separate, future, governed mechanism `reasoning-context.md`
  already names — may promote anything from a task into durable storage.
- **Logging.** Reasoning Context is reasoning input, not a diagnostic
  record. Nothing about it is written to `ParkerLogger` beyond what the
  runtime already logs about message handling generally (message
  accepted, reasoning completed) — the same discipline
  `RuntimeEventLogger` already applies elsewhere: never logging
  conversation content itself.
- **Configuration.** `ParkerRuntimeConfig` is a startup-time concern
  entirely outside Reasoning Context's own per-message scope. Reasoning
  Context does not carry configuration values, timeouts, endpoints, or
  anything from the composition root's own construction-time state.

The general principle, restated from the task itself and consistent with
Section 1's own framing: **Reasoning Context references information. It
does not own systems.** Every item excluded above is excluded because
owning it — rather than drawing a bounded, task-scoped excerpt from it —
would make Reasoning Context a second home for state that already has
exactly one home elsewhere in this architecture.

---

## 3. Reasoning Context Is a Projection, Not a Source of Truth

Every item Section 1 admits already has exactly one home elsewhere in
this architecture — `InboundOwnerMessage` on the message itself,
`PrincipalId`/`displayName` on `IdentityService`, `ToolDescriptor` on the
Tool Registry, and, once integrated, Memory and the World Model for
whatever each of them eventually contributes as Retrieved Context.
Reasoning Context originates nothing. It is assembled by reading from
those existing homes, rendering a task-scoped excerpt of each into
prose, and discarding that excerpt once the task concludes (Section 5,
Lifetime). It never becomes the canonical record of any fact it
carries: if a Reasoning Context entry and its own source of truth ever
disagree, the source of truth is correct and the Reasoning Context entry
is simply stale, to be replaced on the next assembly, never reconciled
or treated as authoritative in its own right.

This restates, as a standing principle rather than a single closing
line, what Section 2 already concludes narrower ("Reasoning Context
references information. It does not own systems."). It is elevated to
its own principle here because it must govern every future Unit that
touches Reasoning Context, not only the items this Scope Lock happens to
admit today. Any future category, source, or item considered for
inclusion must satisfy it: does this genuinely originate elsewhere, and
does Reasoning Context merely project a task-scoped excerpt of it,
never becoming its owner? An item that fails this test does not belong
in Reasoning Context, regardless of how useful it might otherwise seem
to reasoning quality.

---

## 4. Ownership

**Exactly one component constructs Reasoning Context in production: the
Reasoning Context Assembler.** This is a new component, not yet
implemented, named and bounded by this Scope Lock, not by any Kotlin
that exists today.

**Exactly one component invokes it: the Production Composition Root**
(`ParkerRuntime`). This follows directly from Section 2 of the
Implementation Plan: every frozen coordinator between
`ConversationReplyCoordinator` and the Reasoning Provider is
structurally incapable of assembling a `ReasoningContext` (no dependency
slot exists, confirmed against each one's own Scope Lock), and
`ParkerRuntime` is the only frozen component positioned before that
chain begins — it already owns the one production entry point
(`submitOwnerMessage`) every inbound message passes through, and it
already threads a `reasoningContext` parameter down the same chain,
today defaulted to empty. This Scope Lock changes what supplies that
parameter, in a future Unit — not the chain itself.

No other component may construct one. In particular:

- The Reasoning Provider does not assemble its own Reasoning Context —
  restated from `REASONING_PROVIDER_ARCHITECTURE.md` Section 10's own
  Minimalism Review, which already excluded this exact candidate.
- No coordinator (`CommunicationConversationCoordinator`,
  `ConversationTurnReasoningCoordinator`, `ConversationReplyCoordinator`,
  `ReplyDeliveryCoordinator`) constructs one — each already states, in
  its own Scope Lock, that it does not, and this document does not
  reopen any of them to add that capability.
- Memory and the World Model do not construct a `ReasoningContext`
  themselves — they are future *sources* the Reasoning Context Assembler
  draws from, never callers of the assembled result.

---

## 5. Lifetime

- **Creation.** One `ReasoningContext` is constructed per inbound
  message, by the Reasoning Context Assembler, invoked once by the
  Production Composition Root immediately before that message enters the
  frozen coordinator chain (Implementation Plan Section 7).
- **Mutation.** None. `ReasoningContext` is already, today, an immutable
  Kotlin `data class` (`REASONING_PROVIDER_CONTRACT_DESIGN.md` Section
  2) — this Scope Lock does not change that, and no future Reasoning
  Context Assembler implementation may introduce a mutable variant or
  hold a mutable reference that later changes an already-constructed
  instance's own `entries`. **The object is immutable after
  construction, without exception.**
- **Disposal.** No explicit disposal step exists or is needed — the
  instance is discarded when the one call chain it was passed into
  returns, exactly as every other per-call value in this pipeline
  already is (`ReasoningProviderRequest`, `ReasoningProviderResponse`,
  per `REASONING_PROVIDER_CONTRACT_DESIGN.md` Section 5). It is never
  retained by the Reasoning Context Assembler, the Production
  Composition Root, or any coordinator once the message's own handling
  completes. This matches `reasoning-context.md`'s own ephemerality
  principle exactly.

---

## 6. Threading Expectations

- **Sharing.** A given `ReasoningContext` instance is never shared
  across more than one inbound message's own handling. Each
  `submitOwnerMessage` invocation gets its own freshly-assembled
  instance.
- **Mutability.** Immutable (Section 5) — this alone eliminates the
  entire class of concurrent-mutation hazards a mutable shared context
  would otherwise require locking to prevent. No synchronisation
  primitive is needed, or introduced, to protect a `ReasoningContext`
  instance, because nothing about it can change after construction.
- **Coroutine expectations.** Assembled and consumed entirely within the
  same suspend call chain `submitOwnerMessage` already runs inside
  (`suspend fun submitOwnerMessage` -> ... -> `ReasoningProvider.reason`,
  all on the caller's own coroutine, per the frozen chain's existing,
  unmodified structure). A `ReasoningContext` instance is never passed
  across an unrelated coroutine scope, never leaks into a background
  task, and is safe to pass across suspension points precisely because
  it is immutable — the same property that makes `InboundOwnerMessage`
  and `ReasoningProviderResponse` safe to pass across suspension points
  today.

---

## 7. Relationship to the Constitution

This Scope Lock changes no constitutional invariant. "Cognition
proposes" (`parker-constitution.md`) is unaffected: the Reasoning
Context Assembler supplies cognition's *input*, never its authority, and
Section 1's inclusion of available-tool-description *entries* (never
live handles, never framed as already authorised) is deliberately
structured so that nothing this document authorises could be mistaken
for, or substituted for, Trust authorisation. "Trust authorises. Runtime
executes." remains entirely
the Permission Engine's and Execution Pipeline's own, unchanged
responsibility, exactly as `reasoning-context.md` Section "Relationship
to Existing Parker Components" already states: this document "does not
change the roles of the Permission Engine, the Execution Pipeline, the
Tool Registry, the Identity Service, or the Resource Registry."
