package parker.core.runtime

import java.time.Instant
import kotlinx.coroutines.test.runTest
import parker.core.interfaces.DecisionId
import parker.core.interfaces.EventType
import parker.core.interfaces.ExecutionLifecycleState
import parker.core.interfaces.ExecutionRequest
import parker.core.interfaces.ExecutionResultStatus
import parker.core.interfaces.PermissionAction
import parker.core.interfaces.PermissionDecision
import parker.core.interfaces.PermissionDecisionOutcome
import parker.core.interfaces.PermissionLevel
import parker.core.interfaces.PrincipalId
import parker.core.interfaces.RequestId
import parker.core.interfaces.RequestOrigin
import parker.core.interfaces.RequestPriority
import parker.core.interfaces.Resource
import parker.core.interfaces.ResourceSensitivity
import parker.core.interfaces.ResourceId
import parker.core.interfaces.ResourceLifecycleState
import parker.core.interfaces.ResourceType
import parker.core.interfaces.ToolDescriptor
import parker.core.interfaces.ToolLifecycleState
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Proves Priority 4 (Runtime Integration): Execution Pipeline correctly
 * orchestrates Action Mapping -> Permission Engine -> Tool Registry ->
 * Event Bus, using a [FakePermissionEngine] test double so this suite
 * does not need to invent authorisation policy either (see
 * IMPLEMENTATION_GAPS.md #25/#30).
 */
class DefaultExecutionPipelineTest {

    private val calendarResourceId = ResourceId("res.calendar.1")
    private val toolResourceId = ResourceId("res.tool.calendar-reader")

    private suspend fun buildPipeline(
        decisionFor: (ExecutionRequest) -> PermissionDecision,
    ): Triple<DefaultExecutionPipeline, InMemoryEventBus, FakePermissionEngine> {
        val resources = InMemoryResourceRegistry()
        resources.register(
            Resource(
                resourceId = calendarResourceId,
                resourceType = ResourceType.CALENDAR,
                displayName = "Household Calendar",
                ownerPrincipalId = PrincipalId("user-1"),
                sensitivity = ResourceSensitivity.HOUSEHOLD,
                lifecycleState = ResourceLifecycleState.AVAILABLE,
                createdAt = Instant.parse("2026-01-01T00:00:00Z"),
                updatedAt = Instant.parse("2026-01-01T00:00:00Z"),
                source = "test",
            ),
        )
        resources.register(
            Resource(
                resourceId = toolResourceId,
                resourceType = ResourceType.TOOL,
                displayName = "Calendar Reader Tool Resource",
                ownerPrincipalId = PrincipalId("system"),
                sensitivity = ResourceSensitivity.PUBLIC,
                lifecycleState = ResourceLifecycleState.AVAILABLE,
                createdAt = Instant.parse("2026-01-01T00:00:00Z"),
                updatedAt = Instant.parse("2026-01-01T00:00:00Z"),
                source = "test",
            ),
        )

        val vocabulary = InMemoryActionVocabulary()
        vocabulary.register(
            parker.core.interfaces.ActionVocabularyEntry(
                verbPhrase = "read calendar",
                mappings = setOf(parker.core.interfaces.ActionResourceMapping(PermissionAction.READ, ResourceType.CALENDAR)),
            ),
        )
        val actionMapper = ActionMapper(vocabulary)

        val tools = InMemoryToolRegistry(resources)
        tools.register(
            ToolDescriptor(
                toolId = "tool.calendar.read",
                displayName = "Calendar Reader",
                description = "Reads calendar entries",
                supportedActions = setOf(PermissionAction.READ),
                supportedResourceTypes = setOf(ResourceType.CALENDAR),
            ),
            toolResourceId,
        )
        tools.setLifecycleState("tool.calendar.read", "0.1.0", ToolLifecycleState.ENABLED)

        val eventBus = InMemoryEventBus()
        val permissionEngine = FakePermissionEngine(decisionFor)

        val pipeline = DefaultExecutionPipeline(resources, actionMapper, permissionEngine, tools, eventBus)
        return Triple(pipeline, eventBus, permissionEngine)
    }

    private fun request(
        requestId: String = "req-1",
        proposedActions: List<String> = listOf("read calendar"),
        targetResources: List<ResourceId> = listOf(calendarResourceId),
        expiresAt: Instant? = null,
    ) = ExecutionRequest(
        requestId = RequestId(requestId),
        principalId = PrincipalId("user-1"),
        origin = RequestOrigin.TEXT,
        intent = "what's on the calendar today",
        targetResources = targetResources,
        proposedActions = proposedActions,
        priority = RequestPriority.NORMAL,
        createdAt = Instant.parse("2026-01-01T00:00:00Z"),
        correlationId = "corr-1",
        expiresAt = expiresAt,
    )

    private fun approvedDecision(action: PermissionAction = PermissionAction.READ) = PermissionDecision(
        decisionId = DecisionId("dec-1"),
        principalId = PrincipalId("user-1"),
        resourceId = calendarResourceId,
        action = action,
        decision = PermissionDecisionOutcome.APPROVED,
        level = PermissionLevel.AUTOMATIC,
        timestamp = Instant.parse("2026-01-01T00:00:00Z"),
    )

    @Test
    fun `happy path -- approved request resolves a Tool and succeeds`() = runTest {
        val (pipeline, eventBus, permissionEngine) = buildPipeline { approvedDecision() }

        var sawCompleted = false
        eventBus.subscribe(EventType("execution.completed"), PrincipalId("test-subscriber")) { sawCompleted = true }
        var sawGranted = false
        eventBus.subscribe(EventType("permission.granted"), PrincipalId("test-subscriber")) { sawGranted = true }

        val result = pipeline.submit(request())

        assertEquals(ExecutionResultStatus.SUCCESS, result.status)
        assertEquals(1, result.toolResults.size)
        assertEquals("tool.calendar.read", result.toolResults.single().toolId)
        assertEquals(1, permissionEngine.evaluateCallCount)
        assertTrue(sawCompleted)
        assertTrue(sawGranted)

        val status = pipeline.status(RequestId("req-1"))
        assertEquals(ExecutionLifecycleState.COMPLETED, status.state)
    }

    @Test
    fun `denied request never reaches Tool resolution`() = runTest {
        val (pipeline, eventBus, _) = buildPipeline {
            approvedDecision().copy(decision = PermissionDecisionOutcome.DENIED)
        }
        var sawDenied = false
        eventBus.subscribe(EventType("permission.denied"), PrincipalId("test-subscriber")) { sawDenied = true }

        val result = pipeline.submit(request())

        assertEquals(ExecutionResultStatus.DENIED, result.status)
        assertTrue(result.toolResults.isEmpty())
        assertTrue(sawDenied)
    }

    @Test
    fun `deferred request produces a DEFERRED result`() = runTest {
        val (pipeline, _, _) = buildPipeline {
            approvedDecision().copy(decision = PermissionDecisionOutcome.DEFERRED)
        }
        val result = pipeline.submit(request())
        assertEquals(ExecutionResultStatus.DEFERRED, result.status)
    }

    @Test
    fun `an already-expired request never reaches the Permission Engine`() = runTest {
        val (pipeline, _, permissionEngine) = buildPipeline { approvedDecision() }
        // Must be after this helper's fixed createdAt (2026-01-01) -- ExecutionRequest's own
        // constructor invariant (ExecutionRequest.kt) requires expiresAt > createdAt, an
        // object cannot be created already-expired. This value is still safely in the past
        // relative to wall-clock "now" at test-run time, which is what actually makes the
        // pipeline's expiry check (Instant.now().isAfter(expiresAt)) trigger.
        val expired = request(expiresAt = Instant.parse("2026-01-02T00:00:00Z"))

        val result = pipeline.submit(expired)

        assertEquals(ExecutionResultStatus.EXPIRED, result.status)
        assertEquals(0, permissionEngine.evaluateCallCount)
    }

    @Test
    fun `an unresolvable target resource fails validation before Permission evaluation`() = runTest {
        val (pipeline, _, permissionEngine) = buildPipeline { approvedDecision() }
        val badRequest = request(targetResources = listOf(ResourceId("nonexistent-resource")))

        val result = pipeline.submit(badRequest)

        assertEquals(ExecutionResultStatus.FAILED, result.status)
        assertTrue(result.errors.isNotEmpty())
        assertEquals(0, permissionEngine.evaluateCallCount)
        // IMPLEMENTATION_GAPS.md #31: Created -> Failed is now a legal lifecycle edge, so the
        // tracked state should reflect the failure cleanly rather than staying stuck at Created.
        assertEquals(ExecutionLifecycleState.FAILED, pipeline.status(badRequest.requestId).state)
    }

    @Test
    fun `an unresolvable proposed action fails validation before Permission evaluation`() = runTest {
        val (pipeline, _, permissionEngine) = buildPipeline { approvedDecision() }
        val badRequest = request(proposedActions = listOf("do something nonsensical"))

        val result = pipeline.submit(badRequest)

        assertEquals(ExecutionResultStatus.FAILED, result.status)
        assertEquals(0, permissionEngine.evaluateCallCount)
        assertEquals(ExecutionLifecycleState.FAILED, pipeline.status(badRequest.requestId).state)
    }

    @Test
    fun `approved but no matching Tool resolves to a FAILED result`() = runTest {
        // WRITE is approved but no registered Tool supports WRITE against CALENDAR.
        val (pipeline, _, _) = buildPipeline { approvedDecision(action = PermissionAction.WRITE) }
        val result = pipeline.submit(request())

        assertEquals(ExecutionResultStatus.FAILED, result.status)
        assertTrue(result.errors.single().contains("no Tool could be resolved"))
    }

    @Test
    fun `cancel on an unknown requestId reports not cancelled`() = runTest {
        val (pipeline, _, _) = buildPipeline { approvedDecision() }
        val result = pipeline.cancel(RequestId("never-submitted"))
        assertFalse(result.cancelled)
    }

    @Test
    fun `cancel after a terminal result reports not cancelled`() = runTest {
        val (pipeline, _, _) = buildPipeline { approvedDecision() }
        pipeline.submit(request())

        val result = pipeline.cancel(RequestId("req-1"))
        assertFalse(result.cancelled)
    }

    @Test
    fun `status of an unknown requestId throws`() = runTest {
        val (pipeline, _, _) = buildPipeline { approvedDecision() }
        try {
            pipeline.status(RequestId("never-submitted"))
            throw AssertionError("expected NoSuchElementException")
        } catch (e: NoSuchElementException) {
            // expected
        }
    }
}
