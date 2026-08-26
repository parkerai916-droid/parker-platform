package parker.core.runtime

import java.security.MessageDigest
import java.time.Instant
import parker.core.interfaces.*

data class OcrProcessingRepresentationLimits(
    val maximumPdfBytes: Long,
    val maximumImageBytes: Long,
) {
    init {
        require(maximumPdfBytes > 0 && maximumImageBytes > 0) { "Processing representation limits must be positive" }
    }
}

/** Pure construction boundary for a defensive, byte-exact copy of already verified source bytes. */
class OcrProcessingRepresentationFactory(
    private val limits: OcrProcessingRepresentationLimits = OcrProcessingRepresentationLimits(
        ExternalTranscriptionRequest.MAX_SOURCE_BYTES,
        ExternalTranscriptionRequest.MAX_SOURCE_BYTES,
    ),
    private val now: () -> Instant = Instant::now,
) {
    fun create(
        sourceEvidenceArtifactId: EvidenceArtifactId,
        verifiedSourceBytes: ByteArray,
        authoritativeManifestSha256: OcrSha256Digest,
        authoritativeSourceMediaType: String,
        authoritativeSourceByteLength: Long,
        requestedPageScope: OcrPageScope? = null,
        submittedPageScope: OcrPageScope? = requestedPageScope,
    ): OcrProcessingRepresentationOutcome {
        if (verifiedSourceBytes.isEmpty() || authoritativeSourceByteLength <= 0) return OcrProcessingRepresentationOutcome.InvalidSourceFacts
        if (authoritativeSourceByteLength != verifiedSourceBytes.size.toLong()) return OcrProcessingRepresentationOutcome.SourceLengthMismatch
        val limit = when (authoritativeSourceMediaType) {
            "application/pdf" -> limits.maximumPdfBytes
            "image/jpeg", "image/png", "image/webp" -> limits.maximumImageBytes
            else -> return OcrProcessingRepresentationOutcome.UnsupportedMedia
        }
        if (authoritativeSourceByteLength > minOf(limit, ExternalTranscriptionRequest.MAX_SOURCE_BYTES)) {
            return OcrProcessingRepresentationOutcome.BoundsExceeded
        }
        return try {
            val representationBytes = verifiedSourceBytes.copyOf()
            val digest = sha256(representationBytes)
            if (digest != authoritativeManifestSha256) return OcrProcessingRepresentationOutcome.DigestMismatch
            val provenance = OcrProcessingProvenance(
                sourceEvidenceArtifactId = sourceEvidenceArtifactId,
                sourceManifestSha256 = authoritativeManifestSha256,
                sourceMediaType = authoritativeSourceMediaType,
                sourceByteLength = authoritativeSourceByteLength,
                requestedPageScope = requestedPageScope,
                submittedPageScope = submittedPageScope,
                representationMediaType = authoritativeSourceMediaType,
                representationByteLength = representationBytes.size.toLong(),
                representationSha256 = digest,
                byteExactCopy = true,
                processingProfileIdentity = PROCESSING_PROFILE_IDENTITY,
                createdAt = now(),
                materialTransformation = null,
            )
            OcrProcessingRepresentationOutcome.Created(OcrProcessingRepresentation(representationBytes, provenance))
        } catch (_: Exception) {
            OcrProcessingRepresentationOutcome.ImplementationFailure
        }
    }

    private fun sha256(bytes: ByteArray) = OcrSha256Digest(
        MessageDigest.getInstance("SHA-256").digest(bytes).joinToString("") { "%02x".format(it) },
    )

    companion object {
        const val PROCESSING_PROFILE_IDENTITY = "external-transcription.direct-byte-exact-v1"
    }
}
