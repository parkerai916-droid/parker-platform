package parker.core.runtime

import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardCopyOption
import java.nio.channels.FileChannel
import java.nio.file.StandardOpenOption
import java.time.Clock
import java.time.Instant
import java.util.Base64
import java.security.MessageDigest
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import parker.core.interfaces.*

const val STANDING_EXTERNAL_TRANSCRIPTION_POLICY_VERSION = "standing-external-transcription-v1"

private fun standingPolicyId(value: String): String = MessageDigest.getInstance("SHA-256")
    .digest(value.toByteArray(StandardCharsets.UTF_8)).joinToString("") { "%02x".format(it) }

data class StandingExternalTranscriptionPolicy(
    val ownerPrincipalId: PrincipalId,
    val authorizationPurpose: AuthorizationPurposeId,
    val approvedAt: Instant,
    val policyVersion: String = STANDING_EXTERNAL_TRANSCRIPTION_POLICY_VERSION,
) {
    init {
        require(policyVersion == STANDING_EXTERNAL_TRANSCRIPTION_POLICY_VERSION)
        require(authorizationPurpose == ExternalTranscriptionInvocationGate.AUTHORIZATION_PURPOSE)
    }

    /** Stable identity of the exact persisted policy instance; timestamps are material. */
    val policyId: String
        get() = standingPolicyId(listOf(ownerPrincipalId.value, authorizationPurpose.value, approvedAt.toString(), policyVersion).joinToString("\u0000"))
}

/** Durable standing policy. The active file is usable only with its completed audit fact. */
class FileSystemStandingExternalTranscriptionPolicyStore(
    storageRoot: Path,
    private val auditReader: CaseGovernanceAuditReader?,
) {
    private val root = storageRoot.toAbsolutePath().normalize()
    private val active = root.resolve("standing-external-transcription-policy-v1.policy")
    private val staged = root.resolve(".standing-external-transcription-policy-v1.prepared")
    private val mutex = Mutex()

    init {
        require(Files.isDirectory(root) && Files.isReadable(root) && Files.isWritable(root))
    }

    suspend fun establish(policy: StandingExternalTranscriptionPolicy, audit: CaseGovernanceAudit): Boolean = mutex.withLock {
        val encoded = encode(policy)
        if (Files.exists(active)) {
            val existing = decode(Files.readAllBytes(active)) ?: return@withLock false
            if (existing.ownerPrincipalId != policy.ownerPrincipalId ||
                existing.authorizationPurpose != policy.authorizationPurpose ||
                existing.policyVersion != policy.policyVersion) return@withLock false
            // A completed compatible policy is already the standing decision. A retry may
            // carry a newly generated timestamp, but must remain idempotent and preserve the
            // durable policy instance that is already active.
            if (load() != null) return@withLock true
            audit.record(CaseGovernanceAuditRecord(
                CaseGovernanceAuditEventType.STANDING_EXTERNAL_TRANSCRIPTION_POLICY_AUTHORISED,
                null, actorPrincipalId = policy.ownerPrincipalId, recordedAt = policy.approvedAt,
                externalTranscriptionAuthorised = true, authorizationPurpose = policy.authorizationPurpose.value,
                policyVersion = policy.policyVersion,
                policyId = existing.policyId,
            ))
            return@withLock true
        }
        val temporary = Files.createTempFile(root, ".standing-policy-", ".tmp")
        try {
            FileChannel.open(temporary, StandardOpenOption.WRITE).use { channel ->
                channel.write(java.nio.ByteBuffer.wrap(encoded))
                channel.force(true)
            }
            Files.move(temporary, staged, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING)
        } finally {
            Files.deleteIfExists(temporary)
        }
        audit.record(CaseGovernanceAuditRecord(
            CaseGovernanceAuditEventType.STANDING_EXTERNAL_TRANSCRIPTION_POLICY_PREPARED,
            null, actorPrincipalId = policy.ownerPrincipalId, recordedAt = policy.approvedAt,
            externalTranscriptionAuthorised = true, authorizationPurpose = policy.authorizationPurpose.value,
            policyVersion = policy.policyVersion,
            policyId = policy.policyId,
        ))
        Files.move(staged, active, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING)
        audit.record(CaseGovernanceAuditRecord(
            CaseGovernanceAuditEventType.STANDING_EXTERNAL_TRANSCRIPTION_POLICY_AUTHORISED,
            null, actorPrincipalId = policy.ownerPrincipalId, recordedAt = policy.approvedAt,
            externalTranscriptionAuthorised = true, authorizationPurpose = policy.authorizationPurpose.value,
            policyVersion = policy.policyVersion,
            policyId = policy.policyId,
        ))
        true
    }

    fun load(): StandingExternalTranscriptionPolicy? {
        if (!Files.isRegularFile(active)) return null
        val policy = decode(Files.readAllBytes(active)) ?: return null
        val reader = auditReader ?: return null
        return if (reader.has(CaseGovernanceAuditQuery(
                CaseGovernanceAuditEventType.STANDING_EXTERNAL_TRANSCRIPTION_POLICY_AUTHORISED,
                null, externalTranscriptionAuthorised = true,
                authorizationPurpose = policy.authorizationPurpose.value, policyVersion = policy.policyVersion,
                policyId = policy.policyId,
            ))) policy else null
    }

    private fun encode(policy: StandingExternalTranscriptionPolicy): ByteArray = listOf(
        b64(policy.ownerPrincipalId.value), b64(policy.authorizationPurpose.value), policy.approvedAt.toString(), policy.policyVersion,
    ).joinToString("\t").toByteArray(StandardCharsets.UTF_8)

    private fun decode(bytes: ByteArray): StandingExternalTranscriptionPolicy? = runCatching {
        val fields = String(bytes, StandardCharsets.UTF_8).split('\t')
        require(fields.size == 4)
        StandingExternalTranscriptionPolicy(PrincipalId(unb64(fields[0])), AuthorizationPurposeId(unb64(fields[1])), Instant.parse(fields[2]), fields[3])
    }.getOrNull()

    private fun b64(value: String) = Base64.getUrlEncoder().withoutPadding().encodeToString(value.toByteArray(StandardCharsets.UTF_8))
    private fun unb64(value: String) = String(Base64.getUrlDecoder().decode(value), StandardCharsets.UTF_8)
}
