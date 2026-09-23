package parker.core.runtime

import parker.core.interfaces.AuthorizationPurposeId
import parker.core.interfaces.EvidenceArtifactId
import parker.core.interfaces.OcrSha256Digest
import parker.core.interfaces.PrincipalId
import java.security.SecureRandom
import java.security.MessageDigest
import java.time.Clock
import java.time.Duration
import java.time.Instant
import java.time.DateTimeException
import java.util.Base64

/** The only Unit 2 operation currently eligible for PIN unlock. */
enum class OwnerUnlockProofOperation {
    EXTERNAL_TRANSCRIPTION_AUTHORIZATION,
    CASE_EVIDENCE_MUTATION,
}

/** Existing governed purpose; no free-form authorization purpose is accepted here. */
val OWNER_UNLOCK_EXTERNAL_TRANSCRIPTION_PURPOSE: AuthorizationPurposeId =
    ExternalTranscriptionInvocationGate.AUTHORIZATION_PURPOSE

@JvmInline
value class OwnerUnlockProofId private constructor(internal val value: String) {
    override fun toString(): String = "OwnerUnlockProofId([REDACTED])"

    companion object {
        /** Internal reconstruction only; no browser or external caller should receive this type. */
        internal fun fromTrustedValue(value: String): OwnerUnlockProofId {
            require(value.matches(Regex("^[A-Za-z0-9_-]{43}$"))) { "Invalid Owner unlock proof ID" }
            return OwnerUnlockProofId(value)
        }
    }
}

data class OwnerUnlockProofScope(
    val principalId: PrincipalId,
    val evidenceArtifactId: EvidenceArtifactId,
    val sourceSha256: OcrSha256Digest,
    val authorizationPurpose: AuthorizationPurposeId,
    val operation: OwnerUnlockProofOperation,
) {
    init {
        require(authorizationPurpose == OWNER_UNLOCK_EXTERNAL_TRANSCRIPTION_PURPOSE) {
            "Owner PIN unlock purpose is not supported"
        }
        require(operation == OwnerUnlockProofOperation.EXTERNAL_TRANSCRIPTION_AUTHORIZATION) {
            "Owner PIN unlock operation is not supported"
        }
    }
}

data class OwnerUnlockProof(
    val id: OwnerUnlockProofId,
    val scope: OwnerUnlockProofScope,
    val issuedAt: Instant,
    val expiresAt: Instant,
) {
    init {
        require(!expiresAt.isBefore(issuedAt)) { "Owner unlock proof expiry precedes issuance" }
    }

    override fun toString(): String =
        "OwnerUnlockProof(scope=$scope, issuedAt=$issuedAt, expiresAt=$expiresAt)"
}

sealed interface OwnerUnlockProofIssueResult {
    data class Issued(val proof: OwnerUnlockProof) : OwnerUnlockProofIssueResult
    data object Rejected : OwnerUnlockProofIssueResult
    data object CapacityExceeded : OwnerUnlockProofIssueResult
    data object Unavailable : OwnerUnlockProofIssueResult
}

enum class OwnerUnlockProofConsumeResult {
    CONSUMED,
    EXPIRED,
    REPLAY_REJECTED,
    SCOPE_MISMATCH,
    NOT_FOUND,
    UNAVAILABLE,
}

enum class OwnerUnlockProofAuditEvent {
    OWNER_UNLOCK_PROOF_ISSUED,
    OWNER_UNLOCK_PROOF_CONSUMED,
    OWNER_UNLOCK_PROOF_EXPIRED,
    OWNER_UNLOCK_PROOF_REPLAY_REJECTED,
    OWNER_UNLOCK_PROOF_SCOPE_REJECTED,
}

data class OwnerUnlockProofAuditRecord(
    val event: OwnerUnlockProofAuditEvent,
    val proofFingerprint: String,
    val principalId: PrincipalId,
    val occurredAt: Instant,
    val reason: String,
)

fun interface OwnerUnlockProofAudit {
    fun record(record: OwnerUnlockProofAuditRecord)
}

object NoOpOwnerUnlockProofAudit : OwnerUnlockProofAudit {
    override fun record(record: OwnerUnlockProofAuditRecord) = Unit
}

/**
 * Server-only, restart-invalidating proof store. The opaque ID never needs to leave this
 * process: Unit 3 will pass the typed reference internally after server-side PIN verification.
 */
class InMemoryOwnerUnlockProofStore(
    private val clock: Clock = Clock.systemUTC(),
    private val ttl: Duration = Duration.ofMinutes(2),
    private val audit: OwnerUnlockProofAudit = NoOpOwnerUnlockProofAudit,
    private val random: SecureRandom = SecureRandom(),
    private val maximumEntries: Int = 256,
    private val maximumOutstandingPerPrincipal: Int = 8,
) {
    private enum class State { ACTIVE, CONSUMED, BURNED, EXPIRED }
    private data class Entry(val proof: OwnerUnlockProof, var state: State)

    private val entries = mutableMapOf<String, Entry>()

    init {
        require(!ttl.isNegative && !ttl.isZero && ttl <= Duration.ofMinutes(5)) {
            "Owner unlock proof TTL must be between one millisecond and five minutes"
        }
        require(maximumEntries > 0 && maximumOutstandingPerPrincipal > 0)
    }

    @Synchronized
    fun issue(verification: OwnerPinVerificationResult, scope: OwnerUnlockProofScope): OwnerUnlockProofIssueResult {
        if (verification != OwnerPinVerificationResult.VERIFIED ||
            scope.operation != OwnerUnlockProofOperation.EXTERNAL_TRANSCRIPTION_AUTHORIZATION
        ) return OwnerUnlockProofIssueResult.Rejected
        val now = clock.instant()
        cleanup(now)
        if (entries.size >= maximumEntries || entries.values.count { it.state == State.ACTIVE && it.proof.scope.principalId == scope.principalId } >= maximumOutstandingPerPrincipal) {
            return OwnerUnlockProofIssueResult.CapacityExceeded
        }
        val issuedAt = now
        val expiresAt = try {
            issuedAt.plus(ttl)
        } catch (_: DateTimeException) {
            return OwnerUnlockProofIssueResult.Unavailable
        }
        val id = try { OwnerUnlockProofId.fromTrustedValue(generateId()) } catch (_: RuntimeException) {
            return OwnerUnlockProofIssueResult.Unavailable
        }
        val proof = OwnerUnlockProof(id, scope, issuedAt, expiresAt)
        entries[id.value] = Entry(proof, State.ACTIVE)
        if (!auditSafely(OwnerUnlockProofAuditRecord(
            OwnerUnlockProofAuditEvent.OWNER_UNLOCK_PROOF_ISSUED,
            fingerprint(id), scope.principalId, issuedAt, "ISSUED",
        ))) {
            entries.remove(id.value)
            return OwnerUnlockProofIssueResult.Unavailable
        }
        return OwnerUnlockProofIssueResult.Issued(proof)
    }

    @Synchronized
    fun consume(proofId: OwnerUnlockProofId, requestedScope: OwnerUnlockProofScope): OwnerUnlockProofConsumeResult {
        val now = clock.instant()
        cleanup(now)
        val entry = entries[proofId.value] ?: return OwnerUnlockProofConsumeResult.NOT_FOUND
        if (entry.state == State.CONSUMED || entry.state == State.BURNED) {
            return if (auditSafely(OwnerUnlockProofAuditRecord(
                OwnerUnlockProofAuditEvent.OWNER_UNLOCK_PROOF_REPLAY_REJECTED,
                fingerprint(proofId), entry.proof.scope.principalId, now, "REPLAY_REJECTED",
            ))) OwnerUnlockProofConsumeResult.REPLAY_REJECTED else OwnerUnlockProofConsumeResult.UNAVAILABLE
        }
        if (entry.state == State.EXPIRED || !now.isBefore(entry.proof.expiresAt)) {
            entry.state = State.EXPIRED
            return if (auditSafely(OwnerUnlockProofAuditRecord(
                OwnerUnlockProofAuditEvent.OWNER_UNLOCK_PROOF_EXPIRED,
                fingerprint(proofId), entry.proof.scope.principalId, now, "EXPIRED",
            ))) OwnerUnlockProofConsumeResult.EXPIRED else OwnerUnlockProofConsumeResult.UNAVAILABLE
        }
        if (now.isBefore(entry.proof.issuedAt)) return OwnerUnlockProofConsumeResult.UNAVAILABLE
        if (entry.proof.scope != requestedScope) {
            // A lookup followed by any scope attempt burns the proof, preventing probing with
            // alternate principals, evidence identities, purposes, or operations.
            entry.state = State.BURNED
            return if (auditSafely(OwnerUnlockProofAuditRecord(
                OwnerUnlockProofAuditEvent.OWNER_UNLOCK_PROOF_SCOPE_REJECTED,
                fingerprint(proofId), entry.proof.scope.principalId, now, "SCOPE_MISMATCH",
            ))) OwnerUnlockProofConsumeResult.SCOPE_MISMATCH else OwnerUnlockProofConsumeResult.UNAVAILABLE
        }
        entry.state = State.CONSUMED
        return if (auditSafely(OwnerUnlockProofAuditRecord(
            OwnerUnlockProofAuditEvent.OWNER_UNLOCK_PROOF_CONSUMED,
            fingerprint(proofId), entry.proof.scope.principalId, now, "CONSUMED",
        ))) OwnerUnlockProofConsumeResult.CONSUMED else OwnerUnlockProofConsumeResult.UNAVAILABLE
    }

    @Synchronized
    fun sizeForTests(): Int {
        cleanup(clock.instant())
        return entries.size
    }

    private fun cleanup(now: Instant) {
        entries.entries.removeIf { (_, entry) ->
            if (entry.state == State.ACTIVE && !now.isBefore(entry.proof.expiresAt)) {
                entry.state = State.EXPIRED
            }
            val retentionUntil = try { entry.proof.expiresAt.plus(ttl) } catch (_: DateTimeException) { Instant.MAX }
            entry.state != State.ACTIVE && !now.isBefore(retentionUntil)
        }
    }

    private fun auditSafely(record: OwnerUnlockProofAuditRecord): Boolean =
        runCatching { audit.record(record) }.isSuccess

    private fun fingerprint(id: OwnerUnlockProofId): String =
        MessageDigest.getInstance("SHA-256").digest(id.value.toByteArray(Charsets.US_ASCII))
            .joinToString("") { "%02x".format(it.toInt() and 255) }.take(16)

    private fun generateId(): String {
        val bytes = ByteArray(32)
        random.nextBytes(bytes)
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes)
    }
}
