package parker.core.runtime

import java.nio.ByteBuffer
import java.nio.charset.CharacterCodingException
import java.nio.charset.CodingErrorAction
import java.nio.charset.StandardCharsets
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
    internal fun create(
        authoritativeSource: AuthoritativeAcquisitionInput,
        requestedPageScope: OcrPageScope? = null,
        submittedPageScope: OcrPageScope? = requestedPageScope,
    ): OcrProcessingRepresentationOutcome {
        val authoritativeSourceMediaType = authoritativeSource.mediaType
            ?: return OcrProcessingRepresentationOutcome.UnsupportedMedia
        val authoritativeSourceByteLength = authoritativeSource.byteLength
        val authoritativeManifestSha256 = OcrSha256Digest(authoritativeSource.sha256)
        val verifiedSourceBytes = authoritativeSource.bytes()
        if (verifiedSourceBytes.isEmpty() || authoritativeSourceByteLength <= 0) return OcrProcessingRepresentationOutcome.InvalidSourceFacts
        if (authoritativeSourceByteLength != verifiedSourceBytes.size.toLong()) return OcrProcessingRepresentationOutcome.SourceLengthMismatch
        val limit = when (authoritativeSourceMediaType) {
            "application/pdf" -> limits.maximumPdfBytes
            // No distinct text bound exists yet; reusing the image bound is deliberate and
            // narrow -- both currently resolve to the same ExternalTranscriptionRequest.MAX_SOURCE_BYTES
            // value in production, and introducing a dedicated text limit is out of scope here.
            "image/jpeg", "image/png", "image/webp", "text/csv" -> limits.maximumImageBytes
            else -> return OcrProcessingRepresentationOutcome.UnsupportedMedia
        }
        if (authoritativeSourceByteLength > minOf(limit, ExternalTranscriptionRequest.MAX_SOURCE_BYTES)) {
            return OcrProcessingRepresentationOutcome.BoundsExceeded
        }
        return try {
            val representationBytes = verifiedSourceBytes.copyOf()
            val digest = sha256(representationBytes)
            if (digest != authoritativeManifestSha256) return OcrProcessingRepresentationOutcome.DigestMismatch
            if (authoritativeSourceMediaType == "text/csv" && !isStrictlyValidUtf8(representationBytes)) {
                return OcrProcessingRepresentationOutcome.InvalidTextEncoding
            }
            val provenance = OcrProcessingProvenance(
                sourceEvidenceArtifactId = authoritativeSource.evidenceArtifactId,
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

    /**
     * Deterministic, strict UTF-8 validation: RFC 3629 well-formedness only. Rejects malformed
     * sequences, incomplete trailing sequences, invalid continuation bytes, overlong encodings, and
     * surrogate code-point encodings -- whatever the strict decoder itself rejects. No replacement
     * decoding, no alternate-charset guessing, no normalisation: this only decides accept/reject.
     * A leading UTF-8 BOM (EF BB BF) is a valid three-byte UTF-8 sequence and is neither stripped
     * nor special-cased here, matching this codebase's existing authoritative CSV BOM behaviour
     * (ApacheCommonsCsvExtractorTest.kt: "UTF-8 BOM is preserved as literal header content").
     */
    private fun isStrictlyValidUtf8(bytes: ByteArray): Boolean {
        val decoder = StandardCharsets.UTF_8.newDecoder()
            .onMalformedInput(CodingErrorAction.REPORT)
            .onUnmappableCharacter(CodingErrorAction.REPORT)
        return try {
            decoder.decode(ByteBuffer.wrap(bytes))
            true
        } catch (_: CharacterCodingException) {
            false
        }
    }

    companion object {
        const val PROCESSING_PROFILE_IDENTITY = "external-transcription.direct-byte-exact-v1"
    }
}
