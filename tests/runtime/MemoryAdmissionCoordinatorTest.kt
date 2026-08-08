package parker.core.runtime

import kotlinx.coroutines.test.runTest
import parker.core.interfaces.CandidateAssertion
import parker.core.interfaces.CandidateDocument
import parker.core.interfaces.CandidateEntity
import parker.core.interfaces.CandidateProvenance
import parker.core.interfaces.CandidateRelationship
import parker.core.interfaces.DecisionId
import parker.core.interfaces.Entity
import parker.core.interfaces.EvidentialState
import parker.core.interfaces.ExecutionRequest
import parker.core.interfaces.MemoryCore
import parker.core.interfaces.MemoryCoreRecord
import parker.core.interfaces.MemoryCoreRecordReference
import parker.core.interfaces.MemoryCoreRecordStatus
import parker.core.interfaces.MemoryRetrieval
import parker.core.interfaces.PermissionAction
import parker.core.interfaces.PermissionDecision
import parker.core.interfaces.PermissionDecisionOutcome
import parker.core.interfaces.PermissionEngine
import parker.core.interfaces.PermissionLevel
import parker.core.interfaces.PrincipalId
import parker.core.interfaces.Provenance
import java.time.Instant
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Parker Conversational Memory Bridge, Admission Unit. Behavioural tests
 * of [MemoryAdmissionCoordinator], per
 * `docs/implementation/CONVERSATIONAL_MEMORY_ADMISSION_IMPLEMENTATION_PLAN.md`
 * Section 3.5. Uses real [InMemoryMemoryCore] and real
 * [DefaultKnowledgeSubmission]/[DefaultKnowledgeCandidateEvaluator] --
 * already-tested production code, not hand-written fakes -- with only
 * [FakePermissionEngine] standing in for policy, mirroring
 * [EvidenceRegistrationCoordinatorTest]'s own established precedent.
 */
class MemoryAdmissionCoordinatorTest {

    private val principal = PrincipalId("user.owner")

    private fun approved(request: ExecutionRequest) = PermissionDecision(
        decisionId = DecisionId("dec-${request.requestId.value}"),
        principalId = request.principalId,
        resourceId = request.targetResources.single(),
        action = PermissionAction.WRITE,
        decision = PermissionDecisionOutcome.APPROVED,
        level = PermissionLevel.AUTOMATIC,
        timestamp = Instant.parse("2026-01-01T00:00:00Z"),
    )

    private fun denied(request: ExecutionRequest) = PermissionDecision(
        decisionId = DecisionId("dec-${request.requestId.value}"),
        principalId = request.principalId,
        resourceId = request.targetResources.single(),
        action = PermissionAction.WRITE,
        decision = PermissionDecisionOutcome.DENIED,
        level = PermissionLevel.AUTOMATIC,
        timestamp = Instant.parse("2026-01-01T00:00:00Z"),
    )

    private fun approveEverything() = FakePermissionEngine { request -> approved(request) }

    /**
     * [retrieval] is the read-side seam [DefaultKnowledgeCandidateEvaluator]
     * resolves the just-written Assertion through -- for a plain
     * [InMemoryMemoryCore], the same instance genuinely serves both roles
     * (it implements both [MemoryCore] and [MemoryRetrieval]); for
     * [SpyMemoryCore], which implements only [MemoryCore], the caller
     * supplies the real [InMemoryMemoryCore] the spy itself delegates to,
     * so a resolution genuinely observes what the spy actually wrote,
     * never a second, disconnected store.
     */
    private fun coordinator(memoryCore: MemoryCore, permissionEngine: PermissionEngine, retrieval: MemoryRetrieval = memoryCore as MemoryRetrieval): MemoryAdmissionCoordinator {
        val knowledgeSubmission = DefaultKnowledgeSubmission(
            DefaultKnowledgeCandidateEvaluator(retrieval),
            InMemoryKnowledgeItemPersistence(),
            permissionEngine,
        )
        return MemoryAdmissionCoordinator(memoryCore, knowledgeSubmission, permissionEngine)
    }

    /**
     * A thin spy wrapping a real [InMemoryMemoryCore], counting
     * [createProvenance]/[createAssertion] calls -- used to prove,
     * directly, whether this coordinator's own Memory Core write was
     * ever attempted, rather than inferring it indirectly through
     * [MemoryRetrieval], which offers no "list everything" query.
     */
    private class SpyMemoryCore(val delegate: InMemoryMemoryCore) : MemoryCore {
        var createProvenanceCallCount = 0
            private set
        var createAssertionCallCount = 0
            private set
        var lastCandidateProvenance: CandidateProvenance? = null
            private set

        override suspend fun createProvenance(requestingPrincipalId: PrincipalId, candidate: CandidateProvenance): Provenance {
            createProvenanceCallCount++
            lastCandidateProvenance = candidate
            return delegate.createProvenance(requestingPrincipalId, candidate)
        }

        override suspend fun createEntity(requestingPrincipalId: PrincipalId, candidate: CandidateEntity): Entity =
            delegate.createEntity(requestingPrincipalId, candidate)

        override suspend fun registerDocument(requestingPrincipalId: PrincipalId, candidate: CandidateDocument) =
            delegate.registerDocument(requestingPrincipalId, candidate)

        override suspend fun createAssertion(requestingPrincipalId: PrincipalId, candidate: CandidateAssertion) =
            delegate.createAssertion(requestingPrincipalId, candidate).also { createAssertionCallCount++ }

        override suspend fun createRelationship(requestingPrincipalId: PrincipalId, candidate: CandidateRelationship) =
            delegate.createRelationship(requestingPrincipalId, candidate)

        override suspend fun transitionStatus(
            requestingPrincipalId: PrincipalId,
            reference: MemoryCoreRecordReference,
            targetStatus: MemoryCoreRecordStatus,
        ): MemoryCoreRecord = delegate.transitionStatus(requestingPrincipalId, reference, targetStatus)
    }

    // ================= Success =================

    @Test
    fun `a genuine explicit instruction is durably stored and promoted with EvidentialState UNKNOWN`() = runTest {
        val memoryCore = InMemoryMemoryCore()
        val outcome = coordinator(memoryCore, approveEverything())
            .admit(principal, "corr-1", "my favourite coffee mug is black")

        val stored = assertIs<MemoryAdmissionOutcome.Stored>(outcome)
        assertEquals(EvidentialState.UNKNOWN, stored.item.evidentialState)
    }

    @Test
    fun `the underlying Assertion carries confidence null -- never fabricated`() = runTest {
        val memoryCore = InMemoryMemoryCore()
        val outcome = coordinator(memoryCore, approveEverything()).admit(principal, "corr-2", "Stellar is my dog")

        val stored = assertIs<MemoryAdmissionOutcome.Stored>(outcome)
        val assertionId = assertIs<MemoryCoreRecordReference.ToAssertion>(stored.item.evidenceReference).assertionId
        val assertion = memoryCore.getAssertion(principal, assertionId)
        assertNull(assertion?.confidence, "confidence must never be fabricated for a conversationally-sourced Assertion")
    }

    @Test
    fun `the correlation ID is preserved into the durable Provenance's own sourceIdentifier`() = runTest {
        val spy = SpyMemoryCore(InMemoryMemoryCore())
        val outcome = coordinator(spy, approveEverything(), spy.delegate).admit(principal, "corr-preserved-1", "the fact")

        assertIs<MemoryAdmissionOutcome.Stored>(outcome)
        assertEquals("corr-preserved-1", spy.lastCandidateProvenance?.sourceIdentifier)
        assertEquals("conversation", spy.lastCandidateProvenance?.sourceType)
    }

    @Test
    fun `the instruction text reaches the Assertion's own statement unchanged`() = runTest {
        val memoryCore = InMemoryMemoryCore()
        val outcome = coordinator(memoryCore, approveEverything()).admit(principal, "corr-4", "Blaze is Stellar's son")

        val stored = assertIs<MemoryAdmissionOutcome.Stored>(outcome)
        val assertionId = assertIs<MemoryCoreRecordReference.ToAssertion>(stored.item.evidenceReference).assertionId
        val assertion = memoryCore.getAssertion(principal, assertionId)
        assertEquals("Blaze is Stellar's son", assertion?.statement)
    }

    // ================= Permission denial: this coordinator's own admission gate =================

    @Test
    fun `denial at this coordinator's own admission gate prevents any Memory Core write`() = runTest {
        val spy = SpyMemoryCore(InMemoryMemoryCore())
        val permissionEngine = FakePermissionEngine { request ->
            if (MemoryAdmissionCoordinator.CONVERSATIONAL_MEMORY_RESOURCE_ID in request.targetResources) denied(request) else approved(request)
        }

        val outcome = coordinator(spy, permissionEngine, spy.delegate).admit(principal, "corr-5", "should never be stored")

        assertIs<MemoryAdmissionOutcome.NotAuthorised>(outcome)
        assertEquals(0, spy.createProvenanceCallCount, "no Provenance may be created once this coordinator's own gate denies")
        assertEquals(0, spy.createAssertionCallCount, "no Assertion may be created once this coordinator's own gate denies")
    }

    @Test
    fun `NotAuthorised from this coordinator's own gate names the denying decision honestly`() = runTest {
        val memoryCore = InMemoryMemoryCore()
        val permissionEngine = FakePermissionEngine { request ->
            if (MemoryAdmissionCoordinator.CONVERSATIONAL_MEMORY_RESOURCE_ID in request.targetResources) denied(request) else approved(request)
        }

        val outcome = coordinator(memoryCore, permissionEngine).admit(principal, "corr-6", "denied fact")

        val notAuthorised = assertIs<MemoryAdmissionOutcome.NotAuthorised>(outcome)
        assertTrue(notAuthorised.reason.contains(principal.value))
    }

    // ================= Permission denial: KnowledgeSubmission's own separate gate =================

    @Test
    fun `denial at KnowledgeSubmission's own separate gate still surfaces as NotAuthorised, after the Memory Core write already happened`() = runTest {
        val spy = SpyMemoryCore(InMemoryMemoryCore())
        val permissionEngine = FakePermissionEngine { request ->
            if (DefaultKnowledgeSubmission.KNOWLEDGE_SUBMISSION_RESOURCE_ID in request.targetResources) denied(request) else approved(request)
        }

        val outcome = coordinator(spy, permissionEngine, spy.delegate).admit(principal, "corr-7", "a fact Memory Core accepts but Knowledge Submission denies")

        assertIs<MemoryAdmissionOutcome.NotAuthorised>(outcome)
        assertEquals(1, spy.createProvenanceCallCount, "this coordinator's own write already happened -- KnowledgeSubmission's own, separate gate is a later, distinct stage")
        assertEquals(1, spy.createAssertionCallCount)
    }

    @Test
    fun `no double gating -- exactly two permission evaluations occur per successful admission, never one shared or a third`() = runTest {
        val memoryCore = InMemoryMemoryCore()
        var evaluateCallCount = 0
        val permissionEngine = FakePermissionEngine { request ->
            evaluateCallCount++
            approved(request)
        }

        coordinator(memoryCore, permissionEngine).admit(principal, "corr-8", "a fact")

        assertEquals(2, evaluateCallCount, "this coordinator's own admission gate, then KnowledgeSubmission's own separate, already-existing gate -- never more, never shared")
    }

    // ================= Exception propagation =================

    @Test
    fun `a genuine Memory Core fault propagates unchanged, never swallowed into a false outcome`() = runTest {
        val memoryCore = InMemoryMemoryCore()

        // A blank statement fails CandidateAssertion's own constructor validation -- a genuine
        // fault this class performs no try/catch around.
        assertIs<IllegalArgumentException>(
            runCatching { coordinator(memoryCore, approveEverything()).admit(principal, "corr-9", "   ") }
                .exceptionOrNull()
                ?: error("expected an exception to propagate"),
        )
    }
}
