package parker.core.runtime

import java.lang.reflect.Modifier
import java.time.Instant
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertIs
import kotlin.test.assertSame
import kotlin.test.fail
import kotlinx.coroutines.test.runTest
import parker.core.interfaces.AcceptedEvidenceArtifact
import parker.core.interfaces.Assertion
import parker.core.interfaces.CandidateAssertion
import parker.core.interfaces.CandidateDocument
import parker.core.interfaces.CandidateEntity
import parker.core.interfaces.CandidateEvidenceArtifact
import parker.core.interfaces.CandidateProvenance
import parker.core.interfaces.CandidateRelationship
import parker.core.interfaces.ContentNature
import parker.core.interfaces.DecisionId
import parker.core.interfaces.Document
import parker.core.interfaces.DocumentId
import parker.core.interfaces.Entity
import parker.core.interfaces.EvidenceAcceptanceResult
import parker.core.interfaces.EvidenceArtifactId
import parker.core.interfaces.EvidenceCustodian
import parker.core.interfaces.EvidenceManifestRetrievalResult
import parker.core.interfaces.EvidenceRetrievalResult
import parker.core.interfaces.ExecutionRequest
import parker.core.interfaces.MemoryCore
import parker.core.interfaces.MemoryCoreRecord
import parker.core.interfaces.MemoryCoreRecordReference
import parker.core.interfaces.MemoryCoreRecordStatus
import parker.core.interfaces.PermissionAction
import parker.core.interfaces.PermissionDecision
import parker.core.interfaces.PermissionDecisionOutcome
import parker.core.interfaces.PermissionLevel
import parker.core.interfaces.PrincipalId
import parker.core.interfaces.Provenance
import parker.core.interfaces.ProvenanceId
import parker.core.interfaces.Relationship

/**
 * `EvidenceRegistrationCoordinator` behavioural test, per the approved
 * "Runtime Evidence Registration Coordinator" final implementation
 * planning review's own Section 7 test list. Exercises the coordinator
 * against hand-written fakes ([FakeEvidenceCustodian], [FakeMemoryCore])
 * and this repository's own existing [FakePermissionEngine]
 * (`tests/runtime/FakePermissionEngine.kt`) -- no mocking framework is
 * introduced, and no test beyond the plan's own named list is added.
 */
class EvidenceRegistrationCoordinatorTest {

    private val fixedInstant: Instant = Instant.parse("2026-01-01T00:00:00Z")
    private val principalId = PrincipalId("owner-1")

    private fun candidateEvidenceArtifact(bytes: ByteArray = byteArrayOf(1, 2, 3)) = CandidateEvidenceArtifact(bytes)

    private fun candidateProvenance() = CandidateProvenance(
        sourceIdentifier = "source-1",
        sourceType = "upload",
        acquisitionTime = fixedInstant,
        contentNature = ContentNature.ORIGINAL,
    )

    private fun acceptedArtifact(id: String = "evidence-1") = AcceptedEvidenceArtifact(
        evidenceArtifactId = EvidenceArtifactId(id),
        acceptedAt = fixedInstant,
    )

    private fun provenance(id: String = "provenance-1") = Provenance(
        provenanceId = ProvenanceId(id),
        sourceIdentifier = "source-1",
        sourceType = "upload",
        acquisitionTime = fixedInstant,
        ingestionTime = fixedInstant,
        contentNature = ContentNature.ORIGINAL,
    )

    private fun document(candidate: CandidateDocument, id: String = "document-1") = Document(
        documentId = DocumentId(id),
        documentType = candidate.documentType,
        locationReference = candidate.locationReference,
        provenanceId = candidate.provenanceId,
        registeredAt = fixedInstant,
    )

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

    /** Captures its single [accept] call's arguments and count; returns a caller-configured result. */
    private class FakeEvidenceCustodian(
        private val onAccept: (PrincipalId, CandidateEvidenceArtifact) -> EvidenceAcceptanceResult,
    ) : EvidenceCustodian {
        var acceptCallCount = 0
            private set

        override suspend fun accept(
            requestingPrincipalId: PrincipalId,
            candidate: CandidateEvidenceArtifact,
        ): EvidenceAcceptanceResult {
            acceptCallCount++
            return onAccept(requestingPrincipalId, candidate)
        }

        override suspend fun retrieve(
            requestingPrincipalId: PrincipalId,
            evidenceArtifactId: EvidenceArtifactId,
        ): EvidenceRetrievalResult = throw UnsupportedOperationException("not used by EvidenceRegistrationCoordinator")

        override suspend fun retrieveManifest(
            requestingPrincipalId: PrincipalId,
            evidenceArtifactId: EvidenceArtifactId,
        ): EvidenceManifestRetrievalResult = throw UnsupportedOperationException("not used by EvidenceRegistrationCoordinator")
    }

    /**
     * Captures its [createProvenance]/[registerDocument] call counts and
     * last-submitted candidate; returns caller-configured results. The
     * four other [MemoryCore] operations are never called by
     * [EvidenceRegistrationCoordinator] and throw if reached.
     */
    private class FakeMemoryCore(
        private val onCreateProvenance: (PrincipalId, CandidateProvenance) -> Provenance,
        private val onRegisterDocument: (PrincipalId, CandidateDocument) -> Document,
    ) : MemoryCore {
        var createProvenanceCallCount = 0
            private set
        var registerDocumentCallCount = 0
            private set
        var lastCandidateDocument: CandidateDocument? = null
            private set

        override suspend fun createProvenance(requestingPrincipalId: PrincipalId, candidate: CandidateProvenance): Provenance {
            createProvenanceCallCount++
            return onCreateProvenance(requestingPrincipalId, candidate)
        }

        override suspend fun registerDocument(requestingPrincipalId: PrincipalId, candidate: CandidateDocument): Document {
            registerDocumentCallCount++
            lastCandidateDocument = candidate
            return onRegisterDocument(requestingPrincipalId, candidate)
        }

        override suspend fun createEntity(requestingPrincipalId: PrincipalId, candidate: CandidateEntity): Entity =
            throw UnsupportedOperationException("not used by EvidenceRegistrationCoordinator")

        override suspend fun createAssertion(requestingPrincipalId: PrincipalId, candidate: CandidateAssertion): Assertion =
            throw UnsupportedOperationException("not used by EvidenceRegistrationCoordinator")

        override suspend fun createRelationship(requestingPrincipalId: PrincipalId, candidate: CandidateRelationship): Relationship =
            throw UnsupportedOperationException("not used by EvidenceRegistrationCoordinator")

        override suspend fun transitionStatus(
            requestingPrincipalId: PrincipalId,
            reference: MemoryCoreRecordReference,
            targetStatus: MemoryCoreRecordStatus,
        ): MemoryCoreRecord = throw UnsupportedOperationException("not used by EvidenceRegistrationCoordinator")
    }

    // ================= 1. Successful orchestration =================

    @Test
    fun `successful orchestration calls accept, createProvenance, and registerDocument, and bundles all three results`() = runTest {
        val accepted = acceptedArtifact()
        val prov = provenance()
        val evidenceCustodian = FakeEvidenceCustodian { _, _ -> EvidenceAcceptanceResult.Accepted(accepted) }
        val memoryCore = FakeMemoryCore(
            onCreateProvenance = { _, _ -> prov },
            onRegisterDocument = { _, candidate -> document(candidate) },
        )
        val permissionEngine = FakePermissionEngine { request -> approvedDecision(request) }
        val coordinator = EvidenceRegistrationCoordinator(evidenceCustodian, memoryCore, permissionEngine)

        val outcome = coordinator.register(
            requestingPrincipalId = principalId,
            correlationId = "corr-1",
            candidateEvidenceArtifact = candidateEvidenceArtifact(),
            candidateProvenance = candidateProvenance(),
            documentType = "pdf",
        )

        val registered = assertIs<EvidenceRegistrationOutcome.Registered>(outcome)
        assertSame(accepted, registered.acceptedEvidenceArtifact)
        assertSame(prov, registered.provenance)
        assertEquals(1, evidenceCustodian.acceptCallCount)
        assertEquals(1, memoryCore.createProvenanceCallCount)
        assertEquals(1, memoryCore.registerDocumentCallCount)
        assertEquals(2, permissionEngine.evaluateCallCount)
    }

    // ================= 2. Acceptance rejection =================

    @Test
    fun `acceptance rejection returns NotAccepted and never touches Memory Core or the Permission Engine`() = runTest {
        val rejection = EvidenceAcceptanceResult.Rejected("denied by policy")
        val evidenceCustodian = FakeEvidenceCustodian { _, _ -> rejection }
        val memoryCore = FakeMemoryCore(
            onCreateProvenance = { _, _ -> fail("createProvenance must not be called") },
            onRegisterDocument = { _, _ -> fail("registerDocument must not be called") },
        )
        val permissionEngine = FakePermissionEngine { request -> approvedDecision(request) }
        val coordinator = EvidenceRegistrationCoordinator(evidenceCustodian, memoryCore, permissionEngine)

        val outcome = coordinator.register(principalId, "corr-1", candidateEvidenceArtifact(), candidateProvenance(), "pdf")

        val notAccepted = assertIs<EvidenceRegistrationOutcome.NotAccepted>(outcome)
        assertSame(rejection, notAccepted.rejection)
        assertEquals(0, memoryCore.createProvenanceCallCount)
        assertEquals(0, memoryCore.registerDocumentCallCount)
        assertEquals(0, permissionEngine.evaluateCallCount)
    }

    // ================= 3. Provenance permission denial =================

    @Test
    fun `a denied provenance-creation permission decision returns ProvenanceNotAuthorised and never calls Memory Core`() = runTest {
        val accepted = acceptedArtifact()
        val evidenceCustodian = FakeEvidenceCustodian { _, _ -> EvidenceAcceptanceResult.Accepted(accepted) }
        val memoryCore = FakeMemoryCore(
            onCreateProvenance = { _, _ -> fail("createProvenance must not be called") },
            onRegisterDocument = { _, _ -> fail("registerDocument must not be called") },
        )
        val permissionEngine = FakePermissionEngine { request -> deniedDecision(request) }
        val coordinator = EvidenceRegistrationCoordinator(evidenceCustodian, memoryCore, permissionEngine)

        val outcome = coordinator.register(principalId, "corr-1", candidateEvidenceArtifact(), candidateProvenance(), "pdf")

        val denied = assertIs<EvidenceRegistrationOutcome.ProvenanceNotAuthorised>(outcome)
        assertSame(accepted, denied.acceptedEvidenceArtifact)
        assertEquals(0, memoryCore.createProvenanceCallCount)
        assertEquals(0, memoryCore.registerDocumentCallCount)
        assertEquals(1, permissionEngine.evaluateCallCount)
    }

    // ================= 4. Document permission denial =================

    @Test
    fun `a denied document-registration permission decision returns DocumentRegistrationNotAuthorised after Provenance already exists`() = runTest {
        val accepted = acceptedArtifact()
        val prov = provenance()
        val evidenceCustodian = FakeEvidenceCustodian { _, _ -> EvidenceAcceptanceResult.Accepted(accepted) }
        val memoryCore = FakeMemoryCore(
            onCreateProvenance = { _, _ -> prov },
            onRegisterDocument = { _, _ -> fail("registerDocument must not be called") },
        )
        var evaluateCallNumber = 0
        val permissionEngine = FakePermissionEngine { request ->
            evaluateCallNumber++
            if (evaluateCallNumber == 1) approvedDecision(request) else deniedDecision(request)
        }
        val coordinator = EvidenceRegistrationCoordinator(evidenceCustodian, memoryCore, permissionEngine)

        val outcome = coordinator.register(principalId, "corr-1", candidateEvidenceArtifact(), candidateProvenance(), "pdf")

        val denied = assertIs<EvidenceRegistrationOutcome.DocumentRegistrationNotAuthorised>(outcome)
        assertSame(accepted, denied.acceptedEvidenceArtifact)
        assertSame(prov, denied.provenance)
        assertEquals(1, memoryCore.createProvenanceCallCount)
        assertEquals(0, memoryCore.registerDocumentCallCount)
        assertEquals(2, permissionEngine.evaluateCallCount)
    }

    // ================= 5. Provenance exception propagation =================

    @Test
    fun `an exception thrown by MemoryCore createProvenance propagates unchanged and registerDocument is never called`() = runTest {
        val accepted = acceptedArtifact()
        val evidenceCustodian = FakeEvidenceCustodian { _, _ -> EvidenceAcceptanceResult.Accepted(accepted) }
        val memoryCore = FakeMemoryCore(
            onCreateProvenance = { _, _ -> throw IllegalStateException("createProvenance boom") },
            onRegisterDocument = { _, _ -> fail("registerDocument must not be called") },
        )
        val permissionEngine = FakePermissionEngine { request -> approvedDecision(request) }
        val coordinator = EvidenceRegistrationCoordinator(evidenceCustodian, memoryCore, permissionEngine)

        val thrown = assertFailsWith<IllegalStateException> {
            coordinator.register(principalId, "corr-1", candidateEvidenceArtifact(), candidateProvenance(), "pdf")
        }

        assertEquals("createProvenance boom", thrown.message)
        assertEquals(0, memoryCore.registerDocumentCallCount)
    }

    // ================= 6. Document exception propagation =================

    @Test
    fun `an exception thrown by MemoryCore registerDocument propagates unchanged`() = runTest {
        val accepted = acceptedArtifact()
        val prov = provenance()
        val evidenceCustodian = FakeEvidenceCustodian { _, _ -> EvidenceAcceptanceResult.Accepted(accepted) }
        val memoryCore = FakeMemoryCore(
            onCreateProvenance = { _, _ -> prov },
            onRegisterDocument = { _, _ -> throw IllegalStateException("registerDocument boom") },
        )
        val permissionEngine = FakePermissionEngine { request -> approvedDecision(request) }
        val coordinator = EvidenceRegistrationCoordinator(evidenceCustodian, memoryCore, permissionEngine)

        val thrown = assertFailsWith<IllegalStateException> {
            coordinator.register(principalId, "corr-1", candidateEvidenceArtifact(), candidateProvenance(), "pdf")
        }

        assertEquals("registerDocument boom", thrown.message)
        assertEquals(1, memoryCore.createProvenanceCallCount)
    }

    // ================= 7. Correct call ordering =================

    @Test
    fun `accept, createProvenance, and registerDocument occur in exactly that order`() = runTest {
        val callOrder = mutableListOf<String>()
        val accepted = acceptedArtifact()
        val prov = provenance()
        val evidenceCustodian = FakeEvidenceCustodian { _, _ ->
            callOrder += "accept"
            EvidenceAcceptanceResult.Accepted(accepted)
        }
        val memoryCore = FakeMemoryCore(
            onCreateProvenance = { _, _ ->
                callOrder += "createProvenance"
                prov
            },
            onRegisterDocument = { _, candidate ->
                callOrder += "registerDocument"
                document(candidate)
            },
        )
        val permissionEngine = FakePermissionEngine { request -> approvedDecision(request) }
        val coordinator = EvidenceRegistrationCoordinator(evidenceCustodian, memoryCore, permissionEngine)

        coordinator.register(principalId, "corr-1", candidateEvidenceArtifact(), candidateProvenance(), "pdf")

        assertEquals(listOf("accept", "createProvenance", "registerDocument"), callOrder)
    }

    // ================= 8. No subsystem coupling =================

    @Test
    fun `no subsystem coupling -- the coordinator declares exactly three instance fields, typed only as EvidenceCustodian, MemoryCore, and PermissionEngine`() {
        // Static fields are excluded deliberately: the companion object's synthetic `Companion`
        // field and its two `const val` action-name constants are not constructor dependencies,
        // and including them would make this check about the companion object's own contents
        // rather than about what this class can reach through its constructor.
        val instanceFields = EvidenceRegistrationCoordinator::class.java.declaredFields
            .filter { !Modifier.isStatic(it.modifiers) }

        assertEquals(3, instanceFields.size)
        assertEquals(
            setOf("EvidenceCustodian", "MemoryCore", "PermissionEngine"),
            instanceFields.map { it.type.simpleName }.toSet(),
        )
    }

    // ================= 9. No duplicated data =================

    @Test
    fun `no duplicated data -- the submitted CandidateDocument's locationReference and provenanceId are exactly what accept and createProvenance already produced`() = runTest {
        val accepted = acceptedArtifact(id = "evidence-42")
        val prov = provenance(id = "provenance-42")
        val evidenceCustodian = FakeEvidenceCustodian { _, _ -> EvidenceAcceptanceResult.Accepted(accepted) }
        val memoryCore = FakeMemoryCore(
            onCreateProvenance = { _, _ -> prov },
            onRegisterDocument = { _, candidate -> document(candidate) },
        )
        val permissionEngine = FakePermissionEngine { request -> approvedDecision(request) }
        val coordinator = EvidenceRegistrationCoordinator(evidenceCustodian, memoryCore, permissionEngine)

        coordinator.register(principalId, "corr-1", candidateEvidenceArtifact(), candidateProvenance(), "pdf")

        val submittedCandidate = memoryCore.lastCandidateDocument!!
        assertEquals("evidence-42", submittedCandidate.locationReference)
        assertEquals(ProvenanceId("provenance-42"), submittedCandidate.provenanceId)
    }
}
