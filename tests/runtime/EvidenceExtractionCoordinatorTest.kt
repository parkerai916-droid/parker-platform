package parker.core.runtime

import java.lang.reflect.Modifier
import java.security.MessageDigest
import java.time.Instant
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertSame
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
import parker.core.interfaces.ChronologicalLookupQuery
import parker.core.interfaces.ContentNature
import parker.core.interfaces.DecisionId
import parker.core.interfaces.DerivativeReviewRecord
import parker.core.interfaces.DerivativeReviewRegistry
import parker.core.interfaces.DerivativeReviewState
import parker.core.interfaces.Document
import parker.core.interfaces.DocumentId
import parker.core.interfaces.DocumentLookupQuery
import parker.core.interfaces.Entity
import parker.core.interfaces.EntityId
import parker.core.interfaces.EntityLookupQuery
import parker.core.interfaces.EvidenceAcceptanceResult
import parker.core.interfaces.EvidenceArtifactId
import parker.core.interfaces.EvidenceCustodian
import parker.core.interfaces.EvidenceExtractor
import parker.core.interfaces.EvidenceRetrievalResult
import parker.core.interfaces.ExecutionRequest
import parker.core.interfaces.ExtractionIdentity
import parker.core.interfaces.ExtractionOutcome
import parker.core.interfaces.ExtractionResult
import parker.core.interfaces.MemoryCore
import parker.core.interfaces.MemoryCoreRecord
import parker.core.interfaces.MemoryCoreRecordReference
import parker.core.interfaces.MemoryCoreRecordStatus
import parker.core.interfaces.MemoryRetrieval
import parker.core.interfaces.MetadataLookupQuery
import parker.core.interfaces.PermissionAction
import parker.core.interfaces.PermissionDecision
import parker.core.interfaces.PermissionDecisionOutcome
import parker.core.interfaces.PermissionLevel
import parker.core.interfaces.PrincipalId
import parker.core.interfaces.Provenance
import parker.core.interfaces.ProvenanceLookupQuery
import parker.core.interfaces.Relationship
import parker.core.interfaces.RelationshipId
import parker.core.interfaces.RelationshipTraversalQuery

/**
 * Evidence Processing (Searchable PDF), Implementation Units 4A
 * ("Identity, Integrity, and Extraction") and 4B ("Derivative Creation,
 * Registration, and Review"). Behavioural tests for
 * [EvidenceExtractionCoordinator], mirroring
 * `EvidenceRegistrationCoordinatorTest.kt`'s own established style: hand-
 * written fakes for the coordinator's own five dependencies, plus a
 * *real* [EvidenceRegistrationCoordinator] (a concrete, non-open class
 * with no interface of its own) backed by its own hand-written fakes --
 * exactly what a real caller of [EvidenceExtractionCoordinator] gets in
 * production, since this Unit reuses that coordinator entirely unchanged.
 */
class EvidenceExtractionCoordinatorTest {

    private val fixedInstant: Instant = Instant.parse("2026-01-01T00:00:00Z")
    private val principalId = PrincipalId("owner-1")
    private val documentId = DocumentId("document-1")
    private val evidenceArtifactId = EvidenceArtifactId("evidence-1")
    private val originalContent = byteArrayOf(1, 2, 3, 4)

    private fun sha256Hex(bytes: ByteArray): String =
        MessageDigest.getInstance("SHA-256").digest(bytes).joinToString("") { "%02x".format(it.toInt() and 0xFF) }

    private fun document(integrityHash: String? = sha256Hex(originalContent)) = Document(
        documentId = documentId,
        documentType = "pdf",
        locationReference = evidenceArtifactId.value,
        provenanceId = parker.core.interfaces.ProvenanceId("provenance-original"),
        registeredAt = fixedInstant,
        integrityHash = integrityHash,
    )

    private fun extractionResult(text: String = "Hello, Parker searchable PDF.") = ExtractionResult(
        extractedText = text,
        detectedMediaType = "application/pdf",
        extractorName = "Apache Tika",
        extractorVersion = "3.3.1",
        extractionIdentity = ExtractionIdentity(
            parserIdentity = "org.apache.tika.parser.pdf.PDFParser",
            configurationProfile = "tika-pdf-only-v1",
            normalisationProfile = "none",
        ),
        extractedAt = fixedInstant,
        ocrUsed = false,
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

    // ================= Fakes =================

    private class FakeEvidenceCustodian(
        private val onRetrieve: (PrincipalId, EvidenceArtifactId) -> EvidenceRetrievalResult = { _, _ -> fail("retrieve must not be called") },
        private val onAccept: (PrincipalId, CandidateEvidenceArtifact) -> EvidenceAcceptanceResult = { _, _ -> fail("accept must not be called") },
    ) : EvidenceCustodian {
        var retrieveCallCount = 0
            private set

        override suspend fun accept(requestingPrincipalId: PrincipalId, candidate: CandidateEvidenceArtifact): EvidenceAcceptanceResult =
            onAccept(requestingPrincipalId, candidate)

        override suspend fun retrieve(requestingPrincipalId: PrincipalId, evidenceArtifactId: EvidenceArtifactId): EvidenceRetrievalResult {
            retrieveCallCount++
            return onRetrieve(requestingPrincipalId, evidenceArtifactId)
        }
    }

    private class FakeMemoryRetrieval(
        private val onGetDocument: (PrincipalId, DocumentId) -> Document? = { _, _ -> fail("getDocument must not be called") },
    ) : MemoryRetrieval {
        var getDocumentCallCount = 0
            private set

        override suspend fun getDocument(requestingPrincipalId: PrincipalId, documentId: DocumentId): Document? {
            getDocumentCallCount++
            return onGetDocument(requestingPrincipalId, documentId)
        }

        override suspend fun getEntity(requestingPrincipalId: PrincipalId, entityId: EntityId): Entity? =
            throw UnsupportedOperationException("not used by EvidenceExtractionCoordinator")

        override suspend fun getAssertion(requestingPrincipalId: PrincipalId, assertionId: parker.core.interfaces.AssertionId): Assertion? =
            throw UnsupportedOperationException("not used by EvidenceExtractionCoordinator")

        override suspend fun getRelationship(requestingPrincipalId: PrincipalId, relationshipId: RelationshipId): Relationship? =
            throw UnsupportedOperationException("not used by EvidenceExtractionCoordinator")

        override suspend fun findEntities(query: EntityLookupQuery): List<Entity> =
            throw UnsupportedOperationException("not used by EvidenceExtractionCoordinator")

        override suspend fun findDocuments(query: DocumentLookupQuery): List<Document> =
            throw UnsupportedOperationException("not used by EvidenceExtractionCoordinator")

        override suspend fun traverseRelationships(query: RelationshipTraversalQuery): List<Relationship> =
            throw UnsupportedOperationException("not used by EvidenceExtractionCoordinator")

        override suspend fun findByTimeRange(query: ChronologicalLookupQuery): List<MemoryCoreRecord> =
            throw UnsupportedOperationException("not used by EvidenceExtractionCoordinator")

        override suspend fun findByMetadata(query: MetadataLookupQuery): List<MemoryCoreRecord> =
            throw UnsupportedOperationException("not used by EvidenceExtractionCoordinator")

        override suspend fun findByProvenance(query: ProvenanceLookupQuery): List<MemoryCoreRecord> =
            throw UnsupportedOperationException("not used by EvidenceExtractionCoordinator")
    }

    private class FakeEvidenceExtractor(
        private val onExtract: (ByteArray) -> ExtractionOutcome,
    ) : EvidenceExtractor {
        var extractCallCount = 0
            private set

        override suspend fun extract(content: ByteArray): ExtractionOutcome {
            extractCallCount++
            return onExtract(content)
        }
    }

    /** Backs the *real* [EvidenceRegistrationCoordinator] this test constructs -- only createProvenance/registerDocument are ever called. */
    private class FakeMemoryCoreForRegistration(
        private val onCreateProvenance: (PrincipalId, CandidateProvenance) -> Provenance,
        private val onRegisterDocument: (PrincipalId, CandidateDocument) -> Document,
    ) : MemoryCore {
        override suspend fun createProvenance(requestingPrincipalId: PrincipalId, candidate: CandidateProvenance): Provenance =
            onCreateProvenance(requestingPrincipalId, candidate)

        override suspend fun registerDocument(requestingPrincipalId: PrincipalId, candidate: CandidateDocument): Document =
            onRegisterDocument(requestingPrincipalId, candidate)

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

    private class FakeDerivativeReviewRegistry(
        private val onRecord: (DerivativeReviewRecord) -> Unit = {},
    ) : DerivativeReviewRegistry {
        val recordedStates = mutableListOf<DerivativeReviewRecord>()

        override suspend fun recordReviewState(record: DerivativeReviewRecord) {
            recordedStates.add(record)
            onRecord(record)
        }

        override suspend fun currentReviewState(evidenceArtifactId: EvidenceArtifactId): DerivativeReviewState? =
            recordedStates.filter { it.evidenceArtifactId == evidenceArtifactId }.lastOrNull()?.state
    }

    /** A real EvidenceRegistrationCoordinator (concrete, no interface) that always succeeds, registering under [documentId2]. */
    private fun realSucceedingRegistrationCoordinator(
        evidenceCustodian: EvidenceCustodian,
        documentId2: DocumentId = DocumentId("derivative-document-1"),
        derivativeEvidenceArtifactId: EvidenceArtifactId = EvidenceArtifactId("derivative-evidence-1"),
    ): EvidenceRegistrationCoordinator {
        val custodianForAccept = object : EvidenceCustodian {
            override suspend fun accept(requestingPrincipalId: PrincipalId, candidate: CandidateEvidenceArtifact) =
                EvidenceAcceptanceResult.Accepted(
                    parker.core.interfaces.AcceptedEvidenceArtifact(derivativeEvidenceArtifactId, fixedInstant),
                )

            override suspend fun retrieve(requestingPrincipalId: PrincipalId, evidenceArtifactId: EvidenceArtifactId) =
                fail("not used")
        }
        val memoryCore = FakeMemoryCoreForRegistration(
            onCreateProvenance = { _, candidate ->
                Provenance(
                    provenanceId = parker.core.interfaces.ProvenanceId("derivative-provenance-1"),
                    sourceIdentifier = candidate.sourceIdentifier,
                    sourceType = candidate.sourceType,
                    acquisitionTime = candidate.acquisitionTime,
                    ingestionTime = fixedInstant,
                    contentNature = candidate.contentNature,
                    creator = candidate.creator,
                    extractedFrom = candidate.extractedFrom,
                    processingHistory = candidate.processingHistory,
                    confidence = candidate.confidence,
                )
            },
            onRegisterDocument = { _, candidate ->
                Document(
                    documentId = documentId2,
                    documentType = candidate.documentType,
                    locationReference = candidate.locationReference,
                    provenanceId = candidate.provenanceId,
                    registeredAt = fixedInstant,
                    integrityHash = candidate.integrityHash,
                    metadata = candidate.metadata,
                )
            },
        )
        val permissionEngine = FakePermissionEngine { request -> approvedDecision(request) }
        return EvidenceRegistrationCoordinator(custodianForAccept, memoryCore, permissionEngine)
    }

    /** A real EvidenceRegistrationCoordinator that always returns [outcome], for any non-Registered branch. */
    private fun realCoordinatorReturning(outcome: EvidenceRegistrationOutcome): EvidenceRegistrationCoordinator {
        val custodian = object : EvidenceCustodian {
            override suspend fun accept(requestingPrincipalId: PrincipalId, candidate: CandidateEvidenceArtifact) =
                when (outcome) {
                    is EvidenceRegistrationOutcome.NotAccepted -> outcome.rejection
                    else -> fail("accept must not be reached for this outcome")
                }

            override suspend fun retrieve(requestingPrincipalId: PrincipalId, evidenceArtifactId: EvidenceArtifactId) = fail("not used")
        }
        if (outcome is EvidenceRegistrationOutcome.NotAccepted) {
            val memoryCore = FakeMemoryCoreForRegistration(
                onCreateProvenance = { _, _ -> fail("createProvenance must not be called") },
                onRegisterDocument = { _, _ -> fail("registerDocument must not be called") },
            )
            return EvidenceRegistrationCoordinator(custodian, memoryCore, FakePermissionEngine { approvedDecision(it) })
        }
        // ProvenanceNotAuthorised / DocumentRegistrationNotAuthorised: accept succeeds, then permission denies.
        val acceptingCustodian = object : EvidenceCustodian {
            override suspend fun accept(requestingPrincipalId: PrincipalId, candidate: CandidateEvidenceArtifact) =
                EvidenceAcceptanceResult.Accepted(
                    parker.core.interfaces.AcceptedEvidenceArtifact(EvidenceArtifactId("derivative-evidence-1"), fixedInstant),
                )

            override suspend fun retrieve(requestingPrincipalId: PrincipalId, evidenceArtifactId: EvidenceArtifactId) = fail("not used")
        }
        val provenanceOnly = outcome is EvidenceRegistrationOutcome.DocumentRegistrationNotAuthorised
        var evaluateCount = 0
        val memoryCore = FakeMemoryCoreForRegistration(
            onCreateProvenance = { _, candidate ->
                if (!provenanceOnly) fail("createProvenance must not be called for ProvenanceNotAuthorised")
                Provenance(
                    provenanceId = parker.core.interfaces.ProvenanceId("derivative-provenance-1"),
                    sourceIdentifier = candidate.sourceIdentifier,
                    sourceType = candidate.sourceType,
                    acquisitionTime = candidate.acquisitionTime,
                    ingestionTime = fixedInstant,
                    contentNature = candidate.contentNature,
                )
            },
            onRegisterDocument = { _, _ -> fail("registerDocument must never be called by this Unit's own coordinator") },
        )
        val permissionEngine = FakePermissionEngine { request ->
            evaluateCount++
            // ProvenanceNotAuthorised: deny the first (createProvenance) evaluation outright.
            // DocumentRegistrationNotAuthorised: approve the first (createProvenance) evaluation
            // so it genuinely succeeds, then deny the second (registerDocument) evaluation --
            // otherwise EvidenceRegistrationCoordinator would always short-circuit at
            // ProvenanceNotAuthorised and DocumentRegistrationNotAuthorised could never be reached.
            val approveThisEvaluation = provenanceOnly && evaluateCount == 1
            if (approveThisEvaluation) approvedDecision(request) else approvedDecision(request).copy(decision = PermissionDecisionOutcome.DENIED)
        }
        return EvidenceRegistrationCoordinator(acceptingCustodian, memoryCore, permissionEngine)
    }

    // ================= Unit 4A: source document not found =================

    @Test
    fun `source document not found returns SourceDocumentNotFound and never retrieves`() = runTest {
        val evidenceCustodian = FakeEvidenceCustodian()
        val memoryRetrieval = FakeMemoryRetrieval(onGetDocument = { _, _ -> null })
        val extractor = FakeEvidenceExtractor { fail("extract must not be called") }
        val reviewRegistry = FakeDerivativeReviewRegistry()
        val coordinator = EvidenceExtractionCoordinator(
            evidenceCustodian, memoryRetrieval, extractor,
            realCoordinatorReturning(EvidenceRegistrationOutcome.NotAccepted(EvidenceAcceptanceResult.Rejected("unused"))),
            reviewRegistry,
        )

        val outcome = coordinator.extract(principalId, documentId, evidenceArtifactId)

        assertIs<EvidenceExtractionOutcome.SourceDocumentNotFound>(outcome)
        assertEquals(0, evidenceCustodian.retrieveCallCount)
        assertEquals(0, reviewRegistry.recordedStates.size)
    }

    // ================= Unit 4A: source identity mismatch =================

    @Test
    fun `source identity mismatch is detected before any retrieval occurs`() = runTest {
        val evidenceCustodian = FakeEvidenceCustodian()
        val mismatchedDocument = document().copy(locationReference = "a-completely-different-artifact-id")
        val memoryRetrieval = FakeMemoryRetrieval(onGetDocument = { _, _ -> mismatchedDocument })
        val extractor = FakeEvidenceExtractor { fail("extract must not be called") }
        val coordinator = EvidenceExtractionCoordinator(
            evidenceCustodian, memoryRetrieval, extractor,
            realCoordinatorReturning(EvidenceRegistrationOutcome.NotAccepted(EvidenceAcceptanceResult.Rejected("unused"))),
            FakeDerivativeReviewRegistry(),
        )

        val outcome = coordinator.extract(principalId, documentId, evidenceArtifactId)

        val mismatch = assertIs<EvidenceExtractionOutcome.SourceIdentityMismatch>(outcome)
        assertEquals(documentId, mismatch.documentId)
        assertEquals(evidenceArtifactId, mismatch.evidenceArtifactId)
        assertEquals(0, evidenceCustodian.retrieveCallCount, "retrieval must never occur after an identity mismatch")
    }

    // ================= Unit 4A: retrieval rejected / not found =================

    @Test
    fun `retrieval rejected wraps EvidenceRetrievalResult-Rejected unchanged`() = runTest {
        val rejection = EvidenceRetrievalResult.Rejected(evidenceArtifactId, "denied by policy")
        val memoryRetrieval = FakeMemoryRetrieval(onGetDocument = { _, _ -> document() })
        val evidenceCustodian = FakeEvidenceCustodian(onRetrieve = { _, _ -> rejection })
        val coordinator = EvidenceExtractionCoordinator(
            evidenceCustodian, memoryRetrieval, FakeEvidenceExtractor { fail("extract must not be called") },
            realCoordinatorReturning(EvidenceRegistrationOutcome.NotAccepted(EvidenceAcceptanceResult.Rejected("unused"))),
            FakeDerivativeReviewRegistry(),
        )

        val outcome = coordinator.extract(principalId, documentId, evidenceArtifactId)

        val wrapped = assertIs<EvidenceExtractionOutcome.SourceRetrievalRejected>(outcome)
        assertSame(rejection, wrapped.rejection)
    }

    @Test
    fun `retrieval not found wraps EvidenceRetrievalResult-NotFound unchanged`() = runTest {
        val notFound = EvidenceRetrievalResult.NotFound(evidenceArtifactId)
        val memoryRetrieval = FakeMemoryRetrieval(onGetDocument = { _, _ -> document() })
        val evidenceCustodian = FakeEvidenceCustodian(onRetrieve = { _, _ -> notFound })
        val coordinator = EvidenceExtractionCoordinator(
            evidenceCustodian, memoryRetrieval, FakeEvidenceExtractor { fail("extract must not be called") },
            realCoordinatorReturning(EvidenceRegistrationOutcome.NotAccepted(EvidenceAcceptanceResult.Rejected("unused"))),
            FakeDerivativeReviewRegistry(),
        )

        val outcome = coordinator.extract(principalId, documentId, evidenceArtifactId)

        val wrapped = assertIs<EvidenceExtractionOutcome.SourceNotFound>(outcome)
        assertSame(notFound, wrapped.notFound)
    }

    // ================= Unit 4A: integrity verification =================

    @Test
    fun `a verified integrity match continues to extraction`() = runTest {
        val memoryRetrieval = FakeMemoryRetrieval(onGetDocument = { _, _ -> document(integrityHash = sha256Hex(originalContent)) })
        val evidenceCustodian = FakeEvidenceCustodian(onRetrieve = { _, _ -> EvidenceRetrievalResult.Found(evidenceArtifactId, originalContent) })
        val extractor = FakeEvidenceExtractor { ExtractionOutcome.Extracted(extractionResult()) }
        val coordinator = EvidenceExtractionCoordinator(
            evidenceCustodian, memoryRetrieval, extractor,
            realSucceedingRegistrationCoordinator(evidenceCustodian),
            FakeDerivativeReviewRegistry(),
        )

        val outcome = coordinator.extract(principalId, documentId, evidenceArtifactId)

        assertIs<EvidenceExtractionOutcome.Completed>(outcome)
        assertEquals(1, extractor.extractCallCount)
    }

    @Test
    fun `a recorded integrity mismatch fails closed -- extraction never runs`() = runTest {
        val memoryRetrieval = FakeMemoryRetrieval(onGetDocument = { _, _ -> document(integrityHash = "0".repeat(64)) })
        val evidenceCustodian = FakeEvidenceCustodian(onRetrieve = { _, _ -> EvidenceRetrievalResult.Found(evidenceArtifactId, originalContent) })
        val extractor = FakeEvidenceExtractor { fail("extract must not be called") }
        val coordinator = EvidenceExtractionCoordinator(
            evidenceCustodian, memoryRetrieval, extractor,
            realCoordinatorReturning(EvidenceRegistrationOutcome.NotAccepted(EvidenceAcceptanceResult.Rejected("unused"))),
            FakeDerivativeReviewRegistry(),
        )

        val outcome = coordinator.extract(principalId, documentId, evidenceArtifactId)

        val mismatch = assertIs<EvidenceExtractionOutcome.IntegrityMismatch>(outcome)
        assertEquals(sha256Hex(originalContent), mismatch.mismatch.computedDigest)
        assertEquals("0".repeat(64), mismatch.mismatch.recordedDigest)
    }

    @Test
    fun `an absent recorded hash fails closed as Unverifiable, distinctly from Mismatch`() = runTest {
        val memoryRetrieval = FakeMemoryRetrieval(onGetDocument = { _, _ -> document(integrityHash = null) })
        val evidenceCustodian = FakeEvidenceCustodian(onRetrieve = { _, _ -> EvidenceRetrievalResult.Found(evidenceArtifactId, originalContent) })
        val extractor = FakeEvidenceExtractor { fail("extract must not be called") }
        val coordinator = EvidenceExtractionCoordinator(
            evidenceCustodian, memoryRetrieval, extractor,
            realCoordinatorReturning(EvidenceRegistrationOutcome.NotAccepted(EvidenceAcceptanceResult.Rejected("unused"))),
            FakeDerivativeReviewRegistry(),
        )

        val outcome = coordinator.extract(principalId, documentId, evidenceArtifactId)

        assertIs<EvidenceExtractionOutcome.IntegrityUnverifiable>(outcome)
    }

    // ================= Unit 4A: extractor terminal outcomes =================

    @Test
    fun `RequiresOcr from the extractor wraps unchanged and stops the pipeline`() = runTest {
        val requiresOcr = ExtractionOutcome.RequiresOcr(detectedMediaType = "application/pdf", pageCount = 3)
        val memoryRetrieval = FakeMemoryRetrieval(onGetDocument = { _, _ -> document() })
        val evidenceCustodian = FakeEvidenceCustodian(onRetrieve = { _, _ -> EvidenceRetrievalResult.Found(evidenceArtifactId, originalContent) })
        val extractor = FakeEvidenceExtractor { requiresOcr }
        val reviewRegistry = FakeDerivativeReviewRegistry()
        val coordinator = EvidenceExtractionCoordinator(
            evidenceCustodian, memoryRetrieval, extractor,
            realCoordinatorReturning(EvidenceRegistrationOutcome.NotAccepted(EvidenceAcceptanceResult.Rejected("unused"))),
            reviewRegistry,
        )

        val outcome = coordinator.extract(principalId, documentId, evidenceArtifactId)

        val wrapped = assertIs<EvidenceExtractionOutcome.RequiresOcr>(outcome)
        assertSame(requiresOcr, wrapped.outcome)
        assertEquals(0, reviewRegistry.recordedStates.size)
    }

    @Test
    fun `Unsupported from the extractor wraps unchanged and stops the pipeline`() = runTest {
        val unsupported = ExtractionOutcome.Unsupported(detectedMediaType = "text/plain", reason = "not a PDF")
        val memoryRetrieval = FakeMemoryRetrieval(onGetDocument = { _, _ -> document() })
        val evidenceCustodian = FakeEvidenceCustodian(onRetrieve = { _, _ -> EvidenceRetrievalResult.Found(evidenceArtifactId, originalContent) })
        val extractor = FakeEvidenceExtractor { unsupported }
        val coordinator = EvidenceExtractionCoordinator(
            evidenceCustodian, memoryRetrieval, extractor,
            realCoordinatorReturning(EvidenceRegistrationOutcome.NotAccepted(EvidenceAcceptanceResult.Rejected("unused"))),
            FakeDerivativeReviewRegistry(),
        )

        val outcome = coordinator.extract(principalId, documentId, evidenceArtifactId)

        val wrapped = assertIs<EvidenceExtractionOutcome.Unsupported>(outcome)
        assertSame(unsupported, wrapped.outcome)
    }

    @Test
    fun `Malformed from the extractor wraps unchanged and stops the pipeline`() = runTest {
        val malformed = ExtractionOutcome.Malformed(reason = "corrupt bytes")
        val memoryRetrieval = FakeMemoryRetrieval(onGetDocument = { _, _ -> document() })
        val evidenceCustodian = FakeEvidenceCustodian(onRetrieve = { _, _ -> EvidenceRetrievalResult.Found(evidenceArtifactId, originalContent) })
        val extractor = FakeEvidenceExtractor { malformed }
        val coordinator = EvidenceExtractionCoordinator(
            evidenceCustodian, memoryRetrieval, extractor,
            realCoordinatorReturning(EvidenceRegistrationOutcome.NotAccepted(EvidenceAcceptanceResult.Rejected("unused"))),
            FakeDerivativeReviewRegistry(),
        )

        val outcome = coordinator.extract(principalId, documentId, evidenceArtifactId)

        val wrapped = assertIs<EvidenceExtractionOutcome.Malformed>(outcome)
        assertSame(malformed, wrapped.outcome)
    }

    // ================= Unit 4B: registration outcomes, no review state except on Registered =================

    @Test
    fun `NotAccepted registration outcome is surfaced and no review state is recorded`() = runTest {
        val memoryRetrieval = FakeMemoryRetrieval(onGetDocument = { _, _ -> document() })
        val evidenceCustodian = FakeEvidenceCustodian(onRetrieve = { _, _ -> EvidenceRetrievalResult.Found(evidenceArtifactId, originalContent) })
        val extractor = FakeEvidenceExtractor { ExtractionOutcome.Extracted(extractionResult()) }
        val reviewRegistry = FakeDerivativeReviewRegistry()
        val notAccepted = EvidenceRegistrationOutcome.NotAccepted(EvidenceAcceptanceResult.Rejected("derivative acceptance denied"))
        val coordinator = EvidenceExtractionCoordinator(
            evidenceCustodian, memoryRetrieval, extractor, realCoordinatorReturning(notAccepted), reviewRegistry,
        )

        val outcome = coordinator.extract(principalId, documentId, evidenceArtifactId)

        val completed = assertIs<EvidenceExtractionOutcome.Completed>(outcome)
        assertIs<EvidenceRegistrationOutcome.NotAccepted>(completed.registrationOutcome)
        assertEquals(0, reviewRegistry.recordedStates.size)
    }

    @Test
    fun `ProvenanceNotAuthorised registration outcome is surfaced and no review state is recorded`() = runTest {
        val memoryRetrieval = FakeMemoryRetrieval(onGetDocument = { _, _ -> document() })
        val evidenceCustodian = FakeEvidenceCustodian(onRetrieve = { _, _ -> EvidenceRetrievalResult.Found(evidenceArtifactId, originalContent) })
        val extractor = FakeEvidenceExtractor { ExtractionOutcome.Extracted(extractionResult()) }
        val reviewRegistry = FakeDerivativeReviewRegistry()
        val outcomeTemplate = EvidenceRegistrationOutcome.ProvenanceNotAuthorised(
            parker.core.interfaces.AcceptedEvidenceArtifact(EvidenceArtifactId("derivative-evidence-1"), fixedInstant),
            "denied",
        )
        val coordinator = EvidenceExtractionCoordinator(
            evidenceCustodian, memoryRetrieval, extractor, realCoordinatorReturning(outcomeTemplate), reviewRegistry,
        )

        val outcome = coordinator.extract(principalId, documentId, evidenceArtifactId)

        val completed = assertIs<EvidenceExtractionOutcome.Completed>(outcome)
        assertIs<EvidenceRegistrationOutcome.ProvenanceNotAuthorised>(completed.registrationOutcome)
        assertEquals(0, reviewRegistry.recordedStates.size)
    }

    @Test
    fun `DocumentRegistrationNotAuthorised registration outcome is surfaced and no review state is recorded`() = runTest {
        val memoryRetrieval = FakeMemoryRetrieval(onGetDocument = { _, _ -> document() })
        val evidenceCustodian = FakeEvidenceCustodian(onRetrieve = { _, _ -> EvidenceRetrievalResult.Found(evidenceArtifactId, originalContent) })
        val extractor = FakeEvidenceExtractor { ExtractionOutcome.Extracted(extractionResult()) }
        val reviewRegistry = FakeDerivativeReviewRegistry()
        val provenance = Provenance(
            provenanceId = parker.core.interfaces.ProvenanceId("derivative-provenance-1"),
            sourceIdentifier = "x", sourceType = "extraction", acquisitionTime = fixedInstant,
            ingestionTime = fixedInstant, contentNature = ContentNature.EXTRACTED,
        )
        val outcomeTemplate = EvidenceRegistrationOutcome.DocumentRegistrationNotAuthorised(
            parker.core.interfaces.AcceptedEvidenceArtifact(EvidenceArtifactId("derivative-evidence-1"), fixedInstant),
            provenance,
            "denied",
        )
        val coordinator = EvidenceExtractionCoordinator(
            evidenceCustodian, memoryRetrieval, extractor, realCoordinatorReturning(outcomeTemplate), reviewRegistry,
        )

        val outcome = coordinator.extract(principalId, documentId, evidenceArtifactId)

        val completed = assertIs<EvidenceExtractionOutcome.Completed>(outcome)
        assertIs<EvidenceRegistrationOutcome.DocumentRegistrationNotAuthorised>(completed.registrationOutcome)
        assertEquals(0, reviewRegistry.recordedStates.size)
    }

    // ================= Unit 4B: full successful path =================

    @Test
    fun `the full successful path records exactly one PENDING_REVIEW state for the newly registered derivative`() = runTest {
        val memoryRetrieval = FakeMemoryRetrieval(onGetDocument = { _, _ -> document() })
        val evidenceCustodian = FakeEvidenceCustodian(onRetrieve = { _, _ -> EvidenceRetrievalResult.Found(evidenceArtifactId, originalContent) })
        val extractor = FakeEvidenceExtractor { ExtractionOutcome.Extracted(extractionResult()) }
        val reviewRegistry = FakeDerivativeReviewRegistry()
        val derivativeDocumentId = DocumentId("derivative-document-1")
        val derivativeEvidenceArtifactId = EvidenceArtifactId("derivative-evidence-1")
        val coordinator = EvidenceExtractionCoordinator(
            evidenceCustodian, memoryRetrieval, extractor,
            realSucceedingRegistrationCoordinator(evidenceCustodian, derivativeDocumentId, derivativeEvidenceArtifactId),
            reviewRegistry,
        )

        val outcome = coordinator.extract(principalId, documentId, evidenceArtifactId)

        val completed = assertIs<EvidenceExtractionOutcome.Completed>(outcome)
        val registered = assertIs<EvidenceRegistrationOutcome.Registered>(completed.registrationOutcome)
        assertEquals(1, reviewRegistry.recordedStates.size)
        val recorded = reviewRegistry.recordedStates.single()
        assertEquals(DerivativeReviewState.PENDING_REVIEW, recorded.state)
        assertEquals(derivativeEvidenceArtifactId, recorded.evidenceArtifactId)
        assertEquals(derivativeDocumentId, recorded.documentId)
        assertEquals(registered.acceptedEvidenceArtifact.evidenceArtifactId, recorded.evidenceArtifactId)
        assertEquals(registered.document.documentId, recorded.documentId)
    }

    @Test
    fun `the derivative's recorded provenance carries every reproducibility fact unaltered`() = runTest {
        val memoryRetrieval = FakeMemoryRetrieval(onGetDocument = { _, _ -> document() })
        val evidenceCustodian = FakeEvidenceCustodian(onRetrieve = { _, _ -> EvidenceRetrievalResult.Found(evidenceArtifactId, originalContent) })
        val result = extractionResult()
        val extractor = FakeEvidenceExtractor { ExtractionOutcome.Extracted(result) }
        val coordinator = EvidenceExtractionCoordinator(
            evidenceCustodian, memoryRetrieval, extractor,
            realSucceedingRegistrationCoordinator(evidenceCustodian),
            FakeDerivativeReviewRegistry(),
        )

        val outcome = coordinator.extract(principalId, documentId, evidenceArtifactId)

        val completed = assertIs<EvidenceExtractionOutcome.Completed>(outcome)
        val registered = assertIs<EvidenceRegistrationOutcome.Registered>(completed.registrationOutcome)
        assertEquals(ContentNature.EXTRACTED, registered.provenance.contentNature)
        assertEquals(documentId, registered.provenance.extractedFrom)
        assertNull(registered.provenance.confidence, "confidence must never be fabricated for deterministic extraction")
        assertTrue(registered.provenance.processingHistory.any { it.contains(result.extractionIdentity.parserIdentity) })
        assertTrue(registered.provenance.processingHistory.any { it.startsWith("originalEvidenceDigestSha256=") })
        assertTrue(registered.provenance.processingHistory.any { it == "ocrUsed=false" })
        assertEquals("extracted-text", registered.document.documentType)
    }

    // ================= Fault propagation =================

    @Test
    fun `a genuine exception from getDocument propagates unchanged`() = runTest {
        val memoryRetrieval = FakeMemoryRetrieval(onGetDocument = { _, _ -> throw IllegalStateException("getDocument boom") })
        val coordinator = EvidenceExtractionCoordinator(
            FakeEvidenceCustodian(), memoryRetrieval, FakeEvidenceExtractor { fail("must not be called") },
            realCoordinatorReturning(EvidenceRegistrationOutcome.NotAccepted(EvidenceAcceptanceResult.Rejected("unused"))),
            FakeDerivativeReviewRegistry(),
        )

        val thrown = assertFailsWith<IllegalStateException> { coordinator.extract(principalId, documentId, evidenceArtifactId) }
        assertEquals("getDocument boom", thrown.message)
    }

    @Test
    fun `a genuine exception from retrieve propagates unchanged`() = runTest {
        val memoryRetrieval = FakeMemoryRetrieval(onGetDocument = { _, _ -> document() })
        val evidenceCustodian = FakeEvidenceCustodian(onRetrieve = { _, _ -> throw IllegalStateException("retrieve boom") })
        val coordinator = EvidenceExtractionCoordinator(
            evidenceCustodian, memoryRetrieval, FakeEvidenceExtractor { fail("must not be called") },
            realCoordinatorReturning(EvidenceRegistrationOutcome.NotAccepted(EvidenceAcceptanceResult.Rejected("unused"))),
            FakeDerivativeReviewRegistry(),
        )

        val thrown = assertFailsWith<IllegalStateException> { coordinator.extract(principalId, documentId, evidenceArtifactId) }
        assertEquals("retrieve boom", thrown.message)
    }

    @Test
    fun `a genuine exception from extract propagates unchanged`() = runTest {
        val memoryRetrieval = FakeMemoryRetrieval(onGetDocument = { _, _ -> document() })
        val evidenceCustodian = FakeEvidenceCustodian(onRetrieve = { _, _ -> EvidenceRetrievalResult.Found(evidenceArtifactId, originalContent) })
        val extractor = FakeEvidenceExtractor { throw IllegalStateException("extract boom") }
        val coordinator = EvidenceExtractionCoordinator(
            evidenceCustodian, memoryRetrieval, extractor,
            realCoordinatorReturning(EvidenceRegistrationOutcome.NotAccepted(EvidenceAcceptanceResult.Rejected("unused"))),
            FakeDerivativeReviewRegistry(),
        )

        val thrown = assertFailsWith<IllegalStateException> { coordinator.extract(principalId, documentId, evidenceArtifactId) }
        assertEquals("extract boom", thrown.message)
    }

    @Test
    fun `a genuine exception from recordReviewState propagates unchanged after a successful registration`() = runTest {
        val memoryRetrieval = FakeMemoryRetrieval(onGetDocument = { _, _ -> document() })
        val evidenceCustodian = FakeEvidenceCustodian(onRetrieve = { _, _ -> EvidenceRetrievalResult.Found(evidenceArtifactId, originalContent) })
        val extractor = FakeEvidenceExtractor { ExtractionOutcome.Extracted(extractionResult()) }
        val reviewRegistry = FakeDerivativeReviewRegistry(onRecord = { throw IllegalStateException("recordReviewState boom") })
        val coordinator = EvidenceExtractionCoordinator(
            evidenceCustodian, memoryRetrieval, extractor,
            realSucceedingRegistrationCoordinator(evidenceCustodian),
            reviewRegistry,
        )

        val thrown = assertFailsWith<IllegalStateException> { coordinator.extract(principalId, documentId, evidenceArtifactId) }
        assertEquals("recordReviewState boom", thrown.message)
    }

    // ================= Structural safeguards =================

    @Test
    fun `EvidenceExtractionCoordinator declares exactly five instance fields, typed only as its five governed dependencies`() {
        val instanceFields = EvidenceExtractionCoordinator::class.java.declaredFields
            .filter { !Modifier.isStatic(it.modifiers) }

        assertEquals(5, instanceFields.size, "found: ${instanceFields.map { it.name to it.type.simpleName }}")
        assertEquals(
            setOf("EvidenceCustodian", "MemoryRetrieval", "EvidenceExtractor", "EvidenceRegistrationCoordinator", "DerivativeReviewRegistry"),
            instanceFields.map { it.type.simpleName }.toSet(),
        )
    }

    @Test
    fun `EvidenceExtractionCoordinator holds no MemoryCore, EvidenceIntelligence, or ParkerRuntime reference anywhere`() {
        val forbiddenTypeNames = setOf("MemoryCore", "EvidenceIntelligence", "ParkerRuntime", "PermissionEngine")
        val fieldTypeNames = EvidenceExtractionCoordinator::class.java.declaredFields.map { it.type.simpleName }.toSet()

        assertTrue(
            fieldTypeNames.intersect(forbiddenTypeNames).isEmpty(),
            "must hold no reference to $forbiddenTypeNames -- found fields typed: $fieldTypeNames",
        )
    }
}
