package parker.core.runtime

import kotlinx.coroutines.test.runTest
import parker.core.interfaces.ActionResourceMapping
import parker.core.interfaces.ActionVocabularyEntry
import parker.core.interfaces.CommunicationIntakeDisposition
import parker.core.interfaces.Conversation
import parker.core.interfaces.ConversationDisposition
import parker.core.interfaces.ConversationEngine
import parker.core.interfaces.ConversationId
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
import parker.core.interfaces.ReasoningContext
import parker.core.interfaces.ReasoningProviderResponse
import parker.core.interfaces.RequestId
import parker.core.interfaces.Resource
import parker.core.interfaces.ResourceId
import parker.core.interfaces.ResourceLifecycleState
import parker.core.interfaces.ResourceSensitivity
import parker.core.interfaces.ResourceType
import parker.core.interfaces.ResultId
import parker.core.interfaces.Turn
import parker.core.interfaces.TurnId
import java.time.Instant
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertIs
import kotlin.test.assertTrue

/**
 * `ConversationReplyCoordinator` acceptance test, per
 * `docs/implementation/CONVERSATION_REPLY_COORDINATOR_SCOPE_LOCK.md`
 * Section 16 (Scope Locked). [ConversationReplyCoordinator] is exercised
 * in isolation via a real [CommunicationConversationCoordinator] built
 * from [FakeCommunicationIntake] and a real
 * [ConversationTurnReasoningCoordinator] (itself built from a
 * pass-through [ConversationEngine] fake and [FakeReasoningProvider] --
 * the exact fixture combination [CommunicationConversationCoordinatorTest]
 * already uses), and a real [ReplyDeliveryCoordinator] built from a real
 * [ResponseComposer] (via [FakeIdentityService]) and a real
 * [ResponseDelivery] (via [FakeResourceRegistry]/[FakeExecutionPipeline]
 * -- the exact fixture combination [ReplyDeliveryCoordinatorTest]
 * already uses). No new fake is introduced anywhere in this file.
 *
 * **No-construction/no-mutation invariant (Scope Lock Section 14) is
 * verified by direct code review of
 * [ConversationReplyCoordinator.submitAndDeliver]'s own five-line body,
 * not by a runtime test in this file** -- no data-carrying type is
 * constructed by that method, and neither
 * [CommunicationConversationCoordinator] nor [ReplyDeliveryCoordinator]
 * exposes anything through which this class could intercept or alter a
 * value passing between them.
 */
class ConversationReplyCoordinatorTest {

    private val fixedTimestamp: Instant = Instant.parse("2026-01-01T00:00:00Z")
    private val channelId = ModuleId("channel.local-text")
    private val toolResourceId = ResourceId("res.tool.local-text-deliver")

    private fun message(
        text: String = "hello",
        correlationId: String = "corr-1",
        senderPrincipalId: String = "user-1",
    ) = InboundOwnerMessage(
        channelId = channelId,
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

    private val fixedConversationId = ConversationId("conv-1")

    /**
     * A `ConversationEngine` fake that wraps whatever [InboundOwnerMessage]
     * and [ConversationId] it is given into a `Turn` unchanged -- mirrors
     * [CommunicationConversationCoordinatorTest]'s own identical
     * precedent, so tests here stay isolated to
     * [ConversationReplyCoordinator]'s own behaviour, not
     * [InMemoryConversationEngine]'s. `resolveConversationId` is never
     * expected to be called by anything under test in this file.
     */
    private fun passThroughConversationEngine() = object : ConversationEngine {
        override suspend fun resolveConversationId(message: InboundOwnerMessage): ConversationId =
            throw UnsupportedOperationException("not exercised by this coordinator's own tests")

        override suspend fun submitTurn(message: InboundOwnerMessage, conversationId: ConversationId): ConversationDisposition {
            val turnId = TurnId("turn-1")
            return ConversationDisposition(
                conversation = Conversation(
                    conversationId = conversationId,
                    ownerPrincipalId = message.senderPrincipalId,
                    channelId = message.channelId,
                    turnIds = listOf(turnId),
                ),
                turn = Turn(
                    turnId = turnId,
                    conversationId = conversationId,
                    message = message,
                    receivedAt = fixedTimestamp,
                ),
                isNewConversation = true,
            )
        }
    }

    /** The primary fixture (Scope Lock Section 16): real coordinators, fakes one level down. */
    private class Fixture(
        val communicationIntake: FakeCommunicationIntake,
        val reasoningProvider: FakeReasoningProvider,
        val identityService: FakeIdentityService,
        val resources: FakeResourceRegistry,
        val pipeline: FakeExecutionPipeline,
        val coordinator: ConversationReplyCoordinator,
    )

    private fun fixture(
        communicationIntake: FakeCommunicationIntake = FakeCommunicationIntake { msg -> CommunicationIntakeDisposition.Accepted(msg.correlationId, msg) },
        reasoningProvider: FakeReasoningProvider = FakeReasoningProvider { ReasoningProviderResponse.Reply("hello, owner") },
        identityService: FakeIdentityService = registeredIdentityService(),
        resources: FakeResourceRegistry = FakeResourceRegistry { listOf(toolResource()) },
        pipeline: FakeExecutionPipeline = FakeExecutionPipeline { executionResult(it.requestId) },
    ): Fixture {
        val conversationTurnReasoningCoordinator = ConversationTurnReasoningCoordinator(passThroughConversationEngine(), reasoningProvider)
        val communicationConversationCoordinator = CommunicationConversationCoordinator(communicationIntake, conversationTurnReasoningCoordinator)
        val composer = ResponseComposer(identityService)
        val delivery = ResponseDelivery(resources, pipeline)
        val replyDeliveryCoordinator = ReplyDeliveryCoordinator(composer, delivery)
        return Fixture(
            communicationIntake,
            reasoningProvider,
            identityService,
            resources,
            pipeline,
            ConversationReplyCoordinator(communicationConversationCoordinator, replyDeliveryCoordinator),
        )
    }

    // ================= 1. Upstream NotAccepted =================

    @Test
    fun `a rejected message returns CommunicationConversationCoordinator's own NotAccepted unchanged, and downstream is never entered`() = runTest {
        val f = fixture(
            communicationIntake = FakeCommunicationIntake { msg -> CommunicationIntakeDisposition.Rejected(msg.correlationId, "channel not enabled") },
        )

        val outcome = f.coordinator.submitAndDeliver(message(), ReasoningContext(emptyList()), fixedConversationId)

        val notAccepted = assertIs<GatedOutcome.NotAccepted>(outcome)
        assertEquals("channel not enabled", notAccepted.reason)
        assertEquals(0, f.reasoningProvider.reasonCallCount)
        assertEquals(0, f.identityService.resolveCallCount)
        assertEquals(0, f.resources.listByOwnerCallCount)
        assertEquals(0, f.pipeline.submitCallCount)
    }

    // ================= 2. Upstream Produced -- successful end-to-end composition and delivery =================

    @Test
    fun `an accepted Reply composes and delivers successfully, returning Produced carrying ResponseDelivery's own ExecutionResult`() = runTest {
        val f = fixture()
        val originalMessage = message(correlationId = "corr-1")

        val outcome = f.coordinator.submitAndDeliver(originalMessage, ReasoningContext(listOf("prior context")), fixedConversationId)

        assertIs<GatedOutcome.Produced<ExecutionResult>>(outcome)
        val request = f.pipeline.lastSubmittedRequest
        assertEquals(PrincipalId("system.response-composer"), request?.principalId)
        assertEquals(originalMessage.correlationId.value, request?.correlationId)
        assertEquals(mapOf(RESPONSE_TEXT_METADATA_KEY to "hello, owner"), request?.metadata)
        assertEquals(1, f.communicationIntake.submitInboundMessageCallCount)
        assertEquals(1, f.reasoningProvider.reasonCallCount)
        assertEquals(1, f.identityService.resolveCallCount)
        assertEquals(1, f.resources.listByOwnerCallCount)
        assertEquals(1, f.pipeline.submitCallCount)
    }

    // ================= 3. Downstream NotAccepted (Goal / NoAction) =================

    @Test
    fun `a Goal returns ResponseComposer's own NotAccepted unchanged, and ResponseDelivery is never entered`() = runTest {
        val f = fixture(reasoningProvider = FakeReasoningProvider { ReasoningProviderResponse.Goal("book a flight") })

        val outcome = f.coordinator.submitAndDeliver(message(), ReasoningContext(emptyList()), fixedConversationId)

        val notAccepted = assertIs<GatedOutcome.NotAccepted>(outcome)
        assertTrue("Goal" in notAccepted.reason)
        assertEquals(0, f.identityService.resolveCallCount)
        assertEquals(0, f.resources.listByOwnerCallCount)
        assertEquals(0, f.pipeline.submitCallCount)
    }

    @Test
    fun `a NoAction returns ResponseComposer's own NotAccepted unchanged, and ResponseDelivery is never entered`() = runTest {
        val f = fixture(reasoningProvider = FakeReasoningProvider { ReasoningProviderResponse.NoAction })

        val outcome = f.coordinator.submitAndDeliver(message(), ReasoningContext(emptyList()), fixedConversationId)

        val notAccepted = assertIs<GatedOutcome.NotAccepted>(outcome)
        assertTrue("NoAction" in notAccepted.reason)
        assertEquals(0, f.identityService.resolveCallCount)
        assertEquals(0, f.resources.listByOwnerCallCount)
        assertEquals(0, f.pipeline.submitCallCount)
    }

    // ================= 4. Downstream NotAccepted (delivery-level rejection) =================

    @Test
    fun `a Reply that composes successfully but finds no channel Resource returns ResponseDelivery's own NotAccepted unchanged`() = runTest {
        val f = fixture(resources = FakeResourceRegistry { emptyList() })

        val outcome = f.coordinator.submitAndDeliver(message(), ReasoningContext(emptyList()), fixedConversationId)

        val notAccepted = assertIs<GatedOutcome.NotAccepted>(outcome)
        assertTrue("no channel Resource found" in notAccepted.reason)
        assertEquals(1, f.identityService.resolveCallCount)
        assertEquals(1, f.resources.listByOwnerCallCount)
        assertEquals(0, f.pipeline.submitCallCount)
    }

    // ================= 5. Exact upstream and downstream call counts across sequential calls =================

    @Test
    fun `call counts across sequential calls increment only on their own applicable branch`() = runTest {
        val f = fixture(
            reasoningProvider = FakeReasoningProvider { request ->
                when (request.turn.message.text) {
                    "goal" -> ReasoningProviderResponse.Goal("some goal")
                    "noaction" -> ReasoningProviderResponse.NoAction
                    else -> ReasoningProviderResponse.Reply("a reply")
                }
            },
        )

        f.coordinator.submitAndDeliver(message(text = "reply-1", correlationId = "corr-1"), ReasoningContext(emptyList()), fixedConversationId)
        assertEquals(1, f.communicationIntake.submitInboundMessageCallCount)
        assertEquals(1, f.reasoningProvider.reasonCallCount)
        assertEquals(1, f.identityService.resolveCallCount)
        assertEquals(1, f.resources.listByOwnerCallCount)
        assertEquals(1, f.pipeline.submitCallCount)

        f.coordinator.submitAndDeliver(message(text = "goal", correlationId = "corr-2"), ReasoningContext(emptyList()), fixedConversationId)
        assertEquals(2, f.communicationIntake.submitInboundMessageCallCount)
        assertEquals(2, f.reasoningProvider.reasonCallCount)
        assertEquals(1, f.identityService.resolveCallCount)
        assertEquals(1, f.resources.listByOwnerCallCount)
        assertEquals(1, f.pipeline.submitCallCount)

        f.coordinator.submitAndDeliver(message(text = "noaction", correlationId = "corr-3"), ReasoningContext(emptyList()), fixedConversationId)
        assertEquals(3, f.communicationIntake.submitInboundMessageCallCount)
        assertEquals(3, f.reasoningProvider.reasonCallCount)
        assertEquals(1, f.identityService.resolveCallCount)
        assertEquals(1, f.resources.listByOwnerCallCount)
        assertEquals(1, f.pipeline.submitCallCount)

        f.coordinator.submitAndDeliver(message(text = "reply-2", correlationId = "corr-4"), ReasoningContext(emptyList()), fixedConversationId)
        assertEquals(4, f.communicationIntake.submitInboundMessageCallCount)
        assertEquals(4, f.reasoningProvider.reasonCallCount)
        assertEquals(2, f.identityService.resolveCallCount)
        assertEquals(2, f.resources.listByOwnerCallCount)
        assertEquals(2, f.pipeline.submitCallCount)
    }

    // ================= 6. Downstream not called on upstream rejection (explicit) =================

    @Test
    fun `ReplyDeliveryCoordinator's own dependencies are never touched when CommunicationConversationCoordinator itself rejects`() = runTest {
        val f = fixture(
            communicationIntake = FakeCommunicationIntake { msg -> CommunicationIntakeDisposition.Rejected(msg.correlationId, "sender not resolved") },
        )

        f.coordinator.submitAndDeliver(message(), ReasoningContext(emptyList()), fixedConversationId)

        assertEquals(0, f.identityService.resolveCallCount)
        assertEquals(0, f.resources.listByOwnerCallCount)
        assertEquals(0, f.pipeline.submitCallCount)
    }

    // ================= 7. Sequencing evidence =================

    @Test
    fun `reasoning is never reached when the message is rejected, and is reached exactly once when accepted`() = runTest {
        val rejectingFixture = fixture(
            communicationIntake = FakeCommunicationIntake { msg -> CommunicationIntakeDisposition.Rejected(msg.correlationId, "channel not enabled") },
        )
        rejectingFixture.coordinator.submitAndDeliver(message(), ReasoningContext(emptyList()), fixedConversationId)
        assertEquals(0, rejectingFixture.reasoningProvider.reasonCallCount)

        val acceptingFixture = fixture()
        acceptingFixture.coordinator.submitAndDeliver(message(), ReasoningContext(emptyList()), fixedConversationId)
        assertEquals(1, acceptingFixture.reasoningProvider.reasonCallCount)
    }

    // ================= 8. Exception propagation from upstream =================

    @Test
    fun `an exception thrown by CommunicationConversationCoordinator's own first dependency propagates unchanged, and ReplyDeliveryCoordinator is never reached`() = runTest {
        val f = fixture(communicationIntake = FakeCommunicationIntake { throw IllegalStateException("communication boom") })

        assertFailsWith<IllegalStateException> {
            f.coordinator.submitAndDeliver(message(), ReasoningContext(emptyList()), fixedConversationId)
        }
        assertEquals(0, f.reasoningProvider.reasonCallCount)
        assertEquals(0, f.identityService.resolveCallCount)
        assertEquals(0, f.resources.listByOwnerCallCount)
        assertEquals(0, f.pipeline.submitCallCount)
    }

    // ================= 9. Exception propagation from downstream =================

    @Test
    fun `an exception thrown by ReplyDeliveryCoordinator's own dependencies propagates unchanged`() = runTest {
        val f = fixture(identityService = throwingIdentityService())

        assertFailsWith<IllegalStateException> {
            f.coordinator.submitAndDeliver(message(), ReasoningContext(emptyList()), fixedConversationId)
        }
        assertEquals(0, f.resources.listByOwnerCallCount)
        assertEquals(0, f.pipeline.submitCallCount)
    }

    // ================= 10. Structural: constructor accepts exactly two dependencies =================

    @Test
    fun `the coordinator's constructor accepts exactly two dependencies -- CommunicationConversationCoordinator and ReplyDeliveryCoordinator`() {
        val constructor = ConversationReplyCoordinator::class.java.declaredConstructors.single()
        val parameterTypes = constructor.parameterTypes.map { it.simpleName }.toSet()

        assertEquals(setOf("CommunicationConversationCoordinator", "ReplyDeliveryCoordinator"), parameterTypes)
    }

    // ================= 11. Statelessness =================

    @Test
    fun `the coordinator declares no field beyond its two constructor-injected dependencies`() {
        val fieldNames = ConversationReplyCoordinator::class.java.declaredFields.map { it.name }.toSet()

        assertEquals(setOf("communicationConversationCoordinator", "replyDeliveryCoordinator"), fieldNames)
    }

    // ================= 12. Real end-to-end test, narrower scope (FakeReasoningProvider, not ModelReasoningProvider) =================

    @Test
    fun `end-to-end -- a Reply reaches the owner through the real intake, conversation, composition and delivery stack, via one coordinator call`() = runTest {
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
                decisionId = DecisionId("dec-conversation-reply-coordinator-e2e-1"),
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
        // is to exercise real production wiring end-to-end (Scope Lock Section 16, item 12).
        val identityService = InMemoryIdentityService()
        identityService.register(
            Principal(
                principalId = PrincipalId("user-1"),
                principalType = PrincipalType.USER,
                displayName = "Owner",
                owner = null,
                status = PrincipalStatus.CREATED,
                createdAt = fixedTimestamp,
                lastSeenAt = fixedTimestamp,
            ),
        )
        identityService.register(
            Principal(
                principalId = PrincipalId("system.conversation-engine"),
                principalType = PrincipalType.SYSTEM,
                displayName = "Conversation Engine",
                owner = null,
                status = PrincipalStatus.CREATED,
                createdAt = fixedTimestamp,
                lastSeenAt = fixedTimestamp,
            ),
        )
        identityService.register(responseComposerPrincipal())

        val communicationIntake = InMemoryCommunicationIntake(moduleRegistry, identityService)
        val conversationEngine = InMemoryConversationEngine(identityService)
        // FakeReasoningProvider, not ModelReasoningProvider -- this Unit's own scope excludes
        // selecting or configuring a model provider and validating live HTTP behaviour
        // (Scope Lock Section 3/Section 15); no live model server is required anywhere here.
        val reasoningProvider = FakeReasoningProvider { ReasoningProviderResponse.Reply("hello, owner") }
        val conversationTurnReasoningCoordinator = ConversationTurnReasoningCoordinator(conversationEngine, reasoningProvider)
        val communicationConversationCoordinator = CommunicationConversationCoordinator(communicationIntake, conversationTurnReasoningCoordinator)

        val composer = ResponseComposer(identityService)
        val replyDeliveryCoordinator = ReplyDeliveryCoordinator(composer, delivery)

        val coordinator = ConversationReplyCoordinator(communicationConversationCoordinator, replyDeliveryCoordinator)

        val originalMessage = message(correlationId = "corr-e2e-1")
        // Sprint 11 Unit 5: resolution is a real, separate, upstream call in production
        // (ParkerRuntime.submitOwnerMessage) -- mirrored here explicitly rather than via a fake,
        // since this test's own purpose is real, end-to-end production wiring (item 12 above).
        val conversationId = conversationEngine.resolveConversationId(originalMessage)
        val outcome = coordinator.submitAndDeliver(originalMessage, ReasoningContext(emptyList()), conversationId)

        val produced = assertIs<GatedOutcome.Produced<ExecutionResult>>(outcome)
        assertEquals(ExecutionResultStatus.SUCCESS, produced.value.status)
        assertEquals(listOf("hello, owner"), delivered)
    }
}
