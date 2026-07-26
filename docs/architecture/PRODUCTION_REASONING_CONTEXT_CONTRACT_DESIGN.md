# Production Reasoning Context — Contract Design

## Status

**Sprint 11, Unit 2. PES-001 Stage 3 (Contract Design).** Defines the
Reasoning Context Assembler's production contract: its interface,
responsibilities, dependencies, ownership boundaries, lifecycle, and
failure behaviour. Does not implement it. No Kotlin file is created or
modified by this document; any signature shown is an **illustrative
sketch**, not authorised production code.

This document works strictly within the boundaries `PRODUCTION_REASONING_CONTEXT_SCOPE_LOCK.md`
already froze in Sprint 11, Unit 1 and does not revisit them: ownership
(the Reasoning Context Assembler constructs, the Production Composition
Root alone invokes), lifetime (per-message, immutable, ephemeral),
responsibilities (Scope Lock Section 1/2), and the constitutional
boundary (Scope Lock Section 7). Where this document cites a Scope Lock
section number, it means Unit 1's Scope Lock, as refined and frozen.

---

## 1. Governing Documents

In order of authority, additive to `PRODUCTION_REASONING_CONTEXT_IMPLEMENTATION_PLAN.md`
Section 1 (not repeated here):

1. `docs/implementation/PRODUCTION_REASONING_CONTEXT_SCOPE_LOCK.md` —
   the governing document for this Unit. Every contract decision below
   is checked against it; none contradicts it.
2. `docs/implementation/PRODUCTION_REASONING_CONTEXT_IMPLEMENTATION_PLAN.md`
   and `docs/reviews/SPRINT_11_UNIT_1_ENGINEERING_CHECKPOINT.md` — the
   architectural reasoning and named risks this Contract Design must not
   silently resolve by implementation (Section 8, below).
3. `src/interfaces/ReasoningProvider.kt` — `ReasoningContext`'s frozen
   Kotlin shape (`data class ReasoningContext(val entries: List<String>)`),
   unchanged, the only value this Assembler ever produces.
4. `src/composition/ParkerRuntime.kt` — the frozen Production Composition
   Root this Assembler is invoked by; its `submitOwnerMessage` and
   `buildAndRegisterRuntimeGraph` are read, not modified.
5. `src/interfaces/IdentityService.kt`, `src/interfaces/ToolRegistry.kt`,
   `src/interfaces/ConversationEngine.kt`, `src/interfaces/CommunicationIntake.kt`
   — cited for their existing, frozen, unmodified shapes wherever this
   document names a dependency (Section 4).

---

## 2. What the Assembler Receives as Input

**The raw `InboundOwnerMessage`, and nothing else, as its one per-call
argument.** This is a direct, load-bearing consequence of Scope Lock
Section 4 (Ownership): the Production Composition Root is the sole
invoker, and it invokes the Assembler *before* calling
`ConversationReplyCoordinator.submitAndDeliver` — meaning before
`CommunicationConversationCoordinator.submitAndReason` runs, which is,
in turn, before `ConversationEngine.submitTurn` constructs the `Turn`
for this message. **No `Turn` exists yet at the point the Assembler
runs.** `ParkerRuntime` has, at that point, exactly what
`submitOwnerMessage`'s own caller gave it: the `InboundOwnerMessage`.

This is not a limitation this Contract Design works around — it is
confirmed directly against `ConversationEngine.kt`: `submitTurn` is
`ConversationEngine`'s only operation, and it is the one place a `Turn`
is constructed anywhere in this codebase. Nothing upstream of it can
hand the Assembler a `Turn`, and this document does not propose changing
that sequencing (which would mean redesigning `ParkerRuntime` or
`ConversationEngine`, out of this Unit's authority).

Consequently: "Current request" and "Current time" (Scope Lock Section
1's Conversation and Operational Context items respectively) require no
dependency at all — both are fields already on the one input parameter
(`InboundOwnerMessage.text`, `.timestamp`). Likewise "Active
communication channel" is already `InboundOwnerMessage.channelId`. Only
"Participant identities," "Requesting principal identity," "Available
tool descriptions," and "Current conversation" (prior Turns) require
anything beyond the input parameter itself — Section 4 below justifies
each.

---

## 3. The Assembler Interface

One operation, mirroring this codebase's own established convention —
`ConversationEngine`, `ReasoningProvider`, and `CommunicationIntake` are
each exactly one method:

```kotlin
// Illustrative only. Not production code. Not authorised for creation
// by this document.
fun interface ReasoningContextAssembler {
    suspend fun assemble(message: InboundOwnerMessage): ReasoningContext
}
```

- **One input:** the `InboundOwnerMessage` (Section 2).
- **One output:** a `ReasoningContext` — the existing, frozen,
  unmodified shape. No new return type is introduced.
- **`suspend`:** consistent with every dependency this interface may
  read from (`IdentityService`, `ToolRegistry` are both `suspend`
  interfaces today) and with the coroutine expectations Scope Lock
  Section 6 already froze.
- **No second parameter carries a pre-existing `ReasoningContext`.**
  This operation *produces* one; nothing about assembling it depends on
  a prior one, and Scope Lock Section 5 (Lifetime) already establishes
  that each is constructed fresh, never accumulated or amended.

This single-method shape is deliberately the entire public contract.
Nothing else is exposed. In particular, no method returns partial or
intermediate assembly state — the only observable behaviour is: given a
message, produce a complete, immutable `ReasoningContext`, or fail
(Section 6).

---

## 4. Dependencies

Every dependency below answers the question the task poses directly:
*why does this component need this dependency?* Where the honest answer
is "it might be useful," the dependency is not included.

### 4.1 Real dependencies, justified today

- **`IdentityService`, read use only (`resolve`).** Justified by Scope
  Lock Section 1's "Participant identities" and "Requesting principal
  identity" items — both require resolving a `PrincipalId` (already on
  `InboundOwnerMessage.senderPrincipalId`) to a `Principal` for its
  `displayName`. `IdentityService` today exposes no narrower,
  read-only-only interface than the one it already has
  (`register`/`resolve`/`updateStatus`/`touch`/`listByOwner` are all on
  the same interface) — this Contract Design does not propose splitting
  it (that would modify a frozen interface, out of scope). Instead: the
  Assembler's contract states plainly that it calls `resolve` only,
  never `register`, `updateStatus`, or `touch` — a behavioural
  discipline this document commits to, not one the type system enforces
  today (named as a dependency-shape risk, `SPRINT_11_UNIT_2_ENGINEERING_CHECKPOINT.md`
  Section 2).
- **`ToolRegistry`, read use only (`listAll` and/or `findCandidates`).**
  Justified by Scope Lock Section 1's "Available tool descriptions" —
  both methods return `List<ToolDescriptor>`, plain descriptor data,
  never a live `Tool` handle and never anything from `ToolInvocationBinding`.
  The Assembler never calls `register`, `setLifecycleState`, or
  `resolve` (`ToolRegistry.resolve` returns a `ToolResolution`, part of
  the execution-resolution path `DefaultExecutionPipeline` owns, not a
  descriptor lookup — calling it would blur the Assembler into
  execution-adjacent territory the Scope Lock's exclusions (Section 2:
  "Tool implementations") already rule out). Same behavioural-discipline
  caveat as `IdentityService`, above.

### 4.2 Deferred dependency boundaries — named, not designed

Each of the following is a real, anticipated future dependency this
Contract Design names a boundary for, per the task's own instruction not
to design retrieval, storage, or indexing now. None has a Kotlin shape
today. None is a dependency of the Assembler as this Unit defines it —
adding any of them is a future Contract Design's own decision, not
authorised here.

- **Memory Source.** Boundary: some future, narrower, read-only
  interface Memory's own integration Unit exposes — not `MemoryStore`
  wholesale (per the same narrowing discipline Section 4.1 already
  applies to `IdentityService` and `ToolRegistry`). Retrieval,
  relevance, and volume are entirely undecided here.
- **World Model Source.** Same treatment as Memory Source — a future,
  narrower, read-only boundary, not designed here.
- **Conversation History Source.** Justified by Scope Lock Section 1's
  "Current conversation" item (prior Turns) — but **no existing
  interface in this codebase can supply it today.** Confirmed directly:
  `ConversationEngine`'s only operation is `submitTurn`, which
  *constructs* a `Turn` as a side effect (persisting it, per
  `InMemoryConversationEngine`'s own implementation) — it is not a query
  method, and giving the Assembler a live `ConversationEngine` reference
  would hand it the ability to call `submitTurn` itself, a mutating
  operation flatly incompatible with "stateless, side-effect-free"
  (Section 5, below). No other type in this codebase exposes prior Turns
  by `ConversationId` for reading. This boundary is therefore named on
  exactly the same footing as Memory and World Model — undefined,
  deferred, a future Unit's own decision — rather than assumed solvable
  by reusing `ConversationEngine` as-is.

**No constructor signature below includes any of these three.** They
are named so a future Contract Design revision has a clear slot to fill,
not fabricated today because "Do not introduce dependencies simply
because they might be useful" applies exactly as much to a boundary
placeholder as to a live dependency.

### 4.3 Illustrative construction shape

```kotlin
// Illustrative only. Not production code.
class DefaultReasoningContextAssembler(
    private val identityService: IdentityService,
    private val toolRegistry: ToolRegistry,
    // Memory Source, World Model Source, and Conversation History Source
    // each intentionally absent -- Section 4.2. A future Contract Design
    // revision adds each as its own future integration Unit defines its
    // own read-only boundary shape; this Unit does not invent one.
) : ReasoningContextAssembler {
    override suspend fun assemble(message: InboundOwnerMessage): ReasoningContext {
        TODO("Not this Unit's concern -- Contract Design, not implementation.")
    }
}
```

Explicit constructor injection throughout, matching this repository's
own governing composition-root discipline (`PRODUCTION_COMPOSITION_ROOT_IMPLEMENTATION_PLAN.md`
Section on explicit constructor injection) — no service locator, no
singleton, no global lookup, and no dependency this Assembler could
reach for on its own.

---

## 5. Contract Principles

Restated as binding contract terms, not merely aspirational description:

- **Deterministic.** Given the same `InboundOwnerMessage` and the same
  state of every injected dependency at call time, `assemble` produces
  the same `ReasoningContext`. Nothing about the Assembler introduces
  its own randomness, its own clock beyond what `InboundOwnerMessage.timestamp`
  already carries, or any other non-reproducible input.
- **Stateless.** The Assembler holds no mutable field of its own between
  calls. Every `assemble` invocation is independent; nothing observed or
  computed during one call is retained for, or influences, the next.
  This is what makes the Assembler itself safe to construct once, at
  startup, and reuse for every inbound message (Section 7).
- **Side-effect free.** `assemble` never writes to Memory, the World
  Model, `IdentityService`, `ToolRegistry`, or anywhere else. It reads;
  it never causes a change any other component could observe. This is
  the direct, load-bearing consequence of Scope Lock Section 3 (the
  projection principle): a component that only ever projects existing
  state can have no legitimate reason to write anywhere.
- **It does not:** cache (Scope Lock Section 2); persist (Scope Lock
  Section 2); mutate Memory or the World Model (Section 4.2, above —
  it does not even depend on either yet, let alone mutate either);
  invoke Tools (Section 4.1 — `ToolRegistry` read use only, never a
  `Tool` handle); invoke Planner (no dependency on any Planner-adjacent
  type exists in Section 4); invoke the Permission Engine (no dependency
  on it exists in Section 4, and none is justified — the Assembler
  proposes nothing that needs authorising); invoke the Execution
  Pipeline (same reasoning).
- **Its only responsibility is assembling a projection** (Scope Lock
  Section 3, restated as the Assembler's own operating principle, not
  merely the value's property). Every field the Assembler reads already
  has exactly one other home (`InboundOwnerMessage`, `IdentityService`,
  `ToolRegistry`, and, in future, Memory/World Model/Conversation
  History); the Assembler introduces no new home for any of it. If a
  future contribution to this Assembler's own responsibilities cannot be
  traced to reading, projecting, and discarding an excerpt of something
  that already lives elsewhere, it does not belong in this Assembler,
  regardless of how it is later implemented.

---

## 6. Failure Behaviour

The Assembler is not authorised to define its own error-handling
mechanism separate from what `ParkerRuntime` already has — Scope Lock
Section 4 makes the Production Composition Root the Assembler's sole
caller, and `ParkerRuntime.submitOwnerMessage`'s own existing, frozen
`try`/`catch` already covers exactly this shape of fault:

- **A genuine failure during `assemble`** (an unresolvable
  `PrincipalId`, an unreachable `IdentityService` or `ToolRegistry`
  implementation, or any other exception) is not caught by the
  Assembler itself — restating the same "propagates unchanged to the
  caller" discipline every frozen coordinator's own Scope Lock already
  holds itself to (`PRODUCTION_REASONING_CONTEXT_IMPLEMENTATION_PLAN.md`
  Section 2 cites this directly for the coordinators; this Contract
  Design extends the identical discipline to the Assembler). It
  propagates to `ParkerRuntime.submitOwnerMessage`'s own existing outer
  `try`/`catch`.
- **Under today's frozen `ParkerRuntimeOutcome`/`PipelineStage` shape**
  (`src/composition/ParkerRuntimeOutcome.kt`, unmodified by this
  document), such a fault is reported as
  `ParkerRuntimeOutcome.Failed(PipelineStage.UNKNOWN, cause)` — exactly
  the same classification every non-`REASONING` fault already receives,
  per `PipelineStage`'s own KDoc ("this runtime has no tagged,
  stage-labelled exception type to read from any of those components").
  **This Contract Design does not propose adding a dedicated
  `PipelineStage` value for context-assembly failures** — doing so would
  modify a frozen file this Unit is not authorised to touch. The
  resulting loss of diagnostic precision (an assembly fault and, say, a
  `ResponseComposer` fault are both reported identically as `UNKNOWN`)
  is named as a risk, not solved, in `SPRINT_11_UNIT_2_ENGINEERING_CHECKPOINT.md`
  Section 1.
- **A genuine `CancellationException`** thrown during `assemble`
  propagates unchanged, exactly as `ParkerRuntime.submitOwnerMessage`
  already guarantees for every other stage of the pipeline — no special
  case for the Assembler.
- **The Assembler never swallows an exception to substitute a
  degraded-but-valid `ReasoningContext`.** A partial or best-effort
  context (e.g., proceeding with an empty entries list because
  `IdentityService.resolve` failed) is not this Contract Design's
  decision to authorise or forbid for a future implementation — it is
  named as an open question, not resolved here
  (`SPRINT_11_UNIT_2_ENGINEERING_CHECKPOINT.md` Section 1).

---

## 7. Production Lifecycle

Two distinct lifecycles, deliberately not conflated:

- **The Assembler component's own lifecycle.** Constructed once, at
  startup, by `ParkerRuntime.buildAndRegisterRuntimeGraph` (or
  wherever a future implementation Unit places construction within
  that existing, frozen method) — exactly like every other collaborator
  already in that graph (`ConversationTurnReasoningCoordinator`,
  `ResponseComposer`, and so on), each constructed once and reused for
  every inbound message. This is a natural consequence of
  **Statelessness** (Section 5): a stateless component has no reason to
  be reconstructed per call, and reconstructing it per call would add
  nothing but cost. This is additive to, not a revision of, Unit 1's
  Scope Lock — Unit 1 defined the assembled *value's* lifecycle (Scope
  Lock Section 5), not the *component's* own.
- **The assembled `ReasoningContext` value's own lifecycle.** Exactly as
  Scope Lock Section 5 already froze: one per inbound message, immutable
  from the moment `assemble` returns, discarded once the call chain it
  was passed into completes. This document does not revise that.

---

## 8. Immutability Expectations

`ReasoningContext` is already, today, an immutable Kotlin `data class`
(`REASONING_PROVIDER_CONTRACT_DESIGN.md` Section 2) — unchanged by this
document. The contract this Unit adds is about the *moment* immutability
begins, not the type itself: **a `ReasoningContext` becomes immutable
the instant `assemble` returns it.** Everything the Assembler read to
build it (a resolved `Principal`, a `List<ToolDescriptor>`, prior
Turns once Conversation History exists) is rendered into the returned
instance's own `entries` before that return — nothing about the returned
value holds a live reference back to any of those sources, and nothing
the Assembler reads afterward can change an already-returned instance.
See `PRODUCTION_REASONING_CONTEXT_SEQUENCE.md` for exactly where in the
production message flow this moment falls.

---

## 9. Interaction With the Production Composition Root

Restated precisely, since it is this Contract Design's own most
load-bearing boundary:

1. `ParkerRuntime.buildAndRegisterRuntimeGraph` constructs the Assembler
   once, injecting `identityService` and `toolRegistry` — both already
   constructed at that point in the existing, frozen sequence (see
   `ParkerRuntime.kt`'s own current construction order) — explicitly,
   via constructor injection, exactly as it constructs every other
   collaborator.
2. `ParkerRuntime.submitOwnerMessage` invokes `reasoningContextAssembler.assemble(message)`
   exactly once, before calling `conversationReplyCoordinator.submitAndDeliver`.
   This is the one call site. No other component ever invokes `assemble`.
3. The returned `ReasoningContext` is passed, unchanged, into
   `conversationReplyCoordinator.submitAndDeliver(message, reasoningContext)`
   — the same frozen parameter every coordinator downstream already
   accepts today. No frozen coordinator's own signature changes.

`ParkerRuntime`'s own existing default parameter
(`reasoningContext: ReasoningContext = ReasoningContext(emptyList())`)
is not removed or redesigned by this document — a future implementation
Unit decides how the default interacts with a real Assembler call (e.g.,
whether the default is retired once the Assembler exists, or preserved
for callers that supply their own context). Not decided here.

---

## 10. Reinforcing the Projection Principle

Every section above traces back to Scope Lock Section 3. Section 2
(Input) establishes the Assembler adds no new source of truth, only
reads existing ones. Section 4 (Dependencies) admits only narrow,
read-only slices of existing components, and names — without designing
— every future source as a boundary, never a fabricated shape. Section 5
(Contract Principles) states plainly that side-effect-freedom is the
projection principle's own direct consequence. Section 6 (Failure
Behaviour) ensures a fault never causes the Assembler to originate a
"best guess" fact in place of a genuine one. Nothing in this Contract
Design gives the Assembler ownership of Memory, the World Model,
Conversation state, or any runtime state — it remains, in Contract
Design terms exactly as in Scope Lock terms, a projection, never a
source of truth.
