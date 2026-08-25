package parker.integration

import java.nio.file.Files
import java.nio.file.Path
import java.util.concurrent.atomic.AtomicInteger
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assumptions.assumeTrue
import parker.core.interfaces.EvidenceArtifactId
import parker.core.interfaces.OcrRecognitionOutcome
import parker.core.interfaces.OcrRecognitionRequest
import parker.core.runtime.DoclingOcrProviderAdapter
import parker.core.runtime.DoclingOcrProviderAdapterConfiguration
import parker.core.runtime.ProcessBuilderDoclingSubprocessInvoker

/**
 * OCR Mechanism, Docling Concrete Adapter -- this Unit's own required
 * "genuine live integration test" (mirroring
 * `QmdRelevanceMechanismLiveAcceptanceTest.kt`'s own established shape
 * exactly, Section 19/§23 of both the Bridge Contract and the Adapter Plan):
 * runs the real, production `DoclingOcrProviderAdapter` against a real,
 * provisioned Docling installation and a real `tools/docling-ocr-bridge.py`
 * subprocess, over the immutable document-ingestion bake-off fixtures.
 *
 * Lives under `tests/integration`, this repository's own pre-existing
 * detached `liveModelEvaluation` source set (`build.gradle.kts`) --
 * deliberately NOT part of the ordinary `test`/`check`/`build` lifecycle.
 * Gated by [LIVE_PROPERTY]: with the property absent, every test below is
 * skipped via `assumeTrue`, not failed.
 *
 * Run explicitly, from a machine with Docling provisioned into an isolated
 * Python environment, via:
 * ```
 * ./gradlew doclingOcrProviderAdapterLiveAcceptance \
 *   -DDOCLING_TEST_PYTHON=/home/steve/docling-venv/bin/python3
 * ```
 * (task registered in `build.gradle.kts`).
 *
 * Configuration (Python executable, bridge script path, model cache
 * directory) is read from environment variables, mirroring
 * `QmdRelevanceMechanismLiveAcceptanceTest.kt`'s own identical discipline
 * -- never a fabricated, machine-specific fallback, except the bridge
 * script's own repository-relative path (this file's own default, since
 * that path is not deployment-specific -- it is this repository's own
 * committed file).
 */
class DoclingOcrProviderAdapterLiveAcceptanceTest {

    companion object {
        private const val LIVE_PROPERTY = "parker.ocr.docling.live.enabled"
    }

    private fun resolvedPythonExecutablePath(): String? =
        System.getenv("DOCLING_TEST_PYTHON")?.takeIf { it.isNotBlank() }

    private fun resolvedBridgeScriptPath(): String =
        System.getenv("DOCLING_TEST_BRIDGE_SCRIPT")?.takeIf { it.isNotBlank() }
            ?: Path.of("tools", "docling-ocr-bridge.py").toAbsolutePath().toString()

    /**
     * Explicit, diagnosable skip -- never a fabricated path -- when this
     * machine has no provisioned Docling environment. A real, lightweight
     * subprocess probe (`python -c "import docling"`), not merely a file-
     * existence check, since the genuinely load-bearing prerequisite is
     * "Docling is importable in this interpreter," not "some python3
     * executable exists somewhere."
     */
    private fun assumeLiveDoclingPrerequisitesProvisioned() {
        val pythonExecutablePath = resolvedPythonExecutablePath()
        assumeTrue(
            pythonExecutablePath != null,
            "Live Docling prerequisites are not provisioned on this machine -- missing: " +
                "DOCLING_TEST_PYTHON (the Python executable inside a provisioned Docling virtual " +
                "environment). Docling is never auto-discovered, guessed, or downloaded; provision it and " +
                "set this environment variable explicitly to run this live acceptance instrument.",
        )
        val probe = try {
            ProcessBuilder(listOf(pythonExecutablePath!!, "-c", "import docling")).start().apply { waitFor() }.exitValue() == 0
        } catch (_: Exception) {
            false
        }
        assumeTrue(probe, "DOCLING_TEST_PYTHON=$pythonExecutablePath cannot import the docling package -- skipping")
    }

    private fun liveConfiguration(): DoclingOcrProviderAdapterConfiguration {
        val pythonExecutablePath = resolvedPythonExecutablePath()!!
        val bridgeScriptPath = resolvedBridgeScriptPath()
        val modelCacheDir = System.getenv("DOCLING_TEST_MODEL_CACHE_DIR")?.takeIf { it.isNotBlank() }
        return DoclingOcrProviderAdapterConfiguration(
            pythonExecutablePath = pythonExecutablePath,
            bridgeScriptPath = bridgeScriptPath,
            modelCacheDir = modelCacheDir,
            timeoutMillis = 120_000,
        )
    }

    private fun fixtureBytes(name: String): ByteArray =
        Files.readAllBytes(Path.of("tests", "fixtures", "document-ingestion-bakeoff", "fixtures", name))

    /**
     * Deterministically generates a synthetic, N-page PDF via Pillow (an
     * already-provisioned Docling transitive dependency, reused rather than
     * adding a new one) -- never real evidence, never committed to the
     * repository, built fresh into [outputPath] on every call using the
     * same [pythonExecutablePath] this test's own live configuration
     * already resolves. Each page carries only a trivial "Page N of TOTAL"
     * label -- large enough for pypdfium2 to count reliably, small enough
     * to generate and store in well under a second.
     */
    private fun generateSyntheticMultiPagePdf(pythonExecutablePath: String, pageCount: Int, outputPath: Path) {
        val script = """
            from PIL import Image, ImageDraw
            pages = []
            for i in range($pageCount):
                img = Image.new("L", (200, 260), color=255)
                d = ImageDraw.Draw(img)
                d.text((10, 10), f"Page {i + 1} of $pageCount", fill=0)
                pages.append(img)
            pages[0].save(r"${outputPath.toAbsolutePath()}", save_all=True, append_images=pages[1:])
        """.trimIndent()
        val process = ProcessBuilder(listOf(pythonExecutablePath, "-c", script)).redirectErrorStream(true).start()
        val output = process.inputStream.bufferedReader().readText()
        val finished = process.waitFor(60, java.util.concurrent.TimeUnit.SECONDS)
        check(finished && process.exitValue() == 0) {
            "synthetic $pageCount-page PDF generation failed (exit=${if (finished) process.exitValue() else "timed out"}): $output"
        }
    }

    @Test
    fun `live Docling subprocess recognises the accepted scanned PDF fixture`() = runTest {
        assumeTrue(System.getProperty(LIVE_PROPERTY) == "true", "Live Docling property absent; no subprocess invoked")
        assumeLiveDoclingPrerequisitesProvisioned()

        val configuration = liveConfiguration()
        val adapter = DoclingOcrProviderAdapter(configuration, ProcessBuilderDoclingSubprocessInvoker(configuration))
        val originalBytes = fixtureBytes("03-scanned.pdf")
        val originalCopy = originalBytes.copyOf()

        val outcome = adapter.recognise(
            OcrRecognitionRequest(
                sourceEvidenceId = EvidenceArtifactId("live-acceptance-scanned-pdf"),
                content = originalBytes,
                mediaType = "application/pdf",
            ),
        )

        assertTrue(outcome is OcrRecognitionOutcome.Recognised, "expected Recognised, got: $outcome")
        val result = (outcome as OcrRecognitionOutcome.Recognised).result
        println("live Docling PDF recognition: ${result.recognisedText}")
        assertTrue(result.recognisedText.contains("SCANNED SYNTHETIC EVIDENCE"), "expected the fixture's own known text, got: ${result.recognisedText}")
        assertTrue(result.recognisedText.contains("Māori"), "expected Unicode (Māori macron) text to survive the full JSON round-trip intact")
        assertEquals("docling", result.identity.mechanismIdentity)
        assertTrue(result.identity.mechanismVersion != null, "a real Docling installation must report a genuine mechanismVersion")
        assertTrue(result.confidence != null && result.confidence!! in 0.0..1.0, "expected a genuine document-level confidence scalar")
        assertTrue(originalBytes.contentEquals(originalCopy), "source bytes must remain byte-identical after a real invocation")
    }

    @Test
    fun `live Docling subprocess recognises the accepted PNG fixture`() = runTest {
        assumeTrue(System.getProperty(LIVE_PROPERTY) == "true", "Live Docling property absent; no subprocess invoked")
        assumeLiveDoclingPrerequisitesProvisioned()

        val configuration = liveConfiguration()
        val adapter = DoclingOcrProviderAdapter(configuration, ProcessBuilderDoclingSubprocessInvoker(configuration))

        val outcome = adapter.recognise(
            OcrRecognitionRequest(
                sourceEvidenceId = EvidenceArtifactId("live-acceptance-png"),
                content = fixtureBytes("07-text-image.png"),
                mediaType = "image/png",
            ),
        )

        assertTrue(outcome is OcrRecognitionOutcome.Recognised, "expected Recognised, got: $outcome")
        val result = (outcome as OcrRecognitionOutcome.Recognised).result
        println("live Docling PNG recognition: ${result.recognisedText}")
        assertTrue(result.recognisedText.contains("PARKER TEXT IMAGE"), "expected the fixture's own known text, got: ${result.recognisedText}")
    }

    // ---- Tier B OCR Truthful Mandatory Provenance acceptance work ----

    /**
     * Runs a short Python snippet against the *same* interpreter this live
     * test's own [DOCLING_TEST_PYTHON] configuration already uses, to
     * independently compute the SHA-256 digest of the real, currently
     * bundled RapidOCR recognition-model artifact on this machine -- never
     * a hard-coded digest, so this test remains correct across a future
     * `rapidocr` pin upgrade. Deliberately re-derives the digest via a
     * wholly separate code path (a fresh subprocess, no shared Python
     * object) from the one the bridge script itself uses, so a match here
     * is a genuine independent proof, not a tautology.
     */
    private fun independentBundledRecognitionModelDigest(pythonExecutablePath: String): String {
        val script = """
            import hashlib
            from rapidocr import RapidOCR
            from rapidocr.inference_engine.base import FileInfo, InferSession
            engine = RapidOCR()
            cfg = engine.text_rec.cfg
            file_info = FileInfo(cfg.engine_type, cfg.ocr_version, cfg.task_type, cfg.lang_type, cfg.model_type)
            manifest = InferSession.get_model_url(file_info)
            from pathlib import Path
            model_path = Path(cfg.model_root_dir) / Path(manifest["model_dir"]).name
            print(hashlib.sha256(model_path.read_bytes()).hexdigest())
        """.trimIndent()
        val process = ProcessBuilder(listOf(pythonExecutablePath, "-c", script)).redirectErrorStream(false).start()
        val stdout = process.inputStream.bufferedReader().readText().trim()
        val stderr = process.errorStream.bufferedReader().readText()
        val finished = process.waitFor(60, java.util.concurrent.TimeUnit.SECONDS)
        check(finished && process.exitValue() == 0) {
            "independent digest computation failed (exit=${if (finished) process.exitValue() else "timed out"}): $stderr"
        }
        return stdout.lines().last { it.isNotBlank() }
    }

    @Test
    fun `live Docling subprocess reports verified modelIdentity+modelVersion matching an independently computed digest`() = runTest {
        assumeTrue(System.getProperty(LIVE_PROPERTY) == "true", "Live Docling property absent; no subprocess invoked")
        assumeLiveDoclingPrerequisitesProvisioned()

        val configuration = liveConfiguration()
        val adapter = DoclingOcrProviderAdapter(configuration, ProcessBuilderDoclingSubprocessInvoker(configuration))

        val outcome = adapter.recognise(
            OcrRecognitionRequest(
                sourceEvidenceId = EvidenceArtifactId("live-acceptance-provenance"),
                content = fixtureBytes("07-text-image.png"),
                mediaType = "image/png",
            ),
        )

        assertTrue(outcome is OcrRecognitionOutcome.Recognised, "expected Recognised, got: $outcome")
        val identity = (outcome as OcrRecognitionOutcome.Recognised).result.identity

        assertEquals("docling", identity.mechanismIdentity)
        assertTrue(identity.mechanismVersion != null, "mechanismVersion must remain independently populated")

        assertTrue(identity.modelIdentity != null, "expected the verified branch: modelIdentity must be populated against a real, unmodified bundled model")
        assertTrue(identity.modelVersion != null, "expected the verified branch: modelVersion must be populated")
        assertFalse(identity.modelIdentity!!.contains("/"), "modelIdentity must never contain a filesystem path")
        assertFalse(identity.modelIdentity!!.contains("unknown", ignoreCase = true))
        assertTrue(identity.modelVersion!!.matches(Regex("^sha256:[0-9a-f]{64}$")), "modelVersion must exactly match sha256:<64 lowercase hex>, got: ${identity.modelVersion}")

        val independentDigest = independentBundledRecognitionModelDigest(configuration.pythonExecutablePath)
        assertEquals("sha256:$independentDigest", identity.modelVersion, "modelVersion must equal an independently computed sha256 of the real bundled recognition-model artifact")
    }

    @Test
    fun `verified modelIdentity+modelVersion are stable across independent adapter instances -- restart stability`() = runTest {
        assumeTrue(System.getProperty(LIVE_PROPERTY) == "true", "Live Docling property absent; no subprocess invoked")
        assumeLiveDoclingPrerequisitesProvisioned()

        val configuration = liveConfiguration()
        val request = OcrRecognitionRequest(EvidenceArtifactId("live-acceptance-restart-stability"), fixtureBytes("07-text-image.png"), "image/png")

        val firstOutcome = DoclingOcrProviderAdapter(configuration, ProcessBuilderDoclingSubprocessInvoker(configuration)).recognise(request)
        val secondOutcome = DoclingOcrProviderAdapter(configuration, ProcessBuilderDoclingSubprocessInvoker(configuration)).recognise(request)

        assertTrue(firstOutcome is OcrRecognitionOutcome.Recognised)
        assertTrue(secondOutcome is OcrRecognitionOutcome.Recognised)
        val firstIdentity = (firstOutcome as OcrRecognitionOutcome.Recognised).result.identity
        val secondIdentity = (secondOutcome as OcrRecognitionOutcome.Recognised).result.identity

        assertEquals(firstIdentity.modelIdentity, secondIdentity.modelIdentity, "modelIdentity must be stable across independent subprocess invocations for unchanged model bytes")
        assertEquals(firstIdentity.modelVersion, secondIdentity.modelVersion, "modelVersion must be stable across independent subprocess invocations for unchanged model bytes")
    }

    @Test
    fun `live Docling subprocess fails closed, never a fabricated success, when the model cache is genuinely empty`() = runTest {
        // Mirrors QmdRelevanceMechanismLiveAcceptanceTest.kt's own "a missing local embedding
        // model fails closed" test exactly, one tier further along the OCR chain: an empty,
        // fresh temp directory as modelCacheDir, HF_HUB_OFFLINE already set by the adapter
        // itself -- Docling's own layout-model resolution must fail closed rather than
        // triggering an on-demand network download.
        assumeTrue(System.getProperty(LIVE_PROPERTY) == "true", "Live Docling property absent; no subprocess invoked")
        assumeLiveDoclingPrerequisitesProvisioned()

        val emptyCacheDir = Files.createTempDirectory("parker-docling-live-empty-model-cache-")
        try {
            val configuration = liveConfiguration().copy(modelCacheDir = emptyCacheDir.toAbsolutePath().toString())
            val adapter = DoclingOcrProviderAdapter(configuration, ProcessBuilderDoclingSubprocessInvoker(configuration))

            val outcome = adapter.recognise(
                OcrRecognitionRequest(
                    sourceEvidenceId = EvidenceArtifactId("live-acceptance-empty-cache"),
                    content = fixtureBytes("07-text-image.png"),
                    mediaType = "image/png",
                ),
            )

            assertTrue(
                outcome is OcrRecognitionOutcome.ProcessingOrDependencyFailure,
                "an empty modelCacheDir must fail closed as ProcessingOrDependencyFailure, never a fabricated success -- got: $outcome",
            )
        } finally {
            emptyCacheDir.toFile().deleteRecursively()
        }
    }

    @Test
    fun `live Docling subprocess maps a corrupt source to UnsupportedOrInaccessibleInput`() = runTest {
        assumeTrue(System.getProperty(LIVE_PROPERTY) == "true", "Live Docling property absent; no subprocess invoked")
        assumeLiveDoclingPrerequisitesProvisioned()

        val configuration = liveConfiguration()
        val adapter = DoclingOcrProviderAdapter(configuration, ProcessBuilderDoclingSubprocessInvoker(configuration))

        val outcome = adapter.recognise(
            OcrRecognitionRequest(
                sourceEvidenceId = EvidenceArtifactId("live-acceptance-corrupt"),
                content = "not a real pdf".toByteArray(Charsets.UTF_8),
                mediaType = "application/pdf",
            ),
        )

        assertTrue(outcome is OcrRecognitionOutcome.UnsupportedOrInaccessibleInput, "expected UnsupportedOrInaccessibleInput, got: $outcome")
    }

    // ---- Unit 3 acceptance work: real concurrency-one proof ----

    @Test
    fun `two genuine concurrent Docling invocations are serialised, never overlapping -- proven by elapsed time, not merely asserted`() {
        assumeTrue(System.getProperty(LIVE_PROPERTY) == "true", "Live Docling property absent; no subprocess invoked")
        assumeLiveDoclingPrerequisitesProvisioned()

        val configuration = liveConfiguration()
        val pdfBytes = fixtureBytes("03-scanned.pdf")
        val pngBytes = fixtureBytes("07-text-image.png")
        val pdfRequest = OcrRecognitionRequest(EvidenceArtifactId("concurrency-pdf"), pdfBytes, "application/pdf")
        val pngRequest = OcrRecognitionRequest(EvidenceArtifactId("concurrency-png"), pngBytes, "image/png")

        // Solo baselines, sequentially, using fresh adapter instances (each with its own,
        // independent Mutex) -- deliberately not the same adapter instance the concurrent
        // section below uses, so these two timings are genuinely uncontended.
        val soloPdfMs = kotlin.system.measureTimeMillis {
            runBlocking { DoclingOcrProviderAdapter(configuration, ProcessBuilderDoclingSubprocessInvoker(configuration)).recognise(pdfRequest) }
        }
        val soloPngMs = kotlin.system.measureTimeMillis {
            runBlocking { DoclingOcrProviderAdapter(configuration, ProcessBuilderDoclingSubprocessInvoker(configuration)).recognise(pngRequest) }
        }

        // One shared adapter instance (one Mutex) for the concurrent section -- this is the
        // structural condition under test.
        val sharedAdapter = DoclingOcrProviderAdapter(configuration, ProcessBuilderDoclingSubprocessInvoker(configuration))
        var pdfOutcome: OcrRecognitionOutcome? = null
        var pngOutcome: OcrRecognitionOutcome? = null
        val concurrentTotalMs = kotlin.system.measureTimeMillis {
            runBlocking {
                val a = async(Dispatchers.IO) { sharedAdapter.recognise(pdfRequest) }
                val b = async(Dispatchers.IO) { sharedAdapter.recognise(pngRequest) }
                pdfOutcome = a.await()
                pngOutcome = b.await()
            }
        }

        println("live Docling concurrency proof -- solo PDF: ${soloPdfMs}ms, solo PNG: ${soloPngMs}ms, concurrent total: ${concurrentTotalMs}ms")

        // Non-tautological signal (this task's own explicit requirement): if the mutex
        // genuinely serialises the two invocations, total concurrent wall-clock time must be
        // close to the *sum* of the two solo baselines, never close to their *max* (which is
        // what true parallel execution would produce). A generous margin (70% of the sum)
        // absorbs process-launch/model-load timing variance without weakening the actual
        // property under test -- true parallelism would land near max(solo times), typically
        // far below a 70%-of-sum threshold whenever the two solo times are comparable.
        assertTrue(
            concurrentTotalMs >= ((soloPdfMs + soloPngMs) * 0.7).toLong(),
            "concurrent total (${concurrentTotalMs}ms) should be close to the sum of solo runs " +
                "(${soloPdfMs}ms + ${soloPngMs}ms = ${soloPdfMs + soloPngMs}ms), proving serialisation -- " +
                "true parallel execution would instead land near max(${maxOf(soloPdfMs, soloPngMs)}ms)",
        )

        // Both eventually produce truthful, independent, uncorrupted results -- proving no
        // shared-temp-file collision and no cross-invocation contamination.
        assertTrue(pdfOutcome is OcrRecognitionOutcome.Recognised, "expected the PDF invocation to succeed, got: $pdfOutcome")
        assertTrue(pngOutcome is OcrRecognitionOutcome.Recognised, "expected the PNG invocation to succeed, got: $pngOutcome")
        assertTrue((pdfOutcome as OcrRecognitionOutcome.Recognised).result.recognisedText.contains("SCANNED SYNTHETIC EVIDENCE"))
        assertTrue((pngOutcome as OcrRecognitionOutcome.Recognised).result.recognisedText.contains("PARKER TEXT IMAGE"))
        assertTrue(pdfBytes.contentEquals(fixtureBytes("03-scanned.pdf")), "PDF source bytes must remain unchanged after concurrent invocations")
        assertTrue(pngBytes.contentEquals(fixtureBytes("07-text-image.png")), "PNG source bytes must remain unchanged after concurrent invocations")
    }

    // ---- Unit 3 acceptance work: real timeout proof ----

    @Test
    fun `a real Docling invocation that exceeds a deliberately short timeout is killed, cleaned up, and does not break the next invocation`() {
        assumeTrue(System.getProperty(LIVE_PROPERTY) == "true", "Live Docling property absent; no subprocess invoked")
        assumeLiveDoclingPrerequisitesProvisioned()

        val shortTimeoutConfiguration = liveConfiguration().copy(timeoutMillis = 3_000)
        val adapter = DoclingOcrProviderAdapter(shortTimeoutConfiguration, ProcessBuilderDoclingSubprocessInvoker(shortTimeoutConfiguration))

        val bridgeProcessesBefore = liveBridgeProcessCount()
        val elapsedMs = kotlin.system.measureTimeMillis {
            val outcome = runBlocking {
                adapter.recognise(OcrRecognitionRequest(EvidenceArtifactId("timeout-proof"), fixtureBytes("03-scanned.pdf"), "application/pdf"))
            }
            assertTrue(outcome is OcrRecognitionOutcome.ProcessingOrDependencyFailure, "expected a timeout ProcessingOrDependencyFailure, got: $outcome")
            assertTrue(
                (outcome as OcrRecognitionOutcome.ProcessingOrDependencyFailure).reason.contains("timed out"),
                "expected an honest 'timed out' reason, got: ${outcome.reason}",
            )
        }
        // Real wall-clock enforcement: the call must return close to the configured 3s
        // ceiling, never anywhere near the ~15-20s a genuine, uninterrupted recognition takes.
        assertTrue(elapsedMs < 10_000, "a 3s-timeout invocation took ${elapsedMs}ms -- the timeout was not wall-clock enforced against the real subprocess")

        // No lingering direct child bridge process (disclosed limitation: a full
        // process-*tree* kill is not claimed -- only that the direct child itself is gone).
        Thread.sleep(500) // brief grace period for OS-level process teardown to complete
        val bridgeProcessesAfter = liveBridgeProcessCount()
        assertTrue(
            bridgeProcessesAfter <= bridgeProcessesBefore,
            "expected no net increase in live docling-ocr-bridge.py processes after a timeout -- before=$bridgeProcessesBefore after=$bridgeProcessesAfter",
        )

        // No stray Parker-generated source temp files left behind.
        val strayTempFiles = Files.list(Path.of(System.getProperty("java.io.tmpdir"))).use { stream ->
            stream.filter { it.fileName.toString().startsWith("parker-docling-ocr-source-") }.count()
        }
        assertEquals(0L, strayTempFiles, "no parker-docling-ocr-source-* temp file may remain after a timeout")

        // The adapter/mutex is left in a genuinely usable state -- a subsequent, normal-timeout
        // invocation still succeeds truthfully.
        val recoveryOutcome = runBlocking {
            val normalConfiguration = liveConfiguration()
            DoclingOcrProviderAdapter(normalConfiguration, ProcessBuilderDoclingSubprocessInvoker(normalConfiguration))
                .recognise(OcrRecognitionRequest(EvidenceArtifactId("post-timeout-recovery"), fixtureBytes("07-text-image.png"), "image/png"))
        }
        assertTrue(recoveryOutcome is OcrRecognitionOutcome.Recognised, "expected the post-timeout recovery invocation to succeed, got: $recoveryOutcome")
    }

    /** Best-effort live process count -- `ProcessHandle` enumeration, portable, no shell-out. */
    private fun liveBridgeProcessCount(): Long =
        ProcessHandle.allProcesses()
            .filter { handle -> handle.info().commandLine().map { it.contains("docling-ocr-bridge.py") }.orElse(false) }
            .count()

    // ---- Unit 3 acceptance work: real 200/201-page boundary proof ----

    @Test
    fun `a 200-page source is eligible -- a short timeout catches it genuinely mid-OCR, never as a page-count rejection`() {
        assumeTrue(System.getProperty(LIVE_PROPERTY) == "true", "Live Docling property absent; no subprocess invoked")
        assumeLiveDoclingPrerequisitesProvisioned()

        val configuration = liveConfiguration()
        val pdfPath = Files.createTempFile("parker-docling-live-200-pages-", ".pdf")
        try {
            generateSyntheticMultiPagePdf(configuration.pythonExecutablePath, 200, pdfPath)

            // A timeout long enough to clear the page-count preflight and begin genuine
            // per-page OCR, short enough that 200 real pages cannot possibly complete --
            // proving eligibility (the page-count gate did not reject it) without waiting for
            // the (wholly impractical) full 200-page recognition to finish.
            val shortTimeoutConfiguration = configuration.copy(timeoutMillis = 20_000)
            val adapter = DoclingOcrProviderAdapter(shortTimeoutConfiguration, ProcessBuilderDoclingSubprocessInvoker(shortTimeoutConfiguration))

            val outcome = runBlocking {
                adapter.recognise(OcrRecognitionRequest(EvidenceArtifactId("200-page-eligibility"), Files.readAllBytes(pdfPath), "application/pdf"))
            }

            assertTrue(outcome is OcrRecognitionOutcome.ProcessingOrDependencyFailure, "expected a timeout failure (proving eligibility), got: $outcome")
            val reason = (outcome as OcrRecognitionOutcome.ProcessingOrDependencyFailure).reason
            assertTrue(reason.contains("timed out"), "a 200-page source must be eligible -- expected a timeout reason, never a page-count rejection, got: $reason")
            assertFalse(reason.contains("page count"), "a 200-page source must never be rejected for exceeding the page-count bound")
        } finally {
            Files.deleteIfExists(pdfPath)
        }
    }

    @Test
    fun `a 201-page source is rejected before expensive OCR, truthfully, never truncated`() {
        assumeTrue(System.getProperty(LIVE_PROPERTY) == "true", "Live Docling property absent; no subprocess invoked")
        assumeLiveDoclingPrerequisitesProvisioned()

        val configuration = liveConfiguration()
        val pdfPath = Files.createTempFile("parker-docling-live-201-pages-", ".pdf")
        try {
            generateSyntheticMultiPagePdf(configuration.pythonExecutablePath, 201, pdfPath)
            val adapter = DoclingOcrProviderAdapter(configuration, ProcessBuilderDoclingSubprocessInvoker(configuration))

            val elapsedMs = kotlin.system.measureTimeMillis {
                val outcome = runBlocking {
                    adapter.recognise(OcrRecognitionRequest(EvidenceArtifactId("201-page-rejection"), Files.readAllBytes(pdfPath), "application/pdf"))
                }
                assertTrue(outcome is OcrRecognitionOutcome.ProcessingOrDependencyFailure, "expected a resource-limit rejection, got: $outcome")
                val reason = (outcome as OcrRecognitionOutcome.ProcessingOrDependencyFailure).reason
                assertTrue(reason.contains("page count") && reason.contains("201"), "expected an honest, specific page-count rejection reason, got: $reason")
            }
            // Non-tautological "before expensive OCR" proof: the pre-flight page-count check
            // (Python import + pypdfium2 page count) completes in low single-digit seconds --
            // nowhere near the ~15-20s a single real per-page OCR call alone takes, let alone
            // 201 of them.
            assertTrue(elapsedMs < 10_000, "a 201-page rejection took ${elapsedMs}ms -- too slow to have been a genuine pre-flight rejection, before any per-page OCR work")
        } finally {
            Files.deleteIfExists(pdfPath)
        }
    }

    // ---- Unit 3 acceptance work: offline reconfirmation (re-verifies an already-provisioned
    // installation only -- never reinstalls or reprovisions anything) ----

    @Test
    fun `live Docling recognition still succeeds with HF_HUB_OFFLINE forced and no network-dependent env assumed`() {
        // A lighter-weight reconfirmation than Unit 2's own full docker --network none proof
        // (which remains the authoritative, strongest-layer evidence, documented in the
        // Bridge Contract's own addendum): this test only re-confirms that the adapter's own,
        // already-adopted applyDoclingOfflineEnvironment flags are sufficient for a real
        // invocation to succeed without any additional environment help, proving no *new*
        // environment-dependent behaviour was introduced by this Unit's own bridge-script
        // corrections.
        assumeTrue(System.getProperty(LIVE_PROPERTY) == "true", "Live Docling property absent; no subprocess invoked")
        assumeLiveDoclingPrerequisitesProvisioned()

        val configuration = liveConfiguration()
        val adapter = DoclingOcrProviderAdapter(configuration, ProcessBuilderDoclingSubprocessInvoker(configuration))

        val outcome = runBlocking {
            adapter.recognise(OcrRecognitionRequest(EvidenceArtifactId("offline-reconfirmation"), fixtureBytes("07-text-image.png"), "image/png"))
        }

        assertTrue(outcome is OcrRecognitionOutcome.Recognised, "expected Recognised under the adapter's own already-adopted offline environment flags alone, got: $outcome")
        assertTrue((outcome as OcrRecognitionOutcome.Recognised).result.recognisedText.contains("PARKER TEXT IMAGE"))
    }
}
