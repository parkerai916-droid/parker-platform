package parker.core.runtime

import java.time.Instant
import parker.core.interfaces.HermesProcessingCorrection
import parker.core.interfaces.HermesProcessingHumanDecision
import parker.core.interfaces.HermesProcessingHumanDecisionType
import parker.core.interfaces.PrincipalId
import kotlin.test.Test
import kotlin.test.assertFailsWith

/** Hermes Exception Decision Backend, Task 4. Domain-type invariants for the new decision/correction types. */
class HermesProcessingHumanDecisionTest {

    private val sha256 = "a".repeat(64)
    private val batchId = "bulk-a1b2c3d4-0000-0000-0000-000000000000"
    private val decidedBy = PrincipalId("owner-${"1".repeat(64)}")
    private val now: Instant = Instant.parse("2026-01-01T00:00:00Z")

    private fun decision(
        type: HermesProcessingHumanDecisionType,
        correction: HermesProcessingCorrection? = null,
        reason: String? = null,
    ) = HermesProcessingHumanDecision(batchId, sha256, type, decidedBy, now, reason, correction)

    @Test
    fun `ACCEPT, REPROCESS, and REJECT construct cleanly with no correction`() {
        decision(HermesProcessingHumanDecisionType.ACCEPT)
        decision(HermesProcessingHumanDecisionType.REPROCESS)
        decision(HermesProcessingHumanDecisionType.REJECT)
    }

    @Test
    fun `CORRECT requires a correction`() {
        assertFailsWith<IllegalArgumentException> { decision(HermesProcessingHumanDecisionType.CORRECT, correction = null) }
    }

    @Test
    fun `a correction is rejected for every decision type other than CORRECT`() {
        val correction = HermesProcessingCorrection(0, "corrected text", "because")
        assertFailsWith<IllegalArgumentException> { decision(HermesProcessingHumanDecisionType.ACCEPT, correction) }
        assertFailsWith<IllegalArgumentException> { decision(HermesProcessingHumanDecisionType.REPROCESS, correction) }
        assertFailsWith<IllegalArgumentException> { decision(HermesProcessingHumanDecisionType.REJECT, correction) }
    }

    @Test
    fun `blank batchId is rejected`() {
        assertFailsWith<IllegalArgumentException> {
            HermesProcessingHumanDecision("", sha256, HermesProcessingHumanDecisionType.REJECT, decidedBy, now)
        }
    }

    @Test
    fun `a malformed sourceSha256 is rejected`() {
        assertFailsWith<IllegalArgumentException> {
            HermesProcessingHumanDecision(batchId, "not-a-hash", HermesProcessingHumanDecisionType.REJECT, decidedBy, now)
        }
    }

    @Test
    fun `a blank reason is rejected -- omit it entirely instead`() {
        assertFailsWith<IllegalArgumentException> { decision(HermesProcessingHumanDecisionType.REJECT, reason = "   ") }
    }

    @Test
    fun `HermesProcessingCorrection rejects a negative issueIndex`() {
        assertFailsWith<IllegalArgumentException> { HermesProcessingCorrection(-1, "text", "reason") }
    }

    @Test
    fun `HermesProcessingCorrection rejects a blank correctedInterpretation or reason`() {
        assertFailsWith<IllegalArgumentException> { HermesProcessingCorrection(0, "", "reason") }
        assertFailsWith<IllegalArgumentException> { HermesProcessingCorrection(0, "text", "") }
    }
}
