package parker.composition

import java.nio.file.Path
import java.time.Instant
import java.util.UUID
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import parker.core.interfaces.ActionResourceMapping
import parker.core.interfaces.ActionVocabularyEntry
import parker.core.interfaces.AgentPolicy
import parker.core.interfaces.AuthorizationPurposeId
import parker.core.interfaces.CandidateEvidenceArtifact
import parker.core.interfaces.CandidateProvenance
import parker.core.interfaces.ConversationEngine
import parker.core.interfaces.ConversationHistorySource
import parker.core.interfaces.EvidenceAnalysisRequest
import parker.core.interfaces.EvidenceArtifactId
import parker.core.interfaces.EvidenceCustodian
import parker.core.interfaces.EvidenceDeletionResult
import parker.core.interfaces.EvidenceIntelligence
import parker.core.interfaces.EvidenceRetrievalResult
import parker.core.interfaces.InboundOwnerMessage
import parker.core.interfaces.KnowledgeRetrieval
import parker.core.interfaces.KnowledgeSource
import parker.core.interfaces.KnowledgeSubmission
import parker.core.interfaces.MemoryCore
import parker.core.interfaces.ModuleConnectivityDeclaration
import parker.core.interfaces.ModuleDescriptor
import parker.core.interfaces.ModuleId
import parker.core.interfaces.ModulePermissionRequirement
import parker.core.interfaces.OwnerEvidenceDeletionAuthority
import parker.core.interfaces.PermissionAction
import parker.core.interfaces.PermissionDecisionOutcome
import parker.core.interfaces.PermissionEngine
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
import parker.core.runtime.DefaultEvidenceCustodian
import parker.core.runtime.DefaultEvidenceIntelligence
import parker.core.runtime.DefaultExecutionPipeline
import parker.core.runtime.DefaultKnowledgeCandidateEvaluator
import parker.core.runtime.DefaultKnowledgeRetrieval
import parker.core.runtime.DefaultKnowledgeSubmission
import parker.core.runtime.DefaultOwnerEvidenceDeletionAuthority
import parker.core.runtime.DefaultPermissionEngine
import parker.core.runtime.DefaultPermissionPolicy
import parker.core.runtime.DefaultPlanCandidateGenerator
import parker.core.runtime.DefaultReasoningContextAssembler
import parker.core.runtime.DefaultReasoningPromptBuilder
import parker.core.runtime.DeterministicAgentStepSource
import parker.core.runtime.DurableMemoryCore
import parker.core.runtime.EvidenceIntelligenceAcceptanceCoordinator
import parker.core.runtime.EvidenceIntelligenceInputResolver
import parker.core.runtime.EvidenceIntelligenceInvocationGate
import parker.core.runtime.EvidenceIntelligenceReasoningCoordinator
import parker.core.runtime.EvidenceRegistrationCoordinator
import parker.core.runtime.EvidenceRegistrationOutcome
import parker.core.runtime.FileSystemEvidenceArtifactStorage
import parker.core.runtime.FileSystemEvidenceDeletionAudit
import parker.core.runtime.FileSystemMemoryCoreDurabilityLog
import parker.core.runtime.GoalPlanningHandoffCoordinator
import parker.core.runtime.GoalPlanningHandoffOutcome
import parker.core.runtime.InMemoryActionVocabulary
import parker.core.runtime.InMemoryAgentRuntime
import parker.core.runtime.InMemoryAuthorizationPurposeRegistry
import parker.core.runtime.InMemoryCommunicationIntake
import parker.core.runtime.InMemoryConversationEngine
import parker.core.runtime.InMemoryEventBus
import parker.core.runtime.InMemoryIdentityService
import parker.core.runtime.InMemoryKnowledgeItemPersistence
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
import parker.core.runtime.MemoryAdmissionCoordinator
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

    // Evidence Custodian Runtime Integration (Implementation Plan Phase 10). evidenceCustodian
    // and ownerEvidenceDeletionAuthority are held as their own narrow interface types, exactly
    // mirroring the capability-narrowing precedent this field list already applies to
    // conversationEngine (ConversationEngine, not the concrete InMemoryConversationEngine) --
    // reinforcing, even within this trusted composition root, that these are two structurally
    // separate capabilities (Phase 7 Boundary Clarification Section 3), never one. Neither field
    // is ever passed to any other coordinator constructed in buildAndRegisterRuntimeGraph -- only
    // this class's own submitEvidence/retrieveEvidence/deleteEvidenceAsOwner methods read them
    // (Boundary Clarification Section 5).
    private lateinit var evidenceCustodian: EvidenceCustodian
    private lateinit var evidenceRegistrationCoordinator: EvidenceRegistrationCoordinator
    private lateinit var ownerEvidenceDeletionAuthority: OwnerEvidenceDeletionAuthority

    // Programme 4, Evidence Intelligence, Unit 8 ("Runtime Composition"). permissionEngine is
    // promoted from a construction-local val (used throughout buildAndRegisterRuntimeGraph
    // already) to a field only because analyseEvidence, below, is the first production entry
    // point that must evaluate a permission decision directly, rather than delegating to a
    // coordinator that already holds its own reference -- the same single, shared instance,
    // never a second one. evidenceIntelligence is held as its own narrow public interface type
    // (mirroring evidenceCustodian's own precedent, above); evidenceIntelligenceAcceptanceCoordinator
    // remains its concrete, internal type, since Unit 7 authorises no public interface for it.
    private lateinit var permissionEngine: PermissionEngine
    private lateinit var evidenceIntelligence: EvidenceIntelligence
    private lateinit var evidenceIntelligenceAcceptanceCoordinator: EvidenceIntelligenceAcceptanceCoordinator

    // Programme 3, Knowledge Memory, Unit 9.6 ("Runtime Composition"). knowledgeRetrieval is held
    // as its own narrow public interface type (mirroring evidenceIntelligence's own identical
    // precedent, above), promoted to a field -- unlike knowledgeSubmission, which remains a
    // construction-local val handed to evidenceIntelligenceAcceptanceCoordinator -- for a
    // different reason than permissionEngine's/evidenceIntelligence's own promotion: no production
    // entry point consumes it yet (wiring Knowledge Retrieval to Reasoning Context remains
    // Programme 4's own, separately governed act, Scope Lock §4), so a field is the only way this
    // instance remains reachable from the composed graph at all, exactly as the Unit 9 Knowledge
    // Retrieval Implementation Plan's own Unit 9.6 entry requires ("making Knowledge Retrieval
    // reachable within the composed runtime"). No new public ParkerRuntime method is added for it.
    private lateinit var knowledgeRetrieval: KnowledgeRetrieval

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
     * deterministic Agent Runtime Execution Boundary Resource; (3b,
     * Evidence Custodian Runtime Integration) register the five fixed
     * Evidence Custodian/Memory Core/deletion Resources; (4)
     * register the `notify owner`, (3a's companion, Controlled Agent
     * Run Submission) `start agent run`, and (3b's companion, Evidence
     * Custodian Runtime Integration) five Evidence Custodian
     * action-vocabulary entries, and construct `DefaultPermissionPolicy`
     * with the corresponding `PermissionPolicyRule`s for all of the
     * above; (4a, Evidence Custodian Runtime Integration) construct
     * `FileSystemEvidenceArtifactStorage`, `FileSystemMemoryCoreDurabilityLog`
     * and `DurableMemoryCore.create` (Memory Core Durability Unit 8; this
     * composition root's first production `MemoryCore` construction,
     * recovering durable state exactly once and aborting startup on
     * recovery failure), `DefaultEvidenceCustodian`, `EvidenceRegistrationCoordinator`,
     * `FileSystemEvidenceDeletionAudit`, and
     * `DefaultOwnerEvidenceDeletionAuthority` -- the last of which is
     * held only by this class's own `deleteEvidenceAsOwner`, never
     * passed to any coordinator constructed below (Phase 7 Boundary
     * Clarification Section 5); (5)
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
        // Authorization Purpose Implementation Plan, Unit 5 ("Composition Wiring"): constructed at
        // the same composition stage as resourceRegistry/vocabulary above (Scope Lock §2.3 --
        // "composition-time registration... at the same composition stage ActionVocabulary/
        // ResourceRegistry entries already are"), and supplied to DefaultPermissionPolicy below.
        // Deliberately a construction-local val, never a ParkerRuntime field. Gap #54 Memory
        // Retrieval Operationalisation Unit 2 registers exactly the two frozen real Purpose
        // values below, but no consumer receives either value until Unit 3 and no Purpose-aware
        // approving rule exists until Unit 4. Registration is eligibility data, never authority.
        val authorizationPurposeRegistry = InMemoryAuthorizationPurposeRegistry()
        stage("Memory retrieval Authorization Purpose registration") {
            authorizationPurposeRegistry.register(KNOWLEDGE_CANDIDATE_EVALUATION_PURPOSE)
            authorizationPurposeRegistry.register(EVIDENCE_INTELLIGENCE_INPUT_RESOLUTION_PURPOSE)
        }
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

        // Evidence Custodian Runtime Integration (Implementation Plan Phase 10): the five fixed,
        // well-known Resources DefaultEvidenceCustodian, EvidenceRegistrationCoordinator, and
        // DefaultOwnerEvidenceDeletionAuthority each already disclosed, unregistered, in their own
        // KDoc since Phases 3, 4, 5/6, and 7 respectively. resourceType is chosen per that KDoc's
        // own stated expectation (evidence.accept/retrieve -> DOCUMENT; the two Memory Core gates
        // -> MEMORY, since they represent writing to the Memory Core substrate generically, not a
        // Document specifically) -- a runtime-composition mapping decision the Contract Design and
        // Implementation Plan both explicitly deferred to this phase, not a new constitutional one.
        stage("Evidence Custodian resource registration") {
            val now = clock()
            listOf(
                Triple(DefaultEvidenceCustodian.EVIDENCE_INTAKE_RESOURCE_ID, ResourceType.DOCUMENT, "Evidence Custodian Intake"),
                Triple(DefaultEvidenceCustodian.EVIDENCE_RETRIEVAL_RESOURCE_ID, ResourceType.DOCUMENT, "Evidence Custodian Retrieval"),
                Triple(EvidenceRegistrationCoordinator.MEMORY_CORE_PROVENANCE_RESOURCE_ID, ResourceType.MEMORY, "Memory Core Provenance Creation"),
                Triple(EvidenceRegistrationCoordinator.MEMORY_CORE_DOCUMENT_REGISTRATION_RESOURCE_ID, ResourceType.MEMORY, "Memory Core Document Registration"),
                Triple(DefaultOwnerEvidenceDeletionAuthority.EVIDENCE_DELETION_RESOURCE_ID, ResourceType.DOCUMENT, "Evidence Custodian Deletion"),
            ).forEach { (resourceId, resourceType, displayName) ->
                resourceRegistry.register(
                    Resource(
                        resourceId = resourceId,
                        resourceType = resourceType,
                        displayName = displayName,
                        ownerPrincipalId = SYSTEM_PARKER_PRINCIPAL_ID,
                        sensitivity = ResourceSensitivity.PUBLIC,
                        lifecycleState = ResourceLifecycleState.REGISTERED,
                        createdAt = now,
                        updatedAt = now,
                        source = "composition-root:evidence-custodian",
                    ),
                )
            }
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
            // Evidence Custodian Runtime Integration (Implementation Plan Phase 10): the five
            // action-vocabulary entries DefaultEvidenceCustodian, EvidenceRegistrationCoordinator,
            // and DefaultOwnerEvidenceDeletionAuthority each already disclosed, unregistered, in
            // their own KDoc. PermissionAction.DELETE already exists on the frozen PermissionAction
            // enum; resourceType mirrors this same Resource registration's own DOCUMENT/MEMORY
            // choice above.
            vocabulary.register(
                ActionVocabularyEntry(
                    verbPhrase = DefaultEvidenceCustodian.ACCEPT_ACTION_NAME,
                    mappings = setOf(ActionResourceMapping(PermissionAction.WRITE, ResourceType.DOCUMENT)),
                ),
            )
            vocabulary.register(
                ActionVocabularyEntry(
                    verbPhrase = DefaultEvidenceCustodian.RETRIEVE_ACTION_NAME,
                    mappings = setOf(ActionResourceMapping(PermissionAction.READ, ResourceType.DOCUMENT)),
                ),
            )
            vocabulary.register(
                ActionVocabularyEntry(
                    verbPhrase = EvidenceRegistrationCoordinator.CREATE_PROVENANCE_ACTION_NAME,
                    mappings = setOf(ActionResourceMapping(PermissionAction.WRITE, ResourceType.MEMORY)),
                ),
            )
            vocabulary.register(
                ActionVocabularyEntry(
                    verbPhrase = EvidenceRegistrationCoordinator.REGISTER_DOCUMENT_ACTION_NAME,
                    mappings = setOf(ActionResourceMapping(PermissionAction.WRITE, ResourceType.MEMORY)),
                ),
            )
            vocabulary.register(
                ActionVocabularyEntry(
                    verbPhrase = DefaultOwnerEvidenceDeletionAuthority.DELETE_ACTION_NAME,
                    mappings = setOf(ActionResourceMapping(PermissionAction.DELETE, ResourceType.DOCUMENT)),
                ),
            )
            // Gap #54 Memory Retrieval Operationalisation Unit 2: resolution/configuration only.
            // Exact verb-specific DENIED guards below prevent fall-through to the pre-existing
            // coarse READ/MEMORY and READ/DOCUMENT approvals.
            vocabulary.register(
                ActionVocabularyEntry(
                    verbPhrase = PermissionFilteredMemoryRetrieval.RETRIEVE_ACTION_NAME,
                    mappings = setOf(ActionResourceMapping(PermissionAction.READ, ResourceType.MEMORY)),
                ),
            )
            vocabulary.register(
                ActionVocabularyEntry(
                    verbPhrase = PermissionFilteredMemoryRetrieval.RETRIEVE_DOCUMENT_ACTION_NAME,
                    mappings = setOf(ActionResourceMapping(PermissionAction.READ, ResourceType.DOCUMENT)),
                ),
            )
        }

        val permissionPolicy = DefaultPermissionPolicy(
            actionMapper = actionMapper,
            resourceRegistry = resourceRegistry,
            authorizationPurposeRegistry = authorizationPurposeRegistry,
            targetlessResourceTypesByProposedAction = mapOf(
                PermissionFilteredMemoryRetrieval.RETRIEVE_ACTION_NAME to setOf(ResourceType.MEMORY),
                PermissionFilteredMemoryRetrieval.RETRIEVE_DOCUMENT_ACTION_NAME to setOf(ResourceType.DOCUMENT),
            ),
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
                // Evidence Custodian Runtime Integration (Implementation Plan Phase 10): the
                // minimum required for evidence.accept/retrieve, the two Memory Core registration
                // gates, and evidence.delete to be reachable at all -- the same "minimum required,
                // narrow in what it grants" policy-content discipline already applied to the two
                // rules above. AUTOMATIC, not APPROVED_WITH_CONFIRMATION, for all four: no
                // confirmation-collection mechanism exists anywhere in this runtime, so that level
                // would claim a step that does not actually happen (mirroring the Phase 7 Boundary
                // Clarification's own identical reasoning for its audit-record design).
                //
                // DELETE/DOCUMENT's own owner-only guarantee is deliberately NOT enforced here --
                // this flat (action, resourceType) policy mechanism has no per-principal matching
                // capability at all (DefaultPermissionPolicy's own KDoc). Owner-scoping is enforced
                // instead by call-site structure, exactly as Controlled Agent Run Submission's own
                // EXECUTE/AGENT rule above already establishes this precedent: deleteEvidenceAsOwner
                // (below) is the only production caller that can ever construct a delete-shaped
                // ExecutionRequest at all (Phase 8's own structural proof), and it always supplies
                // PrincipalId(config.ownerPrincipalId) itself -- never a caller-supplied principal.
                PermissionPolicyRule(
                    action = PermissionAction.WRITE,
                    resourceType = ResourceType.DOCUMENT,
                    outcome = PermissionDecisionOutcome.APPROVED,
                    level = PermissionLevel.AUTOMATIC,
                ),
                PermissionPolicyRule(
                    action = PermissionAction.READ,
                    resourceType = ResourceType.DOCUMENT,
                    outcome = PermissionDecisionOutcome.APPROVED,
                    level = PermissionLevel.AUTOMATIC,
                ),
                PermissionPolicyRule(
                    action = PermissionAction.WRITE,
                    resourceType = ResourceType.MEMORY,
                    outcome = PermissionDecisionOutcome.APPROVED,
                    level = PermissionLevel.AUTOMATIC,
                ),
                // Programme 3, Knowledge Memory, Unit 9.6 ("Runtime Composition"): a genuinely new
                // (action, resourceType) pair for this policy -- the minimum required for
                // knowledge.retrieve to be reachable at all, mirroring the identical "minimum
                // required, narrow in what it grants" discipline the WRITE/MEMORY rule above
                // already applies to knowledge.submit. AUTOMATIC, not APPROVED_WITH_CONFIRMATION,
                // for the same reason given there: no confirmation-collection mechanism exists
                // anywhere in this runtime. This rule governs only the act-level and item-level
                // gates DefaultKnowledgeRetrieval itself already, separately evaluates (Unit 9.5,
                // Adopted) -- it grants no broader Knowledge Memory permission and does not alter
                // either gate's own evaluation order, count, or denial disposition.
                PermissionPolicyRule(
                    action = PermissionAction.READ,
                    resourceType = ResourceType.MEMORY,
                    outcome = PermissionDecisionOutcome.APPROVED,
                    level = PermissionLevel.AUTOMATIC,
                ),
                // Gap #54 Memory Retrieval Operationalisation Unit 2: verb-only fail-closed
                // guards. They are deliberately listed after the applicable coarse approvals:
                // Unit 1 specificity, never list order, makes them govern these exact verbs.
                // They add no authority; Unit 4 Purpose-plus-verb approvals do not exist yet.
                PermissionPolicyRule(
                    action = PermissionAction.READ,
                    resourceType = ResourceType.MEMORY,
                    outcome = PermissionDecisionOutcome.DENIED,
                    level = PermissionLevel.AUTOMATIC,
                    proposedAction = PermissionFilteredMemoryRetrieval.RETRIEVE_ACTION_NAME,
                ),
                PermissionPolicyRule(
                    action = PermissionAction.READ,
                    resourceType = ResourceType.DOCUMENT,
                    outcome = PermissionDecisionOutcome.DENIED,
                    level = PermissionLevel.AUTOMATIC,
                    proposedAction = PermissionFilteredMemoryRetrieval.RETRIEVE_DOCUMENT_ACTION_NAME,
                ),
                PermissionPolicyRule(
                    action = PermissionAction.DELETE,
                    resourceType = ResourceType.DOCUMENT,
                    outcome = PermissionDecisionOutcome.APPROVED,
                    level = PermissionLevel.AUTOMATIC,
                ),
                // Programme 4, Evidence Intelligence, Unit 8: the invocation-gating proposal class
                // (Implementation Plan Section 8 Unit 6) -- a genuinely new (action, resourceType)
                // pair for this policy (EXECUTE/AGENT above governs a structurally distinct domain
                // act, Controlled Agent Run Submission; DOCUMENT above governs WRITE/READ/DELETE,
                // never previously EXECUTE). AUTOMATIC, not APPROVED_WITH_CONFIRMATION, mirroring
                // every other rule's own "no confirmation-collection mechanism exists" reasoning.
                PermissionPolicyRule(
                    action = PermissionAction.EXECUTE,
                    resourceType = ResourceType.DOCUMENT,
                    outcome = PermissionDecisionOutcome.APPROVED,
                    level = PermissionLevel.AUTOMATIC,
                ),
            ),
        )
        permissionEngine = DefaultPermissionEngine(identityService, permissionPolicy)
        val executionPipeline = DefaultExecutionPipeline(
            resourceRegistry,
            actionMapper,
            permissionEngine,
            toolRegistry,
            eventBus,
            toolInvocationBinding,
        )

        // Evidence Custodian Runtime Integration (Implementation Plan Phase 10). Constructed here,
        // after permissionEngine, since DefaultEvidenceCustodian, EvidenceRegistrationCoordinator,
        // and DefaultOwnerEvidenceDeletionAuthority all require it; construction order among these
        // stateless-until-now collaborators is otherwise this composition root's own sequencing
        // choice, per the Implementation Plan's own "an ordering choice is disclosed as a
        // planning-level convenience, never as a new architectural decision" precedent.
        //
        // InMemoryMemoryCore, recovered below, is the first production construction of MemoryCore
        // anywhere in this composition root -- InMemoryKnowledgeStore (Programme 3's own Knowledge
        // layer, already constructed above) is a distinct interface and has never required a live
        // MemoryCore instance. Plain InMemoryMemoryCore, wrapped by DurableMemoryCore, is used
        // deliberately, not the EventPublishingMemoryCore decorator that already exists in this
        // package (src/composition/EventPublishingMemoryCore.kt): wiring Memory Core's own event
        // publication live for the first time remains a separate decision this Unit does not make --
        // EvidenceRegistrationCoordinator only requires a MemoryCore, not an event-publishing one.
        // PermissionGatedMemoryCore (also already implemented and independently verified, Programme 2)
        // likewise remains unconstructed here: EvidenceRegistrationCoordinator (below) and
        // EvidenceIntelligenceAcceptanceCoordinator (Programme 4 Unit 8, further below) each already
        // gate their own MemoryCore writes internally, so wrapping either's raw memoryCore dependency
        // in PermissionGatedMemoryCore would double-gate an already-gated call; no genuine consumer
        // for it exists in this graph -- Memory Core Durability Unit 8's own Planning Review reaffirms
        // this determination explicitly, against the current state of this file, before composing the
        // durable decorator below.
        val evidenceArtifactStorage = stage("Evidence Custodian storage construction") {
            FileSystemEvidenceArtifactStorage(Path.of(config.evidenceStorageRootPath))
        }

        // Memory Core Durability Unit 8 (Runtime Composition). The filesystem durability log and
        // the recovery it drives are both genuinely suspending (file I/O), so both are constructed
        // inside two stage() calls here in start(), never in a synchronous constructor -- the same
        // "load-before-RUNNING" discipline this composition root's other stage()-wrapped steps
        // already follow. DurableMemoryCore.create() recovers InMemoryMemoryCore's own complete
        // starting state from the durability log exactly once (MemoryCoreRecovery.recover, Units 4-5),
        // deriving identifier counters as part of that same recovery pass -- no separate step is
        // needed here for that. A recovery fault (MemoryCoreRecoveryException, thrown by
        // DurableMemoryCore.create itself) is not a ParkerRuntimeException, so stage()'s own
        // catch-and-wrap turns it into DependencyConstructionFailed, which start()'s own outer
        // handling already turns into state=FAILED, never RUNNING -- there is no separate fallback
        // path here that could construct a fresh, empty InMemoryMemoryCore instead.
        val memoryCoreDurabilityLog = stage("Memory Core durability log construction") {
            FileSystemMemoryCoreDurabilityLog(Path.of(config.memoryCoreDurabilityLogPath))
        }
        val durableMemoryCore = stage("Memory Core recovery") {
            DurableMemoryCore.create(memoryCoreDurabilityLog)
        }
        val memoryCore: MemoryCore = durableMemoryCore
        val defaultEvidenceCustodian = DefaultEvidenceCustodian(evidenceArtifactStorage, permissionEngine)
        evidenceCustodian = defaultEvidenceCustodian
        evidenceRegistrationCoordinator = EvidenceRegistrationCoordinator(defaultEvidenceCustodian, memoryCore, permissionEngine)

        // Phase 7 Boundary Clarification Section 3: DefaultOwnerEvidenceDeletionAuthority is a
        // wholly separate class from DefaultEvidenceCustodian, sharing only evidenceArtifactStorage
        // and permissionEngine -- never EvidenceCustodian, never MemoryCore. Its own reference
        // (ownerEvidenceDeletionAuthority) is held by this class alone; no coordinator constructed
        // anywhere in this method ever receives it (Boundary Clarification Section 5) -- only
        // deleteEvidenceAsOwner, below, ever reads this field.
        val evidenceDeletionAudit = stage("Evidence Custodian deletion audit construction") {
            FileSystemEvidenceDeletionAudit(Path.of(config.evidenceDeletionAuditLogPath))
        }
        ownerEvidenceDeletionAuthority = DefaultOwnerEvidenceDeletionAuthority(
            evidenceArtifactStorage,
            permissionEngine,
            evidenceDeletionAudit,
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

        // Programme 4, Evidence Intelligence, Unit 8 ("Runtime Composition and Full
        // Verification", Implementation Plan Section 8 Unit 8). Wires Units 1-7 -- already
        // implemented and independently verified in isolation -- into this composition root,
        // reusing every shared dependency Section 5 of the Implementation Plan already fixes
        // (memoryCore, defaultEvidenceCustodian, permissionEngine, reasoningProvider);
        // none is duplicated. analyseEvidence, below, is the sole production entry point:
        // neither this block nor any other code path attaches Evidence Intelligence to
        // submitOwnerMessage or any conversation path, and no background analysis is started.
        //
        // permissionFilteredMemoryRetrieval is the one shared PermissionFilteredMemoryRetrieval
        // (Programme 2, already implemented, until now unwired) that both
        // EvidenceIntelligenceInputResolver and DefaultKnowledgeCandidateEvaluator receive --
        // never a second instance. Memory Core Durability Unit 8 (Runtime Composition) wraps
        // durableMemoryCore here, not the raw recovered InMemoryMemoryCore delegate -- that
        // delegate is DurableMemoryCore's own private field, unreachable from this composition
        // root; every read this runtime performs, exactly like every write, now passes through
        // the durable decorator, which itself delegates each MemoryRetrieval call straight to the
        // recovered in-memory state with no durability-log interaction (Unit 6). Unit 2 now
        // registers and derives memory.retrieve/memory.retrieve_document, but the two exact
        // verb-specific DENIED guards above outrank existing coarse approvals. Both consumers
        // still share this unbound decorator and therefore propagate no Authorization Purpose;
        // Unit 3 has not begun and Memory Retrieval remains genuinely fail-closed.
        val permissionFilteredMemoryRetrieval = PermissionFilteredMemoryRetrieval(durableMemoryCore, permissionEngine)

        // Programme 3, Knowledge Memory, Unit 8 ("Constitutional Knowledge Submission"), and now
        // also Unit 9.6 ("Runtime Composition"). One long-lived InMemoryKnowledgeItemPersistence
        // for the lifetime of this ParkerRuntime -- never recreated per invocation, shared
        // unchanged between the write side (knowledgeSubmission, below) and the read side
        // (knowledgeRetrieval, below) -- never a second, parallel persistence instance, so
        // anything knowledgeSubmission successfully promotes is genuinely reachable through
        // knowledgeRetrieval.
        val knowledgeItemPersistence = InMemoryKnowledgeItemPersistence()
        val knowledgeCandidateEvaluator = DefaultKnowledgeCandidateEvaluator(permissionFilteredMemoryRetrieval)
        val knowledgeSubmission: KnowledgeSubmission = DefaultKnowledgeSubmission(
            knowledgeCandidateEvaluator,
            knowledgeItemPersistence,
            permissionEngine,
        )

        // Parker Conversational Memory Bridge, Admission Unit
        // (docs/implementation/CONVERSATIONAL_MEMORY_ADMISSION_IMPLEMENTATION_PLAN.md). Reuses the
        // existing memoryCore (the durable instance -- see the "no double gating" reasoning already
        // established above for evidenceRegistrationCoordinator/evidenceIntelligenceAcceptanceCoordinator)
        // and the knowledgeSubmission instance just constructed -- never a second, parallel instance
        // of either. permissionEngine gates only this coordinator's own Memory Core write; it never
        // re-evaluates knowledgeSubmission's own, separate, already-existing self-gate.
        val memoryAdmissionCoordinator = MemoryAdmissionCoordinator(memoryCore, knowledgeSubmission, permissionEngine)

        conversationReplyCoordinator = ConversationReplyCoordinator(
            communicationConversationCoordinator,
            replyDeliveryCoordinator,
            goalPlanningHandoffCoordinator,
            memoryAdmissionCoordinator,
        )
        runtimeEventLogger = RuntimeEventLogger(eventBus, logger, SYSTEM_PARKER_PRINCIPAL_ID)

        // Programme 3, Knowledge Memory, Unit 9.6 ("Runtime Composition"). The same, shared
        // knowledgeItemPersistence and permissionEngine instances above -- never a second,
        // parallel persistence or a second, parallel Permission Engine. DefaultKnowledgeRetrieval
        // self-gates (docs/governance/PROGRAMME_3_UNIT_9_PERMISSION_ENFORCEMENT_MECHANISM_CLARIFICATION.md,
        // Adopted), so, unlike permissionFilteredMemoryRetrieval above, no external decorator is
        // composed here -- this is the sole, already-gated implementation, held directly. clock is
        // left defaulted (the real system clock), exactly as every other production call site of
        // this class already does.
        knowledgeRetrieval = DefaultKnowledgeRetrieval(knowledgeItemPersistence, permissionEngine)

        val evidenceIntelligenceInputResolver = EvidenceIntelligenceInputResolver(defaultEvidenceCustodian, permissionFilteredMemoryRetrieval)
        val evidenceIntelligenceReasoningCoordinator = EvidenceIntelligenceReasoningCoordinator(reasoningProvider)
        evidenceIntelligence = DefaultEvidenceIntelligence(evidenceIntelligenceInputResolver, evidenceIntelligenceReasoningCoordinator)

        // The existing raw memoryCore, not a PermissionGatedMemoryCore wrapper: this coordinator
        // already gates its own CandidateRecordProduced dispatch internally (its own
        // permissionEngine, MEMORY_CORE_ACCEPTANCE_RESOURCE_ID/ACCEPT_MEMORY_CORE_CANDIDATE_ACTION_NAME),
        // exactly as EvidenceRegistrationCoordinator (above) already does for its own two MemoryCore
        // calls -- wrapping memoryCore here would double-gate an already-gated write.
        evidenceIntelligenceAcceptanceCoordinator = EvidenceIntelligenceAcceptanceCoordinator(
            defaultEvidenceCustodian,
            memoryCore,
            knowledgeSubmission,
            permissionEngine,
        )

        // Resource/ActionVocabulary registration for the three disclosed-but-previously-
        // unregistered conventions this graph now makes reachable: Unit 6's invocation gate
        // (EXECUTE/DOCUMENT, a genuinely new pair for this policy, registered above), Unit 7's
        // Memory Core acceptance gate, and Programme 3 Unit 8's Knowledge Submission gate (both
        // WRITE/MEMORY -- already an APPROVED rule above; only their own Resource/ActionVocabulary
        // registration is new here). Mirrors the Evidence Custodian resource/action registration
        // above exactly, in shape and discipline.
        stage("Evidence Intelligence resource registration") {
            val now = clock()
            listOf(
                Triple(
                    EvidenceIntelligenceInvocationGate.EVIDENCE_INTELLIGENCE_INVOCATION_RESOURCE_ID,
                    ResourceType.DOCUMENT,
                    "Evidence Intelligence Invocation",
                ),
                Triple(
                    EvidenceIntelligenceAcceptanceCoordinator.MEMORY_CORE_ACCEPTANCE_RESOURCE_ID,
                    ResourceType.MEMORY,
                    "Evidence Intelligence Memory Core Acceptance",
                ),
                Triple(DefaultKnowledgeSubmission.KNOWLEDGE_SUBMISSION_RESOURCE_ID, ResourceType.MEMORY, "Knowledge Submission"),
            ).forEach { (resourceId, resourceType, displayName) ->
                resourceRegistry.register(
                    Resource(
                        resourceId = resourceId,
                        resourceType = resourceType,
                        displayName = displayName,
                        ownerPrincipalId = SYSTEM_PARKER_PRINCIPAL_ID,
                        sensitivity = ResourceSensitivity.PUBLIC,
                        lifecycleState = ResourceLifecycleState.REGISTERED,
                        createdAt = now,
                        updatedAt = now,
                        source = "composition-root:evidence-intelligence",
                    ),
                )
            }
        }
        stage("Evidence Intelligence action vocabulary registration") {
            vocabulary.register(
                ActionVocabularyEntry(
                    verbPhrase = EvidenceIntelligenceInvocationGate.ANALYSE_ACTION_NAME,
                    mappings = setOf(ActionResourceMapping(PermissionAction.EXECUTE, ResourceType.DOCUMENT)),
                ),
            )
            vocabulary.register(
                ActionVocabularyEntry(
                    verbPhrase = EvidenceIntelligenceAcceptanceCoordinator.ACCEPT_MEMORY_CORE_CANDIDATE_ACTION_NAME,
                    mappings = setOf(ActionResourceMapping(PermissionAction.WRITE, ResourceType.MEMORY)),
                ),
            )
            vocabulary.register(
                ActionVocabularyEntry(
                    verbPhrase = DefaultKnowledgeSubmission.SUBMIT_ACTION_NAME,
                    mappings = setOf(ActionResourceMapping(PermissionAction.WRITE, ResourceType.MEMORY)),
                ),
            )
        }

        // Programme 3, Knowledge Memory, Unit 9.6 ("Runtime Composition"). Resource/ActionVocabulary
        // registration for DefaultKnowledgeRetrieval's own disclosed-but-previously-unregistered
        // resource/action pair (docs/governance/PROGRAMME_3_UNIT_9_PERMISSION_ENFORCEMENT_MECHANISM_CLARIFICATION.md,
        // Adopted, Section 7) -- a separate stage from Evidence Intelligence's own, above, since
        // Knowledge Retrieval belongs to neither that unit nor Knowledge Submission, even though
        // it shares Knowledge Submission's own KNOWLEDGE_SUBMISSION_RESOURCE_ID's naming
        // convention and ResourceType.MEMORY. The same fixed pair is named by both
        // DefaultKnowledgeRetrieval's own act-level and item-level gates -- Unit 9.5's own Section
        // 7 fixes one resource identity and one action name, evaluated at two granularities, never
        // two separate pairs -- so registering it once here suffices for both.
        stage("Knowledge Retrieval resource registration") {
            val now = clock()
            resourceRegistry.register(
                Resource(
                    resourceId = DefaultKnowledgeRetrieval.KNOWLEDGE_RETRIEVAL_RESOURCE_ID,
                    resourceType = ResourceType.MEMORY,
                    displayName = "Knowledge Retrieval",
                    ownerPrincipalId = SYSTEM_PARKER_PRINCIPAL_ID,
                    sensitivity = ResourceSensitivity.PUBLIC,
                    lifecycleState = ResourceLifecycleState.REGISTERED,
                    createdAt = now,
                    updatedAt = now,
                    source = "composition-root:knowledge-retrieval",
                ),
            )
        }
        stage("Knowledge Retrieval action vocabulary registration") {
            vocabulary.register(
                ActionVocabularyEntry(
                    verbPhrase = DefaultKnowledgeRetrieval.RETRIEVE_ACTION_NAME,
                    mappings = setOf(ActionResourceMapping(PermissionAction.READ, ResourceType.MEMORY)),
                ),
            )
        }

        // Parker Conversational Memory Bridge, Admission Unit. WRITE/MEMORY is already an APPROVED
        // rule (registered above, alongside Evidence Custodian) -- only this Resource/ActionVocabulary
        // registration is new here, mirroring every other Memory-Core-writing coordinator's own
        // identical registration shape.
        stage("Conversational Memory Admission resource registration") {
            val now = clock()
            resourceRegistry.register(
                Resource(
                    resourceId = MemoryAdmissionCoordinator.CONVERSATIONAL_MEMORY_RESOURCE_ID,
                    resourceType = ResourceType.MEMORY,
                    displayName = "Conversational Memory Admission",
                    ownerPrincipalId = SYSTEM_PARKER_PRINCIPAL_ID,
                    sensitivity = ResourceSensitivity.PUBLIC,
                    lifecycleState = ResourceLifecycleState.REGISTERED,
                    createdAt = now,
                    updatedAt = now,
                    source = "composition-root:conversational-memory-admission",
                ),
            )
        }
        stage("Conversational Memory Admission action vocabulary registration") {
            vocabulary.register(
                ActionVocabularyEntry(
                    verbPhrase = MemoryAdmissionCoordinator.CREATE_CONVERSATIONAL_MEMORY_ACTION_NAME,
                    mappings = setOf(ActionResourceMapping(PermissionAction.WRITE, ResourceType.MEMORY)),
                ),
            )
        }
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
     * Evidence Custodian Runtime Integration (Implementation Plan Phase 10).
     * Governed acceptance and Memory Core registration for one evidence
     * artefact, delegating unchanged to
     * [EvidenceRegistrationCoordinator.register] -- this method adds no
     * orchestration of its own beyond the [RuntimeLifecycleState.RUNNING]
     * guard every production entry point on this class already requires.
     * [EvidenceRegistrationCoordinator.register]'s own `correlationId`
     * parameter is minted once, here, mirroring
     * [GoalPlanningHandoffCoordinator]'s own `planningSessionIdFactory`
     * convention -- there is no `InboundOwnerMessage`-equivalent wrapper
     * for evidence submission to carry one in from a caller.
     *
     * Throws [ParkerRuntimeException.NotRunning] if [state] is not
     * [RuntimeLifecycleState.RUNNING].
     */
    suspend fun submitEvidence(
        requestingPrincipalId: PrincipalId,
        candidateEvidenceArtifact: CandidateEvidenceArtifact,
        candidateProvenance: CandidateProvenance,
        documentType: String,
        documentIntegrityHash: String? = null,
        documentMetadata: Map<String, String> = emptyMap(),
    ): EvidenceRegistrationOutcome {
        if (state != RuntimeLifecycleState.RUNNING) {
            throw ParkerRuntimeException.NotRunning(state)
        }
        logger.info("Evidence submission received for registration (principal=${requestingPrincipalId.value})")
        return evidenceRegistrationCoordinator.register(
            requestingPrincipalId = requestingPrincipalId,
            correlationId = UUID.randomUUID().toString(),
            candidateEvidenceArtifact = candidateEvidenceArtifact,
            candidateProvenance = candidateProvenance,
            documentType = documentType,
            documentIntegrityHash = documentIntegrityHash,
            documentMetadata = documentMetadata,
        )
    }

    /**
     * Evidence Custodian Runtime Integration (Implementation Plan Phase 10).
     * Governed, observational retrieval of a previously accepted evidence
     * artefact, delegating unchanged to [EvidenceCustodian.retrieve].
     * Accepts a caller-supplied [requestingPrincipalId], unlike
     * [deleteEvidenceAsOwner] below -- the Permission Engine, not this
     * method, is the intended gate for who may retrieve (Contract Design
     * Section 6.3 anticipates a non-owner Evidence Intelligence consumer
     * requesting authorised read access, "acting only as a consumer").
     *
     * Throws [ParkerRuntimeException.NotRunning] if [state] is not
     * [RuntimeLifecycleState.RUNNING].
     */
    suspend fun retrieveEvidence(
        requestingPrincipalId: PrincipalId,
        evidenceArtifactId: EvidenceArtifactId,
    ): EvidenceRetrievalResult {
        if (state != RuntimeLifecycleState.RUNNING) {
            throw ParkerRuntimeException.NotRunning(state)
        }
        logger.info("Evidence retrieval requested (principal=${requestingPrincipalId.value})")
        return evidenceCustodian.retrieve(requestingPrincipalId, evidenceArtifactId)
    }

    /**
     * Evidence Custodian Runtime Integration (Implementation Plan Phase 10).
     * The one production entry point capable of ending Evidence Custodian
     * custody. Deliberately takes **no** `requestingPrincipalId` parameter
     * -- every other production entry point on this class accepts a
     * caller-supplied principal (see [retrieveEvidence]'s own KDoc for
     * why), but CDR-006 and the Phase 7 Boundary Clarification (Section 3,
     * Determination 3) require deletion specifically to be structurally,
     * not merely policy-content, owner-only. This method always acts as
     * `PrincipalId(config.ownerPrincipalId)` -- there is no parameter
     * through which any caller of this method, internal or external,
     * could substitute a different principal. Mirrors Controlled Agent Run
     * Submission's own precedent: owner-scoping is enforced by call-site
     * structure, "not by [the Permission] rule's own matching logic."
     *
     * [ownerEvidenceDeletionAuthority] is held by this class alone -- no
     * coordinator, reasoning provider, or module constructed anywhere in
     * [buildAndRegisterRuntimeGraph] ever receives a reference to it
     * (Boundary Clarification Section 5); this method is the only
     * reachable path to it in the entire production graph.
     *
     * Throws [ParkerRuntimeException.NotRunning] if [state] is not
     * [RuntimeLifecycleState.RUNNING].
     */
    suspend fun deleteEvidenceAsOwner(evidenceArtifactId: EvidenceArtifactId): EvidenceDeletionResult {
        if (state != RuntimeLifecycleState.RUNNING) {
            throw ParkerRuntimeException.NotRunning(state)
        }
        logger.info("Evidence deletion requested by owner (evidenceArtifactId=${evidenceArtifactId.value})")
        return ownerEvidenceDeletionAuthority.deleteAsOwner(PrincipalId(config.ownerPrincipalId), evidenceArtifactId)
    }

    /**
     * Programme 4, Evidence Intelligence, Unit 8 ("Runtime Composition"). The sole production
     * entry point through which Evidence Intelligence may be invoked -- **explicit invocation
     * only** (Implementation Plan Section 3: "Autonomous or self-initiated analysis" is out of
     * scope). Never called by [submitOwnerMessage], the interactive console, or any other
     * conversation-path code; [request] must already be fully constructed by the caller -- this
     * method performs no document parsing, retrieval, or candidate construction of its own.
     *
     * **Sequence, exactly as Scope Lock Section 6 fixes it:**
     * 1. Evaluates Unit 6's own invocation-gating proposal class
     *    ([EvidenceIntelligenceInvocationGate.buildExecutionRequest]) via the shared
     *    [permissionEngine], using [requestingPrincipalId] -- never [request]'s own embedded
     *    `requestingPrincipalId` field, which [EvidenceIntelligence.analyse] reads for its own,
     *    separate audit purpose.
     * 2. On any outcome other than `APPROVED`/`APPROVED_WITH_CONFIRMATION`, returns
     *    [EvidenceIntelligenceInvocationOutcome.NotAuthorised] immediately -- [evidenceIntelligence]
     *    is never called.
     * 3. On approval, calls [EvidenceIntelligence.analyse] exactly once.
     * 4. Passes its returned list, completely unchanged and in the same order, to
     *    [EvidenceIntelligenceAcceptanceCoordinator.dispatch], using the same
     *    [requestingPrincipalId] used for the gate above.
     * 5. Returns [EvidenceIntelligenceInvocationOutcome.Completed], carrying that dispatch's own
     *    returned list, unchanged.
     *
     * No retry, and no exception handling of any kind beyond the [RuntimeLifecycleState.RUNNING]
     * guard every production entry point on this class already requires -- a genuine fault from
     * [permissionEngine], [evidenceIntelligence], or [evidenceIntelligenceAcceptanceCoordinator]
     * propagates unchanged out of this method.
     *
     * Throws [ParkerRuntimeException.NotRunning] if [state] is not [RuntimeLifecycleState.RUNNING].
     */
    suspend fun analyseEvidence(
        requestingPrincipalId: PrincipalId,
        request: EvidenceAnalysisRequest,
    ): EvidenceIntelligenceInvocationOutcome {
        if (state != RuntimeLifecycleState.RUNNING) {
            throw ParkerRuntimeException.NotRunning(state)
        }

        val decision = permissionEngine.evaluate(EvidenceIntelligenceInvocationGate.buildExecutionRequest(requestingPrincipalId))
        if (decision.decision != PermissionDecisionOutcome.APPROVED &&
            decision.decision != PermissionDecisionOutcome.APPROVED_WITH_CONFIRMATION
        ) {
            logger.info(
                "Evidence Intelligence invocation not authorised (principal=${requestingPrincipalId.value}, " +
                    "decision=${decision.decision})",
            )
            return EvidenceIntelligenceInvocationOutcome.NotAuthorised(
                "Permission Engine did not authorise Evidence Intelligence invocation for principal " +
                    "'${requestingPrincipalId.value}' (decision=${decision.decision})",
            )
        }

        logger.info("Evidence Intelligence invocation authorised (principal=${requestingPrincipalId.value})")
        val results = evidenceIntelligence.analyse(request)
        val acceptanceOutcomes = evidenceIntelligenceAcceptanceCoordinator.dispatch(requestingPrincipalId, results)
        return EvidenceIntelligenceInvocationOutcome.Completed(acceptanceOutcomes)
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
        val KNOWLEDGE_CANDIDATE_EVALUATION_PURPOSE =
            AuthorizationPurposeId("knowledge-memory.candidate-evaluation")
        val EVIDENCE_INTELLIGENCE_INPUT_RESOLUTION_PURPOSE =
            AuthorizationPurposeId("evidence-intelligence.input-resolution")
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
