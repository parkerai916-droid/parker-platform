package parker.core.runtime

import java.nio.file.Path
import kotlin.io.path.readBytes
import kotlin.test.*
import org.junit.jupiter.api.Test
import parker.core.interfaces.*

/** Governed A1 geometry evaluation; source text is never extracted or inspected. */
class R62A1GeometryEvaluationTest {
    @Test fun `A1 regions and source-order anchors are deterministic`() {
        val path = System.getenv("R62_A1_WITNESS_PATH")?.let(Path::of) ?: return
        val bytes = path.readBytes(); assertEquals("7373ad403b4fae5bf5c777deb8524eaa3ba38594ce9fabfa8fcbce22fbd33182", CanonicalPagePixelDigests.sha256(bytes))
        val renderer = DeterministicSourcePageRenderer(); val deriver = DeterministicSourceRegionDeriver()
        val graphs = (1..3).associateWith { pageNumber ->
            val request = SourcePageRenderRequest(EvidenceArtifactId("evidence-0275472f-535a-4cf1-b30d-f45ac7684743"), CanonicalPagePixelDigests.sha256(bytes), "application/pdf", bytes,
                pageNumber, PageRenderProfile("authoritative-page-region-raster-v1", 1, 300))
            val page = assertIs<SourcePageRepresentationOutcome.Created>(renderer.render(request)).representation
            val runs = (1..10).map { assertIs<SourceRegionDerivationOutcome.Derived>(deriver.derive(page)).graph }
            assertEquals(1, runs.map(::signature).toSet().size); runs.first()
        }
        graphs.forEach { (page, graph) -> println("R62_A1|page=$page|regions=${graph.regions.size}|ambiguity=${graph.ambiguityState}|" + graph.regions.sortedBy { it.bounds.top }.joinToString(";") { "${it.id.value.take(12)}:${it.bounds.left},${it.bounds.top},${it.bounds.rightExclusive},${it.bounds.bottomExclusive}:${it.structuralClass}:${it.cropDigest.value.take(12)}" }) }
        // Source-image annotations established from the selected 300-DPI page representation.
        val page2 = graphs.getValue(2); val proposition = regionIntersecting(page2, 350, 1100, 2200, 1320)
        val previous = page2.regions.filter { it.bounds.bottomExclusive <= proposition.bounds.top && horizontalOverlap(it, proposition) }.maxBy { it.bounds.bottomExclusive }
        val following = page2.regions.filter { it.bounds.top >= proposition.bounds.bottomExclusive && horizontalOverlap(it, proposition) }.minBy { it.bounds.top }
        assertBefore(page2, previous, proposition); assertBefore(page2, proposition, following)
        val page3 = graphs.getValue(3); val authorization = regionIntersecting(page3, 250, 120, 2250, 1850)
        val closing = page3.regions.filter { it.bounds.top >= authorization.bounds.bottomExclusive && horizontalOverlap(it, authorization) }.minBy { it.bounds.top }
        assertBefore(page3, authorization, closing)
        println("R62_A1_RELATIONSHIPS|page2=PASS|page3=PASS|p2=${proposition.id.value}|p3=${authorization.id.value}")
    }
    private fun regionIntersecting(g: SourceRegionOrderGraph, l: Int, t: Int, r: Int, b: Int) = g.regions.maxBy { maxOf(0, minOf(r, it.bounds.rightExclusive) - maxOf(l, it.bounds.left)) * maxOf(0, minOf(b, it.bounds.bottomExclusive) - maxOf(t, it.bounds.top)) }
    private fun horizontalOverlap(a: SourceRegion, b: SourceRegion) = minOf(a.bounds.rightExclusive, b.bounds.rightExclusive) > maxOf(a.bounds.left, b.bounds.left)
    private fun assertBefore(g: SourceRegionOrderGraph, a: SourceRegion, b: SourceRegion) = assertTrue(g.edges.contains(SourceRegionOrderEdge(a.id, b.id, SourceRegionOrderRelation.BEFORE)), "missing BEFORE ${a.bounds} -> ${b.bounds}")
    private fun signature(g: SourceRegionOrderGraph) = g.regions.joinToString("|") { "${it.id.value}:${it.bounds}:${it.cropDigest.value}:${it.structuralClass}" } + "#" + g.edges.sortedBy { it.toString() } + "#" + g.ambiguityState
}
