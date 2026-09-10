package parker.core.runtime

import kotlinx.coroutines.test.runTest
import parker.core.interfaces.HermesProcessingFailure
import parker.core.interfaces.HermesProcessingFailureKind
import parker.core.interfaces.HermesProcessingMethod
import parker.core.interfaces.HermesProcessingResult
import parker.core.interfaces.HermesProcessingResultRecordOutcome
import parker.core.interfaces.HermesProcessingStatus
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNull

class InMemoryHermesProcessingResultRegistryTest {

    private fun result(
        sha256: String = "a".repeat(64),
        batchId: String = "bulk-abc",
        status: HermesProcessingStatus = HermesProcessingStatus.PASS,
        failure: HermesProcessingFailure? = null,
    ) = HermesProcessingResult(sha256, batchId, status, setOf(HermesProcessingMethod.OCR), failure = failure)

    @Test
    fun `recording a new key returns Recorded and durably stores it`() = runTest {
        val registry = InMemoryHermesProcessingResultRegistry()
        val submitted = result()

        val outcome = registry.record(submitted)

        assertIs<HermesProcessingResultRecordOutcome.Recorded>(outcome)
        assertEquals(submitted, outcome.result)
        assertEquals(submitted, registry.find("bulk-abc", "a".repeat(64)))
    }

    @Test
    fun `an identical retry is idempotent -- AlreadyRecorded, not a second stored record`() = runTest {
        val registry = InMemoryHermesProcessingResultRegistry()
        val submitted = result()

        registry.record(submitted)
        val retry = registry.record(submitted)

        assertIs<HermesProcessingResultRecordOutcome.AlreadyRecorded>(retry)
        assertEquals(submitted, retry.result)
        assertEquals(1, registry.listForBatch("bulk-abc").size)
    }

    @Test
    fun `a different result for the same key is a conflict, and the original remains unchanged`() = runTest {
        val registry = InMemoryHermesProcessingResultRegistry()
        val original = result(status = HermesProcessingStatus.PASS)
        val changed = result(status = HermesProcessingStatus.FAILED, failure = HermesProcessingFailure(HermesProcessingFailureKind.CORRUPT_SOURCE))

        registry.record(original)
        val conflict = registry.record(changed)

        assertIs<HermesProcessingResultRecordOutcome.Conflict>(conflict)
        assertEquals(original, conflict.existing)
        assertEquals(changed, conflict.attempted)
        assertEquals(original, registry.find("bulk-abc", "a".repeat(64)), "the original record must not be overwritten by a conflicting attempt")
        assertEquals(1, registry.listForBatch("bulk-abc").size)
    }

    @Test
    fun `find returns null for a key that was never recorded`() = runTest {
        val registry = InMemoryHermesProcessingResultRegistry()

        assertNull(registry.find("bulk-nonexistent", "a".repeat(64)))
    }

    @Test
    fun `different source hashes within the same batch are independent keys`() = runTest {
        val registry = InMemoryHermesProcessingResultRegistry()
        val first = result(sha256 = "a".repeat(64))
        val second = result(sha256 = "b".repeat(64))

        registry.record(first)
        registry.record(second)

        assertEquals(2, registry.listForBatch("bulk-abc").size)
        assertEquals(first, registry.find("bulk-abc", "a".repeat(64)))
        assertEquals(second, registry.find("bulk-abc", "b".repeat(64)))
    }

    @Test
    fun `the same source hash under two different batches never collides -- results are batch-scoped`() = runTest {
        val registry = InMemoryHermesProcessingResultRegistry()
        val inBatchA = result(sha256 = "a".repeat(64), batchId = "bulk-a")
        val inBatchB = result(sha256 = "a".repeat(64), batchId = "bulk-b", status = HermesProcessingStatus.FAILED, failure = HermesProcessingFailure(HermesProcessingFailureKind.CORRUPT_SOURCE))

        val outcomeA = registry.record(inBatchA)
        val outcomeB = registry.record(inBatchB)

        assertIs<HermesProcessingResultRecordOutcome.Recorded>(outcomeA)
        assertIs<HermesProcessingResultRecordOutcome.Recorded>(outcomeB)
        assertEquals(inBatchA, registry.find("bulk-a", "a".repeat(64)))
        assertEquals(inBatchB, registry.find("bulk-b", "a".repeat(64)))
    }

    @Test
    fun `listForBatch never leaks results from another batch`() = runTest {
        val registry = InMemoryHermesProcessingResultRegistry()
        registry.record(result(sha256 = "a".repeat(64), batchId = "bulk-a"))
        registry.record(result(sha256 = "b".repeat(64), batchId = "bulk-b"))

        val listA = registry.listForBatch("bulk-a")
        val listB = registry.listForBatch("bulk-b")

        assertEquals(listOf("a".repeat(64)), listA.map { it.sourceSha256 })
        assertEquals(listOf("b".repeat(64)), listB.map { it.sourceSha256 })
    }

    @Test
    fun `listForBatch for a batch with no records is empty, not an error`() = runTest {
        val registry = InMemoryHermesProcessingResultRegistry()

        assertEquals(emptyList(), registry.listForBatch("bulk-empty"))
    }
}
