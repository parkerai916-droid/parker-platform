package parker.core.runtime

import java.time.Instant
import java.util.UUID
import parker.core.interfaces.AcceptedEvidenceArtifact
import parker.core.interfaces.CandidateEvidenceArtifact
import parker.core.interfaces.EvidenceAcceptanceResult
import parker.core.interfaces.EvidenceArtifactId
import parker.core.interfaces.EvidenceArtifactStorage
import parker.core.interfaces.EvidenceCustodian
import parker.core.interfaces.EvidenceRetrievalResult
import parker.core.interfaces.ExecutionRequest
import parker.core.interfaces.PermissionDecisionOutcome
import parker.core.interfaces.PermissionEngine
import parker.core.interfaces.PrincipalId
import parker.core.interfaces.RequestId
import parker.core.interfaces.RequestOrigin
import parker.core.interfaces.RequestPriority
import parker.core.interfaces.ResourceId

/**
 * Evidence Custodian, Implementation Plan Phase 3 ("Governed acceptance
 * path"), Unit 2. The sole implementation of [EvidenceCustodian], composed
 * from two already-existing, unmodified dependencies: [storage]
 * (`EvidenceArtifactStorage`, Unit 1, Phase 2) and [permissionEngine] (the
 * existing `PermissionEngine` interface). See [EvidenceCustodian]'s own
 * file (`src/interfaces/EvidenceCustodian.kt`) for the full Unit 2
 * rationale -- this class implements exactly what that file's KDoc
 * describes, and does not restate it here beyond what is specific to this
 * implementation.
 *
 * ## Sequence
 *
 * 1. Build an [ExecutionRequest] naming this Unit's own well-known,
 *    disclosed [EVIDENCE_INTAKE_RESOURCE_ID]/[ACCEPT_ACTION_NAME]
 *    conventions (see [EvidenceCustodian]'s own KDoc for why neither is
 *    registered anywhere by this Unit) and call
 *    [PermissionEngine.evaluate].
 * 2. If the decision is not `APPROVED` or `APPROVED_WITH_CONFIRMATION`,
 *    return [EvidenceAcceptanceResult.Rejected] immediately -- **nothing
 *    is minted and nothing is written**. Acceptance never occurs
 *    implicitly, silently, or as a side effect of a denied proposal.
 * 3. Otherwise, mint a fresh [EvidenceArtifactId] (see "Identifier
 *    minting," below) and call [EvidenceArtifactStorage.write]. A storage
 *    failure (`EvidenceArtifactStorageException`, any subtype) propagates
 *    unchanged -- this class neither catches nor reinterprets it, per this
 *    Unit's own "storage and acceptance remain separate responsibilities"
 *    instruction.
 * 4. Only once [EvidenceArtifactStorage.write] returns successfully is
 *    [AcceptedEvidenceArtifact.acceptedAt] read (`Instant.now()`, once)
 *    and [EvidenceAcceptanceResult.Accepted] returned.
 *
 * ## Identifier minting -- a random value, not this repository's usual
 * incrementing-sequence convention, and why
 *
 * Every other identifier-minting Unit in this repository
 * (`InMemoryMemoryCore`'s `"document-${nextDocumentSequence++}"` and its
 * four siblings) uses an in-memory, monotonically-increasing counter. That
 * convention is safe there because the store the counter protects is
 * *itself* in-memory only -- a process restart resets the counter and the
 * store together, so no collision is possible.
 *
 * That precondition does not hold here. [storage] may be
 * `FileSystemEvidenceArtifactStorage`, which is durable across process
 * restarts, while an in-memory sequence counter in this class would not
 * be. A restarted process reusing a from-scratch counter starting at `1`
 * could collide with an identifier a *previous* process run already wrote
 * to disk -- not a theoretical concern, a direct consequence of pairing
 * ephemeral counter state with durable storage. This class mints
 * identifiers from [UUID.randomUUID] instead: a `java.util.UUID`'s own
 * string form (`toString()`) is already lowercase hexadecimal and
 * hyphens only, satisfying `EvidenceArtifactStorage`'s own canonical-identifier
 * pattern (`^[a-z0-9_-]+$`) and never colliding with any Windows reserved
 * device name (far longer than any reserved form), without requiring any
 * new persisted counter state -- state this Unit is not authorised to
 * introduce (no new storage implementation). `EvidenceArtifactStorage.write`'s
 * own existing `DuplicateIdentifier` guard remains as a genuine,
 * meaningful backstop against the astronomically unlikely case of a UUID
 * collision, rather than as the primary uniqueness mechanism a
 * restart-unsafe counter would have made it.
 */
class DefaultEvidenceCustodian(
    private val storage: EvidenceArtifactStorage,
    private val permissionEngine: PermissionEngine,
) : EvidenceCustodian {

    override suspend fun accept(
        requestingPrincipalId: PrincipalId,
        candidate: CandidateEvidenceArtifact,
    ): EvidenceAcceptanceResult {
        val decision = permissionEngine.evaluate(
            buildExecutionRequest(
                requestingPrincipalId = requestingPrincipalId,
                resourceId = EVIDENCE_INTAKE_RESOURCE_ID,
                actionName = ACCEPT_ACTION_NAME,
                intent = "Accept evidence artifact into Evidence Custodian custody",
                requestIdPrefix = "evidence-accept",
            ),
        )

        if (decision.decision != PermissionDecisionOutcome.APPROVED &&
            decision.decision != PermissionDecisionOutcome.APPROVED_WITH_CONFIRMATION
        ) {
            return EvidenceAcceptanceResult.Rejected(
                "Permission Engine did not authorise evidence acceptance for principal " +
                    "'${requestingPrincipalId.value}' (decision=${decision.decision})",
            )
        }

        val evidenceArtifactId = EvidenceArtifactId("evidence-${UUID.randomUUID()}")
        storage.write(evidenceArtifactId, candidate.content)

        return EvidenceAcceptanceResult.Accepted(
            AcceptedEvidenceArtifact(evidenceArtifactId, acceptedAt = Instant.now()),
        )
    }

    /**
     * Evidence Custodian, Implementation Plan Phase 4 ("Retrieval interface
     * behaviour"), Unit 3. See [EvidenceCustodian]'s own file KDoc for the
     * full Unit 3 rationale -- this method implements exactly what that
     * file's KDoc describes.
     *
     * ## Sequence
     *
     * 1. Build an [ExecutionRequest] naming this Unit's own well-known,
     *    disclosed [EVIDENCE_RETRIEVAL_RESOURCE_ID]/[RETRIEVE_ACTION_NAME]
     *    conventions and call [PermissionEngine.evaluate] -- **before any
     *    storage access of any kind.**
     * 2. If the decision is not `APPROVED` or `APPROVED_WITH_CONFIRMATION`,
     *    return [EvidenceRetrievalResult.Rejected] immediately. `storage.read`
     *    is never called on this path -- a denied request produces no
     *    storage interaction whatsoever.
     * 3. Otherwise, call `EvidenceArtifactStorage.read(evidenceArtifactId)`.
     *    A `null` result becomes [EvidenceRetrievalResult.NotFound]; a
     *    non-null result becomes [EvidenceRetrievalResult.Found], carrying
     *    back exactly the [evidenceArtifactId] the caller supplied and
     *    exactly the bytes storage returned, unmodified. A thrown
     *    `EvidenceArtifactStorageException` (any subtype) propagates
     *    unchanged -- this method neither catches nor reinterprets it as
     *    `NotFound` or any other result, per this Unit's own "do not hide
     *    genuine storage failures" instruction.
     *
     * This method mints no identifier ([evidenceArtifactId] is
     * caller-supplied, already-existing), writes nothing, and reads no
     * field of [AcceptedEvidenceArtifact] or any other acceptance-side
     * state -- it is, in its entirety, one permission check and one
     * storage read.
     */
    override suspend fun retrieve(
        requestingPrincipalId: PrincipalId,
        evidenceArtifactId: EvidenceArtifactId,
    ): EvidenceRetrievalResult {
        val decision = permissionEngine.evaluate(
            buildExecutionRequest(
                requestingPrincipalId = requestingPrincipalId,
                resourceId = EVIDENCE_RETRIEVAL_RESOURCE_ID,
                actionName = RETRIEVE_ACTION_NAME,
                intent = "Retrieve evidence artifact from Evidence Custodian custody",
                requestIdPrefix = "evidence-retrieve",
            ),
        )

        if (decision.decision != PermissionDecisionOutcome.APPROVED &&
            decision.decision != PermissionDecisionOutcome.APPROVED_WITH_CONFIRMATION
        ) {
            return EvidenceRetrievalResult.Rejected(
                "Permission Engine did not authorise evidence retrieval for principal " +
                    "'${requestingPrincipalId.value}' (decision=${decision.decision})",
            )
        }

        val content = storage.read(evidenceArtifactId)
            ?: return EvidenceRetrievalResult.NotFound(evidenceArtifactId)

        return EvidenceRetrievalResult.Found(evidenceArtifactId, content)
    }

    /**
     * Shared [ExecutionRequest] construction for both [accept] and
     * [retrieve] -- the two operations differ only in which disclosed,
     * unregistered convention constants and intent string they supply,
     * never in the shape of the request itself. See [EvidenceCustodian]'s
     * own file KDoc for what a future runtime-composition Unit (Phase 10)
     * must register for a real [PermissionEngine] to resolve either
     * convention to anything other than a conservative `DENIED`.
     */
    private fun buildExecutionRequest(
        requestingPrincipalId: PrincipalId,
        resourceId: ResourceId,
        actionName: String,
        intent: String,
        requestIdPrefix: String,
    ): ExecutionRequest {
        val now = Instant.now()
        return ExecutionRequest(
            requestId = RequestId("$requestIdPrefix-${UUID.randomUUID()}"),
            principalId = requestingPrincipalId,
            origin = RequestOrigin.REMOTE_INTERFACE,
            intent = intent,
            targetResources = listOf(resourceId),
            proposedActions = listOf(actionName),
            priority = RequestPriority.NORMAL,
            createdAt = now,
            correlationId = "$requestIdPrefix-${UUID.randomUUID()}",
        )
    }

    companion object {
        /**
         * A fixed, well-known [ResourceId] this Unit's own [ExecutionRequest]
         * always names as its target -- not a per-artefact identity (an
         * artefact does not have Resource-registry identity anywhere in
         * this Unit; its only identity is [EvidenceArtifactId]), but a
         * single, stable stand-in for "the evidence intake capability
         * itself," analogous to a fixed endpoint rather than a
         * per-request resource. Not registered anywhere by this Unit --
         * see [EvidenceCustodian]'s own KDoc.
         */
        val EVIDENCE_INTAKE_RESOURCE_ID: ResourceId = ResourceId("evidence-custodian-intake")

        /**
         * A fixed proposed-action name a future `ActionVocabulary`
         * registration is expected to map to
         * `(PermissionAction.WRITE, ResourceType.DOCUMENT)`. Not
         * registered anywhere by this Unit.
         */
        const val ACCEPT_ACTION_NAME: String = "evidence.accept"

        /**
         * Evidence Custodian, Implementation Plan Phase 4, Unit 3: the
         * retrieval counterpart to [EVIDENCE_INTAKE_RESOURCE_ID] -- a
         * distinct literal value, never reused from acceptance's own
         * convention, since acceptance and retrieval remain separately
         * gated proposals (Scope Lock Section 7: "Analytical access ...
         * requires Permission Engine authorisation," stated independently
         * of acceptance's own gating). Not registered anywhere by this
         * Unit -- see [EvidenceCustodian]'s own KDoc. A real
         * `DefaultPermissionPolicy` will deny every retrieval request
         * through this path until a future Phase 10 ("Runtime
         * integration") Unit registers a Resource under this identifier;
         * that registration is not performed here.
         */
        val EVIDENCE_RETRIEVAL_RESOURCE_ID: ResourceId = ResourceId("evidence-custodian-retrieval")

        /**
         * Evidence Custodian, Implementation Plan Phase 4, Unit 3: the
         * retrieval counterpart to [ACCEPT_ACTION_NAME]. A future
         * `ActionVocabulary` registration is expected to map this name to
         * a resolvable `(PermissionAction, ResourceType)` pair -- likely
         * `(PermissionAction.READ, ResourceType.DOCUMENT)`, though this
         * Unit does not decide that mapping, since deciding it is itself
         * Phase 10 runtime-composition work. Not registered anywhere by
         * this Unit.
         */
        const val RETRIEVE_ACTION_NAME: String = "evidence.retrieve"
    }
}
