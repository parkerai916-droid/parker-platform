package parker.integration

import java.nio.file.Files
import java.nio.file.Path
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assumptions.assumeTrue
import parker.composition.ConsoleParkerLogger
import parker.composition.EvidenceIntelligenceInvocationOutcome
import parker.composition.LogLevel
import parker.composition.ParkerRuntime
import parker.composition.ParkerRuntimeConfig
import parker.core.interfaces.EvidenceAnalysisRequest
import parker.core.interfaces.EvidenceCustodian
import parker.core.interfaces.EvidenceRetrievalResult
import parker.core.interfaces.OwnerLocalFileIngressOutcome
import parker.core.interfaces.PrincipalId
import parker.core.interfaces.TierADocumentFormat
import parker.core.interfaces.TierADocumentRoutingResult
import parker.core.interfaces.TierAOwnerInvocationOutcome

/**
 * Document Ingestion Programme, Tier B Owner Routing -- the decisive live acceptance instrument
 * (Phase 14/17): the real, complete, explicit owner workflow --
 * `ParkerRuntime.importEvidenceFileAsOwner` -> `invokeTierAIngestionAsOwner` -> (`RequiresTierB`) ->
 * a separate, explicit `analyseEvidence` call -- over real fixture files, a real, provisioned
 * Docling installation, and a real `tools/docling-ocr-bridge.py` subprocess. Never
 * `DoclingOcrProviderAdapter` (or any other Unit 12/Tier B component) constructed or called
 * directly. Mirrors `OcrMechanismUnit12CompositionLiveAcceptanceTest.kt`'s own established shape
 * (env-var configuration, [LIVE_PROPERTY]-gated `assumeTrue`) and reuses its exact same
 * live-provisioning gate.
 *
 * Lives under `tests/integration`, this repository's own pre-existing detached
 * `liveModelEvaluation` source set (`build.gradle.kts`) -- deliberately not part of the ordinary
 * `test`/`check`/`build` lifecycle.
 *
 * **Provenance disclosure, mirroring `OcrMechanismUnit12CompositionLiveAcceptanceTest.kt`'s own
 * identical, already-disclosed structural constraint.** `EvidenceIntelligenceInvocationOutcome.Completed`'s
 * own constructor/property are `internal`, and `TransientOutput`'s own dispatch target
 * (`NotDispatched`) is a bare marker object carrying no text by design -- so the literal recognised
 * text is not, and cannot be, independently re-asserted through `analyseEvidence`'s own public
 * return value from this file. `mechanismIdentity`/`mechanismVersion` ("docling"/"2.121.0") are
 * independently, already verified genuinely available by `DoclingOcrProviderAdapterLiveAcceptanceTest.kt`'s
 * own live tests, against the exact same real Docling backend this file also exercises; this file's
 * own, complementary proof is that the real owner workflow genuinely reaches that backend and
 * genuinely returns a result to the caller -- not that the backend itself reports correctly (already
 * proven elsewhere).
 */
class TierBOwnerRoutingLiveAcceptanceTest {

    companion object {
        private const val LIVE_PROPERTY = "parker.ocr.docling.live.enabled"
    }

    private val ownerPrincipalId = "user.owner-tier-b-routing-live"

    private fun resolvedPythonExecutablePath(): String? =
        System.getenv("DOCLING_TEST_PYTHON")?.takeIf { it.isNotBlank() }

    private fun resolvedBridgeScriptPath(): String =
        System.getenv("DOCLING_TEST_BRIDGE_SCRIPT")?.takeIf { it.isNotBlank() }
            ?: Path.of("tools", "docling-ocr-bridge.py").toAbsolutePath().toString()

    private fun assumeLiveDoclingPrerequisitesProvisioned() {
        val pythonExecutablePath = resolvedPythonExecutablePath()
        assumeTrue(
            pythonExecutablePath != null,
            "Live Docling prerequisites are not provisioned on this machine -- missing: " +
                "DOCLING_TEST_PYTHON. Docling is never auto-discovered, guessed, or downloaded; " +
                "provision it and set this environment variable explicitly to run this instrument.",
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
        localTextChannelModuleId = "channel.local-text-tier-b-routing-live",
        evidenceStorageRootPath = Files.createTempDirectory("tier-b-live-evidence").toString(),
        evidenceSourceManifestStorageRootPath = Files.createTempDirectory("tier-b-live-manifest").toString(),
        derivativeGenerationStorageRootPath = Files.createTempDirectory("tier-b-live-derivative").toString(),
        derivativeContentStorageRootPath = Files.createTempDirectory("tier-b-live-derivative-content").toString(),
        savedAnalysisStorageRootPath = Files.createTempDirectory("saved-analysis-storage").toString(),
        documentIngestionAuditLogPath = Files.createTempDirectory("tier-b-live-ingestion-audit").resolve("audit.log").toString(),
        evidenceDeletionAuditLogPath = Files.createTempDirectory("tier-b-live-deletion-audit").resolve("audit.log").toString(),
        memoryCoreDurabilityLogPath = Files.createTempDirectory("tier-b-live-memory").resolve("memory-core.log").toString(),
        knowledgeItemDurabilityLogPath = Files.createTempDirectory("tier-b-live-knowledge").resolve("items.log").toString(),
        doclingPythonExecutablePath = resolvedPythonExecutablePath() ?: "python3",
        doclingBridgeScriptPath = resolvedBridgeScriptPath(),
        doclingModelCacheDir = System.getenv("DOCLING_TEST_MODEL_CACHE_DIR")?.takeIf { it.isNotBlank() },
        doclingTimeoutMillis = 120_000L,
        logLevel = LogLevel.ERROR,
    )

    private val fixtureRoot: Path = Path.of("tests", "fixtures", "document-ingestion-bakeoff", "fixtures")

    @Suppress("UNCHECKED_CAST")
    private fun evidenceCustodianOf(runtime: ParkerRuntime): EvidenceCustodian {
        val field = runtime::class.java.declaredFields.first { it.name == "evidenceCustodian" }
        field.isAccessible = true
        return field.get(runtime) as EvidenceCustodian
    }

    private fun logger() = ConsoleParkerLogger(component = "tier-b-routing-live", minLevel = LogLevel.ERROR)

    @Test
    fun `real owner workflow -- scanned PDF fixture 03 -- import, Tier A RequiresTierB, explicit Tier B, real Docling, Completed returned`() = runTest {
        assumeTrue(System.getProperty(LIVE_PROPERTY) == "true", "Live Docling property absent; no subprocess invoked")
        assumeLiveDoclingPrerequisitesProvisioned()

        val runtimeConfig = config()
        val runtime = ParkerRuntime(runtimeConfig, logger())
        runtime.start()
        val principal = PrincipalId(ownerPrincipalId)
        val originalBytes = Files.readAllBytes(fixtureRoot.resolve("03-scanned.pdf"))

        val imported = assertIs<OwnerLocalFileIngressOutcome.Accepted>(
            runtime.importEvidenceFileAsOwner(fixtureRoot.resolve("03-scanned.pdf").toAbsolutePath().toString(), "application/pdf"),
        )
        val evidenceArtifactId = imported.acceptedEvidenceArtifact.evidenceArtifactId

        val routed = assertIs<TierAOwnerInvocationOutcome.Routed>(runtime.invokeTierAIngestionAsOwner(evidenceArtifactId))
        assertIs<TierADocumentRoutingResult.RequiresTierB>(routed.result)

        val start = System.nanoTime()
        val outcome = runtime.analyseEvidence(
            principal,
            EvidenceAnalysisRequest(
                analysisKind = "ocr-transcription",
                requestingPrincipalId = principal,
                evidenceArtifactIds = listOf(evidenceArtifactId),
            ),
        )
        val elapsedMillis = (System.nanoTime() - start) / 1_000_000
        println("Tier B owner routing live PDF: elapsed=${elapsedMillis}ms, outcome=${outcome::class.simpleName}")

        assertIs<EvidenceIntelligenceInvocationOutcome.Completed>(
            outcome,
            "the real, explicit owner Tier B workflow must RETURN a Completed outcome to the caller",
        )
        assertTrue(elapsedMillis > 1000, "real Docling recognition over a real fixture is not near-instantaneous (elapsed=${elapsedMillis}ms)")

        val retrieved = assertIs<EvidenceRetrievalResult.Found>(evidenceCustodianOf(runtime).retrieve(principal, evidenceArtifactId))
        assertTrue(originalBytes.contentEquals(retrieved.content), "the custodied source must remain byte-unchanged after the real Tier B invocation")

        // No Memory Core, Knowledge, or DerivativeGenerationRecord side effect: a TransientOutput
        // dispatches to NotDispatched, which never touches any of these three durability surfaces.
        assertEquals(0L, Files.size(Path.of(runtimeConfig.memoryCoreDurabilityLogPath)), "no Memory Core write may occur from a successful Tier B result")
        assertEquals(0L, Files.size(Path.of(runtimeConfig.knowledgeItemDurabilityLogPath)), "no Knowledge write may occur from a successful Tier B result")
        // FileSystemDerivativeGenerationStorage's own constructor always creates ".tmp"/".prepared"
        // internal staging subdirectories, regardless of any record ever being written -- filtered
        // out here; an actual persisted DerivativeGenerationRecord lives directly under storageRoot.
        Files.newDirectoryStream(Path.of(runtimeConfig.derivativeGenerationStorageRootPath)).use { stream ->
            val unexpected = stream.filterNot { it.fileName.toString().startsWith(".") }
            assertTrue(unexpected.isEmpty(), "no DerivativeGenerationRecord may be created from a successful Tier B result -- found: $unexpected")
        }

        runtime.shutdown()
    }

    @Test
    fun `real owner workflow -- PNG fixture 07 -- import, Tier A RequiresTierB, explicit Tier B, real Docling, Completed returned`() = runTest {
        assumeTrue(System.getProperty(LIVE_PROPERTY) == "true", "Live Docling property absent; no subprocess invoked")
        assumeLiveDoclingPrerequisitesProvisioned()

        val runtime = ParkerRuntime(config(), logger())
        runtime.start()
        val principal = PrincipalId(ownerPrincipalId)
        val originalBytes = Files.readAllBytes(fixtureRoot.resolve("07-text-image.png"))

        val imported = assertIs<OwnerLocalFileIngressOutcome.Accepted>(
            runtime.importEvidenceFileAsOwner(fixtureRoot.resolve("07-text-image.png").toAbsolutePath().toString(), "image/png"),
        )
        val evidenceArtifactId = imported.acceptedEvidenceArtifact.evidenceArtifactId

        val routed = assertIs<TierAOwnerInvocationOutcome.Routed>(runtime.invokeTierAIngestionAsOwner(evidenceArtifactId))
        assertIs<TierADocumentRoutingResult.RequiresTierB>(routed.result)

        val start = System.nanoTime()
        val outcome = runtime.analyseEvidence(
            principal,
            EvidenceAnalysisRequest(
                analysisKind = "ocr-transcription",
                requestingPrincipalId = principal,
                evidenceArtifactIds = listOf(evidenceArtifactId),
            ),
        )
        val elapsedMillis = (System.nanoTime() - start) / 1_000_000
        println("Tier B owner routing live PNG: elapsed=${elapsedMillis}ms, outcome=${outcome::class.simpleName}")

        assertIs<EvidenceIntelligenceInvocationOutcome.Completed>(outcome)
        assertTrue(elapsedMillis > 1000, "real Docling recognition over a real fixture is not near-instantaneous (elapsed=${elapsedMillis}ms)")

        val retrieved = assertIs<EvidenceRetrievalResult.Found>(evidenceCustodianOf(runtime).retrieve(principal, evidenceArtifactId))
        assertTrue(originalBytes.contentEquals(retrieved.content), "the custodied source must remain byte-unchanged after the real Tier B invocation")

        runtime.shutdown()
    }

    @Test
    fun `real owner workflow -- complete seven-fixture matrix -- correct route for every fixture, real Docling for 03 and 07, source unchanged`() = runTest {
        assumeTrue(System.getProperty(LIVE_PROPERTY) == "true", "Live Docling property absent; no subprocess invoked")
        assumeLiveDoclingPrerequisitesProvisioned()

        val runtime = ParkerRuntime(config(), logger())
        runtime.start()
        val principal = PrincipalId(ownerPrincipalId)

        data class Case(val file: String, val mediaType: String, val expectedFormat: TierADocumentFormat?)
        val cases = listOf(
            Case("01-searchable-simple.pdf", "application/pdf", TierADocumentFormat.PDF),
            Case("02-multicolumn-complex.pdf", "application/pdf", TierADocumentFormat.PDF),
            Case("03-scanned.pdf", "application/pdf", null),
            Case("04-structured.docx", "application/vnd.openxmlformats-officedocument.wordprocessingml.document", TierADocumentFormat.DOCX),
            Case("05-email-with-attachment.eml", "message/rfc822", TierADocumentFormat.EML),
            Case("06-structured.csv", "text/csv", TierADocumentFormat.CSV),
            Case("07-text-image.png", "image/png", null),
        )

        var tierBSuccesses = 0
        cases.forEach { case ->
            val originalBytes = Files.readAllBytes(fixtureRoot.resolve(case.file))
            val imported = assertIs<OwnerLocalFileIngressOutcome.Accepted>(
                runtime.importEvidenceFileAsOwner(fixtureRoot.resolve(case.file).toAbsolutePath().toString(), case.mediaType),
            )
            val evidenceArtifactId = imported.acceptedEvidenceArtifact.evidenceArtifactId
            val routed = assertIs<TierAOwnerInvocationOutcome.Routed>(runtime.invokeTierAIngestionAsOwner(evidenceArtifactId))

            if (case.expectedFormat != null) {
                val admitted = assertIs<TierADocumentRoutingResult.Admitted>(routed.result, "expected Admitted for ${case.file}")
                assertEquals(case.expectedFormat, admitted.format, "wrong Tier A format for ${case.file}")
            } else {
                assertIs<TierADocumentRoutingResult.RequiresTierB>(routed.result, "expected RequiresTierB for ${case.file}")
                val outcome = runtime.analyseEvidence(
                    principal,
                    EvidenceAnalysisRequest(analysisKind = "ocr-transcription", requestingPrincipalId = principal, evidenceArtifactIds = listOf(evidenceArtifactId)),
                )
                assertIs<EvidenceIntelligenceInvocationOutcome.Completed>(outcome, "expected a real, Completed Tier B result for ${case.file}")
                tierBSuccesses++
            }

            val retrieved = assertIs<EvidenceRetrievalResult.Found>(evidenceCustodianOf(runtime).retrieve(principal, evidenceArtifactId))
            assertTrue(originalBytes.contentEquals(retrieved.content), "source bytes must remain unchanged for ${case.file}")
        }

        assertEquals(2, tierBSuccesses, "exactly fixtures 03 and 07 must reach a successful, explicit, real-Docling Tier B result")

        runtime.shutdown()
    }
}
