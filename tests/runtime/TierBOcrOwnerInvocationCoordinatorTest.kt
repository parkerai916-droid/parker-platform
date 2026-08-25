package parker.core.runtime

import java.nio.file.Files
import java.security.MessageDigest
import java.time.Instant
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlinx.coroutines.test.runTest
import parker.core.interfaces.CandidateEvidenceArtifact
import parker.core.interfaces.DecisionId
import parker.core.interfaces.EvidenceAcceptanceResult
import parker.core.interfaces.EvidenceArtifactId
import parker.core.interfaces.EvidenceCustodian
import parker.core.interfaces.EvidenceManifestRetrievalResult
import parker.core.interfaces.EvidenceRetrievalResult
import parker.core.interfaces.EvidenceSourceManifest
import parker.core.interfaces.ExecutionRequest
import parker.core.interfaces.OcrMechanism
import parker.core.interfaces.OcrRecognitionIdentity
import parker.core.interfaces.OcrRecognitionOutcome
import parker.core.interfaces.OcrRecognitionRequest
import parker.core.interfaces.OcrRecognitionResult
import parker.core.interfaces.PermissionAction
import parker.core.interfaces.PermissionDecision
import parker.core.interfaces.PermissionDecisionOutcome
import parker.core.interfaces.PermissionEngine
import parker.core.interfaces.PermissionExplanation
import parker.core.interfaces.PermissionLevel
import parker.core.interfaces.PrincipalId
import parker.core.interfaces.ResourceId
import parker.core.interfaces.TierBOcrOwnerInvocationOutcome
import parker.core.interfaces.TranscriptionFidelity

/**
 * Document Ingestion — Tier B Durable OCR Derivative Content. Behavioural
 * tests for [TierBOcrOwnerInvocationCoordinator] -- Permission Engine
 * gating (Tier B scope lock §9), the mandatory-provenance fail-closed gate
 * (§11), and admissible-vs-non-admissible OCR outcome classification
 * (§13). Uses a real [EvidenceIntelligenceOcrCoordinator] and a real,
 * filesystem-backed [DerivativeGenerationCoordinator] (temp storage roots)
 * -- only [PermissionEngine]/[EvidenceCustodian]/[OcrMechanism] are faked,
 * mirroring [EvidenceIntelligenceOcrCoordinatorTest]'s own established
 * style.
 */
class TierBOcrOwnerInvocationCoordinatorTest {

    private val ownerPrincipalId = PrincipalId("owner-1")
    private val evidenceArtifactId = EvidenceArtifactId("evidence-1")
    private val content = "scanned document bytes".toByteArray()

    private fun realSha256(bytes: ByteArray): String =
        MessageDigest.getInstance("SHA-256").digest(bytes).joinToString("") { "%02x".format(it) }

    private fun manifest(receivedMediaType: String? = "image/png") = EvidenceSourceManifest(
        evidenceArtifactId = evidenceArtifactId,
        sha256 = realSha256(content),
        byteLength = content.size.toLong(),
        receivedMediaType = receivedMediaType,
    )

    private class FakeEvidenceCustodian(
        private val manifestResult: EvidenceManifestRetrievalResult,
        private val retrieveResult: EvidenceRetrievalResult,
    ) : EvidenceCustodian {
        override suspend fun accept(requestingPrincipalId: PrincipalId, candidate: CandidateEvidenceArtifact): EvidenceAcceptanceResult =
            throw UnsupportedOperationException("must never be called")

        override suspend fun retrieve(requestingPrincipalId: PrincipalId, evidenceArtifactId: EvidenceArtifactId): EvidenceRetrievalResult =
            retrieveResult

        override suspend fun retrieveManifest(requestingPrincipalId: PrincipalId, evidenceArtifactId: EvidenceArtifactId): EvidenceManifestRetrievalResult =
            manifestResult
    }

    private class FakeOcrMechanism(private val onRecognise: (OcrRecognitionRequest) -> OcrRecognitionOutcome) : OcrMechanism {
        var invocationCount: Int = 0
            private set

        override suspend fun recognise(request: OcrRecognitionRequest): OcrRecognitionOutcome {
            invocationCount += 1
            return onRecognise(request)
        }
    }

    private class FakePermissionEngine(private val outcome: PermissionDecisionOutcome) : PermissionEngine {
        var evaluateCallCount: Int = 0
            private set

        override suspend fun evaluate(request: ExecutionRequest): PermissionDecision {
            evaluateCallCount += 1
            return PermissionDecision(
                decisionId = DecisionId("fake-decision"),
                principalId = request.principalId,
                resourceId = request.targetResources.first(),
                action = PermissionAction.EXECUTE,
                decision = outcome,
                level = PermissionLevel.AUTOMATIC,
                timestamp = Instant.EPOCH,
            )
        }

        override suspend fun explain(decisionId: DecisionId): PermissionExplanation =
            throw UnsupportedOperationException("must never be called")
    }

    private fun realDerivativeGenerationCoordinator(): DerivativeGenerationCoordinator {
        val generationRoot = Files.createTempDirectory("tierb-coordinator-generation")
        val contentRoot = Files.createTempDirectory("tierb-coordinator-content")
        val auditPath = Files.createTempDirectory("tierb-coordinator-audit").resolve("audit.log")
        return DerivativeGenerationCoordinator(
            csvExtractor = ApacheCommonsCsvExtractor(),
            storage = FileSystemDerivativeGenerationStorage(generationRoot),
            audit = FileSystemDocumentIngestionAudit(auditPath),
            contentStorage = FileSystemDerivativeContentStorage(contentRoot),
        )
    }

    private fun identity(mechanismVersion: String? = "docling-2.5.0", modelIdentity: String? = "rapidocr-onnxruntime:PP-OCRv6_rec_small", modelVersion: String? = "sha256:" + "a".repeat(64)) =
        OcrRecognitionIdentity(
            mechanismIdentity = "docling",
            configurationProfile = "docling-bridge-v1",
            mechanismVersion = mechanismVersion,
            modelIdentity = modelIdentity,
            modelVersion = modelVersion,
        )

    private fun recognisedResult(identity: OcrRecognitionIdentity = identity()) = OcrRecognitionResult(
        recognisedText = "recognised text",
        fidelity = TranscriptionFidelity.VERBATIM,
        identity = identity,
        recognisedAt = Instant.EPOCH,
    )

    private fun coordinator(
        permissionOutcome: PermissionDecisionOutcome = PermissionDecisionOutcome.APPROVED,
        manifestResult: EvidenceManifestRetrievalResult = EvidenceManifestRetrievalResult.Found(manifest()),
        retrieveResult: EvidenceRetrievalResult = EvidenceRetrievalResult.Found(evidenceArtifactId, content),
        onRecognise: (OcrRecognitionRequest) -> OcrRecognitionOutcome = { OcrRecognitionOutcome.Recognised(recognisedResult()) },
    ): Triple<TierBOcrOwnerInvocationCoordinator, FakePermissionEngine, FakeOcrMechanism> {
        val permissionEngine = FakePermissionEngine(permissionOutcome)
        val custodian = FakeEvidenceCustodian(manifestResult, retrieveResult)
        val ocrMechanism = FakeOcrMechanism(onRecognise)
        val ocrCoordinator = EvidenceIntelligenceOcrCoordinator(custodian, ocrMechanism)
        val coordinator = TierBOcrOwnerInvocationCoordinator(custodian, permissionEngine, ocrCoordinator, realDerivativeGenerationCoordinator())
        return Triple(coordinator, permissionEngine, ocrMechanism)
    }

    // ---- Permission Engine gating (Tier B scope lock §9) ----

    @Test
    fun `permission denial -- zero OCR invocation, zero durable side effect`() = runTest {
        val (coordinator, permissionEngine, ocrMechanism) = coordinator(permissionOutcome = PermissionDecisionOutcome.DENIED)

        val outcome = coordinator.invoke(ownerPrincipalId, evidenceArtifactId, "corr-1")

        assertIs<TierBOcrOwnerInvocationOutcome.NotAuthorised>(outcome)
        assertEquals(1, permissionEngine.evaluateCallCount)
        assertEquals(0, ocrMechanism.invocationCount, "OCR must never be invoked when Permission Engine denies")
    }

    @Test
    fun `permission deferred -- treated as not authorised, zero OCR invocation`() = runTest {
        val (coordinator, _, ocrMechanism) = coordinator(permissionOutcome = PermissionDecisionOutcome.DEFERRED)

        val outcome = coordinator.invoke(ownerPrincipalId, evidenceArtifactId, "corr-1")

        assertIs<TierBOcrOwnerInvocationOutcome.NotAuthorised>(outcome)
        assertEquals(0, ocrMechanism.invocationCount)
    }

    @Test
    fun `permission approved -- OCR is invoked`() = runTest {
        val (coordinator, _, ocrMechanism) = coordinator()

        val outcome = coordinator.invoke(ownerPrincipalId, evidenceArtifactId, "corr-1")

        assertIs<TierBOcrOwnerInvocationOutcome.Admitted>(outcome)
        assertEquals(1, ocrMechanism.invocationCount)
    }

    @Test
    fun `permission approved with confirmation -- also proceeds`() = runTest {
        val (coordinator, _, _) = coordinator(permissionOutcome = PermissionDecisionOutcome.APPROVED_WITH_CONFIRMATION)

        val outcome = coordinator.invoke(ownerPrincipalId, evidenceArtifactId, "corr-1")

        assertIs<TierBOcrOwnerInvocationOutcome.Admitted>(outcome)
    }

    // ---- Mandatory-provenance fail-closed gate (Tier B scope lock §11) ----

    @Test
    fun `missing mechanismVersion -- MandatoryProvenanceUnavailable, no durable side effect`() = runTest {
        val (coordinator, _, _) = coordinator(onRecognise = {
            OcrRecognitionOutcome.Recognised(recognisedResult(identity(mechanismVersion = null)))
        })

        val outcome = coordinator.invoke(ownerPrincipalId, evidenceArtifactId, "corr-1")

        assertIs<TierBOcrOwnerInvocationOutcome.MandatoryProvenanceUnavailable>(outcome)
    }

    @Test
    fun `missing modelIdentity and modelVersion -- MandatoryProvenanceUnavailable, no durable side effect`() = runTest {
        val (coordinator, _, _) = coordinator(onRecognise = {
            OcrRecognitionOutcome.Recognised(recognisedResult(identity(modelIdentity = null, modelVersion = null)))
        })

        val outcome = coordinator.invoke(ownerPrincipalId, evidenceArtifactId, "corr-1")

        assertIs<TierBOcrOwnerInvocationOutcome.MandatoryProvenanceUnavailable>(outcome)
    }

    @Test
    fun `full truthful provenance -- admitted, producer identity maps every field truthfully`() = runTest {
        val (coordinator, _, _) = coordinator()

        val outcome = assertIs<TierBOcrOwnerInvocationOutcome.Admitted>(coordinator.invoke(ownerPrincipalId, evidenceArtifactId, "corr-1"))

        assertEquals("docling", outcome.record.producerIdentity.pluginIdentity)
        assertEquals("docling-2.5.0", outcome.record.producerIdentity.pluginVersion)
        assertEquals("docling-bridge-v1", outcome.record.producerIdentity.configurationIdentity)
        assertEquals("rapidocr-onnxruntime:PP-OCRv6_rec_small", outcome.record.producerIdentity.modelIdentity)
        assertEquals("sha256:" + "a".repeat(64), outcome.record.producerIdentity.modelVersion)
        assertNull(outcome.record.confidence, "confidence must never be persisted for Tier B (scope lock §16)")
    }

    // ---- Admissible vs. non-admissible OCR outcomes (Tier B scope lock §13) ----

    @Test
    fun `NoRecognisableContent -- OcrNotAdmissible, never durable`() = runTest {
        val (coordinator, _, _) = coordinator(onRecognise = { OcrRecognitionOutcome.NoRecognisableContent("no text found") })

        val outcome = coordinator.invoke(ownerPrincipalId, evidenceArtifactId, "corr-1")

        assertIs<TierBOcrOwnerInvocationOutcome.OcrNotAdmissible>(outcome)
    }

    @Test
    fun `PartialOrDegradedOutput -- admitted with PARTIAL_OR_DEGRADED outcome kind and degradation reason preserved`() = runTest {
        val (coordinator, _, _) = coordinator(onRecognise = {
            OcrRecognitionOutcome.PartialOrDegradedOutput(recognisedResult(), "page 3 could not be fully processed")
        })

        val outcome = assertIs<TierBOcrOwnerInvocationOutcome.Admitted>(coordinator.invoke(ownerPrincipalId, evidenceArtifactId, "corr-1"))

        assertEquals(parker.core.interfaces.OcrDerivativeOutcomeKind.PARTIAL_OR_DEGRADED, outcome.extracted.outcomeKind)
        assertEquals("page 3 could not be fully processed", outcome.extracted.degradationReason)
        assertTrue(outcome.record.warnings.contains("page 3 could not be fully processed"), "degradation reason must be carried alongside the record's own warnings (§14)")
    }

    @Test
    fun `ProcessingOrDependencyFailure -- OcrNotAdmissible, never durable`() = runTest {
        val (coordinator, _, _) = coordinator(onRecognise = { OcrRecognitionOutcome.ProcessingOrDependencyFailure("bridge crashed") })

        assertIs<TierBOcrOwnerInvocationOutcome.OcrNotAdmissible>(coordinator.invoke(ownerPrincipalId, evidenceArtifactId, "corr-1"))
    }

    // ---- Source integrity (via the real, unmodified EvidenceIntelligenceOcrCoordinator) ----

    @Test
    fun `not OCR-eligible media type -- NotOcrEligible, no OCR invocation`() = runTest {
        val (coordinator, _, ocrMechanism) = coordinator(manifestResult = EvidenceManifestRetrievalResult.Found(manifest(receivedMediaType = "text/csv")))

        val outcome = coordinator.invoke(ownerPrincipalId, evidenceArtifactId, "corr-1")

        assertIs<TierBOcrOwnerInvocationOutcome.NotOcrEligible>(outcome)
        assertEquals(0, ocrMechanism.invocationCount)
    }

    @Test
    fun `digest mismatch -- DigestMismatch, no OCR invocation`() = runTest {
        // Same length as `content` (so the coordinator's own byte-length check passes first),
        // different bytes -- isolates a genuine digest-only mismatch.
        val tampered = content.copyOf().also { it[0] = it[0].inc() }
        val (coordinator, _, ocrMechanism) = coordinator(retrieveResult = EvidenceRetrievalResult.Found(evidenceArtifactId, tampered))

        val outcome = coordinator.invoke(ownerPrincipalId, evidenceArtifactId, "corr-1")

        assertIs<TierBOcrOwnerInvocationOutcome.DigestMismatch>(outcome)
        assertEquals(0, ocrMechanism.invocationCount)
    }
}
