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

    private fun adapter(transport: OpenAiResponsesTransport, responseFailureObserver: (OpenAiResponseFailureFingerprint) -> Unit = {}) =
        OpenAiResponsesExternalTranscriptionAdapter(
            ready(), OpenAiApiCredential.fromEnvironment("synthetic-secret")!!, transport,
            responseFailureObserver = responseFailureObserver,
        )

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

    private fun successPayload(
        representation: EmlDerivedRepresentation, binding: ExternalTranscriptionExecutionBinding,
        sourceEvidenceArtifactId: String = representation.sourceEvidenceArtifactId.value,
        submittedRepresentationSha256: String = representation.representationSha256.value,
        processingProfileIdentity: String = representation.transformationProfileIdentity,
        warningsJson: String = "[]",
    ) = """{"profile_id":"${binding.profileId}","request_id":"${binding.requestId}","attempt_id":"${binding.attemptId}",""" +
            """"source_evidence_artifact_id":"$sourceEvidenceArtifactId",""" +
            """"submitted_representation_sha256":"$submittedRepresentationSha256",""" +
            """"processing_profile_identity":"$processingProfileIdentity",""" +
            """"message_outcome":"TRANSCRIBED","completeness_state":"COMPLETE",""" +
            """"sections":[${representation.provenance.bodyAlternativesIncluded.joinToString(",") { """{"mime_entity_id":"$it","outcome":"TRANSCRIBED","reason_classification":null,"reason_detail":null,"warnings":[]}""" }}],""" +
            """"warnings":$warningsJson}"""

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

    // ================= Live EML response-parse diagnostics =================

    @Test fun `A -- a malformed response at MESSAGE_OUTCOME fails as MALFORMED_PROVIDER_RESPONSE and the observer receives that exact stage`() = runTest {
        val representation = emlRepresentation()
        val binding = binding()
        val request = ExternalTranscriptionRequest(representation, 200, executionBinding = binding)
        val payload = successPayload(representation, binding).replace("\"message_outcome\":\"TRANSCRIBED\",", "")
        val transport = FakeTransport { envelope(payload) }
        var captured: OpenAiResponseFailureFingerprint? = null

        val outcome = adapter(transport, responseFailureObserver = { captured = it }).transcribe(request)

        assertEquals("MALFORMED_PROVIDER_RESPONSE", assertIs<ExternalTranscriptionMechanismOutcome.Failure>(outcome).reason)
        assertEquals("MESSAGE_OUTCOME", captured?.parkerParseStage)
    }

    @Test fun `B -- a malformed response at STRUCTURED_PAYLOAD (invalid JSON in the structured text) records that exact stage`() = runTest {
        val representation = emlRepresentation()
        val binding = binding()
        val request = ExternalTranscriptionRequest(representation, 200, executionBinding = binding)
        val transport = FakeTransport { envelope("""{"profile_id": "not closed properly....""") }
        var captured: OpenAiResponseFailureFingerprint? = null

        val outcome = adapter(transport, responseFailureObserver = { captured = it }).transcribe(request)

        assertEquals("MALFORMED_PROVIDER_RESPONSE", assertIs<ExternalTranscriptionMechanismOutcome.Failure>(outcome).reason)
        assertEquals("STRUCTURED_PAYLOAD", captured?.parkerParseStage)
    }

    @Test fun `C -- a failure at ENVELOPE_JSON (top-level response not an object) records that exact stage`() = runTest {
        val representation = emlRepresentation()
        val binding = binding()
        val request = ExternalTranscriptionRequest(representation, 200, executionBinding = binding)
        val transport = FakeTransport { "[]" }
        var captured: OpenAiResponseFailureFingerprint? = null

        val outcome = adapter(transport, responseFailureObserver = { captured = it }).transcribe(request)

        assertEquals("MALFORMED_PROVIDER_RESPONSE", assertIs<ExternalTranscriptionMechanismOutcome.Failure>(outcome).reason)
        assertEquals("ENVELOPE_JSON", captured?.parkerParseStage)
    }

    @Test fun `F -- the captured diagnostic fingerprint never carries source or provider response content`() = runTest {
        val representation = emlRepresentation()
        val binding = binding()
        val request = ExternalTranscriptionRequest(representation, 200, executionBinding = binding)
        val payload = successPayload(representation, binding).replace("\"message_outcome\":\"TRANSCRIBED\",", "")
        val transport = FakeTransport { envelope(payload) }
        var captured: OpenAiResponseFailureFingerprint? = null

        adapter(transport, responseFailureObserver = { captured = it }).transcribe(request)

        val rendered = requireNotNull(captured).render()
        assertFalse(rendered.contains("Hello body"))
        assertFalse(rendered.contains(representation.representationSha256.value))
        assertFalse(rendered.contains("synthetic-secret"))
        assertTrue(rendered.length < 500)
    }

    @Test fun `G -- a successful EML invocation is unaffected by the observer being wired`() = runTest {
        val representation = emlRepresentation()
        val binding = binding()
        val request = ExternalTranscriptionRequest(representation, 200, executionBinding = binding)
        val transport = FakeTransport { envelope(successPayload(representation, binding)) }
        var observerCalled = false

        val outcome = adapter(transport, responseFailureObserver = { observerCalled = true }).transcribe(request)

        assertIs<ExternalTranscriptionMechanismOutcome.Candidate>(outcome)
        assertFalse(observerCalled, "the response-failure observer must not fire on a valid response")
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

    // ================= EML echo-binding correction: precise CANDIDATE field stages =================

    @Test fun `L -- the strengthened instruction explicitly names all three echo fields and the exact-copy requirement`() {
        assertContains(EML_VERIFICATION_INSTRUCTION, "source evidence artifact identifier")
        assertContains(EML_VERIFICATION_INSTRUCTION, "submitted representation digest")
        assertContains(EML_VERIFICATION_INSTRUCTION, "processing profile identifier")
        assertContains(EML_VERIFICATION_INSTRUCTION, "character-for-character")
        assertContains(EML_VERIFICATION_INSTRUCTION, "64-character lowercase hexadecimal")
        assertContains(EML_VERIFICATION_INSTRUCTION, "no recalculation")
    }

    private suspend fun candidateFieldProbe(payload: String): Pair<ExternalTranscriptionMechanismOutcome, OpenAiResponseFailureFingerprint?> {
        val representation = emlRepresentation()
        val binding = binding()
        val request = ExternalTranscriptionRequest(representation, 200, executionBinding = binding)
        val transport = FakeTransport { envelope(payload) }
        var captured: OpenAiResponseFailureFingerprint? = null
        val outcome = adapter(transport, responseFailureObserver = { captured = it }).transcribe(request)
        return outcome to captured
    }

    @Test fun `A -- a malformed source_evidence_artifact_id (blank) records SOURCE_EVIDENCE_ARTIFACT_ID`() = runTest {
        val representation = emlRepresentation()
        val binding = binding()
        val (outcome, captured) = candidateFieldProbe(successPayload(representation, binding, sourceEvidenceArtifactId = ""))
        assertEquals("MALFORMED_PROVIDER_RESPONSE", assertIs<ExternalTranscriptionMechanismOutcome.Failure>(outcome).reason)
        assertEquals("SOURCE_EVIDENCE_ARTIFACT_ID", captured?.parkerParseStage)
    }

    @Test fun `B -- every malformed submitted_representation_sha256 variant records SUBMITTED_REPRESENTATION_SHA256`() = runTest {
        val representation = emlRepresentation()
        val binding = binding()
        val variants = listOf(
            "blank" to "",
            "63 lowercase hex chars" to "a".repeat(63),
            "64 uppercase hex chars" to "A".repeat(64),
            "64 non-hex chars" to "g".repeat(64),
            "ordinary prose" to "I have verified this message and it is authentic.",
        )
        variants.forEach { (label, value) ->
            val (outcome, captured) = candidateFieldProbe(successPayload(representation, binding, submittedRepresentationSha256 = value))
            assertEquals("MALFORMED_PROVIDER_RESPONSE", assertIs<ExternalTranscriptionMechanismOutcome.Failure>(outcome).reason, label)
            assertEquals("SUBMITTED_REPRESENTATION_SHA256", captured?.parkerParseStage, label)
        }
    }

    @Test fun `C -- a malformed processing_profile_identity (blank) records PROCESSING_PROFILE_IDENTITY`() = runTest {
        val representation = emlRepresentation()
        val binding = binding()
        val (outcome, captured) = candidateFieldProbe(successPayload(representation, binding, processingProfileIdentity = ""))
        assertEquals("MALFORMED_PROVIDER_RESPONSE", assertIs<ExternalTranscriptionMechanismOutcome.Failure>(outcome).reason)
        assertEquals("PROCESSING_PROFILE_IDENTITY", captured?.parkerParseStage)
    }

    @Test fun `D -- malformed warnings (wrong element type) records WARNINGS`() = runTest {
        val representation = emlRepresentation()
        val binding = binding()
        val (outcome, captured) = candidateFieldProbe(successPayload(representation, binding, warningsJson = "[123]"))
        assertEquals("MALFORMED_PROVIDER_RESPONSE", assertIs<ExternalTranscriptionMechanismOutcome.Failure>(outcome).reason)
        assertEquals("WARNINGS", captured?.parkerParseStage)
    }

    @Test fun `F -- a residual final-assembly failure still names CANDIDATE (missing top-level required field)`() = runTest {
        val representation = emlRepresentation()
        val binding = binding()
        // profile_id is required by EXECUTION_BINDING already; omitting an unrelated-but-required
        // top-level field the schema demands (message_outcome) still fails earlier, at
        // MESSAGE_OUTCOME -- confirming CANDIDATE itself is unreachable without every prior stage
        // succeeding, and is now reached only for true final-assembly problems, not echo fields.
        val (outcome, captured) = candidateFieldProbe(successPayload(representation, binding).replace("\"message_outcome\":\"TRANSCRIBED\",", ""))
        assertEquals("MALFORMED_PROVIDER_RESPONSE", assertIs<ExternalTranscriptionMechanismOutcome.Failure>(outcome).reason)
        assertEquals("MESSAGE_OUTCOME", captured?.parkerParseStage)
    }

    @Test fun `E -- an overlong provider-reported model identifier records PROVIDER_PROVENANCE`() = runTest {
        val representation = emlRepresentation()
        val binding = binding()
        val payload = successPayload(representation, binding)
        val overlongModel = "gpt-5.6-sol-" + "x".repeat(1_100)
        val envelopeWithOverlongModel =
            """{"id":"resp_eml_1","model":"$overlongModel","output":[{"type":"message","content":[{"type":"output_text","text":"${escape(payload)}"}]}]}"""
        val transport = FakeTransport { envelopeWithOverlongModel }
        var captured: OpenAiResponseFailureFingerprint? = null
        val outcome = adapter(transport, responseFailureObserver = { captured = it })
            .transcribe(ExternalTranscriptionRequest(representation, 200, executionBinding = binding))
        assertEquals("MALFORMED_PROVIDER_RESPONSE", assertIs<ExternalTranscriptionMechanismOutcome.Failure>(outcome).reason)
        assertEquals("PROVIDER_PROVENANCE", captured?.parkerParseStage)
    }

    @Test fun `G -- a valid 64-char lowercase digest succeeds through parsing`() = runTest {
        val representation = emlRepresentation()
        val binding = binding()
        val (outcome, _) = candidateFieldProbe(successPayload(representation, binding))
        val candidate = assertIs<EmlStructuredTranscriptionCandidate>(assertIs<ExternalTranscriptionMechanismOutcome.Candidate>(outcome).candidate)
        assertEquals(representation.representationSha256, candidate.submittedRepresentationSha256)
    }

    @Test fun `H -- a well-formed but wrong digest reaches the validator and is rejected as a contradiction, not a parse failure`() = runTest {
        val representation = emlRepresentation()
        val binding = binding()
        val wrongButWellFormed = "b".repeat(64)
        val (outcome, captured) = candidateFieldProbe(successPayload(representation, binding, submittedRepresentationSha256 = wrongButWellFormed))
        // Parsing must succeed -- the value is syntactically valid, so this is not a parse failure.
        val candidate = assertIs<EmlStructuredTranscriptionCandidate>(assertIs<ExternalTranscriptionMechanismOutcome.Candidate>(outcome).candidate)
        assertEquals(OcrSha256Digest(wrongButWellFormed), candidate.submittedRepresentationSha256)
        assertEquals(null, captured, "the observer must not fire when parsing succeeded")
        // The validator -- never the parser -- is what must catch this contradiction.
        val validated = EmlStructuredResultValidator().validate(candidate, representation, binding)
        val rejected = assertIs<EmlStructuredValidationOutcome.Rejected>(validated)
        assertContains(rejected.reason, "digest does not match")
    }

    private fun sha256(bytes: ByteArray) = MessageDigest.getInstance("SHA-256").digest(bytes).joinToString("") { "%02x".format(it) }
}
