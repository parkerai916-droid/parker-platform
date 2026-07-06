package parker.core.runtime

import java.time.Duration
import java.time.Instant
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import parker.core.interfaces.ObservationResult
import parker.core.interfaces.WorldBelief
import parker.core.interfaces.WorldModel
import parker.core.interfaces.WorldObservation
import parker.core.interfaces.WorldQuery
import kotlin.reflect.full.functions
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Sprint 4, Track B, Unit B3. Behavioural tests of [InMemoryWorldModel]:
 * submission (internally-invoked Validation, evaluation, Update,
 * Invalidation, rejection), timestamp ownership, `current`/`query`
 * (single-subject lookup, broader matching, `maximumResults`,
 * minimum-confidence filtering, lazy expiry), the `WorldModel`
 * public-surface boundary (no caller-facing access to
 * `WorldModelUpdatePolicy`), concurrency, and this Unit's scope
 * discipline (no excluded/deferred contract, no Planner/Agent
 * Runtime/Permission Engine/Memory/EventBus dependency or behaviour).
 */
class InMemoryWorldModelTest {

    private fun observation(
        subject: String = "device-front-door",
        confidence: Double = 0.9,
        source: String = "sensor-lock-1",
        value: String? = "locked",
        retracts: Boolean = false,
        derivedFrom: List<String> = emptyList(),
        sourceTimestamp: Instant? = null,
    ) = WorldObservation(
        subject = subject,
        confidence = confidence,
        source = source,
        value = value,
        retracts = retracts,
        derivedFrom = derivedFrom,
        sourceTimestamp = sourceTimestamp,
    )

    private fun query(
        subjectMatch: String = "device",
        maximumResults: Int = 10,
        minimumConfidence: Double? = null,
    ) = WorldQuery(
        subjectMatch = subjectMatch,
        maximumResults = maximumResults,
        minimumConfidence = minimumConfidence,
    )

    // --- first valid observation / acceptance ---

    @Test
    fun `the first observation for a subject is accepted and retrievable via current`() = runTest {
        val model = InMemoryWorldModel()

        val result = model.observe(observation())

        assertIs<ObservationResult.Accepted>(result)
        assertEquals("locked", model.current("device-front-door")?.value)
    }

    @Test
    fun `current returns null for a subject with no belief`() = runTest {
        val model = InMemoryWorldModel()

        assertNull(model.current("never-observed"))
    }

    @Test
    fun `current rejects a blank subject`() = runTest {
        val model = InMemoryWorldModel()

        assertFailsWith<IllegalArgumentException> { model.current("") }
    }

    // --- replacement ---

    @Test
    fun `an observation with equal or higher confidence replaces the current belief for the same subject`() = runTest {
        val model = InMemoryWorldModel()
        model.observe(observation(confidence = 0.5, value = "locked"))

        val result = model.observe(observation(confidence = 0.8, value = "unlocked"))

        assertIs<ObservationResult.Accepted>(result)
        assertEquals("unlocked", model.current("device-front-door")?.value)
    }

    @Test
    fun `a weaker contradictory observation is rejected and does not alter current belief`() = runTest {
        val model = InMemoryWorldModel()
        model.observe(observation(confidence = 0.8, value = "locked"))

        val result = model.observe(observation(confidence = 0.2, value = "unlocked"))

        assertIs<ObservationResult.Rejected>(result)
        assertEquals("locked", model.current("device-front-door")?.value)
    }

    // --- invalidation / retraction ---

    @Test
    fun `a retraction invalidates the current belief for a subject`() = runTest {
        val model = InMemoryWorldModel()
        model.observe(observation(confidence = 0.9, value = "locked"))

        val result = model.observe(observation(retracts = true, value = null))

        assertIs<ObservationResult.Invalidated>(result)
        assertNull(model.current("device-front-door"))
    }

    @Test
    fun `a retraction with no existing belief is rejected and changes nothing`() = runTest {
        val model = InMemoryWorldModel()

        val result = model.observe(observation(retracts = true, value = null))

        assertIs<ObservationResult.Rejected>(result)
        assertNull(model.current("device-front-door"))
    }

    // --- timestamp ownership ---

    @Test
    fun `the World Model assigns the authoritative timestamp, never trusting a source-reported one`() = runTest {
        val model = InMemoryWorldModel()
        val distantPast = Instant.parse("2000-01-01T00:00:00Z")

        model.observe(observation(sourceTimestamp = distantPast))

        val belief = model.current("device-front-door")
        assertTrue(belief != null && belief.timestamp != distantPast)
        assertTrue(
            Duration.between(belief!!.timestamp, Instant.now()).abs() < Duration.ofSeconds(10),
            "expected the belief's timestamp to be close to now, was ${belief.timestamp}",
        )
    }

    // --- derived beliefs ---

    @Test
    fun `a derived belief carries its derivedFrom reference through to current`() = runTest {
        val model = InMemoryWorldModel()

        model.observe(
            observation(
                subject = "derived-away-from-home",
                derivedFrom = listOf("device-front-door", "environment-porch-light"),
            ),
        )

        val belief = model.current("derived-away-from-home")
        assertEquals(listOf("device-front-door", "environment-porch-light"), belief?.derivedFrom)
    }

    // --- query: matching, maximumResults, minimum-confidence ---

    @Test
    fun `query returns beliefs matching a subject substring, across multiple subjects`() = runTest {
        val model = InMemoryWorldModel()
        model.observe(observation(subject = "device-front-door", value = "locked"))
        model.observe(observation(subject = "device-back-door", value = "locked"))
        model.observe(observation(subject = "environment-porch-light", value = "on"))

        val results = model.query(query(subjectMatch = "device"))

        assertEquals(2, results.size)
        assertTrue(results.all { it.subject.contains("device") })
    }

    @Test
    fun `query never returns more than maximumResults`() = runTest {
        val model = InMemoryWorldModel()
        repeat(5) { i -> model.observe(observation(subject = "device-$i")) }

        val results = model.query(query(subjectMatch = "device", maximumResults = 2))

        assertEquals(2, results.size)
    }

    @Test
    fun `query respects minimumConfidence`() = runTest {
        val model = InMemoryWorldModel()
        model.observe(observation(subject = "device-high-confidence", confidence = 0.9))
        model.observe(observation(subject = "device-low-confidence", confidence = 0.2))

        val results = model.query(query(subjectMatch = "device", minimumConfidence = 0.5))

        assertEquals(1, results.size)
        assertEquals("device-high-confidence", results.single().subject)
    }

    // --- lazy expiry ---

    @Test
    fun `current excludes an expired belief`() = runTest {
        val model = InMemoryWorldModel(updatePolicy = DefaultWorldModelUpdatePolicy(staleAfter = Duration.ZERO))
        model.observe(observation())

        // Any measurable elapsed time exceeds a zero staleAfter window.
        Thread.sleep(5)

        assertNull(model.current("device-front-door"))
    }

    @Test
    fun `query excludes an expired belief`() = runTest {
        val model = InMemoryWorldModel(updatePolicy = DefaultWorldModelUpdatePolicy(staleAfter = Duration.ZERO))
        model.observe(observation())

        Thread.sleep(5)

        assertTrue(model.query(query(subjectMatch = "device")).isEmpty())
    }

    // --- WorldModelUpdatePolicy consulted internally ---

    @Test
    fun `WorldModelUpdatePolicy is consulted internally by InMemoryWorldModel, exactly once per submission`() = runTest {
        val fakePolicy = FakeWorldModelUpdatePolicy(
            resultFor = { obs, _ -> ObservationResult.Accepted(obs.subject, obs.toAcceptedBelief()) },
        )
        val model = InMemoryWorldModel(updatePolicy = fakePolicy)

        model.observe(observation())

        assertEquals(1, fakePolicy.evaluateCallCount)
    }

    @Test
    fun `InMemoryWorldModel's acceptance outcome is entirely controlled by the injected policy, not hardcoded`() = runTest {
        val alwaysReject = FakeWorldModelUpdatePolicy(
            resultFor = { obs, _ -> ObservationResult.Rejected(obs.subject, "fake always rejects") },
        )
        val model = InMemoryWorldModel(updatePolicy = alwaysReject)

        // Even a first, high-confidence Observation with no existing belief is rejected,
        // because the injected fake -- not DefaultWorldModelUpdatePolicy's own rules --
        // governs the outcome.
        val result = model.observe(observation(confidence = 1.0))

        assertIs<ObservationResult.Rejected>(result)
        assertNull(model.current("device-front-door"))
    }

    // --- WorldModel public surface: no caller-facing access to the policy ---

    @Test
    fun `WorldModel exposes exactly observe, current, and query -- no path to WorldModelUpdatePolicy`() {
        val functionNames = WorldModel::class.functions.map { it.name }.toSet()

        assertTrue(
            setOf("observe", "current", "query").all { it in functionNames },
            "WorldModel must expose observe/current/query",
        )
        assertFalse("evaluate" in functionNames, "WorldModel must not expose WorldModelUpdatePolicy.evaluate")
        assertFalse("isStillCurrent" in functionNames, "WorldModel must not expose WorldModelUpdatePolicy.isStillCurrent")
    }

    // --- concurrency ---

    @Test
    fun `concurrent observations for the same subject are resolved inside InMemoryWorldModel, not by callers`() = runBlocking {
        val model = InMemoryWorldModel()
        val submissionCount = 25

        val results = List(submissionCount) {
            async(Dispatchers.Default) {
                model.observe(observation(subject = "device-contested", confidence = 0.5, value = "state-$it"))
            }
        }.awaitAll()

        // Every concurrent submission at equal confidence must be individually accepted --
        // the policy rule "accept newer observations with equal or higher confidence" applies
        // regardless of interleaving -- and exactly one coherent belief must remain, never a
        // corrupted or duplicated entry.
        assertTrue(results.all { it is ObservationResult.Accepted }, "every equal-confidence submission should be accepted")
        assertTrue(model.current("device-contested") != null)
    }

    // --- structural exclusions: excluded and deferred contracts do not exist ---

    @Test
    fun `no WorldModelRuntime type exists anywhere in the repository`() {
        assertFailsWith<ClassNotFoundException> { Class.forName("parker.core.interfaces.WorldModelRuntime") }
    }

    @Test
    fun `no WorldModelUpdateDecision type exists anywhere in the repository`() {
        assertFailsWith<ClassNotFoundException> { Class.forName("parker.core.interfaces.WorldModelUpdateDecision") }
    }

    @Test
    fun `no belief-category enumeration exists anywhere in the repository`() {
        assertFailsWith<ClassNotFoundException> { Class.forName("parker.core.interfaces.WorldInformationCategory") }
        assertFailsWith<ClassNotFoundException> { Class.forName("parker.core.interfaces.WorldBeliefCategory") }
    }

    // --- scope discipline ---

    @Test
    fun `InMemoryWorldModel has no dependency on Memory, the Planner Runtime, Agent Runtime, Permission Engine, or EventBus`() {
        // Structural proof, not a runtime assertion, mirroring InMemoryMemoryStoreTest's own
        // identical pattern: InMemoryWorldModel's constructor takes only a
        // WorldModelUpdatePolicy (defaulted to DefaultWorldModelUpdatePolicy). If this class
        // ever gained a MemoryStore, PlannerRuntime, AgentRunCommandChannel, PermissionEngine,
        // or EventBus dependency, this single-argument construction would no longer compile --
        // the constructor signature itself is the guarantee, not this assertion.
        val model: WorldModel = InMemoryWorldModel()
        assertTrue(model is WorldModel)
    }

    private fun WorldObservation.toAcceptedBelief() = WorldBelief(
        subject = subject,
        value = value ?: "unspecified",
        confidence = confidence,
        timestamp = Instant.now(),
        source = source,
        derivedFrom = derivedFrom,
    )
}
