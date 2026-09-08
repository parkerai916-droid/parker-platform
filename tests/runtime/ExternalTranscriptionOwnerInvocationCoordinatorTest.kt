package parker.core.runtime

import java.security.MessageDigest
import java.time.Instant
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNotNull
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
        val principals = mutableListOf<PrincipalId>()
        override suspend fun accept(requestingPrincipalId: PrincipalId, candidate: CandidateEvidenceArtifact): EvidenceAcceptanceResult = error("not used")
        override suspend fun retrieve(requestingPrincipalId: PrincipalId, evidenceArtifactId: EvidenceArtifactId): EvidenceRetrievalResult {
            events += "source"; sourceCalls++; ids += evidenceArtifactId; principals += requestingPrincipalId; return source
        }
        override suspend fun retrieveManifest(requestingPrincipalId: PrincipalId, evidenceArtifactId: EvidenceArtifactId): EvidenceManifestRetrievalResult {
            events += "manifest"; manifestCalls++; ids += evidenceArtifactId; principals += requestingPrincipalId; return manifest
        }
        override suspend fun submitSource(requestingPrincipalId: PrincipalId, candidate: CandidateEvidenceArtifact, advisorySha256: String?): EvidenceSourceSubmissionResult =
            throw UnsupportedOperationException("submitSource not supported by this fake")
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

        val outcome = coordinator(permission, custodian, mechanism).invoke(owner, evidenceId)

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

        val outcome = coordinator(permission, custodian, mechanism).invoke(owner, evidenceId)

        assertIs<ExternalTranscriptionOwnerInvocationOutcome.Admitted>(outcome)
        assertEquals(listOf("authorize", "source", "manifest", "mechanism"), events)
        assertEquals(listOf(evidenceId, evidenceId), custodian.ids)
        assertEquals(1, mechanism.calls)
        assertEquals(evidenceId, mechanism.request.sourceEvidenceArtifactId)
        assertContentEquals(bytes, mechanism.request.content)
        assertEquals(digest, mechanism.request.sourceManifestSha256.value)
    }

    @Test
    fun `AG-1G -- invoke uses the supplied requestingPrincipalId throughout, never a fixed value`() = runTest {
        val hermesLikePrincipal = PrincipalId("agent.hermes-ingestion-operator")
        var admissionPrincipal: PrincipalId? = null
        val permission = FakePermission(PermissionDecisionOutcome.APPROVED, events)
        val custodian = FakeCustodian()
        val mechanism = FakeMechanism(events) { ExternalTranscriptionMechanismOutcome.Candidate(candidate()) }

        val outcome = coordinator(permission, custodian, mechanism) { admissionPrincipal = it }
            .invoke(hermesLikePrincipal, evidenceId)

        assertIs<ExternalTranscriptionOwnerInvocationOutcome.Admitted>(outcome)
        assertEquals(hermesLikePrincipal, permission.request.principalId, "the internal permission check must use the supplied principal")
        assertTrue(custodian.principals.isNotEmpty())
        custodian.principals.forEach { assertEquals(hermesLikePrincipal, it, "source resolution must use the supplied principal") }
        assertEquals(hermesLikePrincipal, admissionPrincipal, "durable admission must attribute to the supplied principal")

        // A second, distinct principal on a second call proves this is a genuine per-call
        // parameter, not a value memoised from the first call or otherwise fixed.
        events.clear()
        val ownerLikePrincipal = PrincipalId("user.owner-distinct")
        var secondAdmissionPrincipal: PrincipalId? = null
        val secondPermission = FakePermission(PermissionDecisionOutcome.APPROVED, events)
        val secondCustodian = FakeCustodian()
        val secondMechanism = FakeMechanism(events) { ExternalTranscriptionMechanismOutcome.Candidate(candidate()) }
        coordinator(secondPermission, secondCustodian, secondMechanism) { secondAdmissionPrincipal = it }
            .invoke(ownerLikePrincipal, evidenceId)
        assertEquals(ownerLikePrincipal, secondPermission.request.principalId)
        assertEquals(ownerLikePrincipal, secondAdmissionPrincipal)
    }

    @Test
    fun `missing source and manifest integrity failures stop before mechanism`() = runTest {
        val cases = listOf(
            FakeCustodian(source = EvidenceRetrievalResult.NotFound(evidenceId)) to ExternalTranscriptionOwnerInvocationOutcome.SourceNotFound::class,
            FakeCustodian(manifest = EvidenceManifestRetrievalResult.NotFound(evidenceId)) to ExternalTranscriptionOwnerInvocationOutcome.ManifestNotFound::class,
            FakeCustodian(manifest = EvidenceManifestRetrievalResult.Found(manifest(byteLength = bytes.size.toLong() + 1))) to ExternalTranscriptionOwnerInvocationOutcome.ByteLengthMismatch::class,
            FakeCustodian(manifest = EvidenceManifestRetrievalResult.Found(manifest(sha = "0".repeat(64)))) to ExternalTranscriptionOwnerInvocationOutcome.DigestMismatch::class,
            FakeCustodian(manifest = EvidenceManifestRetrievalResult.Found(manifest(media = "text/plain"))) to ExternalTranscriptionOwnerInvocationOutcome.UnsupportedOrOutOfBounds::class,
            // STEP 3 -- text/csv is the only newly-supported media type. message/rfc822 (EML) and
            // DOCX remain fail-closed, exactly as before this unit.
            FakeCustodian(manifest = EvidenceManifestRetrievalResult.Found(manifest(media = "message/rfc822"))) to ExternalTranscriptionOwnerInvocationOutcome.UnsupportedOrOutOfBounds::class,
            FakeCustodian(manifest = EvidenceManifestRetrievalResult.Found(manifest(media = "application/vnd.openxmlformats-officedocument.wordprocessingml.document"))) to ExternalTranscriptionOwnerInvocationOutcome.UnsupportedOrOutOfBounds::class,
        )
        cases.forEach { (custodian, expected) ->
            events.clear()
            val mechanism = FakeMechanism(events) { error("must not run") }
            val result = coordinator(FakePermission(PermissionDecisionOutcome.APPROVED, events), custodian, mechanism).invoke(owner, evidenceId)
            assertEquals(expected, result::class)
            assertEquals(0, mechanism.calls)
        }
    }

    @Test
    fun `STEP 3 -- valid UTF-8 CSV is admitted, byte-exact, and reaches the mechanism exactly once`() = runTest {
        val csvBytes = "name,value\nÉtoile,1\n".toByteArray(Charsets.UTF_8)
        val csvDigest = sha256(csvBytes)
        val custodian = FakeCustodian(
            source = EvidenceRetrievalResult.Found(evidenceId, csvBytes),
            manifest = EvidenceManifestRetrievalResult.Found(EvidenceSourceManifest(evidenceId, csvDigest, csvBytes.size.toLong(), "text/csv")),
        )
        val mechanism = FakeMechanism(events) { ExternalTranscriptionMechanismOutcome.Candidate(candidate(csvBytes, csvDigest, "text/csv")) }

        val outcome = coordinator(FakePermission(PermissionDecisionOutcome.APPROVED, events), custodian, mechanism).invoke(owner, evidenceId)

        assertIs<ExternalTranscriptionOwnerInvocationOutcome.Admitted>(outcome)
        assertEquals(1, mechanism.calls)
        assertContentEquals(csvBytes, mechanism.request.content)
        assertEquals("text/csv", mechanism.request.mediaType)
        assertEquals(csvDigest, mechanism.request.sourceManifestSha256.value)
    }

    @Test
    fun `STEP 3 -- malformed UTF-8 CSV fails closed before the mechanism is ever reached, with no fallback`() = runTest {
        // 0xC3 alone is an incomplete two-byte UTF-8 continuation sequence -- deterministically
        // malformed, not decodable by any strict UTF-8 decoder.
        val malformedBytes = byteArrayOf('a'.code.toByte(), 0xC3.toByte())
        val malformedDigest = sha256(malformedBytes)
        val custodian = FakeCustodian(
            source = EvidenceRetrievalResult.Found(evidenceId, malformedBytes),
            manifest = EvidenceManifestRetrievalResult.Found(EvidenceSourceManifest(evidenceId, malformedDigest, malformedBytes.size.toLong(), "text/csv")),
        )
        val mechanism = FakeMechanism(events) { error("must not run -- invalid encoding must fail closed before provider invocation") }

        val outcome = coordinator(FakePermission(PermissionDecisionOutcome.APPROVED, events), custodian, mechanism).invoke(owner, evidenceId)

        assertIs<ExternalTranscriptionOwnerInvocationOutcome.UnsupportedOrOutOfBounds>(outcome)
        assertEquals(0, mechanism.calls)
        assertEquals(listOf("authorize", "source", "manifest"), events)
    }

    @Test
    fun `provider failure does not retry and contradictory structured candidate rejects`() = runTest {
        val failureMechanism = FakeMechanism(events) { ExternalTranscriptionMechanismOutcome.Failure("provider unavailable") }
        assertIs<ExternalTranscriptionOwnerInvocationOutcome.MechanismFailure>(
            coordinator(FakePermission(PermissionDecisionOutcome.APPROVED, events), FakeCustodian(), failureMechanism).invoke(owner, evidenceId),
        )
        assertEquals(1, failureMechanism.calls)

        events.clear()
        val invalid = candidate().copy(declaredReturnedPageScope = OcrPageScope(emptyList()))
        val invalidMechanism = FakeMechanism(events) { ExternalTranscriptionMechanismOutcome.Candidate(invalid) }
        assertIs<ExternalTranscriptionOwnerInvocationOutcome.ValidationRejected>(
            coordinator(FakePermission(PermissionDecisionOutcome.APPROVED, events), FakeCustodian(), invalidMechanism).invoke(owner, evidenceId),
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
            coordinator(FakePermission(PermissionDecisionOutcome.APPROVED, events), FakeCustodian(), mechanism).invoke(owner, evidenceId),
        )
        assertEquals(1, mechanism.calls)
    }

    @Test
    fun `missing page is made explicit and partial output does not retry`() = runTest {
        val scope = OcrPageScope(listOf(1, 2))
        val partial = candidate().copy(
            requestedPageScope = scope,
            submittedPageScope = scope,
            declaredReturnedPageScope = OcrPageScope(listOf(1)),
            processingProvenance = candidate().processingProvenance.copy(requestedPageScope = scope, submittedPageScope = scope),
        )
        val mechanism = FakeMechanism(events) { ExternalTranscriptionMechanismOutcome.Candidate(partial) }
        val outcome = assertIs<ExternalTranscriptionOwnerInvocationOutcome.Admitted>(
            coordinator(FakePermission(PermissionDecisionOutcome.APPROVED, events), FakeCustodian(), mechanism).invoke(owner, evidenceId),
        )
        assertEquals(OcrPageOutcomeKind.NOT_RETURNED, outcome.extracted.pageAccounting!!.pageOutcomes.last().outcome)
        assertEquals(1, mechanism.calls)
    }

    @Test
    fun `coordinator is structurally isolated from local OCR network storage UI and analysis`() {
        val types = ExternalTranscriptionOwnerInvocationCoordinator::class.java.declaredFields.map { it.type.name }
        val forbidden = listOf("Docling", "RapidOCR", "OpenAI", "Http", "Network", "Storage", "OwnerUi", "Analysis", "Memory", "Knowledge")
        types.forEach { type -> forbidden.forEach { assertTrue(!type.contains(it), "$type contains $it") } }
        assertTrue(ExternalTranscriptionOwnerInvocationCoordinator::class.java.declaredMethods.none { it.name.contains("retry", true) })
    }

    // REAL-DOCUMENT-2F -- Wire Accepted External Transcription into Governed Acquisition. Proves,
    // without a real provider call, that the already-existing ExternalTranscriptionAcquisitionExecutor
    // (now registered in ParkerRuntime's production executor list) is a pure delegation wrapper: it
    // dispatches to exactly this already-governed coordinator and retains its existing invocation
    // gate (permission denial) and admission/provenance behaviour unchanged, never constructing a
    // second provider-invocation path of its own.

    @Test
    fun `the acquisition executor delegates to this exact coordinator and retains its permission gate -- denial performs no custody or provider work`() = runTest {
        val permission = FakePermission(PermissionDecisionOutcome.DENIED, events)
        val custodian = FakeCustodian()
        val mechanism = FakeMechanism(events) { error("must not run") }
        val request = executionRequest()

        val outcome = executor(coordinator(permission, custodian, mechanism)).execute(request)

        assertIs<BoundAcquisitionExecutorOutcome.ExecutionFailed>(outcome)
        assertEquals(listOf("authorize"), events)
        assertEquals(0, custodian.sourceCalls)
        assertEquals(0, custodian.manifestCalls)
        assertEquals(0, mechanism.calls)
    }

    @Test
    fun `the acquisition executor delegates to this exact coordinator and carries an admitted outcome's exact fidelity, generation id and provenance through unchanged`() = runTest {
        val permission = FakePermission(PermissionDecisionOutcome.APPROVED, events)
        val custodian = FakeCustodian()
        val mechanism = FakeMechanism(events) { ExternalTranscriptionMechanismOutcome.Candidate(candidate()) }
        val request = executionRequest()

        val outcome = executor(coordinator(permission, custodian, mechanism)).execute(request)

        val admitted = assertIs<BoundAcquisitionExecutorOutcome.Admitted>(outcome)
        assertEquals(listOf("authorize", "source", "manifest", "mechanism"), events)
        assertEquals(1, mechanism.calls)
        // The exact same fidelity/provenance shape "valid flow retrieves one identity verifies
        // source and invokes one mechanism once" (above) already proves a direct coordinator.invoke()
        // call produces for this identical candidate() -- carried through the executor unchanged.
        assertEquals(TranscriptionFidelity.UNVERIFIED_LITERAL_TRANSCRIPTION, admitted.fidelity)
        assertEquals(DerivativeGenerationId("generation-unit-j"), admitted.derivativeGenerationId)
        assertNotNull(admitted.processingProvenance)
    }

    @Test
    fun `governed acquisition invokes the fresh-binding boundary once per attempt with exact principal and evidence`() = runTest {
        val observed = mutableListOf<Pair<PrincipalId, EvidenceArtifactId>>()
        val generatedBindings = mutableListOf<ExternalTranscriptionExecutionBinding>()
        val executor = ExternalTranscriptionAcquisitionExecutor(
            AcquisitionExecutorBinding(
                ProductionAcquisitionCapabilityCatalogue.FIDELITY_FIRST_EXTERNAL_CAPABILITY_ID,
                EvidenceAcquisitionMechanism.EXTERNAL_TRANSCRIPTION,
                null,
            ),
        ) { principal, id ->
            observed += principal to id
            generatedBindings += ExternalTranscriptionExecutionBinding(
                "request-${generatedBindings.size}", "attempt-${generatedBindings.size}", "profile-accepted",
            )
            ExternalTranscriptionOwnerInvocationOutcome.MechanismFailure("controlled provider failure")
        }

        repeat(2) { assertIs<BoundAcquisitionExecutorOutcome.ExecutionFailed>(executor.execute(executionRequest())) }

        assertEquals(listOf(owner to evidenceId, owner to evidenceId), observed)
        assertEquals(2, generatedBindings.map { it.requestId }.distinct().size)
        assertEquals(2, generatedBindings.map { it.attemptId }.distinct().size)
    }

    @Test
    fun `the acquisition executor is structurally a pure delegation wrapper -- its only collaborator is the governed invocation function, never a provider mechanism`() {
        val fields = ExternalTranscriptionAcquisitionExecutor::class.java.declaredFields.filterNot { it.isSynthetic }
        assertTrue(fields.any { it.name.contains("invokeGovernedExternalTranscription") })
        val forbidden = listOf("Docling", "RapidOCR", "OpenAI", "Http", "Network", "Mechanism")
        fields.map { it.type.name }.forEach { type -> forbidden.forEach { assertTrue(!type.contains(it), "$type contains $it") } }
    }

    // ================= STEP 4G -- EML sibling: end-to-end coordinator dispatch =================

    private val emlBinding = ExternalTranscriptionExecutionBinding("eml-request-1", "eml-attempt-1", "eml-profile-1")
    private val plainEmlBytes = ("From: a@invalid\r\nTo: b@invalid\r\nMIME-Version: 1.0\r\n" +
        "Content-Type: text/plain; charset=utf-8\r\n\r\nHello body\r\n").toByteArray()

    private fun emlManifest(bytes: ByteArray) = EvidenceSourceManifest(evidenceId, sha256(bytes), bytes.size.toLong(), "message/rfc822")

    private fun emlCoordinator(
        permission: PermissionEngine,
        custodian: EvidenceCustodian,
        mechanism: ExternalTranscriptionMechanism,
        emlAdmission: EmlValidatedExternalVerificationAdmission = EmlValidatedExternalVerificationAdmission { id, structural, receipt, _, _ ->
            EmlExternalVerificationAdmissionOutcome.Admitted(
                DerivativeGenerationRecord(
                    DerivativeGenerationId("eml-generation-1"), id, listOf(DerivativeParentReference.RootEvidenceArtifact(id)),
                    "EML external verification receipt", receipt.producerIdentity,
                    listOf(DerivativeTransformation.MODEL_INFERENCE, DerivativeTransformation.STRUCTURAL_PARSING), receipt.recognisedAt,
                    DerivativeContentIdentity.NoCanonicalSerialization, receipt.completenessState, DerivativeOperationalOutcome.USABLE, receipt.warnings,
                ),
                receipt,
            )
        },
    ) = ExternalTranscriptionOwnerInvocationCoordinator(
        permission, custodian, mechanism, OcrStructuredResultValidator(), admission(),
        correlationFactory = { "eml-correlation-1" }, executionBinding = emlBinding, emlDurableAdmission = emlAdmission,
    )

    // EML TRANSPORT-BINDING CORRECTION: sourceEvidenceArtifactId/representationSha256/
    // processingProfileIdentity are no longer candidate fields at all -- Parker-known binding
    // identities are verified as Responses API metadata inside the adapter, before this
    // candidate is ever constructed. This fake mechanism (bypassing the real adapter entirely)
    // has no metadata-binding step to exercise, so this helper only needs section identities.
    private fun emlCandidateFor(sectionIds: List<String>) =
        EmlStructuredTranscriptionCandidate(
            messageOutcome = EmlMessageOutcomeKind.TRANSCRIBED,
            completenessState = DerivativeCompletenessState.ACCOUNTED_FOR,
            sections = sectionIds.map { EmlVerifiedSectionOutcome(it, OcrPageOutcomeKind.TRANSCRIBED) },
            recognitionIdentity = OcrRecognitionIdentity("openai-responses", "eml-profile-1", "2.0.0"),
            providerProvenance = OcrProviderProvenance("OpenAI", "openai-responses-adapter", "2.0.0", "eml-profile-1", "model", OcrModelSnapshot.NotExposed, "corr-1"),
            recognisedAt = Instant.EPOCH,
        )

    @Test fun `STEP 4G -- a plain-text EML is admitted end to end through the shared coordinator`() = runTest {
        val custodian = FakeCustodian(
            source = EvidenceRetrievalResult.Found(evidenceId, plainEmlBytes),
            manifest = EvidenceManifestRetrievalResult.Found(emlManifest(plainEmlBytes)),
        )
        lateinit var submittedText: String
        val mechanism = object : ExternalTranscriptionMechanism {
            override suspend fun transcribe(request: ExternalTranscriptionRequest): ExternalTranscriptionMechanismOutcome {
                submittedText = String(request.content, Charsets.UTF_8)
                val eml = request.representation as EmlDerivedRepresentation
                return ExternalTranscriptionMechanismOutcome.Candidate(
                    emlCandidateFor(eml.provenance.bodyAlternativesIncluded),
                )
            }
        }
        val outcome = emlCoordinator(FakePermission(PermissionDecisionOutcome.APPROVED, events), custodian, mechanism)
            .invoke(owner, evidenceId)
        val admitted = assertIs<ExternalTranscriptionOwnerInvocationOutcome.EmlAdmitted>(outcome)
        assertEquals(DerivativeGenerationId("eml-generation-1"), admitted.record.derivativeGenerationId)
        assertTrue(submittedText.contains("Hello body"), "adapter must receive the exact canonical text, not raw EML bytes")
        assertFalse(submittedText.contains("Content-Transfer-Encoding"), "canonical text is the projection, not the raw MIME headers of the source bytes themselves incidentally")
    }

    @Test fun `STEP 4G -- authorization missing fails closed for an EML source exactly as for any other`() = runTest {
        val custodian = FakeCustodian(
            source = EvidenceRetrievalResult.Found(evidenceId, plainEmlBytes),
            manifest = EvidenceManifestRetrievalResult.Found(emlManifest(plainEmlBytes)),
        )
        val mechanism = FakeMechanism(events) { error("must not run -- authorization must be checked first") }
        val outcome = emlCoordinator(FakePermission(PermissionDecisionOutcome.DENIED, events), custodian, mechanism).invoke(owner, evidenceId)
        assertIs<ExternalTranscriptionOwnerInvocationOutcome.NotAuthorised>(outcome)
        assertEquals(0, mechanism.calls)
    }

    @Test fun `STEP 4G -- malformed EML derivative preparation fails closed before any provider invocation, no fallback`() = runTest {
        val malformed = "not a valid rfc822 message at all with no headers".toByteArray()
        val custodian = FakeCustodian(
            source = EvidenceRetrievalResult.Found(evidenceId, malformed),
            manifest = EvidenceManifestRetrievalResult.Found(emlManifest(malformed)),
        )
        val mechanism = FakeMechanism(events) { error("must not run") }
        val outcome = emlCoordinator(FakePermission(PermissionDecisionOutcome.APPROVED, events), custodian, mechanism).invoke(owner, evidenceId)
        assertIs<ExternalTranscriptionOwnerInvocationOutcome.UnsupportedOrOutOfBounds>(outcome)
        assertEquals(0, mechanism.calls)
    }

    @Test fun `STEP 4G -- an attachments-only EML with no body content fails closed, no fallback`() = runTest {
        val attachmentsOnly = ("From: a@invalid\r\nTo: b@invalid\r\nMIME-Version: 1.0\r\n" +
            "Content-Type: multipart/mixed; boundary=only1\r\n\r\n" +
            "--only1\r\nContent-Type: application/octet-stream\r\n" +
            "Content-Disposition: attachment; filename=\"x.bin\"\r\nContent-Transfer-Encoding: base64\r\n\r\nYWJj\r\n" +
            "--only1--\r\n").toByteArray()
        val custodian = FakeCustodian(
            source = EvidenceRetrievalResult.Found(evidenceId, attachmentsOnly),
            manifest = EvidenceManifestRetrievalResult.Found(emlManifest(attachmentsOnly)),
        )
        val mechanism = FakeMechanism(events) { error("must not run") }
        val outcome = emlCoordinator(FakePermission(PermissionDecisionOutcome.APPROVED, events), custodian, mechanism).invoke(owner, evidenceId)
        assertIs<ExternalTranscriptionOwnerInvocationOutcome.UnsupportedOrOutOfBounds>(outcome)
        assertEquals(0, mechanism.calls)
    }

    @Test fun `STEP 4G -- a mechanism returning the wrong (OCR-shaped) candidate for an EML request is rejected, not miscast`() = runTest {
        val custodian = FakeCustodian(
            source = EvidenceRetrievalResult.Found(evidenceId, plainEmlBytes),
            manifest = EvidenceManifestRetrievalResult.Found(emlManifest(plainEmlBytes)),
        )
        val mechanism = FakeMechanism(events) { ExternalTranscriptionMechanismOutcome.Candidate(candidate()) }
        val outcome = emlCoordinator(FakePermission(PermissionDecisionOutcome.APPROVED, events), custodian, mechanism).invoke(owner, evidenceId)
        assertIs<ExternalTranscriptionOwnerInvocationOutcome.ValidationRejected>(outcome)
    }

    // EML TRANSPORT-BINDING CORRECTION: the removed "wrong response IDs, digest, or profile
    // identity are rejected" test exercised equality checks that lived in EmlStructuredResultValidator.
    // Those checks (and the candidate fields they compared) no longer exist -- Parker-known binding
    // identities are now verified as Responses API metadata inside the real adapter, before a
    // candidate is ever constructed, which this coordinator-level fake-mechanism test cannot
    // exercise (it bypasses the adapter entirely). The equivalent coverage is
    // EmlExternalTranscriptionAdapterTest's transport-binding mismatch tests, at the layer that
    // actually performs these checks now.

@Test fun `STEP 4G -- provider failure surfaces as MechanismFailure, no fallback to native or local`() = runTest {
        val custodian = FakeCustodian(
            source = EvidenceRetrievalResult.Found(evidenceId, plainEmlBytes),
            manifest = EvidenceManifestRetrievalResult.Found(emlManifest(plainEmlBytes)),
        )
        val mechanism = FakeMechanism(events) { ExternalTranscriptionMechanismOutcome.Failure("PROVIDER_UNAVAILABLE") }
        val outcome = emlCoordinator(FakePermission(PermissionDecisionOutcome.APPROVED, events), custodian, mechanism).invoke(owner, evidenceId)
        assertIs<ExternalTranscriptionOwnerInvocationOutcome.MechanismFailure>(outcome)
    }

    @Test fun `STEP 4G -- EML admission not configured fails closed rather than silently succeeding`() = runTest {
        val custodian = FakeCustodian(
            source = EvidenceRetrievalResult.Found(evidenceId, plainEmlBytes),
            manifest = EvidenceManifestRetrievalResult.Found(emlManifest(plainEmlBytes)),
        )
        val mechanism = object : ExternalTranscriptionMechanism {
            override suspend fun transcribe(request: ExternalTranscriptionRequest): ExternalTranscriptionMechanismOutcome {
                val eml = request.representation as EmlDerivedRepresentation
                return ExternalTranscriptionMechanismOutcome.Candidate(
                    emlCandidateFor(eml.provenance.bodyAlternativesIncluded),
                )
            }
        }
        // The default coordinator (no emlDurableAdmission override) uses the fail-closed no-op default.
        val defaultCoordinator = ExternalTranscriptionOwnerInvocationCoordinator(
            FakePermission(PermissionDecisionOutcome.APPROVED, events), custodian, mechanism,
            OcrStructuredResultValidator(), admission(), executionBinding = emlBinding,
        )
        val outcome = defaultCoordinator.invoke(owner, evidenceId)
        assertIs<ExternalTranscriptionOwnerInvocationOutcome.AdmissionFailed>(outcome)
    }

    @Test fun `STEP 4G CORRECTION -- EML persisted but audit write failed is reported as reconciliation required, never as an ordinary admission failure`() = runTest {
        val custodian = FakeCustodian(
            source = EvidenceRetrievalResult.Found(evidenceId, plainEmlBytes),
            manifest = EvidenceManifestRetrievalResult.Found(emlManifest(plainEmlBytes)),
        )
        val mechanism = object : ExternalTranscriptionMechanism {
            override suspend fun transcribe(request: ExternalTranscriptionRequest): ExternalTranscriptionMechanismOutcome {
                val eml = request.representation as EmlDerivedRepresentation
                return ExternalTranscriptionMechanismOutcome.Candidate(
                    emlCandidateFor(eml.provenance.bodyAlternativesIncluded),
                )
            }
        }
        // Simulates: durable persistence (storage.prepare/publishPrepared) already succeeded, but
        // the final ADMITTED audit write failed -- the record below is the genuinely-persisted one.
        val emlAdmission = EmlValidatedExternalVerificationAdmission { id, _, receipt, _, _ ->
            EmlExternalVerificationAdmissionOutcome.AdmittedAuditFailed(
                DerivativeGenerationRecord(
                    DerivativeGenerationId("eml-generation-persisted"), id, listOf(DerivativeParentReference.RootEvidenceArtifact(id)),
                    "EML external verification receipt", receipt.producerIdentity,
                    listOf(DerivativeTransformation.MODEL_INFERENCE, DerivativeTransformation.STRUCTURAL_PARSING), receipt.recognisedAt,
                    DerivativeContentIdentity.NoCanonicalSerialization, receipt.completenessState, DerivativeOperationalOutcome.USABLE, receipt.warnings,
                ),
                receipt, "AUDIT_WRITE_FAILED",
            )
        }
        val coordinator = emlCoordinator(FakePermission(PermissionDecisionOutcome.APPROVED, events), custodian, mechanism, emlAdmission)

        val outcome = coordinator.invoke(owner, evidenceId)

        val reconciliation = assertIs<ExternalTranscriptionOwnerInvocationOutcome.EmlReconciliationRequired>(outcome)
        assertEquals(DerivativeGenerationId("eml-generation-persisted"), reconciliation.record.derivativeGenerationId)
        assertEquals("AUDIT_WRITE_FAILED", reconciliation.reason)
    }

    private fun executor(coordinator: ExternalTranscriptionOwnerInvocationCoordinator) = ExternalTranscriptionAcquisitionExecutor(
        AcquisitionExecutorBinding(ProductionAcquisitionCapabilityCatalogue.FIDELITY_FIRST_EXTERNAL_CAPABILITY_ID, EvidenceAcquisitionMechanism.EXTERNAL_TRANSCRIPTION, null),
        { principal, evidenceId -> coordinator.invoke(principal, evidenceId) },
    )

    /**
     * Resolves the same exact-target [AuthoritativeAcquisitionInput] GovernedAcquisitionExecutionCoordinator
     * itself would resolve, via the real resolver -- never a fabricated stand-in. Uses its own
     * dedicated custodian instance, deliberately never the one passed to the coordinator under
     * test, so this resolution's own call counts never contaminate a test's assertions about the
     * coordinator's own custody calls.
     */
    private suspend fun executionRequest(): GovernedAcquisitionExecutionRequest {
        val silentCustodian = object : EvidenceCustodian {
            override suspend fun accept(requestingPrincipalId: PrincipalId, candidate: CandidateEvidenceArtifact): EvidenceAcceptanceResult = error("not used")
            override suspend fun retrieve(requestingPrincipalId: PrincipalId, evidenceArtifactId: EvidenceArtifactId): EvidenceRetrievalResult =
                EvidenceRetrievalResult.Found(evidenceId, bytes)
            override suspend fun retrieveManifest(requestingPrincipalId: PrincipalId, evidenceArtifactId: EvidenceArtifactId): EvidenceManifestRetrievalResult =
                EvidenceManifestRetrievalResult.Found(manifest())
            override suspend fun submitSource(requestingPrincipalId: PrincipalId, candidate: CandidateEvidenceArtifact, advisorySha256: String?): EvidenceSourceSubmissionResult =
                throw UnsupportedOperationException("submitSource not supported by this fake")
        }
        val resolved = assertIs<AuthoritativeAcquisitionResolution.Verified>(
            AuthoritativeAcquisitionSourceResolver(silentCustodian).resolve(owner, evidenceId),
        )
        val capability = ProductionAcquisitionCapabilityCatalogue.fidelityFirstExternalCapability()
        val source = AcquisitionSource(
            evidenceId, digest, bytes.size.toLong(), "application/pdf", AcquisitionPageCount.Known(1),
            AcquisitionSourceCharacteristics(
                AcquisitionCharacteristicState.ABSENT, AcquisitionCharacteristicState.PRESENT,
                AcquisitionCharacteristicState.ABSENT, AcquisitionCharacteristicState.ABSENT,
                AcquisitionCharacteristicState.ABSENT, AcquisitionCharacteristicState.ABSENT,
            ), HumanAuthorisedCustody.CONFIRMED,
        )
        val decision = EvidenceAcquisitionRoutingDecision(
            source, capability, AcquisitionRepresentationClass.AUTHORITATIVE_SOURCE_OR_BYTE_EXACT_COPY,
            setOf(AcquisitionSelectionReason.SOURCE_CHARACTERISTICS_SUPPORTED),
        )
        return GovernedAcquisitionExecutionRequest(decision, resolved.input, owner)
    }

    private fun coordinator(
        permission: PermissionEngine,
        custodian: EvidenceCustodian,
        mechanism: ExternalTranscriptionMechanism,
        onAdmissionPrincipal: (PrincipalId) -> Unit = {},
    ) = ExternalTranscriptionOwnerInvocationCoordinator(permission, custodian, mechanism, OcrStructuredResultValidator(), admission(onAdmissionPrincipal), correlationFactory = { "correlation-unit-j" })

    private fun admission(onPrincipal: (PrincipalId) -> Unit = {}) = ValidatedExternalTranscriptionAdmission { id, validation, principal, _ ->
        onPrincipal(principal)
        val triple = when (val outcome = validation.outcome) {
            is OcrRecognitionOutcome.Recognised -> Triple(outcome.result, OcrDerivativeOutcomeKind.RECOGNISED, null as String?)
            is OcrRecognitionOutcome.PartialOrDegradedOutput -> Triple(outcome.partialResult, OcrDerivativeOutcomeKind.PARTIAL_OR_DEGRADED, outcome.reason)
            else -> error("test validation was not admissible")
        }
        val (result, kind, reason) = triple
        val producer = DerivativeProducerIdentity("external", "1.0.0", "literal-v1", "adapter", "1.0.0", "model", "model")
        val extracted = OcrDerivativeExtractedResult(
            result.recognisedText, result.fidelity, kind, reason, result.warnings, result.segments, producer,
            listOf(DerivativeTransformation.OCR, DerivativeTransformation.MODEL_INFERENCE), validation.completenessState,
            validation.pageAccounting, result.processingProvenance, result.providerProvenance, result.recognisedAt,
        )
        val record = DerivativeGenerationRecord(
            DerivativeGenerationId("generation-unit-j"), id, listOf(DerivativeParentReference.RootEvidenceArtifact(id)),
            "External transcription recognised text", producer, extracted.transformationHistory, result.recognisedAt,
            DerivativeContentIdentity.NoCanonicalSerialization, validation.completenessState, DerivativeOperationalOutcome.USABLE,
        )
        OcrDerivativeGenerationCoordinationOutcome.Admitted(record, extracted)
    }

    private fun manifest(sha: String = digest, byteLength: Long = bytes.size.toLong(), media: String? = "application/pdf") =
        EvidenceSourceManifest(evidenceId, sha, byteLength, media)

    private fun candidate(
        sourceBytes: ByteArray = bytes,
        sourceDigest: String = digest,
        media: String = "application/pdf",
    ): OcrStructuredTranscriptionCandidate {
        val scope = OcrPageScope(listOf(1))
        return OcrStructuredTranscriptionCandidate(
            scope, scope, scope,
            listOf(OcrStructuredPageCandidate(1, "literal text", OcrPageOutcomeKind.TRANSCRIBED)),
            TranscriptionFidelity.UNVERIFIED_LITERAL_TRANSCRIPTION,
            OcrRecognitionIdentity("external", "literal-v1", "1.0.0"),
            OcrProviderProvenance("provider", "adapter", "1.0.0", "literal-v1", "model", OcrModelSnapshot.NotExposed, "provider-correlation"),
            OcrProcessingProvenance(evidenceId, OcrSha256Digest(sourceDigest), media, sourceBytes.size.toLong(), scope, scope, media, sourceBytes.size.toLong(), OcrSha256Digest(sourceDigest), true, "byte-exact-v1", Instant.EPOCH),
            Instant.EPOCH,
        )
    }

    private fun sha256(value: ByteArray) = MessageDigest.getInstance("SHA-256").digest(value).joinToString("") { "%02x".format(it) }
}
