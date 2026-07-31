package parker.core.runtime

import java.time.Instant
import java.util.UUID
import parker.core.interfaces.AcceptedEvidenceArtifact
import parker.core.interfaces.CandidateDocument
import parker.core.interfaces.CandidateEvidenceArtifact
import parker.core.interfaces.CandidateProvenance
import parker.core.interfaces.Document
import parker.core.interfaces.EvidenceAcceptanceResult
import parker.core.interfaces.EvidenceCustodian
import parker.core.interfaces.ExecutionRequest
import parker.core.interfaces.MemoryCore
import parker.core.interfaces.PermissionDecisionOutcome
import parker.core.interfaces.PermissionEngine
import parker.core.interfaces.PrincipalId
import parker.core.interfaces.Provenance
import parker.core.interfaces.RequestId
import parker.core.interfaces.RequestOrigin
import parker.core.interfaces.RequestPriority
import parker.core.interfaces.ResourceId

/**
 * The result of [EvidenceRegistrationCoordinator.register], per the approved
 * "Runtime Evidence Registration Coordinator" architectural review, final
 * permission-boundary review, and final implementation planning review.
 * Sealed, `class` not `interface`, matching this repository's own exclusive
 * existing convention for outcome types (`GoalPlanningHandoffOutcome`,
 * `GatedOutcome`, `ParkerRuntimeOutcome`; none is a `sealed interface`).
 *
 * Every variant reuses an existing type unchanged rather than re-deriving
 * new fields -- [Registered] bundles exactly the three already-produced
 * values by reference; [NotAccepted] wraps [EvidenceAcceptanceResult.Rejected]
 * unchanged; the two permission-denial variants carry the already-durable
 * facts that preceded the denial ([AcceptedEvidenceArtifact], and
 * [Provenance] where it already exists), so a caller is never told less
 * than what has actually, truthfully happened by this point in the
 * sequence.
 */
sealed class EvidenceRegistrationOutcome {

    /**
     * `accept`, `createProvenance`, and `registerDocument` all succeeded,
     * in that order. Carries all three already-produced values unchanged.
     */
    data class Registered(
        val acceptedEvidenceArtifact: AcceptedEvidenceArtifact,
        val provenance: Provenance,
        val document: Document,
    ) : EvidenceRegistrationOutcome()

    /**
     * [EvidenceCustodian.accept] itself was not authorised.
     * [MemoryCore] is never called on this branch. [rejection] is
     * [EvidenceAcceptanceResult.Rejected] returned by `accept` itself,
     * carried unchanged -- this coordinator never re-decides or
     * re-authors Evidence Custodian's own decision.
     */
    data class NotAccepted(val rejection: EvidenceAcceptanceResult.Rejected) : EvidenceRegistrationOutcome()

    /**
     * Evidence was accepted, but this coordinator's own permission
     * decision for `MemoryCore.createProvenance` was not approved.
     * `createProvenance` and `registerDocument` are never called.
     * [acceptedEvidenceArtifact] is carried so a caller is not told less
     * than the true, already-durable state: the artifact remains
     * genuinely, durably custodied even though nothing was registered.
     */
    data class ProvenanceNotAuthorised(
        val acceptedEvidenceArtifact: AcceptedEvidenceArtifact,
        val reason: String,
    ) : EvidenceRegistrationOutcome()

    /**
     * Evidence was accepted and a [Provenance] record was durably created,
     * but this coordinator's own permission decision for
     * `MemoryCore.registerDocument` was not approved. `registerDocument`
     * is never called. Both [acceptedEvidenceArtifact] and [provenance]
     * are carried, since both already, truthfully, exist -- the
     * [Provenance] record remains, durably, unreferenced by any Document
     * (Memory Core's own history is append-only; nothing here attempts to
     * roll it back, since no such operation exists or is authorised).
     */
    data class DocumentRegistrationNotAuthorised(
        val acceptedEvidenceArtifact: AcceptedEvidenceArtifact,
        val provenance: Provenance,
        val reason: String,
    ) : EvidenceRegistrationOutcome()
}

/**
 * Sequences [EvidenceCustodian.accept], [MemoryCore.createProvenance], and
 * [MemoryCore.registerDocument] -- the Runtime Evidence Registration
 * Coordinator approved by this Unit's own architectural review (Evidence
 * Custodian and Memory Core must remain independent; coordination belongs
 * in the Runtime layer), final permission-boundary review (separate
 * Permission Engine evaluations for `createProvenance` and
 * `registerDocument`, since a single [parker.core.interfaces.PermissionDecision]
 * cannot truthfully represent two independently-authorised acts), and
 * final implementation planning review (this exact dependency list, call
 * order, and outcome shape).
 *
 * **Neither [EvidenceCustodian] nor [MemoryCore] holds a reference to the
 * other, or to this class.** [evidenceCustodian], [memoryCore], and
 * [permissionEngine] are this class's only three dependencies -- the
 * absence of any other constructor parameter is itself the structural
 * guarantee that this class cannot reach anything else, the same
 * convention every other coordinator in this codebase already uses its own
 * dependency list to prove ([GoalPlanningHandoffCoordinator],
 * [ReplyDeliveryCoordinator]).
 *
 * ## Why this coordinator holds a [PermissionEngine] reference, unlike every
 * other existing coordinator
 *
 * [EvidenceCustodian.accept] already gates itself internally
 * (`DefaultEvidenceCustodian`, Unit 2) -- this coordinator never
 * re-evaluates or duplicates that decision. `MemoryCore` cannot self-gate
 * (Memory Core Scope Lock Section 6: "Memory Core never evaluates
 * permissions... No contract implementing `MemoryCore` may hold a
 * `PermissionEngine` reference"), so nothing gates [createProvenance]/
 * [registerDocument] unless this coordinator does. This is the one
 * documented, load-bearing reason this coordinator's dependency list
 * differs from every other existing coordinator's.
 *
 * ## Two separate permission decisions, not one
 *
 * The final permission-boundary review found that
 * [parker.core.interfaces.PermissionDecision] carries exactly one
 * `resourceId`, one `action`, and one `decision` outcome -- it cannot
 * truthfully represent "provenance creation approved, document
 * registration denied" as a single value. [register] therefore evaluates
 * two independent [ExecutionRequest]s, using two distinct, disclosed,
 * unregistered resource/action conventions
 * ([MEMORY_CORE_PROVENANCE_RESOURCE_ID]/[CREATE_PROVENANCE_ACTION_NAME] and
 * [MEMORY_CORE_DOCUMENT_REGISTRATION_RESOURCE_ID]/[REGISTER_DOCUMENT_ACTION_NAME]),
 * mirroring `DefaultEvidenceCustodian`'s own `accept`/`retrieve` split
 * exactly. Neither is registered anywhere by this Unit -- registering
 * either is Implementation Plan Phase 10 ("Runtime integration") work.
 *
 * ## Call order: `createProvenance` before `registerDocument`
 *
 * `MemoryCore.registerDocument`'s own `CandidateDocument.provenanceId` is
 * mandatory and validated against an already-existing `Provenance` record
 * (`InMemoryMemoryCore.requireExistingProvenance`) -- `createProvenance`
 * must succeed, and its `ProvenanceId` must be obtained, before
 * `registerDocument` can be attempted at all. This is not a design
 * preference; it is the only order the existing, frozen `MemoryCore`
 * contract permits.
 *
 * ## Correlation, and its one disclosed limit
 *
 * [correlationId] is reused, unchanged, as the `correlationId` of both
 * [ExecutionRequest]s this class builds -- mirroring
 * [GoalPlanningHandoffCoordinator]'s own "one unwrap only, reuse, never
 * mint a second identifier" precedent. It is **not**, and cannot be,
 * threaded into [EvidenceCustodian.accept]'s own internal permission
 * decision: `accept`'s signature carries no `correlationId` parameter, and
 * its own `ExecutionRequest.correlationId` is self-minted
 * (`DefaultEvidenceCustodian`) -- unreachable from outside, by design. The
 * link between `accept`'s own decision and this coordinator's two decisions
 * is therefore by data, not by a shared correlation tag: the
 * `evidenceArtifactId` `accept` returns becomes exactly
 * `CandidateDocument.locationReference`, permanently recorded on the
 * registered [Document].
 *
 * ## `documentType`, the one new caller-supplied input this Unit requires
 *
 * `CandidateDocument.documentType` is a mandatory field with no derivable
 * substitute anywhere in [CandidateEvidenceArtifact] or
 * [CandidateProvenance] -- [register] therefore accepts it directly as a
 * parameter, the smallest addition this sequencing requires. [register]
 * invents no other field: [CandidateDocument.locationReference] and
 * [CandidateDocument.provenanceId] are derived, never caller-supplied,
 * from [EvidenceCustodian.accept]'s and [MemoryCore.createProvenance]'s own
 * results.
 *
 * ## Failure behaviour
 *
 * No `try`/`catch` anywhere in [register]. A denied permission decision is
 * an ordinary, non-exceptional result (mirroring
 * [EvidenceAcceptanceResult.Rejected]'s own precedent) -- returned as a
 * typed [EvidenceRegistrationOutcome] variant, never thrown. A genuine
 * exception thrown by [EvidenceCustodian.accept], [MemoryCore.createProvenance],
 * or [MemoryCore.registerDocument] propagates unchanged, exactly as
 * [GoalPlanningHandoffCoordinator]'s own "no try/catch, faults propagate to
 * caller" discipline. This coordinator never retries, and never attempts
 * to roll back or delete an already-accepted artifact or an already-created
 * Provenance record -- neither subsystem exposes such an operation, and
 * none is authorised.
 *
 * @param evidenceCustodian Called exactly once per [register] call, first.
 * @param memoryCore Called for [MemoryCore.createProvenance], then, only if
 *   that call's own governing permission decision approves and the call
 *   itself succeeds, [MemoryCore.registerDocument].
 * @param permissionEngine Evaluated exactly twice per [register] call that
 *   reaches Memory Core at all -- once for `createProvenance`, once for
 *   `registerDocument`. Never evaluated for `accept`, which gates itself.
 */
class EvidenceRegistrationCoordinator(
    private val evidenceCustodian: EvidenceCustodian,
    private val memoryCore: MemoryCore,
    private val permissionEngine: PermissionEngine,
) {

    /**
     * Runs the full sequence documented on this class's own KDoc:
     * [EvidenceCustodian.accept] -> (gated) [MemoryCore.createProvenance] ->
     * (gated) [MemoryCore.registerDocument] -> [EvidenceRegistrationOutcome].
     * Returns as soon as any stage does not reach an authorised, successful
     * continuation -- see each [EvidenceRegistrationOutcome] variant's own
     * KDoc for exactly what has, and has not, happened on that branch.
     *
     * @param requestingPrincipalId Passed unchanged to every downstream call
     *   ([EvidenceCustodian.accept], both [ExecutionRequest]s this class
     *   builds, [MemoryCore.createProvenance], [MemoryCore.registerDocument]).
     * @param correlationId Reused, unchanged, as both of this class's own
     *   [ExecutionRequest]s' `correlationId` -- see this class's own KDoc,
     *   "Correlation, and its one disclosed limit," for why this value
     *   cannot also reach `accept`'s own internal decision.
     * @param candidateEvidenceArtifact Passed unchanged to
     *   [EvidenceCustodian.accept].
     * @param candidateProvenance Passed unchanged to
     *   [MemoryCore.createProvenance].
     * @param documentType Passed unchanged as the resulting
     *   [CandidateDocument.documentType] -- see this class's own KDoc,
     *   "`documentType`, the one new caller-supplied input this Unit
     *   requires."
     * @param documentIntegrityHash Passed unchanged as the resulting
     *   [CandidateDocument.integrityHash]. Defaults to `null`, matching
     *   [CandidateDocument]'s own default.
     * @param documentMetadata Passed unchanged as the resulting
     *   [CandidateDocument.metadata]. Defaults to an empty map, matching
     *   [CandidateDocument]'s own default.
     */
    suspend fun register(
        requestingPrincipalId: PrincipalId,
        correlationId: String,
        candidateEvidenceArtifact: CandidateEvidenceArtifact,
        candidateProvenance: CandidateProvenance,
        documentType: String,
        documentIntegrityHash: String? = null,
        documentMetadata: Map<String, String> = emptyMap(),
    ): EvidenceRegistrationOutcome {
        val acceptanceResult = evidenceCustodian.accept(requestingPrincipalId, candidateEvidenceArtifact)
        val acceptedEvidenceArtifact = when (acceptanceResult) {
            is EvidenceAcceptanceResult.Rejected -> return EvidenceRegistrationOutcome.NotAccepted(acceptanceResult)
            is EvidenceAcceptanceResult.Accepted -> acceptanceResult.acceptedEvidenceArtifact
        }

        val provenanceDecision = permissionEngine.evaluate(
            buildExecutionRequest(
                requestingPrincipalId = requestingPrincipalId,
                correlationId = correlationId,
                resourceId = MEMORY_CORE_PROVENANCE_RESOURCE_ID,
                actionName = CREATE_PROVENANCE_ACTION_NAME,
                intent = "Create Memory Core provenance record for accepted evidence artifact " +
                    "'${acceptedEvidenceArtifact.evidenceArtifactId.value}'",
                requestIdPrefix = "evidence-registration-provenance",
            ),
        )
        if (provenanceDecision.decision != PermissionDecisionOutcome.APPROVED &&
            provenanceDecision.decision != PermissionDecisionOutcome.APPROVED_WITH_CONFIRMATION
        ) {
            return EvidenceRegistrationOutcome.ProvenanceNotAuthorised(
                acceptedEvidenceArtifact = acceptedEvidenceArtifact,
                reason = "Permission Engine did not authorise Memory Core provenance creation for principal " +
                    "'${requestingPrincipalId.value}' (decision=${provenanceDecision.decision})",
            )
        }

        val provenance = memoryCore.createProvenance(requestingPrincipalId, candidateProvenance)

        val documentDecision = permissionEngine.evaluate(
            buildExecutionRequest(
                requestingPrincipalId = requestingPrincipalId,
                correlationId = correlationId,
                resourceId = MEMORY_CORE_DOCUMENT_REGISTRATION_RESOURCE_ID,
                actionName = REGISTER_DOCUMENT_ACTION_NAME,
                intent = "Register Memory Core document for accepted evidence artifact " +
                    "'${acceptedEvidenceArtifact.evidenceArtifactId.value}'",
                requestIdPrefix = "evidence-registration-document",
            ),
        )
        if (documentDecision.decision != PermissionDecisionOutcome.APPROVED &&
            documentDecision.decision != PermissionDecisionOutcome.APPROVED_WITH_CONFIRMATION
        ) {
            return EvidenceRegistrationOutcome.DocumentRegistrationNotAuthorised(
                acceptedEvidenceArtifact = acceptedEvidenceArtifact,
                provenance = provenance,
                reason = "Permission Engine did not authorise Memory Core document registration for principal " +
                    "'${requestingPrincipalId.value}' (decision=${documentDecision.decision})",
            )
        }

        val candidateDocument = CandidateDocument(
            documentType = documentType,
            locationReference = acceptedEvidenceArtifact.evidenceArtifactId.value,
            provenanceId = provenance.provenanceId,
            integrityHash = documentIntegrityHash,
            metadata = documentMetadata,
        )
        val document = memoryCore.registerDocument(requestingPrincipalId, candidateDocument)

        return EvidenceRegistrationOutcome.Registered(
            acceptedEvidenceArtifact = acceptedEvidenceArtifact,
            provenance = provenance,
            document = document,
        )
    }

    /**
     * Shared [ExecutionRequest] construction for both of this class's own
     * permission evaluations -- mirroring
     * [DefaultEvidenceCustodian.buildExecutionRequest]'s own identical
     * shared-helper shape, with one difference: [correlationId] is a
     * parameter here, reused from the caller, never minted fresh (see this
     * class's own KDoc, "Correlation, and its one disclosed limit").
     */
    private fun buildExecutionRequest(
        requestingPrincipalId: PrincipalId,
        correlationId: String,
        resourceId: ResourceId,
        actionName: String,
        intent: String,
        requestIdPrefix: String,
    ): ExecutionRequest = ExecutionRequest(
        requestId = RequestId("$requestIdPrefix-${UUID.randomUUID()}"),
        principalId = requestingPrincipalId,
        origin = RequestOrigin.REMOTE_INTERFACE,
        intent = intent,
        targetResources = listOf(resourceId),
        proposedActions = listOf(actionName),
        priority = RequestPriority.NORMAL,
        createdAt = Instant.now(),
        correlationId = correlationId,
    )

    companion object {
        /**
         * A fixed, well-known [ResourceId] this coordinator's own
         * `createProvenance` gate always names as its target -- not
         * registered anywhere by this Unit (Implementation Plan Phase 10).
         */
        val MEMORY_CORE_PROVENANCE_RESOURCE_ID: ResourceId = ResourceId("memory-core-provenance")

        /** The `createProvenance` gate's proposed-action name. Not registered anywhere by this Unit. */
        const val CREATE_PROVENANCE_ACTION_NAME: String = "memory.create-provenance"

        /**
         * A fixed, well-known [ResourceId] this coordinator's own
         * `registerDocument` gate always names as its target -- a distinct
         * literal value from [MEMORY_CORE_PROVENANCE_RESOURCE_ID], since the
         * two Memory Core writes remain separately gated proposals (final
         * permission-boundary review). Not registered anywhere by this Unit.
         */
        val MEMORY_CORE_DOCUMENT_REGISTRATION_RESOURCE_ID: ResourceId = ResourceId("memory-core-document-registration")

        /** The `registerDocument` gate's proposed-action name. Not registered anywhere by this Unit. */
        const val REGISTER_DOCUMENT_ACTION_NAME: String = "memory.register-document"
    }
}
