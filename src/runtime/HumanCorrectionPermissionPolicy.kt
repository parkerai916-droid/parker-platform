package parker.core.runtime

import java.security.MessageDigest
import java.time.Clock
import java.util.UUID
import parker.core.interfaces.*

class HumanCorrectionPermissionPolicy(
    private val owner: OpaqueOwnerPrincipal,
    private val purposes: AuthorizationPurposeRegistry,
    private val permissions: PermissionEngine,
    private val ownerVerification: OwnerHighAuthorityVerification,
    private val clock: Clock = Clock.systemUTC(),
) : HumanCorrectionPermissionEvaluator {
    override suspend fun evaluate(authority: HumanCorrectionAuthorityScope, target: HumanFidelityReviewTarget,
                                  reviewId: HumanFidelityReviewId): HumanCorrectionPermissionResult {
        if (authority.principalId != owner.principalId) return denied(HumanCorrectionDenialReason.WRONG_PRINCIPAL)
        if (authority.authorizationPurpose != HUMAN_TRANSCRIPTION_CORRECTION_PURPOSE) return denied(HumanCorrectionDenialReason.MISSING_OR_WRONG_PURPOSE)
        if (!purposes.isActive(HUMAN_TRANSCRIPTION_CORRECTION_PURPOSE)) return denied(HumanCorrectionDenialReason.PURPOSE_NOT_ACTIVE)
        if (authority.target != target || authority.reviewId != reviewId) return denied(HumanCorrectionDenialReason.TARGET_MISMATCH)
        if (!ownerVerification.verify(authority.principalId, HUMAN_TRANSCRIPTION_CORRECTION_PURPOSE,
                resourceIdFor(target, reviewId), authority.verificationCredential))
            return denied(HumanCorrectionDenialReason.MISSING_OR_INVALID_VERIFICATION_CREDENTIAL)
        val now = clock.instant(); val correlation = "human-correction-${UUID.randomUUID()}"
        val decision = permissions.evaluate(ExecutionRequest(
            requestId = RequestId(correlation), principalId = authority.principalId,
            origin = RequestOrigin.REMOTE_INTERFACE, intent = "Create exact governed human-corrected representation",
            targetResources = listOf(resourceIdFor(target, reviewId)), proposedActions = listOf(CORRECT_ACTION_NAME),
            priority = RequestPriority.NORMAL, createdAt = now, correlationId = correlation,
            authorizationPurpose = HUMAN_TRANSCRIPTION_CORRECTION_PURPOSE,
        ))
        return if (decision.decision in setOf(PermissionDecisionOutcome.APPROVED, PermissionDecisionOutcome.APPROVED_WITH_CONFIRMATION))
            HumanCorrectionPermissionResult.Authorized else denied(HumanCorrectionDenialReason.PERMISSION_POLICY_DENIED)
    }
    private fun denied(r: HumanCorrectionDenialReason) = HumanCorrectionPermissionResult.Denied(r)
    companion object {
        const val CORRECT_ACTION_NAME = "human-transcription-correction.create"
        fun resourceIdFor(target: HumanFidelityReviewTarget, reviewId: HumanFidelityReviewId): ResourceId {
            val text = listOf(target.evidenceArtifactId.value, target.sourceSha256.value, target.preparationIdentity.value,
                target.derivativeGenerationId.value, target.derivativeGenerationSha256.value,
                target.derivativeContentSha256.value, reviewId.value).joinToString("\u0000")
            val hash = MessageDigest.getInstance("SHA-256").digest(text.toByteArray()).joinToString("") { "%02x".format(it.toInt() and 255) }
            return ResourceId("human-correction-target-$hash")
        }
        suspend fun registerPurpose(registry: AuthorizationPurposeRegistry) = registry.register(HUMAN_TRANSCRIPTION_CORRECTION_PURPOSE)
    }
}

fun interface OwnerHighAuthorityVerification {
    fun verify(principalId: PrincipalId, purpose: AuthorizationPurposeId, target: ResourceId,
               presented: OwnerVerificationCredential?): Boolean
}

/**
 * [allowedPurposes] defaults to exactly [HUMAN_TRANSCRIPTION_CORRECTION_PURPOSE] alone, preserving
 * every existing caller's behavior unchanged. UI-INGESTION-5 reuses this same class -- the same
 * secret file, the same constant-time comparison, the same loading discipline -- for a second,
 * explicitly named purpose (evidence-intelligence.external-transcription) rather than inventing a
 * parallel high-authority mechanism; a shared secret still authorizes only the closed set of
 * purposes its loader was explicitly given, never an arbitrary or future one.
 */
class ExternalFileOwnerHighAuthorityVerification private constructor(
    private val expected: ByteArray,
    private val allowedPurposes: Set<AuthorizationPurposeId>,
) : OwnerHighAuthorityVerification {
    override fun verify(principalId: PrincipalId, purpose: AuthorizationPurposeId, target: ResourceId,
                        presented: OwnerVerificationCredential?): Boolean =
        principalId.value.isNotBlank() && purpose in allowedPurposes &&
            target.value.isNotBlank() && presented != null && presented.constantTimeEquals(expected)

    companion object {
        fun load(
            path: java.nio.file.Path,
            allowedPurposes: Set<AuthorizationPurposeId> = setOf(HUMAN_TRANSCRIPTION_CORRECTION_PURPOSE),
        ): ExternalFileOwnerHighAuthorityVerification {
            require(java.nio.file.Files.isRegularFile(path) && !java.nio.file.Files.isSymbolicLink(path))
            val bytes = java.nio.file.Files.readAllBytes(path).let { raw ->
                raw.toString(Charsets.UTF_8).trimEnd('\r', '\n').toByteArray(Charsets.UTF_8)
            }
            require(bytes.size in 32..4096)
            return ExternalFileOwnerHighAuthorityVerification(bytes, allowedPurposes)
        }
    }
}
