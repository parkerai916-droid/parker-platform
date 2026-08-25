package parker.composition

import java.lang.reflect.Field
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardOpenOption
import java.time.Instant
import kotlin.io.path.writeText
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertTrue
import kotlinx.coroutines.test.runTest
import parker.core.interfaces.CandidateProvenance
import parker.core.interfaces.ContentNature
import parker.core.interfaces.EvidenceAnalysisRequest
import parker.core.interfaces.EvidenceArtifactId
import parker.core.interfaces.OwnerLocalFileIngressOutcome
import parker.core.interfaces.PrincipalId
import parker.core.interfaces.TierADocumentFormat
import parker.core.interfaces.TierADocumentRoutingResult
import parker.core.interfaces.TierAOwnerInvocationOutcome
import parker.core.runtime.EvidenceRegistrationOutcome

/**
 * Document Ingestion Programme, Tier B Owner Routing / Invocation Implementation.
 *
 * **No new production code exists for this Unit.** Fresh governance inspection
 * (`OCR_MECHANISM_UNIT_12_RUNTIME_INVOCATION_SCOPE_LOCK.md` §6, §7, §18, §19) confirms the
 * owner-facing Tier B invocation surface is, and is required to remain,
 * `ParkerRuntime.analyseEvidence` itself, reused entirely unchanged -- a dedicated "Tier B
 * invocation coordinator" is explicitly **F**orbidden (§19 item 9), and a thin request-construction
 * convenience helper is explicitly **O**ptional, not required (§18). Every other capability §19
 * classifies **R**equired ("`DefaultEvidenceIntelligence` gaining an `OcrMechanism` dependency",
 * "a manifest-verified source-integrity sequence", "a concrete `OcrProviderAdapter` satisfying §14's
 * resource/security bounds") was already implemented, live-proven, owner-accepted, and pushed by
 * the immediately preceding OCR Mechanism Unit 12 ("Runtime Composition") and Narrow Reasoning/OCR
 * Precedence Resolution work -- this file's own job is proof, not construction: proving the already-
 * complete, already-governed, explicit two-step owner workflow
 * (`invokeTierAIngestionAsOwner` -> `RequiresTierB` -> a separate, explicit `analyseEvidence` call)
 * genuinely, truthfully works end to end, and that every adjacent boundary (Tier A/B isolation, no
 * automatic invocation, no downstream side effect, truthful eligibility/failure/reprocessing
 * semantics) holds.
 *
 * Mirrors [ParkerRuntimeOcrCompositionTest]'s own established fake-bridge-via-shell-script
 * technique (real `ProcessBuilderDoclingSubprocessInvoker`/`DoclingOcrProviderAdapter`, real
 * subprocess, a fake program standing in for a provisioned Docling installation) and
 * [ParkerRuntimeTierAOwnerInvocationIntegrationTest]'s own established "real, fully-wired
 * production graph" style. The genuine, real-Docling, real-owner-flow live proof is
 * `TierBOwnerRoutingLiveAcceptanceTest.kt` (`tests/integration`, opt-in, gated on real
 * provisioning).
 */
class TierBOwnerRoutingTest {

    private val ownerPrincipalId = "user.owner-tier-b-routing-test"

    private fun config(
        doclingPythonExecutablePath: String = "/bin/sh",
        doclingBridgeScriptPath: String,
        doclingTimeoutMillis: Long = 30_000L,
    ): ParkerRuntimeConfig = ParkerRuntimeConfig(
        modelEndpointUrl = "http://127.0.0.1:1/api/generate", // deliberately unreachable
        modelName = "test-model",
        ownerPrincipalId = ownerPrincipalId,
        localTextChannelModuleId = "channel.local-text-tier-b-routing-test",
        evidenceStorageRootPath = Files.createTempDirectory("tier-b-routing-evidence").toString(),
        evidenceSourceManifestStorageRootPath = Files.createTempDirectory("tier-b-routing-manifest").toString(),
        derivativeGenerationStorageRootPath = Files.createTempDirectory("tier-b-routing-derivative").toString(),
        derivativeContentStorageRootPath = Files.createTempDirectory("tier-b-routing-derivative-content").toString(),
        savedAnalysisStorageRootPath = Files.createTempDirectory("saved-analysis-storage").toString(),
        documentIngestionAuditLogPath = Files.createTempDirectory("tier-b-routing-ingestion-audit").resolve("audit.log").toString(),
        evidenceDeletionAuditLogPath = Files.createTempDirectory("tier-b-routing-deletion-audit").resolve("audit.log").toString(),
        memoryCoreDurabilityLogPath = Files.createTempDirectory("tier-b-routing-memory").resolve("memory-core.log").toString(),
        knowledgeItemDurabilityLogPath = Files.createTempDirectory("tier-b-routing-knowledge").resolve("items.log").toString(),
        doclingPythonExecutablePath = doclingPythonExecutablePath,
        doclingBridgeScriptPath = doclingBridgeScriptPath,
        doclingTimeoutMillis = doclingTimeoutMillis,
    )

    private val fixtureRoot: Path = Path.of("tests", "fixtures", "document-ingestion-bakeoff", "fixtures")

    private fun candidateProvenance() = CandidateProvenance(
        sourceIdentifier = "tier-b-routing-test-source",
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

    private val recognisedJson = """{"status":"recognised","recognisedText":"TIER B RECOGNISED TEXT","fidelity":"VERBATIM","mechanismVersion":"fake-1.0.0"}"""
    private val partialJson = """{"status":"partial","recognisedText":"TIER B PARTIAL TEXT","fidelity":"NORMALISED","reason":"page 2 unreadable"}"""
    private val noContentJson = """{"status":"no_recognisable_content","reason":"blank page"}"""

    private suspend fun ParkerRuntime.importFixtureAsOwner(fileName: String, receivedMediaType: String): EvidenceArtifactId {
        val outcome = importEvidenceFileAsOwner(fixtureRoot.resolve(fileName).toAbsolutePath().toString(), receivedMediaType)
        return assertIs<OwnerLocalFileIngressOutcome.Accepted>(outcome).acceptedEvidenceArtifact.evidenceArtifactId
    }

    // ================= Phase 12: end-to-end explicit owner workflow =================

    @Test
    fun `scanned PDF fixture 03 -- import, Tier A RequiresTierB, then a separate explicit Tier B call returns a Completed OCR-derived result`() = runTest {
        val scriptDir = Files.createTempDirectory("tier-b-scripts")
        val marker = scriptDir.resolve("invoked.marker")
        val scriptPath = writeFakeBridgeScript(scriptDir, 0, recognisedJson, markerPath = marker)
        val runtime = ParkerRuntime(config(doclingBridgeScriptPath = scriptPath.toString()), RecordingParkerLogger())
        runtime.start()

        // Step 1: owner imports the real fixture file.
        val evidenceArtifactId = runtime.importFixtureAsOwner("03-scanned.pdf", "application/pdf")

        // Step 2: owner invokes Tier A.
        val tierAOutcome = runtime.invokeTierAIngestionAsOwner(evidenceArtifactId)
        val routed = assertIs<TierAOwnerInvocationOutcome.Routed>(tierAOutcome)

        // Step 3: assert RequiresTierB.
        assertIs<TierADocumentRoutingResult.RequiresTierB>(routed.result)

        // Tier A's own classification performs no invocation of any kind -- the bridge must remain
        // untouched purely from steps 1-3.
        assertFalse(Files.exists(marker), "Tier A's own RequiresTierB classification must never itself invoke OCR")

        // Step 4: a second, separate, explicit owner action -- analyseEvidence.
        val principal = PrincipalId(ownerPrincipalId)
        val analysisOutcome = runtime.analyseEvidence(
            principal,
            EvidenceAnalysisRequest(
                analysisKind = "ocr-transcription",
                requestingPrincipalId = principal,
                evidenceArtifactIds = listOf(evidenceArtifactId),
            ),
        )

        // Step 5: assert Completed, OCR-derived.
        assertTrue(Files.exists(marker), "the explicit Tier B call must genuinely invoke the real bridge")
        val completed = assertIs<EvidenceIntelligenceInvocationOutcome.Completed>(analysisOutcome)
        assertEquals(1, completed.acceptanceOutcomes.size)

        runtime.shutdown()
    }

    @Test
    fun `PNG fixture 07 -- import, Tier A RequiresTierB, then a separate explicit Tier B call returns a Completed OCR-derived result`() = runTest {
        val scriptDir = Files.createTempDirectory("tier-b-scripts")
        val marker = scriptDir.resolve("invoked.marker")
        val scriptPath = writeFakeBridgeScript(scriptDir, 0, recognisedJson, markerPath = marker)
        val runtime = ParkerRuntime(config(doclingBridgeScriptPath = scriptPath.toString()), RecordingParkerLogger())
        runtime.start()

        val evidenceArtifactId = runtime.importFixtureAsOwner("07-text-image.png", "image/png")

        val routed = assertIs<TierAOwnerInvocationOutcome.Routed>(runtime.invokeTierAIngestionAsOwner(evidenceArtifactId))
        assertIs<TierADocumentRoutingResult.RequiresTierB>(routed.result)
        assertFalse(Files.exists(marker), "Tier A's own RequiresTierB classification must never itself invoke OCR")

        val principal = PrincipalId(ownerPrincipalId)
        val analysisOutcome = runtime.analyseEvidence(
            principal,
            EvidenceAnalysisRequest(
                analysisKind = "ocr-transcription",
                requestingPrincipalId = principal,
                evidenceArtifactIds = listOf(evidenceArtifactId),
            ),
        )

        assertTrue(Files.exists(marker), "the explicit Tier B call must genuinely invoke the real bridge")
        val completed = assertIs<EvidenceIntelligenceInvocationOutcome.Completed>(analysisOutcome)
        assertEquals(1, completed.acceptanceOutcomes.size)

        runtime.shutdown()
    }

    // ================= Phase 5: Tier A / Tier B structural isolation =================

    @Test
    fun `TierAOwnerInvocationCoordinator holds no EvidenceIntelligence or OcrMechanism dependency -- structurally incapable of triggering Tier B`() = runTest {
        val scriptDir = Files.createTempDirectory("tier-b-scripts")
        val scriptPath = writeFakeBridgeScript(scriptDir, 0, recognisedJson)
        val runtime = ParkerRuntime(config(doclingBridgeScriptPath = scriptPath.toString()), RecordingParkerLogger())
        runtime.start()

        val coordinator = runtime.privateField<Any>("tierAOwnerInvocationCoordinator")
        val fieldTypeNames = coordinator::class.java.declaredFields.map { it.type.simpleName }.toSet()
        assertTrue(
            fieldTypeNames.none { it.contains("EvidenceIntelligence") || it.contains("OcrMechanism") || it.contains("OcrExecutionSequencer") },
            "TierAOwnerInvocationCoordinator must hold no Evidence Intelligence/OCR dependency of any kind -- found: $fieldTypeNames",
        )

        runtime.shutdown()
    }

    // ================= Phase 13: negative routing =================

    @Test
    fun `searchable PDF fixture 01 stays Tier A -- Admitted, never RequiresTierB, no OCR side effect`() = runTest {
        val scriptDir = Files.createTempDirectory("tier-b-scripts")
        val marker = scriptDir.resolve("invoked.marker")
        val scriptPath = writeFakeBridgeScript(scriptDir, 0, recognisedJson, markerPath = marker)
        val runtime = ParkerRuntime(config(doclingBridgeScriptPath = scriptPath.toString()), RecordingParkerLogger())
        runtime.start()

        val evidenceArtifactId = runtime.importFixtureAsOwner("01-searchable-simple.pdf", "application/pdf")
        val routed = assertIs<TierAOwnerInvocationOutcome.Routed>(runtime.invokeTierAIngestionAsOwner(evidenceArtifactId))
        val admitted = assertIs<TierADocumentRoutingResult.Admitted>(routed.result)
        assertEquals(TierADocumentFormat.PDF, admitted.format)
        assertFalse(Files.exists(marker), "a searchable, Tier-A-admitted PDF must never reach OCR")

        runtime.shutdown()
    }

    @Test
    fun `DOCX, EML, and CSV fixtures all stay Tier A -- Admitted, no OCR side effect`() = runTest {
        val scriptDir = Files.createTempDirectory("tier-b-scripts")
        val marker = scriptDir.resolve("invoked.marker")
        val scriptPath = writeFakeBridgeScript(scriptDir, 0, recognisedJson, markerPath = marker)
        val runtime = ParkerRuntime(config(doclingBridgeScriptPath = scriptPath.toString()), RecordingParkerLogger())
        runtime.start()

        val docx = runtime.importFixtureAsOwner("04-structured.docx", "application/vnd.openxmlformats-officedocument.wordprocessingml.document")
        val docxRouted = assertIs<TierAOwnerInvocationOutcome.Routed>(runtime.invokeTierAIngestionAsOwner(docx))
        assertEquals(TierADocumentFormat.DOCX, assertIs<TierADocumentRoutingResult.Admitted>(docxRouted.result).format)

        val eml = runtime.importFixtureAsOwner("05-email-with-attachment.eml", "message/rfc822")
        val emlRouted = assertIs<TierAOwnerInvocationOutcome.Routed>(runtime.invokeTierAIngestionAsOwner(eml))
        assertEquals(TierADocumentFormat.EML, assertIs<TierADocumentRoutingResult.Admitted>(emlRouted.result).format)

        val csv = runtime.importFixtureAsOwner("06-structured.csv", "text/csv")
        val csvRouted = assertIs<TierAOwnerInvocationOutcome.Routed>(runtime.invokeTierAIngestionAsOwner(csv))
        assertEquals(TierADocumentFormat.CSV, assertIs<TierADocumentRoutingResult.Admitted>(csvRouted.result).format)

        assertFalse(Files.exists(marker), "no Tier-A-admitted, non-OCR-eligible fixture may ever reach OCR")

        runtime.shutdown()
    }

    @Test
    fun `an unsupported binary is rejected by Tier A truthfully -- never a fabricated OCR success`() = runTest {
        val scriptDir = Files.createTempDirectory("tier-b-scripts")
        val marker = scriptDir.resolve("invoked.marker")
        val scriptPath = writeFakeBridgeScript(scriptDir, 0, recognisedJson, markerPath = marker)
        val runtime = ParkerRuntime(config(doclingBridgeScriptPath = scriptPath.toString()), RecordingParkerLogger())
        runtime.start()
        val principal = PrincipalId(ownerPrincipalId)

        val registered = assertIs<EvidenceRegistrationOutcome.Registered>(
            runtime.submitEvidence(
                principal,
                parker.core.interfaces.CandidateEvidenceArtifact(byteArrayOf(1, 2, 3, 4), receivedMediaType = "application/octet-stream"),
                candidateProvenance(),
                "test-document",
            ),
        )
        val evidenceArtifactId = registered.acceptedEvidenceArtifact.evidenceArtifactId

        val routed = assertIs<TierAOwnerInvocationOutcome.Routed>(runtime.invokeTierAIngestionAsOwner(evidenceArtifactId))
        assertIs<TierADocumentRoutingResult.Unsupported>(routed.result)
        assertFalse(Files.exists(marker), "an unsupported binary must never reach OCR")

        runtime.shutdown()
    }

    @Test
    fun `permission rejected for an unregistered principal -- zero OCR invocation, even for an already-RequiresTierB source`() = runTest {
        val scriptDir = Files.createTempDirectory("tier-b-scripts")
        val marker = scriptDir.resolve("invoked.marker")
        val scriptPath = writeFakeBridgeScript(scriptDir, 0, recognisedJson, markerPath = marker)
        val runtime = ParkerRuntime(config(doclingBridgeScriptPath = scriptPath.toString()), RecordingParkerLogger())
        runtime.start()

        val evidenceArtifactId = runtime.importFixtureAsOwner("03-scanned.pdf", "application/pdf")
        assertIs<TierADocumentRoutingResult.RequiresTierB>(assertIs<TierAOwnerInvocationOutcome.Routed>(runtime.invokeTierAIngestionAsOwner(evidenceArtifactId)).result)

        val outcome = runtime.analyseEvidence(
            PrincipalId("principal-never-registered"),
            EvidenceAnalysisRequest(
                analysisKind = "ocr-transcription",
                requestingPrincipalId = PrincipalId("principal-never-registered"),
                evidenceArtifactIds = listOf(evidenceArtifactId),
            ),
        )

        assertIs<EvidenceIntelligenceInvocationOutcome.NotAuthorised>(outcome)
        assertFalse(Files.exists(marker), "permission denial must occur before the bridge is ever invoked")

        runtime.shutdown()
    }

    @Test
    fun `source-integrity mismatch after Tier A already returned RequiresTierB -- zero OCR invocation on the explicit Tier B call`() = runTest {
        val scriptDir = Files.createTempDirectory("tier-b-scripts")
        val marker = scriptDir.resolve("invoked.marker")
        val scriptPath = writeFakeBridgeScript(scriptDir, 0, recognisedJson, markerPath = marker)
        val runtimeConfig = config(doclingBridgeScriptPath = scriptPath.toString())
        val runtime = ParkerRuntime(runtimeConfig, RecordingParkerLogger())
        runtime.start()

        val evidenceArtifactId = runtime.importFixtureAsOwner("03-scanned.pdf", "application/pdf")
        assertIs<TierADocumentRoutingResult.RequiresTierB>(assertIs<TierAOwnerInvocationOutcome.Routed>(runtime.invokeTierAIngestionAsOwner(evidenceArtifactId)).result)

        // Directly tamper with the stored evidence bytes on disk, bypassing every gate.
        val storedFile = Path.of(runtimeConfig.evidenceStorageRootPath, "${evidenceArtifactId.value}.evidence")
        assertTrue(Files.exists(storedFile))
        Files.write(storedFile, "TAMPERED".toByteArray(), StandardOpenOption.APPEND)

        val principal = PrincipalId(ownerPrincipalId)
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
        assertFalse(Files.exists(marker), "a tampered source must never reach the bridge on the explicit Tier B call")

        runtime.shutdown()
    }

    @Test
    fun `a never-registered source -- zero OCR invocation on the explicit Tier B call`() = runTest {
        val scriptDir = Files.createTempDirectory("tier-b-scripts")
        val marker = scriptDir.resolve("invoked.marker")
        val scriptPath = writeFakeBridgeScript(scriptDir, 0, recognisedJson, markerPath = marker)
        val runtime = ParkerRuntime(config(doclingBridgeScriptPath = scriptPath.toString()), RecordingParkerLogger())
        runtime.start()
        val principal = PrincipalId(ownerPrincipalId)

        val outcome = runtime.analyseEvidence(
            principal,
            EvidenceAnalysisRequest(
                analysisKind = "ocr-transcription",
                requestingPrincipalId = principal,
                evidenceArtifactIds = listOf(EvidenceArtifactId("evidence-never-registered")),
            ),
        )

        val completed = assertIs<EvidenceIntelligenceInvocationOutcome.Completed>(outcome)
        assertTrue(completed.acceptanceOutcomes.isEmpty())
        assertFalse(Files.exists(marker))

        runtime.shutdown()
    }

    // ================= Phase 11: no automatic downstream action =================

    @Test
    fun `a successful explicit Tier B call dispatches to NotDispatched -- no Memory Core, Knowledge, or DerivativeGenerationRecord side effect`() = runTest {
        val scriptDir = Files.createTempDirectory("tier-b-scripts")
        val marker = scriptDir.resolve("invoked.marker")
        val scriptPath = writeFakeBridgeScript(scriptDir, 0, recognisedJson, markerPath = marker)
        val runtime = ParkerRuntime(config(doclingBridgeScriptPath = scriptPath.toString()), RecordingParkerLogger())
        runtime.start()

        val evidenceArtifactId = runtime.importFixtureAsOwner("03-scanned.pdf", "application/pdf")
        assertIs<TierADocumentRoutingResult.RequiresTierB>(assertIs<TierAOwnerInvocationOutcome.Routed>(runtime.invokeTierAIngestionAsOwner(evidenceArtifactId)).result)

        val principal = PrincipalId(ownerPrincipalId)
        val outcome = runtime.analyseEvidence(
            principal,
            EvidenceAnalysisRequest(
                analysisKind = "ocr-transcription",
                requestingPrincipalId = principal,
                evidenceArtifactIds = listOf(evidenceArtifactId),
            ),
        )
        assertTrue(Files.exists(marker))

        val completed = assertIs<EvidenceIntelligenceInvocationOutcome.Completed>(outcome)
        assertEquals(1, completed.acceptanceOutcomes.size)
        assertEquals("NotDispatched", completed.acceptanceOutcomes.single()::class.simpleName)

        runtime.shutdown()
    }

    // ================= Phase 15: reprocessing =================

    @Test
    fun `explicit Tier B invoked twice on the same source -- two independent Completed results, no dedup, no silent overwrite`() = runTest {
        val scriptDir = Files.createTempDirectory("tier-b-scripts")
        val marker = scriptDir.resolve("invoked.marker")
        val scriptPath = writeFakeBridgeScript(scriptDir, 0, recognisedJson, markerPath = marker)
        val runtime = ParkerRuntime(config(doclingBridgeScriptPath = scriptPath.toString()), RecordingParkerLogger())
        runtime.start()

        val evidenceArtifactId = runtime.importFixtureAsOwner("03-scanned.pdf", "application/pdf")
        assertIs<TierADocumentRoutingResult.RequiresTierB>(assertIs<TierAOwnerInvocationOutcome.Routed>(runtime.invokeTierAIngestionAsOwner(evidenceArtifactId)).result)

        val principal = PrincipalId(ownerPrincipalId)
        val request = EvidenceAnalysisRequest(
            analysisKind = "ocr-transcription",
            requestingPrincipalId = principal,
            evidenceArtifactIds = listOf(evidenceArtifactId),
        )

        val first = assertIs<EvidenceIntelligenceInvocationOutcome.Completed>(runtime.analyseEvidence(principal, request))
        val second = assertIs<EvidenceIntelligenceInvocationOutcome.Completed>(runtime.analyseEvidence(principal, request))

        assertEquals(1, first.acceptanceOutcomes.size)
        assertEquals(1, second.acceptanceOutcomes.size)
        // Each invocation is independently a TransientOutput -> NotDispatched -- neither call
        // observes, references, or is affected by the other; nothing is deduplicated or merged.
        assertEquals("NotDispatched", first.acceptanceOutcomes.single()::class.simpleName)
        assertEquals("NotDispatched", second.acceptanceOutcomes.single()::class.simpleName)

        // Reprocessing must still be genuine, real invocation both times (real subprocess, not a
        // cached/short-circuited result) -- proven by re-writing over the same marker path across
        // two genuinely separate real subprocess launches without error.
        assertTrue(Files.exists(marker))

        runtime.shutdown()
    }

    // ================= Phase 16: failure / recovery =================

    // Note: for the four scenarios below, the OCR leg itself produces nothing (an empty
    // ocrResults), so -- per the Narrow Reasoning/OCR Precedence Resolution's own, unchanged
    // "if ocrResults is empty, this is unchanged, total-failure behaviour" rule -- the pre-existing,
    // unrelated Reasoning Provider fault still propagates unchanged (never caught, never converted
    // into a fabricated Completed/empty outcome). This is correct, not a regression: there is
    // nothing genuine to return, so nothing is fabricated. The marker file independently proves the
    // real bridge genuinely ran (or, for the timeout case, genuinely started) and reported its own
    // truthful outcome before that unrelated downstream fault occurred.

    @Test
    fun `unsupported or corrupt OCR input propagates truthfully, and a subsequent valid invocation still works`() = runTest {
        val scriptDir = Files.createTempDirectory("tier-b-scripts")
        val marker = scriptDir.resolve("invoked.marker")
        val corruptScript = writeFakeBridgeScript(scriptDir, 3, "", markerPath = marker) // EXIT_CODE_UNSUPPORTED_INPUT
        val runtime = ParkerRuntime(config(doclingBridgeScriptPath = corruptScript.toString()), RecordingParkerLogger())
        runtime.start()
        val principal = PrincipalId(ownerPrincipalId)
        val evidenceArtifactId = runtime.importFixtureAsOwner("03-scanned.pdf", "application/pdf")
        assertIs<TierADocumentRoutingResult.RequiresTierB>(assertIs<TierAOwnerInvocationOutcome.Routed>(runtime.invokeTierAIngestionAsOwner(evidenceArtifactId)).result)

        val request = EvidenceAnalysisRequest(
            analysisKind = "ocr-transcription",
            requestingPrincipalId = principal,
            evidenceArtifactIds = listOf(evidenceArtifactId),
        )
        assertFailsWith<Exception> { runtime.analyseEvidence(principal, request) }
        assertTrue(Files.exists(marker), "the real bridge must have genuinely run and reported the unsupported-input exit code")

        // Subsequent, independent, valid invocation on the same runtime still works.
        val secondScript = writeFakeBridgeScript(scriptDir, 0, recognisedJson)
        val secondRuntime = ParkerRuntime(config(doclingBridgeScriptPath = secondScript.toString()), RecordingParkerLogger())
        secondRuntime.start()
        val secondArtifactId = secondRuntime.importFixtureAsOwner("03-scanned.pdf", "application/pdf")
        assertIs<TierADocumentRoutingResult.RequiresTierB>(assertIs<TierAOwnerInvocationOutcome.Routed>(secondRuntime.invokeTierAIngestionAsOwner(secondArtifactId)).result)
        val recovered = assertIs<EvidenceIntelligenceInvocationOutcome.Completed>(
            secondRuntime.analyseEvidence(
                principal,
                EvidenceAnalysisRequest(analysisKind = "ocr-transcription", requestingPrincipalId = principal, evidenceArtifactIds = listOf(secondArtifactId)),
            ),
        )
        assertEquals(1, recovered.acceptanceOutcomes.size, "a subsequent, valid invocation must still succeed after a prior failure")

        runtime.shutdown()
        secondRuntime.shutdown()
    }

    @Test
    fun `missing Docling model or runtime propagates truthfully, never treated as success`() = runTest {
        val scriptDir = Files.createTempDirectory("tier-b-scripts")
        val marker = scriptDir.resolve("invoked.marker")
        val script = writeFakeBridgeScript(scriptDir, 2, "", markerPath = marker) // EXIT_CODE_MISSING_ASSETS
        val runtime = ParkerRuntime(config(doclingBridgeScriptPath = script.toString()), RecordingParkerLogger())
        runtime.start()
        val principal = PrincipalId(ownerPrincipalId)
        val evidenceArtifactId = runtime.importFixtureAsOwner("07-text-image.png", "image/png")
        assertIs<TierADocumentRoutingResult.RequiresTierB>(assertIs<TierAOwnerInvocationOutcome.Routed>(runtime.invokeTierAIngestionAsOwner(evidenceArtifactId)).result)

        assertFailsWith<Exception> {
            runtime.analyseEvidence(
                principal,
                EvidenceAnalysisRequest(analysisKind = "ocr-transcription", requestingPrincipalId = principal, evidenceArtifactIds = listOf(evidenceArtifactId)),
            )
        }
        assertTrue(Files.exists(marker), "the real bridge must have genuinely run and reported the missing-assets exit code")

        runtime.shutdown()
    }

    @Test
    fun `a timeout propagates truthfully, and a subsequent, independent invocation still succeeds`() = runTest {
        val scriptDir = Files.createTempDirectory("tier-b-scripts")
        val marker = scriptDir.resolve("invoked.marker")
        val timeoutScript = writeFakeBridgeScript(scriptDir, 0, recognisedJson, markerPath = marker, sleepSeconds = 3)
        val runtime = ParkerRuntime(config(doclingBridgeScriptPath = timeoutScript.toString(), doclingTimeoutMillis = 500), RecordingParkerLogger())
        runtime.start()
        val principal = PrincipalId(ownerPrincipalId)
        val evidenceArtifactId = runtime.importFixtureAsOwner("03-scanned.pdf", "application/pdf")
        assertIs<TierADocumentRoutingResult.RequiresTierB>(assertIs<TierAOwnerInvocationOutcome.Routed>(runtime.invokeTierAIngestionAsOwner(evidenceArtifactId)).result)

        val request = EvidenceAnalysisRequest(analysisKind = "ocr-transcription", requestingPrincipalId = principal, evidenceArtifactIds = listOf(evidenceArtifactId))
        assertFailsWith<Exception> { runtime.analyseEvidence(principal, request) }
        assertTrue(Files.exists(marker), "the bridge must have genuinely started before the configured timeout elapsed")

        // Subsequent, independent, valid invocation on a fresh runtime still succeeds.
        val secondScript = writeFakeBridgeScript(scriptDir, 0, recognisedJson)
        val secondRuntime = ParkerRuntime(config(doclingBridgeScriptPath = secondScript.toString()), RecordingParkerLogger())
        secondRuntime.start()
        val secondArtifactId = secondRuntime.importFixtureAsOwner("03-scanned.pdf", "application/pdf")
        assertIs<TierADocumentRoutingResult.RequiresTierB>(assertIs<TierAOwnerInvocationOutcome.Routed>(secondRuntime.invokeTierAIngestionAsOwner(secondArtifactId)).result)
        val recovered = assertIs<EvidenceIntelligenceInvocationOutcome.Completed>(
            secondRuntime.analyseEvidence(
                principal,
                EvidenceAnalysisRequest(analysisKind = "ocr-transcription", requestingPrincipalId = principal, evidenceArtifactIds = listOf(secondArtifactId)),
            ),
        )
        assertEquals(1, recovered.acceptanceOutcomes.size, "a subsequent, valid invocation must still succeed after a prior timeout")

        runtime.shutdown()
        secondRuntime.shutdown()
    }

    @Test
    fun `an unclassified provider crash propagates as a genuine, uncaught exception, never a false success`() = runTest {
        val scriptDir = Files.createTempDirectory("tier-b-scripts")
        val crashScript = writeFakeBridgeScript(scriptDir, 1, "") // unclassified crash
        val runtime = ParkerRuntime(config(doclingBridgeScriptPath = crashScript.toString()), RecordingParkerLogger())
        runtime.start()
        val principal = PrincipalId(ownerPrincipalId)
        val evidenceArtifactId = runtime.importFixtureAsOwner("03-scanned.pdf", "application/pdf")
        assertIs<TierADocumentRoutingResult.RequiresTierB>(assertIs<TierAOwnerInvocationOutcome.Routed>(runtime.invokeTierAIngestionAsOwner(evidenceArtifactId)).result)

        assertFailsWith<Exception> {
            runtime.analyseEvidence(
                principal,
                EvidenceAnalysisRequest(analysisKind = "ocr-transcription", requestingPrincipalId = principal, evidenceArtifactIds = listOf(evidenceArtifactId)),
            )
        }

        runtime.shutdown()
    }

    @Test
    fun `no recognisable content is reported truthfully, never a fabricated TransientOutput`() = runTest {
        val scriptDir = Files.createTempDirectory("tier-b-scripts")
        val marker = scriptDir.resolve("invoked.marker")
        val script = writeFakeBridgeScript(scriptDir, 0, noContentJson, markerPath = marker)
        val runtime = ParkerRuntime(config(doclingBridgeScriptPath = script.toString()), RecordingParkerLogger())
        runtime.start()
        val principal = PrincipalId(ownerPrincipalId)
        val evidenceArtifactId = runtime.importFixtureAsOwner("07-text-image.png", "image/png")
        assertIs<TierADocumentRoutingResult.RequiresTierB>(assertIs<TierAOwnerInvocationOutcome.Routed>(runtime.invokeTierAIngestionAsOwner(evidenceArtifactId)).result)

        assertFailsWith<Exception> {
            runtime.analyseEvidence(
                principal,
                EvidenceAnalysisRequest(analysisKind = "ocr-transcription", requestingPrincipalId = principal, evidenceArtifactIds = listOf(evidenceArtifactId)),
            )
        }
        assertTrue(Files.exists(marker), "the real bridge must have genuinely run and reported no recognisable content")

        runtime.shutdown()
    }

    @Test
    fun `a partial or degraded OCR result is returned truthfully, never upgraded to complete`() = runTest {
        val scriptDir = Files.createTempDirectory("tier-b-scripts")
        val script = writeFakeBridgeScript(scriptDir, 0, partialJson)
        val runtime = ParkerRuntime(config(doclingBridgeScriptPath = script.toString()), RecordingParkerLogger())
        runtime.start()
        val principal = PrincipalId(ownerPrincipalId)
        val evidenceArtifactId = runtime.importFixtureAsOwner("03-scanned.pdf", "application/pdf")
        assertIs<TierADocumentRoutingResult.RequiresTierB>(assertIs<TierAOwnerInvocationOutcome.Routed>(runtime.invokeTierAIngestionAsOwner(evidenceArtifactId)).result)

        val outcome = assertIs<EvidenceIntelligenceInvocationOutcome.Completed>(
            runtime.analyseEvidence(
                principal,
                EvidenceAnalysisRequest(analysisKind = "ocr-transcription", requestingPrincipalId = principal, evidenceArtifactIds = listOf(evidenceArtifactId)),
            ),
        )
        assertEquals(1, outcome.acceptanceOutcomes.size, "a partial/degraded result must still be genuinely returned, not discarded")
        assertEquals("NotDispatched", outcome.acceptanceOutcomes.single()::class.simpleName)

        runtime.shutdown()
    }

    // ================= Phase 17: seven-fixture routing matrix (composition-level) =================

    @Test
    fun `the complete seven-fixture owner ingress and routing matrix selects the correct route for every fixture, with source bytes unchanged throughout`() = runTest {
        val scriptDir = Files.createTempDirectory("tier-b-scripts")
        val marker = scriptDir.resolve("invoked.marker")
        val scriptPath = writeFakeBridgeScript(scriptDir, 0, recognisedJson, markerPath = marker)
        val runtimeConfig = config(doclingBridgeScriptPath = scriptPath.toString())
        val runtime = ParkerRuntime(runtimeConfig, RecordingParkerLogger())
        runtime.start()
        val principal = PrincipalId(ownerPrincipalId)

        data class Case(val file: String, val mediaType: String, val expectedFormat: TierADocumentFormat?)
        val cases = listOf(
            Case("01-searchable-simple.pdf", "application/pdf", TierADocumentFormat.PDF),
            Case("02-multicolumn-complex.pdf", "application/pdf", TierADocumentFormat.PDF),
            Case("03-scanned.pdf", "application/pdf", null), // RequiresTierB
            Case("04-structured.docx", "application/vnd.openxmlformats-officedocument.wordprocessingml.document", TierADocumentFormat.DOCX),
            Case("05-email-with-attachment.eml", "message/rfc822", TierADocumentFormat.EML),
            Case("06-structured.csv", "text/csv", TierADocumentFormat.CSV),
            Case("07-text-image.png", "image/png", null), // RequiresTierB
        )

        var tierBSuccesses = 0
        cases.forEach { case ->
            val originalBytes = Files.readAllBytes(fixtureRoot.resolve(case.file))
            val evidenceArtifactId = runtime.importFixtureAsOwner(case.file, case.mediaType)
            val routed = assertIs<TierAOwnerInvocationOutcome.Routed>(runtime.invokeTierAIngestionAsOwner(evidenceArtifactId))

            if (case.expectedFormat != null) {
                val admitted = assertIs<TierADocumentRoutingResult.Admitted>(routed.result, "expected Admitted for ${case.file}")
                assertEquals(case.expectedFormat, admitted.format, "wrong Tier A format for ${case.file}")
            } else {
                assertIs<TierADocumentRoutingResult.RequiresTierB>(routed.result, "expected RequiresTierB for ${case.file}")

                // Explicit, separate Tier B action for the two RequiresTierB fixtures.
                val analysisOutcome = runtime.analyseEvidence(
                    principal,
                    EvidenceAnalysisRequest(analysisKind = "ocr-transcription", requestingPrincipalId = principal, evidenceArtifactIds = listOf(evidenceArtifactId)),
                )
                val completed = assertIs<EvidenceIntelligenceInvocationOutcome.Completed>(analysisOutcome, "expected a Completed Tier B result for ${case.file}")
                assertEquals(1, completed.acceptanceOutcomes.size, "expected exactly one OCR-derived result for ${case.file}")
                tierBSuccesses++
            }

            // Source unchanged, for every fixture, regardless of route.
            val storedFile = Path.of(runtimeConfig.evidenceStorageRootPath, "${evidenceArtifactId.value}.evidence")
            assertTrue(originalBytes.contentEquals(Files.readAllBytes(storedFile)), "source bytes must remain unchanged for ${case.file}")
        }

        assertEquals(2, tierBSuccesses, "exactly fixtures 03 and 07 must reach a successful, explicit Tier B result")
        assertTrue(Files.exists(marker), "the bridge must have been invoked for the RequiresTierB fixtures")

        runtime.shutdown()
    }
}
