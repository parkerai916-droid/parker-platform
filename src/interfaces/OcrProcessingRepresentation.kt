package parker.core.interfaces

/** Request-scoped, non-authoritative material derived from one manifest-verified source. */
class OcrProcessingRepresentation internal constructor(
    bytes: ByteArray,
    val processingProvenance: OcrProcessingProvenance,
) {
    private val canonicalBytes = bytes.copyOf()

    /** Returns a fresh copy so callers never acquire mutable ownership of the canonical bytes. */
    fun bytes(): ByteArray = canonicalBytes.copyOf()

    val byteLength: Long get() = canonicalBytes.size.toLong()
}

sealed interface OcrProcessingRepresentationOutcome {
    data class Created(val representation: OcrProcessingRepresentation) : OcrProcessingRepresentationOutcome
    data object UnsupportedMedia : OcrProcessingRepresentationOutcome
    data object InvalidSourceFacts : OcrProcessingRepresentationOutcome
    data object BoundsExceeded : OcrProcessingRepresentationOutcome
    data object SourceLengthMismatch : OcrProcessingRepresentationOutcome
    data object DigestMismatch : OcrProcessingRepresentationOutcome
    data object ImplementationFailure : OcrProcessingRepresentationOutcome
}
