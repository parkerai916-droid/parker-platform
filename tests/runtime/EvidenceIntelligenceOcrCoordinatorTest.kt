package parker.core.runtime

import java.security.MessageDigest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlinx.coroutines.test.runTest
import parker.core.interfaces.CandidateEvidenceArtifact
import parker.core.interfaces.EvidenceAcceptanceResult
import parker.core.interfaces.EvidenceArtifactId
import parker.core.interfaces.EvidenceCustodian
import parker.core.interfaces.EvidenceManifestRetrievalResult
import parker.core.interfaces.EvidenceRetrievalResult
import parker.core.interfaces.EvidenceSourceManifest
import parker.core.interfaces.OcrMechanism
import parker.core.interfaces.OcrRecognitionOutcome
import parker.core.interfaces.OcrRecognitionRequest
import parker.core.interfaces.PrincipalId

/**
 * OCR Mechanism, Unit 12 ("Runtime Composition"). Behavioural tests for
 * [EvidenceIntelligenceOcrCoordinator] -- proving the manifest-verified
 * integrity sequence (Implementation Plan Section 5.G), the "no digest
 * tautology" requirement, media-type eligibility, exactly-once
 * [OcrMechanism] invocation, and fault propagation. Mirrors
 * `TierAOwnerInvocationCoordinatorTest.kt`'s own established style for an
 * equivalent manifest-verified sequence.
 */
class EvidenceIntelligenceOcrCoordinatorTest {

    private val principalId = PrincipalId("owner-1")
    private val evidenceArtifactId = EvidenceArtifactId("evidence-1")
    private val content = "hello scanned document".toByteArray()

    private fun realSha256(bytes: ByteArray): String =
        MessageDigest.getInstance("SHA-256").digest(bytes).joinToString("") { "%02x".format(it) }

    private fun manifest(
        sha256: String = realSha256(content),
        byteLength: Long = content.size.toLong(),
        receivedMediaType: String? = "application/pdf",
    ) = EvidenceSourceManifest(
        evidenceArtifactId = evidenceArtifactId,
        sha256 = sha256,
        byteLength = byteLength,
        receivedMediaType = receivedMediaType,
    )

    private class FakeEvidenceCustodian(
        private val onRetrieveManifest: (PrincipalId, EvidenceArtifactId) -> EvidenceManifestRetrievalResult,
    ) : EvidenceCustodian {
        var retrieveManifestCallCount: Int = 0
            private set
        var retrieveCallCount: Int = 0
            private set
        var acceptCallCount: Int = 0
            private set

        override suspend fun accept(requestingPrincipalId: PrincipalId, candidate: CandidateEvidenceArtifact): EvidenceAcceptanceResult {
            acceptCallCount += 1
            throw UnsupportedOperationException("EvidenceIntelligenceOcrCoordinator must never call EvidenceCustodian.accept")
        }

        override suspend fun retrieve(requestingPrincipalId: PrincipalId, evidenceArtifactId: EvidenceArtifactId): EvidenceRetrievalResult {
            retrieveCallCount += 1
            throw UnsupportedOperationException(
                "EvidenceIntelligenceOcrCoordinator must never call EvidenceCustodian.retrieve -- content is " +
                    "already resolved by EvidenceIntelligenceInputResolver and passed in directly",
            )
        }

        override suspend fun retrieveManifest(requestingPrincipalId: PrincipalId, evidenceArtifactId: EvidenceArtifactId): EvidenceManifestRetrievalResult {
            retrieveManifestCallCount += 1
            return onRetrieveManifest(requestingPrincipalId, evidenceArtifactId)
        }
    }

    private class FakeOcrMechanism(
        private val onRecognise: (OcrRecognitionRequest) -> OcrRecognitionOutcome,
    ) : OcrMechanism {
        var invocationCount: Int = 0
            private set
        var lastRequest: OcrRecognitionRequest? = null
            private set

        override suspend fun recognise(request: OcrRecognitionRequest): OcrRecognitionOutcome {
            invocationCount += 1
            lastRequest = request
            return onRecognise(request)
        }
    }

    private fun sampleResult() = parker.core.interfaces.OcrRecognitionResult(
        recognisedText = "recognised text",
        fidelity = parker.core.interfaces.TranscriptionFidelity.VERBATIM,
        identity = parker.core.interfaces.OcrRecognitionIdentity(mechanismIdentity = "docling", configurationProfile = "test"),
        recognisedAt = java.time.Instant.EPOCH,
    )

    // ---- Manifest not found / rejected ----

    @Test
    fun `manifest not found -- no OcrMechanism invocation`() = runTest {
        val custodian = FakeEvidenceCustodian { _, id -> EvidenceManifestRetrievalResult.NotFound(id) }
        val ocrMechanism = FakeOcrMechanism { throw AssertionError("must not be called") }
        val coordinator = EvidenceIntelligenceOcrCoordinator(custodian, ocrMechanism)

        val outcome = coordinator.recognise(principalId, evidenceArtifactId, content)

        assertIs<OcrCoordinatorOutcome.ManifestNotFound>(outcome)
        assertEquals(0, ocrMechanism.invocationCount)
    }

    @Test
    fun `manifest rejected -- no OcrMechanism invocation`() = runTest {
        val custodian = FakeEvidenceCustodian { _, id -> EvidenceManifestRetrievalResult.Rejected(id, "denied") }
        val ocrMechanism = FakeOcrMechanism { throw AssertionError("must not be called") }
        val coordinator = EvidenceIntelligenceOcrCoordinator(custodian, ocrMechanism)

        val outcome = coordinator.recognise(principalId, evidenceArtifactId, content)

        val rejected = assertIs<OcrCoordinatorOutcome.ManifestRejected>(outcome)
        assertEquals("denied", rejected.reason)
        assertEquals(0, ocrMechanism.invocationCount)
    }

    // ---- Byte-length / digest mismatch ----

    @Test
    fun `byte-length mismatch -- no OcrMechanism invocation`() = runTest {
        val custodian = FakeEvidenceCustodian { _, _ -> EvidenceManifestRetrievalResult.Found(manifest(byteLength = content.size.toLong() + 1)) }
        val ocrMechanism = FakeOcrMechanism { throw AssertionError("must not be called") }
        val coordinator = EvidenceIntelligenceOcrCoordinator(custodian, ocrMechanism)

        val outcome = coordinator.recognise(principalId, evidenceArtifactId, content)

        assertIs<OcrCoordinatorOutcome.ByteLengthMismatch>(outcome)
        assertEquals(0, ocrMechanism.invocationCount)
    }

    @Test
    fun `digest mismatch -- no OcrMechanism invocation, no digest tautology`() = runTest {
        // The manifest's own sha256 is deliberately wrong (not recomputed from content) -- proving
        // the coordinator compares against the manifest's own custody-established value, never a
        // digest it recomputes from content and then compares to itself (which could never fail).
        val custodian = FakeEvidenceCustodian { _, _ -> EvidenceManifestRetrievalResult.Found(manifest(sha256 = "0".repeat(64))) }
        val ocrMechanism = FakeOcrMechanism { throw AssertionError("must not be called") }
        val coordinator = EvidenceIntelligenceOcrCoordinator(custodian, ocrMechanism)

        val outcome = coordinator.recognise(principalId, evidenceArtifactId, content)

        assertIs<OcrCoordinatorOutcome.DigestMismatch>(outcome)
        assertEquals(0, ocrMechanism.invocationCount)
    }

    @Test
    fun `matching digest and byte length proceed to eligibility check -- proving the comparison is genuinely exercised, not vacuously true`() = runTest {
        val custodian = FakeEvidenceCustodian { _, _ -> EvidenceManifestRetrievalResult.Found(manifest()) }
        val ocrMechanism = FakeOcrMechanism { OcrRecognitionOutcome.Recognised(sampleResult()) }
        val coordinator = EvidenceIntelligenceOcrCoordinator(custodian, ocrMechanism)

        val outcome = coordinator.recognise(principalId, evidenceArtifactId, content)

        assertIs<OcrCoordinatorOutcome.Recognised>(outcome)
        assertEquals(1, ocrMechanism.invocationCount)
    }

    // ---- Media-type eligibility ----

    @Test
    fun `missing receivedMediaType -- not OCR-eligible, no OcrMechanism invocation`() = runTest {
        val custodian = FakeEvidenceCustodian { _, _ -> EvidenceManifestRetrievalResult.Found(manifest(receivedMediaType = null)) }
        val ocrMechanism = FakeOcrMechanism { throw AssertionError("must not be called") }
        val coordinator = EvidenceIntelligenceOcrCoordinator(custodian, ocrMechanism)

        val outcome = coordinator.recognise(principalId, evidenceArtifactId, content)

        val notEligible = assertIs<OcrCoordinatorOutcome.NotOcrEligible>(outcome)
        assertNull(notEligible.mediaType)
        assertEquals(0, ocrMechanism.invocationCount)
    }

    @Test
    fun `non-image, non-pdf receivedMediaType -- not OCR-eligible, no OcrMechanism invocation`() = runTest {
        val custodian = FakeEvidenceCustodian { _, _ -> EvidenceManifestRetrievalResult.Found(manifest(receivedMediaType = "text/csv")) }
        val ocrMechanism = FakeOcrMechanism { throw AssertionError("must not be called") }
        val coordinator = EvidenceIntelligenceOcrCoordinator(custodian, ocrMechanism)

        val outcome = coordinator.recognise(principalId, evidenceArtifactId, content)

        assertIs<OcrCoordinatorOutcome.NotOcrEligible>(outcome)
        assertEquals(0, ocrMechanism.invocationCount)
    }

    @Test
    fun `image media type is OCR-eligible`() = runTest {
        val custodian = FakeEvidenceCustodian { _, _ -> EvidenceManifestRetrievalResult.Found(manifest(receivedMediaType = "image/png")) }
        val ocrMechanism = FakeOcrMechanism { OcrRecognitionOutcome.Recognised(sampleResult()) }
        val coordinator = EvidenceIntelligenceOcrCoordinator(custodian, ocrMechanism)

        val outcome = coordinator.recognise(principalId, evidenceArtifactId, content)

        assertIs<OcrCoordinatorOutcome.Recognised>(outcome)
        assertEquals(1, ocrMechanism.invocationCount)
    }

    // ---- Exactly-once invocation, exact request construction ----

    @Test
    fun `OcrMechanism is invoked exactly once, with the exact custodied bytes, media type, and no page count`() = runTest {
        val custodian = FakeEvidenceCustodian { _, _ -> EvidenceManifestRetrievalResult.Found(manifest(receivedMediaType = "application/pdf")) }
        val ocrMechanism = FakeOcrMechanism { OcrRecognitionOutcome.Recognised(sampleResult()) }
        val coordinator = EvidenceIntelligenceOcrCoordinator(custodian, ocrMechanism)

        coordinator.recognise(principalId, evidenceArtifactId, content)

        assertEquals(1, ocrMechanism.invocationCount)
        val request = ocrMechanism.lastRequest!!
        assertEquals(evidenceArtifactId, request.sourceEvidenceId)
        assertTrue(content.contentEquals(request.content))
        assertEquals("application/pdf", request.mediaType)
        assertEquals(null, request.pageCount)
    }

    @Test
    fun `EvidenceCustodian retrieve and accept are never called by this coordinator`() = runTest {
        val custodian = FakeEvidenceCustodian { _, _ -> EvidenceManifestRetrievalResult.Found(manifest()) }
        val ocrMechanism = FakeOcrMechanism { OcrRecognitionOutcome.Recognised(sampleResult()) }
        val coordinator = EvidenceIntelligenceOcrCoordinator(custodian, ocrMechanism)

        coordinator.recognise(principalId, evidenceArtifactId, content)

        assertEquals(0, custodian.retrieveCallCount)
        assertEquals(0, custodian.acceptCallCount)
        assertEquals(1, custodian.retrieveManifestCallCount)
    }

    // ---- Outcome pass-through, unmodified ----

    @Test
    fun `the OcrMechanism outcome is returned unchanged, whatever it is`() = runTest {
        val custodian = FakeEvidenceCustodian { _, _ -> EvidenceManifestRetrievalResult.Found(manifest()) }
        val expected = OcrRecognitionOutcome.ProcessingOrDependencyFailure("timed out after 900000ms")
        val ocrMechanism = FakeOcrMechanism { expected }
        val coordinator = EvidenceIntelligenceOcrCoordinator(custodian, ocrMechanism)

        val outcome = coordinator.recognise(principalId, evidenceArtifactId, content)

        val recognised = assertIs<OcrCoordinatorOutcome.Recognised>(outcome)
        assertEquals(expected, recognised.outcome)
    }

    // ---- Fault propagation ----

    @Test
    fun `a genuine fault the OcrMechanism throws propagates unchanged, never caught`() = runTest {
        val custodian = FakeEvidenceCustodian { _, _ -> EvidenceManifestRetrievalResult.Found(manifest()) }
        val fault = IllegalStateException("Docling bridge process exited with code 1")
        val ocrMechanism = FakeOcrMechanism { throw fault }
        val coordinator = EvidenceIntelligenceOcrCoordinator(custodian, ocrMechanism)

        val thrown = assertFailsWith<IllegalStateException> {
            coordinator.recognise(principalId, evidenceArtifactId, content)
        }
        assertEquals(fault, thrown)
    }

    // ---- No PermissionEngine dependency (structural) ----

    @Test
    fun `EvidenceIntelligenceOcrCoordinator declares exactly two constructor parameters -- evidenceCustodian and ocrMechanism`() {
        val constructor = EvidenceIntelligenceOcrCoordinator::class.java.declaredConstructors.single()
        val parameterTypes = constructor.parameterTypes.map { it.simpleName }.toSet()

        assertEquals(setOf("EvidenceCustodian", "OcrMechanism"), parameterTypes)
    }
}
