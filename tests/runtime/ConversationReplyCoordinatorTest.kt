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
import parker.core.interfaces.KnowledgeCandidateEvaluator
import parker.core.interfaces.ModuleConnectivityDeclaration
import parker.core.interfaces.ModuleDescriptor
import parker.core.interfaces.ModuleId
import parker.core.interfaces.PermissionAction
import parker.core.interfaces.PermissionDecision
import parker.core.interfaces.PermissionDecisionOutcome
import parker.core.interfaces.PermissionLevel
import parker.core.interfaces.PlanCandidate
import parker.core.interfaces.PlanCandidateGenerator
import parker.core.interfaces.PlannerRuntime
import parker.core.interfaces.PlanningRequest
import parker.core.interfaces.PlanningSessionId
import parker.core.interfaces.PlanningSessionResult
import parker.core.interfaces.Principal
import parker.core.interfaces.PrincipalId
import parker.core.interfaces.PrincipalStatus
import parker.core.interfaces.PrincipalType
import parker.core.interfaces.ReasoningContext
import parker.core.interfaces.ReasoningProviderResponse
import parker.core.interfaces.ReasoningSubject
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
 * Section 16 (Scope Locked) and, as of the Reasoning-to-Planning
 * Handoff, `docs/implementation/REASONING_TO_PLANNING_HANDOFF_SCOPE_LOCK.md`
 * Section 4. [ConversationReplyCoordinator] is exercised in isolation via
 * a real [CommunicationConversationCoordinator] built from
 * [FakeCommunicationIntake] and a real [ConversationTurnReasoningCoordinator]
 * (itself built from a pass-through [ConversationEngine] fake and
 * [FakeReasoningProvider]), a real [ReplyDeliveryCoordinator] built from a
 * real [ResponseComposer] (via [FakeIdentityService]) and a real
 * [ResponseDelivery] (via [FakeResourceRegistry]/[FakeExecutionPipeline]),
 * and a real [GoalPlanningHandoffCoordinator] built from a `() -> String`
 * plus two small, hand-written fakes local to this file
 * ([fakePlanCandidateGenerator], [fakePlannerRuntime]) -- as of the Plan
 * Candidate to PlannerRuntime Integration, this coordinator genuinely
 * calls `PlannerRuntime.plan()`, so it can no longer be exercised without
 * supplying both.
 *
 * **No-construction/no-mutation invariant (Scope Lock Section 14) is
 * verified by direct code review of
 * [ConversationReplyCoordinator.submitAndDeliver]'s own body, not by a
 * runtime test in this file** -- no data-carrying type this class does
 * not already forward unchanged is constructed by that method beyond
 * delegating to [GoalPlanningHandoffCoordinator] for `PlanningRequest`
 * construction, and neither [CommunicationConversationCoordinator],
 * [ReplyDeliveryCoordinator], nor [GoalPlanningHandoffCoordinator]
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

    /** A hand-written [PlanCandidateGenerator] fake, local to this file -- always returns [onGenerate]'s result, default an empty list. */
    private fun fakePlanCandidateGenerator(onGenerate: (PlanningRequest) -> List<PlanCandidate> = { emptyList() }) =
        object : PlanCandidateGenerator {
            override suspend fun generate(request: PlanningRequest): List<PlanCandidate> = onGenerate(request)
        }

    /** A hand-written [PlannerRuntime] fake, local to this file -- always returns [onPlan]'s result. */
    private fun fakePlannerRuntime(onPlan: (PlanningRequest, List<PlanCandidate>) -> PlanningSessionResult) =
        object : PlannerRuntime {
            override suspend fun plan(request: PlanningRequest, candidates: List<PlanCandidate>): PlanningSessionResult =
                onPlan(request, candidates)
        }

    /** A deterministic [PlanningSessionResult.Failed], matching [InMemoryPlannerRuntime]'s own no-candidates wording. */
    private fun failedResult(planningSessionId: PlanningSessionId) = PlanningSessionResult.Failed(
        planningSessionId = planningSessionId,
        reason = "no Plan Candidates were supplied for this Planning Session",
        rejections = emptyList(),
    )

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
        val planningSessionIdFactoryCallCount: () -> Int,
        val coordinator: ConversationReplyCoordinator,
    )

    private fun fixture(
        communicationIntake: FakeCommunicationIntake = FakeCommunicationIntake { msg -> CommunicationIntakeDisposition.Accepted(msg.correlationId, msg) },
        reasoningProvider: FakeReasoningProvider = FakeReasoningProvider { ReasoningProviderResponse.Reply("hello, owner") },
        identityService: FakeIdentityService = registeredIdentityService(),
        resources: FakeResourceRegistry = FakeResourceRegistry { listOf(toolResource()) },
        pipeline: FakeExecutionPipeline = FakeExecutionPipeline { executionResult(it.requestId) },
        planningSessionIdFactory: () -> String = { "fixed-planning-session-id" },
        planCandidateGenerator: PlanCandidateGenerator = fakePlanCandidateGenerator(),
        plannerRuntime: PlannerRuntime = fakePlannerRuntime { request, _ -> failedResult(request.planningSessionId) },
        memoryAdmissionCoordinator: MemoryAdmissionCoordinator = defaultMemoryAdmissionCoordinator(),
    ): Fixture {
        val conversationTurnReasoningCoordinator = ConversationTurnReasoningCoordinator(passThroughConversationEngine(), reasoningProvider)
        val communicationConversationCoordinator = CommunicationConversationCoordinator(communicationIntake, conversationTurnReasoningCoordinator)
        val composer = ResponseComposer(identityService)
        val delivery = ResponseDelivery(resources, pipeline)
        val replyDeliveryCoordinator = ReplyDeliveryCoordinator(composer, delivery)
        var factoryCallCount = 0
        val goalPlanningHandoffCoordinator = GoalPlanningHandoffCoordinator(
            planningSessionIdFactory = {
                factoryCallCount++
                planningSessionIdFactory()
            },
            planCandidateGenerator = planCandidateGenerator,
            plannerRuntime = plannerRuntime,
        )
        return Fixture(
            communicationIntake,
            reasoningProvider,
            identityService,
            resources,
            pipeline,
            { factoryCallCount },
            ConversationReplyCoordinator(communicationConversationCoordinator, replyDeliveryCoordinator, goalPlanningHandoffCoordinator, memoryAdmissionCoordinator),
        )
    }

    /**
     * Parker Conversational Memory Bridge, Admission Unit. A minimal, real
     * (not fake) [MemoryAdmissionCoordinator] -- an always-approving
     * [FakePermissionEngine], a real, empty [InMemoryMemoryCore], and a
     * real [DefaultKnowledgeSubmission] over it -- for every existing test
     * in this file that does not itself exercise [ReasoningProviderResponse.Remember].
     * None of this file's own pre-existing tests reach any method on this
     * instance; it exists solely to satisfy [ConversationReplyCoordinator]'s
     * own constructor.
     */
    private fun defaultMemoryAdmissionCoordinator(): MemoryAdmissionCoordinator {
        val permissionEngine = FakePermissionEngine { request ->
            PermissionDecision(
                decisionId = DecisionId("dec-memory-admission-default"),
                principalId = request.principalId,
                resourceId = request.targetResources.single(),
                action = PermissionAction.WRITE,
                decision = PermissionDecisionOutcome.APPROVED,
                level = PermissionLevel.AUTOMATIC,
                timestamp = Instant.parse("2026-01-01T00:00:00Z"),
            )
        }
        val memoryCore = InMemoryMemoryCore()
        val knowledgeSubmission = DefaultKnowledgeSubmission(
            DefaultKnowledgeCandidateEvaluator(memoryCore),
            InMemoryKnowledgeItemPersistence(),
            permissionEngine,
        )
        return MemoryAdmissionCoordinator(memoryCore, knowledgeSubmission, permissionEngine)
    }

    // ================= 1. Upstream NotAccepted =================

    @Test
    fun `a rejected message returns CommunicationConversationCoordinator's own NotAccepted unchanged, and downstream is never entered`() = runTest {
        val f = fixture(
            communicationIntake = FakeCommunicationIntake { msg -> CommunicationIntakeDisposition.Rejected(msg.correlationId, "channel not enabled") },
        )

        val outcome = f.coordinator.submitAndDeliver(message(), ReasoningContext(emptyList()), fixedConversationId)

        val notAccepted = assertIs<ConversationOutcome.NotAccepted>(outcome)
        assertEquals("channel not enabled", notAccepted.reason)
        assertEquals(0, f.reasoningProvider.reasonCallCount)
        assertEquals(0, f.identityService.resolveCallCount)
        assertEquals(0, f.resources.listByOwnerCallCount)
        assertEquals(0, f.pipeline.submitCallCount)
        assertEquals(0, f.planningSessionIdFactoryCallCount())
    }

    // ================= 2. Upstream Produced -- successful end-to-end composition and delivery =================

    @Test
    fun `an accepted Reply composes and delivers successfully, returning ReplyDelivered carrying ResponseDelivery's own ExecutionResult`() = runTest {
        val f = fixture()
        val originalMessage = message(correlationId = "corr-1")

        val outcome = f.coordinator.submitAndDeliver(originalMessage, ReasoningContext(listOf("prior context")), fixedConversationId)

        val delivered = assertIs<ConversationOutcome.ReplyDelivered>(outcome)
        assertEquals(ExecutionResultStatus.SUCCESS, delivered.executionResult.status)
        val request = f.pipeline.lastSubmittedRequest
        assertEquals(PrincipalId("system.response-composer"), request?.principalId)
        assertEquals(originalMessage.correlationId.value, request?.correlationId)
        assertEquals(mapOf(RESPONSE_TEXT_METADATA_KEY to "hello, owner"), request?.metadata)
        assertEquals(1, f.communicationIntake.submitInboundMessageCallCount)
        assertEquals(1, f.reasoningProvider.reasonCallCount)
        assertEquals(1, f.identityService.resolveCallCount)
        assertEquals(1, f.resources.listByOwnerCallCount)
        assertEquals(1, f.pipeline.submitCallCount)
        assertEquals(0, f.planningSessionIdFactoryCallCount())
    }

    // ================= 3. Goal routing (Reasoning-to-Planning Handoff) =================

    @Test
    fun `a Goal is routed to GoalPlanningHandoffCoordinator, bypassing ResponseComposer and ResponseDelivery entirely`() = runTest {
        var capturedRequest: PlanningRequest? = null
        val f = fixture(
            reasoningProvider = FakeReasoningProvider { ReasoningProviderResponse.Goal("book a flight") },
            planningSessionIdFactory = { "planning-session-1" },
            planCandidateGenerator = fakePlanCandidateGenerator { request ->
                capturedRequest = request
                emptyList()
            },
            plannerRuntime = fakePlannerRuntime { request, _ -> failedResult(request.planningSessionId) },
        )
        val originalMessage = message(correlationId = "corr-goal-1", senderPrincipalId = "user-9")

        val outcome = f.coordinator.submitAndDeliver(originalMessage, ReasoningContext(emptyList()), fixedConversationId)

        val planned = assertIs<ConversationOutcome.Planned>(outcome)
        val handoffOutcome = assertIs<GoalPlanningHandoffOutcome.Planned>(planned.outcome)
        assertIs<PlanningSessionResult.Failed>(handoffOutcome.planningSessionResult)
        assertEquals("book a flight", capturedRequest?.goal)
        assertEquals(PrincipalId("user-9"), capturedRequest?.initiatingPrincipalId)
        assertEquals("corr-goal-1", capturedRequest?.correlationId)
        assertEquals(1, f.planningSessionIdFactoryCallCount())
        // Goal routing bypasses reply delivery entirely -- ResponseComposer/ResponseDelivery
        // are never reached.
        assertEquals(0, f.identityService.resolveCallCount)
        assertEquals(0, f.resources.listByOwnerCallCount)
        assertEquals(0, f.pipeline.submitCallCount)
    }

    @Test
    fun `a Reply never reaches GoalPlanningHandoffCoordinator`() = runTest {
        val f = fixture(
            planningSessionIdFactory = { throw AssertionError("GoalPlanningHandoffCoordinator must not be called for a Reply") },
        )

        val outcome = f.coordinator.submitAndDeliver(message(), ReasoningContext(emptyList()), fixedConversationId)

        assertIs<ConversationOutcome.ReplyDelivered>(outcome)
    }

    @Test
    fun `a NoAction never reaches GoalPlanningHandoffCoordinator`() = runTest {
        val f = fixture(
            reasoningProvider = FakeReasoningProvider { ReasoningProviderResponse.NoAction },
            planningSessionIdFactory = { throw AssertionError("GoalPlanningHandoffCoordinator must not be called for NoAction") },
        )

        val outcome = f.coordinator.submitAndDeliver(message(), ReasoningContext(emptyList()), fixedConversationId)

        assertIs<ConversationOutcome.NotAccepted>(outcome)
    }

    // ================= 3a. Remember routing (Parker Conversational Memory Bridge, Admission Unit) =================

    private fun approvedDecision(request: parker.core.interfaces.ExecutionRequest) = PermissionDecision(
        decisionId = DecisionId("dec-remember-test"),
        principalId = request.principalId,
        resourceId = request.targetResources.single(),
        action = PermissionAction.WRITE,
        decision = PermissionDecisionOutcome.APPROVED,
        level = PermissionLevel.AUTOMATIC,
        timestamp = fixedTimestamp,
    )

    private fun deniedDecision(request: parker.core.interfaces.ExecutionRequest) = PermissionDecision(
        decisionId = DecisionId("dec-remember-test"),
        principalId = request.principalId,
        resourceId = request.targetResources.single(),
        action = PermissionAction.WRITE,
        decision = PermissionDecisionOutcome.DENIED,
        level = PermissionLevel.AUTOMATIC,
        timestamp = fixedTimestamp,
    )

    /** Always promotes -- a real MemoryAdmissionCoordinator over a fresh InMemoryMemoryCore and an always-approving permission engine. */
    private fun storingMemoryAdmissionCoordinator(): MemoryAdmissionCoordinator {
        val memoryCore = InMemoryMemoryCore()
        val permissionEngine = FakePermissionEngine { request -> approvedDecision(request) }
        return MemoryAdmissionCoordinator(
            memoryCore,
            DefaultKnowledgeSubmission(DefaultKnowledgeCandidateEvaluator(memoryCore), InMemoryKnowledgeItemPersistence(), permissionEngine),
            permissionEngine,
        )
    }

    /** Denies at this coordinator's own admission gate specifically. */
    private fun notAuthorisedMemoryAdmissionCoordinator(): MemoryAdmissionCoordinator {
        val memoryCore = InMemoryMemoryCore()
        val permissionEngine = FakePermissionEngine { request ->
            if (MemoryAdmissionCoordinator.CONVERSATIONAL_MEMORY_RESOURCE_ID in request.targetResources) deniedDecision(request) else approvedDecision(request)
        }
        return MemoryAdmissionCoordinator(
            memoryCore,
            DefaultKnowledgeSubmission(DefaultKnowledgeCandidateEvaluator(memoryCore), InMemoryKnowledgeItemPersistence(), permissionEngine),
            permissionEngine,
        )
    }

    /** Always declines -- a real MemoryAdmissionCoordinator wired to an evaluator that unconditionally rejects, proving buildAdmissionReply's own Declined mapping without contriving an unreachable evaluator state. */
    private fun decliningMemoryAdmissionCoordinator(): MemoryAdmissionCoordinator {
        val memoryCore = InMemoryMemoryCore()
        val permissionEngine = FakePermissionEngine { request -> approvedDecision(request) }
        val alwaysRejects = object : KnowledgeCandidateEvaluator {
            override fun evaluate(candidate: parker.core.interfaces.KnowledgeCandidate) =
                parker.core.interfaces.KnowledgeCandidateEvaluation.Reject("contrived rejection for test")
        }
        return MemoryAdmissionCoordinator(
            memoryCore,
            DefaultKnowledgeSubmission(alwaysRejects, InMemoryKnowledgeItemPersistence(), permissionEngine),
            permissionEngine,
        )
    }

    @Test
    fun `a Remember is routed to MemoryAdmissionCoordinator, and a successful admission produces I'll remember that, delivered as an ordinary Reply`() = runTest {
        val f = fixture(
            reasoningProvider = FakeReasoningProvider { ReasoningProviderResponse.Remember("my favourite coffee mug is black") },
            memoryAdmissionCoordinator = storingMemoryAdmissionCoordinator(),
        )
        val originalMessage = message(correlationId = "corr-remember-1")

        val outcome = f.coordinator.submitAndDeliver(originalMessage, ReasoningContext(emptyList()), fixedConversationId)

        val delivered = assertIs<ConversationOutcome.ReplyDelivered>(outcome)
        assertEquals(ExecutionResultStatus.SUCCESS, delivered.executionResult.status)
        assertEquals(mapOf(RESPONSE_TEXT_METADATA_KEY to "I'll remember that."), f.pipeline.lastSubmittedRequest?.metadata)
        // Never routed to planning.
        assertEquals(0, f.planningSessionIdFactoryCallCount())
    }

    @Test
    fun `a denied admission produces an honest not-authorised reply, never a false success claim`() = runTest {
        val f = fixture(
            reasoningProvider = FakeReasoningProvider { ReasoningProviderResponse.Remember("a fact") },
            memoryAdmissionCoordinator = notAuthorisedMemoryAdmissionCoordinator(),
        )

        val outcome = f.coordinator.submitAndDeliver(message(), ReasoningContext(emptyList()), fixedConversationId)

        assertIs<ConversationOutcome.ReplyDelivered>(outcome)
        val replyText = f.pipeline.lastSubmittedRequest?.metadata?.get(RESPONSE_TEXT_METADATA_KEY)
        assertTrue(replyText?.startsWith("I'm not able to store that right now:") == true, "actual: $replyText")
        assertTrue(replyText?.contains("I've stored that") != true, "must never claim success")
        assertTrue(replyText?.contains("I'll remember") != true, "must never claim success")
    }

    @Test
    fun `a declined submission produces an honest not-stored reply, never a false success claim`() = runTest {
        val f = fixture(
            reasoningProvider = FakeReasoningProvider { ReasoningProviderResponse.Remember("a fact") },
            memoryAdmissionCoordinator = decliningMemoryAdmissionCoordinator(),
        )

        val outcome = f.coordinator.submitAndDeliver(message(), ReasoningContext(emptyList()), fixedConversationId)

        assertIs<ConversationOutcome.ReplyDelivered>(outcome)
        val replyText = f.pipeline.lastSubmittedRequest?.metadata?.get(RESPONSE_TEXT_METADATA_KEY)
        assertTrue(replyText?.startsWith("I wasn't able to store that:") == true, "actual: $replyText")
        assertTrue(replyText?.contains("contrived rejection for test") == true, "the evaluator's own basis must be disclosed honestly")
    }

    @Test
    fun `the success reply is never composed until MemoryAdmissionCoordinator actually returns Stored -- the Reasoning Provider's own text is never used for this branch`() = runTest {
        val f = fixture(
            // A model could claim anything here; buildAdmissionReply must never use reasoningResponse.text
            // for a Remember branch -- only the governed outcome may determine the reply.
            reasoningProvider = FakeReasoningProvider { ReasoningProviderResponse.Remember("this text must never reach the owner directly") },
            memoryAdmissionCoordinator = storingMemoryAdmissionCoordinator(),
        )

        f.coordinator.submitAndDeliver(message(), ReasoningContext(emptyList()), fixedConversationId)

        val replyText = f.pipeline.lastSubmittedRequest?.metadata?.get(RESPONSE_TEXT_METADATA_KEY)
        assertEquals("I'll remember that.", replyText)
    }

    @Test
    fun `a Remember never reaches GoalPlanningHandoffCoordinator`() = runTest {
        val f = fixture(
            reasoningProvider = FakeReasoningProvider { ReasoningProviderResponse.Remember("a fact") },
            memoryAdmissionCoordinator = storingMemoryAdmissionCoordinator(),
            planningSessionIdFactory = { throw AssertionError("GoalPlanningHandoffCoordinator must not be called for Remember") },
        )

        val outcome = f.coordinator.submitAndDeliver(message(), ReasoningContext(emptyList()), fixedConversationId)

        assertIs<ConversationOutcome.ReplyDelivered>(outcome)
    }

    @Test
    fun `an exception thrown by MemoryAdmissionCoordinator propagates unchanged, and ReplyDeliveryCoordinator is never reached`() = runTest {
        val throwingCoordinator = MemoryAdmissionCoordinator(
            InMemoryMemoryCore(),
            DefaultKnowledgeSubmission(DefaultKnowledgeCandidateEvaluator(InMemoryMemoryCore()), InMemoryKnowledgeItemPersistence(), FakePermissionEngine { approvedDecision(it) }),
            FakePermissionEngine { throw IllegalStateException("simulated Permission Engine fault") },
        )
        val f = fixture(
            reasoningProvider = FakeReasoningProvider { ReasoningProviderResponse.Remember("a fact") },
            memoryAdmissionCoordinator = throwingCoordinator,
        )

        assertFailsWith<IllegalStateException> {
            f.coordinator.submitAndDeliver(message(), ReasoningContext(emptyList()), fixedConversationId)
        }
        assertEquals(0, f.identityService.resolveCallCount, "ReplyDeliveryCoordinator/ResponseComposer must never be reached once admission itself faults")
        assertEquals(0, f.pipeline.submitCallCount)
    }

    // ================= 4. Downstream NotAccepted (NoAction) =================

    @Test
    fun `a NoAction returns ResponseComposer's own NotAccepted unchanged, and ResponseDelivery is never entered`() = runTest {
        val f = fixture(reasoningProvider = FakeReasoningProvider { ReasoningProviderResponse.NoAction })

        val outcome = f.coordinator.submitAndDeliver(message(), ReasoningContext(emptyList()), fixedConversationId)

        val notAccepted = assertIs<ConversationOutcome.NotAccepted>(outcome)
        assertTrue("NoAction" in notAccepted.reason)
        assertEquals(0, f.identityService.resolveCallCount)
        assertEquals(0, f.resources.listByOwnerCallCount)
        assertEquals(0, f.pipeline.submitCallCount)
    }

    // ================= 5. Downstream NotAccepted (delivery-level rejection) =================

    @Test
    fun `a Reply that composes successfully but finds no channel Resource returns ResponseDelivery's own NotAccepted unchanged`() = runTest {
        val f = fixture(resources = FakeResourceRegistry { emptyList() })

        val outcome = f.coordinator.submitAndDeliver(message(), ReasoningContext(emptyList()), fixedConversationId)

        val notAccepted = assertIs<ConversationOutcome.NotAccepted>(outcome)
        assertTrue("no channel Resource found" in notAccepted.reason)
        assertEquals(1, f.identityService.resolveCallCount)
        assertEquals(1, f.resources.listByOwnerCallCount)
        assertEquals(0, f.pipeline.submitCallCount)
    }

    // ================= 6. Exact upstream and downstream call counts across sequential calls =================

    @Test
    fun `call counts across sequential calls increment only on their own applicable branch`() = runTest {
        val f = fixture(
            reasoningProvider = FakeReasoningProvider { request ->
                when ((request.subject as ReasoningSubject.OfTurn).turn.message.text) {
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
        assertEquals(0, f.planningSessionIdFactoryCallCount())

        f.coordinator.submitAndDeliver(message(text = "goal", correlationId = "corr-2"), ReasoningContext(emptyList()), fixedConversationId)
        assertEquals(2, f.communicationIntake.submitInboundMessageCallCount)
        assertEquals(2, f.reasoningProvider.reasonCallCount)
        assertEquals(1, f.identityService.resolveCallCount)
        assertEquals(1, f.resources.listByOwnerCallCount)
        assertEquals(1, f.pipeline.submitCallCount)
        assertEquals(1, f.planningSessionIdFactoryCallCount())

        f.coordinator.submitAndDeliver(message(text = "noaction", correlationId = "corr-3"), ReasoningContext(emptyList()), fixedConversationId)
        assertEquals(3, f.communicationIntake.submitInboundMessageCallCount)
        assertEquals(3, f.reasoningProvider.reasonCallCount)
        assertEquals(1, f.identityService.resolveCallCount)
        assertEquals(1, f.resources.listByOwnerCallCount)
        assertEquals(1, f.pipeline.submitCallCount)
        assertEquals(1, f.planningSessionIdFactoryCallCount())

        f.coordinator.submitAndDeliver(message(text = "reply-2", correlationId = "corr-4"), ReasoningContext(emptyList()), fixedConversationId)
        assertEquals(4, f.communicationIntake.submitInboundMessageCallCount)
        assertEquals(4, f.reasoningProvider.reasonCallCount)
        assertEquals(2, f.identityService.resolveCallCount)
        assertEquals(2, f.resources.listByOwnerCallCount)
        assertEquals(2, f.pipeline.submitCallCount)
        assertEquals(1, f.planningSessionIdFactoryCallCount())
    }

    // ================= 7. Downstream not called on upstream rejection (explicit) =================

    @Test
    fun `ReplyDeliveryCoordinator's and GoalPlanningHandoffCoordinator's own dependencies are never touched when CommunicationConversationCoordinator itself rejects`() = runTest {
        val f = fixture(
            communicationIntake = FakeCommunicationIntake { msg -> CommunicationIntakeDisposition.Rejected(msg.correlationId, "sender not resolved") },
        )

        f.coordinator.submitAndDeliver(message(), ReasoningContext(emptyList()), fixedConversationId)

        assertEquals(0, f.identityService.resolveCallCount)
        assertEquals(0, f.resources.listByOwnerCallCount)
        assertEquals(0, f.pipeline.submitCallCount)
        assertEquals(0, f.planningSessionIdFactoryCallCount())
    }

    // ================= 8. Sequencing evidence =================

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

    // ================= 9. Exception propagation from upstream =================

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

    // ================= 10. Exception propagation from downstream =================

    @Test
    fun `an exception thrown by ReplyDeliveryCoordinator's own dependencies propagates unchanged`() = runTest {
        val f = fixture(identityService = throwingIdentityService())

        assertFailsWith<IllegalStateException> {
            f.coordinator.submitAndDeliver(message(), ReasoningContext(emptyList()), fixedConversationId)
        }
        assertEquals(0, f.resources.listByOwnerCallCount)
        assertEquals(0, f.pipeline.submitCallCount)
    }

    // ================= 11. Exception propagation from GoalPlanningHandoffCoordinator =================

    @Test
    fun `an exception thrown by GoalPlanningHandoffCoordinator's own planningSessionIdFactory propagates unchanged, and ReplyDeliveryCoordinator is never reached`() = runTest {
        val f = fixture(
            reasoningProvider = FakeReasoningProvider { ReasoningProviderResponse.Goal("some goal") },
            planningSessionIdFactory = { throw IllegalStateException("factory boom") },
        )

        assertFailsWith<IllegalStateException> {
            f.coordinator.submitAndDeliver(message(), ReasoningContext(emptyList()), fixedConversationId)
        }
        assertEquals(0, f.identityService.resolveCallCount)
        assertEquals(0, f.resources.listByOwnerCallCount)
        assertEquals(0, f.pipeline.submitCallCount)
    }

    // ================= 12. Structural: constructor accepts exactly four dependencies =================

    @Test
    fun `the coordinator's constructor accepts exactly four dependencies -- CommunicationConversationCoordinator, ReplyDeliveryCoordinator, GoalPlanningHandoffCoordinator, and MemoryAdmissionCoordinator`() {
        val constructor = ConversationReplyCoordinator::class.java.declaredConstructors.single()
        val parameterTypes = constructor.parameterTypes.map { it.simpleName }.toSet()

        assertEquals(
            setOf("CommunicationConversationCoordinator", "ReplyDeliveryCoordinator", "GoalPlanningHandoffCoordinator", "MemoryAdmissionCoordinator"),
            parameterTypes,
        )
    }

    // ================= 13. Statelessness =================

    @Test
    fun `the coordinator declares no field beyond its four constructor-injected dependencies`() {
        val fieldNames = ConversationReplyCoordinator::class.java.declaredFields.map { it.name }.toSet()

        assertEquals(
            setOf("communicationConversationCoordinator", "replyDeliveryCoordinator", "goalPlanningHandoffCoordinator", "memoryAdmissionCoordinator"),
            fieldNames,
        )
    }

    // ================= 14. Real end-to-end test, narrower scope (FakeReasoningProvider, not ModelReasoningProvider) =================

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
        // selecting or configuring a model provider and validating live HTTP behaviour;
        // no live model server is required anywhere here.
        val reasoningProvider = FakeReasoningProvider { ReasoningProviderResponse.Reply("hello, owner") }
        val conversationTurnReasoningCoordinator = ConversationTurnReasoningCoordinator(conversationEngine, reasoningProvider)
        val communicationConversationCoordinator = CommunicationConversationCoordinator(communicationIntake, conversationTurnReasoningCoordinator)

        val composer = ResponseComposer(identityService)
        val replyDeliveryCoordinator = ReplyDeliveryCoordinator(composer, delivery)
        // This test's own Reply-only path never reaches GoalPlanningHandoffCoordinator's
        // dependencies -- both fakes below assert that by throwing if ever invoked.
        val goalPlanningHandoffCoordinator = GoalPlanningHandoffCoordinator(
            planningSessionIdFactory = { "planning-session-e2e-1" },
            planCandidateGenerator = fakePlanCandidateGenerator { throw AssertionError("must not be called for a Reply") },
            plannerRuntime = fakePlannerRuntime { _, _ -> throw AssertionError("must not be called for a Reply") },
        )

        val coordinator = ConversationReplyCoordinator(
            communicationConversationCoordinator,
            replyDeliveryCoordinator,
            goalPlanningHandoffCoordinator,
            defaultMemoryAdmissionCoordinator(),
        )

        val originalMessage = message(correlationId = "corr-e2e-1")
        // Resolution is a real, separate, upstream call in production (ParkerRuntime.submitOwnerMessage)
        // -- mirrored here explicitly rather than via a fake, since this test's own purpose is
        // real, end-to-end production wiring (item 12 above).
        val conversationId = conversationEngine.resolveConversationId(originalMessage)
        val outcome = coordinator.submitAndDeliver(originalMessage, ReasoningContext(emptyList()), conversationId)

        val replyDelivered = assertIs<ConversationOutcome.ReplyDelivered>(outcome)
        assertEquals(ExecutionResultStatus.SUCCESS, replyDelivered.executionResult.status)
        assertEquals(listOf("hello, owner"), delivered)
    }
}
