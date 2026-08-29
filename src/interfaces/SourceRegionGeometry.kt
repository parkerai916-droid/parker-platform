package parker.core.interfaces

@JvmInline value class SourceRegionId(val value: String) { init { require(value.matches(Regex("^[0-9a-f]{64}$"))) } }

enum class SourceRegionStructuralClass { TEXT_LIKE, IMAGE_LIKE, TABLE_LIKE, RULE_OR_SEPARATOR, MIXED, UNKNOWN }
enum class SourceRegionAmbiguityState { UNAMBIGUOUS, HUMAN_ORDER_REQUIRED, NOT_YET_SUPPORTED }
enum class SourceRegionOrderRelation { BEFORE, CONTAINS, COLUMN_PEER }

data class SourceRegionDerivationProfile(
    val profileId: String = "pixel-whitespace-source-regions-v1",
    val version: Int = 1,
    val darkChannelThreshold: Int = 245,
    val minimumDarkPixelsPerRow: Int = 2,
    val maximumIntraBandBlankRows: Int = 2,
    val horizontalSplitGapPixels: Int = 80,
    val maximumInterlineGapPixels: Int = 42,
    val alignmentTolerancePixels: Int = 80,
    val paddingPixels: Int = 8,
    val minimumRegionWidthPixels: Int = 4,
    val minimumRegionHeightPixels: Int = 4,
    val maximumRegionsPerPage: Int = 1_000,
) {
    init {
        require(profileId.isNotBlank() && version > 0 && darkChannelThreshold in 1..254)
        require(minimumDarkPixelsPerRow > 0 && maximumIntraBandBlankRows >= 0 && horizontalSplitGapPixels > 0)
        require(maximumInterlineGapPixels >= 0 && alignmentTolerancePixels >= 0 && paddingPixels >= 0)
        require(minimumRegionWidthPixels > 0 && minimumRegionHeightPixels > 0 && maximumRegionsPerPage > 0)
    }
}

data class SourceRegionProvenance(
    val sourceEvidenceArtifactId: EvidenceArtifactId,
    val sourceSha256: String,
    val pageRepresentationId: PageRepresentationId,
    val pageNumber: Int,
    val pagePixelDimensions: PagePixelDimensions,
    val pageCanonicalPixelDigest: CanonicalPixelDigest,
    val derivationProfileId: String,
    val derivationProfileVersion: Int,
) {
    init {
        require(sourceSha256.matches(Regex("^[0-9a-f]{64}$")) && pageNumber > 0)
        require(derivationProfileId.isNotBlank() && derivationProfileVersion > 0)
    }
}

data class SourceRegion(
    val id: SourceRegionId,
    val bounds: PixelCropBounds,
    val structuralClass: SourceRegionStructuralClass,
    val cropDigest: CanonicalPixelDigest,
    val provenance: SourceRegionProvenance,
)

data class SourceRegionOrderEdge(
    val from: SourceRegionId,
    val to: SourceRegionId,
    val relation: SourceRegionOrderRelation,
) { init { require(from != to) } }

data class SourceRegionOrderGraph(
    val pageRepresentationId: PageRepresentationId,
    val regions: List<SourceRegion>,
    val edges: Set<SourceRegionOrderEdge>,
    val ambiguityState: SourceRegionAmbiguityState,
    val reason: String? = null,
) {
    init {
        require(regions.map { it.id }.distinct().size == regions.size)
        val ids = regions.map { it.id }.toSet()
        require(edges.all { it.from in ids && it.to in ids })
        require((ambiguityState == SourceRegionAmbiguityState.UNAMBIGUOUS) == (reason == null))
    }
}

sealed interface SourceRegionDerivationOutcome {
    data class Derived(val graph: SourceRegionOrderGraph) : SourceRegionDerivationOutcome
    data object InvalidPageRepresentation : SourceRegionDerivationOutcome
    data object PageDigestMismatch : SourceRegionDerivationOutcome
    data object InvalidGeometry : SourceRegionDerivationOutcome
    data object CropDigestMismatch : SourceRegionDerivationOutcome
    data object RegionIdentityCollision : SourceRegionDerivationOutcome
    data object ExcessiveRegions : SourceRegionDerivationOutcome
    data object DerivationNonDeterministic : SourceRegionDerivationOutcome
    data object UnsupportedLayout : SourceRegionDerivationOutcome
}

interface SourceRegionDeriver {
    fun derive(page: AuthoritativePageRepresentation): SourceRegionDerivationOutcome
}
