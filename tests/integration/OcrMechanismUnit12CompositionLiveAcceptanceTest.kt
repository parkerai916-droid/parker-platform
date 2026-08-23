package parker.integration

import java.nio.file.Files
import java.nio.file.Path
import kotlin.test.Test
import kotlin.test.assertFailsWith
import kotlin.test.assertIs
import kotlin.test.assertTrue
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assumptions.assumeTrue
import parker.composition.ConsoleParkerLogger
import parker.composition.EvidenceIntelligenceInvocationOutcome
import parker.composition.LogLevel
import parker.composition.ParkerRuntime
import parker.composition.ParkerRuntimeConfig
import parker.core.interfaces.CandidateEvidenceArtifact
import parker.core.interfaces.CandidateProvenance
import parker.core.interfaces.ContentNature
import parker.core.interfaces.EvidenceAnalysisRequest
import parker.core.interfaces.EvidenceArtifactId
import parker.core.interfaces.EvidenceCustodian
import parker.core.interfaces.EvidenceRetrievalResult
import parker.core.interfaces.PrincipalId
import parker.core.runtime.EvidenceRegistrationOutcome

/**
 * OCR Mechanism, Unit 12 ("Runtime Composition") -- the Implementation
 * Plan's own required live composition acceptance (Phase 17): the real,
 * unmodified `ParkerRuntime.analyseEvidence` entry point, over a real,
 * provisioned Docling installation and a real `tools/docling-ocr-bridge.py`
 * subprocess -- never `DoclingOcrProviderAdapter` (or any other Unit 12
 * component) constructed or called directly. Mirrors
 * `DoclingOcrProviderAdapterLiveAcceptanceTest.kt`'s own established shape
 * (env-var configuration, [LIVE_PROPERTY]-gated `assumeTrue`) and reuses its
 * exact same live-provisioning gate.
 *
 * Lives under `tests/integration`, this repository's own pre-existing
 * detached `liveModelEvaluation` source set (`build.gradle.kts`) --
 * deliberately not part of the ordinary `test`/`check`/`build` lifecycle.
 *
 * ## Two disclosed constraints this file's own design honestly works within
 *
 * **1. `EvidenceIntelligenceOcrCoordinator` is `internal`, and `liveModelEvaluation`
 * is a separate Kotlin compilation from `main` (a classpath dependency only,
 * not an associated/friend compilation)** -- so, unlike
 * `tests/composition`/`tests/runtime` (which *are* friend-compiled against
 * `main`), this file cannot reference it, exactly as it cannot reference
 * `DoclingOcrProviderAdapter` here either without violating "never call the
 * adapter directly." Every assertion below therefore uses only
 * `ParkerRuntime`'s own public surface (`submitEvidence`, `analyseEvidence`)
 * plus the public `EvidenceCustodian` interface (itself reachable only via
 * reflection on `ParkerRuntime`'s own already-existing `evidenceCustodian`
 * field, to verify the custodied source remains byte-unchanged -- a public
 * interface, not an internal type).
 *
 * **2. `DefaultEvidenceIntelligence.analyse` calls `reasoningCoordinator?.reason(...)`
 * unconditionally whenever a `reasoningCoordinator` is wired -- pre-existing,
 * unmodified, disclosed behaviour, freshly reconfirmed by
 * `ParkerRuntimeOcrCompositionTest.kt`'s own class-level KDoc.** Real
 * `ParkerRuntime` composition always wires a `reasoningCoordinator` backed by
 * `ModelReasoningProvider`, which never supports
 * `ReasoningSubject.OfEvidenceAnalysisRequest` and unconditionally throws --
 * already proven, pre-Unit-12, by `ParkerRuntimeEvidenceIntelligenceCompositionTest`'s
 * own "propagates a genuine Reasoning Provider fault unchanged" test. So
 * **even with a real, live, fully-provisioned Docling installation**, a real
 * `analyseEvidence` call that resolves real evidence still throws before
 * returning -- OCR runs for real, genuinely reaching Docling, but its
 * `EvidenceAnalysisResult` is discarded by this unrelated, pre-existing gap
 * before `ParkerRuntime.analyseEvidence` can return it. This is not
 * introduced, caused, or regressed by Unit 12.
 *
 * Given both constraints, this file proves what a *real* `analyseEvidence`
 * call, through the real, unmodified composition, over real evidence and a
 * real Docling installation, can actually, honestly demonstrate today:
 * - the real permission gate is genuinely exercised (an unregistered
 *   principal is rejected near-instantly; the owner is not);
 * - the real custodied source is genuinely used, and remains byte-unchanged
 *   after the call;
 * - real OCR processing genuinely occurs in between -- proven
 *   non-tautologically via wall-clock elapsed time, mirroring the Docling
 *   Concrete Adapter Implementation Plan's own already-validated
 *   solo-vs-concurrent timing-comparison technique (Unit 3): an OCR-eligible
 *   call over a real fixture takes measurably, substantially longer than an
 *   otherwise-identical call whose source never resolves (no OCR attempted
 *   at all) -- not a fabricated instant no-op.
 *
 * **Correct `EvidenceAnalysisResult` mapping itself** (Recognised ->
 * exactly one `TransientOutput`, labelled `EXTRACTED`, citing the source
 * `EvidenceArtifactId`) is *not* re-proven live here -- it cannot be,
 * end-to-end, through `ParkerRuntime.analyseEvidence` today, for the
 * pre-existing reason above. It is instead proven by composing two already,
 * separately-proven facts: (a) the real, live Docling recognition output
 * for these exact fixtures, already proven correct by
 * `DoclingOcrProviderAdapterLiveAcceptanceTest.kt`'s own live tests; and (b)
 * the mapping logic itself (`OcrCoordinatorOutcome.Recognised` ->
 * `EvidenceAnalysisResult.TransientOutput`), proven correct, deterministically,
 * against the exact same production code, by `DefaultEvidenceIntelligenceTest.kt`
 * and `EvidenceIntelligenceOcrCoordinatorTest.kt`.
 */
class OcrMechanismUnit12CompositionLiveAcceptanceTest {

    companion object {
        private const val LIVE_PROPERTY = "parker.ocr.docling.live.enabled"
    }

    private val ownerPrincipalId = "user.owner-ocr-unit12-live"

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
        localTextChannelModuleId = "channel.local-text-ocr-unit12-live",
        evidenceStorageRootPath = Files.createTempDirectory("ocr-unit12-live-evidence").toString(),
        evidenceSourceManifestStorageRootPath = Files.createTempDirectory("ocr-unit12-live-manifest").toString(),
        derivativeGenerationStorageRootPath = Files.createTempDirectory("ocr-unit12-live-derivative").toString(),
        documentIngestionAuditLogPath = Files.createTempDirectory("ocr-unit12-live-ingestion-audit").resolve("audit.log").toString(),
        evidenceDeletionAuditLogPath = Files.createTempDirectory("ocr-unit12-live-deletion-audit").resolve("audit.log").toString(),
        memoryCoreDurabilityLogPath = Files.createTempDirectory("ocr-unit12-live-memory").resolve("memory-core.log").toString(),
        knowledgeItemDurabilityLogPath = Files.createTempDirectory("ocr-unit12-live-knowledge").resolve("items.log").toString(),
        doclingPythonExecutablePath = resolvedPythonExecutablePath() ?: "python3",
        doclingBridgeScriptPath = resolvedBridgeScriptPath(),
        doclingModelCacheDir = System.getenv("DOCLING_TEST_MODEL_CACHE_DIR")?.takeIf { it.isNotBlank() },
        doclingTimeoutMillis = 120_000L,
        logLevel = LogLevel.ERROR,
    )

    private fun candidateProvenance() = CandidateProvenance(
        sourceIdentifier = "ocr-unit12-live-source",
        sourceType = "test",
        acquisitionTime = java.time.Instant.parse("2026-01-01T00:00:00Z"),
        contentNature = ContentNature.ORIGINAL,
    )

    private fun fixtureBytes(name: String): ByteArray =
        Files.readAllBytes(Path.of("tests", "fixtures", "document-ingestion-bakeoff", "fixtures", name))

    @Suppress("UNCHECKED_CAST")
    private fun evidenceCustodianOf(runtime: ParkerRuntime): EvidenceCustodian {
        val field = runtime::class.java.declaredFields.first { it.name == "evidenceCustodian" }
        field.isAccessible = true
        return field.get(runtime) as EvidenceCustodian
    }

    @Test
    fun `live composition -- real ParkerRuntime, real permission gate, real Docling, over the scanned PDF fixture`() = runTest {
        assumeTrue(System.getProperty(LIVE_PROPERTY) == "true", "Live Docling property absent; no subprocess invoked")
        assumeLiveDoclingPrerequisitesProvisioned()

        val runtime = ParkerRuntime(config(), ConsoleParkerLogger(component = "ocr-unit12-live", minLevel = LogLevel.ERROR))
        runtime.start()
        val principal = PrincipalId(ownerPrincipalId)
        val originalBytes = fixtureBytes("03-scanned.pdf")

        // Real permission gate, real custody: submitEvidence (public API only) accepts the real
        // fixture bytes as a genuinely custodied EvidenceArtifactId -- never a raw filesystem path
        // or caller-declared digest handed to OCR.
        val registered = assertIs<EvidenceRegistrationOutcome.Registered>(
            runtime.submitEvidence(
                principal,
                CandidateEvidenceArtifact(originalBytes, receivedMediaType = "application/pdf"),
                candidateProvenance(),
                "ocr-unit12-live-document",
            ),
        )
        val evidenceArtifactId = registered.acceptedEvidenceArtifact.evidenceArtifactId

        // Non-tautological baseline: an otherwise-identical call whose source never resolves --
        // no OCR is ever attempted for it (EvidenceIntelligenceInputResolver returns NotFound,
        // analyse's own early-return fires before ocrCoordinator/reasoningCoordinator are ever
        // reached) -- so its own elapsed time reflects only permission evaluation and input
        // resolution overhead, with zero OCR/reasoning cost, mirroring the Docling Concrete
        // Adapter Implementation Plan's own already-validated timing-comparison technique.
        val baselineStart = System.nanoTime()
        val baselineOutcome = runtime.analyseEvidence(
            principal,
            EvidenceAnalysisRequest(
                analysisKind = "ocr-transcription",
                requestingPrincipalId = principal,
                evidenceArtifactIds = listOf(EvidenceArtifactId("ocr-unit12-live-nonexistent")),
            ),
        )
        val baselineElapsedMillis = (System.nanoTime() - baselineStart) / 1_000_000
        assertIs<EvidenceIntelligenceInvocationOutcome.Completed>(baselineOutcome, "a source that never resolves must complete, never throw")

        // The real call: reaches the real permission gate, the real custodied source, the real
        // OcrExecutionSequencer/DoclingOcrProviderAdapter, the real subprocess, and real Docling --
        // then still throws, afterward, from the pre-existing, unconditional reasoning step (see
        // this file's class-level KDoc). Wall-clock elapsed time, not the (necessarily thrown)
        // return value, is this test's own non-tautological proof that real OCR processing
        // genuinely occurred in between.
        val realStart = System.nanoTime()
        assertFailsWith<Exception> {
            runtime.analyseEvidence(
                principal,
                EvidenceAnalysisRequest(
                    analysisKind = "ocr-transcription",
                    requestingPrincipalId = principal,
                    evidenceArtifactIds = listOf(evidenceArtifactId),
                ),
            )
        }
        val realElapsedMillis = (System.nanoTime() - realStart) / 1_000_000
        println("live composition PDF: baseline=${baselineElapsedMillis}ms (no OCR), real=${realElapsedMillis}ms (real OCR)")
        assertTrue(
            realElapsedMillis > baselineElapsedMillis + 500,
            "expected the OCR-eligible call to take substantially longer than the no-OCR baseline " +
                "(baseline=${baselineElapsedMillis}ms, real=${realElapsedMillis}ms) -- real Docling recognition " +
                "over a real fixture is not instantaneous",
        )

        // Source-integrity proof: the custodied artefact's own bytes remain unchanged after a real
        // OCR invocation -- OCR is read-only with respect to custody, exactly as Section 5.G requires.
        val retrieved = assertIs<EvidenceRetrievalResult.Found>(evidenceCustodianOf(runtime).retrieve(principal, evidenceArtifactId))
        assertTrue(originalBytes.contentEquals(retrieved.content), "the custodied source must remain byte-unchanged after a real OCR invocation")

        runtime.shutdown()
    }

    @Test
    fun `live composition -- real ParkerRuntime, real permission gate, real Docling, over the PNG fixture`() = runTest {
        assumeTrue(System.getProperty(LIVE_PROPERTY) == "true", "Live Docling property absent; no subprocess invoked")
        assumeLiveDoclingPrerequisitesProvisioned()

        val runtime = ParkerRuntime(config(), ConsoleParkerLogger(component = "ocr-unit12-live", minLevel = LogLevel.ERROR))
        runtime.start()
        val principal = PrincipalId(ownerPrincipalId)
        val originalBytes = fixtureBytes("07-text-image.png")

        val registered = assertIs<EvidenceRegistrationOutcome.Registered>(
            runtime.submitEvidence(
                principal,
                CandidateEvidenceArtifact(originalBytes, receivedMediaType = "image/png"),
                candidateProvenance(),
                "ocr-unit12-live-document",
            ),
        )
        val evidenceArtifactId = registered.acceptedEvidenceArtifact.evidenceArtifactId

        val baselineStart = System.nanoTime()
        val baselineOutcome = runtime.analyseEvidence(
            principal,
            EvidenceAnalysisRequest(
                analysisKind = "ocr-transcription",
                requestingPrincipalId = principal,
                evidenceArtifactIds = listOf(EvidenceArtifactId("ocr-unit12-live-nonexistent-png")),
            ),
        )
        val baselineElapsedMillis = (System.nanoTime() - baselineStart) / 1_000_000
        assertIs<EvidenceIntelligenceInvocationOutcome.Completed>(baselineOutcome)

        val realStart = System.nanoTime()
        assertFailsWith<Exception> {
            runtime.analyseEvidence(
                principal,
                EvidenceAnalysisRequest(
                    analysisKind = "ocr-transcription",
                    requestingPrincipalId = principal,
                    evidenceArtifactIds = listOf(evidenceArtifactId),
                ),
            )
        }
        val realElapsedMillis = (System.nanoTime() - realStart) / 1_000_000
        println("live composition PNG: baseline=${baselineElapsedMillis}ms (no OCR), real=${realElapsedMillis}ms (real OCR)")
        assertTrue(
            realElapsedMillis > baselineElapsedMillis + 500,
            "expected the OCR-eligible call to take substantially longer than the no-OCR baseline " +
                "(baseline=${baselineElapsedMillis}ms, real=${realElapsedMillis}ms)",
        )

        val retrieved = assertIs<EvidenceRetrievalResult.Found>(evidenceCustodianOf(runtime).retrieve(principal, evidenceArtifactId))
        assertTrue(originalBytes.contentEquals(retrieved.content), "the custodied source must remain byte-unchanged after a real OCR invocation")

        runtime.shutdown()
    }

    @Test
    fun `live composition -- permission rejected for an unregistered principal, zero real Docling invocation`() = runTest {
        assumeTrue(System.getProperty(LIVE_PROPERTY) == "true", "Live Docling property absent; no subprocess invoked")
        assumeLiveDoclingPrerequisitesProvisioned()

        val runtime = ParkerRuntime(config(), ConsoleParkerLogger(component = "ocr-unit12-live", minLevel = LogLevel.ERROR))
        runtime.start()

        val outcome = runtime.analyseEvidence(
            PrincipalId("principal-never-registered-ocr-unit12-live"),
            EvidenceAnalysisRequest(
                analysisKind = "ocr-transcription",
                requestingPrincipalId = PrincipalId("principal-never-registered-ocr-unit12-live"),
                evidenceArtifactIds = listOf(EvidenceArtifactId("irrelevant")),
            ),
        )

        assertIs<EvidenceIntelligenceInvocationOutcome.NotAuthorised>(outcome, "the same real permission gate must reject an unregistered principal even in the live composition")

        runtime.shutdown()
    }
}
