package parker.core.interfaces

import java.nio.charset.StandardCharsets
import java.security.MessageDigest
import java.time.Instant

@JvmInline
value class HermesPreIngestionCorrectionId(val value: String) {
    init { require(value.matches(Regex("^hermes-preingestion-correction-[0-9a-f]{64}$"))) }
}

/** A governed correction produced before Parker has minted an EvidenceArtifactId. */
data class HermesPreIngestionCorrectedRepresentation(
    val representationId: HermesPreIngestionCorrectionId,
    val batchId: String,
    val sourceSha256: String,
    val machineResultStatus: HermesProcessingStatus,
    val issueIndex: Int,
    val machineIssueKind: HermesProcessingIssueKind,
    val machineIssueExplanation: String,
    val machineInterpretation: String?,
    val correctedInterpretation: String,
    val ownerExplanation: String,
    val ownerPrincipalId: PrincipalId,
    val decisionAt: Instant,
) {
    init {
        require(batchId.isNotBlank())
        require(sourceSha256.matches(Regex("^[0-9a-f]{64}$")))
        require(machineResultStatus == HermesProcessingStatus.REVIEW_REQUIRED)
        require(issueIndex >= 0)
        require(machineIssueExplanation.isNotBlank() && machineIssueExplanation.length <= 4096)
        require(machineInterpretation == null || machineInterpretation.length <= 4096)
        require(correctedInterpretation.isNotBlank() && correctedInterpretation.length <= 4096)
        require(ownerExplanation.isNotBlank() && ownerExplanation.length <= 4096)
        require(representationId == deriveId(batchId, sourceSha256, issueIndex, machineIssueKind,
            machineIssueExplanation, machineInterpretation, correctedInterpretation, ownerExplanation,
            ownerPrincipalId))
    }

    fun sameCorrectionAs(other: HermesPreIngestionCorrectedRepresentation): Boolean =
        batchId == other.batchId && sourceSha256 == other.sourceSha256 && issueIndex == other.issueIndex &&
            machineResultStatus == other.machineResultStatus && machineIssueKind == other.machineIssueKind &&
            machineIssueExplanation == other.machineIssueExplanation && machineInterpretation == other.machineInterpretation &&
            correctedInterpretation == other.correctedInterpretation && ownerExplanation == other.ownerExplanation &&
            ownerPrincipalId == other.ownerPrincipalId

    companion object {
        fun deriveId(
            batchId: String,
            sourceSha256: String,
            issueIndex: Int,
            machineIssueKind: HermesProcessingIssueKind,
            machineIssueExplanation: String,
            machineInterpretation: String?,
            correctedInterpretation: String,
            ownerExplanation: String,
            ownerPrincipalId: PrincipalId,
        ) = HermesPreIngestionCorrectionId("hermes-preingestion-correction-" + digest(
            batchId, sourceSha256, issueIndex.toString(), machineIssueKind.name, machineIssueExplanation,
            machineInterpretation ?: "", correctedInterpretation, ownerExplanation, ownerPrincipalId.value,
        ))

        private fun digest(vararg values: String): String {
            val d = MessageDigest.getInstance("SHA-256")
            values.forEach { value ->
                val bytes = value.toByteArray(StandardCharsets.UTF_8)
                d.update(byteArrayOf((bytes.size ushr 24).toByte(), (bytes.size ushr 16).toByte(), (bytes.size ushr 8).toByte(), bytes.size.toByte()))
                d.update(bytes)
            }
            return d.digest().joinToString("") { "%02x".format(it.toInt() and 0xff) }
        }
    }
}

sealed interface HermesPreIngestionCorrectionPublication {
    data class Created(val representation: HermesPreIngestionCorrectedRepresentation) : HermesPreIngestionCorrectionPublication
    data class AlreadyPublished(val representation: HermesPreIngestionCorrectedRepresentation) : HermesPreIngestionCorrectionPublication
    data class Failed(val reason: String) : HermesPreIngestionCorrectionPublication
    data object NotRequired : HermesPreIngestionCorrectionPublication
}

data class HermesPreIngestionCorrectionEvidenceBinding(
    val correctionId: HermesPreIngestionCorrectionId,
    val evidenceArtifactId: EvidenceArtifactId,
    val sourceSha256: String,
    val createdAt: Instant,
) {
    init {
        require(sourceSha256.matches(Regex("^[0-9a-f]{64}$")))
    }
}

data class HermesPreIngestionCorrectionLineage(
    val binding: HermesPreIngestionCorrectionEvidenceBinding,
    val correction: HermesPreIngestionCorrectedRepresentation,
) {
    init {
        require(binding.correctionId == correction.representationId)
        require(binding.sourceSha256 == correction.sourceSha256)
    }

    fun correctedContent(evidenceArtifactId: EvidenceArtifactId): HermesOwnerCorrectedContent =
        HermesOwnerCorrectedContent(
            evidenceArtifactId = evidenceArtifactId,
            correctionId = correction.representationId,
            sourceSha256 = binding.sourceSha256,
            issueIndex = correction.issueIndex,
            machineInterpretation = correction.machineInterpretation,
            correctedInterpretation = correction.correctedInterpretation,
            ownerExplanation = correction.ownerExplanation,
            ownerPrincipalId = correction.ownerPrincipalId,
            decisionAt = correction.decisionAt,
        )
}

/** Issue-scoped governed content; it is not a claim that the whole document was validated. */
data class HermesOwnerCorrectedContent(
    val evidenceArtifactId: EvidenceArtifactId,
    val correctionId: HermesPreIngestionCorrectionId,
    val sourceSha256: String,
    val issueIndex: Int,
    val machineInterpretation: String?,
    val correctedInterpretation: String,
    val ownerExplanation: String,
    val ownerPrincipalId: PrincipalId,
    val decisionAt: Instant,
) {
    val authority: String = "OWNER_AUTHORIZED_CORRECTION"
    val scope: String = "ISSUE"
}

sealed interface HermesPreIngestionCorrectionBindingResult {
    data class Bound(val binding: HermesPreIngestionCorrectionEvidenceBinding) : HermesPreIngestionCorrectionBindingResult
    data class AlreadyBound(val binding: HermesPreIngestionCorrectionEvidenceBinding) : HermesPreIngestionCorrectionBindingResult
    data class Conflict(val reason: String) : HermesPreIngestionCorrectionBindingResult
    data class Failed(val reason: String) : HermesPreIngestionCorrectionBindingResult
}

interface HermesPreIngestionCorrectionRegistry {
    suspend fun publish(representation: HermesPreIngestionCorrectedRepresentation): HermesPreIngestionCorrectionPublication
    suspend fun findForDecision(result: HermesProcessingResult, decision: HermesProcessingHumanDecision): HermesPreIngestionCorrectedRepresentation?
    suspend fun bindToEvidence(
        representation: HermesPreIngestionCorrectedRepresentation,
        evidenceArtifactId: EvidenceArtifactId,
        evidenceSourceSha256: String,
        createdAt: Instant,
    ): HermesPreIngestionCorrectionBindingResult
    suspend fun findLineageForEvidence(evidenceArtifactId: EvidenceArtifactId): HermesPreIngestionCorrectionLineage?
}
