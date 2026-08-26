package parker.core.interfaces

/** Byte-exact, provider-neutral full-source request prepared after authorization and custody verification. */
class ExternalTranscriptionRequest(
    val processingRepresentation: OcrProcessingRepresentation,
    val maximumPageCount: Int,
) {
    val sourceEvidenceArtifactId get() = processingRepresentation.processingProvenance.sourceEvidenceArtifactId
    val mediaType get() = processingRepresentation.processingProvenance.representationMediaType
    val sourceManifestSha256 get() = processingRepresentation.processingProvenance.sourceManifestSha256
    val processingProvenance get() = processingRepresentation.processingProvenance
    val content: ByteArray get() = processingRepresentation.bytes()

    init {
        require(processingRepresentation.byteLength >= 1) { "ExternalTranscriptionRequest.content must not be empty" }
        require(processingRepresentation.byteLength <= MAX_SOURCE_BYTES) { "External transcription source exceeds $MAX_SOURCE_BYTES bytes" }
        require(mediaType == "application/pdf" || mediaType.startsWith("image/", ignoreCase = true)) {
            "ExternalTranscriptionRequest.mediaType must be PDF or image"
        }
        require(maximumPageCount in 1..MAX_PAGE_COUNT) { "ExternalTranscriptionRequest.maximumPageCount must be in 1..$MAX_PAGE_COUNT" }
    }

    companion object {
        const val MAX_SOURCE_BYTES = 64L * 1024L * 1024L
        const val MAX_PAGE_COUNT = 200
    }
}

interface ExternalTranscriptionMechanism {
    suspend fun transcribe(request: ExternalTranscriptionRequest): ExternalTranscriptionMechanismOutcome
}

sealed interface ExternalTranscriptionMechanismOutcome {
    data class Candidate(val candidate: OcrStructuredTranscriptionCandidate) : ExternalTranscriptionMechanismOutcome
    data class Failure(val reason: String) : ExternalTranscriptionMechanismOutcome {
        init { require(reason.isNotBlank() && reason.length <= 4_096) }
    }
}

sealed interface ExternalTranscriptionOwnerInvocationOutcome {
    data class Admitted(
        val evidenceArtifactId: EvidenceArtifactId,
        val record: DerivativeGenerationRecord,
        val extracted: OcrDerivativeExtractedResult,
    ) : ExternalTranscriptionOwnerInvocationOutcome
    data object NotAuthorised : ExternalTranscriptionOwnerInvocationOutcome
    data class SourceNotFound(val evidenceArtifactId: EvidenceArtifactId) : ExternalTranscriptionOwnerInvocationOutcome
    data class SourceRetrievalRejected(val evidenceArtifactId: EvidenceArtifactId) : ExternalTranscriptionOwnerInvocationOutcome
    data class ManifestNotFound(val evidenceArtifactId: EvidenceArtifactId) : ExternalTranscriptionOwnerInvocationOutcome
    data class ManifestRejected(val evidenceArtifactId: EvidenceArtifactId) : ExternalTranscriptionOwnerInvocationOutcome
    data class ByteLengthMismatch(val evidenceArtifactId: EvidenceArtifactId) : ExternalTranscriptionOwnerInvocationOutcome
    data class DigestMismatch(val evidenceArtifactId: EvidenceArtifactId) : ExternalTranscriptionOwnerInvocationOutcome
    data class UnsupportedOrOutOfBounds(val evidenceArtifactId: EvidenceArtifactId) : ExternalTranscriptionOwnerInvocationOutcome
    data class MechanismFailure(val reason: String) : ExternalTranscriptionOwnerInvocationOutcome
    data class ValidationRejected(val reason: String) : ExternalTranscriptionOwnerInvocationOutcome
    data class AdmissionFailed(val reason: String) : ExternalTranscriptionOwnerInvocationOutcome
    data class ReconciliationRequired(
        val evidenceArtifactId: EvidenceArtifactId,
        val record: DerivativeGenerationRecord,
        val extracted: OcrDerivativeExtractedResult,
        val reason: String,
    ) : ExternalTranscriptionOwnerInvocationOutcome
}
