package parker.core.runtime

import kotlinx.coroutines.test.runTest
import parker.core.interfaces.DecisionId
import parker.core.interfaces.ExecutionRequest
import parker.core.interfaces.PermissionAction
import parker.core.interfaces.PermissionDecision
import parker.core.interfaces.PermissionDecisionOutcome
import parker.core.interfaces.PermissionLevel
import parker.core.interfaces.PrincipalId
import parker.core.interfaces.RequestId
import parker.core.interfaces.RequestOrigin
import parker.core.interfaces.RequestPriority
import parker.core.interfaces.ResourceId
import java.time.Instant
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Sprint 1, Unit 8
 * (`docs/implementation/SPRINT_1_VERTICAL_SLICE_PLAN.md` §6): "Deterministic,
 * always-`APPROVED` `PermissionEngine` for Sprint 1's fixed happy path,
 * following the existing `FakePermissionEngine` precedent."
 *
 * `FakePermissionEngine` itself is unmodified Phase 2 code, already relied
 * on indirectly by `DefaultExecutionPipelineTest.kt`, `InMemoryTaskManagerRuntimeTest.kt`
 * (transitively, via `DefaultExecutionPipeline`), and `InMemoryAgentRuntimeTest.kt`.
 * This file is the standalone test of the fixture's own contract that none
 * of those files provide, since each only configures and consumes it as a
 * dependency, never asserts on its behaviour directly. This is what makes
 * the plan's own acceptance text -- "confirm it still applies unmodified"
 * -- a checked claim rather than an inference from other units' tests
 * continuing to pass.
 */
class FakePermissionEngineTest {

    private fun request(
        requestId: String = "req-1",
        principalId: String = "user-1",
    ) = ExecutionRequest(
        requestId = RequestId(requestId),
        principalId = PrincipalId(principalId),
        origin = RequestOrigin.TEXT,
        intent = "read today's calendar",
        targetResources = listOf(ResourceId("res.calendar.1")),
        proposedActions = listOf("read calendar"),
        priority = RequestPriority.NORMAL,
        createdAt = Instant.parse("2026-01-01T00:00:00Z"),
        correlationId = "corr-1",
    )

    private fun decision(
        decisionId: String = "dec-1",
        outcome: PermissionDecisionOutcome = PermissionDecisionOutcome.APPROVED,
    ) = PermissionDecision(
        decisionId = DecisionId(decisionId),
        principalId = PrincipalId("user-1"),
        resourceId = ResourceId("res.calendar.1"),
        action = PermissionAction.READ,
        decision = outcome,
        level = PermissionLevel.AUTOMATIC,
        timestamp = Instant.parse("2026-01-01T00:00:00Z"),
    )

    // --- evaluate is a pure passthrough to the configured decisionFor ---

    @Test
    fun `evaluate returns exactly what decisionFor produces`() = runTest {
        val fixedDecision = decision(outcome = PermissionDecisionOutcome.DENIED)
        val engine = FakePermissionEngine { fixedDecision }

        val result = engine.evaluate(request())

        assertEquals(fixedDecision, result)
    }

    @Test
    fun `configuring an always-APPROVED decisionFor serves Sprint 1's fixed happy path`() = runTest {
        // The exact usage this unit exists to confirm: a single fixed APPROVED decision,
        // returned regardless of which ExecutionRequest is submitted.
        val engine = FakePermissionEngine { decision(outcome = PermissionDecisionOutcome.APPROVED) }

        val first = engine.evaluate(request(requestId = "req-1"))
        val second = engine.evaluate(request(requestId = "req-2", principalId = "user-2"))

        assertEquals(PermissionDecisionOutcome.APPROVED, first.decision)
        assertEquals(PermissionDecisionOutcome.APPROVED, second.decision)
    }

    @Test
    fun `decisionFor may vary its outcome per request -- the fixture forwards the real request, not a fixed one`() = runTest {
        val engine = FakePermissionEngine { req ->
            decision(outcome = if (req.principalId == PrincipalId("user-1")) PermissionDecisionOutcome.APPROVED else PermissionDecisionOutcome.DENIED)
        }

        val approved = engine.evaluate(request(principalId = "user-1"))
        val denied = engine.evaluate(request(principalId = "user-2"))

        assertEquals(PermissionDecisionOutcome.APPROVED, approved.decision)
        assertEquals(PermissionDecisionOutcome.DENIED, denied.decision)
    }

    // --- determinism ---

    @Test
    fun `evaluate is deterministic -- the same request against the same configuration yields the same decision`() = runTest {
        val engine = FakePermissionEngine { decision() }
        val req = request()

        val first = engine.evaluate(req)
        val second = engine.evaluate(req)

        assertEquals(first, second)
    }

    // --- call counting ---

    @Test
    fun `evaluateCallCount starts at zero and increments exactly once per evaluate call`() = runTest {
        val engine = FakePermissionEngine { decision() }
        assertEquals(0, engine.evaluateCallCount)

        engine.evaluate(request())
        assertEquals(1, engine.evaluateCallCount)

        engine.evaluate(request(requestId = "req-2"))
        engine.evaluate(request(requestId = "req-3"))
        assertEquals(3, engine.evaluateCallCount)
    }

    // --- explain ---

    @Test
    fun `explain returns a PermissionExplanation carrying the exact decisionId it was asked about`() = runTest {
        val engine = FakePermissionEngine { decision() }

        val explanation = engine.explain(DecisionId("dec-42"))

        assertEquals(DecisionId("dec-42"), explanation.decisionId)
        assertTrue(explanation.reason.isNotBlank())
    }

    @Test
    fun `explain is deterministic and does not depend on evaluate having been called first`() = runTest {
        val engine = FakePermissionEngine { decision() }

        val explanation = engine.explain(DecisionId("dec-never-evaluated"))

        assertEquals(DecisionId("dec-never-evaluated"), explanation.decisionId)
        assertTrue(explanation.reason.isNotBlank())
    }

    @Test
    fun `explain does not affect evaluateCallCount`() = runTest {
        val engine = FakePermissionEngine { decision() }

        engine.explain(DecisionId("dec-1"))

        assertEquals(0, engine.evaluateCallCount)
    }
}
