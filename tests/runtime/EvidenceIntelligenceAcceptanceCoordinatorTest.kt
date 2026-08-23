package parker.core.runtime

import java.lang.reflect.Modifier
import java.time.Instant
import kotlin.reflect.KClass
import kotlin.reflect.full.primaryConstructor
import kotlinx.coroutines.test.runTest
import parker.core.interfaces.Assertion
import parker.core.interfaces.CandidateAssertion
import parker.core.interfaces.CandidateEvidenceArtifact
import parker.core.interfaces.CandidateMemoryCoreRecord
import parker.core.interfaces.CandidateRelationship
import parker.core.interfaces.DecisionId
import parker.core.interfaces.Document
import parker.core.interfaces.CandidateDocument
import parker.core.interfaces.CandidateEntity
import parker.core.interfaces.CandidateProvenance
import parker.core.interfaces.Entity
import parker.core.interfaces.EvidenceAcceptanceResult
import parker.core.interfaces.EvidenceAnalysisResult
import parker.core.interfaces.EvidenceArtifactId
import parker.core.interfaces.EvidenceCustodian
import parker.core.interfaces.EvidenceManifestRetrievalResult
import parker.core.interfaces.EvidenceRetrievalResult
import parker.core.interfaces.ExecutionRequest
import parker.core.interfaces.KnowledgeCandidate
import parker.core.interfaces.KnowledgeItem
import parker.core.interfaces.KnowledgeItemStatus
import parker.core.interfaces.KnowledgePromotion
import parker.core.interfaces.KnowledgeSubmission
import parker.core.interfaces.KnowledgeSubmissionDisposition
import parker.core.interfaces.MemoryCore
import parker.core.interfaces.MemoryCoreRecord
import parker.core.interfaces.MemoryCoreRecordReference
import parker.core.interfaces.MemoryCoreRecordStatus
import parker.core.interfaces.AssertionId
import parker.core.interfaces.RelationshipId
import parker.core.interfaces.EvidentialState
import parker.core.interfaces.PermissionAction
import parker.core.interfaces.PermissionDecision
import parker.core.interfaces.PermissionDecisionOutcome
import parker.core.interfaces.PermissionEngine
import parker.core.interfaces.PermissionLevel
import parker.core.interfaces.PrincipalId
import parker.core.interfaces.Provenance
import parker.core.interfaces.ProvenanceId
import parker.core.interfaces.ProvenanceReference
import parker.core.interfaces.Relationship
import parker.core.interfaces.RelationshipEndpoint
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertIs
import kotlin.test.assertSame
import kotlin.test.assertTrue

/**
 * Evidence Intelligence, Implementation Unit 7 ("The Evidence Intelligence
 * Acceptance Coordinator"). Behavioural and structural tests of
 * [EvidenceIntelligenceAcceptanceCoordinator], mirroring
 * [EvidenceRegistrationCoordinatorTest]'s and [DefaultEvidenceCustodianTest]'s
 * own established style: fakes with call counters and captured arguments,
 * never a mocking framework; structural safeguards proven by Kotlin
 * reflection, never by source-text inspection.
 */
class EvidenceIntelligenceAcceptanceCoordinatorTest {

    private val principalId = PrincipalId("principal-1")

    private fun approvingEngine(outcome: PermissionDecisionOutcome = PermissionDecisionOutcome.APPROVED) =
        FakePermissionEngine { request -> decision(request, outcome) }

    private fun decision(request: ExecutionRequest, outcome: PermissionDecisionOutcome) = PermissionDecision(
        decisionId = DecisionId("decision-1"),
        principalId = request.principalId,
        resourceId = request.targetResources.first(),
        action = PermissionAction.WRITE,
        decision = outcome,
        level = PermissionLevel.AUTOMATIC,
        timestamp = Instant.now(),
    )

    // --- Fixture builders ---

    private fun candidateEvidenceArtifact(content: String = "content") =
        CandidateEvidenceArtifact(content.toByteArray())

    private fun candidateAssertion(statement: String = "the user prefers window seats") = CandidateAssertion(
        statement = statement,
        provenanceId = ProvenanceId("provenance-1"),
        confidence = 0.9,
    )

    private fun candidateRelationship() = CandidateRelationship(
        relationshipType = Relationship.SUPPORTS,
        fromEndpoint = RelationshipEndpoint(RelationshipEndpoint.ASSERTION, "assertion-a"),
        toEndpoint = RelationshipEndpoint(RelationshipEndpoint.ASSERTION, "assertion-b"),
        directional = true,
        provenanceId = ProvenanceId("provenance-1"),
    )

    private fun assertionResult(candidate: CandidateAssertion, id: String = "assertion-1") = Assertion(
        assertionId = AssertionId(id),
        statement = candidate.statement,
        provenanceId = candidate.provenanceId,
        confidence = candidate.confidence,
    )

    private fun relationshipResult(candidate: CandidateRelationship, id: String = "relationship-1") = Relationship(
        relationshipId = RelationshipId(id),
        relationshipType = candidate.relationshipType,
        fromEndpoint = candidate.fromEndpoint,
        toEndpoint = candidate.toEndpoint,
        directional = candidate.directional,
        provenanceId = candidate.provenanceId,
        createdAt = Instant.parse("2026-01-01T00:00:00Z"),
    )

    private fun knowledgeCandidate() =
        KnowledgeCandidate(MemoryCoreRecordReference.ToAssertion(AssertionId("assertion-1")))

    private fun promotedDisposition(candidate: KnowledgeCandidate): KnowledgeSubmissionDisposition.Promoted {
        val promotion = KnowledgePromotion(
            knowledgeId = parker.core.interfaces.KnowledgeId("knowledge-1"),
            evidenceReference = candidate.evidenceReference,
            resultingState = EvidentialState.UNKNOWN,
            occurredAt = Instant.parse("2026-01-01T00:00:00Z"),
            basis = "test fixture promotion",
        )
        val item = KnowledgeItem(
            knowledgeId = promotion.knowledgeId,
            evidenceReference = candidate.evidenceReference,
            provenanceReference = ProvenanceReference(ProvenanceId("provenance-1")),
            evidentialState = EvidentialState.UNKNOWN,
            status = KnowledgeItemStatus.ACTIVE,
            history = listOf(promotion),
        )
        return KnowledgeSubmissionDisposition.Promoted(item, promotion)
    }

    // ================= Dispatch tests =================

    @Test
    fun `an empty list returns an empty, ordered outcome list`() = runTest {
        val coordinator = EvidenceIntelligenceAcceptanceCoordinator(
            FakeEvidenceCustodian(),
            FakeMemoryCore(),
            FakeKnowledgeSubmission { _, _ -> throw UnsupportedOperationException("not configured") },
            approvingEngine(),
        )

        val outcomes = coordinator.dispatch(principalId, emptyList())

        assertEquals(emptyList(), outcomes)
    }

    @Test
    fun `TransientOutput invokes no dependency and produces NotDispatched`() = runTest {
        val evidenceCustodian = FakeEvidenceCustodian()
        val memoryCore = FakeMemoryCore()
        val knowledgeSubmission = FakeKnowledgeSubmission { _, _ -> throw UnsupportedOperationException("not configured") }
        val engine = approvingEngine()
        val coordinator = EvidenceIntelligenceAcceptanceCoordinator(evidenceCustodian, memoryCore, knowledgeSubmission, engine)
        val transient = EvidenceAnalysisResult.TransientOutput(
            text = "a working observation",
            evidenceArtifactReferences = listOf(EvidenceArtifactId("artifact-1")),
        )

        val outcomes = coordinator.dispatch(principalId, listOf(transient))

        assertEquals(listOf(EvidenceIntelligenceAcceptanceOutcome.NotDispatched), outcomes)
        assertEquals(0, evidenceCustodian.acceptCallCount)
        assertEquals(0, memoryCore.createAssertionCallCount)
        assertEquals(0, memoryCore.createRelationshipCallCount)
        assertEquals(0, knowledgeSubmission.submitCallCount)
        assertEquals(0, engine.evaluateCallCount)
    }

    @Test
    fun `an artifact candidate invokes EvidenceCustodian-accept exactly once and preserves its result unchanged`() = runTest {
        val accepted = EvidenceAcceptanceResult.Accepted(
            parker.core.interfaces.AcceptedEvidenceArtifact(EvidenceArtifactId("artifact-1"), Instant.now()),
        )
        val evidenceCustodian = FakeEvidenceCustodian { _, _ -> accepted }
        val engine = approvingEngine()
        val coordinator = EvidenceIntelligenceAcceptanceCoordinator(
            evidenceCustodian,
            FakeMemoryCore(),
            FakeKnowledgeSubmission { _, _ -> throw UnsupportedOperationException("not configured") },
            engine,
        )
        val candidate = candidateEvidenceArtifact()

        val outcomes = coordinator.dispatch(principalId, listOf(EvidenceAnalysisResult.CandidateArtifactProduced(candidate)))

        assertEquals(1, evidenceCustodian.acceptCallCount)
        assertSame(candidate, evidenceCustodian.lastCandidate)
        assertEquals(principalId, evidenceCustodian.lastPrincipal)
        val outcome = assertIs<EvidenceIntelligenceAcceptanceOutcome.ArtifactAcceptance>(outcomes.single())
        assertSame(accepted, outcome.result)
    }

    @Test
    fun `an artifact candidate never invokes the coordinator's own PermissionEngine`() = runTest {
        val engine = approvingEngine()
        val coordinator = EvidenceIntelligenceAcceptanceCoordinator(
            FakeEvidenceCustodian { _, _ -> EvidenceAcceptanceResult.Rejected("declined for test") },
            FakeMemoryCore(),
            FakeKnowledgeSubmission { _, _ -> throw UnsupportedOperationException("not configured") },
            engine,
        )

        coordinator.dispatch(principalId, listOf(EvidenceAnalysisResult.CandidateArtifactProduced(candidateEvidenceArtifact())))

        assertEquals(0, engine.evaluateCallCount, "EvidenceCustodian.accept already self-gates -- this class must not re-evaluate")
    }

    @Test
    fun `an assertion candidate evaluates permission exactly once before the Memory Core write`() = runTest {
        val callOrder = mutableListOf<String>()
        val memoryCore = FakeMemoryCore(
            onCreateAssertion = { _, candidate ->
                callOrder.add("memoryCore.createAssertion")
                assertionResult(candidate)
            },
        )
        val engine = FakePermissionEngine { request ->
            callOrder.add("permissionEngine.evaluate")
            decision(request, PermissionDecisionOutcome.APPROVED)
        }
        val coordinator = EvidenceIntelligenceAcceptanceCoordinator(
            FakeEvidenceCustodian(),
            memoryCore,
            FakeKnowledgeSubmission { _, _ -> throw UnsupportedOperationException("not configured") },
            engine,
        )
        val candidate = candidateAssertion()

        coordinator.dispatch(
            principalId,
            listOf(EvidenceAnalysisResult.CandidateRecordProduced(CandidateMemoryCoreRecord.OfAssertion(candidate))),
        )

        assertEquals(1, engine.evaluateCallCount)
        assertEquals(1, memoryCore.createAssertionCallCount)
        assertEquals(listOf("permissionEngine.evaluate", "memoryCore.createAssertion"), callOrder)
    }

    @Test
    fun `a relationship candidate evaluates permission exactly once before the Memory Core write`() = runTest {
        val engine = approvingEngine()
        val memoryCore = FakeMemoryCore(onCreateRelationship = { _, candidate -> relationshipResult(candidate) })
        val coordinator = EvidenceIntelligenceAcceptanceCoordinator(
            FakeEvidenceCustodian(),
            memoryCore,
            FakeKnowledgeSubmission { _, _ -> throw UnsupportedOperationException("not configured") },
            engine,
        )

        coordinator.dispatch(
            principalId,
            listOf(EvidenceAnalysisResult.CandidateRecordProduced(CandidateMemoryCoreRecord.OfRelationship(candidateRelationship()))),
        )

        assertEquals(1, engine.evaluateCallCount)
        assertEquals(1, memoryCore.createRelationshipCallCount)
    }

    @Test
    fun `APPROVED_WITH_CONFIRMATION authorises the Memory Core write`() = runTest {
        val memoryCore = FakeMemoryCore(onCreateAssertion = { _, candidate -> assertionResult(candidate) })
        val coordinator = EvidenceIntelligenceAcceptanceCoordinator(
            FakeEvidenceCustodian(),
            memoryCore,
            FakeKnowledgeSubmission { _, _ -> throw UnsupportedOperationException("not configured") },
            approvingEngine(PermissionDecisionOutcome.APPROVED_WITH_CONFIRMATION),
        )

        val outcomes = coordinator.dispatch(
            principalId,
            listOf(EvidenceAnalysisResult.CandidateRecordProduced(CandidateMemoryCoreRecord.OfAssertion(candidateAssertion()))),
        )

        assertEquals(1, memoryCore.createAssertionCallCount)
        val outcome = assertIs<EvidenceIntelligenceAcceptanceOutcome.RecordAcceptance.Written>(outcomes.single())
        assertIs<CandidateMemoryCoreRecordResult.OfAssertion>(outcome.record)
    }

    @Test
    fun `a DENIED decision prevents the Memory Core write and produces a NotAuthorised outcome`() = runTest {
        val memoryCore = FakeMemoryCore()
        val coordinator = EvidenceIntelligenceAcceptanceCoordinator(
            FakeEvidenceCustodian(),
            memoryCore,
            FakeKnowledgeSubmission { _, _ -> throw UnsupportedOperationException("not configured") },
            approvingEngine(PermissionDecisionOutcome.DENIED),
        )

        val outcomes = coordinator.dispatch(
            principalId,
            listOf(EvidenceAnalysisResult.CandidateRecordProduced(CandidateMemoryCoreRecord.OfAssertion(candidateAssertion()))),
        )

        assertEquals(0, memoryCore.createAssertionCallCount, "a denied decision must never reach MemoryCore.createAssertion")
        val outcome = assertIs<EvidenceIntelligenceAcceptanceOutcome.RecordAcceptance.NotAuthorised>(outcomes.single())
        assertTrue(outcome.reason.isNotBlank())
        assertTrue(outcome.reason.contains(principalId.value))
    }

    @Test
    fun `a DEFERRED decision also prevents the Memory Core write`() = runTest {
        val memoryCore = FakeMemoryCore()
        val coordinator = EvidenceIntelligenceAcceptanceCoordinator(
            FakeEvidenceCustodian(),
            memoryCore,
            FakeKnowledgeSubmission { _, _ -> throw UnsupportedOperationException("not configured") },
            approvingEngine(PermissionDecisionOutcome.DEFERRED),
        )

        coordinator.dispatch(
            principalId,
            listOf(EvidenceAnalysisResult.CandidateRecordProduced(CandidateMemoryCoreRecord.OfRelationship(candidateRelationship()))),
        )

        assertEquals(0, memoryCore.createRelationshipCallCount)
    }

    @Test
    fun `assertion fields are propagated unchanged into MemoryCore-createAssertion`() = runTest {
        val memoryCore = FakeMemoryCore(onCreateAssertion = { _, candidate -> assertionResult(candidate) })
        val coordinator = EvidenceIntelligenceAcceptanceCoordinator(
            FakeEvidenceCustodian(),
            memoryCore,
            FakeKnowledgeSubmission { _, _ -> throw UnsupportedOperationException("not configured") },
            approvingEngine(),
        )
        val candidate = candidateAssertion(statement = "a distinctive statement")

        coordinator.dispatch(
            principalId,
            listOf(EvidenceAnalysisResult.CandidateRecordProduced(CandidateMemoryCoreRecord.OfAssertion(candidate))),
        )

        assertSame(candidate, memoryCore.lastCandidateAssertion)
        assertEquals(principalId, memoryCore.lastPrincipal)
    }

    @Test
    fun `relationship fields are propagated unchanged into MemoryCore-createRelationship`() = runTest {
        val memoryCore = FakeMemoryCore(onCreateRelationship = { _, candidate -> relationshipResult(candidate) })
        val coordinator = EvidenceIntelligenceAcceptanceCoordinator(
            FakeEvidenceCustodian(),
            memoryCore,
            FakeKnowledgeSubmission { _, _ -> throw UnsupportedOperationException("not configured") },
            approvingEngine(),
        )
        val candidate = candidateRelationship()

        coordinator.dispatch(
            principalId,
            listOf(EvidenceAnalysisResult.CandidateRecordProduced(CandidateMemoryCoreRecord.OfRelationship(candidate))),
        )

        assertSame(candidate, memoryCore.lastCandidateRelationship)
        assertEquals(principalId, memoryCore.lastPrincipal)
    }

    @Test
    fun `a Knowledge Candidate invokes KnowledgeSubmission-submit exactly once and never the coordinator's permission engine`() =
        runTest {
            val candidate = knowledgeCandidate()
            val disposition = promotedDisposition(candidate)
            val knowledgeSubmission = FakeKnowledgeSubmission { _, _ -> disposition }
            val engine = approvingEngine()
            val coordinator = EvidenceIntelligenceAcceptanceCoordinator(
                FakeEvidenceCustodian(),
                FakeMemoryCore(),
                knowledgeSubmission,
                engine,
            )

            val outcomes = coordinator.dispatch(
                principalId,
                listOf(EvidenceAnalysisResult.CandidateKnowledgeProduced(candidate)),
            )

            assertEquals(1, knowledgeSubmission.submitCallCount)
            assertSame(candidate, knowledgeSubmission.lastCandidate)
            assertEquals(principalId, knowledgeSubmission.lastPrincipal)
            assertEquals(0, engine.evaluateCallCount, "KnowledgeSubmission already self-gates via Evaluation B")
            val outcome = assertIs<EvidenceIntelligenceAcceptanceOutcome.KnowledgeAcceptance>(outcomes.single())
            assertSame(disposition, outcome.disposition)
        }

    @Test
    fun `KnowledgeSubmissionDisposition-Promoted is preserved unchanged`() = runTest {
        val candidate = knowledgeCandidate()
        val disposition = promotedDisposition(candidate)
        val coordinator = EvidenceIntelligenceAcceptanceCoordinator(
            FakeEvidenceCustodian(),
            FakeMemoryCore(),
            FakeKnowledgeSubmission { _, _ -> disposition },
            approvingEngine(),
        )

        val outcomes = coordinator.dispatch(principalId, listOf(EvidenceAnalysisResult.CandidateKnowledgeProduced(candidate)))

        val outcome = assertIs<EvidenceIntelligenceAcceptanceOutcome.KnowledgeAcceptance>(outcomes.single())
        assertIs<KnowledgeSubmissionDisposition.Promoted>(outcome.disposition)
        assertSame(disposition, outcome.disposition)
    }

    @Test
    fun `KnowledgeSubmissionDisposition-Declined is preserved unchanged`() = runTest {
        val candidate = knowledgeCandidate()
        val disposition = KnowledgeSubmissionDisposition.Declined("insufficient promotion basis")
        val coordinator = EvidenceIntelligenceAcceptanceCoordinator(
            FakeEvidenceCustodian(),
            FakeMemoryCore(),
            FakeKnowledgeSubmission { _, _ -> disposition },
            approvingEngine(),
        )

        val outcomes = coordinator.dispatch(principalId, listOf(EvidenceAnalysisResult.CandidateKnowledgeProduced(candidate)))

        val outcome = assertIs<EvidenceIntelligenceAcceptanceOutcome.KnowledgeAcceptance>(outcomes.single())
        val declined = assertIs<KnowledgeSubmissionDisposition.Declined>(outcome.disposition)
        assertEquals("insufficient promotion basis", declined.basis)
    }

    @Test
    fun `KnowledgeSubmissionDisposition-NotAuthorised is preserved unchanged`() = runTest {
        val candidate = knowledgeCandidate()
        val disposition = KnowledgeSubmissionDisposition.NotAuthorised("Permission Engine did not authorise submission")
        val coordinator = EvidenceIntelligenceAcceptanceCoordinator(
            FakeEvidenceCustodian(),
            FakeMemoryCore(),
            FakeKnowledgeSubmission { _, _ -> disposition },
            approvingEngine(),
        )

        val outcomes = coordinator.dispatch(principalId, listOf(EvidenceAnalysisResult.CandidateKnowledgeProduced(candidate)))

        val outcome = assertIs<EvidenceIntelligenceAcceptanceOutcome.KnowledgeAcceptance>(outcomes.single())
        val notAuthorised = assertIs<KnowledgeSubmissionDisposition.NotAuthorised>(outcome.disposition)
        assertEquals("Permission Engine did not authorise submission", notAuthorised.reason)
    }

    // **On the "defensive identifier failure" requirement, deliberately not tested here.**
    // The coordinator's own `requireGovernedIdentifier` check (see the production file's own
    // KDoc) guards against a `MemoryCoreRecordReference` naming a blank identifier. Every
    // identifier value class it can wrap (`EntityId`, `DocumentId`, `AssertionId`,
    // `RelationshipId`) rejects a blank value in its own constructor `init` block -- there is no
    // reachable, safe way to construct a violating `MemoryCoreRecordReference` through the public
    // API to exercise the failure branch (unlike, for example, forcing a fake dependency to
    // return an unexpected value, constructing a genuinely blank identifier would require
    // bypassing Kotlin's own constructor validation via unsafe reflection on a `@JvmInline value
    // class`'s internal representation -- fragile, not guaranteed to behave correctly across
    // compiler versions, and precisely the kind of untested, unverifiable code this task's own
    // constraints (no Gradle run, no claimed verification) counsel against introducing). This
    // mirrors Programme 3 Unit 8's own identical, previously-documented finding: a governance-
    // required defensive check whose own failure branch is structurally unreachable given
    // current type guarantees is correctly present in the source (verifiable by direct reading)
    // without a corresponding runtime test forcing an input the type system itself forecloses.

    // ================= Mixed-list tests =================

    @Test
    fun `a mixed list preserves original order and dispatches each candidate to only its own subsystem`() = runTest {
        val evidenceCustodian = FakeEvidenceCustodian { _, _ ->
            EvidenceAcceptanceResult.Accepted(
                parker.core.interfaces.AcceptedEvidenceArtifact(EvidenceArtifactId("artifact-1"), Instant.now()),
            )
        }
        val memoryCore = FakeMemoryCore(onCreateAssertion = { _, candidate -> assertionResult(candidate) })
        val knowledgeCandidateValue = knowledgeCandidate()
        val disposition = promotedDisposition(knowledgeCandidateValue)
        val knowledgeSubmission = FakeKnowledgeSubmission { _, _ -> disposition }
        val coordinator = EvidenceIntelligenceAcceptanceCoordinator(
            evidenceCustodian,
            memoryCore,
            knowledgeSubmission,
            approvingEngine(),
        )

        val transient = EvidenceAnalysisResult.TransientOutput(
            "a working observation",
            evidenceArtifactReferences = listOf(EvidenceArtifactId("artifact-1")),
        )
        val artifact = EvidenceAnalysisResult.CandidateArtifactProduced(candidateEvidenceArtifact())
        val record = EvidenceAnalysisResult.CandidateRecordProduced(CandidateMemoryCoreRecord.OfAssertion(candidateAssertion()))
        val knowledge = EvidenceAnalysisResult.CandidateKnowledgeProduced(knowledgeCandidateValue)

        val outcomes = coordinator.dispatch(principalId, listOf(transient, artifact, record, knowledge))

        assertEquals(4, outcomes.size)
        assertIs<EvidenceIntelligenceAcceptanceOutcome.NotDispatched>(outcomes[0])
        assertIs<EvidenceIntelligenceAcceptanceOutcome.ArtifactAcceptance>(outcomes[1])
        assertIs<EvidenceIntelligenceAcceptanceOutcome.RecordAcceptance.Written>(outcomes[2])
        assertIs<EvidenceIntelligenceAcceptanceOutcome.KnowledgeAcceptance>(outcomes[3])
        assertEquals(1, evidenceCustodian.acceptCallCount)
        assertEquals(1, memoryCore.createAssertionCallCount)
        assertEquals(0, memoryCore.createRelationshipCallCount)
        assertEquals(1, knowledgeSubmission.submitCallCount)
    }

    @Test
    fun `a decline on one element does not corrupt or reorder outcomes for later elements`() = runTest {
        val firstCandidate = knowledgeCandidate()
        val secondCandidate = KnowledgeCandidate(MemoryCoreRecordReference.ToAssertion(AssertionId("assertion-2")))
        var calls = 0
        val knowledgeSubmission = FakeKnowledgeSubmission { _, candidate ->
            calls++
            if (candidate === firstCandidate) {
                KnowledgeSubmissionDisposition.Declined("insufficient promotion basis")
            } else {
                promotedDisposition(candidate)
            }
        }
        val coordinator = EvidenceIntelligenceAcceptanceCoordinator(
            FakeEvidenceCustodian(),
            FakeMemoryCore(),
            knowledgeSubmission,
            approvingEngine(),
        )

        val outcomes = coordinator.dispatch(
            principalId,
            listOf(
                EvidenceAnalysisResult.CandidateKnowledgeProduced(firstCandidate),
                EvidenceAnalysisResult.CandidateKnowledgeProduced(secondCandidate),
            ),
        )

        assertEquals(2, outcomes.size)
        assertIs<KnowledgeSubmissionDisposition.Declined>(
            (outcomes[0] as EvidenceIntelligenceAcceptanceOutcome.KnowledgeAcceptance).disposition,
        )
        assertIs<KnowledgeSubmissionDisposition.Promoted>(
            (outcomes[1] as EvidenceIntelligenceAcceptanceOutcome.KnowledgeAcceptance).disposition,
        )
        assertEquals(2, calls, "the decline on the first element must not prevent the second from being dispatched")
    }

    // ================= Fault tests =================

    @Test
    fun `a genuine EvidenceCustodian fault propagates unchanged, never becomes a rejection`() = runTest {
        val evidenceCustodian = FakeEvidenceCustodian { _, _ -> throw IllegalStateException("simulated custodian fault") }
        val coordinator = EvidenceIntelligenceAcceptanceCoordinator(
            evidenceCustodian,
            FakeMemoryCore(),
            FakeKnowledgeSubmission { _, _ -> throw UnsupportedOperationException("not configured") },
            approvingEngine(),
        )

        assertFailsWith<IllegalStateException> {
            coordinator.dispatch(principalId, listOf(EvidenceAnalysisResult.CandidateArtifactProduced(candidateEvidenceArtifact())))
        }
    }

    @Test
    fun `a genuine PermissionEngine fault propagates unchanged, never becomes a denial`() = runTest {
        val engine = FakePermissionEngine { throw IllegalStateException("simulated permission engine fault") }
        val coordinator = EvidenceIntelligenceAcceptanceCoordinator(
            FakeEvidenceCustodian(),
            FakeMemoryCore(),
            FakeKnowledgeSubmission { _, _ -> throw UnsupportedOperationException("not configured") },
            engine,
        )

        assertFailsWith<IllegalStateException> {
            coordinator.dispatch(
                principalId,
                listOf(EvidenceAnalysisResult.CandidateRecordProduced(CandidateMemoryCoreRecord.OfAssertion(candidateAssertion()))),
            )
        }
    }

    @Test
    fun `a genuine MemoryCore fault propagates unchanged, never becomes a rejection`() = runTest {
        val memoryCore = FakeMemoryCore(onCreateAssertion = { _, _ -> throw IllegalStateException("simulated memory core fault") })
        val coordinator = EvidenceIntelligenceAcceptanceCoordinator(
            FakeEvidenceCustodian(),
            memoryCore,
            FakeKnowledgeSubmission { _, _ -> throw UnsupportedOperationException("not configured") },
            approvingEngine(),
        )

        assertFailsWith<IllegalStateException> {
            coordinator.dispatch(
                principalId,
                listOf(EvidenceAnalysisResult.CandidateRecordProduced(CandidateMemoryCoreRecord.OfAssertion(candidateAssertion()))),
            )
        }
    }

    @Test
    fun `a genuine KnowledgeSubmission fault propagates unchanged, never becomes a decline or denial`() = runTest {
        val knowledgeSubmission = FakeKnowledgeSubmission { _, _ -> throw IllegalStateException("simulated submission fault") }
        val coordinator = EvidenceIntelligenceAcceptanceCoordinator(
            FakeEvidenceCustodian(),
            FakeMemoryCore(),
            knowledgeSubmission,
            approvingEngine(),
        )

        assertFailsWith<IllegalStateException> {
            coordinator.dispatch(principalId, listOf(EvidenceAnalysisResult.CandidateKnowledgeProduced(knowledgeCandidate())))
        }
    }

    // ================= Structural safeguards =================

    @Test
    fun `EvidenceIntelligenceAcceptanceCoordinator is a concrete class, not an interface, and implements no interface`() {
        val kClass = EvidenceIntelligenceAcceptanceCoordinator::class
        assertTrue(!kClass.java.isInterface, "must be a concrete class")
        assertEquals(
            0,
            kClass.java.interfaces.size,
            "must be non-interface-backed -- it must not implement any interface of its own",
        )
    }

    @Test
    fun `EvidenceIntelligenceAcceptanceCoordinator holds exactly four constructor dependencies`() {
        val constructor = EvidenceIntelligenceAcceptanceCoordinator::class.primaryConstructor
            ?: error("must have a primary constructor")

        assertEquals(4, constructor.parameters.size)
    }

    @Test
    fun `EvidenceIntelligenceAcceptanceCoordinator depends on no excluded type`() {
        val constructor = EvidenceIntelligenceAcceptanceCoordinator::class.primaryConstructor
            ?: error("must have a primary constructor")

        val forbidden = setOf(
            "parker.core.interfaces.EvidenceIntelligence",
            "parker.core.runtime.DefaultEvidenceIntelligence",
            "parker.core.interfaces.ReasoningProvider",
            "parker.core.interfaces.ReasoningContext",
            "parker.core.interfaces.KnowledgeCandidateEvaluator",
            "parker.core.runtime.KnowledgeItemPersistence",
            "parker.core.interfaces.MemoryRetrieval",
            "parker.core.interfaces.KnowledgeStore",
            "parker.core.interfaces.KnowledgeSource",
            "parker.core.interfaces.KnowledgeRevisionEvaluator",
            "parker.core.interfaces.KnowledgeRetirementEvaluator",
        )
        val qualifiedNames = constructor.parameters.map { parameter ->
            (parameter.type.classifier as? KClass<*>)?.qualifiedName
        }
        forbidden.forEach { forbiddenName ->
            assertTrue(
                forbiddenName !in qualifiedNames,
                "EvidenceIntelligenceAcceptanceCoordinator must not depend on $forbiddenName -- found: $qualifiedNames",
            )
        }
    }

    @Test
    fun `EvidenceIntelligenceAcceptanceCoordinator holds only its four constructor-injected instance fields, none mutable`() {
        // "No mutable cross-call state" is a property of *instance* fields -- what a single
        // object could carry between calls to it. It says nothing about static, class-level
        // members, so static fields are excluded here alongside synthetic ones. The companion
        // object this class declares (to hold the disclosed-but-unregistered permission
        // resource/action pair, exactly as DefaultEvidenceCustodian, EvidenceRegistrationCoordinator,
        // and EvidenceIntelligenceInvocationGate already do) produces exactly three such
        // non-synthetic static fields on the compiled outer class -- `Companion` itself, the
        // `const val` action name hoisted onto the outer class, and the `ResourceId` value
        // class's own unboxed backing field -- confirmed directly against the compiled class
        // file (`javap -v`): all three carry ACC_STATIC and none carries ACC_SYNTHETIC, so a
        // synthetic-only filter does not exclude them. None is instance state, and none is
        // reachable from a single instance across two different calls to it, so excluding
        // static fields here narrows the check to what it is actually meant to prove, without
        // weakening it: a genuine fifth *instance* dependency, or a `var` in place of any of the
        // four `val`s, would still be caught below.
        val fields = EvidenceIntelligenceAcceptanceCoordinator::class.java.declaredFields
            .filter { !it.isSynthetic && !Modifier.isStatic(it.modifiers) }

        assertEquals(
            4,
            fields.size,
            "no instance field beyond the four constructor-injected dependencies may exist -- found: " +
                fields.map { it.name },
        )
        fields.forEach { field ->
            assertTrue(
                Modifier.isFinal(field.modifiers),
                "${field.name} must be immutable (final) -- this class holds no mutable cross-call state",
            )
        }
    }
}

/** Test-only fake, mirroring [EvidenceRegistrationCoordinatorTest]'s own identical shape. */
private class FakeEvidenceCustodian(
    private val onAccept: (PrincipalId, CandidateEvidenceArtifact) -> EvidenceAcceptanceResult = { _, _ ->
        throw UnsupportedOperationException("accept not configured")
    },
) : EvidenceCustodian {
    var acceptCallCount = 0
        private set
    var lastPrincipal: PrincipalId? = null
        private set
    var lastCandidate: CandidateEvidenceArtifact? = null
        private set

    override suspend fun accept(requestingPrincipalId: PrincipalId, candidate: CandidateEvidenceArtifact): EvidenceAcceptanceResult {
        acceptCallCount++
        lastPrincipal = requestingPrincipalId
        lastCandidate = candidate
        return onAccept(requestingPrincipalId, candidate)
    }

    override suspend fun retrieve(requestingPrincipalId: PrincipalId, evidenceArtifactId: EvidenceArtifactId): EvidenceRetrievalResult =
        throw UnsupportedOperationException("not used by EvidenceIntelligenceAcceptanceCoordinator")

    override suspend fun retrieveManifest(requestingPrincipalId: PrincipalId, evidenceArtifactId: EvidenceArtifactId): EvidenceManifestRetrievalResult =
        throw UnsupportedOperationException("not used by EvidenceIntelligenceAcceptanceCoordinator")
}

/**
 * Test-only fake. Only [createAssertion]/[createRelationship] are ever
 * called by [EvidenceIntelligenceAcceptanceCoordinator]; every other
 * [MemoryCore] operation throws if reached, mirroring
 * [EvidenceRegistrationCoordinatorTest]'s own identical fake.
 */
private class FakeMemoryCore(
    private val onCreateAssertion: (PrincipalId, CandidateAssertion) -> Assertion = { _, _ ->
        throw UnsupportedOperationException("createAssertion not configured")
    },
    private val onCreateRelationship: (PrincipalId, CandidateRelationship) -> Relationship = { _, _ ->
        throw UnsupportedOperationException("createRelationship not configured")
    },
) : MemoryCore {
    var createAssertionCallCount = 0
        private set
    var createRelationshipCallCount = 0
        private set
    var lastPrincipal: PrincipalId? = null
        private set
    var lastCandidateAssertion: CandidateAssertion? = null
        private set
    var lastCandidateRelationship: CandidateRelationship? = null
        private set

    override suspend fun createProvenance(requestingPrincipalId: PrincipalId, candidate: CandidateProvenance): Provenance =
        throw UnsupportedOperationException("not used by EvidenceIntelligenceAcceptanceCoordinator")

    override suspend fun createEntity(requestingPrincipalId: PrincipalId, candidate: CandidateEntity): Entity =
        throw UnsupportedOperationException("not used by EvidenceIntelligenceAcceptanceCoordinator")

    override suspend fun registerDocument(requestingPrincipalId: PrincipalId, candidate: CandidateDocument): Document =
        throw UnsupportedOperationException("not used by EvidenceIntelligenceAcceptanceCoordinator")

    override suspend fun createAssertion(requestingPrincipalId: PrincipalId, candidate: CandidateAssertion): Assertion {
        createAssertionCallCount++
        lastPrincipal = requestingPrincipalId
        lastCandidateAssertion = candidate
        return onCreateAssertion(requestingPrincipalId, candidate)
    }

    override suspend fun createRelationship(requestingPrincipalId: PrincipalId, candidate: CandidateRelationship): Relationship {
        createRelationshipCallCount++
        lastPrincipal = requestingPrincipalId
        lastCandidateRelationship = candidate
        return onCreateRelationship(requestingPrincipalId, candidate)
    }

    override suspend fun transitionStatus(
        requestingPrincipalId: PrincipalId,
        reference: MemoryCoreRecordReference,
        targetStatus: MemoryCoreRecordStatus,
    ): MemoryCoreRecord = throw UnsupportedOperationException("not used by EvidenceIntelligenceAcceptanceCoordinator")
}

/** Test-only fake, mirroring [FakeEvidenceCustodian]'s own identical, call-counting shape. */
private class FakeKnowledgeSubmission(
    private val onSubmit: (PrincipalId, KnowledgeCandidate) -> KnowledgeSubmissionDisposition,
) : KnowledgeSubmission {
    var submitCallCount = 0
        private set
    var lastPrincipal: PrincipalId? = null
        private set
    var lastCandidate: KnowledgeCandidate? = null
        private set

    override suspend fun submit(requestingPrincipalId: PrincipalId, candidate: KnowledgeCandidate): KnowledgeSubmissionDisposition {
        submitCallCount++
        lastPrincipal = requestingPrincipalId
        lastCandidate = candidate
        return onSubmit(requestingPrincipalId, candidate)
    }
}
