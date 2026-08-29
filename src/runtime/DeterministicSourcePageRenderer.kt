package parker.core.runtime

import java.awt.Color
import java.awt.Graphics2D
import java.awt.RenderingHints
import java.awt.image.BufferedImage
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.math.BigDecimal
import java.security.MessageDigest
import javax.imageio.ImageIO
import org.apache.pdfbox.Loader
import org.apache.pdfbox.rendering.ImageType
import org.apache.pdfbox.rendering.PDFRenderer
import parker.core.interfaces.*

data class SourcePageRendererLimits(
    val maximumSourceBytes: Long = 64L * 1024L * 1024L, val maximumPages: Int = 200,
    val maximumDimensionPixels: Int = 20_000, val maximumDecodedPixels: Long = 50_000_000,
) { init { require(maximumSourceBytes > 0 && maximumPages > 0 && maximumDimensionPixels > 0 && maximumDecodedPixels > 0) } }

/** Direct custody-byte rendering only. No text extraction, OCR, segmentation, or semantic ordering. */
class DeterministicSourcePageRenderer(private val limits: SourcePageRendererLimits = SourcePageRendererLimits()) : SourcePageRenderer {
    override fun render(request: SourcePageRenderRequest): SourcePageRepresentationOutcome {
        val safeRequest = request.copy(sourceBytes = request.sourceBytes.copyOf())
        if (safeRequest.sourceBytes.size.toLong() > limits.maximumSourceBytes) return SourcePageRepresentationOutcome.ResourceLimitExceeded
        if (CanonicalPagePixelDigests.sha256(safeRequest.sourceBytes) != safeRequest.sourceSha256) return SourcePageRepresentationOutcome.SourceDigestMismatch
        return try {
            when (safeRequest.sourceMediaType) {
                "application/pdf" -> if (safeRequest.profile.dpi == null) SourcePageRepresentationOutcome.ProvenanceMismatch else renderPdf(safeRequest)
                "image/png" -> if (safeRequest.profile.dpi != null) SourcePageRepresentationOutcome.ProvenanceMismatch else renderPng(safeRequest)
                else -> SourcePageRepresentationOutcome.UnsupportedMedia
            }
        } catch (_: java.io.IOException) { SourcePageRepresentationOutcome.CorruptSource }
        catch (_: Exception) { SourcePageRepresentationOutcome.RendererFailure }
    }

    private fun renderPdf(request: SourcePageRenderRequest): SourcePageRepresentationOutcome = Loader.loadPDF(request.sourceBytes).use { doc ->
        if (doc.numberOfPages !in 1..limits.maximumPages) return SourcePageRepresentationOutcome.ResourceLimitExceeded
        if (request.pageNumber !in 1..doc.numberOfPages) return SourcePageRepresentationOutcome.InvalidPageIndex
        val page = doc.getPage(request.pageNumber - 1); val box = page.cropBox; val rotation = ((page.rotation % 360) + 360) % 360
        if (rotation !in setOf(0, 90, 180, 270)) return SourcePageRepresentationOutcome.RendererFailure
        val rotated = rotation == 90 || rotation == 270
        val dpi = requireNotNull(request.profile.dpi)
        val width = Math.ceil((if (rotated) box.height else box.width) * dpi / 72.0).toLong()
        val height = Math.ceil((if (rotated) box.width else box.height) * dpi / 72.0).toLong()
        if (!withinLimits(width, height)) return SourcePageRepresentationOutcome.ExtremeDimensions
        create(request, PDFRenderer(doc).renderImageWithDPI(request.pageNumber - 1, dpi.toFloat(), ImageType.RGB),
            doc.numberOfPages, SourcePageDimensions(decimal(box.width), decimal(box.height), "PDF_POINT"), rotation,
            PDFBOX_ID, PDFBOX_VERSION, PDFBOX_BUILD)
    }

    private fun renderPng(request: SourcePageRenderRequest): SourcePageRepresentationOutcome {
        if (request.pageNumber != 1) return SourcePageRepresentationOutcome.InvalidPageIndex
        val image = ImageIO.read(ByteArrayInputStream(request.sourceBytes)) ?: return SourcePageRepresentationOutcome.CorruptSource
        if (!withinLimits(image.width.toLong(), image.height.toLong())) return SourcePageRepresentationOutcome.ExtremeDimensions
        return create(request, image, 1, SourcePageDimensions(image.width.toString(), image.height.toString(), "SOURCE_PIXEL"), 0,
            IMAGEIO_ID, System.getProperty("java.version"), System.getProperty("java.vendor") + ":" + System.getProperty("java.runtime.version"))
    }

    private fun create(request: SourcePageRenderRequest, source: BufferedImage, pageCount: Int, sourceDimensions: SourcePageDimensions,
        rotation: Int, rendererId: String, rendererVersion: String, rendererBuild: String): SourcePageRepresentationOutcome {
        if (!withinLimits(source.width.toLong(), source.height.toLong())) return SourcePageRepresentationOutcome.ExtremeDimensions
        val image = BufferedImage(source.width, source.height, BufferedImage.TYPE_INT_RGB); val g = image.createGraphics(); configure(g)
        g.color = Color.WHITE; g.fillRect(0, 0, image.width, image.height); g.drawImage(source, 0, 0, null); g.dispose()
        val pixels = rgbBytes(image); val dimensions = PagePixelDimensions(image.width, image.height)
        val pixelDigest = CanonicalPagePixelDigests.digest(dimensions, request.profile.pixelFormat, pixels)
        val encoded = ByteArrayOutputStream().use { out -> check(ImageIO.write(image, "png", out)); out.toByteArray() }
        val provenance = PageRepresentationProvenance(request.evidenceArtifactId, request.sourceSha256, request.sourceBytes.size.toLong(),
            request.sourceMediaType, request.pageNumber, pageCount, rendererId, rendererVersion, rendererBuild, request.profile,
            sourceDimensions, rotation, dimensions, pixelDigest, CanonicalPagePixelDigests.sha256(encoded))
        return SourcePageRepresentationOutcome.Created(AuthoritativePageRepresentation(representationId(provenance), provenance, encoded, pixels))
    }

    fun crop(page: AuthoritativePageRepresentation, bounds: PixelCropBounds): DeterministicPageCrop {
        val dims = page.provenance.pixelDimensions; require(bounds.rightExclusive <= dims.width && bounds.bottomExclusive <= dims.height)
        val width = bounds.rightExclusive - bounds.left; val height = bounds.bottomExclusive - bounds.top
        val source = page.canonicalPixels(); val crop = ByteArray(width * height * 3)
        repeat(height) { row -> val start = ((bounds.top + row) * dims.width + bounds.left) * 3; source.copyInto(crop, row * width * 3, start, start + width * 3) }
        val cropDims = PagePixelDimensions(width, height)
        return DeterministicPageCrop(page.id, bounds, cropDims, CanonicalPagePixelDigests.digest(cropDims, PagePixelFormat.SRGB_8_RGB_OPAQUE, crop), crop)
    }

    private fun withinLimits(w: Long, h: Long) = w in 1..limits.maximumDimensionPixels.toLong() && h in 1..limits.maximumDimensionPixels.toLong() && w <= Long.MAX_VALUE / h && w * h <= limits.maximumDecodedPixels
    private fun configure(g: Graphics2D) { g.setRenderingHint(RenderingHints.KEY_ALPHA_INTERPOLATION, RenderingHints.VALUE_ALPHA_INTERPOLATION_QUALITY); g.setRenderingHint(RenderingHints.KEY_COLOR_RENDERING, RenderingHints.VALUE_COLOR_RENDER_QUALITY); g.composite = java.awt.AlphaComposite.SrcOver }
    private fun rgbBytes(image: BufferedImage): ByteArray { val bytes = ByteArray(image.width * image.height * 3); var i = 0; for (y in 0 until image.height) for (x in 0 until image.width) { val rgb = image.getRGB(x, y); bytes[i++] = (rgb ushr 16).toByte(); bytes[i++] = (rgb ushr 8).toByte(); bytes[i++] = rgb.toByte() }; return bytes }
    private fun representationId(p: PageRepresentationProvenance): PageRepresentationId {
        val fields = listOf("parker.source-page.representation-id.v1", p.sourceEvidenceArtifactId.value, p.sourceSha256, p.pageNumber.toString(), p.rendererIdentity, p.rendererVersion, p.rendererBuildIdentity, p.renderProfile.profileId, p.renderProfile.profileVersion.toString(), p.renderProfile.dpi.toString(), p.renderProfile.pixelFormat.name, p.renderProfile.orientationPolicy.name, p.renderProfile.transparencyPolicy.name, p.pixelDimensions.width.toString(), p.pixelDimensions.height.toString(), p.canonicalPixelDigest.value)
        val md = MessageDigest.getInstance("SHA-256"); fields.forEach { value -> val b = value.toByteArray(); md.update(byteArrayOf((b.size ushr 24).toByte(), (b.size ushr 16).toByte(), (b.size ushr 8).toByte(), b.size.toByte())); md.update(b) }
        return PageRepresentationId(md.digest().joinToString("") { "%02x".format(it) })
    }
    private fun decimal(v: Float) = BigDecimal(v.toString()).stripTrailingZeros().toPlainString()
    companion object { const val PDFBOX_ID = "apache-pdfbox"; const val PDFBOX_VERSION = "3.0.7"; const val PDFBOX_BUILD = "org.apache.pdfbox:pdfbox:3.0.7"; const val IMAGEIO_ID = "java-imageio-png" }
}
