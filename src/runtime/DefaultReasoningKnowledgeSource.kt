package parker.core.runtime

import java.time.Clock
import java.time.Duration
import java.time.Instant
import java.util.UUID
import parker.core.interfaces.AuthorizationPurposeId
import parker.core.interfaces.EvidentialState
import parker.core.interfaces.ExecutionRequest
import parker.core.interfaces.KnowledgeItem
import parker.core.interfaces.KnowledgeItemStatus
import parker.core.interfaces.KnowledgePromotion
import parker.core.interfaces.KnowledgeRetrievalQuery
import parker.core.interfaces.MemoryCoreRecordReference
import parker.core.interfaces.MemoryCoreRecordStatus
import parker.core.interfaces.MemoryRetrieval
import parker.core.interfaces.PermissionDecision
import parker.core.interfaces.PermissionDecisionOutcome
import parker.core.interfaces.PermissionEngine
import parker.core.interfaces.PrincipalId
import parker.core.interfaces.ReasoningKnowledgeSource
import parker.core.interfaces.RelevanceCandidate
import parker.core.interfaces.RelevanceCandidateToken
import parker.core.interfaces.RelevanceMechanism
import parker.core.interfaces.RelevanceRequest
import parker.core.interfaces.RelevanceResult
import parker.core.interfaces.RequestId
import parker.core.interfaces.RequestOrigin
import parker.core.interfaces.RequestPriority
import parker.core.interfaces.ResourceId
import parker.core.interfaces.SafeKnowledgeResultEntry
import parker.core.interfaces.StalenessDisclosure

/**
 * Knowledge Discoverability and Governed Retrieval into Reasoning Context, Implementation Unit 2.
 * The sole implementation of [ReasoningKnowledgeSource] -- see
 * `docs/governance/KNOWLEDGE_DISCOVERABILITY_REASONING_CONTEXT_CONTRACT_DESIGN.md` Section 4
 * ("Exact Retrieval Algorithm"), Section 5 ("Content Normalization and Deterministic Matching"),
 * and `docs/governance/KNOWLEDGE_DISCOVERABILITY_REASONING_CONTEXT_SCOPE_LOCK.md` Section 5 for the
 * frozen, binding contract this class implements exactly and nothing more.
 *
 * This is a distinct, read-only surface from [DefaultKnowledgeRetrieval] -- neither modifies, shares
 * mutable state with (beyond the same, already-shared [KnowledgeItemPersistence] and
 * [PermissionEngine] instances composition supplies both from), nor duplicates the other's own
 * contract. Unlike [DefaultKnowledgeRetrieval], this class also holds a purpose-bound
 * [MemoryRetrieval] view ([evidenceMemoryRetrieval]) and dereferences each candidate's own
 * [KnowledgeItem.evidenceReference] to resolve genuine, matchable content -- [DefaultKnowledgeRetrieval]
 * itself never does, and remains completely unmodified by this design.
 *
 * ## Ten-step algorithm, binding order (Contract Design Section 4)
 *
 * 1. Accept the already-validated [KnowledgeRetrievalQuery] ([KnowledgeRetrievalQuery]'s own
 *    construction-time checks already guarantee non-blank `relevance`/`correlationId` and a positive
 *    `maximumResults`).
 * 2. Build the act-level [ExecutionRequest].
 * 3. Evaluate act-level authorization before any persistence read; deny with `emptyList()`.
 * 4. Read [persistence] only after act-level approval.
 * 5. Lifecycle-filter: `ACTIVE` included; `RETIRED` included only when `includeRetired == true`.
 * 6. Evaluate item-level visibility for every structurally eligible candidate, before dereference;
 *    silently exclude a denied candidate.
 * 7. Dereference only item-level-approved candidates: `ToAssertion`/`ToEntity` via the purpose-bound
 *    [evidenceMemoryRetrieval]; `ToDocument`/`ToRelationship` resolve to `null` with no Memory Core
 *    call of any kind.
 * 8. Accept content only from a referenced record whose own [MemoryCoreRecordStatus] is `ACTIVE`;
 *    normalize, then case-insensitive substring match against [KnowledgeRetrievalQuery.relevance].
 * 9. Build [SafeKnowledgeResultEntry] in the frozen field order from resolved content and the owning
 *    [KnowledgeItem]'s own `evidentialState`, `status`, and staleness disclosure.
 * 10. Apply `maximumResults` last, via [List.take], after every earlier filter.
 *
 * A resolution failure for any one candidate (item-level denial, evidence denial, missing/deleted
 * evidence, a non-`ACTIVE` referenced-record status, or an unsupported reference kind) silently
 * excludes that candidate only -- authorized-partial, never fail-whole (Contract Design Section 9).
 * [persistence], [permissionEngine], and [evidenceMemoryRetrieval] are never wrapped in `try`/`catch`
 * anywhere in this class -- a genuine dependency fault propagates completely unchanged, mirroring
 * [DefaultKnowledgeRetrieval]'s own identical fault-propagation discipline.
 *
 * @param persistence The sole read source for already-promoted [KnowledgeItem] values. Read-only;
 *   never read before the act-level gate approves.
 * @param permissionEngine Evaluated once for the act-level gate, plus once per structurally eligible
 *   candidate for the item-level gate -- the same two-tier discipline [DefaultKnowledgeRetrieval]
 *   already established, duplicated deliberately rather than shared or extracted, since neither class
 *   is modified by the other's own design.
 * @param evidenceMemoryRetrieval A purpose-bound [MemoryRetrieval] view -- composition supplies
 *   `permissionFilteredMemoryRetrieval.forAuthorizationPurpose(authorizationPurpose)`, never a raw or
 *   differently-bound view. Only [MemoryRetrieval.getAssertion] and [MemoryRetrieval.getEntity] are
 *   ever called on it.
 * @param authorizationPurpose The exact, frozen Purpose this class's own act/item-level
 *   [ExecutionRequest]s carry -- supplied already-resolved; this class registers nothing and performs
 *   no Purpose-registry lookup of its own.
 * @param relevanceMechanism (RKS.2-RKS.4, `docs/governance/REASONING_KNOWLEDGE_SOURCE_BOUNDED_SEMANTIC_RELEVANCE_CONTRACT_AND_PERMISSION_SUCCESSOR.md`,
 *   `docs/governance/REASONING_KNOWLEDGE_SOURCE_BOUNDED_SEMANTIC_RELEVANCE_IMPLEMENTATION_PLAN.md`)
 *   The sole, mechanism-neutral seam through which [recall] may ever reach Bounded Relevance
 *   Computation -- invoked at most once per call, only when structural matching (steps 7-8) has
 *   completed successfully and found exactly zero relevant candidates from a non-empty dereferenced
 *   candidate set. Deliberately no default: a default relevance mechanism would be silent,
 *   undisclosed configuration inference, mirroring [DefaultKnowledgeRetrieval]'s own identical,
 *   already-governed refusal to default this dependency. This class constructs the concrete instance
 *   for neither production nor test use -- RKS.5's own runtime composition wiring supplies the real,
 *   already-shared [QmdRelevanceMechanism] instance in production (the identical instance
 *   [DefaultKnowledgeRetrieval] itself uses, per the Successor document's own "Shared Unit 9.7
 *   Mechanism Reuse" requirement), and this Unit's own tests supply narrow fakes -- never a live QMD
 *   subprocess.
 * @param clock The time source [disclosureFor] reads "now" from. Defaults to the real system clock in
 *   production; tests supply a fixed [Clock] so staleness assertions remain deterministic.
 */
internal class DefaultReasoningKnowledgeSource(
    private val persistence: KnowledgeItemPersistence,
    private val permissionEngine: PermissionEngine,
    private val evidenceMemoryRetrieval: MemoryRetrieval,
    private val authorizationPurpose: AuthorizationPurposeId,
    private val relevanceMechanism: RelevanceMechanism,
    private val clock: Clock = Clock.systemUTC(),
) : ReasoningKnowledgeSource {

    override suspend fun recall(requestingPrincipalId: PrincipalId, query: KnowledgeRetrievalQuery): List<SafeKnowledgeResultEntry> {
        // Steps 2-3: act-level gate, before any persistence read.
        val actDecision = permissionEngine.evaluate(
            buildExecutionRequest(requestingPrincipalId, query.correlationId, ACT_LEVEL_INTENT),
        )
        if (!isAuthorised(actDecision)) return emptyList()

        // Steps 4-5: structurally eligible KnowledgeItems, lifecycle-shaped -- no content read yet.
        val structurallyEligible = persistence.findAll().filter { isRetrievable(it, query) }

        // Step 6: item-level visibility authorization, before any Memory Core dereference.
        val itemApproved = mutableListOf<KnowledgeItem>()
        for (item in structurallyEligible) {
            val decision = permissionEngine.evaluate(
                buildExecutionRequest(requestingPrincipalId, query.correlationId, itemLevelIntent(item)),
            )
            if (isAuthorised(decision)) itemApproved += item
        }

        // Steps 7-8, restructured (RKS.2, Reasoning Context Bounded Semantic Relevance Implementation
        // Plan Section 7.1/7.2): dereference every item-level-approved candidate once, retaining the
        // item->content pairing instead of discarding it once the current candidate's own structural
        // test completes -- the identical dereferenced set Bounded Relevance Computation needs for its
        // own Pre-computation-admitted closed candidate set, not a second, wider dereference.
        val dereferenced = mutableListOf<Pair<KnowledgeItem, String>>()
        for (item in itemApproved) {
            val content = resolveContent(requestingPrincipalId, item.evidenceReference) ?: continue
            dereferenced += item to content
        }
        val structurallyMatched = dereferenced.filter { (_, content) -> content.contains(query.relevance, ignoreCase = true) }

        // Fallback trigger: one or more structural matches -- regardless of how many ultimately survive
        // -- always stands, unmodified, exactly as before this Unit. Bounded Relevance Computation runs
        // only when structural matching completed successfully and found exactly zero relevant
        // candidates over a non-empty dereferenced set (Successor Section 3/4); an empty dereferenced
        // set means no candidate exists for this principal at all, so the mechanism is never invoked to
        // rank nothing.
        val relevant: List<Pair<KnowledgeItem, String>> = when {
            structurallyMatched.isNotEmpty() -> structurallyMatched
            dereferenced.isEmpty() -> emptyList()
            else -> resolveSemanticFallback(requestingPrincipalId, query, dereferenced)
        }

        // Step 9: safe result construction -- from either the structural match, or (RKS.4) the freshly
        // re-verified Pre-disclosure state; never from a stale Pre-computation snapshot on either path.
        val entries = relevant.map { (item, content) ->
            SafeKnowledgeResultEntry(content, item.evidentialState, item.status, disclosureFor(item))
        }

        // Step 10: bounds applied last, after every authorization/visibility/relevance/fallback filter,
        // identically for both the structural and fallback paths.
        return entries.take(query.maximumResults)
    }

    /**
     * RKS.3 (Mechanism Invocation and Token Minting). Mints one opaque
     * [RelevanceCandidateToken] per already-dereferenced, already
     * item-level-approved candidate, invokes [relevanceMechanism] exactly
     * once with the query's own relevance text and the minimum content
     * boundary [dereferenced] already computed (never a [KnowledgeItem],
     * [MemoryCoreRecordReference], `evidentialState`, `status`, or
     * `StalenessDisclosure` -- Successor Section 6/7), and hands the result
     * to [resolveSemanticResult] for RKS.4's own fail-closed integrity
     * validation and three-check Pre-disclosure re-verification. The
     * token-to-item map is local to this one call -- never a class field,
     * never persisted, never returned (Successor Section 12/13).
     */
    private suspend fun resolveSemanticFallback(
        requestingPrincipalId: PrincipalId,
        query: KnowledgeRetrievalQuery,
        dereferenced: List<Pair<KnowledgeItem, String>>,
    ): List<Pair<KnowledgeItem, String>> {
        val tokenToItem = mutableMapOf<RelevanceCandidateToken, KnowledgeItem>()
        val candidates = dereferenced.map { (item, content) ->
            val token = RelevanceCandidateToken(UUID.randomUUID().toString())
            tokenToItem[token] = item
            RelevanceCandidate(token = token, content = content)
        }

        val relevanceResult = relevanceMechanism.rank(RelevanceRequest(queryText = query.relevance, candidates = candidates))

        return resolveSemanticResult(relevanceResult, tokenToItem, requestingPrincipalId, query)
    }

    /**
     * RKS.4 (Three-Check Pre-Disclosure Re-Verification and Fresh-Content
     * Construction, Successor Section 9/10). [relevanceMechanism]'s own
     * [RelevanceResult] is discovery, never authority: every returned
     * [RelevanceCandidateToken] is validated fail-closed against this one
     * call's own local [tokenToItem] map -- an unknown token, a duplicate
     * token, or more tokens than were supplied is a mechanism-level
     * integrity fault, thrown, never silently dropped or truncated. A
     * genuinely empty [RelevanceResult.rankedTokens] over a non-empty
     * supplied set is not a fault -- it is the mechanism's own honest
     * "nothing here is relevant" answer.
     *
     * A token surviving integrity validation is resolved back to its own
     * [KnowledgeItem] only through [tokenToItem] -- never trusted, never
     * substituted -- and then, immediately before any disclosure, undergoes
     * exactly three fresh checks, each a second invocation of an operation
     * Pre-computation already performed once: **(A)** [KnowledgeItem]
     * currentness, a fresh [KnowledgeItemPersistence.find]; **(B)**
     * permission currentness, a fresh [PermissionEngine.evaluate] for the
     * identical item-level intent; **(C)** Memory Core currentness and
     * content, a fresh, second [resolveContent] dereference -- the
     * genuinely new invocation type this class's own dereferenced-content
     * boundary requires that [DefaultKnowledgeRetrieval]'s simpler
     * two-check pattern never needed. Any of the three failing excludes the
     * candidate outright -- never disclosed, never substituted for. The
     * disclosed content, for every survivor, is always check C's own fresh
     * result, never [RelevanceCandidate.content] and never the
     * Pre-computation snapshot.
     */
    private suspend fun resolveSemanticResult(
        relevanceResult: RelevanceResult,
        tokenToItem: Map<RelevanceCandidateToken, KnowledgeItem>,
        requestingPrincipalId: PrincipalId,
        query: KnowledgeRetrievalQuery,
    ): List<Pair<KnowledgeItem, String>> {
        val rankedTokens = relevanceResult.rankedTokens

        check(rankedTokens.size <= tokenToItem.size) {
            "relevance mechanism returned more tokens (${rankedTokens.size}) than were supplied " +
                "(${tokenToItem.size}) for this request -- integrity fault, rejected fail-closed"
        }
        val seenTokens = mutableSetOf<RelevanceCandidateToken>()
        for (token in rankedTokens) {
            check(tokenToItem.containsKey(token)) {
                "relevance mechanism returned a token that is not a member of this request's own " +
                    "closed candidate set -- integrity fault, rejected fail-closed, never substituted, " +
                    "never treated as a low-relevance result: '${token.value}'"
            }
            check(seenTokens.add(token)) {
                "relevance mechanism returned a duplicate token -- integrity fault, rejected " +
                    "fail-closed, never silently de-duplicated: '${token.value}'"
            }
        }

        if (rankedTokens.isEmpty()) {
            // A genuine, successful "nothing here is relevant" answer over a non-empty supplied
            // candidate set -- never itself a failure.
            return emptyList()
        }

        val freshlyVerified = mutableListOf<Pair<KnowledgeItem, String>>()
        for (token in rankedTokens) {
            val preComputationItem = tokenToItem.getValue(token)
            // Pre-disclosure check A: KnowledgeItem currentness.
            val currentItem = persistence.find(preComputationItem.knowledgeId) ?: continue
            if (!isRetrievable(currentItem, query)) continue
            // Pre-disclosure check B: permission currentness -- the identical item-level intent,
            // resource, and action Pre-computation's own gate already used, evaluated freshly.
            val itemDecision = permissionEngine.evaluate(
                buildExecutionRequest(requestingPrincipalId, query.correlationId, itemLevelIntent(currentItem)),
            )
            if (!isAuthorised(itemDecision)) continue
            // Pre-disclosure check C: a fresh, second Memory Core dereference -- current content, or
            // exclusion if the referenced record is no longer ACTIVE or no longer exists.
            val freshContent = resolveContent(requestingPrincipalId, currentItem.evidenceReference) ?: continue
            freshlyVerified += currentItem to freshContent
        }

        // QMD's own ranked order among rankedTokens is preserved among survivors -- freshlyVerified is
        // built by iterating rankedTokens in order and never re-sorted.
        return freshlyVerified
    }

    // Identical rule to DefaultKnowledgeRetrieval.isRetrievable, duplicated deliberately rather than
    // shared, since DefaultKnowledgeRetrieval is not modified by this design.
    private fun isRetrievable(item: KnowledgeItem, query: KnowledgeRetrievalQuery): Boolean =
        item.status == KnowledgeItemStatus.ACTIVE || query.includeRetired

    // Mirrors DefaultKnowledgeCandidateEvaluator.resolve() exactly for the two content-bearing
    // reference kinds -- null on denial, absence, a non-ACTIVE Memory Core record status (the binding
    // gate, Contract Design Section 5), or an unsupported record kind; never fabricated.
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

    // Content normalization (Contract Design Section 5): CRLF and lone CR both become LF, once,
    // before matching. No trim, no whitespace collapse, no other transformation.
    private fun normalize(raw: String): String = raw.replace("\r\n", "\n").replace('\r', '\n')

    // Identical algorithm to DefaultKnowledgeRetrieval.disclosureFor, duplicated for the same reason
    // as isRetrievable, above.
    private fun disclosureFor(item: KnowledgeItem): StalenessDisclosure {
        val lastClassifiedAt = item.history.filterIsInstance<KnowledgePromotion>()
            .lastOrNull()?.occurredAt ?: return StalenessDisclosure.INDETERMINATE
        val elapsed = Duration.between(lastClassifiedAt, clock.instant())
        return if (elapsed > POSSIBLY_STALE_AFTER) {
            StalenessDisclosure.POSSIBLY_STALE
        } else {
            StalenessDisclosure.INDETERMINATE
        }
    }

    private fun isAuthorised(decision: PermissionDecision): Boolean =
        decision.decision == PermissionDecisionOutcome.APPROVED ||
            decision.decision == PermissionDecisionOutcome.APPROVED_WITH_CONFIRMATION

    private fun itemLevelIntent(item: KnowledgeItem): String =
        "Disclose Knowledge Item '${item.knowledgeId.value}' in a Reasoning Context Knowledge result"

    // Shared ExecutionRequest construction for both granularities -- they differ only in intent,
    // mirroring DefaultKnowledgeRetrieval.buildExecutionRequest's own identical shape, plus this
    // design's own frozen authorizationPurpose (Contract Design Section 7). correlationId is always
    // the caller-supplied KnowledgeRetrievalQuery.correlationId, never freshly minted; requestId is
    // freshly minted once per evaluation.
    private fun buildExecutionRequest(
        requestingPrincipalId: PrincipalId,
        correlationId: String,
        intent: String,
    ): ExecutionRequest {
        return ExecutionRequest(
            requestId = RequestId("reasoning-knowledge-source-${UUID.randomUUID()}"),
            principalId = requestingPrincipalId,
            origin = RequestOrigin.REMOTE_INTERFACE,
            intent = intent,
            targetResources = listOf(REASONING_CONTEXT_RETRIEVAL_RESOURCE_ID),
            proposedActions = listOf(RETRIEVE_FOR_REASONING_CONTEXT_ACTION_NAME),
            priority = RequestPriority.NORMAL,
            createdAt = Instant.now(),
            correlationId = correlationId,
            authorizationPurpose = authorizationPurpose,
        )
    }

    companion object {
        /** Thirty days -- identical bound to [DefaultKnowledgeRetrieval.POSSIBLY_STALE_AFTER]. */
        private val POSSIBLY_STALE_AFTER: Duration = Duration.ofDays(30)

        /** Reuses [DefaultKnowledgeRetrieval.KNOWLEDGE_RETRIEVAL_RESOURCE_ID] unchanged -- no new Resource. */
        val REASONING_CONTEXT_RETRIEVAL_RESOURCE_ID: ResourceId = DefaultKnowledgeRetrieval.KNOWLEDGE_RETRIEVAL_RESOURCE_ID

        /** A genuinely new verb, never a narrowing of [DefaultKnowledgeRetrieval.RETRIEVE_ACTION_NAME]. */
        const val RETRIEVE_FOR_REASONING_CONTEXT_ACTION_NAME: String = "knowledge.retrieve_for_reasoning_context"

        /** The ExecutionRequest.intent the act-level gate always supplies -- fixed, not query-specific. */
        const val ACT_LEVEL_INTENT: String = "Authorise Reasoning Context Knowledge Retrieval for a requesting principal"
    }
}
