package parker.core.runtime

import parker.core.interfaces.AnalysisRequest
import parker.core.interfaces.AnalysisRequestId
import parker.core.interfaces.DerivativeGenerationId
import parker.core.interfaces.EvidenceArtifactId
import parker.core.interfaces.PrincipalId
import parker.core.interfaces.DerivativeCompletenessState
import parker.core.interfaces.DerivativeContentIdentity
import parker.core.interfaces.DerivativeGenerationRecord
import parker.core.interfaces.DerivativeOperationalOutcome
import parker.core.interfaces.DerivativeTransformation
import parker.core.interfaces.TierADerivativePayload
import parker.core.interfaces.TierAContentRetrievalOutcome

data class AnalysisGovernedContent(
    val derivativeGenerationId: DerivativeGenerationId,
    val record: DerivativeGenerationRecord,
    val payload: TierADerivativePayload,
)

data class AnalysisRetrievedEvidence(
    val evidenceArtifactId: EvidenceArtifactId,
    val manifest: AgentGatewayEvidenceManifestProjection,
    val governedContent: AnalysisGovernedContent? = null,
)

data class AnalysisRetrievalPackage(
    val requestId: AnalysisRequestId,
    val question: String,
    val analysisType: parker.core.interfaces.AnalysisType,
    val scope: parker.core.interfaces.AnalysisEvidenceScope,
    val evidence: List<AnalysisRetrievedEvidence>,
)

sealed interface AnalysisRequestResult {
    data class Accepted(val retrievalPackage: AnalysisRetrievalPackage) : AnalysisRequestResult
    data class ScopeRejected(val evidenceArtifactId: EvidenceArtifactId?, val reason: String) : AnalysisRequestResult
    data class Denied(val reason: String = "analysis request denied") : AnalysisRequestResult
}

/**
 * GA-4 transient analysis boundary. It validates a typed request and resolves only the explicit
 * evidence IDs through the existing governed manifest projection. It never reads files directly,
 * invokes a reasoning provider, or persists analysis state.
 */
internal class ParkerAnalysisRequestCoordinator(
    private val projection: AgentGatewayEvidenceProjection,
    private val analysisPrincipalId: PrincipalId,
    private val tierAContentRetrievalCoordinator: TierAContentRetrievalCoordinator,
) {
    suspend fun submit(request: AnalysisRequest): AnalysisRequestResult {
        val retrieved = mutableListOf<AnalysisRetrievedEvidence>()
        for (evidenceArtifactId in request.scope.evidenceArtifactIds) {
            when (val result = projection.retrieveEvidenceManifestAs(analysisPrincipalId, evidenceArtifactId)) {
                is AgentGatewayEvidenceManifestResult.Found -> {
                    val generationId = request.scope.derivativeGenerationIds[evidenceArtifactId.value]
                    if (generationId == null) {
                        return AnalysisRequestResult.ScopeRejected(evidenceArtifactId, "a governed derivative generation must be selected for content retrieval")
                    }
                    when (val content = tierAContentRetrievalCoordinator.retrieve(evidenceArtifactId, generationId)) {
                        is TierAContentRetrievalOutcome.Retrieved -> retrieved += AnalysisRetrievedEvidence(
                            evidenceArtifactId,
                            result.manifest,
                            AnalysisGovernedContent(generationId, content.record, content.payload),
                        )
                        else -> return AnalysisRequestResult.ScopeRejected(
                            evidenceArtifactId,
                            "governed content is unavailable for the selected derivative generation",
                        )
                    }
                }
                is AgentGatewayEvidenceManifestResult.NotFound -> return AnalysisRequestResult.ScopeRejected(evidenceArtifactId, "evidence artifact not found")
                is AgentGatewayEvidenceManifestResult.Denied -> return AnalysisRequestResult.Denied("evidence retrieval denied")
            }
        }
        return AnalysisRequestResult.Accepted(
            AnalysisRetrievalPackage(
                requestId = request.requestId,
                question = request.question,
                analysisType = request.analysisType,
                scope = request.scope,
                evidence = retrieved,
            ),
        )
    }
}
