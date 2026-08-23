package parker.core.runtime

import java.nio.file.Files
import java.nio.file.Path
import java.time.Instant
import kotlin.reflect.KParameter
import kotlin.reflect.full.primaryConstructor
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertIs
import kotlin.test.assertNotEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.test.fail
import kotlinx.coroutines.test.runTest
import parker.core.interfaces.Assertion
import parker.core.interfaces.CandidateAssertion
import parker.core.interfaces.CandidateDocument
import parker.core.interfaces.CandidateEntity
import parker.core.interfaces.CandidateEvidenceArtifact
import parker.core.interfaces.CandidateProvenance
import parker.core.interfaces.CandidateRelationship
import parker.core.interfaces.ContentNature
import parker.core.interfaces.DecisionId
import parker.core.interfaces.DerivativeMemoryRegistrationOutcome
import parker.core.interfaces.Document
import parker.core.interfaces.DocumentId
import parker.core.interfaces.DocumentProcessingStatus
import parker.core.interfaces.EvidenceAcceptanceResult
import parker.core.interfaces.EvidenceCustodian
import parker.core.interfaces.Entity
import parker.core.interfaces.ExecutionRequest
import parker.core.interfaces.MemoryCore
import parker.core.interfaces.MemoryCoreRecord
import parker.core.interfaces.MemoryCoreRecordReference
import parker.core.interfaces.MemoryCoreRecordStatus
import parker.core.interfaces.PermissionAction
import parker.core.interfaces.PermissionDecision
import parker.core.interfaces.PermissionDecisionOutcome
import parker.core.interfaces.PermissionEngine
import parker.core.interfaces.PermissionLevel
import parker.core.interfaces.PrincipalId
import parker.core.interfaces.Provenance
import parker.core.interfaces.ProvenanceId
import parker.core.interfaces.Relationship
import parker.core.interfaces.TierADerivativePayload
import parker.core.interfaces.TierADocumentRoutingResult
import parker.core.interfaces.TierAOwnerInvocationOutcome

/**
 * Document Ingestion, Derivative-to-Memory-Core Registration. Behavioural
 * tests for [DerivativeMemoryRegistrationCoordinator], governed by
 * `docs/architecture/DOCUMENT_INGESTION_DERIVATIVE_TO_MEMORY_CORE_REGISTRATION_SCOPE_LOCK.md`
 * ("the Scope Lock"). Mirrors [EvidenceRegistrationCoordinatorTest]'s own
 * established fake-double style ([FakeMemoryCore], the repository's own
 * existing [FakePermissionEngine]) exactly. The [TierADocumentRoutingResult.Admitted]
 * input used throughout is obtained from the real, unchanged Tier A
 * pipeline against the immutable bake-off fixture corpus -- never a
 * hand-constructed `DerivativeGenerationRecord` -- so every field this
 * coordinator reads is genuine, not merely plausible-looking test data.
 */
class DerivativeMemoryRegistrationCoordinatorTest {

    private val principalId = PrincipalId("owner-1")
    private val fixedInstant: Instant = Instant.parse("2026-01-01T00:00:00Z")

    private fun approvedDecision(request: ExecutionRequest) = PermissionDecision(
        decisionId = DecisionId("dec-${request.requestId.value}"),
        principalId = request.principalId,
        resourceId = request.targetResources.first(),
        action = PermissionAction.WRITE,
        decision = PermissionDecisionOutcome.APPROVED,
        level = PermissionLevel.AUTOMATIC,
        timestamp = fixedInstant,
    )

    private fun deniedDecision(request: ExecutionRequest) = PermissionDecision(
        decisionId = DecisionId("dec-${request.requestId.value}"),
        principalId = request.principalId,
        resourceId = request.targetResources.first(),
        action = PermissionAction.WRITE,
        decision = PermissionDecisionOutcome.DENIED,
        level = PermissionLevel.AUTOMATIC,
        timestamp = fixedInstant,
    )

    /** A real, genuine [TierADocumentRoutingResult.Admitted], obtained via the real, unchanged Tier A pipeline. */
    private suspend fun admittedCsvFixture(fileName: String = "06-structured.csv"): TierADocumentRoutingResult.Admitted {
        val storage = InMemoryEvidenceArtifactStorage()
        val manifestStorage = InMemoryEvidenceSourceManifestStorage()
        val custodian = DefaultEvidenceCustodian(storage, approvingPermissionEngine(), manifestStorage)
        val bytes = Files.readAllBytes(FIXTURE_ROOT.resolve(fileName))
        val accepted = assertIs<EvidenceAcceptanceResult.Accepted>(
            custodian.accept(principalId, CandidateEvidenceArtifact(bytes, "text/csv", fileName)),
        )
        val router = TierADocumentIngestionComposition.create(
            FileSystemDerivativeGenerationStorage(Files.createTempDirectory("derivative-memory-registration-test")),
            FileSystemDocumentIngestionAudit(Files.createTempDirectory("derivative-memory-registration-test-audit").resolve("audit.log")),
        )
        val coordinator = TierAOwnerInvocationCoordinator(custodian, router)
        val outcome = coordinator.invoke(principalId, accepted.acceptedEvidenceArtifact.evidenceArtifactId, "correlation-1")
        val routed = assertIs<TierAOwnerInvocationOutcome.Routed>(outcome)
        return assertIs<TierADocumentRoutingResult.Admitted>(routed.result)
    }

    private fun approvingPermissionEngine() = FakePermissionEngine { request -> approvedDecision(request) }

    // --- A. Authorized owner registration succeeds ---

    @Test
    fun `authorized registration of an eligible Admitted derivative succeeds`() = runTest {
        val admitted = admittedCsvFixture()
        val prov = provenance()
        val memoryCore = FakeMemoryCore(
            onCreateProvenance = { _, _ -> prov },
            onRegisterDocument = { _, candidate -> document(candidate) },
        )
        val permissionEngine = FakePermissionEngine { request -> approvedDecision(request) }
        val coordinator = DerivativeMemoryRegistrationCoordinator(memoryCore, permissionEngine)

        val outcome = coordinator.register(principalId, "corr-1", admitted)

        val registered = assertIs<DerivativeMemoryRegistrationOutcome.Registered>(outcome)
        assertEquals(prov, registered.provenance)
        assertEquals(1, memoryCore.createProvenanceCallCount)
        assertEquals(1, memoryCore.registerDocumentCallCount)
    }

    // --- B. Exact adopted field mapping ---

    @Test
    fun `exact adopted CandidateProvenance and CandidateDocument fields are written`() = runTest {
        val admitted = admittedCsvFixture()
        val prov = provenance()
        val memoryCore = FakeMemoryCore(
            onCreateProvenance = { _, _ -> prov },
            onRegisterDocument = { _, candidate -> document(candidate) },
        )
        val coordinator = DerivativeMemoryRegistrationCoordinator(memoryCore, approvingPermissionEngine())

        coordinator.register(principalId, "corr-1", admitted)

        val submittedProvenance = memoryCore.lastCandidateProvenance!!
        assertEquals(admitted.record.derivativeGenerationId.value, submittedProvenance.sourceIdentifier)
        assertEquals(DerivativeMemoryRegistrationCoordinator.DERIVATIVE_SOURCE_TYPE, submittedProvenance.sourceType)
        assertEquals(ContentNature.EXTRACTED, submittedProvenance.contentNature)
        assertNull(submittedProvenance.extractedFrom)
        assertTrue(submittedProvenance.processingHistory.any { it.contains(admitted.record.derivativeGenerationId.value) })

        val submittedDocument = memoryCore.lastCandidateDocument!!
        assertEquals(admitted.record.derivativeKind, submittedDocument.documentType)
        assertEquals(admitted.record.derivativeGenerationId.value, submittedDocument.locationReference)
        assertEquals(prov.provenanceId, submittedDocument.provenanceId)
        assertEquals(DocumentProcessingStatus.PROCESSED_EXTERNALLY, submittedDocument.processingStatus)
    }

    @Test
    fun `an optional caller-supplied extractedFromDocumentId is passed through, otherwise absent`() = runTest {
        val admitted = admittedCsvFixture()
        val memoryCore = FakeMemoryCore(
            onCreateProvenance = { _, _ -> provenance() },
            onRegisterDocument = { _, candidate -> document(candidate) },
        )
        val coordinator = DerivativeMemoryRegistrationCoordinator(memoryCore, approvingPermissionEngine())

        coordinator.register(principalId, "corr-1", admitted, extractedFromDocumentId = DocumentId("source-doc-1"))

        assertEquals(DocumentId("source-doc-1"), memoryCore.lastCandidateProvenance!!.extractedFrom)
    }

    // --- C. Parser payload never registered ---

    @Test
    fun `parser-extracted payload content never appears in the submitted candidate fields`() = runTest {
        val admitted = admittedCsvFixture()
        val csvPayload = assertIs<TierADerivativePayload.Csv>(admitted.payload).value
        // A distinctive value known to exist only in the fixture's own parsed row data.
        val distinctiveValue = "Alpha, Limited"
        assertTrue(csvPayload.rows.any { row -> row.any { it.contains(distinctiveValue) } }, "fixture sanity check")

        val memoryCore = FakeMemoryCore(
            onCreateProvenance = { _, _ -> provenance() },
            onRegisterDocument = { _, candidate -> document(candidate) },
        )
        val coordinator = DerivativeMemoryRegistrationCoordinator(memoryCore, approvingPermissionEngine())

        coordinator.register(principalId, "corr-1", admitted)

        val submittedProvenance = memoryCore.lastCandidateProvenance!!
        val submittedDocument = memoryCore.lastCandidateDocument!!
        val allSubmittedText = listOfNotNull(
            submittedProvenance.sourceIdentifier, submittedProvenance.sourceType, submittedProvenance.creator,
            submittedProvenance.integrityInformation,
        ) + submittedProvenance.processingHistory +
            listOfNotNull(submittedDocument.documentType, submittedDocument.locationReference, submittedDocument.integrityHash) +
            submittedDocument.metadata.values
        assertTrue(allSubmittedText.none { it.contains(distinctiveValue) }, "parser payload content must never reach Memory Core")
    }

    // --- D. Authorization rejection before mutation ---

    @Test
    fun `a denied provenance-creation permission decision returns ProvenanceNotAuthorised before any Memory Core mutation`() = runTest {
        val admitted = admittedCsvFixture()
        val memoryCore = FakeMemoryCore(
            onCreateProvenance = { _, _ -> fail("createProvenance must not be called") },
            onRegisterDocument = { _, _ -> fail("registerDocument must not be called") },
        )
        val permissionEngine = FakePermissionEngine { request -> deniedDecision(request) }
        val coordinator = DerivativeMemoryRegistrationCoordinator(memoryCore, permissionEngine)

        val outcome = coordinator.register(principalId, "corr-1", admitted)

        assertIs<DerivativeMemoryRegistrationOutcome.ProvenanceNotAuthorised>(outcome)
        assertEquals(0, memoryCore.createProvenanceCallCount)
        assertEquals(0, memoryCore.registerDocumentCallCount)
    }

    @Test
    fun `a denied document-registration permission decision returns DocumentRegistrationNotAuthorised after Provenance already exists`() = runTest {
        val admitted = admittedCsvFixture()
        val prov = provenance()
        val memoryCore = FakeMemoryCore(
            onCreateProvenance = { _, _ -> prov },
            onRegisterDocument = { _, _ -> fail("registerDocument must not be called") },
        )
        var callNumber = 0
        val permissionEngine = FakePermissionEngine { request ->
            callNumber++
            if (callNumber == 1) approvedDecision(request) else deniedDecision(request)
        }
        val coordinator = DerivativeMemoryRegistrationCoordinator(memoryCore, permissionEngine)

        val outcome = coordinator.register(principalId, "corr-1", admitted)

        val denied = assertIs<DerivativeMemoryRegistrationOutcome.DocumentRegistrationNotAuthorised>(outcome)
        assertEquals(prov, denied.provenance)
        assertEquals(1, memoryCore.createProvenanceCallCount)
        assertEquals(0, memoryCore.registerDocumentCallCount)
    }

    // --- E/F. Ineligible states cannot be registered, enforced by the type system ---

    @Test
    fun `register accepts only TierADocumentRoutingResult Admitted, not the general sealed class`() {
        val function = DerivativeMemoryRegistrationCoordinator::class.members.single { it.name == "register" }
        val admittedParameter = function.parameters.single { it.name == "admitted" }

        assertEquals(
            TierADocumentRoutingResult.Admitted::class,
            admittedParameter.type.classifier,
            "register's own parameter type must be exactly TierADocumentRoutingResult.Admitted -- not the " +
                "general TierADocumentRoutingResult sealed class -- so a ReconciliationRequired, RequiresTierB, " +
                "or any other ineligible variant cannot even be passed to this method; eligibility is enforced " +
                "by the type system, not a runtime check",
        )
    }

    // --- G/H/I/J/K. No coupling to storage, Knowledge, Evidence Intelligence, OCR/Tier B, DerivativeReview ---

    @Test
    fun `coordinator declares exactly two constructor dependencies -- MemoryCore and PermissionEngine only`() {
        val constructor = DerivativeMemoryRegistrationCoordinator::class.primaryConstructor!!
        val parameterTypes = constructor.parameters
            .filter { it.kind == KParameter.Kind.VALUE }
            .map { it.type.classifier }

        assertEquals(
            setOf(MemoryCore::class, PermissionEngine::class),
            parameterTypes.toSet(),
            "DerivativeMemoryRegistrationCoordinator must depend on exactly MemoryCore and PermissionEngine -- " +
                "no EvidenceCustodian, DerivativeGenerationStorage, DerivativeReviewRegistry, KnowledgeStore, " +
                "EvidenceIntelligence, OcrMechanism, TierADocumentIngestionRouter, or either existing Document " +
                "Ingestion owner-invocation coordinator reference of its own",
        )
        assertEquals(2, parameterTypes.size)
    }

    // --- L. No automatic invocation merely because Tier A admission succeeded ---

    @Test
    fun `TierAOwnerInvocationCoordinator itself cannot reach Memory Core -- no automatic registration path exists`() {
        val constructor = TierAOwnerInvocationCoordinator::class.primaryConstructor!!
        val parameterTypes = constructor.parameters
            .filter { it.kind == KParameter.Kind.VALUE }
            .map { it.type.classifier }

        assertTrue(
            MemoryCore::class !in parameterTypes && DerivativeMemoryRegistrationCoordinator::class !in parameterTypes,
            "TierAOwnerInvocationCoordinator must hold no reference to MemoryCore or " +
                "DerivativeMemoryRegistrationCoordinator -- successful Tier A admission alone cannot possibly " +
                "reach Memory Core, since no such reference exists to call through",
        )
    }

    // --- M. Independent registration for separately generated derivatives ---

    @Test
    fun `two separately eligible generations each receive their own independent Provenance and Document`() = runTest {
        val firstAdmitted = admittedCsvFixture()
        val secondAdmitted = admittedCsvFixture()
        assertNotEquals(
            firstAdmitted.record.derivativeGenerationId,
            secondAdmitted.record.derivativeGenerationId,
            "two separate Tier A invocations of the same fixture must mint two distinct generations",
        )
        var provenanceCounter = 0
        val memoryCore = FakeMemoryCore(
            onCreateProvenance = { _, _ -> provenanceCounter++; provenance(id = "provenance-$provenanceCounter") },
            onRegisterDocument = { _, candidate -> document(candidate, id = "document-$provenanceCounter") },
        )
        val coordinator = DerivativeMemoryRegistrationCoordinator(memoryCore, approvingPermissionEngine())

        val firstOutcome = assertIs<DerivativeMemoryRegistrationOutcome.Registered>(
            coordinator.register(principalId, "corr-1", firstAdmitted),
        )
        val secondOutcome = assertIs<DerivativeMemoryRegistrationOutcome.Registered>(
            coordinator.register(principalId, "corr-2", secondAdmitted),
        )

        assertNotEquals(firstOutcome.provenance.provenanceId, secondOutcome.provenance.provenanceId)
        assertNotEquals(firstOutcome.document.documentId, secondOutcome.document.documentId)
        assertEquals(2, memoryCore.createProvenanceCallCount)
        assertEquals(2, memoryCore.registerDocumentCallCount)
    }

    // --- Failure propagation, mirroring EvidenceRegistrationCoordinator's own precedent ---

    @Test
    fun `an exception thrown by MemoryCore createProvenance propagates unchanged and registerDocument is never called`() = runTest {
        val admitted = admittedCsvFixture()
        val memoryCore = FakeMemoryCore(
            onCreateProvenance = { _, _ -> throw IllegalStateException("createProvenance boom") },
            onRegisterDocument = { _, _ -> fail("registerDocument must not be called") },
        )
        val coordinator = DerivativeMemoryRegistrationCoordinator(memoryCore, approvingPermissionEngine())

        val thrown = assertFailsWith<IllegalStateException> {
            coordinator.register(principalId, "corr-1", admitted)
        }

        assertEquals("createProvenance boom", thrown.message)
        assertEquals(0, memoryCore.registerDocumentCallCount)
    }

    // --- Test helpers ---

    private fun provenance(id: String = "provenance-1") = Provenance(
        provenanceId = ProvenanceId(id),
        sourceIdentifier = "derivative-1",
        sourceType = DerivativeMemoryRegistrationCoordinator.DERIVATIVE_SOURCE_TYPE,
        acquisitionTime = fixedInstant,
        ingestionTime = fixedInstant,
        contentNature = ContentNature.EXTRACTED,
    )

    private fun document(candidate: CandidateDocument, id: String = "document-1") = Document(
        documentId = DocumentId(id),
        documentType = candidate.documentType,
        locationReference = candidate.locationReference,
        provenanceId = candidate.provenanceId,
        registeredAt = fixedInstant,
    )

    private companion object {
        val FIXTURE_ROOT: Path = Path.of("tests/fixtures/document-ingestion-bakeoff/fixtures")
    }

    /**
     * Captures its [createProvenance]/[registerDocument] call counts and last-submitted
     * candidates; returns caller-configured results. The other four [MemoryCore] operations are
     * never called by [DerivativeMemoryRegistrationCoordinator] and throw if reached.
     */
    private class FakeMemoryCore(
        private val onCreateProvenance: (PrincipalId, CandidateProvenance) -> Provenance,
        private val onRegisterDocument: (PrincipalId, CandidateDocument) -> Document,
    ) : MemoryCore {
        var createProvenanceCallCount = 0
            private set
        var registerDocumentCallCount = 0
            private set
        var lastCandidateProvenance: CandidateProvenance? = null
            private set
        var lastCandidateDocument: CandidateDocument? = null
            private set

        override suspend fun createProvenance(requestingPrincipalId: PrincipalId, candidate: CandidateProvenance): Provenance {
            createProvenanceCallCount++
            lastCandidateProvenance = candidate
            return onCreateProvenance(requestingPrincipalId, candidate)
        }

        override suspend fun registerDocument(requestingPrincipalId: PrincipalId, candidate: CandidateDocument): Document {
            registerDocumentCallCount++
            lastCandidateDocument = candidate
            return onRegisterDocument(requestingPrincipalId, candidate)
        }

        override suspend fun createEntity(requestingPrincipalId: PrincipalId, candidate: CandidateEntity): Entity =
            throw UnsupportedOperationException("not used by DerivativeMemoryRegistrationCoordinator")

        override suspend fun createAssertion(requestingPrincipalId: PrincipalId, candidate: CandidateAssertion): Assertion =
            throw UnsupportedOperationException("not used by DerivativeMemoryRegistrationCoordinator")

        override suspend fun createRelationship(requestingPrincipalId: PrincipalId, candidate: CandidateRelationship): Relationship =
            throw UnsupportedOperationException("not used by DerivativeMemoryRegistrationCoordinator")

        override suspend fun transitionStatus(
            requestingPrincipalId: PrincipalId,
            reference: MemoryCoreRecordReference,
            targetStatus: MemoryCoreRecordStatus,
        ): MemoryCoreRecord = throw UnsupportedOperationException("not used by DerivativeMemoryRegistrationCoordinator")
    }
}
