package parker.core.interfaces

import java.time.Instant

/** The explicit Parker canonical-projection media/profile identity for a governed EML derivative -- never message/rfc822 (the original), never text/plain (an unrelated, unaffiliated type), never an OCR processing-profile identity. */
const val EML_DERIVED_TEXT_REPRESENTATION_MEDIA_TYPE = "application/vnd.parker.eml-derived-text"
const val EML_CANONICAL_PROJECTION_PROFILE_IDENTITY = "parker.eml-canonical-text-projection-v1"

/** One non-attachment, body-shaped MIME entity's decoding outcome, recorded for every part considered for inclusion. */
data class EmlPartDecodingRecord(
    val mimeEntityId: String,
    val declaredCharset: String?,
    val declaredTransferEncoding: String?,
    val decodeSucceeded: Boolean,
)

/** An explicit, disclosed exclusion. For a successfully created representation this list is always empty -- see [EmlDerivedRepresentationProvenance]'s own invariant. */
data class EmlExclusionRecord(
    val mimeEntityId: String,
    val reason: String,
)

/**
 * Two-stage governed provenance for a canonical EML text projection (FIDELITY_PRESERVING_EVIDENCE_ACQUISITION_SCOPE_LOCK.md §6.1):
 * stage 1 is MIME parsing/transfer-decoding/charset-decoding (Apache James Mime4j, via [parker.core.runtime.ApacheJamesMime4jExtractor]);
 * stage 2 is the deterministic canonical text projection built from that structural result.
 * [createdAt] is runtime metadata only -- it never participates in the structural-result digest,
 * the representation digest, or any deterministic content byte.
 */
data class EmlDerivedRepresentationProvenance(
    val sourceEvidenceArtifactId: EvidenceArtifactId,
    val sourceMediaType: String,
    val sourceSha256: OcrSha256Digest,
    val sourceByteLength: Long,

    val structuralParserIdentity: DerivativeProducerIdentity,
    val structuralTransformationHistory: List<DerivativeTransformation>,
    val structuralCompletenessState: DerivativeCompletenessState,
    val structuralResultSha256: OcrSha256Digest,

    val representationGenerationProfileIdentity: String,
    val representationMediaType: String,
    val representationByteLength: Long,
    val representationSha256: OcrSha256Digest,

    val includedMimeEntityCount: Int,
    val mimeEntityOrder: List<String>,
    val perPartDecoding: List<EmlPartDecodingRecord>,
    val bodyAlternativesIncluded: List<String>,
    val excludedParts: List<EmlExclusionRecord>,
    val nestedMessageEntityIds: List<String>,
    val attachmentManifestEntryCount: Int,
    val attachmentManifestSha256: OcrSha256Digest,

    val createdAt: Instant,
) {
    init {
        requireBoundedEmlIdentity(sourceMediaType, "EmlDerivedRepresentationProvenance.sourceMediaType")
        requireBoundedEmlIdentity(representationMediaType, "EmlDerivedRepresentationProvenance.representationMediaType")
        requireBoundedEmlIdentity(representationGenerationProfileIdentity, "EmlDerivedRepresentationProvenance.representationGenerationProfileIdentity")
        require(sourceByteLength >= 1) { "EmlDerivedRepresentationProvenance.sourceByteLength must be positive" }
        require(representationByteLength >= 1) { "EmlDerivedRepresentationProvenance.representationByteLength must be positive" }
        require(includedMimeEntityCount >= 1) { "EmlDerivedRepresentationProvenance.includedMimeEntityCount must be positive" }
        require(attachmentManifestEntryCount >= 0) { "EmlDerivedRepresentationProvenance.attachmentManifestEntryCount must not be negative" }
        require(excludedParts.isEmpty()) {
            "EmlDerivedRepresentationProvenance describes a successfully created representation -- exclusions for " +
                "body-bearing content must be empty; a governed EML derivative is never partial"
        }
    }
}

private fun requireBoundedEmlIdentity(value: String, field: String) {
    require(value.isNotBlank() && value.length <= 1_024) { "$field must contain 1..1024 characters" }
}

/**
 * A deterministic, non-byte-exact canonical text projection of an EML's headers, MIME structure,
 * body alternatives, and attachment manifest. Never the authoritative source -- the original
 * `message/rfc822` EvidenceArtifact remains sole authoritative source regardless
 * (FIDELITY_PRESERVING_EVIDENCE_ACQUISITION_SCOPE_LOCK.md §6.1).
 */
class EmlDerivedRepresentation internal constructor(
    bytes: ByteArray,
    val provenance: EmlDerivedRepresentationProvenance,
) : ExternallySubmittedRepresentation {
    private val canonicalBytes = bytes.copyOf()

    /** Returns a fresh copy so callers never acquire mutable ownership of the canonical bytes. */
    fun bytes(): ByteArray = canonicalBytes.copyOf()

    val byteLength: Long get() = canonicalBytes.size.toLong()

    override val sourceEvidenceArtifactId get() = provenance.sourceEvidenceArtifactId
    override val sourceMediaType get() = provenance.sourceMediaType
    override val sourceSha256 get() = provenance.sourceSha256
    override val sourceByteLength get() = provenance.sourceByteLength
    override val representationMediaType get() = provenance.representationMediaType
    override val representationSha256 get() = provenance.representationSha256
    override val representationByteLength get() = provenance.representationByteLength
    override val byteExactCopy: Boolean = false
    override val representationClass: AcquisitionRepresentationClass = AcquisitionRepresentationClass.DIRECTLY_DERIVED_TRANSFORMED_REPRESENTATION
    override val transformationProfileIdentity get() = provenance.representationGenerationProfileIdentity
    override fun content(): ByteArray = bytes()
}

sealed interface EmlDerivedRepresentationOutcome {
    data class Created(val representation: EmlDerivedRepresentation) : EmlDerivedRepresentationOutcome
    /** The authoritative source's own media type is not message/rfc822, or the structural result's own declared media type disagrees. */
    data object UnsupportedMedia : EmlDerivedRepresentationOutcome
    /** The structural result itself is internally inconsistent (for example, no MIME entities at all) -- defensive; a well-formed extractor output never produces this. */
    data object MalformedStructuralResult : EmlDerivedRepresentationOutcome
    /** No body-shaped MIME entity exists at all (for example, an attachments-only message). */
    data object NoBodyContent : EmlDerivedRepresentationOutcome
    /** At least one body-bearing MIME part could not be deterministically transfer-decoded or charset-decoded. No partial representation is ever created. */
    data class IncompleteBodyContent(val excludedEntityIds: List<String>) : EmlDerivedRepresentationOutcome
    /** The canonical projection's own byte length exceeds the governed external submission ceiling. */
    data object BoundsExceeded : EmlDerivedRepresentationOutcome
    data object ImplementationFailure : EmlDerivedRepresentationOutcome
}
