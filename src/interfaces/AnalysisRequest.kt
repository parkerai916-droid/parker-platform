package parker.core.interfaces

import java.util.UUID

@JvmInline
value class AnalysisRequestId(val value: String) {
    init {
        require(value.matches(Regex("^analysis-[0-9a-fA-F-]{36}$"))) { "invalid AnalysisRequestId" }
    }

    companion object {
        fun new(): AnalysisRequestId = AnalysisRequestId("analysis-${UUID.randomUUID()}")
    }
}

enum class AnalysisType {
    ISSUE_ANALYSIS,
    CHRONOLOGY,
    CONTRADICTION_ANALYSIS,
    EVIDENCE_GAP_ANALYSIS,
    CLAIM_EVIDENCE_MAPPING,
    DOCUMENT_COMPARISON,
    FINANCIAL_ANALYSIS,
}

/** GA-4's deliberately narrow scope: an explicit, owner-authorised set of evidence IDs. */
data class AnalysisEvidenceScope(
    val evidenceArtifactIds: List<EvidenceArtifactId>,
    /** Optional, explicit governed derivative selection for content retrieval. */
    val derivativeGenerationIds: Map<String, DerivativeGenerationId> = emptyMap(),
) {
    init {
        require(evidenceArtifactIds.isNotEmpty()) { "analysis scope must contain at least one evidence artifact" }
        require(evidenceArtifactIds.size <= 100) { "analysis scope may contain at most 100 evidence artifacts" }
        require(evidenceArtifactIds.distinct().size == evidenceArtifactIds.size) { "analysis scope contains duplicate evidence artifacts" }
        require(derivativeGenerationIds.keys.all { key -> evidenceArtifactIds.any { it.value == key } }) {
            "derivative generations may only be selected for scoped evidence artifacts"
        }
    }
}

data class AnalysisRequest(
    val requestId: AnalysisRequestId,
    val question: String,
    val analysisType: AnalysisType,
    val scope: AnalysisEvidenceScope,
) {
    init {
        require(question.isNotBlank()) { "analysis question must not be blank" }
        require(question.length <= 8_000) { "analysis question exceeds the maximum length" }
        require(question.none { it == '\u0000' }) { "analysis question contains a control character" }
    }
}
