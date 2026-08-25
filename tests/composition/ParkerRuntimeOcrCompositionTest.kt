package parker.composition

import java.awt.image.BufferedImage
import java.io.ByteArrayOutputStream
import java.lang.reflect.Field
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardOpenOption
import java.time.Instant
import javax.imageio.ImageIO
import kotlin.io.path.writeText
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertTrue
import kotlinx.coroutines.test.runTest
import parker.core.interfaces.CandidateEvidenceArtifact
import parker.core.interfaces.CandidateProvenance
import parker.core.interfaces.ContentNature
import parker.core.interfaces.EvidenceAnalysisRequest
import parker.core.interfaces.PrincipalId
import parker.core.runtime.DoclingOcrProviderAdapter
import parker.core.runtime.EvidenceIntelligenceOcrCoordinator
import parker.core.runtime.EvidenceRegistrationOutcome
import parker.core.runtime.OcrExecutionSequencer

/**
 * OCR Mechanism, Unit 12 ("Runtime Composition"). Production-composition
 * tests proving *this Unit's own wiring* -- `DoclingOcrProviderAdapter` ->
 * `OcrExecutionSequencer` -> `EvidenceIntelligenceOcrCoordinator` ->
 * `DefaultEvidenceIntelligence`'s third parameter, reachable only through
 * the real, unmodified `ParkerRuntime.analyseEvidence` entry point -- not
 * Docling's own recognition correctness (already proven, live, in
 * `docs/architecture/OCR_MECHANISM_DOCLING_CONCRETE_ADAPTER_IMPLEMENTATION_PLAN.md`'s
 * own live acceptance work) and not the manifest-verification mechanism
 * itself (already, separately proven for the identical, unmodified
 * `EvidenceCustodian.retrieveManifest`/`retrieve` pair by
 * `TierAOwnerInvocationCoordinatorTest`).
 *
 * Every test below that reaches a real OCR invocation uses a **fake
 * bridge script** -- a plain POSIX shell script satisfying
 * `tools/docling-ocr-bridge.py`'s own exact stdout/exit-code contract, run
 * via the real `/bin/sh` and the real, unmodified
 * `ProcessBuilderDoclingSubprocessInvoker` -- never Docling itself, and
 * never a fake `DoclingSubprocessInvoker` Kotlin object (which would not
 * prove the real subprocess/`ProcessBuilder` path this Unit's own review
 * requires). This is a genuine, real subprocess invocation through the
 * real adapter; only the *program on the other end* is a fake standing in
 * for a provisioned Docling installation, mirroring exactly why
 * `ParkerRuntimeEvidenceIntelligenceCompositionTest.kt`'s own
 * `modelEndpointUrl` is "deliberately unreachable" rather than a real
 * model server. The genuine, real-Docling live-composition proof is
 * `OcrMechanismUnit12CompositionLiveAcceptanceTest.kt`
 * (`tests/integration`, opt-in, gated on real provisioning).
 *
 * **Narrow Reasoning/OCR Precedence Resolution (post-Unit-12 follow-up),
 * resolved -- history retained for context.** A pre-existing, disclosed,
 * non-Unit-12 gap was freshly confirmed by this file's own first version:
 * `DefaultEvidenceIntelligence.analyse` called `reasoningCoordinator?.reason(...)`
 * unconditionally whenever a `reasoningCoordinator` was wired, and real
 * `ParkerRuntime` composition always wires one backed by
 * `ModelReasoningProvider`, which never supports
 * `ReasoningSubject.OfEvidenceAnalysisRequest` and unconditionally throws
 * `UnsupportedOperationException` for it -- already proven, pre-Unit-12, by
 * `ParkerRuntimeEvidenceIntelligenceCompositionTest`'s own "analyseEvidence
 * propagates a genuine Reasoning Provider fault unchanged once real input
 * resolves" test (still true, unchanged, for every non-OCR `analysisKind`).
 * Consequently, `analyseEvidence` for an OCR-eligible request that had
 * already produced a genuine OCR result still threw, discarding it. Fixed
 * (`DefaultEvidenceIntelligence.analyse`) per Evidence Intelligence Contract
 * Design §11's own "partial completion... must never be silently collapsed"
 * rule: a reasoning-leg fault, once OCR has already produced a genuine
 * result, is now partial completion -- the genuine OCR result is returned,
 * `Completed`, non-empty, never discarded. Reasoning remains unconditionally
 * *attempted* whenever a coordinator is configured (never skipped by
 * `analysisKind`); only a fault, and only once OCR already succeeded, no
 * longer erases that success. Where OCR itself produces nothing (empty
 * result), or for any non-OCR `analysisKind`, a reasoning fault still
 * propagates unchanged, exactly as before -- proven by the timeout/
 * missing-model/non-eligible/integrity-mismatch tests below, which
 * correctly remain `assertFailsWith`.
 */
class ParkerRuntimeOcrCompositionTest {

    private val ownerPrincipalId = "user.owner-ocr-composition-test"

    private fun config(
        doclingPythonExecutablePath: String = "/bin/sh",
        doclingBridgeScriptPath: String,
        doclingTimeoutMillis: Long = 30_000L,
    ): ParkerRuntimeConfig = ParkerRuntimeConfig(
        modelEndpointUrl = "http://127.0.0.1:1/api/generate", // deliberately unreachable
        modelName = "test-model",
        ownerPrincipalId = ownerPrincipalId,
        localTextChannelModuleId = "channel.local-text-ocr-composition-test",
        evidenceStorageRootPath = Files.createTempDirectory("ocr-composition-evidence").toString(),
        evidenceSourceManifestStorageRootPath = Files.createTempDirectory("ocr-composition-manifest").toString(),
        derivativeGenerationStorageRootPath = Files.createTempDirectory("ocr-composition-derivative").toString(),
        derivativeContentStorageRootPath = Files.createTempDirectory("ocr-composition-derivative-content").toString(),
        savedAnalysisStorageRootPath = Files.createTempDirectory("saved-analysis-storage").toString(),
        documentIngestionAuditLogPath = Files.createTempDirectory("ocr-composition-ingestion-audit").resolve("audit.log").toString(),
        evidenceDeletionAuditLogPath = Files.createTempDirectory("ocr-composition-deletion-audit").resolve("audit.log").toString(),
        memoryCoreDurabilityLogPath = Files.createTempDirectory("ocr-composition-memory").resolve("memory-core.log").toString(),
        knowledgeItemDurabilityLogPath = Files.createTempDirectory("ocr-composition-knowledge").resolve("items.log").toString(),
        doclingPythonExecutablePath = doclingPythonExecutablePath,
        doclingBridgeScriptPath = doclingBridgeScriptPath,
        doclingTimeoutMillis = doclingTimeoutMillis,
    )

    /**
     * A genuinely-decodable, minimal (1x1) PNG -- `DoclingOcrProviderAdapter`'s own real,
     * unmodified pre-flight dimension check (Unit 1's own resource-bound design) uses
     * `javax.imageio.ImageIO` to inspect declared image dimensions *before* ever invoking the
     * bridge; plain, non-image placeholder bytes are correctly, truthfully rejected as
     * `UnsupportedOrInaccessibleInput` pre-flight (proven directly, discovered during this file's
     * own fresh testing) -- not a bug, but a reason this test needs real, decodable image bytes to
     * reach the bridge at all.
     */
    private fun minimalPngBytes(): ByteArray = ByteArrayOutputStream().also {
        ImageIO.write(BufferedImage(1, 1, BufferedImage.TYPE_INT_RGB), "png", it)
    }.toByteArray()

    private fun candidateProvenance() = CandidateProvenance(
        sourceIdentifier = "ocr-composition-test-source",
        sourceType = "test",
        acquisitionTime = Instant.parse("2026-01-01T00:00:00Z"),
        contentNature = ContentNature.ORIGINAL,
    )

    private fun <T> Any.privateField(name: String): T {
        val field: Field = this::class.java.declaredFields.first { it.name == name }
        field.isAccessible = true
        @Suppress("UNCHECKED_CAST")
        return field.get(this) as T
    }

    /**
     * A fake bridge script satisfying `tools/docling-ocr-bridge.py`'s own
     * exact stdout/exit-code contract -- writes exactly [stdout] to stdout
     * (empty string writes nothing), exits with [exitCode], and -- if
     * [markerPath] is supplied -- touches that file first, so a test can
     * assert *no invocation occurred at all* by asserting the marker's
     * continued absence, a non-tautological signal independent of the
     * eventual [parker.core.interfaces.EvidenceAnalysisResult].
     */
    private fun writeFakeBridgeScript(directory: Path, exitCode: Int, stdout: String, markerPath: Path? = null, sleepSeconds: Int = 0): Path {
        val scriptPath = Files.createTempFile(directory, "fake-docling-bridge-", ".sh")
        val markerLine = markerPath?.let { "touch '${it.toAbsolutePath()}'\n" } ?: ""
        val sleepLine = if (sleepSeconds > 0) "sleep $sleepSeconds\n" else ""
        scriptPath.writeText(
            "#!/bin/sh\n" +
                markerLine +
                sleepLine +
                (if (stdout.isNotEmpty()) "printf '%s' '$stdout'\n" else "") +
                "exit $exitCode\n",
        )
        scriptPath.toFile().setExecutable(true)
        return scriptPath
    }

    private val successJson = """{"status":"recognised","recognisedText":"FAKE COMPOSITION TEXT","fidelity":"UNVERIFIED_LITERAL_TRANSCRIPTION","mechanismVersion":"fake-1.0.0"}"""

    // ================= 1. Construction =================

    @Test
    fun `ParkerRuntime constructs successfully with valid OCR config, and the full Docling composition chain is reachable`() = runTest {
        val scriptDir = Files.createTempDirectory("ocr-composition-scripts")
        val scriptPath = writeFakeBridgeScript(scriptDir, 0, successJson)
        val runtime = ParkerRuntime(config(doclingBridgeScriptPath = scriptPath.toString()), RecordingParkerLogger())

        runtime.start()

        assertEquals(RuntimeLifecycleState.RUNNING, runtime.state)

        val evidenceIntelligence = runtime.privateField<Any>("evidenceIntelligence")
        val ocrCoordinator = evidenceIntelligence.privateField<EvidenceIntelligenceOcrCoordinator?>("ocrCoordinator")
        assertTrue(ocrCoordinator != null, "the OCR coordinator must be wired into DefaultEvidenceIntelligence's third constructor parameter")

        val ocrMechanism = ocrCoordinator!!.privateField<Any>("ocrMechanism")
        assertIs<OcrExecutionSequencer>(ocrMechanism, "OcrExecutionSequencer must not be bypassed")
        val adapter = ocrMechanism.privateField<Any>("adapter")
        assertIs<DoclingOcrProviderAdapter>(adapter, "the sole adapter behind OcrExecutionSequencer must be DoclingOcrProviderAdapter")

        runtime.shutdown()
    }

    // ================= 2. No execution during startup =================

    @Test
    fun `OCR composition does not execute during ParkerRuntime startup -- the bridge is never invoked merely by starting`() = runTest {
        val scriptDir = Files.createTempDirectory("ocr-composition-scripts")
        val marker = scriptDir.resolve("invoked.marker")
        val scriptPath = writeFakeBridgeScript(scriptDir, 0, successJson, markerPath = marker)
        val runtime = ParkerRuntime(config(doclingBridgeScriptPath = scriptPath.toString()), RecordingParkerLogger())

        runtime.start()
        assertFalse(Files.exists(marker), "the bridge must never be invoked merely by ParkerRuntime construction/start")
        runtime.shutdown()
        assertFalse(Files.exists(marker), "the bridge must never be invoked merely by ParkerRuntime shutdown either")
    }

    // ================= 3, 8, 9. Explicit, authorised invocation on OCR-eligible sources =================

    @Test
    fun `explicit authorised analyseEvidence on a scanned-PDF-eligible source genuinely invokes the real bridge and RETURNS the OCR-derived EvidenceAnalysisResult to the caller`() = runTest {
        val scriptDir = Files.createTempDirectory("ocr-composition-scripts")
        val marker = scriptDir.resolve("invoked.marker")
        val scriptPath = writeFakeBridgeScript(scriptDir, 0, successJson, markerPath = marker)
        val runtime = ParkerRuntime(config(doclingBridgeScriptPath = scriptPath.toString()), RecordingParkerLogger())
        runtime.start()
        val principal = PrincipalId(ownerPrincipalId)

        val registered = assertIs<EvidenceRegistrationOutcome.Registered>(
            runtime.submitEvidence(principal, CandidateEvidenceArtifact("fake scanned pdf bytes".toByteArray(), receivedMediaType = "application/pdf"), candidateProvenance(), "test-document"),
        )

        // See this file's own class-level KDoc (updated for the Narrow Reasoning/OCR Precedence
        // Resolution): the pre-existing, unrelated Reasoning Provider fault no longer discards a
        // genuine OCR result -- analyseEvidence now returns it, Completed, non-empty.
        val outcome = runtime.analyseEvidence(
            principal,
            EvidenceAnalysisRequest(
                analysisKind = "ocr-transcription",
                requestingPrincipalId = principal,
                evidenceArtifactIds = listOf(registered.acceptedEvidenceArtifact.evidenceArtifactId),
            ),
        )

        assertTrue(Files.exists(marker), "the real bridge script must have been invoked")
        val completed = assertIs<EvidenceIntelligenceInvocationOutcome.Completed>(outcome)
        assertEquals(1, completed.acceptanceOutcomes.size)

        runtime.shutdown()
    }

    @Test
    fun `explicit authorised analyseEvidence on an image-eligible source genuinely invokes the real bridge and RETURNS the OCR-derived EvidenceAnalysisResult to the caller`() = runTest {
        val scriptDir = Files.createTempDirectory("ocr-composition-scripts")
        val marker = scriptDir.resolve("invoked.marker")
        val scriptPath = writeFakeBridgeScript(scriptDir, 0, successJson, markerPath = marker)
        val runtime = ParkerRuntime(config(doclingBridgeScriptPath = scriptPath.toString()), RecordingParkerLogger())
        runtime.start()
        val principal = PrincipalId(ownerPrincipalId)

        val registered = assertIs<EvidenceRegistrationOutcome.Registered>(
            runtime.submitEvidence(principal, CandidateEvidenceArtifact(minimalPngBytes(), receivedMediaType = "image/png"), candidateProvenance(), "test-document"),
        )

        val outcome = runtime.analyseEvidence(
            principal,
            EvidenceAnalysisRequest(
                analysisKind = "ocr-transcription",
                requestingPrincipalId = principal,
                evidenceArtifactIds = listOf(registered.acceptedEvidenceArtifact.evidenceArtifactId),
            ),
        )

        assertTrue(Files.exists(marker), "the real bridge script must have been invoked")
        val completed = assertIs<EvidenceIntelligenceInvocationOutcome.Completed>(outcome)
        assertEquals(1, completed.acceptanceOutcomes.size)

        runtime.shutdown()
    }

    // ================= 3b. Permission gate resource registration =================

    @Test
    fun `the real ResourceRegistry, reached through the real permission graph, genuinely holds EVIDENCE_INTELLIGENCE_INVOCATION_RESOURCE_ID`() = runTest {
        // Unit 12 introduces no new Resource/ActionVocabulary/PermissionPolicyRule -- it reuses the
        // existing (EXECUTE, DOCUMENT) gate as-is (Implementation Plan Section 3 item 7/8, Section
        // 5.H). The owner-authorised tests above already prove this transitively (permission is
        // genuinely granted, not merely unthrown); this test proves the specific, named fact
        // directly: the real ResourceRegistry instance this composition actually wires into
        // DefaultPermissionPolicy holds EvidenceIntelligenceInvocationGate's own
        // EVIDENCE_INTELLIGENCE_INVOCATION_RESOURCE_ID, fresh, not assumed from an earlier report.
        val scriptDir = Files.createTempDirectory("ocr-composition-scripts")
        val scriptPath = writeFakeBridgeScript(scriptDir, 0, successJson)
        val runtime = ParkerRuntime(config(doclingBridgeScriptPath = scriptPath.toString()), RecordingParkerLogger())
        runtime.start()

        val permissionEngine = runtime.privateField<Any>("permissionEngine")
        assertIs<parker.core.runtime.DefaultPermissionEngine>(permissionEngine, "analyseEvidence's own gate must be evaluated by the real, production DefaultPermissionEngine")
        val policy = permissionEngine.privateField<Any>("policy")
        val resourceRegistry = policy.privateField<parker.core.interfaces.ResourceRegistry>("resourceRegistry")

        val resolved = resourceRegistry.resolve(parker.core.runtime.EvidenceIntelligenceInvocationGate.EVIDENCE_INTELLIGENCE_INVOCATION_RESOURCE_ID)
        assertTrue(resolved != null, "EVIDENCE_INTELLIGENCE_INVOCATION_RESOURCE_ID must be registered in the real ResourceRegistry this composition's real DefaultPermissionPolicy actually consults")

        runtime.shutdown()
    }

    // ================= 4. Permission rejected =================

    @Test
    fun `permission rejected for an unregistered principal -- zero OCR invocation`() = runTest {
        val scriptDir = Files.createTempDirectory("ocr-composition-scripts")
        val marker = scriptDir.resolve("invoked.marker")
        val scriptPath = writeFakeBridgeScript(scriptDir, 0, successJson, markerPath = marker)
        val runtime = ParkerRuntime(config(doclingBridgeScriptPath = scriptPath.toString()), RecordingParkerLogger())
        runtime.start()

        val outcome = runtime.analyseEvidence(
            PrincipalId("principal-never-registered"),
            EvidenceAnalysisRequest(
                analysisKind = "ocr-transcription",
                requestingPrincipalId = PrincipalId("principal-never-registered"),
                evidenceArtifactIds = listOf(parker.core.interfaces.EvidenceArtifactId("nonexistent")),
            ),
        )

        assertIs<EvidenceIntelligenceInvocationOutcome.NotAuthorised>(outcome)
        assertFalse(Files.exists(marker), "permission denial must occur before the bridge is ever invoked")

        runtime.shutdown()
    }

    // ================= 5. Source not found =================

    @Test
    fun `source not found -- zero OCR invocation, empty acceptance list`() = runTest {
        val scriptDir = Files.createTempDirectory("ocr-composition-scripts")
        val marker = scriptDir.resolve("invoked.marker")
        val scriptPath = writeFakeBridgeScript(scriptDir, 0, successJson, markerPath = marker)
        val runtime = ParkerRuntime(config(doclingBridgeScriptPath = scriptPath.toString()), RecordingParkerLogger())
        runtime.start()
        val principal = PrincipalId(ownerPrincipalId)

        val outcome = runtime.analyseEvidence(
            principal,
            EvidenceAnalysisRequest(
                analysisKind = "ocr-transcription",
                requestingPrincipalId = principal,
                evidenceArtifactIds = listOf(parker.core.interfaces.EvidenceArtifactId("evidence-never-registered")),
            ),
        )

        val completed = assertIs<EvidenceIntelligenceInvocationOutcome.Completed>(outcome)
        assertTrue(completed.acceptanceOutcomes.isEmpty())
        assertFalse(Files.exists(marker), "a source that never resolves must never reach the bridge")

        runtime.shutdown()
    }

    // ================= 6. Source-integrity failure =================

    @Test
    fun `source-integrity mismatch (tampered stored bytes) -- zero OCR invocation, empty acceptance list, never a fabricated success`() = runTest {
        val scriptDir = Files.createTempDirectory("ocr-composition-scripts")
        val marker = scriptDir.resolve("invoked.marker")
        val scriptPath = writeFakeBridgeScript(scriptDir, 0, successJson, markerPath = marker)
        val runtimeConfig = config(doclingBridgeScriptPath = scriptPath.toString())
        val runtime = ParkerRuntime(runtimeConfig, RecordingParkerLogger())
        runtime.start()
        val principal = PrincipalId(ownerPrincipalId)

        val registered = assertIs<EvidenceRegistrationOutcome.Registered>(
            runtime.submitEvidence(principal, CandidateEvidenceArtifact("original untampered bytes".toByteArray(), receivedMediaType = "application/pdf"), candidateProvenance(), "test-document"),
        )
        val evidenceArtifactId = registered.acceptedEvidenceArtifact.evidenceArtifactId

        // Directly tamper with the stored evidence bytes on disk, bypassing every gate --
        // exactly the way a real caller never could -- so the manifest's own already-persisted
        // sha256/byteLength (unchanged) genuinely disagree with what retrieve() now returns.
        val storedFile = Path.of(runtimeConfig.evidenceStorageRootPath, "${evidenceArtifactId.value}.evidence")
        assertTrue(Files.exists(storedFile), "expected FileSystemEvidenceArtifactStorage's own '<id>.evidence' naming convention")
        Files.write(storedFile, "TAMPERED".toByteArray(), StandardOpenOption.APPEND)

        // The tampered source still resolves fine (retrieve() performs no integrity check of its
        // own -- that is this coordinator's job), so this call still reaches, and still throws
        // from, the pre-existing, unconditional reasoning step (see this file's class-level KDoc).
        // The claim under test here is narrower and unaffected by that: the OCR leg itself must
        // never reach the bridge for a tampered source, proven independently via the marker.
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

        assertFalse(Files.exists(marker), "a source whose stored bytes no longer match its own manifest must never reach the bridge, and must never be silently 'fixed'")

        runtime.shutdown()
    }

    // ================= 7. Non-OCR-eligible request/source =================

    @Test
    fun `a non-OCR-eligible analysisKind never invokes the bridge, even for an otherwise-resolvable source`() = runTest {
        val scriptDir = Files.createTempDirectory("ocr-composition-scripts")
        val marker = scriptDir.resolve("invoked.marker")
        val scriptPath = writeFakeBridgeScript(scriptDir, 0, successJson, markerPath = marker)
        val runtime = ParkerRuntime(config(doclingBridgeScriptPath = scriptPath.toString()), RecordingParkerLogger())
        runtime.start()
        val principal = PrincipalId(ownerPrincipalId)

        val registered = assertIs<EvidenceRegistrationOutcome.Registered>(
            runtime.submitEvidence(principal, CandidateEvidenceArtifact("some content".toByteArray(), receivedMediaType = "application/pdf"), candidateProvenance(), "test-document"),
        )

        // The source still resolves fine, so this call still reaches, and still throws from, the
        // pre-existing, unconditional reasoning step (see this file's class-level KDoc). The claim
        // under test here is narrower and unaffected by that: a non-OCR-eligible analysisKind must
        // never reach the bridge at all, proven independently via the marker.
        assertFailsWith<Exception> {
            runtime.analyseEvidence(
                principal,
                EvidenceAnalysisRequest(
                    analysisKind = "not-ocr-at-all",
                    requestingPrincipalId = principal,
                    evidenceArtifactIds = listOf(registered.acceptedEvidenceArtifact.evidenceArtifactId),
                ),
            )
        }

        assertFalse(Files.exists(marker), "a non-OCR-eligible analysisKind must never reach the bridge")

        runtime.shutdown()
    }

    // ================= 10, 17, 18. Timeout, resource-limit, unclassified failure propagation =================

    @Test
    fun `timeout propagates truthfully through the full composition, never a fabricated success`() = runTest {
        val scriptDir = Files.createTempDirectory("ocr-composition-scripts")
        val marker = scriptDir.resolve("invoked.marker")
        val scriptPath = writeFakeBridgeScript(scriptDir, 0, successJson, markerPath = marker, sleepSeconds = 5)
        val runtime = ParkerRuntime(config(doclingBridgeScriptPath = scriptPath.toString(), doclingTimeoutMillis = 500), RecordingParkerLogger())
        runtime.start()
        val principal = PrincipalId(ownerPrincipalId)
        val registered = assertIs<EvidenceRegistrationOutcome.Registered>(
            runtime.submitEvidence(principal, CandidateEvidenceArtifact("content".toByteArray(), receivedMediaType = "application/pdf"), candidateProvenance(), "test-document"),
        )

        // A timeout maps to OcrRecognitionOutcome.ProcessingOrDependencyFailure, which
        // DefaultEvidenceIntelligence's own §5.R mapping silently excludes (no fabricated
        // TransientOutput) -- OCR itself never leaks a fabricated success. The overall call still
        // throws afterward from the pre-existing, unconditional reasoning step (class-level KDoc);
        // the marker independently proves the bridge genuinely started before timing out.
        assertFailsWith<Exception> {
            runtime.analyseEvidence(
                principal,
                EvidenceAnalysisRequest(
                    analysisKind = "ocr-transcription",
                    requestingPrincipalId = principal,
                    evidenceArtifactIds = listOf(registered.acceptedEvidenceArtifact.evidenceArtifactId),
                ),
            )
        }

        assertTrue(Files.exists(marker), "the bridge must have genuinely started before the configured timeout elapsed")

        runtime.shutdown()
    }

    // ================= 11. Missing model/runtime =================

    @Test
    fun `missing model or runtime propagates truthfully, never treated as success`() = runTest {
        val scriptDir = Files.createTempDirectory("ocr-composition-scripts")
        val marker = scriptDir.resolve("invoked.marker")
        val scriptPath = writeFakeBridgeScript(scriptDir, 2, "", markerPath = marker) // EXIT_CODE_MISSING_ASSETS
        val runtime = ParkerRuntime(config(doclingBridgeScriptPath = scriptPath.toString()), RecordingParkerLogger())
        runtime.start()
        val principal = PrincipalId(ownerPrincipalId)
        val registered = assertIs<EvidenceRegistrationOutcome.Registered>(
            runtime.submitEvidence(principal, CandidateEvidenceArtifact("content".toByteArray(), receivedMediaType = "application/pdf"), candidateProvenance(), "test-document"),
        )

        // Missing assets map to a typed, non-throwing OcrRecognitionOutcome failure -- OCR itself
        // never leaks a fabricated success. The overall call still throws afterward from the
        // pre-existing, unconditional reasoning step (class-level KDoc); the marker independently
        // proves the bridge genuinely ran (and reported the missing-assets exit code) first.
        assertFailsWith<Exception> {
            runtime.analyseEvidence(
                principal,
                EvidenceAnalysisRequest(
                    analysisKind = "ocr-transcription",
                    requestingPrincipalId = principal,
                    evidenceArtifactIds = listOf(registered.acceptedEvidenceArtifact.evidenceArtifactId),
                ),
            )
        }

        assertTrue(Files.exists(marker), "the bridge must have genuinely run and reported the missing-assets exit code")

        runtime.shutdown()
    }

    // ================= 12. Provider failure =================

    @Test
    fun `an unclassified provider crash propagates as a genuine, uncaught exception -- never swallowed, never a false success`() = runTest {
        val scriptDir = Files.createTempDirectory("ocr-composition-scripts")
        val scriptPath = writeFakeBridgeScript(scriptDir, 1, "") // unclassified crash
        val runtime = ParkerRuntime(config(doclingBridgeScriptPath = scriptPath.toString()), RecordingParkerLogger())
        runtime.start()
        val principal = PrincipalId(ownerPrincipalId)
        val registered = assertIs<EvidenceRegistrationOutcome.Registered>(
            runtime.submitEvidence(principal, CandidateEvidenceArtifact("content".toByteArray(), receivedMediaType = "application/pdf"), candidateProvenance(), "test-document"),
        )

        assertFailsWith<Exception> {
            runtime.analyseEvidence(
                principal,
                EvidenceAnalysisRequest(
                    analysisKind = "ocr-transcription",
                    requestingPrincipalId = principal,
                    evidenceArtifactIds = listOf(registered.acceptedEvidenceArtifact.evidenceArtifactId),
                ),
            )
        }

        runtime.shutdown()
    }

    // ================= 13, 14, 15. No Memory/Knowledge/DerivativeGenerationRecord authority =================

    @Test
    fun `a successful OCR recognition dispatches to NotDispatched through the real chain -- no Memory Core, Knowledge, or DerivativeGenerationRecord side effect`() = runTest {
        // The minimal mapping's own default rule (Unit 12 Implementation Plan Section 5.R) --
        // Recognised -> exactly one TransientOutput -> NotDispatched (never touches
        // EvidenceCustodian, MemoryCore, or Knowledge Memory). Following the Narrow Reasoning/OCR
        // Precedence Resolution, this is now directly observable through the real, full
        // ParkerRuntime.analyseEvidence -> EvidenceIntelligenceAcceptanceCoordinator.dispatch chain
        // -- previously impossible here because the pre-existing reasoning-provider fault discarded
        // the OCR result before dispatch() was ever reached.
        val scriptDir = Files.createTempDirectory("ocr-composition-scripts")
        val marker = scriptDir.resolve("invoked.marker")
        val scriptPath = writeFakeBridgeScript(scriptDir, 0, successJson, markerPath = marker)
        val runtime = ParkerRuntime(config(doclingBridgeScriptPath = scriptPath.toString()), RecordingParkerLogger())
        runtime.start()
        val principal = PrincipalId(ownerPrincipalId)
        val registered = assertIs<EvidenceRegistrationOutcome.Registered>(
            runtime.submitEvidence(principal, CandidateEvidenceArtifact("content".toByteArray(), receivedMediaType = "application/pdf"), candidateProvenance(), "test-document"),
        )

        val outcome = runtime.analyseEvidence(
            principal,
            EvidenceAnalysisRequest(
                analysisKind = "ocr-transcription",
                requestingPrincipalId = principal,
                evidenceArtifactIds = listOf(registered.acceptedEvidenceArtifact.evidenceArtifactId),
            ),
        )
        assertTrue(Files.exists(marker), "the real bridge script must have been invoked")

        val completed = assertIs<EvidenceIntelligenceInvocationOutcome.Completed>(outcome)
        assertEquals(1, completed.acceptanceOutcomes.size)
        val dispatched = completed.acceptanceOutcomes.single()
        assertEquals("NotDispatched", dispatched::class.simpleName, "a TransientOutput must dispatch to NotDispatched -- it is never submitted to Memory Core, Knowledge Memory, or any derivative pipeline")

        // No DerivativeGenerationRecord/DerivativeGenerationStorage/MemoryCore/Knowledge dependency
        // is reachable from the OCR coordinator chain at all (structural, not merely behavioural).
        val evidenceIntelligence = runtime.privateField<Any>("evidenceIntelligence")
        val ocrCoordinator = evidenceIntelligence.privateField<Any>("ocrCoordinator")
        val fieldTypeNames = ocrCoordinator::class.java.declaredFields.map { it.type.simpleName }.toSet()
        assertTrue(
            fieldTypeNames.none { it.contains("DerivativeGeneration") || it.contains("MemoryCore") || it.contains("KnowledgeSubmission") },
            "EvidenceIntelligenceOcrCoordinator must hold no DerivativeGenerationRecord/MemoryCore/KnowledgeSubmission dependency of any kind -- found: $fieldTypeNames",
        )

        runtime.shutdown()
    }

    // ================= 16, 17 (RequiresTierB/Tier A). No Tier A router invocation, no automatic RequiresTierB->OCR path =================

    @Test
    fun `EvidenceIntelligenceOcrCoordinator holds no TierADocumentIngestionRouter dependency -- no automatic RequiresTierB to OCR path exists`() {
        val fieldTypeNames = EvidenceIntelligenceOcrCoordinator::class.java.declaredFields.map { it.type.simpleName }.toSet()
        assertTrue(
            fieldTypeNames.none { it.contains("TierA") || it.contains("Router") },
            "EvidenceIntelligenceOcrCoordinator must hold no Tier A / routing dependency of any kind -- found: $fieldTypeNames",
        )
    }

    // ================= 18. Existing non-OCR Evidence Intelligence tests remain green =================
    //
    // Verified by running ParkerRuntimeEvidenceIntelligenceCompositionTest.kt, DefaultEvidenceIntelligenceTest.kt,
    // and the full ordinary regression suite alongside this file -- not re-duplicated here.
}
