# Conversation Continuity and Pre-Turn Identity — Contract Design

## Status

**Sprint 11, Unit 5 (Contract Design stage), PES-001 Stage 3.** Revised
following architectural review. **Revision note (Contract Authority and
Propagation Correction):** the original version of this document selected
a purely-derived, stateless `ConversationId` and claimed
`ConversationEngine`'s interface could remain unchanged while still being
"the" authority. Architectural review found this internally inconsistent:
if `ConversationEngine` never receives the pre-resolved identifier, it must
independently re-decide identity inside `submitTurn`, which is either a
second, competing decision or a claim of consistency resting on trust
rather than structure. This revision replaces that design outright. It
does not keep the prior derivation model as an option — Section 3 explains
why it is rejected, not merely why the resolved model is preferred.

Governs how conversation continuity is recognised and how the applicable
`ConversationId` becomes available before Turn creation. Builds on
`docs/implementation/CONVERSATION_IDENTITY_ARCHITECTURE_SCOPE_LOCK.md`,
`docs/implementation/CONVERSATION_IDENTITY_ARCHITECTURE_IMPLEMENTATION_PLAN.md`,
`docs/architecture/19-conversation-engine.md`, and
`docs/architecture/CONVERSATION_ENGINE_CONTRACT_DESIGN.md`. Contract
Design only. No Kotlin is created or modified by this document; no
production or test file is touched.

---

## 1. The Seven Frozen Facts, Re-Confirmed

Unchanged from the prior revision of this document — see Section 1's
original citations, still accurate against current code:
`ConversationEngine` owns Conversation membership;
`ConversationId` is stable for a logical Conversation's own lifetime;
`ConversationId` is distinct from `CorrelationId`; `ReasoningContextAssembler`
must not depend directly on `ConversationEngine`; Conversation History
Source is read-only and does not determine identity; the current
one-Turn-per-Conversation behaviour is disclosed scaffolding, not a
constitutional ceiling; existing runtime components must not be redesigned
unless a genuine contract impossibility is proven. **This revision does
not touch fact 7's own status** — no impossibility is claimed here either;
instead, this revision demonstrates that preserving fact 1
(`ConversationEngine` ownership) *requires* an honest, additive interface
change, which is a different, and lesser, thing than "redesign." Section 6
below addresses this distinction directly, per architectural review's own
Question 5.

Runtime sequence, unchanged from the prior revision, re-confirmed against
current code this pass: `ParkerRuntime.submitOwnerMessage` →
`reasoningContextAssembler.assemble` → `conversationReplyCoordinator.submitAndDeliver`
→ `CommunicationConversationCoordinator.submitAndReason` →
`communicationIntake.submitInboundMessage` →
`conversationTurnReasoningCoordinator.submitTurnAndReason` →
`conversationEngine.submitTurn` → `reasoningProvider.reason`. This
revision changes what flows along this sequence (Section 5); it does not
change the sequence's own ordering of components.

---

## 2. What Architectural Review Found Wrong

The prior revision claimed, simultaneously: (a) `ConversationId` is a pure,
stateless function of `(channelId, senderPrincipalId)`; (b)
`ConversationEngine`'s interface remains completely unchanged; (c) there is
exactly one authoritative decision per inbound message. Claims (a) and (b)
together entail that `ConversationEngine`'s own future implementation must
*independently re-evaluate* the same formula inside `submitTurn`, since it
receives nothing from the earlier, `ParkerRuntime`-side evaluation. Claim
(c) then rests entirely on an unenforced assumption — that both
evaluations happen to use the identical formula, forever, with no
structural mechanism preventing drift. That is not one decision; it is two
evaluations trusted to agree. This revision removes the trust requirement
by making the second evaluation unnecessary: the value is propagated, not
recomputed (Section 5).

A second, deeper problem, raised directly by architectural review's
Question 4: a purely-derived identifier — a fixed function of
`(channelId, senderPrincipalId)` alone, with no other input — can **never
change** for a given pair. This forecloses, permanently and by
construction, any future ability for a Conversation to end and a
*subsequent* message from the same pair to begin a genuinely new one with a
new identifier — which `19-conversation-engine.md` Section 13 Item 4
(idle/termination) and this Unit's own frozen requirement ("stable
`ConversationId` *for a logical Conversation*" — singular, implying more
than one may exist over time for the same pair) both anticipate as a real
future capability. Pure derivation is therefore not merely inconvenient to
combine with future termination — it is architecturally incompatible with
it. This is why Section 3 rejects it outright rather than merely
preferring an alternative.

---

## 3. Question 3 Resolved First: Derivation Rejected, Resolution Selected

**Selected model: Resolved identity model.** `(channelId,
senderPrincipalId)` is a **continuity key**, not an identity-generating
input. The key is used to *resolve* — via a lookup against
`ConversationEngine`'s own owned state — either the `ConversationId` of an
already-open Conversation for that key, or a newly minted one if none is
open. The models are not mixed: no formula anywhere in this design computes
a `ConversationId` directly from message fields without consulting state.

**Why not the Derived model.** Rejected for the reason given in Section 2:
it cannot support termination or reopening, both of which this Unit's own
frozen requirements anticipate as legitimate future capability, even
though neither is designed here. A model that forecloses them by
construction is a worse fit for "stable `ConversationId` for a logical
Conversation" than one that merely leaves them open.

**Consequence, stated honestly, per Question 4:** state is now required —
not for computing an identifier from nothing, but for recording which
continuity keys currently have an open Conversation and what identifier
that Conversation carries. Section 7 distinguishes exactly what this state
is for, and what it is not for.

---

## 4. Question 1 Resolved: One Authority

**The sole authority for both (i) whether an inbound message continues an
existing Conversation and (ii) which `ConversationId` applies is
`ConversationEngine`.** Not `ParkerRuntime`. Not any component the
Assembler touches. `ParkerRuntime`'s only role is to **invoke** this
authority earlier in the pipeline than `submitTurn` runs today — it makes
no decision of its own, holds no state of its own about conversation
continuity, and never mints or guesses a `ConversationId`.

This is why `ConversationEngine`'s interface must change (Section 6,
answering Question 5 directly): the authority cannot be consulted before
`submitTurn` runs unless it exposes an operation callable before
`submitTurn` runs. Keeping the interface frozen while still requiring
`ConversationEngine` to be the sole authority are mutually exclusive
demands; this revision resolves the conflict by choosing authority over
interface stability, exactly as architectural review's Question 5
instructed: "An unchanged interface is desirable but not mandatory if it
prevents authoritative identity propagation."

---

## 5. Question 2 Resolved: Propagation, Not Re-Derivation

**`ConversationEngine` gains one new, additive operation, and one existing
operation gains one new, additive parameter. No other frozen component's
responsibilities change; three of them gain one additional pass-through
parameter, exactly mirroring the precedent already set when
`reasoningContext` itself was threaded through this same chain in Sprint
11 Unit 3.**

- **New: `ConversationEngine.resolveConversationId(message: InboundOwnerMessage): ConversationId`**
  (illustrative name, not a mandated Kotlin signature). Given the
  continuity key drawn from `message` (`channelId`, `senderPrincipalId`),
  consults `ConversationEngine`'s own owned state: if an open Conversation
  already exists for that key, returns its existing `ConversationId`
  unchanged; otherwise mints a new one, and records the key as now open
  against it. This is the one and only point in the entire pipeline where
  a continuity decision is made, and the one and only point where a new
  `ConversationId` is ever minted.
- **Changed: `ConversationEngine.submitTurn(message: InboundOwnerMessage, conversationId: ConversationId): ConversationDisposition`**
  (additive parameter). No longer decides identity at all — it accepts the
  already-resolved `ConversationId` as a required input, and performs only
  the bookkeeping `19-conversation-engine.md` Section 4 already assigns
  it: constructing or appending to the `Conversation` record for that
  exact identifier, constructing the `Turn`, appending to `turnIds`, and
  determining `isNewConversation` by checking whether a `Conversation`
  record for that identifier already existed *before this call* — a
  question answered entirely from `ConversationEngine`'s own state, not by
  re-consulting the continuity key.

**Propagation path**, decided once, carried unchanged to every consumer:

1. `ParkerRuntime.submitOwnerMessage` calls
   `conversationEngine.resolveConversationId(message)` immediately after
   confirming `RUNNING`, before constructing the Assembler's input. This
   is the one authoritative decision for this inbound message.
2. `ParkerRuntime` constructs the Resolved Inbound Envelope (message,
   resolved `ConversationId`) and calls `reasoningContextAssembler.assemble(envelope)`.
3. `ParkerRuntime` calls `conversationReplyCoordinator.submitAndDeliver(message, reasoningContext, conversationId)`
   — the same resolved value, forwarded as a third, additive, pass-through
   parameter.
4. `ConversationReplyCoordinator.submitAndDeliver` forwards it, unchanged,
   into `communicationConversationCoordinator.submitAndReason(message, reasoningContext, conversationId)`.
5. `CommunicationConversationCoordinator.submitAndReason` forwards it,
   unchanged, into
   `conversationTurnReasoningCoordinator.submitTurnAndReason(disposition.message, reasoningContext, conversationId)`
   (only after `CommunicationIntake` accepts the message, exactly as
   today).
6. `ConversationTurnReasoningCoordinator.submitTurnAndReason` calls
   `conversationEngine.submitTurn(message, conversationId)` — the same
   value, unchanged, consumed rather than re-decided.
7. The resulting `ConversationDisposition.turn.conversationId` is, by
   construction, identical to the value resolved in step 1 — not merely
   expected to match, but structurally incapable of differing, because
   `submitTurn` no longer computes an identifier at all.
8. A future Conversation History Source read would use this same,
   already-resolved value — available from step 1 onward, long before it
   is needed for any such read.

No second derivation and no second identity decision exist anywhere in
this path.

**Disclosed ripple, stated honestly.** This does extend beyond
`ConversationEngine` alone: `ConversationReplyCoordinator.submitAndDeliver`,
`CommunicationConversationCoordinator.submitAndReason`, and
`ConversationTurnReasoningCoordinator.submitTurnAndReason` each gain one
additive, pass-through parameter. None of the three changes what these
coordinators sequence, when they call their own dependencies, or what
decisions they make — each remains exactly as stateless and
non-decision-making as its own existing Scope Lock already requires; they
only forward one more already-resolved value, precisely as they already
forward `reasoningContext`. This is disclosed here rather than minimised,
per architectural review's own instruction not to preserve an interface
merely to avoid an honest additive change.

**Naming caution.** `resolveConversationId` is a **stateful, authoritative
operation, not a passive lookup** — calling it may cause an observable,
recorded change to `ConversationEngine`'s own state (registering a
continuity key as newly open) even when it also returns a value. Nothing
about its name should be read as implying it is read-only or side-effect
free; only `ReasoningContextAssembler` carries that obligation, and it
never calls this operation.

### 5.1 Binding Guarantees (Frozen Before Implementation)

Architectural review requires the following four properties to be
contractual guarantees this Contract Design binds any implementation to —
not implementation preferences, and not merely likely behaviour:

1. **Resolution is atomic.** For a given continuity key, two concurrent
   calls to `resolveConversationId` must never both observe "no active
   Conversation" and each independently mint a distinct `ConversationId`.
   At most one `ConversationId` is ever active for a given continuity key
   at any moment. An implementation must serialise the
   check-and-mint sequence for a given key (or more coarsely, across all
   keys) so this cannot occur — this is a correctness requirement of the
   contract, not an optimisation left to the implementation's own
   discretion.
2. **Resolution is idempotent while the Conversation it names remains
   active.** Repeated calls to `resolveConversationId` for the same
   continuity key, made at any point before that key's Conversation is
   considered terminated, must return the identical `ConversationId` every
   time. Without this guarantee, "`ConversationId` is stable for the life
   of a Conversation" (Frozen Fact 2) would be descriptive language only,
   not an operational property a caller may rely on.
3. **`submitTurn` must not re-resolve.** `submitTurn(message,
   conversationId)` must not invoke the continuity rule, consult the
   continuity-key state, or compute or substitute any identifier other
   than the one supplied. Its only permitted responses regarding the
   supplied `conversationId` are: (a) accept it and proceed, when it
   corresponds to a `ConversationId` this engine's own `resolveConversationId`
   has actually produced; or (b) reject it — as an observable failure, per
   Guarantee 4, never a silent substitution — when it is unknown (never
   produced by `resolveConversationId`) or stale (no longer accepted by
   this engine, once a future termination capability exists). Silently
   accepting an unrecognised, caller-supplied identifier would itself be
   "allowing a caller to mint an arbitrary `ConversationId`" by another
   route, and is therefore excluded by the same rejection criterion this
   Unit's frozen requirements already name.
4. **Resolution failure stops the entire pipeline.** If
   `resolveConversationId` fails, or `submitTurn` rejects a supplied
   identifier under Guarantee 3, then: no `ReasoningContext` is assembled;
   no `Turn` is created; no reasoning occurs; and no response is
   delivered. Such a failure propagates unchanged to
   `ParkerRuntime.submitOwnerMessage`'s own existing outer `try`/`catch`
   (Section 10.4), exactly as every other pipeline-stage fault already
   does. It must never be caught and silently converted into "begin a new
   Conversation instead," by any component, as a convenience.

These four guarantees bind whichever concrete mechanism a future
implementation Unit chooses (e.g. a `Mutex`-serialised critical section);
this document does not mandate a specific mechanism, only the observable
guarantees any mechanism must uphold.

---

## 6. Question 5 Answered Directly: The Interface Does Not Remain Unchanged

`ConversationEngine`'s interface **does** change: one new method, one
additive parameter on the existing method. This is not a redesign of what
`ConversationEngine` owns or is responsible for — `19-conversation-engine.md`
Section 4's ownership statement ("Conversation state — which Turns belong
to which Conversation") is realised, not altered, by either operation; both
remain exclusively `ConversationEngine`'s own. It is an additive extension
of *how many operations* the same, single owner exposes to reach that one
responsibility — the same distinction `CONVERSATION_ENGINE_CONTRACT_DESIGN.md`
Section 2 already relied on when it declined to mandate a generation
algorithm. Fact 7 (no redesign absent proven impossibility) is honoured:
nothing about *what* `ConversationEngine` owns, decides, or mutates is
reassigned or removed; only the shape of its own public surface grows to
make its one, unchanged decision reachable at the point the pipeline now
requires it.

---

## 7. Question 4 Answered: What State Is For

- **Identity selection (continuity-key resolution).** State is required
  here: a mapping, owned by `ConversationEngine`, from continuity key
  (`channelId`, `senderPrincipalId`) to the `ConversationId` of whichever
  Conversation is currently open for that key, if any. This is new state
  `InMemoryConversationEngine` does not hold today (its own KDoc states it
  is "deliberately stateless").
- **Turn retention.** State already required, and already held: `Conversation.turnIds`,
  appended to on every Turn bound to that Conversation. Unaffected by this
  revision's own changes beyond being driven by a supplied, not derived,
  identifier.
- **Active Conversation tracking.** State required: which
  `ConversationId`s currently correspond to open Conversations, so
  `submitTurn` can determine `isNewConversation` by checking for an
  existing record, and so `resolveConversationId` knows which continuity
  keys are presently open. This may be the same underlying state as
  identity selection's own mapping, viewed from two directions
  (key→id, id→record) — an implementation choice, not decided here.
- **Termination.** State would be required for a future capability this
  document does not design: marking a Conversation's continuity-key entry
  as closed, so a subsequent message for the same key is resolved as new
  rather than continuing. `19-conversation-engine.md` Section 13 Item 4
  remains exactly as open as before.
- **Reopening.** Not authorised here. Whether a message arriving for a
  continuity key whose prior Conversation has terminated should ever reuse
  that Conversation's old identifier, or must always receive a new one, is
  an open question this document explicitly declines to answer — Section
  3's rejection of pure derivation only establishes that reopening
  *remains possible*, not what its rule should be.
- **Replacement after expiry.** Not authorised here; depends on
  termination existing first, which it does not yet.

**What state is explicitly not for:** computing a `ConversationId` value
from nothing. No lookup is required to know an identifier *exists*, only
to know whether one is already open for a given key, and, once resolved,
what it is. The prior revision's claim that identity derivation itself
needed no state remains true in isolation — this revision's correction is
that "no state to compute an identifier" is not the same claim as "no
state to recognise continuity," and only the latter is what this Unit
actually requires.

---

## 8. Envelope (Unchanged in Shape, Reduced in Role)

The Resolved Inbound Envelope from the prior revision is retained
unchanged in shape — the original `InboundOwnerMessage` paired with a
resolved `ConversationId` — but its role narrows: it exists solely to
carry the already-resolved value to the Assembler. Propagation to
`ConversationEngine`'s own later `submitTurn` call happens via the
additive parameter described in Section 5, not via the envelope — the
envelope is not threaded through the coordinator chain; the plain
`ConversationId` value is.

---

## 9. Participating Components, Inputs, and Outputs

| Component | Role under this contract | Change from today |
|---|---|---|
| `ParkerRuntime.submitOwnerMessage` | Calls `conversationEngine.resolveConversationId(message)` once, early; builds the envelope; calls the Assembler; forwards the resolved `ConversationId` alongside `message`/`reasoningContext` into the existing coordinator chain. | Additive: one new call, one new local value, one changed argument list on one existing call. |
| `ConversationEngine` | Gains `resolveConversationId` (new); `submitTurn` gains an additive `conversationId` parameter and no longer decides identity. Remains the sole owner of Conversation membership, the sole minter of new `ConversationId`s, and the sole component holding continuity state. | Additive interface change — see Sections 5-6. |
| `ReasoningContextAssembler` | Receives the Resolved Inbound Envelope, unchanged from the prior revision. Never calls `ConversationEngine`. | Additive input-type change only, as before. |
| `ConversationReplyCoordinator`, `CommunicationConversationCoordinator`, `ConversationTurnReasoningCoordinator` | Each forwards the resolved `ConversationId` as one additional, pass-through parameter. No sequencing, decision, or state change. | Additive parameter on one existing method each. |
| `ReplyDeliveryCoordinator`, `CommunicationIntake` | Unchanged; neither needs the resolved identifier. | None. |
| Communication Channel (`DefaultLocalTextChannel`) | Unchanged. | None. |

---

## 10. Paths

### 10.1 Success path (general)

Inbound message accepted → `ConversationEngine.resolveConversationId`
called once (the one authoritative decision) → envelope built → Assembler
runs → resolved `ConversationId` forwarded, unchanged, through the
existing coordinator chain → `ConversationEngine.submitTurn(message,
conversationId)` consumes it, performs Turn/Conversation bookkeeping only
→ reasoning proceeds.

### 10.2 New-conversation path

`resolveConversationId` finds no open Conversation for the continuity key,
mints a new `ConversationId`, records the key as now open against it.
`submitTurn` later finds no existing `Conversation` record for that exact
identifier and constructs one, setting `isNewConversation = true`.

### 10.3 Continuing-conversation path

`resolveConversationId` finds an open Conversation for the continuity key
and returns its existing `ConversationId` unchanged. `submitTurn` later
finds an existing `Conversation` record for that exact identifier and
appends the new Turn, setting `isNewConversation = false`.

### 10.4 Continuity-resolution failure path

A fault inside `resolveConversationId` (e.g. a future revision introduces
a dependency capable of failing — none exists in this design), or a
rejection `submitTurn` raises under Section 5.1 Guarantee 3 (an unknown or
stale supplied identifier), propagates unchanged to
`ParkerRuntime.submitOwnerMessage`'s own existing outer `try`/`catch`,
classified `PipelineStage.UNKNOWN`, identically to how an Assembler-stage
fault already propagates. Per Section 5.1 Guarantee 4, this failure stops
the entire pipeline: no `ReasoningContext` is assembled, no `Turn` is
created, no reasoning occurs, and no response is delivered. On a
resolution-stage failure, neither the Assembler nor `submitTurn` is ever
reached; on a `submitTurn`-stage rejection, the Assembler has already run
(Guarantee 3's failure surfaces later than Guarantee 4's earliest possible
point), but no Turn is created and no reasoning or delivery follows.

---

## 11. Deterministic Behaviour

`resolveConversationId` is deterministic in the sense this codebase already
uses for stateful, read-consulting operations (e.g.
`IdentityService.resolve`): given the same continuity key and the same
prior sequence of resolutions, it returns the same answer. It is not
claimed to be a pure function of the key alone — Section 3 explains why
that claim was withdrawn.

---

## 12. Relationship to `ConversationEngine`, `ReasoningContextAssembler`, and Conversation History Source

`ConversationEngine` ownership is preserved exactly: it remains the only
component that ever constructs a `Conversation` or `Turn`, mutates
`turnIds`, mints a new `ConversationId`, or determines continuity — now
exercised through two additive operations instead of one, per Section 6.
`ReasoningContextAssembler`'s dependencies, determinism, statelessness, and
side-effect-freedom are unchanged; it still never references
`ConversationEngine`, directly or indirectly — the value it receives was
resolved entirely by `ParkerRuntime`'s call to `ConversationEngine`, not by
the Assembler itself. Conversation History Source (Sprint 11 Unit 4) gains
the same benefit as before: a real, singly-authoritative `ConversationId`
available from the moment `resolveConversationId` returns, unchanged
through to any future read — this document still does not design that
read.

---

## 13. What This Document Does Not Decide

- The exact internal representation of `ConversationEngine`'s new
  continuity-key state (e.g. map shape, thread-safety mechanism) —
  deferred to implementation. **Not deferred:** the four observable
  guarantees any such mechanism must uphold (Section 5.1) — those are
  frozen by this document, only their realisation is left open.
- Termination, reopening, and replacement-after-expiry rules
  (`19-conversation-engine.md` Section 13 Item 4) — remain open, per
  Section 7.
- Whether a Conversation may span more than one channel
  (`19-conversation-engine.md` Section 13 Item 5) — unresolved; the
  continuity key remains keyed on `channelId`, foreclosing cross-channel
  span unless a future revision addresses it explicitly.
- Any Kotlin naming, package placement, or exact signatures for
  `resolveConversationId` or the Resolved Inbound Envelope — described
  here only as concepts with defined behaviour.
