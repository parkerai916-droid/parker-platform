package parker.composition

import java.time.Instant
import java.util.UUID
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import parker.core.interfaces.ActionResourceMapping
import parker.core.interfaces.ActionVocabularyEntry
import parker.core.interfaces.AgentPolicy
import parker.core.interfaces.ConversationEngine
import parker.core.interfaces.ConversationHistorySource
import parker.core.interfaces.InboundOwnerMessage
import parker.core.interfaces.KnowledgeSource
import parker.core.interfaces.ModuleConnectivityDeclaration
import parker.core.interfaces.ModuleDescriptor
import parker.core.interfaces.ModuleId
import parker.core.interfaces.ModulePermissionRequirement
import parker.core.interfaces.PermissionAction
import parker.core.interfaces.PermissionDecisionOutcome
import parker.core.interfaces.PermissionLevel
import parker.core.interfaces.PlanningSessionResult
import parker.core.interfaces.Principal
import parker.core.interfaces.PrincipalId
import parker.core.interfaces.PrincipalStatus
import parker.core.interfaces.PrincipalType
import parker.core.interfaces.ReasoningContextAssembler
import parker.core.interfaces.ResolvedInboundMessage
import parker.core.interfaces.Resource
import parker.core.interfaces.ResourceId
import parker.core.interfaces.ResourceLifecycleState
import parker.core.interfaces.ResourceSensitivity
import parker.core.interfaces.ResourceType
import parker.core.interfaces.WorldModelSource
import parker.core.runtime.ActionMapper
import parker.core.runtime.CommunicationConversationCoordinator
import parker.core.runtime.ConversationOutcome
import parker.core.runtime.ConversationReplyCoordinator
import parker.core.runtime.ConversationTurnReasoningCoordinator
import parker.core.runtime.DefaultExecutionPipeline
import parker.core.runtime.DefaultPermissionEngine
import parker.core.runtime.DefaultPermissionPolicy
import parker.core.runtime.DefaultPlanCandidateGenerator
import parker.core.runtime.DefaultReasoningContextAssembler
import parker.core.runtime.DefaultReasoningPromptBuilder
import parker.core.runtime.DeterministicAgentStepSource
import parker.core.runtime.GoalPlanningHandoffCoordinator
import parker.core.runtime.GoalPlanningHandoffOutcome
import parker.core.runtime.InMemoryActionVocabulary
import parker.core.runtime.InMemoryAgentRuntime
import parker.core.runtime.InMemoryCommunicationIntake
import parker.core.runtime.InMemoryConversationEngine
import parker.core.runtime.InMemoryEventBus
import parker.core.runtime.InMemoryIdentityService
import parker.core.runtime.InMemoryKnowledgeStore
import parker.core.runtime.InMemoryModuleRegistry
import parker.core.runtime.InMemoryPlannerRuntime
import parker.core.runtime.InMemoryResourceRegistry
import parker.core.runtime.InMemoryTaskManagerRuntime
import parker.core.runtime.InMemoryToolInvocationBinding
import parker.core.runtime.InMemoryToolRegistry
import parker.core.runtime.InMemoryWorldModel
import parker.core.runtime.LocalHttpModelInferenceClient
import parker.core.runtime.LocalTextChannelDeliverTool
import parker.core.runtime.ModelReasoningProvider
import parker.core.runtime.PermissionPolicyRule
import parker.core.runtime.ReplyDeliveryCoordinator
import parker.core.runtime.ResponseComposer
import parker.core.runtime.ResponseDelivery
import parker.core.runtime.TaggedReasoningResponseParser

/**
 * [ParkerRuntime]'s own lifecycle, restated as an explicit, observable
 * state rather than left implicit in which fields happen to be
 * initialised. [start] drives `NOT_STARTED -> STARTING -> RUNNING`, or
 * `STARTING -> FAILED` on any startup fault; [shutdown] drives
 * `RUNNING -> STOPPING -> STOPPED`, or `STOPPING -> FAILED` on a shutdown
 * fault severe enough to report (see [ParkerRuntime.shutdown]'s own KDoc).
 * [ParkerRuntime.submitOwnerMessage] only accepts work while `RUNNING`.
 */
enum class RuntimeLifecycleState {
    NOT_STARTED,
    STARTING,
    RUNNING,
    STOPPING,
    STOPPED,
    FAILED,
}

/**
 * Sprint 10, Unit 4: Parker's first production composition root.
 *
 * **What this class is.** The single place in this repository that
 * constructs a complete, real runtime graph -- every dependency below is
 * a real, already-implemented, already-tested, frozen production
 * component (`docs/architecture/PARKER_ENGINEERING_STANDARD.md`'s own
 * "Architecture decides. Implementation follows." principle, applied here
 * as: this class decides nothing architectural; it only wires what
 * Sprints 1-10 already built). It introduces no new runtime behaviour, no
 * new coordinator, no new Trust decision, and no bypass of any existing
 * one.
 *
 * **What this class is not.** Not a coordinator, not a Tool, not a
 * runtime component in the sense every class under `src/runtime` already
 * is -- it holds no conversation-domain responsibility. Its own
 * responsibility is exactly, and only: dependency construction, dependency
 * ownership, lifecycle management (startup/shutdown sequencing),
 * configuration loading, and exposing one production entry point,
 * [submitOwnerMessage], that runs an inbound message through the complete,
 * unmodified, existing conversation pipeline.
 *
 * **Construction versus [start].** The constructor takes only [config],
 * [logger], and (optionally) [ownerNotificationSink]/[clock] --
 * [config] itself is produced separately, by [ParkerRuntimeConfigLoader],
 * from a caller-supplied environment map (never read by this class
 * directly). The constructor does not itself construct the runtime graph.
 * [start]
 * does that, explicitly, so construction (cheap, side-effect-free) and
 * startup (registers Principals, Modules, Tools, and vocabulary against
 * mutable in-memory state) remain observably distinct steps, each with
 * its own, separately reportable failure mode.
 *
 * **Explicit constructor injection throughout.** Every object this class
 * constructs receives every one of its own dependencies as a constructor
 * parameter -- nothing this class builds ever reaches for a global,
 * a singleton, a service locator, or constructs its own collaborator
 * internally. This is verified structurally by this Unit's own tests
 * (`tests/composition/ParkerRuntimeStartupAndShutdownTest.kt`) by
 * confirming every `Default*`/`InMemory*`/`Model*` constructor call site
 * below supplies 100% of that class's own declared constructor
 * parameters, none of them read from anywhere but this function's own
 * local variables and [config].
 *
 * **The policy-content decisions this class makes.**
 * `DefaultPermissionPolicy` requires a caller-supplied
 * `List<PermissionPolicyRule>` -- `IMPLEMENTATION_GAPS.md` #25 states
 * plainly that policy *content* "remains something a caller decides."
 * This composition root is the first real caller, and therefore the first
 * component in this repository that must supply one. It supplies exactly
 * two rules: `NOTIFY` on `TOOL` -> `APPROVED`/`AUTOMATIC` -- the minimum
 * required for the one Tool this runtime registers (the Local Text
 * Channel's `deliver` Tool) to be reachable at all, mirroring the
 * identical rule `ResponseDeliveryTest.kt`'s and
 * `LocalTextChannelDeliverToolTest.kt`'s own end-to-end tests already use
 * via `FakePermissionEngine`, now expressed as a real
 * `DefaultPermissionPolicy` rule instead of a test fake's canned decision
 * -- and, per Controlled Agent Run Submission
 * (`docs/implementation/CONTROLLED_AGENT_RUN_SUBMISSION_SCOPE_LOCK.md`
 * Section 3), `EXECUTE` on `AGENT` -> `APPROVED`/`AUTOMATIC`, the minimum
 * required for a legitimate owner-requested Agent Run `START` to be
 * authorised at all -- narrow in what it grants (creation and
 * commencement of the governed Agent Run only, never any action that run
 * later proposes) and scoped to the one Resource
 * [InMemoryAgentRuntime]'s run-initiation check ever targets
 * (`AGENT_RUNTIME_BOUNDARY_RESOURCE_ID`), not a general grant over every
 * `AGENT`-typed Resource that might ever exist. No other action/resource-type
 * pair is approved -- every other request this runtime's `PermissionEngine`
 * ever evaluates is `DENIED` by `DefaultPermissionPolicy`'s own
 * already-implemented, unmodified conservative default (`PermissionPolicy.md`
 * §7).
 */
class ParkerRuntime(
    private val config: ParkerRuntimeConfig,
    private val logger: ParkerLogger,
    private val ownerNotificationSink: OwnerNotificationSink = LoggingOwnerNotificationSink(logger),
    private val clock: () -> Instant = Instant::now,
) {

    private val stateLock = Mutex()
    var state: RuntimeLifecycleState = RuntimeLifecycleState.NOT_STARTED
        private set

    private lateinit var reasoningContextAssembler: ReasoningContextAssembler
    private lateinit var conversationEngine: ConversationEngine
    private lateinit var conversationReplyCoordinator: ConversationReplyCoordinator
    private lateinit var runtimeEventLogger: RuntimeEventLogger

    /**
     * Runs the full construction and startup sequence exactly once. Throws
     * a [ParkerRuntimeException] subtype on any failure -- never silently
     * swallows one (task instruction) -- and leaves [state] at
     * [RuntimeLifecycleState.FAILED] when it does, rather than leaving a
     * caller to infer failure from a missing side effect.
     *
     * **Startup sequence**, in order: (1) transition to `STARTING`, log
     * "Runtime starting"; (2) construct every stateless collaborator
     * (registries, event bus, action mapper, permission policy/engine,
     * execution pipeline); (2a, Sprint 11 Unit 6) construct
     * [ConversationEngine] (`InMemoryConversationEngine`) -- moved ahead of
     * the Assembler's own construction, since the Assembler now also
     * depends on this same instance under its narrower
     * `ConversationHistorySource` type; (2a-ii, Sprint 11 Unit 7) construct
     * `InMemoryKnowledgeStore` -- the first production construction of Memory
     * anywhere in this repository's real, running composition root
     * (`docs/architecture/MEMORY_SOURCE_GOVERNANCE_REVIEW.md` Finding 1);
     * (2a-iii, Sprint 11 Unit 8) construct `InMemoryWorldModel` -- the
     * first production construction of the World Model anywhere in this
     * repository's real, running composition root
     * (`docs/architecture/WORLD_MODEL_SOURCE_GOVERNANCE_REVIEW.md` Finding
     * 1); (2b, Sprint 11 Unit 3/6/7/8) construct
     * the [ReasoningContextAssembler] (`DefaultReasoningContextAssembler`),
     * injecting the already-constructed `identityService`, `toolRegistry`,
     * `conversationHistorySource`, `memorySource`, and `worldModelSource`;
     * (3) register and activate this runtime's
     * system Principals (`system.parker`, `system.conversation-engine`,
     * `system.response-composer`) and the configured owner Principal;
     * (3a, Controlled Agent Run Submission) register the single,
     * deterministic Agent Runtime Execution Boundary Resource; (4)
     * register the `notify owner` and (3a's companion, Controlled Agent
     * Run Submission) `start agent run` action-vocabulary entries; (5)
     * construct the Local Text Channel's `deliver` Tool, register+enable
     * its owning Module, and bind the Tool for invocation; (6) construct
     * the Reasoning Provider stack (real `LocalHttpModelInferenceClient`
     * against [ParkerRuntimeConfig.modelEndpointUrl]); (6a, Controlled
     * Agent Run Submission) construct `DeterministicAgentStepSource` and
     * `InMemoryAgentRuntime` -- now constructed ahead of
     * `InMemoryTaskManagerRuntime`, which submits every `AgentRunCommand`
     * it builds through it; (7) construct the
     * full coordinator chain
     * (`CommunicationConversationCoordinator -> ConversationTurnReasoningCoordinator`,
     * `ReplyDeliveryCoordinator -> ResponseComposer`/`ResponseDelivery`,
     * `GoalPlanningHandoffCoordinator` (Plan Candidate to PlannerRuntime
     * Integration -- now holds `PlanCandidateGenerator`/`PlannerRuntime`
     * references, backed by `DefaultPlanCandidateGenerator`/
     * `InMemoryPlannerRuntime`),
     * `ConversationReplyCoordinator`); (8) start [RuntimeEventLogger]'s
     * own EventBus subscriptions; (9) transition to `RUNNING`, log
     * "Runtime started".
     *
     * Any exception at steps (2)-(8) is caught, logged at `ERROR` with its
     * full stack trace, and re-thrown wrapped as
     * [ParkerRuntimeException.DependencyConstructionFailed] (naming the
     * step that failed) or [ParkerRuntimeException.StartupFailed] (for a
     * fault this method cannot attribute to one named step) -- [state] is
     * left at `FAILED` in either case, never left at `STARTING`.
     */
    suspend fun start() = stateLock.withLock {
        check(state == RuntimeLifecycleState.NOT_STARTED) {
            "ParkerRuntime.start() called while state=$state -- start() may only be called once, from NOT_STARTED"
        }
        state = RuntimeLifecycleState.STARTING
        logger.info("Runtime starting")

        try {
            buildAndRegisterRuntimeGraph()
            runtimeEventLogger.start()
            state = RuntimeLifecycleState.RUNNING
            logger.info("Runtime started")
        } catch (e: ParkerRuntimeException) {
            state = RuntimeLifecycleState.FAILED
            logger.error("Runtime failed to start: ${e.message}", e)
            throw e
        } catch (e: CancellationException) {
            state = RuntimeLifecycleState.FAILED
            throw e
        } catch (e: Exception) {
            state = RuntimeLifecycleState.FAILED
            logger.error("Runtime failed to start (unexpected fault)", e)
            throw ParkerRuntimeException.StartupFailed(e)
        }
    }

    @Suppress("LongMethod")
    private suspend fun buildAndRegisterRuntimeGraph() {
        val resourceRegistry = InMemoryResourceRegistry()
        val vocabulary = InMemoryActionVocabulary()
        val actionMapper = ActionMapper(vocabulary)
        val toolRegistry = InMemoryToolRegistry(resourceRegistry)
        val moduleRegistry = InMemoryModuleRegistry(toolRegistry, resourceRegistry)
        val toolInvocationBinding = InMemoryToolInvocationBinding()
        val eventBus = InMemoryEventBus()
        val identityService = InMemoryIdentityService()

        // Sprint 11 Unit 6: InMemoryConversationEngine must now be constructed before the
        // Reasoning Context Assembler (reversing Unit 3's original order), since the Assembler
        // now also depends on this same instance under its narrower ConversationHistorySource
        // type (CONVERSATION_HISTORY_SOURCE_CONTRACT_DESIGN.md Section 5). A pure ordering
        // change -- InMemoryConversationEngine's own constructor takes only identityService,
        // already available here.
        val inMemoryConversationEngine = InMemoryConversationEngine(identityService)
        conversationEngine = inMemoryConversationEngine
        val conversationHistorySource: ConversationHistorySource = inMemoryConversationEngine

        // Sprint 11 Unit 7: InMemoryKnowledgeStore is constructed here for the first time in this
        // repository's production composition root -- nowhere before this Unit did anything
        // construct KnowledgeStore/InMemoryKnowledgeStore in the real, running system (Governance
        // Review Finding 1). This is a new construction step, not a reordering: InMemoryKnowledgeStore
        // takes only its defaulted DefaultKnowledgePromotionPolicy, so no new ParkerRuntimeConfig
        // field or ordering constraint is introduced beyond existing before the Assembler.
        val inMemoryMemoryStore = InMemoryKnowledgeStore()
        val memorySource: KnowledgeSource = inMemoryMemoryStore

        // Sprint 11 Unit 8: InMemoryWorldModel is constructed here for the first time in this
        // repository's production composition root -- nowhere before this Unit did anything
        // construct WorldModel/InMemoryWorldModel in the real, running system (Governance Review
        // Finding 1), the identical situation Memory Source Integration (Unit 7) faced. This is a
        // new construction step, not a reordering: InMemoryWorldModel takes only its defaulted
        // DefaultWorldModelUpdatePolicy, so no new ParkerRuntimeConfig field or ordering
        // constraint is introduced beyond existing before the Assembler. Exactly one instance is
        // constructed and exposed to the Assembler only through the narrower WorldModelSource
        // type -- no duplicate ownership, no duplicate state, mirroring precisely how
        // InMemoryKnowledgeStore/InMemoryConversationEngine are each constructed once and exposed
        // through two interfaces on the same instance.
        val inMemoryWorldModel = InMemoryWorldModel()
        val worldModelSource: WorldModelSource = inMemoryWorldModel

        reasoningContextAssembler = stage("Reasoning Context Assembler construction") {
            DefaultReasoningContextAssembler(identityService, toolRegistry, conversationHistorySource, memorySource, worldModelSource)
        }

        registerSystemIdentities(identityService)

        // Controlled Agent Run Submission (Scope Lock Section 4): a single, deterministic,
        // pre-registered Resource representing the Agent Runtime's own execution boundary --
        // never a specific Agent Run, which does not exist until InMemoryAgentRuntime.start()
        // creates one, strictly after the run-initiation permission check this Resource backs
        // succeeds. Registered once, at startup, before any TaskProposal is ever submitted.
        stage("agent runtime boundary resource registration") {
            val now = clock()
            resourceRegistry.register(
                Resource(
                    resourceId = AGENT_RUNTIME_BOUNDARY_RESOURCE_ID,
                    resourceType = ResourceType.AGENT,
                    displayName = "Agent Runtime Execution Boundary",
                    ownerPrincipalId = SYSTEM_PARKER_PRINCIPAL_ID,
                    sensitivity = ResourceSensitivity.PUBLIC,
                    lifecycleState = ResourceLifecycleState.REGISTERED,
                    createdAt = now,
                    updatedAt = now,
                    source = "composition-root:agent-runtime-boundary",
                ),
            )
        }

        stage("action vocabulary registration") {
            vocabulary.register(
                ActionVocabularyEntry(
                    verbPhrase = NOTIFY_OWNER_VERB_PHRASE,
                    mappings = setOf(ActionResourceMapping(PermissionAction.NOTIFY, ResourceType.TOOL)),
                ),
            )
            // Controlled Agent Run Submission (Scope Lock Section 3.1): the one production
            // ActionVocabulary entry backing the new run-initiation permission check.
            vocabulary.register(
                ActionVocabularyEntry(
                    verbPhrase = AGENT_RUN_START_VERB_PHRASE,
                    mappings = setOf(ActionResourceMapping(PermissionAction.EXECUTE, ResourceType.AGENT)),
                ),
            )
        }

        val permissionPolicy = DefaultPermissionPolicy(
            actionMapper = actionMapper,
            resourceRegistry = resourceRegistry,
            rules = listOf(
                PermissionPolicyRule(
                    action = PermissionAction.NOTIFY,
                    resourceType = ResourceType.TOOL,
                    outcome = PermissionDecisionOutcome.APPROVED,
                    level = PermissionLevel.AUTOMATIC,
                ),
                // Controlled Agent Run Submission (Scope Lock Section 3.2-3.4): permits
                // creation and commencement of a governed Agent Run only -- it does not
                // authorise any action that Agent Run later proposes, which remains
                // independently evaluated by this same PermissionEngine via ExecutionPipeline,
                // unchanged. Owner-scoping is enforced by call-site structure (Scope Lock
                // Section 3.3), not by this rule's own matching logic: InMemoryTaskManagerRuntime
                // is the sole production caller of AgentRunCommandChannel.submit, and it always
                // sets requestingPrincipalId to the Task's own resolved owner.
                PermissionPolicyRule(
                    action = PermissionAction.EXECUTE,
                    resourceType = ResourceType.AGENT,
                    outcome = PermissionDecisionOutcome.APPROVED,
                    level = PermissionLevel.AUTOMATIC,
                ),
            ),
        )
        val permissionEngine = DefaultPermissionEngine(identityService, permissionPolicy)
        val executionPipeline = DefaultExecutionPipeline(
            resourceRegistry,
            actionMapper,
            permissionEngine,
            toolRegistry,
            eventBus,
            toolInvocationBinding,
        )

        val deliverTool = stage("Local Text Channel deliver Tool construction") {
            LocalTextChannelDeliverTool(onOwnerNotified = ownerNotificationSink::notify)
        }
        stage("Local Text Channel module registration") {
            val moduleId = ModuleId(config.localTextChannelModuleId)
            val moduleDescriptor = ModuleDescriptor(
                moduleId = moduleId,
                name = "Local Text Channel",
                version = "0.1.0",
                toolsExposed = listOf(deliverTool.descriptor),
                requiredPermissions = listOf(ModulePermissionRequirement(PermissionAction.NOTIFY, ResourceType.TOOL)),
                connectivityDeclaration = ModuleConnectivityDeclaration.LOCAL_ONLY,
            )
            moduleRegistry.register(moduleDescriptor)
            moduleRegistry.enable(moduleId, SYSTEM_PARKER_PRINCIPAL_ID)
            toolInvocationBinding.bind(deliverTool.descriptor, deliverTool)
        }

        val communicationIntake = LoggingCommunicationIntake(
            InMemoryCommunicationIntake(moduleRegistry, identityService),
            logger,
        )

        val reasoningProvider = stage("Reasoning Provider construction") {
            LoggingReasoningProvider(
                ModelReasoningProvider(
                    promptBuilder = DefaultReasoningPromptBuilder(),
                    modelInferenceClient = LocalHttpModelInferenceClient(config.modelEndpointUrl, config.modelName),
                    responseParser = TaggedReasoningResponseParser(),
                    timeoutMs = config.modelTimeoutMs,
                ),
                logger,
            )
        }

        val conversationTurnReasoningCoordinator = ConversationTurnReasoningCoordinator(conversationEngine, reasoningProvider)
        val communicationConversationCoordinator = CommunicationConversationCoordinator(communicationIntake, conversationTurnReasoningCoordinator)

        val responseComposer = ResponseComposer(identityService)
        val responseDelivery = ResponseDelivery(resourceRegistry, executionPipeline)
        val replyDeliveryCoordinator = ReplyDeliveryCoordinator(responseComposer, responseDelivery)

        // Controlled Agent Run Submission (Scope Lock Sections 4, 9): InMemoryAgentRuntime must
        // now be constructed before InMemoryTaskManagerRuntime, reversing the prior ordering
        // assumption that the Task Manager Runtime had no runtime-side dependency. permissionEngine
        // is the same instance already composed into executionPipeline above -- one Permission
        // Engine for the whole runtime, not two independently configured ones.
        val deterministicAgentStepSource = DeterministicAgentStepSource()
        val agentRuntime = InMemoryAgentRuntime(
            identityService = identityService,
            executionPipeline = executionPipeline,
            eventBus = eventBus,
            agentStepSource = deterministicAgentStepSource,
            agentPolicy = DEFAULT_AGENT_POLICY,
            permissionEngine = permissionEngine,
            runInitiationResourceId = AGENT_RUNTIME_BOUNDARY_RESOURCE_ID,
            runInitiationVerbPhrase = AGENT_RUN_START_VERB_PHRASE,
        )

        // Plan Candidate to PlannerRuntime Integration: InMemoryTaskManagerRuntime is also
        // constructed to satisfy InMemoryPlannerRuntime's mandatory TaskProposalIntake
        // constructor parameter (docs/implementation/
        // PLAN_CANDIDATE_TO_PLANNER_INTEGRATION_SCOPE_LOCK.md Section 9). Controlled Agent Run
        // Submission updates the claim this comment previously made: InMemoryTaskManagerRuntime
        // now genuinely submits the AgentRunCommand it constructs, via agentRuntime above, which
        // is the sole production AgentRunCommandChannel implementation (Scope Lock decision 2).
        //
        // Two-Phase Acceptance/Execution Amendment (Scope Lock Amendment, A.2): the same
        // agentRuntime instance is also passed as the new agentRunExecutionTrigger argument --
        // InMemoryAgentRuntime implements both AgentRunCommandChannel and
        // AgentRunExecutionTrigger; no second implementation exists or is composed here.
        val taskManagerRuntime = InMemoryTaskManagerRuntime(identityService, eventBus, agentRuntime, agentRuntime)
        val plannerRuntime = InMemoryPlannerRuntime(identityService, eventBus, taskManagerRuntime)
        val planCandidateGenerator = DefaultPlanCandidateGenerator()

        // GoalPlanningHandoffCoordinator's constructor now also takes planCandidateGenerator
        // and plannerRuntime (Scope Lock Section 1) -- planningSessionIdFactory continues to be
        // supplied explicitly here, not defaulted inside the coordinator itself.
        val goalPlanningHandoffCoordinator = GoalPlanningHandoffCoordinator(
            planningSessionIdFactory = { UUID.randomUUID().toString() },
            planCandidateGenerator = planCandidateGenerator,
            plannerRuntime = plannerRuntime,
        )

        conversationReplyCoordinator = ConversationReplyCoordinator(
            communicationConversationCoordinator,
            replyDeliveryCoordinator,
            goalPlanningHandoffCoordinator,
        )
        runtimeEventLogger = RuntimeEventLogger(eventBus, logger, SYSTEM_PARKER_PRINCIPAL_ID)
    }

    private suspend fun registerSystemIdentities(identityService: InMemoryIdentityService) {
        stage("system identity registration") {
            registerActive(identityService, SYSTEM_PARKER_PRINCIPAL_ID, PrincipalType.SYSTEM, "Parker System")
            registerActive(identityService, CONVERSATION_ENGINE_PRINCIPAL_ID, PrincipalType.SYSTEM, "Conversation Engine")
            registerActive(identityService, RESPONSE_COMPOSER_PRINCIPAL_ID, PrincipalType.SYSTEM, "Response Composer")
            registerActive(identityService, PLANNER_RUNTIME_PRINCIPAL_ID, PrincipalType.SYSTEM, "Planner Runtime")
            registerActive(identityService, TASK_MANAGER_RUNTIME_PRINCIPAL_ID, PrincipalType.SYSTEM, "Task Manager Runtime")
            registerActive(
                identityService,
                PrincipalId(config.ownerPrincipalId),
                PrincipalType.USER,
                config.ownerDisplayName,
            )
        }
    }

    private suspend fun registerActive(
        identityService: InMemoryIdentityService,
        principalId: PrincipalId,
        principalType: PrincipalType,
        displayName: String,
    ) {
        val now = clock()
        identityService.register(
            Principal(
                principalId = principalId,
                principalType = principalType,
                displayName = displayName,
                owner = null,
                status = PrincipalStatus.CREATED,
                createdAt = now,
                lastSeenAt = now,
            ),
        )
        identityService.updateStatus(principalId, PrincipalStatus.ACTIVE)
    }

    /**
     * The runtime's one production entry point: accepts an inbound
     * communication and executes the complete, existing conversation
     * pipeline -- `CommunicationIntake -> ConversationEngine ->
     * ReasoningProvider`, then one of two branches depending on the
     * resulting `ReasoningProviderResponse`: `Reply`/`NoAction` continue
     * through `ResponseComposer -> ResponseDelivery -> ExecutionPipeline
     * -> Tool execution`, with Trust authorisation (`PermissionEngine`,
     * via `ExecutionPipeline`) mandatory on that delivery path; `Goal` is
     * routed instead to `GoalPlanningHandoffCoordinator`, which never
     * reaches `ResponseComposer`, `ExecutionPipeline`, or
     * `PermissionEngine`, but which does now genuinely invoke
     * `PlannerRuntime.plan()` (Plan Candidate to PlannerRuntime
     * Integration). Both branches are exactly as
     * [ConversationReplyCoordinator.submitAndDeliver] (this method's own
     * sole delegate) already guarantees on its own, unmodified terms.
     *
     * **Reasoning Context assembly (Sprint 11, Unit 3).** This method
     * invokes [reasoningContextAssembler]`.assemble(...)` exactly once, as
     * the first action it takes after confirming
     * [RuntimeLifecycleState.RUNNING] and before its one call to
     * [ConversationReplyCoordinator.submitAndDeliver]. The resulting
     * `ReasoningContext` is passed, unchanged, into `submitAndDeliver`.
     * Logs one `INFO` line, "Reasoning Context assembled
     * (correlationId=...)", immediately after the call -- mirroring
     * `CommunicationConversationCoordinator`'s "Conversation accepted"
     * and `ModelReasoningProvider`'s "Reasoning completed" INFO logs this
     * method's own caller-facing pipeline already relies on for
     * observability.
     *
     * **Conversation continuity resolution (Sprint 11, Unit 5 --
     * Conversation Continuity Implementation).** Before assembling
     * `ReasoningContext` at all, this method now calls
     * [conversationEngine]`.resolveConversationId(message)` exactly
     * once -- the one authoritative continuity decision for this inbound
     * message (`docs/architecture/CONVERSATION_CONTINUITY_CONTRACT_DESIGN.md`
     * ("the Continuity Contract Design") Section 5, propagation path,
     * step 1). This method makes no decision of its own here: it only
     * invokes the authority ([ConversationEngine] alone decides continuity
     * and mints identifiers) and carries the result forward. Logs one
     * `INFO` line, "Conversation continuity resolved (correlationId=...,
     * conversationId=...)", immediately after the call -- mirroring the
     * "Reasoning Context assembled" precedent above, and giving
     * `tests/composition` a direct way to verify resolution happens
     * exactly once, and before assembly, without a test-only constructor
     * parameter. The resolved
     * [parker.core.interfaces.ConversationId] is wrapped, together with
     * the original, unmutated `message`, into a
     * [ResolvedInboundMessage] (Continuity Contract Design Section 6) --
     * constructed exactly once per inbound message, by this method alone
     * -- which becomes the Assembler's own input. This same resolved
     * identifier is then forwarded, unchanged, as an additional argument
     * to [ConversationReplyCoordinator.submitAndDeliver], propagating
     * through the unchanged coordinator chain to
     * [ConversationEngine.submitTurn] (Continuity Contract Design Section
     * 5, propagation path, steps 2-7) -- never recomputed, never
     * re-resolved, anywhere downstream of this one call.
     *
     * **Resolution failure (Continuity Contract Design Section 5.1,
     * Guarantee 4).** If `resolveConversationId` throws, this method
     * constructs no [ResolvedInboundMessage], never invokes the
     * Assembler, and the fault falls straight through to this method's
     * own existing outer `try`/`catch` below, exactly as any other
     * pipeline-stage fault already does -- reported as
     * [ParkerRuntimeOutcome.Failed] with [PipelineStage.UNKNOWN], never
     * silently converted into "begin a new Conversation instead."
     *
     * Throws [ParkerRuntimeException.NotRunning] if [state] is not
     * [RuntimeLifecycleState.RUNNING].
     *
     * **Production failure handling, this method's own added
     * responsibility.** No coordinator between [ConversationReplyCoordinator]
     * and the model/Tool call sites catches anything itself -- confirmed
     * directly against `CommunicationConversationCoordinator`,
     * `ConversationTurnReasoningCoordinator`, `ReplyDeliveryCoordinator`,
     * `ResponseComposer`, and `ResponseDelivery`'s own Scope Locks, each of
     * which states plainly that an exception "propagates unchanged" to its
     * own caller, and by direct reading of
     * `DefaultExecutionPipeline.executeResolvedTool`, which calls
     * `Tool.execute(request)` with no surrounding `try`/`catch` of its
     * own. This method is therefore the correct, and only correct, place
     * in this repository's own existing architecture for a genuine
     * runtime fault ("model unavailable," "tool failure," "coordinator
     * failure") to be caught at all, per this Unit's own task instructions.
     * A fault is reported as [ParkerRuntimeOutcome.Failed], never silently
     * swallowed -- always logged at `ERROR` with its real cause attached.
     *
     * `kotlinx.coroutines.TimeoutCancellationException` (a
     * `CancellationException` subtype `ModelReasoningProvider`'s own
     * `withTimeout` call throws on a real model timeout) is deliberately
     * caught and reported as [ParkerRuntimeOutcome.Failed] with
     * [PipelineStage.REASONING] -- not rethrown as an ordinary
     * `CancellationException` would be, since doing so would incorrectly
     * cancel this method's own enclosing coroutine scope for what is, in
     * truth, an ordinary "the model was unavailable" operational failure,
     * not a real cancellation request. Every other `CancellationException`
     * (a genuine coroutine cancellation, e.g. structured-concurrency scope
     * shutdown) is rethrown unchanged, never swallowed, never reported as
     * [ParkerRuntimeOutcome.Failed].
     */
    suspend fun submitOwnerMessage(message: InboundOwnerMessage): ParkerRuntimeOutcome {
        if (state != RuntimeLifecycleState.RUNNING) {
            throw ParkerRuntimeException.NotRunning(state)
        }

        return try {
            val conversationId = conversationEngine.resolveConversationId(message)
            logger.info("Conversation continuity resolved (correlationId=${message.correlationId.value}, conversationId=${conversationId.value})")
            val resolvedMessage = ResolvedInboundMessage(message, conversationId)
            val reasoningContext = reasoningContextAssembler.assemble(resolvedMessage)
            logger.info("Reasoning Context assembled (correlationId=${message.correlationId.value})")
            when (val outcome = conversationReplyCoordinator.submitAndDeliver(message, reasoningContext, conversationId)) {
                is ConversationOutcome.NotAccepted -> {
                    logger.info("Conversation not accepted for delivery (correlationId=${message.correlationId.value}, reason=${outcome.reason})")
                    ParkerRuntimeOutcome.NotAccepted(outcome.reason)
                }
                is ConversationOutcome.ReplyDelivered -> {
                    logger.info("Conversation pipeline completed (correlationId=${message.correlationId.value}, status=${outcome.executionResult.status})")
                    ParkerRuntimeOutcome.Delivered(outcome.executionResult)
                }
                is ConversationOutcome.Planned -> when (val handoffOutcome = outcome.outcome) {
                    is GoalPlanningHandoffOutcome.Planned -> {
                        val sessionResult = handoffOutcome.planningSessionResult
                        // PlanningSessionResult itself declares no common planningSessionId member --
                        // each variant (Completed/Rejected/Failed) independently declares its own,
                        // identically-named field, not an `override` of anything on the sealed
                        // supertype (src/contracts/PlanDecision.kt). This `when` exists solely to
                        // extract that shared logging identifier; outcome mapping below is untouched
                        // and remains uniform across all three variants.
                        val planningSessionId = when (sessionResult) {
                            is PlanningSessionResult.Completed -> sessionResult.planningSessionId
                            is PlanningSessionResult.Rejected -> sessionResult.planningSessionId
                            is PlanningSessionResult.Failed -> sessionResult.planningSessionId
                        }
                        logger.info(
                            "Planning attempted (correlationId=${message.correlationId.value}, " +
                                "planningSessionId=${planningSessionId.value}, " +
                                "result=${sessionResult::class.simpleName})",
                        )
                        ParkerRuntimeOutcome.Planned(handoffOutcome)
                    }
                }
            }
        } catch (e: TimeoutCancellationException) {
            logger.error("Reasoning Provider timed out (correlationId=${message.correlationId.value})", e)
            ParkerRuntimeOutcome.Failed(PipelineStage.REASONING, e)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            logger.error("Conversation pipeline fault (correlationId=${message.correlationId.value})", e)
            ParkerRuntimeOutcome.Failed(PipelineStage.UNKNOWN, e)
        }
    }

    /**
     * Graceful shutdown: cancels every [RuntimeEventLogger] subscription,
     * transitions to [RuntimeLifecycleState.STOPPED], and logs "Runtime
     * shutting down" / "Runtime stopped". Best-effort, not
     * fail-fast-and-abort: if a shutdown step throws, this method logs it
     * at `ERROR`, continues attempting any remaining step, and only then
     * throws [ParkerRuntimeException.ShutdownFailed] wrapping the first
     * failure encountered -- so one failing step never prevents another,
     * independent step's own cleanup from being attempted.
     *
     * Safe to call from [RuntimeLifecycleState.RUNNING] or
     * [RuntimeLifecycleState.FAILED] (a runtime that failed to start may
     * still hold live resources needing cleanup, e.g. a partially-started
     * [RuntimeEventLogger]); throws [IllegalStateException] if called from
     * [RuntimeLifecycleState.NOT_STARTED], [RuntimeLifecycleState.STOPPING],
     * or [RuntimeLifecycleState.STOPPED] -- there is nothing meaningful to
     * shut down, or shutdown is already in progress/complete.
     */
    suspend fun shutdown() = stateLock.withLock {
        check(state == RuntimeLifecycleState.RUNNING || state == RuntimeLifecycleState.FAILED) {
            "ParkerRuntime.shutdown() called while state=$state -- shutdown() requires RUNNING or FAILED"
        }
        state = RuntimeLifecycleState.STOPPING
        logger.info("Runtime shutting down")

        var firstFailure: Throwable? = null
        if (::runtimeEventLogger.isInitialized) {
            try {
                runtimeEventLogger.stop()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                logger.error("Runtime Event Logger failed to stop cleanly", e)
                firstFailure = e
            }
        }

        state = if (firstFailure == null) RuntimeLifecycleState.STOPPED else RuntimeLifecycleState.FAILED
        logger.info("Runtime stopped")

        firstFailure?.let { throw ParkerRuntimeException.ShutdownFailed(it) }
    }

    private companion object {
        const val NOTIFY_OWNER_VERB_PHRASE = "notify owner"
        val SYSTEM_PARKER_PRINCIPAL_ID = PrincipalId("system.parker")
        val CONVERSATION_ENGINE_PRINCIPAL_ID = PrincipalId("system.conversation-engine")
        val RESPONSE_COMPOSER_PRINCIPAL_ID = PrincipalId("system.response-composer")

        // Plan Candidate to PlannerRuntime Integration: these two literal string values must
        // exactly match InMemoryPlannerRuntime's and InMemoryTaskManagerRuntime's own private
        // companion-object constants of the same name (PrincipalId is a value class, so identity
        // resolution is by string-value equality) -- duplicated here, not imported, following the
        // same composition-root-owns-its-own-copy convention already established by the three
        // constants above.
        val PLANNER_RUNTIME_PRINCIPAL_ID = PrincipalId("system.planner-runtime")
        val TASK_MANAGER_RUNTIME_PRINCIPAL_ID = PrincipalId("system.task-manager-runtime")

        // Controlled Agent Run Submission (docs/implementation/
        // CONTROLLED_AGENT_RUN_SUBMISSION_SCOPE_LOCK.md Sections 3-4, 9): the verb phrase and
        // the Resource identifying the Agent Runtime's own execution boundary are both owned by
        // this private companion object, and are threaded into InMemoryAgentRuntime's
        // constructor explicitly (runInitiationVerbPhrase, runInitiationResourceId) rather than
        // duplicated as a second literal or exposed via loosened visibility -- the same
        // resolution this project's own Plan Candidate to PlannerRuntime Integration Scope Lock
        // already established for system-identity literals, applied here to a Resource and a
        // verb phrase instead of a PrincipalId.
        const val AGENT_RUN_START_VERB_PHRASE = "start agent run"
        val AGENT_RUNTIME_BOUNDARY_RESOURCE_ID = ResourceId("resource-agent-runtime-boundary")

        // Scope Lock Section 9: a Sprint-phase default, not a permanent or authoritative
        // production value -- no specification currently authorises a specific maxAgentSteps
        // figure. 10 is promoted here explicitly from the only existing precedent for it
        // (tests/runtime/SingleStepAgentStepSource.kt's own DEFAULT_AGENT_POLICY), rather than
        // silently reused.
        val DEFAULT_AGENT_POLICY = AgentPolicy(maxAgentSteps = 10)
    }
}

/**
 * Runs [block], naming this construction/registration step so a failure
 * is diagnosable without reading a stack trace first (see
 * [ParkerRuntimeException.DependencyConstructionFailed]'s own KDoc).
 *
 * **Post-verification correction (Android Studio, Sprint 10 Unit 4) --
 * corrected a second time; see below.**
 *
 * 1. **Extracted from a `private` member of [ParkerRuntime] to this
 *    top-level, `internal` function**, so `tests/composition`'s own
 *    focused cancellation-semantics regression test
 *    (`StageCancellationTest.kt`) can exercise it directly and
 *    deterministically, without needing to force a genuine mid-construction
 *    cancellation race against [ParkerRuntime.start] as a whole. Gradle's
 *    Kotlin plugin makes this module's `tests/composition` compilation a
 *    friend of `src/composition`'s, so `internal` visibility -- not
 *    `public` -- is sufficient and correct here; this is not a new public
 *    contract.
 * 2. **A genuine `CancellationException` thrown from within [block] is
 *    rethrown unchanged, before the general `Exception` catch below.**
 *    Previously, `catch (e: Exception)` caught and wrapped *every*
 *    exception, including `CancellationException` (a subtype of
 *    `Exception`, via `IllegalStateException`) -- meaning a real coroutine
 *    cancellation occurring mid-[ParkerRuntime.start] would have been
 *    misreported as `ParkerRuntimeException.DependencyConstructionFailed`
 *    rather than propagating as the genuine cancellation it is, violating
 *    structured-concurrency cancellation semantics. This mirrors the
 *    identical, already-correct ordering [ParkerRuntime.start]'s own outer
 *    `try`/`catch` and [ParkerRuntime.shutdown]'s own already use.
 *
 * **Correction history on the `suspend` modifier -- first attempt was
 * wrong, recorded here rather than silently replaced.** The first version
 * of this extraction dropped the outer `suspend` keyword, on the reasoning
 * that an `inline` function's only suspension point (invoking the
 * `suspend`-typed [block] parameter) is spliced into the caller's own
 * suspend context, so the enclosing function need not itself be `suspend`.
 * **That reasoning was incorrect for this specific shape** and did not
 * compile: Android Studio reported a hard error --
 * `Suspend inline lambda parameters of non-suspend function type are not
 * supported`, together with `Suspended function 'invoke' should be called
 * only from a coroutine or another suspend function` at the `block()`
 * call site. Calling a `suspend`-typed parameter (`block()`) is itself a
 * suspending call requiring a `Continuation` to be threaded through; only
 * a `suspend` function provides one to its own body. Inlining removes the
 * separate stack frame/lambda object, but does not remove that
 * requirement -- it is not equivalent to the caller's own suspend context
 * "reaching through." This means the original claim in this KDoc (now
 * removed) -- that the outer `suspend` modifier was redundant -- was
 * itself wrong, and should not be repeated. This project does not have
 * the literal original Android Studio warning text on file (only a
 * paraphrase, "redundant suspend modifier," relayed secondhand); given
 * that removing the modifier produces a hard compile error rather than a
 * suppressible warning in this exact configuration, that paraphrase most
 * likely referred to something else near this line, not to `stage`'s own
 * enclosing `suspend` keyword -- but this is not re-diagnosed further
 * here, since doing so without the literal compiler text would be
 * speculation, not finding. The correct, compiling, semantics-preserving
 * form is restored below: `stage` **is** `suspend`, and [block] is
 * `crossinline` so it can still be inlined directly into `try` while
 * remaining callable exactly as it was before either correction.
 */
internal suspend inline fun <T> stage(name: String, crossinline block: suspend () -> T): T = try {
    block()
} catch (e: ParkerRuntimeException) {
    throw e
} catch (e: CancellationException) {
    throw e
} catch (e: Exception) {
    throw ParkerRuntimeException.DependencyConstructionFailed(name, e)
}
