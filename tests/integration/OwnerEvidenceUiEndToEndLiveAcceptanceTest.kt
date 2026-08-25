package parker.integration

import java.nio.file.Files
import java.nio.file.Path
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assumptions.assumeTrue
import parker.composition.ConsoleParkerLogger
import parker.composition.LogLevel
import parker.composition.OwnerUiEvidenceRuntimeAdapter
import parker.composition.ParkerRuntime
import parker.composition.ParkerRuntimeConfig
import parker.core.interfaces.PrincipalId
import parker.ui.OwnerEvidenceFileSelection
import parker.ui.OwnerEvidenceFileStatus
import parker.ui.OwnerEvidenceUiController

/**
 * Owner UI Evidence Upload & Processing -- the decisive live acceptance
 * instrument (Phase 17): the real, complete, explicit owner workflow the
 * Compose desktop UI itself drives -- [OwnerEvidenceUiController] backed by
 * the real [OwnerUiEvidenceRuntimeAdapter], itself backed by a real,
 * provisioned Docling installation and a real
 * `tools/docling-ocr-bridge.py` subprocess -- over all seven governed
 * fixtures. There is no browser and no HTTP transport anywhere in Parker's
 * owner UI (a native Compose Desktop application with direct, in-process
 * access to `ParkerRuntime`) -- this file is that architecture's own
 * "equivalent integration harness," driving the exact production Kotlin
 * objects the Compose window itself calls, never a fake or a UI-only stub.
 *
 * Mirrors `TierBOwnerRoutingLiveAcceptanceTest.kt`'s own established shape
 * (env-var configuration, [LIVE_PROPERTY]-gated `assumeTrue`) and reuses its
 * exact same live-provisioning gate.
 */
class OwnerEvidenceUiEndToEndLiveAcceptanceTest {

    companion object {
        private const val LIVE_PROPERTY = "parker.ocr.docling.live.enabled"
    }

    private val ownerPrincipalId = "user.owner-evidence-ui-e2e-live"
    private val fixtureRoot: Path = Path.of("tests", "fixtures", "document-ingestion-bakeoff", "fixtures")

    private fun resolvedPythonExecutablePath(): String? =
        System.getenv("DOCLING_TEST_PYTHON")?.takeIf { it.isNotBlank() }

    private fun resolvedBridgeScriptPath(): String =
        System.getenv("DOCLING_TEST_BRIDGE_SCRIPT")?.takeIf { it.isNotBlank() }
            ?: Path.of("tools", "docling-ocr-bridge.py").toAbsolutePath().toString()

    private fun assumeLiveDoclingPrerequisitesProvisioned() {
        val pythonExecutablePath = resolvedPythonExecutablePath()
        assumeTrue(
            pythonExecutablePath != null,
            "Live Docling prerequisites are not provisioned on this machine -- missing: DOCLING_TEST_PYTHON.",
        )
        val probe = try {
            ProcessBuilder(listOf(pythonExecutablePath!!, "-c", "import docling")).start().apply { waitFor() }.exitValue() == 0
        } catch (_: Exception) {
            false
        }
        assumeTrue(probe, "DOCLING_TEST_PYTHON=$pythonExecutablePath cannot import the docling package -- skipping")
    }

    private fun config(): ParkerRuntimeConfig = ParkerRuntimeConfig(
        modelEndpointUrl = "http://127.0.0.1:1/api/generate", // deliberately unreachable -- reasoning is not under test here
        modelName = "test-model",
        ownerPrincipalId = ownerPrincipalId,
        localTextChannelModuleId = "channel.local-text-evidence-ui-e2e-live",
        evidenceStorageRootPath = Files.createTempDirectory("evidence-ui-e2e-live-evidence").toString(),
        evidenceSourceManifestStorageRootPath = Files.createTempDirectory("evidence-ui-e2e-live-manifest").toString(),
        derivativeGenerationStorageRootPath = Files.createTempDirectory("evidence-ui-e2e-live-derivative").toString(),
        derivativeContentStorageRootPath = Files.createTempDirectory("evidence-ui-e2e-live-derivative-content").toString(),
        savedAnalysisStorageRootPath = Files.createTempDirectory("saved-analysis-storage").toString(),
        documentIngestionAuditLogPath = Files.createTempDirectory("evidence-ui-e2e-live-ingestion-audit").resolve("audit.log").toString(),
        evidenceDeletionAuditLogPath = Files.createTempDirectory("evidence-ui-e2e-live-deletion-audit").resolve("audit.log").toString(),
        memoryCoreDurabilityLogPath = Files.createTempDirectory("evidence-ui-e2e-live-memory").resolve("memory-core.log").toString(),
        knowledgeItemDurabilityLogPath = Files.createTempDirectory("evidence-ui-e2e-live-knowledge").resolve("items.log").toString(),
        doclingPythonExecutablePath = resolvedPythonExecutablePath() ?: "python3",
        doclingBridgeScriptPath = resolvedBridgeScriptPath(),
        doclingModelCacheDir = System.getenv("DOCLING_TEST_MODEL_CACHE_DIR")?.takeIf { it.isNotBlank() },
        doclingTimeoutMillis = 120_000L,
        logLevel = LogLevel.ERROR,
    )

    @Test
    fun `the real owner UI workflow processes all seven governed fixtures truthfully, with real Docling for the two OCR-eligible fixtures`() {
        runBlocking {
        assumeTrue(System.getProperty(LIVE_PROPERTY) == "true", "Live Docling property absent; no subprocess invoked")
        assumeLiveDoclingPrerequisitesProvisioned()

        val runtime = ParkerRuntime(config(), ConsoleParkerLogger(component = "evidence-ui-e2e-live", minLevel = LogLevel.ERROR))
        runtime.start()
        val adapter = OwnerUiEvidenceRuntimeAdapter(
            ownerPrincipalId = PrincipalId(ownerPrincipalId),
            importEvidenceFileAsOwner = runtime::importEvidenceFileAsOwner,
            invokeTierAIngestionAsOwner = runtime::invokeTierAIngestionAsOwner,
            analyseEvidence = runtime::analyseEvidence,
            retrieveTierAExtractedContentAsOwner = runtime::retrieveTierAExtractedContentAsOwner,
            invokeTierBOcrDurableGenerationAsOwner = runtime::invokeTierBOcrDurableGenerationAsOwner,
            retrieveTierBOcrContentAsOwner = runtime::retrieveTierBOcrContentAsOwner,
            analyseDocumentsAsOwner = runtime::analyseDocumentsAsOwner,
            saveAnalysisAsOwner = runtime::saveAnalysisAsOwner,
            retrieveSavedAnalysisAsOwner = runtime::retrieveSavedAnalysisAsOwner,
            listSavedAnalysesAsOwner = runtime::listSavedAnalysesAsOwner,
        )
        val controller = OwnerEvidenceUiController(adapter)

        data class Case(val file: String, val mediaType: String, val expectFinalStatus: OwnerEvidenceFileStatus, val expectFormat: String?)
        val cases = listOf(
            Case("01-searchable-simple.pdf", "application/pdf", OwnerEvidenceFileStatus.TIER_A_COMPLETE, "PDF"),
            Case("02-multicolumn-complex.pdf", "application/pdf", OwnerEvidenceFileStatus.TIER_A_COMPLETE, "PDF"),
            Case("03-scanned.pdf", "application/pdf", OwnerEvidenceFileStatus.COMPLETE, null),
            Case("04-structured.docx", "application/vnd.openxmlformats-officedocument.wordprocessingml.document", OwnerEvidenceFileStatus.TIER_A_COMPLETE, "DOCX"),
            Case("05-email-with-attachment.eml", "message/rfc822", OwnerEvidenceFileStatus.TIER_A_COMPLETE, "EML"),
            Case("06-structured.csv", "text/csv", OwnerEvidenceFileStatus.TIER_A_COMPLETE, "CSV"),
            Case("07-text-image.png", "image/png", OwnerEvidenceFileStatus.COMPLETE, null),
        )

        // Step 1: select/import all seven files, exactly as the UI's own "Select Files..." action would.
        controller.selectFiles(
            cases.map { case ->
                val path = fixtureRoot.resolve(case.file)
                OwnerEvidenceFileSelection(path.toAbsolutePath().toString(), case.file, Files.size(path), case.mediaType)
            },
        )
        awaitStatus(controller) { rows -> rows.all { it.status == OwnerEvidenceFileStatus.READY_TO_PROCESS } }

        val rowsByFile = controller.state.value.files.associateBy { it.originalFileName }
        assertEquals(7, rowsByFile.size)

        // Step 2: explicit Process for every row.
        rowsByFile.values.forEach { controller.processTierA(it.rowId) }
        awaitStatus(controller) { rows ->
            rows.all { it.status == OwnerEvidenceFileStatus.TIER_A_COMPLETE || it.status == OwnerEvidenceFileStatus.REQUIRES_OCR }
        }

        val afterTierA = controller.state.value.files.associateBy { it.originalFileName }
        assertEquals(OwnerEvidenceFileStatus.REQUIRES_OCR, afterTierA.getValue("03-scanned.pdf").status)
        assertEquals(OwnerEvidenceFileStatus.REQUIRES_OCR, afterTierA.getValue("07-text-image.png").status)
        assertEquals("PDF", afterTierA.getValue("01-searchable-simple.pdf").tierAFormat)
        assertEquals("PDF", afterTierA.getValue("02-multicolumn-complex.pdf").tierAFormat)
        assertEquals("DOCX", afterTierA.getValue("04-structured.docx").tierAFormat)
        assertEquals("EML", afterTierA.getValue("05-email-with-attachment.eml").tierAFormat)
        assertEquals("CSV", afterTierA.getValue("06-structured.csv").tierAFormat)

        // Step 3: explicit Run OCR for the two RequiresTierB rows only -- a real Docling invocation.
        val ocrStart = System.nanoTime()
        controller.processTierB(afterTierA.getValue("03-scanned.pdf").rowId)
        controller.processTierB(afterTierA.getValue("07-text-image.png").rowId)
        awaitStatus(controller) { rows -> cases.all { case -> rows.first { it.originalFileName == case.file }.status == case.expectFinalStatus } }
        val ocrElapsedMillis = (System.nanoTime() - ocrStart) / 1_000_000
        println("Owner UI end-to-end live acceptance: real OCR leg elapsed=${ocrElapsedMillis}ms")
        assertTrue(ocrElapsedMillis > 1000, "real Docling recognition over two real fixtures is not near-instantaneous")

        val finalRows = controller.state.value.files.associateBy { it.originalFileName }
        cases.forEach { case ->
            assertEquals(case.expectFinalStatus, finalRows.getValue(case.file).status, "unexpected final status for ${case.file}")
            if (case.expectFormat != null) {
                assertEquals(case.expectFormat, finalRows.getValue(case.file).tierAFormat, "unexpected Tier A format for ${case.file}")
            }
        }

        runtime.shutdown()
        }
    }

    private suspend fun awaitStatus(
        controller: OwnerEvidenceUiController,
        predicate: (List<parker.ui.OwnerEvidenceFileRow>) -> Boolean,
    ) {
        val deadline = System.nanoTime() + java.util.concurrent.TimeUnit.SECONDS.toNanos(60)
        while (System.nanoTime() < deadline) {
            if (predicate(controller.state.value.files)) return
            kotlinx.coroutines.delay(50)
        }
        error("timed out waiting for owner evidence UI state -- last state: ${controller.state.value.files}")
    }
}
