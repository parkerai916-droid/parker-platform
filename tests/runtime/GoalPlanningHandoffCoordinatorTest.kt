package parker.core.runtime

import kotlinx.coroutines.test.runTest
import parker.core.interfaces.CorrelationId
import parker.core.interfaces.InboundOwnerMessage
import parker.core.interfaces.ModuleId
import parker.core.interfaces.PlanCandidate
import parker.core.interfaces.PlanCandidateGenerator
import parker.core.interfaces.PlanCandidateId
import parker.core.interfaces.PlannerRuntime
import parker.core.interfaces.PlanningRequest
import parker.core.interfaces.PlanningSessionId
import parker.core.interfaces.PlanningSessionResult
import parker.core.interfaces.PrincipalId
import parker.core.interfaces.ReasoningProviderResponse
import parker.core.interfaces.TaskId
import parker.core.interfaces.TaskProposalDisposition
import parker.core.interfaces.TaskProposalId
import java.time.Instant
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertIs
import kotlin.test.assertSame
import kotlin.test.assertTrue

/**
 * `GoalPlanningHandoffCoordinator` acceptance test, per
 * `docs/implementation/PLAN_CANDIDATE_TO_PLANNER_INTEGRATION_SCOPE_LOCK.md`
 * Section 13. Replaces this file's own prior revision entirely -- the
 * coordinator's `Deferred`-only behaviour no longer exists. Exercises
 * [GoalPlanningHandoffCoordinator] against two hand-written fakes
 * ([FakePlanCandidateGenerator], [FakePlannerRuntime]); no mocking
 * framework is introduced.
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

    private fun candidate(id: String = "cand-1", goalText: String = "book a flight to Denver") = PlanCandidate(
        planCandidateId = PlanCandidateId(id),
        goal = goalText,
    )

    private fun completedResult(planningSessionId: PlanningSessionId) = PlanningSessionResult.Completed(
        planningSessionId = planningSessionId,
        taskProposalId = TaskProposalId("${planningSessionId.value}-proposal-1"),
        disposition = TaskProposalDisposition.Accepted(
            taskProposalId = TaskProposalId("${planningSessionId.value}-proposal-1"),
            taskId = TaskId("task-1"),
        ),
        rejections = emptyList(),
    )

    private fun rejectedResult(planningSessionId: PlanningSessionId) = PlanningSessionResult.Rejected(
        planningSessionId = planningSessionId,
        taskProposalId = TaskProposalId("${planningSessionId.value}-proposal-1"),
        disposition = TaskProposalDisposition.Rejected(
            taskProposalId = TaskProposalId("${planningSessionId.value}-proposal-1"),
            reason = "owner unavailable",
        ),
        rejections = emptyList(),
    )

    private fun failedResult(planningSessionId: PlanningSessionId) = PlanningSessionResult.Failed(
        planningSessionId = planningSessionId,
        reason = "no Plan Candidates were supplied for this Planning Session",
        rejections = emptyList(),
    )

    /** Captures its single [generate] call's argument and call count; returns a caller-configured list, or throws a caller-configured exception. */
    private class FakePlanCandidateGenerator(
        private val onGenerate: (PlanningRequest) -> List<PlanCandidate> = { emptyList() },
    ) : PlanCandidateGenerator {
        var callCount = 0
            private set
        var lastRequest: PlanningRequest? = null
            private set

        override suspend fun generate(request: PlanningRequest): List<PlanCandidate> {
            callCount++
            lastRequest = request
            return onGenerate(request)
        }
    }

    /** Captures its single [plan] call's arguments and call count; returns a caller-configured result, or throws a caller-configured exception. */
    private class FakePlannerRuntime(
        private val onPlan: (PlanningRequest, List<PlanCandidate>) -> PlanningSessionResult,
    ) : PlannerRuntime {
        var callCount = 0
            private set
        var lastRequest: PlanningRequest? = null
            private set
        var lastCandidates: List<PlanCandidate>? = null
            private set

        override suspend fun plan(request: PlanningRequest, candidates: List<PlanCandidate>): PlanningSessionResult {
            callCount++
            lastRequest = request
            lastCandidates = candidates
            return onPlan(request, candidates)
        }
    }

    private class ThrowingPlanCandidateGenerator(private val exception: Throwable) : PlanCandidateGenerator {
        override suspend fun generate(request: PlanningRequest): List<PlanCandidate> = throw exception
    }

    private class ThrowingPlannerRuntime(private val exception: Throwable) : PlannerRuntime {
        override suspend fun plan(request: PlanningRequest, candidates: List<PlanCandidate>): PlanningSessionResult = throw exception
    }

    private fun coordinator(
        planningSessionIdFactory: () -> String = { "fixed-id" },
        planCandidateGenerator: PlanCandidateGenerator = FakePlanCandidateGenerator(),
        plannerRuntime: PlannerRuntime = FakePlannerRuntime { request, _ -> completedResult(request.planningSessionId) },
    ) = GoalPlanningHandoffCoordinator(planningSessionIdFactory, planCandidateGenerator, plannerRuntime)

    // ================= 1. Generator called exactly once =================

    @Test
    fun `generator is called exactly once`() = runTest {
        val generator = FakePlanCandidateGenerator()
        val c = coordinator(planCandidateGenerator = generator)

        c.initiatePlanning(message(), goal())

        assertEquals(1, generator.callCount)
    }

    // ================= 2. Planner called exactly once =================

    @Test
    fun `planner is called exactly once`() = runTest {
        val planner = FakePlannerRuntime { request, _ -> completedResult(request.planningSessionId) }
        val c = coordinator(plannerRuntime = planner)

        c.initiatePlanning(message(), goal())

        assertEquals(1, planner.callCount)
    }

    // ================= 3. PlanningRequest constructed once, field-by-field =================

    @Test
    fun `constructs a PlanningRequest with every field extracted or defaulted exactly as specified`() = runTest {
        val generator = FakePlanCandidateGenerator()
        val c = coordinator(planningSessionIdFactory = { "fixed-id" }, planCandidateGenerator = generator)
        val msg = message(correlationId = "corr-7", senderPrincipalId = "user-42")

        c.initiatePlanning(msg, goal("water the plants"))

        val request = generator.lastRequest!!
        assertEquals(PlanningSessionId("fixed-id"), request.planningSessionId)
        assertEquals(PrincipalId("user-42"), request.initiatingPrincipalId)
        assertEquals("corr-7", request.correlationId)
        assertEquals("water the plants", request.goal)
    }

    // ================= 4. Same PlanningRequest instance passed to both dependencies =================

    @Test
    fun `the same PlanningRequest instance is passed to both the generator and the planner`() = runTest {
        val generator = FakePlanCandidateGenerator()
        val planner = FakePlannerRuntime { request, _ -> completedResult(request.planningSessionId) }
        val c = coordinator(planCandidateGenerator = generator, plannerRuntime = planner)

        c.initiatePlanning(message(), goal())

        assertSame(generator.lastRequest, planner.lastRequest)
    }

    // ================= 5. Candidate list passed unchanged =================

    @Test
    fun `the candidate list returned by the generator is passed unchanged to the planner`() = runTest {
        val candidates = listOf(candidate("cand-1"), candidate("cand-2"))
        val generator = FakePlanCandidateGenerator { candidates }
        val planner = FakePlannerRuntime { request, _ -> completedResult(request.planningSessionId) }
        val c = coordinator(planCandidateGenerator = generator, plannerRuntime = planner)

        c.initiatePlanning(message(), goal())

        assertEquals(candidates, planner.lastCandidates)
    }

    // ================= 6. Candidate ordering preserved =================

    @Test
    fun `candidate ordering is preserved exactly`() = runTest {
        val candidates = listOf(candidate("cand-3"), candidate("cand-1"), candidate("cand-2"))
        val generator = FakePlanCandidateGenerator { candidates }
        val planner = FakePlannerRuntime { request, _ -> completedResult(request.planningSessionId) }
        val c = coordinator(planCandidateGenerator = generator, plannerRuntime = planner)

        c.initiatePlanning(message(), goal())

        assertEquals(listOf("cand-3", "cand-1", "cand-2"), planner.lastCandidates?.map { it.planCandidateId.value })
    }

    // ================= 7. Empty list passed to PlannerRuntime =================

    @Test
    fun `an empty candidate list from the generator is passed through to the planner unchanged`() = runTest {
        val generator = FakePlanCandidateGenerator { emptyList() }
        val planner = FakePlannerRuntime { request, _ -> failedResult(request.planningSessionId) }
        val c = coordinator(planCandidateGenerator = generator, plannerRuntime = planner)

        c.initiatePlanning(message(), goal())

        assertEquals(1, planner.callCount)
        assertEquals(emptyList(), planner.lastCandidates)
    }

    // ================= 8. Planner not called when generator throws =================

    @Test
    fun `the planner is never called when the generator throws`() = runTest {
        val planner = FakePlannerRuntime { request, _ -> completedResult(request.planningSessionId) }
        val c = coordinator(
            planCandidateGenerator = ThrowingPlanCandidateGenerator(IllegalStateException("generator boom")),
            plannerRuntime = planner,
        )

        assertFailsWith<IllegalStateException> { c.initiatePlanning(message(), goal()) }

        assertEquals(0, planner.callCount)
    }

    // ================= 9. Generator exception propagates unchanged =================

    @Test
    fun `an exception thrown by the generator propagates unchanged`() = runTest {
        val c = coordinator(planCandidateGenerator = ThrowingPlanCandidateGenerator(IllegalStateException("generator boom")))

        val thrown = assertFailsWith<IllegalStateException> { c.initiatePlanning(message(), goal()) }
        assertEquals("generator boom", thrown.message)
    }

    // ================= 10. Planner exception propagates unchanged =================

    @Test
    fun `an exception thrown by the planner propagates unchanged`() = runTest {
        val c = coordinator(plannerRuntime = ThrowingPlannerRuntime(IllegalStateException("planner boom")))

        val thrown = assertFailsWith<IllegalStateException> { c.initiatePlanning(message(), goal()) }
        assertEquals("planner boom", thrown.message)
    }

    // ================= 11-13. Every PlanningSessionResult variant wrapped unchanged =================

    @Test
    fun `a Completed result is wrapped unchanged in GoalPlanningHandoffOutcome Planned`() = runTest {
        var configuredResult: PlanningSessionResult? = null
        val planner = FakePlannerRuntime { request, _ ->
            completedResult(request.planningSessionId).also { configuredResult = it }
        }
        val c = coordinator(plannerRuntime = planner)

        val outcome = c.initiatePlanning(message(), goal())

        val planned = assertIs<GoalPlanningHandoffOutcome.Planned>(outcome)
        assertSame(configuredResult, planned.planningSessionResult)
    }

    @Test
    fun `a Rejected result is wrapped unchanged in GoalPlanningHandoffOutcome Planned`() = runTest {
        var configuredResult: PlanningSessionResult? = null
        val planner = FakePlannerRuntime { request, _ ->
            rejectedResult(request.planningSessionId).also { configuredResult = it }
        }
        val c = coordinator(plannerRuntime = planner)

        val outcome = c.initiatePlanning(message(), goal())

        val planned = assertIs<GoalPlanningHandoffOutcome.Planned>(outcome)
        assertSame(configuredResult, planned.planningSessionResult)
    }

    @Test
    fun `a Failed result is wrapped unchanged in GoalPlanningHandoffOutcome Planned, not treated as a fault`() = runTest {
        var configuredResult: PlanningSessionResult? = null
        val planner = FakePlannerRuntime { request, _ ->
            failedResult(request.planningSessionId).also { configuredResult = it }
        }
        val c = coordinator(plannerRuntime = planner)

        val outcome = c.initiatePlanning(message(), goal())

        val planned = assertIs<GoalPlanningHandoffOutcome.Planned>(outcome)
        assertSame(configuredResult, planned.planningSessionResult)
    }

    // ================= 14. Structural: exactly three declared fields =================

    @Test
    fun `the coordinator declares exactly three fields -- planningSessionIdFactory, planCandidateGenerator, plannerRuntime`() {
        val fieldNames = GoalPlanningHandoffCoordinator::class.java.declaredFields.map { it.name }.toSet()

        assertEquals(setOf("planningSessionIdFactory", "planCandidateGenerator", "plannerRuntime"), fieldNames)
    }

    // ================= 15. No Task Manager, execution, permission, tool or scheduling dependency =================

    @Test
    fun `no field of any Task Manager, execution, permission, tool or scheduling type exists on the coordinator`() {
        val fieldTypeNames = GoalPlanningHandoffCoordinator::class.java.declaredFields.map { it.type.simpleName }.toSet()

        val forbidden = setOf(
            "TaskProposalIntake",
            "TaskManagerRuntime",
            "InMemoryTaskManagerRuntime",
            "ExecutionPipeline",
            "PermissionEngine",
            "AgentRunCommandChannel",
            "ToolRegistry",
            "ToolInvocationBinding",
        )
        assertTrue(fieldTypeNames.none { it in forbidden })
    }

    // ================= Structural: constructor accepts exactly three dependencies =================

    @Test
    fun `the coordinator's constructor accepts exactly three dependencies`() {
        val constructor = GoalPlanningHandoffCoordinator::class.java.declaredConstructors.single()

        assertEquals(3, constructor.parameterCount)
        assertEquals(
            setOf("Function0", "PlanCandidateGenerator", "PlannerRuntime"),
            constructor.parameterTypes.map { it.simpleName }.toSet(),
        )
    }

    // ================= Blank-ID / throwing-factory propagation, unchanged from prior revision =================

    @Test
    fun `a factory returning a blank string causes IllegalArgumentException`() = runTest {
        val c = coordinator(planningSessionIdFactory = { "" })

        assertFailsWith<IllegalArgumentException> {
            c.initiatePlanning(message(), goal())
        }
    }

    @Test
    fun `an exception thrown by planningSessionIdFactory propagates unchanged`() = runTest {
        val c = coordinator(planningSessionIdFactory = { throw IllegalStateException("factory boom") })

        assertFailsWith<IllegalStateException> {
            c.initiatePlanning(message(), goal())
        }
    }
}
