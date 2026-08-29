package parker.core.runtime

import java.awt.Color
import java.awt.image.BufferedImage
import java.io.ByteArrayOutputStream
import javax.imageio.ImageIO
import kotlin.test.*
import org.apache.pdfbox.pdmodel.PDDocument
import org.apache.pdfbox.pdmodel.PDPage
import org.apache.pdfbox.pdmodel.PDPageContentStream
import org.apache.pdfbox.pdmodel.common.PDRectangle
import org.apache.pdfbox.pdmodel.font.Standard14Fonts
import org.apache.pdfbox.pdmodel.font.PDType1Font
import org.apache.pdfbox.pdmodel.graphics.image.LosslessFactory
import org.junit.jupiter.api.Test
import parker.core.interfaces.*

class DeterministicSourcePageRendererTest {
    private val renderer = DeterministicSourcePageRenderer(); private val artifact = EvidenceArtifactId("synthetic-r6-1")

    @Test fun `ten repeated PDF renders are byte and pixel deterministic at all evaluated DPIs`() {
        val pdf = syntheticPdf()
        for (dpi in listOf(200, 300, 400)) {
            val pixels = mutableSetOf<CanonicalPixelDigest>(); val encoded = mutableSetOf<String>()
            val dimensions = mutableSetOf<PagePixelDimensions>(); val identities = mutableSetOf<PageRepresentationId>()
            repeat(10) { val result = renderPdf(pdf, 1, dpi); pixels += result.provenance.canonicalPixelDigest; encoded += result.provenance.encodedRepresentationSha256; dimensions += result.provenance.pixelDimensions; identities += result.id }
            assertEquals(1, pixels.size); assertEquals(1, encoded.size); assertEquals(1, dimensions.size); assertEquals(1, identities.size)
        }
    }

    @Test fun `synthetic fixture covers visual structures rotation transparency image and fine rules`() {
        val pdf = syntheticPdf(); val first = renderPdf(pdf, 1, 300); val rotated = renderPdf(pdf, 2, 300)
        assertEquals(PagePixelDimensions(2480, 3507), first.provenance.pixelDimensions)
        assertEquals(PagePixelDimensions(3507, 2480), rotated.provenance.pixelDimensions); assertEquals(90, rotated.provenance.sourceRotationDegrees)
        assertTrue(first.encodedBytes().size > 10_000)
    }

    @Test fun `transparent PNG is deterministically composited onto white sRGB and source bound`() {
        val image = BufferedImage(20, 10, BufferedImage.TYPE_INT_ARGB); val g = image.createGraphics(); g.color = Color(255, 0, 0, 128); g.fillRect(0, 0, 20, 10); g.dispose()
        val bytes = ByteArrayOutputStream().use { out -> ImageIO.write(image, "png", out); out.toByteArray() }
        val req = SourcePageRenderRequest(artifact, CanonicalPagePixelDigests.sha256(bytes), "image/png", bytes, 1, PageRenderProfile("authoritative-page-region-raster-v1", 1, null))
        val a = assertIs<SourcePageRepresentationOutcome.Created>(renderer.render(req)).representation; val b = assertIs<SourcePageRepresentationOutcome.Created>(renderer.render(req)).representation
        assertEquals(a.provenance.canonicalPixelDigest, b.provenance.canonicalPixelDigest); assertEquals(a.provenance.encodedRepresentationSha256, b.provenance.encodedRepresentationSha256)
        assertEquals(PageTransparencyPolicy.COMPOSITE_SRGB_WHITE, a.provenance.renderProfile.transparencyPolicy)
    }

    @Test fun `crop is deterministic and bound checked`() {
        val page = renderPdf(syntheticPdf(), 1, 200); val bounds = PixelCropBounds(10, 20, 310, 220)
        assertEquals(renderer.crop(page, bounds).canonicalPixelDigest, renderer.crop(page, bounds).canonicalPixelDigest)
        assertFailsWith<IllegalArgumentException> { renderer.crop(page, PixelCropBounds(0, 0, page.provenance.pixelDimensions.width + 1, 10)) }
    }

    @Test fun `invalid source digest page media corruption and extreme dimensions fail closed`() {
        val pdf = syntheticPdf(); val good = SourcePageRenderRequest(artifact, CanonicalPagePixelDigests.sha256(pdf), "application/pdf", pdf, 1, profile(300))
        assertIs<SourcePageRepresentationOutcome.SourceDigestMismatch>(renderer.render(good.copy(sourceSha256 = "0".repeat(64))))
        assertIs<SourcePageRepresentationOutcome.InvalidPageIndex>(renderer.render(good.copy(pageNumber = 99)))
        assertIs<SourcePageRepresentationOutcome.UnsupportedMedia>(renderer.render(good.copy(sourceMediaType = "image/jpeg")))
        val corrupt = byteArrayOf(1, 2, 3); assertIs<SourcePageRepresentationOutcome.CorruptSource>(renderer.render(good.copy(sourceBytes = corrupt, sourceSha256 = CanonicalPagePixelDigests.sha256(corrupt))))
        assertIs<SourcePageRepresentationOutcome.ExtremeDimensions>(DeterministicSourcePageRenderer(SourcePageRendererLimits(maximumDecodedPixels = 10)).render(good))
    }

    @Test fun `defensive copies preserve source and representation immutability`() {
        val pdf = syntheticPdf(); val original = pdf.copyOf(); val page = renderPdf(pdf, 1, 200); pdf.fill(0); assertFalse(pdf.contentEquals(original))
        val encoded = page.encodedBytes(); encoded.fill(0); assertNotEquals("0".repeat(64), page.provenance.encodedRepresentationSha256)
    }

    private fun renderPdf(bytes: ByteArray, page: Int, dpi: Int): AuthoritativePageRepresentation {
        val req = SourcePageRenderRequest(artifact, CanonicalPagePixelDigests.sha256(bytes), "application/pdf", bytes, page, profile(dpi))
        return assertIs<SourcePageRepresentationOutcome.Created>(renderer.render(req)).representation
    }
    private fun profile(dpi: Int) = PageRenderProfile("authoritative-page-region-raster-v1", 1, dpi)

    private fun syntheticPdf(): ByteArray = PDDocument().use { doc ->
        val font = PDType1Font(Standard14Fonts.FontName.HELVETICA); val bold = PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD)
        val page = PDPage(PDRectangle.A4); doc.addPage(page)
        PDPageContentStream(doc, page).use { cs ->
            fun text(v: String, x: Float, y: Float, size: Float = 10f, b: Boolean = false) { cs.beginText(); cs.setFont(if (b) bold else font, size); cs.newLineAtOffset(x, y); cs.showText(v); cs.endText() }
            text("HEADER - synthetic direct-source page", 48f, 810f, 9f); text("Single-column paragraph with small type.", 48f, 770f, 8f)
            text("EMPHASIZED PROPOSITION BETWEEN PARAGRAPHS", 48f, 742f, 11f, true); text("Following paragraph preserves proposition location.", 48f, 714f)
            text("LEFT COLUMN", 48f, 660f, 10f, true); text("RIGHT COLUMN", 320f, 660f, 10f, true); text("Left column line one", 48f, 640f); text("Right column line one", 320f, 640f)
            cs.setLineWidth(0.25f); for (i in 0..3) { cs.moveTo(48f, 570f - i * 22); cs.lineTo(548f, 570f - i * 22) }; for (x in listOf(48f, 215f, 382f, 548f)) { cs.moveTo(x, 504f); cs.lineTo(x, 570f) }; cs.stroke()
            text("TABLE", 52f, 555f, 9f, true); text("A", 220f, 533f); text("B", 387f, 511f)
            val raster = BufferedImage(80, 40, BufferedImage.TYPE_INT_ARGB); val g = raster.createGraphics(); g.color = Color(0, 80, 200, 100); g.fillRect(0, 0, 80, 40); g.color = Color.BLACK; g.drawLine(0, 0, 79, 39); g.dispose()
            cs.drawImage(LosslessFactory.createFromImage(doc, raster), 48f, 410f, 160f, 80f); text("Mixed text and transparent embedded raster", 220f, 445f)
            text("AUTHORIZATION / SIGNATURE BLOCK", 48f, 320f, 11f, true); cs.moveTo(48f, 290f); cs.lineTo(300f, 290f); cs.stroke(); text("Closing prose follows authorization.", 48f, 250f); text("FOOTER", 48f, 30f, 8f)
        }
        val rotated = PDPage(PDRectangle.A4); rotated.rotation = 90; doc.addPage(rotated)
        PDPageContentStream(doc, rotated).use { cs -> cs.beginText(); cs.setFont(font, 12f); cs.newLineAtOffset(72f, 720f); cs.showText("Rotated page visual orientation test"); cs.endText() }
        ByteArrayOutputStream().use { out -> doc.save(out); out.toByteArray() }
    }
}
