package parker.core.runtime

import java.security.MessageDigest
import java.time.Instant
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue
import parker.core.interfaces.*

class ExternalTranscriptionOwnerInvocationCoordinatorTest {
    private val owner = PrincipalId("owner.external-test")
    private val evidenceId = EvidenceArtifactId("evidence-external-1")
    private val bytes = "bounded source".toByteArray()
    private val digest = sha256(bytes)
    private val events = mutableListOf<String>()

    private class FakePermission(private val outcome: PermissionDecisionOutcome, private val events: MutableList<String>) : PermissionEngine {
        lateinit var request: ExecutionRequest
        override suspend fun evaluate(request: ExecutionRequest): PermissionDecision {
            events += "authorize"
            this.request = request
            return PermissionDecision(DecisionId("decision-1"), request.principalId, request.targetResources.single(), PermissionAction.EXECUTE, outcome, PermissionLevel.AUTOMATIC, Instant.EPOCH)
        }
        override suspend fun explain(decisionId: DecisionId): PermissionExplanation = error("not used")
    }

    private inner class FakeCustodian(
        private val source: EvidenceRetrievalResult = EvidenceRetrievalResult.Found(evidenceId, bytes),
        private val manifest: EvidenceManifestRetrievalResult = EvidenceManifestRetrievalResult.Found(manifest()),
    ) : EvidenceCustodian {
        var sourceCalls = 0
        var manifestCalls = 0
        val ids = mutableListOf<EvidenceArtifactId>()
        override suspend fun accept(requestingPrincipalId: PrincipalId, candidate: CandidateEvidenceArtifact): EvidenceAcceptanceResult = error("not used")
        override suspend fun retrieve(requestingPrincipalId: PrincipalId, evidenceArtifactId: EvidenceArtifactId): EvidenceRetrievalResult {
            events += "source"; sourceCalls++; ids += evidenceArtifactId; return source
        }
        override suspend fun retrieveManifest(requestingPrincipalId: PrincipalId, evidenceArtifactId: EvidenceArtifactId): EvidenceManifestRetrievalResult {
            events += "manifest"; manifestCalls++; ids += evidenceArtifactId; return manifest
        }
    }

    private class FakeMechanism(
        private val events: MutableList<String>,
        private val outcome: (ExternalTranscriptionRequest) -> ExternalTranscriptionMechanismOutcome,
    ) : ExternalTranscriptionMechanism {
        var calls = 0
        lateinit var request: ExternalTranscriptionRequest
        override suspend fun transcribe(request: ExternalTranscriptionRequest): ExternalTranscriptionMechanismOutcome {
            events += "mechanism"; calls++; this.request = request; return outcome(request)
        }
    }

    @Test
    fun `authorization precedes custody and denial performs no custody or provider work`() = runTest {
        val permission = FakePermission(PermissionDecisionOutcome.DENIED, events)
        val custodian = FakeCustodian()
        val mechanism = FakeMechanism(events) { error("must not run") }

        val outcome = coordinator(permission, custodian, mechanism).invoke(evidenceId)

        assertIs<ExternalTranscriptionOwnerInvocationOutcome.NotAuthorised>(outcome)
        assertEquals(listOf("authorize"), events)
        assertEquals(0, custodian.sourceCalls)
        assertEquals(0, custodian.manifestCalls)
        assertEquals(0, mechanism.calls)
        assertEquals(owner, permission.request.principalId)
        assertEquals(evidenceId.value, permission.request.metadata[ExternalTranscriptionInvocationGate.EVIDENCE_ARTIFACT_ID_METADATA_KEY])
    }

    @Test
    fun `valid flow retrieves one identity verifies source and invokes one mechanism once`() = runTest {
        val permission = FakePermission(PermissionDecisionOutcome.APPROVED, events)
        val custodian = FakeCustodian()
        val mechanism = FakeMechanism(events) { ExternalTranscriptionMechanismOutcome.Candidate(candidate()) }

        val outcome = coordinator(permission, custodian, mechanism).invoke(evidenceId)

        assertIs<ExternalTranscriptionOwnerInvocationOutcome.Validated>(outcome)
        assertEquals(listOf("authorize", "source", "manifest", "mechanism"), events)
        assertEquals(listOf(evidenceId, evidenceId), custodian.ids)
        assertEquals(1, mechanism.calls)
        assertEquals(evidenceId, mechanism.request.sourceEvidenceArtifactId)
        assertContentEquals(bytes, mechanism.request.content)
        assertEquals(digest, mechanism.request.sourceManifestSha256.value)
    }

    @Test
    fun `missing source and manifest integrity failures stop before mechanism`() = runTest {
        val cases = listOf(
            FakeCustodian(source = EvidenceRetrievalResult.NotFound(evidenceId)) to ExternalTranscriptionOwnerInvocationOutcome.SourceNotFound::class,
            FakeCustodian(manifest = EvidenceManifestRetrievalResult.NotFound(evidenceId)) to ExternalTranscriptionOwnerInvocationOutcome.ManifestNotFound::class,
            FakeCustodian(manifest = EvidenceManifestRetrievalResult.Found(manifest(byteLength = bytes.size.toLong() + 1))) to ExternalTranscriptionOwnerInvocationOutcome.ByteLengthMismatch::class,
            FakeCustodian(manifest = EvidenceManifestRetrievalResult.Found(manifest(sha = "0".repeat(64)))) to ExternalTranscriptionOwnerInvocationOutcome.DigestMismatch::class,
            FakeCustodian(manifest = EvidenceManifestRetrievalResult.Found(manifest(media = "text/plain"))) to ExternalTranscriptionOwnerInvocationOutcome.UnsupportedOrOutOfBounds::class,
        )
        cases.forEach { (custodian, expected) ->
            events.clear()
            val mechanism = FakeMechanism(events) { error("must not run") }
            val result = coordinator(FakePermission(PermissionDecisionOutcome.APPROVED, events), custodian, mechanism).invoke(evidenceId)
            assertEquals(expected, result::class)
            assertEquals(0, mechanism.calls)
        }
    }

    @Test
    fun `provider failure does not retry and contradictory structured candidate rejects`() = runTest {
        val failureMechanism = FakeMechanism(events) { ExternalTranscriptionMechanismOutcome.Failure("provider unavailable") }
        assertIs<ExternalTranscriptionOwnerInvocationOutcome.MechanismFailure>(
            coordinator(FakePermission(PermissionDecisionOutcome.APPROVED, events), FakeCustodian(), failureMechanism).invoke(evidenceId),
        )
        assertEquals(1, failureMechanism.calls)

        events.clear()
        val invalid = candidate().copy(declaredReturnedPageScope = OcrPageScope(emptyList()))
        val invalidMechanism = FakeMechanism(events) { ExternalTranscriptionMechanismOutcome.Candidate(invalid) }
        assertIs<ExternalTranscriptionOwnerInvocationOutcome.ValidationRejected>(
            coordinator(FakePermission(PermissionDecisionOutcome.APPROVED, events), FakeCustodian(), invalidMechanism).invoke(evidenceId),
        )
        assertEquals(1, invalidMechanism.calls)
    }

    @Test
    fun `impossible uncertainty is rejected after one provider call`() = runTest {
        val span = OcrUncertaintySpan(1, 0, 100, OcrUncertaintyKind.UNCERTAIN, "outside returned text")
        val invalidPage = OcrStructuredPageCandidate(
            1,
            "short",
            OcrPageOutcomeKind.TRANSCRIBED_WITH_QUALIFICATIONS,
            uncertaintySpans = listOf(span),
        )
        val invalid = candidate().copy(pages = listOf(invalidPage))
        val mechanism = FakeMechanism(events) { ExternalTranscriptionMechanismOutcome.Candidate(invalid) }

        assertIs<ExternalTranscriptionOwnerInvocationOutcome.ValidationRejected>(
            coordinator(FakePermission(PermissionDecisionOutcome.APPROVED, events), FakeCustodian(), mechanism).invoke(evidenceId),
        )
        assertEquals(1, mechanism.calls)
    }

    @Test
    fun `missing page is made explicit and partial output does not retry`() = runTest {
        val partial = candidate().copy(pages = emptyList(), declaredReturnedPageScope = OcrPageScope(emptyList()))
        val mechanism = FakeMechanism(events) { ExternalTranscriptionMechanismOutcome.Candidate(partial) }
        val outcome = assertIs<ExternalTranscriptionOwnerInvocationOutcome.Validated>(
            coordinator(FakePermission(PermissionDecisionOutcome.APPROVED, events), FakeCustodian(), mechanism).invoke(evidenceId),
        )
        assertEquals(OcrPageOutcomeKind.NOT_RETURNED, outcome.validation.pageAccounting.pageOutcomes.single().outcome)
        assertEquals(1, mechanism.calls)
    }

    @Test
    fun `coordinator is structurally isolated from local OCR network storage UI and analysis`() {
        val types = ExternalTranscriptionOwnerInvocationCoordinator::class.java.declaredFields.map { it.type.name }
        val forbidden = listOf("Docling", "RapidOCR", "OpenAI", "Http", "Network", "Storage", "OwnerUi", "Analysis", "Memory", "Knowledge")
        types.forEach { type -> forbidden.forEach { assertTrue(!type.contains(it), "$type contains $it") } }
        assertTrue(ExternalTranscriptionOwnerInvocationCoordinator::class.java.declaredMethods.none { it.name.contains("retry", true) })
    }

    private fun coordinator(permission: PermissionEngine, custodian: EvidenceCustodian, mechanism: ExternalTranscriptionMechanism) =
        ExternalTranscriptionOwnerInvocationCoordinator(owner, permission, custodian, mechanism, OcrStructuredResultValidator())

    private fun manifest(sha: String = digest, byteLength: Long = bytes.size.toLong(), media: String? = "application/pdf") =
        EvidenceSourceManifest(evidenceId, sha, byteLength, media)

    private fun candidate(): OcrStructuredTranscriptionCandidate {
        val scope = OcrPageScope(listOf(1))
        return OcrStructuredTranscriptionCandidate(
            scope, scope, scope,
            listOf(OcrStructuredPageCandidate(1, "literal text", OcrPageOutcomeKind.TRANSCRIBED)),
            TranscriptionFidelity.UNVERIFIED_LITERAL_TRANSCRIPTION,
            OcrRecognitionIdentity("external", "literal-v1", "1.0.0"),
            OcrProviderProvenance("provider", "adapter", "1.0.0", "literal-v1", "model", OcrModelSnapshot.NotExposed, "provider-correlation"),
            OcrProcessingProvenance(evidenceId, OcrSha256Digest(digest), "application/pdf", bytes.size.toLong(), scope, scope, "application/pdf", bytes.size.toLong(), OcrSha256Digest(digest), true, "byte-exact-v1", Instant.EPOCH),
            Instant.EPOCH,
        )
    }

    private fun sha256(value: ByteArray) = MessageDigest.getInstance("SHA-256").digest(value).joinToString("") { "%02x".format(it) }
}
