package parker.core.runtime

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFails
import kotlin.test.assertTrue
import kotlinx.coroutines.test.runTest
import parker.core.interfaces.*

class GovernedPageRepresentationPersistenceTest {
    @Test fun `owner order requires a complete exact region set and produces immutable resolution`() {
        val graph = graph(ambiguous = true)
        val ids = graph.regions.map { it.id }
        val resolution = OwnerSourceOrderValidator.validate(graph, OwnerSourceOrderInput("steven", "2026-09-01T00:00:00Z", ids.reversed())).getOrThrow()
        assertEquals("OWNER_RESOLVED_SOURCE_ORDER", resolution.status)
        assertEquals(ids.reversed(), resolution.orderedRegionIds)
        assertFails { OwnerSourceOrderValidator.validate(graph, OwnerSourceOrderInput("steven", "t", ids.drop(1))) .getOrThrow() }
        assertTrue(graph.ambiguityState == SourceRegionAmbiguityState.HUMAN_ORDER_REQUIRED)
    }

    @Test fun `in-memory persistence is write-once and order state is readable`() = runTest {
        val store = InMemoryGovernedPageRepresentationPersistence()
        val graph = graph(ambiguous = false)
        val state = SourceRegionOrderState(graph.pageRepresentationId, SourceRegionSetIdentity.digest(graph), graph.regions.map { it.id }, "DETERMINISTIC_SOURCE_ORDER", "test", 1)
        store.persistGeometry(graph); store.persistOrderState(state)
        assertEquals(state, store.readOrderState(graph.pageRepresentationId))
        assertFails { store.persistOrderState(state) }
    }

    private fun graph(ambiguous: Boolean): SourceRegionOrderGraph {
        val evidence = EvidenceArtifactId("evidence-test")
        val page = PageRepresentationId("a".repeat(64))
        val digest = CanonicalPixelDigest("b".repeat(64))
        fun region(id: String, left: Int) = SourceRegion(SourceRegionId(id.repeat(64)), PixelCropBounds(left, 0, left + 1, 1), SourceRegionStructuralClass.TEXT_LIKE, digest,
            SourceRegionProvenance(evidence, "c".repeat(64), page, 1, PagePixelDimensions(2, 1), digest, "test", 1))
        val first = region("1", 0); val second = region("2", 1)
        return SourceRegionOrderGraph(page, listOf(first, second), emptySet(), if (ambiguous) SourceRegionAmbiguityState.HUMAN_ORDER_REQUIRED else SourceRegionAmbiguityState.UNAMBIGUOUS, if (ambiguous) "ambiguous" else null)
    }
}
