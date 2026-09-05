package parker.composition

import java.awt.image.BufferedImage
import java.io.ByteArrayOutputStream
import java.nio.file.Files
import java.nio.file.Path
import javax.imageio.ImageIO
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue
import kotlinx.coroutines.test.runTest
import parker.core.interfaces.CandidateEvidenceArtifact
import parker.core.interfaces.CandidateProvenance
import parker.core.interfaces.ContentNature
import parker.core.interfaces.EvidenceArtifactId
import parker.core.interfaces.PrincipalId
import parker.core.interfaces.TierBOcrOwnerInvocationOutcome
import parker.core.runtime.EvidenceRegistrationOutcome

/**
 * UI-INGESTION-8D: [buildOwnerHttpAdapter] is the exact construction
 * [main] uses to wire [OwnerEvidenceHttpServer]'s `operations:` seam --
 * the real, deployed production entry point. UI-INGESTION-8C deployed a
 * candidate where the newly accepted exact-evidence OCR derivative
 * discovery capability was wired into
 * [createOwnerUiRuntimeSession] (`OwnerUiRuntimeComposition.kt`, the
 * desktop-launcher composition root) but *not* here, so the deployed
 * adapter silently fell back to [OwnerEvidenceOperations]'s own safe
 * `{ emptyList() }` default -- discovered only by live production
 * verification, rolled back as a result. These tests exercise
 * [buildOwnerHttpAdapter] directly against a real, fully-wired
 * [ParkerRuntime], proving the real production wiring itself works, not
 * merely that [ParkerRuntime.discoverOcrDerivativeGenerationsAsOwner] does
 * (already covered elsewhere) or that some other adapter instance does.
 */
class BuildOwnerHttpAdapterCompositionTest {

    private val ownerPrincipalId = "user.build-owner-http-adapter-test"
    private val digestA = "a".repeat(64)

    private fun config(doclingBridgeScriptPath: String) = ParkerRuntimeConfig(
        modelEndpointUrl = "http://127.0.0.1:1/api/generate", // deliberately unreachable
        modelName = "test-model",
        ownerPrincipalId = ownerPrincipalId,
        localTextChannelModuleId = "channel.local-text-build-owner-http-adapter-test",
        evidenceStorageRootPath = Files.createTempDirectory("build-owner-http-adapter-evidence").toString(),
        evidenceSourceManifestStorageRootPath = Files.createTempDirectory("build-owner-http-adapter-manifest").toString(),
        derivativeGenerationStorageRootPath = Files.createTempDirectory("build-owner-http-adapter-derivative").toString(),
        derivativeContentStorageRootPath = Files.createTempDirectory("build-owner-http-adapter-derivative-content").toString(),
        savedAnalysisStorageRootPath = Files.createTempDirectory("saved-analysis-storage").toString(),
        documentIngestionAuditLogPath = Files.createTempDirectory("build-owner-http-adapter-ingestion-audit").resolve("audit.log").toString(),
        evidenceDeletionAuditLogPath = Files.createTempDirectory("build-owner-http-adapter-deletion-audit").resolve("audit.log").toString(),
        memoryCoreDurabilityLogPath = Files.createTempDirectory("build-owner-http-adapter-memory").resolve("memory-core.log").toString(),
        knowledgeItemDurabilityLogPath = Files.createTempDirectory("build-owner-http-adapter-knowledge").resolve("items.log").toString(),
        doclingPythonExecutablePath = syntheticBridgeShellExecutable(),
        doclingBridgeScriptPath = doclingBridgeScriptPath,
        doclingTimeoutMillis = 30_000L,
        humanFidelityReviewStorageRootPath = Files.createTempDirectory("build-owner-http-adapter-hfr-reviews").toString(),
        humanFidelityGovernanceAuditStorageRootPath = Files.createTempDirectory("build-owner-http-adapter-hfr-audit").toString(),
    )

    private fun minimalPngBytes(): ByteArray = ByteArrayOutputStream().also {
        ImageIO.write(BufferedImage(1, 1, BufferedImage.TYPE_INT_RGB), "png", it)
    }.toByteArray()

    private fun candidateProvenance() = CandidateProvenance(
        sourceIdentifier = "build-owner-http-adapter-test-source",
        sourceType = "test",
        acquisitionTime = java.time.Instant.parse("2026-01-01T00:00:00Z"),
        contentNature = ContentNature.ORIGINAL,
    )

    private fun writeFakeBridgeScript(directory: Path, stdout: String): Path {
        val scriptPath = Files.createTempFile(directory, "fake-docling-bridge-", ".sh")
        Files.writeString(scriptPath, "#!/bin/sh\nprintf '%s' '$stdout'\nexit 0\n")
        scriptPath.toFile().setExecutable(true)
        return scriptPath
    }

    private fun successJson(): String =
        """{"status":"recognised","recognisedText":"BUILD OWNER HTTP ADAPTER TEXT","fidelity":"UNVERIFIED_LITERAL_TRANSCRIPTION","mechanismVersion":"docling-2.5.0","modelIdentity":"rapidocr-onnxruntime:PP-OCRv6_rec_small","modelVersion":"sha256:$digestA"}"""

    /**
     * HFR Owner UI exposure scope lock amendment: exact-target binding requires
     * `OcrProcessingProvenance` -- populated only by the external-transcription admission path
     * ([parker.core.runtime.DerivativeGenerationCoordinator]'s second `OcrDerivativeExtractedResult`
     * construction site), never by [ParkerRuntime.invokeTierBOcrDurableGenerationAsOwner]'s local
     * Docling-based path. Writes a synthetic externally-transcribed generation directly to the same
     * storage roots the runtime itself uses, mirroring [OwnerEvidenceHttpServerTest]'s own
     * `externalAdmitted` fixture shape, without requiring real network egress.
     */
    private suspend fun admitExternalOcrGeneration(
        cfg: ParkerRuntimeConfig,
        evidenceArtifactId: EvidenceArtifactId,
        derivativeGenerationId: parker.core.interfaces.DerivativeGenerationId = parker.core.interfaces.DerivativeGenerationId("generation-hfr-owner-ui-test"),
    ): parker.core.interfaces.DerivativeGenerationId {
        val generationStorage = parker.core.runtime.FileSystemDerivativeGenerationStorage(Path.of(cfg.derivativeGenerationStorageRootPath))
        val contentStorage = parker.core.runtime.FileSystemDerivativeContentStorage(Path.of(cfg.derivativeContentStorageRootPath))
        val scope = parker.core.interfaces.OcrPageScope(listOf(1))
        val accounting = parker.core.interfaces.OcrPageAccounting(
            scope, scope, scope,
            listOf(parker.core.interfaces.OcrPageOutcome(1, parker.core.interfaces.OcrPageOutcomeKind.TRANSCRIBED)),
        )
        val processing = parker.core.interfaces.OcrProcessingProvenance(
            evidenceArtifactId, parker.core.interfaces.OcrSha256Digest("a".repeat(64)), "application/pdf", 10,
            scope, scope, "application/pdf", 10, parker.core.interfaces.OcrSha256Digest("a".repeat(64)), true, "direct-v1", java.time.Instant.EPOCH,
        )
        val provider = parker.core.interfaces.OcrProviderProvenance("OpenAI", "adapter", "1.0.0", "profile", "returned-model", parker.core.interfaces.OcrModelSnapshot.NotExposed, "response-id")
        val producer = parker.core.interfaces.DerivativeProducerIdentity("external", "1.0.0", "profile", "adapter", "1.0.0", "returned-model", null)
        val extracted = parker.core.interfaces.OcrDerivativeExtractedResult(
            "recognised text", parker.core.interfaces.TranscriptionFidelity.VERBATIM, parker.core.interfaces.OcrDerivativeOutcomeKind.RECOGNISED, null,
            emptyList(), listOf(parker.core.interfaces.OcrRecognitionSegment("recognised text", parker.core.interfaces.TranscriptionFidelity.VERBATIM, 1)),
            producer, listOf(parker.core.interfaces.DerivativeTransformation.OCR, parker.core.interfaces.DerivativeTransformation.MODEL_INFERENCE),
            parker.core.interfaces.DerivativeCompletenessState.ACCOUNTED_FOR_WITH_QUALIFICATIONS, accounting, processing, provider, java.time.Instant.EPOCH,
        )
        val id = derivativeGenerationId
        val record = parker.core.interfaces.DerivativeGenerationRecord(
            id, evidenceArtifactId, listOf(parker.core.interfaces.DerivativeParentReference.RootEvidenceArtifact(evidenceArtifactId)),
            "External transcription recognised text", producer, extracted.transformationHistory, java.time.Instant.EPOCH,
            parker.core.interfaces.DerivativeContentIdentity.NoCanonicalSerialization, extracted.completenessState,
            parker.core.interfaces.DerivativeOperationalOutcome.USABLE,
        )
        contentStorage.prepare(parker.core.interfaces.DerivativeContentEntry(id, evidenceArtifactId, parker.core.interfaces.TierADerivativePayload.Ocr(extracted)))
        contentStorage.publishPrepared(id)
        generationStorage.prepare(record)
        generationStorage.publishPrepared(id)
        return id
    }

    private suspend fun registerImage(runtime: ParkerRuntime): EvidenceArtifactId {
        val registered = assertIs<EvidenceRegistrationOutcome.Registered>(
            runtime.submitEvidence(PrincipalId(ownerPrincipalId), CandidateEvidenceArtifact(minimalPngBytes(), receivedMediaType = "image/png"), candidateProvenance(), "test-document"),
        )
        return registered.acceptedEvidenceArtifact.evidenceArtifactId
    }

    @Test
    fun `Main-kt's real adapter construction discovers a real admitted OCR derivative generation -- it does not fall back to the safe empty-list default`() = runTest {
        val scriptDir = Files.createTempDirectory("build-owner-http-adapter-scripts")
        val cfg = config(writeFakeBridgeScript(scriptDir, successJson()).toString())
        val runtime = ParkerRuntime(cfg, RecordingParkerLogger())
        runtime.start()
        try {
            val evidenceArtifactId = registerImage(runtime)
            val admitted = assertIs<TierBOcrOwnerInvocationOutcome.Admitted>(runtime.invokeTierBOcrDurableGenerationAsOwner(evidenceArtifactId))

            val adapter = buildOwnerHttpAdapter(runtime, cfg)
            val discovered = adapter.discoverOcrDerivativeGenerations(evidenceArtifactId)

            assertEquals(1, discovered.size, "the real production adapter must not silently return the empty-list default")
            assertEquals(admitted.record.derivativeGenerationId.value, discovered.single().derivativeGenerationId)
            assertEquals(evidenceArtifactId.value, discovered.single().evidenceArtifactId)
        } finally {
            runtime.shutdown()
        }
    }

    @Test
    fun `Main-kt's real adapter construction correctly returns an empty list for evidence with no admitted OCR derivative`() = runTest {
        val scriptDir = Files.createTempDirectory("build-owner-http-adapter-scripts-empty")
        val cfg = config(writeFakeBridgeScript(scriptDir, successJson()).toString())
        val runtime = ParkerRuntime(cfg, RecordingParkerLogger())
        runtime.start()
        try {
            val evidenceArtifactId = registerImage(runtime)
            val adapter = buildOwnerHttpAdapter(runtime, cfg)

            assertEquals(emptyList(), adapter.discoverOcrDerivativeGenerations(evidenceArtifactId))
        } finally {
            runtime.shutdown()
        }
    }

    @Test
    fun `Main-kt's real adapter construction records and projects a governed Human Fidelity Review through the existing HFR domain`() = runTest {
        val scriptDir = Files.createTempDirectory("build-owner-http-adapter-scripts-hfr")
        val cfg = config(writeFakeBridgeScript(scriptDir, successJson()).toString())
        val runtime = ParkerRuntime(cfg, RecordingParkerLogger())
        runtime.start()
        try {
            val evidenceArtifactId = EvidenceArtifactId("evidence-hfr-owner-ui-test")
            val derivativeGenerationId = admitExternalOcrGeneration(cfg, evidenceArtifactId)
            val adapter = buildOwnerHttpAdapter(runtime, cfg)

            val beforeReview = adapter.effectiveHumanFidelityReview(evidenceArtifactId, derivativeGenerationId)
            assertEquals("UNREVIEWED", beforeReview.effectiveReviewState)

            val outcome = adapter.recordHumanFidelityReview(
                evidenceArtifactId, derivativeGenerationId,
                parker.ui.OwnerHumanFidelityReviewSubmission(
                    reviewOutcome = "HUMAN_REVIEWED_PASS",
                    reviewedPages = listOf(1),
                    descriptiveFidelity = "Verbatim and accurate against the source.",
                ),
            )
            assertIs<parker.ui.OwnerHumanFidelityReviewRecordingOutcome.Recorded>(outcome)

            val afterReview = adapter.effectiveHumanFidelityReview(evidenceArtifactId, derivativeGenerationId)
            assertEquals("HUMAN_REVIEWED_PASS", afterReview.effectiveReviewState)
        } finally {
            runtime.shutdown()
        }
    }

    // HFR-UI-1: the discovery entry and the inspection panel each used to derive their
    // "human review" status from the separate, pre-HFR HumanVerificationRecord mechanism -- which
    // this Tier B path never writes to -- so they silently stayed at "UNREVIEWED" even after a real
    // Human Fidelity Review was recorded through the HFR panel. These tests prove all three
    // surfaces now agree, are read directly from the one existing effective HFR projection, and
    // remain exact-target bound (a review never leaks across generations or evidence artefacts).

    @Test
    fun `discovery, inspection, and the HFR panel all present the same effective HUMAN_REVIEWED_PASS state after a real review`() = runTest {
        val scriptDir = Files.createTempDirectory("build-owner-http-adapter-scripts-hfr-ui-1")
        val cfg = config(writeFakeBridgeScript(scriptDir, successJson()).toString())
        val runtime = ParkerRuntime(cfg, RecordingParkerLogger())
        runtime.start()
        try {
            val evidenceArtifactId = EvidenceArtifactId("evidence-hfr-ui-1-pass")
            val derivativeGenerationId = admitExternalOcrGeneration(cfg, evidenceArtifactId, parker.core.interfaces.DerivativeGenerationId("generation-hfr-ui-1-pass"))
            val adapter = buildOwnerHttpAdapter(runtime, cfg)

            assertIs<parker.ui.OwnerHumanFidelityReviewRecordingOutcome.Recorded>(
                adapter.recordHumanFidelityReview(
                    evidenceArtifactId, derivativeGenerationId,
                    parker.ui.OwnerHumanFidelityReviewSubmission("HUMAN_REVIEWED_PASS", listOf(1), "Faithful and accurate."),
                ),
            )

            val discovered = adapter.discoverOcrDerivativeGenerations(evidenceArtifactId).single { it.derivativeGenerationId == derivativeGenerationId.value }
            assertEquals("HUMAN_REVIEWED_PASS", discovered.humanReviewState, "discovery entry must show the effective HFR state")

            val inspected = assertIs<parker.ui.TierBOcrContentRetrievalResult.Retrieved>(
                adapter.retrieveTierBOcrContent(evidenceArtifactId, derivativeGenerationId),
            )
            assertEquals(listOf("HUMAN_REVIEWED_PASS"), inspected.content.humanReviewStates, "inspection panel must show the effective HFR state")
            // The intrinsic machine-transcription fidelity designation is a separate fact and is
            // never altered by recording a human review.
            assertTrue(inspected.content.externalTranscription, "the intrinsic 'Machine transcription -- unverified' designation must remain unchanged")

            val panel = adapter.effectiveHumanFidelityReview(evidenceArtifactId, derivativeGenerationId)
            assertEquals("HUMAN_REVIEWED_PASS", panel.effectiveReviewState, "the HFR panel itself must show the same effective state")
        } finally {
            runtime.shutdown()
        }
    }

    @Test
    fun `an unreviewed generation shows UNREVIEWED in both discovery and inspection`() = runTest {
        val scriptDir = Files.createTempDirectory("build-owner-http-adapter-scripts-hfr-ui-1-unreviewed")
        val cfg = config(writeFakeBridgeScript(scriptDir, successJson()).toString())
        val runtime = ParkerRuntime(cfg, RecordingParkerLogger())
        runtime.start()
        try {
            val evidenceArtifactId = EvidenceArtifactId("evidence-hfr-ui-1-unreviewed")
            val derivativeGenerationId = admitExternalOcrGeneration(cfg, evidenceArtifactId, parker.core.interfaces.DerivativeGenerationId("generation-hfr-ui-1-unreviewed"))
            val adapter = buildOwnerHttpAdapter(runtime, cfg)

            val discovered = adapter.discoverOcrDerivativeGenerations(evidenceArtifactId).single()
            assertEquals("UNREVIEWED", discovered.humanReviewState)
            val inspected = assertIs<parker.ui.TierBOcrContentRetrievalResult.Retrieved>(
                adapter.retrieveTierBOcrContent(evidenceArtifactId, derivativeGenerationId),
            )
            assertEquals(listOf("UNREVIEWED"), inspected.content.humanReviewStates)
        } finally {
            runtime.shutdown()
        }
    }

    @Test
    fun `a review recorded for one generation does not alter another generation's displayed status -- exact-target isolation`() = runTest {
        val scriptDir = Files.createTempDirectory("build-owner-http-adapter-scripts-hfr-ui-1-gen-isolation")
        val cfg = config(writeFakeBridgeScript(scriptDir, successJson()).toString())
        val runtime = ParkerRuntime(cfg, RecordingParkerLogger())
        runtime.start()
        try {
            val evidenceArtifactId = EvidenceArtifactId("evidence-hfr-ui-1-gen-isolation")
            val reviewedGenerationId = admitExternalOcrGeneration(cfg, evidenceArtifactId, parker.core.interfaces.DerivativeGenerationId("generation-hfr-ui-1-reviewed"))
            val otherGenerationId = admitExternalOcrGeneration(cfg, evidenceArtifactId, parker.core.interfaces.DerivativeGenerationId("generation-hfr-ui-1-other"))
            val adapter = buildOwnerHttpAdapter(runtime, cfg)

            assertIs<parker.ui.OwnerHumanFidelityReviewRecordingOutcome.Recorded>(
                adapter.recordHumanFidelityReview(
                    evidenceArtifactId, reviewedGenerationId,
                    parker.ui.OwnerHumanFidelityReviewSubmission("HUMAN_REVIEWED_PASS", listOf(1), "Faithful and accurate."),
                ),
            )

            val discovered = adapter.discoverOcrDerivativeGenerations(evidenceArtifactId).associateBy { it.derivativeGenerationId }
            assertEquals("HUMAN_REVIEWED_PASS", discovered.getValue(reviewedGenerationId.value).humanReviewState)
            assertEquals("UNREVIEWED", discovered.getValue(otherGenerationId.value).humanReviewState, "generation B must not inherit generation A's review")
        } finally {
            runtime.shutdown()
        }
    }

    @Test
    fun `a review recorded for one evidence artefact does not alter another evidence artefact's displayed status`() = runTest {
        val scriptDir = Files.createTempDirectory("build-owner-http-adapter-scripts-hfr-ui-1-evidence-isolation")
        val cfg = config(writeFakeBridgeScript(scriptDir, successJson()).toString())
        val runtime = ParkerRuntime(cfg, RecordingParkerLogger())
        runtime.start()
        try {
            val evidenceA = EvidenceArtifactId("evidence-hfr-ui-1-a")
            val evidenceB = EvidenceArtifactId("evidence-hfr-ui-1-b")
            val generationA = admitExternalOcrGeneration(cfg, evidenceA, parker.core.interfaces.DerivativeGenerationId("generation-hfr-ui-1-evidence-a"))
            val generationB = admitExternalOcrGeneration(cfg, evidenceB, parker.core.interfaces.DerivativeGenerationId("generation-hfr-ui-1-evidence-b"))
            val adapter = buildOwnerHttpAdapter(runtime, cfg)

            assertIs<parker.ui.OwnerHumanFidelityReviewRecordingOutcome.Recorded>(
                adapter.recordHumanFidelityReview(
                    evidenceA, generationA,
                    parker.ui.OwnerHumanFidelityReviewSubmission("HUMAN_REVIEWED_PASS", listOf(1), "Faithful and accurate."),
                ),
            )

            assertEquals("HUMAN_REVIEWED_PASS", adapter.discoverOcrDerivativeGenerations(evidenceA).single().humanReviewState)
            assertEquals("UNREVIEWED", adapter.discoverOcrDerivativeGenerations(evidenceB).single().humanReviewState, "evidence B must not inherit evidence A's review")
        } finally {
            runtime.shutdown()
        }
    }

    @Test
    fun `discovery and inspection status retrieval perform no HFR write and no evidence-derivative mutation`() = runTest {
        val scriptDir = Files.createTempDirectory("build-owner-http-adapter-scripts-hfr-ui-1-no-write")
        val cfg = config(writeFakeBridgeScript(scriptDir, successJson()).toString())
        val runtime = ParkerRuntime(cfg, RecordingParkerLogger())
        runtime.start()
        try {
            val evidenceArtifactId = EvidenceArtifactId("evidence-hfr-ui-1-no-write")
            val derivativeGenerationId = admitExternalOcrGeneration(cfg, evidenceArtifactId, parker.core.interfaces.DerivativeGenerationId("generation-hfr-ui-1-no-write"))
            val adapter = buildOwnerHttpAdapter(runtime, cfg)

            val generationBytesBefore = Files.readAllBytes(Path.of(cfg.derivativeGenerationStorageRootPath).resolve("${derivativeGenerationId.value}.derivative"))
            val contentBytesBefore = Files.readAllBytes(Path.of(cfg.derivativeContentStorageRootPath).resolve("${derivativeGenerationId.value}.content"))
            val reviewRoot = Path.of(requireNotNull(cfg.humanFidelityReviewStorageRootPath))
            fun reviewFileCount() = Files.list(reviewRoot).use { it.filter(Files::isRegularFile).count() }
            val reviewCountBefore = reviewFileCount()

            repeat(3) {
                adapter.discoverOcrDerivativeGenerations(evidenceArtifactId)
                adapter.retrieveTierBOcrContent(evidenceArtifactId, derivativeGenerationId)
                adapter.effectiveHumanFidelityReview(evidenceArtifactId, derivativeGenerationId)
            }

            assertEquals(reviewCountBefore, reviewFileCount(), "read-only status display must never write an HFR record")
            assertContentEquals(
                generationBytesBefore,
                Files.readAllBytes(Path.of(cfg.derivativeGenerationStorageRootPath).resolve("${derivativeGenerationId.value}.derivative")),
            )
            assertContentEquals(
                contentBytesBefore,
                Files.readAllBytes(Path.of(cfg.derivativeContentStorageRootPath).resolve("${derivativeGenerationId.value}.content")),
            )
        } finally {
            runtime.shutdown()
        }
    }

    @Test
    fun `dual-composition parity -- both Main-kt and OwnerUiRuntimeComposition-kt supply the same real discovery dependency`() {
        // UI-INGESTION-8C's root cause was these two independent, deliberately-separate
        // OwnerUiEvidenceRuntimeAdapter(...) constructions (see
        // OwnerUiRuntimeCompositionTest's own "preserves capability and isolation boundaries"
        // test, which requires this file's construction to remain a distinct, independently
        // auditable source-of-truth from the desktop launcher's -- unifying them would weaken that
        // guarantee) silently drifting apart: one received discoverOcrDerivativeGenerationsAsOwner,
        // the other did not. This does not unify the two constructions -- it only proves both now
        // independently supply the same real dependency, exactly like every other capability both
        // already share (importEvidenceFileAsOwner, invokeTierAIngestionAsOwner, etc).
        val mainSource = Path.of("src/composition/Main.kt").toFile().readText()
        val compositionSource = Path.of("src/composition/OwnerUiRuntimeComposition.kt").toFile().readText()

        assertTrue(
            mainSource.contains("discoverOcrDerivativeGenerationsAsOwner = runtime::discoverOcrDerivativeGenerationsAsOwner"),
            "Main.kt (the real production entry point) must wire discoverOcrDerivativeGenerationsAsOwner",
        )
        assertTrue(
            compositionSource.contains("discoverOcrDerivativeGenerationsAsOwner = runtime::discoverOcrDerivativeGenerationsAsOwner"),
            "OwnerUiRuntimeComposition.kt must also wire discoverOcrDerivativeGenerationsAsOwner",
        )
    }

    @Test
    fun `dual-composition parity -- both Main-kt and OwnerUiRuntimeComposition-kt supply the same real HFR Owner UI dependencies`() {
        // HFR Owner UI exposure scope lock amendment: the exact same dual-composition trap this
        // class already guards against for OCR derivative discovery also applies here -- both
        // composition roots must independently wire recordHumanFidelityReviewAsOwner and
        // projectEffectiveHumanFidelityReviewAsOwner, or one of them silently falls back to the
        // adapter's safe null default and the capability is dead in whichever entry point misses it.
        val mainSource = Path.of("src/composition/Main.kt").toFile().readText()
        val compositionSource = Path.of("src/composition/OwnerUiRuntimeComposition.kt").toFile().readText()

        listOf("recordHumanFidelityReviewAsOwner", "projectEffectiveHumanFidelityReviewAsOwner").forEach { member ->
            assertTrue(
                mainSource.contains("$member = runtime::$member"),
                "Main.kt (the real production entry point) must wire $member",
            )
            assertTrue(
                compositionSource.contains("$member = runtime::$member"),
                "OwnerUiRuntimeComposition.kt must also wire $member",
            )
        }
    }
}
