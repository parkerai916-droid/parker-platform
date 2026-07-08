# ADR-025 – Response Delivery Content Carrier

## Status
Accepted

## Context

`docs/architecture/COMMUNICATION_CONTRACT_DESIGN.md` Section 7 (Response
Delivery) already specifies the entire outbound mechanism at the concept
level: a Communication Channel declares an ordinary "deliver"
`ToolDescriptor`; whatever Runtime component holds a ready
`OutboundParkerResponse` constructs an ordinary `ExecutionRequest`
targeting that Tool and submits it through `ExecutionPipeline.submit`,
evaluated by `PermissionEngine.evaluate` exactly like any other request,
executed via `Tool.execute` exactly like any other Tool. That same
section discloses one unresolved tension, restated in its own words:
"`ExecutionRequest`... has no field shaped for arbitrary payload content
— only `intent: String` (a short description of the request, not its
content) and `metadata: Map<String, String>`. A response's actual `text`
therefore has nowhere to travel except `metadata`... which this
document's own... language... describe[s] as a 'non-authoritative
extension point' — an awkward fit for something as load-bearing as the
actual message being delivered." Section 14 names this as an open item
that "must be resolved — either by accepting `metadata` as sufficient
after all, or by a future, separately-authorised `ExecutionRequest`
revision — before a real 'deliver' `Tool` is implemented against it."

`IMPLEMENTATION_GAPS.md` #53 records this same tension as one of its two
remaining closure paths, and `LOCAL_TEXT_CHANNEL_CONTRACT_DESIGN.md`
independently confirms it is still live: that document's own Contract
Minimalism Review explicitly declined to register even a placeholder
"deliver" `ToolDescriptor` for the Local Text Channel, reasoning that
doing so "given `COMMUNICATION_CONTRACT_DESIGN.md` Section 7's own
disclosed tension... would misrepresent this module's actual, current
capability."

The Sprint 7 Governance Review (accepted) identified Response Delivery as
the highest-value remaining implementation path, precisely because every
other component it depends on already exists and is already tested:
Communication Runtime, Conversation Engine, Reasoning Provider,
Communication → Conversation wiring, Execution Pipeline, Permission
Engine, and Tool Registry. This tension is the one remaining architectural
blocker standing in front of it. This ADR resolves it.

Per this ADR's own instructions, exactly two options are evaluated, no
third is invented:

- **Option A.** Extend `ExecutionRequest` with a dedicated content field.
- **Option B.** Treat `ExecutionRequest.metadata` as sufficient for a
  first implementation.

## Decision

**Option B is adopted. `ExecutionRequest` is not modified.
`ExecutionRequest.metadata` carries a Response Delivery Tool's response
text for a first implementation.**

No field is added to, removed from, or retyped on `ExecutionRequest`. No
existing caller of `ExecutionRequest` — `AgentRunCommand`, `PlanDecision`,
`DefaultExecutionPipeline`, `ToolInvocationBinding`, or any test —
requires any change as a result of this ADR. A future "deliver" Tool
implementation reads its response text from a well-known metadata key
(the exact key name is an Implementation Decision, not an architectural
one, and is not fixed by this ADR).

## Reasoning

**Constitutional consistency.** Neither option touches the Constitution's
authority chain. Response Delivery, under both options, remains an
ordinary `ExecutionRequest` evaluated by `PermissionEngine.evaluate` and
executed only through `ExecutionPipeline` — "Cognition proposes. Trust
authorises. Runtime executes," with no shortcut for a response's content
living in one field rather than another. Option B changes where a string
is read from; it changes nothing about who is trusted to send it, or what
authorises the sending.

**Trust boundary.** Identical under both options. `metadata`'s established
meaning across this codebase (`ParkerEvent.metadata`, `ToolDescriptor`'s
optional fields, `InboundOwnerMessage.metadata`, `ExecutionRequest.metadata`
itself) is "non-authoritative" in one specific, narrow sense: no trust,
permission, or planning decision may be made by inspecting it. That
constraint is about Parker's own control flow, not about what a Tool's own
business logic may read once a request has already been authorised.
`PermissionEngine.evaluate` does not, and under this ADR still does not,
read `metadata` to decide anything — it evaluates the request's
`principalId`, `proposedActions`, and `targetResources`, exactly as it
already does. Carrying response text in `metadata` does not create a
second, informal trust channel; it is read only after authorisation has
already occurred, by the one Tool the request already named as its
target.

**Execution boundary.** No new invocation path is introduced under either
option. `Tool.execute(request: ExecutionRequest): ToolResult` is
`ExecutionPipeline`'s only invocation signature — confirmed by
`src/interfaces/Tool.kt`, which has no separate "parameters" object
distinct from `ExecutionRequest` itself. Every Tool implementation that
will ever exist in this platform must already derive its own operating
data from `ExecutionRequest`'s existing fields, because no other field
exists to supply it. A "deliver" Tool reading its response text from
`metadata` is not a special case; it is the same pattern every future Tool
implementation is already required to follow.

**Architectural simplicity.** Option B is strictly simpler: zero new
fields, zero changed types, zero touched call sites. Option A requires
deciding a field name, a type, a default, a validation rule, and updating
every existing production and test construction site of `ExecutionRequest`
across every subsystem that builds one today (Agent Runtime, Planner
Runtime by way of any future submission path, and `DefaultExecutionPipeline`'s
own test fixtures) — none of which need the new field for their own,
already-working purposes.

**Implementation impact.** Option B has no implementation impact on
anything outside a future Response Delivery unit's own new code. Option A
has implementation impact on every existing `ExecutionRequest` construction
site in the repository, none of which is part of Response Delivery.

**Blast radius.** This is the decisive difference. `ExecutionRequest` is
one of ADR-016's five named core contracts and is, per ADR-017, canonical
for "any proposed work that may cause execution, resource access, state
mutation, or external side effects" — every `RequestOrigin`
(`VOICE`, `TEXT`, `SCHEDULED_TASK`, `AGENT`, `PLUGIN`,
`HOME_ASSISTANT_EVENT`, `ANDROID_EVENT`, `REMOTE_INTERFACE`) shares this one
type. A field added for Response Delivery's benefit would exist on every
`ExecutionRequest` ever constructed by any of those seven other origins,
none of which has any stated need for it today. That is exactly the shape
of change PES-001's Core Engineering Principle warns against implementing
speculatively, and exactly the shape of decision this platform's own
"100,000-line test" discipline (`PRE_MODULE_ID_MULTIPLICITY_DECISION.md`)
already rejected once, for the identical reason: no real consumer exists
yet to validate the wider shape against. Option B's blast radius is zero.

**Future extensibility.** Option B does not foreclose Option A. If a real,
demonstrated need later emerges — multiple distinct consumers needing
structured (not merely string) content, or metadata's untyped-map shape
causing genuine implementation pain once a concrete Tool exists to
measure it against — a future ADR may revisit this decision with real
evidence in hand, per this ADR's own "Permanence" statement below.
Choosing Option A now, before any concrete Tool exists, would be deciding
the wider shape without a real consumer to validate it against — the
opposite of extensibility; it would be premature commitment.

**Compatibility with ADR-016 (Core Contracts).** Fully compatible under
both options; ADR-016 names `ExecutionRequest` as one of five core
contracts but does not fix its field list. Option B leaves all five core
contracts exactly as ADR-016 left them. Option A would still be
compatible with ADR-016 in principle (an additive field does not remove
`ExecutionRequest` from the five), but is unnecessary to remain compatible.

**Compatibility with ADR-017 (ExecutionRequest Is Canonical).** Option B
directly reinforces ADR-017: Response Delivery becomes an ordinary
`ExecutionRequest`, with no parallel or bespoke request type invented for
it — exactly what ADR-017's "no subsystem may invent a parallel execution
request type" requires. Option A is also compatible with ADR-017 (it does
not create a parallel type), but Option B satisfies the same rule with a
smaller change.

**Compatibility with ADR-018 (Immutability After Validation).** Neither
option is affected by ADR-018. `ExecutionRequest` remains a `val`-only
data class either way; a response's text is fixed at construction time,
before submission, under both options, exactly as every other field
already is.

**Compatibility with the existing `ExecutionPipeline`.** `DefaultExecutionPipeline`
requires no change under Option B — it already carries `metadata` through
unmodified to `Tool.execute`. Option A would require no change to
`DefaultExecutionPipeline`'s own logic either, since it already passes the
whole `ExecutionRequest` through, but would still require every one of its
existing test fixtures that construct an `ExecutionRequest` to be
reviewed against the new field's default.

**Compatibility with the Local Text Channel.** Directly confirms Option
B's readiness: `LOCAL_TEXT_CHANNEL_CONTRACT_DESIGN.md` already anticipated
exactly this resolution path by deliberately not registering a
placeholder "deliver" Tool until this tension was resolved. Once a future
Response Delivery unit is authorised, that channel's own Contract Design
requires no revision to accommodate Option B — it already reuses
`ModuleDescriptor.toolsExposed`/`ToolRegistry` unchanged, and Option B
requires no addition to either.

## Why Option A Was Rejected

Option A is not rejected because it is architecturally wrong — a
dedicated content field would, in isolation, be a reasonable, honest
representation of what a response's text actually is: load-bearing
content, not an extension point. It is rejected because of what it would
cost relative to what is actually needed right now: a Level 3
Architectural Change, under PES-001 Chapter 4, to one of the platform's
five canonical core contracts, undertaken for the benefit of exactly one
concrete, not-yet-built consumer, with no other of `ExecutionRequest`'s
seven existing origins asking for it. That is precisely the pattern this
platform has already named and rejected once before, for the identical
reason, in `PRE_MODULE_ID_MULTIPLICITY_DECISION.md`: inventing a wider
shape "with no real consumer to validate it against." Choosing Option A
now would also require a full PES-001 Level 3 workflow — Architecture,
Architecture Review, Contract Design, Specification, full engineering
workflow — before Response Delivery could even begin, which is a
disproportionate cost for a question Option B answers with zero blast
radius today.

The one real cost of Option B — that `metadata`'s established
"non-authoritative" meaning sits in some tension with using it to carry
something as load-bearing as response text — is acknowledged, not
dismissed, and is addressed directly above (Trust Boundary, Execution
Boundary): that established meaning governs Parker's own trust and
control-flow decisions, which never inspect `metadata`, not what a
specific, already-authorised Tool's own business logic may read from the
one request already addressed to it.

## Permanence

**This decision is intentionally provisional, not permanent.** It resolves
what Response Delivery needs for its first implementation, using the
platform's existing shape, with zero blast radius. It does not claim
`metadata` is the correct long-term home for response content for all
future Communication Channels, richer response types (structured content,
attachments, multi-part messages), or any other future consumer this ADR
does not anticipate.

**A future ADR may revisit this decision.** The trigger for doing so is
not the mere passage of time or a hypothetical future feature — consistent
with this platform's own discipline against optimising for hypothetical
capabilities — but a real, demonstrated need: a second or third concrete
consumer of the same content-carrying problem, a concrete Tool
implementation whose correctness or clarity is genuinely impaired by
`metadata`'s untyped shape, or a structured (non-`String`) content
requirement no reasonable metadata encoding can express. Until one of
those is real and observed, Option A remains deferred, not designed.

## Relationship to Gap #53

**This ADR does not close `IMPLEMENTATION_GAPS.md` #53.** It resolves one
of the two remaining prerequisites that gap's closure path (a) names — the
`ExecutionRequest` content-carrying question — but implements nothing.
Response Delivery itself still requires a small Contract Design addendum
(naming the component that turns a `Reply` into a delivered
`OutboundParkerResponse`, and the specific metadata key convention) and a
Stage 3 Implementation Plan before any Kotlin may be written. Gap #53's
own text, and its "Recommended closure" language, should be read as
updated by whoever next touches that document to reflect that this
specific sub-question is now settled — that update is explicitly not
performed by this ADR, per this unit's own instruction not to modify
`IMPLEMENTATION_GAPS.md`.

## Consequences

No Kotlin, test, schema, or specification file is changed by this ADR.
`ExecutionRequest` (`src/contracts/ExecutionRequest.kt` /
`src/interfaces/ExecutionRequest.kt`), `docs/schemas/ExecutionRequest.schema.json`,
and every existing caller remain exactly as they are today. A future
Response Delivery Contract Design and Implementation Plan may proceed
against a settled answer to "where does the text go" rather than
re-litigating it. No other document is modified by this ADR.

## Future Considerations

- The specific metadata key a "deliver" Tool implementation reads (e.g.
  `metadata["responseText"]`) is not fixed here — it is a Stage 4
  Implementation Decision for whichever unit implements Response Delivery,
  not an architectural constraint this ADR imposes.
- If a future Response Delivery unit finds it needs more than one string
  of content (e.g. a structured attachment reference, a rendering hint
  beyond what `OutboundParkerResponse.metadata` already offers), that unit
  should raise the question of whether Option A's trigger condition
  (Permanence, above) has been met, rather than silently working around
  `metadata`'s shape with ad hoc key-encoding conventions.
- This ADR's reasoning — that a canonical, cross-origin core contract
  should not be widened for the benefit of one not-yet-built consumer —
  is a restatement of the same discipline `PRE_MODULE_ID_MULTIPLICITY_DECISION.md`
  already applied to `AgentRunId`/`TaskProposalId` multiplicity, and is
  offered as a reusable precedent for any future core-contract extension
  question, not only this one.
