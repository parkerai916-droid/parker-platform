package parker.composition

import java.time.Instant
import java.util.UUID
import parker.core.interfaces.Assertion
import parker.core.interfaces.CandidateAssertion
import parker.core.interfaces.CandidateDocument
import parker.core.interfaces.CandidateEntity
import parker.core.interfaces.CandidateProvenance
import parker.core.interfaces.CandidateRelationship
import parker.core.interfaces.Document
import parker.core.interfaces.Entity
import parker.core.interfaces.ExecutionRequest
import parker.core.interfaces.MemoryCore
import parker.core.interfaces.MemoryCoreRecord
import parker.core.interfaces.MemoryCoreRecordReference
import parker.core.interfaces.MemoryCoreRecordStatus
import parker.core.interfaces.PermissionDecision
import parker.core.interfaces.PermissionDecisionOutcome
import parker.core.interfaces.PermissionEngine
import parker.core.interfaces.PrincipalId
import parker.core.interfaces.Provenance
import parker.core.interfaces.Relationship
import parker.core.interfaces.RequestId
import parker.core.interfaces.RequestOrigin
import parker.core.interfaces.RequestPriority

/**
 * Programme 2, Memory Core, Implementation Unit 10 ("Runtime composition",
 * `docs/implementation/MEMORY_CORE_IMPLEMENTATION_PLAN.md` Section 7;
 * unblocked by `docs/architecture/MEMORY_CORE_CONTRACT_DESIGN_ERRATA_004.md`,
 * "the interface amendment," Sections 4 and 9). The write-side permission
 * gate: a transparent [MemoryCore] decorator that evaluates a real
 * [PermissionEngine] decision before every write and delegates only on
 * `APPROVED`/`APPROVED_WITH_CONFIRMATION` -- never on any other outcome,
 * and never after. Wraps [MemoryCore] via delegation exactly as
 * [EventPublishingMemoryCore] already wraps `InMemoryMemoryCore` in this
 * same package, without modifying `InMemoryMemoryCore` or the frozen
 * [MemoryCore] contract itself (`MEMORY_CORE_SCOPE_LOCK.md` Section 6:
 * "No contract implementing `MemoryCore`... may hold a `PermissionEngine`
 * reference").
 *
 * ## Composition order: wraps `EventPublishingMemoryCore`, not `InMemoryMemoryCore` directly
 *
 * Errata 004 Section 9 freezes this class as wrapping "a `MemoryCore`
 * delegate (in the frozen stack, `EventPublishingMemoryCore`) exactly as
 * `EventPublishingMemoryCore` itself wraps `InMemoryMemoryCore` --
 * unchanged from Unit 10's own original instructions, now unblocked." A
 * denied call therefore never reaches `EventPublishingMemoryCore` either
 * (Errata 004 Section 9: "no event is published either... since
 * `EventPublishingMemoryCore` sits *below* `PermissionGatedMemoryCore` in
 * the frozen stack and is never reached for a denied call"). This class's
 * own code is agnostic to which concrete [MemoryCore] it wraps -- the
 * frozen stack is a composition-time (`ParkerRuntime.kt`) decision, not
 * something this class enforces structurally.
 *
 * ## Every write operation, gated
 *
 * [MemoryCore] declares six write operations: five creation/registration
 * operations (`createProvenance`, `createEntity`, `registerDocument`,
 * `createAssertion`, `createRelationship`) and one generic lifecycle
 * operation (`transitionStatus`). Every one is gated -- Scope Lock Section
 * 6: "This applies to every operation `MemoryCore`... expose[s], without
 * exception." `transitionStatus` distinguishes exactly one thing, per
 * Errata 004 Section 7's own frozen mapping table: whether
 * [MemoryCoreRecordStatus.DELETED] is the requested target
 * (`PermissionAction.DELETE`, "reserved specifically for the
 * owner-requested erasure path") or any other target status
 * (`PermissionAction.WRITE`, covering dispute recorded/resolved/withdrawn,
 * supersession, archival, and unarchival alike).
 *
 * ## Resource representation: always `emptyList()` -- Errata 004 Section 7
 *
 * Memory Core records are never entries in the Resource Registry (Errata
 * 004 Section 7: "Memory Core has always been its own subsystem, never
 * wired to `ResourceRegistry`"), so `ExecutionRequest.targetResources` is
 * always `emptyList()` here -- there is never a Resource Registry entry to
 * name, for a not-yet-created record or an already-existing one alike.
 * `proposedActions` carries exactly one descriptive string per operation,
 * chosen from Errata 004 Section 7's own frozen table (below), never a
 * disclosed [parker.core.interfaces.ResourceId] of the kind
 * [parker.core.runtime.DefaultKnowledgeSubmission]/
 * [parker.core.runtime.DefaultEvidenceCustodian] use -- that convention
 * depends on a future Resource Registry registration; Memory Core's own
 * governance has already, explicitly, ruled that out for these checks.
 *
 * **Disclosed consequence, not a defect (Errata 004 Section 7's own
 * words):** routing these checks through the exact, already-wired,
 * production `DefaultPermissionPolicy` instance -- the one this class is
 * composed with today -- denies every Memory Core write unconditionally,
 * because that policy's `resourceTypes` set is always empty when
 * `targetResources` is empty, regardless of any [proposedActions] string
 * supplied. Correctly *approving* any of these operations requires a
 * future, dedicated Runtime-composition wiring decision (Errata 004
 * Section 7's own final paragraph) supplying a policy capable of deciding
 * `(WRITE, MEMORY)`, `(WRITE, DOCUMENT)`, and `(DELETE, MEMORY/DOCUMENT)`
 * without a resolved target Resource -- not a change to this class, to
 * [MemoryCore], or to `PermissionEngine`'s interface, and explicitly out
 * of this Unit's own scope (this Unit composes the existing, shared
 * engine only).
 *
 * ## Denial semantics: throws -- Errata 004 Section 9
 *
 * "On `DENIED`/`DEFERRED`, throws rather than reaching the delegate at
 * all (mirroring this Programme's own established 'hard failure for a
 * rejected precondition, never a silently-produced partial record'
 * discipline)." [MemoryCore]'s own frozen return types (`Entity`,
 * `Document`, `Assertion`, `Relationship`, `Provenance`,
 * `MemoryCoreRecord`) carry no denied/rejected variant, and this Unit may
 * not add one -- a thrown [MemoryCoreWriteDeniedException], not a
 * modification to [MemoryCore], is Errata 004's own explicit, frozen
 * resolution, mirroring this same interface's own existing KDoc
 * precedent for `transitionStatus` itself ("an invalid transition
 * rejected there as a thrown `IllegalStateException`... never as a
 * sealed result alongside a legitimate outcome").
 *
 * ## Fault propagation
 *
 * A genuine implementation fault thrown by the wrapped [delegate] (for
 * example, `IllegalStateException` for an invalid lifecycle transition)
 * propagates completely unchanged -- this class catches nothing the
 * delegate throws, mirroring [EventPublishingMemoryCore]'s own identical
 * "the delegate's own exception propagates unchanged" discipline. A
 * genuine fault thrown by [permissionEngine] itself also propagates
 * unchanged and is never caught or reinterpreted as a denial -- only a
 * [PermissionDecision] whose `decision` is not `APPROVED`/
 * `APPROVED_WITH_CONFIRMATION` is treated as a denial.
 */
class PermissionGatedMemoryCore(
    private val delegate: MemoryCore,
    private val permissionEngine: PermissionEngine,
) : MemoryCore {

    override suspend fun createProvenance(requestingPrincipalId: PrincipalId, candidate: CandidateProvenance): Provenance {
        requireApproved(requestingPrincipalId, CREATE_PROVENANCE_ACTION_NAME)
        return delegate.createProvenance(requestingPrincipalId, candidate)
    }

    override suspend fun createEntity(requestingPrincipalId: PrincipalId, candidate: CandidateEntity): Entity {
        requireApproved(requestingPrincipalId, CREATE_ENTITY_ACTION_NAME)
        return delegate.createEntity(requestingPrincipalId, candidate)
    }

    override suspend fun registerDocument(requestingPrincipalId: PrincipalId, candidate: CandidateDocument): Document {
        requireApproved(requestingPrincipalId, REGISTER_DOCUMENT_ACTION_NAME)
        return delegate.registerDocument(requestingPrincipalId, candidate)
    }

    override suspend fun createAssertion(requestingPrincipalId: PrincipalId, candidate: CandidateAssertion): Assertion {
        requireApproved(requestingPrincipalId, CREATE_ASSERTION_ACTION_NAME)
        return delegate.createAssertion(requestingPrincipalId, candidate)
    }

    override suspend fun createRelationship(requestingPrincipalId: PrincipalId, candidate: CandidateRelationship): Relationship {
        requireApproved(requestingPrincipalId, CREATE_RELATIONSHIP_ACTION_NAME)
        return delegate.createRelationship(requestingPrincipalId, candidate)
    }

    override suspend fun transitionStatus(
        requestingPrincipalId: PrincipalId,
        reference: MemoryCoreRecordReference,
        targetStatus: MemoryCoreRecordStatus,
    ): MemoryCoreRecord {
        val actionName = if (targetStatus == MemoryCoreRecordStatus.DELETED) {
            DELETE_RECORD_ACTION_NAME
        } else {
            TRANSITION_STATUS_ACTION_NAME
        }
        requireApproved(requestingPrincipalId, actionName)
        return delegate.transitionStatus(requestingPrincipalId, reference, targetStatus)
    }

    /**
     * Evaluates permission exactly once, and either returns normally
     * (approved) or throws [MemoryCoreWriteDeniedException] (every other
     * outcome) -- always before the corresponding [delegate] call, never
     * after.
     */
    private suspend fun requireApproved(requestingPrincipalId: PrincipalId, actionName: String) {
        val decision = permissionEngine.evaluate(buildExecutionRequest(requestingPrincipalId, actionName))
        if (decision.decision != PermissionDecisionOutcome.APPROVED &&
            decision.decision != PermissionDecisionOutcome.APPROVED_WITH_CONFIRMATION
        ) {
            throw MemoryCoreWriteDeniedException(decision)
        }
    }

    /** Errata 004 Section 7: `targetResources` is always empty; `proposedActions` carries the one descriptive string. */
    private fun buildExecutionRequest(requestingPrincipalId: PrincipalId, actionName: String): ExecutionRequest {
        val id = UUID.randomUUID()
        return ExecutionRequest(
            requestId = RequestId("memory-core-write-$id"),
            principalId = requestingPrincipalId,
            origin = RequestOrigin.AGENT,
            intent = "Perform Memory Core write operation '$actionName'",
            targetResources = emptyList(),
            proposedActions = listOf(actionName),
            priority = RequestPriority.NORMAL,
            createdAt = Instant.now(),
            correlationId = "memory-core-write-$id",
        )
    }

    companion object {
        const val CREATE_PROVENANCE_ACTION_NAME: String = "memory.create_provenance"
        const val CREATE_ENTITY_ACTION_NAME: String = "memory.create_entity"
        const val CREATE_ASSERTION_ACTION_NAME: String = "memory.create_assertion"
        const val CREATE_RELATIONSHIP_ACTION_NAME: String = "memory.create_relationship"
        const val REGISTER_DOCUMENT_ACTION_NAME: String = "memory.register_document"

        /** Every non-deleting [MemoryCoreRecordStatus] transition (dispute, supersession, archival, unarchival). */
        const val TRANSITION_STATUS_ACTION_NAME: String = "memory.transition_status"

        /** Reserved exclusively for a transition targeting [MemoryCoreRecordStatus.DELETED] (Errata 004 Section 7, "Who deletes?"). */
        const val DELETE_RECORD_ACTION_NAME: String = "memory.delete_record"
    }
}

/**
 * Thrown by [PermissionGatedMemoryCore] when [PermissionEngine.evaluate]
 * returns any outcome other than `APPROVED`/`APPROVED_WITH_CONFIRMATION`
 * for a Memory Core write. See [PermissionGatedMemoryCore]'s own KDoc,
 * "Denial semantics," for why a thrown type -- not a returned sealed
 * result -- is the correct, Errata-004-frozen resolution here:
 * [MemoryCore]'s frozen return types carry no denied/rejected variant,
 * and this Unit may not add one. [decision] is the full, unaltered
 * [PermissionDecision] the caller may inspect (via
 * [PermissionEngine.explain] against [PermissionDecision.decisionId]) to
 * learn why; its `resourceId` is always the policy's own "no target
 * resource" placeholder (Errata 004 Section 7 -- `targetResources` is
 * always empty for a Memory Core check), not a meaningful identifier.
 */
class MemoryCoreWriteDeniedException(val decision: PermissionDecision) : RuntimeException(
    "Memory Core write denied for principal '${decision.principalId.value}' (outcome=${decision.decision})",
)
