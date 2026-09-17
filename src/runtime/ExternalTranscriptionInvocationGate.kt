package parker.core.runtime

import java.time.Instant
import java.util.UUID
import parker.core.interfaces.AuthorizationPurposeId
import parker.core.interfaces.EvidenceArtifactId
import parker.core.interfaces.ExecutionRequest
import parker.core.interfaces.PrincipalId
import parker.core.interfaces.RequestId
import parker.core.interfaces.RequestOrigin
import parker.core.interfaces.RequestPriority

/** Pure request construction for the separately owner-selected external-transcription act. */
object ExternalTranscriptionInvocationGate {
    val AUTHORIZATION_PURPOSE = AuthorizationPurposeId("evidence-intelligence.external-transcription")
    const val ACTION_NAME = "evidence-intelligence.transcribe-external"
    const val EVIDENCE_ARTIFACT_ID_METADATA_KEY = "evidenceArtifactId"

    /**
     * Builds one fresh, request-scoped authorization proposal for exactly one selected artifact.
     * It performs no permission evaluation, evidence access, OCR, provider call, or persistence.
     */
    fun buildExecutionRequest(
        ownerPrincipalId: PrincipalId,
        evidenceArtifactId: EvidenceArtifactId,
    ): ExecutionRequest {
        val requestId = UUID.randomUUID()
        val correlationId = UUID.randomUUID()
        return ExecutionRequest(
            requestId = RequestId("external-transcription-$requestId"),
            principalId = ownerPrincipalId,
            origin = RequestOrigin.REMOTE_INTERFACE,
            intent = "Authorize external transcription of owner-selected evidence",
            targetResources = listOf(EvidenceIntelligenceInvocationGate.EVIDENCE_INTELLIGENCE_INVOCATION_RESOURCE_ID),
            proposedActions = listOf(ACTION_NAME),
            priority = RequestPriority.NORMAL,
            createdAt = Instant.now(),
            correlationId = "external-transcription-$correlationId",
            metadata = mapOf(EVIDENCE_ARTIFACT_ID_METADATA_KEY to evidenceArtifactId.value),
            authorizationPurpose = AUTHORIZATION_PURPOSE,
        )
    }

    /** The same governed external-transcription purpose/action for one Owner-authorised batch. */
    fun buildBatchExecutionRequest(
        ownerPrincipalId: PrincipalId,
        batchId: String,
        caseId: String,
    ): ExecutionRequest {
        val requestId = UUID.randomUUID()
        return ExecutionRequest(
            requestId = RequestId("external-transcription-batch-$requestId"),
            principalId = ownerPrincipalId,
            origin = RequestOrigin.REMOTE_INTERFACE,
            intent = "Authorize external transcription for one owner-selected ingestion batch",
            targetResources = listOf(EvidenceIntelligenceInvocationGate.EVIDENCE_INTELLIGENCE_INVOCATION_RESOURCE_ID),
            proposedActions = listOf(ACTION_NAME),
            priority = RequestPriority.NORMAL,
            createdAt = Instant.now(),
            correlationId = "external-transcription-batch-$requestId",
            metadata = mapOf("batchId" to batchId, "caseId" to caseId),
            authorizationPurpose = AUTHORIZATION_PURPOSE,
        )
    }

    /** Owner-only request used to establish the durable standing policy, never an evidence grant. */
    fun buildStandingPolicyExecutionRequest(ownerPrincipalId: PrincipalId): ExecutionRequest {
        val requestId = UUID.randomUUID()
        return ExecutionRequest(
            requestId = RequestId("external-transcription-standing-policy-$requestId"),
            principalId = ownerPrincipalId,
            origin = RequestOrigin.REMOTE_INTERFACE,
            intent = "Establish standing Owner approval for authoritative external transcription",
            targetResources = listOf(EvidenceIntelligenceInvocationGate.EVIDENCE_INTELLIGENCE_INVOCATION_RESOURCE_ID),
            proposedActions = listOf(ACTION_NAME),
            priority = RequestPriority.NORMAL,
            createdAt = Instant.now(),
            correlationId = "external-transcription-standing-policy-$requestId",
            metadata = mapOf("policyVersion" to STANDING_EXTERNAL_TRANSCRIPTION_POLICY_VERSION),
            authorizationPurpose = AUTHORIZATION_PURPOSE,
        )
    }
}
