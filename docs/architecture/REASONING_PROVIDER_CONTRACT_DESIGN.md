# Reasoning Provider Contract Design

## Status

Design proposal, not yet reviewed or accepted. **This document is
contract design only.** No Kotlin is implemented, proposed as a diff, or
changed by it — every shape below is described in prose, not as a
`kotlin`-fenced signature block. Neither `src/` nor `tests/` is touched.
No prompt template, token limit, streaming behaviour, provider selection
mechanism, retry policy, network protocol, local/remote execution
decision, model identifier, or provider implementation is introduced.

### Amendment 1 — ReasoningSubject

**Pending acceptance.** This amendment remains pending until Independent
Constitutional Review and governance acceptance are complete; it is not
yet applied. This document is amended by Amendment 1, which replaces the
`turn: Turn` field on `ReasoningProviderRequest` (Section 2) with a
closed, two-case sealed selector, `ReasoningSubject`, so that a second,
already-authorised caller — Evidence Intelligence — may supply a
reasoning subject of its own without depending on Conversation Engine.
Amendment 1 implements the constitutional interpretation recorded in
`docs/reviews/EVIDENCE_INTELLIGENCE_UNIT_3_CONSTITUTIONAL_DECISION_MEMORANDUM.md`
(the Memorandum): that `reasoning-context.md`, not `Turn`, is the
constitutional authority governing what a reasoning provider reasons
over, and that `Turn` is Conversation Engine's own first concrete
instantiation of that authority, not its universal shape. Amendment 1's
own scope was fixed in advance by
`docs/reviews/REASONING_PROVIDER_CONTRACT_DESIGN_AMENDMENT_PROPOSAL.md`
and `docs/reviews/REASONINGSUBJECT_CONTRACT_DESIGN_STUDY.md`; every
paragraph below marked "Amendment 1" implements exactly what those two
documents identified, and no other paragraph in this document is
altered by it. `Turn`, `Conversation`, and `ConversationEngine`'s
exclusive construction of both remain entirely unchanged
(`CONVERSATION_CONTINUITY_CONTRACT_DESIGN.md` Section 12); Evidence
Intelligence continues to reuse the `ReasoningProvider` interface
exactly as CDR-007 already authorises, with no new dependency on
Conversation Engine.

### Why this unit exists

`docs/architecture/REASONING_PROVIDER_ARCHITECTURE.md` received Stage 1
Architecture Review and was accepted — purpose, responsibilities,
non-responsibilities, ownership, trust boundary, runtime boundary, a
conceptual lifecycle, architectural invariants, and a security model, but
deliberately no field-level Kotlin shape (its own Section 14, "Open
Questions," names the exact mechanism questions this document now begins
to answer: Item 1, the invocation mechanism; Item 2, the exact shape of
"reasoning output"). This document performs the field-level design pass
that document deferred — exactly the same relationship
`CONVERSATION_ENGINE_CONTRACT_DESIGN.md` bears to
`19-conversation-engine.md`, and `COMMUNICATION_CONTRACT_DESIGN.md` bears
to `COMMUNICATION_CHANNEL_ARCHITECTURE.md`.

**A genuine gap this pass surfaced, not invented — but not an unaddressed
one either.** `REASONING_PROVIDER_ARCHITECTURE.md` has no Section-11-style
Failure Model of its own — it never states what happens when a Reasoning
Provider fails, times out, or cannot produce a confident interpretation.
But `19-conversation-engine.md` Section 11 (Failure Model) already
discusses this exact scenario directly: "A reasoning provider that fails,
times out, or cannot confidently interpret a Turn is a failure to be
surfaced, not papered over... the Conversation Engine's correct
behaviour when interpretation itself is unreliable is to make that
visible." That passage places failure-surfacing responsibility on the
*calling* component, not on the Reasoning Provider's own response shape
— which reinforces, rather than merely fails to forbid, this document's
decision not to define a failure or error variant on the response object
below (Section 3, Section 9, Section 10). Because this document must not
invent architecture Stage 1 never authorised, it does **not** define a
`Failed` domain variant. This is named explicitly, not silently worked
around — see the Review section's own callout, and Section 9 (Deferred
Items). A future Architecture amendment to
`REASONING_PROVIDER_ARCHITECTURE.md` — out of this document's own
authority, since this document must not modify any existing architecture
document — remains the right place to give the Reasoning Provider its
own dedicated failure model, if one is ever needed beyond what Chapter
19 Section 11 already establishes for the calling component's side of
it.

## Review

Reviewed, in authority order:

1. `docs/architecture/parker-constitution.md`.
2. `docs/architecture/PARKER_ENGINEERING_STANDARD.md` (PES-001) — Stage
   2A's own definition: reviews prior art without assuming it is correct;
   determines the minimum required set of public contracts, explicitly
   stating what is required, excluded, and deferred, and why; resolves
   named outstanding design questions against approved architecture,
   never inventing new architecture to answer them; states whether a
   separate "Runtime" wrapper interface is needed; ends with a
   Self-Traceability Review.
3. `docs/architecture/REASONING_PROVIDER_ARCHITECTURE.md` — the Stage 1
   Architecture this document implements as contracts and does not
   redefine, redesign, or revise. **Finding, addressed via a sibling
   document, not invented here:** it has no Section 11-style Failure
   Model of its own. `19-conversation-engine.md` Section 11 already
   covers this scenario, placing failure-surfacing on the calling
   component rather than the Reasoning Provider's own response shape —
   see Status, above, and Section 9.
4. `docs/architecture/19-conversation-engine.md` — Section 2
   (responsibilities engaging a reasoning provider), Section 6 (Turn
   diagram), Section 11 (Failure Model, read specifically for its direct
   treatment of "a reasoning provider that fails, times out, or cannot
   confidently interpret a Turn," which grounds this document's decision
   not to define a `Failed` response variant — Status, Section 3, Section
   9, Section 10), Section 13 Item 6 (the exact dependency this document
   closes the Kotlin-shape half of).
5. `docs/architecture/CONVERSATION_ENGINE_CONTRACT_DESIGN.md` — the
   direct upstream contract set this document consumes without
   modification: `ConversationId`, `TurnId`, `Conversation`, `Turn`,
   `ConversationDisposition`; Section 8's own deferral of "reasoning-
   provider contract" in full, which this document now resolves the
   shape of.
6. `docs/architecture/COMMUNICATION_CONTRACT_DESIGN.md` — the further
   upstream contract set reused transitively through `Turn.message`:
   `InboundOwnerMessage`, `CorrelationId`, `PrincipalId`.
7. `docs/architecture/reasoning-context.md` — the three-layer knowledge
   model and its own information-flow chain naming "Reasoning Provider"
   and "Reasoning Context" explicitly, neither of which this document —
   or any document before it — has previously given a field-level shape.
8. `src/interfaces/CommunicationIntake.kt` — the direct, already-
   implemented Kotlin precedent for this document's own interface
   minimalism and identifier style, mirrored again here.
9. `src/contracts/PlanDecision.kt` (`PlanningRequest`) — confirms
   `PlanningRequest.goal: String` requires no change to receive a
   Reasoning Provider's output, once routed there by the Conversation
   Engine.

---

## Constitutional Boundaries

Restated up front, identical in substance to
`REASONING_PROVIDER_ARCHITECTURE.md` Sections 3, 5, and 11, not
re-derived differently here:

- **A Reasoning Provider proposes. It authorises nothing, executes
  nothing, and accesses no Tool.** Restating ADR-001, ADR-010, and the
  Constitution's "Reasoning providers may propose. They may not authorize
  or execute."
- **No confidence score substitutes for Permission Engine evaluation.**
  Nothing in the contract below carries, or is defined to carry, a
  confidence value that any caller could mistake for an authorisation
  signal.
- **Replacement must never alter Parker's trust guarantees.** Every
  contract below is satisfiable by any concrete implementation — a local
  model, a remote API, a rule-based system — without privileged access
  unavailable to another, restating the Constitution's Replaceable
  Reasoning Providers principle directly.
- **Cognition proposes. Trust authorises. Runtime executes — with no
  shortcut for output that arrived via a Reasoning Provider.** No
  operation this document defines calls `ExecutionPipeline`,
  `PermissionEngine.evaluate`, `PlannerRuntime`, `ToolRegistry`,
  `MemoryStore`, or `WorldModel`.

---

## Contract Minimalism Review — Summary

| Candidate | Determination |
| --- | --- |
| `ReasoningProvider` (interface) | **Include.** The one public contract `19-conversation-engine.md` Section 13 Item 6 and `CONVERSATION_ENGINE_CONTRACT_DESIGN.md` Section 8 both require to exist. |
| `ReasoningProviderRequest` | **Include.** The minimal input a Reasoning Provider needs — see Section 2. **Amendment 1:** carries `subject: ReasoningSubject` in place of `turn: Turn`. |
| `ReasoningProviderResponse` | **Include, as a three-variant sealed type** (`Goal`, `Reply`, `NoAction`) — see Section 3, Section 10. |
| `ReasoningContext` | **Include, as a new, deliberately minimal type** — not a reuse, despite the name's prior appearance in `reasoning-context.md`; no field-level shape for it exists anywhere in this repository before this document. See Section 2, Section 10. |
| `ReasoningSubject` | **Include, Amendment 1.** A closed, two-case sealed selector (`OfTurn`, `OfEvidenceAnalysisRequest`), generalising this document's own request shape to the caller-agnostic reasoning subject `reasoning-context.md`'s own constitutional-tier authority already defines ("a task"), reconciling this document's field-level shape with that authority. See Section 2. |
| A failure/error variant on `ReasoningProviderResponse` | **Exclude — reinforced by, not merely undecided by, Stage 1.** `REASONING_PROVIDER_ARCHITECTURE.md` names no failure model of its own, but `19-conversation-engine.md` Section 11 already assigns failure-surfacing to the calling component, not the Reasoning Provider's own response shape. Implementation-level faults remain expressible outside this sealed type (Section 3). See Status, Section 9. |
| A confidence/score field | **Exclude.** No concrete consumer; would risk being mistaken for an authorisation signal, contrary to the Constitutional Boundaries above. |
| A correlation/echo-back identifier on the response | **Exclude.** The request/response shape below is a single call and its single result; nothing requires re-matching a response to a request across a gap. If a future, separately-scoped invocation mechanism (Architecture Section 14 Item 1) requires one, it is added additively then. |
| A caller-principal field on the request | **Exclude.** Ties to Architecture Section 14 Item 5 (whether a Reasoning Provider needs an identity distinct from its caller's), explicitly left open there and not resolved here. |
| A separate `ReasoningProviderRuntime` wrapper interface | **Exclude — one interface suffices.** See Section 1. |
| A `ReasoningProviderRegistry` | **Exclude.** Already excluded at the architecture level (Architecture Section 13); this document does not reopen it. |

Net result: **five new contracts** (`ReasoningProvider`,
`ReasoningProviderRequest`, `ReasoningProviderResponse`,
`ReasoningContext`, `ReasoningSubject` — the last added by Amendment 1),
**six existing contracts reused unchanged** (`ConversationId`, `TurnId`,
`Turn`, `PrincipalId`, `CorrelationId` — three of them transitively, via
`Turn` — and, since Amendment 1, `EvidenceAnalysisRequest`, reused via
`ReasoningSubject.OfEvidenceAnalysisRequest`), **one modified**
(`ReasoningProviderRequest`, Amendment 1: `turn: Turn` replaced by
`subject: ReasoningSubject`), and **one genuine architectural gap
disclosed rather than papered over** (no failure model).

---

## 1. Public `ReasoningProvider` Interface

**`ReasoningProvider`** — the single public interface for reasoning
invocation, mirroring `CommunicationIntake`'s and `ConversationEngine`'s
identical shape and minimalism (one operation, one request type, one
response type):

- **One operation:** given a `ReasoningProviderRequest`, return a
  `ReasoningProviderResponse`.
- **Its responsibility ends there.** This operation does not route,
  submit, deliver, persist, or publish anything. It interprets its input
  and returns reasoning output — restating
  `REASONING_PROVIDER_ARCHITECTURE.md` Section 2 exactly.
- **No separate "Runtime" wrapper interface is needed.** Per PES-001
  Stage 2A's own required determination: exactly one interface,
  `ReasoningProvider`, suffices. Unlike `PlannerRuntime`/`AgentRuntime`,
  which expose operations across a session or run's own multi-step
  lifecycle, a Reasoning Provider's entire contract surface is one
  request in, one response out (Architecture Section 4: no retained
  state; Section 7: one invocation, one return). There is no session,
  run, or multi-step lifecycle of its own to wrap.
- **No invocation protocol is specified.** Whether this operation is
  synchronous, asynchronous, local, or remote is explicitly not decided
  here — restating Architecture Section 14 Item 1, still open, and this
  document's own "must not define... network protocols... local vs
  remote execution" boundary.

## 2. Minimal Request Object — `ReasoningProviderRequest`

The minimal input a Reasoning Provider needs, reusing existing shapes
wherever one already exists:

- **`subject: ReasoningSubject`** — **Amendment 1.** Replaces the
  original `turn: Turn` field with a closed, two-case sealed selector,
  generalising this document's own request shape to the caller-agnostic
  reasoning subject `reasoning-context.md`'s own constitutional-tier
  authority already defines ("a task"), while leaving `Turn` itself, and
  Conversation Engine's exclusive construction of it, completely
  unmodified (`CONVERSATION_CONTINUITY_CONTRACT_DESIGN.md` Section 12).
  `ReasoningSubject`'s two cases:

  - **`OfTurn(turn: Turn)`** — reused unchanged
    (`CONVERSATION_ENGINE_CONTRACT_DESIGN.md` Section 2). Carries
    `turnId`, `conversationId`, and `message: InboundOwnerMessage`
    (which itself carries `senderPrincipalId`, `text`, `correlationId`,
    and `timestamp`) as one already-defined bundle. This document does
    not duplicate any of `Turn`'s own fields as separate top-level
    fields on this case — restating this document's own instruction not
    to duplicate existing contracts, and mirroring
    `CONVERSATION_ENGINE_CONTRACT_DESIGN.md`'s identical discipline of
    embedding an already-shaped type rather than re-deriving its
    fields.
  - **`OfEvidenceAnalysisRequest(request: EvidenceAnalysisRequest)`** —
    reused unchanged (`EVIDENCE_INTELLIGENCE_CONTRACT_DESIGN.md`
    Section 4; `src/interfaces/EvidenceIntelligence.kt`). Carries
    Evidence Intelligence's own already-governed evidence-artefact
    references, Memory Core references, analysis classification, and
    requesting principal, as one already-defined bundle — reused
    exactly as CDR-007 already authorises Evidence Intelligence to
    orchestrate `ReasoningProvider` "as internal analytical
    mechanisms."

  **Relationship to `EvidenceAnalysisRequest.reasoningContext`
  (Amendment 1, correction).** `EvidenceAnalysisRequest`
  (`EVIDENCE_INTELLIGENCE_CONTRACT_DESIGN.md` Section 4) carries its own
  optional `reasoningContext` field, reachable, once wrapped in
  `OfEvidenceAnalysisRequest`, alongside this request's own top-level
  `reasoningContext` field (immediately below). This document does not
  modify `EvidenceAnalysisRequest`, and introduces no third context of
  any kind — it fixes only which of the two already-existing fields
  this request's own invocation uses: **this request's own top-level
  `reasoningContext` field (immediately below) is the sole
  `ReasoningContext` value `ReasoningProvider.reason` ever consults, for
  every case of `subject`, including `OfEvidenceAnalysisRequest`.**
  Whatever value `EvidenceAnalysisRequest.reasoningContext` carries
  internally, for Evidence Intelligence's own purposes, is not read, not
  merged, not compared, and not otherwise given any effect on this
  invocation. This eliminates precedence (the top-level field always
  governs), duplication and conflict (the nested field is never
  consulted, so it cannot disagree with anything), enrichment (no
  merge occurs), and omission (the top-level field is already mandatory
  on this request, so the invocation's context is always defined
  regardless of whether the nested field is present, absent, or
  differs).

  **Frozen properties (Amendment 1; restated from**
  **`docs/reviews/REASONINGSUBJECT_CONTRACT_DESIGN_STUDY.md`), none of**
  **which any future revision may weaken:**

  1. Behaviour-free — no operation beyond the ordinary structural
     operations (equality, textual representation, copying) any plain
     value already has; no operation that performs reasoning,
     invocation, translation, or any other domain act.
  2. Closed to exactly these two cases — `OfTurn`,
     `OfEvidenceAnalysisRequest` — never a third without a further
     amendment to this document.
  3. Owns no data beyond the one selected subject value — no field
     beyond the single wrapped value in either case.
  4. Introduces no new responsibility to `ReasoningProvider` itself —
     `ReasoningProvider.reason`'s own signature and pure-callee status
     (Section 7) are unchanged; this type only supplies the shape this
     field needs to compile a caller-agnostic request, since Kotlin has
     no union type.
  5. Grants no acceptance, persistence, retrieval, reasoning,
     confidence, evidential-state, provenance, ownership, or
     authorisation authority of its own — a pure selection mechanism;
     every one of those responsibilities remains exactly where existing
     governance already assigns it.
  6. Does not modify `Turn` or `EvidenceAnalysisRequest` — both remain
     reused, unmodified; this type only references them, never extends,
     subclasses, or amends either.
  7. Is not a generic, reusable union mechanism — closed to these two
     named, existing types specifically, never a type-parameterised
     abstraction usable for any other pair of types.
  8. Creates no independent dependency entitlement for any other
     subsystem. A `ReasoningProvider` implementation, or any
     composition-level caller, may inspect a value of this type solely
     to determine which existing subject it carries, in order to
     interpret that subject appropriately — that consumption grants no
     ownership, authority, extension right, or independent dependency
     entitlement over this type itself.

  **Explicit exclusions (Amendment 1, correction; restated from**
  **`docs/reviews/REASONINGSUBJECT_CONTRACT_DESIGN_STUDY.md` Section 5):**
  `ReasoningSubject` is **not a registry** — it holds no discovery or
  lookup mechanism of any kind and is not, and does not become,
  `ReasoningProviderRegistry`, already excluded at the Architecture
  tier. It is **not a router** — it does not decide which
  `ReasoningProvider` implementation handles a given case, and performs
  no dispatch of any kind. It is **not a provider selector** — it never
  chooses, ranks, or filters among multiple configured `ReasoningProvider`
  implementations; that question (Architecture Section 14 Item 3)
  remains open and unaffected. It is **not a workflow model** — it
  carries no state, no sequence, and no multi-step lifecycle of its
  own.

  Owned by this Contract Design, exactly as `ReasoningProviderRequest`
  itself already is — never by Conversation Engine, never by Evidence
  Intelligence, and never left ownerless.
- **`reasoningContext: ReasoningContext`** — a **new** type this
  document introduces (Section 10; not a reuse — see the Minimalism
  Review). The already-assembled working set
  `REASONING_PROVIDER_ARCHITECTURE.md` Section 2 and
  `reasoning-context.md` both require a Reasoning Provider to receive,
  but which no prior document has given a field-level shape. Given
  "Reasoning Context assembly" itself remains an unassigned
  responsibility (Architecture Section 14 Item 6, restated in Section 6,
  below), this document keeps `ReasoningContext` deliberately minimal
  and opaque: **an ordered list of already-assembled context entries,
  each a plain prose string, with no further internal structure
  imposed.** This satisfies this document's own instruction that the
  request and response objects "represent semantic meaning, not
  model-specific behaviour" — a list of prose strings carries meaning
  without assuming any model's tokenisation, context-window format, or
  prompt structure. How Memory and World Model excerpts become these
  entries — the actual assembly logic — remains entirely out of scope
  (Section 6, Section 9).

Nothing else enters through this request. In particular: no model
identifier, no prompt template, no token budget, no provider
configuration — all explicitly excluded (Section 9).

## 3. Minimal Response Object — `ReasoningProviderResponse`

The minimal output a Reasoning Provider returns, kept as a **sealed
type with exactly three variants**, restating
`REASONING_PROVIDER_ARCHITECTURE.md` Section 2's own three-way framing
("either a goal worth planning, a direct reply worth sending, or a
determination that neither is warranted") without adding a fourth:

- **`Goal(text: String)`** — a goal worth planning. `text` is
  non-blank prose, deliberately shaped to be directly usable as a
  future `PlanningRequest.goal: String` (`src/contracts/PlanDecision.kt`)
  once the calling component (never this contract) decides to submit
  one — restating `REASONING_PROVIDER_ARCHITECTURE.md` Section 9: "A
  Reasoning Provider never constructs a `PlanningRequest` itself."
- **`Reply(text: String)`** — a direct reply worth sending. `text` is
  non-blank prose, deliberately shaped to be directly usable as a future
  `OutboundParkerResponse.text: String`
  (`COMMUNICATION_CONTRACT_DESIGN.md` Section 3) once the calling
  component decides to route it there.
- **`NoAction`** — a determination that neither a goal nor a reply is
  warranted. Carries no fields; a bare marker, restating
  `REASONING_PROVIDER_ARCHITECTURE.md` Section 7 Step 3's third named
  outcome.

**No fourth variant for failure exists.** Restating Status and Section 9:
`REASONING_PROVIDER_ARCHITECTURE.md` does not define a failure model of
its own for the Reasoning Provider, and `19-conversation-engine.md`
Section 11 already places failure-surfacing responsibility on the
*calling* component ("the Conversation Engine's correct behaviour when
interpretation itself is unreliable is to make that visible") rather
than on the Reasoning Provider's own response shape. This document
therefore does not give a `Failed` domain variant a shape — not merely
because Stage 1 is silent, but because the one Stage 1 document that
does address this scenario assigns the responsibility elsewhere.

**This is a behaviour clarification, not a new domain response variant:
`ReasoningProvider.reason` may still fault for genuine failures.** A
domain-level `Failed` variant and an implementation-level fault are not
the same thing, and excluding the former does not exclude the latter.
Timeout, provider crash, malformed provider output, or an inability to
reason at all are implementation-level failures — a concrete
implementation may signal these by throwing, or by an equivalent
failure-signalling mechanism outside this sealed type; this document
does not specify which, consistent with Section 9's deferral of "failure
and error handling" as a mechanism question. **`NoAction` must never be
used as a catch-all for any of these.** `NoAction` means exactly one
thing: the Reasoning Provider reasoned successfully and confidently
determined that neither a goal nor a reply is warranted. It is a
semantic determination, not a shrug — an implementation that cannot
reach a confident determination has not produced `NoAction`, it has
failed, and must signal that failure by some means other than this
sealed type, not by returning `NoAction` in its place.

**Why a sealed type, not three separate boolean/nullable fields.**
Mirrors `CommunicationIntakeDisposition`'s own precedent
(`COMMUNICATION_CONTRACT_DESIGN.md` Section 6): exactly one of the three
outcomes is ever true for a given invocation, and a sealed type makes
that structurally impossible to violate, rather than merely
conventionally expected of three independent nullable fields.

## 4. Existing Parker Types Reused Unchanged

| Type | Reused via | Not redefined because |
| --- | --- | --- |
| `TurnId` | `Turn.turnId` (transitively) | Already field-shaped, `CONVERSATION_ENGINE_CONTRACT_DESIGN.md` Section 2. |
| `ConversationId` | `Turn.conversationId` (transitively) | Same. |
| `Turn` | `ReasoningProviderRequest.subject`, via `ReasoningSubject.OfTurn` (Amendment 1) | Same; embedding it whole avoids duplicating its own fields (Section 2). |
| `EvidenceAnalysisRequest` | `ReasoningProviderRequest.subject`, via `ReasoningSubject.OfEvidenceAnalysisRequest` (Amendment 1) | Already field-shaped, `EVIDENCE_INTELLIGENCE_CONTRACT_DESIGN.md` Section 4; owned by Evidence Intelligence, reused here unmodified (Section 2). |
| `InboundOwnerMessage` | `Turn.message` (transitively) | Already field-shaped, `COMMUNICATION_CONTRACT_DESIGN.md` Section 2. |
| `PrincipalId` | `Turn.message.senderPrincipalId` (transitively) | Already field-shaped, `src/contracts/Identifiers.kt`. |
| `CorrelationId` | `Turn.message.correlationId` (transitively) | Already field-shaped, `COMMUNICATION_CONTRACT_DESIGN.md` Section 4. |
| `PlanningRequest` | Referenced, not constructed — `Goal.text` is shaped to become a future `PlanningRequest.goal` | Not modified; this contract does not construct one (Section 3, Section 6). |
| `OutboundParkerResponse` | Referenced, not constructed — `Reply.text` is shaped to become a future `OutboundParkerResponse.text` | Not modified; this contract does not construct one (Section 3, Section 6). |

**Not reused, because no existing shape exists to reuse:**
`ReasoningContext`. Named in `reasoning-context.md` as a concept, never
before given a field-level shape by any document — this document
introduces its first, deliberately minimal one (Section 2, Section 10).

## 5. Ownership Boundaries

**Nothing this document defines is owned by the Reasoning Provider.**
Restating `REASONING_PROVIDER_ARCHITECTURE.md` Section 4 at the contract
level: `ReasoningProviderRequest` and `ReasoningProviderResponse` are
transient, per-invocation values — constructed by a caller, passed in,
returned, and not retained by the Reasoning Provider once the call
returns. Neither type has an identifier of its own kind (no
`ReasoningInvocationId`), because nothing about an invocation needs to be
referenced again after it concludes — restating the Minimalism Review's
own exclusion of a correlation field.

**`ReasoningContext`'s ownership is not assigned by this document.**
Restating Architecture Section 14 Item 6 exactly: who owns "Reasoning
Context assembly" is unassigned by every document before this one, and
remains unassigned here. This document defines only the minimal shape a
Reasoning Provider *receives* one in (Section 2); it does not claim
assembly, storage, or lifecycle ownership of `ReasoningContext` for the
Reasoning Provider or for any other component.

**The calling component owns the invocation itself.** Whichever
component invokes `ReasoningProvider.reason` (the Conversation Engine,
per Architecture Section 8) owns the decision to invoke, the resulting
`ReasoningProviderResponse`, and whatever it does with that response
next (Section 6). This document does not change
`CONVERSATION_ENGINE_CONTRACT_DESIGN.md`'s own ownership boundary in any
way.

## 6. Lifecycle Boundaries

At the contract level only — no algorithm, no storage decision,
restating `REASONING_PROVIDER_ARCHITECTURE.md` Section 7 now mapped onto
Section 1's one operation:

1. **Invocation.** A caller constructs a `ReasoningProviderRequest` (a
   `ReasoningSubject` — Amendment 1 — plus an already-assembled
   `ReasoningContext`) and calls `ReasoningProvider.reason`.
2. **Interpretation.** Entirely opaque to this contract — restating
   Architecture Section 7 Step 2 and Section 4: unconstrained by, and
   invisible to, anything this document defines.
3. **Return.** A `ReasoningProviderResponse` — exactly one of `Goal`,
   `Reply`, or `NoAction` — is returned to the caller. The invocation
   concludes here; nothing about it persists past this point (Section
   5).
4. **No lifecycle beyond one invocation.** This contract defines no
   session, no run, and no multi-invocation state of its own. Restating
   Architecture Section 7 Step 5: a Turn may involve zero, one, or many
   such invocations, but that sequencing is entirely the calling
   component's own responsibility (`CONVERSATION_ENGINE_CONTRACT_DESIGN.md`'s
   own deferred Turn-completion mechanism, Section 3 Item 4) — this
   contract's own lifecycle is exactly one invocation long, every time.

**No invocation, and no `ReasoningProviderResponse`, ever carries a
status implying authorisation.** Restating Architecture Section 7's
closing invariant: a `Goal` or a `Reply` is not, and never becomes, a
substitute for a Task Proposal, a Plan Decision, or a Permission
Decision.

## 7. Runtime Boundaries

**`ReasoningProvider.reason`'s signature depends on nothing but the four
types this document defines or reuses.** This is a structural guarantee,
not merely a stated rule: neither `ReasoningProviderRequest` nor
`ReasoningProviderResponse` references `PlannerRuntime`, `AgentRuntime`,
`TaskManagerRuntime`, `MemoryStore`, `WorldModel`, `ExecutionPipeline`,
`PermissionEngine`, `ToolRegistry`, or `ModuleRegistry`, anywhere, at any
depth. `Turn` and its own transitively-embedded types were already
verified free of any such dependency by
`CONVERSATION_ENGINE_CONTRACT_DESIGN.md` Section 7; `EvidenceAnalysisRequest`
and its own transitively-embedded types (Amendment 1) are equally free of
any such dependency, per `EVIDENCE_INTELLIGENCE_CONTRACT_DESIGN.md`
Section 12's own identical verification.

Restating `REASONING_PROVIDER_ARCHITECTURE.md` Section 6 and the task's
own explicit list, now made concrete against this contract:

- **Stateless from Parker's perspective.** No field on either request or
  response type, and no operation on `ReasoningProvider`, retains
  anything between calls (Section 5).
- **A pure callee.** `ReasoningProvider` has exactly one operation, and
  that operation calls nothing — it receives a request and returns a
  response. No dependency of any kind is declared on it.
- **Incapable of executing actions.** No dependency on
  `ExecutionPipeline` or `Tool` (Constitutional Boundaries).
- **Incapable of authorising actions.** No dependency on
  `PermissionEngine` (Constitutional Boundaries).
- **Incapable of invoking Planner Runtime.** No dependency on
  `PlannerRuntime`; `Goal.text` (Section 3) is returned to the caller,
  never submitted by this contract itself.
- **Incapable of invoking Tool Registry.** No dependency on
  `ToolRegistry` or `ToolInvocationBinding`.
- **Incapable of modifying Memory.** No dependency on `MemoryStore`.
- **Incapable of modifying the World Model.** No dependency on
  `WorldModel`.

**Not a seventh frozen peer.** Restating Architecture Section 6:
`ReasoningProvider` is not counted alongside
`ARCHITECTURE_V2_FROZEN_BASELINE.md` Section 2's six frozen runtime
subsystems. It is a role invoked from within a caller's own boundary, not
a runtime subsystem other components address directly.

## 8. Contract Invariants

Restating `REASONING_PROVIDER_ARCHITECTURE.md` Section 11's eight
architectural invariants, now made concrete against this contract's own
shapes:

- **A `ReasoningProviderResponse` proposes; it never authorises or
  executes.** Structurally guaranteed — no variant of the sealed type
  carries an `ExecutionRequest`, a `PermissionDecision`, or anything
  resembling one.
- **No Reasoning Provider implementation is load-bearing for this
  contract's own guarantees.** Every field and operation defined above
  is satisfiable by any implementation; nothing in this contract requires
  a specific model, vendor, or technique to be meaningful.
- **`ReasoningProviderRequest`/`ReasoningProviderResponse` are never
  retained by a `ReasoningProvider` implementation between calls.**
  Section 5, Section 6.
- **`ReasoningProvider` is never a seventh frozen peer.** Section 7.
- **A `Goal` or `Reply` is advisory until routed onward by the calling
  component through the full Cognition-proposes / Trust-authorises /
  Runtime-executes chain.** Section 6's closing statement.
- **`ReasoningProvider.reason` never invokes itself, another
  `ReasoningProvider`, or any Parker runtime subsystem.** Section 7;
  restating Architecture Section 5's identical rule.
- **No field on `ReasoningContext` or `ReasoningProviderResponse` writes
  to Memory or the World Model, directly or indirectly.** Section 2,
  Section 7 — structurally impossible, since neither type references
  `MemoryStore` or `WorldModel` at all.
- **Replacing a `ReasoningProvider` implementation must never alter what
  this contract requires or permits.** Constitutional Boundaries,
  restated throughout.

## 9. Deferred Items

Explicitly deferred, not partially designed — restating the task's own
list, each grounded in why:

- **Prompt templates.** Entirely a concrete implementation's own concern;
  `ReasoningProviderRequest`'s `ReasoningContext` (Section 2) is prose
  entries, not a template.
- **Token limits.** A model-specific constraint; this contract is
  model-independent by construction (Constitutional Boundaries) and
  imposes none.
- **Streaming.** Not decided; ties to Architecture Section 14 Item 1
  (invocation mechanism), still open. `ReasoningProvider.reason` as
  defined here returns one complete `ReasoningProviderResponse`, not a
  stream — whether a future revision adds a streaming variant is not
  decided here.
- **Provider selection.** Ties to Architecture Section 14 Item 3 and the
  excluded `ReasoningProviderRegistry` candidate (Minimalism Review); not
  decided here.
- **Provider routing.** Same as provider selection; this document defines
  what one `ReasoningProvider` looks like, not how a caller chooses among
  several.
- **Retries.** A caller-side or infrastructure-side concern; nothing in
  this contract's one-invocation lifecycle (Section 6) implies or
  requires retry semantics.
- **Network protocols.** Ties to Architecture Section 14 Item 1; whether
  an implementation is local or remote is invisible to this contract by
  design (Section 7).
- **Local vs. remote execution.** Same as network protocols — this
  contract's shape does not change based on where a concrete
  implementation runs.
- **Model identifiers.** Would violate this document's own
  model-independence requirement; no field anywhere in Section 2 or
  Section 3 names a model.
- **Provider implementations.** No concrete `ReasoningProvider`
  implementation, local or remote, rule-based or model-backed, is
  designed, sketched, or implied by this document.
- **A `Failed` domain variant.** Not merely deferred by choice — **not
  authorised, and reinforced elsewhere.** `REASONING_PROVIDER_ARCHITECTURE.md`
  defines no failure model of its own for the Reasoning Provider (Status,
  Review), and `19-conversation-engine.md` Section 11 already places
  failure-surfacing responsibility on the calling component, not the
  Reasoning Provider's own response shape. This document cannot invent a
  domain-level `Failed` variant without exceeding Stage 2A's own
  authority ("resolves named outstanding design questions against
  approved architecture, never inventing new architecture to answer
  them"). A future Architecture amendment — outside this document's own
  authority to make — is the correct place to resolve this if a
  dedicated `Failed`-shaped variant is ever needed beyond what Section 3
  already clarifies is available at the implementation level (faulting
  outside the sealed type). **This exclusion is not the same as leaving
  failure unrepresentable** — see Section 3's explicit clarification that
  `ReasoningProvider.reason` may still fault for genuine failures, and
  that `NoAction` must never be used as a substitute.
- **`ReasoningContext`'s own assembly mechanism.** How Memory and World
  Model excerpts become the prose entries `ReasoningContext` carries
  (Section 2) remains entirely unassigned — restating Architecture
  Section 14 Item 6, unresolved here.

## 10. Contract Minimalism Review

Every candidate type or field considered, with a reason for exclusion
where excluded — expanding the Summary above:

| Candidate | Included? | Reason |
| --- | --- | --- |
| `ReasoningProvider` (one-operation interface) | **Yes** | Minimum surface `19-conversation-engine.md` Section 13 Item 6 requires to exist. |
| `ReasoningProviderRequest` (`subject` + `reasoningContext`) | **Yes** | Minimum input Architecture Section 2 names: a reasoning subject's content and an assembled Reasoning Context. Two fields — `subject` amended by Amendment 1 to a closed sealed selector, `reasoningContext` unchanged. |
| `ReasoningSubject` (`OfTurn` / `OfEvidenceAnalysisRequest`) | **Yes, Amendment 1** | Generalises the single-caller `turn: Turn` field this document originally froze into a closed, two-case selector, reconciling this document's own field-level shape with `reasoning-context.md`'s constitutional-tier "task"-scoped generality (`docs/reviews/EVIDENCE_INTELLIGENCE_UNIT_3_CONSTITUTIONAL_DECISION_MEMORANDUM.md`). |
| `ReasoningProviderResponse` (sealed, `Goal`/`Reply`/`NoAction`) | **Yes** | Directly mirrors Architecture Section 2's own three-way framing; no fourth outcome is architecturally named. |
| `ReasoningContext` (opaque list of prose entries) | **Yes, minimally** | Required for `ReasoningProviderRequest` to be definable at all; kept to the absolute minimum shape that carries meaning without assuming model-specific structure (Section 2). |
| A `ReasoningProviderResponse.Failed` variant | **No** | Not authorised by Stage 1 Architecture, and reinforced rather than merely left open: `19-conversation-engine.md` Section 11 already assigns failure-surfacing to the calling component, not this response shape (Section 9). Implementation-level faulting remains available outside this sealed type (Section 3) — this exclusion is the largest in this review, but does not leave failure unrepresentable. |
| A confidence/score field on `Goal`/`Reply` | **No** | No concrete consumer; risks being mistaken for an authorisation signal (Constitutional Boundaries). |
| A correlation/invocation identifier | **No** | Nothing requires re-matching a response to a request across a gap in a single call/return shape; would anticipate an asynchronous mechanism Architecture Section 14 Item 1 has not decided exists. |
| A caller-principal field on the request | **No** | Ties to Architecture Section 14 Item 5, explicitly undecided there. |
| Structured fields within `ReasoningContext` (e.g. separate Memory/World-Model-sourced sub-lists) | **No** | Would require deciding "Reasoning Context assembly" ownership and shape (Architecture Section 14 Item 6), unassigned; a flat list of prose entries avoids that decision entirely while still being usable. |
| A `ReasoningProviderRuntime` wrapper interface | **No** | One interface suffices (Section 1); no session or multi-step lifecycle exists for it to wrap. |
| A `ReasoningProviderRegistry` | **No** | Already excluded at the architecture level (Architecture Section 13); no concrete need demonstrated here either. |

Net result, restated from the Summary: **five new contracts (Amendment 1
adds `ReasoningSubject`), six existing contracts reused unchanged (three
of them transitively via `Turn`; a fourth, `EvidenceAnalysisRequest`, via
`ReasoningSubject.OfEvidenceAnalysisRequest`, since Amendment 1), one
modified (`ReasoningProviderRequest`), one architectural gap disclosed
rather than papered over.**

## 11. Self-Traceability Review

| Contract | Traced to Reasoning Provider Architecture | Traced to Conversation Engine Architecture / Contract Design | Traced to Parker Constitution | Traced to PES-001 |
| --- | --- | --- | --- | --- |
| `ReasoningProvider` (interface) | Section 1 (Purpose), Section 6 (pure callee) | `19-conversation-engine.md` Section 13 Item 6 | "Cognition proposes" | Stage 2A: "states whether a separate 'Runtime' wrapper interface is needed" |
| `ReasoningProviderRequest` | Section 2 (Responsibilities: "given a Turn's content... and an already-assembled Reasoning Context") | `Turn` (`CONVERSATION_ENGINE_CONTRACT_DESIGN.md` Section 2), via `ReasoningSubject.OfTurn` since Amendment 1 | — | Stage 2A: "minimum required set of public contracts" |
| `ReasoningSubject` (Amendment 1) | Section 2 (Amendment 1: closed selector generalising the request's subject) | `Turn` (`CONVERSATION_ENGINE_CONTRACT_DESIGN.md` Section 2); `EvidenceAnalysisRequest` (`EVIDENCE_INTELLIGENCE_CONTRACT_DESIGN.md` Section 4) | — (traced instead to `reasoning-context.md`'s constitutional tier: "a task," not a Turn) | Stage 2A amendment: resolves the outstanding constitutional question `docs/reviews/EVIDENCE_INTELLIGENCE_UNIT_3_CONSTITUTIONAL_DECISION_MEMORANDUM.md` settles, against already-approved architecture |
| `ReasoningProviderResponse` (`Goal`/`Reply`/`NoAction`) | Section 2 ("either a goal worth planning, a direct reply worth sending, or a determination that neither is warranted") | `PlanningRequest.goal`, `OutboundParkerResponse.text` (both reused, unmodified) | "Reasoning providers may propose. They may not authorize or execute." | Stage 2A: field-level contract for an approved architecture's named responsibility |
| `ReasoningContext` | Section 10 ("Relationship to Memory, World Model, and Reasoning Context") | `reasoning-context.md` (concept origin, no prior shape) | — | Stage 2A: "explicitly stating what is required... and why" |
| Ownership boundaries (Section 5) | Section 4 (no persistent Parker-modelled state) | — | — | Stage 1's "ownership" requirement, made concrete |
| Runtime boundaries (Section 7) | Section 6 (Runtime Boundary), Section 3 (Non-Responsibilities) | `19-conversation-engine.md` Section 4 (not-a-seventh-peer precedent) | "No capability may bypass trust" | Stage 1's "runtime boundaries" requirement, made concrete |
| Contract invariants (Section 8) | Section 11 (all eight, verbatim) | — | Constitutional Tests, Replaceable Reasoning Providers | Stage 1's "invariants" requirement, made concrete |
| Deferred Items (Section 9) | Section 14 (all seven Open Questions) | `CONVERSATION_ENGINE_CONTRACT_DESIGN.md` Section 8 (deferred reasoning-provider contract, now partially resolved) | "Replaceable reasoning providers" | Stage 2A: "explicitly stating... what is deferred, and why" |

Every contract above traces to an authoritative source actually read
while drafting this document (Review, above). No architecture is
invented beyond what `REASONING_PROVIDER_ARCHITECTURE.md` already
authorises at the concept level; the one place this document could not
trace a design question back to Stage 1 (failure handling), it declined
to answer rather than inventing an answer (Section 9).

## Conclusion

**This document gives the Reasoning Provider a settled Stage 2A Contract
Design, as amended by Amendment 1 (ReasoningSubject): one public
interface (`ReasoningProvider`, one operation), five new field-level
types (`ReasoningProviderRequest` — amended, Amendment 1 —
`ReasoningProviderResponse` with its three variants, the deliberately
minimal `ReasoningContext`, and `ReasoningSubject`, Amendment 1's
closed, two-case selector), six existing contracts reused unchanged
(three of them transitively via `Turn`; a fourth,
`EvidenceAnalysisRequest`, via `ReasoningSubject.OfEvidenceAnalysisRequest`,
since Amendment 1), ownership, lifecycle, and runtime boundaries all
made concrete and, in the runtime case, structurally verifiable rather
than merely asserted, eight contract invariants, eleven deferred items
each with a stated reason, a Minimalism Review accounting for every
candidate this document's own brief named, and a Self-Traceability
Review connecting every element back to its authorising source.**

**One finding is surfaced, not silently resolved: `REASONING_PROVIDER_ARCHITECTURE.md`
has no failure model of its own for the Reasoning Provider, so this
document defines no `Failed` domain variant either — but this is
reinforced, not merely left open, by `19-conversation-engine.md` Section
11, which already assigns failure-surfacing to the calling component**
(Status, Section 3, Section 9, Section 10). This document also makes
explicit that excluding a domain-level `Failed` variant does not leave
genuine failure unrepresentable: `ReasoningProvider.reason` may still
fault outside the sealed response type, and `NoAction` must never be
used as a substitute for that (Section 3). A future Architecture
amendment to `REASONING_PROVIDER_ARCHITECTURE.md` itself, outside this
document's own authority — will be needed only if a dedicated
`Failed`-shaped domain variant is ever required beyond what Section 3
already clarifies is available at the implementation level.

Consistent with this unit's own scope, this document does not implement
anything, does not modify `REASONING_PROVIDER_ARCHITECTURE.md`,
`19-conversation-engine.md`, `CONVERSATION_ENGINE_CONTRACT_DESIGN.md`, or
any other existing document, and does not close
`IMPLEMENTATION_GAPS.md` #53. Once reviewed and accepted, this document
is the basis for a future Stage 3 Implementation Plan scoped to exactly
the surface defined above — no more, and not the failure-model gap this
document deliberately leaves for a future Architecture amendment.

## Related

- `docs/architecture/REASONING_PROVIDER_ARCHITECTURE.md`
- `docs/architecture/19-conversation-engine.md`
- `docs/architecture/CONVERSATION_ENGINE_CONTRACT_DESIGN.md`
- `docs/architecture/COMMUNICATION_CONTRACT_DESIGN.md`
- `docs/architecture/parker-constitution.md`
- `docs/architecture/PARKER_ENGINEERING_STANDARD.md`
- `docs/architecture/reasoning-context.md`
- `docs/architecture/ARCHITECTURE_V2_FROZEN_BASELINE.md`
- `docs/adr/ADR-001-models-never-execute-tools.md`
- `docs/adr/ADR-010-reasoning-engine.md`
- `src/interfaces/CommunicationIntake.kt`
- `src/contracts/PlanDecision.kt`
- `docs/architecture/IMPLEMENTATION_GAPS.md` (#53)
- `docs/decisions/CDR-007_CONSTITUTIONAL_CLASSIFICATION_OF_EVIDENCE_INTELLIGENCE.md` (Amendment 1)
- `docs/architecture/EVIDENCE_INTELLIGENCE_CONTRACT_DESIGN.md` (Amendment 1)
- `docs/architecture/EVIDENCE_INTELLIGENCE_SCOPE_LOCK.md` (Amendment 1)
- `docs/reviews/EVIDENCE_INTELLIGENCE_UNIT_3_CONSTITUTIONAL_DECISION_MEMORANDUM.md` (Amendment 1)
- `docs/reviews/REASONING_PROVIDER_CONTRACT_DESIGN_AMENDMENT_PROPOSAL.md` (Amendment 1)
- `docs/reviews/REASONINGSUBJECT_CONTRACT_DESIGN_STUDY.md` (Amendment 1)
- `src/interfaces/EvidenceIntelligence.kt` (Amendment 1)
