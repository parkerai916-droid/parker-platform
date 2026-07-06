# Parker Platform — Communication Channel Architecture

## Status

Architecture document. Sprint 6, Track B, Unit C1. Defines how Parker
communicates with its owner through modules, now that the Module
Framework (`MODULE_FRAMEWORK_ARCHITECTURE.md`, `MODULE_CONTRACT_DESIGN.md`)
and its first implementation (`InMemoryModuleRegistry`, Sprint 6 Track A
Unit M1) exist. This document does not implement anything. No Kotlin
interface, class name, package layout, or channel implementation is
defined here — see Section 8 (Out of Scope) and Section 10 (Engineering
Review).

## Purpose

Parker Core — Identity Runtime, EventBus, Planner Runtime, Agent Runtime,
Memory Runtime, and World Model Runtime — is complete and frozen
(`ARCHITECTURE_V2_FROZEN_BASELINE.md`). The Module Framework now gives
Parker a governed way to add capability without touching that foundation.
Communication is the first concrete capability need that foundation must
support: Parker cannot be useful to an owner until a message can travel in
either direction. This document defines the boundary that makes that
possible **without putting communication logic into Parker Core** — the
same discipline `MODULE_FRAMEWORK_ARCHITECTURE.md` already applied to
modules in general, now applied specifically to the owner-facing message
path.

The goal of this Unit is explicitly **not voice**. The goal is the
boundary itself: local text chat, an Android text UI, speech input,
speech output, wake word, and notifications must all be expressible as
the same kind of thing — a Communication Channel — without Parker Core
ever needing to know which one it is talking to.

---

## 1. What a Communication Channel Is

**A Communication Channel is a module-provided transport for messages
between the owner and Parker. It is not cognition, not planning, not
authority, not Memory, and not World Model.**

This restates, for "communication" specifically, the Constitution's own
law and the Module Framework's own restatement of it
(`MODULE_FRAMEWORK_ARCHITECTURE.md` Section 1): "Parker owns authority.
Modules provide capability." A Communication Channel carries a message.
It does not decide what the message means, whether it warrants an action,
or what Parker should do about it.

Concretely, a Communication Channel is a **module** in the sense
`MODULE_FRAMEWORK_ARCHITECTURE.md` Section 2 already defines — most
naturally an **adapter** ("components that translate between Parker's
internal contracts... and an external system's own protocol or data
shape") — where the "external system" happens to be a human owner reached
through a terminal, a phone screen, a microphone/speaker pair, or a
notification surface, rather than a web API or a database. Nothing about
"communication" requires a new category of module; it requires only that
one already-defined category (adapter) be pointed at the owner instead of
at a third-party service.

**What a Communication Channel is not:**

- **Not Cognition.** It does not interpret a message, decide intent, or
  generate a response. Interpreting what an inbound message means, and
  producing the content of an outbound response, is Cognition's job
  (upstream of Planner Runtime), exactly as it already is for any other
  proposer.
- **Not Planning.** It never generates, ranks, or selects a Plan
  Candidate. It may carry the words that eventually lead to one.
- **Not Authority.** It never grants itself or anything else a
  permission, and never bypasses Identity or Permission evaluation
  (ADR-024 Section A, Rule 3 — a Communication Channel is an ordinary
  Principal like any other module).
- **Not Memory.** It does not store conversation history, remember prior
  turns, or decide what is worth retaining. If a message is ever promoted
  to durable knowledge, that happens through Memory Runtime's own
  existing `MemoryStore` interface, by whatever component already has the
  authority to call it — never by the channel itself.
- **Not World Model.** It does not represent current belief about the
  world's state. A spoken or typed observation about the world ("the
  kitchen light is off") still reaches World Model only through World
  Model's own existing interface, called by whatever component already
  has that responsibility — not by the channel directly.

## 2. Direction

The architecture distinguishes exactly two message directions, and they
are not symmetric in what they require of Parker Core (Section 4 explains
why):

- **Inbound owner message** — words (typed, spoken, or otherwise
  transported) originating from the owner, arriving through a
  Communication Channel module, addressed to Parker.
- **Outbound Parker response** — words originating from Parker (by way of
  whatever produced them — Cognition, Planner Runtime, Agent Runtime, or
  a future Conversation Engine), addressed to the owner, delivered through
  a Communication Channel module.

A single Communication Channel module may support one direction or both
(a text chat channel naturally supports both; a notification channel may
be outbound-only; a future wake-word listener may be inbound-only, in a
very narrow sense — see Section 6 and Section 9). Neither direction is
optional for the architecture to define, even though the first
implementation target (Section 6) exercises both in their simplest form.

## 3. Minimum Message Shape

At the architecture level only — no Kotlin type is defined here; the
exact field-level shape belongs to Communication Contract Design (Section
10). A Communication Message, in either direction, must be able to
express:

- **Channel id** — which Communication Channel this message travelled
  through, so a response can be routed back to the same surface it came
  from, and so more than one channel can be active at once (a text
  channel and a future Android channel, say) without ambiguity.
- **Sender principal** — the `Principal` responsible for this message:
  the owner's own `Principal` for an inbound message, and Parker's
  responding identity (or the identity of whatever Runtime component
  produced the response) for an outbound one. Every Communication Channel
  is itself an ordinary Principal (Section 1); the sender recorded on a
  message is not the channel's own identity, it is whoever the channel is
  carrying the message on behalf of.
- **Message text** — the human-readable content being carried. This
  document does not require every channel to originate text directly (a
  voice channel's audio still needs to become text somewhere — Section 8
  excludes designing that conversion here), only that whatever reaches
  this boundary is expressed as text by the time it does.
- **Timestamp** — when the message was sent, not when it happened to be
  processed.
- **Correlation id** — ties an inbound message to whatever outbound
  response eventually answers it (and, where useful, ties a whole
  exchange together), mirroring the `correlationId` field every other
  request/event shape in this platform already carries
  (`ExecutionRequest`, `ParkerEvent`).
- **Optional metadata** — a channel-specific, non-authoritative extension
  point (e.g. a spoken message's confidence score, a text UI's rendering
  hints), exactly mirroring `ParkerEvent.metadata`'s and
  `ToolDescriptor`'s own existing "optional, additive, non-normative"
  precedent. Nothing Parker Core relies on for a trust or planning
  decision may live only in metadata.

**A Communication Message is deliberately not a `ParkerEvent`, and this
document does not model either direction as an `EventBus` publication.**
`EventBus` events are observability-only and may never be treated as
authorisation to act (ADR-023 Rule 4; ADR-024 Section A, Rule 2). An
inbound owner message must be capable of leading to real action once
properly interpreted — that is the entire point of the owner being able
to talk to Parker at all — which is exactly what `EventBus`'s existing,
correct discipline forbids for anything delivered through it. Reusing
`EventBus`/`ParkerEvent` for inbound delivery would either quietly break
that discipline or make owner messages inert, and this document rejects
both outcomes. (An outbound delivery, or an inbound message's mere
arrival, MAY still be separately, additionally observed via an ordinary
`communication.*` broadcast event later, under the same observability-only
rule every other subsystem's events already follow — that is an
independent, optional decision for a future unit, not a substitute for
the delivery path itself.)

## 4. Routing

```
Owner
  |  (speaks / types)
  v
Communication Channel Module         <- a module; not Cognition, not authority
  |  (inbound Communication Message: channel id, sender principal,
  |   text, timestamp, correlation id, optional metadata)
  v
Parker Runtime intake                <- boundary only; interprets nothing itself
  |
  v
Cognition / Planner Runtime / Agent Runtime / Memory / World Model
  |  (as needed -- interpretation, planning, permission evaluation,
  |   execution, context read/write, all via their own existing,
  |   already-governed interfaces)
  v
Response
  |  (outbound Communication Message, same correlation id)
  v
Communication Channel Module
  |  (delivers)
  v
Owner
```

**Inbound half.** A Communication Channel module receives words from the
owner and hands them, unmodified and uninterpreted, to **Parker Runtime
intake** — a thin reception boundary, not a decision-maker. Intake's only
job is to make an inbound Communication Message available to whatever
governs interpretation today or in the future (Cognition, and downstream
of it Planner Runtime for anything that becomes a Task Proposal) — it
does not itself decide what the message means or whether anything should
happen as a result. From there, ordinary planning, permission evaluation,
and execution proceed exactly as they already do for any other proposer
(Section 5).

**Why "Parker Runtime intake" is named here but not shaped as Kotlin.**
None of the three existing entry points into Parker Core fit an
unsolicited, unstructured inbound message cleanly, and forcing one to fit
would be a category error, not a simplification:

- `EventBus.publish` is wrong by construction (Section 3 — observability
  only, never authorisation to act).
- `ExecutionPipeline.submit` expects an already-decided
  `ExecutionRequest` — a specific, named action. An inbound message is
  not yet a decided action; it is raw material Cognition has not yet
  interpreted.
- `PlannerRuntime.plan` expects an already-structured `PlanningRequest`,
  itself tied to the existing Task/Task Proposal model. Reusing it
  directly for raw conversational text would mean redefining
  `PlanningRequest`, which is out of this document's scope (no
  architecture is redesigned, per this Unit's own constraints) and is not
  obviously correct besides — not every inbound message becomes a Task.

So a minimal reception boundary is, in this narrow sense, unavoidable.
This does **not** mean a new, seventh peer runtime subsystem alongside the
six `ARCHITECTURE_V2_FROZEN_BASELINE.md` names as frozen. The closest,
already-accepted precedent for exactly this shape — a narrow interface
whose only job is to receive something from outside and hand it to the
component that already owns deciding what to do with it — is
`TaskProposalIntake`, which already exists in this codebase for an
analogous purpose (Planner Runtime submitting Task Proposals to Task
Manager Runtime, without Task Manager needing to know how a proposal was
generated). **"Parker Runtime intake" should be understood as this same
kind of thin, `TaskProposalIntake`-shaped reception boundary, for
Communication Messages instead of Task Proposals** — not a new authority,
not a second Planner, and not a change to any of the six frozen
subsystems. Its exact interface, its precise downstream target
(Cognition directly? A future Conversation Engine sitting in front of
Planner Runtime? Section 9's later phases?), and whether it is one
interface or a small family of them, is Communication Contract Design's
job (Section 10), not this document's.

**Outbound half.** A response, once produced, is delivered by invoking
the originating Communication Channel module's own exposed capability —
an ordinary Tool, reached through `ExecutionPipeline`/`ToolRegistry`
exactly like any other Tool (Section 7). No new mechanism is required for
this half: it is already fully covered by the Module Framework's existing
tool-exposure model. The correlation id (Section 3) is what lets whatever
produced the response address it back to the channel id the inbound
message arrived on.

## 5. Trust Boundary

**Communication modules may carry messages. They do not decide whether
Parker acts.**

If an inbound message ever results in an action, that action still
follows the Constitution's own law in full, with no shortcut introduced
for having arrived via a Communication Channel:

**Cognition proposes. Trust authorises. Runtime executes.**

Concretely: a Communication Channel hands a message to Parker Runtime
intake (Section 4); Cognition — not the channel — is what may propose
that the message implies an action; any such proposal still becomes a
Task Proposal, a Plan Candidate, or an `ExecutionRequest` exactly as any
other proposal does, evaluated by `PermissionEngine.evaluate` exactly as
any other request is (AD-007), executed only through `ExecutionPipeline`
exactly as any other approved action is (AD-003). A Communication Channel
is never granted implicit trust, never self-authorises, and never holds a
private or parallel path to action (ADR-024 Section A, Rules 3, 5, 6) —
it is an ordinary module, and therefore an ordinary Principal, subject to
exactly the same chain as every other actor in this platform.

## 6. First Implementation Target

**The first implementation target is a local text channel.** Not
Android. Not voice. Not wake word. Not Bluetooth. Not Home Assistant.

A local text channel — the owner types into some local text surface
(e.g. a terminal or a minimal local UI), Parker responds in text, on the
same machine, with no network transport and no speech processing of any
kind — proves the entire message loop (Section 4's routing diagram, end
to end) with the fewest moving parts possible: no audio pipeline, no
platform-specific UI framework, no wake-word detector, no remote
transport. Every requirement this document defines (channel identity,
direction, message shape, routing, trust boundary, module registration)
is exercised by a local text channel exactly as it would be by any more
elaborate future channel — the later channels differ only in how a
Communication Message's text gets in and out, never in how it is routed,
trusted, or governed once it exists.

## 7. Module Relationship

**Communication Channels are modules.** Nothing about communication
exempts them from the Module Framework this platform already has:

- They **must register through `ModuleRegistry`**, exactly as any other
  module does (`MODULE_CONTRACT_DESIGN.md` Section 5) — declaring a
  `ModuleDescriptor` (name, version, exposed tools, required permissions,
  connectivity declaration, and so on) and moving through the same
  Registered → Enabled → Disabled → Removed lifecycle every other module
  moves through.
- They **may expose tools only if authorised** — an outbound "deliver
  this message" capability is an ordinary `ToolDescriptor` entry in the
  channel's `toolsExposed` list (`MODULE_CONTRACT_DESIGN.md` Section 2),
  registered with `ToolRegistry` at Module Registration time exactly as
  Unit M1 already implements, reachable only once the module is Enabled,
  and only ever invoked through the ordinary Execution Pipeline path.
- They **must not bypass Permission Engine or Execution Pipeline** — a
  Communication Channel's declared `requiredPermissions`
  (`ModulePermissionRequirement`) inform whatever Principal decides to
  Enable it, but grant nothing by themselves (`MODULE_CONTRACT_DESIGN.md`
  Section 6); every actual invocation of its exposed "deliver" Tool is
  independently evaluated by `PermissionEngine.evaluate`, exactly like
  any other Tool invocation.
- A Communication Channel's inbound side (handing a message to Parker
  Runtime intake, Section 4) is a new kind of module behaviour this
  platform has not previously named — it is not tool invocation, and it
  is not event subscription. It is closer, in spirit, to how a module
  might one day be a context provider or an event publisher, and should
  be governed by the same "publication requires authorisation" discipline
  ADR-023/ADR-024 already established for context providers, adapted for
  intake rather than broadcast. The exact authorisation mechanism for a
  module to be allowed to write to Parker Runtime intake at all is
  Communication Contract Design's job (Section 10).

## 8. Out of Scope

This document does not define, and Communication Contract Design (Section
10) is where each of the following belongs once it is reached:

- Speech-to-text or text-to-speech of any kind.
- Wake word detection.
- Android services or any Android-specific integration (Chapter 27
  remains a consumer of this boundary, not a reason to change it).
- Notifications (Chapter 25) as a concrete mechanism.
- Remote messaging, cloud chat, or any network transport.
- Home Assistant voice integration.
- LLM prompt design, or any other detail of how Cognition interprets an
  inbound message or composes an outbound one.
- Conversation memory design — turn-taking, continuity, clarification, or
  follow-up handling (Chapter 19, Conversation Engine) remains a
  conceptual stub this document does not flesh out; it is named in
  Section 4 only as the eventual downstream consumer of Parker Runtime
  intake, not designed here.
- Any Kotlin interface, type, class name, or package layout for a
  Communication Channel, a Communication Message, or Parker Runtime
  intake.
- Any actual channel implementation, reference or otherwise — including
  the local text channel named in Section 6 as the first target.

## 9. Future Phases

In order, each building on the boundary this document defines without
requiring it to change:

1. **Local text channel** (Section 6) — proves the full message loop
   locally, in text, with no network and no speech processing.
2. **Android text channel** — the same boundary, reached from an Android
   UI surface instead of a local terminal; a consumer of this
   architecture, not a reason to revise it.
3. **Speech input** — adds a speech-to-text conversion step ahead of this
   boundary; from Parker Runtime intake's perspective, an inbound message
   still arrives as text (Section 3).
4. **Speech output** — adds a text-to-speech conversion step after this
   boundary; from the responding component's perspective, an outbound
   message is still produced as text and delivered through the channel's
   ordinary "deliver" Tool (Section 4).
5. **Wake word** — a narrow, local trigger that determines when a speech
   channel should start listening; it gates *when* an inbound message
   begins, not *what* the boundary requires of one once it arrives.
6. **Notifications** — an outbound-only channel shape (Chapter 25);
   exercises only the outbound half of Section 4's routing, with no
   inbound direction at all.

No phase after the first requires this document to be revised — each
adds a new kind of Communication Channel module, or a new conversion step
ahead of or behind the boundary, without changing what a Communication
Channel *is* (Section 1), how messages are shaped (Section 3), how they
are routed (Section 4), or how trust is preserved (Section 5).

## 10. Engineering Review — What Communication Contract Design Must Resolve

A future Communication Contract Design document, following PES-001 Stage
2A, must settle:

- The concrete Kotlin shape of a Communication Message, a Communication
  Channel's manifest fields (beyond what an ordinary `ModuleDescriptor`
  already carries, if anything), and whether any new supporting type
  (e.g. a channel-kind enumeration) is actually needed or is better
  expressed through existing `ModuleDescriptor`/`ToolDescriptor` fields.
- The concrete shape of **Parker Runtime intake** (Section 4): whether it
  is a new, narrow Volume-3-style interface mirroring
  `TaskProposalIntake`, an extension of an existing interface, or a small
  family of interfaces — and, specifically, what it hands an inbound
  message *to* (Cognition directly, or a Conversation-Engine-shaped
  intermediary that does not yet exist).
- The authorisation mechanism gating which modules may write to Parker
  Runtime intake at all (Section 7's own open item) — mirroring how
  ADR-023/ADR-024 gate context-provider event publication, adapted for
  intake rather than broadcast.
- Whether a Communication Channel's outbound "deliver" Tool needs any
  shape beyond an ordinary `ToolDescriptor` (Section 4, Section 7), or
  whether the existing Tool model is already sufficient without
  extension.
- How correlation ids (Section 3) are minted, and by whom — the channel,
  Parker Runtime intake, or whatever component first receives an inbound
  message — following this platform's existing "runtime-minted,
  deterministic where possible" identifier discipline
  (`docs/architecture/PRE_MODULE_ID_MULTIPLICITY_DECISION.md`'s own
  reasoning is a direct precedent to check against).
- Whether inbound message arrival should also be observable as an
  ordinary `communication.*` `EventBus` event, purely for
  observability, alongside (never instead of) the intake delivery path
  itself (Section 3's own parenthetical).
- Whether the local text channel (Section 6) needs any new Resource
  type, or is fully expressible through the existing `Resource`/
  `ResourceType.TOOL` model Unit M1's own Module Registry implementation
  already establishes for module-exposed Tools.

## Conclusion

**Parker is ready for Communication Contract Design, on the basis of this
document together with the Module Framework
(`MODULE_FRAMEWORK_ARCHITECTURE.md`, `MODULE_CONTRACT_DESIGN.md`) and
ADR-024, once Communication Contract Design itself is scoped to resolve
exactly the open items Section 10 names — not to revisit Sections 1–7's
own settled conceptual boundary, and not to design speech, wake word,
Android, notifications, or any conversation/LLM behaviour Section 8
excludes.**

This document gives "Communication Channel" a settled definition
(Section 1), direction model (Section 2), minimum message shape (Section
3), routing (Section 4, including an honest account of why a thin,
`TaskProposalIntake`-shaped reception boundary is unavoidable and why it
is not a new peer runtime subsystem), trust boundary (Section 5), first
implementation target (Section 6), module relationship (Section 7),
explicit out-of-scope list (Section 8), and ordered future phases (Section
9) — every question `PARKER_ENGINEERING_STANDARD.md` Stage 1
(Architecture) is meant to answer before Stage 2A (Contract Design)
begins. No Kotlin was written, no Contract Design was begun, and no
channel was implemented by this document.
