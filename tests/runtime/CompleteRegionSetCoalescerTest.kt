package parker.core.runtime

import java.nio.ByteBuffer
import java.security.MessageDigest
import org.junit.jupiter.api.Test
import parker.core.interfaces.*
import kotlin.test.*

/**
 * OI8 feasibility prototype only. This is deliberately outside production source and routing.
 * It proves a deterministic, complete-membership shaping rule without enabling provider egress.
 */
internal class CompleteRegionSetCoalescer(
    private val maximumRequestRegions: Int = 32,
) {
    data class Group(
        val id: SourceRegionId,
        val pageRepresentationId: PageRepresentationId,
        val pageNumber: Int,
        val bounds: PixelCropBounds,
        val cropDigest: CanonicalPixelDigest,
        val structuralClass: SourceRegionStructuralClass,
        val constituentsInSourceOrder: List<SourceRegionId>,
    )

    sealed interface Outcome {
        data class Shaped(val groups: List<Group>) : Outcome
        data class Unsupported(val reason: String) : Outcome
    }

    fun shape(
        graphs: List<SourceRegionOrderGraph>,
        cropDigest: (SourceRegionOrderGraph, PixelCropBounds) -> CanonicalPixelDigest,
    ): Outcome {
        if (graphs.any { it.ambiguityState != SourceRegionAmbiguityState.UNAMBIGUOUS }) {
            return Outcome.Unsupported("source order is not unambiguous")
        }
        val pages = graphs.filter { it.regions.isNotEmpty() }.sortedBy { pageNumber(it) }
        if (pages.isEmpty()) return Outcome.Unsupported("no transcribable regions")
        if (pages.size > maximumRequestRegions) {
            return Outcome.Unsupported("nonempty page count exceeds request-region bound")
        }
        val orderedByPage = pages.associateWith { graph ->
            val byId = graph.regions.associateBy { it.id }
            RegionSourceOrderReconstructor().order(listOf(graph)).getOrElse {
                return Outcome.Unsupported("source order is cyclic or inconsistent")
            }.map(byId::getValue)
        }
        val quotas = pages.associateWith { 1 }.toMutableMap()
        var remaining = maximumRequestRegions - pages.size
        while (remaining > 0) {
            val candidate = pages.filter { quotas.getValue(it) < orderedByPage.getValue(it).size }
                .maxWithOrNull(compareBy<SourceRegionOrderGraph>(
                    { orderedByPage.getValue(it).size.toDouble() / quotas.getValue(it) },
                    { -pageNumber(it) },
                    { it.pageRepresentationId.value },
                )) ?: break
            quotas[candidate] = quotas.getValue(candidate) + 1
            remaining--
        }
        val groups = pages.flatMap { graph ->
            val ordered = orderedByPage.getValue(graph)
            balancedConsecutivePartitions(ordered, quotas.getValue(graph)).map { members ->
                val bounds = union(members.map { it.bounds })
                val digest = cropDigest(graph, bounds)
                Group(
                    id = identity(graph, members, bounds, digest),
                    pageRepresentationId = graph.pageRepresentationId,
                    pageNumber = pageNumber(graph),
                    bounds = bounds,
                    cropDigest = digest,
                    structuralClass = members.map { it.structuralClass }.distinct().singleOrNull()
                        ?: SourceRegionStructuralClass.MIXED,
                    constituentsInSourceOrder = members.map { it.id },
                )
            }
        }
        check(groups.size <= maximumRequestRegions)
        return Outcome.Shaped(groups)
    }

    private fun balancedConsecutivePartitions(regions: List<SourceRegion>, count: Int): List<List<SourceRegion>> =
        (0 until count).map { index ->
            regions.subList(index * regions.size / count, (index + 1) * regions.size / count)
                .also { check(it.isNotEmpty()) }
        }

    private fun union(bounds: List<PixelCropBounds>) = PixelCropBounds(
        bounds.minOf { it.left }, bounds.minOf { it.top },
        bounds.maxOf { it.rightExclusive }, bounds.maxOf { it.bottomExclusive },
    )

    private fun pageNumber(graph: SourceRegionOrderGraph) = graph.regions.first().provenance.pageNumber

    private fun identity(
        graph: SourceRegionOrderGraph,
        members: List<SourceRegion>,
        bounds: PixelCropBounds,
        cropDigest: CanonicalPixelDigest,
    ): SourceRegionId {
        val first = members.first().provenance
        val fields = buildList {
            add("parker.complete-region-set-group.identity.v1")
            add(graph.pageRepresentationId.value); add(first.pageNumber.toString())
            add(first.derivationProfileId); add(first.derivationProfileVersion.toString())
            add(bounds.left.toString()); add(bounds.top.toString())
            add(bounds.rightExclusive.toString()); add(bounds.bottomExclusive.toString())
            add(cropDigest.value); add(members.size.toString())
            members.forEach { add(it.id.value) }
        }
        val md = MessageDigest.getInstance("SHA-256")
        fields.forEach { value ->
            val bytes = value.toByteArray(Charsets.UTF_8)
            md.update(ByteBuffer.allocate(4).putInt(bytes.size).array()); md.update(bytes)
        }
        return SourceRegionId(md.digest().joinToString("") { "%02x".format(it.toInt() and 0xff) })
    }
}

class CompleteRegionSetCoalescerTest {
    @Test fun `above 32 complete set is shaped deterministically with exact membership and source order`() {
        val graphs = listOf(graph(page = 1, count = 41), graph(page = 2, count = 26))
        val coalescer = CompleteRegionSetCoalescer()
        val first = assertIs<CompleteRegionSetCoalescer.Outcome.Shaped>(coalescer.shape(graphs, ::digest))
        val second = assertIs<CompleteRegionSetCoalescer.Outcome.Shaped>(coalescer.shape(graphs, ::digest))
        assertEquals(first, second)
        assertEquals(32, first.groups.size)
        val expected = graphs.flatMap { RegionSourceOrderReconstructor().order(listOf(it)).getOrThrow() }
        val actual = first.groups.flatMap { it.constituentsInSourceOrder }
        assertEquals(expected, actual)
        assertEquals(expected.size, actual.toSet().size)
        assertTrue(first.groups.all { group ->
            group.constituentsInSourceOrder.all { id ->
                graphs.single { it.pageRepresentationId == group.pageRepresentationId }.regions.any { it.id == id }
            }
        })
    }

    @Test fun `at or below 32 retains one group per original region`() {
        val graph = graph(page = 1, count = 32)
        val shaped = assertIs<CompleteRegionSetCoalescer.Outcome.Shaped>(
            CompleteRegionSetCoalescer().shape(listOf(graph), ::digest),
        )
        assertEquals(32, shaped.groups.size)
        assertTrue(shaped.groups.all { it.constituentsInSourceOrder.size == 1 })
        assertEquals(graph.regions.map { it.id }, shaped.groups.flatMap { it.constituentsInSourceOrder })
    }

    @Test fun `ambiguous cyclic and above 32 nonempty pages fail closed`() {
        val ambiguous = graph(1, 2).copy(
            ambiguityState = SourceRegionAmbiguityState.HUMAN_ORDER_REQUIRED,
            reason = "test ambiguity",
        )
        assertIs<CompleteRegionSetCoalescer.Outcome.Unsupported>(
            CompleteRegionSetCoalescer().shape(listOf(ambiguous), ::digest),
        )
        val cyclicBase = graph(1, 2); val ids = cyclicBase.regions.map { it.id }
        val cyclic = cyclicBase.copy(edges = setOf(
            SourceRegionOrderEdge(ids[0], ids[1], SourceRegionOrderRelation.BEFORE),
            SourceRegionOrderEdge(ids[1], ids[0], SourceRegionOrderRelation.BEFORE),
        ))
        assertIs<CompleteRegionSetCoalescer.Outcome.Unsupported>(
            CompleteRegionSetCoalescer().shape(listOf(cyclic), ::digest),
        )
        assertIs<CompleteRegionSetCoalescer.Outcome.Unsupported>(
            CompleteRegionSetCoalescer().shape((1..33).map { graph(it, 1) }, ::digest),
        )
    }

    private fun graph(page: Int, count: Int): SourceRegionOrderGraph {
        val pageId = PageRepresentationId("%064x".format(page))
        val dimensions = PagePixelDimensions(2_000, 4_000)
        val regions = (0 until count).map { index ->
            val bounds = PixelCropBounds(100, 20 + index * 50, 1_900, 55 + index * 50)
            SourceRegion(
                SourceRegionId("%064x".format(page * 10_000 + index + 1)), bounds,
                SourceRegionStructuralClass.TEXT_LIKE,
                CanonicalPixelDigest("%064x".format(page * 20_000 + index + 1)),
                SourceRegionProvenance(
                    EvidenceArtifactId("evidence-test"), "a".repeat(64), pageId, page, dimensions,
                    CanonicalPixelDigest("%064x".format(page * 30_000)),
                    "pixel-whitespace-source-regions-v1", 1,
                ),
            )
        }
        return SourceRegionOrderGraph(pageId, regions, emptySet(), SourceRegionAmbiguityState.UNAMBIGUOUS)
    }

    private fun digest(graph: SourceRegionOrderGraph, bounds: PixelCropBounds): CanonicalPixelDigest {
        val bytes = "${graph.pageRepresentationId.value}:${bounds.left}:${bounds.top}:${bounds.rightExclusive}:${bounds.bottomExclusive}".toByteArray()
        return CanonicalPixelDigest(MessageDigest.getInstance("SHA-256").digest(bytes).joinToString("") { "%02x".format(it.toInt() and 0xff) })
    }
}
