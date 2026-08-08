package parker.core.runtime

import java.time.Instant
import kotlinx.coroutines.runBlocking
import parker.core.interfaces.EvidentialState
import parker.core.interfaces.KnowledgeCandidate
import parker.core.interfaces.KnowledgeCandidateEvaluation
import parker.core.interfaces.KnowledgeCandidateEvaluator
import parker.core.interfaces.KnowledgeId
import parker.core.interfaces.KnowledgeItem
import parker.core.interfaces.KnowledgeItemStatus
import parker.core.interfaces.KnowledgePromotion
import parker.core.interfaces.MemoryCoreRecordReference
import parker.core.interfaces.MemoryRetrieval
import parker.core.interfaces.PrincipalId
import parker.core.interfaces.ProvenanceId
import parker.core.interfaces.ProvenanceReference
import parker.core.interfaces.Relationship
import parker.core.interfaces.RelationshipEndpoint
import parker.core.interfaces.RelationshipTraversalDirection
import parker.core.interfaces.RelationshipTraversalQuery

/**
 * Programme 3, Knowledge Memory, Implementation Unit 6 (Constitutional
 * Knowledge Promotion Pipeline). The concrete
 * [KnowledgeCandidateEvaluator] this Unit supplies.
 *
 * **Corrected against the frozen Contract Design Version 2 §16 (Phase 1
 * Amendment 5 and its extension, §§16.9-16.13).** This class previously
 * promoted unconditionally once a Memory Core record resolved, never
 * rejecting for evidential weakness, and assigned
 * [EvidentialState.CORROBORATED_EVIDENCE] from a single `SUPPORTS`
 * relationship alone. Both behaviours were found non-compliant with
 * Contract Design Version 2 §5 and
 * `docs/governance/PROGRAMME_3_UNIT_6_SCOPE_LOCK_CLARIFICATION.md`
 * §§6-8 (the Constitutional Reconciliation's Issues 1, 3, 4, and 6) and
 * are corrected below. See that Clarification for the full reasoning
 * behind every design decision this class makes; only a summary is
 * restated here.
 *
 * ## Structural prerequisites, checked first, never presented as evidential weighing
 *
 * Exactly one prerequisite exists: the referenced Memory Core record must
 * resolve. [memoryRetrieval] performs no principal-based filtering itself
 * (its own KDoc) -- a future permission-filtering decorator composed in
 * front of it (Unit 8's own responsibility) would surface a denial as a
 * resolution failure here, indistinguishable from non-existence. This
 * class does not, and structurally cannot, tell the two apart, which is
 * the correct behaviour per Contract Design Version 2 Section 7:
 * Evaluation A (Memory Core's own access decision) is never re-litigated
 * at this boundary. Provenance presence is a second structural
 * prerequisite Contract Design Version 2 names, but it requires no
 * runtime check here -- every resolved [parker.core.interfaces.Entity],
 * [parker.core.interfaces.Document], [parker.core.interfaces.Assertion],
 * and [parker.core.interfaces.Relationship] already carries a mandatory,
 * non-nullable `provenanceId` field, enforced at construction by Memory
 * Core itself. Structural resolvability is only a prerequisite for
 * evaluating the promotion criteria below -- it is never, by itself,
 * sufficient to justify promotion.
 *
 * ## The genuine promotion gate -- a corrected, narrower, disclosed baseline
 *
 * Of Contract Design Version 2 §5's six named promotion-criteria
 * categories (repetition, importance, relevance, frequency, confidence,
 * explicit request), only two presently have any constitutionally
 * authorised source reachable by this evaluator
 * (`docs/reviews/PROGRAMME_3_KNOWLEDGE_PROMOTION_FACTOR_PROVENANCE.md`;
 * Contract Design Version 2 §16.10):
 *
 * 1. **Confidence** -- available only for `Assertion`-kind evidence
 *    ([parker.core.interfaces.Assertion.confidence]), and only where
 *    recorded. This factor counts as *reachable* whenever a value is
 *    present, regardless of its magnitude -- Unit 6 Scope Lock
 *    Clarification §6 forbids introducing "numeric weights, scores,
 *    thresholds, or fixed quotas" for any named factor, so a low recorded
 *    value is weighed exactly like a high one; only genuine absence
 *    (`null`) is treated as unreachable. No confidence value is ever
 *    invented, inferred, or synthesised.
 *
 *    **This is a shallow reachability check, not magnitude-based
 *    evidential weighing, and that limitation is disclosed rather than
 *    implied.** Confidence contributes to the gate through its authorised
 *    *presence* alone; its numeric value is read and disclosed in the
 *    basis for transparency, but is never compared, ranked, or weighed
 *    against any threshold -- because frozen governance forbids inventing
 *    one (Unit 6 Scope Lock Clarification §6), presence is the only
 *    non-arbitrary, deterministic criterion available, not a stand-in for
 *    genuine magnitude-based evaluation.
 * 2. **Explicit request** -- [KnowledgeCandidate.explicitlyRequested],
 *    authorised by Contract Design Version 2 §16. Counts as reachable
 *    only when the caller reports `true`; `null` (genuinely undetermined)
 *    and `false` (confirmed not explicit) are both treated as
 *    unreachable for gate purposes, though the actual reported value is
 *    always disclosed honestly in the basis, never coerced to the other.
 *
 * **Repetition, frequency, and user importance remain unreachable** and
 * are never simulated -- repetition/frequency require a Memory Core
 * record-comparison capability that is constitutionally authorised
 * (`docs/architecture/MEMORY_CORE_SCOPE_LOCK.md` §18) but not yet
 * implemented (a Programme 2 responsibility, out of this Unit's scope);
 * user importance has no defined mechanism or owner anywhere in reviewed
 * governance. **Relevance is not evaluated here at all**, consistent
 * with its own, separate assignment to Knowledge Retrieval/Reasoning
 * Context.
 *
 * Contract Design Version 2 §5 requires weighing to consider more than
 * one factor -- no single factor may, by itself, determine promotion,
 * absent an express, documented exception. Since only two named factors
 * are presently reachable at all, this evaluator's gate requires **both**
 * to be satisfied: confidence reachable (non-null) **and** explicit
 * request reachable (`true`). Where fewer than two are satisfied, the
 * candidate is rejected for insufficient promotion basis -- this is a
 * genuine, disclosed, non-placeholder narrower baseline (mirroring
 * `docs/architecture/IMPLEMENTATION_GAPS.md` Gap #46's own already-
 * legitimised precedent for a narrower factor set), not a workaround. A
 * direct, disclosed consequence: `Entity`, `Document`, and `Relationship`
 * evidence, which never carries a confidence field, cannot presently
 * satisfy this gate through confidence at all, and therefore cannot be
 * promoted by this evaluator absent the one express exception below.
 *
 * ## Corroboration -- detected, disclosed, never relied upon
 *
 * A `SUPPORTS`-typed [Relationship] naming the resolved record as an
 * endpoint is still detected (it remains constitutionally relevant per
 * Contract Design Version 2 §5 and Unit 6 Scope Lock Clarification §6),
 * but it is never used to satisfy the promotion gate and never used to
 * assign [EvidentialState.CORROBORATED_EVIDENCE]. Article XI's
 * independence requirement (`docs/architecture/epistemic-integrity.md`)
 * requires a common-origin determination before any supporting account
 * may be treated as independent corroboration, and no authorised Memory
 * Core capability presently performs that determination
 * (`docs/reviews/PROGRAMME_3_BLOCKER_OWNERSHIP_MATRIX.md`, "Common-origin
 * determination"). This class does not simulate one. Where a `SUPPORTS`
 * relationship is detected, that fact is disclosed in the basis as a
 * signal not presently relied upon, preserving it for a future,
 * separately authorised enrichment once a common-origin capability
 * exists, rather than silently discarding it.
 *
 * ## Contradiction -- its own, independent, express exception
 *
 * A `CONTRADICTS`- or `DISPUTES`-typed relationship remains the one
 * express, documented exception to the multi-factor gate (Contract
 * Design Version 2 Section 3's Contradiction paragraph; Unit 6 Scope
 * Lock Clarification §8). Where detected, the candidate is promoted with
 * [EvidentialState.COMPETING_EXPLANATIONS] regardless of whether the
 * two-factor gate below is otherwise satisfied -- Contract Design
 * Version 2 requires an honest, unresolved disclosure rather than a
 * silent omission of one side, and this exception licenses only that
 * disclosure, never a resolution in favour of one side, and never any
 * other classification.
 *
 * ## Why this evaluator never assigns `EvidentialState.INDETERMINATE`
 *
 * Article IV distinguishes [EvidentialState.UNKNOWN] ("insufficient but
 * potentially knowable, given further evidence") from
 * [EvidentialState.INDETERMINATE] ("it cannot presently be established
 * whether the matter is knowable at all" -- strictly weaker than
 * [EvidentialState.UNKNOWN]). This evaluator never assigns
 * [EvidentialState.INDETERMINATE], because no currently authorised and
 * reachable factor distinguishes these two cases from one another --
 * confidence, explicit request, and the disclosed-but-unrelied-upon
 * corroboration signal all bear on whether a proposition is
 * *presently* well-supported, never on whether it is even
 * *knowable in principle*. Absent such a factor, assigning
 * [EvidentialState.INDETERMINATE] would fabricate a distinction this
 * evaluator has no basis to draw; [EvidentialState.UNKNOWN] is the only
 * one of the two mandatory insufficiently-supported states this
 * evaluator can honestly assign.
 *
 * ## No persistence
 *
 * This class never calls [parker.core.interfaces.KnowledgeStore.remember],
 * never writes to [InMemoryKnowledgeStore], and never performs a
 * lifecycle transition. [KnowledgeItem] and [KnowledgePromotion] values
 * returned by [evaluate] are proposed, constructed values only -- durable
 * storage remains a later unit's own, separately authorised
 * responsibility (Unit 6 Clarification Section 4).
 *
 * ## `KnowledgeId` minting, a disclosed implementation choice
 *
 * No frozen document assigns identifier-minting authority to a
 * non-persisting evaluator -- [KnowledgeId] is ordinarily "assigned once,
 * by [parker.core.interfaces.KnowledgeStore], at submission"
 * (`KnowledgeStore.kt`'s own KDoc). Since this class stores nothing, it
 * derives a deterministic identifier directly from the candidate's own
 * [MemoryCoreRecordReference] rather than minting a fresh, non-repeatable
 * one -- evaluating the same candidate twice yields the same
 * [KnowledgeId].
 *
 * ## Determinism, disclosed precisely
 *
 * [KnowledgeId] generation, the promotion-versus-rejection outcome, and
 * the assigned [EvidentialState] classification are all deterministic
 * for identical authoritative inputs (the same stored Memory Core state
 * and the same candidate) -- evaluating the same candidate twice yields
 * the same identity, the same outcome, and the same classification.
 * [KnowledgePromotion.occurredAt], by contrast, is produced from a
 * wall-clock read (`Instant.now()`) and is not, and is not required to
 * be, deterministic -- timestamps may legitimately differ across
 * repeated evaluations of the same candidate. This follows existing
 * project convention (the same pattern `DefaultKnowledgePromotionPolicy`
 * and other event-timestamped types in this codebase already use) and
 * does not weaken, and is entirely separate from, the deterministic
 * identity and classification guarantees above.
 *
 * ## Bridging a `suspend` dependency from a non-`suspend` interface
 *
 * [KnowledgeCandidateEvaluator.evaluate] is deliberately not `suspend`,
 * per this Unit's own authorised interface signature, while every
 * [MemoryRetrieval] method is `suspend`. [evaluate] bridges this with
 * [runBlocking] -- a disclosed, pragmatic accommodation of the fixed
 * interface shape, not a constitutional decision.
 */
class DefaultKnowledgeCandidateEvaluator(
    private val memoryRetrieval: MemoryRetrieval,
) : KnowledgeCandidateEvaluator {

    override fun evaluate(
        requestingPrincipalId: PrincipalId,
        candidate: KnowledgeCandidate,
    ): KnowledgeCandidateEvaluation = runBlocking {
        val resolved = resolve(requestingPrincipalId, candidate.evidenceReference)
            ?: return@runBlocking KnowledgeCandidateEvaluation.Reject(
                "the referenced Memory Core record could not be resolved -- either it does not exist, or " +
                    "access to it was not authorised; Knowledge Memory does not distinguish the two cases here, " +
                    "since Evaluation A's own outcome (Contract Design Version 2 Section 7) is never re-litigated " +
                    "at this boundary",
            )

        val relationships = memoryRetrieval.traverseRelationships(
            RelationshipTraversalQuery(
                requestingPrincipalId = requestingPrincipalId,
                maximumResults = MAX_RELATIONSHIP_RESULTS,
                startingEndpoint = RelationshipEndpoint(resolved.recordKind, resolved.recordId),
                direction = RelationshipTraversalDirection.BOTH,
            ),
        )

        val contradicted = relationships.any {
            it.relationshipType == Relationship.CONTRADICTS || it.relationshipType == Relationship.DISPUTES
        }
        val corroborationDetected = relationships.any { it.relationshipType == Relationship.SUPPORTS }

        val knowledgeId = KnowledgeId("candidate-${resolved.recordKind}-${resolved.recordId}")
        val provenanceReference = ProvenanceReference(resolved.provenanceId)

        val corroborationDisclosure = if (corroborationDetected) {
            "; a SUPPORTS relationship was found for the referenced record, but is not relied upon here -- " +
                "Article XI's independence requirement requires a common-origin determination before any " +
                "supporting account may be treated as corroboration, and no authorised Memory Core capability " +
                "presently performs that determination " +
                "(docs/reviews/PROGRAMME_3_BLOCKER_OWNERSHIP_MATRIX.md); this evaluator does not simulate one, " +
                "and preserves this detection for a future, authorised enrichment rather than discarding it"
        } else {
            ""
        }

        if (contradicted) {
            return@runBlocking promote(
                candidate = candidate,
                knowledgeId = knowledgeId,
                provenanceReference = provenanceReference,
                state = EvidentialState.COMPETING_EXPLANATIONS,
                basis = "the referenced ${resolved.recordKind} participates in an unresolved CONTRADICTS/DISPUTES " +
                    "relationship in Memory Core; Contract Design Version 2 Section 3 (Contradiction) requires " +
                    "this be promoted with an honest, unresolved classification rather than rejected or resolved " +
                    "silently in favour of one side -- this is the one express, documented exception to the " +
                    "multi-factor promotion gate, and it applies independently of that gate" + corroborationDisclosure,
            )
        }

        // Parker Conversational Memory Bridge, Admission Unit
        // (docs/governance/PROGRAMME_3_EXPLICIT_OWNER_INSTRUCTION_PROMOTION_EXCEPTION_SCOPE_LOCK_CLARIFICATION.md).
        // The one, narrow, express single-factor exception that Clarification authorises -- checked
        // after Contradiction (an unresolved contradiction is still disclosed honestly even for an
        // explicit-instruction candidate) and before the ordinary two-factor gate. Always assigns
        // EvidentialState.UNKNOWN, never a stronger state (Clarification Guarantee 3) -- exactly the
        // same, weakest classification the two-factor path below already assigns, never more
        // generous. soleBasisIsExplicitInstruction is deliberately distinct from explicitlyRequested
        // (KnowledgeStore.kt's own KDoc) -- this branch never fires merely because explicitlyRequested
        // is true.
        if (candidate.soleBasisIsExplicitInstruction == true) {
            return@runBlocking promote(
                candidate = candidate,
                knowledgeId = knowledgeId,
                provenanceReference = provenanceReference,
                state = EvidentialState.UNKNOWN,
                basis = "promoted solely because of an explicit, deterministic owner instruction to remember this; " +
                    "no independent evidential weight was established" + corroborationDisclosure,
            )
        }

        val confidenceReachable = resolved.confidence != null
        val explicitRequestReachable = candidate.explicitlyRequested == true
        val reachableFactors = listOfNotNull(
            "confidence (${resolved.confidence})".takeIf { confidenceReachable },
            "explicit request".takeIf { explicitRequestReachable },
        )

        if (reachableFactors.size < 2) {
            return@runBlocking KnowledgeCandidateEvaluation.Reject(
                "insufficient promotion basis -- of the presently authorised, reachable promotion factors " +
                    "(confidence, explicit request), only ${reachableFactors.size} could be established for this " +
                    "candidate (reachable: ${if (reachableFactors.isEmpty()) "none" else reachableFactors.joinToString()}; " +
                    "recorded confidence: ${resolved.confidence}; reported explicit request: ${candidate.explicitlyRequested}); " +
                    "Contract Design Version 2 Section 5 requires more than one factor to be jointly weighed, and " +
                    "no single factor may determine promotion absent an express, documented exception, none of " +
                    "which applies here; repetition, frequency, and user importance remain unreachable pending " +
                    "separate governance (docs/reviews/PROGRAMME_3_BLOCKER_OWNERSHIP_MATRIX.md); this rejection " +
                    "does not assert that the underlying Memory Core evidence is false or invalid" + corroborationDisclosure,
            )
        }

        promote(
            candidate = candidate,
            knowledgeId = knowledgeId,
            provenanceReference = provenanceReference,
            state = EvidentialState.UNKNOWN,
            basis = "promotion is justified by two jointly-weighed, currently authorised factors: recorded " +
                "confidence (${resolved.confidence}) and a caller-reported explicit request " +
                "(${candidate.explicitlyRequested}), consistent with Contract Design Version 2 Section 5's " +
                "multi-factor requirement and neither treated as sufficient alone; the referenced record carries " +
                "no CONTRADICTS/DISPUTES relationship, and no reliable corroboration signal is presently " +
                "available (see below), so the evidential-state classification honestly reflects that the " +
                "available evidence does not presently justify a stronger state, though the matter may become " +
                "knowable with further evidence (Article IV)" + corroborationDisclosure,
        )
    }

    private fun promote(
        candidate: KnowledgeCandidate,
        knowledgeId: KnowledgeId,
        provenanceReference: ProvenanceReference,
        state: EvidentialState,
        basis: String,
    ): KnowledgeCandidateEvaluation.Promote {
        val promotion = KnowledgePromotion(
            knowledgeId = knowledgeId,
            evidenceReference = candidate.evidenceReference,
            resultingState = state,
            occurredAt = Instant.now(),
            basis = basis,
        )
        val item = KnowledgeItem(
            knowledgeId = knowledgeId,
            evidenceReference = candidate.evidenceReference,
            provenanceReference = provenanceReference,
            evidentialState = state,
            status = KnowledgeItemStatus.ACTIVE,
            history = listOf(promotion),
        )
        return KnowledgeCandidateEvaluation.Promote(item, promotion)
    }

    private suspend fun resolve(
        requestingPrincipalId: PrincipalId,
        reference: MemoryCoreRecordReference,
    ): ResolvedRecord? = when (reference) {
        is MemoryCoreRecordReference.ToEntity ->
            memoryRetrieval.getEntity(requestingPrincipalId, reference.entityId)?.let {
                ResolvedRecord(RelationshipEndpoint.ENTITY, reference.entityId.value, it.provenanceId, confidence = null)
            }

        is MemoryCoreRecordReference.ToDocument ->
            memoryRetrieval.getDocument(requestingPrincipalId, reference.documentId)?.let {
                ResolvedRecord(RelationshipEndpoint.DOCUMENT, reference.documentId.value, it.provenanceId, confidence = null)
            }

        is MemoryCoreRecordReference.ToAssertion ->
            memoryRetrieval.getAssertion(requestingPrincipalId, reference.assertionId)?.let {
                ResolvedRecord(RelationshipEndpoint.ASSERTION, reference.assertionId.value, it.provenanceId, confidence = it.confidence)
            }

        is MemoryCoreRecordReference.ToRelationship ->
            memoryRetrieval.getRelationship(requestingPrincipalId, reference.relationshipId)?.let {
                ResolvedRecord(RelationshipEndpoint.RELATIONSHIP, reference.relationshipId.value, it.provenanceId, confidence = null)
            }
    }

    /**
     * Purely internal to this class -- never exposed, never a public
     * contract. Carries exactly what [resolve] learns about whichever
     * Memory Core record kind [MemoryCoreRecordReference] named:
     * [recordKind]/[recordId] (for building the [RelationshipEndpoint]
     * this class queries relationships against), [provenanceId] (for
     * [ProvenanceReference] construction), and [confidence] (present only
     * when the resolved record is an [parker.core.interfaces.Assertion]).
     */
    private data class ResolvedRecord(
        val recordKind: String,
        val recordId: String,
        val provenanceId: ProvenanceId,
        val confidence: Double?,
    )

    companion object {
        /**
         * A pagination bound for the one relationship-traversal query this
         * class issues per evaluation -- a structural query parameter, not
         * an evidential weighting figure; mirrors every other mandatory,
         * positive [RelationshipTraversalQuery.maximumResults] bound
         * elsewhere in this codebase.
         */
        const val MAX_RELATIONSHIP_RESULTS = 200
    }
}
