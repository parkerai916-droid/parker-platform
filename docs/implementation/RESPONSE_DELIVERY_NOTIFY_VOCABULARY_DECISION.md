# Response Delivery — `PermissionAction.NOTIFY` Vocabulary Decision

Not a specification. Not a Contract Design. A pre-coding checklist,
resolving `RESPONSE_DELIVERY_CONTRACT_DESIGN.md`'s Stage 3 Blocking
Prerequisite 2, mirroring `SPRINT_2_B2_IMPLEMENTATION_DECISIONS.md`'s own
established format: every answer below is either already fixed by an
accepted document, or a direct read of `src/contracts/ActionMapping.kt`
and `src/runtime/ActionMapper.kt` as they exist today — nothing here is
invented beyond the one genuine choice (the `verbPhrase` string itself).

**1. Exact `verbPhrase` string?**
`"notify owner"`. Not fixed by any existing document — `COMMUNICATION_CONTRACT_DESIGN.md`
Section 7 names `PermissionAction.NOTIFY` as "the natural, though not
mandated, candidate" action but never proposes vocabulary text, and
`RESPONSE_DELIVERY_CONTRACT_DESIGN.md` Section 1 deliberately "does not
fix" the `proposedActions` string. This is the one genuine interpretive
choice this document makes. Distinct, deliberately, from `ResponseDelivery`'s
own `ExecutionRequest.intent` string (e.g. "deliver response",
`RESPONSE_DELIVERY_CONTRACT_DESIGN.md` Section 1 Step 2) — `intent` and
`proposedActions` are separate fields serving separate purposes
(`ExecutionRequest.kt`), and reusing the same text for both would blur
that distinction without benefit. `"notify owner"` was chosen over
alternatives ("deliver response", "send message") because it names the
*action being requested of the Permission Engine* — notifying the
channel's owner — rather than restating the mechanism (delivery), keeping
it a verb phrase in the same register as this file's own precedent
elsewhere in the vocabulary's intended shape (`action-mapping.md`'s
"Transformation Rules" examples are themselves short, action-first
phrases).

**2. Exact `ActionResourceMapping` set?**
A single mapping, not composite:
```
ActionVocabularyEntry(
    verbPhrase = "notify owner",
    mappings = setOf(
        ActionResourceMapping(
            action = PermissionAction.NOTIFY,
            resourceType = ResourceType.TOOL,
        ),
    ),
)
```
`resourceType = ResourceType.TOOL` because Decision 2
(`RESPONSE_DELIVERY_CONTRACT_DESIGN.md`) locates the channel's backing
Resource filtered to exactly that type, and `ActionMapper.mapOne` requires
`entry.mappings.filter { it.resourceType in targetResourceTypes }` to be
non-empty against the request's actual target Resource types
(`src/runtime/ActionMapper.kt`) — `ResponseDelivery`'s own
`targetResources` (Section 1) always resolves to a single `TOOL`-type
Resource, so no other `ResourceType` mapping is needed or correct.

**3. Is this a composite action (`action-mapping.md` "Composite Actions"),
needing more than one `ActionResourceMapping`?**
No. Response Delivery's own contract (`RESPONSE_DELIVERY_CONTRACT_DESIGN.md`
Section 1) submits exactly one `ExecutionRequest` addressed to exactly one
Resource, requesting exactly one action against it. There is no second
resource-type or second action this delivery attempt requires
authorisation for — unlike `action-mapping.md`'s own "move document to
archive" example (needing both `READ` and `DELETE`/`WRITE`), notifying a
channel's owner is a single, atomic permission question.

**4. Does `PermissionAction.NOTIFY` already exist as an enum value?**
Yes. Confirmed by direct read: `PermissionAction.NOTIFY` is already
defined in `src/contracts/Permission.kt`. This document adds no new
`PermissionAction` value — only the vocabulary entry that maps a proposed
action string onto the existing one.

**5. Where is this entry registered — a code location decision?**
**Not fixed by this document; it is the Implementation Plan's own Included
Work (`RESPONSE_DELIVERY_CONTRACT_DESIGN.md` Stage 3 Blocking Prerequisite
2's own wording: "as Included Work, registering one `ActionVocabularyEntry`... using [`ActionVocabulary.register`]
exactly as it already exists").** What is confirmed here, so the
Implementation Plan does not have to re-derive it: `InMemoryActionVocabulary`
holds zero registered entries anywhere in this repository today (confirmed
by direct read of `src/runtime/ActionMapper.kt` — the map starts empty,
and no call to `.register(...)` exists in any `src/` file). This will be
the **first** vocabulary entry ever registered in this codebase — there is
no existing registration call site to extend, and no composition
root/`main` entry point exists in this repository today to register it at
startup (confirmed: no `fun main(` anywhere under `src/`). The
Implementation Plan must therefore decide, as ordinary Stage 3 scope, where
the one `ActionVocabulary.register(...)` call is made (most likely: inside
whichever test/fixture assembles a `DefaultExecutionPipeline` for
`ResponseDelivery`'s own tests, mirroring how every existing Sprint 7 test
already assembles its own dependency graph by hand — `CommunicationConversationCoordinatorTest`,
`DefaultExecutionPipelineTest`, etc. — with an explicit note that a future,
real composition root must perform the identical call once, at startup,
when one is built).

**6. Any Permission Engine policy implications?**
None new. Registering this vocabulary entry only makes `"notify owner"`
resolvable to `PermissionAction.NOTIFY` against a `TOOL`-type Resource —
it does not itself grant, deny, or configure any `PermissionEngine` policy
for that action. Whatever policy governs `PermissionAction.NOTIFY` today
(or its absence) governs it identically after this entry is registered;
`DefaultExecutionPipeline.submit`'s existing action-mapping-then-permission-evaluate
sequence (`src/runtime/DefaultExecutionPipeline.kt`) is unchanged by this
document.

**7. Any ambiguities left?**
None, for Response Delivery's own narrow use. `action-mapping.md`'s own
"Future Extensibility" section leaves ambiguous-*mapping* tie-breaking
(two different entries claiming the same `verbPhrase`) as a general open
question — irrelevant here, since `"notify owner"` is being registered for
the first time, and `InMemoryActionVocabulary.register`'s own existing
idempotent-reregistration behaviour (`VocabularyRegistrationOutcome.AlreadyRegistered`
for an identical re-registration, `.Rejected` for a conflicting one)
already covers the case cleanly with no new rule required.

**Bottom line:** the entry's shape is fully settled —
`ActionVocabularyEntry(verbPhrase = "notify owner", mappings = setOf(ActionResourceMapping(PermissionAction.NOTIFY, ResourceType.TOOL)))`
— using only already-existing types (`ActionVocabularyEntry`,
`ActionResourceMapping`, `PermissionAction.NOTIFY`, `ResourceType.TOOL`),
registered via the already-existing `ActionVocabulary.register` method,
with no new Kotlin type and no `ActionMapper`/`ActionVocabulary` redesign.
Only the registration call site remains an ordinary Stage 3 Implementation
Plan decision, named here so it is not rediscovered mid-implementation.
