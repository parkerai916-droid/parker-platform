package parker.core.runtime

import java.time.Instant
import kotlinx.coroutines.runBlocking
import parker.core.interfaces.EvidentialState
import parker.core.interfaces.KnowledgeItem
import parker.core.interfaces.KnowledgePromotion
import parker.core.interfaces.KnowledgeRevisionEvaluation
import parker.core.interfaces.KnowledgeRevisionEvaluator
import parker.core.interfaces.MemoryCoreRecordReference
import parker.core.interfaces.MemoryCoreRecordStatus
import parker.core.interfaces.MemoryRetrieval
import parker.core.interfaces.PrincipalId
import parker.core.interfaces.ProvenanceId
import parker.core.interfaces.ProvenanceReference
import parker.core.interfaces.Relationship
import parker.core.interfaces.RelationshipEndpoint
import parker.core.interfaces.RelationshipTraversalDirection
import parker.core.interfaces.RelationshipTraversalQuery

/**
 * Programme 3, Knowledge Memory, Implementation Unit 7.2 (Knowledge
 * Revision and Supersession Evaluation). The concrete
 * [KnowledgeRevisionEvaluator] this Unit supplies.
 *
 * ## Scope -- what this class does and does not do
 *
 * This class implements exactly: revision qualification (this file's own
 * "Revision Qualification Gate" below), supersession qualification (the
 * same gate's `SUPERSEDES`/`SUPERSEDED` case, requiring no separate code
 * path), revision evaluation (the resulting [EvidentialState]), appending
 * exactly one new [KnowledgePromotion] to a returned [KnowledgeItem]
 * value, and the three-outcome [KnowledgeRevisionEvaluation] result. It
 * does **not** implement retirement, restoration, persistence,
 * compare-and-append, lifecycle-sequence enforcement, optimistic
 * concurrency, any Memory Core mutation, Permission Engine wiring (Unit
 * 8), or retrieval/staleness disclosure (Unit 9) -- all remain later
 * units' own, separately authorised responsibility
 * (`docs/governance/PROGRAMME_3_UNIT_7_SCOPE_LOCK_CLARIFICATION.md` §4).
 *
 * ## Revision Qualification Gate (Scope Lock Clarification §7), corrected
 *
 * A submitted [newEvidenceReference] qualifies as a revision of [item]
 * only where at least one of the following holds, checked against
 * [item]'s own already-cited [KnowledgeItem.evidenceReference]:
 *
 * 1. **Directional case.** An `AMENDS` or `SUPERSEDES` [Relationship]
 *    runs *from* the newly submitted evidence *to* the already-cited
 *    evidence -- `relationship.fromEndpoint` naming the new evidence and
 *    `relationship.toEndpoint` naming the already-cited evidence, never
 *    the reverse. Both relationship types are inherently directional
 *    (`MemoryCore.kt`'s own amendment-process KDoc: "create the
 *    replacement, record an `AMENDS`-typed `Relationship`... call this
 *    operation to transition the original to `SUPERSEDED`" -- the
 *    replacement is always the `fromEndpoint`); a relationship of either
 *    type stored in the opposite orientation does not qualify, and is
 *    disclosed as detected-but-misdirected rather than silently ignored.
 * 2. **Symmetric case.** A `CONTRADICTS` or `DISPUTES` [Relationship]
 *    links the newly submitted evidence to the already-cited evidence in
 *    *either* direction -- both types are inherently mutual (a
 *    contradiction between two records is the same fact regardless of
 *    which endpoint is `fromEndpoint`), mirroring
 *    [DefaultKnowledgeCandidateEvaluator]'s own identical, already-frozen
 *    direction-agnostic treatment of these same two types.
 * 3. **Status-transition case.** The already-cited evidence's own
 *    **current** Memory Core status is exactly [MemoryCoreRecordStatus.DISPUTED]
 *    or [MemoryCoreRecordStatus.SUPERSEDED] -- the closed, two-value
 *    qualifying set. `ACTIVE` never qualifies (it is the universal
 *    starting status, never itself a transition). `ARCHIVED` does not
 *    qualify: Memory Core Scope Lock discloses archival as a reversible,
 *    retention-oriented status carrying no implication about the
 *    evidence's own reliability. `DELETED` does not qualify either: it is
 *    reserved to the owner-requested erasure path (Contract Design
 *    Version 2 Section 3), and this Unit does not infer any erasure-
 *    related meaning from it, or from non-resolution, for a revision
 *    decision -- treating erasure as an ordinary "relevant transition"
 *    would risk silently basing a revision on evidence the owner
 *    deliberately removed. No other status exists on
 *    [MemoryCoreRecordStatus]. This class does not, and cannot, observe
 *    *which* transition path was taken or *when* -- only the current
 *    status, which is all Section 7's own "relevant... transition"
 *    language requires be checked.
 *
 * No numerical materiality threshold, no heuristic scoring, and no bare
 * caller assertion is introduced or accepted -- these three, precisely
 * named checks are the entire gate, exactly as Scope Lock Clarification
 * §7 fixes it. Where none holds, the submission is not a revision:
 * [KnowledgeRevisionEvaluation.NotQualifyingRevision] is returned, no
 * event is appended, and [item] is returned to the caller unchanged (by
 * the caller simply discarding the result and keeping its own reference
 * to [item], since this class never mutates it).
 *
 * ## Supersession (Scope Lock Clarification §7's own `SUPERSEDES` case)
 *
 * Supersession is treated as an ordinary revision, never a distinct
 * outcome or event kind -- it qualifies exclusively through case 1's
 * directional `SUPERSEDES` relationship (new evidence supersedes the
 * already-cited evidence, never the reverse) or case 3's status
 * transition landing specifically on [MemoryCoreRecordStatus.SUPERSEDED].
 * This class never creates, modifies, or infers either; it only observes
 * what [MemoryRetrieval] already reports.
 *
 * ## Revision evaluation -- the resulting [EvidentialState]
 *
 * Once qualification succeeds, exactly two authorised outcomes exist,
 * mirroring [DefaultKnowledgeCandidateEvaluator]'s own reachable-factor
 * discipline applied to newly considered evidence (Scope Lock
 * Clarification §7: "the classification a qualifying revision reaches
 * remains governed by whatever evaluation logic a future Unit applies to
 * the newly considered evidence, exactly as Unit 6 governs initial
 * promotion"):
 *
 * - **[EvidentialState.COMPETING_EXPLANATIONS]** -- where a `CONTRADICTS`
 *   or `DISPUTES` relationship (specifically, not merely any qualifying
 *   relationship) links the new evidence to the already-cited evidence,
 *   mirroring Unit 6's own express, independent contradiction exception
 *   (Contract Design Version 2 Section 3); **or** where [item] already
 *   carries [EvidentialState.COMPETING_EXPLANATIONS] from an earlier
 *   event. This second condition is a deliberate, disclosed design
 *   decision, not incidental: Contract Design Version 2 Section 3 states
 *   a disclosed contradiction is "never resolved in favour of one side,"
 *   and no authorised mechanism exists anywhere in frozen governance for
 *   an unrelated, non-contradicting later revision to silently resolve
 *   or downgrade a previously disclosed, unresolved contradiction. A
 *   qualifying revision that does not itself carry a new contradiction
 *   therefore never removes an existing one.
 * - **[EvidentialState.UNKNOWN]** -- otherwise. No stronger authorised,
 *   reachable factor differentiates a stronger classification at
 *   revision time, for the identical reason [DefaultKnowledgeCandidateEvaluator]
 *   never assigns one at promotion time; this evaluator does not
 *   fabricate one either. This is the only one of the two mandatory
 *   insufficiently-supported states ([EvidentialState.UNKNOWN] and
 *   [EvidentialState.INDETERMINATE], Article IV) this class can honestly
 *   assign, for the same reason [DefaultKnowledgeCandidateEvaluator]'s
 *   own KDoc already discloses -- nothing here distinguishes "not yet
 *   knowable" from "not knowable in principle."
 *
 * A qualifying revision that reaches the same [EvidentialState] the item
 * already carried is not a no-op: [KnowledgeRevisionEvaluation.Applied]
 * is still returned, and exactly one new [KnowledgePromotion] event is
 * still appended, honestly disclosing that the evidence was reconsidered
 * and the classification did not change (Scope Lock Clarification §7's
 * No-Change Revision Rule; Epistemic Integrity Articles XVI/XVII).
 *
 * ## Corroboration -- detected, disclosed, never relied upon
 *
 * A `SUPPORTS` relationship linking the new and already-cited evidence is
 * detected and disclosed in the basis exactly as
 * [DefaultKnowledgeCandidateEvaluator] already does for initial
 * promotion, for the identical reason (Article XI's independence
 * requirement has no implemented common-origin determination) -- it is
 * never used to qualify or to influence the resulting classification.
 *
 * ## The updated [KnowledgeItem]
 *
 * Where a revision is applied, the returned [KnowledgeItem] carries:
 * [KnowledgeItem.evidenceReference] and [KnowledgeItem.provenanceReference]
 * updated to the newly qualified evidence (the record now currently
 * supporting the item, mirroring [KnowledgeItem.evidenceReference]'s own
 * "currently supporting" KDoc); [KnowledgeItem.evidentialState] updated to
 * the resulting classification above; [KnowledgeItem.status] unchanged
 * (revision never retires or restores); and [KnowledgeItem.history] equal
 * to the prior value's own history with **exactly one** new
 * [KnowledgePromotion] appended -- no prior entry is ever rewritten,
 * reordered, or removed. This class performs no persistence of any kind:
 * the returned [KnowledgeItem] and [KnowledgePromotion] are proposed,
 * constructed values only, exactly as [DefaultKnowledgeCandidateEvaluator]'s
 * own `Promote` outcome already is.
 *
 * ## Determinism, disclosed precisely
 *
 * Qualification, the resulting [EvidentialState], and the appended
 * [KnowledgePromotion]'s content are all deterministic for identical
 * authoritative inputs (the same stored Memory Core state, the same
 * [item], and the same [newEvidenceReference]). [occurredAt] is supplied
 * by the caller rather than read from the system clock internally
 * (unlike [DefaultKnowledgeCandidateEvaluator]) -- see
 * [KnowledgeRevisionEvaluator]'s own KDoc for why. This class implements
 * no ordering rule and no lifecycle-sequence value of any kind; both
 * remain a later Unit 7 sub-unit's own, separately authorised
 * responsibility (Scope Lock Clarification §10, §11).
 *
 * ## Bridging a `suspend` dependency from a non-`suspend` interface
 *
 * [KnowledgeRevisionEvaluator.evaluate] is deliberately not `suspend`,
 * mirroring [KnowledgeCandidateEvaluator.evaluate]'s own authorised
 * signature, while every [MemoryRetrieval] method is `suspend`.
 * [evaluate] bridges this with [runBlocking], the same disclosed,
 * pragmatic accommodation [DefaultKnowledgeCandidateEvaluator] already
 * uses.
 */
class DefaultKnowledgeRevisionEvaluator(
    private val memoryRetrieval: MemoryRetrieval,
) : KnowledgeRevisionEvaluator {

    override fun evaluate(
        item: KnowledgeItem,
        newEvidenceReference: MemoryCoreRecordReference,
        occurredAt: Instant,
    ): KnowledgeRevisionEvaluation = runBlocking {
        val existingResolved = resolve(item.evidenceReference)
            ?: return@runBlocking KnowledgeRevisionEvaluation.StructurallyUnresolvable(
                "this KnowledgeItem's own already-cited evidence reference could not be resolved -- either it no " +
                    "longer exists, or access to it was not authorised; this evaluator does not distinguish the " +
                    "two cases here, mirroring DefaultKnowledgeCandidateEvaluator's identical structural boundary",
            )

        val newResolved = resolve(newEvidenceReference)
            ?: return@runBlocking KnowledgeRevisionEvaluation.StructurallyUnresolvable(
                "the newly submitted evidence reference could not be resolved -- either it does not exist, or " +
                    "access to it was not authorised; this evaluator does not distinguish the two cases here, " +
                    "mirroring DefaultKnowledgeCandidateEvaluator's identical structural boundary",
            )

        val existingEndpoint = RelationshipEndpoint(existingResolved.recordKind, existingResolved.recordId)
        val newEndpoint = RelationshipEndpoint(newResolved.recordKind, newResolved.recordId)

        val relationships = memoryRetrieval.traverseRelationships(
            RelationshipTraversalQuery(
                requestingPrincipalId = SYSTEM_PRINCIPAL_ID,
                maximumResults = MAX_RELATIONSHIP_RESULTS,
                startingEndpoint = newEndpoint,
                direction = RelationshipTraversalDirection.BOTH,
            ),
        )

        val linkingRelationships = relationships.filter { otherEndpoint(it, newEndpoint) == existingEndpoint }

        val directedQualifyingTypes = linkingRelationships
            .filter { it.relationshipType in DIRECTIONAL_QUALIFYING_RELATIONSHIP_TYPES && it.fromEndpoint == newEndpoint }
            .map { it.relationshipType }
            .toSet()

        val symmetricQualifyingTypes = linkingRelationships
            .map { it.relationshipType }
            .filter { it in SYMMETRIC_QUALIFYING_RELATIONSHIP_TYPES }
            .toSet()

        val qualifyingRelationshipTypes = directedQualifyingTypes + symmetricQualifyingTypes

        val misdirectedTypes = linkingRelationships
            .filter { it.relationshipType in DIRECTIONAL_QUALIFYING_RELATIONSHIP_TYPES && it.fromEndpoint != newEndpoint }
            .map { it.relationshipType }
            .toSet()

        val statusTransitioned = existingResolved.status in QUALIFYING_STATUS_TRANSITIONS
        val corroborationDetected = linkingRelationships.any { it.relationshipType == Relationship.SUPPORTS }

        val corroborationDisclosure = if (corroborationDetected) {
            "; a SUPPORTS relationship was also found linking the new evidence to the already-cited evidence, " +
                "but is not relied upon here, for the same reason DefaultKnowledgeCandidateEvaluator never relies " +
                "on one -- Article XI's independence requirement has no implemented common-origin determination"
        } else {
            ""
        }

        val misdirectedDisclosure = if (misdirectedTypes.isNotEmpty()) {
            "; a ${misdirectedTypes.sorted().joinToString()} relationship was also found linking the new evidence " +
                "to the already-cited evidence, but running in the wrong direction -- only a relationship running " +
                "from the new evidence to the already-cited evidence qualifies, never the reverse"
        } else {
            ""
        }

        if (qualifyingRelationshipTypes.isEmpty() && !statusTransitioned) {
            return@runBlocking KnowledgeRevisionEvaluation.NotQualifyingRevision(
                "no AMENDS or SUPERSEDES relationship runs from the newly submitted ${newResolved.recordKind} to " +
                    "this KnowledgeItem's already-cited ${existingResolved.recordKind}, no CONTRADICTS or DISPUTES " +
                    "relationship links the two in either direction, and the already-cited evidence's own current " +
                    "Memory Core status (${existingResolved.status}) is not one of the qualifying DISPUTED/" +
                    "SUPERSEDED transitions -- Unit 7 Scope Lock Clarification Section 7's Revision Qualification " +
                    "Gate is not satisfied, so this submission is not a revision of this KnowledgeItem; it may " +
                    "proceed separately as a new KnowledgeCandidate under Unit 6" +
                    misdirectedDisclosure + corroborationDisclosure,
            )
        }

        val contradicted = Relationship.CONTRADICTS in qualifyingRelationshipTypes ||
            Relationship.DISPUTES in qualifyingRelationshipTypes

        val resultingState = if (contradicted || item.evidentialState == EvidentialState.COMPETING_EXPLANATIONS) {
            EvidentialState.COMPETING_EXPLANATIONS
        } else {
            EvidentialState.UNKNOWN
        }

        val qualificationDisclosure = buildString {
            if (qualifyingRelationshipTypes.isNotEmpty()) {
                append(
                    "qualified by ${qualifyingRelationshipTypes.sorted().joinToString()} relationship(s) linking " +
                        "the newly submitted ${newResolved.recordKind} to the already-cited ${existingResolved.recordKind}",
                )
            }
            if (statusTransitioned) {
                if (isNotEmpty()) append("; ")
                append(
                    "the already-cited ${existingResolved.recordKind}'s own current Memory Core status is " +
                        "${existingResolved.status}, one of the two qualifying DISPUTED/SUPERSEDED transitions",
                )
            }
        }

        val basis = if (contradicted) {
            "the newly submitted evidence participates in an unresolved CONTRADICTS/DISPUTES relationship with " +
                "this KnowledgeItem's already-cited evidence; Contract Design Version 2 Section 3 requires this " +
                "be classified as an honest, unresolved COMPETING_EXPLANATIONS rather than resolved silently in " +
                "favour of one side ($qualificationDisclosure)$corroborationDisclosure"
        } else if (item.evidentialState == EvidentialState.COMPETING_EXPLANATIONS) {
            "this revision qualifies ($qualificationDisclosure) but does not itself carry a new CONTRADICTS/" +
                "DISPUTES relationship; the item's existing COMPETING_EXPLANATIONS classification from an earlier " +
                "event is preserved rather than silently resolved, since Contract Design Version 2 Section 3 " +
                "requires a disclosed contradiction never be resolved in favour of one side by an unrelated, " +
                "later revision$corroborationDisclosure"
        } else {
            "this revision qualifies ($qualificationDisclosure); no CONTRADICTS/DISPUTES relationship is present, " +
                "and no other currently authorised, reachable factor distinguishes a stronger classification at " +
                "revision time, so the evidential-state classification honestly reflects that the newly " +
                "considered evidence does not presently justify a stronger state, though the matter may become " +
                "knowable with further evidence (Article IV)$corroborationDisclosure"
        }

        val promotion = KnowledgePromotion(
            knowledgeId = item.knowledgeId,
            evidenceReference = newEvidenceReference,
            resultingState = resultingState,
            occurredAt = occurredAt,
            basis = basis,
        )

        val updatedItem = item.copy(
            evidenceReference = newEvidenceReference,
            provenanceReference = ProvenanceReference(newResolved.provenanceId),
            evidentialState = resultingState,
            history = item.history + promotion,
        )

        KnowledgeRevisionEvaluation.Applied(updatedItem, promotion)
    }

    private fun otherEndpoint(relationship: Relationship, known: RelationshipEndpoint): RelationshipEndpoint =
        if (relationship.fromEndpoint == known) relationship.toEndpoint else relationship.fromEndpoint

    private suspend fun resolve(reference: MemoryCoreRecordReference): ResolvedRecord? = when (reference) {
        is MemoryCoreRecordReference.ToEntity ->
            memoryRetrieval.getEntity(SYSTEM_PRINCIPAL_ID, reference.entityId)?.let {
                ResolvedRecord(RelationshipEndpoint.ENTITY, reference.entityId.value, it.provenanceId, it.status)
            }

        is MemoryCoreRecordReference.ToDocument ->
            memoryRetrieval.getDocument(SYSTEM_PRINCIPAL_ID, reference.documentId)?.let {
                ResolvedRecord(RelationshipEndpoint.DOCUMENT, reference.documentId.value, it.provenanceId, it.status)
            }

        is MemoryCoreRecordReference.ToAssertion ->
            memoryRetrieval.getAssertion(SYSTEM_PRINCIPAL_ID, reference.assertionId)?.let {
                ResolvedRecord(RelationshipEndpoint.ASSERTION, reference.assertionId.value, it.provenanceId, it.status)
            }

        is MemoryCoreRecordReference.ToRelationship ->
            memoryRetrieval.getRelationship(SYSTEM_PRINCIPAL_ID, reference.relationshipId)?.let {
                ResolvedRecord(RelationshipEndpoint.RELATIONSHIP, reference.relationshipId.value, it.provenanceId, it.status)
            }
    }

    /**
     * Purely internal to this class -- never exposed, never a public
     * contract. Carries exactly what [resolve] learns about whichever
     * Memory Core record kind [MemoryCoreRecordReference] named:
     * [recordKind]/[recordId] (for building the [RelationshipEndpoint]
     * this class queries relationships against), [provenanceId] (for the
     * updated [KnowledgeItem.provenanceReference]), and [status] (for the
     * Revision Qualification Gate's status-transition check).
     */
    private data class ResolvedRecord(
        val recordKind: String,
        val recordId: String,
        val provenanceId: ProvenanceId,
        val status: MemoryCoreRecordStatus,
    )

    companion object {
        /**
         * This evaluator's own fixed system identity, used only for the
         * mandatory, audit-only `requestingPrincipalId` parameter every
         * [MemoryRetrieval] method requires -- never evaluated or filtered
         * on by Memory Core itself. Mirrors
         * [DefaultKnowledgeCandidateEvaluator]'s own, identically justified
         * constant; declared separately here rather than shared, since
         * this Unit does not authorise refactoring that class.
         */
        val SYSTEM_PRINCIPAL_ID = PrincipalId("system.knowledge-memory")

        /**
         * A pagination bound for the one relationship-traversal query this
         * class issues per evaluation -- a structural query parameter, not
         * an evidential weighting figure; mirrors
         * [DefaultKnowledgeCandidateEvaluator]'s own identical bound.
         */
        const val MAX_RELATIONSHIP_RESULTS = 200

        /**
         * The two relationship types that qualify a revision only in a
         * specific direction -- `fromEndpoint` naming the newly submitted
         * evidence, `toEndpoint` naming the already-cited evidence,
         * exactly as `MemoryCore.kt`'s own amendment-process KDoc
         * describes the replacement always occupying `fromEndpoint`. A
         * relationship of either type stored in the opposite orientation
         * is detected and disclosed as misdirected, but does not qualify.
         */
        val DIRECTIONAL_QUALIFYING_RELATIONSHIP_TYPES = setOf(
            Relationship.AMENDS,
            Relationship.SUPERSEDES,
        )

        /**
         * The two relationship types that qualify a revision regardless
         * of direction -- both are inherently mutual facts about the two
         * records they connect, mirroring
         * [DefaultKnowledgeCandidateEvaluator]'s own identical,
         * already-frozen direction-agnostic treatment of these same two
         * types.
         */
        val SYMMETRIC_QUALIFYING_RELATIONSHIP_TYPES = setOf(
            Relationship.CONTRADICTS,
            Relationship.DISPUTES,
        )

        /**
         * The closed, two-value set of already-cited-evidence current
         * statuses that qualify a revision through Section 7's
         * status-transition case. `ACTIVE` is the universal starting
         * status, never itself a transition. `ARCHIVED` is deliberately
         * excluded -- Memory Core Scope Lock discloses archival as
         * reversible, retention-oriented housekeeping carrying no
         * implication about the evidence's own reliability. `DELETED` is
         * deliberately excluded -- it is reserved to the owner-requested
         * erasure path (Contract Design Version 2 Section 3), and this
         * class does not infer any erasure-related meaning from it for a
         * revision decision.
         */
        val QUALIFYING_STATUS_TRANSITIONS = setOf(
            MemoryCoreRecordStatus.DISPUTED,
            MemoryCoreRecordStatus.SUPERSEDED,
        )
    }
}
