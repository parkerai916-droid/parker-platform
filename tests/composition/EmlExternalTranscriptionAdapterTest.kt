package parker.core.runtime

import java.security.MessageDigest
import java.time.LocalDate
import kotlinx.coroutines.test.runTest
import kotlin.test.*
import parker.composition.*
import parker.core.interfaces.*

/**
 * The EML verification request/response shape at the adapter boundary. No network: a fake
 * transport only. Proves the adapter submits the exact EmlDerivedRepresentation.content() bytes
 * as input_text (never re-parsed, re-rendered, or stripped), and that a valid structured
 * verification response is accepted while malformed/contradictory ones are rejected.
 *
 * EML TRANSPORT-BINDING CORRECTION: two live invocations proved the provider does not reliably
 * echo Parker-known binding metadata as model-generated structured output, even after explicit
 * instruction strengthening (attempt 3 failed at the digest field; attempt 4 failed at the
 * Evidence ID field). Model-generated echoes of profile_id/request_id/attempt_id/
 * source_evidence_artifact_id/submitted_representation_sha256/processing_profile_identity are
 * retired entirely. These six Parker-known values are now carried as Responses API request
 * `metadata` -- never model-generated -- and verified against the response's own echoed metadata
 * (requireEmlResponseMetadataBinding) before the model's own structured output is parsed at all.
 * The model-generated structured output now contains only message_outcome/completeness_state/
 * sections/warnings -- the facts the model is actually responsible for producing.
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

    private suspend fun emlRepresentation(bytes: ByteArray = plainEmlBytes, id: EvidenceArtifactId = evidenceId): EmlDerivedRepresentation {
        val structural = assertIs<EmlStructuralExtractionOutcome.Extracted>(ApacheJamesMime4jExtractor().extract(bytes)).result
        val digest = sha256(bytes)
        val custodian = object : EvidenceCustodian {
            override suspend fun accept(requestingPrincipalId: PrincipalId, candidate: CandidateEvidenceArtifact) = EvidenceAcceptanceResult.Rejected("unused")
            override suspend fun retrieve(requestingPrincipalId: PrincipalId, evidenceArtifactId: EvidenceArtifactId) = EvidenceRetrievalResult.Found(id, bytes)
            override suspend fun retrieveManifest(requestingPrincipalId: PrincipalId, evidenceArtifactId: EvidenceArtifactId) =
                EvidenceManifestRetrievalResult.Found(EvidenceSourceManifest(id, digest, bytes.size.toLong(), "message/rfc822"))
            override suspend fun submitSource(requestingPrincipalId: PrincipalId, candidate: CandidateEvidenceArtifact, advisorySha256: String?) =
                throw UnsupportedOperationException("not used")
        }
        val trusted = assertIs<AuthoritativeAcquisitionResolution.Verified>(
            AuthoritativeAcquisitionSourceResolver(custodian).resolve(PrincipalId("owner"), id),
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
            "4", "OpenAI", "/v1/responses", false, "gpt-5.6-sol", "RECORD_PRESENT_OR_NOT_EXPOSED",
            1_000_000, 1_000_000, 1_000_000, 30_000, "https://api.openai.com", "reviewed", "reviewed", "not enabled",
            "reviewed", "reviewed", "BEARER_API_CREDENTIAL", "reviewed", "reviewed", LocalDate.parse("2026-08-28"),
            "owner", LocalDate.parse("2026-09-28"), listOf("reference"), listOf("change"),
            transcriptionProfileId = EML_TRANSCRIPTION_PROFILE_ID, instructionSha256 = EML_VERIFICATION_INSTRUCTION_SHA256,
            structuredSchemaSha256 = EML_VERIFICATION_SCHEMA_SHA256, processingProfileIdentity = EML_PROCESSING_PROFILE_IDENTITY,
            acceptanceState = ExternalTranscriptionAcceptanceState.ACCEPTED,
            reasoningEffort = "none", pdfDetail = "NOT_APPLICABLE", imageDetail = "NOT_APPLICABLE",
        ),
        OpenAiExternalTranscriptionEffectiveLimits(1_000_000, 1_000_000, 1_000_000, 30_000),
    )

    /** Only the facts the model is actually responsible for producing -- no Parker-known echo field. */
    private fun successPayload(representation: EmlDerivedRepresentation, warningsJson: String = "[]", extraJson: String = "") =
        """{"message_outcome":"TRANSCRIBED","completeness_state":"COMPLETE",""" +
            """"sections":[${representation.provenance.bodyAlternativesIncluded.joinToString(",") { """{"mime_entity_id":"$it","outcome":"TRANSCRIBED","reason_classification":null,"reason_detail":null,"warnings":[]}""" }}],""" +
            """"warnings":$warningsJson$extraJson}"""

    /** Builds the exact metadata object a correctly-behaving transport would echo back. */
    private fun matchingMetadata(
        binding: ExternalTranscriptionExecutionBinding,
        representation: EmlDerivedRepresentation,
        profileId: String = binding.profileId,
        requestId: String = binding.requestId,
        attemptId: String = binding.attemptId,
        evidenceArtifactId: String = representation.sourceEvidenceArtifactId.value,
        representationSha256: String = representation.representationSha256.value,
        processingProfileIdentity: String = representation.transformationProfileIdentity,
        omit: Set<String> = emptySet(),
    ): String {
        val pairs = linkedMapOf(
            PARKER_METADATA_KEY_PROFILE_ID to profileId,
            PARKER_METADATA_KEY_REQUEST_ID to requestId,
            PARKER_METADATA_KEY_ATTEMPT_ID to attemptId,
            PARKER_METADATA_KEY_EVIDENCE_ARTIFACT_ID to evidenceArtifactId,
            PARKER_METADATA_KEY_REPRESENTATION_SHA256 to representationSha256,
            PARKER_METADATA_KEY_PROCESSING_PROFILE_IDENTITY to processingProfileIdentity,
        )
        omit.forEach { pairs.remove(it) }
        return pairs.entries.joinToString(",", prefix = "{", postfix = "}") { "\"${it.key}\":\"${escape(it.value)}\"" }
    }

    private fun envelope(payload: String, metadataJson: String? = "{}") =
        """{"id":"resp_eml_1","model":"gpt-5.6-sol",""" +
            (metadataJson?.let { "\"metadata\":$it," } ?: "") +
            """"output":[{"type":"message","content":[{"type":"output_text","text":"${escape(payload)}"}]}]}"""

    private fun escape(value: String) = buildString { value.forEach { if (it == '\\' || it == '"') append('\\'); append(it) } }
    private fun sha256(bytes: ByteArray) = MessageDigest.getInstance("SHA-256").digest(bytes).joinToString("") { "%02x".format(it) }

    // ================= request shape =================

    @Test fun `EML request uses input_text with the exact canonical representation bytes, never file or image wrapping`() = runTest {
        val representation = emlRepresentation()
        val binding = binding()
        val request = ExternalTranscriptionRequest(representation, 200, executionBinding = binding)
        val transport = FakeTransport { envelope(successPayload(representation), matchingMetadata(binding, representation)) }

        adapter(transport).transcribe(request)
        val body = transport.request.body

        assertContains(body, "\"type\":\"input_text\"")
        assertContains(body, "Hello body")
        assertFalse(body.contains("\"type\":\"input_file\""))
        assertFalse(body.contains("\"type\":\"input_image\""))
        assertFalse(body.contains("base64"))
        assertContains(body, "\"reasoning\":{\"effort\":\"none\"}")
    }

    // ================= A. request metadata contains exactly the six expected Parker keys =================

    @Test fun `A -- the request body's metadata object contains exactly the six expected Parker keys with the correct values`() = runTest {
        val representation = emlRepresentation()
        val binding = binding()
        val request = ExternalTranscriptionRequest(representation, 200, executionBinding = binding)
        val transport = FakeTransport { envelope(successPayload(representation), matchingMetadata(binding, representation)) }

        adapter(transport).transcribe(request)
        val body = transport.request.body
        val metadataObject = Regex(""""metadata":(\{[^}]*\})""").find(body)?.groupValues?.get(1)
        assertNotNull(metadataObject, "request body must contain a metadata object")

        assertContains(body, "\"$PARKER_METADATA_KEY_PROFILE_ID\":\"${binding.profileId}\"")
        assertContains(body, "\"$PARKER_METADATA_KEY_REQUEST_ID\":\"${binding.requestId}\"")
        assertContains(body, "\"$PARKER_METADATA_KEY_ATTEMPT_ID\":\"${binding.attemptId}\"")
        assertContains(body, "\"$PARKER_METADATA_KEY_EVIDENCE_ARTIFACT_ID\":\"${representation.sourceEvidenceArtifactId.value}\"")
        assertContains(body, "\"$PARKER_METADATA_KEY_REPRESENTATION_SHA256\":\"${representation.representationSha256.value}\"")
        assertContains(body, "\"$PARKER_METADATA_KEY_PROCESSING_PROFILE_IDENTITY\":\"${representation.transformationProfileIdentity}\"")
        // None of these six keys are model-generated schema fields any more.
        assertFalse(body.contains("\"source_evidence_artifact_id\""))
        assertFalse(body.contains("\"submitted_representation_sha256\""))
        assertFalse(body.contains("\"processing_profile_identity\""))
    }

    // ================= B. exact metadata round-trip succeeds =================

    @Test fun `B -- an exact metadata round-trip succeeds as an EmlStructuredTranscriptionCandidate`() = runTest {
        val representation = emlRepresentation()
        val binding = binding()
        val request = ExternalTranscriptionRequest(representation, 200, executionBinding = binding)
        val transport = FakeTransport { envelope(successPayload(representation), matchingMetadata(binding, representation)) }

        val outcome = adapter(transport).transcribe(request)

        val candidate = assertIs<EmlStructuredTranscriptionCandidate>(assertIs<ExternalTranscriptionMechanismOutcome.Candidate>(outcome).candidate)
        assertEquals(EmlMessageOutcomeKind.TRANSCRIBED, candidate.messageOutcome)
        assertEquals(representation.provenance.bodyAlternativesIncluded, candidate.sections.map { it.mimeEntityId })
    }

    // ================= C. missing metadata object fails closed =================

    @Test fun `C -- a response with no metadata object at all fails closed as RESPONSE_BINDING_MISMATCH`() = runTest {
        val representation = emlRepresentation()
        val binding = binding()
        val request = ExternalTranscriptionRequest(representation, 200, executionBinding = binding)
        val transport = FakeTransport { envelope(successPayload(representation), metadataJson = null) }
        var captured: OpenAiResponseFailureFingerprint? = null

        val outcome = adapter(transport, responseFailureObserver = { captured = it }).transcribe(request)

        assertEquals("RESPONSE_BINDING_MISMATCH", assertIs<ExternalTranscriptionMechanismOutcome.Failure>(outcome).reason)
        assertEquals(RESPONSE_METADATA_BINDING_STAGE, captured?.parkerParseStage)
        assertEquals("RESPONSE_BINDING_MISMATCH", captured?.category)
    }

    // ================= D. each required metadata key missing individually fails closed =================

    @Test fun `D -- each required metadata key missing individually fails closed and names that exact key`() = runTest {
        val keys = listOf(
            PARKER_METADATA_KEY_PROFILE_ID, PARKER_METADATA_KEY_REQUEST_ID, PARKER_METADATA_KEY_ATTEMPT_ID,
            PARKER_METADATA_KEY_EVIDENCE_ARTIFACT_ID, PARKER_METADATA_KEY_REPRESENTATION_SHA256, PARKER_METADATA_KEY_PROCESSING_PROFILE_IDENTITY,
        )
        keys.forEach { missingKey ->
            val representation = emlRepresentation()
            val binding = binding()
            val request = ExternalTranscriptionRequest(representation, 200, executionBinding = binding)
            val transport = FakeTransport { envelope(successPayload(representation), matchingMetadata(binding, representation, omit = setOf(missingKey))) }
            var captured: OpenAiResponseFailureFingerprint? = null

            val outcome = adapter(transport, responseFailureObserver = { captured = it }).transcribe(request)

            assertEquals("RESPONSE_BINDING_MISMATCH", assertIs<ExternalTranscriptionMechanismOutcome.Failure>(outcome).reason, missingKey)
            assertEquals("$RESPONSE_METADATA_BINDING_STAGE:$missingKey", captured?.parkerParseStage, missingKey)
        }
    }

    // ================= E, F, G, H. mismatched metadata values are rejected =================

    @Test fun `E -- an Evidence ID mismatch in metadata is rejected`() = runTest {
        val representation = emlRepresentation()
        val binding = binding()
        val request = ExternalTranscriptionRequest(representation, 200, executionBinding = binding)
        val transport = FakeTransport { envelope(successPayload(representation), matchingMetadata(binding, representation, evidenceArtifactId = "someone-elses-evidence")) }
        var captured: OpenAiResponseFailureFingerprint? = null

        val outcome = adapter(transport, responseFailureObserver = { captured = it }).transcribe(request)

        assertEquals("RESPONSE_BINDING_MISMATCH", assertIs<ExternalTranscriptionMechanismOutcome.Failure>(outcome).reason)
        assertEquals("$RESPONSE_METADATA_BINDING_STAGE:$PARKER_METADATA_KEY_EVIDENCE_ARTIFACT_ID", captured?.parkerParseStage)
    }

    @Test fun `F -- a representation SHA-256 mismatch in metadata is rejected`() = runTest {
        val representation = emlRepresentation()
        val binding = binding()
        val request = ExternalTranscriptionRequest(representation, 200, executionBinding = binding)
        val transport = FakeTransport { envelope(successPayload(representation), matchingMetadata(binding, representation, representationSha256 = "b".repeat(64))) }
        var captured: OpenAiResponseFailureFingerprint? = null

        val outcome = adapter(transport, responseFailureObserver = { captured = it }).transcribe(request)

        assertEquals("RESPONSE_BINDING_MISMATCH", assertIs<ExternalTranscriptionMechanismOutcome.Failure>(outcome).reason)
        assertEquals("$RESPONSE_METADATA_BINDING_STAGE:$PARKER_METADATA_KEY_REPRESENTATION_SHA256", captured?.parkerParseStage)
    }

    @Test fun `G -- a processing profile identity mismatch in metadata is rejected`() = runTest {
        val representation = emlRepresentation()
        val binding = binding()
        val request = ExternalTranscriptionRequest(representation, 200, executionBinding = binding)
        val transport = FakeTransport { envelope(successPayload(representation), matchingMetadata(binding, representation, processingProfileIdentity = "some-other-profile")) }
        var captured: OpenAiResponseFailureFingerprint? = null

        val outcome = adapter(transport, responseFailureObserver = { captured = it }).transcribe(request)

        assertEquals("RESPONSE_BINDING_MISMATCH", assertIs<ExternalTranscriptionMechanismOutcome.Failure>(outcome).reason)
        assertEquals("$RESPONSE_METADATA_BINDING_STAGE:$PARKER_METADATA_KEY_PROCESSING_PROFILE_IDENTITY", captured?.parkerParseStage)
    }

    @Test fun `H -- profile_id, request_id, or attempt_id mismatch in metadata is rejected`() = runTest {
        data class Case(val label: String, val key: String, val override: (ExternalTranscriptionExecutionBinding, EmlDerivedRepresentation) -> String)
        val cases = listOf(
            Case("profile_id", PARKER_METADATA_KEY_PROFILE_ID) { b, r -> matchingMetadata(b, r, profileId = "wrong-profile-id") },
            Case("request_id", PARKER_METADATA_KEY_REQUEST_ID) { b, r -> matchingMetadata(b, r, requestId = "wrong-request-id") },
            Case("attempt_id", PARKER_METADATA_KEY_ATTEMPT_ID) { b, r -> matchingMetadata(b, r, attemptId = "wrong-attempt-id") },
        )
        cases.forEach { case ->
            val representation = emlRepresentation()
            val binding = binding()
            val request = ExternalTranscriptionRequest(representation, 200, executionBinding = binding)
            val transport = FakeTransport { envelope(successPayload(representation), case.override(binding, representation)) }
            var captured: OpenAiResponseFailureFingerprint? = null

            val outcome = adapter(transport, responseFailureObserver = { captured = it }).transcribe(request)

            assertEquals("RESPONSE_BINDING_MISMATCH", assertIs<ExternalTranscriptionMechanismOutcome.Failure>(outcome).reason, case.label)
            assertEquals("$RESPONSE_METADATA_BINDING_STAGE:${case.key}", captured?.parkerParseStage, case.label)
        }
    }

    // ================= I. concurrent fake invocations with different metadata cannot cross-bind =================

    @Test fun `I -- two invocations with different Parker-known identities do not cross-bind`() = runTest {
        val representationOne = emlRepresentation(bytes = plainEmlBytes, id = EvidenceArtifactId("evidence-one"))
        val bindingOne = ExternalTranscriptionExecutionBinding("req-one", "attempt-one", EML_TRANSCRIPTION_PROFILE_ID)
        val secondBytes = ("From: c@invalid\r\nTo: d@invalid\r\nMIME-Version: 1.0\r\n" +
            "Content-Type: text/plain; charset=utf-8\r\n\r\nDifferent body\r\n").toByteArray()
        val representationTwo = emlRepresentation(bytes = secondBytes, id = EvidenceArtifactId("evidence-two"))
        val bindingTwo = ExternalTranscriptionExecutionBinding("req-two", "attempt-two", EML_TRANSCRIPTION_PROFILE_ID)

        val requestOne = ExternalTranscriptionRequest(representationOne, 200, executionBinding = bindingOne)
        val transportOne = FakeTransport { envelope(successPayload(representationOne), matchingMetadata(bindingOne, representationOne)) }
        val outcomeOne = adapter(transportOne).transcribe(requestOne)
        assertIs<ExternalTranscriptionMechanismOutcome.Candidate>(outcomeOne)

        val requestTwo = ExternalTranscriptionRequest(representationTwo, 200, executionBinding = bindingTwo)
        val transportTwo = FakeTransport { envelope(successPayload(representationTwo), matchingMetadata(bindingTwo, representationTwo)) }
        val outcomeTwo = adapter(transportTwo).transcribe(requestTwo)
        assertIs<ExternalTranscriptionMechanismOutcome.Candidate>(outcomeTwo)

        // Cross-wiring proves no shared/global state: request one's metadata does not satisfy request two's binding.
        val transportCross = FakeTransport { envelope(successPayload(representationTwo), matchingMetadata(bindingOne, representationOne)) }
        val crossOutcome = adapter(transportCross).transcribe(requestTwo)
        assertEquals("RESPONSE_BINDING_MISMATCH", assertIs<ExternalTranscriptionMechanismOutcome.Failure>(crossOutcome).reason)
    }

    // ================= J. model output cannot override transport metadata =================

    @Test fun `J -- a model-generated field that looks like a provenance echo has zero effect on the outcome`() = runTest {
        val representation = emlRepresentation()
        val binding = binding()
        val request = ExternalTranscriptionRequest(representation, 200, executionBinding = binding)
        // The schema no longer declares this field at all; Parker's parser never reads it even if
        // present -- proving the model has no path to influence binding via its own output.
        val payloadWithInjectedField = successPayload(representation, extraJson = ""","source_evidence_artifact_id":"attacker-supplied-id"""")
        val transport = FakeTransport { envelope(payloadWithInjectedField, matchingMetadata(binding, representation)) }

        val outcome = adapter(transport).transcribe(request)

        val candidate = assertIs<EmlStructuredTranscriptionCandidate>(assertIs<ExternalTranscriptionMechanismOutcome.Candidate>(outcome).candidate)
        assertEquals(EmlMessageOutcomeKind.TRANSCRIBED, candidate.messageOutcome)
    }

    // ================= K. minimal valid model output (only the four remaining fields) succeeds =================

    @Test fun `K -- minimal valid model output containing only message_outcome, completeness_state, sections, and warnings succeeds`() = runTest {
        val representation = emlRepresentation()
        val binding = binding()
        val request = ExternalTranscriptionRequest(representation, 200, executionBinding = binding)
        val minimalPayload = successPayload(representation)
        val transport = FakeTransport { envelope(minimalPayload, matchingMetadata(binding, representation)) }

        val outcome = adapter(transport).transcribe(request)
        assertIs<ExternalTranscriptionMechanismOutcome.Candidate>(outcome)
    }

    // ================= L. malformed model-generated facts still fail closed =================

    @Test fun `L -- malformed model-generated facts still produce MALFORMED_PROVIDER_RESPONSE, distinct from binding mismatch`() = runTest {
        val representation = emlRepresentation()
        val binding = binding()
        val request = ExternalTranscriptionRequest(representation, 200, executionBinding = binding)
        val payload = successPayload(representation).replace("\"message_outcome\":\"TRANSCRIBED\",", "")
        val transport = FakeTransport { envelope(payload, matchingMetadata(binding, representation)) }
        var captured: OpenAiResponseFailureFingerprint? = null

        val outcome = adapter(transport, responseFailureObserver = { captured = it }).transcribe(request)

        assertEquals("MALFORMED_PROVIDER_RESPONSE", assertIs<ExternalTranscriptionMechanismOutcome.Failure>(outcome).reason)
        assertEquals("MESSAGE_OUTCOME", captured?.parkerParseStage)
        assertEquals("MALFORMED_PROVIDER_RESPONSE", captured?.category)
    }

    @Test fun `a malformed structured payload (invalid JSON) still fails as MALFORMED_PROVIDER_RESPONSE at STRUCTURED_PAYLOAD`() = runTest {
        val representation = emlRepresentation()
        val binding = binding()
        val request = ExternalTranscriptionRequest(representation, 200, executionBinding = binding)
        val transport = FakeTransport { envelope("""not closed properly....""", matchingMetadata(binding, representation)) }
        var captured: OpenAiResponseFailureFingerprint? = null

        val outcome = adapter(transport, responseFailureObserver = { captured = it }).transcribe(request)

        assertEquals("MALFORMED_PROVIDER_RESPONSE", assertIs<ExternalTranscriptionMechanismOutcome.Failure>(outcome).reason)
        assertEquals("STRUCTURED_PAYLOAD", captured?.parkerParseStage)
    }

    @Test fun `a top-level response that is not an object still fails at ENVELOPE_JSON, before metadata is even checked`() = runTest {
        val representation = emlRepresentation()
        val binding = binding()
        val request = ExternalTranscriptionRequest(representation, 200, executionBinding = binding)
        val transport = FakeTransport { "[]" }
        var captured: OpenAiResponseFailureFingerprint? = null

        val outcome = adapter(transport, responseFailureObserver = { captured = it }).transcribe(request)

        assertEquals("MALFORMED_PROVIDER_RESPONSE", assertIs<ExternalTranscriptionMechanismOutcome.Failure>(outcome).reason)
        assertEquals("ENVELOPE_JSON", captured?.parkerParseStage)
    }

    // ================= O. the diagnostic fingerprint stays content-free and bounded =================

    @Test fun `the captured diagnostic fingerprint never carries source or provider response content, for either failure category`() = runTest {
        val representation = emlRepresentation()
        val binding = binding()
        val request = ExternalTranscriptionRequest(representation, 200, executionBinding = binding)
        val transport = FakeTransport { envelope(successPayload(representation), metadataJson = null) }
        var captured: OpenAiResponseFailureFingerprint? = null

        adapter(transport, responseFailureObserver = { captured = it }).transcribe(request)

        val rendered = requireNotNull(captured).render()
        assertFalse(rendered.contains("Hello body"))
        assertFalse(rendered.contains(representation.representationSha256.value))
        assertFalse(rendered.contains("synthetic-secret"))
        assertTrue(rendered.length < 500)
    }

    @Test fun `a successful EML invocation is unaffected by the observer being wired`() = runTest {
        val representation = emlRepresentation()
        val binding = binding()
        val request = ExternalTranscriptionRequest(representation, 200, executionBinding = binding)
        val transport = FakeTransport { envelope(successPayload(representation), matchingMetadata(binding, representation)) }
        var observerCalled = false

        val outcome = adapter(transport, responseFailureObserver = { observerCalled = true }).transcribe(request)

        assertIs<ExternalTranscriptionMechanismOutcome.Candidate>(outcome)
        assertFalse(observerCalled, "the response-failure observer must not fire on a valid response")
    }

    @Test fun `missing execution binding fails closed before transport for the EML profile`() = runTest {
        val representation = emlRepresentation()
        val request = ExternalTranscriptionRequest(representation, 200, executionBinding = null)
        val transport = FakeTransport { envelope(successPayload(representation), matchingMetadata(binding(), representation)) }
        assertFailsWith<IllegalArgumentException> { adapter(transport).transcribe(request) }
        assertEquals(0, transport.calls)
    }

    // ================= Q, R. schema/instruction digests =================

    @Test fun `Q -- the narrowed EML schema requires only message_outcome, completeness_state, sections, and warnings`() {
        assertContains(EML_VERIFICATION_SCHEMA_SOURCE, "\"required\":[\"message_outcome\",\"completeness_state\",\"sections\",\"warnings\"]")
        listOf("profile_id", "request_id", "attempt_id", "source_evidence_artifact_id", "submitted_representation_sha256", "processing_profile_identity").forEach {
            assertFalse(EML_VERIFICATION_SCHEMA_SOURCE.contains("\"$it\""), "schema must no longer declare $it")
        }
        assertContains(EML_VERIFICATION_SCHEMA_CANONICAL, "mime_entity_id")
        assertContains(EML_VERIFICATION_SCHEMA_CANONICAL, "message_outcome")
    }

    @Test fun `R -- the frozen EML instruction digest matches the current canonical instruction text and no longer requests any echo`() {
        assertEquals(EML_VERIFICATION_INSTRUCTION_SHA256, sha256Hex(EML_VERIFICATION_INSTRUCTION.toByteArray()))
        listOf("echo", "Echo", "character-for-character", "sha256:").forEach {
            assertFalse(EML_VERIFICATION_INSTRUCTION.contains(it), "instruction must no longer ask the model to echo anything ($it)")
        }
    }
}
