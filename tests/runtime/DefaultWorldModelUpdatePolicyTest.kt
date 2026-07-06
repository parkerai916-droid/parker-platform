package parker.core.runtime

import java.time.Duration
import java.time.Instant
import kotlinx.coroutines.test.runTest
import parker.core.interfaces.ObservationResult
import parker.core.interfaces.WorldBelief
import parker.core.interfaces.WorldObservation
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

/**
 * Sprint 4, Track B, Unit B3. Unit tests of [DefaultWorldModelUpdatePolicy]'s
 * own four rules, stated in full in that class's KDoc: a retraction
 * invalidates an existing belief or is rejected if none exists; the
 * first valid Observation for a subject is always accepted; a later
 * Observation is accepted only at or above the existing belief's
 * confidence; and [DefaultWorldModelUpdatePolicy.isStillCurrent] is a
 * separate, read-time staleness check. Deliberately does not test any
 * confidence-weighted blending or sensor fusion -- this policy does not
 * implement it, per its own KDoc.
 */
class DefaultWorldModelUpdatePolicyTest {

    private val policy = DefaultWorldModelUpdatePolicy()

    private fun observation(
        subject: String = "device-front-door",
        confidence: Double = 0.9,
        source: String = "sensor-lock-1",
        value: String? = "locked",
        retracts: Boolean = false,
        derivedFrom: List<String> = emptyList(),
    ) = WorldObservation(
        subject = subject,
        confidence = confidence,
        source = source,
        value = value,
        retracts = retracts,
        derivedFrom = derivedFrom,
    )

    private fun existingBelief(
        subject: String = "device-front-door",
        confidence: Double = 0.5,
        timestamp: Instant = Instant.now(),
    ) = WorldBelief(
        subject = subject,
        value = "unlocked",
        confidence = confidence,
        timestamp = timestamp,
        source = "sensor-lock-1",
    )

    // --- first valid observation ---

    @Test
    fun `the first observation for a subject with no existing belief is accepted`() = runTest {
        val result = policy.evaluate(observation(), existing = null)

        val accepted = assertIs<ObservationResult.Accepted>(result)
        assertEquals("device-front-door", accepted.belief.subject)
        assertEquals("locked", accepted.belief.value)
        assertEquals(0.9, accepted.belief.confidence)
    }

    // --- confidence comparison ---

    @Test
    fun `an observation with higher confidence than the existing belief is accepted`() = runTest {
        val result = policy.evaluate(observation(confidence = 0.8), existing = existingBelief(confidence = 0.5))

        assertIs<ObservationResult.Accepted>(result)
    }

    @Test
    fun `an observation with equal confidence to the existing belief is accepted`() = runTest {
        val result = policy.evaluate(observation(confidence = 0.5), existing = existingBelief(confidence = 0.5))

        assertIs<ObservationResult.Accepted>(result)
    }

    @Test
    fun `a weaker contradictory observation is rejected, not silently discarded`() = runTest {
        val result = policy.evaluate(observation(confidence = 0.2), existing = existingBelief(confidence = 0.5))

        val rejected = assertIs<ObservationResult.Rejected>(result)
        assertTrue(rejected.reason.isNotBlank())
        assertTrue(rejected.reason.contains("confidence", ignoreCase = true))
    }

    @Test
    fun `the accepted belief carries forward the observation's value, source, and derivedFrom`() = runTest {
        val result = policy.evaluate(
            observation(value = "ajar", source = "sensor-lock-2", derivedFrom = listOf("device-front-door-camera")),
            existing = null,
        )

        val accepted = assertIs<ObservationResult.Accepted>(result)
        assertEquals("ajar", accepted.belief.value)
        assertEquals("sensor-lock-2", accepted.belief.source)
        assertEquals(listOf("device-front-door-camera"), accepted.belief.derivedFrom)
    }

    // --- retraction ---

    @Test
    fun `a retraction invalidates an existing belief`() = runTest {
        val result = policy.evaluate(
            observation(retracts = true, value = null),
            existing = existingBelief(),
        )

        val invalidated = assertIs<ObservationResult.Invalidated>(result)
        assertEquals("device-front-door", invalidated.subject)
    }

    @Test
    fun `a retraction with no existing belief is rejected`() = runTest {
        val result = policy.evaluate(observation(retracts = true, value = null), existing = null)

        val rejected = assertIs<ObservationResult.Rejected>(result)
        assertTrue(rejected.reason.contains("no current belief", ignoreCase = true))
    }

    // --- determinism ---

    @Test
    fun `evaluating the same rejected input twice yields an identical decision`() = runTest {
        val input = observation(confidence = 0.1)
        val existing = existingBelief(confidence = 0.9)

        val first = policy.evaluate(input, existing)
        val second = policy.evaluate(input, existing)

        assertEquals(first, second)
    }

    @Test
    fun `evaluating the same accepted input twice yields the same subject, value, and confidence`() = runTest {
        val input = observation(confidence = 0.9)

        val first = assertIs<ObservationResult.Accepted>(policy.evaluate(input, existing = null))
        val second = assertIs<ObservationResult.Accepted>(policy.evaluate(input, existing = null))

        assertEquals(first.belief.subject, second.belief.subject)
        assertEquals(first.belief.value, second.belief.value)
        assertEquals(first.belief.confidence, second.belief.confidence)
    }

    // --- isStillCurrent ---

    @Test
    fun `a freshly accepted belief is still current`() = runTest {
        val fresh = existingBelief(timestamp = Instant.now())

        assertTrue(policy.isStillCurrent(fresh))
    }

    @Test
    fun `a belief older than staleAfter is no longer current`() = runTest {
        val old = existingBelief(timestamp = Instant.now().minus(Duration.ofHours(1)))

        assertTrue(!policy.isStillCurrent(old))
    }

    @Test
    fun `a custom staleAfter duration is honoured`() = runTest {
        val strictPolicy = DefaultWorldModelUpdatePolicy(staleAfter = Duration.ofMillis(1))
        val slightlyOld = existingBelief(timestamp = Instant.now().minus(Duration.ofSeconds(1)))

        assertTrue(!strictPolicy.isStillCurrent(slightlyOld))
    }
}
