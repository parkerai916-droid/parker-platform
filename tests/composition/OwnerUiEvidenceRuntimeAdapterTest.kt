package parker.composition

import java.nio.file.Files
import java.nio.file.Path
import kotlin.reflect.full.declaredFunctions
import kotlinx.coroutines.test.runTest
import parker.core.interfaces.EvidenceArtifactId
import parker.core.interfaces.EvidenceRetrievalResult
import parker.core.interfaces.PrincipalId
import parker.ui.EvidenceImportOutcome
import parker.ui.TierAProcessingOutcome
import parker.ui.TierBProcessingOutcome
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

/**
 * Owner Evidence Upload & Processing (first version). End-to-end acceptance
 * tests against the real, fully-wired production graph -- mirroring
 * [ParkerRuntimeOwnerLocalFileIngressIntegrationTest]/[ParkerRuntimeTierAOwnerInvocationIntegrationTest]'s
 * own established "prove the wiring itself" style, and
 * [ParkerRuntimeOcrCompositionTest]'s own fake-bridge-via-shell-script
 * technique for the Tier B leg (never a fake `DoclingSubprocessInvoker`
 * Kotlin object -- the real subprocess path, a fake program on the other
 * end). Proves [OwnerUiEvidenceRuntimeAdapter] truthfully maps real
 * `ParkerRuntime` outcomes -- not the underlying entry points themselves,
 * already exhaustively covered elsewhere.
 */
class OwnerUiEvidenceRuntimeAdapterTest {

    private val ownerPrincipalId = "user.owner-evidence-ui-adapter-test"
    private val fixtureRoot: Path = Path.of("tests", "fixtures", "document-ingestion-bakeoff", "fixtures")

    private fun config(
        doclingPythonExecutablePath: String = "/bin/sh",
        doclingBridgeScriptPath: String,
    ): ParkerRuntimeConfig = ParkerRuntimeConfig(
        modelEndpointUrl = "http://127.0.0.1:1/api/generate", // deliberately unreachable
        modelName = "test-model",
        ownerPrincipalId = ownerPrincipalId,
        localTextChannelModuleId = "channel.local-text-evidence-ui-adapter-test",
        evidenceStorageRootPath = Files.createTempDirectory("evidence-ui-adapter-evidence").toString(),
        evidenceSourceManifestStorageRootPath = Files.createTempDirectory("evidence-ui-adapter-manifest").toString(),
        derivativeGenerationStorageRootPath = Files.createTempDirectory("evidence-ui-adapter-derivative").toString(),
        documentIngestionAuditLogPath = Files.createTempDirectory("evidence-ui-adapter-ingestion-audit").resolve("audit.log").toString(),
        evidenceDeletionAuditLogPath = Files.createTempDirectory("evidence-ui-adapter-deletion-audit").resolve("audit.log").toString(),
        memoryCoreDurabilityLogPath = Files.createTempDirectory("evidence-ui-adapter-memory").resolve("memory-core.log").toString(),
        knowledgeItemDurabilityLogPath = Files.createTempDirectory("evidence-ui-adapter-knowledge").resolve("items.log").toString(),
        doclingPythonExecutablePath = doclingPythonExecutablePath,
        doclingBridgeScriptPath = doclingBridgeScriptPath,
        doclingTimeoutMillis = 30_000L,
    )

    private fun writeFakeBridgeScript(directory: Path, exitCode: Int, stdout: String): Path {
        val scriptPath = Files.createTempFile(directory, "fake-docling-bridge-", ".sh")
        Files.writeString(
            scriptPath,
            "#!/bin/sh\n" + (if (stdout.isNotEmpty()) "printf '%s' '$stdout'\n" else "") + "exit $exitCode\n",
        )
        scriptPath.toFile().setExecutable(true)
        return scriptPath
    }

    private fun adapterFor(runtime: ParkerRuntime) = OwnerUiEvidenceRuntimeAdapter(
        ownerPrincipalId = PrincipalId(ownerPrincipalId),
        importEvidenceFileAsOwner = runtime::importEvidenceFileAsOwner,
        invokeTierAIngestionAsOwner = runtime::invokeTierAIngestionAsOwner,
        analyseEvidence = runtime::analyseEvidence,
    )

    // ================= Import =================

    @Test
    fun `importFile against the real production graph accepts a real local file end to end -- exact bytes, real EvidenceArtifactId`() = runTest {
        val scriptDir = Files.createTempDirectory("evidence-ui-adapter-scripts")
        val runtime = ParkerRuntime(config(doclingBridgeScriptPath = writeFakeBridgeScript(scriptDir, 0, "").toString()), RecordingParkerLogger())
        runtime.start()
        val sourceDir = Files.createTempDirectory("evidence-ui-adapter-source")
        val sourcePath = sourceDir.resolve("report.pdf")
        val content = "real evidence UI adapter local file bytes".toByteArray()
        Files.write(sourcePath, content)

        val outcome = adapterFor(runtime).importFile(sourcePath.toString(), "application/pdf")

        val imported = assertIs<EvidenceImportOutcome.Imported>(outcome)
        val retrieved = runtime.retrieveEvidence(PrincipalId(ownerPrincipalId), imported.evidenceArtifactId)
        val found = assertIs<EvidenceRetrievalResult.Found>(retrieved)
        assertTrue(content.contentEquals(found.content))

        runtime.shutdown()
    }

    @Test
    fun `a Unicode original filename is preserved as basename metadata through the real import path`() = runTest {
        val scriptDir = Files.createTempDirectory("evidence-ui-adapter-scripts")
        val runtime = ParkerRuntime(config(doclingBridgeScriptPath = writeFakeBridgeScript(scriptDir, 0, "").toString()), RecordingParkerLogger())
        runtime.start()
        val sourceDir = Files.createTempDirectory("evidence-ui-adapter-source")
        val sourcePath = sourceDir.resolve("Kōwhai Whanganui café report — 2026.pdf")
        Files.write(sourcePath, "unicode filename content".toByteArray())

        val outcome = adapterFor(runtime).importFile(sourcePath.toString(), "application/pdf")

        assertIs<EvidenceImportOutcome.Imported>(outcome)

        runtime.shutdown()
    }

    @Test
    fun `an oversized file is rejected truthfully, never accepted`() = runTest {
        val scriptDir = Files.createTempDirectory("evidence-ui-adapter-scripts")
        val runtime = ParkerRuntime(config(doclingBridgeScriptPath = writeFakeBridgeScript(scriptDir, 0, "").toString()), RecordingParkerLogger())
        runtime.start()
        val sourceDir = Files.createTempDirectory("evidence-ui-adapter-source")
        val sourcePath = sourceDir.resolve("oversized.bin")
        Files.write(sourcePath, ByteArray(65 * 1024 * 1024)) // 65 MiB, over the existing 64 MiB bound

        val outcome = adapterFor(runtime).importFile(sourcePath.toString(), "application/octet-stream")

        assertIs<EvidenceImportOutcome.Rejected>(outcome)

        runtime.shutdown()
    }

    @Test
    fun `a nonexistent path is rejected truthfully, never accepted`() = runTest {
        val scriptDir = Files.createTempDirectory("evidence-ui-adapter-scripts")
        val runtime = ParkerRuntime(config(doclingBridgeScriptPath = writeFakeBridgeScript(scriptDir, 0, "").toString()), RecordingParkerLogger())
        runtime.start()

        val outcome = adapterFor(runtime).importFile("/nonexistent/path/never-created.pdf", "application/pdf")

        assertIs<EvidenceImportOutcome.Rejected>(outcome)

        runtime.shutdown()
    }

    // ================= Tier A =================

    @Test
    fun `processTierA routes each governed format correctly through the real production graph`() = runTest {
        val scriptDir = Files.createTempDirectory("evidence-ui-adapter-scripts")
        val runtime = ParkerRuntime(config(doclingBridgeScriptPath = writeFakeBridgeScript(scriptDir, 0, "").toString()), RecordingParkerLogger())
        runtime.start()
        val adapter = adapterFor(runtime)

        val csv = assertIs<EvidenceImportOutcome.Imported>(adapter.importFile(fixtureRoot.resolve("06-structured.csv").toAbsolutePath().toString(), "text/csv"))
        assertEquals(TierAProcessingOutcome.Admitted("CSV"), adapter.processTierA(csv.evidenceArtifactId))

        val eml = assertIs<EvidenceImportOutcome.Imported>(adapter.importFile(fixtureRoot.resolve("05-email-with-attachment.eml").toAbsolutePath().toString(), "message/rfc822"))
        assertEquals(TierAProcessingOutcome.Admitted("EML"), adapter.processTierA(eml.evidenceArtifactId))

        val docx = assertIs<EvidenceImportOutcome.Imported>(
            adapter.importFile(fixtureRoot.resolve("04-structured.docx").toAbsolutePath().toString(), "application/vnd.openxmlformats-officedocument.wordprocessingml.document"),
        )
        assertEquals(TierAProcessingOutcome.Admitted("DOCX"), adapter.processTierA(docx.evidenceArtifactId))

        val pdf = assertIs<EvidenceImportOutcome.Imported>(adapter.importFile(fixtureRoot.resolve("01-searchable-simple.pdf").toAbsolutePath().toString(), "application/pdf"))
        assertEquals(TierAProcessingOutcome.Admitted("PDF"), adapter.processTierA(pdf.evidenceArtifactId))

        val scanned = assertIs<EvidenceImportOutcome.Imported>(adapter.importFile(fixtureRoot.resolve("03-scanned.pdf").toAbsolutePath().toString(), "application/pdf"))
        assertEquals(TierAProcessingOutcome.RequiresTierB, adapter.processTierA(scanned.evidenceArtifactId))

        val png = assertIs<EvidenceImportOutcome.Imported>(adapter.importFile(fixtureRoot.resolve("07-text-image.png").toAbsolutePath().toString(), "image/png"))
        assertEquals(TierAProcessingOutcome.RequiresTierB, adapter.processTierA(png.evidenceArtifactId))

        runtime.shutdown()
    }

    @Test
    fun `processTierA for an evidence artefact never registered fails truthfully, never fabricated`() = runTest {
        val scriptDir = Files.createTempDirectory("evidence-ui-adapter-scripts")
        val runtime = ParkerRuntime(config(doclingBridgeScriptPath = writeFakeBridgeScript(scriptDir, 0, "").toString()), RecordingParkerLogger())
        runtime.start()

        val outcome = adapterFor(runtime).processTierA(EvidenceArtifactId("evidence-never-registered"))

        assertIs<TierAProcessingOutcome.Failed>(outcome)

        runtime.shutdown()
    }

    // ================= Tier B =================

    @Test
    fun `processTierB returns Completed with the genuine result count for a real, successful OCR recognition`() = runTest {
        val scriptDir = Files.createTempDirectory("evidence-ui-adapter-scripts")
        val recognisedJson = """{"status":"recognised","recognisedText":"ADAPTER TEST TEXT","fidelity":"VERBATIM","mechanismVersion":"fake-1.0.0"}"""
        val runtime = ParkerRuntime(config(doclingBridgeScriptPath = writeFakeBridgeScript(scriptDir, 0, recognisedJson).toString()), RecordingParkerLogger())
        runtime.start()
        val adapter = adapterFor(runtime)
        val scanned = assertIs<EvidenceImportOutcome.Imported>(adapter.importFile(fixtureRoot.resolve("03-scanned.pdf").toAbsolutePath().toString(), "application/pdf"))
        assertEquals(TierAProcessingOutcome.RequiresTierB, adapter.processTierA(scanned.evidenceArtifactId))

        val outcome = adapter.processTierB(scanned.evidenceArtifactId)

        assertEquals(TierBProcessingOutcome.Completed(1), outcome)

        runtime.shutdown()
    }

    @Test
    fun `processTierA never invokes the OCR bridge for a governed, non-OCR-eligible format`() = runTest {
        val scriptDir = Files.createTempDirectory("evidence-ui-adapter-scripts")
        val marker = scriptDir.resolve("invoked.marker")
        val scriptPath = Files.createTempFile(scriptDir, "fake-docling-bridge-", ".sh")
        Files.writeString(scriptPath, "#!/bin/sh\ntouch '${marker.toAbsolutePath()}'\nexit 0\n")
        scriptPath.toFile().setExecutable(true)
        val runtime = ParkerRuntime(config(doclingBridgeScriptPath = scriptPath.toString()), RecordingParkerLogger())
        runtime.start()
        val adapter = adapterFor(runtime)

        val csv = assertIs<EvidenceImportOutcome.Imported>(adapter.importFile(fixtureRoot.resolve("06-structured.csv").toAbsolutePath().toString(), "text/csv"))
        assertEquals(TierAProcessingOutcome.Admitted("CSV"), adapter.processTierA(csv.evidenceArtifactId))

        assertTrue(Files.notExists(marker), "Tier A processing of a governed, non-OCR-eligible format must never invoke the OCR bridge")

        runtime.shutdown()
    }

    // ================= Structural: owner-only, no caller-supplied principal =================

    @Test
    fun `processTierB declares no requestingPrincipalId or PrincipalId parameter -- structurally always the configured owner`() {
        val function = OwnerUiEvidenceRuntimeAdapter::class.declaredFunctions.single { it.name == "processTierB" }
        val valueParameterTypes = function.parameters
            .filter { it.kind == kotlin.reflect.KParameter.Kind.VALUE }
            .map { it.type.classifier }

        assertEquals(
            listOf(EvidenceArtifactId::class),
            valueParameterTypes,
            "processTierB must take exactly one parameter (the target identifier) -- no PrincipalId " +
                "parameter of any kind, so no caller of this method can ever substitute a principal " +
                "other than the configured owner",
        )
    }
}
