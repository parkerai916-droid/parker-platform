package parker.core.interfaces

import java.time.Instant
import java.util.Collections

/** Canonical, immutable, one-based page numbers. Empty means a known empty scope; unavailable scope is represented by absence at the containing OCR contract. */
class OcrPageScope(pageNumbers: Collection<Int>) {
    val pageNumbers: List<Int>

    init {
        require(pageNumbers.all { it >= 1 }) { "OcrPageScope page numbers must be one-based and positive" }
        require(pageNumbers.size == pageNumbers.toSet().size) { "OcrPageScope must not contain duplicate page numbers" }
        this.pageNumbers = Collections.unmodifiableList(pageNumbers.sorted())
    }

    override fun equals(other: Any?): Boolean = other is OcrPageScope && pageNumbers == other.pageNumbers
    override fun hashCode(): Int = pageNumbers.hashCode()
    override fun toString(): String = "OcrPageScope(pageNumbers=$pageNumbers)"
}

enum class OcrPageOutcomeKind {
    TRANSCRIBED,
    TRANSCRIBED_WITH_QUALIFICATIONS,
    ILLEGIBLE_OR_NO_RECOGNISABLE_CONTENT,
    FAILED,
    NOT_RETURNED,
}

enum class OcrUncertaintyKind {
    UNCERTAIN,
    ILLEGIBLE,
}

/** Character offsets address returned page text only; they assert no spatial location on the source image. */
data class OcrUncertaintySpan(
    val pageNumber: Int,
    val startOffsetInclusive: Int,
    val endOffsetExclusive: Int,
    val kind: OcrUncertaintyKind,
    val disclosure: String,
) {
    init {
        require(pageNumber >= 1) { "OcrUncertaintySpan.pageNumber must be one-based and positive" }
        require(startOffsetInclusive >= 0) { "OcrUncertaintySpan.startOffsetInclusive must not be negative" }
        require(endOffsetExclusive > startOffsetInclusive) { "OcrUncertaintySpan.endOffsetExclusive must be greater than startOffsetInclusive" }
        require(disclosure.isNotBlank() && disclosure.length <= MAX_DISCLOSURE_CHARACTERS) {
            "OcrUncertaintySpan.disclosure must contain 1..$MAX_DISCLOSURE_CHARACTERS characters"
        }
    }
}

/** A bounded machine-readable classification plus optional human-readable detail. */
data class OcrPageOutcomeReason(
    val classification: String,
    val detail: String? = null,
) {
    init {
        require(REASON_CLASSIFICATION.matches(classification)) {
            "OcrPageOutcomeReason.classification must be 1..64 uppercase letters, digits, or underscores, beginning with a letter"
        }
        require(!classification.equals("UNKNOWN", ignoreCase = true)) {
            "OcrPageOutcomeReason.classification must not fabricate UNKNOWN"
        }
        require(detail == null || (detail.isNotBlank() && detail.length <= MAX_DISCLOSURE_CHARACTERS)) {
            "OcrPageOutcomeReason.detail must be absent or contain 1..$MAX_DISCLOSURE_CHARACTERS characters"
        }
    }
}

data class OcrPageOutcome(
    val pageNumber: Int,
    val outcome: OcrPageOutcomeKind,
    val reason: OcrPageOutcomeReason? = null,
    val warnings: List<String> = emptyList(),
    val uncertaintySpans: List<OcrUncertaintySpan> = emptyList(),
) {
    init {
        require(pageNumber >= 1) { "OcrPageOutcome.pageNumber must be one-based and positive" }
        require(warnings.size <= MAX_PAGE_WARNINGS) { "OcrPageOutcome.warnings must contain at most $MAX_PAGE_WARNINGS entries" }
        require(warnings.all { it.isNotBlank() && it.length <= MAX_DISCLOSURE_CHARACTERS }) {
            "OcrPageOutcome warnings must each contain 1..$MAX_DISCLOSURE_CHARACTERS characters"
        }
    }
}

/** Facts only. Reconciliation and completeness derivation belong to a later validation unit. */
data class OcrPageAccounting(
    val requestedScope: OcrPageScope,
    val submittedScope: OcrPageScope,
    val returnedScope: OcrPageScope,
    val pageOutcomes: List<OcrPageOutcome>,
)

@JvmInline
value class OcrSha256Digest(val value: String) {
    init {
        require(SHA256_PATTERN.matches(value)) { "OcrSha256Digest must be exactly 64 lowercase hexadecimal characters" }
    }
}

data class OcrPixelDimensions(val width: Int, val height: Int) {
    init {
        require(width >= 1 && height >= 1) { "OcrPixelDimensions width and height must be positive" }
    }
}

data class OcrCropParameters(val leftPx: Int, val topPx: Int, val widthPx: Int, val heightPx: Int) {
    init {
        require(leftPx >= 0 && topPx >= 0) { "OcrCropParameters origin must not be negative" }
        require(widthPx >= 1 && heightPx >= 1) { "OcrCropParameters dimensions must be positive" }
    }
}

/** Material transformation facts; optional fields are absent only when that operation did not apply. */
data class OcrMaterialTransformation(
    val mechanismIdentity: String,
    val mechanismVersion: String,
    val sourcePageScope: OcrPageScope,
    val dpi: Int? = null,
    val dimensions: OcrPixelDimensions? = null,
    val rotationDegrees: Double? = null,
    val colourMode: String? = null,
    val scaleX: Double? = null,
    val scaleY: Double? = null,
    val crop: OcrCropParameters? = null,
    val compression: String? = null,
) {
    init {
        requireBoundedIdentity(mechanismIdentity, "OcrMaterialTransformation.mechanismIdentity")
        requireBoundedIdentity(mechanismVersion, "OcrMaterialTransformation.mechanismVersion")
        require(dpi == null || dpi >= 1) { "OcrMaterialTransformation.dpi must be positive when present" }
        require(rotationDegrees == null || rotationDegrees.isFinite()) { "OcrMaterialTransformation.rotationDegrees must be finite when present" }
        require((scaleX == null) == (scaleY == null)) { "OcrMaterialTransformation scaleX and scaleY must be both present or both absent" }
        require(scaleX == null || (scaleX > 0.0 && scaleY!! > 0.0)) { "OcrMaterialTransformation scale factors must be positive" }
        colourMode?.let { requireBoundedIdentity(it, "OcrMaterialTransformation.colourMode") }
        compression?.let { requireBoundedIdentity(it, "OcrMaterialTransformation.compression") }
    }
}

data class OcrProcessingProvenance(
    val sourceEvidenceArtifactId: EvidenceArtifactId,
    val sourceManifestSha256: OcrSha256Digest,
    val sourceMediaType: String,
    val sourceByteLength: Long,
    val requestedPageScope: OcrPageScope,
    val submittedPageScope: OcrPageScope,
    val representationMediaType: String,
    val representationByteLength: Long,
    val representationSha256: OcrSha256Digest,
    val byteExactCopy: Boolean,
    val processingProfileIdentity: String,
    val createdAt: Instant,
    val materialTransformation: OcrMaterialTransformation? = null,
) {
    init {
        requireBoundedIdentity(sourceMediaType, "OcrProcessingProvenance.sourceMediaType")
        requireBoundedIdentity(representationMediaType, "OcrProcessingProvenance.representationMediaType")
        require(sourceByteLength >= 1) { "OcrProcessingProvenance.sourceByteLength must be positive" }
        require(representationByteLength >= 1) { "OcrProcessingProvenance.representationByteLength must be positive" }
        requireBoundedIdentity(processingProfileIdentity, "OcrProcessingProvenance.processingProfileIdentity")
        require(byteExactCopy == (materialTransformation == null)) {
            "OcrProcessingProvenance must carry no material transformation for a byte-exact copy, and must carry one for a transformed representation"
        }
    }
}

sealed interface OcrModelSnapshot {
    data class Present(val value: String) : OcrModelSnapshot {
        init {
            requireBoundedIdentity(value, "OcrModelSnapshot.Present.value")
            require(!value.equals("unknown", ignoreCase = true)) { "OcrModelSnapshot must never fabricate 'unknown'" }
        }
    }

    data object NotExposed : OcrModelSnapshot
}

/** Provider-neutral provenance for a completed provider request; carries no endpoint or authentication material. */
data class OcrProviderProvenance(
    val providerIdentity: String,
    val adapterIdentity: String,
    val adapterVersion: String,
    val transcriptionConfigurationProfile: String,
    val providerReportedModelIdentifier: String,
    val modelSnapshot: OcrModelSnapshot,
    val providerCorrelationIdentifier: String,
) {
    init {
        requireBoundedIdentity(providerIdentity, "OcrProviderProvenance.providerIdentity")
        requireBoundedIdentity(adapterIdentity, "OcrProviderProvenance.adapterIdentity")
        requireBoundedIdentity(adapterVersion, "OcrProviderProvenance.adapterVersion")
        requireBoundedIdentity(transcriptionConfigurationProfile, "OcrProviderProvenance.transcriptionConfigurationProfile")
        requireBoundedIdentity(providerReportedModelIdentifier, "OcrProviderProvenance.providerReportedModelIdentifier")
        require(!providerReportedModelIdentifier.equals("unknown", ignoreCase = true)) {
            "OcrProviderProvenance.providerReportedModelIdentifier must never fabricate 'unknown'"
        }
        requireBoundedIdentity(providerCorrelationIdentifier, "OcrProviderProvenance.providerCorrelationIdentifier")
        require(!providerCorrelationIdentifier.equals("unknown", ignoreCase = true)) {
            "OcrProviderProvenance.providerCorrelationIdentifier must never fabricate 'unknown'"
        }
    }
}

private const val MAX_DISCLOSURE_CHARACTERS = 4_096
private const val MAX_PAGE_WARNINGS = 200
private const val MAX_IDENTITY_CHARACTERS = 1_024
private val SHA256_PATTERN = Regex("^[0-9a-f]{64}$")
private val REASON_CLASSIFICATION = Regex("^[A-Z][A-Z0-9_]{0,63}$")

private fun requireBoundedIdentity(value: String, field: String) {
    require(value.isNotBlank() && value.length <= MAX_IDENTITY_CHARACTERS) {
        "$field must contain 1..$MAX_IDENTITY_CHARACTERS characters"
    }
}
