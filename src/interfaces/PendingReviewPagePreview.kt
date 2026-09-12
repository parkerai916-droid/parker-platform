package parker.core.interfaces

/** A page preview of verified pre-ingestion custody bytes. This is not evidence. */
data class PendingReviewPageRenderRequest(
    val batchId: String,
    val sourceSha256: String,
    val sourceMediaType: String,
    val sourceBytes: ByteArray,
    val pageNumber: Int,
    val profile: PageRenderProfile,
) {
    init {
        require(batchId.matches(Regex("^bulk-[A-Za-z0-9._:-]+$")))
        require(sourceSha256.matches(Regex("^[0-9a-f]{64}$")))
        require(sourceBytes.isNotEmpty() && pageNumber > 0)
    }
}

data class PendingReviewPagePreviewProvenance(
    val batchId: String,
    val sourceSha256: String,
    val sourceByteLength: Long,
    val sourceMediaType: String,
    val pageNumber: Int,
    val declaredPageCount: Int,
    val rendererIdentity: String,
    val rendererVersion: String,
    val rendererBuildIdentity: String,
    val renderProfile: PageRenderProfile,
    val sourceDimensions: SourcePageDimensions,
    val sourceRotationDegrees: Int,
    val pixelDimensions: PagePixelDimensions,
    val canonicalPixelDigest: CanonicalPixelDigest,
    val encodedRepresentationSha256: String,
) {
    init {
        require(sourceSha256.matches(Regex("^[0-9a-f]{64}$")))
        require(sourceByteLength > 0 && pageNumber in 1..declaredPageCount)
        require(sourceMediaType.isNotBlank())
        require(sourceRotationDegrees in setOf(0, 90, 180, 270))
    }
}

class PendingReviewPagePreview internal constructor(
    val provenance: PendingReviewPagePreviewProvenance,
    encodedBytes: ByteArray,
) {
    private val encoded = encodedBytes.copyOf()
    fun encodedBytes(): ByteArray = encoded.copyOf()
}

sealed interface PendingReviewPagePreviewOutcome {
    data class Created(val preview: PendingReviewPagePreview) : PendingReviewPagePreviewOutcome
    data object UnsupportedMedia : PendingReviewPagePreviewOutcome
    data object CorruptSource : PendingReviewPagePreviewOutcome
    data object SourceDigestMismatch : PendingReviewPagePreviewOutcome
    data object InvalidPageIndex : PendingReviewPagePreviewOutcome
    data object ExtremeDimensions : PendingReviewPagePreviewOutcome
    data object ResourceLimitExceeded : PendingReviewPagePreviewOutcome
    data object ProvenanceMismatch : PendingReviewPagePreviewOutcome
    data object RendererFailure : PendingReviewPagePreviewOutcome
}
