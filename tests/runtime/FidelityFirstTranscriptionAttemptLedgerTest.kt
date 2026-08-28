package parker.core.runtime

import java.nio.file.Files
import java.time.Instant
import kotlin.test.*
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Path
import java.util.concurrent.Executors
import java.util.concurrent.Callable

class FidelityFirstTranscriptionAttemptLedgerTest {
    @TempDir lateinit var root: Path
    private fun identity(attempt: String = "attempt-1") = FidelityFirstExecutionIdentity(
        "execution-1", "request-1", attempt, "evidence-1", "a".repeat(64), 10, "application/pdf",
        "b".repeat(40), "OpenAI", "gpt-5.6-sol", "openai-fidelity-first-transcription-v1",
        "c".repeat(64), "d".repeat(64), "external-transcription.direct-authoritative-byte-v1", "2.0.0",
    )

    @Test fun `provider attempt is durable and prevents ambiguous second execution after restart`() {
        val ledger = FileSystemFidelityFirstAttemptLedger(root) { Instant.EPOCH }
        val id = identity()
        ledger.requireAvailable(id)
        ledger.transition(id, FidelityFirstAttemptStage.PREFLIGHT_PASSED)
        ledger.transition(id, FidelityFirstAttemptStage.SOURCE_RETRIEVED)
        ledger.transition(id, FidelityFirstAttemptStage.REQUEST_PREPARED)
        ledger.transition(id, FidelityFirstAttemptStage.PROVIDER_ATTEMPT_STARTED)
        val restarted = FileSystemFidelityFirstAttemptLedger(root) { Instant.EPOCH.plusSeconds(1) }
        assertTrue(restarted.open(id).providerAttemptStarted)
        assertFailsWith<IllegalArgumentException> { restarted.requireAvailable(id) }
    }

    @Test fun `corruption and conflicting attempt identity fail closed`() {
        val ledger = FileSystemFidelityFirstAttemptLedger(root) { Instant.EPOCH }
        ledger.open(identity())
        assertFailsWith<IllegalArgumentException> { ledger.open(identity("attempt-2")) }
        val file = root.resolve("execution-1.fidelity-attempt-ledger")
        Files.writeString(file, Files.readString(file).replace("OpenAI", "Other"))
        assertFailsWith<IllegalArgumentException> { ledger.open(identity()) }
    }

    @Test fun `tracker records exactly one provider attempt transition`() {
        val tracker = FidelityFirstAttemptTracker(FileSystemFidelityFirstAttemptLedger(root) { Instant.EPOCH }, identity())
        tracker.authorised(); tracker.preflightPassed(); tracker.sourceRetrieved(); tracker.requestPrepared()
        tracker.providerAttemptStarting(); tracker.providerResponseReceived(); tracker.generationAdmitted(); tracker.terminalSuccess()
        assertEquals(1, tracker.snapshot().stages.count { it == FidelityFirstAttemptStage.PROVIDER_ATTEMPT_STARTED })
    }

    @Test fun `safe identity preserves safe values and deterministically encodes governed punctuation`() {
        assertEquals("fa-9_4p-a1", FidelityFirstExecutionSafeIdentity.derive("fa-9_4p-a1"))
        val dotted = FidelityFirstExecutionSafeIdentity.derive("fa-9.4p-a1")
        assertEquals(dotted, FidelityFirstExecutionSafeIdentity.derive("fa-9.4p-a1"))
        assertTrue(dotted.matches(Regex("^[A-Za-z0-9_-]{1,120}$")))
        assertNotEquals(dotted, FidelityFirstExecutionSafeIdentity.derive("fa-9_4p-a1"))
    }

    @Test fun `safe identity rejects controls and encodes separators whitespace and unicode punctuation`() {
        listOf("a/b", "a\\b", "a b", "a—b").forEach { governed ->
            val safe = FidelityFirstExecutionSafeIdentity.derive(governed)
            assertTrue(safe.matches(Regex("^[A-Za-z0-9_-]{1,120}$")))
            assertFalse('/' in safe || '\\' in safe || ".." in safe || safe.any(Char::isWhitespace))
        }
        assertFailsWith<IllegalArgumentException> { FidelityFirstExecutionSafeIdentity.derive("a\nb") }
    }

    @Test fun `long and punctuation-distinct identities remain collision resistant and bounded`() {
        val prefix = "a".repeat(4_000)
        val first = FidelityFirstExecutionSafeIdentity.derive("$prefix.x")
        val second = FidelityFirstExecutionSafeIdentity.derive("$prefix-y")
        assertTrue(first.length <= 120 && second.length <= 120)
        assertNotEquals(first, second)
        assertNotEquals(
            FidelityFirstExecutionSafeIdentity.derive("punctuation.one"),
            FidelityFirstExecutionSafeIdentity.derive("punctuation-one"),
        )
        val encoded = FidelityFirstExecutionSafeIdentity.derive("punctuation.one")
        assertNotEquals(encoded, FidelityFirstExecutionSafeIdentity.derive(encoded))
    }

    @Test fun `dotted governed execution identity retains correlation and uses one safe ledger key`() {
        val governed = FidelityFirstExecutionIdentity(
            "execution-fa-9.4p-a1-synthetic", "request-fa-9.4p-a1-synthetic", "attempt-fa-9.4p-a1-synthetic",
            "evidence-1", "a".repeat(64), 10, "application/pdf", "b".repeat(40), "OpenAI", "gpt-5.6-sol",
            "openai-fidelity-first-transcription-v1", "c".repeat(64), "d".repeat(64),
            "external-transcription.direct-authoritative-byte-v1", "2.0.0",
        )
        val ledger = FileSystemFidelityFirstAttemptLedger(root) { Instant.EPOCH }
        ledger.requireAvailable(governed)
        val restarted = FileSystemFidelityFirstAttemptLedger(root) { Instant.EPOCH.plusSeconds(1) }
        assertEquals(governed, restarted.open(governed).identity)
        assertTrue(Files.exists(root.resolve("${governed.safeExecutionId}.fidelity-attempt-ledger")))
        assertEquals(1, Files.list(root).use { files -> files.filter { it.fileName.toString().endsWith(".fidelity-attempt-ledger") }.count() })
    }

    @Test fun `concurrent duplicate dotted identity creates one ledger attempt`() {
        val governed = identity().copy(
            executionId = "execution-fa-9.4p-a1-synthetic",
            requestId = "request-fa-9.4p-a1-synthetic",
            attemptId = "attempt-fa-9.4p-a1-synthetic",
        )
        val ledger = FileSystemFidelityFirstAttemptLedger(root) { Instant.EPOCH }
        val pool = Executors.newFixedThreadPool(2)
        try {
            val results = List(2) { pool.submit(Callable { runCatching { ledger.requireAvailable(governed) } }) }
            assertTrue(results.map { it.get() }.any { it.getOrNull()?.identity == governed })
        } finally { pool.shutdownNow() }
        assertEquals(1, Files.list(root).use { files -> files.filter { it.fileName.toString().endsWith(".fidelity-attempt-ledger") }.count() })
    }
}
