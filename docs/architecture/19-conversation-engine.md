# Chapter 19 — Conversation Engine

## Status

**Stage 1 Architecture document, PES-001.** Replaces the three-line
conceptual stub this chapter previously held. Produced as Milestone 1 of
the roadmap `IMPLEMENTATION_GAPS.md` #53 itself names: that gap's own
"Update (planning pass)" text records that closing path (b) — defining
the concrete mechanism by which Cognition consumes an accepted
`InboundOwnerMessage` — "requires Cognition/Conversation Engine to first
receive real Stage 1 Architecture," and that `19-conversation-engine.md`
was, at the time that note was written, "a three-line stub with no
responsibilities, ownership, trust boundary, lifecycle, invariants, or
security model." This document supplies that missing content.

`COMMUNICATION_CHANNEL_ARCHITECTURE.md` Section 8 (Out of Scope) already
named this chapter as relevant, without designing it: "Conversation
memory design — turn-taking, continuity, clarification, or follow-up
handling (Chapter 19, Conversation Engine) remains a conceptual stub
this document does not flesh out; it is named in Section 4 only as the
eventual downstream consumer of Parker Runtime intake, not designed
here." Section 4 of that same document left one question explicitly
open for whichever document reached this point first: whether Parker
Runtime intake's downstream target is "Cognition directly," or "a
future Conversation Engine sitting in front of Planner Runtime." This
document settles that question, at the architecture level, and only at
the architecture level: **a Conversation Engine exists as a distinct
architectural component**, described in full below. It does not define
any Kotlin interface, class name, package layout, or implementation —
see Section 13 (Open Questions and Deferred Items) and Section 14
(Implementation Sequencing).

This document does not close `IMPLEMENTATION_GAPS.md` #53. It is the
Stage 1 prerequisite a future, separately-scoped Contract Design pass
would need before attempting to close that gap's closure path (b); see
Section 14.

---

## 1. Purpose

The Conversation Engine is the component responsible for turning a
sequence of individual inbound and outbound Communication Messages —
each one structurally valid and already accepted or produced by the
mechanisms `COMMUNICATION_CONTRACT_DESIGN.md` and
`LOCAL_TEXT_CHANNEL_CONTRACT_DESIGN.md` already define — into something
that feels like a continuous exchange rather than a series of unrelated
commands. This restates, and gives full architectural weight to, this
chapter's own original one-line mandate: "Conversation should feel
continuous rather than command-driven."

Concretely, the Conversation Engine exists to answer a question no
existing frozen subsystem answers: once `CommunicationIntake` has
accepted an `InboundOwnerMessage` (Sprint 7, Unit C1), *what receives it
next, and how does that thing know whether this message is a new
request or a continuation of one already in progress?* Neither
`PlannerRuntime.plan` (which expects an already-formed `Goal`) nor
`ExecutionPipeline.submit` (which expects an already-decided
`ExecutionRequest`) can answer this — both already assume the
interpretive and continuity work this chapter defines has already
happened (`COMMUNICATION_CHANNEL_ARCHITECTURE.md` Section 4's own
reasoning for why a reception boundary was unavoidable applies with
equal force one layer downstream, to interpretation and continuity).

The Conversation Engine is not itself the answer to "what does this
message mean" — that remains a reasoning provider's job, exactly as
`reasoning-context.md` and the Constitution already establish ("Cognition
proposes"). The Conversation Engine is the structural, deterministic
layer around that reasoning: it receives accepted messages, maintains
enough continuity that a reasoning provider does not need to
reconstruct an entire exchange from nothing on every turn, and routes
whatever a reasoning provider proposes onward through Parker's existing,
already-governed paths — never inventing a new one.

## 2. Responsibilities

- **Receive accepted `InboundOwnerMessage` instances downstream of
  `CommunicationIntake`.** The Conversation Engine is the "eventual
  downstream consumer" `COMMUNICATION_CHANNEL_ARCHITECTURE.md` Section 8
  already named. It does not perform, repeat, or second-guess
  `CommunicationIntake`'s own two structural checks (channel `ENABLED`,
  sender resolves) — by the time a message reaches this layer, both are
  already satisfied.
- **Maintain conversational continuity across turns.** A **Conversation**
  (Section 6) is the bounded concept that groups related **Turns** — each
  Turn being one inbound message and, where produced, its corresponding
  outbound response — so that a reasoning provider engaged for a later
  Turn can be given the relevant continuity from earlier ones, per this
  chapter's own original mandate: turn-taking, interruptions,
  clarifications, follow-up questions, continuity.
- **Correlate messages belonging to the same exchange.** Using
  `CorrelationId` (`COMMUNICATION_CONTRACT_DESIGN.md` Section 4) as the
  mechanism already established for tying an inbound message to whatever
  eventually answers it, extended here to also let the Conversation
  Engine recognise when a new inbound message continues an existing
  Conversation rather than starting a new one. The exact recognition
  rule is not decided here (Section 13).
- **Engage one or more reasoning providers during the processing of a
  Turn.** Restating `reasoning-context.md`'s own chain (Reasoning
  Context -> Reasoning Provider -> Trust Engine -> Execution Pipeline):
  the Conversation Engine is a plausible supplier of "the specifics of
  the current task" that chain already names as one input to Reasoning
  Context assembly (Section 9) — specifically, the current Turn's own
  content and whatever continuity from prior Turns is relevant. This
  document deliberately does not assume exactly one reasoning-provider
  invocation per Turn: a future reasoning-provider contract may involve
  a single provider, a sequence of providers, or a provider invoked
  again after an intermediate tool result, before a Turn's processing
  concludes. The exact shape of that is not decided here (Section 13);
  what is fixed is only that the Conversation Engine itself never
  performs the reasoning — it engages whatever the reasoning-provider
  contract eventually specifies, and acts on the result.
- **Route a reasoning provider's resulting proposal onward, through
  existing, already-governed paths only.** If a Turn's interpretation
  implies work worth doing, that becomes an ordinary `PlanningRequest`
  submitted to `PlannerRuntime.plan`, exactly as
  `COMMUNICATION_CONTRACT_DESIGN.md` Section 9 already describes for
  "Cognition." If a Turn's interpretation implies only a reply, that
  becomes an `OutboundParkerResponse` delivered through the originating
  channel's own exposed "deliver" Tool (`COMMUNICATION_CHANNEL_ARCHITECTURE.md`
  Section 4, Section 7) — once `IMPLEMENTATION_GAPS.md` #53's own
  Response Delivery half is itself closed; this document does not close
  it (Section 14).
- **Hold its own bounded, transient Conversation state — nothing else's.**
  Section 4 defines exactly what this is and is not.

## 3. Non-Responsibilities

Restating, for the Conversation Engine specifically, the same discipline
`COMMUNICATION_CHANNEL_ARCHITECTURE.md` Section 1 already applies to a
Communication Channel:

- **Not the reasoning provider itself.** The Conversation Engine does
  not interpret what a message means, does not decide intent, and does
  not generate response content. It engages a reasoning provider for
  that (Section 2) and acts on the result; it is never itself the thing
  producing an interpretation.
- **Not Planner Runtime.** It never generates, ranks, or selects a Plan
  Candidate itself. At most, it is one possible caller of
  `PlannerRuntime.plan` — exactly as any other proposer already is
  (Section 8).
- **Not Task Manager Runtime or Agent Runtime.** It never creates,
  tracks, or transitions a Task Manager Task or an Agent Run directly. A
  Task Proposal it (or the reasoning provider it engaged) causes to be
  submitted is accepted or rejected by the Task Manager Runtime under
  that runtime's own existing rules, exactly like any other Task
  Proposal.
- **Not Authority.** It never grants itself or anything else a
  permission, and never bypasses Identity or Permission evaluation
  (Section 5).
- **Not Memory.** It does not decide what is worth retaining long-term.
  Section 9 states this precisely.
- **Not the World Model.** It does not represent current belief about
  the world's state. Section 9 states this precisely.
- **Not a Communication Channel.** It does not register through
  `ModuleRegistry`, does not expose a "deliver" Tool itself, and is not
  itself the thing a message "arrives through" (`COMMUNICATION_CHANNEL_ARCHITECTURE.md`
  Section 3's own "Channel id" field names the channel, never this
  layer).
- **Not Android, voice, CLI, networking, or UI.** Exactly as excluded for
  every document in this program so far (`COMMUNICATION_CHANNEL_ARCHITECTURE.md`
  Section 8), unchanged here.
- **Not a redefinition of `ExecutionRequest`.** `IMPLEMENTATION_GAPS.md`
  #53's closure path (a) — a Volume 1 core contract revision to
  `ExecutionRequest`'s content-carrying shape — is not attempted,
  proposed, or assumed by this document. Everything this document
  describes is designed to work whether or not that revision is ever
  made, by routing exclusively through `PlannerRuntime.plan` (Section 8)
  rather than constructing an `ExecutionRequest` directly.

## 4. Ownership Boundary

The Conversation Engine owns exactly one thing: **Conversation state** —
which Turns belong to which Conversation, and whatever bounded,
transient continuity information (Section 6) a reasoning provider needs
to treat a new Turn as part of an ongoing exchange rather than reasoning
from nothing. This is a genuinely new kind of state no existing frozen
subsystem owns: it is not a Task Manager Task, not an Agent Run, not a
Planning Session, and not Memory or World Model content (Section 9).

**Invariant: Conversation state exists solely to preserve continuity
between Turns, and carries no authority outside the Conversation it
belongs to.** It is a continuity record, never a workflow record — it
must never be used to hold state that belongs to a Task, an Agent Run,
or a Planning Session (restated in Section 5's trust invariants below).
Anything that looks like it needs to survive beyond a single
Conversation's own continuity purpose belongs in Memory, the World
Model, or an already-owned runtime's own state (Section 9) — never
smuggled into Conversation state because it was convenient to reach.

It owns nothing else. In particular, it does not own, and must never
directly mutate:

- `Task`, `TaskStatus`, or any Task Manager Runtime state
  (`TaskManagerRuntimeSpecification.md` Section 4) — owned solely by the
  Task Manager Runtime.
- `AgentRun`, `AgentRunStatus`, or any Agent Runtime state
  (`AgentRuntimeSpecification.md` Section 4) — owned solely by the Agent
  Runtime.
- Planning Session state (`PlannerRuntimeSpecification.md` Section 4) —
  owned solely by the Planner Runtime.
- `ModuleDescriptor`, `ModuleStatus`, or any Module Registry state
  (`MODULE_CONTRACT_DESIGN.md`) — owned solely by the Module Registry;
  the Conversation Engine is not a Communication Channel and does not
  register one on a channel's behalf.
- Memory records or World Model beliefs (Section 9) — owned solely by
  Memory Runtime and World Model Runtime respectively.
- Permission decisions or Principal identity/lifecycle — owned solely by
  the Permission Engine and Identity Service respectively (Section 5).

The Conversation Engine sits, conceptually, at the same level Task
Manager Runtime already occupies relative to Agent Runtime: a
coordinating layer with its own narrow, bounded state, positioned
between two already-governed surfaces (`CommunicationIntake` on one
side, `PlannerRuntime`/Response Delivery on the other), never reaching
into either surface's own owned state directly, and never becoming a
"seventh peer" alongside the six frozen runtime subsystems
`ARCHITECTURE_V2_FROZEN_BASELINE.md` Section 2 names (Identity Runtime,
EventBus, Planner Runtime, Agent Runtime, Memory Runtime, World Model
Runtime) — restating, for this layer, the same "not a new peer runtime
subsystem" conclusion `COMMUNICATION_CHANNEL_ARCHITECTURE.md` Section 4
already reaches for Parker Runtime intake itself.

## 5. Trust Boundary

**The Conversation Engine coordinates messages. It does not decide
whether Parker acts.**

Restating the Constitution's own law, unchanged for this layer:

> Cognition proposes. Trust authorises. Runtime executes.

Concretely: the reasoning provider the Conversation Engine engages
(Section 2) may propose that a Turn implies an action; only the
resulting Task Proposal, evaluated by the Task Manager Runtime and, once
accepted, executed under the same Permission Engine and Execution
Pipeline path every other proposal already uses, ever leads to real
effect. The Conversation Engine itself:

- **Never self-authorises.** It has no path to `ExecutionPipeline.submit`
  or `PermissionEngine.evaluate` of its own; Section 8 defines its only
  two legitimate outward paths (`PlannerRuntime.plan`, and, once
  authorised by a future unit, Response Delivery's Tool-based path).
- **Acts under a resolvable Principal identity, never anonymously.**
  Mirroring `InMemoryPlannerRuntime`'s own `PLANNER_RUNTIME_PRINCIPAL_ID`
  and `InMemoryTaskManagerRuntime`'s own
  `TASK_MANAGER_RUNTIME_PRINCIPAL_ID` precedent (both resolved through
  `IdentityService` before publishing or acting, per
  `IMPLEMENTATION_GAPS.md` #49's own closure), any Conversation Engine
  implementation must resolve its own operating identity through the
  Identity Service before engaging a reasoning provider or submitting a
  `PlanningRequest` — this document does not name the exact identity
  value (Stage 3/6 territory), only the requirement that one must exist
  and be resolvable, so that nothing this layer does is ever
  unattributable.
- **Never treats the message's sender as its own identity.** A Turn's
  `InboundOwnerMessage.senderPrincipalId` remains the owner's identity
  throughout (`COMMUNICATION_CONTRACT_DESIGN.md` Section 5, Section 9's
  own "the message's own `senderPrincipalId` (the owner, not the
  channel)" rule) — the Conversation Engine's own operating identity,
  above, is a distinct, second Principal, never substituted for the
  owner's.
- **Treats `metadata` as non-authoritative**, exactly as
  `COMMUNICATION_CHANNEL_ARCHITECTURE.md` Section 3 already establishes
  for every Communication Message field of that kind: nothing this layer
  or anything downstream of it relies on for a trust or planning
  decision may live only in a message's `metadata`.
- **Grants itself no standing capability.** Consistent with ADR-024
  Section A (restated here for a non-module core component, since the
  same discipline applies regardless of whether a component is a module
  or a core service): no implicit trust, no self-approval, no bypass of
  `PermissionEngine.evaluate`, no direct write into Task, Agent Run, or
  Planning Session state, and no private or parallel request path.
- **Is the only conversational path into planning.** A Conversation may
  exist, and a Turn within it may complete, without ever producing a
  Task Proposal — restating Section 6, a Turn may conclude at reasoning
  or response alone. But **no Task Proposal may originate from a
  conversational exchange except through the Conversation Engine's own
  routing** (Section 8). This is a strictly one-way dependency —
  Conversation depends on nothing downstream of it, and nothing
  downstream of Planner Runtime may reach back and originate or resume a
  Conversation on its own authority. This forecloses a future component
  inventing a second, parallel conversational entry point into
  `PlannerRuntime.plan`: if a proposal's origin is conversational, it
  passed through this layer, or it was not legitimately conversational
  in origin.

## 6. Lifecycle

At the architecture level only — no Kotlin state machine is defined
here; the exact states, transitions, and their names are Contract
Design's job (Section 13, Section 14).

A **Turn** is the bounded unit of processing for one inbound message:

```
Owner message
     ↓
Conversation processing
     ↓
Reasoning
     ↓
Planning (optional)
     ↓
Response (optional)
     ↓
Turn complete
```

Not every Turn reaches every stage. Restating Section 2 and Section 8: a
Turn may conclude at reasoning alone (the reasoning provider determines
no action or reply is warranted), at response only (no planning
implied), or at planning (whose own downstream disposition, once
submitted, is no longer this layer's concern). "Optional" here means a
Turn's processing may legitimately skip a stage — it never means a stage
may be skipped silently in a way that hides what happened; Section 11
governs how an incomplete or failed Turn is surfaced.

Conceptually, a **Conversation** is the bounded grouping of related
**Turns**:

1. An accepted `InboundOwnerMessage` arrives from `CommunicationIntake`
   (Section 7). The Conversation Engine determines whether it continues
   an existing Conversation or begins a new one (the exact recognition
   rule — correlation-based, time-window-based, explicit-session-based,
   or some combination — is not decided here; Section 13).
2. A reasoning provider is engaged for that Turn (Section 2), given
   whatever continuity from the Conversation's prior Turns is relevant.
3. The reasoning provider's proposal is routed onward (Section 8) —
   either as a `PlanningRequest` to `PlannerRuntime.plan`, or, once
   authorised, as an `OutboundParkerResponse` for delivery.
4. The Turn concludes once a response has been delivered, a Task
   Proposal has been submitted (its own downstream disposition is no
   longer this layer's concern, per Section 8), or the reasoning
   provider concludes no action or reply is warranted.
5. The Conversation continues (a further Turn arrives that continues
   it), goes idle, or ends. The exact idle/termination rule, and whether
   a Conversation can span more than one Communication Channel, are not
   decided here (Section 13).

No Conversation, and no Turn within one, ever grants itself a status
that implies authorisation. "Conversation" and "Turn" are continuity and
correlation concepts only — restating Section 5, neither is, or ever
becomes, a substitute for a Task Proposal, a Plan Decision, or a
Permission Decision.

## 7. Relationship to `CommunicationIntake`

The Conversation Engine **is** the "downstream consumer of Parker
Runtime intake" `COMMUNICATION_CHANNEL_ARCHITECTURE.md` Section 8 named
without designing, and it settles the open question Section 4 of that
same document left for a later document to resolve: an accepted
`InboundOwnerMessage` is handed to the Conversation Engine, not directly
to an unnamed "Cognition" with no structural home.

What this relationship is **not**:

- The Conversation Engine never calls
  `CommunicationIntake.submitInboundMessage` — that remains the
  Communication Channel's own responsibility
  (`LOCAL_TEXT_CHANNEL_CONTRACT_DESIGN.md` Section 1), never this
  layer's.
- The Conversation Engine never repeats, second-guesses, or bypasses
  `CommunicationIntake`'s own two structural checks (`ModuleStatus ==
  ENABLED`, sender resolves) — by construction, it only ever sees
  messages for which both are already true.
- The Conversation Engine is not a new `EventBus` subscriber for this
  purpose. Restating `COMMUNICATION_CHANNEL_ARCHITECTURE.md` Section 3's
  own rule for the inbound path generally: an accepted
  `InboundOwnerMessage`'s delivery to the Conversation Engine is not
  modelled as an `EventBus` publication, and must not become one — an
  inbound owner message can lead to real action, which is exactly what
  `EventBus`'s observability-only discipline forbids for anything
  delivered through it (ADR-023 Rule 3/4, ADR-024 Section A Rule 2).

The exact mechanism — a pull-style inspection surface (mirroring
`InMemoryCommunicationIntake.acceptedMessages()`'s existing,
already-implemented precedent), a push-style callback registration, or
something else — is explicitly **not decided here**. `COMMUNICATION_CONTRACT_DESIGN.md`
Section 14's own first open item names this exact question and reserves
it for "a future Contract Design pass once Cognition/Conversation
Engine itself is scoped" — this document is that scoping; the mechanism
itself remains Contract Design's job (Section 14).

## 8. Relationship to `TaskProposal` / Planner / Execution Pipeline

Restating `COMMUNICATION_CONTRACT_DESIGN.md` Section 9's own already-approved
pattern, now naming the Conversation Engine as one concrete party to it
(that section itself left this attribution open, between "Cognition" and
"a future Conversation Engine"):

- **If a Turn's interpretation implies a goal worth planning**, the
  reasoning provider's resulting proposal becomes an ordinary
  `PlanningRequest` — `initiatingPrincipalId` = the Turn's own
  `InboundOwnerMessage.senderPrincipalId` (the owner, never the
  Conversation Engine's own operating identity), `correlationId` = the
  message's own `CorrelationId.value`, `source` = `RequestOrigin.TEXT`
  (already the default) — submitted via `PlannerRuntime.plan`, exactly
  as any other caller already does. No new entry point into Planner
  Runtime is introduced; `PlanningRequest`'s existing shape requires no
  change (`COMMUNICATION_CONTRACT_DESIGN.md` Section 9's own conclusion,
  restated here).
- **From `PlannerRuntime.plan` onward, nothing changes.** A resulting
  Task Proposal is accepted or rejected by the Task Manager Runtime
  under its own existing rules; an accepted proposal may acquire zero,
  one, or many Agent Run References exactly as any other Task already
  can (`TaskManagerRuntimeSpecification.md` Section 6); any Agent Run
  submits through the Execution Pipeline exactly as any other Agent Run
  already does. The Conversation Engine has no special channel into any
  of this, and no visibility requirement beyond what any other Task
  Proposal's originator already has.
- **The Conversation Engine never constructs an `ExecutionRequest`
  directly, and never calls `ExecutionPipeline.submit` or
  `PermissionEngine.evaluate` directly.** Restating Section 5: its only
  two legitimate outward paths are `PlannerRuntime.plan` (this section)
  and, once authorised, Response Delivery's Tool-based path (Section 2).
- **A Goal's origin, for a Conversation-originated `PlanningRequest`, is
  "a Turn this layer coordinated."** `AgentRuntimeSpecification.md`
  Section 4 already states a Goal's formation mechanism — "user request,
  schedule, another system" — is out of scope for that document. This
  document does not change that; it names the Conversation Engine as one
  concrete answer to "how a Goal is formed" for the conversational-origin
  case, without altering `Goal`'s own existing shape or the Agent Runtime
  Specification itself.

## 9. Relationship to Memory, World Model, and Reasoning Context

**The Conversation Engine is not a fourth knowledge layer alongside
Memory, the World Model, and Reasoning Context** (`reasoning-context.md`).
Its own Conversation/Turn continuity state (Section 4, Section 6) is
conceptually the kind of "information supplied directly as part of the
current request" `reasoning-context.md`'s own definition of Reasoning
Context already names as one of the inputs Reasoning Context assembly
may draw on — not a competing or parallel definition of "what Parker
currently knows."

Precisely:

- **The Conversation Engine does not assemble Reasoning Context.** That
  remains "Reasoning Context assembly"'s own, already-named
  responsibility (`reasoning-context.md`, "Architectural
  Responsibilities"). The Conversation Engine, at most, supplies one
  input to that assembly — the current Turn's content and relevant
  continuity from prior Turns in the same Conversation — for a given
  task's Reasoning Context to be built from, alongside whatever Memory
  and World Model already contribute.
- **The Conversation Engine is not Memory.** It does not decide what is
  worth retaining long-term, and Conversation/Turn state is not
  durable knowledge by virtue of existing. If a Turn's content is ever
  worth promoting into Memory, that happens only through Memory
  Runtime's own existing promotion-governed interface, called by
  whatever component already has that authority — restating
  `COMMUNICATION_CONTRACT_DESIGN.md` Section 9's identical rule, now
  naming the Conversation Engine as a candidate caller alongside
  Cognition, never a bypass.
- **The Conversation Engine is not the World Model.** It does not
  represent current belief about the world's state. If a Turn describes
  something worth updating World Model's belief about, that happens only
  through World Model's own existing interface, called by whatever
  component already has that authority — same rule, restated.
- **Reasoning Context remains ephemeral, per task, exactly as
  `reasoning-context.md` already establishes.** A Conversation's own
  continuity state is not itself Reasoning Context (Reasoning Context is
  assembled per task and discarded once that task concludes); a
  Conversation may outlive any single task-scoped Reasoning Context
  built from one of its Turns, and this document does not collapse the
  two concepts into one.

## 10. Event Model

**No new `EventBus` publication is introduced or mandated by this
document.** Restating Section 7: an inbound message's delivery to, and
handling by, the Conversation Engine is never modelled as event
publication or subscription — it can lead to real action, which
`EventBus`'s observability-only discipline (ADR-023, ADR-024 Section A)
forbids for anything delivered through it.

This document **authorises, but does not require, mandate, or name**, a
future `conversation.*` `EventType` namespace for purely observational
purposes, mirroring `agent.*`/`task.*`/`planner.*`'s own existing role —
one-way broadcast, never a control channel, never treated by any
subscriber as authorisation to act (ADR-023 Rules 3–5, restated here for
a non-context-provider coordinating layer by the same reasoning ADR-024
Section A already applies to modules generally). Candidate event
*categories* a future Contract Design pass may define exactly (names,
payload schemas — neither is decided here, mirroring
`AgentRuntimeSpecification.md` Section 9's own "specifies event types,
not payload schemas" split): a Turn beginning, a reasoning provider being
engaged, a Turn's proposal being routed onward (to Planner Runtime or to
Response Delivery), and a Conversation ending. None of these is a
requirement this document imposes on a future implementation; they are
named only so a future unit does not have to independently re-derive
that such a namespace would be permissible.

**If the Conversation Engine ever subscribes to the `EventBus`** — for
example, to observe `task.completed`/`agent.failed` for a Task Proposal
it caused to be submitted — that subscription is governed by the same
discipline ADR-024 Section A Rule 2 already imposes on any module: read-only
observability only, never a trigger for autonomous behaviour that
bypasses Sections 5/8's own outward paths. This document does not decide
whether such a subscription is ever needed (Section 13).

## 11. Failure Model

Restating this platform's own established discipline — visible, explicit
failure, never a silent retry that grants extra authority, never a
guess dressed up as confidence:

- **A reasoning provider that fails, times out, or cannot confidently
  interpret a Turn is a failure to be surfaced, not papered over.**
  Exactly as the Constitution's own "If trust cannot be verified, Parker
  cannot act" already establishes for trust specifically, the
  Conversation Engine's correct behaviour when interpretation itself is
  unreliable is to make that visible — for example, by producing a
  response that asks for clarification, or by recording a failed Turn —
  never to proceed as if a low-confidence interpretation were a
  confident one.
- **A `PlanningRequest` this layer causes to be submitted may be
  rejected by `PlannerRuntime.plan` (`PlanningSessionResult.Failed`,
  `PlannerRuntimeSpecification.md` Section 5) exactly as any other
  caller's request may be.** The Conversation Engine has no special
  retry, override, or escalation path around such a rejection; restating
  Section 8, its only recourse is the same one any other caller has —
  surfacing the failure, or trying again as a genuinely new request.
- **A Task Proposal's own downstream disposition (accepted, rejected,
  deferred, split, merged — `TaskManagerRuntimeSpecification.md` Section
  15) is not this layer's failure to handle.** Once submitted, its fate
  belongs to the Task Manager Runtime under that runtime's own existing
  rules, exactly as for any other proposer.
- **A message that cannot be correlated to any existing Conversation, or
  that correlates ambiguously, is not an error condition that blocks
  processing** — Section 6 already allows a new inbound message to begin
  a new Conversation when continuation cannot be established. The exact
  ambiguity-resolution rule is not decided here (Section 13).
- **Response Delivery failing (once it exists) is a failure of that
  separately-scoped mechanism, not of this layer** — the Conversation
  Engine's own responsibility ends at handing a well-formed
  `OutboundParkerResponse` (or equivalent) to the existing Tool-invocation
  path (Section 2, Section 8); it does not retry indefinitely or invent
  a parallel delivery mechanism if that path reports failure.

## 12. Security Model

Restating the Constitution's Constitutional Tests (`parker-constitution.md`),
applied specifically to this layer:

- **Owner control is preserved.** Every action a Conversation Turn
  eventually leads to still passes through the same Permission Engine
  evaluation, still traces to the owner's own `senderPrincipalId`
  (Section 5), and remains as visible and revocable as any other
  authorised action — this layer introduces no separate, less-visible
  path.
- **Authority and capability remain separate.** Engaging a reasoning
  provider (a capability) never itself authorises anything (Section 5);
  the reasoning provider's proposal still requires the full Cognition
  proposes / Trust authorises / Runtime executes chain.
- **No bypass of trust authorisation exists.** Section 8 names the
  Conversation Engine's only two legitimate outward paths, both already
  fully governed; no third, private path is introduced anywhere in this
  document.
- **Auditability is preserved.** Because the Conversation Engine must
  act under its own resolvable Principal identity (Section 5), and
  because every downstream effect still flows through
  `PlannerRuntime.plan`/the Execution Pipeline's own existing,
  already-auditable path, nothing this layer does is unattributable or
  untraceable back to a specific Turn and a specific owner request.
- **Conversational content is owner data, and is treated with the same
  sensitivity discipline as any other Resource.** A Turn's raw text may
  contain anything an owner chooses to say — including
  `ResourceSensitivity.PERSONAL`, `FINANCIAL`, `MEDICAL`, or more
  sensitive categories (`Resource-Schema.md`'s existing enum). This
  document does not invent a new classification scheme for conversation
  content; any future persistence of Conversation/Turn state, or
  promotion of Turn content into Memory (Section 9), is bound by exactly
  the same Resource sensitivity and Memory-promotion discipline
  everything else in this platform already is — no special exemption for
  content that happened to arrive via conversation.
- **No standing capability is granted by a Conversation's mere
  existence.** A Conversation being "in progress" is a continuity fact,
  never itself a grant of trust, permission, or elevated confidence in
  a subsequent Turn's interpretation (Section 6, Section 11).

## 13. Open Questions and Deferred Items

Named rather than invented, per this program's own established
discipline (`LOCAL_TEXT_CHANNEL_CONTRACT_DESIGN.md` Section 10's
identical convention):

1. **The exact mechanism by which the Conversation Engine consumes an
   accepted `InboundOwnerMessage` from `CommunicationIntake`** (Section
   7) — pull-style, push-style, or something else. Contract Design's
   job.
2. **The exact Conversation/Turn lifecycle state machine** (Section 6) —
   states, transitions, and their names. Contract Design's job.
3. **The exact rule for recognising that a new inbound message continues
   an existing Conversation** rather than beginning a new one (Section
   6, Section 11) — correlation-based, time-window-based,
   explicit-session-based, or some combination. Whatever this rule
   becomes, **Conversation identity is distinct from `CorrelationId`**:
   `CorrelationId` ties one inbound message to whatever eventually
   answers it (`COMMUNICATION_CONTRACT_DESIGN.md` Section 4), scoped to
   a single Turn; a Conversation identifier, if one is ever introduced,
   would group many Turns — each with its own `CorrelationId` — under
   one continuing exchange. The two are not interchangeable, even though
   a future recognition rule may use `CorrelationId` as one input among
   others.
4. **Idle and termination rules for a Conversation** — how long a
   Conversation remains eligible for continuation, and what ends one
   explicitly versus by inactivity.
5. **Whether a Conversation may span more than one Communication
   Channel** — for example, beginning on the Local Text Channel and
   continuing on a future Android text channel
   (`COMMUNICATION_CHANNEL_ARCHITECTURE.md` Section 9, Phase 2). Not
   decided here.
6. **The exact relationship between the Conversation Engine and a
   reasoning provider** — call direction, dependency shape, and whether
   this is one relationship or a family of them (Section 2, Section 9).
   This cannot be fully resolved even at Contract Design level until a
   reasoning-provider/Cognition Kotlin contract itself exists somewhere
   in this repository — none does today (`ModelManager.kt` remains
   excluded from the build per `build.gradle.kts`, and no interface
   named for a reasoning provider exists in `src/interfaces`). A future
   Conversation Engine Contract Design pass may therefore need to scope
   itself to what Section 7 already permits (consuming accepted
   messages, maintaining continuity) without yet resolving this item,
   mirroring exactly how Communication Contract Design itself scoped
   Unit C1 to structural acceptance only, deferring Response Delivery
   and Cognition consumption (Section 14).
7. **Whether, and exactly how, a `conversation.*` `EventBus` namespace is
   defined** (Section 10) — names, payload schemas, and whether it is
   needed at all before a real consumer exists for it, per this
   platform's own established "100,000-line test" discipline.
8. **`IMPLEMENTATION_GAPS.md` #53's closure path (a)** — `ExecutionRequest`'s
   content-carrying gap — remains entirely untouched by this document,
   exactly as Section 3 states. Whether that path is ever pursued,
   instead of or alongside this document's own path (b), is not decided
   here.
9. **Whether the Conversation Engine ever needs durable state beyond a
   single Conversation's own lifetime** — ties directly to
   `IMPLEMENTATION_GAPS.md` #51's own open persistence/durability
   boundary question. Not decided here; any future durability claim for
   Conversation state is subject to whatever ADR eventually settles gap
   #51 generally, not a special case this document carves out.
10. **The Conversation Engine's own exact operating Principal identity
    value** — Section 5 states only that one must exist and be
    resolvable; the concrete value (mirroring
    `TASK_MANAGER_RUNTIME_PRINCIPAL_ID`'s own precedent) is Stage 3/6
    territory.

## 14. Implementation Sequencing

Per PES-001's own stage gates, restated for this document specifically:

1. **This document (Stage 1 Architecture)** — establishes responsibilities,
   ownership, trust boundary, lifecycle (conceptually), relationships to
   every already-frozen subsystem this chapter touches, event model,
   failure model, security model, and the open items above. No Kotlin is
   authorised by this document alone.
2. **Stage 2 Architecture Review** — a human (or human-directed) review
   determining whether the above is sufficiently complete for a Contract
   Design pass to begin against it, per PES-001 Stage 2's own questions
   (are responsibilities explicit? is ownership complete? are runtime
   boundaries defined? are invariants documented? are unresolved
   questions identified?). Not performed by this document itself.
3. **Stage 2A Contract Design (future, separately-scoped)** — scoped to
   resolving exactly the open items Section 13 names that are ready to
   resolve, mirroring `LOCAL_TEXT_CHANNEL_CONTRACT_DESIGN.md`'s own
   precedent of scoping tightly to what a first unit can actually close.
   Given item 6's own dependency on a not-yet-scoped reasoning-provider
   contract, the most immediately achievable first Contract Design pass
   is likely **the Conversation Engine's inbound half only** — consuming
   an accepted `InboundOwnerMessage` and maintaining Conversation/Turn
   continuity state (Section 7, Section 6) — deferring the
   reasoning-provider engagement and Response Delivery routing halves
   (Section 2, Section 8) exactly as Communication Contract Design
   already deferred its own Response Delivery and Cognition-consumption
   halves for Unit C1.
4. **Stage 3 Implementation Plan, Stage 4 Implementation Decisions, Stage
   5 Scope Lock, Stage 6 Implementation** — follow only once Contract
   Design produces field-level Kotlin shapes to plan and implement
   against. None of this is authorised by this document.
5. **`IMPLEMENTATION_GAPS.md` #53 remains open** throughout every step
   above, until whichever future implementation unit actually closes it
   — this document does not close it, and does not claim to (per
   Steven's own explicit instruction).

## 15. Stage 1 Self-Review

Checking this document against PES-001 Stage 1's own seven required
elements (`PARKER_ENGINEERING_STANDARD.md`, Chapter 1, "Stage 1 –
Architecture"):

| Required element | Where satisfied |
| --- | --- |
| Responsibilities | Section 2 |
| Ownership | Section 4 |
| Trust boundaries | Section 5 |
| Runtime boundaries | Section 4 (what it owns vs. every adjacent subsystem), Section 7, Section 8, Section 9 |
| Lifecycle | Section 6 |
| Invariants | Section 3 (non-responsibilities), Section 5 (trust invariants), Section 12 (security invariants) |
| Security model | Section 12 |

Additional checks, mirroring this program's own established
Self-Traceability/Engineering Review convention
(`LOCAL_TEXT_CHANNEL_CONTRACT_DESIGN.md` Section 10's Stage 2A
equivalent, adapted here for Stage 1):

- **No Kotlin interface, class name, package layout, or field-level
  contract is defined anywhere in this document.** Every mechanism
  question (Section 13) is named, not designed.
- **No implementation plan is created.** Section 14 sequences future
  work; it does not itself constitute a Stage 3 document.
- **`ExecutionRequest` is not revised.** Section 3, Section 8, and
  Section 13 item 8 all restate this explicitly.
- **Cognition/the reasoning provider is not implemented.** Section 1 and
  Section 3 state plainly that interpretation and response generation
  remain a reasoning provider's job, never this document's or this
  layer's own.
- **Android, voice, CLI, networking, and UI behaviour are not defined.**
  Section 3 restates this exclusion unchanged from every prior document
  in this program.
- **Trust, Planner, Task Manager, Agent Runtime, and Execution Pipeline
  are not bypassed.** Section 5 and Section 8 together name the
  Conversation Engine's only two legitimate outward paths, both already
  fully governed by existing, unmodified mechanisms.
- **`IMPLEMENTATION_GAPS.md` #53 is not closed.** Section 14 states this
  explicitly; this document is named only as that gap's own Stage 1
  prerequisite, per the Status section above.
- **Every substantive claim in this document traces to an authoritative
  source actually read while drafting it:** `parker-constitution.md`;
  `PARKER_ENGINEERING_STANDARD.md`; `COMMUNICATION_CHANNEL_ARCHITECTURE.md`;
  `COMMUNICATION_CONTRACT_DESIGN.md`; `LOCAL_TEXT_CHANNEL_CONTRACT_DESIGN.md`;
  `07-runtime-architecture.md`; `TaskManagerRuntimeSpecification.md`;
  `AgentRuntimeSpecification.md`; `PlannerRuntimeSpecification.md`;
  `ExecutionPipeline.md`; `ExecutionRequest.md`; `reasoning-context.md`;
  `ARCHITECTURE_V2_FROZEN_BASELINE.md`; ADR-023; ADR-024; and
  `IMPLEMENTATION_GAPS.md` #53 itself. No architecture is invented beyond
  what these sources already anticipate at the concept level.

## Conclusion

**This document gives the Conversation Engine a settled Stage 1
architecture: purpose, responsibilities, non-responsibilities, ownership
boundary, trust boundary, a conceptual (non-Kotlin) lifecycle, its
relationship to `CommunicationIntake`, to `PlannerRuntime`/Task
Manager/Execution Pipeline, to Memory/World Model/Reasoning Context, an
event model (permissive, not mandatory), a failure model, a security
model, ten explicitly named open items, and an implementation sequence
that does not itself authorise any Kotlin.**

Consistent with Milestone 1's own scope, this document does not
implement anything, does not modify any existing architecture, contract,
or specification document, and does not close `IMPLEMENTATION_GAPS.md`
#53 — Response Delivery and Cognition's actual consumption mechanism
remain exactly as open as they were before this document was written,
now with a settled architectural home to be designed against. Once
reviewed and accepted (Stage 2), this document is the basis for a future
Stage 2A Contract Design pass scoped to exactly the open items Section
13 names that are ready to resolve — no more.

## Related

- `docs/architecture/parker-constitution.md`
- `docs/architecture/PARKER_ENGINEERING_STANDARD.md`
- `docs/architecture/COMMUNICATION_CHANNEL_ARCHITECTURE.md`
- `docs/architecture/COMMUNICATION_CONTRACT_DESIGN.md`
- `docs/architecture/LOCAL_TEXT_CHANNEL_CONTRACT_DESIGN.md`
- `docs/architecture/07-runtime-architecture.md`
- `docs/architecture/reasoning-context.md`
- `docs/architecture/ARCHITECTURE_V2_FROZEN_BASELINE.md`
- `docs/specifications/volume-05-task-manager-runtime/TaskManagerRuntimeSpecification.md`
- `docs/specifications/volume-04-agent-runtime/AgentRuntimeSpecification.md`
- `docs/specifications/volume-06-planner-runtime/PlannerRuntimeSpecification.md`
- `docs/specifications/volume-03-core-interfaces/ExecutionPipeline.md`
- `docs/specifications/volume-01-core-contracts/ExecutionRequest.md`
- `docs/adr/ADR-023-context-provider-event-publication.md`
- `docs/adr/ADR-024-module-event-audit-durability-boundary.md`
- `docs/architecture/IMPLEMENTATION_GAPS.md` (#53, #49, #51)
