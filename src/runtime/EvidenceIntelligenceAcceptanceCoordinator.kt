package parker.core.runtime

import java.time.Instant
import java.util.UUID
import parker.core.interfaces.Assertion
import parker.core.interfaces.CandidateMemoryCoreRecord
import parker.core.interfaces.EvidenceAcceptanceResult
import parker.core.interfaces.EvidenceAnalysisResult
import parker.core.interfaces.EvidenceCustodian
import parker.core.interfaces.ExecutionRequest
import parker.core.interfaces.KnowledgeSubmission
import parker.core.interfaces.KnowledgeSubmissionDisposition
import parker.core.interfaces.MemoryCore
import parker.core.interfaces.MemoryCoreRecordReference
import parker.core.interfaces.PermissionDecisionOutcome
import parker.core.interfaces.PermissionEngine
import parker.core.interfaces.PrincipalId
import parker.core.interfaces.Relationship
import parker.core.interfaces.RequestId
import parker.core.interfaces.RequestOrigin
import parker.core.interfaces.RequestPriority
import parker.core.interfaces.ResourceId

/**
 * Evidence Intelligence, Implementation Unit 7 ("The Evidence Intelligence
 * Acceptance Coordinator"). Governed in full by
 * `docs/architecture/EVIDENCE_INTELLIGENCE_SCOPE_LOCK.md` ("the Scope
 * Lock") §6 step 4; `docs/implementation/EVIDENCE_INTELLIGENCE_IMPLEMENTATION_PLAN.md`
 * ("the Implementation Plan") §5, §6 item 5, §8 Unit 7; and this Unit's
 * own accepted `docs/reviews/EVIDENCE_INTELLIGENCE_UNIT_7_PLANNING_AND_BOUNDARY_REVIEW.md`
 * and its own follow-up review confirming the Programme 3 Unit 8
 * dependency (Finding 1) is now satisfied.
 *
 * A **sequencing mechanism only** (Scope Lock §6 step 4): it consumes the
 * `List<EvidenceAnalysisResult>` one [parker.core.interfaces.EvidenceIntelligence]
 * invocation returns, and dispatches each candidate to the one existing
 * acceptance interface its own kind already names --
 * [EvidenceCustodian.accept], `MemoryCore`'s write interface, or
 * [KnowledgeSubmission.submit] -- never reconstructing, filtering,
 * sorting, combining, or reinterpreting that list, and never retaining
 * anything about a candidate once its own outcome has been produced.
 *
 * **Concrete, non-interface-backed** -- exactly as the Implementation
 * Plan (§8 Unit 7) freezes: "An interface here would itself be the new
 * public type the Scope Lock's own 'no fifth new public type or
 * interface' freeze already excludes." This class implements no
 * interface of its own, and is never referenced from
 * [parker.core.interfaces.EvidenceAnalysisRequest],
 * [parker.core.interfaces.EvidenceAnalysisResult], or
 * [parker.core.interfaces.EvidenceIntelligence] -- the same footing Unit
 * 6's own [EvidenceIntelligenceInvocationGate] already occupies.
 *
 * ## Exactly four dependencies, mirroring [EvidenceRegistrationCoordinator]'s
 * own precedent
 *
 * [evidenceCustodian], [memoryCore], [knowledgeSubmission], and
 * [permissionEngine] are this class's only four dependencies -- the
 * absence of any other constructor parameter is itself the structural
 * guarantee that this class cannot reach [parker.core.interfaces.EvidenceIntelligence],
 * a [parker.core.interfaces.ReasoningProvider],
 * [parker.core.interfaces.KnowledgeCandidateEvaluator],
 * [KnowledgeItemPersistence], [parker.core.interfaces.MemoryRetrieval],
 * the legacy [parker.core.interfaces.KnowledgeStore] path, or any
 * evidence-deletion/storage internal.
 *
 * ## Two of three acceptance legs already self-gate; only the Memory Core
 * leg needs this class's own [PermissionEngine]
 *
 * [EvidenceCustodian.accept] and [KnowledgeSubmission.submit] each already
 * evaluate their own permission decision internally
 * (`DefaultEvidenceCustodian`, `DefaultKnowledgeSubmission`) -- this class
 * never re-evaluates either, never duplicating a decision those
 * interfaces' own contracts already make. `MemoryCore`'s write interface
 * constitutionally never self-gates (Memory Core Scope Lock §6: "Memory
 * Core never evaluates permissions... No contract implementing
 * `MemoryCore`... may hold a `PermissionEngine` reference"), so nothing
 * gates a `CandidateRecordProduced` dispatch unless this class does --
 * the one documented, load-bearing reason this class holds
 * [permissionEngine] at all, mirroring
 * [EvidenceRegistrationCoordinator]'s own identical, already-accepted
 * precedent for its own `createProvenance`/`registerDocument` calls.
 *
 * ## Why a new, minimal internal outcome type is necessary -- and why it
 * is the smallest one that could work
 *
 * The Implementation Plan (§8 Unit 7) requires the observable outcome for
 * each candidate to be "exactly whichever of three already-existing
 * possibilities occurred, each expressed through that accepting
 * subsystem's own, already-existing, unmodified contract -- never a new
 * shape." For three of the four `EvidenceAnalysisResult` branches, an
 * existing public type already suffices, and is reused entirely
 * unchanged: [EvidenceAcceptanceResult] for `CandidateArtifactProduced`;
 * [KnowledgeSubmissionDisposition] for `CandidateKnowledgeProduced`; and
 * `TransientOutput` needs no disposition at all, since it is never
 * dispatched anywhere (Contract Design §5: "never submitted anywhere...
 * discarded or regenerated freely").
 *
 * The fourth branch, `CandidateRecordProduced`, cannot be represented this
 * way without semantic loss. `MemoryCore`'s write interface
 * (`createAssertion`/`createRelationship`) either returns the created
 * record or throws -- it has **no vocabulary at all** for "not
 * authorised," because it is constitutionally forbidden from ever holding
 * a permission opinion of its own (Memory Core Scope Lock §6, quoted
 * above). A permission denial on this leg can only ever originate from
 * *this class's own* [permissionEngine] decision -- a fact no existing
 * public `MemoryCore` type can express, since `MemoryCore` itself never
 * knows a denial occurred; the call is simply never made. Collapsing that
 * denial into, for example, a fabricated [EvidenceAcceptanceResult] or a
 * [KnowledgeSubmissionDisposition] would misattribute the decision to a
 * subsystem that never rendered it -- exactly the "semantic collapse"
 * this Unit's own governing task forbids. [EvidenceRegistrationCoordinator]
 * already faced, and already solved, this identical problem for its own
 * two `MemoryCore` calls, by defining its own outcome type
 * (`EvidenceRegistrationOutcome`) rather than forcing an existing one to
 * carry a meaning it does not have. [EvidenceIntelligenceAcceptanceOutcome]
 * and [CandidateMemoryCoreRecordResult] below are that same, minimal
 * response, scoped as narrowly as possible: one sealed hierarchy adding
 * exactly the one missing case (a coordinator-rendered Memory Core
 * denial) and one selector distinguishing the two record kinds
 * `MemoryCore`'s write interface can produce -- nothing else.
 *
 * Both types are `internal`, never `public` -- the Scope Lock's own "no
 * fifth new public type" ceiling governs Evidence Intelligence's own
 * four-type public model (§4, as amended); it does not, and was never
 * intended to, reach a composition-level outcome representation that no
 * public type in this file, and no type reachable from
 * [parker.core.interfaces.EvidenceAnalysisRequest],
 * [parker.core.interfaces.EvidenceAnalysisResult], or
 * [parker.core.interfaces.EvidenceIntelligence], ever references -- the
 * same reasoning that already places [EvidenceIntelligenceInvocationGate]
 * and this class itself outside that ceiling.
 *
 * ## Faults are never swallowed
 *
 * No `try`/`catch` exists anywhere in this class, mirroring
 * [EvidenceRegistrationCoordinator]'s own identical, disclosed discipline.
 * A genuine exception thrown by [evidenceCustodian], [permissionEngine],
 * [memoryCore], or [knowledgeSubmission] propagates unchanged out of
 * [dispatch], terminating that call -- it is never caught, never
 * reinterpreted as a rejection or a permission denial, and never
 * converted into a disposition this class's own sealed outcome type
 * could represent instead.
 *
 * ## Statelessness
 *
 * This class holds no `var`, no mutable collection, and no cache of any
 * candidate, outcome, or identifier across candidates or across
 * invocations -- only its four constructor-injected dependencies as
 * fields, mirroring [CommunicationConversationCoordinator]'s own
 * "Statelessness invariant" precedent exactly. [dispatch] performs no
 * retry, no batching across invocations, no scheduling, and no
 * cross-invocation aggregation.
 *
 * @param evidenceCustodian Invoked, at most once, for every
 *   `CandidateArtifactProduced` element -- already self-gating.
 * @param memoryCore Invoked, at most once, for every `CandidateRecordProduced`
 *   element whose own dispatch this class's own [permissionEngine] check
 *   approves. Never invoked on denial.
 * @param knowledgeSubmission Invoked, at most once, for every
 *   `CandidateKnowledgeProduced` element -- already self-gating (Evaluation
 *   B, `docs/governance/PROGRAMME_3_KNOWLEDGE_MEMORY_CONTRACT_DESIGN_V2.md`
 *   §7).
 * @param permissionEngine Evaluated, at most once per `CandidateRecordProduced`
 *   element, and never for any other element -- the one leg this class
 *   itself must gate.
 */
internal class EvidenceIntelligenceAcceptanceCoordinator(
    private val evidenceCustodian: EvidenceCustodian,
    private val memoryCore: MemoryCore,
    private val knowledgeSubmission: KnowledgeSubmission,
    private val permissionEngine: PermissionEngine,
) {

    /**
     * Dispatches every element of [results], in order, to its own
     * accepting subsystem, returning one [EvidenceIntelligenceAcceptanceOutcome]
     * per element, in the same order. [results] is read only -- never
     * copied into a new order, filtered, merged, or otherwise
     * reinterpreted; [requestingPrincipalId] is passed unchanged to every
     * downstream call this dispatch makes.
     *
     * Processing a `Declined`/`NotAuthorised`/`NotAuthorised`-shaped
     * outcome for one element is an ordinary, successful return value --
     * it never stops this function from dispatching the remaining
     * elements. A genuine exception thrown by any dependency is not
     * caught here; it propagates out of this suspend function exactly as
     * thrown, naturally ending this call without dispatching whatever
     * elements remained (see this class's own header KDoc, "Faults are
     * never swallowed").
     */
    suspend fun dispatch(
        requestingPrincipalId: PrincipalId,
        results: List<EvidenceAnalysisResult>,
    ): List<EvidenceIntelligenceAcceptanceOutcome> = results.map { result ->
        when (result) {
            is EvidenceAnalysisResult.TransientOutput ->
                EvidenceIntelligenceAcceptanceOutcome.NotDispatched

            is EvidenceAnalysisResult.CandidateArtifactProduced ->
                EvidenceIntelligenceAcceptanceOutcome.ArtifactAcceptance(
                    evidenceCustodian.accept(requestingPrincipalId, result.candidateEvidenceArtifact),
                )

            is EvidenceAnalysisResult.CandidateRecordProduced ->
                dispatchRecord(requestingPrincipalId, result.candidateRecord)

            is EvidenceAnalysisResult.CandidateKnowledgeProduced ->
                dispatchKnowledge(requestingPrincipalId, result)
        }
    }

    /**
     * The one leg this class itself gates. Evaluates [permissionEngine]
     * exactly once, using the disclosed-but-unregistered
     * [MEMORY_CORE_ACCEPTANCE_RESOURCE_ID]/[ACCEPT_MEMORY_CORE_CANDIDATE_ACTION_NAME]
     * pair -- distinct from [EvidenceRegistrationCoordinator]'s own two
     * pairs, since this is a different governed act (accepting an
     * Evidence-Intelligence-produced candidate, never Evidence
     * Registration's own document/provenance intake). On any outcome
     * other than `APPROVED`/`APPROVED_WITH_CONFIRMATION`, returns
     * immediately -- [memoryCore] is never called. On approval, dispatches
     * to [MemoryCore.createAssertion] or [MemoryCore.createRelationship]
     * per [candidateRecord]'s own selected case, passing every field
     * unchanged.
     */
    private suspend fun dispatchRecord(
        requestingPrincipalId: PrincipalId,
        candidateRecord: CandidateMemoryCoreRecord,
    ): EvidenceIntelligenceAcceptanceOutcome.RecordAcceptance {
        val decision = permissionEngine.evaluate(buildMemoryCoreAcceptanceRequest(requestingPrincipalId))

        if (decision.decision != PermissionDecisionOutcome.APPROVED &&
            decision.decision != PermissionDecisionOutcome.APPROVED_WITH_CONFIRMATION
        ) {
            return EvidenceIntelligenceAcceptanceOutcome.RecordAcceptance.NotAuthorised(
                "Permission Engine did not authorise Memory Core acceptance of an Evidence Intelligence " +
                    "candidate for principal '${requestingPrincipalId.value}' (decision=${decision.decision})",
            )
        }

        val written = when (candidateRecord) {
            is CandidateMemoryCoreRecord.OfAssertion ->
                CandidateMemoryCoreRecordResult.OfAssertion(
                    memoryCore.createAssertion(requestingPrincipalId, candidateRecord.candidateAssertion),
                )

            is CandidateMemoryCoreRecord.OfRelationship ->
                CandidateMemoryCoreRecordResult.OfRelationship(
                    memoryCore.createRelationship(requestingPrincipalId, candidateRecord.candidateRelationship),
                )
        }

        return EvidenceIntelligenceAcceptanceOutcome.RecordAcceptance.Written(written)
    }

    /**
     * Performs the Implementation Plan's own required defensive check
     * (§8 Unit 7: "before submitting any Knowledge Candidate... it
     * confirms the Memory Core record that candidate references already
     * carries a governed identifier") using only [result]'s own
     * already-carried [parker.core.interfaces.KnowledgeCandidate.evidenceReference]
     * -- never a [parker.core.interfaces.MemoryRetrieval] read, which this
     * class holds no dependency capable of performing. Then invokes
     * [KnowledgeSubmission.submit] exactly once, unchanged, and surfaces
     * its own [KnowledgeSubmissionDisposition] unchanged -- this class
     * never calls [parker.core.interfaces.KnowledgeCandidateEvaluator]
     * directly and never evaluates a permission decision of its own for
     * this leg, since [knowledgeSubmission] already performs Evaluation B
     * internally.
     */
    private suspend fun dispatchKnowledge(
        requestingPrincipalId: PrincipalId,
        result: EvidenceAnalysisResult.CandidateKnowledgeProduced,
    ): EvidenceIntelligenceAcceptanceOutcome.KnowledgeAcceptance {
        requireGovernedIdentifier(result.knowledgeCandidate.evidenceReference)

        return EvidenceIntelligenceAcceptanceOutcome.KnowledgeAcceptance(
            knowledgeSubmission.submit(requestingPrincipalId, result.knowledgeCandidate),
        )
    }

    /**
     * The defensive check itself: every [MemoryCoreRecordReference] case
     * wraps an identifier value class ([parker.core.interfaces.EntityId],
     * [parker.core.interfaces.DocumentId],
     * [parker.core.interfaces.AssertionId],
     * [parker.core.interfaces.RelationshipId]) that already rejects a
     * blank value at its own construction -- so this check "should, per
     * the Contract Design's own structural guarantee, never fail"
     * (Implementation Plan §8 Unit 7). It is performed anyway, defensively,
     * entirely against [reference]'s own already-carried data, reading
     * nothing from Memory Core. Failure here is treated exactly as the
     * Implementation Plan requires -- "a genuine implementation fault,"
     * a thrown exception, never a silent proceed and never one of this
     * class's own ordinary dispositions.
     */
    private fun requireGovernedIdentifier(reference: MemoryCoreRecordReference) {
        val identifierValue = when (reference) {
            is MemoryCoreRecordReference.ToEntity -> reference.entityId.value
            is MemoryCoreRecordReference.ToDocument -> reference.documentId.value
            is MemoryCoreRecordReference.ToAssertion -> reference.assertionId.value
            is MemoryCoreRecordReference.ToRelationship -> reference.relationshipId.value
        }
        check(identifierValue.isNotBlank()) {
            "Knowledge Candidate references a Memory Core record with no governed identifier -- this " +
                "should be structurally impossible (every MemoryCoreRecordReference wraps an identifier " +
                "value class that already rejects a blank value at construction); its occurrence here " +
                "indicates a caller other than Evidence Intelligence's own operation supplied this " +
                "result -- a genuine implementation fault"
        }
    }

    /**
     * Mirrors [DefaultEvidenceCustodian.buildExecutionRequest]'s own
     * shared-helper shape. Mints a fresh `requestId`/`correlationId` per
     * call -- `EvidenceAnalysisResult` carries no correlation identifier
     * of its own for this function to reuse instead.
     */
    private fun buildMemoryCoreAcceptanceRequest(requestingPrincipalId: PrincipalId): ExecutionRequest {
        val id = UUID.randomUUID()
        return ExecutionRequest(
            requestId = RequestId("evidence-intelligence-memory-core-accept-$id"),
            principalId = requestingPrincipalId,
            origin = RequestOrigin.REMOTE_INTERFACE,
            intent = "Accept Evidence Intelligence-produced Memory Core candidate record",
            targetResources = listOf(MEMORY_CORE_ACCEPTANCE_RESOURCE_ID),
            proposedActions = listOf(ACCEPT_MEMORY_CORE_CANDIDATE_ACTION_NAME),
            priority = RequestPriority.NORMAL,
            createdAt = Instant.now(),
            correlationId = "evidence-intelligence-memory-core-accept-$id",
        )
    }

    companion object {
        /**
         * A fixed, well-known [ResourceId] this class's own Memory Core
         * acceptance gate always names as its target -- the coordinator's
         * governed act of accepting an Evidence-Intelligence-produced
         * candidate record, never generic access to all `MemoryCore`
         * operations, and distinct from [EvidenceRegistrationCoordinator]'s
         * own `MEMORY_CORE_PROVENANCE_RESOURCE_ID`/
         * `MEMORY_CORE_DOCUMENT_REGISTRATION_RESOURCE_ID` -- a different
         * governed act entirely. Not registered anywhere by this Unit.
         */
        val MEMORY_CORE_ACCEPTANCE_RESOURCE_ID: ResourceId =
            ResourceId("evidence-intelligence-memory-core-acceptance")

        /**
         * A fixed proposed-action name a future `ActionVocabulary`
         * registration is expected to map to a resolvable
         * `(PermissionAction, ResourceType)` pair. Not registered anywhere
         * by this Unit.
         */
        const val ACCEPT_MEMORY_CORE_CANDIDATE_ACTION_NAME: String = "evidence-intelligence.accept-memory-core-candidate"
    }
}

/**
 * The minimal payload selector for [EvidenceIntelligenceAcceptanceOutcome.RecordAcceptance.Written],
 * distinguishing which of `MemoryCore`'s two write operations produced
 * the durable record -- mirroring [CandidateMemoryCoreRecord]'s own,
 * input-side selector shape exactly, on the output side. `internal`,
 * never public; owned exclusively by this file.
 */
internal sealed class CandidateMemoryCoreRecordResult {
    data class OfAssertion(val assertion: Assertion) : CandidateMemoryCoreRecordResult()
    data class OfRelationship(val relationship: Relationship) : CandidateMemoryCoreRecordResult()
}

/**
 * The internal, non-public outcome representation
 * [EvidenceIntelligenceAcceptanceCoordinator.dispatch] returns one of, per
 * candidate -- see that class's own header KDoc, "Why a new, minimal
 * internal outcome type is necessary," for the full justification. Four
 * cases, one per [EvidenceAnalysisResult] branch, each reusing an
 * existing public type unchanged wherever one already exists:
 *
 * - [NotDispatched] -- `TransientOutput` was never submitted anywhere;
 *   no dependency was invoked.
 * - [ArtifactAcceptance] -- [EvidenceAcceptanceResult], Evidence
 *   Custodian's own existing sealed type, surfaced unchanged.
 * - [RecordAcceptance] -- the one genuinely new case, and the only reason
 *   this type exists at all: [RecordAcceptance.Written] carries the
 *   created record (via [CandidateMemoryCoreRecordResult]);
 *   [RecordAcceptance.NotAuthorised] carries this coordinator's own
 *   Permission Engine denial, a fact `MemoryCore`'s own contract has no
 *   vocabulary to express.
 * - [KnowledgeAcceptance] -- [KnowledgeSubmissionDisposition], Knowledge
 *   Memory's own existing sealed type, surfaced unchanged, keeping
 *   `Promoted`, `Declined`, and `NotAuthorised` fully distinguishable.
 */
internal sealed class EvidenceIntelligenceAcceptanceOutcome {

    object NotDispatched : EvidenceIntelligenceAcceptanceOutcome()

    data class ArtifactAcceptance(val result: EvidenceAcceptanceResult) : EvidenceIntelligenceAcceptanceOutcome()

    sealed class RecordAcceptance : EvidenceIntelligenceAcceptanceOutcome() {
        data class Written(val record: CandidateMemoryCoreRecordResult) : RecordAcceptance()
        data class NotAuthorised(val reason: String) : RecordAcceptance()
    }

    data class KnowledgeAcceptance(val disposition: KnowledgeSubmissionDisposition) : EvidenceIntelligenceAcceptanceOutcome()
}
