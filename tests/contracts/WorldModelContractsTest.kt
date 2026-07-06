package parker.core.interfaces

import java.time.Instant
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

/**
 * Sprint 4, Track B, Unit B3. Construction-time validation tests for the
 * field-level World Model contracts
 * `docs/architecture/WORLD_MODEL_CONTRACT_DESIGN.md` approved:
 * [WorldBelief], [WorldObservation], [ObservationResult], and
 * [WorldQuery]. Behavioural tests of [WorldModel] and
 * [WorldModelUpdatePolicy] live in `tests/runtime/InMemoryWorldModelTest.kt`
 * and `tests/runtime/DefaultWorldModelUpdatePolicyTest.kt` instead --
 * this file is pure data-shape validation, mirroring
 * `MemoryContractsTest.kt`'s own scope.
 */
class WorldModelContractsTest {

    // --- WorldBelief ---

    private fun belief(
        subject: String = "device-front-door",
        value: String = "locked",
        confidence: Double = 0.9,
        timestamp: Instant = Instant.parse("2026-01-01T00:00:00Z"),
        source: String = "sensor-lock-1",
    ) = WorldBelief(
        subject = subject,
        value = value,
        confidence = confidence,
        timestamp = timestamp,
        source = source,
    )

    @Test
    fun `a WorldBelief with a blank subject is rejected`() {
        assertFailsWith<IllegalArgumentException> { belief(subject = "") }
    }

    @Test
    fun `a WorldBelief with a blank value is rejected`() {
        assertFailsWith<IllegalArgumentException> { belief(value = "") }
    }

    @Test
    fun `a WorldBelief with a blank source is rejected`() {
        assertFailsWith<IllegalArgumentException> { belief(source = "") }
    }

    @Test
    fun `a WorldBelief confidence outside 0-0 to 1-0 is rejected`() {
        assertFailsWith<IllegalArgumentException> { belief(confidence = 1.5) }
        assertFailsWith<IllegalArgumentException> { belief(confidence = -0.1) }
    }

    @Test
    fun `a WorldBelief with a valid confidence at each boundary is accepted`() {
        belief(confidence = 0.0)
        belief(confidence = 1.0)
    }

    @Test
    fun `a WorldBelief carries an optional derivedFrom reference`() {
        val derived = belief().copy(derivedFrom = listOf("device-front-door", "environment-porch-light"))
        assertEquals(listOf("device-front-door", "environment-porch-light"), derived.derivedFrom)
    }

    // --- WorldObservation ---

    private fun observation(
        subject: String = "device-front-door",
        confidence: Double = 0.9,
        source: String = "sensor-lock-1",
        value: String? = "locked",
        retracts: Boolean = false,
    ) = WorldObservation(
        subject = subject,
        confidence = confidence,
        source = source,
        value = value,
        retracts = retracts,
    )

    @Test
    fun `a WorldObservation with a blank subject is rejected`() {
        assertFailsWith<IllegalArgumentException> { observation(subject = "") }
    }

    @Test
    fun `a WorldObservation with a blank source is rejected`() {
        assertFailsWith<IllegalArgumentException> { observation(source = "") }
    }

    @Test
    fun `a WorldObservation confidence outside 0-0 to 1-0 is rejected`() {
        assertFailsWith<IllegalArgumentException> { observation(confidence = 1.1) }
        assertFailsWith<IllegalArgumentException> { observation(confidence = -0.5) }
    }

    @Test
    fun `a non-retracting WorldObservation with a blank or absent value is rejected`() {
        assertFailsWith<IllegalArgumentException> { observation(value = "") }
        assertFailsWith<IllegalArgumentException> { observation(value = null) }
    }

    @Test
    fun `a retracting WorldObservation with no value is accepted`() {
        observation(value = null, retracts = true)
    }

    @Test
    fun `a WorldObservation carries an optional source-reported timestamp distinct from any authoritative one`() {
        val sourceTimestamp = Instant.parse("2026-01-01T00:00:00Z")
        val obs = observation().copy(sourceTimestamp = sourceTimestamp)
        assertEquals(sourceTimestamp, obs.sourceTimestamp)
    }

    // --- ObservationResult ---

    @Test
    fun `ObservationResult Rejected with a blank reason is rejected`() {
        assertFailsWith<IllegalArgumentException> {
            ObservationResult.Rejected("device-front-door", "")
        }
    }

    @Test
    fun `ObservationResult Accepted, Invalidated, and Rejected all expose the same subject field`() {
        val b = belief()
        val accepted: ObservationResult = ObservationResult.Accepted("device-front-door", b)
        val invalidated: ObservationResult = ObservationResult.Invalidated("device-front-door")
        val rejected: ObservationResult = ObservationResult.Rejected("device-front-door", "stale")

        assertEquals("device-front-door", accepted.subject)
        assertEquals("device-front-door", invalidated.subject)
        assertEquals("device-front-door", rejected.subject)
    }

    // --- WorldQuery ---

    private fun worldQuery(
        subjectMatch: String = "device",
        maximumResults: Int = 10,
        minimumConfidence: Double? = null,
    ) = WorldQuery(
        subjectMatch = subjectMatch,
        maximumResults = maximumResults,
        minimumConfidence = minimumConfidence,
    )

    @Test
    fun `a WorldQuery with a blank subjectMatch is rejected`() {
        assertFailsWith<IllegalArgumentException> { worldQuery(subjectMatch = "") }
    }

    @Test
    fun `a WorldQuery with a non-positive maximumResults is rejected`() {
        assertFailsWith<IllegalArgumentException> { worldQuery(maximumResults = 0) }
        assertFailsWith<IllegalArgumentException> { worldQuery(maximumResults = -1) }
    }

    @Test
    fun `a WorldQuery with maximumResults of exactly 1 is accepted`() {
        worldQuery(maximumResults = 1)
    }

    @Test
    fun `a WorldQuery minimumConfidence outside 0-0 to 1-0 is rejected`() {
        assertFailsWith<IllegalArgumentException> { worldQuery(minimumConfidence = 1.2) }
        assertFailsWith<IllegalArgumentException> { worldQuery(minimumConfidence = -0.2) }
    }

    @Test
    fun `a WorldQuery with an absent minimumConfidence is accepted`() {
        worldQuery(minimumConfidence = null)
    }
}
