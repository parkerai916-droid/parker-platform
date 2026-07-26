# Production Reasoning Context — Sequence

## Status

**Sprint 11, Unit 2.** Companion to `PRODUCTION_REASONING_CONTEXT_CONTRACT_DESIGN.md`.
Describes the production message flow from an inbound message to the
Reasoning Provider, showing exactly where a `ReasoningContext` is
assembled, exactly where it becomes immutable, and exactly where
structural responsibility for it passes from one component to the next.
Architecture only — the Reasoning Context Assembler step below is a
**proposed integration point**, not existing code. Every other step in
this sequence is the real, frozen, unmodified production sequence, cited
directly against `src/composition/ParkerRuntime.kt` and each frozen
coordinator.

---

## 1. What "Ownership Transfers" Means for an Immutable Value

Before the sequence itself: Scope Lock Section 3 (Projection, Not a
Source of Truth) means `ReasoningContext` is never *owned* in the
mutable-state sense anywhere in this flow — nothing downstream of the
Assembler can change it, so there is no liability to transfer, no lock
to hand off, no risk of two components racing to mutate the same value.
What *does* move, step to step, is narrower: **structural
responsibility for holding the reference and passing it to the next
step.** That is what "ownership transfers" means below — never mutation
rights, which do not exist for this value anywhere in this flow.

---

## 2. The Sequence

```
Inbound Message
      |
      v
Production Composition Root (ParkerRuntime.submitOwnerMessage)
      |
      |  (1) invokes, exactly once
      v
Reasoning Context Assembler (reasoningContextAssembler.assemble(message))
      |
      |  (2) returns a new, already-immutable ReasoningContext
      v
Production Composition Root (holds the returned ReasoningContext)
      |
      |  (3) passes message + reasoningContext together
      v
ConversationReplyCoordinator.submitAndDeliver(message, reasoningContext)
      |
      |  (4) passes both, unchanged
      v
CommunicationConversationCoordinator.submitAndReason(message, reasoningContext)
      |
      |  (5) internally: CommunicationIntake accepts the message,
      |      ConversationEngine.submitTurn constructs the Turn
      |      -- reasoningContext is not involved in this step
      |
      |  (6) passes the constructed Turn + the same reasoningContext
      v
ConversationTurnReasoningCoordinator.submitTurnAndReason(turn, reasoningContext)
      |
      |  (7) constructs ReasoningProviderRequest(turn, reasoningContext)
      v
Reasoning Provider (ModelBackedReasoningProvider.reason(request))
      |
      |  (8) reads request.reasoningContext.entries once, to build a
      |      prompt (ReasoningPromptBuilder) -- does not retain it
      v
ReasoningProviderResponse (Goal / Reply / NoAction)
      |
      |  (9) the ReasoningContext instance is not referenced again by
      |      anything in this call chain -- it is discarded once this
      |      one submitOwnerMessage invocation returns (Scope Lock
      |      Section 5, Disposal)
      v
(rest of the frozen pipeline: ResponseComposer -> ReplyDeliveryCoordinator
 -> ExecutionPipeline -> Tool execution -- unaffected by, and not
 carrying, ReasoningContext any further)
```

---

## 3. Where Assembly Happens

**Step (1).** `ParkerRuntime.submitOwnerMessage` invokes the Reasoning
Context Assembler exactly once, as the first action it takes after
confirming `state == RuntimeLifecycleState.RUNNING` (its own existing,
frozen precondition check) and before its one existing call to
`conversationReplyCoordinator.submitAndDeliver`. This is the only place
in the entire production sequence a `ReasoningContext` is constructed.
No frozen coordinator downstream of this point ever constructs one —
each already states, in its own Scope Lock, that it does not
(`PRODUCTION_REASONING_CONTEXT_IMPLEMENTATION_PLAN.md` Section 2 cites
this directly for every one of them).

---

## 4. Where It Becomes Immutable

**Step (2).** The moment `reasoningContextAssembler.assemble(message)`
returns. `ReasoningContext` is already an immutable Kotlin `data class`
(`REASONING_PROVIDER_CONTRACT_DESIGN.md` Section 2), so there is no
further step, anywhere later in this sequence, at which immutability
begins or could be revoked — it holds from step (2) onward, through
every subsequent hand-off, all the way to its discard at step (9).
Nothing between steps (2) and (9) constructs a second `ReasoningContext`
or amends the first.

---

## 5. Where Structural Responsibility Passes

Restating Section 1's framing at each hand-off point in the sequence
above:

| From | To | Step | What actually moves |
| --- | --- | --- | --- |
| Reasoning Context Assembler | Production Composition Root | (2) | The one, already-immutable instance, as a return value. |
| Production Composition Root | `ConversationReplyCoordinator` | (3) | The same reference, as a method argument, alongside `message`. |
| `ConversationReplyCoordinator` | `CommunicationConversationCoordinator` | (4) | The same reference, unchanged — `ConversationReplyCoordinator`'s own Scope Lock already states it never inspects or mutates `reasoningContext` (`PRODUCTION_REASONING_CONTEXT_IMPLEMENTATION_PLAN.md` Section 2). |
| `CommunicationConversationCoordinator` | `ConversationTurnReasoningCoordinator` | (6) | The same reference, now alongside a newly-constructed `Turn` rather than the raw message. |
| `ConversationTurnReasoningCoordinator` | Reasoning Provider | (7) | The same reference, wrapped as one field of a new `ReasoningProviderRequest` — the wrapping object is new; the `ReasoningContext` inside it is not. |
| Reasoning Provider | *(nowhere — discarded)* | (9) | Nothing. The Reasoning Provider reads `.entries` to build a prompt and retains no reference afterward (`ReasoningProvider`'s own Contract Design Section 5: "transient, per-invocation values... not retained by the Reasoning Provider once the call returns"). |

At no point does more than one component hold a reference with any
stated intent to use it beyond forwarding it to the next step —
restating Scope Lock Section 6 (Sharing): "A given `ReasoningContext`
instance is never shared across more than one inbound message's own
handling."

---

## 6. What This Sequence Does Not Show

- **How the Assembler itself resolves participant identities, tool
  descriptions, or (in future) Memory/World Model/Conversation History
  content.** That is implementation, out of this Unit's authority
  (`PRODUCTION_REASONING_CONTEXT_CONTRACT_DESIGN.md` Section 4 names the
  dependency boundaries; it does not describe retrieval).
- **Any change to an existing, frozen method signature.** Every method
  named above (`submitOwnerMessage`, `submitAndDeliver`, `submitAndReason`,
  `submitTurnAndReason`, `reason`) is shown exactly as it exists in
  production today; none is modified by this document.
- **What happens once the Reasoning Provider returns.** `ResponseComposer`,
  `ReplyDeliveryCoordinator`, the Execution Pipeline, and Tool execution
  all continue exactly as today, entirely independent of
  `ReasoningContext`, which has already been discarded by the time any
  of them run.
