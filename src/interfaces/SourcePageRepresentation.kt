package parker.core.interfaces

import java.security.MessageDigest

@JvmInline value class PageRepresentationId(val value: String) { init { require(value.matches(Regex("^[0-9a-f]{64}$"))) } }
@JvmInline value class CanonicalPixelDigest(val value: String) { init { require(value.matches(Regex("^[0-9a-f]{64}$"))) } }

data class PagePixelDimensions(val width: Int, val height: Int) {
    init { require(width > 0 && height > 0) }
    val pixelCount: Long get() = width.toLong() * height.toLong()
}

data class SourcePageDimensions(val width: String, val height: String, val unit: String) {
    init {
        require(width.matches(Regex("^[0-9]+(\\.[0-9]+)?$")) && height.matches(Regex("^[0-9]+(\\.[0-9]+)?$")))
        require(width.toBigDecimal() > java.math.BigDecimal.ZERO && height.toBigDecimal() > java.math.BigDecimal.ZERO)
        require(unit in setOf("PDF_POINT", "SOURCE_PIXEL"))
    }
}

enum class PagePixelFormat { SRGB_8_RGB_OPAQUE }
enum class PageOrientationPolicy { VISUALLY_PRESENTED }
enum class PageTransparencyPolicy { COMPOSITE_SRGB_WHITE }

data class PageRenderProfile(
    val profileId: String, val profileVersion: Int, val dpi: Int?,
    val encodedMediaType: String = "image/png",
    val pixelFormat: PagePixelFormat = PagePixelFormat.SRGB_8_RGB_OPAQUE,
    val orientationPolicy: PageOrientationPolicy = PageOrientationPolicy.VISUALLY_PRESENTED,
    val transparencyPolicy: PageTransparencyPolicy = PageTransparencyPolicy.COMPOSITE_SRGB_WHITE,
) {
    init { require(profileId.isNotBlank() && profileVersion > 0); require(dpi == null || dpi in 72..600); require(encodedMediaType == "image/png") }
}

data class PageRepresentationProvenance(
    val sourceEvidenceArtifactId: EvidenceArtifactId, val sourceSha256: String, val sourceByteLength: Long,
    val sourceMediaType: String, val pageNumber: Int, val declaredPageCount: Int,
    val rendererIdentity: String, val rendererVersion: String, val rendererBuildIdentity: String,
    val renderProfile: PageRenderProfile, val sourceDimensions: SourcePageDimensions,
    val sourceRotationDegrees: Int, val pixelDimensions: PagePixelDimensions,
    val canonicalPixelDigest: CanonicalPixelDigest, val encodedRepresentationSha256: String,
) {
    init {
        require(sourceSha256.matches(Regex("^[0-9a-f]{64}$")) && encodedRepresentationSha256.matches(Regex("^[0-9a-f]{64}$")))
        require(sourceByteLength > 0 && pageNumber in 1..declaredPageCount && sourceMediaType.isNotBlank())
        require(rendererIdentity.isNotBlank() && rendererVersion.isNotBlank() && rendererBuildIdentity.isNotBlank())
        require(sourceRotationDegrees in setOf(0, 90, 180, 270))
    }
}

class AuthoritativePageRepresentation internal constructor(
    val id: PageRepresentationId, val provenance: PageRepresentationProvenance,
    encodedBytes: ByteArray, canonicalPixels: ByteArray,
) {
    private val encoded = encodedBytes.copyOf(); private val pixels = canonicalPixels.copyOf()
    fun encodedBytes(): ByteArray = encoded.copyOf()
    internal fun canonicalPixels(): ByteArray = pixels.copyOf()
}

data class PixelCropBounds(val left: Int, val top: Int, val rightExclusive: Int, val bottomExclusive: Int) {
    init { require(left >= 0 && top >= 0 && rightExclusive > left && bottomExclusive > top) }
}
data class DeterministicPageCrop(
    val sourceRepresentationId: PageRepresentationId, val bounds: PixelCropBounds,
    val dimensions: PagePixelDimensions, val canonicalPixelDigest: CanonicalPixelDigest,
    private val pixels: ByteArray,
) { fun canonicalPixels(): ByteArray = pixels.copyOf() }

data class SourcePageRenderRequest(
    val evidenceArtifactId: EvidenceArtifactId, val sourceSha256: String, val sourceMediaType: String,
    val sourceBytes: ByteArray, val pageNumber: Int, val profile: PageRenderProfile,
) {
    init { require(sourceSha256.matches(Regex("^[0-9a-f]{64}$"))); require(sourceBytes.isNotEmpty() && pageNumber > 0) }
}

sealed interface SourcePageRepresentationOutcome {
    data class Created(val representation: AuthoritativePageRepresentation) : SourcePageRepresentationOutcome
    data object UnsupportedMedia : SourcePageRepresentationOutcome
    data object CorruptSource : SourcePageRepresentationOutcome
    data object SourceDigestMismatch : SourcePageRepresentationOutcome
    data object InvalidPageIndex : SourcePageRepresentationOutcome
    data object ExtremeDimensions : SourcePageRepresentationOutcome
    data object ResourceLimitExceeded : SourcePageRepresentationOutcome
    data object NonDeterministicPixels : SourcePageRepresentationOutcome
    data object ProvenanceMismatch : SourcePageRepresentationOutcome
    data object DigestMismatch : SourcePageRepresentationOutcome
    data object RendererFailure : SourcePageRepresentationOutcome
}

interface SourcePageRenderer { fun render(request: SourcePageRenderRequest): SourcePageRepresentationOutcome }

object CanonicalPagePixelDigests {
    private val DOMAIN = "parker.source-page.canonical-pixels.v1\u0000".toByteArray(Charsets.UTF_8)
    fun digest(dimensions: PagePixelDimensions, format: PagePixelFormat, rowMajorPixels: ByteArray): CanonicalPixelDigest {
        require(rowMajorPixels.size.toLong() == dimensions.pixelCount * 3L)
        val md = MessageDigest.getInstance("SHA-256"); md.update(DOMAIN)
        md.update(intBytes(dimensions.width)); md.update(intBytes(dimensions.height)); md.update(format.name.toByteArray()); md.update(0); md.update(rowMajorPixels)
        return CanonicalPixelDigest(md.digest().toHex())
    }
    fun sha256(bytes: ByteArray): String = MessageDigest.getInstance("SHA-256").digest(bytes).toHex()
    private fun intBytes(v: Int) = byteArrayOf((v ushr 24).toByte(), (v ushr 16).toByte(), (v ushr 8).toByte(), v.toByte())
    private fun ByteArray.toHex() = joinToString("") { "%02x".format(it) }
}
