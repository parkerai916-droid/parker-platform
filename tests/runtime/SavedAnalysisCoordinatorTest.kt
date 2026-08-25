package parker.core.runtime

import java.nio.file.Files
import java.time.Instant
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlinx.coroutines.test.runTest
import parker.core.interfaces.AnalysisEvidenceItem
import parker.core.interfaces.DerivativeCompletenessState
import parker.core.interfaces.DerivativeContentIdentity
import parker.core.interfaces.DerivativeGenerationId
import parker.core.interfaces.EvidenceArtifactId
import parker.core.interfaces.OwnerDocumentAnalysisResult
import parker.core.interfaces.PendingAnalysisId
import parker.core.interfaces.RetrieveSavedAnalysisOutcome
import parker.core.interfaces.SaveAnalysisOutcome
import parker.core.interfaces.SavedAnalysisId
import parker.core.interfaces.SavedAnalysisRecord
import parker.core.interfaces.SavedAnalysisStorage
import parker.core.interfaces.SavedAnalysisStorageException
import parker.core.interfaces.TierADerivativePayloadFixtures

/**
 * Reviewed Analysis Result — Explicit Owner Save. Behavioural tests for
 * [SavedAnalysisCoordinator]: the full claim → validate → persist → finalize
 * sequence, honest failure handling that never loses a still-saveable
 * pending result, and bounded retrieval/listing.
 */
class SavedAnalysisCoordinatorTest {

    private fun result(
        analysisText: String = "some analysis text",
        instruction: String = "Summarise",
        evidenceItemCount: Int = 1,
    ) = OwnerDocumentAnalysisResult(
        analysisText = analysisText,
        evidenceItems = (1..evidenceItemCount).map {
            AnalysisEvidenceItem(
                evidenceArtifactId = EvidenceArtifactId("evidence-$it"),
                derivativeGenerationId = DerivativeGenerationId("gen-$it"),
                derivativeKind = "Searchable PDF literal text",
                contentIdentity = DerivativeContentIdentity.NoCanonicalSerialization,
                producerIdentity = TierADerivativePayloadFixtures.PRODUCER,
                extractedText = "extracted text",
                completenessState = DerivativeCompletenessState.ACCOUNTED_FOR,
                warnings = emptyList(),
            )
        },
        mechanismIdentity = null,
        mechanismVersion = null,
        analysedAt = Instant.parse("2026-01-01T00:00:00Z"),
        instruction = instruction,
        warnings = emptyList(),
    )

    private class FailingSavedAnalysisStorage(private val delegate: SavedAnalysisStorage, private val failPrepare: Boolean) : SavedAnalysisStorage {
        var prepareCallCount = 0
            private set

        override suspend fun prepare(record: SavedAnalysisRecord) {
            prepareCallCount += 1
            if (failPrepare) throw SavedAnalysisStorageException.PersistenceFailure("simulated disk failure", RuntimeException("simulated"))
            delegate.prepare(record)
        }

        override suspend fun publishPrepared(savedAnalysisId: SavedAnalysisId) = delegate.publishPrepared(savedAnalysisId)
        override suspend fun retrieve(savedAnalysisId: SavedAnalysisId): SavedAnalysisRecord? = delegate.retrieve(savedAnalysisId)
        override suspend fun listRecentIds(maxCount: Int): List<SavedAnalysisId> = delegate.listRecentIds(maxCount)
    }

    private fun realStorage() = FileSystemSavedAnalysisStorage(Files.createTempDirectory("saved-analysis-coordinator-test"))

    // ================= A, D: successful save persists exact instruction/result/references =================

    @Test
    fun `D a successful Save persists the exact instruction, analysis text, and evidence references the coordinator held`() = runTest {
        val cache = PendingAnalysisCache()
        val storage = realStorage()
        val coordinator = SavedAnalysisCoordinator(cache, storage)
        val analysisResult = result(analysisText = "The Case-ID is PF-007/26.", instruction = "State the Case-ID")
        val pendingId = cache.register(analysisResult)

        val outcome = coordinator.save(pendingId)

        val saved = assertIs<SaveAnalysisOutcome.Saved>(outcome)
        val retrieved = assertIs<RetrieveSavedAnalysisOutcome.Retrieved>(coordinator.retrieve(saved.savedAnalysisId))
        assertEquals(analysisResult.instruction, retrieved.record.instruction)
        assertEquals(analysisResult.analysisText, retrieved.record.analysisText)
        assertEquals(analysisResult.analysedAt, retrieved.record.analysedAt)
        assertEquals(1, retrieved.record.evidenceReferences.size)
        assertEquals(analysisResult.evidenceItems.single().evidenceArtifactId, retrieved.record.evidenceReferences.single().evidenceArtifactId)
        assertEquals(analysisResult.evidenceItems.single().derivativeGenerationId, retrieved.record.evidenceReferences.single().derivativeGenerationId)
    }

    // ================= F, G: unknown/expired pending id rejected =================

    @Test
    fun `F an unknown pending id is rejected with UnknownOrExpiredPendingAnalysis, nothing is persisted`() = runTest {
        val cache = PendingAnalysisCache()
        val storage = realStorage()
        val coordinator = SavedAnalysisCoordinator(cache, storage)

        val outcome = coordinator.save(PendingAnalysisId("never-registered"))

        assertEquals(SaveAnalysisOutcome.UnknownOrExpiredPendingAnalysis, outcome)
        assertEquals(emptyList(), storage.listRecentIds(10))
    }

    // ================= H: one-shot / duplicate-safe semantics =================

    @Test
    fun `H a second Save attempt on an already-saved pending id is rejected, never creating a second saved record`() = runTest {
        val cache = PendingAnalysisCache()
        val storage = realStorage()
        val coordinator = SavedAnalysisCoordinator(cache, storage)
        val pendingId = cache.register(result())

        val first = assertIs<SaveAnalysisOutcome.Saved>(coordinator.save(pendingId))
        val second = coordinator.save(pendingId)

        assertEquals(SaveAnalysisOutcome.UnknownOrExpiredPendingAnalysis, second)
        assertEquals(listOf(first.savedAnalysisId), storage.listRecentIds(10))
    }

    // ================= I: failed publication does not lose a still-saveable pending result =================

    @Test
    fun `I a failed durable publication leaves the pending analysis saveable -- a retry with the same pending id succeeds`() = runTest {
        val cache = PendingAnalysisCache()
        val realBackingStorage = realStorage()
        val failingStorage = FailingSavedAnalysisStorage(realBackingStorage, failPrepare = true)
        val coordinator = SavedAnalysisCoordinator(cache, failingStorage)
        val analysisResult = result()
        val pendingId = cache.register(analysisResult)

        val firstAttempt = coordinator.save(pendingId)
        assertIs<SaveAnalysisOutcome.PersistenceFailed>(firstAttempt)
        assertEquals(emptyList(), realBackingStorage.listRecentIds(10))

        // Retry with the SAME pending id, now against a working storage -- proves the pending
        // result was never consumed by the failed first attempt.
        val retryCoordinator = SavedAnalysisCoordinator(cache, realBackingStorage)
        val retryOutcome = assertIs<SaveAnalysisOutcome.Saved>(retryCoordinator.save(pendingId))
        val retrieved = assertIs<RetrieveSavedAnalysisOutcome.Retrieved>(retryCoordinator.retrieve(retryOutcome.savedAnalysisId))
        assertEquals(analysisResult.analysisText, retrieved.record.analysisText)
    }

    // ================= Concurrency: AlreadyInProgress =================

    @Test
    fun `a Save already claimed (in-flight) is reported as SaveAlreadyInProgress, never racing to create two saved records`() = runTest {
        val cache = PendingAnalysisCache()
        val storage = realStorage()
        val coordinator = SavedAnalysisCoordinator(cache, storage)
        val pendingId = cache.register(result())
        // Simulate a concurrent, still-in-flight Save by claiming directly without finalizing.
        cache.claim(pendingId)

        val outcome = coordinator.save(pendingId)

        assertEquals(SaveAnalysisOutcome.SaveAlreadyInProgress, outcome)
    }

    // ================= M: oversized instruction/result/reference set rejected =================

    @Test
    fun `M an oversized reviewed instruction is rejected with SavedRecordTooLarge, the pending analysis remains saveable`() = runTest {
        val cache = PendingAnalysisCache()
        val storage = realStorage()
        val coordinator = SavedAnalysisCoordinator(cache, storage)
        val oversizedInstruction = "x".repeat(DocumentAnalysisCoordinator.MAX_INSTRUCTION_CHARACTERS + 1)
        val pendingId = cache.register(result(instruction = oversizedInstruction))

        val outcome = coordinator.save(pendingId)

        val tooLarge = assertIs<SaveAnalysisOutcome.SavedRecordTooLarge>(outcome)
        assertEquals("instruction", tooLarge.field)
        assertEquals(emptyList(), storage.listRecentIds(10))
    }

    @Test
    fun `M an oversized reviewed analysis text is rejected with SavedRecordTooLarge`() = runTest {
        val cache = PendingAnalysisCache()
        val storage = realStorage()
        val coordinator = SavedAnalysisCoordinator(cache, storage)
        val oversizedText = "y".repeat(DocumentAnalysisCoordinator.MAX_RESPONSE_CHARACTERS + 1)
        val pendingId = cache.register(result(analysisText = oversizedText))

        val outcome = coordinator.save(pendingId)

        val tooLarge = assertIs<SaveAnalysisOutcome.SavedRecordTooLarge>(outcome)
        assertEquals("analysisText", tooLarge.field)
    }

    // ================= J, K: corrupt / unsupported version retrieval =================

    @Test
    fun `J a corrupt saved record is reported honestly, never partially decoded`() = runTest {
        val root = Files.createTempDirectory("saved-analysis-coordinator-corrupt")
        val storage = FileSystemSavedAnalysisStorage(root)
        val cache = PendingAnalysisCache()
        val coordinator = SavedAnalysisCoordinator(cache, storage)
        val pendingId = cache.register(result())
        val saved = assertIs<SaveAnalysisOutcome.Saved>(coordinator.save(pendingId))
        Files.write(root.resolve("${saved.savedAnalysisId.value}.saved-analysis"), byteArrayOf(9, 9, 9, 9))

        val outcome = coordinator.retrieve(saved.savedAnalysisId)

        assertIs<RetrieveSavedAnalysisOutcome.CorruptRecord>(outcome)
    }

    // ================= N, O: bounded listing, metadata only =================

    @Test
    fun `N listRecent returns a bounded, newest-first set of summaries`() = runTest {
        val cache = PendingAnalysisCache()
        val storage = realStorage()
        val coordinator = SavedAnalysisCoordinator(cache, storage)
        repeat(3) { i ->
            val pendingId = cache.register(result(instruction = "instruction-$i"))
            coordinator.save(pendingId)
            Thread.sleep(20)
        }

        val summaries = coordinator.listRecent()

        assertEquals(3, summaries.size)
        assertEquals("instruction-2", summaries.first().instructionPreview)
    }

    @Test
    fun `O a listing entry never carries the full analysis text -- only a bounded instruction preview`() = runTest {
        val cache = PendingAnalysisCache()
        val storage = realStorage()
        val coordinator = SavedAnalysisCoordinator(cache, storage)
        val longInstruction = "a".repeat(SavedAnalysisCoordinator.INSTRUCTION_PREVIEW_MAX_CHARACTERS + 500)
        val pendingId = cache.register(result(instruction = longInstruction, analysisText = "very long analysis text ".repeat(100)))
        coordinator.save(pendingId)

        val summary = coordinator.listRecent().single()

        assertEquals(SavedAnalysisCoordinator.INSTRUCTION_PREVIEW_MAX_CHARACTERS, summary.instructionPreview.length)
    }

    // ================= P: retrieve-by-id exact equality, no model invocation possible from this class at all =================

    @Test
    fun `P retrieve-by-id returns exact field equality with what was saved`() = runTest {
        val cache = PendingAnalysisCache()
        val storage = realStorage()
        val coordinator = SavedAnalysisCoordinator(cache, storage)
        val analysisResult = result(analysisText = "exact text", instruction = "exact instruction")
        val pendingId = cache.register(analysisResult)
        val saved = assertIs<SaveAnalysisOutcome.Saved>(coordinator.save(pendingId))

        val retrieved = assertIs<RetrieveSavedAnalysisOutcome.Retrieved>(coordinator.retrieve(saved.savedAnalysisId))

        assertEquals(analysisResult.analysisText, retrieved.record.analysisText)
        assertEquals(analysisResult.instruction, retrieved.record.instruction)
        assertEquals(saved.savedAnalysisId, retrieved.record.savedAnalysisId)
    }

    // ================= W: this class holds no ModelInferenceClient/reasoning-provider dependency of any kind =================

    @Test
    fun `W this coordinator's own declared fields contain no ModelInferenceClient or reasoning-provider dependency`() {
        val fieldTypeNames = SavedAnalysisCoordinator::class.java.declaredFields.map { it.type.simpleName }.toSet()
        val forbidden = listOf("ModelInferenceClient", "ReasoningProvider", "DocumentAnalysisPromptBuilder", "PermissionEngine")
        forbidden.forEach { forbiddenType ->
            assert(fieldTypeNames.none { it.contains(forbiddenType) }) {
                "SavedAnalysisCoordinator's own declared fields must contain no $forbiddenType reference -- found: $fieldTypeNames"
            }
        }
    }
}
