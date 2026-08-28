package parker.core.runtime

import java.nio.file.Files
import java.nio.file.Path
import java.security.MessageDigest
import java.time.Instant
import java.util.concurrent.atomic.AtomicInteger
import kotlin.test.*
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.io.TempDir
import parker.core.interfaces.*

class FidelityFirstAcceptanceCoordinatorTest {
    @TempDir lateinit var root: Path
    private val owner = PrincipalId("owner.synthetic-acceptance")
    private val evidenceId = EvidenceArtifactId("evidence-synthetic-acceptance")
    private val bytes = "%PDF synthetic pending acceptance".toByteArray()
    private val digest = sha256(bytes)
    private val commit = "b".repeat(40)
    private val configuration = FidelityFirstEffectiveConfiguration(
        ProductionAcquisitionCapabilityCatalogue.FIDELITY_FIRST_EXTERNAL_CAPABILITY_ID,
        "openai-responses-adapter", "2.0.0", "gpt-5.6-sol", "openai-fidelity-first-transcription-v1",
        "external-transcription.direct-authoritative-byte-v1", "none", false, "high", "c".repeat(64), "d".repeat(64),
    )

    @Test fun `authority is create once persistent and conflicting rewrite fails closed`() {
        val authorityRoot = Files.createDirectory(root.resolve("authority-immutability"))
        val storage = FileSystemFidelityFirstAcceptanceAuthorityStorage(authorityRoot)
        storage.admit(authority())
        assertEquals(authority(), FileSystemFidelityFirstAcceptanceAuthorityStorage(authorityRoot).load(authority().authorityId))
        storage.admit(authority())
        assertFailsWith<IllegalArgumentException> { storage.admit(authority().copy(modelAlias = "conflicting-model")) }
    }

    @Test fun `pending authority executes once admits unreviewed generation and survives restart`() = runTest {
        val fixture = fixture()
        fixture.authorities.admit(authority())
        val first = assertIs<FidelityFirstAcceptanceOutcome.Admitted>(fixture.coordinator().invoke("authority-synthetic-a1"))
        assertEquals("generation-synthetic-acceptance", first.generationId.value)
        assertEquals(1, fixture.transmissions.get())
        assertEquals(
            listOf(FidelityFirstAttemptStage.AUTHORISED, FidelityFirstAttemptStage.PREFLIGHT_PASSED,
                FidelityFirstAttemptStage.SOURCE_RETRIEVED, FidelityFirstAttemptStage.REQUEST_PREPARED,
                FidelityFirstAttemptStage.PROVIDER_ATTEMPT_STARTED, FidelityFirstAttemptStage.PROVIDER_RESPONSE_RECEIVED,
                FidelityFirstAttemptStage.GENERATION_ADMITTED, FidelityFirstAttemptStage.TERMINAL_SUCCESS),
            fixture.ledger.open(authority().executionIdentity()).stages,
        )
        assertEquals("generation-synthetic-acceptance", fixture.ledger.open(authority().executionIdentity()).admittedGenerationId)
        val restarted = fixture.coordinator()
        assertEquals("ATTEMPT_UNAVAILABLE", assertIs<FidelityFirstAcceptanceOutcome.Blocked>(restarted.invoke("authority-synthetic-a1")).reason)
        assertEquals(1, fixture.transmissions.get())
        // Admission does not create a review decision; the generation record has no implicit approval field.
        assertFalse(FidelityFirstAcceptanceOutcome.Admitted::class.java.declaredFields.any { it.name.contains("review", true) })
    }

    @Test fun `A1 shaped governed identifiers derive safe ledger identity and execute exactly once`() = runTest {
        val fixture = fixture()
        val dotted = authority().copy(
            authorityId = "authority-fa-9.4p-a1-synthetic",
            programmeUnit = "FA.9.4P-A1",
            executionId = "execution-fa-9.4p-a1-synthetic",
            requestId = "request-fa-9.4p-a1-synthetic",
            attemptId = "attempt-fa-9.4p-a1-synthetic",
        )
        fixture.authorities.admit(dotted)
        assertIs<FidelityFirstAcceptanceOutcome.Admitted>(fixture.coordinator().invoke(dotted.authorityId))
        val snapshot = fixture.ledger.open(dotted.executionIdentity())
        assertEquals(dotted.attemptId, snapshot.identity.attemptId)
        assertTrue(snapshot.identity.safeAttemptId.matches(Regex("^[A-Za-z0-9_-]{1,120}$")))
        assertEquals(FidelityFirstAttemptStage.TERMINAL_SUCCESS, snapshot.stages.last())
        assertEquals(1, fixture.transmissions.get())
        assertEquals("ATTEMPT_UNAVAILABLE", assertIs<FidelityFirstAcceptanceOutcome.Blocked>(fixture.coordinator().invoke(dotted.authorityId)).reason)
        assertEquals(1, fixture.transmissions.get())
    }

    @Test fun `missing corrupt and exact binding mismatches block before transmission`() = runTest {
        val fixture = fixture()
        assertEquals("AUTHORITY_MISSING", assertIs<FidelityFirstAcceptanceOutcome.Blocked>(fixture.coordinator().invoke("missing")).reason)
        fixture.authorities.admit(authority())
        Files.writeString(fixture.authorityRoot.resolve("authority-synthetic-a1.acceptance-authority"), "corrupt\n")
        assertEquals("AUTHORITY_CORRUPT", assertIs<FidelityFirstAcceptanceOutcome.Blocked>(fixture.coordinator().invoke("authority-synthetic-a1")).reason)
        assertEquals(0, fixture.transmissions.get())

        listOf(
            authority().copy(authorityId = "authority-source", sourceSha256 = "0".repeat(64)),
            authority().copy(authorityId = "authority-config", modelAlias = "different-model"),
            authority().copy(authorityId = "authority-commit", productionCommit = "a".repeat(40)),
        ).forEach { a -> fixture.authorities.admit(a) }
        assertEquals("AUTHORITY_SOURCE_MISMATCH", assertIs<FidelityFirstAcceptanceOutcome.Blocked>(fixture.coordinator().invoke("authority-source")).reason)
        assertEquals("AUTHORITY_CONFIGURATION_MISMATCH", assertIs<FidelityFirstAcceptanceOutcome.Blocked>(fixture.coordinator().invoke("authority-config")).reason)
        assertEquals("AUTHORITY_COMMIT_MISMATCH", assertIs<FidelityFirstAcceptanceOutcome.Blocked>(fixture.coordinator().invoke("authority-commit")).reason)
        assertEquals(0, fixture.transmissions.get())
    }

    @Test fun `disabled accepted and suspended never use pending acceptance lane`() = runTest {
        for (state in listOf(FidelityFirstAcceptanceLifecycle.DISABLED, FidelityFirstAcceptanceLifecycle.ACCEPTED, FidelityFirstAcceptanceLifecycle.SUSPENDED)) {
            val fixture = fixture(state)
            fixture.authorities.admit(authority())
            assertEquals("LIFECYCLE_NOT_ACCEPTANCE_PENDING", assertIs<FidelityFirstAcceptanceOutcome.Blocked>(fixture.coordinator().invoke("authority-synthetic-a1")).reason)
            assertEquals(0, fixture.transmissions.get())
        }
    }

    @Test fun `concurrent duplicate invocation can transmit at most once`() = runTest {
        val fixture = fixture()
        fixture.authorities.admit(authority())
        val outcomes = listOf(async { fixture.coordinator().invoke("authority-synthetic-a1") }, async { fixture.coordinator().invoke("authority-synthetic-a1") }).awaitAll()
        assertEquals(1, outcomes.count { it is FidelityFirstAcceptanceOutcome.Admitted })
        assertEquals(1, fixture.transmissions.get())
    }

    @Test fun `crash before attempt marker can resume but crash after marker cannot retry`() = runTest {
        val before = fixture(crash = Crash.BEFORE_ATTEMPT)
        before.authorities.admit(authority())
        assertFailsWith<SimulatedCrash> { before.coordinator().invoke("authority-synthetic-a1") }
        assertFalse(before.ledger.open(authority().executionIdentity()).providerAttemptStarted)
        before.crash = Crash.NONE
        assertIs<FidelityFirstAcceptanceOutcome.Admitted>(before.coordinator().invoke("authority-synthetic-a1"))
        assertEquals(1, before.transmissions.get())

        val after = fixture(crash = Crash.AFTER_ATTEMPT)
        after.authorities.admit(authority())
        assertFailsWith<SimulatedCrash> { after.coordinator().invoke("authority-synthetic-a1") }
        assertTrue(after.ledger.open(authority().executionIdentity()).providerAttemptStarted)
        after.crash = Crash.NONE
        assertEquals("ATTEMPT_UNAVAILABLE", assertIs<FidelityFirstAcceptanceOutcome.Blocked>(after.coordinator().invoke("authority-synthetic-a1")).reason)
        assertEquals(1, after.transmissions.get())
    }

    @Test fun `crash after response before admission cannot cause second request`() = runTest {
        val fixture = fixture(crash = Crash.AFTER_RESPONSE)
        fixture.authorities.admit(authority())
        assertFailsWith<SimulatedCrash> { fixture.coordinator().invoke("authority-synthetic-a1") }
        assertEquals(FidelityFirstAttemptStage.PROVIDER_RESPONSE_RECEIVED, fixture.ledger.open(authority().executionIdentity()).stages.last())
        fixture.crash = Crash.NONE
        assertIs<FidelityFirstAcceptanceOutcome.Blocked>(fixture.coordinator().invoke("authority-synthetic-a1"))
        assertEquals(1, fixture.transmissions.get())
    }

    @Test fun `provider and admission failures are terminal and never retry`() = runTest {
        val provider = fixture(providerFailure = true)
        provider.authorities.admit(authority())
        assertIs<FidelityFirstAcceptanceOutcome.Failed>(provider.coordinator().invoke("authority-synthetic-a1"))
        assertEquals(FidelityFirstAttemptStage.TERMINAL_FAILURE, provider.ledger.open(authority().executionIdentity()).stages.last())
        assertIs<FidelityFirstAcceptanceOutcome.Blocked>(provider.coordinator().invoke("authority-synthetic-a1"))
        assertEquals(1, provider.transmissions.get())
    }

    private enum class Crash { NONE, BEFORE_ATTEMPT, AFTER_ATTEMPT, AFTER_RESPONSE }
    private class SimulatedCrash : Error()

    private inner class Fixture(
        private val state: FidelityFirstAcceptanceLifecycle,
        var crash: Crash,
        private val providerFailure: Boolean,
    ) {
        val authorityRoot = Files.createDirectory(root.resolve("authorities-${System.nanoTime()}"))
        private val attemptRoot = Files.createDirectory(root.resolve("attempts-${System.nanoTime()}"))
        val authorities = FileSystemFidelityFirstAcceptanceAuthorityStorage(authorityRoot)
        val ledger = FileSystemFidelityFirstAttemptLedger(attemptRoot) { Instant.EPOCH }
        val transmissions = AtomicInteger()
        private val custodian = object : EvidenceCustodian {
            override suspend fun accept(requestingPrincipalId: PrincipalId, candidate: CandidateEvidenceArtifact) = error("unused")
            override suspend fun retrieve(requestingPrincipalId: PrincipalId, evidenceArtifactId: EvidenceArtifactId) = EvidenceRetrievalResult.Found(evidenceId, bytes.copyOf())
            override suspend fun retrieveManifest(requestingPrincipalId: PrincipalId, evidenceArtifactId: EvidenceArtifactId) =
                EvidenceManifestRetrievalResult.Found(EvidenceSourceManifest(evidenceId, digest, bytes.size.toLong(), "application/pdf"))
        }
        private val permission = object : PermissionEngine {
            override suspend fun evaluate(request: ExecutionRequest) = PermissionDecision(
                DecisionId("decision-synthetic"), request.principalId, request.targetResources.single(), PermissionAction.EXECUTE,
                PermissionDecisionOutcome.APPROVED, PermissionLevel.AUTOMATIC, Instant.EPOCH,
            )
            override suspend fun explain(decisionId: DecisionId): PermissionExplanation = error("unused")
        }
        fun coordinator() = FidelityFirstAcceptanceCoordinator(
            authorities, ledger, { state }, { configuration }, { commit }, owner, custodian, permission,
            mechanismFactory = { observer -> object : ExternalTranscriptionMechanism {
                override suspend fun transcribe(request: ExternalTranscriptionRequest): ExternalTranscriptionMechanismOutcome {
                    if (crash == Crash.BEFORE_ATTEMPT) throw SimulatedCrash()
                    observer.providerAttemptStarting(); transmissions.incrementAndGet()
                    if (crash == Crash.AFTER_ATTEMPT) throw SimulatedCrash()
                    if (providerFailure) return ExternalTranscriptionMechanismOutcome.Failure("synthetic failure")
                    observer.providerResponseReceived()
                    if (crash == Crash.AFTER_RESPONSE) throw SimulatedCrash()
                    return ExternalTranscriptionMechanismOutcome.Candidate(candidate(request))
                }
            } },
            validator = OcrStructuredResultValidator(),
            durableAdmission = ValidatedExternalTranscriptionAdmission { id, validation, _, _ -> admission(id, validation) },
        )
    }

    private fun fixture(
        state: FidelityFirstAcceptanceLifecycle = FidelityFirstAcceptanceLifecycle.ACCEPTANCE_PENDING,
        crash: Crash = Crash.NONE,
        providerFailure: Boolean = false,
    ) = Fixture(state, crash, providerFailure)

    private fun authority() = FidelityFirstAcceptanceAuthority(
        "authority-synthetic-a1", "FA.synthetic", "execution-synthetic-a1", "request-synthetic-a1", "attempt-synthetic-a1",
        evidenceId.value, digest, bytes.size.toLong(), "application/pdf", commit, configuration.capabilityId,
        configuration.adapterId, configuration.adapterVersion, configuration.modelAlias, configuration.profileId,
        configuration.processingProfile, configuration.reasoning, configuration.store, configuration.sourceDetail,
        configuration.instructionSha256, configuration.schemaSha256, 1, "owner.synthetic", Instant.EPOCH,
    )

    private fun candidate(request: ExternalTranscriptionRequest): OcrStructuredTranscriptionCandidate {
        val scope = OcrPageScope(listOf(1))
        return OcrStructuredTranscriptionCandidate(
            scope, scope, scope, listOf(OcrStructuredPageCandidate(1, "synthetic literal text", OcrPageOutcomeKind.TRANSCRIBED)),
            TranscriptionFidelity.UNVERIFIED_LITERAL_TRANSCRIPTION,
            OcrRecognitionIdentity("OpenAI", configuration.profileId, configuration.adapterVersion),
            OcrProviderProvenance("OpenAI", configuration.adapterId, configuration.adapterVersion, configuration.profileId,
                configuration.modelAlias, OcrModelSnapshot.NotExposed, "response-synthetic"),
            request.processingProvenance, Instant.EPOCH,
        )
    }

    private fun admission(id: EvidenceArtifactId, validation: OcrStructuredValidationOutcome.Validated): OcrDerivativeGenerationCoordinationOutcome {
        val recognised = (validation.outcome as OcrRecognitionOutcome.Recognised).result
        val producer = DerivativeProducerIdentity("OpenAI", "2.0.0", configuration.profileId, configuration.adapterId, "2.0.0", configuration.modelAlias, configuration.modelAlias)
        val extracted = OcrDerivativeExtractedResult(
            recognised.recognisedText, recognised.fidelity, OcrDerivativeOutcomeKind.RECOGNISED, null, recognised.warnings,
            recognised.segments, producer, listOf(DerivativeTransformation.OCR, DerivativeTransformation.MODEL_INFERENCE),
            validation.completenessState, validation.pageAccounting, recognised.processingProvenance, recognised.providerProvenance, recognised.recognisedAt,
        )
        val record = DerivativeGenerationRecord(
            DerivativeGenerationId("generation-synthetic-acceptance"), id, listOf(DerivativeParentReference.RootEvidenceArtifact(id)),
            "External transcription recognised text", producer, extracted.transformationHistory, Instant.EPOCH,
            DerivativeContentIdentity.NoCanonicalSerialization, validation.completenessState, DerivativeOperationalOutcome.USABLE,
        )
        return OcrDerivativeGenerationCoordinationOutcome.Admitted(record, extracted)
    }
    private fun sha256(value: ByteArray) = MessageDigest.getInstance("SHA-256").digest(value).joinToString("") { "%02x".format(it) }
}
