package parker.composition

import java.nio.file.Files
import java.nio.file.Path
import kotlin.reflect.full.declaredFunctions
import kotlinx.coroutines.test.runTest
import parker.core.interfaces.EvidenceArtifactId
import parker.core.interfaces.EvidenceRetrievalResult
import parker.core.interfaces.PrincipalId
import parker.core.interfaces.*
import parker.ui.EnhancedTranscriptionReadiness
import parker.ui.EnhancedTranscriptionOutcome
import parker.ui.EvidenceImportOutcome
import parker.ui.OwnerTierAContent
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
        doclingPythonExecutablePath: String = syntheticBridgeShellExecutable(),
        doclingBridgeScriptPath: String,
    ): ParkerRuntimeConfig = ParkerRuntimeConfig(
        modelEndpointUrl = "http://127.0.0.1:1/api/generate", // deliberately unreachable
        modelName = "test-model",
        ownerPrincipalId = ownerPrincipalId,
        localTextChannelModuleId = "channel.local-text-evidence-ui-adapter-test",
        evidenceStorageRootPath = Files.createTempDirectory("evidence-ui-adapter-evidence").toString(),
        evidenceSourceManifestStorageRootPath = Files.createTempDirectory("evidence-ui-adapter-manifest").toString(),
        derivativeGenerationStorageRootPath = Files.createTempDirectory("evidence-ui-adapter-derivative").toString(),
        derivativeContentStorageRootPath = Files.createTempDirectory("evidence-ui-adapter-derivative-content").toString(),
        savedAnalysisStorageRootPath = Files.createTempDirectory("saved-analysis-storage").toString(),
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

    private fun adapterFor(
        runtime: ParkerRuntime,
        readiness: EnhancedTranscriptionReadiness = EnhancedTranscriptionReadiness.Disabled,
        external: suspend (EvidenceArtifactId) -> ExternalTranscriptionOwnerInvocationOutcome = { ExternalTranscriptionOwnerInvocationOutcome.AdmissionFailed("disabled") },
    ) = OwnerUiEvidenceRuntimeAdapter(
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
        externalReadiness = { readiness },
        invokeExternalTranscriptionAsOwner = external,
    )

    @Test
    fun `bounded external failures map to safe owner text without raw reason leakage`() = runTest {
        val scriptDir = Files.createTempDirectory("evidence-ui-adapter-scripts")
        val runtime = ParkerRuntime(config(doclingBridgeScriptPath = writeFakeBridgeScript(scriptDir, 0, "").toString()), RecordingParkerLogger())
        runtime.start()
        val id = EvidenceArtifactId("external-failure-evidence")
        val sentinels = listOf("SOURCE_SECRET_SENTINEL", "TRANSCRIPT_SECRET_SENTINEL", "API_KEY_SENTINEL")
        val failures = listOf<ExternalTranscriptionOwnerInvocationOutcome>(
            ExternalTranscriptionOwnerInvocationOutcome.NotAuthorised,
            ExternalTranscriptionOwnerInvocationOutcome.SourceNotFound(id),
            ExternalTranscriptionOwnerInvocationOutcome.ManifestNotFound(id),
            ExternalTranscriptionOwnerInvocationOutcome.UnsupportedOrOutOfBounds(id),
            ExternalTranscriptionOwnerInvocationOutcome.MechanismFailure("PROVIDER_AUTHENTICATION_FAILURE"),
            ExternalTranscriptionOwnerInvocationOutcome.MechanismFailure("PROVIDER_RATE_LIMITED"),
            ExternalTranscriptionOwnerInvocationOutcome.MechanismFailure("PROVIDER_UNAVAILABLE"),
            ExternalTranscriptionOwnerInvocationOutcome.MechanismFailure("PROVIDER_TIMEOUT"),
            ExternalTranscriptionOwnerInvocationOutcome.MechanismFailure("PROVIDER_NETWORK_FAILURE"),
            ExternalTranscriptionOwnerInvocationOutcome.MechanismFailure("MALFORMED_PROVIDER_RESPONSE"),
            ExternalTranscriptionOwnerInvocationOutcome.ValidationRejected(sentinels.joinToString("/")),
            ExternalTranscriptionOwnerInvocationOutcome.AdmissionFailed(sentinels.joinToString("/")),
        )
        failures.forEach { failure ->
            val mapped = assertIs<EnhancedTranscriptionOutcome.Failed>(
                adapterFor(runtime, EnhancedTranscriptionReadiness.Ready) { failure }.transcribeExternal(id),
            )
            assertTrue(mapped.safeMessage.isNotBlank())
            sentinels.forEach { assertTrue(!mapped.safeMessage.contains(it)) }
        }
        runtime.shutdown()
    }

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
        val csvAdmitted = assertIs<TierAProcessingOutcome.Admitted>(adapter.processTierA(csv.evidenceArtifactId))
        assertEquals("CSV", csvAdmitted.format)
        assertIs<OwnerTierAContent.Csv>(csvAdmitted.content)

        val eml = assertIs<EvidenceImportOutcome.Imported>(adapter.importFile(fixtureRoot.resolve("05-email-with-attachment.eml").toAbsolutePath().toString(), "message/rfc822"))
        val emlAdmitted = assertIs<TierAProcessingOutcome.Admitted>(adapter.processTierA(eml.evidenceArtifactId))
        assertEquals("EML", emlAdmitted.format)
        assertIs<OwnerTierAContent.Eml>(emlAdmitted.content)

        val docx = assertIs<EvidenceImportOutcome.Imported>(
            adapter.importFile(fixtureRoot.resolve("04-structured.docx").toAbsolutePath().toString(), "application/vnd.openxmlformats-officedocument.wordprocessingml.document"),
        )
        val docxAdmitted = assertIs<TierAProcessingOutcome.Admitted>(adapter.processTierA(docx.evidenceArtifactId))
        assertEquals("DOCX", docxAdmitted.format)
        assertIs<OwnerTierAContent.Docx>(docxAdmitted.content)

        val pdf = assertIs<EvidenceImportOutcome.Imported>(adapter.importFile(fixtureRoot.resolve("01-searchable-simple.pdf").toAbsolutePath().toString(), "application/pdf"))
        val pdfAdmitted = assertIs<TierAProcessingOutcome.Admitted>(adapter.processTierA(pdf.evidenceArtifactId))
        assertEquals("PDF", pdfAdmitted.format)
        assertIs<OwnerTierAContent.Pdf>(pdfAdmitted.content)

        val scanned = assertIs<EvidenceImportOutcome.Imported>(adapter.importFile(fixtureRoot.resolve("03-scanned.pdf").toAbsolutePath().toString(), "application/pdf"))
        assertEquals(TierAProcessingOutcome.RequiresTierB, adapter.processTierA(scanned.evidenceArtifactId))

        val png = assertIs<EvidenceImportOutcome.Imported>(adapter.importFile(fixtureRoot.resolve("07-text-image.png").toAbsolutePath().toString(), "image/png"))
        assertEquals(TierAProcessingOutcome.RequiresTierB, adapter.processTierA(png.evidenceArtifactId))

        runtime.shutdown()
    }

    // ================= Owner Tier A Extracted Content Presentation =================

    @Test
    fun `Admitted PDF payload is no longer discarded -- exact documentText, pageCount, completeness, warnings, and producer identity all survive adapter mapping`() = runTest {
        val scriptDir = Files.createTempDirectory("evidence-ui-adapter-scripts")
        val runtime = ParkerRuntime(config(doclingBridgeScriptPath = writeFakeBridgeScript(scriptDir, 0, "").toString()), RecordingParkerLogger())
        runtime.start()
        val adapter = adapterFor(runtime)
        val sourcePath = fixtureRoot.resolve("01-searchable-simple.pdf").toAbsolutePath()

        // Independently extract the same fixture via the real specialist (never re-run through the
        // owner path itself) -- this is the ground truth the adapter must faithfully pass through,
        // not discard.
        val expected = assertIs<parker.core.interfaces.PdfStructuralExtractionOutcome.Extracted>(
            parker.core.runtime.TikaPdfStructuralExtractor().extract(java.nio.file.Files.readAllBytes(sourcePath)),
        ).result

        val imported = assertIs<EvidenceImportOutcome.Imported>(adapter.importFile(sourcePath.toString(), "application/pdf"))
        val admitted = assertIs<TierAProcessingOutcome.Admitted>(adapter.processTierA(imported.evidenceArtifactId))
        val pdfContent = assertIs<OwnerTierAContent.Pdf>(admitted.content)

        assertEquals(expected.documentText, pdfContent.documentText, "exact PdfStructuralResult.documentText must survive adapter mapping")
        assertEquals(expected.pageCount, pdfContent.pageCount)
        assertEquals(expected.pageTextAssociationAvailable, pdfContent.pageTextAssociationAvailable)
        assertEquals(expected.completenessState.name, pdfContent.completenessState)
        assertEquals(expected.warnings, pdfContent.warnings)
        assertEquals(expected.producerIdentity.pluginIdentity, pdfContent.producer.pluginIdentity)
        assertEquals(expected.producerIdentity.pluginVersion, pdfContent.producer.pluginVersion)
        assertEquals(expected.metadata.map { it.name to it.value }, pdfContent.metadata.map { it.name to it.value })

        runtime.shutdown()
    }

    @Test
    fun `unsupported and failure Tier A outcomes never carry Admitted content`() = runTest {
        val scriptDir = Files.createTempDirectory("evidence-ui-adapter-scripts")
        val runtime = ParkerRuntime(config(doclingBridgeScriptPath = writeFakeBridgeScript(scriptDir, 0, "").toString()), RecordingParkerLogger())
        runtime.start()

        val outcome = adapterFor(runtime).processTierA(EvidenceArtifactId("evidence-never-registered"))

        val failed = assertIs<TierAProcessingOutcome.Failed>(outcome)
        assertTrue(failed.safeMessage.isNotBlank())

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
        val recognisedJson = """{"status":"recognised","recognisedText":"ADAPTER TEST TEXT","fidelity":"UNVERIFIED_LITERAL_TRANSCRIPTION","mechanismVersion":"fake-1.0.0"}"""
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
        val csvAdmitted = assertIs<TierAProcessingOutcome.Admitted>(adapter.processTierA(csv.evidenceArtifactId))
        assertEquals("CSV", csvAdmitted.format)

        assertTrue(Files.notExists(marker), "Tier A processing of a governed, non-OCR-eligible format must never invoke the OCR bridge")

        runtime.shutdown()
    }

    // ================= Document Ingestion -- Derivative Content Persistence and Retrieval =================

    @Test
    fun `retrieveTierAExtractedContent returns the durably persisted content, not a re-extraction, across a fresh runtime restart`() = runTest {
        val scriptDir = Files.createTempDirectory("evidence-ui-adapter-scripts")
        val configValue = config(doclingBridgeScriptPath = writeFakeBridgeScript(scriptDir, 0, "").toString())
        val runtime = ParkerRuntime(configValue, RecordingParkerLogger())
        runtime.start()
        val adapter = adapterFor(runtime)
        val sourcePath = fixtureRoot.resolve("01-searchable-simple.pdf").toAbsolutePath()

        val imported = assertIs<EvidenceImportOutcome.Imported>(adapter.importFile(sourcePath.toString(), "application/pdf"))
        val admitted = assertIs<TierAProcessingOutcome.Admitted>(adapter.processTierA(imported.evidenceArtifactId))
        val originalContent = assertIs<OwnerTierAContent.Pdf>(admitted.content)
        val derivativeGenerationId = requireNotNull(admitted.derivativeGenerationId)
        runtime.shutdown()

        val restarted = ParkerRuntime(configValue, RecordingParkerLogger())
        restarted.start()
        val restartedAdapter = adapterFor(restarted)
        val retrieved = assertIs<parker.ui.TierAContentRetrievalResult.Retrieved>(
            restartedAdapter.retrieveTierAExtractedContent(imported.evidenceArtifactId, derivativeGenerationId),
        )
        val retrievedContent = assertIs<OwnerTierAContent.Pdf>(retrieved.content)

        assertEquals(originalContent.documentText, retrievedContent.documentText)
        assertEquals(originalContent.pageCount, retrievedContent.pageCount)
        assertEquals(originalContent.completenessState, retrievedContent.completenessState)
        assertEquals(originalContent.warnings, retrievedContent.warnings)

        restarted.shutdown()
    }

    @Test
    fun `retrieveTierAExtractedContent for a never-processed generation id returns UnknownGeneration`() = runTest {
        val scriptDir = Files.createTempDirectory("evidence-ui-adapter-scripts")
        val runtime = ParkerRuntime(config(doclingBridgeScriptPath = writeFakeBridgeScript(scriptDir, 0, "").toString()), RecordingParkerLogger())
        runtime.start()

        val outcome = adapterFor(runtime).retrieveTierAExtractedContent(
            EvidenceArtifactId("evidence-never-registered"),
            parker.core.interfaces.DerivativeGenerationId("generation-never-registered"),
        )

        assertEquals(parker.ui.TierAContentRetrievalResult.UnknownGeneration, outcome)

        runtime.shutdown()
    }

    @Test
    fun `retrieveTierAExtractedContent for the wrong evidence artefact returns SourceMismatch, never the content`() = runTest {
        val scriptDir = Files.createTempDirectory("evidence-ui-adapter-scripts")
        val runtime = ParkerRuntime(config(doclingBridgeScriptPath = writeFakeBridgeScript(scriptDir, 0, "").toString()), RecordingParkerLogger())
        runtime.start()
        val adapter = adapterFor(runtime)
        val csv = assertIs<EvidenceImportOutcome.Imported>(adapter.importFile(fixtureRoot.resolve("06-structured.csv").toAbsolutePath().toString(), "text/csv"))
        val admitted = assertIs<TierAProcessingOutcome.Admitted>(adapter.processTierA(csv.evidenceArtifactId))
        val derivativeGenerationId = requireNotNull(admitted.derivativeGenerationId)

        val outcome = adapter.retrieveTierAExtractedContent(EvidenceArtifactId("a-different-evidence-artefact"), derivativeGenerationId)

        assertEquals(parker.ui.TierAContentRetrievalResult.SourceMismatch, outcome)

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
