# Production Reasoning Context — Implementation Plan

## Status

**Sprint 11, Unit 1.** Architecture-only. This Plan, together with
`PRODUCTION_REASONING_CONTEXT_SCOPE_LOCK.md` (the governing document),
`SPRINT_11_UNIT_1_ENGINEERING_CHECKPOINT.md`, and
`SPRINT_11_UNIT_1_ACCEPTANCE_CHECKLIST.md`, freezes the architectural
boundaries of Production Reasoning Context assembly before any Kotlin
implementing it is written. No implementation, no Contract Design, and
no production code change is authorised by this Plan. Per PES-001, this
Unit produces architecture; a future Unit produces the Contract Design
and, later still, the implementation.

No architecture, ADR, or Constitution document is modified by this Plan.
`docs/architecture/reasoning-context.md` remains untouched and
authoritative; this Plan specializes it toward one open question it
deliberately left unassigned.

---

## 1. Governing Documents

In order of authority:

1. `docs/architecture/parker-constitution.md` — "Parker owns authority.
   Modules provide capability." and "Cognition proposes. Trust
   authorises. Runtime executes." Both preserved without exception.
2. `docs/architecture/reasoning-context.md` — Constitutional. Defines
   Reasoning Context as the third knowledge layer ("what matters for the
   current task"), distinct from Memory ("what Parker has learned") and
   the World Model ("what Parker currently believes"), and states the
   information flow `Memory + World Model -> Reasoning Context ->
   Reasoning Provider`. This Plan does not redefine any of this — it
   answers a question this document names but declines to assign: who
   performs "Reasoning Context assembly."
3. `docs/architecture/REASONING_PROVIDER_ARCHITECTURE.md` — Section 14
   Item 6: "Who owns 'Reasoning Context assembly.' Named by
   `reasoning-context.md` as a responsibility, not assigned to any
   component by that document, `19-conversation-engine.md`, or this
   document. Remains unassigned." This Plan exists to assign it. Section
   10's Minimalism Review also excludes one specific candidate — the
   Reasoning Provider itself owning assembly — a finding this Plan
   preserves (Section 3, below).
4. `docs/architecture/REASONING_PROVIDER_CONTRACT_DESIGN.md` — Section 2
   (`ReasoningContext`'s frozen Kotlin shape:
   `data class ReasoningContext(val entries: List<String>)`, an opaque,
   ordered list of prose entries); Section 5 ("`ReasoningContext`'s
   ownership is not assigned by this document... remains unassigned
   here"); Section 9 ("`ReasoningContext`'s own assembly mechanism...
   remains entirely unassigned"); Section 10 (a structured
   `ReasoningContext` was explicitly excluded because it "would require
   deciding 'Reasoning Context assembly' ownership and shape... unassigned").
5. `docs/architecture/IMPLEMENTATION_GAPS.md` #53 — has carried
   "`ReasoningContext` assembly ownership remains unassigned" across
   every Sprint 7–10 update without closing it. This Unit begins closing
   it, architecturally only.
6. The frozen coordinator Scope Locks —
   `docs/implementation/CONVERSATION_REPLY_COORDINATOR_SCOPE_LOCK.md`,
   `docs/implementation/REPLY_DELIVERY_COORDINATOR_SCOPE_LOCK.md`, and
   the Contract-Design-level guarantees for
   `CommunicationConversationCoordinator` and
   `ConversationTurnReasoningCoordinator` — each states, structurally,
   that its own class holds no dependency slot capable of assembling a
   `ReasoningContext` (Section 2, below, cites this directly).
7. `docs/implementation/PRODUCTION_COMPOSITION_ROOT_IMPLEMENTATION_PLAN.md`
   — the Production Composition Root (`ParkerRuntime`) is frozen and
   already the one production entry point through which every inbound
   message passes.

---

## 2. Why This Question Is Still Open

Direct evidence, not inference, that no existing frozen component
assembles `ReasoningContext` today:

- `ConversationReplyCoordinator.submitAndDeliver(message, reasoningContext)`
  — accepts `reasoningContext` as a parameter; its own KDoc states "this
  class does not assemble one -- Scope Lock Section 15"; its own Scope
  Lock Section 6 confirms no dependency slot exists that could reach
  Memory, the World Model, or any other context source.
- `CommunicationConversationCoordinator.submitAndReason(message,
  reasoningContext)` — forwards the same value, unchanged, to
  `ConversationTurnReasoningCoordinator.submitTurnAndReason`.
- `ConversationTurnReasoningCoordinator.submitTurnAndReason` — forwards
  the same value, unchanged, into `ReasoningProviderRequest(turn,
  reasoningContext)`.
- `ModelReasoningProvider.reason` — passes `request.reasoningContext`
  straight into `ReasoningPromptBuilder.buildPrompt`, which only
  flattens `.entries` into a prompt block; it does not assemble content,
  only formats what it is given.
- `ParkerRuntime.submitOwnerMessage(message, reasoningContext:
  ReasoningContext = ReasoningContext(emptyList()))` — the Production
  Composition Root's own one production entry point defaults an
  unsupplied `ReasoningContext` to an **empty list**. Its own KDoc
  states plainly: "`reasoningContext` is accepted, not assembled --
  `ReasoningContext` assembly ownership remains unassigned
  (`REASONING_PROVIDER_CONTRACT_DESIGN.md` Section 9), unchanged by this
  Unit."

Every frozen component between message receipt and the Reasoning
Provider is a pure pass-through. Today, in production, a real
`ReasoningContext` is never constructed at all — the pipeline runs on an
always-empty one unless a caller (currently only test code) supplies
something else. This is the exact, concrete shape of the gap this Unit
closes architecturally.

---

## 3. Responsibilities

Production Reasoning Context assembly is the responsibility of
combining, for one inbound message, exactly the task-scoped information
`docs/architecture/reasoning-context.md` describes ("what matters for
the current task") into the existing, frozen `ReasoningContext(entries:
List<String>)` shape — no new Kotlin shape, no change to
`ReasoningProviderRequest`, `ReasoningProvider`, or any coordinator.

This responsibility is **not** owned by the Reasoning Provider.
`REASONING_PROVIDER_ARCHITECTURE.md` Section 10's Minimalism Review
already considered and excluded exactly this candidate ("A dedicated
'Reasoning Context Assembler' responsibility, owned by the Reasoning
Provider" — excluded, because `reasoning-context.md` already names
assembly as its own, distinct responsibility, not the Reasoning
Provider's). This Plan preserves that exclusion. What that Minimalism
Review did not exclude, because it was never proposed, is a distinct
component — outside the Reasoning Provider — owning assembly. That is
the position this Plan takes (Scope Lock Section on Ownership).

---

## 4. Lifecycle

One `ReasoningContext` is assembled per inbound message, immediately
before that message enters the frozen coordinator chain
(`ConversationReplyCoordinator.submitAndDeliver` and everything it
calls). It is passed down unchanged, exactly as every frozen component
already does today, and is discarded once that one call returns — never
retained, cached, or reused for a later message. This mirrors
`reasoning-context.md`'s own "Reasoning Context is ephemeral" principle
directly: "Reasoning Context exists only for as long as the task it was
assembled for... It is not retained, cached for reuse in unrelated
future tasks."

---

## 5. Ownership

Exactly one new, not-yet-implemented component — the **Reasoning
Context Assembler** — owns constructing a `ReasoningContext`. The
**Production Composition Root** (`ParkerRuntime`) owns invoking it
exactly once per inbound message, as one more step in the sequence
`ParkerRuntime.submitOwnerMessage` already performs before delegating to
`ConversationReplyCoordinator.submitAndDeliver`. See the Scope Lock's own
"Ownership" section for the full reasoning and the boundary this draws.

No frozen coordinator's own signature changes. `reasoningContext`
remains an ordinary parameter passed down the same chain it already
flows through today — only what supplies a non-empty value at the top of
that chain changes, in a future implementation Unit, not this one.

---

## 6. Construction

Not described here beyond the ownership boundary above (Section 5) —
per this Unit's own instruction, this Plan describes architecture, not
implementation. How the Reasoning Context Assembler itself is
constructed (its own dependencies, whether it is constructor-injected by
`ParkerRuntime` following this repository's existing
explicit-constructor-injection discipline) is a Contract Design and
Implementation Plan question for the future Unit that actually builds
it, not resolved here. What is settled now is only: it does not yet
exist, and when it does, the Production Composition Root constructs and
owns it exactly as it already constructs and owns every other
collaborator in its runtime graph — no service locator, no singleton, no
global lookup, consistent with this repository's own governing
composition-root discipline.

---

## 7. Interaction With Existing Runtime

No existing, frozen component's contract changes:

```
Communication Intake -> Conversation Engine -> CommunicationConversationCoordinator
    -> ConversationTurnReasoningCoordinator -> ModelBackedReasoningProvider
    -> ResponseComposer -> ReplyDeliveryCoordinator -> ConversationReplyCoordinator
```

The Reasoning Context Assembler sits **before** this chain begins, owned
and invoked by the Production Composition Root, which already sits at
the top of it. The chain itself is untouched: every coordinator
continues to accept `reasoningContext` as an ordinary parameter, exactly
as today, unaware of where the value came from or how it was assembled.
This is precisely why no frozen component's Scope Lock needs revisiting:
none of them claim, and none of them are asked to claim, assembly
responsibility.

---

## 8. Excluded From This Unit

Restated plainly, matching the task's own constraints:

- No Kotlin is written. No production file is modified.
- Memory retrieval or storage design — a future Unit; this Plan defines
  only that Memory is one of the Reasoning Context Assembler's future
  input sources, not how.
- World Model internals — a future Unit; this Plan defines only that the
  World Model is another future input source, not how.
- Goal / Planner Runtime routing — unrelated; `ReasoningContext` may
  carry Goal-relevant information as a prose entry, exactly as it does
  today, but no planning occurs here or anywhere this Unit touches.
- Any Home Assistant, Pickle, or other downstream-consumer detail —
  Reasoning Context remains platform-independent, exactly as
  `reasoning-context.md` requires of every layer it defines.
- A Contract Design for the Reasoning Context Assembler itself — this
  Plan and its Scope Lock establish the boundary a future Contract
  Design will work within; they are not that Contract Design.

---

## Conclusion

This Plan, and the Scope Lock it governs, assign the one architectural
responsibility `REASONING_PROVIDER_ARCHITECTURE.md` Section 14 Item 6
and `REASONING_PROVIDER_CONTRACT_DESIGN.md` Sections 5 and 9 each left
open: who performs Reasoning Context assembly. The answer is a new,
dedicated Reasoning Context Assembler, owned and invoked by the
Production Composition Root, sitting entirely outside the frozen
coordinator chain and the Reasoning Provider. No Kotlin exists yet. No
frozen component is redesigned. `IMPLEMENTATION_GAPS.md` #53's
`ReasoningContext` assembly item moves from "unassigned" to
"assigned, not yet implemented" — closure remains for a future,
separately-scoped Unit.
