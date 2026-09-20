package parker.core.runtime

import java.nio.file.Files
import java.time.Instant
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertTrue
import parker.core.interfaces.HermesProcessingServiceV1Request
import parker.core.interfaces.HermesProcessingServiceV1Source
import parker.core.interfaces.HermesV1BatchId
import parker.core.interfaces.HermesV1FailureCategory
import parker.core.interfaces.HermesV1FailureDetailCode
import parker.core.interfaces.HermesV1JobId
import parker.core.interfaces.HermesV1MediaType
import parker.core.interfaces.HermesV1OccurrenceId
import parker.core.interfaces.HermesV1OriginalFilename
import parker.core.interfaces.HermesV1ProcessingMethod
import parker.core.interfaces.HermesV1ProcessingPrincipal
import parker.core.interfaces.HermesV1RequestId
import parker.core.interfaces.HermesV1Sha256
import parker.core.interfaces.HermesV1SourceReference
import parker.core.interfaces.HermesV1SourceSize

class HermesProcessingServiceV1IdempotencyLedgerTest {
    private val principal = HermesV1ProcessingPrincipal.PARKER_PROCESSING
    private val now = Instant.parse("2026-01-01T00:00:00Z")

    @Test
    fun `first claim and same identity are durable and idempotent across reopen`() {
        val root = Files.createTempDirectory("hermes-ledger-")
        val request = request()
        val ledger = HermesProcessingServiceV1IdempotencyLedger(root)
        val created = assertIs<HermesV1LedgerClaim.Created>(ledger.claim(principal, request, now))
        assertEquals(HermesV1LedgerState.RECEIVED, created.record.state)
        val reopened = HermesProcessingServiceV1IdempotencyLedger(root)
        val existing = assertIs<HermesV1LedgerClaim.Existing>(reopened.claim(principal, request, now.plusSeconds(1)))
        assertEquals(created.record, existing.record)
    }

    @Test
    fun `source identity conflicts fail closed while filename alone does not redefine identity`() {
        val root = Files.createTempDirectory("hermes-ledger-")
        val ledger = HermesProcessingServiceV1IdempotencyLedger(root)
        ledger.claim(principal, request(), now)
        assertIs<HermesV1LedgerClaim.Existing>(ledger.claim(principal, request().copy(source = request().source.copy(originalFilename = HermesV1OriginalFilename("renamed.txt"))), now))
        listOf(
            request().copy(source = request().source.copy(sourceSha256 = HermesV1Sha256("b".repeat(64)))),
            request().copy(occurrenceId = HermesV1OccurrenceId("occurrence-2")),
            request().copy(jobId = HermesV1JobId("job-2")),
            request().copy(batchId = HermesV1BatchId("batch-2")),
            request().copy(source = request().source.copy(sizeBytes = HermesV1SourceSize(1))),
            request().copy(source = request().source.copy(mediaType = HermesV1MediaType("application/octet-stream"))),
        ).forEach { conflicting ->
            val rejected = assertIs<HermesV1LedgerClaim.Rejected>(ledger.claim(principal, conflicting, now))
            assertEquals(HermesV1FailureDetailCode.REQUEST_IDENTITY_CONFLICT, rejected.failure.detailCode)
            assertEquals(false, rejected.failure.retryable)
        }
    }

    @Test
    fun `different authenticated principal has a separate request record`() {
        val root = Files.createTempDirectory("hermes-ledger-")
        val request = request()
        val ledger = HermesProcessingServiceV1IdempotencyLedger(root)
        assertIs<HermesV1LedgerClaim.Created>(ledger.claim(principal, request, now))
        val otherPrincipal = HermesV1ProcessingPrincipal("other-processing-principal")
        assertIs<HermesV1LedgerClaim.Created>(ledger.claim(otherPrincipal, request, now))
        assertEquals(2, Files.list(root.resolve("records")).use { it.count() })
    }

    @Test
    fun `state transitions are closed and terminal result survives restart`() {
        val root = Files.createTempDirectory("hermes-ledger-")
        val request = request()
        val ledger = HermesProcessingServiceV1IdempotencyLedger(root)
        ledger.claim(principal, request, now)
        assertIs<HermesV1LedgerTransition.Updated>(ledger.transition(principal, request, HermesV1LedgerState.VERIFIED, now = now.plusSeconds(1)))
        assertIs<HermesV1LedgerTransition.Updated>(ledger.transition(principal, request, HermesV1LedgerState.PROCESSING, now = now.plusSeconds(2)))
        val result = HermesV1LedgerResult("{\"status\":\"PASS\"}".toByteArray())
        val complete = assertIs<HermesV1LedgerTransition.Updated>(ledger.transition(principal, request, HermesV1LedgerState.COMPLETE, result, now = now.plusSeconds(3)))
        assertEquals(now.plusSeconds(3).plusSeconds(7 * 24 * 60 * 60), complete.record.expiresAt)
        val reopened = HermesProcessingServiceV1IdempotencyLedger(root)
        val found = assertIs<HermesV1LedgerRecord>(reopened.find(principal, request.requestId))
        assertEquals(HermesV1LedgerState.COMPLETE, found.state)
        assertEquals(result, found.terminalResult)
        assertIs<HermesV1LedgerTransition.Rejected>(reopened.transition(principal, request, HermesV1LedgerState.FAILED, failure = testFailure(), now = now.plusSeconds(4)))
    }

    @Test
    fun `failed result is durable and backward transition is rejected`() {
        val root = Files.createTempDirectory("hermes-ledger-")
        val request = request()
        val ledger = HermesProcessingServiceV1IdempotencyLedger(root)
        ledger.claim(principal, request, now)
        assertIs<HermesV1LedgerTransition.Updated>(ledger.transition(principal, request, HermesV1LedgerState.VERIFIED, now = now.plusSeconds(1)))
        assertIs<HermesV1LedgerTransition.Rejected>(ledger.transition(principal, request, HermesV1LedgerState.RECEIVED, now = now.plusSeconds(2)))
        assertIs<HermesV1LedgerTransition.Updated>(ledger.transition(principal, request, HermesV1LedgerState.FAILED, failure = testFailure(), now = now.plusSeconds(3)))
        val reopened = HermesProcessingServiceV1IdempotencyLedger(root)
        assertEquals(HermesV1LedgerState.FAILED, reopened.find(principal, request.requestId)?.state)
    }

    @Test
    fun `oversized result is rejected without truncation or terminal mutation`() {
        val root = Files.createTempDirectory("hermes-ledger-")
        val request = request()
        val ledger = HermesProcessingServiceV1IdempotencyLedger(root)
        ledger.claim(principal, request, now)
        ledger.transition(principal, request, HermesV1LedgerState.VERIFIED, now = now.plusSeconds(1))
        ledger.transition(principal, request, HermesV1LedgerState.PROCESSING, now = now.plusSeconds(2))
        assertFailsWith<IllegalArgumentException> { HermesV1LedgerResult(ByteArray(8 * 1024 * 1024 + 1) { 'x'.code.toByte() }) }
        assertEquals(HermesV1LedgerState.PROCESSING, ledger.find(principal, request.requestId)?.state)
    }

    @Test
    fun `cleanup is bounded and preserves active records`() {
        val root = Files.createTempDirectory("hermes-ledger-")
        val requestOne = request()
        val requestTwo = request().copy(requestId = HermesV1RequestId("request-2"))
        val ledger = HermesProcessingServiceV1IdempotencyLedger(root)
        ledger.claim(principal, requestOne, now)
        ledger.transition(principal, requestOne, HermesV1LedgerState.VERIFIED, now = now.plusSeconds(1))
        ledger.transition(principal, requestOne, HermesV1LedgerState.PROCESSING, now = now.plusSeconds(2))
        ledger.transition(principal, requestOne, HermesV1LedgerState.COMPLETE, HermesV1LedgerResult("{}".toByteArray()), now = now.plusSeconds(3))
        ledger.claim(principal, requestTwo, now)
        assertEquals(0, ledger.cleanupExpired(now.plusSeconds(4), 1))
        assertEquals(1, ledger.cleanupExpired(now.plusSeconds(7 * 24 * 60 * 60 + 3), 1))
        assertNull(ledger.find(principal, requestOne.requestId))
        assertTrue(ledger.find(principal, requestTwo.requestId) != null)
    }

    @Test
    fun `corrupt and incompatible records fail closed on reopen`() {
        val root = Files.createTempDirectory("hermes-ledger-")
        val request = request()
        val ledger = HermesProcessingServiceV1IdempotencyLedger(root)
        ledger.claim(principal, request, now)
        val path = ledger.recordPathForTesting(principal, request.requestId)
        Files.write(path, byteArrayOf(0, 1, 2, 3))
        val corrupt = assertFailsWith<HermesV1LedgerException> { HermesProcessingServiceV1IdempotencyLedger(root) }
        assertEquals(HermesV1FailureDetailCode.LEDGER_CORRUPT, corrupt.failure.detailCode)
        Files.write(path, byteArrayOf(0x48, 0x56, 0x4c, 0x47, 0, 0, 0, 99))
        val schema = assertFailsWith<HermesV1LedgerException> { HermesProcessingServiceV1IdempotencyLedger(root) }
        assertEquals(HermesV1FailureDetailCode.LEDGER_SCHEMA_MISMATCH, schema.failure.detailCode)
    }

    @Test
    fun `concurrent duplicate claims create one durable record`() {
        val root = Files.createTempDirectory("hermes-ledger-")
        val request = request()
        val ledger = HermesProcessingServiceV1IdempotencyLedger(root)
        val results = (1..8).map { Thread { ledger.claim(principal, request, now) } }
        results.forEach(Thread::start)
        results.forEach(Thread::join)
        assertEquals(HermesV1LedgerState.RECEIVED, ledger.find(principal, request.requestId)?.state)
        assertEquals(1, Files.list(root.resolve("records")).use { it.count() })
    }

    private fun request() = HermesProcessingServiceV1Request(
        protocolVersion = parker.core.interfaces.HermesV1ProtocolVersion.CURRENT,
        requestId = HermesV1RequestId("request-1"),
        jobId = HermesV1JobId("job-1"),
        occurrenceId = HermesV1OccurrenceId("occurrence-1"),
        batchId = HermesV1BatchId("batch-1"),
        source = HermesProcessingServiceV1Source(
            HermesV1SourceReference("source-1"), HermesV1Sha256("a".repeat(64)),
            HermesV1SourceSize(0), HermesV1OriginalFilename("file.txt"), HermesV1MediaType("text/plain"),
        ),
        requestedMethods = listOf(HermesV1ProcessingMethod.DIRECT_TEXT_EXTRACTION),
    )

    private fun testFailure() = parker.core.interfaces.HermesV1Failure(
        HermesV1FailureCategory.PROCESSOR,
        HermesV1FailureDetailCode.PROCESSOR_FAILED,
        retryable = false,
        detail = "processor failed",
    )
}
