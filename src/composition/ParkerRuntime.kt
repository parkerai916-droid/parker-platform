package parker.composition

import java.time.Instant
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import parker.core.interfaces.ActionResourceMapping
import parker.core.interfaces.ActionVocabularyEntry
import parker.core.interfaces.InboundOwnerMessage
import parker.core.interfaces.ModuleConnectivityDeclaration
import parker.core.interfaces.ModuleDescriptor
import parker.core.interfaces.ModuleId
import parker.core.interfaces.ModulePermissionRequirement
import parker.core.interfaces.PermissionAction
import parker.core.interfaces.PermissionDecisionOutcome
import parker.core.interfaces.PermissionLevel
import parker.core.interfaces.Principal
import parker.core.interfaces.PrincipalId
import parker.core.interfaces.PrincipalStatus
import parker.core.interfaces.PrincipalType
import parker.core.interfaces.ReasoningContextAssembler
import parker.core.interfaces.ResourceType
import parker.core.runtime.ActionMapper
import parker.core.runtime.CommunicationConversationCoordinator
import parker.core.runtime.ConversationReplyCoordinator
import parker.core.runtime.ConversationTurnReasoningCoordinator
import parker.core.runtime.DefaultExecutionPipeline
import parker.core.runtime.DefaultPermissionEngine
import parker.core.runtime.DefaultPermissionPolicy
import parker.core.runtime.DefaultReasoningContextAssembler
import parker.core.runtime.DefaultReasoningPromptBuilder
import parker.core.runtime.GatedOutcome
import parker.core.runtime.InMemoryActionVocabulary
import parker.core.runtime.InMemoryCommunicationIntake
import parker.core.runtime.InMemoryConversationEngine
import parker.core.runtime.InMemoryEventBus
import parker.core.runtime.InMemoryIdentityService
import parker.core.runtime.InMemoryModuleRegistry
import parker.core.runtime.InMemoryResourceRegistry
import parker.core.runtime.InMemoryToolInvocationBinding
import parker.core.runtime.InMemoryToolRegistry
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
 * **The one real policy-content decision this class makes.**
 * `DefaultPermissionPolicy` requires a caller-supplied
 * `List<PermissionPolicyRule>` -- `IMPLEMENTATION_GAPS.md` #25 states
 * plainly that policy *content* "remains something a caller decides."
 * This composition root is the first real caller, and therefore the first
 * component in this repository that must supply one. It supplies exactly
 * one rule: `NOTIFY` on `TOOL` -> `APPROVED`/`AUTOMATIC` -- the minimum
 * required for the one Tool this runtime registers (the Local Text
 * Channel's `deliver` Tool) to be reachable at all, mirroring the
 * identical rule `ResponseDeliveryTest.kt`'s and
 * `LocalTextChannelDeliverToolTest.kt`'s own end-to-end tests already use
 * via `FakePermissionEngine`, now expressed as a real
 * `DefaultPermissionPolicy` rule instead of a test fake's canned decision.
 * No other action/resource-type pair is approved -- every other request
 * this runtime's `PermissionEngine` ever evaluates is `DENIED` by
 * `DefaultPermissionPolicy`'s own already-implemented, unmodified
 * conservative default (`PermissionPolicy.md` §7).
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
     * execution pipeline); (2a, Sprint 11 Unit 3) construct the
     * [ReasoningContextAssembler] (`DefaultReasoningContextAssembler`),
     * injecting the already-constructed `identityService` and
     * `toolRegistry`; (3) register and activate this runtime's
     * system Principals (`system.parker`, `system.conversation-engine`,
     * `system.response-composer`) and the configured owner Principal; (4)
     * register the `notify owner` action-vocabulary entry; (5) construct
     * the Local Text Channel's `deliver` Tool, register+enable its owning
     * Module, and bind the Tool for invocation; (6) construct the
     * Reasoning Provider stack (real `LocalHttpModelInferenceClient`
     * against [ParkerRuntimeConfig.modelEndpointUrl]); (7) construct the
     * full coordinator chain
     * (`CommunicationConversationCoordinator -> ConversationTurnReasoningCoordinator`,
     * `ReplyDeliveryCoordinator -> ResponseComposer`/`ResponseDelivery`,
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

        reasoningContextAssembler = stage("Reasoning Context Assembler construction") {
            DefaultReasoningContextAssembler(identityService, toolRegistry)
        }

        registerSystemIdentities(identityService)

        stage("action vocabulary registration") {
            vocabulary.register(
                ActionVocabularyEntry(
                    verbPhrase = NOTIFY_OWNER_VERB_PHRASE,
                    mappings = setOf(ActionResourceMapping(PermissionAction.NOTIFY, ResourceType.TOOL)),
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
        val conversationEngine = InMemoryConversationEngine(identityService)

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

        conversationReplyCoordinator = ConversationReplyCoordinator(communicationConversationCoordinator, replyDeliveryCoordinator)
        runtimeEventLogger = RuntimeEventLogger(eventBus, logger, SYSTEM_PARKER_PRINCIPAL_ID)
    }

    private suspend fun registerSystemIdentities(identityService: InMemoryIdentityService) {
        stage("system identity registration") {
            registerActive(identityService, SYSTEM_PARKER_PRINCIPAL_ID, PrincipalType.SYSTEM, "Parker System")
            registerActive(identityService, CONVERSATION_ENGINE_PRINCIPAL_ID, PrincipalType.SYSTEM, "Conversation Engine")
            registerActive(identityService, RESPONSE_COMPOSER_PRINCIPAL_ID, PrincipalType.SYSTEM, "Response Composer")
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
     * ReasoningProvider -> ResponseComposer -> ResponseDelivery ->
     * ExecutionPipeline -> Tool execution`, with Trust authorisation
     * (`PermissionEngine`, via `ExecutionPipeline`) mandatory on the
     * delivery path, exactly as [ConversationReplyCoordinator.submitAndDeliver]
     * (this method's own sole delegate) already guarantees on its own,
     * unmodified terms.
     *
     * **Reasoning Context assembly (Sprint 11, Unit 3).** This method
     * invokes [reasoningContextAssembler]`.assemble(message)` exactly
     * once, as the first action it takes after confirming
     * [RuntimeLifecycleState.RUNNING] and before its one call to
     * [ConversationReplyCoordinator.submitAndDeliver] --
     * `PRODUCTION_REASONING_CONTEXT_SEQUENCE.md` Section 3's own
     * production call site, now real. The resulting `ReasoningContext` is
     * passed, unchanged, into `submitAndDeliver`. This retires the
     * previous always-empty `reasoningContext: ReasoningContext =
     * ReasoningContext(emptyList())` default parameter this method used
     * to accept -- `PRODUCTION_REASONING_CONTEXT_CONTRACT_DESIGN.md`
     * Section 9 explicitly left "whether the default is retired once the
     * Assembler exists, or preserved for callers that supply their own
     * context" to this implementation Unit's own decision. It is retired,
     * not preserved: a caller-suppliable override would make "the
     * Assembler is invoked exactly once per inbound message" a
     * conditional guarantee (true only when a caller omits the argument)
     * rather than the unconditional one this Unit's own required
     * acceptance tests verify. No existing caller in this repository ever
     * supplied an explicit `reasoningContext` argument (confirmed by
     * direct search of every `submitOwnerMessage` call site under
     * `tests/`), so this narrowing changes no existing call site's
     * compilation. Logs one `INFO` line, "Reasoning Context assembled
     * (correlationId=...)", immediately after the call -- mirroring
     * `CommunicationConversationCoordinator`'s "Conversation accepted"
     * and `ModelReasoningProvider`'s "Reasoning completed" INFO logs this
     * method's own caller-facing pipeline already relies on for
     * observability, and giving `tests/composition` a direct way to
     * verify "invoked exactly once per inbound message" without adding a
     * test-only constructor parameter to this class.
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
            val reasoningContext = reasoningContextAssembler.assemble(message)
            logger.info("Reasoning Context assembled (correlationId=${message.correlationId.value})")
            when (val outcome = conversationReplyCoordinator.submitAndDeliver(message, reasoningContext)) {
                is GatedOutcome.NotAccepted -> {
                    logger.info("Conversation not accepted for delivery (correlationId=${message.correlationId.value}, reason=${outcome.reason})")
                    ParkerRuntimeOutcome.NotAccepted(outcome.reason)
                }
                is GatedOutcome.Produced -> {
                    logger.info("Conversation pipeline completed (correlationId=${message.correlationId.value}, status=${outcome.value.status})")
                    ParkerRuntimeOutcome.Delivered(outcome.value)
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
