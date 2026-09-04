package parker.core.runtime

import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset
import kotlinx.coroutines.test.runTest
import parker.core.interfaces.*
import kotlin.test.*

class HumanCorrectionPermissionPolicyTest {
    private val owner = HumanFidelityReviewFixture.reviewer
    private val target = HumanFidelityReviewFixture.target
    private val reviewId = HumanFidelityReviewFixture.review().reviewId
    private val clock = Clock.fixed(Instant.parse("2026-09-04T00:00:00Z"), ZoneOffset.UTC)

    @Test
    fun `exact correction purpose owner review and target authorize`() = runTest {
        val registry = InMemoryAuthorizationPurposeRegistry()
        assertEquals(AuthorizationPurposeRegistrationOutcome.Registered(HUMAN_TRANSCRIPTION_CORRECTION_PURPOSE),
            HumanCorrectionPermissionPolicy.registerPurpose(registry))
        assertEquals(AuthorizationPurposeRegistrationOutcome.AlreadyRegistered(HUMAN_TRANSCRIPTION_CORRECTION_PURPOSE),
            HumanCorrectionPermissionPolicy.registerPurpose(registry))
        val engine = RecordingEngine(PermissionDecisionOutcome.APPROVED)
        assertEquals(HumanCorrectionPermissionResult.Authorized,
            HumanCorrectionPermissionPolicy(owner, registry, engine, clock).evaluate(authority(), target, reviewId))
        assertEquals(HUMAN_TRANSCRIPTION_CORRECTION_PURPOSE, engine.request!!.authorizationPurpose)
        assertEquals(HumanCorrectionPermissionPolicy.resourceIdFor(target, reviewId), engine.request!!.targetResources.single())
    }

    @Test
    fun `wrong owner purpose target and review fail closed before permission evaluation`() = runTest {
        val registry = InMemoryAuthorizationPurposeRegistry().also { HumanCorrectionPermissionPolicy.registerPurpose(it) }
        val mutations = listOf(
            authority().copy(principalId = PrincipalId("owner.wrong")),
            authority().copy(authorizationPurpose = HUMAN_FIDELITY_REVIEW_RECORDING_PURPOSE),
            authority().copy(target = target.copy(sourceSha256 = OcrSha256Digest("1".repeat(64)))),
            authority().copy(reviewId = HumanFidelityReviewId("review-" + "1".repeat(64))),
        )
        mutations.forEach { mutated ->
            val engine = RecordingEngine(PermissionDecisionOutcome.APPROVED)
            assertIs<HumanCorrectionPermissionResult.Denied>(
                HumanCorrectionPermissionPolicy(owner, registry, engine, clock).evaluate(mutated, target, reviewId))
            assertNull(engine.request)
        }
    }

    @Test
    fun `inactive purpose and permission denial fail closed`() = runTest {
        assertEquals(HumanCorrectionPermissionResult.Denied(HumanCorrectionDenialReason.PURPOSE_NOT_ACTIVE),
            HumanCorrectionPermissionPolicy(owner, InMemoryAuthorizationPurposeRegistry(),
                RecordingEngine(PermissionDecisionOutcome.APPROVED), clock).evaluate(authority(), target, reviewId))
        val active = InMemoryAuthorizationPurposeRegistry().also { HumanCorrectionPermissionPolicy.registerPurpose(it) }
        assertEquals(HumanCorrectionPermissionResult.Denied(HumanCorrectionDenialReason.PERMISSION_POLICY_DENIED),
            HumanCorrectionPermissionPolicy(owner, active, RecordingEngine(PermissionDecisionOutcome.DENIED), clock)
                .evaluate(authority(), target, reviewId))
    }

    private fun authority() = HumanCorrectionAuthorityScope(owner, HUMAN_TRANSCRIPTION_CORRECTION_PURPOSE, target, reviewId)

    private inner class RecordingEngine(private val outcome: PermissionDecisionOutcome) : PermissionEngine {
        var request: ExecutionRequest? = null
        override suspend fun evaluate(request: ExecutionRequest): PermissionDecision {
            this.request = request
            return PermissionDecision(DecisionId("decision-correction"), request.principalId, request.targetResources.single(),
                PermissionAction.WRITE, outcome, PermissionLevel.HIGH_ASSURANCE, clock.instant())
        }
        override suspend fun explain(decisionId: DecisionId) = PermissionExplanation(decisionId, "test")
    }
}
