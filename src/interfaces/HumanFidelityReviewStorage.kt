package parker.core.interfaces

import java.time.Instant

sealed interface HumanFidelityReviewPreparationResult {
    data object Prepared : HumanFidelityReviewPreparationResult
    data object AlreadyPrepared : HumanFidelityReviewPreparationResult
    data object AlreadyPublished : HumanFidelityReviewPreparationResult
}

sealed interface HumanFidelityReviewPublicationResult {
    data object Published : HumanFidelityReviewPublicationResult
    data object AlreadyPublished : HumanFidelityReviewPublicationResult
}

sealed class HumanFidelityReviewStorageException(message: String, cause: Throwable? = null) : RuntimeException(message, cause) {
    class ConflictingIdentifier(val reviewId: HumanFidelityReviewId) :
        HumanFidelityReviewStorageException("Human fidelity review '${reviewId.value}' already exists with different canonical content")
    class UnsafeIdentifier(val reviewId: HumanFidelityReviewId) :
        HumanFidelityReviewStorageException("Human fidelity review identifier '${reviewId.value}' is unsafe")
    class InvalidStorageRoot(path: String, reason: String) :
        HumanFidelityReviewStorageException("Human fidelity review storage root '$path' is invalid: $reason")
    class MissingPreparedRecord(val reviewId: HumanFidelityReviewId) :
        HumanFidelityReviewStorageException("No prepared human fidelity review exists for '${reviewId.value}'")
    class IncompleteAudit(val reviewId: HumanFidelityReviewId, eventType: HumanFidelityGovernanceAuditEventType) :
        HumanFidelityReviewStorageException("Human fidelity review '${reviewId.value}' lacks required ${eventType.name} audit fact")
    class PersistenceFailure(message: String, cause: Throwable) : HumanFidelityReviewStorageException(message, cause)
    class CorruptRecord(val reviewId: HumanFidelityReviewId, message: String, cause: Throwable? = null) :
        HumanFidelityReviewStorageException("Human fidelity review '${reviewId.value}' is corrupt: $message", cause)
    class UnsupportedRepresentationVersion(val reviewId: HumanFidelityReviewId, val version: Int) :
        HumanFidelityReviewStorageException("Human fidelity review '${reviewId.value}' representation version $version is unsupported")
}

interface HumanFidelityReviewStorage {
    suspend fun prepare(record: HumanFidelityReviewRecord): HumanFidelityReviewPreparationResult
    suspend fun publishPrepared(reviewId: HumanFidelityReviewId): HumanFidelityReviewPublicationResult
    suspend fun retrieve(reviewId: HumanFidelityReviewId): HumanFidelityReviewRecord?
    suspend fun listForExactTarget(target: HumanFidelityReviewTarget): List<HumanFidelityReviewRecord>
}

@JvmInline
value class HumanFidelityGovernanceAuditEventId(val value: String) {
    init {
        require(value.matches(Regex("^fidelity-audit-[0-9a-f]{64}$"))) {
            "HumanFidelityGovernanceAuditEventId must be fidelity-audit- followed by a lowercase SHA-256"
        }
    }
}

enum class HumanFidelityGovernanceAuditEventType {
    REVIEW_PREPARED,
    REVIEW_PUBLISHED,
    REVIEW_DUPLICATE_CONFIRMED,
}

enum class HumanFidelityGovernanceAuditOutcome { SUCCEEDED, EXACT_DUPLICATE }

data class HumanFidelityGovernanceAuditRecord(
    val eventId: HumanFidelityGovernanceAuditEventId,
    val eventType: HumanFidelityGovernanceAuditEventType,
    val recordedAt: Instant,
    val actorPrincipalId: PrincipalId,
    val reviewId: HumanFidelityReviewId,
    val target: HumanFidelityReviewTarget,
    val reviewPayloadSha256: OcrSha256Digest,
    val outcome: HumanFidelityGovernanceAuditOutcome,
    val factualDetail: String? = null,
) {
    init {
        require(factualDetail == null || (factualDetail.isNotBlank() && factualDetail.length <= 1_024)) {
            "Human fidelity audit detail must be absent or contain 1..1024 characters"
        }
        require(eventId == HumanFidelityGovernanceAuditIdentity.derive(
            eventType, actorPrincipalId, reviewId, target, reviewPayloadSha256, outcome,
        )) { "Human fidelity audit event identity must match its canonical facts" }
    }
}

object HumanFidelityGovernanceAuditIdentity {
    fun derive(
        eventType: HumanFidelityGovernanceAuditEventType,
        actorPrincipalId: PrincipalId,
        reviewId: HumanFidelityReviewId,
        target: HumanFidelityReviewTarget,
        reviewPayloadSha256: OcrSha256Digest,
        outcome: HumanFidelityGovernanceAuditOutcome,
    ): HumanFidelityGovernanceAuditEventId = HumanFidelityGovernanceAuditEventId(
        "fidelity-audit-" + fidelityDigest(
            listOf(
                eventType.name, actorPrincipalId.value, reviewId.value,
                target.evidenceArtifactId.value, target.sourceSha256.value, target.preparationIdentity.value,
                target.derivativeGenerationId.value, target.derivativeGenerationSha256.value,
                target.derivativeContentSha256.value, reviewPayloadSha256.value, outcome.name,
            ).joinToString("\u0000"),
        ).value,
    )
}

sealed interface HumanFidelityGovernanceAuditAppendResult {
    data object Appended : HumanFidelityGovernanceAuditAppendResult
    data object AlreadyPresent : HumanFidelityGovernanceAuditAppendResult
}

sealed class HumanFidelityGovernanceAuditException(message: String, cause: Throwable? = null) : RuntimeException(message, cause) {
    class ConflictingIdentifier(val eventId: HumanFidelityGovernanceAuditEventId) :
        HumanFidelityGovernanceAuditException("Human fidelity audit event '${eventId.value}' conflicts with an existing event")
    class InvalidStorageRoot(path: String, reason: String) :
        HumanFidelityGovernanceAuditException("Human fidelity audit root '$path' is invalid: $reason")
    class PersistenceFailure(message: String, cause: Throwable) : HumanFidelityGovernanceAuditException(message, cause)
    class CorruptRecord(val eventId: HumanFidelityGovernanceAuditEventId, message: String, cause: Throwable? = null) :
        HumanFidelityGovernanceAuditException("Human fidelity audit event '${eventId.value}' is corrupt: $message", cause)
    class UnsupportedRepresentationVersion(val eventId: HumanFidelityGovernanceAuditEventId, val version: Int) :
        HumanFidelityGovernanceAuditException("Human fidelity audit event '${eventId.value}' representation version $version is unsupported")
}

interface HumanFidelityGovernanceAudit {
    suspend fun append(record: HumanFidelityGovernanceAuditRecord): HumanFidelityGovernanceAuditAppendResult
    suspend fun retrieve(eventId: HumanFidelityGovernanceAuditEventId): HumanFidelityGovernanceAuditRecord?
    suspend fun listForReview(reviewId: HumanFidelityReviewId): List<HumanFidelityGovernanceAuditRecord>
}
