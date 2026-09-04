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
) {
    init {
        require(evidenceArtifactId.isNotBlank())
        require(sourceSha256.matches(Regex("^[0-9a-f]{64}$")))
        require(principalId.isNotBlank())
        require(purpose.isNotBlank())
    }
}

enum class ExternalTranscriptionAuthorizationDisposition { NOT_AUTHORISED, AUTHORISED, UNAVAILABLE }

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

/** Idempotent, tamper-evident, one-file-per-target durable grant store. Never calls a provider. */
class FileSystemExternalTranscriptionAuthorizationStore(storageRoot: Path) {
    private val root = storageRoot.toAbsolutePath().normalize()
    init { require(Files.isDirectory(root) && Files.isReadable(root) && Files.isWritable(root)) }

    fun loadIfPresent(evidenceArtifactId: String): ExternalTranscriptionOwnerAuthorization? {
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
    fun createOrGet(grant: ExternalTranscriptionOwnerAuthorization): ExternalTranscriptionOwnerAuthorizationStoreOutcome {
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
        val fields = listOf(g.evidenceArtifactId, g.sourceSha256, g.principalId, b64(g.purpose), g.approvedAt.toString())
        val body = fields.joinToString("\t")
        return "$body\t${digest(body)}\n"
    }

    private fun decode(text: String): ExternalTranscriptionOwnerAuthorization {
        val parts = text.trimEnd().split('\t')
        require(parts.size == 6)
        val body = parts.take(5).joinToString("\t")
        require(parts.last() == digest(body)) { "authorization record failed integrity check" }
        return ExternalTranscriptionOwnerAuthorization(parts[0], parts[1], parts[2], unb64(parts[3]), Instant.parse(parts[4]))
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
    private val store: FileSystemExternalTranscriptionAuthorizationStore,
    private val clock: Clock = Clock.systemUTC(),
) {
    private val purpose = ExternalTranscriptionInvocationGate.AUTHORIZATION_PURPOSE

    suspend fun status(evidenceArtifactId: EvidenceArtifactId): ExternalTranscriptionAuthorizationView {
        val existing = store.loadIfPresent(evidenceArtifactId.value)
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
    fun isAuthorized(evidenceArtifactId: EvidenceArtifactId): Boolean = store.loadIfPresent(evidenceArtifactId.value) != null

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
        if (!purposes.isActive(purpose)) {
            return ExternalTranscriptionAuthorizationView(
                ExternalTranscriptionAuthorizationDisposition.UNAVAILABLE, evidenceArtifactId.value,
                detail = "PURPOSE_NOT_ACTIVE",
            )
        }
        val manifest = when (val retrieved = evidenceCustodian.retrieveManifest(ownerPrincipalId, evidenceArtifactId)) {
            is EvidenceManifestRetrievalResult.Found -> retrieved.manifest
            else -> return ExternalTranscriptionAuthorizationView(
                ExternalTranscriptionAuthorizationDisposition.UNAVAILABLE, evidenceArtifactId.value,
                detail = "SOURCE_UNAVAILABLE",
            )
        }
        if (manifest.evidenceArtifactId != evidenceArtifactId) {
            return ExternalTranscriptionAuthorizationView(
                ExternalTranscriptionAuthorizationDisposition.UNAVAILABLE, evidenceArtifactId.value,
                detail = "SOURCE_IDENTITY_MISMATCH",
            )
        }
        val target = ResourceId("external-transcription-authorization-${manifest.evidenceArtifactId.value}-${manifest.sha256}")
        if (!ownerVerification.verify(ownerPrincipalId, purpose, target, presented)) {
            return ExternalTranscriptionAuthorizationView(
                ExternalTranscriptionAuthorizationDisposition.NOT_AUTHORISED, evidenceArtifactId.value,
                detail = "HIGH_AUTHORITY_VERIFICATION_FAILED",
            )
        }
        val decision = permissions.evaluate(ExternalTranscriptionInvocationGate.buildExecutionRequest(ownerPrincipalId, evidenceArtifactId))
        if (decision.decision != PermissionDecisionOutcome.APPROVED && decision.decision != PermissionDecisionOutcome.APPROVED_WITH_CONFIRMATION) {
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
}
