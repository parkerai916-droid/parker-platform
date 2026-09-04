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
import parker.core.interfaces.DerivativeGenerationId
import parker.core.interfaces.DerivativeMemoryRegistrationOutcome
import parker.core.interfaces.DocumentAnalysisInvocationResult
import parker.core.interfaces.DocumentAnalysisOutcome
import parker.core.interfaces.EvidenceAnalysisRequest
import parker.core.interfaces.EvidenceArtifactId
import parker.core.interfaces.EvidenceCustodian
import parker.core.interfaces.EvidenceDeletionResult
import parker.core.interfaces.EvidenceIntelligence
import parker.core.interfaces.EvidenceRetrievalResult
import parker.core.interfaces.ExternalTranscriptionMechanism
import parker.core.interfaces.ExternalTranscriptionMechanismOutcome
import parker.core.interfaces.ExternalTranscriptionOwnerInvocationOutcome
import parker.core.interfaces.ExternalTranscriptionRequest
import parker.core.interfaces.HumanVerificationRecord
import parker.core.interfaces.HumanVerificationStorage
import parker.core.interfaces.HumanFidelityReviewStorage
import parker.core.interfaces.GovernedHumanFidelityReviewRecordingService
import parker.core.interfaces.EffectiveHumanFidelityReviewProjector
import parker.core.interfaces.GovernedHumanCorrectionService
import parker.core.interfaces.HumanCorrectedRepresentationStorage
import parker.core.interfaces.HumanCorrectionAudit
import parker.core.interfaces.OpaqueOwnerPrincipal
import parker.core.interfaces.InboundOwnerMessage
import parker.core.interfaces.KnowledgeRetrieval
import parker.core.interfaces.KnowledgeSubmission
import parker.core.interfaces.MemoryCore
import parker.core.interfaces.ModuleConnectivityDeclaration
import parker.core.interfaces.ModuleDescriptor
import parker.core.interfaces.ModuleId
import parker.core.interfaces.ModulePermissionRequirement
import parker.core.interfaces.OcrMechanism
import parker.core.interfaces.OwnerDocumentAnalysisRequest
import parker.core.interfaces.PendingAnalysisId
import parker.core.interfaces.RetrieveSavedAnalysisOutcome
import parker.core.interfaces.SaveAnalysisOutcome
import parker.core.interfaces.SavedAnalysisId
import parker.core.interfaces.SavedAnalysisSummary
import parker.core.interfaces.OwnerEvidenceDeletionAuthority
import parker.core.interfaces.OwnerLocalFileIngressOutcome
import parker.core.interfaces.OwnerVerificationCredential
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
import parker.core.interfaces.ReasoningKnowledgeSource
import parker.core.interfaces.RelevanceMechanism
import parker.core.interfaces.ResolvedInboundMessage
import parker.core.interfaces.Resource
import parker.core.interfaces.ResourceId
import parker.core.interfaces.ResourceLifecycleState
import parker.core.interfaces.ResourceSensitivity
import parker.core.interfaces.ResourceType
import parker.core.interfaces.TierAContentRetrievalOutcome
import parker.core.interfaces.TierADocumentIngestionRouter
import parker.core.interfaces.TierADocumentRoutingResult
import parker.core.interfaces.TierAOwnerInvocationOutcome
import parker.core.interfaces.TierBOcrContentRetrievalOutcome
import parker.core.interfaces.TierBOcrOwnerInvocationOutcome
import parker.core.interfaces.WorldModelSource
import parker.core.runtime.ActionMapper
import parker.core.runtime.CommunicationConversationCoordinator
import parker.core.runtime.ConversationOutcome
import parker.core.runtime.ConversationReplyCoordinator
import parker.core.runtime.ConversationTurnReasoningCoordinator
import parker.core.runtime.GovernedAcquisitionOwnerEvaluation
import parker.core.runtime.GovernedAcquisitionOwnerExecution
import parker.core.runtime.GovernedAcquisitionOwnerWorkflow
import parker.core.runtime.GovernedAcquisitionExecutionCoordinator
import parker.core.runtime.DeterministicEvidenceAcquisitionRouter
import parker.core.runtime.ProductionAcquisitionCapabilityCatalogue
import parker.core.runtime.TierANativeAcquisitionExecutor
import parker.core.runtime.LocalOcrAcquisitionExecutor
import parker.core.runtime.AcquisitionExecutorBinding
import parker.core.interfaces.EvidenceAcquisitionMechanism
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
import parker.core.runtime.DefaultReasoningKnowledgeSource
import parker.core.runtime.DefaultReasoningPromptBuilder
import parker.core.runtime.DefaultDocumentAnalysisPromptBuilder
import parker.core.runtime.DerivativeMemoryRegistrationCoordinator
import parker.core.runtime.DeterministicAgentStepSource
import parker.core.runtime.DocumentAnalysisCoordinator
import parker.core.runtime.FileSystemSavedAnalysisStorage
import parker.core.runtime.FileSystemHumanVerificationStorage
import parker.core.runtime.FileSystemHumanFidelityGovernanceAudit
import parker.core.runtime.FileSystemHumanFidelityReviewStorage
import parker.core.runtime.DefaultGovernedHumanFidelityReviewRecordingService
import parker.core.runtime.DefaultEffectiveHumanFidelityReviewProjector
import parker.core.runtime.HumanFidelityReviewRecordingPermissionPolicy
import parker.core.runtime.HumanCorrectionPermissionPolicy
import parker.core.runtime.ExternalFileOwnerHighAuthorityVerification
import parker.core.runtime.DefaultGovernedHumanCorrectionService
import parker.core.runtime.FileSystemHumanCorrectedRepresentationStorage
import parker.core.runtime.FileSystemHumanCorrectionAudit
import parker.core.runtime.HumanCorrectedRepresentationRetrievalService
import parker.core.runtime.DefaultHumanCorrectedRepresentationEligibilityEvaluator
import parker.core.runtime.StoredHumanCorrectionProviderResolver
import parker.core.runtime.PendingAnalysisCache
import parker.core.runtime.SavedAnalysisCoordinator
import parker.core.runtime.DurableMemoryCore
import parker.core.runtime.DurableKnowledgeItemPersistence
import parker.core.runtime.EvidenceIntelligenceAcceptanceCoordinator
import parker.core.runtime.EvidenceIntelligenceInputResolver
import parker.core.runtime.EvidenceIntelligenceInvocationGate
import parker.core.runtime.ExternalTranscriptionInvocationGate
import parker.core.runtime.ExternalTranscriptionOwnerInvocationCoordinator
import parker.core.runtime.FidelityFirstAcceptanceCoordinator
import parker.core.runtime.FidelityFirstAcceptanceLifecycle
import parker.core.runtime.FidelityFirstAcceptanceOutcome
import parker.core.runtime.FidelityFirstEffectiveConfiguration
import parker.core.runtime.FileSystemFidelityFirstAcceptanceAuthorityStorage
import parker.core.runtime.FileSystemFidelityFirstAttemptLedger
import parker.core.runtime.FileSystemRegionProviderStateStore
import parker.core.runtime.GovernedRegionTranscriptionExecutionCoordinator
import parker.core.runtime.JdkOpenAiResponsesTransport
import parker.core.runtime.OpenAiRegionTranscriptionAdapter
import parker.core.runtime.FileSystemOrdinaryRegionCapabilityAcceptanceStore
import parker.core.runtime.FileSystemOrdinaryRegionAuthorizationStore
import parker.core.runtime.OrdinaryRegionAuthorizationGuard
import parker.core.runtime.OrdinaryRegionCapabilityAcceptanceEvaluator
import parker.core.runtime.OrdinaryRegionCapabilityIdentity
import parker.core.runtime.OrdinaryRegionDerivativeAdmission
import parker.core.runtime.OrdinaryRegionIngestionWorkflow
import parker.core.runtime.OrdinaryRegionRequestPreparer
import parker.core.runtime.OpenAiResponsesExternalTranscriptionAdapter
import parker.core.runtime.EvidenceIntelligenceReasoningCoordinator
import parker.core.runtime.EvidenceRegistrationCoordinator
import parker.core.runtime.EvidenceRegistrationOutcome
import parker.core.runtime.DefaultExplicitOwnerPersistenceDirectiveClassifier
import parker.core.runtime.ExplicitOwnerPersistenceDirectiveReasoningProvider
import parker.core.runtime.FileSystemDerivativeContentStorage
import parker.core.runtime.FileSystemDerivativeGenerationStorage
import parker.core.runtime.FileSystemDocumentIngestionAudit
import parker.core.runtime.FileSystemEvidenceArtifactStorage
import parker.core.runtime.FileSystemEvidenceDeletionAudit
import parker.core.runtime.FileSystemEvidenceSourceManifestStorage
import parker.core.runtime.FileSystemMemoryCoreDurabilityLog
import parker.core.runtime.FileSystemKnowledgeItemDurabilityLog
import parker.core.runtime.GoalPlanningHandoffCoordinator
import parker.core.runtime.GoalPlanningHandoffOutcome
import parker.core.runtime.InMemoryActionVocabulary
import parker.core.runtime.InMemoryAgentRuntime
import parker.core.runtime.InMemoryAuthorizationPurposeRegistry
import parker.core.runtime.InMemoryCommunicationIntake
import parker.core.runtime.DoclingOcrProviderAdapter
import parker.core.runtime.DoclingOcrProviderAdapterConfiguration
import parker.core.runtime.EvidenceIntelligenceOcrCoordinator
import parker.core.runtime.InMemoryConversationEngine
import parker.core.runtime.InMemoryEventBus
import parker.core.runtime.InMemoryIdentityService
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
import parker.core.runtime.OcrExecutionSequencer
import parker.core.runtime.OcrStructuredResultValidator
import parker.core.runtime.OwnerLocalFileIngressCoordinator
import parker.core.runtime.ProcessBuilderDoclingSubprocessInvoker
import parker.core.runtime.PermissionPolicyRule
import parker.core.runtime.ProcessBuilderQmdSubprocessInvoker
import parker.core.runtime.QmdRelevanceMechanism
import parker.core.runtime.QmdRelevanceMechanismConfiguration
import parker.core.runtime.ReplyDeliveryCoordinator
import parker.core.runtime.ResponseComposer
import parker.core.runtime.ResponseDelivery
import parker.core.runtime.ApacheCommonsCsvExtractor
import parker.core.runtime.DerivativeGenerationCoordinator
import parker.core.runtime.TaggedReasoningResponseParser
import parker.core.runtime.TierADocumentIngestionComposition
import parker.core.runtime.TierAContentRetrievalCoordinator
import parker.core.runtime.TierAOwnerInvocationCoordinator
import parker.core.runtime.TierBOcrContentRetrievalCoordinator
import parker.core.runtime.TierBOcrOwnerInvocationCoordinator

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
private object DisabledExternalTranscriptionMechanism : ExternalTranscriptionMechanism {
    override suspend fun transcribe(request: ExternalTranscriptionRequest): ExternalTranscriptionMechanismOutcome =
        ExternalTranscriptionMechanismOutcome.Failure("External transcription mechanism is not configured")
}

class ParkerRuntime(
    private val config: ParkerRuntimeConfig,
    private val logger: ParkerLogger,
    private val ownerNotificationSink: OwnerNotificationSink = LoggingOwnerNotificationSink(logger),
    private val clock: () -> Instant = Instant::now,
    private val buildIdentity: () -> String? = { discoverRuntimeEmbeddedSourceCommit() },
) {

    private val stateLock = Mutex()
    var state: RuntimeLifecycleState = RuntimeLifecycleState.NOT_STARTED
        private set
    var openAiExternalTranscriptionReadiness: OpenAiExternalTranscriptionReadiness =
        OpenAiExternalTranscriptionReadiness.Disabled
        private set
    var openAiExternalTranscriptionBackendReadiness: OpenAiExternalTranscriptionBackendReadiness =
        OpenAiExternalTranscriptionBackendReadiness.Disabled
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
    private lateinit var ownerEvidenceListing: parker.core.runtime.FileSystemOwnerEvidenceListing
    private lateinit var evidenceRegistrationCoordinator: EvidenceRegistrationCoordinator
    private lateinit var ownerEvidenceDeletionAuthority: OwnerEvidenceDeletionAuthority

    // Document Ingestion, Owner-Authorized Local File Ingress. Held as its own narrow class,
    // exactly mirroring ownerEvidenceDeletionAuthority's own "only this class's own entry-point
    // method reads it" isolation -- no other coordinator constructed in
    // buildAndRegisterRuntimeGraph ever receives a reference to it.
    private lateinit var ownerLocalFileIngressCoordinator: OwnerLocalFileIngressCoordinator

    // Document Ingestion, Owner-Facing Tier A Runtime Invocation Boundary. Held as its own
    // narrow class, exactly mirroring ownerEvidenceDeletionAuthority's own "only this class's
    // own entry-point method reads it" isolation -- no other coordinator constructed in
    // buildAndRegisterRuntimeGraph ever receives a reference to it.
    private lateinit var tierAOwnerInvocationCoordinator: TierAOwnerInvocationCoordinator

    // Document Ingestion — Derivative Content Persistence and Retrieval. Held as its own narrow
    // class, mirroring tierAOwnerInvocationCoordinator's own isolation -- no other coordinator
    // constructed in buildAndRegisterRuntimeGraph ever receives a reference to it.
    private lateinit var tierAContentRetrievalCoordinator: TierAContentRetrievalCoordinator

    // Document Ingestion — Tier B Durable OCR Derivative Content. Held as its own narrow class,
    // mirroring tierAOwnerInvocationCoordinator's own isolation exactly -- no other coordinator
    // constructed in buildAndRegisterRuntimeGraph ever receives a reference to it.
    private lateinit var tierBOcrOwnerInvocationCoordinator: TierBOcrOwnerInvocationCoordinator

    // Document Ingestion — Tier B Durable OCR Derivative Content Retrieval. A separate class from
    // tierAContentRetrievalCoordinator (never modified or repurposed, Tier B scope lock §28),
    // mirroring its own isolation.
    private lateinit var tierBOcrContentRetrievalCoordinator: TierBOcrContentRetrievalCoordinator

    // External transcription Unit E: a separate owner-only, pre-admission operation. The real
    // provider is composed only after the enablement, profile, and credential gates are all Ready.
    private lateinit var externalTranscriptionOwnerInvocationCoordinator: ExternalTranscriptionOwnerInvocationCoordinator
    private var fidelityFirstAcceptanceCoordinator: FidelityFirstAcceptanceCoordinator? = null
    // FA.9.4P-A1E-R6.6A: composition only. No public runtime entry point is exposed until a
    // separately governed region-bound acceptance authority exists.
    private var governedRegionTranscriptionExecutionCoordinator: GovernedRegionTranscriptionExecutionCoordinator? = null
    private var regionAcceptanceExecutionCoordinator: parker.core.runtime.RegionAcceptanceExecutionCoordinatorV2? = null
    private var regionAcceptanceAuthorityCreationCoordinator: parker.core.runtime.RegionTranscriptionAcceptanceAuthorityCreationCoordinator? = null
    private var ordinaryRegionIngestionWorkflow: parker.core.runtime.OrdinaryRegionOwnerWorkflowPort? = null
    private var correctedPreparationService: parker.core.runtime.GovernedCorrectedPreparationService? = null
    private var ordinaryRegionCapabilityAcceptanceCoordinator: parker.core.runtime.OrdinaryRegionCapabilityPromotionPort? = null
    private var legacyOrdinaryRegionAcceptanceEvaluator: parker.core.runtime.OrdinaryRegionCapabilityAcceptanceEvaluator? = null
    private lateinit var governedAcquisitionOwnerWorkflow: GovernedAcquisitionOwnerWorkflow
    // UI-INGESTION-5: exact-target owner authorization for the existing enhanced-transcription
    // capability. Optional/additive -- null (feature absent) unless
    // PARKER_EXTERNAL_TRANSCRIPTION_AUTHORIZATION_STORAGE_ROOT is configured, preserving prior
    // behavior exactly for every deployment that does not yet opt in.
    private var externalTranscriptionAuthorizationCoordinator: parker.core.runtime.ExternalTranscriptionOwnerAuthorizationCoordinator? = null

    // Minimum Production Document Pipeline — Local Reasoning Implementation. Held as its own
    // narrow class, mirroring tierBOcrContentRetrievalCoordinator's own isolation -- no other
    // coordinator constructed in buildAndRegisterRuntimeGraph ever receives a reference to it.
    private lateinit var documentAnalysisCoordinator: DocumentAnalysisCoordinator

    // Reviewed Analysis Result — Explicit Owner Save. pendingAnalysisCache is read directly by
    // analyseDocumentsAsOwner (to register a freshly completed result) and held internally by
    // savedAnalysisCoordinator (to claim/finalize/release a pending id during Save) -- the only
    // two readers, mirroring this class's own established narrow-isolation discipline.
    private lateinit var pendingAnalysisCache: PendingAnalysisCache
    private lateinit var savedAnalysisCoordinator: SavedAnalysisCoordinator
    private lateinit var humanVerificationStorage: HumanVerificationStorage

    // OI11R6V-A5: internally composed only. No HTTP/UI/public recording entry point exists.
    private var humanFidelityReviewRecordingService: GovernedHumanFidelityReviewRecordingService? = null
    private var humanFidelityReviewExactTargetRegistrar: HumanFidelityReviewExactTargetRegistrar? = null
    // OI11R6V-A8A: read-only internal projection seam. It cannot mutate a review or derivative.
    private var effectiveHumanFidelityReviewProjector: EffectiveHumanFidelityReviewProjector? = null
    private var governedHumanCorrectionService: GovernedHumanCorrectionService? = null
    private var humanCorrectedRepresentationStorage: HumanCorrectedRepresentationStorage? = null
    private var humanCorrectionAudit: HumanCorrectionAudit? = null
    private var humanCorrectionExactTargetRegistrar: HumanCorrectionExactTargetRegistrar? = null
    private var humanCorrectedRepresentationRetrievalService: HumanCorrectedRepresentationRetrievalService? = null

    // Document Ingestion, Derivative-to-Memory-Core Registration. Held as its own narrow class,
    // exactly mirroring tierAOwnerInvocationCoordinator's own isolation -- no other coordinator
    // constructed in buildAndRegisterRuntimeGraph ever receives a reference to it. Unlike
    // tierAOwnerInvocationCoordinator, this one depends on memoryCore/permissionEngine, not
    // evidenceCustodian, per the adopted scope lock's own "MemoryCore and PermissionEngine only"
    // dependency boundary.
    private lateinit var derivativeMemoryRegistrationCoordinator: DerivativeMemoryRegistrationCoordinator

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
     * `ConversationHistorySource` type; (2a-ii, Sprint 11 Unit 8) construct
     * `InMemoryWorldModel` -- the
     * first production construction of the World Model anywhere in this
     * repository's real, running composition root
     * (`docs/architecture/WORLD_MODEL_SOURCE_GOVERNANCE_REVIEW.md` Finding
     * 1); (2b, Knowledge Discoverability and Governed Retrieval into
     * Reasoning Context, Implementation Unit 3) construct
     * `DefaultReasoningKnowledgeSource` from the same, already-shared
     * `knowledgeItemPersistence` and `permissionEngine` instances
     * `DefaultKnowledgeRetrieval` itself uses, plus a new Purpose-bound
     * `MemoryRetrieval` view; (2c, Sprint 11 Unit 3/6/7/8, revised by
     * Implementation Unit 3) construct
     * the [ReasoningContextAssembler] (`DefaultReasoningContextAssembler`),
     * injecting the already-constructed `identityService`, `toolRegistry`,
     * `conversationHistorySource`, the new `reasoningKnowledgeSource`, and
     * `worldModelSource`;
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
        openAiExternalTranscriptionReadiness = stage("OpenAI external transcription provider profile readiness") {
            OpenAiExternalTranscriptionProviderReadinessEvaluator { clock().atZone(java.time.ZoneOffset.UTC).toLocalDate() }
                .evaluate(
                    config.openAiExternalTranscriptionEnabled,
                    config.openAiExternalTranscriptionProviderProfilePath,
                )
        }
        openAiExternalTranscriptionBackendReadiness = externalTranscriptionBackendReadiness(
            openAiExternalTranscriptionReadiness,
            config.openAiApiCredential,
        )
        val resourceRegistry = InMemoryResourceRegistry()
        val vocabulary = InMemoryActionVocabulary()
        val actionMapper = ActionMapper(vocabulary)
        val humanFidelityReviewConfigured = when {
            config.humanFidelityReviewStorageRootPath == null &&
                config.humanFidelityGovernanceAuditStorageRootPath == null -> false
            config.humanFidelityReviewStorageRootPath != null &&
                config.humanFidelityGovernanceAuditStorageRootPath != null -> true
            else -> throw ParkerRuntimeException.InvalidConfiguration(
                ParkerRuntimeConfigLoader.KEY_HUMAN_FIDELITY_REVIEW_STORAGE_ROOT,
                "human fidelity review and governance audit roots must be configured together",
            )
        }
        val humanCorrectionConfigured = when {
            config.humanCorrectedRepresentationStorageRootPath == null && config.humanCorrectionAuditStorageRootPath == null -> false
            config.humanCorrectedRepresentationStorageRootPath != null && config.humanCorrectionAuditStorageRootPath != null && humanFidelityReviewConfigured -> true
            else -> throw ParkerRuntimeException.InvalidConfiguration(
                ParkerRuntimeConfigLoader.KEY_HUMAN_CORRECTED_REPRESENTATION_STORAGE_ROOT,
                "human-corrected representation and correction-audit roots require each other and human-fidelity storage",
            )
        }
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
            authorizationPurposeRegistry.register(REASONING_CONTEXT_RETRIEVAL_PURPOSE)
            authorizationPurposeRegistry.register(ExternalTranscriptionInvocationGate.AUTHORIZATION_PURPOSE)
            if (humanFidelityReviewConfigured) {
                HumanFidelityReviewRecordingPermissionPolicy.registerPurpose(authorizationPurposeRegistry)
            }
            if (humanCorrectionConfigured) HumanCorrectionPermissionPolicy.registerPurpose(authorizationPurposeRegistry)
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

        // Knowledge Discoverability and Governed Retrieval into Reasoning Context, Implementation
        // Unit 3: the legacy InMemoryKnowledgeStore()/memorySource production binding this comment
        // block, and its own Sprint 11 Unit 7 construction step, previously occupied here is
        // retired -- no production path constructs InMemoryKnowledgeStore any longer.
        // DefaultReasoningContextAssembler's own fourth dependency is now a ReasoningKnowledgeSource,
        // constructed further below (Implementation Plan Section 8), alongside the same, already-
        // shared knowledgeItemPersistence and permissionEngine instances DefaultKnowledgeRetrieval
        // itself uses -- neither of which exists yet at this point in construction, so
        // reasoningContextAssembler's own assignment (its declaration remains the same
        // private lateinit var field, read only later inside submitOwnerMessage, never during
        // construction by another coordinator) moves there too, in the same atomic change as this
        // retirement. KnowledgeSource, KnowledgeStore, and InMemoryKnowledgeStore are not deleted --
        // only this one production wiring site.

        // Sprint 11 Unit 8: InMemoryWorldModel is constructed here for the first time in this
        // repository's production composition root -- nowhere before this Unit did anything
        // construct WorldModel/InMemoryWorldModel in the real, running system (Governance Review
        // Finding 1), the identical situation Memory Source Integration (Unit 7) faced. This is a
        // new construction step, not a reordering: InMemoryWorldModel takes only its defaulted
        // DefaultWorldModelUpdatePolicy, so no new ParkerRuntimeConfig field or ordering
        // constraint is introduced beyond existing before the Assembler. Exactly one instance is
        // constructed and exposed to the Assembler only through the narrower WorldModelSource
        // type -- no duplicate ownership, no duplicate state, mirroring precisely how
        // InMemoryConversationEngine is itself constructed once and exposed through two interfaces
        // on the same instance.
        val inMemoryWorldModel = InMemoryWorldModel()
        val worldModelSource: WorldModelSource = inMemoryWorldModel

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
                Triple(
                    DefaultEvidenceCustodian.EVIDENCE_MANIFEST_RETRIEVAL_RESOURCE_ID,
                    ResourceType.DOCUMENT,
                    "Evidence Custodian Manifest Retrieval",
                ),
                Triple(EvidenceRegistrationCoordinator.MEMORY_CORE_PROVENANCE_RESOURCE_ID, ResourceType.MEMORY, "Memory Core Provenance Creation"),
                Triple(EvidenceRegistrationCoordinator.MEMORY_CORE_DOCUMENT_REGISTRATION_RESOURCE_ID, ResourceType.MEMORY, "Memory Core Document Registration"),
                Triple(DefaultOwnerEvidenceDeletionAuthority.EVIDENCE_DELETION_RESOURCE_ID, ResourceType.DOCUMENT, "Evidence Custodian Deletion"),
                // Document Ingestion, Derivative-to-Memory-Core Registration (Scope Lock Section
                // 18): the same registration EvidenceRegistrationCoordinator's own two Memory Core
                // resources above already require -- DefaultPermissionPolicy resolves a target
                // Resource's own registered resourceType, so an unregistered ResourceId cannot be
                // approved regardless of any matching ActionVocabulary/policy rule.
                Triple(
                    DerivativeMemoryRegistrationCoordinator.DERIVATIVE_MEMORY_PROVENANCE_RESOURCE_ID,
                    ResourceType.MEMORY,
                    "Derivative Memory Core Provenance Creation",
                ),
                Triple(
                    DerivativeMemoryRegistrationCoordinator.DERIVATIVE_MEMORY_DOCUMENT_REGISTRATION_RESOURCE_ID,
                    ResourceType.MEMORY,
                    "Derivative Memory Core Document Registration",
                ),
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
            if (humanFidelityReviewConfigured) {
                vocabulary.register(
                    ActionVocabularyEntry(
                        verbPhrase = HumanFidelityReviewRecordingPermissionPolicy.RECORD_ACTION_NAME,
                        mappings = setOf(ActionResourceMapping(PermissionAction.WRITE, ResourceType.DOCUMENT)),
                    ),
                )
            }
            if (humanCorrectionConfigured) {
                vocabulary.register(ActionVocabularyEntry(
                    verbPhrase = HumanCorrectionPermissionPolicy.CORRECT_ACTION_NAME,
                    mappings = setOf(ActionResourceMapping(PermissionAction.WRITE, ResourceType.DOCUMENT)),
                ))
            }
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
            // Document Ingestion, Authoritative Source Manifest Foundation Implementation
            // (Scope Lock Section 17): the same (READ, DOCUMENT) pair evidence.retrieve already
            // uses -- no new PermissionAction or ResourceType is introduced.
            vocabulary.register(
                ActionVocabularyEntry(
                    verbPhrase = DefaultEvidenceCustodian.RETRIEVE_MANIFEST_ACTION_NAME,
                    mappings = setOf(ActionResourceMapping(PermissionAction.READ, ResourceType.DOCUMENT)),
                ),
            )
            // Document Ingestion, Owner-Authorized Local File Ingress (Scope Lock Section 3): a
            // new, distinct verb phrase, mapped to the identical existing (WRITE, DOCUMENT) pair
            // "evidence.accept" already uses -- no new PermissionAction or ResourceType is
            // introduced, and the existing coarse (WRITE, DOCUMENT) policy rule above already
            // governs it, exactly as it already, today, governs "evidence.accept" itself.
            vocabulary.register(
                ActionVocabularyEntry(
                    verbPhrase = OwnerLocalFileIngressCoordinator.LOCAL_FILE_READ_ACTION_NAME,
                    mappings = setOf(ActionResourceMapping(PermissionAction.WRITE, ResourceType.DOCUMENT)),
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
            // Document Ingestion, Derivative-to-Memory-Core Registration (Scope Lock Section 18):
            // two new, distinct verb phrases, mapped to the identical existing (WRITE, MEMORY)
            // pair "memory.create-provenance"/"memory.register-document" already use -- no new
            // PermissionAction or ResourceType is introduced, and the existing coarse (WRITE,
            // MEMORY) policy rule above already governs both, exactly as it already, today,
            // governs EvidenceRegistrationCoordinator's own two writes.
            vocabulary.register(
                ActionVocabularyEntry(
                    verbPhrase = DerivativeMemoryRegistrationCoordinator.CREATE_PROVENANCE_ACTION_NAME,
                    mappings = setOf(ActionResourceMapping(PermissionAction.WRITE, ResourceType.MEMORY)),
                ),
            )
            vocabulary.register(
                ActionVocabularyEntry(
                    verbPhrase = DerivativeMemoryRegistrationCoordinator.REGISTER_DOCUMENT_ACTION_NAME,
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
                // Unit 4's two Purpose-plus-verb approvals below outrank these guards only for
                // the exact active candidate-evaluation Purpose. Every other Purpose state still
                // resolves through these guards rather than the coarse approvals above.
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
                // Gap #54 Memory Retrieval Operationalisation Unit 4: the complete and only
                // production Memory Core retrieval authority introduced by this programme.
                // These rules authorize governed candidate-evidence resolution, not the caller,
                // and do not approve Evidence Intelligence or any absent/other Purpose.
                PermissionPolicyRule(
                    action = PermissionAction.READ,
                    resourceType = ResourceType.MEMORY,
                    outcome = PermissionDecisionOutcome.APPROVED,
                    level = PermissionLevel.AUTOMATIC,
                    authorizationPurpose = KNOWLEDGE_CANDIDATE_EVALUATION_PURPOSE,
                    proposedAction = PermissionFilteredMemoryRetrieval.RETRIEVE_ACTION_NAME,
                ),
                PermissionPolicyRule(
                    action = PermissionAction.READ,
                    resourceType = ResourceType.DOCUMENT,
                    outcome = PermissionDecisionOutcome.APPROVED,
                    level = PermissionLevel.AUTOMATIC,
                    authorizationPurpose = KNOWLEDGE_CANDIDATE_EVALUATION_PURPOSE,
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
                // External transcription is a distinct disclosure act. The verb-only guard
                // outranks the coarse EXECUTE/DOCUMENT approval for every absent, unknown,
                // retired, or mismatched Purpose. Only the exact active Purpose-plus-verb rule
                // below has greater specificity; list order is irrelevant by policy design.
                PermissionPolicyRule(
                    action = PermissionAction.EXECUTE,
                    resourceType = ResourceType.DOCUMENT,
                    outcome = PermissionDecisionOutcome.DENIED,
                    level = PermissionLevel.AUTOMATIC,
                    proposedAction = ExternalTranscriptionInvocationGate.ACTION_NAME,
                ),
                PermissionPolicyRule(
                    action = PermissionAction.EXECUTE,
                    resourceType = ResourceType.DOCUMENT,
                    outcome = PermissionDecisionOutcome.APPROVED,
                    level = PermissionLevel.AUTOMATIC,
                    authorizationPurpose = ExternalTranscriptionInvocationGate.AUTHORIZATION_PURPOSE,
                    proposedAction = ExternalTranscriptionInvocationGate.ACTION_NAME,
                ),
                // Knowledge Discoverability and Governed Retrieval into Reasoning Context,
                // Implementation Unit 3 (Contract Design Section 7; Scope Lock Section 4). Fail-closed
                // guard for the new verb -- specificity 1, outranks the existing coarse (READ, MEMORY)
                // approval above (specificity 0) for this verb only; leaves that coarse rule, and
                // every other verb it still governs, completely unchanged.
                PermissionPolicyRule(
                    action = PermissionAction.READ,
                    resourceType = ResourceType.MEMORY,
                    outcome = PermissionDecisionOutcome.DENIED,
                    level = PermissionLevel.AUTOMATIC,
                    proposedAction = DefaultReasoningKnowledgeSource.RETRIEVE_FOR_REASONING_CONTEXT_ACTION_NAME,
                ),
                // Specificity 2: outranks the guard immediately above only for a request carrying
                // this exact, active Purpose.
                PermissionPolicyRule(
                    action = PermissionAction.READ,
                    resourceType = ResourceType.MEMORY,
                    outcome = PermissionDecisionOutcome.APPROVED,
                    level = PermissionLevel.AUTOMATIC,
                    authorizationPurpose = REASONING_CONTEXT_RETRIEVAL_PURPOSE,
                    proposedAction = DefaultReasoningKnowledgeSource.RETRIEVE_FOR_REASONING_CONTEXT_ACTION_NAME,
                ),
                // Memory Core evidence resolution: the existing Gap #54 Unit 2 DENIED guard for
                // memory.retrieve (above) already governs every Purpose without a specificity-2
                // override -- only this one rule is added. No memory.retrieve_document rule is added
                // for this Purpose -- least authority (Contract Invariant 13): the locked algorithm
                // never calls getDocument, so no Document-retrieval authority is granted; the
                // pre-existing Gap #54 verb-only DENIED guard for memory.retrieve_document remains
                // untouched and applicable to this Purpose exactly as to every other Purpose lacking
                // its own specificity-2 override.
                PermissionPolicyRule(
                    action = PermissionAction.READ,
                    resourceType = ResourceType.MEMORY,
                    outcome = PermissionDecisionOutcome.APPROVED,
                    level = PermissionLevel.AUTOMATIC,
                    authorizationPurpose = REASONING_CONTEXT_RETRIEVAL_PURPOSE,
                    proposedAction = PermissionFilteredMemoryRetrieval.RETRIEVE_ACTION_NAME,
                ),
            ) + (if (humanFidelityReviewConfigured) listOf(
                PermissionPolicyRule(
                    action = PermissionAction.WRITE,
                    resourceType = ResourceType.DOCUMENT,
                    outcome = PermissionDecisionOutcome.DENIED,
                    level = PermissionLevel.AUTOMATIC,
                    proposedAction = HumanFidelityReviewRecordingPermissionPolicy.RECORD_ACTION_NAME,
                ),
                PermissionPolicyRule(
                    action = PermissionAction.WRITE,
                    resourceType = ResourceType.DOCUMENT,
                    outcome = PermissionDecisionOutcome.APPROVED,
                    level = PermissionLevel.HIGH_ASSURANCE,
                    authorizationPurpose = parker.core.interfaces.HUMAN_FIDELITY_REVIEW_RECORDING_PURPOSE,
                    proposedAction = HumanFidelityReviewRecordingPermissionPolicy.RECORD_ACTION_NAME,
                ),
            ) else emptyList()) + (if (humanCorrectionConfigured) listOf(
                PermissionPolicyRule(
                    action = PermissionAction.WRITE,
                    resourceType = ResourceType.DOCUMENT,
                    outcome = PermissionDecisionOutcome.DENIED,
                    level = PermissionLevel.AUTOMATIC,
                    proposedAction = HumanCorrectionPermissionPolicy.CORRECT_ACTION_NAME,
                ),
                PermissionPolicyRule(
                    action = PermissionAction.WRITE,
                    resourceType = ResourceType.DOCUMENT,
                    outcome = PermissionDecisionOutcome.APPROVED,
                    level = PermissionLevel.HIGH_ASSURANCE,
                    authorizationPurpose = parker.core.interfaces.HUMAN_TRANSCRIPTION_CORRECTION_PURPOSE,
                    proposedAction = HumanCorrectionPermissionPolicy.CORRECT_ACTION_NAME,
                ),
            ) else emptyList()),
        )
        permissionEngine = DefaultPermissionEngine(identityService, permissionPolicy)

        var composedHumanFidelityStorage: HumanFidelityReviewStorage? = null
        if (humanFidelityReviewConfigured) {
            val humanFidelityAudit = stage("Human fidelity governance audit construction") {
                FileSystemHumanFidelityGovernanceAudit(
                    Path.of(requireNotNull(config.humanFidelityGovernanceAuditStorageRootPath)),
                )
            }
            val humanFidelityStorage = stage("Human fidelity review storage construction") {
                FileSystemHumanFidelityReviewStorage(
                    Path.of(requireNotNull(config.humanFidelityReviewStorageRootPath)),
                    humanFidelityAudit,
                )
            }
            composedHumanFidelityStorage = humanFidelityStorage
            val humanFidelityPermission = HumanFidelityReviewRecordingPermissionPolicy(
                PrincipalId(config.ownerPrincipalId),
                authorizationPurposeRegistry,
                permissionEngine,
            )
            humanFidelityReviewRecordingService = DefaultGovernedHumanFidelityReviewRecordingService(
                humanFidelityPermission,
                humanFidelityStorage,
            )
            effectiveHumanFidelityReviewProjector = DefaultEffectiveHumanFidelityReviewProjector(
                humanFidelityStorage,
            )
            humanFidelityReviewExactTargetRegistrar = HumanFidelityReviewExactTargetRegistrar(
                resourceRegistry,
                PrincipalId(config.ownerPrincipalId),
                clock,
            )
        }
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
        // anywhere in this composition root -- knowledgeItemPersistence (Programme 3's own Knowledge
        // layer, constructed separately, elsewhere in this same composition sequence) is a distinct
        // interface and has never required a live MemoryCore instance. Plain InMemoryMemoryCore,
        // wrapped by DurableMemoryCore, is used
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

        // Document Ingestion, Authoritative Source Manifest Foundation Implementation. A sibling
        // storage root to evidenceArtifactStorage, never nested inside it -- the Authoritative
        // Evidence Source Manifest is never itself an EvidenceArtifact (Scope Lock Section 4/20).
        val evidenceSourceManifestStorage = stage("Evidence Custodian source manifest storage construction") {
            FileSystemEvidenceSourceManifestStorage(Path.of(config.evidenceSourceManifestStorageRootPath))
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
        val defaultEvidenceCustodian = DefaultEvidenceCustodian(
            evidenceArtifactStorage,
            permissionEngine,
            evidenceSourceManifestStorage,
        )
        evidenceCustodian = defaultEvidenceCustodian
        ownerEvidenceListing = parker.core.runtime.FileSystemOwnerEvidenceListing(
            Path.of(config.evidenceSourceManifestStorageRootPath),
            PrincipalId(config.ownerPrincipalId),
            defaultEvidenceCustodian,
        )
        evidenceRegistrationCoordinator = EvidenceRegistrationCoordinator(defaultEvidenceCustodian, memoryCore, permissionEngine)
        // Document Ingestion, Derivative-to-Memory-Core Registration. Depends on memoryCore and
        // permissionEngine only -- never evidenceCustodian, never DerivativeGenerationStorage,
        // never either existing Document Ingestion owner-invocation coordinator (Scope Lock
        // Section 4.C).
        derivativeMemoryRegistrationCoordinator = DerivativeMemoryRegistrationCoordinator(memoryCore, permissionEngine)

        // Document Ingestion, Owner-Authorized Local File Ingress. Unlike
        // tierAOwnerInvocationCoordinator (below), this coordinator performs its own, new,
        // distinct gated act -- reading a local file the owner designates -- before ever
        // reaching defaultEvidenceCustodian.accept's own, separately gated path, so it holds a
        // permissionEngine reference of its own in addition to defaultEvidenceCustodian.
        ownerLocalFileIngressCoordinator = OwnerLocalFileIngressCoordinator(permissionEngine, defaultEvidenceCustodian)

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
            evidenceSourceManifestStorage,
        )

        // Document Ingestion, Owner-Facing Tier A Runtime Invocation Boundary. Wires the
        // already-governed, already-implemented Tier A composition (Implementation Unit,
        // DOCUMENT_INGESTION_TIER_A_IMPLEMENTATION_CLOSURE.md) into this runtime for the first
        // time -- construction alone performs no I/O beyond the two storage/audit constructions
        // below (mirroring evidenceArtifactStorage's own precedent) and starts no background
        // work, watcher, or scheduled task of any kind; the router is only ever reached through
        // invokeTierAIngestionAsOwner, below.
        val derivativeGenerationStorage = stage("Document Ingestion derivative generation storage construction") {
            FileSystemDerivativeGenerationStorage(Path.of(config.derivativeGenerationStorageRootPath))
        }
        val documentIngestionAudit = stage("Document Ingestion audit construction") {
            FileSystemDocumentIngestionAudit(Path.of(config.documentIngestionAuditLogPath))
        }
        // Document Ingestion — Derivative Content Persistence and Retrieval
        // (DOCUMENT_INGESTION_DERIVATIVE_CONTENT_PERSISTENCE_RETRIEVAL_SCOPE_LOCK.md). A wholly
        // separate, subordinate store from derivativeGenerationStorage above -- own storage root,
        // own file extension (`.content`), never nested inside it (Scope Lock §4).
        val derivativeContentStorage = stage("Document Ingestion derivative content storage construction") {
            FileSystemDerivativeContentStorage(Path.of(config.derivativeContentStorageRootPath))
        }
        if (humanCorrectionConfigured) {
            val correctionAudit = stage("Human correction audit construction") {
                FileSystemHumanCorrectionAudit(Path.of(requireNotNull(config.humanCorrectionAuditStorageRootPath)))
            }
            val correctionStorage = stage("Human-corrected representation storage construction") {
                FileSystemHumanCorrectedRepresentationStorage(
                    Path.of(requireNotNull(config.humanCorrectedRepresentationStorageRootPath)),
                )
            }
            val correctionPermission = HumanCorrectionPermissionPolicy(
                OpaqueOwnerPrincipal(PrincipalId(requireNotNull(config.ownerHighAuthorityPrincipalId
                    ?: throw ParkerRuntimeException.MissingConfiguration(
                        ParkerRuntimeConfigLoader.KEY_OWNER_HIGH_AUTHORITY_PRINCIPAL_ID)))),
                authorizationPurposeRegistry, permissionEngine,
                ExternalFileOwnerHighAuthorityVerification.load(Path.of(requireNotNull(
                    config.ownerHighAuthorityVerificationCredentialFilePath
                        ?: throw ParkerRuntimeException.MissingConfiguration(
                            ParkerRuntimeConfigLoader.KEY_OWNER_HIGH_AUTHORITY_VERIFICATION_CREDENTIAL_FILE),
                ))),
            )
            governedHumanCorrectionService = DefaultGovernedHumanCorrectionService(
                correctionPermission,
                requireNotNull(composedHumanFidelityStorage),
                requireNotNull(effectiveHumanFidelityReviewProjector),
                StoredHumanCorrectionProviderResolver(derivativeGenerationStorage, derivativeContentStorage),
                correctionStorage,
                correctionAudit,
            )
            humanCorrectedRepresentationStorage = correctionStorage
            humanCorrectionAudit = correctionAudit
            humanCorrectedRepresentationRetrievalService = HumanCorrectedRepresentationRetrievalService(
                correctionStorage,
                DefaultHumanCorrectedRepresentationEligibilityEvaluator(
                    requireNotNull(composedHumanFidelityStorage),
                    requireNotNull(effectiveHumanFidelityReviewProjector),
                ),
            )
            humanCorrectionExactTargetRegistrar = HumanCorrectionExactTargetRegistrar(
                resourceRegistry, PrincipalId(config.ownerPrincipalId), clock,
            )
        }
        val tierADocumentIngestionRouter = TierADocumentIngestionComposition.create(derivativeGenerationStorage, documentIngestionAudit, derivativeContentStorage)
        tierAOwnerInvocationCoordinator = TierAOwnerInvocationCoordinator(defaultEvidenceCustodian, tierADocumentIngestionRouter)
        tierAContentRetrievalCoordinator = TierAContentRetrievalCoordinator(
            derivativeGenerationStorage,
            derivativeContentStorage,
            effectiveHumanFidelityReviewProjector,
            humanCorrectedRepresentationRetrievalService,
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

        // Hoisted so the Minimum Production Document Pipeline's own documentAnalysisCoordinator
        // (wired below, after tierBOcrContentRetrievalCoordinator) can reuse this exact same
        // ModelInferenceClient instance -- the currently configured LOCAL implementation only --
        // rather than constructing a second HTTP client instance pointed at the same endpoint.
        val modelInferenceClient = LocalHttpModelInferenceClient(config.modelEndpointUrl, config.modelName)
        val reasoningProvider = stage("Reasoning Provider construction") {
            LoggingReasoningProvider(
                ExplicitOwnerPersistenceDirectiveReasoningProvider(
                    ownerPrincipalId = PrincipalId(config.ownerPrincipalId),
                    classifier = DefaultExplicitOwnerPersistenceDirectiveClassifier(),
                    delegate = ModelReasoningProvider(
                        promptBuilder = DefaultReasoningPromptBuilder(),
                        modelInferenceClient = modelInferenceClient,
                        responseParser = TaggedReasoningResponseParser(),
                        timeoutMs = config.modelTimeoutMs,
                    ),
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
        // verb-specific DENIED guards above outrank existing coarse approvals. Unit 3 creates two
        // immutable Purpose-bound views below; both delegate to this one parent and neither can
        // override policy. Unit 4 authorizes only the candidate view's two exact retrieval verbs;
        // the Evidence Intelligence view and every non-candidate state remain fail-closed.
        val permissionFilteredMemoryRetrieval = PermissionFilteredMemoryRetrieval(durableMemoryCore, permissionEngine)
        val candidateEvaluationMemoryRetrieval =
            permissionFilteredMemoryRetrieval.forAuthorizationPurpose(KNOWLEDGE_CANDIDATE_EVALUATION_PURPOSE)
        val evidenceIntelligenceMemoryRetrieval =
            permissionFilteredMemoryRetrieval.forAuthorizationPurpose(EVIDENCE_INTELLIGENCE_INPUT_RESOLUTION_PURPOSE)

        // Programme 3, Knowledge Memory, Unit 8 ("Constitutional Knowledge Submission"), and now
        // also Unit 9.6 ("Runtime Composition"). One recovered DurableKnowledgeItemPersistence
        // for the lifetime of this ParkerRuntime -- never recreated per invocation, shared
        // unchanged between the write side (knowledgeSubmission, below) and the read side
        // (knowledgeRetrieval, below) -- never a second, parallel persistence instance, so
        // anything knowledgeSubmission successfully promotes is genuinely reachable through
        // knowledgeRetrieval.
        val knowledgeItemDurabilityLog = stage("Knowledge Item durability log construction") {
            FileSystemKnowledgeItemDurabilityLog(Path.of(config.knowledgeItemDurabilityLogPath))
        }
        val knowledgeItemPersistence = stage("Knowledge Item recovery") {
            DurableKnowledgeItemPersistence.create(knowledgeItemDurabilityLog)
        }
        val knowledgeCandidateEvaluator = DefaultKnowledgeCandidateEvaluator(candidateEvaluationMemoryRetrieval)
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
        //
        // Unit 9.7.4 ("Integrity Validation, Canonical Token Re-resolution, and Fresh Pre-disclosure
        // Re-verification") added RelevanceMechanism as a required DefaultKnowledgeRetrieval
        // constructor dependency -- structurally unavoidable, since that Unit must be able to call
        // RelevanceMechanism.rank() from inside DefaultKnowledgeRetrieval.retrieve() itself.
        //
        // Programme 3, Unit 9.7.5 ("Runtime Composition Wiring"). This is the sole, separately
        // authorised unit permitted to construct QmdRelevanceMechanism, its
        // QmdRelevanceMechanismConfiguration, or any node/model/bridge-path runtime configuration at
        // this site -- superseding the Unit 9.7.4 -> Unit 9.7.5 compile-preserving fail-closed
        // placeholder this site previously held (that placeholder threw immediately and
        // unconditionally on any invocation; it is now fully replaced, not merely widened).
        //
        // DefaultKnowledgeRetrieval itself remains mechanism-neutral: it knows only the
        // RelevanceMechanism interface (Unit 9.7.1), never QmdRelevanceMechanism concretely -- the
        // concrete QMD composition lives here, and only here, exactly as it did not before this
        // Unit's own change. No authority transfers to QMD by this wiring: qmdConfiguration and
        // qmdInvoker below carry no KnowledgeItemPersistence, no PermissionEngine, and no other
        // canonical-Parker-state handle -- the same architectural-inability-by-omission technique
        // this file already relies on elsewhere, and QmdRelevanceMechanism.kt's own constructor
        // shape makes any other composition impossible to express.
        //
        // Frozen mechanism identity/version/configuration (Section 13/13.1 spike evidence record;
        // Contract and Permission Successor Section 3 condition 14; Implementation Plan Section 12).
        // mechanismName, qmdVersion, embeddingModelUri, vectorDimension, similarityMetric, and
        // bridgeProtocolVersion are fixed literals here -- never read from `config` or any other
        // mutable environment state -- because they are retrieval-relevant identity: a changed value
        // is a disclosed, governed reconfiguration (a new, reviewed ParkerRuntime.kt diff), never
        // silent per-deployment drift. qmdVersion "2.8.3" and embeddingModelUri
        // "hf:ggml-org/embeddinggemma-300M-GGUF/embeddinggemma-300M-Q8_0.gguf" (768-dimension,
        // cosine similarity) are exactly the values the spike evidence record's own accepted
        // six-candidate emergency-vet fixture used, and exactly what
        // QmdRelevanceMechanismLiveAcceptanceTest.kt's own liveConfiguration() already freezes for
        // this same mechanism identity -- restated here, not reinvented. embeddingModelFileSha256 is
        // left at its own class default (`null`): no adopted governance document freezes a specific
        // file hash, and this Unit does not invent one. similarityMetric and bridgeProtocolVersion
        // are left at their own class defaults ("cosine", "1"), which already match this exact
        // identity -- restating them here would only risk a future silent divergence between two
        // copies of the same frozen value.
        //
        // Deployment-specific locations (node executable, bridge script, tsx loader, model cache,
        // timeout, QMD source root) come from `config` -- ParkerRuntimeConfig's own new, narrowly-added
        // qmdNodeExecutablePath/qmdBridgeScriptPath/qmdTsxCliPath/qmdModelCacheDir/qmdTimeoutMillis/
        // qmdSourceRoot fields (qmdSourceRoot added by the Main-Promotion Gate / Production QMD
        // Bridge Portability Correction follow-on, mirroring the others exactly; see that class's own
        // KDoc for why each is optional rather than required-with-no-default) -- mirroring exactly how
        // modelEndpointUrl/modelName already reach LocalHttpModelInferenceClient below, never a
        // second, parallel configuration mechanism. No Windows-specific developer path is hard-coded
        // at this site: every path-shaped value above is either a portable convention
        // (qmdNodeExecutablePath's own "node" default, qmdBridgeScriptPath's own repository-relative
        // default) or comes from `config`, resolved from environment/properties exactly as this
        // repository's other local-runtime dependencies already are. qmdSourceRoot in particular
        // replaces what were, before this correction, two hard-coded, Steve-specific absolute Windows
        // import paths inside `tools/qmd-relevance-bridge.mts` itself (see that script's own header
        // comment, and `QmdRelevanceMechanismConfiguration.qmdSourceRoot`'s own KDoc, for the full
        // account) -- this composition site never hard-codes that location either, only reads it from
        // `config`, same as every other deployment-specific QMD value above.
        val qmdConfiguration = QmdRelevanceMechanismConfiguration(
            qmdVersion = "2.8.3",
            embeddingModelUri = "hf:ggml-org/embeddinggemma-300M-GGUF/embeddinggemma-300M-Q8_0.gguf",
            vectorDimension = 768,
            nodeExecutablePath = config.qmdNodeExecutablePath,
            additionalNodeArguments = listOfNotNull(config.qmdTsxCliPath),
            bridgeScriptPath = config.qmdBridgeScriptPath,
            modelCacheDir = config.qmdModelCacheDir,
            timeoutMillis = config.qmdTimeoutMillis,
            qmdSourceRoot = config.qmdSourceRoot,
        )
        val qmdInvoker = ProcessBuilderQmdSubprocessInvoker(qmdConfiguration)
        val relevanceMechanism: RelevanceMechanism = QmdRelevanceMechanism(qmdConfiguration, qmdInvoker)

        knowledgeRetrieval = DefaultKnowledgeRetrieval(knowledgeItemPersistence, permissionEngine, relevanceMechanism)

        // Knowledge Discoverability and Governed Retrieval into Reasoning Context, Implementation
        // Unit 3 (Contract Design Section 7, Section 12; Scope Lock Section 4, Section 7). A third
        // immutable Purpose-bound view over the same permissionFilteredMemoryRetrieval parent above --
        // never a second, parallel MemoryRetrieval decorator -- authorizing only this Purpose's own
        // two reachable verbs (knowledge.retrieve_for_reasoning_context, memory.retrieve); no
        // Document authority. DefaultReasoningKnowledgeSource reuses the same, already-shared
        // knowledgeItemPersistence and permissionEngine instances DefaultKnowledgeRetrieval itself
        // uses immediately above -- never a second, parallel persistence instance -- so anything
        // knowledgeSubmission successfully promotes is genuinely reachable through this new surface
        // too. clock is left defaulted (the real system clock).
        // RKS.5 ("Runtime Composition Wiring", Reasoning Context Bounded Semantic Relevance
        // Implementation Plan Section 9). Reuses the identical relevanceMechanism instance
        // constructed above for DefaultKnowledgeRetrieval -- never a second, independently
        // configured instance -- satisfying the Successor document's own "Shared Unit 9.7
        // Mechanism Reuse" requirement (Section 8) by construction: both surfaces are backed by
        // the same, single QmdRelevanceMechanism object, with the same frozen identity/version/
        // configuration.
        val reasoningContextMemoryRetrieval =
            permissionFilteredMemoryRetrieval.forAuthorizationPurpose(REASONING_CONTEXT_RETRIEVAL_PURPOSE)
        val reasoningKnowledgeSource: ReasoningKnowledgeSource = DefaultReasoningKnowledgeSource(
            knowledgeItemPersistence,
            permissionEngine,
            reasoningContextMemoryRetrieval,
            REASONING_CONTEXT_RETRIEVAL_PURPOSE,
            relevanceMechanism,
        )

        // Cutover, atomically, in this same unit and the same reviewable change as
        // DefaultReasoningContextAssembler's own constructor-signature change: the legacy
        // InMemoryKnowledgeStore()/memorySource production binding is retired (above), and
        // DefaultReasoningContextAssembler is constructed here instead with the new
        // reasoningKnowledgeSource in that position -- there is no intermediate state, and no
        // separately reviewed unit boundary, at which the assembler declares one dependency type
        // while production still supplies the other. reasoningContextAssembler is a private
        // lateinit var field (declared once, near the top of this class) read only later, inside
        // submitOwnerMessage -- moving its own assignment here, after its now-real dependencies
        // exist, changes no other coordinator's own construction, since none of them read this
        // field during construction.
        reasoningContextAssembler = stage("Reasoning Context Assembler construction") {
            DefaultReasoningContextAssembler(identityService, toolRegistry, conversationHistorySource, reasoningKnowledgeSource, worldModelSource)
        }

        val evidenceIntelligenceInputResolver = EvidenceIntelligenceInputResolver(defaultEvidenceCustodian, evidenceIntelligenceMemoryRetrieval)
        val evidenceIntelligenceReasoningCoordinator = EvidenceIntelligenceReasoningCoordinator(reasoningProvider)

        // OCR Mechanism, Unit 12 ("Runtime Composition"). Governed in full by
        // docs/architecture/OCR_MECHANISM_UNIT_12_RUNTIME_INVOCATION_SCOPE_LOCK.md and
        // docs/architecture/OCR_MECHANISM_UNIT_12_IMPLEMENTATION_PLAN.md Section 5.A-5.K. Exactly the
        // already-adopted composition chain: DoclingOcrProviderAdapter -> OcrExecutionSequencer
        // (OcrMechanism) -> EvidenceIntelligenceOcrCoordinator -> DefaultEvidenceIntelligence's third,
        // nullable constructor parameter. No new Resource, ActionVocabulary entry, or
        // PermissionPolicyRule is added anywhere in this stage -- the existing (EXECUTE, DOCUMENT)
        // invocation-level gate, already registered above, already governs every analyseEvidence call
        // regardless of analysisKind (Unit 12 Scope Lock Section 5). Every path-shaped value below is
        // either a portable convention (doclingPythonExecutablePath's own "python3" default,
        // doclingBridgeScriptPath's own repository-relative default) or comes from config, resolved
        // from environment exactly as this repository's other local-runtime dependencies (QMD,
        // immediately above) already are -- no development-machine-specific path (for example, a
        // ~/docling-venv virtual environment location) is hard-coded at this site.
        val doclingConfiguration = DoclingOcrProviderAdapterConfiguration(
            pythonExecutablePath = config.doclingPythonExecutablePath,
            bridgeScriptPath = config.doclingBridgeScriptPath,
            modelCacheDir = config.doclingModelCacheDir,
            timeoutMillis = config.doclingTimeoutMillis,
        )
        val doclingOcrProviderAdapter = DoclingOcrProviderAdapter(doclingConfiguration, ProcessBuilderDoclingSubprocessInvoker(doclingConfiguration))
        val ocrMechanism: OcrMechanism = OcrExecutionSequencer(doclingOcrProviderAdapter)
        val evidenceIntelligenceOcrCoordinator = EvidenceIntelligenceOcrCoordinator(defaultEvidenceCustodian, ocrMechanism)

        evidenceIntelligence = DefaultEvidenceIntelligence(evidenceIntelligenceInputResolver, evidenceIntelligenceReasoningCoordinator, evidenceIntelligenceOcrCoordinator)

        // Document Ingestion — Tier B Durable OCR Derivative Content
        // (DOCUMENT_INGESTION_TIER_B_DURABLE_OCR_DERIVATIVE_CONTENT_SCOPE_LOCK.md,
        // TIER_B_OCR_DURABLE_REPRESENTATION_BOUNDS_DECISION.md). Reuses the exact same
        // derivativeGenerationStorage/documentIngestionAudit/derivativeContentStorage instances
        // Tier A's own wiring above already constructed -- one unified generation-identity space
        // (scope lock §5), never a parallel store. csvExtractor is DerivativeGenerationCoordinator's
        // own required (non-nullable) constructor parameter -- a fresh ApacheCommonsCsvExtractor is
        // supplied purely to satisfy it; this Tier B instance never calls ingestCsv/ingestEml/
        // ingestDocx/ingestPdf, only the new ingestOcr.
        val tierBDerivativeGenerationCoordinator = DerivativeGenerationCoordinator(
            csvExtractor = ApacheCommonsCsvExtractor(),
            storage = derivativeGenerationStorage,
            audit = documentIngestionAudit,
            contentStorage = derivativeContentStorage,
        )
        // One transport implementation is shared by the ordinary (currently disabled while the
        // profile is ACCEPTANCE_PENDING) and region-bound adapters. Construction performs no I/O.
        val openAiTransport = JdkOpenAiResponsesTransport()
        val externalTranscriptionMechanism: ExternalTranscriptionMechanism =
            when (openAiExternalTranscriptionBackendReadiness) {
                OpenAiExternalTranscriptionBackendReadiness.Ready ->
                    OpenAiResponsesExternalTranscriptionAdapter(
                        readiness = openAiExternalTranscriptionReadiness as OpenAiExternalTranscriptionReadiness.Ready,
                        credential = requireNotNull(config.openAiApiCredential),
                        transport = openAiTransport,
                    )
                OpenAiExternalTranscriptionBackendReadiness.Disabled,
                OpenAiExternalTranscriptionBackendReadiness.MissingCredential,
                is OpenAiExternalTranscriptionBackendReadiness.ConfigurationNotAccepted,
                is OpenAiExternalTranscriptionBackendReadiness.ProfileNotReady,
                -> DisabledExternalTranscriptionMechanism
            }
        // Unit J durable admission remains downstream of the explicitly owner-invoked mechanism;
        // constructing either mechanism performs no provider request.
        externalTranscriptionOwnerInvocationCoordinator = ExternalTranscriptionOwnerInvocationCoordinator(
            ownerPrincipalId = PrincipalId(config.ownerPrincipalId),
            permissionEngine = permissionEngine,
            evidenceCustodian = defaultEvidenceCustodian,
            externalMechanism = externalTranscriptionMechanism,
            validator = OcrStructuredResultValidator(),
            durableAdmission = tierBDerivativeGenerationCoordinator,
        )
        // UI-INGESTION-5: optional/additive -- absent unless the new storage root is configured,
        // in which case it also requires the already-mandatory high-authority verification
        // credential/principal this deployment already supplies for human correction.
        val externalTranscriptionAuthorizationRoot = config.externalTranscriptionAuthorizationStorageRootPath
        externalTranscriptionAuthorizationCoordinator = if (
            externalTranscriptionAuthorizationRoot != null &&
            config.ownerHighAuthorityVerificationCredentialFilePath != null &&
            config.ownerHighAuthorityPrincipalId != null
        ) {
            parker.core.runtime.ExternalTranscriptionOwnerAuthorizationCoordinator(
                ownerPrincipalId = PrincipalId(config.ownerPrincipalId),
                evidenceCustodian = defaultEvidenceCustodian,
                purposes = authorizationPurposeRegistry,
                permissions = permissionEngine,
                ownerVerification = ExternalFileOwnerHighAuthorityVerification.load(
                    Path.of(config.ownerHighAuthorityVerificationCredentialFilePath),
                    allowedPurposes = setOf(ExternalTranscriptionInvocationGate.AUTHORIZATION_PURPOSE),
                ),
                store = parker.core.runtime.FileSystemExternalTranscriptionAuthorizationStore(Path.of(externalTranscriptionAuthorizationRoot)),
            )
        } else null
        // A separately persisted authority is the only bridge from ACCEPTANCE_PENDING to the raw
        // provider adapter. The ordinary coordinator above remains bound to the disabled mechanism
        // until global ACCEPTED. Merely configuring these roots grants no execution authority.
        val authorityRoot = config.fidelityFirstAcceptanceAuthorityStorageRootPath
        val attemptRoot = config.fidelityFirstAttemptStorageRootPath
        val buildCommit = config.productionCommit
        val readyProfile = openAiExternalTranscriptionReadiness as? OpenAiExternalTranscriptionReadiness.Ready
        val credential = config.openAiApiCredential
        val embeddedCommit = buildIdentity()
        val acceptanceAttemptLedger = attemptRoot?.let { FileSystemFidelityFirstAttemptLedger(Path.of(it)) }
        val readinessDiagnostic = RuntimeReadinessDiagnostic.fromEvaluated(
            config, embeddedCommit, openAiExternalTranscriptionReadiness,
        )
        fidelityFirstAcceptanceCoordinator = if (
            authorityRoot != null && attemptRoot != null && buildCommit != null && buildCommit == embeddedCommit &&
            readyProfile != null && credential != null && acceptanceAttemptLedger != null
        ) {
            val profile = readyProfile.profile
            FidelityFirstAcceptanceCoordinator(
                authorities = FileSystemFidelityFirstAcceptanceAuthorityStorage(Path.of(authorityRoot)),
                ledger = acceptanceAttemptLedger,
                lifecycle = {
                    when (profile.acceptanceState) {
                        ExternalTranscriptionAcceptanceState.ACCEPTANCE_PENDING -> FidelityFirstAcceptanceLifecycle.ACCEPTANCE_PENDING
                        ExternalTranscriptionAcceptanceState.ACCEPTED -> FidelityFirstAcceptanceLifecycle.ACCEPTED
                        ExternalTranscriptionAcceptanceState.DISABLED -> FidelityFirstAcceptanceLifecycle.DISABLED
                        ExternalTranscriptionAcceptanceState.SUSPENDED -> FidelityFirstAcceptanceLifecycle.SUSPENDED
                        ExternalTranscriptionAcceptanceState.CONFIGURATION_READY -> FidelityFirstAcceptanceLifecycle.INVALID
                    }
                },
                effectiveConfiguration = {
                    FidelityFirstEffectiveConfiguration(
                        ProductionAcquisitionCapabilityCatalogue.FIDELITY_FIRST_EXTERNAL_CAPABILITY_ID,
                        "openai-responses-adapter", "2.0.0", profile.modelSelectionRule,
                        profile.transcriptionProfileId, profile.processingProfileIdentity, profile.reasoningEffort,
                        profile.store, profile.pdfDetail, requireNotNull(profile.instructionSha256),
                        requireNotNull(profile.structuredSchemaSha256),
                    )
                },
                deployedCommit = { requireNotNull(embeddedCommit) },
                ownerPrincipalId = PrincipalId(config.ownerPrincipalId),
                evidenceCustodian = defaultEvidenceCustodian,
                permissionEngine = permissionEngine,
                mechanismFactory = { observer ->
                    OpenAiResponsesExternalTranscriptionAdapter(
                        readiness = requireNotNull(readyProfile), credential = requireNotNull(credential),
                        transport = JdkOpenAiResponsesTransport(), transportLifecycleObserver = observer,
                    )
                },
                validator = OcrStructuredResultValidator(),
                durableAdmission = tierBDerivativeGenerationCoordinator,
            )
        } else null
        var composedRegionProviderStateStore: FileSystemRegionProviderStateStore? = null
        governedRegionTranscriptionExecutionCoordinator = config.regionProviderStateStorageRootPath?.let { configuredRoot ->
            stage("Governed region transcription execution composition") {
                if (
                    !readinessDiagnostic.overallReady || acceptanceAttemptLedger == null
                ) {
                    throw ParkerRuntimeException.InvalidConfiguration(
                        ParkerRuntimeConfigLoader.KEY_REGION_PROVIDER_STATE_STORAGE_ROOT,
                        "region execution requires complete acceptance storage, matching build identity, ready provider profile, and credential",
                    )
                }
                val providerStateStore = FileSystemRegionProviderStateStore(Path.of(configuredRoot))
                composedRegionProviderStateStore = providerStateStore
                GovernedRegionTranscriptionExecutionCoordinator(
                    ledger = acceptanceAttemptLedger,
                    providerStateStore = providerStateStore,
                    mechanism = OpenAiRegionTranscriptionAdapter(
                        credential = requireNotNull(credential),
                        transport = openAiTransport,
                        providerStateStore = providerStateStore,
                    ),
                )
            }
        }
        regionAcceptanceExecutionCoordinator = config.regionAcceptanceAuthorityStorageRootPath?.let { configuredRoot ->
            stage("Region-specific acceptance authority composition") {
                val sourceCommit = requireNotNull(config.sourceCommit)
                val imageId = requireNotNull(config.deployedImmutableImageId)
                val runtimeCommit = requireNotNull(embeddedCommit)
                val executionCoordinator = requireNotNull(governedRegionTranscriptionExecutionCoordinator)
                val deployment = parker.core.runtime.RegionAcceptanceDeploymentFacts(
                    sourceCommit, requireNotNull(buildCommit), runtimeCommit, imageId,
                    parker.core.runtime.OpenAiRegionTranscriptionAdapter.ENDPOINT.toString(),
                )
                val authorities = parker.core.runtime.FileSystemRegionAcceptanceAuthorityStorageV2(Path.of(configuredRoot))
                val reconstructor = parker.core.runtime.CustodyRegionAcceptanceReconstructor(
                    defaultEvidenceCustodian, PrincipalId(config.ownerPrincipalId), deployment,
                    parker.core.runtime.RegionAcceptanceContextPolicy.REGION_ONLY,
                )
                val providerSurface = { parker.core.runtime.RegionAcceptanceProviderSurface() }
                regionAcceptanceAuthorityCreationCoordinator = parker.core.runtime.RegionTranscriptionAcceptanceAuthorityCreationCoordinator(
                    authorities = authorities,
                    reconstructor = parker.core.runtime.RegionAcceptanceCurrentFactsReconstructor(reconstructor::reconstructCurrent),
                    provider = providerSurface,
                    attemptExists = requireNotNull(acceptanceAttemptLedger)::exists,
                    providerStateExists = requireNotNull(composedRegionProviderStateStore)::responseExistsFor,
                )
                parker.core.runtime.RegionAcceptanceExecutionCoordinatorV2(
                    authorities = authorities,
                    lifecycle = {
                        when (readyProfile!!.profile.acceptanceState) {
                            ExternalTranscriptionAcceptanceState.ACCEPTANCE_PENDING -> FidelityFirstAcceptanceLifecycle.ACCEPTANCE_PENDING
                            ExternalTranscriptionAcceptanceState.ACCEPTED -> FidelityFirstAcceptanceLifecycle.ACCEPTED
                            ExternalTranscriptionAcceptanceState.DISABLED -> FidelityFirstAcceptanceLifecycle.DISABLED
                            ExternalTranscriptionAcceptanceState.SUSPENDED -> FidelityFirstAcceptanceLifecycle.SUSPENDED
                            ExternalTranscriptionAcceptanceState.CONFIGURATION_READY -> FidelityFirstAcceptanceLifecycle.INVALID
                        }
                    },
                    reconstructor = parker.core.runtime.RegionAcceptanceCurrentFactsReconstructor(reconstructor::reconstructCurrent),
                    provider = providerSurface,
                    execution = parker.core.runtime.GovernedRegionExecutionPort { binding -> executionCoordinator.execute(binding) },
                )
            }
        }
        ordinaryRegionIngestionWorkflow = if (config.ordinaryRegionIngestionEnabled) {
            stage("Ordinary external request-region-v8 ingestion composition") {
                val acceptanceStore = parker.core.runtime.FileSystemOrdinaryRequestRegionV8CapabilityAcceptanceStore(
                    Path.of(requireNotNull(config.ordinaryRegionCapabilityAcceptanceStorageRootPath)))
                legacyOrdinaryRegionAcceptanceEvaluator = parker.core.runtime.OrdinaryRegionCapabilityAcceptanceEvaluator(
                    parker.core.runtime.FileSystemOrdinaryRegionCapabilityAcceptanceStore(
                        Path.of(requireNotNull(config.ordinaryRegionCapabilityAcceptanceStorageRootPath))),
                    parker.core.runtime.OrdinaryRegionCapabilityIdentity()
                ) { buildIdentity() }
                val authorizationRoot = Path.of(requireNotNull(config.ordinaryRegionOwnerAuthorizationStorageRootPath))
                val authorizationStore = parker.core.runtime.FileSystemOrdinaryRequestRegionV8AuthorizationStore(authorizationRoot)
                val ledger = requireNotNull(acceptanceAttemptLedger)
                val providerStateRoot = requireNotNull(config.regionProviderStateStorageRootPath)
                val providerStateStore = parker.core.runtime.FileSystemRequestRegionV8ProviderStateStore(Path.of(providerStateRoot))
                val embedded = requireNotNull(embeddedCommit)
                val correctedPreparationStore = parker.core.runtime.FileSystemFullPageAchromaticPreparationStore(
                    Path.of(requireNotNull(config.correctedPreparationStorageRootPath)))
                correctedPreparationService = parker.core.runtime.GovernedCorrectedPreparationService(
                    defaultEvidenceCustodian, PrincipalId(config.ownerPrincipalId), correctedPreparationStore)
                val v8Promotion = parker.core.runtime.OrdinaryRequestRegionV8CapabilityAcceptanceCoordinator(
                    acceptanceStore,{buildIdentity()},config.ownerPrincipalId,clock)
                val legacyPromotion = parker.core.runtime.OrdinaryRegionCapabilityAcceptanceCoordinator(
                    parker.core.runtime.FileSystemOrdinaryRegionCapabilityAcceptanceStore(
                        Path.of(requireNotNull(config.ordinaryRegionCapabilityAcceptanceStorageRootPath))),
                    parker.core.runtime.DurableOrdinaryRegionR69EvidenceLoader(
                        parker.core.runtime.FileSystemRegionProviderStateStore(Path.of(providerStateRoot)),
                        parker.core.runtime.FileSystemRegionAcceptanceAuthorityStorageV2(
                            Path.of(requireNotNull(config.regionAcceptanceAuthorityStorageRootPath))),
                        ledger), { buildIdentity() }, config.ownerPrincipalId, clock)
                ordinaryRegionCapabilityAcceptanceCoordinator = object : parker.core.runtime.OrdinaryRegionCapabilityPromotionPort {
                    override fun create(request: parker.core.runtime.OrdinaryRegionCapabilityPromotionRequest) =
                        if (request.capabilityId == parker.core.runtime.ORDINARY_REQUEST_REGION_V8_CAPABILITY_ID) v8Promotion.create(request)
                        else legacyPromotion.create(request)
                }
                val exchange = parker.core.runtime.OpenAiRequestRegionV8ProviderExchange(
                    credential = requireNotNull(credential), transport = openAiTransport, state = providerStateStore)
                parker.core.runtime.OrdinaryRequestRegionV8IngestionWorkflow(
                    owner = PrincipalId(config.ownerPrincipalId), evidenceCustodian = defaultEvidenceCustodian,
                    acceptance = parker.core.runtime.OrdinaryRequestRegionV8AcceptanceEvaluator(acceptanceStore) { buildIdentity() },
                    authorizations = authorizationStore, guard = OrdinaryRegionAuthorizationGuard(authorizationRoot), ledger = ledger,
                    preparer = parker.core.runtime.OrdinaryRequestRegionV8RequestPreparer(correctedPreparationStore),
                    execution = parker.core.runtime.GovernedRequestRegionV8ExecutionCoordinator(ledger,providerStateStore,exchange),
                    admission = OrdinaryRegionDerivativeAdmission(derivativeGenerationStorage, derivativeContentStorage, documentIngestionAudit),
                    runtimeCommit = { embedded },
                )
            }
        } else null
        tierBOcrOwnerInvocationCoordinator = TierBOcrOwnerInvocationCoordinator(
            defaultEvidenceCustodian, permissionEngine, evidenceIntelligenceOcrCoordinator, tierBDerivativeGenerationCoordinator,
        )
        val acquisitionRegistry = ProductionAcquisitionCapabilityCatalogue.create(
            ordinaryRegionCapabilityProjection = ordinaryRegionIngestionWorkflow?.let {
                ProductionAcquisitionCapabilityCatalogue.ordinaryRequestRegionV8Capability(
                    it.capabilityStatus().disposition == parker.core.runtime.OrdinaryRegionCapabilityDisposition.ACCEPTED,
                )
            },
        )
        val acquisitionRouter = DeterministicEvidenceAcquisitionRouter()
        governedAcquisitionOwnerWorkflow = GovernedAcquisitionOwnerWorkflow(
            ownerPrincipalId = PrincipalId(config.ownerPrincipalId),
            evidenceCustodian = defaultEvidenceCustodian,
            registry = acquisitionRegistry,
            router = acquisitionRouter,
            executionCoordinator = GovernedAcquisitionExecutionCoordinator(
                acquisitionRegistry, acquisitionRouter, defaultEvidenceCustodian,
                listOf(
                    TierANativeAcquisitionExecutor(
                        AcquisitionExecutorBinding(
                            ProductionAcquisitionCapabilityCatalogue.NATIVE_CAPABILITY_ID,
                            EvidenceAcquisitionMechanism.DIRECT_NATIVE_EXTRACTION,
                            null,
                        ),
                        tierAOwnerInvocationCoordinator,
                    ),
                    LocalOcrAcquisitionExecutor(
                        AcquisitionExecutorBinding(
                            ProductionAcquisitionCapabilityCatalogue.LOCAL_OCR_CAPABILITY_ID,
                            EvidenceAcquisitionMechanism.LOCAL_OCR,
                            null,
                        ),
                        tierBOcrOwnerInvocationCoordinator,
                    ),
                ),
            ),
            externalEgressAuthorised = { id -> externalTranscriptionAuthorizationCoordinator?.isAuthorized(id) == true },
        )
        tierBOcrContentRetrievalCoordinator = TierBOcrContentRetrievalCoordinator(derivativeGenerationStorage, derivativeContentStorage)

        // Construct the parent saved-analysis store before its separately governed review
        // sub-store; both validate their roots during construction.
        pendingAnalysisCache = PendingAnalysisCache()
        val savedAnalysisStorage = stage("Saved analysis storage construction") {
            FileSystemSavedAnalysisStorage(Path.of(config.savedAnalysisStorageRootPath))
        }

        humanVerificationStorage = stage("Human verification storage construction") {
            FileSystemHumanVerificationStorage(Path.of(config.savedAnalysisStorageRootPath).resolve("human-verification"))
        }

        // Minimum Production Document Pipeline — Local Reasoning Implementation. Reuses
        // permissionEngine (the same, already-registered EvidenceIntelligenceInvocationGate
        // (EXECUTE, DOCUMENT) proposal class tierBOcrOwnerInvocationCoordinator, above, already
        // evaluates), tierAContentRetrievalCoordinator/tierBOcrContentRetrievalCoordinator
        // (already wired above), and modelInferenceClient (hoisted above, the same instance
        // reasoningProvider's own ModelReasoningProvider already uses) -- no new dependency of
        // any kind is constructed for this coordinator alone.
        documentAnalysisCoordinator = DocumentAnalysisCoordinator(
            permissionEngine = permissionEngine,
            tierAContentRetrievalCoordinator = tierAContentRetrievalCoordinator,
            tierBOcrContentRetrievalCoordinator = tierBOcrContentRetrievalCoordinator,
            modelInferenceClient = modelInferenceClient,
            promptBuilder = DefaultDocumentAnalysisPromptBuilder(),
            modelTimeoutMs = config.modelTimeoutMs,
            sourceManifestStorage = evidenceSourceManifestStorage,
            humanVerificationStorage = humanVerificationStorage,
        )

        // Reviewed Analysis Result — Explicit Owner Save. pendingAnalysisCache is the entire
        // anti-forgery mechanism (see its own KDoc): a small, bounded, TTL-expiring, in-memory-only
        // map, never durable on its own. savedAnalysisStorage is a wholly separate storage root
        // from every other store this class already constructs -- never nested inside, never
        // sharing an identifier namespace with, Evidence/Derivative Generation/Derivative Content/
        // Memory/Knowledge.
        savedAnalysisCoordinator = SavedAnalysisCoordinator(
            pendingAnalysisCache = pendingAnalysisCache,
            storage = savedAnalysisStorage,
        )

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
                    verbPhrase = ExternalTranscriptionInvocationGate.ACTION_NAME,
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
            // Knowledge Discoverability and Governed Retrieval into Reasoning Context, Implementation
            // Unit 3 (Contract Design Section 7). No new Resource -- this new verb reuses the same
            // Knowledge Retrieval resource identity registered immediately above, since both verbs
            // govern the same conceptual Knowledge Retrieval boundary.
            vocabulary.register(
                ActionVocabularyEntry(
                    verbPhrase = DefaultReasoningKnowledgeSource.RETRIEVE_FOR_REASONING_CONTEXT_ACTION_NAME,
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

    /** Read-only, manifest-only governed acquisition evaluation. It performs no extraction, OCR, or provider call. */
    suspend fun evaluateGovernedAcquisitionAsOwner(evidenceArtifactId: EvidenceArtifactId): GovernedAcquisitionOwnerEvaluation {
        if (state != RuntimeLifecycleState.RUNNING) throw ParkerRuntimeException.NotRunning(state)
        return governedAcquisitionOwnerWorkflow.evaluate(evidenceArtifactId)
    }

    /** Explicit exact-decision acquisition. The expected capability is revalidated before execution. */
    suspend fun executeGovernedAcquisitionAsOwner(
        evidenceArtifactId: EvidenceArtifactId,
        expectedCapabilityId: String,
    ): GovernedAcquisitionOwnerExecution {
        if (state != RuntimeLifecycleState.RUNNING) throw ParkerRuntimeException.NotRunning(state)
        return governedAcquisitionOwnerWorkflow.execute(evidenceArtifactId, expectedCapabilityId)
    }

    /**
     * Document Ingestion, Owner-Facing Tier A Runtime Invocation Boundary. The one production
     * entry point through which Tier A Document Ingestion may be invoked -- **explicit,
     * individually-authorized owner invocation only**, mirroring [deleteEvidenceAsOwner]'s own
     * structural owner-only pattern exactly: this method takes **no** `requestingPrincipalId`
     * parameter, always acts as `PrincipalId(config.ownerPrincipalId)`, and there is no code
     * path through which any caller, internal or external, could substitute a different
     * principal (`docs/architecture/DOCUMENT_INGESTION_AUTHORITATIVE_SOURCE_MANIFEST_RETRIEVAL_SCOPE_LOCK.md`
     * ("the Scope Lock") Section 17).
     *
     * Accepts only [evidenceArtifactId] -- an identity for an already-custodied source. It
     * never accepts source bytes, an expected digest, a media type, a filename, a path, a URL,
     * an uploaded file, a Gmail message, a parser choice, or an OCR choice; every one of those
     * facts is resolved exclusively from Evidence Custodian's own authoritative manifest and
     * bytes (Scope Lock Section 21). [tierAOwnerInvocationCoordinator] performs the entire
     * governed chain -- manifest retrieval, byte retrieval, byte-length and SHA-256
     * verification, and exactly one call to the existing, unchanged Tier A router -- this
     * method adds no orchestration of its own beyond the [RuntimeLifecycleState.RUNNING] guard
     * every production entry point on this class already requires and minting [correlationValue]
     * once, here, mirroring [submitEvidence]'s own identical correlation-id convention.
     *
     * This method never invokes Tier B, OCR, Memory Core registration, Knowledge promotion, or
     * Evidence Intelligence analysis -- the Tier A router it calls does not perform any of
     * those either (`DOCUMENT_INGESTION_TIER_A_IMPLEMENTATION_CLOSURE.md` §11, §14).
     *
     * Throws [ParkerRuntimeException.NotRunning] if [state] is not [RuntimeLifecycleState.RUNNING].
     */
    suspend fun invokeTierAIngestionAsOwner(evidenceArtifactId: EvidenceArtifactId): TierAOwnerInvocationOutcome {
        if (state != RuntimeLifecycleState.RUNNING) {
            throw ParkerRuntimeException.NotRunning(state)
        }
        logger.info("Tier A document ingestion invoked by owner (evidenceArtifactId=${evidenceArtifactId.value})")
        return tierAOwnerInvocationCoordinator.invoke(
            PrincipalId(config.ownerPrincipalId),
            evidenceArtifactId,
            UUID.randomUUID().toString(),
        )
    }

    /** Fresh durable owner evidence listing; validates every manifest against governed custody bytes. */
    suspend fun listRegisteredEvidenceAsOwner(): List<parker.core.runtime.OwnerRegisteredEvidence> {
        if (state != RuntimeLifecycleState.RUNNING) throw ParkerRuntimeException.NotRunning(state)
        return ownerEvidenceListing.listRegistered()
    }

    /**
     * Document Ingestion — Derivative Content Persistence and Retrieval. The one production entry
     * point through which an already-persisted Tier A derivative's durable content may be
     * retrieved by known identity -- **explicit, individually-authorized owner invocation only**,
     * mirroring [invokeTierAIngestionAsOwner]'s own structural owner-only pattern exactly: no
     * `requestingPrincipalId` parameter, always acts as `PrincipalId(config.ownerPrincipalId)`.
     *
     * Accepts only [evidenceArtifactId] and [derivativeGenerationId] -- both already-known Parker
     * identities the caller must already possess (from a prior Upload/Process response), never a
     * filesystem path. Performs no re-extraction: [tierAContentRetrievalCoordinator] resolves
     * durable storage only (`DOCUMENT_INGESTION_DERIVATIVE_CONTENT_PERSISTENCE_RETRIEVAL_SCOPE_LOCK.md`
     * §11/§12). No general enumeration/browse capability exists here or anywhere else in this
     * class (Scope Lock §11).
     *
     * Throws [ParkerRuntimeException.NotRunning] if [state] is not [RuntimeLifecycleState.RUNNING].
     */
    suspend fun retrieveTierAExtractedContentAsOwner(
        evidenceArtifactId: EvidenceArtifactId,
        derivativeGenerationId: DerivativeGenerationId,
    ): TierAContentRetrievalOutcome {
        if (state != RuntimeLifecycleState.RUNNING) {
            throw ParkerRuntimeException.NotRunning(state)
        }
        logger.info(
            "Tier A derivative content retrieval invoked by owner " +
                "(evidenceArtifactId=${evidenceArtifactId.value}, derivativeGenerationId=${derivativeGenerationId.value})",
        )
        return tierAContentRetrievalCoordinator.retrieve(evidenceArtifactId, derivativeGenerationId)
    }

    /**
     * Document Ingestion — Tier B Durable OCR Derivative Content. The one production entry point
     * through which durable Tier B OCR may be invoked -- **explicit, individually-authorized owner
     * invocation only**, mirroring [invokeTierAIngestionAsOwner]'s own structural owner-only
     * pattern exactly: no `requestingPrincipalId` parameter, always acts as
     * `PrincipalId(config.ownerPrincipalId)`.
     *
     * Unlike [invokeTierAIngestionAsOwner], this operation is **also** gated by a real Permission
     * Engine evaluation before any OCR work begins (Tier B scope lock §9) -- structural owner-only
     * and Permission-Engine-authorised are two distinct, both-required guarantees, neither a
     * substitute for the other. [tierBOcrOwnerInvocationCoordinator] performs the entire governed
     * chain: authorisation, source retrieval, OCR invocation (via the existing, unmodified
     * [EvidenceIntelligenceOcrCoordinator]/[parker.core.interfaces.OcrMechanism] path), the
     * mandatory-provenance fail-closed gate (§11), `DerivativeGenerationId` minting, and the full
     * prepare/publish/audit ordering (§19). Never invoked automatically -- not by upload, not by
     * Tier A completion, not by retrieval, not by restart (§26).
     *
     * Throws [ParkerRuntimeException.NotRunning] if [state] is not [RuntimeLifecycleState.RUNNING].
     */
    suspend fun invokeTierBOcrDurableGenerationAsOwner(evidenceArtifactId: EvidenceArtifactId): TierBOcrOwnerInvocationOutcome {
        if (state != RuntimeLifecycleState.RUNNING) {
            throw ParkerRuntimeException.NotRunning(state)
        }
        logger.info("Tier B durable OCR generation invoked by owner (evidenceArtifactId=${evidenceArtifactId.value})")
        return tierBOcrOwnerInvocationCoordinator.invoke(
            PrincipalId(config.ownerPrincipalId),
            evidenceArtifactId,
            UUID.randomUUID().toString(),
        )
    }

    /**
     * Explicit owner-only external transcription backend boundary. No caller principal, provider
     * selection, fallback, durable publication, analysis, or UI exposure exists in this unit.
     *
     * UI-INGESTION-5: when [externalTranscriptionAuthorizationCoordinator] is configured, this
     * exact evidence target must already carry a durable owner authorization
     * ([parker.core.runtime.ExternalTranscriptionOwnerAuthorizationCoordinator.isAuthorized])
     * before [ExternalTranscriptionOwnerInvocationCoordinator.invoke] -- and therefore the
     * provider -- is ever reached. [ExternalTranscriptionOwnerInvocationCoordinator] itself is
     * unchanged; this check runs only at this one call site. When the coordinator is not
     * configured, behavior is unchanged from before this unit.
     */
    suspend fun invokeExternalTranscriptionAsOwner(
        evidenceArtifactId: EvidenceArtifactId,
    ): ExternalTranscriptionOwnerInvocationOutcome {
        if (state != RuntimeLifecycleState.RUNNING) throw ParkerRuntimeException.NotRunning(state)
        val authorization = externalTranscriptionAuthorizationCoordinator
        if (authorization != null && !authorization.isAuthorized(evidenceArtifactId)) {
            return ExternalTranscriptionOwnerInvocationOutcome.NotAuthorised
        }
        return externalTranscriptionOwnerInvocationCoordinator.invoke(evidenceArtifactId)
    }

    /** Read-only, exact-target owner authorization status. Never invokes a provider. */
    suspend fun externalTranscriptionAuthorizationStatusAsOwner(
        evidenceArtifactId: EvidenceArtifactId,
    ): parker.core.runtime.ExternalTranscriptionAuthorizationView {
        if (state != RuntimeLifecycleState.RUNNING) throw ParkerRuntimeException.NotRunning(state)
        return externalTranscriptionAuthorizationCoordinator?.status(evidenceArtifactId)
            ?: parker.core.runtime.ExternalTranscriptionAuthorizationView(
                parker.core.runtime.ExternalTranscriptionAuthorizationDisposition.UNAVAILABLE, evidenceArtifactId.value,
                detail = "AUTHORIZATION_LANE_NOT_CONFIGURED",
            )
    }

    /**
     * Explicit owner confirmation, gated by the existing opaque owner high-authority verification
     * boundary. Creates the durable per-target grant only; never invokes a provider. [credential]
     * is the raw presented verification secret only -- never the owner's legal name, never logged.
     */
    suspend fun authorizeExternalTranscriptionAsOwner(
        evidenceArtifactId: EvidenceArtifactId,
        credential: String?,
    ): parker.core.runtime.ExternalTranscriptionAuthorizationView {
        if (state != RuntimeLifecycleState.RUNNING) throw ParkerRuntimeException.NotRunning(state)
        val coordinator = externalTranscriptionAuthorizationCoordinator
            ?: return parker.core.runtime.ExternalTranscriptionAuthorizationView(
                parker.core.runtime.ExternalTranscriptionAuthorizationDisposition.UNAVAILABLE, evidenceArtifactId.value,
                detail = "AUTHORIZATION_LANE_NOT_CONFIGURED",
            )
        return coordinator.authorize(evidenceArtifactId, OwnerVerificationCredential.presented(credential))
    }

    /** Exact-authority administrative acceptance command; never accepts source or configuration overrides. */
    suspend fun invokeFidelityFirstAcceptanceAsOwner(authorityId: String): FidelityFirstAcceptanceOutcome {
        if (state != RuntimeLifecycleState.RUNNING) throw ParkerRuntimeException.NotRunning(state)
        if (!Regex("^[A-Za-z0-9_.-]{1,120}$").matches(authorityId)) {
            return FidelityFirstAcceptanceOutcome.Blocked("AUTHORITY_ID_INVALID")
        }
        return fidelityFirstAcceptanceCoordinator?.invoke(authorityId)
            ?: FidelityFirstAcceptanceOutcome.Blocked("ACCEPTANCE_LANE_NOT_CONFIGURED")
    }

    /** Region-only, exact-authority acceptance command. No source/request/provider override exists. */
    suspend fun invokeRegionTranscriptionAcceptanceAsOwner(authorityId: String): parker.core.runtime.RegionAcceptanceExecutionOutcomeV2 {
        if (state != RuntimeLifecycleState.RUNNING) throw ParkerRuntimeException.NotRunning(state)
        if (!Regex("^[A-Za-z0-9_.-]{1,120}$").matches(authorityId)) {
            return parker.core.runtime.RegionAcceptanceExecutionOutcomeV2.Blocked("AUTHORITY_ID_INVALID")
        }
        return regionAcceptanceExecutionCoordinator?.invoke(authorityId)
            ?: parker.core.runtime.RegionAcceptanceExecutionOutcomeV2.Blocked("REGION_ACCEPTANCE_LANE_NOT_CONFIGURED")
    }

    /** Explicit acceptance-administration boundary; reconstructs all governed facts internally and never executes. */
    suspend fun createRegionTranscriptionAcceptanceAuthorityAsOwner(
        request: parker.core.runtime.RegionAcceptanceAuthorityCreationRequest,
    ): parker.core.runtime.RegionAcceptanceAuthorityCreationOutcome {
        if (state != RuntimeLifecycleState.RUNNING) throw ParkerRuntimeException.NotRunning(state)
        return regionAcceptanceAuthorityCreationCoordinator?.create(request)
            ?: parker.core.runtime.RegionAcceptanceAuthorityCreationOutcome.Blocked("REGION_ACCEPTANCE_CREATION_LANE_NOT_CONFIGURED")
    }

    /** Governed preparation-only command. It persists/readbacks preparation and never creates execution state. */
    suspend fun prepareCorrectedEvidenceAsOwner(
        evidenceArtifactId: EvidenceArtifactId,
        profileId: String,
        profileVersion: Int,
    ): parker.core.runtime.GovernedCorrectedPreparationOutcome {
        if (state != RuntimeLifecycleState.RUNNING) throw ParkerRuntimeException.NotRunning(state)
        return correctedPreparationService?.prepare(evidenceArtifactId, profileId, profileVersion)
            ?: parker.core.runtime.GovernedCorrectedPreparationOutcome.Rejected("PREPARATION_LANE_NOT_CONFIGURED")
    }

    /** Non-executing ordinary request-region-v8 proposal. It never reserves or creates attempt state. */
    suspend fun proposeOrdinaryRegionIngestionAsOwner(evidenceArtifactId: EvidenceArtifactId): parker.core.runtime.OrdinaryRegionProposal? {
        if (state != RuntimeLifecycleState.RUNNING) throw ParkerRuntimeException.NotRunning(state)
        return ordinaryRegionIngestionWorkflow?.proposal(evidenceArtifactId)
    }

    /** Fresh, read-only exact request-region-v8 acceptance and routing identity evaluation. */
    fun ordinaryRegionCapabilityStatusAsOwner(): parker.core.runtime.OrdinaryRegionCapabilityStatus {
        if (state != RuntimeLifecycleState.RUNNING) throw ParkerRuntimeException.NotRunning(state)
        // Preserve truthful readback of legacy V5 acceptance records without using them to authorize V8.
        if (legacyOrdinaryRegionAcceptanceEvaluator?.evaluate() is parker.core.runtime.OrdinaryRegionCapabilityAcceptanceEvaluation.Accepted) {
            val legacy = parker.core.runtime.OrdinaryRegionCapabilityIdentity()
            return parker.core.runtime.OrdinaryRegionCapabilityStatus(
                legacy.capabilityId, legacy.provider, legacy.endpointOperation, legacy.model,
                legacy.adapterId, legacy.adapterVersion, legacy.providerProfile, legacy.wireVersion,
                "application/pdf", legacy.maximumRegions, legacy.aggregateRequestBodyMaximumBytes, false,
                parker.core.runtime.OrdinaryRegionCapabilityDisposition.ACCEPTED, buildIdentity(), null,
            )
        }
        return ordinaryRegionIngestionWorkflow?.capabilityStatus() ?: parker.core.runtime.OrdinaryRequestRegionV8CapabilityIdentity().let { capability ->
            parker.core.runtime.OrdinaryRegionCapabilityStatus(
                capability.capabilityId, capability.provider, capability.endpointOperation, capability.model,
                capability.adapterId, capability.adapterVersion, capability.profile, capability.wireVersion,
                "application/pdf", capability.maximumRegions, capability.maximumBodyBytes,
                false, parker.core.runtime.OrdinaryRegionCapabilityDisposition.NOT_CONFIGURED,
                buildIdentity(), null,
            )
        }
    }

    /** Governed capability promotion only; reconstructs fixed evidence and performs no egress or execution. */
    fun createOrdinaryRegionCapabilityAcceptanceAsOwner(
        request: parker.core.runtime.OrdinaryRegionCapabilityPromotionRequest,
    ): parker.core.runtime.OrdinaryRegionCapabilityPromotionOutcome {
        if (state != RuntimeLifecycleState.RUNNING) throw ParkerRuntimeException.NotRunning(state)
        return ordinaryRegionCapabilityAcceptanceCoordinator?.create(request)
            ?: parker.core.runtime.OrdinaryRegionCapabilityPromotionOutcome.Blocked("ACCEPTANCE_LANE_NOT_CONFIGURED")
    }

    /** Explicit owner grant creation only; no reservation, attempt, or provider activity. */
    fun createOrdinaryRegionAuthorizationAsOwner(grant: parker.core.runtime.OrdinaryRegionOwnerAuthorization): Boolean {
        if (state != RuntimeLifecycleState.RUNNING) throw ParkerRuntimeException.NotRunning(state)
        val workflow = ordinaryRegionIngestionWorkflow ?: return false
        workflow.createAuthorization(grant); return true
    }

    /** Fresh evidence-bound authorization projection; no reservation, attempt, or provider activity. */
    suspend fun ordinaryRegionAuthorizationStatusAsOwner(evidenceArtifactId: EvidenceArtifactId): parker.core.runtime.OrdinaryRegionOwnerAuthorizationView {
        if (state != RuntimeLifecycleState.RUNNING) throw ParkerRuntimeException.NotRunning(state)
        return ordinaryRegionIngestionWorkflow?.authorizationStatus(evidenceArtifactId)
            ?: parker.core.runtime.OrdinaryRegionOwnerAuthorizationView(
                parker.core.runtime.OrdinaryRegionOwnerAuthorizationDisposition.UNAVAILABLE,
                evidenceArtifactId.value, detail = "AUTHORIZATION_LANE_NOT_CONFIGURED")
    }

    /** Narrow owner UI command; reconstructs the exact governed grant and never executes. */
    suspend fun authorizeOrdinaryRegionIngestionAsOwner(evidenceArtifactId: EvidenceArtifactId): parker.core.runtime.OrdinaryRegionOwnerAuthorizationOutcome {
        if (state != RuntimeLifecycleState.RUNNING) throw ParkerRuntimeException.NotRunning(state)
        return ordinaryRegionIngestionWorkflow?.authorize(evidenceArtifactId)
            ?: parker.core.runtime.OrdinaryRegionOwnerAuthorizationOutcome.Blocked(
                parker.core.runtime.OrdinaryRegionOwnerAuthorizationView(
                    parker.core.runtime.OrdinaryRegionOwnerAuthorizationDisposition.UNAVAILABLE,
                    evidenceArtifactId.value, detail = "AUTHORIZATION_LANE_NOT_CONFIGURED"))
    }

    /** Explicit non-executing reservation to one governed execution identity. */
    fun reserveOrdinaryRegionAuthorizationAsOwner(authorizationId: String, executionId: String): parker.core.runtime.OrdinaryRegionAuthorizationSnapshot? {
        if (state != RuntimeLifecycleState.RUNNING) throw ParkerRuntimeException.NotRunning(state)
        return ordinaryRegionIngestionWorkflow?.reserve(authorizationId, executionId)
    }

    /** Revocation shares the same authorization guard as durable attempt-start. */
    fun revokeOrdinaryRegionAuthorizationAsOwner(authorizationId: String): parker.core.runtime.OrdinaryRegionAuthorizationSnapshot? {
        if (state != RuntimeLifecycleState.RUNNING) throw ParkerRuntimeException.NotRunning(state)
        return ordinaryRegionIngestionWorkflow?.revoke(authorizationId)
    }

    /** The only ordinary request-region-v8 execution entry point; all provider facts are composed internally. */
    suspend fun executeOrdinaryRegionIngestionAsOwner(evidenceArtifactId: EvidenceArtifactId, authorizationId: String,
        executionId: String, attemptId: String): parker.core.runtime.OrdinaryRegionOwnerResult {
        if (state != RuntimeLifecycleState.RUNNING) throw ParkerRuntimeException.NotRunning(state)
        return ordinaryRegionIngestionWorkflow?.execute(evidenceArtifactId, authorizationId, executionId, attemptId)
            ?: parker.core.runtime.OrdinaryRegionOwnerResult(
                parker.core.runtime.OrdinaryRegionDisposition.CAPABILITY_NOT_ACCEPTED,
                "ordinary request-region-v8 ingestion is not configured",
            )
    }

    /** Provider-free continuation from one exact, already-persisted request-region-v8 response. */
    suspend fun continueOrdinaryRegionPostEgressAsOwner(evidenceArtifactId: EvidenceArtifactId, authorizationId: String,
        executionId: String, providerStateId: String): parker.core.runtime.OrdinaryRegionOwnerResult {
        if (state != RuntimeLifecycleState.RUNNING) throw ParkerRuntimeException.NotRunning(state)
        return ordinaryRegionIngestionWorkflow?.continuePostEgress(evidenceArtifactId, authorizationId, executionId, providerStateId)
            ?: parker.core.runtime.OrdinaryRegionOwnerResult(
                parker.core.runtime.OrdinaryRegionDisposition.VALIDATION_FAILED,
                "post-egress continuation is not configured",
            )
    }

    /** Owner-safe executable readiness, matching the same fail-closed gates used for composition. */
    fun ownerEnhancedTranscriptionReadiness(): parker.ui.EnhancedTranscriptionReadiness =
        when (val readiness = openAiExternalTranscriptionBackendReadiness) {
            OpenAiExternalTranscriptionBackendReadiness.Disabled -> parker.ui.EnhancedTranscriptionReadiness.Disabled
            OpenAiExternalTranscriptionBackendReadiness.MissingCredential -> parker.ui.EnhancedTranscriptionReadiness.MissingCredential
            is OpenAiExternalTranscriptionBackendReadiness.ProfileNotReady ->
                parker.ui.EnhancedTranscriptionReadiness.ProfileNotReady(
                    when (readiness.profileReadiness) {
                        is OpenAiExternalTranscriptionReadiness.StaleProfile -> "The enhanced transcription provider profile requires review."
                        else -> "The enhanced transcription provider profile is not ready."
                    },
                )
            is OpenAiExternalTranscriptionBackendReadiness.ConfigurationNotAccepted ->
                parker.ui.EnhancedTranscriptionReadiness.ProfileNotReady(
                    "The enhanced transcription configuration is ${readiness.state.name.lowercase().replace('_', ' ')}.",
                )
            OpenAiExternalTranscriptionBackendReadiness.Ready -> parker.ui.EnhancedTranscriptionReadiness.Ready
        }

    /**
     * Document Ingestion — Tier B Durable OCR Derivative Content. The one production entry point
     * through which an already-persisted Tier B OCR durable content may be retrieved by known
     * identity -- **explicit, individually-authorized owner invocation only**, mirroring
     * [retrieveTierAExtractedContentAsOwner]'s own structural owner-only pattern exactly: no
     * `requestingPrincipalId` parameter.
     *
     * Performs no re-extraction and holds no path to OCR of any kind (Tier B scope lock §35's own
     * structural non-regeneration guarantee): [tierBOcrContentRetrievalCoordinator] resolves
     * durable storage only, verifying the resolved record's own kind discriminator before ever
     * decoding content as the Tier B representation shape (§28) -- never reachable through, and
     * never affecting, [retrieveTierAExtractedContentAsOwner]/[tierAContentRetrievalCoordinator].
     *
     * Throws [ParkerRuntimeException.NotRunning] if [state] is not [RuntimeLifecycleState.RUNNING].
     */
    suspend fun retrieveTierBOcrContentAsOwner(
        evidenceArtifactId: EvidenceArtifactId,
        derivativeGenerationId: DerivativeGenerationId,
    ): TierBOcrContentRetrievalOutcome {
        if (state != RuntimeLifecycleState.RUNNING) {
            throw ParkerRuntimeException.NotRunning(state)
        }
        logger.info(
            "Tier B durable OCR content retrieval invoked by owner " +
                "(evidenceArtifactId=${evidenceArtifactId.value}, derivativeGenerationId=${derivativeGenerationId.value})",
        )
        return tierBOcrContentRetrievalCoordinator.retrieve(evidenceArtifactId, derivativeGenerationId)
    }

    /** Exact-pair, metadata-only human-review records; ordering conveys no precedence. */
    suspend fun listHumanVerificationRecordsAsOwner(
        evidenceArtifactId: EvidenceArtifactId,
        derivativeGenerationId: DerivativeGenerationId,
    ): List<HumanVerificationRecord> {
        if (state != RuntimeLifecycleState.RUNNING) throw ParkerRuntimeException.NotRunning(state)
        return humanVerificationStorage.listForExactGeneration(evidenceArtifactId, derivativeGenerationId)
    }

    /**
     * Minimum Production Document Pipeline — Local Reasoning Implementation. The one production
     * entry point through which one or more already-admitted evidence derivative generations may
     * be submitted, as a bounded package, to Parker's currently configured LOCAL model-inference
     * seam for a human-reviewable analysis -- **explicit, individually-authorized owner invocation
     * only**, mirroring [invokeTierBOcrDurableGenerationAsOwner]'s own structural owner-only
     * pattern exactly: no `requestingPrincipalId` parameter, always acts as
     * `PrincipalId(config.ownerPrincipalId)`. Also gated by a real Permission Engine evaluation
     * (the same `EvidenceIntelligenceInvocationGate` proposal class
     * [invokeTierBOcrDurableGenerationAsOwner] already evaluates) before any derivative retrieval
     * or model invocation begins.
     *
     * [documentAnalysisCoordinator] performs the entire governed chain: authorisation,
     * per-selection derivative resolution (Tier B OCR first, Tier A fallback -- never
     * re-extraction, never re-running OCR), bounded evidence-package assembly, prompt
     * construction, and exactly one call to the configured LOCAL [ModelInferenceClient]. Creates
     * no durable side effect of any kind: no Evidence write, no derivative write, no Memory/
     * Knowledge/QMD/RKS write, no analysis-result persistence, no new audit store. The returned
     * analysis text is provider-generated material for human review only, never automatically
     * promoted to canonical Parker truth.
     *
     * The log line below records no evidence content, no prompt content, and no model response --
     * only that an invocation occurred, mirroring every other owner entry point's own logging
     * discipline.
     *
     * Throws [ParkerRuntimeException.NotRunning] if [state] is not [RuntimeLifecycleState.RUNNING].
     */
    suspend fun analyseDocumentsAsOwner(request: OwnerDocumentAnalysisRequest): DocumentAnalysisInvocationResult {
        if (state != RuntimeLifecycleState.RUNNING) {
            throw ParkerRuntimeException.NotRunning(state)
        }
        logger.info("Document analysis invoked by owner (selectionCount=${request.selections.size})")
        val outcome = documentAnalysisCoordinator.analyse(PrincipalId(config.ownerPrincipalId), request)
        val pendingAnalysisId = if (outcome is DocumentAnalysisOutcome.Completed) {
            pendingAnalysisCache.register(outcome.result)
        } else {
            null
        }
        return DocumentAnalysisInvocationResult(outcome, pendingAnalysisId)
    }

    /**
     * Reviewed Analysis Result — Explicit Owner Save. The one production entry point through
     * which an already-completed, still-pending analysis (identified only by the opaque
     * [PendingAnalysisId] [analyseDocumentsAsOwner] returned alongside it) may be durably saved --
     * **explicit, individually-authorized owner invocation only**, mirroring
     * [analyseDocumentsAsOwner]'s own structural owner-only pattern exactly: no
     * `requestingPrincipalId` parameter. Carries no Permission Engine gate of its own -- saving an
     * already-produced, already-authorised analysis touches no Evidence, no OCR, and no
     * reasoning-model invocation (see [SavedAnalysisCoordinator]'s own KDoc for the precedent this
     * follows). Never trusts caller-submitted analysis content: [savedAnalysisCoordinator] resolves
     * the exact server-held pending result by id only.
     *
     * The log line below records no analysis content, no instruction, and no evidence reference --
     * only that an invocation occurred.
     *
     * Throws [ParkerRuntimeException.NotRunning] if [state] is not [RuntimeLifecycleState.RUNNING].
     */
    suspend fun saveAnalysisAsOwner(pendingAnalysisId: PendingAnalysisId): SaveAnalysisOutcome {
        if (state != RuntimeLifecycleState.RUNNING) {
            throw ParkerRuntimeException.NotRunning(state)
        }
        logger.info("Analysis save invoked by owner")
        return savedAnalysisCoordinator.save(pendingAnalysisId)
    }

    /**
     * Reviewed Analysis Result — Explicit Owner Save. The one production entry point through
     * which an already-saved analysis may be retrieved by known [SavedAnalysisId] --
     * **explicit, individually-authorized owner invocation only**. Never re-runs analysis, never
     * invokes the model, never re-runs OCR/extraction: [savedAnalysisCoordinator] resolves durable
     * storage only.
     *
     * Throws [ParkerRuntimeException.NotRunning] if [state] is not [RuntimeLifecycleState.RUNNING].
     */
    suspend fun retrieveSavedAnalysisAsOwner(savedAnalysisId: SavedAnalysisId): RetrieveSavedAnalysisOutcome {
        if (state != RuntimeLifecycleState.RUNNING) {
            throw ParkerRuntimeException.NotRunning(state)
        }
        logger.info("Saved analysis retrieval invoked by owner")
        return savedAnalysisCoordinator.retrieve(savedAnalysisId)
    }

    /**
     * Reviewed Analysis Result — Explicit Owner Save. The one production entry point through
     * which a bounded listing of the most recently saved analyses may be retrieved --
     * **explicit, individually-authorized owner invocation only**. Metadata only (see
     * [SavedAnalysisSummary]) -- never the full analysis text or evidence references of any listed
     * entry.
     *
     * Throws [ParkerRuntimeException.NotRunning] if [state] is not [RuntimeLifecycleState.RUNNING].
     */
    suspend fun listSavedAnalysesAsOwner(): List<SavedAnalysisSummary> {
        if (state != RuntimeLifecycleState.RUNNING) {
            throw ParkerRuntimeException.NotRunning(state)
        }
        return savedAnalysisCoordinator.listRecent()
    }

    /**
     * Document Ingestion, Owner-Authorized Local File Ingress. The one production entry point
     * through which Parker may read a local filesystem file on the owner's behalf --
     * **explicit, individually-authorized owner invocation only**, mirroring
     * [deleteEvidenceAsOwner]'s/[invokeTierAIngestionAsOwner]'s own structural owner-only
     * pattern exactly: this method takes **no** `requestingPrincipalId` parameter, always acts
     * as `PrincipalId(config.ownerPrincipalId)`, and there is no code path through which any
     * caller, internal or external, could substitute a different principal
     * (`docs/architecture/DOCUMENT_INGESTION_OWNER_AUTHORIZED_LOCAL_FILE_INGRESS_SCOPE_LOCK.md`
     * ("the Scope Lock") Section 3).
     *
     * Accepts only [absolutePath] -- an absolute local filesystem path the owner designates --
     * and an optional [receivedMediaType] the owner may literally declare (Scope Lock Section
     * 11). It never accepts an `EvidenceArtifactId`, an expected digest, a byte length, a
     * filename override, a parser choice, an OCR choice, a Tier selection, or any Memory/
     * Knowledge field; every one of those facts is either resolved exclusively from the file
     * itself and Evidence Custodian's own admission authority, or not accepted at all (Scope
     * Lock Section 12). [ownerLocalFileIngressCoordinator] performs the entire governed chain --
     * permission evaluation, absolute-path validation, symlink rejection, regular-file
     * validation, the 64 MiB size bound, a bounded byte-exact read, and exactly one call to the
     * existing, unchanged [EvidenceCustodian.accept] -- this method adds no orchestration of
     * its own beyond the [RuntimeLifecycleState.RUNNING] guard every production entry point on
     * this class already requires.
     *
     * This method never invokes Tier A, OCR, Tier B, Memory Core registration, Knowledge
     * promotion, or Evidence Intelligence analysis -- successful Evidence Custodian acceptance
     * ends this operation (Scope Lock Section 14); a subsequent Tier A invocation, if wanted, is
     * a separate, later, independently authorized owner action via [invokeTierAIngestionAsOwner].
     *
     * The log line below records no path -- only that an invocation occurred -- mirroring
     * [invokeTierAIngestionAsOwner]'s own "identifier only, never a path" logging discipline
     * (Scope Lock Section 18/Section 20).
     *
     * Throws [ParkerRuntimeException.NotRunning] if [state] is not [RuntimeLifecycleState.RUNNING].
     */
    suspend fun importEvidenceFileAsOwner(
        absolutePath: String,
        receivedMediaType: String? = null,
    ): OwnerLocalFileIngressOutcome {
        if (state != RuntimeLifecycleState.RUNNING) {
            throw ParkerRuntimeException.NotRunning(state)
        }
        logger.info("Local file evidence ingress invoked by owner")
        return ownerLocalFileIngressCoordinator.invoke(
            PrincipalId(config.ownerPrincipalId),
            absolutePath,
            receivedMediaType,
        )
    }

    /** Browser-upload ingress retaining its safe client basename instead of the staging name. */
    suspend fun importUploadedEvidenceFileAsOwner(
        absolutePath: String,
        receivedMediaType: String?,
        originalFileName: String,
    ): OwnerLocalFileIngressOutcome {
        if (state != RuntimeLifecycleState.RUNNING) throw ParkerRuntimeException.NotRunning(state)
        logger.info("Uploaded file evidence ingress invoked by owner")
        return ownerLocalFileIngressCoordinator.invoke(
            PrincipalId(config.ownerPrincipalId), absolutePath, receivedMediaType, originalFileName,
        )
    }

    /**
     * Document Ingestion, Derivative-to-Memory-Core Registration. The one production entry point
     * through which an already-admitted Tier A derivative may be registered into Memory Core --
     * **explicit, individually-authorized owner invocation only**, mirroring
     * [deleteEvidenceAsOwner]'s/[invokeTierAIngestionAsOwner]'s/[importEvidenceFileAsOwner]'s own
     * structural owner-only pattern exactly: this method takes **no** `requestingPrincipalId`
     * parameter, always acts as `PrincipalId(config.ownerPrincipalId)`, and there is no code path
     * through which any caller, internal or external, could substitute a different principal
     * (`docs/architecture/DOCUMENT_INGESTION_DERIVATIVE_TO_MEMORY_CORE_REGISTRATION_SCOPE_LOCK.md`
     * ("the Scope Lock") Section 5).
     *
     * Accepts only [admitted] -- an already-admitted Tier A result the owner separately obtained
     * from a prior, independent [invokeTierAIngestionAsOwner] call. There is no code path from a
     * successful [importEvidenceFileAsOwner] or [invokeTierAIngestionAsOwner] call to this method
     * -- registration is always a third, separate, explicit owner action, never automatically
     * chained (Scope Lock Section 5's own "explicit, separate, owner-triggered" decision).
     * [derivativeMemoryRegistrationCoordinator] performs the entire governed chain -- two
     * separately gated Memory Core writes, using only [admitted]'s own `record`/`format` fields,
     * never its `payload` -- this method adds no orchestration of its own beyond the
     * [RuntimeLifecycleState.RUNNING] guard every production entry point on this class already
     * requires.
     *
     * This method never invokes Knowledge promotion, Evidence Intelligence, OCR, or Tier B, never
     * writes to `DerivativeReviewRegistry`, and never alters the already-admitted derivative
     * Document Ingestion's own storage holds -- a registration failure of any kind leaves that
     * derivative exactly as durably admitted as it already was (Scope Lock Section 15).
     *
     * The log line below records no derivative identity beyond that an invocation occurred,
     * mirroring every other owner entry point's own logging discipline.
     *
     * Throws [ParkerRuntimeException.NotRunning] if [state] is not [RuntimeLifecycleState.RUNNING].
     */
    suspend fun registerDerivativeInMemoryAsOwner(
        admitted: TierADocumentRoutingResult.Admitted,
    ): DerivativeMemoryRegistrationOutcome {
        if (state != RuntimeLifecycleState.RUNNING) {
            throw ParkerRuntimeException.NotRunning(state)
        }
        logger.info("Derivative Memory Core registration invoked by owner")
        return derivativeMemoryRegistrationCoordinator.register(
            PrincipalId(config.ownerPrincipalId),
            UUID.randomUUID().toString(),
            admitted,
        )
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
        val REASONING_CONTEXT_RETRIEVAL_PURPOSE =
            AuthorizationPurposeId("knowledge-memory.reasoning-context-retrieval")
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
