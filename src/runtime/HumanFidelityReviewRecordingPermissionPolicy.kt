package parker.core.runtime

import java.time.Clock
import java.security.MessageDigest
import java.util.UUID
import parker.core.interfaces.*

/**
 * A3's narrow owner and exact-target guard around Parker's existing Permission Engine.
 *
 * All local structural checks occur before the engine call. An approval is returned only when
 * the configured owner, exact canonical purpose, active vocabulary entry, exact A1 target, and
 * existing action/resource permission policy all agree. This class has no storage or service
 * dependency, so it cannot prepare/publish reviews or append audit facts.
 */
class HumanFidelityReviewRecordingPermissionPolicy(
    private val configuredOwnerPrincipalId: PrincipalId,
    private val authorizationPurposeRegistry: AuthorizationPurposeRegistry,
    private val permissionEngine: PermissionEngine,
    private val clock: Clock = Clock.systemUTC(),
) : HumanFidelityReviewRecordingPermissionEvaluator {

    override suspend fun evaluate(
        request: HumanFidelityReviewRecordingPermissionRequest,
    ): HumanFidelityReviewRecordingPermissionResult {
        if (request.authority.principalId != configuredOwnerPrincipalId) {
            return denied(HumanFidelityReviewRecordingDenialReason.WRONG_PRINCIPAL)
        }
        if (request.authority.authorizationPurpose != HUMAN_FIDELITY_REVIEW_RECORDING_PURPOSE) {
            return denied(HumanFidelityReviewRecordingDenialReason.MISSING_OR_WRONG_PURPOSE)
        }
        if (!authorizationPurposeRegistry.isActive(request.authority.authorizationPurpose)) {
            return denied(HumanFidelityReviewRecordingDenialReason.PURPOSE_NOT_ACTIVE)
        }
        if (request.authority.target != request.proposedReviewTarget) {
            return denied(HumanFidelityReviewRecordingDenialReason.TARGET_MISMATCH)
        }

        val decision = permissionEngine.evaluate(buildExecutionRequest(request.authority))
        return if (decision.decision == PermissionDecisionOutcome.APPROVED ||
            decision.decision == PermissionDecisionOutcome.APPROVED_WITH_CONFIRMATION
        ) {
            HumanFidelityReviewRecordingPermissionResult.Authorized
        } else {
            denied(HumanFidelityReviewRecordingDenialReason.PERMISSION_POLICY_DENIED)
        }
    }

    private fun buildExecutionRequest(scope: HumanFidelityReviewRecordingAuthorityScope): ExecutionRequest {
        val now = clock.instant()
        val correlation = "human-fidelity-review-authority-${UUID.randomUUID()}"
        return ExecutionRequest(
            requestId = RequestId(correlation),
            principalId = scope.principalId,
            origin = RequestOrigin.REMOTE_INTERFACE,
            intent = "Authorize immutable human fidelity review recording for an exact governed target",
            targetResources = listOf(resourceIdFor(scope.target)),
            proposedActions = listOf(RECORD_ACTION_NAME),
            priority = RequestPriority.NORMAL,
            createdAt = now,
            correlationId = correlation,
            authorizationPurpose = requireNotNull(scope.authorizationPurpose),
        )
    }

    private fun denied(reason: HumanFidelityReviewRecordingDenialReason) =
        HumanFidelityReviewRecordingPermissionResult.Denied(reason)

    companion object {
        const val RECORD_ACTION_NAME: String = "human-fidelity-review.record"

        /** Exact target resource: registration is an explicit prerequisite, never a wildcard capability. */
        fun resourceIdFor(target: HumanFidelityReviewTarget): ResourceId {
            val canonical = listOf(
                target.evidenceArtifactId.value,
                target.sourceSha256.value,
                target.preparationIdentity.value,
                target.derivativeGenerationId.value,
                target.derivativeGenerationSha256.value,
                target.derivativeContentSha256.value,
            ).joinToString("\u0000")
            val digest = MessageDigest.getInstance("SHA-256").digest(canonical.toByteArray(Charsets.UTF_8))
                .joinToString("") { "%02x".format(it.toInt() and 0xff) }
            return ResourceId("human-fidelity-review-target-$digest")
        }

        suspend fun registerPurpose(registry: AuthorizationPurposeRegistry): AuthorizationPurposeRegistrationOutcome =
            registry.register(HUMAN_FIDELITY_REVIEW_RECORDING_PURPOSE)
    }
}
