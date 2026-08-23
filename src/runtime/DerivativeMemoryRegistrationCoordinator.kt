package parker.core.runtime

import java.time.Instant
import java.util.UUID
import parker.core.interfaces.CandidateDocument
import parker.core.interfaces.CandidateProvenance
import parker.core.interfaces.ContentNature
import parker.core.interfaces.DerivativeContentIdentity
import parker.core.interfaces.DerivativeMemoryRegistrationOutcome
import parker.core.interfaces.DocumentId
import parker.core.interfaces.DocumentProcessingStatus
import parker.core.interfaces.ExecutionRequest
import parker.core.interfaces.MemoryCore
import parker.core.interfaces.PermissionDecisionOutcome
import parker.core.interfaces.PermissionEngine
import parker.core.interfaces.PrincipalId
import parker.core.interfaces.RequestId
import parker.core.interfaces.RequestOrigin
import parker.core.interfaces.RequestPriority
import parker.core.interfaces.ResourceId
import parker.core.interfaces.TierADocumentRoutingResult

/**
 * Document Ingestion, Derivative-to-Memory-Core Registration. Implements
 * exactly the act
 * `docs/architecture/DOCUMENT_INGESTION_DERIVATIVE_TO_MEMORY_CORE_REGISTRATION_SCOPE_LOCK.md`
 * ("the Scope Lock") Section 4/7 fixes: registering an already-admitted
 * Tier A derivative into Memory Core as a [parker.core.interfaces.Provenance]/
 * [parker.core.interfaces.Document] pair, mirroring
 * [EvidenceRegistrationCoordinator]'s own already-adopted two-gate
 * shape, for a different input (an already-admitted derivative, never a
 * fresh candidate to accept).
 *
 * **Two dependencies only: [MemoryCore] and [PermissionEngine].** Not
 * `EvidenceCustodian`, not `DerivativeGenerationStorage`, not a
 * reference to either existing Document Ingestion owner-invocation
 * coordinator (Scope Lock Section 4.C). `MemoryCore` cannot self-gate
 * (Memory Core Scope Lock Section 6), so nothing gates this act unless
 * this coordinator does -- the identical, disclosed reason
 * [EvidenceRegistrationCoordinator] already holds a [PermissionEngine]
 * reference.
 *
 * **Eligibility enforced by the type system, not a runtime check.**
 * [register] accepts [TierADocumentRoutingResult.Admitted] directly --
 * not the general [TierADocumentRoutingResult] sealed class -- so a
 * `ReconciliationRequired`, `RequiresTierB`, or any other variant cannot
 * even be passed to this method; no ineligible-state branch exists
 * because none is reachable (Scope Lock Section 6).
 *
 * **Never reads [TierADocumentRoutingResult.Admitted.payload].** Only
 * [admitted]'s own `record`/`format` fields are ever read -- the
 * extracted parser payload is never touched, never inspected, and never
 * copied into any Memory Core field (Scope Lock Section 8's content
 * boundary).
 *
 * ## Field mapping (Scope Lock Section 7)
 *
 * Every [CandidateProvenance]/[CandidateDocument] field is derived
 * either from [admitted]'s own `record`, or from a fixed, coordinator-
 * owned constant -- never from caller-supplied override, except
 * [extractedFromDocumentId], the one fact this coordinator cannot
 * itself derive (whether, and under what identity, the derivative's own
 * source was separately registered into Memory Core).
 *
 * ## Failure sequencing (Scope Lock Section 15)
 *
 * No `try`/`catch` anywhere in [register]. A denied permission decision
 * is an ordinary, non-exceptional result, mirroring
 * [EvidenceRegistrationOutcome.ProvenanceNotAuthorised]/[EvidenceRegistrationOutcome.DocumentRegistrationNotAuthorised]'s
 * own precedent exactly. A genuine exception thrown by
 * [MemoryCore.createProvenance]/[MemoryCore.registerDocument] propagates
 * unchanged. This coordinator never retries and never attempts to roll
 * back an already-created [parker.core.interfaces.Provenance] record --
 * no such operation exists or is authorized. A downstream registration
 * failure of any kind never retroactively invalidates, unpublishes, or
 * reinterprets [admitted]'s own already-admitted derivative -- this
 * coordinator never writes to, or otherwise touches, Document
 * Ingestion's own storage at all.
 */
class DerivativeMemoryRegistrationCoordinator(
    private val memoryCore: MemoryCore,
    private val permissionEngine: PermissionEngine,
) {
    suspend fun register(
        requestingPrincipalId: PrincipalId,
        correlationId: String,
        admitted: TierADocumentRoutingResult.Admitted,
        extractedFromDocumentId: DocumentId? = null,
    ): DerivativeMemoryRegistrationOutcome {
        val provenanceDecision = permissionEngine.evaluate(
            buildExecutionRequest(
                requestingPrincipalId = requestingPrincipalId,
                correlationId = correlationId,
                resourceId = DERIVATIVE_MEMORY_PROVENANCE_RESOURCE_ID,
                actionName = CREATE_PROVENANCE_ACTION_NAME,
                intent = "Create Memory Core provenance record for admitted derivative " +
                    "'${admitted.record.derivativeGenerationId.value}'",
                requestIdPrefix = "derivative-memory-registration-provenance",
            ),
        )
        if (provenanceDecision.decision != PermissionDecisionOutcome.APPROVED &&
            provenanceDecision.decision != PermissionDecisionOutcome.APPROVED_WITH_CONFIRMATION
        ) {
            return DerivativeMemoryRegistrationOutcome.ProvenanceNotAuthorised(
                "Permission Engine did not authorise Memory Core provenance creation for principal " +
                    "'${requestingPrincipalId.value}' (decision=${provenanceDecision.decision})",
            )
        }

        val integrityInformation = when (val contentIdentity = admitted.record.contentIdentity) {
            is DerivativeContentIdentity.Digest -> contentIdentity.digest
            is DerivativeContentIdentity.NoCanonicalSerialization -> null
        }

        val candidateProvenance = CandidateProvenance(
            sourceIdentifier = admitted.record.derivativeGenerationId.value,
            sourceType = DERIVATIVE_SOURCE_TYPE,
            acquisitionTime = Instant.now(),
            contentNature = ContentNature.EXTRACTED,
            extractedFrom = extractedFromDocumentId,
            processingHistory = listOf(
                "Tier A derivative generation ${admitted.record.derivativeGenerationId.value}",
            ),
            integrityInformation = integrityInformation,
        )
        val provenance = memoryCore.createProvenance(requestingPrincipalId, candidateProvenance)

        val documentDecision = permissionEngine.evaluate(
            buildExecutionRequest(
                requestingPrincipalId = requestingPrincipalId,
                correlationId = correlationId,
                resourceId = DERIVATIVE_MEMORY_DOCUMENT_REGISTRATION_RESOURCE_ID,
                actionName = REGISTER_DOCUMENT_ACTION_NAME,
                intent = "Register Memory Core document for admitted derivative " +
                    "'${admitted.record.derivativeGenerationId.value}'",
                requestIdPrefix = "derivative-memory-registration-document",
            ),
        )
        if (documentDecision.decision != PermissionDecisionOutcome.APPROVED &&
            documentDecision.decision != PermissionDecisionOutcome.APPROVED_WITH_CONFIRMATION
        ) {
            return DerivativeMemoryRegistrationOutcome.DocumentRegistrationNotAuthorised(
                provenance = provenance,
                reason = "Permission Engine did not authorise Memory Core document registration for principal " +
                    "'${requestingPrincipalId.value}' (decision=${documentDecision.decision})",
            )
        }

        val candidateDocument = CandidateDocument(
            documentType = admitted.record.derivativeKind,
            locationReference = admitted.record.derivativeGenerationId.value,
            provenanceId = provenance.provenanceId,
            integrityHash = integrityInformation,
            processingStatus = DocumentProcessingStatus.PROCESSED_EXTERNALLY,
        )
        val document = memoryCore.registerDocument(requestingPrincipalId, candidateDocument)

        return DerivativeMemoryRegistrationOutcome.Registered(provenance, document)
    }

    /**
     * Shared [ExecutionRequest] construction for both of this class's own
     * permission evaluations -- mirrors
     * [EvidenceRegistrationCoordinator.buildExecutionRequest]'s own
     * identical shape.
     */
    private fun buildExecutionRequest(
        requestingPrincipalId: PrincipalId,
        correlationId: String,
        resourceId: ResourceId,
        actionName: String,
        intent: String,
        requestIdPrefix: String,
    ): ExecutionRequest = ExecutionRequest(
        requestId = RequestId("$requestIdPrefix-${UUID.randomUUID()}"),
        principalId = requestingPrincipalId,
        origin = RequestOrigin.REMOTE_INTERFACE,
        intent = intent,
        targetResources = listOf(resourceId),
        proposedActions = listOf(actionName),
        priority = RequestPriority.NORMAL,
        createdAt = Instant.now(),
        correlationId = correlationId,
    )

    companion object {
        /**
         * A fixed, well-known [ResourceId] this coordinator's own
         * `createProvenance` gate always names as its target -- distinct
         * from [EvidenceRegistrationCoordinator.MEMORY_CORE_PROVENANCE_RESOURCE_ID]
         * (Scope Lock Section 18: "or new, distinct ones, is
         * implementation-plan work"). Not registered anywhere by this
         * class.
         */
        val DERIVATIVE_MEMORY_PROVENANCE_RESOURCE_ID: ResourceId = ResourceId("derivative-memory-provenance")

        /** The `createProvenance` gate's proposed-action name (Scope Lock Section 18's own suggested example). Not registered anywhere by this class. */
        const val CREATE_PROVENANCE_ACTION_NAME: String = "derivative.create-provenance"

        /**
         * A fixed, well-known [ResourceId] this coordinator's own
         * `registerDocument` gate always names as its target -- a
         * distinct literal value from [DERIVATIVE_MEMORY_PROVENANCE_RESOURCE_ID],
         * since the two Memory Core writes remain separately gated
         * proposals, mirroring [EvidenceRegistrationCoordinator]'s own
         * identical discipline. Not registered anywhere by this class.
         */
        val DERIVATIVE_MEMORY_DOCUMENT_REGISTRATION_RESOURCE_ID: ResourceId = ResourceId("derivative-memory-document-registration")

        /** The `registerDocument` gate's proposed-action name (Scope Lock Section 18's own suggested example). Not registered anywhere by this class. */
        const val REGISTER_DOCUMENT_ACTION_NAME: String = "derivative.register-document"

        /**
         * The fixed `Provenance.sourceType` classification for every
         * derivative this coordinator ever registers (Scope Lock Section
         * 7's own example value, adopted verbatim).
         */
        const val DERIVATIVE_SOURCE_TYPE: String = "document-ingestion-tier-a-derivative"
    }
}
