package parker.core.runtime

import parker.core.interfaces.EvidenceAnalysisRequest
import parker.core.interfaces.EvidenceAnalysisResult
import parker.core.interfaces.EvidenceRetrievalResult
import parker.core.interfaces.MemoryCoreRecord
import parker.core.interfaces.RelationshipEndpoint

/**
 * R0, Evidence Intelligence Grounding Validation Gate.
 *
 * This is a non-durable, provider-independent structural check over the
 * already-resolved results of one [EvidenceIntelligenceInputResolver] call.
 * It validates reference legitimacy only: it does not determine whether a
 * source semantically supports prose, and it does not create or retain any
 * grounding or governance state.
 *
 * The gate deliberately returns the same result list it receives. An invalid
 * reference throws before the enclosing Evidence Intelligence operation can
 * return a result to an acceptance or delivery caller; invalid prose is never
 * silently retained as a grounded result.
 */
internal object EvidenceIntelligenceGroundingValidationGate {

    /**
     * Validates every governed reference on every [EvidenceAnalysisResult.TransientOutput]
     * against the successful resolutions from this invocation.
     *
     * The successful sets are derived exclusively from [evidenceResults] and
     * [memoryCoreResults], which are the direct outputs of the current input
     * resolution. The request is also checked so a malformed resolver result
     * cannot enlarge the invocation's authority with an unrequested identity.
     */
    fun validate(
        request: EvidenceAnalysisRequest,
        evidenceResults: List<EvidenceRetrievalResult>,
        memoryCoreResults: List<Pair<RelationshipEndpoint, MemoryCoreRecord?>>,
        results: List<EvidenceAnalysisResult>,
    ): List<EvidenceAnalysisResult> {
        val resolvedEvidenceArtifactIds = evidenceResults
            .filterIsInstance<EvidenceRetrievalResult.Found>()
            .map { it.evidenceArtifactId }
            .toSet()
        check(resolvedEvidenceArtifactIds.all { it in request.evidenceArtifactIds }) {
            "Evidence Intelligence grounding validation failed: resolver returned an " +
                "unrequested EvidenceArtifactId"
        }

        val resolvedMemoryCoreReferences = memoryCoreResults
            .filter { (_, record) -> record != null }
            .map { (endpoint, _) -> endpoint }
            .toSet()
        check(resolvedMemoryCoreReferences.all { it in request.memoryCoreReferences }) {
            "Evidence Intelligence grounding validation failed: resolver returned an " +
                "unrequested Memory Core reference"
        }

        results.forEachIndexed { index, result ->
            if (result is EvidenceAnalysisResult.TransientOutput) {
                check(result.evidenceArtifactReferences.all { it in resolvedEvidenceArtifactIds }) {
                    "Evidence Intelligence grounding validation failed: TransientOutput[$index] " +
                        "carries an EvidenceArtifactId not successfully resolved in this invocation"
                }
                check(result.memoryCoreReferences.all { it in resolvedMemoryCoreReferences }) {
                    "Evidence Intelligence grounding validation failed: TransientOutput[$index] " +
                        "carries a Memory Core reference not successfully resolved in this invocation"
                }
            }
        }

        return results
    }
}
