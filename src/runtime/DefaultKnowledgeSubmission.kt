package parker.core.runtime

import java.time.Instant
import java.util.UUID
import parker.core.interfaces.ExecutionRequest
import parker.core.interfaces.KnowledgeCandidate
import parker.core.interfaces.KnowledgeCandidateEvaluation
import parker.core.interfaces.KnowledgeCandidateEvaluator
import parker.core.interfaces.KnowledgeSubmission
import parker.core.interfaces.KnowledgeSubmissionDisposition
import parker.core.interfaces.PermissionDecisionOutcome
import parker.core.interfaces.PermissionEngine
import parker.core.interfaces.PrincipalId
import parker.core.interfaces.RequestId
import parker.core.interfaces.RequestOrigin
import parker.core.interfaces.RequestPriority
import parker.core.interfaces.ResourceId

/**
 * Programme 3, Knowledge Memory, Implementation Unit 8 (Knowledge
 * Submission). The sole implementation of
 * [parker.core.interfaces.KnowledgeSubmission] -- see
 * `docs/governance/PROGRAMME_3_UNIT_8_SCOPE_LOCK_CLARIFICATION.md`
 * ("the Unit 8 Clarification"), in full, for the constitutional reasoning
 * this class implements exactly and nothing more.
 *
 * ## Sequence (Unit 8 Clarification §8)
 *
 * 1. Receive the submission ([requestingPrincipalId], `candidate`).
 * 2. Perform structural admissibility checks requiring no read of
 *    protected content. There is, deliberately, no code for this step:
 *    [KnowledgeCandidate]'s own construction already guarantees every
 *    invariant Unit 5 authorises -- it "has no `init` block... there is no
 *    field beyond ordinary type safety left to validate" (its own KDoc) --
 *    so by the time [submit] runs there is nothing left to check that
 *    would not itself require reading the Memory Core record the
 *    candidate's own `evidenceReference` names, a read this class is never
 *    authorised to perform (see "No dependency capable of reading Memory
 *    Core," below).
 * 3. Evaluate Evaluation B via [permissionEngine].
 * 4. On any outcome other than `APPROVED`/`APPROVED_WITH_CONFIRMATION`,
 *    return [KnowledgeSubmissionDisposition.NotAuthorised] immediately --
 *    [evaluator] is never invoked and [persistence] is never touched.
 * 5. Invoke [evaluator] exactly once.
 * 6. On [KnowledgeCandidateEvaluation.Reject], return
 *    [KnowledgeSubmissionDisposition.Declined] carrying the evaluator's own
 *    [KnowledgeCandidateEvaluation.Reject.basis] unchanged. Nothing is
 *    persisted.
 * 7. On [KnowledgeCandidateEvaluation.Promote], persist the proposed
 *    [parker.core.interfaces.KnowledgeItem] via [persistence], then return
 *    [KnowledgeSubmissionDisposition.Promoted] carrying exactly the
 *    evaluator's own `item`/`promotion` pair.
 *
 * ## No dependency capable of reading Memory Core
 *
 * This class holds exactly three dependencies -- [evaluator],
 * [persistence], and [permissionEngine] -- and, deliberately, no
 * [parker.core.interfaces.MemoryRetrieval] reference of its own. It is
 * therefore structurally incapable of reading any Memory Core content, at
 * any point in [submit], before or after Evaluation B -- resolving the
 * candidate's own `evidenceReference` remains exclusively [evaluator]'s
 * own, already-closed Unit 6 responsibility, performed only after
 * Evaluation B has already approved.
 *
 * ## Why this class holds [PermissionEngine] directly, unlike `MemoryCore`
 *
 * `MemoryCore`/`MemoryRetrieval` are constitutionally forbidden from
 * holding a [PermissionEngine] reference
 * (`docs/architecture/MEMORY_CORE_SCOPE_LOCK.md` §6, §14), because that
 * interface serves five independent write operations reachable by many
 * callers and cannot self-gate by design -- gating is instead performed
 * externally, by [EvidenceRegistrationCoordinator]. This class is the
 * opposite shape: one act (submit), one accepting responsibility,
 * mirroring [DefaultEvidenceCustodian]'s own identical "single,
 * self-contained, accept-shaped boundary holds its own `PermissionEngine`"
 * precedent exactly (Unit 8 Clarification §5).
 *
 * ## Disclosed-but-unregistered resource/action
 *
 * [KNOWLEDGE_SUBMISSION_RESOURCE_ID]/[SUBMIT_ACTION_NAME] are a fixed,
 * single, not-candidate-specific pair, following the same disclosed
 * convention this codebase already uses for every other gated action
 * ([DefaultEvidenceCustodian]'s own five; [EvidenceRegistrationCoordinator]'s
 * own two) -- neither is registered anywhere by this Unit; a real
 * `DefaultPermissionPolicy` denies every submission through this path
 * until a future Runtime-integration unit registers a Resource under this
 * identifier (Unit 8 Clarification §7).
 *
 * @param evaluator Invoked exactly once per [submit] call that Evaluation B
 *   approves. Never invoked otherwise.
 * @param persistence Written to exactly once per [submit] call that
 *   [evaluator] promotes. Never written to otherwise.
 * @param permissionEngine Evaluated exactly once per [submit] call, always
 *   first, before [evaluator] or [persistence] is ever touched.
 */
internal class DefaultKnowledgeSubmission(
    private val evaluator: KnowledgeCandidateEvaluator,
    private val persistence: KnowledgeItemPersistence,
    private val permissionEngine: PermissionEngine,
) : KnowledgeSubmission {

    override suspend fun submit(
        requestingPrincipalId: PrincipalId,
        candidate: KnowledgeCandidate,
    ): KnowledgeSubmissionDisposition {
        val decision = permissionEngine.evaluate(buildExecutionRequest(requestingPrincipalId))

        if (decision.decision != PermissionDecisionOutcome.APPROVED &&
            decision.decision != PermissionDecisionOutcome.APPROVED_WITH_CONFIRMATION
        ) {
            return KnowledgeSubmissionDisposition.NotAuthorised(
                "Permission Engine did not authorise Knowledge Candidate submission for principal " +
                    "'${requestingPrincipalId.value}' (decision=${decision.decision})",
            )
        }

        return when (val evaluation = evaluator.evaluate(requestingPrincipalId, candidate)) {
            is KnowledgeCandidateEvaluation.Reject ->
                KnowledgeSubmissionDisposition.Declined(evaluation.basis)

            is KnowledgeCandidateEvaluation.Promote -> {
                persistence.store(evaluation.item)
                KnowledgeSubmissionDisposition.Promoted(evaluation.item, evaluation.promotion)
            }
        }
    }

    /**
     * Mirrors [DefaultEvidenceCustodian.buildExecutionRequest]'s own
     * identical shared-helper shape. [candidate] itself carries no
     * `correlationId` field (Unit 5's own frozen field list), so, exactly
     * as [DefaultEvidenceCustodian.accept]/[DefaultEvidenceCustodian.retrieve]
     * already do, a fresh identifier is minted here and reused as both the
     * [ExecutionRequest.requestId] and [ExecutionRequest.correlationId].
     */
    private fun buildExecutionRequest(requestingPrincipalId: PrincipalId): ExecutionRequest {
        val id = UUID.randomUUID()
        return ExecutionRequest(
            requestId = RequestId("knowledge-submission-$id"),
            principalId = requestingPrincipalId,
            origin = RequestOrigin.REMOTE_INTERFACE,
            intent = "Submit Knowledge Candidate for promotion evaluation",
            targetResources = listOf(KNOWLEDGE_SUBMISSION_RESOURCE_ID),
            proposedActions = listOf(SUBMIT_ACTION_NAME),
            priority = RequestPriority.NORMAL,
            createdAt = Instant.now(),
            correlationId = "knowledge-submission-$id",
        )
    }

    companion object {
        /**
         * A fixed, well-known [ResourceId] this class's own Evaluation B
         * gate always names as its target -- not a per-candidate identity
         * (Unit 8 Clarification §7: "a single, fixed... resource identity
         * representing the Knowledge Submission boundary itself"). Not
         * registered anywhere by this Unit.
         */
        val KNOWLEDGE_SUBMISSION_RESOURCE_ID: ResourceId = ResourceId("knowledge-memory-submission")

        /**
         * A fixed proposed-action name a future `ActionVocabulary`
         * registration is expected to map to a resolvable
         * `(PermissionAction, ResourceType)` pair. Not registered anywhere
         * by this Unit.
         */
        const val SUBMIT_ACTION_NAME: String = "knowledge.submit"
    }
}
