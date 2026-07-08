package parker.core.runtime

import kotlinx.coroutines.test.runTest
import parker.core.interfaces.ActionResourceMapping
import parker.core.interfaces.ActionVocabularyEntry
import parker.core.interfaces.CorrelationId
import parker.core.interfaces.DecisionId
import parker.core.interfaces.ExecutionRequest
import parker.core.interfaces.ExecutionResult
import parker.core.interfaces.ExecutionResultStatus
import parker.core.interfaces.ModuleId
import parker.core.interfaces.PermissionAction
import parker.core.interfaces.PermissionDecision
import parker.core.interfaces.PermissionDecisionOutcome
import parker.core.interfaces.PermissionLevel
import parker.core.interfaces.OutboundParkerResponse
import parker.core.interfaces.PrincipalId
import parker.core.interfaces.RequestId
import parker.core.interfaces.RequestOrigin
import parker.core.interfaces.RequestPriority
import parker.core.interfaces.Resource
import parker.core.interfaces.ResourceId
import parker.core.interfaces.ResourceLifecycleState
import parker.core.interfaces.ResourceSensitivity
import parker.core.interfaces.ResourceType
import parker.core.interfaces.ResultId
import parker.core.interfaces.ToolDescriptor
import parker.core.interfaces.ToolLifecycleState
import java.time.Instant
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertIs
import kotlin.test.assertNotEquals
import kotlin.test.assertSame
import kotlin.test.assertTrue

/**
 * Sprint 7, Unit C4 acceptance test, per
 * `docs/implementation/RESPONSE_DELIVERY_IMPLEMENTATION_PLAN.md` Section 6
 * (Scope Locked). [ResponseDelivery] is exercised in isolation via
 * [FakeResourceRegistry]/[FakeExecutionPipeline] for every structural and
 * invariant-level assertion, and once, end-to-end, against the real
 * [InMemoryResourceRegistry] + [DefaultExecutionPipeline] stack (mirroring
 * [VerticalSliceEndToEndTest]'s own established pattern) to prove
 * `ADR-026`'s Resource-ownership convention and the registered `NOTIFY`
 * vocabulary entry actually cohere as a real, running path.
 */
class ResponseDeliveryTest {

    private val fixedTimestamp: Instant = Instant.parse("2026-01-01T00:00:00Z")
    private val channelId = ModuleId("channel.local-text")
    private val toolResourceId = ResourceId("res.tool.local-text-deliver")

    private fun response(
        text: String = "hello, owner",
        correlationId: String = "corr-1",
        senderPrincipalId: String = "user-1",
        channel: ModuleId = channelId,
    ) = OutboundParkerResponse(
        channelId = channel,
        senderPrincipalId = PrincipalId(senderPrincipalId),
        text = text,
        timestamp = fixedTimestamp,
        correlationId = CorrelationId(correlationId),
    )

    private fun toolResource(
        resourceId: ResourceId = toolResourceId,
        owner: PrincipalId = PrincipalId(channelId.value),
        resourceType: ResourceType = ResourceType.TOOL,
    ) = Resource(
        resourceId = resourceId,
        resourceType = resourceType,
        displayName = "Local Text Channel Deliver Tool Resource",
        ownerPrincipalId = owner,
        sensitivity = ResourceSensitivity.PUBLIC,
        lifecycleState = ResourceLifecycleState.AVAILABLE,
        createdAt = fixedTimestamp,
        updatedAt = fixedTimestamp,
        source = "test",
    )

    private fun executionResult(requestId: RequestId, status: ExecutionResultStatus = ExecutionResultStatus.SUCCESS) = ExecutionResult(
        resultId = ResultId("result-${requestId.value}"),
        requestId = requestId,
        status = status,
        startedAt = fixedTimestamp,
        completedAt = fixedTimestamp,
    )

    // ================= Zero-match path =================

    @Test
    fun `zero matching Resource returns NotAccepted and never calls ExecutionPipeline`() = runTest {
        val resources = FakeResourceRegistry { emptyList() }
        val pipeline = FakeExecutionPipeline { executionResult(it.requestId) }
        val delivery = ResponseDelivery(resources, pipeline)

        val outcome = delivery.deliver(response())

        val notAccepted = assertIs<GatedOutcome.NotAccepted>(outcome)
        assertTrue("no channel Resource found" in notAccepted.reason)
        assertEquals(1, resources.listByOwnerCallCount)
        assertEquals(0, pipeline.submitCallCount)
    }

    // ================= Many-match path =================

    @Test
    fun `multiple matching Resources returns NotAccepted and never calls ExecutionPipeline`() = runTest {
        val resources = FakeResourceRegistry {
            listOf(
                toolResource(resourceId = ResourceId("res.tool.one")),
                toolResource(resourceId = ResourceId("res.tool.two")),
            )
        }
        val pipeline = FakeExecutionPipeline { executionResult(it.requestId) }
        val delivery = ResponseDelivery(resources, pipeline)

        val outcome = delivery.deliver(response())

        val notAccepted = assertIs<GatedOutcome.NotAccepted>(outcome)
        assertTrue("ambiguous" in notAccepted.reason)
        assertTrue("2" in notAccepted.reason)
        assertEquals(0, pipeline.submitCallCount)
    }

    // ================= Non-TOOL Resources are ignored =================

    @Test
    fun `non-TOOL Resources for the same owner are ignored -- a TOOL and a non-TOOL Resource together still resolve to exactly one match`() = runTest {
        val nonToolResource = toolResource(resourceId = ResourceId("res.household.calendar"), resourceType = ResourceType.CALENDAR)
        val resources = FakeResourceRegistry { listOf(toolResource(), nonToolResource) }
        val pipeline = FakeExecutionPipeline { executionResult(it.requestId) }
        val delivery = ResponseDelivery(resources, pipeline)

        val outcome = delivery.deliver(response())

        assertIs<GatedOutcome.Produced<ExecutionResult>>(outcome)
        assertEquals(1, pipeline.submitCallCount)
        assertEquals(listOf(toolResourceId), pipeline.lastSubmittedRequest?.targetResources)
    }

    @Test
    fun `only non-TOOL Resources for the same owner is treated as a zero-match, not a one-match`() = runTest {
        val resources = FakeResourceRegistry { listOf(toolResource(resourceType = ResourceType.CALENDAR)) }
        val pipeline = FakeExecutionPipeline { executionResult(it.requestId) }
        val delivery = ResponseDelivery(resources, pipeline)

        val outcome = delivery.deliver(response())

        val notAccepted = assertIs<GatedOutcome.NotAccepted>(outcome)
        assertTrue("no channel Resource found" in notAccepted.reason)
        assertEquals(0, pipeline.submitCallCount)
    }

    // ================= Exactly-one-match path =================

    @Test
    fun `exactly one matching TOOL Resource submits exactly one ExecutionRequest and returns Produced`() = runTest {
        val resources = FakeResourceRegistry { listOf(toolResource()) }
        val canned = executionResult(RequestId("deliver-response-corr-1"))
        val pipeline = FakeExecutionPipeline { canned }
        val delivery = ResponseDelivery(resources, pipeline)

        val outcome = delivery.deliver(response())

        val produced = assertIs<GatedOutcome.Produced<ExecutionResult>>(outcome)
        assertSame(canned, produced.value)
        assertEquals(1, pipeline.submitCallCount)
    }

    // ================= ExecutionRequest field-construction correctness =================

    @Test
    fun `the submitted ExecutionRequest's fields are constructed correctly`() = runTest {
        val resources = FakeResourceRegistry { listOf(toolResource()) }
        val pipeline = FakeExecutionPipeline { executionResult(it.requestId) }
        val delivery = ResponseDelivery(resources, pipeline)
        val theResponse = response(text = "your package has arrived", correlationId = "corr-field-test", senderPrincipalId = "user-42")

        delivery.deliver(theResponse)

        val request = pipeline.lastSubmittedRequest
        assertEquals(theResponse.senderPrincipalId, request?.principalId)
        assertEquals(RequestOrigin.TEXT, request?.origin)
        assertEquals("deliver response", request?.intent)
        assertEquals(listOf(toolResourceId), request?.targetResources)
        assertEquals(listOf("notify owner"), request?.proposedActions)
        assertEquals(theResponse.correlationId.value, request?.correlationId)
        assertEquals(mapOf(RESPONSE_TEXT_METADATA_KEY to theResponse.text), request?.metadata)
        // Plan Section 5, Decisions 3 and 4 (Scope Lock amendment).
        assertEquals(RequestPriority.NORMAL, request?.priority)
        assertEquals(RequestId("deliver-response-corr-field-test"), request?.requestId)
    }

    // ================= ExecutionPipeline result returned unchanged =================

    @Test
    fun `the ExecutionResult ExecutionPipeline returns is wrapped in Produced unchanged, not translated or reinterpreted`() = runTest {
        val resources = FakeResourceRegistry { listOf(toolResource()) }
        val denied = ExecutionResult(
            resultId = ResultId("result-denied"),
            requestId = RequestId("deliver-response-corr-1"),
            status = ExecutionResultStatus.DENIED,
            startedAt = fixedTimestamp,
            completedAt = fixedTimestamp,
        )
        val pipeline = FakeExecutionPipeline { denied }
        val delivery = ResponseDelivery(resources, pipeline)

        val outcome = delivery.deliver(response())

        val produced = assertIs<GatedOutcome.Produced<ExecutionResult>>(outcome)
        assertSame(denied, produced.value)
        assertEquals(ExecutionResultStatus.DENIED, produced.value.status)
    }

    // ================= Exception propagation, not recovery (Plan Section 4c) =================

    @Test
    fun `an exception thrown by ResourceRegistry-listByOwner propagates unchanged, and ExecutionPipeline is never reached`() = runTest {
        val resources = FakeResourceRegistry { throw IllegalStateException("resource registry boom") }
        val pipeline = FakeExecutionPipeline { executionResult(it.requestId) }
        val delivery = ResponseDelivery(resources, pipeline)

        assertFailsWith<IllegalStateException> {
            delivery.deliver(response())
        }
        assertEquals(0, pipeline.submitCallCount)
    }

    @Test
    fun `an exception thrown by ExecutionPipeline-submit propagates unchanged`() = runTest {
        val resources = FakeResourceRegistry { listOf(toolResource()) }
        val pipeline = FakeExecutionPipeline { throw IllegalStateException("execution pipeline boom") }
        val delivery = ResponseDelivery(resources, pipeline)

        assertFailsWith<IllegalStateException> {
            delivery.deliver(response())
        }
    }

    // ================= Structural: no prohibited dependency slot =================

    @Test
    fun `ResponseDelivery's constructor accepts exactly two dependencies -- ResourceRegistry and ExecutionPipeline`() {
        val constructor = ResponseDelivery::class.java.declaredConstructors.single()
        val parameterTypes = constructor.parameterTypes.map { it.simpleName }.toSet()

        assertEquals(setOf("ResourceRegistry", "ExecutionPipeline"), parameterTypes)
    }

    // ================= Statelessness (Plan Section 4a) =================

    @Test
    fun `ResponseDelivery declares no field beyond its two constructor-injected dependencies`() {
        val fieldNames = ResponseDelivery::class.java.declaredFields.map { it.name }.toSet()

        assertEquals(setOf("resourceRegistry", "executionPipeline"), fieldNames)
    }

    @Test
    fun `two independent invocations for different responses never observably interact`() = runTest {
        val firstOwnerResource = toolResource(resourceId = ResourceId("res.tool.channel-one"), owner = PrincipalId("channel.one"))
        val secondOwnerResource = toolResource(resourceId = ResourceId("res.tool.channel-two"), owner = PrincipalId("channel.two"))
        val resources = FakeResourceRegistry { owner ->
            when (owner.value) {
                "channel.one" -> listOf(firstOwnerResource)
                "channel.two" -> listOf(secondOwnerResource)
                else -> emptyList()
            }
        }
        val pipeline = FakeExecutionPipeline { request -> executionResult(request.requestId) }
        val delivery = ResponseDelivery(resources, pipeline)

        val firstOutcome = delivery.deliver(response(correlationId = "corr-1", channel = ModuleId("channel.one")))
        val firstTargets = pipeline.lastSubmittedRequest?.targetResources
        val secondOutcome = delivery.deliver(response(correlationId = "corr-2", channel = ModuleId("channel.two")))
        val secondTargets = pipeline.lastSubmittedRequest?.targetResources

        assertIs<GatedOutcome.Produced<ExecutionResult>>(firstOutcome)
        assertIs<GatedOutcome.Produced<ExecutionResult>>(secondOutcome)
        assertEquals(listOf(firstOwnerResource.resourceId), firstTargets)
        assertEquals(listOf(secondOwnerResource.resourceId), secondTargets)
        assertNotEquals(firstTargets, secondTargets)
        assertEquals(2, resources.listByOwnerCallCount)
        assertEquals(2, pipeline.submitCallCount)
    }

    // ================= End-to-end: real ResourceRegistry + real ExecutionPipeline, NOTIFY vocabulary registered =================

    @Test
    fun `end-to-end -- ADR-026's ownership convention and the registered NOTIFY vocabulary entry cohere through the real stack`() = runTest {
        val resources = InMemoryResourceRegistry()
        resources.register(toolResource())

        val vocabulary = InMemoryActionVocabulary()
        vocabulary.register(
            ActionVocabularyEntry(
                verbPhrase = "notify owner",
                mappings = setOf(ActionResourceMapping(PermissionAction.NOTIFY, ResourceType.TOOL)),
            ),
        )
        val actionMapper = ActionMapper(vocabulary)

        val tools = InMemoryToolRegistry(resources)
        val deliverToolDescriptor = ToolDescriptor(
            toolId = "tool.test.local-text-deliver",
            displayName = "Test Local Text Deliver Tool",
            description = "Minimal test Tool for ResponseDeliveryTest's own end-to-end verification only -- not the real Local Text Channel deliver Tool (RESPONSE_DELIVERY_IMPLEMENTATION_PLAN.md Section 8).",
            supportedActions = setOf(PermissionAction.NOTIFY),
            supportedResourceTypes = setOf(ResourceType.TOOL),
        )
        tools.register(deliverToolDescriptor, toolResourceId)
        tools.setLifecycleState(deliverToolDescriptor.toolId, deliverToolDescriptor.version, ToolLifecycleState.ENABLED)

        val toolInvocationBinding = InMemoryToolInvocationBinding()
        toolInvocationBinding.bind(deliverToolDescriptor, MockTool(deliverToolDescriptor))

        val eventBus = InMemoryEventBus()
        val permissionEngine = FakePermissionEngine { request ->
            PermissionDecision(
                decisionId = DecisionId("dec-e2e-1"),
                principalId = request.principalId,
                resourceId = request.targetResources.single(),
                action = PermissionAction.NOTIFY,
                decision = PermissionDecisionOutcome.APPROVED,
                level = PermissionLevel.AUTOMATIC,
                timestamp = fixedTimestamp,
            )
        }

        val pipeline = DefaultExecutionPipeline(resources, actionMapper, permissionEngine, tools, eventBus, toolInvocationBinding)
        val delivery = ResponseDelivery(resources, pipeline)

        val outcome = delivery.deliver(response())

        val produced = assertIs<GatedOutcome.Produced<ExecutionResult>>(outcome)
        assertEquals(ExecutionResultStatus.SUCCESS, produced.value.status)
        assertEquals(1, permissionEngine.evaluateCallCount)
    }
}
