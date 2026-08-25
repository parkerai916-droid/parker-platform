package parker.ui

import java.util.concurrent.atomic.AtomicInteger
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import parker.core.interfaces.DerivativeGenerationId
import parker.core.interfaces.EvidenceArtifactId

/**
 * Owner Evidence Upload & Processing (first version). Behavioural tests for
 * [OwnerEvidenceUiController], mirroring [OwnerUiController]'s own existing
 * test discipline: a fake [OwnerEvidenceOperations], never a real
 * `ParkerRuntime` -- real-graph proof belongs to
 * `OwnerUiEvidenceRuntimeAdapterTest`.
 */
class OwnerEvidenceUiControllerTest {

    private fun TestScope.controller(operations: OwnerEvidenceOperations, rowIds: Iterator<String> = generateSequence(0) { it + 1 }.map { "row-$it" }.iterator()) =
        OwnerEvidenceUiController(operations, coroutineContext, rowIdSource = { rowIds.next() })

    private fun selection(name: String = "report.pdf", path: String = "/home/owner/$name", bytes: Long = 1024) =
        OwnerEvidenceFileSelection(absolutePath = path, originalFileName = name, byteLength = bytes)

    // ================= Import =================

    @Test
    fun `selecting one file transitions UPLOADING to READY_TO_PROCESS and carries the returned EvidenceArtifactId`() = runTest {
        val operations = FakeOwnerEvidenceOperations(
            importResult = { EvidenceImportOutcome.Imported(EvidenceArtifactId("evidence-1")) },
        )
        val controller = controller(operations)

        controller.selectFiles(listOf(selection()))
        advanceUntilIdle()

        val row = controller.state.value.files.single()
        assertEquals(OwnerEvidenceFileStatus.READY_TO_PROCESS, row.status)
        assertEquals(EvidenceArtifactId("evidence-1"), row.evidenceArtifactId)
        assertEquals("report.pdf", row.originalFileName)
        assertEquals(1024, row.byteLength)
    }

    @Test
    fun `a rejected import transitions to IMPORT_FAILED and carries the truthful reason`() = runTest {
        val operations = FakeOwnerEvidenceOperations(
            importResult = { EvidenceImportOutcome.Rejected("source exceeds the maximum accepted size") },
        )
        val controller = controller(operations)

        controller.selectFiles(listOf(selection()))
        advanceUntilIdle()

        val row = controller.state.value.files.single()
        assertEquals(OwnerEvidenceFileStatus.IMPORT_FAILED, row.status)
        assertEquals("source exceeds the maximum accepted size", row.message)
        assertNull(row.evidenceArtifactId)
    }

    @Test
    fun `an operations exception during import is caught and surfaced truthfully, never propagated`() = runTest {
        val operations = FakeOwnerEvidenceOperations(importResult = { throw IllegalStateException("boom") })
        val controller = controller(operations)

        controller.selectFiles(listOf(selection()))
        advanceUntilIdle()

        assertEquals(OwnerEvidenceFileStatus.IMPORT_FAILED, controller.state.value.files.single().status)
    }

    @Test
    fun `the absolute path is passed to importFile exactly once and never stored in state`() = runTest {
        val observedPaths = mutableListOf<String>()
        val operations = FakeOwnerEvidenceOperations(
            importResult = { path -> observedPaths += path; EvidenceImportOutcome.Imported(EvidenceArtifactId("evidence-1")) },
        )
        val controller = controller(operations)

        controller.selectFiles(listOf(selection(path = "/home/owner/secret-directory/report.pdf")))
        advanceUntilIdle()

        assertEquals(listOf("/home/owner/secret-directory/report.pdf"), observedPaths)
        val row = controller.state.value.files.single()
        assertTrue("secret-directory" !in row.originalFileName)
        assertTrue(row.toString().let { "secret-directory" !in it }, "the row's own state must never carry the local path")
    }

    @Test
    fun `the selection's own declared media type is threaded through to importFile unchanged`() = runTest {
        var observedMediaType: String? = "not observed"
        val operations = FakeOwnerEvidenceOperations(
            importResult = { EvidenceImportOutcome.Imported(EvidenceArtifactId("evidence-1")) },
            onImport = { _, declaredMediaType -> observedMediaType = declaredMediaType },
        )
        val controller = controller(operations)

        controller.selectFiles(
            listOf(OwnerEvidenceFileSelection(absolutePath = "/home/owner/06-structured.csv", originalFileName = "06-structured.csv", byteLength = 248, declaredMediaType = "text/csv")),
        )
        advanceUntilIdle()

        assertEquals("text/csv", observedMediaType)
    }

    @Test
    fun `a null declared media type is passed through as null, never fabricated`() = runTest {
        var observedMediaType: String? = "not observed"
        val operations = FakeOwnerEvidenceOperations(
            importResult = { EvidenceImportOutcome.Imported(EvidenceArtifactId("evidence-1")) },
            onImport = { _, declaredMediaType -> observedMediaType = declaredMediaType },
        )
        val controller = controller(operations)

        controller.selectFiles(listOf(selection()))
        advanceUntilIdle()

        assertNull(observedMediaType)
    }

    // ================= Multi-file independence =================

    @Test
    fun `multiple selected files each produce independent rows with independent EvidenceArtifactIds`() = runTest {
        val counter = AtomicInteger(0)
        val operations = FakeOwnerEvidenceOperations(
            importResult = { EvidenceImportOutcome.Imported(EvidenceArtifactId("evidence-${counter.incrementAndGet()}")) },
        )
        val controller = controller(operations)

        controller.selectFiles(listOf(selection("a.pdf"), selection("b.csv"), selection("c.docx")))
        advanceUntilIdle()

        val rows = controller.state.value.files
        assertEquals(3, rows.size)
        assertEquals(setOf("evidence-1", "evidence-2", "evidence-3"), rows.mapNotNull { it.evidenceArtifactId?.value }.toSet())
        assertEquals(setOf("row-0", "row-1", "row-2"), rows.map { it.rowId }.toSet())
    }

    @Test
    fun `one failed file does not affect or block already-successful independent siblings`() = runTest {
        var call = 0
        val operations = FakeOwnerEvidenceOperations(
            importResult = {
                call += 1
                if (call == 2) EvidenceImportOutcome.Rejected("hostile input") else EvidenceImportOutcome.Imported(EvidenceArtifactId("evidence-$call"))
            },
        )
        val controller = controller(operations)

        controller.selectFiles(listOf(selection("a.pdf"), selection("b.pdf"), selection("c.pdf")))
        advanceUntilIdle()

        val rows = controller.state.value.files.associateBy { it.originalFileName }
        assertEquals(OwnerEvidenceFileStatus.READY_TO_PROCESS, rows.getValue("a.pdf").status)
        assertEquals(OwnerEvidenceFileStatus.IMPORT_FAILED, rows.getValue("b.pdf").status)
        assertEquals(OwnerEvidenceFileStatus.READY_TO_PROCESS, rows.getValue("c.pdf").status)
    }

    // ================= Tier A =================

    @Test
    fun `processTierA is only reachable from READY_TO_PROCESS and calls the real Tier A path exactly once`() = runTest {
        var tierACalls = 0
        val operations = FakeOwnerEvidenceOperations(
            importResult = { EvidenceImportOutcome.Imported(EvidenceArtifactId("evidence-1")) },
            tierAResult = { tierACalls++; TierAProcessingOutcome.Admitted("CSV") },
        )
        val controller = controller(operations)
        controller.selectFiles(listOf(selection()))
        advanceUntilIdle()
        val rowId = controller.state.value.files.single().rowId

        controller.processTierA(rowId)
        advanceUntilIdle()

        assertEquals(1, tierACalls)
        val row = controller.state.value.files.single()
        assertEquals(OwnerEvidenceFileStatus.TIER_A_COMPLETE, row.status)
        assertEquals("CSV", row.tierAFormat)
    }

    @Test
    fun `processTierA before import completes is a no-op -- never calls Tier A out of order`() = runTest {
        var tierACalls = 0
        val operations = FakeOwnerEvidenceOperations(
            importResult = { EvidenceImportOutcome.Imported(EvidenceArtifactId("evidence-1")) },
            tierAResult = { tierACalls++; TierAProcessingOutcome.Admitted("CSV") },
        )
        val controller = controller(operations, rowIds = listOf("row-0").iterator())

        controller.selectFiles(listOf(selection()))
        // Deliberately not advancing the scheduler -- the row is still UPLOADING.
        controller.processTierA("row-0")
        advanceUntilIdle()

        assertEquals(0, tierACalls)
    }

    @Test
    fun `a RequiresTierB Tier A result transitions to REQUIRES_OCR, never automatically to OCR_PROCESSING or COMPLETE`() = runTest {
        var tierBCalls = 0
        val operations = FakeOwnerEvidenceOperations(
            importResult = { EvidenceImportOutcome.Imported(EvidenceArtifactId("evidence-1")) },
            tierAResult = { TierAProcessingOutcome.RequiresTierB },
            tierBResult = { tierBCalls++; TierBProcessingOutcome.Completed(1) },
        )
        val controller = controller(operations)
        controller.selectFiles(listOf(selection()))
        advanceUntilIdle()
        val rowId = controller.state.value.files.single().rowId

        controller.processTierA(rowId)
        advanceUntilIdle()

        assertEquals(OwnerEvidenceFileStatus.REQUIRES_OCR, controller.state.value.files.single().status)
        assertEquals(0, tierBCalls, "Tier B must never be invoked automatically after Tier A returns RequiresTierB")
    }

    @Test
    fun `an unsupported Tier A result transitions to FAILED with a truthful reason`() = runTest {
        val operations = FakeOwnerEvidenceOperations(
            importResult = { EvidenceImportOutcome.Imported(EvidenceArtifactId("evidence-1")) },
            tierAResult = { TierAProcessingOutcome.Unsupported("unrecognised binary content") },
        )
        val controller = controller(operations)
        controller.selectFiles(listOf(selection()))
        advanceUntilIdle()
        controller.processTierA(controller.state.value.files.single().rowId)
        advanceUntilIdle()

        val row = controller.state.value.files.single()
        assertEquals(OwnerEvidenceFileStatus.FAILED, row.status)
        assertTrue(row.message!!.contains("unrecognised binary content"))
    }

    // ================= Tier B =================

    @Test
    fun `processTierB is only reachable from REQUIRES_OCR and calls the real Tier B path exactly once`() = runTest {
        var tierBCalls = 0
        val operations = FakeOwnerEvidenceOperations(
            importResult = { EvidenceImportOutcome.Imported(EvidenceArtifactId("evidence-1")) },
            tierAResult = { TierAProcessingOutcome.RequiresTierB },
            tierBResult = { tierBCalls++; TierBProcessingOutcome.Completed(1) },
        )
        val controller = controller(operations)
        controller.selectFiles(listOf(selection()))
        advanceUntilIdle()
        val rowId = controller.state.value.files.single().rowId
        controller.processTierA(rowId)
        advanceUntilIdle()

        controller.processTierB(rowId)
        advanceUntilIdle()

        assertEquals(1, tierBCalls)
        assertEquals(OwnerEvidenceFileStatus.COMPLETE, controller.state.value.files.single().status)
    }

    @Test
    fun `processTierB before Tier A has returned RequiresTierB is a no-op`() = runTest {
        var tierBCalls = 0
        val operations = FakeOwnerEvidenceOperations(
            importResult = { EvidenceImportOutcome.Imported(EvidenceArtifactId("evidence-1")) },
            tierBResult = { tierBCalls++; TierBProcessingOutcome.Completed(1) },
        )
        val controller = controller(operations)
        controller.selectFiles(listOf(selection()))
        advanceUntilIdle()

        controller.processTierB(controller.state.value.files.single().rowId)
        advanceUntilIdle()

        assertEquals(0, tierBCalls)
    }

    @Test
    fun `a failed Tier B call transitions to FAILED with a safe, truthful message`() = runTest {
        val operations = FakeOwnerEvidenceOperations(
            importResult = { EvidenceImportOutcome.Imported(EvidenceArtifactId("evidence-1")) },
            tierAResult = { TierAProcessingOutcome.RequiresTierB },
            tierBResult = { TierBProcessingOutcome.Failed("OCR provider unavailable") },
        )
        val controller = controller(operations)
        controller.selectFiles(listOf(selection()))
        advanceUntilIdle()
        val rowId = controller.state.value.files.single().rowId
        controller.processTierA(rowId)
        advanceUntilIdle()

        controller.processTierB(rowId)
        advanceUntilIdle()

        val row = controller.state.value.files.single()
        assertEquals(OwnerEvidenceFileStatus.FAILED, row.status)
        assertEquals("OCR provider unavailable", row.message)
    }

    // ================= Fakes =================

    private class FakeOwnerEvidenceOperations(
        private val importResult: suspend (String) -> EvidenceImportOutcome = { EvidenceImportOutcome.Imported(EvidenceArtifactId("evidence-1")) },
        private val tierAResult: suspend () -> TierAProcessingOutcome = { TierAProcessingOutcome.Admitted("PDF") },
        private val tierBResult: suspend () -> TierBProcessingOutcome = { TierBProcessingOutcome.Completed(1) },
        private val onImport: (String, String?) -> Unit = { _, _ -> },
    ) : OwnerEvidenceOperations {
        override suspend fun importFile(absolutePath: String, declaredMediaType: String?): EvidenceImportOutcome {
            onImport(absolutePath, declaredMediaType)
            return importResult(absolutePath)
        }

        override suspend fun processTierA(evidenceArtifactId: EvidenceArtifactId): TierAProcessingOutcome = tierAResult()

        override suspend fun processTierB(evidenceArtifactId: EvidenceArtifactId): TierBProcessingOutcome = tierBResult()

        override suspend fun retrieveTierAExtractedContent(
            evidenceArtifactId: EvidenceArtifactId,
            derivativeGenerationId: DerivativeGenerationId,
        ): TierAContentRetrievalResult = TierAContentRetrievalResult.UnknownGeneration

        override suspend fun processTierBDurable(evidenceArtifactId: EvidenceArtifactId): parker.ui.TierBDurableProcessingOutcome =
            parker.ui.TierBDurableProcessingOutcome.NotAuthorised("not authorised")

        override suspend fun retrieveTierBOcrContent(
            evidenceArtifactId: EvidenceArtifactId,
            derivativeGenerationId: DerivativeGenerationId,
        ): parker.ui.TierBOcrContentRetrievalResult = parker.ui.TierBOcrContentRetrievalResult.UnknownGeneration
    }
}
