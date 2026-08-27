package parker.core.interfaces

import java.time.Instant

@JvmInline
value class HumanVerificationRecordId(val value: String) {
    init { require(value.isNotBlank() && value.length <= 1_024) { "HumanVerificationRecordId must be bounded and non-blank" } }
}

enum class HumanVerificationOutcome { REVIEW_PASSED, REVIEW_FAILED, PARTIALLY_VERIFIED }

data class HumanVerificationCharacterScope(
    val pageNumber: Int,
    val startOffsetInclusive: Int,
    val endOffsetExclusive: Int,
) {
    init {
        require(pageNumber >= 1)
        require(startOffsetInclusive >= 0 && endOffsetExclusive > startOffsetInclusive)
    }
}

data class HumanVerificationRecord(
    val humanVerificationRecordId: HumanVerificationRecordId,
    val evidenceArtifactId: EvidenceArtifactId,
    val derivativeGenerationId: DerivativeGenerationId,
    val reviewedPageScope: OcrPageScope,
    val reviewedCharacterScopes: List<HumanVerificationCharacterScope> = emptyList(),
    val reviewerPrincipalId: PrincipalId,
    val reviewedAt: Instant,
    val outcome: HumanVerificationOutcome,
    val reviewArtifactSha256: OcrSha256Digest,
    val sensitiveNotes: String? = null,
) {
    init {
        require(reviewedPageScope.pageNumbers.isNotEmpty()) { "Human verification page scope must not be empty" }
        require(reviewedCharacterScopes.size <= MAX_CHARACTER_SCOPES)
        require(reviewedCharacterScopes.all { it.pageNumber in reviewedPageScope.pageNumbers })
        require(sensitiveNotes == null || sensitiveNotes.length in 1..MAX_NOTES_CHARACTERS)
    }

    override fun toString(): String =
        "HumanVerificationRecord(humanVerificationRecordId=$humanVerificationRecordId, evidenceArtifactId=$evidenceArtifactId, " +
            "derivativeGenerationId=$derivativeGenerationId, reviewedPageScope=$reviewedPageScope, " +
            "reviewedCharacterScopeCount=${reviewedCharacterScopes.size}, reviewerPrincipalId=$reviewerPrincipalId, " +
            "reviewedAt=$reviewedAt, outcome=$outcome, reviewArtifactSha256=$reviewArtifactSha256, sensitiveNotes=<redacted>)"

    companion object {
        const val MAX_CHARACTER_SCOPES = 1_000
        const val MAX_NOTES_CHARACTERS = 4_096
    }
}

sealed class HumanVerificationStorageException(message: String, cause: Throwable? = null) : RuntimeException(message, cause) {
    class DuplicateIdentifier(id: HumanVerificationRecordId) : HumanVerificationStorageException("Human verification record '${id.value}' already exists")
    class UnsafeIdentifier(id: HumanVerificationRecordId) : HumanVerificationStorageException("Human verification record identifier '${id.value}' is unsafe")
    class InvalidStorageRoot(path: String, reason: String) : HumanVerificationStorageException("Human verification storage root '$path' is invalid: $reason")
    class PersistenceFailure(message: String, cause: Throwable) : HumanVerificationStorageException(message, cause)
    class CorruptRecord(id: HumanVerificationRecordId, message: String, cause: Throwable? = null) :
        HumanVerificationStorageException("Human verification record '${id.value}' is corrupt: $message", cause)
    class UnsupportedRepresentationVersion(id: HumanVerificationRecordId, val version: Int) :
        HumanVerificationStorageException("Human verification record '${id.value}' representation version $version is unsupported")
}

interface HumanVerificationStorage {
    suspend fun prepare(record: HumanVerificationRecord)
    suspend fun publishPrepared(humanVerificationRecordId: HumanVerificationRecordId)
    suspend fun retrieve(humanVerificationRecordId: HumanVerificationRecordId): HumanVerificationRecord?
    /** All exact-pair records, sorted only by opaque record id for deterministic presentation; ordering conveys no precedence. */
    suspend fun listForExactGeneration(
        evidenceArtifactId: EvidenceArtifactId,
        derivativeGenerationId: DerivativeGenerationId,
    ): List<HumanVerificationRecord>
}
