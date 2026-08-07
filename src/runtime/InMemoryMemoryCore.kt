package parker.core.runtime

import java.time.Instant
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import parker.core.interfaces.Assertion
import parker.core.interfaces.AssertionId
import parker.core.interfaces.CandidateAssertion
import parker.core.interfaces.CandidateDocument
import parker.core.interfaces.CandidateEntity
import parker.core.interfaces.CandidateProvenance
import parker.core.interfaces.CandidateRelationship
import parker.core.interfaces.ChronologicalLookupQuery
import parker.core.interfaces.Document
import parker.core.interfaces.DocumentId
import parker.core.interfaces.DocumentLookupQuery
import parker.core.interfaces.Entity
import parker.core.interfaces.EntityId
import parker.core.interfaces.EntityLookupQuery
import parker.core.interfaces.MemoryCore
import parker.core.interfaces.MemoryCoreRecord
import parker.core.interfaces.MemoryCoreRecordReference
import parker.core.interfaces.MemoryCoreRecordStatus
import parker.core.interfaces.MemoryRetrieval
import parker.core.interfaces.MetadataLookupQuery
import parker.core.interfaces.PrincipalId
import parker.core.interfaces.Provenance
import parker.core.interfaces.ProvenanceId
import parker.core.interfaces.ProvenanceLookupQuery
import parker.core.interfaces.Relationship
import parker.core.interfaces.RelationshipEndpoint
import parker.core.interfaces.RelationshipId
import parker.core.interfaces.RelationshipTraversalDirection
import parker.core.interfaces.RelationshipTraversalQuery

/**
 * Programme 2, Memory Core, Implementation Unit 8. The first in-memory
 * implementation of [MemoryCore] and [MemoryRetrieval]
 * (`docs/architecture/MEMORY_CORE_CONTRACT_DESIGN.md`, the Contract
 * Design; `docs/architecture/MEMORY_CORE_SCOPE_LOCK.md`, the Scope Lock;
 * `docs/implementation/MEMORY_CORE_IMPLEMENTATION_PLAN.md`, the
 * Implementation Plan). Implements both interfaces directly on one
 * class, over one shared set of in-memory stores -- mirroring
 * [InMemoryKnowledgeStore]'s own "one interface, one implementing class"
 * precedent, extended here to two interfaces over the same owned state,
 * the same way [InMemoryKnowledgeStore] itself already implements both
 * [parker.core.interfaces.KnowledgeStore] and
 * [parker.core.interfaces.KnowledgeSource].
 *
 * ## What this class does not do
 *
 * Per this Unit's explicit scope: no event publication (`memory.*`
 * events are Unit 9's own responsibility), no permission evaluation or
 * owner-authority check (entirely external -- Runtime authorises before
 * invocation; a future `PermissionGatedMemoryCore`/
 * `PermissionFilteredMemoryRetrieval` decorator, not built here, is
 * where that lives), no persistence (in-memory only, exactly like
 * [InMemoryKnowledgeStore]), no Knowledge Memory/Conversation/World Model
 * integration, no semantic or ranked retrieval, and no runtime
 * composition (`ParkerRuntime.kt` is untouched by this Unit). This
 * class's constructor takes no dependency at all -- no `PermissionEngine`,
 * no `EventBus`, no `IdentityService` -- since nothing in the approved
 * architecture requires one.
 *
 * ## `requestingPrincipalId`, added by Errata 004
 *
 * Every [MemoryCore] and direct-lookup [MemoryRetrieval] method now
 * takes a leading `requestingPrincipalId: PrincipalId`
 * (`docs/architecture/MEMORY_CORE_CONTRACT_DESIGN_ERRATA_004.md`). This
 * class accepts and ignores it -- it is never compared, branched on,
 * stored, or otherwise read anywhere in this file. The parameter exists
 * so a permission-gating/-filtering decorator sitting above this class
 * (`PermissionGatedMemoryCore`/`PermissionFilteredMemoryRetrieval`,
 * still not built here) has a Principal to evaluate against; it changes
 * nothing about what this class itself does. Two calls differing only in
 * `requestingPrincipalId` produce byte-for-byte identical stored records
 * and identical retrieval results.
 *
 * ## Concurrency
 *
 * One [Mutex] guards every store and every counter, exactly as
 * [InMemoryKnowledgeStore] itself does -- no per-kind or per-operation
 * locking, since nothing in this Unit's scope requires finer granularity
 * and a single lock keeps every operation's ordering unambiguous.
 *
 * ## Identifier minting
 *
 * Each of the five record kinds has its own independent, monotonically
 * increasing counter, minted only while holding [mutex] -- guaranteeing
 * every identifier this instance ever mints is unique within that kind's
 * own namespace, mirroring [InMemoryKnowledgeStore.nextSequence]'s identical
 * pattern.
 *
 * ## Referential integrity implemented by this Unit
 *
 * Exactly two checks, both enforced only at creation time, never
 * re-validated afterward (Memory Core is append-only history, not a
 * live-consistency graph): every [CandidateEntity]/[CandidateDocument]/
 * [CandidateAssertion]/[CandidateRelationship]'s own `provenanceId` must
 * already exist in [provenanceStore] (Contract Design's own "creation
 * fails if required provenance does not exist" rule); every
 * [CandidateRelationship]'s endpoint of a Memory-Core-owned kind
 * ([RelationshipEndpoint.ENTITY], [RelationshipEndpoint.DOCUMENT],
 * [RelationshipEndpoint.ASSERTION], [RelationshipEndpoint.RELATIONSHIP])
 * must already exist in the matching store; an endpoint of any other
 * kind ([RelationshipEndpoint.CONVERSATION_TURN],
 * [RelationshipEndpoint.KNOWLEDGE_RECORD], or any future/unrecognised
 * kind) is accepted unverified (Contract Design Section 8;
 * [Relationship]'s own KDoc). Self-reference rejection needs no check
 * here at all -- [CandidateRelationship]'s own `init` block already
 * guarantees `fromEndpoint != toEndpoint` before this class ever sees a
 * candidate. [Provenance.derivedFrom] and [Provenance.extractedFrom] are
 * deliberately **not** verified by this Unit -- neither the Contract
 * Design nor the Scope Lock names them among the checks approved for
 * Version 1, and adding an unrequested check would be scope expansion,
 * not implementation of what was approved.
 *
 * ## Lifecycle
 *
 * [transitionStatus] validates every transition against
 * [MemoryCoreLifecycleTransitions]'s own closed table -- the exact table
 * `docs/architecture/MEMORY_CORE_CONTRACT_DESIGN_ERRATA_003.md` froze,
 * no more and no fewer transitions. An unknown target record throws
 * [NoSuchElementException]; an existing record targeted with a status
 * unreachable from its current one throws [IllegalArgumentException] --
 * via [MemoryCoreLifecycleTransitions.requireValidTransition], mirroring
 * [parker.core.contracts.ExecutionLifecycleTransitions.requireValidTransition]'s
 * own, already-established, directly-on-point precedent for exactly this
 * kind of check in this codebase. A record set to
 * [MemoryCoreRecordStatus.DELETED] is never removed from its store --
 * only its own `status` field changes; every deleted record remains
 * retrievable by every [MemoryRetrieval] method exactly as any other
 * status would be, since no method in this class performs implicit
 * status filtering the caller did not itself request.
 */
class InMemoryMemoryCore : MemoryCore, MemoryRetrieval {

    private val mutex = Mutex()

    private val provenanceStore = mutableMapOf<ProvenanceId, Provenance>()
    private val entityStore = mutableMapOf<EntityId, Entity>()
    private val documentStore = mutableMapOf<DocumentId, Document>()
    private val assertionStore = mutableMapOf<AssertionId, Assertion>()
    private val relationshipStore = mutableMapOf<RelationshipId, Relationship>()

    private var nextProvenanceSequence = 1L
    private var nextEntitySequence = 1L
    private var nextDocumentSequence = 1L
    private var nextAssertionSequence = 1L
    private var nextRelationshipSequence = 1L

    // ================= Write behaviour =================

    override suspend fun createProvenance(requestingPrincipalId: PrincipalId, candidate: CandidateProvenance): Provenance = mutex.withLock {
        val provenanceId = ProvenanceId("provenance-${nextProvenanceSequence++}")
        val provenance = Provenance(
            provenanceId = provenanceId,
            sourceIdentifier = candidate.sourceIdentifier,
            sourceType = candidate.sourceType,
            acquisitionTime = candidate.acquisitionTime,
            ingestionTime = Instant.now(),
            contentNature = candidate.contentNature,
            creator = candidate.creator,
            creatorPrincipalId = candidate.creatorPrincipalId,
            claimedCreationTime = candidate.claimedCreationTime,
            derivedFrom = candidate.derivedFrom,
            extractedFrom = candidate.extractedFrom,
            processingHistory = candidate.processingHistory,
            integrityInformation = candidate.integrityInformation,
            confidence = candidate.confidence,
            sensitivity = candidate.sensitivity,
        )
        provenanceStore[provenanceId] = provenance
        provenance
    }

    override suspend fun createEntity(requestingPrincipalId: PrincipalId, candidate: CandidateEntity): Entity = mutex.withLock {
        requireExistingProvenance(candidate.provenanceId)
        val entityId = EntityId("entity-${nextEntitySequence++}")
        val entity = Entity(
            entityId = entityId,
            entityType = candidate.entityType,
            primaryLabel = candidate.primaryLabel,
            provenanceId = candidate.provenanceId,
            createdAt = Instant.now(),
            aliases = candidate.aliases,
            relatedPrincipalId = candidate.relatedPrincipalId,
            status = MemoryCoreRecordStatus.ACTIVE,
            metadata = candidate.metadata,
        )
        entityStore[entityId] = entity
        entity
    }

    override suspend fun registerDocument(requestingPrincipalId: PrincipalId, candidate: CandidateDocument): Document = mutex.withLock {
        requireExistingProvenance(candidate.provenanceId)
        val documentId = DocumentId("document-${nextDocumentSequence++}")
        val document = Document(
            documentId = documentId,
            documentType = candidate.documentType,
            locationReference = candidate.locationReference,
            provenanceId = candidate.provenanceId,
            registeredAt = Instant.now(),
            integrityHash = candidate.integrityHash,
            processingStatus = candidate.processingStatus,
            status = MemoryCoreRecordStatus.ACTIVE,
            metadata = candidate.metadata,
        )
        documentStore[documentId] = document
        document
    }

    override suspend fun createAssertion(requestingPrincipalId: PrincipalId, candidate: CandidateAssertion): Assertion = mutex.withLock {
        requireExistingProvenance(candidate.provenanceId)
        val assertionId = AssertionId("assertion-${nextAssertionSequence++}")
        val assertion = Assertion(
            assertionId = assertionId,
            statement = candidate.statement,
            provenanceId = candidate.provenanceId,
            confidence = candidate.confidence,
            status = MemoryCoreRecordStatus.ACTIVE,
            metadata = candidate.metadata,
        )
        assertionStore[assertionId] = assertion
        assertion
    }

    override suspend fun createRelationship(requestingPrincipalId: PrincipalId, candidate: CandidateRelationship): Relationship = mutex.withLock {
        requireExistingProvenance(candidate.provenanceId)
        requireResolvableEndpoint(candidate.fromEndpoint)
        requireResolvableEndpoint(candidate.toEndpoint)
        // Self-reference is already impossible to construct here: CandidateRelationship's own
        // init block rejects fromEndpoint == toEndpoint before this method is ever reached.
        val relationshipId = RelationshipId("relationship-${nextRelationshipSequence++}")
        val relationship = Relationship(
            relationshipId = relationshipId,
            relationshipType = candidate.relationshipType,
            fromEndpoint = candidate.fromEndpoint,
            toEndpoint = candidate.toEndpoint,
            directional = candidate.directional,
            provenanceId = candidate.provenanceId,
            createdAt = Instant.now(),
            status = MemoryCoreRecordStatus.ACTIVE,
        )
        relationshipStore[relationshipId] = relationship
        relationship
    }

    override suspend fun transitionStatus(
        requestingPrincipalId: PrincipalId,
        reference: MemoryCoreRecordReference,
        targetStatus: MemoryCoreRecordStatus,
    ): MemoryCoreRecord = mutex.withLock {
        when (reference) {
            is MemoryCoreRecordReference.ToEntity -> {
                val current = entityStore[reference.entityId]
                    ?: throw NoSuchElementException("Entity '${reference.entityId.value}' does not exist")
                MemoryCoreLifecycleTransitions.requireValidTransition(current.status, targetStatus)
                val updated = current.copy(status = targetStatus)
                entityStore[reference.entityId] = updated
                MemoryCoreRecord.OfEntity(updated)
            }

            is MemoryCoreRecordReference.ToDocument -> {
                val current = documentStore[reference.documentId]
                    ?: throw NoSuchElementException("Document '${reference.documentId.value}' does not exist")
                MemoryCoreLifecycleTransitions.requireValidTransition(current.status, targetStatus)
                val updated = current.copy(status = targetStatus)
                documentStore[reference.documentId] = updated
                MemoryCoreRecord.OfDocument(updated)
            }

            is MemoryCoreRecordReference.ToAssertion -> {
                val current = assertionStore[reference.assertionId]
                    ?: throw NoSuchElementException("Assertion '${reference.assertionId.value}' does not exist")
                MemoryCoreLifecycleTransitions.requireValidTransition(current.status, targetStatus)
                val updated = current.copy(status = targetStatus)
                assertionStore[reference.assertionId] = updated
                MemoryCoreRecord.OfAssertion(updated)
            }

            is MemoryCoreRecordReference.ToRelationship -> {
                val current = relationshipStore[reference.relationshipId]
                    ?: throw NoSuchElementException("Relationship '${reference.relationshipId.value}' does not exist")
                MemoryCoreLifecycleTransitions.requireValidTransition(current.status, targetStatus)
                val updated = current.copy(status = targetStatus)
                relationshipStore[reference.relationshipId] = updated
                MemoryCoreRecord.OfRelationship(updated)
            }
        }
    }

    // ================= Restore behaviour (Memory Core Durability, Implementation Unit 4) =================

    /**
     * Memory Core Durability, Implementation Unit 4 (Replay and Startup
     * Recovery). Five internal-only restoration functions -- one per
     * creatable record kind -- added as a strictly additive extension to
     * this class, authorised by
     * `docs/implementation/MEMORY_CORE_DURABILITY_IMPLEMENTATION_PLAN.md`'s
     * own Section 5 ("Design Resolution -- Identifier Restoration Without
     * Public-Contract Reuse"). **No existing method's signature,
     * behaviour, or semantics changes anywhere in this class.** [restoreProvenance],
     * [restoreEntity], [restoreDocument], [restoreAssertion], and
     * [restoreRelationship] are `internal`, not `public` -- unreachable
     * through [MemoryCore] or [MemoryRetrieval], and callable only by code
     * elsewhere in this same module (`MemoryCoreRecovery`, this Unit's own
     * new orchestration; this Unit's own test file) -- mirroring
     * [MemoryCoreLifecycleTransitions]'s own established precedent
     * exactly: kept `internal`, not `private`, "solely so
     * `InMemoryMemoryCoreTest.kt` can exercise [isValidTransition]
     * directly across every pair this table defines, rather than only
     * indirectly through" the public write surface.
     *
     * ## Why a separate function, not reuse of `create*`
     *
     * Each `create*` operation mints a fresh identifier internally
     * (`"kind-${nextKindSequence++}"`) -- calling one during replay would
     * mint a *new* identifier for what is actually the *restoration* of an
     * already-existing one, immediately violating identifier stability
     * across a restart. Each restore function instead accepts the
     * already-complete domain record (carrying its own original
     * identifier, timestamp, provenance reference, and initial status
     * exactly as first recorded) and stores it directly -- no identifier
     * is minted, no sequence counter is advanced (identifier-counter
     * restoration is Implementation Unit 5's own, separate, later
     * responsibility, per the Implementation Plan's own Section 5).
     *
     * ## Referential integrity, re-enforced during restore -- not skipped
     *
     * Each restore function (aside from [restoreProvenance], which -- like
     * [createProvenance] -- has no provenance reference of its own to
     * check) re-runs the exact same [requireExistingProvenance]/
     * [requireResolvableEndpoint] checks its own `create*` counterpart
     * already performs, now validated against whatever this restoration
     * pass has already restored -- the direct mechanism satisfying the
     * Durability Contract Design's own Section 6: "referential-integrity
     * validation is required during recovery, not optional."
     *
     * ## Repeated identical records are accepted idempotently; conflicting
     * duplicates are rejected
     *
     * Each function checks whether a record already occupies the incoming
     * identifier. If one does and is byte-for-byte identical to the
     * incoming record, the call returns normally without altering
     * anything -- the Durability Contract Design's own Section 6
     * "repeated durable record handling" requirement ("must be recognised
     * and idempotently skipped during replay, never treated as a second,
     * conflicting record"). If one exists and differs, this is genuine
     * corruption -- a [check] failure (`IllegalStateException`), never
     * silently overwritten and never silently ignored.
     *
     * ## No permission evaluation, exactly like every `create*` operation
     *
     * None of these functions holds, constructs, or references a
     * `PermissionEngine` -- identical to every other operation this class
     * already implements (this class's own top-level KDoc: "no permission
     * evaluation or owner-authority check"). Restoration authority is
     * assumed to have already been established by the fact that this
     * content was previously, lawfully durably committed in the first
     * place -- replaying it is not a new write requiring a new permission
     * decision.
     */
    internal suspend fun restoreProvenance(provenance: Provenance) = mutex.withLock {
        val existing = provenanceStore[provenance.provenanceId]
        if (existing != null) {
            check(existing == provenance) {
                "Cannot restore Provenance '${provenance.provenanceId.value}': an existing record with " +
                    "different content already occupies this identifier"
            }
            return@withLock
        }
        provenanceStore[provenance.provenanceId] = provenance
    }

    internal suspend fun restoreEntity(entity: Entity) = mutex.withLock {
        val existing = entityStore[entity.entityId]
        if (existing != null) {
            check(existing == entity) {
                "Cannot restore Entity '${entity.entityId.value}': an existing record with different " +
                    "content already occupies this identifier"
            }
            return@withLock
        }
        requireExistingProvenance(entity.provenanceId)
        entityStore[entity.entityId] = entity
    }

    internal suspend fun restoreDocument(document: Document) = mutex.withLock {
        val existing = documentStore[document.documentId]
        if (existing != null) {
            check(existing == document) {
                "Cannot restore Document '${document.documentId.value}': an existing record with different " +
                    "content already occupies this identifier"
            }
            return@withLock
        }
        requireExistingProvenance(document.provenanceId)
        documentStore[document.documentId] = document
    }

    internal suspend fun restoreAssertion(assertion: Assertion) = mutex.withLock {
        val existing = assertionStore[assertion.assertionId]
        if (existing != null) {
            check(existing == assertion) {
                "Cannot restore Assertion '${assertion.assertionId.value}': an existing record with " +
                    "different content already occupies this identifier"
            }
            return@withLock
        }
        requireExistingProvenance(assertion.provenanceId)
        assertionStore[assertion.assertionId] = assertion
    }

    internal suspend fun restoreRelationship(relationship: Relationship) = mutex.withLock {
        val existing = relationshipStore[relationship.relationshipId]
        if (existing != null) {
            check(existing == relationship) {
                "Cannot restore Relationship '${relationship.relationshipId.value}': an existing record " +
                    "with different content already occupies this identifier"
            }
            return@withLock
        }
        requireExistingProvenance(relationship.provenanceId)
        requireResolvableEndpoint(relationship.fromEndpoint)
        requireResolvableEndpoint(relationship.toEndpoint)
        relationshipStore[relationship.relationshipId] = relationship
    }

    // ================= Identifier counter restoration (Memory Core Durability, Implementation Unit 5) =================

    /**
     * Memory Core Durability, Implementation Unit 5 (Identifier
     * Restoration). Restores all five per-kind identifier counters in one
     * call, each to `(highest numeric suffix among that kind's own
     * currently-stored identifiers) + 1` -- the exact rule
     * `docs/architecture/MEMORY_CORE_DURABILITY_SCOPE_LOCK.md` Section 18
     * fixes as binding: "A Memory Core durability implementation **SHALL**
     * restore each per-kind identifier counter to (highest persisted
     * identifier of that kind) + 1 on recovery, and **SHALL NOT** permit
     * identifier reuse across a restart." A kind with no currently-stored
     * identifiers restores to `1` -- the same starting value this class
     * already uses for a genuinely fresh store, computed here as the
     * natural result of `(0) + 1` rather than special-cased.
     *
     * ## Derivation, not separate persistence -- by design, not by
     * omission
     *
     * `docs/implementation/MEMORY_CORE_DURABILITY_IMPLEMENTATION_PLAN.md`'s
     * own Unit 5 section selects deriving each counter from the
     * already-restored records themselves, over durably persisting a
     * counter value as its own separate fact, "avoiding a second,
     * redundant source of truth that could itself drift out of sync with
     * the records it counts."
     * This function is that derivation, reading directly from this
     * class's own store keys -- the single, authoritative source of which
     * identifiers actually exist -- never from a separately-tracked tally.
     *
     * ## Independent per kind; immune to duplicates, gaps, and lifecycle
     * transitions
     *
     * Each of the five stores is inspected independently, using only that
     * store's own current key set: a sparse or out-of-order-looking
     * identifier sequence within one kind restores correctly because every
     * key is considered, not merely the most recently encountered one, and
     * because deriving from the *final* store contents (not from a
     * running tally kept during replay) means a durably-repeated creation
     * entry -- already idempotently collapsed to one stored record by
     * [restoreEntity] and its four counterparts -- can never inflate a
     * counter twice for what is, in the store, one single key. A
     * [DurableMemoryCoreEntry.StatusTransitioned] entry never adds,
     * removes, or renames a store key (only [transitionStatus]'s own
     * `.copy(status = ...)` touches an existing entry's `status` field),
     * so lifecycle transitions have no way to influence any counter this
     * function computes.
     *
     * ## Malformed identifiers fail closed -- never silently ignored, never
     * silently defaulted
     *
     * Every identifier type this repository defines validates only that
     * its own value is non-blank -- nothing enforces the `"kind-N"`
     * minting format this class's own `create*` operations always use at
     * the type level. An identifier that does not match that format
     * (wrong prefix, non-numeric or non-positive suffix) is refused
     * outright, via [require] (`IllegalArgumentException`), rather than
     * silently skipped (which could under-count and risk a future
     * collision) or silently coerced to some default (which would be
     * exactly the fabricated-certainty this Programme's own governing
     * principle forbids). A suffix at or adjacent to [Long]'s own maximum
     * value, whose `+ 1` would silently wrap to a negative, invalid
     * counter under ordinary Kotlin arithmetic, is instead detected via
     * [Math.addExact] and reported as a failure, never allowed to wrap
     * silently.
     *
     * ## Call discipline: after successful replay only, never before, and
     * only from within this same module
     *
     * `internal`, mirroring every other Unit 4/5 addition to this class:
     * unreachable through [MemoryCore] or [MemoryRetrieval]. This function
     * performs no referential-integrity check and constructs no new
     * record -- it is meaningful, and safe to call, only once every
     * `restore*` call for this recovery pass has already completed
     * successfully; `MemoryCoreRecovery.recover` is the only caller, and
     * calls this exactly once, as the last step before returning a
     * successfully recovered instance -- never on a partially-populated
     * store a failed recovery pass would otherwise discard.
     */
    internal suspend fun restoreIdentifierCounters() = mutex.withLock {
        nextProvenanceSequence = nextSequenceFor(provenanceStore.keys.map { it.value }, "provenance-")
        nextEntitySequence = nextSequenceFor(entityStore.keys.map { it.value }, "entity-")
        nextDocumentSequence = nextSequenceFor(documentStore.keys.map { it.value }, "document-")
        nextAssertionSequence = nextSequenceFor(assertionStore.keys.map { it.value }, "assertion-")
        nextRelationshipSequence = nextSequenceFor(relationshipStore.keys.map { it.value }, "relationship-")
    }

    private fun nextSequenceFor(identifierValues: Collection<String>, prefix: String): Long {
        var maxSuffix = 0L
        identifierValues.forEach { value ->
            require(value.startsWith(prefix) && value.length > prefix.length) {
                "Cannot restore identifier counters: '$value' does not match the expected '$prefix<N>' format"
            }
            val suffix = value.substring(prefix.length).toLongOrNull()
            require(suffix != null && suffix >= 1L) {
                "Cannot restore identifier counters: '$value' does not carry a positive numeric suffix"
            }
            if (suffix > maxSuffix) {
                maxSuffix = suffix
            }
        }
        return try {
            Math.addExact(maxSuffix, 1L)
        } catch (e: ArithmeticException) {
            throw IllegalStateException(
                "Cannot restore identifier counters for prefix '$prefix': maximum restored suffix " +
                    "$maxSuffix is already at Long's own upper bound -- the next identifier would overflow",
                e,
            )
        }
    }

    // ================= Retrieval behaviour =================

    override suspend fun getEntity(requestingPrincipalId: PrincipalId, entityId: EntityId): Entity? =
        mutex.withLock { entityStore[entityId] }

    override suspend fun getDocument(requestingPrincipalId: PrincipalId, documentId: DocumentId): Document? =
        mutex.withLock { documentStore[documentId] }

    override suspend fun getAssertion(requestingPrincipalId: PrincipalId, assertionId: AssertionId): Assertion? =
        mutex.withLock { assertionStore[assertionId] }

    override suspend fun getRelationship(requestingPrincipalId: PrincipalId, relationshipId: RelationshipId): Relationship? =
        mutex.withLock { relationshipStore[relationshipId] }

    /**
     * Matching: [EntityLookupQuery.labelOrAliasMatch] is a case-insensitive
     * substring match against [Entity.primaryLabel] or any entry of
     * [Entity.aliases]; [EntityLookupQuery.entityType] and
     * [EntityLookupQuery.status] are exact matches. Every omitted filter
     * matches everything. Ordering: insertion order (the order
     * [createEntity] assigned each identifier), truncated to
     * [EntityLookupQuery.maximumResults] -- deterministic, never ranked.
     */
    override suspend fun findEntities(query: EntityLookupQuery): List<Entity> = mutex.withLock {
        entityStore.values.asSequence()
            .filter { entity ->
                (query.labelOrAliasMatch == null || entity.matchesLabelOrAlias(query.labelOrAliasMatch)) &&
                    (query.entityType == null || entity.entityType == query.entityType) &&
                    (query.status == null || entity.status == query.status)
            }
            .take(query.maximumResults)
            .toList()
    }

    /**
     * Matching: [DocumentLookupQuery.locationReferenceMatch] is a
     * case-insensitive substring match against [Document.locationReference];
     * [DocumentLookupQuery.documentType] and
     * [DocumentLookupQuery.processingStatus] are exact matches. Ordering:
     * insertion order, truncated to [DocumentLookupQuery.maximumResults].
     */
    override suspend fun findDocuments(query: DocumentLookupQuery): List<Document> = mutex.withLock {
        documentStore.values.asSequence()
            .filter { document ->
                (query.documentType == null || document.documentType == query.documentType) &&
                    (query.locationReferenceMatch == null ||
                        document.locationReference.contains(query.locationReferenceMatch, ignoreCase = true)) &&
                    (query.processingStatus == null || document.processingStatus == query.processingStatus)
            }
            .take(query.maximumResults)
            .toList()
    }

    /**
     * Matching: every stored [Relationship] whose [Relationship.fromEndpoint]
     * or [Relationship.toEndpoint] (depending on
     * [RelationshipTraversalQuery.direction]) equals
     * [RelationshipTraversalQuery.startingEndpoint], optionally narrowed by
     * an exact [RelationshipTraversalQuery.relationshipType] match.
     * Ordering: insertion order, truncated to
     * [RelationshipTraversalQuery.maximumResults].
     */
    override suspend fun traverseRelationships(query: RelationshipTraversalQuery): List<Relationship> = mutex.withLock {
        relationshipStore.values.asSequence()
            .filter { relationship ->
                val matchesDirection = when (query.direction) {
                    RelationshipTraversalDirection.FORWARD -> relationship.fromEndpoint == query.startingEndpoint
                    RelationshipTraversalDirection.REVERSE -> relationship.toEndpoint == query.startingEndpoint
                    RelationshipTraversalDirection.BOTH ->
                        relationship.fromEndpoint == query.startingEndpoint || relationship.toEndpoint == query.startingEndpoint
                }
                matchesDirection && (query.relationshipType == null || relationship.relationshipType == query.relationshipType)
            }
            .take(query.maximumResults)
            .toList()
    }

    /**
     * Every record whose own effective creation time falls within
     * [ChronologicalLookupQuery.from]..[ChronologicalLookupQuery.to]
     * (inclusive), optionally narrowed to one
     * [ChronologicalLookupQuery.recordKind]. "Effective creation time" is
     * [Entity.createdAt]/[Document.registeredAt]/[Relationship.createdAt]
     * directly; for [Assertion] -- which carries no timestamp of its own
     * (Unit 5's own deliberate decision) -- it is the referenced
     * [Provenance.ingestionTime], exactly as [Assertion]'s own KDoc already
     * says answers "when was this recorded." Ordering: ascending by
     * effective creation time; ties (including cross-kind ties, when
     * [ChronologicalLookupQuery.recordKind] is `null`) broken by a fixed
     * kind order (Entity, Document, Assertion, Relationship) and then
     * insertion order within that kind -- deterministic and reproducible,
     * never a ranking. Truncated to
     * [ChronologicalLookupQuery.maximumResults].
     */
    override suspend fun findByTimeRange(query: ChronologicalLookupQuery): List<MemoryCoreRecord> = mutex.withLock {
        val candidates = mutableListOf<Pair<Instant, MemoryCoreRecord>>()

        if (query.recordKind == null || query.recordKind == RelationshipEndpoint.ENTITY) {
            entityStore.values.forEach { candidates += it.createdAt to MemoryCoreRecord.OfEntity(it) }
        }
        if (query.recordKind == null || query.recordKind == RelationshipEndpoint.DOCUMENT) {
            documentStore.values.forEach { candidates += it.registeredAt to MemoryCoreRecord.OfDocument(it) }
        }
        if (query.recordKind == null || query.recordKind == RelationshipEndpoint.ASSERTION) {
            assertionStore.values.forEach { assertion ->
                val effectiveTime = provenanceStore.getValue(assertion.provenanceId).ingestionTime
                candidates += effectiveTime to MemoryCoreRecord.OfAssertion(assertion)
            }
        }
        if (query.recordKind == null || query.recordKind == RelationshipEndpoint.RELATIONSHIP) {
            relationshipStore.values.forEach { candidates += it.createdAt to MemoryCoreRecord.OfRelationship(it) }
        }

        candidates
            .asSequence()
            .filter { (time, _) -> !time.isBefore(query.from) && !time.isAfter(query.to) }
            .sortedBy { (time, _) -> time }
            .map { (_, record) -> record }
            .take(query.maximumResults)
            .toList()
    }

    /**
     * Every record of [MetadataLookupQuery.recordKind] whose own `metadata`
     * map contains every entry in [MetadataLookupQuery.metadataFilter].
     * [Relationship] carries no metadata field at all (Errata 001), and any
     * `recordKind` this Unit does not recognise has no known metadata
     * representation to filter against -- both cases structurally match
     * nothing, deterministically, never an exception. Ordering: insertion
     * order, truncated to [MetadataLookupQuery.maximumResults].
     */
    override suspend fun findByMetadata(query: MetadataLookupQuery): List<MemoryCoreRecord> = mutex.withLock {
        fun matchesFilter(metadata: Map<String, String>): Boolean =
            query.metadataFilter.all { (key, value) -> metadata[key] == value }

        val matches: Sequence<MemoryCoreRecord> = when (query.recordKind) {
            RelationshipEndpoint.ENTITY -> entityStore.values.asSequence()
                .filter { matchesFilter(it.metadata) }
                .map { MemoryCoreRecord.OfEntity(it) }

            RelationshipEndpoint.DOCUMENT -> documentStore.values.asSequence()
                .filter { matchesFilter(it.metadata) }
                .map { MemoryCoreRecord.OfDocument(it) }

            RelationshipEndpoint.ASSERTION -> assertionStore.values.asSequence()
                .filter { matchesFilter(it.metadata) }
                .map { MemoryCoreRecord.OfAssertion(it) }

            else -> emptySequence()
        }

        matches.take(query.maximumResults).toList()
    }

    /**
     * Every record of [ProvenanceLookupQuery.recordKind] whose referenced
     * [Provenance] matches every criterion
     * [ProvenanceLookupQuery] supplies ([ProvenanceLookupQuery.sourceType],
     * [ProvenanceLookupQuery.creator], [ProvenanceLookupQuery.contentNature],
     * [ProvenanceLookupQuery.sensitivity] -- each an exact match where
     * supplied). Ordering: insertion order, truncated to
     * [ProvenanceLookupQuery.maximumResults].
     */
    override suspend fun findByProvenance(query: ProvenanceLookupQuery): List<MemoryCoreRecord> = mutex.withLock {
        fun matchesProvenance(provenanceId: ProvenanceId): Boolean {
            val provenance = provenanceStore[provenanceId] ?: return false
            return (query.sourceType == null || provenance.sourceType == query.sourceType) &&
                (query.creator == null || provenance.creator == query.creator) &&
                (query.contentNature == null || provenance.contentNature == query.contentNature) &&
                (query.sensitivity == null || provenance.sensitivity == query.sensitivity)
        }

        val matches: Sequence<MemoryCoreRecord> = when (query.recordKind) {
            RelationshipEndpoint.ENTITY -> entityStore.values.asSequence()
                .filter { matchesProvenance(it.provenanceId) }
                .map { MemoryCoreRecord.OfEntity(it) }

            RelationshipEndpoint.DOCUMENT -> documentStore.values.asSequence()
                .filter { matchesProvenance(it.provenanceId) }
                .map { MemoryCoreRecord.OfDocument(it) }

            RelationshipEndpoint.ASSERTION -> assertionStore.values.asSequence()
                .filter { matchesProvenance(it.provenanceId) }
                .map { MemoryCoreRecord.OfAssertion(it) }

            RelationshipEndpoint.RELATIONSHIP -> relationshipStore.values.asSequence()
                .filter { matchesProvenance(it.provenanceId) }
                .map { MemoryCoreRecord.OfRelationship(it) }

            else -> emptySequence()
        }

        matches.take(query.maximumResults).toList()
    }

    // ================= Private helpers (hold mutex when called) =================

    private fun requireExistingProvenance(provenanceId: ProvenanceId) {
        require(provenanceId in provenanceStore) {
            "Provenance '${provenanceId.value}' does not exist"
        }
    }

    private fun requireResolvableEndpoint(endpoint: RelationshipEndpoint) {
        when (endpoint.recordKind) {
            RelationshipEndpoint.ENTITY -> require(EntityId(endpoint.recordId) in entityStore) {
                "Relationship endpoint references unknown Entity '${endpoint.recordId}'"
            }

            RelationshipEndpoint.DOCUMENT -> require(DocumentId(endpoint.recordId) in documentStore) {
                "Relationship endpoint references unknown Document '${endpoint.recordId}'"
            }

            RelationshipEndpoint.ASSERTION -> require(AssertionId(endpoint.recordId) in assertionStore) {
                "Relationship endpoint references unknown Assertion '${endpoint.recordId}'"
            }

            RelationshipEndpoint.RELATIONSHIP -> require(RelationshipId(endpoint.recordId) in relationshipStore) {
                "Relationship endpoint references unknown Relationship '${endpoint.recordId}'"
            }

            // A kind Memory Core does not own (conversation-turn, knowledge-record) or any
            // future/unrecognised kind -- accepted unverified, per Contract Design Section 8.
            else -> Unit
        }
    }

    private fun Entity.matchesLabelOrAlias(match: String): Boolean =
        primaryLabel.contains(match, ignoreCase = true) || aliases.any { it.contains(match, ignoreCase = true) }
}

/**
 * The one closed table of valid Memory Core lifecycle transitions
 * (`docs/architecture/MEMORY_CORE_CONTRACT_DESIGN_ERRATA_003.md`), and
 * the only place [MemoryCoreLifecycleTransitions] permits deciding
 * whether a transition is legal -- mirroring
 * [parker.core.contracts.ExecutionLifecycleTransitions]'s own,
 * directly-on-point, already-established precedent for exactly this
 * pattern elsewhere in this codebase. Declared alongside
 * [InMemoryMemoryCore] rather than in `src/contracts/`, since
 * `MemoryCoreRecordStatus`'s own KDoc already places transition
 * enforcement in "MemoryCore's own implementation," not in the contract
 * layer itself, and this Unit's own repository discipline scopes its
 * addition to one file.
 *
 * Kept `internal`, not `private`, solely so `InMemoryMemoryCoreTest.kt`
 * can exercise [isValidTransition] directly across every pair this table
 * defines, rather than only indirectly through
 * [InMemoryMemoryCore.transitionStatus].
 */
internal object MemoryCoreLifecycleTransitions {

    private val allowed: Map<MemoryCoreRecordStatus, Set<MemoryCoreRecordStatus>> = mapOf(
        MemoryCoreRecordStatus.ACTIVE to setOf(
            MemoryCoreRecordStatus.DISPUTED,
            MemoryCoreRecordStatus.SUPERSEDED,
            MemoryCoreRecordStatus.ARCHIVED,
            MemoryCoreRecordStatus.DELETED,
        ),
        MemoryCoreRecordStatus.DISPUTED to setOf(
            MemoryCoreRecordStatus.ACTIVE,
            MemoryCoreRecordStatus.SUPERSEDED,
            MemoryCoreRecordStatus.ARCHIVED,
            MemoryCoreRecordStatus.DELETED,
        ),
        MemoryCoreRecordStatus.SUPERSEDED to setOf(
            MemoryCoreRecordStatus.ARCHIVED,
            MemoryCoreRecordStatus.DELETED,
        ),
        MemoryCoreRecordStatus.ARCHIVED to setOf(
            MemoryCoreRecordStatus.ACTIVE,
            MemoryCoreRecordStatus.DISPUTED,
            MemoryCoreRecordStatus.SUPERSEDED,
            MemoryCoreRecordStatus.DELETED,
        ),
        MemoryCoreRecordStatus.DELETED to emptySet(),
    )

    fun isValidTransition(from: MemoryCoreRecordStatus, to: MemoryCoreRecordStatus): Boolean =
        to in allowed.getValue(from)

    fun requireValidTransition(from: MemoryCoreRecordStatus, to: MemoryCoreRecordStatus) {
        require(isValidTransition(from, to)) {
            "Illegal Memory Core lifecycle transition: $from -> $to"
        }
    }
}
