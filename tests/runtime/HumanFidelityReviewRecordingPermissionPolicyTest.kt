package parker.core.runtime

import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset
import kotlinx.coroutines.test.runTest
import kotlin.test.*
import parker.core.interfaces.*

class HumanFidelityReviewRecordingPermissionPolicyTest {
    private val owner = PrincipalId("owner.steven-francis-mctague")
    private val other = PrincipalId("owner.someone-else")
    private val clock = Clock.fixed(Instant.parse("2026-09-03T00:00:00Z"), ZoneOffset.UTC)

    @Test
    fun `canonical purpose registers exactly and duplicate registration is deterministic`() = runTest {
        val registry = InMemoryAuthorizationPurposeRegistry()
        assertEquals("document-ingestion.human-fidelity-review-recording", HUMAN_FIDELITY_REVIEW_RECORDING_PURPOSE.value)
        assertEquals(
            AuthorizationPurposeRegistrationOutcome.Registered(HUMAN_FIDELITY_REVIEW_RECORDING_PURPOSE),
            HumanFidelityReviewRecordingPermissionPolicy.registerPurpose(registry),
        )
        assertEquals(
            AuthorizationPurposeRegistrationOutcome.AlreadyRegistered(HUMAN_FIDELITY_REVIEW_RECORDING_PURPOSE),
            HumanFidelityReviewRecordingPermissionPolicy.registerPurpose(registry),
        )
        assertTrue(registry.isActive(HUMAN_FIDELITY_REVIEW_RECORDING_PURPOSE))
        assertNull(registry.lookup(AuthorizationPurposeId("document-ingestion.human-transcription-correction")))
    }

    @Test
    fun `owner exact purpose exact target and real permission policy authorize`() = runTest {
        val fixture = fixture()
        assertEquals(HumanFidelityReviewRecordingPermissionResult.Authorized, fixture.gate.evaluate(request()))
        assertEquals(HUMAN_FIDELITY_REVIEW_RECORDING_PURPOSE, fixture.engine.lastRequest?.authorizationPurpose)
        assertEquals(HumanFidelityReviewRecordingPermissionPolicy.resourceIdFor(target()), fixture.engine.lastRequest?.targetResources?.single())
        assertEquals(HumanFidelityReviewRecordingPermissionPolicy.RECORD_ACTION_NAME, fixture.engine.lastRequest?.proposedActions?.single())
    }

    @Test
    fun `wrong absent unknown and retired purposes fail closed before permission mutation`() = runTest {
        val fixture = fixture()
        assertDenied(HumanFidelityReviewRecordingDenialReason.MISSING_OR_WRONG_PURPOSE,
            fixture.gate.evaluate(request(purpose = null)))
        assertDenied(HumanFidelityReviewRecordingDenialReason.MISSING_OR_WRONG_PURPOSE,
            fixture.gate.evaluate(request(purpose = AuthorizationPurposeId("evidence-intelligence.external-transcription"))))

        val unknownRegistry = InMemoryAuthorizationPurposeRegistry()
        val unknown = gate(unknownRegistry, approvingEngine())
        assertDenied(HumanFidelityReviewRecordingDenialReason.PURPOSE_NOT_ACTIVE, unknown.evaluate(request()))

        fixture.registry.retire(HUMAN_FIDELITY_REVIEW_RECORDING_PURPOSE)
        assertDenied(HumanFidelityReviewRecordingDenialReason.PURPOSE_NOT_ACTIVE, fixture.gate.evaluate(request()))
    }

    @Test
    fun `wrong principal and malformed identity fail closed`() = runTest {
        val fixture = fixture()
        assertDenied(HumanFidelityReviewRecordingDenialReason.WRONG_PRINCIPAL,
            fixture.gate.evaluate(request(principal = other)))
        assertFailsWith<IllegalArgumentException> { PrincipalId(" ") }
    }

    @Test
    fun `each exact target identity mismatch is denied before engine evaluation`() = runTest {
        val mutations = listOf(
            target().copy(evidenceArtifactId = EvidenceArtifactId("evidence-${"1".repeat(64)}")),
            target().copy(sourceSha256 = OcrSha256Digest("1".repeat(64))),
            target().copy(preparationIdentity = OcrSha256Digest("2".repeat(64))),
            target().copy(derivativeGenerationId = DerivativeGenerationId("region-${"3".repeat(64)}")),
            target().copy(derivativeGenerationSha256 = OcrSha256Digest("4".repeat(64))),
            target().copy(derivativeContentSha256 = OcrSha256Digest("5".repeat(64))),
        )
        mutations.forEach { proposed ->
            val fixture = fixture()
            assertDenied(HumanFidelityReviewRecordingDenialReason.TARGET_MISMATCH,
                fixture.gate.evaluate(request(proposed = proposed)))
            assertEquals(0, fixture.engine.calls)
        }
    }

    @Test
    fun `real policy denial remains denial and no permissive fallback exists`() = runTest {
        val fixture = fixture(policyOutcome = PermissionDecisionOutcome.DENIED)
        assertDenied(HumanFidelityReviewRecordingDenialReason.PERMISSION_POLICY_DENIED, fixture.gate.evaluate(request()))

        val approvedExactOnly = fixture()
        val unregisteredTarget = target().copy(sourceSha256 = OcrSha256Digest("6".repeat(64)))
        assertDenied(
            HumanFidelityReviewRecordingDenialReason.PERMISSION_POLICY_DENIED,
            approvedExactOnly.gate.evaluate(request(authorityTarget = unregisteredTarget, proposed = unregisteredTarget)),
        )
    }

    @Test
    fun `review purpose cannot authorize external transcription or correction actions`() = runTest {
        val fixture = fixture()
        val review = fixture.engine.evaluate(execution(HumanFidelityReviewRecordingPermissionPolicy.RECORD_ACTION_NAME))
        val external = fixture.engine.evaluate(execution("external-transcription.invoke"))
        val correction = fixture.engine.evaluate(execution("human-transcription-correction.accept"))
        assertEquals(PermissionDecisionOutcome.APPROVED, review.decision)
        assertEquals(PermissionDecisionOutcome.DENIED, external.decision)
        assertEquals(PermissionDecisionOutcome.DENIED, correction.decision)
    }

    @Test
    fun `authority contract and evaluator expose no storage correction provider or audit operation`() {
        val operations = HumanFidelityReviewRecordingPermissionEvaluator::class.members
            .filter { it.isAbstract }.map { it.name }.toSet()
        assertEquals(setOf("evaluate"), operations)
        val fields = HumanFidelityReviewRecordingAuthorityScope::class.members.map { it.name }.toSet()
        assertEquals(setOf("principalId", "authorizationPurpose", "target"), fields.intersect(
            setOf("principalId", "authorizationPurpose", "target", "correction", "provider", "audit", "storage")))
    }

    private suspend fun fixture(
        policyOutcome: PermissionDecisionOutcome = PermissionDecisionOutcome.APPROVED,
    ): Fixture {
        val registry = InMemoryAuthorizationPurposeRegistry()
        HumanFidelityReviewRecordingPermissionPolicy.registerPurpose(registry)
        val engine = realEngine(policyOutcome, registry)
        return Fixture(registry, engine, gate(registry, engine))
    }

    private fun gate(registry: AuthorizationPurposeRegistry, engine: PermissionEngine) =
        HumanFidelityReviewRecordingPermissionPolicy(owner, registry, engine, clock)

    private suspend fun realEngine(
        outcome: PermissionDecisionOutcome,
        registry: AuthorizationPurposeRegistry,
    ): RecordingEngine {
        val identities = InMemoryIdentityService()
        val created = Principal(owner, PrincipalType.USER, "Owner", null, PrincipalStatus.CREATED,
            clock.instant(), clock.instant())
        identities.register(created)
        identities.updateStatus(owner, PrincipalStatus.ACTIVE)

        val resources = InMemoryResourceRegistry()
        resources.register(Resource(
            HumanFidelityReviewRecordingPermissionPolicy.resourceIdFor(target()), ResourceType.DOCUMENT,
            "Human fidelity review recording", owner, ResourceSensitivity.LEGAL,
            ResourceLifecycleState.AVAILABLE, clock.instant(), clock.instant(), "test",
        ))
        val vocabulary = InMemoryActionVocabulary()
        vocabulary.register(ActionVocabularyEntry(
            HumanFidelityReviewRecordingPermissionPolicy.RECORD_ACTION_NAME,
            setOf(ActionResourceMapping(PermissionAction.WRITE, ResourceType.DOCUMENT)),
        ))
        val rule = PermissionPolicyRule(
            PermissionAction.WRITE, ResourceType.DOCUMENT, outcome, PermissionLevel.HIGH_ASSURANCE,
            HUMAN_FIDELITY_REVIEW_RECORDING_PURPOSE,
            HumanFidelityReviewRecordingPermissionPolicy.RECORD_ACTION_NAME,
        )
        val delegate = DefaultPermissionEngine(identities, DefaultPermissionPolicy(
            ActionMapper(vocabulary), resources, listOf(rule), registry,
        ))
        return RecordingEngine(delegate)
    }

    private fun approvingEngine(): PermissionEngine = object : PermissionEngine {
        override suspend fun evaluate(request: ExecutionRequest) = PermissionDecision(
            DecisionId("decision-test"), request.principalId, request.targetResources.single(),
            PermissionAction.WRITE, PermissionDecisionOutcome.APPROVED, PermissionLevel.HIGH_ASSURANCE,
            clock.instant(),
        )

        override suspend fun explain(decisionId: DecisionId) = PermissionExplanation(decisionId, "test")
    }

    private fun request(
        principal: PrincipalId = owner,
        purpose: AuthorizationPurposeId? = HUMAN_FIDELITY_REVIEW_RECORDING_PURPOSE,
        authorityTarget: HumanFidelityReviewTarget = target(),
        proposed: HumanFidelityReviewTarget = target(),
    ) = HumanFidelityReviewRecordingPermissionRequest(
        HumanFidelityReviewRecordingAuthorityScope(principal, purpose, authorityTarget), proposed,
    )

    private fun target() = HumanFidelityReviewFixture.target

    private fun execution(action: String) = ExecutionRequest(
        RequestId("request-$action"), owner, RequestOrigin.REMOTE_INTERFACE, "test",
        listOf(HumanFidelityReviewRecordingPermissionPolicy.resourceIdFor(target())), listOf(action),
        RequestPriority.NORMAL, clock.instant(), "correlation-$action",
        authorizationPurpose = HUMAN_FIDELITY_REVIEW_RECORDING_PURPOSE,
    )

    private fun assertDenied(
        reason: HumanFidelityReviewRecordingDenialReason,
        actual: HumanFidelityReviewRecordingPermissionResult,
    ) = assertEquals(HumanFidelityReviewRecordingPermissionResult.Denied(reason), actual)

    private data class Fixture(
        val registry: InMemoryAuthorizationPurposeRegistry,
        val engine: RecordingEngine,
        val gate: HumanFidelityReviewRecordingPermissionPolicy,
    )

    private class RecordingEngine(private val delegate: PermissionEngine) : PermissionEngine by delegate {
        var calls = 0
        var lastRequest: ExecutionRequest? = null
        override suspend fun evaluate(request: ExecutionRequest): PermissionDecision {
            calls++
            lastRequest = request
            return delegate.evaluate(request)
        }
    }
}
