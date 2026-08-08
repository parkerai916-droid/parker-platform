package parker.composition

import java.time.Instant
import java.util.UUID
import parker.core.interfaces.AssertionId
import parker.core.interfaces.Assertion
import parker.core.interfaces.AuthorizationPurposeId
import parker.core.interfaces.ChronologicalLookupQuery
import parker.core.interfaces.Document
import parker.core.interfaces.DocumentId
import parker.core.interfaces.DocumentLookupQuery
import parker.core.interfaces.Entity
import parker.core.interfaces.EntityId
import parker.core.interfaces.EntityLookupQuery
import parker.core.interfaces.ExecutionRequest
import parker.core.interfaces.MemoryCoreRecord
import parker.core.interfaces.MemoryRetrieval
import parker.core.interfaces.MetadataLookupQuery
import parker.core.interfaces.PermissionDecisionOutcome
import parker.core.interfaces.PermissionEngine
import parker.core.interfaces.PrincipalId
import parker.core.interfaces.ProvenanceLookupQuery
import parker.core.interfaces.Relationship
import parker.core.interfaces.RelationshipId
import parker.core.interfaces.RelationshipTraversalQuery
import parker.core.interfaces.RequestId
import parker.core.interfaces.RequestOrigin
import parker.core.interfaces.RequestPriority

/**
 * Programme 2, Memory Core, Implementation Unit 10 ("Runtime composition",
 * `docs/implementation/MEMORY_CORE_IMPLEMENTATION_PLAN.md` Section 7;
 * unblocked by `docs/architecture/MEMORY_CORE_CONTRACT_DESIGN_ERRATA_004.md`,
 * Sections 5, 6, 8, 9). The read-side permission gate: a transparent
 * [MemoryRetrieval] decorator evaluating a real [PermissionEngine]
 * decision before exposing any record, filtering out anything not
 * approved. Wraps [MemoryRetrieval] directly (`InMemoryMemoryCore` in the
 * frozen stack, Errata 004 Section 9: "the frozen stack has no
 * `EventPublishingMemoryCore` equivalent on the read side, since
 * retrieval was already established to publish nothing"), without
 * modifying `InMemoryMemoryCore` or the frozen [MemoryRetrieval] contract
 * itself.
 *
 * ## Principal propagation -- no ambient identity
 *
 * The four direct-lookup methods already carry an explicit
 * `requestingPrincipalId` parameter (Errata 004 Section 5); the six
 * query-based methods already carry `requestingPrincipalId` on their own
 * query object (Unit 7, "for auditability only" -- Errata 004 Section 6
 * extends its use, without changing its shape, requiredness, or
 * validation, to also drive this class's own per-record permission
 * checks). This class introduces no second source of identity anywhere.
 *
 * ## Resource representation and action names -- Errata 004 Section 7
 *
 * Exactly as `PermissionGatedMemoryCore`: Memory Core records are never
 * Resource Registry entries, so `ExecutionRequest.targetResources` is
 * always `emptyList()`; `proposedActions` carries exactly one descriptive
 * string, `RETRIEVE_DOCUMENT_ACTION_NAME` for a `Document` (Errata 004's
 * own literal frozen table row, "Retrieval of a Document | READ |
 * DOCUMENT") or `RETRIEVE_ACTION_NAME` for every other Memory Core record
 * kind ("Retrieval of any other Memory Core record | READ | MEMORY").
 * The same disclosed consequence [PermissionGatedMemoryCore] documents
 * applies here identically: approving any retrieval requires a future,
 * dedicated policy able to decide `(READ, MEMORY)`/`(READ, DOCUMENT)`
 * without a resolved target Resource -- out of this Unit's own scope,
 * which composes the existing, shared engine only. **This class does not
 * redesign the Permission Engine to solve that limitation** -- it calls
 * [PermissionEngine.evaluate] exactly as specified, once per record,
 * exactly as the frozen design requires, regardless of what the
 * currently-composed policy instance happens to decide today.
 *
 * ## Direct lookups: retrieve first, then gate -- Errata 004 Section 9
 *
 * "Retrieve unconditionally from the delegate, then evaluate permission
 * against the actual retrieved record's own kind... returning the record
 * if permitted or `null` otherwise." If the delegate itself returns
 * `null` (genuinely no such record), this class evaluates nothing and
 * returns `null` immediately -- there is nothing to protect and nothing
 * to gate; this is not a "not found equals denied" conflation, since no
 * permission decision is made in that path at all. Only when a record
 * genuinely exists does this class ask whether the requester may see it.
 *
 * ## Direct-read non-disclosure: `null` for both absence and denial -- Errata 004 Section 8
 *
 * `Entity?`/`Document?`/`Assertion?`/`Relationship?` already collapse "no
 * such record" and "record exists, not shown" into the same,
 * indistinguishable signal. This class introduces no new denial type,
 * sentinel, or thrown exception on this path -- doing so would itself
 * leak which case occurred (Errata 004 Section 8's own reasoning,
 * verbatim).
 *
 * ## Query-based methods: filter, never re-rank or gate the whole query -- Errata 004 Section 9
 *
 * "Retrieve the full, unfiltered structural match set from the delegate
 * ..., then evaluate each result individually and keep only the permitted
 * ones -- never filtering, ranking, or otherwise altering
 * `InMemoryMemoryCore`'s own structural matching, only removing entries
 * the requester may not see." Kotlin's `List.filter` preserves the
 * original relative order of retained elements and never mutates an
 * element -- both properties this class relies on rather than
 * re-implements.
 *
 * ## Fault propagation
 *
 * A genuine dependency fault -- thrown by [delegate] or by
 * [permissionEngine] -- propagates completely unchanged. This class
 * catches nothing either dependency throws; only a returned
 * [parker.core.interfaces.PermissionDecision] whose `decision` is not
 * `APPROVED`/`APPROVED_WITH_CONFIRMATION` is treated as a denial.
 */
class PermissionFilteredMemoryRetrieval(
    private val delegate: MemoryRetrieval,
    private val permissionEngine: PermissionEngine,
) : MemoryRetrieval {

    /**
     * Gap #54 Operationalisation Unit 3: creates a non-authoritative, immutable Purpose carrier
     * over this one decorator. The returned view has no engine or raw delegate and can only route
     * calls back through this parent's existing retrieval, request construction and filtering.
     * Kept `internal` for composition and verification; [ParkerRuntime] owns the sole production
     * instance locally and exposes neither the parent nor this factory to runtime callers.
     */
    internal fun forAuthorizationPurpose(authorizationPurpose: AuthorizationPurposeId): MemoryRetrieval =
        PurposeBoundMemoryRetrieval(this, authorizationPurpose)

    override suspend fun getEntity(requestingPrincipalId: PrincipalId, entityId: EntityId): Entity? =
        getEntity(requestingPrincipalId, entityId, null)

    private suspend fun getEntity(
        requestingPrincipalId: PrincipalId,
        entityId: EntityId,
        authorizationPurpose: AuthorizationPurposeId?,
    ): Entity? {
        val entity = delegate.getEntity(requestingPrincipalId, entityId) ?: return null
        return if (isApproved(requestingPrincipalId, RETRIEVE_ACTION_NAME, authorizationPurpose)) entity else null
    }

    override suspend fun getDocument(requestingPrincipalId: PrincipalId, documentId: DocumentId): Document? =
        getDocument(requestingPrincipalId, documentId, null)

    private suspend fun getDocument(
        requestingPrincipalId: PrincipalId,
        documentId: DocumentId,
        authorizationPurpose: AuthorizationPurposeId?,
    ): Document? {
        val document = delegate.getDocument(requestingPrincipalId, documentId) ?: return null
        return if (isApproved(requestingPrincipalId, RETRIEVE_DOCUMENT_ACTION_NAME, authorizationPurpose)) document else null
    }

    override suspend fun getAssertion(requestingPrincipalId: PrincipalId, assertionId: AssertionId): Assertion? =
        getAssertion(requestingPrincipalId, assertionId, null)

    private suspend fun getAssertion(
        requestingPrincipalId: PrincipalId,
        assertionId: AssertionId,
        authorizationPurpose: AuthorizationPurposeId?,
    ): Assertion? {
        val assertion = delegate.getAssertion(requestingPrincipalId, assertionId) ?: return null
        return if (isApproved(requestingPrincipalId, RETRIEVE_ACTION_NAME, authorizationPurpose)) assertion else null
    }

    override suspend fun getRelationship(requestingPrincipalId: PrincipalId, relationshipId: RelationshipId): Relationship? =
        getRelationship(requestingPrincipalId, relationshipId, null)

    private suspend fun getRelationship(
        requestingPrincipalId: PrincipalId,
        relationshipId: RelationshipId,
        authorizationPurpose: AuthorizationPurposeId?,
    ): Relationship? {
        val relationship = delegate.getRelationship(requestingPrincipalId, relationshipId) ?: return null
        return if (isApproved(requestingPrincipalId, RETRIEVE_ACTION_NAME, authorizationPurpose)) relationship else null
    }

    override suspend fun findEntities(query: EntityLookupQuery): List<Entity> =
        findEntities(query, null)

    private suspend fun findEntities(query: EntityLookupQuery, authorizationPurpose: AuthorizationPurposeId?): List<Entity> =
        delegate.findEntities(query).filter { isApproved(query.requestingPrincipalId, RETRIEVE_ACTION_NAME, authorizationPurpose) }

    override suspend fun findDocuments(query: DocumentLookupQuery): List<Document> =
        findDocuments(query, null)

    private suspend fun findDocuments(query: DocumentLookupQuery, authorizationPurpose: AuthorizationPurposeId?): List<Document> =
        delegate.findDocuments(query).filter { isApproved(query.requestingPrincipalId, RETRIEVE_DOCUMENT_ACTION_NAME, authorizationPurpose) }

    override suspend fun traverseRelationships(query: RelationshipTraversalQuery): List<Relationship> =
        traverseRelationships(query, null)

    private suspend fun traverseRelationships(
        query: RelationshipTraversalQuery,
        authorizationPurpose: AuthorizationPurposeId?,
    ): List<Relationship> =
        delegate.traverseRelationships(query).filter { isApproved(query.requestingPrincipalId, RETRIEVE_ACTION_NAME, authorizationPurpose) }

    override suspend fun findByTimeRange(query: ChronologicalLookupQuery): List<MemoryCoreRecord> =
        findByTimeRange(query, null)

    private suspend fun findByTimeRange(
        query: ChronologicalLookupQuery,
        authorizationPurpose: AuthorizationPurposeId?,
    ): List<MemoryCoreRecord> =
        filterAuthorisedRecords(query.requestingPrincipalId, delegate.findByTimeRange(query), authorizationPurpose)

    override suspend fun findByMetadata(query: MetadataLookupQuery): List<MemoryCoreRecord> =
        findByMetadata(query, null)

    private suspend fun findByMetadata(
        query: MetadataLookupQuery,
        authorizationPurpose: AuthorizationPurposeId?,
    ): List<MemoryCoreRecord> =
        filterAuthorisedRecords(query.requestingPrincipalId, delegate.findByMetadata(query), authorizationPurpose)

    override suspend fun findByProvenance(query: ProvenanceLookupQuery): List<MemoryCoreRecord> =
        findByProvenance(query, null)

    private suspend fun findByProvenance(
        query: ProvenanceLookupQuery,
        authorizationPurpose: AuthorizationPurposeId?,
    ): List<MemoryCoreRecord> =
        filterAuthorisedRecords(query.requestingPrincipalId, delegate.findByProvenance(query), authorizationPurpose)

    /**
     * One [PermissionEngine.evaluate] call per candidate [records] entry,
     * keeping only the permitted ones, in their original relative order
     * (`List.filter`'s own guarantee) -- never a single, whole-query gate
     * (Errata 004 Section 9).
     */
    private suspend fun filterAuthorisedRecords(
        requestingPrincipalId: PrincipalId,
        records: List<MemoryCoreRecord>,
        authorizationPurpose: AuthorizationPurposeId?,
    ): List<MemoryCoreRecord> =
        records.filter { record ->
            val actionName = when (record) {
                is MemoryCoreRecord.OfDocument -> RETRIEVE_DOCUMENT_ACTION_NAME
                is MemoryCoreRecord.OfEntity, is MemoryCoreRecord.OfAssertion, is MemoryCoreRecord.OfRelationship -> RETRIEVE_ACTION_NAME
            }
            isApproved(requestingPrincipalId, actionName, authorizationPurpose)
        }

    private suspend fun isApproved(
        requestingPrincipalId: PrincipalId,
        actionName: String,
        authorizationPurpose: AuthorizationPurposeId?,
    ): Boolean {
        val decision = permissionEngine.evaluate(buildExecutionRequest(requestingPrincipalId, actionName, authorizationPurpose))
        return decision.decision == PermissionDecisionOutcome.APPROVED ||
            decision.decision == PermissionDecisionOutcome.APPROVED_WITH_CONFIRMATION
    }

    /** Errata 004 Section 7: `targetResources` is always empty; `proposedActions` carries the one descriptive string. */
    private fun buildExecutionRequest(
        requestingPrincipalId: PrincipalId,
        actionName: String,
        authorizationPurpose: AuthorizationPurposeId?,
    ): ExecutionRequest {
        val id = UUID.randomUUID()
        return ExecutionRequest(
            requestId = RequestId("memory-core-read-$id"),
            principalId = requestingPrincipalId,
            origin = RequestOrigin.AGENT,
            intent = "Perform Memory Core retrieval operation '$actionName'",
            targetResources = emptyList(),
            proposedActions = listOf(actionName),
            priority = RequestPriority.NORMAL,
            createdAt = Instant.now(),
            correlationId = "memory-core-read-$id",
            authorizationPurpose = authorizationPurpose,
        )
    }

    /** Immutable carrier only: no engine, policy, registry, raw delegate, or decision logic. */
    private class PurposeBoundMemoryRetrieval(
        private val parent: PermissionFilteredMemoryRetrieval,
        private val authorizationPurpose: AuthorizationPurposeId,
    ) : MemoryRetrieval {
        override suspend fun getEntity(requestingPrincipalId: PrincipalId, entityId: EntityId): Entity? =
            parent.getEntity(requestingPrincipalId, entityId, authorizationPurpose)

        override suspend fun getDocument(requestingPrincipalId: PrincipalId, documentId: DocumentId): Document? =
            parent.getDocument(requestingPrincipalId, documentId, authorizationPurpose)

        override suspend fun getAssertion(requestingPrincipalId: PrincipalId, assertionId: AssertionId): Assertion? =
            parent.getAssertion(requestingPrincipalId, assertionId, authorizationPurpose)

        override suspend fun getRelationship(requestingPrincipalId: PrincipalId, relationshipId: RelationshipId): Relationship? =
            parent.getRelationship(requestingPrincipalId, relationshipId, authorizationPurpose)

        override suspend fun findEntities(query: EntityLookupQuery): List<Entity> =
            parent.findEntities(query, authorizationPurpose)

        override suspend fun findDocuments(query: DocumentLookupQuery): List<Document> =
            parent.findDocuments(query, authorizationPurpose)

        override suspend fun traverseRelationships(query: RelationshipTraversalQuery): List<Relationship> =
            parent.traverseRelationships(query, authorizationPurpose)

        override suspend fun findByTimeRange(query: ChronologicalLookupQuery): List<MemoryCoreRecord> =
            parent.findByTimeRange(query, authorizationPurpose)

        override suspend fun findByMetadata(query: MetadataLookupQuery): List<MemoryCoreRecord> =
            parent.findByMetadata(query, authorizationPurpose)

        override suspend fun findByProvenance(query: ProvenanceLookupQuery): List<MemoryCoreRecord> =
            parent.findByProvenance(query, authorizationPurpose)
    }

    companion object {
        /** Retrieval of a `Document` (Errata 004 Section 7: `READ` on `DOCUMENT`). */
        const val RETRIEVE_DOCUMENT_ACTION_NAME: String = "memory.retrieve_document"

        /** Retrieval of any other Memory Core record kind (Errata 004 Section 7: `READ` on `MEMORY`). */
        const val RETRIEVE_ACTION_NAME: String = "memory.retrieve"
    }
}
