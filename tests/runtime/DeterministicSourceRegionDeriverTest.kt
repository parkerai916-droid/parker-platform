package parker.core.runtime

import java.awt.Color
import java.awt.image.BufferedImage
import java.io.ByteArrayOutputStream
import javax.imageio.ImageIO
import kotlin.test.*
import org.junit.jupiter.api.Test
import parker.core.interfaces.*

class DeterministicSourceRegionDeriverTest {
    private val renderer = DeterministicSourcePageRenderer(); private val deriver = DeterministicSourceRegionDeriver()

    @Test fun `ten repeated derivations preserve regions identities crops classes and graph`() {
        val page = page(singleColumnFixture())
        val signatures = (1..10).map { signature(assertIs<SourceRegionDerivationOutcome.Derived>(deriver.derive(page)).graph) }
        assertEquals(1, signatures.toSet().size)
    }

    @Test fun `single column and emphasized insertion are geometrically ordered`() {
        val graph = derive(singleColumnFixture())
        assertTrue(graph.regions.size >= 3); assertEquals(SourceRegionAmbiguityState.UNAMBIGUOUS, graph.ambiguityState)
        val ordered = graph.regions.sortedBy { it.bounds.top }
        assertTrue(graph.edges.contains(SourceRegionOrderEdge(ordered[0].id, ordered[1].id, SourceRegionOrderRelation.BEFORE)))
        assertTrue(graph.edges.contains(SourceRegionOrderEdge(ordered[1].id, ordered[2].id, SourceRegionOrderRelation.BEFORE)))
    }

    @Test fun `two columns remain column peers rather than naive global y order`() {
        val graph = derive(twoColumnFixture()); assertEquals(SourceRegionAmbiguityState.UNAMBIGUOUS, graph.ambiguityState)
        assertTrue(graph.edges.any { it.relation == SourceRegionOrderRelation.COLUMN_PEER })
    }

    @Test fun `table signature header footer mixed media separators and whitespace remain bounded regions`() {
        val graph = derive(complexFixture())
        assertTrue(graph.regions.any { it.structuralClass == SourceRegionStructuralClass.TABLE_LIKE })
        assertTrue(graph.regions.size >= 5); assertTrue(graph.regions.all { it.bounds.top >= 0 && it.bounds.bottomExclusive <= 900 })
    }

    @Test fun `overlapping non-contained regions fail closed in order graph`() {
        val overlapDeriver = DeterministicSourceRegionDeriver(SourceRegionDerivationProfile(paddingPixels = 50))
        val graph = assertIs<SourceRegionDerivationOutcome.Derived>(overlapDeriver.derive(page(overlapFixture()))).graph
        assertEquals(SourceRegionAmbiguityState.HUMAN_ORDER_REQUIRED, graph.ambiguityState)
    }

    @Test fun `source binding invalid bounds digest and resource limits fail closed`() {
        assertFailsWith<IllegalArgumentException> { PixelCropBounds(0, 0, 0, 4) }
        val limited = DeterministicSourceRegionDeriver(SourceRegionDerivationProfile(maximumRegionsPerPage = 1))
        assertIs<SourceRegionDerivationOutcome.ExcessiveRegions>(limited.derive(page(singleColumnFixture())))
        val page = page(singleColumnFixture()); val a = assertIs<SourceRegionDerivationOutcome.Derived>(deriver.derive(page)).graph.regions.first()
        val crop = renderer.crop(page, a.bounds); assertEquals(a.cropDigest, crop.canonicalPixelDigest)
        assertEquals(page.provenance.sourceSha256, a.provenance.sourceSha256)
    }

    private fun derive(image: BufferedImage) = assertIs<SourceRegionDerivationOutcome.Derived>(deriver.derive(page(image))).graph
    private fun page(image: BufferedImage): AuthoritativePageRepresentation {
        val bytes = ByteArrayOutputStream().use { out -> ImageIO.write(image, "png", out); out.toByteArray() }
        val request = SourcePageRenderRequest(EvidenceArtifactId("synthetic-r6-2"), CanonicalPagePixelDigests.sha256(bytes), "image/png", bytes, 1,
            PageRenderProfile("authoritative-page-region-raster-v1", 1, null))
        return assertIs<SourcePageRepresentationOutcome.Created>(renderer.render(request)).representation
    }
    private fun canvas() = BufferedImage(1200, 900, BufferedImage.TYPE_INT_RGB).also { val g = it.createGraphics(); g.color = Color.WHITE; g.fillRect(0, 0, it.width, it.height); g.dispose() }
    private fun line(i: BufferedImage, x: Int, y: Int, w: Int, h: Int = 12) { val g = i.createGraphics(); g.color = Color.BLACK; g.fillRect(x, y, w, h); g.dispose() }
    private fun singleColumnFixture() = canvas().also { i -> for (y in listOf(80, 105, 130)) line(i, 100, y, 750); for (y in listOf(260, 285)) line(i, 140, y, 650, 18); for (y in listOf(430, 455, 480)) line(i, 100, y, 780) }
    private fun twoColumnFixture() = canvas().also { i -> for (y in listOf(100, 130, 160, 190)) { line(i, 80, y, 420); line(i, 700, y, 420) } }
    private fun complexFixture() = canvas().also { i ->
        line(i, 80, 30, 1000); line(i, 80, 850, 1000)
        val g = i.createGraphics(); g.color = Color.BLACK; for (y in listOf(180, 240, 300)) g.fillRect(80, y, 600, 4); for (x in listOf(80, 280, 480, 680)) g.fillRect(x, 180, 4, 124)
        g.color = Color(30, 100, 180); g.fillRect(760, 190, 300, 180); g.dispose()
        for (y in listOf(500, 530)) line(i, 100, y, 700); line(i, 100, 620, 520, 20); line(i, 100, 700, 760)
    }
    private fun overlapFixture() = canvas().also { i -> line(i, 100, 100, 300, 50); line(i, 490, 100, 300, 50) }
    private fun signature(g: SourceRegionOrderGraph) = g.regions.joinToString("|") { "${it.id.value}:${it.bounds}:${it.cropDigest.value}:${it.structuralClass}" } + "#" + g.edges.sortedBy { it.toString() } + "#" + g.ambiguityState
}
