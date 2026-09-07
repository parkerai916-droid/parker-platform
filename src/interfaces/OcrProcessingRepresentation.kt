package parker.core.interfaces

/** Request-scoped, non-authoritative material derived from one manifest-verified source. */
class OcrProcessingRepresentation internal constructor(
    bytes: ByteArray,
    val processingProvenance: OcrProcessingProvenance,
) : ExternallySubmittedRepresentation {
    private val canonicalBytes = bytes.copyOf()

    /** Returns a fresh copy so callers never acquire mutable ownership of the canonical bytes. */
    fun bytes(): ByteArray = canonicalBytes.copyOf()

    val byteLength: Long get() = canonicalBytes.size.toLong()

    override val sourceEvidenceArtifactId get() = processingProvenance.sourceEvidenceArtifactId
    override val sourceMediaType get() = processingProvenance.sourceMediaType
    override val sourceSha256 get() = processingProvenance.sourceManifestSha256
    override val sourceByteLength get() = processingProvenance.sourceByteLength
    override val representationMediaType get() = processingProvenance.representationMediaType
    override val representationSha256 get() = processingProvenance.representationSha256
    override val representationByteLength get() = processingProvenance.representationByteLength
    override val byteExactCopy get() = processingProvenance.byteExactCopy
    override val representationClass: AcquisitionRepresentationClass
        get() = if (byteExactCopy) AcquisitionRepresentationClass.AUTHORITATIVE_SOURCE_OR_BYTE_EXACT_COPY
        else AcquisitionRepresentationClass.DIRECTLY_DERIVED_TRANSFORMED_REPRESENTATION
    override val transformationProfileIdentity get() = processingProvenance.processingProfileIdentity
    override fun content(): ByteArray = bytes()
}

sealed interface OcrProcessingRepresentationOutcome {
    data class Created(val representation: OcrProcessingRepresentation) : OcrProcessingRepresentationOutcome
    data object UnsupportedMedia : OcrProcessingRepresentationOutcome
    data object InvalidSourceFacts : OcrProcessingRepresentationOutcome
    data object BoundsExceeded : OcrProcessingRepresentationOutcome
    data object SourceLengthMismatch : OcrProcessingRepresentationOutcome
    data object DigestMismatch : OcrProcessingRepresentationOutcome
    data object InvalidTextEncoding : OcrProcessingRepresentationOutcome
    data object ImplementationFailure : OcrProcessingRepresentationOutcome
}
