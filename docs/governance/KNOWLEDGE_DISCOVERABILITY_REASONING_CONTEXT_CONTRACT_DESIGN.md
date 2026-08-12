**Status:** Contract Design only. Governance and contract-definition document. No Kotlin is
implemented, proposed as a diff, or changed by this document. Neither `src/` nor `tests/` is
touched. This document resolves the three prerequisite questions and the eleven-item Decision
Register the Boundary Review fixed; it does not create a Scope Lock or Implementation Plan, does
not prescribe unit-by-unit implementation sequencing, does not reopen Gap #54, does not introduce
restart durability, embeddings, semantic search, a database, a remote service, or a new index, does
not expand Evidence Intelligence authority, and does not modify the Remember/promotion path. Every
Kotlin type and method signature below is a contract sketch for a later Scope Lock and
Implementation Plan to adopt, adapt, or reject -- none is applied to `src/` by this document.

# Knowledge Discoverability and Governed Retrieval into Reasoning Context — Contract Design

---

## 1. Status and Governing Inputs

Read fresh, in full, for this Contract Design:
`docs/governance/KNOWLEDGE_DISCOVERABILITY_REASONING_CONTEXT_PLANNING_REVIEW.md` ("the Planning
Review") and `docs/governance/KNOWLEDGE_DISCOVERABILITY_REASONING_CONTEXT_BOUNDARY_REVIEW.md`
("the Boundary Review"). Every contract below is fixed inside the ownership, information-flow,
principal-and-purpose, authority, lifecycle-and-failure, data-ownership, and retrieval-surface
boundaries the Boundary Review already fixed (its Sections 3-9) -- this document narrows those
boundaries into exact algorithms and types; it does not loosen, reinterpret, or relitigate any of
them.

Production source inspected fresh, in full or in every relevant section, to verify this design
against current reality: `src/runtime/DefaultKnowledgeRetrieval.kt`,
`src/runtime/DefaultReasoningContextAssembler.kt`, `src/runtime/KnowledgeItemPersistence.kt`,
`src/composition/PermissionFilteredMemoryRetrieval.kt`, `src/runtime/DefaultPermissionPolicy.kt`,
`src/contracts/ExecutionRequest.kt`, `src/contracts/AuthorizationPurposeVocabulary.kt`,
`src/runtime/AuthorizationPurposeRegistry.kt`, `src/interfaces/KnowledgeStore.kt`,
`src/interfaces/MemoryCore.kt`, `src/interfaces/CommunicationIntake.kt`,
`src/runtime/DefaultKnowledgeCandidateEvaluator.kt`, and `src/composition/ParkerRuntime.kt`
(construction, resource/vocabulary registration, and policy-rule sections).

This is not a reopening of Gap #54, which remains complete. This document does not modify any
frozen governance document; it reads, cites, and reasons from the two documents named above and
from the current production source cited throughout.

---

## 2. Adopted Architecture

**Query-time governed dereferencing is adopted.** Direct source evidence does not prove it
constitutionally impossible -- to the contrary, `DefaultKnowledgeCandidateEvaluator.resolve()`
(`src/runtime/DefaultKnowledgeCandidateEvaluator.kt` lines 355-378) already performs exactly this
pattern today, for candidate-evidence resolution during promotion, using a purpose-bound
`MemoryRetrieval` view. This design extends the identical, already-proven pattern to the read
(query) path, never inventing a new one.

The concrete flow (Boundary Review Section 4, instantiated):

```
owner query (InboundOwnerMessage.text, non-blank by construction)
  -> DefaultReasoningContextAssembler.assemble (conversation/reasoning coordination)
  -> ReasoningKnowledgeSource.recall (Reasoning Context assembly's own governed Knowledge Retrieval)
  -> DefaultReasoningKnowledgeSource: act-level authorization
  -> DefaultReasoningKnowledgeSource: structurally eligible KnowledgeItem enumeration + lifecycle shaping
  -> DefaultReasoningKnowledgeSource: item-level Knowledge Retrieval visibility authorization
  -> DefaultReasoningKnowledgeSource: governed Memory Core resolution (purpose-bound MemoryRetrieval)
  -> DefaultReasoningKnowledgeSource: content relevance evaluation, against lawfully resolved content only
  -> DefaultReasoningKnowledgeSource: SafeKnowledgeResultEntry construction + bounding
  -> DefaultReasoningContextAssembler: "Memory: ..." ReasoningContext entries
  -> model prompt
```

The four partial-order invariants Boundary Review Section 4 fixes are each satisfied by
construction, never by convention: act-level authorization gates persistence read (Section 4,
below); governed Memory Core resolution gates content disclosure (Section 4); safe result
construction gates Reasoning Context projection (Section 8); Reasoning Context assembly gates
model-prompt delivery (unchanged `DefaultReasoningContextAssembler.assemble` behaviour, Section
12).

**This is a distinct path from the Remember/promotion (write) path, which this document does not
touch.** `MemoryAdmissionCoordinator`, `DefaultKnowledgeSubmission`,
`DefaultKnowledgeCandidateEvaluator`, and `PermissionFilteredMemoryRetrieval.forAuthorizationPurpose`
are unmodified by every contract this document defines -- this design adds one new consumer of the
existing, unmodified `PermissionFilteredMemoryRetrieval.forAuthorizationPurpose` factory, exactly as
`candidateEvaluationMemoryRetrieval` and `evidenceIntelligenceMemoryRetrieval` already do
(`src/composition/ParkerRuntime.kt` lines 860-863).

---

## 3. Contract Types and Responsibility Allocation

| Type | Owner | Status | Role in this design |
|---|---|---|---|
| `KnowledgeItem`, `KnowledgeItemStatus`, `KnowledgeLifecycleEvent` | Knowledge Memory | Frozen (Programme 3) | Unchanged. Sole authoritative promoted/evaluated record. |
| `KnowledgeRetrievalQuery` | Knowledge Memory | Frozen (Unit 9) | Reused unchanged as the new surface's own request shape. |
| `KnowledgeItemPersistence` | Knowledge Memory | Frozen, `internal` (Unit 8/9.2) | Reused unchanged; read-only; never written by the new class. |
| `MemoryRetrieval`, `MemoryCoreRecordReference`, `Entity`, `Assertion`, `Document`, `Relationship` | Memory Core | Frozen (Programme 2) | Unchanged. Sole authoritative source of remembered content and provenance. |
| `PermissionFilteredMemoryRetrieval.forAuthorizationPurpose` | Trust Framework / Memory Core boundary | Frozen (Gap #54 Unit 3) | Reused unchanged; supplies this design's own purpose-bound dereference capability. |
| `DefaultKnowledgeRetrieval`, `KnowledgeRetrieval`, `KnowledgeRetrievalResult`, `KnowledgeResultEntry` | Knowledge Memory | Frozen (Unit 9) | **Untouched by this design.** Remains available for any future structural-matching-only consumer; carries no content, never will under this design. |
| `ReasoningKnowledgeSource` (new) | Knowledge Memory | Proposed, Contract Design tier | New, narrow, public contract; Section 3's smallest-shape decision (below). |
| `SafeKnowledgeResultEntry` (new) | Knowledge Memory | Proposed, Contract Design tier | New, minimal safe projection; Section 8. |
| `DefaultReasoningKnowledgeSource` (new) | Knowledge Memory | Proposed, Contract Design tier | New production class; owns the exact algorithm, Section 4. |
| `DefaultReasoningContextAssembler` | Programme 4, Reasoning Context | Existing, revised constructor | Consumes `ReasoningKnowledgeSource` in place of `KnowledgeSource`; renders `SafeKnowledgeResultEntry` values as entries. |

**Decision (Register item 3, Interface and adapter shape): the internal-adapter option is
adopted, not the widened-`KnowledgeRetrieval` option.** Widening `KnowledgeRetrieval.retrieve`'s
own return shape to carry resolved content would reopen the frozen Unit 9 Contract Design's own
explicit, disclosed "`KnowledgeItem` carries no free-text payload... by design: Knowledge Memory
never copies or duplicates Memory Core content" decision
(`src/runtime/DefaultKnowledgeRetrieval.kt` lines 136-138) for *every* current and future consumer
of that frozen, general-purpose contract -- not only the Reasoning Context path. `ReasoningKnowledgeSource`
is instead a new, narrower, additive contract, leaving `KnowledgeRetrieval`/`KnowledgeRetrievalResult`/
`KnowledgeResultEntry` completely untouched, satisfying Boundary Review Section 12's stop condition
against reopening a frozen guarantee without explicit upstream authorisation. This is the smallest
interface shape that preserves ownership: one new method, reusing the existing, frozen
`KnowledgeRetrievalQuery` request shape unchanged, returning a new, minimal, purpose-built result
type.

---

## 4. Exact Retrieval Algorithm

```kotlin
// src/interfaces/KnowledgeStore.kt (additive)
interface ReasoningKnowledgeSource {
    suspend fun recall(requestingPrincipalId: PrincipalId, query: KnowledgeRetrievalQuery): List<SafeKnowledgeResultEntry>
}
```

```kotlin
// src/runtime/DefaultReasoningKnowledgeSource.kt (new file)
internal class DefaultReasoningKnowledgeSource(
    private val persistence: KnowledgeItemPersistence,
    private val permissionEngine: PermissionEngine,
    private val evidenceMemoryRetrieval: MemoryRetrieval, // purpose-bound: forAuthorizationPurpose(REASONING_CONTEXT_RETRIEVAL_PURPOSE)
    private val authorizationPurpose: AuthorizationPurposeId,
    private val clock: Clock = Clock.systemUTC(),
) : ReasoningKnowledgeSource {

    override suspend fun recall(requestingPrincipalId: PrincipalId, query: KnowledgeRetrievalQuery): List<SafeKnowledgeResultEntry> {
        // Steps 1-3: query structure already validated by KnowledgeRetrievalQuery's own
        // construction-time checks (Section 5, below). Act-level gate, before any persistence read.
        val actDecision = permissionEngine.evaluate(buildExecutionRequest(requestingPrincipalId, query.correlationId, ACT_LEVEL_INTENT))
        if (!isAuthorised(actDecision)) return emptyList()

        // Steps 4-5: structurally eligible KnowledgeItems, lifecycle-shaped -- no content read yet.
        val structurallyEligible = persistence.findAll().filter { isRetrievable(it, query) }

        // Step 6: item-level visibility authorization, before any Memory Core dereference.
        val itemApproved = mutableListOf<KnowledgeItem>()
        for (item in structurallyEligible) {
            val decision = permissionEngine.evaluate(buildExecutionRequest(requestingPrincipalId, query.correlationId, itemLevelIntent(item)))
            if (isAuthorised(decision)) itemApproved += item
        }

        // Steps 7-8: governed dereference, then content relevance -- only for item-level-approved
        // candidates; a resolution failure (denied, missing, deleted, unsupported) silently
        // excludes that candidate (Section 9, authorized-partial), never the whole query.
        val relevant = mutableListOf<Pair<KnowledgeItem, String>>()
        for (item in itemApproved) {
            val content = resolveContent(requestingPrincipalId, item.evidenceReference) ?: continue
            if (content.contains(query.relevance, ignoreCase = true)) relevant += item to content
        }

        // Step 9: safe result construction.
        val entries = relevant.map { (item, content) ->
            SafeKnowledgeResultEntry(content, item.evidentialState, item.status, disclosureFor(item))
        }

        // Step 10: bounds applied last, after every authorization/visibility/relevance filter.
        return entries.take(query.maximumResults)
    }

    // Step: lifecycle shaping -- identical rule to DefaultKnowledgeRetrieval.isRetrievable
    // (src/runtime/DefaultKnowledgeRetrieval.kt lines 488-490), duplicated deliberately rather
    // than shared, since DefaultKnowledgeRetrieval is not modified by this design (Section 3).
    private fun isRetrievable(item: KnowledgeItem, query: KnowledgeRetrievalQuery): Boolean =
        item.status == KnowledgeItemStatus.ACTIVE || query.includeRetired

    // Steps 7: mirrors DefaultKnowledgeCandidateEvaluator.resolve() (lines 355-378) exactly --
    // null on denial, absence, or an unsupported record kind (Section 5, below); never fabricated.
    private suspend fun resolveContent(requestingPrincipalId: PrincipalId, reference: MemoryCoreRecordReference): String? =
        when (reference) {
            is MemoryCoreRecordReference.ToAssertion ->
                evidenceMemoryRetrieval.getAssertion(requestingPrincipalId, reference.assertionId)
                    ?.takeIf { it.status == MemoryCoreRecordStatus.ACTIVE }?.statement
            is MemoryCoreRecordReference.ToEntity ->
                evidenceMemoryRetrieval.getEntity(requestingPrincipalId, reference.entityId)
                    ?.takeIf { it.status == MemoryCoreRecordStatus.ACTIVE }
                    ?.let { (listOf(it.primaryLabel) + it.aliases).joinToString(", ") }
            is MemoryCoreRecordReference.ToDocument, is MemoryCoreRecordReference.ToRelationship -> null
        }

    // Staleness: identical algorithm to DefaultKnowledgeRetrieval.disclosureFor (lines 492-501),
    // duplicated for the same reason as isRetrievable, above.
    private fun disclosureFor(item: KnowledgeItem): StalenessDisclosure { /* identical to DefaultKnowledgeRetrieval */ TODO() }
    private fun isAuthorised(decision: PermissionDecision): Boolean = /* identical to DefaultKnowledgeRetrieval */ TODO()
    private fun buildExecutionRequest(requestingPrincipalId: PrincipalId, correlationId: String, intent: String): ExecutionRequest = TODO()
    private fun itemLevelIntent(item: KnowledgeItem): String = "Disclose Knowledge Item '${item.knowledgeId.value}' in a Reasoning Context Knowledge result"

    companion object {
        val REASONING_CONTEXT_RETRIEVAL_RESOURCE_ID: ResourceId = DefaultKnowledgeRetrieval.KNOWLEDGE_RETRIEVAL_RESOURCE_ID
        const val RETRIEVE_FOR_REASONING_CONTEXT_ACTION_NAME: String = "knowledge.retrieve_for_reasoning_context"
        const val ACT_LEVEL_INTENT: String = "Authorise Reasoning Context Knowledge Retrieval for a requesting principal"
    }
}
```

`TODO()` bodies mark implementation-time duplication of already-adopted, disclosed,
not-architecturally-significant logic (`isAuthorised`, `buildExecutionRequest`'s shared shape,
`disclosureFor`) -- Implementation Plan territory, not a Contract Design gap. The **ordering** above
is the binding contract; the literal Kotlin is illustrative.

**Justification for step placement (Boundary Review Section 4's own required justification).** Item-
level authorization (step 6) is placed *before* dereference (step 7) deliberately: it is the
cheapest, Memory-Core-free filter available after structural eligibility, and evaluating it first
means a caller never causes a Memory Core read for an item they may not even see -- minimising
both unnecessary Memory Core traffic and the number of principals whose visibility a dereference
failure could ever be observed by. Relevance evaluation (step 8) is placed *after* dereference
because it operates on dereferenced content, structurally impossible before that content exists.
Bounding (step 10) is placed last because every earlier filter can shrink the candidate set, and a
caller must never receive fewer than `maximumResults` entries merely because bounding truncated a
larger, not-yet-filtered candidate list -- mirroring `DefaultKnowledgeRetrieval`'s own identical
"bounding after permission filtering, not before" discipline (lines 70-75 of that file).

---

## 5. Content Normalization and Deterministic Matching

**Which Memory Core record kinds supply discoverable content.** Of the four `MemoryCoreRecordReference`
variants, only two name a record kind carrying genuine free-text content: `ToAssertion` (`Assertion.statement`,
`src/interfaces/MemoryCore.kt` line 472) and `ToEntity` (`Entity.primaryLabel` plus `Entity.aliases`,
lines 303, 306). `ToDocument` is structurally excluded: `Document`'s own KDoc discloses it "never
represents a document's parsed contents, extracted text, page structure, or any interpretation of
what the document says" (lines 349-350) -- there is no field to read. `ToRelationship` is likewise
excluded: `Relationship` carries only a closed-ish `relationshipType` vocabulary and two identifier
endpoints (lines 583-587), no free-text proposition. A `KnowledgeItem` referencing a Document or
Relationship therefore never content-matches any non-blank query -- mirroring
`DefaultKnowledgeRetrieval.matches()`'s own precedent of treating a structurally absent basis as
"never matching, rather than fabricating a basis that does not exist" (lines 148-153), applied here
to absent content instead of absent history.

**Record-status gate, new and additive.** Only a resolved `Assertion` or `Entity` whose own
`MemoryCoreRecordStatus` is `ACTIVE` supplies content; `DISPUTED`, `SUPERSEDED`, `ARCHIVED`, and
`DELETED` are all treated identically to "unavailable" (Section 9, below). This is a new, disclosed,
honest signal this design's own Memory Core access makes possible for the first time -- it never
narrows what `DefaultKnowledgeRetrieval`'s own age-based `StalenessDisclosure` proxy already
discloses (Section 10, below); it only additionally screens content this narrower surface would
otherwise disclose from a record whose own status has since diverged from `ACTIVE`. Implementation-
defined, not architecturally significant -- may be revised by a future implementation unit without a
Contract Design revision, mirroring `POSSIBLY_STALE_AFTER`'s own identical treatment.

**Matching algorithm.** Case-insensitive substring match of `KnowledgeRetrievalQuery.relevance`
against the resolved content string -- the identical, already-proven, already-tested convention
`DefaultKnowledgeRetrieval.matches()` and `InMemoryKnowledgeStore` both already use, applied to
genuinely dereferenced content instead of a promotion-basis string. No embeddings, semantic search,
stemming, synonym expansion, or model-based classification exists anywhere in this design, matching
the closed universe of retrieval heuristics this repository's governance has ever authorised for
either Memory Core or Knowledge Memory.

**Blank queries.** Structurally impossible through the real production call path: `KnowledgeRetrievalQuery.relevance`
already requires non-blank at construction (`src/interfaces/KnowledgeStore.kt` line 1364), and its
own real input, `InboundOwnerMessage.text`, already requires non-blank at construction
(`src/interfaces/CommunicationIntake.kt` line 91). No additional handling is a new decision this
design makes; both guarantees are inherited unchanged.

**Content that cannot be rendered safely.** Covered exhaustively by the record-kind exclusion and
the record-status gate above -- there is no third case: every `MemoryCoreRecordReference` variant is
either content-bearing-and-checked (`ToAssertion`/`ToEntity`) or structurally excluded
(`ToDocument`/`ToRelationship`); a resolved record either supplies content or resolves to `null`.

---

## 6. Principal, Correlation, and Request Identity

- **Originating principal.** `message.senderPrincipalId` -- the same owner `PrincipalId`
  `DefaultReasoningContextAssembler.assemble` already reads to construct today's `memoryQuery`
  (`src/runtime/DefaultReasoningContextAssembler.kt` line 303) -- is the sole `requestingPrincipalId`
  passed to `ReasoningKnowledgeSource.recall`. This principal reaches both the Knowledge Retrieval
  act/item-level gates (as `buildExecutionRequest`'s `principalId`) and Memory Core evidence-resolution
  authorization (as the same value passed to `evidenceMemoryRetrieval.getAssertion`/`getEntity`) --
  one propagated value, never re-derived, never substituted.
- **Parker system principal.** `SYSTEM_PARKER_PRINCIPAL_ID` (`src/composition/ParkerRuntime.kt`
  line 1440) remains lawful only for composition-time acts unrelated to this design (resource and
  vocabulary registration, `RuntimeEventLogger`) -- never as `requestingPrincipalId` for a
  `recall` call. Substituting it for the owner principal in a visibility decision is prohibited
  (Boundary Review Section 5).
- **Correlation identifier.** `message.correlationId.value` -- already `KnowledgeRetrievalQuery.correlationId`'s
  own existing field -- propagates unchanged through every `ExecutionRequest` this design's own
  gates construct, at both granularities, mirroring `DefaultKnowledgeRetrieval.buildExecutionRequest`'s
  own identical, disclosed "never freshly minted" discipline (lines 511-513). No new correlation
  identifier is minted anywhere inside the retrieval chain.
- **Fresh request identifiers.** Only `ExecutionRequest.requestId` -- an address for one specific
  permission evaluation, never a correlation value -- is freshly minted, once per `permissionEngine.evaluate`
  call, mirroring `DefaultKnowledgeRetrieval.buildExecutionRequest`'s own identical treatment (line
  524).

---

## 7. Authorization Purpose and Policy Contract

**Adopted Purpose identifier:** `knowledge-memory.reasoning-context-retrieval`. This matches the
frozen `<domain>.<purpose>` namespace convention `InMemoryAuthorizationPurposeRegistry.hasGovernedNamespaceShape`
enforces (`src/runtime/AuthorizationPurposeRegistry.kt` lines 118-128) and follows the exact,
already-adopted sibling precedent `AuthorizationPurposeId("knowledge-memory.candidate-evaluation")`
(`src/composition/ParkerRuntime.kt` line 1437) -- same domain segment, a distinct, disclosed purpose
segment naming this design's own consumption, never reused from or confused with candidate
evaluation.

**Registration.** Registered once, at composition time, exactly as `KNOWLEDGE_CANDIDATE_EVALUATION_PURPOSE`
and `EVIDENCE_INTELLIGENCE_INPUT_RESOLUTION_PURPOSE` already are, via `AuthorizationPurposeRegistry.register`.
Must be `ACTIVE` for `DefaultPermissionPolicy.evaluate`'s own `authorizationPurposeRegistry.isActive`
check to fold it into `effectivePurpose` (`src/runtime/DefaultPermissionPolicy.kt` lines 195-200).

**Consumer holding the purpose-bound capability.** `DefaultReasoningKnowledgeSource` holds the
Purpose value directly (constructor parameter, Section 4) for its own act/item-level
`ExecutionRequest`s, and a purpose-bound `MemoryRetrieval` view --
`permissionFilteredMemoryRetrieval.forAuthorizationPurpose(REASONING_CONTEXT_RETRIEVAL_PURPOSE)`,
constructed once at composition time, mirroring `candidateEvaluationMemoryRetrieval`'s own identical
construction (`src/composition/ParkerRuntime.kt` lines 860-861) -- for its own evidence-resolution
calls. No other component ever holds or constructs either capability.

**New verb, not a narrowing of `knowledge.retrieve`.** A genuinely new proposed-action name,
`knowledge.retrieve_for_reasoning_context`, is introduced rather than narrowing
`DefaultKnowledgeRetrieval.RETRIEVE_ACTION_NAME` ("knowledge.retrieve") itself. Narrowing the
existing verb would retroactively require Purpose on every current and future consumer of the
frozen, general-purpose `KnowledgeRetrieval` contract -- `DefaultKnowledgeRetrieval.buildExecutionRequest`
constructs no `authorizationPurpose` on any request today (confirmed, `src/runtime/DefaultKnowledgeRetrieval.kt`
lines 518-534) -- which would silently deny every such future caller unless it too adopted this
Purpose, an unjustified, undisclosed reopening of Decision Register item 8's own preservation
requirement. The new verb leaves `knowledge.retrieve` and `DefaultKnowledgeRetrieval`'s own existing
authorization behaviour completely untouched.

**Four new `PermissionPolicyRule` entries**, added alongside the existing rule list in
`src/composition/ParkerRuntime.kt` (after line 675), mirroring the already-adopted Gap #54 Unit 2/4
verb-guard-plus-purpose-override pattern (lines 617-656) exactly:

```kotlin
// Fail-closed guard for the new verb -- specificity 1, outranks the existing coarse (READ, MEMORY)
// approval (line 611-616, specificity 0) for this verb only; leaves that coarse rule, and every
// other verb it still governs, completely unchanged.
PermissionPolicyRule(
    action = PermissionAction.READ, resourceType = ResourceType.MEMORY,
    outcome = PermissionDecisionOutcome.DENIED, level = PermissionLevel.AUTOMATIC,
    proposedAction = DefaultReasoningKnowledgeSource.RETRIEVE_FOR_REASONING_CONTEXT_ACTION_NAME,
)
// Specificity 2: outranks the guard above only for a request carrying this exact, active Purpose.
PermissionPolicyRule(
    action = PermissionAction.READ, resourceType = ResourceType.MEMORY,
    outcome = PermissionDecisionOutcome.APPROVED, level = PermissionLevel.AUTOMATIC,
    authorizationPurpose = REASONING_CONTEXT_RETRIEVAL_PURPOSE,
    proposedAction = DefaultReasoningKnowledgeSource.RETRIEVE_FOR_REASONING_CONTEXT_ACTION_NAME,
)
// Memory Core evidence resolution: the existing Gap #54 Unit 2 DENIED guards for memory.retrieve /
// memory.retrieve_document (lines 623-636) already govern every Purpose without a specificity-2
// override -- only the two new specificity-2 APPROVED rules below are added, mirroring
// KNOWLEDGE_CANDIDATE_EVALUATION_PURPOSE's own identical pair (lines 641-656) exactly.
PermissionPolicyRule(
    action = PermissionAction.READ, resourceType = ResourceType.MEMORY,
    outcome = PermissionDecisionOutcome.APPROVED, level = PermissionLevel.AUTOMATIC,
    authorizationPurpose = REASONING_CONTEXT_RETRIEVAL_PURPOSE,
    proposedAction = PermissionFilteredMemoryRetrieval.RETRIEVE_ACTION_NAME,
)
PermissionPolicyRule(
    action = PermissionAction.READ, resourceType = ResourceType.DOCUMENT,
    outcome = PermissionDecisionOutcome.APPROVED, level = PermissionLevel.AUTOMATIC,
    authorizationPurpose = REASONING_CONTEXT_RETRIEVAL_PURPOSE,
    proposedAction = PermissionFilteredMemoryRetrieval.RETRIEVE_DOCUMENT_ACTION_NAME,
)
```

**Denial behaviour.** `DefaultPermissionPolicy.ruleOutcomeFor`'s own existing, unmodified maximal-
specificity mechanism (`src/runtime/DefaultPermissionPolicy.kt` lines 228-244) already guarantees:
absent, unregistered, retired, or wrong Purpose folds to "no purpose" (lines 195-200), which cannot
satisfy the specificity-2 rules above, leaving only the specificity-1 DENIED guard applicable --
denied, automatically, with no code this design adds. This is the existing mechanism applied to new
rule data, never a new mechanism.

**Proof Evidence Intelligence remains denied.** `EVIDENCE_INTELLIGENCE_INPUT_RESOLUTION_PURPOSE` and
`REASONING_CONTEXT_RETRIEVAL_PURPOSE` are distinct `AuthorizationPurposeId` values; `DefaultPermissionPolicy`'s
own rule filter requires `rule.authorizationPurpose == purpose` exactly (line 235) -- no rule this
design adds can ever satisfy Evidence Intelligence's own Purpose, and no rule Evidence Intelligence
already relies on is modified. This is a structural, not merely observed, non-widening proof.

**Resource/vocabulary registration.** No new `Resource` -- `RETRIEVE_FOR_REASONING_CONTEXT_ACTION_NAME`
reuses the already-registered `DefaultKnowledgeRetrieval.KNOWLEDGE_RETRIEVAL_RESOURCE_ID`
(`src/composition/ParkerRuntime.kt` lines 991-1006), since both verbs govern the same conceptual
Knowledge Retrieval boundary. One new `ActionVocabularyEntry` (`knowledge.retrieve_for_reasoning_context`
-> `(READ, MEMORY)`), mirroring the existing registration for `knowledge.retrieve` (lines 1007-1014)
exactly.

---

## 8. Safe Result Projection

```kotlin
// src/interfaces/KnowledgeStore.kt (additive)
data class SafeKnowledgeResultEntry(
    val content: String,
    val evidentialState: EvidentialState,
    val status: KnowledgeItemStatus,
    val staleness: StalenessDisclosure,
)
```

Field-by-field justification, against Boundary Review Section 8's own list:

- `content`: the authorized, dereferenced proposition text (Section 5) -- what Reasoning Context
  actually needs to render a "Memory:" entry. Never a raw Memory Core entity.
- Knowledge identity (`KnowledgeId`/`KnowledgeReference`): **omitted, not constitutionally
  necessary.** Nothing in this design's own rendering (below) or in `DefaultReasoningContextAssembler`'s
  existing rendering convention for any other source correlates entries by identity across turns;
  omitting it is the smaller surface, consistent with "only what Reasoning Context needs."
- `evidentialState`: reused unchanged (`EvidentialState`, already frozen, Article IV) -- an honest
  confidence signal, not a truth claim, exactly as `KnowledgeItem.evidentialState` already discloses
  it.
- `status`: reused unchanged (`KnowledgeItemStatus`, already frozen, two values) -- the lifecycle
  disclosure Section 10 requires, satisfied by reusing the existing type rather than inventing a
  parallel boolean.
- `staleness`: reused unchanged (`StalenessDisclosure`, already frozen, Unit 9.3) -- the mandatory,
  never-optional staleness signal, computed identically to `DefaultKnowledgeRetrieval.disclosureFor`
  (Section 4).
- Provenance reference: **omitted.** A raw `ProvenanceReference` crossing into Reasoning Context
  would itself be "a reusable retrieval handle" in substance if a future Representation Engine or
  caller treated it as a second Memory Core lookup key -- out of this document's own scope (Boundary
  Review Section 10 excludes Representation Engine work). No governing requirement makes it
  necessary for this cutover.
- No raw Memory Core entity or `MemoryRetrieval` capability of any kind is reachable from this type
  -- it carries only primitive/enum/already-frozen-value fields.

**Rendering and ordering.** `DefaultReasoningContextAssembler.assemble` renders one `"Memory: ..."`
entry per `SafeKnowledgeResultEntry`, in the exact order `ReasoningKnowledgeSource.recall` returns
them (Section 9, below, fixes that this design never reorders), immediately replacing today's
`memorySource.recall(memoryQuery).forEach { ... }` block (`src/runtime/DefaultReasoningContextAssembler.kt`
lines 309-312) with an equivalent block reading `SafeKnowledgeResultEntry` fields instead of
`KnowledgeRecord` fields. An empty result renders no entries, exactly as today. Final user-facing
wording (e.g. how staleness or evidential state is phrased in the rendered string) is Implementation
Plan territory, not fixed here -- this document fixes only which fields exist and their source, per
its own governing exclusion of "final user-facing explanations."

---

## 9. Failure and Partial-Result Semantics

| Case | Behaviour |
|---|---|
| Denied act-level retrieval | `recall` returns `emptyList()`. No persistence read occurs (Section 4, step 2-3). |
| Denied item-level visibility | That `KnowledgeItem` is silently excluded -- never a distinguishable per-item denial, mirroring `DefaultKnowledgeRetrieval`'s own established Unit 9.5 Clarification precedent. |
| Denied referenced evidence | `resolveContent` returns `null` (the delegate's own `getAssertion`/`getEntity` returns `null` for a denied record, `PermissionFilteredMemoryRetrieval` lines 127-137) -- silently excluded, identically to item-level denial. |
| Missing, deleted, malformed, unsupported, or unavailable evidence | `resolveContent` returns `null` (absent record, non-`ACTIVE` status, or an excluded reference kind) -- silently excluded, identically. |
| Authorized empty result | `recall` returns `emptyList()` -- **externally indistinguishable from denial**, by construction: both paths return the same empty `List` type, carrying no reason, count, or denial marker. |
| Partial resolution (some candidates succeed, others fail) | **Authorized-partial is adopted, not fail-whole.** The candidates that resolve and match are returned; the candidates that do not are silently excluded. Justification: fail-whole (denying the entire query because one candidate's evidence is unavailable) would be a strict availability regression with no governing requirement behind it, and is inconsistent with the already-adopted per-item silent-exclusion precedent this design otherwise mirrors throughout. |
| Internal audit distinction vs. external non-disclosure | Preserved internally by the genuine, real `PermissionDecision` each `permissionEngine.evaluate` call produces (auditable through whatever audit path already observes those decisions) -- never by the `recall` return type itself, which is deliberately incapable of carrying the distinction outward (Section 8). |
| Model-provider failure after context assembly | Out of this design's own boundary entirely: `ReasoningKnowledgeSource.recall` completes (or fails structurally, per the fault-propagation rule below) before a model call ever begins; nothing in this design writes to Knowledge Memory or Memory Core, so no model-provider failure downstream can ever mutate state this design owns. |
| Genuine dependency fault (`persistence`/`permissionEngine`/`evidenceMemoryRetrieval` throws) | Propagates unchanged -- no `try`/`catch` anywhere in `DefaultReasoningKnowledgeSource`, mirroring `DefaultKnowledgeRetrieval`'s own identical fault-propagation discipline. |

This document selects no user-facing failure wording (Boundary Review Section 7's own exclusion,
restated).

---

## 10. Lifecycle and Supersession

- **Default handling of `ACTIVE`/`RETIRED`.** Identical to `DefaultKnowledgeRetrieval.isRetrievable`
  (Section 4): `ACTIVE` items are eligible by default; `RETIRED` items are excluded unless the
  caller sets `KnowledgeRetrievalQuery.includeRetired = true`. `DefaultReasoningContextAssembler`'s
  own constructed `KnowledgeRetrievalQuery` (Section 12, below) does not set this field, so it
  defaults to `false` -- ordinary conversational retrieval never requests retired items, preserving
  today's implicit behaviour (nothing currently renders retired items into Reasoning Context either).
- **Supersession representation.** Unchanged from Unit 9's own governed treatment: supersession is
  not a status and never forks a `KnowledgeItem`; `KnowledgeItem.evidentialState` already holds the
  current classification. This design reads `item.evidentialState` and `item.status` directly
  (Section 8) and never inspects, renders, or truncates `KnowledgeItem.history` -- a superseded
  item's current entry appears exactly as any other current entry would, and multi-hop history
  remains available to any consumer holding the full `KnowledgeItem`, unaffected by this narrower
  surface.
- **Superseded content in ordinary Reasoning Context.** Yes, exactly as any other current,
  `ACTIVE`-status item would -- supersession is a revision-kind history event, not a distinct
  visibility state; excluding a superseded-but-still-current item would misrepresent supersession as
  a lifecycle status it is not (Contract Design Version 2 §3).
- **`DefaultKnowledgeRetrieval` preservation.** Not touched by this design at all (Section 3) --
  every existing lifecycle, ordering, staleness, and permission guarantee it makes remains exactly
  as adopted, unconditionally, since no line of that file is proposed to change.

---

## 11. Ordering, Limits, Duplicates, and Side-Channel Controls

- **Candidate ordering.** `KnowledgeItemPersistence.findAll`'s own disclosed insertion order
  (`src/runtime/KnowledgeItemPersistence.kt` lines 56-60) is preserved through every filter and loop
  in Section 4's algorithm -- `List.filter` and a sequential `for` loop both preserve relative
  order; nothing in this design sorts, ranks, or scores.
- **Relevance comparison.** A pure, deterministic substring predicate (Section 5) -- no ranking
  score of any kind exists to compare.
- **Authorized-result ordering.** Identical to candidate ordering -- the same list, filtered, never
  reordered.
- **`maximumResults` application.** Applied once, last, via `List.take` (Section 4, step 10) --
  after every authorization, visibility, and relevance filter, mirroring `DefaultKnowledgeRetrieval`'s
  own identical "bounding after permission filtering" discipline.
- **Duplicate handling.** No deduplication step exists or is needed: `KnowledgeItemPersistence`
  stores at most one `KnowledgeItem` per `KnowledgeId` (a `MutableMap`, `KnowledgeItemPersistence.kt`
  line 84), so `findAll()` can never yield the same item twice.
- **Empty-query behaviour.** Not reachable (Section 5) -- `KnowledgeRetrievalQuery.relevance` cannot
  be blank by construction.
- **Multiple `KnowledgeItem`s referencing the same evidence.** Each is evaluated, dereferenced, and
  matched independently -- this design performs no cross-item deduplication by `evidenceReference`,
  since two distinct promoted `KnowledgeItem`s citing the same Memory Core record are two genuinely
  distinct promoted facts (possibly with different `evidentialState` or history), never a single
  entity this surface is authorised to collapse.
- **Side-channel protections.** Count, identifier, denial, and timing leakage are each prevented
  structurally, not by convention: `recall`'s return type carries no count field, no identifier
  field (Section 8), no denial marker (Section 9), and no timing metadata of any kind -- a caller
  observing only the returned `List<SafeKnowledgeResultEntry>` cannot distinguish "denied," "found
  nothing," or "some candidates silently excluded" from one another by inspecting the value alone.
- **No semantic ranking** exists anywhere in this design (Section 5).

---

## 12. Legacy Production Cutover

**Retired from production composition:** the `InMemoryKnowledgeStore()` construction and its
binding to `memorySource: KnowledgeSource`, and that binding's use as
`DefaultReasoningContextAssembler`'s fourth constructor argument
(`src/composition/ParkerRuntime.kt` lines 391-392, 409). `DefaultReasoningContextAssembler`'s
constructor signature changes: `memorySource: KnowledgeSource` is replaced by
`knowledgeSource: ReasoningKnowledgeSource`; its `assemble` method's existing `memoryQuery`
construction and `memorySource.recall(memoryQuery).forEach { ... }` block (lines 302-312) are
replaced by an equivalent call to `knowledgeSource.recall(message.senderPrincipalId, knowledgeQuery)`
against a `KnowledgeRetrievalQuery` built from the same fields (`relevance = message.text`,
`correlationId = message.correlationId.value`, `maximumResults = MEMORY_QUERY_MAXIMUM_RESULTS`),
rendering `SafeKnowledgeResultEntry` values (Section 8) in place of `KnowledgeRecord` values.

In `ParkerRuntime.kt`'s composition, the new `DefaultReasoningKnowledgeSource` is constructed from
the same, already-shared `knowledgeItemPersistence` and `permissionEngine` instances
`DefaultKnowledgeRetrieval` already uses (lines 872, 905) -- never a second, parallel persistence
instance -- plus a new `reasoningContextMemoryRetrieval =
permissionFilteredMemoryRetrieval.forAuthorizationPurpose(REASONING_CONTEXT_RETRIEVAL_PURPOSE)`,
mirroring the existing `candidateEvaluationMemoryRetrieval`/`evidenceIntelligenceMemoryRetrieval`
construction exactly (lines 860-863).

**Legacy interfaces/implementation: retained, not deleted.** `KnowledgeSource`, `KnowledgeStore`,
and `InMemoryKnowledgeStore` are not authorised for deletion by this document. Direct repository
evidence shows real, non-production consumers remain: `tests/runtime/FakeKnowledgeSource.kt`,
`tests/runtime/InMemoryKnowledgeStoreTest.kt`,
`tests/runtime/DefaultReasoningContextAssemblerTest.kt`, and
`tests/composition/ParkerRuntimeReasoningContextIntegrationTest.kt` all reference these types
today. Only the one production wiring site above is retired; the interfaces and their test-only
implementation remain exactly as they are.

**No two active production knowledge feeds.** After this cutover, `DefaultReasoningContextAssembler`
holds exactly one knowledge-shaped collaborator (`ReasoningKnowledgeSource`) in production; no
`KnowledgeSource` binding reaches it through `ParkerRuntime.kt`'s composition any longer.

**Required migration and regression evidence.** A composition test proving no production path
constructs `InMemoryKnowledgeStore` or binds a `KnowledgeSource` into
`DefaultReasoningContextAssembler` any longer (mirroring
`tests/composition/ParkerRuntimeKnowledgeRetrievalCompositionTest.kt`'s own existing negative-proof
style); regression tests proving `DefaultReasoningContextAssembler`'s five other entry kinds
(identity, communication channel, current time, current conversation, prior messages, world
beliefs, available tools, current request) and their existing relative ordering are byte-for-byte
unchanged by this cutover; and a test proving the pre-cutover, always-empty "Memories" behaviour
(Planning Review Section 5, `InMemoryKnowledgeStore` never written to) is replaced by genuine,
non-empty behaviour only once a real, promoted, matching `KnowledgeItem` exists (Section 14, below).

---

## 13. Contract Invariants

Restated here as a single, checkable list -- each already argued individually above:

1. Memory Core remains the sole authoritative source of remembered proposition content and
   provenance (Section 2, Section 5) -- no field of `KnowledgeItem`, `SafeKnowledgeResultEntry`, or
   any type this design adds duplicates or copies it durably.
2. `KnowledgeItem` remains the authoritative promoted/evaluated record (Section 3) -- unmodified.
3. No second durable or indexed content store is introduced (Section 2, Section 8) -- content is
   resolved fresh, per query, per candidate, never cached or persisted by this design.
4. Every content read is a governed, permission-evaluated path (Section 4, Section 7) -- no raw
   Memory Core bypass exists anywhere in this design.
5. Permission filtering occurs before content or match results become observable (Section 4, step
   6-8; Section 9).
6. Evidence Intelligence authority is not widened (Section 7's own structural proof).
7. Missing, denied, deleted, or unavailable evidence is handled deterministically and honestly
   (Section 5, Section 9) -- never silently fabricated, never silently omitted without a disclosed,
   uniform non-disclosure rule.
8. Reasoning Context and the model receive no reusable `MemoryRetrieval` capability (Section 3,
   Section 8) -- `SafeKnowledgeResultEntry` carries no such handle.
9. No two active production knowledge feeds coexist after cutover (Section 12).
10. `DefaultKnowledgeRetrieval`'s own existing contract and authorization behaviour are unmodified
    (Section 3, Section 7, Section 10).

---

## 14. Required Verification Matrix

Contract tests (`tests/runtime/DefaultReasoningKnowledgeSourceTest.kt`, new): normalization/matching
(Assertion content matches, Entity content matches, Document/Relationship references never match,
case-insensitivity); lifecycle (`ACTIVE` included by default, `RETIRED` excluded by default and
included only with `includeRetired = true`); ordering (insertion order preserved through filtering);
bounds (`maximumResults` applied last, never truncates before relevance filtering).

Authorization tests: act-level denial produces `emptyList()` with zero `persistence.findAll()` calls
(a fake/mock persistence proving zero invocations, mirroring Unit 9.5's own "must never read
`KnowledgeItemPersistence` before it completes" proof style); item-level denial silently excludes
exactly the denied item; wrong, absent, inactive, unregistered, and mismatched Purpose each produce
`emptyList()` through the specificity-1 DENIED guard (Section 7); a request carrying
`KNOWLEDGE_CANDIDATE_EVALUATION_PURPOSE` or `EVIDENCE_INTELLIGENCE_INPUT_RESOLUTION_PURPOSE` against
`knowledge.retrieve_for_reasoning_context` is denied (coarse-rule/cross-Purpose fall-through
prevention, direct proof of Section 7's specificity argument).

Referenced-evidence tests: denied Assertion/Entity produces silent exclusion, not an exception or a
distinguishable denial marker; missing (deleted) evidence produces the same; a resolved but
non-`ACTIVE`-status record produces the same; a `ToDocument`/`ToRelationship` reference never enters
the matched set regardless of query content.

Partial-result test: a query where one candidate's evidence resolves and matches and a second
candidate's evidence is denied returns exactly the first candidate's entry -- direct proof of
Section 9's authorized-partial adoption.

Evidence Intelligence non-widening test (same runtime, immediately after a successful
`recall`): `EvidenceIntelligenceInputResolver`'s own existing denial behaviour is unchanged --
mirroring Gap #54 Unit 5's own same-runtime non-widening proof style exactly.

Composition tests (`tests/composition/`, extending or mirroring
`ParkerRuntimeKnowledgeRetrievalCompositionTest.kt`): `DefaultReasoningContextAssembler` is
constructed with a `ReasoningKnowledgeSource`, never a `KnowledgeSource`; no production path
constructs `InMemoryKnowledgeStore` any longer; `REASONING_CONTEXT_RETRIEVAL_PURPOSE` is registered
and active at composition time.

Regression tests (extending `DefaultReasoningContextAssemblerTest.kt`): every non-memory entry kind,
its rendering, and its relative ordering are unchanged; empty-`SafeKnowledgeResultEntry`-list
behaviour renders zero "Memory:" entries, exactly as today's empty-`KnowledgeRecord`-list behaviour
does.

**Required genuine end-to-end proof** (mirroring Planning Review Section 11 exactly, same-runtime
only):

```
owner Remember X (real MemoryAdmissionCoordinator -> DefaultKnowledgeSubmission -> promoted KnowledgeItem)
  -> later owner query using X's own content, a separate conversational turn
  -> ReasoningKnowledgeSource.recall, resolved by X's dereferenced content
  -> a real ReasoningContext.entries value genuinely containing X's content
  -> the real assembled model prompt containing X's content
```

A friendly reply alone is not evidence -- inspection of real `ReasoningContext.entries` or the
assembled model prompt is required, mirroring the Unit 5 Completion Review's own "real persistence,
not a friendly reply" discipline. Restart durability is not claimed or tested by any unit this
verification matrix authorises (Boundary Review Section 10's own exclusion, unchanged).

---

## 15. Files Likely Affected by Later Implementation

- `src/interfaces/KnowledgeStore.kt` -- additive: `ReasoningKnowledgeSource`, `SafeKnowledgeResultEntry`.
- `src/runtime/DefaultReasoningKnowledgeSource.kt` -- new file.
- `src/runtime/DefaultReasoningContextAssembler.kt` -- constructor signature change; `assemble`'s
  memory-rendering block replaced.
- `src/composition/ParkerRuntime.kt` -- new Purpose constant and registration; four new
  `PermissionPolicyRule` entries; new `ActionVocabularyEntry`; `DefaultReasoningKnowledgeSource`
  construction; retirement of the `InMemoryKnowledgeStore`/`memorySource` production binding.
- `tests/runtime/DefaultReasoningKnowledgeSourceTest.kt` -- new file.
- `tests/runtime/DefaultReasoningContextAssemblerTest.kt` -- extended.
- `tests/composition/` -- extended or new composition tests (Section 14).

This list is Contract-Design-tier disclosure, not an Implementation Plan's own file-by-file unit
sequencing, which remains a later, separate document's responsibility.

---

## 16. Explicit Exclusions and Non-Claims

- This document implements nothing -- no Kotlin, no test, no configuration.
- It does not create a Scope Lock or an Implementation Plan, and does not prescribe unit-by-unit
  implementation sequencing.
- It does not reopen Gap #54, which remains complete.
- It does not introduce restart durability, in any form -- every type and algorithm above operates
  only on already-in-memory, same-runtime state.
- It does not introduce embeddings, semantic search, a database, a remote service, or a new index.
- It does not expand Evidence Intelligence authority (Section 7's own structural proof).
- It does not modify the Remember/promotion path -- `MemoryAdmissionCoordinator`,
  `DefaultKnowledgeSubmission`, and `DefaultKnowledgeCandidateEvaluator` are untouched.
- It does not modify `DefaultKnowledgeRetrieval`, `KnowledgeRetrieval`, `KnowledgeRetrievalResult`,
  or `KnowledgeResultEntry` -- all four remain exactly as Unit 9 froze them.
- It does not delete `KnowledgeSource`, `KnowledgeStore`, or `InMemoryKnowledgeStore` -- direct
  repository evidence (Section 12) shows real, non-production consumers remain.
- It does not select final user-facing rendering wording for staleness, evidential state, or
  lifecycle disclosure -- Section 8 fixes only which fields exist and their source.
- It does not design Representation Engine behaviour.
- It does not rename Evidence Intelligence or modify any of its source comments.
- It does not create or reserve a new numbered gap, or a new programme identity.

---

## 17. Decision-Register Closure Table

| # | Decision | Status | Section | Rejected alternatives and why |
|---|---|---|---|---|
| 1 | Content representation and discovery | RESOLVED | 2, 5 | Adding content to `KnowledgeItem` (reopens frozen Unit 9/Programme 3 no-duplication guarantee); a separate indexed projection (second, unaudited source of truth, Boundary Review Section 8). Query-time governed dereferencing adopted, mirroring the already-proven `DefaultKnowledgeCandidateEvaluator.resolve()` pattern. |
| 2 | Authorization and dereference sequence | RESOLVED | 4 | An always-dereference-first ordering (wastes Memory Core reads on items the caller cannot see); bounding before filtering (would leak that a bound was applied before visibility was known, `DefaultKnowledgeRetrieval`'s own already-rejected shape). The ten-step ordering in Section 4 is adopted, with item-level authorization placed before dereference and bounding placed last. |
| 3 | Interface and adapter shape | RESOLVED | 3 | Widening `KnowledgeRetrieval` itself to carry content (reopens the frozen Unit 9 "never copies Memory Core content" guarantee for every current and future consumer, not only Reasoning Context). A new, narrow, additive `ReasoningKnowledgeSource` contract, reusing the frozen `KnowledgeRetrievalQuery` request shape unchanged, is adopted. |
| 4 | Principal and correlation propagation | RESOLVED | 6 | Reusing or minting a system-level principal for the retrieval call (would substitute away owner visibility, Boundary Review Section 5's own explicit prohibition). Owner principal propagation, unchanged correlation identifier propagation, and per-evaluation-only fresh request identifiers are adopted, mirroring `DefaultKnowledgeRetrieval`'s and `DefaultReasoningContextAssembler`'s own existing conventions exactly. |
| 5 | Authorization Purpose and policy specificity | RESOLVED | 7 | Narrowing the existing `knowledge.retrieve` verb itself (would retroactively deny every other current/future `KnowledgeRetrieval` consumer, none of which sets a Purpose today); leaving the coarse `(READ, MEMORY)` rule as the sole gate (the exact unconditional-permissiveness gap the Planning Review identified, Section 8). A new verb, `knowledge.retrieve_for_reasoning_context`, a new registered Purpose, `knowledge-memory.reasoning-context-retrieval`, and four new specificity-ranked `PermissionPolicyRule` entries mirroring the already-adopted Gap #54 pattern are adopted. |
| 6 | Safe result representation and evidential metadata | RESOLVED | 8 | Including `KnowledgeId`/`KnowledgeReference` (not constitutionally necessary, larger surface than required); including a raw `ProvenanceReference` (functions as a reusable retrieval handle in substance, Representation-Engine-tier decision, out of scope). `SafeKnowledgeResultEntry` (content, evidentialState, status, staleness) is adopted -- reusing every existing frozen value type it references. |
| 7 | Failure and partial-result semantics | RESOLVED | 9 | Fail-whole on any single candidate's evidence-resolution failure (unjustified availability regression, no governing requirement behind it). Authorized-partial, with denial and authorized-empty both externally represented as the identical, non-distinguishable empty `List`, is adopted. |
| 8 | Lifecycle and supersession | RESOLVED | 10 | A new "superseded" status or field (would misrepresent supersession as a lifecycle status the constitutional model does not recognise, Contract Design Version 2 §3). Reuse of `KnowledgeItem.status`/`evidentialState` unchanged, with `DefaultKnowledgeRetrieval`'s own `RETIRED`-excluded-by-default rule mirrored exactly, is adopted; `DefaultKnowledgeRetrieval` itself remains completely unmodified. |
| 9 | Ordering, limits, and side channels | RESOLVED | 11 | Any ranking or scoring step (never authorised anywhere in this repository's governance). Insertion-order preservation through every filter, bounding applied last, and a return type structurally incapable of carrying count/identifier/denial/timing information are adopted. |
| 10 | Legacy retirement | RESOLVED | 12 | Deleting `KnowledgeSource`/`KnowledgeStore`/`InMemoryKnowledgeStore` (direct repository evidence shows real, non-production test consumers remain -- deletion is not authorised). Retiring only the one production wiring site, leaving the legacy interfaces and implementation in place for their existing test consumers, is adopted. |
| 11 | Test and live-verification seams | RESOLVED | 14 | None rejected -- this item names the required seams directly; no competing alternative was considered, since the Planning Review's own end-to-end proof discipline (Section 11) is adopted unchanged rather than replaced. |

No item remains TBD, deferred, or ambiguous.

---

## 18. Stop Conditions

- No Kotlin implementation may begin before an accepted Scope Lock and Implementation Plan each
  exist, building on this Contract Design.
- Halt if a later unit finds `DefaultKnowledgeRetrieval`, `KnowledgeRetrieval`,
  `KnowledgeRetrievalResult`, or `KnowledgeResultEntry` must change to implement this design -- this
  Contract Design's own Section 3 decision depends on all four remaining untouched; a discovered
  need to change any of them is a stop condition requiring a return to this document, not implied
  authority to proceed.
- Halt if `KnowledgeSource`, `KnowledgeStore`, or `InMemoryKnowledgeStore` deletion is proposed
  without a fresh repository check proving zero remaining consumers.
- Halt if remembered content would be duplicated outside Memory Core, in any form, at any stage of
  implementation.
- Halt if Reasoning Context or the model would gain raw Memory Core access, or a reusable
  `MemoryRetrieval`-shaped capability, at any stage of implementation.
- Halt if Evidence Intelligence authority would widen, in any form.
- Halt if authorization would occur after persistence or content disclosure, at any stage.
- Halt if a broad or coarse rule is found to override absent, inactive, unregistered, wrong, or
  mismatched Purpose for `knowledge.retrieve_for_reasoning_context`, `memory.retrieve`, or
  `memory.retrieve_document`.
- Halt if two production knowledge feeds are found active into `DefaultReasoningContextAssembler`
  simultaneously, at any point during implementation.
- Halt if a frozen Programme 3 or Memory Core guarantee is found to require reopening beyond what
  this document explicitly authorises (Section 13's own invariant list).
- Halt if live verification cannot inspect real `ReasoningContext.entries` or the real assembled
  model prompt directly.

---

## 19. Recommended Next Stage

A Scope Lock, freezing the exact file list (Section 15), the exact type and method signatures
(Sections 4, 7, 8), and the exact `PermissionPolicyRule` set (Section 7) this Contract Design fixes,
followed by an Implementation Plan sequencing the independently reviewed implementation units --
each requiring its own Completion Review and Independent Constitutional Review before the next
begins, mirroring Gap #54's own established discipline exactly. Neither is authorised or begun by
this document.

```
KNOWLEDGE DISCOVERABILITY AND GOVERNED RETRIEVAL INTO REASONING CONTEXT
CONTRACT DESIGN -- COMPLETE, PENDING SCOPE LOCK
```
