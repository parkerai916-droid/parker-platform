package parker.core.runtime

import java.time.Instant
import kotlin.reflect.full.primaryConstructor
import kotlinx.coroutines.test.runTest
import parker.core.interfaces.CandidateAssertion
import parker.core.interfaces.CandidateProvenance
import parker.core.interfaces.ContentNature
import parker.core.interfaces.DecisionId
import parker.core.interfaces.ExecutionRequest
import parker.core.interfaces.KnowledgeCandidate
import parker.core.interfaces.KnowledgeCandidateEvaluation
import parker.core.interfaces.KnowledgeCandidateEvaluator
import parker.core.interfaces.KnowledgeItem
import parker.core.interfaces.KnowledgeSubmissionDisposition
import parker.core.interfaces.MemoryCoreRecordReference
import parker.core.interfaces.PermissionAction
import parker.core.interfaces.PermissionDecision
import parker.core.interfaces.PermissionDecisionOutcome
import parker.core.interfaces.PermissionEngine
import parker.core.interfaces.PermissionLevel
import parker.core.interfaces.PrincipalId
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Programme 3, Knowledge Memory, Implementation Unit 8 (Knowledge
 * Submission). Behavioural and structural tests of
 * [DefaultKnowledgeSubmission] -- the orchestration between
 * [PermissionEngine] (Evaluation B), [KnowledgeCandidateEvaluator] (Unit
 * 6, unchanged), and [KnowledgeItemPersistence] (Unit 8's own new,
 * internal seam). Mirrors [DefaultEvidenceCustodianTest]'s own scope and
 * style exactly: behavioural coverage of the orchestration this class
 * itself adds, plus the structural safeguards
 * `docs/governance/PROGRAMME_3_UNIT_8_SCOPE_LOCK_CLARIFICATION.md`
 * requires be demonstrable, not merely asserted in prose.
 *
 * Uses a real [InMemoryMemoryCore] and a real [DefaultKnowledgeCandidateEvaluator]
 * wherever a genuine `Promote`/`Reject` outcome is needed -- already-tested
 * production code, not new test infrastructure, mirroring
 * [DefaultKnowledgeCandidateEvaluatorTest]'s own identical choice. A
 * [FakeKnowledgeCandidateEvaluator] is used only where a call count or a
 * specific, arbitrary outcome (unreachable through the real evaluator's
 * own promotion gate) is the point of the test.
 */
class DefaultKnowledgeSubmissionTest {

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

    private suspend fun promotableCandidate(core: InMemoryMemoryCore): KnowledgeCandidate {
        val writer = PrincipalId("test.writer")
        val provenance = core.createProvenance(
            writer,
            CandidateProvenance(
                sourceIdentifier = "test-source",
                sourceType = "test-harness",
                acquisitionTime = Instant.parse("2026-01-01T00:00:00Z"),
                contentNature = ContentNature.ORIGINAL,
            ),
        )
        val assertion = core.createAssertion(
            writer,
            CandidateAssertion(
                statement = "the user prefers window seats",
                provenanceId = provenance.provenanceId,
                confidence = 0.9,
            ),
        )
        return KnowledgeCandidate(
            MemoryCoreRecordReference.ToAssertion(assertion.assertionId),
            explicitlyRequested = true,
        )
    }

    private suspend fun undecidableCandidate(core: InMemoryMemoryCore): KnowledgeCandidate {
        val writer = PrincipalId("test.writer")
        val provenance = core.createProvenance(
            writer,
            CandidateProvenance(
                sourceIdentifier = "test-source",
                sourceType = "test-harness",
                acquisitionTime = Instant.parse("2026-01-01T00:00:00Z"),
                contentNature = ContentNature.ORIGINAL,
            ),
        )
        val assertion = core.createAssertion(
            writer,
            CandidateAssertion(statement = "an unsupported claim", provenanceId = provenance.provenanceId),
        )
        return KnowledgeCandidate(MemoryCoreRecordReference.ToAssertion(assertion.assertionId))
    }

    // --- Successful promotion ---

    @Test
    fun `an APPROVED decision followed by Promote persists the KnowledgeItem and returns Promoted`() = runTest {
        val core = InMemoryMemoryCore()
        val evaluator = DefaultKnowledgeCandidateEvaluator(core)
        val persistence = InMemoryKnowledgeItemPersistence()
        val submission = DefaultKnowledgeSubmission(evaluator, persistence, approvingEngine())

        val result = submission.submit(principalId, promotableCandidate(core))

        val promoted = assertIs<KnowledgeSubmissionDisposition.Promoted>(result)
        assertEquals(promoted.item, persistence.find(promoted.item.knowledgeId))
        assertEquals(listOf(promoted.promotion), promoted.item.history)
    }

    @Test
    fun `an APPROVED_WITH_CONFIRMATION decision is also treated as authorised`() = runTest {
        val core = InMemoryMemoryCore()
        val evaluator = DefaultKnowledgeCandidateEvaluator(core)
        val persistence = InMemoryKnowledgeItemPersistence()
        val submission = DefaultKnowledgeSubmission(
            evaluator,
            persistence,
            approvingEngine(PermissionDecisionOutcome.APPROVED_WITH_CONFIRMATION),
        )

        val result = submission.submit(principalId, promotableCandidate(core))

        assertIs<KnowledgeSubmissionDisposition.Promoted>(result)
    }

    // --- Substantive decline ---

    @Test
    fun `an APPROVED decision followed by Reject persists nothing and returns Declined`() = runTest {
        val core = InMemoryMemoryCore()
        val evaluator = DefaultKnowledgeCandidateEvaluator(core)
        val persistence = FakeKnowledgeItemPersistence()
        val submission = DefaultKnowledgeSubmission(evaluator, persistence, approvingEngine())

        val result = submission.submit(principalId, undecidableCandidate(core))

        val declined = assertIs<KnowledgeSubmissionDisposition.Declined>(result)
        assertTrue(declined.basis.isNotBlank())
        assertEquals(0, persistence.storeCallCount, "a declined promotion must never reach persistence.store")
    }

    // --- Permission denial ---

    @Test
    fun `a DENIED decision is NotAuthorised, and the evaluator is never invoked`() = runTest {
        val evaluator = FakeKnowledgeCandidateEvaluator(KnowledgeCandidateEvaluation.Reject("unused"))
        val persistence = FakeKnowledgeItemPersistence()
        val submission = DefaultKnowledgeSubmission(
            evaluator,
            persistence,
            approvingEngine(PermissionDecisionOutcome.DENIED),
        )

        val result = submission.submit(principalId, arbitraryCandidate())

        assertIs<KnowledgeSubmissionDisposition.NotAuthorised>(result)
        assertEquals(0, evaluator.evaluateCallCount, "a denied decision must never reach the evaluator")
        assertEquals(0, persistence.storeCallCount, "a denied decision must never reach persistence")
    }

    @Test
    fun `a DEFERRED decision is NotAuthorised, treated the same as denial`() = runTest {
        val evaluator = FakeKnowledgeCandidateEvaluator(KnowledgeCandidateEvaluation.Reject("unused"))
        val persistence = FakeKnowledgeItemPersistence()
        val submission = DefaultKnowledgeSubmission(
            evaluator,
            persistence,
            approvingEngine(PermissionDecisionOutcome.DEFERRED),
        )

        val result = submission.submit(principalId, arbitraryCandidate())

        assertIs<KnowledgeSubmissionDisposition.NotAuthorised>(result)
        assertEquals(0, evaluator.evaluateCallCount)
        assertEquals(0, persistence.storeCallCount)
    }

    @Test
    fun `a NotAuthorised result carries a non-blank reason naming the requesting principal`() = runTest {
        val evaluator = FakeKnowledgeCandidateEvaluator(KnowledgeCandidateEvaluation.Reject("unused"))
        val persistence = FakeKnowledgeItemPersistence()
        val submission = DefaultKnowledgeSubmission(
            evaluator,
            persistence,
            approvingEngine(PermissionDecisionOutcome.DENIED),
        )

        val result = assertIs<KnowledgeSubmissionDisposition.NotAuthorised>(
            submission.submit(principalId, arbitraryCandidate()),
        )

        assertTrue(result.reason.isNotBlank())
        assertTrue(result.reason.contains(principalId.value), "reason should name the requesting principal")
    }

    // --- Call counts and ordering ---

    @Test
    fun `the evaluator is invoked exactly once when permission approves`() = runTest {
        val evaluator = FakeKnowledgeCandidateEvaluator(KnowledgeCandidateEvaluation.Reject("declined for test"))
        val persistence = FakeKnowledgeItemPersistence()
        val submission = DefaultKnowledgeSubmission(evaluator, persistence, approvingEngine())

        submission.submit(principalId, arbitraryCandidate())

        assertEquals(1, evaluator.evaluateCallCount)
        assertEquals(principalId, evaluator.lastRequestingPrincipalId)
    }

    @Test
    fun `two submitting principals are forwarded unchanged without reuse or substitution`() = runTest {
        val evaluator = FakeKnowledgeCandidateEvaluator(KnowledgeCandidateEvaluation.Reject("declined for test"))
        val submission = DefaultKnowledgeSubmission(evaluator, FakeKnowledgeItemPersistence(), approvingEngine())
        val first = PrincipalId("user.accountable-first")
        val second = PrincipalId("user.accountable-second")

        submission.submit(first, arbitraryCandidate())
        assertEquals(first, evaluator.lastRequestingPrincipalId)

        submission.submit(second, arbitraryCandidate())
        assertEquals(second, evaluator.lastRequestingPrincipalId)
        assertEquals(2, evaluator.evaluateCallCount)
    }

    @Test
    fun `submit requires an actual Permission Engine call -- it is not bypassed`() = runTest {
        val engine = approvingEngine()
        val evaluator = FakeKnowledgeCandidateEvaluator(KnowledgeCandidateEvaluation.Reject("declined for test"))
        val submission = DefaultKnowledgeSubmission(evaluator, FakeKnowledgeItemPersistence(), engine)

        submission.submit(principalId, arbitraryCandidate())

        assertEquals(1, engine.evaluateCallCount)
    }

    @Test
    fun `the permission gate occurs before the evaluator is invoked -- a denied request never reaches it`() = runTest {
        val callOrder = mutableListOf<String>()
        val evaluator = object : KnowledgeCandidateEvaluator {
            override fun evaluate(
                requestingPrincipalId: PrincipalId,
                candidate: KnowledgeCandidate,
            ): KnowledgeCandidateEvaluation {
                callOrder.add("evaluator.evaluate")
                return KnowledgeCandidateEvaluation.Reject("unused")
            }
        }
        val engine = FakePermissionEngine { request ->
            callOrder.add("permissionEngine.evaluate")
            decision(request, PermissionDecisionOutcome.DENIED)
        }
        val submission = DefaultKnowledgeSubmission(evaluator, FakeKnowledgeItemPersistence(), engine)

        submission.submit(principalId, arbitraryCandidate())

        assertEquals(listOf("permissionEngine.evaluate"), callOrder, "the evaluator must never be reached on denial")
    }

    @Test
    fun `the requesting principal is propagated unchanged into the ExecutionRequest Evaluation B evaluates`() = runTest {
        var capturedPrincipal: PrincipalId? = null
        val engine = FakePermissionEngine { request ->
            capturedPrincipal = request.principalId
            decision(request, PermissionDecisionOutcome.APPROVED)
        }
        val evaluator = FakeKnowledgeCandidateEvaluator(KnowledgeCandidateEvaluation.Reject("unused"))
        val submission = DefaultKnowledgeSubmission(evaluator, FakeKnowledgeItemPersistence(), engine)

        submission.submit(principalId, arbitraryCandidate())

        assertEquals(principalId, capturedPrincipal)
    }

    // --- Structural safeguards ---

    @Test
    fun `DefaultKnowledgeSubmission holds no dependency capable of reading Memory Core`() {
        val constructor = DefaultKnowledgeSubmission::class.primaryConstructor
            ?: error("DefaultKnowledgeSubmission must have a primary constructor")

        val forbidden = setOf(
            "parker.core.interfaces.MemoryRetrieval",
            "parker.core.interfaces.MemoryCore",
        )
        constructor.parameters.forEach { parameter ->
            val qualifiedName = (parameter.type.classifier as? kotlin.reflect.KClass<*>)?.qualifiedName
            assertTrue(
                qualifiedName !in forbidden,
                "DefaultKnowledgeSubmission must not depend on $qualifiedName -- " +
                    "resolving Memory Core content remains exclusively the evaluator's own, already-closed responsibility",
            )
        }
    }

    @Test
    fun `DefaultKnowledgeSubmission holds no dependency on the legacy KnowledgeStore path`() {
        val constructor = DefaultKnowledgeSubmission::class.primaryConstructor
            ?: error("DefaultKnowledgeSubmission must have a primary constructor")

        val forbidden = setOf(
            "parker.core.interfaces.KnowledgeStore",
            "parker.core.interfaces.KnowledgeSource",
            "parker.core.interfaces.CandidateKnowledge",
            "parker.core.interfaces.KnowledgePromotionPolicy",
        )
        constructor.parameters.forEach { parameter ->
            val qualifiedName = (parameter.type.classifier as? kotlin.reflect.KClass<*>)?.qualifiedName
            assertTrue(
                qualifiedName !in forbidden,
                "DefaultKnowledgeSubmission must not depend on the legacy path ($qualifiedName)",
            )
        }
    }

    @Test
    fun `DefaultKnowledgeSubmission holds no dependency on Evidence Intelligence or Evidence Custodian`() {
        val constructor = DefaultKnowledgeSubmission::class.primaryConstructor
            ?: error("DefaultKnowledgeSubmission must have a primary constructor")

        constructor.parameters.forEach { parameter ->
            val qualifiedName = (parameter.type.classifier as? kotlin.reflect.KClass<*>)?.qualifiedName ?: ""
            assertTrue(
                !qualifiedName.contains("Evidence", ignoreCase = false),
                "DefaultKnowledgeSubmission must not depend on any Evidence-family type -- found $qualifiedName",
            )
        }
    }

    @Test
    fun `DefaultKnowledgeSubmission holds exactly the three authorised dependencies`() {
        val constructor = DefaultKnowledgeSubmission::class.primaryConstructor
            ?: error("DefaultKnowledgeSubmission must have a primary constructor")

        assertEquals(3, constructor.parameters.size, "DefaultKnowledgeSubmission must depend on exactly three collaborators")
    }

    private fun arbitraryCandidate(): KnowledgeCandidate =
        KnowledgeCandidate(MemoryCoreRecordReference.ToAssertion(parker.core.interfaces.AssertionId("assertion-1")))
}

/** Test-only fake, mirroring [FakePermissionEngine]'s own lambda-configurable, call-counting shape. */
private class FakeKnowledgeCandidateEvaluator(
    private val result: KnowledgeCandidateEvaluation,
) : KnowledgeCandidateEvaluator {

    var evaluateCallCount: Int = 0
        private set
    var lastRequestingPrincipalId: PrincipalId? = null
        private set

    override fun evaluate(
        requestingPrincipalId: PrincipalId,
        candidate: KnowledgeCandidate,
    ): KnowledgeCandidateEvaluation {
        evaluateCallCount++
        lastRequestingPrincipalId = requestingPrincipalId
        return result
    }
}

/**
 * Test-only fake, mirroring [FakeEvidenceArtifactStorage]'s own
 * delegate-plus-call-count shape. Delegates to a real
 * [InMemoryKnowledgeItemPersistence] so tests that only care about call
 * counting still get real storage semantics.
 */
private class FakeKnowledgeItemPersistence : KnowledgeItemPersistence {

    private val delegate = InMemoryKnowledgeItemPersistence()

    var storeCallCount: Int = 0
        private set

    override suspend fun store(item: KnowledgeItem): KnowledgeItem {
        storeCallCount++
        return delegate.store(item)
    }

    override suspend fun find(knowledgeId: parker.core.interfaces.KnowledgeId): KnowledgeItem? =
        delegate.find(knowledgeId)

    override suspend fun findAll(): List<KnowledgeItem> = delegate.findAll()
}
