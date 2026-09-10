package parker.core.runtime

import java.time.Instant
import kotlinx.coroutines.test.runTest
import parker.core.interfaces.HermesProcessingCorrection
import parker.core.interfaces.HermesProcessingHumanDecision
import parker.core.interfaces.HermesProcessingHumanDecisionType
import parker.core.interfaces.PrincipalId
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

/** Hermes Exception Decision Backend, Task 4. Append-only history semantics for [InMemoryHermesProcessingDecisionRegistry]. */
class InMemoryHermesProcessingDecisionRegistryTest {

    private val decidedBy = PrincipalId("owner-${"1".repeat(64)}")
    private val sha256 = "a".repeat(64)
    private val batchId = "bulk-abc"

    private fun decision(
        type: HermesProcessingHumanDecisionType,
        at: Instant,
        batch: String = batchId,
        sha: String = sha256,
        correction: HermesProcessingCorrection? = null,
    ) = HermesProcessingHumanDecision(batch, sha, type, decidedBy, at, correction = correction)

    @Test
    fun `latest returns null when nothing has been recorded`() = runTest {
        val registry = InMemoryHermesProcessingDecisionRegistry()
        assertNull(registry.latest(batchId, sha256))
        assertEquals(emptyList(), registry.history(batchId, sha256))
    }

    @Test
    fun `record then latest returns exactly what was recorded`() = runTest {
        val registry = InMemoryHermesProcessingDecisionRegistry()
        val d = decision(HermesProcessingHumanDecisionType.REJECT, Instant.parse("2026-01-01T00:00:00Z"))

        val returned = registry.record(d)

        assertEquals(d, returned)
        assertEquals(d, registry.latest(batchId, sha256))
    }

    @Test
    fun `a second decision against the same key never overwrites the first -- both remain in history, and latest reflects the most recent`() = runTest {
        val registry = InMemoryHermesProcessingDecisionRegistry()
        val first = decision(HermesProcessingHumanDecisionType.REPROCESS, Instant.parse("2026-01-01T00:00:00Z"))
        val second = decision(HermesProcessingHumanDecisionType.ACCEPT, Instant.parse("2026-01-02T00:00:00Z"))

        registry.record(first)
        registry.record(second)

        assertEquals(listOf(first, second), registry.history(batchId, sha256))
        assertEquals(second, registry.latest(batchId, sha256))
    }

    @Test
    fun `decisions never cross batch or source-hash keys`() = runTest {
        val registry = InMemoryHermesProcessingDecisionRegistry()
        val inBatchA = decision(HermesProcessingHumanDecisionType.REJECT, Instant.parse("2026-01-01T00:00:00Z"), batch = "bulk-a")
        val inBatchB = decision(HermesProcessingHumanDecisionType.ACCEPT, Instant.parse("2026-01-01T00:00:00Z"), batch = "bulk-b")
        val otherSourceSameBatch = decision(HermesProcessingHumanDecisionType.REPROCESS, Instant.parse("2026-01-01T00:00:00Z"), sha = "b".repeat(64))

        registry.record(inBatchA)
        registry.record(inBatchB)
        registry.record(otherSourceSameBatch)

        assertEquals(inBatchA, registry.latest("bulk-a", sha256))
        assertEquals(inBatchB, registry.latest("bulk-b", sha256))
        assertEquals(otherSourceSameBatch, registry.latest(batchId, "b".repeat(64)))
        assertNull(registry.latest("bulk-a", "b".repeat(64)))
    }

    @Test
    fun `decidedBy and decidedAt are retained exactly as recorded`() = runTest {
        val registry = InMemoryHermesProcessingDecisionRegistry()
        val at = Instant.parse("2026-03-15T09:30:00Z")
        val d = decision(HermesProcessingHumanDecisionType.CORRECT, at, correction = HermesProcessingCorrection(0, "corrected", "reason"))

        registry.record(d)

        val stored = registry.latest(batchId, sha256)!!
        assertEquals(decidedBy, stored.decidedBy)
        assertEquals(at, stored.decidedAt)
    }
}
