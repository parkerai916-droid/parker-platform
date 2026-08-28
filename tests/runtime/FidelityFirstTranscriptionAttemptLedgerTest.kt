package parker.core.runtime

import java.nio.file.Files
import java.time.Instant
import kotlin.test.*
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Path

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
}
