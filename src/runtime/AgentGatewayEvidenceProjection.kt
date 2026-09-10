package parker.core.runtime

import java.time.Instant
import java.util.UUID
import parker.core.interfaces.AuthorizationPurposeId
import parker.core.interfaces.CandidateEvidenceArtifact
import parker.core.interfaces.EvidenceAcquisitionRoutingOutcome
import parker.core.interfaces.EvidenceArtifactId
import parker.core.interfaces.EvidenceCustodian
import parker.core.interfaces.EvidenceManifestRetrievalResult
import parker.core.interfaces.EvidenceRetrievalResult
import parker.core.interfaces.EvidenceSourceSubmissionResult
import parker.core.interfaces.ExecutionRequest
import parker.core.interfaces.PermissionDecision
import parker.core.interfaces.PermissionDecisionOutcome
import parker.core.interfaces.PermissionEngine
import parker.core.interfaces.PrincipalId
import parker.core.interfaces.RequestId
import parker.core.interfaces.RequestOrigin
import parker.core.interfaces.RequestPriority
import parker.core.interfaces.ResourceId

/**
 * Parker Agent Gateway, AG-1D/AG-1F (R0 Governed Runtime Projections and R1 Candidate-Source
 * Submission, `docs/architecture/PARKER_AGENT_GATEWAY_SCOPE_LOCK.md` Section 7 item 2, Section 8,
 * Section 20). The minimum, thin "AsAgent" projection layer for Hermes's own R0 supervision reads
 * -- evidence retrieval and evidence source-manifest retrieval, both by an already-known
 * [EvidenceArtifactId] -- and, as of AG-1F, [submitSource]: idempotent candidate-source
 * submission. All three methods share the identical structural-principal-binding and
 * no-duplicated-logic guarantees documented below.
 *
 * ## Structural principal binding (AG-1C security review's own hard requirement)
 *
 * [hermesPrincipalId] and [agentGatewayPurpose] are fixed at construction --
 * there is no method on this class, public or otherwise, through which any
 * caller could substitute a different `PrincipalId`, `AuthorizationPurposeId`,
 * `proposedAction`, or target `ResourceId`. Every [ExecutionRequest] this
 * class ever constructs always names exactly:
 * - `principalId` = [hermesPrincipalId] (production composition: AG-1B's
 *   own `PrincipalId("agent.hermes-ingestion-operator")`)
 * - `authorizationPurpose` = [agentGatewayPurpose] (production composition:
 *   AG-1B's own `AuthorizationPurposeId("agent-gateway.hermes-ingestion")`)
 * - the exact AG-1C `proposedAction`/target `ResourceId` pair for the
 *   operation requested -- never a caller-supplied verb or resource
 *
 * This mirrors exactly how `ParkerRuntime.deleteEvidenceAsOwner`/Controlled
 * Agent Run Submission already enforce principal-sensitive operations by
 * call-site structure, never by `DefaultPermissionPolicy` content -- AG-1C's
 * own documented, frozen invariant is that this policy mechanism has "no
 * per-principal matching capability at all." A `PLUGIN`, `INTERNAL_AGENT`,
 * `TOOL`, or Owner principal cannot reach either method below *as
 * themselves* -- there is no parameter through which their own identity
 * could ever be substituted for Hermes's.
 *
 * ## No duplicated logic
 *
 * This class invents no manifest, provenance, source-resolution, or
 * permission logic of its own. Each method performs exactly one
 * [PermissionEngine.evaluate] call using the Agent-Gateway-specific shape
 * above and, only if that decision is `APPROVED`/`APPROVED_WITH_CONFIRMATION`,
 * delegates unchanged to the existing, already-governed
 * [EvidenceCustodian.retrieve]/[EvidenceCustodian.retrieveManifest] -- the
 * same methods Owner UI itself already calls, via the same shared
 * [permissionEngine] instance. Those methods perform their own, separate,
 * pre-existing internal permission check under Owner's own
 * `evidence.retrieve`/`evidence.retrieve-manifest` verbs; this class adds
 * an additional, narrower, Agent-Gateway-specific gate in front of them --
 * it never replaces, weakens, or bypasses their own check.
 *
 * ## Narrow projection, not raw content
 *
 * [retrieveEvidence] deliberately projects only a byte length, never the
 * retrieved bytes themselves -- Hermes's own stated supervision need
 * (confirm a submission's presence/status across a batch) does not require
 * raw content, and returning it here would make this "thin projection
 * layer" a second content-retrieval surface the Scope Lock's own API
 * projection rules (Section 14: "never a serialized internal object")
 * caution against growing informally. [retrieveEvidenceManifest] projects
 * the manifest's own already-narrow, already-opaque fields.
 */
internal class AgentGatewayEvidenceProjection(
    private val hermesPrincipalId: PrincipalId,
    private val agentGatewayPurpose: AuthorizationPurposeId,
    private val permissionEngine: PermissionEngine,
    private val evidenceCustodian: EvidenceCustodian,
    /**
     * Parker Agent Gateway, AG-1G (R2 Governed-Acquisition Request, Section 9, Section 20). The
     * second, Hermes-principal-scoped instance of the identical, unmodified
     * `GovernedAcquisitionOwnerWorkflow` class Section 9 permits -- sharing every other
     * dependency (registry, router, execution coordinator, evidence custodian,
     * external-egress-authorisation source) by reference with `ParkerRuntime`'s own
     * owner-composed instance. `null` only for compositions that never wire governed acquisition
     * at all (mirrors this class's other constructor defaults' own "always supplied in
     * production" convention) -- [requestAcquisition] returns [AgentGatewayAcquisitionResult.Failed]
     * rather than throwing if absent.
     */
    private val governedAcquisitionWorkflow: GovernedAcquisitionOwnerWorkflow? = null,
    private val clock: () -> Instant = Instant::now,
    private val bulkIngestionBindingCoordinator: BulkIngestionBindingCoordinator? = null,
    /**
     * Hermes Processing Result Intake, Task 2. `null` only for compositions that never wire
     * processing-result intake at all -- [submitProcessingResult]/[listProcessingResultsForBatch]
     * return [AgentGatewayProcessingResultSubmissionResult.Denied]/
     * [AgentGatewayProcessingResultListResult.Denied] rather than throwing if absent, mirroring
     * [governedAcquisitionWorkflow]'s own identical "always supplied in production" convention.
     */
    private val processingResultRegistry: parker.core.interfaces.HermesProcessingResultRegistry? = null,
) {

    suspend fun retrieveEvidence(evidenceArtifactId: EvidenceArtifactId): AgentGatewayEvidenceRetrievalResult {
        val decision = permissionEngine.evaluate(
            buildRequest(
                resourceId = AGENT_GATEWAY_EVIDENCE_RETRIEVAL_RESOURCE_ID,
                actionName = AGENT_GATEWAY_EVIDENCE_RETRIEVE_ACTION_NAME,
                requestIdPrefix = "agent-gateway-evidence-retrieve",
                contextId = evidenceArtifactId.value,
            ),
        )
        if (!decision.isApproved()) {
            return AgentGatewayEvidenceRetrievalResult.Denied(evidenceArtifactId, decision.decision)
        }
        return when (val result = evidenceCustodian.retrieve(hermesPrincipalId, evidenceArtifactId)) {
            is EvidenceRetrievalResult.Found -> AgentGatewayEvidenceRetrievalResult.Found(
                evidenceArtifactId = result.evidenceArtifactId,
                byteLength = result.content.size,
            )
            is EvidenceRetrievalResult.NotFound -> AgentGatewayEvidenceRetrievalResult.NotFound(evidenceArtifactId)
            is EvidenceRetrievalResult.Rejected -> AgentGatewayEvidenceRetrievalResult.Denied(evidenceArtifactId, PermissionDecisionOutcome.DENIED)
        }
    }

    suspend fun retrieveEvidenceManifest(evidenceArtifactId: EvidenceArtifactId): AgentGatewayEvidenceManifestResult {
        val decision = permissionEngine.evaluate(
            buildRequest(
                resourceId = AGENT_GATEWAY_EVIDENCE_MANIFEST_RETRIEVAL_RESOURCE_ID,
                actionName = AGENT_GATEWAY_EVIDENCE_RETRIEVE_MANIFEST_ACTION_NAME,
                requestIdPrefix = "agent-gateway-evidence-retrieve-manifest",
                contextId = evidenceArtifactId.value,
            ),
        )
        if (!decision.isApproved()) {
            return AgentGatewayEvidenceManifestResult.Denied(evidenceArtifactId, decision.decision)
        }
        return when (val result = evidenceCustodian.retrieveManifest(hermesPrincipalId, evidenceArtifactId)) {
            is EvidenceManifestRetrievalResult.Found -> AgentGatewayEvidenceManifestResult.Found(
                AgentGatewayEvidenceManifestProjection(
                    evidenceArtifactId = result.manifest.evidenceArtifactId,
                    sha256 = result.manifest.sha256,
                    byteLength = result.manifest.byteLength,
                    receivedMediaType = result.manifest.receivedMediaType,
                    originalFileName = result.manifest.originalFileName,
                ),
            )
            is EvidenceManifestRetrievalResult.NotFound -> AgentGatewayEvidenceManifestResult.NotFound(evidenceArtifactId)
            is EvidenceManifestRetrievalResult.Rejected -> AgentGatewayEvidenceManifestResult.Denied(evidenceArtifactId, PermissionDecisionOutcome.DENIED)
        }
    }

    /**
     * Parker Agent Gateway, AG-1F (R1 Candidate-Source Submission, Section 8, Section 20).
     * Performs exactly one [PermissionEngine.evaluate] call using the Agent-Gateway-specific
     * submission shape (Hermes's own fixed principal, the fixed gateway purpose, the exact
     * `agent-gateway.evidence.submit` verb/resource -- never a caller-supplied one) and, only if
     * approved, delegates unchanged to [EvidenceCustodian.submitSource] -- the "separate, narrow,
     * `EvidenceCustodian`-owned addition" Section 8 requires, never reimplemented here. This
     * class invents no hash computation, no duplicate-detection, and no acceptance logic of its
     * own; [advisorySha256] is passed through unchanged for [EvidenceCustodian.submitSource]'s
     * own comparison against its own authoritative, independently computed hash.
     */
    suspend fun submitSource(candidate: CandidateEvidenceArtifact, advisorySha256: String?, batchId: String? = null): AgentGatewaySourceSubmissionResult {
        val decision = permissionEngine.evaluate(
            buildRequest(
                resourceId = AGENT_GATEWAY_EVIDENCE_SUBMIT_RESOURCE_ID,
                actionName = AGENT_GATEWAY_EVIDENCE_SUBMIT_ACTION_NAME,
                requestIdPrefix = "agent-gateway-evidence-submit",
                contextId = UUID.randomUUID().toString(),
            ),
        )
        if (!decision.isApproved()) {
            return AgentGatewaySourceSubmissionResult.Denied(decision.decision)
        }
        if (batchId != null && bulkIngestionBindingCoordinator?.isAuthorised(batchId) != true) {
            return AgentGatewaySourceSubmissionResult.Denied(PermissionDecisionOutcome.DENIED)
        }
        return when (val outcome = evidenceCustodian.submitSource(hermesPrincipalId, candidate, advisorySha256)) {
            is EvidenceSourceSubmissionResult.Registered -> if (batchId == null || bulkIngestionBindingCoordinator?.recordSubmission(batchId, outcome.evidenceArtifactId) == true) AgentGatewaySourceSubmissionResult.Registered(projectionOf(outcome.evidenceArtifactId, outcome.manifest)) else AgentGatewaySourceSubmissionResult.Denied(PermissionDecisionOutcome.DENIED)
            is EvidenceSourceSubmissionResult.AlreadyRegistered -> if (batchId == null || bulkIngestionBindingCoordinator?.recordSubmission(batchId, outcome.evidenceArtifactId) == true) AgentGatewaySourceSubmissionResult.AlreadyRegistered(projectionOf(outcome.evidenceArtifactId, outcome.manifest)) else AgentGatewaySourceSubmissionResult.Denied(PermissionDecisionOutcome.DENIED)
            is EvidenceSourceSubmissionResult.HashMismatch -> AgentGatewaySourceSubmissionResult.HashMismatch(outcome.computedSha256, outcome.advisorySha256)
            is EvidenceSourceSubmissionResult.Rejected -> AgentGatewaySourceSubmissionResult.Denied(PermissionDecisionOutcome.DENIED)
            is EvidenceSourceSubmissionResult.Conflict -> AgentGatewaySourceSubmissionResult.Conflict(outcome.evidenceArtifactId, outcome.computedSha256, outcome.reason)
        }
    }

    suspend fun bindIngestionEvidence(batchId: String, evidenceArtifactId: EvidenceArtifactId): AgentGatewayBulkBindingResult {
        val decision = permissionEngine.evaluate(buildRequest(
            resourceId = AGENT_GATEWAY_INGESTION_BIND_RESOURCE_ID,
            actionName = AGENT_GATEWAY_INGESTION_BIND_ACTION_NAME,
            requestIdPrefix = "agent-gateway-ingestion-bind",
            contextId = evidenceArtifactId.value,
        ))
        if (!decision.isApproved()) return AgentGatewayBulkBindingResult.Denied
        return when (val result = bulkIngestionBindingCoordinator?.assignFromHermes(batchId, evidenceArtifactId)
            ?: BulkIngestionAssignment.Failure("BULK_BINDING_NOT_CONFIGURED")) {
            is BulkIngestionAssignment.Assigned -> AgentGatewayBulkBindingResult.Assigned(result.caseId)
            BulkIngestionAssignment.UnknownBatch -> AgentGatewayBulkBindingResult.UnknownBatch
            BulkIngestionAssignment.EvidenceNotSubmittedUnderBatch -> AgentGatewayBulkBindingResult.EvidenceNotSubmitted
            is BulkIngestionAssignment.Rejected -> AgentGatewayBulkBindingResult.Rejected(result.reason)
            is BulkIngestionAssignment.Failure -> AgentGatewayBulkBindingResult.Failed(result.reason)
        }
    }

    /**
     * Hermes Processing Result Intake, Task 2. Performs exactly one [PermissionEngine.evaluate]
     * call using the Agent-Gateway-specific processing-result-submission shape (Hermes's own
     * fixed principal, the fixed gateway purpose, the exact `agent-gateway.processing-result.submit`
     * verb/resource -- never a caller-supplied one) and, only if approved, validates [batchId]
     * against the existing [bulkIngestionBindingCoordinator] before ever touching
     * [processingResultRegistry] -- mirroring [submitSource]'s own "permission first, batch
     * validity second" ordering exactly. A batch id that is not a real, Parker-minted,
     * currently-authorised batch identity -- including one that is not even shaped like one, which
     * [BulkIngestionBindingCoordinator.isAuthorised] itself validates by throwing
     * [IllegalArgumentException] rather than returning `false` -- is uniformly reported as
     * [AgentGatewayProcessingResultSubmissionResult.UnknownBatch], never a 500-shaped internal
     * failure. This class invents no case-binding logic of its own: [result] carries no `CaseId`
     * field at all (see [parker.core.interfaces.HermesProcessingResult]'s own KDoc), so there is no
     * field here through which a caller could assert or alter one.
     */
    suspend fun submitProcessingResult(batchId: String, result: parker.core.interfaces.HermesProcessingResult): AgentGatewayProcessingResultSubmissionResult {
        val decision = permissionEngine.evaluate(buildRequest(
            resourceId = AGENT_GATEWAY_PROCESSING_RESULT_SUBMIT_RESOURCE_ID,
            actionName = AGENT_GATEWAY_PROCESSING_RESULT_SUBMIT_ACTION_NAME,
            requestIdPrefix = "agent-gateway-processing-result-submit",
            contextId = "$batchId-${result.sourceSha256}",
        ))
        if (!decision.isApproved()) return AgentGatewayProcessingResultSubmissionResult.Denied(decision.decision)
        if (!isBatchAuthorised(batchId)) return AgentGatewayProcessingResultSubmissionResult.UnknownBatch
        val registry = processingResultRegistry ?: return AgentGatewayProcessingResultSubmissionResult.Denied(PermissionDecisionOutcome.DENIED)
        return when (val outcome = registry.record(result)) {
            is parker.core.interfaces.HermesProcessingResultRecordOutcome.Recorded -> AgentGatewayProcessingResultSubmissionResult.Recorded(outcome.result)
            is parker.core.interfaces.HermesProcessingResultRecordOutcome.AlreadyRecorded -> AgentGatewayProcessingResultSubmissionResult.AlreadyRecorded(outcome.result)
            is parker.core.interfaces.HermesProcessingResultRecordOutcome.Conflict -> AgentGatewayProcessingResultSubmissionResult.Conflict(outcome.existing)
        }
    }

    /**
     * Hermes Processing Result Intake, Task 2. The narrow, authorised read-back path for stored
     * processing results -- the same "permission first, batch validity second" shape
     * [submitProcessingResult] uses. Returns only results already recorded for [batchId]; never
     * leaks a result recorded under a different batch, and never offers any broader search or
     * evidence-listing behaviour.
     */
    suspend fun listProcessingResultsForBatch(batchId: String): AgentGatewayProcessingResultListResult {
        val decision = permissionEngine.evaluate(buildRequest(
            resourceId = AGENT_GATEWAY_PROCESSING_RESULT_LIST_RESOURCE_ID,
            actionName = AGENT_GATEWAY_PROCESSING_RESULT_LIST_ACTION_NAME,
            requestIdPrefix = "agent-gateway-processing-result-list",
            contextId = batchId,
        ))
        if (!decision.isApproved()) return AgentGatewayProcessingResultListResult.Denied(decision.decision)
        if (!isBatchAuthorised(batchId)) return AgentGatewayProcessingResultListResult.UnknownBatch
        return AgentGatewayProcessingResultListResult.Found(processingResultRegistry?.listForBatch(batchId) ?: emptyList())
    }

    /** `false` for a batch that does not exist, exactly as for one whose id is not even shaped like a real one -- never throws. */
    private suspend fun isBatchAuthorised(batchId: String): Boolean =
        try { bulkIngestionBindingCoordinator?.isAuthorised(batchId) == true } catch (_: IllegalArgumentException) { false }

    /**
     * Hermes Governed Ingestion, Task 3. Gates the existing governed source-submission path
     * ([submitSource]) and the existing batch/case-binding path ([bindIngestionEvidence]) behind a
     * stored Task 2 [parker.core.interfaces.HermesProcessingResult] -- it invents no dedup,
     * registration, or case-binding logic of its own. Order: permission first (reusing the exact
     * same `agent-gateway.evidence.submit` verb/resource [submitSource] itself uses -- "PASS
     * orchestration must be denied if PermissionEngine denies the existing governed
     * source-submission action," never a new trust boundary), then batch validity, then actual-byte
     * hash verification against [expectedSha256] (the caller's own declared identity for the source
     * it is submitting -- computed with [parker.core.interfaces.CanonicalPagePixelDigests.sha256],
     * Parker's own existing SHA-256 primitive, never a second hashing implementation), then the
     * stored processing result's own status.
     *
     * [submitSource] and [bindIngestionEvidence] are called unchanged -- each performs its own,
     * separate, pre-existing permission and batch-validity check; this method's own upfront checks
     * are strictly additive gating, never a bypass or a relaxation of either.
     */
    suspend fun submitGovernedIngestion(
        batchId: String,
        expectedSha256: String,
        candidate: CandidateEvidenceArtifact,
    ): AgentGatewayGovernedIngestionResult {
        val decision = permissionEngine.evaluate(buildRequest(
            resourceId = AGENT_GATEWAY_EVIDENCE_SUBMIT_RESOURCE_ID,
            actionName = AGENT_GATEWAY_EVIDENCE_SUBMIT_ACTION_NAME,
            requestIdPrefix = "agent-gateway-governed-ingestion",
            contextId = "$batchId-$expectedSha256",
        ))
        if (!decision.isApproved()) return AgentGatewayGovernedIngestionResult.Denied(decision.decision)
        if (!isBatchAuthorised(batchId)) return AgentGatewayGovernedIngestionResult.UnknownBatch

        val computedSha256 = parker.core.interfaces.CanonicalPagePixelDigests.sha256(candidate.content)
        if (!computedSha256.equals(expectedSha256, ignoreCase = true)) {
            return AgentGatewayGovernedIngestionResult.HashMismatch(computedSha256, expectedSha256)
        }

        val stored = processingResultRegistry?.find(batchId, computedSha256)
            ?: return AgentGatewayGovernedIngestionResult.ProcessingResultRequired

        when (stored.status) {
            parker.core.interfaces.HermesProcessingStatus.REVIEW_REQUIRED -> return AgentGatewayGovernedIngestionResult.HeldForReview
            parker.core.interfaces.HermesProcessingStatus.FAILED -> return AgentGatewayGovernedIngestionResult.ProcessingFailed(
                stored.failure ?: parker.core.interfaces.HermesProcessingFailure(parker.core.interfaces.HermesProcessingFailureKind.PROCESSOR_FAILURE),
            )
            parker.core.interfaces.HermesProcessingStatus.PASS -> Unit
        }

        return when (val submission = submitSource(candidate, computedSha256, batchId)) {
            is AgentGatewaySourceSubmissionResult.Registered -> completeGovernedIngestion(batchId, submission.projection, alreadyIngested = false)
            is AgentGatewaySourceSubmissionResult.AlreadyRegistered -> completeGovernedIngestion(batchId, submission.projection, alreadyIngested = true)
            is AgentGatewaySourceSubmissionResult.HashMismatch -> AgentGatewayGovernedIngestionResult.HashMismatch(submission.computedSha256, submission.advisorySha256)
            is AgentGatewaySourceSubmissionResult.Denied -> AgentGatewayGovernedIngestionResult.Denied(submission.decision)
            is AgentGatewaySourceSubmissionResult.Conflict -> AgentGatewayGovernedIngestionResult.Conflict(submission.evidenceArtifactId, submission.computedSha256, submission.reason)
        }
    }

    /**
     * Completes governed ingestion by delegating unchanged to the existing [bindIngestionEvidence]
     * -- Parker's own existing batch -> case authority remains the only source of case membership;
     * this method asserts no `CaseId` of its own. [bindIngestionEvidence] is already idempotent for
     * a repeat assignment of the same evidence (its own `Assigned`/`NoChange` mapping) -- calling it
     * again for an already-bound evidence is a harmless no-op, never a duplicate side effect.
     */
    private suspend fun completeGovernedIngestion(
        batchId: String,
        projection: AgentGatewayEvidenceManifestProjection,
        alreadyIngested: Boolean,
    ): AgentGatewayGovernedIngestionResult {
        when (val binding = bindIngestionEvidence(batchId, projection.evidenceArtifactId)) {
            is AgentGatewayBulkBindingResult.Assigned -> Unit
            AgentGatewayBulkBindingResult.Denied -> return AgentGatewayGovernedIngestionResult.Denied(PermissionDecisionOutcome.DENIED)
            AgentGatewayBulkBindingResult.UnknownBatch -> return AgentGatewayGovernedIngestionResult.UnknownBatch
            AgentGatewayBulkBindingResult.EvidenceNotSubmitted -> return AgentGatewayGovernedIngestionResult.CaseBindingRejected("EVIDENCE_NOT_SUBMITTED_UNDER_BATCH")
            is AgentGatewayBulkBindingResult.Rejected -> return AgentGatewayGovernedIngestionResult.CaseBindingRejected(binding.reason)
            is AgentGatewayBulkBindingResult.Failed -> return AgentGatewayGovernedIngestionResult.CaseBindingRejected(binding.reason)
        }
        return if (alreadyIngested) AgentGatewayGovernedIngestionResult.AlreadyIngested(projection) else AgentGatewayGovernedIngestionResult.Ingested(projection)
    }

    /**
     * Parker Agent Gateway, AG-1G (R2 Governed-Acquisition Request, Section 9, Section 20).
     * Performs exactly one [PermissionEngine.evaluate] call using the Agent-Gateway-specific
     * acquisition-request shape (Hermes's own fixed principal, the fixed gateway purpose, the
     * exact `agent-gateway.evidence.acquire` verb/resource -- never a caller-supplied one) and,
     * only if approved, delegates unchanged to [governedAcquisitionWorkflow] -- the same,
     * unmodified `GovernedAcquisitionOwnerWorkflow` class the Owner UI's own governed acquisition
     * route already uses, Hermes-principal-scoped. This class invents no routing, source
     * verification, egress-authorisation, or provider logic of its own.
     *
     * `evaluate` is called first; on [GovernedAcquisitionOwnerEvaluation.Evaluated] with a
     * [EvidenceAcquisitionRoutingOutcome.Selected] routing outcome, `execute` is called with
     * exactly the capability id that same evaluation just selected -- never a caller-supplied
     * "expected capability" (Hermes never sees or chooses a capability; Parker decides the
     * entire path). Passing the freshly-computed id back in as the expected one is safe by
     * construction: `execute` internally re-evaluates and compares against it, so a genuine race
     * between these two calls naturally falls through to [GovernedAcquisitionOwnerExecution.StaleOrUnavailable],
     * never a mismatched or stale acquisition.
     */
    suspend fun requestAcquisition(evidenceArtifactId: EvidenceArtifactId): AgentGatewayAcquisitionResult {
        val decision = permissionEngine.evaluate(
            buildRequest(
                resourceId = AGENT_GATEWAY_EVIDENCE_ACQUIRE_RESOURCE_ID,
                actionName = AGENT_GATEWAY_EVIDENCE_ACQUIRE_ACTION_NAME,
                requestIdPrefix = "agent-gateway-evidence-acquire",
                contextId = evidenceArtifactId.value,
            ),
        )
        if (!decision.isApproved()) {
            return AgentGatewayAcquisitionResult.Denied(decision.decision)
        }
        val workflow = governedAcquisitionWorkflow
            ?: return AgentGatewayAcquisitionResult.Failed(evidenceArtifactId, "ACQUISITION_NOT_CONFIGURED")
        return mapEvaluation(evidenceArtifactId, workflow.evaluate(evidenceArtifactId), workflow)
    }

    private suspend fun mapEvaluation(
        evidenceArtifactId: EvidenceArtifactId,
        evaluation: GovernedAcquisitionOwnerEvaluation,
        workflow: GovernedAcquisitionOwnerWorkflow,
    ): AgentGatewayAcquisitionResult = when (evaluation) {
        is GovernedAcquisitionOwnerEvaluation.SourceUnavailable ->
            if (evaluation.reason == "SOURCE_MANIFEST_NOT_FOUND") {
                AgentGatewayAcquisitionResult.NotFound(evidenceArtifactId)
            } else {
                AgentGatewayAcquisitionResult.Failed(evidenceArtifactId, evaluation.reason)
            }
        is GovernedAcquisitionOwnerEvaluation.Evaluated -> when (val routing = evaluation.routing) {
            is EvidenceAcquisitionRoutingOutcome.Selected -> when (
                val execution = workflow.execute(evidenceArtifactId, routing.decision.capability.capabilityId)
            ) {
                is GovernedAcquisitionOwnerExecution.Executed -> mapExecutionResult(evidenceArtifactId, execution.result)
                is GovernedAcquisitionOwnerExecution.StaleOrUnavailable -> mapEvaluation(evidenceArtifactId, execution.current, workflow)
            }
            is EvidenceAcquisitionRoutingOutcome.NoEligibleCapability -> mapNoSelection(evidenceArtifactId, routing.reasons)
            is EvidenceAcquisitionRoutingOutcome.Indeterminate -> mapNoSelection(evidenceArtifactId, routing.reasons)
            is EvidenceAcquisitionRoutingOutcome.Ambiguous -> mapNoSelection(evidenceArtifactId, routing.reasons)
        }
    }

    /**
     * The router's own [EvidenceAcquisitionRoutingOutcome.NoEligibleCapability.reasons] (and
     * [EvidenceAcquisitionRoutingOutcome.Indeterminate.reasons]/[EvidenceAcquisitionRoutingOutcome.Ambiguous.reasons])
     * is a *flat union* across every considered capability's own independent ineligibility
     * reasons (`DeterministicEvidenceAcquisitionRouter.noEligibleReasons`) -- it always includes
     * the constant [parker.core.interfaces.AcquisitionNoSelectionReason.NO_ELIGIBLE_CAPABILITY]
     * marker, plus one entry per *other* capability's own distinct failure reason. A disabled
     * capability (Local OCR, in production) contributes
     * [parker.core.interfaces.AcquisitionNoSelectionReason.CAPABILITY_DISABLED_OR_NOT_READY] to
     * this same set on *every* submission regardless of media type, so its mere presence does not
     * by itself mean "every otherwise-eligible capability is disabled" -- it may simply mean one
     * permanently-disabled, already-inapplicable capability happened to also be considered.
     * [AgentGatewayAcquisitionResult.ProviderNotReady] is therefore reported only when disablement
     * is the *entire* remaining explanation (no unsupported-media/fidelity/limit reason also
     * present) -- otherwise the more informative [AgentGatewayAcquisitionResult.Failed] is
     * returned, echoing every reason. [AgentGatewayAcquisitionResult.AuthorizationRequired] always
     * takes priority when present -- it is the one actionable state a human owner, not Hermes,
     * must resolve, regardless of what else also failed to match.
     */
    private fun mapNoSelection(
        evidenceArtifactId: EvidenceArtifactId,
        reasons: Set<parker.core.interfaces.AcquisitionNoSelectionReason>,
    ): AgentGatewayAcquisitionResult {
        val meaningful = reasons - parker.core.interfaces.AcquisitionNoSelectionReason.NO_ELIGIBLE_CAPABILITY
        return when {
            parker.core.interfaces.AcquisitionNoSelectionReason.EXTERNAL_EGRESS_NOT_AUTHORISED in meaningful ->
                AgentGatewayAcquisitionResult.AuthorizationRequired(evidenceArtifactId)
            meaningful.isNotEmpty() && meaningful == setOf(parker.core.interfaces.AcquisitionNoSelectionReason.CAPABILITY_DISABLED_OR_NOT_READY) ->
                AgentGatewayAcquisitionResult.ProviderNotReady(evidenceArtifactId)
            else -> AgentGatewayAcquisitionResult.Failed(evidenceArtifactId, reasons.joinToString(",") { it.name }.ifEmpty { "NO_ELIGIBLE_CAPABILITY" })
        }
    }

    private fun mapExecutionResult(
        evidenceArtifactId: EvidenceArtifactId,
        result: GovernedAcquisitionExecutionResult,
    ): AgentGatewayAcquisitionResult = when (result) {
        is GovernedAcquisitionExecutionResult.Admitted -> AgentGatewayAcquisitionResult.Completed(
            evidenceArtifactId = evidenceArtifactId,
            derivativeGenerationId = result.derivativeGenerationId,
            capabilityId = result.routingProvenance.capabilityId,
            mechanism = result.routingProvenance.mechanism,
        )
        is GovernedAcquisitionExecutionResult.Failed -> {
            val routingReasons = when (val routing = result.routingOutcome) {
                is EvidenceAcquisitionRoutingOutcome.NoEligibleCapability -> routing.reasons
                is EvidenceAcquisitionRoutingOutcome.Indeterminate -> routing.reasons
                is EvidenceAcquisitionRoutingOutcome.Ambiguous -> routing.reasons
                is EvidenceAcquisitionRoutingOutcome.Selected, null -> emptySet()
            }
            val meaningful = routingReasons - parker.core.interfaces.AcquisitionNoSelectionReason.NO_ELIGIBLE_CAPABILITY
            if (parker.core.interfaces.AcquisitionNoSelectionReason.EXTERNAL_EGRESS_NOT_AUTHORISED in meaningful) {
                AgentGatewayAcquisitionResult.AuthorizationRequired(evidenceArtifactId)
            } else if (meaningful.isNotEmpty() && meaningful == setOf(parker.core.interfaces.AcquisitionNoSelectionReason.CAPABILITY_DISABLED_OR_NOT_READY)) {
                AgentGatewayAcquisitionResult.ProviderNotReady(evidenceArtifactId)
            } else {
                AgentGatewayAcquisitionResult.Failed(evidenceArtifactId, result.reason.name)
            }
        }
    }

    private fun projectionOf(evidenceArtifactId: EvidenceArtifactId, manifest: parker.core.interfaces.EvidenceSourceManifest) =
        AgentGatewayEvidenceManifestProjection(
            evidenceArtifactId = evidenceArtifactId,
            sha256 = manifest.sha256,
            byteLength = manifest.byteLength,
            receivedMediaType = manifest.receivedMediaType,
            originalFileName = manifest.originalFileName,
        )

    private fun PermissionDecision.isApproved(): Boolean =
        decision == PermissionDecisionOutcome.APPROVED || decision == PermissionDecisionOutcome.APPROVED_WITH_CONFIRMATION

    private fun buildRequest(
        resourceId: ResourceId,
        actionName: String,
        requestIdPrefix: String,
        contextId: String,
    ): ExecutionRequest {
        val now = clock()
        return ExecutionRequest(
            requestId = RequestId("$requestIdPrefix-$contextId-${UUID.randomUUID()}"),
            principalId = hermesPrincipalId,
            origin = RequestOrigin.AGENT,
            intent = "Agent Gateway R0/R1 evidence supervision request",
            targetResources = listOf(resourceId),
            proposedActions = listOf(actionName),
            priority = RequestPriority.NORMAL,
            createdAt = now,
            correlationId = "$requestIdPrefix-$contextId",
            authorizationPurpose = agentGatewayPurpose,
        )
    }

    companion object {
        const val AGENT_GATEWAY_EVIDENCE_RETRIEVE_ACTION_NAME = "agent-gateway.evidence.retrieve"
        const val AGENT_GATEWAY_EVIDENCE_RETRIEVE_MANIFEST_ACTION_NAME = "agent-gateway.evidence.retrieve-manifest"
        const val AGENT_GATEWAY_EVIDENCE_SUBMIT_ACTION_NAME = "agent-gateway.evidence.submit"
        const val AGENT_GATEWAY_EVIDENCE_ACQUIRE_ACTION_NAME = "agent-gateway.evidence.acquire"
        const val AGENT_GATEWAY_INGESTION_BIND_ACTION_NAME = "agent-gateway.ingestion.bind"
        val AGENT_GATEWAY_EVIDENCE_RETRIEVAL_RESOURCE_ID = ResourceId("agent-gateway-evidence-retrieval")
        val AGENT_GATEWAY_EVIDENCE_MANIFEST_RETRIEVAL_RESOURCE_ID = ResourceId("agent-gateway-evidence-manifest-retrieval")
        val AGENT_GATEWAY_EVIDENCE_SUBMIT_RESOURCE_ID = ResourceId("agent-gateway-evidence-submit")
        val AGENT_GATEWAY_EVIDENCE_ACQUIRE_RESOURCE_ID = ResourceId("agent-gateway-evidence-acquire")
        val AGENT_GATEWAY_INGESTION_BIND_RESOURCE_ID = ResourceId("agent-gateway-ingestion-bind")

        // Hermes Processing Result Intake, Task 2: one new write verb (submitting a result) and
        // one new read verb (listing already-submitted results for a batch) -- reusing the
        // identical existing (WRITE, DOCUMENT)/(READ, DOCUMENT) pairs every other Agent Gateway
        // verb above already uses, and the same existing AGENT_GATEWAY_HERMES_INGESTION_PURPOSE
        // (ParkerRuntime's own companion object) -- no new PermissionAction, ResourceType, or
        // AuthorizationPurposeId is introduced anywhere by this Task.
        const val AGENT_GATEWAY_PROCESSING_RESULT_SUBMIT_ACTION_NAME = "agent-gateway.processing-result.submit"
        const val AGENT_GATEWAY_PROCESSING_RESULT_LIST_ACTION_NAME = "agent-gateway.processing-result.list"
        val AGENT_GATEWAY_PROCESSING_RESULT_SUBMIT_RESOURCE_ID = ResourceId("agent-gateway-processing-result-submit")
        val AGENT_GATEWAY_PROCESSING_RESULT_LIST_RESOURCE_ID = ResourceId("agent-gateway-processing-result-list")
    }
}

/**
 * Hermes Processing Result Intake, Task 2. Reuses [AgentGatewayEvidenceManifestProjection]'s own
 * "flat, opaque-identifier-only projection" precedent conceptually, but the underlying value is
 * already exactly the flat, fully-validated [parker.core.interfaces.HermesProcessingResult]
 * domain type itself -- Task 1's own contract -- so no separate projection type wraps it here.
 */
sealed class AgentGatewayProcessingResultSubmissionResult {
    data class Recorded(val result: parker.core.interfaces.HermesProcessingResult) : AgentGatewayProcessingResultSubmissionResult()
    data class AlreadyRecorded(val result: parker.core.interfaces.HermesProcessingResult) : AgentGatewayProcessingResultSubmissionResult()

    /** [existing] is the unchanged, previously-recorded result; the attempted submission was not stored. */
    data class Conflict(val existing: parker.core.interfaces.HermesProcessingResult) : AgentGatewayProcessingResultSubmissionResult()

    /** No batch exists under the requested id -- including one that is not even shaped like a real one. */
    data object UnknownBatch : AgentGatewayProcessingResultSubmissionResult()
    data class Denied(val decision: PermissionDecisionOutcome) : AgentGatewayProcessingResultSubmissionResult()
}

/** The narrow, authorised, single-batch read-back path for stored [parker.core.interfaces.HermesProcessingResult] records. */
sealed class AgentGatewayProcessingResultListResult {
    data class Found(val results: List<parker.core.interfaces.HermesProcessingResult>) : AgentGatewayProcessingResultListResult()
    data object UnknownBatch : AgentGatewayProcessingResultListResult()
    data class Denied(val decision: PermissionDecisionOutcome) : AgentGatewayProcessingResultListResult()
}

/**
 * Hermes Governed Ingestion, Task 3. The gated outcome of [AgentGatewayEvidenceProjection.submitGovernedIngestion]
 * -- a stored [parker.core.interfaces.HermesProcessingResult] deciding whether the existing governed
 * source-submission/batch-binding path ([AgentGatewaySourceSubmissionResult]/[AgentGatewayBulkBindingResult])
 * is ever reached at all. [Ingested]/[AlreadyIngested] carry the same
 * [AgentGatewayEvidenceManifestProjection] those existing outcomes already return -- Parker's
 * existing hash-based dedup remains the sole source of the final, authoritative
 * [parker.core.interfaces.EvidenceArtifactId]; nothing here mints or asserts one independently.
 */
sealed class AgentGatewayGovernedIngestionResult {

    /** No prior evidence existed for this hash; governed submission and case binding both newly completed. */
    data class Ingested(val projection: AgentGatewayEvidenceManifestProjection) : AgentGatewayGovernedIngestionResult()

    /** This exact source hash was already governed-ingested (by an earlier attempt or a repeat of this same one) -- idempotent, not a duplicate. */
    data class AlreadyIngested(val projection: AgentGatewayEvidenceManifestProjection) : AgentGatewayGovernedIngestionResult()

    /** The stored processing result's status is REVIEW_REQUIRED -- no EvidenceArtifactId is minted or registered. */
    data object HeldForReview : AgentGatewayGovernedIngestionResult()

    /** The stored processing result's status is FAILED -- no governed evidence admission occurs. */
    data class ProcessingFailed(val failure: parker.core.interfaces.HermesProcessingFailure) : AgentGatewayGovernedIngestionResult()

    /** No stored [parker.core.interfaces.HermesProcessingResult] exists for this exact (batchId, sourceSha256) -- the sanctioned Hermes bulk path never silently falls back to ungated submission. */
    data object ProcessingResultRequired : AgentGatewayGovernedIngestionResult()

    /** The actual submitted bytes hash to something other than the caller's own declared [expectedSha256] -- rejected outright, never merely logged. */
    data class HashMismatch(val computedSha256: String, val expectedSha256: String) : AgentGatewayGovernedIngestionResult()

    /** No batch exists under the requested id -- including one that is not even shaped like a real one. */
    data object UnknownBatch : AgentGatewayGovernedIngestionResult()

    /** The Agent-Gateway-specific permission check (the same verb ordinary source submission uses) was not approved. */
    data class Denied(val decision: PermissionDecisionOutcome) : AgentGatewayGovernedIngestionResult()

    /** Mirrors [AgentGatewaySourceSubmissionResult.Conflict] exactly -- a rare internal consistency fault, never an ordinary outcome. */
    data class Conflict(val evidenceArtifactId: EvidenceArtifactId, val computedSha256: String, val reason: String) : AgentGatewayGovernedIngestionResult()

    /** The existing batch/case-binding step ([AgentGatewayBulkBindingResult.Rejected]/[AgentGatewayBulkBindingResult.Failed]) refused to complete case membership for otherwise-successfully-submitted evidence. */
    data class CaseBindingRejected(val reason: String) : AgentGatewayGovernedIngestionResult()
}

sealed interface AgentGatewayBulkBindingResult {
    data class Assigned(val caseId: parker.core.interfaces.CaseId) : AgentGatewayBulkBindingResult
    data object UnknownBatch : AgentGatewayBulkBindingResult
    data object EvidenceNotSubmitted : AgentGatewayBulkBindingResult
    data class Rejected(val reason: String) : AgentGatewayBulkBindingResult
    data class Failed(val reason: String) : AgentGatewayBulkBindingResult
    data object Denied : AgentGatewayBulkBindingResult
}

/**
 * AG-1D's own narrow projection of [EvidenceRetrievalResult] -- never the
 * retrieved bytes, never an internal storage/manifest type. `byteLength`
 * is the one fact this projection carries beyond identity/outcome, useful
 * for Hermes to sanity-check a prior submission's size without granting
 * content access through this path.
 */
sealed class AgentGatewayEvidenceRetrievalResult {
    data class Found(val evidenceArtifactId: EvidenceArtifactId, val byteLength: Int) : AgentGatewayEvidenceRetrievalResult()
    data class NotFound(val evidenceArtifactId: EvidenceArtifactId) : AgentGatewayEvidenceRetrievalResult()
    data class Denied(val evidenceArtifactId: EvidenceArtifactId, val decision: PermissionDecisionOutcome) : AgentGatewayEvidenceRetrievalResult()
}

/** AG-1D's own narrow projection of [EvidenceManifestRetrievalResult] -- a flat copy of the manifest's own already-opaque fields, never the internal type itself. */
sealed class AgentGatewayEvidenceManifestResult {
    data class Found(val manifest: AgentGatewayEvidenceManifestProjection) : AgentGatewayEvidenceManifestResult()
    data class NotFound(val evidenceArtifactId: EvidenceArtifactId) : AgentGatewayEvidenceManifestResult()
    data class Denied(val evidenceArtifactId: EvidenceArtifactId, val decision: PermissionDecisionOutcome) : AgentGatewayEvidenceManifestResult()
}

/**
 * A flat, opaque-identifier-only projection of [parker.core.interfaces.EvidenceSourceManifest]
 * -- no filesystem path, no storage/database key, no internal object
 * reference. `receivedMediaType`/`originalFileName` remain nullable exactly
 * as the source manifest itself declares them (Scope Lock: "never inferred,
 * computed, or fabricated").
 */
data class AgentGatewayEvidenceManifestProjection(
    val evidenceArtifactId: EvidenceArtifactId,
    val sha256: String,
    val byteLength: Long,
    val receivedMediaType: String?,
    val originalFileName: String?,
)

/**
 * AG-1F's own narrow projection of [EvidenceSourceSubmissionResult] -- reuses
 * [AgentGatewayEvidenceManifestProjection] for both success variants (the same flat,
 * opaque-identifier-only fields the manifest read projection already returns; a newly-registered
 * and an already-registered source are equally safe to describe this way). No raw bytes, no
 * filesystem path, in either variant.
 */
sealed class AgentGatewaySourceSubmissionResult {
    data class Registered(val projection: AgentGatewayEvidenceManifestProjection) : AgentGatewaySourceSubmissionResult()
    data class AlreadyRegistered(val projection: AgentGatewayEvidenceManifestProjection) : AgentGatewaySourceSubmissionResult()
    data class HashMismatch(val computedSha256: String, val advisorySha256: String) : AgentGatewaySourceSubmissionResult()
    data class Denied(val decision: PermissionDecisionOutcome) : AgentGatewaySourceSubmissionResult()

    /**
     * Crash-safe idempotency review correction. Mirrors [EvidenceSourceSubmissionResult.Conflict]
     * -- an internal consistency fault in canonical Evidence Custodian state under the reserved
     * identity, never an ordinary outcome. No raw storage detail beyond [reason]'s plain-language
     * explanation crosses this projection boundary.
     */
    data class Conflict(val evidenceArtifactId: EvidenceArtifactId, val computedSha256: String, val reason: String) : AgentGatewaySourceSubmissionResult()
}

/**
 * AG-1G's own narrow projection of governed acquisition's existing result types
 * ([GovernedAcquisitionOwnerEvaluation]/[GovernedAcquisitionOwnerExecution]/
 * [GovernedAcquisitionExecutionResult]). Every variant carries only an opaque identifier, a
 * narrow enum-derived diagnostic string, or -- for [Completed] -- the already-opaque
 * [parker.core.interfaces.DerivativeGenerationId]/[parker.core.interfaces.EvidenceAcquisitionMechanism]
 * the existing machinery itself already returns. No raw source bytes, filesystem path, provider
 * credential, or stack trace crosses this projection boundary; [Failed.reason] is always derived
 * from an existing Parker enum's own `name`, never a caught exception's message.
 *
 * Governed acquisition is fully synchronous end-to-end (`GovernedAcquisitionOwnerWorkflow.execute`
 * suspends until the selected executor -- Tier A native extraction, Local OCR, or the external
 * transcription invocation coordinator -- itself returns), so [Completed] is the final result, not
 * a "started" or "accepted" placeholder; there is no separate status/polling projection because
 * no asynchronous job system exists anywhere in this call chain.
 *
 * No `AlreadyComplete` variant: the existing governed acquisition machinery (`GovernedAcquisitionOwnerWorkflow`,
 * `GovernedAcquisitionExecutionCoordinator`, and every `BoundAcquisitionCapabilityExecutor`) carries
 * no signal distinguishing "this evidence already has a derivative" from an ordinary execution
 * outcome -- inventing one here would assert a fact current Parker semantics cannot back.
 */
sealed class AgentGatewayAcquisitionResult {

    /** Governed acquisition executed and durably admitted a derivative. Terminal -- acquisition is synchronous. */
    data class Completed(
        val evidenceArtifactId: EvidenceArtifactId,
        val derivativeGenerationId: parker.core.interfaces.DerivativeGenerationId,
        val capabilityId: String,
        val mechanism: parker.core.interfaces.EvidenceAcquisitionMechanism,
    ) : AgentGatewayAcquisitionResult()

    /**
     * Governed routing determined that the selected capability requires external egress, and the
     * exact-target authorisation Section 9's external-egress fail-closed contract requires is
     * absent. Hermes cannot satisfy this itself -- see this class's own file KDoc, "Egress
     * authorisation."
     */
    data class AuthorizationRequired(val evidenceArtifactId: EvidenceArtifactId) : AgentGatewayAcquisitionResult()

    /** Governed routing determined every otherwise-eligible capability is disabled or not yet ready (e.g. unaccepted provider configuration). */
    data class ProviderNotReady(val evidenceArtifactId: EvidenceArtifactId) : AgentGatewayAcquisitionResult()

    /** No source manifest exists under this exact identity -- distinct from a routing/execution failure. */
    data class NotFound(val evidenceArtifactId: EvidenceArtifactId) : AgentGatewayAcquisitionResult()

    /**
     * Any other governed-acquisition failure -- routing found no eligible capability for a reason
     * other than egress/readiness, source verification failed, or the selected executor itself
     * failed. [reason] is always one of a fixed, already-existing Parker enum's own constant
     * names (never free text, never an exception message).
     */
    data class Failed(val evidenceArtifactId: EvidenceArtifactId, val reason: String) : AgentGatewayAcquisitionResult()

    /** The Agent-Gateway-specific acquisition-request permission check was not approved. */
    data class Denied(val decision: PermissionDecisionOutcome) : AgentGatewayAcquisitionResult()
}
