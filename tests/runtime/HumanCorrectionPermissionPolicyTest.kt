package parker.core.runtime

import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset
import java.nio.file.Files
import java.nio.file.Path
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.io.TempDir
import parker.core.interfaces.*
import kotlin.test.*

class HumanCorrectionPermissionPolicyTest {
    @TempDir lateinit var directory: Path
    private val ownerId = PrincipalId("owner-" + "a".repeat(64))
    private val owner = OpaqueOwnerPrincipal(ownerId)
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
            HumanCorrectionPermissionPolicy(owner, registry, engine, verifier(), clock).evaluate(authority(), target, reviewId))
        assertEquals(HUMAN_TRANSCRIPTION_CORRECTION_PURPOSE, engine.request!!.authorizationPurpose)
        assertEquals(HumanCorrectionPermissionPolicy.resourceIdFor(target, reviewId), engine.request!!.targetResources.single())
    }

    @Test
    fun `wrong owner purpose target and review fail closed before permission evaluation`() = runTest {
        val registry = InMemoryAuthorizationPurposeRegistry().also { HumanCorrectionPermissionPolicy.registerPurpose(it) }
        val mutations = listOf(
            authority().copy(principalId = PrincipalId("owner-" + "b".repeat(64))),
            authority().copy(authorizationPurpose = HUMAN_FIDELITY_REVIEW_RECORDING_PURPOSE),
            authority().copy(target = target.copy(sourceSha256 = OcrSha256Digest("1".repeat(64)))),
            authority().copy(reviewId = HumanFidelityReviewId("review-" + "1".repeat(64))),
        )
        mutations.forEach { mutated ->
            val engine = RecordingEngine(PermissionDecisionOutcome.APPROVED)
            assertIs<HumanCorrectionPermissionResult.Denied>(
                HumanCorrectionPermissionPolicy(owner, registry, engine, verifier(), clock).evaluate(mutated, target, reviewId))
            assertNull(engine.request)
        }
    }

    @Test
    fun `inactive purpose and permission denial fail closed`() = runTest {
        assertEquals(HumanCorrectionPermissionResult.Denied(HumanCorrectionDenialReason.PURPOSE_NOT_ACTIVE),
            HumanCorrectionPermissionPolicy(owner, InMemoryAuthorizationPurposeRegistry(),
                RecordingEngine(PermissionDecisionOutcome.APPROVED), verifier(), clock).evaluate(authority(), target, reviewId))
        val active = InMemoryAuthorizationPurposeRegistry().also { HumanCorrectionPermissionPolicy.registerPurpose(it) }
        assertEquals(HumanCorrectionPermissionResult.Denied(HumanCorrectionDenialReason.PERMISSION_POLICY_DENIED),
            HumanCorrectionPermissionPolicy(owner, active, RecordingEngine(PermissionDecisionOutcome.DENIED), verifier(), clock)
                .evaluate(authority(), target, reviewId))
    }

    @Test
    fun `missing and wrong external credentials deny without permission evaluation`() = runTest {
        val active = InMemoryAuthorizationPurposeRegistry().also { HumanCorrectionPermissionPolicy.registerPurpose(it) }
        listOf(null, OwnerVerificationCredential.presented("wrong-secret-value-that-is-long-enough")) .forEach { credential ->
            val engine = RecordingEngine(PermissionDecisionOutcome.APPROVED)
            val result = HumanCorrectionPermissionPolicy(owner, active, engine, verifier(), clock)
                .evaluate(authority().copy(verificationCredential = credential), target, reviewId)
            assertEquals(HumanCorrectionPermissionResult.Denied(HumanCorrectionDenialReason.MISSING_OR_INVALID_VERIFICATION_CREDENTIAL), result)
            assertNull(engine.request)
        }
    }

    @Test
    fun `protected file credential verifies without exposing or persisting secret material`() = runTest {
        val sentinel = "opaque-high-authority-secret-sentinel-0001"
        val secretFile = directory.resolve("owner-verification.secret")
        Files.writeString(secretFile, sentinel + "\n")
        val verifier = ExternalFileOwnerHighAuthorityVerification.load(secretFile)
        val resource = HumanCorrectionPermissionPolicy.resourceIdFor(target, reviewId)
        val presented = requireNotNull(OwnerVerificationCredential.presented(sentinel))
        assertTrue(verifier.verify(ownerId, HUMAN_TRANSCRIPTION_CORRECTION_PURPOSE, resource, presented))
        val registry = InMemoryAuthorizationPurposeRegistry().also { HumanCorrectionPermissionPolicy.registerPurpose(it) }
        assertEquals(HumanCorrectionPermissionResult.Authorized,
            HumanCorrectionPermissionPolicy(owner, registry, RecordingEngine(PermissionDecisionOutcome.APPROVED), verifier, clock)
                .evaluate(authority().copy(verificationCredential = presented), target, reviewId))
        assertFalse(verifier.verify(ownerId, HUMAN_TRANSCRIPTION_CORRECTION_PURPOSE, resource,
            OwnerVerificationCredential.presented("wrong-high-authority-secret-value-0001")))
        assertFalse(sentinel in presented.toString())
        val audit = HumanCorrectionAuditRecord.deriveId(
            HumanCorrectionAuditEventType.CORRECTED_REPRESENTATION_PREPARED, ownerId,
            DerivativeGenerationId("human-corrected-" + "c".repeat(64)), target, reviewId,
            CorrectionAcceptanceId("correction-acceptance-" + "d".repeat(64)), OcrSha256Digest("e".repeat(64)),
        )
        assertFalse(sentinel in audit)
    }

    private fun authority() = HumanCorrectionAuthorityScope(ownerId, HUMAN_TRANSCRIPTION_CORRECTION_PURPOSE, target, reviewId,
        OwnerVerificationCredential.presented("correct-secret-value-that-is-long-enough"))
    private fun verifier() = OwnerHighAuthorityVerification { principal, purpose, resource, credential ->
        principal == ownerId && purpose == HUMAN_TRANSCRIPTION_CORRECTION_PURPOSE &&
            resource == HumanCorrectionPermissionPolicy.resourceIdFor(target, reviewId) &&
            credential != null && credential.constantTimeEquals("correct-secret-value-that-is-long-enough".toByteArray())
    }

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
