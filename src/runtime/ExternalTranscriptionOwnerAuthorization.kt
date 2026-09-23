package parker.core.runtime

import java.nio.file.FileAlreadyExistsException
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardOpenOption
import java.security.MessageDigest
import java.time.Clock
import java.time.Instant
import parker.core.interfaces.*

/**
 * UI-INGESTION-5: durable, exact-target owner authorization for the existing, already-governed
 * "enhanced transcription" capability ([ExternalTranscriptionInvocationGate],
 * [ExternalTranscriptionOwnerInvocationCoordinator]). Neither the static [PermissionPolicyRule]
 * for [ExternalTranscriptionInvocationGate.AUTHORIZATION_PURPOSE] (approves the coarse verb once
 * the Purpose is active, for every evidence target alike) nor [GovernedAcquisitionOwnerWorkflow]
 * (previously always [ExternalEgressAuthorisation.NOT_AUTHORISED]) previously recorded a
 * per-evidence-artifact owner decision. This file adds exactly that record, and nothing else --
 * the Authorization Purpose, permission-policy evaluation, and provider adapter are all reused
 * unmodified.
 */

data class ExternalTranscriptionOwnerAuthorization(
    val evidenceArtifactId: String,
    val sourceSha256: String,
    val principalId: String,
    val purpose: String,
    val approvedAt: Instant,
    val derivedFromBatchId: String? = null,
) {
    init {
        require(evidenceArtifactId.isNotBlank())
        require(sourceSha256.matches(Regex("^[0-9a-f]{64}$")))
        require(principalId.isNotBlank())
        require(purpose.isNotBlank())
        require(derivedFromBatchId == null || derivedFromBatchId.matches(Regex("^bulk-[a-f0-9-]+$")))
    }
}

enum class ExternalTranscriptionAuthorizationDisposition { NOT_AUTHORISED, AUTHORISED, UNAVAILABLE }

sealed interface ExternalTranscriptionBatchAuthorizationOutcome {
    data object Authorised : ExternalTranscriptionBatchAuthorizationOutcome
    data class Rejected(val reason: String) : ExternalTranscriptionBatchAuthorizationOutcome
}

data class ExternalTranscriptionAuthorizationView(
    val disposition: ExternalTranscriptionAuthorizationDisposition,
    val evidenceArtifactId: String,
    val provider: String = "OpenAI",
    val purpose: String = ExternalTranscriptionInvocationGate.AUTHORIZATION_PURPOSE.value,
    val disclosure: String = "This document will be sent to the configured external transcription provider only if " +
        "\"Run enhanced transcription\" is separately triggered afterward.",
    val approvedAt: Instant? = null,
    val detail: String? = null,
)

/** The existing per-evidence grant store contract. Never calls a provider. */
interface ExternalTranscriptionOwnerAuthorizationStore {
    fun loadIfPresent(evidenceArtifactId: String): ExternalTranscriptionOwnerAuthorization?
    fun createOrGet(grant: ExternalTranscriptionOwnerAuthorization): ExternalTranscriptionOwnerAuthorizationStoreOutcome
}

/** Idempotent, tamper-evident, one-file-per-target durable grant store. Never calls a provider. */
class FileSystemExternalTranscriptionAuthorizationStore(storageRoot: Path) : ExternalTranscriptionOwnerAuthorizationStore {
    private val root = storageRoot.toAbsolutePath().normalize()
    init { require(Files.isDirectory(root) && Files.isReadable(root) && Files.isWritable(root)) }

    override fun loadIfPresent(evidenceArtifactId: String): ExternalTranscriptionOwnerAuthorization? {
        val path = base(evidenceArtifactId)
        if (!Files.isRegularFile(path)) return null
        return decode(Files.readString(path))
    }

    /**
     * Creates the grant if absent. If a grant already exists for this exact evidence artifact,
     * this call is idempotent only when every other field also matches exactly (same principal,
     * purpose, source digest) -- any mismatch fails closed rather than silently reusing a
     * differently-scoped prior grant.
     */
    override fun createOrGet(grant: ExternalTranscriptionOwnerAuthorization): ExternalTranscriptionOwnerAuthorizationStoreOutcome {
        val path = base(grant.evidenceArtifactId)
        return try {
            Files.newByteChannel(path, StandardOpenOption.CREATE_NEW, StandardOpenOption.WRITE).use { channel ->
                channel.write(java.nio.ByteBuffer.wrap(encode(grant).toByteArray(Charsets.UTF_8)))
            }
            ExternalTranscriptionOwnerAuthorizationStoreOutcome.Created(grant)
        } catch (_: FileAlreadyExistsException) {
            val existing = decode(Files.readString(path))
            if (existing.sourceSha256 == grant.sourceSha256 && existing.principalId == grant.principalId &&
                existing.purpose == grant.purpose
            ) {
                ExternalTranscriptionOwnerAuthorizationStoreOutcome.AlreadyExisted(existing)
            } else {
                ExternalTranscriptionOwnerAuthorizationStoreOutcome.Conflict(existing)
            }
        }
    }

    private fun base(evidenceArtifactId: String) =
        root.resolve("${digest(evidenceArtifactId)}.external-transcription-authorization-v1").normalize()
            .also { require(it.parent == root) }

    private fun encode(g: ExternalTranscriptionOwnerAuthorization): String {
        val fields = listOf(g.evidenceArtifactId, g.sourceSha256, g.principalId, b64(g.purpose), g.approvedAt.toString(), b64(g.derivedFromBatchId ?: ""))
        val body = fields.joinToString("\t")
        return "$body\t${digest(body)}\n"
    }

    private fun decode(text: String): ExternalTranscriptionOwnerAuthorization {
        val parts = text.trimEnd().split('\t')
        require(parts.size == 6 || parts.size == 7)
        val body = parts.dropLast(1).joinToString("\t")
        require(parts.last() == digest(body)) { "authorization record failed integrity check" }
        val batchId = if (parts.size == 7) unb64(parts[5]).ifBlank { null } else null
        return ExternalTranscriptionOwnerAuthorization(parts[0], parts[1], parts[2], unb64(parts[3]), Instant.parse(parts[4]), batchId)
    }

    private fun digest(value: String) =
        MessageDigest.getInstance("SHA-256").digest(value.toByteArray(Charsets.UTF_8)).joinToString("") { "%02x".format(it.toInt() and 255) }
    private fun b64(value: String) = java.util.Base64.getEncoder().encodeToString(value.toByteArray(Charsets.UTF_8))
    private fun unb64(value: String) = String(java.util.Base64.getDecoder().decode(value), Charsets.UTF_8)
}

sealed interface ExternalTranscriptionOwnerAuthorizationStoreOutcome {
    data class Created(val grant: ExternalTranscriptionOwnerAuthorization) : ExternalTranscriptionOwnerAuthorizationStoreOutcome
    data class AlreadyExisted(val grant: ExternalTranscriptionOwnerAuthorization) : ExternalTranscriptionOwnerAuthorizationStoreOutcome
    data class Conflict(val existing: ExternalTranscriptionOwnerAuthorization) : ExternalTranscriptionOwnerAuthorizationStoreOutcome
}

/**
 * Owner-facing coordinator: reuses the existing Authorization Purpose, the existing
 * [PermissionEngine] decision, and the existing opaque owner high-authority verification
 * boundary ([OwnerHighAuthorityVerification]) -- adds only the durable per-target record.
 * [authorize] never invokes [ExternalTranscriptionMechanism] or any other provider adapter.
 */
class ExternalTranscriptionOwnerAuthorizationCoordinator(
    private val ownerPrincipalId: PrincipalId,
    private val evidenceCustodian: EvidenceCustodian,
    private val purposes: AuthorizationPurposeRegistry,
    private val permissions: PermissionEngine,
    private val ownerVerification: OwnerHighAuthorityVerification,
    private val store: ExternalTranscriptionOwnerAuthorizationStore,
    private val clock: Clock = Clock.systemUTC(),
    private val auditReader: CaseGovernanceAuditReader? = null,
    private val standingPolicy: FileSystemStandingExternalTranscriptionPolicyStore? = null,
    private val governanceAudit: CaseGovernanceAudit? = null,
    private val ownerUnlockProofStore: InMemoryOwnerUnlockProofStore? = null,
) {
    private val purpose = ExternalTranscriptionInvocationGate.AUTHORIZATION_PURPOSE

    private sealed interface ExactTargetResolution {
        data class Ready(val manifest: EvidenceSourceManifest) : ExactTargetResolution
        data class Rejected(val view: ExternalTranscriptionAuthorizationView) : ExactTargetResolution
    }

    private suspend fun resolveExactTarget(evidenceArtifactId: EvidenceArtifactId): ExactTargetResolution {
        if (!purposes.isActive(purpose)) {
            return ExactTargetResolution.Rejected(
                ExternalTranscriptionAuthorizationView(
                    ExternalTranscriptionAuthorizationDisposition.UNAVAILABLE, evidenceArtifactId.value,
                    detail = "PURPOSE_NOT_ACTIVE",
                ),
            )
        }
        val manifest = when (val retrieved = evidenceCustodian.retrieveManifest(ownerPrincipalId, evidenceArtifactId)) {
            is EvidenceManifestRetrievalResult.Found -> retrieved.manifest
            else -> return ExactTargetResolution.Rejected(
                ExternalTranscriptionAuthorizationView(
                    ExternalTranscriptionAuthorizationDisposition.UNAVAILABLE, evidenceArtifactId.value,
                    detail = "SOURCE_UNAVAILABLE",
                ),
            )
        }
        if (manifest.evidenceArtifactId != evidenceArtifactId) {
            return ExactTargetResolution.Rejected(
                ExternalTranscriptionAuthorizationView(
                    ExternalTranscriptionAuthorizationDisposition.UNAVAILABLE, evidenceArtifactId.value,
                    detail = "SOURCE_IDENTITY_MISMATCH",
                ),
            )
        }
        return ExactTargetResolution.Ready(manifest)
    }

    private suspend fun permissionAllows(evidenceArtifactId: EvidenceArtifactId): Boolean {
        val decision = permissions.evaluate(ExternalTranscriptionInvocationGate.buildExecutionRequest(ownerPrincipalId, evidenceArtifactId))
        return decision.decision == PermissionDecisionOutcome.APPROVED ||
            decision.decision == PermissionDecisionOutcome.APPROVED_WITH_CONFIRMATION
    }

    /** Explicit Owner-only administrative establishment of the standing policy. */
    suspend fun establishStandingExternalTranscriptionPolicy(presented: OwnerVerificationCredential?): Boolean {
        val policyStore = standingPolicy ?: return false
        if (!purposes.isActive(purpose)) return false
        val target = ResourceId("standing-external-transcription-policy-${ownerPrincipalId.value}")
        if (!ownerVerification.verify(ownerPrincipalId, purpose, target, presented)) return false
        val decision = permissions.evaluate(ExternalTranscriptionInvocationGate.buildStandingPolicyExecutionRequest(ownerPrincipalId))
        if (decision.decision != PermissionDecisionOutcome.APPROVED && decision.decision != PermissionDecisionOutcome.APPROVED_WITH_CONFIRMATION) return false
        return policyStore.establish(
            StandingExternalTranscriptionPolicy(ownerPrincipalId, purpose, clock.instant()),
            governanceAudit ?: return false,
        )
    }

    suspend fun status(evidenceArtifactId: EvidenceArtifactId): ExternalTranscriptionAuthorizationView {
        val existing = loadUsable(evidenceArtifactId.value)
        return if (existing != null) {
            ExternalTranscriptionAuthorizationView(
                ExternalTranscriptionAuthorizationDisposition.AUTHORISED, evidenceArtifactId.value,
                approvedAt = existing.approvedAt,
            )
        } else {
            ExternalTranscriptionAuthorizationView(ExternalTranscriptionAuthorizationDisposition.NOT_AUTHORISED, evidenceArtifactId.value)
        }
    }

    /** True only when a durable grant for this exact evidence target already exists. */
    fun isAuthorized(evidenceArtifactId: EvidenceArtifactId): Boolean = loadUsable(evidenceArtifactId.value) != null

    /**
     * Returns an existing grant only when the current trusted manifest and permission policy
     * still validate the exact target. This is the idempotent pre-check used by the PIN route;
     * it never invokes high-authority verification and never executes a provider.
     */
    suspend fun currentAuthorizationIfValid(evidenceArtifactId: EvidenceArtifactId): ExternalTranscriptionAuthorizationView? {
        val resolution = resolveExactTarget(evidenceArtifactId)
        val manifest = (resolution as? ExactTargetResolution.Ready)?.manifest ?: return null
        if (!permissionAllows(evidenceArtifactId)) return null
        val existing = loadUsable(evidenceArtifactId.value) ?: return null
        if (existing.sourceSha256 != manifest.sha256 || existing.principalId != ownerPrincipalId.value || existing.purpose != purpose.value) return null
        return ExternalTranscriptionAuthorizationView(
            ExternalTranscriptionAuthorizationDisposition.AUTHORISED,
            evidenceArtifactId.value,
            approvedAt = existing.approvedAt,
        )
    }

    /**
     * Verifies one transient Owner approval for one Parker-minted batch. This creates no
     * evidence-level grant and never invokes a provider; the binding coordinator persists only
     * the resulting boolean and its audit fact. The batch identity is included in the verified
     * target and may not be substituted by Hermes.
     */
    suspend fun authorizeBatch(
        batchId: String,
        caseId: CaseId,
        presented: OwnerVerificationCredential?,
    ): ExternalTranscriptionBatchAuthorizationOutcome {
        if (!purposes.isActive(purpose)) return ExternalTranscriptionBatchAuthorizationOutcome.Rejected("PURPOSE_NOT_ACTIVE")
        val target = ResourceId("external-transcription-batch-authorization-$batchId-${caseId.value}")
        if (!ownerVerification.verify(ownerPrincipalId, purpose, target, presented)) {
            return ExternalTranscriptionBatchAuthorizationOutcome.Rejected("HIGH_AUTHORITY_VERIFICATION_FAILED")
        }
        val decision = permissions.evaluate(ExternalTranscriptionInvocationGate.buildBatchExecutionRequest(ownerPrincipalId, batchId, caseId.value))
        if (decision.decision != PermissionDecisionOutcome.APPROVED && decision.decision != PermissionDecisionOutcome.APPROVED_WITH_CONFIRMATION) {
            return ExternalTranscriptionBatchAuthorizationOutcome.Rejected("PERMISSION_POLICY_DENIED")
        }
        return ExternalTranscriptionBatchAuthorizationOutcome.Authorised
    }

    /**
     * Derives the normal exact-evidence grant from an already verified Owner-authorised batch.
     * Callers must prove the batch is valid and contains the evidence before invoking this
     * method. No Owner credential is accepted here: the batch approval is the authority basis.
     */
    suspend fun deriveEvidenceAuthorizationFromOwnerAuthorisedBatch(
        batchId: String,
        evidenceArtifactId: EvidenceArtifactId,
        beforePersist: suspend (ExternalTranscriptionOwnerAuthorization) -> Unit = {},
        afterPersist: suspend (ExternalTranscriptionOwnerAuthorization) -> Unit = {},
    ): Boolean {
        if (!purposes.isActive(purpose)) return false
        val policy = standingPolicy?.load() ?: return false
        if (policy.ownerPrincipalId != ownerPrincipalId || policy.authorizationPurpose != purpose) return false
        val manifest = when (val retrieved = evidenceCustodian.retrieveManifest(ownerPrincipalId, evidenceArtifactId)) {
            is EvidenceManifestRetrievalResult.Found -> retrieved.manifest
            else -> return false
        }
        val decision = permissions.evaluate(ExternalTranscriptionInvocationGate.buildExecutionRequest(ownerPrincipalId, evidenceArtifactId))
        if (decision.decision != PermissionDecisionOutcome.APPROVED && decision.decision != PermissionDecisionOutcome.APPROVED_WITH_CONFIRMATION) return false
        val grant = ExternalTranscriptionOwnerAuthorization(
            evidenceArtifactId = evidenceArtifactId.value,
            sourceSha256 = manifest.sha256,
            principalId = ownerPrincipalId.value,
            purpose = purpose.value,
            approvedAt = clock.instant(),
            derivedFromBatchId = batchId,
        )
        beforePersist(grant)
        return when (val outcome = store.createOrGet(grant)) {
            is ExternalTranscriptionOwnerAuthorizationStoreOutcome.Created -> {
                afterPersist(outcome.grant)
                isAuthorized(evidenceArtifactId)
            }
            is ExternalTranscriptionOwnerAuthorizationStoreOutcome.AlreadyExisted -> {
                if (outcome.grant.derivedFromBatchId == batchId) afterPersist(outcome.grant)
                isAuthorized(evidenceArtifactId)
            }
            is ExternalTranscriptionOwnerAuthorizationStoreOutcome.Conflict -> false
        }
    }

    /** Compatibility overload for existing direct callers that are not deriving from a batch. */
    suspend fun deriveEvidenceAuthorizationFromOwnerAuthorisedBatch(
        evidenceArtifactId: EvidenceArtifactId,
        beforePersist: suspend (ExternalTranscriptionOwnerAuthorization) -> Unit = {},
    ): Boolean {
        if (!purposes.isActive(purpose)) return false
        val manifest = (evidenceCustodian.retrieveManifest(ownerPrincipalId, evidenceArtifactId) as? EvidenceManifestRetrievalResult.Found)?.manifest ?: return false
        val decision = permissions.evaluate(ExternalTranscriptionInvocationGate.buildExecutionRequest(ownerPrincipalId, evidenceArtifactId))
        if (decision.decision != PermissionDecisionOutcome.APPROVED && decision.decision != PermissionDecisionOutcome.APPROVED_WITH_CONFIRMATION) return false
        val grant = ExternalTranscriptionOwnerAuthorization(evidenceArtifactId.value, manifest.sha256, ownerPrincipalId.value, purpose.value, clock.instant())
        beforePersist(grant)
        return when (store.createOrGet(grant)) {
            is ExternalTranscriptionOwnerAuthorizationStoreOutcome.Created,
            is ExternalTranscriptionOwnerAuthorizationStoreOutcome.AlreadyExisted -> true
            is ExternalTranscriptionOwnerAuthorizationStoreOutcome.Conflict -> false
        }
    }

    private fun loadUsable(evidenceArtifactId: String): ExternalTranscriptionOwnerAuthorization? {
        val grant = store.loadIfPresent(evidenceArtifactId) ?: return null
        val batchId = grant.derivedFromBatchId ?: return grant
        val reader = auditReader ?: return null
        return runCatching {
            if (reader.has(CaseGovernanceAuditQuery(
                    CaseGovernanceAuditEventType.INGESTION_BATCH_EXTERNAL_TRANSCRIPTION_DERIVED,
                    null, EvidenceArtifactId(evidenceArtifactId), batchId, true,
                    authorizationPurpose = grant.purpose, policyVersion = STANDING_EXTERNAL_TRANSCRIPTION_POLICY_VERSION,
                ))) grant else null
        }.getOrNull()
    }

    /**
     * Explicit owner confirmation. Requires: an active Authorization Purpose, a resolvable
     * authoritative manifest for exact-target binding, a passing high-authority verification of
     * [presented], and an APPROVED/APPROVED_WITH_CONFIRMATION permission decision -- the same
     * gates the existing invocation path already checks at execution time, checked again here so
     * creation fails closed exactly like execution would. No provider is ever invoked.
     */
    suspend fun authorize(
        evidenceArtifactId: EvidenceArtifactId,
        presented: OwnerVerificationCredential?,
    ): ExternalTranscriptionAuthorizationView {
        val resolution = resolveExactTarget(evidenceArtifactId)
        val manifest = when (resolution) {
            is ExactTargetResolution.Ready -> resolution.manifest
            is ExactTargetResolution.Rejected -> return resolution.view
        }
        val target = ResourceId("external-transcription-authorization-${manifest.evidenceArtifactId.value}-${manifest.sha256}")
        if (!ownerVerification.verify(ownerPrincipalId, purpose, target, presented)) {
            return ExternalTranscriptionAuthorizationView(
                ExternalTranscriptionAuthorizationDisposition.NOT_AUTHORISED, evidenceArtifactId.value,
                detail = "HIGH_AUTHORITY_VERIFICATION_FAILED",
            )
        }
        if (!permissionAllows(evidenceArtifactId)) {
            return ExternalTranscriptionAuthorizationView(
                ExternalTranscriptionAuthorizationDisposition.NOT_AUTHORISED, evidenceArtifactId.value,
                detail = "PERMISSION_POLICY_DENIED",
            )
        }
        val grant = ExternalTranscriptionOwnerAuthorization(
            evidenceArtifactId.value, manifest.sha256, ownerPrincipalId.value, purpose.value, clock.instant(),
        )
        return when (val outcome = store.createOrGet(grant)) {
            is ExternalTranscriptionOwnerAuthorizationStoreOutcome.Created ->
                ExternalTranscriptionAuthorizationView(ExternalTranscriptionAuthorizationDisposition.AUTHORISED, evidenceArtifactId.value, approvedAt = outcome.grant.approvedAt)
            is ExternalTranscriptionOwnerAuthorizationStoreOutcome.AlreadyExisted ->
                ExternalTranscriptionAuthorizationView(ExternalTranscriptionAuthorizationDisposition.AUTHORISED, evidenceArtifactId.value, approvedAt = outcome.grant.approvedAt)
            is ExternalTranscriptionOwnerAuthorizationStoreOutcome.Conflict ->
                ExternalTranscriptionAuthorizationView(
                    ExternalTranscriptionAuthorizationDisposition.UNAVAILABLE, evidenceArtifactId.value,
                    detail = "CONFLICTING_PRIOR_AUTHORIZATION",
                )
        }
    }

    /**
     * Consumes a server-side Owner PIN unlock proof and then enters the same exact-source
     * authorization store used by [authorize]. The proof is consumed only after the current
     * manifest, purpose, and permission policy have been revalidated. Consumption is never
     * refunded if durable authorization persistence subsequently fails.
     */
    suspend fun authorizeWithUnlockProof(
        evidenceArtifactId: EvidenceArtifactId,
        proofId: OwnerUnlockProofId,
    ): ExternalTranscriptionAuthorizationView {
        val proofStore = ownerUnlockProofStore
            ?: return ExternalTranscriptionAuthorizationView(
                ExternalTranscriptionAuthorizationDisposition.UNAVAILABLE, evidenceArtifactId.value,
                detail = "OWNER_UNLOCK_PROOF_UNAVAILABLE",
            )
        val resolution = resolveExactTarget(evidenceArtifactId)
        val manifest = when (resolution) {
            is ExactTargetResolution.Ready -> resolution.manifest
            is ExactTargetResolution.Rejected -> return resolution.view
        }
        if (!permissionAllows(evidenceArtifactId)) {
            return ExternalTranscriptionAuthorizationView(
                ExternalTranscriptionAuthorizationDisposition.NOT_AUTHORISED, evidenceArtifactId.value,
                detail = "PERMISSION_POLICY_DENIED",
            )
        }

        val requestedScope = OwnerUnlockProofScope(
            principalId = ownerPrincipalId,
            evidenceArtifactId = evidenceArtifactId,
            sourceSha256 = parker.core.interfaces.OcrSha256Digest(manifest.sha256),
            authorizationPurpose = purpose,
            operation = OwnerUnlockProofOperation.EXTERNAL_TRANSCRIPTION_AUTHORIZATION,
        )
        return when (proofStore.consume(proofId, requestedScope)) {
            OwnerUnlockProofConsumeResult.CONSUMED -> {
                val grant = ExternalTranscriptionOwnerAuthorization(
                    evidenceArtifactId.value, manifest.sha256, ownerPrincipalId.value, purpose.value, clock.instant(),
                )
                try {
                    when (val outcome = store.createOrGet(grant)) {
                        is ExternalTranscriptionOwnerAuthorizationStoreOutcome.Created ->
                            ExternalTranscriptionAuthorizationView(
                                ExternalTranscriptionAuthorizationDisposition.AUTHORISED,
                                evidenceArtifactId.value,
                                approvedAt = outcome.grant.approvedAt,
                            )
                        is ExternalTranscriptionOwnerAuthorizationStoreOutcome.AlreadyExisted ->
                            ExternalTranscriptionAuthorizationView(
                                ExternalTranscriptionAuthorizationDisposition.AUTHORISED,
                                evidenceArtifactId.value,
                                approvedAt = outcome.grant.approvedAt,
                            )
                        is ExternalTranscriptionOwnerAuthorizationStoreOutcome.Conflict ->
                            ExternalTranscriptionAuthorizationView(
                                ExternalTranscriptionAuthorizationDisposition.UNAVAILABLE,
                                evidenceArtifactId.value,
                                detail = "CONFLICTING_PRIOR_AUTHORIZATION",
                            )
                    }
                } catch (_: RuntimeException) {
                    ExternalTranscriptionAuthorizationView(
                        ExternalTranscriptionAuthorizationDisposition.UNAVAILABLE,
                        evidenceArtifactId.value,
                        detail = "AUTHORIZATION_STORE_UNAVAILABLE",
                    )
                }
            }
            OwnerUnlockProofConsumeResult.EXPIRED -> proofFailure(evidenceArtifactId, "PROOF_EXPIRED")
            OwnerUnlockProofConsumeResult.REPLAY_REJECTED -> proofFailure(evidenceArtifactId, "PROOF_REPLAYED")
            OwnerUnlockProofConsumeResult.SCOPE_MISMATCH,
            OwnerUnlockProofConsumeResult.NOT_FOUND -> proofFailure(evidenceArtifactId, "PROOF_INVALID")
            OwnerUnlockProofConsumeResult.UNAVAILABLE -> proofFailure(evidenceArtifactId, "OWNER_UNLOCK_PROOF_UNAVAILABLE")
        }
    }

    private fun proofFailure(evidenceArtifactId: EvidenceArtifactId, detail: String) =
        ExternalTranscriptionAuthorizationView(
            ExternalTranscriptionAuthorizationDisposition.NOT_AUTHORISED,
            evidenceArtifactId.value,
            detail = detail,
        )
}
