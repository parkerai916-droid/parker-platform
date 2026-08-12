**Status:** Contract Design only. Governance and contract-definition document. No Kotlin is implemented,
proposed as a diff, or changed by this document. Neither `src/` nor `tests/` is touched. This document resolves
the three prerequisite questions and the eleven-item Decision Register the Boundary Review fixed, and
incorporates a subsequent, accepted timing-boundary amendment to the Planning Review and Boundary Review plus
independent-review corrections to content rendering, the Memory Core record-status gate, and auditability; it
does not create a Scope Lock or Implementation Plan, does not prescribe unit-by-unit implementation sequencing,
does not reopen Gap #54, does not introduce restart durability, embeddings, semantic search, a database, a
remote service, or a new index, does not expand Evidence Intelligence authority, and does not modify the
Remember/promotion path. Every Kotlin type and method signature below is a contract sketch for a later Scope
Lock and Implementation Plan to adopt, adapt, or reject -- none is applied to `src/` by this document.

# Knowledge Discoverability and Governed Retrieval into Reasoning Context — Contract Design

---

## 1. Status and Governing Inputs

Read fresh, in full, for this Contract Design:
`docs/governance/KNOWLEDGE_DISCOVERABILITY_REASONING_CONTEXT_PLANNING_REVIEW.md` ("the Planning Review") and
`docs/governance/KNOWLEDGE_DISCOVERABILITY_REASONING_CONTEXT_BOUNDARY_REVIEW.md` ("the Boundary Review"), both
as amended by their own accepted timing-boundary correction. Every contract below is fixed inside the
ownership, information-flow, principal-and-purpose, authority, lifecycle-and-failure, data-ownership, and
retrieval-surface boundaries the Boundary Review already fixed (its Sections 3-9) -- this document narrows
those boundaries into exact algorithms and types; it does not loosen, reinterpret, or relitigate any of them.

Production source inspected fresh, in full or in every relevant section, to verify this design against current
reality: `src/runtime/DefaultKnowledgeRetrieval.kt`, `src/runtime/DefaultReasoningContextAssembler.kt`,
`src/runtime/KnowledgeItemPersistence.kt`, `src/composition/PermissionFilteredMemoryRetrieval.kt`,
`src/runtime/DefaultPermissionPolicy.kt`, `src/runtime/DefaultPermissionEngine.kt`,
`src/contracts/ExecutionRequest.kt`, `src/contracts/AuthorizationPurposeVocabulary.kt`,
`src/runtime/AuthorizationPurposeRegistry.kt`, `src/interfaces/KnowledgeStore.kt`, `src/interfaces/MemoryCore.kt`,
`src/interfaces/CommunicationIntake.kt`, `src/runtime/DefaultKnowledgeCandidateEvaluator.kt`, and
`src/composition/ParkerRuntime.kt` (construction, resource/vocabulary registration, and policy-rule sections).

This is not a reopening of Gap #54, which remains complete. This document does not modify any frozen governance
document; it reads, cites, and reasons from the two documents named above and from the current production
source cited throughout.

---

## 2. Adopted Architecture

**Query-time governed dereferencing is adopted.** Direct source evidence does not prove it constitutionally
impossible -- to the contrary, `DefaultKnowledgeCandidateEvaluator.resolve()`
(`src/runtime/DefaultKnowledgeCandidateEvaluator.kt` lines 355-378) already performs exactly this pattern
today, for candidate-evidence resolution during promotion, using a purpose-bound `MemoryRetrieval` view. This
design extends the identical, already-proven pattern to the read (query) path, never inventing a new one.

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

The four partial-order invariants Boundary Review Section 4 fixes are each satisfied by construction, never by
convention: act-level authorization gates persistence read (Section 4, below); governed Memory Core resolution
gates content disclosure (Section 4); safe result construction gates Reasoning Context projection (Section 8);
Reasoning Context assembly gates model-prompt delivery (unchanged
`DefaultReasoningContextAssembler.assemble` behaviour, Section 12).

**This is a distinct path from the Remember/promotion (write) path, which this document does not touch.**
`MemoryAdmissionCoordinator`, `DefaultKnowledgeSubmission`, `DefaultKnowledgeCandidateEvaluator`, and
`PermissionFilteredMemoryRetrieval.forAuthorizationPurpose` are unmodified by every contract this document
defines -- this design adds one new consumer of the existing, unmodified
`PermissionFilteredMemoryRetrieval.forAuthorizationPurpose` factory, exactly as
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

**Decision (Register item 3, Interface and adapter shape): the internal-adapter option is adopted, not the
widened-`KnowledgeRetrieval` option.** Widening `KnowledgeRetrieval.retrieve`'s own return shape to carry
resolved content would reopen the frozen Unit 9 Contract Design's own explicit, disclosed "`KnowledgeItem`
carries no free-text payload... by design: Knowledge Memory never copies or duplicates Memory Core content"
decision (`src/runtime/DefaultKnowledgeRetrieval.kt` lines 136-138) for *every* current and future consumer of
that frozen, general-purpose contract -- not only the Reasoning Context path. `ReasoningKnowledgeSource` is
instead a new, narrower, additive contract, leaving `KnowledgeRetrieval`/`KnowledgeRetrievalResult`/
`KnowledgeResultEntry` completely untouched, satisfying Boundary Review Section 12's stop condition against
reopening a frozen guarantee without explicit upstream authorisation. This is the smallest interface shape
that preserves ownership: one new method, reusing the existing, frozen `KnowledgeRetrievalQuery` request shape
unchanged, returning a new, minimal, purpose-built result type.

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
    // null on denial, absence, a non-ACTIVE Memory Core record status (binding gate, Section 5,
    // below), or an unsupported record kind (Section 5); never fabricated.
    private suspend fun resolveContent(requestingPrincipalId: PrincipalId, reference: MemoryCoreRecordReference): String? =
        when (reference) {
            is MemoryCoreRecordReference.ToAssertion ->
                evidenceMemoryRetrieval.getAssertion(requestingPrincipalId, reference.assertionId)
                    ?.takeIf { it.status == MemoryCoreRecordStatus.ACTIVE }?.statement?.let(::normalize)
            is MemoryCoreRecordReference.ToEntity ->
                evidenceMemoryRetrieval.getEntity(requestingPrincipalId, reference.entityId)
                    ?.takeIf { it.status == MemoryCoreRecordStatus.ACTIVE }
                    ?.let { (listOf(it.primaryLabel) + it.aliases).map(::normalize).joinToString(" | ") }
            is MemoryCoreRecordReference.ToDocument, is MemoryCoreRecordReference.ToRelationship -> null
        }

    // Content normalization (Section 5, below): CRLF and lone CR both become LF, once, before
    // matching. No trim, no whitespace collapse, no other transformation.
    private fun normalize(raw: String): String = raw.replace("\r\n", "\n").replace('\r', '\n')

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

`TODO()` bodies mark implementation-time duplication of already-adopted, disclosed, not-architecturally-
significant logic (`isAuthorised`, `buildExecutionRequest`'s shared shape, `disclosureFor`) -- Implementation
Plan territory, not a Contract Design gap; none conceals an unresolved contract decision. The **ordering**
above is the binding contract; the literal Kotlin is illustrative.

**Justification for step placement (Boundary Review Section 4's own required justification).** Item-level
authorization (step 6) is placed *before* dereference (step 7) deliberately: it is the cheapest,
Memory-Core-free filter available after structural eligibility, and evaluating it first means a caller never
causes a Memory Core read for an item they may not even see -- minimising both unnecessary Memory Core traffic
and the number of principals whose visibility a dereference failure could ever be observed by. Relevance
evaluation (step 8) is placed *after* dereference because it operates on dereferenced content, structurally
impossible before that content exists. Bounding (step 10) is placed last because every earlier filter can
shrink the candidate set, and a caller must never receive fewer than `maximumResults` entries merely because
bounding truncated a larger, not-yet-filtered candidate list -- mirroring `DefaultKnowledgeRetrieval`'s own
identical "bounding after permission filtering, not before" discipline (lines 70-75 of that file).

---

## 5. Content Normalization and Deterministic Matching

**Which Memory Core record kinds supply discoverable content.** Of the four `MemoryCoreRecordReference` variants,
only two name a record kind carrying genuine free-text content: `ToAssertion` (`Assertion.statement`,
`src/interfaces/MemoryCore.kt` line 472) and `ToEntity` (`Entity.primaryLabel` plus `Entity.aliases`, lines 303,
306). `ToDocument` is structurally excluded: `Document`'s own KDoc discloses it "never represents a document's
parsed contents, extracted text, page structure, or any interpretation of what the document says" (lines
349-350) -- there is no field to read. `ToRelationship` is likewise excluded: `Relationship` carries only a
closed-ish `relationshipType` vocabulary and two identifier endpoints (lines 583-587), no free-text proposition.
A `KnowledgeItem` referencing a Document or Relationship therefore never content-matches any non-blank query --
mirroring `DefaultKnowledgeRetrieval.matches()`'s own precedent of treating a structurally absent basis as
"never matching, rather than fabricating a basis that does not exist" (lines 148-153), applied here to absent
content instead of absent history.

**Record-status gate -- a binding Contract Design decision, not implementation-defined.** Only a resolved
`Assertion` or `Entity` whose own `MemoryCoreRecordStatus` is `ACTIVE` supplies content. **`DISPUTED`,
`SUPERSEDED`, `ARCHIVED`, and `DELETED` referenced Memory Core records do not supply conversationally
discoverable content in this first programme.** Unlike `POSSIBLY_STALE_AFTER` -- a genuinely tunable threshold
inside an already-fixed algorithm -- which `MemoryCoreRecordStatus` values permit content disclosure is a
policy decision with direct leakage and integrity consequences, not a tuning parameter; it is fixed here and
changing it requires its own future Contract Design revision, never a silent implementation change. This gate
is a new, disclosed, honest signal this design's own Memory Core access makes possible for the first time -- it
never narrows what `DefaultKnowledgeRetrieval`'s own age-based `StalenessDisclosure` proxy already discloses
(Section 10, below); it only additionally screens content this narrower surface would otherwise disclose from a
record whose own status has since diverged from `ACTIVE`. **This gate is distinct from, and independent of,
`KnowledgeItem.status` `ACTIVE`/`RETIRED` lifecycle filtering (Section 10)** -- the two operate on different
records (the referenced Memory Core record versus the `KnowledgeItem` itself) and neither substitutes for or
implies the other.

**Content normalization -- deterministic, locale-independent, Unicode-preserving.** Every character of an
authorized proposition's own Unicode text is preserved unchanged, with exactly one transformation applied,
once, immediately after a record's own free-text field is read and before either matching or rendering ever
sees it: every CRLF pair and every remaining lone CR is replaced by a bare LF
(`raw.replace("\r\n", "\n").replace('\r', '\n')`). No trimming, no whitespace collapsing, no stemming, no
tokenization, no synonym expansion, no classification, and no semantic ranking exists anywhere in this step or
in matching, below -- normalization exists solely to fix one deterministic line-ending representation before
render-time escaping (Section 8) can operate on it.

**Matching algorithm.** Case-insensitive substring match of `KnowledgeRetrievalQuery.relevance` against the
normalized content string, via Kotlin's `String.contains(other, ignoreCase = true)` -- the identical,
already-proven, already-tested convention `DefaultKnowledgeRetrieval.matches()` and `InMemoryKnowledgeStore`
both already use, applied to genuinely dereferenced, normalized content instead of a promotion-basis string.
This comparison is locale-independent by construction: Kotlin's `ignoreCase` case-folding operates per-`Char`
(`Char.uppercaseChar()`/`lowercaseChar()`), which takes no `Locale` parameter, unlike
`String.uppercase(Locale)`/`lowercase(Locale)` -- no explicit `Locale` is threaded through this design because
none is needed. Matching operates on the normalized content, never on raw, unnormalized text. An empty
normalized content value cannot match: every content source this design reads is non-blank by its own frozen
construction-time validation (`Assertion.statement`, `Entity.primaryLabel`; `src/interfaces/MemoryCore.kt`
lines 479, 313), and line-ending normalization alone cannot reduce non-blank text to empty text;
`String.contains` on an empty receiver against `KnowledgeRetrievalQuery.relevance` (itself always non-blank,
per "Blank queries," below) is `false` by Kotlin's own stdlib definition regardless. No embeddings, semantic
search, stemming, synonym expansion, or model-based classification exists anywhere in this design, matching the
closed universe of retrieval heuristics this repository's governance has ever authorised for either Memory Core
or Knowledge Memory.

**Entity content construction -- a fixed, deterministic separator.** `ToEntity` content is
`listOf(entity.primaryLabel).plus(entity.aliases).joinToString(" | ")` -- `primaryLabel` first, then every
alias in `Entity.aliases`' own existing order, each segment joined by the fixed, literal three-character
separator `" | "` (space, pipe, space), never a locale-sensitive list-formatting function. Each segment is
normalized (above) individually before joining; joining introduces no new line-ending character.

**Blank queries.** Structurally impossible through the real production call path:
`KnowledgeRetrievalQuery.relevance` already requires non-blank at construction
(`src/interfaces/KnowledgeStore.kt` line 1364), and its own real input, `InboundOwnerMessage.text`, already
requires non-blank at construction (`src/interfaces/CommunicationIntake.kt` line 91) -- both guarantees are
inherited unchanged; no additional handling is a new decision this design makes. **Content that cannot be
rendered safely** is covered exhaustively by the record-kind exclusion and the record-status gate above --
there is no third case: every `MemoryCoreRecordReference` variant is either content-bearing-and-checked
(`ToAssertion`/`ToEntity`) or structurally excluded (`ToDocument`/`ToRelationship`); a resolved record either
supplies content or resolves to `null`. Render-time safety for content that *is* supplied -- escaping so it
cannot inject additional prompt entries or structural lines -- is fixed in Section 8, below, not here:
normalization (above) fixes line-ending *representation*; escaping fixes line-ending and control-character
*safety* at the one point content crosses into the model prompt.

---

## 6. Principal, Correlation, and Request Identity

- **Originating principal.** `message.senderPrincipalId` -- the same owner `PrincipalId`
  `DefaultReasoningContextAssembler.assemble` already reads to construct today's `memoryQuery`
  (`src/runtime/DefaultReasoningContextAssembler.kt` line 303) -- is the sole `requestingPrincipalId` passed to
  `ReasoningKnowledgeSource.recall`. This principal reaches both the Knowledge Retrieval act/item-level gates
  (as `buildExecutionRequest`'s `principalId`) and Memory Core evidence-resolution authorization (as the same
  value passed to `evidenceMemoryRetrieval.getAssertion`/`getEntity`) -- one propagated value, never
  re-derived, never substituted.
- **Parker system principal.** `SYSTEM_PARKER_PRINCIPAL_ID` (`src/composition/ParkerRuntime.kt` line 1440)
  remains lawful only for composition-time acts unrelated to this design (resource and vocabulary
  registration, `RuntimeEventLogger`) -- never as `requestingPrincipalId` for a `recall` call. Substituting it
  for the owner principal in a visibility decision is prohibited (Boundary Review Section 5).
- **Correlation identifier.** `message.correlationId.value` -- already `KnowledgeRetrievalQuery.correlationId`'s
  own existing field -- propagates unchanged through every `ExecutionRequest` this design's own gates
  construct, at both granularities, mirroring `DefaultKnowledgeRetrieval.buildExecutionRequest`'s own
  identical, disclosed "never freshly minted" discipline (lines 511-513). No new correlation identifier is
  minted anywhere inside the retrieval chain.
- **Fresh request identifiers.** Only `ExecutionRequest.requestId` -- an address for one specific permission
  evaluation, never a correlation value -- is freshly minted, once per `permissionEngine.evaluate` call,
  mirroring `DefaultKnowledgeRetrieval.buildExecutionRequest`'s own identical treatment (line 524).

---

## 7. Authorization Purpose and Policy Contract

**Adopted Purpose identifier:** `knowledge-memory.reasoning-context-retrieval`. This matches the frozen
`<domain>.<purpose>` namespace convention `InMemoryAuthorizationPurposeRegistry.hasGovernedNamespaceShape`
enforces (`src/runtime/AuthorizationPurposeRegistry.kt` lines 118-128) and follows the exact, already-adopted
sibling precedent `AuthorizationPurposeId("knowledge-memory.candidate-evaluation")`
(`src/composition/ParkerRuntime.kt` line 1437) -- same domain segment, a distinct, disclosed purpose segment
naming this design's own consumption, never reused from or confused with candidate evaluation. **Registration:**
registered once, at composition time, exactly as `KNOWLEDGE_CANDIDATE_EVALUATION_PURPOSE` and
`EVIDENCE_INTELLIGENCE_INPUT_RESOLUTION_PURPOSE` already are, via `AuthorizationPurposeRegistry.register`. Must
be `ACTIVE` for `DefaultPermissionPolicy.evaluate`'s own `authorizationPurposeRegistry.isActive` check to fold
it into `effectivePurpose` (`src/runtime/DefaultPermissionPolicy.kt` lines 195-200). **Consumer holding the
purpose-bound capability:** `DefaultReasoningKnowledgeSource` holds the Purpose value
directly (constructor parameter, Section 4) for its own act/item-level `ExecutionRequest`s, and a purpose-bound
`MemoryRetrieval` view -- `permissionFilteredMemoryRetrieval.forAuthorizationPurpose(REASONING_CONTEXT_RETRIEVAL_PURPOSE)`,
constructed once at composition time, mirroring `candidateEvaluationMemoryRetrieval`'s own identical
construction (`src/composition/ParkerRuntime.kt` lines 860-861) -- for its own evidence-resolution calls. No
other component ever holds or constructs either capability.

**New verb, not a narrowing of `knowledge.retrieve`.** A genuinely new proposed-action name,
`knowledge.retrieve_for_reasoning_context`, is introduced rather than narrowing
`DefaultKnowledgeRetrieval.RETRIEVE_ACTION_NAME` ("knowledge.retrieve") itself. Narrowing the existing verb
would retroactively require Purpose on every current and future consumer of the frozen, general-purpose
`KnowledgeRetrieval` contract -- `DefaultKnowledgeRetrieval.buildExecutionRequest` constructs no
`authorizationPurpose` on any request today (confirmed, `src/runtime/DefaultKnowledgeRetrieval.kt` lines
518-534) -- which would silently deny every such future caller unless it too adopted this Purpose, an
unjustified, undisclosed reopening of Decision Register item 8's own preservation requirement. The new verb
leaves `knowledge.retrieve` and `DefaultKnowledgeRetrieval`'s own existing authorization behaviour completely
untouched.

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

**Denial behaviour.** `DefaultPermissionPolicy.ruleOutcomeFor`'s own existing, unmodified maximal-specificity
mechanism (`src/runtime/DefaultPermissionPolicy.kt` lines 228-244) already guarantees: absent, unregistered,
retired, or wrong Purpose folds to "no purpose" (lines 195-200), which cannot satisfy the specificity-2 rules
above, leaving only the specificity-1 DENIED guard applicable -- denied, automatically, with no code this
design adds. This is the existing mechanism applied to new rule data, never a new mechanism.

**Proof Evidence Intelligence remains denied.** `EVIDENCE_INTELLIGENCE_INPUT_RESOLUTION_PURPOSE` and
`REASONING_CONTEXT_RETRIEVAL_PURPOSE` are distinct `AuthorizationPurposeId` values; `DefaultPermissionPolicy`'s
own rule filter requires `rule.authorizationPurpose == purpose` exactly (line 235) -- no rule this design adds
can ever satisfy Evidence Intelligence's own Purpose, and no rule Evidence Intelligence already relies on is
modified. This is a structural, not merely observed, non-widening proof.

**Resource/vocabulary registration.** No new `Resource` -- `RETRIEVE_FOR_REASONING_CONTEXT_ACTION_NAME` reuses
the already-registered `DefaultKnowledgeRetrieval.KNOWLEDGE_RETRIEVAL_RESOURCE_ID`
(`src/composition/ParkerRuntime.kt` lines 991-1006), since both verbs govern the same conceptual Knowledge
Retrieval boundary. One new `ActionVocabularyEntry` (`knowledge.retrieve_for_reasoning_context` -> `(READ,
MEMORY)`), mirroring the existing registration for `knowledge.retrieve` (lines 1007-1014) exactly.

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

- `content`: the authorized, dereferenced proposition text (Section 5) -- what Reasoning Context actually needs
  to render a "Memory:" entry. Never a raw Memory Core entity.
- Knowledge identity (`KnowledgeId`/`KnowledgeReference`): **omitted, not constitutionally necessary.** Nothing
  in this design's own rendering (below) or in `DefaultReasoningContextAssembler`'s existing rendering
  convention for any other source correlates entries by identity across turns; omitting it is the smaller
  surface, consistent with "only what Reasoning Context needs."
- `evidentialState`: reused unchanged (`EvidentialState`, already frozen, Article IV) -- an honest confidence
  signal, not a truth claim, exactly as `KnowledgeItem.evidentialState` already discloses it.
- `status`: reused unchanged (`KnowledgeItemStatus`, already frozen, two values) -- the lifecycle disclosure
  Section 10 requires, satisfied by reusing the existing type rather than inventing a parallel boolean.
- `staleness`: reused unchanged (`StalenessDisclosure`, already frozen, Unit 9.3) -- the mandatory,
  never-optional staleness signal, computed identically to `DefaultKnowledgeRetrieval.disclosureFor` (Section
  4).
- Provenance reference: **omitted.** A raw `ProvenanceReference` crossing into Reasoning Context would itself
  be "a reusable retrieval handle" in substance if a future Representation Engine or caller treated it as a
  second Memory Core lookup key -- out of this document's own scope (Boundary Review Section 10 excludes
  Representation Engine work). No governing requirement makes it necessary for this cutover.
- No raw Memory Core entity or `MemoryRetrieval` capability of any kind is reachable from this type -- it
  carries only primitive/enum/already-frozen-value fields.

**Rendering: a fixed, single internal model-prompt format, not final owner-facing wording.** This is an
internal projection into the model prompt only -- never final, owner-facing Representation Engine wording,
which remains excluded (Section 16). Field order and the exact format string are fixed here, one entry per
`SafeKnowledgeResultEntry`, in the exact order `ReasoningKnowledgeSource.recall` returns them (Section 11
fixes that this design never reorders):

```kotlin
// Applied only at this boundary -- SafeKnowledgeResultEntry.content itself remains the
// normalized (Section 5), unescaped text; escaping is a render-time-only transform.
private fun escapeForPrompt(normalized: String): String = buildString {
    for (c in normalized) {
        when {
            c == '\\' -> append("\\\\")
            c == '\n' -> append("\\n")
            c == '\r' -> append("\\r")
            c == '\t' -> append("\\t")
            c.code <= 0x1F || c.code == 0x7F || c.code in 0x80..0x9F -> append("\\u" + c.code.toString(16).uppercase().padStart(4, '0'))
            else -> append(c)
        }
    }
}

private fun renderKnowledgeEntry(entry: SafeKnowledgeResultEntry): String =
    "Memory: ${escapeForPrompt(entry.content)} (evidentialState=${entry.evidentialState.name}, " +
        "status=${entry.status.name}, staleness=${entry.staleness.name})"
```

Fixed field order: `content` (escaped), then `evidentialState`, then `status`, then `staleness`. Enum values
render via Kotlin's own stable, unlocalized `.name` property, never a phrased or translated string --
`evidentialState`/`status`/`staleness` therefore never vary by platform locale. Escaping covers, at minimum,
backslash, LF, CR, TAB, every remaining C0 control character (`0x00`-`0x1F`), DEL (`0x7F`), and every C1
control character (`0x80`-`0x9F`), each as a deterministic, zero-padded, four-hex-digit `\uXXXX` form except
the four two-character forms named explicitly above. Because every LF, CR, and other control character
`content` could carry is escaped to a literal, non-control character sequence before it ever reaches
`ReasoningContext.entries`, no authorized proposition's own text can inject an additional prompt entry or an
additional structural line -- each rendered `String` remains exactly one line, always, by construction. This
replaces today's `memorySource.recall(memoryQuery).forEach { ... }` block
(`src/runtime/DefaultReasoningContextAssembler.kt` lines 309-312) with an equivalent block calling
`renderKnowledgeEntry` per `SafeKnowledgeResultEntry`. An empty result renders no entries, exactly as today.

---

## 9. Failure and Partial-Result Semantics

| Case | Behaviour |
|---|---|
| Denied act-level retrieval | `recall` returns `emptyList()`. No persistence read occurs (Section 4, step 2-3). |
| Denied item-level visibility | That `KnowledgeItem` is silently excluded -- never a distinguishable per-item denial, mirroring `DefaultKnowledgeRetrieval`'s own established Unit 9.5 Clarification precedent. |
| Denied referenced evidence | `resolveContent` returns `null` (the delegate's own `getAssertion`/`getEntity` returns `null` for a denied record, `PermissionFilteredMemoryRetrieval` lines 127-137) -- silently excluded, identically to item-level denial. |
| Missing, deleted, malformed, unsupported, or non-`ACTIVE`-status evidence | `resolveContent` returns `null` (absent record, `DISPUTED`/`SUPERSEDED`/`ARCHIVED`/`DELETED` status -- the binding gate, Section 5 -- or an excluded reference kind) -- silently excluded, identically. |
| Authorized empty result | `recall` returns `emptyList()` -- **externally indistinguishable from denial**, by construction: both paths return the same empty `List` type, carrying no reason, count, or denial marker. |
| Partial resolution (some candidates succeed, others fail) | **Authorized-partial is adopted, not fail-whole.** The candidates that resolve and match are returned; the candidates that do not are silently excluded. Justification: fail-whole (denying the entire query because one candidate's evidence is unavailable) would be a strict availability regression with no governing requirement behind it, and is inconsistent with the already-adopted per-item silent-exclusion precedent this design otherwise mirrors throughout. |
| Internal control-flow knowledge vs. external non-disclosure | The `recall` return type is deliberately incapable of carrying a denial/count/identifier/timing distinction outward (Section 8, Section 11). Internally, each `permissionEngine.evaluate` call's own real `PermissionDecision` exists only within that one call's own transient control flow -- **no durable audit claim is made.** `DefaultPermissionEngine` retains no decision history and publishes no event for these direct, self-gating calls (`src/runtime/DefaultPermissionEngine.kt` lines 62-85: `evaluate` has no side effect, and `explain` states outright, "`DefaultPermissionEngine does not retain decision history`"). Adding durable audit persistence or event publication for these decisions is a future, separately governed concern this document does not claim, design, or authorise. |
| Elapsed wall-clock latency across denial, authorized-empty, filtering, and dereference paths | **Naturally variable, honestly disclosed, never eliminated.** Act denial, authorized-empty retrieval, item-level filtering, and Memory Core dereference each perform a genuinely different amount of suspend work; this design provides no constant-time execution and no artificial padding (Planning Review Section 6, Boundary Review Section 6, both as amended). No explicit timing field, count, denial marker, deliberate delay, or deliberately encoded protected-state timing signal crosses the `recall` result boundary (Section 11) -- that is the full extent of this design's own timing guarantee. This programme does not claim, and must not be read to claim, resistance to active timing analysis. |
| Model-provider failure after context assembly | Out of this design's own boundary entirely: `ReasoningKnowledgeSource.recall` completes (or fails structurally, per the fault-propagation rule below) before a model call ever begins; nothing in this design writes to Knowledge Memory or Memory Core, so no model-provider failure downstream can ever mutate state this design owns. |
| Genuine dependency fault (`persistence`/`permissionEngine`/`evidenceMemoryRetrieval` throws) | Propagates unchanged -- no `try`/`catch` anywhere in `DefaultReasoningKnowledgeSource`, mirroring `DefaultKnowledgeRetrieval`'s own identical fault-propagation discipline. |

This document selects no user-facing failure wording (Boundary Review Section 7's own exclusion, restated).

---

## 10. Lifecycle and Supersession

- **Default handling of `ACTIVE`/`RETIRED`.** Identical to `DefaultKnowledgeRetrieval.isRetrievable` (Section
  4): `ACTIVE` items are eligible by default; `RETIRED` items are excluded unless the caller sets
  `KnowledgeRetrievalQuery.includeRetired = true`. `DefaultReasoningContextAssembler`'s own constructed
  `KnowledgeRetrievalQuery` (Section 12, below) does not set this field, so it defaults to `false` -- ordinary
  conversational retrieval never requests retired items, preserving today's implicit behaviour (nothing
  currently renders retired items into Reasoning Context either).
- **Supersession representation.** Unchanged from Unit 9's own governed treatment: supersession is not a
  status and never forks a `KnowledgeItem`; `KnowledgeItem.evidentialState` already holds the current
  classification. This design reads `item.evidentialState` and `item.status` directly (Section 8) and never
  inspects, renders, or truncates `KnowledgeItem.history` -- a superseded item's current entry appears exactly
  as any other current entry would, and multi-hop history remains available to any consumer holding the full
  `KnowledgeItem`, unaffected by this narrower surface.
- **Superseded content in ordinary Reasoning Context.** Yes, exactly as any other current, `ACTIVE`-status item
  would -- supersession is a revision-kind history event, not a distinct visibility state; excluding a
  superseded-but-still-current item would misrepresent supersession as a lifecycle status it is not (Contract
  Design Version 2 §3).
- **`DefaultKnowledgeRetrieval` preservation.** Not touched by this design at all (Section 3) -- every existing
  lifecycle, ordering, staleness, and permission guarantee it makes remains exactly as adopted,
  unconditionally, since no line of that file is proposed to change.
- **Not to be confused with the Memory Core record-status gate (Section 5).** `KnowledgeItem.status`
  (`ACTIVE`/`RETIRED`) governs whether a *promoted `KnowledgeItem`* is structurally eligible at all, decided
  before any Memory Core read occurs. The binding `ACTIVE`-only Memory Core record-status gate (Section 5) is
  a separate, later check on the *referenced Memory Core record itself*, decided only for items that already
  passed this lifecycle filter and item-level authorization. Both must pass; neither implies or substitutes
  for the other.

---

## 11. Ordering, Limits, Duplicates, and Side-Channel Controls

- **Candidate ordering.** `KnowledgeItemPersistence.findAll`'s own disclosed insertion order
  (`src/runtime/KnowledgeItemPersistence.kt` lines 56-60) is preserved through every filter and loop in Section
  4's algorithm -- `List.filter` and a sequential `for` loop both preserve relative order; nothing in this
  design sorts, ranks, or scores.
- **Relevance comparison.** A pure, deterministic substring predicate (Section 5) -- no ranking score of any
  kind exists to compare.
- **Authorized-result ordering.** Identical to candidate ordering -- the same list, filtered, never reordered.
- **`maximumResults` application.** Applied once, last, via `List.take` (Section 4, step 10) -- after every
  authorization, visibility, and relevance filter, mirroring `DefaultKnowledgeRetrieval`'s own identical
  "bounding after permission filtering" discipline.
- **Duplicate handling.** No deduplication step exists or is needed: `KnowledgeItemPersistence` stores at most
  one `KnowledgeItem` per `KnowledgeId` (a `MutableMap`, `KnowledgeItemPersistence.kt` line 84), so `findAll()`
  can never yield the same item twice.
- **Empty-query behaviour.** Not reachable (Section 5) -- `KnowledgeRetrievalQuery.relevance` cannot be blank
  by construction.
- **Multiple `KnowledgeItem`s referencing the same evidence.** Each is evaluated, dereferenced, and matched
  independently -- this design performs no cross-item deduplication by `evidenceReference`, since two distinct
  promoted `KnowledgeItem`s citing the same Memory Core record are two genuinely distinct promoted facts
  (possibly with different `evidentialState` or history), never a single entity this surface is authorised to
  collapse.
- **Side-channel protections -- the result value itself.** Count, identifier, and denial leakage through the
  returned *value* are each prevented structurally, not by convention: `recall`'s return type carries no count
  field, no identifier field (Section 8), and no denial marker (Section 9) -- a caller inspecting only the
  returned `List<SafeKnowledgeResultEntry>` cannot distinguish "denied," "found nothing," or "some candidates
  silently excluded" from one another by inspecting the value alone. No explicit timing field, and no
  deliberate delay, count, or signal engineered to encode protected state, exists anywhere in this design
  either (Boundary Review Section 6, as amended).
- **Side-channel protections -- what this design does not, and cannot, claim.** Elapsed wall-clock latency is a
  genuinely different channel from the result value, and this design does not claim it is indistinguishable
  across paths (Section 9's own dedicated latency row states the accepted, disclosed limitation in full). This
  programme provides no constant-time execution, no artificial padding, batching, or obfuscation, and no
  resistance to active timing analysis -- adding any of those would require separate governance this document
  does not authorise.
- **No semantic ranking** exists anywhere in this design (Section 5).

---

## 12. Legacy Production Cutover

**Retired from production composition:** the `InMemoryKnowledgeStore()` construction and its binding to
`memorySource: KnowledgeSource`, and that binding's use as `DefaultReasoningContextAssembler`'s fourth
constructor argument (`src/composition/ParkerRuntime.kt` lines 391-392, 409).
`DefaultReasoningContextAssembler`'s constructor signature changes: `memorySource: KnowledgeSource` is replaced
by `knowledgeSource: ReasoningKnowledgeSource`; its `assemble` method's existing `memoryQuery` construction and
`memorySource.recall(memoryQuery).forEach { ... }` block (lines 302-312) are replaced by an equivalent call to
`knowledgeSource.recall(message.senderPrincipalId, knowledgeQuery)` against a `KnowledgeRetrievalQuery` built
from the same fields (`relevance = message.text`, `correlationId = message.correlationId.value`,
`maximumResults = MEMORY_QUERY_MAXIMUM_RESULTS`), rendering `SafeKnowledgeResultEntry` values (Section 8) in
place of `KnowledgeRecord` values.

In `ParkerRuntime.kt`'s composition, the new `DefaultReasoningKnowledgeSource` is constructed from the same,
already-shared `knowledgeItemPersistence` and `permissionEngine` instances `DefaultKnowledgeRetrieval` already
uses (lines 872, 905) -- never a second, parallel persistence instance -- plus a new
`reasoningContextMemoryRetrieval = permissionFilteredMemoryRetrieval.forAuthorizationPurpose(REASONING_CONTEXT_RETRIEVAL_PURPOSE)`,
mirroring the existing `candidateEvaluationMemoryRetrieval`/`evidenceIntelligenceMemoryRetrieval` construction
exactly (lines 860-863).

**Legacy interfaces/implementation: retained, not deleted.** `KnowledgeSource`, `KnowledgeStore`, and
`InMemoryKnowledgeStore` are not authorised for deletion by this document. Direct repository evidence shows
real, non-production consumers remain: `tests/runtime/FakeKnowledgeSource.kt`,
`tests/runtime/InMemoryKnowledgeStoreTest.kt`, `tests/runtime/DefaultReasoningContextAssemblerTest.kt`, and
`tests/composition/ParkerRuntimeReasoningContextIntegrationTest.kt` all reference these types today. Only the
one production wiring site above is retired; the interfaces and their test-only implementation remain exactly
as they are.

**No two active production knowledge feeds.** After this cutover, `DefaultReasoningContextAssembler` holds
exactly one knowledge-shaped collaborator (`ReasoningKnowledgeSource`) in production; no `KnowledgeSource`
binding reaches it through `ParkerRuntime.kt`'s composition any longer. **Required migration and regression
evidence:** a composition test proving no production path constructs
`InMemoryKnowledgeStore` or binds a `KnowledgeSource` into `DefaultReasoningContextAssembler` any longer
(mirroring `tests/composition/ParkerRuntimeKnowledgeRetrievalCompositionTest.kt`'s own existing negative-proof
style); regression tests proving `DefaultReasoningContextAssembler`'s five other entry kinds (identity,
communication channel, current time, current conversation, prior messages, world beliefs, available tools,
current request) and their existing relative ordering are byte-for-byte unchanged by this cutover; and a test
proving the pre-cutover, always-empty "Memories" behaviour (Planning Review Section 5, `InMemoryKnowledgeStore`
never written to) is replaced by genuine, non-empty behaviour only once a real, promoted, matching
`KnowledgeItem` exists (Section 14, below).

---

## 13. Contract Invariants

Restated here as a single, checkable list -- each already argued individually above:

1. Memory Core remains the sole authoritative source of remembered proposition content and provenance (Section
   2, Section 5) -- no field of `KnowledgeItem`, `SafeKnowledgeResultEntry`, or any type this design adds
   duplicates or copies it durably.
2. `KnowledgeItem` remains the authoritative promoted/evaluated record (Section 3) -- unmodified.
3. No second durable or indexed content store is introduced (Section 2, Section 8) -- content is resolved
   fresh, per query, per candidate, never cached or persisted by this design.
4. Every content read is a governed, permission-evaluated path (Section 4, Section 7) -- no raw Memory Core
   bypass exists anywhere in this design.
5. Permission filtering occurs before content or match results become observable (Section 4, step 6-8; Section
   9).
6. Evidence Intelligence authority is not widened (Section 7's own structural proof).
7. Missing, denied, deleted, or non-`ACTIVE`-status evidence is handled deterministically and honestly (Section
   5, Section 9) -- never silently fabricated, never silently omitted without a disclosed, uniform
   non-disclosure rule; the `ACTIVE`-only Memory Core record-status gate is a binding decision of this
   document, not implementation-defined (Section 5).
8. Reasoning Context and the model receive no reusable `MemoryRetrieval` capability (Section 3, Section 8) --
   `SafeKnowledgeResultEntry` carries no such handle.
9. No two active production knowledge feeds coexist after cutover (Section 12).
10. `DefaultKnowledgeRetrieval`'s own existing contract and authorization behaviour are unmodified (Section 3,
    Section 7, Section 10).
11. Authorized content can never reach `ReasoningContext.entries` as more than one structural line: every LF,
    CR, and other control character is deterministically escaped at render time (Section 8) before crossing
    that boundary -- no authorized proposition can inject an additional prompt entry or structural line.
12. No explicit timing field, count, denial marker, deliberate delay, or deliberately encoded protected-state
    timing signal crosses the `recall` result boundary (Section 9, Section 11); naturally variable elapsed
    latency across paths is disclosed, never claimed to be eliminated, and no constant-time or
    timing-analysis-resistance claim is made anywhere in this design.

---

## 14. Required Verification Matrix

Contract tests (`tests/runtime/DefaultReasoningKnowledgeSourceTest.kt`, new): normalization/matching (Assertion
content matches, Entity content matches, Document/Relationship references never match, case-insensitivity
verified independent of default JVM `Locale` -- e.g. running the same assertion under
`Locale.forLanguageTag("tr")` still matches, proving `Char`-level case folding is genuinely used, not a
`Locale`-sensitive `String.uppercase()`/`lowercase()` call); normalization (CRLF and lone CR both become LF; a
non-CR/LF Unicode character, including outside the Basic Multilingual Plane, is preserved unchanged; no
trimming or whitespace collapse occurs); Entity content joins `primaryLabel` and every alias with the fixed
`" | "` separator, in `Entity.aliases`' own order; lifecycle (`ACTIVE` included by default, `RETIRED` excluded
by default and included only with `includeRetired = true`); the `ACTIVE`-only Memory Core record-status gate
excludes `DISPUTED`, `SUPERSEDED`, `ARCHIVED`, and `DELETED` referenced records identically, proving the gate
is a fixed contract rule, not a configurable parameter; ordering (insertion order preserved through filtering);
bounds (`maximumResults` applied last, never truncates before relevance filtering).

Rendering tests (`tests/runtime/DefaultReasoningContextAssemblerTest.kt`, extended): the exact fixed format
string and field order (content, evidentialState, status, staleness); backslash, LF, CR, TAB, every remaining
C0 control character, DEL, and every C1 control character each escape to their exact defined form (Section 8),
including the deterministic four-hex-digit `\uXXXX` case; a rendered entry containing an embedded LF or CR in
its source content still yields exactly one `ReasoningContext` entry -- direct proof of Invariant 11's
line-injection prevention.

Authorization tests: act-level denial produces `emptyList()` with zero `persistence.findAll()` calls (a
fake/mock persistence proving zero invocations, mirroring Unit 9.5's own "must never read
`KnowledgeItemPersistence` before it completes" proof style); item-level denial silently excludes exactly the
denied item; wrong, absent, inactive, unregistered, and mismatched Purpose each produce `emptyList()` through
the specificity-1 DENIED guard (Section 7); a request carrying `KNOWLEDGE_CANDIDATE_EVALUATION_PURPOSE` or
`EVIDENCE_INTELLIGENCE_INPUT_RESOLUTION_PURPOSE` against `knowledge.retrieve_for_reasoning_context` is denied
(coarse-rule/cross-Purpose fall-through prevention, direct proof of Section 7's specificity argument).

Referenced-evidence tests: denied Assertion/Entity produces silent exclusion, not an exception or a
distinguishable denial marker; missing (deleted) evidence produces the same; a resolved but non-`ACTIVE`-status
record produces the same; a `ToDocument`/`ToRelationship` reference never enters the matched set regardless of
query content.

Side-channel and timing-scope tests: `SafeKnowledgeResultEntry` and the rendered `String` each expose no count,
identifier, or denial field, verified by structural/reflection inspection of the type itself; act-denied and
authorized-empty calls return identically-shaped, identically-empty results, verified by value equality, never
by an elapsed-time threshold. **Tests may prove the absence of explicit timing metadata and the absence of
intentional timing encoding; no test may claim, assert, or attempt to prove constant-time behaviour from an
ordinary elapsed-time measurement -- doing so would itself misrepresent this design's own disclosed limitation
(Section 9) as a guarantee it does not make.**

Partial-result test: a query where one candidate's evidence resolves and matches and a second candidate's
evidence is denied returns exactly the first candidate's entry -- direct proof of Section 9's
authorized-partial adoption.

Evidence Intelligence non-widening test (same runtime, immediately after a successful `recall`):
`EvidenceIntelligenceInputResolver`'s own existing denial behaviour is unchanged -- mirroring Gap #54 Unit 5's
own same-runtime non-widening proof style exactly.

Composition tests (`tests/composition/`, extending or mirroring `ParkerRuntimeKnowledgeRetrievalCompositionTest.kt`):
`DefaultReasoningContextAssembler` is constructed with a `ReasoningKnowledgeSource`, never a `KnowledgeSource`;
no production path constructs `InMemoryKnowledgeStore` any longer; `REASONING_CONTEXT_RETRIEVAL_PURPOSE` is
registered and active at composition time.

Regression tests (extending `DefaultReasoningContextAssemblerTest.kt`): every non-memory entry kind, its
rendering, and its relative ordering are unchanged; empty-`SafeKnowledgeResultEntry`-list behaviour renders
zero "Memory:" entries, exactly as today's empty-`KnowledgeRecord`-list behaviour does.

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
- `src/runtime/DefaultReasoningContextAssembler.kt` -- constructor signature change; `assemble`'s memory-
  rendering block replaced with the fixed rendering contract (Section 8).
- `src/composition/ParkerRuntime.kt` -- new Purpose constant and registration; four new
  `PermissionPolicyRule` entries; new `ActionVocabularyEntry`; `DefaultReasoningKnowledgeSource` construction;
  retirement of the `InMemoryKnowledgeStore`/`memorySource` production binding.
- `tests/runtime/DefaultReasoningKnowledgeSourceTest.kt` -- new file.
- `tests/runtime/DefaultReasoningContextAssemblerTest.kt` -- extended.
- `tests/composition/` -- extended or new composition tests (Section 14).

This list is Contract-Design-tier disclosure, not an Implementation Plan's own file-by-file unit sequencing,
which remains a later, separate document's responsibility.

---

## 16. Explicit Exclusions and Non-Claims

- This document implements nothing -- no Kotlin, no test, no configuration.
- It does not create a Scope Lock or an Implementation Plan, and does not prescribe unit-by-unit implementation
  sequencing.
- It does not reopen Gap #54, which remains complete.
- It does not introduce restart durability, in any form -- every type and algorithm above operates only on
  already-in-memory, same-runtime state.
- It does not introduce embeddings, semantic search, a database, a remote service, or a new index.
- It does not expand Evidence Intelligence authority (Section 7's own structural proof).
- It does not modify the Remember/promotion path -- `MemoryAdmissionCoordinator`,
  `DefaultKnowledgeSubmission`, and `DefaultKnowledgeCandidateEvaluator` are untouched.
- It does not modify `DefaultKnowledgeRetrieval`, `KnowledgeRetrieval`, `KnowledgeRetrievalResult`, or
  `KnowledgeResultEntry` -- all four remain exactly as Unit 9 froze them.
- It does not delete `KnowledgeSource`, `KnowledgeStore`, or `InMemoryKnowledgeStore` -- direct repository
  evidence (Section 12) shows real, non-production consumers remain.
- It fixes the internal, single-line model-prompt rendering format (Section 8) but does not select final,
  owner-facing Representation Engine wording -- the two are distinct, per the original task's own distinction,
  and only the latter remains excluded here.
- It does not design Representation Engine behaviour.
- It does not rename Evidence Intelligence or modify any of its source comments.
- It does not create or reserve a new numbered gap, or a new programme identity.
- It does not claim durable, persisted, or event-published auditing of the direct, self-gating
  `permissionEngine.evaluate` decisions this design produces -- `DefaultPermissionEngine` retains no decision
  history and publishes no such event today (`src/runtime/DefaultPermissionEngine.kt` lines 62-85); adding
  audit persistence or event publication is a future, separately governed concern this document does not
  design or authorise, and does not add any logging or event requirement of its own.
- It does not claim constant-time execution or resistance to active timing analysis, in any form -- it
  discloses, rather than conceals, that elapsed latency naturally varies across denial, authorized-empty,
  filtering, and dereference paths (Section 9, Section 11, Section 13 Invariant 12), and does not add padding,
  batching, obfuscation, or any other timing-channel mitigation.

---

## 17. Decision-Register Closure Table

| # | Decision | Status | Section | Rejected alternatives and why |
|---|---|---|---|---|
| 1 | Content representation and discovery | RESOLVED | 2, 5 | Adding content to `KnowledgeItem` (reopens frozen Unit 9/Programme 3 no-duplication guarantee); a separate indexed projection (second, unaudited source of truth, Boundary Review Section 8); treating the Memory-Core-record-status gate as freely revisable (a policy decision with real leakage consequences, not a tuning parameter). Query-time governed dereferencing, a fixed CRLF/CR-to-LF, Unicode-preserving, locale-independent normalization contract, and a binding `ACTIVE`-only record-status gate are all adopted, mirroring the already-proven `DefaultKnowledgeCandidateEvaluator.resolve()` pattern. |
| 2 | Authorization and dereference sequence | RESOLVED | 4 | An always-dereference-first ordering (wastes Memory Core reads on items the caller cannot see); bounding before filtering (would leak that a bound was applied before visibility was known, `DefaultKnowledgeRetrieval`'s own already-rejected shape). The ten-step ordering in Section 4 is adopted, with item-level authorization placed before dereference and bounding placed last. |
| 3 | Interface and adapter shape | RESOLVED | 3 | Widening `KnowledgeRetrieval` itself to carry content (reopens the frozen Unit 9 "never copies Memory Core content" guarantee for every current and future consumer, not only Reasoning Context). A new, narrow, additive `ReasoningKnowledgeSource` contract, reusing the frozen `KnowledgeRetrievalQuery` request shape unchanged, is adopted. |
| 4 | Principal and correlation propagation | RESOLVED | 6 | Reusing or minting a system-level principal for the retrieval call (would substitute away owner visibility, Boundary Review Section 5's own explicit prohibition). Owner principal propagation, unchanged correlation identifier propagation, and per-evaluation-only fresh request identifiers are adopted, mirroring `DefaultKnowledgeRetrieval`'s and `DefaultReasoningContextAssembler`'s own existing conventions exactly. |
| 5 | Authorization Purpose and policy specificity | RESOLVED | 7 | Narrowing the existing `knowledge.retrieve` verb itself (would retroactively deny every other current/future `KnowledgeRetrieval` consumer, none of which sets a Purpose today); leaving the coarse `(READ, MEMORY)` rule as the sole gate (the exact unconditional-permissiveness gap the Planning Review identified, Section 8). A new verb, `knowledge.retrieve_for_reasoning_context`, a new registered Purpose, `knowledge-memory.reasoning-context-retrieval`, and four new specificity-ranked `PermissionPolicyRule` entries mirroring the already-adopted Gap #54 pattern are adopted. |
| 6 | Safe result representation and evidential metadata | RESOLVED | 8 | Including `KnowledgeId`/`KnowledgeReference` (not constitutionally necessary, larger surface than required); including a raw `ProvenanceReference` (functions as a reusable retrieval handle in substance, Representation-Engine-tier decision, out of scope); deferring the internal rendering format to Implementation Plan (leaves the safe-projection contract incomplete, and risks an unescaped line-injection vector). `SafeKnowledgeResultEntry` (content, evidentialState, status, staleness), a fixed one-line format string, fixed field order, and a fully specified control-character escaping scheme are all adopted -- reusing every existing frozen value type it references. |
| 7 | Failure and partial-result semantics | RESOLVED | 9 | Fail-whole on any single candidate's evidence-resolution failure (unjustified availability regression, no governing requirement behind it); claiming durable auditability of direct `permissionEngine.evaluate` decisions (unsupported -- `DefaultPermissionEngine` retains no decision history, Section 9). Authorized-partial, with denial and authorized-empty both externally represented as the identical, non-distinguishable empty `List`, and an honest, non-durable characterisation of internal control-flow knowledge, are both adopted. |
| 8 | Lifecycle and supersession | RESOLVED | 10 | A new "superseded" status or field (would misrepresent supersession as a lifecycle status the constitutional model does not recognise, Contract Design Version 2 §3); conflating the binding Memory-Core-record-status gate (Section 5) with `KnowledgeItem` lifecycle filtering. Reuse of `KnowledgeItem.status`/`evidentialState` unchanged, with `DefaultKnowledgeRetrieval`'s own `RETIRED`-excluded-by-default rule mirrored exactly and the two gates kept explicitly distinct, is adopted; `DefaultKnowledgeRetrieval` itself remains completely unmodified. |
| 9 | Ordering, limits, and side channels | RESOLVED | 11 | Any ranking or scoring step (never authorised anywhere in this repository's governance); claiming the result type's own absence of a timing field eliminates observable wall-clock latency variance (a category error between an explicit metadata field and a genuine timing side channel). Insertion-order preservation through every filter, bounding applied last, and a return type structurally incapable of carrying count/identifier/denial/explicit-timing information are adopted, together with an honest, disclosed acknowledgement that natural latency variance is not eliminated and no constant-time claim is made. |
| 10 | Legacy retirement | RESOLVED | 12 | Deleting `KnowledgeSource`/`KnowledgeStore`/`InMemoryKnowledgeStore` (direct repository evidence shows real, non-production test consumers remain -- deletion is not authorised). Retiring only the one production wiring site, leaving the legacy interfaces and implementation in place for their existing test consumers, is adopted. |
| 11 | Test and live-verification seams | RESOLVED | 14 | Tests attempting to prove constant-time behaviour from an elapsed-time threshold (would misrepresent a disclosed limitation as a guarantee, Section 9). Contract, rendering, authorization, referenced-evidence, side-channel, composition, regression, and same-runtime end-to-end seams are all adopted, each confined to proving absence of explicit metadata/intentional encoding, never constant-time behaviour; the Planning Review's own end-to-end proof discipline (Section 11) is adopted unchanged. |

No item remains TBD, deferred, or ambiguous.

---

## 18. Stop Conditions

- No Kotlin implementation may begin before an accepted Scope Lock and Implementation Plan each exist, building
  on this Contract Design.
- Halt if a later unit finds `DefaultKnowledgeRetrieval`, `KnowledgeRetrieval`, `KnowledgeRetrievalResult`, or
  `KnowledgeResultEntry` must change to implement this design -- this Contract Design's own Section 3 decision
  depends on all four remaining untouched; a discovered need to change any of them is a stop condition
  requiring a return to this document, not implied authority to proceed.
- Halt if `KnowledgeSource`, `KnowledgeStore`, or `InMemoryKnowledgeStore` deletion is proposed without a fresh
  repository check proving zero remaining consumers.
- Halt if remembered content would be duplicated outside Memory Core, in any form, at any stage of
  implementation.
- Halt if Reasoning Context or the model would gain raw Memory Core access, or a reusable
  `MemoryRetrieval`-shaped capability, at any stage of implementation.
- Halt if Evidence Intelligence authority would widen, in any form.
- Halt if authorization would occur after persistence or content disclosure, at any stage.
- Halt if a broad or coarse rule is found to override absent, inactive, unregistered, wrong, or mismatched
  Purpose for `knowledge.retrieve_for_reasoning_context`, `memory.retrieve`, or `memory.retrieve_document`.
- Halt if two production knowledge feeds are found active into `DefaultReasoningContextAssembler`
  simultaneously, at any point during implementation.
- Halt if a frozen Programme 3 or Memory Core guarantee is found to require reopening beyond what this
  document explicitly authorises (Section 13's own invariant list).
- Halt if live verification cannot inspect real `ReasoningContext.entries` or the real assembled model prompt
  directly.
- Halt if unescaped content, or content containing an unescaped LF/CR or other unescaped control character, is
  found reaching `ReasoningContext.entries` -- the render-time escaping contract (Section 8) is binding, not
  optional.
- Halt if the `ACTIVE`-only Memory Core record-status gate (Section 5) is treated as freely revisable,
  configurable, or implementation-defined without a future Contract Design revision.
- Halt if implementation introduces an intentional timing signal, a deliberate delay, or explicit
  protected-state timing metadata -- never merely because authorized and denied paths naturally perform
  different amounts of work and therefore take different amounts of time.
- Halt if any later review or test claims constant-time execution or resistance to timing analysis without a
  separately governed mitigation mechanism and its own, matching verification.
- Halt if durable audit persistence or event publication for `permissionEngine.evaluate` decisions
  is added without separate, future governance authorising it.

---

## 19. Recommended Next Stage

A Scope Lock, freezing the exact file list (Section 15), the exact type and method signatures (Sections 4, 7,
8), and the exact `PermissionPolicyRule` set (Section 7) this Contract Design fixes, followed by an
Implementation Plan sequencing the independently reviewed implementation units -- each requiring its own
Completion Review and Independent Constitutional Review before the next begins, mirroring Gap #54's own
established discipline exactly. Neither is authorised or begun by this document.

```
KNOWLEDGE DISCOVERABILITY AND GOVERNED RETRIEVAL INTO REASONING CONTEXT
CONTRACT DESIGN -- COMPLETE, PENDING SCOPE LOCK
```
