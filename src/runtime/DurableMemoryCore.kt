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
import parker.core.interfaces.ProvenanceLookupQuery
import parker.core.interfaces.Relationship
import parker.core.interfaces.RelationshipId
import parker.core.interfaces.RelationshipTraversalQuery

/**
 * Memory Core Durability, Implementation Unit 6 (Durable Memory Core
 * Decorator). Transparently combines [InMemoryMemoryCore],
 * [MemoryCoreDurabilityLog], and [MemoryCoreRecovery] into one
 * [MemoryCore]/[MemoryRetrieval] implementation, mirroring
 * [InMemoryMemoryCore]'s own single-class, both-interfaces shape exactly
 * (itself mirroring `InMemoryKnowledgeStore`'s own "one interface, one
 * implementing class" precedent, per [InMemoryMemoryCore]'s own KDoc).
 * A caller holding only a [MemoryCore] or [MemoryRetrieval] reference
 * cannot observe, from the public contract alone, whether the underlying
 * implementation is this class or the plain, non-durable
 * [InMemoryMemoryCore] it wraps -- the Durability Contract Design's own
 * Section 1 purpose statement, realised directly.
 *
 * ## Construction: recovery exactly once, never partial
 *
 * There is no public constructor. [create] is the only way to obtain an
 * instance: it recovers a complete [InMemoryMemoryCore] via
 * [MemoryCoreRecovery.recover] and wraps it. If recovery fails, [create]
 * throws the same [MemoryCoreRecoveryException] [MemoryCoreRecovery]
 * itself would throw -- unwrapped, since it is already a specific,
 * well-named failure type this Unit has no reason to obscure behind a
 * second wrapper. No instance of this class can exist without a
 * completed, successful recovery behind it; there is no code path
 * producing a "partial" instance.
 *
 * ## Recovery happens only at construction -- never again
 *
 * No method on this class ever calls [MemoryCoreRecovery.recover] a
 * second time. There is no lazy replay, no background repair, and no
 * automatic rebuilding anywhere in this file -- the [InMemoryMemoryCore]
 * instance [create] recovers once is the same instance every subsequent
 * operation reads from and writes to, for this instance's entire
 * lifetime.
 *
 * ## Write operations: durable append before in-memory effect, always
 *
 * `docs/architecture/MEMORY_CORE_DURABILITY_CONTRACT_DESIGN.md`'s own
 * Section 11 names exactly one acceptable failure window -- durable
 * commit completing before the in-memory update does -- and never once
 * contemplates the reverse. Every one of this class's own six write
 * operations therefore: (1) obtains a complete, validated,
 * uniquely-identified record without yet storing it (the five new
 * `prepare*` functions on [InMemoryMemoryCore], Implementation Unit 6's
 * own narrow addition to that class, or -- for [transitionStatus] --
 * reads the current status and validates the transition using the
 * existing [MemoryCoreLifecycleTransitions], the single source of truth
 * [InMemoryMemoryCore.transitionStatus] itself already uses); (2)
 * durably appends the corresponding [DurableMemoryCoreEntry]; (3), only
 * if the append succeeds, makes the change visible in memory, via the
 * existing `restore*` functions (creation) or the existing, unmodified
 * [InMemoryMemoryCore.transitionStatus] (transitions). If the append
 * throws, this class propagates that failure immediately -- no retry,
 * no rollback, no compensation, and (for the five creation operations)
 * no attempt to un-mint the identifier [InMemoryMemoryCore.prepareProvenance]
 * and its four counterparts already minted, which simply becomes a
 * permanent, already-tolerated gap in that kind's own identifier
 * sequence (Implementation Unit 6's own `prepare*` KDoc, on
 * [InMemoryMemoryCore], states this in full).
 *
 * ## Why `transitionStatus` pre-validates before appending -- reusing
 * the existing check, never duplicating it
 *
 * Naively appending a [DurableMemoryCoreEntry.StatusTransitioned] entry
 * before confirming the transition is actually valid would risk durably
 * committing a transition fact that the subsequent, real
 * [InMemoryMemoryCore.transitionStatus] call then rejects -- leaving a
 * corrupt-looking entry in the durability log that a future recovery
 * pass ([MemoryCoreRecovery]'s own `ImpossibleTransition`/
 * `PriorStatusMismatch` handling) would then refuse to replay, failing
 * recovery entirely for a fact that was never actually applied. This
 * class avoids that by calling
 * [MemoryCoreLifecycleTransitions.requireValidTransition] -- the exact
 * same closed-table check [InMemoryMemoryCore.transitionStatus] itself
 * uses internally, not a second, parallel implementation of the
 * transition rules -- *before* ever touching the durability log. A
 * transition that fails this check never reaches the log at all. A
 * narrower window remains, disclosed and not closed here: a transition
 * that races with a concurrent one between this class's own
 * pre-validation read and its own subsequent append could still durably
 * commit an entry the real `transitionStatus` call then rejects --
 * exactly the class of gap Implementation Unit 7 (Concurrency and
 * Ordering), explicitly out of scope for this Unit, exists to close.
 *
 * ## Read operations: pure delegation, nothing else
 *
 * Every [MemoryRetrieval] method on this class delegates directly,
 * unchanged, to the wrapped [InMemoryMemoryCore] -- no reference to
 * [durabilityLog] appears anywhere in any read method, confirmed both by
 * direct reading and by a dedicated structural test. No read method
 * performs replay, allocates an identifier, or touches the filesystem in
 * any way.
 *
 * ## Concurrency: one outer [Mutex], serialising the whole append-then-commit
 * sequence of every write operation (Implementation Unit 7)
 *
 * [writeMutex] guards the entire body of each of this class's own six
 * write operations, from the moment a record is prepared/validated
 * through the durable append to the final in-memory commit -- distinct
 * from, and outer to, [InMemoryMemoryCore]'s own internal `Mutex` and
 * [MemoryCoreDurabilityLog]'s own internal write guard, exactly as
 * `docs/implementation/MEMORY_CORE_DURABILITY_IMPLEMENTATION_PLAN.md`'s
 * own Unit 7 fixes: "a `DurableMemoryCore`-level `Mutex` guarding the
 * append-then-delegate sequence as a whole... so that two concurrent
 * callers can never observe one call's durable commit interleaved with a
 * different call's in-memory update." Without this guard, two concurrent
 * writers' own append-then-commit sequences could interleave (writer A
 * appends, writer B appends, writer B commits, writer A commits) --
 * durable order and in-memory commit order would then disagree, breaking
 * the Contract Design's own Section 10 total-write-order guarantee
 * `findByTimeRange`'s own cross-kind tiebreak depends on. This is stated,
 * and implemented, purely behaviourally: `Mutex` is this Unit's own
 * chosen mechanism, not a fixed requirement (Scope Lock Section 12 itself
 * forbids naming a concrete mechanism as the binding requirement) --
 * freely replaceable by an equally correct alternative without a
 * governance revision.
 *
 * As a direct consequence, this guard also closes the one race window
 * [transitionStatus]'s own KDoc previously disclosed and left open: a
 * concurrent transition invalidating another's own pre-validated
 * `priorStatus` between its read and its append is no longer possible,
 * since no second write operation of any kind can begin until the first
 * one's own entire read-validate-append-commit sequence has completed.
 *
 * Read operations deliberately do **not** acquire [writeMutex] -- a
 * reader observing durably-committed-but-not-yet-in-memory-visible state
 * during the narrow window between one write's own append and its own
 * commit is exactly the "acceptable" window Contract Design Section 11
 * already names, not a condition this Unit's own guard exists to close.
 *
 * ## What this Unit deliberately does not implement
 *
 * Composition into `ParkerRuntime.kt` (Implementation Unit 8); backup;
 * snapshots; compaction; pruning; version migration; cross-process
 * concurrency (Contract Design Section 10 explicitly inherits, not
 * closes, this limitation -- [writeMutex] is an in-process guard only).
 * No change exists anywhere to `MemoryCore`, `MemoryRetrieval`, the
 * recovery algorithm ([MemoryCoreRecovery]'s own logic, called exactly as
 * Unit 4 left it), the codec, [MemoryCoreDurabilityLog]'s own interface,
 * [InMemoryMemoryCore.restoreIdentifierCounters]'s own identifier
 * restoration logic (Implementation Unit 5), or [InMemoryMemoryCore]'s
 * own internal `Mutex` (untouched) -- confirmed by this Unit's own
 * additive-only diff.
 */
internal class DurableMemoryCore private constructor(
    private val inMemory: InMemoryMemoryCore,
    private val durabilityLog: MemoryCoreDurabilityLog,
) : MemoryCore, MemoryRetrieval {

    companion object {
        /**
         * The only way to obtain a [DurableMemoryCore]. Recovers
         * [inMemory]'s own complete starting state from [durabilityLog]
         * exactly once, via [MemoryCoreRecovery.recover] -- if recovery
         * fails, this function throws and no instance is ever produced.
         */
        suspend fun create(durabilityLog: MemoryCoreDurabilityLog): DurableMemoryCore {
            val recovered = MemoryCoreRecovery.recover(durabilityLog)
            return DurableMemoryCore(recovered, durabilityLog)
        }
    }

    /**
     * Implementation Unit 7 (Concurrency and Ordering). Guards the entire
     * body of every write operation below, outer to and distinct from
     * [InMemoryMemoryCore]'s own internal `Mutex` and
     * [MemoryCoreDurabilityLog]'s own internal write guard -- see this
     * class's own top-level KDoc, "Concurrency" section, for the full
     * reasoning.
     */
    private val writeMutex = Mutex()

    // ================= Write behaviour: prepare, append, then commit -- serialised as a whole =================

    override suspend fun createProvenance(requestingPrincipalId: PrincipalId, candidate: CandidateProvenance): Provenance =
        writeMutex.withLock {
            val prepared = inMemory.prepareProvenance(candidate)
            durabilityLog.append(DurableMemoryCoreEntry.ProvenanceCreated(provenance = prepared))
            inMemory.restoreProvenance(prepared)
            prepared
        }

    override suspend fun createEntity(requestingPrincipalId: PrincipalId, candidate: CandidateEntity): Entity =
        writeMutex.withLock {
            val prepared = inMemory.prepareEntity(candidate)
            durabilityLog.append(DurableMemoryCoreEntry.EntityCreated(entity = prepared))
            inMemory.restoreEntity(prepared)
            prepared
        }

    override suspend fun registerDocument(requestingPrincipalId: PrincipalId, candidate: CandidateDocument): Document =
        writeMutex.withLock {
            val prepared = inMemory.prepareDocument(candidate)
            durabilityLog.append(DurableMemoryCoreEntry.DocumentRegistered(document = prepared))
            inMemory.restoreDocument(prepared)
            prepared
        }

    override suspend fun createAssertion(requestingPrincipalId: PrincipalId, candidate: CandidateAssertion): Assertion =
        writeMutex.withLock {
            val prepared = inMemory.prepareAssertion(candidate)
            durabilityLog.append(DurableMemoryCoreEntry.AssertionCreated(assertion = prepared))
            inMemory.restoreAssertion(prepared)
            prepared
        }

    override suspend fun createRelationship(requestingPrincipalId: PrincipalId, candidate: CandidateRelationship): Relationship =
        writeMutex.withLock {
            val prepared = inMemory.prepareRelationship(candidate)
            durabilityLog.append(DurableMemoryCoreEntry.RelationshipCreated(relationship = prepared))
            inMemory.restoreRelationship(prepared)
            prepared
        }

    override suspend fun transitionStatus(
        requestingPrincipalId: PrincipalId,
        reference: MemoryCoreRecordReference,
        targetStatus: MemoryCoreRecordStatus,
    ): MemoryCoreRecord = writeMutex.withLock {
        val priorStatus = currentStatusOf(reference)
            ?: throw NoSuchElementException("${describeReference(reference)} does not exist")
        MemoryCoreLifecycleTransitions.requireValidTransition(priorStatus, targetStatus)

        durabilityLog.append(
            DurableMemoryCoreEntry.StatusTransitioned(
                reference = reference,
                priorStatus = priorStatus,
                targetStatus = targetStatus,
                transitionedAt = Instant.now(),
            ),
        )

        inMemory.transitionStatus(requestingPrincipalId, reference, targetStatus)
    }

    private suspend fun currentStatusOf(reference: MemoryCoreRecordReference): MemoryCoreRecordStatus? = when (reference) {
        is MemoryCoreRecordReference.ToEntity -> inMemory.getEntity(SYSTEM_PRINCIPAL, reference.entityId)?.status
        is MemoryCoreRecordReference.ToDocument -> inMemory.getDocument(SYSTEM_PRINCIPAL, reference.documentId)?.status
        is MemoryCoreRecordReference.ToAssertion -> inMemory.getAssertion(SYSTEM_PRINCIPAL, reference.assertionId)?.status
        is MemoryCoreRecordReference.ToRelationship -> inMemory.getRelationship(SYSTEM_PRINCIPAL, reference.relationshipId)?.status
    }

    /**
     * Mirrors, field-for-field, the exact `"<Kind> '<value>'"` format
     * [InMemoryMemoryCore.transitionStatus] already uses in its own four
     * [NoSuchElementException] messages -- so a caller sees an identically-shaped
     * message whether talking to this decorator or the raw
     * [InMemoryMemoryCore] directly, per this class's own "no altered
     * `MemoryCore` semantics" purpose.
     */
    private fun describeReference(reference: MemoryCoreRecordReference): String = when (reference) {
        is MemoryCoreRecordReference.ToEntity -> "Entity '${reference.entityId.value}'"
        is MemoryCoreRecordReference.ToDocument -> "Document '${reference.documentId.value}'"
        is MemoryCoreRecordReference.ToAssertion -> "Assertion '${reference.assertionId.value}'"
        is MemoryCoreRecordReference.ToRelationship -> "Relationship '${reference.relationshipId.value}'"
    }

    // ================= Read behaviour: pure delegation =================

    override suspend fun getEntity(requestingPrincipalId: PrincipalId, entityId: EntityId): Entity? =
        inMemory.getEntity(requestingPrincipalId, entityId)

    override suspend fun getDocument(requestingPrincipalId: PrincipalId, documentId: DocumentId): Document? =
        inMemory.getDocument(requestingPrincipalId, documentId)

    override suspend fun getAssertion(requestingPrincipalId: PrincipalId, assertionId: AssertionId): Assertion? =
        inMemory.getAssertion(requestingPrincipalId, assertionId)

    override suspend fun getRelationship(requestingPrincipalId: PrincipalId, relationshipId: RelationshipId): Relationship? =
        inMemory.getRelationship(requestingPrincipalId, relationshipId)

    override suspend fun findEntities(query: EntityLookupQuery): List<Entity> = inMemory.findEntities(query)

    override suspend fun findDocuments(query: DocumentLookupQuery): List<Document> = inMemory.findDocuments(query)

    override suspend fun traverseRelationships(query: RelationshipTraversalQuery): List<Relationship> =
        inMemory.traverseRelationships(query)

    override suspend fun findByTimeRange(query: ChronologicalLookupQuery): List<MemoryCoreRecord> =
        inMemory.findByTimeRange(query)

    override suspend fun findByMetadata(query: MetadataLookupQuery): List<MemoryCoreRecord> =
        inMemory.findByMetadata(query)

    override suspend fun findByProvenance(query: ProvenanceLookupQuery): List<MemoryCoreRecord> =
        inMemory.findByProvenance(query)
}

/**
 * The [PrincipalId] [DurableMemoryCore] passes to the read calls its own
 * [DurableMemoryCore.transitionStatus] performs purely to determine a
 * target record's current status before constructing a durable entry --
 * never read or interpreted by [InMemoryMemoryCore] (Errata 004), so its
 * exact value has no behavioural effect. Named for diagnostic clarity
 * only, mirroring [MemoryCoreRecovery]'s own identical `system.*`
 * Principal-naming convention for an internally-originated, non-caller-
 * initiated read.
 */
private val SYSTEM_PRINCIPAL = PrincipalId("system.durable-memory-core")
