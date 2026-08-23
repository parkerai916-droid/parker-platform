package parker.core.runtime

import java.time.Instant
import java.util.UUID
import parker.core.interfaces.EvidenceArtifactId
import parker.core.interfaces.EvidenceArtifactStorage
import parker.core.interfaces.EvidenceDeletionAudit
import parker.core.interfaces.EvidenceDeletionAuditRecord
import parker.core.interfaces.EvidenceDeletionAuditStage
import parker.core.interfaces.EvidenceDeletionResult
import parker.core.interfaces.EvidenceSourceManifestStorage
import parker.core.interfaces.ExecutionRequest
import parker.core.interfaces.OwnerEvidenceDeletionAuthority
import parker.core.interfaces.PermissionDecisionOutcome
import parker.core.interfaces.PermissionEngine
import parker.core.interfaces.PrincipalId
import parker.core.interfaces.RequestId
import parker.core.interfaces.RequestOrigin
import parker.core.interfaces.RequestPriority
import parker.core.interfaces.ResourceId

/**
 * Evidence Custodian, Implementation Plan Phase 7 ("Deletion workflow").
 * The sole implementation of [OwnerEvidenceDeletionAuthority], governed
 * by `docs/architecture/EVIDENCE_CUSTODIAN_PHASE_7_BOUNDARY_CLARIFICATION.md`
 * ("the Boundary Clarification") in full. See that document's Sections
 * 3-7 for the complete rationale this class implements exactly; this
 * KDoc restates only what is specific to this implementation.
 *
 * ## Not a coordinator (Boundary Clarification Section 3)
 *
 * This class sequences a Permission Engine check and two calls to its
 * own dependencies ([storage], [evidenceDeletionAudit]) -- the same
 * shape [DefaultEvidenceCustodian.accept] already has. It never touches
 * a second, independently-governed subsystem the way
 * [EvidenceRegistrationCoordinator] sequences Evidence Custodian and
 * Memory Core -- there is no `MemoryCore` reference anywhere in this
 * class's constructor, structurally, not merely by omission.
 *
 * ## A separate class, not a second interface on [DefaultEvidenceCustodian]
 * (Boundary Clarification Section 3's reasoning applies symmetrically
 * here)
 *
 * [deleteAsOwner] needs a dependency ([evidenceDeletionAudit])
 * [DefaultEvidenceCustodian]'s own `accept`/`retrieve` never do;
 * implementing [OwnerEvidenceDeletionAuthority] on
 * [DefaultEvidenceCustodian] itself would force every existing and
 * future accept/retrieve-only consumer and test to also supply an
 * irrelevant audit dependency. [DefaultEvidenceCustodian] itself is
 * unmodified by this Unit.
 *
 * ## Ordering (Boundary Clarification Section 6) -- why no `try`/`catch`
 * appears anywhere in [deleteAsOwner]
 *
 * 1. [PermissionEngine.evaluate], first. Not approved ->
 *    [EvidenceDeletionResult.Rejected], with no further side effect.
 * 2. Approved -> durably write an [EvidenceDeletionAuditStage.AUTHORISED]
 *    record. If [EvidenceDeletionAudit.record] throws here, that
 *    exception propagates unchanged and [storage] is never reached --
 *    physical deletion never proceeds without durable evidence, already
 *    confirmed, that it was authorised.
 * 3. [EvidenceArtifactStorage.delete]. Nothing present ->
 *    [EvidenceDeletionResult.NotFound]; only the `AUTHORISED` record
 *    exists for this attempt. A genuine storage fault propagates
 *    unchanged; again, only the `AUTHORISED` record exists.
 * 4. Deletion succeeded -> durably write a second record, same
 *    [EvidenceDeletionAuditRecord.deletionRequestId], `stage =
 *    COMPLETED`. If this write throws, it propagates unchanged and
 *    [EvidenceDeletionResult.Deleted] is never returned -- the
 *    underlying bytes are already, irreversibly gone (Boundary
 *    Clarification Section 8 disclaims any rollback), but the caller is
 *    never told a durably-audited deletion succeeded unless it truly
 *    did.
 *
 * No exception is ever caught, reinterpreted, or hidden anywhere in this
 * class -- every fault above propagates to the caller exactly as
 * thrown, mirroring [DefaultEvidenceCustodian]'s own established
 * discipline.
 *
 * ## Two disclosed, unregistered conventions, exactly mirroring
 * [DefaultEvidenceCustodian]'s own precedent
 *
 * This class builds an [ExecutionRequest] naming a fixed, well-known
 * [ResourceId] ([EVIDENCE_DELETION_RESOURCE_ID]) and a fixed
 * proposed-action name ([DELETE_ACTION_NAME]). Neither is registered
 * anywhere by this Unit -- registering either, and deciding which
 * component in the production runtime is ever given a reference to this
 * class at all, is Implementation Plan Phase 10 ("Runtime integration")
 * work (Boundary Clarification Section 5), explicitly out of this
 * Unit's own scope. Until a future Phase 10 unit performs that
 * registration, a real `DefaultPermissionPolicy` denies every request
 * through this path -- the same safe, conservative failure mode
 * [DefaultEvidenceCustodian]'s own two operations are already in today.
 */
class DefaultOwnerEvidenceDeletionAuthority(
    private val storage: EvidenceArtifactStorage,
    private val permissionEngine: PermissionEngine,
    private val evidenceDeletionAudit: EvidenceDeletionAudit,
    private val manifestStorage: EvidenceSourceManifestStorage = InMemoryEvidenceSourceManifestStorage(),
) : OwnerEvidenceDeletionAuthority {

    override suspend fun deleteAsOwner(
        requestingPrincipalId: PrincipalId,
        evidenceArtifactId: EvidenceArtifactId,
    ): EvidenceDeletionResult {
        val decision = permissionEngine.evaluate(buildExecutionRequest(requestingPrincipalId, evidenceArtifactId))

        if (decision.decision != PermissionDecisionOutcome.APPROVED &&
            decision.decision != PermissionDecisionOutcome.APPROVED_WITH_CONFIRMATION
        ) {
            return EvidenceDeletionResult.Rejected(
                "Permission Engine did not authorise evidence deletion for principal " +
                    "'${requestingPrincipalId.value}' (decision=${decision.decision})",
            )
        }

        val deletionRequestId = "evidence-deletion-${UUID.randomUUID()}"
        evidenceDeletionAudit.record(
            EvidenceDeletionAuditRecord(
                deletionRequestId = deletionRequestId,
                evidenceArtifactId = evidenceArtifactId,
                requestingPrincipalId = requestingPrincipalId,
                recordedAt = Instant.now(),
                stage = EvidenceDeletionAuditStage.AUTHORISED,
            ),
        )

        val deleted = storage.delete(evidenceArtifactId)

        // Authoritative Source Manifest Foundation Implementation, Scope Lock Section 19: the
        // active manifest must not survive as a phantom authority once its source bytes are gone.
        // Called unconditionally -- regardless of whether storage.delete found anything this call
        // -- so that a manifest orphaned by an earlier attempt (for example, one whose own
        // manifestStorage.delete previously failed after storage.delete had already succeeded) is
        // still cleaned up on a later retry, even though that retry's own storage.delete now
        // truthfully returns false. No tombstone is written -- manifestStorage.delete physically
        // removes the manifest with no retained marker, exactly like storage.delete itself, and is
        // itself an ordinary no-op (false) if nothing was present. A genuine I/O fault here
        // propagates unchanged, uncaught, mirroring every other fault in this sequence.
        manifestStorage.delete(evidenceArtifactId)

        if (!deleted) {
            return EvidenceDeletionResult.NotFound(evidenceArtifactId)
        }

        evidenceDeletionAudit.record(
            EvidenceDeletionAuditRecord(
                deletionRequestId = deletionRequestId,
                evidenceArtifactId = evidenceArtifactId,
                requestingPrincipalId = requestingPrincipalId,
                recordedAt = Instant.now(),
                stage = EvidenceDeletionAuditStage.COMPLETED,
            ),
        )

        return EvidenceDeletionResult.Deleted(evidenceArtifactId)
    }

    private fun buildExecutionRequest(
        requestingPrincipalId: PrincipalId,
        evidenceArtifactId: EvidenceArtifactId,
    ): ExecutionRequest {
        val now = Instant.now()
        return ExecutionRequest(
            requestId = RequestId("evidence-delete-${UUID.randomUUID()}"),
            principalId = requestingPrincipalId,
            origin = RequestOrigin.REMOTE_INTERFACE,
            intent = "Delete evidence artifact '${evidenceArtifactId.value}' from Evidence Custodian custody",
            targetResources = listOf(EVIDENCE_DELETION_RESOURCE_ID),
            proposedActions = listOf(DELETE_ACTION_NAME),
            priority = RequestPriority.NORMAL,
            createdAt = now,
            correlationId = "evidence-delete-${UUID.randomUUID()}",
        )
    }

    companion object {
        /**
         * A fixed, well-known [ResourceId] this class's own
         * [ExecutionRequest] always names as its target -- not
         * registered anywhere by this Unit. See this class's own KDoc,
         * "Two disclosed, unregistered conventions."
         */
        val EVIDENCE_DELETION_RESOURCE_ID: ResourceId = ResourceId("evidence-custodian-deletion")

        /**
         * A fixed proposed-action name a future `ActionVocabulary`
         * registration is expected to map to a resolvable
         * `(PermissionAction.DELETE, ResourceType)` pair -- deciding the
         * exact `ResourceType` is itself Phase 10 runtime-composition
         * work, not decided here. Not registered anywhere by this Unit.
         */
        const val DELETE_ACTION_NAME: String = "evidence.delete"
    }
}
