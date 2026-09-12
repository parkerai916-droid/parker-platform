package parker.core.runtime

import java.time.Instant
import java.util.UUID
import parker.core.interfaces.ExecutionRequest
import parker.core.interfaces.HermesProcessingCorrection
import parker.core.interfaces.HermesProcessingDecisionRegistry
import parker.core.interfaces.HermesProcessingHumanDecision
import parker.core.interfaces.HermesProcessingHumanDecisionType
import parker.core.interfaces.HermesPreIngestionCorrectedRepresentation
import parker.core.interfaces.HermesPreIngestionCorrectionPublication
import parker.core.interfaces.HermesPreIngestionCorrectionRegistry
import parker.core.interfaces.HermesProcessingResultRegistry
import parker.core.interfaces.HermesProcessingStatus
import parker.core.interfaces.PermissionDecision
import parker.core.interfaces.PermissionDecisionOutcome
import parker.core.interfaces.PermissionEngine
import parker.core.interfaces.PrincipalId
import parker.core.interfaces.RequestId
import parker.core.interfaces.RequestOrigin
import parker.core.interfaces.RequestPriority
import parker.core.interfaces.ResourceId

/**
 * Hermes Exception Decision Backend, Task 4. The one production seam through which an Owner
 * decision against a pre-ingestion Hermes exception is authorised and durably recorded -- the
 * `Steve -> Owner-authenticated endpoint -> PermissionEngine / Owner authorization -> pre-ingestion
 * decision coordinator` trust boundary Task 4 requires. Mirrors
 * [DefaultOwnerEvidenceDeletionAuthority]'s own established shape exactly: a
 * [PermissionEngine.evaluate] call first, using a fixed [ResourceId]/proposed-action pair this
 * class alone ever names, then (only if approved) the actual read/write.
 *
 * **Structural Owner-only reachability.** Like every other Owner-only coordinator in this
 * codebase, [PermissionEngine]'s own flat (action, resourceType) policy content has "no
 * per-principal matching capability at all" -- Owner-scoping is enforced instead by call-site
 * structure: `ParkerRuntime.recordHermesProcessingDecisionAsOwner`/`listHermesProcessingReviewAsOwner`
 * are the only production callers of this class, and both always supply
 * `PrincipalId(config.ownerPrincipalId)` themselves -- never a caller-supplied principal. Hermes's
 * own principal ([ParkerRuntime.HERMES_INGESTION_OPERATOR_PRINCIPAL_ID]) is never passed to
 * [AgentGatewayEvidenceProjection] (the only class Hermes's own HTTP surface,
 * `AgentGatewayHttpServer`, ever reaches), so Hermes has no code path to this class at all -- not
 * merely a policy-content denial, a structural absence of any reachable call site.
 *
 * **No case, evidence, or provenance authority of its own.** This class never touches
 * [BulkIngestionBindingCoordinator] beyond the read-only, optional [caseDisplayNameForBatch]
 * lookup used only for [listPendingForOwner]'s own display projection -- it never authorises,
 * mutates, or resolves case membership.
 */
class HermesProcessingDecisionCoordinator(
    private val permissionEngine: PermissionEngine,
    private val processingResultRegistry: HermesProcessingResultRegistry,
    private val decisionRegistry: HermesProcessingDecisionRegistry,
    private val caseDisplayNameForBatch: suspend (String) -> String? = { null },
    private val clock: () -> Instant = Instant::now,
    private val preIngestionCorrectionRegistry: HermesPreIngestionCorrectionRegistry? = null,
) {

    /**
     * Records one Owner decision against the stored [parker.core.interfaces.HermesProcessingResult]
     * for (`batchId`, `sourceSha256`). Order: permission first, then existence of a stored
     * processing result, then decision-shape legality against that result's own current status --
     * exactly Task 4's own required "no evidence admission" discipline for an illegal ACCEPT/CORRECT.
     *
     * - No stored [parker.core.interfaces.HermesProcessingResult] exists for this exact key ->
     *   [HermesProcessingDecisionOutcome.UnknownProcessingResult]. The sanctioned Hermes bulk path
     *   never lets a decision be recorded against a source Hermes has not itself reported on.
     * - `ACCEPT` while the stored status is `FAILED` -> [HermesProcessingDecisionOutcome.InvalidDecision]
     *   (Task 4's own strict rule: "FAILED -> cannot ACCEPT directly"). Nothing is recorded.
     * - `CORRECT` whose [HermesProcessingCorrection.issueIndex] does not name an existing entry in
     *   the stored result's own `issues` list -> [HermesProcessingDecisionOutcome.InvalidDecision].
     *   Nothing is recorded. (This structurally also covers every FAILED result with no `issues`
     *   at all -- the ordinary case -- without a FAILED-specific special case.)
     * - `ACCEPT` or `CORRECT` against `REVIEW_REQUIRED` without a non-blank Owner explanation ->
     *   [HermesProcessingDecisionOutcome.InvalidDecision]. The machine discrepancy and the Owner's
     *   explanation remain separate facts; an override is never a reasonless approval flag.
     * - Otherwise -> durably appended via [decisionRegistry], returned as
     *   [HermesProcessingDecisionOutcome.Recorded].
     */
    suspend fun recordDecision(
        requestingPrincipalId: PrincipalId,
        batchId: String,
        sourceSha256: String,
        decisionType: HermesProcessingHumanDecisionType,
        reason: String?,
        correction: HermesProcessingCorrection?,
    ): HermesProcessingDecisionOutcome {
        val decision = permissionEngine.evaluate(
            buildRequest(requestingPrincipalId, DECISION_RESOURCE_ID, DECISION_RECORD_ACTION_NAME, "$batchId-$sourceSha256"),
        )
        if (!decision.isApproved()) {
            return HermesProcessingDecisionOutcome.Denied(decision.decision)
        }

        val stored = processingResultRegistry.find(batchId, sourceSha256)
            ?: return HermesProcessingDecisionOutcome.UnknownProcessingResult

        when (decisionType) {
            HermesProcessingHumanDecisionType.ACCEPT -> {
                if (stored.status == HermesProcessingStatus.FAILED) {
                    return HermesProcessingDecisionOutcome.InvalidDecision(
                        "ACCEPT is not permitted while the Hermes processing status is FAILED",
                    )
                }
                if (stored.status == HermesProcessingStatus.REVIEW_REQUIRED && reason.isNullOrBlank()) {
                    return HermesProcessingDecisionOutcome.InvalidDecision(
                        "an Owner explanation is required to override REVIEW_REQUIRED",
                    )
                }
            }
            HermesProcessingHumanDecisionType.CORRECT -> {
                if (stored.status == HermesProcessingStatus.REVIEW_REQUIRED && reason.isNullOrBlank()) {
                    return HermesProcessingDecisionOutcome.InvalidDecision(
                        "an Owner explanation is required to override REVIEW_REQUIRED",
                    )
                }
                val index = correction?.issueIndex
                if (index == null || index !in stored.issues.indices) {
                    return HermesProcessingDecisionOutcome.InvalidDecision(
                        "correction.issueIndex does not name an existing processing issue for this source",
                    )
                }
            }
            HermesProcessingHumanDecisionType.REPROCESS, HermesProcessingHumanDecisionType.REJECT -> Unit
        }

        val recorded = decisionRegistry.record(
            HermesProcessingHumanDecision(
                batchId = batchId,
                sourceSha256 = sourceSha256,
                decision = decisionType,
                decidedBy = requestingPrincipalId,
                decidedAt = clock(),
                reason = reason,
                correction = correction,
            ),
        )
        val publication = if (decisionType == HermesProcessingHumanDecisionType.CORRECT && stored.status == HermesProcessingStatus.REVIEW_REQUIRED) {
            val correction = requireNotNull(correction)
            val issue = stored.issues[correction.issueIndex]
            try {
                preIngestionCorrectionRegistry?.publish(
                    HermesPreIngestionCorrectedRepresentation(
                        representationId = HermesPreIngestionCorrectedRepresentation.deriveId(
                            stored.batchId, stored.sourceSha256, correction.issueIndex, issue.kind, issue.explanation,
                            issue.hermesInterpretation, correction.correctedInterpretation, requireNotNull(reason), requestingPrincipalId,
                        ),
                        batchId = stored.batchId,
                        sourceSha256 = stored.sourceSha256,
                        machineResultStatus = stored.status,
                        issueIndex = correction.issueIndex,
                        machineIssueKind = issue.kind,
                        machineIssueExplanation = issue.explanation,
                        machineInterpretation = issue.hermesInterpretation,
                        correctedInterpretation = correction.correctedInterpretation,
                        ownerExplanation = requireNotNull(reason),
                        ownerPrincipalId = requestingPrincipalId,
                        decisionAt = recorded.decidedAt,
                    ),
                ) ?: HermesPreIngestionCorrectionPublication.Failed("CORRECTION_PUBLICATION_NOT_CONFIGURED")
            } catch (e: Exception) {
                HermesPreIngestionCorrectionPublication.Failed(e.message ?: "CORRECTION_PUBLICATION_FAILED")
            }
        } else HermesPreIngestionCorrectionPublication.NotRequired
        return HermesProcessingDecisionOutcome.Recorded(recorded, publication)
    }

    /** The Owner review queue (see [HermesProcessingReviewProjection]), gated by the same, separate `hermes-processing.review.list` verb. */
    suspend fun listPendingForOwner(requestingPrincipalId: PrincipalId): HermesProcessingReviewListOutcome {
        val decision = permissionEngine.evaluate(
            buildRequest(requestingPrincipalId, REVIEW_RESOURCE_ID, REVIEW_LIST_ACTION_NAME, "review-${UUID.randomUUID()}"),
        )
        if (!decision.isApproved()) {
            return HermesProcessingReviewListOutcome.Denied(decision.decision)
        }
        val projection = HermesProcessingReviewProjection(processingResultRegistry, decisionRegistry, caseDisplayNameForBatch)
        return HermesProcessingReviewListOutcome.Found(projection.listPendingForOwner())
    }

    private fun PermissionDecision.isApproved(): Boolean =
        decision == PermissionDecisionOutcome.APPROVED || decision == PermissionDecisionOutcome.APPROVED_WITH_CONFIRMATION

    private fun buildRequest(
        requestingPrincipalId: PrincipalId,
        resourceId: ResourceId,
        actionName: String,
        correlationSuffix: String,
    ): ExecutionRequest {
        val now = clock()
        return ExecutionRequest(
            requestId = RequestId("hermes-processing-decision-$correlationSuffix-${UUID.randomUUID()}"),
            principalId = requestingPrincipalId,
            origin = RequestOrigin.REMOTE_INTERFACE,
            intent = "Owner pre-ingestion Hermes exception decision",
            targetResources = listOf(resourceId),
            proposedActions = listOf(actionName),
            priority = RequestPriority.NORMAL,
            createdAt = now,
            correlationId = "hermes-processing-decision-$correlationSuffix",
        )
    }

    companion object {
        /** Not registered anywhere by this class -- mirrors [DefaultOwnerEvidenceDeletionAuthority]'s own disclosed convention; runtime composition performs registration. */
        val DECISION_RESOURCE_ID: ResourceId = ResourceId("hermes-processing-decision")
        val REVIEW_RESOURCE_ID: ResourceId = ResourceId("hermes-processing-review")
        const val DECISION_RECORD_ACTION_NAME: String = "hermes-processing.decision.record"
        const val REVIEW_LIST_ACTION_NAME: String = "hermes-processing.review.list"
    }
}

/** The typed outcome of [HermesProcessingDecisionCoordinator.recordDecision]. */
sealed class HermesProcessingDecisionOutcome {
    data class Recorded(
        val decision: HermesProcessingHumanDecision,
        val correctionPublication: HermesPreIngestionCorrectionPublication = HermesPreIngestionCorrectionPublication.NotRequired,
    ) : HermesProcessingDecisionOutcome()

    /** No stored [parker.core.interfaces.HermesProcessingResult] exists for this exact (batchId, sourceSha256). */
    data object UnknownProcessingResult : HermesProcessingDecisionOutcome()

    /** The requested decision is not permitted against the stored result's current status/shape. Nothing is recorded. */
    data class InvalidDecision(val reason: String) : HermesProcessingDecisionOutcome()
    data class Denied(val decision: PermissionDecisionOutcome) : HermesProcessingDecisionOutcome()
}

/** The typed outcome of [HermesProcessingDecisionCoordinator.listPendingForOwner]. */
sealed class HermesProcessingReviewListOutcome {
    data class Found(val items: List<HermesProcessingReviewItem>) : HermesProcessingReviewListOutcome()
    data class Denied(val decision: PermissionDecisionOutcome) : HermesProcessingReviewListOutcome()
}
