# Conversation History Source — Contract Design

## Status

**Sprint 11, Unit 6. PES-001 Stage 3 (Contract Design), folding in the
mandatory Phase 0/Phase 1 repository and governance review that precedes
it.** Builds on `CONVERSATION_HISTORY_SOURCE_IMPLEMENTATION_PLAN.md`
(Sprint 11, Unit 4) and revises `CONVERSATION_HISTORY_SOURCE_SCOPE_LOCK.md`
(same Unit) in place, now that the two findings Unit 4 recorded as
blocking a full design have been re-examined against the repository's
*current* state, not Unit 4's.

---

## 1. Phase 0 — Repository Review (fresh reads, this Unit)

Read directly, not from memory, immediately before this document was
written: `docs/architecture/parker-constitution.md`,
`docs/architecture/19-conversation-engine.md` (via the documents that cite
it), `docs/architecture/CONVERSATION_ENGINE_CONTRACT_DESIGN.md` (via
citation in `src/interfaces/ConversationEngine.kt`),
`docs/architecture/CONVERSATION_CONTINUITY_CONTRACT_DESIGN.md` in full,
`docs/implementation/CONVERSATION_HISTORY_SOURCE_IMPLEMENTATION_PLAN.md`
in full, `docs/implementation/CONVERSATION_HISTORY_SOURCE_SCOPE_LOCK.md` in
full, `docs/architecture/PRODUCTION_REASONING_CONTEXT_CONTRACT_DESIGN.md`
(Section 4.2, Section 12-equivalent cross-references),
`docs/architecture/AUTHENTICATION_AND_TRUST_GOVERNANCE.md`,
`docs/architecture/09-trust-framework.md`,
`docs/architecture/42-authentication-framework.md`,
`docs/architecture/IMPLEMENTATION_GAPS.md` (Gap #53 and its most recent
Sprint 11 Unit 5 update), and the current, real source of
`src/interfaces/ConversationEngine.kt`,
`src/runtime/InMemoryConversationEngine.kt`,
`src/interfaces/ReasoningContextAssembler.kt`,
`src/runtime/DefaultReasoningContextAssembler.kt`,
`src/composition/ParkerRuntime.kt`, and `src/runtime/ResponseComposer.kt`.

**Central finding, not previously recorded this precisely.** Re-reading
`InMemoryConversationEngine.submitTurn` directly (not from memory) shows
that `conversationsById` stores `Conversation` values, and
`Conversation.turnIds` is `List<TurnId>` — a list of *identifiers*, not of
`Turn` values. The `Turn` object `submitTurn` constructs on each call
(carrying the actual `InboundOwnerMessage`, including its `text`) is
returned once, inside `ConversationDisposition`, to that one caller, and is
never itself stored anywhere. **`ConversationEngine`'s own owned state
today retains that a Turn happened and in what order, but not what a Turn
*contained*.**

This sharpens, rather than contradicts, Unit 4's own Finding 1
(`ConversationEngine exposes no read operation`) and supersedes Unit 4's
Section 1 assumption that Conversation History Source could simply read
"the prior Turns already belonging to it, as `ConversationEngine`'s own
owned state ... already records them" — today, it does not; it records
only their identifiers and order. Section 6 of the Implementation Plan
anticipated needing exactly this kind of resolution ("a future Contract
Design must decide whether Conversation History Source is backed by
`ConversationEngine` itself through some new, narrower, read-only
surface ... naming this boundary does not presuppose its construction").
This document is that resolution (Section 4, below).

Unit 4's own **Finding 2** (`ReasoningContextAssembler.assemble` runs
before any `ConversationId` is known, so a reader would have nothing to
query with) is **resolved, not merely re-examined** — by Sprint 11 Unit 5.
`ParkerRuntime.submitOwnerMessage` now calls
`conversationEngine.resolveConversationId(message)` and constructs a
`ResolvedInboundMessage` *before* invoking `reasoningContextAssembler.assemble`,
so a `ConversationId` is available to the Assembler, and to anything the
Assembler consults, from before a `Turn` exists. Finding 2 is closed; this
document does not re-open it.

---

## 2. Phase 1 — Governance Review / Conflict Check

Checked directly against the frozen, current state of each governing
document and component:

- **Authentication & Trust** (`AUTHENTICATION_AND_TRUST_GOVERNANCE.md`):
  no reference to Conversation History, `ConversationEngine`, or any
  reasoning-pipeline read boundary anywhere in its eleven sections; its own
  Constraints section states it "must not alter Conversation History" —
  a one-way constraint on *that* document, not a dependency this Unit must
  satisfy. No conflict; Authentication & Trust is untouched by this Unit
  and this Unit introduces no authentication, authorisation, or trust
  concept of its own (Constitution: "Cognition proposes. Trust
  authorises. Runtime executes." — Conversation History Source proposes
  and authorises nothing; it is capability, read only, exactly as
  `CONVERSATION_HISTORY_SOURCE_SCOPE_LOCK.md` Section 7 already states).
- **Conversation Identity** (`resolveConversationId`,
  `CONVERSATION_CONTINUITY_CONTRACT_DESIGN.md`): Section 12 of that
  document states plainly that Conversation History Source "gains the
  same benefit as before: a real, singly-authoritative `ConversationId`
  available from the moment `resolveConversationId` returns ... this
  document still does not design that read." No conflict — this document
  is the read design that Section 12 explicitly deferred, and does not
  touch identity resolution, continuity keys, or `resolveConversationId`
  in any way.
- **`ConversationEngine`'s existing two-operation contract**
  (`resolveConversationId`, `submitTurn`): neither operation's signature,
  behaviour, or the four Binding Guarantees (Continuity Contract Design
  Section 5.1) are altered by this design (Section 4, below, adds a third,
  read-only operation as an additive extension — the same pattern already
  used once this Sprint for `resolveConversationId` itself).
- **`ReasoningContextAssembler`**: its determinism, statelessness, and
  side-effect-freedom (Reasoning Context Contract Design Section 5) are
  preserved — a new read-only collaborator is not a side effect, exactly
  as `identityService.resolve` and `toolRegistry.listAll` already are not.
- **Conversation History Source Scope Lock (Unit 4)**: Sections 2
  (Exclusions), 3 (Governing Principle), 4 (Ownership), 5 (Lifetime), 6
  (Threading), and 7 (Constitution) all remain consistent with the design
  below and are carried forward, revised in place only where Section 1's
  "as `ConversationEngine`'s own owned state already records them" needed
  the correction Section 1 of this document makes.

**Conclusion: no architectural conflict exists.** One open question,
already anticipated and explicitly deferred by governing documents
(Implementation Plan Section 6; Continuity Contract Design Section 12), is
resolved by this design: how Conversation History Source is backed, given
`ConversationEngine`'s current owned state retains Turn order but not Turn
content. This is a design decision, not a conflict, and is made in Section
4 below with its reasoning shown in full.

---

## 3. Responsibilities (unchanged from Unit 4, restated as binding)

Conversation History Source owns exactly one responsibility: **given a
`ConversationId`, retrieve the already-existing, ordered, read-only Turns
recorded for it.** It does not decide how many Turns are relevant, does
not rank them, does not summarise them, does not reshape them, does not
authenticate anyone, and does not authorise anything. Nothing about
identity resolution, Turn creation, or Conversation mutation belongs to
it — those remain `ConversationEngine`'s own, sole, unchanged
responsibility.

---

## 4. Interface Design

### 4.1 The abstraction

```kotlin
// src/interfaces/ConversationHistorySource.kt (new file)
fun interface ConversationHistorySource {
    suspend fun history(conversationId: ConversationId): List<Turn>
}
```

One operation, `suspend`-declared (Scope Lock Section 6's own stated
expectation, now confirmed rather than merely anticipated). Returns the
Turns already recorded for `conversationId`, **in the order they were
recorded** (oldest first) — the same order `Conversation.turnIds` already
preserves, since `submitTurn` only ever appends. Returns an **empty
list**, never throws, for a `conversationId` with no Turns recorded yet
(including one `ConversationEngine` has never seen) — retrieval of
"nothing recorded" is not itself a failure; this mirrors
`IdentityService.resolve`'s own already-established "return null/empty,
do not throw, for a not-found lookup" convention
(`DefaultReasoningContextAssembler`'s own KDoc cites the identical
precedent for `IdentityService.resolve`).

This single method is the entirety of the interface. No `count`, no
`since`, no pagination, no filter parameter — Scope Lock Section 2
excludes "relevance, ranking, or any semantic criterion" from this
boundary; a future component may add those, on top of this one, if ever
justified, but this Unit does not anticipate that need (Steven's own
final instruction: a smaller, precisely-scoped component is preferred
over one that anticipates future capability).

### 4.2 Backing implementation — resolving Section 1's finding

**Decision: `InMemoryConversationEngine` implements `ConversationHistorySource`
directly, as a second, narrower interface over the same instance and the
same owned state — not a second store, and not a new method added to the
`ConversationEngine` interface itself.**

Concretely:

- `InMemoryConversationEngine` gains one additional private map,
  `turnsById: MutableMap<TurnId, Turn>`, populated inside `submitTurn`'s
  existing `stateLock.withLock` block, at the same moment `turn` is
  already constructed there today. This is not new authority — `submitTurn`
  already constructs every `Turn` it will ever be asked about; this change
  only retains what it already, momentarily, held.
- `InMemoryConversationEngine.history(conversationId)` reads
  `conversationsById[conversationId]?.turnIds.orEmpty()` and maps each
  `TurnId` through `turnsById`, returning the result — a pure read, no
  lock required beyond what a `Mutex.withLock`-guarded snapshot read
  needs for consistency with concurrent `submitTurn` calls (Section 6,
  below).
- `class InMemoryConversationEngine : ConversationEngine, ConversationHistorySource`
  — one concrete class, two interfaces, exactly as many components in
  this codebase already expose a broader implementation through a
  narrower declared type at their point of injection (the same shape
  `DefaultReasoningContextAssembler` already gives `ReasoningContextAssembler`
  callers no way to reach anything beyond `assemble`).

**Why not add a read method to the `ConversationEngine` interface
itself.** A caller holding a `ConversationHistorySource`-typed reference
must be structurally unable to call `resolveConversationId` or
`submitTurn` — Scope Lock Section 2's "Conversation History Source ...
never calls anything equivalent to `submitTurn`" is best enforced by the
Kotlin type system, not by convention alone. Declaring `history` on a
*separate* interface, rather than as a third method on `ConversationEngine`,
means the Reasoning Context Assembler (Section 5, below) can be given a
`ConversationHistorySource` reference and nothing more — the same
capability-narrowing discipline `ConversationEngine.kt`'s own KDoc already
credits for why `ReasoningContextAssembler` "never references
`ConversationEngine`, directly or indirectly."

**Why not an independent store.** `CONVERSATION_HISTORY_SOURCE_IMPLEMENTATION_PLAN.md`
Section 3 and `CONVERSATION_HISTORY_SOURCE_SCOPE_LOCK.md` Section 3 both
already require that Conversation History Source be "a projection of
`ConversationEngine`'s own owned state ... never a second source of truth
for it." A second, independently written store — even one `ConversationEngine`
itself writes to — creates exactly that second source of truth, and a new
category of bug this Unit has no reason to accept (the two stores
disagreeing after a partial failure). Sharing one instance's one set of
maps, exposed through two interfaces, makes disagreement structurally
impossible rather than merely disciplined-against.

### 4.3 What Turn content is actually available — a disclosed limitation

`Turn.message` is an `InboundOwnerMessage` — the owner's inbound message
only. **No Turn record anywhere in this runtime today captures the
outbound reply Parker composed for it.** `ResponseComposer` constructs an
`OutboundParkerResponse` downstream of `ConversationEngine.submitTurn`,
correlated only by `CorrelationId`, and nothing writes it back to
`Conversation` or `Turn`. Consequently, **Conversation History Source
returns a one-sided history: the owner's own prior messages, in order,
never Parker's own prior replies.**

This is disclosed here, and in `IMPLEMENTATION_GAPS.md` (Section 7,
below), rather than silently narrowed or worked around — inventing a
second record of outbound replies, or retrofitting `Turn` to carry one,
is exactly the kind of scope expansion Steven's own final instruction
warns against ("a smaller component with precise responsibilities is
preferred over a larger component that anticipates future capabilities").
A future Unit, separately scoped and separately justified, may extend
`Turn` or `ConversationEngine`'s own owned state to also retain outbound
replies; this Unit does not.

---

## 5. Integration Point

`DefaultReasoningContextAssembler` gains one additional constructor
dependency, `conversationHistorySource: ConversationHistorySource`,
alongside its existing `identityService` and `toolRegistry`. Inside
`assemble`, after reading `resolvedMessage.conversationId` (already
present, Sprint 11 Unit 5), it calls
`conversationHistorySource.history(resolvedMessage.conversationId)` and
renders zero or more additional entries — one per returned `Turn`, in the
order returned — immediately after the existing "Current conversation"
entry. Rendering format mirrors the Assembler's own existing convention
(a plain, labelled string entry; no structured type, no markup) — e.g.
`"Prior message: <text> (from <senderPrincipalId>, at <receivedAt>)"` —
decided by the Assembler, not by Conversation History Source, exactly as
Scope Lock Section 1 already requires ("Nothing about assembling that
excerpt into a `ReasoningContext` entry ... belongs to it").

No truncation, ranking, or volume limit is applied at either the source
or the Assembler by this Unit — every Turn `history` returns is rendered.
This is a disclosed, deliberate scope boundary (Section 4.1 above; Gap
entry, Section 7 below), not an oversight: bounding history length is a
relevance/volume decision Scope Lock Section 2 explicitly excludes from
this boundary, and the Assembler's own Contract Design has never owned
that decision either. It remains open for a future Unit.

`ParkerRuntime` (the sole production composition root, Scope Lock Section
4) constructs one `InMemoryConversationEngine` instance and injects it
into `DefaultReasoningContextAssembler`'s constructor twice — once as
`ConversationEngine` (unchanged, existing wiring) and once as
`ConversationHistorySource` (new). No second instance, no second map, no
second startup step beyond re-ordering: `InMemoryConversationEngine` must
now be constructed before `DefaultReasoningContextAssembler`, reversing
today's construction order (Assembler is currently built first). This is
a pure ordering change with no behavioural consequence — `InMemoryConversationEngine`'s
own constructor takes only `identityService`, already available at the
point the Assembler is built today.

---

## 6. Concurrency

`InMemoryConversationEngine`'s existing `stateLock` (a single `Mutex`,
Continuity Contract Design Section 5.1) is extended to also guard
`turnsById`'s single write (inside `submitTurn`) and `history`'s single
read (`conversationsById` lookup plus `turnsById` lookups) — the same
lock, no second lock, no per-key lock, mirroring `InMemoryConversationEngine.kt`'s
own existing KDoc rationale for why one lock, held only across the map
operations themselves, is correct here: "correctness, not throughput, is
this revision's own concern, and no evidence of contention exists to
justify more." `history` never blocks on, or is blocked by, anything
downstream of `ConversationEngine` (reasoning, model invocation, response
composition, delivery) — identical discipline to `resolveConversationId`
and `submitTurn`.

---

## 7. Documentation Consequences (recorded here, applied in Section 3 of
the revised Scope Lock and in `IMPLEMENTATION_GAPS.md`)

- `CONVERSATION_HISTORY_SOURCE_SCOPE_LOCK.md` Section 1's "as `ConversationEngine`'s
  own owned state already records them" is corrected in place: owned
  state is *extended*, additively, by this Unit's implementation, to
  actually retain Turn content — it did not already do so.
- A new `IMPLEMENTATION_GAPS.md` entry (or a Gap #53 successor) discloses
  the one-sided (inbound-only) history limitation from Section 4.3, and
  the unbounded-volume, no-ranking limitation from Section 5 — both
  deliberate, both open for a future, separately-scoped Unit.

---

## 8. What This Document Does Not Decide

- Any bound on history volume, age, or relevance (Section 5).
- Whether outbound replies are ever captured (Section 4.3) — a distinct,
  future, separately-justified extension to `Turn`/`ConversationEngine`,
  not proposed here.
- Termination, expiry, reopening, or cross-channel span
  (`CONVERSATION_CONTINUITY_CONTRACT_DESIGN.md` Section 13 — unchanged,
  not touched by this document).
- Any Memory, World Model, embeddings, or semantic-retrieval capability —
  permanently excluded (Scope Lock Section 2), not deferred.

---

## Conclusion

Conversation History Source is designed here as a single-method,
`suspend`-declared, read-only interface, `ConversationHistorySource`,
backed by `InMemoryConversationEngine` implementing it directly as a
second, narrower view over the same owned state `ConversationEngine`
already has sole authority over — no second store, no new authority, no
change to `ConversationEngine`'s own interface, identity resolution, or
Turn-submission behaviour. Two disclosed, deliberate limitations (Section
4.3, Section 5) are named rather than worked around, consistent with
optimising for architectural correctness over feature completeness.
