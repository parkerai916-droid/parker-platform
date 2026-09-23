package parker.core.runtime

import parker.core.interfaces.AuthorizationPurposeId
import parker.core.interfaces.EvidenceArtifactId
import parker.core.interfaces.OcrSha256Digest
import parker.core.interfaces.PrincipalId
import java.security.SecureRandom
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
}

sealed interface OwnerUnlockProofIssueResult {
    data class Issued(val proof: OwnerUnlockProof) : OwnerUnlockProofIssueResult
    data object Rejected : OwnerUnlockProofIssueResult
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
    val proofId: String,
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
) {
    private enum class State { ACTIVE, CONSUMED, BURNED, EXPIRED }
    private data class Entry(val proof: OwnerUnlockProof, var state: State)

    private val entries = mutableMapOf<String, Entry>()

    init {
        require(!ttl.isNegative && !ttl.isZero && ttl <= Duration.ofMinutes(5)) {
            "Owner unlock proof TTL must be between one millisecond and five minutes"
        }
    }

    @Synchronized
    fun issue(verification: OwnerPinVerificationResult, scope: OwnerUnlockProofScope): OwnerUnlockProofIssueResult {
        if (verification != OwnerPinVerificationResult.VERIFIED ||
            scope.operation != OwnerUnlockProofOperation.EXTERNAL_TRANSCRIPTION_AUTHORIZATION
        ) return OwnerUnlockProofIssueResult.Rejected
        cleanup(clock.instant())
        val issuedAt = clock.instant()
        val expiresAt = try {
            issuedAt.plus(ttl)
        } catch (_: DateTimeException) {
            return OwnerUnlockProofIssueResult.Unavailable
        }
        val id = OwnerUnlockProofId.fromTrustedValue(generateId())
        val proof = OwnerUnlockProof(id, scope, issuedAt, expiresAt)
        entries[id.value] = Entry(proof, State.ACTIVE)
        audit.record(OwnerUnlockProofAuditRecord(
            OwnerUnlockProofAuditEvent.OWNER_UNLOCK_PROOF_ISSUED,
            id.value, scope.principalId, issuedAt, "ISSUED",
        ))
        return OwnerUnlockProofIssueResult.Issued(proof)
    }

    @Synchronized
    fun consume(proofId: OwnerUnlockProofId, requestedScope: OwnerUnlockProofScope): OwnerUnlockProofConsumeResult {
        val now = clock.instant()
        cleanup(now)
        val entry = entries[proofId.value] ?: return OwnerUnlockProofConsumeResult.NOT_FOUND
        if (entry.state == State.CONSUMED || entry.state == State.BURNED) {
            audit.record(OwnerUnlockProofAuditRecord(
                OwnerUnlockProofAuditEvent.OWNER_UNLOCK_PROOF_REPLAY_REJECTED,
                proofId.value, entry.proof.scope.principalId, now, "REPLAY_REJECTED",
            ))
            return OwnerUnlockProofConsumeResult.REPLAY_REJECTED
        }
        if (entry.state == State.EXPIRED || !now.isBefore(entry.proof.expiresAt)) {
            entry.state = State.EXPIRED
            audit.record(OwnerUnlockProofAuditRecord(
                OwnerUnlockProofAuditEvent.OWNER_UNLOCK_PROOF_EXPIRED,
                proofId.value, entry.proof.scope.principalId, now, "EXPIRED",
            ))
            return OwnerUnlockProofConsumeResult.EXPIRED
        }
        if (now.isBefore(entry.proof.issuedAt)) return OwnerUnlockProofConsumeResult.UNAVAILABLE
        if (entry.proof.scope != requestedScope) {
            // A lookup followed by any scope attempt burns the proof, preventing probing with
            // alternate principals, evidence identities, purposes, or operations.
            entry.state = State.BURNED
            audit.record(OwnerUnlockProofAuditRecord(
                OwnerUnlockProofAuditEvent.OWNER_UNLOCK_PROOF_SCOPE_REJECTED,
                proofId.value, entry.proof.scope.principalId, now, "SCOPE_MISMATCH",
            ))
            return OwnerUnlockProofConsumeResult.SCOPE_MISMATCH
        }
        entry.state = State.CONSUMED
        audit.record(OwnerUnlockProofAuditRecord(
            OwnerUnlockProofAuditEvent.OWNER_UNLOCK_PROOF_CONSUMED,
            proofId.value, entry.proof.scope.principalId, now, "CONSUMED",
        ))
        return OwnerUnlockProofConsumeResult.CONSUMED
    }

    @Synchronized
    fun sizeForTests(): Int {
        cleanup(clock.instant())
        return entries.size
    }

    private fun cleanup(now: Instant) {
        val retention = try { ttl.multipliedBy(2) } catch (_: ArithmeticException) { ttl }
        entries.entries.removeIf { (_, entry) ->
            val retentionUntil = try { entry.proof.expiresAt.plus(retention) } catch (_: DateTimeException) { Instant.MAX }
            !now.isBefore(retentionUntil)
        }
    }

    private fun generateId(): String {
        val bytes = ByteArray(32)
        random.nextBytes(bytes)
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes)
    }
}
