package parker.core.interfaces

import java.time.Instant

@JvmInline
value class GovernedAnalysisResultId(val value: String) {
    init { require(value.matches(Regex("^governed-analysis-[0-9a-fA-F-]{36}$"))) }
    companion object {
        fun forRequest(requestId: AnalysisRequestId): GovernedAnalysisResultId =
            GovernedAnalysisResultId("governed-analysis-${requestId.value.removePrefix("analysis-")}")
    }
}

/** Immutable, case-scoped analytical output; never canonical evidence or a derivative. */
data class GovernedAnalysisEvidenceScopeEntry(
    val evidenceArtifactId: EvidenceArtifactId,
    val associationId: CaseEvidenceAssociationId?,
    val occurrenceIds: List<EvidenceOccurrenceId>,
    val derivativeGenerationId: DerivativeGenerationId,
    val sourceSha256: String?,
    val sourceLabel: String?,
)

data class GovernedAnalysisResult(
    val schemaVersion: Int,
    val resultId: GovernedAnalysisResultId,
    val analysisRequestId: AnalysisRequestId,
    val caseId: CaseId,
    val caseName: String?,
    val question: String,
    val analysisType: AnalysisType,
    val generatedAt: Instant,
    val providerIdentity: String?,
    val modelIdentity: String?,
    val hermesSessionId: String?,
    val profile: String,
    val evidenceScope: List<GovernedAnalysisEvidenceScopeEntry>,
    val analysisText: String,
    val structuredResult: StructuredAnalysisResult,
    val warnings: List<String> = emptyList(),
) {
    init {
        require(schemaVersion == 1)
        require(question.isNotBlank() && analysisText.isNotBlank() && profile.isNotBlank())
        require(evidenceScope.isNotEmpty())
        require(evidenceScope.map { it.evidenceArtifactId }.distinct().size == evidenceScope.size)
    }
}

sealed interface GovernedAnalysisResultCreationOutcome {
    data class Created(val result: GovernedAnalysisResult) : GovernedAnalysisResultCreationOutcome
    data class AlreadyPresent(val result: GovernedAnalysisResult) : GovernedAnalysisResultCreationOutcome
    data class ConflictingResult(val analysisRequestId: AnalysisRequestId) : GovernedAnalysisResultCreationOutcome
}

interface GovernedAnalysisResultStorage {
    suspend fun createOrGet(result: GovernedAnalysisResult): GovernedAnalysisResultCreationOutcome
    suspend fun findByAnalysisRequestId(analysisRequestId: AnalysisRequestId): GovernedAnalysisResult?
}

sealed class GovernedAnalysisResultStorageException(message: String, cause: Throwable? = null) : RuntimeException(message, cause) {
    class InvalidStorageRoot(path: String, reason: String) : GovernedAnalysisResultStorageException("Governed analysis storage root '$path' is invalid: $reason")
    class UnsafeIdentifier(id: GovernedAnalysisResultId) : GovernedAnalysisResultStorageException("Unsafe governed analysis result identifier '${id.value}'")
    class PersistenceFailure(message: String, cause: Throwable) : GovernedAnalysisResultStorageException(message, cause)
    class CorruptRecord(id: GovernedAnalysisResultId, message: String, cause: Throwable? = null) : GovernedAnalysisResultStorageException("Governed analysis result '${id.value}' is corrupt: $message", cause)
}
