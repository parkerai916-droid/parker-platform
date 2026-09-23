package parker.core.runtime

import org.junit.jupiter.api.Test
import parker.core.interfaces.AuthorizationPurposeId
import parker.core.interfaces.EvidenceArtifactId
import parker.core.interfaces.OcrSha256Digest
import parker.core.interfaces.PrincipalId
import java.time.Clock
import java.time.Duration
import java.time.Instant
import java.time.ZoneOffset
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

class OwnerUnlockProofTest {
    private val principalA = PrincipalId("owner-a")
    private val principalB = PrincipalId("owner-b")
    private val purpose = OWNER_UNLOCK_EXTERNAL_TRANSCRIPTION_PURPOSE
    private val scope = scope(principalA)

    @Test
    fun `only verified PIN result can issue proof`() {
        val store = InMemoryOwnerUnlockProofStore()
        assertIs<OwnerUnlockProofIssueResult.Issued>(store.issue(OwnerPinVerificationResult.VERIFIED, scope))
        listOf(OwnerPinVerificationResult.REJECTED, OwnerPinVerificationResult.TEMPORARILY_LOCKED, OwnerPinVerificationResult.UNAVAILABLE)
            .forEach { assertEquals(OwnerUnlockProofIssueResult.Rejected, store.issue(it, scope)) }
    }

    @Test
    fun `exact scope consumes once and mismatch burns proof`() {
        val store = InMemoryOwnerUnlockProofStore()
        val issued = assertIs<OwnerUnlockProofIssueResult.Issued>(store.issue(OwnerPinVerificationResult.VERIFIED, scope))
        assertEquals(OwnerUnlockProofConsumeResult.CONSUMED, store.consume(issued.proof.id, scope))
        assertEquals(OwnerUnlockProofConsumeResult.REPLAY_REJECTED, store.consume(issued.proof.id, scope))

        val second = assertIs<OwnerUnlockProofIssueResult.Issued>(store.issue(OwnerPinVerificationResult.VERIFIED, scope))
        assertEquals(OwnerUnlockProofConsumeResult.SCOPE_MISMATCH, store.consume(second.proof.id, scope(principalB)))
        assertEquals(OwnerUnlockProofConsumeResult.REPLAY_REJECTED, store.consume(second.proof.id, scope))
    }

    @Test
    fun `source evidence principal purpose and operation are all bound`() {
        val store = InMemoryOwnerUnlockProofStore()
        val variants = listOf(
            scope(principalA, evidence = "evidence-2"),
            scope(principalA, sha = "b".repeat(64)),
            scope(principalB),
        )
        variants.forEach { variant ->
            val issued = assertIs<OwnerUnlockProofIssueResult.Issued>(store.issue(OwnerPinVerificationResult.VERIFIED, scope))
            assertEquals(OwnerUnlockProofConsumeResult.SCOPE_MISMATCH, store.consume(issued.proof.id, variant))
        }
        assertTrue(runCatching {
            OwnerUnlockProofScope(principalA, EvidenceArtifactId("evidence-1"), OcrSha256Digest("a".repeat(64)), purpose, OwnerUnlockProofOperation.CASE_EVIDENCE_MUTATION)
        }.isFailure)
    }

    @Test
    fun `wrong purpose cannot construct a supported proof scope`() {
        val wrong = runCatching {
            OwnerUnlockProofScope(
                principalA, EvidenceArtifactId("evidence-1"), OcrSha256Digest("a".repeat(64)),
                AuthorizationPurposeId("case.mutation"), OwnerUnlockProofOperation.EXTERNAL_TRANSCRIPTION_AUTHORIZATION,
            )
        }
        assertTrue(wrong.isFailure)
    }

    @Test
    fun `expired proof is rejected and not consumable`() {
        val clock = MutableClock(Instant.parse("2026-01-01T00:00:00Z"))
        val store = InMemoryOwnerUnlockProofStore(clock, Duration.ofMinutes(2))
        val issued = assertIs<OwnerUnlockProofIssueResult.Issued>(store.issue(OwnerPinVerificationResult.VERIFIED, scope))
        clock.advance(Duration.ofMinutes(2))
        assertEquals(OwnerUnlockProofConsumeResult.EXPIRED, store.consume(issued.proof.id, scope))
        assertEquals(OwnerUnlockProofConsumeResult.EXPIRED, store.consume(issued.proof.id, scope))
    }

    @Test
    fun `clock before issuance fails closed`() {
        val clock = MutableClock(Instant.parse("2026-01-01T00:00:00Z"))
        val store = InMemoryOwnerUnlockProofStore(clock)
        val issued = assertIs<OwnerUnlockProofIssueResult.Issued>(store.issue(OwnerPinVerificationResult.VERIFIED, scope))
        clock.set(Instant.parse("2025-12-31T23:59:59Z"))
        assertEquals(OwnerUnlockProofConsumeResult.UNAVAILABLE, store.consume(issued.proof.id, scope))
    }

    @Test
    fun `restart invalidates outstanding proof`() {
        val first = InMemoryOwnerUnlockProofStore()
        val issued = assertIs<OwnerUnlockProofIssueResult.Issued>(first.issue(OwnerPinVerificationResult.VERIFIED, scope))
        val restarted = InMemoryOwnerUnlockProofStore()
        assertEquals(OwnerUnlockProofConsumeResult.NOT_FOUND, restarted.consume(OwnerUnlockProofId.fromTrustedValue(issued.proof.id.value), scope))
    }

    @Test
    fun `concurrent consumption permits exactly one consumer`() {
        val store = InMemoryOwnerUnlockProofStore()
        val issued = assertIs<OwnerUnlockProofIssueResult.Issued>(store.issue(OwnerPinVerificationResult.VERIFIED, scope))
        val gate = CountDownLatch(1)
        val pool = Executors.newFixedThreadPool(8)
        val results = (1..8).map {
            pool.submit<OwnerUnlockProofConsumeResult> { gate.await(); store.consume(issued.proof.id, scope) }
        }
        gate.countDown()
        val outcomes = results.map { it.get() }
        pool.shutdown()
        assertEquals(1, outcomes.count { it == OwnerUnlockProofConsumeResult.CONSUMED })
        assertEquals(7, outcomes.count { it == OwnerUnlockProofConsumeResult.REPLAY_REJECTED })
    }

    @Test
    fun `proof IDs are opaque random and redacted`() {
        val store = InMemoryOwnerUnlockProofStore()
        val first = assertIs<OwnerUnlockProofIssueResult.Issued>(store.issue(OwnerPinVerificationResult.VERIFIED, scope)).proof
        val second = assertIs<OwnerUnlockProofIssueResult.Issued>(store.issue(OwnerPinVerificationResult.VERIFIED, scope)).proof
        assertNotEquals(first.id.value, second.id.value)
        assertEquals("OwnerUnlockProofId([REDACTED])", first.id.toString())
    }

    @Test
    fun `audit records are bounded and contain no PIN or source content`() {
        val audit = RecordingAudit()
        val store = InMemoryOwnerUnlockProofStore(audit = audit)
        val issued = assertIs<OwnerUnlockProofIssueResult.Issued>(store.issue(OwnerPinVerificationResult.VERIFIED, scope))
        store.consume(issued.proof.id, scope)
        assertTrue(audit.records.any { it.event == OwnerUnlockProofAuditEvent.OWNER_UNLOCK_PROOF_ISSUED })
        assertTrue(audit.records.any { it.event == OwnerUnlockProofAuditEvent.OWNER_UNLOCK_PROOF_CONSUMED })
        assertTrue(audit.records.all { it.proofFingerprint.length == 16 && it.reason.length <= 120 && "123456" !in it.reason && "source text" !in it.reason })
        assertTrue(audit.records.none { it.proofFingerprint == issued.proof.id.value })
    }

    @Test
    fun `capacity exhaustion fails closed and cleanup releases bounded tombstones`() {
        val clock = MutableClock(Instant.parse("2026-01-01T00:00:00Z"))
        val store = InMemoryOwnerUnlockProofStore(clock, Duration.ofMinutes(2), maximumEntries = 2, maximumOutstandingPerPrincipal = 2)
        val first = assertIs<OwnerUnlockProofIssueResult.Issued>(store.issue(OwnerPinVerificationResult.VERIFIED, scope))
        val second = assertIs<OwnerUnlockProofIssueResult.Issued>(store.issue(OwnerPinVerificationResult.VERIFIED, scope(principalB)))
        assertEquals(OwnerUnlockProofIssueResult.CapacityExceeded, store.issue(OwnerPinVerificationResult.VERIFIED, scope))
        assertEquals(OwnerUnlockProofConsumeResult.CONSUMED, store.consume(first.proof.id, scope))
        assertEquals(OwnerUnlockProofIssueResult.CapacityExceeded, store.issue(OwnerPinVerificationResult.VERIFIED, scope))
        clock.advance(Duration.ofMinutes(4).plusMillis(1))
        assertEquals(0, store.sizeForTests())
        assertIs<OwnerUnlockProofIssueResult.Issued>(store.issue(OwnerPinVerificationResult.VERIFIED, scope))
        assertEquals(OwnerUnlockProofConsumeResult.NOT_FOUND, store.consume(OwnerUnlockProofId.fromTrustedValue(first.proof.id.value), scope))
        assertEquals(OwnerUnlockProofConsumeResult.NOT_FOUND, store.consume(OwnerUnlockProofId.fromTrustedValue(second.proof.id.value), scope(principalB)))
    }

    @Test
    fun `audit failure fails closed and leaves proof unusable`() {
        val failingAudit = SwitchableAudit()
        val store = InMemoryOwnerUnlockProofStore(audit = failingAudit)
        val issued = assertIs<OwnerUnlockProofIssueResult.Issued>(store.issue(OwnerPinVerificationResult.VERIFIED, scope))
        failingAudit.fail = true
        assertEquals(OwnerUnlockProofConsumeResult.UNAVAILABLE, store.consume(issued.proof.id, scope))
        failingAudit.fail = false
        assertEquals(OwnerUnlockProofConsumeResult.REPLAY_REJECTED, store.consume(issued.proof.id, scope))
    }

    private fun scope(
        principal: PrincipalId,
        evidence: String = "evidence-1",
        sha: String = "a".repeat(64),
        operation: OwnerUnlockProofOperation = OwnerUnlockProofOperation.EXTERNAL_TRANSCRIPTION_AUTHORIZATION,
    ) = OwnerUnlockProofScope(principal, EvidenceArtifactId(evidence), OcrSha256Digest(sha), purpose, operation)

    private class RecordingAudit : OwnerUnlockProofAudit {
        val records = mutableListOf<OwnerUnlockProofAuditRecord>()
        override fun record(record: OwnerUnlockProofAuditRecord) { synchronized(records) { records += record } }
    }

    private class SwitchableAudit : OwnerUnlockProofAudit {
        var fail = false
        override fun record(record: OwnerUnlockProofAuditRecord) { if (fail) error("audit unavailable") }
    }

    private class MutableClock(private var value: Instant) : Clock() {
        override fun getZone() = ZoneOffset.UTC
        override fun withZone(zone: java.time.ZoneId) = this
        override fun instant() = value
        fun advance(duration: Duration) { value = value.plus(duration) }
        fun set(next: Instant) { value = next }
    }
}
