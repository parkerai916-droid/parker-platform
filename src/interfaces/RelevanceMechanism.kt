package parker.core.interfaces

/**
 * Programme 3, Unit 9.7.1 (Relevance Contract Types). The opaque,
 * request-scoped candidate identifier a Bounded Semantic Relevance
 * mechanism receives and returns tokens against, per
 * `docs/governance/PROGRAMME_3_UNIT_9_7_BOUNDED_SEMANTIC_RELEVANCE_IMPLEMENTATION_PLAN.md`
 * §7.3 ("A new, request-scoped identifier type... never derived from or
 * convertible to `KnowledgeId`... is minted fresh for each member of the
 * closed candidate set") and Frozen Boundary #4 ("Opaque request-scoped
 * identifiers").
 *
 * Deliberately not [KnowledgeId], and deliberately carrying no field a
 * caller could derive a [KnowledgeId] from: a single, blank-rejecting
 * `String` value with no structural or semantic relationship to
 * Knowledge Memory's own identifier space. The mapping from a token back
 * to the canonical item it stands in for is held only by whichever later
 * unit mints it (Unit 9.7.2's own fallback coordinator; RKS.2's own
 * equivalent for `DefaultReasoningKnowledgeSource`) -- method-scoped,
 * never persisted, and deliberately external to this type itself (§7.3).
 *
 * Equality and hashing are the default `value class` structural equality
 * Kotlin already provides; no additional identity semantics are declared
 * here, since none are required by the governance this Unit implements.
 */
@JvmInline
value class RelevanceCandidateToken(val value: String) {
    init {
        require(value.isNotBlank()) { "RelevanceCandidateToken must not be blank" }
    }
}

/**
 * One member of the closed, Parker-supplied candidate set a
 * [RelevanceMechanism] is permitted to see, per
 * `PROGRAMME_3_UNIT_9_7_BOUNDED_SEMANTIC_RELEVANCE_IMPLEMENTATION_PLAN.md`
 * §7.3 ("held only in a local, method-scoped mapping... from token to the
 * item's own minimum normalised content") and the adopted
 * `PROGRAMME_3_UNIT_9_7_BOUNDED_SEMANTIC_RELEVANCE_CONTRACT_AND_PERMISSION_SUCCESSOR.md`
 * §4.1 ("Only the resulting authorised candidates' minimally necessary
 * content may cross into the relevance mechanism").
 *
 * [token] identifies this candidate for the lifetime of one
 * [RelevanceMechanism.rank] call only; [content] is the same minimum
 * normalised text Parker's own existing structural matching already
 * isolates (`matches()`'s own `basis` field, per §7.3 -- "no widening of
 * what content is ever exposed"). Deliberately absent: any [KnowledgeId],
 * evidential-state, provenance, permission, or lifecycle field of any
 * kind -- none of those may cross the mechanism boundary (Frozen Boundary
 * #6; Successor §4.2).
 */
data class RelevanceCandidate(
    val token: RelevanceCandidateToken,
    val content: String,
) {
    init {
        require(content.isNotBlank()) { "RelevanceCandidate.content must not be blank" }
    }
}

/**
 * What a [RelevanceMechanism] receives for one fallback relevance
 * computation -- the authorised query text and the closed, Parker-supplied
 * candidate set, and nothing else (Successor §3, conditions 4 and 6;
 * §4.1's single "Fallback relevance computation" responsibility).
 *
 * [queryText] is the same authorised relevance text Parker's own
 * structural matching was already given for the same call -- this type
 * introduces no separate or additional query concept of its own.
 * [candidates] is required to carry distinct tokens: Parker mints one
 * fresh [RelevanceCandidateToken] per eligible item (§7.3), so a request
 * carrying a repeated token would already indicate a defect in whichever
 * caller assembled it, not a case this contract represents as valid
 * input.
 */
data class RelevanceRequest(
    val queryText: String,
    val candidates: List<RelevanceCandidate>,
) {
    init {
        require(queryText.isNotBlank()) { "RelevanceRequest.queryText must not be blank" }
        val tokens = candidates.map { it.token }
        require(tokens.size == tokens.distinct().size) {
            "RelevanceRequest.candidates must not repeat a RelevanceCandidateToken"
        }
    }
}

/**
 * What a [RelevanceMechanism] returns from one fallback relevance
 * computation: an ordering over, or subset of, the exact
 * [RelevanceCandidateToken] values it was given in the corresponding
 * [RelevanceRequest] -- never a new token, never content, never a
 * permission, lifecycle, or evidential-state assertion
 * (`PROGRAMME_3_UNIT_9_7_BOUNDED_SEMANTIC_RELEVANCE_IMPLEMENTATION_PLAN.md`
 * §7.4; Frozen Boundary #6).
 *
 * [rankedTokens] is deliberately a plain, ordered `List`: position
 * expresses the mechanism's own relevance ordering (most relevant
 * first), and omission expresses exclusion -- a token this list does not
 * carry is, by construction, not part of the result. A successful
 * computation that finds nothing relevant is represented by an empty
 * list here, exactly as `KnowledgeRetrievalResult.entries` already
 * represents a successful, empty structural match -- a genuine,
 * distinguishable outcome from a mechanism failure, which this contract
 * never represents as a value of this type at all: per the adopted
 * Successor §3 condition 12, a failing mechanism fails closed by
 * propagating a thrown exception from [RelevanceMechanism.rank], never by
 * returning a [RelevanceResult] that merely looks empty. This mirrors
 * this codebase's own established "faults propagate as thrown
 * exceptions, never absorbed" discipline, already relied on by
 * [KnowledgeRetrievalDisposition] and `DefaultKnowledgeSubmission`.
 *
 * This type declares no facility for verifying that [rankedTokens] only
 * repeats tokens the originating [RelevanceRequest] actually supplied, or
 * that it contains no repeated token: per
 * `PROGRAMME_3_UNIT_9_7_BOUNDED_SEMANTIC_RELEVANCE_IMPLEMENTATION_PLAN.md`
 * §7.5, resolving each returned token back to its own canonical item, and
 * rejecting an unknown, repeated, or otherwise malformed token as an
 * integrity fault, is a later unit's own resolver responsibility (Unit
 * 9.7.2's own fallback coordinator; RKS.3's own equivalent) --
 * deliberately out of scope for this Unit's own properties-level
 * contract.
 */
data class RelevanceResult(
    val rankedTokens: List<RelevanceCandidateToken>,
)

/**
 * Programme 3, Unit 9.7.1 (Relevance Contract Types). The shared,
 * implementation-neutral relevance-mechanism interface
 * `PROGRAMME_3_UNIT_9_7_BOUNDED_SEMANTIC_RELEVANCE_IMPLEMENTATION_PLAN.md`
 * §8 requires -- "the relevance-mechanism interface itself, at the
 * properties level only -- no concrete backing implementation." No class
 * implements this interface yet: mechanism selection is Unit 9.7's own
 * §13 spike, and a concrete adapter is Unit 9.7.3 -- both explicitly out
 * of scope for this Unit.
 *
 * One operation, deliberately -- mirroring [KnowledgeSource]'s own "one
 * operation, deliberately" precedent. [rank] receives only a
 * [RelevanceRequest] (query text plus the closed candidate set) and
 * returns only a [RelevanceResult] (an ordering or subset of the exact
 * tokens supplied). By omission -- not by comment, prompt, or policy
 * statement -- this interface is never given a `PermissionEngine`
 * reference, a `KnowledgeItemPersistence`/`MemoryRetrieval` reference, or
 * any handle capable of reaching canonical storage
 * (`PROGRAMME_3_UNIT_9_7_BOUNDED_SEMANTIC_RELEVANCE_IMPLEMENTATION_PLAN.md`
 * §7.4; Frozen Boundary #9), mirroring the same "architectural inability
 * by omission" technique [KnowledgeSource] and `DefaultKnowledgeRetrieval`
 * already establish elsewhere in this codebase. Its declared signature is
 * consequently structurally incapable of expressing a write, a
 * permission decision, or a lifecycle/evidential-state assertion of any
 * kind -- there is no parameter or return path through which one could be
 * passed.
 *
 * `suspend`, for the same reason [KnowledgeSource.recall] and
 * [KnowledgePromotionPolicy.evaluate] both are: a future concrete
 * mechanism (Unit 9.7.3) may need to perform local, in-process
 * computation this signature must not foreclose by being declared
 * non-suspending before any real implementation exists.
 *
 * Deliberately reusable, unchanged, by both governed retrieval surfaces
 * `PROGRAMME_3_UNIT_9_7_BOUNDED_SEMANTIC_RELEVANCE_IMPLEMENTATION_PLAN.md`
 * and
 * `REASONING_KNOWLEDGE_SOURCE_BOUNDED_SEMANTIC_RELEVANCE_IMPLEMENTATION_PLAN.md`
 * both depend on: `DefaultKnowledgeRetrieval` (Unit 9.7.2 onward) and
 * `DefaultReasoningKnowledgeSource` (RKS.1 onward, per that plan's own
 * strict compatibility-gate requirement -- RKS.1 holds no authority to
 * modify or extend this interface). No mechanism-selection, algorithm, or
 * wiring decision is made by this Unit; declaring this interface does not
 * itself enable semantic retrieval anywhere in Parker.
 *
 * **Deliberately absent: mechanism identity, version, or configuration.**
 * The adopted Successor §3 condition 14 requires that state to be
 * "frozen, disclosed, retrieval-relevant state" once a mechanism exists,
 * but this Unit's own Implementation Plan §8 entry limits Unit 9.7.1 to
 * "the opaque request-scoped token type, the minimal candidate/result
 * shapes... and the relevance-mechanism interface itself" and names only
 * Frozen Boundaries #3, #4, #6, and #9 as this Unit's own governance
 * properties -- not the determinism/version boundary. Identity, version,
 * and configuration are inescapably properties of a concrete mechanism,
 * which does not exist until Unit 9.7.3 (after the §13 selection spike);
 * declaring a field or type for them here would require presupposing a
 * shape for a decision this Unit is explicitly not authorised to make.
 * That representation is left to Unit 9.7.3's own adapter.
 */
fun interface RelevanceMechanism {
    suspend fun rank(request: RelevanceRequest): RelevanceResult
}
