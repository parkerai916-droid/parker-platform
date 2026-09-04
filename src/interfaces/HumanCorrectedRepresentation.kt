package parker.core.interfaces

import java.nio.charset.StandardCharsets
import java.security.MessageDigest
import java.time.Instant

const val HUMAN_CORRECTED_REGION_TRANSCRIPTION_KIND = "HUMAN_CORRECTED_REGION_TRANSCRIPTION"
val HUMAN_TRANSCRIPTION_CORRECTION_PURPOSE =
    AuthorizationPurposeId("document-ingestion.human-transcription-correction")

@JvmInline
value class CorrectionProposalId(val value: String) {
    init { require(value.matches(Regex("^correction-proposal-[0-9a-f]{64}$"))) }
}

@JvmInline
value class CorrectionAcceptanceId(val value: String) {
    init { require(value.matches(Regex("^correction-acceptance-[0-9a-f]{64}$"))) }
}

data class HumanTranscriptionCorrectionProposal(
    val proposalId: CorrectionProposalId,
    val reviewId: HumanFidelityReviewId,
    val discrepancyId: FidelityDiscrepancyId,
    val target: HumanFidelityReviewTarget,
    val providerValue: String,
    val acceptedSourceValue: String,
    val proposerPrincipalId: PrincipalId,
    val proposedAt: Instant,
    val reason: String,
) {
    init {
        require(providerValue.isNotEmpty() && providerValue.length <= 4096)
        require(acceptedSourceValue.isNotEmpty() && acceptedSourceValue.length <= 4096)
        require(reason.isNotBlank() && reason.length <= 4096)
        require(proposalId == deriveId(reviewId, discrepancyId, target, providerValue, acceptedSourceValue,
            proposerPrincipalId, proposedAt, reason))
    }

    companion object {
        fun deriveId(
            reviewId: HumanFidelityReviewId,
            discrepancyId: FidelityDiscrepancyId,
            target: HumanFidelityReviewTarget,
            providerValue: String,
            acceptedSourceValue: String,
            proposerPrincipalId: PrincipalId,
            proposedAt: Instant,
            reason: String,
        ) = CorrectionProposalId("correction-proposal-" + correctedSha256(
            reviewId.value, discrepancyId.value, target.correctedIdentity(), providerValue,
            acceptedSourceValue, proposerPrincipalId.value, proposedAt.toString(), reason,
        ))
    }
}

data class HumanTranscriptionCorrectionAcceptance(
    val acceptanceId: CorrectionAcceptanceId,
    val reviewId: HumanFidelityReviewId,
    val target: HumanFidelityReviewTarget,
    val proposalIds: List<CorrectionProposalId>,
    val acceptingPrincipalId: PrincipalId,
    val acceptedAt: Instant,
    val authorizationPurpose: AuthorizationPurposeId,
    val supersedesAcceptanceId: CorrectionAcceptanceId? = null,
) {
    init {
        require(proposalIds.isNotEmpty() && proposalIds.size <= 1000)
        require(proposalIds.distinct().size == proposalIds.size)
        require(authorizationPurpose == HUMAN_TRANSCRIPTION_CORRECTION_PURPOSE)
        require(supersedesAcceptanceId != acceptanceId)
        require(acceptanceId == deriveId(reviewId, target, proposalIds, acceptingPrincipalId, acceptedAt,
            authorizationPurpose, supersedesAcceptanceId))
    }

    companion object {
        fun deriveId(
            reviewId: HumanFidelityReviewId,
            target: HumanFidelityReviewTarget,
            proposalIds: Collection<CorrectionProposalId>,
            acceptingPrincipalId: PrincipalId,
            acceptedAt: Instant,
            authorizationPurpose: AuthorizationPurposeId = HUMAN_TRANSCRIPTION_CORRECTION_PURPOSE,
            supersedesAcceptanceId: CorrectionAcceptanceId? = null,
        ) = CorrectionAcceptanceId("correction-acceptance-" + correctedSha256(
            reviewId.value, target.correctedIdentity(), proposalIds.map { it.value }.sorted().joinToString("\u0000"),
            acceptingPrincipalId.value, acceptedAt.toString(), authorizationPurpose.value,
            supersedesAcceptanceId?.value ?: "",
        ))
    }
}

data class HumanCorrectedRegionTranscription(
    val representationVersion: Int = 1,
    val derivativeGenerationId: DerivativeGenerationId,
    val representationKind: String = HUMAN_CORRECTED_REGION_TRANSCRIPTION_KIND,
    val target: HumanFidelityReviewTarget,
    val reviewId: HumanFidelityReviewId,
    val proposals: List<HumanTranscriptionCorrectionProposal>,
    val acceptance: HumanTranscriptionCorrectionAcceptance,
    val correctedTranscriptionBlocks: List<String>,
    val correctedContentSha256: OcrSha256Digest,
    val createdAt: Instant,
    val producerIdentity: String = "parker.human-correction",
    val schemaIdentity: String = "human-corrected-region-transcription-v1",
) {
    init {
        require(representationVersion == 1)
        require(representationKind == HUMAN_CORRECTED_REGION_TRANSCRIPTION_KIND)
        require(producerIdentity == "parker.human-correction")
        require(schemaIdentity == "human-corrected-region-transcription-v1")
        require(proposals.isNotEmpty() && proposals.size <= 1000)
        require(proposals.map { it.proposalId }.distinct().size == proposals.size)
        require(proposals.all { it.reviewId == reviewId && it.target == target })
        require(acceptance.reviewId == reviewId && acceptance.target == target)
        require(acceptance.proposalIds.toSet() == proposals.map { it.proposalId }.toSet())
        require(correctedTranscriptionBlocks.isNotEmpty() && correctedTranscriptionBlocks.size <= 1000)
        require(correctedTranscriptionBlocks.all { it.length <= 500_000 })
        require(correctedContentSha256 == contentDigest(correctedTranscriptionBlocks))
        require(derivativeGenerationId == deriveGenerationId(target, reviewId, acceptance, correctedContentSha256))
    }

    companion object {
        fun contentDigest(blocks: List<String>) = OcrSha256Digest(correctedSha256(*blocks.toTypedArray()))

        fun deriveGenerationId(
            target: HumanFidelityReviewTarget,
            reviewId: HumanFidelityReviewId,
            acceptance: HumanTranscriptionCorrectionAcceptance,
            contentSha256: OcrSha256Digest,
        ) = DerivativeGenerationId("human-corrected-" + correctedSha256(
            target.correctedIdentity(), reviewId.value, acceptance.acceptanceId.value, contentSha256.value,
        ))
    }
}

data class HumanCorrectionAuthorityScope(
    val principalId: PrincipalId,
    val authorizationPurpose: AuthorizationPurposeId?,
    val target: HumanFidelityReviewTarget,
    val reviewId: HumanFidelityReviewId,
    val verificationCredential: OwnerVerificationCredential? = null,
)

/** Ephemeral verification input: deliberately non-serializable and redacted when rendered. */
class OwnerVerificationCredential private constructor(private val secretBytes: ByteArray) {
    internal fun constantTimeEquals(expected: ByteArray): Boolean = MessageDigest.isEqual(expected, secretBytes)
    override fun toString(): String = "OwnerVerificationCredential([REDACTED])"
    companion object {
        fun presented(value: String?): OwnerVerificationCredential? = value?.takeIf { it.isNotBlank() }
            ?.let { OwnerVerificationCredential(it.toByteArray(StandardCharsets.UTF_8)) }
    }
}

enum class GovernedPrincipalRole { OWNER }

data class OpaqueOwnerPrincipal(val principalId: PrincipalId, val role: GovernedPrincipalRole = GovernedPrincipalRole.OWNER) {
    init { require(principalId.value.matches(Regex("^owner-[0-9a-f]{64}$"))) }
}

enum class HumanCorrectionDenialReason {
    WRONG_PRINCIPAL, MISSING_OR_WRONG_PURPOSE, PURPOSE_NOT_ACTIVE, TARGET_MISMATCH,
    MISSING_OR_INVALID_VERIFICATION_CREDENTIAL, PERMISSION_POLICY_DENIED,
}

sealed interface HumanCorrectionPermissionResult {
    data object Authorized : HumanCorrectionPermissionResult
    data class Denied(val reason: HumanCorrectionDenialReason) : HumanCorrectionPermissionResult
}

fun interface HumanCorrectionPermissionEvaluator {
    suspend fun evaluate(authority: HumanCorrectionAuthorityScope, target: HumanFidelityReviewTarget,
                         reviewId: HumanFidelityReviewId): HumanCorrectionPermissionResult
}

sealed interface HumanCorrectedRepresentationPrepareResult {
    data object Prepared : HumanCorrectedRepresentationPrepareResult
    data object AlreadyPrepared : HumanCorrectedRepresentationPrepareResult
    data object AlreadyPublished : HumanCorrectedRepresentationPrepareResult
}

interface HumanCorrectedRepresentationStorage {
    suspend fun prepare(representation: HumanCorrectedRegionTranscription): HumanCorrectedRepresentationPrepareResult
    suspend fun publishPrepared(id: DerivativeGenerationId)
    suspend fun retrieve(id: DerivativeGenerationId): HumanCorrectedRegionTranscription?

    /** Exact provenance lookup only; ordering carries no precedence or winner semantics. */
    suspend fun listForExactTarget(target: HumanFidelityReviewTarget): List<HumanCorrectedRegionTranscription> = emptyList()
}

enum class HumanCorrectionAuditEventType { CORRECTED_REPRESENTATION_PREPARED, CORRECTED_REPRESENTATION_PUBLISHED }

data class HumanCorrectionAuditRecord(
    val eventId: String,
    val eventType: HumanCorrectionAuditEventType,
    val occurredAt: Instant,
    val actorPrincipalId: PrincipalId,
    val representationId: DerivativeGenerationId,
    val target: HumanFidelityReviewTarget,
    val reviewId: HumanFidelityReviewId,
    val acceptanceId: CorrectionAcceptanceId,
    val contentSha256: OcrSha256Digest,
) {
    init {
        require(eventId == deriveId(eventType, actorPrincipalId, representationId, target, reviewId, acceptanceId, contentSha256))
    }

    companion object {
        fun deriveId(
            eventType: HumanCorrectionAuditEventType,
            actorPrincipalId: PrincipalId,
            representationId: DerivativeGenerationId,
            target: HumanFidelityReviewTarget,
            reviewId: HumanFidelityReviewId,
            acceptanceId: CorrectionAcceptanceId,
            contentSha256: OcrSha256Digest,
        ) = "correction-audit-" + correctedSha256(
            eventType.name, actorPrincipalId.value, representationId.value, target.correctedIdentity(), reviewId.value,
            acceptanceId.value, contentSha256.value,
        )
    }
}

interface HumanCorrectionAudit {
    suspend fun append(record: HumanCorrectionAuditRecord)
    suspend fun listForRepresentation(id: DerivativeGenerationId): List<HumanCorrectionAuditRecord>
}

data class GovernedHumanCorrectionRequest(
    val target: HumanFidelityReviewTarget,
    val reviewId: HumanFidelityReviewId,
    val proposals: List<HumanTranscriptionCorrectionProposal>,
    val acceptance: HumanTranscriptionCorrectionAcceptance,
    val authority: HumanCorrectionAuthorityScope,
)

enum class GovernedHumanCorrectionFailureReason {
    AUTHORITY_EVALUATION_FAILED, TARGET_OR_REVIEW_MISMATCH, PROVIDER_OR_REVIEW_NOT_FOUND,
    REVIEW_CONFLICT, INVALID_CORRECTION, STORAGE_FAILURE, CANONICAL_READBACK_FAILED,
}

sealed interface GovernedHumanCorrectionResult {
    data class Created(val representation: HumanCorrectedRegionTranscription) : GovernedHumanCorrectionResult
    data class AlreadyCreated(val representation: HumanCorrectedRegionTranscription) : GovernedHumanCorrectionResult
    data class AuthorizationDenied(val reason: HumanCorrectionDenialReason) : GovernedHumanCorrectionResult
    data class Failed(val reason: GovernedHumanCorrectionFailureReason) : GovernedHumanCorrectionResult
}

interface GovernedHumanCorrectionService {
    suspend fun create(request: GovernedHumanCorrectionRequest): GovernedHumanCorrectionResult
}

data class ResolvedProviderTranscription(
    val target: HumanFidelityReviewTarget,
    val transcription: OrdinaryRegionTranscriptionDerivative,
)

fun interface HumanCorrectionProviderResolver {
    suspend fun resolve(target: HumanFidelityReviewTarget): ResolvedProviderTranscription?
}

fun interface HumanCorrectedRepresentationEligibilityEvaluator {
    suspend fun evaluate(representation: HumanCorrectedRegionTranscription): SourceConfirmedEligibility
}

data class HumanCorrectedRepresentationPresentation(
    val representation: HumanCorrectedRegionTranscription,
    val sourceConfirmedEligibility: SourceConfirmedEligibility,
)

internal fun HumanFidelityReviewTarget.correctedIdentity() = listOf(
    evidenceArtifactId.value, sourceSha256.value, preparationIdentity.value, derivativeGenerationId.value,
    derivativeGenerationSha256.value, derivativeContentSha256.value,
).joinToString("\u0000")

internal fun correctedSha256(vararg values: String): String {
    val digest = MessageDigest.getInstance("SHA-256")
    values.forEach { value ->
        val bytes = value.toByteArray(StandardCharsets.UTF_8)
        digest.update(byteArrayOf((bytes.size ushr 24).toByte(), (bytes.size ushr 16).toByte(),
            (bytes.size ushr 8).toByte(), bytes.size.toByte()))
        digest.update(bytes)
    }
    return digest.digest().joinToString("") { "%02x".format(it.toInt() and 0xff) }
}
