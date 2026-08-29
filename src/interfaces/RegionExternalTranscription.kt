package parker.core.interfaces

import java.security.MessageDigest

const val REGION_TRANSCRIPTION_PROFILE_ID = "region-anchored-fidelity-acquisition-v1"
const val REGION_TRANSCRIPTION_SCHEMA_ID = "region-anchored-transcription-schema-v1"
const val REGION_TRANSCRIPTION_WIRE_VERSION = 4
const val REGION_TRANSCRIPTION_PROCESSING_PROFILE = "external-transcription.deterministic-source-region-raster-v1"

enum class RegionTranscriptionStatus {
    TRANSCRIBED, PARTIALLY_TRANSCRIBED, ILLEGIBLE, NO_VISIBLE_TEXT, UNSUPPORTED_VISUAL_CONTENT,
}

enum class RegionTranscriptionUncertaintyCategory {
    ILLEGIBLE, AMBIGUOUS_CHARACTER, AMBIGUOUS_WORD, PARTIALLY_OCCLUDED, LOW_CONTRAST,
    HANDWRITING_UNCERTAIN, CLIPPED, OTHER_VISUAL_UNCERTAINTY,
}

enum class RegionVisualObservationKind {
    LINE_BREAK, PARAGRAPH_BREAK, LIST_MARKER, TABLE_CELL_TEXT, BOLD, ITALIC, UNDERLINE, ALL_CAPS, ENLARGED_TEXT,
}

data class RegionTranscriptionUncertainty(
    val startCodePoint: Int,
    val endCodePointExclusive: Int,
    val exactSubstring: String,
    val category: RegionTranscriptionUncertaintyCategory,
    val alternatives: List<String> = emptyList(),
    val providerConfidence: String? = null,
)

data class RegionVisualObservation(
    val kind: RegionVisualObservationKind,
    val startCodePoint: Int? = null,
    val endCodePointExclusive: Int? = null,
)

class RegionTranscriptionImage(
    val pageRepresentationId: PageRepresentationId,
    val bounds: PixelCropBounds,
    val cropDigest: CanonicalPixelDigest,
    val encodedMediaType: String,
    val encodedSha256: String,
    encodedBytes: ByteArray,
) {
    private val bytes = encodedBytes.copyOf()
    init {
        require(encodedMediaType == "image/png")
        require(bytes.isNotEmpty() && bytes.size <= MAX_IMAGE_BYTES)
        require(encodedSha256.matches(Regex("^[0-9a-f]{64}$")))
        require(sha256(bytes) == encodedSha256) { "region image encoded digest mismatch" }
    }
    fun encodedBytes(): ByteArray = bytes.copyOf()
    companion object {
        const val MAX_IMAGE_BYTES = 32 * 1024 * 1024
        fun sha256(bytes: ByteArray): String = MessageDigest.getInstance("SHA-256").digest(bytes).joinToString("") { "%02x".format(it) }
    }
}

data class RegionTranscriptionTarget(
    val sourceEvidenceArtifactId: EvidenceArtifactId,
    val sourceSha256: String,
    val pageRepresentationId: PageRepresentationId,
    val pageNumber: Int,
    val pageDimensions: PagePixelDimensions,
    val sourceRegionId: SourceRegionId,
    val bounds: PixelCropBounds,
    val cropDigest: CanonicalPixelDigest,
    val structuralClass: SourceRegionStructuralClass,
    val derivationProfileId: String,
    val derivationProfileVersion: Int,
    val regionImage: RegionTranscriptionImage,
    val pageContextImage: RegionTranscriptionImage? = null,
) {
    init {
        require(sourceSha256.matches(Regex("^[0-9a-f]{64}$")) && pageNumber > 0)
        require(bounds.rightExclusive <= pageDimensions.width && bounds.bottomExclusive <= pageDimensions.height)
        require(derivationProfileId.isNotBlank() && derivationProfileId.length <= 128 && derivationProfileVersion > 0)
        require(regionImage.pageRepresentationId == pageRepresentationId && regionImage.bounds == bounds && regionImage.cropDigest == cropDigest)
        pageContextImage?.let {
            require(it.pageRepresentationId == pageRepresentationId)
            require(it.bounds == PixelCropBounds(0, 0, pageDimensions.width, pageDimensions.height))
        }
    }
}

data class RegionTranscriptionRequest(
    val correlationId: String,
    val transcriptionProfileId: String,
    val schemaId: String,
    val schemaVersion: Int,
    val schemaSha256: String,
    val processingProfile: String,
    val literalInstruction: String,
    val targets: List<RegionTranscriptionTarget>,
) {
    init {
        require(correlationId.matches(Regex("^[A-Za-z0-9_-]{1,120}$")))
        require(transcriptionProfileId == REGION_TRANSCRIPTION_PROFILE_ID)
        require(schemaId == REGION_TRANSCRIPTION_SCHEMA_ID && schemaVersion == REGION_TRANSCRIPTION_WIRE_VERSION)
        require(schemaSha256.matches(Regex("^[0-9a-f]{64}$")))
        require(processingProfile == REGION_TRANSCRIPTION_PROCESSING_PROFILE)
        require(literalInstruction.isNotBlank() && literalInstruction.length <= 4_096)
        require(targets.size in 1..MAX_REGIONS_PER_REQUEST)
        require(targets.map { it.sourceRegionId }.distinct().size == targets.size)
    }
    companion object { const val MAX_REGIONS_PER_REQUEST = 32 }
}

data class RegionTranscriptionProviderProvenance(
    val provider: String,
    val requestedModel: String,
    val providerReportedModel: String?,
    val providerResponseId: String?,
    val adapterId: String,
    val adapterVersion: String,
    val parserId: String,
    val parserVersion: String,
)

data class RegionTranscriptionBlock(
    val sourceRegionId: SourceRegionId,
    val pageNumber: Int,
    val literalText: String?,
    val status: RegionTranscriptionStatus,
    val uncertainties: List<RegionTranscriptionUncertainty>,
    val warnings: List<String>,
    val providerReturnedOrdinal: Int,
    val visualObservations: List<RegionVisualObservation>,
)

data class RegionTranscriptionResult(
    val correlationId: String,
    val transcriptionProfileId: String,
    val schemaId: String,
    val schemaVersion: Int,
    val providerProvenance: RegionTranscriptionProviderProvenance,
    val blocksInProviderOrder: List<RegionTranscriptionBlock>,
)

interface RegionExternalTranscriptionMechanism {
    suspend fun transcribe(request: RegionTranscriptionRequest): RegionExternalTranscriptionOutcome
}

sealed interface RegionExternalTranscriptionOutcome {
    data class Candidate(val exactStructuredResponse: Map<String, Any?>) : RegionExternalTranscriptionOutcome
    data class Failure(val code: String) : RegionExternalTranscriptionOutcome
}
