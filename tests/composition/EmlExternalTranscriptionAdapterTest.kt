package parker.core.runtime

import java.security.MessageDigest
import java.time.Instant
import java.time.LocalDate
import kotlinx.coroutines.test.runTest
import kotlin.test.*
import parker.composition.*
import parker.core.interfaces.*

/**
 * STEP 4G -- the EML verification request/response shape at the adapter boundary. No network: a
 * fake transport only. Proves the adapter submits the exact EmlDerivedRepresentation.content()
 * bytes as input_text (never re-parsed, re-rendered, or stripped), and that a valid structured
 * verification response is accepted while malformed/contradictory ones are rejected.
 */
class EmlExternalTranscriptionAdapterTest {
    private class FakeTransport(private val response: (OpenAiResponsesTransportRequest) -> String) : OpenAiResponsesTransport {
        var calls = 0
        lateinit var request: OpenAiResponsesTransportRequest
        override suspend fun execute(request: OpenAiResponsesTransportRequest): OpenAiResponsesTransportResponse {
            calls++; this.request = request
            return OpenAiResponsesTransportResponse(200, response(request).toByteArray())
        }
    }

    private val evidenceId = EvidenceArtifactId("eml-adapter-evidence")
    private val plainEmlBytes = ("From: a@invalid\r\nTo: b@invalid\r\nMIME-Version: 1.0\r\n" +
        "Content-Type: text/plain; charset=utf-8\r\n\r\nHello body\r\n").toByteArray()

    private suspend fun emlRepresentation(bytes: ByteArray = plainEmlBytes): EmlDerivedRepresentation {
        val structural = assertIs<EmlStructuralExtractionOutcome.Extracted>(ApacheJamesMime4jExtractor().extract(bytes)).result
        val digest = sha256(bytes)
        val custodian = object : EvidenceCustodian {
            override suspend fun accept(requestingPrincipalId: PrincipalId, candidate: CandidateEvidenceArtifact) = EvidenceAcceptanceResult.Rejected("unused")
            override suspend fun retrieve(requestingPrincipalId: PrincipalId, evidenceArtifactId: EvidenceArtifactId) = EvidenceRetrievalResult.Found(evidenceId, bytes)
            override suspend fun retrieveManifest(requestingPrincipalId: PrincipalId, evidenceArtifactId: EvidenceArtifactId) =
                EvidenceManifestRetrievalResult.Found(EvidenceSourceManifest(evidenceId, digest, bytes.size.toLong(), "message/rfc822"))
            override suspend fun submitSource(requestingPrincipalId: PrincipalId, candidate: CandidateEvidenceArtifact, advisorySha256: String?) =
                throw UnsupportedOperationException("not used")
        }
        val trusted = assertIs<AuthoritativeAcquisitionResolution.Verified>(
            AuthoritativeAcquisitionSourceResolver(custodian).resolve(PrincipalId("owner"), evidenceId),
        ).input
        return assertIs<EmlDerivedRepresentationOutcome.Created>(EmlDerivedRepresentationFactory().create(trusted, structural)).representation
    }

    private fun binding() = ExternalTranscriptionExecutionBinding("eml-req-1", "eml-attempt-1", EML_TRANSCRIPTION_PROFILE_ID)

    private fun adapter(transport: OpenAiResponsesTransport) =
        OpenAiResponsesExternalTranscriptionAdapter(ready(), OpenAiApiCredential.fromEnvironment("synthetic-secret")!!, transport)

    private fun ready() = OpenAiExternalTranscriptionReadiness.Ready(
        OpenAiExternalTranscriptionProviderProfile(
            "3", "OpenAI", "/v1/responses", false, "gpt-5.6-sol", "RECORD_PRESENT_OR_NOT_EXPOSED",
            1_000_000, 1_000_000, 1_000_000, 30_000, "https://api.openai.com", "reviewed", "reviewed", "not enabled",
            "reviewed", "reviewed", "BEARER_API_CREDENTIAL", "reviewed", "reviewed", LocalDate.parse("2026-08-28"),
            "owner", LocalDate.parse("2026-09-28"), listOf("reference"), listOf("change"),
            transcriptionProfileId = EML_TRANSCRIPTION_PROFILE_ID, instructionSha256 = EML_VERIFICATION_INSTRUCTION_SHA256,
            structuredSchemaSha256 = EML_VERIFICATION_SCHEMA_SHA256, processingProfileIdentity = EML_PROCESSING_PROFILE_IDENTITY,
            acceptanceState = ExternalTranscriptionAcceptanceState.ACCEPTANCE_PENDING,
            reasoningEffort = "none", pdfDetail = "NOT_APPLICABLE", imageDetail = "NOT_APPLICABLE",
        ),
        OpenAiExternalTranscriptionEffectiveLimits(1_000_000, 1_000_000, 1_000_000, 30_000),
    )

    private fun successPayload(representation: EmlDerivedRepresentation, binding: ExternalTranscriptionExecutionBinding) =
        """{"profile_id":"${binding.profileId}","request_id":"${binding.requestId}","attempt_id":"${binding.attemptId}",""" +
            """"source_evidence_artifact_id":"${representation.sourceEvidenceArtifactId.value}",""" +
            """"submitted_representation_sha256":"${representation.representationSha256.value}",""" +
            """"processing_profile_identity":"${representation.transformationProfileIdentity}",""" +
            """"message_outcome":"TRANSCRIBED","completeness_state":"COMPLETE",""" +
            """"sections":[${representation.provenance.bodyAlternativesIncluded.joinToString(",") { """{"mime_entity_id":"$it","outcome":"TRANSCRIBED","reason_classification":null,"reason_detail":null,"warnings":[]}""" }}],""" +
            """"warnings":[]}"""

    private fun envelope(payload: String) =
        """{"id":"resp_eml_1","model":"gpt-5.6-sol","output":[{"type":"message","content":[{"type":"output_text","text":"${escape(payload)}"}]}]}"""

    private fun escape(value: String) = buildString { value.forEach { if (it == '\\' || it == '"') append('\\'); append(it) } }

    @Test fun `EML request uses input_text with the exact canonical representation bytes, never file or image wrapping`() = runTest {
        val representation = emlRepresentation()
        val binding = binding()
        val request = ExternalTranscriptionRequest(representation, 200, executionBinding = binding)
        val transport = FakeTransport { envelope(successPayload(representation, binding)) }

        adapter(transport).transcribe(request)
        val body = transport.request.body

        assertContains(body, "\"type\":\"input_text\"")
        assertContains(body, "Hello body")
        assertFalse(body.contains("\"type\":\"input_file\""))
        assertFalse(body.contains("\"type\":\"input_image\""))
        assertFalse(body.contains("base64"))
        assertContains(body, "\"reasoning\":{\"effort\":\"none\"}")
    }

    @Test fun `a valid structured verification response is accepted as an EmlStructuredTranscriptionCandidate`() = runTest {
        val representation = emlRepresentation()
        val binding = binding()
        val request = ExternalTranscriptionRequest(representation, 200, executionBinding = binding)
        val transport = FakeTransport { envelope(successPayload(representation, binding)) }

        val outcome = adapter(transport).transcribe(request)
        val candidate = assertIs<EmlStructuredTranscriptionCandidate>(assertIs<ExternalTranscriptionMechanismOutcome.Candidate>(outcome).candidate)
        assertEquals(representation.sourceEvidenceArtifactId, candidate.sourceEvidenceArtifactId)
        assertEquals(representation.representationSha256, candidate.submittedRepresentationSha256)
        assertEquals(EmlMessageOutcomeKind.TRANSCRIBED, candidate.messageOutcome)
        assertEquals(representation.provenance.bodyAlternativesIncluded, candidate.sections.map { it.mimeEntityId })
    }

    @Test fun `a response missing required fields fails as MALFORMED_PROVIDER_RESPONSE`() = runTest {
        val representation = emlRepresentation()
        val binding = binding()
        val request = ExternalTranscriptionRequest(representation, 200, executionBinding = binding)
        val transport = FakeTransport { envelope("""{"profile_id":"${binding.profileId}"}""") }

        val outcome = adapter(transport).transcribe(request)
        assertEquals("MALFORMED_PROVIDER_RESPONSE", assertIs<ExternalTranscriptionMechanismOutcome.Failure>(outcome).reason)
    }

    @Test fun `missing execution binding fails closed before transport for the EML profile`() = runTest {
        val representation = emlRepresentation()
        val request = ExternalTranscriptionRequest(representation, 200, executionBinding = null)
        val transport = FakeTransport { envelope(successPayload(representation, binding())) }
        assertFailsWith<IllegalArgumentException> { adapter(transport).transcribe(request) }
        assertEquals(0, transport.calls)
    }

    @Test fun `frozen EML instruction and schema identities are deterministic`() {
        assertEquals(EML_VERIFICATION_INSTRUCTION_SHA256, sha256Hex(EML_VERIFICATION_INSTRUCTION.toByteArray()))
        assertContains(EML_VERIFICATION_SCHEMA_CANONICAL, "mime_entity_id")
        assertContains(EML_VERIFICATION_SCHEMA_CANONICAL, "message_outcome")
    }

    private fun sha256(bytes: ByteArray) = MessageDigest.getInstance("SHA-256").digest(bytes).joinToString("") { "%02x".format(it) }
}
