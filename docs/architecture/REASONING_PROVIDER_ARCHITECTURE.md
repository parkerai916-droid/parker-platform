# Reasoning Provider Architecture

## Status

**Stage 1 Architecture document, PES-001.** First real architectural
elaboration of two previously one-line ADRs: ADR-001 ("Language models
must never hold references to executable tool instances") and ADR-010
("Reasoning never executes actions directly"). Neither ADR has ever been
expanded beyond its own headline decision; this document does not revise
either, it gives the concept both already gesture at — a reasoning
provider — full Stage 1 treatment for the first time.

**Produced as Milestone 2 of the roadmap the most recent governance
review named.** That review found the missing reasoning-provider
contract to be the highest remaining architectural dependency within the
communication/conversation track: `docs/architecture/19-conversation-engine.md`
Section 13 Item 6 had already found no reasoning-provider Kotlin contract
exists anywhere in this repository (`ModelManager.kt` remains excluded
from the build — consistent with, not contrary to, ADR-022's own rule
that "initial Kotlin files under `src/interfaces` are interface stubs
only" and that "concrete implementation must not be added until runtime
architecture reaches the implementation phase"; no interface for a
reasoning provider exists in `src/interfaces`), and
`docs/architecture/CONVERSATION_ENGINE_CONTRACT_DESIGN.md`
Section 8 deferred "reasoning-provider contract" in full for exactly that
reason. This document is the Stage 1 prerequisite that dependency needs
before a future, separately-scoped Contract Design pass can give it a
Kotlin shape — it does not itself close that dependency, and does not
claim to.

**Terminology, reconciled with existing documents.** The Parker
Constitution and `docs/architecture/reasoning-context.md` both already
use "reasoning provider" as a concrete, swappable component name, while
"Cognition" names the constitutional *stage* that component performs
("Cognition proposes. Trust authorises. Runtime executes"): the
Constitution's own definition is "a reasoning provider — whichever model
or engine is configured — interprets a request and proposes an action, a
draft, or a plan," and its Replaceable Reasoning Providers principle
states "reasoning providers are interchangeable services that cognition
may use to generate proposals." Wherever earlier documents in this
program spoke of "Cognition" constructing a `PlanningRequest`
(`docs/architecture/COMMUNICATION_CONTRACT_DESIGN.md` Section 9) or
"Cognition" consuming an accepted `InboundOwnerMessage`
(`19-conversation-engine.md` Section 1, Section 7), they were referring,
at the concept level, to whatever component now receives its first real
architectural definition here. This document does not rename or replace
"Cognition" as the constitutional stage name; it names, for the first
time, the concrete, replaceable component that performs it.

## Review

Reviewed, in authority order:

1. `docs/architecture/parker-constitution.md` — "Cognition proposes.
   Trust authorises. Runtime executes"; "Replaceable reasoning
   providers"; the Constitutional Tests; "Reasoning providers may
   propose. They may not authorize or execute, and their replacement
   must never alter Parker's trust guarantees" (Architectural
   Responsibilities).
2. `docs/architecture/PARKER_ENGINEERING_STANDARD.md` (PES-001) — Stage
   1's seven required elements (responsibilities, ownership, trust
   boundaries, runtime boundaries, lifecycle, invariants, security
   model).
3. `docs/adr/ADR-001-models-never-execute-tools.md`,
   `docs/adr/ADR-010-reasoning-engine.md` — the two one-line ADRs this
   document elaborates without revising.
4. `docs/adr/ADR-002-memory-context-world-model-separation.md`,
   `docs/adr/ADR-013-agents-and-services-use-principal-identities.md`,
   `docs/adr/ADR-016-core-contracts.md`,
   `docs/adr/ADR-017-execution-request-is-canonical.md`,
   `docs/adr/ADR-022-kotlin-interface-stubs.md`,
   `docs/adr/ADR-023-context-provider-event-publication.md`,
   `docs/adr/ADR-024-module-event-audit-durability-boundary.md`.
5. `docs/architecture/reasoning-context.md` — the three-layer knowledge
   model (Memory, World Model, Reasoning Context) and its own
   information-flow chain naming "Reasoning Provider" explicitly.
6. `docs/architecture/ARCHITECTURE_V2_FROZEN_BASELINE.md` — Section 2's
   six frozen runtime subsystems this document builds on top of, not
   into.
7. `docs/architecture/19-conversation-engine.md` — Section 2
   (responsibilities engaging a reasoning provider), Section 8
   (relationship to Planner Runtime), Section 13 Item 6 (the exact
   dependency this document begins to unblock).
8. `docs/architecture/COMMUNICATION_CONTRACT_DESIGN.md` — Section 9's
   already-approved `PlanningRequest` construction pattern.
9. `docs/specifications/volume-06-planner-runtime/PlannerRuntimeSpecification.md`
   Section 4 — Goal, Plan Candidate, Planning Request, confirming Plan
   Candidate generation is the Planner Runtime's own responsibility, not
   an upstream input to it.
10. `docs/specifications/volume-04-agent-runtime/AgentRuntimeSpecification.md`
    Section 4 — "A Goal is an input to the Agent Runtime, not something
    the Agent Runtime produces... how a Goal is formed... is out of
    scope."

---

## 1. Purpose

A Reasoning Provider is the replaceable service that interprets
conversational context and produces reasoning output — nothing else.
Restating the Constitution directly: "Cognition proposes. Trust
authorises. Runtime executes." A Reasoning Provider is what performs the
proposing. It is the concrete thing "whichever model or engine is
configured" (Constitution) names, and the thing `reasoning-context.md`'s
own information-flow chain places between Reasoning Context and the
trust-authorising stage.

This document exists to answer the question `19-conversation-engine.md`
Section 13 Item 6 named and declined to answer without authority to do
so: what is a reasoning provider, architecturally, before any Contract
Design gives it a Kotlin shape? Concretely, it must fit cleanly between
the Conversation Engine (which invokes it) and Planner Runtime (which a
Reasoning Provider never calls directly — Section 6, Section 9), and it
must remain fully model-independent: nothing in this document, or in any
document it is grounded in, names a specific model, vendor, or reasoning
technique. "Reasoning Provider" names a role, not a product.

## 2. Responsibilities

- **Interpret conversational context.** Given a Turn's content —
  precisely, `Turn.message: InboundOwnerMessage`
  (`CONVERSATION_ENGINE_CONTRACT_DESIGN.md` Section 2), the Communication
  Message a Turn wraps, not the Turn itself, already accepted and bound
  by the Conversation Engine (`19-conversation-engine.md` Section 2) —
  and an already-assembled Reasoning Context (`reasoning-context.md`;
  Section 10, below), a Reasoning Provider determines what that Turn
  means.
- **Produce reasoning output.** The result of that interpretation —
  conceptually, either a goal worth planning, a direct reply worth
  sending, or a determination that neither is warranted — returned to
  whichever component invoked it. This document does not give "reasoning
  output" a Kotlin shape (Section 13, Open Questions); conceptually, it
  must be expressible as the existing `PlanningRequest.goal: String` or
  `OutboundParkerResponse.text: String` fields already accept, since
  neither of those reused contracts is modified by this document or any
  document it authorises.
- **Return its output to its caller, and nothing more.** A Reasoning
  Provider does not itself route, submit, or deliver anything (Section
  3). Its responsibility ends at producing reasoning output; what happens
  to that output is the calling component's decision, exactly as
  `19-conversation-engine.md` Section 2 and Section 8 already establish
  for the Conversation Engine's own role in routing a reasoning
  provider's resulting proposal onward.
- **Remain replaceable.** Restating the Constitution's own Replaceable
  Reasoning Providers principle directly: "Parker's authority, safety
  guarantees, and behavioral contracts do not depend on which provider is
  plugged in, and none of them may be weakened by a change of provider."
  A Reasoning Provider's responsibilities above must be satisfiable by
  any concrete implementation — a local model, a remote API, a
  rule-based system, or anything else — without any of them requiring
  privileged access unavailable to another.

## 3. Non-Responsibilities

Restating the task's own explicit boundary, each grounded in an existing
source rather than invented here:

- **Never executes actions.** Restating ADR-001 and ADR-010 directly, and
  the Constitution's "Reasoning providers may propose. They may not
  authorize or execute." A Reasoning Provider has no path to
  `ExecutionPipeline.submit`, `Tool.execute`, or any Tool invocation of
  any kind.
- **Never authorises actions.** Restating the Constitution's "Trust
  authorises" stage: only the Permission Engine, evaluating a proposal
  against owner-defined policy, produces an authorisation decision. A
  Reasoning Provider's output is never treated as, and never substitutes
  for, a `PermissionDecision`.
- **Never accesses Tools directly.** No dependency on `ToolRegistry`,
  `ToolInvocationBinding`, or any `Tool` implementation. Restating
  ADR-001 ("models must never hold references to executable tool
  instances") at the architecture level, and ADR-017's "no subsystem may
  invent a parallel execution request type": nothing in this document
  grants a Reasoning Provider a reference to an invocable Tool, under any
  circumstance.
- **Never bypasses Planner Runtime.** A Reasoning Provider does not call
  `PlannerRuntime.plan` itself, does not construct a `PlanningRequest`
  itself, and does not decompose a goal into Plan Candidates or Task
  Proposals — restating `PlannerRuntimeSpecification.md` Section 4's own
  ownership of Plan Candidate generation. When a Reasoning Provider's
  output implies a goal worth planning, submitting that goal as a
  `PlanningRequest` remains the calling component's decision and action
  (`19-conversation-engine.md` Section 8), never the Reasoning Provider's
  own.
- **Never bypasses the Permission Engine.** No dependency on
  `PermissionEngine.evaluate`, direct or indirect. Restating ADR-016's
  five-core-contracts model (Principal, Resource, Permission,
  `ExecutionRequest`, `ExecutionResult`) and ADR-017's "any proposed work
  that may cause execution, resource access, state mutation, or external
  side effects MUST become an `ExecutionRequest`": a Reasoning Provider's
  output reaches authorisation only by passing through whatever component
  already has the standing to request it — never by the Reasoning
  Provider requesting it itself, and never by any path that isn't an
  ordinary `ExecutionRequest`.
- **Never modifies Memory or the World Model directly.** Restating
  `reasoning-context.md` verbatim: "Nothing in this flow allows a
  reasoning provider to write back into Memory or the World Model
  directly; any such change is a separate, governed act, not a side
  effect of reasoning." A Reasoning Provider has no dependency on
  `MemoryStore` or `WorldModel` (Section 6, Section 10).
- **Not a seventh peer runtime subsystem.** Restating the same discipline
  already applied to Parker Runtime intake
  (`COMMUNICATION_CHANNEL_ARCHITECTURE.md` Section 4) and the
  Conversation Engine (`19-conversation-engine.md` Section 4): a
  Reasoning Provider is not counted alongside
  `ARCHITECTURE_V2_FROZEN_BASELINE.md` Section 2's six frozen runtime
  subsystems (Identity Runtime, EventBus, Planner Runtime, Agent Runtime,
  Memory Runtime, World Model Runtime). It is a role a caller invokes,
  not a peer of the runtimes that decide, track, or execute.
- **Not Reasoning Context assembly.** `reasoning-context.md` names
  "Reasoning Context assembly" as its own responsibility, combining
  Memory and World Model into a bounded, task-scoped working set. A
  Reasoning Provider consumes an already-assembled Reasoning Context
  (Section 10); it does not assemble one, and this document does not
  assign that responsibility to it.
- **Not the Conversation Engine.** A Reasoning Provider does not bind
  Turns to Conversations, does not maintain continuity state, and owns no
  part of `19-conversation-engine.md` Section 4's Conversation state. It
  is invoked by the Conversation Engine (or a future caller with
  equivalent standing); it is never the thing doing the invoking of
  itself.

## 4. Ownership

**A Reasoning Provider owns no persistent Parker-modelled state.** This
is a deliberate, notable departure from every other component this
program has architected so far — the Conversation Engine owns
Conversation/Turn continuity state, the Task Manager Runtime owns Task
state, the Agent Runtime owns Agent Run state, Memory Runtime owns
durable knowledge, World Model Runtime owns current belief. A Reasoning
Provider owns none of these, and none of its own kind: it does not
retain, between invocations, any record this document defines or any
other Parker contract already defines.

**A Reasoning Provider's own internal state is out of Parker's ownership
model entirely, and out of this document's scope.** A concrete
implementation may hold model weights, a context window, session
tokens, or other internal state needed to function — none of that is a
Parker-owned contract, none of it is visible to or governed by this
document, and none of it may become load-bearing for anything this
document defines (restating the Replaceable Reasoning Providers
principle: Parker's guarantees cannot depend on what is opaque inside a
specific provider).

**It owns no identity of its own kind, but must resolve to one.**
Restating ADR-013 ("internal actors must have explicit principal
identities... accountability requires attribution"): whichever concrete
Reasoning Provider is configured must be attributable to a resolvable
Principal for any action taken on its behalf to be attributable — but
this document does not decide whether that is the calling component's
own identity (the Conversation Engine's operating Principal) or a
distinct identity per provider (Section 13, Open Questions).

## 5. Trust Boundary

**A Reasoning Provider proposes. It authorises nothing, and its proposal
carries no authority of its own.** Restating the Constitution's
Architectural Responsibilities directly: "Reasoning providers may
propose. They may not authorize or execute, and their replacement must
never alter Parker's trust guarantees."

- **No standing capability is granted by being invoked.** A Reasoning
  Provider being asked to interpret a Turn is not itself a trust
  decision, and confers no elevated standing on its output — restating
  `19-conversation-engine.md` Section 12's identical rule for a
  Conversation's mere existence.
- **No confidence score, however high, substitutes for Permission Engine
  evaluation.** A Reasoning Provider may internally have a notion of how
  confident its interpretation is; nothing about that confidence changes
  which authorisation path its resulting proposal must still travel
  through. There is no "fast path" this document authorises for
  high-confidence output.
- **Replacement must never alter Parker's trust guarantees.** Restating
  the Constitution verbatim. Swapping which concrete Reasoning Provider
  is configured — a different model, a different vendor, a local
  implementation for a remote one — must never change what a proposal is
  capable of, or what authorisation it still requires, by construction of
  this architecture, not by policy about any specific provider.
- **A Reasoning Provider never invokes itself, another reasoning
  provider, or any Parker runtime subsystem autonomously.** Any
  sequencing of multiple reasoning-provider invocations within one Turn —
  already anticipated by `19-conversation-engine.md` Section 2's "a
  sequence of providers, or a provider invoked again after an
  intermediate tool result" — remains the calling component's
  responsibility, never the Reasoning Provider's own. A Reasoning
  Provider that received an invocation and returned reasoning output has
  concluded its involvement in that invocation; it does not decide,
  autonomously, to act further.

## 6. Runtime Boundary

A Reasoning Provider sits between the Conversation Engine and Planner
Runtime — invoked by the former, never itself calling the latter:

- **Conversation Engine → Reasoning Provider.** The Conversation Engine
  (or a future caller with equivalent standing) invokes a Reasoning
  Provider with a Turn's content and an assembled Reasoning Context, and
  receives reasoning output in return (Section 2, Section 8).
- **Reasoning Provider → its caller, only.** A Reasoning Provider's only
  outward-facing relationship is returning its output to whoever invoked
  it. It has no dependency on, and never calls, `PlannerRuntime`,
  `AgentRuntime`, `TaskManagerRuntime`, `MemoryStore`, `WorldModel`,
  `ExecutionPipeline`, `PermissionEngine`, `ToolRegistry`, or
  `ModuleRegistry` (Section 3).
- **Never a seventh frozen peer.** Restating Section 3: not counted
  alongside Identity Runtime, EventBus, Planner Runtime, Agent Runtime,
  Memory Runtime, or World Model Runtime. It is a role invoked from
  within the Conversation Engine's own boundary (or a future equivalent
  caller's), not a runtime subsystem other components address directly.
- **No dependency in the reverse direction.** No frozen runtime
  subsystem, and no already-approved component in this program, calls
  into a Reasoning Provider. It is only ever a callee, never a caller of
  anything Parker already governs.

## 7. Lifecycle

At the architecture level only — no Kotlin, no invocation protocol, no
algorithm:

1. **Invocation.** A caller with standing to do so (the Conversation
   Engine, per `19-conversation-engine.md` Section 2) determines a Turn
   requires interpretation and invokes a Reasoning Provider, supplying
   the Turn's content and an already-assembled Reasoning Context.
2. **Interpretation.** The Reasoning Provider reasons over its input by
   whatever internal means the concrete implementation uses — entirely
   opaque to, and unconstrained by, this document (Section 4).
3. **Reasoning output produced.** The Reasoning Provider produces its
   result: a goal worth planning, a direct reply, or a determination that
   neither is warranted.
4. **Return.** The reasoning output is returned to the caller. The
   Reasoning Provider's involvement in this invocation concludes here —
   restating Section 5, it does not act further, and does not itself
   decide what happens to its own output next.
5. **Zero, one, or many invocations per Turn.** Restating
   `19-conversation-engine.md` Section 2 directly: a single Turn may
   involve no Reasoning Provider invocation (if none is needed), one, or
   a sequence — including one invoked again after an intermediate result
   — entirely at the calling component's discretion, never the Reasoning
   Provider's own. **This is safe precisely because of Section 4's
   no-retained-state rule:** since a Reasoning Provider "does not retain,
   between invocations, any record this document defines," any
   continuity a chained sequence needs — the prior step's output, an
   intermediate result — is supplied by the caller re-presenting it as
   input on the next invocation, never recovered from something the
   Reasoning Provider itself remembered.

No invocation, and no reasoning output, ever carries a status implying
authorisation — restating `19-conversation-engine.md` Section 6's
identical closing invariant for Conversations and Turns: reasoning output
is not, and never becomes, a substitute for a Task Proposal, a Plan
Decision, or a Permission Decision.

## 8. Relationship to the Conversation Engine

The Conversation Engine is a Reasoning Provider's primary, architecturally
named caller. Restating `19-conversation-engine.md` Section 2 ("Engage
one or more reasoning providers during the processing of a Turn") and
Section 13 Item 6 (the relationship's exact call shape "cannot be fully
resolved even at Contract Design level until a reasoning-provider/
Cognition Kotlin contract itself exists"): this document is that
contract's architectural predecessor, not its Contract Design. It
confirms the relationship's *shape* — Conversation Engine invokes,
Reasoning Provider returns output, Conversation Engine routes the result
— without yet defining the Kotlin interface either side would use
(Section 13, Open Questions).

The Conversation Engine's own Contract Design
(`CONVERSATION_ENGINE_CONTRACT_DESIGN.md` Section 8) deferred "reasoning-
provider contract" in full, and deferred `TurnStatus`/`ConversationStatus`
and a `completeTurn` operation specifically because Turn/Conversation
completion depends on knowing when a Reasoning Provider's engagement with
a Turn concludes (`CONVERSATION_ENGINE_CONTRACT_DESIGN.md` Section 3, Item
4). This document's Section 7 (Lifecycle) now supplies that missing
concept — a Reasoning Provider's involvement concludes at Section 7 Step
4 (Return) — without itself defining the Kotlin field or operation that
would record it; that remains a future, separately-scoped Contract Design
pass for both components together.

## 9. Relationship to Planner Runtime

**A Reasoning Provider has no direct relationship to Planner Runtime at
all.** This is deliberate, not an oversight (Section 3, Section 6).
Restating `19-conversation-engine.md` Section 8 and
`COMMUNICATION_CONTRACT_DESIGN.md` Section 9's already-approved pattern:
when a Reasoning Provider's output implies a goal worth planning, it is
the *calling* component — the Conversation Engine — that constructs an
ordinary `PlanningRequest` (`initiatingPrincipalId` = the Turn's own
owner, `correlationId` = the message's own correlation value, `source` =
`RequestOrigin.TEXT`) and submits it to `PlannerRuntime.plan`, exactly as
any other caller already does. `PlanningRequest.goal: String` is where a
Reasoning Provider's output ultimately lands — but only after passing
through the calling component's own decision to submit it, never
directly.

**Plan Candidate generation remains entirely Planner Runtime's own
responsibility.** `PlannerRuntimeSpecification.md` Section 4 defines Plan
Candidate as something Planner Runtime produces in response to a
`PlanningRequest`, not something supplied to it. A Reasoning Provider
never produces a Plan Candidate, a Task Proposal, or anything
Planner-Runtime-shaped — its output is bounded at "a goal, in prose,"
restating Section 2.

## 10. Relationship to Memory, World Model, and Reasoning Context

**A Reasoning Provider consumes an already-assembled Reasoning Context.
It does not assemble one, and it never touches Memory or the World Model
directly.** Restating `reasoning-context.md` verbatim: "Nothing in this
flow allows a reasoning provider to write back into Memory or the World
Model directly; any such change is a separate, governed act, not a side
effect of reasoning."

**A note on `reasoning-context.md`'s own diagram, reconciled rather than
silently followed.** That document's information-flow chain reads
`Reasoning Context -> Reasoning Provider -> Trust Engine -> Execution
Pipeline`, without a separate node for Planner Runtime, Task Manager
Runtime, or Agent Runtime. This document does not treat that as a literal
claim that a Reasoning Provider's output skips directly from proposal to
execution. Read alongside `19-conversation-engine.md` Section 8's more
detailed path — Conversation Engine submits a `PlanningRequest`, Planner
Runtime evaluates it, an accepted Task Proposal may acquire Agent Run
References, only an Agent Run's own `ExecutionRequest` ever reaches the
Execution Pipeline — `reasoning-context.md`'s "Trust Engine" is read here
as naming the authorisation *principle* (Permission Engine evaluation
occurs somewhere before execution), not asserting adjacency. This
document does not resolve which of the two framings is more precise; it
notes the difference so a future reader does not mistake one document's
simplification for a contradiction of the other's detail.

**Who assembles Reasoning Context is not decided by this document.**
`reasoning-context.md` names "Reasoning Context assembly" as its own
responsibility without assigning it to a named component, and
`19-conversation-engine.md` Section 9 already declined to claim it for
the Conversation Engine ("The Conversation Engine does not assemble
Reasoning Context... at most, supplies one input to that assembly").
This document does not assign it to the Reasoning Provider either
(Section 3) — it remains unassigned, named as an Open Question (Section
14), not invented here.

## 11. Architectural Invariants

- **A Reasoning Provider proposes; it never authorises or executes.**
  Constitution, restated throughout this document.
- **No reasoning provider is load-bearing for Parker's trust model.**
  Constitution, verbatim, restated in Section 5.
- **A Reasoning Provider owns no persistent Parker-modelled state.**
  Section 4.
- **A Reasoning Provider is never a seventh frozen peer runtime
  subsystem.** Section 3, Section 6.
- **A Reasoning Provider's output is advisory until it passes through
  the full Cognition-proposes / Trust-authorises / Runtime-executes
  chain.** No invocation, no reasoning output, and no invocation history
  ever substitutes for a Task Proposal, a Plan Decision, or a Permission
  Decision (Section 7).
- **A Reasoning Provider never invokes itself, another reasoning
  provider, or any Parker runtime subsystem autonomously.** Section 5.
- **A Reasoning Provider never writes to Memory or the World Model,
  directly or indirectly.** Section 10; any resulting write happens only
  through their own existing governed interfaces, called by whichever
  component already has the authority — restating
  `COMMUNICATION_CONTRACT_DESIGN.md` Section 9's identical rule,
  generalised here.
- **Replacing a Reasoning Provider must never alter Parker's trust
  guarantees.** Constitution, verbatim, restated in Section 2 and
  Section 5.

## 12. Security Model

Restating the Constitution's Constitutional Tests, applied specifically
to a Reasoning Provider:

- **Owner control is preserved.** Every action a Reasoning Provider's
  output eventually leads to still passes through the same Permission
  Engine evaluation and remains as visible and revocable as any other
  authorised action — a Reasoning Provider introduces no separate,
  less-visible path (Section 5).
- **Authority and capability remain separate.** A Reasoning Provider
  supplies interpretation (a capability); it never supplies, and never
  approximates, authorisation (Section 5).
- **No bypass of trust authorisation exists.** Section 3, Section 6 name
  every path a Reasoning Provider explicitly does not have.
- **Blast radius is bounded by whatever the calling component does with
  its output, never by the Reasoning Provider itself.** Because a
  Reasoning Provider cannot submit, execute, or authorise anything
  directly (Section 3), a fully compromised Reasoning Provider's
  worst-case behaviour is limited to producing misleading reasoning
  output — which still must pass through Planner Runtime, the Permission
  Engine, and the Execution Pipeline exactly like any other proposal,
  restating the Constitutional Tests' seventh question directly.
- **Conversational content reaching a Reasoning Provider is owner data,
  and carries the same sensitivity discipline as anywhere else in this
  platform.** Restating `19-conversation-engine.md` Section 12: a Turn's
  raw text may contain `ResourceSensitivity.PERSONAL`, `FINANCIAL`,
  `MEDICAL`, or more sensitive content. This document does not invent a
  new classification scheme, and does not decide how a specific Reasoning
  Provider implementation — particularly one hosted outside the owner's
  own infrastructure — handles, retains, or transmits that content once
  invoked. That question is named, not resolved, in Section 14.

## 13. Architectural Minimalism Review

Candidate responsibilities and components considered for this
architecture, and why each is excluded:

| Candidate | Included? | Reason |
| --- | --- | --- |
| A single "Reasoning Provider" role, invoked by a caller, producing reasoning output only | **Yes** | The minimum concept `19-conversation-engine.md` Section 13 Item 6 and `CONVERSATION_ENGINE_CONTRACT_DESIGN.md` Section 8 both require to exist before either can be extended. |
| A "Reasoning Provider Registry" (mirroring `ToolRegistry`/`ModuleRegistry`) | **No** | No concrete need for multiple simultaneously-selectable providers has been demonstrated; if one arises, a registry can be added additively, mirroring this program's own "100,000-line test" discipline already applied elsewhere (`COMMUNICATION_CONTRACT_DESIGN.md`'s channel-kind enumeration exclusion). |
| A dedicated "Reasoning Context Assembler" responsibility, owned by the Reasoning Provider | **No** | Already named by `reasoning-context.md` as its own, distinct responsibility, not assigned to any component including the Conversation Engine; this document does not claim it either (Section 10, Section 14). |
| A `reasoning.*` `EventBus` namespace | **No, deferred** | Mirrors `19-conversation-engine.md` Section 10's identical treatment of `conversation.*`: authorised in principle should a concrete observability need arise, not mandated, and no operation this document defines publishes anything. |
| A direct Tool-access shortcut for latency-sensitive reasoning | **No** | Flatly prohibited by ADR-001 and Section 3; no latency argument overrides a constitutional invariant. |
| A caching or memoisation layer owned by the Reasoning Provider | **No** | Would constitute retained state this document's Ownership section (Section 4) explicitly declines to grant; if caching reasoning output is ever architecturally justified, it belongs to Memory Runtime's own existing, governed promotion path, not a new Reasoning-Provider-owned mechanism. |
| Multiple reasoning providers collaborating as a named "ensemble" concept | **No, deferred** | `19-conversation-engine.md` Section 2's "one or more reasoning providers... a sequence of providers" already permits sequential invocation without this document inventing collaborative/ensemble semantics; no concrete need for the latter has been demonstrated. |
| A confidence-based fast path around Permission Engine evaluation | **No** | Flatly prohibited by Section 5 and the Constitutional Tests' third question; no confidence score, however produced, substitutes for authorisation. |
| A dedicated Reasoning Provider Principal identity type | **No** | ADR-013 requires attributability, not a new identifier type; `PrincipalId`/`IdentityService` (already existing) are reused, exactly as every other component in this program reuses them. Whether a Reasoning Provider needs a *distinct* resolved identity from its caller's is named, not decided, in Section 14. |

Net result: **one architectural role defined** (Reasoning Provider), **zero
new runtime subsystems**, **zero new persistent state**, **every excluded
candidate traced to either an existing, already-governed mechanism or a
concrete-need-not-yet-demonstrated deferral.**

## 14. Open Questions

Named rather than invented, per this program's own established
discipline:

1. **The exact invocation mechanism** — synchronous call/response,
   asynchronous request with callback, or something else — between a
   caller (the Conversation Engine) and a Reasoning Provider. Contract
   Design's job.
2. **The exact shape of "reasoning output"** — a bare string, a small
   structured type distinguishing "goal" from "reply" from "no action,"
   or something else — bounded by this document to be expressible
   through `PlanningRequest.goal: String` and `OutboundParkerResponse.text:
   String` without modifying either, but not more precisely specified
   here. Contract Design's job.
3. **How a specific Reasoning Provider is selected or configured** — a
   single default, multiple selectable providers, or per-Conversation
   configuration. Not decided here; ties to the excluded "Reasoning
   Provider Registry" candidate (Section 13), which remains excluded
   until a concrete need is demonstrated.
4. **Data handling, retention, and egress for remote or cloud-hosted
   Reasoning Providers.** Section 12 names this as a real, unresolved
   security question: "model-independent" implies some configured
   providers may be hosted outside the owner's own infrastructure, and
   this document does not decide what sensitivity-classified content
   (Section 12) may or may not be sent to one, or under what policy. This
   is flagged as likely requiring its own ADR, parallel to how Gap #51's
   persistence/durability boundary question was flagged rather than
   resolved in-line.
5. **Whether a Reasoning Provider needs a resolved Principal identity
   distinct from its caller's own**, for accountability when multiple
   providers are configured or swapped (Section 4, Section 13). Not
   decided here.
6. **Who owns "Reasoning Context assembly."** Named by `reasoning-context.md`
   as a responsibility, not assigned to any component by that document,
   `19-conversation-engine.md`, or this document. Remains unassigned.
7. **Whether `reasoning-context.md`'s own "Reasoning Provider -> Trust
   Engine" diagram should eventually be revised to show the fuller path**
   (Planner Runtime, Task Manager Runtime, Agent Runtime) this document's
   Section 10 reconciles rather than resolves. Not decided here — this
   document does not modify `reasoning-context.md`.

## 15. Self-Traceability Review

| Element | Where satisfied | Traced to |
| --- | --- | --- |
| Responsibilities | Section 2 | Constitution ("Cognition proposes"); `reasoning-context.md` (Reasoning Provider's role in the information-flow chain) |
| Non-responsibilities | Section 3 | ADR-001, ADR-010, Constitution ("may not authorize or execute"), `PlannerRuntimeSpecification.md` (Plan Candidate ownership), `reasoning-context.md` (no write-back to Memory/World Model) |
| Ownership | Section 4 | ADR-013 (attributable identity); this program's own precedent of naming what a component does and does not own (`19-conversation-engine.md` Section 4) |
| Trust boundary | Section 5 | Constitution (Architectural Responsibilities, Replaceable Reasoning Providers, Constitutional Tests) |
| Runtime boundary | Section 6 | `ARCHITECTURE_V2_FROZEN_BASELINE.md` Section 2 (six frozen subsystems); `19-conversation-engine.md` Section 4 (not-a-seventh-peer precedent) |
| Lifecycle | Section 7 | `19-conversation-engine.md` Section 2 (multiple invocations per Turn), Section 6 (no status implies authorisation) |
| Architectural invariants | Section 11 | Constitution (verbatim quotations throughout) |
| Security model | Section 12 | Constitution (Constitutional Tests); `19-conversation-engine.md` Section 12 (Resource sensitivity discipline) |
| Relationship to Conversation Engine | Section 8 | `19-conversation-engine.md` Section 2, Section 13 Item 6; `CONVERSATION_ENGINE_CONTRACT_DESIGN.md` Section 8 |
| Relationship to Planner Runtime | Section 9 | `19-conversation-engine.md` Section 8; `COMMUNICATION_CONTRACT_DESIGN.md` Section 9; `PlannerRuntimeSpecification.md` Section 4 |
| Relationship to Memory/World Model/Reasoning Context | Section 10 | `reasoning-context.md` (verbatim quotation, information-flow chain) |
| Model-independence | Section 1, Section 4 | Constitution's Replaceable Reasoning Providers principle, verbatim |

Every claim in this document traces to an authoritative source actually
read while drafting it (Review, above). No architecture is invented
beyond what these sources already anticipate at the concept level; where
a genuine tension exists between two sources (Section 10's diagram
reconciliation), it is disclosed rather than silently resolved in either
direction.

## Conclusion

**This document gives the Reasoning Provider a settled Stage 1
architecture: purpose, responsibilities, non-responsibilities, ownership
(deliberately minimal — no persistent Parker-modelled state), trust
boundary, runtime boundary, a conceptual lifecycle, its relationship to
the Conversation Engine and to Planner Runtime, its relationship to
Memory/World Model/Reasoning Context (including a disclosed reconciliation
of `reasoning-context.md`'s own simplified diagram), eight architectural
invariants, a security model, an Architectural Minimalism Review
accounting for nine candidate concepts, seven explicitly named open
questions, and a Self-Traceability Review connecting every element back to
its authorising source.**

Consistent with this unit's own scope, this document does not implement
anything, does not modify `19-conversation-engine.md`,
`CONVERSATION_ENGINE_CONTRACT_DESIGN.md`, `reasoning-context.md`, ADR-001,
ADR-010, or any other existing document, and does not close
`IMPLEMENTATION_GAPS.md` #53 — it narrows one dependency named in Section
13 Item 6 of Chapter 19 and Section 8 of the Conversation Engine Contract
Design, and no more. Once reviewed and accepted, this document is the
basis for a future Stage 2A Contract Design pass — Milestone 3 of the
governance review's own recommended roadmap — scoped to exactly the
mechanism questions Section 14 names as ready to resolve.

## Related

- `docs/architecture/parker-constitution.md`
- `docs/architecture/PARKER_ENGINEERING_STANDARD.md`
- `docs/adr/ADR-001-models-never-execute-tools.md`
- `docs/adr/ADR-010-reasoning-engine.md`
- `docs/adr/ADR-002-memory-context-world-model-separation.md`
- `docs/adr/ADR-013-agents-and-services-use-principal-identities.md`
- `docs/adr/ADR-023-context-provider-event-publication.md`
- `docs/adr/ADR-024-module-event-audit-durability-boundary.md`
- `docs/architecture/reasoning-context.md`
- `docs/architecture/ARCHITECTURE_V2_FROZEN_BASELINE.md`
- `docs/architecture/19-conversation-engine.md`
- `docs/architecture/CONVERSATION_ENGINE_CONTRACT_DESIGN.md`
- `docs/architecture/COMMUNICATION_CONTRACT_DESIGN.md`
- `docs/specifications/volume-06-planner-runtime/PlannerRuntimeSpecification.md`
- `docs/specifications/volume-04-agent-runtime/AgentRuntimeSpecification.md`
- `docs/architecture/IMPLEMENTATION_GAPS.md` (#53)
