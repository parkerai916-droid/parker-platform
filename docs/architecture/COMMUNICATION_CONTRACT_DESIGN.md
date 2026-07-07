# Communication Contract Design

## Status

Design proposal, not yet reviewed or accepted. **This document is
contract design only.** No Kotlin is implemented, proposed as a diff, or
changed by it — every shape below is described in prose, not as a
`kotlin`-fenced signature block. Neither `src/` nor `tests/` is touched.
No voice, Android, wake word, notification, LLM prompt design, or
conversation memory concept is introduced.

### Why this unit exists

`docs/architecture/COMMUNICATION_CHANNEL_ARCHITECTURE.md` defined what a
Communication Channel is, the direction and minimum shape of a message,
the routing path, the trust boundary, the first implementation target,
and the module relationship — deliberately stopping short of any
field-level shape (its own Section 8, "Out of Scope": "Any Kotlin
interface, type, class name, or package layout for a Communication
Channel, a Communication Message, or Parker Runtime intake"). This
document performs the field-level design pass that document deferred, and
resolves the open items its own Section 10 (Engineering Review) named, so
a future implementation unit builds the Communication Runtime by
implementing an already-approved contract set — exactly the same
relationship `MODULE_CONTRACT_DESIGN.md` bears to
`MODULE_FRAMEWORK_ARCHITECTURE.md`.

## Review

Reviewed, in authority order:

1. `docs/architecture/parker-constitution.md`.
2. `docs/architecture/PARKER_ENGINEERING_STANDARD.md` (PES-001).
3. `docs/architecture/COMMUNICATION_CHANNEL_ARCHITECTURE.md` — the
   architecture this document implements as contracts and does not
   redefine.
4. `docs/architecture/MODULE_FRAMEWORK_ARCHITECTURE.md` and
   `docs/architecture/MODULE_CONTRACT_DESIGN.md` — a Communication
   Channel is a module; every contract below builds on, and does not
   duplicate, `ModuleId`, `ModuleDescriptor`, `ModuleRegistry`,
   `ToolDescriptor`, and `ToolRegistry`.
5. `docs/adr/ADR-024-module-event-audit-durability-boundary.md` — the
   module-access boundary and the "events are observability only, never
   authorisation to act" rule this document's Section 6 depends on.
6. `docs/architecture/ARCHITECTURE_V2_FROZEN_BASELINE.md` — the six
   frozen runtime subsystems this document builds on top of, not into.
7. `src/contracts/TaskProposal.kt` (`TaskProposalIntake`,
   `TaskProposalDisposition`) — the direct, already-accepted structural
   precedent `COMMUNICATION_CHANNEL_ARCHITECTURE.md` Section 4 itself
   names for the shape of an intake boundary.
8. `src/contracts/ExecutionRequest.kt`, `src/contracts/PlanDecision.kt`
   (`PlanningRequest`) — both already carry `RequestOrigin.TEXT`/`.VOICE`
   and a plain-`String` `correlationId`, confirming the existing contract
   surface already anticipated text/voice-originated requests without
   needing to be changed.
9. `src/interfaces/Tool.kt`, `src/interfaces/ExecutionPipeline.kt` — the
   existing invocation path Response Delivery (Section 7) reuses
   unmodified.

---

## Constitutional Boundaries

Restated up front, identical in substance to
`COMMUNICATION_CHANNEL_ARCHITECTURE.md` Section 1 and Section 5, not
re-derived differently here:

- **A Communication Channel carries a message. It never decides what the
  message means, whether it warrants an action, or what Parker should do
  about it.** Interpretation is Cognition's job, entirely out of this
  document's scope.
- **A Communication Channel is an ordinary module, and therefore an
  ordinary Principal.** No contract below grants it implicit trust,
  self-approval, or a bypass of Identity/Permission evaluation.
- **An inbound message must be capable of leading to real action once
  interpreted — and `EventBus` publication is therefore never an
  acceptable delivery path for it.** `EventBus` events are
  observability-only and may never be treated as authorisation to act
  (ADR-023 Rule 4; ADR-024 Section A, Rule 2). Every contract below keeps
  inbound delivery on a path that is *capable* of leading to action
  through the ordinary Cognition → Trust → Runtime chain, never through a
  broadcast channel that categorically cannot.
- **Cognition proposes. Trust authorises. Runtime executes — with no
  shortcut for messages that arrived via a Communication Channel.** Any
  action an inbound message eventually causes is still an ordinary
  `ExecutionRequest`, evaluated by `PermissionEngine.evaluate`, executed
  only through `ExecutionPipeline`.

---

## Contract Minimalism Review — Summary

| Candidate | Determination |
| --- | --- |
| `CorrelationId` | **Include.** Minted by the Communication Channel itself; the one genuinely new identifier this document introduces. |
| `InboundOwnerMessage` | **Include.** The inbound message, field-shaped. |
| `OutboundParkerResponse` | **Include.** Kept distinct from `InboundOwnerMessage`, not a single direction-flagged type — see Section 3. |
| `CommunicationIntakeDisposition` | **Include.** A minimal, two-variant, structural-only outcome — mirrors `TaskProposalDisposition`'s shape, not its five-way semantic richness. |
| `CommunicationIntake` | **Include.** The single public interface for inbound reception, one operation, mirroring `TaskProposalIntake` exactly. |
| A separate `ChannelId` type | **Exclude — reuse `ModuleId`.** A Communication Channel is a module (one channel, one `ModuleId`); no concrete need for a second, parallel identifier requiring its own uniqueness/synchronisation has been identified. |
| A combined `CommunicationMessage` with a direction flag | **Exclude.** `InboundOwnerMessage`/`OutboundParkerResponse` are kept as two distinct types, mirroring `CandidateMemory`/`MemoryRecord` and `WorldObservation`/`WorldBelief`'s identical precedent of keeping semantically distinct lifecycle stages structurally distinct even when their field lists look similar. |
| A `CognitionIntake`/`ConversationEngine` interface | **Exclude.** Out of this document's scope (Section 11) — Cognition and Chapter 19's Conversation Engine remain conceptual; this document defines only the boundary up to an accepted `InboundOwnerMessage`, not what consumes one. |
| A new `ResourceType` or `PermissionAction` value for communication | **Exclude.** `ResourceType.TOOL` (already used for every module-exposed Tool, per Unit M1) and `PermissionAction.NOTIFY` (already defined) are sufficient; no new vocabulary value is required. |
| A required `communication.*` `EventBus` event | **Exclude — optional, deferred.** `COMMUNICATION_CHANNEL_ARCHITECTURE.md` Section 3's own parenthetical already frames this as optional and additive, never a substitute for the delivery path; not required for a first implementation. |
| A channel-kind enumeration (text/voice/notification) | **Exclude for now.** No operation defined here needs to branch on it; if a future channel's behaviour genuinely requires distinguishing kinds, add it additively to `ModuleDescriptor.metadata` or a future manifest field, not invented here without a concrete consumer. |
| Extending `ExecutionRequest` with a dedicated payload/content field | **Not decided here — flagged, not solved.** See Section 7's own disclosed tension. |

Net result: **five required contracts** (`CorrelationId`,
`InboundOwnerMessage`, `OutboundParkerResponse`,
`CommunicationIntakeDisposition`, `CommunicationIntake`), all remaining
concepts resolved by reusing existing contracts unchanged, and one
genuine open tension disclosed rather than papered over (Section 7).

---

## 1. Communication Channel Identity

**A Communication Channel's identity is its `ModuleId`. No separate
`ChannelId` type is introduced.**

A Communication Channel is a module (`COMMUNICATION_CHANNEL_ARCHITECTURE.md`
Section 1, Section 7) — specifically the **adapter** flavour of capability
`MODULE_FRAMEWORK_ARCHITECTURE.md` Section 2 already names, pointed at the
owner instead of a third-party service. Nothing in the parent
architecture document, or in the first implementation target (a single
local text channel, Section 6 of that document), requires one module to
host more than one simultaneously-active channel identity. Reusing
`ModuleId` directly — the identity `ModuleRegistry` already tracks through
Registered → Enabled → Disabled → Removed — avoids a second identifier
that would need to stay synchronised with the first for no concrete
benefit. **"Channel id," wherever it appears on a Communication Message
(Section 2, Section 3), is a plain `ModuleId` value.**

If a future channel genuinely needs to expose more than one independent
communication surface (e.g. one module offering both a text and a
notification-only surface), that is a concrete need a future Contract
Design revision should address when it actually arises — not invented
speculatively here, consistent with this repository's own "100,000-line
test" discipline (`docs/reviews/SPRINT_4_ARCHITECTURE_ACTIONS.md`).

## 2. Inbound Owner Message

**`InboundOwnerMessage`** — required fields, per
`COMMUNICATION_CHANNEL_ARCHITECTURE.md` Section 3:

- **`channelId: ModuleId`** — Section 1, above.
- **`senderPrincipalId: PrincipalId`** — the owner's own `Principal`
  (Section 5, below) — never the channel's own identity.
- **`text: String`**, non-blank — the message content. This document does
  not require every channel to originate text directly; a future speech
  channel's audio must already have become text by the time it reaches
  this boundary (`COMMUNICATION_CHANNEL_ARCHITECTURE.md` Section 3,
  Section 9's "speech input" future phase).
- **`timestamp: Instant`** — when the owner sent the message, not when it
  happened to be processed.
- **`correlationId: CorrelationId`** — Section 4, below. Present on the
  inbound message itself, minted by the channel before submission — not
  minted by `CommunicationIntake`.
- **`metadata: Map<String, String>`**, defaulted to empty — a
  channel-specific, non-authoritative extension point, mirroring
  `ParkerEvent.metadata`/`ToolDescriptor`'s identical existing precedent.
  Nothing this document, `CommunicationIntake` (Section 6), or any
  downstream consumer relies on for a trust or planning decision may live
  only here.

**Lifecycle.** Constructed once, by the Communication Channel, from
whatever the owner said; submitted whole to
`CommunicationIntake.submitInboundMessage` (Section 6); not retained,
mutated, or re-submitted by the channel once submitted.

## 3. Outbound Parker Response

**`OutboundParkerResponse`** — the same conceptual field list as
`InboundOwnerMessage`, kept as a **distinct type**, not a shared
direction-flagged one (see the Minimalism Review's own reasoning: this
mirrors `CandidateMemory`/`MemoryRecord` and
`WorldObservation`/`WorldBelief` keeping structurally-similar but
semantically-distinct lifecycle stages as separate types, precisely so a
future implementation cannot accidentally deliver an inbound message back
out unchanged, or treat an outbound response as if it still carried the
owner's own authority):

- **`channelId: ModuleId`** — which channel to deliver through; normally
  copied from the `InboundOwnerMessage` this response answers.
- **`senderPrincipalId: PrincipalId`** — **not** the channel's own
  identity. The `Principal` responsible for the response's content —
  whatever Runtime component produced it (Section 9). Mirrors
  `InMemoryPlannerRuntime`/`InMemoryAgentRuntime`'s own established
  discipline of resolving and threading a real, resolved
  `Principal.principalId` rather than a hardcoded constant (the exact
  defect gap #49 fixed for Planner Runtime).
- **`text: String`**, non-blank — the response content.
- **`timestamp: Instant`** — when the response was produced.
- **`correlationId: CorrelationId`** — copied unchanged from the
  `InboundOwnerMessage` that prompted this response, so the channel can
  pair a reply with the message that elicited it.
- **`metadata: Map<String, String>`**, defaulted to empty — same
  non-authoritative extension-point discipline as Section 2.

**Lifecycle.** Constructed once, by whatever Runtime component produced
the response content; delivered through Response Delivery (Section 7);
not retained by this contract set itself.

## 4. Correlation Identifier

**`CorrelationId`** — a single, non-blank string value, matching
`PrincipalId`/`ModuleId`/`RequestId`'s identical established shape.

**Who mints it, and when — the open item
`COMMUNICATION_CHANNEL_ARCHITECTURE.md` Section 10 named, now settled:**
the **Communication Channel** mints a `CorrelationId` at the moment it
constructs an `InboundOwnerMessage` from whatever the owner just said —
before ever calling `CommunicationIntake`. `CommunicationIntake` trusts
the `CorrelationId` already present on the message; it does not mint or
overwrite one (Section 6). Whatever Runtime component eventually produces
a response threads the same `CorrelationId` value onto its
`OutboundParkerResponse` (Section 3) unchanged.

This mirrors, rather than invents, this platform's existing identifier
discipline: `RequestId`/`DecisionId`/`ResultId` are likewise plain,
blank-rejecting wrappers with no mandated generation algorithm — this
document similarly does not mandate whether a channel mints a
`CorrelationId` deterministically or via a random/UUID scheme, only that
it does so once, at first receipt, and carries it through unchanged
afterward.

**Threading beyond this contract set.** When Cognition constructs a
`PlanningRequest` in response to an inbound message,
`PlanningRequest.correlationId` (already a plain `String`, unchanged by
this document) should be populated from
`InboundOwnerMessage.correlationId.value` — `PlanningRequest` needs no
modification to support this; its existing shape already anticipated a
text-originated correlation id (`PlanningRequest.source` already defaults
to `RequestOrigin.TEXT`).

## 5. Sender Principal

No new type. `PrincipalId`/`Principal` (`IdentityService`) are reused
directly, for both directions, with different practical meaning per
direction (Section 2, Section 3):

- **Inbound.** `senderPrincipalId` is the owner's own, already-registered
  `Principal` — resolved and checked by `CommunicationIntake` before
  acceptance (Section 6). This document does not require the resolved
  Principal's `principalType` to be `USER` specifically, or its `status`
  to be `ACTIVE` specifically — consistent with this platform's existing
  precedent elsewhere (gap #37: `resolve()` does not itself suppress
  non-Active Principals; that remains a caller decision this document
  does not change).
- **Outbound.** `senderPrincipalId` is whichever `Principal` is
  responsible for the response's content — never the channel's own
  identity. This document does not decide which specific `Principal` that
  is (a `SYSTEM`-type "Parker" identity, or the identity of whichever
  Runtime component authored the response) — that is Cognition's own
  concern (Section 9), out of scope here. What this document does require
  is that it be a real, resolved `PrincipalId`, threaded through
  explicitly, never a hardcoded constant — mirroring exactly the
  discipline `InMemoryPlannerRuntime`'s gap #49 fix already established
  for Planner Runtime's own publisher identity.

**The channel's own identity (its `ModuleId`) is never the value of
`senderPrincipalId` on any message it carries.** The channel is
transport, not the party responsible for a message's content, in either
direction.

## 6. Communication Intake Interface

**`CommunicationIntake`** — the single public interface for inbound
reception, mirroring `TaskProposalIntake`'s exact shape and minimalism
(one method, one payload type, one outcome type):

- **One operation:** given an `InboundOwnerMessage`, perform exactly two
  structural checks — nothing more, nothing interpretive — and return a
  `CommunicationIntakeDisposition`:
  1. **Is `message.channelId` a currently `ENABLED` module?** — checked
     against `ModuleRegistry.getModuleStatus` (Unit M1's own, already-
     implemented lookup). A message from an unregistered, `DISABLED`, or
     `REMOVED` channel is rejected.
  2. **Does `message.senderPrincipalId` resolve to a registered
     `Principal`?** — checked against `IdentityService.resolve`, mirroring
     `InMemoryPlannerRuntime`/`InMemoryAgentRuntime`'s own established
     "resolve before acting" precedent. An unresolvable sender is
     rejected.
  If both checks pass, the message is accepted.
- **What `CommunicationIntake` explicitly does not do:** it does not
  interpret `message.text`, does not decide whether the message implies
  an action, does not construct a `PlanningRequest` or an
  `ExecutionRequest`, and does not itself call `PlannerRuntime`,
  `AgentRuntime`, `MemoryStore`, or `WorldModel` (Section 9). It performs
  exactly the two structural checks named above — the same kind of
  precondition-only gating `InMemoryPlannerRuntime.plan` and
  `InMemoryAgentRuntime.start` already perform before publishing or
  acting, never a semantic decision about content.
- **What happens to an accepted message next is not decided by this
  document.** `CommunicationIntake` makes an accepted `InboundOwnerMessage`
  available; the mechanism by which Cognition subsequently consumes it
  (a callback, a queue, a direct call into a not-yet-specified Cognition
  intake contract) depends on a Cognition-side contract this repository
  does not yet have — Chapter 19's Conversation Engine remains a
  conceptual stub (`COMMUNICATION_CHANNEL_ARCHITECTURE.md` Section 8).
  This is a genuine, disclosed open item for a future Contract Design
  pass (Section 14), not solved here, and does not block implementing
  `CommunicationIntake`'s own defined surface (Section 12, Conclusion).

**Dependencies.** `CommunicationIntake`'s only two collaborators are
`ModuleRegistry` (channel-status check) and `IdentityService`
(sender-resolution check) — no dependency on `ToolRegistry`,
`ExecutionPipeline`, `PlannerRuntime`, `AgentRuntime`, `MemoryStore`, or
`WorldModel` is introduced (Section 9).

**Why a sealed disposition, not a throw.** `TaskProposalIntake` itself
returns a sealed `TaskProposalDisposition`, and `CommunicationIntake`
mirrors that exact choice (rather than the throw-based pattern
`ModuleRegistry`/`IdentityService` use for their own operations) because
rejection here is an expected, routine outcome of ordinary channel
lifecycle timing (a channel briefly disabled during maintenance, a
just-removed Principal), not a caller-misuse condition — the same
distinction `PlanningSessionResult.Failed` already draws for Planner
Runtime's own safe-failure path.

## 7. Response Delivery

**No new public contract is required for outbound delivery.** It is
fully expressible through the Module Framework's existing tool-exposure
model (`MODULE_CONTRACT_DESIGN.md` Section 2, Section 7; Unit M1's own
`InMemoryModuleRegistry` implementation), reached through
`ExecutionPipeline`/`ToolRegistry` exactly like any other Tool
invocation:

- A Communication Channel declares an ordinary "deliver" `ToolDescriptor`
  in its `ModuleDescriptor.toolsExposed` — no new descriptor shape, no new
  `ResourceType` (`ResourceType.TOOL` already applies, per Unit M1's own
  Resource-per-exposed-Tool wiring), and `PermissionAction.NOTIFY`
  (already defined) is a natural, though not mandated, candidate action
  for it to declare support for.
- Whatever Runtime component holds a ready `OutboundParkerResponse`
  constructs an ordinary `ExecutionRequest` targeting that Tool
  (`principalId` = the response's own `senderPrincipalId`; `origin` =
  `RequestOrigin.TEXT` or `.VOICE` as fits the channel; `targetResources`
  = the deliver Tool's own backing `ResourceId`; `correlationId` =
  `response.correlationId.value`) and submits it through
  `ExecutionPipeline.submit`, evaluated by `PermissionEngine.evaluate`
  exactly like any other request, executed via `Tool.execute` exactly
  like any other Tool.

**Disclosed, unresolved tension — flagged, not solved here.**
`ExecutionRequest` (`src/contracts/ExecutionRequest.kt`) has no field
shaped for arbitrary payload content — only `intent: String` (a short
description of the request, not its content) and
`metadata: Map<String, String>`. A response's actual `text` therefore has
nowhere to travel except `metadata` (e.g. `metadata["responseText"]`),
which this document's own Section 2/Section 3 language, and
`COMMUNICATION_CHANNEL_ARCHITECTURE.md` Section 3, both describe as a
"non-authoritative extension point" — an awkward fit for something as
load-bearing as the actual message being delivered. Modifying
`ExecutionRequest` itself is a Volume 1 core contract change, outside
this document's authoritative sources and outside this Unit's scope.
**This is named as an open item for Section 14 (Engineering Review) and
must be resolved — either by accepting `metadata` as sufficient after
all, or by a future, separately-authorised `ExecutionRequest` revision —
before a real "deliver" `Tool` is implemented against it.** It does not
block defining or implementing `CommunicationIntake` (Section 6), which
does not depend on it.

## 8. Trust Boundary

Restated, unchanged in substance from
`COMMUNICATION_CHANNEL_ARCHITECTURE.md` Section 5, and made concrete
against the contracts above:

- `CommunicationIntake`'s two structural checks (Section 6) are
  **preconditions, not permission decisions.** Passing them means a
  message is well-formed enough to be handed to Cognition — it is not, and
  must never be read as, an approval of any action the message might
  eventually be interpreted to request.
- **No contract in this document grants a Communication Channel, or any
  Principal acting through one, a permission, implicit trust, or a
  private execution path.** Every action that eventually results from an
  inbound message is still proposed by Cognition, authorised by
  `PermissionEngine.evaluate`, and executed only through
  `ExecutionPipeline` — **Cognition proposes. Trust authorises. Runtime
  executes** — with no exception carved out for having arrived via a
  Communication Channel.
- Response Delivery (Section 7) is itself subject to exactly this chain:
  delivering a response is an ordinary, permission-evaluated Tool
  invocation, not a trusted side channel a Communication Channel or
  whatever produced the response can reach unilaterally.

## 9. Relationship to Planner / Agent / Memory / World Model

**`CommunicationIntake` has no dependency on, and never calls,
`PlannerRuntime`, `AgentRuntime`, `MemoryStore`, or `WorldModel`.** Its
only collaborators are `ModuleRegistry` and `IdentityService` (Section 6).

Once an `InboundOwnerMessage` is accepted, engaging any of the four
frozen subsystems is Cognition's own decision to make, using exactly the
paths that already exist for any other proposer — no special-cased path
for messages that arrived via a Communication Channel:

- **Planner Runtime.** If Cognition decides an inbound message implies a
  goal worth planning, it constructs an ordinary `PlanningRequest` —
  `initiatingPrincipalId` = the message's own `senderPrincipalId` (the
  owner, not the channel), `correlationId` = the message's
  `correlationId.value`, `source` = `RequestOrigin.TEXT` (already the
  default) — and calls `PlannerRuntime.plan` exactly as any other caller
  would. `PlanningRequest`'s existing shape requires no change to support
  this (Section 4).
- **Agent Runtime.** Reached only downstream of a Task Proposal's own
  disposition, exactly as it already is today — no new path is introduced
  for Communication-Channel-originated Tasks.
- **Memory / World Model.** If a message is ever worth remembering, or
  describes something worth updating World Model's current belief about,
  that happens through their own existing `MemoryStore`/`WorldModel` read
  and write interfaces, called by whatever component already has the
  authority to call them (Cognition, or a future Conversation Engine) —
  never by the Communication Channel or `CommunicationIntake` directly
  (`COMMUNICATION_CHANNEL_ARCHITECTURE.md` Section 1's own "not Memory,
  not World Model" boundary, restated here at the contract level).

## 10. Module Relationship

Restated and made concrete against Unit M1's own, already-implemented
`InMemoryModuleRegistry`:

- A Communication Channel **registers through `ModuleRegistry`**
  (`ModuleRegistry.register`, `MODULE_CONTRACT_DESIGN.md` Section 5) like
  any other module — declaring a `ModuleDescriptor` naming its exposed
  "deliver" Tool (Section 7), its required permissions, and its
  connectivity declaration (almost certainly `LOCAL_ONLY` for the first
  implementation target, Section 6 of the parent architecture document).
- It **moves through the same Registered → Enabled → Disabled → Removed
  lifecycle** every other module does — no special-cased lifecycle for
  communication.
- **`CommunicationIntake`'s own channel-status check (Section 6) is what
  operationalises "communication modules may carry messages only when
  authorised" at the intake boundary**: a channel must be `ENABLED` (an
  explicit, attributable decision by some Principal, per
  `MODULE_CONTRACT_DESIGN.md` Section 4/Section 5) before any inbound
  message it carries is accepted. A `REGISTERED`-but-not-yet-`ENABLED`, or
  a `DISABLED`, channel cannot inject messages into Parker.
- It **exposes tools only if authorised**, and **never bypasses Permission
  Engine or Execution Pipeline** — both already true by construction of
  the existing Module Framework (Section 7); this document introduces no
  exception.

## 11. Out of Scope

This document does not define, and a future, separately-scoped unit is
where each of the following belongs:

- Speech-to-text, text-to-speech, or wake word detection.
- Android services or any Android-specific integration.
- Notifications as a concrete mechanism (Chapter 25 remains a future
  phase, per `COMMUNICATION_CHANNEL_ARCHITECTURE.md` Section 9).
- Remote messaging, cloud chat, or any network transport.
- Home Assistant voice integration.
- LLM prompt design, or any detail of how Cognition interprets an inbound
  message or composes an outbound one.
- Conversation memory design — turn-taking, continuity, clarification, or
  follow-up handling (Chapter 19, Conversation Engine) remains a
  conceptual stub this document does not flesh out or give a Kotlin
  shape to.
- The concrete mechanism by which Cognition consumes an accepted
  `InboundOwnerMessage` from `CommunicationIntake` (Section 6's own
  disclosed open item).
- Any Kotlin implementation, package layout, or class name — this
  document names concepts and their field lists only.
- Any actual channel implementation, including the local text channel
  named as the first implementation target.
- Any modification to `ExecutionRequest`, `PlanningRequest`,
  `ModuleDescriptor`, `ToolDescriptor`, `ModuleRegistry`, or any other
  existing contract — every one is reused exactly as it already exists.

## 12. Public Contract Inventory

| Contract | New / Reused | Purpose |
| --- | --- | --- |
| `CorrelationId` | **New** | Ties an inbound message to its eventual response; minted by the channel (Section 4). |
| `InboundOwnerMessage` | **New** | The inbound message, field-shaped (Section 2). |
| `OutboundParkerResponse` | **New** | The outbound response, field-shaped, kept distinct from the inbound type (Section 3). |
| `CommunicationIntakeDisposition` | **New** | The two-variant (Accepted/Rejected), structural-only outcome of submitting an inbound message (Section 6). |
| `CommunicationIntake` | **New** | The single public interface for inbound reception (Section 6). |
| `ModuleId` | Reused | A channel's identity (Section 1) — no separate `ChannelId`. |
| `PrincipalId` / `IdentityService` | Reused | Sender identity, in both directions (Section 5). |
| `ModuleDescriptor` / `ModuleRegistry` | Reused | Channel registration and lifecycle (Section 10). |
| `ToolDescriptor` / `ToolRegistry` | Reused | The channel's exposed "deliver" capability (Section 7). |
| `ExecutionRequest` / `ExecutionPipeline` / `Tool` | Reused | Response delivery's actual invocation path (Section 7) — with one disclosed, unresolved content-carrying tension. |
| `PlanningRequest` / `PlannerRuntime` | Reused, unchanged | Where Cognition sends a message-derived goal, if any (Section 9). |

**Five new contracts, eleven reused unchanged, zero modified.**

## 13. Self-Traceability Review

| Contract | Authorised by (`COMMUNICATION_CHANNEL_ARCHITECTURE.md`) |
| --- | --- |
| `CorrelationId` | Section 3 ("Correlation id"); Section 10 ("How correlation ids are minted, and by whom") |
| `InboundOwnerMessage` | Section 2 ("Inbound owner message"); Section 3 (message shape) |
| `OutboundParkerResponse` | Section 2 ("Outbound Parker response"); Section 3 (message shape) |
| `CommunicationIntakeDisposition` | Section 4 ("Parker Runtime intake"), read together with its own "does not itself decide" boundary |
| `CommunicationIntake` | Section 4 (routing, and the `TaskProposalIntake`-precedent reasoning); Section 10 (its own concrete-shape open item) |
| Reuse of `ModuleId` (Section 1) | Section 7 ("Communication Channels are modules") |
| Reuse of `PrincipalId` (Section 5) | Section 3 ("Sender principal") |
| Reuse of `ModuleRegistry`/Tool exposure (Sections 7, 10) | Section 7 ("Module Relationship") |
| No `EventBus` for inbound delivery (Constitutional Boundaries) | Section 3's own `ParkerEvent`/`EventBus` exclusion |
| Trust Boundary (Section 8) | Section 5 ("Trust Boundary") |

No contract in this document introduces a concept
`COMMUNICATION_CHANNEL_ARCHITECTURE.md` did not already anticipate at the
architectural level. Every exclusion in the Minimalism Review is traceable
to that same document's own Section 8 (Out of Scope) or to a concrete
absence of need identified while performing this design pass.

---

## 14. Engineering Review

**Architectural consistency.** Every included contract traces to a named
`COMMUNICATION_CHANNEL_ARCHITECTURE.md` section (Section 13, above).
Nothing here introduces a concept that document did not already name at
the concept level, and nothing here reopens any of the six frozen runtime
subsystems (`ARCHITECTURE_V2_FROZEN_BASELINE.md`) — `CommunicationIntake`
is explicitly framed as a thin boundary depending only on `ModuleRegistry`
and `IdentityService`, never as a seventh peer subsystem.

**Model independence.** No contract in this document assumes a specific
reasoning or model implementation sits behind how an inbound message is
interpreted or a response composed (both remain entirely Cognition's
concern, out of scope here), consistent with AD-010.

**Minimalism.** Five new contracts, eleven existing contracts reused
unchanged, zero existing contracts modified (Section 12). A separate
`ChannelId`, a combined direction-flagged message type, a semantic
(rather than structural) intake disposition, a new `ResourceType`/
`PermissionAction`, a required `EventBus` event, and a channel-kind
enumeration were each considered and rejected for a stated, concrete
reason (Minimalism Review Summary).

**Traceability.** Every required contract's authorising section is named
in Section 13.

**Consistency with ADR-024 and ADR-023.** Section 6/Constitutional
Boundaries checks the "no `EventBus` for inbound delivery" decision
directly against ADR-023 Rule 4 and ADR-024 Section A Rule 2; no contract
here treats event receipt as authorisation to act, and none authorises a
Communication Channel to subscribe to anything (subscription remains
governed entirely by ADR-024 Section E's own module-subscription gate,
untouched by this document).

**Open items, disclosed, not blocking.** Two genuine open questions are
named rather than silently resolved:

1. **The mechanism by which Cognition consumes an accepted
   `InboundOwnerMessage` from `CommunicationIntake`** (Section 6) — left
   to a future Contract Design pass once Cognition/Conversation Engine
   itself is scoped, since no Kotlin contract for that layer exists yet
   anywhere in this repository.
2. **`ExecutionRequest`'s lack of a dedicated payload/content field**
   (Section 7) — response text currently has nowhere to travel except the
   non-authoritative `metadata` map when delivered as a Tool invocation;
   resolving this cleanly likely requires a Volume 1 core contract
   revision outside this document's authority.

Neither open item blocks implementing `CommunicationIntake`,
`InboundOwnerMessage`, `OutboundParkerResponse`, or
`CommunicationIntakeDisposition` themselves, nor registering a
Communication Channel module through the existing `ModuleRegistry` — both
are bounded, disclosed follow-ups affecting only what happens *after* an
accepted message leaves this contract set's own defined surface, or how a
response's *content specifically* rides inside an otherwise-unmodified
`ExecutionRequest`.

## Conclusion

**Communication Runtime implementation may begin, scoped exactly to what
this document defines: `CommunicationIntake` and its two structural
checks, `InboundOwnerMessage`/`OutboundParkerResponse`/`CorrelationId`/
`CommunicationIntakeDisposition`, and a first Communication Channel module
(the local text channel named in
`COMMUNICATION_CHANNEL_ARCHITECTURE.md` Section 6) registering through the
existing `ModuleRegistry` and exposing an ordinary "deliver" Tool through
the existing `ToolRegistry`/`ExecutionPipeline` path.**

Implementation must **not** attempt to resolve, in the same unit, either
open item Section 14 names (Cognition's consumption mechanism, or
`ExecutionRequest`'s content-carrying gap) — both remain explicitly
deferred, bounded follow-ups. A first implementation unit may reasonably
stop at: `CommunicationIntake` accepting or rejecting inbound messages and
making accepted ones inspectable (mirroring `InMemoryMemoryStore.wasForgotten`'s
"observability method outside the formal interface" precedent), with
actual Cognition consumption and actual Tool-based response delivery
following once their own respective open items are resolved by a future,
separately-scoped unit.
