package parker.core.runtime

import java.nio.file.Files
import java.time.Instant
import kotlinx.coroutines.test.runTest
import kotlin.test.*
import parker.core.interfaces.*

class HumanVerificationStorageTest {
    private val evidence = EvidenceArtifactId("evidence-review")
    private val generation = DerivativeGenerationId("generation-review")

    @Test
    fun `records are immutable exact-pair durable and absence derives UNREVIEWED`() = runTest {
        val root = Files.createTempDirectory("human-verification")
        val store = FileSystemHumanVerificationStorage(root)
        assertTrue(store.listForExactGeneration(evidence, generation).isEmpty())

        val failed = record("review-b", HumanVerificationOutcome.REVIEW_FAILED, "sensitive-review-note")
        store.prepare(failed); store.publishPrepared(failed.humanVerificationRecordId)
        assertEquals(failed, store.retrieve(failed.humanVerificationRecordId))
        assertFailsWith<HumanVerificationStorageException.DuplicateIdentifier> { store.prepare(failed) }
        assertTrue(store.listForExactGeneration(EvidenceArtifactId("wrong"), generation).isEmpty())
        assertTrue(store.listForExactGeneration(evidence, DerivativeGenerationId("wrong")).isEmpty())

        val passed = record("review-a", HumanVerificationOutcome.REVIEW_PASSED, null)
        store.prepare(passed); store.publishPrepared(passed.humanVerificationRecordId)
        val partial = record("review-c", HumanVerificationOutcome.PARTIALLY_VERIFIED, null)
        store.prepare(partial); store.publishPrepared(partial.humanVerificationRecordId)

        val restarted = FileSystemHumanVerificationStorage(root)
        assertEquals(listOf(passed, failed, partial), restarted.listForExactGeneration(evidence, generation))
        assertEquals(TranscriptionFidelity.UNVERIFIED_LITERAL_TRANSCRIPTION, TranscriptionFidelity.UNVERIFIED_LITERAL_TRANSCRIPTION)
    }

    @Test
    fun `codec rejects corruption and diagnostics never contain sensitive notes`() {
        val value = record("review-safe", HumanVerificationOutcome.REVIEW_FAILED, "SECRET_REVIEW_NOTE")
        val encoded = HumanVerificationRecordCodec.encode(value)
        assertEquals(value, HumanVerificationRecordCodec.decode(encoded))
        assertFails { HumanVerificationRecordCodec.decode(encoded + 1) }
        assertFalse(value.toString().contains("SECRET_REVIEW_NOTE"))
        assertFalse(HumanVerificationStorageException.CorruptRecord(value.humanVerificationRecordId, "bounded").message.orEmpty().contains("SECRET_REVIEW_NOTE"))
    }

    private fun record(id: String, outcome: HumanVerificationOutcome, notes: String?) = HumanVerificationRecord(
        HumanVerificationRecordId(id), evidence, generation, OcrPageScope(listOf(1)),
        listOf(HumanVerificationCharacterScope(1, 0, 3)), PrincipalId("owner.review"),
        Instant.parse("2026-08-27T00:00:00Z"), outcome, OcrSha256Digest("a".repeat(64)), notes,
    )
}
