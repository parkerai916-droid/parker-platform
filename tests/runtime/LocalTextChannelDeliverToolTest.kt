package parker.core.runtime

import kotlinx.coroutines.test.runTest
import parker.core.interfaces.ActionResourceMapping
import parker.core.interfaces.ActionVocabularyEntry
import parker.core.interfaces.CorrelationId
import parker.core.interfaces.DecisionId
import parker.core.interfaces.ExecutionRequest
import parker.core.interfaces.ExecutionResult
import parker.core.interfaces.ExecutionResultStatus
import parker.core.interfaces.ModuleConnectivityDeclaration
import parker.core.interfaces.ModuleDescriptor
import parker.core.interfaces.ModuleId
import parker.core.interfaces.OutboundParkerResponse
import parker.core.interfaces.PermissionAction
import parker.core.interfaces.PermissionDecision
import parker.core.interfaces.PermissionDecisionOutcome
import parker.core.interfaces.PermissionLevel
import parker.core.interfaces.PrincipalId
import parker.core.interfaces.RequestId
import parker.core.interfaces.RequestOrigin
import parker.core.interfaces.RequestPriority
import parker.core.interfaces.ResourceId
import parker.core.interfaces.ResourceType
import parker.core.interfaces.ValidationResult
import java.time.Instant
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertSame
import kotlin.test.assertTrue

/**
 * Sprint 8 acceptance test, per
 * `docs/implementation/LOCAL_TEXT_CHANNEL_DELIVER_TOOL_IMPLEMENTATION_PLAN.md`
 * (Scope Locked). [LocalTextChannelDeliverTool] is exercised in
 * isolation -- no fake collaborator is needed, since its only dependency
 * is a plain injected callback, not an interface -- and once, end-to-end,
 * through the real `ModuleRegistry` / `ResourceRegistry` / `ToolRegistry`
 * / `ToolInvocationBinding` / `DefaultExecutionPipeline` stack, mirroring
 * `ResponseDeliveryTest.kt`'s own established end-to-end pattern, to
 * prove `ResponseDelivery` can reach this real Tool, not only a
 * throwaway placeholder.
 */
class LocalTextChannelDeliverToolTest {

    private val fixedTimestamp: Instant = Instant.parse("2026-01-01T00:00:00Z")
    private val channelModuleId = ModuleId("channel.local-text")

    private fun executionRequest(metadata: Map<String, String> = emptyMap()) = ExecutionRequest(
        requestId = RequestId("req-1"),
        principalId = PrincipalId("user-1"),
        origin = RequestOrigin.TEXT,
        intent = "deliver response",
        targetResources = listOf(ResourceId("res.tool.local-text-deliver")),
        proposedActions = listOf("notify owner"),
        priority = RequestPriority.NORMAL,
        createdAt = fixedTimestamp,
        correlationId = "corr-1",
        metadata = metadata,
    )

    // ================= descriptor shape =================

    @Test
    fun `descriptor declares the locked toolId, capability, and resource-type shape`() {
        val tool = LocalTextChannelDeliverTool { }

        assertEquals("deliver", tool.descriptor.toolId)
        assertTrue(tool.descriptor.displayName.isNotBlank())
        assertTrue(tool.descriptor.description.isNotBlank())
        assertEquals(setOf(PermissionAction.NOTIFY), tool.descriptor.supportedActions)
        assertEquals(setOf(ResourceType.TOOL), tool.descriptor.supportedResourceTypes)
    }

    // ================= validate() =================

    @Test
    fun `validate rejects a request whose metadata is missing RESPONSE_TEXT_METADATA_KEY`() = runTest {
        val tool = LocalTextChannelDeliverTool { }
        val request = executionRequest(metadata = emptyMap())

        val result = tool.validate(request)

        val invalid = assertIs<ValidationResult.Invalid>(result)
        assertTrue(invalid.reasons.isNotEmpty())
        assertTrue(invalid.reasons.any { RESPONSE_TEXT_METADATA_KEY in it })
    }

    @Test
    fun `validate rejects a request whose RESPONSE_TEXT_METADATA_KEY value is blank`() = runTest {
        val tool = LocalTextChannelDeliverTool { }
        val request = executionRequest(metadata = mapOf(RESPONSE_TEXT_METADATA_KEY to "   "))

        val result = tool.validate(request)

        assertIs<ValidationResult.Invalid>(result)
    }

    @Test
    fun `validate accepts a request whose RESPONSE_TEXT_METADATA_KEY value is non-blank`() = runTest {
        val tool = LocalTextChannelDeliverTool { }
        val request = executionRequest(metadata = mapOf(RESPONSE_TEXT_METADATA_KEY to "hello, owner"))

        val result = tool.validate(request)

        assertEquals(ValidationResult.Valid, result)
    }

    // ================= execute() =================

    @Test
    fun `execute invokes the callback exactly once with the exact response text, unchanged`() = runTest {
        val received = mutableListOf<String>()
        val tool = LocalTextChannelDeliverTool { text -> received.add(text) }
        val unusualText = "  Your package HAS arrived -- 3 items  "
        val request = executionRequest(metadata = mapOf(RESPONSE_TEXT_METADATA_KEY to unusualText))

        tool.execute(request)

        // Single-element list: exactly one invocation. Exact string equality (including
        // leading/trailing whitespace and mixed case): no trim, normalisation, or reformatting.
        assertEquals(listOf(unusualText), received)
    }

    @Test
    fun `execute returns a successful ToolResult for the locked toolId`() = runTest {
        val tool = LocalTextChannelDeliverTool { }
        val request = executionRequest(metadata = mapOf(RESPONSE_TEXT_METADATA_KEY to "hello, owner"))

        val result = tool.execute(request)

        assertEquals("deliver", result.toolId)
        assertTrue(result.success)
    }

    // ================= Statelessness =================

    @Test
    fun `LocalTextChannelDeliverTool declares exactly one field -- the injected callback`() {
        val fieldNames = LocalTextChannelDeliverTool::class.java.declaredFields.map { it.name }.toSet()

        // descriptor is a computed property (get() = ...), not a stored value -- no backing
        // field is generated for it, per this class's own KDoc.
        assertEquals(setOf("onOwnerNotified"), fieldNames)
    }

    // ================= Descriptor identity / binding (Plan Decision 9's ownership rule) =================

    @Test
    fun `LocalTextChannelDeliverTool can be bound and retrieved through InMemoryToolInvocationBinding using its own descriptor`() = runTest {
        val tool = LocalTextChannelDeliverTool { }
        val binding = InMemoryToolInvocationBinding()

        binding.bind(tool.descriptor, tool)
        val bound = binding.invocableFor(tool.descriptor)

        assertSame(tool, bound)
    }

    // ================= End-to-end: real ModuleRegistry + ResourceRegistry + ToolRegistry + =================
    // ================= ToolInvocationBinding + DefaultExecutionPipeline + ResponseDelivery  =================

    @Test
    fun `end-to-end -- ResponseDelivery reaches the real LocalTextChannelDeliverTool through DefaultExecutionPipeline`() = runTest {
        val resources = InMemoryResourceRegistry()
        val tools = InMemoryToolRegistry(resources)
        val moduleRegistry = InMemoryModuleRegistry(tools, resources)
        val toolInvocationBinding = InMemoryToolInvocationBinding()

        val delivered = mutableListOf<String>()
        val tool = LocalTextChannelDeliverTool { text -> delivered.add(text) }

        val moduleDescriptor = ModuleDescriptor(
            moduleId = channelModuleId,
            name = "Local Text Channel",
            version = "0.1.0",
            toolsExposed = listOf(tool.descriptor),
            requiredPermissions = emptyList(),
            connectivityDeclaration = ModuleConnectivityDeclaration.LOCAL_ONLY,
        )
        // Single-source-of-truth check (Plan Decision 9's ownership rule; Locked Decisions
        // 10-12): the descriptor placed in toolsExposed is tool.descriptor itself, not a
        // separately-declared literal.
        assertEquals(tool.descriptor, moduleDescriptor.toolsExposed.single())

        moduleRegistry.register(moduleDescriptor)
        moduleRegistry.enable(channelModuleId, PrincipalId("system.parker"))
        toolInvocationBinding.bind(tool.descriptor, tool)

        // ADR-026: the backing Resource InMemoryModuleRegistry.register created automatically
        // is owned by PrincipalId(channelModuleId.value) -- exactly one TOOL-type match, the
        // same mechanism ResponseDelivery.deliver depends on.
        val ownedResources = resources.listByOwner(PrincipalId(channelModuleId.value))
        assertEquals(1, ownedResources.size)
        assertEquals(ResourceType.TOOL, ownedResources.single().resourceType)

        val vocabulary = InMemoryActionVocabulary()
        vocabulary.register(
            ActionVocabularyEntry(
                verbPhrase = "notify owner",
                mappings = setOf(ActionResourceMapping(PermissionAction.NOTIFY, ResourceType.TOOL)),
            ),
        )
        val actionMapper = ActionMapper(vocabulary)

        val eventBus = InMemoryEventBus()
        val permissionEngine = FakePermissionEngine { request ->
            PermissionDecision(
                decisionId = DecisionId("dec-e2e-local-text-deliver-1"),
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

        val response = OutboundParkerResponse(
            channelId = channelModuleId,
            senderPrincipalId = PrincipalId("user-1"),
            text = "hello, owner",
            timestamp = fixedTimestamp,
            correlationId = CorrelationId("corr-e2e-1"),
        )

        val outcome = delivery.deliver(response)

        val produced = assertIs<GatedOutcome.Produced<ExecutionResult>>(outcome)
        assertEquals(ExecutionResultStatus.SUCCESS, produced.value.status)
        assertEquals(listOf(response.text), delivered)
        assertEquals(1, permissionEngine.evaluateCallCount)
    }
}
