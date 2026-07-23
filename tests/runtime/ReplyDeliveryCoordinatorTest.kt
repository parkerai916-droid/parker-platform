package parker.core.runtime

import kotlinx.coroutines.test.runTest
import parker.core.interfaces.ActionResourceMapping
import parker.core.interfaces.ActionVocabularyEntry
import parker.core.interfaces.CorrelationId
import parker.core.interfaces.DecisionId
import parker.core.interfaces.ExecutionResult
import parker.core.interfaces.ExecutionResultStatus
import parker.core.interfaces.InboundOwnerMessage
import parker.core.interfaces.ModuleConnectivityDeclaration
import parker.core.interfaces.ModuleDescriptor
import parker.core.interfaces.ModuleId
import parker.core.interfaces.PermissionAction
import parker.core.interfaces.PermissionDecision
import parker.core.interfaces.PermissionDecisionOutcome
import parker.core.interfaces.PermissionLevel
import parker.core.interfaces.Principal
import parker.core.interfaces.PrincipalId
import parker.core.interfaces.PrincipalStatus
import parker.core.interfaces.PrincipalType
import parker.core.interfaces.ReasoningProviderResponse
import parker.core.interfaces.RequestId
import parker.core.interfaces.Resource
import parker.core.interfaces.ResourceId
import parker.core.interfaces.ResourceLifecycleState
import parker.core.interfaces.ResourceSensitivity
import parker.core.interfaces.ResourceType
import parker.core.interfaces.ResultId
import java.time.Instant
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertIs
import kotlin.test.assertTrue

/**
 * Sprint 10, Unit 2 acceptance test, per
 * `docs/implementation/REPLY_DELIVERY_COORDINATOR_SCOPE_LOCK.md` Section
 * 11 (Scope Locked). [ReplyDeliveryCoordinator] is exercised in
 * isolation via a real [ResponseComposer] built from [FakeIdentityService]
 * and a real [ResponseDelivery] built from [FakeResourceRegistry]/
 * [FakeExecutionPipeline] for every structural, branch, and
 * invariant-level assertion (Scope Lock Section 5, Decision 2 --
 * neither [ResponseComposer] nor [ResponseDelivery] is itself
 * interface-backed, so verification proceeds through each dependency's
 * own already-existing interface-backed seam, at value level, not by
 * object-reference identity), and once, end-to-end, against the real
 * `InMemoryResourceRegistry` / `InMemoryToolRegistry` /
 * `InMemoryModuleRegistry` / `InMemoryToolInvocationBinding` /
 * `DefaultExecutionPipeline` / `LocalTextChannelDeliverTool` /
 * `InMemoryIdentityService` stack (mirroring `ResponseComposerTest.kt`'s
 * own established compatibility-test pattern, one layer further out).
 *
 * **No-construction, no-mutation invariant (Scope Lock Section 9;
 * companion Plan Section 4b) is verified by direct code review of
 * [ReplyDeliveryCoordinator.composeAndDeliver]'s own five-line body, not
 * by a runtime test in this file** -- no
 * `OutboundParkerResponse` constructor call, no `.copy()` call, and no
 * `metadata` mutation appears anywhere in it, and neither
 * [ResponseComposer] nor [ResponseDelivery] exposes anything through
 * which this class could intercept or alter the value passing between
 * them.
 */
class ReplyDeliveryCoordinatorTest {

    private val fixedTimestamp: Instant = Instant.parse("2026-01-01T00:00:00Z")
    private val channelId = ModuleId("channel.local-text")
    private val toolResourceId = ResourceId("res.tool.local-text-deliver")

    private fun message(
        text: String = "hello",
        correlationId: String = "corr-1",
        senderPrincipalId: String = "user-1",
        channel: ModuleId = channelId,
    ) = InboundOwnerMessage(
        channelId = channel,
        senderPrincipalId = PrincipalId(senderPrincipalId),
        text = text,
        timestamp = fixedTimestamp,
        correlationId = CorrelationId(correlationId),
    )

    private fun responseComposerPrincipal() = Principal(
        principalId = PrincipalId("system.response-composer"),
        principalType = PrincipalType.SYSTEM,
        displayName = "Response Composer",
        owner = null,
        status = PrincipalStatus.CREATED,
        createdAt = fixedTimestamp,
        lastSeenAt = fixedTimestamp,
    )

    /** A [FakeIdentityService] with `system.response-composer` registered. */
    private fun registeredIdentityService() = FakeIdentityService { principalId ->
        if (principalId == PrincipalId("system.response-composer")) responseComposerPrincipal() else null
    }

    /** A [FakeIdentityService] whose [FakeIdentityService.resolve] throws. */
    private fun throwingIdentityService() = FakeIdentityService {
        throw IllegalStateException("identity service boom")
    }

    private fun toolResource(
        resourceId: ResourceId = toolResourceId,
        owner: PrincipalId = PrincipalId(channelId.value),
    ) = Resource(
        resourceId = resourceId,
        resourceType = ResourceType.TOOL,
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

    /** The primary fixture (Scope Lock Section 11): real [ResponseComposer]/[ResponseDelivery], fake collaborators one level down. */
    private class Fixture(
        val identityService: FakeIdentityService,
        val resources: FakeResourceRegistry,
        val pipeline: FakeExecutionPipeline,
        val coordinator: ReplyDeliveryCoordinator,
    )

    private fun fixture(
        identityService: FakeIdentityService = registeredIdentityService(),
        resources: FakeResourceRegistry = FakeResourceRegistry { listOf(toolResource()) },
        pipeline: FakeExecutionPipeline = FakeExecutionPipeline { executionResult(it.requestId) },
    ): Fixture {
        val composer = ResponseComposer(identityService)
        val delivery = ResponseDelivery(resources, pipeline)
        return Fixture(identityService, resources, pipeline, ReplyDeliveryCoordinator(composer, delivery))
    }

    // ================= Produced path (Reply, successful delivery) =================

    @Test
    fun `a Reply that composes and delivers successfully returns Produced carrying ResponseDelivery's own ExecutionResult`() = runTest {
        val f = fixture()
        val originalMessage = message(correlationId = "corr-1")

        val outcome = f.coordinator.composeAndDeliver(originalMessage, ReasoningProviderResponse.Reply("hello, owner"))

        assertIs<GatedOutcome.Produced<ExecutionResult>>(outcome)
        val request = f.pipeline.lastSubmittedRequest
        assertEquals(PrincipalId("system.response-composer"), request?.principalId)
        assertEquals(originalMessage.correlationId.value, request?.correlationId)
        assertEquals(mapOf(RESPONSE_TEXT_METADATA_KEY to "hello, owner"), request?.metadata)
        assertEquals(1, f.identityService.resolveCallCount)
        assertEquals(1, f.resources.listByOwnerCallCount)
        assertEquals(1, f.pipeline.submitCallCount)
    }

    // ================= NotAccepted path (Goal) -- ResponseDelivery never entered =================

    @Test
    fun `a Goal returns ResponseComposer's own NotAccepted unchanged, and ResponseDelivery is never entered`() = runTest {
        val f = fixture()

        val outcome = f.coordinator.composeAndDeliver(message(), ReasoningProviderResponse.Goal("book a flight"))

        val notAccepted = assertIs<GatedOutcome.NotAccepted>(outcome)
        assertTrue("Goal" in notAccepted.reason)
        assertEquals(0, f.identityService.resolveCallCount)
        assertEquals(0, f.resources.listByOwnerCallCount)
        assertEquals(0, f.pipeline.submitCallCount)
    }

    // ================= NotAccepted path (NoAction) -- ResponseDelivery never entered =================

    @Test
    fun `a NoAction returns ResponseComposer's own NotAccepted unchanged, and ResponseDelivery is never entered`() = runTest {
        val f = fixture()

        val outcome = f.coordinator.composeAndDeliver(message(), ReasoningProviderResponse.NoAction)

        val notAccepted = assertIs<GatedOutcome.NotAccepted>(outcome)
        assertTrue("NoAction" in notAccepted.reason)
        assertEquals(0, f.identityService.resolveCallCount)
        assertEquals(0, f.resources.listByOwnerCallCount)
        assertEquals(0, f.pipeline.submitCallCount)
    }

    // ================= NotAccepted path (delivery-level rejection) -- entered but rejected =================

    @Test
    fun `a Reply that composes successfully but finds no channel Resource returns ResponseDelivery's own NotAccepted unchanged`() = runTest {
        val f = fixture(resources = FakeResourceRegistry { emptyList() })

        val outcome = f.coordinator.composeAndDeliver(message(), ReasoningProviderResponse.Reply("hello, owner"))

        val notAccepted = assertIs<GatedOutcome.NotAccepted>(outcome)
        assertTrue("no channel Resource found" in notAccepted.reason)
        // Composition succeeded (identity resolved), delivery was entered (listByOwner called),
        // but never reached ExecutionPipeline -- the call-counted distinction between "never
        // entered ResponseDelivery" (Goal/NoAction, above) and "entered ResponseDelivery but
        // was itself rejected" (this case).
        assertEquals(1, f.identityService.resolveCallCount)
        assertEquals(1, f.resources.listByOwnerCallCount)
        assertEquals(0, f.pipeline.submitCallCount)
    }

    // ================= Exactly-once invocation, no retries, per-branch counts across sequential calls =================

    @Test
    fun `dependency call counts increment only on their own applicable branch, across sequential calls of different variants`() = runTest {
        val f = fixture()

        f.coordinator.composeAndDeliver(message(), ReasoningProviderResponse.Reply("first reply"))
        assertEquals(1, f.identityService.resolveCallCount)
        assertEquals(1, f.resources.listByOwnerCallCount)
        assertEquals(1, f.pipeline.submitCallCount)

        f.coordinator.composeAndDeliver(message(), ReasoningProviderResponse.Goal("some goal"))
        assertEquals(1, f.identityService.resolveCallCount)
        assertEquals(1, f.resources.listByOwnerCallCount)
        assertEquals(1, f.pipeline.submitCallCount)

        f.coordinator.composeAndDeliver(message(), ReasoningProviderResponse.NoAction)
        assertEquals(1, f.identityService.resolveCallCount)
        assertEquals(1, f.resources.listByOwnerCallCount)
        assertEquals(1, f.pipeline.submitCallCount)

        f.coordinator.composeAndDeliver(message(), ReasoningProviderResponse.Reply("second reply"))
        assertEquals(2, f.identityService.resolveCallCount)
        assertEquals(2, f.resources.listByOwnerCallCount)
        assertEquals(2, f.pipeline.submitCallCount)
    }

    // ================= Exception propagation, not recovery (Scope Lock Section 10) =================

    @Test
    fun `an exception thrown while composing propagates unchanged, and ResponseDelivery is never reached`() = runTest {
        val f = fixture(identityService = throwingIdentityService())

        assertFailsWith<IllegalStateException> {
            f.coordinator.composeAndDeliver(message(), ReasoningProviderResponse.Reply("hello, owner"))
        }
        assertEquals(0, f.resources.listByOwnerCallCount)
        assertEquals(0, f.pipeline.submitCallCount)
    }

    @Test
    fun `an exception thrown while delivering propagates unchanged`() = runTest {
        val f = fixture(resources = FakeResourceRegistry { throw IllegalStateException("resource registry boom") })

        assertFailsWith<IllegalStateException> {
            f.coordinator.composeAndDeliver(message(), ReasoningProviderResponse.Reply("hello, owner"))
        }
    }

    // ================= Structural / negative test (Scope Lock Section 9) =================

    @Test
    fun `the coordinator's constructor accepts exactly two dependencies -- ResponseComposer and ResponseDelivery`() {
        val constructor = ReplyDeliveryCoordinator::class.java.declaredConstructors.single()
        val parameterTypes = constructor.parameterTypes.map { it.simpleName }.toSet()

        assertEquals(setOf("ResponseComposer", "ResponseDelivery"), parameterTypes)
    }

    // ================= Statelessness (companion Plan Section 4a) =================

    @Test
    fun `the coordinator declares no field beyond its two constructor-injected dependencies`() {
        val fieldNames = ReplyDeliveryCoordinator::class.java.declaredFields.map { it.name }.toSet()

        assertEquals(setOf("responseComposer", "responseDelivery"), fieldNames)
    }

    // ================= Real end-to-end test, narrower scope (Scope Lock Section 11, item 10) =================

    @Test
    fun `end-to-end -- a Reply reaches the owner through the real ResponseComposer plus ResponseDelivery stack, via one coordinator call`() = runTest {
        val resources = InMemoryResourceRegistry()
        val tools = InMemoryToolRegistry(resources)
        val moduleRegistry = InMemoryModuleRegistry(tools, resources)
        val toolInvocationBinding = InMemoryToolInvocationBinding()

        val delivered = mutableListOf<String>()
        val tool = LocalTextChannelDeliverTool { text -> delivered.add(text) }

        val moduleDescriptor = ModuleDescriptor(
            moduleId = channelId,
            name = "Local Text Channel",
            version = "0.1.0",
            toolsExposed = listOf(tool.descriptor),
            requiredPermissions = emptyList(),
            connectivityDeclaration = ModuleConnectivityDeclaration.LOCAL_ONLY,
        )
        moduleRegistry.register(moduleDescriptor)
        moduleRegistry.enable(channelId, PrincipalId("system.parker"))
        toolInvocationBinding.bind(tool.descriptor, tool)

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
                decisionId = DecisionId("dec-reply-delivery-coordinator-e2e-1"),
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

        // A real InMemoryIdentityService, not FakeIdentityService -- this test's own purpose
        // is to exercise real production wiring end-to-end (Scope Lock Section 11, item 10).
        val identityService = InMemoryIdentityService()
        identityService.register(responseComposerPrincipal())
        val composer = ResponseComposer(identityService)

        val coordinator = ReplyDeliveryCoordinator(composer, delivery)

        val originalMessage = message(correlationId = "corr-e2e-1")
        val outcome = coordinator.composeAndDeliver(originalMessage, ReasoningProviderResponse.Reply("hello, owner"))

        val produced = assertIs<GatedOutcome.Produced<ExecutionResult>>(outcome)
        assertEquals(ExecutionResultStatus.SUCCESS, produced.value.status)
        assertEquals(listOf("hello, owner"), delivered)
    }
}
