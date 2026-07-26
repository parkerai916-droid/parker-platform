package parker.core.runtime

import kotlinx.coroutines.test.runTest
import parker.core.interfaces.CorrelationId
import parker.core.interfaces.InboundOwnerMessage
import parker.core.interfaces.ModuleId
import parker.core.interfaces.PlanningSessionId
import parker.core.interfaces.PrincipalId
import parker.core.interfaces.ReasoningProviderResponse
import parker.core.interfaces.RequestOrigin
import parker.core.interfaces.RequestPriority
import java.time.Instant
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertIs
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

/**
 * `GoalPlanningHandoffCoordinator` acceptance test, per
 * `docs/implementation/REASONING_TO_PLANNING_HANDOFF_SCOPE_LOCK.md`
 * Section 4. Exercises [GoalPlanningHandoffCoordinator] directly -- it
 * has exactly one dependency, [Function0]-typed, requiring no fake.
 */
class GoalPlanningHandoffCoordinatorTest {

    private val fixedTimestamp: Instant = Instant.parse("2026-01-01T00:00:00Z")
    private val channelId = ModuleId("channel.local-text")

    private fun message(
        text: String = "book a flight",
        correlationId: String = "corr-1",
        senderPrincipalId: String = "user-1",
    ) = InboundOwnerMessage(
        channelId = channelId,
        senderPrincipalId = PrincipalId(senderPrincipalId),
        text = text,
        timestamp = fixedTimestamp,
        correlationId = CorrelationId(correlationId),
    )

    private fun goal(text: String = "book a flight to Denver") = ReasoningProviderResponse.Goal(text)

    // ================= 1. Field-by-field PlanningRequest construction =================

    @Test
    fun `constructs a PlanningRequest with every field extracted or defaulted exactly as specified`() = runTest {
        val coordinator = GoalPlanningHandoffCoordinator(planningSessionIdFactory = { "fixed-id" })
        val msg = message(correlationId = "corr-7", senderPrincipalId = "user-42")
        val goalResponse = goal("water the plants")

        val outcome = coordinator.initiatePlanning(msg, goalResponse)

        val deferred = assertIs<GoalPlanningHandoffOutcome.Deferred>(outcome)
        assertEquals(PlanningSessionId("fixed-id"), deferred.planningRequest.planningSessionId)
        assertEquals(PrincipalId("user-42"), deferred.planningRequest.initiatingPrincipalId)
        assertEquals("corr-7", deferred.planningRequest.correlationId)
        assertEquals("water the plants", deferred.planningRequest.goal)
        assertEquals(RequestOrigin.TEXT, deferred.planningRequest.source)
        assertEquals(RequestPriority.NORMAL, deferred.planningRequest.priority)
    }

    // ================= 2. Reason and detail =================

    @Test
    fun `returns Deferred with the authoritative CANDIDATE_GENERATION_UNAVAILABLE reason and a non-blank, non-authoritative detail`() = runTest {
        val coordinator = GoalPlanningHandoffCoordinator(planningSessionIdFactory = { "fixed-id" })

        val outcome = coordinator.initiatePlanning(message(), goal())

        val deferred = assertIs<GoalPlanningHandoffOutcome.Deferred>(outcome)
        assertEquals(PlanningDeferralReason.CANDIDATE_GENERATION_UNAVAILABLE, deferred.reason)
        assertTrue(deferred.detail.isNotBlank())
        assertTrue("PlanCandidate" in deferred.detail)
    }

    // ================= 3. Exactly one invocation of the ID factory =================

    @Test
    fun `calls planningSessionIdFactory exactly once per initiatePlanning call`() = runTest {
        var callCount = 0
        val coordinator = GoalPlanningHandoffCoordinator(
            planningSessionIdFactory = {
                callCount++
                "id-$callCount"
            },
        )

        coordinator.initiatePlanning(message(), goal())

        assertEquals(1, callCount)
    }

    // ================= 4. Distinct IDs across separate handoffs =================

    @Test
    fun `two separate calls produce two different planningSessionId values`() = runTest {
        var counter = 0
        val coordinator = GoalPlanningHandoffCoordinator(
            planningSessionIdFactory = {
                counter++
                "id-$counter"
            },
        )

        val first = assertIs<GoalPlanningHandoffOutcome.Deferred>(
            coordinator.initiatePlanning(message(correlationId = "c1"), goal()),
        )
        val second = assertIs<GoalPlanningHandoffOutcome.Deferred>(
            coordinator.initiatePlanning(message(correlationId = "c2"), goal()),
        )

        assertNotEquals(first.planningRequest.planningSessionId, second.planningRequest.planningSessionId)
    }

    // ================= 5. Blank-ID failure =================

    @Test
    fun `a factory returning a blank string causes IllegalArgumentException`() = runTest {
        val coordinator = GoalPlanningHandoffCoordinator(planningSessionIdFactory = { "" })

        assertFailsWith<IllegalArgumentException> {
            coordinator.initiatePlanning(message(), goal())
        }
    }

    // ================= 6. Throwing-factory propagation =================

    @Test
    fun `an exception thrown by planningSessionIdFactory propagates unchanged`() = runTest {
        val coordinator = GoalPlanningHandoffCoordinator(
            planningSessionIdFactory = { throw IllegalStateException("factory boom") },
        )

        assertFailsWith<IllegalStateException> {
            coordinator.initiatePlanning(message(), goal())
        }
    }

    // ================= 7. Structural: constructor accepts exactly one dependency =================

    @Test
    fun `the coordinator's constructor accepts exactly one dependency`() {
        val constructor = GoalPlanningHandoffCoordinator::class.java.declaredConstructors.single()

        assertEquals(1, constructor.parameterCount)
        assertEquals("Function0", constructor.parameterTypes.single().simpleName)
    }

    // ================= 8. Statelessness =================

    @Test
    fun `the coordinator declares no field beyond its one constructor-injected dependency`() {
        val fieldNames = GoalPlanningHandoffCoordinator::class.java.declaredFields.map { it.name }.toSet()

        assertEquals(setOf("planningSessionIdFactory"), fieldNames)
    }
}
