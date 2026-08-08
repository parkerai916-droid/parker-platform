package parker.core.runtime

import java.time.Instant
import java.util.UUID
import parker.core.interfaces.CandidateAssertion
import parker.core.interfaces.CandidateProvenance
import parker.core.interfaces.ContentNature
import parker.core.interfaces.ExecutionRequest
import parker.core.interfaces.KnowledgeCandidate
import parker.core.interfaces.KnowledgeItem
import parker.core.interfaces.KnowledgeSubmission
import parker.core.interfaces.KnowledgeSubmissionDisposition
import parker.core.interfaces.MemoryCore
import parker.core.interfaces.MemoryCoreRecordReference
import parker.core.interfaces.PermissionDecisionOutcome
import parker.core.interfaces.PermissionEngine
import parker.core.interfaces.PrincipalId
import parker.core.interfaces.RequestId
import parker.core.interfaces.RequestOrigin
import parker.core.interfaces.RequestPriority
import parker.core.interfaces.ResourceId

/**
 * Parker Conversational Memory Bridge, Admission Unit. Sequences the full
 * "explicit owner remember instruction" admission path
 * `docs/implementation/CONVERSATIONAL_MEMORY_ADMISSION_IMPLEMENTATION_PLAN.md`
 * (the "Plan") Section 3.5 fixes: a self-gated Memory Core write
 * ([MemoryCore.createProvenance], [MemoryCore.createAssertion]) followed
 * by submission through the existing, unmodified [KnowledgeSubmission]
 * boundary, relying on the one, narrow single-factor promotion exception
 * `docs/governance/PROGRAMME_3_EXPLICIT_OWNER_INSTRUCTION_PROMOTION_EXCEPTION_SCOPE_LOCK_CLARIFICATION.md`
 * (the "Clarification") authorises.
 *
 * **No double gating.** [knowledgeSubmission] already self-gates its own
 * `submit` call (its own existing `KNOWLEDGE_SUBMISSION_RESOURCE_ID`/
 * `SUBMIT_ACTION_NAME` check, unmodified by this class). [permissionEngine]
 * here gates only the Memory Core write this class performs itself (the
 * Provenance/Assertion creation) -- a distinct proposal from Knowledge
 * Submission's own, never a second evaluation of the same one.
 *
 * **Confidence is never fabricated.** The [CandidateAssertion] this class
 * constructs always carries `confidence = null` -- honest absence, per
 * Article XIV and Clarification Guarantee 6, never invented to help the
 * candidate satisfy any factor.
 *
 * **No exception is caught here.** A genuine fault from [memoryCore] or
 * [knowledgeSubmission] propagates unchanged to this class's own caller,
 * mirroring [EvidenceRegistrationCoordinator]'s identical, established
 * discipline -- this class contains no `try`/`catch`.
 *
 * @param memoryCore The existing, already-composed durable `MemoryCore`
 *   instance -- the identical reference [EvidenceRegistrationCoordinator]
 *   and [EvidenceIntelligenceAcceptanceCoordinator] already hold, never a
 *   second or parallel instance.
 * @param knowledgeSubmission The existing, already-composed
 *   `KnowledgeSubmission` instance -- the identical reference
 *   [EvidenceIntelligenceAcceptanceCoordinator] already holds.
 * @param permissionEngine The one, shared Permission Engine every other
 *   coordinator in this composition graph already uses.
 */
class MemoryAdmissionCoordinator(
    private val memoryCore: MemoryCore,
    private val knowledgeSubmission: KnowledgeSubmission,
    private val permissionEngine: PermissionEngine,
) {

    /**
     * Runs the full sequence documented on this class's own KDoc:
     * (gated) [MemoryCore.createProvenance] -> [MemoryCore.createAssertion]
     * -> [KnowledgeSubmission.submit] -> [MemoryAdmissionOutcome]. Returns
     * as soon as the permission gate denies -- no Memory Core write is
     * ever attempted on that branch.
     *
     * @param requestingPrincipalId The owner Principal whose conversational
     *   instruction this is -- passed unchanged to every downstream call.
     * @param correlationId Reused, unchanged, as both this class's own
     *   [ExecutionRequest.correlationId] and the resulting
     *   [CandidateProvenance.sourceIdentifier] -- so the durable Provenance
     *   record remains traceable back to the conversation turn that
     *   produced it.
     * @param instructionText The proposition the owner asked to be
     *   remembered, exactly as [parker.core.interfaces.ReasoningProviderResponse.Remember]
     *   carried it -- never reworded, summarised, or embellished by this
     *   class.
     */
    suspend fun admit(
        requestingPrincipalId: PrincipalId,
        correlationId: String,
        instructionText: String,
    ): MemoryAdmissionOutcome {
        val decision = permissionEngine.evaluate(
            ExecutionRequest(
                requestId = RequestId("conversational-memory-admission-${UUID.randomUUID()}"),
                principalId = requestingPrincipalId,
                origin = RequestOrigin.REMOTE_INTERFACE,
                intent = "Create Memory Core record for an explicit conversational remember instruction",
                targetResources = listOf(CONVERSATIONAL_MEMORY_RESOURCE_ID),
                proposedActions = listOf(CREATE_CONVERSATIONAL_MEMORY_ACTION_NAME),
                priority = RequestPriority.NORMAL,
                createdAt = Instant.now(),
                correlationId = correlationId,
            ),
        )
        if (decision.decision != PermissionDecisionOutcome.APPROVED &&
            decision.decision != PermissionDecisionOutcome.APPROVED_WITH_CONFIRMATION
        ) {
            return MemoryAdmissionOutcome.NotAuthorised(
                "Permission Engine did not authorise creating a conversational memory record for principal " +
                    "'${requestingPrincipalId.value}' (decision=${decision.decision})",
            )
        }

        val provenance = memoryCore.createProvenance(
            requestingPrincipalId,
            CandidateProvenance(
                sourceIdentifier = correlationId,
                sourceType = "conversation",
                acquisitionTime = Instant.now(),
                contentNature = ContentNature.ORIGINAL,
            ),
        )
        val assertion = memoryCore.createAssertion(
            requestingPrincipalId,
            CandidateAssertion(statement = instructionText, provenanceId = provenance.provenanceId, confidence = null),
        )

        val candidate = KnowledgeCandidate(
            evidenceReference = MemoryCoreRecordReference.ToAssertion(assertion.assertionId),
            soleBasisIsExplicitInstruction = true,
        )

        return when (val disposition = knowledgeSubmission.submit(requestingPrincipalId, candidate)) {
            is KnowledgeSubmissionDisposition.Promoted -> MemoryAdmissionOutcome.Stored(disposition.item)
            is KnowledgeSubmissionDisposition.Declined -> MemoryAdmissionOutcome.NotStored(disposition.basis)
            is KnowledgeSubmissionDisposition.NotAuthorised -> MemoryAdmissionOutcome.NotAuthorised(disposition.reason)
        }
    }

    companion object {
        /** A fixed, well-known [ResourceId] this coordinator's own admission gate always names as its target. */
        val CONVERSATIONAL_MEMORY_RESOURCE_ID: ResourceId = ResourceId("resource-conversational-memory")

        /** This coordinator's own admission gate's proposed-action name. */
        const val CREATE_CONVERSATIONAL_MEMORY_ACTION_NAME: String = "create conversational memory record"
    }
}

/**
 * The result of [MemoryAdmissionCoordinator.admit]. Sealed `class`, not
 * `interface`, matching [EvidenceRegistrationOutcome]'s own established
 * convention. Every variant reuses an existing type unchanged rather than
 * re-deriving new fields.
 */
sealed class MemoryAdmissionOutcome {

    /** [KnowledgeSubmission.submit] returned [KnowledgeSubmissionDisposition.Promoted]. Carries the resulting [KnowledgeItem] unchanged. */
    data class Stored(val item: KnowledgeItem) : MemoryAdmissionOutcome()

    /** [KnowledgeSubmission.submit] returned [KnowledgeSubmissionDisposition.Declined]. Carries the evaluator's own basis unchanged. */
    data class NotStored(val basis: String) : MemoryAdmissionOutcome()

    /** Either this class's own admission gate, or [KnowledgeSubmission]'s own separate gate, denied the request. Carries the denying party's own reason unchanged. */
    data class NotAuthorised(val reason: String) : MemoryAdmissionOutcome()
}
